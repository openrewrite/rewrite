/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.yaml.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class AutodetectTest implements RewriteTest {

    private static Yaml.Documents parse(String code) {
        return (Yaml.Documents) YamlParser.builder().build().parse(code).findFirst().orElseThrow();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7125")
    @Test
    void detectsIndentedSequenceStyle() {
        IndentsStyle style = Autodetect.tabsAndIndents(parse(
            """
            fruit:
              - name: apple
                color: red
            """
        ), YamlDefaultStyles.indents());

        assertThat(style.isIndentedSequences()).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7125")
    @Test
    void detectsSameColumnSequenceStyle() {
        IndentsStyle style = Autodetect.tabsAndIndents(parse(
            """
            fruit:
            - name: apple
              color: red
            """
        ), YamlDefaultStyles.indents());

        assertThat(style.isIndentedSequences()).isFalse();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7125")
    @Test
    void defaultsToIndentedWhenNoSequences() {
        IndentsStyle style = Autodetect.tabsAndIndents(parse(
            """
            key:
              nested: value
            """
        ), YamlDefaultStyles.indents());

        assertThat(style.isIndentedSequences()).isTrue();
    }

    @Test
    void detectorDetectsIndentSize() {
        rewriteRun(
          withDetectedStyle(IndentsStyle.class, indentsStyle ->
            assertThat(indentsStyle.getIndentSize()).isEqualTo(4)),
          yaml(
            """
            root:
                child:
                    grandchild: value
            """
          )
        );
    }

    @Test
    void detectorDetectsDefaultIndentSize() {
        rewriteRun(
          withDetectedStyle(IndentsStyle.class, indentsStyle ->
            assertThat(indentsStyle.getIndentSize()).isEqualTo(2)),
          yaml(
            """
            root:
              child:
                grandchild: value
            """
          )
        );
    }

    @Test
    void detectorDetectsGeneralFormat() {
        rewriteRun(
          withDetectedStyle(GeneralFormatStyle.class, generalFormatStyle ->
            assertThat(generalFormatStyle.isUseCRLFNewLines()).isFalse()),
          yaml(
            """
            key: value
            other: data
            """
          )
        );
    }

    private static <S extends Style> Consumer<RecipeSpec> withDetectedStyle(Class<S> styleClass, Consumer<S> fn) {
        return spec -> spec.beforeRecipe(sources -> {
            Autodetect.Detector detector = Autodetect.detector();
            sources.forEach(detector::sample);

            @SuppressWarnings("unchecked")
            var foundStyle = (S) detector.build().getStyles().stream()
              .filter(styleClass::isInstance)
              .findAny().orElseThrow();
            fn.accept(foundStyle);
        });
    }
}
