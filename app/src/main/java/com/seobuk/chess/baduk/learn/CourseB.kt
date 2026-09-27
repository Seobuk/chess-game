package com.seobuk.chess.baduk.learn

// 바둑 입문 코스 5~9장: 돌을 잡는 기술, 삶과 죽음, 집 짓기와 포석, 끝내기와 계가, 첫 대국.
//
// Every diagram, sequence and machine-checkable problem in this file is verified by
// verify_b/check_b.py (rules and readers in verify_b/go.py). Run it again after any change.
// Keep the layout regular (named arguments, one row string per line, plain string literals)
// so that the checker can keep parsing the file.
// Lines starting with "// verify:" are extra assertions for the step that follows them
// (quiz answers, refutations named in the texts, territory counts).
//
// Assumptions behind the machine-checked goals:
//  - KILL / LIVE: both sides only play inside the eye region of the marked group: the points reached
//    from it through empty points and its own stones, plus attacker stones whose liberties all lie
//    inside. The enclosing wall always has two one-point eyes outside that region, so a reader that
//    keeps to the region needs a few hundred nodes at most. A reader without a region would let
//    the attacker fill the eyes of its own wall and explode.
//  - CATCH: ladders, nets and snapbacks; the marked stones count as escaped once they have
//    three liberties on the attacker's turn.
//  - POINT problems in chapters 8 and 9 carry their own "// verify:" assertions.
//
// One function per lesson keeps every method far below the JVM method size limit.

internal val courseChaptersB: List<Chapter> = listOf(
    chapter5(),
    chapter6(),
    chapter7(),
    chapter8(),
    chapter9(),
)

// ─────────────────────────────── 5장 돌을 잡는 기술 ───────────────────────────────

private fun chapter5() = Chapter(
    id = "ch5",
    number = 5,
    title = "돌을 잡는 기술",
    summary = "단수만으로는 잡히지 않는 돌을 잡는 네 가지 기술을 배워요.",
    lessons = listOf(lesson5x1(), lesson5x2(), lesson5x3(), lesson5x4(), lesson5x5()),
)

private fun lesson5x1() = Lesson(
    id = "c5-1",
    title = "축, 계단처럼 몰아 잡기",
    summary = "단수를 이어 가며 상대 돌을 판 끝까지 모는 축을 배워요.",
    minutes = 5,
    steps = listOf(
        // verify: libs 2
        Explain(
            text = "표시된 백돌의 활로는 a와 b 둘이에요. 흑이 한쪽을 막아 단수를 치면 백은 남은 쪽으로 달아나요. 그런데 달아난 뒤에도 활로가 둘뿐이라면 또 단수를 칠 수 있어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". X b . . . .",
                    ". X W a . . .",
                    ". . X . . . .",
                    ". . . . . . .",
                ),
                caption = "백돌의 활로는 a와 b예요.",
            ),
        ),
        // verify: seqcaptures black a,b,c,d,e,f,g,h,i,j,k,l,m,n,p 8
        Explain(
            text = "단수를 이어 가며 상대를 모는 기술을 축이라고 해요. 한 수씩 넘기며 따라가 보세요. 백이 달아날 때마다 흑이 앞을 막아서 계단 모양이 돼요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . m l n p",
                    ". . . g j k .",
                    ". . c f h i .",
                    ". X b d e . .",
                    ". X W a . . .",
                    ". . X . . . .",
                    ". . . . . . .",
                ),
            ),
            sequence = "a b c d e f g h i j k l m n p",
            first = Side.BLACK,
            notes = listOf(
                "흑이 단수를 쳤어요. 백의 활로는 하나예요.",
                "백이 늘어서 달아나요. 활로는 다시 둘이에요.",
                "흑이 앞을 막으며 또 단수를 쳐요.",
                "백이 옆으로 꺾어 달아나요.",
                "흑이 다시 단수를 쳐요. 계단 모양이 보이나요?",
                "백은 달아나도 활로가 늘 둘뿐이에요.",
                "흑은 쉬지 않고 단수를 쳐요.",
                "백이 또 달아나요.",
                "흑이 또 앞을 막아요.",
                "백돌이 점점 길어져요.",
                "판 끝이 가까워졌어요.",
                "백이 맨 윗줄에 닿았어요. 더 올라갈 곳이 없어요.",
                "흑이 단수를 쳐요.",
                "백이 마지막으로 늘어 봐요. 활로는 하나뿐이에요.",
                "흑이 따냈어요. 달아난 돌까지 모두 잡혔어요.",
            ),
        ),
        // verify: seqlibs black b,a 3
        Explain(
            text = "방향이 중요해요. 흑이 b로 단수 치면 백은 a로 늘어요. 이쪽에는 막아 줄 흑돌이 없어서 백의 활로가 셋이 돼요. 활로가 셋이면 다음 단수가 없어요. 내 돌이 기다리는 쪽으로 몰아야 해요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". X b . . . .",
                    ". X W a . . .",
                    ". . X . . . .",
                    ". . . . . . .",
                ),
            ),
            sequence = "b a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 위에서 단수를 쳤어요.",
                "백이 늘자 활로가 셋이 됐어요. 더는 몰 수 없어요.",
            ),
        ),
        Quiz(
            question = "축에 걸린 돌이 계속 달아나면 어떻게 될까요?",
            choices = listOf(
                "잡히는 돌만 점점 늘어나요",
                "끝까지 달아나면 살아요",
                "아무 차이가 없어요",
            ),
            answer = 0,
            explanation = "축에 걸린 돌은 달아날수록 길어지고, 판 끝에서 한꺼번에 잡혀요. 축에 걸렸다면 그 돌은 내버려 두고 다른 곳에 두는 편이 좋아요.",
        ),
        // verify: after black b,a libs 3
        Problem(
            prompt = "표시된 백돌 둘을 축으로 잡으세요. 어느 쪽에서 단수를 칠까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . X X . .",
                    ". . X W W a .",
                    ". . . X b c .",
                    ". . . e d f g",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "맞아요. 백은 아래로 달아나지만 가는 곳마다 흑이 막아서 판 끝에서 잡혀요.",
            wrong = mapOf(
                "b" to "b로 단수 치면 백이 a로 늘어요. 오른쪽은 비어 있어서 활로가 셋이 되고, 더는 몰 수 없어요.",
            ),
            fallback = "백돌 둘의 활로는 a와 b 둘이에요. 단수가 되려면 그중 한 곳에 둬야 해요.",
            followUp = "b c d e f g",
            hint = "백이 달아날 쪽에 내 돌이 기다리고 있어야 해요. 아래쪽에 흑돌이 있어요.",
        ),
        // verify: after black b,a libs 3
        Problem(
            prompt = "백돌 하나를 축으로 잡으세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . X . .",
                    ". . . a W X .",
                    ". . e d b X .",
                    ". . g f c . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "좋아요. 백이 아래로 달아나도 오른쪽 흑돌이 벽이 되어 줘요. 곧 판 끝이에요.",
            wrong = mapOf(
                "b" to "b로 단수 치면 백이 a로 늘어요. 왼쪽은 넓게 비어 있어서 활로가 셋이 돼요.",
            ),
            fallback = "단수가 되는 자리는 a와 b 두 곳뿐이에요. 어느 쪽으로 몰지 골라 보세요.",
            followUp = "b c d e f g",
            hint = "흑돌이 많은 쪽으로 백을 몰아요.",
        ),
        // verify: after black b,a libs 3
        Problem(
            prompt = "축을 몰던 중이에요. 백이 방금 달아났어요. 다음 단수는 어디일까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . i k",
                    ". . . . e h j",
                    ". . . a d f g",
                    ". . X W b c .",
                    ". X W W X . .",
                    ". . X X . . .",
                    ". . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "맞아요. 계단을 이어 가면 백은 오른쪽 위 구석에서 잡혀요.",
            wrong = mapOf(
                "b" to "b로 막으면 백이 a로 올라가요. 위쪽이 넓어서 활로가 셋이 되고 축이 끊겨요.",
            ),
            fallback = "백의 활로는 a와 b예요. 축은 단수를 이어 가야 해요.",
            followUp = "b c d e f g h i j k",
            hint = "지금까지 계단이 어느 쪽으로 올라왔는지 보세요. 같은 모양을 이어 가요.",
        ),
    ),
)

private fun lesson5x2() = Lesson(
    id = "c5-2",
    title = "축머리, 축을 깨는 돌",
    summary = "축이 가는 길에 상대 돌이 있으면 축은 실패해요. 몰기 전에 길을 읽어요.",
    minutes = 6,
    steps = listOf(
        // verify: seqlibs black a,b,c,d,e,f,g,h 3
        Explain(
            text = "축이 가는 길에 백돌이 미리 놓여 있으면 어떻게 될까요? 표시된 백돌을 보면서 한 수씩 따라가 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . g W . .",
                    ". . c f h . .",
                    ". X b d e . .",
                    ". X O a . . .",
                    ". . X . . . .",
                    ". . . . . . .",
                ),
            ),
            sequence = "a b c d e f g h",
            first = Side.BLACK,
            notes = listOf(
                "흑이 단수를 치며 축을 시작해요.",
                "백이 달아나요.",
                "흑이 또 단수를 쳐요.",
                "백이 달아나요.",
                "여기까지는 보통 축과 같아요.",
                "백이 표시된 백돌 쪽으로 다가가요.",
                "흑이 마지막 단수를 쳐요.",
                "백이 표시된 백돌과 이어졌어요. 활로가 셋이라 더는 단수가 안 돼요.",
            ),
        ),
        // verify: ataris white a 2
        // verify: ataris white b 2
        Explain(
            text = "축을 깨는 돌을 축머리라고 해요. 축이 실패하면 몬 쪽이 크게 다쳐요. 단수 치느라 놓은 흑돌들이 띄엄띄엄 떨어져 있기 때문이에요. 백이 a나 b에 두면 양단수예요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . a X O . .",
                    ". . X O O . .",
                    ". X O O X . .",
                    ". X O X b . .",
                    ". . X . . . .",
                    ". . . . . . .",
                ),
                caption = "축이 실패한 뒤의 모양이에요.",
            ),
            sequence = "a",
            first = Side.WHITE,
            notes = listOf(
                "흑 두 곳이 한꺼번에 단수예요. 하나를 살리면 다른 하나가 잡혀요.",
            ),
        ),
        // verify: ladder yes target=E5
        // verify: after white a ladder no target=E5
        // verify: after white b ladder no target=E5
        Explain(
            text = "그래서 축을 몰기 전에 길을 눈으로 먼저 따라가 봐요. 축은 대각선으로 뻗어 가요. a나 b처럼 그 길 위에 백돌이 있었다면 축머리가 돼요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . b . .",
                    ". . . . . a . . .",
                    ". . . X . . . . .",
                    ". . X O O . . . .",
                    ". X O O X . . . .",
                    ". X O X . . . . .",
                    ". . X . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "축은 오른쪽 위로 대각선을 따라 올라가요.",
            ),
        ),
        // verify: catch a no target=C3
        // verify: answer 성립하지 않아요
        Quiz(
            question = "흑이 a로 단수 쳐서 축으로 몰려고 해요. 오른쪽 위에 표시된 백돌이 있어요. 이 축은 성립할까요?",
            choices = listOf(
                "성립해요. 백을 잡을 수 있어요",
                "성립하지 않아요. 표시된 백돌이 축머리예요",
            ),
            answer = 1,
            explanation = "축이 올라가는 대각선 길 위에 표시된 백돌이 있어요. 백이 그 돌과 이어지면 활로가 늘어서 잡을 수 없어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . W . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". X . . . . . . .",
                    ". X O a . . . . .",
                    ". . X . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
        ),
        // verify: catch a yes target=C3
        // verify: answer 성립해요
        Quiz(
            question = "이번에는 표시된 백돌이 조금 다른 곳에 있어요. 흑이 a로 몰면 축은 성립할까요?",
            choices = listOf(
                "성립해요. 표시된 백돌은 길에서 벗어나 있어요",
                "성립하지 않아요. 표시된 백돌이 축머리예요",
            ),
            answer = 0,
            explanation = "축은 표시된 백돌의 왼쪽을 지나 위로 올라가요. 표시된 백돌은 그 길에 닿지 않아서 백을 도와주지 못해요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . W .",
                    ". X . . . . . . .",
                    ". X O a . . . . .",
                    ". . X . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
        ),
        // verify: after black b,a ladder no
        Problem(
            prompt = "표시된 백돌 하나를 축으로 잡으세요. 오른쪽 위에 백돌이 있으니 방향을 잘 골라야 해요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . O .",
                    ". . . X . . .",
                    ". . X W a . .",
                    ". e d b X . .",
                    ". g f c . . .",
                    ". i h j k . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "맞아요. 축머리가 없는 왼쪽 아래로 몰았어요. 백은 판 끝에서 잡혀요.",
            wrong = mapOf(
                "b" to "b로 몰면 백이 a로 나와 오른쪽 위로 달아나요. 그 길에 백돌이 기다리고 있어서 축이 깨져요.",
            ),
            fallback = "백의 활로는 a와 b예요. 둘 중 축머리가 없는 쪽으로 모는 단수를 골라 보세요.",
            followUp = "b c d e f g h i j k",
            hint = "a에 두면 백은 아래로, b에 두면 백은 오른쪽 위로 달아나요.",
        ),
        // verify: libs 1
        // verify: escape a yes
        // verify: answer 달아나도 돼요
        Quiz(
            question = "이번에는 흑이 몰리는 쪽이에요. 백이 표시된 흑돌에 단수를 쳤어요. 흑은 a로 달아나도 될까요?",
            choices = listOf(
                "달아나도 돼요. 오른쪽 위 흑돌이 축머리예요",
                "안 돼요. 축에 걸려서 더 크게 잡혀요",
            ),
            answer = 0,
            explanation = "백이 축으로 몰아도 흑은 오른쪽 위의 흑돌과 이어져요. 축머리는 내 돌을 지킬 때도 똑같이 쓸모가 있어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . X . .",
                    ". . . . . . .",
                    ". O a . . . .",
                    ". O B O . . .",
                    ". . O . . . .",
                    ". . . . . . .",
                ),
            ),
        ),
    ),
)

