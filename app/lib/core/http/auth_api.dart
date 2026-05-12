import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../auth/auth_models.dart';
import 'dio_provider.dart';
import 'endpoints.dart';

class AuthApi {
  const AuthApi(this._dio);
  final Dio _dio;

  Future<TokenResponse> signup({
    required String loginId,
    required String password,
    required String phone,
    required String name,
    required String role,
    String? email,
    String? inviteToken,
    String? birthDate,
  }) async {
    final body = <String, dynamic>{
      'loginId': loginId,
      'password': password,
      'phone': phone,
      'name': name,
      'role': role,
      if (email != null && email.isNotEmpty) 'email': email,
      if (inviteToken != null && inviteToken.isNotEmpty) 'inviteToken': inviteToken,
      if (birthDate != null && birthDate.isNotEmpty) 'birthDate': birthDate,
    };
    final res = await _dio.post<Map<String, dynamic>>(Endpoints.signup, data: body);
    return TokenResponse.fromJson(res.data!);
  }

  Future<TokenResponse> login({
    required String loginId,
    required String password,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      Endpoints.login,
      data: {'loginId': loginId, 'password': password},
    );
    return TokenResponse.fromJson(res.data!);
  }

  Future<TokenResponse> refresh(String refreshToken) async {
    final res = await _dio.post<Map<String, dynamic>>(
      Endpoints.refresh,
      data: {'refreshToken': refreshToken},
    );
    return TokenResponse.fromJson(res.data!);
  }
}

final authApiProvider = Provider<AuthApi>(
  (ref) => AuthApi(ref.read(dioProvider)),
);
