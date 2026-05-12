import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_bottom_nav.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_input.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/auth/auth_provider.dart';
import '../../core/auth/auth_state.dart';
import '../../core/http/event_api.dart';
import '../../core/tts/tts_controller.dart';
import '../../core/stt/stt_controller.dart';
import '../notification/notification_provider.dart';
import 'event_model.dart';

final _eventsProvider = StateNotifierProvider<_EventsNotifier, _EventsState>(
  (ref) => _EventsNotifier(ref.read(eventApiProvider)),
);

class _EventsState {
  const _EventsState({
    this.items = const [],
    this.isLoading = false,
    this.isLoadingMore = false,
    this.hasMore = true,
    this.page = 0,
    this.query = '',
    this.error,
  });
  final List<EventSummary> items;
  final bool isLoading;
  final bool isLoadingMore;
  final bool hasMore;
  final int page;
  final String query;
  final String? error;

  _EventsState copyWith({
    List<EventSummary>? items,
    bool? isLoading,
    bool? isLoadingMore,
    bool? hasMore,
    int? page,
    String? query,
    String? error,
  }) =>
      _EventsState(
        items: items ?? this.items,
        isLoading: isLoading ?? this.isLoading,
        isLoadingMore: isLoadingMore ?? this.isLoadingMore,
        hasMore: hasMore ?? this.hasMore,
        page: page ?? this.page,
        query: query ?? this.query,
        error: error,
      );
}

class _EventsNotifier extends StateNotifier<_EventsState> {
  _EventsNotifier(this._api) : super(const _EventsState()) {
    load();
  }
  final EventApi _api;

  Future<void> load({String query = ''}) async {
    state = state.copyWith(isLoading: true, query: query, page: 0, items: []);
    try {
      final result = await _api.listEvents(q: query);
      state = state.copyWith(
        items: result.content,
        isLoading: false,
        page: 1,
        hasMore: result.content.length < result.totalElements,
      );
    } catch (_) {
      state = state.copyWith(
          isLoading: false, error: '모임 목록을 불러오지 못했어요. 다시 시도해 주세요.');
    }
  }

  Future<void> loadMore() async {
    if (!state.hasMore || state.isLoadingMore) return;
    state = state.copyWith(isLoadingMore: true);
    try {
      final result = await _api.listEvents(q: state.query, page: state.page);
      final all = [...state.items, ...result.content];
      state = state.copyWith(
        items: all,
        isLoadingMore: false,
        page: state.page + 1,
        hasMore: all.length < result.totalElements,
      );
    } catch (_) {
      state = state.copyWith(isLoadingMore: false);
    }
  }
}

class EventsScreen extends ConsumerStatefulWidget {
  const EventsScreen({super.key});

  @override
  ConsumerState<EventsScreen> createState() => _EventsScreenState();
}

class _EventsScreenState extends ConsumerState<EventsScreen> {
  final _searchCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  int _navIndex = 1;

