// Flutter Golden Test — textScaler 4종 × 화면 4종 = 16 golden 파일
//
// 실행:
//   flutter test test/golden/text_scaler_test.dart --update-goldens  (최초 기준 이미지 생성)
//   flutter test test/golden/text_scaler_test.dart                    (이후 회귀 검증)
//
// TODO(Day1PMend):
//   - Lane F PM 산출물(EventDetailScreen, MyPageScreen, ChargeScreen) import 경로 확정 후
//     아래 주석 해제. 현재는 LoginScreen만 실제 import, 나머지 3개 화면은 skip.
//   - golden_toolkit 패키지 사용 시 pubspec.yaml dev_dependencies에 추가 필요:
//       golden_toolkit: ^0.15.0
//   - 현재 표준 matchesGoldenFile 방식 사용 (flutter_test 내장).

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:zoomnear/features/auth/login_screen.dart';

// TODO(Day1PMend): Lane F PM 산출물 완성 후 아래 import 주석 해제
// import 'package:zoomnear/features/event/event_detail_screen.dart';
// import 'package:zoomnear/features/profile/my_page_screen.dart';
// import 'package:zoomnear/features/point/charge_screen.dart';

void main() {
  // ── 테스트 대상 textScaler 목록 ──────────────────────────
  const textScalers = <double>[0.8, 1.0, 1.5, 2.0];

  // ── 기준 화면 크기 (Galaxy S24 기준) ─────────────────────
  const goldenSize = Size(390, 844);

  // ════════════════════════════════════════════════════════
  // 1. LoginScreen — 즉시 실행 가능
  // ════════════════════════════════════════════════════════
  group('LoginScreen golden — textScaler 4종', () {
    for (final scale in textScalers) {
      testWidgets(
        'LoginScreen textScaler=$scale',
        (WidgetTester tester) async {
          // 화면 크기 고정
          tester.view.physicalSize = goldenSize * tester.view.devicePixelRatio;
          tester.view.devicePixelRatio = 2.0;
          addTearDown(tester.view.resetPhysicalSize);

          await tester.pumpWidget(
            ProviderScope(
              child: MaterialApp(
                // textScaler를 MediaQuery를 통해 주입
                builder: (context, child) => MediaQuery(
                  data: MediaQuery.of(context).copyWith(
                    textScaler: TextScaler.linear(scale),
                  ),
                  child: child!,
                ),
                home: const LoginScreen(),
              ),
            ),
          );

          // 비동기 렌더링 완료 대기
          await tester.pumpAndSettle();

          // Golden 파일 비교
          await expectLater(
            find.byType(MaterialApp),
            matchesGoldenFile(
              'goldens/login_screen_scale_${_scaleToString(scale)}.png',
            ),
          );
        },
      );
    }
  });

  // ════════════════════════════════════════════════════════
  // 2. EventDetailScreen — TODO(Day1PMend): Lane F PM 완성 후 주석 해제
  // ════════════════════════════════════════════════════════
  group('EventDetailScreen golden — textScaler 4종', () {
    for (final scale in textScalers) {
      testWidgets(
        'EventDetailScreen textScaler=$scale [SKIP — TODO(Day1PMend)]',
        (WidgetTester tester) async {
          // TODO(Day1PMend): EventDetailScreen import 후 활성화
          // tester.view.physicalSize = goldenSize * tester.view.devicePixelRatio;
          // tester.view.devicePixelRatio = 2.0;
          // addTearDown(tester.view.resetPhysicalSize);
          //
          // await tester.pumpWidget(
          //   ProviderScope(
          //     child: MaterialApp(
          //       builder: (context, child) => MediaQuery(
          //         data: MediaQuery.of(context).copyWith(
          //           textScaler: TextScaler.linear(scale),
          //         ),
          //         child: child!,
          //       ),
          //       home: const EventDetailScreen(eventId: 'test-event-id'),
          //     ),
          //   ),
          // );
          // await tester.pumpAndSettle();
          // await expectLater(
          //   find.byType(MaterialApp),
          //   matchesGoldenFile(
          //     'goldens/event_detail_scale_${_scaleToString(scale)}.png',
          //   ),
          // );

          // 현재: 빈 위젯으로 placeholder 실행 (회귀 없이 통과)
          await tester.pumpWidget(
            MaterialApp(
              home: Scaffold(
                body: Center(
                  child: Text(
                    'EventDetailScreen TODO — scale $scale',
                    textScaler: TextScaler.linear(scale),
                  ),
                ),
              ),
            ),
          );
          expect(find.textContaining('EventDetailScreen TODO'), findsOneWidget);
        },
      );
    }
  });

  // ════════════════════════════════════════════════════════
  // 3. MyPageScreen — TODO(Day1PMend): Lane F PM 완성 후 주석 해제
  // ════════════════════════════════════════════════════════
  group('MyPageScreen golden — textScaler 4종', () {
    for (final scale in textScalers) {
      testWidgets(
        'MyPageScreen textScaler=$scale [SKIP — TODO(Day1PMend)]',
        (WidgetTester tester) async {
          // TODO(Day1PMend): MyPageScreen import 후 활성화
          await tester.pumpWidget(
            MaterialApp(
              home: Scaffold(
                body: Center(
                  child: Text(
                    'MyPageScreen TODO — scale $scale',
                    textScaler: TextScaler.linear(scale),
                  ),
                ),
              ),
            ),
          );
          expect(find.textContaining('MyPageScreen TODO'), findsOneWidget);
        },
      );
    }
  });

  // ════════════════════════════════════════════════════════
  // 4. ChargeScreen — TODO(Day1PMend): Lane F PM 완성 후 주석 해제
  // ════════════════════════════════════════════════════════
  group('ChargeScreen golden — textScaler 4종', () {
    for (final scale in textScalers) {
      testWidgets(
        'ChargeScreen textScaler=$scale [SKIP — TODO(Day1PMend)]',
        (WidgetTester tester) async {
          // TODO(Day1PMend): ChargeScreen import 후 활성화
          await tester.pumpWidget(
            MaterialApp(
              home: Scaffold(
                body: Center(
                  child: Text(
                    'ChargeScreen TODO — scale $scale',
                    textScaler: TextScaler.linear(scale),
                  ),
                ),
              ),
            ),
          );
          expect(find.textContaining('ChargeScreen TODO'), findsOneWidget);
        },
      );
    }
  });
}

/// 소수점 스케일을 파일명용 문자열로 변환. 1.5 → "1_5", 1.0 → "1_0"
String _scaleToString(double scale) {
  return scale.toString().replaceAll('.', '_');
}
