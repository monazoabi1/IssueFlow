package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.AddDependencyRequest;
import com.att.tdp.issueflow.dto.TicketDependencyResponse;
import com.att.tdp.issueflow.service.TicketDependencyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets/{ticketId}/dependencies")
public class TicketDependencyController {

    private final TicketDependencyService ticketDependencyService;

    public TicketDependencyController(TicketDependencyService ticketDependencyService) {
        this.ticketDependencyService = ticketDependencyService;
    }

    @PostMapping
    public ResponseEntity<Void> addDependency(
            @PathVariable Long ticketId, @Valid @RequestBody AddDependencyRequest request) {
        ticketDependencyService.addDependency(ticketId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<TicketDependencyResponse> listDependencies(@PathVariable Long ticketId) {
        return ticketDependencyService.listBlockers(ticketId).stream()
                .map(TicketDependencyResponse::new)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{blockerId}")
    public ResponseEntity<Void> removeDependency(
            @PathVariable Long ticketId, @PathVariable Long blockerId) {
        ticketDependencyService.removeDependency(ticketId, blockerId);
        return ResponseEntity.ok().build();
    }
}
