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
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

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
}
