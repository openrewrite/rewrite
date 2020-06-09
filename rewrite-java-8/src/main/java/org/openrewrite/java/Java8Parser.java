package org.openrewrite.java;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java8tools.com.sun.tools.javac.comp.*;
import java8tools.com.sun.tools.javac.file.JavacFileManager;
import java8tools.com.sun.tools.javac.main.JavaCompiler;
import java8tools.com.sun.tools.javac.tree.JCTree;
import java8tools.com.sun.tools.javac.util.Context;
import java8tools.com.sun.tools.javac.util.Log;
import java8tools.com.sun.tools.javac.util.Options;
import org.openrewrite.Formatting;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class Java8Parser implements JavaParser {
    private static final Logger logger = LoggerFactory.getLogger(Java8Parser.class);

    @Nullable
    private final List<Path> classpath;

    private final Charset charset;

    private final MeterRegistry meterRegistry;

    /**
     * When true, enables a parser to use class types from the in-memory type cache rather than performing
     * a deep equality check. Useful when deep class types have already been built from a separate parsing phase
     * and we want to parse some code snippet without requiring the classpath to be fully specified, using type
     * information we've already learned about in a prior phase.
     */
    private final boolean relaxedClassTypeMatching;

    private final JavacFileManager pfm;

    private final Context context = new Context();
    private final JavaCompiler compiler;
    private final ResettableLog compilerLog = new ResettableLog(context);

    private Java8Parser(@Nullable List<Path> classpath, Charset charset,
                         boolean relaxedClassTypeMatching,
                         MeterRegistry meterRegistry,
                         boolean logCompilationWarningsAndErrors) {
        this.meterRegistry = meterRegistry;
        this.classpath = classpath;
        this.charset = charset;
        this.relaxedClassTypeMatching = relaxedClassTypeMatching;
        this.pfm = new JavacFileManager(context, true, charset);

        // otherwise, consecutive string literals in binary expressions are concatenated by the parser, losing the original
        // structure of the expression!
        Options.instance(context).put("allowStringFolding", "false");

        // MUST be created (registered with the context) after pfm and compilerLog
        compiler = new JavaCompiler(context);

        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position
        // for every tree element
        compiler.genEndPos = true;
        compiler.keepComments = true;

        compilerLog.setWriters(new PrintWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) {
                String log = new String(Arrays.copyOfRange(cbuf, off, len));
                if (logCompilationWarningsAndErrors && !StringUtils.isBlank(log)) {
                    logger.warn(log);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        }));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<J.CompilationUnit> parse(List<Path> sourceFiles, @Nullable Path relativeTo) {
        if (classpath != null) { // override classpath
            if (context.get(JavaFileManager.class) != pfm) {
                throw new IllegalStateException("JavaFileManager has been forked unexpectedly");
            }

            try {
                pfm.setLocation(StandardLocation.CLASS_PATH, classpath.stream().map(Path::toFile).collect(toList()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Iterable<? extends JavaFileObject> fileObjects = pfm.getJavaFileObjects(filterSourceFiles(sourceFiles)
                .stream().map(Path::toFile).toArray(File[]::new));
        Map<Path, JCTree.JCCompilationUnit> cus = stream(fileObjects.spliterator(), false)
                .collect(Collectors.toMap(
                        p -> Paths.get(p.toUri()),
                        filename -> Timer.builder("rewrite.parse")
                                .description("The time spent by the JDK in parsing and tokenizing the source file")
                                .tag("file.type", "Java")
                                .tag("step", "JDK parsing")
                                .register(meterRegistry)
                                .record(() -> compiler.parse(filename)),
                        (e2, e1) -> e1,
                        LinkedHashMap::new));

        try {
            enterAll(cus.values());
            compiler.attribute(new TimedTodo(compiler.todo));
        } catch (Throwable t) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always a BEST EFFORT in the presence of errors)
            logger.warn("Failed symbol entering or attribution", t);
        }

        return cus.entrySet().stream().map(cuByPath ->
                Timer.builder("rewrite.parse")
                        .description("The time spent mapping the OpenJDK AST to Rewrite's AST")
                        .tag("file.type", "Java")
                        .tag("step", "Map to Rewrite AST")
                        .register(meterRegistry)
                        .record(() -> {
                            Path path = cuByPath.getKey();
                            logger.trace("Building AST for {}", path.toAbsolutePath().getFileName());
                            try {
                                Java8ParserVisitor parser = new Java8ParserVisitor(
                                        relativeTo == null ? path : relativeTo.relativize(path),
                                        new String(Files.readAllBytes(path), charset),
                                        relaxedClassTypeMatching);
                                return (J.CompilationUnit) parser.scan(cuByPath.getValue(), Formatting.EMPTY);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        })
        ).collect(toList());
    }

    @Override
    public Java8Parser reset() {
        compilerLog.reset();
        pfm.flush();
        Check.instance(context).compiled.clear();
        return this;
    }

    /**
     * Enter symbol definitions into each compilation unit's scope
     */
    private void enterAll(Collection<JCTree.JCCompilationUnit> cus) {
        Enter enter = Enter.instance(context);
        java8tools.com.sun.tools.javac.util.List<JCTree.JCCompilationUnit> compilationUnits = java8tools.com.sun.tools.javac.util.List.from(
                cus.toArray(new JCTree.JCCompilationUnit[0]));
        enter.main(compilationUnits);
    }

    private static class ResettableLog extends Log {
        protected ResettableLog(Context context) {
            super(context);
        }

        public void reset() {
            sourceMap.clear();
        }
    }

    private List<Path> filterSourceFiles(List<Path> sourceFiles) {
        return sourceFiles.stream().filter(source -> source.getFileName().toString().endsWith(".java"))
                .collect(Collectors.toList());
    }

    private class TimedTodo extends Todo {
        private final Todo todo;
        private Timer.Sample sample;

        private TimedTodo(Todo todo) {
            super(new Context());
            this.todo = todo;
        }

        @Override
        public boolean isEmpty() {
            if (sample != null) {
                sample.stop(Timer.builder("rewrite.parse")
                        .description("The time spent by the JDK in type attributing the source file")
                        .tag("file.type", "Java")
                        .tag("step", "Type attribution")
                        .register(meterRegistry));
            }
            return todo.isEmpty();
        }

        @Override
        public Env<AttrContext> remove() {
            this.sample = Timer.start();
            return todo.remove();
        }
    }

    public static class Builder extends JavaParser.Builder<Java8Parser, Builder> {
        @Override
        Java8Parser build() {
            return new Java8Parser(classpath, charset, relaxedClassTypeMatching,
                    meterRegistry, logCompilationWarningsAndErrors);
        }
    }
}
