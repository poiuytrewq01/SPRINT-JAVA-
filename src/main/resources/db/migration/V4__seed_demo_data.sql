-- =====================================================================
-- V4 - Carga inicial de demonstracao
--
-- Substitui o antigo DataInitializer (CommandLineRunner). Manter a carga
-- aqui significa que ela e versionada, auditavel e roda uma unica vez --
-- o Flyway registra a execucao em flyway_schema_history.
--
-- Senha de todos os usuarios: senha123
-- Os hashes abaixo sao BCrypt com fator de custo 10 ($2a$10$).
--
-- As datas sao relativas a CURRENT_DATE para que a demonstracao continue
-- coerente independentemente de quando o banco for criado.
-- =====================================================================

INSERT INTO veterinarians (id, name, crmv, email, phone, specialty, created_at) VALUES
    (1, 'Dra. Ana Souza',  'SP-12345', 'ana@clinicapet.com',    '11987654321', 'Clinica Geral', CURRENT_TIMESTAMP),
    (2, 'Dr. Carlos Lima', 'SP-67890', 'carlos@clinicapet.com', '11976543210', 'Dermatologia',  CURRENT_TIMESTAMP);

INSERT INTO tutors (id, name, email, phone, cpf, created_at) VALUES
    (1, 'Maria Silva', 'maria@email.com', '11912345678', '12345678901', CURRENT_TIMESTAMP),
    (2, 'Joao Santos', 'joao@email.com',  '11998765432', '98765432100', CURRENT_TIMESTAMP);

INSERT INTO users (id, name, email, password, role, enabled, tutor_id, veterinarian_id, created_at) VALUES
    (1, 'Administrador', 'admin@pethealth.com',   '$2a$10$GIscsHLK2VcywExvVG49puRqix6eE39dFj3cFvms4NniFbApi5PH.', 'ADMIN',        TRUE, NULL, NULL, CURRENT_TIMESTAMP),
    (2, 'Maria Silva',   'maria@email.com',       '$2a$10$Ma6AwQB8fWZu0ryMqz3P6OITcuZad5EAMip4mSkdf8S.r2FegDUwS', 'TUTOR',        TRUE, 1,    NULL, CURRENT_TIMESTAMP),
    (3, 'Joao Santos',   'joao@email.com',        '$2a$10$2TRW2/6wFu9XzUXxgVXmxuaKItAhtmNi3LucFVCcm4zQ1zan96KUS', 'TUTOR',        TRUE, 2,    NULL, CURRENT_TIMESTAMP),
    (4, 'Dra. Ana Souza','ana@clinicapet.com',    '$2a$10$BTR50pPJLtrZHiKasK6EAOhKMhTVdt6JouZiiCZNHn0ectJVjrlRm', 'VETERINARIAN', TRUE, NULL, 1,    CURRENT_TIMESTAMP),
    (5, 'Dr. Carlos Lima','carlos@clinicapet.com','$2a$10$cPHEonDzzVInBObCdwC3i.AybRHOLuImr6zDXebk87rlhfhDOPNT2', 'VETERINARIAN', TRUE, NULL, 2,    CURRENT_TIMESTAMP);

