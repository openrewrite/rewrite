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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;


import static org.openrewrite.java.Assertions.java;

class MigrateToRewrite8Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateToRewrite8())
          .parser(JavaParser.fromJavaVersion()
            // .classpathFromResources(new InMemoryExecutionContext(), "rewrite-core-7.41.0-SNAPSHOT")
            .classpath(JavaParser.runtimeClasspath())
          );
    }

    @Test
    void deprecateVisitJavaSourceFile() {
        rewriteRun(
          java(
            """
              package org.openrewrite.staticanalysis;

              import org.openrewrite.*;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.RenameVariable;
              import org.openrewrite.java.tree.Flag;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaSourceFile;

              import java.time.Duration;
              import java.util.*;

              import static org.openrewrite.internal.NameCaseConvention.LOWER_CAMEL;

              public class RenamePrivateFieldsToCamelCase extends Recipe {

                  @Override
                  public String getDisplayName() {
                      return "Reformat private field names to camelCase";
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new RenameNonCompliantNames();
                  }

                  private static class RenameNonCompliantNames extends JavaIsoVisitor<ExecutionContext> {
                      @Override
                      public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
                          Map<J.VariableDeclarations.NamedVariable, String> renameVariablesMap = new LinkedHashMap<>();
                          Set<String> hasNameSet = new HashSet<>();

                          getCursor().putMessage("RENAME_VARIABLES_KEY", renameVariablesMap);
                          getCursor().putMessage("HAS_NAME_KEY", hasNameSet);
                          super.visitJavaSourceFile(cu, ctx);

                          renameVariablesMap.forEach((key, value) -> {
                              if (!hasNameSet.contains(value) && !hasNameSet.contains(key.getSimpleName())) {
                                  doAfterVisit(new RenameVariable<>(key, value));
                                  hasNameSet.add(value);
                              }
                          });
                          return cu;
                      }
                  }
              }
              """,
            """
              package org.openrewrite.staticanalysis;

              import org.openrewrite.*;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.RenameVariable;
              import org.openrewrite.java.tree.Flag;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaSourceFile;

              import java.time.Duration;
              import java.util.*;

              import static org.openrewrite.internal.NameCaseConvention.LOWER_CAMEL;

              public class RenamePrivateFieldsToCamelCase extends Recipe {

                  @Override
                  public String getDisplayName() {
                      return "Reformat private field names to camelCase";
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new RenameNonCompliantNames();
                  }

                  private static class RenameNonCompliantNames extends JavaIsoVisitor<ExecutionContext> {

                      @Override
                      public  @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                          if (tree instanceof JavaSourceFile) {
                              JavaSourceFile cu = (JavaSourceFile) tree;
                              Map<J.VariableDeclarations.NamedVariable, String> renameVariablesMap = new LinkedHashMap<>();
                              Set<String> hasNameSet = new HashSet<>();
            
                              getCursor().putMessage("RENAME_VARIABLES_KEY", renameVariablesMap);
                              getCursor().putMessage("HAS_NAME_KEY", hasNameSet);
                              super.visit(cu, ctx);

                              renameVariablesMap.forEach((key, value) -> {
                                  if (!hasNameSet.contains(value) && !hasNameSet.contains(key.getSimpleName())) {
                                      doAfterVisit(new RenameVariable<>(key, value));
                                      hasNameSet.add(value);
                                  }
                              });
                          }
                          return super.visit(tree, ctx);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void getSingleSourceApplicableTestToPreconditions() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.cleanup;

              import org.openrewrite.Applicability;
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.TreeVisitor;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.MethodMatcher;
              import org.openrewrite.java.search.UsesMethod;
              import org.openrewrite.java.tree.*;
              import org.openrewrite.java.PartProvider;

              import java.time.Duration;
              import java.util.ArrayList;
              import java.util.List;

              import static java.util.Collections.emptyList;
              import static java.util.Collections.singletonList;

              public class ChainStringBuilderAppendCalls extends Recipe {
                  private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");
                  private static J.Binary additiveBinaryTemplate = null;

                  @Override
                  public String getDisplayName() {
                      return "Chain `StringBuilder.append()` calls";
                  }

                  @Override
                  protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
                      return Applicability.or(new UsesMethod<>(STRING_BUILDER_APPEND),
                          new UsesMethod<>(STRING_BUILDER_APPEND));
                  }

                  @Override
                  protected JavaIsoVisitor<ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                              J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                              // do something
                              return m;
                          }
                      };
                  }
              }
              """,
            """
              package org.openrewrite.java.cleanup;

              import org.openrewrite.*;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.MethodMatcher;
              import org.openrewrite.java.search.UsesMethod;
              import org.openrewrite.java.tree.*;
              import org.openrewrite.java.PartProvider;

              import java.time.Duration;
              import java.util.ArrayList;
              import java.util.List;

              import static java.util.Collections.emptyList;
              import static java.util.Collections.singletonList;

              public class ChainStringBuilderAppendCalls extends Recipe {
                  private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");
                  private static J.Binary additiveBinaryTemplate = null;

                  @Override
                  public String getDisplayName() {
                      return "Chain `StringBuilder.append()` calls";
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return Preconditions.check(
                              Preconditions.or(new UsesMethod<>(STRING_BUILDER_APPEND),
                                      new UsesMethod<>(STRING_BUILDER_APPEND)), new JavaIsoVisitor<ExecutionContext>() {
                                  @Override
                                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                      J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                                      // do something
                                      return m;
                                  }
                              });
                  }
              }
              """
          )
        );
    }
}
