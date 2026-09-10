/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

class GroovyTemplateTest implements RewriteTest {

    // Replaces the invocation named `placeholder`, the shape Gradle DSL recipes need: a new statement in a script body
    private static Recipe replacePlaceholder(String template) {
        return toRecipe(() -> new GroovyVisitor<>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (!"placeholder".equals(method.getSimpleName())) {
                    return super.visitMethodInvocation(method, ctx);
                }
                return GroovyTemplate.builder(template)
                  .build()
                  .apply(getCursor(), method.getCoordinates().replace());
            }
        });
    }

    // Replaces `placeholder(...)`, passing the placeholder's own arguments to the template as parameters
    private static Recipe replacePlaceholderWithArguments(String template) {
        return toRecipe(() -> new GroovyVisitor<>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (!"placeholder".equals(method.getSimpleName())) {
                    return super.visitMethodInvocation(method, ctx);
                }
                return GroovyTemplate.builder(template)
                  .build()
                  .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().toArray());
            }
        });
    }

    // A Groovy script holds its statements directly, so a coordinate on one can only be reached from the file
    private static Recipe insertAtTopLevel(boolean before) {
        return toRecipe(() -> new GroovyVisitor<>() {
            @Override
            public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                G.CompilationUnit c = (G.CompilationUnit) super.visitCompilationUnit(cu, ctx);
                if (c.getStatements().size() != 1) {
                    return c;
                }
                Statement only = c.getStatements().get(0);
                return GroovyTemplate.builder("ext['x'] = 'y'")
                  .build()
                  .apply(updateCursor(c), before ? only.getCoordinates().before() : only.getCoordinates().after());
            }
        });
    }

    @DocumentExample
    @Test
    void replaceContextFreeStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GroovyVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  return GroovyTemplate.builder("println(\"foo\")")
                    .build()
                    .apply(getCursor(), multiVariable.getCoordinates().replace());
              }
          })),
          groovy(
            """
              class Test {
                  def foo() {
                      boolean b1 = 1 == 2
                  }
              }
              """,
            """
              class Test {
                  def foo() {
                      println("foo")
                  }
              }
              """
          ));
    }

    @Test
    void replaceAnnotationArgumentsOnMethodWithNestedClassSibling() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GroovyVisitor<>() {
              @Override
              public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("SuppressWarnings".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null &&
                      annotation.getArguments().stream().noneMatch(a -> a.toString().contains("all"))) {
                      return GroovyTemplate.builder("\"all\"")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                  }
                  return annotation;
              }
          })),
          groovy(
            """
              class A {
                  static class Sibling {
                  }

                  @SuppressWarnings("unchecked")
                  def method() {}
              }
              """,
            """
              class A {
                  static class Sibling {
                  }

                  @SuppressWarnings("all")
                  def method() {}
              }
              """
          ));
    }

    @Test
    void statementWithEmptyClosure() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholder(
            """
              configurations.all {
              }
              """
          )),
          groovy(
            """
              placeholder()
              """,
            """
              configurations.all {
              }
              """
          ));
    }

    @Test
    void nestedBlock() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholder(
            """
              java {
                  sourceCompatibility = 11
              }
              """
          )),
          groovy(
            """
              placeholder()
              """,
            """
              java {
                  sourceCompatibility = 11
              }
              """
          ));
    }

    @Test
    void assignmentWithSubscript() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholder("ext['x'] = 'y'")),
          groovy(
            """
              placeholder()
              """,
            """
              ext['x'] = 'y'
              """
          ));
    }

    @Test
    void methodInvocationWithTrailingClosureArgument() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholder(
            """
              tasks.named('test') {
                  useJUnitPlatform()
              }
              """
          )),
          groovy(
            """
              placeholder()
              """,
            """
              tasks.named('test') {
                  useJUnitPlatform()
              }
              """
          ));
    }

    @Test
    void substituteExpressionIntoClosureBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GroovyVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (!"placeholder".equals(method.getSimpleName())) {
                      return super.visitMethodInvocation(method, ctx);
                  }
                  return GroovyTemplate.builder(
                      """
                        configurations.all {
                            #{any()}
                        }
                        """)
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
              }
          })),
          groovy(
            """
              placeholder(exclude(group: 'commons-logging'))
              """,
            """
              configurations.all {
                  exclude(group: 'commons-logging')
              }
              """
          ));
    }

    @Test
    void statementWithClosureInsideMethod() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholder(
            """
              configurations.all {
              }
              """
          )),
          groovy(
            """
              class Test {
                  def foo() {
                      placeholder()
                  }
              }
              """,
            """
              class Test {
                  def foo() {
                      configurations.all {
                      }
                  }
              }
              """
          ));
    }

    @Test
    void closureWithExplicitParameter() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholder(
            """
              configurations.all { config ->
                  config.exclude group: 'log4j'
              }
              """
          )),
          groovy(
            """
              placeholder()
              """,
            """
              configurations.all { config ->
                  config.exclude group: 'log4j'
              }
              """
          ));
    }

    @Test
    void reindentStatementsPouredIntoATemplatedShell() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GroovyVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (!"outer".equals(method.getSimpleName())) {
                      return super.visitMethodInvocation(method, ctx);
                  }
                  J.Block outerBody = (J.Block) ((J.Lambda) method.getArguments().get(0)).getBody();
                  // A closure's last statement is an implicit return
                  J.MethodInvocation inner = (J.MethodInvocation) ((J.Return) outerBody.getStatements().get(0)).getExpression();
                  List<Statement> poured = ((J.Block) ((J.Lambda) inner.getArguments().get(0)).getBody()).getStatements();

                  J.MethodInvocation shell = GroovyTemplate.builder(
                      """
                        configurations.all {
                        }
                        """)
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace());
                  J.Lambda shellLambda = (J.Lambda) shell.getArguments().get(0);
                  J.Block shellBody = (J.Block) shellLambda.getBody();
                  return autoIndent(shell
                      .withArguments(List.of(shellLambda.withBody(shellBody.withStatements(poured)))),
                    ctx, getCursor().getParentOrThrow());
              }
          })),
          groovy(
            """
              outer {
                  inner {
                      foo()
                      bar()
                  }
              }
              """,
            """
              configurations.all {
                  foo()
                  bar()
              }
              """
          ));
    }

    @Test
    void insertAfterTopLevelScriptStatement() {
        rewriteRun(
          spec -> spec.recipe(insertAtTopLevel(false)),
          groovy(
            """
              plugins {
                  id 'java'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              ext['x'] = 'y'
              """
          ));
    }

    @Test
    void insertBeforeTopLevelScriptStatement() {
        rewriteRun(
          spec -> spec.recipe(insertAtTopLevel(true)),
          groovy(
            """
              plugins {
                  id 'java'
              }
              """,
            """
              ext['x'] = 'y'
              plugins {
                  id 'java'
              }
              """
          ));
    }

    @Test
    void insertBeforeTopLevelScriptStatementKeepsLicenseHeaderOnTop() {
        rewriteRun(
          spec -> spec.recipe(insertAtTopLevel(true)),
          groovy(
            """
              // Copyright
              plugins {
                  id 'java'
              }
              """,
            """
              // Copyright
              ext['x'] = 'y'
              plugins {
                  id 'java'
              }
              """
          ));
    }

    @Test
    void commandSyntaxSurvivesASubstitutedArgument() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholderWithArguments(
            """
              flatDir {
                  dirs #{any()}
              }
              """
          )),
          groovy(
            """
              placeholder('libs')
              """,
            """
              flatDir {
                  dirs 'libs'
              }
              """
          ));
    }

    @Test
    void commandSyntaxWithSeveralSubstitutedArguments() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholderWithArguments(
            """
              flatDir {
                  dirs #{any()}, #{any()}, #{any()}
              }
              """
          )),
          groovy(
            """
              placeholder('libs', 'moreLibs', 'evenMoreLibs')
              """,
            """
              flatDir {
                  dirs 'libs', 'moreLibs', 'evenMoreLibs'
              }
              """
          ));
    }

    @Test
    void commandSyntaxAlongsideAParenthesizedCall() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholderWithArguments(
            """
              flatDir {
                  dirs #{any()}
                  println(#{any()})
              }
              """
          )),
          groovy(
            """
              placeholder('libs', 'done')
              """,
            """
              flatDir {
                  dirs 'libs'
                  println('done')
              }
              """
          ));
    }

    @Test
    void parenthesesWrittenInTheTemplateAreKept() {
        rewriteRun(
          spec -> spec.recipe(replacePlaceholderWithArguments(
            """
              flatDir {
                  dirs(#{any()})
              }
              """
          )),
          groovy(
            """
              placeholder('libs')
              """,
            """
              flatDir {
                  dirs('libs')
              }
              """
          ));
    }

    @Test
    void replaceArgumentsOfExplicitConstructorInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GroovyVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("super".equals(method.getSimpleName()) && method.getMethodType() != null && method.getArguments().size() == 1) {
                      return JavaTemplate.builder("message, null")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replaceArguments());
                  }
                  return method;
              }
          })),
          groovy(
            """
              @groovy.transform.CompileStatic
              class A extends RuntimeException {
                  A(String message) {
                      super(message)
                  }
              }
              """,
            """
              @groovy.transform.CompileStatic
              class A extends RuntimeException {
                  A(String message) {
                      super(message, null)
                  }
              }
              """
          ));
    }
}
