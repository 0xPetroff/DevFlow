package com.devflow.project.mapper;

import com.devflow.project.dto.ProjectMemberResponse;
import com.devflow.project.entity.ProjectMember;
import com.devflow.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = UserMapper.class)
public interface ProjectMemberMapper {

    @Mapping(target = "owner", source = "owner")
    ProjectMemberResponse toResponse(ProjectMember member, boolean owner);
}
