package com.devflow.issue.mapper;

import com.devflow.issue.dto.CommentResponse;
import com.devflow.issue.entity.Comment;
import com.devflow.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = UserMapper.class)
public interface CommentMapper {

    // Auditing stamps both timestamps with the same instant on insert, so a later one means an edit.
    @Mapping(target = "edited",
            expression = "java(comment.getUpdatedAt().isAfter(comment.getCreatedAt()))")
    CommentResponse toResponse(Comment comment);
}
