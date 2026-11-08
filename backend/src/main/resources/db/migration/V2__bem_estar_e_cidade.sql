-- Cidade no perfil de saúde (exibida na tela de perfil) e hábito diário da tela Bem-Estar.

ALTER TABLE health_profiles
    ADD COLUMN city VARCHAR(80);

CREATE TABLE wellbeing_logs (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT      NOT NULL,
    logged_on          DATE        NOT NULL,
    water_cups         INTEGER     NOT NULL DEFAULT 0,
    activity_minutes   INTEGER     NOT NULL DEFAULT 0,
    mood               VARCHAR(20),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_wellbeing_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_wellbeing_logs_user_day UNIQUE (user_id, logged_on),
    CONSTRAINT ck_wellbeing_logs_water CHECK (water_cups BETWEEN 0 AND 30),
    CONSTRAINT ck_wellbeing_logs_minutes CHECK (activity_minutes BETWEEN 0 AND 600),
    CONSTRAINT ck_wellbeing_logs_mood CHECK (mood IS NULL OR mood IN ('OTIMO', 'BEM', 'NORMAL', 'TRISTE'))
);
