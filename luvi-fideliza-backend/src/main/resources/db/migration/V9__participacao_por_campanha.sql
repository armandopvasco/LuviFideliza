-- Um mesmo telefone pode participar de campanhas diferentes do mesmo estabelecimento.
DROP INDEX IF EXISTS uk_participacao_estabelecimento_whatsapp;
CREATE UNIQUE INDEX uk_participacao_programa_whatsapp
  ON participacao (programa_id, whatsapp) WHERE whatsapp IS NOT NULL;
