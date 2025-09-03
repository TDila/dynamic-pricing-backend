package com.ecommerce.dynamic_pricing_backend.controller;

import com.ecommerce.dynamic_pricing_backend.dto.UpdateUserRequest;
import com.ecommerce.dynamic_pricing_backend.dto.UserDto;
import com.ecommerce.dynamic_pricing_backend.dto.UserStatsDto;
import com.ecommerce.dynamic_pricing_backend.entity.User;
import com.ecommerce.dynamic_pricing_backend.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ==== CUSTOMER ENDPOINTS (Customer Authentication Required) ====

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<UserDto> getCurrentUserProfile(@AuthenticationPrincipal User user) {
        UserDto userDto = convertToDto(user);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserProfile(@AuthenticationPrincipal User currentUser, @RequestBody UpdateUserRequest
            request) {
        try {
            // Create User object from request for the service
            User userDetails = new User();
            userDetails.setFirstName(request.getFirstName());
            userDetails.setLastName(request.getLastName());
            userDetails.setPhone(request.getPhone());
            userDetails.setAddress(request.getAddress());

            User updatedUser = userService.updateUser(currentUser.getId(), userDetails);
            UserDto userDto = convertToDto(updatedUser);
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<UserStatsDto> getUserStats(@AuthenticationPrincipal User user) {
        UserStatsDto stats = new UserStatsDto();
        stats.setUserId(user.getId());
        stats.setOrderCount(userService.getOrderCount(user.getId()));
        stats.setFirstTimeBuyer(userService.isFirstTimeBuyer(user.getId()));
        return ResponseEntity.ok(stats);
    }

    // ==== ADMIN ENDPOINTS (Admin Authentication Required) ====

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserDto> userDtos = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        try {
            User user = userService.findById(id);
            UserDto userDto = convertToDto(user);
            return ResponseEntity.ok(userDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserById(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            User userDetails = new User();
            userDetails.setFirstName(request.getFirstName());
            userDetails.setLastName(request.getLastName());
            userDetails.setPhone(request.getPhone());
            userDetails.setAddress(request.getAddress());

            User updatedUser = userService.updateUser(id, userDetails);
            UserDto userDto = convertToDto(updatedUser);
            return ResponseEntity.ok(userDto);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserStatsDto> getUserStatsById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        try {
            userService.findById(id); // Verify user exists
            UserStatsDto stats = new UserStatsDto();
            stats.setUserId(id);
            stats.setOrderCount(userService.getOrderCount(id));
            stats.setFirstTimeBuyer(userService.isFirstTimeBuyer(id));
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ==== HELPER METHODS ====

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setRole(user.getRole().name());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
