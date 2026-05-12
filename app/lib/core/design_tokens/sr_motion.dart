import 'package:flutter/material.dart';

/// ZOOM NEAR 모션 토큰 — easeOut만 사용
abstract final class SrMotion {
  static const Duration fast = Duration(milliseconds: 150);
  static const Duration normal = Duration(milliseconds: 250);

  static const Curve curve = Curves.easeOut;

  static final AnimationStyle fastStyle = AnimationStyle(
    curve: Curves.easeOut,
    duration: Duration(milliseconds: 150),
  );

  static final AnimationStyle normalStyle = AnimationStyle(
    curve: Curves.easeOut,
    duration: Duration(milliseconds: 250),
  );
}
