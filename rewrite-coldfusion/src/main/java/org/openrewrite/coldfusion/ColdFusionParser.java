/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion;

import lombok.Getter;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.coldfusion.internal.ColdFusionParserVisitor;
import org.openrewrite.coldfusion.internal.ComponentIndex;
import org.openrewrite.coldfusion.internal.IndexTypeContext;
import org.openrewrite.coldfusion.internal.TypeAttribution;
import org.openrewrite.coldfusion.internal.grammar.CFMLLexer;
import org.openrewrite.coldfusion.internal.grammar.CFMLParser;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Parses ColdFusion templates ({@code .cfm}, {@code .cfml}) and components ({@code .cfc}), in tag or
 * script syntax.
 */
public class ColdFusionParser implements Parser {

    /**
     * The directories that the components files name but do not contain are in, by the logical path that reaches
     * them, as CFML's mappings have it.
     */
    private final Map<String, Path> mappings;

    /**
     * The ForgeBox packages the files depend on, as {@code slug@range}.
     */
    private final List<String> packages;

    /**
     * The index of each set of mappings in this JVM, since a parser is built per test and what a package's cached
     * version holds never changes. One whose directories change between parses is indexed each time it is built.
     */
    private static final Map<Map<String, Path>, SoftReference<ComponentIndex>> INDEXES = new ConcurrentHashMap<>();

    @Nullable
    private ComponentIndex dependencies;

    private final TypeInference.@Nullable Factory typeInference;

    public ColdFusionParser() {
        this(emptyMap(), emptyList(), null);
    }

    private ColdFusionParser(Map<String, Path> mappings, List<String> packages, TypeInference.@Nullable Factory typeInference) {
        this.mappings = mappings;
        this.packages = packages;
        this.typeInference = typeInference;
    }

