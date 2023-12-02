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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;


import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("all")
class MigrateRecipeToRewrite8Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateRecipeToRewrite8())
          .parser(JavaParser.fromJavaVersion()
            .classpath(JavaParser.runtimeClasspath())
          )
          .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample("`org.openrewrite.java.JavaVisitor.visitJavaSourceFile(JavaSourceFile cu, P p)` is removed, use `org.openrewrite.java.TreeVisitor.visit(@Nullable Tree tree, P p)` instead.")
    @Test
    void deprecateVisitJavaSourceFile() {
        // language=java
        rewriteRun(
          java(
            """
              package org.openrewrite.staticanalysis;

              import org.openrewrite.*;
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
                      public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
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

    @DocumentExample("`getSingleSourceApplicableTest()` is removed, change to use `Preconditions` instead. And also change `getVisitor()` to be public.")
    @Test
    void getSingleSourceApplicableTestToPreconditions() {
        // language=java
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

              public class ChainStringBuilderAppendCalls extends Recipe {
                  private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");

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

              public class ChainStringBuilderAppendCalls extends Recipe {
                  private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");

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

    @Test
    void visitAndCast() {
        // language=java
        rewriteRun(
          java(
            """
              package org.openrewrite.java.cleanup;

              import org.openrewrite.Cursor;
              import org.openrewrite.Incubating;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.UnwrapParentheses;
              import org.openrewrite.java.format.AutoFormatVisitor;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaSourceFile;
              import org.openrewrite.java.tree.Space;

              @Incubating(since = "7.0.0")
              public class SimplifyBooleanExpressionVisitor<P> extends JavaVisitor<P> {
                  private static final String MAYBE_AUTO_FORMAT_ME = "MAYBE_AUTO_FORMAT_ME";

                  @Override
                  public J visitJavaSourceFile(JavaSourceFile cu, P p) {
                      JavaSourceFile c = visitAndCast(cu, p, super::visitJavaSourceFile);
                      if (c != cu) {
                          doAfterVisit(new SimplifyBooleanExpressionVisitor<>());
                      }
                      return c;
                  }
              }
              """,
            """
              package org.openrewrite.java.cleanup;

              import org.openrewrite.Cursor;
              import org.openrewrite.Incubating;
              import org.openrewrite.Tree;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.UnwrapParentheses;
              import org.openrewrite.java.format.AutoFormatVisitor;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaSourceFile;
              import org.openrewrite.java.tree.Space;

              @Incubating(since = "7.0.0")
              public class SimplifyBooleanExpressionVisitor<P> extends JavaVisitor<P> {
                  private static final String MAYBE_AUTO_FORMAT_ME = "MAYBE_AUTO_FORMAT_ME";

                  @Override
                  public @Nullable J visit(@Nullable Tree tree, P p) {
                      if (tree instanceof JavaSourceFile) {
                          JavaSourceFile cu = (JavaSourceFile) tree;
                          JavaSourceFile c = visitAndCast(cu, p, super::visit);
                          if (c != cu) {
                              doAfterVisit(new SimplifyBooleanExpressionVisitor<>());
                          }
                      }
                      return super.visit(tree, p);
                  }
              }
              """
          )
        );
    }

    @Test
    void castReturnTypeForSuperVisit() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.cleanup;

              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.internal.ListUtils;
              import org.openrewrite.java.AnnotationMatcher;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.search.UsesType;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaSourceFile;
              import org.openrewrite.java.tree.JavaType;

              import java.time.Duration;
              import java.util.Collections;
              import java.util.List;
              import java.util.Set;

              public class UnnecessaryPrimitiveAnnotations extends Recipe {
                  private static final AnnotationMatcher CHECK_FOR_NULL_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.annotation.CheckForNull");
                  private static final AnnotationMatcher NULLABLE_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.annotation.Nullable");

                  @Override
                  public String getDisplayName() {
                      return "Remove Nullable and CheckForNull annotations from primitives";
                  }

                  @Override
                  protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                              doAfterVisit(new UsesType<>("javax.annotation.CheckForNull", false));
                              doAfterVisit(new UsesType<>("javax.annotation.Nullable", false));
                              return cu;
                          }
                      };
                  }

                  @Override
                  public JavaIsoVisitor<ExecutionContext> getVisitor() {

                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                              JavaSourceFile c = super.visitJavaSourceFile(cu, executionContext);
                              super.visitJavaSourceFile(cu, executionContext);
                              return c;
                          }
                      };
                  }
              }
              """,
            """
              package org.openrewrite.java.cleanup;

              import org.openrewrite.*;
              import org.openrewrite.internal.ListUtils;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.java.AnnotationMatcher;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.search.UsesType;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaSourceFile;
              import org.openrewrite.java.tree.JavaType;

              import java.time.Duration;
              import java.util.Collections;
              import java.util.List;
              import java.util.Set;

              public class UnnecessaryPrimitiveAnnotations extends Recipe {
                  private static final AnnotationMatcher CHECK_FOR_NULL_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.annotation.CheckForNull");
                  private static final AnnotationMatcher NULLABLE_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.annotation.Nullable");

                  @Override
                  public String getDisplayName() {
                      return "Remove Nullable and CheckForNull annotations from primitives";
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {

                      return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public @Nullable J visit(@Nullable Tree tree, ExecutionContext executionContext) {
                              if (tree instanceof JavaSourceFile) {
                                  JavaSourceFile cu = (JavaSourceFile) tree;
                                  doAfterVisit(new UsesType<>("javax.annotation.CheckForNull", false));
                                  doAfterVisit(new UsesType<>("javax.annotation.Nullable", false));
                              }
                              return super.visit(tree, executionContext);
                          }
                      }, new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public @Nullable J visit(@Nullable Tree tree, ExecutionContext executionContext) {
                              if (tree instanceof JavaSourceFile) {
                                  JavaSourceFile cu = (JavaSourceFile) tree;
                                  JavaSourceFile c = (JavaSourceFile) super.visit(cu, executionContext);
                                  super.visit(cu, executionContext);
                              }
                              return super.visit(tree, executionContext);
                          }
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void doNextToDoAfterVisit() {
        rewriteRun(
          java(
            """
              package org.openrewrite.staticanalysis;

              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.java.ChangePackage;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.J;

              import java.time.Duration;
              import java.util.Collections;
              import java.util.Set;

              public class LowercasePackage extends Recipe {
                  @Override
                  public String getDisplayName() {
                      return "Rename packages to lowercase";
                  }

                  @Override
                  public JavaIsoVisitor<ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.Package visitPackage(J.Package pkg, ExecutionContext executionContext) {
                              String packageText = pkg.getExpression().print(getCursor()).replaceAll("\\\\s", "");
                              String lowerCase = packageText.toLowerCase();
                              if(!packageText.equals(lowerCase)) {
                                  doNext(new ChangePackage(packageText, lowerCase, true));
                              }
                              return pkg;
                          }
                      };
                  }
              }
              """,
            """
              package org.openrewrite.staticanalysis;

              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.TreeVisitor;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.java.ChangePackage;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.J;

              import java.time.Duration;
              import java.util.Collections;
              import java.util.Set;

              public class LowercasePackage extends Recipe {
                  @Override
                  public String getDisplayName() {
                      return "Rename packages to lowercase";
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.Package visitPackage(J.Package pkg, ExecutionContext executionContext) {
                              String packageText = pkg.getExpression().print(getCursor()).replaceAll("\\\\s", "");
                              String lowerCase = packageText.toLowerCase();
                              if(!packageText.equals(lowerCase)) {
                                  // [Rewrite8 migration] Method `Recipe#doNext(..)` has been removed, you might want to change the recipe to be a scanning recipe, or just simply replace to use `TreeVisitor#doAfterVisit`, please follow the migration guide here: https://docs.openrewrite.org/changelog/8-1-2-release
                                  doNext(new ChangePackage(packageText, lowerCase, true));
                              }
                              return pkg;
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void addCommentToMigrateScanningRecipeManually() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.cleanup;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.*;
              import java.util.*;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class FixSerializableFields extends Recipe {

                  @Override
                  public String getDisplayName() {
                      return "Fields in a `Serializable` class should either be transient or serializable";
                  }

                  @Override
                  protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
                      return before;
                  }
              }
              """,
            """
              package org.openrewrite.java.cleanup;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.*;
              import java.util.*;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class FixSerializableFields extends Recipe {

                  @Override
                  public String getDisplayName() {
                      return "Fields in a `Serializable` class should either be transient or serializable";
                  }

                  // [Rewrite8 migration] This recipe uses the visit multiple sources method `visit(List<SourceFile> before, P p)`, needs to be migrated to use new introduced scanning recipe, please follow the migration guide here: https://docs.openrewrite.org/changelog/8-1-2-release
                  @Override
                  protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
                      return before;
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleReturnsInGetSingleSourceApplicableTest() {
        rewriteRun(
          java(
            """
              package org.openrewrite.kubernetes.rbac;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.*;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.kubernetes.KubernetesVisitor;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class AddRuleToRole extends Recipe {

                  @Option(displayName = "Optional file matcher",
                          description = "Matching files will be modified. This is a glob expression.",
                          required = false,
                          example = "**/pod-*.yml")
                  @Nullable
                  String fileMatcher;

                  public AddRuleToRole(@Nullable String fileMatcher) {
                      this.fileMatcher = fileMatcher;
                  }

                  @Override
                  public String getDisplayName() {
                      return "Add RBAC rules";
                  }
                  @Override
                  protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
                      if (fileMatcher != null) {
                          return new HasSourcePath(fileMatcher).getVisitor();
                      } else {
                          return null;
                      }
                  }

                  @Override
                  protected TreeVisitor<?, ExecutionContext> getVisitor() {
                      return null;
                  }
              }
              """,
            """
              package org.openrewrite.kubernetes.rbac;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.*;
              import org.openrewrite.internal.lang.Nullable;
              import org.openrewrite.kubernetes.KubernetesVisitor;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class AddRuleToRole extends Recipe {

                  @Option(displayName = "Optional file matcher",
                          description = "Matching files will be modified. This is a glob expression.",
                          required = false,
                          example = "**/pod-*.yml")
                  @Nullable
                  String fileMatcher;

                  public AddRuleToRole(@Nullable String fileMatcher) {
                      this.fileMatcher = fileMatcher;
                  }

                  @Override
                  public String getDisplayName() {
                      return "Add RBAC rules";
                  }
                  // [Rewrite8 migration] This getSingleSourceApplicableTest methods might have multiple returns, need manually migrate to use `Precondition#check()`, please follow the migration guide here: https://docs.openrewrite.org/changelog/8-1-2-release
                  @Override
                  protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
                      if (fileMatcher != null) {
                          return new HasSourcePath(fileMatcher).getVisitor();
                      } else {
                          return null;
                      }
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Disabled
    @Test
    void doAfterVisitRecipeIsRemoved() {
        // language=java
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

              public class ChainStringBuilderAppendCalls extends Recipe {
                  private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");

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
                              doAfterVisit(new ChangePackage("A", "B", true));
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

              public class ChainStringBuilderAppendCalls extends Recipe {
                  private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");

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
                                      // [Rewrite8 migration] TreeVisitor#doAfterVisit(Recipe) has been removed, it could be mistaken usage of `TreeVisitor#doAfterVisit(TreeVisitor<?, P> visitor)` here, please review code and see if it can be replaced.
                                      doAfterVisit(new ChangePackage("A", "B", true));
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
