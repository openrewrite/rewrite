/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.ruby;

import com.dylibso.chicory.runtime.Memory;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.ruby.internal.NestingDepth;
import org.openrewrite.ruby.internal.PrismSource;
import org.openrewrite.ruby.tree.Rb;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;
import org.ruby_lang.prism.Loader;
import org.ruby_lang.prism.Nodes;
import org.ruby_lang.prism.ParseResult;
import org.ruby_lang.prism.ParsingOptions;
import org.ruby_lang.prism.wasm.Prism;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.stream.Stream;

public class RubyParser implements Parser {

    private static final Object PRISM_LOCK = new Object();

    /**
     * Booting the Prism WASM module costs about a second, so parses share one instance until one
     * leaves it unusable. The underlying parse is not reentrant, hence {@link #PRISM_LOCK}.
     */
    private static @Nullable StackGuardedPrism prism;

    /**
     * Why the module would not boot, so that every later parse fails at once rather than booting
     * it again.
     */
    private static @Nullable RuntimeException bootFailure;

    /**
     * Prism's WASM module keeps its C stack in the 64 KiB of linear memory just above its global
     * data, and nothing stops the stack at the bottom: some 45 levels of nesting run it down into
     * the globals that malloc and Prism rely on. That parse, or any later one on the instance, may
     * then return garbage, trap, or never return. So a source estimated to nest deeper than this
     * is not parsed at all.
     */
    private static final int MAX_NESTING = 40;

    /**
     * Catches the overruns {@link NestingDepth} does not foresee: the bottom of the stack is filled
     * with a pattern before each parse, and a parse that wrote into it fails.
     */
    private static final class StackGuardedPrism extends Prism {
        private static final int GUARD_BYTES = 4096;
        private static final byte GUARD = (byte) 0xA5;

        private final int stackLow = (int) exports._StackLow().getValue();

        @Override
        public byte[] parse(byte[] source, byte[] options) {
            Memory memory = exports.memory();
            memory.fill(GUARD, stackLow, stackLow + GUARD_BYTES);
            byte[] serialized = super.parse(source, options);
            for (byte b : memory.readBytes(stackLow, GUARD_BYTES)) {
                if (b != GUARD) {
                    throw new StackExhaustedException("Ruby source nests too deeply for Prism's 64 KiB WASM stack.");
                }
            }
            return serialized;
        }
    }

    /**
     * The source nests deeper than the Prism WASM module's stack allows.
     */
    public static class StackExhaustedException extends RuntimeException {
        StackExhaustedException(String message) {
            super(message);
        }
    }

    static ParseResult parse(Path path, PrismSource source) {
        int nesting = NestingDepth.of(source.getText());
        if (nesting > MAX_NESTING) {
            throw new StackExhaustedException("Ruby source nests about " + nesting + " levels deep. " +
                                              "Prism's 64 KiB WASM stack has room for about " + MAX_NESTING + ".");
        }
        byte[] options = ParsingOptions.serialize(
                path.toString().getBytes(StandardCharsets.UTF_8),
                1,
                source.getCharset().name().getBytes(StandardCharsets.UTF_8),
                false,
                EnumSet.noneOf(ParsingOptions.CommandLine.class),
                ParsingOptions.SyntaxVersion.LATEST,
                false,
                // Not a "main script": a shebang naming something other than ruby (`#!/usr/bin/env
                // rake`) is then just a comment rather than a file with no Ruby in it.
                false,
                // OpenRewrite is routinely handed fragments rather than whole scripts, so `next`,
                // `break` and `return` at the top level have to parse rather than error out.
                true,
                new ParsingOptions.Scope[0]);
        byte[] serialized;
        synchronized (PRISM_LOCK) {
            if (bootFailure != null) {
                throw bootFailure;
            }
            if (prism == null) {
                try {
                    prism = new StackGuardedPrism();
                } catch (RuntimeException | LinkageError e) {
                    bootFailure = new IllegalStateException("The Prism WASM module did not boot", e);
                    throw bootFailure;
                }
            }
            StackGuardedPrism instance = prism;
            try {
                serialized = instance.parse(source.getParseBytes(), options);
            } catch (Throwable t) {
                // A trap or a stack overrun leaves the module's memory in a state no later parse can trust.
                prism = null;
                try {
                    instance.close();
                } catch (Throwable e) {
                    t.addSuppressed(e);
                }
                throw t;
            }
        }
        ParseResult result = Loader.load(serialized);
        if (result.errors.length > 0) {
            throw new RubySyntaxException(result, source);
        }
        return result;
    }

