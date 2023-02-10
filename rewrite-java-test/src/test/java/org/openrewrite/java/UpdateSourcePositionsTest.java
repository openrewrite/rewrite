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
import org.openrewrite.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Range;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateSourcePositionsTest {

    private static String printWithLines(SourceFile sourceFile) {
        return sourceFile.printAll(new PrintOutputCapture<>(0, new PrintOutputCapture.MarkerPrinter() {
            @Override
            public String beforePrefix(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                if (marker instanceof Range && cursor.getParent().getValue() instanceof J.Identifier) {
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
        List<J.CompilationUnit> cus = JavaParser.fromJavaVersion().build().parse(
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
            """
        );
        Result res = new UpdateSourcePositions().run(cus).getResults().get(0);
        assertThat(printWithLines(res.getAfter())).isEqualTo(
          """
            package [(1, 8), (1, 11)]org.[(1, 12), (1, 16)]test;
            
            import [(3, 8), (3, 12)]java.[(3, 13), (3, 17)]util.[(3, 18), (3, 26)]function.[(3, 27), (3, 35)]Consumer;
            
            public class[(5, 14), (5, 25)] Application {
            
                public [(7, 12), (7, 20)]Consumer<[(7, 21), (7, 27)]String>[(7, 29), (7, 33)] demo() {
                    return ([(8, 17), (8, 21)]args) -> {
                        [(9, 13), (9, 16)]log.[(9, 17), (9, 21)]info("");
                    };
                }
            }
            """
        );
    }

    @Test
    void updateSourcePositions() {
        List<J.CompilationUnit> cus = JavaParser.fromJavaVersion().build().parse(
          """ 
            class Test {
                int n;
                
                void test() {
                }
            }
            """
        );
        Result res = new UpdateSourcePositions().run(cus).getResults().get(0);
        assertThat(printWithLines(res.getAfter())).isEqualTo(
          """
            class[(1, 6), (1, 10)] Test {
                int [(2, 9), (2, 10)]n;
            
                void[(4, 10), (4, 14)] test() {
                }
            }
            """
        );
    }
}
