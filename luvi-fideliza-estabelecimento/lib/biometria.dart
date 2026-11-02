abstract class Biometria {
  Future<bool> podeUsar();
  Future<bool> estaLigada();
  Future<bool> ligar();
  Future<void> desligar();
  Future<bool> confirmar();
}

class BiometriaMemoria implements Biometria {
  BiometriaMemoria({this.pode = false, this.ligada = false, this.aceita = true});

  bool pode;
  bool ligada;
  bool aceita;

  @override
  Future<bool> confirmar() async => aceita;

  @override
  Future<void> desligar() async => ligada = false;

  @override
  Future<bool> estaLigada() async => ligada;

  @override
  Future<bool> ligar() async {
    if (!pode || !aceita) return false;
    ligada = true;
    return true;
  }

  @override
  Future<bool> podeUsar() async => pode;
}
