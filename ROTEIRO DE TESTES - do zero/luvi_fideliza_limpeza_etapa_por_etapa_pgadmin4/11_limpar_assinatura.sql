-- ETAPA 11: APAGA TODOS OS DADOS DE public.assinatura. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.assinatura;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.assinatura;
