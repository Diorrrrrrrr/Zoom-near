import 'package:flutter/material.dart';
import '../design_tokens/tokens.dart';

/// 4탭 고정 바텀 네비게이션
/// - 라벨 항상 표시
/// - 아이콘 32dp
/// - [unreadCount] 알림 탭(index 2)에 미읽음 배지 표시
class SrBottomNav extends StatelessWidget {
  /// [currentIndex] 현재 선택된 탭 인덱스
  /// [onTap] 탭 선택 콜백
  /// [unreadCount] 알림 탭 미읽음 카운트 (0이면 배지 숨김)
  const SrBottomNav({
    super.key,
    required this.currentIndex,
    required this.onTap,
    this.unreadCount = 0,
  });

  final int currentIndex;
  final ValueChanged<int> onTap;
  final int unreadCount;

  @override
  Widget build(BuildContext context) {
    return BottomNavigationBar(
      currentIndex: currentIndex,
      onTap: onTap,
      type: BottomNavigationBarType.fixed,
      backgroundColor: SrColors.bgPrimary,
      selectedItemColor: SrColors.brandPrimary,
      unselectedItemColor: SrColors.textDisabled,
      showSelectedLabels: true,
      showUnselectedLabels: true,
      selectedFontSize: 14,
      unselectedFontSize: 14,
      iconSize: 32,
      items: [
        const BottomNavigationBarItem(
          icon: Icon(Icons.home_outlined, size: 32),
          activeIcon: Icon(Icons.home, size: 32, color: SrColors.brandPrimary),
          label: '홈',
        ),
        const BottomNavigationBarItem(
          icon: Icon(Icons.event_outlined, size: 32),
          activeIcon:
              Icon(Icons.event, size: 32, color: SrColors.brandPrimary),
          label: '이벤트',
        ),
        // 알림 탭 — 미읽음 배지
        BottomNavigationBarItem(
          icon: _NotificationIcon(
            isActive: false,
            unreadCount: unreadCount,
          ),
          activeIcon: _NotificationIcon(
            isActive: true,
            unreadCount: unreadCount,
          ),
          label: '알림',
        ),
        const BottomNavigationBarItem(
          icon: Icon(Icons.person_outlined, size: 32),
          activeIcon:
              Icon(Icons.person, size: 32, color: SrColors.brandPrimary),
          label: '내정보',
        ),
      ],
    );
  }
}

/// 알림 아이콘 + 미읽음 배지
class _NotificationIcon extends StatelessWidget {
  const _NotificationIcon({
    required this.isActive,
    required this.unreadCount,
  });

  final bool isActive;
  final int unreadCount;

  @override
  Widget build(BuildContext context) {
    final icon = Icon(
      isActive ? Icons.notifications : Icons.notifications_outlined,
      size: 32,
      color: isActive ? SrColors.brandPrimary : SrColors.textDisabled,
    );

    if (unreadCount <= 0) return icon;

    return Stack(
      clipBehavior: Clip.none,
      children: [
        icon,
        Positioned(
          top: -4,
          right: -6,
          child: Container(
            constraints: const BoxConstraints(minWidth: 20, minHeight: 20),
            padding: const EdgeInsets.symmetric(horizontal: 4),
            decoration: const BoxDecoration(
              color: SrColors.semanticDanger,
              borderRadius: BorderRadius.all(Radius.circular(10)),
            ),
            child: Center(
              child: Text(
                unreadCount > 99 ? '99+' : '$unreadCount',
                style: const TextStyle(
                  color: SrColors.brandOn,
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  height: 1.0,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
