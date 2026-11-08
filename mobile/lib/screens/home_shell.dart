import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../services/api_service.dart';
import '../theme.dart';
import 'agenda_screen.dart';
import 'assistente_screen.dart';
import 'inicio_screen.dart';
import 'perfil_screen.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({super.key, required this.api});

  final ApiService api;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int index = 0;

  @override
  Widget build(BuildContext context) {
    final pages = [
      InicioScreen(api: widget.api),
      AgendaScreen(api: widget.api),
      AssistenteScreen(api: widget.api),
      PerfilScreen(api: widget.api),
    ];
    return Scaffold(
      appBar: AppBar(
        title: const Text('VivaBem 50+', style: TextStyle(fontWeight: FontWeight.w800)),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: FilledButton.icon(
              style: FilledButton.styleFrom(backgroundColor: vivaDanger, foregroundColor: Colors.white),
              onPressed: () => launchUrl(Uri.parse('tel:192')),
              icon: const Icon(Icons.phone),
              label: const Text('Emergência'),
            ),
          ),
        ],
      ),
      body: pages[index],
      bottomNavigationBar: NavigationBar(
        selectedIndex: index,
        onDestinationSelected: (value) => setState(() => index = value),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Início'),
          NavigationDestination(icon: Icon(Icons.calendar_month_outlined), selectedIcon: Icon(Icons.calendar_month), label: 'Agenda'),
          NavigationDestination(icon: Icon(Icons.favorite_outline), selectedIcon: Icon(Icons.favorite), label: 'Assistente'),
          NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Perfil'),
        ],
      ),
    );
  }
}
