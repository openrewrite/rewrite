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
public class JavaToJavaScriptRpcTest {
    private static JavaScriptRewriteRpc client;
    private static PrintStream log;

    @BeforeSuite
    static void beforeSuite() throws FileNotFoundException {
        log = new PrintStream(new FileOutputStream("rpc.java.log"));

        client = JavaScriptRewriteRpc.start(
          Environment.builder().build(),
          "/usr/local/bin/node",
          "--enable-source-maps",
          // Uncomment this to debug the server
//          "--inspect-brk",
          "./rewrite/dist/src/rpc/server.js"
        );

        client.batchSize(20)
          .timeout(Duration.ofMinutes(10))
          .traceGetObjectOutput()
          .traceGetObjectInput(log);

        assertThat(client.installRecipes(new File("rewrite/dist/test/modify-all-trees.js")))
          .isEqualTo(1);
        Recipe modifyAll = client.prepareRecipe("org.openrewrite.java.test.modify-all-trees");

        RecipeSpec.DEFAULTS = () -> new RecipeSpec()
          .recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J preVisit(@NonNull J tree, @NonNull ExecutionContext ctx) {
                  SourceFile t = (SourceFile) modifyAll.getVisitor().visitNonNull(tree, ctx);
                  assertThat(t.printAll()).isEqualTo(((SourceFile) tree).printAll());
                  stopAfterPreVisit();
                  return tree;
              }
          }));
    }

    @AfterSuite
    static void afterSuite() {
        log.close();
        client.shutdown();
        RecipeSpec.DEFAULTS = RecipeSpec::new;
    }
}
