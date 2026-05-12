import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_input.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/auth/auth_provider.dart';
import '../../core/http/auth_api.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _idController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _isLoading = false;
  bool _autoLogin = true;
  String? _errorText;

  @override
  void dispose() {
    _idController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _onLogin() async {
    final loginId = _idController.text.trim();
    final password = _passwordController.text;

    if (loginId.isEmpty || password.isEmpty) {
      setState(() => _errorText = '아이디와 비밀번호를 모두 입력해 주세요.');
      return;
    }

    setState(() {
      _isLoading = true;
      _errorText = null;
    });

    try {
      final api = ref.read(authApiProvider);
      final response = await api.login(loginId: loginId, password: password);
      await ref.read(authProvider.notifier).login(response);
      if (mounted) context.go('/home');
    } on Exception {
      setState(() => _errorText = '아이디 또는 비밀번호가 맞지 않아요. 다시 확인해 주세요.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SrScaffold(
      appBar: const SrAppBar(title: '로그인', automaticallyImplyLeading: false),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: SrSpacing.xl),
            const SrText(
              '주니어',
              variant: SrTextVariant.display,
              color: SrColors.brandPrimary,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: SrSpacing.xs),
            const SrText(
              '우리 동네 즐거운 만남',
              variant: SrTextVariant.bodyLg,
              color: SrColors.textSecondary,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: SrSpacing.xxl),
            SrInput(
              label: '아이디',
              hint: '아이디를 입력하세요',
              controller: _idController,
              keyboardType: TextInputType.text,
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: SrSpacing.lg),
            SrInput(
              label: '비밀번호',
              hint: '비밀번호를 입력하세요',
              controller: _passwordController,
              obscureText: true,
              textInputAction: TextInputAction.done,
              onSubmitted: (_) => _onLogin(),
              errorText: _errorText,
            ),
            const SizedBox(height: SrSpacing.md),
            // 자동 로그인 스위치
            Row(
              children: [
                Switch(
                  value: _autoLogin,
                  onChanged: (v) => setState(() => _autoLogin = v),
                  activeColor: SrColors.brandPrimary,
                ),
                const SizedBox(width: SrSpacing.xs),
                const SrText('자동 로그인', variant: SrTextVariant.bodyMd),
              ],
            ),
            if (_errorText != null) ...[
              const SizedBox(height: SrSpacing.xs),
              Row(
                children: [
                  const Icon(Icons.error_outline, color: SrColors.semanticDanger, size: 22),
                  const SizedBox(width: SrSpacing.xs),
                  Expanded(
                    child: SrText(
                      _errorText!,
                      variant: SrTextVariant.bodyMd,
                      color: SrColors.semanticDanger,
                    ),
                  ),
                ],
              ),
            ],
            const SizedBox(height: SrSpacing.xl),
            SrButton(
              label: '로그인',
              onPressed: _onLogin,
              isLoading: _isLoading,
              size: SrButtonSize.large,
            ),
            const SizedBox(height: SrSpacing.md),
            SrButton(
              label: '회원가입 하기',
              onPressed: () => context.go('/auth/signup'),
              variant: SrButtonVariant.secondary,
              size: SrButtonSize.large,
            ),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }
}
