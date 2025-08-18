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

import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.lombok.LombokSupport;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.Processor;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.sun.tools.javac.util.List.nil;
import static java.util.stream.Collectors.toList;

class ReloadableJava8Parser implements JavaParser {
    private final JavaTypeCache typeCache;

    @Nullable
    private Collection<Path> classpath;

    @Nullable
    private final Collection<Input> dependsOn;

    private final JavacFileManager pfm;

    private final Context context;
    private final JavaCompiler compiler;
    private final ResettableLog compilerLog;
    private final Collection<NamedStyles> styles;
    private final List<Processor> annotationProcessors;

    ReloadableJava8Parser(@Nullable Collection<Path> classpath,
                          Collection<byte[]> classBytesClasspath,
                          @Nullable Collection<Input> dependsOn,
                          Charset charset,
                          boolean logCompilationWarningsAndErrors,
                          Collection<NamedStyles> styles,
                          JavaTypeCache typeCache) {
        this.classpath = classpath;
        this.dependsOn = dependsOn;
        this.styles = styles;
        this.typeCache = typeCache;

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
        Options.instance(context).put("-parameters", "true");

        // Ensure type attribution continues despite errors in individual files or nodes.
        // If an error occurs in a single file or node, type attribution should still proceed
        // for all other source files and unaffected nodes within the same file.
        Options.instance(context).put("should-stop.ifError", "GENERATE");

        annotationProcessors = new ArrayList<>(1);
        if (System.getenv().getOrDefault("REWRITE_LOMBOK", System.getProperty("rewrite.lombok")) != null &&
            classpath != null && classpath.stream().anyMatch(it -> it.toString().contains("lombok"))) {
            try {
                Processor lombokProcessor = LombokSupport.createLombokProcessor(getClass().getClassLoader());
                if (lombokProcessor != null) {
                    Options.instance(context).put(Option.PROCESSOR, "lombok.launch.AnnotationProcessorHider$AnnotationProcessor");
                    annotationProcessors.add(lombokProcessor);
                }
            } catch (ReflectiveOperationException ignore) {
                // Lombok was not found or could not be initialized
            }
        }

        // MUST be created (registered with the context) after pfm and compilerLog
        compiler = new JavaCompiler(context);

        // otherwise, the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position
        // for every tree element
        compiler.genEndPos = true;

        compiler.keepComments = true;

        // we don't need this, so as a minor performance improvement, omit these compiler features
        compiler.lineDebugInfo = false;

        compilerLog.setWriters(new PrintWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) {
                if (logCompilationWarningsAndErrors) {
                    String log = new String(Arrays.copyOfRange(cbuf, off, len));
                    if (!StringUtils.isBlank(log)) {
                        LoggerFactory.getLogger(ReloadableJava8Parser.class).warn(log);
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

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        LinkedHashMap<Input, JCTree.JCCompilationUnit> cus = parseInputsToCompilerAst(sourceFiles, ctx);
        return cus.entrySet().stream().map(cuByPath -> {
            Input input = cuByPath.getKey();
            parsingListener.startedParsing(input);
            try {
                ReloadableJava8ParserVisitor parser = new ReloadableJava8ParserVisitor(
                        input.getRelativePath(relativeTo),
                        input.getFileAttributes(),
                        input.getSource(ctx),
                        styles,
                        typeCache,
                        ctx,
                        context);
                J.CompilationUnit cu = (J.CompilationUnit) parser.scan(cuByPath.getValue(), Space.EMPTY);
                //noinspection DataFlowIssue
                cuByPath.setValue(null); // allow memory used by this JCCompilationUnit to be released
                parsingListener.parsed(input, cu);
                return requirePrintEqualsInput(cu, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    LinkedHashMap<Input, JCTree.JCCompilationUnit> parseInputsToCompilerAst(Iterable<Input> sourceFiles, ExecutionContext ctx) {
        if (classpath != null) { // override classpath
            // Lombok is expected to replace the file manager with its own, so we need to check for that
            if (context.get(JavaFileManager.class) != pfm && (annotationProcessors.isEmpty() || !(context.get(JavaFileManager.class) instanceof ForwardingJavaFileManager))) {
                throw new IllegalStateException("JavaFileManager has been forked unexpectedly");
            }

            try {
                pfm.setLocation(StandardLocation.CLASS_PATH, classpath.stream().map(Path::toFile).collect(toList()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        LinkedHashMap<Input, JCTree.JCCompilationUnit> cus = new LinkedHashMap<>();
        List<Java8ParserInputFileObject> inputFileObjects = acceptedInputs(sourceFiles)
                .map(input -> new Java8ParserInputFileObject(input, ctx))
                .collect(toList());
        if (!annotationProcessors.isEmpty()) {
            compiler.initProcessAnnotations(annotationProcessors);
        }
        try {
            //noinspection unchecked
            com.sun.tools.javac.util.List<JCTree.JCCompilationUnit> jcCompilationUnits = com.sun.tools.javac.util.List.from(
                    inputFileObjects.stream()
                            .map(input -> compiler.parse(input))
                            .toArray(JCTree.JCCompilationUnit[]::new));
            for (int i = 0; i < inputFileObjects.size(); i++) {
                cus.put(inputFileObjects.get(i).getInput(), jcCompilationUnits.get(i));
            }
            try {
                enterAll(cus.values());
                JavaCompiler delegate = annotationProcessors.isEmpty() ? compiler : compiler.processAnnotations(jcCompilationUnits, nil());
                while (!delegate.todo.isEmpty()) {
                    try {
                        delegate.attribute(delegate.todo);
                    } catch (Throwable t) {
                        handleParsingException(ctx, t);
                    }
                }
            } catch (Throwable t) {
                handleParsingException(ctx, t);
            }
        } catch (IllegalStateException e) {
            if ("endPosTable already set".equals(e.getMessage())) {
                throw new IllegalStateException(
                        "Call reset() on JavaParser before parsing another set of source files that " +
                                "have some of the same fully qualified names.", e);
            }
            throw e;
        }

        return cus;
    }

    private void handleParsingException(ExecutionContext ctx, Throwable t) {
        // when symbol entering fails on problems like missing types, attribution can often times proceed
        // unhindered, but it sometimes cannot (so attribution is always best-effort in the presence of errors)
        ctx.getOnError().accept(new JavaParsingException("Failed symbol entering or attribution", t));
    }

    @Override
    public ReloadableJava8Parser reset() {
        typeCache.clear();
        compilerLog.reset();
        pfm.flush();
        compileDependencies();
        return this;
    }

    @Override
    public JavaParser reset(Collection<URI> uris) {
        if (!uris.isEmpty()) {
            compilerLog.reset(uris);
        }
        pfm.flush();
        return this;
    }

    @Override
    public void setClasspath(Collection<Path> classpath) {
        this.classpath = classpath;
    }

    private void compileDependencies() {
        if (dependsOn != null) {
            InMemoryExecutionContext ctx = new InMemoryExecutionContext();
            ctx.putMessage("org.openrewrite.java.skipSourceSetMarker", true);
            parseInputs(dependsOn, null, ctx);
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

        public void reset(Collection<URI> uris) {
            for (Iterator<JavaFileObject> itr = sourceMap.keySet().iterator(); itr.hasNext(); ) {
                JavaFileObject f = itr.next();
                if (uris.contains(f.toUri())) {
                    itr.remove();
                }
            }
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
            if (StandardLocation.CLASS_PATH == location) {
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
        @Getter
        private final String className;
        private final byte[] classBytes;

        private PackageAwareJavaFileObject(byte[] classBytes) {
            super(URI.create("file:///.byteArray"), Kind.CLASS);

            AtomicReference<String> pkgRef = new AtomicReference<>();
            AtomicReference<String> nameRef = new AtomicReference<>();

            ClassReader classReader = new ClassReader(classBytes);
            classReader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    if (name.contains("/")) {
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

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(classBytes);
        }
    }
}
