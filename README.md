# chess-game

체스 마스터: AI와 두면서 실력을 키우는 Android 체스 트레이닝 앱.

- AI 상대 10단계 (자체 Kotlin 엔진: 알파베타 탐색 + PeSTO 평가)
- 코치: 수 평가(최선 ~ 블런더), 힌트 화살표, 평가 바, 오프닝 자동 인식
- 학습: 오프닝 27개(수마다 설명, 이어서 연습), 전략 가이드 20개
- 성장: 레이팅, 추천 상대, 레벨별 전적, 대국 후 정확도
- 테마 5종, 라이트/다크

## Stack

Kotlin + Jetpack Compose (Material 3), AGP 9.4, Gradle 9.8, minSdk 26.

## Build

```bash
./gradlew testDebugUnitTest assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`

서명: `keystore/chess.jks` + `keystore/PASSWORD.txt`(git 제외)가 있으면 그 키로, 없으면 디버그 키로 서명하고 경고를 찍는다. 배포 APK는 반드시 정식 키로 서명해야 기존 설치 위에 업데이트된다. 키는 백업해 둘 것.

## Update

앱이 GitHub Releases 최신 버전(`releases/latest`)을 30분에 한 번 확인하고, 새 버전이 있으면 홈에서 받아 설치한다. 릴리스는 prerelease가 아닌 정식 릴리스로 올리고 `.apk`를 첨부한다.

## License

MIT
