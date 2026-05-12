import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_input.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/http/point_api.dart';

const _presetAmounts = [10000, 30000, 50000, 100000];

class ChargeScreen extends ConsumerStatefulWidget {
  const ChargeScreen({super.key});

  @override
  ConsumerState<ChargeScreen> createState() => _ChargeScreenState();
}

class _ChargeScreenState extends ConsumerState<ChargeScreen> {
  int? _selectedPreset;
  final _customCtrl = TextEditingController();
  bool _isLoading = false;
  String? _errorText;

  @override
  void dispose() {
    _customCtrl.dispose();
    super.dispose();
  }

  int get _amount {
    if (_selectedPreset != null) return _selectedPreset!;
    return int.tryParse(_customCtrl.text.replaceAll(',', '')) ?? 0;
  }

  Future<void> _charge() async {
    if (_amount <= 0) {
      setState(() => _errorText = '충전 금액을 선택하거나 입력해 주세요.');
      return;
    }
    setState(() { _isLoading = true; _errorText = null; });
    try {
      final newBalance = await ref.read(pointApiProvider).mockTopup(amount: _amount);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('충전 완료! 현재 잔액: ${newBalance}P'),
          backgroundColor: SrColors.semanticSuccess,
        ),
      );
      context.go('/me');
    } on Exception {
      setState(() => _errorText = '충전 중 문제가 생겼어요. 다시 시도해 주세요.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SrScaffold(
      appBar: const SrAppBar(title: '포인트 충전', isModal: true),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 임시 결제 안내 카드
            SrCard(
              color: const Color(0xFFFFF7ED),
              child: Row(
                children: const [
                  Icon(Icons.info_outline, color: SrColors.brandPrimary, size: 28),
                  SizedBox(width: SrSpacing.xs),
                  Expanded(
                    child: SrText(
                      '(임시) 실 결제는 추후 연동 예정이에요.\n현재는 테스트용 포인트가 즉시 지급됩니다.',
                      variant: SrTextVariant.bodyMd,
                      color: SrColors.brandPrimary,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: SrSpacing.xl),
            const SrText('충전 금액 선택', variant: SrTextVariant.title),
            const SizedBox(height: SrSpacing.md),
            // 프리셋 칩
            Wrap(
              spacing: SrSpacing.xs,
              runSpacing: SrSpacing.xs,
              children: _presetAmounts.map((amount) {
                final sel = _selectedPreset == amount;
                return GestureDetector(
                  onTap: () {
                    setState(() {
                      _selectedPreset = sel ? null : amount;
                      if (!sel) _customCtrl.clear();
                    });
                  },
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: SrSpacing.lg, vertical: SrSpacing.md),
                    decoration: BoxDecoration(
                      color: sel ? SrColors.brandPrimary : SrColors.bgSurface,
                      borderRadius: SrRadius.mdAll,
                      border: Border.all(color: sel ? SrColors.brandPrimary : const Color(0xFFE5E5E5), width: sel ? 2 : 1),
                    ),
                    child: SrText(
                      '${_fmtMoney(amount)}원',
                      variant: SrTextVariant.bodyLg,
                      color: sel ? SrColors.brandOn : SrColors.textPrimary,
                    ),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: SrSpacing.xl),
            const SrText('직접 입력', variant: SrTextVariant.bodyLg),
            const SizedBox(height: SrSpacing.xs),
            SrInput(
              label: '금액 (원)',
              hint: '예: 20000',
              controller: _customCtrl,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              onChanged: (_) => setState(() => _selectedPreset = null),
              textInputAction: TextInputAction.done,
            ),
            const SizedBox(height: SrSpacing.xl),
            // 선택 금액 표시
            if (_amount > 0)
              SrCard(
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const SrText('충전 예정 금액: ', variant: SrTextVariant.bodyLg),
                    SrText('${_fmtMoney(_amount)}P', variant: SrTextVariant.title, color: SrColors.brandPrimary),
                  ],
                ),
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
            SrButton(
              label: '충전하기',
              onPressed: _charge,
              isLoading: _isLoading,
              size: SrButtonSize.large,
            ),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }

  String _fmtMoney(int n) {
    final s = n.toString();
    final buf = StringBuffer();
    for (var i = 0; i < s.length; i++) {
      if (i > 0 && (s.length - i) % 3 == 0) buf.write(',');
      buf.write(s[i]);
    }
    return buf.toString();
  }
}
