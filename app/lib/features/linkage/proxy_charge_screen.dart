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
import '../../core/http/linkage_api.dart';
import '../../core/http/point_api.dart';
import '../linkage/linkage_model.dart';

final _proxyLinkagesProvider = FutureProvider<List<LinkageItem>>(
  (ref) => ref.read(linkageApiProvider).myLinkages(),
);

const _proxyPresetAmounts = [10000, 30000, 50000, 100000];

class ProxyChargeScreen extends ConsumerStatefulWidget {
  const ProxyChargeScreen({super.key});

  @override
  ConsumerState<ProxyChargeScreen> createState() => _ProxyChargeScreenState();
}

class _ProxyChargeScreenState extends ConsumerState<ProxyChargeScreen> {
  LinkageItem? _selectedTuntun;
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
    if (_selectedTuntun == null) {
      setState(() => _errorText = '충전할 튼튼이를 선택해 주세요.');
      return;
    }
    if (_amount <= 0) {
      setState(() => _errorText = '충전 금액을 선택하거나 입력해 주세요.');
      return;
    }
    setState(() { _isLoading = true; _errorText = null; });
    try {
      final newBalance = await ref.read(pointApiProvider).mockTopupProxy(
        tuntunId: _selectedTuntun!.tuntunId,
        amount: _amount,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('${_selectedTuntun!.tuntunName} 님께 충전 완료! 잔액: ${newBalance}P'),
          backgroundColor: SrColors.semanticSuccess,
        ),
      );
      context.go('/linkage');
    } on Exception {
      setState(() => _errorText = '충전 중 문제가 생겼어요. 다시 시도해 주세요.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final asyncLinkages = ref.watch(_proxyLinkagesProvider);

    return SrScaffold(
      appBar: const SrAppBar(title: '대리 충전', isModal: true),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SrCard(
              color: const Color(0xFFFFF7ED),
              child: Row(
                children: const [
                  Icon(Icons.info_outline, color: SrColors.brandPrimary, size: 28),
                  SizedBox(width: SrSpacing.xs),
                  Expanded(
                    child: SrText(
                      '(임시) 실 결제는 추후 연동 예정이에요.',
                      variant: SrTextVariant.bodyMd,
                      color: SrColors.brandPrimary,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: SrSpacing.xl),
            const SrText('튼튼이 선택', variant: SrTextVariant.title),
            const SizedBox(height: SrSpacing.md),
            asyncLinkages.when(
              loading: () => const Center(child: CircularProgressIndicator(color: SrColors.brandPrimary)),
              error: (_, __) => const SrText('연동 목록을 불러오지 못했어요.', variant: SrTextVariant.bodyMd, color: SrColors.semanticDanger),
              data: (linkages) => Column(
                children: linkages.map((item) {
                  final sel = _selectedTuntun?.id == item.id;
                  return Padding(
                    padding: const EdgeInsets.only(bottom: SrSpacing.xs),
                    child: GestureDetector(
                      onTap: () => setState(() => _selectedTuntun = item),
                      child: Container(
                        padding: const EdgeInsets.all(SrSpacing.md),
                        decoration: BoxDecoration(
                          color: sel ? const Color(0xFFFFF7ED) : SrColors.bgPrimary,
                          borderRadius: SrRadius.lgAll,
                          border: Border.all(color: sel ? SrColors.brandPrimary : const Color(0xFFE5E5E5), width: sel ? 2 : 1),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.account_circle, size: 40, color: SrColors.textSecondary),
                            const SizedBox(width: SrSpacing.md),
                            SrText(item.tuntunName, variant: SrTextVariant.bodyLg),
                            const Spacer(),
                            if (sel) const Icon(Icons.check_circle, color: SrColors.brandPrimary, size: 28),
                          ],
                        ),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
            const SizedBox(height: SrSpacing.xl),
            const SrText('충전 금액 선택', variant: SrTextVariant.title),
            const SizedBox(height: SrSpacing.md),
            Wrap(
              spacing: SrSpacing.xs,
              runSpacing: SrSpacing.xs,
              children: _proxyPresetAmounts.map((amount) {
                final sel = _selectedPreset == amount;
                return GestureDetector(
                  onTap: () => setState(() {
                    _selectedPreset = sel ? null : amount;
                    if (!sel) _customCtrl.clear();
                  }),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: SrSpacing.lg, vertical: SrSpacing.md),
                    decoration: BoxDecoration(
                      color: sel ? SrColors.brandPrimary : SrColors.bgSurface,
                      borderRadius: SrRadius.mdAll,
                      border: Border.all(color: sel ? SrColors.brandPrimary : const Color(0xFFE5E5E5), width: sel ? 2 : 1),
                    ),
                    child: SrText(
                      '${amount ~/ 10000}만원',
                      variant: SrTextVariant.bodyLg,
                      color: sel ? SrColors.brandOn : SrColors.textPrimary,
                    ),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: SrSpacing.md),
            SrInput(
              label: '직접 입력 (원)',
              hint: '예: 20000',
              controller: _customCtrl,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              onChanged: (_) => setState(() => _selectedPreset = null),
              textInputAction: TextInputAction.done,
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
            SrButton(label: '대리 충전하기', onPressed: _charge, isLoading: _isLoading, size: SrButtonSize.large),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }
}
