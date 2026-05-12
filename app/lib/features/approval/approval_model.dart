/// 승인 대기 아이템 (TUNTUN의 승인 대기 목록)
class ApprovalItem {
  const ApprovalItem({
    required this.id,
    required this.type,
    required this.requesterName,
    required this.payload,
    required this.expiresAt,
  });

  factory ApprovalItem.fromJson(Map<String, dynamic> json) => ApprovalItem(
        id: (json['id'] ?? '').toString(),
        type: json['type'] as String? ?? '',
        requesterName: json['requesterName'] as String? ?? '',
        payload: json['payload'] as Map<String, dynamic>? ?? {},
        expiresAt: json['expiresAt'] as String? ?? '',
      );

  final String id;
  final String type;
  final String requesterName;
  final Map<String, dynamic> payload;
  final String expiresAt;

  /// payload에서 이벤트 제목 추출 (이벤트 참여 요청인 경우)
  String get eventTitle => payload['eventTitle'] as String? ?? '';
}
