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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.style.PreserveImportLayoutStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AddExplicitImportVisitor<P> extends AddImport<P> {
    final String imports;
    public AddExplicitImportVisitor(String imports) {
        this.imports = imports;
    }

    @Override
    public @Nullable J preVisit(J tree, P p) {
        stopAfterPreVisit();
        J j = tree;
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            final String[] split = imports.split("\n");
            final List<String> receivedImports = Arrays.stream(split)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            final List<J.Import> jImports = new ArrayList<>();
            final Space firstClassPrefix = cu.getClasses().get(0).getPrefix();
            final List<JRightPadded<J.Import>> existingImports = new ArrayList<>(cu.getPadding().getImports());
            for (String anImport : receivedImports) {
                final JLeftPadded<Boolean> statik;
                if (anImport.startsWith("static ")) {
                    statik = JLeftPadded.build(true).withBefore(Space.SINGLE_SPACE);
                    anImport = anImport.replace("static ", "");
                } else {
                    statik = new JLeftPadded<>(Space.EMPTY, false, Markers.EMPTY);
                }
                final int lastDotIdx = anImport.lastIndexOf('.');
                final String packageName = lastDotIdx != -1 ? anImport.substring(0, lastDotIdx) : null;
                if (packageName == null) {
                    continue;
                }

                J.Import aJimport = new J.Import(randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        statik,
                        TypeTree.build(anImport).withPrefix(Space.SINGLE_SPACE),
                        null);

                if ((jImports.isEmpty() || existingImports.isEmpty()) && !cu.getClasses().isEmpty() && cu.getPackageDeclaration() == null) {
                    aJimport = aJimport.withPrefix(firstClassPrefix
                            .withComments(ListUtils.map(firstClassPrefix.getComments(),
                                    comment -> comment instanceof Javadoc ? null : comment))
                            .withWhitespace(""));
                }
                jImports.add(aJimport);
            }

            PreserveImportLayoutStyle layoutStyle = new PreserveImportLayoutStyle(cu.getPadding().getImports());
            List<JRightPadded<J.Import>> newImports = layoutStyle.addImports(jImports);

            // ImportLayoutStyle::addImport adds always `\n` as newlines. Checking if we need to fix them
            newImports = checkCRLF(cu, newImports);

            cu = cu.getPadding().withImports(newImports);

            JavaSourceFile c = cu;
            cu = cu.withClasses(ListUtils.mapFirst(cu.getClasses(), clazz -> {
                J.ClassDeclaration cl = autoFormat(clazz, clazz.getName(), p, new Cursor(null, c));
                return clazz.withPrefix(clazz.getPrefix().withWhitespace(cl.getPrefix().getWhitespace()));
            }));

            j = cu;
        }
        return j;
    }
}
