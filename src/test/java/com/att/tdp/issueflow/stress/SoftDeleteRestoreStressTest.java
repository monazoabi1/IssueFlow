package com.att.tdp.issueflow.stress;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.service.ProjectService;
import com.att.tdp.issueflow.service.TicketService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Tag("stress")
@SpringBootTest
@Transactional
class SoftDeleteRestoreStressTest {

    private static final int PROJECT_COUNT = 40;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserRepository userRepository;

    private UserEntity adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new UserEntity("sdstress", "sdstress@example.com", "Admin", Role.ADMIN);
        adminUser.setPassword("secret");
        adminUser = userRepository.save(adminUser);
        authenticateAs(adminUser);
    }

    /** Goal: Bulk project soft-delete and restore round-trips all projects. */
    @Test
    void deleteAndRestoreManyProjects_roundTrip() {
        List<Long> projectIds = new ArrayList<>();
        for (int i = 0; i < PROJECT_COUNT; i++) {
            CreateProjectRequest request = new CreateProjectRequest();
            request.setName("SDStress-" + System.nanoTime() + "-" + i);
            request.setDescription("Soft delete stress");
            request.setOwnerId(adminUser.getId());
            projectIds.add(projectService.createProject(request).getId());
        }

        for (Long id : projectIds) {
            projectService.deleteProject(id);
        }

        assertThat(projectService.getDeletedProjects()).hasSizeGreaterThanOrEqualTo(PROJECT_COUNT);

        for (Long id : projectIds) {
            projectService.restoreProject(id);
            assertThat(projectService.getProjectById(id)).isPresent();
        }

        assertThat(projectService.getAllProjects())
                .extracting(p -> p.getId())
                .containsAll(projectIds);
    }

    /** Goal: Bulk ticket delete makes all tickets visible in admin deleted list. */
    @Test
    void deleteManyTickets_adminCanListDeleted() {
        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("TicketSDStress-" + System.nanoTime());
        projectRequest.setDescription("Ticket soft delete");
        projectRequest.setOwnerId(adminUser.getId());
        long projectId = projectService.createProject(projectRequest).getId();

        List<Long> ticketIds = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            CreateTicketRequest ticketRequest = new CreateTicketRequest();
            ticketRequest.setTitle("T" + i);
            ticketRequest.setDescription("Body");
            ticketRequest.setStatus(TicketStatus.TODO);
            ticketRequest.setPriority(TicketPriority.LOW);
            ticketRequest.setType(TicketType.BUG);
            ticketRequest.setProjectId(projectId);
            ticketIds.add(ticketService.createTicket(ticketRequest).getTicketId());
        }

        ticketIds.forEach(ticketService::deleteTicket);

        assertThat(ticketService.getDeletedTicketsByProjectId(projectId))
                .hasSize(ticketIds.size());
    }

    private void authenticateAs(UserEntity user) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of()));
    }
}
