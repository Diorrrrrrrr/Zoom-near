import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_input.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/auth/auth_provider.dart';
import '../../core/http/auth_api.dart';

/// 3 step wizard 회원가입 화면
class SignupScreen extends ConsumerStatefulWidget {
  const SignupScreen({super.key, this.inviteToken});

  final String? inviteToken;

  @override
  ConsumerState<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends ConsumerState<SignupScreen> {
  int _step = 0; // 0: 역할/약관, 1: 아이디·비밀번호, 2: 개인정보

  // Step 0
  String _role = 'TUNTUN';
  bool _agreePrivacy = false;
  bool _agreeTerms = false;

  // Step 1
  final _idCtrl = TextEditingController();
  final _pwCtrl = TextEditingController();
  final _pwConfirmCtrl = TextEditingController();

  // Step 2
  final _nameCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _birthCtrl = TextEditingController();

  bool _isLoading = false;
  String? _errorText;

  @override
  void initState() {
    super.initState();
    if (widget.inviteToken != null) {
      _role = 'TUNTUN';
    }
  }

  @override
  void dispose() {
    _idCtrl.dispose();
    _pwCtrl.dispose();
    _pwConfirmCtrl.dispose();
    _nameCtrl.dispose();
    _phoneCtrl.dispose();
    _emailCtrl.dispose();
    _birthCtrl.dispose();
    super.dispose();
  }

  bool get _step0Valid => _agreePrivacy && _agreeTerms;

  bool get _step1Valid {
    final id = _idCtrl.text.trim();
    final pw = _pwCtrl.text;
    final pwc = _pwConfirmCtrl.text;
    return id.length >= 4 && pw.length >= 6 && pw == pwc;
  }

  bool get _step2Valid {
    final name = _nameCtrl.text.trim();
    final phone = _phoneCtrl.text.trim();
    return name.isNotEmpty && phone.length >= 10;
  }

  void _next() {
    setState(() => _errorText = null);
    if (_step == 0 && !_step0Valid) {
      setState(() => _errorText = '역할을 선택하고 약관에 동의해 주세요.');
      return;
    }
    if (_step == 1 && !_step1Valid) {
      if (_pwCtrl.text != _pwConfirmCtrl.text) {
        setState(() => _errorText = '비밀번호가 서로 맞지 않아요. 다시 확인해 주세요.');
      } else {
        setState(() => _errorText = '아이디(4자 이상)와 비밀번호(6자 이상)를 올바르게 입력해 주세요.');
      }
      return;
    }
    if (_step < 2) setState(() => _step++);
  }

  Future<void> _submit() async {
    if (!_step2Valid) {
      setState(() => _errorText = '이름과 전화번호를 올바르게 입력해 주세요.');
      return;
    }
    setState(() {
      _isLoading = true;
      _errorText = null;
    });
    try {
      final api = ref.read(authApiProvider);
      final response = await api.signup(
        loginId: _idCtrl.text.trim(),
        password: _pwCtrl.text,
        phone: _phoneCtrl.text.trim(),
        name: _nameCtrl.text.trim(),
        role: _role,
        email: _emailCtrl.text.trim().isEmpty ? null : _emailCtrl.text.trim(),
        inviteToken: widget.inviteToken,
        birthDate: _birthCtrl.text.trim().isEmpty ? null : _birthCtrl.text.trim(),
      );
      await ref.read(authProvider.notifier).login(response);
      if (mounted) context.go('/home');
    } on Exception {
      setState(() => _errorText = '회원가입 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SrScaffold(
      appBar: SrAppBar(
        title: '회원가입 (${_step + 1}/3)',
        automaticallyImplyLeading: _step == 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 단계 표시 바
            _StepIndicator(current: _step),
            const SizedBox(height: SrSpacing.xl),

            if (widget.inviteToken != null)
              Padding(
                padding: const EdgeInsets.only(bottom: SrSpacing.lg),
                child: SrCard(
                  color: const Color(0xFFFFF7ED),
                  child: Row(
                    children: [
                      const Icon(Icons.link, color: SrColors.brandPrimary, size: 28),
                      const SizedBox(width: SrSpacing.xs),
                      Expanded(
                        child: SrText(
                          '초대 링크로 가입하고 있어요',
                          variant: SrTextVariant.bodyMd,
                          color: SrColors.brandPrimary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),

            if (_step == 0) _buildStep0(),
            if (_step == 1) _buildStep1(),
            if (_step == 2) _buildStep2(),

            if (_errorText != null) ...[
              const SizedBox(height: SrSpacing.md),
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

            if (_step < 2)
              SrButton(
                label: '다음',
                onPressed: _next,
                size: SrButtonSize.large,
              )
            else
              SrButton(
                label: '가입 완료',
                onPressed: _submit,
                isLoading: _isLoading,
                size: SrButtonSize.large,
              ),

            if (_step > 0) ...[
              const SizedBox(height: SrSpacing.md),
              SrButton(
                label: '이전',
                onPressed: () => setState(() {
                  _step--;
                  _errorText = null;
                }),
                variant: SrButtonVariant.secondary,
                size: SrButtonSize.large,
              ),
            ],
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }

  Widget _buildStep0() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SrText('역할을 선택해 주세요', variant: SrTextVariant.title),
        const SizedBox(height: SrSpacing.lg),
        _RoleCard(
          title: '튼튼이',
          subtitle: '모임에 참여하는 회원',
          icon: Icons.directions_walk,
          selected: _role == 'TUNTUN',
          onTap: () => setState(() => _role = 'TUNTUN'),
        ),
        const SizedBox(height: SrSpacing.md),
        _RoleCard(
          title: '든든이',
          subtitle: '가족·보호자 계정',
          icon: Icons.favorite,
          selected: _role == 'DUNDUN',
          onTap: () => setState(() => _role = 'DUNDUN'),
        ),
        const SizedBox(height: SrSpacing.xl),
        const SrText('약관 동의', variant: SrTextVariant.title),
        const SizedBox(height: SrSpacing.md),
        _AgreementRow(
          label: '[필수] 개인정보 처리방침에 동의합니다',
          value: _agreePrivacy,
          onChanged: (v) => setState(() => _agreePrivacy = v),
        ),
        const SizedBox(height: SrSpacing.sm),
        _AgreementRow(
          label: '[필수] 서비스 이용약관에 동의합니다',
          value: _agreeTerms,
          onChanged: (v) => setState(() => _agreeTerms = v),
        ),
      ],
    );
  }

  Widget _buildStep1() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SrText('아이디와 비밀번호', variant: SrTextVariant.title),
        const SizedBox(height: SrSpacing.lg),
        SrInput(
          label: '아이디 (4자 이상)',
          hint: '사용할 아이디를 입력하세요',
          controller: _idCtrl,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: SrSpacing.lg),
        SrInput(
          label: '비밀번호 (6자 이상)',
          hint: '비밀번호를 입력하세요',
          controller: _pwCtrl,
          obscureText: true,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: SrSpacing.lg),
        SrInput(
          label: '비밀번호 확인',
          hint: '비밀번호를 한 번 더 입력하세요',
          controller: _pwConfirmCtrl,
          obscureText: true,
          textInputAction: TextInputAction.done,
        ),
      ],
    );
  }

  Widget _buildStep2() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SrText('개인정보 입력', variant: SrTextVariant.title),
        const SizedBox(height: SrSpacing.lg),
        SrInput(
          label: '이름',
          hint: '실명을 입력하세요',
          controller: _nameCtrl,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: SrSpacing.lg),
        SrInput(
          label: '전화번호',
          hint: '01012345678',
          controller: _phoneCtrl,
          keyboardType: TextInputType.phone,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: SrSpacing.lg),
        SrInput(
          label: '이메일 (선택)',
          hint: 'example@email.com',
          controller: _emailCtrl,
          keyboardType: TextInputType.emailAddress,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: SrSpacing.lg),
        SrInput(
          label: '생년월일 (선택, YYYY-MM-DD)',
          hint: '1960-01-01',
          controller: _birthCtrl,
          keyboardType: TextInputType.datetime,
          textInputAction: TextInputAction.done,
        ),
      ],
    );
  }
}

class _StepIndicator extends StatelessWidget {
  const _StepIndicator({required this.current});
  final int current;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: List.generate(3, (i) {
        final active = i <= current;
        return Expanded(
          child: Container(
            height: 6,
            margin: EdgeInsets.only(right: i < 2 ? 6 : 0),
            decoration: BoxDecoration(
              color: active ? SrColors.brandPrimary : const Color(0xFFE5E5E5),
              borderRadius: SrRadius.smAll,
            ),
          ),
        );
      }),
    );
  }
}