private fun lesson5x3() = Lesson(
    id = "c5-3",
    title = "장문, 그물로 가두기",
    summary = "단수를 치지 않고 달아날 길목을 막아 잡는 장문을 배워요.",
    minutes = 5,
    steps = listOf(
        // verify: seqlibs black a,b,c,d,e,f,g,h 3 target=D5
        Explain(
            text = "가운데 백돌 하나를 잡고 싶어요. 그런데 표시된 백돌이 축머리예요. 축으로 몰면 어떻게 되는지 먼저 볼까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . X X . . . .",
                    ". . X O b c . . .",
                    ". . X a d f g . .",
                    ". . . . e h W . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            sequence = "a b c d e f g h",
            first = Side.BLACK,
            notes = listOf(
                "흑이 단수를 쳐요.",
                "백이 달아나요.",
                "흑이 또 단수를 쳐요.",
                "백이 달아나요.",
                "흑이 계속 몰아요.",
                "백이 표시된 백돌 쪽으로 가요.",
                "흑의 마지막 단수예요.",
                "백이 표시된 백돌과 이어졌어요. 축은 실패예요.",
            ),
        ),
        // verify: seqcaptures black a,b,c,d,e 3
        Explain(
            text = "이럴 때 쓰는 기술이 장문이에요. 단수를 치지 않고, 백이 달아날 길목에 한 발 떨어져서 둬요. 그물을 치는 것과 같아요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . X X . . . .",
                    ". . X W b c . . .",
                    ". . X d a . . . .",
                    ". . . e . . O . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            sequence = "a b c d e",
            first = Side.BLACK,
            notes = listOf(
                "흑이 대각선 자리에 그물을 쳤어요. 단수는 아니에요.",
                "백이 오른쪽으로 나가 봐요.",
                "흑이 막아요. 백돌 둘이 단수예요.",
                "백이 아래로 나가 봐도 활로는 하나뿐이에요.",
                "흑이 따냈어요. 백은 어느 쪽으로도 나갈 수 없었어요.",
            ),
        ),
        Explain(
            text = "장문의 자리는 잡을 돌에서 대각선으로 한 칸 떨어진 곳이에요. 백이 나갈 수 있는 두 길을 한 수로 함께 지켜봐요. 장문은 멀리 있는 축머리를 걱정하지 않아도 돼요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . X X . . .",
                    ". X W b . . .",
                    ". X c a . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                ),
                caption = "a가 장문이에요. 백이 b나 c로 나오면 그때 막아요.",
            ),
        ),
        // verify: after black b,c ladder no
        // verify: after black c,b ladder no
        Problem(
            prompt = "표시된 백돌 하나를 잡으세요. 아래쪽 백돌이 축머리라서 축은 안 돼요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . X X . . .",
                    ". X W b d . .",
                    ". X c a . . .",
                    ". . e . O . .",
                    ". . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "맞아요. 장문이에요. 백이 어느 쪽으로 나와도 막으면 잡혀요.",
            wrong = mapOf(
                "b" to "b로 단수 치면 백이 c로 달아나요. 축으로 몰아도 아래쪽 백돌과 이어져서 잡을 수 없어요.",
                "c" to "c로 단수 치면 백이 b로 달아나요. 축으로 몰아도 아래쪽 백돌과 이어져서 잡을 수 없어요.",
            ),
            fallback = "그 자리는 백이 나갈 길을 막지 못해요. 백의 활로 둘을 한 번에 지켜보는 자리를 찾아보세요.",
            followUp = "b d c e",
            hint = "백돌에서 대각선으로 한 칸 떨어진 자리예요.",
        ),
        // verify: after black b,d ladder no
        // verify: after black d,b ladder no
        Problem(
            prompt = "표시된 백돌 둘을 잡으세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . X X X . . .",
                    ". . X W W b c . .",
                    ". . X X d a . . .",
                    ". . . . e . . . .",
                    ". . . . . . O . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "좋아요. 돌이 둘이어도 장문은 같아요. 나갈 길목을 한 발 앞에서 막았어요.",
            wrong = mapOf(
                "b" to "b로 단수 치면 백이 d로 달아나요. 계속 몰아도 오른쪽 아래 백돌이 축머리라서 놓쳐요.",
                "d" to "d로 단수 치면 백이 b로 달아나요. 계속 몰아도 오른쪽 아래 백돌이 축머리라서 놓쳐요.",
            ),
            fallback = "그 자리는 백의 길목을 막지 못해요. 백이 나갈 두 곳을 함께 지켜보는 자리를 찾아보세요.",
            followUp = "b c d e",
            hint = "백의 활로 두 곳에서 모두 한 칸 떨어진 자리예요.",
        ),
        // verify: after black b,d ladder no
        // verify: after black d,b ladder no
        Problem(
            prompt = "표시된 백돌 하나를 잡으세요. 왼쪽 위에 백돌이 있어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . O . e . .",
                    ". . . a d X .",
                    ". . c b W X .",
                    ". . . X X . .",
                    ". . . . . . .",
                    ". . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "맞아요. 방향이 바뀌어도 장문의 자리는 대각선으로 한 칸 떨어진 곳이에요.",
            wrong = mapOf(
                "b" to "b로 단수 치면 백이 d로 달아나요. 축으로 몰아도 왼쪽 위 백돌이 축머리라서 잡을 수 없어요.",
                "d" to "d로 단수 치면 백이 b로 달아나요. 축으로 몰아도 왼쪽 위 백돌이 축머리라서 잡을 수 없어요.",
            ),
            fallback = "그 자리는 백이 나갈 길을 막지 못해요. 백의 활로 둘을 한 번에 지켜보는 자리를 찾아보세요.",
            followUp = "b c d e",
            hint = "백의 활로는 b와 d예요. 두 곳과 모두 맞닿은 빈 자리를 찾아요.",
        ),
    ),
)

private fun lesson5x4() = Lesson(
    id = "c5-4",
    title = "환격, 주고 나서 크게 잡기",
    summary = "돌 하나를 일부러 잡혀 주고 더 많은 돌을 되잡는 환격을 배워요.",
    minutes = 5,
    steps = listOf(
        // verify: seqcaptures black a,b,a 3
        Explain(
            text = "표시된 백돌 둘의 활로는 a와 b예요. 흑이 a에 두면 그 돌은 바로 잡혀요. 그래도 일부러 둬요. 어떻게 되는지 따라가 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . X X . .",
                    "O O X X X X .",
                    ". O X W W X .",
                    ". O O a b X .",
                ),
            ),
            sequence = "a b a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 백 품 안에 돌을 넣었어요. 이 돌의 활로는 b 하나예요.",
                "백이 b에 두어 흑돌 하나를 따냈어요. 그런데 백돌 셋의 활로가 a 하나만 남았어요.",
                "흑이 다시 a에 두어 백돌 셋을 따내요. 돌 하나를 주고 셋을 잡았어요.",
            ),
        ),
        Explain(
            text = "이 기술을 환격이라고 해요. 패와 비슷해 보이지만 달라요. 패는 돌 하나를 따낸 자리를 바로 되따낼 수 없다는 규칙이었어요. 환격은 돌 여럿을 한꺼번에 따내니까 패가 아니에요. 그래서 바로 둘 수 있어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . X X . .",
                    "O O X X X X .",
                    ". O X W W X .",
                    ". O O a b X .",
                ),
                caption = "흑이 a에 넣고 백이 b로 따내면, 흑은 곧바로 a에 다시 둘 수 있어요.",
            ),
        ),
        // verify: seqlibs black b,a 4
        Explain(
            text = "밖에서 단수를 치면 어떨까요? 흑이 b에 두면 백은 a에 이어요. 왼쪽 백돌과 한 몸이 되어서 잡을 수 없어요. 상대가 이을 자리에 내가 먼저 들어가는 것이 환격의 핵심이에요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . X X . .",
                    "O O X X X X .",
                    ". O X W W X .",
                    ". O O a b X .",
                ),
            ),
            sequence = "b a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 밖에서 단수를 쳤어요.",
                "백이 이었어요. 활로가 넉넉해져서 잡을 수 없어요.",
            ),
        ),
        // verify: after black b captures white a 1
        // verify: after black b,a ladder no
        Problem(
            prompt = "귀에 있는 표시된 백돌 둘을 잡으세요.",
            diagram = Diagram(
                rows = listOf(
                    "b a O O . . .",
                    "W W X O . . .",
                    "X X X O . . .",
                    ". . X O . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "맞아요. 백이 b로 따내도 활로가 a 하나라서 흑이 되따내요. 환격이에요.",
            wrong = mapOf(
                "b" to "b에 두면 흑돌의 활로가 a 하나예요. 백이 a에 두면 흑돌 하나를 따내면서 오른쪽 백과 이어져요.",
            ),
            fallback = "백돌 둘의 활로는 a와 b예요. 그중 백이 이으려는 자리를 찾아보세요.",
            followUp = "b a",
            hint = "백돌 둘이 오른쪽 백돌과 이어지는 자리는 어디일까요?",
        ),
        // verify: after black b,a ladder no
        Problem(
            prompt = "표시된 백돌 넷을 잡으세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . X X X . .",
                    ". X W W W X .",
                    ". X W a b X .",
                    ". X X O X X .",
                    ". . O O O . .",
                    ". . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "좋아요. 백이 b로 따내면 백돌 다섯의 활로가 a 하나예요. 흑이 다시 a에 두어 모두 따내요.",
            wrong = mapOf(
                "b" to "b로 밖에서 단수 치면 백이 a에 이어요. 아래 백돌과 한 몸이 되어 활로가 넉넉해져요.",
            ),
            fallback = "백돌 넷의 활로는 a와 b예요. 그중 백이 이으려는 자리를 찾아보세요.",
            followUp = "b a",
            hint = "아래쪽 백돌과 표시된 백돌이 만나는 자리를 보세요.",
        ),
        // verify: after black b,a ladder no
        Problem(
            prompt = "오른쪽 변의 표시된 백돌 둘을 잡으세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . . . X X X",
                    ". . . X X W b",
                    ". . . X X W a",
                    ". . . . X X O",
                    ". . . . O O O",
                    ". . . . O . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "맞아요. 백이 b로 따내도 활로가 a 하나뿐이라 흑이 되따내요.",
            wrong = mapOf(
                "b" to "b로 단수 치면 백이 a에 이어요. 아래쪽 백돌과 한 몸이 되어 잡을 수 없어요.",
            ),
            fallback = "백돌 둘의 활로는 a와 b예요. 그중 백이 이으려는 자리를 찾아보세요.",
            followUp = "b a",
            hint = "백돌 둘이 아래쪽 백돌과 이어지는 자리를 찾아요.",
        ),
    ),
)

private fun lesson5x5() = Lesson(
    id = "c5-5",
    title = "먹여치기 맛보기",
    summary = "잡힐 자리에 일부러 돌을 넣어 상대의 활로를 줄이는 먹여치기를 알아봐요.",
    minutes = 4,
    steps = listOf(
        // verify: libs 2
        // verify: seqcaptures black a,b,a 11
        Explain(
            text = "표시된 백돌의 활로는 a와 b 둘뿐이에요. 둘 다 백 품 안이라 흑이 들어가면 바로 잡힐 것 같죠. 그래도 넣어 봐요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    "X X X X X X .",
                    "X W W W W X .",
                    "X W a b W X .",
                    "X W W W W X .",
                    "X X X X X X .",
                    ". . . . . . .",
                ),
            ),
            sequence = "a b a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 a에 넣었어요. 이 돌은 활로가 b 하나라 곧 잡혀요.",
                "백이 b에 두어 따냈어요. 그런데 백 전체의 활로가 a 하나만 남았어요.",
                "흑이 다시 a에 두어 백을 모두 따내요.",
            ),
        ),
        Explain(
            text = "이렇게 상대가 따낼 수 있는 자리에 일부러 돌을 넣는 수를 먹여치기라고 해요. 돌 하나를 내주는 대신 상대의 활로를 줄여요. 환격의 첫 수도 먹여치기였어요. 다음 장에서는 먹여치기로 상대의 집을 없애는 법도 배워요.",
        ),
        // verify: seqlibs black a,b 2
        Explain(
            text = "먹여치기가 늘 통하는 건 아니에요. 이번에는 백에게 바깥 활로 c가 있어요. 흑이 a에 넣고 백이 b로 따내면 백의 활로는 a와 c 둘이에요. 흑은 돌 하나만 잃었어요. 넣기 전에, 상대가 따낸 뒤 활로가 하나만 남는지 세어 봐요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    "X X X X X X .",
                    "X W W W W X .",
                    "X W a b W c .",
                    "X W W W W X .",
                    "X X X X X X .",
                    ". . . . . . .",
                ),
                caption = "이 모양에서는 바깥 활로 c부터 메워야 해요.",
            ),
            sequence = "a b",
            first = Side.BLACK,
            notes = listOf(
                "흑이 먼저 a에 넣었어요.",
                "백이 따냈어요. 활로가 a와 c 둘이라 흑은 a에 다시 둘 수 없어요.",
            ),
        ),
        // verify: after black b,a ladder no
        Problem(
            prompt = "위쪽 변의 표시된 백돌 둘을 잡으세요. 먹여칠 자리는 어디일까요?",
            diagram = Diagram(
                rows = listOf(
                    ". X b a O O .",
                    ". X W W X O .",
                    ". X X X X O O",
                    ". . X X . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "맞아요. 백이 b로 따내면 백돌 셋의 활로가 a 하나예요. 흑이 다시 a에 두어 따내요.",
            wrong = mapOf(
                "b" to "b로 단수 치면 백이 a에 이어요. 오른쪽 백돌과 한 몸이 되어 잡을 수 없어요.",
            ),
            fallback = "백돌 둘의 활로는 a와 b예요. 상대가 이으려는 자리에 먼저 넣어 보세요.",
            followUp = "b a",
            hint = "백돌 둘이 오른쪽 백돌과 이어지는 자리예요.",
        ),
        // verify: after black b,a ladder no
        // verify: after black c,a ladder no
        Problem(
            prompt = "표시된 백돌 넷을 잡으세요. a, b, c 가운데 어디일까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . .",
                    ". . O O O . .",
                    ". X X O X X .",
                    ". X b a W X .",
                    ". X W W W X .",
                    ". . X X X c .",
                    ". . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CATCH,
            solutions = "a",
            success = "좋아요. 백이 b로 따내도 활로가 a 하나라 흑이 되따내요. 먹여치기가 환격으로 이어졌어요.",
            wrong = mapOf(
                "b" to "b로 단수 치면 백이 a에 이어요. 위쪽 백돌과 한 몸이 되어 잡을 수 없어요.",
                "c" to "c는 백의 활로가 아니에요. 그 사이에 백이 a에 이어서 살아가요.",
            ),
            fallback = "백돌 넷의 활로는 a와 b예요. 상대가 이으려는 자리에 먼저 넣어 보세요.",
            followUp = "b a",
            hint = "위쪽 백돌과 표시된 백돌이 만나는 자리를 찾아요.",
        ),
        Quiz(
            question = "먹여치기를 하기 전에 꼭 확인할 것은 무엇일까요?",
            choices = listOf(
                "상대가 따낸 뒤 그 돌의 활로가 하나만 남는지",
                "내 돌이 절대 잡히지 않는지",
                "상대 돌이 몇 개인지",
            ),
            answer = 0,
            explanation = "먹여친 돌은 잡히는 것이 당연해요. 중요한 것은 그다음이에요. 상대가 따낸 뒤에도 활로가 하나뿐이어야 되따낼 수 있어요.",
        ),
    ),
)

// ─────────────────────────────── 6장 삶과 죽음 ───────────────────────────────

private fun chapter6() = Chapter(
    id = "ch6",
    number = 6,
    title = "삶과 죽음",
    summary = "둘러싸여도 잡히지 않는 돌의 비밀, 두 집을 배워요.",
    lessons = listOf(lesson6x1(), lesson6x2(), lesson6x3(), lesson6x4(), lesson6x5()),
)

