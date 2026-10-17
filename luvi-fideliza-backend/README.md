# LUVI Fideliza+ — backend 0.4

Java 17, Spring Boot 3.5.6, PostgreSQL, Maven e Flyway.

Nesta entrega: participantes, ciclos de cartão, carimbos idempotentes,
conta do consumidor, convite de vínculo e carteira da própria conta.

**Já está com a 0.3 funcionando? Comece por docs/TESTAR_0.4.md.**
Não recrie os usuários nem os estabelecimentos. Use o banco existente e
mantenha LUVI_FIDELIZA_BOOTSTRAP_ENABLED=false.

## Executar localmente
Abra o pom.xml no IntelliJ, selecione JDK 17 e configure:
- LUVI_FIDELIZA_DB_URL=jdbc:postgresql://localhost:5432/luvi_fideliza
- LUVI_FIDELIZA_DB_USER=luvi_fideliza_app
- LUVI_FIDELIZA_DB_PASSWORD=sua senha do banco

Execute LuviFidelizaApplication; porta padrão 8081.
Flyway executa só as migrações pendentes. V1 a V3 não foram alteradas.
Para uma instalação do zero, crie um banco próprio e habilite temporariamente
SPRING_PROFILES_ACTIVE=local, LUVI_FIDELIZA_BOOTSTRAP_ENABLED=true,
LUVI_FIDELIZA_ADMIN_EMAIL e LUVI_FIDELIZA_ADMIN_PASSWORD.
Após criar o administrador, desative o bootstrap e retire suas credenciais iniciais.
Não há senha padrão. Nunca salve credenciais no Git.

## Regras do módulo
- Nome do participante é local ao negócio; nomes iguais não identificam uma pessoa.
- Um cartão é criado sem conta vinculada; depois o consumidor aceita o convite com sua conta.
- A chave UUID do cadastro evita repetir o mesmo envio criando duas participações.
- Uma conta só pode ter uma participação por programa, mas pode frequentar vários negócios.
- Um carimbo por compra registrada. Mesma chave e dados repetidos não duplicam o saldo.
- Chave repetida com outros dados retorna 409.
- Cartão guarda a versão das regras da abertura. Alterações valem para ciclos novos.
- Ciclos completos ficam preservados; a compra seguinte abre o próximo ciclo.
- Recompensas e resgates, estornos e junção de cadastros serão entregas posteriores.

## Testar
```bash
mvn clean package
```
A coleção completa está em docs/LUVI_Fideliza_0.4.postman_collection.json.
As coleções antigas permanecem para referência, mas use a 0.4 nos novos testes.
O roteiro começa com seus cadastros existentes e não exige repetir itens 01 a 15.

## Operação
Tudo local. Nenhum domínio ou servidor foi alterado.
Estes fluxos ainda não incluem controles necessários para publicação, como
recuperação/verificação de conta, limites de tentativas e regras comerciais de assinatura.
A estratégia de lock serializa gravações por estabelecimento para o volume do piloto.
