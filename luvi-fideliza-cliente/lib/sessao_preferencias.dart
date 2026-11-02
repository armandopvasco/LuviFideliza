import 'package:shared_preferences/shared_preferences.dart';

import 'api.dart';

class SessaoPreferencias implements Sessao {
  static const _chave = 'luvi_token';

  @override
  Future<void> limpar() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_chave);
  }

  @override
  Future<String?> ler() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_chave);
  }

  @override
  Future<void> salvar(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_chave, token);
  }
}
