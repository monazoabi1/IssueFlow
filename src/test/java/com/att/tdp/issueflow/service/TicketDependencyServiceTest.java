package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketDependencyServiceTest {

    @Autowired
    private TicketDependencyService dependencyService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    private long projectId;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("depowner", "depowner@example.com", "Owner", Role.ADMIN);
        owner.setPassword("secret");
        owner = userRepository.save(owner);

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("DepProject-" + System.nanoTime());
        projectRequest.setDescription("Dependency tests");
        projectRequest.setOwnerId(owner.getId());
        projectId = projectService.createProject(projectRequest).getId();
    }

    /** Goal: addDependency persists blocker; listBlockers returns it. */
    @Test
    void addAndListDependency() {
        long blocked = createTicket("Blocked");
        long blocker = createTicket("Blocker");

        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedBy(blocker);
        dependencyService.addDependency(blocked, request);

        assertThat(dependencyService.listBlockers(blocked))
                .extracting(t -> t.getTicketId())
                .containsExactly(blocker);
    }

    /** Goal: Adding circular dependency throws ConflictException. */
    @Test
    void addDependency_circularDependency_throwsConflict() {
        long ticketA = createTicket("A");
        long ticketB = createTicket("B");

        AddDependencyRequest aBlockedByB = new AddDependencyRequest();
        aBlockedByB.setBlockedBy(ticketB);
        dependencyService.addDependency(ticketA, aBlockedByB);

        AddDependencyRequest bBlockedByA = new AddDependencyRequest();
        bBlockedByA.setBlockedBy(ticketA);

        assertThatThrownBy(() -> dependencyService.addDependency(ticketB, bBlockedByA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("circular");
    }

    /** Goal: Transitioning blocked ticket to DONE throws ConflictException. */
    @Test
    void transitionToDone_withUnresolvedBlocker_throwsConflict() {
        long blocked = createTicket("Blocked");
        long blocker = createTicket("Blocker");

        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedBy(blocker);
        dependencyService.addDependency(blocked, request);

        advanceToInReview(blocked);

        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setVersion(currentVersion(blocked));
        update.setStatus(TicketStatus.DONE);

        assertThatThrownBy(() -> ticketService.updateTicket(blocked, update))
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
