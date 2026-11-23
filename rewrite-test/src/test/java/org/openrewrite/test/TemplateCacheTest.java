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
package org.openrewrite.test;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class TemplateCacheTest implements RewriteTest {

    @Test
    void compilesTemplateOnceAcrossCyclesRerunsAndProbes() {
        FindSizeComparedToZero.COMPILES.set(0);
        rewriteRun(
          spec -> spec.recipe(new FindSizeComparedToZero()),
          java(
            """
              import java.util.Collections;
              class A {
                  boolean empty() {
                      return Collections.emptyList().size() == 0;
                  }
              }
              """,
            """
              import java.util.Collections;
              class A {
                  boolean empty() {
                      return /*~~>*/Collections.emptyList().size() == 0;
                  }
              }
              """
          ),
          java(
            """
              import java.util.List;
              class B {
                  boolean none(List<String> names) {
                      return names.size() == 0;
                  }
              }
              """,
            """
              import java.util.List;
              class B {
                  boolean none(List<String> names) {
                      return /*~~>*/names.size() == 0;
                  }
              }
              """
          )
        );
        assertThat(FindSizeComparedToZero.COMPILES).hasValue(1);
    }
}

/**
 * Top level, because the harness reruns no recipe declared inside a test class.
 */
class FindSizeComparedToZero extends Recipe {
    static final AtomicInteger COMPILES = new AtomicInteger();

    @Override
    public String getDisplayName() {
        return "Find a list's size compared to zero";
    }

    @Override
    public String getDescription() {
        return "Marks `size() == 0` on a list.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaTemplate sizeIsZero = JavaTemplate.builder("#{any(java.util.List)}.size() == 0")
          .doBeforeParseTemplate(stub -> COMPILES.incrementAndGet())
          .build();
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
                return sizeIsZero.matches(getCursor()) ? SearchResult.found(binary) : binary;
            }
        };
    }
}
