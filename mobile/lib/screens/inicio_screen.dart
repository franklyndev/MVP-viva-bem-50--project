import 'package:flutter/material.dart';

import '../services/api_service.dart';
import '../theme.dart';
import 'bem_estar_screen.dart';

class InicioScreen extends StatefulWidget {
  const InicioScreen({super.key, required this.api});
  final ApiService api;

  @override
  State<InicioScreen> createState() => _InicioScreenState();
}

class _InicioScreenState extends State<InicioScreen> {
  Map<String, dynamic>? data;
  String? error;
  bool loading = true;
  bool saving = false;

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    if (mounted) {
      setState(() {
        loading = true;
        error = null;
      });
    }
    try {
      final agenda = await widget.api.agenda();
      if (mounted) setState(() => data = agenda);
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> toggle(Map<String, dynamic> item, bool done) async {
    if (saving || data == null) return;
    setState(() {
      saving = true;
      error = null;
    });
    try {
      await widget.api.complete(
        source: item['source'] as String,
        sourceId: (item['sourceId'] as num).toInt(),
        date: data!['date'] as String,
        done: !done,
      );
      await load();
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  String greeting() {
    final h = DateTime.now().hour;
    if (h < 12) return 'Bom dia';
    if (h < 18) return 'Boa tarde';
    return 'Boa noite';
  }

  @override
  Widget build(BuildContext context) {
    if (data == null) {
      if (loading) return const Center(child: CircularProgressIndicator());
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(error ?? 'Não foi possível carregar sua rotina.', textAlign: TextAlign.center),
              const SizedBox(height: 12),
              FilledButton(onPressed: load, child: const Text('Tentar novamente')),
            ],
          ),
        ),
      );
    }
    final user = data!['user'] as Map<String, dynamic>;
    final items = (data!['items'] as List).cast<Map<String, dynamic>>();
    final wellbeing = data!['wellbeing'] as Map<String, dynamic>;
    final next = data!['nextAppointment'] as Map<String, dynamic>?;
    final pending = items.where((item) => item['done'] != true).length;
    final first = (user['name'] as String).split(' ').first;

    return RefreshIndicator(
      onRefresh: load,
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          if (error != null) Text(error!, style: const TextStyle(color: vivaDanger, fontWeight: FontWeight.bold)),
          Text('${greeting()}, $first!', style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w800)),
          Text('Você tem $pending ${pending == 1 ? 'tarefa' : 'tarefas'} para hoje.'),
          const SizedBox(height: 20),
          const Text('Resumo do dia', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          SizedBox(
            height: 140,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                Container(
                  width: 260,
                  margin: const EdgeInsets.only(right: 12),
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(color: vivaTeal, borderRadius: BorderRadius.circular(16)),
                  child: Text(
                    next == null
                        ? 'Nenhuma consulta hoje\nAgenda livre'
                        : 'Próxima consulta · ${next['time']}\n${next['title']}\n${next['detail'] ?? ''}',
                    style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w600),
                  ),
                ),
                InkWell(
                  onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => BemEstarScreen(api: widget.api))),
                  child: Container(
                    width: 220,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
                    child: Text('Hidratação\n${wellbeing['waterCups']} de ${wellbeing['waterGoal']} copos\nVer bem-estar'),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          const Text('Linha do tempo', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          if (items.isEmpty) const Text('Nada programado para hoje.'),
          ...items.map((item) {
            final done = item['done'] == true;
            return Card(
              child: ListTile(
                leading: Text(item['time'] as String, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 18)),
                title: Text(item['title'] as String, style: TextStyle(decoration: done ? TextDecoration.lineThrough : null)),
                subtitle: Text('${item['kind']} · ${item['detail'] ?? ''}'),
                trailing: FilledButton(
                  style: FilledButton.styleFrom(backgroundColor: done ? vivaSuccess : Colors.white, foregroundColor: done ? Colors.white : vivaSuccess, side: const BorderSide(color: vivaSuccess, width: 2)),
                  onPressed: saving ? null : () => toggle(item, done),
                  child: Text(saving ? 'Salvando…' : done ? 'Feito' : 'Concluir'),
                ),
              ),
            );
          }),
        ],
      ),
    );
  }
}
