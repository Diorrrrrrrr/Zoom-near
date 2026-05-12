import 'package:flutter/material.dart';
import '../design_tokens/tokens.dart';
import 'sr_confirm_dialog.dart';
import 'sr_text.dart';

/// Sr 버튼 variant
enum SrButtonVariant { primary, secondary, danger }

/// Sr 버튼 크기 — YOLD 친화 (h56 large는 최소 48dp+α 접근성 + 모던 비주얼)
enum SrButtonSize {
  large(56),
  medium(48);

  const SrButtonSize(this.height);

  /// 버튼 높이 (dp)
  final double height;
}

/// 시니어 UX 버튼 위젯
/// - variant: primary / secondary / danger
/// - size: large(h64) / medium(h56)
/// - requiresConfirm: true 시 SrConfirmDialog 거친 후 onPressed 호출
class SrButton extends StatelessWidget {
  /// [label] 버튼 텍스트
  /// [onPressed] 탭 핸들러
  /// [variant] 버튼 스타일 variant
  /// [size] 버튼 크기
  /// [icon] 아이콘 (선택)
  /// [requiresConfirm] true 시 확인 다이얼로그 표시 후 onPressed 실행
  /// [confirmTitle] 확인 다이얼로그 제목
  /// [confirmMessage] 확인 다이얼로그 본문
  /// [isLoading] 로딩 상태
  /// [enabled] 활성화 여부
  const SrButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.variant = SrButtonVariant.primary,
    this.size = SrButtonSize.large,
    this.icon,
    this.requiresConfirm = false,
    this.confirmTitle = '확인하시겠어요?',
    this.confirmMessage = '이 작업을 진행합니다.',
    this.isLoading = false,
    this.enabled = true,
  });

  final String label;
  final VoidCallback onPressed;
  final SrButtonVariant variant;
  final SrButtonSize size;
  final Widget? icon;
  final bool requiresConfirm;
  final String confirmTitle;
  final String confirmMessage;
  final bool isLoading;
  final bool enabled;

  Future<void> _handlePress(BuildContext context) async {
    if (!enabled || isLoading) return;

    if (requiresConfirm) {
      final confirmed = await SrConfirmDialog.show(
        context,
        title: confirmTitle,
        message: confirmMessage,
        isDangerous: variant == SrButtonVariant.danger,
      );
      if (!confirmed) return;
    }
    onPressed();
  }

  @override
  Widget build(BuildContext context) {
    final effectiveEnabled = enabled && !isLoading;

    Widget labelWidget = SrText(
      label,
      variant: SrTextVariant.bodyLg,
      color: _labelColor(),
    );

    if (isLoading) {
      labelWidget = SizedBox(
        width: 24,
        height: 24,
        child: CircularProgressIndicator(
          strokeWidth: 2.5,
          color: _labelColor(),
        ),
      );
    }

    final child = icon != null && !isLoading
        ? Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              icon!,
              const SizedBox(width: SrSpacing.xs),
              labelWidget,
            ],
          )
        : labelWidget;

    const shape = RoundedRectangleBorder(borderRadius: SrRadius.mdAll);
    const buttonPadding = EdgeInsets.symmetric(horizontal: 24, vertical: 16);

    if (variant == SrButtonVariant.secondary) {
      return SizedBox(
        height: size.height,
        width: double.infinity,
        child: OutlinedButton(
          onPressed: effectiveEnabled ? () => _handlePress(context) : null,
          style: OutlinedButton.styleFrom(
            foregroundColor: SrColors.textPrimary,
            side: const BorderSide(color: Color(0xFFD1D5DB), width: 1.5),
            shape: shape,
            padding: buttonPadding,
            textStyle: SrTypography.bodyLg,
          ),
          child: child,
        ),
      );
    }

    return SizedBox(
      height: size.height,
      width: double.infinity,
      child: ElevatedButton(
        onPressed: effectiveEnabled ? () => _handlePress(context) : null,
        style: ElevatedButton.styleFrom(
          backgroundColor: _bgColor(),
          foregroundColor: SrColors.brandOn,
          disabledBackgroundColor: const Color(0xFFE5E5E5),
          shape: shape,
          padding: buttonPadding,
          textStyle: SrTypography.bodyLg,
        ),
        child: child,
      ),
    );
  }

  Color _bgColor() => switch (variant) {
        SrButtonVariant.primary => SrColors.brandPrimary,
        SrButtonVariant.secondary => SrColors.bgPrimary,
        SrButtonVariant.danger => SrColors.semanticDanger,
      };

  Color _labelColor() => switch (variant) {
        SrButtonVariant.secondary => SrColors.textPrimary,
        _ => SrColors.brandOn,
      };
}
