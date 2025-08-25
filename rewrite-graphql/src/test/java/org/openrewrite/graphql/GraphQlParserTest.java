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
package org.openrewrite.graphql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphQlParserTest {
    
    private final GraphQlParser parser = GraphQlParser.builder().build();
    
    @ParameterizedTest
    @ValueSource(strings = {
        "schema.graphql",
        "query.gql",
        "types.graphqls",
        "/path/to/schema.graphql",
        "/path/to/query.gql",
        "/path/to/types.graphqls",
        "file.GRAPHQL",  // Case shouldn't matter on case-insensitive file systems
        "file.GQL",
        "file.GRAPHQLS"
    })
    void acceptsValidGraphQlFiles(String fileName) {
        Path path = Paths.get(fileName);
        assertTrue(parser.accept(path), "Should accept " + fileName);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "schema.json",
        "query.yaml",
        "types.xml",
        "graphql.txt",
        "schema.graphql.bak",
        "query.gql.old",
        "/path/to/not-graphql.txt"
    })
    void rejectsNonGraphQlFiles(String fileName) {
        Path path = Paths.get(fileName);
        assertFalse(parser.accept(path), "Should reject " + fileName);
    }
    
    @Test
    void sourcePathFromSourceText() {
        Path prefix = Paths.get("/test");
        Path result = parser.sourcePathFromSourceText(prefix, "type Query { hello: String }");
        assertTrue(result.toString().endsWith("file.graphql"));
        assertTrue(result.toString().startsWith("/test"));
    }
}