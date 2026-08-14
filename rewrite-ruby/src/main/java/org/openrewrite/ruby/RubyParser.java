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
import org.jruby.ParseResult;
import org.jruby.ast.RootNode;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.ruby.tree.Rb;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

public class RubyParser implements Parser {

    /**
     * Booting a JRuby runtime costs on the order of a second, so all parses share one. JRuby's
     * parser is not documented as thread-safe, hence the synchronization on the parse call.
     */
    private static final class Runtime {
        static final org.jruby.Ruby INSTANCE;

        static {
            // JRuby 10 still defaults to the legacy `org.jruby.ast` front end this parser is written
            // against, but the Prism provider is already registered as a service. Pin the choice
            // before the runtime reads the option, or a future default flip breaks parsing.
            System.setProperty("jruby.parser.prism", "false");
            // https://github.com/jruby/jruby/wiki/DirectJRubyEmbedding#user-content-Direct_Embedding
            INSTANCE = JavaEmbedUtils.initialize(Collections.emptyList());
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
                String source = is.readFully();
                Charset charset = is.getCharset();

                ParseResult parseResult;
                synchronized (Runtime.INSTANCE) {
                    parseResult = Runtime.INSTANCE.getParserManager().parseFile(path.toString(), 0,
                            new ByteArrayInputStream(source.getBytes(charset)), null);
                }
                if (!(parseResult instanceof RootNode)) {
                    throw new IllegalStateException("Expected JRuby's legacy parser to return an " +
                                                    "org.jruby.ast.RootNode, but it returned a " + parseResult.getClass().getName() +
                                                    ". Is the jruby.parser.prism system property set to true?");
                }

                Rb.CompilationUnit cu = new RubyParserVisitor(input.getPath(), input.getFileAttributes(),
                        source, charset, is.isCharsetBomMarked()).visitRootNode((RootNode) parseResult);
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
