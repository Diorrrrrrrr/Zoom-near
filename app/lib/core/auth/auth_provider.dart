import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'auth_state.dart';
import 'auth_models.dart';
import 'secure_token_storage.dart';

/// 전역 인증 상태 provider
final authProvider = StateNotifierProvider<AuthNotifier, AuthState>(
  (ref) => AuthNotifier(ref.read(secureTokenStorageProvider)),
);

/// 인증 여부만 필요할 때 사용하는 파생 provider
final isAuthenticatedProvider = Provider<bool>(
  (ref) => ref.watch(authProvider).isAuthenticated,
);

/// accessToken만 필요할 때 사용하는 파생 provider
final accessTokenProvider = Provider<String?>(
  (ref) => ref.watch(authProvider).accessToken,
);

/// 현재 역할만 필요할 때 사용하는 파생 provider
final userRoleProvider = Provider<UserRole>(
  (ref) => ref.watch(authProvider).role,
);

/// 인증 상태 노티파이어
class AuthNotifier extends StateNotifier<AuthState> {
  AuthNotifier(this._storage) : super(const AuthState.unauthenticated());

  final SecureTokenStorage _storage;

  /// 앱 시작 시 secure storage에서 토큰 복원
  Future<void> loadFromStorage() async {
    final saved = await _storage.loadTokens();
    if (saved == null) return;
    state = AuthState(
      accessToken: saved.accessToken,
      refreshToken: saved.refreshToken,
      role: UserRole.fromString(saved.role),
      userId: saved.userId,
      isAuthenticated: true,
    );
  }

  /// 로그인 성공 후 상태 + 스토리지 업데이트
  Future<void> login(TokenResponse response) async {
    final role = UserRole.fromString(response.role);
    await _storage.saveTokens(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      userId: response.userId,
      role: response.role,
    );
    state = AuthState(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      role: role,
      userId: response.userId,
      isAuthenticated: true,
    );
  }

  /// refresh 후 토큰만 갱신
  Future<void> updateTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    await Future.wait([
      _storage.updateAccessToken(accessToken),
    ]);
    state = state.copyWith(
      accessToken: accessToken,
      refreshToken: refreshToken,
    );
  }

  /// accessToken만 갱신 (interceptor 용)
  Future<void> updateToken(String accessToken) async {
    await _storage.updateAccessToken(accessToken);
    state = state.copyWith(accessToken: accessToken);
  }

  /// 로그아웃: 스토리지 + 상태 초기화
  Future<void> logout() async {
    await _storage.clearAll();
    state = const AuthState.unauthenticated();
  }

  /// MeProfile로 이름 등 보완
  void applyProfile(MeProfile profile) {
    state = state.copyWith(name: profile.name);
  }
}
