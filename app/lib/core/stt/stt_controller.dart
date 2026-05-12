import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;

/// STT 상태
class SttState {
  const SttState({
    this.isListening = false,
    this.isAvailable = false,
    this.lastWords = '',
    this.error,
  });

  final bool isListening;
  final bool isAvailable;
  final String lastWords;
  final String? error;

  SttState copyWith({
    bool? isListening,
    bool? isAvailable,
    String? lastWords,
    String? error,
  }) =>
      SttState(
        isListening: isListening ?? this.isListening,
        isAvailable: isAvailable ?? this.isAvailable,
        lastWords: lastWords ?? this.lastWords,
        error: error,
      );
}

/// speech_to_text 래퍼 — 음성 검색용
class SttController extends AutoDisposeNotifier<SttState> {
  final stt.SpeechToText _speech = stt.SpeechToText();

  @override
  SttState build() {
    ref.onDispose(() {
      _speech.stop();
    });
    _init();
    return const SttState();
  }

  Future<void> _init() async {
    final available = await _speech.initialize(
      onError: (error) {
        {
          state = state.copyWith(
            isListening: false,
            error: _friendlyError(error.errorMsg),
          );
        }
      },
      onStatus: (status) {
        if (status == stt.SpeechToText.doneStatus ||
            status == stt.SpeechToText.notListeningStatus) {
          state = state.copyWith(isListening: false);
        }
      },
    );
    state = state.copyWith(isAvailable: available);
  }

  /// 음성 인식 시작.
  /// [onResult] 인식 텍스트 전달 콜백
  /// 권한 거절 시 error 상태 설정
  Future<void> start(void Function(String) onResult) async {
    if (!state.isAvailable) {
      state = state.copyWith(
        error: '음성 검색을 사용할 수 없어요. 마이크 권한을 허용해 주세요.',
      );
      return;
    }
    if (state.isListening) return;

    state = state.copyWith(isListening: true, lastWords: '', error: null);

    await _speech.listen(
      onResult: (result) {
        final words = result.recognizedWords;
        state = state.copyWith(lastWords: words);
        if (result.finalResult && words.isNotEmpty) {
          onResult(words);
        }
      },
      localeId: 'ko_KR',
      listenFor: const Duration(seconds: 10),
      pauseFor: const Duration(seconds: 3),
    );
  }

  Future<void> stop() async {
    await _speech.stop();
    state = state.copyWith(isListening: false);
  }

  Future<void> cancel() async {
    await _speech.cancel();
    state = state.copyWith(isListening: false, lastWords: '');
  }

  String _friendlyError(String raw) {
    if (raw.contains('permission') || raw.contains('not_authorized')) {
      return '마이크 권한이 없어요. 설정에서 허용해 주세요.';
    }
    if (raw.contains('network')) {
      return '인터넷 연결을 확인해 주세요.';
    }
    return '음성 인식에 문제가 생겼어요. 다시 시도해 주세요.';
  }
}

final sttControllerProvider =
    NotifierProvider.autoDispose<SttController, SttState>(
  SttController.new,
);
