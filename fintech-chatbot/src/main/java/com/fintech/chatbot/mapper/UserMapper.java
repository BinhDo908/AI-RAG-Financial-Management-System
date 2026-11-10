package com.fintech.chatbot.mapper;

import com.fintech.chatbot.dto.response.UserResponse;
import com.fintech.chatbot.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);
}
