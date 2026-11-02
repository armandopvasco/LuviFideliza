import 'package:local_auth/local_auth.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'biometria.dart';

class BiometriaCelular implements Biometria {
  BiometriaCelular({LocalAuthentication? autenticacao}) : _autenticacao = autenticacao ?? LocalAuthentication();

  final LocalAuthentication _autenticacao;
  static const _chave = 'luvi_biometria';

  Future<bool> _flag() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_chave) ?? false;
  }

  Future<bool> _pedir(String motivo) async {
    try {
      return await _autenticacao.authenticate(
        localizedReason: motivo,
        options: const AuthenticationOptions(biometricOnly: true, stickyAuth: true),
      );
    } catch (_) {
      return false;
    }
  }

  @override
  Future<bool> confirmar() => _pedir('Use a biometria para abrir o aplicativo.');

  @override
  Future<void> desligar() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_chave, false);
  }

  @override
  Future<bool> estaLigada() => _flag();

  @override
  Future<bool> ligar() async {
    if (!await podeUsar()) return false;
    if (!await _pedir('Confirme a biometria para ativar o acesso.')) return false;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_chave, true);
    return true;
  }

  @override
  Future<bool> podeUsar() async {
    try {
      return await _autenticacao.canCheckBiometrics || await _autenticacao.isDeviceSupported();
    } catch (_) {
      return false;
    }
  }
}
