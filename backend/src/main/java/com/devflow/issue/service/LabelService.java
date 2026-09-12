package com.devflow.issue.service;

import com.devflow.exception.ConflictException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.issue.dto.LabelRequest;
import com.devflow.issue.dto.LabelResponse;
import com.devflow.issue.entity.Label;
import com.devflow.issue.mapper.LabelMapper;
import com.devflow.issue.repository.LabelRepository;
import com.devflow.project.entity.Project;
import com.devflow.project.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LabelService {

    private final LabelRepository labelRepository;
    private final ProjectService projectService;
    private final LabelMapper labelMapper;

    public LabelService(LabelRepository labelRepository, ProjectService projectService, LabelMapper labelMapper) {
        this.labelRepository = labelRepository;
        this.projectService = projectService;
        this.labelMapper = labelMapper;
    }

    public List<LabelResponse> list(UUID projectId) {
        projectService.requireProject(projectId);
        return labelRepository.findByProjectIdOrderByNameAsc(projectId).stream()
                .map(labelMapper::toResponse)
                .toList();
    }

    @Transactional
    public LabelResponse create(UUID projectId, LabelRequest request) {
        Project project = projectService.requireActiveProject(projectId);
        String name = request.name().trim();
        if (labelRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new ConflictException("A label named %s already exists in this project".formatted(name));
        }
        return labelMapper.toResponse(labelRepository.save(new Label(project, name, request.color())));
    }

    @Transactional
    public LabelResponse update(UUID projectId, UUID labelId, LabelRequest request) {
        projectService.requireActiveProject(projectId);
        Label label = requireLabel(projectId, labelId);

        String name = request.name().trim();
        if (!label.getName().equalsIgnoreCase(name)
                && labelRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new ConflictException("A label named %s already exists in this project".formatted(name));
        }

        label.setName(name);
        label.setColor(request.color());
        return labelMapper.toResponse(label);
    }

    /** The issue_labels rows go with it, by the cascade on the join table's foreign key. */
    @Transactional
    public void delete(UUID projectId, UUID labelId) {
        projectService.requireActiveProject(projectId);
        labelRepository.delete(requireLabel(projectId, labelId));
    }

    private Label requireLabel(UUID projectId, UUID labelId) {
        return labelRepository.findByIdAndProjectId(labelId, projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Label", labelId));
    }
}
