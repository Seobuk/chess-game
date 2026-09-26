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

APK: `app/build/outputs/apk/release/app-release.apk` (현재 디버그 키로 서명, 스토어 배포 전 정식 키 필요)

## License

MIT
