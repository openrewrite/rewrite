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
package org.openrewrite.ruby;

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.ruby.tree.Ruby;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

//public class RubyParser implements Parser<Ruby.CompilationUnit> {
//    @Override
//    public List<Ruby.CompilationUnit> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
//        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
//        return acceptedInputs(sourceFiles).stream()
//                .map(sourceFile -> {
//                    Path path = sourceFile.getRelativePath(relativeTo);
//                    try {
//                        EncodingDetectingInputStream is = sourceFile.getSource(ctx);
//                        String sourceStr = is.readFully();
//
//                        // FIXME implement me!
//                        Ruby.CompilationUnit document = null;
//                        parsingListener.parsed(sourceFile, document);
//                        return document;
//                    } catch (Throwable t) {
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
//    public List<Ruby.CompilationUnit> parse(@Language("ruby") String... sources) {
//        return parse(new InMemoryExecutionContext(), sources);
//    }
//
//    @Override
//    public boolean accept(Path path) {
//        String p = path.toString();
//        return p.endsWith(".rb");
//    }
//
//    @Override
//    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
//        return prefix.resolve("file.rb");
//    }
//
//    public static Builder builder() {
//        return new Builder();
//    }
//
//    public static class Builder extends Parser.Builder {
//
//        public Builder() {
//            super(Ruby.CompilationUnit.class);
//        }
//
//        @Override
//        public RubyParser build() {
//            return new RubyParser();
//        }
//
//        @Override
//        public String getDslName() {
//            return "ruby";
//        }
//    }
//}
