package com.seobuk.chess.baduk.learn

import com.seobuk.chess.baduk.ai.Solver
import com.seobuk.chess.baduk.core.Board
import com.seobuk.chess.baduk.core.PASS
import com.seobuk.chess.baduk.core.Points
import com.seobuk.chess.baduk.core.Stone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the whole course against the real rules engine and the solver. */
class CourseContentTest {
    private class At(val lesson: Lesson, val index: Int, val step: Step) {
        override fun toString() = "${lesson.id} step ${index + 1}"
    }

    private val lessons = BadukCourse.chapters.flatMap { it.lessons }
    private val steps = lessons.flatMap { l -> l.steps.mapIndexed { i, s -> At(l, i, s) } }
    private val problems = steps.filter { it.step is Problem }

    private fun stone(s: Side) = if (s == Side.BLACK) Stone.BLACK else Stone.WHITE

    private fun diagram(step: Step): Diagram? = when (step) {
        is Explain -> step.diagram
        is Problem -> step.diagram
        is Quiz -> step.diagram
    }

    private fun check(errors: List<String>) = assertTrue(errors.joinToString("\n", "\n"), errors.isEmpty())

    @Test fun idsAreUniqueAndWellFormed() {
        assertEquals((1..9).toList(), BadukCourse.chapters.map { it.number })
        for (c in BadukCourse.chapters) {
            assertEquals("ch${c.number}", c.id)
            assertTrue(c.id, c.lessons.isNotEmpty() && c.title.isNotBlank() && c.summary.isNotBlank())
            c.lessons.forEachIndexed { i, l ->
                assertEquals("c${c.number}-${i + 1}", l.id)
                assertTrue(l.id, l.title.isNotBlank() && l.summary.isNotBlank() && l.minutes in 1..15)
            }
        }
        assertEquals(lessons.size, lessons.map { it.id }.toSet().size)
        assertEquals(lessons.size, BadukCourse.lessonCount)
        for ((i, l) in lessons.withIndex()) {
            assertEquals(l, BadukCourse.lesson(l.id))
            assertEquals(lessons.getOrNull(i + 1), BadukCourse.next(l.id))
        }
        assertNull(BadukCourse.lesson("nope"))
        assertNull(BadukCourse.next("nope"))
    }

    @Test fun everyDiagramParses() {
        val errors = ArrayList<String>()
        var count = 0
        for (at in steps) {
            val d = diagram(at.step) ?: continue
            count++
            try {
                Board.fromRows(d.rows)
                if (d.rows.size !in listOf(5, 6, 7, 9)) errors += "$at: board size ${d.rows.size}"
                val labels = Board.labels(d.rows)
                for (l in (d.territoryBlack + " " + d.territoryWhite).split(' ').filter { it.isNotEmpty() }) {
                    if (l.length != 1 || l[0] !in labels) errors += "$at: territory label '$l' is not in the diagram"
                }
            } catch (e: IllegalArgumentException) {
                errors += "$at: ${e.message}"
            }
        }
        assertTrue("diagrams: $count", count > 200)
        check(errors)
    }

    /** Plays [labels] in turn; returns the final board or null (and an error) if a move is illegal. */
    private fun play(b: Board, names: Map<Char, Int>, labels: List<String>, what: String, errors: MutableList<String>): Board? {
        for (l in labels) {
            val p = names[l.singleOrNull()]
            if (p == null) {
                errors += "$what: label '$l' is not in the diagram"
                return null
            }
            if (!b.play(p)) {
                errors += "$what: '$l' is illegal for ${b.toMove} (${b.illegalReason(p)})"
                return null
            }
        }
        return b
    }

    @Test fun sequencesAndFollowUpsAreLegal() {
        val errors = ArrayList<String>()
        for (at in steps) {
            val step = at.step
            if (step is Explain) {
                val moves = step.sequence.split(' ').filter { it.isNotEmpty() }
                if (moves.isEmpty()) {
                    if (step.notes.isNotEmpty()) errors += "$at: notes without a sequence"
                    continue
                }
                val d = step.diagram
                if (d == null) {
                    errors += "$at: sequence without a diagram"
                    continue
                }
                if (step.notes.isNotEmpty() && step.notes.size != moves.size) errors += "$at: ${step.notes.size} notes for ${moves.size} moves"
                play(Board.fromRows(d.rows, stone(step.first)), Board.labels(d.rows), moves, "$at sequence", errors)
            } else if (step is Problem) {
                val names = Board.labels(step.diagram.rows)
                val follow = step.followUp.split(' ').filter { it.isNotEmpty() }
                for (s in step.solutions.split(' ').filter { it.isNotEmpty() }) {
                    val b = Board.fromRows(step.diagram.rows, stone(step.toPlay))
                    play(b, names, listOf(s) + follow, "$at followUp after '$s'", errors)
                }
            }
        }
        check(errors)
    }

    private fun oneGroup(b: Board, marks: IntArray) =
        marks.all { b[it] != null } && marks.all { b.groupId(it) == b.groupId(marks[0]) }

