import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'dio_provider.dart';
import 'endpoints.dart';

class LedgerItem {
  const LedgerItem({
    required this.delta,
    required this.reason,
    required this.createdAt,
    required this.balanceAfter,
  });

  factory LedgerItem.fromJson(Map<String, dynamic> json) => LedgerItem(
        delta: (json['delta'] as num).toInt(),
        reason: json['reason'] as String? ?? '',
        createdAt: json['createdAt'] as String? ?? '',
        balanceAfter: (json['balanceAfter'] as num).toInt(),
      );

  final int delta;
  final String reason;
  final String createdAt;
  final int balanceAfter;
}

class PointApi {
  const PointApi(this._dio);
  final Dio _dio;

  Future<int> mockTopup({required int amount, String? reasonText}) async {
    final res = await _dio.post<Map<String, dynamic>>(
      Endpoints.pointsMockTopup,
      data: {
        'amount': amount,
        if (reasonText != null) 'reasonText': reasonText,
      },
    );
    return (res.data!['newBalance'] as num).toInt();
  }

  Future<int> mockTopupProxy({
    required String tuntunId,
    required int amount,
    String? reasonText,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      Endpoints.pointsMockTopupProxy,
      data: {
        'tuntunId': tuntunId,
        'amount': amount,
        if (reasonText != null) 'reasonText': reasonText,
      },
    );
    return (res.data!['newBalance'] as num).toInt();
  }

  Future<int> getBalance() async {
    final res = await _dio.get<Map<String, dynamic>>(Endpoints.pointsBalance);
    return (res.data!['balance'] as num).toInt();
  }

  Future<List<LedgerItem>> getLedger({int limit = 20}) async {
    final res = await _dio.get<Map<String, dynamic>>(
      Endpoints.pointsLedger,
      queryParameters: {'limit': limit},
    );
    final items = res.data!['items'] as List<dynamic>;
    return items
        .map((e) => LedgerItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}

final pointApiProvider = Provider<PointApi>(
  (ref) => PointApi(ref.read(dioProvider)),
);
