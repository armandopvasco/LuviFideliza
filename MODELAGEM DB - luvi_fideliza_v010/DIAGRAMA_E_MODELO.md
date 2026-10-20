# LUVI Fideliza+ — revisão de modelagem para v0.10.0

**Base analisada:** código-fonte v0.9.1, migrações Flyway V1–V13.
**Estado:** proposta de arquitetura; NÃO é uma versão funcional v0.10.0.

## DER conceitual

```mermaid
erDiagram
    USUARIO ||--o{ FUNCIONARIO : trabalha_em
    USUARIO ||--o{ CLIENTE : participa_de
    ESTABELECIMENTO ||--o{ FUNCIONARIO : possui
    ESTABELECIMENTO ||--o{ CLIENTE : atende
    ESTABELECIMENTO ||--o{ PROGRAMA_FIDELIDADE : oferece
    PROGRAMA_FIDELIDADE ||--o{ PARTICIPACAO : recebe
    USUARIO ||--o{ PARTICIPACAO : pode_identificar
    PARTICIPACAO ||--o{ CARTAO_FIDELIDADE : gera
    CARTAO_FIDELIDADE ||--o{ MOVIMENTACAO_CARIMBO : registra
    CARTAO_FIDELIDADE ||--o{ RECOMPENSA : gera
    USUARIO ||--o{ ATIVACAO_CONTA : ativa
    USUARIO {
      uuid id PK
      string nome
      string email UK
      string telefone UK
      string senha_hash
      string status
      boolean administrador_plataforma
    }
    FUNCIONARIO {
      uuid id PK
      uuid usuario_id FK
      uuid estabelecimento_id FK
      string papel
      boolean ativo
    }
    CLIENTE {
      uuid usuario_id FK
      uuid estabelecimento_id FK
      timestamp criado_em
    }
    ESTABELECIMENTO {
      uuid id PK
      string nome
      boolean ativo
    }
    PROGRAMA_FIDELIDADE {
      uuid id PK
      uuid estabelecimento_id FK
    }
    PARTICIPACAO {
      uuid id PK
      uuid programa_id FK
      uuid usuario_id FK
    }
    CARTAO_FIDELIDADE {
      uuid id PK
      uuid participacao_id FK
    }
    MOVIMENTACAO_CARIMBO {
      uuid id PK
      uuid cartao_id FK
    }
    RECOMPENSA {
      uuid id PK
    }
    ATIVACAO_CONTA {
      uuid id PK
      uuid usuario_id FK
      string token_hash UK
      timestamp expira_em
      timestamp utilizado_em
    }
```

## Mapeamento: estrutura atual → nomes mais claros

| Tabela atual | Nome conceitual | Observação |
|---|---|---|
| `usuario` | `usuario` | Pessoa única; credenciais e status de ativação |
| `membro_estabelecimento` | `funcionario` | Proprietário e funcionário são papéis do vínculo |
| `adesao_estabelecimento` | `cliente` | Cliente da loja, não uma pessoa duplicada |
| `estabelecimento` | `estabelecimento` | Loja |
| `programa_fidelidade` | `programa_fidelidade` | Campanhas e regras; avaliar terminologia existente |
| `participacao` | `participacao` | Adesão do cliente a cada programa |
| `cartao_fidelidade` | `cartao_fidelidade` | Ciclos e saldos |
| `movimentacao_carimbo` | `movimentacao_carimbo` | Registro auditável de carimbos |
| `recompensa` | `recompensa` | Prêmio e resgate |
| `convite_proprietario` e `convite_equipe` | `ativacao_conta` (novo) | Tokens únicos, expiração e uso único |

**Importante:** `cliente` (vínculo com estabelecimento) não é igual a `participacao` (vínculo com programa/campanha). Manter os dois conceitos evita perder o histórico dos cartões.

## Fluxos esperados da implementação

1. ADM cadastra estabelecimento e pré-cadastra proprietário numa única transação.
2. Proprietário pré-cadastra funcionário; funcionário autorizado pré-cadastra cliente.
3. Pré-cadastro cria `usuario` com status `PENDENTE_ATIVACAO` e vínculo correspondente; não cria senha conhecida pelo operador.
4. Token aleatório de uso único e 48 horas; somente hash armazenado. O link mostra nome e contato pré-cadastrados, sem pedir recadastro.
5. Usuário define senha e confirma; backend ativa a conta, invalida token e só então permite login.
6. Conta existente: exigir autenticação para aprovar vínculo, nunca assumir conta apenas por telefone/e-mail coincidente.
7. QR Code público segue permitindo adesão; evitar duplicação por restrição única de `(estabelecimento_id, usuario_id)`.
8. Envio via WhatsApp gera link HTTPS com mensagem; não faz envio automático. E-mail automático requer SMTP/provedor configurado.

## Política de migração

- **Não** alterar migrações V1–V13 já aplicadas.
- **Não** apagar tabelas, redefinir `flyway_schema_history` ou limpar usuários automaticamente.
- Primeiro implementar entidades, serviços, endpoints, telas e testes de integração em uma branch.
- Depois criar V14+ de migração incremental e executar com backup.
- Para deixar o pgAdmin legível desde já, há consultas de leitura em `CONSULTAS_PGADMIN.sql`.
