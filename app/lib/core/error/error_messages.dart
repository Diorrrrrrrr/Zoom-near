/// 친근체 에러 메시지 매핑
/// 모든 네트워크/API 에러를 한국어 친근체로 변환
abstract final class ErrorMessages {
  // ── 네트워크 ─────────────────────────────────────────────
  static const String connectionTimeout =
      '인터넷 연결이 느려요. 와이파이나 데이터를 확인해 주세요.';
  static const String receiveTimeout =
      '서버 응답이 늦어요. 잠시 후 다시 시도해 주세요.';
  static const String noInternet =
      '인터넷에 연결되지 않았어요. 연결을 확인해 주세요.';
  static const String unknown =
      '알 수 없는 오류가 발생했어요. 잠시 후 다시 시도해 주세요.';

  // ── 인증 ─────────────────────────────────────────────────
  static const String unauthorized =
      '로그인이 필요해요. 다시 로그인해 주세요.';
  static const String forbidden =
      '접근 권한이 없어요.';
  static const String sessionExpired =
      '로그인이 만료됐어요. 다시 로그인해 주세요.';

  // ── 서버 응답 코드 매핑 ───────────────────────────────────
  static const Map<String, String> _codeMap = {
    'USER_NOT_FOUND': '사용자 정보를 찾을 수 없어요.',
    'INVALID_PASSWORD': '비밀번호가 올바르지 않아요.',
    'EMAIL_ALREADY_EXISTS': '이미 사용 중인 이메일이에요.',
    'INVALID_INVITE_TOKEN': '초대 링크가 유효하지 않아요.',
    'INVITE_ALREADY_USED': '이미 사용된 초대 링크예요.',
    'INSUFFICIENT_BALANCE': '포인트가 부족해요. 충전 후 다시 시도해 주세요.',
    'EVENT_FULL': '정원이 마감된 모임이에요.',
    'EVENT_NOT_FOUND': '모임 정보를 찾을 수 없어요.',
    'ALREADY_JOINED': '이미 참여한 모임이에요.',
    'LINKAGE_NOT_FOUND': '연동 정보를 찾을 수 없어요.',
    'NOTIFICATION_NOT_FOUND': '알림 정보를 찾을 수 없어요.',
  };

  /// 서버 응답 code로 친근체 메시지 반환. 없으면 message 사용.
  static String fromCode(String? code, {String? fallback}) {
    if (code != null && _codeMap.containsKey(code)) {
      return _codeMap[code]!;
    }
    return fallback ?? unknown;
  }

  /// HTTP 상태코드로 기본 메시지 반환
  static String fromStatusCode(int? statusCode) {
    return switch (statusCode) {
      400 => '입력 정보를 다시 확인해 주세요.',
      401 => unauthorized,
      403 => forbidden,
      404 => '요청한 정보를 찾을 수 없어요.',
      409 => '이미 처리된 요청이에요.',
      422 => '입력 내용을 다시 확인해 주세요.',
      500 => '서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요.',
      503 => '서비스가 잠시 점검 중이에요. 곧 돌아올게요.',
      _ => unknown,
    };
  }
}
