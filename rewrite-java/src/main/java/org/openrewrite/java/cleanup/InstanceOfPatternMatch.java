/*
 * Copyright 2021 the original author or authors.
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.VariableNameUtils;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER;

@Incubating(since = "7.36.0")
@Value
@EqualsAndHashCode(callSuper = true)
public class InstanceOfPatternMatch extends Recipe {

    @Override
    public String getDisplayName() {
        return "Changes code to use Java 17's `instanceof` pattern matching";
    }

    @Override
    public String getDescription() {
        return "Adds pattern variables to `instanceof` expressions wherever the same (side effect free) expression is referenced in a corresponding type cast expression within the flow scope of the `instanceof`."
                + " Currently, this recipe supports `if` statements and ternary operator expressions.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesJavaVersion<>(17);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public @Nullable J postVisit(J tree, ExecutionContext executionContext) {
                J result = super.postVisit(tree, executionContext);
                InstanceOfPatternReplacements original = getCursor().getMessage("flowTypeScope");
                if (original != null && !original.isEmpty()) {
                    return UseInstanceOfPatternMatching.refactor(result, original, getCursor());
                }
                return result;
            }

            @Override
            public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext executionContext) {
                instanceOf = (J.InstanceOf) super.visitInstanceOf(instanceOf, executionContext);
                if (instanceOf.getPattern() != null || !instanceOf.getSideEffects().isEmpty()) {
                    return instanceOf;
                }

                Cursor maybeReplacementRoot = null;
                J additionalContext = null;
                boolean flowScopeBreakEncountered = false;
                for (Iterator<Cursor> it = getCursor().getPathAsCursors(); it.hasNext(); ) {
                    Cursor next = it.next();
                    Object value = next.getValue();
                    if (value instanceof J.Binary) {
                        J.Binary binary = (J.Binary) value;
                        if (!flowScopeBreakEncountered && binary.getOperator() == J.Binary.Type.And) {
                            additionalContext = binary;
                        } else {
                            flowScopeBreakEncountered = true;
                        }
                    } else if (value instanceof Statement) {
                        maybeReplacementRoot = next;
                        break;
                    }
                }

                if (maybeReplacementRoot != null) {
                    J root = maybeReplacementRoot.getValue();
                    Set<J> contexts = new HashSet<>();
                    if (!flowScopeBreakEncountered) {
                        if (root instanceof J.If) {
                            contexts.add(((J.If) root).getThenPart());
                        } else if (root instanceof J.Ternary) {
                            contexts.add(((J.Ternary) root).getTruePart());
                        }
                    }
                    if (additionalContext != null) {
                        contexts.add(additionalContext);
                    }

                    if (!contexts.isEmpty()) {
                        InstanceOfPatternReplacements replacements = maybeReplacementRoot
                                .computeMessageIfAbsent("flowTypeScope", k -> new InstanceOfPatternReplacements(root));
                        replacements.registerInstanceOf(instanceOf, contexts);
                    }
                }
                return instanceOf;
            }

            @Override
            public J visitTypeCast(J.TypeCast typeCast, ExecutionContext executionContext) {
                J result = super.visitTypeCast(typeCast, executionContext);
                if (result instanceof J.TypeCast) {
                    InstanceOfPatternReplacements replacements = getCursor().getNearestMessage("flowTypeScope");
                    if (replacements != null) {
                        replacements.registerTypeCast((J.TypeCast) result, getCursor());
                    }
                }
                return result;
            }
        };
    }

    @Data
    private static class ExpressionAndType {
        private final Expression expression;
        private final JavaType type;
    }

    @Data
    private static class InstanceOfPatternReplacements {
        private final J root;
        private final Map<ExpressionAndType, J.InstanceOf> instanceOfs = new HashMap<>();
        private final Map<J.InstanceOf, Set<J>> contexts = new HashMap<>();
        private final Map<J.TypeCast, J.InstanceOf> replacements = new HashMap<>();

        public void registerInstanceOf(J.InstanceOf instanceOf, Set<J> contexts) {
            org.openrewrite.java.tree.Expression expression = instanceOf.getExpression();
            JavaType type = toJavaType((TypedTree) instanceOf.getClazz());

            Optional<ExpressionAndType> existing = instanceOfs.keySet().stream()
                    .filter(k -> k.getType().equals(type)
                            && SemanticallyEqual.areEqual(k.getExpression(), expression))
                    .findAny();
            if (!existing.isPresent()) {
                instanceOfs.put(new ExpressionAndType(expression, type), instanceOf);
                this.contexts.put(instanceOf, contexts);
            }
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        public void registerTypeCast(J.TypeCast typeCast, Cursor cursor) {
            Expression expression = typeCast.getExpression();
            JavaType type = toJavaType(typeCast.getClazz().getTree());

            Optional<ExpressionAndType> match = instanceOfs.keySet().stream()
                    .filter(k -> k.getType().equals(type)
                            && SemanticallyEqual.areEqual(k.getExpression(), expression))
                    .findAny();
            if (match.isPresent()) {
                J.InstanceOf instanceOf = instanceOfs.get(match.get());
                Set<J> validContexts = contexts.get(instanceOf);
                for (Iterator<?> it = cursor.getPath(); it.hasNext(); ) {
                    Object next = it.next();
                    if (validContexts.contains(next)) {
                        replacements.put(typeCast, instanceOf);
                        break;
                    } else if (root == next) {
                        break;
                    }
                }
            }
        }

        public boolean isEmpty() {
            return replacements.isEmpty();
        }

        public J.InstanceOf processInstanceOf(J.InstanceOf instanceOf, Cursor cursor) {
            @Nullable JavaType type = toJavaType((TypeTree) instanceOf.getClazz());
            String name = patternVariableName(instanceOf, cursor);
            J.InstanceOf result = instanceOf.withPattern(new J.Identifier(
                    randomId(),
                    Space.build(" ", emptyList()),
                    Markers.EMPTY,
                    name,
                    type,
                    null));

            // update entry in replacements to share the pattern variable name
            for (Map.Entry<J.TypeCast, J.InstanceOf> entry : replacements.entrySet()) {
                if (entry.getValue() == instanceOf) {
                    entry.setValue(result);
                }
            }
            return result;
        }

        @Nullable
        public J processTypeCast(J.TypeCast typeCast, Cursor cursor) {
            J.InstanceOf instanceOf = replacements.get(typeCast);
            if (instanceOf != null && instanceOf.getPattern() != null) {
                String name = ((J.Identifier) instanceOf.getPattern()).getSimpleName();
                TypedTree owner = cursor.firstEnclosing(J.MethodDeclaration.class);
                owner = owner != null ? owner : cursor.firstEnclosingOrThrow(J.ClassDeclaration.class);
                JavaType.Variable fieldType = new JavaType.Variable(null, Flag.Default.getBitMask(), name, owner.getType(), typeCast.getType(), emptyList());
                return new J.Identifier(
                        randomId(),
                        typeCast.getPrefix(),
                        Markers.EMPTY,
                        name,
                        typeCast.getType(),
                        fieldType);
            }
            return null;
        }
    }

    // FIXME remove this method when https://github.com/openrewrite/rewrite/issues/2713 is addressed and use `TypedTree#getType()`
    @Nullable
    private static JavaType toJavaType(TypedTree typeTree) {
        if (typeTree instanceof J.ArrayType) {
            JavaType.Array result = new JavaType.Array(null, ((J.ArrayType) typeTree).getElementType().getType());
            for (int i = 0; i < ((J.ArrayType) typeTree).getDimensions().size() - 1; i++) {
                result = new JavaType.Array(null, result);
            }
            return result;
        }
        return typeTree.getType();
    }

    private static String patternVariableName(J.InstanceOf instanceOf, Cursor cursor) {
        return VariableNameUtils.generateVariableName(shortVariableBaseName((TypeTree) instanceOf.getClazz()), cursor, INCREMENT_NUMBER);
    }

    private static String shortVariableBaseName(TypeTree typeTree) {
        return shortVariableBaseName(toJavaType(typeTree));
    }

    private static String shortVariableBaseName(@Nullable JavaType type) {
        // the instanceof operator only accepts classes (without generics) and arrays
        if (type instanceof JavaType.FullyQualified) {
            String qualifiedName = ((JavaType.FullyQualified) type).getFullyQualifiedName();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < qualifiedName.length(); i++) {
                char c = qualifiedName.charAt(i);
                if (Character.isUpperCase(c)) {
                    builder.append(Character.toLowerCase(c));
                }
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
        } else if (type instanceof JavaType.Primitive) {
            return ((JavaType.Primitive) type).getKeyword().substring(0, 1);
        } else if (type instanceof JavaType.Array) {
            JavaType elemType = ((JavaType.Array) type).getElemType();
            while (elemType instanceof JavaType.Array) {
                elemType = ((JavaType.Array) elemType).getElemType();
            }
            return shortVariableBaseName(elemType) + 's';
        }
        return "o";
    }

    private static class UseInstanceOfPatternMatching extends JavaVisitor<Integer> {

        private final InstanceOfPatternReplacements replacements;

        public UseInstanceOfPatternMatching(InstanceOfPatternReplacements replacements) {
            this.replacements = replacements;
        }

        @Nullable
        static J refactor(@Nullable J tree, InstanceOfPatternReplacements replacements, Cursor cursor) {
            return new UseInstanceOfPatternMatching(replacements).visit(tree, 0, cursor);
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, Integer executionContext) {
            instanceOf = (J.InstanceOf) super.visitInstanceOf(instanceOf, executionContext);
            instanceOf = replacements.processInstanceOf(instanceOf, getCursor());
            return instanceOf;
        }

        @Override
        public <T extends J> J visitParentheses(J.Parentheses<T> parens, Integer executionContext) {
            if (parens.getTree() instanceof J.TypeCast) {
                J replacement = replacements.processTypeCast((J.TypeCast) parens.getTree(), getCursor());
                if (replacement != null) {
                    return replacement;
                }
            }
            return super.visitParentheses(parens, executionContext);
        }

        @Override
        public J visitTypeCast(J.TypeCast typeCast, Integer executionContext) {
            typeCast = (J.TypeCast) super.visitTypeCast(typeCast, executionContext);
            J replacement = replacements.processTypeCast(typeCast, getCursor());
            if (replacement != null) {
                return replacement;
            }
            return typeCast;
        }
    }
}