    /** [side] to move: can it make the marked stones one group within [plies] own moves, whatever the opponent does? */
    private fun canConnect(b: Board, side: Stone, marks: IntArray, plies: Int): Boolean {
        if (marks.any { b[it] == null }) return false
        if (oneGroup(b, marks)) return true
        if (plies == 0) return false
        for (p in b.legalMoves()) {
            val c = b.copy()
            c.play(p)
            if (marks.any { c[it] == null }) continue
            if (oneGroup(c, marks)) return true
            if (plies > 1 && holdsConnection(c, side, marks, plies - 1)) return true
        }
        return false
    }

    /** The opponent of [side] to move: whatever it plays (or if it passes), [side] connects. */
    private fun holdsConnection(b: Board, side: Stone, marks: IntArray, plies: Int): Boolean {
        if (marks.any { b[it] == null }) return false
        for (q in b.legalMoves() + PASS) {
            val c = b.copy()
            c.play(q)
            if (!canConnect(c, side, marks, plies)) return false
        }
        return true
    }

    /** Does [p], played by the side to move on [b0], reach [goal]? null = the solver gave up. */
    private fun reaches(goal: Goal, b0: Board, marks: IntArray, p: Int, region: Set<Int>): Boolean? {
        val me = b0.toMove
        val b = b0.copy()
        if (!b.play(p)) return false
        val target = marks.minOrNull() ?: -1
        return when (goal) {
            Goal.CAPTURE -> marks.all { it in b.lastCaptured }
            Goal.ATARI -> marks.all { b[it] != null && b.liberties(it) == 1 } && (b.liberties(p) >= 2 || b.lastCaptureCount > 0)
            Goal.ESCAPE -> when {
                b[target] == null -> false
                b.liberties(target) < 3 && b.lastCaptureCount == 0 -> false
                else -> Solver.capturable(b, target)?.not()
            }
            Goal.CONNECT -> marks.all { b[it] != null } && (oneGroup(b, marks) || holdsConnection(b, me, marks, 1))
            Goal.CUT -> marks.any { b[it] == null } || !canConnect(b, me.opposite, marks, 2)
            Goal.KO -> b.lastCaptureCount == 1 && b.koPoint != -1
            Goal.CATCH -> if (b[target] == null) true else Solver.capturable(b, target)
            Goal.KILL -> if (b[target] == null) true else Solver.capturable(b, target, region + Solver.eyeRegion(b, target))
            Goal.LIVE -> if (b[target] == null) false else Solver.capturable(b, target, region + Solver.eyeRegion(b, target))?.not()
            Goal.POINT -> null
        }
    }

    @Test fun problemsReachTheirGoal() {
        val errors = ArrayList<String>()
        val perGoal = HashMap<Goal, Int>()
        for (at in problems) {
            val step = at.step as Problem
            perGoal.merge(step.goal, 1, Int::plus)
            val rows = step.diagram.rows
            val names = Board.labels(rows)
            val me = stone(step.toPlay)
            val b = Board.fromRows(rows, me)
            val size = b.size
            val solutions = step.solutions.split(' ').filter { it.isNotEmpty() }
            fun tag(p: Int): String = Points.name(p, size) + (names.entries.firstOrNull { it.value == p }?.let { " (label ${it.key})" } ?: "")

            if (solutions.isEmpty() || solutions.toSet().size != solutions.size) errors += "$at: bad solutions \"${step.solutions}\""
            if (step.prompt.isBlank() || step.success.isBlank() || step.fallback.isBlank()) errors += "$at: empty text"
            for (l in solutions + step.wrong.keys) if (l.length != 1 || l[0] !in names) errors += "$at: label '$l' is not in the diagram"
            for (l in step.wrong.keys) if (l in solutions) errors += "$at: '$l' is both right and wrong"
            if (errors.any { it.startsWith("$at:") }) continue
            for (l in solutions) if (!b.isLegal(names.getValue(l[0]))) errors += "$at: solution '$l' is illegal (${b.illegalReason(names.getValue(l[0]))})"
            if (step.goal == Goal.POINT) continue

            val marks = Board.marked(rows)
            if (marks.isEmpty()) {
                if (step.goal != Goal.KO) {
                    errors += "$at: ${step.goal} needs marked stones"
                    continue
                }
            } else {
                val own = step.goal in listOf(Goal.ESCAPE, Goal.CONNECT, Goal.LIVE)
                if (marks.any { b[it] != if (own) me else me.opposite }) {
                    errors += "$at: ${step.goal} marks the wrong colour"
                    continue
                }
                val groups = marks.map { b.groupId(it) }.toSet().size
                when (step.goal) {
                    Goal.CONNECT, Goal.CUT -> if (groups < 2) errors += "$at: ${step.goal} needs marks on two or more groups"
                    Goal.CAPTURE, Goal.ATARI -> {}
                    else -> if (groups != 1) errors += "$at: ${step.goal} needs marks on one group"
                }
            }
            val target = marks.minOrNull() ?: -1
            var region = emptySet<Int>()
            var tries = b.legalMoves().toList()
            when (step.goal) {
                Goal.ESCAPE -> if (b.liberties(target) != 1) errors += "$at: the marked stones are not in atari"
                Goal.ATARI -> if (marks.any { b.liberties(it) < 2 }) errors += "$at: a marked group is in atari already"
                Goal.CATCH -> {
                    if (size > 9) errors += "$at: board too large"
                    if (Solver.capturable(b.copy().also { it.setToMove(me.opposite) }, target) != false) errors += "$at: caught already, even if the defender moves first"
                }
                Goal.KILL, Goal.LIVE -> {
                    region = Solver.eyeRegion(b, target)
                    tries = tries.filter { it in region }
                    if (size > 7) errors += "$at: board too large"
                    if (b.emptyCount > 12) errors += "$at: ${b.emptyCount} empty points"
                    val attackerFirst = b.copy().also { it.setToMove(b[target]!!.opposite) }
                    val defenderFirst = b.copy().also { it.setToMove(b[target]!!) }
                    if (step.goal == Goal.LIVE && Solver.capturable(attackerFirst, target, region) != true) errors += "$at: alive already"
                    if (step.goal == Goal.KILL && Solver.capturable(defenderFirst, target, region) != false) errors += "$at: dead already"
                }
                else -> {}
            }
            val right = solutions.map { names.getValue(it[0]) }.toSet()
            for (p in tries) {
                val r = reaches(step.goal, b, marks, p, region)
                if (r == null) errors += "$at: the solver gave up at ${tag(p)}"
                else if (p in right && !r) errors += "$at: ${step.goal} solution ${tag(p)} does not reach the goal"
                else if (p !in right && r) errors += "$at: ${step.goal} ${tag(p)} reaches the goal too but is not a solution"
            }
            // Listed wrong answers that are illegal moves are wrong by definition; legal ones were tried above.
            for (l in step.wrong.keys) {
                val p = names.getValue(l[0])
                if (b.isLegal(p) && p !in tries && reaches(step.goal, b, marks, p, region) != false) errors += "$at: wrong answer ${tag(p)} reaches the goal"
            }
        }
        println("course problems by goal: ${perGoal.toSortedMap()}")
        check(errors)
    }

