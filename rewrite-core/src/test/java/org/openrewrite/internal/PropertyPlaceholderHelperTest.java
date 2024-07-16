/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.internal;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyPlaceholderHelperTest {

    @Test
    void dashed() {
        var helper = new PropertyPlaceholderHelper("%%{", "}", null);
        var s = helper.replacePlaceholders("%%{k1} %%{k2}", k -> switch (k) {
            case "k1" -> "hi";
            case "k2" -> "jon";
            default -> throw new UnsupportedOperationException();
        });
        assertThat(s).isEqualTo("hi jon");
    }

    @Test
    void testMavenBuiltInProperty() {
        var helper = new PropertyPlaceholderHelper("${", "}", null);
        final Set<String> visitedPlaceholders = new HashSet<>();
        visitedPlaceholders.add("project.version");
        visitedPlaceholders.add("pom.basedir");
        visitedPlaceholders.add("maven.home");
        visitedPlaceholders.add("env.PATH");
        visitedPlaceholders.add("settings.localRepository");
        assertThat(helper.parseStringValue("${project.version}", Function.identity(), visitedPlaceholders))
          .isEqualTo("project.version");
        assertThat(helper.parseStringValue("${pom.basedir}", Function.identity(), visitedPlaceholders))
          .isEqualTo("pom.basedir");
        assertThat(helper.parseStringValue("${maven.home}", Function.identity(), visitedPlaceholders))
          .isEqualTo("maven.home");
        assertThat(helper.parseStringValue("${env.PATH}", Function.identity(), visitedPlaceholders))
          .isEqualTo("env.PATH");
        assertThat(helper.parseStringValue("${settings.localRepository}", Function.identity(), visitedPlaceholders))
          .isEqualTo("settings.localRepository");
    }
}
