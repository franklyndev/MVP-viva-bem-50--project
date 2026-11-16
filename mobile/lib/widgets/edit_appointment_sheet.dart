import 'package:flutter/material.dart';

import '../services/api_service.dart';

/// Abre a janela para editar uma consulta, exame ou lembrete. Devolve true se salvou.
Future<bool> showEditAppointmentSheet(BuildContext context, ApiService api, int id) async {
  final saved = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    builder: (_) => _EditAppointmentSheet(api: api, id: id),
  );
  return saved ?? false;
}

class _EditAppointmentSheet extends StatefulWidget {
  const _EditAppointmentSheet({required this.api, required this.id});
  final ApiService api;
  final int id;

  @override
  State<_EditAppointmentSheet> createState() => _EditAppointmentSheetState();
}

class _EditAppointmentSheetState extends State<_EditAppointmentSheet> {
  final title = TextEditingController();
  final location = TextEditingController();
  final notes = TextEditingController();
  String type = 'CONSULTA';
  DateTime day = DateTime.now();
  TimeOfDay time = const TimeOfDay(hour: 10, minute: 0);
  int? duration;
  bool loading = true;
  bool saving = false;
  String? error;

  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  void dispose() {
    title.dispose();
    location.dispose();
    notes.dispose();
    super.dispose();
  }

  Future<void> load() async {
    try {
      final appointment = await widget.api.appointment(widget.id);
      // startsAt vem em UTC; o formulário mostra o dia e a hora locais.
      final starts = DateTime.parse(appointment['startsAt'] as String).toLocal();
      if (!mounted) return;
      setState(() {
        title.text = appointment['title'] as String;
        location.text = (appointment['location'] as String?) ?? '';
        notes.text = (appointment['notes'] as String?) ?? '';
        type = appointment['type'] as String;
        day = DateTime(starts.year, starts.month, starts.day);
        time = TimeOfDay(hour: starts.hour, minute: starts.minute);
        duration = (appointment['durationMinutes'] as num?)?.toInt();
        loading = false;
      });
    } catch (e) {
      if (mounted) {
        setState(() {
          error = e.toString();
          loading = false;
        });
      }
    }
  }

  Future<void> save() async {
    if (saving) return;
    if (title.text.trim().isEmpty) {
      setState(() => error = 'Informe o título do compromisso.');
      return;
    }
    setState(() {
      saving = true;
      error = null;
    });
    try {
      final starts = DateTime(day.year, day.month, day.day, time.hour, time.minute).toUtc();
      await widget.api.updateAppointment(widget.id, {
        'type': type,
        'title': title.text.trim(),
        'location': location.text.trim().isEmpty ? null : location.text.trim(),
        'notes': notes.text.trim().isEmpty ? null : notes.text.trim(),
        'startsAt': starts.toIso8601String(),
        'durationMinutes': duration,
      });
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        setState(() {
          error = e.toString();
          saving = false;
        });
      }
    }
  }

  String dayText() =>
      '${day.day.toString().padLeft(2, '0')}/${day.month.toString().padLeft(2, '0')}/${day.year}';

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
          left: 20, right: 20, top: 20, bottom: MediaQuery.of(context).viewInsets.bottom + 20),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('Editar compromisso', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
            const SizedBox(height: 8),
            if (error != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Text(error!, style: TextStyle(color: Theme.of(context).colorScheme.error, fontWeight: FontWeight.bold)),
              ),
            if (loading)
              const Center(child: Padding(padding: EdgeInsets.all(24), child: CircularProgressIndicator()))
            else ...[
              TextField(controller: title, maxLength: 120, decoration: const InputDecoration(labelText: 'Título')),
              DropdownButtonFormField<String>(
                initialValue: type,
                decoration: const InputDecoration(labelText: 'Tipo'),
                items: const [
                  DropdownMenuItem(value: 'CONSULTA', child: Text('Consulta')),
                  DropdownMenuItem(value: 'EXAME', child: Text('Exame')),
                  DropdownMenuItem(value: 'OUTRO', child: Text('Lembrete')),
                ],
                onChanged: (value) => type = value ?? type,
              ),
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text('Dia · ${dayText()}'),
                trailing: const Icon(Icons.calendar_today),
                onTap: () async {
                  final picked = await showDatePicker(
                    context: context,
                    initialDate: day,
                    firstDate: DateTime(day.year - 1),
                    lastDate: DateTime(day.year + 2),
                  );
                  if (picked != null && mounted) setState(() => day = picked);
                },
              ),
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text('Horário · ${time.format(context)}'),
                trailing: const Icon(Icons.schedule),
                onTap: () async {
                  final picked = await showTimePicker(context: context, initialTime: time);
                  if (picked != null && mounted) setState(() => time = picked);
                },
              ),
              TextField(controller: location, maxLength: 160, decoration: const InputDecoration(labelText: 'Local')),
              TextField(controller: notes, maxLength: 2000, decoration: const InputDecoration(labelText: 'Observação')),
              const SizedBox(height: 8),
              FilledButton(
                style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                onPressed: saving ? null : save,
                child: Text(saving ? 'Salvando…' : 'Salvar alterações'),
              ),
            ],
            const SizedBox(height: 8),
            OutlinedButton(
              style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(52)),
              onPressed: saving ? null : () => Navigator.pop(context, false),
              child: const Text('Cancelar'),
            ),
          ],
        ),
      ),
    );
  }
}