private fun lesson6x1() = Lesson(
    id = "c6-1",
    title = "두 집이면 살아요",
    summary = "집이 하나인 돌은 잡히고, 집이 둘인 돌은 절대 잡히지 않아요.",
    minutes = 6,
    steps = listOf(
        // verify: libs 1
        // verify: captures white a 3
        // verify: status dead
        Explain(
            text = "표시된 흑돌이 백에게 완전히 둘러싸였어요. 흑돌이 둘러싼 빈 자리 a는 흑의 집이에요. 하지만 흑의 활로도 a 하나뿐이에요. 백이 a에 두면 어떻게 될까요?",
            diagram = Diagram(
                rows = listOf(
                    "a B O O O",
                    "B B O . O",
                    "O O O O O",
                    "O . O O O",
                    "O O O O O",
                ),
            ),
            sequence = "a",
            first = Side.WHITE,
            notes = listOf(
                "백이 집 안에 두어 흑돌을 모두 따냈어요. 따내는 수는 착수금지가 아니에요. 집이 하나뿐인 돌은 이렇게 잡혀요.",
            ),
        ),
        // verify: illegal white a
        // verify: illegal white b
        // verify: status alive
        Explain(
            text = "이번에는 집이 a와 b 둘이에요. 백이 a에 두면 그 돌은 활로가 없고, 흑을 따내지도 못해요. 흑에게 활로 b가 남아 있으니까요. 그래서 a는 백의 착수금지예요. b도 마찬가지예요.",
            diagram = Diagram(
                rows = listOf(
                    "a B b B O",
                    "B B B B O",
                    "O O O O O",
                    "O O O O O",
                    "O . O . O",
                ),
                caption = "백은 a에도 b에도 둘 수 없어요.",
            ),
        ),
        Explain(
            text = "따로 떨어진 집이 둘 있으면 그 돌은 절대 잡히지 않아요. 이것을 두 집을 내고 살았다고 해요. 잡히지 않는 돌은 산 돌, 언젠가 잡힐 수밖에 없는 돌은 죽은 돌이라고 불러요. 아래쪽 백돌도 집이 둘이라 산 돌이에요.",
            diagram = Diagram(
                rows = listOf(
                    "a B b B O",
                    "B B B B O",
                    "O O O O O",
                    "O O O O O",
                    "O c O d O",
                ),
                caption = "흑의 집은 a와 b, 백의 집은 c와 d예요.",
            ),
        ),
        // verify: seqcaptures white a,b,a 5
        // verify: status dead
        Explain(
            text = "빈 자리가 둘이어도 서로 붙어 있으면 집은 하나예요. 백이 a에 먹여치면 흑 전체가 단수라서 흑은 b로 따낼 수밖에 없어요. 그러면 빈 자리는 a 하나만 남아요.",
            diagram = Diagram(
                rows = listOf(
                    "a b B O O",
                    "B B B O O",
                    "O O O O O",
                    "O O O O O",
                    "O . O . O",
                ),
            ),
            sequence = "a b a",
            first = Side.WHITE,
            notes = listOf(
                "백이 먹여쳤어요. 흑 전체의 활로가 b 하나예요.",
                "흑이 백돌을 따냈어요. 하지만 이제 빈 자리는 a 하나예요.",
                "백이 a에 두어 흑돌을 모두 따내요.",
            ),
        ),
        // verify: status alive
        // verify: answer 잡을 수 없어요
        Quiz(
            question = "표시된 흑돌이 백에게 둘러싸였어요. 백은 이 흑돌을 잡을 수 있을까요?",
            choices = listOf(
                "잡을 수 없어요. 집이 둘이에요",
                "잡을 수 있어요. 활로가 둘뿐이에요",
            ),
            answer = 0,
            explanation = "a와 b는 서로 떨어진 집이에요. 백은 어느 쪽에도 둘 수 없어요. 활로가 둘뿐이어도 그 둘이 따로 떨어진 집이면 산 돌이에요.",
            diagram = Diagram(
                rows = listOf(
                    "O O O O O O",
                    "O . O O . O",
                    "O O O O O O",
                    "B B B B O O",
                    "a B b B O O",
                    "B B B B O O",
                ),
            ),
        ),
        // verify: status dead
        // verify: answer 죽었어요
        Quiz(
            question = "표시된 흑돌에는 집 a가 있고, 바깥쪽 활로 c와 d도 남아 있어요. 이 흑돌은 살았을까요?",
            choices = listOf(
                "살았어요. 활로가 셋이나 돼요",
                "죽었어요. 집은 하나뿐이에요",
            ),
            answer = 1,
            explanation = "c와 d는 백돌과 맞닿은 바깥 활로라서 백이 언제든 메울 수 있어요. 다 메우고 나면 집 하나만 남고, 백이 a에 두어 따내요. 활로의 수가 아니라 집의 수를 세어야 해요.",
            diagram = Diagram(
                rows = listOf(
                    "a B c O O",
                    "B B d O O",
                    "O O O O O",
                    "O O O O O",
                    "O . O . O",
                ),
            ),
        ),
        // verify: after black b,a status dead
        Problem(
            prompt = "흑 차례예요. 표시된 흑돌은 집이 하나 있고, 두 번째 집은 아직 덜 만들어졌어요. 한 수로 두 집을 완성해 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "c B b a O O",
                    "B B B B O O",
                    "O O O O O O",
                    "O . O O . O",
                    "O O O O O O",
                    "O O O O O O",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.LIVE,
            solutions = "a",
            success = "맞아요. a를 막자 b가 흑돌로 둘러싸인 집이 됐어요. c와 b, 집이 둘이라 살았어요.",
            wrong = mapOf(
                "b" to "b는 집이 될 자리예요. 스스로 메우면 집이 c 하나만 남아요. 백이 a까지 메우면 잡혀요.",
                "c" to "c는 이미 완성된 집이에요. 스스로 메우면 집을 하나 잃어요.",
            ),
            fallback = "집 안을 메우지 말고, 집의 울타리에서 비어 있는 곳을 찾아보세요.",
            hint = "b가 집이 되려면 b의 오른쪽이 흑돌로 막혀야 해요.",
        ),
        // verify: after black b,a status alive
        Problem(
            prompt = "흑 차례예요. 표시된 백돌은 귀에 집이 하나 있어요. 두 번째 집을 만들지 못하게 해서 잡아 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "X X X X X X",
                    "X . X X . X",
                    "X X X X X X",
                    "X X X X X X",
                    "X X W W W W",
                    "X X a b W c",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.KILL,
            solutions = "a",
            success = "맞아요. 이제 b는 흑돌과 맞닿아서 집이 될 수 없어요. 백은 집이 c 하나뿐이라 죽었어요.",
            wrong = mapOf(
                "b" to "b에 둔 돌은 활로가 a 하나예요. 백이 a에 두어 따내면 b가 백의 두 번째 집이 돼요.",
            ),
            fallback = "백이 두 번째 집을 만들 자리는 a와 b 쪽이에요. 그곳을 살펴보세요.",
            followUp = "b c",
            hint = "백이 a에 두면 b가 집이 돼요. 그 자리를 먼저 차지해요.",
        ),
    ),
)

private fun lesson6x2() = Lesson(
    id = "c6-2",
    title = "옥집은 집이 아니에요",
    summary = "집처럼 보이지만 결국 메워야 하는 가짜 집, 옥집을 가려내요.",
    minutes = 5,
    steps = listOf(
        // verify: status dead
        Explain(
            text = "표시된 흑돌에는 빈 자리 a와 b가 있어요. 집이 둘이라 살았을까요? b를 잘 보세요. 오른쪽 흑돌 둘은 b를 통해서만 표시된 흑돌과 이어져요.",
            diagram = Diagram(
                rows = listOf(
                    "a B b X X c",
                    "B B B O O O",
                    "O O O O . O",
                    "O . O O O O",
                    "O O O O O O",
                    "O O O O O O",
                ),
                caption = "b는 집일까요?",
            ),
        ),
        // verify: seqcaptures white c,b,a 7
        Explain(
            text = "백이 c로 단수 치면 흑돌 둘이 잡힐 위기예요. 살리려면 b에 이어야 해요. 스스로 집을 메우는 셈이에요. 이렇게 결국 메울 수밖에 없는 가짜 집을 옥집이라고 해요.",
            diagram = Diagram(
                rows = listOf(
                    "a B b X X c",
                    "B B B O O O",
                    "O O O O . O",
                    "O . O O O O",
                    "O O O O O O",
                    "O O O O O O",
                ),
            ),
            sequence = "c b a",
            first = Side.WHITE,
            notes = listOf(
                "백이 밖에서 단수를 쳤어요. 흑돌 둘의 활로는 b 하나예요.",
                "흑이 b에 이었어요. 집이 하나 사라졌어요.",
                "남은 집은 a 하나뿐이에요. 백이 a에 두어 모두 따내요.",
            ),
        ),
        Explain(
            text = "옥집을 가려내려면 집의 대각선 자리를 봐요. 변에 있는 집 c는 대각선 a와 b를 모두 흑이 차지해야 진짜 집이에요. 백이 하나라도 차지하면 집을 둘러싼 흑돌이 끊겨서 옥집이 돼요. 판 가운데의 집은 대각선 네 곳 가운데 세 곳을 차지하면 돼요.",
            diagram = Diagram(
                rows = listOf(
                    ". . X c X . .",
                    ". . a X b . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                    ". . . . . . .",
                ),
                caption = "c의 대각선 자리는 a와 b예요.",
            ),
        ),
        // verify: status dead
        // verify: answer b가 옥집
        Quiz(
            question = "표시된 흑돌에 빈 자리 a와 b가 있어요. 옥집은 어느 쪽일까요?",
            choices = listOf(
                "a가 옥집이에요",
                "b가 옥집이에요",
                "둘 다 진짜 집이에요",
            ),
            answer = 1,
            explanation = "b 왼쪽의 흑돌 둘은 b를 통해서만 이어져 있어요. 백이 왼쪽 끝을 막아 단수 치면 흑은 b를 메워야 해요. 귀의 a는 표시된 흑돌이 빈틈없이 둘러싼 진짜 집이에요.",
            diagram = Diagram(
                rows = listOf(
                    "O O O O O O",
                    "O O O O O O",
                    "O O O O . O",
                    "O . O O O O",
                    "O O O B B B",
                    ". X X b B a",
                ),
            ),
        ),
        // verify: after black a,b status alive
        // verify: after black b,a libs 1 target=D5
        Problem(
            prompt = "흑 차례예요. 표시된 백돌에는 집이 하나 있고, 두 번째 집도 거의 다 됐어요. 그 집을 옥집으로 만들어 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". W . O a",
                    "W W W b X",
                    "X X X X X",
                    "X X X X X",
                    "X . X . X",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.KILL,
            solutions = "b",
            success = "맞아요. 대각선 자리를 빼앗았어요. 위쪽 백돌은 이제 집을 통해서만 이어져요. 백이 a로 늘어도 단수라서, 결국 집을 메워 이어야 해요.",
            wrong = mapOf(
                "a" to "a에 두면 백이 b에 이어요. 백돌이 모두 단단히 이어져서 집 둘이 진짜 집이 돼요.",
            ),
            fallback = "두 번째 집을 둘러싼 백돌 가운데 아직 이어지지 않은 곳을 찾아보세요.",
            followUp = "a",
            hint = "집의 대각선 자리를 봐요. 백돌과 백돌 사이가 비어 있어요.",
        ),
        // verify: after black a,b status dead
        Problem(
            prompt = "흑 차례예요. 표시된 흑돌의 집 하나가 옥집이 될 위기예요. 한 수로 지켜 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "O . O . O",
                    "O O O O O",
                    "O O O O O",
                    "O b B B B",
                    "a X c B d",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.LIVE,
            solutions = "b",
            success = "맞아요. 떨어져 있던 흑돌이 단단히 이어졌어요. c와 d, 진짜 집이 둘이라 살았어요.",
            wrong = mapOf(
                "a" to "a에 두어도 왼쪽 흑돌은 아직 떨어져 있어요. 백이 b에 두면 단수가 되고, 흑은 c를 메워야 해요.",
                "c" to "c는 집이 될 자리예요. 스스로 메우면 집이 d 하나만 남아요.",
                "d" to "d는 이미 완성된 집이에요. 스스로 메우면 안 돼요.",
            ),
            fallback = "집 안을 메우지 말고, 흑돌과 흑돌 사이의 빈틈을 찾아보세요.",
            hint = "c의 대각선 자리 가운데 비어 있는 곳이 있어요.",
        ),
    ),
)

