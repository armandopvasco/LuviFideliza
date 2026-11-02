# Aplicativo do balcão

O atendente usa o celular da barbearia. A câmera lê o QR do cliente e abre o cartão. O carimbo e a entrega da recompensa acontecem depois da confirmação na tela. O WhatsApp continua disponível para quem não mostrar o QR.

O cliente permanece no aplicativo da carteira. Campanhas, equipe e o restante da configuração continuam no painel web.

Android é o alvo agora. O mesmo projeto Flutter gera o iOS depois, sem reescrever as telas.

## Executar

O backend precisa estar no ar. No celular de teste, use o endereço da máquina, não `localhost`.

```bash
flutter pub get
flutter run --dart-define=API_BASE=http://192.168.0.10:8081
```

No Android, o manifesto de depuração libera HTTP local. A câmera pede permissão para ler o QR.

## Testes

```bash
flutter test
```
