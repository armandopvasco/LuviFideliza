-- ETAPA 09: APAGA TODOS OS DADOS DE public.convite_proprietario. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.convite_proprietario;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.convite_proprietario;
