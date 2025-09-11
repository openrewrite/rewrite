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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.SearchResult;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindComments extends Recipe {

    @Option(displayName = "Text patterns",
            description = "A list of regular expressions to search for.",
            example = "-----BEGIN RSA PRIVATE KEY-----")
    List<String> patterns;

    @Override
    public String getDisplayName() {
        return "Find within comments and literals";
    }

    @Override
    public String getDescription() {
        return "Find regular expression matches within comments and literals. \"Literals\" includes string literals, " +
               "character literals, and numeric literals.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test(
                "patterns",
                "Patterns must be compilable regular expressions",
                patterns, ps -> {
                    for (String p : ps) {
                        try {
                            Pattern.compile(p);
                        } catch (PatternSyntaxException e) {
                            return false;
                        }
                    }
                    return true;
                })
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<Pattern> compiledPatterns = patterns.stream()
                .map(Pattern::compile)
                .collect(toList());

        return new JavaIsoVisitor<ExecutionContext>() {

            private final JavadocVisitor<ExecutionContext> javadocVisitor = new JavadocVisitor<ExecutionContext>(this) {
                @Override
                public Javadoc visitText(Javadoc.Text text, ExecutionContext ctx) {
                    return match(text, text.getText());
                }
            };

            @Override
            protected JavadocVisitor<ExecutionContext> getJavadocVisitor() {
                return javadocVisitor;
            }

            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                return space.withComments(ListUtils.map(space.getComments(), comment -> {
                    if (comment instanceof TextComment) {
                        for (Pattern p : compiledPatterns) {
                            if (p.matcher(((TextComment) comment).getText()).find()) {
                                return comment.withMarkers(comment.getMarkers().
                                        computeByType(new SearchResult(randomId(), null), (s1, s2) -> s1 == null ? s2 : s1));
                            }
                        }
                    } else if (comment instanceof Javadoc.DocComment) {
                        return (Comment) getJavadocVisitor().visitDocComment((Javadoc.DocComment) comment, ctx);
                    }
                    return comment;
                }));
            }

            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                if (literal.getType() == JavaType.Primitive.Null) {
                    return literal;
                }

                J.Literal matched = literal.getValue() != null ? match(literal, literal.getValue().toString()) : literal;
                if (matched != literal) {
                    return matched;
                }

                return match(literal, literal.getValueSource());
            }

            private <T extends Tree> T match(T t, @Nullable String value) {
                if (value == null) {
                    return t;
                }

                for (Pattern p : compiledPatterns) {
                    if (p.matcher(value).find()) {
                        return SearchResult.found(t);
                    }
                }

                return t;
            }
        };
    }
}
