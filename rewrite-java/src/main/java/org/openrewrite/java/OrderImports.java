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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.internal.StringUtils.hasLineBreak;

/**
 * This recipe will group and order the imports for a compilation unit using the rules defined by an {@link ImportLayoutStyle}.
 * If a style has not been defined, this recipe will use the default import layout style that is modeled after
 * IntelliJ's default import settings.
 * <p>
 * The @{link {@link OrderImports#removeUnused}} flag (which is defaulted to true) can be used to also remove any
 * imports that are not referenced within the compilation unit.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class OrderImports extends Recipe {

    @Option(displayName = "Remove unused",
            description = "Remove unnecessary imports.",
            required = false)
    @Nullable
    Boolean removeUnused;

    @Option(displayName = "Style YAML",
            description = "An OpenRewrite [style](https://docs.openrewrite.org/concepts-and-explanations/styles) formatted in YAML.",
            //language=yaml
            example = "type: specs.openrewrite.org/v1beta/style\n" +
                    "name: com.yourorg.CustomImportLayout\n" +
                    "styleConfigs:\n" +
                    "  - org.openrewrite.java.style.ImportLayoutStyle:\n" +
                    "      classCountToUseStarImport: 999\n" +
                    "      nameCountToUseStarImport: 999\n" +
                    "      layout:\n" +
                    "        - 'import java.*'\n" +
                    "        - 'import javax.*'\n" +
                    "        - '<blank line>'\n" +
                    "        - 'import all other imports'\n" +
                    "        - '<blank line>'\n" +
                    "        - 'import static all other imports'\n" +
                    "      packagesToFold:\n" +
                    "        - 'import java.awt.*'\n" +
                    "        - 'import static org.junit.jupiter.api.Assertions.*",
            required = false)
    @Nullable
    String style;

    String displayName = "Order imports";

    String description = "Groups and orders import statements. If a [style has been defined](https://docs.openrewrite.org/concepts-and-explanations/styles), this recipe will order the imports " +
                "according to that style. If no style is detected, this recipe will default to ordering imports in " +
                "the same way that IntelliJ IDEA does.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<NamedStyles> namedStyles = styleFromYaml(style);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                Optional<JavaSourceSet> sourceSet = cu.getMarkers().findFirst(JavaSourceSet.class);
                List<JavaType.FullyQualified> classpath = sourceSet.map(JavaSourceSet::getClasspath).orElse(emptyList());
                boolean classpathDirty = JavaSourceSet.isDirty(ctx, cu);

                ImportLayoutStyle importLayoutStyle = importLayoutStyle(cu, namedStyles);
                ImportComments comments = new ImportComments(cu);
                List<JRightPadded<J.Import>> orderedImports = importLayoutStyle.orderImports(comments.imports, classpath, classpathDirty);
                comments.recordFoldedImports(orderedImports);
                // Restore comments after removal, so dropping an unused import cannot discard its neighbor's comments.
                if (Boolean.TRUE.equals(removeUnused)) {
                    J.CompilationUnit prepared = cu.getPadding().withImports(orderedImports);
                    prepared = prepared.getClasses().isEmpty() ? prepared.withEof(comments.following) :
                            prepared.withClasses(ListUtils.mapFirst(prepared.getClasses(), cd -> cd.withPrefix(comments.following)));
                    J.CompilationUnit retained = (J.CompilationUnit) new RemoveUnusedImports().getVisitor()
                            .visitNonNull(withStyles(prepared, namedStyles), ctx, getCursor().getParentOrThrow());
                    orderedImports = retained.getPadding().getImports();
                    comments.recordUnfoldedImports(orderedImports);
                }
                J.CompilationUnit ordered = comments.restore(orderedImports);
                boolean changed = ordered != cu;
                cu = withStyles(ordered, namedStyles);

                if (changed && !cu.getClasses().isEmpty() && cu.getImports().isEmpty()) {
                    cu = autoFormat(cu, cu.getClasses().get(0).getName(), ctx, getCursor().getParentOrThrow());
                } else if (!Boolean.TRUE.equals(removeUnused) && changed && !cu.getClasses().isEmpty()) {
                    Cursor cuCursor = updateCursor(cu);
                    UUID lastImport = orderedImports.get(orderedImports.size() - 1).getElement().getId();
                    cu = cu.withClasses(ListUtils.mapFirst(cu.getClasses(), cd -> {
                        J.ClassDeclaration formatted = autoFormat(cd.withPrefix(comments.following), cd.getName(), ctx, cuCursor);
                        return formatted.withPrefix(comments.prependTrailing(lastImport, formatted.getPrefix()));
                    }));
                }

                return cu;
            }
        };
    }

    private static class ImportComments {
        private final J.CompilationUnit cu;
        private final Space header;
        private final Space following;
        private final Map<UUID, JRightPadded<J.Import>> originals = new HashMap<>();
        private final Map<UUID, Space> leading = new HashMap<>();
        private final Map<UUID, Space> trailing = new HashMap<>();
        private final List<JRightPadded<J.Import>> imports;

        ImportComments(J.CompilationUnit cu) {
            this.cu = cu;
            header = Space.firstPrefix(cu.getImports());
            List<JRightPadded<J.Import>> originalImports = cu.getPadding().getImports();
            for (int i = 0; i < originalImports.size(); i++) {
                JRightPadded<J.Import> padded = originalImports.get(i);
                J.Import anImport = padded.getElement();
                originals.put(anImport.getId(), padded);
                leading.put(anImport.getId(), i == 0 ? header.withComments(emptyList()) :
                        detachTrailing(anImport.getPrefix(), originalImports.get(i - 1).getElement().getId()));
            }
            Space afterImports = cu.getClasses().isEmpty() ? cu.getEof() : cu.getClasses().get(0).getPrefix();
            following = originalImports.isEmpty() ? afterImports :
                    detachTrailing(afterImports, originalImports.get(originalImports.size() - 1).getElement().getId());

            // End-of-line comments are stored on the following node. Carry them with their import
            // while sorting, then put them back on its new follower (including the first class/EOF).
            // https://github.com/openrewrite/rewrite/issues/6143
            imports = ListUtils.map(originalImports, padded -> {
                J.Import anImport = padded.getElement();
                Space prefix = leading.get(anImport.getId());
                Space suffix = trailing.get(anImport.getId());
                if (suffix != null) {
                    prefix = prefix.withComments(ListUtils.concatAll(prefix.getComments(), suffix.getComments()));
                }
                return prefix.equals(anImport.getPrefix()) ? padded : padded.withElement(anImport.withPrefix(prefix));
            });
        }

        private Space detachTrailing(Space prefix, UUID previousImport) {
            List<Comment> comments = prefix.getComments();
            String whitespace = prefix.getWhitespace();
            int count = 0;
            while (count < comments.size() && !hasLineBreak(whitespace)) {
                whitespace = comments.get(count++).getSuffix();
            }
            if (count == 0) {
                return prefix;
            }
            trailing.put(previousImport, Space.build(prefix.getWhitespace(), comments.subList(0, count)));
            return Space.build(whitespace, comments.subList(count, comments.size()));
        }

        void recordFoldedImports(List<JRightPadded<J.Import>> orderedImports) {
            Set<UUID> retained = new HashSet<>();
            for (JRightPadded<J.Import> padded : orderedImports) {
                retained.add(padded.getElement().getId());
            }
            for (JRightPadded<J.Import> padded : orderedImports) {
                J.Import wildcard = padded.getElement();
                if (!"*".equals(wildcard.getQualid().getSimpleName())) {
                    continue;
                }
                String target = wildcard.getQualid().getTarget().printTrimmed();
                for (JRightPadded<J.Import> original : imports) {
                    J.Import anImport = original.getElement();
                    if (retained.contains(anImport.getId()) || anImport.isStatic() != wildcard.isStatic() ||
                        !target.equals(anImport.getQualid().getTarget().printTrimmed())) {
                        continue;
                    }
                    Space prefix = leading.get(anImport.getId());
                    Space suffix = trailing.getOrDefault(anImport.getId(), Space.EMPTY);
                    List<Comment> comments = ListUtils.concatAll(prefix.getComments(), original.getAfter().getComments());
                    comments = ListUtils.concatAll(comments, suffix.getComments());
                    if (!comments.isEmpty()) {
                        // The individual import is gone; keep its comments above the wildcard, leaving
                        // the wildcard's own trailing comment in place. A final line comment needs a newline.
                        Space wildcardPrefix = leading.get(wildcard.getId());
                        String newline = prefix.getWhitespace().contains("\r\n") ||
                                wildcardPrefix.getWhitespace().contains("\r\n") ? "\r\n" : "\n";
                        comments = ListUtils.map(comments, c -> hasLineBreak(c.getSuffix()) ? c : c.withSuffix(newline));
                        leading.put(wildcard.getId(), wildcardPrefix.withComments(
                                ListUtils.concatAll(wildcardPrefix.getComments(), comments)));
                    }
                }
            }
        }

        void recordUnfoldedImports(List<JRightPadded<J.Import>> retainedImports) {
            Map<UUID, List<J.Import>> unfolded = new HashMap<>();
            for (JRightPadded<J.Import> padded : retainedImports) {
                J.Import anImport = padded.getElement();
                if (!originals.containsKey(anImport.getId())) {
                    // Unfolding changes the import's ID and name, but retains its qualifier.
                    unfolded.computeIfAbsent(anImport.getQualid().getTarget().getId(), id -> new ArrayList<>()).add(anImport);
                    leading.put(anImport.getId(), Space.EMPTY);
                }
            }
            for (JRightPadded<J.Import> original : originals.values()) {
                J.Import anImport = original.getElement();
                List<J.Import> replacements = unfolded.get(anImport.getQualid().getTarget().getId());
                if (replacements != null) {
                    leading.put(replacements.get(0).getId(), leading.get(anImport.getId()));
                    Space suffix = trailing.get(anImport.getId());
                    if (suffix != null) {
                        trailing.put(replacements.get(replacements.size() - 1).getId(), suffix);
                    }
                }
            }
        }

        J.CompilationUnit restore(List<JRightPadded<J.Import>> orderedImports) {
            if (orderedImports.isEmpty()) {
                if (cu.getImports().isEmpty()) {
                    return cu;
                }
                J.CompilationUnit c = cu.getPadding().withImports(orderedImports);
                Space prefix = header.getComments().isEmpty() ? following :
                        header.withComments(ListUtils.concatAll(header.getComments(), following.getComments()));
                return c.getClasses().isEmpty() ? c.withEof(prefix) :
                        c.withClasses(ListUtils.mapFirst(c.getClasses(), cd -> cd.withPrefix(prefix)));
            }
            List<JRightPadded<J.Import>> restored = ListUtils.map(orderedImports, (i, padded) -> {
                J.Import anImport = padded.getElement();
                Space prefix = anImport.getPrefix().withComments(leading.get(anImport.getId()).getComments());
                if (i == 0) {
                    prefix = header.withComments(ListUtils.concatAll(header.getComments(), prefix.getComments()));
                } else {
                    prefix = prependTrailing(orderedImports.get(i - 1).getElement().getId(), prefix);
                }
                JRightPadded<J.Import> original = originals.getOrDefault(anImport.getId(), padded);
                if (anImport.getQualid() == original.getElement().getQualid() &&
                    prefix.equals(original.getElement().getPrefix())) {
                    return original;
                }
                return padded.withElement(anImport.withPrefix(prefix));
            });
            // Reuse the original list when sorting and restoring comments made no changes.
            if (restored.size() == cu.getImports().size()) {
                List<JRightPadded<J.Import>> result = restored;
                restored = ListUtils.map(cu.getPadding().getImports(), (i, original) -> result.get(i));
            }
            J.CompilationUnit c = cu.getPadding().withImports(restored);
            Space afterImports = prependTrailing(orderedImports.get(orderedImports.size() - 1).getElement().getId(), following);
            if (c.getClasses().isEmpty()) {
                return afterImports.equals(c.getEof()) ? c : c.withEof(afterImports);
            }
            return afterImports.equals(c.getClasses().get(0).getPrefix()) ? c :
                    c.withClasses(ListUtils.mapFirst(c.getClasses(), cd -> cd.withPrefix(afterImports)));
        }

        private Space prependTrailing(UUID previousImport, Space prefix) {
            Space suffix = trailing.get(previousImport);
            if (suffix == null) {
                return prefix;
            }
            List<Comment> comments = ListUtils.mapLast(suffix.getComments(), comment -> comment.withSuffix(prefix.getWhitespace()));
            return suffix.withComments(ListUtils.concatAll(comments, prefix.getComments()));
        }
    }

    private List<NamedStyles> styleFromYaml(@Nullable String style) {
        if (style == null) {
            return emptyList();
        }
        return new ArrayList<>(new YamlResourceLoader(new ByteArrayInputStream(style.getBytes()),
                URI.create("OrderImports$style"),
                new Properties()).listStyles());
    }

    private ImportLayoutStyle importLayoutStyle(SourceFile cu, List<NamedStyles> parsedStyles) {
        if (parsedStyles.isEmpty()) {
            return Optional.ofNullable(Style.from(ImportLayoutStyle.class, cu))
                    .orElse(IntelliJ.importLayout());
        }
        return requireNonNull(NamedStyles.merge(ImportLayoutStyle.class, parsedStyles));
    }

    private <T extends SourceFile> T withStyles(T cu, List<NamedStyles> parsedStyles) {
        if (parsedStyles.isEmpty()) {
            return cu;
        }
        List<NamedStyles> existingStyles = cu.getMarkers().findAll(NamedStyles.class);
        // Check if all parsed styles already exist (ignoring id)
        boolean allPresent = parsedStyles.stream()
                .allMatch(ns -> existingStyles.stream().anyMatch(es -> namedStylesEqual(ns, es)));
        if (allPresent) {
            return cu;
        }
        // New styles must appear last to take precedence
        List<Marker> markers = cu.getMarkers().getMarkers();
        for (NamedStyles namedStyle : parsedStyles) {
            if (existingStyles.stream().noneMatch(es -> namedStylesEqual(namedStyle, es))) {
                markers = ListUtils.concat(markers, namedStyle);
            }
        }
        return cu.withMarkers(Markers.build(markers));
    }

    private boolean namedStylesEqual(NamedStyles a, NamedStyles b) {
        return a.getName().equals(b.getName());
    }
}
