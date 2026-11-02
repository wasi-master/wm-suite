# --- JSch (SFTP backup location) ----------------------------------------------
# JSch builds every algorithm with Class.forName on a name from its config
# table, so R8 sees no reference to any of them and would strip them all. Keep
# exactly the classes SftpAlgorithms can reach, each with the
# no-arg constructor JSch calls; NewBackupLocationsTest fails when the two
# drift apart. Everything else in JSch (ChannelSftp, Kerberos, zlib, agents,
# the Bouncy-Castle-only ciphers nobody offers) goes, which is the point of
# naming them rather than keeping com.jcraft.jsch.**.
-keep class com.jcraft.jsch.jce.** { <init>(); }
-keep class com.jcraft.jsch.jbcrypt.JBCrypt { <init>(); }
-keep class com.jcraft.jsch.bc.XDH { <init>(); }
-keep class com.jcraft.jsch.bc.MLKEM768 { <init>(); }
-keep class com.jcraft.jsch.bc.SNTRUP761 { <init>(); }
-keep class com.jcraft.jsch.bc.SignatureEd25519 { <init>(); }
-keep class com.jcraft.jsch.bc.KeyPairGenEdDSA { <init>(); }
-keep class com.jcraft.jsch.bc.ChaCha20Poly1305 { <init>(); }
-keep class com.jcraft.jsch.DH25519MLKEM768 { <init>(); }
-keep class com.jcraft.jsch.DH25519SNTRUP761 { <init>(); }
-keep class com.jcraft.jsch.DH25519 { <init>(); }
-keep class com.jcraft.jsch.DHEC256 { <init>(); }
-keep class com.jcraft.jsch.DHEC384 { <init>(); }
-keep class com.jcraft.jsch.DHEC521 { <init>(); }
-keep class com.jcraft.jsch.DHGEX256 { <init>(); }
-keep class com.jcraft.jsch.DHGEX1 { <init>(); }
-keep class com.jcraft.jsch.DHG14 { <init>(); }
-keep class com.jcraft.jsch.DHG14256 { <init>(); }
-keep class com.jcraft.jsch.DHG16 { <init>(); }
-keep class com.jcraft.jsch.DHG18 { <init>(); }
-keep class com.jcraft.jsch.CipherNone { <init>(); }
-keep class com.jcraft.jsch.UserAuthNone { <init>(); }
-keep class com.jcraft.jsch.UserAuthPassword { <init>(); }
-keep class com.jcraft.jsch.UserAuthKeyboardInteractive { <init>(); }
-keep class com.jcraft.jsch.UserAuthPublicKey { <init>(); }
# Several of those classes and constructors are package-private, reached from
# JSch's own classes in the same package. Repackaging would split them apart
# and turn the lookup into an IllegalAccessException, as it did for LuaJ above.
-keeppackagenames com.jcraft.jsch.**
# Optional integrations JSch compiles against and never loads here.
-dontwarn org.ietf.jgss.**
-dontwarn com.sun.jna.**
-dontwarn org.newsclub.net.unix.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.slf4j.**
