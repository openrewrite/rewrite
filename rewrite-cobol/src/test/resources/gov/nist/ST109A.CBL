      *HEADER,COBOL,ST109A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1094.2
000200 PROGRAM-ID.                                                      ST1094.2
000300     ST109A.                                                      ST1094.2
000400****************************************************************  ST1094.2
000500*                                                              *  ST1094.2
000600*    VALIDATION FOR:-                                          *  ST1094.2
000700*                                                              *  ST1094.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1094.2
000900*                                                              *  ST1094.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1094.2
001100*                                                              *  ST1094.2
001200****************************************************************  ST1094.2
001300*                                                              *  ST1094.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1094.2
001500*                                                              *  ST1094.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1094.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1094.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1094.2
001900*                                                              *  ST1094.2
002000****************************************************************  ST1094.2
002100*    ST109 BUILDS A FILE WHICH IS SORTED IN ST110 AND CHECKED IN  ST1094.2
002200*    ST111. THE CREATED FILE CONSISTS OF 40 RECORDS OF VARYING    ST1094.2
002300*    LENGTH (50, 75, 100 CHARACTERS). THE THREE RECORDS SHOWN     ST1094.2
002400*    BELOW REOCCUR UNTIL 40 IS REACHED.                           ST1094.2
002500*      NON-KEY KEY-1        KEY-2               FILLER            ST1094.2
002600*      X(2)     X(10)       X(38)                                 ST1094.2
002700*                                                                 ST1094.2
002800*      "BB"    "LOWEST TWO" "MIDDLE TWO-FIRST"  X(25) VALUE ZERO  ST1094.2
002900*      "CC"    "LOWEST TWO" "MIDDLE TWO-SECOND" X(50) VALUE QUOTE ST1094.2
003000*      "AA"    "LOWEST ONE" "MIDDLE ONE-ONLY"   (NONE)            ST1094.2
003100*                                                                 ST1094.2
003200*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  * *.ST1094.2
003300                                                                  ST1094.2
003400 ENVIRONMENT DIVISION.                                            ST1094.2
003500 CONFIGURATION SECTION.                                           ST1094.2
003600 SOURCE-COMPUTER.                                                 ST1094.2
003700     XXXXX082.                                                    ST1094.2
003800 OBJECT-COMPUTER.                                                 ST1094.2
003900     XXXXX083.                                                    ST1094.2
004000 INPUT-OUTPUT SECTION.                                            ST1094.2
004100 FILE-CONTROL.                                                    ST1094.2
004200     SELECT PRINT-FILE ASSIGN TO                                  ST1094.2
004300     XXXXX055.                                                    ST1094.2
004400     SELECT   SORTOUT-1I ASSIGN TO                                ST1094.2
004500     XXXXP001.                                                    ST1094.2
004600 DATA DIVISION.                                                   ST1094.2
004700 FILE SECTION.                                                    ST1094.2
004800 FD  PRINT-FILE.                                                  ST1094.2
004900 01  PRINT-REC PICTURE X(120).                                    ST1094.2
005000 01  DUMMY-RECORD PICTURE X(120).                                 ST1094.2
005100 FD  SORTOUT-1I                                                   ST1094.2
005200     LABEL RECORDS STANDARD                                       ST1094.2
005300C    VALUE OF                                                     ST1094.2
005400C    XXXXX074                                                     ST1094.2
005500C    IS                                                           ST1094.2
005600C    XXXXX075                                                     ST1094.2
005700*    XXXXX069                                                     ST1094.2
005800     RECORD CONTAINS 50 TO 100 CHARACTERS                         ST1094.2
005900     DATA RECORDS ARE SHORT-RECORD                                ST1094.2
006000                      MEDIUM-RECORD                               ST1094.2
006100                      LONG-RECORD.                                ST1094.2
006200 01  SHORT-RECORD       PICTURE X(50).                            ST1094.2
006300 01  MEDIUM-RECORD      PICTURE X(75).                            ST1094.2
006400 01  LONG-RECORD        PICTURE X(100).                           ST1094.2
006500 WORKING-STORAGE SECTION.                                         ST1094.2
006600 77  COMMENT-SENTENCE  PICTURE X(116) VALUE " ST109 HAS CREATED A ST1094.2
006700-    "FILE OF 40 VARIABLE-LENGTH-RECORDS. THESE RECORDS WILL BE SOST1094.2
006800-    "RTED IN ST110 AND CHECKED IN ST111.".                       ST1094.2
006900 77  UTIL-CTR           PICTURE S99999 VALUE ZERO.                ST1094.2
007000 01  SHORT-WORK.                                                  ST1094.2
007100     02 FILLER          PICTURE XX    VALUE "AA".                 ST1094.2
007200     02 FILLER          PICTURE X(10) VALUE "LOWEST ONE".         ST1094.2
007300     02 FILLER          PICTURE X(38) VALUE "MIDDLE ONE-ONLY".    ST1094.2
007400 01  MEDIUM-WORK.                                                 ST1094.2
007500     02 FILLER          PICTURE XX    VALUE "BB".                 ST1094.2
007600     02 FILLER          PICTURE X(10) VALUE "LOWEST TWO".         ST1094.2
007700     02 FILLER          PICTURE X(38) VALUE "MIDDLE TWO-FIRST".   ST1094.2
007800     02 FILLER          PICTURE X(25) VALUE ZERO.                 ST1094.2
007900 01  LONG-WORK.                                                   ST1094.2
008000     02 FILLER          PICTURE XX    VALUE "CC".                 ST1094.2
008100     02 FILLER          PICTURE X(10) VALUE "LOWEST TWO".         ST1094.2
008200     02 FILLER          PICTURE X(38) VALUE "MIDDLE TWO-SECOND".  ST1094.2
008300     02 FILLER          PICTURE X(50) VALUE QUOTE.                ST1094.2
008400 01  TEST-RESULTS.                                                ST1094.2
008500     02 FILLER                   PIC X      VALUE SPACE.          ST1094.2
008600     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1094.2
008700     02 FILLER                   PIC X      VALUE SPACE.          ST1094.2
008800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1094.2
008900     02 FILLER                   PIC X      VALUE SPACE.          ST1094.2
009000     02  PAR-NAME.                                                ST1094.2
009100       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1094.2
009200       03  PARDOT-X              PIC X      VALUE SPACE.          ST1094.2
009300       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1094.2
009400     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1094.2
009500     02 RE-MARK                  PIC X(61).                       ST1094.2
009600 01  TEST-COMPUTED.                                               ST1094.2
009700     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1094.2
009800     02 FILLER                   PIC X(17)  VALUE                 ST1094.2
009900            "       COMPUTED=".                                   ST1094.2
010000     02 COMPUTED-X.                                               ST1094.2
010100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1094.2
010200     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1094.2
010300                                 PIC -9(9).9(9).                  ST1094.2
010400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1094.2
010500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1094.2
010600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1094.2
010700     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1094.2
010800         04 COMPUTED-18V0                    PIC -9(18).          ST1094.2
010900         04 FILLER                           PIC X.               ST1094.2
011000     03 FILLER PIC X(50) VALUE SPACE.                             ST1094.2
011100 01  TEST-CORRECT.                                                ST1094.2
011200     02 FILLER PIC X(30) VALUE SPACE.                             ST1094.2
011300     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1094.2
011400     02 CORRECT-X.                                                ST1094.2
011500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1094.2
011600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1094.2
011700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1094.2
011800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1094.2
011900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1094.2
012000     03      CR-18V0 REDEFINES CORRECT-A.                         ST1094.2
012100         04 CORRECT-18V0                     PIC -9(18).          ST1094.2
012200         04 FILLER                           PIC X.               ST1094.2
012300     03 FILLER PIC X(2) VALUE SPACE.                              ST1094.2
012400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1094.2
012500 01  CCVS-C-1.                                                    ST1094.2
012600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1094.2
012700-    "SS  PARAGRAPH-NAME                                          ST1094.2
012800-    "       REMARKS".                                            ST1094.2
012900     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1094.2
013000 01  CCVS-C-2.                                                    ST1094.2
013100     02 FILLER                     PIC X        VALUE SPACE.      ST1094.2
013200     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1094.2
013300     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1094.2
013400     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1094.2
013500     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1094.2
013600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1094.2
013700 01  REC-CT                        PIC 99       VALUE ZERO.       ST1094.2
013800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1094.2
013900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1094.2
014000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1094.2
014100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1094.2
014200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1094.2
014300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1094.2
014400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1094.2
014500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1094.2
014600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1094.2
014700 01  CCVS-H-1.                                                    ST1094.2
014800     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1094.2
014900     02  FILLER                    PIC X(42)    VALUE             ST1094.2
015000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1094.2
015100     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1094.2
015200 01  CCVS-H-2A.                                                   ST1094.2
015300   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1094.2
015400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1094.2
015500   02  FILLER                        PIC XXXX   VALUE             ST1094.2
015600     "4.2 ".                                                      ST1094.2
015700   02  FILLER                        PIC X(28)  VALUE             ST1094.2
015800            " COPY - NOT FOR DISTRIBUTION".                       ST1094.2
015900   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1094.2
016000                                                                  ST1094.2
016100 01  CCVS-H-2B.                                                   ST1094.2
016200   02  FILLER                        PIC X(15)  VALUE             ST1094.2
016300            "TEST RESULT OF ".                                    ST1094.2
016400   02  TEST-ID                       PIC X(9).                    ST1094.2
016500   02  FILLER                        PIC X(4)   VALUE             ST1094.2
016600            " IN ".                                               ST1094.2
016700   02  FILLER                        PIC X(12)  VALUE             ST1094.2
016800     " HIGH       ".                                              ST1094.2
016900   02  FILLER                        PIC X(22)  VALUE             ST1094.2
017000            " LEVEL VALIDATION FOR ".                             ST1094.2
017100   02  FILLER                        PIC X(58)  VALUE             ST1094.2
017200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1094.2
017300 01  CCVS-H-3.                                                    ST1094.2
017400     02  FILLER                      PIC X(34)  VALUE             ST1094.2
017500            " FOR OFFICIAL USE ONLY    ".                         ST1094.2
017600     02  FILLER                      PIC X(58)  VALUE             ST1094.2
017700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1094.2
017800     02  FILLER                      PIC X(28)  VALUE             ST1094.2
017900            "  COPYRIGHT   1985 ".                                ST1094.2
018000 01  CCVS-E-1.                                                    ST1094.2
018100     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1094.2
018200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1094.2
018300     02 ID-AGAIN                     PIC X(9).                    ST1094.2
018400     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1094.2
018500 01  CCVS-E-2.                                                    ST1094.2
018600     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1094.2
018700     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1094.2
018800     02 CCVS-E-2-2.                                               ST1094.2
018900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1094.2
019000         03 FILLER                   PIC X      VALUE SPACE.      ST1094.2
019100         03 ENDER-DESC               PIC X(44)  VALUE             ST1094.2
019200            "ERRORS ENCOUNTERED".                                 ST1094.2
019300 01  CCVS-E-3.                                                    ST1094.2
019400     02  FILLER                      PIC X(22)  VALUE             ST1094.2
019500            " FOR OFFICIAL USE ONLY".                             ST1094.2
019600     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1094.2
019700     02  FILLER                      PIC X(58)  VALUE             ST1094.2
019800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1094.2
019900     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1094.2
020000     02 FILLER                       PIC X(15)  VALUE             ST1094.2
020100             " COPYRIGHT 1985".                                   ST1094.2
020200 01  CCVS-E-4.                                                    ST1094.2
020300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1094.2
020400     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1094.2
020500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1094.2
020600     02 FILLER                       PIC X(40)  VALUE             ST1094.2
020700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1094.2
020800 01  XXINFO.                                                      ST1094.2
020900     02 FILLER                       PIC X(19)  VALUE             ST1094.2
021000            "*** INFORMATION ***".                                ST1094.2
021100     02 INFO-TEXT.                                                ST1094.2
021200       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1094.2
021300       04 XXCOMPUTED                 PIC X(20).                   ST1094.2
021400       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1094.2
021500       04 XXCORRECT                  PIC X(20).                   ST1094.2
021600     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1094.2
021700 01  HYPHEN-LINE.                                                 ST1094.2
021800     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1094.2
021900     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1094.2
022000-    "*****************************************".                 ST1094.2
022100     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1094.2
022200-    "******************************".                            ST1094.2
022300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1094.2
022400     "ST109A".                                                    ST1094.2
022500 PROCEDURE DIVISION.                                              ST1094.2
022600 CCVS1 SECTION.                                                   ST1094.2
022700 OPEN-FILES.                                                      ST1094.2
022800     OPEN     OUTPUT PRINT-FILE.                                  ST1094.2
022900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1094.2
023000     MOVE    SPACE TO TEST-RESULTS.                               ST1094.2
023100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1094.2
023200     GO TO CCVS1-EXIT.                                            ST1094.2
023300 CLOSE-FILES.                                                     ST1094.2
023400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1094.2
023500 TERMINATE-CCVS.                                                  ST1094.2
023600*    EXIT PROGRAM.                                                ST1094.2
023700*TERMINATE-CALL.                                                  ST1094.2
023800     STOP     RUN.                                                ST1094.2
023900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1094.2
024000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1094.2
024100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1094.2
024200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1094.2
024300     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1094.2
024400 PRINT-DETAIL.                                                    ST1094.2
024500     IF REC-CT NOT EQUAL TO ZERO                                  ST1094.2
024600             MOVE "." TO PARDOT-X                                 ST1094.2
024700             MOVE REC-CT TO DOTVALUE.                             ST1094.2
024800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1094.2
024900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1094.2
025000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1094.2
025100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1094.2
025200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1094.2
025300     MOVE SPACE TO CORRECT-X.                                     ST1094.2
025400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1094.2
025500     MOVE     SPACE TO RE-MARK.                                   ST1094.2
025600 HEAD-ROUTINE.                                                    ST1094.2
025700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1094.2
025800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1094.2
025900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1094.2
026000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1094.2
026100 COLUMN-NAMES-ROUTINE.                                            ST1094.2
026200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1094.2
026300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1094.2
026400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1094.2
026500 END-ROUTINE.                                                     ST1094.2
026600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1094.2
026700 END-RTN-EXIT.                                                    ST1094.2
026800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1094.2
026900 END-ROUTINE-1.                                                   ST1094.2
027000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1094.2
027100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1094.2
027200      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1094.2
027300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1094.2
027400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1094.2
027500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1094.2
027600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1094.2
027700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1094.2
027800  END-ROUTINE-12.                                                 ST1094.2
027900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1094.2
028000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1094.2
028100         MOVE "NO " TO ERROR-TOTAL                                ST1094.2
028200         ELSE                                                     ST1094.2
028300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1094.2
028400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1094.2
028500     PERFORM WRITE-LINE.                                          ST1094.2
028600 END-ROUTINE-13.                                                  ST1094.2
028700     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1094.2
028800         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1094.2
028900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1094.2
029000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1094.2
029100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1094.2
029200      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1094.2
029300          MOVE "NO " TO ERROR-TOTAL                               ST1094.2
029400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1094.2
029500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1094.2
029600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1094.2
029700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1094.2
029800 WRITE-LINE.                                                      ST1094.2
029900     ADD 1 TO RECORD-COUNT.                                       ST1094.2
030000*    IF RECORD-COUNT GREATER 42                                   ST1094.2
030100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1094.2
030200*        MOVE SPACE TO DUMMY-RECORD                               ST1094.2
030300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1094.2
030400*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1094.2
030500*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1094.2
030600*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1094.2
030700*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1094.2
030800*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1094.2
030900*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1094.2
031000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1094.2
031100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1094.2
031200*        MOVE ZERO TO RECORD-COUNT.                               ST1094.2
031300     PERFORM WRT-LN.                                              ST1094.2
031400 WRT-LN.                                                          ST1094.2
031500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1094.2
031600     MOVE SPACE TO DUMMY-RECORD.                                  ST1094.2
031700 BLANK-LINE-PRINT.                                                ST1094.2
031800     PERFORM WRT-LN.                                              ST1094.2
031900 FAIL-ROUTINE.                                                    ST1094.2
032000     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1094.2
032100            GO TO   FAIL-ROUTINE-WRITE.                           ST1094.2
032200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1094.2
032300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1094.2
032400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1094.2
032500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1094.2
032600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1094.2
032700     GO TO  FAIL-ROUTINE-EX.                                      ST1094.2
032800 FAIL-ROUTINE-WRITE.                                              ST1094.2
032900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1094.2
033000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1094.2
033100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1094.2
033200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1094.2
033300 FAIL-ROUTINE-EX. EXIT.                                           ST1094.2
033400 BAIL-OUT.                                                        ST1094.2
033500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1094.2
033600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1094.2
033700 BAIL-OUT-WRITE.                                                  ST1094.2
033800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1094.2
033900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1094.2
034000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1094.2
034100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1094.2
034200 BAIL-OUT-EX. EXIT.                                               ST1094.2
034300 CCVS1-EXIT.                                                      ST1094.2
034400     EXIT.                                                        ST1094.2
034500 ST109-0001-01.                                                   ST1094.2
034600     OPEN     OUTPUT   SORTOUT-1I.                                ST1094.2
034700 BUILD-LOOP.                                                      ST1094.2
034800     MOVE     MEDIUM-WORK TO MEDIUM-RECORD.                       ST1094.2
034900     WRITE    MEDIUM-RECORD.                                      ST1094.2
035000     ADD      1 TO UTIL-CTR.                                      ST1094.2
035100     IF       UTIL-CTR GREATER 39                                 ST1094.2
035200              GO TO ST109-0002-01.                                ST1094.2
035300     MOVE     LONG-WORK TO LONG-RECORD.                           ST1094.2
035400     WRITE    LONG-RECORD.                                        ST1094.2
035500     ADD      1 TO UTIL-CTR.                                      ST1094.2
035600     MOVE     SHORT-WORK TO SHORT-RECORD.                         ST1094.2
035700     WRITE    SHORT-RECORD.                                       ST1094.2
035800     ADD      1 TO UTIL-CTR.                                      ST1094.2
035900     GO       TO BUILD-LOOP.                                      ST1094.2
036000 ST109-0002-01.                                                   ST1094.2
036100     MOVE    SPACES TO TEST-RESULTS.                              ST1094.2
036200     MOVE    COMMENT-SENTENCE TO TEST-RESULTS.                    ST1094.2
036300     PERFORM PRINT-DETAIL.                                        ST1094.2
036400     MOVE    SPACES TO TEST-RESULTS.                              ST1094.2
036500     CLOSE    SORTOUT-1I.                                         ST1094.2
036600 CCVS-EXIT SECTION.                                               ST1094.2
036700 CCVS-999999.                                                     ST1094.2
036800     GO TO CLOSE-FILES.                                           ST1094.2
      *END-OF,ST109A                                                                  
