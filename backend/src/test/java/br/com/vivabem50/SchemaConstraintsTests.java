package br.com.vivabem50;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Confere as regras de integridade que ficam no próprio banco. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class SchemaConstraintsTests {

    @Autowired
    private JdbcTemplate jdbc;

    private long newUser(String email) {
        return jdbc.queryForObject("""
                INSERT INTO users (name, email, password_hash, birth_date, data_consent_at)
                VALUES ('Teste', ?, 'hash', DATE '1960-05-10', now()) RETURNING id
                """, Long.class, email);
    }

    private long newAppointment(long userId) {
        return jdbc.queryForObject("""
                INSERT INTO appointments (user_id, type, title, starts_at)
                VALUES (?, 'CONSULTA', 'Consulta', now()) RETURNING id
                """, Long.class, userId);
    }

    private long newActivity(long userId) {
        return jdbc.queryForObject("""
                INSERT INTO activities (user_id, type, title, day_of_week, time_of_day)
                VALUES (?, 'CAMINHADA', 'Caminhada', 1, TIME '10:00') RETURNING id
                """, Long.class, userId);
    }

    private void insertReminder(Long appointmentId, Long activityId) {
        jdbc.update("INSERT INTO reminders (appointment_id, activity_id, remind_at) "
                + "VALUES (?, ?, TIMESTAMPTZ '2026-11-20 09:00:00-03')", appointmentId, activityId);
    }

    @Test
    void goalCatalogIsSeeded() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM goals", Integer.class)).isEqualTo(3);
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        newUser("maria@example.com");
        assertThatThrownBy(() -> newUser("MARIA@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reminderAcceptsOneTarget() {
        long appointment = newAppointment(newUser("bia@example.com"));
        insertReminder(appointment, null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reminders WHERE appointment_id = ?", Integer.class, appointment)).isEqualTo(1);
    }

    @Test
    void reminderRejectsNoTarget() {
        assertThatThrownBy(() -> insertReminder(null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reminderRejectsTwoTargets() {
        long user = newUser("joao@example.com");
        long appointment = newAppointment(user);
        long activity = newActivity(user);
        assertThatThrownBy(() -> insertReminder(appointment, activity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameReminderCannotBeCreatedTwice() {
        long appointment = newAppointment(newUser("ana@example.com"));
        insertReminder(appointment, null);
        assertThatThrownBy(() -> insertReminder(appointment, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void activityDayMustBeBetweenOneAndSeven() {
        long user = newUser("rui@example.com");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO activities (user_id, type, title, day_of_week, time_of_day)
                VALUES (?, 'CAMINHADA', 'Caminhada', 8, TIME '10:00')
                """, user)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingUserRemovesPersonalData() {
        long user = newUser("lia@example.com");
        long appointment = newAppointment(user);
        insertReminder(appointment, null);

        jdbc.update("DELETE FROM users WHERE id = ?", user);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM appointments WHERE user_id = ?", Integer.class, user)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reminders WHERE appointment_id = ?", Integer.class, appointment)).isZero();
    }
}
