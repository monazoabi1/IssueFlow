package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.ProjectResponse;
import com.att.tdp.issueflow.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.dto.WorkloadEntryResponse;
import com.att.tdp.issueflow.model.ProjectEntity;
import com.att.tdp.issueflow.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> getAllProjects() {
        return projectService.getAllProjects().stream()
                .map(ProjectResponse::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/deleted")
    public List<ProjectResponse> getDeletedProjects() {
        return projectService.getDeletedProjects().stream()
                .map(ProjectResponse::new)
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/restore")
    public ProjectResponse restoreProject(@PathVariable Long id) {
        return new ProjectResponse(projectService.restoreProject(id));
    }

    @GetMapping("/{projectId}/workload")
    public List<WorkloadEntryResponse> getProjectWorkload(@PathVariable Long projectId) {
        return projectService.getProjectWorkload(projectId);
    }

    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Long id) {
        ProjectEntity project = projectService.getProjectById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return new ProjectResponse(project);
    }

    @PostMapping
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return new ProjectResponse(projectService.createProject(request));
    }

    @PatchMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return new ProjectResponse(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }
}
