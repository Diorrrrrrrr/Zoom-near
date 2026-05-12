import 'package:flutter/material.dart';

/// ZOOM NEAR 보더 라디우스 토큰
abstract final class SrRadius {
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;

  static const BorderRadius smAll = BorderRadius.all(Radius.circular(sm));
  static const BorderRadius mdAll = BorderRadius.all(Radius.circular(md));
  static const BorderRadius lgAll = BorderRadius.all(Radius.circular(lg));
}
