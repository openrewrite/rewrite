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
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.PreserveImportLayoutStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AddExplicitImportVisitor<P> extends AddImport<P> {

    private final boolean isStatic;
    public AddExplicitImportVisitor(String fullyQualifiedName, boolean isStatic) {
        super(fullyQualifiedName, null, false);
        this.isStatic = isStatic;
    }

    @Override
    public @Nullable J preVisit(J tree, P p) {
        stopAfterPreVisit();
        J j = tree;
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            if (packageName == null || JavaType.Primitive.fromKeyword(fullyQualifiedName) != null) {
                return cu;
            }
            if ("java.lang".equals(packageName)) {
                return cu;
            }
            // Nor if the classes are within the same package
            if (!"Record".equals(typeName) && cu.getPackageDeclaration() != null &&
                    packageName.equals(cu.getPackageDeclaration().getExpression().printTrimmed(getCursor()))) {
                return cu;
            }

            if (!"Record".equals(typeName) && cu.getImports().stream().anyMatch(i -> {
                String ending = i.getQualid().getSimpleName();
                return !i.isStatic() && i.getPackageName().equals(packageName) &&
                        (ending.equals(typeName) || "*".equals(ending));
            })) {
                return cu;
            }

            JLeftPadded<Boolean> statik = new JLeftPadded<>(Space.EMPTY, false, Markers.EMPTY);
            Space prefix = Space.EMPTY;

            if (isStatic) {
                statik = JLeftPadded.build(true).withBefore(Space.SINGLE_SPACE);
            }
            J.Import importToAdd = new J.Import(randomId(),
                    prefix,
                    Markers.EMPTY,
                    statik,
                    TypeTree.build(fullyQualifiedName).withPrefix(Space.SINGLE_SPACE),
                    null);

            List<JRightPadded<J.Import>> imports = new ArrayList<>(cu.getPadding().getImports());

            if (imports.isEmpty() && !cu.getClasses().isEmpty() && cu.getPackageDeclaration() == null) {
                // leave javadocs on the class and move other comments up to the import
                // (which could include license headers and the like)
                Space firstClassPrefix = cu.getClasses().get(0).getPrefix();
                importToAdd = importToAdd.withPrefix(firstClassPrefix
                        .withComments(ListUtils.map(firstClassPrefix.getComments(),
                                comment -> comment instanceof Javadoc ? null : comment))
                        .withWhitespace(""));

                cu = cu.withClasses(ListUtils.mapFirst(cu.getClasses(), clazz ->
                        clazz.withComments(ListUtils.map(clazz.getComments(), comment -> comment instanceof Javadoc ?
                                comment : null))
                ));
            }

            ImportLayoutStyle layoutStyle = new PreserveImportLayoutStyle();
            List<JavaType.FullyQualified> classpath = cu.getMarkers().findFirst(JavaSourceSet.class)
                    .map(JavaSourceSet::getClasspath)
                    .orElse(Collections.emptyList());

            List<JRightPadded<J.Import>> newImports = layoutStyle.addImport(cu.getPadding().getImports(), importToAdd
                    , cu.getPackageDeclaration(), classpath);

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
