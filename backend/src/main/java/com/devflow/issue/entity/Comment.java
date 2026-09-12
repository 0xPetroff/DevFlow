package com.devflow.issue.entity;

import com.devflow.common.AuditedEntity;
import com.devflow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class Comment extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false, updatable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @Column(name = "body", nullable = false)
    private String body;

    protected Comment() {
    }

    public Comment(Issue issue, User author, String body) {
        this.issue = issue;
        this.author = author;
        this.body = body;
    }

    public Issue getIssue() {
        return issue;
    }

    public User getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
