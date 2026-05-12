# Flutter Golden Test 매트릭스 — textScaler × 핵심 화면

## 개요

4가지 텍스트 스케일 비율(`textScaler`) × 4개 핵심 화면의 golden 이미지를 사전 승인하고,
CI에서 픽셀 회귀를 자동 탐지합니다.
명도 대비 자동 검사는 `accessibility_test` 패키지로 수행합니다.

---

## 매트릭스

| 화면 ID | 화면명 | textScaler 0.8 | textScaler 1.0 (기준) | textScaler 1.5 | textScaler 2.0 |
|---------|--------|:--------------:|:---------------------:|:--------------:|:--------------:|
| SCR-01 | 홈 피드 (`HomeScreen`) | golden/home_08.png | golden/home_10.png | golden/home_15.png | golden/home_20.png |
| SCR-02 | 이벤트 상세 (`EventDetailScreen`) | golden/event_detail_08.png | golden/event_detail_10.png | golden/event_detail_15.png | golden/event_detail_20.png |
| SCR-03 | 든든이 대리 승인 다이얼로그 (`ProxyApprovalDialog`) | golden/proxy_approval_08.png | golden/proxy_approval_10.png | golden/proxy_approval_15.png | golden/proxy_approval_20.png |
| SCR-04 | 포인트 지갑 (`PointWalletScreen`) | golden/wallet_08.png | golden/wallet_10.png | golden/wallet_15.png | golden/wallet_20.png |

> golden 파일 저장 경로: `app/test/goldens/`
> `textScaler 1.0`을 기준 이미지로 승인 후 나머지를 생성합니다.

---

## textScaler별 특이사항

| textScaler | 주요 검증 항목 |
|-----------|--------------|
| 0.8 | 텍스트 잘림 없음, 버튼 탭 영역 최소 48×48dp 유지 |
| 1.0 | 디자인 시안과 픽셀 1:1 일치 (기준) |
| 1.5 | 레이아웃 overflow 없음, 카드 높이 자동 확장 확인 |
| 2.0 | 한 줄 텍스트가 2줄로 wrapping 시 UI 붕괴 없음, 스크롤 가능 |

---

## 명도 대비 자동 검사 (`accessibility_test` 패키지)

### 패키지 추가

```yaml
# app/pubspec.yaml
dev_dependencies:
  accessibility_test: ^0.0.10
  flutter_test:
    sdk: flutter
```

### 사용 예시

```dart
// app/test/accessibility/home_screen_accessibility_test.dart

import 'package:accessibility_test/accessibility_test.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:your_app/screens/home_screen.dart'; // TODO: 실제 경로로 교체

void main() {
  group('홈 화면 접근성', () {
    testWidgets(
      '모든 텍스트 요소가 WCAG AA 명도 대비(4.5:1) 기준을 충족한다',
      (WidgetTester tester) async {
        await tester.pumpWidget(
          const MaterialApp(home: HomeScreen()),
        );

        // SemanticsHandle 활성화
        final SemanticsHandle handle = tester.ensureSemantics();

        // guideline: textContrastGuideline = WCAG AA (4.5:1 본문, 3:1 대형텍스트)
        await expectLater(
          tester,
          meetsGuideline(textContrastGuideline),
        );

        // 탭 가능한 요소 최소 크기 48×48
        await expectLater(
          tester,
          meetsGuideline(androidTapTargetGuideline),
        );

        // 레이블 없는 이미지 버튼 차단
        await expectLater(
          tester,
          meetsGuideline(labeledTapTargetGuideline),
        );

        handle.dispose();
      },
    );
  });
}
```

### Golden 테스트 예시 (textScaler 파라미터화)

```dart
// app/test/goldens/event_detail_golden_test.dart

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:your_app/screens/event_detail_screen.dart'; // TODO: 실제 경로

const _scales = [0.8, 1.0, 1.5, 2.0];

void main() {
  for (final scale in _scales) {
    testWidgets(
      'EventDetailScreen golden — textScaler $scale',
      (WidgetTester tester) async {
        // 폰트 스케일 주입
        await tester.pumpWidget(
          MaterialApp(
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(context).copyWith(
                textScaler: TextScaler.linear(scale),
              ),
              child: child!,
            ),
            home: const EventDetailScreen(
              // TODO(Day1PM): 테스트 픽스처 이벤트 모델 주입
            ),
          ),
        );

        await tester.pumpAndSettle();

        final scaleStr = scale.toString().replaceAll('.', '');
        await expectLater(
          find.byType(EventDetailScreen),
          matchesGoldenFile('goldens/event_detail_$scaleStr.png'),
        );
      },
    );
  }
}
```

### CI 통합 (GitHub Actions 예시)

```yaml
# .github/workflows/flutter_golden.yml
- name: Flutter Golden Test
  run: |
    cd app
    flutter test test/goldens/ --update-goldens  # 최초 기준 이미지 생성
    # 이후 CI에서는 --update-goldens 없이 실행하여 회귀 탐지
    flutter test test/goldens/
    flutter test test/accessibility/
```

---

## 권장 검사 가이드라인 체크리스트

- [ ] `textContrastGuideline` — WCAG AA 기준(본문 4.5:1, 대형텍스트 3:1)
- [ ] `androidTapTargetGuideline` — 최소 탭 영역 48×48dp
- [ ] `iOSTapTargetGuideline` — 최소 탭 영역 44×44pt
- [ ] `labeledTapTargetGuideline` — 모든 인터랙티브 요소에 semanticLabel
- [ ] overflow 없음 — `textScaler 2.0`에서 `RenderFlex overflow` 경고 0건
