package com.seobuk.chess.ui.sound

import android.content.Context
import android.content.pm.ApplicationInfo
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import com.seobuk.chess.R
import com.seobuk.chess.core.Move
import com.seobuk.chess.core.PieceType
import com.seobuk.chess.core.Position
import com.seobuk.chess.core.Squares
import com.seobuk.chess.data.ProgressStore
import kotlin.math.abs
import kotlin.random.Random

enum class SfxEvent { MOVE, CAPTURE, CHECK, CASTLE, PROMOTE, ILLEGAL, WIN, LOSS, DRAW }

/** The cue for [move] played from [before], giving [after]. Pure (no Android), so the JVM tests cover it. */
fun sfxFor(before: Position, move: Move, after: Position): SfxEvent = when {
    after.isInCheck() -> SfxEvent.CHECK
    move.promotion != null -> SfxEvent.PROMOTE
    before.pieceAt(move.from)?.type == PieceType.KING && abs(Squares.file(move.to) - Squares.file(move.from)) == 2 -> SfxEvent.CASTLE
    before.isCapture(move) -> SfxEvent.CAPTURE
    else -> SfxEvent.MOVE
}

val LocalSfx = staticCompositionLocalOf<Sfx> { error("Sfx not provided") }

/**
 * The app's sound cues: all nine loaded once into a SoundPool. [play] is a no-op while sound is off in the
 * settings or until that cue has finished loading, and allocates nothing.
 */
class Sfx(context: Context, private val store: ProgressStore) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = IntArray(SfxEvent.entries.size)
    private val loaded = BooleanArray(SfxEvent.entries.size) // main thread only: the pool posts to the creating looper

    init {
        val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        pool.setOnLoadCompleteListener { _, id, status ->
            val i = ids.indexOf(id)
            if (i >= 0 && status == 0) loaded[i] = true
            if (debug) Log.d("Sfx", "load ${SfxEvent.entries.getOrNull(i)} id=$id status=$status")
        }
        for (e in SfxEvent.entries) ids[e.ordinal] = pool.load(context, res(e), 1)
    }

    fun play(event: SfxEvent) {
        val i = event.ordinal
        if (!loaded[i] || !store.soundEnabled) return
        // A touch of pitch variation keeps repeated piece sounds from feeling mechanical.
        val rate = when (event) {
            SfxEvent.MOVE, SfxEvent.CAPTURE, SfxEvent.CASTLE -> 0.97f + Random.nextFloat() * 0.06f
            else -> 1f
        }
        pool.play(ids[i], 1f, 1f, 1, 0, rate)
    }

    fun release() = pool.release()

    private fun res(e: SfxEvent) = when (e) {
        SfxEvent.MOVE -> R.raw.sfx_move
        SfxEvent.CAPTURE -> R.raw.sfx_capture
        SfxEvent.CHECK -> R.raw.sfx_check
        SfxEvent.CASTLE -> R.raw.sfx_castle
        SfxEvent.PROMOTE -> R.raw.sfx_promote
        SfxEvent.ILLEGAL -> R.raw.sfx_illegal
        SfxEvent.WIN -> R.raw.sfx_win
        SfxEvent.LOSS -> R.raw.sfx_loss
        SfxEvent.DRAW -> R.raw.sfx_draw
    }
}
