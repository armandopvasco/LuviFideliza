import 'package:shared_preferences/shared_preferences.dart';

import 'api.dart';

class SessaoPreferencias implements Sessao {
  static const _chave = 'luvi_balcao_token';
  static const _nome = 'luvi_balcao_nome';

  @override
  Future<void> limpar() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_chave);
    await prefs.remove(_nome);
  }

  @override
  Future<String?> ler() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_chave);
  }

  @override
  Future<String?> lerNome() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_nome);
  }

  @override
  Future<void> salvar(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_chave, token);
  }

  @override
  Future<void> salvarNome(String nome) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_nome, nome);
  }
}
