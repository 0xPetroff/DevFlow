package com.devflow.issue.mapper;

import com.devflow.issue.dto.IssueResponse;
import com.devflow.issue.dto.IssueSummary;
import com.devflow.issue.entity.Issue;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.user.mapper.UserMapper;
import org.mapstruct.Mapper;

@Mapper(uses = {UserMapper.class, ProjectMapper.class, LabelMapper.class})
public interface IssueMapper {

    IssueResponse toResponse(Issue issue);

    IssueSummary toSummary(Issue issue);
}
