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
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.NamedStyles;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * This parser is NOT thread-safe, as the OpenJDK parser maintains in-memory caches in static state.
 */
@NonNullApi
public class Java11Parser implements JavaParser {

    @Nullable
    private Collection<Path> classpath;

    @Nullable
    private final Collection<Input> dependsOn;

    /**
     * When true, enables a parser to use class types from the in-memory type cache rather than performing
     * a deep equality check. Useful when deep class types have already been built from a separate parsing phase
     * and we want to parse some code snippet without requiring the classpath to be fully specified, using type
     * information we've already learned about in a prior phase.
     */
    private final boolean relaxedClassTypeMatching;

    private final JavacFileManager pfm;

    private final Context context;
    private final JavaCompiler compiler;
    private final ResettableLog compilerLog;
    private final Collection<NamedStyles> styles;

    private Java11Parser(@Nullable Collection<Path> classpath,
                         Collection<byte[]> classBytesClasspath,
                         @Nullable Collection<Input> dependsOn,
                         Charset charset,
                         boolean relaxedClassTypeMatching,
                         boolean logCompilationWarningsAndErrors,
                         Collection<NamedStyles> styles) {
        this.classpath = classpath;
        this.dependsOn = dependsOn;
        this.relaxedClassTypeMatching = relaxedClassTypeMatching;
        this.styles = styles;

        this.context = new Context();
        this.compilerLog = new ResettableLog(context);
        this.pfm = new ByteArrayCapableJavacFileManager(context, true, charset, classBytesClasspath);

        // otherwise, consecutive string literals in binary expressions are concatenated by the parser, losing the original
        // structure of the expression!
        Options.instance(context).put("allowStringFolding", "false");
        Options.instance(context).put("compilePolicy", "attr");

        // JavaCompiler line 452 (call to ImplicitSourcePolicy.decode(..))
        Options.instance(context).put("-implicit", "none");

        // https://docs.oracle.com/en/java/javacard/3.1/guide/setting-java-compiler-options.html
        Options.instance(context).put("-g", "-g");
        Options.instance(context).put("-proc", "none");

        // MUST be created ahead of compiler construction
        new TimedTodo(context);

        // MUST be created (registered with the context) after pfm and compilerLog
        compiler = new JavaCompiler(context);

        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position
        // for every tree element
        compiler.genEndPos = true;

        // we don't need either of these, so as a minor performance improvement, omit these compiler features
        compiler.keepComments = false;
        compiler.lineDebugInfo = false;

        compilerLog.setWriters(new PrintWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) {
                if (logCompilationWarningsAndErrors) {
                    String log = new String(Arrays.copyOfRange(cbuf, off, len));
                    if (!log.isBlank()) {
                        org.slf4j.LoggerFactory.getLogger(Java11Parser.class).warn(log);
                    }
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        }));

