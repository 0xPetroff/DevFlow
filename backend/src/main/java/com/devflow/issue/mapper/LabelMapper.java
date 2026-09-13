package com.devflow.issue.mapper;

import com.devflow.issue.dto.LabelResponse;
import com.devflow.issue.entity.Label;
import org.mapstruct.Mapper;

@Mapper
public interface LabelMapper {

    LabelResponse toResponse(Label label);
}
