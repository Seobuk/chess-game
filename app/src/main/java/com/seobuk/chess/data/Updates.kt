package com.seobuk.chess.data

/** One GitHub release asset as the API lists it. */
data class Asset(val name: String, val url: String, val size: Long)

/** A newer release ready to offer: [version] without the "v", the APK [url] and the first line of the notes. */
data class Release(val version: String, val sizeBytes: Long, val note: String?, val url: String)

/** Self-update decisions without Android, so the JVM tests cover them. */
object Updates {
    const val LATEST_URL = "https://api.github.com/repos/Seobuk/chess-game/releases/latest"
    const val DOWNLOAD_PREFIX = "https://github.com/Seobuk/chess-game/releases/download/"
    const val CHECK_INTERVAL_MS = 30 * 60 * 1000L
    const val SNOOZE_MS = 24 * 60 * 60 * 1000L

    /** "v0.2.0" -> "0.2.0". */
    fun version(tag: String): String = tag.trim().removePrefix("v")

    /** Dot-separated numeric compare: missing parts are 0 (0.1 == 0.1.0), non-numeric parts count as 0. */
    fun compare(a: String, b: String): Int {
        val pa = version(a).split('.').map { it.trim().toIntOrNull() ?: 0 }
        val pb = version(b).split('.').map { it.trim().toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val d = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    fun isNewer(tag: String, installed: String): Boolean = compare(tag, installed) > 0

    /** The first .apk asset served from an allowed [prefixes] location; anything else is ignored. */
    fun pickAsset(assets: List<Asset>, prefixes: List<String> = listOf(DOWNLOAD_PREFIX)): Asset? =
        assets.firstOrNull { a -> a.name.endsWith(".apk") && prefixes.any(a.url::startsWith) }

    /**
     * A download redirect may only land on GitHub itself or its asset CDN (any *.githubusercontent.com host: the
     * exact host has changed before, e.g. objects -> release-assets), and only over https.
     */
    fun redirectAllowed(url: String): Boolean {
        val u = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        val host = u.host?.lowercase() ?: return false
        return u.scheme == "https" && (host == "github.com" || host.endsWith(".githubusercontent.com"))
    }

    /** One check per [CHECK_INTERVAL_MS]; a clock set back (negative elapsed) does not block checks. */
    fun throttled(checkedAt: Long, now: Long): Boolean = now - checkedAt in 0 until CHECK_INTERVAL_MS

    /** "나중에" hides [version] for a day; a different version shows again at once. */
    fun snoozed(version: String, snoozedVersion: String?, snoozedAt: Long, now: Long): Boolean =
        version == snoozedVersion && now - snoozedAt in 0 until SNOOZE_MS

    /** First non-empty line of the release notes, without a leading markdown marker. */
    fun firstLine(body: String?): String? =
        body?.lineSequence()?.map { it.trimStart('#', '-', '*', ' ').trim() }?.firstOrNull { it.isNotEmpty() }
}
