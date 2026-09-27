/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.golang.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.golang.marker.GoProject;
import org.openrewrite.golang.marker.GroupedImport;
import org.openrewrite.golang.marker.ImportBlock;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Adds a Go import to a {@link Go.CompilationUnit} if it doesn't already exist.
 * <p>
 * Go imports are path-based strings (e.g., "fmt", "net/http"). The import path
 * corresponds to the Go module path, passed as the {@code importPath} parameter.
 * Same-package types are automatically skipped. Mirrors the Go-side AddImport:
 * the import lands at the end of its stdlib / third-party / local group.
 */
public class GolangAddImport<P> extends GolangVisitor<P> {

    private final String importPath;
    private final @Nullable String alias;
    private final boolean onlyIfReferenced;

    public GolangAddImport(String importPath, @Nullable String alias, boolean onlyIfReferenced) {
        this.importPath = importPath;
        this.alias = alias;
        this.onlyIfReferenced = onlyIfReferenced;
    }

    @Override
    public J visitGoCompilationUnit(Go.CompilationUnit cu, P p) {
        // Skip same-package imports
        String packageName = cu.getPackageDecl() != null
                ? cu.getPackageDecl().getSimpleName() : "";
        if (importPath.equals(packageName)) {
            return cu;
        }

        // "main" and "builtin" are not valid import paths
        if ("main".equals(importPath) || "builtin".equals(importPath)) {
            return cu;
        }

        if (hasImport(cu) || onlyIfReferenced && !isReferenced(cu)) {
            return cu;
        }

        // Build the new J.Import with FieldAccess qualid (matching Go RPC receiver format)
        J.Import newImport = buildGoImport(importPath, alias);

        JContainer<J.Import> container = cu.getImportsContainer();
        if (container != null && !container.getPadding().getElements().isEmpty()) {
            String modulePath = cu.getMarkers().findFirst(GoProject.class).map(GoProject::getModulePath).orElse(null);
            return cu.withImportsContainer(addToBlock(container, newImport, modulePath));
        }

        // No existing imports — create import section with grouped style
        List<JRightPadded<J.Import>> imports = new ArrayList<>();
        imports.add(new JRightPadded<>(newImport, Space.format("\n"), Markers.EMPTY));
        Markers containerMarkers = Markers.build(singletonList(
                new GroupedImport(Tree.randomId(), Space.SINGLE_SPACE)));
        return cu.withImportsContainer(JContainer.build(
                Space.format("\n\n"), imports, containerMarkers));
    }

