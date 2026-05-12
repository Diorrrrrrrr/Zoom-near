import 'package:flutter/material.dart';
import '../design_tokens/tokens.dart';
import 'sr_text.dart';

/// 시니어 UX 2단 확인 다이얼로그
class SrConfirmDialog extends StatelessWidget {
  /// [title] 다이얼로그 제목
  /// [message] 본문 메시지
  /// [confirmLabel] 확인 버튼 텍스트
  /// [cancelLabel] 취소 버튼 텍스트
  /// [onConfirm] 확인 콜백
  /// [onCancel] 취소 콜백
  /// [isDangerous] true 시 확인 버튼을 danger 색상으로
  const SrConfirmDialog({
    super.key,
    required this.title,
    required this.message,
    this.confirmLabel = '확인',
    this.cancelLabel = '취소',
    required this.onConfirm,
    this.onCancel,
    this.isDangerous = false,
  });

  final String title;
  final String message;
  final String confirmLabel;
  final String cancelLabel;
  final VoidCallback onConfirm;
  final VoidCallback? onCancel;
  final bool isDangerous;

  static Future<bool> show(
    BuildContext context, {
    required String title,
    required String message,
    String confirmLabel = '확인',
    String cancelLabel = '취소',
    bool isDangerous = false,
  }) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (_) => SrConfirmDialog(
        title: title,
        message: message,
        confirmLabel: confirmLabel,
        cancelLabel: cancelLabel,
        onConfirm: () => Navigator.of(context).pop(true),
        onCancel: () => Navigator.of(context).pop(false),
        isDangerous: isDangerous,
      ),
    );
    return result ?? false;
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: SrColors.bgPrimary,
      shape: const RoundedRectangleBorder(borderRadius: SrRadius.lgAll),
      title: SrText(title, variant: SrTextVariant.title),
      content: SrText(message, variant: SrTextVariant.bodyMd),
      contentPadding: const EdgeInsets.fromLTRB(
        SrSpacing.lg, SrSpacing.sm, SrSpacing.lg, SrSpacing.md,
      ),
      actionsPadding: const EdgeInsets.all(SrSpacing.md),
      actionsAlignment: MainAxisAlignment.spaceEvenly,
      actions: [
        SizedBox(
          height: 56,
          child: OutlinedButton(
            onPressed: onCancel ?? () => Navigator.of(context).pop(false),
            style: OutlinedButton.styleFrom(
              foregroundColor: SrColors.textSecondary,
              side: const BorderSide(color: SrColors.textDisabled),
              shape: const RoundedRectangleBorder(borderRadius: SrRadius.mdAll),
              textStyle: SrTypography.bodyMd,
              padding: const EdgeInsets.symmetric(horizontal: SrSpacing.lg),
            ),
            child: SrText(cancelLabel, variant: SrTextVariant.bodyMd, color: SrColors.textSecondary),
          ),
        ),
        SizedBox(
          height: 56,
          child: ElevatedButton(
            onPressed: onConfirm,
            style: ElevatedButton.styleFrom(
              backgroundColor: isDangerous ? SrColors.semanticDanger : SrColors.brandPrimary,
              foregroundColor: SrColors.brandOn,
              shape: const RoundedRectangleBorder(borderRadius: SrRadius.mdAll),
              textStyle: SrTypography.bodyMd,
              padding: const EdgeInsets.symmetric(horizontal: SrSpacing.lg),
            ),
            child: SrText(confirmLabel, variant: SrTextVariant.bodyMd, color: SrColors.brandOn),
          ),
        ),
      ],
    );
  }
}
