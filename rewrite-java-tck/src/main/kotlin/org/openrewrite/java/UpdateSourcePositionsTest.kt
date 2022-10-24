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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.PrintOutputCapture
import org.openrewrite.SourceFile
import org.openrewrite.marker.Marker
import org.openrewrite.marker.Range
import java.util.function.UnaryOperator

@Disabled
interface UpdateSourcePositionsTest : JavaRecipeTest {

    private fun SourceFile.printWithLines() = printAll(PrintOutputCapture(0, object : PrintOutputCapture.MarkerPrinter {
        override fun beforePrefix(marker: Marker, cursor: Cursor, commentWrapper: UnaryOperator<String>): String = when (marker) {
            is Range -> "[(${marker.start.line}, ${marker.start.column}), (${marker.end.line}, ${marker.end.column})]"
            else -> ""
        }
    }))

    @Test
    fun lamdaParameter(jp: JavaParser) {
        val cu = jp.parse(
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
            """.trimIndent()
        )

        val after = UpdateSourcePositions().run(cu).results[0].after!!

        assertThat(after.printWithLines()).isEqualTo("""
            [(1, 0), (13, 2)][(1, 0), (1, 16)]package [(1, 8), (1, 16)][(1, 8), (1, 11)]org.[(1, 12), (1, 16)]test;
                          
            [(3, 1), (3, 35)]import [(3, 8), (3, 35)][(3, 8), (3, 26)][(3, 8), (3, 17)][(3, 8), (3, 12)]java.[(3, 13), (3, 17)]util.[(3, 18), (3, 26)]function.[(3, 27), (3, 35)]Consumer;
                            
            [(5, 1), (13, 2)][(5, 1), (5, 7)]public class [(5, 14), (5, 25)]Application [(5, 26), (13, 2)]{
            
                [(7, 5), (11, 6)][(7, 5), (7, 11)]public [(7, 12), (7, 28)][(7, 12), (7, 20)]Consumer<[(7, 21), (7, 27)]String> [(7, 29), (7, 33)]demo([(7, 34), (7, 34)]) [(7, 36), (11, 6)]{
                    [(8, 9), (10, 10)]return [(8, 16), (10, 10)]([(8, 17), (8, 21)][(8, 17), (8, 21)][(8, 17), (8, 21)]args) -> [(8, 26), (10, 10)]{
                        [(9, 13), (9, 25)][(9, 13), (9, 16)]log.[(9, 17), (9, 21)]info([(9, 22), (9, 24)]"");
                    };
                }
            
            }
        """.trimIndent())

    }

    @Test
    fun updateSourcePositions(jp: JavaParser) {
        val cu = jp.parse(
                """ 
                class Test {
                    int n;
                    
                    void test() {
                    }
                }
            """.trimIndent()
        )

        val after = UpdateSourcePositions().run(cu).results[0].after!!

        assertThat(after.printWithLines()).isEqualTo("""
            [0,54][0,54]class [6,4]Test [11,43]{
                [17,5][17,3]int [21,1][21,1]n;
                
                [33,19][33,4]void [38,4]test([43,0]) [45,7]{
                }
            }
        """.trimIndent())

        assertThat(after.printWithLines()).isEqualTo("""
            [(1, 0), (6, 2)][(1, 0), (6, 2)]class [(1, 6), (1, 10)]Test [(1, 11), (6, 2)]{
                [(2, 5), (2, 10)][(2, 5), (2, 8)]int [(2, 9), (2, 10)][(2, 9), (2, 10)]n;
                
                [(4, 5), (5, 6)][(4, 5), (4, 9)]void [(4, 10), (4, 14)]test([(4, 15), (4, 15)]) [(4, 17), (5, 6)]{
                }
            }
        """.trimIndent())

    }

    @Test
    fun updateSourcePositions2(jp: JavaParser) {
        val source = """
            package org.test;
            
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Scope;
            
            @Scope(value=)
            public class TestScopeCompletion {
            
                @Bean
                @Scope("onMethod")
                public Object myBean() {
                    return null;
                }
            }
            """.trimIndent()

        val cu = jp.parse(source)

        val after = UpdateSourcePositions().run(cu).results[0].after!!

        assertThat(after.printAll()).isEqualTo(source)

        assertThat(after.printWithLines()).isEqualTo("""
            [0,266][0,16]package [8,8][8,3]org.[12,4]test;
            
            [19,50]import [26,43][26,38][26,27][26,19][26,3]org.[30,15]springframework.[46,7]context.[54,10]annotation.[65,4]Bean;
            [71,51]import [78,44][78,38][78,27][78,19][78,3]org.[82,15]springframework.[98,7]context.[106,10]annotation.[117,5]Scope;
            
            [125,141][125,14]@[126,5]Scope([132,6][132,5]value=)
            [140,6]public class [153,19]TestScopeCompletion [173,93]{
            
                [180,84][180,5]@[181,4]Bean
                [190,18]@[191,5]Scope([197,10]"onMethod")
                [213,6]public [220,6]Object [227,6]myBean([234,0]) [236,28]{
                    [246,11]return [253,4]null;
                }
            }
        """.trimIndent())

        assertThat(after.printWithLines()).isEqualTo("""
            [(1, 0), (14, 2)][(1, 0), (1, 16)]package [(1, 8), (1, 16)][(1, 8), (1, 11)]org.[(1, 12), (1, 16)]test;

            [(3, 1), (3, 51)]import [(3, 8), (3, 51)][(3, 8), (3, 46)][(3, 8), (3, 35)][(3, 8), (3, 27)][(3, 8), (3, 11)]org.[(3, 12), (3, 27)]springframework.[(3, 28), (3, 35)]context.[(3, 36), (3, 46)]annotation.[(3, 47), (3, 51)]Bean;
            [(4, 1), (4, 52)]import [(4, 8), (4, 52)][(4, 8), (4, 46)][(4, 8), (4, 35)][(4, 8), (4, 27)][(4, 8), (4, 11)]org.[(4, 12), (4, 27)]springframework.[(4, 28), (4, 35)]context.[(4, 36), (4, 46)]annotation.[(4, 47), (4, 52)]Scope;

            [(6, 1), (14, 2)][(6, 1), (6, 15)]@[(6, 2), (6, 7)]Scope([(6, 8), (6, 14)][(6, 8), (6, 13)]value=)
            [(7, 1), (7, 7)]public class [(7, 14), (7, 33)]TestScopeCompletion [(7, 34), (14, 2)]{

                [(9, 5), (13, 6)][(9, 5), (9, 10)]@[(9, 6), (9, 10)]Bean
                [(10, 5), (10, 23)]@[(10, 6), (10, 11)]Scope([(10, 12), (10, 22)]"onMethod")
                [(11, 5), (11, 11)]public [(11, 12), (11, 18)]Object [(11, 19), (11, 25)]myBean([(11, 26), (11, 26)]) [(11, 28), (13, 6)]{
                    [(12, 9), (12, 20)]return [(12, 16), (12, 20)]null;
                }
            }
        """.trimIndent()
        )
    }

    @Test
    fun lineColumnTest(jp: JavaParser) {
        val source = """
            package com.example;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class EmptyBoot15WebAppApplication {
            
                public static void main(String[] args) {
                    SpringApplication.run(EmptyBoot15WebAppApplication.class, args);
                }
            }
        """.trimIndent()

        val cu = jp.parse(source)

        val after = UpdateSourcePositions().run(cu).results[0].after!!

        assertThat(after.printAll()).isEqualTo(source)

        assertThat(after.printWithLines()).isEqualTo("""
            [(1, 0), (12, 2)][(1, 0), (1, 19)]package [(1, 8), (1, 19)][(1, 8), (1, 11)]com.[(1, 12), (1, 19)]example;
            
            [(3, 1), (3, 50)]import [(3, 8), (3, 50)][(3, 8), (3, 32)][(3, 8), (3, 27)][(3, 8), (3, 11)]org.[(3, 12), (3, 27)]springframework.[(3, 28), (3, 32)]boot.[(3, 33), (3, 50)]SpringApplication;
            [(4, 1), (4, 68)]import [(4, 8), (4, 68)][(4, 8), (4, 46)][(4, 8), (4, 32)][(4, 8), (4, 27)][(4, 8), (4, 11)]org.[(4, 12), (4, 27)]springframework.[(4, 28), (4, 32)]boot.[(4, 33), (4, 46)]autoconfigure.[(4, 47), (4, 68)]SpringBootApplication;
            
            [(6, 1), (12, 2)][(6, 1), (6, 23)]@[(6, 2), (6, 23)]SpringBootApplication
            [(7, 1), (7, 7)]public class [(7, 14), (7, 42)]EmptyBoot15WebAppApplication [(7, 43), (12, 2)]{
            
                [(9, 5), (11, 6)][(9, 5), (9, 11)]public [(9, 12), (9, 18)]static [(9, 19), (9, 23)]void [(9, 24), (9, 28)]main([(9, 29), (9, 42)][(9, 29), (9, 35)]String[] [(9, 38), (9, 42)][(9, 38), (9, 42)]args) [(9, 44), (11, 6)]{
                    [(10, 9), (10, 72)][(10, 9), (10, 26)]SpringApplication.[(10, 27), (10, 30)]run([(10, 31), (10, 65)][(10, 31), (10, 59)]EmptyBoot15WebAppApplication.[(10, 60), (10, 65)]class, [(10, 67), (10, 71)]args);
                }
            }
        """.trimIndent())
    }
}
