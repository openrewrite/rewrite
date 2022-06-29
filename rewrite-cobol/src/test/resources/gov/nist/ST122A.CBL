      *HEADER,COBOL,ST122A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1224.2
000200 PROGRAM-ID.                                                      ST1224.2
000300     ST122A.                                                      ST1224.2
000400****************************************************************  ST1224.2
000500*                                                              *  ST1224.2
000600*    VALIDATION FOR:-                                          *  ST1224.2
000700*                                                              *  ST1224.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1224.2
000900*                                                              *  ST1224.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1224.2
001100*                                                              *  ST1224.2
001200****************************************************************  ST1224.2
001300*                                                              *  ST1224.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1224.2
001500*                                                              *  ST1224.2
001600*        X-01                                                  *  ST1224.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  ST1224.2
001800*        X-69                                                  *  ST1224.2
001900*        X-74                                                  *  ST1224.2
002000*        X-75                                                  *  ST1224.2
002100*        X-82  - SOURCE COMPUTER NAME.                         *  ST1224.2
002200*        X-83  - OBJECT COMPUTER NAME.                         *  ST1224.2
002300*                                                              *  ST1224.2
002400****************************************************************  ST1224.2
002500*   ST122 BUILDS A FILE WHICH IS SORTED IN ST123 AND CHECKED IN*  ST1224.2
002600*   ST124. THE CREATED FILE CONSISTS OF 40 RECORDS OF VARYING  *  ST1224.2
002700*   LENGTH (50, 75, 100 CHARACTERS). THE THREE RECORDS SHOWN   *  ST1224.2
002800*   BELOW REOCCUR UNTIL 40 IS REACHED.                         *  ST1224.2
002900*      NON-KEY KEY-1        KEY-2               FILLER         *  ST1224.2
003000*      X(2)     X(10)       X(38)                              *  ST1224.2
003100*                                                              *  ST1224.2
003200*      "BB"  "LOWEST TWO" "MIDDLE TWO-FIRST"  X(25) VALUE ZERO *  ST1224.2
003300*      "CC"  "LOWEST TWO" "MIDDLE TWO-SECOND" X(50) VALUE QUOTE*  ST1224.2
003400*      "AA"  "LOWEST ONE" "MIDDLE ONE-ONLY"   (NONE)           *  ST1224.2
003500*                                                              *  ST1224.2
003600*    PROGRAMS ST122A, ST123A AND ST124A WILL BE USED           *  ST1224.2
003700*    ONLY IF LEVEL 2 OF THE SEQUENTIAL I-O MODULE IS SUPPORTED *  ST1224.2
003800*    AS THE "RECORD IS VARYING IN SIZE" CLAUSE IS USED IN      *  ST1224.2
003900*    ST123A.                                                   *  ST1224.2
004000*                                                              *  ST1224.2
004100****************************************************************  ST1224.2
004200                                                                  ST1224.2
004300 ENVIRONMENT DIVISION.                                            ST1224.2
004400 CONFIGURATION SECTION.                                           ST1224.2
004500 SOURCE-COMPUTER.                                                 ST1224.2
004600     XXXXX082.                                                    ST1224.2
004700 OBJECT-COMPUTER.                                                 ST1224.2
004800     XXXXX083.                                                    ST1224.2
004900 INPUT-OUTPUT SECTION.                                            ST1224.2
005000 FILE-CONTROL.                                                    ST1224.2
005100     SELECT PRINT-FILE ASSIGN TO                                  ST1224.2
005200     XXXXX055.                                                    ST1224.2
005300     SELECT   SORTOUT-1I ASSIGN TO                                ST1224.2
005400     XXXXP001.                                                    ST1224.2
005500 DATA DIVISION.                                                   ST1224.2
005600 FILE SECTION.                                                    ST1224.2
005700 FD  PRINT-FILE.                                                  ST1224.2
005800 01  PRINT-REC PICTURE X(120).                                    ST1224.2
005900 01  DUMMY-RECORD PICTURE X(120).                                 ST1224.2
006000 FD  SORTOUT-1I                                                   ST1224.2
006100     LABEL RECORDS STANDARD                                       ST1224.2
006200C    VALUE OF                                                     ST1224.2
006300C    XXXXX074                                                     ST1224.2
006400C    IS                                                           ST1224.2
006500C    XXXXX075                                                     ST1224.2
006600*    XXXXX069                                                     ST1224.2
006700     RECORD IS VARYING IN SIZE FROM 50 TO 100 CHARACTERS          ST1224.2
006800     DATA RECORDS ARE SHORT-RECORD                                ST1224.2
006900                      MEDIUM-RECORD                               ST1224.2
007000                      LONG-RECORD.                                ST1224.2
007100 01  SHORT-RECORD       PICTURE X(50).                            ST1224.2
007200 01  MEDIUM-RECORD      PICTURE X(75).                            ST1224.2
007300 01  LONG-RECORD        PICTURE X(100).                           ST1224.2
007400 WORKING-STORAGE SECTION.                                         ST1224.2
007500 77  COMMENT-SENTENCE  PICTURE X(119) VALUE " ST122A HAS CREATED AST1224.2
007600-    "FILE OF 40 VARIABLE-LENGTH-RECORDS. THESE RECORDS WILL BE SOST1224.2
007700-    "RTED IN ST123A AND CHECKED IN ST124A.".                     ST1224.2
007800 77  UTIL-CTR           PICTURE S99999 VALUE ZERO.                ST1224.2
007900 01  SHORT-WORK.                                                  ST1224.2
008000     02 FILLER          PICTURE XX    VALUE "AA".                 ST1224.2
008100     02 FILLER          PICTURE X(10) VALUE "LOWEST ONE".         ST1224.2
008200     02 FILLER          PICTURE X(38) VALUE "MIDDLE ONE-ONLY".    ST1224.2
008300 01  MEDIUM-WORK.                                                 ST1224.2
008400     02 FILLER          PICTURE XX    VALUE "BB".                 ST1224.2
008500     02 FILLER          PICTURE X(10) VALUE "LOWEST TWO".         ST1224.2
008600     02 FILLER          PICTURE X(38) VALUE "MIDDLE TWO-FIRST".   ST1224.2
008700     02 FILLER          PICTURE X(25) VALUE ZERO.                 ST1224.2
008800 01  LONG-WORK.                                                   ST1224.2
008900     02 FILLER          PICTURE XX    VALUE "CC".                 ST1224.2
009000     02 FILLER          PICTURE X(10) VALUE "LOWEST TWO".         ST1224.2
009100     02 FILLER          PICTURE X(38) VALUE "MIDDLE TWO-SECOND".  ST1224.2
009200     02 FILLER          PICTURE X(50) VALUE QUOTE.                ST1224.2
009300 01  TEST-RESULTS.                                                ST1224.2
009400     02 FILLER                   PIC X      VALUE SPACE.          ST1224.2
009500     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1224.2
009600     02 FILLER                   PIC X      VALUE SPACE.          ST1224.2
009700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1224.2
009800     02 FILLER                   PIC X      VALUE SPACE.          ST1224.2
009900     02  PAR-NAME.                                                ST1224.2
010000       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1224.2
010100       03  PARDOT-X              PIC X      VALUE SPACE.          ST1224.2
010200       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1224.2
010300     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1224.2
010400     02 RE-MARK                  PIC X(61).                       ST1224.2
010500 01  TEST-COMPUTED.                                               ST1224.2
010600     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1224.2
010700     02 FILLER                   PIC X(17)  VALUE                 ST1224.2
010800            "       COMPUTED=".                                   ST1224.2
010900     02 COMPUTED-X.                                               ST1224.2
011000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1224.2
011100     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1224.2
011200                                 PIC -9(9).9(9).                  ST1224.2
011300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1224.2
011400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1224.2
011500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1224.2
011600     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1224.2
011700         04 COMPUTED-18V0                    PIC -9(18).          ST1224.2
011800         04 FILLER                           PIC X.               ST1224.2
011900     03 FILLER PIC X(50) VALUE SPACE.                             ST1224.2
012000 01  TEST-CORRECT.                                                ST1224.2
012100     02 FILLER PIC X(30) VALUE SPACE.                             ST1224.2
012200     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1224.2
012300     02 CORRECT-X.                                                ST1224.2
012400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1224.2
012500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1224.2
012600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1224.2
012700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1224.2
012800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1224.2
012900     03      CR-18V0 REDEFINES CORRECT-A.                         ST1224.2
013000         04 CORRECT-18V0                     PIC -9(18).          ST1224.2
013100         04 FILLER                           PIC X.               ST1224.2
013200     03 FILLER PIC X(2) VALUE SPACE.                              ST1224.2
013300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1224.2
013400 01  CCVS-C-1.                                                    ST1224.2
013500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1224.2
013600-    "SS  PARAGRAPH-NAME                                          ST1224.2
013700-    "       REMARKS".                                            ST1224.2
013800     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1224.2
013900 01  CCVS-C-2.                                                    ST1224.2
014000     02 FILLER                     PIC X        VALUE SPACE.      ST1224.2
014100     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1224.2
014200     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1224.2
014300     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1224.2
014400     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1224.2
014500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1224.2
014600 01  REC-CT                        PIC 99       VALUE ZERO.       ST1224.2
014700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1224.2
014800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1224.2
014900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1224.2
015000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1224.2
015100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1224.2
015200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1224.2
015300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1224.2
015400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1224.2
015500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1224.2
015600 01  CCVS-H-1.                                                    ST1224.2
015700     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1224.2
015800     02  FILLER                    PIC X(42)    VALUE             ST1224.2
015900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1224.2
016000     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1224.2
016100 01  CCVS-H-2A.                                                   ST1224.2
016200   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1224.2
016300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1224.2
016400   02  FILLER                        PIC XXXX   VALUE             ST1224.2
016500     "4.2 ".                                                      ST1224.2
016600   02  FILLER                        PIC X(28)  VALUE             ST1224.2
016700            " COPY - NOT FOR DISTRIBUTION".                       ST1224.2
016800   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1224.2
016900                                                                  ST1224.2
017000 01  CCVS-H-2B.                                                   ST1224.2
017100   02  FILLER                        PIC X(15)  VALUE             ST1224.2
017200            "TEST RESULT OF ".                                    ST1224.2
017300   02  TEST-ID                       PIC X(9).                    ST1224.2
017400   02  FILLER                        PIC X(4)   VALUE             ST1224.2
017500            " IN ".                                               ST1224.2
017600   02  FILLER                        PIC X(12)  VALUE             ST1224.2
017700     " HIGH       ".                                              ST1224.2
017800   02  FILLER                        PIC X(22)  VALUE             ST1224.2
017900            " LEVEL VALIDATION FOR ".                             ST1224.2
018000   02  FILLER                        PIC X(58)  VALUE             ST1224.2
018100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1224.2
018200 01  CCVS-H-3.                                                    ST1224.2
018300     02  FILLER                      PIC X(34)  VALUE             ST1224.2
018400            " FOR OFFICIAL USE ONLY    ".                         ST1224.2
018500     02  FILLER                      PIC X(58)  VALUE             ST1224.2
018600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1224.2
018700     02  FILLER                      PIC X(28)  VALUE             ST1224.2
018800            "  COPYRIGHT   1985 ".                                ST1224.2
018900 01  CCVS-E-1.                                                    ST1224.2
019000     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1224.2
019100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1224.2
019200     02 ID-AGAIN                     PIC X(9).                    ST1224.2
019300     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1224.2
019400 01  CCVS-E-2.                                                    ST1224.2
019500     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1224.2
019600     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1224.2
019700     02 CCVS-E-2-2.                                               ST1224.2
019800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1224.2
019900         03 FILLER                   PIC X      VALUE SPACE.      ST1224.2
020000         03 ENDER-DESC               PIC X(44)  VALUE             ST1224.2
020100            "ERRORS ENCOUNTERED".                                 ST1224.2
020200 01  CCVS-E-3.                                                    ST1224.2
020300     02  FILLER                      PIC X(22)  VALUE             ST1224.2
020400            " FOR OFFICIAL USE ONLY".                             ST1224.2
020500     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1224.2
020600     02  FILLER                      PIC X(58)  VALUE             ST1224.2
020700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1224.2
020800     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1224.2
020900     02 FILLER                       PIC X(15)  VALUE             ST1224.2
021000             " COPYRIGHT 1985".                                   ST1224.2
021100 01  CCVS-E-4.                                                    ST1224.2
021200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1224.2
021300     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1224.2
021400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1224.2
021500     02 FILLER                       PIC X(40)  VALUE             ST1224.2
021600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1224.2
021700 01  XXINFO.                                                      ST1224.2
021800     02 FILLER                       PIC X(19)  VALUE             ST1224.2
021900            "*** INFORMATION ***".                                ST1224.2
022000     02 INFO-TEXT.                                                ST1224.2
022100       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1224.2
022200       04 XXCOMPUTED                 PIC X(20).                   ST1224.2
022300       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1224.2
022400       04 XXCORRECT                  PIC X(20).                   ST1224.2
022500     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1224.2
022600 01  HYPHEN-LINE.                                                 ST1224.2
022700     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1224.2
022800     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1224.2
022900-    "*****************************************".                 ST1224.2
023000     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1224.2
023100-    "******************************".                            ST1224.2
023200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1224.2
023300     "ST122A".                                                    ST1224.2
023400 PROCEDURE DIVISION.                                              ST1224.2
023500 CCVS1 SECTION.                                                   ST1224.2
023600 OPEN-FILES.                                                      ST1224.2
023700     OPEN     OUTPUT PRINT-FILE.                                  ST1224.2
023800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1224.2
023900     MOVE    SPACE TO TEST-RESULTS.                               ST1224.2
024000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1224.2
024100     GO TO CCVS1-EXIT.                                            ST1224.2
024200 CLOSE-FILES.                                                     ST1224.2
024300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1224.2
024400 TERMINATE-CCVS.                                                  ST1224.2
024500*    EXIT PROGRAM.                                                ST1224.2
024600*TERMINATE-CALL.                                                  ST1224.2
024700     STOP     RUN.                                                ST1224.2
024800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1224.2
024900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1224.2
025000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1224.2
025100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1224.2
025200     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1224.2
025300 PRINT-DETAIL.                                                    ST1224.2
025400     IF REC-CT NOT EQUAL TO ZERO                                  ST1224.2
025500             MOVE "." TO PARDOT-X                                 ST1224.2
025600             MOVE REC-CT TO DOTVALUE.                             ST1224.2
025700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1224.2
025800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1224.2
025900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1224.2
026000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1224.2
026100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1224.2
026200     MOVE SPACE TO CORRECT-X.                                     ST1224.2
026300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1224.2
026400     MOVE     SPACE TO RE-MARK.                                   ST1224.2
026500 HEAD-ROUTINE.                                                    ST1224.2
026600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1224.2
026700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1224.2
026800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1224.2
026900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1224.2
027000 COLUMN-NAMES-ROUTINE.                                            ST1224.2
027100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1224.2
027200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1224.2
027300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1224.2
027400 END-ROUTINE.                                                     ST1224.2
027500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1224.2
027600 END-RTN-EXIT.                                                    ST1224.2
027700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1224.2
027800 END-ROUTINE-1.                                                   ST1224.2
027900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1224.2
028000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1224.2
028100      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1224.2
028200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1224.2
028300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1224.2
028400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1224.2
028500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1224.2
028600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1224.2
028700  END-ROUTINE-12.                                                 ST1224.2
028800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1224.2
028900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1224.2
029000         MOVE "NO " TO ERROR-TOTAL                                ST1224.2
029100         ELSE                                                     ST1224.2
029200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1224.2
029300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1224.2
029400     PERFORM WRITE-LINE.                                          ST1224.2
029500 END-ROUTINE-13.                                                  ST1224.2
029600     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1224.2
029700         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1224.2
029800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1224.2
029900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1224.2
030000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1224.2
030100      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1224.2
030200          MOVE "NO " TO ERROR-TOTAL                               ST1224.2
030300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1224.2
030400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1224.2
030500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1224.2
030600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1224.2
030700 WRITE-LINE.                                                      ST1224.2
030800     ADD 1 TO RECORD-COUNT.                                       ST1224.2
030900*    IF RECORD-COUNT GREATER 42                                   ST1224.2
031000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1224.2
031100*        MOVE SPACE TO DUMMY-RECORD                               ST1224.2
031200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1224.2
031300*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1224.2
031400*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1224.2
031500*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1224.2
031600*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1224.2
031700*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1224.2
031800*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1224.2
031900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1224.2
032000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1224.2
032100*        MOVE ZERO TO RECORD-COUNT.                               ST1224.2
032200     PERFORM WRT-LN.                                              ST1224.2
032300 WRT-LN.                                                          ST1224.2
032400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1224.2
032500     MOVE SPACE TO DUMMY-RECORD.                                  ST1224.2
032600 BLANK-LINE-PRINT.                                                ST1224.2
032700     PERFORM WRT-LN.                                              ST1224.2
032800 FAIL-ROUTINE.                                                    ST1224.2
032900     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1224.2
033000            GO TO   FAIL-ROUTINE-WRITE.                           ST1224.2
033100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1224.2
033200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1224.2
033300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1224.2
033400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1224.2
033500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1224.2
033600     GO TO  FAIL-ROUTINE-EX.                                      ST1224.2
033700 FAIL-ROUTINE-WRITE.                                              ST1224.2
033800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1224.2
033900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1224.2
034000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1224.2
034100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1224.2
034200 FAIL-ROUTINE-EX. EXIT.                                           ST1224.2
034300 BAIL-OUT.                                                        ST1224.2
034400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1224.2
034500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1224.2
034600 BAIL-OUT-WRITE.                                                  ST1224.2
034700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1224.2
034800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1224.2
034900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1224.2
035000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1224.2
035100 BAIL-OUT-EX. EXIT.                                               ST1224.2
035200 CCVS1-EXIT.                                                      ST1224.2
035300     EXIT.                                                        ST1224.2
035400 ST122A-0001-01.                                                  ST1224.2
035500     OPEN     OUTPUT   SORTOUT-1I.                                ST1224.2
035600 BUILD-LOOP.                                                      ST1224.2
035700     MOVE     MEDIUM-WORK TO MEDIUM-RECORD.                       ST1224.2
035800     WRITE    MEDIUM-RECORD.                                      ST1224.2
035900     ADD      1 TO UTIL-CTR.                                      ST1224.2
036000     IF       UTIL-CTR GREATER 39                                 ST1224.2
036100              GO TO ST122A-0002-01.                               ST1224.2
036200     MOVE     LONG-WORK TO LONG-RECORD.                           ST1224.2
036300     WRITE    LONG-RECORD.                                        ST1224.2
036400     ADD      1 TO UTIL-CTR.                                      ST1224.2
036500     MOVE     SHORT-WORK TO SHORT-RECORD.                         ST1224.2
036600     WRITE    SHORT-RECORD.                                       ST1224.2
036700     ADD      1 TO UTIL-CTR.                                      ST1224.2
036800     GO       TO BUILD-LOOP.                                      ST1224.2
036900 ST122A-0002-01.                                                  ST1224.2
037000     MOVE    SPACES TO TEST-RESULTS.                              ST1224.2
037100     MOVE    COMMENT-SENTENCE TO TEST-RESULTS.                    ST1224.2
037200     PERFORM PRINT-DETAIL.                                        ST1224.2
037300     MOVE    SPACES TO TEST-RESULTS.                              ST1224.2
037400     CLOSE    SORTOUT-1I.                                         ST1224.2
037500 CCVS-EXIT SECTION.                                               ST1224.2
037600 CCVS-999999.                                                     ST1224.2
037700     GO TO CLOSE-FILES.                                           ST1224.2
      *END-OF,ST122A                                                                  
