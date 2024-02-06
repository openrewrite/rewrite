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
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.openrewrite.java.style.ImportLayoutStyle.isPackageAlwaysFolded;
import static org.openrewrite.java.tree.TypeUtils.fullyQualifiedNamesAreEqual;
import static org.openrewrite.java.tree.TypeUtils.toFullyQualifiedName;

/**
 * This recipe will remove any imports for types that are not referenced within the compilation unit. This recipe
 * is aware of the import layout style and will correctly handle unfolding of wildcard imports if the import counts
 * drop below the configured values.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveUnusedImports extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove unused imports";
    }

    @Override
    public String getDescription() {
        return "Remove imports for types that are not referenced. As a precaution against incorrect changes no imports " +
               "will be removed from any source where unknown types are referenced. The most common cause of unknown " +
               "types is the use of annotation processors not supported by OpenRewrite, such as lombok.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1128");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new NoMissingTypes(), new RemoveUnusedImportsVisitor());
    }

    private static class RemoveUnusedImportsVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            ImportLayoutStyle layoutStyle = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                    .orElse(IntelliJ.importLayout());
            String sourcePackage = cu.getPackageDeclaration() == null ? "" :
                    cu.getPackageDeclaration().getExpression().printTrimmed(getCursor()).replaceAll("\\s", "");
            Map<String, TreeSet<String>> methodsAndFieldsByTypeName = new HashMap<>();
            Map<String, Set<JavaType.FullyQualified>> typesByPackage = new HashMap<>();

            for (JavaType.Method method : cu.getTypesInUse().getUsedMethods()) {
                if (method.hasFlags(Flag.Static)) {
                    methodsAndFieldsByTypeName.computeIfAbsent(method.getDeclaringType().getFullyQualifiedName(), t -> new TreeSet<>())
                            .add(method.getName());
                }
            }

            for (JavaType.Variable variable : cu.getTypesInUse().getVariables()) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getOwner());
                if (fq != null) {
                    methodsAndFieldsByTypeName.computeIfAbsent(fq.getFullyQualifiedName(), f -> new TreeSet<>())
                            .add(variable.getName());
                }
            }

            for (JavaType javaType : cu.getTypesInUse().getTypesInUse()) {
                if (javaType instanceof JavaType.Parameterized) {
                    JavaType.Parameterized parameterized = (JavaType.Parameterized) javaType;
                    typesByPackage.computeIfAbsent(parameterized.getType().getPackageName(), f -> new HashSet<>())
                            .add(parameterized.getType());
                    for (JavaType typeParameter : parameterized.getTypeParameters()) {
                        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(typeParameter);
                        if (fq != null) {
                            typesByPackage.computeIfAbsent(fq.getPackageName(), f -> new HashSet<>()).add(fq);
                        }
                    }
                } else if (javaType instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified fq = (JavaType.FullyQualified) javaType;
                    typesByPackage.computeIfAbsent(fq.getPackageName(), f -> new HashSet<>()).add(fq);
                }
            }

            boolean changed = false;

            // the key is a list because a star import may get replaced with multiple unfolded imports
            List<ImportUsage> importUsage = new ArrayList<>(cu.getPadding().getImports().size());
            for (JRightPadded<J.Import> anImport : cu.getPadding().getImports()) {
                // assume initially that all imports are unused
                ImportUsage singleUsage = new ImportUsage();
                singleUsage.imports.add(anImport);
                importUsage.add(singleUsage);
            }

            // whenever an import statement is found to be used and not already in use it should be marked true
            Set<String> checkedImports = new HashSet<>();
            Set<String> usedWildcardImports = new HashSet<>();
            Set<String> usedStaticWildcardImports = new HashSet<>();
            for (ImportUsage anImport : importUsage) {
                J.Import elem = anImport.imports.get(0).getElement();
                J.FieldAccess qualid = elem.getQualid();
                J.Identifier name = qualid.getName();

                if (checkedImports.contains(elem.toString())) {
                    anImport.used = false;
                    changed = true;
                } else if (elem.isStatic()) {
                    String outerType = elem.getTypeName();
                    SortedSet<String> methodsAndFields = methodsAndFieldsByTypeName.get(outerType);

                    // some class names are not handled properly by `getTypeName()`
                    // see https://github.com/openrewrite/rewrite/issues/1698 for more detail
                    String target = qualid.getTarget().toString();
                    String modifiedTarget = methodsAndFieldsByTypeName.keySet().stream()
                            .filter((fqn) -> fullyQualifiedNamesAreEqual(target, fqn))
                            .findFirst()
                            .orElse(target);
                    SortedSet<String> targetMethodsAndFields = methodsAndFieldsByTypeName.get(modifiedTarget);

                    Set<JavaType.FullyQualified> staticClasses = null;
                    for (JavaType.FullyQualified maybeStatic : typesByPackage.getOrDefault(elem.getPackageName(), emptySet())) {
                        if (maybeStatic.getOwningClass() != null && outerType.startsWith(maybeStatic.getOwningClass().getFullyQualifiedName())) {
                            if (staticClasses == null) {
                                staticClasses = new HashSet<>();
                            }
                            staticClasses.add(maybeStatic);
                        }
                    }

                    if (methodsAndFields == null && targetMethodsAndFields == null && staticClasses == null) {
                        anImport.used = false;
                        changed = true;
                    } else if ("*".equals(qualid.getSimpleName())) {
                        if (isPackageAlwaysFolded(layoutStyle.getPackagesToFold(), elem)) {
                            anImport.used = true;
                            usedStaticWildcardImports.add(elem.getTypeName());
                        } else if (((methodsAndFields == null ? 0 : methodsAndFields.size()) +
                                (staticClasses == null ? 0 : staticClasses.size())) < layoutStyle.getNameCountToUseStarImport()) {
                            // replacing the star with a series of unfolded imports
                            anImport.imports.clear();

                            // add each unfolded import
                            if (methodsAndFields != null) {
                                for (String method : methodsAndFields) {
                                    anImport.imports.add(new JRightPadded<>(elem
                                            .withQualid(qualid.withName(name.withSimpleName(method)))
                                            .withPrefix(Space.format("\n")), Space.EMPTY, Markers.EMPTY));
                                }
                            }

                            if (staticClasses != null) {
                                for (JavaType.FullyQualified fqn : staticClasses) {
                                    anImport.imports.add(new JRightPadded<>(elem
                                            .withQualid(qualid.withName(name.withSimpleName(fqn.getClassName().contains(".") ? fqn.getClassName().substring(fqn.getClassName().lastIndexOf(".") + 1) : fqn.getClassName())))
                                            .withPrefix(Space.format("\n")), Space.EMPTY, Markers.EMPTY));
                                }
                            }

                            // move whatever the original prefix of the star import was to the first unfolded import
                            anImport.imports.set(0, anImport.imports.get(0).withElement(anImport.imports.get(0)
                                    .getElement().withPrefix(elem.getPrefix())));

                            changed = true;
                        } else {
                            usedStaticWildcardImports.add(elem.getTypeName());
                        }
                    } else if (staticClasses != null && staticClasses.stream().anyMatch(c -> elem.getTypeName().equals(c.getFullyQualifiedName())) ||
                            (methodsAndFields != null && methodsAndFields.contains(qualid.getSimpleName())) ||
                            (targetMethodsAndFields != null && targetMethodsAndFields.contains(qualid.getSimpleName()))) {
                        anImport.used = true;
                    } else {
                        anImport.used = false;
                        changed = true;
                    }
                } else {
                    Set<JavaType.FullyQualified> types = typesByPackage.getOrDefault(elem.getPackageName(), new HashSet<>());
                    Set<JavaType.FullyQualified> typesByFullyQualifiedClassPath = typesByPackage.getOrDefault(toFullyQualifiedName(elem.getPackageName()), new HashSet<>());
                    Set<JavaType.FullyQualified> combinedTypes = Stream.concat(types.stream(), typesByFullyQualifiedClassPath.stream())
                            .collect(Collectors.toSet());
                    JavaType.FullyQualified qualidType = TypeUtils.asFullyQualified(elem.getQualid().getType());
                    if (combinedTypes.isEmpty() || sourcePackage.equals(elem.getPackageName()) && qualidType != null && !qualidType.getFullyQualifiedName().contains("$")) {
                        anImport.used = false;
                        changed = true;
                    } else if ("*".equals(elem.getQualid().getSimpleName())) {
                        if (isPackageAlwaysFolded(layoutStyle.getPackagesToFold(), elem)) {
                            anImport.used = true;
                            usedWildcardImports.add(elem.getPackageName());
                        } else if (combinedTypes.size() < layoutStyle.getClassCountToUseStarImport()) {
                            // replacing the star with a series of unfolded imports
                            anImport.imports.clear();

                            // add each unfolded import
                            combinedTypes.stream().map(JavaType.FullyQualified::getClassName).sorted().distinct().forEach(type ->
                                    anImport.imports.add(new JRightPadded<>(elem
                                            .withQualid(qualid.withName(name.withSimpleName(type)))
                                            .withPrefix(Space.format("\n")), Space.EMPTY, Markers.EMPTY))
                            );

                            // move whatever the original prefix of the star import was to the first unfolded import
                            anImport.imports.set(0, anImport.imports.get(0).withElement(anImport.imports.get(0)
                                    .getElement().withPrefix(elem.getPrefix())));

                            changed = true;
                        } else {
                            usedWildcardImports.add(elem.getPackageName());
                        }
                    } else if (combinedTypes.stream().noneMatch(c -> {
                        if ("*".equals(elem.getQualid().getSimpleName())) {
                            return elem.getPackageName().equals(c.getPackageName());
                        }
                        return fullyQualifiedNamesAreEqual(c.getFullyQualifiedName(), elem.getTypeName());
                    })) {
                        anImport.used = false;
                        changed = true;
                    }
                }
                checkedImports.add(elem.toString());
            }

            // Do not use direct imports that are imported by a wildcard import
            for (ImportUsage anImport : importUsage) {
                J.Import elem = anImport.imports.get(0).getElement();
                if (!"*".equals(elem.getQualid().getSimpleName())) {
                    if (elem.isStatic()) {
                        if (usedStaticWildcardImports.contains(elem.getTypeName())) {
                            anImport.used = false;
                            changed = true;
                        }
                    } else {
                        if (usedWildcardImports.size() == 1 && usedWildcardImports.contains(elem.getPackageName()) && !elem.getTypeName().contains("$") && !conflictsWithJavaLang(elem)) {
                            anImport.used = false;
                            changed = true;
                        }
                    }
                }
            }

            if (changed) {
                List<JRightPadded<J.Import>> imports = new ArrayList<>();
                Space lastUnusedImportSpace = null;
                for (ImportUsage anImportGroup : importUsage) {
                    if (anImportGroup.used) {
                        List<JRightPadded<J.Import>> importGroup = anImportGroup.imports;
                        for (int i = 0; i < importGroup.size(); i++) {
                            JRightPadded<J.Import> anImport = importGroup.get(i);
                            if (i == 0 && lastUnusedImportSpace != null && anImport.getElement().getPrefix().getLastWhitespace()
                                    .chars().filter(c -> c == '\n').count() <= 1) {
                                anImport = anImport.withElement(anImport.getElement().withPrefix(lastUnusedImportSpace));
                            }
                            imports.add(anImport);
                        }
                        lastUnusedImportSpace = null;
                    } else if (lastUnusedImportSpace == null) {
                        lastUnusedImportSpace = anImportGroup.imports.get(0).getElement().getPrefix();
                    }
                }

                cu = cu.getPadding().withImports(imports);
                if (cu.getImports().isEmpty() && !cu.getClasses().isEmpty()) {
                    cu = autoFormat(cu, cu.getClasses().get(0).getName(), ctx, getCursor().getParentOrThrow());
                }
            }

            return cu;
        }

        private static final Set<String> JAVA_LANG_CLASS_NAMES = new HashSet<>(Arrays.asList(
                "AbstractMethodError",
                "Appendable",
                "ArithmeticException",
                "ArrayIndexOutOfBoundsException",
                "ArrayStoreException",
                "AssertionError",
                "AutoCloseable",
                "Boolean",
                "BootstrapMethodError",
                "Byte",
                "Character",
                "CharSequence",
                "Class",
                "ClassCastException",
                "ClassCircularityError",
                "ClassFormatError",
                "ClassLoader",
                "ClassNotFoundException",
                "ClassValue",
                "Cloneable",
                "CloneNotSupportedException",
                "Comparable",
                "Deprecated",
                "Double",
                "Enum",
                "EnumConstantNotPresentException",
                "Error",
                "Exception",
                "ExceptionInInitializerError",
                "Float",
                "FunctionalInterface",
                "IllegalAccessError",
                "IllegalAccessException",
                "IllegalArgumentException",
                "IllegalCallerException",
                "IllegalMonitorStateException",
                "IllegalStateException",
                "IllegalThreadStateException",
                "IncompatibleClassChangeError",
                "IndexOutOfBoundsException",
                "InheritableThreadLocal",
                "InstantiationError",
                "InstantiationException",
                "Integer",
                "InternalError",
                "InterruptedException",
                "Iterable",
                "LayerInstantiationException",
                "LinkageError",
                "Long",
                "MatchException",
                "Math",
                "Module",
                "ModuleLayer",
                "NegativeArraySizeException",
                "NoClassDefFoundError",
                "NoSuchFieldError",
                "NoSuchFieldException",
                "NoSuchMethodError",
                "NoSuchMethodException",
                "NullPointerException",
                "Number",
                "NumberFormatException",
                "Object",
                "OutOfMemoryError",
                "Override",
                "Package",
                "Process",
                "ProcessBuilder",
                "ProcessHandle",
                "Readable",
                "Record",
                "ReflectiveOperationException",
                "Runnable",
                "Runtime",
                "RuntimeException",
                "RuntimePermission",
                "SafeVarargs",
                "ScopedValue",
                "SecurityException",
                "SecurityManager",
                "Short",
                "StackOverflowError",
                "StackTraceElement",
                "StackWalker",
                "StrictMath",
                "String",
                "StringBuffer",
                "StringBuilder",
                "StringIndexOutOfBoundsException",
                "StringTemplate",
                "SuppressWarnings",
                "System",
                "Thread",
                "ThreadDeath",
                "ThreadGroup",
                "ThreadLocal",
                "Throwable",
                "TypeNotPresentException",
                "UnknownError",
                "UnsatisfiedLinkError",
                "UnsupportedClassVersionError",
                "UnsupportedOperationException",
                "VerifyError",
                "VirtualMachineError",
                "Void",
                "WrongThreadException"
        ));

        private static boolean conflictsWithJavaLang(J.Import elem) {
            return JAVA_LANG_CLASS_NAMES.contains(elem.getClassName());
        }
    }

    private static class ImportUsage {
        final List<JRightPadded<J.Import>> imports = new ArrayList<>();
        boolean used = true;
    }
}
