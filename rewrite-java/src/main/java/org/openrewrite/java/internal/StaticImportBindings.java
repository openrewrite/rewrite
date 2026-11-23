/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RandomizeIdVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;

/** Retains single static imports whose precedence over on-demand imports is needed by the source. */
public final class StaticImportBindings {
    private static final int METHOD = 1;
    private static final int FIELD = 2;
    private static final int MEMBER_TYPE = 4;

    private StaticImportBindings() {
    }

    public static J.CompilationUnit preserve(J.CompilationUnit before, J.CompilationUnit after) {
        if (before.getImports().isEmpty() || after.getImports().isEmpty()) {
            return after;
        }
        Set<String> used = new HashSet<>();
        Map<String, String> bindings = new HashMap<>();
        Map<String, Integer> namespaces = new HashMap<>();
        new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.Import visitImport(J.Import anImport, Set<String> names) {
                return anImport;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<String> names) {
                if (method.getSelect() == null) {
                    names.add(method.getSimpleName());
                    namespaces.merge(method.getSimpleName(), METHOD, (a, b) -> a | b);
                    bindings.put(method.getSimpleName(), method.getMethodType() == null ? "" :
                            method.getMethodType().getDeclaringType().getFullyQualifiedName().replace('$', '.'));
                }
                return super.visitMethodInvocation(method, names);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> names) {
                Object parent = getCursor().getParentOrThrow().getValue();
                if (parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == identifier) {
                    return identifier;
                }
                names.add(identifier.getSimpleName());
                JavaType.Variable field = identifier.getFieldType();
                if (field != null && field.hasFlags(Flag.Static) && field.getOwner() instanceof JavaType.FullyQualified) {
                    bindings.put(identifier.getSimpleName(), ((JavaType.FullyQualified) field.getOwner()).getFullyQualifiedName().replace('$', '.'));
                    namespaces.merge(identifier.getSimpleName(), FIELD, (a, b) -> a | b);
                }
                JavaType.FullyQualified memberType = TypeUtils.asFullyQualified(identifier.getType());
                if (memberType != null && memberType.getOwningClass() != null &&
                        memberType.getClassName().endsWith("." + identifier.getSimpleName())) {
                    bindings.put(identifier.getSimpleName(), memberType.getOwningClass().getFullyQualifiedName().replace('$', '.'));
                    namespaces.merge(identifier.getSimpleName(), MEMBER_TYPE, (a, b) -> a | b);
                }
                return identifier;
            }
        }.visit(before, used);

