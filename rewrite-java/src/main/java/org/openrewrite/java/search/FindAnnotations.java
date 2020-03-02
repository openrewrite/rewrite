/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
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
import org.openrewrite.Tree;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.internal.grammar.AnnotationSignatureParser;
import org.openrewrite.java.internal.grammar.AspectJLexer;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class FindAnnotations extends JavaSourceVisitor<List<J.Annotation>> {
    private final AnnotationMatcher matcher;

    public FindAnnotations(String signature) {
        this.matcher = new AnnotationMatcher(signature);
    }

    @Override
    public List<J.Annotation> defaultTo(Tree t) {
        return emptyList();
    }

    @Override
    public List<J.Annotation> visitAnnotation(J.Annotation annotation) {
        return matcher.matches(annotation) ? singletonList(annotation) : emptyList();
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
            if(pairs == null || pairs.elementValuePair() == null) {
                return true;
            }

            List<AnnotationParameter> matchArgs = pairs.elementValuePair().stream()
                    .map(pair -> new AnnotationParameter(pair.Identifier().getText(), pair.elementValue().getText()))
                    .collect(toList());

            return annotation.getArgs() != null && annotation.getArgs().getArgs().stream()
                    .map(arg -> {
                        J.Assign assign = (J.Assign) arg;
                        return new AnnotationParameter(assign.getVariable().printTrimmed(), assign.getAssignment().printTrimmed());
                    })
                    .allMatch(param -> matchArgs.stream().anyMatch(param::equals));
        }

        private boolean matchesSingleParameter(J.Annotation annotation) {
            if(match.elementValue() == null) {
                return true;
            }

            return annotation.getArgs() == null ? true : annotation.getArgs().getArgs().stream()
                    .findAny()
                    .map(arg -> {
                        if(arg instanceof J.Assign) {
                            return ((J.Assign) arg).getAssignment().printTrimmed().equals(match.elementValue().getText());
                        }
                        if(arg instanceof J.Literal) {
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
