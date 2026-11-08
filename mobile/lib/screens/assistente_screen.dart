import 'package:flutter/material.dart';

import '../services/api_service.dart';
import '../theme.dart';

class AssistenteScreen extends StatefulWidget {
  const AssistenteScreen({super.key, required this.api});
  final ApiService api;

  @override
  State<AssistenteScreen> createState() => _AssistenteScreenState();
}

class _AssistenteScreenState extends State<AssistenteScreen> {
  final text = TextEditingController();
  final msgs = <({bool me, String text})>[];
  bool busy = false;

  static const chips = ['Agendar consulta', 'Dica de alimentação', 'Lembrar remédio', 'Exercício leve'];

  @override
  void dispose() {
    text.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    widget.api.storedUser().then((user) {
      final name = (user?['name'] as String? ?? '').split(' ').first;
      if (mounted) {
        setState(() => msgs.add((me: false, text: 'Olá, $name! Sou a Vivi, sua assistente. Como posso ajudar hoje?')));
      }
    });
  }

  Future<void> send(String value) async {
    final message = value.trim();
    if (message.isEmpty || busy) return;
    if (message.length > 500) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('A mensagem pode ter até 500 caracteres.')));
      return;
    }
    setState(() {
      msgs.add((me: true, text: message));
      busy = true;
      text.clear();
    });
    try {
      final reply = await widget.api.chat(message);
      if (mounted) setState(() => msgs.add((me: false, text: reply)));
    } catch (e) {
      if (mounted) setState(() => msgs.add((me: false, text: e.toString())));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const Text('Assistente Vivi', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800)),
        const SizedBox(height: 12),
        ...msgs.map((m) => Align(
              alignment: m.me ? Alignment.centerRight : Alignment.centerLeft,
              child: Container(
                margin: const EdgeInsets.only(bottom: 10),
                padding: const EdgeInsets.all(14),
                constraints: const BoxConstraints(maxWidth: 320),
                decoration: BoxDecoration(
                  color: m.me ? vivaTeal : Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Text(m.text, style: TextStyle(color: m.me ? Colors.white : Colors.black87, fontSize: 16)),
              ),
            )),
        Wrap(
          spacing: 8,
          children: chips.map((c) => ActionChip(label: Text(c), onPressed: busy ? null : () => send(c))).toList(),
        ),
        if (busy) const Padding(padding: EdgeInsets.only(top: 8), child: Text('Vivi está pensando…')),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(child: TextField(controller: text, maxLength: 500, enabled: !busy, onSubmitted: send, decoration: const InputDecoration(hintText: 'Ou escreva aqui…'))),
            IconButton.filled(onPressed: busy ? null : () => send(text.text), icon: const Icon(Icons.send), style: IconButton.styleFrom(backgroundColor: vivaTeal)),
          ],
        ),
      ],
    );
  }
}
