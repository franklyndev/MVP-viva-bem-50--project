import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiException implements Exception {
  ApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

class ApiService {
  ApiService({http.Client? client, this.onSessionExpired}) : _client = client ?? http.Client();

  static const _configuredBaseUrl = String.fromEnvironment('API_URL');
  static String get baseUrl {
    if (_configuredBaseUrl.isNotEmpty) return _configuredBaseUrl.replaceFirst(RegExp(r'/$'), '');
    if (kIsWeb || defaultTargetPlatform != TargetPlatform.android) return 'http://localhost:8080';
    return 'http://10.0.2.2:8080';
  }

  static const _tokenKey = 'vivabem_token';
  static const _userKey = 'vivabem_user';

  final http.Client _client;
  final void Function()? onSessionExpired;

  Future<bool> isHealthy() async {
    try {
      final response = await _client
          .get(Uri.parse('$baseUrl/api/health'))
          .timeout(const Duration(seconds: 5));
      final body = jsonDecode(response.body);
      return body is Map<String, dynamic> && body['database'] == 'UP';
    } catch (_) {
      return false;
    }
  }

  Future<Map<String, dynamic>?> storedUser() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_userKey);
    if (raw == null || prefs.getString(_tokenKey) == null) {
      await signOut();
      return null;
    }
    try {
      final user = jsonDecode(raw);
      if (user is Map<String, dynamic>) return user;
    } on FormatException {
      // Remove an invalid cached session below.
    }
    await signOut();
    return null;
  }

  Future<void> signOut() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_userKey);
  }

  Future<Map<String, dynamic>> login(String email, String password) {
    return _auth('/api/auth/login', {'email': email, 'password': password});
  }

  Future<Map<String, dynamic>> register(Map<String, dynamic> payload) {
    return _auth('/api/auth/register', payload);
  }

  Future<Map<String, dynamic>> agenda([String? date]) {
    final query = date == null ? '' : '?date=$date';
    return _json('GET', '/api/agenda$query');
  }

  Future<void> complete({
    required String source,
    required int sourceId,
    required String date,
    required bool done,
  }) async {
    await _json('POST', '/api/agenda/complete', payload: {
      'source': source,
      'sourceId': sourceId,
      'date': date,
      'done': done,
    });
  }

  Future<Map<String, dynamic>> createAppointment(Map<String, dynamic> payload) {
    return _json('POST', '/api/appointments', payload: payload);
  }

  Future<Map<String, dynamic>> wellbeing([String? date]) {
    final query = date == null ? '' : '?date=$date';
    return _json('GET', '/api/wellbeing$query');
  }

  Future<Map<String, dynamic>> updateWellbeing(Map<String, dynamic> payload) {
    return _json('PUT', '/api/wellbeing', payload: payload);
  }

  Future<String> chat(String message) async {
    final body = await _json('POST', '/api/ai/chat', payload: {'message': message});
    return body['reply'] as String;
  }

  Future<Map<String, dynamic>> _auth(String path, Map<String, dynamic> payload) async {
    final body = await _json('POST', path, payload: payload, auth: false);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, body['token'] as String);
    await prefs.setString(_userKey, jsonEncode(body['user']));
    return body;
  }

  Future<Map<String, dynamic>> _json(
    String method,
    String path, {
    Map<String, dynamic>? payload,
    bool auth = true,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    final headers = <String, String>{};
    if (payload != null) headers['Content-Type'] = 'application/json';
    if (auth) {
      final token = prefs.getString(_tokenKey);
      if (token != null) headers['Authorization'] = 'Bearer $token';
    }
    final uri = Uri.parse('$baseUrl$path');
    late http.Response response;
    try {
      switch (method) {
        case 'POST':
          response = await _client
              .post(uri, headers: headers, body: jsonEncode(payload))
              .timeout(const Duration(seconds: 15));
          break;
        case 'PUT':
          response = await _client
              .put(uri, headers: headers, body: jsonEncode(payload))
              .timeout(const Duration(seconds: 15));
          break;
        default:
          response = await _client
              .get(uri, headers: headers)
              .timeout(const Duration(seconds: 15));
      }
    } on TimeoutException {
      throw ApiException('A API demorou para responder. Tente novamente.');
    } catch (_) {
      throw ApiException('Não foi possível conectar à API. Verifique se o serviço está ativo.');
    }
    if (response.statusCode == 204) return {};
    if (response.statusCode == 401) {
      if (auth) {
        await signOut();
        onSessionExpired?.call();
      }
      throw ApiException(auth ? 'Sua sessão expirou. Entre novamente.' : 'E-mail ou senha incorretos.');
    }

    dynamic decoded = <String, dynamic>{};
    if (response.body.isNotEmpty) {
      try {
        decoded = jsonDecode(response.body);
      } on FormatException {
        decoded = {'detail': response.body};
      }
    }
    if (response.statusCode >= 400) {
      final detail = decoded is Map ? (decoded['detail'] ?? decoded['message']) : null;
      throw ApiException(detail?.toString() ?? 'A API respondeu ${response.statusCode}');
    }
    if (decoded is Map<String, dynamic>) return decoded;
    return {'data': decoded};
  }
}
