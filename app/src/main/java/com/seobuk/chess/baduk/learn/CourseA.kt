package com.seobuk.chess.baduk.learn

// 바둑 입문 코스 1~4장: 첫 만남, 활로와 따내기, 착수금지와 패, 연결과 끊기.
// 모든 도형, 수순, 문제의 정답은 verify/check_a.py (작은 바둑 규칙 구현)로 기계 검증했어요.
// 내용을 고치면 검증기를 다시 돌려요. 검증기가 읽을 수 있게 이름 붙인 인자와 한 줄 문자열만 써요.

internal val courseChaptersA: List<Chapter> = listOf(
    // ───────────── 1장 ─────────────
    Chapter(
        id = "ch1",
        number = 1,
        title = "바둑과 첫 만남",
        summary = "바둑판과 돌, 두는 순서, 그리고 바둑의 목표를 알아봐요.",
        lessons = listOf(
            Lesson(
                id = "c1-1",
                title = "바둑판과 돌",
                summary = "돌은 선과 선이 만나는 점에 놓아요. 흑이 먼저 둬요.",
                minutes = 3,
                steps = listOf(
                    Explain(
                        text = "바둑은 두 사람이 흑돌과 백돌을 나누어 쥐고 판 위에 놓아 가는 놀이예요. 판은 빈 채로 시작해요. 정식 바둑판은 19줄이지만, 여기서는 작은 9줄 판으로 배워요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                            caption = "가로 9줄, 세로 9줄인 9줄 바둑판이에요.",
                        ),
                    ),
                    Explain(
                        text = "돌은 네모 칸 안에 놓지 않아요. 선과 선이 만나는 점에 놓아요. 이 점을 '교차점'이라고 해요. 맨 끝 선 위나 구석 꼭짓점도 교차점이라서 돌을 놓을 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . O . .",
                                ". . . . . . . . .",
                                "X . . . X . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . O",
                            ),
                            caption = "돌은 모두 선이 만나는 점 위에 있어요. 왼쪽 끝 선과 오른쪽 아래 꼭짓점에도 놓였어요.",
                        ),
                    ),
                    Explain(
                        text = "판에는 자리마다 이름이 있어요. 구석 쪽은 '귀', 가장자리 쪽은 '변', 가운데 쪽은 '중앙'이라고 불러요. a 자리는 귀, b 자리는 변, c 자리는 중앙이에요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . a . . . . . .",
                                ". . . . . . . . .",
                                ". . . . c . . . .",
                                ". . . . . . . . .",
                                ". . . . b . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                            caption = "9줄 판에는 귀가 네 곳, 변이 네 곳 있어요.",
                        ),
                    ),
                    Explain(
                        text = "바둑은 흑이 먼저 둬요. 그다음에 백이 둬요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . a . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . b . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                        ),
                        sequence = "a b",
                        notes = listOf(
                            "흑이 첫 수를 귀에 뒀어요.",
                            "이어서 백이 맞은편 귀에 뒀어요.",
                        ),
                    ),
                    Quiz(
                        question = "돌은 어디에 놓을까요?",
                        choices = listOf("선과 선이 만나는 점", "네모 칸 안", "아무 데나"),
                        answer = 0,
                        explanation = "돌은 선이 만나는 교차점에 놓아요. 끝 선 위와 구석 꼭짓점도 교차점이에요.",
                    ),
                    Quiz(
                        question = "빈 판에서 첫 수는 누가 둘까요?",
                        choices = listOf("흑", "백", "아무나"),
                        answer = 0,
                        explanation = "바둑은 흑이 먼저 둬요. 그래서 흑돌을 쥔 사람이 첫 수를 둬요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 세 자리 가운데 귀에 있는 자리를 눌러 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . c . a . . . .",
                                ". . . . . . . . .",
                                ". . . . . . b . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "b",
                        success = "맞아요. 구석 쪽이 귀예요.",
                        wrong = mapOf(
                            "a" to "이 자리는 중앙이에요. 귀는 판의 구석 쪽이에요.",
                            "c" to "이 자리는 변이에요. 귀는 두 변이 만나는 구석 쪽이에요.",
                        ),
                        fallback = "표시된 세 자리 가운데에서 골라 보세요.",
                        hint = "귀는 판의 구석 쪽이에요.",
                    ),
                    Problem(
                        prompt = "흑과 백이 한 수씩 귀에 뒀어요. 흑 차례예요. 이번에는 표시된 자리 가운데 변에 있는 자리를 눌러 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . a . b . X . .",
                                ". . . . . . . . .",
                                ". . . . c . . . .",
                                ". . . . . . . . .",
                                ". . O . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "b",
                        success = "맞아요. 귀와 귀 사이의 가장자리 쪽이 변이에요.",
                        wrong = mapOf(
                            "a" to "이 자리는 귀예요. 변은 귀와 귀 사이의 가장자리 쪽이에요.",
                            "c" to "이 자리는 중앙이에요. 변은 판의 가장자리 쪽이에요.",
                        ),
                        fallback = "표시된 세 자리 가운데에서 골라 보세요.",
                        hint = "변은 귀와 귀 사이에 있어요.",
                    ),
                ),
            ),
            Lesson(
                id = "c1-2",
                title = "한 수씩 번갈아 둬요",
                summary = "흑과 백이 돌을 하나씩 번갈아 놓아요. 한 번 놓은 돌은 움직이지 않아요.",
                minutes = 3,
                steps = listOf(
                    Explain(
                        text = "흑과 백은 돌을 한 번에 하나씩, 번갈아 놓아요. 돌 하나를 놓는 것을 '한 수'라고 해요.",
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
                        notes = listOf(
                            "흑의 첫 수예요.",
                            "백이 둘째 수를 뒀어요.",
                            "다시 흑 차례예요. 셋째 수예요.",
                            "백의 넷째 수예요. 이렇게 한 수씩 번갈아 둬요.",
                        ),
                    ),
                    Explain(
                        text = "한 번 놓은 돌은 옮기지 않아요. 체스나 장기처럼 말을 움직이는 놀이가 아니에요. 돌은 놓인 자리에 그대로 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . O . . . X . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . O . . . X . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                            caption = "네 수를 둔 모양이에요. 이 돌들은 앞으로도 이 자리에 있어요.",
                        ),
                    ),
                    Explain(
                        text = "돌이 이미 있는 자리에는 둘 수 없어요. 비어 있는 교차점에만 놓아요. a 자리와 b 자리는 비어 있어서 둘 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . O . b . X . .",
                                ". . . . . . . . .",
                                ". . . . a . . . .",
                                ". . . . . . . . .",
                                ". . O . . . X . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                            caption = "돌이 놓인 네 자리에는 다시 둘 수 없어요.",
                        ),
                    ),
                    Explain(
                        text = "판 위의 돌이 없어지는 경우는 하나뿐이에요. 상대에게 잡혔을 때예요. 돌을 잡는 방법은 2장에서 배워요.",
                    ),
                    Explain(
                        text = "더 두고 싶은 곳이 없으면 쉬어도 돼요. 이것을 '한 수 쉼'이라고 해요. 두 사람이 연달아 쉬면 판이 끝나요.",
                    ),
                    Quiz(
                        question = "판 위에 흑돌이 3개, 백돌이 2개 있어요. 잡힌 돌은 없어요. 다음은 누구 차례일까요?",
                        choices = listOf("백 차례", "흑 차례"),
                        answer = 0,
                        explanation = "흑이 먼저 두고 한 수씩 번갈아 둬요. 흑돌이 하나 더 많으니 이번에는 백 차례예요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . O . . . X . .",
                                ". . . . . . . . .",
                                ". . . . X . . . .",
                                ". . . . . . . . .",
                                ". . O . . . X . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                        ),
                    ),
                    Quiz(
                        question = "이미 놓은 내 돌을 옆 자리로 옮겨도 될까요?",
                        choices = listOf("옮길 수 없어요", "한 칸은 옮길 수 있어요", "내 차례라면 옮길 수 있어요"),
                        answer = 0,
                        explanation = "한 번 놓은 돌은 움직이지 않아요. 그래서 한 수 한 수를 잘 생각하고 둬요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 세 자리 가운데 마음에 드는 곳에 돌을 놓아 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . a . . . X . .",
                                ". . . . . . . . .",
                                ". . . . c . . . .",
                                ". . . . . . . . .",
                                ". . O . b . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "a b c",
                        success = "좋아요. 비어 있는 교차점이면 어디든 둘 수 있어요. 귀도 변도 중앙도 괜찮아요.",
                        fallback = "이번에는 표시된 세 자리 가운데에서 골라 보세요.",
                        hint = "세 자리 모두 비어 있어요. 어디를 골라도 돼요.",
                    ),
                ),
            ),
            Lesson(
                id = "c1-3",
                title = "바둑의 목표는 집",
                summary = "내 돌로 둘러싼 빈 자리가 집이에요. 집이 많은 쪽이 이겨요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "바둑은 돌을 많이 잡는 놀이가 아니에요. '집'을 더 많이 지은 쪽이 이겨요. 집은 내 돌로 둘러싼 빈 자리예요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a b X O h",
                                "c d X O i",
                                "e X X O j",
                                "f X O O k",
                                "g X O l m",
                            ),
                            caption = "다 둔 판이에요. 왼쪽 색칠한 곳이 흑집, 오른쪽 색칠한 곳이 백집이에요.",
                            territoryBlack = "a b c d e f g",
                            territoryWhite = "h i j k l m",
                        ),
                    ),
                    Explain(
                        text = "빈 교차점 하나가 한 집이에요. 돌이 놓인 자리는 집으로 세지 않아요. 흑돌이 둘러싼 빈 자리는 여섯 곳이니까 6집이에요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a b c X . . .",
                                "d e f X . . .",
                                "X X X X . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                            caption = "색칠한 여섯 자리가 흑집이에요.",
                            territoryBlack = "a b c d e f",
                        ),
                    ),
                    Explain(
                        text = "판의 끝 선은 벽 역할을 해요. 그래서 귀에서는 돌을 적게 쓰고도 집을 지을 수 있어요. 똑같은 4집을 짓는 데 귀는 돌 4개, 변은 6개, 중앙은 8개가 들어요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a b X . . . . . .",
                                "c d X . . . . . .",
                                "X X . . . X X . .",
                                ". . . . X e f X .",
                                ". . . . X g h X .",
                                ". . . . . X X . .",
                                ". . X X . . . . .",
                                ". X i j X . . . .",
                                ". X k l X . . . .",
                            ),
                            caption = "귀, 중앙, 변에 지은 4집이에요.",
                            territoryBlack = "a b c d e f g h i j k l",
                        ),
                    ),
                    Explain(
                        text = "판이 끝나면 서로의 집을 세어 견줘요. 흑집은 7집, 백집은 6집이에요. 흑이 1집 많으니 흑이 이겨요. 실제 대국에서는 따낸 돌과 '덤'이라는 점수도 함께 세요. 8장에서 자세히 배워요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a b X O h",
                                "c d X O i",
                                "e X X O j",
                                "f X O O k",
                                "g X O l m",
                            ),
                            caption = "흑집 7집, 백집 6집이에요.",
                            territoryBlack = "a b c d e f g",
                            territoryWhite = "h i j k l m",
                        ),
                    ),
                    Quiz(
                        question = "색칠한 곳이 흑집이에요. 흑집은 몇 집일까요?",
                        choices = listOf("4집", "5집", "6집"),
                        answer = 1,
                        explanation = "색칠한 빈 자리를 하나씩 세면 다섯 곳이에요. 돌이 놓인 자리는 세지 않아요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a b c X .",
                                "d e X . .",
                                "X X . . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                            territoryBlack = "a b c d e",
                        ),
                    ),
                    Quiz(
                        question = "같은 크기의 집을 지을 때 돌이 가장 적게 드는 곳은 어디일까요?",
                        choices = listOf("귀", "변", "중앙"),
                        answer = 0,
                        explanation = "귀는 끝 선 두 개가 벽이 되어 줘요. 그래서 바둑은 보통 귀부터 둬요.",
                    ),
                    Quiz(
                        question = "판이 끝났어요. 위쪽은 흑집, 아래쪽은 백집이에요. 어느 쪽이 몇 집 많을까요?",
                        choices = listOf("흑이 3집 많아요", "백이 3집 많아요", "집 수가 같아요"),
                        answer = 0,
                        explanation = "흑집은 8집, 백집은 5집이에요. 흑이 3집 많아요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a b c d e",
                                "f g h X X",
                                "X X X X O",
                                "O O O O O",
                                "i j k l m",
                            ),
                            territoryBlack = "a b c d e f g h",
                            territoryWhite = "i j k l m",
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 흑의 울타리에 빈틈이 하나 있어요. 빈틈을 막아서 왼쪽을 흑집으로 만들어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . X O . .",
                                ". . . X O . .",
                                ". . . X O . .",
                                ". b . a O c .",
                                ". . . X O . .",
                                ". . . X O . .",
                                ". . . X O . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "a",
                        success = "맞아요. 울타리가 빈틈없이 이어져서 왼쪽이 모두 흑집이 됐어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 이미 흑의 울타리 안쪽이에요. 빈틈이 그대로라서 백이 그 틈으로 들어올 수 있어요.",
                            "c" to "이 자리는 백의 울타리 안쪽이에요. 백돌에 둘러싸여 잡히기 쉬워요. 내 울타리의 빈틈도 그대로예요.",
                        ),
                        fallback = "울타리의 빈틈이 그대로예요. 흑돌이 늘어선 줄에서 끊어진 자리를 찾아 보세요.",
                        hint = "흑돌이 세로로 늘어선 줄을 위에서 아래로 따라가 보세요.",
                    ),
                ),
            ),
        ),
    ),
    // ───────────── 2장 ─────────────
    Chapter(
        id = "ch2",
        number = 2,
        title = "활로와 따내기",
        summary = "돌의 숨구멍인 활로를 세고, 활로를 막아 돌을 따내는 법을 배워요.",
        lessons = listOf(
            Lesson(
                id = "c2-1",
                title = "활로 세기",
                summary = "돌 옆의 빈 자리가 활로예요. 가운데는 4개, 변은 3개, 귀는 2개예요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "돌 바로 옆, 선으로 이어진 빈 자리를 '활로'라고 해요. 돌이 숨을 쉬는 길이에요. 가운데에 놓인 흑돌의 활로는 a, b, c, d 네 개예요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . a . .",
                                ". b X c .",
                                ". . d . .",
                                ". . . . .",
                            ),
                        ),
                    ),
                    Explain(
                        text = "대각선 자리는 활로가 아니에요. a 자리는 돌과 선으로 바로 이어져 있어서 활로예요. b 자리는 비스듬히 떨어져 있어서 활로가 아니에요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . a b .",
                                ". . X . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                    ),
                    Explain(
                        text = "끝 선 위에 놓인 돌은 활로가 3개예요. 판 바깥으로는 길이 없기 때문이에요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". a X b .",
                                ". . c . .",
                                ". . . . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                            caption = "변의 흑돌은 활로가 a, b, c 세 개예요.",
                        ),
                    ),
                    Explain(
                        text = "구석 꼭짓점에 놓인 돌은 활로가 2개뿐이에요. 그래서 판 끝에 있는 돌일수록 잡히기 쉬워요.",
                        diagram = Diagram(
                            rows = listOf(
                                "X a . . .",
                                "b . . . .",
                                ". . . . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                            caption = "귀의 흑돌은 활로가 a, b 두 개예요.",
                        ),
                    ),
                    Explain(
                        text = "상대 돌이 옆에 붙으면 그 자리는 활로가 아니에요. 백돌이 오른쪽에 붙어서 흑돌의 활로는 a, b, c 세 개로 줄었어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . a . .",
                                ". b X O .",
                                ". . c . .",
                                ". . . . .",
                            ),
                        ),
                    ),
                    Quiz(
                        question = "아래쪽 끝 선에 있는 흑돌의 활로는 몇 개일까요?",
                        choices = listOf("1개", "2개", "3개"),
                        answer = 1,
                        explanation = "끝 선의 돌은 활로가 3개예요. 왼쪽에 백돌이 붙어서 위쪽과 오른쪽, 두 개가 남았어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . . . .",
                                ". . . . .",
                                ". . . . .",
                                ". O X . .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 백돌의 활로를 하나 골라 막아 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". d X e .",
                                ". a W b .",
                                ". f c g .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "a b c",
                        success = "맞아요. 백돌의 활로를 하나 막았어요. 위쪽은 이미 흑돌이 막고 있어서, 이제 백돌의 활로는 2개예요.",
                        wrong = mapOf(
                            "d" to "이 자리는 백돌의 대각선이에요. 선으로 바로 이어져 있지 않아서 활로가 아니에요.",
                            "e" to "이 자리는 백돌의 대각선이에요. 선으로 바로 이어져 있지 않아서 활로가 아니에요.",
                            "f" to "이 자리는 백돌의 대각선이에요. 선으로 바로 이어져 있지 않아서 활로가 아니에요.",
                            "g" to "이 자리는 백돌의 대각선이에요. 선으로 바로 이어져 있지 않아서 활로가 아니에요.",
                        ),
                        fallback = "이 자리는 백돌과 붙어 있지 않아요. 백돌 바로 옆, 선으로 이어진 빈 자리를 찾아 보세요.",
                        hint = "활로는 돌의 위, 아래, 왼쪽, 오른쪽 자리예요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 이번에는 끝 선에 있는 백돌이에요. 표시된 백돌의 활로를 하나 골라 막아 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                "a d . . .",
                                "W b f . .",
                                "c e . . .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "a b c",
                        success = "맞아요. 끝 선의 돌은 활로가 3개뿐이에요. 하나를 막았으니 이제 2개가 남았어요.",
                        wrong = mapOf(
                            "d" to "이 자리는 백돌의 대각선이에요. 선으로 바로 이어져 있지 않아서 활로가 아니에요.",
                            "e" to "이 자리는 백돌의 대각선이에요. 선으로 바로 이어져 있지 않아서 활로가 아니에요.",
                            "f" to "이 자리는 백돌에서 한 칸 떨어져 있어요. 활로는 돌 바로 옆 자리예요.",
                        ),
                        fallback = "이 자리는 백돌과 붙어 있지 않아요. 백돌 바로 옆, 선으로 이어진 빈 자리를 찾아 보세요.",
                        hint = "끝 선의 돌은 판 바깥쪽으로는 활로가 없어요. 나머지 세 방향을 보세요.",
                    ),
                ),
            ),
            Lesson(
                id = "c2-2",
                title = "단수",
                summary = "활로가 하나만 남은 돌은 단수예요. 다음 수에 잡힐 수 있어요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "활로가 하나만 남은 돌은 다음 수에 잡힐 수 있어요. 이런 상태를 '단수'라고 해요. 표시된 백돌의 활로는 a 하나뿐이에요. 백돌은 지금 단수예요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . X . .",
                                ". X W a .",
                                ". . X . .",
                                ". . . . .",
                            ),
                        ),
                    ),
                    Explain(
                        text = "활로가 2개인 돌의 활로를 하나 막으면 단수가 돼요. 이것을 '단수 친다'고 해요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . X . .",
                                ". X W a .",
                                ". . b . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 활로 하나를 막았어요. 백돌의 활로는 b 하나만 남았어요. 단수예요.",
                        ),
                    ),
                    Explain(
                        text = "끝 선의 돌은 활로가 3개예요. 그래서 돌 두 개로 단수가 돼요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . . . .",
                                ". . . . .",
                                ". . b . .",
                                ". X W a .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 옆을 막았어요. 백돌의 활로는 b 하나만 남았어요.",
                        ),
                    ),
                    Explain(
                        text = "구석 꼭짓점의 돌은 활로가 2개예요. 그래서 돌 하나로 단수가 돼요.",
                        diagram = Diagram(
                            rows = listOf(
                                "W a . . .",
                                "b . . . .",
                                ". . . . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑돌 하나로 단수예요. 백돌의 활로는 b 하나만 남았어요.",
                        ),
                    ),
                    Quiz(
                        question = "백돌이 세 개 있어요. 지금 단수인 백돌은 어느 것일까요?",
                        choices = listOf("왼쪽 위 백돌", "오른쪽 백돌", "아래쪽 백돌"),
                        answer = 1,
                        explanation = "오른쪽 백돌은 흑돌 셋에 막혀서 활로가 하나뿐이에요. 나머지 두 백돌은 활로가 2개씩 남아 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". X . . . . .",
                                "X O . . . . .",
                                ". . . . . X .",
                                ". . . . X O .",
                                ". . . . . X .",
                                ". . . . . . .",
                                ". X O . . . .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 백돌을 단수로 만들어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". c a d .",
                                ". b W X .",
                                ". e X . .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ATARI,
                        solutions = "a b",
                        success = "맞아요. 백돌의 활로가 하나만 남았어요. 활로가 2개인 돌은 어느 쪽을 막아도 단수예요.",
                        wrong = mapOf(
                            "c" to "이 자리는 백돌의 대각선이라 활로가 아니에요. 백돌의 활로는 여전히 2개예요.",
                            "d" to "이 자리는 백돌의 대각선이라 활로가 아니에요. 백돌의 활로는 여전히 2개예요.",
                            "e" to "이 자리는 백돌의 대각선이라 활로가 아니에요. 백돌의 활로는 여전히 2개예요.",
                        ),
                        fallback = "백돌의 활로는 여전히 2개예요. 백돌 바로 옆의 빈 자리를 막아 보세요.",
                        hint = "백돌의 활로는 지금 2개예요. 그 가운데 하나를 막으면 돼요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 끝 선에 있는 백돌을 단수로 만들어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . . c X",
                                ". . . a W",
                                ". . . d b",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ATARI,
                        solutions = "a b",
                        success = "맞아요. 끝 선의 돌은 활로가 적어서 금방 단수가 돼요.",
                        wrong = mapOf(
                            "c" to "이 자리는 백돌의 대각선이라 활로가 아니에요. 백돌의 활로는 여전히 2개예요.",
                            "d" to "이 자리는 백돌의 대각선이라 활로가 아니에요. 백돌의 활로는 여전히 2개예요.",
                        ),
                        fallback = "백돌의 활로는 여전히 2개예요. 백돌 바로 옆의 빈 자리를 막아 보세요.",
                        hint = "끝 선의 백돌에 남은 활로는 왼쪽과 아래쪽이에요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 백돌을 단수로 만들어 보세요. 단수를 친 내 돌의 활로도 함께 세어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . X O .",
                                ". X W a O",
                                ". . b c .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ATARI,
                        solutions = "b",
                        success = "맞아요. 백돌은 단수가 됐고, 내 돌은 활로가 3개라서 안전해요.",
                        wrong = mapOf(
                            "a" to "이 자리에 두면 백돌은 단수가 되지만, 내 돌도 활로가 하나뿐이에요. 백이 먼저 내 돌을 잡을 수 있어요.",
                            "c" to "이 자리는 백돌의 대각선이라 활로가 아니에요. 백돌의 활로는 여전히 2개예요.",
                        ),
                        fallback = "백돌의 활로는 여전히 2개예요. 백돌 바로 옆의 빈 자리를 막아 보세요.",
                        hint = "단수를 친 내 돌의 활로가 2개 이상인 자리를 골라요.",
                    ),
                ),
            ),
            Lesson(
                id = "c2-3",
                title = "따내기",
                summary = "단수인 돌의 마지막 활로를 막으면 그 돌을 판에서 들어내요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "단수인 돌의 마지막 활로를 막으면 그 돌을 판에서 들어내요. 이것을 '따낸다'고 해요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . X . .",
                                ". X W a .",
                                ". . X . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 마지막 활로를 막았어요. 백돌을 따냈어요.",
                        ),
                    ),
                    Explain(
                        text = "따낸 돌은 내가 가져요. 판이 끝나고 점수를 셀 때 따낸 돌 하나가 한 점이 돼요.",
                    ),
                    Explain(
                        text = "끝 선에서는 돌 3개, 구석에서는 돌 2개로 따낼 수 있어요. 활로가 적기 때문이에요. 흑이 a에 두면 귀의 백돌을, b에 두면 변의 백돌을 따내요.",
                        diagram = Diagram(
                            rows = listOf(
                                "O X . . .",
                                "a . . . .",
                                ". . . . .",
                                ". . b . .",
                                ". X O X .",
                            ),
                        ),
                    ),
                    Explain(
                        text = "상대 돌이 단수인지 늘 살펴봐요. 내가 따내지 않으면 상대가 달아날 수 있어요. 따낼 기회는 한 수 만에 사라지기도 해요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . X . .",
                                ". X W a .",
                                ". . X . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 먼저 달아났어요. 백돌 둘의 활로가 3개로 늘었어요. 따낼 기회가 사라졌어요.",
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 백돌을 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". b a c .",
                                ". X W X .",
                                ". . X . .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CAPTURE,
                        solutions = "a",
                        success = "맞아요. 마지막 활로를 막아서 백돌을 따냈어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 백돌의 대각선이에요. 백돌의 마지막 활로는 그대로 남아 있어요.",
                            "c" to "이 자리는 백돌의 대각선이에요. 백돌의 마지막 활로는 그대로 남아 있어요.",
                        ),
                        fallback = "백돌은 아직 판 위에 있어요. 백돌 바로 옆에 하나 남은 빈 자리를 찾아 보세요.",
                        hint = "백돌의 위, 아래, 왼쪽, 오른쪽 가운데 비어 있는 곳은 한 곳이에요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 끝 선에 있는 백돌을 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". X W X .",
                                ". b a c .",
                                ". . . . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CAPTURE,
                        solutions = "a",
                        success = "맞아요. 끝 선의 돌은 돌 3개로 따낼 수 있어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 백돌의 대각선이에요. 백돌의 활로를 막지 못해요.",
                            "c" to "이 자리는 백돌의 대각선이에요. 백돌의 활로를 막지 못해요.",
                        ),
                        fallback = "백돌은 아직 판 위에 있어요. 백돌 바로 옆에 하나 남은 빈 자리를 찾아 보세요.",
                        hint = "백돌의 왼쪽과 오른쪽은 이미 막혀 있어요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 백돌이 둘 있어요. 표시된 귀의 백돌을 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . . . .",
                                ". . . . .",
                                ". X . . X",
                                "c O b a W",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CAPTURE,
                        solutions = "a",
                        success = "맞아요. 귀의 돌은 활로가 2개뿐이라 돌 2개로 따낼 수 있어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 표시되지 않은 백돌의 활로예요. 그 백돌은 활로가 하나 더 남아서 아직 따낼 수 없어요.",
                            "c" to "이 자리는 표시되지 않은 백돌의 활로예요. 그 백돌은 활로가 하나 더 남아서 아직 따낼 수 없어요.",
                        ),
                        fallback = "표시된 백돌은 아직 판 위에 있어요. 그 돌 바로 옆에 하나 남은 빈 자리를 찾아 보세요.",
                        hint = "표시된 백돌의 위쪽은 이미 흑돌이 막고 있어요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 백돌 둘을 한 수로 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". X b X .",
                                "X W a W X",
                                ". X c X .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CAPTURE,
                        solutions = "a",
                        success = "맞아요. 두 백돌이 같은 자리를 마지막 활로로 쓰고 있었어요. 한 수로 둘 다 따냈어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 두 백돌의 대각선이에요. 백돌의 활로를 막지 못해요.",
                            "c" to "이 자리는 두 백돌의 대각선이에요. 백돌의 활로를 막지 못해요.",
                        ),
                        fallback = "백돌은 아직 판 위에 있어요. 두 백돌에 함께 붙어 있는 빈 자리를 찾아 보세요.",
                        hint = "두 백돌의 활로가 겹치는 자리를 찾아 보세요.",
                    ),
                ),
            ),
            Lesson(
                id = "c2-4",
                title = "이어진 돌은 한 몸",
                summary = "선으로 이어진 같은 색 돌은 활로를 함께 쓰고, 함께 잡혀요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "같은 색 돌이 선으로 이어져 있으면 한 몸이에요. 활로도 함께 써요. 나란히 놓인 흑돌 둘의 활로는 모두 6개예요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". a b . .",
                                "c X X d .",
                                ". e f . .",
                                ". . . . .",
                            ),
                            caption = "활로는 a, b, c, d, e, f 여섯 자리예요.",
                        ),
                    ),
                    Explain(
                        text = "대각선으로 놓인 돌은 이어진 것이 아니에요. 서로 다른 몸이라서 활로도 따로 세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". X . . .",
                                ". . X . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                            caption = "두 흑돌은 아직 한 몸이 아니에요. 활로는 각각 4개예요.",
                        ),
                    ),
                    Explain(
                        text = "한 몸인 돌은 함께 잡혀요. 활로가 모두 막히면 한꺼번에 들어내요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . X . .",
                                ". X W X .",
                                ". X W X .",
                                ". . a . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "두 돌의 마지막 활로를 막았어요. 백돌 둘을 한꺼번에 따냈어요.",
                        ),
                    ),
                    Explain(
                        text = "돌이 여러 개면 활로를 빠뜨리기 쉬워요. 돌마다 옆 자리를 하나씩 짚어 가며 세요. 두 돌에 함께 붙은 자리는 한 번만 세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". a b . .",
                                "c X X d .",
                                "e X f . .",
                                ". g . . .",
                                ". . . . .",
                            ),
                            caption = "활로는 a, b, c, d, e, f, g 일곱 자리예요. f 자리는 두 돌에 함께 붙어 있지만 한 번만 세요.",
                        ),
                    ),
                    Quiz(
                        question = "끝 선에 백돌 셋이 이어져 있어요. 이 백돌들의 활로는 모두 몇 개일까요?",
                        choices = listOf("3개", "4개", "5개"),
                        answer = 1,
                        explanation = "위쪽 세 자리와 오른쪽 한 자리, 모두 4개예요. 왼쪽은 흑돌이 막고 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". X O O O . .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 이어진 백돌 둘을 한꺼번에 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                "X . . . .",
                                "W X . . .",
                                "W X . . .",
                                "a b . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CAPTURE,
                        solutions = "a",
                        success = "맞아요. 두 돌이 함께 쓰던 마지막 활로를 막았어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 백돌의 대각선이에요. 백돌의 마지막 활로는 그대로예요.",
                        ),
                        fallback = "백돌은 아직 판 위에 있어요. 두 백돌 옆에 하나 남은 빈 자리를 찾아 보세요.",
                        hint = "두 백돌 옆의 자리를 하나씩 짚어 보세요. 비어 있는 곳은 한 곳뿐이에요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 이어진 백돌 둘을 단수로 만들어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". X X c .",
                                "X W W a .",
                                ". X b d .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ATARI,
                        solutions = "a b",
                        success = "맞아요. 백돌 둘의 활로가 하나만 남았어요. 한 몸인 돌은 함께 단수가 돼요.",
                        wrong = mapOf(
                            "c" to "이 자리는 백돌의 대각선이에요. 백돌의 활로는 여전히 2개예요.",
                            "d" to "이 자리는 백돌의 대각선이에요. 백돌의 활로는 여전히 2개예요.",
                        ),
                        fallback = "백돌의 활로는 여전히 2개예요. 두 백돌 옆의 빈 자리를 막아 보세요.",
                        hint = "두 백돌이 함께 쓰는 활로는 지금 2개예요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 귀에 있는 백돌 셋을 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                "W W X . .",
                                "W a b . .",
                                "X c . . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CAPTURE,
                        solutions = "a",
                        success = "맞아요. 세 돌이 함께 쓰던 마지막 활로를 막아서 한꺼번에 따냈어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 백돌과 붙어 있지 않아요. 백돌의 마지막 활로는 그대로예요.",
                            "c" to "이 자리는 백돌과 붙어 있지 않아요. 백돌의 마지막 활로는 그대로예요.",
                        ),
                        fallback = "백돌은 아직 판 위에 있어요. 세 백돌 옆에 하나 남은 빈 자리를 찾아 보세요.",
                        hint = "백돌 셋 옆의 자리를 하나씩 짚어 보세요.",
                    ),
                ),
            ),
            Lesson(
                id = "c2-5",
                title = "단수에서 달아나기",
                summary = "내 돌이 단수에 걸리면 활로를 늘려요. 늘거나, 단수 친 돌을 따내요.",
                minutes = 5,
                steps = listOf(
                    Explain(
                        text = "표시된 흑돌이 단수에 걸렸어요. 가만히 두면 다음 수에 잡혀요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O . .",
                                ". O B a .",
                                ". . O . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 마지막 활로를 막아서 흑돌을 따냈어요.",
                        ),
                    ),
                    Explain(
                        text = "남은 활로 쪽으로 돌을 하나 이어 두면 활로가 늘어요. 이것을 '는다'고 해요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O . .",
                                ". O B a .",
                                ". . O . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 늘었어요. 흑돌 둘은 한 몸이 되고 활로는 3개예요. 당장은 안전해요.",
                        ),
                    ),
                    Explain(
                        text = "달아나기 전에, 달아난 뒤의 활로를 미리 세어 봐요. 끝 선 쪽으로 늘면 활로가 잘 늘지 않아요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . . . .",
                                ". . O . .",
                                ". O B O .",
                                ". b a c d",
                            ),
                        ),
                        sequence = "a b c d",
                        notes = listOf(
                            "흑이 끝 선으로 늘었어요. 활로는 2개뿐이에요.",
                            "백이 다시 단수를 쳤어요.",
                            "흑이 또 늘었지만 활로는 여전히 하나예요.",
                            "백이 마지막 활로를 막아서 흑돌 셋을 모두 따냈어요.",
                        ),
                    ),
                    Explain(
                        text = "단수 친 상대 돌을 따낼 수 있다면 따내는 것도 좋은 방법이에요. 표시된 흑돌이 단수예요. 그런데 오른쪽 백돌도 단수예요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O X .",
                                ". O B O X",
                                ". . . a .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 백돌을 따냈어요. 표시된 흑돌의 활로가 2개로 늘었어요.",
                        ),
                    ),
                    Quiz(
                        question = "흑돌이 단수예요. 흑이 a로 늘면 흑돌 둘의 활로는 몇 개가 될까요?",
                        choices = listOf("1개", "2개", "3개"),
                        answer = 1,
                        explanation = "a에 늘면 활로는 위쪽과 아래쪽, 두 개예요. 오른쪽은 백돌이 막고 있어요. 활로가 2개뿐이라 백이 다시 단수를 칠 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". O . . .",
                                "O X a O .",
                                ". O . . .",
                                ". . . . .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 흑돌이 단수예요. 달아나 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . O b . .",
                                ". . O B a . .",
                                ". . . O c . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ESCAPE,
                        solutions = "a",
                        success = "맞아요. 흑돌 둘이 한 몸이 되어 활로가 3개로 늘었어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 흑돌과 이어지지 않아요. 흑돌은 여전히 단수라서 다음 수에 잡혀요.",
                            "c" to "이 자리는 흑돌과 이어지지 않아요. 흑돌은 여전히 단수라서 다음 수에 잡혀요.",
                        ),
                        fallback = "표시된 흑돌은 여전히 단수예요. 백이 마지막 활로를 막으면 잡혀요. 흑돌의 남은 활로를 찾아 보세요.",
                        hint = "흑돌에게 남은 활로는 하나예요. 그 자리에 돌을 이어 두세요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 흑돌이 단수예요. 늘 자리는 끝 선이라 좁아요. 다른 방법으로 살려 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . . O X . .",
                                ". . O B O X .",
                                ". . O b a . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ESCAPE,
                        solutions = "a",
                        success = "맞아요. 단수 친 백돌을 따내서 활로가 생겼어요. 늘 곳이 마땅치 않으면 따낼 수 있는 돌이 있는지 살펴봐요.",
                        wrong = mapOf(
                            "b" to "이렇게 늘어도 활로는 하나뿐이에요. 백이 그 자리를 막으면 흑돌 둘이 함께 잡혀요.",
                        ),
                        fallback = "표시된 흑돌은 여전히 단수예요. 흑돌을 둘러싼 백돌 가운데 약한 돌을 찾아 보세요.",
                        hint = "흑돌을 둘러싼 백돌 가운데 단수인 돌이 있어요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 이어진 흑돌 둘이 단수예요. 달아나 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . O O b . .",
                                ". O B B a . .",
                                ". . O O c . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ESCAPE,
                        solutions = "a",
                        success = "맞아요. 세 돌이 한 몸이 되어 활로가 3개예요.",
                        wrong = mapOf(
                            "b" to "이 자리는 흑돌과 이어지지 않아요. 흑돌 둘은 여전히 단수라서 다음 수에 함께 잡혀요.",
                            "c" to "이 자리는 흑돌과 이어지지 않아요. 흑돌 둘은 여전히 단수라서 다음 수에 함께 잡혀요.",
                        ),
                        fallback = "흑돌 둘은 여전히 단수예요. 두 돌이 함께 쓰는 마지막 활로를 찾아 보세요.",
                        hint = "두 흑돌 옆의 자리를 하나씩 짚어 보세요. 비어 있는 곳은 한 곳뿐이에요.",
                    ),
                ),
            ),
        ),
    ),
    // ───────────── 3장 ─────────────
    Chapter(
        id = "ch3",
        number = 3,
        title = "둘 수 없는 곳과 패",
        summary = "돌을 놓을 수 없는 자리와, 같은 모양이 되풀이되는 패의 규칙을 배워요.",
        lessons = listOf(
            Lesson(
                id = "c3-1",
                title = "착수금지",
                summary = "놓자마자 활로가 하나도 없는 자리에는 둘 수 없어요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "a 자리는 사방이 백돌이에요. 흑이 여기에 두면 놓자마자 활로가 하나도 없어요. 이런 자리에는 돌을 놓을 수 없어요. '착수금지'라고 해요. 스스로 잡히러 들어가는 수라서 '자살수'라고도 불러요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O . .",
                                ". O a O .",
                                ". . O . .",
                                ". . . . .",
                            ),
                        ),
                    ),
                    Explain(
                        text = "귀와 변에서는 돌 몇 개만으로도 착수금지가 생겨요. a 자리는 백돌 둘에, b 자리는 백돌 셋에 막혀 있어요. 흑은 두 자리 모두 둘 수 없어요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a O . . .",
                                "O . . . .",
                                ". . . . .",
                                ". . O . .",
                                ". O b O .",
                            ),
                        ),
                    ),
                    Explain(
                        text = "같은 자리라도 백에게는 착수금지가 아니에요. 백이 a에 두면 옆의 백돌과 이어져서 활로를 함께 쓰기 때문이에요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O . .",
                                ". O a O .",
                                ". . O . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        first = Side.WHITE,
                        notes = listOf(
                            "백돌 다섯이 한 몸이 됐어요. 활로가 넉넉해요.",
                        ),
                    ),
                    Explain(
                        text = "내 돌과 이어지는 자리라도, 이은 뒤에 활로가 하나도 없으면 둘 수 없어요. 흑돌 둘의 활로는 a 하나예요. 흑이 a에 두면 세 돌 모두 활로가 없어져요. 그래서 a는 흑의 착수금지예요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". O O O .",
                                "O X X a O",
                                ". O O O .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                    ),
                    Quiz(
                        question = "흑 차례예요. 흑이 둘 수 없는 자리는 어디일까요?",
                        choices = listOf("a 자리", "b 자리", "c 자리"),
                        answer = 0,
                        explanation = "a 자리는 사방이 백돌이라 활로가 없어요. b 자리는 백돌 옆이지만 빈 자리가 붙어 있어서 활로가 있어요. c 자리도 마찬가지예요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O . .",
                                ". O a O .",
                                ". b O . .",
                                ". . . c .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 세 자리 가운데 흑이 둘 수 있는 곳을 눌러 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a O . . .",
                                "O . . c O",
                                ". . O O .",
                                ". O b O .",
                                ". . O . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "c",
                        success = "맞아요. 백돌이 옆에 있어도 빈 자리가 붙어 있으면 활로가 있어요. 그래서 둘 수 있어요.",
                        wrong = mapOf(
                            "a" to "귀의 이 자리는 백돌 둘에 막혀 있어요. 활로가 없어서 둘 수 없어요.",
                            "b" to "이 자리는 사방이 백돌이에요. 활로가 없어서 둘 수 없어요.",
                        ),
                        fallback = "이번에는 표시된 세 자리 가운데에서 골라 보세요.",
                        hint = "돌을 놓았을 때 바로 옆에 빈 자리가 하나라도 있는지 보세요.",
                    ),
                    Problem(
                        prompt = "이번에는 백 차례예요. 표시된 두 자리 가운데 백이 둘 수 있는 곳을 눌러 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". X . . .",
                                "X a X . .",
                                ". X . O .",
                                ". . O b O",
                                ". . . O .",
                            ),
                        ),
                        toPlay = Side.WHITE,
                        goal = Goal.POINT,
                        solutions = "b",
                        success = "맞아요. 자기 돌 사이의 빈 자리는 둘 수 있어요. 옆의 돌과 이어져서 활로를 함께 써요.",
                        wrong = mapOf(
                            "a" to "이 자리는 사방이 흑돌이에요. 백에게는 활로가 없는 착수금지예요.",
                        ),
                        fallback = "이번에는 표시된 두 자리 가운데에서 골라 보세요.",
                        hint = "백돌을 놓았을 때 같은 색 돌과 이어지는 자리를 찾아 보세요.",
                    ),
                ),
            ),
            Lesson(
                id = "c3-2",
                title = "따내면 둘 수 있어요",
                summary = "활로가 없는 자리라도 상대 돌을 따내는 수라면 둘 수 있어요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "a 자리는 백돌에 막혀 있어서 착수금지처럼 보여요. 하지만 백돌 셋의 활로도 a 하나뿐이에요. 이럴 때는 흑이 a에 둘 수 있어요. 상대 돌을 따내는 수는 둘 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a O X . .",
                                "O O X . .",
                                "X X . . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "백돌 셋을 따냈어요. 따낸 자리가 비어서 흑돌에게 활로가 생겼어요.",
                        ),
                    ),
                    Explain(
                        text = "순서를 기억해요. 돌을 놓으면 먼저 활로가 없어진 상대 돌을 들어내요. 그다음에 내 돌의 활로를 봐요.",
                        diagram = Diagram(
                            rows = listOf(
                                "X . X . .",
                                ". . X . .",
                                "X X . . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                            caption = "백돌을 들어낸 뒤의 모양이에요. 귀의 흑돌은 활로가 2개예요.",
                        ),
                    ),
                    Explain(
                        text = "이번에는 백돌에게 바깥 활로 b가 남아 있어요. 지금 흑이 a에 두면 백돌을 따내지 못하니까 착수금지예요. 바깥 활로 b를 먼저 막으면, 그다음에는 a에 둘 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                "a O b . .",
                                "O O X . .",
                                "X X . c .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "b c a",
                        notes = listOf(
                            "흑이 바깥 활로를 막았어요. 백돌은 단수예요.",
                            "백이 다른 곳에 뒀어요.",
                            "이제는 따내는 수라서 둘 수 있어요. 흑이 백돌 셋을 따냈어요.",
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 백돌 셋을 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . . . .",
                                ". . b X X",
                                ". . X W W",
                                ". . X W a",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CAPTURE,
                        solutions = "a",
                        success = "맞아요. 활로가 없어 보이는 자리지만 백돌 셋을 따내는 수라서 둘 수 있어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 백돌과 붙어 있지 않아요. 바깥은 이미 흑돌이 모두 막고 있어요.",
                        ),
                        fallback = "백돌은 아직 판 위에 있어요. 착수금지처럼 보여도 따내는 수라면 둘 수 있어요.",
                        hint = "백돌 셋의 활로가 몇 개 남았는지 세어 보세요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 백돌 다섯이 빈 자리 하나를 품고 있어요. 표시된 백돌을 모두 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". X W a W X .",
                                ". X W W W X .",
                                ". b X X X c .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CAPTURE,
                        solutions = "a",
                        success = "맞아요. 안쪽 빈 자리가 백돌의 마지막 활로였어요. 백돌 다섯을 한꺼번에 따냈어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 백돌의 대각선이에요. 백돌의 활로를 막지 못해요.",
                            "c" to "이 자리는 백돌의 대각선이에요. 백돌의 활로를 막지 못해요.",
                        ),
                        fallback = "백돌은 아직 판 위에 있어요. 바깥은 흑돌이 모두 막고 있어요. 남은 활로는 어디일까요?",
                        hint = "백돌에 붙어 있는 빈 자리는 한 곳뿐이에요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 귀마다 백돌 셋이 있어요. a와 b 가운데 흑이 지금 둘 수 있는 자리는 어디일까요?",
                        diagram = Diagram(
                            rows = listOf(
                                "a O X . . . .",
                                "O O X . . . .",
                                "X X . . . . .",
                                ". . . . . . .",
                                ". . . . . X .",
                                ". . . . X O O",
                                ". . . . X O b",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "a",
                        success = "맞아요. 왼쪽 위 백돌은 활로가 하나뿐이라 따낼 수 있어요. 따내는 수는 둘 수 있어요.",
                        wrong = mapOf(
                            "b" to "이 자리에 두어도 백돌을 따내지 못해요. 백돌에게 바깥 활로가 하나 남아 있어요. 그래서 지금은 착수금지예요.",
                        ),
                        fallback = "이번에는 표시된 두 자리 가운데에서 골라 보세요.",
                        hint = "두 귀의 백돌 활로를 각각 세어 보세요.",
                    ),
                    Quiz(
                        question = "b 자리는 지금 흑의 착수금지예요. 흑이 b에 둘 수 있으려면 먼저 무엇을 해야 할까요?",
                        choices = listOf("c 자리를 먼저 막아요", "한 수 쉬어요", "방법이 없어요"),
                        answer = 0,
                        explanation = "c는 백돌의 바깥 활로예요. c를 막으면 백돌의 활로는 b 하나만 남아요. 그러면 b는 따내는 수가 되어 둘 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . . . .",
                                ". . . X c",
                                ". . X O O",
                                ". . X O b",
                            ),
                        ),
                    ),
                ),
            ),
            Lesson(
                id = "c3-3",
                title = "패의 모양",
                summary = "서로 돌 하나를 따내고 되따낼 수 있는 모양이 패예요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "표시된 백돌은 단수예요. 흑이 a에 두면 백돌 하나를 따낼 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". X O . .",
                                "X W a O .",
                                ". X O . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 백돌 하나를 따냈어요. 그런데 방금 둔 흑돌의 활로도 하나뿐이에요.",
                        ),
                    ),
                    Explain(
                        text = "따낸 뒤의 모양이에요. 이번에는 표시된 흑돌이 단수예요. 만약 백이 곧바로 a에 둔다면 흑돌을 되따내고, 처음 모양으로 돌아가요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". X O . .",
                                "X a B O .",
                                ". X O . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 되따내면 처음과 똑같은 모양이에요.",
                        ),
                    ),
                    Explain(
                        text = "이렇게 서로 돌 하나를 따내고 되따낼 수 있는 모양을 '패'라고 해요. 서로 따내기만 되풀이하면 판이 끝나지 않아요. 그래서 패에는 특별한 규칙이 있어요. 다음 레슨에서 배워요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". X O . .",
                                "X O . O .",
                                ". X O . .",
                                ". . . . .",
                            ),
                            caption = "패 모양이에요. 흑돌과 백돌이 서로 맞물려 있어요.",
                        ),
                    ),
                    Explain(
                        text = "패는 끝 선에서도 생겨요. 끝 선에서는 더 적은 돌로 패가 돼요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . X W a O .",
                                ". . . X O . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 끝 선의 백돌 하나를 따냈어요. 방금 둔 흑돌도 단수예요. 패 모양이에요.",
                        ),
                    ),
                    Quiz(
                        question = "흑이 a나 b에 두면 백돌 하나를 따내요. 패 모양은 어느 쪽일까요?",
                        choices = listOf("a가 있는 왼쪽 모양", "b가 있는 오른쪽 모양"),
                        answer = 0,
                        explanation = "왼쪽은 따낸 흑돌이 곧바로 단수가 되어 백이 되따낼 수 있는 모양이에요. 오른쪽은 따낸 흑돌의 활로가 4개나 돼서 백이 되따낼 수 없어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". X O . . . X . .",
                                "X O a O . X O b .",
                                ". X O . . . X . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                                ". . . . . . . . .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 패 모양이에요. 표시된 백돌을 따내서 패를 시작해 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . X . .",
                                ". X W X .",
                                ". O a O .",
                                ". b O c .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.KO,
                        solutions = "a",
                        success = "맞아요. 백돌 하나를 따냈어요. 방금 둔 흑돌도 단수라서 패가 됐어요.",
                        wrong = mapOf(
                            "b" to "이 자리에서는 아무 돌도 따내지 못해요. 단수인 백돌의 마지막 활로를 찾아 보세요.",
                            "c" to "이 자리에서는 아무 돌도 따내지 못해요. 단수인 백돌의 마지막 활로를 찾아 보세요.",
                        ),
                        fallback = "표시된 백돌은 아직 판 위에 있어요. 그 돌의 마지막 활로를 찾아 보세요.",
                        hint = "표시된 백돌의 활로는 하나뿐이에요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 이번에는 끝 선의 패예요. 표시된 백돌을 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . c . . .",
                                ". . b O X . .",
                                ". . O a W X .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.KO,
                        solutions = "a",
                        success = "맞아요. 끝 선에서도 돌 하나를 따내고 되따낼 수 있는 패가 생겨요.",
                        wrong = mapOf(
                            "b" to "이 자리에서는 아무 돌도 따내지 못해요. 단수인 백돌의 마지막 활로를 찾아 보세요.",
                            "c" to "이 자리에서는 아무 돌도 따내지 못해요. 단수인 백돌의 마지막 활로를 찾아 보세요.",
                        ),
                        fallback = "표시된 백돌은 아직 판 위에 있어요. 그 돌의 마지막 활로를 찾아 보세요.",
                        hint = "표시된 백돌의 오른쪽과 위쪽은 흑돌이에요. 남은 쪽은 어디일까요?",
                    ),
                ),
            ),
            Lesson(
                id = "c3-4",
                title = "패의 규칙",
                summary = "상대가 패를 따낸 바로 다음 수에는 되따낼 수 없어요. 다른 곳에 한 수 둔 뒤에 따내요.",
                minutes = 5,
                steps = listOf(
                    Explain(
                        text = "흑이 방금 표시된 돌로 패를 따냈어요. 백이 곧바로 a에 두어 되따내면 같은 모양이 끝없이 되풀이돼요. 그래서 규칙이 있어요. 상대가 패를 따낸 바로 다음 수에는 되따낼 수 없어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . X O . . .",
                                ". X a B O . .",
                                ". . X O . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                            caption = "백은 지금 a에 둘 수 없어요.",
                        ),
                    ),
                    Explain(
                        text = "다른 곳에 한 수를 둔 다음에는 되따낼 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . b .",
                                ". . X O . . .",
                                ". X a B O . .",
                                ". . X O . . .",
                                ". c . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "b c a",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 다른 곳에 뒀어요.",
                            "흑도 다른 곳에 뒀어요.",
                            "한 수를 건넜으니 이제 백이 패를 되따낼 수 있어요.",
                        ),
                    ),
                    Explain(
                        text = "그런데 흑은 그사이에 a에 이어서 패를 끝낼 수도 있어요. 패를 이으면 더는 따낼 돌이 없어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . b .",
                                ". . X O . . .",
                                ". X a B O . .",
                                ". . X O . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "b a",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 다른 곳에 뒀어요.",
                            "흑이 패를 이었어요. 패가 끝났어요.",
                        ),
                    ),
                    Explain(
                        text = "그래서 백은 흑이 꼭 받아야 하는 곳에 둬요. 이런 수를 '팻감'이라고 해요. 흑이 팻감을 받으면 그때 패를 되따내요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . X O . . .",
                                ". X a B O . .",
                                ". . X O . c .",
                                ". . . . O X b",
                                ". . . . . O .",
                            ),
                        ),
                        sequence = "b c a",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 아래쪽 흑돌을 단수로 몰았어요. 팻감이에요.",
                            "흑이 돌을 살리려고 늘었어요. 팻감을 받았어요.",
                            "백이 패를 되따냈어요. 이번에는 흑이 팻감을 찾을 차례예요.",
                        ),
                    ),
                    Explain(
                        text = "흑이 팻감을 받지 않고 패를 이으면 어떻게 될까요? 흑은 패를 끝내지만, 백은 그 대가로 단수 친 흑돌을 따내요. 패와 팻감 가운데 어느 쪽이 더 큰지 견줘서 정해요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . X O . . .",
                                ". X a B O . .",
                                ". . X O . c .",
                                ". . . . O X b",
                                ". . . . . O .",
                            ),
                        ),
                        sequence = "b a c",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 팻감을 썼어요.",
                            "흑이 받지 않고 패를 이었어요.",
                            "백이 흑돌 하나를 따냈어요. 서로 하나씩 얻었어요.",
                        ),
                    ),
                    Quiz(
                        question = "백이 방금 표시된 돌로 패를 따냈어요. 흑 차례예요. 흑이 지금 둘 수 없는 자리는 어디일까요?",
                        choices = listOf("a 자리", "b 자리", "c 자리"),
                        answer = 0,
                        explanation = "a는 패를 되따내는 자리예요. 상대가 패를 따낸 바로 다음 수에는 둘 수 없어요. 다른 곳에 한 수 둔 뒤에는 둘 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . b .",
                                ". . O X . . .",
                                ". O a W X . .",
                                ". . O X . . .",
                                ". c . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "백이 방금 패를 따냈어요. 흑은 바로 되따낼 수 없어요. 오른쪽 아래의 표시된 백돌을 단수로 몰아서 팻감을 써 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . O X . . .",
                                ". O a O X . .",
                                ". . O X . c O",
                                ". . . . X W b",
                                ". . . . . X .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ATARI,
                        solutions = "c",
                        success = "맞아요. 백이 돌을 살리려고 받았어요. 한 수를 건넜으니 흑이 패를 되따냈어요.",
                        wrong = mapOf(
                            "a" to "패는 바로 되따낼 수 없어요. 다른 곳에 한 수 둔 뒤에 따낼 수 있어요.",
                            "b" to "이 자리에 두면 내 돌의 활로가 하나뿐이에요. 백이 그 돌을 따내면 팻감이 되지 않아요.",
                        ),
                        fallback = "백이 받지 않아도 되는 자리예요. 백이 패를 이으면 패가 끝나요. 백이 꼭 받아야 하는 곳을 찾아 보세요.",
                        followUp = "b a",
                        hint = "단수는 상대가 꼭 받아야 하는 수예요. 내 돌의 활로도 함께 세어 보세요.",
                    ),
                    Problem(
                        prompt = "흑이 패를 따냈고, 백은 다른 곳에 뒀어요. 흑 차례예요. 표시된 흑돌을 모두 이어서 패를 끝내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . B O . . .",
                                ". B a B O . .",
                                ". . B O . . .",
                                ". . . O . b .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CONNECT,
                        solutions = "a",
                        success = "맞아요. 패를 이으면 흑돌이 한 몸이 돼요. 백은 더 따낼 돌이 없어요.",
                        wrong = mapOf(
                            "b" to "패를 그대로 두면 백이 되따낼 수 있어요. 지금은 패를 이어서 끝낼 기회예요.",
                        ),
                        fallback = "패가 그대로 남아 있어요. 백이 되따낼 수 있어요. 단수에 걸린 흑돌을 이어 보세요.",
                        hint = "단수에 걸린 흑돌 옆의 빈 자리를 보세요.",
                    ),
                ),
            ),
        ),
    ),
    // ───────────── 4장 ─────────────
    Chapter(
        id = "ch4",
        number = 4,
        title = "연결과 끊기",
        summary = "돌을 이어서 튼튼하게 만들고, 상대 돌의 약한 곳을 끊는 법을 배워요.",
        lessons = listOf(
            Lesson(
                id = "c4-1",
                title = "이어진 돌은 강해요",
                summary = "돌을 이으면 활로를 함께 써서 잡기 어려워져요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "흑돌 둘이 한 칸 떨어져 있어요. 활로는 각각 3개예요. 사이의 a에 이으면 세 돌이 한 몸이 돼요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". O . O .",
                                ". X a X .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "세 돌이 한 몸이 됐어요. 활로는 6개예요. 활로가 많은 돌은 잡기 어려워요.",
                        ),
                    ),
                    Explain(
                        text = "같은 모양에서 백이 먼저 a에 두면 흑돌은 둘로 갈라져요. 갈라진 돌은 따로따로 공격받아요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". O . O .",
                                ". X a X .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        first = Side.WHITE,
                        notes = listOf(
                            "흑돌 둘의 활로가 2개씩으로 줄었어요. 서로 도울 수도 없어요.",
                        ),
                    ),
                    Explain(
                        text = "대각선으로 놓인 두 돌은 아직 한 몸이 아니에요. 하지만 이을 자리가 a와 b 두 곳이에요. 백이 한쪽을 막으면 흑은 다른 쪽에 이으면 돼요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". X a . .",
                                ". b X . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a b",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 한쪽을 막았어요.",
                            "흑이 다른 쪽에 이었어요. 두 돌이 한 몸이 됐어요.",
                        ),
                    ),
                    Explain(
                        text = "백돌이 이미 한쪽을 차지했다면 이을 자리는 a 하나뿐이에요. 백이 a까지 두면 흑돌은 끊겨요. 이런 자리는 서둘러 이어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". X O . .",
                                ". a X . .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 먼저 이었어요. 이제 끊길 걱정이 없어요.",
                        ),
                    ),
                    Quiz(
                        question = "흑이 a에 이으면 흑돌 셋의 활로는 모두 몇 개일까요?",
                        choices = listOf("3개", "4개", "5개"),
                        answer = 1,
                        explanation = "위쪽 하나, 오른쪽 둘, 아래쪽 하나로 모두 4개예요. 끝 선이라 왼쪽에는 활로가 없어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                "X O . . .",
                                "a . . . .",
                                "X . . . .",
                                ". . . . .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 흑돌 둘을 이어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . b O . . .",
                                ". . B O . . .",
                                ". O a B c . .",
                                ". . O . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CONNECT,
                        solutions = "a",
                        success = "맞아요. 두 돌이 한 몸이 됐어요. 활로도 4개로 늘었어요.",
                        wrong = mapOf(
                            "b" to "이 자리에 두면 활로는 늘지만 두 돌은 여전히 떨어져 있어요. 백이 사이를 끊으면 흑돌이 둘로 갈라져요.",
                            "c" to "이 자리에 두면 활로는 늘지만 두 돌은 여전히 떨어져 있어요. 백이 사이를 끊으면 흑돌이 둘로 갈라져요.",
                        ),
                        fallback = "두 흑돌은 여전히 떨어져 있어요. 백이 사이에 두면 끊겨요. 두 돌에 함께 붙어 있는 빈 자리를 찾아 보세요.",
                        hint = "두 흑돌에 함께 붙어 있는 빈 자리는 한 곳이에요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 한 칸 떨어진 흑돌 둘을 이어 보세요. 위아래에서 백돌이 사이를 노리고 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . b O c . .",
                                ". . B a B . .",
                                ". . . O . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CONNECT,
                        solutions = "a",
                        success = "맞아요. 세 돌이 한 몸이 됐어요. 백이 끊을 틈이 없어요.",
                        wrong = mapOf(
                            "b" to "이 자리에 두어도 두 돌 사이는 비어 있어요. 백이 그 사이에 두면 흑돌이 갈라져요.",
                            "c" to "이 자리에 두어도 두 돌 사이는 비어 있어요. 백이 그 사이에 두면 흑돌이 갈라져요.",
                        ),
                        fallback = "두 흑돌 사이가 비어 있어요. 백이 그 사이에 두면 흑돌이 갈라져요.",
                        hint = "두 흑돌 사이의 빈 자리를 보세요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 왼쪽 흑돌이 단수예요. 오른쪽 흑돌과 이어서 살려 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . O b . . .",
                                ". O B a B . .",
                                ". . O c . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CONNECT,
                        solutions = "a",
                        success = "맞아요. 이으면서 활로가 5개로 늘었어요. 이음은 단수에서 벗어나는 좋은 방법이에요.",
                        wrong = mapOf(
                            "b" to "이 자리는 단수에 걸린 흑돌과 이어지지 않아요. 백이 마지막 활로를 막으면 흑돌이 잡혀요.",
                            "c" to "이 자리는 단수에 걸린 흑돌과 이어지지 않아요. 백이 마지막 활로를 막으면 흑돌이 잡혀요.",
                        ),
                        fallback = "왼쪽 흑돌은 여전히 단수예요. 백이 마지막 활로를 막으면 잡혀요.",
                        hint = "단수에 걸린 흑돌의 마지막 활로가 곧 이을 자리예요.",
                    ),
                ),
            ),
            Lesson(
                id = "c4-2",
                title = "끊는 점 찾기",
                summary = "상대 돌 사이의 약한 자리를 찾아 끊어요. 끊긴 돌은 약해져요.",
                minutes = 4,
                steps = listOf(
                    Explain(
                        text = "상대 돌이 이어지지 못하게 사이를 막는 것을 '끊는다'고 해요. 백돌 둘이 대각선으로 놓여 있고, 한쪽은 흑돌이 차지했어요. 남은 a 자리가 끊는 점이에요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O X .",
                                ". . a O .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 끊었어요. 백돌 둘은 이제 이어질 수 없어요.",
                        ),
                    ),
                    Explain(
                        text = "두 자리가 모두 비어 있으면 끊을 수 없어요. 흑이 a에 두면 백이 b에 이어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O a .",
                                ". . b O .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a b",
                        notes = listOf(
                            "흑이 한쪽에 뒀어요.",
                            "백이 다른 쪽에 이었어요. 백돌은 한 몸이 됐어요.",
                        ),
                    ),
                    Explain(
                        text = "끊긴 돌은 활로가 적고 서로 도울 수 없어요. 그래서 끊은 다음에는 공격하기 쉬워요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . X . . .",
                                ". . b O X . .",
                                ". . . a O . .",
                                ". . . . c . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "a b c",
                        notes = listOf(
                            "흑이 끊으면서 위쪽 백돌을 단수로 몰았어요.",
                            "백이 위쪽 돌을 살렸어요.",
                            "이번에는 아래쪽 백돌이 단수예요. 백은 두 곳을 다 돌보기 어려워요.",
                        ),
                    ),
                    Explain(
                        text = "끊기 전에 내 돌의 활로를 세어 봐요. 여기서 a에 끊으면 흑돌의 활로는 하나뿐이에요. 백이 b에 두어 바로 따내요. 이런 자리는 끊어도 소용이 없어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O X .",
                                ". O a O .",
                                ". . b . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a b",
                        notes = listOf(
                            "흑이 끊었지만 활로가 하나뿐이에요.",
                            "백이 흑돌을 따냈어요. 끊기는 실패예요.",
                        ),
                    ),
                    Quiz(
                        question = "흑이 a에 끊으면, 끊은 흑돌의 활로는 몇 개일까요?",
                        choices = listOf("1개", "2개", "3개"),
                        answer = 1,
                        explanation = "왼쪽과 아래쪽이 비어 있어서 활로는 2개예요. 바로 잡히지 않으니 끊을 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O X .",
                                ". . a O .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 백돌 둘이 이어지지 못하게 끊어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . X . . . .",
                                ". . X W b . .",
                                ". . W a . . .",
                                ". . c . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CUT,
                        solutions = "a",
                        success = "맞아요. 백돌 둘이 갈라졌어요. 끊은 흑돌의 활로도 2개라서 바로 잡히지 않아요.",
                        wrong = mapOf(
                            "b" to "이 자리는 백돌 하나의 옆을 막을 뿐이에요. 백이 사이를 이으면 백돌이 한 몸이 돼요.",
                            "c" to "이 자리는 백돌 하나의 옆을 막을 뿐이에요. 백이 사이를 이으면 백돌이 한 몸이 돼요.",
                        ),
                        fallback = "백이 사이를 이으면 백돌이 한 몸이 돼요. 두 백돌에 함께 붙어 있는 빈 자리를 찾아 보세요.",
                        hint = "두 백돌에 함께 붙어 있는 빈 자리가 끊는 점이에요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 한 칸 떨어진 백돌 둘 사이를 끊어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . X . . .",
                                ". b W a W c .",
                                ". . . X . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CUT,
                        solutions = "a",
                        success = "맞아요. 위아래 흑돌과 이어지면서 백돌을 둘로 갈랐어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 백돌 하나의 옆을 막을 뿐이에요. 백이 가운데를 이으면 백돌이 한 몸이 돼요.",
                            "c" to "이 자리는 백돌 하나의 옆을 막을 뿐이에요. 백이 가운데를 이으면 백돌이 한 몸이 돼요.",
                        ),
                        fallback = "백이 가운데를 이으면 백돌이 한 몸이 돼요. 두 백돌 사이의 빈 자리를 보세요.",
                        hint = "두 백돌 사이에 빈 자리가 하나 있어요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 끊을 수 있는 자리가 두 곳 보여요. 표시된 백돌 둘을 안전하게 끊어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . X . . .",
                                ". . W X O . .",
                                ". . a W b O .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CUT,
                        solutions = "a",
                        success = "맞아요. 끊은 흑돌의 활로가 2개라서 바로 잡히지 않아요.",
                        wrong = mapOf(
                            "b" to "이 자리에 끊으면 내 돌의 활로가 하나뿐이에요. 백이 바로 따내요. 끊기 전에 내 돌의 활로를 세어 봐요.",
                        ),
                        fallback = "백이 사이를 이으면 표시된 백돌이 한 몸이 돼요. 두 백돌에 함께 붙어 있는 빈 자리를 찾아 보세요.",
                        hint = "끊은 뒤 내 돌의 활로가 2개 이상인 자리를 골라요.",
                    ),
                ),
            ),
            Lesson(
                id = "c4-3",
                title = "꽉 이음과 호구",
                summary = "끊는 점을 지키는 두 가지 방법이에요. 꽉 이음은 튼튼하고, 호구는 넓어요.",
                minutes = 5,
                steps = listOf(
                    Explain(
                        text = "끊는 점에 직접 두어 잇는 것을 '꽉 이음'이라고 해요. 빈틈이 없어서 가장 튼튼해요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . X O . . .",
                                ". . a X . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 꽉 이었어요. 흑돌 셋은 한 몸이에요.",
                        ),
                    ),
                    Explain(
                        text = "끊는 점 바로 옆에 두어 지키는 방법도 있어요. 흑이 b에 두면 흑돌 셋이 끊는 점 a를 세 방향에서 감싸요. 이 모양을 '호구'라고 해요. 호랑이가 입을 벌린 모양이에요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . X O . . .",
                                ". . a X . . .",
                                ". . b . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "b",
                        notes = listOf(
                            "호구 모양이에요. 끊는 점 a는 아직 비어 있어요.",
                        ),
                    ),
                    Explain(
                        text = "백이 호구 안에 들어오면 어떻게 될까요? 들어온 백돌은 놓자마자 단수예요. 흑이 바로 따내요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . X O . . .",
                                ". b a X . . .",
                                ". . X . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "a b",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 끊으러 들어왔어요. 활로는 하나뿐이에요.",
                            "흑이 백돌을 따냈어요. 흑돌은 끊기지 않았어요.",
                        ),
                    ),
                    Explain(
                        text = "호구에는 약점도 있어요. 백이 입구인 b에 먼저 두면, 다음에 a에 끊는 수가 생겨요. 그때는 흑이 a에 이어서 받아요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . X O . . .",
                                ". b a X . . .",
                                ". . X . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "b a",
                        first = Side.WHITE,
                        notes = listOf(
                            "백이 호구의 입구에 뒀어요. 다음에 끊으려는 수예요.",
                            "흑이 이었어요. 이제 끊을 곳이 없어요.",
                        ),
                    ),
                    Quiz(
                        question = "흑돌 셋이 호구 모양이에요. 백이 호구 안의 a에 두면 어떻게 될까요?",
                        choices = listOf("백돌이 단수가 되어 흑이 따낼 수 있어요", "흑돌이 끊겨요", "흑돌이 단수가 돼요"),
                        answer = 0,
                        explanation = "a에 들어온 백돌은 세 방향이 흑돌에 막혀 활로가 하나뿐이에요. 흑이 남은 활로를 막아 따내요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . X O . . .",
                                ". . a X . . .",
                                ". . X . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                    ),
                    Problem(
                        prompt = "흑 차례예요. 백돌이 끊는 점 옆에 와 있어요. 표시된 흑돌 둘을 끊기지 않게 이어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . B O . . .",
                                ". O a B . . .",
                                ". . b . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CONNECT,
                        solutions = "a",
                        success = "맞아요. 상대 돌이 가까이 있을 때는 꽉 잇는 것이 확실해요.",
                        wrong = mapOf(
                            "b" to "호구를 만들고 싶지만, 입구에 이미 백돌이 있어요. 백이 끊으러 들어와도 따낼 수 없어요.",
                        ),
                        fallback = "백이 끊는 점에 두면 흑돌이 갈라져요. 두 흑돌에 함께 붙어 있는 빈 자리를 찾아 보세요.",
                        hint = "가장 튼튼한 방법은 끊는 점에 직접 두는 거예요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 끊는 점을 호구 모양으로 지켜 보세요. 호구는 끊는 점을 세 방향에서 감싸는 모양이에요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". . X O . . .",
                                ". c a X . . .",
                                ". . b . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "b c",
                        success = "맞아요. 호구 모양이에요. 백이 끊으러 들어오면 바로 따낼 수 있어요.",
                        wrong = mapOf(
                            "a" to "이 수는 꽉 이음이에요. 튼튼하지만, 이번에는 호구 모양을 만들어 보세요.",
                        ),
                        fallback = "호구가 되지 않았어요. 끊는 점 바로 옆 자리 가운데, 흑돌 둘과 함께 끊는 점을 감싸는 곳을 찾아 보세요.",
                        hint = "끊는 점의 네 방향 가운데 두 곳은 이미 흑돌이에요. 남은 두 곳 가운데 하나에 두면 돼요.",
                    ),
                    Problem(
                        prompt = "백이 호구의 입구에 뒀어요. 흑 차례예요. 표시된 흑돌이 끊기지 않게 받아 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . O . . .",
                                ". b B O . . .",
                                "O O a B . . .",
                                ". c B . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CONNECT,
                        solutions = "a",
                        success = "맞아요. 입구가 막히면 호구는 이어서 받아요. 흑돌 넷이 한 몸이 됐어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 백돌 옆을 막을 뿐이에요. 백이 호구 안쪽에 두면 흑돌이 갈라져요.",
                            "c" to "이 자리는 백돌 옆을 막을 뿐이에요. 백이 호구 안쪽에 두면 흑돌이 갈라져요.",
                        ),
                        fallback = "백이 호구 안쪽에 두면 흑돌이 갈라져요. 이번에는 들어온 백돌을 따낼 수 없어요.",
                        hint = "백의 다음 수는 호구 안쪽이에요. 그 자리를 먼저 차지해요.",
                    ),
                ),
            ),
            Lesson(
                id = "c4-4",
                title = "양단수",
                summary = "한 수로 두 곳을 단수 치는 수예요. 상대는 한쪽만 살릴 수 있어요.",
                minutes = 5,
                steps = listOf(
                    Explain(
                        text = "한 수로 두 군데를 한꺼번에 단수 치는 것을 '양단수'라고 해요. 표시된 백돌 둘은 활로가 2개씩이고, a 자리를 함께 쓰고 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". X . . .",
                                "b W X . .",
                                ". a W X .",
                                ". . c . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a b c",
                        notes = listOf(
                            "흑이 두 백돌을 한꺼번에 단수로 몰았어요. 양단수예요.",
                            "백이 한쪽을 살렸어요.",
                            "흑이 다른 쪽을 따냈어요. 백은 둘 다 살릴 수 없었어요.",
                        ),
                    ),
                    Explain(
                        text = "양단수 자리는 이렇게 찾아요. 활로가 2개인 상대 돌 둘을 찾아요. 두 돌이 함께 쓰는 활로가 있다면 그 자리가 양단수예요. 끊는 점에서 자주 생겨요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . . . .",
                                ". . . . .",
                                ". a W X .",
                                ". W X X .",
                            ),
                            caption = "두 백돌은 활로가 2개씩이에요. 함께 쓰는 a 자리가 양단수예요.",
                        ),
                    ),
                    Explain(
                        text = "양단수를 당하지 않으려면 그 자리를 먼저 차지해요. 백이 a에 이으면 두 돌이 한 몸이 되고 활로도 늘어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". X . . .",
                                ". W X . .",
                                ". a W X .",
                                ". . . . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        first = Side.WHITE,
                        notes = listOf(
                            "백돌 셋이 한 몸이 됐어요. 활로는 4개예요.",
                        ),
                    ),
                    Quiz(
                        question = "양단수를 당했어요. 두 곳 가운데 한 곳만 살릴 수 있어요. 보통 어느 쪽을 살릴까요?",
                        choices = listOf("돌이 더 많은 쪽", "돌이 더 적은 쪽", "어느 쪽도 살리지 않아요"),
                        answer = 0,
                        explanation = "보통은 돌이 많은 쪽, 더 중요한 쪽을 살려요. 하나를 잃더라도 큰 쪽을 지키는 것이 이득이에요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 백돌 둘을 한꺼번에 단수로 몰아 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . b .",
                                ". . a W X",
                                ". c W X .",
                                ". . X . .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ATARI,
                        solutions = "a",
                        success = "맞아요. 양단수예요. 백이 한쪽을 살리는 사이에 다른 쪽을 따냈어요.",
                        wrong = mapOf(
                            "b" to "위쪽 백돌만 단수예요. 백이 두 돌을 이으면 한 몸이 되어 단수에서 벗어나요.",
                            "c" to "아래쪽 백돌만 단수예요. 백이 두 돌을 이으면 한 몸이 되어 단수에서 벗어나요.",
                        ),
                        fallback = "두 백돌이 한꺼번에 단수가 되지 않았어요. 두 돌이 함께 쓰는 활로를 찾아 보세요.",
                        followUp = "b c",
                        hint = "두 백돌의 활로를 각각 짚어 보세요. 겹치는 자리가 있어요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 이번에는 백돌이 세 개예요. 표시된 백돌을 모두 한꺼번에 단수로 몰아 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . X . . .",
                                ". X a W c . .",
                                "b W W X X . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.ATARI,
                        solutions = "a",
                        success = "맞아요. 양단수예요. 백이 한쪽을 살리는 사이에 끝 선의 백돌 둘을 따냈어요.",
                        wrong = mapOf(
                            "b" to "이 자리에 두면 끝 선의 백돌만 단수예요. 게다가 내 돌의 활로도 하나뿐이에요.",
                            "c" to "위쪽 백돌만 단수예요. 백이 두 무리를 이으면 단수에서 벗어나요.",
                        ),
                        fallback = "백돌이 한꺼번에 단수가 되지 않았어요. 두 무리가 함께 쓰는 활로를 찾아 보세요.",
                        followUp = "c b",
                        hint = "끝 선의 백돌 둘과 위쪽 백돌 하나가 함께 쓰는 활로가 있어요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 흑돌 둘이 양단수를 당할 모양이에요. 미리 지켜 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". d b . .",
                                "e a B O .",
                                "c B O O .",
                                ". O . . .",
                                ". . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.POINT,
                        solutions = "a d e",
                        success = "맞아요. 양단수를 막았어요. 끊는 점에 직접 이어도 되고, 호구로 지켜도 돼요.",
                        wrong = mapOf(
                            "b" to "이 자리에 늘면 위쪽 돌의 활로는 늘어요. 하지만 백이 두 돌 사이에 두면 아래쪽 돌이 단수가 되고 둘은 끊겨요.",
                            "c" to "이 자리에 늘면 아래쪽 돌의 활로는 늘어요. 하지만 백이 두 돌 사이에 두면 위쪽 돌이 단수가 되고 둘은 끊겨요.",
                        ),
                        fallback = "백이 두 흑돌 사이에 두면 양단수예요. 두 흑돌이 함께 쓰는 활로를 찾아 보세요.",
                        hint = "백이 두고 싶은 자리를 먼저 찾아요. 그 자리를 직접 차지하거나 호구로 감싸요.",
                    ),
                    Problem(
                        prompt = "흑이 양단수를 쳤고, 백은 위쪽 돌을 살렸어요. 흑 차례예요. 남은 백돌을 따내 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". c X . . . .",
                                ". O O X . . .",
                                ". b X W X . .",
                                ". . . a . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CAPTURE,
                        solutions = "a",
                        success = "맞아요. 양단수의 마무리예요. 상대가 살리지 못한 쪽을 따냈어요.",
                        wrong = mapOf(
                            "b" to "위쪽 백돌의 활로를 하나 줄였을 뿐이에요. 그사이 백이 표시된 돌을 살려요. 단수인 돌부터 따내요.",
                            "c" to "위쪽 백돌의 활로를 하나 줄였을 뿐이에요. 그사이 백이 표시된 돌을 살려요. 단수인 돌부터 따내요.",
                        ),
                        fallback = "표시된 백돌은 아직 판 위에 있어요. 백이 늘면 따낼 기회가 사라져요.",
                        hint = "표시된 백돌의 활로는 하나뿐이에요.",
                    ),
                ),
            ),
            Lesson(
                id = "c4-5",
                title = "끊을까 이을까",
                summary = "내 약점과 상대 약점이 함께 보일 때, 무엇을 먼저 할지 정하는 법이에요.",
                minutes = 5,
                steps = listOf(
                    Explain(
                        text = "이을 자리와 끊을 자리가 같은 점일 때가 있어요. a 자리는 흑이 두면 흑돌이 이어지고 백돌이 끊겨요. 백이 두면 그 반대예요. 이런 자리는 먼저 두는 쪽이 임자예요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . .",
                                ". . O . .",
                                ". X a X .",
                                ". . O . .",
                                ". . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 먼저 뒀어요. 흑돌 셋은 한 몸이 되고 백돌 둘은 갈라졌어요.",
                        ),
                    ),
                    Explain(
                        text = "내 돌이 단수라면 끊기보다 살리기가 먼저예요. 표시된 흑돌이 단수예요. 흑이 욕심을 내서 a에 끊으면 백이 b에 두어 흑돌을 따내요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". a O . . . .",
                                ". O B b X . .",
                                ". . O . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "a b",
                        notes = listOf(
                            "흑이 끊었어요. 하지만 단수인 돌을 내버려 뒀어요.",
                            "백이 흑돌을 따냈어요. 끊은 돌도 힘을 잃었어요.",
                        ),
                    ),
                    Explain(
                        text = "같은 모양이에요. 이번에는 단수인 돌부터 살려요. b에 이으면 흑돌 셋이 한 몸이 돼요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". a O . . . .",
                                ". O B b X . .",
                                ". . O . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "b",
                        notes = listOf(
                            "흑이 먼저 이어서 살렸어요. 이제 a에 끊는 수를 노릴 수 있어요.",
                        ),
                    ),
                    Explain(
                        text = "반대로 내 돌이 튼튼하다면 망설이지 말고 끊어요. 흑돌은 호구 모양이에요. b에 백이 들어와도 따낼 수 있으니 끊기지 않아요. 그래서 백의 끊는 점 a를 바로 끊을 수 있어요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . X O a . .",
                                ". . b X O . .",
                                ". . X . . . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        sequence = "a",
                        notes = listOf(
                            "흑이 끊었어요. 위쪽 백돌은 단수가 됐어요.",
                        ),
                    ),
                    Quiz(
                        question = "내 돌 하나가 단수에 걸렸고, 상대 돌을 끊을 자리도 보여요. 보통 무엇을 먼저 할까요?",
                        choices = listOf("단수에 걸린 내 돌부터 살펴요", "무조건 끊어요", "한 수 쉬어요"),
                        answer = 0,
                        explanation = "내 돌이 잡히면 끊은 돌도 힘을 잃어요. 먼저 내 돌이 안전한지 보고, 그다음에 끊어요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 표시된 흑돌 가운데 오른쪽 돌이 단수예요. 백을 끊을 자리도 보여요. 먼저 둬야 할 곳을 눌러 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . . . . .",
                                ". . . . O b .",
                                ". . B a B O .",
                                ". . . . O . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CONNECT,
                        solutions = "a",
                        success = "맞아요. 단수인 돌부터 이어서 살렸어요. 끊는 수는 그다음에 노려요.",
                        wrong = mapOf(
                            "b" to "끊고 싶은 자리지만, 단수에 걸린 흑돌을 내버려 뒀어요. 백이 그 돌을 따내면 끊은 돌도 힘을 잃어요.",
                        ),
                        fallback = "오른쪽 흑돌은 여전히 단수예요. 백이 마지막 활로를 막으면 잡혀요.",
                        hint = "내 돌이 단수라면 살리기가 먼저예요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 흑돌은 호구 모양이라 튼튼해요. 표시된 백돌 둘을 끊어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . c . . .",
                                ". . a W X . .",
                                ". . W X b . .",
                                ". . . . X . .",
                                ". . . . . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CUT,
                        solutions = "a",
                        success = "맞아요. 내 돌이 튼튼하니 바로 끊을 수 있어요. 끊으면서 위쪽 백돌은 단수가 됐어요.",
                        wrong = mapOf(
                            "b" to "이 자리는 이미 호구라서 끊기지 않아요. 지킬 필요가 없는 곳을 지키는 사이에 백이 끊는 점을 이어요.",
                            "c" to "위쪽 백돌을 단수로 몰았지만, 백이 끊는 점에 이으면서 달아나요. 백돌이 한 몸이 돼요.",
                        ),
                        fallback = "백이 끊는 점을 이으면 백돌이 한 몸이 돼요. 두 백돌에 함께 붙어 있는 빈 자리를 찾아 보세요.",
                        hint = "두 백돌에 함께 붙어 있는 빈 자리를 찾아 보세요.",
                    ),
                    Problem(
                        prompt = "흑 차례예요. 흑도 백도 가운데 한 점을 노리고 있어요. 표시된 흑돌을 이으면서 백을 끊어 보세요.",
                        diagram = Diagram(
                            rows = listOf(
                                ". . . . . . .",
                                ". . . B . . .",
                                ". . b B . . .",
                                ". O O a O O .",
                                ". . . B c . .",
                                ". . . B . . .",
                                ". . . . . . .",
                            ),
                        ),
                        toPlay = Side.BLACK,
                        goal = Goal.CONNECT,
                        solutions = "a",
                        success = "맞아요. 한 수로 내 돌은 잇고 상대 돌은 끊었어요. 이런 자리는 먼저 두는 쪽이 임자예요.",
                        wrong = mapOf(
                            "b" to "이 자리에 두는 사이에 백이 가운데를 차지해요. 백돌은 이어지고 흑돌은 끊겨요.",
                            "c" to "이 자리에 두는 사이에 백이 가운데를 차지해요. 백돌은 이어지고 흑돌은 끊겨요.",
                        ),
                        fallback = "백이 가운데를 차지하면 백돌은 이어지고 흑돌은 끊겨요. 흑돌과 백돌에 모두 붙어 있는 자리를 찾아 보세요.",
                        hint = "흑돌 둘과 백돌 둘에 모두 붙어 있는 자리예요.",
                    ),
                ),
            ),
        ),
    ),
)
