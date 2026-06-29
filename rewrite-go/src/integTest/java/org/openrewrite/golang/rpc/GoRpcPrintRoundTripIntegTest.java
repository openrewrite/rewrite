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
    void fixedSizeArrayTypeKeepsLength() {
        // Go.ArrayType carries the inline length `5` across the wire; if it were
        // dropped this would print `[]int`.
        assertPrintsUnchangedAfterReset(
                "package main\n" +
                "\n" +
                "func process(data [5]int) [3]string {\n" +
                "\treturn [3]string{}\n" +
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
    void chainedMethodCallInsideErrorCheckBlock() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func handle(err error) {
                \tif err != nil {
                \t\tlogger.Error().Err(err).Msg("Cannot retrieve data")
                \t}
                }
                """);
    }

    @Test
    void errorCheckBlockNestedInForLoop() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func handle(items []int, err error) {
                \tfor range items {
                \t\tif err != nil {
                \t\t\tlogger.Error().Err(err).Msg("Cannot retrieve data")
                \t\t}
                \t}
                }
                """);
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
    void multiValueReturnSurvivesDeserialization() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func retrieve() (int, error) {
                \treturn 0, nil
                }
                """);
    }

    @Test
    void ifWithInitClauseSurvivesDeserialization() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func f() {
                \tif err := g(); err != nil {
                \t} else {
                \t}
                }
                """);
    }

    @Test
    void switchWithInitClauseSurvivesDeserialization() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func f() {
                \tswitch x := g(); x {
                \tcase 1:
                \t}
                }
                """);
    }

    @Test
    void methodReceiverSurvivesDeserialization() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                type Service struct {
                }

                func (s *Service) Run() {
                }
                """);
    }

    @Test
    void valueReceiverWithParamsAndReturn() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                type Service struct {
                }

                func (s Service) Add(a int, b int) int {
                \treturn a + b
                }
                """);
    }

    @Test
    void receiverMethodWithMultiValueReturn() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                type Service struct {
                }

                func (s *Service) Run() (int, error) {
                \treturn 0, nil
                }
                """);
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

    /**
     * for-range headers carry up to two loop targets plus a {@code :=}/{@code =}
     * operator. These live in the {@code Variable} slot of J.ForEachLoop.Control
     * as a Go MultiAssignment (the {@code :=} vs {@code =} is a ShortVarDecl
     * marker), so the full head must survive the wire. Before that mapping the
     * Java side could hold only a single loop variable and always re-emitted
     * {@code :=}, so {@code for k, v = range} came back as {@code for k := range}.
     */
    @Test
    void forRangeKeylessSurvivesReset() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func f(items []int) {
                \tfor range items {
                \t}
                }
                """);
    }

    @Test
    void forRangeTwoVarsDefineSurvivesReset() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func f(items []int) {
                \tfor k, v := range items {
                \t\t_ = k
                \t\t_ = v
                \t}
                }
                """);
    }

    @Test
    void forRangeBlankKeySurvivesReset() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func f(items []int) {
                \tfor _, v := range items {
                \t\t_ = v
                \t}
                }
                """);
    }

    @Test
    void forRangeTwoVarsAssignSurvivesReset() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func f(items []int) {
                \tvar k, v int
                \tfor k, v = range items {
                \t}
                \t_ = k
                \t_ = v
                }
                """);
    }

    @Test
    void forRangeArbitraryLhsAssignSurvivesReset() {
        assertPrintsUnchangedAfterReset(
                """
                package main

                func f(items []int, m map[int]int) {
                \tvar v int
                \tfor m[0], v = range items {
                \t}
                \t_ = v
                }
                """);
    }
}
