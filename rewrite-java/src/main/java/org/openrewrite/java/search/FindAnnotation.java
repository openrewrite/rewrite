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
package org.openrewrite.java.search;

import lombok.Data;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.internal.grammar.AnnotationSignatureParser;
import org.openrewrite.java.internal.grammar.AspectJLexer;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Validated.required;

public class FindAnnotation extends Recipe {
    private String signature;

    public FindAnnotation() {
        this.processor = () -> new FindAnnotationProcessor(signature);
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public Validated validate() {
        return required("signature", signature);
    }

    public static Set<J.Annotation> find(J j, String clazz) {
        return SearchResult.find(new FindAnnotationProcessor(clazz).visit(j,
                ExecutionContext.builder().build()));
    }

    private static class FindAnnotationProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final AnnotationMatcher matcher;

        public FindAnnotationProcessor(String signature) {
            this.matcher = new AnnotationMatcher(signature);
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = super.visitAnnotation(annotation, ctx);
            if (matcher.matches(annotation)) {
                a = a.withMarkers(a.getMarkers().add(new SearchResult()));
            }
            return a;
        }

        private static class AnnotationMatcher {
            private final AnnotationSignatureParser.AnnotationContext match;

            public AnnotationMatcher(String signature) {
                this.match = new AnnotationSignatureParser(new CommonTokenStream(new AspectJLexer(CharStreams.fromString(signature))))
                        .annotation();
            }

            public boolean matches(J.Annotation annotation) {
                return matchesAnnotationName(annotation) &&
                        matchesSingleParameter(annotation) &&
                        matchesNamedParameters(annotation);
            }

            private boolean matchesAnnotationName(J.Annotation annotation) {
                JavaType.Class typeAsClass = TypeUtils.asClass(annotation.getType());
                return match.annotationName().getText().equals(typeAsClass == null ? null : typeAsClass.getFullyQualifiedName());
            }

            private boolean matchesNamedParameters(J.Annotation annotation) {
                AnnotationSignatureParser.ElementValuePairsContext pairs = match.elementValuePairs();
                if (pairs == null || pairs.elementValuePair() == null) {
                    return true;
                }

                List<AnnotationParameter> matchArgs = pairs.elementValuePair().stream()
                        .map(pair -> new AnnotationParameter(pair.Identifier().getText(), pair.elementValue().getText()))
                        .collect(toList());

                return annotation.getArgs() != null && annotation.getArgs().getElem().stream()
                        .map(arg -> {
                            J.Assign assign = (J.Assign) arg.getElem();
                            return new AnnotationParameter(assign.getVariable().printTrimmed(),
                                    assign.getAssignment().getElem().printTrimmed());
                        })
                        .allMatch(param -> matchArgs.stream().anyMatch(param::equals));
            }

            private boolean matchesSingleParameter(J.Annotation annotation) {
                if (match.elementValue() == null) {
                    return true;
                }

                return annotation.getArgs() == null || annotation.getArgs().getElem().stream()
                        .findAny()
                        .map(JRightPadded::getElem)
                        .map(arg -> {
                            if (arg instanceof J.Assign) {
                                return ((J.Assign) arg).getAssignment().getElem().printTrimmed().equals(match.elementValue().getText());
                            }
                            if (arg instanceof J.Literal) {
                                return ((J.Literal) arg).getValueSource().equals(match.elementValue().getText());
                            }
                            return false;
                        })
                        .orElse(true);
            }

            @Data
            private static class AnnotationParameter {
                private final String id;
                private final String value;
            }
        }
    }
}
