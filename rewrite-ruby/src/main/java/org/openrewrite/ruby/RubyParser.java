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

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.EncodingDetectingInputStream;
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

    /**
     * Booting the Prism WASM module costs about a second, so all parses share one instance. The
     * underlying parse is not reentrant, hence the synchronization.
     */
    private static final class PrismHolder {
        static final Prism INSTANCE = new Prism();
    }

    static ParseResult parse(Path path, PrismSource source) {
        byte[] options = ParsingOptions.serialize(
                path.toString().getBytes(StandardCharsets.UTF_8),
                1,
                source.getCharset().name().getBytes(StandardCharsets.UTF_8),
                false,
                EnumSet.noneOf(ParsingOptions.CommandLine.class),
                ParsingOptions.SyntaxVersion.LATEST,
                false,
                true,
                // OpenRewrite is routinely handed fragments rather than whole scripts, so `next`,
                // `break` and `return` at the top level have to parse rather than error out.
                true,
                new ParsingOptions.Scope[0]);
        byte[] serialized;
        synchronized (PrismHolder.INSTANCE) {
            serialized = PrismHolder.INSTANCE.parse(source.getParseBytes(), options);
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
                Rb.CompilationUnit cu = new RubyParserVisitor(input.getPath(), input.getFileAttributes(),
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
        return ext.equals("rb") ||
               ext.equals("arb") ||
               ext.equals("axlsx") ||
               ext.equals("builder") ||
               ext.equals("fcgi") ||
               ext.equals("gemfile") ||
               ext.equals("gemspec") ||
               ext.equals("god") ||
               ext.equals("jb") ||
               ext.equals("jbuilder") ||
               ext.equals("mspec") ||
               ext.equals("opal") ||
               ext.equals("pluginspec") ||
               ext.equals("podspec") ||
               ext.equals("rabl") ||
               ext.equals("rake") ||
               ext.equals("rbuild") ||
               ext.equals("rbw") ||
               ext.equals("rbx") ||
               ext.equals("ru") ||
               ext.equals("ruby") ||
               ext.equals("schema") ||
               ext.equals("spec") ||
               ext.equals("thor") ||
               ext.equals("watchr") ||
               fileName.equals(".irbrc") ||
               fileName.equals(".pryrc") ||
               fileName.equals(".simplecov") ||
               fileName.equals("buildfile") ||
               fileName.equals("Appraisals") ||
               fileName.equals("Berksfile") ||
               fileName.equals("Brewfile") ||
               fileName.equals("Buildfile") ||
               fileName.equals("Capfile") ||
               fileName.equals("Cheffile") ||
               fileName.equals("Dangerfile") ||
               fileName.equals("Deliverfile") ||
               fileName.endsWith("Fastfile") ||
               fileName.equals("Gemfile") ||
               fileName.equals("Guardfile") ||
               fileName.equals("Jarfile") ||
               fileName.equals("Mavenfile") ||
               fileName.equals("Podfile") ||
               fileName.equals("Puppetfile") ||
               fileName.equals("Rakefile") ||
               fileName.equals("rakefile") ||
               fileName.equals("Schemafile") ||
               fileName.equals("Snapfile") ||
               fileName.equals("Steepfile") ||
               fileName.equals("Thorfile") ||
               fileName.equals("Vagabondfile") ||
               fileName.equals("Vagrantfile");
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
