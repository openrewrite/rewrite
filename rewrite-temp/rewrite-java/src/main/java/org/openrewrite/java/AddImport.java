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
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.FindType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TreeBuilder;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

/**
 * A Java refactoring visitor that can be used to add an import (or static import) to a given compilation unit.
 * This visitor can also be configured to only add the import if the imported class/method are referenced within the
 * compilation unit.
 * <P><P>
 * The {@link AddImport#type} must be supplied and represents a fully qualified class name.
 * <P><P>
 * The {@link AddImport#statik} is an optional method within the imported type. The staticMethod can be set to "*"
 * to represent a static wildcard import.
 * <P><P>
 * The {@link AddImport#onlyIfReferenced} is a flag (defaulted to true) to indicate if the import should only be added
 * if there is a reference to the imported class/method.
 */
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AddImport extends JavaIsoProcessor<ExecutionContext> {
    @EqualsAndHashCode.Include
    private final String type;

    @EqualsAndHashCode.Include
    @Nullable
    private final String statik;

    @EqualsAndHashCode.Include
    private final boolean onlyIfReferenced;

    private JavaType.Class classType;

    public AddImport(String type, @Nullable String statik, boolean onlyIfReferenced) {
        this.type = type;
        this.classType = JavaType.Class.build(type);
        this.statik = statik;
        this.onlyIfReferenced = onlyIfReferenced;
    }

    /**
     * Regex intended to be used on the whitespace, possibly containing comments, between import statements and class definition.
     * It will match() when the text begins with two `\n` characters, while allowing for other whitespace such as spaces
     * <p>
     * Fragment    Purpose
     * [ \t\r]*    match 0 or more whitespace characters, omitting \n
     * .*          match anything that's left, including multi-line comments, whitespace, etc., thanks to Pattern.DOTALL
     */
    private static final Pattern prefixedByTwoNewlines = Pattern.compile("[ \t\r]*\n[ \t\r]*\n[ \t\n]*.*", Pattern.DOTALL);

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
        if (JavaType.Primitive.fromKeyword(classType.getFullyQualifiedName()) != null) {
            return cu;
        }

        if (onlyIfReferenced && !hasReference(cu)) {
            return cu;
        }

        if (classType.getPackageName().isEmpty()) {
            return cu;
        }

        if (cu.getImports().stream().map(JRightPadded::getElem).anyMatch(i -> {
            String ending = i.getQualid().getSimpleName();
            if (statik == null) {
                return !i.isStatic() && i.getPackageName().equals(classType.getPackageName()) &&
                        (ending.equals(classType.getClassName()) ||
                                ending.equals("*"));
            }
            return i.isStatic() && i.getTypeName().equals(classType.getFullyQualifiedName()) &&
                    (ending.equals(statik) ||
                            ending.equals("*"));
        })) {
            return cu;
        }

        J.Import importToAdd = new J.Import(randomId(),
                TreeBuilder.buildName(classType.getFullyQualifiedName() +
                        (statik == null ? "" : "." + statik)).withFormatting(Formatting.format(" ")),
                statik != null,
                emptyList(),
                Formatting.EMPTY,
                Markers.EMPTY);

        List<J.Import> imports = new ArrayList<>(cu.getImports());

        if (imports.isEmpty()) {
            importToAdd = cu.getPackageDecl() == null ?
                    importToAdd.withPrefix(cu.getClasses().get(0).getPrefix() + "\n\n") :
                    importToAdd.withPrefix("\n\n");
        }

        // Add just enough newlines to yield a blank line between imports and the first class declaration
        if (cu.getClasses().iterator().hasNext()) {
            while (!prefixedByTwoNewlines.matcher(Formatting.firstPrefix(cu.getClasses())).matches()) {
                cu = cu.withClasses(Formatting.formatFirstPrefix(cu.getClasses(), "\n" + Formatting.firstPrefix(cu.getClasses())));
            }
        }

        imports.add(importToAdd);
        cu = cu.withImports(imports);

        OrderImports orderImports = new OrderImports();
        orderImports.setRemoveUnused(false);
        andThen(orderImports);

        return cu;
    }

    /**
     * Returns true if there is at least one matching references for this associated import.
     * An import is considered a match if:
     * It is non-static and has a field reference
     * It is static, the static method is a wildcard, and there is at least on method invocation on the given import type.
     * It is static, the static method is explicitly defined, and there is at least on method invocation matching the type and method.
     *
     * @param compilationUnit The compilation passed to the visitCompilationUnit
     * @return true if the import is referenced by the class either explicitly or through a method reference.
     */

    //Note that using anyMatch when a stream is empty ends up returning true, which is not the behavior needed here!
    @SuppressWarnings("SimplifyStreamApiCallChains")
    private boolean hasReference(J.CompilationUnit compilationUnit, ExecutionContext ctx) {

        if (statik == null) {
            //Non-static imports, we just look for field accesses.
            return FindType.find(compilationUnit, type).stream()
                    .filter(t -> !(t instanceof J.FieldAccess) || !((J.FieldAccess) t).isFullyQualifiedClassReference(type))
                    .findAny()
                    .isPresent();
        }

        //For static imports, we are either looking for a specific method or a wildcard.
        return compilationUnit.findMethodCalls(type + " *(..)").stream()
                .filter(
                        invocation -> invocation.getSelect() == null &&
                                (statik.equals("*") || invocation.getName().getSimpleName().equals(statik))
                )
                .findAny()
                .isPresent();
    }
}
