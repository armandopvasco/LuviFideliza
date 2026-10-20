-- ETAPA 03: APAGA TODOS OS DADOS DE public.cartao_fidelidade. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.cartao_fidelidade;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.cartao_fidelidade;
