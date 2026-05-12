import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/linkage/linkage_model.dart';
import 'dio_provider.dart';
import 'endpoints.dart';

class LinkageApi {
  const LinkageApi(this._dio);
  final Dio _dio;

  /// uniqueCode로 TUNTUN 검색
  Future<TuntunSearchResult> searchByCode(String uniqueCode) async {
    final res = await _dio.get<Map<String, dynamic>>(
      Endpoints.usersSearch,
      queryParameters: {'uniqueCode': uniqueCode},
    );
    return TuntunSearchResult.fromJson(res.data!);
  }

  /// 연동 생성 (DUNDUN 전용)
  Future<LinkageItem> link({required String tuntunId, bool isPrimary = false}) async {
    final res = await _dio.post<Map<String, dynamic>>(
      Endpoints.linkages,
      data: {'tuntunId': tuntunId, 'isPrimary': isPrimary},
    );
    return LinkageItem.fromJson(res.data!);
  }

  /// 연동 해제
  Future<void> unlink(String linkageId) async {
    await _dio.delete<void>(Endpoints.linkageById(linkageId));
  }

  /// 내 연동 목록 (DUNDUN 본인)
  Future<List<LinkageItem>> myLinkages() async {
    final res = await _dio.get<List<dynamic>>(Endpoints.myLinkages);
    return (res.data ?? [])
        .map((e) => LinkageItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}

final linkageApiProvider = Provider<LinkageApi>(
  (ref) => LinkageApi(ref.read(dioProvider)),
);
