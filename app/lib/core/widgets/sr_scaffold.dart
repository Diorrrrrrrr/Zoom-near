import 'package:flutter/material.dart';
import '../design_tokens/tokens.dart';

/// 시니어 UX Scaffold 래퍼
/// - 배경 흰색 강제
/// - SafeArea 기본 on
class SrScaffold extends StatelessWidget {
  /// [appBar] SrAppBar 인스턴스
  /// [body] 화면 콘텐츠
  /// [bottomNavigationBar] SrBottomNav 인스턴스
  /// [floatingActionButton] FAB
  /// [safeArea] SafeArea 적용 여부 (기본 true)
  /// [resizeToAvoidBottomInset] 키보드 올라올 때 리사이즈 (기본 true)
  const SrScaffold({
    super.key,
    this.appBar,
    required this.body,
    this.bottomNavigationBar,
    this.floatingActionButton,
    this.safeArea = true,
    this.resizeToAvoidBottomInset = true,
    this.backgroundColor,
  });

  final PreferredSizeWidget? appBar;
  final Widget body;
  final Widget? bottomNavigationBar;
  final Widget? floatingActionButton;
  final bool safeArea;
  final bool resizeToAvoidBottomInset;
  final Color? backgroundColor;

  @override
  Widget build(BuildContext context) {
    final effectiveBody = safeArea
        ? SafeArea(child: body)
        : body;

    return Scaffold(
      backgroundColor: backgroundColor ?? const Color(0xFFF9FAFB),
      appBar: appBar,
      body: effectiveBody,
      bottomNavigationBar: bottomNavigationBar,
      floatingActionButton: floatingActionButton,
      resizeToAvoidBottomInset: resizeToAvoidBottomInset,
    );
  }
}
