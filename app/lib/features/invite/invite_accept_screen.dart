import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/http/invite_api.dart';
import '../../core/tts/tts_controller.dart';

final _invitePreviewProvider = FutureProvider.family<InvitePreview, String>(
  (ref, token) => ref.read(inviteApiProvider).getInvite(token),
);

class InviteAcceptScreen extends ConsumerStatefulWidget {
  const InviteAcceptScreen({super.key, required this.token});
  final String token;

  @override
  ConsumerState<InviteAcceptScreen> createState() => _InviteAcceptScreenState();
}

class _InviteAcceptScreenState extends ConsumerState<InviteAcceptScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(ttsControllerProvider.notifier).speak('초대 링크 화면입니다. 초대한 분의 정보를 확인하세요.');
    });
  }

  @override
  Widget build(BuildContext context) {
    final asyncPreview = ref.watch(_invitePreviewProvider(widget.token));

    return SrScaffold(
      appBar: const SrAppBar(title: '초대 받기', automaticallyImplyLeading: false),
      body: asyncPreview.when(
        loading: () => const Center(child: CircularProgressIndicator(color: SrColors.brandPrimary)),
        error: (_, __) => Center(
          child: Padding(
            padding: const EdgeInsets.all(SrSpacing.lg),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.link_off, size: 60, color: SrColors.semanticDanger),
                const SizedBox(height: SrSpacing.md),
                const SrText('초대 링크가 유효하지 않아요.', variant: SrTextVariant.bodyLg, color: SrColors.semanticDanger),
                const SizedBox(height: SrSpacing.lg),
                SrButton(
                  label: '회원가입 하기',
                  onPressed: () => context.go('/auth/signup'),
                  size: SrButtonSize.large,
                ),
              ],
            ),
          ),
        ),
        data: (preview) => preview.valid
            ? Padding(
                padding: const EdgeInsets.all(SrSpacing.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const SizedBox(height: SrSpacing.xl),
                    const Icon(Icons.celebration, size: 64, color: SrColors.brandPrimary),
                    const SizedBox(height: SrSpacing.lg),
                    SrText(
                      '${preview.inviterName} 님이 초대했어요!',
                      variant: SrTextVariant.title,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: SrSpacing.lg),
                    SrCard(
                      child: Column(
                        children: [
                          Row(
                            children: [
                              const Icon(Icons.person, size: 28, color: SrColors.brandPrimary),
                              const SizedBox(width: SrSpacing.xs),
                              const SrText('초대한 분', variant: SrTextVariant.bodyMd, color: SrColors.textSecondary),
                              const Spacer(),
                              SrText(preview.inviterName, variant: SrTextVariant.bodyLg),
                            ],
                          ),
                          const Divider(height: SrSpacing.lg),
                          Row(
                            children: [
                              const Icon(Icons.access_time, size: 28, color: SrColors.semanticWarning),
                              const SizedBox(width: SrSpacing.xs),
                              const SrText('만료 시간', variant: SrTextVariant.bodyMd, color: SrColors.textSecondary),
                              const Spacer(),
                              SrText(
                                _fmtDate(preview.expiresAt),
                                variant: SrTextVariant.bodyMd,
                                color: SrColors.semanticWarning,
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const Spacer(),
                    SrButton(
                      label: '회원가입 하기',
                      onPressed: () => context.go('/auth/signup?inviteToken=${widget.token}'),
                      size: SrButtonSize.large,
                    ),
                    const SizedBox(height: SrSpacing.xxl),
                  ],
                ),
              )
            : Padding(
                padding: const EdgeInsets.all(SrSpacing.lg),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.timer_off, size: 60, color: SrColors.semanticWarning),
                    const SizedBox(height: SrSpacing.md),
                    const SrText('초대 링크가 만료됐어요.', variant: SrTextVariant.bodyLg, color: SrColors.semanticWarning),
                    const SizedBox(height: SrSpacing.lg),
                    SrButton(
                      label: '회원가입 하기',
                      onPressed: () => context.go('/auth/signup'),
                      size: SrButtonSize.large,
                    ),
                  ],
                ),
              ),
      ),
    );
  }

  String _fmtDate(String iso) {
    if (iso.length < 16) return iso;
    return iso.substring(0, 16).replaceFirst('T', ' ');
  }
}
