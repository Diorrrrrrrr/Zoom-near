import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'dio_provider.dart';
import 'endpoints.dart';

class InviteInfo {
  const InviteInfo({
    required this.token,
    required this.expiresAt,
    required this.url,
  });

  factory InviteInfo.fromJson(Map<String, dynamic> json) => InviteInfo(
        token: json['token'] as String? ?? '',
        expiresAt: json['expiresAt'] as String? ?? '',
        url: json['url'] as String? ?? '',
      );

  final String token;
  final String expiresAt;
  final String url;
}

class InvitePreview {
  const InvitePreview({
    required this.inviterName,
    required this.expiresAt,
    required this.valid,
  });

  factory InvitePreview.fromJson(Map<String, dynamic> json) => InvitePreview(
        inviterName: json['inviterName'] as String? ?? '',
        expiresAt: json['expiresAt'] as String? ?? '',
        valid: json['valid'] as bool? ?? false,
      );

  final String inviterName;
  final String expiresAt;
  final bool valid;
}

class InviteApi {
  const InviteApi(this._dio);
  final Dio _dio;

  Future<InviteInfo> createInvite() async {
    final res = await _dio.post<Map<String, dynamic>>(Endpoints.invites);
    return InviteInfo.fromJson(res.data!);
  }

  Future<InvitePreview> getInvite(String token) async {
    final res = await _dio.get<Map<String, dynamic>>(Endpoints.inviteByToken(token));
    return InvitePreview.fromJson(res.data!);
  }
}

final inviteApiProvider = Provider<InviteApi>(
  (ref) => InviteApi(ref.read(dioProvider)),
);
