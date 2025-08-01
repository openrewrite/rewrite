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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("RedundantNullableReturnType")
class FieldAccessTest implements RewriteTest {

    @Test
    void fieldAccessOnParameterizedType() {
        rewriteRun(
          kotlin(
            """
              class Foo {
                val s =/*1*/java/*2*/./*3*/util/*4*/./*5*/ArrayList<String>()/*6*/./*7*/toString()
              }
              """
          )
        );
    }

    @Test
    void fieldAccessOnIdentifier() {
        rewriteRun(
          kotlin(
            """
              class Foo {
                val sb = /*1*/java/*2*/./*3*/lang/*4*/./*5*/StringBuilder()/*6*/./*7*/toString()
              }
              """
          )
        );
    }

    @Test
    void thisAccess() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  var id : String = ""
                  fun setId ( id : String ) {
                      this . id = id
                  }
              }
              """
          )
        );
    }

    @Test
    void notNullAssertionAfterFieldAccess() {
        rewriteRun(
          kotlin(
            """
              class A {
                val a : String? = null
              }
              val x = A().a!!
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/18")
    @Test
    void superAccess() {
        rewriteRun(
          kotlin(
            """
              open class A {
                  val id : Int = 0
              }
              class B : A ( ) {
                  fun getId ( ) : Int {
                      return super . id
                  }
              }
              """
          )
        );
    }

    @Test
    void constructorDelegationWithExpression() {
        rewriteRun(
          kotlin(
            """
              open class Super(val id : Int)
              class Test(val id2 : Int) : @Suppress Super(1 + 3)
              """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration test = (J.ClassDeclaration) cu.getStatements().get(1);
                assertThat(test.getImplements()).satisfiesExactly(
                  superType -> {
                      K.ConstructorInvocation call = (K.ConstructorInvocation) superType;
                      assertThat(((JavaType.FullyQualified) call.getType()).getFullyQualifiedName()).isEqualTo("Super");
                      assertThat(((J.Identifier) call.getTypeTree()).getSimpleName()).isEqualTo("Super");
                      assertThat(call.getArguments()).satisfiesExactly(
                        id -> assertThat(id).isInstanceOf(J.Binary.class)
                      );
                  }
                );
                assertThat(test.getBody().getStatements()).satisfiesExactly(
                  stmt -> {
                      J.MethodDeclaration constr = (J.MethodDeclaration) stmt;
                      assertThat(constr.getParameters()).satisfiesExactly(
                        id2 -> assertThat(id2).isInstanceOf(J.VariableDeclarations.class)
                      );
                      assertThat(constr.getBody()).isNull();
                  }
                );
            })
          )
        );
    }

    @Test
    void nullSafeDereference() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  val property = 42
              }
              fun method ( test : Test ? ) {
                  val a = test ?. property
              }
              """
          )
        );
    }

    @Test
    void elvisOperator() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  val value : Int ? = 42
              }
              fun method ( test : Test ) {
                  val a = test . value ?: null
              }
              """
          )
        );
    }

    @Test
    void qualifier() {
        rewriteRun(
          kotlin(
            """
              import java.nio.ByteBuffer

              private val crlf : ByteBuffer = ByteBuffer . wrap( "\\r\\n" . toByteArray ( ) )
              """
          )
        );
    }

    @Test
    void platformFieldType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                  if ("pattern".equals(variable.getSimpleName())) {
                      JavaType.Variable variableType = variable.getVariableType();
                      assertThat(variableType).isNotNull();
                      assertThat(variableType.getName()).isEqualTo("pattern");
                      assertThat(variableType.getType().toString()).isEqualTo("java.util.regex.Pattern");
                  }
                  return super.visitVariable(variable, ctx);
              }
          })),
          kotlin(
            """
              val pattern = java . util . regex . Pattern . compile ( ".*" )
              """
          )
        );
    }

    @Test
    void propertyFieldType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                  if ("MIN_VALUE".equals(fieldAccess.getSimpleName())) {
                      JavaType.Variable fieldType = fieldAccess.getName().getFieldType();
                      assertThat(fieldType).isNotNull();
                      assertThat(fieldType.getName()).isEqualTo("MIN_VALUE");
                      assertThat(fieldType.getType().toString()).isEqualTo("kotlin.Int");
                      if (fieldType.getOwner() != null) {
                          assertThat(fieldType.getOwner().toString()).isEqualTo("kotlin.Int$Companion");
                      }
                  }
                  return super.visitFieldAccess(fieldAccess, ctx);
              }
          })),
          kotlin(
            """
              val i = Int.MIN_VALUE
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/133")
    @Test
    void qualifiedThis() {
        rewriteRun(
          kotlin(
            """
              class A {
                  inner class B {
                      val a = this@A
                      val b = this@B
                  }
              }
              """
          )
        );
    }

    @Test
    void dataClassFieldAccessRetainsNullableMarker() {
        // language=kotlin
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                  if ("completed".equals(fieldAccess.getSimpleName())) {
                      // TODO Type is not nullable, but field is nullable
                  }
                  return super.visitFieldAccess(fieldAccess, ctx);
              }
          })),
          kotlin(
            """
              data class Todo(val completed: Boolean?)
              fun main() {
                val todo = Todo(null)
                val isCompleted = todo.completed
              }
              """
          )
        );
    }
}
