package com.devflow.issue.entity;

import com.devflow.common.BaseEntity;
import com.devflow.project.entity.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Project-scoped tag. Names are unique within a project, not globally. */
@Entity
@Table(name = "labels")
public class Label extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    protected Label() {
    }

    public Label(Project project, String name, String color) {
        this.project = project;
        this.name = name;
        this.color = color;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
