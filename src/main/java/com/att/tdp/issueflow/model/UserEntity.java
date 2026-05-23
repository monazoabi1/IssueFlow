package com.att.tdp.issueflow.model;

import jakarta.persistence.*; //JPA annotations - @Entity, @Table, @Id, @GeneratedValue, @Enumerated
import jakarta.validation.constraints.*; //validation annotations - @NotBlank, @Email


@Entity // class is a JPA entity
@Table(name = "users")
public class UserEntity {

    @NotBlank
    @Column(name = "username", unique = true)
    private String username;

    @Email
    private String email;

    @NotBlank
    private String fullName;
    
    @Enumerated(EnumType.STRING)
    private Role role;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    public enum Role {
        ADMIN,
        DEVELOPER
    }

    @NotBlank
    @Column(nullable = false)
    private String password;
 
    public UserEntity() {}
    
    public UserEntity(String username, String email, String fullName, Role role) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    // -------------------------- GETTERS --------------------------
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    // -------------------------- SETTERS --------------------------
    public void setUsername(String username) {
        this.username = username;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public void setRole(Role role) {
        this.role = role;
    }
    public void setId(long id) {
        this.id = id;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}