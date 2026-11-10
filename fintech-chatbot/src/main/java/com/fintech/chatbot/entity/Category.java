package com.fintech.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fintech.chatbot.entity.enums.CategoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E-03 — Danh mục thu chi. {@code userId = null} là danh mục mặc định do Admin quản lý (UC-C06);
 * khác null là danh mục riêng của user (UC-A03). Vì vậy không kế thừa UserOwnedEntity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CategoryType type;

    @Column(name = "icon", length = 50)
    private String icon;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(name = "parent_id", insertable = false, updatable = false)
    private Long parentId;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    public void assignParent(Category value) {
        parent = value;
        parentId = value == null ? null : value.getId();
    }

    public boolean isDefaultCategory() {
        return userId == null;
    }
}
