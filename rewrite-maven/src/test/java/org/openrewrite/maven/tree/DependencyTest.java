/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven.tree;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyTest {

    @Test
    void toStringNotationWithAllFieldsPresent() {
        Dependency dep = Dependency.builder()
                .gav(new GroupArtifactVersion("com.example", "artifact", "1.0.0"))
                .classifier("sources")
                .type("jar")
                .build();
        assertThat(dep.toStringNotation()).isEqualTo("com.example:artifact:1.0.0:sources@jar");
    }

    @Test
    void toStringNotationWithOnlyGroupAndArtifact() {
        Dependency dep = Dependency.builder()
                .gav(new GroupArtifactVersion("com.example", "artifact", null))
                .build();
        assertThat(dep.toStringNotation()).isEqualTo("com.example:artifact");
    }

    @Test
    void toStringNotationWithNullGroup() {
        Dependency dep = Dependency.builder()
                .gav(new GroupArtifactVersion(null, "artifact", "1.0.0"))
                .build();
        assertThat(dep.toStringNotation()).isEqualTo(":artifact:1.0.0");
    }

    @Test
    void toStringNotationWithNullVersionButClassifierPresent() {
        Dependency dep = Dependency.builder()
                .gav(new GroupArtifactVersion("com.example", "artifact", null))
                .classifier("sources")
                .build();
        assertThat(dep.toStringNotation()).isEqualTo("com.example:artifact::sources");
    }

    @Test
    void toStringNotationWithNullVersionAndClassifierButExtensionPresent() {
        Dependency dep = Dependency.builder()
                .gav(new GroupArtifactVersion("com.example", "artifact", null))
                .type("jar")
                .build();
        assertThat(dep.toStringNotation()).isEqualTo("com.example:artifact@jar");
    }

    @Test
    void toStringNotationWithNullClassifierButExtensionPresent() {
        Dependency dep = Dependency.builder()
                .gav(new GroupArtifactVersion("com.example", "artifact", "1.0.0"))
                .type("jar")
                .build();
        assertThat(dep.toStringNotation()).isEqualTo("com.example:artifact:1.0.0@jar");
    }

    @Test
    void toStringNotationWithOnlyArtifact() {
        Dependency dep = Dependency.builder()
                .gav(new GroupArtifactVersion(null, "artifact", null))
                .build();
        assertThat(dep.toStringNotation()).isEqualTo(":artifact");
    }

    @Test
    void parseWithAllFieldsPresent() {
        Dependency dep = Dependency.parse("com.example:artifact:1.0.0:sources@jar");
        assertThat(dep).isNotNull();
        assertThat(dep.getGroupId()).isEqualTo("com.example");
        assertThat(dep.getArtifactId()).isEqualTo("artifact");
        assertThat(dep.getVersion()).isEqualTo("1.0.0");
        assertThat(dep.getClassifier()).isEqualTo("sources");
        assertThat(dep.getType()).isEqualTo("jar");
    }

    @Test
    void parseWithOnlyGroupAndArtifact() {
        Dependency dep = Dependency.parse("com.example:artifact");
        assertThat(dep).isNotNull();
        assertThat(dep.getGroupId()).isEqualTo("com.example");
        assertThat(dep.getArtifactId()).isEqualTo("artifact");
        assertThat(dep.getVersion()).isNull();
        assertThat(dep.getClassifier()).isNull();
        assertThat(dep.getType()).isNull();
    }

    @Test
    void parseWithNullGroup() {
        Dependency dep = Dependency.parse(":artifact:1.0.0");
        assertThat(dep).isNotNull();
        // Empty string group is preserved in parse
        assertThat(dep.getGroupId()).isEqualTo("");
        assertThat(dep.getArtifactId()).isEqualTo("artifact");
        assertThat(dep.getVersion()).isEqualTo("1.0.0");
        assertThat(dep.getClassifier()).isNull();
        assertThat(dep.getType()).isNull();
    }

    @Test
    void parseWithNullVersionButClassifierPresent() {
        Dependency dep = Dependency.parse("com.example:artifact::sources");
        assertThat(dep).isNotNull();
        assertThat(dep.getGroupId()).isEqualTo("com.example");
        assertThat(dep.getArtifactId()).isEqualTo("artifact");
        assertThat(dep.getVersion()).isEqualTo("");
        assertThat(dep.getClassifier()).isEqualTo("sources");
        assertThat(dep.getType()).isNull();
    }

    @Test
    void parseWithNullVersionAndClassifierButExtensionPresent() {
        Dependency dep = Dependency.parse("com.example:artifact@jar");
        assertThat(dep).isNotNull();
        assertThat(dep.getGroupId()).isEqualTo("com.example");
        assertThat(dep.getArtifactId()).isEqualTo("artifact");
        assertThat(dep.getVersion()).isNull();
        assertThat(dep.getClassifier()).isNull();
        assertThat(dep.getType()).isEqualTo("jar");
    }

    @Test
    void parseWithNullClassifierButExtensionPresent() {
        Dependency dep = Dependency.parse("com.example:artifact:1.0.0@jar");
        assertThat(dep).isNotNull();
        assertThat(dep.getGroupId()).isEqualTo("com.example");
        assertThat(dep.getArtifactId()).isEqualTo("artifact");
        assertThat(dep.getVersion()).isEqualTo("1.0.0");
        assertThat(dep.getClassifier()).isNull();
        assertThat(dep.getType()).isEqualTo("jar");
    }

    @Test
    void parseWithOnlyArtifact() {
        Dependency dep = Dependency.parse(":artifact");
        assertThat(dep).isNotNull();
        // Empty string group is preserved in parse
        assertThat(dep.getGroupId()).isEqualTo("");
        assertThat(dep.getArtifactId()).isEqualTo("artifact");
        assertThat(dep.getVersion()).isNull();
        assertThat(dep.getClassifier()).isNull();
        assertThat(dep.getType()).isNull();
    }

    @Test
    void parseNull() {
        assertThat(Dependency.parse(null)).isNull();
    }

    @Test
    void parseInvalidNotationTooFewComponents() {
        assertThat(Dependency.parse("artifact")).isNull();
    }

    @Test
    void parseInvalidNotationTooManyComponents() {
        assertThat(Dependency.parse("group:artifact:version:classifier:extra")).isNull();
    }

    @Test
    void parseInvalidNotationTooManyComponentsWithExtension() {
        assertThat(Dependency.parse("group:artifact:version:classifier:extra@jar")).isNull();
    }

    // Round-trip tests
    @Test
    void parseAndToStringNotationRoundTrip() {
        String notation = "com.example:artifact:1.0.0:sources@jar";
        Dependency dep = Dependency.parse(notation);
        assertThat(dep).isNotNull();
        assertThat(dep.toStringNotation()).isEqualTo(notation);
    }

    @Test
    void parseAndToStringNotationRoundTripMinimal() {
        String notation = ":artifact";
        Dependency dep = Dependency.parse(notation);
        assertThat(dep).isNotNull();
        assertThat(dep.toStringNotation()).isEqualTo(notation);
    }

    @Test
    void parseAndToStringNotationRoundTripWithEmptyVersion() {
        String notation = "com.example:artifact::sources";
        Dependency dep = Dependency.parse(notation);
        assertThat(dep).isNotNull();
        assertThat(dep.toStringNotation()).isEqualTo(notation);
    }

    // Tests for convenience methods
    @Test
    void withGroupId() {
        Dependency original = Dependency.parse("com.example:artifact:1.0.0");
        assertThat(original).isNotNull();
        Dependency updated = original.withGroupId("org.example");
        assertThat(updated.getGroupId()).isEqualTo("org.example");
        assertThat(updated.getArtifactId()).isEqualTo("artifact");
        assertThat(updated.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void withArtifactId() {
        Dependency original = Dependency.parse("com.example:artifact:1.0.0");
        assertThat(original).isNotNull();
        Dependency updated = original.withArtifactId("new-artifact");
        assertThat(updated.getGroupId()).isEqualTo("com.example");
        assertThat(updated.getArtifactId()).isEqualTo("new-artifact");
        assertThat(updated.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void withVersion() {
        Dependency original = Dependency.parse("com.example:artifact:1.0.0");
        assertThat(original).isNotNull();
        Dependency updated = original.withVersion("2.0.0");
        assertThat(updated.getGroupId()).isEqualTo("com.example");
        assertThat(updated.getArtifactId()).isEqualTo("artifact");
        assertThat(updated.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void withExt() {
        Dependency original = Dependency.parse("com.example:artifact:1.0.0");
        assertThat(original).isNotNull();
        Dependency updated = original.withExt("war");
        assertThat(updated.getGroupId()).isEqualTo("com.example");
        assertThat(updated.getArtifactId()).isEqualTo("artifact");
        assertThat(updated.getVersion()).isEqualTo("1.0.0");
        assertThat(updated.getType()).isEqualTo("war");
        assertThat(updated.getExt()).isEqualTo("war");
    }

    @Test
    void getExt() {
        Dependency dep = Dependency.parse("com.example:artifact:1.0.0@war");
        assertThat(dep).isNotNull();
        assertThat(dep.getExt()).isEqualTo("war");
        assertThat(dep.getType()).isEqualTo("war");
    }

    @Test
    void parseWithAllFieldsPresentUsingFluentAssertions() {
        Dependency dep = Dependency.parse("com.example:artifact:1.0.0:sources@jar");

        assertThat(dep)
                .isNotNull()
                .satisfies(d -> {
                    assertThat(d.getGroupId()).isEqualTo("com.example");
                    assertThat(d.getArtifactId()).isEqualTo("artifact");
                    assertThat(d.getVersion()).isEqualTo("1.0.0");
                    assertThat(d.getClassifier()).isEqualTo("sources");
                    assertThat(d.getType()).isEqualTo("jar");
                })
                .extracting(Dependency::toStringNotation)
                .isEqualTo("com.example:artifact:1.0.0:sources@jar");
    }

    @Test
    void parseRejectsGroovyMapNotation() {
        assertThat(Dependency.parse("group : \"com.google.guava\"")).isNull();
        assertThat(Dependency.parse("name : \"guava\"")).isNull();
        assertThat(Dependency.parse(" group: \"value\" ")).isNull();
        assertThat(Dependency.parse("version: '29.0-jre'")).isNull();
    }

    @Test
    void parseRejectsNotationWithQuotedValues() {
        // These look like partial Groovy notation, not valid dependency strings
        assertThat(Dependency.parse("'com.example':'artifact'")).isNull();
    }

    @Test
    void parseRejectsNotationWithTrailingSpaces() {
        // Valid dependency notation should not have spaces around colons
        assertThat(Dependency.parse("com.example : artifact")).isNull();
        assertThat(Dependency.parse("com.example: artifact")).isNull();
        assertThat(Dependency.parse("com.example :artifact")).isNull();
    }

    @Test
    void parseRejectsEmptyString() {
        assertThat(Dependency.parse("")).isNull();
    }

    @Test
    void parseRejectsOnlyColons() {
        assertThat(Dependency.parse(":")).isNull();
        assertThat(Dependency.parse("::")).isNull();
        assertThat(Dependency.parse(":::")).isNull();
    }

    @Test
    void parseRejectsInvalidCharactersInGroupId() {
        // GroupIds must match [A-Za-z0-9_.-]+
        assertThat(Dependency.parse("com/example:artifact")).isNull();
        assertThat(Dependency.parse("com example:artifact")).isNull();
        assertThat(Dependency.parse("com*example:artifact")).isNull();
        assertThat(Dependency.parse("com@example:artifact")).isNull();
        assertThat(Dependency.parse("com+example:artifact")).isNull();
        assertThat(Dependency.parse("com[example]:artifact")).isNull();
    }

    @Test
    void parseRejectsInvalidCharactersInArtifactId() {
        // ArtifactIds must match [A-Za-z0-9_.-]+
        assertThat(Dependency.parse("com.example:my/artifact")).isNull();
        assertThat(Dependency.parse("com.example:my artifact")).isNull();
        assertThat(Dependency.parse("com.example:my*artifact")).isNull();
        assertThat(Dependency.parse("com.example:my+artifact")).isNull();
        assertThat(Dependency.parse("com.example:my[artifact]")).isNull();
        assertThat(Dependency.parse("com.example:my#artifact")).isNull();
    }

    @Test
    void parseAcceptsValidCharactersInGroupAndArtifact() {
        // Valid characters: A-Z, a-z, 0-9, _, ., -
        Dependency dep = Dependency.parse("com.example-test_123:my-artifact_456.test");
        assertThat(dep).isNotNull();
        assertThat(dep.getGroupId()).isEqualTo("com.example-test_123");
        assertThat(dep.getArtifactId()).isEqualTo("my-artifact_456.test");
    }
}