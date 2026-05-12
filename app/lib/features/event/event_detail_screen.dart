import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_confirm_dialog.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/auth/auth_provider.dart';
import '../../core/http/event_api.dart';
import '../../core/http/point_api.dart';
import '../../core/tts/tts_controller.dart';
import 'event_model.dart';

final _eventDetailProvider = FutureProvider.family<EventDetail, String>(
  (ref, id) => ref.read(eventApiProvider).getEvent(id),
);

class EventDetailScreen extends ConsumerStatefulWidget {
  const EventDetailScreen({super.key, required this.eventId});
  final String eventId;

  @override
  ConsumerState<EventDetailScreen> createState() => _EventDetailScreenState();
}

class _EventDetailScreenState extends ConsumerState<EventDetailScreen> {
  bool _isJoining = false;

  @override
  void initState() {
    super.initState();
    // TTS 자동 발화 — 화면 진입 시 데이터 로드 완료 후
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final detail = ref.read(_eventDetailProvider(widget.eventId)).value;
      if (detail != null) _speak(detail);
    });
  }

  String _buildSpokenText(EventDetail detail) {
    final starts = _fmt(detail.startsAt);
    final ends = _fmt(detail.endsAt);
    return '${detail.title}. '
        '장소: ${detail.regionText}. '
        '$starts부터 $ends까지. '
        '정원 ${detail.capacity}명. '
        '차감 포인트 ${detail.pointCost}점.';
  }

  void _speak(EventDetail detail) {
    ref.read(ttsControllerProvider.notifier).speak(_buildSpokenText(detail));
  }

  Future<void> _onJoin(EventDetail detail) async {
    try {
      final balance = await ref.read(pointApiProvider).getBalance();
      if (balance < detail.pointCost) {
        if (!mounted) return;
        final go = await SrConfirmDialog.show(
          context,
          title: '포인트가 부족해요',
          message: '현재 잔액(${balance}P)이 부족해요.\n충전 화면으로 이동할까요?',
          confirmLabel: '충전하기',
          cancelLabel: '취소',
        );
        if (go && mounted) context.go('/me/charge');
        return;
      }
    } catch (_) {
      // 잔액 조회 실패해도 참여 시도
    }

    final confirmed = await SrConfirmDialog.show(
      context,
      title: '모임 참여',
      message: '"${detail.title}" 모임에 참여할까요?\n${detail.pointCost}P가 차감돼요.',
      confirmLabel: '참여하기',
    );
    if (!confirmed || !mounted) return;

    setState(() => _isJoining = true);
    try {
      await ref.read(eventApiProvider).joinEvent(widget.eventId);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('모임에 참여했어요!'),
          backgroundColor: SrColors.semanticSuccess,
        ),
      );
      ref.invalidate(_eventDetailProvider(widget.eventId));
    } on Exception {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('참여 중 문제가 생겼어요. 다시 시도해 주세요.'),
          backgroundColor: SrColors.semanticDanger,
        ),
      );
    } finally {
      if (mounted) setState(() => _isJoining = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final asyncDetail = ref.watch(_eventDetailProvider(widget.eventId));
    final ttsState = ref.watch(ttsControllerProvider);

    return SrScaffold(
      appBar: SrAppBar(
        title: '모임 상세',
        isSpeaking: ttsState.isSpeaking,
        onSpeak: () {
          final detail = asyncDetail.value;
          if (detail != null) {
            ref
                .read(ttsControllerProvider.notifier)
                .toggle(_buildSpokenText(detail));
          }
        },
      ),
      body: asyncDetail.when(
        loading: () => const Center(
            child: CircularProgressIndicator(color: SrColors.brandPrimary)),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(SrSpacing.lg),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline,
                    size: 48, color: SrColors.semanticDanger),
                const SizedBox(height: SrSpacing.md),
                const SrText('모임 정보를 불러오지 못했어요.',
                    variant: SrTextVariant.bodyMd,
                    color: SrColors.semanticDanger),
                const SizedBox(height: SrSpacing.lg),
                SrButton(
                  label: '다시 시도',
                  onPressed: () =>
                      ref.invalidate(_eventDetailProvider(widget.eventId)),
                ),
              ],
            ),
          ),
        ),
        data: (detail) => _buildDetail(detail),
      ),
    );
  }

  Widget _buildDetail(EventDetail detail) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(SrSpacing.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SrText(detail.title, variant: SrTextVariant.display),
          const SizedBox(height: SrSpacing.lg),
          SrCard(
            child: Column(
              children: [
                _InfoRow(
                    icon: Icons.location_on,
                    label: '장소',
                    value: detail.regionText),
                const Divider(height: SrSpacing.lg),
                _InfoRow(
                    icon: Icons.access_time,
                    label: '시작',
                    value: _fmt(detail.startsAt)),
                const Divider(height: SrSpacing.lg),
                _InfoRow(
                    icon: Icons.access_time_filled,
                    label: '종료',
                    value: _fmt(detail.endsAt)),
                const Divider(height: SrSpacing.lg),
                _InfoRow(
                    icon: Icons.people,
                    label: '정원',
                    value: '${detail.joinedCount}/${detail.capacity}명'),
                const Divider(height: SrSpacing.lg),
                Row(
                  children: [
                    const Icon(Icons.monetization_on,
                        size: 28, color: SrColors.semanticDanger),
                    const SizedBox(width: SrSpacing.xs),
                    const SrText('차감 포인트', variant: SrTextVariant.bodyMd),
                    const Spacer(),
                    SrText(
                      '${detail.pointCost}P',
                      variant: SrTextVariant.title,
                      color: SrColors.semanticDanger,
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: SrSpacing.lg),
          if (detail.description.isNotEmpty) ...[
            const SrText('모임 소개', variant: SrTextVariant.title),
            const SizedBox(height: SrSpacing.md),
            SrCard(
              child: SrText(detail.description, variant: SrTextVariant.bodyMd),
            ),
            const SizedBox(height: SrSpacing.lg),
          ],
          if (detail.creatorName != null)
            SrText('주최: ${detail.creatorName}',
                variant: SrTextVariant.bodyMd,
                color: SrColors.textSecondary),
          const SizedBox(height: SrSpacing.xxl),
          if (!detail.isFull && detail.status == 'OPEN')
            SrButton(
              label: '참여하기',
              onPressed: () => _onJoin(detail),
              isLoading: _isJoining,
              size: SrButtonSize.large,
            )
          else
            SrCard(
              color: const Color(0xFFF5F5F5),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    detail.isFull ? Icons.block : Icons.lock,
                    color: SrColors.textDisabled,
                    size: 24,
                  ),
                  const SizedBox(width: SrSpacing.xs),
                  SrText(
                    detail.isFull ? '정원이 마감됐어요' : '참여할 수 없는 모임이에요',
                    variant: SrTextVariant.bodyMd,
                    color: SrColors.textDisabled,
                  ),
                ],
              ),
            ),
          const SizedBox(height: SrSpacing.xl),
        ],
      ),
    );
  }

  String _fmt(String iso) {
    if (iso.length < 16) return iso;
    return iso.substring(0, 16).replaceFirst('T', ' ');
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow(
      {required this.icon, required this.label, required this.value});
  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 28, color: SrColors.textSecondary),
        const SizedBox(width: SrSpacing.xs),
        SrText(label,
            variant: SrTextVariant.bodyMd, color: SrColors.textSecondary),
        const Spacer(),
        SrText(value, variant: SrTextVariant.bodyMd),
      ],
    );
  }
}
