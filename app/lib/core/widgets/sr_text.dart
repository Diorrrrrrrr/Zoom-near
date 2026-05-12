import 'package:flutter/material.dart';
import '../design_tokens/tokens.dart';

/// Sr 텍스트 variant
enum SrTextVariant { display, title, bodyLg, bodyMd, caption }

/// 시니어 UX 텍스트 위젯 — textScaler 자동 적용
class SrText extends StatelessWidget {
  /// [text] 표시할 문자열
  /// [variant] 타이포그래피 토큰 variant
  /// [color] 글자 색상 (null 시 variant 기본값)
  /// [textAlign] 정렬
  /// [maxLines] 최대 줄 수
  const SrText(
    this.text, {
    super.key,
    this.variant = SrTextVariant.bodyMd,
    this.color,
    this.textAlign,
    this.maxLines,
    this.overflow,
  });

  final String text;
  final SrTextVariant variant;
  final Color? color;
  final TextAlign? textAlign;
  final int? maxLines;
  final TextOverflow? overflow;

  TextStyle _baseStyle() {
    return switch (variant) {
      SrTextVariant.display => SrTypography.display,
      SrTextVariant.title => SrTypography.title,
      SrTextVariant.bodyLg => SrTypography.bodyLg,
      SrTextVariant.bodyMd => SrTypography.bodyMd,
      SrTextVariant.caption => SrTypography.caption,
    };
  }

  @override
  Widget build(BuildContext context) {
    final style = color != null ? _baseStyle().copyWith(color: color) : _baseStyle();
    return Text(
      text,
      style: style,
      textAlign: textAlign,
      maxLines: maxLines,
      overflow: overflow,
    );
  }
}
