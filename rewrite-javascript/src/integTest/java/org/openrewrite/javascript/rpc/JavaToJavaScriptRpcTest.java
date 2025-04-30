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

import org.jspecify.annotations.NonNull;
import org.junit.platform.suite.api.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;

@Suite
@SuiteDisplayName("Java LSTs sent to and from a JavaScript Rewrite RPC server")
@SelectPackages("org.openrewrite.java.tree")
//@IncludeClassNamePatterns(".*ProblemTest")
//@IncludeClassNamePatterns(".*TypeParameterAndWildcardTest")
@ExcludeClassNamePatterns({ ".*JavadocTest", ".*TypeUtilsTest"})
public class JavaToJavaScriptRpcTest {

    @BeforeSuite
    static void beforeSuite() {
        RecipeSpec.DEFAULTS = () -> new RecipeSpec()
          .recipe(toRecipe(() -> {
              try {
                  PrintStream log = new PrintStream(new FileOutputStream("rpc.java.log"));
                  JavaScriptRewriteRpc client = JavaScriptRewriteRpc.start(
                    Environment.builder().build(),
                    "/usr/local/bin/node",
                    "--enable-source-maps",
                    // Uncomment this to debug the server
//                  "--inspect-brk",
                    "./rewrite/dist/src/rpc/server.js"
                  );

                  client.batchSize(20)
                    .timeout(Duration.ofMinutes(10))
                    .traceGetObjectOutput()
                    .traceGetObjectInput(log);

                  assertThat(client.installRecipes(new File("rewrite/dist/test/modify-all-trees.js")))
                    .isEqualTo(1);
                  Recipe modifyAll = client.prepareRecipe("org.openrewrite.java.test.modify-all-trees");

                  return new JavaVisitor<>() {
                      @Override
                      public J preVisit(@NonNull J tree, @NonNull ExecutionContext ctx) {
                          SourceFile t = (SourceFile) modifyAll.getVisitor().visitNonNull(tree, ctx);
                          try {
                              assertThat(t.printAll()).isEqualTo(((SourceFile) tree).printAll());
                              stopAfterPreVisit();
                              return tree;
                          } finally {
                              log.close();
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
    }
}
