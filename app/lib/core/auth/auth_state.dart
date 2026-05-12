import 'package:equatable/equatable.dart';

/// 사용자 역할 — 백엔드 role 문자열과 1:1 매핑
enum UserRole {
  tuntun('TUNTUN'),
  dundun('DUNDUN'),
  manager('MANAGER'),
  unknown('UNKNOWN');

  const UserRole(this.raw);
  final String raw;

  static UserRole fromString(String? value) {
    return switch (value?.toUpperCase()) {
      'TUNTUN' => UserRole.tuntun,
      'DUNDUN' => UserRole.dundun,
      'MANAGER' => UserRole.manager,
      _ => UserRole.unknown,
    };
  }
}

/// 인증 상태 모델
class AuthState extends Equatable {
  const AuthState({
    this.accessToken,
    this.refreshToken,
    this.role = UserRole.unknown,
    this.userId,
    this.isAuthenticated = false,
    this.name,
  });

  const AuthState.unauthenticated()
      : accessToken = null,
        refreshToken = null,
        role = UserRole.unknown,
        userId = null,
        isAuthenticated = false,
        name = null;

  final String? accessToken;
  final String? refreshToken;
  final UserRole role;
  final String? userId;
  final bool isAuthenticated;
  final String? name;

  AuthState copyWith({
    String? accessToken,
    String? refreshToken,
    UserRole? role,
    String? userId,
    bool? isAuthenticated,
    String? name,
  }) {
    return AuthState(
      accessToken: accessToken ?? this.accessToken,
      refreshToken: refreshToken ?? this.refreshToken,
      role: role ?? this.role,
      userId: userId ?? this.userId,
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      name: name ?? this.name,
    );
  }

  @override
  List<Object?> get props => [
        accessToken,
        refreshToken,
        role,
        userId,
        isAuthenticated,
        name,
      ];
}
