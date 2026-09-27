package com.seobuk.chess.ui.update

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import com.seobuk.chess.MainActivity
import com.seobuk.chess.data.Asset
import com.seobuk.chess.data.ProgressStore
import com.seobuk.chess.data.Release
import com.seobuk.chess.data.Updates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data object Installing : UpdateState
    data class Failed(val message: String) : UpdateState
}

/** What a check found: [HAS_UPDATE] means the Home card shows (or is already busy installing). */
enum class UpdateCheck { HAS_UPDATE, UP_TO_DATE, ERROR }

/**
 * Self-update from this repo's GitHub releases: a throttled check on every resume, then download to the cache
 * and hand the APK to PackageInstaller. The installer's result comes back to [MainActivity] and [onInstallStatus].
 * All checks fail quietly (debug log, state stays as it was); only a download or install the user started shows an error.
 */
class Updater(private val app: Application, private val store: ProgressStore, private val scope: CoroutineScope) {
    var state by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    @Suppress("DEPRECATION")
    val versionName: String = app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "0"

    /** Debug builds only: the launch-intent extra "updateUrl" replaces the releases/latest URL and allows 10.0.2.2 downloads. */
    var debugUrl: String? = null
        set(value) {
            if (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) field = value
        }

    private var release: Release? = null
    private var awaitingPermission = false
    private var job: Job? = null
    private var sessionId = -1

    private val downloadPrefixes get() = listOf(Updates.DOWNLOAD_PREFIX) + listOfNotNull(EMULATOR_HOST.takeIf { debugUrl != null })
    private val redirectPrefixes get() = Updates.REDIRECT_PREFIXES + listOfNotNull(EMULATOR_HOST.takeIf { debugUrl != null })

    /** Each resume: finish an install that waited for the unknown-sources permission, then a throttled check. */
    fun onResume() {
        if (awaitingPermission) {
            awaitingPermission = false
            if (app.packageManager.canRequestPackageInstalls()) install()
        }
        scope.launch { check() }
    }

    suspend fun check(force: Boolean = false): UpdateCheck {
        if (state is UpdateState.Downloading || state is UpdateState.Installing) return UpdateCheck.HAS_UPDATE
        if (!force && state is UpdateState.Available) return UpdateCheck.HAS_UPDATE
        if (installer() == PLAY_STORE) return UpdateCheck.UP_TO_DATE
        val now = System.currentTimeMillis()
        if (!force && Updates.throttled(store.updateCheckedAt, now)) return UpdateCheck.UP_TO_DATE
        val result = fetch(force)
        // A found update is not persisted, so it is not throttled either: a restart fetches it again and shows the card.
        if (result != UpdateCheck.HAS_UPDATE) store.updateCheckedAt = now
        return result
    }

    private suspend fun fetch(force: Boolean): UpdateCheck {
        val found = try {
            withContext(Dispatchers.IO) { fetchLatest() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "check failed", e)
            return UpdateCheck.ERROR
        }
        if (found == null || !Updates.isNewer(found.version, versionName)) return UpdateCheck.UP_TO_DATE
        release = found
        if (!force && store.updateSnoozed(found.version)) return UpdateCheck.UP_TO_DATE
        state = UpdateState.Available(found)
        return UpdateCheck.HAS_UPDATE
    }

    /** "업데이트" / "다시 시도": asks for the unknown-sources permission once, then downloads and commits the install. */
    fun install() {
        val r = release ?: return
        if (!app.packageManager.canRequestPackageInstalls()) {
            awaitingPermission = true
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { app.startActivity(settings) }.onFailure {
                awaitingPermission = false
                state = UpdateState.Failed("설치 허용 설정을 열지 못했어요.")
            }
            return
        }
        if (job?.isActive == true) return
        state = UpdateState.Downloading(0f)
        job = scope.launch {
            val error = withContext(Dispatchers.IO) {
                val file = try {
                    download(r.url, r.sizeBytes)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.d(TAG, "download failed", e)
                    return@withContext "받지 못했어요. 연결을 확인해 주세요."
                }
                ensureActive() // the user left the app: no install behind their back
                state = UpdateState.Installing // before commit: the installer's result may arrive at once
                try {
                    commit(file); null
                } catch (e: Exception) {
                    Log.d(TAG, "install failed", e)
                    "설치를 시작하지 못했어요."
                }
            }
            error?.let { state = UpdateState.Failed(it) }
        }
    }

    /** "나중에": hides this version for a day, from the Available or the Failed card. */
    fun snooze() {
        release?.let { store.snoozeUpdate(it.version) }
        state = UpdateState.Idle
    }

