/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.parse;

import com.netflix.rewrite.internal.lang.NonNullApi;
import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tr;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * This parser is NOT thread-safe, as the OpenJDK parser maintains in-memory caches in static state.
 */
@NonNullApi
public class OpenJdkParser extends AbstractParser {
    private static final Logger logger = LoggerFactory.getLogger(OpenJdkParser.class);

    @Nullable
    private final List<Path> classpath;

    private final Charset charset;

    private final JavacFileManager pfm;

    private final Context context = new Context();
    private final JavaCompiler compiler;
    private final ResettableLog compilerLog = new ResettableLog(context);

    public OpenJdkParser() {
        this(emptyList());
    }

    public OpenJdkParser(@Nullable List<Path> classpath) {
        this(classpath, Charset.defaultCharset());
    }

    public OpenJdkParser(@Nullable List<Path> classpath, Charset charset) {
        this.classpath = classpath;
        this.charset = charset;
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
                var log = new String(Arrays.copyOfRange(cbuf, off, len));
                if(!log.isBlank()) {
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

    @Override
    public List<Tr.CompilationUnit> parse(List<Path> sourceFiles, @Nullable Path relativeTo) {
        if (classpath != null) { // override classpath
            if(context.get(JavaFileManager.class) != pfm) {
                throw new IllegalStateException("JavaFileManager has been forked unexpectedly");
            }

            try {
                pfm.setLocation(StandardLocation.CLASS_PATH, classpath.stream().map(Path::toFile).collect(toList()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        var fileObjects = pfm.getJavaFileObjects(filterSourceFiles(sourceFiles).toArray(Path[]::new));
        var cus = stream(fileObjects.spliterator(), false)
                .collect(Collectors.toMap(p -> Paths.get(p.toUri()), compiler::parse,
                        (e2, e1) -> e1, LinkedHashMap::new));

        try {
            initModules(cus.values());
            enterAll(cus.values());

            // For some reason this is necessary in JDK 9+, where the the internal block counter that
            // annotationsBlocked() tests against remains >0 after attribution.
            Annotate annotate = Annotate.instance(context);
            while(annotate.annotationsBlocked()) {
                annotate.unblockAnnotations(); // also flushes once unblocked
            }

            compiler.attribute(compiler.todo);
        } catch(Throwable t) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always a BEST EFFORT in the presence of errors)
            logger.warn("Failed symbol entering or attribution", t);
        }

        return cus.entrySet().stream().map(cuByPath -> {
            var path = cuByPath.getKey();
            logger.trace("Building AST for {}", path.toAbsolutePath().getFileName());
            try {
                OpenJdkParserVisitor parser = new OpenJdkParserVisitor(
                        relativeTo == null ? path : relativeTo.relativize(path),
                        Files.readString(path, charset)
                );
                return (Tr.CompilationUnit) parser.scan(cuByPath.getValue(), Formatting.EMPTY);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).collect(toList());
    }

    public void reset() {
        compilerLog.reset();
        pfm.flush();
        Check.instance(context).newRound();
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
}
