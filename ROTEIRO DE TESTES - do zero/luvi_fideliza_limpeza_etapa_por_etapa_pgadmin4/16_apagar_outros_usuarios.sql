-- ETAPA FINAL. MUITO IMPORTANTE: altere o e-mail do ADM antes de executar.
-- Se a validacao falhar, nada sera excluido.
BEGIN;
DO $bloco$
DECLARE
 v_email text := 'armandopvasco@gmail.com';
 v_admin_id uuid;
BEGIN
 IF v_email = 'SUBSTITUA_PELO_EMAIL_DO_ADM@EXEMPLO.COM' THEN
   RAISE EXCEPTION 'Informe o email do ADM no script';
 END IF;
 IF (SELECT count(*) FROM public.usuario WHERE administrador_plataforma IS TRUE) <> 1 THEN
   RAISE EXCEPTION 'Esperado exatamente um ADM';
 END IF;
 SELECT id INTO v_admin_id FROM public.usuario
  WHERE administrador_plataforma IS TRUE AND lower(email)=lower(trim(v_email));
 IF v_admin_id IS NULL THEN
   RAISE EXCEPTION 'ADM informado nao encontrado';
 END IF;
 DELETE FROM public.usuario WHERE id <> v_admin_id;
 IF (SELECT count(*) FROM public.usuario) <> 1 THEN
   RAISE EXCEPTION 'Validacao final falhou';
 END IF;
END $bloco$;
COMMIT;
SELECT id,nome,email,telefone,administrador_plataforma FROM public.usuario;
