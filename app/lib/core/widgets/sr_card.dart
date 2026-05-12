import 'package:flutter/material.dart';
import '../design_tokens/tokens.dart';

/// YOLD 친화 카드 위젯 — 라운드 16, 흰 배경, 미세한 보더 + 부드러운 그림자
/// 프리미엄 톤을 위해 elevation 대신 light shadow 활용
class SrCard extends StatelessWidget {
  /// [child] 카드 내부 콘텐츠
  /// [padding] 내부 패딩 (기본 md)
  /// [onTap] 탭 핸들러
  /// [elevated] true 시 부드러운 그림자 추가 (목록 카드 등)
  const SrCard({
    super.key,
    required this.child,
    this.padding,
    this.onTap,
    this.color,
    this.elevated = false,
  });

  final Widget child;
  final EdgeInsetsGeometry? padding;
  final VoidCallback? onTap;
  final Color? color;
  final bool elevated;

  @override
  Widget build(BuildContext context) {
    final container = Container(
      decoration: BoxDecoration(
        color: color ?? SrColors.bgPrimary,
        borderRadius: SrRadius.lgAll,
        border: Border.all(color: const Color(0xFFEDEDED)),
        boxShadow: elevated
            ? const [
                BoxShadow(
                  color: Color(0x0A000000),
                  blurRadius: 12,
                  offset: Offset(0, 2),
                ),
              ]
            : null,
      ),
      padding: padding ?? const EdgeInsets.all(SrSpacing.md),
      child: child,
    );

    if (onTap != null) {
      return Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: SrRadius.lgAll,
          splashColor: const Color(0x0FC2410C),
          highlightColor: const Color(0x08C2410C),
          child: container,
        ),
      );
    }
    return container;
  }
}
