import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/http/notification_api.dart';
import 'notification_model.dart';

/// 알림 목록 + 미읽음 카운트 상태
class NotificationsState {
  const NotificationsState({
    this.items = const [],
    this.isLoading = false,
    this.error,
  });

  final List<NotificationItem> items;
  final bool isLoading;
  final String? error;

  int get unreadCount => items.where((i) => i.isUnread).length;

  NotificationsState copyWith({
    List<NotificationItem>? items,
    bool? isLoading,
    String? error,
  }) =>
      NotificationsState(
        items: items ?? this.items,
        isLoading: isLoading ?? this.isLoading,
        error: error,
      );
}

/// 알림 Notifier — 30초 polling, 읽음 처리 포함
class NotificationsNotifier extends AutoDisposeNotifier<NotificationsState> {
  Timer? _timer;

  @override
  NotificationsState build() {
    ref.onDispose(() {
      _timer?.cancel();
    });
    // 첫 로드
    Future.microtask(fetch);
    // 30초마다 자동 갱신
    _timer = Timer.periodic(const Duration(seconds: 30), (_) => fetch());
    return const NotificationsState(isLoading: true);
  }

  Future<void> fetch() async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final items = await ref
          .read(notificationApiProvider)
          .listNotifications(unreadOnly: false, limit: 50);
      state = state.copyWith(items: items, isLoading: false);
    } catch (_) {
      state = state.copyWith(
        isLoading: false,
        error: '알림을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
      );
    }
  }

  Future<void> markRead(NotificationItem item) async {
    if (!item.isUnread) return;
    try {
      await ref.read(notificationApiProvider).markRead(item.id);
      // 낙관적 업데이트: 목록에서 readAt 갱신
      final now = DateTime.now().toIso8601String();
      final updated = state.items.map((n) {
        if (n.id == item.id) {
          return NotificationItem(
            id: n.id,
            type: n.type,
            title: n.title,
            body: n.body,
            createdAt: n.createdAt,
            readAt: now,
            payload: n.payload,
          );
        }
        return n;
      }).toList();
      state = state.copyWith(items: updated);
    } catch (_) {
      // 읽음 처리 실패는 조용히 무시
    }
  }
}

final notificationsProvider =
    NotifierProvider.autoDispose<NotificationsNotifier, NotificationsState>(
  NotificationsNotifier.new,
);

/// 미읽음 카운트만 필요한 provider (BottomNav 배지용)
final unreadCountProvider = Provider.autoDispose<int>((ref) {
  return ref.watch(notificationsProvider).unreadCount;
});
