# Contributing

## 빌드

JDK 21이 필요합니다.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
```

릴리스 서명 빌드(`assembleRelease`)는 환경 변수 `PDFBOOK_KEYSTORE`, `PDFBOOK_STORE_PASSWORD`, `PDFBOOK_KEY_ALIAS`, `PDFBOOK_KEY_PASSWORD`가 있어야 합니다.

## 폰트

- Pretendard v1.3.9 (OFL) — https://github.com/orioncactus/pretendard/releases — subset renamed to PDFBook Sans (RFN)
- JetBrains Mono v2.304 (OFL) — https://github.com/JetBrains/JetBrainsMono/releases — digits/symbols subset

라이선스 전문: [licenses/Pretendard-OFL.txt](licenses/Pretendard-OFL.txt), [licenses/JetBrainsMono-OFL.txt](licenses/JetBrainsMono-OFL.txt). 서브셋은 `scripts/subset_fonts.py`로 다시 만들 수 있습니다.

## 커밋 메시지

커밋 메시지는 [Conventional Commits](https://www.conventionalcommits.org/)를 따릅니다. 로컬 검사 훅을 쓰려면:

```bash
chmod +x scripts/commit-msg
git config core.hooksPath scripts
```
