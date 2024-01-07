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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.FormatFirstClassPrefix;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.style.ImportLayoutStyle.isPackageAlwaysFolded;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RemoveImport<P> extends JavaIsoVisitor<P> {
    @EqualsAndHashCode.Include
    private final String type;

    private final String owner;

    @EqualsAndHashCode.Include
    private final boolean force;

    public RemoveImport(String type) {
        this(type, false);
    }

    @JsonCreator
    public RemoveImport(String type, boolean force) {
        this.type = type;
        this.owner = type.substring(0, Math.max(0, type.lastIndexOf('.')));
        this.force = force;
    }

    @Override
    public @Nullable J preVisit(J tree, P p) {
        stopAfterPreVisit();
        J j = tree;
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            ImportLayoutStyle importLayoutStyle = Optional.ofNullable(((SourceFile) cu).getStyle(ImportLayoutStyle.class))
                    .orElse(IntelliJ.importLayout());

            boolean typeUsed = false;
            Set<String> otherTypesInPackageUsed = new TreeSet<>();

            Set<String> methodsAndFieldsUsed = new HashSet<>();
            Set<String> otherMethodsAndFieldsInTypeUsed = new TreeSet<>();
            Set<String> originalImports = new HashSet<>();
            for (J.Import cuImport : cu.getImports()) {
                if (cuImport.getQualid().getType() != null) {
                    originalImports.add(((JavaType.FullyQualified) cuImport.getQualid().getType()).getFullyQualifiedName().replace("$", "."));
                }
            }

            for (JavaType.Variable variable : cu.getTypesInUse().getVariables()) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getOwner());
                if (fq != null && (TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), type)
                        || TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), owner))) {
                    methodsAndFieldsUsed.add(variable.getName());
                }
            }

            for (JavaType.Method method : cu.getTypesInUse().getUsedMethods()) {
                if (method.hasFlags(Flag.Static)) {
                    String declaringType = method.getDeclaringType().getFullyQualifiedName();
                    if (TypeUtils.fullyQualifiedNamesAreEqual(declaringType, type)) {
                        methodsAndFieldsUsed.add(method.getName());
                    } else if (declaringType.equals(owner)) {
                        if (method.getName().equals(type.substring(type.lastIndexOf('.') + 1))) {
                            methodsAndFieldsUsed.add(method.getName());
                        } else {
                            otherMethodsAndFieldsInTypeUsed.add(method.getName());
                        }
                    }
                }
            }

            for (JavaType javaType : cu.getTypesInUse().getTypesInUse()) {
                if (javaType instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) javaType;
                    if (TypeUtils.fullyQualifiedNamesAreEqual(fullyQualified.getFullyQualifiedName(), type)) {
                        typeUsed = true;
                    } else if (TypeUtils.fullyQualifiedNamesAreEqual(fullyQualified.getFullyQualifiedName(), owner)
                            || TypeUtils.fullyQualifiedNamesAreEqual(fullyQualified.getPackageName(), owner)) {
                        if (!originalImports.contains(fullyQualified.getFullyQualifiedName().replace("$", "."))) {
                            otherTypesInPackageUsed.add(fullyQualified.getClassName());
                        }
                    }
                }
            }

            JavaSourceFile c = cu;

            boolean keepImport = !force && (typeUsed || !otherTypesInPackageUsed.isEmpty() && type.endsWith(".*"));
            AtomicReference<Space> spaceForNextImport = new AtomicReference<>();
            c = c.withImports(ListUtils.flatMap(c.getImports(), import_ -> {
                if (spaceForNextImport.get() != null) {
                    Space removedPrefix = spaceForNextImport.get();
                    Space currentPrefix = import_.getPrefix();
                    if (removedPrefix.getLastWhitespace().isEmpty() ||
                        (countTrailingLinebreaks(removedPrefix) > countTrailingLinebreaks(currentPrefix))) {
                        import_ = import_.withPrefix(currentPrefix.withWhitespace(removedPrefix.getLastWhitespace()));
                    }
                    spaceForNextImport.set(null);
                }

                String typeName = import_.getTypeName();
                if (import_.isStatic()) {
                    String imported = import_.getQualid().getSimpleName();
                    if (TypeUtils.fullyQualifiedNamesAreEqual(typeName + "." + imported, type) && (force || !methodsAndFieldsUsed.contains(imported))) {
                        // e.g. remove java.util.Collections.emptySet when type is java.util.Collections.emptySet
                        spaceForNextImport.set(import_.getPrefix());
                        return null;
                    } else if ("*".equals(imported) && (TypeUtils.fullyQualifiedNamesAreEqual(typeName, type)
                            || TypeUtils.fullyQualifiedNamesAreEqual(typeName + type.substring(type.lastIndexOf('.')), type))) {
                        if (methodsAndFieldsUsed.isEmpty() && otherMethodsAndFieldsInTypeUsed.isEmpty()) {
                            spaceForNextImport.set(import_.getPrefix());
                            return null;
                        } else if (!isPackageAlwaysFolded(importLayoutStyle.getPackagesToFold(), import_) &&
                                methodsAndFieldsUsed.size() + otherMethodsAndFieldsInTypeUsed.size() < importLayoutStyle.getNameCountToUseStarImport()) {
                            methodsAndFieldsUsed.addAll(otherMethodsAndFieldsInTypeUsed);
                            return unfoldStarImport(import_, methodsAndFieldsUsed);
                        }
                    } else if (TypeUtils.fullyQualifiedNamesAreEqual(typeName, type) && !methodsAndFieldsUsed.contains(imported)) {
                        // e.g. remove java.util.Collections.emptySet when type is java.util.Collections
                        spaceForNextImport.set(import_.getPrefix());
                        return null;
                    }
                } else if (!keepImport && TypeUtils.fullyQualifiedNamesAreEqual(typeName, type)) {
                    spaceForNextImport.set(import_.getPrefix());
                    return null;
                } else if (!keepImport && import_.getPackageName().equals(owner) &&
                        "*".equals(import_.getClassName()) &&
                        !isPackageAlwaysFolded(importLayoutStyle.getPackagesToFold(), import_) &&
                        otherTypesInPackageUsed.size() < importLayoutStyle.getClassCountToUseStarImport()) {
                    if (otherTypesInPackageUsed.isEmpty()) {
                        spaceForNextImport.set(import_.getPrefix());
                        return null;
                    } else {
                        return unfoldStarImport(import_, otherTypesInPackageUsed);
                    }
                }
                return import_;
            }));

            if (c != cu && c.getPackageDeclaration() == null && c.getImports().isEmpty() &&
                    c.getPrefix() == Space.EMPTY) {
                doAfterVisit(new FormatFirstClassPrefix<>());
            }

            j = c;
        }

        return j;
    }

    private long countTrailingLinebreaks(Space space) {
        return space.getLastWhitespace().chars().filter(s -> s == '\n').count();
    }

    private Object unfoldStarImport(J.Import starImport, Set<String> otherImportsUsed) {
        List<J.Import> unfoldedImports = new ArrayList<>(otherImportsUsed.size());
        int i = 0;
        for (String other : otherImportsUsed) {
            J.Import unfolded = starImport.withQualid(starImport.getQualid().withName(starImport
                    .getQualid().getName().withSimpleName(other))).withId(randomId());
            unfoldedImports.add(i++ == 0 ? unfolded : unfolded.withPrefix(Space.format("\n")));
        }
        return unfoldedImports;
    }
}
