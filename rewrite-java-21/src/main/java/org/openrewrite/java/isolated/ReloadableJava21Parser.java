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
package org.openrewrite.java.isolated;

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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaParsingException;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

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
@NullMarked
public class ReloadableJava21Parser implements JavaParser {
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

    private ReloadableJava21Parser(boolean logCompilationWarningsAndErrors,
                                   @Nullable Collection<Path> classpath,
                                   Collection<byte[]> classBytesClasspath,
                                   @Nullable Collection<Input> dependsOn,
                                   Charset charset,
                                   Collection<NamedStyles> styles,
                                   JavaTypeCache typeCache) {
        this.classpath = classpath;
        this.dependsOn = dependsOn;
        this.styles = styles;
        this.typeCache = typeCache;

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
                    if (!log.isBlank()) {
                        org.slf4j.LoggerFactory.getLogger(ReloadableJava21Parser.class).warn(log);
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
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        LinkedHashMap<Input, JCTree.JCCompilationUnit> cus = parseInputsToCompilerAst(sourceFiles, ctx);
        return cus.entrySet().stream().map(cuByPath -> {
            Input input = cuByPath.getKey();
            parsingListener.startedParsing(input);
            try {
                ReloadableJava21ParserVisitor parser = new ReloadableJava21ParserVisitor(
                        input.getRelativePath(relativeTo),
                        input.getFileAttributes(),
                        input.getSource(ctx),
                        styles,
                        typeCache,
                        ctx,
                        context
                );

                J.CompilationUnit cu = (J.CompilationUnit) parser.scan(cuByPath.getValue(), Space.EMPTY);
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
        acceptedInputs(sourceFiles).forEach(input1 -> {
            try {
                JCTree.JCCompilationUnit jcCompilationUnit = compiler.parse(new ReloadableJava21ParserInputFileObject(input1, ctx));
                cus.put(input1, jcCompilationUnit);
            } catch (IllegalStateException e) {
                if ("endPosTable already set".equals(e.getMessage())) {
                    throw new IllegalStateException(
                            "Call reset() on JavaParser before parsing another set of source files that " +
                            "have some of the same fully qualified names. Source file [" +
                            input1.getPath() + "]\n[\n" + StringUtils.readFully(input1.getSource(ctx), getCharset(ctx)) + "\n]", e);
                }
                throw e;
            }
        });

        try {
            initModules(cus.values());
            enterAll(cus.values());

            // For some reason this is necessary in JDK 9+, where the internal block counter that
            // annotationsBlocked() tests against remains >0 after attribution.
            Annotate annotate = Annotate.instance(context);
            while (annotate.annotationsBlocked()) {
                annotate.unblockAnnotations(); // also flushes once unblocked
            }

            compiler.attribute(compiler.todo);
        } catch (
                Throwable t) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always best-effort in the presence of errors)
            ctx.getOnError().accept(new JavaParsingException("Failed symbol entering or attribution", t));
        }
        return cus;
    }

    @Override
    public ReloadableJava21Parser reset() {
        typeCache.clear();
        compilerLog.reset();
        pfm.flush();
        Check.instance(context).newRound();
        Annotate.instance(context).newRound();
        Enter.instance(context).newRound();
        Modules.instance(context).newRound();
        compileDependencies();
        return this;
    }

    @Override
    public JavaParser reset(Collection<URI> uris) {
        if (!uris.isEmpty()) {
            compilerLog.reset(uris);
        }
        pfm.flush();
        Check.instance(context).newRound();
        Annotate.instance(context).newRound();
        Enter.instance(context).newRound();
        Modules.instance(context).newRound();
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

        public void reset(Collection<URI> uris) {
            sourceMap.keySet().removeIf(f -> uris.contains(f.toUri()));
        }
    }

    public static class Builder extends JavaParser.Builder<ReloadableJava21Parser, Builder> {
        @Override
        public ReloadableJava21Parser build() {
            return new ReloadableJava21Parser(logCompilationWarningsAndErrors, resolvedClasspath(), classBytesClasspath, dependsOn, charset, styles, javaTypeCache);
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
                return classByteClasspath.isEmpty() ? listed :
                        Stream.concat(classByteClasspath.stream()
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
                        pkgRef.set("");
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
