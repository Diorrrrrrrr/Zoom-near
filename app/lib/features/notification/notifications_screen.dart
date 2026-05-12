import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_bottom_nav.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/tts/tts_controller.dart';
import 'notification_model.dart';
import 'notification_provider.dart';

class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});

  @override
  ConsumerState<NotificationsScreen> createState() =>
      _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
  int _navIndex = 2;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _speakSummary();
    });
  }

  void _speakSummary() {
    final count = ref.read(notificationsProvider).unreadCount;
    final text = count > 0
        ? count <= 10
            ? '읽지 않은 알림 ${count}개가 있어요.'
            : '읽지 않은 알림이 많이 있어요.'
        : '새 알림이 없어요.';
    ref.read(ttsControllerProvider.notifier).speak(text);
  }

  void _onNavTap(int index) {
    setState(() => _navIndex = index);
    switch (index) {
      case 0:
        context.go('/home');
      case 1:
        context.go('/events');
      case 2:
        context.go('/notifications');
      case 3:
        context.go('/me');
    }
  }

  void _onItemTap(NotificationItem item) {
    // 읽음 처리
    ref.read(notificationsProvider.notifier).markRead(item);
    // 딥링크: payload.eventId가 있으면 이벤트 상세로
    final eventId = item.payload?.eventId;
    if (eventId != null) {
      context.go('/events/$eventId');
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(notificationsProvider);

    return SrScaffold(
      appBar: SrAppBar(
        title: '알림',
        automaticallyImplyLeading: false,
        onSpeak: () => _speakSummary(),
      ),
      bottomNavigationBar:
          SrBottomNav(currentIndex: _navIndex, onTap: _onNavTap),
      body: RefreshIndicator(
        color: SrColors.brandPrimary,
        onRefresh: () => ref.read(notificationsProvider.notifier).fetch(),
        child: _buildBody(state),
      ),
    );
  }

  Widget _buildBody(NotificationsState state) {
    if (state.isLoading && state.items.isEmpty) {
      return const Center(
        child: CircularProgressIndicator(color: SrColors.brandPrimary),
      );
    }

    if (state.error != null && state.items.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(SrSpacing.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline,
                  size: 48, color: SrColors.semanticDanger),
              const SizedBox(height: SrSpacing.md),
              SrText(
                state.error!,
                variant: SrTextVariant.bodyMd,
                color: SrColors.semanticDanger,
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      );
    }

    if (state.items.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(SrSpacing.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.notifications_off_outlined,
                  size: 60, color: SrColors.textDisabled),
              SizedBox(height: SrSpacing.md),
              SrText('알림이 없어요',
                  variant: SrTextVariant.bodyLg,
                  color: SrColors.textSecondary),
            ],
          ),
        ),
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(SrSpacing.lg),
      itemCount: state.items.length,
      separatorBuilder: (_, __) => const SizedBox(height: SrSpacing.md),
      itemBuilder: (context, i) {
        final item = state.items[i];
        return _NotificationCard(
          item: item,
          onTap: () => _onItemTap(item),
        );
      },
    );
  }
}

class _NotificationCard extends StatelessWidget {
  const _NotificationCard({required this.item, required this.onTap});
  final NotificationItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SrCard(
      color: item.isUnread ? const Color(0xFFFFF7ED) : SrColors.bgPrimary,
      onTap: onTap,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Stack(
            children: [
              Icon(
                _iconForType(item.type),
                size: 32,
                color:
                    item.isUnread ? SrColors.brandPrimary : SrColors.textDisabled,
              ),
              if (item.isUnread)
                Positioned(
                  top: 0,
                  right: 0,
                  child: Container(
                    width: 10,
                    height: 10,
                    decoration: const BoxDecoration(
                      color: SrColors.semanticDanger,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(width: SrSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SrText(item.title, variant: SrTextVariant.bodyLg),
                const SizedBox(height: 4),
                SrText(item.body,
                    variant: SrTextVariant.bodyMd,
                    color: SrColors.textSecondary),
                const SizedBox(height: 4),
                SrText(
                  _fmtDate(item.createdAt),
                  variant: SrTextVariant.caption,
                  color: SrColors.textDisabled,
                ),
              ],
            ),
          ),
          if (item.isUnread)
            const Icon(Icons.circle, size: 12, color: SrColors.brandPrimary),
        ],
      ),
    );
  }

  IconData _iconForType(String type) {
    return switch (type) {
      'LINKAGE_CREATED' => Icons.handshake_outlined,
      'EVENT_JOIN_REQUEST' => Icons.person_add_outlined,
      'EVENT_JOIN_APPROVED' => Icons.check_circle_outline,
      'EVENT_JOIN_REJECTED' => Icons.cancel_outlined,
      'MOCK_TOPUP_RECEIVED' => Icons.monetization_on_outlined,
      _ => Icons.notifications_outlined,
    };
  }

  String _fmtDate(String iso) {
    if (iso.length < 16) return iso;
    return iso.substring(0, 16).replaceFirst('T', ' ');
  }
}
