package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.UserRegistrationResponseDto;
import com.vitaliy.medcard.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserRegistrationResponseDto toDto(User user);
}
