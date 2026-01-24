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
package org.openrewrite.kotlin.replace;

import kotlin.Deprecated;
import kotlin.Metadata;
import kotlin.ReplaceWith;
import kotlin.metadata.*;
import kotlin.metadata.jvm.JvmExtensionsKt;
import kotlin.metadata.jvm.JvmMethodSignature;
import kotlin.metadata.jvm.KotlinClassMetadata;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

/**
 * Scans Kotlin class files for {@code @Deprecated(replaceWith=ReplaceWith(...))} annotations
 * and extracts the replacement information.
 * <p>
 * Uses Java reflection to access the {@code kotlin.Deprecated} annotation (which has RUNTIME retention)
 * and kotlin-metadata-jvm to get proper Kotlin function signatures.
 */
public class KotlinDeprecatedMethodScanner {

    /**
     * Result of scanning a JAR for deprecated methods.
     */
    public record ScanResult(
            String groupId,
            String artifactId,
            String version,
            String majorVersion,
            List<DeprecatedMethod> deprecatedMethods
    ) {
    }

    /**
     * A deprecated method with its replacement information.
     */
    public record DeprecatedMethod(
            String methodPattern,
            String replacement,
            List<String> imports,
            String classpathResource
    ) {
    }

    /**
     * Scans the classpath for a JAR matching the given artifact ID and extracts deprecated methods.
     *
     * @param artifactId the artifact ID to search for (e.g., "arrow-core")
     * @return scan result containing GAV info and deprecated methods, or null if JAR not found
     */
    public @Nullable ScanResult scan(String artifactId) throws IOException {
        Path jarPath = findJarOnClasspath(artifactId);
        if (jarPath == null) {
            return null;
        }

        return scanJar(jarPath, artifactId);
    }

