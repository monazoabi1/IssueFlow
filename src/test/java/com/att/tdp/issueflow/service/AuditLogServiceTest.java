package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.model.AuditLogEntity;
import com.att.tdp.issueflow.model.AuditLogEntity.ActorType;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuditLogServiceTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EntityManager entityManager;

    /** Goal: log persists audit entry with action, entity, performer, and timestamp. */
    @Test
    void log_persistsAppendOnlyEntry() {
        auditLogService.log(AuditAction.CREATE, EntityType.PROJECT, 1L, 99L, ActorType.USER);

        var logs = auditLogService.getAuditLogs(EntityType.PROJECT, 1L, AuditAction.CREATE, null, null);
        assertThat(logs).hasSize(1);
        AuditLogEntity log = logs.getFirst();
        assertThat(log.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(log.getEntityType()).isEqualTo(EntityType.PROJECT);
        assertThat(log.getEntityId()).isEqualTo(1L);
        assertThat(log.getPerformedBy()).isEqualTo(99L);
        assertThat(log.getActor()).isEqualTo(ActorType.USER);
        assertThat(log.getTimestamp()).isNotNull();
    }

    /** Goal: getAuditLogs filtered by performedBy returns only matching performer. */
    @Test
    void getAuditLogs_filtersByPerformedBy() {
        auditLogService.log(AuditAction.CREATE, EntityType.USER, 10L, 1L, ActorType.USER);
        auditLogService.log(AuditAction.CREATE, EntityType.USER, 11L, 2L, ActorType.USER);

        assertThat(auditLogService.getAuditLogs(null, null, null, null, 1L))
                .extracting(AuditLogEntity::getEntityId)
                .containsExactly(10L);
    }

    /** Goal: getAuditLogs returns entries sorted by timestamp descending. */
    @Test
    void getAuditLogs_returnsNewestFirst() {
        long entityId = 88_888_888L;
        auditLogService.log(AuditAction.CREATE, EntityType.TICKET, entityId, null, ActorType.SYSTEM);
        auditLogService.log(AuditAction.UPDATE, EntityType.TICKET, entityId, null, ActorType.SYSTEM);
        auditLogService.log(AuditAction.DELETE, EntityType.TICKET, entityId, null, ActorType.SYSTEM);

        var logs = auditLogService.getAuditLogs(EntityType.TICKET, entityId, null, null, null);
        assertThat(logs).hasSize(3);
        assertThat(logs)
                .extracting(AuditLogEntity::getTimestamp)
                .isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat(logs)
                .extracting(AuditLogEntity::getAction)
                .containsExactlyInAnyOrder(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.DELETE);
    }

    /** Goal: getAuditLogs with no filters returns all persisted entries. */
    @Test
    void getAuditLogs_noFilters_returnsAll() {
        auditLogService.log(AuditAction.CREATE, EntityType.PROJECT, 1L, null, ActorType.USER);
        auditLogService.log(AuditAction.CREATE, EntityType.TICKET, 2L, null, ActorType.USER);

        assertThat(auditLogService.getAuditLogs(null, null, null, null, null)).hasSizeGreaterThanOrEqualTo(2);
    }

    /** Goal: Persisted audit logs cannot be updated or deleted (append-only). */
    @Test
    void persistedAuditLog_isAppendOnly() throws Exception {
        auditLogService.log(AuditAction.CREATE, EntityType.COMMENT, 7L, 3L, ActorType.USER);
        AuditLogEntity log =
                auditLogService.getAuditLogs(EntityType.COMMENT, 7L, AuditAction.CREATE, null, null).getFirst();
        final long logId = log.getId();

        entityManager.flush();
        entityManager.clear();
        AuditLogEntity managed = entityManager.find(AuditLogEntity.class, logId);

        var actionField = AuditLogEntity.class.getDeclaredField("action");
        actionField.setAccessible(true);
        actionField.set(managed, AuditAction.UPDATE);
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("append-only");

        entityManager.clear();
        AuditLogEntity managedForDelete = entityManager.find(AuditLogEntity.class, logId);
        assertThatThrownBy(() -> entityManager.remove(managedForDelete))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("append-only");
    }
}
