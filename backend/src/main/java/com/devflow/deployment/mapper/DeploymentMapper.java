package com.devflow.deployment.mapper;

import com.devflow.deployment.dto.DeploymentResponse;
import com.devflow.deployment.dto.EnvironmentResponse;
import com.devflow.deployment.entity.Deployment;
import com.devflow.deployment.entity.Environment;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.user.mapper.UserMapper;
import org.mapstruct.Mapper;

@Mapper(uses = {UserMapper.class, ProjectMapper.class})
public interface DeploymentMapper {

    DeploymentResponse toResponse(Deployment deployment);

    EnvironmentResponse toResponse(Environment environment);
}