    /** The PackageInstaller status delivered to our PendingIntent; returns the confirmation screen to start, if any. */
    fun onInstallStatus(intent: Intent): Intent? {
        // MainActivity is exported, so anyone can send ACTION_INSTALL: only our own pending session may change state
        // or hand us an Intent to start (intent redirection).
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        if (state !is UpdateState.Installing || intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1) != sessionId) {
            Log.d(TAG, "ignored install status $status: not our session")
            return null
        }
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION ->
                return IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.takeIf { it.action in CONFIRM_ACTIONS }
            PackageInstaller.STATUS_SUCCESS -> state = UpdateState.Idle // the process is about to be replaced
            PackageInstaller.STATUS_FAILURE_ABORTED -> state = release?.let(UpdateState::Available) ?: UpdateState.Idle // user cancelled: offer again
            else -> {
                Log.d(TAG, "install status $status: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
                state = UpdateState.Failed("설치하지 못했어요.")
            }
        }
        return null
    }

    private fun installer(): String? = if (Build.VERSION.SDK_INT >= 30) {
        app.packageManager.getInstallSourceInfo(app.packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION") app.packageManager.getInstallerPackageName(app.packageName)
    }

    /** GET releases/latest; null when the release has no APK from this repo. */
    private fun fetchLatest(): Release? {
        val c = URL(debugUrl ?: Updates.LATEST_URL).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            val o = JSONObject(c.inputStream.bufferedReader().readText())
            val assets = o.optJSONArray("assets")?.let { arr ->
                List(arr.length()) { i -> arr.getJSONObject(i).let { Asset(it.optString("name"), it.optString("browser_download_url"), it.optLong("size")) } }
            }.orEmpty()
            val apk = Updates.pickAsset(assets, downloadPrefixes) ?: return null
            return Release(Updates.version(o.getString("tag_name")), apk.size, Updates.firstLine(o.opt("body") as? String), apk.url)
        } finally {
            c.disconnect()
        }
    }

    /**
     * Downloads [url] to cache/update.apk, following redirects only inside the allowlist. The API's asset [size] is
     * the expected length (Content-Length, when present, must agree); stops promptly when the coroutine is cancelled.
     */
    private suspend fun download(url: String, size: Long): File {
        val file = File(app.cacheDir, "update.apk").apply { delete() }
        try {
            var next = url
            repeat(5) {
                val c = URL(next).openConnection() as HttpURLConnection
                c.instanceFollowRedirects = false
                c.connectTimeout = 8000
                c.readTimeout = 30000
                c.setRequestProperty("Accept-Encoding", "identity") // no transparent gzip: byte counts must match
                try {
                    val code = c.responseCode
                    if (code in REDIRECTS) {
                        next = URL(URL(next), c.getHeaderField("Location") ?: throw IOException("redirect without Location")).toString()
                        if (redirectPrefixes.none(next::startsWith)) throw IOException("redirect outside allowlist: $next")
                        return@repeat
                    }
                    if (code != 200) throw IOException("HTTP $code")
                    val header = c.contentLengthLong
                    val total = if (size > 0) size else header
                    if (total <= 0) throw IOException("unknown size")
                    if (header > 0 && header != total) throw IOException("Content-Length $header, expected $total")
                    var read = 0L
                    var pct = -1
                    c.inputStream.use { input ->
                        file.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                read += n
                                if (read > total) throw IOException("longer than $total")
                                val p = (read * 100 / total).toInt()
                                if (p != pct) { pct = p; state = UpdateState.Downloading(read.toFloat() / total) }
                            }
                        }
                    }
                    if (read != total) throw IOException("size mismatch $read/$total")
                    return file
                } finally {
                    c.disconnect()
                }
            }
            throw IOException("too many redirects")
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    /** Streams [file] into a PackageInstaller session and commits it; the result arrives at [MainActivity]. */
    private fun commit(file: File) {
        val installer = app.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.packageName)
            setSize(file.length())
            setInstallReason(PackageManager.INSTALL_REASON_USER)
        }
        sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            session.openWrite("base.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val back = Intent(app, MainActivity::class.java).setAction(ACTION_INSTALL)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // the installer fills in the status
            // Target 35+ blocks the PendingIntent creator's background activity start by default; allow it explicitly.
            @Suppress("DEPRECATION")
            val options = if (Build.VERSION.SDK_INT >= 34) android.app.ActivityOptions.makeBasic()
                .setPendingIntentCreatorBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle() else null
            session.commit(PendingIntent.getActivity(app, 0, back, flags, options).intentSender)
        } catch (e: Exception) {
            session.abandon()
            throw e
        } finally {
            session.close()
            file.delete() // committed: the session has its own copy; failed: nothing left to retry with
        }
    }

    companion object {
        const val ACTION_INSTALL = "com.seobuk.chess.INSTALL"
        private const val TAG = "Updater"
        private const val PLAY_STORE = "com.android.vending"
        private const val EMULATOR_HOST = "http://10.0.2.2"
        private val REDIRECTS = setOf(301, 302, 303, 307, 308)
        /** The system's install confirmation (hidden PackageInstaller constants): API 29+ and API 26-28. */
        private val CONFIRM_ACTIONS = setOf("android.content.pm.action.CONFIRM_INSTALL", "android.content.pm.action.CONFIRM_PERMISSIONS")
    }
}
