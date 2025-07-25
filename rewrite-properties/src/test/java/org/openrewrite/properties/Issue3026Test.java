/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.properties;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("UnusedProperty")
class Issue3026Test implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/3026")
    @Test
    void modifyContents() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new PropertiesVisitor<>() {
              @Override
              public Properties visitFile(Properties.File file, ExecutionContext ctx) {
                  List<Properties.Content> content = file.getContent();
                  if (content.size() != 1) {
                      return file;
                  }
                  content = ListUtils.concat(new Properties.Comment(randomId(), "", Markers.EMPTY, Properties.Comment.Delimiter.HASH_TAG, "foo\n"), content);
                  return file.withContent(content);
              }
          })),
          properties(
            """
              foo=bar
              """,
            """
              #foo
              foo=bar
              """
          )
        );
    }
}
