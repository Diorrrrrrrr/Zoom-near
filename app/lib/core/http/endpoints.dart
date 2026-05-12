/// ZOOM NEAR API 엔드포인트 상수 — Lane B API 계약 기준
abstract final class Endpoints {
  // Auth
  static const String signup = '/api/v1/auth/signup';
  static const String login = '/api/v1/auth/login';
  static const String refresh = '/api/v1/auth/refresh';

  // Me
  static const String me = '/api/v1/me';
  static const String mePassword = '/api/v1/me/password';

  // Events
  static const String events = '/api/v1/events';
  static String eventDetail(String id) => '/api/v1/events/$id';
  static String eventJoin(String id) => '/api/v1/events/$id/join';
  static String eventCancel(String id) => '/api/v1/events/$id/cancel';

  // Points
  static const String pointsMockTopup = '/api/v1/points/mock-topup';
  static const String pointsMockTopupProxy = '/api/v1/points/mock-topup-proxy';
  static const String pointsBalance = '/api/v1/points/me/balance';
  static const String pointsLedger = '/api/v1/points/me/ledger';

  // Users
  static const String usersSearch = '/api/v1/users/search';

  // Linkages
  static const String linkages = '/api/v1/linkages';
  static String linkageById(String id) => '/api/v1/linkages/$id';
  static const String myLinkages = '/api/v1/linkages/me';

  // Invites
  static const String invites = '/api/v1/invites';
  static String inviteByToken(String token) => '/api/v1/invites/$token';

  // Notifications
  static const String notifications = '/api/v1/notifications';
  static String notificationRead(String id) => '/api/v1/notifications/$id/read';

  // Approvals
  static const String approvalsMe = '/api/v1/approvals/me';
  static String approvalApprove(String id) => '/api/v1/approvals/$id/approve';
  static String approvalReject(String id) => '/api/v1/approvals/$id/reject';
}
