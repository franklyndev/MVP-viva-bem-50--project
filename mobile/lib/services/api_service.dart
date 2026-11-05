import 'dart:convert';

import 'package:http/http.dart' as http;

/// Todas as chamadas à API passam por aqui.
class ApiService {
  ApiService({http.Client? client}) : _client = client ?? http.Client();

  /// 10.0.2.2 é o endereço do computador visto pelo emulador Android.
  /// Em celular físico, rode com --dart-define=API_URL=http://IP_DO_NOTEBOOK:8080
  static const baseUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  final http.Client _client;

  /// Retorna true quando a API e o banco de dados estão respondendo.
  Future<bool> isHealthy() async {
    final response = await _client
        .get(Uri.parse('$baseUrl/api/health'))
        .timeout(const Duration(seconds: 5));
    final body = jsonDecode(response.body) as Map<String, dynamic>;
    return body['database'] == 'UP';
  }
}
