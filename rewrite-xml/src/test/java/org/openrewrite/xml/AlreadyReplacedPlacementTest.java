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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

class AlreadyReplacedPlacementTest implements RewriteTest {

    @Test
    void rejectPlanCarriedOnTheDocument() {
        AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlIsoVisitor<>() {
              @Override
              public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                  if (document.getMarkers().findFirst(AlreadyReplaced.class).isPresent()) {
                      return document;
                  }
                  Xml.Document d = super.visitDocument(document, ctx);
                  d = d.withRoot(d.getRoot().withName("migrated"));
                  return d.withMarkers(d.getMarkers().add(new AlreadyReplaced(randomId(), "plan", "sha256")));
              }
          })),
          xml("<project/>", "<migrated/>")
        ));
        assertThat(e).hasMessageContaining("AlreadyReplaced on the source file");
    }
}
