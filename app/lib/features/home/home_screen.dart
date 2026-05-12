import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_bottom_nav.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/auth/auth_provider.dart';
import '../../core/auth/auth_state.dart';
import '../../core/tts/tts_controller.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  int _navIndex = 0;

  @override
  void initState() {
    super.initState();
    // 화면 진입 시 자동 발화
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final name = ref.read(authProvider).name;
      final greeting = name != null ? '$name 님, 반가워요!' : '주니어에 오신 것을 환영해요!';
      ref.read(ttsControllerProvider.notifier).speak(greeting);
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

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authProvider);
    final name = auth.name ?? '회원';
    final isDundun = auth.role == UserRole.dundun;

    return SrScaffold(
      appBar: SrAppBar(
        title: '주니어',
        automaticallyImplyLeading: false,
        onSpeak: () {
          ref.read(ttsControllerProvider.notifier).speak('$name 님, 반가워요! 오늘도 즐거운 하루 보내세요.');
        },
      ),
      bottomNavigationBar: SrBottomNav(
        currentIndex: _navIndex,
        onTap: _onNavTap,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SrCard(
              color: const Color(0xFFFFF7ED),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.waving_hand, color: SrColors.brandPrimary, size: 32),
                      const SizedBox(width: SrSpacing.xs),
                      Expanded(
                        child: SrText(
                          '$name 님, 반가워요!',
                          variant: SrTextVariant.title,
                          color: SrColors.brandPrimary,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: SrSpacing.xs),
                  const SrText(
                    '오늘도 즐거운 하루 보내세요.',
                    variant: SrTextVariant.bodyMd,
                    color: SrColors.textSecondary,
                  ),
                ],
              ),
            ),
            const SizedBox(height: SrSpacing.xl),
            const SrText('빠른 메뉴', variant: SrTextVariant.title),
            const SizedBox(height: SrSpacing.md),
            // 빠른 액션 4개 — 2x2 그리드
            Row(
              children: [
                Expanded(
                  child: _QuickActionCard(
                    icon: Icons.event,
                    label: '모임 보기',
                    onTap: () => context.go('/events'),
                  ),
                ),
                const SizedBox(width: SrSpacing.md),
                Expanded(
                  child: _QuickActionCard(
                    icon: Icons.notifications,
                    label: '알림',
                    onTap: () => context.go('/notifications'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: SrSpacing.md),
            Row(
              children: [
                Expanded(
                  child: _QuickActionCard(
                    icon: Icons.account_balance_wallet,
                    label: '내 잔액',
                    onTap: () => context.go('/me'),
                  ),
                ),
                const SizedBox(width: SrSpacing.md),
                Expanded(
                  child: _QuickActionCard(
                    icon: isDundun ? Icons.link : Icons.person,
                    label: isDundun ? '연동 관리' : '내 정보',
                    onTap: () => isDundun ? context.go('/linkage') : context.go('/me'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }
}

class _QuickActionCard extends StatelessWidget {
  const _QuickActionCard({
    required this.icon,
    required this.label,
    required this.onTap,
  });
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SrCard(
      onTap: onTap,
      padding: const EdgeInsets.symmetric(vertical: SrSpacing.lg, horizontal: SrSpacing.md),
      child: Column(
        children: [
          Icon(icon, size: 40, color: SrColors.brandPrimary),
          const SizedBox(height: SrSpacing.xs),
          SrText(label, variant: SrTextVariant.bodyMd, textAlign: TextAlign.center),
        ],
      ),
    );
  }
}
