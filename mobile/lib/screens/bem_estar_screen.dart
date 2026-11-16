import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

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
  final waterController = TextEditingController();
  final activityGoalController = TextEditingController();

  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  void dispose() {
    waterController.dispose();
    activityGoalController.dispose();
    super.dispose();
  }

  String formatLiters(double value) => value
      .toStringAsFixed(2)
      .replaceFirst(RegExp(r'\.?0+$'), '')
      .replaceAll('.', ',');

  void syncInputs(Map<String, dynamic> value,
      {bool water = true, bool activityGoal = true}) {
    final liters = (value['waterLiters'] as num?)?.toDouble() ?? 0;
    if (water) waterController.text = formatLiters(liters);
    if (activityGoal)
      activityGoalController.text = '${value['activityGoal'] ?? 30}';
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
      if (mounted) {
        setState(() => data = value);
        syncInputs(value);
      }
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
      final value =
          await widget.api.updateWellbeing({'date': data!['date'], ...patch});
      if (mounted) {
        setState(() => data = value);
        syncInputs(
          value,
          water: patch.containsKey('waterLiters') ||
              patch.containsKey('waterCups'),
          activityGoal: patch.containsKey('activityGoal'),
        );
      }
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  Future<void> saveWaterInput() async {
    final liters =
        double.tryParse(waterController.text.trim().replaceAll(',', '.'));
    if (liters == null || liters < 0 || liters > 20) {
      setState(() => error = 'Informe um valor entre 0 e 20 litros.');
      return;
    }
    await save({'waterLiters': double.parse(liters.toStringAsFixed(2))});
  }

  Future<void> saveActivityGoal() async {
    final goal = int.tryParse(activityGoalController.text.trim());
    if (goal == null || goal < 1 || goal > 600) {
      setState(() => error = 'Informe uma meta entre 1 e 600 minutos.');
      return;
    }
    await save({'activityGoal': goal});
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
                    FilledButton(
                        onPressed: load, child: const Text('Tentar novamente')),
                  ],
                ),
        ),
      );
    }
    final water = (data!['waterLiters'] as num).toDouble();
    final waterGoal = (data!['waterGoalLiters'] as num).toDouble();
    final minutes = data!['activityMinutes'] as int;
    final minuteGoal = data!['activityGoal'] as int;
    final mood = data!['mood'] as String?;
    return Scaffold(
      appBar: AppBar(title: const Text('Bem-Estar e Hábitos')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          if (error != null)
            Text(error!,
                style: const TextStyle(
                    color: vivaDanger, fontWeight: FontWeight.bold)),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Hidratação',
                      style:
                          TextStyle(fontWeight: FontWeight.w700, fontSize: 20)),
                  Text(
                      '${formatLiters(water)} de ${formatLiters(waterGoal)} L hoje'),
                  const SizedBox(height: 4),
                  LinearProgressIndicator(
                      value: (water / waterGoal).clamp(0, 1).toDouble(),
                      color: vivaSuccess),
                  const SizedBox(height: 12),
                  TextField(
                    controller: waterController,
                    enabled: !saving,
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[0-9,.]'))
                    ],
                    decoration: const InputDecoration(
                        labelText: 'Litros tomados hoje', suffixText: 'L'),
                  ),
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: saving ? null : saveWaterInput,
                      child: Text(saving ? 'Salvando…' : 'Salvar hidratação'),
                    ),
                  ),
                  Row(
                    children: [
                      IconButton(
                        tooltip: 'Diminuir 0,25 litro',
                        onPressed: saving || water <= 0
                            ? null
                            : () => save({
                                  'waterLiters':
                                      (water - 0.25).clamp(0, 20).toDouble()
                                }),
                        icon: const Icon(Icons.remove),
                      ),
                      const Text('Ajustar em 0,25 L'),
                      IconButton(
                        tooltip: 'Adicionar 0,25 litro',
                        onPressed: saving || water >= 20
                            ? null
                            : () => save({
                                  'waterLiters':
                                      (water + 0.25).clamp(0, 20).toDouble()
                                }),
                        icon: const Icon(Icons.add),
                      ),
                    ],
                  ),
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
                  const Text('Atividade física',
                      style:
                          TextStyle(fontWeight: FontWeight.w700, fontSize: 20)),
                  Text('$minutes de $minuteGoal minutos'),
                  const SizedBox(height: 4),
                  LinearProgressIndicator(
                      value: (minutes / minuteGoal).clamp(0, 1).toDouble(),
                      color: vivaSuccess),
                  TextField(
                    controller: activityGoalController,
                    enabled: !saving,
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    decoration: const InputDecoration(
                        labelText: 'Sua meta diária', suffixText: 'minutos'),
                  ),
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton(
                      onPressed: saving ? null : saveActivityGoal,
                      child: const Text('Salvar meta de atividade'),
                    ),
                  ),
                  Row(
                    children: [
                      IconButton(
                        tooltip: 'Diminuir 5 minutos realizados',
                        onPressed: saving || minutes <= 0
                            ? null
                            : () => save({
                                  'activityMinutes': (minutes - 5).clamp(0, 600)
                                }),
                        icon: const Icon(Icons.remove),
                      ),
                      const Text('Minutos realizados'),
                      IconButton(
                        tooltip: 'Adicionar 5 minutos realizados',
                        onPressed: saving || minutes >= 600
                            ? null
                            : () => save({
                                  'activityMinutes': (minutes + 5).clamp(0, 600)
                                }),
                        icon: const Icon(Icons.add),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          Wrap(
            spacing: 8,
            children: [
              for (final entry in {
                'OTIMO': 'Ótimo',
                'BEM': 'Bem',
                'NORMAL': 'Normal',
                'TRISTE': 'Triste'
              }.entries)
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
