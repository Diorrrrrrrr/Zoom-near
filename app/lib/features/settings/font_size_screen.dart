import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/theme/app_text_scaler.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/tts/tts_controller.dart';

class FontSizeScreen extends ConsumerWidget {
  const FontSizeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final asyncScale = ref.watch(appFontScaleProvider);
    final currentScale =
        asyncScale.valueOrNull ?? AppFontScale.normal;

    return SrScaffold(
      appBar: SrAppBar(
        title: '글자 크기 설정',
        isModal: true,
        onSpeak: () => ref
            .read(ttsControllerProvider.notifier)
            .speak('글자 크기 설정 화면입니다. 원하시는 크기를 선택해 주세요.'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SrText(
              '글자 크기를 선택해 주세요',
              variant: SrTextVariant.title,
            ),
            const SizedBox(height: SrSpacing.md),
            ...AppFontScale.values.map(
              (scale) => Padding(
                padding: const EdgeInsets.only(bottom: SrSpacing.md),
                child: _FontScaleOption(
                  scale: scale,
                  isSelected: currentScale == scale,
                  onTap: () async {
                    await ref
                        .read(appFontScaleProvider.notifier)
                        .setScale(scale);
                    ref
                        .read(ttsControllerProvider.notifier)
                        .speak('글자 크기를 ${scale.label}(으)로 변경했어요.');
                  },
                ),
              ),
            ),
            const SizedBox(height: SrSpacing.lg),
            SrCard(
              color: SrColors.bgSurface,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SrText('미리보기', variant: SrTextVariant.bodyLg),
                  const SizedBox(height: SrSpacing.md),
                  Text(
                    '안녕하세요! 주니어 앱에 오신 것을 환영해요.',
                    style: TextStyle(
                      fontSize: 20 * currentScale.factor,
                      fontWeight: FontWeight.w600,
                      color: SrColors.textPrimary,
                      height: 1.5,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FontScaleOption extends StatelessWidget {
  const _FontScaleOption({
    required this.scale,
    required this.isSelected,
    required this.onTap,
  });

  final AppFontScale scale;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(
          horizontal: SrSpacing.lg,
          vertical: SrSpacing.md,
        ),
        decoration: BoxDecoration(
          color: isSelected
              ? const Color(0xFFFFF7ED)
              : SrColors.bgSurface,
          borderRadius: SrRadius.mdAll,
          border: Border.all(
            color: isSelected ? SrColors.brandPrimary : SrColors.textDisabled,
            width: isSelected ? 2.5 : 1,
          ),
        ),
        child: Row(
          children: [
            // 큰 라디오 버튼 (시니어 접근성)
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: isSelected
                      ? SrColors.brandPrimary
                      : SrColors.textDisabled,
                  width: 2.5,
                ),
                color: isSelected ? SrColors.brandPrimary : SrColors.bgPrimary,
              ),
              child: isSelected
                  ? const Icon(Icons.check, color: SrColors.brandOn, size: 20)
                  : null,
            ),
            const SizedBox(width: SrSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SrText(
                    scale.label,
                    variant: SrTextVariant.bodyLg,
                    color: isSelected
                        ? SrColors.brandPrimary
                        : SrColors.textPrimary,
                  ),
                  Text(
                    '가나다라마바사',
                    style: TextStyle(
                      fontSize: 16 * scale.factor,
                      color: SrColors.textSecondary,
                      height: 1.4,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