  @override
  void initState() {
    super.initState();
    _scrollCtrl.addListener(_onScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(ttsControllerProvider.notifier)
          .speak('모임 목록 화면입니다. 원하는 모임을 찾아보세요.');
    });
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollCtrl.position.pixels >=
        _scrollCtrl.position.maxScrollExtent - 200) {
      ref.read(_eventsProvider.notifier).loadMore();
    }
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

  Future<void> _startStt() async {
    final sttState = ref.read(sttControllerProvider);
    if (sttState.isListening) {
      await ref.read(sttControllerProvider.notifier).cancel();
      return;
    }

    // 듣는 중 안내 모달 표시
    _showListeningModal();

    await ref.read(sttControllerProvider.notifier).start((text) {
      if (!mounted) return;
      _searchCtrl.text = text;
      // 모달 닫기
      if (Navigator.of(context).canPop()) Navigator.of(context).pop();
      // 자동 검색 트리거
      ref.read(_eventsProvider.notifier).load(query: text);
    });

    // STT 불가 시 모달 닫고 에러 표시
    final newState = ref.read(sttControllerProvider);
    if (newState.error != null && mounted) {
      if (Navigator.of(context).canPop()) Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(newState.error!),
          backgroundColor: SrColors.semanticDanger,
        ),
      );
    }
  }

  void _showListeningModal() {
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => _SttListeningDialog(
        onCancel: () async {
          await ref.read(sttControllerProvider.notifier).cancel();
          if (mounted && Navigator.of(context).canPop()) {
            Navigator.of(context).pop();
          }
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(_eventsProvider);
    final role = ref.watch(userRoleProvider);
    final canCreate = role == UserRole.tuntun || role == UserRole.manager;
    final unreadCount = ref.watch(unreadCountProvider);
    final sttState = ref.watch(sttControllerProvider);
    final ttsState = ref.watch(ttsControllerProvider);

    return SrScaffold(
      appBar: SrAppBar(
        title: '모임',
        automaticallyImplyLeading: false,
        isSpeaking: ttsState.isSpeaking,
        onSpeak: () => ref
            .read(ttsControllerProvider.notifier)
            .toggle('모임 목록 화면입니다.'),
      ),
      bottomNavigationBar: SrBottomNav(
        currentIndex: _navIndex,
        onTap: _onNavTap,
        unreadCount: unreadCount,
      ),
      floatingActionButton: canCreate
          ? FloatingActionButton.extended(
              onPressed: () => context.go('/events/create'),
              backgroundColor: SrColors.brandPrimary,
              icon: const Icon(Icons.add, color: SrColors.brandOn, size: 28),
              label: const SrText('모임 만들기',
                  variant: SrTextVariant.bodyMd, color: SrColors.brandOn),
            )
          : null,
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(
                SrSpacing.lg, SrSpacing.md, SrSpacing.lg, 0),
            child: Row(
              children: [
                Expanded(
                  child: SrInput(
                    label: '모임 검색',
                    hint: '모임 이름, 지역을 입력하세요',
                    controller: _searchCtrl,
                    textInputAction: TextInputAction.search,
                    onSubmitted: (v) =>
                        ref.read(_eventsProvider.notifier).load(query: v),
                  ),
                ),
                const SizedBox(width: SrSpacing.xs),
                // STT 마이크 버튼
                Semantics(
                  label: sttState.isListening ? '음성 인식 취소' : '음성으로 검색',
                  button: true,
                  child: GestureDetector(
                    onTap: _startStt,
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      width: 60,
                      height: 60,
                      margin: const EdgeInsets.only(top: 28),
                      decoration: BoxDecoration(
                        color: sttState.isListening
                            ? SrColors.semanticDanger
                            : SrColors.brandPrimary,
                        borderRadius: SrRadius.mdAll,
                      ),
                      child: Icon(
                        sttState.isListening ? Icons.mic_off : Icons.mic,
                        color: SrColors.brandOn,
                        size: 28,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: SrSpacing.md),
          Expanded(
            child: state.isLoading
                ? const Center(
                    child: CircularProgressIndicator(
                        color: SrColors.brandPrimary))
                : state.error != null
                    ? _ErrorView(
                        message: state.error!,
                        onRetry: () =>
                            ref.read(_eventsProvider.notifier).load())
                    : state.items.isEmpty
                        ? const _EmptyView()
                        : ListView.separated(
                            controller: _scrollCtrl,
                            padding: const EdgeInsets.symmetric(
                                horizontal: SrSpacing.lg),
                            itemCount: state.items.length +
                                (state.isLoadingMore ? 1 : 0),
                            separatorBuilder: (_, __) =>
                                const SizedBox(height: SrSpacing.md),
                            itemBuilder: (context, i) {
                              if (i == state.items.length) {
                                return const Center(
                                  child: Padding(
                                    padding: EdgeInsets.all(SrSpacing.lg),
                                    child: CircularProgressIndicator(
                                        color: SrColors.brandPrimary),
                                  ),
                                );
                              }
                              final ev = state.items[i];
                              return _EventCard(
                                event: ev,
                                onTap: () => context.go('/events/${ev.id}'),
                              );
                            },
                          ),
          ),
        ],
      ),
    );
  }
}

/// STT 듣는 중 안내 모달
class _SttListeningDialog extends StatefulWidget {
  const _SttListeningDialog({required this.onCancel});
  final VoidCallback onCancel;

  @override
  State<_SttListeningDialog> createState() => _SttListeningDialogState();
}

class _SttListeningDialogState extends State<_SttListeningDialog>
    with SingleTickerProviderStateMixin {
  late AnimationController _animCtrl;
  late Animation<double> _blinkAnim;

  @override
  void initState() {
    super.initState();
    _animCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    )..repeat(reverse: true);
    _blinkAnim = Tween<double>(begin: 0.3, end: 1.0).animate(_animCtrl);
  }

  @override
  void dispose() {
    _animCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: SrColors.bgPrimary,
      shape: const RoundedRectangleBorder(borderRadius: SrRadius.lgAll),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          FadeTransition(
            opacity: _blinkAnim,
            child: const Icon(Icons.mic,
                size: 64, color: SrColors.semanticDanger),
          ),
          const SizedBox(height: SrSpacing.md),
          const SrText('말씀해 주세요', variant: SrTextVariant.title),
          const SizedBox(height: SrSpacing.xs),
          const SrText(
            '검색할 모임을 말씀해 주세요.',
            variant: SrTextVariant.bodyMd,
            color: SrColors.textSecondary,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: SrSpacing.lg),
          SrButton(
            label: '취소',
            onPressed: widget.onCancel,
            variant: SrButtonVariant.secondary,
            size: SrButtonSize.medium,
          ),
        ],
      ),
    );
  }
}

