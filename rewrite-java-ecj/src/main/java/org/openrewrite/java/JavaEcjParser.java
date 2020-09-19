package org.openrewrite.java;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
    public List<J.CompilationUnit> parseInputs(Iterable<Input> sources, Path relativeTo) {
        try {
            FileSystem nameEnvironment = new FileSystem(
                    classpath == null ?
                            new String[0] :
                            classpath.stream().map(Path::toString).toArray(String[]::new),
                    new String[0],
                    StandardCharsets.UTF_8.name());

            CompilerOptions compilerOptions = new CompilerOptions();
            compilerOptions.generateClassFiles = false;
            compilerOptions.generateGenericSignatureForLambdaExpressions = true;
            compilerOptions.complianceLevel = languageLevel;
//            compilerOptions.performMethodsFullRecovery
//            compilerOptions.performStatementsRecovery

            // See Compiler line 907: "No need of analysis or generation of code if statements are not required"
//            compilerOptions.ignoreMethodBodies

            List<J.CompilationUnit> cus = new ArrayList<>();

            Compiler compiler = new Compiler(
                    nameEnvironment,
                    PERMISSIVE_ERROR_HANDLING,
                    compilerOptions,
                    result -> {
                        // TODO call visitor to map to J.CompilationUnit and add to list
                        for (CategorizedProblem problem : result.getAllProblems()) {
                            logger.warn(problem.getMessage());
                        }

                        // J.CompilationUnit mapped = ...
                        // cus.add(mapped);
                    },
                    new DefaultProblemFactory(),
                    IoBuilder.forLogger(logger).setLevel(Level.WARN).buildPrintWriter()
            );

            compiler.compile(stream(sources.spliterator(), false)
                .map(source -> new org.eclipse.jdt.internal.compiler.batch.CompilationUnit(
                        StringUtils.readFully(source.getSource()).toCharArray(),
                        source.getPath().toString(),
                        StandardCharsets.UTF_8.name()
                ))
                .toArray(ICompilationUnit[]::new));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return null;
    }

    @Override
    public JavaParser reset() {
        return this;
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
            return new JavaEcjParser(languageLevel, classpath, charset, relaxedClassTypeMatching,
                    meterRegistry, logCompilationWarningsAndErrors, styles);
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
