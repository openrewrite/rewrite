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
package org.openrewrite.java;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Cursor;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class OrderImportsLegacyLayoutTest implements RewriteTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void retainNestedEnumMembersWithLegacyModelStyle(boolean removeUnused) {
        ImportLayoutStyle legacyStyle = new ImportLayoutStyle(5, 3, IntelliJ.importLayout().getLayout(), emptyList()) {
            @Override
            public List<JRightPadded<J.Import>> orderImports(List<JRightPadded<J.Import>> imports,
                    Collection<JavaType.FullyQualified> classpath, boolean dirty, @Nullable Cursor cursor) {
                if (imports.isEmpty()) {
                    return imports;
                }
                // Captured legacy behavior: State and State.* are incorrectly folded to Thread.*.
                JRightPadded<J.Import> first = imports.get(0);
                J.FieldAccess qualid = first.getElement().getQualid();
                return singletonList(first.withElement(first.getElement().withQualid(
                  qualid.withName(qualid.getName().withSimpleName("*")))));
            }
        };
        String before = """
              import static java.lang.Thread.State;
              import static java.lang.Thread.State.*;

              class Example {
                  State first = RUNNABLE;
                  State second = NEW;
              }
              """;
        rewriteRun(
          spec -> spec.recipe(new OrderImports(removeUnused, null)).parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(randomId(), "legacy", "Legacy model style", null, emptySet(), singletonList(legacyStyle))))),
          removeUnused ? java(before, """
              import static java.lang.Thread.State;
              import static java.lang.Thread.State.NEW;
              import static java.lang.Thread.State.RUNNABLE;

              class Example {
                  State first = RUNNABLE;
                  State second = NEW;
              }
              """) : java(before)
        );
    }
}
