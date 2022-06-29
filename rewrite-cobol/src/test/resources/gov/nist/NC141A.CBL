      *HEADER,COBOL,NC141A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1414.2
000200 PROGRAM-ID.                                                      NC1414.2
000300     NC141A.                                                      NC1414.2
000400****************************************************************  NC1414.2
000500*                                                              *  NC1414.2
000600*    VALIDATION FOR:-                                          *  NC1414.2
000700*                                                              *  NC1414.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1414.2
000900*                                                              *  NC1414.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1414.2
001100*                                                              *  NC1414.2
001200****************************************************************  NC1414.2
001300*                                                              *  NC1414.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1414.2
001500*                                                              *  NC1414.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1414.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1414.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1414.2
001900*                                                              *  NC1414.2
002000****************************************************************  NC1414.2
002100*                                                                 NC1414.2
002200*    PROGRAM NC141A TESTS FORMAT 1 AND 2 OF THE "SET"             NC1414.2
002300*    STATEMENT USING IDENTIFIERS INDEXED BY RELATIVE INDEXES      NC1414.2
002400*    AND NUMERIC LITERALS.                                        NC1414.2
002500*                                                                 NC1414.2
002600 ENVIRONMENT DIVISION.                                            NC1414.2
002700 CONFIGURATION SECTION.                                           NC1414.2
002800 SOURCE-COMPUTER.                                                 NC1414.2
002900     XXXXX082.                                                    NC1414.2
003000 OBJECT-COMPUTER.                                                 NC1414.2
003100     XXXXX083.                                                    NC1414.2
003200 INPUT-OUTPUT SECTION.                                            NC1414.2
003300 FILE-CONTROL.                                                    NC1414.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1414.2
003500     XXXXX055.                                                    NC1414.2
003600 DATA DIVISION.                                                   NC1414.2
003700 FILE SECTION.                                                    NC1414.2
003800 FD  PRINT-FILE.                                                  NC1414.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1414.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1414.2
004100 WORKING-STORAGE SECTION.                                         NC1414.2
004200 01  TABLE1.                                                      NC1414.2
004300     02  TABLE1-REC              PICTURE 99                       NC1414.2
004400                                 OCCURS 100 TIMES                 NC1414.2
004500                                 INDEXED BY INDEX1.               NC1414.2
004600 01  TABLE2.                                                      NC1414.2
004700     02  TABLE2-REC              PICTURE 99                       NC1414.2
004800                                 OCCURS 12 TIMES                  NC1414.2
004900                                 INDEXED BY INDEX2.               NC1414.2
005000 01  INDEX-ID                    PIC 999         VALUE ZERO.      NC1414.2
005100 01  TEST-RESULTS.                                                NC1414.2
005200     02 FILLER                   PIC X      VALUE SPACE.          NC1414.2
005300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1414.2
005400     02 FILLER                   PIC X      VALUE SPACE.          NC1414.2
005500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1414.2
005600     02 FILLER                   PIC X      VALUE SPACE.          NC1414.2
005700     02  PAR-NAME.                                                NC1414.2
005800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1414.2
005900       03  PARDOT-X              PIC X      VALUE SPACE.          NC1414.2
006000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1414.2
006100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1414.2
006200     02 RE-MARK                  PIC X(61).                       NC1414.2
006300 01  TEST-COMPUTED.                                               NC1414.2
006400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1414.2
006500     02 FILLER                   PIC X(17)  VALUE                 NC1414.2
006600            "       COMPUTED=".                                   NC1414.2
006700     02 COMPUTED-X.                                               NC1414.2
006800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1414.2
006900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1414.2
007000                                 PIC -9(9).9(9).                  NC1414.2
007100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1414.2
007200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1414.2
007300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1414.2
007400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1414.2
007500         04 COMPUTED-18V0                    PIC -9(18).          NC1414.2
007600         04 FILLER                           PIC X.               NC1414.2
007700     03 FILLER PIC X(50) VALUE SPACE.                             NC1414.2
007800 01  TEST-CORRECT.                                                NC1414.2
007900     02 FILLER PIC X(30) VALUE SPACE.                             NC1414.2
008000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1414.2
008100     02 CORRECT-X.                                                NC1414.2
008200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1414.2
008300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1414.2
008400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1414.2
008500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1414.2
008600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1414.2
008700     03      CR-18V0 REDEFINES CORRECT-A.                         NC1414.2
008800         04 CORRECT-18V0                     PIC -9(18).          NC1414.2
008900         04 FILLER                           PIC X.               NC1414.2
009000     03 FILLER PIC X(2) VALUE SPACE.                              NC1414.2
009100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1414.2
009200 01  CCVS-C-1.                                                    NC1414.2
009300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1414.2
009400-    "SS  PARAGRAPH-NAME                                          NC1414.2
009500-    "       REMARKS".                                            NC1414.2
009600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1414.2
009700 01  CCVS-C-2.                                                    NC1414.2
009800     02 FILLER                     PIC X        VALUE SPACE.      NC1414.2
009900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1414.2
010000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1414.2
010100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1414.2
010200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1414.2
010300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1414.2
010400 01  REC-CT                        PIC 99       VALUE ZERO.       NC1414.2
010500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1414.2
010600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1414.2
010700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1414.2
010800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1414.2
010900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1414.2
011000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1414.2
011100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1414.2
011200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1414.2
011300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1414.2
011400 01  CCVS-H-1.                                                    NC1414.2
011500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1414.2
011600     02  FILLER                    PIC X(42)    VALUE             NC1414.2
011700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1414.2
011800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1414.2
011900 01  CCVS-H-2A.                                                   NC1414.2
012000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1414.2
012100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1414.2
012200   02  FILLER                        PIC XXXX   VALUE             NC1414.2
012300     "4.2 ".                                                      NC1414.2
012400   02  FILLER                        PIC X(28)  VALUE             NC1414.2
012500            " COPY - NOT FOR DISTRIBUTION".                       NC1414.2
012600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1414.2
012700                                                                  NC1414.2
012800 01  CCVS-H-2B.                                                   NC1414.2
012900   02  FILLER                        PIC X(15)  VALUE             NC1414.2
013000            "TEST RESULT OF ".                                    NC1414.2
013100   02  TEST-ID                       PIC X(9).                    NC1414.2
013200   02  FILLER                        PIC X(4)   VALUE             NC1414.2
013300            " IN ".                                               NC1414.2
013400   02  FILLER                        PIC X(12)  VALUE             NC1414.2
013500     " HIGH       ".                                              NC1414.2
013600   02  FILLER                        PIC X(22)  VALUE             NC1414.2
013700            " LEVEL VALIDATION FOR ".                             NC1414.2
013800   02  FILLER                        PIC X(58)  VALUE             NC1414.2
013900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1414.2
014000 01  CCVS-H-3.                                                    NC1414.2
014100     02  FILLER                      PIC X(34)  VALUE             NC1414.2
014200            " FOR OFFICIAL USE ONLY    ".                         NC1414.2
014300     02  FILLER                      PIC X(58)  VALUE             NC1414.2
014400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1414.2
014500     02  FILLER                      PIC X(28)  VALUE             NC1414.2
014600            "  COPYRIGHT   1985 ".                                NC1414.2
014700 01  CCVS-E-1.                                                    NC1414.2
014800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1414.2
014900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1414.2
015000     02 ID-AGAIN                     PIC X(9).                    NC1414.2
015100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1414.2
015200 01  CCVS-E-2.                                                    NC1414.2
015300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1414.2
015400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1414.2
015500     02 CCVS-E-2-2.                                               NC1414.2
015600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1414.2
015700         03 FILLER                   PIC X      VALUE SPACE.      NC1414.2
015800         03 ENDER-DESC               PIC X(44)  VALUE             NC1414.2
015900            "ERRORS ENCOUNTERED".                                 NC1414.2
016000 01  CCVS-E-3.                                                    NC1414.2
016100     02  FILLER                      PIC X(22)  VALUE             NC1414.2
016200            " FOR OFFICIAL USE ONLY".                             NC1414.2
016300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1414.2
016400     02  FILLER                      PIC X(58)  VALUE             NC1414.2
016500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1414.2
016600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1414.2
016700     02 FILLER                       PIC X(15)  VALUE             NC1414.2
016800             " COPYRIGHT 1985".                                   NC1414.2
016900 01  CCVS-E-4.                                                    NC1414.2
017000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1414.2
017100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1414.2
017200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1414.2
017300     02 FILLER                       PIC X(40)  VALUE             NC1414.2
017400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1414.2
017500 01  XXINFO.                                                      NC1414.2
017600     02 FILLER                       PIC X(19)  VALUE             NC1414.2
017700            "*** INFORMATION ***".                                NC1414.2
017800     02 INFO-TEXT.                                                NC1414.2
017900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1414.2
018000       04 XXCOMPUTED                 PIC X(20).                   NC1414.2
018100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1414.2
018200       04 XXCORRECT                  PIC X(20).                   NC1414.2
018300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1414.2
018400 01  HYPHEN-LINE.                                                 NC1414.2
018500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1414.2
018600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1414.2
018700-    "*****************************************".                 NC1414.2
018800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1414.2
018900-    "******************************".                            NC1414.2
019000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1414.2
019100     "NC141A".                                                    NC1414.2
019200 PROCEDURE DIVISION.                                              NC1414.2
019300 CCVS1 SECTION.                                                   NC1414.2
019400 OPEN-FILES.                                                      NC1414.2
019500     OPEN     OUTPUT PRINT-FILE.                                  NC1414.2
019600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1414.2
019700     MOVE    SPACE TO TEST-RESULTS.                               NC1414.2
019800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1414.2
019900     GO TO CCVS1-EXIT.                                            NC1414.2
020000 CLOSE-FILES.                                                     NC1414.2
020100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1414.2
020200 TERMINATE-CCVS.                                                  NC1414.2
020300*    EXIT PROGRAM.                                                NC1414.2
020400*TERMINATE-CALL.                                                  NC1414.2
020500     STOP     RUN.                                                NC1414.2
020600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1414.2
020700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1414.2
020800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1414.2
020900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1414.2
021000     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1414.2
021100 PRINT-DETAIL.                                                    NC1414.2
021200     IF REC-CT NOT EQUAL TO ZERO                                  NC1414.2
021300             MOVE "." TO PARDOT-X                                 NC1414.2
021400             MOVE REC-CT TO DOTVALUE.                             NC1414.2
021500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1414.2
021600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1414.2
021700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1414.2
021800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1414.2
021900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1414.2
022000     MOVE SPACE TO CORRECT-X.                                     NC1414.2
022100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1414.2
022200     MOVE     SPACE TO RE-MARK.                                   NC1414.2
022300 HEAD-ROUTINE.                                                    NC1414.2
022400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1414.2
022500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1414.2
022600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1414.2
022700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1414.2
022800 COLUMN-NAMES-ROUTINE.                                            NC1414.2
022900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1414.2
023000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1414.2
023100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1414.2
023200 END-ROUTINE.                                                     NC1414.2
023300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1414.2
023400 END-RTN-EXIT.                                                    NC1414.2
023500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1414.2
023600 END-ROUTINE-1.                                                   NC1414.2
023700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1414.2
023800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1414.2
023900      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1414.2
024000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1414.2
024100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1414.2
024200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1414.2
024300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1414.2
024400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1414.2
024500  END-ROUTINE-12.                                                 NC1414.2
024600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1414.2
024700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1414.2
024800         MOVE "NO " TO ERROR-TOTAL                                NC1414.2
024900         ELSE                                                     NC1414.2
025000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1414.2
025100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1414.2
025200     PERFORM WRITE-LINE.                                          NC1414.2
025300 END-ROUTINE-13.                                                  NC1414.2
025400     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1414.2
025500         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1414.2
025600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1414.2
025700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1414.2
025800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1414.2
025900      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1414.2
026000          MOVE "NO " TO ERROR-TOTAL                               NC1414.2
026100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1414.2
026200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1414.2
026300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1414.2
026400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1414.2
026500 WRITE-LINE.                                                      NC1414.2
026600     ADD 1 TO RECORD-COUNT.                                       NC1414.2
026700*    IF RECORD-COUNT GREATER 42                                   NC1414.2
026800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1414.2
026900*        MOVE SPACE TO DUMMY-RECORD                               NC1414.2
027000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1414.2
027100*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1414.2
027200*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1414.2
027300*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1414.2
027400*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1414.2
027500*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1414.2
027600*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1414.2
027700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1414.2
027800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1414.2
027900*        MOVE ZERO TO RECORD-COUNT.                               NC1414.2
028000     PERFORM WRT-LN.                                              NC1414.2
028100 WRT-LN.                                                          NC1414.2
028200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1414.2
028300     MOVE SPACE TO DUMMY-RECORD.                                  NC1414.2
028400 BLANK-LINE-PRINT.                                                NC1414.2
028500     PERFORM WRT-LN.                                              NC1414.2
028600 FAIL-ROUTINE.                                                    NC1414.2
028700     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1414.2
028800            GO TO FAIL-ROUTINE-WRITE.                             NC1414.2
028900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1414.2
029000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1414.2
029100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1414.2
029200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1414.2
029300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1414.2
029400     GO TO  FAIL-ROUTINE-EX.                                      NC1414.2
029500 FAIL-ROUTINE-WRITE.                                              NC1414.2
029600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1414.2
029700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1414.2
029800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1414.2
029900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1414.2
030000 FAIL-ROUTINE-EX. EXIT.                                           NC1414.2
030100 BAIL-OUT.                                                        NC1414.2
030200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1414.2
030300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1414.2
030400 BAIL-OUT-WRITE.                                                  NC1414.2
030500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1414.2
030600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1414.2
030700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1414.2
030800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1414.2
030900 BAIL-OUT-EX. EXIT.                                               NC1414.2
031000 CCVS1-EXIT.                                                      NC1414.2
031100     EXIT.                                                        NC1414.2
031200 SECT-NC141A-001 SECTION.                                         NC1414.2
031300 INIT-PARA.                                                       NC1414.2
031400     MOVE "VI-127 6.22.4" TO ANSI-REFERENCE.                      NC1414.2
031500 BUILD-TABLE2.                                                    NC1414.2
031600     MOVE 21 TO TABLE2-REC (1).                                   NC1414.2
031700     MOVE 02 TO TABLE2-REC (2).                                   NC1414.2
031800     MOVE 03 TO TABLE2-REC (3).                                   NC1414.2
031900     MOVE 11 TO TABLE2-REC (4).                                   NC1414.2
032000     MOVE 05 TO TABLE2-REC (5).                                   NC1414.2
032100     MOVE 10 TO TABLE2-REC (6).                                   NC1414.2
032200     MOVE 26 TO TABLE2-REC (7).                                   NC1414.2
032300     MOVE 02 TO TABLE2-REC (8).                                   NC1414.2
032400     MOVE 16 TO TABLE2-REC (9).                                   NC1414.2
032500     MOVE 62 TO TABLE2-REC (10).                                  NC1414.2
032600     MOVE 10 TO TABLE2-REC (11).                                  NC1414.2
032700     MOVE 04 TO TABLE2-REC (12).                                  NC1414.2
032800 SET-TEST-1.                                                      NC1414.2
032900     MOVE "SET ... TO" TO FEATURE.                                NC1414.2
033000     SET INDEX1 TO 1.                                             NC1414.2
033100     SET INDEX2 TO 7.                                             NC1414.2
033200     SET INDEX1 TO TABLE2-REC (INDEX2).                           NC1414.2
033300     IF INDEX1 EQUAL TO 26                                        NC1414.2
033400        PERFORM PASS                                              NC1414.2
033500        ELSE GO TO SET-FAIL-1.                                    NC1414.2
033600     GO TO SET-WRITE-1.                                           NC1414.2
033700 SET-DELETE-1.                                                    NC1414.2
033800     PERFORM DE-LETE.                                             NC1414.2
033900     GO TO SET-WRITE-1.                                           NC1414.2
034000 SET-FAIL-1.                                                      NC1414.2
034100     PERFORM FAIL.                                                NC1414.2
034200     SET INDEX-ID TO INDEX1.                                      NC1414.2
034300     MOVE INDEX-ID TO COMPUTED-18V0.                              NC1414.2
034400     MOVE 26 TO CORRECT-18V0.                                     NC1414.2
034500 SET-WRITE-1.                                                     NC1414.2
034600     MOVE "SET-TEST-1" TO PAR-NAME.                               NC1414.2
034700     PERFORM PRINT-DETAIL.                                        NC1414.2
034800 SET-TEST-2.                                                      NC1414.2
034900     MOVE "SET ... UP BY" TO FEATURE.                             NC1414.2
035000     SET INDEX1 TO 7.                                             NC1414.2
035100     SET INDEX2 TO 8.                                             NC1414.2
035200     SET INDEX1 UP BY TABLE2-REC (INDEX2).                        NC1414.2
035300     IF INDEX1 EQUAL TO 9                                         NC1414.2
035400        PERFORM PASS                                              NC1414.2
035500        ELSE GO TO SET-FAIL-2.                                    NC1414.2
035600     GO TO SET-WRITE-2.                                           NC1414.2
035700 SET-DELETE-2.                                                    NC1414.2
035800     PERFORM DE-LETE.                                             NC1414.2
035900     GO TO SET-WRITE-2.                                           NC1414.2
036000 SET-FAIL-2.                                                      NC1414.2
036100     PERFORM FAIL.                                                NC1414.2
036200     SET INDEX-ID TO INDEX1.                                      NC1414.2
036300     MOVE INDEX-ID TO COMPUTED-18V0.                              NC1414.2
036400     MOVE 09 TO CORRECT-18V0.                                     NC1414.2
036500 SET-WRITE-2.                                                     NC1414.2
036600     MOVE "SET-TEST-2" TO PAR-NAME.                               NC1414.2
036700     PERFORM PRINT-DETAIL.                                        NC1414.2
036800 SET-TEST-3.                                                      NC1414.2
036900     MOVE "SET ... DOWN BY" TO FEATURE.                           NC1414.2
037000     SET INDEX1 TO 56.                                            NC1414.2
037100     SET INDEX2 TO 9.                                             NC1414.2
037200     SET INDEX1 DOWN BY TABLE2-REC (INDEX2).                      NC1414.2
037300     IF INDEX1 EQUAL TO 40                                        NC1414.2
037400        PERFORM PASS                                              NC1414.2
037500        ELSE GO TO SET-FAIL-3.                                    NC1414.2
037600     GO TO SET-WRITE-3.                                           NC1414.2
037700 SET-DELETE-3.                                                    NC1414.2
037800     PERFORM DE-LETE.                                             NC1414.2
037900     GO TO SET-WRITE-3.                                           NC1414.2
038000 SET-FAIL-3.                                                      NC1414.2
038100     PERFORM FAIL.                                                NC1414.2
038200     SET INDEX-ID TO INDEX1.                                      NC1414.2
038300     MOVE INDEX-ID TO COMPUTED-18V0.                              NC1414.2
038400     MOVE 40 TO CORRECT-18V0.                                     NC1414.2
038500 SET-WRITE-3.                                                     NC1414.2
038600     MOVE "SET-TEST-3" TO PAR-NAME.                               NC1414.2
038700     PERFORM PRINT-DETAIL.                                        NC1414.2
038800 SET-TEST-4.                                                      NC1414.2
038900     MOVE "SET ... TO" TO FEATURE.                                NC1414.2
039000     SET INDEX1 TO 1.                                             NC1414.2
039100     SET INDEX2 TO 9.                                             NC1414.2
039200     SET INDEX1 TO TABLE2-REC (INDEX2 + 1).                       NC1414.2
039300     IF INDEX1 EQUAL TO 62                                        NC1414.2
039400        PERFORM PASS                                              NC1414.2
039500        ELSE GO TO SET-FAIL-4.                                    NC1414.2
039600     GO TO SET-WRITE-4.                                           NC1414.2
039700 SET-DELETE-4.                                                    NC1414.2
039800     PERFORM DE-LETE.                                             NC1414.2
039900     GO TO SET-WRITE-4.                                           NC1414.2
040000 SET-FAIL-4.                                                      NC1414.2
040100     PERFORM FAIL.                                                NC1414.2
040200     SET INDEX-ID TO INDEX1.                                      NC1414.2
040300     MOVE INDEX-ID TO COMPUTED-18V0.                              NC1414.2
040400     MOVE 62 TO CORRECT-18V0.                                     NC1414.2
040500 SET-WRITE-4.                                                     NC1414.2
040600     MOVE "SET-TEST-4" TO PAR-NAME.                               NC1414.2
040700     PERFORM PRINT-DETAIL.                                        NC1414.2
040800 SET-TEST-5.                                                      NC1414.2
040900     MOVE "SET ... UP BY" TO FEATURE.                             NC1414.2
041000     SET INDEX1 TO 10.                                            NC1414.2
041100     SET INDEX2 TO 12.                                            NC1414.2
041200     SET INDEX1 UP BY TABLE2-REC (INDEX2 - 1).                    NC1414.2
041300     IF INDEX1 EQUAL TO 20                                        NC1414.2
041400        PERFORM PASS                                              NC1414.2
041500        ELSE GO TO SET-FAIL-5.                                    NC1414.2
041600     GO TO SET-WRITE-5.                                           NC1414.2
041700 SET-DELETE-5.                                                    NC1414.2
041800     PERFORM DE-LETE.                                             NC1414.2
041900     GO TO SET-WRITE-5.                                           NC1414.2
042000 SET-FAIL-5.                                                      NC1414.2
042100     PERFORM FAIL.                                                NC1414.2
042200     SET INDEX-ID TO INDEX1.                                      NC1414.2
042300     MOVE INDEX-ID TO COMPUTED-18V0.                              NC1414.2
042400     MOVE 20 TO CORRECT-18V0.                                     NC1414.2
042500 SET-WRITE-5.                                                     NC1414.2
042600     MOVE "SET-TEST-5" TO PAR-NAME.                               NC1414.2
042700     PERFORM PRINT-DETAIL.                                        NC1414.2
042800 SET-TEST-6.                                                      NC1414.2
042900     MOVE "SET ... DOWN BY" TO FEATURE.                           NC1414.2
043000     SET INDEX1 TO 15.                                            NC1414.2
043100     SET INDEX2 TO 8.                                             NC1414.2
043200     SET INDEX1 DOWN BY TABLE2-REC (INDEX2 + 4).                  NC1414.2
043300     IF INDEX1 EQUAL TO 11                                        NC1414.2
043400        PERFORM PASS                                              NC1414.2
043500        ELSE GO TO SET-FAIL-6.                                    NC1414.2
043600     GO TO SET-WRITE-6.                                           NC1414.2
043700 SET-DELETE-6.                                                    NC1414.2
043800     PERFORM DE-LETE.                                             NC1414.2
043900     GO TO SET-WRITE-6.                                           NC1414.2
044000 SET-FAIL-6.                                                      NC1414.2
044100     PERFORM FAIL.                                                NC1414.2
044200     SET INDEX-ID TO INDEX1.                                      NC1414.2
044300     MOVE INDEX-ID TO COMPUTED-18V0.                              NC1414.2
044400     MOVE 11 TO CORRECT-18V0.                                     NC1414.2
044500 SET-WRITE-6.                                                     NC1414.2
044600     MOVE "SET-TEST-6" TO PAR-NAME.                               NC1414.2
044700     PERFORM PRINT-DETAIL.                                        NC1414.2
044800 SET-TEST-7.                                                      NC1414.2
044900     MOVE "SET ... TO" TO FEATURE.                                NC1414.2
045000     SET INDEX1 TO 1.                                             NC1414.2
045100     SET INDEX1 TO TABLE2-REC (1).                                NC1414.2
045200     IF INDEX1 EQUAL TO 21                                        NC1414.2
045300        PERFORM PASS                                              NC1414.2
045400        ELSE GO TO SET-FAIL-7.                                    NC1414.2
045500     GO TO SET-WRITE-7.                                           NC1414.2
045600 SET-DELETE-7.                                                    NC1414.2
045700     PERFORM DE-LETE.                                             NC1414.2
045800     GO TO SET-WRITE-7.                                           NC1414.2
045900 SET-FAIL-7.                                                      NC1414.2
046000     PERFORM FAIL.                                                NC1414.2
046100     SET INDEX-ID TO INDEX1.                                      NC1414.2
046200     MOVE INDEX-ID TO COMPUTED-18V0.                              NC1414.2
046300     MOVE 21 TO CORRECT-18V0.                                     NC1414.2
046400 SET-WRITE-7.                                                     NC1414.2
046500     MOVE "SET-TEST-7" TO PAR-NAME.                               NC1414.2
046600     PERFORM PRINT-DETAIL.                                        NC1414.2
046700 SET-TEST-8.                                                      NC1414.2
046800     MOVE "SET ... UP BY" TO FEATURE.                             NC1414.2
046900     SET INDEX1 TO 21.                                            NC1414.2
047000     SET INDEX1 UP BY TABLE2-REC (2).                             NC1414.2
047100     IF INDEX1 EQUAL TO 23                                        NC1414.2
047200        PERFORM PASS                                              NC1414.2
047300        ELSE GO TO SET-FAIL-8.                                    NC1414.2
047400     GO TO SET-WRITE-8.                                           NC1414.2
047500 SET-DELETE-8.                                                    NC1414.2
047600     PERFORM DE-LETE.                                             NC1414.2
047700     GO TO SET-WRITE-8.                                           NC1414.2
047800 SET-FAIL-8.                                                      NC1414.2
047900     PERFORM FAIL.                                                NC1414.2
048000     SET INDEX-ID TO INDEX1.                                      NC1414.2
048100     MOVE INDEX-ID TO COMPUTED-18V0.                              NC1414.2
048200     MOVE 23 TO CORRECT-18V0.                                     NC1414.2
048300 SET-WRITE-8.                                                     NC1414.2
048400     MOVE "SET-TEST-8" TO PAR-NAME.                               NC1414.2
048500     PERFORM PRINT-DETAIL.                                        NC1414.2
048600 SET-TEST-9.                                                      NC1414.2
048700     MOVE "SET ... DOWN BY" TO FEATURE.                           NC1414.2
048800     SET INDEX1 TO 23.                                            NC1414.2
048900     SET INDEX1 DOWN BY TABLE2-REC (3).                           NC1414.2
049000     IF INDEX1 EQUAL TO 20                                        NC1414.2
049100        PERFORM PASS                                              NC1414.2
049200        ELSE GO TO SET-FAIL-9.                                    NC1414.2
049300     GO TO SET-WRITE-9.                                           NC1414.2
049400 SET-DELETE-9.                                                    NC1414.2
049500     PERFORM DE-LETE.                                             NC1414.2
049600     GO TO SET-WRITE-9.                                           NC1414.2
049700 SET-FAIL-9.                                                      NC1414.2
049800     PERFORM FAIL.                                                NC1414.2
049900     SET INDEX-ID TO INDEX1.                                      NC1414.2
050000     MOVE INDEX-ID TO COMPUTED-18V0.                              NC1414.2
050100     MOVE 20 TO CORRECT-18V0.                                     NC1414.2
050200 SET-WRITE-9.                                                     NC1414.2
050300     MOVE "SET-TEST-9" TO PAR-NAME.                               NC1414.2
050400     PERFORM PRINT-DETAIL.                                        NC1414.2
050500 CCVS-EXIT SECTION.                                               NC1414.2
050600 CCVS-999999.                                                     NC1414.2
050700     GO TO CLOSE-FILES.                                           NC1414.2
      *END-OF,NC141A                                                                  
