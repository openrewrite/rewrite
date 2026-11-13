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
package org.openrewrite.golang;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GolangVisitorDispatchTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    private static Set<Class<?>> goClassesImplementing(Class<?> kind) {
        return Arrays.stream(Go.class.getDeclaredClasses())
                .filter(c -> !c.isInterface() && Go.class.isAssignableFrom(c) && kind.isAssignableFrom(c))
                .collect(toSet());
    }

    @Test
    void everyGoStatementAndExpressionReachesItsHook() {
        // given
        SourceFile cu = GolangParser.builder().build()
                .parse(new InMemoryExecutionContext(), SOURCE)
                .findFirst().orElseThrow();
        assertThat(cu).isInstanceOf(Go.CompilationUnit.class);
        Set<Class<?>> present = new HashSet<>();
        Set<Class<?>> statements = new HashSet<>();
        Set<Class<?>> expressions = new HashSet<>();

        // when
        new GolangVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                present.add(tree.getClass());
                return tree;
            }

            @Override
            public J visitStatement(Statement statement, Integer p) {
                statements.add(statement.getClass());
                return statement;
            }

            @Override
            public J visitExpression(Expression expression, Integer p) {
                expressions.add(expression.getClass());
                return expression;
            }
        }.visit(cu, 0);

        // then
        assertThat(present).as("the source holds every Go statement and expression")
                .containsAll(goClassesImplementing(Statement.class))
                .containsAll(goClassesImplementing(Expression.class));
        assertThat(statements).containsAll(goClassesImplementing(Statement.class));
        assertThat(expressions).containsAll(goClassesImplementing(Expression.class));
    }

    private static final String SOURCE = """
            package main

            type Number interface {
            	~int | ~float64
            }

            type Pair[K comparable, V any] struct {
            	Key K
            	Val V
            }

            type S struct{}

            var (
            	arr [3]int
            	ptrs map[string]*int
            	ch chan int
            	fn func(int) (int, error)
            )

            func (s *S) N() int { return 0 }

            func Map2[A, B any]() {}

            func (s *S) M(xs ...int) (int, error) {
            	p := Pair[string, int]{Key: "k", Val: 1}
            	_ = p
            	g := Map2[int, string]
            	_ = g
            	_ = [...]int{1, 2}
            	x, y := 1, 2
            	x, y = y, x
            	x &^= y
            	_ = x &^ y
            	_ = &x
            	_ = xs[1:2]
            	var i any = x
            	_ = i.(int)
            	(s.N())
            	f := func() {}
            	go f()
            	defer f()
            	ch <- 1
            	select {
            	case v := <-ch:
            		_ = v
            	}
            	if z := x; z > 0 {
            	}
            	switch x {
            	case 1:
            		fallthrough
            	default:
            	}
            	goto End
            End:
            	return x, nil
            }
            """;
}
