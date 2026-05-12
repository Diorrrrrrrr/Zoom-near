import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// flutter_secure_storage 래퍼 — access/refresh 토큰 영속화
class SecureTokenStorage {
  const SecureTokenStorage(this._storage);

  final FlutterSecureStorage _storage;

  static const _kAccessToken = 'zoomnear_access_token';
  static const _kRefreshToken = 'zoomnear_refresh_token';
  static const _kUserId = 'zoomnear_user_id';
  static const _kRole = 'zoomnear_role';

  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String role,
  }) async {
    await Future.wait([
      _storage.write(key: _kAccessToken, value: accessToken),
      _storage.write(key: _kRefreshToken, value: refreshToken),
      _storage.write(key: _kUserId, value: userId),
      _storage.write(key: _kRole, value: role),
    ]);
  }

  Future<void> updateAccessToken(String accessToken) async {
    await _storage.write(key: _kAccessToken, value: accessToken);
  }

  Future<({String accessToken, String refreshToken, String userId, String role})?> loadTokens() async {
    final results = await Future.wait([
      _storage.read(key: _kAccessToken),
      _storage.read(key: _kRefreshToken),
      _storage.read(key: _kUserId),
      _storage.read(key: _kRole),
    ]);
    final accessToken = results[0];
    final refreshToken = results[1];
    final userId = results[2];
    final role = results[3];

    if (accessToken == null || refreshToken == null || userId == null || role == null) {
      return null;
    }
    return (
      accessToken: accessToken,
      refreshToken: refreshToken,
      userId: userId,
      role: role,
    );
  }

  Future<String?> getRefreshToken() => _storage.read(key: _kRefreshToken);

  Future<void> clearAll() async {
    await Future.wait([
      _storage.delete(key: _kAccessToken),
      _storage.delete(key: _kRefreshToken),
      _storage.delete(key: _kUserId),
      _storage.delete(key: _kRole),
    ]);
  }
}

final secureTokenStorageProvider = Provider<SecureTokenStorage>((ref) {
  const storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  return SecureTokenStorage(storage);
});
