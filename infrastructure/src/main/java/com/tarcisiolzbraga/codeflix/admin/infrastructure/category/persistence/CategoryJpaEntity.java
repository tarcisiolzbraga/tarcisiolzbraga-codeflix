package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.persistence.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

@Audited
@Entity(name = "Category")
@Table(name = "category")
public class CategoryJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected CategoryJpaEntity() {
    }

    private CategoryJpaEntity(final Category category) {
        super(category.getId().getValue(), category.getCreatedAt(), category.getUpdatedAt(), category.getDeletedAt());
        this.name = category.getName();
        this.description = category.getDescription();
        this.active = category.isActive();
    }

    public static CategoryJpaEntity from(final Category category) {
        return new CategoryJpaEntity(category);
    }

    public Category toAggregate() {
        return Category.with(
                CategoryID.from(getId()),
                this.name,
                this.description,
                this.active,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt());
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
}
