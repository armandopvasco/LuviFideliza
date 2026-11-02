# Aplicativo do cliente

Carteira LUVI Fideliza+. O cliente entra com WhatsApp ou e-mail, adere a uma loja pelo QR do balcão e mostra o QR pessoal para receber o carimbo. A recompensa pronta tem o próprio QR para o estabelecimento confirmar a entrega.

O comerciante continua no painel web. Este aplicativo não registra carimbos.

## Executar

O backend precisa estar no ar. No celular de teste, use o endereço da máquina, não `localhost`.

```bash
flutter pub get
flutter run --dart-define=API_BASE=http://192.168.0.10:8081
```

No Android, o manifesto de depuração libera HTTP local (`usesCleartextTraffic`). A câmera pede permissão para ler o QR da loja.

## Testes

```bash
flutter test
```
