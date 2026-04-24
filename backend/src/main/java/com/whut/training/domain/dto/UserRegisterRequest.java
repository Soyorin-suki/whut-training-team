package com.whut.training.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRegisterRequest {

    @NotBlank(message = "username cannot be blank")
    @Size(max = 50, message = "username length must be <= 50")
    private String username;

    @NotBlank(message = "email cannot be blank")
    @Email(message = "email format is invalid")
    private String email;

    @NotBlank(message = "password cannot be blank")
    @Size(min = 6, max = 64, message = "password length must be 6-64")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
