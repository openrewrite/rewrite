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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.Collections;
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
        private static String buildName(@Nullable JavaType type) {
            if (type != null) {
                if (type instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified asFQN = TypeUtils.asFullyQualified(type);
                    assert asFQN != null;
                    return asFQN.getClassName();
                } else if (type instanceof JavaType.Primitive) {
                    JavaType.Primitive asPrimitive = TypeUtils.asPrimitive(type);
                    assert asPrimitive != null;
                    return asPrimitive.getKeyword();
                } else if (type instanceof JavaType.Array) {
                    JavaType.Array arrayType = TypeUtils.asArray(type);
                    assert arrayType != null;
                    StringBuilder typeAsString = new StringBuilder();
                    JavaType elemType = arrayType.getElemType();
                    if (elemType instanceof JavaType.Primitive) {
                        typeAsString.append(buildName(elemType));
                    } else if (elemType instanceof JavaType.FullyQualified) {
                        typeAsString.append(buildName(elemType));
                    } else if (elemType instanceof JavaType.Array) {
                        JavaType typeOfArray = ((JavaType.Array) elemType).getElemType();
                        typeAsString.append(buildName(typeOfArray));
                        for (; arrayType.getElemType() instanceof JavaType.Array; arrayType = (JavaType.Array) arrayType.getElemType()) {
                            typeAsString.append("[]");
                        }
                    }
                    typeAsString.append("[]");
                    return typeAsString.toString();
                } else if(type instanceof JavaType.Variable) {
                    return buildName(((JavaType.Variable) type).getType());
                }
            }
            return null;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            // if the type expression is null, it implies the types on the lambda arguments are implicit.
            if (multiVariable.getTypeExpression() == null && getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.Lambda &&
                    !(multiVariable.getVariables().get(0).getType() instanceof JavaType.GenericTypeVariable)) {
                J.VariableDeclarations.NamedVariable nv = multiVariable.getVariables().get(0);
                String name = buildName(nv.getType());
                if (name != null) {
                    multiVariable = multiVariable.withTypeExpression(
                            new J.Identifier(Tree.randomId(),
                                    Space.EMPTY,
                                    Markers.EMPTY,
                                    name,
                                    nv.getType(),
                                    null
                            )
                    );
                    maybeAddImport(TypeUtils.asFullyQualified(nv.getType()));
                    getCursor().dropParentUntil(J.Lambda.class::isInstance).putMessage(ADDED_EXPLICIT_TYPE_KEY, true);
                }
            }
            return super.visitVariableDeclarations(multiVariable, ctx);
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
