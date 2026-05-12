import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/notification/notification_model.dart';
import 'dio_provider.dart';
import 'endpoints.dart';

class NotificationApi {
  const NotificationApi(this._dio);
  final Dio _dio;

  Future<List<NotificationItem>> listNotifications({
    bool unreadOnly = false,
    int limit = 20,
  }) async {
    final res = await _dio.get<Map<String, dynamic>>(
      Endpoints.notifications,
      queryParameters: {
        if (unreadOnly) 'unread': true,
        'limit': limit,
      },
    );
    final items = res.data!['items'] as List<dynamic>;
    return items
        .map((e) => NotificationItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> markRead(String id) async {
    await _dio.post<void>(Endpoints.notificationRead(id));
  }
}

final notificationApiProvider = Provider<NotificationApi>(
  (ref) => NotificationApi(ref.read(dioProvider)),
);
