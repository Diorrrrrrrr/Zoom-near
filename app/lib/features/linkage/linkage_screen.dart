import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_card.dart';
import '../../core/widgets/sr_confirm_dialog.dart';
import '../../core/widgets/sr_input.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/http/linkage_api.dart';
import '../../core/http/invite_api.dart';
import '../../core/share/invite_share.dart';
import '../../core/tts/tts_controller.dart';
import '../linkage/linkage_model.dart';

final _myLinkagesProvider = FutureProvider<List<LinkageItem>>(
  (ref) => ref.read(linkageApiProvider).myLinkages(),
);

class LinkageScreen extends ConsumerStatefulWidget {
  const LinkageScreen({super.key});

  @override
  ConsumerState<LinkageScreen> createState() => _LinkageScreenState();
}

class _LinkageScreenState extends ConsumerState<LinkageScreen> {
  final _codeCtrl = TextEditingController();
  TuntunSearchResult? _searchResult;
  bool _isSearching = false;
  bool _isLinking = false;
  bool _isCreatingInvite = false;
  String? _searchError;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(ttsControllerProvider.notifier).speak('연동 관리 화면입니다. 튼튼이를 검색하거나 초대 링크를 만드세요.');
    });
  }

  @override
  void dispose() {
    _codeCtrl.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    final code = _codeCtrl.text.trim();
    if (code.isEmpty) {
      setState(() => _searchError = '6자리 코드를 입력해 주세요.');
      return;
    }
    setState(() { _isSearching = true; _searchError = null; _searchResult = null; });
    try {
      final result = await ref.read(linkageApiProvider).searchByCode(code);
      setState(() => _searchResult = result);
    } on Exception {
      setState(() => _searchError = '해당 코드의 튼튼이를 찾지 못했어요. 코드를 다시 확인해 주세요.');
    } finally {
      if (mounted) setState(() => _isSearching = false);
    }
  }

  Future<void> _link(TuntunSearchResult tuntun) async {
    setState(() => _isLinking = true);
    try {
      await ref.read(linkageApiProvider).link(tuntunId: tuntun.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${tuntun.name} 님과 연동됐어요!'), backgroundColor: SrColors.semanticSuccess),
      );
      ref.invalidate(_myLinkagesProvider);
      setState(() { _searchResult = null; _codeCtrl.clear(); });
    } on Exception {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('연동에 실패했어요. 다시 시도해 주세요.'), backgroundColor: SrColors.semanticDanger),
      );
    } finally {
      if (mounted) setState(() => _isLinking = false);
    }
  }

  Future<void> _unlink(LinkageItem item) async {
    final confirmed = await SrConfirmDialog.show(
      context,
      title: '연동 해제',
      message: '${item.tuntunName} 님과의 연동을 해제할까요?',
      confirmLabel: '해제',
      isDangerous: true,
    );
    if (!confirmed || !mounted) return;
    try {
      await ref.read(linkageApiProvider).unlink(item.id);
      ref.invalidate(_myLinkagesProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${item.tuntunName} 님과의 연동이 해제됐어요.'), backgroundColor: SrColors.semanticSuccess),
      );
    } on Exception {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('연동 해제에 실패했어요.'), backgroundColor: SrColors.semanticDanger),
      );
    }
  }

  Future<void> _createInvite() async {
    setState(() => _isCreatingInvite = true);
    try {
      final info = await ref.read(inviteApiProvider).createInvite();
      await InviteShare.shareInviteUrl(info.url);
    } on Exception {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('초대 링크 만들기에 실패했어요.'), backgroundColor: SrColors.semanticDanger),
      );
    } finally {
      if (mounted) setState(() => _isCreatingInvite = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final asyncLinkages = ref.watch(_myLinkagesProvider);

    return SrScaffold(
      appBar: SrAppBar(
        title: '연동 관리',
        onSpeak: () => ref.read(ttsControllerProvider.notifier).speak('연동 관리 화면입니다.'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SrText('튼튼이 검색', variant: SrTextVariant.title),
            const SizedBox(height: SrSpacing.md),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: SrInput(
                    label: '6자리 코드 입력',
                    hint: 'ABC123',
                    controller: _codeCtrl,
                    textInputAction: TextInputAction.search,
                    onSubmitted: (_) => _search(),
                    errorText: _searchError,
                  ),
                ),
                const SizedBox(width: SrSpacing.xs),
                SizedBox(
                  width: 120,
                  child: SrButton(
                    label: '검색',
                    onPressed: _search,
                    isLoading: _isSearching,
                    size: SrButtonSize.medium,
                    icon: const Icon(Icons.search, color: SrColors.brandOn, size: 24),
                  ),
                ),
              ],
            ),
            if (_searchResult != null) ...[
              const SizedBox(height: SrSpacing.md),
              SrCard(
                child: Row(
                  children: [
                    const Icon(Icons.account_circle, size: 40, color: SrColors.brandPrimary),
                    const SizedBox(width: SrSpacing.md),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        SrText(_searchResult!.name, variant: SrTextVariant.bodyLg),
                        SrText(_searchResult!.uniqueCode, variant: SrTextVariant.bodyMd, color: SrColors.textSecondary),
                      ],
                    ),
                    const Spacer(),
                    SizedBox(
                      width: 100,
                      child: SrButton(
                        label: '연동',
                        onPressed: () => _link(_searchResult!),
                        isLoading: _isLinking,
                        size: SrButtonSize.medium,
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: SrSpacing.xl),
            const SrText('내 연동 목록', variant: SrTextVariant.title),
            const SizedBox(height: SrSpacing.md),
            asyncLinkages.when(
              loading: () => const Center(child: CircularProgressIndicator(color: SrColors.brandPrimary)),
              error: (_, __) => const SrText('연동 목록을 불러오지 못했어요.', variant: SrTextVariant.bodyMd, color: SrColors.semanticDanger),
              data: (linkages) => linkages.isEmpty
                  ? const SrCard(
                      child: Center(
                        child: SrText('연동된 튼튼이가 없어요.', variant: SrTextVariant.bodyMd, color: SrColors.textSecondary),
                      ),
                    )
                  : Column(
                      children: linkages.map((item) => Padding(
                        padding: const EdgeInsets.only(bottom: SrSpacing.md),
                        child: SrCard(
                          child: Row(
                            children: [
                              const Icon(Icons.account_circle, size: 40, color: SrColors.textSecondary),
                              const SizedBox(width: SrSpacing.md),
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  SrText(item.tuntunName, variant: SrTextVariant.bodyLg),
                                  if (item.isPrimary)
                                    Row(
                                      children: const [
                                        Icon(Icons.star, size: 18, color: SrColors.semanticWarning),
                                        SizedBox(width: 4),
                                        SrText('주 보호자', variant: SrTextVariant.caption, color: SrColors.semanticWarning),
                                      ],
                                    ),
                                ],
                              ),
                              const Spacer(),
                              SizedBox(
                                width: 90,
                                child: SrButton(
                                  label: '해제',
                                  onPressed: () => _unlink(item),
                                  variant: SrButtonVariant.danger,
                                  size: SrButtonSize.medium,
                                  requiresConfirm: false,
                                ),
                              ),
                            ],
                          ),
                        ),
                      )).toList(),
                    ),
            ),
            const SizedBox(height: SrSpacing.xl),
            SrButton(
              label: '초대 링크 만들기',
              onPressed: _createInvite,
              isLoading: _isCreatingInvite,
              variant: SrButtonVariant.secondary,
              size: SrButtonSize.large,
              icon: const Icon(Icons.link, color: SrColors.brandPrimary),
            ),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }
}
