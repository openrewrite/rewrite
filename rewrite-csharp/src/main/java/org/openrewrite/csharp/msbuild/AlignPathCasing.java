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
package org.openrewrite.csharp.msbuild;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.csharp.table.PathCasingMismatches;
import org.openrewrite.text.PlainText;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.unmodifiableSet;

/**
 * Rewrites path references inside solution and MSBuild project files so that every path segment
 * is cased the way the file or directory is actually committed to the repository.
 */
public class AlignPathCasing extends ScanningRecipe<AlignPathCasing.PathIndex> {

    /**
     * Attributes whose value is one or more paths. Compared case-insensitively, because MSBuild
     * treats element and attribute names case-insensitively.
     */
    private static final Set<String> PATH_ATTRIBUTES = unmodifiableSet(new HashSet<>(Arrays.asList(
            "include", "exclude", "update", "remove", "project", "path")));

    /**
     * Item types whose {@code Include} is an identity — an assembly name, a package id, a
     * namespace — rather than a path. Aligning those against a same-named directory would
     * silently rename the reference.
     */
    private static final Set<String> IDENTITY_ITEMS = unmodifiableSet(new HashSet<>(Arrays.asList(
            "reference", "packagereference", "packageversion", "packagedownload", "frameworkreference",
            "projectcapability", "using", "internalsvisibleto", "assemblyattribute", "assemblymetadata",
            "trimmerrootassembly", "runtimehostconfigurationoption", "supportedplatform", "service",
            "bootstrapperpackage", "compilervisibleproperty")));

    /**
     * Elements whose text is a path.
     */
    private static final Set<String> PATH_ELEMENTS = unmodifiableSet(new HashSet<>(Arrays.asList(
            "hintpath", "projectpath", "applicationicon", "applicationmanifest", "assemblyoriginatorkeyfile",
            "codeanalysisruleset", "documentationfile", "nuspecfile", "runsettingsfilepath")));

    private static final Set<String> ITEM_PATH_ATTRIBUTES = unmodifiableSet(new HashSet<>(Arrays.asList(
            "include", "exclude", "update", "remove")));

    /**
     * {@code Project("{FAE04EC0-...}") = "Name", "relative\path\Project.csproj", "{GUID}"}
     */
    private static final Pattern SOLUTION_PROJECT = Pattern.compile(
            "(\\s*Project\\(\"\\{[^}]*}\"\\)\\s*=\\s*\"[^\"]*\",\\s*\")([^\"]*)(\".*)");

    /**
     * {@code path\to\file = path\to\file} inside a {@code ProjectSection(SolutionItems)} block.
     */
    private static final Pattern SOLUTION_ITEM = Pattern.compile("(\\s*)(\\S.*?)(\\s*=\\s*)(\\S.*?)(\\s*)");

    private final transient PathCasingMismatches mismatches = new PathCasingMismatches(this);

    @Override
    public String getDisplayName() {
        return "Align MSBuild path casing with the repository";
    }

    @Override
    public String getDescription() {
        return "MSBuild resolves paths case-insensitively on Windows, so a solution can reference " +
               "`assemblies/WPFToolkit/WPFToolkit.csproj` while the directory committed to git is actually " +
               "named `assemblies/Wpftoolkit`. The same reference fails with `MSB3202` or `Project file not " +
               "found` on a case-sensitive file system. This recipe rewrites path references in `.sln`, " +
               "`.slnx`, `.csproj`, `.props`, and `.targets` files so that every segment matches the casing of " +
               "the file or directory that is actually in the repository. References that already resolve " +
               "exactly, that cannot be resolved at all, or whose casing is ambiguous — because two files or " +
               "directories differ only by case — are left untouched.";
    }

    @Override
    public Set<String> getTags() {
        return new LinkedHashSet<>(Arrays.asList("csharp", "dotnet", "msbuild", "csproj", "sln"));
    }

    @Override
    public PathIndex getInitialValue(ExecutionContext ctx) {
        return new PathIndex();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(PathIndex acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    acc.add(((SourceFile) tree).getSourcePath());
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(PathIndex acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                String fileName = sourceFile.getSourcePath().getFileName().toString().toLowerCase(Locale.ROOT);
                if (tree instanceof Xml.Document && isMsBuildProjectFile(fileName)) {
                    return new AlignXmlPathCasing(acc, sourceFile.getSourcePath()).visit(tree, ctx);
                }
                if (tree instanceof PlainText && fileName.endsWith(".sln")) {
                    return alignSolution(acc, (PlainText) sourceFile, ctx);
                }
                return tree;
            }
        };
    }

    private static boolean isMsBuildProjectFile(String lowerCaseFileName) {
        return lowerCaseFileName.endsWith("proj") ||
               lowerCaseFileName.endsWith(".props") ||
               lowerCaseFileName.endsWith(".targets") ||
               lowerCaseFileName.endsWith(".projitems") ||
               lowerCaseFileName.endsWith(".slnx");
    }

