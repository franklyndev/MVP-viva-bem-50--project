package br.com.vivabem50.controller;

import br.com.vivabem50.dto.UserResponse;
import br.com.vivabem50.security.CurrentUser;
import br.com.vivabem50.service.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUser currentUser;
    private final UserMapper userMapper;

    public MeController(CurrentUser currentUser, UserMapper userMapper) {
        this.currentUser = currentUser;
        this.userMapper = userMapper;
    }

    @GetMapping
    public UserResponse me() {
        return userMapper.toResponse(currentUser.require());
    }
}
