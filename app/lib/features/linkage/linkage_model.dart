/// 연동 아이템 (DUNDUN의 튼튼이 연동)
class LinkageItem {
  const LinkageItem({
    required this.id,
    required this.tuntunId,
    required this.tuntunName,
    required this.isPrimary,
  });

  factory LinkageItem.fromJson(Map<String, dynamic> json) => LinkageItem(
        id: (json['id'] ?? '').toString(),
        tuntunId: (json['tuntunId'] ?? '').toString(),
        tuntunName: json['tuntunName'] as String? ?? '',
        isPrimary: json['isPrimary'] as bool? ?? false,
      );

  final String id;
  final String tuntunId;
  final String tuntunName;
  final bool isPrimary;
}

/// 튼튼이 코드 검색 결과
class TuntunSearchResult {
  const TuntunSearchResult({
    required this.id,
    required this.name,
    required this.uniqueCode,
    required this.role,
  });

  factory TuntunSearchResult.fromJson(Map<String, dynamic> json) => TuntunSearchResult(
        id: (json['id'] ?? '').toString(),
        name: json['name'] as String? ?? '',
        uniqueCode: json['uniqueCode'] as String? ?? '',
        role: json['role'] as String? ?? 'TUNTUN',
      );

  final String id;
  final String name;
  final String uniqueCode;
  final String role;
}
