-- ETAPA 12: APAGA TODOS OS DADOS DE public.beneficio_comercial. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.beneficio_comercial;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.beneficio_comercial;