    /**
     * A blank or dot import doesn't satisfy a request for a regular one; an aliased request needs that alias.
     */
    private boolean hasImport(Go.CompilationUnit cu) {
        for (J.Import anImport : cu.getImports()) {
            if (!importPath.equals(getImportPath(anImport))) {
                continue;
            }
            String existingAlias = anImport.getAlias() == null ? null : anImport.getAlias().getSimpleName();
            if (alias == null ? !"_".equals(existingAlias) && !".".equals(existingAlias) : alias.equals(existingAlias)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the parser's type attribution names {@code importPath} outside the import declarations,
     * mirroring the Go-side AddImport's {@code ReferencedPackages}.
     */
    private boolean isReferenced(Go.CompilationUnit cu) {
        AtomicBoolean referenced = new AtomicBoolean();
        new GolangVisitor<AtomicBoolean>() {
            @Override
            public J visitImport(J.Import anImport, AtomicBoolean found) {
                return anImport;
            }

            @Override
            public J visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                if (identifier.getType() instanceof JavaType.FullyQualified &&
                    importPath.equals(packagePathOf(((JavaType.FullyQualified) identifier.getType()).getFullyQualifiedName()))) {
                    found.set(true);
                }
                return identifier;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                JavaType.Method type = method.getMethodType();
                if (type != null && importPath.equals(packagePathOf(type.getDeclaringType().getFullyQualifiedName()))) {
                    found.set(true);
                }
                return super.visitMethodInvocation(method, found);
            }
        }.visit(cu, referenced);
        return referenced.get();
    }

    /**
     * The import path a Go type-attribution FQN belongs to: {@code "<path>"} for a package alias or
     * {@code "<path>.<Name>"} for a member, where a gopkg.in-style {@code .vN} is part of the path.
     */
    private static String packagePathOf(String fqn) {
        int lastSlash = fqn.lastIndexOf('/');
        if (lastSlash >= 0) {
            String[] elements = fqn.substring(lastSlash + 1).split("\\.", -1);
            int end = lastSlash + 1 + elements[0].length();
            for (int i = 1; i < elements.length && isVersionElement(elements[i]); i++) {
                end += 1 + elements[i].length();
            }
            return fqn.substring(0, end);
        }
        int dot = fqn.indexOf('.');
        return dot >= 0 ? fqn.substring(0, dot) : fqn;
    }

    static boolean isVersionElement(String element) {
        if (element.length() < 2 || element.charAt(0) != 'v') {
            return false;
        }
        for (int i = 1; i < element.length(); i++) {
            char c = element.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Inserts into the last import declaration holding the new import's group, else the last one. The first
     * declaration's shape lives on the container, each later one's on an {@link ImportBlock} marker on its
     * first import.
     */
    private JContainer<J.Import> addToBlock(JContainer<J.Import> imports, J.Import newImport, @Nullable String modulePath) {
        List<JRightPadded<J.Import>> elements = new ArrayList<>(imports.getPadding().getElements());
        ImportGroup group = ImportGroup.of(importPath, modulePath);

        List<Integer> starts = new ArrayList<>(singletonList(0));
        for (int i = 1; i < elements.size(); i++) {
            if (importBlock(elements.get(i)) != null) {
                starts.add(i);
            }
        }
        int block = starts.size() - 1;
        for (int b = starts.size() - 1; b >= 0; b--) {
            if (holdsGroup(elements, starts.get(b), blockEnd(starts, b, elements.size()), group, modulePath)) {
                block = b;
                break;
            }
        }
        int start = starts.get(block);
        int end = blockEnd(starts, block, elements.size());

        ImportBlock blockMarker = importBlock(elements.get(start));
        boolean grouped = blockMarker == null ?
                imports.getMarkers().findFirst(GroupedImport.class).isPresent() :
                blockMarker.isGrouped();
        if (!grouped) {
            imports = promoteToGrouped(imports, elements, start, end);
            blockMarker = importBlock(elements.get(start));
        }

        int insertAt = end;
        for (int i = start; i < end; i++) {
            if (groupOf(elements.get(i), modulePath).compareTo(group) > 0) {
                insertAt = i;
                break;
            }
        }
        Space indent = indent(elements.subList(start, end));
        boolean opensGroup = insertAt > start && groupOf(elements.get(insertAt - 1), modulePath) != group;
        J.Import imp = newImport.withPrefix(opensGroup ? groupSeparator(indent) : indent);
        if (insertAt == start) {
            // The new import opens the declaration, and the one it displaces now opens the next group.
            JRightPadded<J.Import> head = elements.get(start);
            J.Import displaced = head.getElement();
            if (blockMarker != null) {
                imp = imp.withMarkers(imp.getMarkers().add(blockMarker));
                displaced = displaced.withMarkers(displaced.getMarkers().removeByType(ImportBlock.class));
            }
            displaced = displaced.withPrefix(displaced.getPrefix().withWhitespace(groupSeparator(indent).getWhitespace()));
            elements.set(start, head.withElement(displaced));
        }

        JRightPadded<J.Import> added = new JRightPadded<>(imp, Space.EMPTY, Markers.EMPTY);
        if (insertAt == end) {
            // A grouped declaration's last import holds the space before its `)`.
            JRightPadded<J.Import> tail = elements.get(end - 1);
            added = added.withAfter(tail.getAfter());
            elements.set(end - 1, tail.withAfter(Space.EMPTY));
        }
        elements.add(insertAt, added);
        return imports.getPadding().withElements(elements);
    }

    /**
     * An ungrouped declaration holds exactly one import; wrap it in {@code ( ... )} so it can take another.
     */
    private static JContainer<J.Import> promoteToGrouped(JContainer<J.Import> imports, List<JRightPadded<J.Import>> elements,
                                                         int start, int end) {
        JRightPadded<J.Import> only = elements.get(start);
        J.Import imp = only.getElement();
        imp = imp.withPrefix(imp.getPrefix().withWhitespace("\n\t"));
        if (imp.getAlias() == null) {
            imp = imp.withQualid(imp.getQualid().withPrefix(Space.EMPTY));
        }
        ImportBlock blockMarker = importBlock(only);
        if (blockMarker == null) {
            imports = imports.withMarkers(imports.getMarkers().add(new GroupedImport(Tree.randomId(), Space.SINGLE_SPACE)));
        } else {
            imp = imp.withMarkers(imp.getMarkers().setByType(blockMarker.withGrouped(true).withGroupedBefore(Space.SINGLE_SPACE)));
        }
        elements.set(start, only.withElement(imp).withAfter(Space.format("\n")));

        if (end < elements.size()) {
            JRightPadded<J.Import> next = elements.get(end);
            ImportBlock nextBlock = importBlock(next);
            if (nextBlock != null) {
                J.Import nextImport = next.getElement();
                elements.set(end, next.withElement(nextImport.withMarkers(
                        nextImport.getMarkers().setByType(nextBlock.withClosePrevious(true)))));
            }
        }
        return imports;
    }

    private static int blockEnd(List<Integer> starts, int block, int size) {
        return block + 1 < starts.size() ? starts.get(block + 1) : size;
    }

    private static boolean holdsGroup(List<JRightPadded<J.Import>> elements, int start, int end,
                                      ImportGroup group, @Nullable String modulePath) {
        for (int i = start; i < end; i++) {
            if (groupOf(elements.get(i), modulePath) == group) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable ImportBlock importBlock(JRightPadded<J.Import> padded) {
        return padded.getElement().getMarkers().findFirst(ImportBlock.class).orElse(null);
    }

    private static ImportGroup groupOf(JRightPadded<J.Import> padded, @Nullable String modulePath) {
        return ImportGroup.of(getImportPath(padded.getElement()), modulePath);
    }

    /**
     * The per-line indent inside {@code import ( ... )}, read off the first import that carries one.
     */
    private static Space indent(List<JRightPadded<J.Import>> block) {
        for (JRightPadded<J.Import> padded : block) {
            String whitespace = padded.getElement().getPrefix().getWhitespace();
            while (whitespace.startsWith("\n\n")) {
                whitespace = whitespace.substring(1);
            }
            if (whitespace.startsWith("\n")) {
                return Space.format(whitespace);
            }
        }
        return Space.format("\n\t");
    }

    private static Space groupSeparator(Space indent) {
        return Space.format("\n" + indent.getWhitespace());
    }

    private enum ImportGroup {
        STDLIB, THIRD_PARTY, LOCAL;

        static ImportGroup of(String importPath, @Nullable String modulePath) {
            if (modulePath != null && !modulePath.isEmpty() &&
                (importPath.equals(modulePath) || importPath.startsWith(modulePath + "/"))) {
                return LOCAL;
            }
            int slash = importPath.indexOf('/');
            return (slash >= 0 ? importPath.substring(0, slash) : importPath).contains(".") ? THIRD_PARTY : STDLIB;
        }
    }

    /**
     * Extracts the import path string from a Go import.
     * Go imports have qualid as FieldAccess(target=Empty, name=Identifier("path"))
     * where the path is the quoted import path without quotes.
     */
    private static String getImportPath(J.Import anImport) {
        J.FieldAccess qualid = anImport.getQualid();
        return qualid.getName().getSimpleName();
    }

    /**
     * Builds a J.Import for a Go import path, using the FieldAccess representation
     * that matches the Go RPC receiver format.
     */
    private static J.Import buildGoImport(String importPath, @Nullable String aliasName) {
        // Qualid: FieldAccess(target=Empty, name=Identifier(importPath)); an alias is spaced from the path
        J.FieldAccess qualid = new J.FieldAccess(
                Tree.randomId(),
                aliasName == null ? Space.EMPTY : Space.SINGLE_SPACE,
                Markers.EMPTY,
                new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY),
                JLeftPadded.build(new J.Identifier(
                        Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                        emptyList(), importPath, null, null)),
                null
        );

        // Alias (optional)
        JLeftPadded<J.Identifier> aliasField = null;
        if (aliasName != null) {
            aliasField = new JLeftPadded<>(
                    Space.EMPTY,
                    new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                            emptyList(), aliasName, null, null),
                    Markers.EMPTY
            );
        }

        return new J.Import(
                Tree.randomId(),
                Space.format("\n\t"),     // indent with newline + tab
                Markers.EMPTY,
                new JLeftPadded<>(Space.EMPTY, false, Markers.EMPTY),  // not static
                qualid,
                aliasField
        );
    }
}
