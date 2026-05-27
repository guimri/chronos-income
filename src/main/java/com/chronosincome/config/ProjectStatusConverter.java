package com.chronosincome.config;

import com.chronosincome.entity.Project.ProjectStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProjectStatusConverter implements Converter<String, ProjectStatus> {

    @Override
    public ProjectStatus convert(String value) {
        try {
            return ProjectStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Status inválido: '" + value + "'. Valores aceitos: ACTIVE, INACTIVE, COMPLETED"
            );
        }
    }
}
