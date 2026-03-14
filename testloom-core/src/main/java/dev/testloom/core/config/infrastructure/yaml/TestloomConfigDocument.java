package dev.testloom.core.config.infrastructure.yaml;

import dev.testloom.core.config.domain.model.TestloomConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * YAML document wrapper that maps the top-level {@code testloom} key.
 */
@Getter
@Setter
public class TestloomConfigDocument {
    private TestloomConfig testloom;
}
