      *HEADER,COBOL,ST112M,SUBPRG,ST114M                                              
000100 IDENTIFICATION DIVISION.                                         ST1144.2
000200 PROGRAM-ID.                                                      ST1144.2
000300     ST114M.                                                      ST1144.2
000400****************************************************************  ST1144.2
000500*                                                              *  ST1144.2
000600*    VALIDATION FOR:-                                          *  ST1144.2
000700*                                                              *  ST1144.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1144.2
000900*                                                              *  ST1144.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1144.2
001100*                                                              *  ST1144.2
001200****************************************************************  ST1144.2
001300*                                                              *  ST1144.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1144.2
001500*                                                              *  ST1144.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1144.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1144.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1144.2
001900*                                                              *  ST1144.2
002000****************************************************************  ST1144.2
002100 ENVIRONMENT DIVISION.                                            ST1144.2
002200 CONFIGURATION SECTION.                                           ST1144.2
002300 SOURCE-COMPUTER.                                                 ST1144.2
002400     XXXXX082.                                                    ST1144.2
002500 OBJECT-COMPUTER.                                                 ST1144.2
002600     XXXXX083.                                                    ST1144.2
002700 INPUT-OUTPUT SECTION.                                            ST1144.2
002800 FILE-CONTROL.                                                    ST1144.2
002900     SELECT PRINT-FILE ASSIGN TO                                  ST1144.2
003000     XXXXX055.                                                    ST1144.2
003100     SELECT   SORTIN-1N ASSIGN TO                                 ST1144.2
003200     XXXXD001.                                                    ST1144.2
003300 DATA DIVISION.                                                   ST1144.2
003400 FILE SECTION.                                                    ST1144.2
003500 FD  PRINT-FILE.                                                  ST1144.2
003600 01  PRINT-REC PICTURE X(120).                                    ST1144.2
003700 01  DUMMY-RECORD PICTURE X(120).                                 ST1144.2
003800 FD  SORTIN-1N                                                    ST1144.2
003900     LABEL RECORDS STANDARD                                       ST1144.2
004000C    VALUE OF                                                     ST1144.2
004100C    XXXXX074                                                     ST1144.2
004200C    IS                                                           ST1144.2
004300C    XXXXX075                                                     ST1144.2
004400*    XXXXX069                                                     ST1144.2
004500              DATA RECORDS ARE SORT-KEY.                          ST1144.2
004600 01  SORT-KEY           PICTURE X(33).                            ST1144.2
004700 WORKING-STORAGE SECTION.                                         ST1144.2
004800 77  ALL-A              PICTURE X(33) VALUE                       ST1144.2
004900     ALL "A".                                                     ST1144.2
005000 77  ALL-N              PICTURE X(33) VALUE                       ST1144.2
005100     ALL "N".                                                     ST1144.2
005200 77  ALL-Z              PICTURE X(33) VALUE                       ST1144.2
005300     ALL "Z".                                                     ST1144.2
005400 77  UTIL-CTR           PICTURE S99999 VALUE ZERO.                ST1144.2
005500 01  COMPUTED-BREAKDOWN.                                          ST1144.2
005600     02 FIRST-20-CM     PICTURE X(20).                            ST1144.2
005700     02 SECOND-20-CM    PICTURE X(20).                            ST1144.2
005800 01  CORRECT-BREAKDOWN.                                           ST1144.2
005900     02 FIRST-20-CR     PICTURE X(20).                            ST1144.2
006000     02 SECOND-20-CR    PICTURE X(20).                            ST1144.2
006100 01  TEST-RESULTS.                                                ST1144.2
006200     02 FILLER                   PIC X      VALUE SPACE.          ST1144.2
006300     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1144.2
006400     02 FILLER                   PIC X      VALUE SPACE.          ST1144.2
006500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1144.2
006600     02 FILLER                   PIC X      VALUE SPACE.          ST1144.2
006700     02  PAR-NAME.                                                ST1144.2
006800       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1144.2
006900       03  PARDOT-X              PIC X      VALUE SPACE.          ST1144.2
007000       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1144.2
007100     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1144.2
007200     02 RE-MARK                  PIC X(61).                       ST1144.2
007300 01  TEST-COMPUTED.                                               ST1144.2
007400     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1144.2
007500     02 FILLER                   PIC X(17)  VALUE                 ST1144.2
007600            "       COMPUTED=".                                   ST1144.2
007700     02 COMPUTED-X.                                               ST1144.2
007800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1144.2
007900     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1144.2
008000                                 PIC -9(9).9(9).                  ST1144.2
008100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1144.2
008200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1144.2
008300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1144.2
008400     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1144.2
008500         04 COMPUTED-18V0                    PIC -9(18).          ST1144.2
008600         04 FILLER                           PIC X.               ST1144.2
008700     03 FILLER PIC X(50) VALUE SPACE.                             ST1144.2
008800 01  TEST-CORRECT.                                                ST1144.2
008900     02 FILLER PIC X(30) VALUE SPACE.                             ST1144.2
009000     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1144.2
009100     02 CORRECT-X.                                                ST1144.2
009200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1144.2
009300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1144.2
009400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1144.2
009500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1144.2
009600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1144.2
009700     03      CR-18V0 REDEFINES CORRECT-A.                         ST1144.2
009800         04 CORRECT-18V0                     PIC -9(18).          ST1144.2
009900         04 FILLER                           PIC X.               ST1144.2
010000     03 FILLER PIC X(2) VALUE SPACE.                              ST1144.2
010100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1144.2
010200 01  CCVS-C-1.                                                    ST1144.2
010300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1144.2
010400-    "SS  PARAGRAPH-NAME                                          ST1144.2
010500-    "       REMARKS".                                            ST1144.2
010600     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1144.2
010700 01  CCVS-C-2.                                                    ST1144.2
010800     02 FILLER                     PIC X        VALUE SPACE.      ST1144.2
010900     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1144.2
011000     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1144.2
011100     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1144.2
011200     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1144.2
011300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1144.2
011400 01  REC-CT                        PIC 99       VALUE ZERO.       ST1144.2
011500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1144.2
011600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1144.2
011700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1144.2
011800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1144.2
011900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1144.2
012000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1144.2
012100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1144.2
012200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1144.2
012300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1144.2
012400 01  CCVS-H-1.                                                    ST1144.2
012500     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1144.2
012600     02  FILLER                    PIC X(42)    VALUE             ST1144.2
012700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1144.2
012800     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1144.2
012900 01  CCVS-H-2A.                                                   ST1144.2
013000   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1144.2
013100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1144.2
013200   02  FILLER                        PIC XXXX   VALUE             ST1144.2
013300     "4.2 ".                                                      ST1144.2
013400   02  FILLER                        PIC X(28)  VALUE             ST1144.2
013500            " COPY - NOT FOR DISTRIBUTION".                       ST1144.2
013600   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1144.2
013700                                                                  ST1144.2
013800 01  CCVS-H-2B.                                                   ST1144.2
013900   02  FILLER                        PIC X(15)  VALUE             ST1144.2
014000            "TEST RESULT OF ".                                    ST1144.2
014100   02  TEST-ID                       PIC X(9).                    ST1144.2
014200   02  FILLER                        PIC X(4)   VALUE             ST1144.2
014300            " IN ".                                               ST1144.2
014400   02  FILLER                        PIC X(12)  VALUE             ST1144.2
014500     " HIGH       ".                                              ST1144.2
014600   02  FILLER                        PIC X(22)  VALUE             ST1144.2
014700            " LEVEL VALIDATION FOR ".                             ST1144.2
014800   02  FILLER                        PIC X(58)  VALUE             ST1144.2
014900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1144.2
015000 01  CCVS-H-3.                                                    ST1144.2
015100     02  FILLER                      PIC X(34)  VALUE             ST1144.2
015200            " FOR OFFICIAL USE ONLY    ".                         ST1144.2
015300     02  FILLER                      PIC X(58)  VALUE             ST1144.2
015400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1144.2
015500     02  FILLER                      PIC X(28)  VALUE             ST1144.2
015600            "  COPYRIGHT   1985 ".                                ST1144.2
015700 01  CCVS-E-1.                                                    ST1144.2
015800     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1144.2
015900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1144.2
016000     02 ID-AGAIN                     PIC X(9).                    ST1144.2
016100     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1144.2
016200 01  CCVS-E-2.                                                    ST1144.2
016300     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1144.2
016400     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1144.2
016500     02 CCVS-E-2-2.                                               ST1144.2
016600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1144.2
016700         03 FILLER                   PIC X      VALUE SPACE.      ST1144.2
016800         03 ENDER-DESC               PIC X(44)  VALUE             ST1144.2
016900            "ERRORS ENCOUNTERED".                                 ST1144.2
017000 01  CCVS-E-3.                                                    ST1144.2
017100     02  FILLER                      PIC X(22)  VALUE             ST1144.2
017200            " FOR OFFICIAL USE ONLY".                             ST1144.2
017300     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1144.2
017400     02  FILLER                      PIC X(58)  VALUE             ST1144.2
017500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1144.2
017600     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1144.2
017700     02 FILLER                       PIC X(15)  VALUE             ST1144.2
017800             " COPYRIGHT 1985".                                   ST1144.2
017900 01  CCVS-E-4.                                                    ST1144.2
018000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1144.2
018100     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1144.2
018200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1144.2
018300     02 FILLER                       PIC X(40)  VALUE             ST1144.2
018400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1144.2
018500 01  XXINFO.                                                      ST1144.2
018600     02 FILLER                       PIC X(19)  VALUE             ST1144.2
018700            "*** INFORMATION ***".                                ST1144.2
018800     02 INFO-TEXT.                                                ST1144.2
018900       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1144.2
019000       04 XXCOMPUTED                 PIC X(20).                   ST1144.2
019100       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1144.2
019200       04 XXCORRECT                  PIC X(20).                   ST1144.2
019300     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1144.2
019400 01  HYPHEN-LINE.                                                 ST1144.2
019500     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1144.2
019600     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1144.2
019700-    "*****************************************".                 ST1144.2
019800     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1144.2
019900-    "******************************".                            ST1144.2
020000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1144.2
020100     "ST114M".                                                    ST1144.2
020200 PROCEDURE DIVISION.                                              ST1144.2
020300 CCVS1 SECTION.                                                   ST1144.2
020400 OPEN-FILES.                                                      ST1144.2
020500     OPEN     OUTPUT PRINT-FILE.                                  ST1144.2
020600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1144.2
020700     MOVE    SPACE TO TEST-RESULTS.                               ST1144.2
020800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1144.2
020900     GO TO CCVS1-EXIT.                                            ST1144.2
021000 CLOSE-FILES.                                                     ST1144.2
021100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1144.2
021200 TERMINATE-CCVS.                                                  ST1144.2
021300*    EXIT PROGRAM.                                                ST1144.2
021400*TERMINATE-CALL.                                                  ST1144.2
021500     STOP     RUN.                                                ST1144.2
021600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1144.2
021700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1144.2
021800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1144.2
021900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1144.2
022000     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1144.2
022100 PRINT-DETAIL.                                                    ST1144.2
022200     IF REC-CT NOT EQUAL TO ZERO                                  ST1144.2
022300             MOVE "." TO PARDOT-X                                 ST1144.2
022400             MOVE REC-CT TO DOTVALUE.                             ST1144.2
022500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1144.2
022600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1144.2
022700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1144.2
022800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1144.2
022900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1144.2
023000     MOVE SPACE TO CORRECT-X.                                     ST1144.2
023100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1144.2
023200     MOVE     SPACE TO RE-MARK.                                   ST1144.2
023300 HEAD-ROUTINE.                                                    ST1144.2
023400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1144.2
023500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1144.2
023600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1144.2
023700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1144.2
023800 COLUMN-NAMES-ROUTINE.                                            ST1144.2
023900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1144.2
024000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1144.2
024100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1144.2
024200 END-ROUTINE.                                                     ST1144.2
024300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1144.2
024400 END-RTN-EXIT.                                                    ST1144.2
024500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1144.2
024600 END-ROUTINE-1.                                                   ST1144.2
024700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1144.2
024800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1144.2
024900      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1144.2
025000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1144.2
025100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1144.2
025200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1144.2
025300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1144.2
025400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1144.2
025500  END-ROUTINE-12.                                                 ST1144.2
025600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1144.2
025700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1144.2
025800         MOVE "NO " TO ERROR-TOTAL                                ST1144.2
025900         ELSE                                                     ST1144.2
026000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1144.2
026100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1144.2
026200     PERFORM WRITE-LINE.                                          ST1144.2
026300 END-ROUTINE-13.                                                  ST1144.2
026400     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1144.2
026500         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1144.2
026600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1144.2
026700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1144.2
026800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1144.2
026900      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1144.2
027000          MOVE "NO " TO ERROR-TOTAL                               ST1144.2
027100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1144.2
027200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1144.2
027300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1144.2
027400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1144.2
027500 WRITE-LINE.                                                      ST1144.2
027600     ADD 1 TO RECORD-COUNT.                                       ST1144.2
027700*    IF RECORD-COUNT GREATER 42                                   ST1144.2
027800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1144.2
027900*        MOVE SPACE TO DUMMY-RECORD                               ST1144.2
028000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1144.2
028100*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1144.2
028200*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1144.2
028300*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1144.2
028400*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1144.2
028500*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1144.2
028600*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1144.2
028700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1144.2
028800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1144.2
028900*        MOVE ZERO TO RECORD-COUNT.                               ST1144.2
029000     PERFORM WRT-LN.                                              ST1144.2
029100 WRT-LN.                                                          ST1144.2
029200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1144.2
029300     MOVE SPACE TO DUMMY-RECORD.                                  ST1144.2
029400 BLANK-LINE-PRINT.                                                ST1144.2
029500     PERFORM WRT-LN.                                              ST1144.2
029600 FAIL-ROUTINE.                                                    ST1144.2
029700     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1144.2
029800            GO TO   FAIL-ROUTINE-WRITE.                           ST1144.2
029900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1144.2
030000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1144.2
030100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1144.2
030200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1144.2
030300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1144.2
030400     GO TO  FAIL-ROUTINE-EX.                                      ST1144.2
030500 FAIL-ROUTINE-WRITE.                                              ST1144.2
030600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1144.2
030700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1144.2
030800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1144.2
030900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1144.2
031000 FAIL-ROUTINE-EX. EXIT.                                           ST1144.2
031100 BAIL-OUT.                                                        ST1144.2
031200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1144.2
031300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1144.2
031400 BAIL-OUT-WRITE.                                                  ST1144.2
031500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1144.2
031600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1144.2
031700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1144.2
031800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1144.2
031900 BAIL-OUT-EX. EXIT.                                               ST1144.2
032000 CCVS1-EXIT.                                                      ST1144.2
032100     EXIT.                                                        ST1144.2
032200 SECT-ST114-0001 SECTION.                                         ST1144.2
032300 ST114-0001-01.                                                   ST1144.2
032400     OPEN     INPUT   SORTIN-1N.                                  ST1144.2
032500     MOVE    "        ************   ST113 WILL NOT PRODUCE ANY PRST1144.2
032600-    "INTED REPORT   ************" TO TEST-RESULTS.               ST1144.2
032700     PERFORM PRINT-DETAIL.                                        ST1144.2
032800     MOVE    SPACE TO TEST-RESULTS.                               ST1144.2
032900     PERFORM END-ROUTINE.                                         ST1144.2
033000     PERFORM BLANK-LINE-PRINT.                                    ST1144.2
033100 SORT-INIT-A.                                                     ST1144.2
033200     MOVE     "SORT, MULTIPLE REEL" TO FEATURE.                   ST1144.2
033300 SORT-TEST-1.                                                     ST1144.2
033400     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1144.2
033500     PERFORM  READ-SORTIN.                                        ST1144.2
033600     IF       SORT-KEY EQUAL TO ALL-Z                             ST1144.2
033700              PERFORM PASS GO TO SORT-WRITE-1.                    ST1144.2
033800 SORT-FAIL-1.                                                     ST1144.2
033900     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
034000     MOVE     ALL-Z TO CORRECT-BREAKDOWN.                         ST1144.2
034100     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
034200 SORT-WRITE-1.                                                    ST1144.2
034300     PERFORM  PRINT-DETAIL.                                       ST1144.2
034400 SORT-TEST-2.                                                     ST1144.2
034500     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1144.2
034600     PERFORM  READ-SORTIN.                                        ST1144.2
034700     IF       SORT-KEY EQUAL TO ALL-Z                             ST1144.2
034800              PERFORM PASS GO TO SORT-WRITE-2.                    ST1144.2
034900 SORT-FAIL-2.                                                     ST1144.2
035000     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
035100     MOVE     ALL-Z TO CORRECT-BREAKDOWN.                         ST1144.2
035200     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
035300 SORT-WRITE-2.                                                    ST1144.2
035400     PERFORM  PRINT-DETAIL.                                       ST1144.2
035500 SORT-TEST-3.                                                     ST1144.2
035600     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1144.2
035700     PERFORM  READ-SORTIN.                                        ST1144.2
035800     IF       SORT-KEY EQUAL TO ALL-Z                             ST1144.2
035900              PERFORM PASS GO TO SORT-WRITE-3.                    ST1144.2
036000 SORT-FAIL-3.                                                     ST1144.2
036100     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
036200     MOVE     ALL-Z TO CORRECT-BREAKDOWN.                         ST1144.2
036300     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
036400 SORT-WRITE-3.                                                    ST1144.2
036500     PERFORM  PRINT-DETAIL.                                       ST1144.2
036600 SORT-TEST-4.                                                     ST1144.2
036700     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1144.2
036800     PERFORM  READ-SORTIN 34 TIMES.                               ST1144.2
036900     IF       SORT-KEY EQUAL TO ALL-N                             ST1144.2
037000              PERFORM PASS GO TO SORT-WRITE-4.                    ST1144.2
037100 SORT-FAIL-4.                                                     ST1144.2
037200     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
037300     MOVE     ALL-N TO CORRECT-BREAKDOWN.                         ST1144.2
037400     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
037500 SORT-WRITE-4.                                                    ST1144.2
037600     PERFORM  PRINT-DETAIL.                                       ST1144.2
037700 SORT-TEST-5.                                                     ST1144.2
037800     MOVE     "SORT-TEST-5" TO PAR-NAME.                          ST1144.2
037900     PERFORM  READ-SORTIN.                                        ST1144.2
038000     IF       SORT-KEY EQUAL TO ALL-N                             ST1144.2
038100              PERFORM PASS GO TO SORT-WRITE-5.                    ST1144.2
038200 SORT-FAIL-5.                                                     ST1144.2
038300     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
038400     MOVE     ALL-N TO CORRECT-BREAKDOWN.                         ST1144.2
038500     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
038600 SORT-WRITE-5.                                                    ST1144.2
038700     PERFORM  PRINT-DETAIL.                                       ST1144.2
038800 SORT-TEST-6.                                                     ST1144.2
038900     PERFORM  READ-SORTIN.                                        ST1144.2
039000     MOVE     "SORT-TEST-6" TO PAR-NAME.                          ST1144.2
039100     IF       SORT-KEY EQUAL TO ALL-N                             ST1144.2
039200              PERFORM PASS GO TO SORT-WRITE-6.                    ST1144.2
039300 SORT-FAIL-6.                                                     ST1144.2
039400     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
039500     MOVE     ALL-N TO CORRECT-BREAKDOWN.                         ST1144.2
039600     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
039700 SORT-WRITE-6.                                                    ST1144.2
039800     PERFORM  PRINT-DETAIL.                                       ST1144.2
039900 SORT-TEST-7.                                                     ST1144.2
040000     MOVE     "SORT-TEST-7" TO PAR-NAME.                          ST1144.2
040100     PERFORM  READ-SORTIN 37 TIMES.                               ST1144.2
040200     IF       SORT-KEY EQUAL TO ALL-A                             ST1144.2
040300              PERFORM PASS GO TO SORT-WRITE-7.                    ST1144.2
040400 SORT-FAIL-7.                                                     ST1144.2
040500     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
040600     MOVE     ALL-A TO CORRECT-BREAKDOWN.                         ST1144.2
040700     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
040800 SORT-WRITE-7.                                                    ST1144.2
040900     PERFORM  PRINT-DETAIL.                                       ST1144.2
041000 SORT-TEST-8.                                                     ST1144.2
041100     MOVE     "SORT-TEST-8" TO PAR-NAME.                          ST1144.2
041200     PERFORM  READ-SORTIN.                                        ST1144.2
041300     IF       SORT-KEY EQUAL TO ALL-A                             ST1144.2
041400              PERFORM PASS GO TO SORT-WRITE-8.                    ST1144.2
041500 SORT-FAIL-8.                                                     ST1144.2
041600     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
041700     MOVE     ALL-A TO CORRECT-BREAKDOWN.                         ST1144.2
041800     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
041900 SORT-WRITE-8.                                                    ST1144.2
042000     PERFORM  PRINT-DETAIL.                                       ST1144.2
042100 SORT-TEST-9.                                                     ST1144.2
042200     MOVE     "SORT-TEST-9" TO PAR-NAME.                          ST1144.2
042300     PERFORM  READ-SORTIN.                                        ST1144.2
042400     IF       SORT-KEY EQUAL TO ALL-A                             ST1144.2
042500              PERFORM PASS GO TO SORT-WRITE-9.                    ST1144.2
042600 SORT-FAIL-9.                                                     ST1144.2
042700     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
042800     MOVE     ALL-A TO CORRECT-BREAKDOWN.                         ST1144.2
042900     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
043000 SORT-WRITE-9.                                                    ST1144.2
043100     PERFORM  PRINT-DETAIL.                                       ST1144.2
043200 SORT-TEST-10.                                                    ST1144.2
043300     MOVE     "SORT-TEST-10" TO PAR-NAME.                         ST1144.2
043400     READ     SORTIN-1N AT END                                    ST1144.2
043500              PERFORM PASS GO TO SORT-WRITE-10.                   ST1144.2
043600 SORT-FAIL-10.                                                    ST1144.2
043700     MOVE     SORT-KEY TO COMPUTED-BREAKDOWN.                     ST1144.2
043800     MOVE     SPACE TO CORRECT-BREAKDOWN.                         ST1144.2
043900     PERFORM  BREAKDOWN-PARA.                                     ST1144.2
044000     PERFORM  PRINT-DETAIL.                                       ST1144.2
044100     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1144.2
044200 SORT-WRITE-10.                                                   ST1144.2
044300     PERFORM  PRINT-DETAIL.                                       ST1144.2
044400     CLOSE    SORTIN-1N.                                          ST1144.2
044500     GO TO    CCVS-EXIT.                                          ST1144.2
044600 BREAKDOWN-PARA.                                                  ST1144.2
044700     PERFORM  FAIL.                                               ST1144.2
044800     MOVE     FIRST-20-CM TO COMPUTED-A.                          ST1144.2
044900     MOVE     FIRST-20-CR TO CORRECT-A.                           ST1144.2
045000     MOVE     "FIRST 20 CHARACTERS" TO RE-MARK.                   ST1144.2
045100     PERFORM  PRINT-DETAIL.                                       ST1144.2
045200     MOVE     SECOND-20-CM TO COMPUTED-A.                         ST1144.2
045300     MOVE     SECOND-20-CR TO CORRECT-A.                          ST1144.2
045400     MOVE     "LAST 13 CHARACTERS" TO RE-MARK.                    ST1144.2
045500 READ-SORTIN.                                                     ST1144.2
045600     READ     SORTIN-1N AT END GO TO READ-ERROR.                  ST1144.2
045700     ADD      1 TO UTIL-CTR.                                      ST1144.2
045800 READ-ERROR.                                                      ST1144.2
045900     MOVE     "READ-SORTIN" TO PAR-NAME.                          ST1144.2
046000     PERFORM  FAIL.                                               ST1144.2
046100     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1144.2
046200     MOVE     78 TO CORRECT-N.                                    ST1144.2
046300     MOVE     "TOO FEW RECORDS IN FILE" TO RE-MARK.               ST1144.2
046400     PERFORM  PRINT-DETAIL.                                       ST1144.2
046500 CCVS-EXIT SECTION.                                               ST1144.2
046600 CCVS-999999.                                                     ST1144.2
046700     GO TO CLOSE-FILES.                                           ST1144.2
      *END-OF,ST114M                                                                  