private fun lesson6x3() = Lesson(
    id = "c6-3",
    title = "급소, 3궁의 가운데",
    summary = "빈 자리가 셋인 궁도는 가운데 한 점이 삶과 죽음을 갈라요.",
    minutes = 6,
    steps = listOf(
        // verify: status unsettled
        Explain(
            text = "돌이 둘러싼 빈 공간을 궁도라고 해요. 빈 자리가 셋이면 3궁, 넷이면 4궁이에요. 표시된 흑돌의 궁도는 a, b, c가 나란히 있는 곧은 3궁이에요. 아직은 집이 하나로 뭉쳐 있어요.",
            diagram = Diagram(
                rows = listOf(
                    "a b c B O",
                    "B B B B O",
                    "O O O O O",
                    "O O O O O",
                    "O . O . O",
                ),
                caption = "빈 자리 셋이 한 줄로 늘어선 곧은 3궁이에요.",
            ),
        ),
        // verify: after black b status alive
        Explain(
            text = "흑이 먼저 둔다면 가운데 b예요. 궁도가 a와 c로 나뉘어서 따로 떨어진 집 둘이 돼요.",
            diagram = Diagram(
                rows = listOf(
                    "a b c B O",
                    "B B B B O",
                    "O O O O O",
                    "O O O O O",
                    "O . O . O",
                ),
            ),
            sequence = "b",
            first = Side.BLACK,
            notes = listOf(
                "가운데에 두자 a와 c가 집 둘이 됐어요. 흑은 살았어요.",
            ),
        ),
        // verify: after white b status dead
        Explain(
            text = "백이 먼저 둔다면 역시 가운데 b예요. 이제 흑은 a에 둬도 c에 둬도 스스로 단수가 돼요. 집을 둘로 나눌 방법이 없어요. 이렇게 삶과 죽음을 가르는 한 점을 급소라고 해요.",
            diagram = Diagram(
                rows = listOf(
                    "a b c B O",
                    "B B B B O",
                    "O O O O O",
                    "O O O O O",
                    "O . O . O",
                ),
            ),
            sequence = "b",
            first = Side.WHITE,
            notes = listOf(
                "백이 급소를 차지했어요. 흑은 죽었어요.",
            ),
        ),
        // verify: status unsettled
        // verify: after black a status alive
        // verify: after white a status dead
        Explain(
            text = "꺾인 모양의 3궁도 있어요. 굽은 3궁이라고 해요. 급소는 이번에도 가운데, 곧 꺾이는 자리 a예요. 흑이 두면 살고 백이 두면 죽어요.",
            diagram = Diagram(
                rows = listOf(
                    "a b B O O",
                    "c B B O O",
                    "B B O O .",
                    "O O O O O",
                    "O . O O O",
                ),
                caption = "굽은 3궁의 급소는 꺾이는 자리 a예요.",
            ),
        ),
        // verify: after black a,b status dead
        // verify: after black c,b status dead
        Problem(
            prompt = "흑 차례예요. 왼쪽 변의 표시된 흑돌을 살려 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "B B O O O",
                    "a B O . O",
                    "b B O O O",
                    "c B O . O",
                    "B B O O O",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.LIVE,
            solutions = "b",
            success = "맞아요. 곧은 3궁의 급소는 가운데예요. a와 c가 집 둘이 됐어요.",
            wrong = mapOf(
                "a" to "a에 두면 빈 자리가 b와 c만 남아요. 붙어 있는 두 자리는 집 하나예요. 백이 b에 먹여치면 잡혀요.",
                "c" to "c에 두면 빈 자리가 a와 b만 남아요. 붙어 있는 두 자리는 집 하나예요. 백이 b에 먹여치면 잡혀요.",
            ),
            fallback = "궁도 안의 세 자리 가운데에서 골라 보세요.",
            hint = "세 자리를 둘로 나누는 자리는 어디일까요?",
        ),
        // verify: after black b captures white a 1
        // verify: after black b,a status alive
        // verify: after black c captures white a 1
        // verify: after black c,a status alive
        Problem(
            prompt = "흑 차례예요. 귀에 있는 표시된 백돌을 잡아 보세요. 백의 궁도는 굽은 3궁이에요.",
            diagram = Diagram(
                rows = listOf(
                    "X X X . X",
                    "X X X X X",
                    ". X X W W",
                    "X X W W c",
                    "X X W b a",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.KILL,
            solutions = "a",
            success = "맞아요. 꺾이는 자리가 급소예요. 백은 b에 둬도 c에 둬도 스스로 단수가 돼요.",
            wrong = mapOf(
                "b" to "b에 둔 돌은 활로가 a 하나예요. 백이 a에 두어 따내면 b와 c가 따로 떨어진 집 둘이 돼요.",
                "c" to "c에 둔 돌은 활로가 a 하나예요. 백이 a에 두어 따내면 b와 c가 따로 떨어진 집 둘이 돼요.",
            ),
            fallback = "궁도 안의 세 자리 가운데에서 골라 보세요.",
            followUp = "c b",
            hint = "굽은 3궁의 급소는 꺾이는 자리예요.",
        ),
        // verify: after black a,b status dead
        // verify: after black c,b status dead
        Problem(
            prompt = "흑 차례예요. 위쪽 변의 표시된 흑돌을 살려 보세요. 이번에는 굽은 3궁이에요.",
            diagram = Diagram(
                rows = listOf(
                    "B a b B O",
                    "B B c B O",
                    "O B B B O",
                    "O O O O O",
                    "O . O . O",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.LIVE,
            solutions = "b",
            success = "맞아요. 꺾이는 자리 b에 두자 a와 c가 집 둘이 됐어요.",
            wrong = mapOf(
                "a" to "a에 두면 빈 자리가 b와 c만 남아요. 붙어 있는 두 자리는 집 하나예요. 백이 b에 먹여치면 잡혀요.",
                "c" to "c에 두면 빈 자리가 a와 b만 남아요. 붙어 있는 두 자리는 집 하나예요. 백이 b에 먹여치면 잡혀요.",
            ),
            fallback = "궁도 안의 세 자리 가운데에서 골라 보세요.",
            hint = "a와 c 둘 다와 맞닿은 자리를 찾아요.",
        ),
        // verify: after black a captures white b 1
        // verify: after black a,b status alive
        // verify: after black c captures white b 1
        // verify: after black c,b status alive
        Problem(
            prompt = "흑 차례예요. 판 가운데의 표시된 백돌을 잡아 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "X X X X X X X",
                    "X W W W W W X",
                    "X W a b c W X",
                    "X W W W W W X",
                    "X X X X X X X",
                    "X . X X X . X",
                    "X X X X X X X",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.KILL,
            solutions = "b",
            success = "맞아요. 판 가운데에서도 3궁의 급소는 가운데예요. 백은 집을 둘로 나눌 수 없어요.",
            wrong = mapOf(
                "a" to "a에 두면 백이 급소 b를 차지해요. 흑돌 하나를 따내면서 a와 c가 집 둘이 돼요.",
                "c" to "c에 두면 백이 급소 b를 차지해요. 흑돌 하나를 따내면서 a와 c가 집 둘이 돼요.",
            ),
            fallback = "궁도 안의 세 자리 가운데에서 골라 보세요.",
            followUp = "a c",
            hint = "귀에서도 변에서도 가운데에서도 곧은 3궁의 급소는 같아요.",
        ),
    ),
)

private fun lesson6x4() = Lesson(
    id = "c6-4",
    title = "4궁, 사는 모양과 죽는 모양",
    summary = "빈 자리가 넷이면 모양에 따라 살기도 하고 죽기도 해요.",
    minutes = 7,
    steps = listOf(
        // verify: status alive
        // verify: after white b,c status alive
        Explain(
            text = "빈 자리 넷이 한 줄로 늘어선 곧은 4궁이에요. 이 모양은 손을 대지 않아도 살아 있어요. 급소가 b와 c 두 곳이라서, 백이 한쪽에 두면 흑이 다른 쪽에 두면 돼요.",
            diagram = Diagram(
                rows = listOf(
                    "a b c d B O",
                    "B B B B B O",
                    "O O O O O O",
                    "O O O O O O",
                    "O . O O . O",
                    "O O O O O O",
                ),
            ),
            sequence = "b c",
            first = Side.WHITE,
            notes = listOf(
                "백이 b에 뒀어요.",
                "흑이 c에 받았어요. d가 집 하나, 그리고 활로가 a뿐인 백돌을 따내면 집이 하나 더 생겨요.",
            ),
        ),
        // verify: status alive
        // verify: after white c,b status alive
        Explain(
            text = "한 번 꺾인 굽은 4궁도 살아 있어요. 급소는 가운데 두 자리 b와 c예요. 백이 c에 두면 흑은 b에 받아요.",
            diagram = Diagram(
                rows = listOf(
                    "B a b c B O",
                    "B B B d B O",
                    "O O B B B O",
                    "O O O O O O",
                    "O . O O . O",
                    "O O O O O O",
                ),
            ),
            sequence = "c b",
            first = Side.WHITE,
            notes = listOf(
                "백이 c에 뒀어요.",
                "흑이 b에 받았어요. a가 집 하나, 그리고 활로가 d뿐인 백돌을 따내면 집이 하나 더 생겨요.",
            ),
        ),
        // verify: status dead
        // verify: after black a,d status dead
        Explain(
            text = "네모난 4궁은 죽은 모양이에요. 흑이 먼저 둬도 살 수 없어요. 흑이 a에 두면 백이 맞은편 d에 둬요. 남은 b와 c는 흑이 두는 순간 스스로 단수가 되는 자리예요.",
            diagram = Diagram(
                rows = listOf(
                    "a b B O O",
                    "c d B O O",
                    "B B B O O",
                    "O O O O O",
                    "O . O . O",
                ),
            ),
            sequence = "a d",
            first = Side.BLACK,
            notes = listOf(
                "흑이 먼저 한 자리를 차지해 봐요.",
                "백이 대각선 맞은편에 뒀어요. 흑은 집 둘을 만들 수 없어요.",
            ),
        ),
        // verify: status unsettled
        // verify: after black b status alive
        // verify: after white b status dead
        Explain(
            text = "가운데에서 세 갈래로 뻗은 모양은 삿갓 4궁이라고 해요. 급소는 세 자리와 모두 맞닿은 가운데 b예요. 흑이 두면 a, c, d가 모두 집이 되어 살고, 백이 두면 죽어요.",
            diagram = Diagram(
                rows = listOf(
                    "B a b c B O",
                    "B B d B B O",
                    "O B B B O O",
                    "O O O O O O",
                    "O . O O . O",
                    "O O O O O O",
                ),
                caption = "삿갓 4궁의 급소는 가운데 b예요.",
            ),
        ),
        Quiz(
            question = "백이 먼저 둬도 살아 있는 모양은 어느 것일까요?",
            choices = listOf(
                "곧은 4궁",
                "네모난 4궁",
                "곧은 3궁",
            ),
            answer = 0,
            explanation = "곧은 4궁은 급소가 두 곳이라 백이 하나를 차지해도 흑이 나머지를 차지해요. 네모난 4궁은 흑이 먼저 둬도 죽고, 곧은 3궁은 백이 가운데에 두면 죽어요.",
        ),
        // verify: after black a,c captures black d 2
        // verify: after black a,c,d status dead
        // verify: after black d,a status dead
        Problem(
            prompt = "흑 차례예요. 곧은 4궁에 백이 먼저 들어왔어요. 표시된 흑돌을 살려 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "a O c d B O",
                    "B B B B B O",
                    "O O O O O O",
                    "O O O O O O",
                    "O . O O . O",
                    "O O O O O O",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.LIVE,
            solutions = "c",
            success = "맞아요. 남은 급소를 차지했어요. d가 집 하나, 백돌을 따내면 집이 하나 더 생겨요.",
            wrong = mapOf(
                "a" to "a에 두면 백이 c로 늘어요. 흑이 d에 두어 백돌 둘을 따내도 빈 자리는 붙어 있는 둘뿐이라 집이 하나예요.",
                "d" to "d에 두면 궁도가 스스로 좁아져요. 백이 a에 두면 흑이 따내도 집은 하나뿐이에요.",
            ),
            fallback = "궁도 안의 빈 자리 가운데에서 골라 보세요.",
            hint = "곧은 4궁의 급소는 가운데 두 자리예요. 하나는 백이 이미 뒀어요.",
        ),
        // verify: after black a captures white b 1
        // verify: after black a,b status alive
        // verify: after black c captures white b 1
        // verify: after black c,b status alive
        // verify: after black d captures white b 1
        // verify: after black d,b status alive
        Problem(
            prompt = "흑 차례예요. 왼쪽 변의 표시된 백돌을 잡아 보세요. 백의 궁도는 삿갓 4궁이에요.",
            diagram = Diagram(
                rows = listOf(
                    "X X X X X X",
                    "W W X X . X",
                    "c W W X X X",
                    "b d W X X X",
                    "a W W X . X",
                    "W W X X X X",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.KILL,
            solutions = "b",
            success = "맞아요. 백이 이 흑돌을 따내려면 a, c, d를 모두 스스로 메워야 해요. 그러면 집은 하나만 남아요.",
            wrong = mapOf(
                "a" to "a에 두면 백이 급소 b를 차지해요. 흑돌 하나를 따내고 나면 a, c, d가 따로 떨어진 집이 돼요.",
                "c" to "c에 두면 백이 급소 b를 차지해요. 흑돌 하나를 따내고 나면 a, c, d가 따로 떨어진 집이 돼요.",
                "d" to "d에 두면 백이 급소 b를 차지해요. 흑돌 하나를 따내고 나면 a, c, d가 따로 떨어진 집이 돼요.",
            ),
            fallback = "궁도 안의 네 자리 가운데에서 골라 보세요.",
            hint = "나머지 세 자리와 모두 맞닿은 자리를 찾아요.",
        ),
        // verify: after black a,b captures black d 2
        // verify: after black a,b,d status dead
        // verify: after black d,b captures black a 2
        // verify: after black d,b,a status dead
        Problem(
            prompt = "흑 차례예요. 굽은 4궁에 백이 먼저 들어왔어요. 표시된 흑돌을 살려 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "B a b O B O",
                    "B B B d B O",
                    "O O B B B O",
                    "O O O O O O",
                    "O . O O . O",
                    "O O O O O O",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.LIVE,
            solutions = "b",
            success = "맞아요. 남은 급소를 차지했어요. a가 집 하나, 백돌을 따내면 집이 하나 더 생겨요.",
            wrong = mapOf(
                "a" to "a에 두면 백이 b로 늘어요. 흑이 d에 두어 백돌 둘을 따내도 빈 자리는 붙어 있는 둘뿐이라 집이 하나예요.",
                "d" to "d에 두면 백이 b로 늘어요. 흑이 a에 두어 백돌 둘을 따내도 빈 자리는 붙어 있는 둘뿐이라 집이 하나예요.",
            ),
            fallback = "궁도 안의 빈 자리 가운데에서 골라 보세요.",
            hint = "굽은 4궁의 급소는 가운데 두 자리예요. 하나는 백이 이미 뒀어요.",
        ),
    ),
)

private fun lesson6x5() = Lesson(
    id = "c6-5",
    title = "궁도 넓히기와 좁히기",
    summary = "살려는 쪽은 궁도를 넓히고, 잡으려는 쪽은 밖에서 좁혀요.",
    minutes = 5,
    steps = listOf(
        // verify: status unsettled
        // verify: after black a status alive
        Explain(
            text = "궁도가 넓으면 살기 쉽고 좁으면 죽기 쉬워요. 표시된 흑돌의 궁도는 왼쪽 a가 아직 열려 있어요. 흑이 a를 막으면 빈 자리 넷이 나란한 곧은 4궁이 돼요. 곧은 4궁은 살아 있는 모양이었죠.",
            diagram = Diagram(
                rows = listOf(
                    "b c d e B O",
                    "a B B B B O",
                    "O O O O O O",
                    "O O O O O O",
                    "O . O O . O",
                    "O O O O O O",
                ),
            ),
            sequence = "a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 열린 곳을 막아 궁도를 넓게 지켰어요. 곧은 4궁이라 살았어요.",
            ),
        ),
        // verify: after white a,c,b status dead
        Explain(
            text = "백이 먼저라면 같은 자리 a로 들어와요. 밖에서 궁도를 좁히는 수예요. 흑이 c로 집을 나누려 해도 백이 b로 밀고 들어와요. 남은 빈 자리는 붙어 있는 둘뿐이라 집은 하나예요.",
            diagram = Diagram(
                rows = listOf(
                    "b c d e B O",
                    "a B B B B O",
                    "O O O O O O",
                    "O O O O O O",
                    "O . O O . O",
                    "O O O O O O",
                ),
            ),
            sequence = "a c b",
            first = Side.WHITE,
            notes = listOf(
                "백이 밖에서 밀고 들어왔어요. 궁도가 좁아졌어요.",
                "흑이 집을 둘로 나누려고 해요.",
                "백이 한 번 더 밀었어요. 흑의 빈 자리는 d와 e, 붙어 있는 둘뿐이에요.",
            ),
        ),
        Explain(
            text = "정리해 볼까요? 살려는 쪽은 먼저 궁도를 넓혀요. 잡으려는 쪽은 밖에서 좁히고, 충분히 좁아지면 안쪽 급소에 둬요. 급소부터 둘지 밖에서 좁힐지 헷갈리면, 상대가 그 뒤에 어떻게 넓힐 수 있는지 떠올려 봐요.",
        ),
        // verify: after black a,c,d status alive
        // verify: after black c,a status dead
        // verify: after black d,a status dead
        // verify: after black e,b status dead
        // verify: after black b,a status dead
        Problem(
            prompt = "흑 차례예요. 표시된 흑돌의 궁도가 오른쪽에서 열려 있어요. 한 수로 살려 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "B b c d e",
                    "B B B B a",
                    "O O O B O",
                    "O O O O O",
                    "O . O . O",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.LIVE,
            solutions = "a",
            success = "맞아요. 열린 곳을 막아 곧은 4궁을 만들었어요. 백이 한쪽 급소에 두면 다른 쪽에 받으면 돼요.",
            wrong = mapOf(
                "c" to "안쪽에 먼저 두면 백이 a로 밀고 들어와요. 궁도가 좁아져서 집 둘을 만들 수 없어요.",
                "d" to "안쪽에 먼저 두면 백이 a로 밀고 들어와요. 궁도가 좁아져서 집 둘을 만들 수 없어요.",
                "e" to "e에 두면 백이 b에 들어와요. 흑이 어떻게 받아도 집 둘을 만들 수 없어요.",
                "b" to "b는 궁도를 스스로 좁히는 수예요. 백이 a로 밀고 들어오면 살 수 없어요.",
            ),
            fallback = "궁도의 울타리에서 아직 열려 있는 곳을 찾아보세요.",
            followUp = "c d",
            hint = "백돌과 맞닿은 빈 자리가 열린 곳이에요.",
        ),
        // verify: after black a,e,c status dead
        // verify: after black c,a status alive
        // verify: after black d,a status alive
        // verify: after black e,a status alive
        // verify: after black b,c status alive
        // verify: after black f,c status alive
        Problem(
            prompt = "흑 차례예요. 표시된 백돌을 잡아 보세요. 궁도가 넓어서 안쪽 급소부터 두면 실패해요.",
            diagram = Diagram(
                rows = listOf(
                    "W b c d e f",
                    "W W W W W a",
                    "X X X X W X",
                    "X X X X X X",
                    "X . X X . X",
                    "X X X X X X",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.KILL,
            solutions = "a",
            success = "맞아요. 밖에서 먼저 좁혔어요. 백이 e로 막으면 남은 궁도는 곧은 3궁이고, 흑이 가운데 급소 c에 두면 백은 죽어요.",
            wrong = mapOf(
                "c" to "안쪽에 먼저 두면 백이 a를 막아 궁도를 넓혀요. 넓은 궁도에서는 백이 집 둘을 만들 수 있어요.",
                "d" to "안쪽에 먼저 두면 백이 a를 막아 궁도를 넓혀요. 넓은 궁도에서는 백이 집 둘을 만들 수 있어요.",
                "e" to "안쪽에 먼저 두면 백이 a를 막아 궁도를 넓혀요. 넓은 궁도에서는 백이 집 둘을 만들 수 있어요.",
                "b" to "b에 두면 백이 c에 받아요. 궁도가 넓어서 백이 집 둘을 만들 수 있어요.",
                "f" to "f에 둔 돌은 흑돌과 이어져 있지 않아요. 백이 c에 두면 집 둘을 만들 수 있어요.",
            ),
            fallback = "궁도의 울타리에서 아직 열려 있는 곳을 찾아보세요.",
            followUp = "e c",
            hint = "백돌과 흑돌이 함께 맞닿은 빈 자리가 있어요.",
        ),
        // verify: after black a,d,e status alive
        // verify: after black g,a status dead
        // verify: after black c,a status dead
        // verify: after black d,a status dead
        // verify: after black e,a status dead
        // verify: after black f,a status dead
        // verify: after black b,a status dead
        Problem(
            prompt = "흑 차례예요. 오른쪽 위 귀의 표시된 흑돌을 살려 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "O O O O O a b",
                    "O O . O B g c",
                    "O O O O B B d",
                    "O O O O O B e",
                    "O O O O O B f",
                    "O O . O O B B",
                    "O O O O O O O",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.LIVE,
            solutions = "a",
            success = "맞아요. 열린 곳을 막자 궁도가 넓게 남았어요. 이 넓이면 백이 어디에 둬도 집 둘을 만들 수 있어요.",
            wrong = mapOf(
                "g" to "g에 두면 백이 a로 밀고 들어와요. 궁도가 좁아져서 집 둘을 만들 수 없어요.",
                "c" to "안쪽에 먼저 두면 백이 a로 밀고 들어와요. 궁도가 좁아져서 집 둘을 만들 수 없어요.",
                "d" to "안쪽에 먼저 두면 백이 a로 밀고 들어와요. 궁도가 좁아져서 집 둘을 만들 수 없어요.",
                "e" to "안쪽에 먼저 두면 백이 a로 밀고 들어와요. 궁도가 좁아져서 집 둘을 만들 수 없어요.",
                "f" to "안쪽에 먼저 두면 백이 a로 밀고 들어와요. 궁도가 좁아져서 집 둘을 만들 수 없어요.",
                "b" to "b는 궁도를 스스로 좁히는 수예요. 백이 a로 밀고 들어오면 집 둘을 만들 수 없어요.",
            ),
            fallback = "궁도의 울타리에서 아직 열려 있는 곳을 찾아보세요.",
            followUp = "d e",
            hint = "백돌과 맞닿은 빈 자리가 열린 곳이에요.",
        ),
    ),
)

// ─────────────────────────────── 7장 집 짓기와 포석 ───────────────────────────────

private fun chapter7() = Chapter(
    id = "ch7",
    number = 7,
    title = "집 짓기와 포석",
    summary = "판이 비어 있을 때 어디부터 둘지, 집을 짓는 순서를 배워요.",
    lessons = listOf(lesson7x1(), lesson7x2(), lesson7x3(), lesson7x4(), lesson7x5()),
)

private fun lesson7x1() = Lesson(
    id = "c7-1",
    title = "귀, 변, 중앙의 순서",
    summary = "같은 집을 지어도 귀가 가장 돌이 적게 들어요. 그래서 귀부터 둬요.",
    minutes = 5,
    steps = listOf(
        // verify: region a 9 black
        // verify: stones black=6 white=0
        Explain(
            text = "대국을 시작해서 돌을 넓게 펼쳐 놓는 단계를 포석이라고 해요. 포석에서는 집을 짓기 쉬운 곳부터 차지해요. 먼저 귀를 볼까요? 귀에서는 판의 끝 선 둘이 울타리가 되어 줘요. 돌 6개로 9집을 지었어요.",
            diagram = Diagram(
                rows = listOf(
                    "a b c X . . . . .",
                    "d e f X . . . . .",
                    "g h i X . . . . .",
                    "X X X . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "귀에서는 돌 6개로 9집이에요.",
                territoryBlack = "a b c d e f g h i",
            ),
        ),
        // verify: region a 9 black
        // verify: stones black=9 white=0
        Explain(
            text = "변에서는 끝 선 하나만 울타리가 되어 줘요. 같은 9집을 지으려면 돌이 9개 필요해요.",
            diagram = Diagram(
                rows = listOf(
                    ". . X a b c X . .",
                    ". . X d e f X . .",
                    ". . X g h i X . .",
                    ". . . X X X . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "변에서는 돌 9개로 9집이에요.",
                territoryBlack = "a b c d e f g h i",
            ),
        ),
        // verify: region a 9 black
        // verify: stones black=12 white=0
        Explain(
            text = "중앙에는 도와주는 끝 선이 없어요. 사방을 모두 돌로 둘러싸야 해서 9집에 돌이 12개나 들어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . X X X . . .",
                    ". . X a b c X . .",
                    ". . X d e f X . .",
                    ". . X g h i X . .",
                    ". . . X X X . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "중앙에서는 돌 12개로 9집이에요.",
                territoryBlack = "a b c d e f g h i",
            ),
        ),
        Explain(
            text = "그래서 포석은 귀에서 시작해요. 네 귀가 다 차면 변으로, 그다음에 중앙으로 나아가요. 귀, 변, 중앙의 순서예요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . d . . . a . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . b . . . c . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            sequence = "a b c d",
            first = Side.BLACK,
            notes = listOf(
                "흑이 오른쪽 위 귀에 뒀어요.",
                "백은 맞은편 귀를 차지해요.",
                "흑이 귀를 하나 더 가져요.",
                "백이 마지막 귀를 차지했어요. 이제 변으로 눈을 돌릴 차례예요.",
            ),
        ),
        Quiz(
            question = "같은 9집을 짓는 데 돌이 가장 적게 드는 곳은 어디일까요?",
            choices = listOf(
                "귀",
                "변",
                "중앙",
            ),
            answer = 0,
            explanation = "귀는 돌 6개, 변은 9개, 중앙은 12개가 들어요. 귀에서는 끝 선 둘이 울타리 노릇을 해 주기 때문이에요.",
        ),
        Problem(
            prompt = "흑 차례예요. 빈 판에 첫 수를 둬요. 표시된 자리 가운데 가장 좋은 곳을 골라 보세요.",
            diagram = Diagram(
                rows = listOf(
                    "c . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . a . .",
                    ". . . . . . . . .",
                    ". . . . . . . . d",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . b . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 귀에서 끝 선과 두 줄 떨어진 자리예요. 귀를 차지하면서도 돌이 튼튼해요.",
            wrong = mapOf(
                "b" to "맨 끝 선이에요. 돌 아래에 집을 지을 자리가 없어요.",
                "c" to "꼭짓점은 활로가 2개뿐인 가장 약한 자리예요. 집도 지을 수 없어요.",
                "d" to "여기도 맨 끝 선이에요. 끝 선에 붙은 돌은 집을 짓지 못해요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "집을 짓기 가장 쉬운 곳은 귀예요.",
        ),
        Problem(
            prompt = "흑 차례예요. 귀 셋에는 이미 돌이 있고, 백은 아래쪽 변에도 뒀어요. 표시된 자리 가운데 가장 큰 곳은 어디일까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . . . d . . . .",
                    ". . . . . . . . .",
                    ". . a . b . X . .",
                    ". . . . . . . . .",
                    ". . . . c . . . .",
                    ". . . . . . . . .",
                    ". . O . O . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 아직 빈 귀가 남아 있었어요. 귀가 변이나 중앙보다 먼저예요.",
            wrong = mapOf(
                "b" to "변도 좋은 곳이지만 아직 빈 귀가 남아 있어요. 귀가 먼저예요.",
                "c" to "중앙은 집을 짓는 데 돌이 가장 많이 들어요. 빈 귀가 있을 때는 귀가 먼저예요.",
                "d" to "맨 끝 선이에요. 돌 아래에 집을 지을 자리가 없어요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "네 귀 가운데 아직 아무도 두지 않은 곳이 있어요.",
        ),
    ),
)

private fun lesson7x2() = Lesson(
    id = "c7-2",
    title = "3선과 4선",
    summary = "끝 선에서 셋째 줄과 넷째 줄이 포석의 중심이에요.",
    minutes = 5,
    steps = listOf(
        Explain(
            text = "판의 줄은 끝에서부터 세어요. 맨 끝 선이 1선, 그 안쪽이 2선, 3선, 4선이에요. 아래쪽 변에서 a는 1선, b는 2선, c는 3선, d는 4선이에요. 9줄 판에서는 5선인 e가 한가운데예요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . e . . . .",
                    ". . . . d . . . .",
                    ". . . . c . . . .",
                    ". . . . b . . . .",
                    ". . . . a . . . .",
                ),
                caption = "끝에서부터 1선, 2선, 3선, 4선이에요.",
            ),
        ),
        Explain(
            text = "3선은 집을 짓기 좋은 줄이에요. 3선에 돌이 놓이면 그 아래 두 줄이 집이 되기 쉬워요. 상대가 그 밑으로 들어와도 살 자리가 좁아요. 그래서 3선을 실리선이라고도 불러요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . X . X . X . .",
                    ". a b c d e f g .",
                    ". h i j k l m n .",
                ),
                caption = "3선의 돌 아래는 흑집이 되기 쉬워요.",
                territoryBlack = "a b c d e f g h i j k l m n",
            ),
        ),
        Explain(
            text = "4선은 한 줄 더 높아요. 아래쪽이 넓어서 백이 a로 들어올 틈이 있어요. 대신 돌이 높아서 중앙으로 뻗어 나가는 힘이 세요. 그래서 4선을 세력선이라고도 불러요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . X . X . X . .",
                    ". . . . . . . . .",
                    ". . . . a . . . .",
                    ". . . . . . . . .",
                ),
                caption = "4선은 집보다 힘을 얻는 줄이에요.",
            ),
        ),
        // verify: territory black=9 white=54 neutral=0
        Explain(
            text = "1선과 2선은 너무 낮아요. 흑이 2선으로만 가고 백이 그 위를 따라 막으면 이렇게 돼요. 흑집은 맨 아랫줄 9집뿐이고, 나머지 넓은 곳은 모두 백의 것이에요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    "O O O O O O O O O",
                    "X X X X X X X X X",
                    "a b c d e f g h i",
                ),
                caption = "흑집은 9집, 백집은 54집이에요.",
                territoryBlack = "a b c d e f g h i",
            ),
        ),
        Quiz(
            question = "집을 짓기 좋아서 실리선이라고도 부르는 줄은 어느 것일까요?",
            choices = listOf(
                "1선",
                "3선",
                "5선",
            ),
            answer = 1,
            explanation = "3선은 그 아래 두 줄을 집으로 만들기 쉬워요. 1선은 돌 아래에 집을 지을 자리가 없고, 5선은 집을 짓기에는 너무 높아요.",
        ),
        Problem(
            prompt = "흑 차례예요. 네 귀가 모두 찼어요. 아래쪽 변에 돌을 놓으려고 해요. 어느 높이가 좋을까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . d . . . .",
                    ". . O . c . X . .",
                    ". . . . b . . . .",
                    ". . . . a . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "c d",
            success = "맞아요. 3선과 4선이 포석의 중심이에요. 3선은 집에, 4선은 힘에 조금 더 무게를 둔 자리예요.",
            wrong = mapOf(
                "a" to "1선은 너무 낮아요. 돌 아래에 집을 지을 자리가 없어요.",
                "b" to "2선은 집이 너무 작아요. 백이 위에서 누르면 한 줄짜리 집밖에 짓지 못해요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "포석에서는 3선이나 4선에 둬요.",
        ),
        Quiz(
            question = "포석에서 2선으로만 계속 두면 어떻게 될까요?",
            choices = listOf(
                "집은 조금 얻고 넓은 바깥을 상대에게 내줘요",
                "끝 선과 가까워서 집이 가장 많이 생겨요",
                "돌이 절대 잡히지 않아요",
            ),
            answer = 0,
            explanation = "2선의 돌 아래에는 한 줄밖에 없어요. 그 사이에 상대는 위쪽에 돌을 쌓아 넓은 곳을 차지해요.",
        ),
    ),
)

private fun lesson7x3() = Lesson(
    id = "c7-3",
    title = "굳힘과 걸침",
    summary = "내 귀는 한 수 더 들여 굳히고, 상대 귀에는 다가가서 걸쳐요.",
    minutes = 5,
    steps = listOf(
        Explain(
            text = "귀에 돌 하나만 있으면 아직 상대가 들어올 틈이 있어요. 한 수를 더 들여 귀를 단단히 지키는 것을 굳힘이라고 해요. 왼쪽 위 흑돌에서 보면 a가 굳힘의 자리예요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . a . X . .",
                    ". . X . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . O . .",
                    ". . O . b . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            sequence = "a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 귀를 굳혔어요. 돌 둘이 힘을 모아 왼쪽 위 귀를 지켜요.",
            ),
        ),
        Explain(
            text = "상대 귀에는 반대로 해요. 상대가 굳히기 전에 그 돌에 다가가는 것을 걸침이라고 해요. 오른쪽 아래 백돌에서 보면 b가 그 자리예요. 흑이 b에 두면 백은 귀를 마음 편히 굳힐 수 없어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . a . X . .",
                    ". . X . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . O . .",
                    ". . O . b . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            sequence = "b",
            first = Side.BLACK,
            notes = listOf(
                "흑이 백의 귀에 걸쳤어요. 백이 굳히려던 자리를 먼저 차지했어요.",
            ),
        ),
        Explain(
            text = "굳힘과 걸침은 같은 자리를 두고 다투는 거예요. a는 흑이 두면 굳힘이지만 백이 먼저 두면 백의 걸침이 돼요. b도 마찬가지예요. 네 귀가 찬 다음에는 굳힐지 걸칠지부터 생각해요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . a . X . .",
                    ". . X . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . O . .",
                    ". . O . b . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "a와 b는 먼저 두는 쪽이 임자예요.",
            ),
        ),
        Quiz(
            question = "상대 귀의 돌에 다가가서 굳히지 못하게 하는 수를 무엇이라고 할까요?",
            choices = listOf(
                "굳힘",
                "걸침",
                "먹여치기",
            ),
            answer = 1,
            explanation = "내 귀를 지키면 굳힘, 상대 귀에 다가가면 걸침이에요. 먹여치기는 잡힐 자리에 일부러 돌을 넣는 기술이었어요.",
        ),
        Problem(
            prompt = "흑 차례예요. 오른쪽 위 귀의 표시된 흑돌을 굳혀 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . b",
                    ". . . . . . . d .",
                    ". . O . a . . . .",
                    ". . . . . . B . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . X . .",
                    ". . . . . . . . .",
                    ". . . . c . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 귀의 돌과 힘을 모으는 자리예요. 이제 백이 오른쪽 위 귀에 들어오기 어려워요.",
            wrong = mapOf(
                "b" to "꼭짓점은 집도 짓지 못하고 활로도 2개뿐이에요. 귀를 지키는 데 도움이 되지 않아요.",
                "c" to "1선은 너무 낮아요. 귀에서도 멀어서 굳힘이 되지 않아요.",
                "d" to "이미 흑돌이 지키고 있는 귀 안쪽이에요. 너무 낮아서 집이 늘지 않아요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "표시된 흑돌에서 변 쪽으로 두 칸, 끝 선 쪽으로 한 칸 간 자리예요.",
        ),
        Problem(
            prompt = "흑 차례예요. 왼쪽 아래 귀의 표시된 백돌에 걸쳐 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . c .",
                    ". . O . . . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . W . . . . . .",
                    ". . . . a . X . .",
                    ". . . . . . . . .",
                    "d . . . b . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 백이 굳히려던 자리를 먼저 차지했어요. 오른쪽 아래 흑돌과도 가까워서 든든해요.",
            wrong = mapOf(
                "b" to "1선은 너무 낮아요. 백돌을 전혀 방해하지 못해요.",
                "c" to "내 귀 안쪽이에요. 백의 귀와는 상관없는 곳이라 걸침이 아니에요.",
                "d" to "꼭짓점은 활로가 2개뿐이에요. 백이 바로 단수를 칠 수 있어요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "백이 귀를 굳히려면 어디에 둘지 생각해 보세요. 그 자리가 걸침의 자리예요.",
        ),
    ),
)

private fun lesson7x4() = Lesson(
    id = "c7-4",
    title = "벌림",
    summary = "변에서는 내 돌에서 두 칸 떨어진 자리로 벌려요.",
    minutes = 5,
    steps = listOf(
        Explain(
            text = "귀 다음은 변이에요. 변에서 내 돌로부터 옆으로 간격을 두고 두는 것을 벌림이라고 해요. 왼쪽 아래 흑돌에서 빈 자리 둘을 건너뛴 a가 두 칸 벌림이에요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . X . . a . O .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            sequence = "a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 두 칸 벌렸어요. 아래쪽 변에 흑의 터가 생겼어요.",
            ),
        ),
        Explain(
            text = "왜 두 칸일까요? a는 한 칸 벌림이에요. 튼튼하지만 좁아요. b는 두 칸 벌림이에요. 넓으면서도 백이 사이에 들어오기 어려워요. c까지 가면 돌 사이가 너무 멀어서 백이 가운데로 들어올 수 있어요. 게다가 백돌과 바로 붙어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . X . a b c O .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "돌 하나에서는 두 칸 벌림이 기본이에요.",
            ),
        ),
        Explain(
            text = "벌림에는 두 가지 뜻이 있어요. 하나는 집을 지을 터를 넓히는 것이에요. 또 하나는 돌이 살 자리를 마련하는 것이에요. 변에 두 칸 벌린 돌은 그 아래에 집 둘을 낼 공간이 있어서 쉽게 잡히지 않아요.",
        ),
        Quiz(
            question = "변에 놓인 돌 하나에서 옆으로 벌릴 때 기본이 되는 간격은 얼마일까요?",
            choices = listOf(
                "바로 옆에 붙여요",
                "두 칸을 띄워요",
                "다섯 칸을 띄워요",
            ),
            answer = 1,
            explanation = "두 칸 벌림은 넓으면서도 끊기기 어려워요. 바로 옆에 붙이면 터가 넓어지지 않고, 너무 멀리 가면 상대가 사이로 들어와요.",
        ),
        Problem(
            prompt = "흑 차례예요. 왼쪽 아래의 표시된 흑돌에서 아래쪽 변으로 벌려 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . B a . b c O .",
                    ". . . . . . . . .",
                    ". . . . . d . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "b",
            success = "맞아요. 두 칸 벌림이에요. 터를 넓히면서도 돌 사이가 끊기지 않아요.",
            wrong = mapOf(
                "a" to "돌 옆에 바로 붙였어요. 튼튼하지만 터가 거의 넓어지지 않아요.",
                "c" to "너무 멀리 갔어요. 백돌과 바로 붙어서 공격받기 쉽고, 돌 사이로 백이 들어올 수 있어요.",
                "d" to "1선은 너무 낮아요. 돌 아래에 집을 지을 자리가 없어요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "표시된 흑돌에서 빈 자리 둘을 건너뛰어요.",
        ),
        Problem(
            prompt = "흑 차례예요. 이번에는 오른쪽 변이에요. 오른쪽 위의 표시된 흑돌에서 아래로 벌려 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . B . .",
                    ". . . . . . a . .",
                    ". . . . . . . . .",
                    ". . . . . . b . d",
                    ". . X . . . c . .",
                    ". . . . . . O . .",
                    ". . . . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "b",
            success = "맞아요. 방향이 바뀌어도 같아요. 빈 자리 둘을 건너뛴 두 칸 벌림이에요.",
            wrong = mapOf(
                "a" to "돌 옆에 바로 붙였어요. 튼튼하지만 터가 거의 넓어지지 않아요.",
                "c" to "너무 멀리 갔어요. 백돌과 바로 붙어서 공격받기 쉽고, 돌 사이로 백이 들어올 수 있어요.",
                "d" to "1선은 너무 낮아요. 끝 선에 붙은 돌은 집을 짓지 못해요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "표시된 흑돌에서 아래로 빈 자리 둘을 건너뛰어요.",
        ),
    ),
)

private fun lesson7x5() = Lesson(
    id = "c7-5",
    title = "큰 곳 찾기",
    summary = "넓게 비어 있는 곳이 큰 곳이에요. 다만 급한 곳이 있으면 그곳이 먼저예요.",
    minutes = 6,
    steps = listOf(
        Explain(
            text = "지금까지 배운 순서를 정리해 볼까요? 첫째는 빈 귀, 둘째는 굳힘과 걸침, 셋째는 변의 벌림, 마지막이 중앙이에요. 한 수의 값이 큰 자리를 큰 곳이라고 해요. 포석은 큰 곳을 번갈아 차지하는 단계예요.",
        ),
        Explain(
            text = "큰 곳은 넓게 비어 있는 곳이에요. a는 흑돌과 백돌 사이의 넓은 변이라서 누가 두든 터가 크게 늘어요. b는 이미 흑돌 둘 사이에 있는 좁은 곳이에요. 두지 않아도 흑의 터라서 값이 작아요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . X . . . O . .",
                    ". . b . . . . . .",
                    ". . . . . . . . .",
                    ". . X . . . . . .",
                    ". . . . a . O . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "a는 넓은 곳, b는 좁은 곳이에요.",
            ),
        ),
        // verify: libs 1
        // verify: after black a libs 3
        Explain(
            text = "큰 곳보다 먼저인 곳도 있어요. 바로 급한 곳이에요. 표시된 흑돌은 단수예요. 아무리 큰 곳이 보여도 a로 달아나는 것이 먼저예요. 돌이 잡히면 그 둘레가 모두 상대의 터가 되니까요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . X . .",
                    ". . . . O . . . .",
                    ". . . O B a . . .",
                    ". . . . O . . . .",
                    ". . X . . . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "단수에 몰린 돌이 있으면 그곳이 가장 급해요.",
            ),
            sequence = "a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 달아났어요. 활로가 3개로 늘었어요.",
            ),
        ),
        Quiz(
            question = "포석에서 두는 순서로 알맞은 것은 어느 것일까요?",
            choices = listOf(
                "귀, 변, 중앙",
                "중앙, 변, 귀",
                "변, 중앙, 귀",
            ),
            answer = 0,
            explanation = "집을 짓기 쉬운 곳부터 차지해요. 돌이 가장 적게 드는 귀가 먼저이고, 그다음이 변, 마지막이 중앙이에요.",
        ),
        Problem(
            prompt = "흑 차례예요. 네 귀가 모두 찼어요. 표시된 자리 가운데 큰 곳을 골라 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . a . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . c .",
                    ". . . . . . . . .",
                    ". . O . b . X . .",
                    ". . . . . . . . .",
                    ". . . . d . . . e",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a b",
            success = "맞아요. 흑돌과 백돌 사이의 넓은 변이에요. 흑이 두면 흑의 터가 넓어지고, 백이 두면 백의 터가 넓어지는 자리예요.",
            wrong = mapOf(
                "c" to "흑돌 둘이 이미 지키고 있는 안쪽이에요. 2선이라 낮고, 두지 않아도 흑의 터예요.",
                "d" to "1선은 너무 낮아요. 포석에서는 3선이나 4선에 둬요.",
                "e" to "꼭짓점은 집도 짓지 못하고 활로도 2개뿐이에요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "흑돌과 백돌이 마주 보는 변을 찾아보세요.",
        ),
        // verify: libs 1 target=E5
        // verify: after black a libs 3 target=E5
        Problem(
            prompt = "흑 차례예요. 아래쪽 변에 큰 곳이 보여요. 그런데 판 가운데를 잘 살펴보세요. 어디에 둘까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . X . .",
                    ". . . . O . . . .",
                    ". . . O X a . . .",
                    ". . . . O . . . .",
                    ". . X . b . X . .",
                    ". . . . . . . . .",
                    ". . . . c . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 가운데 흑돌이 단수였어요. 큰 곳보다 급한 곳이 먼저예요.",
            wrong = mapOf(
                "b" to "큰 곳이지만 지금은 가운데 흑돌이 단수예요. 백이 a에 두면 흑돌이 잡히고 가운데가 백의 터가 돼요.",
                "c" to "1선은 너무 낮아요. 게다가 가운데 흑돌이 단수라서 그곳이 더 급해요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "활로가 하나뿐인 흑돌이 있어요.",
        ),
        Problem(
            prompt = "흑 차례예요. 네 귀에 돌이 하나씩 있어요. 표시된 자리 가운데 큰 곳을 골라 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . e .",
                    ". . c . a . X . .",
                    ". . X . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . O . .",
                    ". . O . b . . . .",
                    ". . . . . . . . .",
                    ". . . . d . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a b",
            success = "맞아요. a는 내 귀를 굳히는 자리, b는 백의 귀에 걸치는 자리예요. 귀 다음으로 큰 곳은 굳힘과 걸침이에요.",
            wrong = mapOf(
                "c" to "흑돌 바로 옆이에요. 튼튼하지만 터가 거의 넓어지지 않아요.",
                "d" to "1선은 너무 낮아요. 포석에서는 3선이나 4선에 둬요.",
                "e" to "흑돌이 이미 지키고 있는 귀 안쪽이에요. 낮아서 집이 늘지 않아요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "네 귀가 찬 다음에는 굳힘과 걸침을 생각해요.",
        ),
    ),
)

// ─────────────────────────────── 8장 끝내기와 계가 ───────────────────────────────

private fun chapter8() = Chapter(
    id = "ch8",
    number = 8,
    title = "끝내기와 계가",
    summary = "집의 경계를 마무리하고, 누가 이겼는지 세는 법을 배워요.",
    lessons = listOf(lesson8x1(), lesson8x2(), lesson8x3(), lesson8x4(), lesson8x5()),
)

private fun lesson8x1() = Lesson(
    id = "c8-1",
    title = "끝내기와 공배",
    summary = "집의 경계를 마무리하는 끝내기와, 둬도 집이 늘지 않는 공배를 구별해요.",
    minutes = 5,
    steps = listOf(
        // verify: after black a territory black=21 white=14 neutral=0
        Explain(
            text = "판이 거의 다 찼어요. 왼쪽은 흑집, 오른쪽은 백집이에요. 그런데 흑의 울타리에 a가 비어 있어요. 이렇게 집의 경계를 마무리하는 단계를 끝내기라고 해요. 흑이 a를 막으면 흑집은 21집이에요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . b a O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                ),
            ),
            sequence = "a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 울타리의 빈틈을 막았어요. 흑집은 21집이에요.",
            ),
        ),
        // verify: after white a,b territory black=20 white=14 neutral=0
        Explain(
            text = "백이 먼저 두면 어떨까요? 백이 a로 밀고 들어오면 흑은 b로 물러나서 막아야 해요. 흑집이 20집으로 한 집 줄었어요. 같은 자리라도 누가 먼저 두느냐에 따라 집이 달라져요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . b a O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                ),
            ),
            sequence = "a b",
            first = Side.WHITE,
            notes = listOf(
                "백이 빈틈으로 밀고 들어왔어요.",
                "흑이 한 발 물러나 막았어요. 흑집은 20집이에요.",
            ),
        ),
        // verify: territory black=18 white=14 neutral=1
        // verify: after black d territory black=18 white=14 neutral=0
        // verify: after white d territory black=18 white=14 neutral=0
        Explain(
            text = "이번에는 d를 보세요. 흑돌과 백돌 사이에 끼어 있는 빈 자리예요. 흑이 둬도 백이 둬도 집은 늘지도 줄지도 않아요. 이런 자리를 공배라고 해요. 공배는 끝내기가 모두 끝난 뒤에 메워요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . X X O . .",
                    ". . X d O . .",
                    ". . X X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                ),
                caption = "d는 누구의 집도 아닌 공배예요.",
            ),
        ),
        Quiz(
            question = "공배에 돌을 두면 내 집은 몇 집 늘어날까요?",
            choices = listOf(
                "늘지 않아요",
                "1집 늘어요",
                "2집 늘어요",
            ),
            answer = 0,
            explanation = "공배는 누구의 집도 아닌 자리예요. 그래서 집이 걸린 끝내기를 먼저 두고, 공배는 맨 마지막에 메워요.",
        ),
        // verify: after black a territory black=18 white=14 neutral=1
        // verify: after white a,c territory black=17 white=14 neutral=1
        // verify: after black c territory black=17 white=14 neutral=2
        Problem(
            prompt = "흑 차례예요. 끝내기를 할 곳이 남아 있어요. 표시된 자리 가운데 흑집을 가장 잘 지키는 곳은 어디일까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . X X O . .",
                    ". . X d O . .",
                    ". . X X O . .",
                    ". . c a O . .",
                    ". . . X O . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 울타리의 빈틈을 막았어요. 흑집 18집을 온전히 지켰어요.",
            wrong = mapOf(
                "d" to "d는 공배예요. 누가 둬도 집은 그대로예요. 그 사이에 백이 a로 밀고 들어와서 흑집이 줄어요.",
                "c" to "c에 둬도 울타리는 닫혀요. 하지만 a에서 막을 수 있는데 한 발 물러선 셈이라 흑집이 한 집 줄어요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "흑의 울타리에서 백돌과 맞닿은 빈틈을 찾아보세요.",
        ),
        // verify: after black a,b territory black=13 white=20 neutral=1
        // verify: after white a territory black=13 white=21 neutral=1
        Problem(
            prompt = "흑 차례예요. 이번에는 백의 울타리에 빈틈이 있어요. 표시된 자리 가운데 백집을 줄이는 곳은 어디일까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . X O . . .",
                    ". . X O . . .",
                    ". . X a b . .",
                    ". . X O . . .",
                    ". . X O . . .",
                    ". X d O . . .",
                    ". . X O . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 백은 b로 물러나서 막아야 해요. 백이 먼저 a를 막았다면 21집이었을 백집이 20집으로 줄었어요.",
            wrong = mapOf(
                "b" to "너무 깊이 들어갔어요. 백이 a를 막으면 흑돌이 백집 안에 갇혀서 잡혀요.",
                "d" to "d는 공배예요. 누가 둬도 집은 그대로예요. 그 사이에 백이 a를 막아요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            followUp = "b",
            hint = "흑돌과 이어지면서 백의 울타리 빈틈에 닿는 자리예요.",
        ),
    ),
)

private fun lesson8x2() = Lesson(
    id = "c8-2",
    title = "선수 끝내기",
    summary = "상대가 꼭 받아야 하는 끝내기를 먼저 둬요.",
    minutes = 6,
    steps = listOf(
        Explain(
            text = "내가 둔 수를 상대가 꼭 받아야 하면 선수라고 해요. 상대가 받으면 다시 내 차례가 와요. 반대로 상대가 받지 않아도 되는 수는 후수예요. 내가 두고 나면 차례가 상대에게 넘어가요.",
        ),
        // verify: libs 2
        // verify: ataris black a 1
        // verify: after black a,c territory black=18 white=14 neutral=0 nodead
        Explain(
            text = "표시된 백돌의 활로는 a와 c예요. 흑이 a에 두면 단수예요. 백은 c에 이어야 해요. 잇지 않으면 흑이 c에 두어 백돌을 따내고 백집 안으로 들어가니까요. 백이 꼭 받아야 하니 흑의 a는 선수예요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . X X O . .",
                    ". . X W c . .",
                    ". . X a O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                ),
            ),
            sequence = "a c",
            first = Side.BLACK,
            notes = listOf(
                "흑이 단수를 쳤어요.",
                "백이 이었어요. 다시 흑 차례예요.",
            ),
        ),
        Explain(
            text = "이번에는 후수예요. 흑이 a를 막아도 백은 받을 곳이 없어요. 위험한 돌이 없으니까요. 흑이 두고 나면 백 차례예요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . . a O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                ),
            ),
            sequence = "a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 빈틈을 막았어요. 백은 받지 않고 다른 곳에 둘 수 있어요.",
            ),
        ),
        // verify: after black a,c,d territory black=17 white=14 neutral=0
        Explain(
            text = "선수 끝내기와 후수 끝내기가 함께 있으면 선수부터 둬요. 위쪽 a는 단수라서 선수, 아래쪽 d는 후수예요. a를 먼저 두면 백이 받은 뒤에 d까지 둘 수 있어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . X X O . .",
                    ". . X O c . .",
                    ". . X a O . .",
                    ". . X X O . .",
                    ". . e d O . .",
                    ". . . X O . .",
                ),
            ),
            sequence = "a c d",
            first = Side.BLACK,
            notes = listOf(
                "흑이 선수 끝내기부터 뒀어요.",
                "백은 이어야 해요.",
                "흑이 후수 끝내기까지 뒀어요. 흑집 17집, 백집 14집이에요.",
            ),
        ),
        // verify: after black d,a territory black=17 white=15 neutral=0
        Explain(
            text = "순서를 바꾸면 어떨까요? 흑이 후수인 d부터 두면 차례가 백에게 넘어가요. 백이 a를 차지하면 c가 백집이 돼요. 백집이 15집으로 한 집 늘었어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . X X O . .",
                    ". . X O c . .",
                    ". . X a O . .",
                    ". . X X O . .",
                    ". . e d O . .",
                    ". . . X O . .",
                ),
            ),
            sequence = "d a",
            first = Side.BLACK,
            notes = listOf(
                "흑이 후수 끝내기부터 뒀어요.",
                "백이 a를 차지했어요. 백은 c를 메우지 않아도 돼요.",
            ),
        ),
        Quiz(
            question = "선수란 어떤 수일까요?",
            choices = listOf(
                "상대가 꼭 받아야 하는 수",
                "가장 먼저 둔 수",
                "집이 가장 많이 느는 수",
            ),
            answer = 0,
            explanation = "상대가 받지 않으면 큰 손해를 보는 수가 선수예요. 상대가 받아 주니 두고 나서도 다시 내 차례가 와요.",
        ),
        // verify: ataris black a 1
        // verify: after black a,c,d territory black=17 white=14 neutral=0
        // verify: after black d,a territory black=17 white=15 neutral=0
        // verify: after black c libs 1 target=E3
        Problem(
            prompt = "흑 차례예요. 끝내기를 할 곳이 두 군데 있어요. 어디를 먼저 둘까요?",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . e d O . .",
                    ". . X X O . .",
                    ". . X a O . .",
                    ". . X O c . .",
                    ". . X X O . .",
                    ". . . X O . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 단수라서 백이 c에 이어야 해요. 그다음에 d까지 두면 두 곳을 모두 차지해요.",
            wrong = mapOf(
                "d" to "d는 후수예요. 그 사이에 백이 a를 차지해서 백집이 한 집 늘어요.",
                "c" to "c에 둔 돌은 활로가 하나뿐이에요. 백이 바로 따내요.",
                "e" to "e는 흑집 안이에요. 두면 흑집이 한 집 줄고, 울타리의 빈틈도 그대로예요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            followUp = "c d",
            hint = "백이 꼭 받아야 하는 곳부터 둬요. 단수가 되는 자리가 있어요.",
        ),
        // verify: libs 1
        // verify: captures black c 1
        // verify: answer 백돌을 따내고
        Quiz(
            question = "흑이 표시된 백돌에 단수를 쳤어요. 백이 받지 않고 다른 곳에 두면 어떻게 될까요?",
            choices = listOf(
                "흑이 c에 두어 백돌을 따내고 백집 안으로 들어가요",
                "아무 일도 일어나지 않아요",
                "흑돌이 잡혀요",
            ),
            answer = 0,
            explanation = "백돌의 활로는 c 하나뿐이에요. 흑이 c에 두면 백돌을 따내면서 백의 울타리가 뚫려요. 그래서 백은 받을 수밖에 없고, 흑의 단수는 선수예요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . X X O . .",
                    ". . X W c . .",
                    ". . X X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                ),
            ),
        ),
    ),
)