    /**
     * Carries Prism's typed, recoverable errors with 1-based line numbers so that OpenRewrite's
     * {@link ParseError} machinery surfaces real diagnostics.
     */
    public static class RubySyntaxException extends RuntimeException {
        public RubySyntaxException(ParseResult result, PrismSource source) {
            super(describe(result, source));
        }

        private static String describe(ParseResult result, PrismSource source) {
            StringBuilder message = new StringBuilder("Ruby syntax error");
            for (ParseResult.Error error : result.errors) {
                Nodes.Source prismSource = result.source;
                message.append("\n  line ")
                        .append(prismSource == null ? "?" : prismSource.line(error.location.startOffset))
                        .append(", offset ").append(source.charOffset(error.location.startOffset))
                        .append(" [").append(error.type).append('/').append(error.level).append("] ")
                        .append(error.message);
            }
            return message.toString();
        }
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).map(input -> {
            parsingListener.startedParsing(input);
            Path path = input.getRelativePath(relativeTo);
            try {
                EncodingDetectingInputStream is = input.getSource(ctx);
                String text = is.readFully();
                Charset charset = is.getCharset();
                PrismSource source = new PrismSource(text, charset);

                ParseResult parseResult = parse(path, source);
                Rb.CompilationUnit cu = new RubyParserVisitor(path, input.getFileAttributes(),
                        source, is.isCharsetBomMarked()).visitProgram(parseResult);
                parsingListener.parsed(input, cu);
                return requirePrintEqualsInput(cu, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public Stream<SourceFile> parse(@Language("rb") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public boolean accept(Path path) {
        if (Files.isDirectory(path)) {
            return false;
        }
        String fileName = path.toFile().getName();
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        return "rb".equals(ext) ||
               "arb".equals(ext) ||
               "axlsx".equals(ext) ||
               "builder".equals(ext) ||
               "fcgi".equals(ext) ||
               "gemfile".equals(ext) ||
               "gemspec".equals(ext) ||
               "god".equals(ext) ||
               "jb".equals(ext) ||
               "jbuilder".equals(ext) ||
               "mspec".equals(ext) ||
               "opal".equals(ext) ||
               "pluginspec".equals(ext) ||
               "podspec".equals(ext) ||
               "rabl".equals(ext) ||
               "rake".equals(ext) ||
               "rbuild".equals(ext) ||
               "rbw".equals(ext) ||
               "rbx".equals(ext) ||
               "ru".equals(ext) ||
               "ruby".equals(ext) ||
               "schema".equals(ext) ||
               "spec".equals(ext) ||
               "thor".equals(ext) ||
               "watchr".equals(ext) ||
               ".irbrc".equals(fileName) ||
               ".pryrc".equals(fileName) ||
               ".simplecov".equals(fileName) ||
               "buildfile".equals(fileName) ||
               "Appraisals".equals(fileName) ||
               "Berksfile".equals(fileName) ||
               "Brewfile".equals(fileName) ||
               "Buildfile".equals(fileName) ||
               "Capfile".equals(fileName) ||
               "Cheffile".equals(fileName) ||
               "Dangerfile".equals(fileName) ||
               "Deliverfile".equals(fileName) ||
               fileName.endsWith("Fastfile") ||
               "Gemfile".equals(fileName) ||
               "Guardfile".equals(fileName) ||
               "Jarfile".equals(fileName) ||
               "Mavenfile".equals(fileName) ||
               "Podfile".equals(fileName) ||
               "Puppetfile".equals(fileName) ||
               "Rakefile".equals(fileName) ||
               "rakefile".equals(fileName) ||
               "Schemafile".equals(fileName) ||
               "Snapfile".equals(fileName) ||
               "Steepfile".equals(fileName) ||
               "Thorfile".equals(fileName) ||
               "Vagabondfile".equals(fileName) ||
               "Vagrantfile".equals(fileName);
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.rb");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        public Builder() {
            super(Rb.CompilationUnit.class);
        }

        @Override
        public RubyParser build() {
            return new RubyParser();
        }

        @Override
        public String getDslName() {
            return "ruby";
        }
    }
}
