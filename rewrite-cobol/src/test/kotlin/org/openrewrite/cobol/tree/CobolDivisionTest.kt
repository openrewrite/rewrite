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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.cobol.CobolVisitor
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe

class CobolDivisionTest : RewriteTest {

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

    @Disabled("Implement fileControlEntry clauses")
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
                INPUT-OUTPUT SECTION.
                FILE-CONTROL. SELECT OPTIONAL IDENTIFIER ASSIGN TO DISK.
                I-O-CONTROL. IDENTIFIER. RERUN ON IDENTIFIER EVERY 10 RECORDS.
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
                ENVIRONMENT DIVISION.
                INPUT-OUTPUT SECTION.
                FILE-CONTROL.
                    SELECT PRINT-FILE ASSIGN TO
                        XXXXX055.
                    SELECT SEQ-FILE ASSIGN TO
                        XXXXX014.
                    SELECT SEQ-FILE RESERVE NO ALTERNATE AREA.
                    SELECT SEQ-FILE ORGANIZATION IS RECORD BINARY INDEXED.
            """
        )
    )

    @Test
    fun procedureDivision() = rewriteRun(
        cobol("""
            IDENTIFICATION  DIVISION .
            PROGRAM-ID    . HELLO     .
            PROCEDURE DIVISION USING GRP-01 GIVING dataName.
            DECLARATIVES.
            sectionName SECTION 77.
            USE GLOBAL AFTER STANDARD ERROR PROCEDURE ON INPUT.
            END DECLARATIVES.
        """)
    )

    @Test
    fun divisionUsing() = rewriteRun(
        cobol("""
            IDENTIFICATION  DIVISION .
            PROGRAM-ID    . HELLO     .
            PROCEDURE DIVISION USING GRP-01.
            STOP RUN.
        """)
    )

    @Disabled("Not yet implemented")
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
    fun moveStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. MOVETEST.
            DATA DIVISION.
            PROCEDURE DIVISION USING GRP-01.
            PARA-MOVETEST.
                MOVE "MOVETEST" TO DN1.
                MOVE SPACE TO WS1.
        """)
    )

    @Test
    fun mergeStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. MERGETEST.
            PROCEDURE DIVISION.
            MERGE-TEST.
                MERGE ST-FS4  ON ASCENDING KEY SORT-KEY
                    USING  SQ-FS1  SQ-FS2
                    OUTPUT PROCEDURE IS MERGE-OUTPUT-PROC.
        """)
    )

    @Test

    fun multiplyStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. MULTIPLYTEST.
            PROCEDURE DIVISION.
            MULTIPLY -1.3 BY MULT4 ROUNDED.
        """)
    )

    @Test
    fun openStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. OPENTEST.
            PROCEDURE DIVISION.
            OPEN OUTPUT SQ-FS2.
            OPEN INPUT TFIL REVERSED.
            OPEN INPUT TFIL WITH NO REWIND.
        """)
    )

    @Test
    fun performStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. PARSERTEST.
            PROCEDURE DIVISION.
            PERFORM ST301M-MERGE THRU ST301M-SORT 1 TIMES.            
        """)
    )

    @Test
    fun readStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. READTEST.
            PROCEDURE DIVISION.
            READ SQ-FS3 END .
        """)
    )

    @Test
    fun receiveStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. MERGETEST.
            PROCEDURE DIVISION.
            RECEIVE CM-INQUE-1 MESSAGE INTO MSG-72
                NO DATA.
        """.trimIndent())
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
            01 SCREEN1 BLANK LINE
            BELL
            BLINK
            ERASE EOL
            HIGHLIGHT
            GRID
            UNDERLINE
            SIZE IS IDENTIFIER IN IDENTIFIER
            LINE NUMBER IS PLUS IDENTIFIER IN IDENTIFIER
            COLUMN NUMBER IS PLUS IDENTIFIER IN IDENTIFIER
            FOREGROUND-COLOR IS IDENTIFIER IN IDENTIFIER
            BACKGROUND-COLOR IS IDENTIFIER IN IDENTIFIER
            CONTROL IS IDENTIFIER IN IDENTIFIER
            VALUE IS 10
            PICTURE IS ${'$'}(10)
            FROM IDENTIFIER IN IDENTIFIER TO IDENTIFIER IN IDENTIFIER
            USING IDENTIFIER IN IDENTIFIER
            USAGE IS DISPLAY
            BLANK WHEN ZERO
            JUSTIFIED RIGHT
            SIGN IS LEADING SEPARATE CHARACTER
            AUTO
            SECURE
            REQUIRED
            PROMPT CHARACTER IS IDENTIFIER IN IDENTIFIER OCCURS 01 TIMES
            FULL
            ZERO-FILL
            .
        """)
    )

    @Disabled("Potential lexer issue: The REVERSE-VIDEO token maps to RESERVE-VIDEO")
    @Test
    fun reverseVideo() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. DBSection.
            DATA DIVISION.
            SCREEN SECTION.
            01 REVERSE-VIDEO.
        """)
    )

    @Test
    fun acceptStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            PARAGRAPH_NAME.
            ACCEPT identifier FROM DATE YYYYMMDD END-ACCEPT
            ACCEPT identifier FROM ESCAPE KEY
            ACCEPT identifier FROM mnemonicName
            ACCEPT identifier MESSAGE COUNT.
        """)
    )

    @Test
    fun alterStatement() = rewriteRun(
        cobol(
            """
                IDENTIFICATION DIVISION .
                PROGRAM-ID . HELLO-WORLD .
                PROCEDURE DIVISION .
                ALTER PARA-54 TO PROCEED TO PARA-54B.
                ALTER PARA-23 TO PARA-24.
            """
        )
    )

    @Test
    fun cancelStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            PARAGRAPH_NAME.
            CANCEL "literal"
            CANCEL identifier
            CANCEL libraryName BYTITLE.
        """)
    )

    @Test
    fun closeStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            PARAGRAPH_NAME.
            CLOSE fileName UNIT FOR REMOVAL WITH LOCK
            CLOSE fileName WITH NO REWIND
            CLOSE fileName NO WAIT USING CLOSE-DISPOSITION OF ABORT
            CLOSE fileName NO WAIT USING ASSOCIATED-DATA identifier
            CLOSE fileName NO WAIT USING ASSOCIATED-DATA-LENGTH OF identifier.
        """)
    )

    @Test
    fun rewriteStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            PARAGRAPH_NAME.
            REWRITE dataName IN fileName END-REWRITE.
        """)
    )

    @Test
    fun callStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION USING GRP-01.
            SECT-IC109-0001 SECTION.
            PARA-IC109.
                CALL "IC110A" USING BY REFERENCE WS1 GRP-01.
                CALL "IC110A" USING BY VALUE ADDRESS OF GRP-01.
                CALL "IC110A" USING BY CONTENT LENGTH OF GRP-01.
                CALL "IC110A" GIVING GRP-01.
                CALL "IC110A" ON OVERFLOW CONTINUE.
                CALL "IC110A" ON EXCEPTION CONTINUE.
                CALL "IC110A" NOT ON EXCEPTION CONTINUE.
        """)
    )

    @Test
    fun writeStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION USING GRP-01.
            PARA-IC109.
                WRITE IC110A FROM GRP-01.
                WRITE IC110A BEFORE ADVANCING PAGE.
                WRITE IC110A BEFORE ADVANCING 10 LINES.
                WRITE IC110A BEFORE ADVANCING GRP-01.
                WRITE IC110A AT END-OF-PAGE CONTINUE.
                WRITE IC110A NOT AT END-OF-PAGE CONTINUE.
                WRITE IC110A INVALID KEY CONTINUE.
                WRITE IC110A NOT INVALID KEY CONTINUE.
        """)
    )

    @Test
    fun computeStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION .
            PROGRAM-ID . HELLO-WORLD .
            PROCEDURE DIVISION .
                COMPUTE V = (1 + 2) .
                COMPUTE LAG-TIME =
                    ((SYS-HRS * 3600) + (SYS-MINS * 60) + SYS-SECS) -
                    ((HOURS OF MSG-TIME * 3600) + (MINUTES OF MSG-TIME * 60)
                    + SECONDS OF MSG-TIME)
                    END-COMPUTE .
        """)
    )

    @Test
    fun divideStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION USING GRP-01.
            SIG-TEST-GF-5-0.
                DIVIDE 0.097 INTO DIV7 ROUNDED.
                DIVIDE 0.097 INTO DIV7 GIVING DIV8 ROUNDED.
                DIVIDE 0.097 BY DIV7 GIVING DIV8 ROUNDED.
                DIVIDE 0.097 INTO DIV7 REMAINDER DIV9.
                DIVIDE 0.097 INTO DIV7 ON SIZE ERROR CONTINUE.
                DIVIDE 0.097 INTO DIV7 NOT ON SIZE ERROR CONTINUE.
        """)
    )

    @Test
    fun evaluateStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION USING GRP-01.
            F-ANNUITY-02.
            EVALUATE IC110A END-EVALUATE.
            EVALUATE IC110A ALSO IC110B.
            EVALUATE IC110A
            WHEN ANY ALSO ANY
                CONTINUE
            WHEN IDENTIFIER THRU IDENTIFIER
                CONTINUE
            WHEN TRUE
                CONTINUE
            WHEN OTHER
                CONTINUE.
        """)
    )

    @Test
    fun conditions() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION USING GRP-01.
            F-ANNUITY-02.
            EVALUATE IC110A
            WHEN IDENTIFIER IS NOT ALPHABETIC-LOWER
                CONTINUE
            WHEN IDENTIFIER IN IDENTIFIER
                CONTINUE.
        """)
    )

    @Test
    fun conditionNameSubscriptReference() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            COMMA-SUBSCRIPT-TEST.
            EVALUATE NOT IDENTIFIER (IDENTIFIER, IDENTIFIER IDENTIFIER)
            .
        """)
    )

    @Test
    fun sendStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            SEND CM-OUTQUE-1 FROM MSG-70 WITH EMI
                AFTER ADVANCING PAGE.
        """)
    )

    @Test
    fun tableCallTest() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            COMMA-SUBSCRIPT-TEST.
            EVALUATE SUBSCRIPT
            WHEN IDENTIFIER (IDENTIFIER, IDENTIFIER IDENTIFIER)
                CONTINUE.
        """)
    )

    @Test
    fun functionCallTest() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            COMMA-SUBSCRIPT-TEST.
            EVALUATE SUBSCRIPT
            WHEN IDENTIFIER (FUNCTION INTEGER (IDENTIFIER, IDENTIFIER IDENTIFIER) (1: 10))
                CONTINUE.
        """)
    )

    @Test
    fun relationConditions() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            F-ANNUITY-02.
            EVALUATE IC110A
            WHEN NOT +IDENTIFIER IS NOT ZERO
            WHEN NOT +IDENTIFIER IS GREATER OR EQUAL +IDENTIFIER
            WHEN NOT +ZERO GREATER THAN (IDENTIFIER AND IDENTIFIER OR IDENTIFIER)
            .
        """)
    )

    @Test
    fun multiElementLiteral() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            Literal-Test.
            EVALUATE DFHRESP (IDENTIFIER).
        """)
    )

    @Test
    fun multiElementIdentifier() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            Identifier-Test.
            EVALUATE IDENTIFIER IN IDENTIFIER.
        """)
    )

    @Test
    fun openMultipleStatements() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            OPEN-FILES.
                OPEN     INPUT IDENTIFIER REVERSED INPUT IDENTIFIER REVERSED
                OPEN     OUTPUT IDENTIFIER WITH NO REWIND IDENTIFIER WITH NO REWIND
                OPEN     I-O IDENTIFIER IDENTIFIER I-O IDENTIFIER IDENTIFIER
                OPEN     EXTEND IDENTIFIER IDENTIFIER EXTEND IDENTIFIER IDENTIFIER.
        """)
    )

    @Test
    fun outOfOrderOpenStatements() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            OPEN-FILES.
                OPEN     INPUT IDENTIFIER OUTPUT IDENTIFIER INPUT IDENTIFIER OUTPUT IDENTIFIER.
        """)
    )

    @Test
    fun unstringStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. acceptStatement.
            PROCEDURE DIVISION.
            UNSTRING-TEST.
                UNSTRING IDENTIFIER DELIMITED BY ALL IDENTIFIER OR ALL IDENTIFIER INTO IDENTIFIER DELIMITER IN IDENTIFIER COUNT IN IDENTIFIER END-UNSTRING
                UNSTRING IDENTIFIER INTO IDENTIFIER WITH POINTER IDENTIFIER
                UNSTRING IDENTIFIER INTO IDENTIFIER TALLYING IN IDENTIFIER
                UNSTRING IDENTIFIER INTO IDENTIFIER ON OVERFLOW
                UNSTRING IDENTIFIER INTO IDENTIFIER NOT ON OVERFLOW.
        """)
    )

    @Test
    fun terminateStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. terminateStatement.
            PROCEDURE DIVISION.
            RW301M-CONTROL.
                TERMINATE RFIL2.
        """)
    )

    @Test
    fun generateStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. terminateStatement.
            PROCEDURE DIVISION.
            RW301M-CONTROL.
                GENERATE RREC.
        """)
    )

    @Test
    fun subtractStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. subtractStatement.
            PROCEDURE DIVISION.
            RW301M-CONTROL.
                SUBTRACT 1 FROM ERROR-COUNTER
                SUBTRACT N-10 FROM 0 GIVING N-19
                SUBTRACT CORRESPONDING IDENTIFIER FROM IDENTIFIER ROUNDED.
        """)
    )

    @Test
    fun exitStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. exitStatement.
            PROCEDURE DIVISION.
            RW301M-CONTROL.
                EXIT PROGRAM.
        """)
    )

    @Test
    fun sortStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. sortStatement.
            PROCEDURE DIVISION.
            SORT-STATEMENT.
                SORT SORTFILE-1B
                    ON DESCENDING KEY KEY-1
                    ON ASCENDING KEY KEY-2
                    ON DESCENDING KEY KEY-3
                    ASCENDING KEY-4 KEY-5
                USING SORTIN-1B
                GIVING SORTOUT-1B.
        """)
    )

    @Test
    fun stringStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. subtractStatement.
            PROCEDURE DIVISION.
            RW301M-CONTROL.
                STRING NONNUMERICLITERAL, NONNUMERICLITERAL NONNUMERICLITERAL DELIMITED BY SIZE
                INTO IDENTIFIER
                WITH POINTER IDENTIFIER END-STRING
            .
        """)
    )

    @Test
    fun startStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. subtractStatement.
            PROCEDURE DIVISION.
            STA-TEST-GF-01.
                START IX-FS2 KEY IS NOT LESS THAN IDENTIFIER IN IDENTIFIER END-START.
            .
        """)
    )

    @Test
    fun goToStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. subtractStatement.
            PROCEDURE DIVISION.
            ACCEPT-TEST-01.
                GO TO CM105-FINI.
                GO TO CM105-FINI DEPENDING ON IDENTIFIER IN IDENTIFIER.
            .
        """)
    )

    @Test
    fun ifStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. subtractStatement.
            PROCEDURE DIVISION.
            IF--TEST-GF-99.
                IF      ZERO IS EQUAL TO IF-D1
                   THEN PERFORM PASS
                ELSE
                    PERFORM FAIL.
            .
        """)
    )

    @Test
    fun initializeStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. subtractStatement.
            PROCEDURE DIVISION.
            INI-TEST-GF-1-0.
                INITIALIZE IDENTIFIER IN IDENTIFIER REPLACING NATIONAL DATA BY 42.
            .
        """)
    )

    @Test
    fun initiateStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. subtractStatement.
            PROCEDURE DIVISION.
            RW301M-CONTROL.
                INITIATE RFIL2.
            .
        """)
    )

    @Test
    fun inspectStatement() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. subtractStatement.
            PROCEDURE DIVISION.
            RW301M-CONTROL.
                INSPECT IDENTIFIER IN IDENTIFIER TALLYING IDENTIFIER IN IDENTIFIER FOR CHARACTER BEFORE INITIAL 42
                INSPECT IDENTIFIER IN IDENTIFIER REPLACING CHARACTER BY IDENTIFIER IN IDENTIFIER BEFORE INITIAL 42
                INSPECT IDENTIFIER IN IDENTIFIER TALLYING IDENTIFIER IN IDENTIFIER FOR CHARACTER BEFORE IDENTIFIER IN IDENTIFIER REPLACING ALL IDENTIFIER IN IDENTIFIER BY IDENTIFIER IN IDENTIFIER
                INSPECT IDENTIFIER IN IDENTIFIER CONVERTING IDENTIFIER IN IDENTIFIER TO IDENTIFIER IN IDENTIFIER BEFORE IDENTIFIER IN IDENTIFIER
            .
        """)
    )

    @Test
    fun communicationSection() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. communicationSection.
            DATA DIVISION.
            COMMUNICATION SECTION.
                CD COMMNAME FOR INITIAL INPUT.
                CD COMMNAME FOR OUTPUT.
                CD COMMNAME FOR INITIAL I-O.
        """)
    )

    @Test
    fun reportSection() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. communicationSection.
            DATA DIVISION.
            REPORT SECTION.
                RD IDENTIFIER IN IDENTIFIER IS GLOBAL.
                10 IDENTIFIER LINE NUMBER IS 10 ON NEXT PAGE.
        """)
    )

    @Test
    fun programLibrarySection() = rewriteRun(
        cobol("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. communicationSection.
            DATA DIVISION.
            PROGRAM-LIBRARY SECTION.
                LD IDENTIFIER EXPORT ATTRIBUTE SHARING IS DONTCARE ENTRY-PROCEDURE IDENTIFIER FOR ZERO
                LB IDENTIFIER IMPORT IS GLOBAL IS COMMON ATTRIBUTE
                FUNCTIONNAME IS ZERO LIBACCESS IS BYFUNCTION LIBPARAMETER IS ZERO
        """)
    )
}
