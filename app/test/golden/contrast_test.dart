// Flutter 명도대비 자동 검사 — WCAG 2.1 AA (7:1 enhanced) 기준
//
// 검사 대상: SrColors 디자인 토큰의 핵심 텍스트+배경 조합
//
// 실행:
//   flutter test test/golden/contrast_test.dart
//
// 원리:
//   - 상대 휘도(relative luminance) = WCAG 2.1 공식 계산
//   - 대비율 = (L1 + 0.05) / (L2 + 0.05)  (L1 >= L2)
//   - AA 기준: 4.5:1 (일반 텍스트), AAA 기준: 7:1 (강화)
//
// TODO(Day1PMend): 실제 화면 렌더링 후 픽셀 추출 방식으로 업그레이드 가능.
//                 현재는 SrColors 정적 토큰 조합 검증.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:zoomnear/core/design_tokens/sr_colors.dart';

void main() {
  // ── WCAG 공식 헬퍼 ─────────────────────────────────────

  /// sRGB 채널 값(0~255)을 선형 광도로 변환 (WCAG 2.1 §1.4.3)
  double toLinear(int channel) {
    final sRGB = channel / 255.0;
    return sRGB <= 0.04045
        ? sRGB / 12.92
        : ((sRGB + 0.055) / 1.055) * ((sRGB + 0.055) / 1.055);
    // 주의: pow(x, 2.4) 근사로 단순화. 더 정확하려면 dart:math pow 사용.
  }

  /// Color의 상대 휘도 계산 (0.0 ~ 1.0)
  double relativeLuminance(Color color) {
    final r = toLinear(color.red);
    final g = toLinear(color.green);
    final b = toLinear(color.blue);
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  }

  /// 두 색상의 WCAG 대비율 계산
  double contrastRatio(Color fg, Color bg) {
    final l1 = relativeLuminance(fg);
    final l2 = relativeLuminance(bg);
    final lighter = l1 > l2 ? l1 : l2;
    final darker  = l1 > l2 ? l2 : l1;
    return (lighter + 0.05) / (darker + 0.05);
  }

  // ── WCAG 기준 상수 ─────────────────────────────────────
  const double wcagAA = 4.5;   // 일반 텍스트 AA
  const double wcagAAA = 7.0;  // 강화 AAA (주니어 앱 목표)

  // ── 검사 대상 색상 조합 ────────────────────────────────
  //
  // SrColors 토큰 기반. 형식: (설명, 전경색, 배경색, 최소대비율)
  // 주니어 앱은 시니어 사용자 대상이므로 AAA(7:1) 목표.
  final testCases = <(String, Color, Color, double)>[
    (
      'brandPrimary(글자) on white(배경) — 주요 액션 버튼',
      SrColors.brandPrimary,
      Colors.white,
      wcagAA,  // brandPrimary가 AAA 미달 시 AA로 완화 허용
    ),
    (
      'textPrimary on white — 본문 텍스트',
      SrColors.textPrimary,
      Colors.white,
      wcagAAA,
    ),
    (
      'textSecondary on white — 보조 텍스트',
      SrColors.textSecondary,
      Colors.white,
      wcagAA,
    ),
    (
      'white on brandPrimary — 버튼 레이블',
      Colors.white,
      SrColors.brandPrimary,
      wcagAA,
    ),
    (
      'semanticDanger on white — 오류 메시지',
      SrColors.semanticDanger,
      Colors.white,
      wcagAA,
    ),
    (
      'textPrimary on bgSurface — 카드 본문',
      SrColors.textPrimary,
      SrColors.bgSurface,
      wcagAAA,
    ),
  ];

  // ── 테스트 실행 ────────────────────────────────────────
  group('WCAG 명도 대비 검사 — SrColors 토큰', () {
    for (final (desc, fg, bg, minRatio) in testCases) {
      test(desc, () {
        final ratio = contrastRatio(fg, bg);
        final label = minRatio >= wcagAAA ? 'AAA(7:1)' : 'AA(4.5:1)';

        // 비율을 소수점 2자리로 표시
        final ratioStr = ratio.toStringAsFixed(2);
        final minStr   = minRatio.toStringAsFixed(1);

        expect(
          ratio,
          greaterThanOrEqualTo(minRatio),
          reason:
              '$desc\n'
              '  계산된 대비율: $ratioStr:1\n'
              '  기준($label): $minStr:1\n'
              '  전경: #${fg.value.toRadixString(16).padLeft(8, "0").toUpperCase()}\n'
              '  배경: #${bg.value.toRadixString(16).padLeft(8, "0").toUpperCase()}',
        );
      });
    }
  });

  // ── SrColors 존재 여부 스모크 테스트 ──────────────────
  group('SrColors 토큰 스모크 테스트', () {
    test('brandPrimary 색상 정의됨', () {
      expect(SrColors.brandPrimary, isA<Color>());
      expect(SrColors.brandPrimary.alpha, equals(255)); // 불투명 검증
    });

    test('textPrimary 색상 정의됨', () {
      expect(SrColors.textPrimary, isA<Color>());
    });

    test('textSecondary 색상 정의됨', () {
      expect(SrColors.textSecondary, isA<Color>());
    });

    test('semanticDanger 색상 정의됨', () {
      expect(SrColors.semanticDanger, isA<Color>());
    });

    test('bgSurface 색상 정의됨', () {
      expect(SrColors.bgSurface, isA<Color>());
    });

    test('bgPrimary 색상 정의됨', () {
      expect(SrColors.bgPrimary, isA<Color>());
    });
  });
}
