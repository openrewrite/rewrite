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
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaParser;
import org.openrewrite.scala.ScalaVisitor;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

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
    void commentsAroundPatternDefinition() {
        rewriteRun(
          scala(
            """
              object Test {
                private /* a /* b */ c */ val (a, b) /* d /* e */ f */ = /* g /* h */ i */ (1, 2)
                private /* a /* b */ c */ lazy /* val */ val (c, d) = (1, 2)
                val e /* x /* y */ , */ , f = 1
                private /* lazy */ val (g, h) = (1, 2)
              }
              """
          )
        );
    }

    @Test
    void commentsAndLineBreaksAmongValModifiers() {
        rewriteRun(
          scala(
            """
              object Test {
                private /* c */ final val a = 1
                final /* val */ val b = 1
                private
                final val c = 1
              }
              """
          )
        );
    }

    @Test
    void softModifierBeforeVal() {
        rewriteRun(
          // Soft modifiers are not mapped yet, so `inline` is held as whitespace.
          spec -> spec.typeValidationOptions(TypeValidation.builder().allowNonWhitespaceInWhitespace(true).build()),
          scala(
            """
              object Test {
                inline val a = 1
                private inline val b = 2
                inline private val c = 3
              }
              """
          )
        );
    }

    @Test
    void accessQualifierAfterASpace() {
        rewriteRun(
          scala(
            """
              package p
              object Test {
                private [p] val a = 1
              }
              """
          )
        );
    }

    @Test
    void commentsAroundValDefinition() {
        rewriteRun(
          scala(
            """
              object Test {
                val x /* a /* b */ c */ : Int /* d /* e */ f */ = /* g /* h */ i */ 1
              }
              """
          )
        );
    }

    @SuppressWarnings("deprecation")
    @Test
    void patternDefinitionHoldingOnlyItsSourceTextStillPrints() {
        String source = "object Test {\n  val (a, b) = (1, 2)\n}";
        J cu = ScalaParser.builder().build().parse(source).map(J.class::cast).findFirst().orElseThrow();
        J asWrittenBefore = new ScalaVisitor<Integer>() {
            @Override
            public J visitPatternDefinition(S.PatternDefinition patDef, Integer p) {
                return new S.PatternDefinition(patDef.getId(), patDef.getPrefix(), patDef.getMarkers(),
                  null, null, null, null, null, "val (a, b) = (1, 2)");
            }
        }.visitNonNull(cu, 0);

        assertThat(((SourceFile) asWrittenBefore).printAll()).isEqualTo(source);
        assertThat(new ScalaVisitor<Integer>().visitNonNull(asWrittenBefore, 0)).isSameAs(asWrittenBefore);
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
                val (m, n): (Int, Int) = (1, 2)
                val _ = 4
              }
              trait Abstract {
                val q, r: Int
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                List<String> shapes = new ArrayList<>();
                List<String> boundTypes = new ArrayList<>();
                new ScalaVisitor<Integer>() {
                    @Override
                    public J visitIdentifier(J.Identifier identifier, Integer p) {
                        if (("a".equals(identifier.getSimpleName()) || "b".equals(identifier.getSimpleName())) &&
                            identifier.getFieldType() != null) {
                            boundTypes.add(identifier.getSimpleName() + ": " + identifier.getFieldType().getType());
                        }
                        return identifier;
                    }

                    @Override
                    public J visitPatternDefinition(S.PatternDefinition patDef, Integer p) {
                        shapes.add((patDef.getModifiers().getLast().getType() == J.Modifier.Type.Final ? "final " : "") +
                                   patDef.getModifiers().getLast().getKeyword() + " " + patDef.getPatterns().stream()
                          .map(pattern -> pattern.getClass().getSimpleName()).collect(Collectors.joining(",")) +
                                   (patDef.getTypeExpression() == null ? "" : " :") + (patDef.getInitializer() == null ? "" : " ="));
                        return super.visitPatternDefinition(patDef, p);
                    }
                }.visit(cu, 0);
                assertThat(boundTypes).containsExactly("a: int", "b: int");
                assertThat(shapes).containsExactly(
                  "final val TuplePattern =",
                  "var DeconstructionPattern =",
                  "final val TuplePattern =",
                  "final val Binding =",
                  "final val DeconstructionPattern =",
                  "final val Identifier,Identifier : =",
                  "final val TuplePattern : =",
                  "final val Wildcard =",
                  "final val Identifier,Identifier :"
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
