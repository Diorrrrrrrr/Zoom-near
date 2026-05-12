import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/approval/approval_model.dart';
import 'dio_provider.dart';
import 'endpoints.dart';

class ApprovalApi {
  const ApprovalApi(this._dio);
  final Dio _dio;

  Future<List<ApprovalItem>> myPending({String status = 'PENDING'}) async {
    final res = await _dio.get<Map<String, dynamic>>(
      Endpoints.approvalsMe,
      queryParameters: {'status': status},
    );
    final items = res.data!['items'] as List<dynamic>;
    return items
        .map((e) => ApprovalItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> approve(String id) async {
    await _dio.post<void>(Endpoints.approvalApprove(id));
  }

  Future<void> reject(String id) async {
    await _dio.post<void>(Endpoints.approvalReject(id));
  }
}

final approvalApiProvider = Provider<ApprovalApi>(
  (ref) => ApprovalApi(ref.read(dioProvider)),
);
