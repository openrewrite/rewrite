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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ExplicitLambdaArgumentTypes extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use explicit types on lambda arguments";
    }

    @Override
    public String getDescription() {
        return "Adds explicit types on lambda arguments, which are otherwise optional. This can make the code clearer and easier to read. " +
                "This does not add explicit types on arguments when the lambda has one or two parameters and does not have a block body, as things are considered more readable in those cases. " +
                "For example, `stream.map((a, b) -> a.length);` will not have explicit types added.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2211");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ExplicitLambdaArgumentTypesVisitor();
    }

    private static class ExplicitLambdaArgumentTypesVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final String ADDED_EXPLICIT_TYPE_KEY = "ADDED_EXPLICIT_TYPE";

        @Nullable
        private TypeTree buildTypeTree(@Nullable JavaType type, Space space) {
            if (type == null || type instanceof JavaType.Unknown) {
                return null;
            } else if (type instanceof JavaType.Primitive) {
                return new J.Primitive(
                        Tree.randomId(),
                        space,
                        Markers.EMPTY,
                        (JavaType.Primitive) type
                );
            } else if (type instanceof JavaType.Parameterized) {
                return new J.ParameterizedType(
                        Tree.randomId(),
                        space,
                        Markers.EMPTY,
                        buildTypeTree(((JavaType.Parameterized) type).getType(), Space.EMPTY),
                        buildTypeParameters(((JavaType.Parameterized) type).getTypeParameters())
                );
            } else if (type instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified fq = (JavaType.FullyQualified) type;
                maybeAddImport(fq);
                return new J.Identifier(Tree.randomId(),
                        space,
                        Markers.EMPTY,
                        fq.getClassName(),
                        type,
                        null
                );
            } else if (type instanceof JavaType.Array) {
                return (buildTypeTree(((JavaType.Array) type).getElemType(), space));
            } else if(type instanceof JavaType.Variable) {
                return buildTypeTree(((JavaType.Variable) type).getType(), space);
            } else if (type instanceof JavaType.GenericTypeVariable) {
                JavaType.GenericTypeVariable genericType = (JavaType.GenericTypeVariable) type;

                if (!genericType.getName().equals("?")) {
                    return new J.Identifier(Tree.randomId(),
                            space,
                            Markers.EMPTY,
                            genericType.getName(),
                            type,
                            null
                    );
                }
                JLeftPadded<J.Wildcard.Bound> bound = null;
                NameTree boundedType = null;
                if (genericType.getVariance() == JavaType.GenericTypeVariable.Variance.COVARIANT) {
                    bound = new JLeftPadded<>(Space.format(" "), J.Wildcard.Bound.Extends, Markers.EMPTY);
                } else if (genericType.getVariance() == JavaType.GenericTypeVariable.Variance.CONTRAVARIANT) {
                    bound = new JLeftPadded<>(Space.format(" "), J.Wildcard.Bound.Super, Markers.EMPTY);
                }

                if (!genericType.getBounds().isEmpty()) {
                    boundedType = buildTypeTree(genericType.getBounds().get(0), Space.format(" "));
                }

                return new J.Wildcard(
                        Tree.randomId(),
                        space,
                        Markers.EMPTY,
                        bound,
                        boundedType
                );
            }
            return null;
        }

        private JContainer<Expression> buildTypeParameters(List<JavaType> typeParameters) {
            List<JRightPadded<Expression>> typeExpressions = new ArrayList<>();

            for (JavaType type : typeParameters) {
                typeExpressions.add(new JRightPadded<>(
                        (Expression) buildTypeTree(type, Space.EMPTY),
                        Space.EMPTY,
                        Markers.EMPTY
                ));
            }
            return JContainer.build(Space.EMPTY, typeExpressions, Markers.EMPTY);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            // if the type expression is null, it implies the types on the lambda arguments are implicit.
            if (multiVariable.getTypeExpression() == null && getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.Lambda) {
                J.VariableDeclarations.NamedVariable nv = multiVariable.getVariables().get(0);
                TypeTree typeExpression = buildTypeTree(nv.getType(), Space.EMPTY);
                if (typeExpression != null) {
                    multiVariable = multiVariable.withTypeExpression(typeExpression);
                    int arrayDimensions = countDimensions(nv.getType());
                    if (arrayDimensions > 0) {
                        List<JLeftPadded<Space>> dimensions = new ArrayList<>();
                        for (int index = 0; index < arrayDimensions; index++) {
                            dimensions.add(new JLeftPadded<>(Space.EMPTY, Space.EMPTY, Markers.EMPTY));
                        }
                        multiVariable = multiVariable.withDimensionsBeforeName(dimensions);
                    }
                    getCursor().dropParentUntil(J.Lambda.class::isInstance).putMessage(ADDED_EXPLICIT_TYPE_KEY, true);
                }
            }
            return super.visitVariableDeclarations(multiVariable, ctx);
        }

        public int countDimensions(JavaType type) {
            if (!(type instanceof JavaType.Array)) {
                return 0;
            }

            int count = 0;
            while (type instanceof JavaType.Array) {
                type = ((JavaType.Array) type).getElemType();
                count++;
            }
            return count;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable nv = super.visitVariable(variable, ctx);
            Cursor c = getCursor().dropParentUntil(J.class::isInstance).dropParentUntil(J.class::isInstance);
            if (c.getValue() instanceof J.Lambda && c.getMessage(ADDED_EXPLICIT_TYPE_KEY) != null) {
                nv = nv.withPrefix(nv.getPrefix().withWhitespace(" "));
            }
            return nv;
        }

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
            if (lambda.getParameters().getParameters().size() <= 2 && !(lambda.getBody() instanceof J.Block)) {
                return lambda;
            }
            J.Lambda l = super.visitLambda(lambda, ctx);
            if (getCursor().getMessage(ADDED_EXPLICIT_TYPE_KEY) != null) {
                l = l.withParameters(l.getParameters().withParenthesized(true));
            }
            return l;
        }

    }

}
