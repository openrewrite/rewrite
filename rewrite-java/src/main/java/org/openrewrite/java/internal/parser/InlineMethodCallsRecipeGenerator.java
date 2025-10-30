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
package org.openrewrite.java.internal.parser;

import lombok.AllArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;

public class InlineMethodCallsRecipeGenerator {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: InlineMethodCallsRecipeGenerator <input-tsv-path> <output-yaml-path> <recipe-name>");
            System.exit(1);
        }

        Path inputPath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);
        String recipeName = args[2];

        generate(inputPath, outputPath, recipeName);
    }

    static void generate(Path tsvFile, Path outputPath, String recipeName) {
        List<InlineMeMethod> inlineMethods = new ArrayList<>();

        TypeTable.Reader reader = new TypeTable.Reader(new InMemoryExecutionContext());
        try (InputStream is = Files.newInputStream(tsvFile); InputStream inflate = new GZIPInputStream(is)) {
            reader.parseTsvAndProcess(inflate, TypeTable.Reader.Options.matchAll(), (gav, classes, nestedTypes) -> {
                if (gav == null) {
                    return;
                }

                // Process each class in this GAV
                for (TypeTable.ClassDefinition classDef : classes.values()) {
                    // Process each member (method/constructor) in the class
                    for (TypeTable.Member member : classDef.getMembers()) {
                        // Check if member has @InlineMe annotation
                        String annotations = member.getAnnotations();
                        if (annotations != null && annotations.contains("InlineMe")) {
                            InlineMeMethod inlineMethod = extractInlineMeMethod(gav, classDef, member);
                            if (inlineMethod != null) {
                                inlineMethods.add(inlineMethod);
                            }
                        }
                    }
                }
            });

            // Generate YAML recipes
            generateYamlRecipes(inlineMethods, outputPath, recipeName);

            System.out.println("Generated " + inlineMethods.size() + " inline recipes to " + outputPath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static @Nullable InlineMeMethod extractInlineMeMethod(TypeTable.GroupArtifactVersion gav,
                                                                  TypeTable.ClassDefinition classDef,
                                                                  TypeTable.Member member) {
        try {
            // Parse the annotations to find @InlineMe
            List<AnnotationDeserializer.AnnotationInfo> annotations =
                    AnnotationDeserializer.parseAnnotations(requireNonNull(member.getAnnotations()));
            for (AnnotationDeserializer.AnnotationInfo annotation : annotations) {
                if (!annotation.getDescriptor().endsWith("/InlineMe;")) {
                    continue;
                }

                List<AnnotationDeserializer.AttributeInfo> attributes = annotation.getAttributes();
                if (attributes == null) {
                    continue;
                }

                // Extract annotation values
                String replacement = null;
                List<String> imports = new ArrayList<>();
                List<String> staticImports = new ArrayList<>();

                for (AnnotationDeserializer.AttributeInfo attr : attributes) {
                    switch (attr.getName()) {
                        case "replacement":
                            replacement = (String) attr.getValue();
                            break;
                        case "imports":
                            if (attr.getValue() instanceof Object[]) {
                                for (Object imp : (Object[]) attr.getValue()) {
                                    imports.add((String) imp);
                                }
                            }
                            break;
                        case "staticImports":
                            if (attr.getValue() instanceof Object[]) {
                                for (Object imp : (Object[]) attr.getValue()) {
                                    staticImports.add((String) imp);
                                }
                            }
                            break;
                    }
                }

                if (replacement != null) {
                    // Build the method pattern
                    String methodPattern = buildMethodPattern(classDef, member);

                    return new InlineMeMethod(
                            gav,
                            methodPattern,
                            replacement,
                            imports,
                            staticImports,
                            gav.getArtifactId() + "-" + gav.getVersion()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse annotations for " + classDef.getName() + "." + member.getName() + ": " + e.getMessage());
        }

        return null;
    }

    private static String buildMethodPattern(TypeTable.ClassDefinition classDef, TypeTable.Member member) {
        String className = classDef.getName().replace('/', '.');
        String methodName = member.getName();

        // For constructors, use the class name
        if ("<init>".equals(methodName)) {
            methodName = className.substring(className.lastIndexOf('.') + 1);
        }

        // Parse method descriptor to extract parameter types
        String descriptor = member.getDescriptor();
        String paramPattern = parseMethodParameters(descriptor);

        return className + " " + methodName + paramPattern;
    }

    private static String parseMethodParameters(String descriptor) {
        if (!descriptor.startsWith("(")) {
            return "()";
        }

        List<String> paramTypes = new ArrayList<>();
        int i = 1; // Skip opening '('
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            String type = parseType(descriptor, i);
            paramTypes.add(type);
            i += getTypeLength(descriptor, i);
        }

        if (paramTypes.isEmpty()) {
            return "()";
        }
        return "(" + String.join(", ", paramTypes) + ")";
    }

    private static String parseType(String descriptor, int start) {
        char c = descriptor.charAt(start);
        switch (c) {
            case 'B':
                return "byte";
            case 'C':
                return "char";
            case 'D':
                return "double";
            case 'F':
                return "float";
            case 'I':
                return "int";
            case 'J':
                return "long";
            case 'S':
                return "short";
            case 'Z':
                return "boolean";
            case 'V':
                return "void";
            case 'L':
                // Object type - extract class name
                int semicolon = descriptor.indexOf(';', start);
                String className = descriptor.substring(start + 1, semicolon);
                return className.replace('/', '.');
            case '[':
                // Array type
                String elementType = parseType(descriptor, start + 1);
                return elementType + "[]";
            default:
                return "Object"; // Fallback
        }
    }

    private static int getTypeLength(String descriptor, int start) {
        char c = descriptor.charAt(start);
        switch (c) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 'V':
                return 1;
            case 'L':
                // Object type - find the semicolon
                return descriptor.indexOf(';', start) - start + 1;
            case '[':
                // Array type - recurse for element type
                return 1 + getTypeLength(descriptor, start + 1);
            default:
                return 1;
        }
    }

    private static void generateYamlRecipes(List<InlineMeMethod> methods, Path outputPath, String recipeName) throws IOException {
        StringBuilder yaml = new StringBuilder();
        Path licenseHeader = Paths.get("gradle/licenseHeader.txt");
        if (Files.isRegularFile(licenseHeader)) {
            try (Stream<String> lines = Files.lines(licenseHeader)) {
                lines.forEach(line -> yaml
                        .append("# ")
                        .append(line.replace("${year}", String.valueOf(Year.now().getValue())))
                        .append("\n"));
            }
        }

        yaml.append("#\n");
        yaml.append("# Generated InlineMe recipes from TypeTable\n");
        yaml.append("#\n\n");

        yaml.append("type: specs.openrewrite.org/v1beta/recipe\n");
        yaml.append(format("name: %s\n", recipeName));
        yaml.append("displayName: Inline methods annotated with `@InlineMe`\n");
        yaml.append("description: >-\n");
        yaml.append("  Automatically generated recipes to inline method calls based on `@InlineMe` annotations\n");
        yaml.append("  discovered in the type table.\n");
        yaml.append("recipeList:\n");

        // Group methods by GAV for better organization
        Map<TypeTable.GroupArtifactVersion, List<InlineMeMethod>> methodsByGav =
                methods.stream().collect(groupingBy(m -> m.gav));

        for (Map.Entry<TypeTable.GroupArtifactVersion, List<InlineMeMethod>> entry : methodsByGav.entrySet()) {
            TypeTable.GroupArtifactVersion gav = entry.getKey();
            List<InlineMeMethod> gavMethods = entry.getValue();

            yaml.append("\n  # From ").append(gav.getGroupId()).append(":").append(gav.getArtifactId())
                    .append(":").append(gav.getVersion()).append("\n");

            for (InlineMeMethod method : gavMethods) {
                yaml.append("  - org.openrewrite.java.InlineMethodCalls:\n");
                yaml.append("      methodPattern: '").append(escapeYaml(method.methodPattern)).append("'\n");
                yaml.append("      replacement: '").append(escapeYaml(method.replacement)).append("'\n");

                if (!method.imports.isEmpty()) {
                    yaml.append("      imports:\n");
                    for (String imp : method.imports) {
                        yaml.append("        - '").append(escapeYaml(imp)).append("'\n");
                    }
                }

                if (!method.staticImports.isEmpty()) {
                    yaml.append("      staticImports:\n");
                    for (String imp : method.staticImports) {
                        yaml.append("        - '").append(escapeYaml(imp)).append("'\n");
                    }
                }

                yaml.append("      classpathFromResources:\n");
                yaml.append("        - '").append(escapeYaml(method.classpathResource)).append("'\n");
            }
        }

        Files.write(outputPath, yaml.toString().getBytes());
    }

    private static String escapeYaml(String value) {
        // Escape single quotes by doubling them
        return value.replace("'", "''");
    }

    @AllArgsConstructor
    private static class InlineMeMethod {
        final TypeTable.GroupArtifactVersion gav;
        final String methodPattern;
        final String replacement;
        final List<String> imports;
        final List<String> staticImports;
        final String classpathResource;
    }
}
