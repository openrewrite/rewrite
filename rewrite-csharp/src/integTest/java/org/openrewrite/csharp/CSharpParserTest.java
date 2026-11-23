/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.csharp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.csharp.rpc.CSharpRewriteRpc;
import org.openrewrite.csharp.tree.Cs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * What {@link CSharpParser} does with a source before the native parser sees it.
 */
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class CSharpParserTest {

    @BeforeAll
    static void setUpFactory() {
        CSharpPrinterParityTest.setUpFactory();
    }

    @AfterEach
    void tearDown() {
        CSharpRewriteRpc.resetCurrent();
    }

    @AfterAll
    static void shutDown() {
        CSharpRewriteRpc.shutdownCurrent();
    }

    @Test
    void byteOrderMark() {
        Cs.CompilationUnit cu = parse("\uFEFFclass C { }\n");
        assertThat(cu.isCharsetBomMarked()).isTrue();
        assertThat(cu.printAll()).isEqualTo("\uFEFFclass C { }\n");
    }

    @Test
    void characterAcrossAReadBuffer() {
        // the three bytes of the euro sign begin on the last byte of a 4096 byte read
        String source = "// " + "x".repeat(4092) + "\u20AC\nclass C { }\n";
        assertThat(parse(source).printAll()).isEqualTo(source);
    }

    private static Cs.CompilationUnit parse(String source) {
        Path path = Paths.get("C.cs");
        return CSharpPrinterParityTest.parse(singletonMap(path, source)).get(path);
    }
}
