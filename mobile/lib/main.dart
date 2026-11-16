import 'package:flutter/material.dart';

import 'screens/home_shell.dart';
import 'screens/login_screen.dart';
import 'services/api_service.dart';
import 'theme.dart';

void main() {
  final navigatorKey = GlobalKey<NavigatorState>();
  final apiService = ApiService(
    onSessionExpired: () => navigatorKey.currentState?.pushNamedAndRemoveUntil('/', (_) => false),
  );
  runApp(VivaBemApp(apiService: apiService, navigatorKey: navigatorKey));
}

class VivaBemApp extends StatelessWidget {
  const VivaBemApp({super.key, required this.apiService, required this.navigatorKey});

  final ApiService apiService;
  final GlobalKey<NavigatorState> navigatorKey;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      navigatorKey: navigatorKey,
      title: 'VivaBem 50+',
      theme: vivaTheme(),
      routes: {
        '/': (_) => LoginScreen(api: apiService),
        '/home': (_) => HomeShell(api: apiService),
      },
    );
  }
}