private fun lesson8x3() = Lesson(
    id = "c8-3",
    title = "사석과 계가",
    summary = "대국이 끝나면 죽은 돌을 들어내고 집을 세요.",
    minutes = 5,
    steps = listOf(
        Explain(
            text = "끝내기도 공배도 끝나서 더 둘 곳이 없으면 한 수 쉬어요. 두 사람이 연달아 쉬면 대국이 끝나요. 이제 누가 이겼는지 세어 볼 차례예요. 집을 세는 일을 계가라고 해요.",
        ),
        // verify: territory black=17 white=15 neutral=0
        // verify: score bcap=1 wcap=2 black=19 white=24.5
        Explain(
            text = "판 위에는 살 수 없는 돌이 남아 있을 수 있어요. 표시된 백돌은 흑집 안에 혼자 있어서 두 집을 낼 수 없어요. 표시된 흑돌도 마찬가지예요. 이런 죽은 돌을 사석이라고 해요. 사석은 굳이 따내지 않아도 돼요. 대국이 끝나면 그대로 들어내요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". W . X O . .",
                    ". . . X O . .",
                    ". . X X O O .",
                    ". . X O O . .",
                    ". . X O . B .",
                    ". . X O . . .",
                ),
                caption = "표시된 돌은 사석이에요.",
            ),
        ),
        Explain(
            text = "들어낸 사석은 대국 중에 따낸 돌과 합쳐요. 내 점수는 내 집의 수에 내가 따낸 돌의 수를 더한 것이에요. 사석이 있던 자리는 빈 자리가 되니 집으로 함께 세어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". W . X O . .",
                    ". . . X O . .",
                    ". . X X O O .",
                    ". . X O O . .",
                    ". . X O . B .",
                    ". . X O . . .",
                ),
                caption = "흑집은 17집, 백집은 15집이에요.",
            ),
        ),
        Explain(
            text = "이 앱에서는 계가를 자동으로 해 줘요. 대국이 끝나면 죽은 돌을 미리 표시해서 보여 줘요. 표시가 틀렸다고 생각되면 그 돌을 눌러서 고칠 수 있어요. 집, 따낸 돌, 합계가 바로 다시 계산돼요.",
        ),
        Quiz(
            question = "대국이 끝났을 때 내 집 안에 남아 있는 상대의 사석은 어떻게 할까요?",
            choices = listOf(
                "들어내서 따낸 돌에 보태요",
                "그대로 두고 상대의 집으로 세어요",
                "한 수씩 더 두어 직접 따내야 해요",
            ),
            answer = 0,
            explanation = "사석은 따로 따내지 않아도 대국이 끝나면 들어내요. 들어낸 돌은 따낸 돌과 똑같이 내 점수가 돼요.",
        ),
        // verify: mention 19집
        // verify: answer 19집
        Quiz(
            question = "흑집이 17집이에요. 흑은 대국 중에 백돌 1개를 따냈고, 끝난 뒤 사석 1개를 들어냈어요. 흑의 점수는 몇 집일까요?",
            choices = listOf(
                "17집",
                "18집",
                "19집",
            ),
            answer = 2,
            explanation = "집 17집에 따낸 돌 1개와 사석 1개를 더해요. 17 더하기 2는 19집이에요.",
        ),
    ),
)

