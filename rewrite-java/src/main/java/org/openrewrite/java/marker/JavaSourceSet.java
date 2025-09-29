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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.PathUtils;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SourceSet;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class JavaSourceSet implements SourceSet {
    @EqualsAndHashCode.Include
    UUID id;

    String name;

    List<JavaType.FullyQualified> classpath;

    /**
     * Mapping of a String taking the form "group:artifact:version" to the types provided by that artifact.
     * Does not include java standard library types.
     */
    Map<String, List<JavaType.FullyQualified>> gavToTypes;

    /**
     * Extract type information from the provided classpath.
     * Uses ClassGraph to compute the classpath.
     * <p>
     * Does not support gavToTypes or typeToGav mapping
     *
     * @param fullTypeInformation Not used, does not do anything, to be deleted
     * @param ignore              Not used, does not do anything, to be deleted
     */
    @Deprecated
    public static JavaSourceSet build(String sourceSetName, Collection<Path> classpath,
                                      JavaTypeCache ignore, boolean fullTypeInformation) {
        if (fullTypeInformation) {
            throw new UnsupportedOperationException();
        }

        List<String> typeNames;
        if (!classpath.iterator().hasNext()) {
            // Only load JRE-provided types
            try (ScanResult scanResult = new ClassGraph()
                    .enableClassInfo()
                    .enableSystemJarsAndModules()
                    .acceptPackages("java")
                    .ignoreClassVisibility()
                    .scan()) {
                typeNames = packagesToTypeDeclarations(scanResult);
            }
        } else {
            // Load types from the classpath
            try (ScanResult scanResult = new ClassGraph()
                    .overrideClasspath(classpath)
                    .enableSystemJarsAndModules()
                    .enableClassInfo()
                    .ignoreClassVisibility()
                    .scan()) {
                typeNames = packagesToTypeDeclarations(scanResult);
            }
        }

        // Peculiarly, Classgraph will not return a ClassInfo for java.lang.Object, although it does for all other java.lang types
        typeNames.add("java.lang.Object");
        return new JavaSourceSet(randomId(), sourceSetName, typesFrom(typeNames), emptyMap());
    }


    /*
     * Create a map of package names to types contained within that package. Type names are not fully qualified, except for type parameter bounds.
     * e.g.: "java.util" -> [List, Date]
     */
    private static List<String> packagesToTypeDeclarations(ScanResult scanResult) {
        List<String> result = new ArrayList<>();
        for (ClassInfo classInfo : scanResult.getAllClasses()) {
            // Skip private classes, allowing package-private
            if (classInfo.isAnonymousInnerClass() || classInfo.isPrivate() || classInfo.isSynthetic() || classInfo.getName().contains(".enum.")) {
                continue;
            }
            if (classInfo.isStandardClass() && !classInfo.getName().startsWith("java.")) {
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
            result.add(typeDeclaration);
        }
        return result;
    }

    private static List<JavaType.FullyQualified> typesFrom(List<String> typeNames) {
        List<JavaType.FullyQualified> types = new ArrayList<>(typeNames.size());
        for (String typeName : typeNames) {
            types.add(JavaType.ShallowClass.build(typeName));
        }
        return types;
    }

    /**
     * Java allows "$" in class names, and also uses "$" as part of the names of inner classes. e.g.: OuterClass$InnerClass
     * So if you only look at the textual representation of a class name, you can't tell if "A$B" means "class A$B {}" or "class A { class B {}}"
     * The declarable name of "class A$B {}" is "A$B"
     * The declarable name of class B in "class A { class B {}}" is "A.B"
     * ClassInfo.getPackageName() does not understand this and will always replace "$" in names with "."
     * <p>
     * This method takes all of these considerations into account and returns a fully qualified name which replaces
     * inner-class signifying "$" with ".", while preserving
     */
    private static @Nullable String declarableFullyQualifiedName(ClassInfo classInfo) {
        String name;
        if (classInfo.getName().startsWith("java.") && !classInfo.isPublic()) {
            // Because we put java-supplied types into another package, we cannot access package-private types
            return null;
        }
        if (classInfo.isInnerClass()) {
            StringBuilder sb = new StringBuilder();
            ClassInfoList outerClasses = classInfo.getOuterClasses();
            // Classgraph orders this collection innermost -> outermost, but type names are declared outermost -> innermost
            for (int i = outerClasses.size() - 1; i >= 0; i--) {
                ClassInfo outerClass = outerClasses.get(i);
                if (outerClass.isPrivate() || outerClass.isAnonymousInnerClass() || outerClass.isSynthetic() || outerClass.isExternalClass()) {
                    return null;
                }
                if (i == outerClasses.size() - 1) {
                    sb.append(outerClass.getName()).append(".");
                } else if (!outerClass.getName().startsWith(sb.toString())) {
                    // Code obfuscators can generate inner classes which don't share a common package prefix with their outer class
                    return classInfo.getName();
                } else {
                    sb.append(outerClass.getName().substring(sb.length())).append(".");
                }
            }
            if (!classInfo.getName().startsWith(sb.toString())) {
                // Code obfuscators can generate inner classes which don't share a common package prefix with their outer class
                return classInfo.getName();
            }
            String nameFragment = classInfo.getName().substring(sb.length());

            if (!isDeclarable(nameFragment)) {
                return null;
            }
            sb.append(nameFragment);
            name = sb.toString();
        } else {
            name = classInfo.getName();
        }
        if (!isDeclarable(name)) {
            return null;
        }
        return name;
    }


    // Purely IO-based classpath scanning below this point

    /**
     * Extract type information from the provided classpath.
     * Uses file I/O to compute the classpath.
     */
    public static JavaSourceSet build(String sourceSetName, Collection<Path> classpath) {
        List<JavaType.FullyQualified> types = getJavaStandardLibraryTypes();
        Map<String, List<JavaType.FullyQualified>> gavToTypes = new LinkedHashMap<>();
        for (Path path : classpath) {
            List<JavaType.FullyQualified> typesFromPath = typesFromPath(path, null);

            types.addAll(typesFromPath);
            String gav = gavFromPath(path);
            if (gav != null) {
                gavToTypes.put(gav, typesFromPath);
            }
        }
        return new JavaSourceSet(randomId(), sourceSetName, types, gavToTypes);
    }

    /**
     * Assuming the provided path is to a jar file in a local maven repository or Gradle cache, derive the GAV coordinate from it.
     * If no GAV can be determined returns null.
     */
    static @Nullable String gavFromPath(Path path) {
        String pathStr = PathUtils.separatorsToUnix(path.toString());
        List<String> pathParts = Arrays.asList(pathStr.split("/"));
        // Example maven path: ~/.m2/repository/org/openrewrite/rewrite-core/8.32.0/rewrite-core-8.32.0.jar
        // Example gradle path: ~/.gradle/caches/modules-2/files-2.1/org.openrewrite/rewrite-core/8.32.0/64ddcc371f1bf29593b4b27e907757d5554d1a83/rewrite-core-8.32.0.jar
        // Example typetable path: ~/.rewrite/classpath/.tt/org/junit/jupiter/junit-jupiter-api/6.0.0-RC3

        // Either of these directories may be relocated, so a fixed index is unreliable
        String groupId = null;
        String artifactId = null;
        String version = null;
        try {
            if (pathStr.contains("modules-2/files-2.1") && pathParts.size() >= 5) {
                groupId = pathParts.get(pathParts.size() - 5);
                artifactId = pathParts.get(pathParts.size() - 4);
                version = pathParts.get(pathParts.size() - 3);
            } else if (pathParts.contains(".tt")) {
                int ttIndex = pathParts.indexOf(".tt");
                if (pathParts.size() - ttIndex > 3) {
                    groupId = String.join(".", pathParts.subList(ttIndex + 1, pathParts.size() - 2));
                    artifactId = pathParts.get(pathParts.size() - 2);
                    version = pathParts.get(pathParts.size() - 1);
                }
            } else if (pathParts.size() >= 4) {
                version = pathParts.get(pathParts.size() - 2);
                artifactId = pathParts.get(pathParts.size() - 3);
                // Unknown how many of the next several path parts will together comprise the groupId
                // Maven repository root will have a "repository.xml" file
                StringBuilder groupIdBuilder = new StringBuilder();
                int i = pathParts.size() - 3;
                while (i > 0) {
                    Path maybeRepositoryRoot = Paths.get(String.join("/", pathParts.subList(0, i)));
                    if (maybeRepositoryRoot.endsWith("repository") || Files.exists(maybeRepositoryRoot.resolve("repository.xml"))) {
                        groupId = groupIdBuilder.substring(1); // Trim off the preceding "."
                        break;
                    }
                    groupIdBuilder.insert(0, "." + pathParts.get(i - 1));
                    i--;
                }
            }
        } catch (Exception e) {
            return null;
        }
        if (groupId == null || artifactId == null || version == null) {
            return null;
        }
        return groupId + ":" + artifactId + ":" + version;
    }


    // Worth caching as there is typically substantial overlap in dependencies in use within the same repository
    // Even a single module project will typically have at least two source sets, main and test
    private static List<JavaType.FullyQualified> typesFromPath(Path path, @Nullable String acceptPackage) {
        List<JavaType.FullyQualified> types = new ArrayList<>();
        try {
            // Paths will be to either directories of class files or jar files
            if (Files.isRegularFile(path)) {
                try (JarFile jarFile = new JarFile(path.toFile())) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        String entryName = entries.nextElement().getName();
                        if (entryName.endsWith(".class")) {
                            String s = entryNameToClassName(entryName);
                            if (isDeclarable(s)) {
                                types.add(JavaType.ShallowClass.build(s));
                            }
                        }
                    }
                }
            } else {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                        if (file.getFileName().toString().endsWith(".class")) {
                            String pathStr = file.isAbsolute() ? path.relativize(file).toString() : file.toString();
                            String s = entryNameToClassName(pathStr);
                            if ((acceptPackage == null || s.startsWith(acceptPackage)) && isDeclarable(s)) {
                                types.add(JavaType.ShallowClass.build(s));
                            }
                        }
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            // Partial results better than no results
        }
        return types;
    }

    private static List<JavaType.FullyQualified> getJavaStandardLibraryTypes() {
        List<JavaType.FullyQualified> javaStandardLibraryTypes = new ArrayList<>();
        Path toolsJar = Paths.get(System.getProperty("java.home")).resolve("../lib/tools.jar");
        if (Files.exists(toolsJar)) {
            javaStandardLibraryTypes.addAll(typesFromPath(toolsJar, "java"));
        } else {
            javaStandardLibraryTypes.addAll(typesFromPath(
                    FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules", "java.base"),
                    "java"));
        }
        return javaStandardLibraryTypes;
    }

    private static String entryNameToClassName(String entryName) {
        int start = entryName.startsWith("modules/java.base/") ? "modules/java.base/".length() : 0;
        return entryName.substring(start, entryName.length() - ".class".length())
                .replace('/', '.');
    }

    static boolean isDeclarable(String className) {
        int dotIndex = Math.max(className.lastIndexOf("."), className.lastIndexOf('$'));
        return dotIndex != -1 && dotIndex < className.length() - 1 && Character.isJavaIdentifierStart(className.charAt(dotIndex + 1));
    }
}
