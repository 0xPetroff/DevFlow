package com.devflow.project.mapper;

import com.devflow.project.dto.ProjectResponse;
import com.devflow.project.dto.ProjectSummary;
import com.devflow.project.entity.Project;
import com.devflow.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = UserMapper.class)
public interface ProjectMapper {

    @Mapping(target = "memberCount", source = "memberCount")
    ProjectResponse toResponse(Project project, long memberCount);

    ProjectSummary toSummary(Project project);
}
