/*
 * Copyright 2025 the original author or authors.
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.ImportAnalyzer.ImportStatus.*;

public class ImportAnalyzer {

    private static final J.Import JAVA_LANG = new J.Import(randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
            new JLeftPadded<>(Space.EMPTY,false, Markers.EMPTY), TypeTree.build("java.lang.*"), null);

    private final List<ImportEntry> imports;
    private final Set<JavaType.FullyQualified> declaredTypes;
    private final Set<JavaType> declaredStaticMembers;

    @Getter private final List<ImportEntry> unusedImports = new ArrayList<>();
    @Getter private final List<ImportEntry> ambiguousImports = new ArrayList<>();
    @Getter private final List<JavaType.FullyQualified> missingTypeImports = new ArrayList<>();
    @Getter private final List<JavaType> missingMemberImports = new ArrayList<>();

    private ImportAnalyzer(List<ImportEntry> imports, Set<JavaType.FullyQualified> declaredTypes, Set<JavaType> declaredStaticMembers) {
        this.imports = imports;
        this.declaredTypes = declaredTypes;
        this.declaredStaticMembers = declaredStaticMembers;
    }

    public static ImportAnalyzer init(J.CompilationUnit cu) {
        Set<JavaType.FullyQualified> typeReferences = new FindUnqualifiedTypeReference().reduce(cu, new HashSet<>());
        Set<JavaType> memberReferences = new FindStaticReferences().reduce(cu, new HashSet<>());
        Set<JavaType.FullyQualified> declaredTypes = new FindDeclaredTypes().reduce(cu, new HashSet<>());
        Set<JavaType> declaredStaticMembers = new FindDeclaredStaticMembers().reduce(cu, new HashSet<>());

        List<J.Import> jImports = cu.getImports();
        // explicit imports first, wildcard later
        jImports.sort((i1, i2) ->
                i1.getQualid().getSimpleName().equals("*") ^ i2.getQualid().getSimpleName().equals("*")
                        ? i1.getQualid().getSimpleName().equals("*") ? 1 : -1 : 0);
        List<ImportEntry> imports = Stream.concat(jImports.stream(), Stream.of(JAVA_LANG))
                .map(imp -> ImportEntry.create(imp, typeReferences, memberReferences))
                .collect(Collectors.toList());

        String packageName = cu.getPackageDeclaration() == null ? "" : cu.getPackageDeclaration().getPackageName();

        ImportAnalyzer importManager = new ImportAnalyzer(imports, declaredTypes, declaredStaticMembers);
        importManager.populateUnusedImports();
        importManager.populateAmbiguousImports();
        importManager.populateMissingTypeImports(typeReferences, packageName);
        importManager.populateMissingMemberImports(memberReferences);
        return importManager;
    }

    private void populateUnusedImports() {
        for (ImportEntry importEntry : imports) {
            if (importEntry.getResolvedTypes().isEmpty() && importEntry.getResolvedMembers().isEmpty()) {
                unusedImports.add(importEntry);
            }
        }
    }

    private void populateAmbiguousImports() {
        collectAmbiguousImports(imports, ImportEntry::getResolvedTypes);
        collectAmbiguousImports(imports, ImportEntry::getResolvedMembers);
    }

    private void populateMissingTypeImports(Set<JavaType.FullyQualified> typeReferences, String packageName) {
        Set<JavaType.FullyQualified> importedTypes = imports.stream().map(ImportEntry::getResolvedTypes).flatMap(List::stream).collect(Collectors.toSet());
        missingTypeImports.addAll(typeReferences.stream()
                .filter(c -> !importedTypes.contains(c) && !declaredTypes.contains(c) && !c.getPackageName().equals(packageName))
                .distinct()
                .collect(Collectors.toList()));
    }

    private void populateMissingMemberImports(Set<JavaType> memberReferences) {
        Set<JavaType> importedMembers = imports.stream().map(ImportEntry::getResolvedMembers).flatMap(List::stream).collect(Collectors.toSet());
        missingMemberImports.addAll(memberReferences.stream()
                .filter(m -> !importedMembers.contains(m) && !declaredStaticMembers.contains(m))
                .distinct()
                .collect(Collectors.toList()));
    }

    static @Nullable String toMemberName(JavaType javaType) {
        if (javaType instanceof JavaType.Variable) {
            JavaType.Variable variable = (JavaType.Variable) javaType;
            if (variable.getOwner() instanceof JavaType.FullyQualified) {
                return ((JavaType.FullyQualified) variable.getOwner()).getFullyQualifiedName() + "." + variable.getName();
            }
            return null;
        } else if (javaType instanceof JavaType.Method) {
            JavaType.Method javaMethod = (JavaType.Method) javaType;
            return javaMethod.getDeclaringType().getFullyQualifiedName() + "." + javaMethod.getName();
        } else {
            return null;
        }
    }

    private void collectAmbiguousImports(List<ImportEntry> imports, Function<ImportEntry, List<? extends JavaType>> resolver) {
        Map<String, Set<ImportEntry>> importsBySimpleName = new HashMap<>();
        for (ImportEntry entry : imports) {
            for (JavaType type : resolver.apply(entry)) {
                String name = null;
                if (type instanceof JavaType.FullyQualified) {
                    name = ((JavaType.FullyQualified) type).getClassName();
                }
                if (type instanceof JavaType.Variable) {
                    name = ((JavaType.Variable) type).getName();
                }
                if (type instanceof JavaType.Method) {
                    name = ((JavaType.Method) type).getName();
                }
                if (name != null) {
                    importsBySimpleName.computeIfAbsent(name, k -> new HashSet<>()).add(entry);
                }
            }
        }
        ambiguousImports.addAll(importsBySimpleName.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList()));
    }

    public ImportStatus checkTypeForImport(String typeName) {
        if (declaredTypes.stream().anyMatch(d -> d.getFullyQualifiedName().equals(typeName))) {
            return IMPLICITLY_IMPORTED;
        }
        Optional<ImportEntry> maybeImportEntry = imports.stream().filter(entry -> {
            if (entry.isMemberImport()) {
                return false;
            }
            if (!entry.isWildcardImport()) {
                return entry.getTypeName().equals(typeName);
            }
            return prefixBeforeLastDot(entry.getTypeName()).equals(prefixBeforeLastDot(typeName));
        }).findFirst();
        if (maybeImportEntry.isPresent() && !maybeImportEntry.get().isWildcardImport()) {
            return EXPLICITLY_IMPORTED;
        }
        String simpleName = suffix(typeName);
        if (imports.stream()
                .flatMap(entry -> entry.getResolvedTypes().stream())
                .anyMatch(resolvedType -> !resolvedType.getFullyQualifiedName().equals(typeName) && resolvedType.getClassName().equals(simpleName))) {
            return IMPORT_AMBIGUITY;
        }
        return maybeImportEntry.isPresent() ? IMPLICITLY_IMPORTED : NOT_IMPORTED;
    }

    public ImportStatus checkMemberForImport(String typeName, String memberName) {
        if (declaredStaticMembers.stream().anyMatch(d -> {
            String fqn = toMemberName(d);
            return fqn != null && fqn.equals(typeName + "." + memberName);
        })) {
            return IMPLICITLY_IMPORTED;
        }
        Optional<ImportEntry> maybeImportEntry = imports.stream().filter(entry -> {
            if (!entry.isMemberImport() || !entry.getTypeName().equals(typeName)) {
                return false;
            }
            return entry.isWildcardImport() || memberName.equals(entry.getMemberName());
        }).findFirst();
        if (maybeImportEntry.isPresent() && !maybeImportEntry.get().isWildcardImport()) {
            return EXPLICITLY_IMPORTED;
        }
        if (imports.stream()
                .flatMap(entry -> entry.getResolvedMembers().stream())
                .anyMatch(member -> !(typeName + "." + memberName).equals(toMemberName(member)) && memberName.equals(getVarOrMethodName(member)))) {
            return IMPORT_AMBIGUITY;
        }
        return maybeImportEntry.isPresent() ? IMPLICITLY_IMPORTED : NOT_IMPORTED;
    }

    private @Nullable String getVarOrMethodName(JavaType javaType) {
        if (javaType instanceof JavaType.Variable) {
            return ((JavaType.Variable) javaType).getName();
        } else if (javaType instanceof JavaType.Method) {
            return ((JavaType.Method) javaType).getName();
        } else {
            return null;
        }
    }

    private String suffix(String fullyQualifiedName) {
        return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
    }

    private static String prefixBeforeLastDot(String s) {
        int idx = s.lastIndexOf('.');
        return (idx != -1) ? s.substring(0, idx) : "";
    }

    private static boolean isStaticClassVariable(JavaType.@Nullable Variable variable) {
        return variable != null
                && variable.getOwner() instanceof JavaType.Class
                && variable.hasFlags(Flag.Static);
    }

    public enum ImportStatus {
        NOT_IMPORTED,
        IMPLICITLY_IMPORTED,
        EXPLICITLY_IMPORTED,
        IMPORT_AMBIGUITY,
    }

    @Value
    public static class ImportEntry {
        String typeName;
        boolean isWildcardImport;
        @Nullable String memberName;
        @Getter(AccessLevel.PRIVATE)
        List<JavaType.FullyQualified> resolvedTypes = new ArrayList<>();
        @Getter(AccessLevel.PRIVATE)
        List<JavaType> resolvedMembers = new ArrayList<>();

        public boolean isMemberImport() {
            return memberName != null;
        }

        public String getImportName() {
            return typeName + (memberName != null ? "." + memberName : "");
        }

        private static ImportEntry create(J.Import imp, Set<JavaType.FullyQualified> typeReferences, Set<JavaType> memberReferences) {
            boolean isWildcardImport = imp.getQualid().getSimpleName().equals("*");
            ImportEntry importEntry;
            if (imp.isStatic()) {
                importEntry = new ImportEntry(
                        imp.getQualid().getTarget().printTrimmed(new JavaPrinter<>()),
                        isWildcardImport,
                        imp.getQualid().getSimpleName());
                importEntry.populateResolvedMembers(memberReferences);
            } else {
                importEntry = new ImportEntry(imp.getQualid().printTrimmed(new JavaPrinter<>()), isWildcardImport, null);
                importEntry.populateResolvedTypes(typeReferences);
            }
            return importEntry;
        }

        private void populateResolvedTypes(Set<JavaType.FullyQualified> typeReferences) {
            if (isMemberImport()) {
                return;
            }
            List<JavaType.FullyQualified> matched = typeReferences.stream().filter(fqn -> {
                if (!isWildcardImport) {
                    return fqn.getFullyQualifiedName().equals(typeName);
                }
                return prefixBeforeLastDot(fqn.getFullyQualifiedName()).equals(prefixBeforeLastDot(typeName));
            }).collect(Collectors.toList());
            resolvedTypes.addAll(matched);
            matched.forEach(typeReferences::remove);
        }

        private void populateResolvedMembers(Set<JavaType> memberReferences) {
            if (!isMemberImport()) {
                return;
            }
            List<JavaType> matched = memberReferences.stream().filter(member -> {
                String fullyQualifiedMemberName = toMemberName(member);
                if (!isWildcardImport) {
                    return getImportName().equals(fullyQualifiedMemberName);
                }
                return fullyQualifiedMemberName != null && typeName.equals(prefixBeforeLastDot(fullyQualifiedMemberName));
            }).collect(Collectors.toList());
            resolvedMembers.addAll(matched);
            matched.forEach(memberReferences::remove);
        }
    }

    private static class FindUnqualifiedTypeReference extends JavaVisitor<Set<JavaType.FullyQualified>> {
        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, Set<JavaType.FullyQualified> types) {
            JavaType.FullyQualified fieldAccessType = TypeUtils.asFullyQualified(fieldAccess.getType());
            if (fieldAccessType != null) {
                if (fieldAccess.isFullyQualifiedClassReference(fieldAccessType.getFullyQualifiedName())) {
                    return fieldAccess;
                }
            }
            return super.visitFieldAccess(fieldAccess, types);
        }

        @Override
        public J visitIdentifier(J.Identifier identifier, Set<JavaType.FullyQualified> types) {
            if (identifier.getFieldType() != null) {
                return super.visitIdentifier(identifier, types);
            }
            JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(identifier.getType());
            if (fullyQualified != null) {
                types.add(fullyQualified);
            }
            return super.visitIdentifier(identifier, types);
        }
    }

    private static class FindStaticReferences extends JavaIsoVisitor<Set<JavaType>> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<JavaType> memberReferences) {
            if (method.getSelect() == null && method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static)) {
                memberReferences.add(method.getMethodType());
            }
            return super.visitMethodInvocation(method, memberReferences);
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Set<JavaType> memberReferences) {
            if (!(identifier.getType() instanceof JavaType.Variable)) {
                return identifier;
            }
            JavaType.Variable variable = identifier.getFieldType();
            if (isStaticClassVariable(variable)) {
                memberReferences.add(variable);
            }
            return super.visitIdentifier(identifier, memberReferences);
        }
    }

    private static class FindDeclaredTypes extends JavaIsoVisitor<Set<JavaType.FullyQualified>> {
        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<JavaType.FullyQualified> declaredTypes) {
            if (classDecl.getType() != null) {
                declaredTypes.add(classDecl.getType());
            }
            return super.visitClassDeclaration(classDecl, declaredTypes);
        }
    }

    private static class FindDeclaredStaticMembers extends JavaIsoVisitor<Set<JavaType>> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Set<JavaType> declaredMembers) {
            if (method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static)) {
                declaredMembers.add(method.getMethodType());
            }
            return super.visitMethodDeclaration(method, declaredMembers);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<JavaType> declaredMembers) {
            if (isStaticClassVariable(variable.getVariableType())) {
                declaredMembers.add(variable.getVariableType());
            }
            return super.visitVariable(variable, declaredMembers);
        }
    }
}
