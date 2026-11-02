package com.wasimaster.wmsuite.sinks.net

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer

/**
 * What the sinks ask of the app they run in: whether this build may use the
 * internet, where a request is logged, and where a debug line goes. All three
 * do nothing until the app says otherwise, so an app without a network log
 * pays for none.
 */
public object SinkNet {
    /** Throws an [IOException] when this build may not reach the internet. */
    @Volatile
    public var checkInternet: () -> Unit = {}

    /** Opens a row in the app's network log, or null when it has none. */
    @Volatile
    public var logger: NetLogger? = null

    /** A debug line: level `d` or `w`, the message, and the failure when there is one. */
    @Volatile
    public var debug: (level: Char, message: String, failure: Throwable?) -> Unit = { _, _, _ -> }
}

/** One app's network log. See [SinkNet.logger]. */
public fun interface NetLogger {
    public fun call(
        method: String,
        scheme: String,
        host: String,
        port: Int,
        route: String?,
        background: Boolean,
    ): NetCall
}

/** One request being logged. */
public interface NetCall {
    public var status: Int
    public fun sent(bytes: Long)
    public fun received(bytes: Long)
    public fun fail(failure: Throwable)
    public fun end()

    public object None : NetCall {
        override var status: Int = 0
        override fun sent(bytes: Long) = Unit
        override fun received(bytes: Long) = Unit
        override fun fail(failure: Throwable) = Unit
        override fun end() = Unit
    }
}

/** Opens a row in the app's log, and a call that goes nowhere when it has none. */
public object NetLog {
    public fun callTo(
        method: String,
        scheme: String,
        host: String,
        port: Int,
        route: String?,
        background: Boolean,
    ): NetCall = SinkNet.logger?.call(method, scheme, host, port, route, background) ?: NetCall.None
}

/**
 * Refuses every request of an OkHttp client in a build without the internet
 * permission, with the `IOException` its callers already handle as "offline".
 * An application interceptor because OkHttp looks the host up before it runs
 * the network ones.
 */
public object InternetGate : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        SinkNet.checkInternet()
        return chain.proceed(chain.request())
    }
}

/**
 * Puts every request an OkHttp client makes into the app's network log. A
 * network interceptor, so each redirect and retry is its own row. The response
 * body is counted as the caller reads it, so an abandoned download says how
 * far it got.
 */
public class NetLogInterceptor(
    private val route: (HttpUrl) -> String? = { null },
    private val background: () -> Boolean = { false },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val call = NetLog.callTo(
            method = request.method,
            scheme = url.scheme,
            host = url.host,
            port = if (url.port == HttpUrl.defaultPort(url.scheme)) -1 else url.port,
            route = route(url),
            background = background(),
        )
        request.body?.contentLength()?.takeIf { it > 0 }?.let(call::sent)
        val response = try {
            chain.proceed(request)
        } catch (t: Throwable) {
            call.fail(t)
            call.end()
            throw t
        }
        call.status = response.code
        val counted = CountingSource(response.body.source(), call).buffer()
        return response.newBuilder()
            .body(counted.asResponseBody(response.body.contentType(), response.body.contentLength()))
            .build()
    }

    private class CountingSource(delegate: okio.Source, private val call: NetCall) : ForwardingSource(delegate) {
        override fun read(sink: Buffer, byteCount: Long): Long {
            val read = try {
                super.read(sink, byteCount)
            } catch (t: Throwable) {
                call.fail(t)
                call.end()
                throw t
            }
            if (read > 0) call.received(read) else if (read < 0) call.end()
            return read
        }

        override fun close() {
            try {
                super.close()
            } finally {
                call.end()
            }
        }
    }

    public companion object {
        /** Shows the whole path: for clients talking to the user's own servers. */
        public val PATH: (HttpUrl) -> String? = { it.encodedPath }
    }
}

/** Whether the traffic under way is a scheduled backup rather than something the user pressed. */
public object BackupTraffic {
    @Volatile
    public var unattended: Boolean = false
}

/** [runCatching], but coroutine cancellation still propagates. */
public inline fun <T> runCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

/** [ContentResolver.openInputStream] with the null case turned into a described failure. */
public fun ContentResolver.requireInputStream(uri: Uri): InputStream =
    openInputStream(uri) ?: throw IOException("Cannot open $uri for reading")

/**
 * [ContentResolver.openOutputStream] with the null case turned into a described
 * failure, and a mode that truncates: plain `"w"` does not on every provider,
 * which leaves the tail of a longer old file behind a short new one.
 */
public fun ContentResolver.requireOutputStream(uri: Uri, mode: String = "wt"): OutputStream =
    openOutputStream(uri, mode) ?: throw IOException("Cannot open $uri for writing")
