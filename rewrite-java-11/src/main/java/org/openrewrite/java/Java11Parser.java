/*
 * Copyright 2020 the original author or authors.
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

import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.openrewrite.Formatting;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * This parser is NOT thread-safe, as the OpenJDK parser maintains in-memory caches in static state.
 */
@NonNullApi
public class Java11Parser implements JavaParser {
    private static final Logger logger = LoggerFactory.getLogger(Java11Parser.class);

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

    private final JavacFileManager pfm;

    private final Context context = new Context();
    private final JavaCompiler compiler;
    private final ResettableLog compilerLog = new ResettableLog(context);
    private final Collection<JavaStyle> styles;

    private Java11Parser(@Nullable Collection<Path> classpath,
                         Charset charset,
                         boolean relaxedClassTypeMatching,
                         MeterRegistry meterRegistry,
                         boolean logCompilationWarningsAndErrors,
                         Collection<JavaStyle> styles) {
        this.meterRegistry = meterRegistry;
        this.classpath = classpath;
        this.relaxedClassTypeMatching = relaxedClassTypeMatching;
        this.styles = styles;

        this.pfm = new JavacFileManager(context, true, charset);
        context.put(JavaFileManager.class, this.pfm);

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
                var log = new String(Arrays.copyOfRange(cbuf, off, len));
                if (logCompilationWarningsAndErrors && !log.isBlank()) {
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
    public List<J.CompilationUnit> parseInputs(Iterable<Input> sourceFiles, @Nullable URI relativeTo) {
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

        var cus = acceptedInputs(sourceFiles).stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        input -> Timer.builder("rewrite.parse")
                                .description("The time spent by the JDK in parsing and tokenizing the source file")
                                .tag("file.type", "Java")
                                .tag("step", "JDK parsing")
                                .register(meterRegistry)
                                .record(() -> {
                                    try {
                                        return compiler.parse(new Java11ParserInputFileObject(input));
                                    } catch (IllegalStateException e) {
                                        if (e.getMessage().equals("endPosTable already set")) {
                                            throw new IllegalStateException("Call reset() on JavaParser before parsing another" +
                                                    "set of source files that have some of the same fully qualified names", e);
                                        }
                                        throw e;
                                    }
                                }),
                        (e2, e1) -> e1, LinkedHashMap::new));

        try {
            initModules(cus.values());
            enterAll(cus.values());

            // For some reason this is necessary in JDK 9+, where the the internal block counter that
            // annotationsBlocked() tests against remains >0 after attribution.
            Annotate annotate = Annotate.instance(context);
            while (annotate.annotationsBlocked()) {
                annotate.unblockAnnotations(); // also flushes once unblocked
            }

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
                            var input = cuByPath.getKey();
                            logger.trace("Building AST for {}", input.getUri());
                            Java11ParserVisitor parser = new Java11ParserVisitor(
                                    input.getRelativePath(relativeTo),
                                    StringUtils.readFully(input.getSource()),
                                    relaxedClassTypeMatching, styles);
                            return (J.CompilationUnit) parser.scan(cuByPath.getValue(), Formatting.EMPTY);
                        })
        ).collect(toList());
    }

    @Override
    public Java11Parser reset() {
        compilerLog.reset();
        pfm.flush();
        Check.instance(context).newRound();
        Annotate.instance(context).newRound();
        Enter.instance(context).newRound();
        Modules.instance(context).newRound();
        return this;
    }

    /**
     * Initialize modules
     */
    private void initModules(Collection<JCTree.JCCompilationUnit> cus) {
        var modules = Modules.instance(context);
        modules.initModules(com.sun.tools.javac.util.List.from(cus));
    }

    /**
     * Enter symbol definitions into each compilation unit's scope
     */
    private void enterAll(Collection<JCTree.JCCompilationUnit> cus) {
        var enter = Enter.instance(context);
        var compilationUnits = com.sun.tools.javac.util.List.from(
                cus.toArray(JCTree.JCCompilationUnit[]::new));
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

    public static class Builder extends JavaParser.Builder<Java11Parser, Builder> {
        @Override
        public Java11Parser build() {
            return new Java11Parser(classpath, charset, relaxedClassTypeMatching,
                    meterRegistry, logCompilationWarningsAndErrors, styles);
        }
    }
}
