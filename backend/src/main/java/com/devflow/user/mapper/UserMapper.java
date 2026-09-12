package com.devflow.user.mapper;

import com.devflow.user.dto.UserResponse;
import com.devflow.user.dto.UserSummary;
import com.devflow.user.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    UserSummary toSummary(User user);
}
