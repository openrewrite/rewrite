/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticallyEqualTest {

    @Test
    void elvisWithThrowIsNotEqualToThrow() {
        // given
        List<J.Try.Catch> catches = catches("""
          class ActiveLeaseExists : RuntimeException()
          fun record() {}
          fun f() {
              try {
                  record()
              } catch (e: IllegalStateException) {
                  record()
                  (e.cause as? ActiveLeaseExists)?.let { throw it } ?: throw e
              } catch (e: Exception) {
                  record()
                  throw e
              }
          }
          """);

        // when
        boolean forward = SemanticallyEqual.areEqual(catches.get(0).getBody(), catches.get(1).getBody());
        boolean backward = SemanticallyEqual.areEqual(catches.get(1).getBody(), catches.get(0).getBody());

        // then
        assertThat(forward).isFalse();
        assertThat(backward).isFalse();
    }

    @Test
    void elvisIsNotEqualToItsOperand() {
        // given
        List<J.Try.Catch> catches = catches("""
          fun f(a: String?) {
              try {
                  println(a)
              } catch (e: IllegalStateException) {
                  println(a ?: a)
              } catch (e: Exception) {
                  println(a)
              }
          }
          """);

        // when
        boolean equal = SemanticallyEqual.areEqual(catches.get(0).getBody(), catches.get(1).getBody());

        // then
        assertThat(equal).isFalse();
    }

    private static List<J.Try.Catch> catches(String source) {
        K.CompilationUnit cu = KotlinParser.builder().build()
          .parse(new InMemoryExecutionContext(), source)
          .findFirst()
          .map(K.CompilationUnit.class::cast)
          .orElseThrow();
        List<J.Try.Catch> catches = new KotlinIsoVisitor<List<J.Try.Catch>>() {
            @Override
            public J.Try.Catch visitCatch(J.Try.Catch _catch, List<J.Try.Catch> found) {
                found.add(_catch);
                return super.visitCatch(_catch, found);
            }
        }.reduce(cu, new ArrayList<>());
        assertThat(catches).hasSize(2);
        return catches;
    }
}
