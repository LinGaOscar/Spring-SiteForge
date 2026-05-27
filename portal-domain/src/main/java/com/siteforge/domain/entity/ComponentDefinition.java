package com.siteforge.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "component_definition")
@Getter @Setter @NoArgsConstructor
public class ComponentDefinition {

    @Id
    @Column(length = 100)
    private String key;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Column(name = "schema_json", columnDefinition = "TEXT")
    private String schemaJson;

    @Column(name = "device_mode", nullable = false, length = 20)
    private String deviceMode = "RWD";
}
