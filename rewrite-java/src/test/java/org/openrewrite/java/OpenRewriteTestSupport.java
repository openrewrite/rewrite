/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Fabian Krüger
 */
package org.openrewrite.java;

import org.assertj.core.api.Assertions;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class OpenRewriteTestSupport {

//    /**
//     * Creates <code>J.CompilationUnit</code>s from given sourceCodes.
//     *
//     * The first class name and package is used to retrieve the file path of the <code>J.CompilationUnit</code>.
//     *
//     * @param classpath   entries in <code>artifact:gruopId:version</code> format.
//     * @param sourceCodes
//     * @return list of <code>J.CompilationUnit</code>s
//     */
//    public static List<J.CompilationUnit> createCompilationUnitsFromStrings(List<String> classpath, String... sourceCodes) {
//        JavaParser javaParser = OpenRewriteTestSupport.getJavaParser(classpath.toArray(new String[]{}));
//        List<J.CompilationUnit> compilationUnits = javaParser.parse(sourceCodes);
//        compilationUnits.stream()
//                .map(compilationUnit -> {
//                    Path filePath = Path.of(JavaSourceTestUtil.retrieveFullyQualifiedClassFileName(compilationUnit.print()));
//                    compilationUnit = compilationUnit.withSourcePath(filePath);
//                    return compilationUnit;
//                })
//                .collect(Collectors.toList());
//        return compilationUnits;
//    }

    /**
     * Creates <code>J.CompilationUnit</code>s from given sourceCodes.
     *
     * The first class name and package is used to retrieve the file path of the <code>J.CompilationUnit</code>.
     *
     * @param sourceCodes
     * @return list of <code>J.CompilationUnit</code>s
     */
//    public static List<J.CompilationUnit> createCompilationUnitsFromStrings(String... sourceCodes) {
//        return createCompilationUnitsFromStrings(List.of(), sourceCodes);
//    }

    /**
     * Creates a <code>J.CompilationUnit</code> from given sourceCode.
     *
     * The first class name and package is used to retrieve the file path of the <code>J.CompilationUnit</code>.
     *
     * @param sourceCode
     * @return the created <code>J.CompilationUnit</code>
     */
//    public static J.CompilationUnit createCompilationUnitFromString(String sourceCode) {
//        return createCompilationUnitsFromStrings(List.of(), sourceCode).get(0);
//    }

    public static J.CompilationUnit createCompilationUnit(JavaParser parser, Path sourceFolder, String sourceCode) {
        J.CompilationUnit cu = parser.parse(sourceCode).get(0);
        return cu.withSourcePath(cu.getPackageDeclaration() == null
                ? sourceFolder.resolve(cu.getSourcePath())
                : sourceFolder
                .resolve(cu.getPackageDeclaration().getExpression().printTrimmed().replace('.', File.separatorChar))
                .resolve(cu.getSourcePath()));
    }

    /**
     * Verifies that applying the visitor to given results in expected.
     *
     * @param visitor   Supplier to supply the visitor from a lambda
     * @param given     CompilationUnit the visitor will be applied on
     * @param expected  source code after applying the visitor
     * @param classpath required for resolving dependencies to create a CompilationUnit from given
     */
    public static <P> void verifyChange(Supplier<JavaIsoVisitor<ExecutionContext>> visitor, String given, String expected, String... classpath) {
        verifyChange(visitor.get(), given, expected, classpath);
    }


    /**
     * Verifies that applying the visitor to given results in expected.
     *
     * @param visitor   that will be applied
     * @param given     CompilationUnit the visitor will be applied on
     * @param expected  source code after applying the visitor
     * @param classpath required for resolving dependencies to create a CompilationUnit from given
     */
    public static <P> void verifyChange(JavaIsoVisitor<ExecutionContext> visitor, String given, String expected, String... classpath) {
        J.CompilationUnit compilationUnit = createCompilationUnit(given, classpath);
        verifyChange(visitor, compilationUnit, expected);
    }

    /**
     * Verifies that applying the visitor to given results in expected.
     *
     * Additionally it's verified that the returned change contains exactly given as before and expected as after
     *
     * Use this method if you had to create a CompilationUnit, e.g. to define a scope for the tested visitor.
     * If the visitor is not scoped it is probably easier (and less caller code)
     * to use
     * {@link #verifyChange(JavaIsoVisitor, String, String, String...)}
     * or
     * {@link #verifyChange(Supplier, String, String, String...)}
     *
     * @param visitor  that will be applied
     * @param given    CompilationUnit the visitor will be applied on
     * @param expected source code after applying the visitor
     */
    public static <P> void verifyChange(JavaIsoVisitor<ExecutionContext> visitor, J.CompilationUnit given, String expected) {
        final Collection<Result> newChanges = refactor(given, visitor);
        Assertions.assertThat(newChanges.iterator().hasNext()).as("No change was found.").isTrue();
        Assertions.assertThat(given.print())
                .as(TestDiff.of(given.print(), newChanges.iterator().next().getBefore().printAll()))
                .isEqualTo(newChanges.iterator().next().getBefore().printAll());

        Assertions.assertThat(newChanges.iterator().next().getAfter().printAll())
                .as(TestDiff.of(newChanges.iterator().next().getAfter().printAll(), expected))
                .isEqualTo(expected);
    }

    /**
     * Verifies that applying the visitor to given results in expected.
     *
     * It's does not check that given equals before in the change.
     * Use this method if you had to create a CompilationUnit, e.g. to define a scope for the tested visitor.
     * If the visitor is not scoped it is probably easier (and less caller code)
     * to use
     * {@link #verifyChange(JavaIsoVisitor, String, String, String...)}
     * or
     * {@link #verifyChange(Supplier, String, String, String...)}
     *
     * @param visitor  that will be applied
     * @param given    CompilationUnit the visitor will be applied on
     * @param expected source code after applying the visitor
     */
    public static <P> void verifyChangeIgnoringGiven(JavaIsoVisitor<ExecutionContext> visitor, String given, String expected, String... classpath) {
        J.CompilationUnit compilationUnit = createCompilationUnit(given, classpath);
        final Collection<Result> newChanges = refactor(compilationUnit, visitor);
        Assertions.assertThat(newChanges.iterator().hasNext()).as("No change was found.").isTrue();
        Assertions.assertThat(expected)
                .as(TestDiff.of(expected, newChanges.iterator().next().getAfter().printAll()))
                .isEqualTo(newChanges.iterator().next().getAfter().printAll());
    }

    /**
     * Verify that applying the given visitor doesn't change the given sourceCode
     *
     * @param visitor   supplies the visitor to apply
     * @param given     the source code to apply the visitor on
     * @param classpath required to compile the given sourceCode in 'groupId:artifactId:version' format
     */
    public static <P> void verifyNoChange(Supplier<JavaIsoVisitor<ExecutionContext>> visitor, String given, String... classpath) {
        J.CompilationUnit compilationUnit = createCompilationUnit(given, classpath);
        final Collection<Result> newChanges = refactor(compilationUnit, visitor.get());
        Assertions.assertThat(newChanges).isEmpty();
    }

    /**
     * Verify that applying the given visitor doesn't change the given sourceCode
     *
     * @param visitor   to apply
     * @param given     the source code to apply the visitor on
     * @param classpath required to compile the given sourceCode in 'groupId:artifactId:version' format
     */
    public static <P> void verifyNoChange(JavaIsoVisitor<ExecutionContext> visitor, String given, String... classpath) {
        J.CompilationUnit compilationUnit = createCompilationUnit(given, classpath);
        final Collection<Result> newChanges = refactor(compilationUnit, visitor);
        Assertions.assertThat(newChanges).isEmpty();
    }

    /**
     * Create a <code>J.CompilationUnit</code> from given using classpath
     *
     * @param given     sourceCode
     * @param classpath provided in 'groupId:artifactId:version' format
     */
    public static J.CompilationUnit createCompilationUnit(String given, String... classpath) {
        JavaParser javaParser = getJavaParser(classpath);

        List<J.CompilationUnit> compilationUnits = javaParser
                .parse(given);
        if (compilationUnits.size() > 1)
            throw new RuntimeException("More than one compilation was found in given String");
        return compilationUnits.get(0);
    }

    /**
     * Create a <code>JavaParser</code> with provided classpath.
     *
     * @param classpath in 'groupId:artifactId:version' format
     */
    public static  JavaParser getJavaParser(String... classpath) {
        JavaParser.Builder<? extends JavaParser, ?> jp = JavaParser.fromJavaVersion()
                                                    .logCompilationWarningsAndErrors(true)
                                                    .relaxedClassTypeMatching(false);

//        List<Path> collect = getClasspathFiles(classpath);
//        if (classpath.length > 0) {
//            jp.classpath(collect);
//        }

        return jp.build();
    }

    /**
     * Retrieve the <code>Path</code>s of jars for given classpath
     *
     */
//    public static List<Path> getClasspathFiles(String... classpath) {
//        if (classpath.length == 0) return List.of();
//        File[] as = Maven.resolver().resolve(classpath).withTransitivity().as(File.class);
//        return Arrays.stream(as)
//                .map(File::toPath)
//                .collect(Collectors.toList());
//    }

    private static <P> Collection<Result> refactor(J.CompilationUnit given, JavaVisitor<ExecutionContext> visitor) {
        GenericOpenRewriteTestRecipe<JavaVisitor<ExecutionContext>> recipe = new GenericOpenRewriteTestRecipe<>(visitor);
        return recipe.run(List.of(given));
    }

    /** Helper class to avoid circular dependency to the module containing {@code GenericOpenRewriteRecipe} */
    private static class GenericOpenRewriteTestRecipe<V extends TreeVisitor<?, ExecutionContext>> extends Recipe {

        private final V visitor;

        public GenericOpenRewriteTestRecipe(V visitor) {
            this.visitor = visitor;
        }

        @Override
        protected TreeVisitor<?, ExecutionContext> getVisitor() {
            return visitor;
        }

        @Override
        public String getDisplayName() {
            return visitor.getClass().getSimpleName();
        }
    }
}
