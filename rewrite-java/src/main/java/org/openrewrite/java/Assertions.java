package org.openrewrite.java;

import org.intellij.lang.annotations.Language;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.test.ParserSupplier;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.openrewrite.test.SourceSpecs.dir;

public class Assertions {
    private static final Map<Integer, JavaVersion> javaVersions = new HashMap<>();
    private static final Map<String, JavaProject> javaProjects = new HashMap<>();
    private static final Map<String, JavaSourceSet> javaSourceSets = new HashMap<>();

    private Assertions() {
    }

    public static SourceSpecs java(@Language("java") @Nullable String before) {
        return java(before, s -> {
        });
    }

    private static ParserSupplier javaParserSupplier() {
        return new ParserSupplier(J.CompilationUnit.class, "java", () -> JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true)
                .build());
    }

    public static SourceSpecs java(@Language("java") @Nullable String before, Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(
                J.CompilationUnit.class, null, javaParserSupplier(), before, null,
                (after, testMethodSpec, testClassSpec) -> {
                    if (after instanceof JavaSourceFile) {
                        TypeValidation typeValidation = testMethodSpec.typeValidation != null ? testMethodSpec.typeValidation : testClassSpec.typeValidation;
                        if (typeValidation == null) {
                            typeValidation = new TypeValidation();
                        }
                        typeValidation.assertValidTypes((JavaSourceFile) after);
                    }
                }
        );
        spec.accept(java);
        return java;
    }

    public static SourceSpecs java(@Language("java") @Nullable String before, @Language("java") String after) {
        return java(before, after, s -> {
        });
    }

    public static SourceSpecs java(@Language("java") @Nullable String before, @Language("java") String after,
                                   Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(J.CompilationUnit.class, null, javaParserSupplier(), before, after);
        spec.accept(java);
        return java;
    }

    public static SourceSpecs mavenProject(String project, Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... sources) {
        return dir(project, spec, sources);
    }

    public static SourceSpecs mavenProject(String project, SourceSpecs... sources) {
        return mavenProject(project, spec -> project(spec, project), sources);
    }

    public static SourceSpecs srcMainJava(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/main/java", spec, javaSources);
    }

    public static SourceSpecs srcMainJava(SourceSpecs... javaSources) {
        return srcMainJava(spec -> sourceSet(spec, "main"), javaSources);
    }

    public static SourceSpecs srcMainResources(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... resources) {
        return dir("src/main/resources", spec, resources);
    }

    public static SourceSpecs srcMainResources(SourceSpecs... resources) {
        return srcMainResources(spec -> sourceSet(spec, "main"), resources);
    }

    public static SourceSpecs srcTestJava(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/test/java", spec, javaSources);
    }

    public static SourceSpecs srcTestJava(SourceSpecs... javaSources) {
        return srcTestJava(spec -> sourceSet(spec, "test"), javaSources);
    }

    public static SourceSpecs srcTestResources(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... resources) {
        return dir("src/test/resources", spec, resources);
    }

    public static SourceSpecs srcTestResources(SourceSpecs... resources) {
        return srcTestResources(spec -> sourceSet(spec, "test"), resources);
    }

    public static SourceSpec<?> version(SourceSpec<?> sourceSpec, int version) {
        return sourceSpec.markers(javaVersion(version));
    }

    public static SourceSpec<?> project(SourceSpec<?> sourceSpec, String projectName) {
        return sourceSpec.markers(javaProject(projectName));
    }

    public static SourceSpec<?> sourceSet(SourceSpec<?> sourceSpec, String sourceSet) {
        sourceSpec.setSourceSetName(sourceSet);
        return sourceSpec;
    }

    private static JavaVersion javaVersion(int version) {
        return javaVersions.computeIfAbsent(version, v ->
                new JavaVersion(Tree.randomId(), "openjdk", "adoptopenjdk",
                        Integer.toString(v), Integer.toString(v)));
    }

    private static JavaProject javaProject(String projectName) {
        return javaProjects.computeIfAbsent(projectName, name ->
                new JavaProject(Tree.randomId(), name, null));
    }

    private static JavaSourceSet javaSourceSet(String sourceSet) {
        return javaSourceSets.computeIfAbsent(sourceSet, name ->
                new JavaSourceSet(Tree.randomId(), name, Collections.emptyList()));
    }
}
