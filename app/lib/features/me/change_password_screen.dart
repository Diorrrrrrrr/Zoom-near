import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_input.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/http/me_api.dart';

class ChangePasswordScreen extends ConsumerStatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  ConsumerState<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends ConsumerState<ChangePasswordScreen> {
  final _oldPwCtrl = TextEditingController();
  final _newPwCtrl = TextEditingController();
  final _confirmCtrl = TextEditingController();
  bool _isLoading = false;
  String? _errorText;

  @override
  void dispose() {
    _oldPwCtrl.dispose();
    _newPwCtrl.dispose();
    _confirmCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final old = _oldPwCtrl.text;
    final newPw = _newPwCtrl.text;
    final confirm = _confirmCtrl.text;

    if (old.isEmpty || newPw.isEmpty || confirm.isEmpty) {
      setState(() => _errorText = '모든 항목을 입력해 주세요.');
      return;
    }
    if (newPw != confirm) {
      setState(() => _errorText = '새 비밀번호가 서로 맞지 않아요. 다시 확인해 주세요.');
      return;
    }
    if (newPw.length < 6) {
      setState(() => _errorText = '비밀번호는 6자 이상이어야 해요.');
      return;
    }

    setState(() { _isLoading = true; _errorText = null; });
    try {
      await ref.read(meApiProvider).changePassword(oldPassword: old, newPassword: newPw);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('비밀번호가 변경됐어요!'), backgroundColor: SrColors.semanticSuccess),
      );
      context.go('/me');
    } on Exception {
      setState(() => _errorText = '비밀번호 변경에 실패했어요. 현재 비밀번호를 확인해 주세요.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SrScaffold(
      appBar: const SrAppBar(title: '비밀번호 변경', isModal: true),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: SrSpacing.lg),
            SrInput(
              label: '현재 비밀번호',
              hint: '현재 비밀번호를 입력하세요',
              controller: _oldPwCtrl,
              obscureText: true,
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: SrSpacing.lg),
            SrInput(
              label: '새 비밀번호 (6자 이상)',
              hint: '새 비밀번호를 입력하세요',
              controller: _newPwCtrl,
              obscureText: true,
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: SrSpacing.lg),
            SrInput(
              label: '새 비밀번호 확인',
              hint: '새 비밀번호를 한 번 더 입력하세요',
              controller: _confirmCtrl,
              obscureText: true,
              textInputAction: TextInputAction.done,
              onSubmitted: (_) => _submit(),
            ),
            if (_errorText != null) ...[
              const SizedBox(height: SrSpacing.md),
              Row(
                children: [
                  const Icon(Icons.error_outline, color: SrColors.semanticDanger, size: 22),
                  const SizedBox(width: SrSpacing.xs),
                  Expanded(child: SrText(_errorText!, variant: SrTextVariant.bodyMd, color: SrColors.semanticDanger)),
                ],
              ),
            ],
            const SizedBox(height: SrSpacing.xl),
            SrButton(label: '비밀번호 변경', onPressed: _submit, isLoading: _isLoading, size: SrButtonSize.large),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }
}
