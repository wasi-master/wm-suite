package com.wasimaster.wmkeyboard.core.settings

/**
 * Where an automatic backup goes.
 *
 * Twenty destinations, none of them a server of ours. That is the whole shape of
 * this feature: the app writes a file somewhere the user already has, and never
 * holds a copy.
 */
enum class BackupDestination(
    /** Stable on disk. Never store an enum's [name] and hope. */
    val id: String,
) {
    /**
     * A folder picked through the Storage Access Framework. Reaches anything
     * with a `DocumentsProvider`, needs no account of any kind, and works on a
     * device with no Google Play services at all. The default, and the one to
     * suggest first.
     */
    FOLDER("folder"),

    /** A WebDAV server: Nextcloud, ownCloud, or anything else that speaks it. */
    WEBDAV("webdav"),

    /**
     * The app's own hidden folder in the user's Google Drive.
     *
     * Only reachable on a build with Google Play services compiled in, and only
     * after the user authorizes it. [FOLDER] already reaches Drive through the
     * Drive app's own provider; this exists for the case where that app is not
     * installed, and to put the backups somewhere the user cannot delete by
     * tidying up a folder.
     */
    DRIVE("drive"),

    /**
     * A bucket on anything that speaks the S3 API: AWS itself, MinIO on a
     * machine at home, Cloudflare R2, Backblaze B2, Wasabi, Garage.
     *
     * One protocol reaching all of them, with no account of ours and no OAuth
     * dance — the credentials are a key pair the user already has.
     */
    S3("s3"),

    /** The app's own folder in the user's Dropbox, via the App Folder scope. */
    DROPBOX("dropbox"),

    /** The app's own folder in the user's OneDrive, via `Files.ReadWrite.AppFolder`. */
    ONEDRIVE("onedrive"),

    /**
     * An FTP server, with TLS unless the user insists otherwise.
     *
     * The oldest option here and the one with the fewest guarantees, kept
     * because a lot of home NAS boxes and cheap web hosts offer nothing else.
     */
    FTP("ftp"),

    /** An SSH server's SFTP subsystem. See [SftpConfig]. */
    SFTP("sftp"),

    /** A Windows or Samba share, over SMB 2 or 3. See [SmbConfig]. */
    SMB("smb"),

    /** A folder in a GitHub, GitLab, Gitea or Forgejo repository. See [GitConfig]. */
    GIT("git"),

    /** A folder of messages in a mail account. See [ImapConfig]. */
    IMAP("imap"),

    /** A folder in the user's Box, signed in through the browser. See [BoxConfig]. */
    BOX("box"),

    /** A folder in pCloud, through its own API. See [PCloudConfig]. */
    PCLOUD("pcloud"),

    /** A Synology NAS through DSM's File Station: Synology Drive and every shared folder. */
    SYNOLOGY("synology"),

    /** A QNAP NAS through the QTS File Station. */
    QNAP("qnap"),

    /** An NFS export, version 3. See [NfsConfig]. */
    NFS("nfs"),

    /** A folder in MEGA, encrypted on the phone the way MEGA's own apps do. See [MegaConfig]. */
    MEGA("mega"),

    /** A folder in iCloud Drive, through iCloud.com's web API. See [ICloudConfig]. */
    ICLOUD("icloud"),

    /** A folder in Proton Drive, end-to-end encrypted as Proton's apps do. See [ProtonConfig]. */
    PROTON("proton"),

    /**
     * A computer with WM Keyboard, paired over KDE Connect: sync files go to
     * it and come from it directly while both are on one network. A sync
     * location only, never a backup destination; made when the computer is
     * paired, not from the add sheet. See [BackupLocation.nearbyDeviceId].
     */
    NEARBY("nearby"),
}

/**
 * A bucket on an S3-compatible service.
 *
 * Grouped rather than flat inside [AutoBackupSettings] to keep each
 * destination's settings readable next to each other; the DataStore keys stay
 * flat as always.
 */
data class S3Config(
    /**
     * The service endpoint, for example `https://s3.eu-west-1.amazonaws.com`,
     * `https://<account>.r2.cloudflarestorage.com`, or a MinIO address on the
     * local network. Empty means AWS, derived from [region].
     */
    val endpoint: String = "",
    /** Signing region. `us-east-1` is what R2 and most MinIO setups expect. */
    val region: String = "us-east-1",
    val bucket: String = "",
    /** Optional key prefix, so backups can live in a folder inside the bucket. */
    val prefix: String = "",
    val accessKeyId: String = "",
    /** In [SettingsBackup.SECRET_KEYS]. */
    val secretAccessKey: String = "",
    /**
     * Whether to address the bucket as a path (`endpoint/bucket/key`) rather
     * than as a subdomain (`bucket.endpoint/key`).
     *
     * Virtual-hosted style is what AWS prefers and what R2 requires; path style
     * is what MinIO does out of the box and what an IP address must use, since
     * a bucket name cannot be prepended to one.
     */
    val pathStyle: Boolean = false,
    /** An [S3Preset] id, for the screen. Empty: the endpoint was typed. */
    val preset: String = "",
    /** The `{account}` part of the preset's endpoint. See [S3Account]. */
    val account: String = "",
)

/** An FTP server. */
data class FtpConfig(
    val host: String = "",
    val port: Int = 21,
    val user: String = "",
    /** In [SettingsBackup.SECRET_KEYS]. */
    val password: String = "",
    /** Directory to write into, relative to wherever the login lands. */
    val path: String = "",
    /**
     * Whether to negotiate TLS with `AUTH TLS` before logging in.
     *
     * On by default and worth leaving on: plain FTP sends the password as text
     * on the wire, exactly like WebDAV over http, which this app refuses
     * outright. FTP is allowed to be turned down to plain only because for some
     * old NAS boxes it is that or nothing, and the screen says what it costs.
     */
    val secure: Boolean = true,
)

/** Whether the destination is on the network at all, rather than a folder on this phone. */
val BackupDestination.needsNetwork: Boolean
    get() = this != BackupDestination.FOLDER && this != BackupDestination.NEARBY

/**
 * Whether access is an account sign-in rather than a folder grant or typed
 * credentials. Decides the words for a lost permission: "choose the folder
 * again" is the wrong advice for Dropbox.
 */
val BackupDestination.signsIn: Boolean
    get() = this == BackupDestination.DRIVE ||
        this == BackupDestination.DROPBOX ||
        this == BackupDestination.ONEDRIVE ||
        this == BackupDestination.BOX ||
        this == BackupDestination.PCLOUD ||
        this == BackupDestination.MEGA ||
        this == BackupDestination.ICLOUD ||
        this == BackupDestination.PROTON
