-- VivaBem 50+ | Schema inicial
-- Nunca edite uma migração já aplicada: crie um novo arquivo V2__descricao.sql.

-- Dados cadastrais. Guarda a data de nascimento; a idade é calculada quando necessário.
CREATE TABLE users (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR(120) NOT NULL,
    email             VARCHAR(254) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    birth_date        DATE         NOT NULL,
    data_consent_at   TIMESTAMPTZ  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_birth_date CHECK (birth_date >= DATE '1900-01-01')
);

-- O e-mail é único sem diferenciar maiúsculas de minúsculas.
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

-- Dados potencialmente sensíveis (Política de Governança, item 14). Relação 1:1 com users.
CREATE TABLE health_profiles (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    activity_level  VARCHAR(20),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_health_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_health_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_health_profiles_activity_level
        CHECK (activity_level IN ('SEDENTARIO', 'LEVE', 'MODERADO', 'ATIVO'))
);

-- Catálogo de objetivos e relação N:N com users.
CREATE TABLE goals (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code   VARCHAR(40)  NOT NULL,
    label  VARCHAR(120) NOT NULL,
    CONSTRAINT uq_goals_code UNIQUE (code)
);

CREATE TABLE user_goals (
    user_id  BIGINT NOT NULL,
    goal_id  BIGINT NOT NULL,
    CONSTRAINT pk_user_goals PRIMARY KEY (user_id, goal_id),
    CONSTRAINT fk_user_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_goals_goal FOREIGN KEY (goal_id) REFERENCES goals (id) ON DELETE RESTRICT
);

CREATE INDEX ix_user_goals_goal ON user_goals (goal_id);

INSERT INTO goals (code, label) VALUES
    ('ATIVIDADE_FISICA',          'Atividade física'),
    ('ACOMPANHAMENTO_PREVENTIVO', 'Acompanhamento preventivo'),
    ('ORGANIZAR_COMPROMISSOS',    'Organizar consultas e exames');

-- Compromissos com data e hora definidas: consultas, exames e outros.
CREATE TABLE appointments (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    type              VARCHAR(20)  NOT NULL,
    title             VARCHAR(120) NOT NULL,
    location          VARCHAR(160),
    notes             TEXT,
    starts_at         TIMESTAMPTZ  NOT NULL,
    duration_minutes  INTEGER,
    status            VARCHAR(20)  NOT NULL DEFAULT 'AGENDADO',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_appointments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_appointments_type CHECK (type IN ('CONSULTA', 'EXAME', 'OUTRO')),
    CONSTRAINT ck_appointments_status
        CHECK (status IN ('AGENDADO', 'CONFIRMADO', 'REALIZADO', 'CANCELADO')),
    CONSTRAINT ck_appointments_duration CHECK (duration_minutes > 0)
);

CREATE INDEX ix_appointments_user_starts_at ON appointments (user_id, starts_at);

-- Atividades planejadas. Cada linha é um horário semanal: "caminhada, segunda, 10:00".
-- day_of_week segue a ISO 8601: 1 = segunda-feira, 7 = domingo.
CREATE TABLE activities (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    type              VARCHAR(20)  NOT NULL,
    title             VARCHAR(120) NOT NULL,
    day_of_week       SMALLINT     NOT NULL,
    time_of_day       TIME         NOT NULL,
    duration_minutes  INTEGER,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_activities_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_activities_type
        CHECK (type IN ('CAMINHADA', 'MOBILIDADE', 'FORCA', 'EQUILIBRIO', 'OUTRA')),
    CONSTRAINT ck_activities_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_activities_duration CHECK (duration_minutes > 0)
);

CREATE INDEX ix_activities_user_day ON activities (user_id, day_of_week);

-- Acompanhamento: um registro por atividade em cada dia.
CREATE TABLE activity_records (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activity_id   BIGINT      NOT NULL,
    performed_on  DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    notes         TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_activity_records_activity
        FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE CASCADE,
    CONSTRAINT uq_activity_records_activity_day UNIQUE (activity_id, performed_on),
    CONSTRAINT ck_activity_records_status CHECK (status IN ('CONCLUIDA', 'NAO_REALIZADA'))
);

-- Um lembrete pertence a um compromisso OU a uma atividade, nunca aos dois.
-- O usuário vem do compromisso ou da atividade; repetir user_id aqui criaria dependência transitiva.
-- Os UNIQUE impedem que o job agendado crie o mesmo lembrete duas vezes.
CREATE TABLE reminders (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    appointment_id  BIGINT,
    activity_id     BIGINT,
    remind_at       TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_reminders_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id) ON DELETE CASCADE,
    CONSTRAINT fk_reminders_activity
        FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE CASCADE,
    CONSTRAINT ck_reminders_one_target CHECK (num_nonnulls(appointment_id, activity_id) = 1),
    CONSTRAINT ck_reminders_status CHECK (status IN ('PENDENTE', 'ENVIADO', 'LIDO', 'CANCELADO')),
    CONSTRAINT uq_reminders_appointment_time UNIQUE (appointment_id, remind_at),
    CONSTRAINT uq_reminders_activity_time UNIQUE (activity_id, remind_at)
);

CREATE INDEX ix_reminders_status_time ON reminders (status, remind_at);

-- Métricas e rastreabilidade da IA. Não guarda o contexto enviado nem o texto gerado,
-- para não duplicar dados de saúde (Política de Governança, item 13).
CREATE TABLE ai_interactions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    use_case        VARCHAR(30)  NOT NULL,
    model_name      VARCHAR(80)  NOT NULL,
    response_valid  BOOLEAN      NOT NULL,
    accepted        BOOLEAN,
    user_rating     SMALLINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_ai_interactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_interactions_use_case CHECK (use_case IN ('ANALISE_AGENDA', 'RESUMO_SEMANAL')),
    CONSTRAINT ck_ai_interactions_rating CHECK (user_rating BETWEEN 1 AND 5)
);

CREATE INDEX ix_ai_interactions_user ON ai_interactions (user_id, created_at);
