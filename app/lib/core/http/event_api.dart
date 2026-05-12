import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/event/event_model.dart';
import 'dio_provider.dart';
import 'endpoints.dart';

class EventListResult {
  const EventListResult({
    required this.content,
    required this.totalElements,
    required this.page,
  });
  final List<EventSummary> content;
  final int totalElements;
  final int page;
}

class EventApi {
  const EventApi(this._dio);
  final Dio _dio;

  Future<EventListResult> listEvents({
    String status = 'OPEN',
    String q = '',
    String regionText = '',
    int page = 0,
    int size = 20,
  }) async {
    final res = await _dio.get<Map<String, dynamic>>(
      Endpoints.events,
      queryParameters: {
        'status': status,
        if (q.isNotEmpty) 'q': q,
        if (regionText.isNotEmpty) 'regionText': regionText,
        'page': page,
        'size': size,
      },
    );
    final data = res.data!;
    final content = (data['content'] as List<dynamic>)
        .map((e) => EventSummary.fromJson(e as Map<String, dynamic>))
        .toList();
    return EventListResult(
      content: content,
      totalElements: (data['totalElements'] as num?)?.toInt() ?? 0,
      page: (data['page'] as num?)?.toInt() ?? 0,
    );
  }

  Future<EventDetail> getEvent(String id) async {
    final res = await _dio.get<Map<String, dynamic>>(Endpoints.eventDetail(id));
    return EventDetail.fromJson(res.data!);
  }

  Future<EventDetail> createEvent({
    required String title,
    required String description,
    required String regionText,
    required String category,
    required String startsAt,
    required String endsAt,
    required int capacity,
    required int pointCost,
    bool isManagerProgram = false,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      Endpoints.events,
      data: {
        'title': title,
        'description': description,
        'regionText': regionText,
        'category': category,
        'startsAt': startsAt,
        'endsAt': endsAt,
        'capacity': capacity,
        'pointCost': pointCost,
        'isManagerProgram': isManagerProgram,
      },
    );
    return EventDetail.fromJson(res.data!);
  }

  Future<Map<String, dynamic>> joinEvent(String id, {String? proxiedFor}) async {
    final res = await _dio.post<Map<String, dynamic>>(
      Endpoints.eventJoin(id),
      data: {
        if (proxiedFor != null) 'proxiedFor': proxiedFor,
      },
    );
    return res.data!;
  }

  Future<void> cancelEvent(String id) async {
    await _dio.post<void>(Endpoints.eventCancel(id));
  }
}

final eventApiProvider = Provider<EventApi>(
  (ref) => EventApi(ref.read(dioProvider)),
);
