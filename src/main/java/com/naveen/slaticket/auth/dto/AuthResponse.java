package com.naveen.slaticket.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType
) {}