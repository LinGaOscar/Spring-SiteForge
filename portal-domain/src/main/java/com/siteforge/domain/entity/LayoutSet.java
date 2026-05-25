package com.siteforge.domain.entity;

import com.siteforge.domain.enums.TemplateKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "layout_set")
@Getter @Setter @NoArgsConstructor
public class LayoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "header_key", nullable = false, length = 50)
    private TemplateKey headerKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_key", length = 50)
    private TemplateKey bodyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "footer_key", nullable = false, length = 50)
    private TemplateKey footerKey;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
