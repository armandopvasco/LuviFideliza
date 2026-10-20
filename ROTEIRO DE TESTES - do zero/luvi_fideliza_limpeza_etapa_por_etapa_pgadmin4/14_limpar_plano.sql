-- ETAPA 14: APAGA TODOS OS DADOS DE public.plano. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.plano;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.plano;
