import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_bottom_nav.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_confirm_dialog.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/auth/auth_provider.dart';
import '../../core/auth/auth_state.dart';
import '../../core/http/me_api.dart';
import '../../core/auth/auth_models.dart';
import '../../core/tts/tts_controller.dart';
import '../notification/notification_provider.dart';

final _meProfileProvider = FutureProvider<MeProfile>(
  (ref) => ref.read(meApiProvider).getMe(),
);

class MyPageScreen extends ConsumerStatefulWidget {
  const MyPageScreen({super.key});

  @override
  ConsumerState<MyPageScreen> createState() => _MyPageScreenState();
}

class _MyPageScreenState extends ConsumerState<MyPageScreen> {
  int _navIndex = 3;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(ttsControllerProvider.notifier).speak('내 정보 화면입니다.');
    });
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

  Future<void> _logout() async {
    final confirmed = await SrConfirmDialog.show(
      context,
      title: '로그아웃',
      message: '정말 로그아웃 하시겠어요?',
      confirmLabel: '로그아웃',
      isDangerous: true,
    );
    if (!confirmed || !mounted) return;
    await ref.read(authProvider.notifier).logout();
    if (!mounted) return;
    context.go('/auth/login');
  }

  @override
  Widget build(BuildContext context) {
    final asyncProfile = ref.watch(_meProfileProvider);
    final auth = ref.watch(authProvider);
    final isDundun = auth.role == UserRole.dundun;
    final isManager = auth.role == UserRole.manager;
    final unreadCount = ref.watch(unreadCountProvider);

    return SrScaffold(
      appBar: SrAppBar(
        title: '내 정보',
        automaticallyImplyLeading: false,
        onSpeak: () =>
            ref.read(ttsControllerProvider.notifier).speak('내 정보 화면입니다.'),
      ),
      bottomNavigationBar: SrBottomNav(
        currentIndex: _navIndex,
        onTap: _onNavTap,
        unreadCount: unreadCount,
      ),
      body: asyncProfile.when(
        loading: () => const Center(
            child: CircularProgressIndicator(color: SrColors.brandPrimary)),
        error: (_, __) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline,
                  size: 48, color: SrColors.semanticDanger),
              const SizedBox(height: SrSpacing.md),
              const SrText('정보를 불러오지 못했어요.',
                  variant: SrTextVariant.bodyMd,
                  color: SrColors.semanticDanger),
              const SizedBox(height: SrSpacing.lg),
              SrButton(
                  label: '다시 시도',
                  onPressed: () => ref.invalidate(_meProfileProvider)),
            ],
          ),
        ),
        data: (profile) => SingleChildScrollView(
          padding: const EdgeInsets.all(SrSpacing.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 카드 1: 이름·6자리코드·랭크
              SrCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.account_circle,
                            size: 48, color: SrColors.brandPrimary),
                        const SizedBox(width: SrSpacing.md),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            SrText(profile.name, variant: SrTextVariant.title),
                            SrText(
                              profile.role == 'TUNTUN'
                                  ? '튼튼이'
                                  : profile.role == 'DUNDUN'
                                      ? '든든이'
                                      : profile.role == 'MANAGER'
                                          ? '매니저'
                                          : profile.role,
                              variant: SrTextVariant.bodyMd,
                              color: SrColors.textSecondary,
                            ),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(height: SrSpacing.md),
                    Row(
                      children: [
                        const Icon(Icons.tag,
                            size: 24, color: SrColors.textSecondary),
                        const SizedBox(width: SrSpacing.xs),
                        const SrText('내 코드',
                            variant: SrTextVariant.bodyMd,
                            color: SrColors.textSecondary),
                        const Spacer(),
                        SrText(profile.uniqueCode,
                            variant: SrTextVariant.title,
                            color: SrColors.brandPrimary),
                      ],
                    ),
                    if (profile.rankDisplayName != null) ...[
                      const SizedBox(height: SrSpacing.xs),
                      Row(
                        children: [
                          const Icon(Icons.military_tech,
                              size: 24, color: SrColors.semanticWarning),
                          const SizedBox(width: SrSpacing.xs),
                          const SrText('등급',
                              variant: SrTextVariant.bodyMd,
                              color: SrColors.textSecondary),
                          const Spacer(),
                          SrText(profile.rankDisplayName!,
                              variant: SrTextVariant.bodyMd,
                              color: SrColors.semanticWarning),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: SrSpacing.md),
              // 카드 2: 잔액 + 충전하기
              SrCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.account_balance_wallet,
                            size: 32, color: SrColors.brandPrimary),
                        const SizedBox(width: SrSpacing.xs),
                        const SrText('내 잔액', variant: SrTextVariant.bodyLg),
                        const Spacer(),
                        SrText('${profile.balance}P',
                            variant: SrTextVariant.title,
                            color: SrColors.brandPrimary),
                      ],
                    ),
                    const SizedBox(height: SrSpacing.md),
                    SrButton(
                      label: isDundun ? '대리 충전하기' : '충전하기',
                      onPressed: () => context.go(
                          isDundun ? '/linkage/proxy-charge' : '/me/charge'),
                      size: SrButtonSize.large,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: SrSpacing.md),
              // 카드 3: 설정 메뉴
              SrCard(
                child: Column(
                  children: [
                    if (isDundun) ...[
                      _MenuItem(
                          icon: Icons.link,
                          label: '연동 관리',
                          onTap: () => context.go('/linkage')),
                      const Divider(),
                    ],
                    if (isManager) ...[
                      _MenuItem(
                          icon: Icons.dashboard_outlined,
                          label: '매니저 콘솔',
                          onTap: () => context.go('/manager/console')),
                      const Divider(),
                    ],
                    _MenuItem(
                        icon: Icons.notifications_outlined,
                        label: '알림함',
                        onTap: () => context.go('/notifications')),
                    const Divider(),
                    _MenuItem(
                        icon: Icons.lock_outline,
                        label: '비밀번호 변경',
                        onTap: () => context.go('/me/change-password')),
                    const Divider(),
                    _MenuItem(
                        icon: Icons.text_fields,
                        label: '글자 크기 설정',
                        onTap: () => context.go('/settings/font-size')),
                    const Divider(),
                    _MenuItem(
                        icon: Icons.help_outline,
                        label: '도움말',
                        onTap: () => context.go('/legal/help')),
                    const Divider(),
                    _MenuItem(
                        icon: Icons.description_outlined,
                        label: '이용약관',
                        onTap: () => context.go('/legal/terms')),
                    const Divider(),
                    _MenuItem(
                        icon: Icons.privacy_tip_outlined,
                        label: '개인정보처리방침',
                        onTap: () => context.go('/legal/privacy')),
                    const Divider(),
                    _MenuItem(
                      icon: Icons.logout,
                      label: '로그아웃',
                      onTap: _logout,
                      color: SrColors.semanticDanger,
                    ),
                    const Divider(),
                    _MenuItem(
                      icon: Icons.person_remove_outlined,
                      label: '회원 탈퇴',
                      onTap: () => context.go('/me/withdraw'),
                      color: SrColors.semanticDanger,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: SrSpacing.xxl),
            ],
          ),
        ),
      ),
    );
  }
}

class _MenuItem extends StatelessWidget {
  const _MenuItem(
      {required this.icon,
      required this.label,
      required this.onTap,
      this.color});
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final effectiveColor = color ?? SrColors.textPrimary;
    return InkWell(
      onTap: onTap,
      borderRadius: SrRadius.mdAll,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: SrSpacing.md),
        child: Row(
          children: [
            Icon(icon, size: 28, color: effectiveColor),
            const SizedBox(width: SrSpacing.md),
            SrText(label,
                variant: SrTextVariant.bodyMd, color: effectiveColor),
            const Spacer(),
            Icon(Icons.chevron_right,
                size: 28, color: color ?? SrColors.textDisabled),
          ],
        ),
      ),
    );
  }
}
