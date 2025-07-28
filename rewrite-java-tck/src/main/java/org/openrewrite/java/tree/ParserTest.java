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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

class ParserTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/pull/4914")
    @Test
    void parseString() throws IOException {
        // path needs to be resolvable from `rewrite-java-8` etc.
        Path targetFile = Paths.get("../rewrite-java-tck/src/main/java/org/openrewrite/java/tree/ParserTest.java");
        @SuppressWarnings("SimplifyStreamApiCallChains") List<SourceFile> ignore = JavaParser.fromJavaVersion()
          .build()
          .parse(new String(Files.readAllBytes(targetFile)))
          .collect(Collectors.toList());
    }
}
