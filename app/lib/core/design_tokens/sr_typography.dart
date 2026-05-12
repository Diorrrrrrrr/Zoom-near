import 'package:flutter/material.dart';
import 'sr_colors.dart';

/// ZOOM NEAR YOLD(액티브 시니어) 타이포그래피 토큰
/// 50~70대 디지털 리터러시 + 노안 양립. 폰트 굵기와 여백으로 위계 표현.
/// 모든 사이즈는 OS 폰트 스케일러로 자동 확대(상한 2.0).
abstract final class SrTypography {
  static const String _fontFamily = 'Pretendard';
  static const List<String> _fontFamilyFallback = ['Noto Sans KR', 'sans-serif'];

  static const double _lineHeight = 1.6;

  /// display: 28sp / weight 700 — 화면 메인 타이틀(랜딩, 가입 성공 등)
  static const TextStyle display = TextStyle(
    fontFamily: _fontFamily,
    fontFamilyFallback: _fontFamilyFallback,
    fontSize: 28,
    fontWeight: FontWeight.w700,
    height: _lineHeight,
    color: SrColors.textPrimary,
  );

  /// title: 22sp / weight 700 — 섹션·카드 타이틀
  static const TextStyle title = TextStyle(
    fontFamily: _fontFamily,
    fontFamilyFallback: _fontFamilyFallback,
    fontSize: 22,
    fontWeight: FontWeight.w700,
    height: _lineHeight,
    color: SrColors.textPrimary,
  );

  /// bodyLg: 18sp / weight 600 — 강조 본문, 버튼 라벨, 주요 CTA
  static const TextStyle bodyLg = TextStyle(
    fontFamily: _fontFamily,
    fontFamilyFallback: _fontFamilyFallback,
    fontSize: 18,
    fontWeight: FontWeight.w600,
    height: _lineHeight,
    color: SrColors.textPrimary,
  );

  /// bodyMd: 16sp / weight 400 — 일반 본문 (정보 밀도가 필요한 영역)
  static const TextStyle bodyMd = TextStyle(
    fontFamily: _fontFamily,
    fontFamilyFallback: _fontFamilyFallback,
    fontSize: 16,
    fontWeight: FontWeight.w400,
    height: _lineHeight,
    color: SrColors.textPrimary,
  );

  /// caption: 14sp / weight 500 — 보조 메타(시각·작성자), 명도 대비 7:1 유지 위해 textSecondary 사용
  static const TextStyle caption = TextStyle(
    fontFamily: _fontFamily,
    fontFamilyFallback: _fontFamilyFallback,
    fontSize: 14,
    fontWeight: FontWeight.w500,
    height: _lineHeight,
    color: SrColors.textSecondary,
  );
}
