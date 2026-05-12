import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/tts/tts_controller.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_text.dart';

class HelpScreen extends ConsumerStatefulWidget {
  const HelpScreen({super.key});

  @override
  ConsumerState<HelpScreen> createState() => _HelpScreenState();
}

class _HelpScreenState extends ConsumerState<HelpScreen> {
  static const _spokenText = '도움말 화면이에요. 자주 묻는 질문을 모아놨어요.';

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
        title: '도움말',
        isModal: true,
        onSpeak: () =>
            ref.read(ttsControllerProvider.notifier).speak(_spokenText),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SrText('자주 묻는 질문', variant: SrTextVariant.title),
            const SizedBox(height: SrSpacing.md),
            ..._faqs.map((faq) => Padding(
                  padding: const EdgeInsets.only(bottom: SrSpacing.md),
                  child: _FaqCard(question: faq.$1, answer: faq.$2),
                )),
            const SizedBox(height: SrSpacing.lg),
            SrCard(
              color: const Color(0xFFFFF7ED),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Row(
                    children: [
                      Icon(Icons.headset_mic_outlined,
                          size: 28, color: SrColors.brandPrimary),
                      SizedBox(width: SrSpacing.xs),
                      SrText('고객 지원',
                          variant: SrTextVariant.bodyLg,
                          color: SrColors.brandPrimary),
                    ],
                  ),
                  SizedBox(height: SrSpacing.xs),
                  SrText(
                    '추가 문의사항은 help@zoomnear.kr 로 연락해 주세요.',
                    variant: SrTextVariant.bodyMd,
                    color: SrColors.textSecondary,
                  ),
                ],
              ),
            ),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }

  static const List<(String, String)> _faqs = [
    (
      '포인트는 어떻게 충전하나요?',
      '내 정보 화면에서 "충전하기" 버튼을 누르면 포인트를 충전할 수 있어요. 보호자(든든이)가 대신 충전해 드릴 수도 있어요.',
    ),
    (
      '모임에 어떻게 참여하나요?',
      '모임 탭에서 원하는 모임을 선택하고 "참여하기" 버튼을 누르세요. 포인트가 차감되며 참여가 완료돼요.',
    ),
    (
      '음성 읽어주기 기능이 뭔가요?',
      '각 화면 오른쪽 위의 "🔊 읽어주기" 버튼을 누르면 화면 내용을 음성으로 읽어줘요. 글자 크기 설정에서 텍스트 크기도 조정할 수 있어요.',
    ),
    (
      '연동(든든이 연결)은 어떻게 하나요?',
      '든든이(보호자)가 앱에서 초대 링크를 보내면, 시니어가 링크를 눌러 연동을 수락하면 돼요.',
    ),
    (
      '탈퇴하면 포인트는 어떻게 되나요?',
      '탈퇴하면 보유 포인트를 포함한 모든 정보가 영구 삭제되며 복구되지 않아요. 신중하게 결정해 주세요.',
    ),
  ];
}

class _FaqCard extends StatefulWidget {
  const _FaqCard({required this.question, required this.answer});
  final String question;
  final String answer;

  @override
  State<_FaqCard> createState() => _FaqCardState();
}

class _FaqCardState extends State<_FaqCard> {
  bool _expanded = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => setState(() => _expanded = !_expanded),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.all(SrSpacing.md),
        decoration: BoxDecoration(
          color: _expanded ? const Color(0xFFFFF7ED) : SrColors.bgSurface,
          borderRadius: SrRadius.mdAll,
          border: Border.all(
            color:
                _expanded ? SrColors.brandPrimary : SrColors.textDisabled,
            width: _expanded ? 1.5 : 1,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: SrText(
                    widget.question,
                    variant: SrTextVariant.bodyLg,
                    color: _expanded
                        ? SrColors.brandPrimary
                        : SrColors.textPrimary,
                  ),
                ),
                Icon(
                  _expanded ? Icons.expand_less : Icons.expand_more,
                  size: 28,
                  color: _expanded
                      ? SrColors.brandPrimary
                      : SrColors.textSecondary,
                ),
              ],
            ),
            if (_expanded) ...[
              const SizedBox(height: SrSpacing.sm),
              SrText(
                widget.answer,
                variant: SrTextVariant.bodyMd,
                color: SrColors.textSecondary,
              ),
            ],
          ],
        ),
      ),
    );
  }
}
