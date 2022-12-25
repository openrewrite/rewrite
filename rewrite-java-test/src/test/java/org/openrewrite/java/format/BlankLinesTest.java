/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.BlankLinesStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;

class BlankLinesTest implements RewriteTest {

    private static Consumer<RecipeSpec> blankLines() {
        return blankLines(style -> style);
    }

    private static Consumer<RecipeSpec> blankLines(UnaryOperator<BlankLinesStyle> with) {
        return spec -> spec.recipe(new BlankLines())
          .parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(
              randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(IntelliJ.blankLines()))
            )
          )));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/621")
    @Test
    void leaveTrailingComments() {
        rewriteRun(
          blankLines(),
          java(
            """
              public class A {
                  private Long id; // this comment will move to wrong place
                            
                  public Long id() {
                      return id;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/620")
    @Test
    void noBlankLineForFirstEnum() {
        rewriteRun(
          blankLines(),
          java(
            """
              public enum TheEnum {
                  FIRST,
                  SECOND
              }
              """
          )
        );
    }

    @Test
    void eachMethodOnItsOwnLine() {
        rewriteRun(
          blankLines(),
          java(
            """
              public class Test {
                  void a() {
                  }    void b() {
                  }
              }
              """,
            """
              public class Test {
                  void a() {
                  }
                            
                  void b() {
                  }
              }
              """
          )
        );
    }

    @Test
    void keepMaximumInDeclarations() {
        rewriteRun(
          blankLines(style -> style.withKeepMaximum(style.getKeepMaximum().withInDeclarations(0))),
          java(
            """
              import java.util.List;
                            
                            
              public class Test {
                            
                            
                  private int field1;
                  private int field2;
                            
                  {
                      field1 = 2;
                  }
                            
                  public void test1() {
                      new Runnable() {
                          public void run() {
                          }
                      };
                  }
                            
                  public class InnerClass {
                  }
              
                  public enum InnerEnum {
              
                      FIRST,
                      SECOND
                  }
              }
              """,
            """
              import java.util.List;
                            
              public class Test {
                  private int field1;
                  private int field2;
                            
                  {
                      field1 = 2;
                  }
                            
                  public void test1() {
                      new Runnable() {
                          public void run() {
                          }
                      };
                  }
                            
                  public class InnerClass {
                  }
              
                  public enum InnerEnum {
                      FIRST,
                      SECOND
                  }
              }
              """
          )
        );
    }

    @Test
    void keepMaximumInCode() {
        rewriteRun(
          blankLines(style -> style.withKeepMaximum(style.getKeepMaximum().withInCode(0))),
          java(
            """
              public class Test {
                  private int field1;
                            
                  {
              
              
                      field1 = 2;
                  }
              }
              """,
            """
              public class Test {
                  private int field1;
                            
                  {
                      field1 = 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void keepMaximumBeforeEndOfBlock() {
        rewriteRun(
          blankLines(style -> style.withKeepMaximum(style.getKeepMaximum().withBeforeEndOfBlock(0))),
          java(
            """
              public class Test {
                  private int field1;
                            
                  {
                      field1 = 2;
              
              
                  }
              
                  enum Test {
                      FIRST,
                      SECOND
              
                  }
              }
              """,
            """
              public class Test {
                  private int field1;
                            
                  {
                      field1 = 2;
                  }
              
                  enum Test {
                      FIRST,
                      SECOND
                  }
              }
              """
          )
        );
    }

    @Test
    void keepMaximumBetweenHeaderAndPackage() {
        rewriteRun(
          blankLines(style -> style.withKeepMaximum(style.getKeepMaximum().withBetweenHeaderAndPackage(0))),
          java(
            """
              /*
               * This is a sample file.
               */
              
              package com.intellij.samples;
              
              public class Test {
              }
              """,
            """
              /*
               * This is a sample file.
               */
              package com.intellij.samples;
              
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumPackageWithComment() {
        rewriteRun(
          blankLines(style -> style
            .withKeepMaximum(style.getKeepMaximum().withBeforeEndOfBlock(0))
            .withMinimum(style.getMinimum().withBeforePackage(1))),
          java(
            """
              /*
               * This is a sample file.
               */
              package com.intellij.samples;
              
              public class Test {
              }
              """,
            """
              /*
               * This is a sample file.
               */
              
              package com.intellij.samples;
              
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumBeforePackage() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withBeforePackage(1))),
          java(
            """
              
              package com.intellij.samples;
              
              public class Test {
              }
              """,
            """
              package com.intellij.samples;
              
              public class Test {
              }
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void minimumBeforePackageWithComment() {
        rewriteRun(
          blankLines(style -> style
            .withKeepMaximum(style.getKeepMaximum().withBetweenHeaderAndPackage(0))
            .withMinimum(style.getMinimum().withBeforePackage(1))), // this takes precedence over the "keep max"
          java(
            """
              /* Comment */
              package com.intellij.samples;
              
              public class Test {
              }
              """,
            """
              /* Comment */
              
              package com.intellij.samples;
              
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumBeforeImportsWithPackage() {
        rewriteRun(
          // no blank lines if nothing preceding package
          blankLines(style -> style.withMinimum(style.getMinimum().withBeforeImports(1))),
          java(
            """
              package com.intellij.samples;
              import java.util.Vector;
              
              public class Test {
              }
              """,
            """
              package com.intellij.samples;
              
              import java.util.Vector;
              
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumBeforeImports() {
        rewriteRun(
          // no blank lines if nothing preceding package
          blankLines(style -> style.withMinimum(style.getMinimum().withBeforeImports(1))),
          java(
            """
              
              import java.util.Vector;
              
              public class Test {
              }
              """,
            """
              import java.util.Vector;
              
              public class Test {
              }
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void minimumBeforeImportsWithComment() {
        rewriteRun(
          // no blank lines if nothing preceding package
          blankLines(style -> style.withMinimum(style.getMinimum().withBeforeImports(1))),
          java(
            """
              /*
               * This is a sample file.
               */
              import java.util.Vector;
              
              public class Test {
              }
              """,
            """
              /*
               * This is a sample file.
               */
              
              import java.util.Vector;
              
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumAfterPackageWithImport() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum()
            .withBeforeImports(0)
            .withAfterPackage(1))),
          java(
            """
              package com.intellij.samples;
              import java.util.Vector;
              
              public class Test {
              }
              """,
            """
              package com.intellij.samples;
              
              import java.util.Vector;
              
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumAfterPackage() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAfterPackage(1))),
          java(
            """
              package com.intellij.samples;
              public class Test {
              }
              """,
            """
              package com.intellij.samples;
              
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumAfterImports() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAfterImports(1))),
          java(
            """
              import java.util.Vector;
              public class Test {
              }
              """,
            """
              import java.util.Vector;
              
              public class Test {
              }
              """
          )
        );
    }

    @Disabled
    @Test
    void noImportsNoPackage() {
        rewriteRun(
          java(
            "\nclass Test {}",
            "class Test {}",
            spec -> spec
              .afterRecipe(cu -> assertThat(cu.getClasses().get(0).getPrefix().getWhitespace()).isEmpty())
              .noTrim()
          )
        );
    }

    @Test
    void minimumAroundClass() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAroundClass(2))),
          java(
            """
              import java.util.Vector;
              
              public class Test {
              }
              
              class Test2 {
                  class InnerClass0 {
                  }
                  class InnerClass1 {
                  }
              }
              """,
            """
              import java.util.Vector;
              
              public class Test {
              }
              
              
              class Test2 {
                  class InnerClass0 {
                  }
              
              
                  class InnerClass1 {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1171")
    @Test
    void minimumAroundClassNestedEnum() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAroundClass(2))),
          java(
            """
              enum OuterEnum {
                  FIRST,
                  SECOND
              }
              
              class OuterClass {
                  enum InnerEnum0 {
                      FIRST,
                      SECOND
                  }
                  enum InnerEnum1 {
                      FIRST,
                      SECOND
                  }
              }
              """,
            """
              enum OuterEnum {
                  FIRST,
                  SECOND
              }
              
              
              class OuterClass {
                  enum InnerEnum0 {
                      FIRST,
                      SECOND
                  }
              
              
                  enum InnerEnum1 {
                      FIRST,
                      SECOND
                  }
              }
              """
          )
        );
    }

    @Test
    void minimumAfterClassHeader() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAfterClassHeader(1))),
          java(
            """
              public class Test {
                  private int field1;
              }
              """,
            """
              public class Test {
              
                  private int field1;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1171")
    @Test
    void minimumAfterClassHeaderNestedClasses() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAfterClassHeader(1))),
          java(
            """
              class OuterClass {
                  class InnerClass0 {
                      private int unused = 0;
                  }
              
                  class InnerClass1 {
                      private int unused = 0;
                  }
              }
              """,
            """
              class OuterClass {
              
                  class InnerClass0 {
              
                      private int unused = 0;
                  }
              
                  class InnerClass1 {
              
                      private int unused = 0;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1171")
    @Test
    void minimumAfterClassHeaderNestedEnum() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAfterClassHeader(1))),
          java(
            """
              class OuterClass {
                  enum InnerEnum0 {
                      FIRST,
                      SECOND
                  }
              }
              """,
            """
              class OuterClass {
              
                  enum InnerEnum0 {
                      FIRST,
                      SECOND
                  }
              }
              """
          )
        );
    }

    @Test
    void minimumBeforeClassEnd() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withBeforeClassEnd(1))),
          java(
            """
              public class Test {
              }
              """,
            """
              public class Test {
              
              }
              """
          )
        );
    }

    @Test
    void minimumAfterAnonymousClassHeader() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAfterAnonymousClassHeader(1))),
          java(
            """
              public class Test {
                  public void test1() {
                      new Runnable() {
                          public void run() {
                          }
                      };
                  }
              }
              """,
            """
              public class Test {
                  public void test1() {
                      new Runnable() {
              
                          public void run() {
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void minimumAroundFieldInInterface() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAroundFieldInInterface(1))),
          java(
            """
              interface TestInterface {
                  int MAX = 10;
                  int MIN = 1;
              }
              """,
            """
              interface TestInterface {
                  int MAX = 10;
              
                  int MIN = 1;
              }
              """
          )
        );
    }

    @Test
    void minimumAroundField() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAroundField(1))),
          java(
            """
              class Test {
                  int max = 10;
                  int min = 1;
              }
              """,
            """
              class Test {
                  int max = 10;
              
                  int min = 1;
              }
              """
          )
        );
    }

    @Test
    void minimumAroundMethodInInterface() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAroundMethodInInterface(1))),
          java(
            """
              interface TestInterface {
                  void method1();
                  void method2();
              }
              """,
            """
              interface TestInterface {
                  void method1();
              
                  void method2();
              }
              """
          )
        );
    }

    @Test
    void minimumAroundMethod() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAroundMethod(1))),
          java(
            """
              class Test {
                  void method1() {}
                  void method2() {}
              }
              """,
            """
              class Test {
                  void method1() {}
              
                  void method2() {}
              }
              """
          )
        );
    }

    @Test
    void beforeMethodBody() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withBeforeMethodBody(1))),
          java(
            """
              class Test {
                  void method1() {}
              
                  void method2() {
                      int n = 0;
                  }
              }
              """,
            """
              class Test {
                  void method1() {
              
                  }
              
                  void method2() {
              
                      int n = 0;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"EmptyClassInitializer", "ClassInitializerMayBeStatic"})
    @Test
    void aroundInitializer() {
        rewriteRun(
          blankLines(style -> style.withMinimum(style.getMinimum().withAroundInitializer(1))),
          java(
            """
              public class Test {
                  private int field1;
                  {
                      field1 = 2;
                  }
                  private int field2;
              }
              public enum Enum {
                  A, B;
                  {
              
                  }
                  private int field;
              }
              """,
            """
              public class Test {
                  private int field1;
              
                  {
                      field1 = 2;
                  }
              
                  private int field2;
              }
                            
              public enum Enum {
                  A, B;
              
                  {
              
                  }
              
                  private int field;
              }
              """
          )
        );
    }

    @Test
    void unchanged() {
        rewriteRun(
          blankLines(),
          java(
            """
              package com.intellij.samples;
              
              public class Test {
              }
              """
          )
        );
    }
}
