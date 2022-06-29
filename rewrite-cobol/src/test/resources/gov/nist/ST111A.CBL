      *HEADER,COBOL,ST109A,SUBPRG,ST111A                                              
000100 IDENTIFICATION DIVISION.                                         ST1114.2
000200 PROGRAM-ID.                                                      ST1114.2
000300     ST111A.                                                      ST1114.2
000400****************************************************************  ST1114.2
000500*                                                              *  ST1114.2
000600*    VALIDATION FOR:-                                          *  ST1114.2
000700*                                                              *  ST1114.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1114.2
000900*                                                              *  ST1114.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1114.2
001100*                                                              *  ST1114.2
001200****************************************************************  ST1114.2
001300*                                                              *  ST1114.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1114.2
001500*                                                              *  ST1114.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1114.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1114.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1114.2
001900*                                                              *  ST1114.2
002000****************************************************************  ST1114.2
002100*    ST111 CHECKS THE OUTPUT FROM ST110. WHICH IN TURN USED INPUT ST1114.2
002200*    FROM ST109. 40 VARIABLE-LENGTH RECORDS HAVE BEEN SORTED AND  ST1114.2
002300*    SHOULD APPEAR AS SHOWN                                       ST1114.2
002400*        NON-KEY KEY-1       KEY-2               FILLER           ST1114.2
002500*           X(2) X(10)       X(38)                                ST1114.2
002600*                                                                 ST1114.2
002700*    FIRST 13 RECORDS ---                                         ST1114.2
002800*            "CC""LOWEST TWO""MIDDLE TWO-SECOND" X(50) VALUE QUOTEST1114.2
002900*    NEXT 14 RECORDS ---                                          ST1114.2
003000*            "BB""LOWEST TWO""MIDDLE TWO-FIRST"  X(25) VALUE ZERO ST1114.2
003100*    LAST 13 RECORDS ---                                          ST1114.2
003200*            "AA""LOWEST ONE""MIDDLE ONE-ONLY"         (NONE)     ST1114.2
003300                                                                  ST1114.2
003400 ENVIRONMENT DIVISION.                                            ST1114.2
003500 CONFIGURATION SECTION.                                           ST1114.2
003600 SOURCE-COMPUTER.                                                 ST1114.2
003700     XXXXX082.                                                    ST1114.2
003800 OBJECT-COMPUTER.                                                 ST1114.2
003900     XXXXX083.                                                    ST1114.2
004000 INPUT-OUTPUT SECTION.                                            ST1114.2
004100 FILE-CONTROL.                                                    ST1114.2
004200     SELECT PRINT-FILE ASSIGN TO                                  ST1114.2
004300     XXXXX055.                                                    ST1114.2
004400     SELECT   SORTIN-1K ASSIGN TO                                 ST1114.2
004500     XXXXP002.                                                    ST1114.2
004600 DATA DIVISION.                                                   ST1114.2
004700 FILE SECTION.                                                    ST1114.2
004800 FD  PRINT-FILE.                                                  ST1114.2
004900 01  PRINT-REC PICTURE X(120).                                    ST1114.2
005000 01  DUMMY-RECORD PICTURE X(120).                                 ST1114.2
005100 FD  SORTIN-1K                                                    ST1114.2
005200     LABEL RECORDS STANDARD                                       ST1114.2
005300C    VALUE OF                                                     ST1114.2
005400C    XXXXX074                                                     ST1114.2
005500C    IS                                                           ST1114.2
005600C    XXXXX076                                                     ST1114.2
005700*    XXXXX069                                                     ST1114.2
005800     RECORD CONTAINS 50 TO 100 CHARACTERS                         ST1114.2
005900     DATA RECORDS ARE SHORT-RECORD                                ST1114.2
006000                      MEDIUM-RECORD                               ST1114.2
006100                      LONG-RECORD.                                ST1114.2
006200 01  SHORT-RECORD       PICTURE X(50).                            ST1114.2
006300 01  MEDIUM-RECORD      PICTURE X(75).                            ST1114.2
006400 01  LONG-RECORD        PICTURE X(100).                           ST1114.2
006500 WORKING-STORAGE SECTION.                                         ST1114.2
006600 77  UTIL-CTR           PICTURE S99999 VALUE ZERO.                ST1114.2
006700 01  SHORT-WORK.                                                  ST1114.2
006800     02 FILLER          PICTURE XX VALUE "AA".                    ST1114.2
006900     02 FILLER          PICTURE X(10) VALUE "LOWEST ONE".         ST1114.2
007000     02 FILLER          PICTURE X(38)                             ST1114.2
007100     VALUE "MIDDLE ONE-ONLY                       ".              ST1114.2
007200 01  MEDIUM-WORK.                                                 ST1114.2
007300     02 FILLER          PICTURE XX VALUE "BB".                    ST1114.2
007400     02 FILLER          PICTURE X(10) VALUE "LOWEST TWO".         ST1114.2
007500     02 FILLER          PICTURE X(38)                             ST1114.2
007600     VALUE "MIDDLE TWO-FIRST                      ".              ST1114.2
007700     02 FILLER          PICTURE X(25) VALUE ZERO.                 ST1114.2
007800 01  LONG-WORK.                                                   ST1114.2
007900     02 FILLER         PICTURE XX VALUE "CC".                     ST1114.2
008000     02 FILLER         PICTURE X(10) VALUE "LOWEST TWO".          ST1114.2
008100     02 FILLER          PICTURE X(38)                             ST1114.2
008200     VALUE "MIDDLE TWO-SECOND                     ".              ST1114.2
008300     02  FILLER        PICTURE X(50) VALUE QUOTE.                 ST1114.2
008400 01  BREAKDOWN-LIMIT    PICTURE 999.                              ST1114.2
008500 01  COMPUTED-BREAKDOWN.                                          ST1114.2
008600     02 FIRST-20-CM     PICTURE X(20).                            ST1114.2
008700     02 SECOND-20-CM    PICTURE X(20).                            ST1114.2
008800     02 THIRD-20-CM     PICTURE X(20).                            ST1114.2
008900     02 FOURTH-20-CM    PICTURE X(20).                            ST1114.2
009000     02 FIFTH-20-CM     PICTURE X(20).                            ST1114.2
009100 01  CORRECT-BREAKDOWN.                                           ST1114.2
009200     02 FIRST-20-CR     PICTURE X(20).                            ST1114.2
009300     02 SECOND-20-CR    PICTURE X(20).                            ST1114.2
009400     02 THIRD-20-CR     PICTURE X(20).                            ST1114.2
009500     02 FOURTH-20-CR    PICTURE X(20).                            ST1114.2
009600     02 FIFTH-20-CR     PICTURE X(20).                            ST1114.2
009700 01  TEST-RESULTS.                                                ST1114.2
009800     02 FILLER                   PIC X      VALUE SPACE.          ST1114.2
009900     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1114.2
010000     02 FILLER                   PIC X      VALUE SPACE.          ST1114.2
010100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1114.2
010200     02 FILLER                   PIC X      VALUE SPACE.          ST1114.2
010300     02  PAR-NAME.                                                ST1114.2
010400       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1114.2
010500       03  PARDOT-X              PIC X      VALUE SPACE.          ST1114.2
010600       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1114.2
010700     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1114.2
010800     02 RE-MARK                  PIC X(61).                       ST1114.2
010900 01  TEST-COMPUTED.                                               ST1114.2
011000     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1114.2
011100     02 FILLER                   PIC X(17)  VALUE                 ST1114.2
011200            "       COMPUTED=".                                   ST1114.2
011300     02 COMPUTED-X.                                               ST1114.2
011400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1114.2
011500     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1114.2
011600                                 PIC -9(9).9(9).                  ST1114.2
011700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1114.2
011800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1114.2
011900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1114.2
012000     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1114.2
012100         04 COMPUTED-18V0                    PIC -9(18).          ST1114.2
012200         04 FILLER                           PIC X.               ST1114.2
012300     03 FILLER PIC X(50) VALUE SPACE.                             ST1114.2
012400 01  TEST-CORRECT.                                                ST1114.2
012500     02 FILLER PIC X(30) VALUE SPACE.                             ST1114.2
012600     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1114.2
012700     02 CORRECT-X.                                                ST1114.2
012800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1114.2
012900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1114.2
013000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1114.2
013100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1114.2
013200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1114.2
013300     03      CR-18V0 REDEFINES CORRECT-A.                         ST1114.2
013400         04 CORRECT-18V0                     PIC -9(18).          ST1114.2
013500         04 FILLER                           PIC X.               ST1114.2
013600     03 FILLER PIC X(2) VALUE SPACE.                              ST1114.2
013700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1114.2
013800 01  CCVS-C-1.                                                    ST1114.2
013900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1114.2
014000-    "SS  PARAGRAPH-NAME                                          ST1114.2
014100-    "       REMARKS".                                            ST1114.2
014200     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1114.2
014300 01  CCVS-C-2.                                                    ST1114.2
014400     02 FILLER                     PIC X        VALUE SPACE.      ST1114.2
014500     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1114.2
014600     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1114.2
014700     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1114.2
014800     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1114.2
014900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1114.2
015000 01  REC-CT                        PIC 99       VALUE ZERO.       ST1114.2
015100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1114.2
015200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1114.2
015300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1114.2
015400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1114.2
015500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1114.2
015600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1114.2
015700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1114.2
015800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1114.2
015900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1114.2
016000 01  CCVS-H-1.                                                    ST1114.2
016100     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1114.2
016200     02  FILLER                    PIC X(42)    VALUE             ST1114.2
016300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1114.2
016400     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1114.2
016500 01  CCVS-H-2A.                                                   ST1114.2
016600   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1114.2
016700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1114.2
016800   02  FILLER                        PIC XXXX   VALUE             ST1114.2
016900     "4.2 ".                                                      ST1114.2
017000   02  FILLER                        PIC X(28)  VALUE             ST1114.2
017100            " COPY - NOT FOR DISTRIBUTION".                       ST1114.2
017200   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1114.2
017300                                                                  ST1114.2
017400 01  CCVS-H-2B.                                                   ST1114.2
017500   02  FILLER                        PIC X(15)  VALUE             ST1114.2
017600            "TEST RESULT OF ".                                    ST1114.2
017700   02  TEST-ID                       PIC X(9).                    ST1114.2
017800   02  FILLER                        PIC X(4)   VALUE             ST1114.2
017900            " IN ".                                               ST1114.2
018000   02  FILLER                        PIC X(12)  VALUE             ST1114.2
018100     " HIGH       ".                                              ST1114.2
018200   02  FILLER                        PIC X(22)  VALUE             ST1114.2
018300            " LEVEL VALIDATION FOR ".                             ST1114.2
018400   02  FILLER                        PIC X(58)  VALUE             ST1114.2
018500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1114.2
018600 01  CCVS-H-3.                                                    ST1114.2
018700     02  FILLER                      PIC X(34)  VALUE             ST1114.2
018800            " FOR OFFICIAL USE ONLY    ".                         ST1114.2
018900     02  FILLER                      PIC X(58)  VALUE             ST1114.2
019000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1114.2
019100     02  FILLER                      PIC X(28)  VALUE             ST1114.2
019200            "  COPYRIGHT   1985 ".                                ST1114.2
019300 01  CCVS-E-1.                                                    ST1114.2
019400     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1114.2
019500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1114.2
019600     02 ID-AGAIN                     PIC X(9).                    ST1114.2
019700     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1114.2
019800 01  CCVS-E-2.                                                    ST1114.2
019900     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1114.2
020000     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1114.2
020100     02 CCVS-E-2-2.                                               ST1114.2
020200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1114.2
020300         03 FILLER                   PIC X      VALUE SPACE.      ST1114.2
020400         03 ENDER-DESC               PIC X(44)  VALUE             ST1114.2
020500            "ERRORS ENCOUNTERED".                                 ST1114.2
020600 01  CCVS-E-3.                                                    ST1114.2
020700     02  FILLER                      PIC X(22)  VALUE             ST1114.2
020800            " FOR OFFICIAL USE ONLY".                             ST1114.2
020900     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1114.2
021000     02  FILLER                      PIC X(58)  VALUE             ST1114.2
021100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1114.2
021200     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1114.2
021300     02 FILLER                       PIC X(15)  VALUE             ST1114.2
021400             " COPYRIGHT 1985".                                   ST1114.2
021500 01  CCVS-E-4.                                                    ST1114.2
021600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1114.2
021700     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1114.2
021800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1114.2
021900     02 FILLER                       PIC X(40)  VALUE             ST1114.2
022000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1114.2
022100 01  XXINFO.                                                      ST1114.2
022200     02 FILLER                       PIC X(19)  VALUE             ST1114.2
022300            "*** INFORMATION ***".                                ST1114.2
022400     02 INFO-TEXT.                                                ST1114.2
022500       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1114.2
022600       04 XXCOMPUTED                 PIC X(20).                   ST1114.2
022700       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1114.2
022800       04 XXCORRECT                  PIC X(20).                   ST1114.2
022900     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1114.2
023000 01  HYPHEN-LINE.                                                 ST1114.2
023100     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1114.2
023200     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1114.2
023300-    "*****************************************".                 ST1114.2
023400     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1114.2
023500-    "******************************".                            ST1114.2
023600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1114.2
023700     "ST111A".                                                    ST1114.2
023800 PROCEDURE DIVISION.                                              ST1114.2
023900 CCVS1 SECTION.                                                   ST1114.2
024000 OPEN-FILES.                                                      ST1114.2
024100     OPEN     OUTPUT PRINT-FILE.                                  ST1114.2
024200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1114.2
024300     MOVE    SPACE TO TEST-RESULTS.                               ST1114.2
024400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1114.2
024500     GO TO CCVS1-EXIT.                                            ST1114.2
024600 CLOSE-FILES.                                                     ST1114.2
024700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1114.2
024800 TERMINATE-CCVS.                                                  ST1114.2
024900*    EXIT PROGRAM.                                                ST1114.2
025000*TERMINATE-CALL.                                                  ST1114.2
025100     STOP     RUN.                                                ST1114.2
025200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1114.2
025300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1114.2
025400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1114.2
025500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1114.2
025600     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1114.2
025700 PRINT-DETAIL.                                                    ST1114.2
025800     IF REC-CT NOT EQUAL TO ZERO                                  ST1114.2
025900             MOVE "." TO PARDOT-X                                 ST1114.2
026000             MOVE REC-CT TO DOTVALUE.                             ST1114.2
026100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1114.2
026200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1114.2
026300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1114.2
026400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1114.2
026500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1114.2
026600     MOVE SPACE TO CORRECT-X.                                     ST1114.2
026700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1114.2
026800     MOVE     SPACE TO RE-MARK.                                   ST1114.2
026900 HEAD-ROUTINE.                                                    ST1114.2
027000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1114.2
027100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1114.2
027200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1114.2
027300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1114.2
027400 COLUMN-NAMES-ROUTINE.                                            ST1114.2
027500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1114.2
027600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1114.2
027700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1114.2
027800 END-ROUTINE.                                                     ST1114.2
027900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1114.2
028000 END-RTN-EXIT.                                                    ST1114.2
028100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1114.2
028200 END-ROUTINE-1.                                                   ST1114.2
028300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1114.2
028400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1114.2
028500      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1114.2
028600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1114.2
028700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1114.2
028800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1114.2
028900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1114.2
029000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1114.2
029100  END-ROUTINE-12.                                                 ST1114.2
029200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1114.2
029300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1114.2
029400         MOVE "NO " TO ERROR-TOTAL                                ST1114.2
029500         ELSE                                                     ST1114.2
029600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1114.2
029700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1114.2
029800     PERFORM WRITE-LINE.                                          ST1114.2
029900 END-ROUTINE-13.                                                  ST1114.2
030000     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1114.2
030100         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1114.2
030200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1114.2
030300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1114.2
030400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1114.2
030500      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1114.2
030600          MOVE "NO " TO ERROR-TOTAL                               ST1114.2
030700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1114.2
030800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1114.2
030900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1114.2
031000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1114.2
031100 WRITE-LINE.                                                      ST1114.2
031200     ADD 1 TO RECORD-COUNT.                                       ST1114.2
031300*    IF RECORD-COUNT GREATER 42                                   ST1114.2
031400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1114.2
031500*        MOVE SPACE TO DUMMY-RECORD                               ST1114.2
031600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1114.2
031700*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1114.2
031800*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1114.2
031900*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1114.2
032000*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1114.2
032100*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1114.2
032200*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1114.2
032300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1114.2
032400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1114.2
032500*        MOVE ZERO TO RECORD-COUNT.                               ST1114.2
032600     PERFORM WRT-LN.                                              ST1114.2
032700 WRT-LN.                                                          ST1114.2
032800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1114.2
032900     MOVE SPACE TO DUMMY-RECORD.                                  ST1114.2
033000 BLANK-LINE-PRINT.                                                ST1114.2
033100     PERFORM WRT-LN.                                              ST1114.2
033200 FAIL-ROUTINE.                                                    ST1114.2
033300     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1114.2
033400            GO TO   FAIL-ROUTINE-WRITE.                           ST1114.2
033500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1114.2
033600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1114.2
033700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1114.2
033800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1114.2
033900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1114.2
034000     GO TO  FAIL-ROUTINE-EX.                                      ST1114.2
034100 FAIL-ROUTINE-WRITE.                                              ST1114.2
034200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1114.2
034300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1114.2
034400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1114.2
034500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1114.2
034600 FAIL-ROUTINE-EX. EXIT.                                           ST1114.2
034700 BAIL-OUT.                                                        ST1114.2
034800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1114.2
034900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1114.2
035000 BAIL-OUT-WRITE.                                                  ST1114.2
035100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1114.2
035200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1114.2
035300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1114.2
035400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1114.2
035500 BAIL-OUT-EX. EXIT.                                               ST1114.2
035600 CCVS1-EXIT.                                                      ST1114.2
035700     EXIT.                                                        ST1114.2
035800 SECT-ST111-0001 SECTION.                                         ST1114.2
035900 ST111-0001-01.                                                   ST1114.2
036000     OPEN     INPUT  SORTIN-1K.                                   ST1114.2
036100     MOVE    " *****  ST110 DOES NOT PRODUCE A PRINTED REPORT  ***ST1114.2
036200-    "**" TO TEST-RESULTS.                                        ST1114.2
036300     PERFORM PRINT-DETAIL.                                        ST1114.2
036400     MOVE    SPACE TO TEST-RESULTS.                               ST1114.2
036500     PERFORM END-ROUTINE.                                         ST1114.2
036600     MOVE     "SORT VARIABLE RECORD" TO FEATURE.                  ST1114.2
036700 SORT-TEST-1.                                                     ST1114.2
036800     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1114.2
036900     PERFORM  READ-SORTIN.                                        ST1114.2
037000     IF       LONG-RECORD EQUAL TO LONG-WORK                      ST1114.2
037100              PERFORM PASS GO TO SORT-WRITE-1.                    ST1114.2
037200*        NOTE FIRST RECORD.                                       ST1114.2
037300 SORT-FAIL-1.                                                     ST1114.2
037400     MOVE     100 TO BREAKDOWN-LIMIT.                             ST1114.2
037500     MOVE     LONG-RECORD TO COMPUTED-BREAKDOWN.                  ST1114.2
037600     MOVE     LONG-WORK TO CORRECT-BREAKDOWN.                     ST1114.2
037700     PERFORM  BREAKDOWN-PARA THRU BREAKDOWN-EXIT.                 ST1114.2
037800 SORT-WRITE-1.                                                    ST1114.2
037900     PERFORM  PRINT-DETAIL.                                       ST1114.2
038000 SORT-TEST-2.                                                     ST1114.2
038100     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1114.2
038200     PERFORM  READ-SORTIN 12 TIMES.                               ST1114.2
038300     IF       LONG-RECORD EQUAL TO LONG-WORK                      ST1114.2
038400              PERFORM PASS GO TO SORT-WRITE-2.                    ST1114.2
038500*        NOTE THIRTEENTH RECORD.                                  ST1114.2
038600 SORT-FAIL-2.                                                     ST1114.2
038700     MOVE     100 TO BREAKDOWN-LIMIT.                             ST1114.2
038800     MOVE     LONG-RECORD TO COMPUTED-BREAKDOWN.                  ST1114.2
038900     MOVE     LONG-WORK TO CORRECT-BREAKDOWN.                     ST1114.2
039000     PERFORM  BREAKDOWN-PARA THRU BREAKDOWN-EXIT.                 ST1114.2
039100 SORT-WRITE-2.                                                    ST1114.2
039200     PERFORM  PRINT-DETAIL.                                       ST1114.2
039300 SORT-TEST-3.                                                     ST1114.2
039400     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1114.2
039500     PERFORM  READ-SORTIN.                                        ST1114.2
039600     IF       MEDIUM-RECORD EQUAL TO MEDIUM-WORK                  ST1114.2
039700              PERFORM PASS GO TO SORT-WRITE-3.                    ST1114.2
039800*        NOTE FOURTEENTH RECORD.                                  ST1114.2
039900 SORT-FAIL-3.                                                     ST1114.2
040000     MOVE     75 TO BREAKDOWN-LIMIT.                              ST1114.2
040100     MOVE     MEDIUM-RECORD TO COMPUTED-BREAKDOWN.                ST1114.2
040200     MOVE     MEDIUM-WORK TO CORRECT-BREAKDOWN.                   ST1114.2
040300     PERFORM  BREAKDOWN-PARA THRU BREAKDOWN-EXIT.                 ST1114.2
040400 SORT-WRITE-3.                                                    ST1114.2
040500     PERFORM  PRINT-DETAIL.                                       ST1114.2
040600 SORT-TEST-4.                                                     ST1114.2
040700     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1114.2
040800     PERFORM  READ-SORTIN 13 TIMES.                               ST1114.2
040900     IF       MEDIUM-RECORD EQUAL TO MEDIUM-WORK                  ST1114.2
041000              PERFORM PASS GO TO SORT-WRITE-4.                    ST1114.2
041100*        NOTE TWENTY-SEVENTH RECORD.                              ST1114.2
041200 SORT-FAIL-4.                                                     ST1114.2
041300     MOVE     75 TO BREAKDOWN-LIMIT.                              ST1114.2
041400     MOVE     MEDIUM-RECORD TO COMPUTED-BREAKDOWN.                ST1114.2
041500     MOVE     MEDIUM-WORK TO CORRECT-BREAKDOWN.                   ST1114.2
041600     PERFORM  BREAKDOWN-PARA THRU BREAKDOWN-EXIT.                 ST1114.2
041700 SORT-WRITE-4.                                                    ST1114.2
041800     PERFORM  PRINT-DETAIL.                                       ST1114.2
041900 SORT-TEST-5.                                                     ST1114.2
042000     MOVE     "SORT-TEST-5" TO PAR-NAME.                          ST1114.2
042100     PERFORM  READ-SORTIN.                                        ST1114.2
042200     IF       SHORT-RECORD EQUAL TO SHORT-WORK                    ST1114.2
042300              PERFORM PASS GO TO SORT-WRITE-5.                    ST1114.2
042400*        NOTE TWENTY-EIGHTH RECORD.                               ST1114.2
042500 SORT-FAIL-5.                                                     ST1114.2
042600     MOVE     50 TO BREAKDOWN-LIMIT.                              ST1114.2
042700     MOVE     SHORT-RECORD TO COMPUTED-BREAKDOWN.                 ST1114.2
042800     MOVE     SHORT-WORK TO CORRECT-BREAKDOWN.                    ST1114.2
042900     PERFORM  BREAKDOWN-PARA THRU BREAKDOWN-EXIT.                 ST1114.2
043000 SORT-WRITE-5.                                                    ST1114.2
043100     PERFORM  PRINT-DETAIL.                                       ST1114.2
043200 SORT-TEST-6.                                                     ST1114.2
043300     MOVE     "SORT-TEST-6" TO PAR-NAME.                          ST1114.2
043400     PERFORM  READ-SORTIN 12 TIMES.                               ST1114.2
043500     IF       SHORT-RECORD EQUAL TO SHORT-WORK                    ST1114.2
043600              PERFORM PASS GO TO SORT-WRITE-6.                    ST1114.2
043700*        NOTE FORTIETH RECORD.                                    ST1114.2
043800 SORT-FAIL-6.                                                     ST1114.2
043900     MOVE     50 TO BREAKDOWN-LIMIT.                              ST1114.2
044000     MOVE     SHORT-RECORD TO COMPUTED-BREAKDOWN.                 ST1114.2
044100     MOVE     SHORT-WORK TO CORRECT-BREAKDOWN.                    ST1114.2
044200     PERFORM  BREAKDOWN-PARA THRU BREAKDOWN-EXIT.                 ST1114.2
044300 SORT-WRITE-6.                                                    ST1114.2
044400     PERFORM  PRINT-DETAIL.                                       ST1114.2
044500 SORT-TEST-7.                                                     ST1114.2
044600     MOVE     "SORT-TEST-7" TO PAR-NAME.                          ST1114.2
044700     READ     SORTIN-1K AT END                                    ST1114.2
044800              PERFORM PASS GO TO SORT-WRITE-7.                    ST1114.2
044900 SORT-FAIL-7.                                                     ST1114.2
045000     MOVE     100 TO BREAKDOWN-LIMIT.                             ST1114.2
045100     MOVE     LONG-RECORD TO COMPUTED-BREAKDOWN.                  ST1114.2
045200     MOVE     SPACE TO CORRECT-BREAKDOWN.                         ST1114.2
045300     PERFORM  BREAKDOWN-PARA THRU BREAKDOWN-EXIT.                 ST1114.2
045400     PERFORM  PRINT-DETAIL.                                       ST1114.2
045500     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1114.2
045600 SORT-WRITE-7.                                                    ST1114.2
045700     PERFORM  PRINT-DETAIL.                                       ST1114.2
045800     CLOSE    SORTIN-1K.                                          ST1114.2
045900     GO TO    CCVS-EXIT.                                          ST1114.2
046000 BREAKDOWN-PARA.                                                  ST1114.2
046100     PERFORM  FAIL.                                               ST1114.2
046200     MOVE     FIRST-20-CM TO COMPUTED-A.                          ST1114.2
046300     MOVE     FIRST-20-CR TO CORRECT-A.                           ST1114.2
046400     MOVE     "FIRST TWENTY CHARACTERS" TO RE-MARK.               ST1114.2
046500     PERFORM  PRINT-DETAIL.                                       ST1114.2
046600     MOVE     SECOND-20-CM TO COMPUTED-A.                         ST1114.2
046700     MOVE     SECOND-20-CR TO CORRECT-A.                          ST1114.2
046800     MOVE     "SECOND TWENTY CHARACTERS" TO RE-MARK.              ST1114.2
046900     PERFORM  PRINT-DETAIL.                                       ST1114.2
047000     MOVE     THIRD-20-CM TO COMPUTED-A.                          ST1114.2
047100     MOVE     THIRD-20-CR TO CORRECT-A.                           ST1114.2
047200     MOVE     "THIRD TWENTY CHARACTERS" TO RE-MARK.               ST1114.2
047300     PERFORM  PRINT-DETAIL.                                       ST1114.2
047400     IF       BREAKDOWN-LIMIT LESS THAN 61 GO TO BREAKDOWN-EXIT.  ST1114.2
047500     MOVE     FOURTH-20-CM TO COMPUTED-A.                         ST1114.2
047600     MOVE     FOURTH-20-CR TO CORRECT-A.                          ST1114.2
047700     MOVE     "FOURTH TWENTY CHARACTERS" TO RE-MARK.              ST1114.2
047800     PERFORM  PRINT-DETAIL.                                       ST1114.2
047900     IF       BREAKDOWN-LIMIT LESS THAN 81 GO TO BREAKDOWN-EXIT.  ST1114.2
048000     MOVE     FIFTH-20-CM TO COMPUTED-A.                          ST1114.2
048100     MOVE     FIFTH-20-CR TO CORRECT-A.                           ST1114.2
048200     MOVE     "FIFTH TWENTY CHARACTERS" TO RE-MARK.               ST1114.2
048300 BREAKDOWN-EXIT.                                                  ST1114.2
048400     EXIT.                                                        ST1114.2
048500 READ-SORTIN.                                                     ST1114.2
048600     READ     SORTIN-1K AT END GO TO READ-ERROR.                  ST1114.2
048700     ADD      1 TO UTIL-CTR.                                      ST1114.2
048800 READ-ERROR.                                                      ST1114.2
048900     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1114.2
049000     MOVE     40 TO CORRECT-N.                                    ST1114.2
049100     MOVE     "TOO FEW INPUT RECORDS" TO RE-MARK.                 ST1114.2
049200     MOVE     "READ-SORTIN" TO PAR-NAME.                          ST1114.2
049300     PERFORM  FAIL.                                               ST1114.2
049400     PERFORM  PRINT-DETAIL.                                       ST1114.2
049500 CCVS-EXIT SECTION.                                               ST1114.2
049600 CCVS-999999.                                                     ST1114.2
049700     GO TO CLOSE-FILES.                                           ST1114.2
      *END-OF,ST111A                                                                  
