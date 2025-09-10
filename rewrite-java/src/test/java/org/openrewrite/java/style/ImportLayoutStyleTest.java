/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed, in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.style;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.config.DeclarativeNamedStyles;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class ImportLayoutStyleTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void roundTripSerialize() throws Exception {
        var style = mapper.writeValueAsString(ImportLayoutStyle
                .builder()
                .packageToFold("java.awt.*")
                .packageToFold("java.swing.*", false)
                .staticPackageToFold("org.unit.Assert.*")
                .staticPackageToFold("org.mockito.Matchers.*", false)
                .importPackage("import java.*")
                .importPackage("import javax.*", false)
                .importAllOthers()
                .importStaticAllOthers()
                .build());

        var deserialized = mapper.readValue(style, ImportLayoutStyle.class);
        assertThat(style).isEqualTo(mapper.writeValueAsString(deserialized));
    }

    @Test
    void deserializeInDeclarativeNamedStyles() throws Exception {
        var style = new DeclarativeNamedStyles(
                randomId(),
                "name",
                "displayName",
                "description",
                Set.of("tag1", "tag2"),
                List.of(ImportLayoutStyle.builder()
                        .classCountToUseStarImport(5)
                        .nameCountToUseStarImport(5)
                        .importPackage("java.*")
                        .blankLine()
                        .importPackage("javax.*")
                        .blankLine()
                        .importAllOthers()
                        .blankLine()
                        .importPackage("org.springframework.*")
                        .blankLine()
                        .importStaticAllOthers()
                        .packageToFold("java.awt.*")
                        .packageToFold("java.swing.*")
                        .build()
                )
        );

        mapper.readValue(mapper.writeValueAsBytes(style), DeclarativeNamedStyles.class);
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4196")
    void addImportInPresenceOfDuplicateOtherImport() {
        ImportLayoutStyle style = new ImportLayoutStyle(
                Integer.MAX_VALUE, Integer.MAX_VALUE, emptyList(), emptyList());
        JRightPadded<J.Import> import1 = new JRightPadded<>(
                new J.Import(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        new JLeftPadded<>(Space.SINGLE_SPACE, true, Markers.EMPTY),
                        TypeTree.build("pkg.Clazz.MEMBER_1").withPrefix(Space.SINGLE_SPACE),
                        null),
                Space.EMPTY,
                Markers.EMPTY);
        JRightPadded<J.Import> import2 = new JRightPadded<>(
                new J.Import(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        new JLeftPadded<>(Space.SINGLE_SPACE, true, Markers.EMPTY),
                        TypeTree.build("pkg.Clazz.MEMBER_1").withPrefix(Space.SINGLE_SPACE),
                        null),
                Space.EMPTY,
                Markers.EMPTY);
        J.Import importToAdd = new J.Import(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                new JLeftPadded<>(Space.SINGLE_SPACE, true, Markers.EMPTY),
                TypeTree.build("pkg.Clazz.MEMBER_2").withPrefix(Space.SINGLE_SPACE),
            null);
        assertThat(style.addImport(List.of(import1, import2), importToAdd, null, emptyList()))
                .containsExactlyInAnyOrder(
                        import1, import1, new JRightPadded<>(importToAdd, Space.EMPTY, Markers.EMPTY));
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4241")
    void addImportWithNewLineInUnsortedImportList() {
        ImportLayoutStyle style = new ImportLayoutStyle(
          Integer.MAX_VALUE, Integer.MAX_VALUE, emptyList(), emptyList());
        JRightPadded<J.Import> import0 = new JRightPadded<>(
          new J.Import(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            new JLeftPadded<>(Space.SINGLE_SPACE, true, Markers.EMPTY),
            TypeTree.build("pkg.AAA.MEMBER_0").withPrefix(Space.SINGLE_SPACE),
            null),
          Space.EMPTY,
          Markers.EMPTY);
        JRightPadded<J.Import> import1 = new JRightPadded<>(
          new J.Import(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            new JLeftPadded<>(Space.SINGLE_SPACE, true, Markers.EMPTY),
            TypeTree.build("pkg.Clazz.MEMBER_3").withPrefix(Space.SINGLE_SPACE),
            null),
          Space.EMPTY,
          Markers.EMPTY);
        JRightPadded<J.Import> import3 = new JRightPadded<>(
          new J.Import(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            new JLeftPadded<>(Space.SINGLE_SPACE, true, Markers.EMPTY),
            TypeTree.build("pkg.Clazz.MEMBER_1").withPrefix(Space.SINGLE_SPACE),
            null),
          Space.EMPTY,
          Markers.EMPTY);
        J.Import importToAdd = new J.Import(
          randomId(),
          Space.EMPTY,
          Markers.EMPTY,
          new JLeftPadded<>(Space.EMPTY, true, Markers.EMPTY),
          TypeTree.build("pkg.Clazz.MEMBER_2").withPrefix(Space.SINGLE_SPACE),
          null);

        assertThat(style.addImport(List.of(import0, import1, import3), importToAdd, null, emptyList()).get(1).getElement().getPrefix()).isEqualTo(Space.format("\n"));
    }
}
