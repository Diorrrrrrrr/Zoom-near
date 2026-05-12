import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _kFontScaleKey = 'app_font_scale';

/// 앱 내 사용자 글자 크기 설정 enum
enum AppFontScale {
  small(0.9, '작게'),
  normal(1.0, '보통'),
  large(1.2, '크게'),
  extraLarge(1.5, '매우 크게');

  const AppFontScale(this.factor, this.label);

  /// 스케일 배율
  final double factor;

  /// 화면 표시 라벨
  final String label;

  static AppFontScale fromFactor(double f) {
    for (final v in values) {
      if ((v.factor - f).abs() < 0.01) return v;
    }
    return normal;
  }
}

/// 앱 글자 크기 설정 Notifier — SharedPreferences 영속화
class AppFontScaleNotifier extends AsyncNotifier<AppFontScale> {
  @override
  Future<AppFontScale> build() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getDouble(_kFontScaleKey);
    return saved != null ? AppFontScale.fromFactor(saved) : AppFontScale.normal;
  }

  Future<void> setScale(AppFontScale scale) async {
    state = const AsyncLoading();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble(_kFontScaleKey, scale.factor);
    state = AsyncData(scale);
  }
}

final appFontScaleProvider =
    AsyncNotifierProvider<AppFontScaleNotifier, AppFontScale>(
  AppFontScaleNotifier.new,
);

/// 시스템 textScaler와 앱 설정 중 큰 값을 채택, 상한 2.0
TextScaler resolveTextScaler(BuildContext context, AppFontScale appScale) {
  final systemScaler = MediaQuery.textScalerOf(context);
  final systemFactor = systemScaler.scale(1.0);
  final effective =
      systemFactor > appScale.factor ? systemFactor : appScale.factor;
  final clamped = effective > 2.0 ? 2.0 : effective;
  return TextScaler.linear(clamped);
}
