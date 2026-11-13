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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.lang.reflect.Modifier;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class GolangVisitorTypeTest {

    @BeforeAll
    static void startEngine() throws Exception {
        BundledEngine.use();
    }

    @AfterAll
    static void stopEngine() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void visitsTheTypeOfEveryGoNode() {
        SourceFile cu = GolangParser.builder().build()
          .parse(new InMemoryExecutionContext(), TYPED_NODES)
          .findFirst().orElseThrow();

        List<Tree> typedNodes = new ArrayList<>();
        Set<Tree> typeVisited = Collections.newSetFromMap(new IdentityHashMap<>());
        new GolangVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                if (tree instanceof Go && declaresType(tree.getClass())) {
                    typedNodes.add(tree);
                }
                return tree;
            }

            @Override
            public @Nullable JavaType visitType(@Nullable JavaType javaType, Integer p) {
                typeVisited.add(getCursor().getValue());
                return javaType;
            }
        }.visit(cu, 0);

        assertThat(typedNodes).extracting(Object::getClass).containsAll(Arrays.stream(Go.class.getDeclaredClasses())
          .filter(GolangVisitorTypeTest::declaresType)
          .toList());
        assertThat(typedNodes).filteredOn(t -> !typeVisited.contains(t))
          .extracting(t -> t.getClass().getSimpleName())
          .isEmpty();
    }

    private static boolean declaresType(Class<?> c) {
        return Arrays.stream(c.getDeclaredFields())
          .anyMatch(f -> !Modifier.isStatic(f.getModifiers()) && JavaType.class.isAssignableFrom(f.getType()));
    }

    static final String TYPED_NODES = """
      package main

      type S struct{ a int }
      type I interface{ M() }
      type N interface{ int | string }

      func Pair[A, B any](a A, b B) {}

      func results() (int, error) { return 0, nil }

      func main() {
      	var arr [3]int
      	var p *int
      	var ch chan int
      	var m map[string]int
      	var fn func(int) int
      	s := S{a: 1}
      	var x interface{} = s
      	_ = x.(S)
      	v := <-ch
      	v &^= 1
      	arr2 := [...]int{1, 2}
      	pair := Pair[int, string]
      	_, _, _, _, _, _, _ = arr, p, m, fn, v, arr2, pair
      }
      """;
}
