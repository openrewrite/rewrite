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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.EqualsAndHashCode;
import org.openrewrite.Formatting;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.FindType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TreeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.openrewrite.Formatting.*;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

/**
 * A Java refactoring visitor that can be used to add either an import or static import to a given compilation unit.
 *
 * The {@link AddImport#type} must be supplied and represents a fully qualified class name.
 * The {@link AddImport#staticMethod} is an optional method within the imported type. The staticMethod can be set to "*"
 * to represent a static wildcard import.
 */
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AddImport extends JavaIsoRefactorVisitor {
    @EqualsAndHashCode.Include
    private String type;

    @EqualsAndHashCode.Include
    @Nullable
    private String staticMethod;

    @EqualsAndHashCode.Include
    private boolean onlyIfReferenced = true;

    private JavaType.Class classType;

    public void setType(String type) {
        this.type = type;
        this.classType = JavaType.Class.build(type);
    }

    public void setStaticMethod(@Nullable String staticMethod) {
        this.staticMethod = staticMethod;
    }

    public void setOnlyIfReferenced(boolean onlyIfReferenced) {
        this.onlyIfReferenced = onlyIfReferenced;
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("class", type, "static.method", staticMethod == null ? "none" : staticMethod);
    }

    @Override
    public Validated validate() {
        return required("type", type);
    }

    /**
     * Regex intended to be used on the whitespace, possibly containing comments, between import statements and class definition.
     * It will match() when the text begins with two `\n` characters, while allowing for other whitespace such as spaces
     *
     *     Fragment    Purpose
     *     [ \t\r]*    match 0 or more whitespace characters, omitting \n
     *     .*          match anything that's left, including multi-line comments, whitespace, etc., thanks to Pattern.DOTALL
     */
    private static final Pattern prefixedByTwoNewlines = Pattern.compile("[ \t\r]*\n[ \t\r]*\n[ \t\n]*.*", Pattern.DOTALL);

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {
        if(JavaType.Primitive.fromKeyword(classType.getFullyQualifiedName()) != null) {
            return cu;
        }

        if (onlyIfReferenced && ! hasReference(cu)) {
            return cu;
        }

        if (classType.getPackageName().isEmpty()) {
            return cu;
        }

        if (cu.getImports().stream().anyMatch(i -> {
            String ending = i.getQualid().getSimpleName();
            if (staticMethod == null) {
                return !i.isStatic() && i.getPackageName().equals(classType.getPackageName()) &&
                        (ending.equals(classType.getClassName()) ||
                                ending.equals("*"));
            }
            return i.isStatic() && i.getTypeName().equals(classType.getFullyQualifiedName()) &&
                    (ending.equals(staticMethod) ||
                            ending.equals("*"));
        })) {
            return cu;
        }

        J.Import importToAdd = new J.Import(randomId(),
                TreeBuilder.buildName(classType.getFullyQualifiedName() +
                        (staticMethod == null ? "" : "." + staticMethod), Formatting.format(" ")),
                staticMethod != null,
                EMPTY);

        List<J.Import> imports = new ArrayList<>(cu.getImports());

        if (imports.isEmpty()) {
            importToAdd = cu.getPackageDecl() == null ?
                    importToAdd.withPrefix(cu.getClasses().get(0).getPrefix() + "\n\n") :
                    importToAdd.withPrefix("\n\n");
        }

        // Add just enough newlines to yield a blank line between imports and the first class declaration
        if(cu.getClasses().iterator().hasNext()) {
            while (!prefixedByTwoNewlines.matcher(firstPrefix(cu.getClasses())).matches()) {
                cu = cu.withClasses(formatFirstPrefix(cu.getClasses(), "\n" + firstPrefix(cu.getClasses())));
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
     *  It is non-static and has a field reference
     *  It is static, the static method is a wildcard, and there is at least on method invocation on the given import type.
     *  It is static, the static method is explicitly defined, and there is at least on method invocation matching the type and method.
     *
     * @param compilationUnit The compilation passed to the visitCompilationUnit
     * @return true if the import is referenced by the class either explicitly or through a method reference.
     */

    //Note that using anyMatch when a stream is empty ends up returning true, which is not the behavior needed here!
    @SuppressWarnings("SimplifyStreamApiCallChains")
    private boolean hasReference(J.CompilationUnit compilationUnit) {

        if (staticMethod == null) {
            //Non-static imports, we just look for field accesses.
            return new FindType(type).visit(compilationUnit).stream()
                    .filter(t -> !(t instanceof J.FieldAccess) || !((J.FieldAccess) t).isFullyQualifiedClassReference(type))
                    .findAny()
                    .isPresent();
        }

        //For static imports, we are either looking for a specific method or a wildcard.
        return compilationUnit.findMethodCalls(type + " *(..)").stream()
                .filter(invocation -> staticMethod.equals("*") || invocation.getName().getSimpleName().equals(staticMethod))
                .findAny()
                .isPresent();
    }
}
