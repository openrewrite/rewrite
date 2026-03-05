/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.csharp.rpc;

import lombok.Value;

import java.util.ArrayList;

/**
 * Response from ParseSolution RPC call.
 * Contains a list of parsed source file items with their IDs, types, and project paths.
 */
class ParseSolutionResponse extends ArrayList<ParseSolutionResponse.Item> {

    /**
     * A single parsed source file item.
     */
    @Value
    static class Item {
        /**
         * The object ID that can be used to retrieve the parsed source file.
         */
        String id;

        /**
         * The fully qualified class name of the source file type.
         * Example: org.openrewrite.csharp.tree.Cs$CompilationUnit
         */
        String sourceFileType;

        /**
         * The path of the project this file belongs to.
         */
        String projectPath;
    }
}
