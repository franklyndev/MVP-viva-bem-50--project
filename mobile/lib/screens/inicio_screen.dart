import 'package:flutter/material.dart';

import '../services/api_service.dart';
import '../theme.dart';
import '../widgets/confirm_delete.dart';
import 'bem_estar_screen.dart';

class InicioScreen extends StatefulWidget {
  const InicioScreen({super.key, required this.api});
  final ApiService api;

  @override
  State<InicioScreen> createState() => _InicioScreenState();
}

class _InicioScreenState extends State<InicioScreen> {
  Map<String, dynamic>? data;
  Map<String, Map<String, dynamic>> agendasByDay = {};
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
      final today = DateTime.now();
      final dates = List.generate(
          7,
          (index) => DateTime(today.year, today.month, today.day)
              .subtract(Duration(days: index)));
      final results = await Future.wait(dates.map((day) async {
        final key = iso(day);
        try {
          return MapEntry(key, await widget.api.agenda(key));
        } catch (_) {
          if (key == iso(today)) rethrow;
          return null;
        }
      }));
      final loaded = <String, Map<String, dynamic>>{
        for (final result in results)
          if (result != null) result.key: result.value,
      };
      if (mounted)
        setState(() {
          agendasByDay = loaded;
          data = loaded[iso(today)];
        });
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  String iso(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

  String dayLabel(DateTime date, DateTime today) {
    const weekdays = [
      'domingo',
      'segunda-feira',
      'terça-feira',
      'quarta-feira',
      'quinta-feira',
      'sexta-feira',
      'sábado'
    ];
    final dateText =
        '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}';
    if (iso(date) == iso(today)) return 'Hoje · $dateText';
    if (iso(date) == iso(today.subtract(const Duration(days: 1))))
      return 'Ontem · $dateText';
    return '${weekdays[date.weekday % 7]} · $dateText';
  }

  Future<void> toggle(Map<String, dynamic> item, bool done, String date) async {
    if (saving || data == null) return;
    setState(() {
      saving = true;
      error = null;
    });
    try {
      await widget.api.complete(
        source: item['source'] as String,
        sourceId: (item['sourceId'] as num).toInt(),
        date: date,
        done: !done,
      );
      await load();
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  Future<void> remove(Map<String, dynamic> item) async {
    if (saving) return;
    if (!await confirmDelete(context, item['title'] as String)) return;
    setState(() {
      saving = true;
      error = null;
    });
    try {
      await widget.api.deleteAppointment((item['sourceId'] as num).toInt());
      await load();
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  bool hasAppointment(Map<String, dynamic>? agenda) =>
      ((agenda?['items'] as List?) ?? [])
          .any((item) => item['source'] == 'APPOINTMENT');

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
              Text(error ?? 'Não foi possível carregar sua rotina.',
                  textAlign: TextAlign.center),
              const SizedBox(height: 12),
              FilledButton(
                  onPressed: load, child: const Text('Tentar novamente')),
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
    final today = DateTime.now();
    final timelineDates = List.generate(
            7,
            (index) => DateTime(today.year, today.month, today.day)
                .subtract(Duration(days: index)))
        .where((day) => hasAppointment(agendasByDay[iso(day)]))
        .toList();

    return RefreshIndicator(
      onRefresh: load,
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          if (error != null)
            Text(error!,
                style: const TextStyle(
                    color: vivaDanger, fontWeight: FontWeight.bold)),
          Text('${greeting()}, $first!',
              style:
                  const TextStyle(fontSize: 28, fontWeight: FontWeight.w800)),
          Text(
              'Você tem $pending ${pending == 1 ? 'tarefa' : 'tarefas'} para hoje.'),
          const SizedBox(height: 20),
          const Text('Resumo do dia',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
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
                  decoration: BoxDecoration(
                      color: vivaTeal, borderRadius: BorderRadius.circular(16)),
                  child: Text(
                    next == null
                        ? 'Nenhuma consulta hoje\nAgenda livre'
                        : 'Próxima consulta · ${next['time']}\n${next['title']}\n${next['detail'] ?? ''}',
                    style: const TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.w600),
                  ),
                ),
                InkWell(
                  onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (_) => BemEstarScreen(api: widget.api))),
                  child: Container(
                    width: 220,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(16)),
                    child: Text(
                        'Hidratação\n${(wellbeing['waterLiters'] as num).toStringAsFixed(2).replaceAll('.', ',')} L hoje\nVer bem-estar'),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          const Text('Linha do tempo',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
          const Text('Acompanhe seus compromissos e atividades dia a dia.'),
          const SizedBox(height: 8),
          if (timelineDates.isEmpty)
            const Card(
              child: ListTile(
                  title: Text('Nenhum lembrete ou consulta nos últimos 7 dias.')),
            ),
          ...timelineDates.map((day) {
            final date = iso(day);
            final dayItems = (agendasByDay[date]!['items'] as List)
                .cast<Map<String, dynamic>>();
            return Card(
              clipBehavior: Clip.antiAlias,
              child: ExpansionTile(
                initiallyExpanded: day == timelineDates.first,
                leading: Icon(Icons.event_note,
                    color: date == iso(today) ? vivaTeal : Colors.black54),
                title: Text(dayLabel(day, today),
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                subtitle: Text(
                    '${dayItems.length} ${dayItems.length == 1 ? 'item' : 'itens'}'),
                children: [
                  ...dayItems.map((item) {
                      final done = item['done'] == true;
                      final detail = (item['detail'] as String?) ?? '';
                      return ListTile(
                        leading: Text(item['time'] as String,
                            style: const TextStyle(
                                fontWeight: FontWeight.w800, fontSize: 16)),
                        title: Text(item['title'] as String,
                            style: TextStyle(
                                decoration:
                                    done ? TextDecoration.lineThrough : null)),
                        subtitle: Text(detail.isEmpty
                            ? item['kind'] as String
                            : '${item['kind']} · $detail'),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            FilledButton(
                              style: FilledButton.styleFrom(
                                  backgroundColor:
                                      done ? vivaSuccess : Colors.white,
                                  foregroundColor:
                                      done ? Colors.white : vivaSuccess,
                                  side: const BorderSide(
                                      color: vivaSuccess, width: 2)),
                              onPressed: saving
                                  ? null
                                  : () => toggle(item, done, date),
                              child: Text(saving
                                  ? 'Salvando…'
                                  : done
                                      ? 'Feito'
                                      : 'Concluir'),
                            ),
                            if (item['source'] == 'APPOINTMENT')
                              IconButton(
                                tooltip: 'Excluir',
                                iconSize: 28,
                                color: vivaDanger,
                                onPressed: saving ? null : () => remove(item),
                                icon: const Icon(Icons.delete_outline),
                              ),
                          ],
                        ),
                      );
                    }),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }
}
