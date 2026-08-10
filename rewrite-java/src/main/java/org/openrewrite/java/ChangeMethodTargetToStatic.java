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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodTargetToStatic extends Recipe {

    /**
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "The original method call may or may not be a static method invocation. " + MethodMatcher.METHOD_PATTERN_DESCRIPTION,
            example = "com.google.common.collect.ImmutableSet of(..)")
    String methodPattern;

    @Option(displayName = "Fully-qualified target type name",
            description = "A fully-qualified class name of the type upon which the static method is defined.",
            example = "java.util.Set")
    String fullyQualifiedTargetTypeName;

    @Option(displayName = "Return type after change",
            description = "Sometimes changing the target type also changes the return type. In the Guava example, changing from `ImmutableSet#of(..)` to `Set#of(..)` widens the return type from Guava's `ImmutableSet` to just `java.util.Set`.",
            example = "java.util.Set",
            required = false)
    @Nullable
    String returnType;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Option(displayName = "Match unknown types",
            description = "When enabled, include method invocations which appear to match if full type information is missing. " +
                          "Using matchUnknownTypes can improve recipe resiliency for an AST with missing type information, but " +
                          "also increases the risk of false-positive matches on unrelated method invocations.",
            required = false)
    @Nullable
    Boolean matchUnknownTypes;

    public ChangeMethodTargetToStatic(String methodPattern, String fullyQualifiedTargetTypeName,
                                      @Nullable String returnType, @Nullable Boolean matchOverrides) {
        this(methodPattern, fullyQualifiedTargetTypeName, returnType, matchOverrides, false);
    }

    @JsonCreator
    public ChangeMethodTargetToStatic(String methodPattern, String fullyQualifiedTargetTypeName, @Nullable String returnType, @Nullable Boolean matchOverrides, @Nullable Boolean matchUnknownTypes) {
        this.methodPattern = methodPattern;
        this.fullyQualifiedTargetTypeName = fullyQualifiedTargetTypeName;
        this.returnType = returnType;
        this.matchOverrides = matchOverrides;
        this.matchUnknownTypes = matchUnknownTypes;
    }

    String displayName = "Change method target to static";

    String description = "Change method invocations to static method calls.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean matchUnknown = Boolean.TRUE.equals(matchUnknownTypes);
        ChangeMethodTargetToStaticVisitor visitor = new ChangeMethodTargetToStaticVisitor(new MethodMatcher(methodPattern, matchOverrides), matchUnknown);
        return matchUnknown ? visitor : Preconditions.check(new UsesMethod<>(methodPattern, matchOverrides), visitor);
    }

    private class ChangeMethodTargetToStaticVisitor extends JavaVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;
        private final boolean matchUnknownTypes;
        private final JavaType.FullyQualified classType = JavaType.ShallowClass.build(fullyQualifiedTargetTypeName);

        public ChangeMethodTargetToStaticVisitor(MethodMatcher methodMatcher, boolean matchUnknownTypes) {
            this.methodMatcher = methodMatcher;
            this.matchUnknownTypes = matchUnknownTypes;
        }

        /**
         * Check if the method call is already a static call on the target type.
         */
        private boolean isAlreadyStaticCallOnTargetType(@Nullable Expression target, MethodCall methodCall) {
            boolean isStatic = methodCall.getMethodType() != null && methodCall.getMethodType().hasFlags(Flag.Static);
            boolean isSameReceiverType = target != null && TypeUtils.isOfClassType(target.getType(), fullyQualifiedTargetTypeName);
            boolean calledOnTargetType = target instanceof J.Identifier && ((J.Identifier) target).getFieldType() == null;
            return isStatic && isSameReceiverType && calledOnTargetType;
        }

        /**
         * Java evaluates the expression qualifying a static method invocation and then discards its value,
         * without a null check. This only drops an expression whose evaluation cannot be observed: a type name,
         * {@code this} (possibly qualified), or a simple non-volatile variable read (class initialization
         * aside). As a deliberate exception that preserves the {@code new A().staticMethod()} shape this recipe
         * exists to rewrite, an instantiation whose arguments are themselves discardable is also dropped, even
         * though a constructor can run arbitrary code and throw. A method invocation, a dereference, an array
         * access, a cast or any other expression may mutate state or throw, so the invocation is left alone
         * instead.
         */
        private boolean isSafeToDiscard(@Nullable Expression expression) {
            Expression expr = Expression.unwrap(expression);
            if (expr == null || expr instanceof J.Empty || expr instanceof J.Literal) {
                return true;
            }
            if (expr instanceof J.Identifier) {
                JavaType.Variable fieldType = ((J.Identifier) expr).getFieldType();
                return fieldType == null || !fieldType.hasFlags(Flag.Volatile);
            }
            if (expr instanceof J.FieldAccess) {
                return isTypeReference(expr);
            }
            if (expr instanceof J.NewClass) {
                // Instantiating a type only to call a static method on it is the case this recipe was written
                // for, so the instantiation itself is still discarded even though a constructor can throw.
                // Its arguments are not covered by that, so they have to be discardable in their own right.
                J.NewClass newClass = (J.NewClass) expr;
                if (newClass.getBody() != null || newClass.getEnclosing() != null) {
                    return false;
                }
                for (Expression argument : newClass.getArguments()) {
                    if (!isSafeToDiscard(argument)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        /**
         * A qualifier that is itself an invocation this recipe rewrites needs no preservation: the recipe's
         * long-standing treatment of such chains is to collapse them, so that with a fluent
         * {@code a.Legacy value()} pattern {@code legacy.value().value()} becomes {@code Modern.value()}.
         * Blocking that collapse would leave the rewritten qualifier behind as the receiver of the outer
         * call, producing {@code Modern.value().value()}, which no longer resolves once the migrated method
         * is static on the target type. The qualifier's own receiver is checked recursively, so a chain
         * rooted in an expression that cannot be discarded is left entirely unchanged instead.
         */
        private boolean collapsesOntoTargetType(@Nullable Expression expression) {
            Expression expr = Expression.unwrap(expression);
            if (!(expr instanceof J.MethodInvocation)) {
                return false;
            }
            J.MethodInvocation qualifier = (J.MethodInvocation) expr;
            return !isAlreadyStaticCallOnTargetType(qualifier.getSelect(), qualifier) &&
                   methodMatcher.matches(qualifier, matchUnknownTypes) &&
                   (isSafeToDiscard(qualifier.getSelect()) || collapsesOntoTargetType(qualifier.getSelect()));
        }

        /**
         * Check if the expression only names a type or is {@code this} (possibly qualified, as in
         * {@code Outer.this}), so that evaluating it always succeeds without any observable effect. Unlike an
         * invocation, a member reference evaluates and null checks its qualifier when the reference is created,
         * so it only replaces a qualifier of that shape with the target type.
         */
        private boolean isTypeReference(@Nullable Expression expression) {
            if (expression instanceof J.Identifier) {
                J.Identifier identifier = (J.Identifier) expression;
                return identifier.getFieldType() == null || "this".equals(identifier.getSimpleName());
            }
            if (expression instanceof J.FieldAccess) {
                J.Identifier name = ((J.FieldAccess) expression).getName();
                return (name.getFieldType() == null || "this".equals(name.getSimpleName())) &&
                       name.getType() instanceof JavaType.FullyQualified;
            }
            return false;
        }

        /**
         * Transform the method type to reflect the new declaring type and static flag.
         */
        private JavaType.Method transformMethodType(JavaType.Method methodType) {
            JavaType.Method transformedType = methodType.withDeclaringType(classType);
            if (!methodType.hasFlags(Flag.Static)) {
                Set<Flag> flags = new LinkedHashSet<>(methodType.getFlags());
                flags.add(Flag.Static);
                transformedType = transformedType.withFlags(flags);
            }
            if (returnType != null) {
                JavaType returnTypeType = JavaType.ShallowClass.build(returnType);
                transformedType = transformedType.withReturnType(returnTypeType);
            }
            return transformedType;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
            Expression select = method.getSelect();
            if (!isAlreadyStaticCallOnTargetType(select, method) &&
                methodMatcher.matches(method, matchUnknownTypes) &&
                (isSafeToDiscard(select) || collapsesOntoTargetType(select))) {
                JavaType.Method transformedType = null;
                if (method.getMethodType() != null) {
                    maybeRemoveImport(method.getMethodType().getDeclaringType());
                    transformedType = transformMethodType(method.getMethodType());
                }
                if (m.getSelect() == null) {
                    maybeAddImport(fullyQualifiedTargetTypeName, m.getSimpleName(), !matchUnknownTypes);
                } else {
                    maybeAddImport(fullyQualifiedTargetTypeName, !matchUnknownTypes);
                    m = method.withSelect(
                            new J.Identifier(randomId(),
                                    select == null ?
                                            Space.EMPTY :
                                            select.getPrefix(),
                                    Markers.EMPTY,
                                    emptyList(),
                                    classType.getClassName(),
                                    classType,
                                    null
                            )
                    );
                }
                m = m.withMethodType(transformedType)
                        .withName(m.getName().withType(transformedType));
            }
            return m;
        }

        @Override
        public J visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference m = (J.MemberReference) super.visitMemberReference(memberRef, ctx);
            Expression containing = memberRef.getContaining();
            if (!isAlreadyStaticCallOnTargetType(containing, memberRef) &&
                methodMatcher.matches(memberRef) &&
                (isTypeReference(containing) || collapsesOntoTargetType(containing))) {
                JavaType.Method transformedType = null;
                if (memberRef.getMethodType() != null) {
                    maybeRemoveImport(memberRef.getMethodType().getDeclaringType());
                    transformedType = transformMethodType(memberRef.getMethodType());
                }
                maybeAddImport(fullyQualifiedTargetTypeName, !matchUnknownTypes);
                m = memberRef.withContaining(
                        new J.Identifier(randomId(),
                                containing.getPrefix(),
                                Markers.EMPTY,
                                emptyList(),
                                classType.getClassName(),
                                classType,
                                null
                        )
                );
                m = m.withMethodType(transformedType);
            }
            return m;
        }

        @Override
        public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = (J.NewClass) super.visitNewClass(newClass, ctx);
            if (n.getBody() != null || n.getEnclosing() != null) {
                return n;
            }
            if (!methodPattern.contains("<constructor>") && !methodPattern.contains("<init>")) {
                return n;
            }
            if (methodMatcher.matches(n)) {
                String methodName;
                JavaType.Method transformedType = null;
                if (n.getConstructorType() != null) {
                    methodName = n.getConstructorType().getConstructorName();
                    if (methodName == null) {
                        return n;
                    }
                    maybeRemoveImport(n.getConstructorType().getDeclaringType());
                    transformedType = transformMethodType(n.getConstructorType())
                            .withName(methodName);
                } else {
                    return n;
                }

                maybeAddImport(fullyQualifiedTargetTypeName, !matchUnknownTypes);

                J.Identifier selectId = new J.Identifier(
                        randomId(),
                        n.getPrefix(),
                        Markers.EMPTY,
                        emptyList(),
                        classType.getClassName(),
                        classType,
                        null
                );

                J.Identifier nameId = new J.Identifier(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        emptyList(),
                        methodName,
                        transformedType,
                        null
                );

                return new J.MethodInvocation(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        JRightPadded.build(selectId),
                        null,
                        nameId,
                        n.getPadding().getArguments(),
                        transformedType
                );
            }
            return n;
        }
    }
}
