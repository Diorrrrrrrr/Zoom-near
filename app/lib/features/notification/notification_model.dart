/// 알림 payload (딥링크용)
class NotificationPayload {
  const NotificationPayload({this.eventId});

  factory NotificationPayload.fromJson(Map<String, dynamic> json) =>
      NotificationPayload(
        eventId: json['eventId'] as String?,
      );

  final String? eventId;
}

/// 알림 아이템
class NotificationItem {
  const NotificationItem({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    required this.createdAt,
    this.readAt,
    this.payload,
  });

  factory NotificationItem.fromJson(Map<String, dynamic> json) =>
      NotificationItem(
        id: (json['id'] ?? '').toString(),
        type: json['type'] as String? ?? '',
        title: json['title'] as String? ?? '',
        body: json['body'] as String? ?? '',
        createdAt: json['createdAt'] as String? ?? '',
        readAt: json['readAt'] as String?,
        payload: json['payload'] != null
            ? NotificationPayload.fromJson(
                json['payload'] as Map<String, dynamic>)
            : null,
      );

  final String id;
  final String type;
  final String title;
  final String body;
  final String createdAt;
  final String? readAt;
  final NotificationPayload? payload;

  bool get isUnread => readAt == null;
}
