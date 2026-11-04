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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class JavaParserReleaseTest implements RewriteTest {

    private static final String ELEMENT_RECORD = """
      package com.example.element;
      public interface Record {
          Column getColumn(int i);
      }
      """;

    private static final String ELEMENT_COLUMN = """
      package com.example.element;
      public class Column {
      }
      """;

    private static final String FILTER_TRANSFORMER = """
      package com.example.core;
      import com.example.element.*;
      public class FilterTransformer {
          public Record evaluate(Record record) {
              Column column = record.getColumn(0);
              return record;
          }
      }
      """;

    @MinimumJava17
    @Test
    void ownRecordImportedOnDemandResolvesForJava8Sources() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().release(8)),
          java(ELEMENT_RECORD),
          java(ELEMENT_COLUMN),
          java(
            FILTER_TRANSFORMER,
            spec -> spec.afterRecipe(cu -> assertThat(typesNamed(cu, "Record"))
              .isNotEmpty()
              .allSatisfy(type -> assertThat(TypeUtils.isOfClassType(type, "com.example.element.Record")).isTrue()))
          )
        );
    }

    @MinimumJava17
    @Test
    void ownRecordImportedOnDemandIsAmbiguousWithoutRelease() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(ELEMENT_RECORD),
          java(ELEMENT_COLUMN),
          java(
            FILTER_TRANSFORMER,
            spec -> spec.afterRecipe(cu -> assertThat(typesNamed(cu, "Record"))
              .isNotEmpty()
              .allSatisfy(type -> assertThat(type).isInstanceOf(JavaType.Unknown.class)))
          )
        );
    }

    @MinimumJava11
    @Test
    void ownModuleImportedOnDemandResolvesForJava8Sources() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().release(8)),
          java(
            """
              package com.example.plugin;
              public class Module {
              }
              """
          ),
          java(
            """
              package com.example.core;
              import com.example.plugin.*;
              class Loader {
                  Module load() {
                      return new Module();
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(typesNamed(cu, "Module"))
              .isNotEmpty()
              .allSatisfy(type -> assertThat(TypeUtils.isOfClassType(type, "com.example.plugin.Module")).isTrue()))
          )
        );
    }

    @Test
    void classesOfTheReleaseStillResolve() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().release(8)),
          java(
            """
              class A {
                  String s = String.valueOf(Integer.MAX_VALUE);
                  Character.UnicodeBlock block = Character.UnicodeBlock.of('a');
                  Thread thread = Thread.currentThread();
                  StringBuilder sb = new StringBuilder(s).append(Math.max(1, 2));
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void recordStaysForSourcesOfAReleaseThatHasIt() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().release(17)),
          java(
            """
              record Point(int x, int y) {
                  Record asRecord() {
                      return this;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(typesNamed(cu, "Record"))
              .isNotEmpty()
              .allSatisfy(type -> assertThat(TypeUtils.isOfClassType(type, "java.lang.Record")).isTrue()))
          )
        );
    }

    private static List<JavaType> typesNamed(J.CompilationUnit cu, String simpleName) {
        List<JavaType> types = new ArrayList<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                if (simpleName.equals(identifier.getSimpleName()) && !(getCursor().getParentTreeCursor().getValue() instanceof J.Import) &&
                    !(getCursor().getParentTreeCursor().getValue() instanceof J.FieldAccess)) {
                    types.add(identifier.getType());
                }
                return identifier;
            }
        }.visit(cu, 0);
        return types;
    }
}
