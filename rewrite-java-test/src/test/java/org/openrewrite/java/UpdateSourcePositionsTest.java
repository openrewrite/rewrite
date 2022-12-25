/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Range;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@Disabled
class UpdateSourcePositionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateSourcePositions());
    }

    private static String printWithLines(SourceFile sourceFile) {
        return sourceFile.printAll(new PrintOutputCapture<>(0, new PrintOutputCapture.MarkerPrinter() {
            @Override
            public String beforePrefix(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                if (marker instanceof Range) {
                    //noinspection PatternVariableCanBeUsed
                    Range r = (Range) marker;
                    return "[(" + r.getStart().getLine() + ", " + r.getStart().getColumn() + "), (" +
                           r.getEnd().getLine() + ", " + r.getEnd().getColumn() + ")]";
                }
                return "";
            }
        }));
    }

    @Test
    void lamdaParameter() {
        rewriteRun(
          java(
            """
              package org.test;
                            
              import java.util.function.Consumer;
                              
              public class Application {
                          
                  public Consumer<String> demo() {
                      return (args) -> {
                          log.info("");
                      };
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(printWithLines(cu)).isEqualTo(
              """
                [(1, 0), (13, 2)][(1, 0), (1, 16)]package [(1, 8), (1, 16)][(1, 8), (1, 11)]org.[(1, 12), (1, 16)]test;
                            
                [(3, 1), (3, 35)]import [(3, 8), (3, 35)][(3, 8), (3, 26)][(3, 8), (3, 17)][(3, 8), (3, 12)]java.[(3, 13), (3, 17)]util.[(3, 18), (3, 26)]function.[(3, 27), (3, 35)]Consumer;
                              
                [(5, 1), (13, 2)][(5, 1), (5, 7)]public class [(5, 14), (5, 25)]Application [(5, 26), (13, 2)]{
                              
                    [(7, 5), (11, 6)][(7, 5), (7, 11)]public [(7, 12), (7, 28)][(7, 12), (7, 20)]Consumer<[(7, 21), (7, 27)]String> [(7, 29), (7, 33)]demo([(7, 34), (7, 34)]) [(7, 36), (11, 6)]{
                        [(8, 9), (10, 10)]return [(8, 16), (10, 10)]([(8, 17), (8, 21)][(8, 17), (8, 21)][(8, 17), (8, 21)]args) -> [(8, 26), (10, 10)]{
                            [(9, 13), (9, 25)][(9, 13), (9, 16)]log.[(9, 17), (9, 21)]info([(9, 22), (9, 24)]"");
                        };
                    }
                }
                """
            ))
          )
        );
    }

    @Test
    void updateSourcePositions() {
        rewriteRun(
          java(
            """ 
              class Test {
                  int n;
                  
                  void test() {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(printWithLines(cu)).isEqualTo(
              """
                [(1, 0), (6, 2)][(1, 0), (6, 2)]class [(1, 6), (1, 10)]Test [(1, 11), (6, 2)]{
                    [(2, 5), (2, 10)][(2, 5), (2, 8)]int [(2, 9), (2, 10)][(2, 9), (2, 10)]n;
                  
                    [(4, 5), (5, 6)][(4, 5), (4, 9)]void [(4, 10), (4, 14)]test([(4, 15), (4, 15)]) [(4, 17), (5, 6)]{
                    }
                }
                """
            ))
          )
        );
    }
}
