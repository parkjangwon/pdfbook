# 기여 안내

## 버전 규칙

- [SemVer](https://semver.org/lang/ko/)를 따릅니다. 1.0 전에는 `0.MINOR.PATCH`입니다.
- 정기 릴리스(금요일)는 MINOR를 올립니다. 마일스톤 이름이 곧 버전 이름입니다(예: `v0.4.0`).
- PATCH는 릴리스 사이의 급한 버그 수정에만 씁니다.
- 버전은 `gradle.properties`의 `version` 한 곳에만 적습니다. versionName은 이 값이고, versionCode는 `MAJOR×10000 + MINOR×100 + PATCH`로 계산됩니다(0.4.0 → 400). `./gradlew -q printVersion`으로 확인할 수 있어요.
- 태그는 `vX.Y.Z` 형식으로, main의 릴리스 커밋에만 붙이고 옮기지 않습니다.

## 릴리스 절차

1. 마일스톤용 버전 올림 PR을 엽니다: `gradle.properties`의 `version`을 올리고, `CHANGELOG.md` 맨 위에 `## [X.Y.Z] - 날짜` 항목을 추가합니다. 내용은 마일스톤 이슈를 바탕으로 사용자가 읽기 쉬운 말로 씁니다.
2. PR을 main에 합칩니다.
3. 합쳐진 main 커밋에 주석 태그를 붙여 올립니다.
   ```sh
   git tag -a vX.Y.Z -m "vX.Y.Z" <main 커밋>
   git push origin vX.Y.Z
   ```
4. 릴리스 워크플로가 태그와 `version`이 같은지 검사합니다. 다르면 실패하니, 태그를 옮기지 말고 원인을 고친 뒤 다음 버전으로 다시 릴리스합니다. 통과하면 서명된 APK와 `CHANGELOG.md`의 해당 항목으로 GitHub Release가 만들어집니다.

## 급한 버그 수정(핫픽스)

PATCH 마일스톤(예: `v0.4.1`)을 만들고, 이슈와 PR로 고친 뒤 위 절차대로 PATCH를 올려 릴리스합니다.
