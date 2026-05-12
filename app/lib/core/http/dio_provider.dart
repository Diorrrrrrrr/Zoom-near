import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'auth_interceptor.dart';

/// baseUrl은 --dart-define=API_BASE_URL=https://... 으로 주입
const String _defaultBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://localhost:8080',
);

/// 전역 Dio 인스턴스 provider
final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(
    BaseOptions(
      baseUrl: _defaultBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 30),
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    ),
  );

  dio.interceptors.add(AuthInterceptor(ref));
  dio.interceptors.add(
    LogInterceptor(
      requestBody: true,
      responseBody: true,
      logPrint: (obj) {
        // TODO(Day1PM): 프로덕션에서는 제거 또는 구조화 로깅으로 교체
        assert(() {
          // ignore: avoid_print
          print('[Dio] $obj');
          return true;
        }());
      },
    ),
  );

  return dio;
});
