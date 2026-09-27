package com.seobuk.chess.baduk.learn

// 바둑 입문 코스의 모델. Kotlin stdlib만 써요 (core/ai를 import하지 않아요).
//
// Diagram format: one string per board row from TOP to bottom, cells separated by single spaces,
// always a full small board (5, 6, 7 or 9 lines).
//   '.' empty   'X' black   'O' white   'B' / 'W' black / white stone carrying the target mark
//   'a'..'w' except 'o' = an EMPTY point with that label (each label at most once per diagram)
// Answers and sequences refer to labels, never to coordinates.

enum class Side { BLACK, WHITE }

data class Diagram(
    val rows: List<String>,
    val caption: String? = null,
    /** Labels to shade as that side's 집, space separated. */
    val territoryBlack: String = "",
    val territoryWhite: String = "",
)

enum class Goal {
    CAPTURE, ATARI, ESCAPE, CONNECT, CUT, KILL, LIVE,
    /** Ladder or net: the marked stones cannot escape. */
    CATCH,
    KO,
    /** Judgement (opening, endgame, legality): not machine-checkable. */
    POINT,
}

sealed interface Step

data class Explain(
    val text: String,
    val diagram: Diagram? = null,
    /** Labels played in order, space separated; colours alternate starting with [first]. */
    val sequence: String = "",
    val first: Side = Side.BLACK,
    /** Optional caption shown after each move of the sequence (empty, or one per move). */
    val notes: List<String> = emptyList(),
) : Step

data class Problem(
    val prompt: String,
    val diagram: Diagram,
    val toPlay: Side,
    val goal: Goal,
    /** Correct first moves: labels, space separated, at least one. */
    val solutions: String,
    /** Shown when solved. */
    val success: String,
    /** Label -> why it fails. */
    val wrong: Map<String, String> = emptyMap(),
    /** Shown for any other wrong point. */
    val fallback: String,
    /** Labels played after the first correct move to show the result; starts with the OPPONENT. */
    val followUp: String = "",
    val hint: String = "",
) : Step

data class Quiz(
    val question: String,
    val choices: List<String>,
    val answer: Int,
    val explanation: String,
    val diagram: Diagram? = null,
) : Step

data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val minutes: Int,
    val steps: List<Step>,
)

data class Chapter(
    val id: String,
    val number: Int,
    val title: String,
    val summary: String,
    val lessons: List<Lesson>,
)

object BadukCourse {
    val chapters: List<Chapter> = courseChaptersA + courseChaptersB

    private val lessons: List<Lesson> = chapters.flatMap { it.lessons }

    val lessonCount: Int = lessons.size

    fun lesson(id: String): Lesson? = lessons.firstOrNull { it.id == id }

    /** Following lesson in course order; null after the last lesson or for an unknown id. */
    fun next(id: String): Lesson? {
        val i = lessons.indexOfFirst { it.id == id }
        return if (i < 0) null else lessons.getOrNull(i + 1)
    }
}
