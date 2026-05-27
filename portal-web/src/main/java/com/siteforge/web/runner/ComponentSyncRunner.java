package com.siteforge.web.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siteforge.domain.entity.ComponentDefinition;
import com.siteforge.domain.repository.ComponentDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComponentSyncRunner implements ApplicationRunner {

    private final ComponentDefinitionRepository componentDefinitionRepository;
    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        syncType("BODY",   "classpath:templates/fragments/body/*.html");
        syncType("HEADER", "classpath:templates/fragments/header/*.html");
        syncType("FOOTER", "classpath:templates/fragments/footer/*.html");
    }

    private void syncType(String type, String pattern) throws Exception {
        Resource[] resources = resourcePatternResolver.getResources(pattern);
        Set<String> found = Arrays.stream(resources)
                .map(r -> r.getFilename().replace(".html", ""))
                .collect(Collectors.toSet());

        // 掃描不到的 fragment → 標 inactive（不刪除，保留歷史紀錄）
        componentDefinitionRepository.findByTypeAndActiveTrue(type).stream()
                .filter(cd -> !found.contains(cd.getKey()))
                .forEach(cd -> {
                    cd.setActive(false);
                    cd.setSyncedAt(LocalDateTime.now());
                    componentDefinitionRepository.save(cd);
                });

        // 新增或重新啟用，並讀取 schema JSON
        for (String key : found) {
            ComponentDefinition cd = componentDefinitionRepository.findById(key)
                    .orElseGet(() -> {
                        ComponentDefinition c = new ComponentDefinition();
                        c.setKey(key);
                        return c;
                    });
            cd.setType(type);
            cd.setActive(true);
            cd.setSyncedAt(LocalDateTime.now());
            loadSchema(cd, key);
            componentDefinitionRepository.save(cd);
        }

        log.info("元件同步完成 [{}]: {} 個 active", type, found.size());
    }

    private void loadSchema(ComponentDefinition cd, String key) {
        try {
            Resource schemaResource = resourcePatternResolver.getResource(
                    "classpath:component-schemas/" + key + ".json");
            if (!schemaResource.exists()) {
                cd.setDeviceMode("RWD");
                return;
            }
            String rawJson = new String(schemaResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            cd.setSchemaJson(rawJson);
            Map<String, Object> schemaMap = objectMapper.readValue(rawJson, new TypeReference<>() {});
            Object dm = schemaMap.get("deviceMode");
            cd.setDeviceMode(dm instanceof String s ? s : "RWD");
        } catch (Exception e) {
            log.warn("無法讀取元件 schema [{}]: {}", key, e.getMessage());
            cd.setDeviceMode("RWD");
        }
    }
}
