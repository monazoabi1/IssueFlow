package com.att.tdp.issueflow.model;

import jakarta.persistence.*; //JPA annotations - @Entity, @Table, @Id, @GeneratedValue, @Enumerated
import jakarta.validation.constraints.*; //validation annotations - @NotBlank, @Email
import java.time.LocalDateTime;

@Entity // class is a JPA entity
@Table(name = "projects")
public class ProjectEntity {

    @NotBlank
    @Column(name = "name", unique = true)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long projectId;

    private boolean isDeleted;
    
    //todo: check if these are needed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    protected ProjectEntity() {}

    public ProjectEntity(String name, String description, UserEntity owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.isDeleted = false;
    }

    // -------------------------- GETTERS --------------------------
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public UserEntity getOwner() {
        return owner;
    }
    public boolean isDeleted() {
        return isDeleted;
    }
    public long getId() {
        return projectId;
    }

    // -------------------------- SETTERS --------------------------
    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }
    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
    public void setId(long projectId) {
        this.projectId = projectId;
    }
}
