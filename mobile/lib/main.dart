import 'package:flutter/material.dart';

import 'services/api_service.dart';

void main() => runApp(const VivaBemApp());

class VivaBemApp extends StatelessWidget {
  const VivaBemApp({super.key, this.apiService});

  final ApiService? apiService;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'VivaBem 50+',
      theme: ThemeData(
        colorSchemeSeed: const Color(0xFF0F7A68),
        // Texto maior para o público 50+.
        textTheme: Typography.blackMountainView.apply(fontSizeFactor: 1.15),
      ),
      home: HomeScreen(apiService: apiService ?? ApiService()),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, required this.apiService});

  final ApiService apiService;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  String _message = 'Verificando a conexão...';

  @override
  void initState() {
    super.initState();
    _checkConnection();
  }

  Future<void> _checkConnection() async {
    String message;
    try {
      final healthy = await widget.apiService.isHealthy();
      message = healthy
          ? 'API e banco de dados conectados.'
          : 'A API está no ar, mas o banco de dados não respondeu.';
    } catch (_) {
      message = 'Não foi possível falar com a API. Verifique se ela está rodando.';
    }
    if (mounted) setState(() => _message = message);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('VivaBem 50+')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Sua rotina de saúde organizada em um só lugar.'),
            const SizedBox(height: 16),
            Text(_message, style: const TextStyle(fontWeight: FontWeight.w600)),
          ],
        ),
      ),
    );
  }
}
