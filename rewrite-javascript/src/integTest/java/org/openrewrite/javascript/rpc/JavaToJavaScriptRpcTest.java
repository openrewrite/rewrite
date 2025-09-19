/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.javascript.rpc;

import org.junit.platform.suite.api.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.rpc.Trace;
import org.openrewrite.test.RecipeSpec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;

@Suite
@SuiteDisplayName("Java LSTs sent to and from a JavaScript Rewrite RPC server")
@SelectPackages("org.openrewrite.java.tree")
// If you need to narrow down the suite to debug one test
//@IncludeClassNamePatterns(".*AnnotationTest")
@ExcludeClassNamePatterns({
  ".*JavadocTest",
  ".*TypeUtilsTest",
  ".*RecordPatternMatchingTest",
  ".*SwitchPatternMatchingTest"
})
public class JavaToJavaScriptRpcTest {

    @BeforeSuite
    static void beforeSuite() {
        RecipeSpec.DEFAULTS = () -> new RecipeSpec()
          .recipe(toRecipe(() -> {
              try {
                  Trace.TRACE_SENDER = true;
                  Trace.TRACE_RECEIVER = new PrintStream(new FileOutputStream("rpc.java.log"));
                  JavaScriptRewriteRpc client = JavaScriptRewriteRpc.getOrStart();

                  assertThat(client.installRecipes(new File("rewrite/dist-fixtures/modify-all-trees.js")))
                    .isEqualTo(1);
                  Recipe modifyAll = client.prepareRecipe("org.openrewrite.java.test.modify-all-trees");

                  return new JavaVisitor<>() {
                      @Override
                      public J preVisit(J tree, ExecutionContext ctx) {
                          SourceFile t = (SourceFile) modifyAll.getVisitor().visitNonNull(tree, ctx);
                          try {
                              assertThat(t.printAll()).isEqualTo(((SourceFile) tree).printAll());
                              stopAfterPreVisit();
                              return tree;
                          } finally {
                              Trace.TRACE_RECEIVER.close();
                              client.shutdown();
                          }
                      }
                  };
              } catch (FileNotFoundException e) {
                  throw new RuntimeException(e);
              }
          }));
    }

    @AfterSuite
    static void afterSuite() {
        RecipeSpec.DEFAULTS = RecipeSpec::new;
        JavaScriptRewriteRpc.shutdownCurrent();
    }
}
