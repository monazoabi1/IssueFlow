package com.att.tdp.issueflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.model.ProjectEntity;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@DataJpaTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {ProjectRepository.class, UserRepository.class}))
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    /** Goal: Saved project is loadable by id when not soft-deleted. */
    @Test
    void saveAndFindByIdAndIsDeletedFalse() {
        UserEntity owner = saveOwner("1");
        ProjectEntity project = new ProjectEntity("Alpha", "First project", owner);

        ProjectEntity saved = projectRepository.save(project);

        assertThat(saved.getId()).isPositive();
        assertThat(projectRepository.findByIdAndIsDeletedFalse(saved.getId()))
                .isPresent()
                .get()
                .extracting(ProjectEntity::getName, ProjectEntity::getDescription)
                .containsExactly("Alpha", "First project");
    }

    /** Goal: findAllByIsDeletedFalse omits soft-deleted projects. */
    @Test
    void findAllByIsDeletedFalse_excludesDeletedProjects() {
        UserEntity owner = saveOwner("2");
        ProjectEntity active = projectRepository.save(new ProjectEntity("Active", "Still visible", owner));
        ProjectEntity deleted = projectRepository.save(new ProjectEntity("Deleted", "Should be hidden", owner));
        deleted.setDeleted(true);
        projectRepository.save(deleted);

        assertThat(projectRepository.findAllByIsDeletedFalse())
                .extracting(ProjectEntity::getId)
                .contains(active.getId())
                .doesNotContain(deleted.getId());
    }

    /** Goal: findByIdAndIsDeletedFalse returns empty for soft-deleted project. */
    @Test
    void findByIdAndIsDeletedFalse_returnsEmptyWhenSoftDeleted() {
        UserEntity owner = saveOwner("3");
        ProjectEntity project = projectRepository.save(new ProjectEntity("Gone", "Soft deleted", owner));
        project.setDeleted(true);
        projectRepository.save(project);

        assertThat(projectRepository.findByIdAndIsDeletedFalse(project.getId())).isEmpty();
    }

    private UserEntity saveOwner(String suffix) {
        UserEntity user = new UserEntity(
                "owner" + suffix,
                "owner" + suffix + "@example.com",
                "Owner " + suffix,
                Role.DEVELOPER);
        user.setPassword("hashed-password");
        return userRepository.save(user);
    }
}
