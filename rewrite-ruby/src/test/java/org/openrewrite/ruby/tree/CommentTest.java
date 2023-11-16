package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class CommentTest implements RewriteTest {

    @Test
    void singleLine() {
        rewriteRun(
          ruby(
            """
              #!/usr/bin/ruby -w
              # This is a single line comment.
                            
              puts "Hello, Ruby!"
              """
          )
        );
    }

    @Test
    void trailingComment() {
        rewriteRun(
          ruby(
            """
              counter = 42    # keeps track times page has been hit
              """
          )
        );
    }

    @Test
    void multiLine() {
        rewriteRun(
          ruby(
            """
              #!/usr/bin/ruby -w
                                  
              puts("Hello, Ruby!")
                                 
              =begin
              This is a multiline comment and can span as many lines as you
              like. But =begin and =end should come in the first line only.\s
              =end
              """,
            spec -> spec.afterRecipe(cu -> {
                cu.printAllTrimmed();
            })
          )
        );
    }
}
