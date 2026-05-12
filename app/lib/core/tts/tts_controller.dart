import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _kTtsEnabled = 'tts_enabled';
const _kTtsRate = 'tts_rate';

/// TTS 상태
class TtsState {
  const TtsState({
    this.isSpeaking = false,
    this.isEnabled = true,
    this.rate = 0.5,
  });

  final bool isSpeaking;
  final bool isEnabled;
  final double rate;

  TtsState copyWith({bool? isSpeaking, bool? isEnabled, double? rate}) =>
      TtsState(
        isSpeaking: isSpeaking ?? this.isSpeaking,
        isEnabled: isEnabled ?? this.isEnabled,
        rate: rate ?? this.rate,
      );

  /// 화면 코드 호환 — state 위에서 직접 speak/stop 호출되는 경우 no-op
  Future<void> speak(String _, {String? lang}) async {}
  Future<void> stop() async {}
}

/// flutter_tts 래퍼 — Riverpod Notifier
/// speak / stop / pause / resume / setEnabled / setRate
class TtsController extends AutoDisposeNotifier<TtsState> {
  final FlutterTts _tts = FlutterTts();

  @override
  TtsState build() {
    ref.onDispose(() {
      _tts.stop();
    });
    _init();
    return const TtsState();
  }

  Future<void> _init() async {
    await _tts.setLanguage('ko-KR');
    await _tts.setVolume(1.0);
    await _tts.setPitch(1.0);

    // SharedPreferences에서 저장값 복원
    final prefs = await SharedPreferences.getInstance();
    final enabled = prefs.getBool(_kTtsEnabled) ?? true;
    final rate = prefs.getDouble(_kTtsRate) ?? 0.5;
    await _tts.setSpeechRate(rate);

    _tts.setCompletionHandler(() {
      state = state.copyWith(isSpeaking: false);
    });
    _tts.setErrorHandler((_) {
      state = state.copyWith(isSpeaking: false);
    });
    _tts.setCancelHandler(() {
      state = state.copyWith(isSpeaking: false);
    });

    {
      state = state.copyWith(isEnabled: enabled, rate: rate);
    }
  }

  /// 텍스트 발화. isEnabled=false 또는 text 비어있으면 무시.
  Future<void> speak(String text, {String lang = 'ko-KR'}) async {
    if (!state.isEnabled || text.isEmpty) return;
    await _tts.setLanguage(lang);
    await _tts.stop();
    state = state.copyWith(isSpeaking: true);
    await _tts.speak(text);
  }

  /// 발화 중이면 stop, 아니면 speak (토글)
  Future<void> toggle(String text) async {
    if (state.isSpeaking) {
      await stop();
    } else {
      await speak(text);
    }
  }

  Future<void> stop() async {
    await _tts.stop();
    state = state.copyWith(isSpeaking: false);
  }

  Future<void> pause() async {
    await _tts.pause();
    state = state.copyWith(isSpeaking: false);
  }

  Future<void> resume() async {
    await _tts.speak('');
    state = state.copyWith(isSpeaking: true);
  }

  /// TTS 전체 활성/비활성 + SharedPreferences 저장
  Future<void> setEnabled(bool enabled) async {
    if (!enabled) await _tts.stop();
    state = state.copyWith(isEnabled: enabled, isSpeaking: false);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_kTtsEnabled, enabled);
  }

  /// 발화 속도 설정 (0.4 ~ 0.7 권장) + 저장
  Future<void> setRate(double rate) async {
    final clamped = rate.clamp(0.1, 1.0);
    await _tts.setSpeechRate(clamped);
    state = state.copyWith(rate: clamped);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble(_kTtsRate, clamped);
  }
}

final ttsControllerProvider =
    NotifierProvider.autoDispose<TtsController, TtsState>(
  TtsController.new,
);