    @Test fun quizAnswersAreInRange() {
        for (at in steps) {
            val q = at.step as? Quiz ?: continue
            assertTrue("$at", q.choices.size in 2..4 && q.answer in q.choices.indices)
            assertEquals("$at", q.choices.size, q.choices.toSet().size)
            assertTrue("$at", q.question.isNotBlank() && q.explanation.isNotBlank() && q.choices.none { it.isBlank() })
        }
    }

    @Test fun sizeIsWithinTheTarget() {
        assertTrue("lessons ${lessons.size}", lessons.size in 32..44)
        for (l in lessons) {
            assertTrue("${l.id}: ${l.steps.size} steps", l.steps.size in 4..8)
            // The last lesson closes with the invitation to a first game, after its two exercises.
            val steps = if (l == lessons.last() && l.steps.last() is Explain) l.steps.dropLast(1) else l.steps
            assertTrue("${l.id} must end with two exercises", steps.takeLast(2).none { it is Explain })
        }
        val exercises = steps.count { it.step !is Explain }
        println("course: ${BadukCourse.chapters.size} chapters, ${lessons.size} lessons, ${steps.size} steps, " +
            "${problems.size} problems, ${exercises - problems.size} quizzes")
        assertTrue("exercises $exercises", exercises in 110..140)
    }

    private fun strings(): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        for (c in BadukCourse.chapters) {
            out += c.id to c.title
            out += c.id to c.summary
            for (l in c.lessons) {
                out += l.id to l.title
                out += l.id to l.summary
            }
        }
        for (at in steps) {
            val where = at.toString()
            diagram(at.step)?.caption?.let { out += where to it }
            when (val s = at.step) {
                is Explain -> (listOf(s.text) + s.notes).forEach { out += where to it }
                is Problem -> (listOf(s.prompt, s.success, s.fallback, s.hint) + s.wrong.values).forEach { out += where to it }
                is Quiz -> (listOf(s.question, s.explanation) + s.choices).forEach { out += where to it }
            }
        }
        return out
    }

    @Test fun copyFollowsTheHouseRules() {
        val errors = ArrayList<String>()
        for ((where, text) in strings()) {
            for (line in text.split('\n')) {
                val bad = line.filter { it in "—–‒―!！" }
                if (bad.isNotEmpty()) errors += "$where: '$bad' in \"$line\""
                if (line.count { it == '·' || it == 'ㆍ' || it == '・' } > 1) errors += "$where: two middle dots in \"$line\""
            }
            if (text != text.trim() || "  " in text) errors += "$where: stray whitespace in \"$text\""
            val odd = text.filter { it !in '가'..'힣' && it !in ' '..'~' && it != '\n' && it != '·' }
            if (odd.isNotEmpty()) errors += "$where: '$odd' in \"$text\""
            if ('$' in text || '\\' in text) errors += "$where: template leftovers in \"$text\""
        }
        check(errors)
    }

    @Test fun lastLessonInvitesToAFirstGame() {
        val last = lessons.last()
        assertEquals("c9-4", last.id)
        assertTrue(strings().filter { it.first.startsWith("c9-4") }.any { "9줄" in it.second })
    }
}
