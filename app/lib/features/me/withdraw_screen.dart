import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/auth/auth_provider.dart';
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

class WithdrawScreen extends ConsumerStatefulWidget {
  const WithdrawScreen({super.key});

  @override
  ConsumerState<WithdrawScreen> createState() => _WithdrawScreenState();
}

class _WithdrawScreenState extends ConsumerState<WithdrawScreen> {
  final _pwCtrl = TextEditingController();
  bool _isLoading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(ttsControllerProvider.notifier)
          .speak('회원 탈퇴 화면이에요. 탈퇴하시면 모든 정보가 삭제돼요.');
    });
  }

  @override
  void dispose() {
    _pwCtrl.dispose();
    super.dispose();
  }

  Future<void> _onWithdraw() async {
    final pw = _pwCtrl.text.trim();
    if (pw.isEmpty) {
      setState(() => _error = '비밀번호를 입력해 주세요.');
      return;
    }
    setState(() => _error = null);

    // 1단계 확인
    final first = await SrConfirmDialog.show(
      context,
      title: '정말 탈퇴하시겠어요?',
      message: '탈퇴하시면 모든 정보가 삭제되고 복구되지 않아요.',
      confirmLabel: '계속하기',
      isDangerous: true,
    );
    if (!first || !mounted) return;

    // 2단계 확인
    final second = await SrConfirmDialog.show(
      context,
      title: '마지막으로 확인해요',
      message: '포인트, 모임 내역, 연동 정보가 모두 삭제돼요.\n정말 탈퇴하시겠어요?',
      confirmLabel: '탈퇴하기',
      cancelLabel: '취소',
      isDangerous: true,
    );
    if (!second || !mounted) return;

    setState(() => _isLoading = true);
    try {
      // TODO(Day2PM): DELETE /api/v1/me 엔드포인트 백엔드 추가 후 활성화
      // final dio = ref.read(dioProvider);
      // await dio.delete<void>('/api/v1/me', data: {'password': pw});
      await Future.delayed(const Duration(milliseconds: 500)); // placeholder
      if (!mounted) return;
      await ref.read(authProvider.notifier).logout();
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _error = '탈퇴 처리 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return SrScaffold(
      appBar: SrAppBar(
        title: '회원 탈퇴',
        isModal: true,
        onSpeak: () => ref
            .read(ttsControllerProvider.notifier)
            .speak('회원 탈퇴 화면이에요.'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 경고 카드
            SrCard(
              color: const Color(0xFFFFF1F1),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Row(
                    children: [
                      Icon(Icons.warning_amber_rounded,
                          size: 32, color: SrColors.semanticDanger),
                      SizedBox(width: SrSpacing.xs),
                      SrText('탈퇴 전 꼭 확인해 주세요',
                          variant: SrTextVariant.bodyLg,
                          color: SrColors.semanticDanger),
                    ],
                  ),
                  SizedBox(height: SrSpacing.md),
                  SrText(
                    '탈퇴하시면 모든 정보가 삭제되고 복구되지 않습니다.',
                    variant: SrTextVariant.bodyMd,
                    color: SrColors.semanticDanger,
                  ),
                  SizedBox(height: SrSpacing.xs),
                  SrText(
                    '• 보유 포인트 전액 소멸\n• 모임 참여 내역 삭제\n• 연동된 계정 정보 삭제\n• 재가입 시 복구 불가',
                    variant: SrTextVariant.bodyMd,
                    color: SrColors.textSecondary,
                  ),
                ],
              ),
            ),
            const SizedBox(height: SrSpacing.xl),
            SrInput(
              label: '비밀번호 확인',
              hint: '현재 비밀번호를 입력해 주세요',
              controller: _pwCtrl,
              obscureText: true,
              keyboardType: TextInputType.visiblePassword,
              textInputAction: TextInputAction.done,
              errorText: _error,
            ),
            const SizedBox(height: SrSpacing.xl),
            SrButton(
              label: '탈퇴하기',
              variant: SrButtonVariant.danger,
              onPressed: _onWithdraw,
              isLoading: _isLoading,
              size: SrButtonSize.large,
            ),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }
}
