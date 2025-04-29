package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ProblemTest implements RewriteTest {

    @Test
    void tryWithResources() {
        rewriteRun(
          java(
            """
              import java.io.*;
              class Test {
                  void test() {
                      File f = new File("file.txt");
                      try (FileInputStream fis = new FileInputStream(f)) {
                      }
                      catch(IOException ignored) {
                      }
                  }
              }
              """
          )
        );
    }
}
