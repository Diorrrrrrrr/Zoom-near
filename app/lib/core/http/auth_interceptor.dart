import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../auth/auth_provider.dart';
import '../error/error_messages.dart';
import 'endpoints.dart';

/// Authorization 헤더 자동 부착 + 401 시 refresh 시도 + 실패 시 logout
/// DioException 타입별 친근체 메시지 변환
class AuthInterceptor extends Interceptor {
  AuthInterceptor(this._ref);

  final Ref _ref;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = _ref.read(accessTokenProvider);
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
      DioException err, ErrorInterceptorHandler handler) async {
    // 401: refresh 시도 → 실패 시 logout
    if (err.response?.statusCode == 401) {
      final refreshed = await _tryRefresh(err.requestOptions);
      if (refreshed != null) {
        handler.resolve(refreshed);
        return;
      }
      await _ref.read(authProvider.notifier).logout();
      handler.next(_withFriendlyMessage(err, ErrorMessages.sessionExpired));
      return;
    }

    handler.next(_withFriendlyMessage(err, _resolveMessage(err)));
  }

  /// DioException 타입·상태코드로 친근체 메시지 결정
  String _resolveMessage(DioException err) {
    switch (err.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
        return ErrorMessages.connectionTimeout;
      case DioExceptionType.receiveTimeout:
        return ErrorMessages.receiveTimeout;
      case DioExceptionType.connectionError:
        return ErrorMessages.noInternet;
      case DioExceptionType.badResponse:
        final data = err.response?.data;
        if (data is Map<String, dynamic>) {
          final code = data['code'] as String?;
          final message = data['message'] as String?;
          return ErrorMessages.fromCode(
            code,
            fallback: message ??
                ErrorMessages.fromStatusCode(err.response?.statusCode),
          );
        }
        return ErrorMessages.fromStatusCode(err.response?.statusCode);
      case DioExceptionType.cancel:
        return '요청이 취소됐어요.';
      default:
        return ErrorMessages.unknown;
    }
  }

  /// DioException에 friendlyMessage를 extra로 첨부하여 반환
  DioException _withFriendlyMessage(DioException err, String message) {
    return DioException(
      requestOptions: err.requestOptions,
      response: err.response,
      type: err.type,
      error: err.error,
      stackTrace: err.stackTrace,
      message: message,
    );
  }

  Future<Response<dynamic>?> _tryRefresh(RequestOptions original) async {
    try {
      final authState = _ref.read(authProvider);
      final refreshToken = authState.refreshToken;
      if (refreshToken == null) return null;

      final dio = Dio(BaseOptions(baseUrl: original.baseUrl));
      final response = await dio.post<Map<String, dynamic>>(
        Endpoints.refresh,
        data: {'refreshToken': refreshToken},
      );

      final newAccess = response.data?['accessToken'] as String?;
      final newRefresh = response.data?['refreshToken'] as String?;
      if (newAccess == null) return null;

      await _ref.read(authProvider.notifier).updateTokens(
            accessToken: newAccess,
            refreshToken: newRefresh ?? refreshToken,
          );

      final retryOptions = Options(
        method: original.method,
        headers: {
          ...original.headers,
          'Authorization': 'Bearer $newAccess',
        },
      );

      final retryDio = Dio(BaseOptions(baseUrl: original.baseUrl));
      return await retryDio.request<dynamic>(
        original.path,
        data: original.data,
        queryParameters: original.queryParameters,
        options: retryOptions,
      );
    } catch (_) {
      return null;
    }
  }
}
