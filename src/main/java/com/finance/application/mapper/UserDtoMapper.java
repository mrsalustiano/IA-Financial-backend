package com.finance.application.mapper;

import com.finance.application.dto.AuthResponseDto;
import com.finance.application.dto.UserResponseDto;
import com.finance.domain.model.AuthSession;
import com.finance.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserDtoMapper {

    public UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(user.getId(), user.getEmail(), user.getCpf(), user.isActive(),
            user.getCreatedAt());
    }

    public AuthResponseDto toResponseDto(AuthSession session) {
        User user = session.user();
        AuthResponseDto.AuthUserDto authUser = new AuthResponseDto.AuthUserDto(user.getId(), user.getEmail(),
            user.isActive(), user.getCreatedAt());
        return new AuthResponseDto(session.accessToken(), session.refreshToken(), session.tokenType(),
            session.expiresInSeconds(), authUser);
    }
}
