      *HEADER,COBOL,ST101A,SUBPRG,ST103A                                              
000100 IDENTIFICATION DIVISION.                                         ST1034.2
000200 PROGRAM-ID.                                                      ST1034.2
000300     ST103A.                                                      ST1034.2
000400****************************************************************  ST1034.2
000500*                                                              *  ST1034.2
000600*    VALIDATION FOR:-                                          *  ST1034.2
000700*                                                              *  ST1034.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1034.2
000900*                                                              *  ST1034.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1034.2
001100*                                                              *  ST1034.2
001200****************************************************************  ST1034.2
001300*                                                              *  ST1034.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1034.2
001500*                                                              *  ST1034.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1034.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1034.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1034.2
001900*                                                              *  ST1034.2
002000****************************************************************  ST1034.2
002100 ENVIRONMENT DIVISION.                                            ST1034.2
002200 CONFIGURATION SECTION.                                           ST1034.2
002300 SOURCE-COMPUTER.                                                 ST1034.2
002400     XXXXX082.                                                    ST1034.2
002500 OBJECT-COMPUTER.                                                 ST1034.2
002600     XXXXX083.                                                    ST1034.2
002700 INPUT-OUTPUT SECTION.                                            ST1034.2
002800 FILE-CONTROL.                                                    ST1034.2
002900     SELECT PRINT-FILE ASSIGN TO                                  ST1034.2
003000     XXXXX055.                                                    ST1034.2
003100     SELECT SORTIN-1C ASSIGN TO                                   ST1034.2
003200     XXXXD002.                                                    ST1034.2
003300 DATA DIVISION.                                                   ST1034.2
003400 FILE SECTION.                                                    ST1034.2
003500 FD  PRINT-FILE.                                                  ST1034.2
003600 01  PRINT-REC PICTURE X(120).                                    ST1034.2
003700 01  DUMMY-RECORD PICTURE X(120).                                 ST1034.2
003800 FD  SORTIN-1C                                                    ST1034.2
003900     BLOCK CONTAINS 10 RECORDS                                    ST1034.2
004000     LABEL RECORD STANDARD                                        ST1034.2
004100C    VALUE OF                                                     ST1034.2
004200C    XXXXX074                                                     ST1034.2
004300C    IS                                                           ST1034.2
004400C    XXXXX076                                                     ST1034.2
004500*    XXXXX069                                                     ST1034.2
004600     DATA  RECORD IS SORTIN-REC.                                  ST1034.2
004700 01  SORTIN-REC.                                                  ST1034.2
004800     02  KEYS-GROUP PICTURE 9(15).                                ST1034.2
004900     02  FILLER PICTURE X(105).                                   ST1034.2
005000 WORKING-STORAGE SECTION.                                         ST1034.2
005100 01  TEST-RESULTS.                                                ST1034.2
005200     02 FILLER                   PIC X      VALUE SPACE.          ST1034.2
005300     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1034.2
005400     02 FILLER                   PIC X      VALUE SPACE.          ST1034.2
005500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1034.2
005600     02 FILLER                   PIC X      VALUE SPACE.          ST1034.2
005700     02  PAR-NAME.                                                ST1034.2
005800       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1034.2
005900       03  PARDOT-X              PIC X      VALUE SPACE.          ST1034.2
006000       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1034.2
006100     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1034.2
006200     02 RE-MARK                  PIC X(61).                       ST1034.2
006300 01  TEST-COMPUTED.                                               ST1034.2
006400     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1034.2
006500     02 FILLER                   PIC X(17)  VALUE                 ST1034.2
006600            "       COMPUTED=".                                   ST1034.2
006700     02 COMPUTED-X.                                               ST1034.2
006800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1034.2
006900     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1034.2
007000                                 PIC -9(9).9(9).                  ST1034.2
007100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1034.2
007200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1034.2
007300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1034.2
007400     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1034.2
007500         04 COMPUTED-18V0                    PIC -9(18).          ST1034.2
007600         04 FILLER                           PIC X.               ST1034.2
007700     03 FILLER PIC X(50) VALUE SPACE.                             ST1034.2
007800 01  TEST-CORRECT.                                                ST1034.2
007900     02 FILLER PIC X(30) VALUE SPACE.                             ST1034.2
008000     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1034.2
008100     02 CORRECT-X.                                                ST1034.2
008200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1034.2
008300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1034.2
008400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1034.2
008500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1034.2
008600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1034.2
008700     03      CR-18V0 REDEFINES CORRECT-A.                         ST1034.2
008800         04 CORRECT-18V0                     PIC -9(18).          ST1034.2
008900         04 FILLER                           PIC X.               ST1034.2
009000     03 FILLER PIC X(2) VALUE SPACE.                              ST1034.2
009100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1034.2
009200 01  CCVS-C-1.                                                    ST1034.2
009300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1034.2
009400-    "SS  PARAGRAPH-NAME                                          ST1034.2
009500-    "       REMARKS".                                            ST1034.2
009600     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1034.2
009700 01  CCVS-C-2.                                                    ST1034.2
009800     02 FILLER                     PIC X        VALUE SPACE.      ST1034.2
009900     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1034.2
010000     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1034.2
010100     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1034.2
010200     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1034.2
010300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1034.2
010400 01  REC-CT                        PIC 99       VALUE ZERO.       ST1034.2
010500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1034.2
010600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1034.2
010700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1034.2
010800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1034.2
010900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1034.2
011000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1034.2
011100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1034.2
011200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1034.2
011300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1034.2
011400 01  CCVS-H-1.                                                    ST1034.2
011500     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1034.2
011600     02  FILLER                    PIC X(42)    VALUE             ST1034.2
011700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1034.2
011800     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1034.2
011900 01  CCVS-H-2A.                                                   ST1034.2
012000   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1034.2
012100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1034.2
012200   02  FILLER                        PIC XXXX   VALUE             ST1034.2
012300     "4.2 ".                                                      ST1034.2
012400   02  FILLER                        PIC X(28)  VALUE             ST1034.2
012500            " COPY - NOT FOR DISTRIBUTION".                       ST1034.2
012600   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1034.2
012700                                                                  ST1034.2
012800 01  CCVS-H-2B.                                                   ST1034.2
012900   02  FILLER                        PIC X(15)  VALUE             ST1034.2
013000            "TEST RESULT OF ".                                    ST1034.2
013100   02  TEST-ID                       PIC X(9).                    ST1034.2
013200   02  FILLER                        PIC X(4)   VALUE             ST1034.2
013300            " IN ".                                               ST1034.2
013400   02  FILLER                        PIC X(12)  VALUE             ST1034.2
013500     " HIGH       ".                                              ST1034.2
013600   02  FILLER                        PIC X(22)  VALUE             ST1034.2
013700            " LEVEL VALIDATION FOR ".                             ST1034.2
013800   02  FILLER                        PIC X(58)  VALUE             ST1034.2
013900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1034.2
014000 01  CCVS-H-3.                                                    ST1034.2
014100     02  FILLER                      PIC X(34)  VALUE             ST1034.2
014200            " FOR OFFICIAL USE ONLY    ".                         ST1034.2
014300     02  FILLER                      PIC X(58)  VALUE             ST1034.2
014400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1034.2
014500     02  FILLER                      PIC X(28)  VALUE             ST1034.2
014600            "  COPYRIGHT   1985 ".                                ST1034.2
014700 01  CCVS-E-1.                                                    ST1034.2
014800     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1034.2
014900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1034.2
015000     02 ID-AGAIN                     PIC X(9).                    ST1034.2
015100     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1034.2
015200 01  CCVS-E-2.                                                    ST1034.2
015300     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1034.2
015400     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1034.2
015500     02 CCVS-E-2-2.                                               ST1034.2
015600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1034.2
015700         03 FILLER                   PIC X      VALUE SPACE.      ST1034.2
015800         03 ENDER-DESC               PIC X(44)  VALUE             ST1034.2
015900            "ERRORS ENCOUNTERED".                                 ST1034.2
016000 01  CCVS-E-3.                                                    ST1034.2
016100     02  FILLER                      PIC X(22)  VALUE             ST1034.2
016200            " FOR OFFICIAL USE ONLY".                             ST1034.2
016300     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1034.2
016400     02  FILLER                      PIC X(58)  VALUE             ST1034.2
016500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1034.2
016600     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1034.2
016700     02 FILLER                       PIC X(15)  VALUE             ST1034.2
016800             " COPYRIGHT 1985".                                   ST1034.2
016900 01  CCVS-E-4.                                                    ST1034.2
017000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1034.2
017100     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1034.2
017200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1034.2
017300     02 FILLER                       PIC X(40)  VALUE             ST1034.2
017400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1034.2
017500 01  XXINFO.                                                      ST1034.2
017600     02 FILLER                       PIC X(19)  VALUE             ST1034.2
017700            "*** INFORMATION ***".                                ST1034.2
017800     02 INFO-TEXT.                                                ST1034.2
017900       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1034.2
018000       04 XXCOMPUTED                 PIC X(20).                   ST1034.2
018100       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1034.2
018200       04 XXCORRECT                  PIC X(20).                   ST1034.2
018300     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1034.2
018400 01  HYPHEN-LINE.                                                 ST1034.2
018500     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1034.2
018600     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1034.2
018700-    "*****************************************".                 ST1034.2
018800     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1034.2
018900-    "******************************".                            ST1034.2
019000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1034.2
019100     "ST103A".                                                    ST1034.2
019200 PROCEDURE DIVISION.                                              ST1034.2
019300 CCVS1 SECTION.                                                   ST1034.2
019400 OPEN-FILES.                                                      ST1034.2
019500     OPEN     OUTPUT PRINT-FILE.                                  ST1034.2
019600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1034.2
019700     MOVE    SPACE TO TEST-RESULTS.                               ST1034.2
019800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1034.2
019900     GO TO CCVS1-EXIT.                                            ST1034.2
020000 CLOSE-FILES.                                                     ST1034.2
020100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1034.2
020200 TERMINATE-CCVS.                                                  ST1034.2
020300*    EXIT PROGRAM.                                                ST1034.2
020400*TERMINATE-CALL.                                                  ST1034.2
020500     STOP     RUN.                                                ST1034.2
020600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1034.2
020700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1034.2
020800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1034.2
020900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1034.2
021000     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1034.2
021100 PRINT-DETAIL.                                                    ST1034.2
021200     IF REC-CT NOT EQUAL TO ZERO                                  ST1034.2
021300             MOVE "." TO PARDOT-X                                 ST1034.2
021400             MOVE REC-CT TO DOTVALUE.                             ST1034.2
021500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1034.2
021600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1034.2
021700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1034.2
021800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1034.2
021900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1034.2
022000     MOVE SPACE TO CORRECT-X.                                     ST1034.2
022100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1034.2
022200     MOVE     SPACE TO RE-MARK.                                   ST1034.2
022300 HEAD-ROUTINE.                                                    ST1034.2
022400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1034.2
022500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1034.2
022600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1034.2
022700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1034.2
022800 COLUMN-NAMES-ROUTINE.                                            ST1034.2
022900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1034.2
023000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1034.2
023100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1034.2
023200 END-ROUTINE.                                                     ST1034.2
023300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1034.2
023400 END-RTN-EXIT.                                                    ST1034.2
023500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1034.2
023600 END-ROUTINE-1.                                                   ST1034.2
023700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1034.2
023800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1034.2
023900      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1034.2
024000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1034.2
024100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1034.2
024200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1034.2
024300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1034.2
024400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1034.2
024500  END-ROUTINE-12.                                                 ST1034.2
024600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1034.2
024700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1034.2
024800         MOVE "NO " TO ERROR-TOTAL                                ST1034.2
024900         ELSE                                                     ST1034.2
025000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1034.2
025100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1034.2
025200     PERFORM WRITE-LINE.                                          ST1034.2
025300 END-ROUTINE-13.                                                  ST1034.2
025400     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1034.2
025500         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1034.2
025600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1034.2
025700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1034.2
025800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1034.2
025900      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1034.2
026000          MOVE "NO " TO ERROR-TOTAL                               ST1034.2
026100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1034.2
026200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1034.2
026300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1034.2
026400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1034.2
026500 WRITE-LINE.                                                      ST1034.2
026600     ADD 1 TO RECORD-COUNT.                                       ST1034.2
026700*    IF RECORD-COUNT GREATER 42                                   ST1034.2
026800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1034.2
026900*        MOVE SPACE TO DUMMY-RECORD                               ST1034.2
027000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1034.2
027100*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1034.2
027200*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1034.2
027300*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1034.2
027400*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1034.2
027500*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1034.2
027600*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1034.2
027700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1034.2
027800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1034.2
027900*        MOVE ZERO TO RECORD-COUNT.                               ST1034.2
028000     PERFORM WRT-LN.                                              ST1034.2
028100 WRT-LN.                                                          ST1034.2
028200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1034.2
028300     MOVE SPACE TO DUMMY-RECORD.                                  ST1034.2
028400 BLANK-LINE-PRINT.                                                ST1034.2
028500     PERFORM WRT-LN.                                              ST1034.2
028600 FAIL-ROUTINE.                                                    ST1034.2
028700     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1034.2
028800            GO TO   FAIL-ROUTINE-WRITE.                           ST1034.2
028900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1034.2
029000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1034.2
029100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1034.2
029200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1034.2
029300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1034.2
029400     GO TO  FAIL-ROUTINE-EX.                                      ST1034.2
029500 FAIL-ROUTINE-WRITE.                                              ST1034.2
029600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1034.2
029700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1034.2
029800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1034.2
029900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1034.2
030000 FAIL-ROUTINE-EX. EXIT.                                           ST1034.2
030100 BAIL-OUT.                                                        ST1034.2
030200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1034.2
030300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1034.2
030400 BAIL-OUT-WRITE.                                                  ST1034.2
030500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1034.2
030600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1034.2
030700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1034.2
030800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1034.2
030900 BAIL-OUT-EX. EXIT.                                               ST1034.2
031000 CCVS1-EXIT.                                                      ST1034.2
031100     EXIT.                                                        ST1034.2
031200 SECT-ST103-0001 SECTION.                                         ST1034.2
031300 ST103-0001-01.                                                   ST1034.2
031400     OPEN    INPUT  SORTIN-1C.                                    ST1034.2
031500     MOVE     "THIS PROGRAM TESTS THE" TO RE-MARK.                ST1034.2
031600     PERFORM  PRINT-DETAIL.                                       ST1034.2
031700     MOVE     "OUTPUT GENERATED BY ST102,"  TO RE-MARK.           ST1034.2
031800     PERFORM  PRINT-DETAIL.                                       ST1034.2
031900     MOVE     "WHICH WAS IN TURN GENERATED" TO RE-MARK.           ST1034.2
032000     PERFORM  PRINT-DETAIL.                                       ST1034.2
032100     MOVE     "IN ST101."                   TO RE-MARK.           ST1034.2
032200     PERFORM  PRINT-DETAIL.                                       ST1034.2
032300     MOVE     "SORT - USING, GIVING" TO FEATURE.                  ST1034.2
032400 SORT-TEST-1.                                                     ST1034.2
032500     PERFORM  READ-SORTED-FILE.                                   ST1034.2
032600     IF       KEYS-GROUP EQUAL TO 900009000000000                 ST1034.2
032700              PERFORM PASS GO TO SORT-WRITE-1.                    ST1034.2
032800     GO       TO SORT-FAIL-1.                                     ST1034.2
032900 SORT-DELETE-1.                                                   ST1034.2
033000     PERFORM  DE-LETE.                                            ST1034.2
033100     GO       TO SORT-WRITE-1.                                    ST1034.2
033200 SORT-FAIL-1.                                                     ST1034.2
033300     PERFORM  FAIL.                                               ST1034.2
033400     MOVE     KEYS-GROUP TO COMPUTED-18V0.                        ST1034.2
033500     MOVE     900009000000000 TO CORRECT-18V0.                    ST1034.2
033600 SORT-WRITE-1.                                                    ST1034.2
033700     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1034.2
033800     PERFORM  PRINT-DETAIL.                                       ST1034.2
033900 SORT-TEST-2.                                                     ST1034.2
034000     PERFORM  READ-SORTED-FILE.                                   ST1034.2
034100     IF       KEYS-GROUP EQUAL TO 900008000000000                 ST1034.2
034200              PERFORM PASS GO TO SORT-WRITE-2.                    ST1034.2
034300     GO       TO SORT-FAIL-2.                                     ST1034.2
034400 SORT-DELETE-2.                                                   ST1034.2
034500     PERFORM  DE-LETE.                                            ST1034.2
034600     GO       TO SORT-WRITE-2.                                    ST1034.2
034700 SORT-FAIL-2.                                                     ST1034.2
034800     PERFORM  FAIL.                                               ST1034.2
034900     MOVE     KEYS-GROUP TO COMPUTED-18V0.                        ST1034.2
035000     MOVE     900008000000000 TO CORRECT-18V0.                    ST1034.2
035100 SORT-WRITE-2.                                                    ST1034.2
035200     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1034.2
035300     PERFORM  PRINT-DETAIL.                                       ST1034.2
035400 SORT-TEST-3.                                                     ST1034.2
035500     PERFORM  READ-SORTED-FILE.                                   ST1034.2
035600     IF       KEYS-GROUP EQUAL TO 201002000100001                 ST1034.2
035700              PERFORM PASS GO TO SORT-WRITE-3.                    ST1034.2
035800     GO       TO SORT-FAIL-3.                                     ST1034.2
035900 SORT-DELETE-3.                                                   ST1034.2
036000     PERFORM  DE-LETE.                                            ST1034.2
036100     GO       TO SORT-WRITE-3.                                    ST1034.2
036200 SORT-FAIL-3.                                                     ST1034.2
036300     PERFORM  FAIL.                                               ST1034.2
036400     MOVE     KEYS-GROUP TO COMPUTED-18V0.                        ST1034.2
036500     MOVE     201002000100001 TO CORRECT-18V0.                    ST1034.2
036600 SORT-WRITE-3.                                                    ST1034.2
036700     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1034.2
036800     PERFORM  PRINT-DETAIL.                                       ST1034.2
036900 SORT-TEST-4.                                                     ST1034.2
037000     PERFORM  READ-SORTED-FILE 48 TIMES.                          ST1034.2
037100     IF       KEYS-GROUP EQUAL TO 101002000100001                 ST1034.2
037200              PERFORM PASS GO TO SORT-WRITE-4.                    ST1034.2
037300     GO       TO SORT-FAIL-4.                                     ST1034.2
037400 SORT-DELETE-4.                                                   ST1034.2
037500     PERFORM  DE-LETE.                                            ST1034.2
037600     GO       TO SORT-WRITE-4.                                    ST1034.2
037700 SORT-FAIL-4.                                                     ST1034.2
037800     PERFORM  FAIL.                                               ST1034.2
037900     MOVE     KEYS-GROUP TO COMPUTED-18V0.                        ST1034.2
038000     MOVE     101002000100001 TO CORRECT-18V0.                    ST1034.2
038100 SORT-WRITE-4.                                                    ST1034.2
038200     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1034.2
038300     PERFORM  PRINT-DETAIL.                                       ST1034.2
038400 SORT-TEST-5.                                                     ST1034.2
038500     PERFORM  READ-SORTED-FILE 40 TIMES.                          ST1034.2
038600     IF       KEYS-GROUP EQUAL TO 106002000100001                 ST1034.2
038700              PERFORM PASS GO TO SORT-WRITE-5.                    ST1034.2
038800     GO       TO SORT-FAIL-5.                                     ST1034.2
038900 SORT-DELETE-5.                                                   ST1034.2
039000     PERFORM  DE-LETE.                                            ST1034.2
039100     GO       TO SORT-WRITE-5.                                    ST1034.2
039200 SORT-FAIL-5.                                                     ST1034.2
039300     PERFORM  FAIL.                                               ST1034.2
039400     MOVE     KEYS-GROUP TO COMPUTED-18V0.                        ST1034.2
039500     MOVE     106002000100001 TO CORRECT-18V0.                    ST1034.2
039600 SORT-WRITE-5.                                                    ST1034.2
039700     MOVE     "SORT-TEST-5" TO PAR-NAME.                          ST1034.2
039800     PERFORM  PRINT-DETAIL.                                       ST1034.2
039900 SORT-TEST-6.                                                     ST1034.2
040000     PERFORM  READ-SORTED-FILE  7 TIMES.                          ST1034.2
040100     IF       KEYS-GROUP EQUAL TO 106001000200002                 ST1034.2
040200              PERFORM PASS GO TO SORT-WRITE-6.                    ST1034.2
040300     GO       TO SORT-FAIL-6.                                     ST1034.2
040400 SORT-DELETE-6.                                                   ST1034.2
040500     PERFORM  DE-LETE.                                            ST1034.2
040600     GO       TO SORT-WRITE-6.                                    ST1034.2
040700 SORT-FAIL-6.                                                     ST1034.2
040800     PERFORM  FAIL.                                               ST1034.2
040900     MOVE     KEYS-GROUP TO COMPUTED-18V0.                        ST1034.2
041000     MOVE     106001000200002 TO CORRECT-18V0.                    ST1034.2
041100 SORT-WRITE-6.                                                    ST1034.2
041200     MOVE     "SORT-TEST-6" TO PAR-NAME.                          ST1034.2
041300     PERFORM  PRINT-DETAIL.                                       ST1034.2
041400 SORT-TEST-7.                                                     ST1034.2
041500     PERFORM  READ-SORTED-FILE.                                   ST1034.2
041600     IF       KEYS-GROUP EQUAL TO 009000000900008                 ST1034.2
041700              PERFORM PASS GO TO SORT-WRITE-7.                    ST1034.2
041800     GO       TO SORT-FAIL-7.                                     ST1034.2
041900 SORT-DELETE-7.                                                   ST1034.2
042000     PERFORM  DE-LETE.                                            ST1034.2
042100     GO       TO SORT-WRITE-7.                                    ST1034.2
042200 SORT-FAIL-7.                                                     ST1034.2
042300     PERFORM  FAIL.                                               ST1034.2
042400     MOVE     KEYS-GROUP TO COMPUTED-18V0.                        ST1034.2
042500     MOVE     009000000900008 TO CORRECT-18V0.                    ST1034.2
042600 SORT-WRITE-7.                                                    ST1034.2
042700     MOVE     "SORT-TEST-7" TO PAR-NAME.                          ST1034.2
042800     PERFORM  PRINT-DETAIL.                                       ST1034.2
042900 SORT-TEST-8.                                                     ST1034.2
043000     PERFORM  READ-SORTED-FILE.                                   ST1034.2
043100     IF       KEYS-GROUP EQUAL TO 009000000900009                 ST1034.2
043200              PERFORM PASS GO TO SORT-WRITE-8.                    ST1034.2
043300     GO       TO SORT-FAIL-8.                                     ST1034.2
043400 SORT-DELETE-8.                                                   ST1034.2
043500     PERFORM  DE-LETE.                                            ST1034.2
043600     GO       TO SORT-WRITE-8.                                    ST1034.2
043700 SORT-FAIL-8.                                                     ST1034.2
043800     PERFORM  FAIL.                                               ST1034.2
043900     MOVE     KEYS-GROUP TO COMPUTED-18V0.                        ST1034.2
044000     MOVE     009000000900009 TO CORRECT-18V0.                    ST1034.2
044100 SORT-WRITE-8.                                                    ST1034.2
044200     MOVE     "SORT-TEST-8" TO PAR-NAME.                          ST1034.2
044300     PERFORM  PRINT-DETAIL.                                       ST1034.2
044400 SORT-TEST-9.                                                     ST1034.2
044500     READ     SORTIN-1C AT END                                    ST1034.2
044600              PERFORM PASS GO TO SORT-WRITE-9.                    ST1034.2
044700*    NOTE     THE FOLLOWING STATEMENTS SHOULD NOT BE EXECUTED.    ST1034.2
044800     PERFORM  FAIL.                                               ST1034.2
044900     MOVE     KEYS-GROUP TO COMPUTED-18V0.                        ST1034.2
045000     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1034.2
045100     GO       TO SORT-WRITE-9.                                    ST1034.2
045200 SORT-DELETE-9.                                                   ST1034.2
045300     PERFORM  DE-LETE.                                            ST1034.2
045400 SORT-WRITE-9.                                                    ST1034.2
045500     MOVE     "SORT-TEST-9" TO PAR-NAME.                          ST1034.2
045600     PERFORM  PRINT-DETAIL.                                       ST1034.2
045700     CLOSE    SORTIN-1C.                                          ST1034.2
045800     GO TO    CCVS-EXIT.                                          ST1034.2
045900 READ-SORTED-FILE.                                                ST1034.2
046000     READ     SORTIN-1C          AT END GO TO BAD-FILE.           ST1034.2
046100 BAD-FILE.                                                        ST1034.2
046200     PERFORM FAIL.                                                ST1034.2
046300     MOVE     "BAD-FILE" TO PAR-NAME.                             ST1034.2
046400     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               ST1034.2
046500     PERFORM  PRINT-DETAIL.                                       ST1034.2
046600     MOVE     SPACE TO FEATURE.                                   ST1034.2
046700     MOVE     "FOUND, PREVIOUS TEST WAS" TO RE-MARK.              ST1034.2
046800     PERFORM  PRINT-DETAIL.                                       ST1034.2
046900     MOVE     "LAST SUCCESSFUL TEST." TO RE-MARK.                 ST1034.2
047000     PERFORM  PRINT-DETAIL.                                       ST1034.2
047100 CCVS-EXIT SECTION.                                               ST1034.2
047200 CCVS-999999.                                                     ST1034.2
047300     GO TO CLOSE-FILES.                                           ST1034.2
      *END-OF,ST103A                                                                  
