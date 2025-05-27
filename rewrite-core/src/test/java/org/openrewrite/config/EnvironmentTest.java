package org.openrewrite.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentTest {

    @Test
    void listYamlLicenses() {
        var env = Environment.builder().scanYamlResources().build();
        var licenses = env.getRequiredLicenses();
        assertThat(licenses).containsExactlyInAnyOrder(
          new License("Apache License Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
          new License("Moderne Source Available License", "https://docs.moderne.io/licensing/moderne-source-available-license")
        );
    }

    @Test
    void listDependencyDetectedLicenses() {
        // Detect the hibernate license here.
        var env = Environment.builder().scanRuntimeClasspath().build();
        var licenses = env.getRequiredLicenses();
        assertThat(licenses).containsExactlyInAnyOrder(
          new License("Apache License Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
          new License("Moderne Source Available License", "https://docs.moderne.io/licensing/moderne-source-available-license")
        );
    }
}