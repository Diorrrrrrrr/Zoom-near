import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/tts/tts_controller.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_text.dart';

class TermsScreen extends ConsumerStatefulWidget {
  const TermsScreen({super.key});

  @override
  ConsumerState<TermsScreen> createState() => _TermsScreenState();
}

class _TermsScreenState extends ConsumerState<TermsScreen> {
  static const _spokenText = '이용약관 화면이에요.';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(ttsControllerProvider.notifier).speak(_spokenText);
    });
  }

  @override
  Widget build(BuildContext context) {
    return SrScaffold(
      appBar: SrAppBar(
        title: '이용약관',
        isModal: true,
        onSpeak: () =>
            ref.read(ttsControllerProvider.notifier).speak(_spokenText),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            SrText('제1조 (목적)', variant: SrTextVariant.bodyLg),
            SizedBox(height: SrSpacing.xs),
            SrText(
              '본 약관은 주니어(이하 "서비스")의 이용에 관한 기본적인 사항을 규정합니다.',
              variant: SrTextVariant.bodyMd,
              color: SrColors.textSecondary,
            ),
            SizedBox(height: SrSpacing.lg),
            SrText('제2조 (서비스 이용)', variant: SrTextVariant.bodyLg),
            SizedBox(height: SrSpacing.xs),
            SrText(
              '서비스는 만 60세 이상 시니어 이용자와 이를 지원하는 보호자를 위해 운영됩니다. '
              '이용자는 본 약관에 동의함으로써 서비스를 이용할 수 있습니다.',
              variant: SrTextVariant.bodyMd,
              color: SrColors.textSecondary,
            ),
            SizedBox(height: SrSpacing.lg),
            SrText('제3조 (개인정보 보호)', variant: SrTextVariant.bodyLg),
            SizedBox(height: SrSpacing.xs),
            SrText(
              '서비스는 이용자의 개인정보를 관련 법령에 따라 안전하게 관리합니다. '
              '자세한 내용은 개인정보처리방침을 참조해 주세요.',
              variant: SrTextVariant.bodyMd,
              color: SrColors.textSecondary,
            ),
            SizedBox(height: SrSpacing.lg),
            SrText('제4조 (포인트 정책)', variant: SrTextVariant.bodyLg),
            SizedBox(height: SrSpacing.xs),
            SrText(
              '포인트는 서비스 내 가상 재화로, 환금 및 양도가 불가합니다. '
              '회원 탈퇴 시 보유 포인트는 소멸됩니다.',
              variant: SrTextVariant.bodyMd,
              color: SrColors.textSecondary,
            ),
            SizedBox(height: SrSpacing.lg),
            SrText(
              '※ 이 약관은 서비스 오픈 전 임시 내용입니다. 정식 서비스 시 업데이트됩니다.',
              variant: SrTextVariant.caption,
              color: SrColors.textDisabled,
            ),
            SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }
}
