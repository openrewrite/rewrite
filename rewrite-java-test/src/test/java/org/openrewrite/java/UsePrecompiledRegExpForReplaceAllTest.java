/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Pattern;

import static org.openrewrite.java.Assertions.java;

class UsePrecompiledRegExpForReplaceAllTest implements RewriteTest {

    @Test
    void replaceSimpleVar() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          class A {
              public void replace(){
                  String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String changed = init.replaceAll("/[@]/g,", "_");
              }
          }
          """, """
          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPattern = Pattern.compile("/[@]/g,");
              public void replace(){
                  String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String changed = openRewriteReplaceAllPattern.matcher(init).replaceAll("_");
              }
          }
          """));
    }

    @Test
    void replaceMultipleReplaceAllOccurrences() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          class A {
              public void replace(){
                  String firstReplace = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String firstChanged = firstReplace.replaceAll("/[@]/g,", "_");
                  
                  String secondReplace = "Some other subject";
                  String secondChanged = secondReplace.replaceAll("\\s", "_");
              }
          }
          """, """
          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPattern1 = Pattern.compile("\\s");
              private static final java.util.regex.Pattern openRewriteReplaceAllPattern = Pattern.compile("/[@]/g,");
              public void replace(){
                  String firstReplace = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String firstChanged = openRewriteReplaceAllPattern.matcher(firstReplace).replaceAll("_");

                  String secondReplace = "Some other subject";
                  String secondChanged = openRewriteReplaceAllPattern1.matcher(secondReplace).replaceAll("_");
              }
          }
          """));
    }

    @Test
    void replaceMultipleReplaceAllOccurrencesAtDifferentLocations() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          class A {
              public void replace(){
                  String firstReplace = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String firstChanged = firstReplace.replaceAll("/[@]/g,", "_");
              }

              public void otherMethod(){
                  String secondReplace = "Some other subject";
                  String secondChanged = secondReplace.replaceAll("\\s", "_");
              }
          }
          """, """
          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPattern1 = Pattern.compile("\\s");
              private static final java.util.regex.Pattern openRewriteReplaceAllPattern = Pattern.compile("/[@]/g,");
              public void replace(){
                  String firstReplace = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String firstChanged = openRewriteReplaceAllPattern.matcher(firstReplace).replaceAll("_");
              }

              public void otherMethod(){
                  String secondReplace = "Some other subject";
                  String secondChanged = openRewriteReplaceAllPattern1.matcher(secondReplace).replaceAll("_");
              }
          }
          """));
    }

    @Test
    void replaceReplaceAllWithinAStreamMapLambda() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          import java.util.List;
          import java.util.stream.Stream;

          class A {
              public void replace(){
                  final var strings = Stream.of("One", "Two", "Three", "Four");
                  final var result = strings.map(value -> value.replaceAll("/[@]/g,", "_")).toList();
              }
          }
          """, """
          import java.util.List;
          import java.util.stream.Stream;

          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPattern = Pattern.compile("/[@]/g,");
              public void replace(){
                  final var strings = Stream.of("One", "Two", "Three", "Four");
                  final var result = strings.map(value -> openRewriteReplaceAllPattern.matcher(value).replaceAll("_")).toList();
              }
          }
          """));
    }

    @Test
    void replaceReplaceAllWithinAOptionMapLambda() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          import java.util.Optional;

          class A {
              public void replace(){
                  final var optionalString = Optional.of("Bob is a Bird... Bob is a Plane... Bob is Superman!");
                  final var result = optionalString.map(value -> value.replaceAll("/[@]/g,", "_")).orElse("");
              }
          }
          """, """
          import java.util.Optional;

          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPattern = Pattern.compile("/[@]/g,");
              public void replace(){
                  final var optionalString = Optional.of("Bob is a Bird... Bob is a Plane... Bob is Superman!");
                  final var result = optionalString.map(value -> openRewriteReplaceAllPattern.matcher(value).replaceAll("_")).orElse("");
              }
          }
          """));
    }

    @Test
    void replaceReplaceAllWithinAnonymousInnerClass() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          import java.lang.reflect.InvocationTargetException;

          class A {
              public void replace() {
                  String outerInit = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String outerChanged = outerInit.replaceAll("\\\\.\\\\.\\\\.", " <>");
                  
                  System.out.println(outerChanged);
              
                  final Object anonymousInner = new Object() {
                      public void doReplace() {
                          String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                          String changed = init.replaceAll("\\\\.\\\\.\\\\.", " -");
                          
                          System.out.println(changed);
                      }
                  };
                  
                  try {
                      anonymousInner.getClass().getMethod("doReplace").invoke(anonymousInner);
                  }
                  catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                      e.printStackTrace();
                  }
              }
          }
          """, """
            import java.lang.reflect.InvocationTargetException;
            
            class A {
                private static final java.util.regex.Pattern openRewriteReplaceAllPattern1 = Pattern.compile("\\\\.\\\\.\\\\.");
                private static final java.util.regex.Pattern openRewriteReplaceAllPattern = Pattern.compile("\\\\.\\\\.\\\\.");
                public void replace() {
                    String outerInit = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                    String outerChanged = openRewriteReplaceAllPattern.matcher(outerInit).replaceAll(" <>");
         
                    System.out.println(outerChanged);

                    final Object anonymousInner = new Object() {
                        public void doReplace() {
                            String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                            String changed = openRewriteReplaceAllPattern1.matcher(init).replaceAll(" -");
                            
                            System.out.println(changed);
                        }
                    };
                    
                    try {
                        anonymousInner.getClass().getMethod("doReplace").invoke(anonymousInner);
                    }
                    catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
            """));
    }

    @Test
    void replaceReplaceAllWithinInnerClass() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          class A {
              public void replace() {
                  String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String changed = init.replaceAll("\\\\.\\\\.\\\\.", " <>");
                         
                  System.out.println(changed);
              }
              
              class B {
                  public void replace() {
                      String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                      String changed = init.replaceAll("\\\\.\\\\.\\\\.", " -");
                             
                      System.out.println(changed);
                  }
              }
          }
          """, """
          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPattern = Pattern.compile("\\\\.\\\\.\\\\.");
              public void replace() {
                  String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String changed = openRewriteReplaceAllPattern.matcher(init).replaceAll(" <>");
              
                  System.out.println(changed);
              }
              
              class B {
                  private static final java.util.regex.Pattern openRewriteReplaceAllPattern1 = Pattern.compile("\\\\.\\\\.\\\\.");
                  public void replace() {
                      String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                      String changed = openRewriteReplaceAllPattern1.matcher(init).replaceAll(" -");
              
                      System.out.println(changed);
                  }
              }
          }
            """));
    }

    @Test
    void replaceReplaceAllWithinStaticInnerClass() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          class A {
              public void replace() {
                  String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String changed = init.replaceAll("\\\\.\\\\.\\\\.", " <>");
                         
                  System.out.println(changed);
              }
              
              static class B {
                  public void replace() {
                      String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                      String changed = init.replaceAll("\\\\.\\\\.\\\\.", " -");
                             
                      System.out.println(changed);
                  }
              }
          }
          """, """
          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPattern = Pattern.compile("\\\\.\\\\.\\\\.");
              public void replace() {
                  String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String changed = openRewriteReplaceAllPattern.matcher(init).replaceAll(" <>");
              
                  System.out.println(changed);
              }

              static class B {
                  private static final java.util.regex.Pattern openRewriteReplaceAllPattern1 = Pattern.compile("\\\\.\\\\.\\\\.");
                  public void replace() {
                      String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                      String changed = openRewriteReplaceAllPattern1.matcher(init).replaceAll(" -");
              
                      System.out.println(changed);
                  }
              }
          }
            """));
    }
}
