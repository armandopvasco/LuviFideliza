-- Apenas leitura. CONFIRME que existe exatamente um ADM antes do ultimo passo.
SELECT id, nome, email, telefone, administrador_plataforma, ativo FROM public.usuario ORDER BY administrador_plataforma DESC;
SELECT count(*) AS total_administradores FROM public.usuario WHERE administrador_plataforma IS TRUE;
