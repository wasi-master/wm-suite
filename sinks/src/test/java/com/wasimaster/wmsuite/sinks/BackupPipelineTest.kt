package com.wasimaster.wmsuite.sinks

import com.wasimaster.wmkeyboard.core.settings.sink.BackupSink
import com.wasimaster.wmkeyboard.core.settings.sink.SinkEntry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPipelineTest {
    private class Memory(var corrupt: Boolean = false, var unreadable: Boolean = false) : BackupSink {
        val files = LinkedHashMap<String, ByteArray>()
        val times = HashMap<String, Long>()
        var clock = 1000L
        override val id = "memory"
        override suspend fun readiness() = Result.success(Unit)
        override suspend fun write(name: String, mimeType: String, body: (OutputStream) -> Unit): Result<SinkEntry> {
            val out = ByteArrayOutputStream()
            body(out)
            val bytes = out.toByteArray()
            files[name] = if (corrupt) bytes.copyOf().also { it[0] = (it[0] + 1).toByte() } else bytes
            times[name] = ++clock
            return Result.success(SinkEntry(name, name, bytes.size.toLong(), clock))
        }
        override suspend fun list() = Result.success(files.map { (n, b) -> SinkEntry(n, n, b.size.toLong(), times.getValue(n)) })
        override suspend fun read(entry: SinkEntry): Result<InputStream> =
            if (unreadable) Result.failure(IOException("gone")) else Result.success(ByteArrayInputStream(files.getValue(entry.name)))
        override suspend fun delete(entry: SinkEntry): Result<Unit> {
            files.remove(entry.name)
            return Result.success(Unit)
        }
    }

    private val pipeline = BackupPipeline("wmpass-", ".wmvault")

    @Test
    fun `rotation keeps the newest copies and nothing of anyone else's`() = runBlocking {
        val sink = Memory()
        sink.files["tax-return.pdf"] = byteArrayOf(1)
        sink.times["tax-return.pdf"] = 1
        sink.files["wmpass-notes.txt"] = byteArrayOf(1)
        sink.times["wmpass-notes.txt"] = 2
        for (i in 1..5) pipeline.run(sink, byteArrayOf(i.toByte()), "$i", keep = 2).getOrThrow()
        assertEquals(setOf("tax-return.pdf", "wmpass-notes.txt", "wmpass-4.wmvault", "wmpass-5.wmvault"), sink.files.keys)
    }

    @Test
    fun `a copy that reads back different fails the run and spares the old ones`() = runBlocking {
        val sink = Memory()
        pipeline.run(sink, byteArrayOf(1), "1", keep = 1).getOrThrow()
        sink.corrupt = true
        val result = pipeline.run(sink, byteArrayOf(2), "2", keep = 1)
        assertTrue(result.isFailure)
        assertEquals(setOf("wmpass-1.wmvault"), sink.files.keys)
    }

    @Test
    fun `a failed read spares the old copies`() = runBlocking {
        val sink = Memory()
        pipeline.run(sink, byteArrayOf(1), "1", keep = 1).getOrThrow()
        sink.unreadable = true
        assertTrue(pipeline.run(sink, byteArrayOf(2), "2", keep = 1).isFailure)
        assertTrue("wmpass-1.wmvault" in sink.files)
    }

    @Test
    fun `the caller's own check can refuse a copy`() = runBlocking {
        val sink = Memory()
        val result = pipeline.run(sink, byteArrayOf(1), "1", keep = 1) { false }
        assertFalse(result.isSuccess)
        assertTrue(sink.files.isEmpty())
    }
}
