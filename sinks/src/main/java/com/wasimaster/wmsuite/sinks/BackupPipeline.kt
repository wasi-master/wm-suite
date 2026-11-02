package com.wasimaster.wmsuite.sinks

import com.wasimaster.wmkeyboard.core.settings.sink.BackupSink
import com.wasimaster.wmkeyboard.core.settings.sink.BackupSinkException
import com.wasimaster.wmkeyboard.core.settings.sink.SinkEntry
import com.wasimaster.wmkeyboard.core.settings.sink.SinkError
import com.wasimaster.wmsuite.sinks.net.runCancellable
import java.io.IOException
import java.security.MessageDigest

/**
 * One backup to one destination, in the only order that is safe: check the
 * destination, write, read back what actually landed and compare it, and only
 * then delete old copies. A backup that fails any step leaves every older copy
 * where it was, because rotating first is how a bad run costs you the good ones.
 *
 * [prefix] and [extension] are the whole of what marks a file as ours. The
 * destination may be a folder the user keeps other things in, so rotation never
 * looks at a name that lacks both.
 */
public class BackupPipeline(
    private val prefix: String,
    private val extension: String,
    private val mimeType: String = "application/octet-stream",
) {
    /** What a successful run left behind. */
    public data class Outcome(val written: SinkEntry, val deleted: Int, val deleteFailures: Int)

    /**
     * Writes [payload] as `prefix + stamp + extension`, verifies it, and keeps
     * the newest [keep] copies. [verify] gets the bytes read back and may check
     * more than equality, such as that a vault file still parses; returning
     * false fails the run and the file it wrote is removed.
     */
    public suspend fun run(
        sink: BackupSink,
        payload: ByteArray,
        stamp: String,
        keep: Int,
        verify: (ByteArray) -> Boolean = { true },
    ): Result<Outcome> = runCancellable {
        require(keep >= 1) { "A rotation that keeps nothing is a delete." }
        sink.readiness().getOrThrow()
        val entry = sink.write(prefix + stamp + extension, mimeType) { it.write(payload) }.getOrThrow()
        val readBack = sink.read(entry).getOrThrow().use { it.readBytes() }
        val same = MessageDigest.isEqual(sha256(readBack), sha256(payload))
        if (!same || !verify(readBack)) {
            sink.delete(entry)
            throw BackupSinkException(SinkError.IO, IOException("Backup did not read back the same"))
        }
        var deleted = 0
        var failures = 0
        // A listing that fails here is not a failed backup: the new copy is
        // safe, and the old ones can wait for the next run.
        val all = sink.list().getOrNull().orEmpty()
        for (old in doomed(all, keep, entry)) {
            if (sink.delete(old).isSuccess) deleted++ else failures++
        }
        Outcome(entry, deleted, failures)
    }

    /**
     * Which of [entries] rotation removes: ours only, oldest first past [keep],
     * and never [fresh], whatever a coarse clock on the far side says about it.
     */
    public fun doomed(entries: List<SinkEntry>, keep: Int, fresh: SinkEntry? = null): List<SinkEntry> =
        entries.asSequence()
            .filter { it.name.startsWith(prefix) && it.name.endsWith(extension) }
            .filter { fresh == null || it.id != fresh.id }
            .sortedByDescending { it.modifiedAtMs }
            .drop((keep - if (fresh == null) 0 else 1).coerceAtLeast(0))
            .toList()

    private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
}
