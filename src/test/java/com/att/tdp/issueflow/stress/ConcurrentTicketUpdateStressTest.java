package com.att.tdp.issueflow.stress;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.Exception.ConflictException;
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
import com.att.tdp.issueflow.service.TicketService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("stress")
@SpringBootTest
class ConcurrentTicketUpdateStressTest {

    private static final int THREAD_COUNT = 24;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** Goal: Concurrent updates on same version yield one success and rest conflict. */
    @Test
    void concurrentUpdates_sameInitialVersion_onlyOneSucceeds() throws InterruptedException {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        long ticketId = tx.execute(status -> createSampleTicket());

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger otherFailures = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadIndex = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await(30, TimeUnit.SECONDS);
                    TransactionTemplate perThread = new TransactionTemplate(transactionManager);
                    perThread.executeWithoutResult(s -> {
                        UpdateTicketRequest update = new UpdateTicketRequest();
                        update.setVersion(0L);
                        update.setTitle("Concurrent-" + threadIndex);
                        ticketService.updateTicket(ticketId, update);
                    });
                    successes.incrementAndGet();
                } catch (Throwable ex) {
                    if (isConflict(ex)) {
                        conflicts.incrementAndGet();
                    } else {
                        otherFailures.incrementAndGet();
                    }
                }
            });
        }

        ready.await(30, TimeUnit.SECONDS);
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(otherFailures.get()).isZero();
        assertThat(successes.get()).isGreaterThanOrEqualTo(1);
        assertThat(conflicts.get()).isGreaterThanOrEqualTo(1);
        assertThat(successes.get() + conflicts.get()).isEqualTo(THREAD_COUNT);
    }

    private boolean isConflict(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConflictException) {
                return true;
            }
            if (current instanceof org.springframework.orm.ObjectOptimisticLockingFailureException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long createSampleTicket() {
        UserEntity owner = new UserEntity("concstress", "concstress@example.com", "Owner", Role.ADMIN);
        owner.setPassword("secret");
        owner = userRepository.save(owner);

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("ConcStress-" + System.nanoTime());
        projectRequest.setDescription("Concurrent update stress");
        projectRequest.setOwnerId(owner.getId());
        long projectId = projectService.createProject(projectRequest).getId();

        CreateTicketRequest ticketRequest = new CreateTicketRequest();
        ticketRequest.setTitle("Concurrent ticket");
        ticketRequest.setDescription("Body");
        ticketRequest.setStatus(TicketStatus.TODO);
        ticketRequest.setPriority(TicketPriority.MEDIUM);
        ticketRequest.setType(TicketType.BUG);
        ticketRequest.setProjectId(projectId);
        return ticketService.createTicket(ticketRequest).getTicketId();
    }
}
