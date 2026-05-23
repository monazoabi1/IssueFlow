package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // SQL: SELECT * FROM users WHERE username = ?
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    // SQL: SELECT COUNT(*) FROM users WHERE username = ?
    boolean existsByUsername(String username);

    List<UserEntity> findByRoleOrderByIdAsc(Role role);
}
