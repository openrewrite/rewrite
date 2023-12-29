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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindLiterals extends Recipe {
    @Option(displayName = "Pattern",
            description = "A regular expression pattern to match literals against.",
            example = "file://")
    String pattern;

    @Override
    public String getDisplayName() {
        return "Find literals";
    }

    @Override
    public String getDescription() {
        return "Find literals matching a pattern.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(
                Validated.test("pattern", "Must be a valid regular expression", pattern,
                        p -> {
                            try {
                                Pattern.compile(p);
                                return true;
                            } catch (PatternSyntaxException e) {
                                return false;
                            }
                        })
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern compiledPattern = Pattern.compile(pattern);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                if (literal.getValueSource() != null) {
                    if (literal.getType() == JavaType.Primitive.String) {
                        if (!literal.getValueSource().isEmpty() && compiledPattern.matcher(literal.getValueSource().substring(1, literal.getValueSource().length() - 1)).matches()) {
                            return SearchResult.found(literal);
                        }
                    }
                    if (compiledPattern.matcher(literal.getValueSource()).matches()) {
                        return SearchResult.found(literal);
                    }
                }
                return literal;
            }
        };
    }
}
