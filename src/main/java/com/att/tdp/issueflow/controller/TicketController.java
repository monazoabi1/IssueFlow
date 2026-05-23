package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.dto.TicketImportResponse;
import com.att.tdp.issueflow.dto.TicketResponse;
import com.att.tdp.issueflow.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.service.TicketCsvService;
import com.att.tdp.issueflow.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketCsvService ticketCsvService;

    public TicketController(TicketService ticketService, TicketCsvService ticketCsvService) {
        this.ticketService = ticketService;
        this.ticketCsvService = ticketCsvService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTickets(@RequestParam Long projectId) {
        byte[] csv = ticketCsvService.exportTickets(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets-" + projectId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketImportResponse importTickets(
            @RequestParam Long projectId, @RequestPart("file") MultipartFile file) {
        return ticketCsvService.importTickets(projectId, file);
    }

    @GetMapping
    public List<TicketResponse> getTicketsByProject(@RequestParam Long projectId) {
        return ticketService.getAllTicketsByProjectId(projectId).stream()
                .map(TicketResponse::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/deleted")
    public List<TicketResponse> getDeletedTickets(@RequestParam Long projectId) {
        return ticketService.getDeletedTicketsByProjectId(projectId).stream()
                .map(TicketResponse::new)
                .collect(Collectors.toList());
    }

    @PostMapping("/{ticketId}/restore")
    public TicketResponse restoreTicket(@PathVariable Long ticketId) {
        return new TicketResponse(ticketService.restoreTicket(ticketId));
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicketById(@PathVariable Long ticketId) {
        TicketEntity ticket = ticketService.getTicketById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        return new TicketResponse(ticket);
    }

    @PostMapping
    public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return new TicketResponse(ticketService.createTicket(request));
    }

    @PatchMapping("/{ticketId}")
    public TicketResponse updateTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketRequest request) {
        return new TicketResponse(ticketService.updateTicket(ticketId, request));
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long ticketId) {
        ticketService.deleteTicket(ticketId);
        return ResponseEntity.ok().build();
    }
}
