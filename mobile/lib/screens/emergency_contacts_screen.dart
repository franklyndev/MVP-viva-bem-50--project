import 'package:flutter/material.dart';

import '../theme.dart';

class EmergencyContactsScreen extends StatelessWidget {
  const EmergencyContactsScreen({super.key});

  static const contacts = [
    ('Polícia Militar', '190', 'Emergências de segurança pública'),
    ('SAMU — ambulância', '192', 'Urgências e emergências médicas'),
    ('Corpo de Bombeiros', '193', 'Incêndios, acidentes e resgates'),
    ('Polícia Rodoviária Federal', '191', 'Emergências em rodovias federais'),
    ('Polícia Civil', '197', 'Orientações e denúncias'),
    ('Defesa Civil', '199', 'Desastres e áreas de risco'),
    (
      'Central de Atendimento à Mulher',
      '180',
      'Orientação e atendimento à mulher'
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Contatos de emergência')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          const Text(
            'Números úteis no Brasil',
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          const Text(
              'Consulte o serviço adequado. Os números abaixo são apenas informativos.'),
          const SizedBox(height: 16),
          for (final contact in contacts)
            Card(
              child: ListTile(
                leading: const Icon(Icons.support_agent, color: vivaTeal),
                title: Text(contact.$1,
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                subtitle: Text(contact.$3),
                trailing: Text(
                  contact.$2,
                  style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w800,
                      color: vivaTeal),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
