# Pretendard 폰트 설정 가이드

## 다운로드

공식 GitHub Releases에서 다운로드:
https://github.com/orioncactus/pretendard/releases

또는 CDN:
https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/variable/pretendardvariable.css

## 필요한 파일 (OTF 권장)

이 디렉토리(`app/assets/fonts/`)에 아래 3개 파일을 배치하세요:

| 파일명 | 굵기 | 용도 |
|--------|------|------|
| `Pretendard-Regular.otf` | 400 | 본문 |
| `Pretendard-SemiBold.otf` | 600 | 강조 본문 |
| `Pretendard-Bold.otf` | 700 | 제목 |

## pubspec.yaml 활성화

파일 배치 후 `app/pubspec.yaml`의 주석을 해제하세요:

```yaml
flutter:
  fonts:
    - family: Pretendard
      fonts:
        - asset: assets/fonts/Pretendard-Regular.otf
          weight: 400
        - asset: assets/fonts/Pretendard-SemiBold.otf
          weight: 600
        - asset: assets/fonts/Pretendard-Bold.otf
          weight: 700
```

## 확인

`flutter pub get` 후 앱 재빌드하면 Noto Sans KR 폴백에서 Pretendard로 자동 전환됩니다.
