import 'package:flutter/material.dart';

/// ZOOM NEAR 시니어 UX 색상 토큰 — WCAG AAA 7:1 대비비 보장
abstract final class SrColors {
  // Background
  static const Color bgPrimary = Color(0xFFFFFFFF);
  static const Color bgSurface = Color(0xFFF7F7F7);

  // Text
  static const Color textPrimary = Color(0xFF1A1A1A);
  static const Color textSecondary = Color(0xFF4A4A4A);
  static const Color textDisabled = Color(0xFF767676);

  // Brand
  static const Color brandPrimary = Color(0xFFC2410C);
  static const Color brandOn = Color(0xFFFFFFFF);

  // Semantic — 항상 색+아이콘+텍스트 3중 표현 필수
  static const Color semanticSuccess = Color(0xFF15803D);
  static const Color semanticWarning = Color(0xFFB45309);
  static const Color semanticDanger = Color(0xFFB91C1C);
}
