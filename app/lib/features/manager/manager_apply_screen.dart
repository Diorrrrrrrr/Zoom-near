import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/http/dio_provider.dart';
import '../../core/tts/tts_controller.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_confirm_dialog.dart';
import '../../core/widgets/sr_input.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_text.dart';

class ManagerApplyScreen extends ConsumerStatefulWidget {
  const ManagerApplyScreen({super.key});

  @override
  ConsumerState<ManagerApplyScreen> createState() => _ManagerApplyScreenState();
}

class _ManagerApplyScreenState extends ConsumerState<ManagerApplyScreen> {
  final _reasonCtrl = TextEditingController();
  bool _isSubmitting = false;
  bool _applied = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(ttsControllerProvider.notifier)
          .speak('매니저 신청 화면이에요. 신청 사유를 입력해 주세요.');
    });
  }

  @override
  void dispose() {
    _reasonCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final reason = _reasonCtrl.text.trim();
    if (reason.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('신청 사유를 입력해 주세요.'),
          backgroundColor: SrColors.semanticDanger,
        ),
      );
      return;
    }

    final confirmed = await SrConfirmDialog.show(
      context,
      title: '매니저 신청',
      message: '매니저로 신청하시겠어요?\n관리자 승인 후 활동할 수 있어요.',
      confirmLabel: '신청하기',
    );
    if (!confirmed || !mounted) return;

    setState(() => _isSubmitting = true);
    try {
      final dio = ref.read(dioProvider);
      await dio.post<void>(
        '/api/v1/manager/apply',
        data: {'reason': reason},
      );
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _applied = true;
      });
      ref
          .read(ttsControllerProvider.notifier)
          .speak('매니저 신청이 완료됐어요. 관리자 승인을 기다려 주세요.');
    } catch (_) {
      if (!mounted) return;
      setState(() => _isSubmitting = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('신청 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.'),
          backgroundColor: SrColors.semanticDanger,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return SrScaffold(
      appBar: SrAppBar(
        title: '매니저 신청',
        onSpeak: () => ref
            .read(ttsControllerProvider.notifier)
            .speak('매니저 신청 화면이에요.'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: _applied ? _buildDone() : _buildForm(),
      ),
    );
  }

  Widget _buildForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SrCard(
          color: const Color(0xFFFFF7ED),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: const [
              Row(
                children: [
                  Icon(Icons.info_outline,
                      size: 28, color: SrColors.brandPrimary),
                  SizedBox(width: SrSpacing.xs),
                  SrText('매니저란?',
                      variant: SrTextVariant.bodyLg,
                      color: SrColors.brandPrimary),
                ],
              ),
              SizedBox(height: SrSpacing.sm),
              SrText(
                '매니저는 시니어분들을 위한 프로그램을 직접 만들고 운영할 수 있어요. 관리자 검토 후 승인이 완료되면 활동을 시작할 수 있어요.',
                variant: SrTextVariant.bodyMd,
                color: SrColors.textSecondary,
              ),
            ],
          ),
        ),
        const SizedBox(height: SrSpacing.lg),
        SrInput(
          label: '신청 사유',
          hint: '매니저로 활동하고 싶은 이유를 자유롭게 적어 주세요.',
          controller: _reasonCtrl,
          maxLines: 5,
          keyboardType: TextInputType.multiline,
          textInputAction: TextInputAction.newline,
        ),
        const SizedBox(height: SrSpacing.xl),
        SrButton(
          label: '매니저 신청하기',
          onPressed: _submit,
          isLoading: _isSubmitting,
          size: SrButtonSize.large,
        ),
        const SizedBox(height: SrSpacing.xxl),
      ],
    );
  }

  Widget _buildDone() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: SrSpacing.xxl),
        const Center(
          child: Icon(Icons.check_circle_outline,
              size: 80, color: SrColors.semanticSuccess),
        ),
        const SizedBox(height: SrSpacing.lg),
        const SrText(
          '신청이 완료됐어요!',
          variant: SrTextVariant.title,
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: SrSpacing.md),
        const SrText(
          '관리자 승인 대기 중이에요.\n승인이 완료되면 알림으로 안내해 드릴게요.',
          variant: SrTextVariant.bodyMd,
          color: SrColors.textSecondary,
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: SrSpacing.xxl),
        SrButton(
          label: '홈으로 돌아가기',
          onPressed: () => context.go('/home'),
          size: SrButtonSize.large,
          variant: SrButtonVariant.secondary,
        ),
        const SizedBox(height: SrSpacing.xxl),
      ],
    );
  }
}