        compileDependencies();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<J.CompilationUnit> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        if (classpath != null) { // override classpath
            if (context.get(JavaFileManager.class) != pfm) {
                throw new IllegalStateException("JavaFileManager has been forked unexpectedly");
            }

            try {
                pfm.setLocationFromPaths(StandardLocation.CLASS_PATH, new ArrayList<>(classpath));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        LinkedHashMap<Input, JCTree.JCCompilationUnit> cus = new LinkedHashMap<>();
        for (Input input1 : acceptedInputs(sourceFiles)) {
            cus.put(input1, MetricsHelper.successTags(
                    Timer.builder("rewrite.parse")
                            .description("The time spent by the JDK in parsing and tokenizing the source file")
                            .tag("file.type", "Java")
                            .tag("step", "(1) JDK parsing"))
                    .register(Metrics.globalRegistry)
                    .record(() -> {
                        try {
                            return compiler.parse(new Java11ParserInputFileObject(input1));
                        } catch (IllegalStateException e) {
                            if (e.getMessage().equals("endPosTable already set")) {
                                throw new IllegalStateException("Call reset() on JavaParser before parsing another" +
                                        "set of source files that have some of the same fully qualified names", e);
                            }
                            throw e;
                        }
                    }));
        }

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
            ctx.getOnError().accept(new JavaParsingException("Failed symbol entering or attribution", t));
        }

        Map<String, JavaType.FullyQualified> sharedClassTypes = new HashMap<>();
        return cus.entrySet().stream()
                .map(cuByPath -> {
                    Timer.Sample sample = Timer.start();
                    Input input = cuByPath.getKey();
                    try {
                        Java11ParserVisitor parser = new Java11ParserVisitor(
                                input.getRelativePath(relativeTo),
                                StringUtils.readFully(input.getSource()),
                                relaxedClassTypeMatching,
                                styles,
                                sharedClassTypes,
                                ctx
                        );

                        J.CompilationUnit cu = (J.CompilationUnit) parser.scan(cuByPath.getValue(), Space.EMPTY);
                        sample.stop(MetricsHelper.successTags(
                                Timer.builder("rewrite.parse")
                                        .description("The time spent mapping the OpenJDK AST to Rewrite's AST")
                                        .tag("file.type", "Java")
                                        .tag("step", "(3) Map to Rewrite AST"))
                                .register(Metrics.globalRegistry));
                        return cu;
                    } catch (Throwable t) {
                        sample.stop(MetricsHelper.errorTags(
                                Timer.builder("rewrite.parse")
                                        .description("The time spent mapping the OpenJDK AST to Rewrite's AST")
                                        .tag("file.type", "Java")
                                        .tag("step", "(3) Map to Rewrite AST"), t)
                                .register(Metrics.globalRegistry));

                        ctx.getOnError().accept(t);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    @Override
    public Java11Parser reset() {
        compilerLog.reset();
        pfm.flush();
        Check.instance(context).newRound();
        Annotate.instance(context).newRound();
        Enter.instance(context).newRound();
        Modules.instance(context).newRound();
        compileDependencies();
        return this;
    }

    public void setClasspath(Collection<Path> classpath) {
        this.classpath = classpath;
    }

    private void compileDependencies() {
        if (dependsOn != null) {
            parseInputs(dependsOn, null, new InMemoryExecutionContext());
        }
        Modules.instance(context).newRound();
    }


    /**
     * Initialize modules
     */
    private void initModules(Collection<JCTree.JCCompilationUnit> cus) {
        Modules modules = Modules.instance(context);
        // Creating a new round is necessary for multiple pass parsing, where we want to keep the symbol table from a
        // previous parse intact
        modules.newRound();
        modules.initModules(com.sun.tools.javac.util.List.from(cus));
    }

    /**
     * Enter symbol definitions into each compilation unit's scope
     */
    private void enterAll(Collection<JCTree.JCCompilationUnit> cus) {
        Enter enter = Enter.instance(context);
        com.sun.tools.javac.util.List<JCTree.JCCompilationUnit> compilationUnits = com.sun.tools.javac.util.List.from(
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

    private static class TimedTodo extends Todo {
        @Nullable
        private Timer.Sample sample;

        private TimedTodo(Context context) {
            super(context);
        }

        @Override
        public boolean isEmpty() {
            if (sample != null) {
                sample.stop(MetricsHelper.successTags(
                        Timer.builder("rewrite.parse")
                                .description("The time spent by the JDK in type attributing the source file")
                                .tag("file.type", "Java")
                                .tag("step", "(2) Type attribution"))
                        .register(Metrics.globalRegistry));
            }
            return super.isEmpty();
        }

        @Override
        public Env<AttrContext> remove() {
            this.sample = Timer.start();
            return super.remove();
        }
    }

    public static class Builder extends JavaParser.Builder<Java11Parser, Builder> {
        @Override
        public Java11Parser build() {
            return new Java11Parser(classpath, classBytesClasspath, dependsOn, charset, relaxedClassTypeMatching,
                    logCompilationWarningsAndErrors, styles);
        }
    }

    private static class ByteArrayCapableJavacFileManager extends JavacFileManager {
        private final List<PackageAwareJavaFileObject> classByteClasspath;

        public ByteArrayCapableJavacFileManager(Context context,
                                                boolean register,
                                                Charset charset,
                                                Collection<byte[]> classByteClasspath) {
            super(context, register, charset);
            this.classByteClasspath = classByteClasspath.stream()
                    .map(PackageAwareJavaFileObject::new)
                    .collect(toList());
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            if (file instanceof PackageAwareJavaFileObject) {
                return ((PackageAwareJavaFileObject) file).getClassName();
            }
            return super.inferBinaryName(location, file);
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
            if (StandardLocation.CLASS_PATH.equals(location)) {
                Iterable<JavaFileObject> listed = super.list(location, packageName, kinds, recurse);
                return Stream.concat(
                        classByteClasspath.stream()
                                .filter(jfo -> jfo.getPackage().equals(packageName)),
                        StreamSupport.stream(listed.spliterator(), false)
                ).collect(toList());
            }
            return super.list(location, packageName, kinds, recurse);
        }
    }

    private static class PackageAwareJavaFileObject extends SimpleJavaFileObject {
        private final String pkg;
        private final String className;
        private final byte[] classBytes;

        private PackageAwareJavaFileObject(byte[] classBytes) {
            super(URI.create("dontCare"), Kind.CLASS);

            AtomicReference<String> pkgRef = new AtomicReference<>();
            AtomicReference<String> nameRef = new AtomicReference<>();

            ClassReader classReader = new ClassReader(classBytes);
            classReader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    if(name.contains("/")) {
                        pkgRef.set(name.substring(0, name.lastIndexOf('/'))
                                .replace('/', '.'));
                        nameRef.set(name.substring(name.lastIndexOf('/') + 1));
                    } else {
                        pkgRef.set(name);
                        nameRef.set(name);
                    }
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);

            this.pkg = pkgRef.get();
            this.className = nameRef.get();
            this.classBytes = classBytes;
        }

        public String getPackage() {
            return pkg;
        }

        public String getClassName() {
            return className;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(classBytes);
        }
    }
}
