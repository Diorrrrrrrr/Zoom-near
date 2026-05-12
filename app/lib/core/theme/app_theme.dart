import 'package:flutter/material.dart';
import '../design_tokens/tokens.dart';

/// ZOOM NEAR 라이트 테마 빌더
/// textScaler 정책: resolveTextScaler() — app_text_scaler.dart 참조
ThemeData buildLightTheme() {
  return ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme(
      brightness: Brightness.light,
      primary: SrColors.brandPrimary,
      onPrimary: SrColors.brandOn,
      secondary: SrColors.brandPrimary,
      onSecondary: SrColors.brandOn,
      error: SrColors.semanticDanger,
      onError: SrColors.brandOn,
      surface: SrColors.bgSurface,
      onSurface: SrColors.textPrimary,
    ),
    scaffoldBackgroundColor: SrColors.bgPrimary,
    fontFamily: 'Pretendard',
    textTheme: TextTheme(
      displayLarge: SrTypography.display,
      titleLarge: SrTypography.title,
      bodyLarge: SrTypography.bodyLg,
      bodyMedium: SrTypography.bodyMd,
      bodySmall: SrTypography.caption,
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        minimumSize: const Size(double.infinity, 64),
        backgroundColor: SrColors.brandPrimary,
        foregroundColor: SrColors.brandOn,
        textStyle: SrTypography.bodyLg,
        shape: const RoundedRectangleBorder(
          borderRadius: SrRadius.lgAll,
        ),
        animationDuration: SrMotion.normal,
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(double.infinity, 64),
        foregroundColor: SrColors.brandPrimary,
        textStyle: SrTypography.bodyLg,
        side: const BorderSide(color: SrColors.brandPrimary, width: 2),
        shape: const RoundedRectangleBorder(
          borderRadius: SrRadius.lgAll,
        ),
        animationDuration: SrMotion.normal,
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: SrColors.bgSurface,
      contentPadding: const EdgeInsets.symmetric(
        horizontal: SrSpacing.md,
        vertical: SrSpacing.md,
      ),
      labelStyle: SrTypography.bodyMd,
      hintStyle: SrTypography.bodyMd.copyWith(color: SrColors.textDisabled),
      border: const OutlineInputBorder(
        borderRadius: SrRadius.mdAll,
        borderSide: BorderSide(color: SrColors.textDisabled),
      ),
      enabledBorder: const OutlineInputBorder(
        borderRadius: SrRadius.mdAll,
        borderSide: BorderSide(color: SrColors.textDisabled),
      ),
      focusedBorder: const OutlineInputBorder(
        borderRadius: SrRadius.mdAll,
        borderSide: BorderSide(color: SrColors.brandPrimary, width: 2),
      ),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: SrColors.bgPrimary,
      foregroundColor: SrColors.textPrimary,
      elevation: 0,
      scrolledUnderElevation: 1,
      titleTextStyle: SrTypography.title,
    ),
    bottomNavigationBarTheme: const BottomNavigationBarThemeData(
      backgroundColor: SrColors.bgPrimary,
      selectedItemColor: SrColors.brandPrimary,
      unselectedItemColor: SrColors.textDisabled,
      selectedLabelStyle: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w700,
      ),
      unselectedLabelStyle: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w600,
      ),
      showSelectedLabels: true,
      showUnselectedLabels: true,
    ),
    cardTheme: const CardThemeData(
      color: SrColors.bgPrimary,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: SrRadius.lgAll,
        side: BorderSide(color: Color(0xFFE5E5E5)),
      ),
    ),
    dialogTheme: const DialogThemeData(
      backgroundColor: SrColors.bgPrimary,
      shape: RoundedRectangleBorder(borderRadius: SrRadius.lgAll),
      titleTextStyle: SrTypography.title,
      contentTextStyle: SrTypography.bodyMd,
    ),
  );
}
