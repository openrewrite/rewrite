/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.sln;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes Project entries with the given file extension (e.g., {@code .njsproj}) from
 * Visual Studio Solution (.sln/.slnx) files. Cleans up the corresponding
 * ProjectConfigurationPlatforms entries and (best-effort) NestedProjects entries.
 * <p>
 * Useful when the solution contains projects that {@code dotnet build} cannot handle —
 * notably Node.js Tools projects ({@code .njsproj}) which require Visual Studio
 * extensions that don't ship with the .NET SDK.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveNjsprojFromSolution extends Recipe {

    private static final Pattern PROJECT_BLOCK = Pattern.compile(
            "(?m)^Project\\(\"\\{[^}]+}\"\\)\\s*=\\s*\"[^\"]*\",\\s*\"([^\"]*)\",\\s*\"\\{([^}]+)}\"\\s*$"
                    + "([\\s\\S]*?)"
                    + "^EndProject\\s*$\\r?\\n?");

    @Override
    public String getDisplayName() {
        return "Remove projects from solution by extension";
    }

    @Override
    public String getDescription() {
        return "Removes Project entries with a matching file extension (e.g., `.njsproj`) " +
                "from Visual Studio Solution (.sln/.slnx) files, plus associated " +
                "ProjectConfigurationPlatforms and NestedProjects entries. " +
                "Default extension is `.njsproj` (Node.js Tools projects, which " +
                "`dotnet build` cannot process).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                String path = sourceFile.getSourcePath().toString().toLowerCase();
                return path.endsWith(".sln") || path.endsWith(".slnx");
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                if (!isAcceptable(sourceFile, ctx)) {
                    return tree;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);
                String text = plainText.getText();

                Set<String> removedGuids = new HashSet<>();
                Matcher m = PROJECT_BLOCK.matcher(text);
                StringBuilder out = new StringBuilder(text.length());
                int last = 0;
                boolean changed = false;
                while (m.find()) {
                    String relativePath = m.group(1);
                    String projectGuid = m.group(2);
                    out.append(text, last, m.start());
                    if (relativePath.toLowerCase().endsWith(".njsproj")) {
                        removedGuids.add(projectGuid.toUpperCase());
                        changed = true;
                    } else {
                        out.append(text, m.start(), m.end());
                    }
                    last = m.end();
                }
                out.append(text, last, text.length());

                if (!changed) {
                    return tree;
                }

                String afterProjects = out.toString();
                String afterCleanup = removeReferencesToGuids(afterProjects, removedGuids);
                return plainText.withText(afterCleanup);
            }
        };
    }

    /**
     * Removes any line that mentions one of the given (uppercased) project GUIDs
     * — covers ProjectConfigurationPlatforms and NestedProjects sections.
     */
    private static String removeReferencesToGuids(String text, Set<String> guids) {
        if (guids.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        for (String line : text.split("\\r?\\n", -1)) {
            String upper = line.toUpperCase();
            boolean keep = true;
            for (String g : guids) {
                if (upper.contains(g)) {
                    keep = false;
                    break;
                }
            }
            if (keep) {
                out.append(line).append('\n');
            }
        }
        // Preserve trailing-newline behavior: split with limit -1 produces a trailing empty
        // entry when the input ended in \n; strip the last \n we just appended for that case.
        if (text.endsWith("\n") && out.length() > 0) {
            out.setLength(out.length() - 1);
        } else if (!text.endsWith("\n") && out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }
}
