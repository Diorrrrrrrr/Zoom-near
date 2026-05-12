import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/routing/app_router.dart';
import 'core/theme/app_theme.dart';
import 'core/theme/app_text_scaler.dart';
import 'core/auth/auth_provider.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ProviderScope(child: ZoomNearApp()));
}

/// ZOOM NEAR (주니어) 앱 루트. secure storage 로드는 비동기 백그라운드.
class ZoomNearApp extends ConsumerStatefulWidget {
  const ZoomNearApp({super.key});

  @override
  ConsumerState<ZoomNearApp> createState() => _ZoomNearAppState();
}

class _ZoomNearAppState extends ConsumerState<ZoomNearApp> {
  @override
  void initState() {
    super.initState();
    // 백그라운드 토큰 로드 — UI 블로킹 X
    Future.microtask(() {
      ref
          .read(authProvider.notifier)
          .loadFromStorage()
          .timeout(const Duration(seconds: 2))
          .catchError((_) {});
    });
  }

  @override
  Widget build(BuildContext context) {
    final router = ref.watch(appRouterProvider);
    final asyncScale = ref.watch(appFontScaleProvider);
    final appScale = asyncScale.valueOrNull ?? AppFontScale.normal;

    return MaterialApp.router(
      title: '주니어',
      debugShowCheckedModeBanner: false,
      theme: buildLightTheme(),
      routerConfig: router,
      builder: (context, child) {
        final scaler = resolveTextScaler(context, appScale);
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(textScaler: scaler),
          child: child ?? const SizedBox.shrink(),
        );
      },
    );
  }
}
