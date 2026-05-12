/// 이벤트 목록 아이템
class EventSummary {
  const EventSummary({
    required this.id,
    required this.title,
    required this.regionText,
    required this.startsAt,
    required this.endsAt,
    required this.capacity,
    required this.joinedCount,
    required this.pointCost,
    required this.isManagerProgram,
  });

  factory EventSummary.fromJson(Map<String, dynamic> json) => EventSummary(
        id: (json['id'] ?? '').toString(),
        title: json['title'] as String? ?? '',
        regionText: json['regionText'] as String? ?? '',
        startsAt: json['startsAt'] as String? ?? '',
        endsAt: json['endsAt'] as String? ?? '',
        capacity: (json['capacity'] as num?)?.toInt() ?? 0,
        joinedCount: (json['joinedCount'] as num?)?.toInt() ?? 0,
        pointCost: (json['pointCost'] as num?)?.toInt() ?? 0,
        isManagerProgram: json['isManagerProgram'] as bool? ?? false,
      );

  final String id;
  final String title;
  final String regionText;
  final String startsAt;
  final String endsAt;
  final int capacity;
  final int joinedCount;
  final int pointCost;
  final bool isManagerProgram;

  bool get isFull => joinedCount >= capacity;
}

/// 이벤트 상세
class EventDetail extends EventSummary {
  const EventDetail({
    required super.id,
    required super.title,
    required super.regionText,
    required super.startsAt,
    required super.endsAt,
    required super.capacity,
    required super.joinedCount,
    required super.pointCost,
    required super.isManagerProgram,
    required this.description,
    required this.status,
    this.creatorName,
  });

  factory EventDetail.fromJson(Map<String, dynamic> json) {
    final summary = EventSummary.fromJson(json);
    return EventDetail(
      id: summary.id,
      title: summary.title,
      regionText: summary.regionText,
      startsAt: summary.startsAt,
      endsAt: summary.endsAt,
      capacity: summary.capacity,
      joinedCount: summary.joinedCount,
      pointCost: summary.pointCost,
      isManagerProgram: summary.isManagerProgram,
      description: json['description'] as String? ?? '',
      status: json['status'] as String? ?? 'OPEN',
      creatorName: (json['creator'] as Map<String, dynamic>?)?['name'] as String?,
    );
  }

  final String description;
  final String status;
  final String? creatorName;
}