    /**
     * Parses every file first, to learn what each component and template declares, so that every file's names, calls
     * and {@code new} expressions resolve to what they name. Each file is parsed again to type it, which keeps no more
     * than one file's tree in memory at a time. A {@link Builder#typeInference type inference} sees each file once more
     * between the two.
     */
    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        return parseInputs(sourceFiles, emptyList(), relativeTo, ctx);
    }

    /**
     * Parses and types {@code sourceFiles} with the components and templates of {@code indexed} in the index beside
     * theirs, so that what they name in those files resolves as if every file were parsed. The indexed files are parsed
     * only to index them: they are neither typed nor returned, and a type inference does not see them. A caller that
     * changed a few files of a repository types only those.
     */
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, Iterable<Input> indexed, @Nullable Path relativeTo,
                                          ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        List<Input> inputs = acceptedInputs(sourceFiles).collect(Collectors.toList());
        ComponentIndex index = new ComponentIndex(dependencies(ctx));
        if (relativeTo != null && relativeTo.getFileName() != null) {
            index.repository(relativeTo.getFileName().toString());
        }
        // in the order of their paths, so that what a file resolves to does not depend on which files are typed
        List<Input> all = Stream.concat(inputs.stream(), acceptedInputs(indexed))
                .sorted(Comparator.comparing(input -> input.getRelativePath(relativeTo).toString()))
                .collect(Collectors.toList());
        for (Input input : all) {
            Path path = input.getRelativePath(relativeTo);
            try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                index.add(path, parse(path, text(path, is), input.getFileAttributes(), is.getCharset(), is.isCharsetBomMarked()));
            } catch (Throwable ignored) {
                // a file that does not parse declares nothing, and its error is reported when it is parsed again
            }
        }
        index.link();
        TypeInference inference = infer(inputs, index, relativeTo, ctx);
        return inputs.stream().map(input -> {
            parsingListener.startedParsing(input);
            Path path = input.getRelativePath(relativeTo);
            try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                String source = text(path, is);
                Cf.CompilationUnit cu = TypeAttribution.attribute(
                        parse(path, source, input.getFileAttributes(), is.getCharset(), is.isCharsetBomMarked()), index);
                if (inference != null) {
                    try {
                        cu = inference.attribute(cu);
                    } catch (Throwable t) {
                        ctx.getOnError().accept(t);
                    }
                }
                parsingListener.parsed(input, cu);
                return requirePrintEqualsInput(cu, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    /**
     * The inference's first two steps, before the parser types the files to return them: each file parsed and typed
     * for {@link TypeInference#collect}, then {@link TypeInference#solve}. Null without one, or when it cannot solve.
     */
    private @Nullable TypeInference infer(List<Input> inputs, ComponentIndex index, @Nullable Path relativeTo,
                                          ExecutionContext ctx) {
        if (typeInference == null) {
            return null;
        }
        IndexTypeContext context = new IndexTypeContext(index);
        try {
            TypeInference inference = typeInference.create(context);
            for (Input input : inputs) {
                Path path = input.getRelativePath(relativeTo);
                Cf.CompilationUnit cu;
                try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                    cu = TypeAttribution.attribute(parse(path, text(path, is), input.getFileAttributes(), is.getCharset(),
                            is.isCharsetBomMarked()), index);
                } catch (Throwable ignored) {
                    // reported when the file is parsed again to be returned
                    continue;
                }
                try {
                    inference.collect(cu);
                } catch (Throwable t) {
                    ctx.getOnError().accept(t);
                }
            }
            context.solving(true);
            try {
                inference.solve();
            } finally {
                context.solving(false);
            }
            return inference;
        } catch (Throwable t) {
            ctx.getOnError().accept(t);
            return null;
        }
    }

    /**
     * The components the mappings and the ForgeBox packages reach, indexed once for every parse, since they do not
     * change between them. A package that does not resolve is an error the context hears of.
     */
    private synchronized @Nullable ComponentIndex dependencies(ExecutionContext ctx) {
        if (dependencies == null && (!mappings.isEmpty() || !packages.isEmpty())) {
            Map<String, Path> all = new LinkedHashMap<>(mappings);
            if (!packages.isEmpty()) {
                ForgeBoxPackages forgeBox = new ForgeBoxPackages(HttpSenderExecutionContextView.view(ctx).getHttpSender());
                for (String coordinate : packages) {
                    try {
                        ForgeBoxPackages.Installed installed = forgeBox.install(coordinate);
                        all.putIfAbsent(installed.getInstallPath(), installed.getDirectory());
                    } catch (RuntimeException e) {
                        ctx.getOnError().accept(new IllegalStateException("Unable to install " + coordinate + " from ForgeBox", e));
                    }
                }
                SoftReference<ComponentIndex> indexed = INDEXES.get(all);
                dependencies = indexed == null ? null : indexed.get();
                if (dependencies == null) {
                    dependencies = index(all);
                    INDEXES.put(all, new SoftReference<>(dependencies));
                }
            } else {
                dependencies = index(all);
            }
        }
        return dependencies;
    }

    /**
     * Indexes the components that mappings reach, each named by its logical path: with {@code testbox} mapped to a
     * directory, the directory's {@code system/BaseSpec.cfc} is {@code testbox.system.BaseSpec}.
     */
    static ComponentIndex index(Map<String, Path> mappings) {
        ComponentIndex index = new ComponentIndex(null);
        for (Map.Entry<String, Path> mapping : mappings.entrySet()) {
            String logicalPath = mapping.getKey().replace('\\', '/');
            while (logicalPath.startsWith("/")) {
                logicalPath = logicalPath.substring(1);
            }
            while (logicalPath.endsWith("/")) {
                logicalPath = logicalPath.substring(0, logicalPath.length() - 1);
            }
            Path directory = mapping.getValue();
            List<Path> files;
            try (Stream<Path> walk = Files.walk(directory)) {
                files = walk.filter(Files::isRegularFile)
                        .filter(file -> isCfml(file.toString().toLowerCase(Locale.ROOT)))
                        .sorted()
                        .collect(Collectors.toList());
            } catch (IOException | UncheckedIOException e) {
                // a mapping to a directory that cannot be read reaches no components
                continue;
            }
            for (Path file : files) {
                String relative = directory.relativize(file).toString().replace('\\', '/');
                Path logical = Paths.get(logicalPath.isEmpty() ? relative : logicalPath + "/" + relative);
                try (InputStream in = Files.newInputStream(file);
                     EncodingDetectingInputStream is = new EncodingDetectingInputStream(in)) {
                    index.add(logical, parse(logical, text(logical, is), null, is.getCharset(), is.isCharsetBomMarked()));
                } catch (Throwable ignored) {
                    // a component that does not parse declares nothing
                }
            }
        }
        index.link();
        return index;
    }

    /**
     * A file's text: its bytes after any byte order mark, decoded in the charset detected for them. Bytes that do not
     * decode to text that encodes back to them are no CFML, as a zip archive or a template cfencode encrypted is not.
     * Parsing one fails rather than printing other bytes back.
     */
    static String text(Path path, EncodingDetectingInputStream is) throws IOException {
        ByteArrayOutputStream read = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int n; (n = is.read(buffer, 0, buffer.length)) != -1; ) {
            read.write(buffer, 0, n);
        }
        byte[] bytes = read.toByteArray();
        Charset charset = is.getCharset();
        String text = new String(bytes, charset);
        if (!Arrays.equals(text.getBytes(charset), bytes)) {
            throw new ColdFusionParsingException("not text in " + charset.name(), path.toString());
        }
        return text;
    }

    static Cf.CompilationUnit parse(Path path, String source, @Nullable FileAttributes fileAttributes,
                                            Charset charset, boolean charsetBomMarked) {
        boolean script = isScriptComponent(path, source);
        ParserRuleContext tree;
        CFMLParser parser;
        try {
            // SLL prediction is faster and almost always enough
            parser = parser(path, source, script);
            parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
            parser.setErrorHandler(new BailErrorStrategy());
            tree = script ? parser.scriptComponent() : parser.template();
        } catch (ParseCancellationException e) {
            // lexed again, since the parser retypes the first word of each statement as it goes
            parser = parser(path, source, script);
            parser.addErrorListener(new ThrowingErrorListener(path, source));
            tree = script ? parser.scriptComponent() : parser.template();
        }
        List<Token> tokens = ((BufferedTokenStream) parser.getTokenStream()).getTokens();
        return (Cf.CompilationUnit) new ColdFusionParserVisitor(path, fileAttributes, source, charset, charsetBomMarked, tokens)
                .visit(tree);
    }

    private static CFMLParser parser(Path path, String source, boolean script) {
        CFMLLexer lexer = new CFMLLexer(CharStreams.fromString(source));
        if (script) {
            lexer.script();
        } else {
            lexer.template();
        }
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener(path, source));
        CFMLParser parser = new CFMLParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        return parser;
    }

    /**
     * A {@code .cfc} written in script: after any comments and imports, it begins with
     * {@code component} or {@code interface}.
     */
    static boolean isScriptComponent(Path path, String source) {
        if (!path.toString().toLowerCase(Locale.ROOT).endsWith(".cfc")) {
            return false;
        }
        int i = 0;
        int length = source.length();
        while (i < length) {
            char c = source.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000B' || c == '\u00A0' || c == '\uFEFF') {
                i++;
            } else if (source.startsWith("//", i)) {
                while (i < length && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
                    i++;
                }
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? length : end + 2;
            } else if (source.startsWith("<!---", i)) {
                int end = source.indexOf("--->", i + 5);
                i = end < 0 ? length : end + 4;
            } else if (source.regionMatches(true, i, "import", 0, 6) && i + 6 < length && !isIdentPart(source.charAt(i + 6))) {
                int end = source.indexOf(';', i);
                if (end < 0) {
                    return false;
                }
                i = end + 1;
            } else {
                break;
            }
        }
        for (String keyword : new String[]{"component", "interface", "abstract", "final"}) {
            if (source.regionMatches(true, i, keyword, 0, keyword.length()) &&
                (i + keyword.length() >= length || !isIdentPart(source.charAt(i + keyword.length())))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    @Override
    public Stream<SourceFile> parse(String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public boolean accept(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        return isCfml(name);
    }

    private static boolean isCfml(String lowerCaseName) {
        return lowerCaseName.endsWith(".cfm") || lowerCaseName.endsWith(".cfc") || lowerCaseName.endsWith(".cfml");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        String trimmed = sourceCode.trim();
        boolean component = isScriptComponent(Paths.get("Component.cfc"), sourceCode) ||
                            trimmed.regionMatches(true, 0, "<cfcomponent", 0, 12) ||
                            trimmed.regionMatches(true, 0, "<cfinterface", 0, 12);
        return prefix.resolve(component ? "Component.cfc" : "template.cfm");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        private final Map<String, Path> mappings = new LinkedHashMap<>();
        private final List<String> packages = new ArrayList<>();

        private TypeInference.@Nullable Factory typeInference;

        public Builder() {
            super(Cf.CompilationUnit.class);
        }

        /**
         * Types more of each repository than the parser's own rules do, with an inference the factory makes for each
         * call to {@link #parseInputs}. It costs one more parse of each file. Without one, the parser types what
         * declarations say and what agrees within a function.
         */
        public Builder typeInference(TypeInference.@Nullable Factory typeInference) {
            this.typeInference = typeInference;
            return this;
        }

        /**
         * Makes the components in a directory outside the files being parsed reachable by the names those files give
         * them, as a CFML mapping does. It is what a classpath is to Java: the parser types what the files call on
         * those components. With {@code mapping("testbox", directory)}, {@code extends="testbox.system.BaseSpec"}
         * names the directory's {@code system/BaseSpec.cfc}. A package CommandBox installs is a mapping at the path it
         * installs to.
         */
        public Builder mapping(String logicalPath, Path directory) {
            mappings.put(logicalPath, directory);
            return this;
        }

        public Builder mappings(Map<String, Path> mappings) {
            this.mappings.putAll(mappings);
            return this;
        }

        /**
         * The ForgeBox packages the files being parsed depend on, as {@code box.json} lists them: {@code slug} for the
         * newest release, or {@code slug@range}, as {@code coldbox@^7}. Each downloads once into
         * {@code ~/.rewrite/cache/forgebox} and is a mapping at the path CommandBox installs it to, so a test can
         * declare what its sources depend on as a Java test declares a classpath.
         */
        public Builder dependencies(String... packages) {
            Collections.addAll(this.packages, packages);
            return this;
        }

        @Override
        public ColdFusionParser build() {
            return new ColdFusionParser(new LinkedHashMap<>(mappings), new ArrayList<>(packages), typeInference);
        }

        @Override
        public String getDslName() {
            return "coldfusion";
        }
    }

    private static class ThrowingErrorListener extends BaseErrorListener {
        private final Path path;
        private final String source;

        ThrowingErrorListener(Path path, String source) {
            this.path = path;
            this.source = source;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, @Nullable Object offendingSymbol, int line, int charPositionInLine,
                                String msg, @Nullable RecognitionException e) {
            String what;
            int codePoint;
            if (offendingSymbol instanceof Token) {
                Token token = (Token) offendingSymbol;
                codePoint = token.getStartIndex();
                String text = token.getText().length() > 20 ? token.getText().substring(0, 20) + "..." : token.getText();
                what = token.getType() == Token.EOF ? "unexpected end of file" :
                        "unexpected '" + text.replace("\r", "\\r").replace("\n", "\\n") + "'";
                if (recognizer instanceof org.antlr.v4.runtime.Parser) {
                    List<String> rules = ((org.antlr.v4.runtime.Parser) recognizer).getRuleInvocationStack();
                    if (!rules.isEmpty()) {
                        what += " in " + rules.get(0);
                    }
                }
            } else {
                codePoint = e instanceof LexerNoViableAltException ? ((LexerNoViableAltException) e).getStartIndex() :
                        ((Lexer) recognizer).getCharIndex();
                what = "unexpected character";
            }
            int index = source.offsetByCodePoints(0, Math.min(codePoint, source.codePointCount(0, source.length())));
            throw new ColdFusionParsingException(what, ColdFusionParserVisitor.where(path, source, index));
        }
    }

    /**
     * CFML the parser has not been taught, or that is not CFML. The message names what was expected
     * or found, then where.
     */
    @Getter
    public static class ColdFusionParsingException extends RuntimeException {
        private final String what;

        public ColdFusionParsingException(String what, String where) {
            super(what + " at " + where);
            this.what = what;
        }
    }
}