private fun lesson8x4() = Lesson(
    id = "c8-4",
    title = "덤",
    summary = "먼저 두는 흑이 유리해서, 백은 덤 6.5집을 받아요.",
    minutes = 4,
    steps = listOf(
        Explain(
            text = "바둑은 흑이 먼저 둬요. 먼저 두는 쪽이 큰 곳을 하나 더 차지하니까 흑이 유리해요. 공평하게 하려고 계가할 때 백에게 집을 더해 줘요. 이것을 덤이라고 해요. 이 앱에서는 덤이 6.5집이에요.",
        ),
        Explain(
            text = "덤에는 왜 반집이 붙어 있을까요? 판 위의 집은 1집, 2집처럼 늘 딱 떨어지는 수예요. 여기에 6.5집을 더하면 두 사람의 점수가 같아질 수 없어요. 그래서 비기는 일 없이 승부가 나요. 가장 작은 차이로 이기면 반집 승이라고 해요.",
        ),
        // verify: mention 5.5집
        // verify: answer 백이 5.5집
        Quiz(
            question = "계가를 했더니 흑은 19집, 백은 18집이에요. 백에게 덤 6.5집을 더하면 결과는 어떻게 될까요?",
            choices = listOf(
                "흑이 1집 차이로 이겨요",
                "백이 5.5집 차이로 이겨요",
                "백이 6.5집 차이로 이겨요",
            ),
            answer = 1,
            explanation = "백은 18집에 덤 6.5집을 더해 24.5집이에요. 흑은 19집이니까 백이 5.5집 차이로 이겨요.",
        ),
        // verify: answer 7집
        Quiz(
            question = "덤이 6.5집일 때, 흑이 이기려면 덤을 더하기 전에 백보다 적어도 몇 집 많아야 할까요?",
            choices = listOf(
                "6집",
                "7집",
                "10집",
            ),
            answer = 1,
            explanation = "6집 많으면 덤을 더한 뒤 백이 반집 앞서요. 7집 많아야 흑이 반집 차이로 이겨요.",
        ),
        Quiz(
            question = "덤을 받는 쪽은 누구일까요?",
            choices = listOf(
                "먼저 두는 흑",
                "나중에 두는 백",
                "집이 적은 쪽",
            ),
            answer = 1,
            explanation = "먼저 두는 흑이 유리하기 때문에, 나중에 두는 백이 덤을 받아요.",
        ),
    ),
)

