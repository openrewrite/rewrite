/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.cobol.tree

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.cobol.CobolVisitor
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe

class CobolBasicsTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(toRecipe {
            object : CobolVisitor<ExecutionContext>() {
                override fun visitSpace(space: Space, p: ExecutionContext): Space {
                    if (space.whitespace.trim().isNotEmpty()) {
                        return space.withWhitespace("(~~>${space.whitespace}<~~)")
                    }
                    return space
                }
            }
        })
    }

    @Test
    fun helloWorld() = rewriteRun(
        cobol(
            """
                IDENTIFICATION  DIVISION .
                PROGRAM-ID    . HELLO     .
                PROCEDURE DIVISION.
                DISPLAY 'Hello world!'.
                STOP RUN.
            """
        )
    )

    @Test
    fun arithmetic() = rewriteRun(
        cobol(
            """
                IDENTIFICATION DIVISION .
                PROGRAM-ID . HELLO-WORLD .
                DATA DIVISION .
                    WORKING-STORAGE SECTION .
                        77 X PIC 99.
                        77 Y PIC 99.
                        77 Z PIC 99.
                PROCEDURE DIVISION .
                    SET X TO 10 .
                    SET Y TO 25 .
                    ADD X Y GIVING Z .
                    DISPLAY "X + Y = "Z .
                STOP RUN .
            """
        )
    )

    @Test
    fun environmentDivision() = rewriteRun(
        cobol(
            """
                IDENTIFICATION DIVISION.
                PROGRAM-ID.
                    IC109A.
                ENVIRONMENT DIVISION.
                CONFIGURATION SECTION.
                SOURCE-COMPUTER.
                    XXXXX082.
                OBJECT-COMPUTER.
                    XXXXX083
                    MEMORY SIZE XXXXX068 CHARACTERS
                    PROGRAM COLLATING SEQUENCE IS COLLATING-SEQ-1.
                SPECIAL-NAMES.
                    ALPHABET PRG-COLL-SEQ IS
                    STANDARD-2.
            """
        )
    )

    @Test
    fun inputOutputSection() = rewriteRun(
        cobol(
            """
                IDENTIFICATION DIVISION.
                PROGRAM-ID.
                    IC109A.
                INPUT-OUTPUT SECTION.
                FILE-CONTROL.
                    SELECT PRINT-FILE ASSIGN TO
                    XXXXX055.
                    SELECT SEQ-FILE ASSIGN TO
                    XXXXX014.
            """
        )
    )

    @Test
    fun ic109a() = rewriteRun(
        cobol(
            """
                IDENTIFICATION DIVISION.                                       
                PROGRAM-ID.                                                    
                    IC109A.                                                    
                ENVIRONMENT DIVISION.                                          
                CONFIGURATION SECTION.                                         
                SOURCE-COMPUTER.                                               
                    XXXXX082.                                                  
                OBJECT-COMPUTER.                                               
                    XXXXX083.                                                  
                INPUT-OUTPUT SECTION.                                          
                FILE-CONTROL.                                                  
                    SELECT PRINT-FILE ASSIGN TO                                
                    XXXXX055.                                                  
                DATA DIVISION.                                                 
                FILE SECTION.                                                  
                FD  PRINT-FILE.                                                
                01  PRINT-REC PICTURE X(120).                                  
                01  DUMMY-RECORD PICTURE X(120).                               
                WORKING-STORAGE SECTION.                                       
                77  WS1 PICTURE X.                                             
                LINKAGE SECTION.                                               
                01  GRP-01.                                                    
                    02  SUB-CALLED.                                            
                        03  DN1  PICTURE X(6).                                 
                        03  DN2  PICTURE X(6).                                 
                        03  DN3  PICTURE X(6).                                 
                    02  TIMES-CALLED.                                          
                        03  DN4  PICTURE S999.                                 
                        03  DN5  PICTURE S999.                                 
                        03  DN6  PICTURE S999.                                 
                    02  SPECIAL-FLAGS.                                         
                        03  DN7 PICTURE X.                                     
                        03  DN8 PICTURE X.                                     
                        03  DN9 PICTURE X.                                     
                PROCEDURE DIVISION USING GRP-01.                               
                SECT-IC109-0001 SECTION.                                       
                PARA-IC109.                                                    
                    MOVE "IC109A" TO DN1.                                      
                    MOVE SPACE TO WS1.                                         
                    CALL "IC110A" USING WS1 GRP-01.                            
                    ADD 1 TO DN4.                                              
                    MOVE WS1 TO DN9.                                           
                EXIT-IC109.                                                    
                    EXIT PROGRAM.                                              
                END-OF
            """
        )
    )

    @Test
    fun fileSection() = rewriteRun(
        cobol("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID.
                    IC109A.
                DATA DIVISION.
                FILE SECTION.
                FD  PRINT-FILE.
                01  PRINT-REC PICTURE X(120).
                01  DUMMY-RECORD PICTURE X(120).
                WORKING-STORAGE SECTION.
                77  WS1 PICTURE X.
        """)
    )

    @Test
    fun linkageSection() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
                PROGRAM-ID.
                    IC109A.
                DATA DIVISION.
                LINKAGE SECTION.                
                01  GRP-01.                     
                    02  SUB-CALLED.             
                        03  DN1  PICTURE X(6).  
                        03  DN2  PICTURE X(6).  
                        03  DN3  PICTURE X(6).  
                    02  TIMES-CALLED.           
                        03  DN4  PICTURE S999.  
                        03  DN5  PICTURE S999.  
                        03  DN6  PICTURE S999.  
                    02  SPECIAL-FLAGS.          
                        03  DN7 PICTURE X.      
                        03  DN8 PICTURE X.      
                        03  DN9 PICTURE X.      
        """)
    )

    @Test
    fun localStorageSection() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. LocalStorage.
            DATA DIVISION.
            LOCAL-STORAGE Section.
            01  NUM  PIC 9(4).
        """)
    )

    @Test
    fun dataBaseSection() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. DBSection.
            DATA DIVISION.
            DATA-BASE SECTION.
            01 TRUE INVOKE TRUE
        """)
    )

    @Test
    fun screenSection() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. DBSection.
            DATA DIVISION.
            SCREEN SECTION.
            01 SCREEN1 BLANK LINE BELL BLINK CONTROL IS 77 SIZE IS 77 USING 77.
        """)
    )
}
