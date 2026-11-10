package com.darmoz.whatsapp.flow;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bots/{botId}/flows")
public class FlowController {

    private final FlowService flowService;

    FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    @PostMapping
    ResponseEntity<FlowDtos.Detail> create(@PathVariable UUID botId, @Valid @RequestBody FlowDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flowService.create(botId, request));
    }

    @GetMapping
    List<FlowDtos.Summary> list(@PathVariable UUID botId) {
        return flowService.list(botId);
    }

    @GetMapping("/{flowId}")
    FlowDtos.Detail get(@PathVariable UUID botId, @PathVariable UUID flowId) {
        return flowService.get(botId, flowId);
    }

    @PatchMapping("/{flowId}")
    FlowDtos.Detail rename(@PathVariable UUID botId, @PathVariable UUID flowId,
                           @Valid @RequestBody FlowDtos.RenameRequest request) {
        return flowService.rename(botId, flowId, request);
    }

    @PutMapping("/{flowId}/draft")
    FlowDtos.Detail saveDraft(@PathVariable UUID botId, @PathVariable UUID flowId,
                              @Valid @RequestBody FlowDtos.DraftRequest request) {
        return flowService.saveDraft(botId, flowId, request);
    }

    @PostMapping("/{flowId}/validate")
    List<FlowValidator.Issue> validate(@PathVariable UUID botId, @PathVariable UUID flowId) {
        return flowService.validate(botId, flowId);
    }

    @PostMapping("/{flowId}/publish")
    FlowDtos.PublishResponse publish(@PathVariable UUID botId, @PathVariable UUID flowId) {
        return flowService.publish(botId, flowId);
    }

    @DeleteMapping("/{flowId}")
    ResponseEntity<Void> delete(@PathVariable UUID botId, @PathVariable UUID flowId) {
        flowService.delete(botId, flowId);
        return ResponseEntity.noContent().build();
    }
}
