package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.dto.TicketImportResponse;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketCsvServiceTest {

    @Autowired
    private TicketCsvService ticketCsvService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    private long projectId;
    private long assigneeId;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("csvowner", "csvowner@example.com", "Owner", Role.ADMIN);
        owner.setPassword("secret");
        owner = userRepository.save(owner);
        assigneeId = owner.getId();

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("CsvProject-" + System.nanoTime());
        projectRequest.setDescription("CSV tests");
        projectRequest.setOwnerId(owner.getId());
        projectId = projectService.createProject(projectRequest).getId();

        CreateTicketRequest ticket = new CreateTicketRequest();
        ticket.setTitle("Export me");
        ticket.setDescription("Line with, comma and \"quotes\"");
        ticket.setStatus(TicketStatus.TODO);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setType(TicketType.BUG);
        ticket.setProjectId(projectId);
        ticket.setAssigneeId(assigneeId);
        ticketService.createTicket(ticket);
    }

    /** Goal: exportTickets CSV includes headers and properly quoted ticket fields. */
    @Test
    void exportTickets_includesQuotedFields() {
        byte[] csv = ticketCsvService.exportTickets(projectId);
        String text = new String(csv, StandardCharsets.UTF_8);

        assertThat(text).contains("id,title,description,status,priority,type,assigneeId");
        assertThat(text).contains("Export me");
        assertThat(text).contains("comma");
    }

    /** Goal: importTickets with valid CSV creates all rows with no failures. */
    @Test
    void importTickets_validCsv_createsRows() {
        String csv = """
                id,title,description,status,priority,type,assigneeId
                ,Imported one,Desc one,TODO,MEDIUM,FEATURE,%d
                ,Imported two,"Desc two, with comma",TODO,LOW,BUG,
                """
                .formatted(assigneeId);

        MockMultipartFile file =
                new MockMultipartFile("file", "import.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        TicketImportResponse response = ticketCsvService.importTickets(projectId, file);

        assertThat(response.getCreated()).isEqualTo(2);
        assertThat(response.getFailed()).isZero();
        assertThat(response.getErrors()).isEmpty();
    }

    /** Goal: importTickets skips invalid rows, records errors, and imports valid ones. */
    @Test
    void importTickets_invalidRow_recordsErrorAndContinues() {
        String csv = """
                id,title,description,status,priority,type,assigneeId
                ,Good ticket,Valid,TODO,HIGH,BUG,
                ,Bad ticket,Invalid status,NOT_A_STATUS,HIGH,BUG,
                ,Another good,Also valid,TODO,HIGH,BUG,
                """;

        MockMultipartFile file =
                new MockMultipartFile("file", "import.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        TicketImportResponse response = ticketCsvService.importTickets(projectId, file);

        assertThat(response.getCreated()).isEqualTo(2);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getErrors()).anyMatch(msg -> msg.contains("invalid status"));
    }

    /** Goal: exportTickets for unknown project throws ResourceNotFoundException. */
    @Test
    void exportTickets_unknownProject_throwsNotFound() {
        assertThatThrownBy(() -> ticketCsvService.exportTickets(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found");
    }
}
