/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaVisitor;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class VariableDeclarationsTest implements RewriteTest {

    @Test
    void extraSpaceBetweenAnnotationAndVal() {
        rewriteRun(
          scala(
            """
            class Test {
              @deprecated   val x = 1
            }
            """
          )
        );
    }

    @Test
    void annotationNewlineBeforeModifier() {
        rewriteRun(
          scala(
            """
            object Test {
              @deprecated
              private var x = 1
            }
            """
          )
        );
    }

    @Test
    void extraSpaceBetweenModifierAndVal() {
        rewriteRun(
          scala(
            """
            class Test {
              private   val x = 1
            }
            """
          )
        );
    }

    @Test
    void modifierExtraSpaceBeforeVar() {
        rewriteRun(
          scala(
            """
            object Test {
              private  var x = 1
            }
            """
          )
        );
    }

    @Test
    void annotationExtraSpaceBeforeVar() {
        rewriteRun(
          scala(
            """
            object Test {
              @deprecated  var x = 1
            }
            """
          )
        );
    }

    @Test
    void annotationNewlineBeforeVar() {
        rewriteRun(
          scala(
            """
            object Test {
              @deprecated
              var x = 1
            }
            """
          )
        );
    }

    @Test
    void valDeclaration() {
        rewriteRun(
          scala("val x = 5")
        );
    }

    @Test
    void varDeclaration() {
        rewriteRun(
          scala("var y = 10")
        );
    }

    @Test
    void valWithTypeAnnotation() {
        rewriteRun(
          scala("val x: Int = 5")
        );
    }

    @Test
    void varWithTypeAnnotation() {
        rewriteRun(
          scala("var y: String = \"hello\"")
        );
    }

    @Test
    void lazyVal() {
        rewriteRun(
          scala("lazy val z = compute()")
        );
    }

    @Test
    void patternDeclaration() {
        rewriteRun(
          scala("val (a, b) = (1, 2)")
        );
    }

    @Test
    void patternDefinitionsHoldTheirPatternsAsTrees() {
        rewriteRun(
          scala(
            """
              object Test {
                val (a, b) = (1, 2)
                var Some(x) = Option(1)
                private lazy val ((c, d), e) = ((1, 2), 3)
                @deprecated val p @ (f, g) = (1, 2)
                val h :: t = List(1, 2)
                val K, L: Int = 3
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                List<String> shapes = new ArrayList<>();
                new ScalaVisitor<Integer>() {
                    @Override
                    public J visitPatternDefinition(S.PatternDefinition patDef, Integer p) {
                        shapes.add(patDef.getModifiers().getLast().getKeyword() + " " + patDef.getPatterns().stream()
                          .map(pattern -> pattern.getClass().getSimpleName()).collect(Collectors.joining(",")) +
                                   (patDef.getTypeExpression() == null ? "" : " :") + (patDef.getInitializer() == null ? "" : " ="));
                        return patDef;
                    }
                }.visit(cu, 0);
                assertThat(shapes).containsExactly(
                  "val TuplePattern =",
                  "var DeconstructionPattern =",
                  "val TuplePattern =",
                  "val Binding =",
                  "val DeconstructionPattern =",
                  "val Identifier,Identifier : ="
                );
            })
          )
        );
    }

    @Test
    void multipleDeclarations() {
        rewriteRun(
          scala(
            """
            val x = 1
            val y = 2
            val z = 3
            """
          )
        );
    }

    @Test
    void privateVal() {
        rewriteRun(
          scala("private val secret = 42")
        );
    }

    @Test
    void protectedVar() {
        rewriteRun(
          scala("protected var count = 0")
        );
    }

    @Test
    void finalVal() {
        rewriteRun(
          scala("final val constant = 3.14")
        );
    }

    @Test
    void valWithComplexType() {
        rewriteRun(
          scala("val list: List[Int] = List(1, 2, 3)")
        );
    }

    @Test
    void valWithNoInitializer() {
        rewriteRun(
          scala("var x: Int = _")
        );
    }

    @Test
    void tuplePatternWithExtraSpace() {
        rewriteRun(
          scala(
            """
            object Test {
              val pair = (1, 2)
              val (a, b) = pair
            }
            """
          )
        );
    }

    @Test
    void significantCharactersInComments() {
        // visitValDef — `(` in line comment between `val` and tuple pattern
        rewriteRun(
          scala(
            """
            val // (
            (a, b) = (1, 2)
            """
          )
        );
        // visitValDef — `_` in line comment between `val` and underscore pattern
        rewriteRun(
          scala(
            """
            val // _
            _ = println("x")
            """
          )
        );
    }
}
