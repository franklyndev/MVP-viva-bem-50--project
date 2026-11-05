package br.com.vivabem50.model;

import br.com.vivabem50.model.enums.AiUseCase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** Métricas da IA. Não guarda o contexto enviado nem o texto gerado. */
@Entity
@Table(name = "ai_interactions")
public class AiInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "use_case", nullable = false, length = 30)
    private AiUseCase useCase;

    @Column(name = "model_name", nullable = false, length = 80)
    private String modelName;

    @Column(name = "response_valid", nullable = false)
    private boolean responseValid;

    @Column
    private Boolean accepted;

    @Column(name = "user_rating")
    private Short userRating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public AiUseCase getUseCase() { return useCase; }
    public void setUseCase(AiUseCase useCase) { this.useCase = useCase; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public boolean isResponseValid() { return responseValid; }
    public void setResponseValid(boolean responseValid) { this.responseValid = responseValid; }
    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
    public Short getUserRating() { return userRating; }
    public void setUserRating(Short userRating) { this.userRating = userRating; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
