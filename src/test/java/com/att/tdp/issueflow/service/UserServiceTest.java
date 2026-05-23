package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    /** Goal: createUser BCrypt-hashes the password and writes a CREATE audit log. */
    @Test
    void createUser_hashesPassword() {
        UserEntity user = new UserEntity("jdoe", "jdoe@example.com", "John Doe", Role.DEVELOPER);
        user.setPassword("secret");

        UserEntity created = userService.createUser(user);

        assertThat(created.getId()).isPositive();
        assertThat(created.getPassword()).isNotEqualTo("secret");
        assertThat(created.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("secret", created.getPassword())).isTrue();
        assertThat(auditLogService.getAuditLogs(EntityType.USER, created.getId(), AuditAction.CREATE, null, null))
                .hasSize(1)
                .first()
                .satisfies(log -> {
                    assertThat(log.getEntityId()).isEqualTo(created.getId());
                    assertThat(log.getAction()).isEqualTo(AuditAction.CREATE);
                    assertThat(log.getEntityType()).isEqualTo(EntityType.USER);
                });
    }
}
