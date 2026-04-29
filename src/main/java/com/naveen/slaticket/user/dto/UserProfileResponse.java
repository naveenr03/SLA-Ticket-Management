package com.naveen.slaticket.user.dto;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String role
) {}