class _RoleCard extends StatelessWidget {
  const _RoleCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.selected,
    required this.onTap,
  });
  final String title;
  final String subtitle;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(SrSpacing.lg),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFFFF7ED) : SrColors.bgPrimary,
          borderRadius: SrRadius.lgAll,
          border: Border.all(
            color: selected ? SrColors.brandPrimary : const Color(0xFFE5E5E5),
            width: selected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            Icon(icon, size: 40, color: selected ? SrColors.brandPrimary : SrColors.textDisabled),
            const SizedBox(width: SrSpacing.md),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SrText(title, variant: SrTextVariant.title, color: selected ? SrColors.brandPrimary : SrColors.textPrimary),
                SrText(subtitle, variant: SrTextVariant.bodyMd, color: SrColors.textSecondary),
              ],
            ),
            const Spacer(),
            if (selected)
              const Icon(Icons.check_circle, color: SrColors.brandPrimary, size: 32),
          ],
        ),
      ),
    );
  }
}

class _AgreementRow extends StatelessWidget {
  const _AgreementRow({
    required this.label,
    required this.value,
    required this.onChanged,
  });
  final String label;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => onChanged(!value),
      child: Row(
        children: [
          SizedBox(
            width: 44,
            height: 44,
            child: Checkbox(
              value: value,
              onChanged: (v) => onChanged(v ?? false),
              activeColor: SrColors.brandPrimary,
            ),
          ),
          const SizedBox(width: SrSpacing.xs),
          Expanded(child: SrText(label, variant: SrTextVariant.bodyMd)),
        ],
      ),
    );
  }
}
