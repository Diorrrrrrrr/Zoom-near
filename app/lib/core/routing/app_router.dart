import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../auth/auth_provider.dart';
import '../auth/auth_state.dart';
import '../../features/auth/login_screen.dart';
import '../../features/auth/signup_screen.dart';
import '../../features/home/home_screen.dart';
import '../../features/event/events_screen.dart';
import '../../features/event/event_detail_screen.dart';
import '../../features/event/event_create_screen.dart';
import '../../features/me/my_page_screen.dart';
import '../../features/me/charge_screen.dart';
import '../../features/me/change_password_screen.dart';
import '../../features/me/withdraw_screen.dart';
import '../../features/linkage/linkage_screen.dart';
import '../../features/linkage/proxy_charge_screen.dart';
import '../../features/notification/notifications_screen.dart';
import '../../features/approval/approvals_screen.dart';
import '../../features/invite/invite_accept_screen.dart';
import '../../features/settings/font_size_screen.dart';
import '../../features/manager/manager_console_screen.dart';
import '../../features/manager/manager_apply_screen.dart';
import '../../features/legal/terms_screen.dart';
import '../../features/legal/privacy_screen.dart';
import '../../features/legal/help_screen.dart';

/// GoRouter 인스턴스 provider
final appRouterProvider = Provider<GoRouter>((ref) {
  final refreshNotifier = _AuthChangeNotifier(ref);

  return GoRouter(
    initialLocation: '/auth/login',
    refreshListenable: refreshNotifier,
    // 딥링크: zoomnear://invite?token=XXX → /invite/accept?token=XXX
    redirect: (context, state) {
      final isAuth = ref.read(isAuthenticatedProvider);
      final role = ref.read(userRoleProvider);
      final loc = state.matchedLocation;

      final isAuthRoute = loc.startsWith('/auth') || loc == '/splash';
      final isInviteRoute = loc.startsWith('/invite');

      // 미인증: auth 라우트·초대 라우트 외에는 로그인으로
      if (!isAuth && !isAuthRoute && !isInviteRoute) return '/auth/login';
      // 인증됨: auth·splash 라우트에서 홈으로
      if (isAuth && isAuthRoute) return '/home';

      // role 가드: /events/create는 TUNTUN·MANAGER만
      if (loc == '/events/create') {
        if (role == UserRole.tuntun || role == UserRole.manager) return null;
        return '/events';
      }

      // role 가드: /linkage, /linkage/proxy-charge는 DUNDUN만
      if (loc.startsWith('/linkage')) {
        if (role == UserRole.dundun) return null;
        return '/home';
      }

      // role 가드: /approvals는 TUNTUN만
      if (loc == '/approvals') {
        if (role == UserRole.tuntun) return null;
        return '/home';
      }

      // role 가드: /manager/console은 MANAGER만
      if (loc.startsWith('/manager/console')) {
        if (role == UserRole.manager) return null;
        return '/home';
      }

      return null;
    },
    routes: [
      GoRoute(
        path: '/splash',
        name: 'splash',
        pageBuilder: (context, state) => _buildPage(
          state,
          const _SplashScreen(),
          meta: const {'autoSpeak': false, 'title': '시작', 'spokenText': ''},
        ),
      ),
      GoRoute(
        path: '/auth/login',
        name: 'login',
        pageBuilder: (context, state) => _buildPage(
          state,
          const LoginScreen(),
          meta: const {'autoSpeak': false, 'title': '로그인', 'spokenText': ''},
        ),
      ),
      GoRoute(
        path: '/auth/signup',
        name: 'signup',
        pageBuilder: (context, state) {
          final token = state.uri.queryParameters['inviteToken'];
          return _buildPage(
            state,
            SignupScreen(inviteToken: token),
            meta: const {
              'autoSpeak': false,
              'title': '회원가입',
              'spokenText': ''
            },
          );
        },
      ),
      GoRoute(
        path: '/home',
        name: 'home',
        pageBuilder: (context, state) => _buildPage(
          state,
          const HomeScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '홈',
            'spokenText': '홈 화면입니다.'
          },
        ),
      ),
      GoRoute(
        path: '/events',
        name: 'events',
        pageBuilder: (context, state) => _buildPage(
          state,
          const EventsScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '모임',
            'spokenText': '모임 목록 화면입니다.'
          },
        ),
        routes: [
          GoRoute(
            path: 'create',
            name: 'eventCreate',
            pageBuilder: (context, state) => _buildPage(
              state,
              const EventCreateScreen(),
              meta: const {
                'autoSpeak': false,
                'title': '모임 만들기',
                'spokenText': ''
              },
            ),
          ),
          GoRoute(
            path: ':id',
            name: 'eventDetail',
            pageBuilder: (context, state) {
              final id = state.pathParameters['id']!;
              return _buildPage(
                state,
                EventDetailScreen(eventId: id),
                meta: const {
                  'autoSpeak': true,
                  'title': '모임 상세',
                  'spokenText': '모임 상세 화면입니다.'
                },
              );
            },
          ),
        ],
      ),
      GoRoute(
        path: '/notifications',
        name: 'notifications',
        pageBuilder: (context, state) => _buildPage(
          state,
          const NotificationsScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '알림',
            'spokenText': '알림 화면입니다.'
          },
        ),
      ),
      GoRoute(
        path: '/approvals',
        name: 'approvals',
        pageBuilder: (context, state) => _buildPage(
          state,
          const ApprovalsScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '승인 요청',
            'spokenText': '승인 요청 화면입니다.'
          },
        ),
      ),
      GoRoute(
        path: '/me',
        name: 'me',
        pageBuilder: (context, state) => _buildPage(
          state,
          const MyPageScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '내 정보',
            'spokenText': '내 정보 화면입니다.'
          },
        ),
        routes: [
          GoRoute(
            path: 'charge',
            name: 'meCharge',
            pageBuilder: (context, state) => _buildPage(
              state,
              const ChargeScreen(),
              meta: const {
                'autoSpeak': false,
                'title': '포인트 충전',
                'spokenText': ''
              },
            ),
          ),
          GoRoute(
            path: 'change-password',
            name: 'changePassword',
            pageBuilder: (context, state) => _buildPage(
              state,
              const ChangePasswordScreen(),
              meta: const {
                'autoSpeak': false,
                'title': '비밀번호 변경',
                'spokenText': ''
              },
            ),
          ),
          GoRoute(
            path: 'withdraw',
            name: 'withdraw',
            pageBuilder: (context, state) => _buildPage(
              state,
              const WithdrawScreen(),
              meta: const {
                'autoSpeak': false,
                'title': '회원 탈퇴',
                'spokenText': ''
              },
            ),
          ),
        ],
      ),
      GoRoute(
        path: '/linkage',
        name: 'linkage',
        pageBuilder: (context, state) => _buildPage(
          state,
          const LinkageScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '연동 관리',
            'spokenText': '연동 관리 화면입니다.'
          },
        ),
        routes: [
          GoRoute(
            path: 'proxy-charge',
            name: 'proxyCharge',
            pageBuilder: (context, state) => _buildPage(
              state,
              const ProxyChargeScreen(),
              meta: const {
                'autoSpeak': false,
                'title': '대리 충전',
                'spokenText': ''
              },
            ),
          ),
        ],
      ),
      GoRoute(
        path: '/invite/accept',
        name: 'inviteAccept',
        pageBuilder: (context, state) {
          final token = state.uri.queryParameters['token'] ?? '';
          return _buildPage(
            state,
            InviteAcceptScreen(token: token),
            meta: const {
              'autoSpeak': true,
              'title': '초대 받기',
              'spokenText': '초대 링크 화면입니다.'
            },
          );
        },
      ),
      // 글자 크기 설정
      GoRoute(
        path: '/settings/font-size',
        name: 'fontSizeSettings',
        pageBuilder: (context, state) => _buildPage(
          state,
          const FontSizeScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '글자 크기 설정',
            'spokenText': '글자 크기 설정 화면입니다.'
          },
        ),
      ),
      // 매니저 콘솔
      GoRoute(
        path: '/manager/console',
        name: 'managerConsole',
        pageBuilder: (context, state) => _buildPage(
          state,
          const ManagerConsoleScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '매니저 콘솔',
            'spokenText': '매니저 콘솔 화면이에요.'
          },
        ),
      ),
      // 매니저 신청
      GoRoute(
        path: '/manager/apply',
        name: 'managerApply',
        pageBuilder: (context, state) => _buildPage(
          state,
          const ManagerApplyScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '매니저 신청',
            'spokenText': '매니저 신청 화면이에요.'
          },
        ),
      ),
      // 법적 고지
      GoRoute(
        path: '/legal/terms',
        name: 'terms',
        pageBuilder: (context, state) => _buildPage(
          state,
          const TermsScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '이용약관',
            'spokenText': '이용약관 화면이에요.'
          },
        ),
      ),
      GoRoute(
        path: '/legal/privacy',
        name: 'privacy',
        pageBuilder: (context, state) => _buildPage(
          state,
          const PrivacyScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '개인정보처리방침',
            'spokenText': '개인정보처리방침 화면이에요.'
          },
        ),
      ),
      GoRoute(
        path: '/legal/help',
        name: 'help',
        pageBuilder: (context, state) => _buildPage(
          state,
          const HelpScreen(),
          meta: const {
            'autoSpeak': true,
            'title': '도움말',
            'spokenText': '도움말 화면이에요.'
          },
        ),
      ),
    ],
  );
});

