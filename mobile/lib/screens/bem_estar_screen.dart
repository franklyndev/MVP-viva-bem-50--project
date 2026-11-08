import 'package:flutter/material.dart';

import '../services/api_service.dart';
import '../theme.dart';

class BemEstarScreen extends StatefulWidget {
  const BemEstarScreen({super.key, required this.api});
  final ApiService api;

  @override
  State<BemEstarScreen> createState() => _BemEstarScreenState();
}

class _BemEstarScreenState extends State<BemEstarScreen> {
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
      final value = await widget.api.wellbeing();
      if (mounted) setState(() => data = value);
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> save(Map<String, dynamic> patch) async {
    if (saving || data == null) return;
    setState(() {
      saving = true;
      error = null;
    });
    try {
      final value = await widget.api.updateWellbeing({'date': data!['date'], ...patch});
      if (mounted) setState(() => data = value);
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (data == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Bem-Estar')),
        body: Center(
          child: loading
              ? const CircularProgressIndicator()
              : Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(error ?? 'Não foi possível carregar seus hábitos.'),
                    const SizedBox(height: 12),
                    FilledButton(onPressed: load, child: const Text('Tentar novamente')),
                  ],
                ),
        ),
      );
    }
    final water = data!['waterCups'] as int;
    final waterGoal = data!['waterGoal'] as int;
    final minutes = data!['activityMinutes'] as int;
    final minuteGoal = data!['activityGoal'] as int;
    final mood = data!['mood'] as String?;
    return Scaffold(
      appBar: AppBar(title: const Text('Bem-Estar e Hábitos')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          if (error != null) Text(error!, style: const TextStyle(color: vivaDanger, fontWeight: FontWeight.bold)),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Hidratação', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 20)),
                  Text('$water de $waterGoal copos'),
                  LinearProgressIndicator(value: (water / waterGoal).clamp(0, 1).toDouble(), color: vivaSuccess),
                  const SizedBox(height: 8),
                  FilledButton(onPressed: saving || water >= 30 ? null : () => save({'waterCups': water + 1}), child: Text(saving ? 'Salvando…' : '+ Bebi um copo')),
                ],
              ),
            ),
          ),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Atividade física', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 20)),
                  Text('$minutes de $minuteGoal minutos'),
                  LinearProgressIndicator(value: (minutes / minuteGoal).clamp(0, 1).toDouble(), color: vivaSuccess),
                  Row(
                    children: [
                      IconButton(onPressed: saving || minutes <= 0 ? null : () => save({'activityMinutes': (minutes - 5).clamp(0, 600)}), icon: const Icon(Icons.remove)),
                      IconButton(onPressed: saving || minutes >= 600 ? null : () => save({'activityMinutes': (minutes + 5).clamp(0, 600)}), icon: const Icon(Icons.add)),
                    ],
                  ),
                ],
              ),
            ),
          ),
          Wrap(
            spacing: 8,
            children: [
              for (final entry in {'OTIMO': 'Ótimo', 'BEM': 'Bem', 'NORMAL': 'Normal', 'TRISTE': 'Triste'}.entries)
                ChoiceChip(
                  label: Text(entry.value),
                  selected: mood == entry.key,
                  onSelected: saving ? null : (_) => save({'mood': entry.key}),
                ),
            ],
          ),
        ],
      ),
    );
  }
}
