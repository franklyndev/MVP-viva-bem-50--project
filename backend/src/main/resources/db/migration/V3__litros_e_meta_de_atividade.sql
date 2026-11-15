ALTER TABLE wellbeing_logs
    DROP CONSTRAINT ck_wellbeing_logs_water;

ALTER TABLE wellbeing_logs
    RENAME COLUMN water_cups TO water_liters;

ALTER TABLE wellbeing_logs
    ALTER COLUMN water_liters TYPE NUMERIC(5, 2)
        USING (water_liters * 0.25)::NUMERIC(5, 2);

ALTER TABLE wellbeing_logs
    ADD CONSTRAINT ck_wellbeing_logs_water_liters CHECK (water_liters BETWEEN 0 AND 20);

ALTER TABLE users
    ADD COLUMN activity_goal INTEGER NOT NULL DEFAULT 30,
    ADD CONSTRAINT ck_users_activity_goal CHECK (activity_goal BETWEEN 1 AND 600);
