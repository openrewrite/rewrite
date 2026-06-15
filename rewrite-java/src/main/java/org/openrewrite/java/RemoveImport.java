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
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.internal.FormatFirstClassPrefix;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.*;
import org.openrewrite.style.Style;

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
            ImportLayoutStyle importLayoutStyle = Style.from(ImportLayoutStyle.class, cu, IntelliJ::importLayout);

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
                if (fq != null && (TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), type) ||
                        TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), owner))) {
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
                    } else if (TypeUtils.fullyQualifiedNamesAreEqual(fullyQualified.getFullyQualifiedName(), owner) ||
                            TypeUtils.fullyQualifiedNamesAreEqual(fullyQualified.getPackageName(), owner)) {
                        if (!originalImports.contains(fullyQualified.getFullyQualifiedName().replace("$", "."))) {
                            otherTypesInPackageUsed.add(fullyQualified.getClassName());
                        }
                    }
                }
            }

            JavaSourceFile c = cu;

            boolean keepImport = !force && (typeUsed || !otherTypesInPackageUsed.isEmpty() && type.endsWith(".*"));
            AtomicReference<@Nullable Space> spaceForNextImport = new AtomicReference<>();
            c = c.withImports(ListUtils.flatMap(c.getImports(), import_ -> {
                Space removedPrefix = spaceForNextImport.get();
                if (removedPrefix != null) {
                    // An end-of-line comment on the removed import's line is stored at the start of
                    // this (the next) import's prefix. It explained the now-removed import, so drop it.
                    Space currentPrefix = dropTrailingCommentOfRemovedImport(import_.getPrefix());
                    if (!removedPrefix.getComments().isEmpty()) {
                        // The removed import's prefix carried the trailing comment(s) of the preceding
                        // line (e.g. a `// ktlint-disable` suppression) and/or standalone (commented-out)
                        // lines. Move them onto the next import so they are not lost, preserving any
                        // blank line that separated the imports.
                        List<Comment> comments = ListUtils.concatAll(removedPrefix.getComments(), currentPrefix.getComments());
                        if (currentPrefix.getComments().isEmpty() &&
                            countTrailingLinebreaks(currentPrefix) > countTrailingLinebreaks(removedPrefix)) {
                            String currentLastWhitespace = currentPrefix.getLastWhitespace();
                            comments = ListUtils.mapLast(comments, comment -> comment.withSuffix(currentLastWhitespace));
                        }
                        currentPrefix = removedPrefix.withComments(comments);
                    } else if (removedPrefix.getLastWhitespace().isEmpty() ||
                        (countTrailingLinebreaks(removedPrefix) > countTrailingLinebreaks(currentPrefix))) {
                        currentPrefix = currentPrefix.withWhitespace(removedPrefix.getLastWhitespace());
                    }
                    import_ = import_.withPrefix(currentPrefix);
                    spaceForNextImport.set(null);
                }

                String typeName = import_.getTypeName();
                if (import_.isStatic()) {
                    String imported = import_.getQualid().getSimpleName();
                    if (TypeUtils.fullyQualifiedNamesAreEqual(typeName + "." + imported, type) && (force || !methodsAndFieldsUsed.contains(imported))) {
                        // e.g. remove java.util.Collections.emptySet when type is java.util.Collections.emptySet
                        spaceForNextImport.set(import_.getPrefix());
                        return null;
                    } else if ("*".equals(imported) && (TypeUtils.fullyQualifiedNamesAreEqual(typeName, type) ||
                            !owner.isEmpty() && TypeUtils.fullyQualifiedNamesAreEqual(typeName, owner))) {
                        if (methodsAndFieldsUsed.isEmpty() && otherMethodsAndFieldsInTypeUsed.isEmpty()) {
                            spaceForNextImport.set(import_.getPrefix());
                            return null;
                        } else if (!isPackageAlwaysFolded(importLayoutStyle.getPackagesToFold(), import_) &&
                                methodsAndFieldsUsed.size() + otherMethodsAndFieldsInTypeUsed.size() < importLayoutStyle.getNameCountToUseStarImport()) {
                            methodsAndFieldsUsed.addAll(otherMethodsAndFieldsInTypeUsed);
                            return unfoldStarImport(import_, methodsAndFieldsUsed, cu);
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
                        return unfoldStarImport(import_, otherTypesInPackageUsed, cu);
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

    /**
     * The end-of-line comment of a removed import is stored as the first comment of the following
     * import's prefix, on the same line as the removed import (i.e. no newline precedes it). It
     * described the removed import, so drop it. Standalone comments (preceded by a newline, such as
     * commented-out import lines) and the trailing comments of earlier lines are left untouched.
     */
    private static Space dropTrailingCommentOfRemovedImport(Space prefix) {
        List<Comment> comments = prefix.getComments();
        if (comments.isEmpty() || prefix.getWhitespace().contains("\n")) {
            return prefix;
        }
        return Space.build(comments.get(0).getSuffix(), comments.subList(1, comments.size()));
    }

    private Object unfoldStarImport(J.Import starImport, Set<String> otherImportsUsed, JavaSourceFile cu) {
        List<J.Import> unfoldedImports = new ArrayList<>(otherImportsUsed.size());
        int i = 0;
        for (String other : otherImportsUsed) {
            J.FieldAccess newQualid = starImport.getQualid().withName(starImport
                    .getQualid().getName().withSimpleName(other));

            // Set type attribution on the unfolded import so downstream recipes
            // can properly match and transform it
            if (!starImport.isStatic()) {
                String fqn = starImport.getPackageName() + "." + other;
                JavaType.FullyQualified typeForImport = findType(fqn, cu);
                newQualid = newQualid.withType(typeForImport);
            }

            J.Import unfolded = starImport.withQualid(newQualid).withId(randomId());
            unfoldedImports.add(i++ == 0 ? unfolded : unfolded.withPrefix(Space.format("\n")));
        }
        return unfoldedImports;
    }

    /**
     * Find a fully qualified type by name. First checks TypesInUse for a fully hydrated type,
     * then checks the JavaSourceSet classpath, and falls back to creating a ShallowClass.
     */
    private JavaType.FullyQualified findType(String fqn, JavaSourceFile cu) {
        // First, check if the type is already used in the source file (fully hydrated)
        for (JavaType javaType : cu.getTypesInUse().getTypesInUse()) {
            if (javaType instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified fq = (JavaType.FullyQualified) javaType;
                if (TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), fqn)) {
                    return fq;
                }
            }
        }

        // Check the JavaSourceSet classpath
        Optional<JavaSourceSet> sourceSet = cu.getMarkers().findFirst(JavaSourceSet.class);
        if (sourceSet.isPresent()) {
            for (JavaType.FullyQualified fq : sourceSet.get().getClasspath()) {
                if (TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), fqn)) {
                    return fq;
                }
            }
        }

        // Fall back to creating a ShallowClass
        return JavaType.ShallowClass.build(fqn);
    }
}
