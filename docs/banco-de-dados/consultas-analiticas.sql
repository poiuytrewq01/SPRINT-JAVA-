-- =====================================================================
-- PetHealth - consultas analiticas sobre o schema das migrations V1-V5
--
-- Evidencia da integracao com a disciplina de Banco de Dados: as mesmas
-- tabelas que a aplicacao usa respondem perguntas de negocio direto em SQL.
-- Rodam em PostgreSQL 16 (banco local do docker-compose ou o do Render):
--
--   docker exec -i pethealth-db psql -U pethealth -d pethealth < docs/banco-de-dados/consultas-analiticas.sql
-- =====================================================================


-- 1. Pets com vacina vencida, por tutor ------------------------------------
-- Uma vacina esta vencida quando a proxima dose ja passou. Responde a "quem
-- a clinica precisa contatar hoje?".
SELECT t.name                         AS tutor,
       p.name                         AS pet,
       v.name                         AS vacina,
       v.next_dose_date               AS venceu_em,
       CURRENT_DATE - v.next_dose_date AS dias_de_atraso
FROM vaccines v
JOIN pets   p ON p.id = v.pet_id
JOIN tutors t ON t.id = p.tutor_id
WHERE v.next_dose_date < CURRENT_DATE
ORDER BY dias_de_atraso DESC;


-- 2. Tempo de resposta dos veterinarios as solicitacoes de vinculo ---------
-- Mede o fluxo 2: quanto tempo o tutor espera ate o profissional decidir.
-- FILTER separa as contagens por estado numa unica passada pela tabela.
SELECT vt.name                                                AS veterinario,
       COUNT(*)                                               AS solicitacoes,
       COUNT(*) FILTER (WHERE r.status = 'PENDING')           AS pendentes,
       COUNT(*) FILTER (WHERE r.status = 'APPROVED')          AS aprovadas,
       COUNT(*) FILTER (WHERE r.status = 'REJECTED')          AS recusadas,
       ROUND(AVG(EXTRACT(EPOCH FROM (r.responded_at - r.created_at)) / 3600)
             FILTER (WHERE r.responded_at IS NOT NULL), 1)    AS horas_media_resposta
FROM vet_link_requests r
JOIN veterinarians vt ON vt.id = r.veterinarian_id
GROUP BY vt.name
ORDER BY pendentes DESC, vt.name;


-- 3. Ranking de engajamento (gamificacao) ----------------------------------
-- Funcao de janela: a posicao de cada pet no ranking sem agrupar as linhas.
SELECT RANK() OVER (ORDER BY s.current_streak DESC, s.total_check_ins DESC) AS posicao,
       p.name            AS pet,
       s.current_streak  AS sequencia_atual,
       s.longest_streak  AS recorde,
       s.level           AS nivel,
       s.total_check_ins AS check_ins
FROM pet_streaks s
JOIN pets p ON p.id = s.pet_id
ORDER BY posicao;


-- 4. Notificacoes entregues pela fila, por usuario e tipo ------------------
-- Le o resultado do consumidor RabbitMQ (tabela da V5). Mostra quantas
-- notificacoes cada pessoa recebeu e quantas ainda nao abriu.
SELECT u.name                                        AS usuario,
       u.role                                        AS perfil,
       n.type                                        AS tipo,
       COUNT(*)                                      AS recebidas,
       COUNT(*) FILTER (WHERE n.read_at IS NULL)     AS nao_lidas,
       MAX(n.occurred_at)                            AS ultima
FROM notifications n
JOIN users u ON u.id = n.user_id
GROUP BY u.name, u.role, n.type
ORDER BY u.name, n.type;


-- 5. Retornos agendados automaticamente nos proximos 60 dias ---------------
-- Lembretes do tipo RETURN sao criados pelo fluxo 3 (atendimento clinico).
SELECT r.due_date AS data_retorno,
       p.name     AS pet,
       t.name     AS tutor,
       r.message  AS lembrete
FROM reminders r
JOIN pets   p ON p.id = r.pet_id
JOIN tutors t ON t.id = p.tutor_id
WHERE r.type = 'RETURN'
  AND r.status = 'PENDING'
  AND r.due_date BETWEEN CURRENT_DATE AND CURRENT_DATE + 60
ORDER BY r.due_date;
