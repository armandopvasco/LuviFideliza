-- ETAPA 06: APAGA TODOS OS DADOS DE public.versao_programa. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.versao_programa;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.versao_programa;
