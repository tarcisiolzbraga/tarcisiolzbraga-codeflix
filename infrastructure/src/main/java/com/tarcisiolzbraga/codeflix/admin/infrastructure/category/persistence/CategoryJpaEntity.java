package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Audited
@Entity(name = "Category")
@Table(name = "category")
public class CategoryJpaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected CategoryJpaEntity() {
    }

    private CategoryJpaEntity(final Category category) {
        this.id = category.getId().getValue();
        this.name = category.getName();
        this.description = category.getDescription();
        this.active = category.isActive();
        this.createdAt = category.getCreatedAt();
        this.updatedAt = category.getUpdatedAt();
        this.deletedAt = category.getDeletedAt();
    }

    public static CategoryJpaEntity from(final Category category) {
        return new CategoryJpaEntity(category);
    }

    public Category toAggregate() {
        return Category.with(
                CategoryID.from(this.id),
                this.name,
                this.description,
                this.active,
                this.createdAt,
                this.updatedAt,
                this.deletedAt);
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isActive() {
        return this.active;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Instant getDeletedAt() {
        return this.deletedAt;
    }
}
