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

import java.util.List;
import java.util.function.Predicate;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodName extends Recipe {

    private static final String VALID_JAVA_METHOD_PATTERN = "^\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*$";
    private static final Predicate<String> IS_NOT_RESERVED_KEYWORD = s -> !JavaKeywordUtils.isReservedKeyword(s);
    private static final Predicate<String> IS_NOT_RESERVED_LITERAL = s -> !JavaKeywordUtils.isReservedLiteral(s);
    private static final Predicate<String> IS_VALID_METHOD_PATTERN = s -> s.matches(VALID_JAVA_METHOD_PATTERN);
    private static final Predicate<String> LOOKS_LIKE_METHOD_PATTERN =
          s -> s != null && (s.indexOf('(') >= 0 || s.indexOf('.') >= 0 || s.indexOf(' ') >= 0);
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

    String displayName = "Change method name";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", methodPattern, newMethodName);
    }

    String description = "Rename a method.";

    @Override
    public Validated<Object> validate() {
        return super.validate()
              .and(MethodMatcher.validate(methodPattern))
              .and(Validated.test("newMethodName",
                    "should not be a reserved keyword.",
                    newMethodName,
                    IS_NOT_RESERVED_KEYWORD))
              .and(Validated.test("newMethodName",
                    "should not be a reserved literal.",
                    newMethodName,
                    IS_NOT_RESERVED_LITERAL))
              .and(Validated.test("newMethodName",
                    LOOKS_LIKE_METHOD_PATTERN.test(newMethodName) ?
                          "should be just the new name of the method, not a method pattern." :
                          "should be a valid method name.",
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
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                if (IS_VALID_METHOD_NAME.test(newMethodName)) {
                    return super.isAcceptable(sourceFile, ctx);
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
                if (methodMatches && !declaresSiblingWithNewName(method)) {
                    JavaType.Method type = m.getMethodType();
                    if (type != null) {
                        type = type.withName(newMethodName);
                    }
                    m = m.withName(m.getName().withSimpleName(newMethodName).withType(type))
                            .withMethodType(type);
                }
                return m;
            }

            /**
             * A declaration should not be renamed when the enclosing type already declares a method
             * with the new name and the same parameter types, as that would produce a duplicate method.
             */
            private boolean declaresSiblingWithNewName(J.MethodDeclaration method) {
                J.Block body = getCursor().firstEnclosing(J.Block.class);
                if (body == null) {
                    return false;
                }
                for (Statement statement : body.getStatements()) {
                    if (statement != method && statement instanceof J.MethodDeclaration) {
                        J.MethodDeclaration sibling = (J.MethodDeclaration) statement;
                        if (sibling.getSimpleName().equals(newMethodName) && sameParameterTypes(method, sibling)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private boolean sameParameterTypes(J.MethodDeclaration m1, J.MethodDeclaration m2) {
                JavaType.Method t1 = m1.getMethodType();
                JavaType.Method t2 = m2.getMethodType();
                if (t1 == null || t2 == null) {
                    // Without type attribution, assume a clash when the arity matches
                    return m1.getParameters().size() == m2.getParameters().size();
                }
                return CalledMethod.sameTypes(t1.getParameterTypes(), t2.getParameterTypes());
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(method) && !method.getSimpleName().equals(newMethodName)) {
                    JavaType.Method type = m.getMethodType();
                    if (type != null) {
                        type = renamed(m, type);
                    }
                    m = m.withName(m.getName().withSimpleName(newMethodName).withType(type))
                            .withMethodType(type);
                }
                return m;
            }

            /**
             * The method named {@code newMethodName} that {@code call} calls, such as a {@code select()} that
             * returns another type than the {@code transform()} it replaces, or {@code type} with the new name where
             * that can't be told.
             */
            private JavaType.Method renamed(J.MethodInvocation call, JavaType.Method type) {
                JavaType.Method renamed = type.withName(newMethodName);
                JavaType.FullyQualified declaringType = type.getDeclaringType();
                JavaType.FullyQualified receiver = call.getSelect() != null ?
                        TypeUtils.asFullyQualified(call.getSelect().getType()) : thisType(declaringType);
                // Kotlin, Groovy and the other languages resolve a call by rules of their own
                if (receiver == null || getCursor().firstEnclosing(J.CompilationUnit.class) == null) {
                    return renamed;
                }
                JavaType.Method called;
                if (!TypeUtils.isAssignableTo(declaringType.getFullyQualifiedName(), receiver)) {
                    // an earlier recipe gave the receiver another type, as in `multi.select().byTakingFirstItems(2)`
                    called = CalledMethod.find(receiver, newMethodName, call.getArguments());
                } else {
                    // Where this recipe renames the old method's declaration too, the call calls that method, which
                    // its type's members don't hold. The declaration keeps its name only where its type declares the
                    // new name with the same parameters, so only then is a method of the new name there to call.
                    JavaType.Method original = declared(declaringType, type.getName(),
                            method -> TypeUtils.isOfTypeIgnoringGenerics(method, type));
                    if (original == null) {
                        return renamed;
                    }
                    JavaType.Method target = declared(declaringType, newMethodName,
                            method -> CalledMethod.sameTypes(method.getParameterTypes(), original.getParameterTypes()));
                    if (target == null) {
                        return renamed;
                    }
                    // the receiver's own type may override that method, or overload it for these arguments
                    List<JavaType.Method> candidates = CalledMethod.candidates(receiver, newMethodName,
                            call.getArguments());
                    called = candidates.size() == 1 ? candidates.get(0) :
                            CalledMethod.find(receiver, newMethodName, call.getArguments());
                    if (called != null && TypeUtils.isOfType(called, target) &&
                        TypeUtils.isOfType(original.getReturnType(), target.getReturnType()) &&
                        CalledMethod.sameTypes(original.getThrownExceptions(), target.getThrownExceptions())) {
                        // javac inferred the call's types from this signature, type variables of the method included
                        return target.withDeclaringType(declaringType)
                                .withReturnType(type.getReturnType())
                                .withParameterTypes(type.getParameterTypes())
                                .withThrownExceptions(type.getThrownExceptions());
                    }
                }
                if (called == null || CalledMethod.isErased(receiver, called, call.getArguments())) {
                    return renamed;
                }
                called = CalledMethod.calledOn(receiver, called);
                return called == null ? renamed : called;
            }

            /**
             * The type of {@code this} for the call at the cursor, which has no receiver and calls a method of
             * {@code declaringType}. It is the class around the call where that is a subtype. An anonymous class
             * doesn't tell its type and a static import has none, so there it is {@code declaringType} as a raw type.
             */
            private JavaType.FullyQualified thisType(JavaType.FullyQualified declaringType) {
                Object enclosing = getCursor().dropParentUntil(tree -> tree instanceof J.ClassDeclaration ||
                        tree instanceof J.NewClass && ((J.NewClass) tree).getBody() != null ||
                        tree == Cursor.ROOT_VALUE).getValue();
                if (enclosing instanceof J.ClassDeclaration) {
                    JavaType.FullyQualified type = ((J.ClassDeclaration) enclosing).getType();
                    if (type != null && TypeUtils.isAssignableTo(declaringType.getFullyQualifiedName(), type)) {
                        return type;
                    }
                }
                return declaringType instanceof JavaType.Parameterized ?
                        ((JavaType.Parameterized) declaringType).getType() : declaringType;
            }

            /**
             * The one method that {@code type} itself declares with the name {@code name} that passes {@code test}.
             */
            private JavaType.@Nullable Method declared(JavaType.FullyQualified type, String name,
                                                       Predicate<JavaType.Method> test) {
                JavaType.Method declared = null;
                for (JavaType.Method method : type.getMethods()) {
                    if (method.getName().equals(name) && test.test(method)) {
                        if (declared != null) {
                            return null;
                        }
                        declared = method;
                    }
                }
                return declared;
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
