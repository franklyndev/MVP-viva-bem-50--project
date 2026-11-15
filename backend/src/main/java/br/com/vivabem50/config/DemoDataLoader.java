package br.com.vivabem50.config;

import br.com.vivabem50.model.Activity;
import br.com.vivabem50.model.Appointment;
import br.com.vivabem50.model.HealthProfile;
import br.com.vivabem50.model.User;
import br.com.vivabem50.model.WellbeingLog;
import br.com.vivabem50.model.enums.ActivityLevel;
import br.com.vivabem50.model.enums.ActivityType;
import br.com.vivabem50.model.enums.AppointmentStatus;
import br.com.vivabem50.model.enums.AppointmentType;
import br.com.vivabem50.repository.ActivityRepository;
import br.com.vivabem50.repository.AppointmentRepository;
import br.com.vivabem50.repository.HealthProfileRepository;
import br.com.vivabem50.repository.UserRepository;
import br.com.vivabem50.repository.WellbeingLogRepository;
import br.com.vivabem50.service.AgendaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Cria a usuária de demonstração "Maria Souza" com uma rotina pronta.
 * Só roda com DEMO_DATA_ENABLED=true; use apenas em desenvolvimento e na apresentação.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final ActivityRepository activityRepository;
    private final AppointmentRepository appointmentRepository;
    private final WellbeingLogRepository wellbeingLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final ZoneId zoneId;
    private final String demoEmail;
    private final String demoPassword;

    public DemoDataLoader(UserRepository userRepository, HealthProfileRepository healthProfileRepository,
                          ActivityRepository activityRepository, AppointmentRepository appointmentRepository,
                          WellbeingLogRepository wellbeingLogRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.timezone:America/Maceio}") String timezone,
                          @Value("${app.demo.email}") String demoEmail,
                          @Value("${app.demo.password}") String demoPassword) {
        this.userRepository = userRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.activityRepository = activityRepository;
        this.appointmentRepository = appointmentRepository;
        this.wellbeingLogRepository = wellbeingLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.zoneId = ZoneId.of(timezone);
        this.demoEmail = demoEmail.trim().toLowerCase(Locale.ROOT);
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User user = userRepository.findByEmailIgnoreCase(demoEmail).orElseGet(this::createMaria);
        if (!activityRepository.existsByUserId(user.getId())) {
            seedRoutine(user);
        }
    }

    private User createMaria() {
        User user = new User();
        user.setName("Maria Souza");
        user.setEmail(demoEmail);
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.setBirthDate(LocalDate.of(1964, 3, 15));
        user.setDataConsentAt(OffsetDateTime.now());
        userRepository.save(user);

        HealthProfile profile = new HealthProfile();
        profile.setUser(user);
        profile.setCity("Aracaju");
        profile.setActivityLevel(ActivityLevel.LEVE);
        healthProfileRepository.save(profile);
        return user;
    }

    private void seedRoutine(User user) {
        for (short day = 1; day <= 7; day++) {
            Activity walk = new Activity();
            walk.setUser(user);
            walk.setType(ActivityType.CAMINHADA);
            walk.setTitle("Caminhada no parque");
            walk.setDayOfWeek(day);
            walk.setTimeOfDay(LocalTime.of(9, 30));
            walk.setDurationMinutes(30);
            walk.setActive(true);
            activityRepository.save(walk);
        }

        LocalDate today = LocalDate.now(zoneId);
        saveAppointment(user, AppointmentType.OUTRO, "Losartana 50mg", null, "1 comprimido com água",
                AgendaService.at(today, LocalTime.of(8, 0), zoneId));
        saveAppointment(user, AppointmentType.CONSULTA, "Consulta – Dra. Helena", "Clínica Vida",
                "Cardiologista", AgendaService.at(today, LocalTime.of(15, 0), zoneId));
        saveAppointment(user, AppointmentType.OUTRO, "Vitamina D", null, "1 cápsula após o jantar",
                AgendaService.at(today, LocalTime.of(20, 0), zoneId));

        WellbeingLog log = new WellbeingLog();
        log.setUser(user);
        log.setLoggedOn(today);
        log.setWaterCups(4);
        log.setActivityMinutes(20);
        wellbeingLogRepository.save(log);
    }

    private void saveAppointment(User user, AppointmentType type, String title, String location, String notes,
                                 OffsetDateTime startsAt) {
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setType(type);
        appointment.setTitle(title);
        appointment.setLocation(location);
        appointment.setNotes(notes);
        appointment.setStartsAt(startsAt);
        appointment.setStatus(AppointmentStatus.AGENDADO);
        appointmentRepository.save(appointment);
    }
}
