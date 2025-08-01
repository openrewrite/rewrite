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
package org.openrewrite.toml;

import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.toml.Assertions.toml;

class TomlVisitorTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-spring/issues/665")
    @Test
    void visitMarkupErrorMarkers() {
        List<RuntimeException> exceptions = new ArrayList<>();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
              @Override
              public Tree preVisit(Tree tree, ExecutionContext ctx) {
                  // Mimics what we do in the rewrite-gradle-plugin
                  tree.getMarkers().findFirst(Markup.Error.class).ifPresent(e -> {
                      Optional<SourceFile> sourceFile = Optional.ofNullable(getCursor().firstEnclosing(SourceFile.class));
                      String sourcePath = sourceFile.map(SourceFile::getSourcePath).map(Path::toString).orElse("<unknown>");
                      exceptions.add(new RuntimeException("Error while visiting " + sourcePath + ": " + e.getDetail()));
                  });
                  return tree;
              }
          })),
          toml(
            """
              [versions]
              jackson = '2.14.2'

              [libraries]
              jackson-annotations = { module = 'com.fasterxml.jackson.core:jackson-annotations', version.ref = 'jackson' }
              jackson-core = { module = 'com.fasterxml.jackson.core:jackson-core', version.ref = 'jackson' }

              [bundles]
              jackson = ['jackson-annotations', 'jackson-core']
              """
          )
        );
        assertThat(exceptions).isEmpty();
    }
}
