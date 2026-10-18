<p align="center"><img src="docs/banner.png" alt="PDFBook" width="100%"></p>

# PDFBook

폴더블에 맞춘 Android PDF 뷰어.

An Android PDF viewer made for foldables.

## 기능

- 책 폴더 여러 개 등록, 폴더별 서재와 표지 목록
- 이어 읽기(마지막으로 읽던 쪽에서 다시 열기)
- 두쪽 펼침 보기, 표지 단독 표시
- 오른쪽 → 왼쪽 / 왼쪽 → 오른쪽 넘김, 책마다 방향 저장
- 볼륨 키로 넘기기
- 오버레이와 페이지 슬라이더
- 핀치 줌, 더블탭 확대
- 라이트 / 다크 테마, 한국어 / English

## 설치

[Releases](https://github.com/parkjangwon/pdfbook/releases)에서 `pdfbook-<버전>.apk`를 받아 설치합니다. Android 15(API 35) 이상이 필요합니다.

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

## 라이선스

코드는 [MIT License](LICENSE)를 따릅니다. 앱에 포함된 폰트는 SIL Open Font License 1.1이며 전문은 [licenses/](licenses/)에 있습니다.

## 커밋 메시지 훅

Conventional Commits 검사를 쓰려면:

```bash
chmod +x scripts/commit-msg
git config core.hooksPath scripts
```
