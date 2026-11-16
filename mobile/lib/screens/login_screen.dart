import 'package:flutter/material.dart';

import '../services/api_service.dart';
import '../theme.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, required this.api});

  final ApiService api;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final email = TextEditingController();
  final password = TextEditingController();
  final name = TextEditingController();
  final city = TextEditingController();
  DateTime? birth;
  bool register = true;
  bool consent = false;
  bool loading = false;
  bool checkingSession = true;
  String? error;

  @override
  void initState() {
    super.initState();
    restoreSession();
  }

  Future<void> restoreSession() async {
    try {
      final user = await widget.api.storedUser();
      if (user != null && mounted) {
        Navigator.pushReplacementNamed(context, '/home');
        return;
      }
    } catch (_) {
      await widget.api.signOut();
    }
    if (mounted) setState(() => checkingSession = false);
  }

  @override
  void dispose() {
    email.dispose();
    password.dispose();
    name.dispose();
    city.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      if (register) {
        if (name.text.trim().isEmpty) {
          throw ApiException('Informe seu nome.');
        }
        if (birth == null) {
          throw ApiException('Informe a data de nascimento.');
        }
        if (password.text.length < 8 || password.text.length > 72) {
          throw ApiException('A senha precisa ter entre 8 e 72 caracteres.');
        }
        if (!consent) {
          throw ApiException('Autorize o uso dos dados para criar sua conta.');
        }
        await widget.api.register({
          'name': name.text.trim(),
          'email': email.text.trim(),
          'password': password.text,
          'birthDate': birth!.toIso8601String().split('T').first,
          'city': city.text.trim(),
          'consent': consent,
        });
      } else {
        await widget.api.login(email.text.trim(), password.text);
      }
      if (mounted) Navigator.pushReplacementNamed(context, '/home');
    } catch (e) {
      setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (checkingSession) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return Scaffold(
      body: ListView(
        padding: EdgeInsets.zero,
        children: [
          Container(
            color: vivaTeal,
            padding: const EdgeInsets.fromLTRB(24, 72, 24, 48),
            child: const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('VivaBem 50+', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w600)),
                SizedBox(height: 12),
                Text('Olá! Que bom ter você aqui.', style: TextStyle(color: Colors.white, fontSize: 32, fontWeight: FontWeight.w800)),
                SizedBox(height: 8),
                Text('Sua saúde e sua rotina, organizadas com carinho.', style: TextStyle(color: Colors.white, fontSize: 18)),
              ],
            ),
          ),
          Transform.translate(
            offset: const Offset(0, -24),
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      if (register) ...[
                        TextField(controller: name, maxLength: 120, decoration: const InputDecoration(labelText: 'Nome')),
                        const SizedBox(height: 12),
                        ListTile(
                          contentPadding: EdgeInsets.zero,
                          title: Text(birth == null ? 'Data de nascimento' : birth!.toIso8601String().split('T').first),
                          trailing: const Icon(Icons.calendar_month),
                          onTap: () async {
                            final picked = await showDatePicker(
                              context: context,
                              firstDate: DateTime(1900),
                              lastDate: DateTime.now(),
                              initialDate: DateTime.now().subtract(const Duration(days: 365 * 50)),
                            );
                            if (picked != null) setState(() => birth = picked);
                          },
                        ),
                        TextField(controller: city, maxLength: 80, decoration: const InputDecoration(labelText: 'Cidade')),
                        CheckboxListTile(
                          contentPadding: EdgeInsets.zero,
                          value: consent,
                          onChanged: loading ? null : (value) => setState(() => consent = value ?? false),
                          title: const Text('Concordo com o uso dos meus dados para oferecer os recursos de saúde do VivaBem 50+.'),
                          controlAffinity: ListTileControlAffinity.leading,
                        ),
                        const SizedBox(height: 12),
                      ],
                      TextField(controller: email, keyboardType: TextInputType.emailAddress, maxLength: 254, decoration: const InputDecoration(labelText: 'E-mail')),
                      const SizedBox(height: 12),
                      TextField(controller: password, obscureText: true, maxLength: 72, decoration: const InputDecoration(labelText: 'Senha')),
                      if (error != null) ...[
                        const SizedBox(height: 12),
                        Text(error!, style: const TextStyle(color: vivaDanger, fontWeight: FontWeight.bold)),
                      ],
                      const SizedBox(height: 16),
                      FilledButton(
                        onPressed: loading ? null : submit,
                        style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52), backgroundColor: vivaTeal),
                        child: Text(loading ? 'Aguarde…' : register ? 'Criar conta' : 'Entrar'),
                      ),
                      const SizedBox(height: 8),
                      OutlinedButton(
                        onPressed: loading ? null : () => setState(() {
                          register = !register;
                          consent = false;
                          error = null;
                        }),
                        style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                        child: Text(register ? 'Já tenho conta' : 'Quero me cadastrar'),
                      ),
                      const SizedBox(height: 12),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
