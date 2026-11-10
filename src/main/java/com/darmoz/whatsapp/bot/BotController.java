package com.darmoz.whatsapp.bot;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bots")
public class BotController {

    private final BotService botService;

    BotController(BotService botService) {
        this.botService = botService;
    }

    @PostMapping
    ResponseEntity<BotResponse> create(@Valid @RequestBody BotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(botService.create(request));
    }

    @PutMapping("/{id}")
    BotResponse update(@PathVariable UUID id, @Valid @RequestBody BotRequest request) {
        return botService.update(id, request);
    }

    @GetMapping("/{id}")
    BotResponse get(@PathVariable UUID id) {
        return botService.getById(id);
    }

    @GetMapping
    List<BotResponse> list(@RequestParam(required = false) UUID applicationId) {
        return botService.list(applicationId);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        botService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
