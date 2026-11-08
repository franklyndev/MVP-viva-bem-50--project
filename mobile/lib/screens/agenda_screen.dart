import 'package:flutter/material.dart';

import '../services/api_service.dart';
import '../theme.dart';

class AgendaScreen extends StatefulWidget {
  const AgendaScreen({super.key, required this.api});
  final ApiService api;

  @override
  State<AgendaScreen> createState() => _AgendaScreenState();
}

class _AgendaScreenState extends State<AgendaScreen> {
  late DateTime selected;
  Map<String, dynamic>? data;
  String? error;
  bool loading = true;
  bool saving = false;
  final title = TextEditingController();
  final location = TextEditingController();
  TimeOfDay time = const TimeOfDay(hour: 10, minute: 0);
  String type = 'CONSULTA';

  @override
  void initState() {
    super.initState();
    selected = DateTime.now();
    load();
  }

  @override
  void dispose() {
    title.dispose();
    location.dispose();
    super.dispose();
  }

  String iso(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

  Future<void> load() async {
    if (mounted) {
      setState(() {
        loading = true;
        error = null;
      });
    }
    try {
      final agenda = await widget.api.agenda(iso(selected));
      if (mounted) setState(() => data = agenda);
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  List<DateTime> get week {
    final start = selected.subtract(Duration(days: selected.weekday % 7));
    return List.generate(7, (i) => start.add(Duration(days: i)));
  }

  Future<void> add() async {
    if (saving) return;
    if (title.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Informe o título do compromisso.')));
      return;
    }
    setState(() => saving = true);
    try {
      final starts = DateTime(selected.year, selected.month, selected.day, time.hour, time.minute).toUtc();
      await widget.api.createAppointment({
        'type': type,
        'title': title.text.trim(),
        'location': location.text.trim().isEmpty ? null : location.text.trim(),
        'startsAt': starts.toIso8601String(),
        'durationMinutes': 30,
      });
      title.clear();
      location.clear();
      if (mounted) Navigator.pop(context);
      await load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    const dias = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
    final items = (data?['items'] as List?)?.cast<Map<String, dynamic>>() ?? [];
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const Text('Minha Agenda', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800)),
        Row(
          children: week.map((day) {
            final on = iso(day) == iso(selected);
            return Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 2),
                child: FilledButton(
                  style: FilledButton.styleFrom(
                    backgroundColor: on ? vivaTeal : Colors.white,
                    foregroundColor: on ? Colors.white : Colors.black87,
                    padding: const EdgeInsets.symmetric(vertical: 12),
                  ),
                  onPressed: () {
                    setState(() => selected = day);
                    load();
                  },
                  child: Text('${dias[day.weekday % 7]}\n${day.day}', textAlign: TextAlign.center),
                ),
              ),
            );
          }).toList(),
        ),
        const SizedBox(height: 16),
        if (error != null) ...[
          Text(error!, style: const TextStyle(color: vivaDanger, fontWeight: FontWeight.bold)),
          TextButton(onPressed: load, child: const Text('Tentar novamente')),
        ],
        if (loading) const Center(child: Padding(padding: EdgeInsets.all(20), child: CircularProgressIndicator())),
        if (!loading) ...items.map((item) => Card(
              child: ListTile(
                leading: Text(item['time'] as String, style: const TextStyle(color: vivaTeal, fontWeight: FontWeight.w800)),
                title: Text(item['title'] as String),
                subtitle: Text('${item['kind']} · ${item['detail'] ?? ''}'),
              ),
            )),
        const SizedBox(height: 16),
        FilledButton.icon(
          style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52), backgroundColor: vivaAccent),
          onPressed: saving ? null : () {
            showModalBottomSheet(
              context: context,
              isScrollControlled: true,
              builder: (sheetContext) => StatefulBuilder(
                builder: (sheetContext, setSheetState) => Padding(
                  padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: MediaQuery.of(sheetContext).viewInsets.bottom + 20),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      TextField(controller: title, maxLength: 120, decoration: const InputDecoration(labelText: 'Título')),
                      DropdownButtonFormField<String>(
                        initialValue: type,
                        items: const [
                          DropdownMenuItem(value: 'CONSULTA', child: Text('Consulta')),
                          DropdownMenuItem(value: 'EXAME', child: Text('Exame')),
                          DropdownMenuItem(value: 'OUTRO', child: Text('Lembrete')),
                        ],
                        onChanged: (value) => type = value ?? 'CONSULTA',
                      ),
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        title: Text('Horário · ${time.format(sheetContext)}'),
                        trailing: const Icon(Icons.schedule),
                        onTap: () async {
                          final picked = await showTimePicker(context: sheetContext, initialTime: time);
                          if (picked != null && sheetContext.mounted) {
                            setState(() => time = picked);
                            setSheetState(() {});
                          }
                        },
                      ),
                      TextField(controller: location, maxLength: 160, decoration: const InputDecoration(labelText: 'Local')),
                      const SizedBox(height: 8),
                      FilledButton(onPressed: saving ? null : add, child: Text(saving ? 'Salvando…' : 'Salvar no banco')),
                    ],
                  ),
                ),
              ),
            );
          },
          icon: const Icon(Icons.add),
          label: const Text('Adicionar lembrete'),
        ),
      ],
    );
  }
}
