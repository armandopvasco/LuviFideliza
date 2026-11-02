import 'package:flutter/widgets.dart';

import 'app.dart';
import 'http_api.dart';
import 'sessao_preferencias.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  const base = String.fromEnvironment('API_BASE', defaultValue: 'http://localhost:8081');
  runApp(LuviApp(api: LuviHttp(base: base, sessao: SessaoPreferencias())));
}
