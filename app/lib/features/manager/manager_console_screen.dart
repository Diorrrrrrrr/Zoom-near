import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/http/event_api.dart';
import '../../core/tts/tts_controller.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_text.dart';
import '../event/event_model.dart';

final _managerEventsProvider = FutureProvider<List<EventSummary>>((ref) async {
  // 내가 만든 이벤트 목록 (is_manager_program=true 포함)
  final result = await ref.read(eventApiProvider).listEvents();
  return result.content;
});

class ManagerConsoleScreen extends ConsumerStatefulWidget {
  const ManagerConsoleScreen({super.key});

  @override
  ConsumerState<ManagerConsoleScreen> createState() =>
      _ManagerConsoleScreenState();
}

class _ManagerConsoleScreenState extends ConsumerState<ManagerConsoleScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(ttsControllerProvider.notifier)
          .speak('매니저 콘솔 화면이에요. 내가 등록한 프로그램을 관리할 수 있어요.');
    });
  }

  @override
  Widget build(BuildContext context) {
    final asyncEvents = ref.watch(_managerEventsProvider);

    return SrScaffold(
      appBar: SrAppBar(
        title: '매니저 콘솔',
        onSpeak: () => ref
            .read(ttsControllerProvider.notifier)
            .speak('매니저 콘솔 화면이에요.'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 새 프로그램 만들기 카드
            SrCard(
              color: const Color(0xFFFFF7ED),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.add_circle_outline,
                          size: 32, color: SrColors.brandPrimary),
                      SizedBox(width: SrSpacing.xs),
                      SrText('새 매니저 프로그램',
                          variant: SrTextVariant.bodyLg,
                          color: SrColors.brandPrimary),
                    ],
                  ),
                  const SizedBox(height: SrSpacing.sm),
                  const SrText(
                    '매니저 전용 프로그램을 만들어 시니어분들을 모아보세요.',
                    variant: SrTextVariant.bodyMd,
                    color: SrColors.textSecondary,
                  ),
                  const SizedBox(height: SrSpacing.md),
                  SrButton(
                    label: '프로그램 만들기',
                    onPressed: () => context.go('/events/create'),
                    size: SrButtonSize.large,
                  ),
                ],
              ),
            ),
            const SizedBox(height: SrSpacing.lg),

            // 내가 등록한 이벤트 목록
            const SrText('내가 등록한 프로그램', variant: SrTextVariant.title),
            const SizedBox(height: SrSpacing.md),
            asyncEvents.when(
              loading: () => const Center(
                child:
                    CircularProgressIndicator(color: SrColors.brandPrimary),
              ),
              error: (_, __) => Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline,
                        size: 48, color: SrColors.semanticDanger),
                    const SizedBox(height: SrSpacing.md),
                    const SrText(
                      '프로그램 목록을 불러오지 못했어요.',
                      variant: SrTextVariant.bodyMd,
                      color: SrColors.semanticDanger,
                    ),
                    const SizedBox(height: SrSpacing.md),
                    SrButton(
                      label: '다시 시도',
                      onPressed: () => ref.invalidate(_managerEventsProvider),
                      size: SrButtonSize.medium,
                    ),
                  ],
                ),
              ),
              data: (events) => events.isEmpty
                  ? const SrCard(
                      child: Column(
                        children: [
                          Icon(Icons.event_busy,
                              size: 48, color: SrColors.textDisabled),
                          SizedBox(height: SrSpacing.sm),
                          SrText(
                            '등록한 프로그램이 없어요.',
                            variant: SrTextVariant.bodyMd,
                            color: SrColors.textSecondary,
                          ),
                        ],
                      ),
                    )
                  : Column(
                      children: events
                          .map((ev) => Padding(
                                padding: const EdgeInsets.only(
                                    bottom: SrSpacing.md),
                                child: _ManagerEventCard(
                                  event: ev,
                                  onTap: () => context.go('/events/${ev.id}'),
                                ),
                              ))
                          .toList(),
                    ),
            ),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }
}

class _ManagerEventCard extends StatelessWidget {
  const _ManagerEventCard({required this.event, required this.onTap});
  final EventSummary event;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final statusColor =
        event.isFull ? SrColors.semanticDanger : SrColors.semanticSuccess;
    final statusLabel = event.isFull ? '마감' : '모집중';
    final statusIcon =
        event.isFull ? Icons.block : Icons.check_circle_outline;

    return SrCard(
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: SrText(event.title,
                    variant: SrTextVariant.bodyLg,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
              ),
              const SizedBox(width: SrSpacing.xs),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: statusColor.withOpacity(0.12),
                  borderRadius: SrRadius.smAll,
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(statusIcon, size: 16, color: statusColor),
                    const SizedBox(width: 4),
                    SrText(statusLabel,
                        variant: SrTextVariant.caption, color: statusColor),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: SrSpacing.xs),
          Row(
            children: [
              const Icon(Icons.people_outline,
                  size: 20, color: SrColors.textSecondary),
              const SizedBox(width: 4),
              SrText('${event.joinedCount}/${event.capacity}명',
                  variant: SrTextVariant.bodyMd,
                  color: SrColors.textSecondary),
              const Spacer(),
              Row(
                children: [
                  const Icon(Icons.monetization_on_outlined,
                      size: 20, color: SrColors.semanticDanger),
                  const SizedBox(width: 4),
                  SrText('${event.pointCost}P',
                      variant: SrTextVariant.bodyMd,
                      color: SrColors.semanticDanger),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }
}