    private class AlignXmlPathCasing extends XmlVisitor<ExecutionContext> {
        private final PathIndex index;
        private final Path sourcePath;

        AlignXmlPathCasing(PathIndex index, Path sourcePath) {
            this.index = index;
            this.sourcePath = sourcePath;
        }

        @Override
        public Xml visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
            Xml.Attribute a = (Xml.Attribute) super.visitAttribute(attribute, ctx);
            String name = a.getKeyAsString().toLowerCase(Locale.ROOT);
            if (!PATH_ATTRIBUTES.contains(name)) {
                return a;
            }
            Xml.Tag tag = getCursor().firstEnclosing(Xml.Tag.class);
            String tagName = tag == null ? "" : tag.getName();
            if (ITEM_PATH_ATTRIBUTES.contains(name) && IDENTITY_ITEMS.contains(tagName.toLowerCase(Locale.ROOT))) {
                return a;
            }
            String aligned = alignPathList(index, sourcePath, a.getValueAsString());
            if (aligned == null) {
                return a;
            }
            recordMismatch(ctx, sourcePath, tagName + "/@" + a.getKeyAsString(), a.getValueAsString(), aligned);
            return a.withValue(a.getValue().withValue(aligned));
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
            if (!PATH_ELEMENTS.contains(t.getName().toLowerCase(Locale.ROOT))) {
                return t;
            }
            Optional<String> value = t.getValue();
            if (!value.isPresent()) {
                return t;
            }
            String aligned = alignPathList(index, sourcePath, value.get());
            if (aligned == null) {
                return t;
            }
            recordMismatch(ctx, sourcePath, t.getName(), value.get(), aligned);
            return t.withValue(aligned);
        }
    }

    private PlainText alignSolution(PathIndex index, PlainText solution, ExecutionContext ctx) {
        String text = solution.getText();
        StringBuilder aligned = new StringBuilder(text.length());
        boolean changed = false;
        boolean inSolutionItems = false;
        int i = 0;
        while (i < text.length()) {
            int newLine = text.indexOf('\n', i);
            int end = newLine < 0 ? text.length() : newLine + 1;
            String line = text.substring(i, end);
            i = end;

            int bodyEnd = line.length();
            while (bodyEnd > 0 && (line.charAt(bodyEnd - 1) == '\n' || line.charAt(bodyEnd - 1) == '\r')) {
                bodyEnd--;
            }
            String body = line.substring(0, bodyEnd);
            String terminator = line.substring(bodyEnd);

            String trimmed = body.trim();
            if (trimmed.startsWith("ProjectSection(SolutionItems)")) {
                inSolutionItems = true;
            } else if (trimmed.startsWith("EndProjectSection")) {
                inSolutionItems = false;
            }

            String alignedBody = alignSolutionLine(index, solution.getSourcePath(), body, inSolutionItems, ctx);
            if (alignedBody == null) {
                aligned.append(line);
            } else {
                changed = true;
                aligned.append(alignedBody).append(terminator);
            }
        }
        return changed ? solution.withText(aligned.toString()) : solution;
    }

    private @Nullable String alignSolutionLine(PathIndex index, Path sourcePath, String line,
                                               boolean inSolutionItems, ExecutionContext ctx) {
        Matcher project = SOLUTION_PROJECT.matcher(line);
        if (project.matches()) {
            String aligned = alignPathList(index, sourcePath, project.group(2));
            if (aligned == null) {
                return null;
            }
            recordMismatch(ctx, sourcePath, "Project", project.group(2), aligned);
            return project.group(1) + aligned + project.group(3);
        }
        if (inSolutionItems) {
            Matcher item = SOLUTION_ITEM.matcher(line);
            if (item.matches()) {
                String alignedKey = alignPathList(index, sourcePath, item.group(2));
                String alignedValue = alignPathList(index, sourcePath, item.group(4));
                if (alignedKey == null && alignedValue == null) {
                    return null;
                }
                String key = alignedKey == null ? item.group(2) : alignedKey;
                String value = alignedValue == null ? item.group(4) : alignedValue;
                if (alignedKey != null) {
                    recordMismatch(ctx, sourcePath, "SolutionItems", item.group(2), key);
                }
                // A solution item is written `path = path`; only report it once.
                if (alignedValue != null && !item.group(4).equals(item.group(2))) {
                    recordMismatch(ctx, sourcePath, "SolutionItems", item.group(4), value);
                }
                return item.group(1) + key + item.group(3) + value + item.group(5);
            }
        }
        return null;
    }

    private void recordMismatch(ExecutionContext ctx, Path sourcePath, String location, String reference, String aligned) {
        mismatches.insertRow(ctx, new PathCasingMismatches.Row(
                sourcePath.toString(), location, reference, aligned));
    }

    /**
     * Aligns a semicolon-separated MSBuild path list, preserving the whitespace around each entry.
     *
     * @return the aligned list, or {@code null} when nothing in it needed aligning.
     */
    private static @Nullable String alignPathList(PathIndex index, Path sourcePath, String value) {
        if (value.isEmpty()) {
            return null;
        }
        Path parent = sourcePath.getParent();
        String baseDir = parent == null ? "" : parent.toString().replace('\\', '/');
        StringBuilder aligned = new StringBuilder(value.length());
        boolean changed = false;
        String[] entries = value.split(";", -1);
        for (int i = 0; i < entries.length; i++) {
            if (i > 0) {
                aligned.append(';');
            }
            String entry = entries[i];
            int start = 0;
            while (start < entry.length() && Character.isWhitespace(entry.charAt(start))) {
                start++;
            }
            int end = entry.length();
            while (end > start && Character.isWhitespace(entry.charAt(end - 1))) {
                end--;
            }
            String alignedEntry = alignPath(index, baseDir, entry.substring(start, end));
            if (alignedEntry == null) {
                aligned.append(entry);
            } else {
                changed = true;
                aligned.append(entry, 0, start).append(alignedEntry).append(entry, end, entry.length());
            }
        }
        return changed ? aligned.toString() : null;
    }

    /**
     * @return the path with each segment cased as it is in the repository, or {@code null} when the
     * path is already correct, is not a repository-relative path, or cannot be resolved unambiguously.
     */
    private static @Nullable String alignPath(PathIndex index, String baseDir, String path) {
        if (path.isEmpty() ||
            path.contains("$(") || path.contains("@(") || path.contains("%(") ||
            path.contains("://") ||
            path.charAt(0) == '/' || path.charAt(0) == '\\' ||
            (path.length() > 1 && path.charAt(1) == ':')) {
            return null;
        }
        boolean hasSeparator = indexOfSeparator(path, 0) >= 0;

        StringBuilder aligned = new StringBuilder(path.length());
        String current = baseDir;
        boolean changed = false;
        boolean literal = true;
        int i = 0;
        while (true) {
            int separator = indexOfSeparator(path, i);
            String segment = path.substring(i, separator < 0 ? path.length() : separator);
            if (!literal || segment.isEmpty() || ".".equals(segment)) {
                aligned.append(segment);
            } else if ("..".equals(segment)) {
                if (current.isEmpty()) {
                    // Escapes the repository root, so there is nothing to align against.
                    return null;
                }
                int slash = current.lastIndexOf('/');
                current = slash < 0 ? "" : current.substring(0, slash);
                aligned.append(segment);
            } else if (segment.indexOf('*') >= 0 || segment.indexOf('?') >= 0) {
                // A glob; the remainder is matched by MSBuild rather than named by it.
                literal = false;
                aligned.append(segment);
            } else {
                String candidate = current.isEmpty() ? segment : current + '/' + segment;
                String key = candidate.toLowerCase(Locale.ROOT);
                String actual = index.actualPath(key);
                if (actual == null) {
                    // Unknown to the repository, or ambiguous between two paths that differ only by
                    // case. Either way, picking a casing here would be a guess.
                    return null;
                }
                if (!hasSeparator && !index.isFile(key)) {
                    // A bare name like `<Reference Include="MsBuild.Utilities"/>` that happens to
                    // match a directory is an identity, not a path.
                    return null;
                }
                String actualSegment = actual.substring(actual.lastIndexOf('/') + 1);
                changed |= !actualSegment.equals(segment);
                aligned.append(actualSegment);
                current = actual;
            }
            if (separator < 0) {
                break;
            }
            aligned.append(path.charAt(separator));
            i = separator + 1;
        }
        return changed ? aligned.toString() : null;
    }

    private static int indexOfSeparator(String path, int from) {
        for (int i = from; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/' || c == '\\') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Every file in the repository, and every directory containing one, indexed by its lower-cased
     * path so a reference can be matched the way MSBuild would match it on Windows.
     */
    public static class PathIndex {
        private final Map<String, String> byLowerCase = new HashMap<>();
        private final Set<String> ambiguous = new HashSet<>();
        private final Set<String> files = new HashSet<>();

        void add(Path sourcePath) {
            String path = sourcePath.toString().replace('\\', '/');
            boolean file = true;
            int end = path.length();
            while (end > 0) {
                String actual = path.substring(0, end);
                String key = actual.toLowerCase(Locale.ROOT);
                String existing = byLowerCase.put(key, actual);
                if (existing != null && !existing.equals(actual)) {
                    ambiguous.add(key);
                    byLowerCase.put(key, existing);
                }
                if (file) {
                    files.add(key);
                    file = false;
                }
                end = actual.lastIndexOf('/');
            }
        }

        /**
         * @return the repository's own casing of the given lower-cased path, or {@code null} when the
         * path is unknown or two paths differ only by case.
         */
        @Nullable String actualPath(String lowerCaseKey) {
            return ambiguous.contains(lowerCaseKey) ? null : byLowerCase.get(lowerCaseKey);
        }

        boolean isFile(String lowerCaseKey) {
            return files.contains(lowerCaseKey);
        }
    }
}
