import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../design_tokens/tokens.dart';
import 'sr_text.dart';

/// 시니어 UX 텍스트 입력 위젯
/// - label 위치 고정 (Floating 금지)
/// - 큰 글씨 hint
class SrInput extends StatelessWidget {
  /// [label] 입력 필드 라벨 (위에 고정)
  /// [hint] placeholder 텍스트
  /// [controller] TextEditingController
  /// [onChanged] 값 변경 콜백
  /// [keyboardType] 키보드 타입
  /// [obscureText] 비밀번호 입력 여부
  /// [enabled] 활성화 여부
  /// [errorText] 에러 메시지
  /// [maxLines] 최대 줄 수
  /// [inputFormatters] 입력 포매터
  /// [textInputAction] 키보드 액션
  /// [onSubmitted] 완료 콜백
  const SrInput({
    super.key,
    required this.label,
    this.hint,
    this.controller,
    this.onChanged,
    this.keyboardType,
    this.obscureText = false,
    this.enabled = true,
    this.errorText,
    this.maxLines = 1,
    this.inputFormatters,
    this.textInputAction,
    this.onSubmitted,
    this.focusNode,
  });

  final String label;
  final String? hint;
  final TextEditingController? controller;
  final ValueChanged<String>? onChanged;
  final TextInputType? keyboardType;
  final bool obscureText;
  final bool enabled;
  final String? errorText;
  final int maxLines;
  final List<TextInputFormatter>? inputFormatters;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onSubmitted;
  final FocusNode? focusNode;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        SrText(label, variant: SrTextVariant.bodyLg),
        const SizedBox(height: SrSpacing.xs),
        TextField(
          controller: controller,
          onChanged: onChanged,
          keyboardType: keyboardType,
          obscureText: obscureText,
          enabled: enabled,
          maxLines: maxLines,
          inputFormatters: inputFormatters,
          textInputAction: textInputAction,
          onSubmitted: onSubmitted,
          focusNode: focusNode,
          style: SrTypography.bodyLg,
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: SrTypography.bodyLg.copyWith(color: SrColors.textDisabled),
            errorText: errorText,
            errorStyle: SrTypography.caption.copyWith(color: SrColors.semanticDanger),
            filled: true,
            fillColor: enabled ? SrColors.bgPrimary : const Color(0xFFF3F4F6),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: SrSpacing.md,
              vertical: SrSpacing.md,
            ),
            border: const OutlineInputBorder(
              borderRadius: SrRadius.mdAll,
              borderSide: BorderSide(color: Color(0xFFE5E7EB)),
            ),
            enabledBorder: const OutlineInputBorder(
              borderRadius: SrRadius.mdAll,
              borderSide: BorderSide(color: Color(0xFFE5E7EB)),
            ),
            focusedBorder: const OutlineInputBorder(
              borderRadius: SrRadius.mdAll,
              borderSide: BorderSide(color: SrColors.brandPrimary, width: 2),
            ),
            errorBorder: const OutlineInputBorder(
              borderRadius: SrRadius.mdAll,
              borderSide: BorderSide(color: SrColors.semanticDanger, width: 2),
            ),
            focusedErrorBorder: const OutlineInputBorder(
              borderRadius: SrRadius.mdAll,
              borderSide: BorderSide(color: SrColors.semanticDanger, width: 2),
            ),
            // Floating label 명시적 비활성화
            floatingLabelBehavior: FloatingLabelBehavior.never,
          ),
        ),
      ],
    );
  }
}
