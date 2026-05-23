package com.att.tdp.issueflow.stress;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.dto.AddDependencyRequest;
import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.service.ProjectService;
import com.att.tdp.issueflow.service.TicketDependencyService;
import com.att.tdp.issueflow.service.TicketService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Tag("stress")
@SpringBootTest
@Transactional
class TicketDependencyStressTest {

    private static final int CHAIN_LENGTH = 25;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketDependencyService dependencyService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    private long projectId;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("depstress", "depstress@example.com", "Owner", Role.ADMIN);
        owner.setPassword("secret");
        owner = userRepository.save(owner);

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("DepStress-" + System.nanoTime());
        projectRequest.setDescription("Dependency chain stress");
        projectRequest.setOwnerId(owner.getId());
        projectId = projectService.createProject(projectRequest).getId();
    }

    /** Goal: Long blocker chain prevents tail ticket DONE until blockers resolved. */
    @Test
    void longDependencyChain_blocksDoneUntilResolved() {
        List<Long> chain = new ArrayList<>();
        for (int i = 0; i < CHAIN_LENGTH; i++) {
            chain.add(createTicket("Chain " + i));
        }

        for (int i = 1; i < CHAIN_LENGTH; i++) {
            AddDependencyRequest request = new AddDependencyRequest();
            request.setBlockedBy(chain.get(i - 1));
            dependencyService.addDependency(chain.get(i), request);
        }

        long tail = chain.get(CHAIN_LENGTH - 1);
        advanceToInReview(tail);

        UpdateTicketRequest done = new UpdateTicketRequest();
        done.setVersion(currentVersion(tail));
        done.setStatus(TicketStatus.DONE);

        assertThatThrownBy(() -> ticketService.updateTicket(tail, done))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("blocked by");
    }

    private long createTicket(String title) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(title);
        request.setDescription("Body");
        request.setStatus(TicketStatus.TODO);
        request.setPriority(TicketPriority.MEDIUM);
        request.setType(TicketType.BUG);
        request.setProjectId(projectId);
        return ticketService.createTicket(request).getTicketId();
    }

    private void advanceToInReview(long ticketId) {
        updateStatus(ticketId, TicketStatus.IN_PROGRESS);
        updateStatus(ticketId, TicketStatus.IN_REVIEW);
    }

    private void updateStatus(long ticketId, TicketStatus status) {
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setVersion(currentVersion(ticketId));
        update.setStatus(status);
        ticketService.updateTicket(ticketId, update);
    }

    private long currentVersion(long ticketId) {
        return ticketService.getTicketById(ticketId).orElseThrow().getVersion();
    }
}
