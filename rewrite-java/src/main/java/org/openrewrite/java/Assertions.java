package org.openrewrite.java;

import org.intellij.lang.annotations.Language;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.ParserSupplier;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public class Assertions {
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
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(J.CompilationUnit.class, null, javaParserSupplier(), before, null);
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
        return mavenProject(project, spec -> spec.java().project(project), sources);
    }

    public static SourceSpecs srcMainJava(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/main/java", spec, javaSources);
    }

    public static SourceSpecs srcMainJava(SourceSpecs... javaSources) {
        return srcMainJava(spec -> spec.java().sourceSet("main"), javaSources);
    }

    public static SourceSpecs srcMainResources(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... resources) {
        return dir("src/main/resources", spec, resources);
    }

    public static SourceSpecs srcMainResources(SourceSpecs... resources) {
        return srcMainResources(spec -> spec.java().sourceSet("main"), resources);
    }

    public static SourceSpecs srcTestJava(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/test/java", spec, javaSources);
    }

    public static SourceSpecs srcTestJava(SourceSpecs... javaSources) {
        return srcTestJava(spec -> spec.java().sourceSet("test"), javaSources);
    }

    public static SourceSpecs srcTestResources(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... resources) {
        return dir("src/test/resources", spec, resources);
    }

    public static SourceSpecs srcTestResources(SourceSpecs... resources) {
        return srcTestResources(spec -> spec.java().sourceSet("test"), resources);
    }

//    public static Java java(SourceSpec<?>... specs) {
//        return new Java();
//    }
//
//    public class Java {
//        public Java version(int version) {
//            markers(SourceSpecMarkers.javaVersion(version));
//            return this;
//        }
//
//        public Java project(String projectName) {
//            markers(SourceSpecMarkers.javaProject(projectName));
//            return this;
//        }
//
//        public Java sourceSet(String sourceSet) {
//            sourceSetName = sourceSet;
//            return this;
//        }
//    }
}