private fun lesson8x5() = Lesson(
    id = "c8-5",
    title = "한 판 계가 따라 하기",
    summary = "끝난 9줄 대국 한 판을 처음부터 끝까지 함께 세어 봐요.",
    minutes = 7,
    steps = listOf(
        // verify: stones black=19 white=16
        Explain(
            text = "9줄 대국 한 판이 끝났어요. 왼쪽이 흑, 오른쪽이 백이에요. 대국 중에 흑은 백돌 4개를, 백은 흑돌 1개를 따냈어요. 계가하기 전에 먼저 공배를 메워요. 공배는 a와 b 두 곳이에요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . X O . . .",
                    ". . X . X O O . .",
                    ". X . X X O . . O",
                    ". . . X a O O . .",
                    "X X X X X O . X .",
                    ". . . X b O O . .",
                    ". O . X X O . O .",
                    ". . X . X O . . .",
                    ". . . . X O O . .",
                ),
            ),
            sequence = "a b",
            first = Side.BLACK,
            notes = listOf(
                "흑이 공배 하나를 메웠어요.",
                "백이 남은 공배를 메웠어요. 집은 그대로예요.",
            ),
        ),
        // verify: territory black=25 white=21 neutral=0
        // verify: score bcap=4 wcap=1 black=30 white=29.5
        Explain(
            text = "다음은 사석이에요. 표시된 백돌은 흑집 안에서, 표시된 흑돌은 백집 안에서 살 수 없어요. 둘 다 들어내요. 흑이 따낸 돌은 4개에 사석 1개를 더해 5개, 백이 따낸 돌은 1개에 사석 1개를 더해 2개가 돼요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . X O . . .",
                    ". . X . X O O . .",
                    ". X . X X O . . O",
                    ". . . X X O O . .",
                    "X X X X X O . B .",
                    ". . . X O O O . .",
                    ". W . X X O . O .",
                    ". . X . X O . . .",
                    ". . . . X O O . .",
                ),
                caption = "표시된 돌 둘이 사석이에요.",
            ),
        ),
        // verify: territory black=25 white=21 neutral=0
        // verify: shaded 12
        Explain(
            text = "이제 흑집을 세어요. 흑집은 가운데 흑돌을 사이에 두고 위아래로 나뉘어 있어요. 위쪽부터 세어 볼까요? 색칠된 자리를 하나씩 세면 12집이에요.",
            diagram = Diagram(
                rows = listOf(
                    "a b c d X O . . .",
                    "e f X g X O O . .",
                    "h X i X X O . . O",
                    "j k l X X O O . .",
                    "X X X X X O . . .",
                    ". . . X O O O . .",
                    ". . . X X O . O .",
                    ". . X . X O . . .",
                    ". . . . X O O . .",
                ),
                caption = "위쪽 흑집은 12집이에요.",
                territoryBlack = "a b c d e f g h i j k l",
            ),
        ),
        // verify: territory black=25 white=21 neutral=0
        // verify: shaded 13
        Explain(
            text = "아래쪽 흑집은 13집이에요. 사석을 들어낸 자리도 집으로 세었어요. 위쪽 12집과 더하면 흑집은 모두 25집이에요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . X O . . .",
                    ". . X . X O O . .",
                    ". X . X X O . . O",
                    ". . . X X O O . .",
                    "X X X X X O . . .",
                    "a b c X O O O . .",
                    "d e f X X O . O .",
                    "g h X i X O . . .",
                    "j k l m X O O . .",
                ),
                caption = "아래쪽 흑집은 13집이에요.",
                territoryBlack = "a b c d e f g h i j k l m",
            ),
        ),
        // verify: territory black=25 white=21 neutral=0
        // verify: shaded 21
        Explain(
            text = "백집도 같은 방법으로 세어요. 색칠된 자리를 모두 세면 21집이에요. 이제 재료가 다 모였어요. 흑은 집 25집에 따낸 돌 5개, 백은 집 21집에 따낸 돌 2개예요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . X O a b c",
                    ". . X . X O O d e",
                    ". X . X X O f g O",
                    ". . . X X O O h i",
                    "X X X X X O j k l",
                    ". . . X O O O m n",
                    ". . . X X O p O q",
                    ". . X . X O r s t",
                    ". . . . X O O u v",
                ),
                caption = "백집은 21집이에요.",
                territoryWhite = "a b c d e f g h i j k l m n p q r s t u v",
            ),
        ),
        // verify: mention 30집
        // verify: answer 30집
        Quiz(
            question = "흑은 집이 25집, 따낸 돌이 사석까지 5개예요. 흑의 점수는 몇 집일까요?",
            choices = listOf(
                "25집",
                "30집",
                "31.5집",
            ),
            answer = 1,
            explanation = "집 25집에 따낸 돌 5개를 더해서 30집이에요. 흑은 덤을 받지 않아요.",
        ),
        // verify: mention 29.5집
        // verify: answer 29.5집
        Quiz(
            question = "백은 집이 21집, 따낸 돌이 사석까지 2개예요. 덤 6.5집까지 더하면 백의 점수는 몇 집일까요?",
            choices = listOf(
                "23집",
                "27.5집",
                "29.5집",
            ),
            answer = 2,
            explanation = "집 21집에 따낸 돌 2개를 더하면 23집이에요. 여기에 덤 6.5집을 더해서 29.5집이에요.",
        ),
        // verify: answer 흑의 반집 승
        Quiz(
            question = "흑은 30집, 백은 29.5집이에요. 이 판의 결과는 무엇일까요?",
            choices = listOf(
                "흑의 반집 승",
                "백의 반집 승",
                "비겼어요",
            ),
            answer = 0,
            explanation = "흑이 0.5집, 곧 반집 차이로 이겼어요. 덤이 없었다면 7집 차이였을 거예요. 덤이 있어서 끝까지 한 집 한 집이 중요해요.",
        ),
    ),
)

