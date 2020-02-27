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
package org.openrewrite;

import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.Formatting;
import org.openrewrite.tree.J;
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
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * This parser is NOT thread-safe, as the OpenJDK parser maintains in-memory caches in static state.
 */
@NonNullApi
public class JavaParser {
    private static final Logger logger = LoggerFactory.getLogger(JavaParser.class);

    /**
     * When true, enables a parser to use class types from the in-memory type cache rather than performing
     * a deep equality check. Useful when deep class types have already been built from a separate parsing phase
     * and we want to parse some code snippet without requiring the classpath to be fully specified, using type
     * information we've already learned about in a prior phase.
     */
    private final boolean relaxedClassTypeMatching;

    @Nullable
    private final List<Path> classpath;

    private final Charset charset;

    private final JavacFileManager pfm;

    private final Context context = new Context();
    private final JavaCompiler compiler;
    private final ResettableLog compilerLog = new ResettableLog(context);

    /**
     * Convenience utility for constructing a parser with binary dependencies on the runtime classpath of the process
     * constructing the parser.
     *
     * @param artifactNames The "artifact name" of the dependency to look for. Artifact name is the artifact portion of
     *                      group:artifact:version coordinates. For example, for Google's Guava (com.google.guava:guava:VERSION),
     *                      the artifact name is "guava".
     * @return A set of paths of jars on the runtime classpath matching the provided artifact names, to the extent such
     * matching jars can be found.
     */
    public static List<Path> dependenciesFromClasspath(String... artifactNames) {
        List<Pattern> artifactNamePatterns = Arrays.stream(artifactNames)
                .map(name -> Pattern.compile(name + "-.*?\\.jar$"))
                .collect(toList());

        return Arrays.stream(System.getProperty("java.class.path").split("\\Q" + System.getProperty("path.separator") + "\\E"))
                .filter(cpEntry -> artifactNamePatterns.stream().anyMatch(namePattern -> namePattern.matcher(cpEntry).find()))
                .map(cpEntry -> new File(cpEntry).toPath())
                .collect(toList());
    }

    public JavaParser() {
        this(null, Charset.defaultCharset(), false);
    }

    public JavaParser(@Nullable List<Path> classpath) {
        this(classpath, Charset.defaultCharset(), false);
    }

    public JavaParser(@Nullable List<Path> classpath, Charset charset, boolean relaxedClassTypeMatching) {
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
                var log = new String(Arrays.copyOfRange(cbuf, off, len));
                if (!log.isBlank()) {
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
            while (annotate.annotationsBlocked()) {
                annotate.unblockAnnotations(); // also flushes once unblocked
            }

            compiler.attribute(compiler.todo);
        } catch (Throwable t) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always a BEST EFFORT in the presence of errors)
            logger.warn("Failed symbol entering or attribution", t);
        }

        return cus.entrySet().stream().map(cuByPath -> {
            var path = cuByPath.getKey();
            logger.trace("Building AST for {}", path.toAbsolutePath().getFileName());
            try {
                JavaParserVisitor parser = new JavaParserVisitor(
                        relativeTo == null ? path : relativeTo.relativize(path),
                        Files.readString(path, charset),
                        relaxedClassTypeMatching);
                return (J.CompilationUnit) parser.scan(cuByPath.getValue(), Formatting.EMPTY);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).collect(toList());
    }

    public J.CompilationUnit parse(String source, String whichDependsOn) {
        return parse(source, singletonList(whichDependsOn));
    }

    public J.CompilationUnit parse(String source, List<String> whichDependOn) {
        return parse(source, whichDependOn.toArray(String[]::new));
    }

    public List<J.CompilationUnit> parse(List<Path> sourceFiles) {
        return parse(sourceFiles, null);
    }

    public J.CompilationUnit parse(String source, String... whichDependOn) {
        try {
            Path temp = Files.createTempDirectory("sources");

            var classPattern = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)");

            Function<String, String> simpleName = sourceStr -> {
                var classMatcher = classPattern.matcher(sourceStr);
                return classMatcher.find() ? classMatcher.group(3) : null;
            };

            Function<String, Path> sourceFile = sourceText -> {
                var file = temp.resolve(simpleName.apply(sourceText) + ".java");
                try {
                    Files.writeString(file, sourceText);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return file;
            };

            try {
                List<J.CompilationUnit> cus = parse(Stream.concat(
                        Arrays.stream(whichDependOn).map(sourceFile),
                        Stream.of(sourceFile.apply(source))
                ).collect(toList()));

                return cus.get(cus.size() - 1);
            } finally {
                // delete temp recursively
                //noinspection ResultOfMethodCallIgnored
                Files.walk(temp)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Clear any in-memory parser caches that may prevent reparsing of classes with the same fully qualified name in
     * different rounds
     */
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

    private List<Path> filterSourceFiles(List<Path> sourceFiles) {
        return sourceFiles.stream().filter(source -> source.getFileName().toString().endsWith(".java"))
                .collect(Collectors.toList());
    }
}
