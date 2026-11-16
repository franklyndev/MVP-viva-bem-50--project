package br.com.vivabem50.controller;

import br.com.vivabem50.config.AuthenticatedUser;
import br.com.vivabem50.dto.ai.ChatRequest;
import br.com.vivabem50.dto.ai.ChatResponse;
import br.com.vivabem50.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "Assistente")
public class AiController {

    private final AiChatService aiChatService;

    public AiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Conversa com a assistente, que responde dúvidas de bem-estar e consulta ou altera a agenda")
    public ChatResponse chat(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChatRequest request) {
        return aiChatService.reply(AuthenticatedUser.id(jwt), request);
    }
}
