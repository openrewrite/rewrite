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
package org.openrewrite.java.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.openrewrite.java.format.AutodetectGeneralFormatStyle.autodetectGeneralFormatStyle;

public class EmptyNewlineAtEndOfFile extends Recipe {
    @Override
    public String getDisplayName() {
        return "End files with a single newline";
    }

    @Override
    public String getDescription() {
        return "Some tools work better when files end with an empty line.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("RSPEC-S113");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    GeneralFormatStyle generalFormatStyle = Style.from(GeneralFormatStyle.class, cu, () -> autodetectGeneralFormatStyle(cu));
                    String lineEnding = generalFormatStyle.newLine();

                    Space eof = cu.getEof();
                    if (StringUtils.isBlank(eof.getLastWhitespace()) &&
                        eof.getLastWhitespace().chars().filter(c -> c == '\n').count() != 1) {
                        List<Comment> comments = eof.getComments();
                        if (comments.isEmpty()) {
                            return cu.withEof(Space.format(lineEnding));
                        } else {
                            return cu.withEof(cu.getEof().withComments(ListUtils.mapLast(comments, it -> it.withSuffix(lineEnding))));
                        }
                    }
                    return cu;
                }
                return (J) tree;
            }
        };
    }
}
