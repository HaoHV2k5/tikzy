package com.tikzy.auth.mapper;

import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper cho User -> UserResponse.
 * role được map từ role.code (Role entity) thông qua nested property mapping.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.code", target = "role")
    UserResponse toUserResponse(User user);
}
