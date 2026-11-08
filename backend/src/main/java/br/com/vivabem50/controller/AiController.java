package br.com.vivabem50.controller;

import br.com.vivabem50.dto.ChatRequest;
import br.com.vivabem50.dto.ChatResponse;
import br.com.vivabem50.security.CurrentUser;
import br.com.vivabem50.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiChatService aiChatService;
    private final CurrentUser currentUser;

    public AiController(AiChatService aiChatService, CurrentUser currentUser) {
        this.aiChatService = aiChatService;
        this.currentUser = currentUser;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return aiChatService.reply(currentUser.require(), request.message());
    }
}
