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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.PathUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SourceSet;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
     * Mapping of a JavaType to the String "group:artifact:version" of the artifact that provides that type.
     * Does not include java standard library types.
     */
    Map<JavaType.FullyQualified, String> typeToGav;

    /**
     * Extract type information from the provided classpath.
     *
     * @param fullTypeInformation Not used, does not do anything, to be deleted
     * @param ignore Not used, does not do anything, to be deleted
     */
    @Deprecated
    public static JavaSourceSet build(String sourceSetName, Collection<Path> classpath,
                                      JavaTypeCache ignore, boolean fullTypeInformation) {
        return build(sourceSetName, classpath);
    }

    public static JavaSourceSet build(String sourceSetName, Collection<Path> classpath) {
        List<JavaType.FullyQualified> types = getJavaStandardLibraryTypes();
        Map<String, List<JavaType.FullyQualified>> gavToTypes = new LinkedHashMap<>();
        Map<JavaType.FullyQualified, String> typeToGav = new LinkedHashMap<>();
        for (Path path : classpath) {
            List<JavaType.FullyQualified> typesFromPath = typesFromPath(path, null);

            types.addAll(typesFromPath);
            String gav = gavFromPath(path);
            if(gav != null) {
                gavToTypes.put(gav, typesFromPath);
                for(JavaType.FullyQualified type : typesFromPath) {
                    typeToGav.put(type, gav);
                }
            }
        }
        return new JavaSourceSet(randomId(), sourceSetName, types, gavToTypes, typeToGav);
    }

    /**
     * Assuming the provided path is to a jar file in a local maven repository or Gradle cache, derive the GAV coordinate from it.
     * If no GAV can be determined returns null.
     *
     */
    @Nullable
    static String gavFromPath(Path path) {
        String pathStr = PathUtils.separatorsToUnix(path.toString());
        List<String> pathParts = Arrays.asList(pathStr.split("/"));
        // Example maven path: ~/.m2/repository/org/openrewrite/rewrite-core/8.32.0/rewrite-core-8.32.0.jar
        // Example gradle path: ~/.gradle/caches/modules-2/files-2.1/org.openrewrite/rewrite-core/8.32.0/64ddcc371f1bf29593b4b27e907757d5554d1a83/rewrite-core-8.32.0.jar

        // Either of these directories may be relocated, so a fixed index is unreliable
        String groupId = null;
        String artifactId = null;
        String version = null;
        if (pathStr.contains("modules-2/files-2.1") && pathParts.size() >= 5) {
            groupId = pathParts.get(pathParts.size() - 5);
            artifactId = pathParts.get(pathParts.size() - 4);
            version = pathParts.get(pathParts.size() - 3);
        } else if (pathParts.size() >= 4) {
            version = pathParts.get(pathParts.size() - 2);
            artifactId = pathParts.get(pathParts.size() - 3);
            // Unknown how many of the next several path parts will together comprise the groupId
            // Maven repository root will have a "repository.xml" file
            StringBuilder groupIdBuilder = new StringBuilder();
            int i = pathParts.size() - 3;
            while(i > 0) {
                Path maybeRepositoryRoot = Paths.get(String.join("/", pathParts.subList(0, i)));
                if(maybeRepositoryRoot.endsWith("repository") || Files.exists(maybeRepositoryRoot.resolve("repository.xml"))) {
                    groupId = groupIdBuilder.substring(1); // Trim off the preceding "."
                    break;
                }
                groupIdBuilder.insert(0, "." + pathParts.get(i - 1));
                i--;
            }
        }
        if(groupId == null || artifactId == null || version == null) {
            return null;
        }
        return groupId + ":" + artifactId + ":" + version;
    }


    // Worth caching as there is typically substantial overlap in dependencies in use within the same repository
    // Even a single module project will typically have at least two source sets, main and test
    private static final Map<Path, List<JavaType.FullyQualified>> PATH_TO_TYPES = new LinkedHashMap<>();
    private static List<JavaType.FullyQualified> typesFromPath(Path path, @Nullable String acceptPackage) {
        return PATH_TO_TYPES.computeIfAbsent(path, unused -> {
            List<JavaType.FullyQualified> types = new ArrayList<>();
            try {
                // Paths will be to either directories of class files or jar files
                if (Files.isRegularFile(path)) {
                    try (JarFile jarFile = new JarFile(path.toFile())) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while(entries.hasMoreElements()) {
                            String entryName = entries.nextElement().getName();
                            if(entryName.endsWith(".class")) {
                                String s = entryNameToClassName(entryName);
                                if(isDeclarable(s)) {
                                    types.add(JavaType.ShallowClass.build(s));
                                }
                            }
                        }
                    }
                } else {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                            String pathStr = file.toString();
                            if (pathStr.endsWith(".class")) {
                                String s = entryNameToClassName(pathStr);
                                if((acceptPackage == null || s.startsWith(acceptPackage)) &&isDeclarable(s)) {
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
        });
    }

    @Nullable
    private static List<JavaType.FullyQualified> javaStandardLibraryTypes;
    private static List<JavaType.FullyQualified> getJavaStandardLibraryTypes() {
        if(javaStandardLibraryTypes == null) {
            javaStandardLibraryTypes = new ArrayList<>();
            Path toolsJar = Paths.get(System.getProperty("java.home")).resolve("../lib/tools.jar");
            if(Files.exists(toolsJar)) {
                javaStandardLibraryTypes.addAll(typesFromPath(toolsJar, "java"));
            } else {
                javaStandardLibraryTypes.addAll(typesFromPath(
                        FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules", "java.base"),
                        "java"));
            }
        }
        return javaStandardLibraryTypes;
    }

    private static String entryNameToClassName(String entryName) {
        String result = entryName;
        if(entryName.startsWith("modules/java.base/")) {
            result = entryName.substring("modules/java.base/".length());
        }
        return result.substring(0, result.length() - ".class".length())
                .replace('/', '.');
    }

    private static boolean isDeclarable(String className) {
        int dotIndex = Math.max(className.lastIndexOf("."), className.lastIndexOf('$'));
        className = className.substring(dotIndex + 1);
        char firstChar = className.charAt(0);
        return Character.isJavaIdentifierPart(firstChar) && !Character.isDigit(firstChar);
    }
}
