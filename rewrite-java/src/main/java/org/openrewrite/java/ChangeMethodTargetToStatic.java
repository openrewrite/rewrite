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

    private class ChangeMethodTargetToStaticVisitor extends JavaIsoVisitor<ExecutionContext> {
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
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            Expression select = method.getSelect();
            if (!isAlreadyStaticCallOnTargetType(select, method) &&
                methodMatcher.matches(method, matchUnknownTypes)) {
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
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference m = super.visitMemberReference(memberRef, ctx);
            Expression containing = memberRef.getContaining();
            if (!isAlreadyStaticCallOnTargetType(containing, memberRef) &&
                methodMatcher.matches(memberRef)) {
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
    }
}