    /**
     * Scans a specific JAR file for deprecated methods.
     *
     * @param jarPath    path to the JAR file
     * @param artifactId the artifact ID (used for classpath resource naming)
     * @return scan result containing GAV info and deprecated methods
     */
    public ScanResult scanJar(Path jarPath, String artifactId) throws IOException {
        List<DeprecatedMethod> deprecatedMethods = new ArrayList<>();
        String groupId = null;
        String version = null;

        try (JarFile jarFile = new JarFile(jarPath.toFile());
             URLClassLoader classLoader = new URLClassLoader(
                     new URL[]{jarPath.toUri().toURL()},
                     KotlinDeprecatedMethodScanner.class.getClassLoader())) {

            // Extract version and actual artifact name from JAR path (handles both artifactId and artifactId-jvm naming)
            String jarName = jarPath.getFileName().toString();
            String resolvedArtifactId = artifactId;
            Pattern gavPattern = Pattern.compile("(" + Pattern.quote(artifactId) + "(?:-jvm)?)-(\\d+\\.\\d+[^/]*)\\.jar");
            Matcher matcher = gavPattern.matcher(jarName);
            if (matcher.matches()) {
                resolvedArtifactId = matcher.group(1);
                version = matcher.group(2);
            }

            // Try to extract groupId from pom.properties in JAR
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith("pom.properties")) {
                    Properties props = new Properties();
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        props.load(is);
                        groupId = props.getProperty("groupId");
                        if (version == null) {
                            version = props.getProperty("version");
                        }
                    }
                    break;
                }
            }

            if (groupId == null) {
                groupId = inferGroupIdFromArtifact(artifactId);
            }
            if (version == null) {
                version = "unknown";
            }

            String majorVersion = extractMajorVersion(version);
            String classpathResource = resolvedArtifactId + "-" + majorVersion;

            // Scan all class files in the JAR
            entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class") && !"module-info.class".equals(name)) {
                    String className = name.replace('/', '.').replace(".class", "");
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        List<DeprecatedMethod> methods = extractDeprecatedMethods(clazz, classpathResource);
                        deprecatedMethods.addAll(methods);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // Skip classes that can't be loaded
                    } catch (LinkageError e) {
                        // Skip classes with linkage issues
                    }
                }
            }

            return new ScanResult(groupId, artifactId, version, majorVersion, deprecatedMethods);
        }
    }

    private @Nullable Path findJarOnClasspath(String artifactId) {
        String classpath = System.getProperty("java.class.path");
        if (classpath == null) {
            return null;
        }

        // Try exact match first, then JVM variant
        for (String variant : List.of(artifactId, artifactId + "-jvm")) {
            for (String path : classpath.split(System.getProperty("path.separator"))) {
                // Match artifactId-version.jar pattern
                if (path.contains("/" + variant + "-") && path.endsWith(".jar")) {
                    Path jarPath = Path.of(path);
                    if (Files.exists(jarPath)) {
                        return jarPath;
                    }
                }
            }
        }
        return null;
    }

    private String inferGroupIdFromArtifact(String artifactId) {
        if (artifactId.startsWith("arrow-")) {
            return "io.arrow-kt";
        }
        if (artifactId.startsWith("kotlinx-coroutines")) {
            return "org.jetbrains.kotlinx";
        }
        if (artifactId.startsWith("kotlinx-serialization")) {
            return "org.jetbrains.kotlinx";
        }
        return "unknown";
    }

    private String extractMajorVersion(String version) {
        return version.contains(".") ? version.substring(0, version.indexOf('.')) : version;
    }

    private List<DeprecatedMethod> extractDeprecatedMethods(Class<?> clazz, String classpathResource) {
        List<DeprecatedMethod> methods = new ArrayList<>();

        // Get Kotlin metadata to build proper method patterns
        Metadata metadata = clazz.getAnnotation(Metadata.class);
        Map<String, KmFunction> kmFunctions = new HashMap<>();

        if (metadata != null) {
            KotlinClassMetadata classMetadata = KotlinClassMetadata.readLenient(metadata);
            collectFunctions(classMetadata, kmFunctions);
        }

        // Scan methods using reflection
        for (Method method : clazz.getDeclaredMethods()) {
            Deprecated deprecated = method.getAnnotation(Deprecated.class);
            if (deprecated != null) {
                ReplaceWith replaceWith = deprecated.replaceWith();
                String expression = replaceWith.expression();
                if (expression != null && !expression.isEmpty()) {
                    // Use the original declaring type (interface) since the Kotlin parser
                    // resolves method calls to the interface declaring the method
                    String declaringType = findOriginalDeclaringType(clazz, method);
                    String methodPattern = buildMethodPattern(declaringType, method, kmFunctions);
                    List<String> imports = Arrays.asList(replaceWith.imports());
                    methods.add(new DeprecatedMethod(methodPattern, expression, imports, classpathResource));
                }
            }
        }

        return methods;
    }

    /**
     * Finds the original declaring type for a method by walking up the interface hierarchy.
     * The Kotlin parser resolves method calls to the interface that originally declares the method,
     * so the scanner must do the same to generate matching method patterns.
     */
    private String findOriginalDeclaringType(Class<?> clazz, Method method) {
        String name = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();

        // Walk up interfaces to find where this method is originally declared
        for (Class<?> iface : getAllInterfaces(clazz)) {
            try {
                iface.getDeclaredMethod(name, paramTypes);
                return iface.getName();
            } catch (NoSuchMethodException e) {
                // Not declared in this interface
            }
        }

        // Check superclass hierarchy
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            try {
                superClass.getDeclaredMethod(name, paramTypes);
                return superClass.getName();
            } catch (NoSuchMethodException e) {
                superClass = superClass.getSuperclass();
            }
        }

        return clazz.getName();
    }

    private Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        collectInterfaces(clazz, interfaces);
        return interfaces;
    }

    private void collectInterfaces(Class<?> clazz, Set<Class<?>> interfaces) {
        for (Class<?> iface : clazz.getInterfaces()) {
            if (interfaces.add(iface)) {
                collectInterfaces(iface, interfaces);
            }
        }
        if (clazz.getSuperclass() != null) {
            collectInterfaces(clazz.getSuperclass(), interfaces);
        }
    }

    private void collectFunctions(KotlinClassMetadata classMetadata, Map<String, KmFunction> kmFunctions) {
        if (classMetadata instanceof KotlinClassMetadata.Class classData) {
            KmClass kmClass = classData.getKmClass();
            for (KmFunction func : kmClass.getFunctions()) {
                addFunctionSignature(func, kmFunctions);
            }
        } else if (classMetadata instanceof KotlinClassMetadata.FileFacade fileFacade) {
            KmPackage kmPackage = fileFacade.getKmPackage();
            for (KmFunction func : kmPackage.getFunctions()) {
                addFunctionSignature(func, kmFunctions);
            }
        } else if (classMetadata instanceof KotlinClassMetadata.MultiFileClassPart multiFilePart) {
            KmPackage kmPackage = multiFilePart.getKmPackage();
            for (KmFunction func : kmPackage.getFunctions()) {
                addFunctionSignature(func, kmFunctions);
            }
        }
    }

    private void addFunctionSignature(KmFunction func, Map<String, KmFunction> kmFunctions) {
        JvmMethodSignature sig = JvmExtensionsKt.getSignature(func);
        if (sig != null) {
            kmFunctions.put(sig.getName() + sig.getDescriptor(), func);
        }
    }

    private String buildMethodPattern(String className, Method method, Map<String, KmFunction> kmFunctions) {
        // Build method signature key for lookup
        String sigKey = method.getName() + getMethodDescriptor(method);
        KmFunction kmFunc = kmFunctions.get(sigKey);

        if (kmFunc != null) {
            return buildMethodPatternFromKotlin(className, kmFunc);
        }
        return buildMethodPatternFromReflection(className, method);
    }

    private String buildMethodPatternFromKotlin(String className, KmFunction function) {
        StringBuilder pattern = new StringBuilder();
        pattern.append(className).append(" ");
        pattern.append(function.getName());
        pattern.append("(");

        List<KmValueParameter> params = function.getValueParameters();
        if (!params.isEmpty()) {
            pattern.append(params.stream()
                    .map(p -> typeToPattern(p.getType()))
                    .collect(joining(", ")));
        }
        pattern.append(")");

        return pattern.toString();
    }

    private String buildMethodPatternFromReflection(String className, Method method) {
        StringBuilder pattern = new StringBuilder();
        pattern.append(className).append(" ");
        pattern.append(method.getName());
        pattern.append("(");

        Class<?>[] params = method.getParameterTypes();
        if (params.length > 0) {
            pattern.append(Arrays.stream(params)
                    .map(this::classToPattern)
                    .collect(joining(", ")));
        }
        pattern.append(")");

        return pattern.toString();
    }

    private String getMethodDescriptor(Method method) {
        StringBuilder desc = new StringBuilder("(");
        for (Class<?> param : method.getParameterTypes()) {
            desc.append(getTypeDescriptor(param));
        }
        desc.append(")");
        desc.append(getTypeDescriptor(method.getReturnType()));
        return desc.toString();
    }

    private String getTypeDescriptor(Class<?> type) {
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        if (type.isArray()) {
            return "[" + getTypeDescriptor(type.getComponentType());
        }
        return "L" + type.getName().replace('.', '/') + ";";
    }

    private String classToPattern(Class<?> type) {
        if (type.isArray()) {
            return classToPattern(type.getComponentType()) + "[]";
        }
        String name = type.getName();
        // Map JVM function types back to Kotlin types (the Kotlin parser uses kotlin.FunctionN)
        if (name.startsWith("kotlin.jvm.functions.Function")) {
            return "kotlin." + name.substring("kotlin.jvm.functions.".length());
        }
        return name;
    }

    private String typeToPattern(KmType type) {
        if (type == null) {
            return "..";
        }

        KmClassifier classifier = type.getClassifier();
        if (classifier instanceof KmClassifier.Class classClassifier) {
            String name = classClassifier.getName().replace('/', '.');
            return mapKotlinType(name);
        }
        return "..";
    }

    private String mapKotlinType(String kotlinType) {
        return switch (kotlinType) {
            case "kotlin.Int" -> "int";
            case "kotlin.Long" -> "long";
            case "kotlin.Short" -> "short";
            case "kotlin.Byte" -> "byte";
            case "kotlin.Float" -> "float";
            case "kotlin.Double" -> "double";
            case "kotlin.Boolean" -> "boolean";
            case "kotlin.Char" -> "char";
            case "kotlin.String" -> "java.lang.String";
            case "kotlin.Unit" -> "void";
            case "kotlin.Any" -> "java.lang.Object";
            case "kotlin.IntArray" -> "int[]";
            case "kotlin.LongArray" -> "long[]";
            case "kotlin.ShortArray" -> "short[]";
            case "kotlin.ByteArray" -> "byte[]";
            case "kotlin.FloatArray" -> "float[]";
            case "kotlin.DoubleArray" -> "double[]";
            case "kotlin.BooleanArray" -> "boolean[]";
            case "kotlin.CharArray" -> "char[]";
            default -> kotlinType;
        };
    }
}
