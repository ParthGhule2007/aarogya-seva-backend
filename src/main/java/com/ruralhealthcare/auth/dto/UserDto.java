package com.ruralhealthcare.auth.dto;

import com.ruralhealthcare.auth.domain.User;
import com.ruralhealthcare.auth.domain.UserRole;

import java.util.UUID;

public class UserDto {

    private UUID id;
    private String username;
    private String fullName;
    private String phoneNumber;
    private String email;
    private UserRole role;
    private String associatedEntityId;

    public UserDto() {
    }

    public UserDto(UUID id, String username, String fullName, String phoneNumber, String email, UserRole role, String associatedEntityId) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.role = role;
        this.associatedEntityId = associatedEntityId;
    }

    public static UserDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole(),
                user.getAssociatedEntityId()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getAssociatedEntityId() {
        return associatedEntityId;
    }

    public void setAssociatedEntityId(String associatedEntityId) {
        this.associatedEntityId = associatedEntityId;
    }
}
