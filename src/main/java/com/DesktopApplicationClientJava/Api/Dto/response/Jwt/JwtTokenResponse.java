package com.DesktopApplicationClientJava.Api.Dto.response.Jwt;

public record JwtTokenResponse(
        String accessToken,
        String refreshToken
) {
}

