/// 인증 API 응답 모델
class TokenResponse {
  const TokenResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.userId,
    required this.role,
  });

  factory TokenResponse.fromJson(Map<String, dynamic> json) => TokenResponse(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
        userId: (json['userId'] ?? json['id'] ?? '').toString(),
        role: json['role'] as String? ?? 'TUNTUN',
      );

  final String accessToken;
  final String refreshToken;
  final String userId;
  final String role;
}

/// /api/v1/me 응답 모델
class MeProfile {
  const MeProfile({
    required this.id,
    required this.loginId,
    required this.name,
    required this.role,
    required this.uniqueCode,
    required this.balance,
    this.rankCode,
    this.rankDisplayName,
  });

  factory MeProfile.fromJson(Map<String, dynamic> json) => MeProfile(
        id: (json['id'] ?? '').toString(),
        loginId: json['loginId'] as String? ?? '',
        name: json['name'] as String? ?? '',
        role: json['role'] as String? ?? 'TUNTUN',
        uniqueCode: json['uniqueCode'] as String? ?? '',
        balance: (json['balance'] as num?)?.toInt() ?? 0,
        rankCode: json['rankCode'] as String?,
        rankDisplayName: json['rankDisplayName'] as String?,
      );

  final String id;
  final String loginId;
  final String name;
  final String role;
  final String uniqueCode;
  final int balance;
  final String? rankCode;
  final String? rankDisplayName;

  Map<String, dynamic> toJson() => {
        'id': id,
        'loginId': loginId,
        'name': name,
        'role': role,
        'uniqueCode': uniqueCode,
        'balance': balance,
        'rankCode': rankCode,
        'rankDisplayName': rankDisplayName,
      };
}