-- Mia nasce sem veterinario: e ela que sera usada para demonstrar o fluxo
-- de solicitacao de vinculo do zero.
INSERT INTO pets (id, name, species, breed, birth_date, weight, gender, profile_public, tutor_id, veterinarian_id, created_at, updated_at) VALUES
    (1, 'Rex',  'DOG', 'Labrador',         DATE '2020-03-15', 28.50, 'MALE',   FALSE, 1, 1,    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Mia',  'CAT', 'Siames',           DATE '2021-07-20',  4.20, 'FEMALE', FALSE, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Thor', 'DOG', 'Golden Retriever', DATE '2019-11-05', 32.00, 'MALE',   FALSE, 2, 2,    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO vaccines (id, name, manufacturer, application_date, next_dose_date, certificate_number, pet_id, veterinarian_id, created_at) VALUES
    (1, 'V10',        'MSD',        CURRENT_DATE - INTERVAL '6 months', CURRENT_DATE + INTERVAL '6 months', 'CERT-001', 1, 1, CURRENT_TIMESTAMP),
    (2, 'Antirrabica','Zoetis',     CURRENT_DATE - INTERVAL '3 months', CURRENT_DATE + INTERVAL '9 months', 'CERT-002', 1, 1, CURRENT_TIMESTAMP),
    (3, 'V4 Felina',  'Boehringer', CURRENT_DATE - INTERVAL '4 months', CURRENT_DATE + INTERVAL '8 months', 'CERT-003', 2, 1, CURRENT_TIMESTAMP),
    -- Dose vencida de proposito: derruba o health score do Thor e da assunto
    -- para a tela de alertas durante a demonstracao.
    (4, 'V10',        'MSD',        CURRENT_DATE - INTERVAL '14 months', CURRENT_DATE - INTERVAL '2 months', 'CERT-004', 3, 2, CURRENT_TIMESTAMP);

INSERT INTO clinical_records (id, date, description, diagnosis, treatment, weight, pet_id, veterinarian_id, created_at) VALUES
    (1, CURRENT_DATE - INTERVAL '2 months', 'Consulta de rotina', 'Saudavel',            'Nenhum tratamento necessario',    28.50, 1, 1, CURRENT_TIMESTAMP),
    (2, CURRENT_DATE - INTERVAL '1 month',  'Dermatite leve',     'Dermatite alergica',  'Shampoo medicamentoso por 30 dias', 32.00, 3, 2, CURRENT_TIMESTAMP);

INSERT INTO reminders (id, type, due_date, message, status, pet_id, created_at) VALUES
    (1, 'CHECKUP', CURRENT_DATE + INTERVAL '15 days', 'Check-up anual de Rex',                'PENDING', 1, CURRENT_TIMESTAMP),
    (2, 'VACCINE', CURRENT_DATE + INTERVAL '7 days',  'Reforco vacina V4 Felina para Mia',    'PENDING', 2, CURRENT_TIMESTAMP),
    (3, 'RETURN',  CURRENT_DATE + INTERVAL '5 months','Retorno de Thor: Dermatite leve',      'PENDING', 3, CURRENT_TIMESTAMP);

-- Rex chega com 4 dias consecutivos, sendo o ultimo ontem. Um check-in feito
-- durante a demonstracao leva o streak a 5 -- o efeito fica visivel na tela.
INSERT INTO activity_check_ins (id, date, activity_type, duration_minutes, notes, pet_id, created_at) VALUES
    (1, CURRENT_DATE - 4, 'WALK',    45, 'Passeio no parque',   1, CURRENT_TIMESTAMP),
    (2, CURRENT_DATE - 3, 'PLAY',    30, 'Brincadeira em casa', 1, CURRENT_TIMESTAMP),
    (3, CURRENT_DATE - 2, 'WALK',    50, 'Passeio longo',       1, CURRENT_TIMESTAMP),
    (4, CURRENT_DATE - 1, 'FEEDING', 15, 'Racao premium',       1, CURRENT_TIMESTAMP),
    (5, CURRENT_DATE - 1, 'BATH',    40, 'Banho e tosa',        3, CURRENT_TIMESTAMP);

INSERT INTO pet_streaks (id, pet_id, current_streak, longest_streak, last_check_in, level, total_check_ins, updated_at) VALUES
    (1, 1, 4, 9, CURRENT_DATE - 1, 'BEGINNER', 4, CURRENT_TIMESTAMP),
    (2, 3, 1, 1, CURRENT_DATE - 1, 'BEGINNER', 1, CURRENT_TIMESTAMP);

-- Solicitacao pendente ja na carga inicial: ao entrar como Dra. Ana, a fila
-- de aprovacao nao esta vazia.
INSERT INTO vet_link_requests (id, pet_id, veterinarian_id, requested_by_id, status, message, created_at) VALUES
    (1, 2, 1, 2, 'PENDING', 'Gostaria de acompanhar a Mia na clinica.', CURRENT_TIMESTAMP);

-- As colunas de identidade foram preenchidas manualmente acima. Sem este
-- ajuste, a proxima insercao feita pela aplicacao comecaria em 1 e violaria
-- a chave primaria.
SELECT setval(pg_get_serial_sequence('veterinarians',     'id'), (SELECT MAX(id) FROM veterinarians));
SELECT setval(pg_get_serial_sequence('tutors',            'id'), (SELECT MAX(id) FROM tutors));
SELECT setval(pg_get_serial_sequence('users',             'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('pets',              'id'), (SELECT MAX(id) FROM pets));
SELECT setval(pg_get_serial_sequence('vaccines',          'id'), (SELECT MAX(id) FROM vaccines));
SELECT setval(pg_get_serial_sequence('clinical_records',  'id'), (SELECT MAX(id) FROM clinical_records));
SELECT setval(pg_get_serial_sequence('reminders',         'id'), (SELECT MAX(id) FROM reminders));
SELECT setval(pg_get_serial_sequence('activity_check_ins','id'), (SELECT MAX(id) FROM activity_check_ins));
SELECT setval(pg_get_serial_sequence('pet_streaks',       'id'), (SELECT MAX(id) FROM pet_streaks));
SELECT setval(pg_get_serial_sequence('vet_link_requests', 'id'), (SELECT MAX(id) FROM vet_link_requests));
