package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.Exception.BadRequestException;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.dto.TicketImportResponse;
import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.validation.TicketCsvValidator;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TicketCsvService {

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final TicketCsvValidator ticketCsvValidator;

    public TicketCsvService(
            TicketRepository ticketRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            TicketService ticketService,
            TicketCsvValidator ticketCsvValidator) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.ticketCsvValidator = ticketCsvValidator;
    }

    @Transactional(readOnly = true)
    public byte[] exportTickets(Long projectId) {
        projectRepository
                .findByIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<TicketEntity> tickets = ticketRepository.findAllByProjectIdAndIsDeletedFalse(projectId);

        try (StringWriter writer = new StringWriter();
                CSVPrinter printer = new CSVPrinter(
                        writer,
                        CSVFormat.DEFAULT.builder().setHeader(TicketCsvValidator.REQUIRED_HEADERS.toArray(new String[0])).build())) {
            for (TicketEntity ticket : tickets) {
                printer.printRecord(
                        ticket.getTicketId(),
                        ticket.getTitle(),
                        ticket.getDescription(),
                        ticket.getStatus().name(),
                        ticket.getPriority().name(),
                        ticket.getType().name(),
                        ticket.getAssigneeId() == null ? "" : ticket.getAssigneeId());
            }
            printer.flush();
            return writer.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to export tickets to CSV");
        }
    }

    @Transactional
    public TicketImportResponse importTickets(Long projectId, MultipartFile file) {
        projectRepository
                .findByIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file is required");
        }

        TicketImportResponse response = new TicketImportResponse();

        try (InputStream inputStream = file.getInputStream();
                CSVParser parser = CSVParser.parse(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                        CSVFormat.DEFAULT.builder()
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .build())) {

            ticketCsvValidator.validateHeaders(parser.getHeaderNames());

            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rowNumber++;
                try {
                    ticketCsvValidator.validateRow(record, rowNumber);
                    CreateTicketRequest request = toCreateRequest(record, projectId, rowNumber);
                    ticketService.createTicket(request);
                    response.incrementCreated();
                } catch (BadRequestException | ResourceNotFoundException ex) {
                    response.addError(ex.getMessage());
                } catch (Exception ex) {
                    response.addError("Row " + rowNumber + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read CSV file");
        }

        return response;
    }

    private CreateTicketRequest toCreateRequest(CSVRecord record, Long projectId, int rowNumber) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(record.get("title").trim());
        request.setDescription(record.get("description").trim());
        request.setStatus(ticketCsvValidator.parseStatus(record.get("status"), rowNumber));
        request.setPriority(ticketCsvValidator.parsePriority(record.get("priority"), rowNumber));
        request.setType(ticketCsvValidator.parseType(record.get("type"), rowNumber));
        request.setProjectId(projectId);

        Long assigneeId = ticketCsvValidator.parseAssigneeId(record);
        if (assigneeId != null) {
            userRepository
                    .findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Row " + rowNumber + ": User not found"));
            request.setAssigneeId(assigneeId);
        }
        return request;
    }
}
