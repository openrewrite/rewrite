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
package org.openrewrite.toml;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

//public class TomlParser implements Parser<Toml.Document> {
//    @Override
//    public List<Toml.Document> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
//        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
//        return acceptedInputs(sourceFiles).stream()
//                .map(sourceFile -> {
//                    Timer.Builder timer = Timer.builder("rewrite.parse")
//                            .description("The time spent parsing an XML file")
//                            .tag("file.type", "XML");
//                    Timer.Sample sample = Timer.start();
//                    Path path = sourceFile.getRelativePath(relativeTo);
//                    try {
//                        EncodingDetectingInputStream is = sourceFile.getSource(ctx);
//                        String sourceStr = is.readFully();
//
//                        // FIXME implement me!
//                        Toml.Document document = null;
//
//                        sample.stop(MetricsHelper.successTags(timer).register(Metrics.globalRegistry));
//                        parsingListener.parsed(sourceFile, document);
//                        return document;
//                    } catch (Throwable t) {
//                        sample.stop(MetricsHelper.errorTags(timer, t).register(Metrics.globalRegistry));
//                        ParsingExecutionContextView.view(ctx).parseFailure(sourceFile, relativeTo, this, t);
//                        ctx.getOnError().accept(new IllegalStateException(path + " " + t.getMessage(), t));
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .collect(toList());
//    }
//
//    @Override
//    public List<Toml.Document> parse(@Language("xml") String... sources) {
//        return parse(new InMemoryExecutionContext(), sources);
//    }
//
//    @Override
//    public boolean accept(Path path) {
//        String p = path.toString();
//        return p.endsWith(".xml") ||
//               p.endsWith(".wsdl") ||
//               p.endsWith(".xhtml") ||
//               p.endsWith(".xsd") ||
//               p.endsWith(".xsl") ||
//               p.endsWith(".xslt") ||
//               p.endsWith(".tld");
//    }
//
//    @Override
//    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
//        return prefix.resolve("file.xml");
//    }
//
//    public static Builder builder() {
//        return new Builder();
//    }
//
//    public static class Builder extends Parser.Builder {
//
//        public Builder() {
//            super(Toml.Document.class);
//        }
//
//        @Override
//        public TomlParser build() {
//            return new TomlParser();
//        }
//
//        @Override
//        public String getDslName() {
//            return "toml";
//        }
//    }
//}
