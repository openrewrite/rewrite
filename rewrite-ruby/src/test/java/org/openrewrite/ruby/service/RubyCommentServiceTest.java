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
package org.openrewrite.ruby.service;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.ruby.RubyIsoVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Comments;

import static org.openrewrite.ruby.Assertions.ruby;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * The language-agnostic {@code Comments} trait resolves a {@code CommentService} from the source
 * file; without a Ruby one it lands on the Java implementation, which throws when it asks Ruby for
 * an {@code AutoFormatService} and silently sees no comments at all.
 */
class RubyCommentServiceTest implements RewriteTest {

    @Test
    void addComment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new RubyIsoVisitor<ExecutionContext>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return (J.MethodInvocation) Comments.of(getCursor()).comment(" TODO");
              }
          })),
          ruby(
            """
              puts 'hi'
              """,
            """
              # TODO
              puts 'hi'
              """
          )
        );
    }

    @Test
    void removeComment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new RubyIsoVisitor<ExecutionContext>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return (J.MethodInvocation) Comments.of(getCursor()).removeComment(" TODO");
              }
          })),
          ruby(
            """
              puts 'first'
              # TODO
              puts 'hi'
              """,
            """
              puts 'first'
              puts 'hi'
              """
          )
        );
    }

    @Test
    void replaceBlockCommentBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new RubyIsoVisitor<ExecutionContext>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return (J.MethodInvocation) Comments.of(getCursor()).replaceComment("docs", "notes");
              }
          })),
          ruby(
            """
              puts 'first'
              =begin rdoc
              docs
              =end
              puts 'hi'
              """,
            """
              puts 'first'
              =begin rdoc
              notes
              =end
              puts 'hi'
              """
          )
        );
    }
}
