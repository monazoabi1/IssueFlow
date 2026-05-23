package com.att.tdp.issueflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@DataJpaTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = UserRepository.class))
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    /** Goal: Persist a user and load it back by primary key with correct fields. */
    @Test
    void saveAndFindById() {
        UserEntity user = new UserEntity("jdoe", "jdoe@example.com", "John Doe", Role.DEVELOPER);
        user.setPassword("hashed-password");

        UserEntity saved = userRepository.save(user);

        assertThat(saved.getId()).isPositive();
        assertThat(userRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(UserEntity::getUsername, UserEntity::getEmail)
                .containsExactly("jdoe", "jdoe@example.com");
    }

    /** Goal: findByUsername returns the user when username exists. */
    @Test
    void findByUsername() {
        UserEntity user = new UserEntity("alice", "alice@example.com", "Alice Smith", Role.ADMIN);
        user.setPassword("hashed-password");
        userRepository.save(user);

        assertThat(userRepository.findByUsername("alice"))
                .isPresent()
                .get()
                .extracting(UserEntity::getFullName)
                .isEqualTo("Alice Smith");
    }

    /** Goal: existsByUsername is true for saved users and false for unknown names. */
    @Test
    void existsByUsername() {
        UserEntity user = new UserEntity("bob", "bob@example.com", "Bob Lee", Role.DEVELOPER);
        user.setPassword("hashed-password");
        userRepository.save(user);

        assertThat(userRepository.existsByUsername("bob")).isTrue();
        assertThat(userRepository.existsByUsername("unknown")).isFalse();
    }
}
