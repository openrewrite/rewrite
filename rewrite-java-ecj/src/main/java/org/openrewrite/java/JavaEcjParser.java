/*
 * Copyright 2023 the original author or authors.
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.ClasspathJar;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;

// check out CodeSnippetParser
public class JavaEcjParser implements JavaParser {
    private static final Logger logger = LogManager.getLogger(JavaEcjParser.class);

    private final long languageLevel;

    @Nullable
    private final Collection<Path> classpath;

    private final MeterRegistry meterRegistry;

    /**
     * When true, enables a parser to use class types from the in-memory type cache rather than performing
     * a deep equality check. Useful when deep class types have already been built from a separate parsing phase
     * and we want to parse some code snippet without requiring the classpath to be fully specified, using type
     * information we've already learned about in a prior phase.
     */
    private final boolean relaxedClassTypeMatching;

    private final Collection<JavaStyle> styles;

    private JavaEcjParser(long languageLevel,
                          @Nullable Collection<Path> classpath,
                          Charset charset,
                          boolean relaxedClassTypeMatching,
                          MeterRegistry meterRegistry,
                          boolean logCompilationWarningsAndErrors,
                          Collection<JavaStyle> styles) {
        this.languageLevel = languageLevel;
        this.meterRegistry = meterRegistry;
        this.classpath = classpath;
        this.relaxedClassTypeMatching = relaxedClassTypeMatching;
        this.styles = styles;
    }

    // org.eclipse.jdt.internal.compiler.Compiler line 452
    // this.options.generateClassFiles

    @Override
    public List<J.CompilationUnit> parseInputs(Iterable<Input> sources, Path relativeTo, ExecutionContext ctx) {
        try {
            CompilerOptions compilerOptions = new CompilerOptions();
            compilerOptions.generateClassFiles = false;
            compilerOptions.generateGenericSignatureForLambdaExpressions = true;
            compilerOptions.complianceLevel = languageLevel;
//            compilerOptions.performMethodsFullRecovery
//            compilerOptions.performStatementsRecovery

            // See Compiler line 907: "No need of analysis or generation of code if statements are not required"
//            compilerOptions.ignoreMethodBodies

//            compilerOptions.verbose = true;

            List<J.CompilationUnit> cus = new ArrayList<>();

            List<FileSystem.Classpath> classpath = new ArrayList<>();

            // NOTE: will do nothing in natively compiled image
            Util.collectVMBootclasspath(classpath, Util.getJavaHome());

            if (this.classpath != null) {
                this.classpath.forEach(cp -> new ClasspathJar(cp.toFile(), true,
                        null, null));
            }

            FileSystem nameEnvironment = new FileSystem(
                    classpath.toArray(new FileSystem.Classpath[0]),
                    new String[0],
                    true) {
            };

            Stack<CompilationUnit> compilationUnitStack = stream(sources.spliterator(), false)
                    .map(source -> new CompilationUnit(
                            StringUtils.readFully(source.getSource()).toCharArray(),
                            source.getPath().toString(),
                            StandardCharsets.UTF_8.name()
                    ))
                    .collect(Collectors.toCollection(Stack::new));

            long start = System.currentTimeMillis();

            @SuppressWarnings("deprecation")
            Compiler compiler = new Compiler(
                    nameEnvironment,
                    PERMISSIVE_ERROR_HANDLING,
                    compilerOptions,
                    result -> {
                        if(result.getAllProblems() != null) {
                            for (CategorizedProblem problem : result.getAllProblems()) {
                                logger.warn(problem.getMessage());
                            }
                        }
                    },
                    new DefaultProblemFactory(),
                    IoBuilder.forLogger(logger).setLevel(Level.WARN).buildPrintWriter()
            ) {
                @Override
                public void process(CompilationUnitDeclaration unit, int i) {
                    super.process(unit, i);

                    logger.info("Batch compiler took: " + (System.currentTimeMillis() - start));
                    JavaEcjParserVisitor visitor = new JavaEcjParserVisitor(compilationUnitStack.pop());
                    cus.add(visitor.visit(unit));

                    logger.info("Full Rewrite parse: " + (System.currentTimeMillis() - start));

                }
            };

            compiler.compile(compilationUnitStack.toArray(new ICompilationUnit[0]));

            return cus;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }



    @Override
    public JavaParser reset() {
        return this;
    }

    @Override
    public JavaParser reset(Collection<URI> uris) {
        return this;
    }

    @Override
    public void setClasspath(Collection<Path> classpath) {

    }

    @Override
    public void setSourceSet(String sourceSet) {

    }

    @Override
    public JavaSourceSet getSourceSet(ExecutionContext ctx) {
        return null;
    }

    static IPath projectPath(Iterable<Input> sources) {
        Optional<Path> path = stream(sources.spliterator(), false)
                .map(Input::getPath)
                .reduce((p1, p2) -> {
                    Path relativePath = p1.relativize(p2).normalize();
                    while (relativePath != null && !relativePath.endsWith("..")) {
                        relativePath = relativePath.getParent();
                    }
                    //noinspection ConstantConditions
                    return p1.resolve(relativePath).normalize();
                });

        return path
                .map(p -> org.eclipse.core.runtime.Path.fromPortableString(p.toString()))
                .orElseThrow(() -> new IllegalArgumentException("No common path for inputs"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends JavaParser.Builder<JavaEcjParser, Builder> {
        private long languageLevel = ClassFileConstants.JDK11;

        /**
         * @param languageLevel A language level constant defined in {@link ClassFileConstants}.
         * @return This builder.
         */
        public Builder languageLevel(long languageLevel) {
            this.languageLevel = languageLevel;
            return this;
        }

        @Override
        public JavaEcjParser build() {
            return new JavaEcjParser(languageLevel, classpath, charset, true,
                    new SimpleMeterRegistry(), logCompilationWarningsAndErrors, new ArrayList<>());
        }
    }

    private static final IErrorHandlingPolicy PERMISSIVE_ERROR_HANDLING = new IErrorHandlingPolicy() {
        @Override
        public boolean proceedOnErrors() {
            return true;
        }

        @Override
        public boolean stopOnFirstError() {
            return false;
        }

        @Override
        public boolean ignoreAllErrors() {
            return false;
        }
    };
}
