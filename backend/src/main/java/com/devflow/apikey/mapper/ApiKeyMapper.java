package com.devflow.apikey.mapper;

import com.devflow.apikey.dto.ApiKeyResponse;
import com.devflow.apikey.entity.ApiKey;
import com.devflow.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = UserMapper.class)
public interface ApiKeyMapper {

    @Mapping(target = "scopes", expression = "java(apiKey.scopeSet())")
    @Mapping(target = "active", expression = "java(apiKey.isActive())")
    ApiKeyResponse toResponse(ApiKey apiKey);
}
