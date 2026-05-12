import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../auth/auth_models.dart';
import 'dio_provider.dart';
import 'endpoints.dart';

class MeApi {
  const MeApi(this._dio);
  final Dio _dio;

  Future<MeProfile> getMe() async {
    final res = await _dio.get<Map<String, dynamic>>(Endpoints.me);
    return MeProfile.fromJson(res.data!);
  }

  /// 비밀번호 변경 — 204 응답
  Future<void> changePassword({
    required String oldPassword,
    required String newPassword,
  }) async {
    await _dio.put<void>(
      Endpoints.mePassword,
      data: {'oldPassword': oldPassword, 'newPassword': newPassword},
    );
  }
}

final meApiProvider = Provider<MeApi>(
  (ref) => MeApi(ref.read(dioProvider)),
);
