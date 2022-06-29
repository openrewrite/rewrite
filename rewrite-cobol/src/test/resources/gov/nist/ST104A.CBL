      *HEADER,COBOL,ST104A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1044.2
000200 PROGRAM-ID.                                                      ST1044.2
000300     ST104A.                                                      ST1044.2
000400****************************************************************  ST1044.2
000500*                                                              *  ST1044.2
000600*    VALIDATION FOR:-                                          *  ST1044.2
000700*                                                              *  ST1044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1044.2
000900*                                                              *  ST1044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1044.2
001100*                                                              *  ST1044.2
001200****************************************************************  ST1044.2
001300*                                                              *  ST1044.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1044.2
001500*                                                              *  ST1044.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1044.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1044.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1044.2
001900*                                                              *  ST1044.2
002000****************************************************************  ST1044.2
002100 ENVIRONMENT DIVISION.                                            ST1044.2
002200 CONFIGURATION SECTION.                                           ST1044.2
002300 SOURCE-COMPUTER.                                                 ST1044.2
002400     XXXXX082.                                                    ST1044.2
002500 OBJECT-COMPUTER.                                                 ST1044.2
002600     XXXXX083.                                                    ST1044.2
002700 INPUT-OUTPUT SECTION.                                            ST1044.2
002800 FILE-CONTROL.                                                    ST1044.2
002900     SELECT PRINT-FILE ASSIGN TO                                  ST1044.2
003000     XXXXX055.                                                    ST1044.2
003100     SELECT   SORTOUT-1D ASSIGN TO                                ST1044.2
003200     XXXXP001.                                                    ST1044.2
003300 DATA DIVISION.                                                   ST1044.2
003400 FILE SECTION.                                                    ST1044.2
003500 FD  PRINT-FILE.                                                  ST1044.2
003600 01  PRINT-REC PICTURE X(120).                                    ST1044.2
003700 01  DUMMY-RECORD PICTURE X(120).                                 ST1044.2
003800 FD  SORTOUT-1D                                                   ST1044.2
003900     LABEL RECORDS STANDARD                                       ST1044.2
004000C    VALUE OF                                                     ST1044.2
004100C    XXXXX074                                                     ST1044.2
004200C    IS                                                           ST1044.2
004300C    XXXXX075                                                     ST1044.2
004400*    XXXXX069                                                     ST1044.2
004500     DATA RECORD IS SORTOUT-REC.                                  ST1044.2
004600 01  SORTOUT-REC.                                                 ST1044.2
004700     02 KEY-ITEM        PICTURE S999V999.                         ST1044.2
004800     02 NON-KEY-ITEM    PICTURE S9(12).                           ST1044.2
004900 WORKING-STORAGE SECTION.                                         ST1044.2
005000 77  U-TILITY           PICTURE 999V999     VALUE ZERO.           ST1044.2
005100 77  UTIL-SW            PICTURE 9           VALUE ZERO.           ST1044.2
005200 77  WRITE-COUNTER      PICTURE 999         VALUE ZERO.           ST1044.2
005300 01  TEST-RESULTS.                                                ST1044.2
005400     02 FILLER                   PIC X      VALUE SPACE.          ST1044.2
005500     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1044.2
005600     02 FILLER                   PIC X      VALUE SPACE.          ST1044.2
005700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1044.2
005800     02 FILLER                   PIC X      VALUE SPACE.          ST1044.2
005900     02  PAR-NAME.                                                ST1044.2
006000       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1044.2
006100       03  PARDOT-X              PIC X      VALUE SPACE.          ST1044.2
006200       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1044.2
006300     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1044.2
006400     02 RE-MARK                  PIC X(61).                       ST1044.2
006500 01  TEST-COMPUTED.                                               ST1044.2
006600     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1044.2
006700     02 FILLER                   PIC X(17)  VALUE                 ST1044.2
006800            "       COMPUTED=".                                   ST1044.2
006900     02 COMPUTED-X.                                               ST1044.2
007000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1044.2
007100     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1044.2
007200                                 PIC -9(9).9(9).                  ST1044.2
007300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1044.2
007400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1044.2
007500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1044.2
007600     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1044.2
007700         04 COMPUTED-18V0                    PIC -9(18).          ST1044.2
007800         04 FILLER                           PIC X.               ST1044.2
007900     03 FILLER PIC X(50) VALUE SPACE.                             ST1044.2
008000 01  TEST-CORRECT.                                                ST1044.2
008100     02 FILLER PIC X(30) VALUE SPACE.                             ST1044.2
008200     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1044.2
008300     02 CORRECT-X.                                                ST1044.2
008400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1044.2
008500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1044.2
008600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1044.2
008700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1044.2
008800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1044.2
008900     03      CR-18V0 REDEFINES CORRECT-A.                         ST1044.2
009000         04 CORRECT-18V0                     PIC -9(18).          ST1044.2
009100         04 FILLER                           PIC X.               ST1044.2
009200     03 FILLER PIC X(2) VALUE SPACE.                              ST1044.2
009300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1044.2
009400 01  CCVS-C-1.                                                    ST1044.2
009500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1044.2
009600-    "SS  PARAGRAPH-NAME                                          ST1044.2
009700-    "       REMARKS".                                            ST1044.2
009800     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1044.2
009900 01  CCVS-C-2.                                                    ST1044.2
010000     02 FILLER                     PIC X        VALUE SPACE.      ST1044.2
010100     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1044.2
010200     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1044.2
010300     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1044.2
010400     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1044.2
010500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1044.2
010600 01  REC-CT                        PIC 99       VALUE ZERO.       ST1044.2
010700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1044.2
010800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1044.2
010900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1044.2
011000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1044.2
011100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1044.2
011200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1044.2
011300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1044.2
011400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1044.2
011500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1044.2
011600 01  CCVS-H-1.                                                    ST1044.2
011700     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1044.2
011800     02  FILLER                    PIC X(42)    VALUE             ST1044.2
011900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1044.2
012000     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1044.2
012100 01  CCVS-H-2A.                                                   ST1044.2
012200   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1044.2
012300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1044.2
012400   02  FILLER                        PIC XXXX   VALUE             ST1044.2
012500     "4.2 ".                                                      ST1044.2
012600   02  FILLER                        PIC X(28)  VALUE             ST1044.2
012700            " COPY - NOT FOR DISTRIBUTION".                       ST1044.2
012800   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1044.2
012900                                                                  ST1044.2
013000 01  CCVS-H-2B.                                                   ST1044.2
013100   02  FILLER                        PIC X(15)  VALUE             ST1044.2
013200            "TEST RESULT OF ".                                    ST1044.2
013300   02  TEST-ID                       PIC X(9).                    ST1044.2
013400   02  FILLER                        PIC X(4)   VALUE             ST1044.2
013500            " IN ".                                               ST1044.2
013600   02  FILLER                        PIC X(12)  VALUE             ST1044.2
013700     " HIGH       ".                                              ST1044.2
013800   02  FILLER                        PIC X(22)  VALUE             ST1044.2
013900            " LEVEL VALIDATION FOR ".                             ST1044.2
014000   02  FILLER                        PIC X(58)  VALUE             ST1044.2
014100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1044.2
014200 01  CCVS-H-3.                                                    ST1044.2
014300     02  FILLER                      PIC X(34)  VALUE             ST1044.2
014400            " FOR OFFICIAL USE ONLY    ".                         ST1044.2
014500     02  FILLER                      PIC X(58)  VALUE             ST1044.2
014600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1044.2
014700     02  FILLER                      PIC X(28)  VALUE             ST1044.2
014800            "  COPYRIGHT   1985 ".                                ST1044.2
014900 01  CCVS-E-1.                                                    ST1044.2
015000     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1044.2
015100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1044.2
015200     02 ID-AGAIN                     PIC X(9).                    ST1044.2
015300     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1044.2
015400 01  CCVS-E-2.                                                    ST1044.2
015500     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1044.2
015600     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1044.2
015700     02 CCVS-E-2-2.                                               ST1044.2
015800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1044.2
015900         03 FILLER                   PIC X      VALUE SPACE.      ST1044.2
016000         03 ENDER-DESC               PIC X(44)  VALUE             ST1044.2
016100            "ERRORS ENCOUNTERED".                                 ST1044.2
016200 01  CCVS-E-3.                                                    ST1044.2
016300     02  FILLER                      PIC X(22)  VALUE             ST1044.2
016400            " FOR OFFICIAL USE ONLY".                             ST1044.2
016500     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1044.2
016600     02  FILLER                      PIC X(58)  VALUE             ST1044.2
016700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1044.2
016800     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1044.2
016900     02 FILLER                       PIC X(15)  VALUE             ST1044.2
017000             " COPYRIGHT 1985".                                   ST1044.2
017100 01  CCVS-E-4.                                                    ST1044.2
017200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1044.2
017300     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1044.2
017400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1044.2
017500     02 FILLER                       PIC X(40)  VALUE             ST1044.2
017600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1044.2
017700 01  XXINFO.                                                      ST1044.2
017800     02 FILLER                       PIC X(19)  VALUE             ST1044.2
017900            "*** INFORMATION ***".                                ST1044.2
018000     02 INFO-TEXT.                                                ST1044.2
018100       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1044.2
018200       04 XXCOMPUTED                 PIC X(20).                   ST1044.2
018300       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1044.2
018400       04 XXCORRECT                  PIC X(20).                   ST1044.2
018500     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1044.2
018600 01  HYPHEN-LINE.                                                 ST1044.2
018700     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1044.2
018800     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1044.2
018900-    "*****************************************".                 ST1044.2
019000     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1044.2
019100-    "******************************".                            ST1044.2
019200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1044.2
019300     "ST104A".                                                    ST1044.2
019400 PROCEDURE DIVISION.                                              ST1044.2
019500 CCVS1 SECTION.                                                   ST1044.2
019600 OPEN-FILES.                                                      ST1044.2
019700     OPEN     OUTPUT PRINT-FILE.                                  ST1044.2
019800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1044.2
019900     MOVE    SPACE TO TEST-RESULTS.                               ST1044.2
020000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1044.2
020100     GO TO CCVS1-EXIT.                                            ST1044.2
020200 CLOSE-FILES.                                                     ST1044.2
020300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1044.2
020400 TERMINATE-CCVS.                                                  ST1044.2
020500*    EXIT PROGRAM.                                                ST1044.2
020600*TERMINATE-CALL.                                                  ST1044.2
020700     STOP     RUN.                                                ST1044.2
020800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1044.2
020900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1044.2
021000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1044.2
021100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1044.2
021200     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1044.2
021300 PRINT-DETAIL.                                                    ST1044.2
021400     IF REC-CT NOT EQUAL TO ZERO                                  ST1044.2
021500             MOVE "." TO PARDOT-X                                 ST1044.2
021600             MOVE REC-CT TO DOTVALUE.                             ST1044.2
021700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1044.2
021800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1044.2
021900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1044.2
022000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1044.2
022100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1044.2
022200     MOVE SPACE TO CORRECT-X.                                     ST1044.2
022300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1044.2
022400     MOVE     SPACE TO RE-MARK.                                   ST1044.2
022500 HEAD-ROUTINE.                                                    ST1044.2
022600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1044.2
022700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1044.2
022800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1044.2
022900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1044.2
023000 COLUMN-NAMES-ROUTINE.                                            ST1044.2
023100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1044.2
023200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1044.2
023300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1044.2
023400 END-ROUTINE.                                                     ST1044.2
023500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1044.2
023600 END-RTN-EXIT.                                                    ST1044.2
023700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1044.2
023800 END-ROUTINE-1.                                                   ST1044.2
023900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1044.2
024000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1044.2
024100      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1044.2
024200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1044.2
024300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1044.2
024400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1044.2
024500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1044.2
024600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1044.2
024700  END-ROUTINE-12.                                                 ST1044.2
024800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1044.2
024900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1044.2
025000         MOVE "NO " TO ERROR-TOTAL                                ST1044.2
025100         ELSE                                                     ST1044.2
025200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1044.2
025300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1044.2
025400     PERFORM WRITE-LINE.                                          ST1044.2
025500 END-ROUTINE-13.                                                  ST1044.2
025600     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1044.2
025700         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1044.2
025800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1044.2
025900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1044.2
026000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1044.2
026100      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1044.2
026200          MOVE "NO " TO ERROR-TOTAL                               ST1044.2
026300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1044.2
026400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1044.2
026500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1044.2
026600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1044.2
026700 WRITE-LINE.                                                      ST1044.2
026800     ADD 1 TO RECORD-COUNT.                                       ST1044.2
026900*    IF RECORD-COUNT GREATER 42                                   ST1044.2
027000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1044.2
027100*        MOVE SPACE TO DUMMY-RECORD                               ST1044.2
027200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1044.2
027300*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1044.2
027400*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1044.2
027500*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1044.2
027600*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1044.2
027700*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1044.2
027800*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1044.2
027900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1044.2
028000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1044.2
028100*        MOVE ZERO TO RECORD-COUNT.                               ST1044.2
028200     PERFORM WRT-LN.                                              ST1044.2
028300 WRT-LN.                                                          ST1044.2
028400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1044.2
028500     MOVE SPACE TO DUMMY-RECORD.                                  ST1044.2
028600 BLANK-LINE-PRINT.                                                ST1044.2
028700     PERFORM WRT-LN.                                              ST1044.2
028800 FAIL-ROUTINE.                                                    ST1044.2
028900     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1044.2
029000            GO TO   FAIL-ROUTINE-WRITE.                           ST1044.2
029100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1044.2
029200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1044.2
029300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1044.2
029400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1044.2
029500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1044.2
029600     GO TO  FAIL-ROUTINE-EX.                                      ST1044.2
029700 FAIL-ROUTINE-WRITE.                                              ST1044.2
029800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1044.2
029900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1044.2
030000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1044.2
030100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1044.2
030200 FAIL-ROUTINE-EX. EXIT.                                           ST1044.2
030300 BAIL-OUT.                                                        ST1044.2
030400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1044.2
030500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1044.2
030600 BAIL-OUT-WRITE.                                                  ST1044.2
030700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1044.2
030800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1044.2
030900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1044.2
031000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1044.2
031100 BAIL-OUT-EX. EXIT.                                               ST1044.2
031200 CCVS1-EXIT.                                                      ST1044.2
031300     EXIT.                                                        ST1044.2
031400 SECT-ST104-0001 SECTION.                                         ST1044.2
031500 ST104-0001-01.                                                   ST1044.2
031600     OPEN     OUTPUT SORTOUT-1D.                                  ST1044.2
031700 BUILD-FILE.                                                      ST1044.2
031800     MOVE     +987654321078 TO NON-KEY-ITEM.                      ST1044.2
031900     MOVE     U-TILITY TO KEY-ITEM.                               ST1044.2
032000     IF       U-TILITY GREATER THAN 214.200                       ST1044.2
032100              MOVE 1 TO UTIL-SW.                                  ST1044.2
032200     WRITE    SORTOUT-REC.                                        ST1044.2
032300     ADD      1 TO WRITE-COUNTER ON SIZE ERROR                    ST1044.2
032400              MOVE "SIZE ERROR ENCOUNTERED" TO RE-MARK            ST1044.2
032500              GO TO BUILD-FILE-FAIL.                              ST1044.2
032600     IF       UTIL-SW EQUAL TO 1                                  ST1044.2
032700              SUBTRACT 002.142 FROM U-TILITY                      ST1044.2
032800              ELSE                                                ST1044.2
032900              ADD 002.142 TO U-TILITY.                            ST1044.2
033000     IF       U-TILITY NOT EQUAL TO ZERO                          ST1044.2
033100              GO TO BUILD-FILE.                                   ST1044.2
033200     MOVE     +987654321078 TO NON-KEY-ITEM.                      ST1044.2
033300     MOVE     U-TILITY TO KEY-ITEM.                               ST1044.2
033400     WRITE    SORTOUT-REC.                                        ST1044.2
033500     ADD      1 TO WRITE-COUNTER ON SIZE ERROR                    ST1044.2
033600              MOVE "SIZE ERROR FOUND" TO RE-MARK                  ST1044.2
033700              GO TO BUILD-FILE-FAIL.                              ST1044.2
033800 BUILD-FILE-TEST.                                                 ST1044.2
033900     IF       WRITE-COUNTER EQUAL TO 203                          ST1044.2
034000              PERFORM PASS GO TO BUILD-FILE-WRITE.                ST1044.2
034100 BUILD-FILE-FAIL.                                                 ST1044.2
034200     MOVE     WRITE-COUNTER TO COMPUTED-N.                        ST1044.2
034300     MOVE     203 TO CORRECT-N.                                   ST1044.2
034400     PERFORM  FAIL.                                               ST1044.2
034500 BUILD-FILE-WRITE.                                                ST1044.2
034600     MOVE     "TAPE BEING BUILT" TO FEATURE.                      ST1044.2
034700     MOVE     "BUILD-FILE-TEST" TO PAR-NAME.                      ST1044.2
034800     PERFORM  PRINT-DETAIL.                                       ST1044.2
034900     CLOSE    SORTOUT-1D.                                         ST1044.2
035000 CCVS-EXIT SECTION.                                               ST1044.2
035100 CCVS-999999.                                                     ST1044.2
035200     GO TO   CLOSE-FILES.                                         ST1044.2
      *END-OF,ST104A                                                                  