// ─────────────────────────────── 9장 첫 대국 ───────────────────────────────

private fun chapter9() = Chapter(
    id = "ch9",
    number = 9,
    title = "첫 대국",
    summary = "배운 것을 모아 9줄 바둑판에서 첫 판을 둘 준비를 해요.",
    lessons = listOf(lesson9x1(), lesson9x2(), lesson9x3(), lesson9x4()),
)

private fun lesson9x1() = Lesson(
    id = "c9-1",
    title = "9줄 바둑 요령",
    summary = "좁은 9줄 판에서는 돌을 튼튼하게 두고, 두기 전에 단수부터 살펴요.",
    minutes = 5,
    steps = listOf(
        Explain(
            text = "9줄 판은 좁아요. 몇 수만 두어도 흑돌과 백돌이 맞닿아서 싸움이 시작돼요. 그래서 9줄에서는 멋진 포석보다 돌을 튼튼하게 두는 것이 더 중요해요. 한 판이 금방 끝나니 여러 판을 두며 배우기에도 좋아요.",
        ),
        Explain(
            text = "첫 수는 어디가 좋을까요? 귀의 화점 b, c, d, e는 귀를 차지하는 든든한 자리예요. 9줄에서는 한가운데 a도 좋은 첫 수예요. 판이 좁아서 가운데 돌이 네 귀 모두에 힘을 미쳐요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . b . . . c . .",
                    ". . . . . . . . .",
                    ". . . . a . . . .",
                    ". . . . . . . . .",
                    ". . d . . . e . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "점이 찍힌 다섯 자리를 화점이라고 해요.",
            ),
        ),
        Explain(
            text = "새 돌은 내 돌 가까이에 둬요. a처럼 바로 옆에 두면 끊길 걱정이 없어요. b처럼 한 칸 띄우면 조금 더 멀리 가면서도 이어지기 쉬워요. c처럼 너무 멀리 두면 백이 사이로 들어와서 돌이 따로따로 싸우게 돼요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . . . .",
                    ". . . . . . . . .",
                    ". . . . X a b . c",
                    ". . . . . . . . .",
                    ". . . . . . O . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "가까울수록 튼튼하고, 멀수록 끊기기 쉬워요.",
            ),
        ),
        Explain(
            text = "돌을 놓기 전에 세 가지를 살펴요. 첫째, 내 돌 가운데 단수인 돌이 있나요? 둘째, 상대 돌 가운데 단수인 돌이 있나요? 셋째, 내 돌이 끊기는 곳은 없나요? 이 세 가지만 지켜도 큰 실수가 크게 줄어요.",
        ),
        // verify: captures black a 2
        // verify: captures black b 0
        // verify: captures black c 0
        // verify: after white a libs 4 target=E5
        Problem(
            prompt = "흑 차례예요. 백돌 가운데 단수인 돌이 있어요. 찾아서 따내 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . X . X . .",
                    ". . . X O X . . .",
                    ". . c X O a O . .",
                    ". . . . X . . . .",
                    ". . O . b . O . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 가운데 백돌 둘의 활로는 a 하나뿐이었어요. 따내고 나니 흑돌이 모두 튼튼해졌어요.",
            wrong = mapOf(
                "b" to "좋아 보이는 자리지만 더 급한 곳이 있어요. 백이 a로 늘면 단수였던 백돌이 오른쪽 백돌과 이어져서 달아나요.",
                "c" to "백돌의 활로와 상관없는 곳이에요. 백이 a로 늘면 단수였던 백돌이 오른쪽 백돌과 이어져서 달아나요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "가운데 백돌 둘의 활로를 세어 보세요.",
        ),
        // verify: libs 1 target=D5
        // verify: after black a libs 3 target=D5
        // verify: after black a ladder no target=D5
        // verify: after black b captures white a 2
        // verify: after black c captures white a 2
        Problem(
            prompt = "흑 차례예요. 이번에는 내 돌 가운데 단수인 돌이 있어요. 찾아서 살려 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . c . . . X . .",
                    ". . . O O . . . .",
                    ". . O X X a . . .",
                    ". . . O O . . . .",
                    ". . X . b . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 가운데 흑돌 둘의 활로는 a 하나뿐이었어요. 늘고 나니 활로가 3개가 됐어요.",
            wrong = mapOf(
                "b" to "백돌에 다가갔지만 단수는 아니에요. 백이 a에 두면 흑돌 둘이 잡혀요.",
                "c" to "빈 귀는 큰 곳이에요. 하지만 지금은 흑돌 둘이 단수예요. 백이 a에 두면 잡혀요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "가운데 흑돌 둘의 활로를 세어 보세요.",
        ),
    ),
)

private fun lesson9x2() = Lesson(
    id = "c9-2",
    title = "대국 예절",
    summary = "바둑은 상대와 함께 두는 놀이예요. 서로 기분 좋게 두는 법을 알아봐요.",
    minutes = 4,
    steps = listOf(
        Explain(
            text = "사람과 바둑을 둘 때는 인사로 시작해요. 시작할 때는 잘 부탁드립니다, 끝났을 때는 잘 두었습니다 하고 말해요. 이기든 지든 함께 한 판을 만든 상대에게 고마움을 전하는 거예요.",
        ),
        Explain(
            text = "한 번 놓은 돌은 다시 집어 들지 않아요. 둘 곳을 정한 다음에 돌을 집고, 놓은 뒤에는 손을 떼요. 상대가 생각하는 동안에는 재촉하지 않고 기다려요. 오래 생각하는 것은 실례가 아니에요.",
        ),
        Explain(
            text = "아무리 둬도 이길 수 없는 판이라면 졌다고 말하고 그만둘 수 있어요. 이것을 기권이라고 해요. 기권은 부끄러운 일이 아니에요. 상대의 실력을 인정하는 예의 바른 방법이에요.",
        ),
        Explain(
            text = "이 앱에서 AI와 둘 때는 조금 달라요. 연습이니까 무르기를 써도 괜찮아요. 실수한 수를 물러서 다시 두어 보면 무엇이 달라지는지 배울 수 있어요. 사람과 둘 때는 무르지 않는 것이 약속이에요.",
        ),
        Quiz(
            question = "사람과 대국하는 중에 방금 둔 수가 실수였다는 것을 알았어요. 어떻게 할까요?",
            choices = listOf(
                "무르지 않고 다음 수를 더 잘 생각해요",
                "돌을 집어서 다른 곳에 다시 둬요",
                "상대가 안 볼 때 돌을 옮겨요",
            ),
            answer = 0,
            explanation = "한 번 놓은 돌은 움직이지 않는 것이 바둑의 약속이에요. 실수는 누구나 해요. 다음 수로 만회하면 돼요.",
        ),
        Quiz(
            question = "도저히 이길 수 없는 판이에요. 예의 바르게 끝내는 방법은 무엇일까요?",
            choices = listOf(
                "아무 말 없이 자리를 떠나요",
                "졌다고 말하고 기권해요",
                "상대가 지칠 때까지 아무 데나 계속 둬요",
            ),
            answer = 1,
            explanation = "졌다고 말하고 기권하는 것이 예의예요. 끝난 뒤에는 잘 두었습니다 하고 인사해요.",
        ),
    ),
)

private fun lesson9x3() = Lesson(
    id = "c9-3",
    title = "자주 하는 실수",
    summary = "처음 둘 때 누구나 하는 실수를 미리 알아 두면 덜 하게 돼요.",
    minutes = 6,
    steps = listOf(
        Explain(
            text = "가장 흔한 실수는 단수를 못 보는 거예요. 내 돌이 단수인 줄 모르고 다른 곳에 두거나, 따낼 수 있는 상대 돌을 그냥 지나쳐요. 상대가 돌을 놓으면 그 돌이 내 돌의 활로를 줄였는지부터 살펴요.",
        ),
        // verify: captures black a 3
        // verify: captures black b 0
        // verify: captures black c 0
        // verify: after white a libs 3 target=D5
        Problem(
            prompt = "흑 차례예요. 판 위에 따낼 수 있는 백돌이 있어요. 찾아서 따내 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . O . X . .",
                    ". . . a . . . . .",
                    ". . X O X b O . .",
                    ". . X O O X . . .",
                    ". . . X X . O . .",
                    ". . . . c . . . .",
                    ". . . . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.POINT,
            solutions = "a",
            success = "맞아요. 백돌 셋의 활로는 a 하나뿐이었어요. 한꺼번에 셋을 따냈어요.",
            wrong = mapOf(
                "b" to "흑돌을 잇는 튼튼한 수예요. 하지만 지금은 백돌 셋을 따낼 기회예요. 백이 a로 늘면 활로가 3개로 늘어서 놓쳐요.",
                "c" to "2선은 낮아요. 게다가 백돌 셋을 따낼 기회를 놓쳐요. 백이 a로 늘면 활로가 3개로 늘어요.",
            ),
            fallback = "표시된 자리 가운데에서 골라 보세요.",
            hint = "가운데 백돌 셋의 활로를 세어 보세요.",
        ),
        // verify: territory black=21 white=14 neutral=0
        // verify: after black a territory black=20 white=14 neutral=0
        Explain(
            text = "두 번째 실수는 내 집을 스스로 메우는 거예요. 경계가 다 막힌 내 집 안에 돌을 두면 집이 한 집 줄어요. 흑이 a에 두면 21집이던 흑집이 20집이 돼요. 더 둘 곳이 없으면 집 안에 두지 말고 한 수 쉬어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . X O . .",
                    ". . . X O . .",
                    ". a . X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                    ". . . X O . .",
                ),
                caption = "내 집 안에 두면 집이 줄어요.",
            ),
        ),
        Quiz(
            question = "끝내기도 공배도 다 끝났어요. 남은 빈 자리는 내 집과 상대 집뿐이에요. 어떻게 할까요?",
            choices = listOf(
                "한 수 쉬어요",
                "내 집 안에 돌을 둬요",
                "상대 집 안에 돌을 둬요",
            ),
            answer = 0,
            explanation = "내 집 안에 두면 집이 한 집 줄어요. 상대 집 안에 두면 그 돌은 사석이 되어 상대에게 점수를 줘요. 더 둘 곳이 없으면 쉬는 것이 가장 좋아요.",
        ),
        Explain(
            text = "세 번째 실수는 끊기는 곳을 그냥 두는 거예요. 표시된 흑돌은 a에서 비스듬히 만나요. 백이 a에 두면 흑돌이 둘로 끊겨요. 끊긴 돌은 따로따로 쫓겨서 약해져요. 끊기는 곳이 보이면 먼저 이어요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . X . .",
                    ". . . B O . . . .",
                    ". . . B a O O . .",
                    ". . . O B B . . .",
                    ". . X . . . O . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
                caption = "a는 흑이 이을 자리이자 백이 끊을 자리예요.",
            ),
        ),
        Problem(
            prompt = "흑 차례예요. 표시된 흑돌이 끊기지 않게 이어 보세요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . O . . . X . .",
                    ". . . . O B . . .",
                    ". . O O a B . . .",
                    ". . . B B O . . .",
                    ". . O . b . X . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                ),
            ),
            toPlay = Side.BLACK,
            goal = Goal.CONNECT,
            solutions = "a",
            success = "맞아요. 흑돌 넷이 한 몸이 됐어요. 이제 끊길 걱정이 없어요.",
            wrong = mapOf(
                "b" to "아래쪽을 넓히는 수지만 더 급한 곳이 있어요. 백이 a에 두면 흑돌이 둘로 끊겨요.",
            ),
            fallback = "그 자리에 두면 백이 a에 두어 흑돌을 둘로 끊어요. 흑돌이 비스듬히 만나는 곳을 찾아보세요.",
            hint = "표시된 흑돌 넷 모두와 가까운 빈 자리가 있어요.",
        ),
        // verify: libs 1
        // verify: escape a no
        // verify: answer 축에 걸려서
        Quiz(
            question = "표시된 흑돌이 단수예요. a로 달아나면 어떻게 될까요?",
            choices = listOf(
                "축에 걸려서 더 많은 돌이 잡혀요",
                "활로가 늘어서 살아요",
            ),
            answer = 0,
            explanation = "백이 축으로 몰면 오른쪽 위 구석까지 몰려서 잡혀요. 그 길에 도와줄 흑돌이 없어요. 네 번째 실수는 살릴 수 없는 돌을 자꾸 살리려는 거예요. 그런 돌은 내버려 두고 큰 곳에 둬요.",
            diagram = Diagram(
                rows = listOf(
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". . . . . . . . .",
                    ". O a . . . . X .",
                    ". O B O . . X . .",
                    ". . O . . X . . .",
                    ". . . . . . . . .",
                ),
            ),
        ),
    ),
)

private fun lesson9x4() = Lesson(
    id = "c9-4",
    title = "AI와 첫 판 두기",
    summary = "대국 화면의 쓰임새를 알아보고 첫 판을 시작해요.",
    minutes = 4,
    steps = listOf(
        Explain(
            text = "첫 판은 9줄 판에서 레벨 1과 둬요. 홈에서 대국 시작을 누르고, 판 크기는 9줄, 상대는 레벨 1, 돌은 흑을 골라요. 흑이 먼저 두니까 첫 수는 여러분 차례예요. 화점이나 한가운데에 놓아 보세요.",
        ),
        Explain(
            text = "대국 화면 아래에는 도움 단추가 있어요. 힌트는 좋은 자리를 하나 알려 줘요. 형세는 지금 어느 쪽 집이 많은지 판 위에 보여 줘요. 무르기는 방금 둔 수를 되돌려요. 막힐 때마다 마음껏 써 보세요.",
        ),
        Explain(
            text = "더 둘 곳이 없으면 한 수 쉼을 눌러요. AI도 쉬면 대국이 끝나고 계가 화면이 나와요. 죽은 돌 표시를 확인하고 확정을 누르면 결과가 나와요. 처음에는 져도 괜찮아요. 끝까지 두어 보는 것이 가장 큰 공부예요.",
        ),
        Quiz(
            question = "끝내기와 공배까지 모두 끝나서 더 둘 곳이 없어요. 어떤 단추를 누를까요?",
            choices = listOf(
                "한 수 쉼",
                "기권",
                "무르기",
            ),
            answer = 0,
            explanation = "한 수 쉼을 누르면 돌을 놓지 않고 차례를 넘겨요. 두 사람이 연달아 쉬면 대국이 끝나고 계가를 해요. 기권은 졌다고 인정하고 그만두는 단추예요.",
        ),
        Quiz(
            question = "대국 중에 지금 누가 앞서고 있는지 궁금해요. 어떤 단추를 누를까요?",
            choices = listOf(
                "힌트",
                "형세",
                "한 수 쉼",
            ),
            answer = 1,
            explanation = "형세를 누르면 흑집과 백집이 될 곳을 판 위에 보여 주고, 어느 쪽이 몇 집 앞서는지 알려 줘요.",
        ),
        Explain(
            text = "입문 코스를 모두 마쳤어요. 활로와 단수, 연결과 끊기, 두 집과 급소, 포석과 끝내기, 계가까지 배웠어요. 이제 9줄 바둑판에서 레벨 1과 첫 판을 둬 보세요. 두기 전에 단수부터 살피는 것만 기억하면 돼요.",
        ),
    ),
)
