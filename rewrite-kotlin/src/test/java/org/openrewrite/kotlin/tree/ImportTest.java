/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("UnusedReceiverParameter")
class ImportTest implements RewriteTest {

    @Test
    void jdkImport() {
        rewriteRun(
          kotlin("import   java.util.ArrayList")
        );
    }

    @Test
    void kotlinImport() {
        rewriteRun(
          kotlin("import   kotlin.collections.List")
        );
    }

    @CsvSource({
      "import kotlin.collections.List,false",
      "import java.util.regex.Pattern.CASE_INSENSITIVE,true",
    })
    @ParameterizedTest
    void staticImports(String _import, Boolean isStatic) {
        rewriteRun(
          kotlin("%s".formatted(_import),
            spec -> spec.afterRecipe(cu ->
              new KotlinIsoVisitor<Integer>() {
                @Override
                public J.Import visitImport(J.Import _import, Integer i) {
                    assertThat(_import.isStatic()).isEqualTo(isStatic);
                    return super.visitImport(_import, i);
                }
            }.visit(cu, 0)))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "java.util./* extension */ArrayList",
      "java.util./*extension*/ArrayList",
      "java./**/util.ArrayList",
      "java.util/* before dot */.ArrayList",
      "java /* package */ . util . /* name */ ArrayList",
      "java.util./*$ identifier_type {*/ArrayList/*$}*/",
      "java.util./*$ gui_graphics_type {*/ArrayList/*$}*/ as L",
      "java.util./* comment.with.dots and $ */ArrayList",
      "java./* outer /* nested */ comment */util.ArrayList",
      "java.`util`./* name */`ArrayList`",
      "java.util./** name */ArrayList"
    })
    void commentsInImportPath(String path) {
        rewriteRun(
          kotlin(
            "import " + path,
            spec -> spec.afterRecipe(cu -> {
                J.FieldAccess qualid = cu.getImports().getFirst().getQualid();
                assertThat(qualid.getSimpleName()).isEqualTo("ArrayList");
                assertThat(qualid.isFullyQualifiedClassReference("java.util.ArrayList")).isTrue();
            })
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "java.util./* wildcard */*",
      "java./* package */util.*",
      "java.util /* before dot */ . /* after dot */ *"
    })
    void commentsInWildcardImport(String path) {
        rewriteRun(
          kotlin(
            "import " + path,
            spec -> spec.afterRecipe(cu -> {
                J.Import anImport = cu.getImports().getFirst();
                assertThat(anImport.getQualid().getSimpleName()).isEqualTo("*");
                assertThat(anImport.getPackageName()).isEqualTo("java.util");
            })
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "import kotlin.*",
      "import /* prefix */kotlin.*",
      "import   kotlin /* before dot */ . /* after dot */ *"
    })
    void rootPackageWildcardImport(String source) {
        rewriteRun(
          kotlin(
            source,
            spec -> spec.afterRecipe(cu -> {
                J.Import anImport = cu.getImports().getFirst();
                assertThat(anImport.getQualid().getSimpleName()).isEqualTo("*");
                assertThat(anImport.getPackageName()).isEqualTo("kotlin");
            })
          )
        );
    }

    @Test
    void wildCard() {
        rewriteRun(
          kotlin("import kotlin.collections.*")
        );
    }

    @Test
    void inlineImport() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Target {
                  inline fun method ( ) { }
              }
              """
          ),
          kotlin(
            """
              import a.b.method

              class A
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/158")
    @Test
    void methodName() {
        rewriteRun(
          kotlin("fun <T : Any> Class<T>.createInstance() {}"),
          kotlin(
            "import   createInstance /*C1*/",
            spec -> spec.afterRecipe(cu ->
                assertThat(cu.getImports().getFirst().getPackageName()).isEmpty())
          )
        );
    }

    @Test
    void alias() {
        rewriteRun(
          kotlin(
            """
              import kotlin.collections.List as L
              import kotlin.collections.Set as S

              class T
              """
          )
        );
    }

    @Test
    void aliasFieldAccess() {
        rewriteRun(
          kotlin(
            """
              import kotlin.Int as Number
              var max = Number.MAX_VALUE
              """
          )
        );
    }

    @SuppressWarnings("RedundantSemicolon")
    @Test
    void importWithTrailingSemiColon() {
        rewriteRun(
          kotlin(
            """
              import kotlin . collections . List ;

              class T
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/412")
    @Test
    void aliasFromSamePackage() {
        rewriteRun(
          kotlin("class Foo"),
          kotlin(
            """
              import Foo as Bar

              class Test
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/570")
    @Test
    void escapedImport() {
        rewriteRun(
          kotlin(
            """
              import org.`should be equal to`
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/564")
    @Test
    void quotedImportWithDollar() {
        rewriteRun(
          kotlin(
            """
              import my.org.`$x`
              """
          )
        );
    }

    @Test
    void interfaceInformation() {
        rewriteRun(
          kotlin(
            """
              package org.example
              interface SuperShared {
                  fun one() = "one"
              }
              interface Shared : SuperShared
              class A {
                  companion object : Shared
              }
              """
          ),
          kotlin(
            """
              import org.example.A.Companion.one
              """,
            spec -> spec.afterRecipe(cu -> {
                JavaType firstImportType = cu.getImports().getFirst().getQualid().getType();
                JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(firstImportType);

                //noinspection DataFlowIssue
                assertThat(fullyQualified.getOwningClass().getInterfaces())
                  .singleElement()
                  .satisfies(sharedInterface -> {
                      assertThat(sharedInterface.getFullyQualifiedName()).isEqualTo("org.example.Shared");
                      assertThat(sharedInterface.getInterfaces())
                        .singleElement()
                        .satisfies(it -> {
                            assertThat(it.getFullyQualifiedName()).isEqualTo("org.example.SuperShared");
                            assertThat(it.getMethods()).singleElement().extracting(JavaType.Method::getName).isEqualTo("one");
                        });
                  });
              }
            )
          )
        );
    }
}
