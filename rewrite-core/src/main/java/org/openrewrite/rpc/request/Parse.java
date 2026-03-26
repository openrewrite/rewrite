/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc.request;

import com.fasterxml.jackson.annotation.JsonValue;
import io.moderne.jsonrpc.JsonRpcMethod;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Value
public class Parse implements RpcRequest {

    List<Input> inputs;

    @Nullable
    String relativeTo;

    public interface Input {
        Path getSourcePath();
    }

    @Value
    public static class StringInput implements Input {
        String text;
        Path sourcePath;
    }

    @Value
    public static class PathInput implements Input {
        @JsonValue
        Path sourcePath;
    }

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<Parse> {
        private final Map<String, Object> localObjects;
        private final Supplier<List<Parser>> parsers;

        @Override
        protected Object handle(Parse request) {
            @Nullable Path relativeTo = request.getRelativeTo() != null
                    ? Paths.get(request.getRelativeTo()) : null;

            List<String> ids = new ArrayList<>();
            for (Input input : request.getInputs()) {
                Parser parser = findParser(input.getSourcePath());
                Parser.Input parserInput = toParserInput(input);
                SourceFile sourceFile = parser.parseInputs(
                        Collections.singletonList(parserInput), relativeTo, new InMemoryExecutionContext()
                ).findFirst().orElseThrow(() ->
                        new IllegalStateException("Parser returned no results for " + input.getSourcePath()));
                String id = sourceFile.getId().toString();
                localObjects.put(id, sourceFile);
                ids.add(id);
            }

            ParseResponse response = new ParseResponse();
            response.addAll(ids);
            return response;
        }

        private Parser findParser(Path sourcePath) {
            for (Parser parser : parsers.get()) {
                if (parser.accept(sourcePath)) {
                    return parser;
                }
            }
            throw new IllegalArgumentException("No parser accepts " + sourcePath);
        }

        private static Parser.Input toParserInput(Input input) {
            if (input instanceof StringInput) {
                StringInput si = (StringInput) input;
                return new Parser.Input(
                        si.getSourcePath(),
                        () -> new ByteArrayInputStream(si.getText().getBytes(StandardCharsets.UTF_8))
                );
            } else if (input instanceof PathInput) {
                return new Parser.Input(((PathInput) input).getSourcePath(), null);
            }
            throw new IllegalArgumentException("Unknown input type: " + input.getClass());
        }
    }
}
