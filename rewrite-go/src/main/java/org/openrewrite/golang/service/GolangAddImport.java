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
import org.openrewrite.golang.marker.GroupedImport;
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
 * Same-package types are automatically skipped.
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

        // Check if already imported by examining the qualid path string
        for (J.Import anImport : cu.getImports()) {
            String existingPath = getImportPath(anImport);
            if (importPath.equals(existingPath)) {
                return cu;
            }
        }

        if (onlyIfReferenced && !isReferenced(cu)) {
            return cu;
        }

        // Build the new J.Import with FieldAccess qualid (matching Go RPC receiver format)
        J.Import newImport = buildGoImport(importPath, alias);

        // Add to existing imports or create new import section
        JContainer<J.Import> container = cu.getImportsContainer();
        if (container != null && !container.getPadding().getElements().isEmpty()) {
            if (container.getPadding().getElements().size() == 1 &&
                !container.getMarkers().findFirst(GroupedImport.class).isPresent()) {
                container = promoteToGrouped(container);
            }
            List<JRightPadded<J.Import>> updated = new ArrayList<>(container.getPadding().getElements());
            // The last element's After holds the closing paren space in grouped imports.
            // Move it to the new last element.
            JRightPadded<J.Import> last = updated.get(updated.size() - 1);
            updated.set(updated.size() - 1, last.withAfter(Space.EMPTY));
            updated.add(new JRightPadded<>(newImport, last.getAfter(), Markers.EMPTY));
            return cu.withImportsContainer(container.getPadding().withElements(updated));
        } else {
            // No existing imports — create import section with grouped style
            List<JRightPadded<J.Import>> imports = new ArrayList<>();
            imports.add(new JRightPadded<>(newImport, Space.format("\n"), Markers.EMPTY));
            Markers containerMarkers = Markers.build(singletonList(
                    new GroupedImport(Tree.randomId(), Space.SINGLE_SPACE)));
            return cu.withImportsContainer(JContainer.build(
                    Space.format("\n\n"), imports, containerMarkers));
        }
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

    private static boolean isVersionElement(String element) {
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
     * {@code import "fmt"} can only hold one path, so move its lone import inside {@code import ( ... )}.
     */
    private static JContainer<J.Import> promoteToGrouped(JContainer<J.Import> imports) {
        JRightPadded<J.Import> only = imports.getPadding().getElements().get(0);
        J.Import imp = only.getElement().withPrefix(Space.format("\n\t"));
        if (imp.getAlias() == null) {
            imp = imp.withQualid(imp.getQualid().withPrefix(Space.EMPTY));
        }
        return imports.getPadding()
                .withElements(singletonList(only.withElement(imp).withAfter(Space.format("\n"))))
                .withMarkers(imports.getMarkers().add(new GroupedImport(Tree.randomId(), Space.SINGLE_SPACE)));
    }

    /**
     * Extracts the import path string from a Go import.
     * Go imports have qualid as FieldAccess(target=Empty, name=Identifier("path"))
     * where the path is the quoted import path without quotes.
     */
    private static @Nullable String getImportPath(J.Import anImport) {
        J.FieldAccess qualid = anImport.getQualid();
        return qualid.getName().getSimpleName();
    }

    /**
     * Builds a J.Import for a Go import path, using the FieldAccess representation
     * that matches the Go RPC receiver format.
     */
    private static J.Import buildGoImport(String importPath, @Nullable String aliasName) {
        // Qualid: FieldAccess(target=Empty, name=Identifier(importPath))
        J.FieldAccess qualid = new J.FieldAccess(
                Tree.randomId(),
                Space.EMPTY,
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
