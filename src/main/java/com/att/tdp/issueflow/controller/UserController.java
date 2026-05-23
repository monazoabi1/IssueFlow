package com.att.tdp.issueflow.controller;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.service.UserService;
import com.att.tdp.issueflow.dto.CreateUserRequest;
import com.att.tdp.issueflow.dto.MentionsPageResponse;
import com.att.tdp.issueflow.dto.UpdateUserRequest;
import com.att.tdp.issueflow.dto.UserResponse;
import com.att.tdp.issueflow.service.CommentService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final CommentService commentService;

    public UserController(UserService userService, CommentService commentService) {
        this.userService = userService;
        this.commentService = commentService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        List<UserEntity> users = userService.getAllUsers();
        return users.stream().map(UserResponse::new).collect(Collectors.toList());
    }

    @GetMapping("/{userId}/mentions")
    public MentionsPageResponse getMentionsForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return commentService.getMentionsForUser(userId, page, pageSize);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        UserEntity user = userService.getUserById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserResponse(user);
    }   

    @PostMapping
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        UserEntity createdUser = userService.createUser(request.toEntity());
        return new UserResponse(createdUser);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        UserEntity updatedUser = userService.updateUser(id, request.toEntity());
        return new UserResponse(updatedUser);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }


}
