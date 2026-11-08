package br.com.vivabem50.controller;

import br.com.vivabem50.dto.WellbeingResponse;
import br.com.vivabem50.dto.WellbeingUpdateRequest;
import br.com.vivabem50.security.CurrentUser;
import br.com.vivabem50.service.UserMapper;
import br.com.vivabem50.service.WellbeingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/wellbeing")
public class WellbeingController {

    private final WellbeingService wellbeingService;
    private final CurrentUser currentUser;
    private final UserMapper userMapper;

    public WellbeingController(WellbeingService wellbeingService, CurrentUser currentUser, UserMapper userMapper) {
        this.wellbeingService = wellbeingService;
        this.currentUser = currentUser;
        this.userMapper = userMapper;
    }

    @GetMapping
    public WellbeingResponse get(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate day = date != null ? date : LocalDate.now(userMapper.zoneId());
        return wellbeingService.get(currentUser.require(), day);
    }

    @PutMapping
    public WellbeingResponse update(@Valid @RequestBody WellbeingUpdateRequest request) {
        return wellbeingService.update(currentUser.require(), request);
    }
}
