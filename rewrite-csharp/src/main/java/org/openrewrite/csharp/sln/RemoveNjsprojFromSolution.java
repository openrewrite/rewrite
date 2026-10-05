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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.text.PlainText;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Drops Node.js Tools projects, and everything in the solution that refers to them, from
 * {@code .sln} and {@code .slnx} files.
 */
public class RemoveNjsprojFromSolution extends Recipe {

    private static final String NJSPROJ = ".njsproj";

    private static final Pattern SOLUTION_PROJECT = Pattern.compile(
            "\\s*Project\\(\"\\{[^}]*}\"\\)\\s*=\\s*\"[^\"]*\"\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\"(\\{[^}]*})\"\\s*");

    @Override
    public String getDisplayName() {
        return "Remove `.njsproj` projects from solution";
    }

    @Override
    public String getDescription() {
        return "Removes Project entries with the `.njsproj` extension (Node.js Tools projects) from Visual " +
               "Studio solution files, together with the configuration, nesting, and dependency entries that " +
               "refer to them. The .NET SDK does not ship `Microsoft.NodejsTools.targets`, so `dotnet build` " +
               "fails on a solution that still lists one of these projects.";
    }

    @Override
    public Set<String> getTags() {
        return new LinkedHashSet<>(Arrays.asList("csharp", "dotnet", "sln"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                String fileName = ((SourceFile) tree).getSourcePath().getFileName().toString()
                        .toLowerCase(Locale.ROOT);
                if (tree instanceof PlainText && fileName.endsWith(".sln")) {
                    return removeFromSolution((PlainText) tree);
                }
                if (tree instanceof Xml.Document && fileName.endsWith(".slnx")) {
                    return new RemoveNjsprojProject().visit(tree, ctx);
                }
                return tree;
            }
        };
    }

    private static PlainText removeFromSolution(PlainText solution) {
        List<String> lines = lines(solution.getText());

        Set<String> njsprojGuids = new LinkedHashSet<>();
        for (String line : lines) {
            Matcher project = SOLUTION_PROJECT.matcher(body(line));
            if (project.matches() && project.group(1).toLowerCase(Locale.ROOT).endsWith(NJSPROJ)) {
                njsprojGuids.add(project.group(2).toUpperCase(Locale.ROOT));
            }
        }
        if (njsprojGuids.isEmpty()) {
            return solution;
        }

        StringBuilder kept = new StringBuilder(solution.getText().length());
        boolean inRemovedProject = false;
        for (String line : lines) {
            String body = body(line);
            String keyword = body.trim();
            if (inRemovedProject) {
                if (endsProjectBlock(keyword)) {
                    inRemovedProject = false;
                    continue;
                }
                if (!startsProjectBlock(keyword) && !startsGlobalSections(keyword)) {
                    continue;
                }
                inRemovedProject = false;
            }
            Matcher project = SOLUTION_PROJECT.matcher(body);
            if (project.matches() && njsprojGuids.contains(project.group(2).toUpperCase(Locale.ROOT))) {
                inRemovedProject = true;
                continue;
            }
            if (!referencesAnyGuid(body, njsprojGuids)) {
                kept.append(line);
            }
        }
        return solution.withText(kept.toString());
    }

    private static boolean startsProjectBlock(String keyword) {
        return keyword.startsWith("Project(");
    }

    private static boolean endsProjectBlock(String keyword) {
        return "EndProject".equals(keyword);
    }

    private static boolean startsGlobalSections(String keyword) {
        return "Global".equals(keyword);
    }

    private static boolean referencesAnyGuid(String line, Set<String> guids) {
        String upperCase = line.toUpperCase(Locale.ROOT);
        for (String guid : guids) {
            if (upperCase.contains(guid)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> lines(String text) {
        List<String> lines = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int newLine = text.indexOf('\n', i);
            int end = newLine < 0 ? text.length() : newLine + 1;
            lines.add(text.substring(i, end));
            i = end;
        }
        return lines;
    }

    private static String body(String line) {
        int end = line.length();
        while (end > 0 && (line.charAt(end - 1) == '\n' || line.charAt(end - 1) == '\r')) {
            end--;
        }
        return line.substring(0, end);
    }

    private static class RemoveNjsprojProject extends XmlVisitor<ExecutionContext> {
        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
            if (t.getContent() == null) {
                return t;
            }
            return t.withContent(ListUtils.map(t.getContent(), c -> isNjsprojProject(c) ? null : c));
        }

        private static boolean isNjsprojProject(Content content) {
            if (!(content instanceof Xml.Tag) || !"Project".equalsIgnoreCase(((Xml.Tag) content).getName())) {
                return false;
            }
            for (Xml.Attribute attribute : ((Xml.Tag) content).getAttributes()) {
                if ("Path".equalsIgnoreCase(attribute.getKeyAsString()) &&
                    attribute.getValueAsString().toLowerCase(Locale.ROOT).endsWith(NJSPROJ)) {
                    return true;
                }
            }
            return false;
        }
    }
}
