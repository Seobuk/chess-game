package com.seobuk.chess.data

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import com.seobuk.chess.ai.AiLevel
import com.seobuk.chess.ai.AiLevels
import com.seobuk.chess.ai.MoveQuality
import com.seobuk.chess.ui.screens.LevelRecord
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Player progress in SharedPreferences. Main-thread only. Every getter reads [rev] first, so a
 * composable that reads the store recomposes after any write.
 */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private val rev = mutableIntStateOf(0)

    val rating: Int get() = rev.intValue.let { prefs.getInt(RATING, START_RATING) }

    /** Oldest first, ends with the current rating; at most [HISTORY] entries. */
    val ratingHistory: List<Int>
        get() = rev.intValue.let {
            prefs.getString(HISTORY_KEY, "")!!.split(',').mapNotNull { it.toIntOrNull() }.ifEmpty { listOf(rating) }
        }

    val levelRecords: Map<Int, LevelRecord>
        get() = rev.intValue.let {
            AiLevels.all.associate { l ->
                l.level to LevelRecord(prefs.getInt("w_${l.level}", 0), prefs.getInt("d_${l.level}", 0), prefs.getInt("l_${l.level}", 0))
            }
        }

    val qualityTotals: Map<MoveQuality, Int>
        get() = rev.intValue.let { MoveQuality.entries.associateWith { prefs.getInt("q_${it.name}", 0) } }

    val hintsUsed: Int get() = rev.intValue.let { prefs.getInt(HINTS, 0) }

    /** Selected app theme id (drives the whole app and the board). */
    var themeId: String
        get() = rev.intValue.let { themeIdOf(prefs.getString(THEME, null)) }
        set(value) = write { putString(THEME, value) }

    var soundEnabled: Boolean
        get() = rev.intValue.let { prefs.getBoolean(SOUND, true) }
        set(value) = write { putBoolean(SOUND, value) }

    /** When the last update check started (epoch ms). No [rev] bump: nothing on screen reads it. */
    var updateCheckedAt: Long
        get() = prefs.getLong(UPD_AT, 0)
        set(value) = prefs.edit().putLong(UPD_AT, value).apply()

    /** "나중에": hides [version] for a day ([Updates.snoozed]). */
    fun snoozeUpdate(version: String) = prefs.edit().putString(UPD_SNOOZE, version).putLong(UPD_SNOOZE_AT, System.currentTimeMillis()).apply()

    fun updateSnoozed(version: String): Boolean =
        Updates.snoozed(version, prefs.getString(UPD_SNOOZE, null), prefs.getLong(UPD_SNOOZE_AT, 0), System.currentTimeMillis())

    fun recommendedLevel(): AiLevel = rating.let { r -> AiLevels.all.minBy { abs(it.approxElo - r) } }

    fun addHint() = write { putInt(HINTS, prefs.getInt(HINTS, 0) + 1) }

    /** Records one finished game ([score] 1 win, 0.5 draw, 0 loss) and returns the new rating. */
    fun recordGame(level: AiLevel, score: Double, qualities: Map<MoveQuality, Int>): Int {
        val old = rating
        val new = newRating(old, level.approxElo, score)
        val history = (ratingHistory + new).takeLast(HISTORY)
        val key = when (score) { 1.0 -> "w_"; 0.5 -> "d_"; else -> "l_" } + level.level
        write {
            putInt(RATING, new)
            putString(HISTORY_KEY, history.joinToString(","))
            putInt(key, prefs.getInt(key, 0) + 1)
            qualities.forEach { (q, n) -> putInt("q_${q.name}", prefs.getInt("q_${q.name}", 0) + n) }
        }
        return new
    }

    private inline fun write(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        rev.intValue++
    }

    companion object {
        const val START_RATING = 600
        private const val HISTORY = 30
        private const val RATING = "rating"
        private const val HISTORY_KEY = "history"
        private const val HINTS = "hints"
        private const val THEME = "theme"
        private const val SOUND = "sound"
        private const val UPD_AT = "upd_at"
        private const val UPD_SNOOZE = "upd_snooze"
        private const val UPD_SNOOZE_AT = "upd_snooze_at"

        private val OLD_BOARD_THEMES = mapOf("wood" to "walnut", "emerald" to "forest", "midnight" to "slate")

        /** Maps a stored id, including the retired board-theme ids, to an app theme id; null means the default. */
        fun themeIdOf(stored: String?): String = OLD_BOARD_THEMES[stored] ?: stored ?: "forest"

        /** Elo with K = 32, floored at 100. */
        fun newRating(rating: Int, opponent: Int, score: Double): Int {
            val expected = 1.0 / (1.0 + 10.0.pow((opponent - rating) / 400.0))
            return (rating + 32 * (score - expected)).roundToInt().coerceAtLeast(100)
        }
    }
}
