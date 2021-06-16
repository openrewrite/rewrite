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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.java.tree.Space;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

class ReloadableJava8Parser implements JavaParser {
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

    ReloadableJava8Parser(@Nullable Collection<Path> classpath,
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
        context.put(JavaFileManager.class, this.pfm);

        // otherwise, consecutive string literals in binary expressions are concatenated by the parser, losing the original
        // structure of the expression!
        Options.instance(context).put("allowStringFolding", "false");
        Options.instance(context).put("compilePolicy", "attr");

        // JavaCompiler line 452 (call to ImplicitSourcePolicy.decode(..))
        Options.instance(context).put("-implicit", "none");

        // https://docs.oracle.com/en/java/javacard/3.1/guide/setting-java-compiler-options.html
        Options.instance(context).put("-g", "-g");
        Options.instance(context).put("-proc", "none");

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
                String log = new String(Arrays.copyOfRange(cbuf, off, len));
                if (logCompilationWarningsAndErrors && !StringUtils.isBlank(log)) {
                    org.slf4j.LoggerFactory.getLogger(ReloadableJava8Parser.class).warn(log);
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

    @Override
    public List<J.CompilationUnit> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
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

        LinkedHashMap<Input, JCTree.JCCompilationUnit> cus = acceptedInputs(sourceFiles).stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        input -> MetricsHelper.successTags(
                                Timer.builder("rewrite.parse")
                                        .description("The time spent by the JDK in parsing and tokenizing the source file")
                                        .tag("file.type", "Java")
                                        .tag("step", "(1) JDK parsing"))
                                .register(Metrics.globalRegistry)
                                .record(() -> {
                                    try {
                                        return compiler.parse(new Java8ParserInputFileObject(input));
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
            enterAll(cus.values());
            compiler.attribute(new TimedTodo(compiler.todo));
        } catch (Throwable t) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always a BEST EFFORT in the presence of errors)
            ctx.getOnError().accept(new JavaParsingException("Failed symbol entering or attribution", t));
        }

        Map<String, JavaType.Class> sharedClassTypes = new HashMap<>();
        return cus.entrySet().stream()
                .map(cuByPath -> {
                    Timer.Sample sample = Timer.start();
                    Input input = cuByPath.getKey();
                    try {
                        ReloadableJava8ParserVisitor parser = new ReloadableJava8ParserVisitor(
                                input.getRelativePath(relativeTo),
                                StringUtils.readFully(input.getSource()),
                                relaxedClassTypeMatching,
                                styles,
                                sharedClassTypes,
                                ctx);
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
    public ReloadableJava8Parser reset() {
        compilerLog.reset();
        pfm.flush();
        Check.instance(context).compiled.clear();
        return this;
    }

    @Override
    public void setClasspath(Collection<Path> classpath) {
        this.classpath = classpath;
    }

    private void compileDependencies() {
        if (dependsOn != null) {
            parseInputs(dependsOn, null, new InMemoryExecutionContext());
        }
        Check.instance(context).compiled.clear();
    }

    /**
     * Enter symbol definitions into each compilation unit's scope
     */
    private void enterAll(Collection<JCTree.JCCompilationUnit> cus) {
        Enter enter = Enter.instance(context);
        com.sun.tools.javac.util.List<JCTree.JCCompilationUnit> compilationUnits = com.sun.tools.javac.util.List.from(
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

    private static class TimedTodo extends Todo {
        private final Todo todo;
        private @Nullable Timer.Sample sample;

        private TimedTodo(Todo todo) {
            super(new Context());
            this.todo = todo;
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
            return todo.isEmpty();
        }

        @Override
        public Env<AttrContext> remove() {
            this.sample = Timer.start();
            return todo.remove();
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
        public boolean isSameFile(FileObject fileObject, FileObject fileObject1) {
            return fileObject.equals(fileObject1);
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
