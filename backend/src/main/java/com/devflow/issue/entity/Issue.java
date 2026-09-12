package com.devflow.issue.entity;

import com.devflow.common.AuditedEntity;
import com.devflow.project.entity.Project;
import com.devflow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "issues")
public class Issue extends AuditedEntity {

    /** The gap left between adjacent cards, and the position of the first card in a column. */
    public static final double POSITION_GAP = 1000;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "issue_number", nullable = false, updatable = false)
    private int issueNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IssueStatus status = IssueStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private IssuePriority priority = IssuePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private IssueType type = IssueType.TASK;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false, updatable = false)
    private User creator;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "board_position", nullable = false)
    private double boardPosition = POSITION_GAP;

    @Column(name = "estimate_points")
    private Short estimatePoints;

    /*
     * Batched rather than join-fetched: a collection fetch combined with pagination forces
     * Hibernate to page in memory, which loads the whole result set to return twenty rows.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "issue_labels",
            joinColumns = @JoinColumn(name = "issue_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id"))
    @BatchSize(size = 50)
    private Set<Label> labels = new LinkedHashSet<>();

    protected Issue() {
    }

    public Issue(Project project, int issueNumber, String title, User creator) {
        this.project = project;
        this.issueNumber = issueNumber;
        this.title = title;
        this.creator = creator;
    }

    /** The human-facing identifier, for example DEVF-42. */
    public String getKey() {
        return "%s-%d".formatted(project.getProjectKey(), issueNumber);
    }

    public Project getProject() {
        return project;
    }

    public int getIssueNumber() {
        return issueNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public void setPriority(IssuePriority priority) {
        this.priority = priority;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        this.type = type;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public User getCreator() {
        return creator;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public double getBoardPosition() {
        return boardPosition;
    }

    public void setBoardPosition(double boardPosition) {
        this.boardPosition = boardPosition;
    }

    public Short getEstimatePoints() {
        return estimatePoints;
    }

    public void setEstimatePoints(Short estimatePoints) {
        this.estimatePoints = estimatePoints;
    }

    public Set<Label> getLabels() {
        return labels;
    }

    public void replaceLabels(Set<Label> replacement) {
        labels.clear();
        labels.addAll(replacement);
    }
}
