-- Base de identidade, estabelecimentos e condições comerciais.
-- Não cria usuários, senhas ou cortesias automaticamente.
CREATE TABLE usuario (
 id UUID PRIMARY KEY, nome VARCHAR(120) NOT NULL,
 email VARCHAR(254) NOT NULL, senha_hash VARCHAR(255) NOT NULL,
 email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
 administrador_plataforma BOOLEAN NOT NULL DEFAULT FALSE,
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_usuario_email ON usuario (LOWER(email));
CREATE TABLE estabelecimento (
 id UUID PRIMARY KEY, nome VARCHAR(150) NOT NULL,
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE membro_estabelecimento (
 id UUID PRIMARY KEY,
 usuario_id UUID NOT NULL REFERENCES usuario(id),
 estabelecimento_id UUID NOT NULL REFERENCES estabelecimento(id),
 papel VARCHAR(20) NOT NULL CHECK (papel IN ('PROPRIETARIO','FUNCIONARIO')),
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 UNIQUE (usuario_id, estabelecimento_id)
);
CREATE INDEX ix_membro_estabelecimento ON membro_estabelecimento(estabelecimento_id);
CREATE TABLE plano (
 id UUID PRIMARY KEY, nome VARCHAR(80) NOT NULL,
 limite_clientes INTEGER NOT NULL CHECK (limite_clientes > 0),
 valor_mensal NUMERIC(12,2) NOT NULL CHECK (valor_mensal >= 0),
 ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE assinatura (
 id UUID PRIMARY KEY,
 estabelecimento_id UUID NOT NULL REFERENCES estabelecimento(id),
 plano_id UUID NOT NULL REFERENCES plano(id),
 status VARCHAR(20) NOT NULL CHECK (status IN ('ATIVA','SUSPENSA','ENCERRADA')),
 inicio TIMESTAMPTZ NOT NULL, fim TIMESTAMPTZ,
 CHECK (fim IS NULL OR fim > inicio)
);
CREATE UNIQUE INDEX uk_assinatura_ativa ON assinatura(estabelecimento_id) WHERE status = 'ATIVA';
CREATE TABLE beneficio_comercial (
 id UUID PRIMARY KEY,
 estabelecimento_id UUID NOT NULL REFERENCES estabelecimento(id),
 tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('TESTE_GRATUITO','CORTESIA_TEMPORARIA','CORTESIA_PERMANENTE')),
 inicio TIMESTAMPTZ NOT NULL, fim TIMESTAMPTZ,
 motivo VARCHAR(500) NOT NULL,
 concedido_por UUID NOT NULL REFERENCES usuario(id),
 revogado_em TIMESTAMPTZ,
 CHECK (fim IS NULL OR fim > inicio),
 CHECK ((tipo = 'CORTESIA_PERMANENTE' AND fim IS NULL) OR (tipo <> 'CORTESIA_PERMANENTE' AND fim IS NOT NULL))
);
CREATE INDEX ix_beneficio_estabelecimento ON beneficio_comercial(estabelecimento_id);
