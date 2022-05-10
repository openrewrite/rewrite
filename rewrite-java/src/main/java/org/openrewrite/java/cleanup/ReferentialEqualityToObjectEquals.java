/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.singletonList;

public class ReferentialEqualityToObjectEquals extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replace referential equality operators with Object equals method invocations when the operands both override `Object.equals(Object obj)`";
    }

    @Override
    public String getDescription() {
        return "Using `==` or `!=` compares object references, not the equality of two objects. " +
                "This modifies code where both sides of a binary operation (`==` or `!=`) override `Object.equals(Object obj)` " +
                "except when the comparison is within an overridden `Object.equals(Object obj)` method declaration itself." +
                "WARNING, The resulting transformation must be carefully reviewed since any modifications change the programs semantics.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1698");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReferentialEqualityToObjectEqualityVisitor();
    }

    private static class ReferentialEqualityToObjectEqualityVisitor extends JavaVisitor<ExecutionContext> {
        private static final JavaType TYPE_OBJECT = JavaType.buildType("java.lang.Object");

        private static J.MethodInvocation asEqualsMethodInvocation(J.Binary binary, @Nullable JavaType.FullyQualified selectType) {
            return new J.MethodInvocation(
                    Tree.randomId(),
                    binary.getPrefix(),
                    Markers.EMPTY,
                    new JRightPadded<>(binary.getLeft().withPrefix(Space.EMPTY), Space.EMPTY, Markers.EMPTY),
                    null,
                    new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "equals", JavaType.Primitive.Boolean, null),
                    JContainer.build(singletonList(new JRightPadded<>(binary.getRight().withPrefix(Space.EMPTY), Space.EMPTY, Markers.EMPTY))),
                    new JavaType.Method(
                            null,
                            Flag.Public.getBitMask(),
                            selectType,
                            "equals",
                            JavaType.Primitive.Boolean,
                            singletonList("o"),
                            singletonList(TYPE_OBJECT),
                            null, null
                    )
            );
        }

        private static J.Unary asNegatedUnary(J.MethodInvocation mi) {
            return new J.Unary(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    new JLeftPadded<>(Space.EMPTY, J.Unary.Type.Not, Markers.EMPTY),
                    mi,
                    JavaType.Primitive.Boolean
            );
        }

        @Override
        public J visitBinary(J.Binary binary, ExecutionContext ctx) {
            if (!isExcludedBinary(binary)) {
                JavaType.FullyQualified leftType = TypeUtils.asFullyQualified(binary.getLeft().getType());
                Optional<JavaType.Method> leftEqualsMethod = TypeUtils.findDeclaredMethod(leftType, "equals", singletonList(TYPE_OBJECT));
                JavaType.FullyQualified rightType = TypeUtils.asFullyQualified(binary.getRight().getType());
                Optional<JavaType.Method> rightEqualsMethod = TypeUtils.findDeclaredMethod(rightType, "equals", singletonList(TYPE_OBJECT));

                if (leftEqualsMethod.isPresent() && rightEqualsMethod.isPresent()) {
                    JavaType.Method leftEqualsOverride = TypeUtils.findOverriddenMethod(leftEqualsMethod.get()).orElse(null);
                    JavaType.Method rightEqualsOverride = TypeUtils.findOverriddenMethod(rightEqualsMethod.get()).orElse(null);
                    if (leftEqualsOverride != null && rightEqualsOverride != null && !TypeUtils.isOfClassType(leftEqualsMethod.get().getDeclaringType(), "java.lang.Enum")) {
                        J after = null;
                        if (binary.getOperator() == J.Binary.Type.Equal) {
                            after = asEqualsMethodInvocation(binary, leftType);
                        } else if (binary.getOperator() == J.Binary.Type.NotEqual) {
                            J.MethodInvocation mi = asEqualsMethodInvocation(binary, leftType);
                            after = asNegatedUnary(mi);
                        }
                        if (after != null) {
                            return after;
                        }
                    }
                }
            }
            return super.visitBinary(binary, ctx);
        }

        private boolean isExcludedBinary(J.Binary binary) {
            return isInEqualsOverrideMethod() || isPrimitiveNull(binary.getRight()) || hasThisIdentifier(binary) || isBoxedTypeComparison(binary)
                    || TypeUtils.isOfClassType(binary.getLeft().getType(), "java.lang.Enum") || TypeUtils.isOfClassType(binary.getRight().getType(), "java.lang.Enum");
        }

        private boolean isInEqualsOverrideMethod() {
            J.MethodDeclaration md = getCursor().firstEnclosing(J.MethodDeclaration.class);
            if (md != null && "equals".equals(md.getSimpleName())) {
                return TypeUtils.isOverride(md.getMethodType());
            }
            return false;
        }

        private boolean isPrimitiveNull(Expression expression) {
            return expression.getType() == JavaType.Primitive.Null;
        }

        private boolean hasThisIdentifier(J.Binary binary) {
            return ((binary.getRight() instanceof J.Identifier && "this".equals(((J.Identifier)binary.getRight()).getSimpleName()))
                    || ((binary.getLeft() instanceof J.Identifier && "this".equals(((J.Identifier)binary.getLeft()).getSimpleName()))));
        }

        private boolean isBoxedTypeComparison(J.Binary binary) {
            if (binary.getLeft() != null && binary.getLeft().getType() != null
                    && binary.getRight() != null && binary.getRight().getType() != null) {
                return  isBoxed(binary.getRight().getType())
                        && isBoxed(binary.getLeft().getType());
            }
            return false;
        }
        private boolean isBoxed(JavaType type) {
            return type instanceof JavaType.Primitive
                    || TypeUtils.isOfClassType(type,"java.lang.Byte")
                    || TypeUtils.isOfClassType(type,"java.lang.Character")
                    || TypeUtils.isOfClassType(type,"java.lang.Short")
                    || TypeUtils.isOfClassType(type,"java.lang.Integer")
                    || TypeUtils.isOfClassType(type,"java.lang.Long")
                    || TypeUtils.isOfClassType(type,"java.lang.Float")
                    || TypeUtils.isOfClassType(type,"java.lang.Double")
                    || TypeUtils.isOfClassType(type,"java.lang.Boolean");
        }

    }

}
