package org.openrewrite.java.internal.parser;

import org.openrewrite.InMemoryExecutionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class InlineMethodCallsRecipeGenerator {

    private static final String INLINE_ME_DESCRIPTOR = "Lorg/openrewrite/java/InlineMe;";

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: InlineMethodCallsRecipeGenerator <input-tsv-path> <output-yaml-path>");
            System.exit(1);
        }

        Path inputPath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);

        List<InlineMeMethod> inlineMethods = new ArrayList<>();

        TypeTable.Reader reader = new TypeTable.Reader(new InMemoryExecutionContext());
        try (InputStream is = Files.newInputStream(inputPath)) {
            reader.parseTsvAndProcess(is, TypeTable.Reader.Options.matchAll(), (gav, classes, nestedTypes) -> {
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
            generateYamlRecipes(inlineMethods, outputPath);

            System.out.println("Generated " + inlineMethods.size() + " inline recipes to " + outputPath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static @org.jspecify.annotations.Nullable InlineMeMethod extractInlineMeMethod(TypeTable.GroupArtifactVersion gav,
                                                         TypeTable.ClassDefinition classDef,
                                                         TypeTable.Member member) {
        try {
            // Parse the annotations to find @InlineMe
            List<AnnotationDeserializer.AnnotationInfo> annotations =
                AnnotationDeserializer.parseAnnotations(member.getAnnotations());

            for (AnnotationDeserializer.AnnotationInfo annotation : annotations) {
                if (INLINE_ME_DESCRIPTOR.equals(annotation.getDescriptor())) {
                    // Extract annotation values
                    String replacement = null;
                    List<String> imports = new ArrayList<>();
                    List<String> staticImports = new ArrayList<>();

                    List<AnnotationDeserializer.AttributeInfo> attributes = annotation.getAttributes();
                    if (attributes != null) {
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
                    }

                    if (replacement != null) {
                        // Build the method pattern
                        String methodPattern = buildMethodPattern(classDef, member);

                        return new InlineMeMethod(
                            gav,
                            classDef.getName().replace('/', '.'),
                            member.getName(),
                            methodPattern,
                            replacement,
                            imports,
                            staticImports
                        );
                    }
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

        // Build parameter pattern - using (..) to match any parameters
        String paramPattern = "(..)";

        return className + " " + methodName + paramPattern;
    }

    private static void generateYamlRecipes(List<InlineMeMethod> methods, Path outputPath) throws IOException {
        StringBuilder yaml = new StringBuilder();
        yaml.append("#\n");
        yaml.append("# Generated InlineMe recipes from TypeTable\n");
        yaml.append("# Generated at: ").append(new Date()).append("\n");
        yaml.append("#\n\n");

        yaml.append("type: specs.openrewrite.org/v1beta/recipe\n");
        yaml.append("name: org.openrewrite.java.InlineMethodCallsGenerated\n");
        yaml.append("displayName: Inline methods annotated with @InlineMe\n");
        yaml.append("description: >\n");
        yaml.append("  Automatically generated recipes to inline method calls based on @InlineMe annotations\n");
        yaml.append("  discovered in the classpath.\n");
        yaml.append("recipeList:\n");

        // Group methods by GAV for better organization
        Map<TypeTable.GroupArtifactVersion, List<InlineMeMethod>> methodsByGav =
            methods.stream().collect(Collectors.groupingBy(m -> m.gav));

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
            }
        }

        Files.write(outputPath, yaml.toString().getBytes());
    }

    private static String escapeYaml(String value) {
        // Escape single quotes by doubling them
        return value.replace("'", "''");
    }

    private static class InlineMeMethod {
        final TypeTable.GroupArtifactVersion gav;
        final String className;
        final String methodName;
        final String methodPattern;
        final String replacement;
        final List<String> imports;
        final List<String> staticImports;

        InlineMeMethod(TypeTable.GroupArtifactVersion gav, String className, String methodName,
                      String methodPattern, String replacement,
                      List<String> imports, List<String> staticImports) {
            this.gav = gav;
            this.className = className;
            this.methodName = methodName;
            this.methodPattern = methodPattern;
            this.replacement = replacement;
            this.imports = imports;
            this.staticImports = staticImports;
        }
    }
}