        List<JRightPadded<J.Import>> imports = after.getPadding().getImports();
        Set<String> originalStars = new HashSet<>();
        Map<String, List<JRightPadded<J.Import>>> originalGroups = new HashMap<>();
        for (JRightPadded<J.Import> padded : before.getPadding().getImports()) {
            J.Import anImport = padded.getElement();
            if (anImport.isStatic()) {
                String owner = anImport.getQualid().getTarget().printTrimmed();
                originalGroups.computeIfAbsent(owner, k -> new ArrayList<>()).add(padded);
                if ("*".equals(anImport.getQualid().getSimpleName())) {
                    originalStars.add(owner);
                } else {
                    // A single import selects its owner even when the member is inherited.
                    if (used.contains(anImport.getQualid().getSimpleName())) {
                        bindings.put(anImport.getQualid().getSimpleName(), owner);
                    }
                }
            }
        }
        // Only imported bindings can be affected. Qualified references and locally declared
        // members must not prevent otherwise safe folds in an unrelated static namespace.
        bindings.entrySet().removeIf(binding -> {
            if (originalGroups.containsKey(binding.getValue())) {
                return false;
            }
            for (J.Import anImport : before.getImports()) {
                if (anImport.isStatic() && "*".equals(anImport.getQualid().getSimpleName()) &&
                        (binding.getValue().isEmpty() || TypeUtils.isAssignableTo(binding.getValue(),
                                anImport.getQualid().getTarget().getType()))) {
                    return false;
                }
            }
            return true;
        });
        for (JRightPadded<J.Import> padded : after.getPadding().getImports()) {
            J.Import anImport = padded.getElement();
            String owner = anImport.getQualid().getTarget().printTrimmed();
            if (!anImport.isStatic() || !"*".equals(anImport.getQualid().getSimpleName()) || originalStars.contains(owner)) {
                continue;
            }
            for (Map.Entry<String, String> binding : bindings.entrySet()) {
                if (!owner.equals(binding.getValue()) &&
                        mayDeclare(anImport.getQualid().getTarget().getType(), binding.getKey(), namespaces.getOrDefault(binding.getKey(), 7), new HashSet<>())) {
                    List<JRightPadded<J.Import>> group = originalGroups.get(owner);
                    if (group != null) {
                        imports = new ArrayList<>(imports);
                        int index = 0;
                        while (imports.get(index) != padded) {
                            index++;
                        }
                        imports.remove(index);
                        for (JRightPadded<J.Import> original : group) {
                            boolean present = false;
                            for (JRightPadded<J.Import> existing : imports) {
                                if (existing.getElement().isStatic() && existing.getElement().getQualid().printTrimmed()
                                        .equals(original.getElement().getQualid().printTrimmed())) {
                                    present = true;
                                    break;
                                }
                            }
                            if (!present) {
                                imports.add(index++, original);
                            }
                        }
                    }
                    break;
                }
            }
        }
        Set<String> paths = new HashSet<>();
        List<J.Import> stars = new ArrayList<>();
        for (JRightPadded<J.Import> paddedImport : imports) {
            J.Import anImport = paddedImport.getElement();
            if (anImport.isStatic()) {
                paths.add(anImport.getQualid().printTrimmed());
                if ("*".equals(anImport.getQualid().getSimpleName())) {
                    stars.add(anImport);
                }
            }
        }
        for (JRightPadded<J.Import> padded : before.getPadding().getImports()) {
            J.Import single = padded.getElement();
            String name = single.getQualid().getSimpleName();
            if (!single.isStatic() || "*".equals(name) || !used.contains(name) ||
                    paths.contains(single.getQualid().printTrimmed())) {
                continue;
            }
            String owner = single.getQualid().getTarget().printTrimmed();
            for (J.Import star : stars) {
                if (!owner.equals(star.getQualid().getTarget().printTrimmed()) &&
                        mayDeclare(star.getQualid().getTarget().getType(), name, namespaces.getOrDefault(name, 7), new HashSet<>())) {
                    // Keep the folded group, restoring only the single import needed to select
                    // its original owner. This also works with a legacy, shared ImportLayoutStyle.
                    imports = new ArrayList<>(imports);
                    int index = imports.size();
                    for (int i = 0; i < imports.size(); i++) {
                        J.Import existing = imports.get(i).getElement();
                        if (existing.isStatic() && owner.equals(existing.getQualid().getTarget().printTrimmed())) {
                            index = i + 1;
                        }
                    }
                    JRightPadded<J.Import> retained = padded;
                    for (JRightPadded<J.Import> existing : imports) {
                        if (existing.getElement().getId().equals(single.getId())) {
                            // Folding may reuse the first single import's tree id for the star.
                            retained = padded.withElement((J.Import) new RandomizeIdVisitor<Integer>().visit(single, 0));
                            break;
                        }
                    }
                    imports.add(index, retained);
                    paths.add(single.getQualid().printTrimmed());
                    break;
                }
            }
        }
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < imports.size(); i++) {
            JRightPadded<J.Import> padded = imports.get(i);
            if (!ids.add(padded.getElement().getId())) {
                imports = new ArrayList<>(imports);
                imports.set(i, padded.withElement((J.Import) new RandomizeIdVisitor<Integer>().visit(padded.getElement(), 0)));
            }
        }
        List<JRightPadded<J.Import>> original = before.getPadding().getImports();
        if (imports.size() == original.size()) {
            boolean same = true;
            for (int i = 0; i < imports.size(); i++) {
                if (imports.get(i) != original.get(i)) {
                    same = false;
                    break;
                }
            }
            if (same) {
                if (before.getClasses() == after.getClasses() && before.getMarkers() == after.getMarkers() &&
                        before.getPrefix() == after.getPrefix() && before.getEof() == after.getEof() &&
                        before.getPackageDeclaration() == after.getPackageDeclaration()) {
                    return before;
                }
                imports = original;
            }
        }
        return after.getPadding().withImports(imports);
    }

    private static boolean mayDeclare(@Nullable JavaType type, String name, int namespace, Set<String> visited) {
        JavaType.FullyQualified owner = TypeUtils.asFullyQualified(type);
        if (owner == null || owner instanceof JavaType.ShallowClass) {
            // Project types need not be in JavaSourceSet's external classpath. Missing target
            // metadata cannot establish that removing a single import is safe.
            return true;
        }
        if (!visited.add(owner.getFullyQualifiedName())) {
            return false;
        }
        for (JavaType.Method method : owner.getMethods()) {
            if ((namespace & METHOD) != 0 && method.hasFlags(Flag.Static) && !method.hasFlags(Flag.Private) && name.equals(method.getName())) {
                return true;
            }
        }
        for (JavaType.Variable field : owner.getMembers()) {
            if ((namespace & FIELD) != 0 && field.hasFlags(Flag.Static) && !field.hasFlags(Flag.Private) && name.equals(field.getName())) {
                return true;
            }
        }
        if (owner.getSupertype() != null && mayDeclare(owner.getSupertype(), name, namespace, visited)) {
            return true;
        }
        for (JavaType.FullyQualified anInterface : owner.getInterfaces()) {
            if (mayDeclare(anInterface, name, namespace & ~METHOD, visited)) {
                return true;
            }
        }
        // FullyQualified exposes fields/methods and ancestry, but no complete inventory of
        // enclosed types. Keep type-name disambiguation when absence cannot be established.
        return (namespace & MEMBER_TYPE) != 0;
    }
}
