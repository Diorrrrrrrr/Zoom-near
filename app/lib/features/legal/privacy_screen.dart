import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/tts/tts_controller.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_text.dart';

class PrivacyScreen extends ConsumerStatefulWidget {
  const PrivacyScreen({super.key});

  @override
  ConsumerState<PrivacyScreen> createState() => _PrivacyScreenState();
}

class _PrivacyScreenState extends ConsumerState<PrivacyScreen> {
  static const _spokenText = '개인정보처리방침 화면이에요.';

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
        title: '개인정보처리방침',
        isModal: true,
        onSpeak: () =>
            ref.read(ttsControllerProvider.notifier).speak(_spokenText),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            SrText('수집하는 개인정보', variant: SrTextVariant.bodyLg),
            SizedBox(height: SrSpacing.xs),
            SrText(
              '• 이름, 휴대폰 번호, 생년월일\n• 서비스 이용 기록, 접속 로그\n• 포인트 거래 내역',
              variant: SrTextVariant.bodyMd,
              color: SrColors.textSecondary,
            ),
            SizedBox(height: SrSpacing.lg),
            SrText('수집 목적', variant: SrTextVariant.bodyLg),
            SizedBox(height: SrSpacing.xs),
            SrText(
              '• 회원 식별 및 서비스 제공\n• 모임 참여 관리\n• 고객 지원 및 분쟁 처리',
              variant: SrTextVariant.bodyMd,
              color: SrColors.textSecondary,
            ),
            SizedBox(height: SrSpacing.lg),
            SrText('보유 및 이용 기간', variant: SrTextVariant.bodyLg),
            SizedBox(height: SrSpacing.xs),
            SrText(
              '회원 탈퇴 후 즉시 삭제합니다. 단, 관계 법령에 따라 일정 기간 보관이 필요한 경우 해당 기간 동안 보관합니다.',
              variant: SrTextVariant.bodyMd,
              color: SrColors.textSecondary,
            ),
            SizedBox(height: SrSpacing.lg),
            SrText('개인정보 보호 책임자', variant: SrTextVariant.bodyLg),
            SizedBox(height: SrSpacing.xs),
            SrText(
              '문의: privacy@zoomnear.kr',
              variant: SrTextVariant.bodyMd,
              color: SrColors.textSecondary,
            ),
            SizedBox(height: SrSpacing.lg),
            SrText(
              '※ 이 방침은 서비스 오픈 전 임시 내용입니다. 정식 서비스 시 업데이트됩니다.',
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
