package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    List<ProjectEntity> findAllByIsDeletedFalse();

    List<ProjectEntity> findAllByIsDeletedTrue();

    Optional<ProjectEntity> findByIdAndIsDeletedFalse(Long id);

    Optional<ProjectEntity> findByIdAndIsDeletedTrue(Long id);
}
