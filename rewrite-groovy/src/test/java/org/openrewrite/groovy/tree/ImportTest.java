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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class ImportTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/9473")
    @Test
    void annotatedImport() {
        rewriteRun(
          groovy(
            """
              // shared library
              @Library('my-pipeline-library') import com.acme.Deployer
              new Deployer(this).deploy()
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getStatements().getFirst()).isInstanceOf(G.AnnotatedImport.class);
                G.AnnotatedImport annotated = (G.AnnotatedImport) cu.getStatements().getFirst();
                assertThat(annotated.getLeadingAnnotations()).singleElement()
                  .extracting(J.Annotation::getSimpleName).isEqualTo("Library");
                assertThat(cu.getImports()).containsExactly(annotated.getImportDeclaration());
                assertThat(cu.withImports(cu.getImports())).isSameAs(cu);
                assertThat(cu.getPadding().withImports(cu.getPadding().getImports())).isSameAs(cu);
            })
          )
        );
    }

    @Test
    void annotationsOnLaterStaticAliasedImport() {
        rewriteRun(
          groovy(
            """
              import java.util.List
              // annotated static import
              @Deprecated
              @SuppressWarnings('unused')
              import static java.util.Collections.emptyList as empty;
              empty()
              """,
            spec -> spec.afterRecipe(cu -> {
                G.AnnotatedImport annotated = (G.AnnotatedImport) cu.getStatements().get(1);
                assertThat(annotated.getLeadingAnnotations()).hasSize(2);
                assertThat(annotated.getImportDeclaration().isStatic()).isTrue();
                assertThat(cu.getImports()).hasSize(2);
            })
          )
        );
    }

    @Test
    void grabOnImport() {
        rewriteRun(
          groovy(
            """
              @Grab('org.apache.commons:commons-lang3:3.12.0') import org.apache.commons.lang3.StringUtils
              StringUtils.isBlank('')
              """,
            spec -> spec.afterRecipe(cu -> {
                G.AnnotatedImport annotated = (G.AnnotatedImport) cu.getStatements().getFirst();
                assertThat(annotated.getLeadingAnnotations()).singleElement()
                  .extracting(J.Annotation::getSimpleName).isEqualTo("Grab");
            })
          )
        );
    }

    @Test
    void visitImportAnnotation() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new GroovyIsoVisitor<>() {
              @Override
              public J.Literal visitLiteral(J.Literal literal, ExecutionContext p) {
                  J.Literal l = super.visitLiteral(literal, p);
                  return "old-library".equals(l.getValue()) ?
                    l.withValue("new-library").withValueSource("'new-library'") : l;
              }
          })),
          groovy(
            "@Library('old-library') import com.acme.Deployer",
            "@Library('new-library') import com.acme.Deployer"
          )
        );
    }

    @Test
    void preserveAnnotatedImportsWhenReordering() {
        rewriteRun(
          groovy(
            """
              @Deprecated import java.util.Set
              @SuppressWarnings('unused') import java.util.List
              """,
            spec -> spec.afterRecipe(cu -> {
                var imports = new ArrayList<>(cu.getImports());
                Collections.reverse(imports);
                G.CompilationUnit reordered = cu.withImports(imports);
                assertThat(reordered.getImports()).containsExactlyElementsOf(imports);
                assertThat(reordered.getStatements().getFirst().getId())
                  .isEqualTo(cu.getStatements().get(1).getId());
                assertThat(((G.AnnotatedImport) reordered.getStatements().getFirst()).getLeadingAnnotations())
                  .singleElement().extracting(J.Annotation::getSimpleName).isEqualTo("SuppressWarnings");
            })
          )
        );
    }

    @Test
    void removingAnnotatedImportRemovesAnnotations() {
        rewriteRun(
          groovy(
            """
              @Deprecated import java.util.List
              println 'hello'
              """,
            spec -> spec.afterRecipe(cu -> {
                G.CompilationUnit withoutImports = cu.withImports(Collections.emptyList());
                assertThat(withoutImports.getImports()).isEmpty();
                assertThat(withoutImports.getStatements()).hasSize(1);
                assertThat(withoutImports.printAll()).doesNotContain("@Deprecated", "import java.util.List");
            })
          )
        );
    }

    @Test
    void classesRemainAfterAnnotatedImports() {
        rewriteRun(
          groovy(
            """
              package example

              @Deprecated import java.util.List

              class Example {}
              """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration renamed = cu.getClasses().getFirst()
                  .withName(cu.getClasses().getFirst().getName().withSimpleName("Renamed"));
                G.CompilationUnit updated = (G.CompilationUnit) cu.withClasses(Collections.singletonList(renamed));
                assertThat(updated.getStatements().getFirst()).isInstanceOf(G.AnnotatedImport.class);
                assertThat(updated.getStatements().get(1)).isSameAs(renamed);
            })
          )
        );
    }

    @Test
    void importFormattingMovesWithAnnotations() {
        rewriteRun(
          groovy(
            """
              @Deprecated import java.util.List
              """,
            spec -> spec.afterRecipe(cu -> {
                J.Import anImport = cu.getImports().getFirst();
                G.CompilationUnit formatted = cu.withImports(Collections.singletonList(
                  anImport.withPrefix(Space.format("\n"))));
                assertThat(formatted.printAll()).isEqualTo("\n" + cu.printAll());
            })
          )
        );
    }

    @Test
    void nestedImportAttributesEachClassSegment() {
        rewriteRun(
          groovy(
            """
              import java.util.Map.Entry
              """,
            spec -> spec.afterRecipe(cu -> {
                J.FieldAccess entry = (J.FieldAccess) cu.getImports().get(0).getQualid();
                J.FieldAccess map = (J.FieldAccess) entry.getTarget();
                J.FieldAccess util = (J.FieldAccess) map.getTarget();
                J.Identifier java = (J.Identifier) util.getTarget();

                // Each class segment carries its own resolved type; package segments carry none.
                assertThat(TypeUtils.asFullyQualified(entry.getType()))
                  .as("leaf 'Entry'").isNotNull()
                  .extracting(JavaType.FullyQualified::getFullyQualifiedName).isEqualTo("java.util.Map$Entry");
                assertThat(TypeUtils.asFullyQualified(map.getType()))
                  .as("'Map'").isNotNull()
                  .extracting(JavaType.FullyQualified::getFullyQualifiedName).isEqualTo("java.util.Map");
                assertThat(util.getType()).as("package segment 'util'").isNull();
                assertThat(java.getType()).as("package segment 'java'").isNull();
            })
          )
        );
    }

    @Test
    void classImport() {
        rewriteRun(
          groovy(
            """
              import java.util.List
              """
          )
        );
    }

    @Test
    void multipleImportsOnOneLine() {
        rewriteRun(
          groovy(
            """
              import java.util.List;import java.util.Set
              """
          )
        );
    }

    @Test
    void starImport() {
        rewriteRun(
          groovy(
            """
              import java.util.*
              """
          )
        );
    }

    @Test
    void staticImport() {
        rewriteRun(
          groovy(
            """
              import static java.util.Collections.singletonList
              """
          )
        );
    }

    @Test
    void staticStarImport() {
        rewriteRun(
          groovy(
            """
              import static java.util.Collections.*
              """
          )
        );
    }

    @Test
    void classImportAlias() {
        rewriteRun(
          groovy(
            """
              import java.util.List as L
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getEof()).isEqualTo(Space.EMPTY))
          )
        );
    }

    @Test
    void staticImportAlias() {
        rewriteRun(
          groovy(
            """
              import static java.util.Collections.singletonList as listOf
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getEof()).isEqualTo(Space.EMPTY))
          )
        );
    }

    @Test
    void duplicateImportsAndComments() {
        rewriteRun(
          groovy(
            """
                    import static      java.util.Collections.*    ;     import               static      java.util.Collections.*
                    //import static      java.util.Collections.*    ;     import               static      java.util.Collections.*
              import java.util.Collections.*  ; import static java.util.Collections.*
              import java.util.Collections.*
              import static java.util.Collections.singletonList as listOf
              import static java.util.Collections.singletonList as listOf
              import /*static java.util.Collections.singletonList;import static*/ java.util.Collections.*
              import /*static java.util.Collections.singletonList;


              import static java.util.Collections.**/java.util.Collections.*
              import /*static java.util.Collections.singletonList;


              import static java.util.Collections.**/static          java.util.Collections.*
              import static java.util.Collections.singletonList;import static java.util.Collections.*
              import java.util.Collections.*
              """
          )
        );
    }

    @Test
    void multiLineCommentBeforeStarStaticImport() {
        rewriteRun(
          groovy(
            """
            /*
             * Hello
             */
            import java.io.File
            import static java.lang.Math.*
            import java.nio.file.Path
            final String s = new String("s")
            """
          )
        );
    }

    @Test
    void twoSameImportStarStatements() {
        rewriteRun(
          groovy(
            """
            import static java.lang.Math.*
            import static java.lang.Math.*
            """
          )
        );
    }

    @Test
    void oneImportStaticStarSplitIntoTwoLines() {
        rewriteRun(
          groovy(
            """
            import/* export
            */static java.lang.Math.*
            """
          )
        );
    }
}
