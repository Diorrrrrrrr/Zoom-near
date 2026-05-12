import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/design_tokens/tokens.dart';
import '../../core/widgets/sr_scaffold.dart';
import '../../core/widgets/sr_app_bar.dart';
import '../../core/widgets/sr_button.dart';
import '../../core/widgets/sr_input.dart';
import '../../core/widgets/sr_text.dart';
import '../../core/http/event_api.dart';

const _categories = ['친목', '운동', '문화', '교육', '봉사', '기타'];

class EventCreateScreen extends ConsumerStatefulWidget {
  const EventCreateScreen({super.key});

  @override
  ConsumerState<EventCreateScreen> createState() => _EventCreateScreenState();
}

class _EventCreateScreenState extends ConsumerState<EventCreateScreen> {
  final _titleCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  final _regionCtrl = TextEditingController();
  final _capacityCtrl = TextEditingController(text: '10');
  final _pointCostCtrl = TextEditingController(text: '0');
  String _category = '친목';
  DateTime? _startsAt;
  DateTime? _endsAt;
  bool _isLoading = false;
  String? _errorText;

  @override
  void dispose() {
    _titleCtrl.dispose();
    _descCtrl.dispose();
    _regionCtrl.dispose();
    _capacityCtrl.dispose();
    _pointCostCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDateTime({required bool isStart}) async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: now,
      firstDate: now,
      lastDate: now.add(const Duration(days: 365)),
    );
    if (date == null || !mounted) return;
    final time = await showTimePicker(context: context, initialTime: TimeOfDay.now());
    if (time == null) return;
    final dt = DateTime(date.year, date.month, date.day, time.hour, time.minute);
    setState(() {
      if (isStart) _startsAt = dt;
      else _endsAt = dt;
    });
  }

  Future<void> _submit() async {
    final title = _titleCtrl.text.trim();
    final desc = _descCtrl.text.trim();
    final region = _regionCtrl.text.trim();
    final capacity = int.tryParse(_capacityCtrl.text) ?? 0;
    final pointCost = int.tryParse(_pointCostCtrl.text) ?? 0;

    if (title.isEmpty || desc.isEmpty || region.isEmpty || _startsAt == null || _endsAt == null) {
      setState(() => _errorText = '모든 필수 항목을 입력해 주세요.');
      return;
    }
    if (capacity <= 0) {
      setState(() => _errorText = '정원은 1명 이상이어야 해요.');
      return;
    }

    setState(() { _isLoading = true; _errorText = null; });
    try {
      await ref.read(eventApiProvider).createEvent(
        title: title,
        description: desc,
        regionText: region,
        category: _category,
        startsAt: _startsAt!.toIso8601String(),
        endsAt: _endsAt!.toIso8601String(),
        capacity: capacity,
        pointCost: pointCost,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('모임이 만들어졌어요!'), backgroundColor: SrColors.semanticSuccess),
        );
        context.go('/events');
      }
    } on Exception {
      setState(() => _errorText = '모임 만들기에 실패했어요. 다시 시도해 주세요.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SrScaffold(
      appBar: const SrAppBar(title: '모임 만들기', isModal: true),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(SrSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SrInput(label: '모임 이름', hint: '어떤 모임인지 알려주세요', controller: _titleCtrl, textInputAction: TextInputAction.next),
            const SizedBox(height: SrSpacing.lg),
            SrInput(label: '모임 소개', hint: '모임을 소개해 주세요', controller: _descCtrl, maxLines: 4, textInputAction: TextInputAction.next),
            const SizedBox(height: SrSpacing.lg),
            SrInput(label: '장소', hint: '서울 종로구', controller: _regionCtrl, textInputAction: TextInputAction.next),
            const SizedBox(height: SrSpacing.lg),
            // 카테고리
            const SrText('카테고리', variant: SrTextVariant.bodyMd),
            const SizedBox(height: SrSpacing.xs),
            Wrap(
              spacing: SrSpacing.xs,
              runSpacing: SrSpacing.xs,
              children: _categories.map((c) {
                final sel = _category == c;
                return GestureDetector(
                  onTap: () => setState(() => _category = c),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: SrSpacing.md, vertical: SrSpacing.xs),
                    decoration: BoxDecoration(
                      color: sel ? SrColors.brandPrimary : SrColors.bgSurface,
                      borderRadius: SrRadius.smAll,
                      border: Border.all(color: sel ? SrColors.brandPrimary : const Color(0xFFE5E5E5)),
                    ),
                    child: SrText(c, variant: SrTextVariant.bodyMd, color: sel ? SrColors.brandOn : SrColors.textPrimary),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: SrSpacing.lg),
            // 시작 일시
            _DateTimePicker(
              label: '시작 일시',
              value: _startsAt,
              onTap: () => _pickDateTime(isStart: true),
            ),
            const SizedBox(height: SrSpacing.lg),
            _DateTimePicker(
              label: '종료 일시',
              value: _endsAt,
              onTap: () => _pickDateTime(isStart: false),
            ),
            const SizedBox(height: SrSpacing.lg),
            SrInput(
              label: '정원 (명)',
              hint: '10',
              controller: _capacityCtrl,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: SrSpacing.lg),
            SrInput(
              label: '참여 포인트',
              hint: '0',
              controller: _pointCostCtrl,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
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
            SrButton(label: '모임 만들기', onPressed: _submit, isLoading: _isLoading, size: SrButtonSize.large),
            const SizedBox(height: SrSpacing.xxl),
          ],
        ),
      ),
    );
  }
}

class _DateTimePicker extends StatelessWidget {
  const _DateTimePicker({required this.label, this.value, required this.onTap});
  final String label;
  final DateTime? value;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final display = value != null
        ? '${value!.year}-${_p(value!.month)}-${_p(value!.day)} ${_p(value!.hour)}:${_p(value!.minute)}'
        : '날짜와 시간을 선택하세요';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SrText(label, variant: SrTextVariant.bodyMd),
        const SizedBox(height: SrSpacing.xs),
        GestureDetector(
          onTap: onTap,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: SrSpacing.md, vertical: SrSpacing.md),
            decoration: BoxDecoration(
              color: SrColors.bgSurface,
              borderRadius: SrRadius.mdAll,
              border: Border.all(color: SrColors.textDisabled),
            ),
            child: Row(
              children: [
                const Icon(Icons.calendar_today, size: 24, color: SrColors.textSecondary),
                const SizedBox(width: SrSpacing.xs),
                SrText(display, variant: SrTextVariant.bodyLg, color: value != null ? SrColors.textPrimary : SrColors.textDisabled),
              ],
            ),
          ),
        ),
      ],
    );
  }

  String _p(int n) => n.toString().padLeft(2, '0');
}
