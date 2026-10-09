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
            Set<String> ownerAndSupertypes = new HashSet<>();
            for (J.Import cuImport : cu.getImports()) {
                JavaType.FullyQualified qualidType = TypeUtils.asFullyQualified(cuImport.getQualid().getType());
                if (qualidType != null) {
                    originalImports.add(qualidType.getFullyQualifiedName().replace("$", "."));
                    if (TypeUtils.fullyQualifiedNamesAreEqual(qualidType.getFullyQualifiedName(), type) &&
                            qualidType.getOwningClass() != null) {
                        collectSupertypeNames(qualidType.getOwningClass(), ownerAndSupertypes);
                    }
                }
            }

            for (JavaType.Variable variable : cu.getTypesInUse().getVariables()) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getOwner());
                if (fq != null && (TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), type) ||
                        TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), owner))) {
                    methodsAndFieldsUsed.add(variable.getName());
                }
            }

            String importedName = type.substring(type.lastIndexOf('.') + 1);
            for (JavaType.Method method : cu.getTypesInUse().getUsedMethods()) {
                JavaType.FullyQualified methodDeclaringType = method.getDeclaringType();
                if (methodDeclaringType != null &&
                        TypeUtils.fullyQualifiedNamesAreEqual(packageQualifiedName(methodDeclaringType, method.getName()), type)) {
                    methodsAndFieldsUsed.add(method.getName());
                } else if (methodDeclaringType != null && method.getName().equals(importedName) &&
                        ownerAndSupertypes.contains(TypeUtils.toFullyQualifiedName(methodDeclaringType.getFullyQualifiedName()))) {
                    methodsAndFieldsUsed.add(method.getName());
                } else if (method.hasFlags(Flag.Static)) {
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
                    import_ = import_.withPrefix(mergeRemovedImportPrefix(removedPrefix, import_.getPrefix(), true));
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
                } else if (!keepImport && TypeUtils.fullyQualifiedNamesAreEqual(typeName, type) &&
                        !methodsAndFieldsUsed.contains(import_.getQualid().getSimpleName())) {
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

            Space removedLastImportPrefix = spaceForNextImport.get();
            if (removedLastImportPrefix != null) {
                // The last import was removed, so there is no following import to carry its prefix
                // onto. Forward it to the first element after the imports (the first class, or the
                // EOF when there are none) so a preceding line's comment is not lost.
                if (!c.getClasses().isEmpty()) {
                    c = c.withClasses(ListUtils.mapFirst(c.getClasses(), cd ->
                            cd.withPrefix(mergeRemovedImportPrefix(removedLastImportPrefix, cd.getPrefix(), false))));
                } else {
                    c = c.withEof(mergeRemovedImportPrefix(removedLastImportPrefix, c.getEof(), false));
                }
            }

            if (c != cu && c.getPackageDeclaration() == null && c.getImports().isEmpty() &&
                    c.getPrefix() == Space.EMPTY) {
                doAfterVisit(new FormatFirstClassPrefix<>());
            }

            j = c;
        }

        return j;
    }

    private static String packageQualifiedName(JavaType.FullyQualified declaringType, String name) {
        String packageName = declaringType.getPackageName();
        return packageName.isEmpty() ? name : packageName + "." + name;
    }

    private static void collectSupertypeNames(JavaType.FullyQualified type, Set<String> names) {
        if (!names.add(TypeUtils.toFullyQualifiedName(type.getFullyQualifiedName()))) {
            return;
        }
        for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
            collectSupertypeNames(anInterface, names);
        }
        if (type.getSupertype() != null) {
            collectSupertypeNames(type.getSupertype(), names);
        }
    }

    private long countTrailingLinebreaks(Space space) {
        return space.getLastWhitespace().chars().filter(s -> s == '\n').count();
    }

    /**
     * Carry a removed import's prefix onto the element that now follows it. The removed import's own
     * end-of-line comment is dropped, while a preceding line's trailing comment and standalone
     * (commented-out) lines are preserved. When the follower is another import, the blank line that
     * separated import groups is preserved too; when it is the first class / EOF (the last import was
     * removed) the follower keeps its own spacing, so import-group blank lines are not pushed onto it.
     */
    private Space mergeRemovedImportPrefix(Space removedPrefix, Space targetPrefix, boolean targetIsImport) {
        // An end-of-line comment on the removed import's line is stored at the start of the follower's
        // prefix (no newline before it). It described the now-removed import, so drop it. What remains
        // is the spacing/comments that genuinely belong to the follower.
        Space followerPrefix = dropTrailingCommentOfRemovedImport(targetPrefix);

        // Case 1: the removed import carried comments in its prefix - a comment on the line above it,
        // or commented-out lines. Those belong to neither import specifically, so keep them by
        // prepending them to the follower's own comments.
        if (!removedPrefix.getComments().isEmpty()) {
            List<Comment> comments = ListUtils.concatAll(removedPrefix.getComments(), followerPrefix.getComments());
            // If the follower had no comments of its own but was separated by a wider gap (a blank
            // line starting a new import group) than the removed import, preserve that wider gap by
            // moving the follower's trailing whitespace onto the last carried comment's suffix.
            // Otherwise the comment would inherit the removed import's narrower spacing and the blank
            // line between groups would be lost.
            if (followerPrefix.getComments().isEmpty() &&
                countTrailingLinebreaks(followerPrefix) > countTrailingLinebreaks(removedPrefix)) {
                String followerWhitespace = followerPrefix.getLastWhitespace();
                comments = ListUtils.mapLast(comments, comment -> comment.withSuffix(followerWhitespace));
            }
            return removedPrefix.withComments(comments);
        }

        // Case 2: the removed import had no comments, but when the follower is another import we may
        // still need to carry the removed import's leading whitespace onto it - specifically when the
        // removed import was the first import (empty prefix, so the follower must not gain a leading
        // blank line) or started a new group (a wider blank-line gap that should be kept).
        if (targetIsImport && (removedPrefix.getLastWhitespace().isEmpty() ||
            countTrailingLinebreaks(removedPrefix) > countTrailingLinebreaks(followerPrefix))) {
            return followerPrefix.withWhitespace(removedPrefix.getLastWhitespace());
        }

        // Case 3: nothing to carry over - the follower keeps its own prefix unchanged.
        return followerPrefix;
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
