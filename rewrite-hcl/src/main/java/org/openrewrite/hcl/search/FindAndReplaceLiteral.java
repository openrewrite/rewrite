/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.hcl.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.marker.AlreadyReplaced;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindAndReplaceLiteral extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find and replace literals in HCL files";
    }

    @Override
    public String getDescription() {
        return "Find and replace literal values in HCL files. This recipe parses the source files on which it runs as HCL, " +
               "meaning you can execute HCL language-specific recipes before and after this recipe in a single recipe run.";
    }

    @Option(displayName = "Find", description = "The literal to find (and replace)", example = "blacklist")
    String find;

    @Option(displayName = "Replace",
            description = "The replacement literal for `find`. This snippet can be multiline.",
            example = "denylist",
            required = false)
    @Nullable
    String replace;

    @Option(displayName = "Regex",
            description = "Default false. If true, `find` will be interpreted as a Regular Expression, and capture group contents will be available in `replace`.",
            required = false)
    @Nullable
    Boolean regex;

    @Option(displayName = "Case sensitive",
            description = "If `true` the search will be sensitive to case. Default `false`.", required = false)
    @Nullable
    Boolean caseSensitive;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new HclIsoVisitor<ExecutionContext>() {
            @Override
            public Hcl.Literal visitLiteral(final Hcl.Literal literal, final ExecutionContext ctx) {
                for (AlreadyReplaced alreadyReplaced : literal.getMarkers().findAll(AlreadyReplaced.class)) {
                    if (Objects.equals(find, alreadyReplaced.getFind()) &&
                        Objects.equals(replace, alreadyReplaced.getReplace())) {
                        return literal;
                    }
                }
                String searchStr = find;
                if (!Boolean.TRUE.equals(regex)) {
                    searchStr = Pattern.quote(searchStr);
                }
                int patternOptions = 0;
                if (!Boolean.TRUE.equals(caseSensitive)) {
                    patternOptions |= Pattern.CASE_INSENSITIVE;
                }
                Pattern pattern = Pattern.compile(searchStr, patternOptions);
                Matcher matcher = pattern.matcher(literal.getValue().toString());
                if (!matcher.find()) {
                    return literal;
                }
                String replacement = replace == null ? "" : replace;
                if (!Boolean.TRUE.equals(regex)) {
                    replacement = replacement.replace("$", "\\$");
                }
                String newLiteral = matcher.replaceAll(replacement);
                return literal.withValue(newLiteral).withValueSource(newLiteral)
                        .withMarkers(literal.getMarkers().add(new AlreadyReplaced(randomId(), find, replace)));
            }
        };
    }
}
