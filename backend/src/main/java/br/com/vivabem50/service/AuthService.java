package br.com.vivabem50.service;

import br.com.vivabem50.dto.AuthResponse;
import br.com.vivabem50.dto.LoginRequest;
import br.com.vivabem50.dto.RegisterRequest;
import br.com.vivabem50.model.HealthProfile;
import br.com.vivabem50.model.User;
import br.com.vivabem50.repository.HealthProfileRepository;
import br.com.vivabem50.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final UserRepository users;
    private final HealthProfileRepository healthProfiles;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final UserMapper userMapper;

    public AuthService(
            UserRepository users,
            HealthProfileRepository healthProfiles,
            PasswordEncoder passwordEncoder,
            TokenService tokens,
            UserMapper userMapper
    ) {
        this.users = users;
        this.healthProfiles = healthProfiles;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.consent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É preciso consentir com o uso dos dados.");
        }
        if (users.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setBirthDate(request.birthDate());
        user.setDataConsentAt(OffsetDateTime.now());
        users.save(user);

        HealthProfile profile = new HealthProfile();
        profile.setUser(user);
        profile.setCity(blankToNull(request.city()));
        healthProfiles.save(profile);

        return new AuthResponse(tokens.issue(user), userMapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }
        return new AuthResponse(tokens.issue(user), userMapper.toResponse(user));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
