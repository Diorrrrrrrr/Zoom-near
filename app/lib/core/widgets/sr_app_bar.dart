import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../design_tokens/tokens.dart';
import 'sr_text.dart';

/// 시니어 UX 앱바 위젯
/// - 우측에 "🔊 읽어주기" 버튼 슬롯
/// - isSpeaking=true 이면 아이콘 brand color + 토글 의미
/// - isModal=true 이면 우측에 ✕ 닫기 버튼 표시 (pop)
/// - Navigator.canPop() 이면 좌측 뒤로가기(←) 자동 표시
class SrAppBar extends StatelessWidget implements PreferredSizeWidget {
  /// [title] 앱바 제목
  /// [onSpeak] 읽어주기 버튼 탭 콜백 (null 시 버튼 숨김)
  /// [isSpeaking] true 시 버튼 brand color (발화 중 표시)
  /// [leading] 좌측 위젯 (null 시 canPop 기반 자동 뒤로가기)
  /// [actions] 읽어주기 버튼 앞에 추가할 액션들
  /// [automaticallyImplyLeading] 자동 뒤로가기 버튼 표시 여부
  /// [isModal] true 시 우측 닫기(✕) 버튼 표시
  const SrAppBar({
    super.key,
    required this.title,
    this.onSpeak,
    this.isSpeaking = false,
    this.leading,
    this.actions,
    this.automaticallyImplyLeading = true,
    this.isModal = false,
  });

  final String title;
  final VoidCallback? onSpeak;
  final bool isSpeaking;
  final Widget? leading;
  final List<Widget>? actions;
  final bool automaticallyImplyLeading;
  final bool isModal;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight + 1);

  @override
  Widget build(BuildContext context) {
    final canPop = Navigator.canPop(context);
    final currentLocation = GoRouterState.of(context).matchedLocation;
    // BottomNav 4탭은 leading 불필요(home/events/notifications/me 와 그 하위 제외 첫 단계)
    const bottomNavRoots = {'/home', '/events', '/notifications', '/me'};
    final isBottomNavRoot = bottomNavRoots.contains(currentLocation);

    // 좌측 뒤로가기 또는 홈으로 가는 fallback
    Widget? effectiveLeading = leading;
    if (effectiveLeading == null && !isModal && !isBottomNavRoot) {
      if (canPop) {
        effectiveLeading = Semantics(
          label: '뒤로가기',
          button: true,
          child: IconButton(
            icon: const Icon(Icons.arrow_back),
            color: SrColors.textPrimary,
            iconSize: 28,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
            onPressed: () => Navigator.pop(context),
          ),
        );
      } else {
        // pop 불가 → 홈으로 가는 fallback 버튼
        effectiveLeading = Semantics(
          label: '홈으로',
          button: true,
          child: IconButton(
            icon: const Icon(Icons.home_outlined),
            color: SrColors.textPrimary,
            iconSize: 28,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
            onPressed: () => context.go('/home'),
          ),
        );
      }
    }

    final speakButton = onSpeak != null
        ? Semantics(
            label: isSpeaking ? '읽기 멈추기' : '화면 내용 읽어주기',
            button: true,
            child: TextButton.icon(
              onPressed: onSpeak,
              icon: Text(
                '🔊',
                style: TextStyle(
                  fontSize: 20,
                  color: isSpeaking ? SrColors.brandPrimary : null,
                ),
              ),
              label: SrText(
                isSpeaking ? '멈추기' : '읽어주기',
                variant: SrTextVariant.caption,
                color: SrColors.brandPrimary,
              ),
              style: TextButton.styleFrom(
                minimumSize: const Size(60, 60),
                padding:
                    const EdgeInsets.symmetric(horizontal: SrSpacing.sm),
                backgroundColor: isSpeaking
                    ? const Color(0xFFFFF7ED)
                    : Colors.transparent,
                shape: const RoundedRectangleBorder(
                    borderRadius: SrRadius.mdAll),
              ),
            ),
          )
        : null;

    // 닫기 버튼: isModal=true 인 경우 우측에 표시
    final closeButton = isModal
        ? Semantics(
            label: '닫기',
            button: true,
            child: IconButton(
              icon: const Icon(Icons.close),
              color: SrColors.textPrimary,
              iconSize: 28,
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
              onPressed: () => Navigator.pop(context),
            ),
          )
        : null;

    final allActions = [
      ...?actions,
      if (speakButton != null) speakButton,
      if (closeButton != null) closeButton,
    ];

    return AppBar(
      backgroundColor: SrColors.bgPrimary,
      elevation: 0,
      scrolledUnderElevation: 0,
      bottom: PreferredSize(
        preferredSize: const Size.fromHeight(1),
        child: Container(
          height: 1,
          color: const Color(0xFFEDEDED),
        ),
      ),
      leading: effectiveLeading,
      automaticallyImplyLeading: isModal ? false : automaticallyImplyLeading,
      titleSpacing: effectiveLeading != null ? SrSpacing.xs : SrSpacing.lg,
      title: Text(
        title,
        style: const TextStyle(
          fontFamily: 'Pretendard',
          fontFamilyFallback: ['Noto Sans KR', 'sans-serif'],
          fontSize: 22,
          fontWeight: FontWeight.w700,
          color: SrColors.textPrimary,
          height: 1.3,
        ),
      ),
      centerTitle: false,
      actions: allActions.isEmpty ? null : allActions,
    );
  }
}
