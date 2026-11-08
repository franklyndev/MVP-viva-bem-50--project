import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../services/api_service.dart';
import '../theme.dart';
import 'bem_estar_screen.dart';

class PerfilScreen extends StatefulWidget {
  const PerfilScreen({super.key, required this.api});
  final ApiService api;

  @override
  State<PerfilScreen> createState() => _PerfilScreenState();
}

class _PerfilScreenState extends State<PerfilScreen> {
  Map<String, dynamic>? user;

  @override
  void initState() {
    super.initState();
    widget.api.storedUser().then((value) {
      if (mounted) setState(() => user = value);
    });
  }

  @override
  Widget build(BuildContext context) {
    final name = user?['name'] as String? ?? '';
    final age = user?['age'];
    final city = user?['city'];
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        Row(
          children: [
            CircleAvatar(
              radius: 40,
              backgroundColor: vivaTeal,
              child: Text(name.isEmpty ? '?' : name.substring(0, 1).toUpperCase(), style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w800)),
            ),
            const SizedBox(width: 16),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(name, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w800)),
                Text('$age anos${city == null ? '' : ' · $city'}'),
              ],
            ),
          ],
        ),
        const SizedBox(height: 20),
        ListTile(
          leading: const Icon(Icons.favorite, color: vivaTeal),
          title: const Text('Bem-Estar e Hábitos'),
          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => BemEstarScreen(api: widget.api))),
        ),
        ListTile(
          leading: const Icon(Icons.groups, color: vivaTeal),
          title: const Text('Contatos de emergência'),
          onTap: () => launchUrl(Uri.parse('tel:192')),
        ),
        const ListTile(
          leading: Icon(Icons.text_fields, color: vivaTeal),
          title: Text('Texto grande já está ativo'),
        ),
        ListTile(
          leading: const Icon(Icons.logout, color: vivaDanger),
          title: const Text('Sair', style: TextStyle(color: vivaDanger, fontWeight: FontWeight.bold)),
          onTap: () async {
            await widget.api.signOut();
            if (context.mounted) Navigator.pushNamedAndRemoveUntil(context, '/', (_) => false);
          },
        ),
      ],
    );
  }
}
