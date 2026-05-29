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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.GolangParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the {@code Print}-path deserialization corruption observed in
 * {@code mod run} output (missing whitespace, operators rendered as {@code ?}).
 * <p>
 * Why a plain parse-then-print round trip does <em>not</em> catch this: the Go
 * RPC server <strong>caches</strong> the tree it parsed (keyed by source-file
 * id). When Java sends an unmodified tree back for {@code Print}, every node
 * arrives as {@code NO_CHANGE}, so the Go receiver returns its own correct
 * cached node and never exercises the {@code ADD}/{@code CHANGE} deserialization
 * path. A recipe (or any tree mutation) forces real deserialization, which is
 * what surfaces the bug in {@code mod run}.
 * <p>
 * These tests reproduce it without a recipe by calling {@link GoRewriteRpc#reset()}
 * between parse and print. {@code reset()} clears the cache on <em>both</em> sides
 * (Java's {@code remoteObjects}/{@code localObjects} and the Go server via the
 * {@code Reset} RPC), so the subsequent {@code Print} must fully deserialize the
 * tree from the wire — the same path a recipe edit takes.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GoRpcPrintRoundTripIntegTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log"))
                .traceRpcMessages());
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    /**
     * Parse {@code source}, drop both sides' caches, then print it back through
     * the Go printer. With the caches cleared the print must reconstruct the
     * tree entirely from the RPC wire, so any field the receiver drops shows up
     * as a difference here.
     */
    private void assertPrintsUnchangedAfterReset(String source) {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        SourceFile cu = GolangParser.builder().build()
                .parse(source).findFirst().orElseThrow();

        assertThat(cu).as("parse must yield a Go.CompilationUnit, not a ParseError: %s", cu)
                .isInstanceOf(org.openrewrite.golang.tree.Go.CompilationUnit.class);

        // Force the Print path to deserialize the whole tree (no NO_CHANGE shortcut).
        rpc.reset();

        String printed = rpc.print(cu);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void logicalAndOrInCondition() {
        assertPrintsUnchangedAfterReset(
                "package main\n" +
                "\n" +
                "func check(a error, b error) bool {\n" +
                "\tif a != nil && b != nil {\n" +
                "\t\treturn true\n" +
                "\t}\n" +
                "\tif a != nil || b != nil {\n" +
                "\t\treturn true\n" +
                "\t}\n" +
                "\treturn false\n" +
                "}\n");
    }

    @Test
    void blockBodyStatementsKeepWhitespace() {
        assertPrintsUnchangedAfterReset(
                "package main\n" +
                "\n" +
                "func sum(n int) int {\n" +
                "\ts := 0\n" +
                "\tfor i := 0; i < n; i++ {\n" +
                "\t\ts += i\n" +
                "\t}\n" +
                "\treturn s\n" +
                "}\n");
    }

    @Test
    void stringConcatAndCompoundAssign() {
        assertPrintsUnchangedAfterReset(
                "package main\n" +
                "\n" +
                "func build(parts []string) string {\n" +
                "\ts := \"\"\n" +
                "\tfor i := 0; i < len(parts); i++ {\n" +
                "\t\ts += \" \" + parts[i]\n" +
                "\t}\n" +
                "\treturn s\n" +
                "}\n");
    }

    @Test
    void goUnaryOperators() {
        assertPrintsUnchangedAfterReset(
                "package main\n" +
                "\n" +
                "type T struct {\n" +
                "\tName string\n" +
                "}\n" +
                "\n" +
                "func use(ch chan int) *T {\n" +
                "\tp := &T{Name: \"x\"}\n" +
                "\tv := *p\n" +
                "\t_ = v\n" +
                "\t_ = <-ch\n" +
                "\treturn p\n" +
                "}\n");
    }

    @Test
    void typeSwitch() {
        assertPrintsUnchangedAfterReset(
                "package main\n\nfunc f(v any) string {\n\tswitch v.(type) {\n\tcase int:\n\t\treturn \"int\"\n\tcase string:\n\t\treturn \"string\"\n\tdefault:\n\t\treturn \"other\"\n\t}\n}\n");
    }

    @Test
    void selectStatement() {
        assertPrintsUnchangedAfterReset(
                "package main\n\nfunc f(c chan int) int {\n\tselect {\n\tcase v := <-c:\n\t\treturn v\n\tdefault:\n\t\treturn 0\n\t}\n}\n");
    }

    @Test
    void multipleAssignment() {
        assertPrintsUnchangedAfterReset(
                "package main\n\nfunc f() {\n\tx, y := 1, 2\n\tx, y = y, x\n\t_ = x\n\t_ = y\n}\n");
    }

    @Test
    void rangeKeyValue() {
        assertPrintsUnchangedAfterReset(
                "package main\n\nfunc total(items []int) int {\n\ts := 0\n\tfor i, v := range items {\n\t\ts += i + v\n\t}\n\treturn s\n}\n");
    }

    @Test
    void rangeKeyOnly() {
        assertPrintsUnchangedAfterReset(
                "package main\n\nfunc count(items []int) int {\n\tn := 0\n\tfor i := range items {\n\t\tn += i\n\t}\n\treturn n\n}\n");
    }

    @Test
    void rangeNoVariable() {
        assertPrintsUnchangedAfterReset(
                "package main\n\nfunc count(items []int) int {\n\tn := 0\n\tfor range items {\n\t\tn++\n\t}\n\treturn n\n}\n");
    }

    @Test
    void rangeWithAssign() {
        assertPrintsUnchangedAfterReset(
                "package main\n\nfunc f(items []int) int {\n\tvar i, v int\n\tfor i, v = range items {\n\t\t_ = i\n\t\t_ = v\n\t}\n\treturn v\n}\n");
    }

    @Test
    void goBitClearBinary() {
        assertPrintsUnchangedAfterReset(
                "package main\n" +
                "\n" +
                "func clear(a int, b int) int {\n" +
                "\treturn a &^ b\n" +
                "}\n");
    }

    @Test
    void goBitClearAssign() {
        assertPrintsUnchangedAfterReset(
                "package main\n" +
                "\n" +
                "func clear(a int, b int) int {\n" +
                "\ta &^= b\n" +
                "\treturn a\n" +
                "}\n");
    }

    @Test
    void variadicParameterAndCallSpread() {
        assertPrintsUnchangedAfterReset(
                "package main\n" +
                "\n" +
                "func sum(nums ...int) int {\n" +
                "\ts := 0\n" +
                "\tfor i := 0; i < len(nums); i++ {\n" +
                "\t\ts += nums[i]\n" +
                "\t}\n" +
                "\treturn s\n" +
                "}\n" +
                "\n" +
                "func wrap(nums []int) int {\n" +
                "\treturn sum(nums...)\n" +
                "}\n");
    }

    @Test
    void addressOfAndVariadicSpread() {
        assertPrintsUnchangedAfterReset(
                "package main\n" +
                "\n" +
                "type T struct {\n" +
                "\tName string\n" +
                "}\n" +
                "\n" +
                "func merge(dst []string, src []string) *T {\n" +
                "\tdst = append(dst, src...)\n" +
                "\treturn &T{Name: dst[0]}\n" +
                "}\n");
    }
}
