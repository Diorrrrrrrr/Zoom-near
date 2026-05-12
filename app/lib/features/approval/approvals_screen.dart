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
import '../../core/http/approval_api.dart';
import '../../core/tts/tts_controller.dart';
import 'approval_model.dart';

final _approvalsProvider = FutureProvider<List<ApprovalItem>>(
  (ref) => ref.read(approvalApiProvider).myPending(),
);

class ApprovalsScreen extends ConsumerStatefulWidget {
  const ApprovalsScreen({super.key});

  @override
  ConsumerState<ApprovalsScreen> createState() => _ApprovalsScreenState();
}

class _ApprovalsScreenState extends ConsumerState<ApprovalsScreen> {
  final Set<String> _processingIds = {};

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(ttsControllerProvider.notifier).speak('승인 요청 화면입니다. 자녀의 모임 참여 요청을 확인하세요.');
    });
  }

  Future<void> _approve(ApprovalItem item) async {
    final confirmed = await SrConfirmDialog.show(
      context,
      title: '참여 승인',
      message: '${item.requesterName} 님의 "${item.eventTitle}" 모임 참여를 승인할까요?',
      confirmLabel: '승인',
    );
    if (!confirmed || !mounted) return;
    setState(() => _processingIds.add(item.id));
    try {
      await ref.read(approvalApiProvider).approve(item.id);
      ref.invalidate(_approvalsProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${item.requesterName} 님의 참여를 승인했어요.'), backgroundColor: SrColors.semanticSuccess),
      );
    } on Exception {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('승인 처리 중 문제가 생겼어요.'), backgroundColor: SrColors.semanticDanger),
      );
    } finally {
      if (mounted) setState(() => _processingIds.remove(item.id));
    }
  }

  Future<void> _reject(ApprovalItem item) async {
    final confirmed = await SrConfirmDialog.show(
      context,
      title: '참여 거절',
      message: '${item.requesterName} 님의 "${item.eventTitle}" 모임 참여를 거절할까요?',
      confirmLabel: '거절',
      isDangerous: true,
    );
    if (!confirmed || !mounted) return;
    setState(() => _processingIds.add(item.id));
    try {
      await ref.read(approvalApiProvider).reject(item.id);
      ref.invalidate(_approvalsProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${item.requesterName} 님의 참여를 거절했어요.'), backgroundColor: SrColors.semanticWarning),
      );
    } on Exception {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('거절 처리 중 문제가 생겼어요.'), backgroundColor: SrColors.semanticDanger),
      );
    } finally {
      if (mounted) setState(() => _processingIds.remove(item.id));
    }
  }

  @override
  Widget build(BuildContext context) {
    final asyncItems = ref.watch(_approvalsProvider);

    return SrScaffold(
      appBar: SrAppBar(
        title: '승인 요청',
        onSpeak: () => ref.read(ttsControllerProvider.notifier).speak('승인 요청 화면입니다.'),
      ),
      body: asyncItems.when(
        loading: () => const Center(child: CircularProgressIndicator(color: SrColors.brandPrimary)),
        error: (_, __) => const Center(
          child: Padding(
            padding: EdgeInsets.all(SrSpacing.lg),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.error_outline, size: 48, color: SrColors.semanticDanger),
                SizedBox(height: SrSpacing.md),
                SrText('승인 요청 목록을 불러오지 못했어요.', variant: SrTextVariant.bodyMd, color: SrColors.semanticDanger),
              ],
            ),
          ),
        ),
        data: (items) => items.isEmpty
            ? const Center(
                child: Padding(
                  padding: EdgeInsets.all(SrSpacing.lg),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.check_circle_outline, size: 60, color: SrColors.semanticSuccess),
                      SizedBox(height: SrSpacing.md),
                      SrText('대기 중인 승인 요청이 없어요', variant: SrTextVariant.bodyLg, color: SrColors.textSecondary),
                    ],
                  ),
                ),
              )
            : ListView.separated(
                padding: const EdgeInsets.all(SrSpacing.lg),
                itemCount: items.length,
                separatorBuilder: (_, __) => const SizedBox(height: SrSpacing.md),
                itemBuilder: (context, i) {
                  final item = items[i];
                  final isProcessing = _processingIds.contains(item.id);
                  return SrCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.person, size: 32, color: SrColors.brandPrimary),
                            const SizedBox(width: SrSpacing.xs),
                            Expanded(
                              child: SrText(
                                '${item.requesterName} 님이 "${item.eventTitle}" 모임 참여를 요청했어요',
                                variant: SrTextVariant.bodyLg,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: SrSpacing.xs),
                        Row(
                          children: [
                            const Icon(Icons.access_time, size: 20, color: SrColors.semanticWarning),
                            const SizedBox(width: 4),
                            const SrText('만료: ', variant: SrTextVariant.bodyMd, color: SrColors.semanticWarning),
                            SrText(_fmtDate(item.expiresAt), variant: SrTextVariant.bodyMd, color: SrColors.semanticWarning),
                          ],
                        ),
                        const SizedBox(height: SrSpacing.lg),
                        Row(
                          children: [
                            Expanded(
                              child: SrButton(
                                label: '거절',
                                onPressed: isProcessing ? () {} : () => _reject(item),
                                variant: SrButtonVariant.danger,
                                size: SrButtonSize.large,
                                enabled: !isProcessing,
                              ),
                            ),
                            const SizedBox(width: SrSpacing.md),
                            Expanded(
                              child: SrButton(
                                label: '승인',
                                onPressed: isProcessing ? () {} : () => _approve(item),
                                isLoading: isProcessing,
                                size: SrButtonSize.large,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  );
                },
              ),
      ),
    );
  }

  String _fmtDate(String iso) {
    if (iso.length < 16) return iso;
    return iso.substring(0, 16).replaceFirst('T', ' ');
  }
}
