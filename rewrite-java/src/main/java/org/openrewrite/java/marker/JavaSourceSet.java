/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.marker;

import io.github.classgraph.*;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Marker;

import java.nio.file.Path;
import java.util.*;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class JavaSourceSet implements Marker {
    @EqualsAndHashCode.Include
    UUID id;

    String name;

    List<JavaType.FullyQualified> classpath;

    /**
     * Extract type information from the provided classpath.
     *
     * @param fullTypeInformation when false classpath will be filled with shallow types (effectively just fully-qualified names).
     *                            when true a much more memory-intensive, time-consuming approach will extract full type information
     */
    public static JavaSourceSet build(String sourceSetName, Collection<Path> classpath,
                                      JavaTypeCache typeCache, boolean fullTypeInformation) {
        List<JavaType.FullyQualified> types;
        // Load JRE-provided types
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableSystemJarsAndModules()
                .acceptPackages("java")
                .ignoreClassVisibility()
                .scan()) {
            Map<String, List<String>> packagesToTypes = packagesToTypeDeclarations(scanResult);
            // Peculiarly, Classgraph will not return a ClassInfo for java.lang.Object, although it does for all other java.lang types
            packagesToTypes.computeIfAbsent("java.lang", p -> new ArrayList<>())
                    .add("java.lang.Object");
            types = typesFrom(packagesToTypes, typeCache, Collections.emptyList(), fullTypeInformation);
        }
        if (classpath.iterator().hasNext()) {
            // Load types from the classpath
            try (ScanResult scanResult = new ClassGraph()
                    .overrideClasspath(classpath)
                    .enableMemoryMapping()
                    .enableClassInfo()
                    .ignoreClassVisibility()
                    .scan()) {
                types.addAll(typesFrom(packagesToTypeDeclarations(scanResult), typeCache, classpath, fullTypeInformation));
            }
        }

        return new JavaSourceSet(randomId(), sourceSetName, types);
    }

    /*
     * Create a map of package names to types contained within that package. Type names are not fully qualified, except for type parameter bounds.
     * e.g.: "java.util" -> [List, Date]
     */
    private static Map<String, List<String>> packagesToTypeDeclarations(ScanResult scanResult) {
        Map<String, List<String>> result = new HashMap<>();
        for (ClassInfo classInfo : scanResult.getAllClasses()) {
            // Skip private classes, allowing package-private
            if (classInfo.isAnonymousInnerClass() || classInfo.isPrivate() || classInfo.isSynthetic() || classInfo.getName().contains(".enum.")) {
                continue;
            }
            // Although the classfile says its bytecode version is 50 (within the range Java 8 supports),
            // the Java 8 compiler says these class files from kotlin-reflect are invalid
            // The error is severe enough that all subsequent stubs have missing type information, so exclude that package
            if (classInfo.getPackageName().startsWith("kotlin.reflect.jvm.internal.impl.resolve.jvm")) {
                continue;
            }
            String typeDeclaration = declarableFullyQualifiedName(classInfo);
            if (typeDeclaration == null) {
                continue;
            }
            result.computeIfAbsent(classInfo.getPackageName(), p -> new ArrayList<>())
                    .add(typeDeclaration);
        }
        return result;
    }

    private static List<JavaType.FullyQualified> typesFrom(
            Map<String, List<String>> packagesToTypes,
            JavaTypeCache typeCache,
            Collection<Path> classpath,
            boolean fullTypeInformation
    ) {
        List<JavaType.FullyQualified> types = new ArrayList<>();
        if (fullTypeInformation) {

            @Language("java")
            String[] typeStubs = typeStubsFor(packagesToTypes);

            ExecutionContext noRecursiveJavaSourceSet = new InMemoryExecutionContext();
            noRecursiveJavaSourceSet.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);

            JavaParser jp = JavaParser.fromJavaVersion()
                    .typeCache(typeCache)
                    .classpath(classpath)
                    .build();

            List<J.CompilationUnit> cus = jp.parse(noRecursiveJavaSourceSet, typeStubs);
            for (J.CompilationUnit cu : cus) {
                if (!cu.getClasses().isEmpty()) {
                    J.Block body = cu.getClasses().get(0).getBody();
                    for (Statement s : body.getStatements()) {
                        JavaType type = ((J.MethodDeclaration) s).getType();
                        if (type instanceof JavaType.FullyQualified) {
                            types.add((JavaType.FullyQualified) type);
                        }
                    }
                }
            }
        } else {
            for (Map.Entry<String, List<String>> packageToTypes : packagesToTypes.entrySet()) {
                for (String className : packageToTypes.getValue()) {
                    types.add(JavaType.ShallowClass.build(className));
                }
            }
        }
        return types;
    }

    /**
     * Produce Java source code that, when compiled, contains all the types represented in packagesToTypeNames.
     */
    private static String[] typeStubsFor(Map<String, List<String>> packagesToTypeNames) {
        String[] result = new String[packagesToTypeNames.size()];
        int i = 0;
        for (Map.Entry<String, List<String>> packageToTypes : packagesToTypeNames.entrySet()) {
            boolean isJreType = packageToTypes.getKey().startsWith("java.");
            StringBuilder sb = new StringBuilder("package ");
            if(isJreType) {
                // Avoid java compiler complaints that we're redefining java.lang, etc.
                // Only apply to java provided types because this limits our ability to get type information from package-private classes
                sb.append("rewrite.");
            }
            sb.append(packageToTypes.getKey()).append(";\n")
                    .append("abstract class $RewriteTypeStub {\n");

            List<String> value = packageToTypes.getValue();
            for (int j = 0; j < value.size(); j++) {
                String type = value.get(j);
                if (type == null) {
                    continue;
                }
                sb.append("    abstract ").append(type).append(" t").append(j).append("();\n");
            }
            sb.append("}");
            result[i] = sb.toString();
            i++;
        }
        return result;
    }

    /**
     * A declaration is the text you would use to declare the type parameters and return type a method.
     * So the declarable name of "com.foo.Clazz" is "Clazz" since to declare a variable of that type would write "Clazz <name>"
     * Java/Kotlin/Groovy all sometimes compile lambdas/closures to classes named things like "ClassThatUsesLambda$1".
     * These types are not declarable.
     *
     * @return the name a variable of the class's type can be declared with, or null if a variable of the type cannot be declared
     */
    @Nullable
    private static String typeDeclarationFor(ClassInfo classInfo) {
        String name = declarableFullyQualifiedName(classInfo);
        if(name == null) {
            return null;
        }
        ClassTypeSignature cts = classInfo.getTypeSignature();
        if (cts == null) {
            return name;
        }
        List<TypeParameter> typeParameters = cts.getTypeParameters();
        if (typeParameters == null || typeParameters.isEmpty()) {
            return name;
        }
        StringBuilder withTypeParams = new StringBuilder("<");
        for (int i = 0; i < typeParameters.size(); i++) {
            TypeParameter typeParameter = typeParameters.get(i);
            StringBuilder bounds = new StringBuilder();
            if (typeParameter.getClassBound() != null) {
                String bound;
                if(typeParameter.getClassBound() instanceof ClassRefTypeSignature) {
                    bound = printBound((ClassRefTypeSignature)typeParameter.getClassBound());
                } else {
                    bound = typeParameter.getClassBound().toString()
                            // e.g. for ClassSpecializer<T,K,S extends ClassSpecializer<T,K,S>.SpeciesData>
                            .replace(">$", ">.");
                }
                if (!"java.lang.Object".equals(bound)) {
                    bounds.append(bound);
                }
            } else if (typeParameter.getInterfaceBounds() != null) {
                StringJoiner interfaceBounds = new StringJoiner(" & ");
                for (ReferenceTypeSignature interfaceBound : typeParameter.getInterfaceBounds()) {
                    if(interfaceBound instanceof ClassRefTypeSignature) {
                        interfaceBounds.add(printBound((ClassRefTypeSignature)interfaceBound));
                    } else {
                        interfaceBounds.add(interfaceBound.toString());
                    }
                }
                bounds.append(interfaceBounds);
            }

            if (bounds.length() == 0) {
                withTypeParams.append(typeParameter.getName());
            } else {
                withTypeParams.append(typeParameter.getName()).append(" extends ").append(bounds);
            }
            if (i < typeParameters.size() - 1) {
                withTypeParams.append(", ");
            }
        }
        withTypeParams.append(">");
        withTypeParams.append(" ");
        withTypeParams.append(name);
        withTypeParams.append(typeParametersNoBounds(classInfo));
        return withTypeParams.toString();
    }

    private static String typeParametersNoBounds(ClassInfo classInfo) {
        ClassTypeSignature cts = classInfo.getTypeSignature();
        if (cts == null) {
            return "";
        }
        List<TypeParameter> typeParameters = cts.getTypeParameters();
        if (typeParameters == null || typeParameters.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        for (int i = 0; i < typeParameters.size(); i++) {
            TypeParameter typeParameter = typeParameters.get(i);
            sb.append(typeParameter.getName());
            if (i < typeParameters.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(">");
        return sb.toString();
    }
    private static String printBound(ClassRefTypeSignature refTypeSignature) {
        StringBuilder sb = new StringBuilder(refTypeSignature.getBaseClassName());
        List<TypeArgument> typeArguments = refTypeSignature.getTypeArguments();
        if(typeArguments != null && typeArguments.size() > 0) {
            sb.append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                TypeArgument typeParameter = typeArguments.get(i);
                sb.append(typeParameter.toString());
                if (i < typeArguments.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        }
        List<String> suffixes = refTypeSignature.getSuffixes();
        if(suffixes != null && suffixes.size() > 0) {
            for(int i = 0; i < suffixes.size(); i++) {
                String suffix = suffixes.get(0);
                List<TypeArgument> suffixTypeArgs = refTypeSignature.getSuffixTypeArguments().get(i);
                sb.append(".").append(suffix).append("<");
                for (int j = 0; j < suffixTypeArgs.size(); j++) {
                    sb.append(suffixTypeArgs.get(j));
                    if(j < suffixTypeArgs.size() - 1) {
                        sb.append(",");
                    }
                }
                sb.append(">");
            }
        }
        return sb.toString();
    }

    /**
     * Java allows "$" in class names, and also uses "$" as part of the names of inner classes. e.g.: OuterClass$InnerClass
     * So if you only look at the textual representation of a class name, you can't tell if "A$B" means "class A$B {}" or "class A { class B {}}"
     * The declarable name of "class A$B {}" is "A$B"
     * The declarable name of class B in "class A { class B {}}" is "A.B"
     * ClassInfo.getPackageName() does not understand this and will always replace "$" in names with "."
     *
     * This method takes all of these considerations into account and returns a fully qualified name which replaces
     * inner-class signifying "$" with ".", while preserving
     */
    @Nullable
    private static String declarableFullyQualifiedName(ClassInfo classInfo) {
        String name;
        if(classInfo.getName().startsWith("java.") && !classInfo.isPublic()) {
            // Because we put java-supplied types into another package, we cannot access package-private types
            return null;
        }
        if (classInfo.isInnerClass()) {
            StringBuilder sb = new StringBuilder();
            ClassInfoList outerClasses = classInfo.getOuterClasses();
            // Classgraph orders this collection innermost -> outermost, but type names are declared outermost -> innermost
            for (int i = outerClasses.size() - 1; i >= 0; i--) {
                ClassInfo outerClass = outerClasses.get(i);
                if (outerClass.isPrivate() || outerClass.isAnonymousInnerClass() || outerClass.isSynthetic()) {
                    return null;
                }
                if(i == outerClasses.size() - 1) {
                    sb.append(outerClass.getName()).append(".");
                } else {
                    sb.append(outerClass.getName().substring(sb.length())).append(".");
                }
            }
            String nameFragment = classInfo.getName().substring(sb.length());

            if (isUndeclarable(nameFragment)) {
                return null;
            }
            sb.append(nameFragment);
            name = sb.toString();
        } else {
            name = classInfo.getName();
        }
        if (isUndeclarable(name)) {
            return null;
        }
        return name;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static boolean isUndeclarable(String className) {
        char firstChar = className.charAt(0);
        return !Character.isJavaIdentifierPart(firstChar) || Character.isDigit(firstChar);
    }
}
