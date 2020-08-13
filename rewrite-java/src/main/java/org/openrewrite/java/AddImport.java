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
import org.openrewrite.java.search.HasType;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TreeBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Formatting.*;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AddImport extends JavaRefactorVisitor {
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

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu) {
        // note that using anyMatch here would return true for an empty list returned by FindType!
        @SuppressWarnings("SimplifyStreamApiCallChains") boolean hasReferences = new FindType(type).visit(cu).stream()
                .filter(t -> !(t instanceof J.FieldAccess) || !((J.FieldAccess) t).isFullyQualifiedClassReference(type))
                .findAny()
                .isPresent();

        if (onlyIfReferenced && !hasReferences) {
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

        // add just enough newlines to yield a blank line between imports and the first class declaration
        if(cu.getClasses().iterator().hasNext()) {
            while (!firstPrefix(cu.getClasses()).startsWith("\n\n")) {
                cu = cu.withClasses(formatFirstPrefix(cu.getClasses(), "\n" + firstPrefix(cu.getClasses())));
            }
        }

        imports.add(importToAdd);
        cu = cu.withImports(imports);

        OrderImports orderImports = cu.getStyle(ImportLayoutStyle.class)
                .map(ImportLayoutStyle::orderImports)
                .orElse(OrderImports.DEFAULT);
        orderImports.setRemoveUnused(false);
        andThen(orderImports);

        return cu;
    }
}
