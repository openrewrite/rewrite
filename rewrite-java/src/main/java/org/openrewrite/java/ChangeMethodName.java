/*
 * Copyright 2020 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.function.Predicate;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodName extends Recipe {

    private static final String VALID_JAVA_METHOD_PATTERN = "^\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*$";
    private static final Predicate<String> IS_NOT_RESERVED_KEYWORD = s -> !JavaKeywordUtils.isReservedKeyword(s);
    private static final Predicate<String> IS_NOT_RESERVED_LITERAL = s -> !JavaKeywordUtils.isReservedLiteral(s);
    private static final Predicate<String> IS_VALID_METHOD_PATTERN = s -> s.matches(VALID_JAVA_METHOD_PATTERN);
    private static final Predicate<String> IS_VALID_METHOD_NAME = IS_NOT_RESERVED_KEYWORD
          .and(IS_NOT_RESERVED_LITERAL)
          .and(IS_VALID_METHOD_PATTERN);

    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_DESCRIPTION,
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method name",
            description = "The method name that will replace the existing name.",
            example = "any")
    String newMethodName;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Option(displayName = "Ignore type definition",
            description = "When set to `true` the definition of the old type will be left untouched. " +
                          "This is useful when you're replacing usage of a class but don't want to rename it.",
            required = false)
    @Nullable
    Boolean ignoreDefinition;

    @Override
    public String getDisplayName() {
        return "Change method name";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", methodPattern, newMethodName);
    }

    @Override
    public String getDescription() {
        return "Rename a method.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate()
              .and(MethodMatcher.validate(methodPattern))
              .and(Validated.test("newMethodName",
                    "should not be a Java Reserved Keyword.",
                    newMethodName,
                    IS_NOT_RESERVED_KEYWORD))
              .and(Validated.test("newMethodName",
                    "should not be a Java Reserved Literal.",
                    newMethodName,
                    IS_NOT_RESERVED_LITERAL))
              .and(Validated.test("newMethodName",
                    "should be a valid Java method name.",
                    newMethodName,
                    IS_VALID_METHOD_PATTERN));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> condition = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    if (Boolean.TRUE.equals(ignoreDefinition)) {
                        J j = new DeclaresMethod<>(methodPattern, matchOverrides).visitNonNull(cu, ctx);
                        if (cu != j) {
                            return cu;
                        }
                    } else {
                        cu = (JavaSourceFile) new DeclaresMethod<>(methodPattern, matchOverrides).visitNonNull(cu, ctx);
                        if (cu != tree) {
                            return cu;
                        }
                    }
                    return new UsesMethod<>(methodPattern, matchOverrides).visitNonNull(cu, ctx);
                }
                return super.visit(tree, ctx);
            }
        };

        return Preconditions.check(condition, new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
                if (IS_VALID_METHOD_NAME.test(newMethodName)) {
                    return super.isAcceptable(sourceFile, executionContext);
                }
                return false;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                J.NewClass newClass = getCursor().firstEnclosing(J.NewClass.class);
                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                boolean methodMatches = newClass != null && methodMatcher.matches(method, newClass) ||
                                        classDecl != null && methodMatcher.matches(method, classDecl);
                if (methodMatches) {
                    JavaType.Method type = m.getMethodType();
                    if (type != null) {
                        type = type.withName(newMethodName);
                    }
                    m = m.withName(m.getName().withSimpleName(newMethodName).withType(type))
                            .withMethodType(type);
                }
                return m;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(method) && !method.getSimpleName().equals(newMethodName)) {
                    JavaType.Method type = m.getMethodType();
                    if (type != null) {
                        type = type.withName(newMethodName);
                    }
                    m = m.withName(m.getName().withSimpleName(newMethodName).withType(type))
                            .withMethodType(type);
                }
                return m;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                if (methodMatcher.matches(m.getMethodType()) && !m.getReference().getSimpleName().equals(newMethodName)) {
                    JavaType.Method type = m.getMethodType();
                    if (type != null) {
                        type = type.withName(newMethodName);
                    }
                    m = m.withReference(m.getReference().withSimpleName(newMethodName)).withMethodType(type);
                }
                return m;
            }

            /**
             * The only time field access should be relevant to changing method names is static imports.
             * This exists to turn
             * import static com.abc.B.static1;
             * into
             * import static com.abc.B.static2;
             */
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess f = super.visitFieldAccess(fieldAccess, ctx);
                if (getCursor().getParentTreeCursor().getValue() instanceof J.Import && methodMatcher.isFullyQualifiedClassReference(f)) {
                    Expression target = f.getTarget();
                    if (target instanceof J.FieldAccess) {
                        String className = target.printTrimmed(getCursor());
                        String fullyQualified = className + "." + newMethodName;
                        return TypeTree.build(fullyQualified)
                                .withPrefix(f.getPrefix());
                    }
                }
                return f;
            }
        });
    }
}
