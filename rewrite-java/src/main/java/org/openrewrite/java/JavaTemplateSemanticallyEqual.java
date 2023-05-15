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
package org.openrewrite.java;

import lombok.Value;
import lombok.With;
import org.antlr.v4.runtime.*;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.java.internal.grammar.TemplateParameterLexer;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

class JavaTemplateSemanticallyEqual extends SemanticallyEqual {

    @Value
    static class TemplateMatchResult {
        boolean match;
        List<J> matchedParameters;
    }

    static TemplateMatchResult matchesTemplate(JavaTemplate template, J input) {
        JavaCoordinates coordinates;
        if (input instanceof Expression) {
            coordinates = ((Expression) input).getCoordinates().replace();
        } else if (input instanceof Statement) {
            coordinates = ((Statement) input).getCoordinates().replace();
        } else {
            throw new IllegalArgumentException("Only expressions and statements can be matched against a template: " + input.getClass());
        }

        J[] parameters = createTemplateParameters(template.getCode());
        try {
            J templateTree = template.withTemplate(input, null, coordinates, parameters);
            return matchTemplate(templateTree, input);
        } catch (RuntimeException e) {
            // FIXME this is just a workaround, as template matching finds many new corner cases in `JavaTemplate` which we need to fix
            return new TemplateMatchResult(false, Collections.emptyList());
        }
    }

    private static J[] createTemplateParameters(String code) {
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(
                "#{", "}", null);

        List<J> parameters = new ArrayList<>();
        String substituted = code;
        while (true) {
            String previous = substituted;
            substituted = propertyPlaceholderHelper.replacePlaceholders(substituted, key -> {
                String s;
                if (!key.isEmpty()) {
                    TemplateParameterParser parser = new TemplateParameterParser(new CommonTokenStream(new TemplateParameterLexer(
                            CharStreams.fromString(key))));

                    parser.removeErrorListeners();
                    parser.addErrorListener(new BaseErrorListener() {
                        @Override
                        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                                int line, int charPositionInLine, String msg, RecognitionException e) {
                            throw new IllegalArgumentException(
                                    String.format("Syntax error at line %d:%d %s.", line, charPositionInLine, msg), e);
                        }
                    });

                    TemplateParameterParser.MatcherPatternContext ctx = parser.matcherPattern();
                    String matcherName = ctx.matcherName().Identifier().getText();
                    List<TemplateParameterParser.MatcherParameterContext> params = ctx.matcherParameter();

                    if ("any".equals(matcherName)) {
                        String fqn;

                        if (params.size() == 1) {
                            if (params.get(0).Identifier() != null) {
                                fqn = params.get(0).Identifier().getText();
                            } else {
                                fqn = params.get(0).FullyQualifiedName().getText();
                            }
                        } else {
                            fqn = "java.lang.Object";
                        }

                        s = fqn.replace("$", ".");

                        Markers markers = Markers.build(Collections.singleton(new TemplateParameter(randomId(), s)));
                        parameters.add(new J.Empty(randomId(), Space.EMPTY, markers));
                    } else {
                        throw new IllegalArgumentException("Invalid template matcher '" + key + "'");
                    }
                } else {
                    throw new IllegalArgumentException("Only typed placeholders are allowed.");
                }

                return s;
            });

            if (previous.equals(substituted)) {
                break;
            }
        }

        return parameters.toArray(new J[0]);
    }

    private static TemplateMatchResult matchTemplate(J templateTree, J tree) {
        JavaTemplateSemanticallyEqualVisitor semanticallyEqualVisitor = new JavaTemplateSemanticallyEqualVisitor();
        semanticallyEqualVisitor.visit(templateTree, tree);
        return new TemplateMatchResult(semanticallyEqualVisitor.isEqual(), semanticallyEqualVisitor.matchedParameters);
    }

    @Value
    @With
    private static class TemplateParameter implements Marker {
        UUID id;
        String typeName;
    }

    @SuppressWarnings("ConstantConditions")
    private static class JavaTemplateSemanticallyEqualVisitor extends SemanticallyEqualVisitor {

        final List<J> matchedParameters = new ArrayList<>();

        public JavaTemplateSemanticallyEqualVisitor() {
            super(true);
        }

        private boolean matchTemplateParameterPlaceholder(J.Empty empty, J j) {
            if (j instanceof TypedTree && !(j instanceof J.Primitive)) {
                TemplateParameter marker = (TemplateParameter) empty.getMarkers().getMarkers().get(0);
                if ("java.lang.Object".equals(marker.typeName)
                        || TypeUtils.isAssignableTo(marker.typeName, ((TypedTree) j).getType())) {
                    return registerMatch(j);
                }
            }
            return false;
        }

        private boolean registerMatch(J j) {
            return matchedParameters.add(j);
        }

        @Override
        public J.Empty visitEmpty(J.Empty empty, J j) {
            if (isEqual.get()) {
                if (isTemplateParameterPlaceholder(empty)) {
                    isEqual.set(matchTemplateParameterPlaceholder(empty, j));
                    return empty;
                }
                if (!(j instanceof J.Empty)) {
                    isEqual.set(false);
                    return empty;
                }

                J.Empty compareTo = (J.Empty) j;
                if (nullMissMatch(empty.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return empty;
                }
            }
            return empty;
        }

        private static boolean isTemplateParameterPlaceholder(J.Empty empty) {
            Markers markers = empty.getMarkers();
            return markers.getMarkers().size() == 1 && markers.getMarkers().get(0) instanceof TemplateParameter;
        }
    }
}
