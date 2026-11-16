package br.com.vivabem50.model;

import br.com.vivabem50.model.enums.Mood;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "wellbeing_logs")
public class WellbeingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "logged_on", nullable = false)
    private LocalDate loggedOn;

    @Column(name = "water_liters", nullable = false, precision = 5, scale = 2)
    private BigDecimal waterLiters = BigDecimal.ZERO;

    @Column(name = "activity_minutes", nullable = false)
    private int activityMinutes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Mood mood;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDate getLoggedOn() { return loggedOn; }
    public void setLoggedOn(LocalDate loggedOn) { this.loggedOn = loggedOn; }
    public BigDecimal getWaterLiters() { return waterLiters; }
    public void setWaterLiters(BigDecimal waterLiters) { this.waterLiters = waterLiters; }
    public int getActivityMinutes() { return activityMinutes; }
    public void setActivityMinutes(int activityMinutes) { this.activityMinutes = activityMinutes; }
    public Mood getMood() { return mood; }
    public void setMood(Mood mood) { this.mood = mood; }
}
