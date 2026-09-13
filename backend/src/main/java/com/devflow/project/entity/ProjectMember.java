package com.devflow.project.entity;

import com.devflow.common.BaseEntity;
import com.devflow.user.entity.Role;
import com.devflow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A user's role inside one project. This is the second tier of the permission model: the
 * account-wide role only decides who may create projects and who bypasses these checks.
 */
@Entity
@Table(name = "project_members")
public class ProjectMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.DEVELOPER;

    protected ProjectMember() {
    }

    public ProjectMember(Project project, User user, Role role) {
        this.project = project;
        this.user = user;
        this.role = role;
    }

    public Project getProject() {
        return project;
    }

    public User getUser() {
        return user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
