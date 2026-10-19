-- Clientes anteriores continuam válidos sem telefone até a atualização cadastral.
ALTER TABLE participacao ADD COLUMN whatsapp VARCHAR(13);
ALTER TABLE participacao ADD CONSTRAINT ck_participacao_whatsapp
  CHECK (whatsapp IS NULL OR whatsapp ~ '^55[1-9][0-9]9[0-9]{8}$');
CREATE UNIQUE INDEX uk_participacao_estabelecimento_whatsapp
  ON participacao (estabelecimento_id, whatsapp) WHERE whatsapp IS NOT NULL;
