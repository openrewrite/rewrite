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
import org.openrewrite.Cursor;
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

    private final String simpleName;

    @EqualsAndHashCode.Include
    private final boolean force;

    public RemoveImport(String type) {
        this(type, false);
    }

    @JsonCreator
    public RemoveImport(String type, boolean force) {
        this.type = type;
        this.owner = type.substring(0, Math.max(0, type.lastIndexOf('.')));
        this.simpleName = type.substring(Math.max(type.lastIndexOf('.'), type.lastIndexOf('$')) + 1);
        this.force = force;
    }

    @Override
    public @Nullable J preVisit(J tree, P p) {
        stopAfterPreVisit();
        J j = tree;
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            ImportLayoutStyle importLayoutStyle = Style.from(ImportLayoutStyle.class, cu, IntelliJ::importLayout);

            Set<String> otherTypesInPackageUsed = new TreeSet<>();

            Set<String> methodsAndFieldsUsed = new HashSet<>();
            Set<String> otherMethodsAndFieldsInTypeUsed = new TreeSet<>();
            for (JavaType.Variable variable : cu.getTypesInUse().getVariables()) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getOwner());
                if (fq != null && (TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), type) ||
                        TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), owner))) {
                    methodsAndFieldsUsed.add(variable.getName());
                }
            }
            // a static import of a nested type also imports a field of the same name
            boolean fieldNamedLikeTypeUsed = cu.getTypesInUse().getVariables().stream().anyMatch(variable -> {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getOwner());
                return simpleName.equals(variable.getName()) && fq != null &&
                       TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName() + "." + simpleName, type);
            });

            for (JavaType.Method method : cu.getTypesInUse().getUsedMethods()) {
                if (method.hasFlags(Flag.Static)) {
                    String declaringType = method.getDeclaringType().getFullyQualifiedName();
                    if (TypeUtils.fullyQualifiedNamesAreEqual(declaringType, type)) {
                        methodsAndFieldsUsed.add(method.getName());
                    } else if (declaringType.equals(owner)) {
                        if (method.getName().equals(simpleName)) {
                            methodsAndFieldsUsed.add(method.getName());
                        } else {
                            otherMethodsAndFieldsInTypeUsed.add(method.getName());
                        }
                    }
                }
            }

            int memberCount = methodsAndFieldsUsed.size() + otherMethodsAndFieldsInTypeUsed.size();
            Set<J.Import> staticWildcardsToChange = new HashSet<>();
            Set<String> originalImports = new HashSet<>();
            boolean starImportOfOwner = false;
            boolean memberImportOfType = false;
            for (J.Import cuImport : cu.getImports()) {
                JavaType.FullyQualified importedType = TypeUtils.asFullyQualified(cuImport.getQualid().getType());
                if (importedType != null) {
                    originalImports.add(importedType.getFullyQualifiedName().replace("$", "."));
                }
                if (cuImport.isStatic() && "*".equals(cuImport.getQualid().getSimpleName())) {
                    String importedFrom = cuImport.getTypeName();
                    if ((TypeUtils.fullyQualifiedNamesAreEqual(importedFrom, type) ||
                            TypeUtils.fullyQualifiedNamesAreEqual(importedFrom, owner)) &&
                            (memberCount == 0 || memberCount < importLayoutStyle.getNameCountToUseStarImport() &&
                             !isPackageAlwaysFolded(importLayoutStyle.getPackagesToFold(), cuImport))) {
                        staticWildcardsToChange.add(cuImport);
                    }
                }
                if (isMemberImportOf(cuImport)) {
                    memberImportOfType = true;
                }
                if (!cuImport.isStatic() && "*".equals(cuImport.getClassName()) &&
                        cuImport.getPackageName().equals(owner)) {
                    starImportOfOwner = true;
                }
            }

            boolean hasStaticNestedType = false;
            boolean typeInUse = false;
            for (JavaType javaType : cu.getTypesInUse().getTypesInUse()) {
                if (javaType instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) javaType;
                    if (!hasStaticNestedType && !staticWildcardsToChange.isEmpty() &&
                            fullyQualified.getOwningClass() != null && fullyQualified.hasFlags(Flag.Static)) {
                        hasStaticNestedType = staticWildcardsToChange.stream().anyMatch(wildcard ->
                                hasMemberType(wildcard.getQualid().getTarget().getType(), fullyQualified));
                    }
                    if (TypeUtils.fullyQualifiedNamesAreEqual(fullyQualified.getFullyQualifiedName(), type)) {
                        typeInUse = true;
                    } else if (TypeUtils.fullyQualifiedNamesAreEqual(fullyQualified.getFullyQualifiedName(), owner) ||
                            TypeUtils.fullyQualifiedNamesAreEqual(fullyQualified.getPackageName(), owner)) {
                        // A package star import provides only top-level types, so a nested type such as Map.Entry
                        // is a candidate through its top-level class.
                        String className = fullyQualified.getClassName();
                        int firstDot = className.indexOf('.');
                        String topLevelClassName = firstDot < 0 ? className : className.substring(0, firstDot);
                        String topLevelType = fullyQualified.getPackageName() + "." + topLevelClassName;
                        if (!TypeUtils.fullyQualifiedNamesAreEqual(topLevelType, type) &&
                                !originalImports.contains(topLevelType)) {
                            otherTypesInPackageUsed.add(topLevelClassName);
                        }
                    }
                }
            }

            boolean typeImportMayStay = !force && typeInUse;
            boolean checkNames = typeImportMayStay || !force && memberImportOfType ||
                    starImportOfOwner && !otherTypesInPackageUsed.isEmpty();
            Set<String> namesInSource = checkNames || hasStaticNestedType ?
                    namesInSource(cu, hasStaticNestedType ? staticWildcardsToChange : Collections.emptySet()) :
                    Collections.emptySet();
            boolean typeUsed = typeImportMayStay && namesInSource.contains(simpleName);
            otherTypesInPackageUsed.retainAll(namesInSource);

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
                J.Identifier alias = import_.getAlias();
                if (import_.isStatic()) {
                    String imported = import_.getQualid().getSimpleName();
                    if (TypeUtils.fullyQualifiedNamesAreEqual(typeName + "." + imported, type) &&
                            (force || !methodsAndFieldsUsed.contains(imported) && !fieldNamedLikeTypeUsed)) {
                        // e.g. remove java.util.Collections.emptySet when type is java.util.Collections.emptySet
                        spaceForNextImport.set(import_.getPrefix());
                        return null;
                    } else if ("*".equals(imported)) {
                        if (staticWildcardsToChange.contains(import_)) {
                            if (memberCount == 0) {
                                spaceForNextImport.set(import_.getPrefix());
                                return null;
                            }
                            Set<String> membersUsed = new HashSet<>(methodsAndFieldsUsed);
                            membersUsed.addAll(otherMethodsAndFieldsInTypeUsed);
                            return unfoldStarImport(import_, membersUsed, cu);
                        }
                    } else if (TypeUtils.fullyQualifiedNamesAreEqual(typeName, type) && !methodsAndFieldsUsed.contains(imported)) {
                        // e.g. remove java.util.Collections.emptySet when type is java.util.Collections
                        spaceForNextImport.set(import_.getPrefix());
                        return null;
                    }
                } else if (isMemberImportOf(import_)) {
                    String name = alias != null ? alias.getSimpleName() : import_.getQualid().getSimpleName();
                    if (force || !namesInSource.contains(name)) {
                        spaceForNextImport.set(import_.getPrefix());
                        return null;
                    }
                } else if (alias != null && TypeUtils.fullyQualifiedNamesAreEqual(typeName, type)) {
                    if (!typeImportMayStay || !namesInSource.contains(alias.getSimpleName())) {
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
                    } else if (type.lastIndexOf('$') > type.lastIndexOf('.')) {
                        // the star import never provided a nested type, so it has no reason to change while in use
                        return import_;
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

    // Java member imports are always static; other languages, e.g. Scala, can import a member without `static`
    private boolean isMemberImportOf(J.Import import_) {
        JavaType importedType = import_.getQualid().getType();
        if (import_.isStatic() || !(importedType instanceof JavaType.Method || importedType instanceof JavaType.Variable)) {
            return false;
        }
        String typeName = import_.getTypeName();
        String memberOwner = typeName.substring(0, Math.max(0, Math.max(typeName.lastIndexOf('.'), typeName.lastIndexOf('$'))));
        return TypeUtils.fullyQualifiedNamesAreEqual(typeName, type) || TypeUtils.fullyQualifiedNamesAreEqual(memberOwner, type);
    }

    /**
     * The simple names the source writes outside its imports. A type that only the type attribution carries,
     * such as the inferred type of a {@code var} or of a variable read, needs no import.
     */
    static Set<String> namesInSource(JavaSourceFile cu) {
        return namesInSource(cu, Collections.emptySet());
    }

    private static Set<String> namesInSource(JavaSourceFile cu, Set<J.Import> staticWildcardsToChange) {
        Map<String, List<JavaType>> singleImportOwners = new HashMap<>();
        if (!staticWildcardsToChange.isEmpty()) {
            for (J.Import i : cu.getImports()) {
                JavaType target = i.getQualid().getTarget().getType();
                if (target != null && !"*".equals(i.getQualid().getSimpleName())) {
                    String name = i.getAlias() == null ? i.getQualid().getSimpleName() : i.getAlias().getSimpleName();
                    singleImportOwners.computeIfAbsent(name, k -> new ArrayList<>()).add(target);
                }
            }
        }
        J.Package pkg = cu.getPackageDeclaration();
        String packageName = staticWildcardsToChange.isEmpty() || pkg == null ? "" : pkg.getPackageName();
        return new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.Import visitImport(J.Import import_, Set<String> names) {
                return import_;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> names) {
                names.add(identifier.getSimpleName());
                if (!staticWildcardsToChange.isEmpty() && identifier.getFieldType() == null) {
                    retainRequiredWildcards(identifier, getCursor(), packageName, singleImportOwners, staticWildcardsToChange);
                }
                return super.visitIdentifier(identifier, names);
            }
        }.reduce(cu, new HashSet<>());
    }

    private static void retainRequiredWildcards(J.Identifier identifier, Cursor cursor, String packageName,
                                                Map<String, List<JavaType>> singleImportOwners,
                                                Set<J.Import> wildcardsToChange) {
        JavaType.FullyQualified nested = TypeUtils.asFullyQualified(identifier.getType());
        if (nested == null || nested.getOwningClass() == null || !nested.hasFlags(Flag.Static) ||
                nested.hasFlags(Flag.Private) || !nested.hasFlags(Flag.Public) && !nested.getPackageName().equals(packageName) ||
                !nested.getClassName().endsWith("." + identifier.getSimpleName())) {
            return;
        }

        Object parent = cursor.getParentTreeCursor().getValue();
        if (parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == identifier ||
                isInheritingScope(cursor, nested)) {
            return;
        }
        List<JavaType> owners = singleImportOwners.get(identifier.getSimpleName());
        if (owners != null && owners.stream().anyMatch(t -> hasMemberType(t, nested))) {
            return;
        }
        wildcardsToChange.removeIf(wildcard ->
                hasMemberType(wildcard.getQualid().getTarget().getType(), nested));
    }

    private static boolean isInheritingScope(Cursor cursor, JavaType.FullyQualified nested) {
        for (Iterator<Cursor> blocks = cursor.getPathAsCursors(c -> c.getValue() instanceof J.Block); blocks.hasNext();) {
            Object parent = blocks.next().getParentTreeCursor().getValue();
            if ((parent instanceof J.ClassDeclaration || parent instanceof J.NewClass) &&
                    hasMemberType(((TypedTree) parent).getType(), nested)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMemberType(@Nullable JavaType from, JavaType.FullyQualified nested) {
        JavaType.FullyQualified owner = nested.getOwningClass();
        if (owner == null) {
            return false;
        }
        if (nested.hasFlags(Flag.Public) || nested.hasFlags(Flag.Protected)) {
            return TypeUtils.isAssignableTo(owner.getFullyQualifiedName(), from);
        }
        // A package-private member stops being inherited at the first superclass outside its package.
        for (JavaType.FullyQualified t = TypeUtils.asFullyQualified(from);
             t != null && owner.getPackageName().equals(t.getPackageName()); t = t.getSupertype()) {
            if (TypeUtils.isOfClassType(t, owner.getFullyQualifiedName())) {
                return true;
            }
        }
        return false;
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
                    // an import names the raw type, not the parameterization of it that the source uses
                    return fq instanceof JavaType.Parameterized ? ((JavaType.Parameterized) fq).getType() : fq;
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

    /**
     * The names to pass to {@link JavaVisitor#maybeRemoveImport(String)} for the types outside {@code java.lang} and
     * the static members {@code tree} references, such as {@code java.util.List} or
     * {@code java.util.Collections.emptyList}.
     */
    static Set<String> namesReferencedBy(J tree) {
        return new JavaIsoVisitor<Set<String>>() {
            @Override
            public @Nullable JavaType visitType(@Nullable JavaType javaType, Set<String> names) {
                if (javaType instanceof JavaType.Class) {
                    JavaType.Class type = (JavaType.Class) javaType;
                    if (!"java.lang".equals(type.getPackageName())) {
                        names.add(type.getFullyQualifiedName());
                    }
                } else if (javaType instanceof JavaType.Variable) {
                    JavaType.Variable variable = (JavaType.Variable) javaType;
                    if (variable.hasFlags(Flag.Static) && variable.getOwner() instanceof JavaType.Class) {
                        JavaType.Class owner = (JavaType.Class) variable.getOwner();
                        names.add(owner.getFullyQualifiedName() + "." + variable.getName());
                    }
                }
                return super.visitType(javaType, names);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, Set<String> names) {
                if (mi.getMethodType() != null && mi.getMethodType().hasFlags(Flag.Static) && mi.getSelect() == null) {
                    JavaType.FullyQualified receiverType = mi.getMethodType().getDeclaringType();
                    names.add(receiverType.getFullyQualifiedName() + "." + mi.getSimpleName());
                }
                return super.visitMethodInvocation(mi, names);
            }
        }.reduce(tree, new HashSet<>());
    }
}