CustomTransitionPage<void> _buildPage(
  GoRouterState state,
  Widget child, {
  Map<String, dynamic> meta = const {},
}) {
  return CustomTransitionPage<void>(
    key: state.pageKey,
    child: child,
    transitionsBuilder: (context, animation, secondaryAnimation, child) {
      return FadeTransition(
        opacity: CurvedAnimation(parent: animation, curve: Curves.easeOut),
        child: child,
      );
    },
    transitionDuration: const Duration(milliseconds: 250),
  );
}

/// AuthState 변화를 GoRouter refreshListenable에 연결
class _AuthChangeNotifier extends ChangeNotifier {
  _AuthChangeNotifier(this._ref) {
    _sub = _ref.listen<AuthState>(authProvider, (_, __) => notifyListeners());
  }

  final Ref _ref;
  late final ProviderSubscription<AuthState> _sub;

  @override
  void dispose() {
    _sub.close();
    super.dispose();
  }
}

class _SplashScreen extends StatelessWidget {
  const _SplashScreen();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      backgroundColor: Color(0xFFFFFFFF),
      body: Center(
        child: Text(
          '주니어',
          style: TextStyle(
            fontSize: 32,
            fontWeight: FontWeight.w700,
            color: Color(0xFFC2410C),
          ),
        ),
      ),
    );
  }
}