class _EventCard extends StatelessWidget {
  const _EventCard({required this.event, required this.onTap});
  final EventSummary event;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
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
                      overflow: TextOverflow.ellipsis)),
              if (event.isManagerProgram)
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: const BoxDecoration(
                      color: Color(0xFFFFF7ED),
                      borderRadius: SrRadius.smAll),
                  child: const SrText('매니저',
                      variant: SrTextVariant.caption,
                      color: SrColors.brandPrimary),
                ),
            ],
          ),
          const SizedBox(height: SrSpacing.xs),
          Row(
            children: [
              const Icon(Icons.location_on_outlined,
                  size: 20, color: SrColors.textSecondary),
              const SizedBox(width: 4),
              SrText(event.regionText,
                  variant: SrTextVariant.bodyMd,
                  color: SrColors.textSecondary),
            ],
          ),
          const SizedBox(height: 4),
          Row(
            children: [
              const Icon(Icons.access_time,
                  size: 20, color: SrColors.textSecondary),
              const SizedBox(width: 4),
              Expanded(
                child: SrText(
                  '${_fmt(event.startsAt)} ~ ${_fmt(event.endsAt)}',
                  variant: SrTextVariant.bodyMd,
                  color: SrColors.textSecondary,
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
          if (event.isFull) ...[
            const SizedBox(height: SrSpacing.xs),
            Row(
              children: const [
                Icon(Icons.block, size: 20, color: SrColors.semanticDanger),
                SizedBox(width: 4),
                SrText('마감',
                    variant: SrTextVariant.bodyMd,
                    color: SrColors.semanticDanger),
              ],
            ),
          ],
        ],
      ),
    );
  }

  String _fmt(String iso) {
    if (iso.length < 16) return iso;
    return iso.substring(0, 16).replaceFirst('T', ' ');
  }
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline,
                size: 48, color: SrColors.semanticDanger),
            const SizedBox(height: SrSpacing.md),
            SrText(message,
                variant: SrTextVariant.bodyMd,
                color: SrColors.semanticDanger,
                textAlign: TextAlign.center),
            const SizedBox(height: SrSpacing.lg),
            SrButton(
                label: '다시 시도',
                onPressed: onRetry,
                size: SrButtonSize.large),
          ],
        ),
      ),
    );
  }
}

class _EmptyView extends StatelessWidget {
  const _EmptyView();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Padding(
        padding: EdgeInsets.all(SrSpacing.lg),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.event_busy, size: 60, color: SrColors.textDisabled),
            SizedBox(height: SrSpacing.md),
            SrText('열려있는 모임이 없어요',
                variant: SrTextVariant.bodyLg,
                color: SrColors.textSecondary),
          ],
        ),
      ),
    );
  }
}
