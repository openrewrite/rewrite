      *HEADER,COBOL,NC138A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1384.2
000200 PROGRAM-ID.                                                      NC1384.2
000300     NC138A.                                                      NC1384.2
000400****************************************************************  NC1384.2
000500*                                                              *  NC1384.2
000600*    VALIDATION FOR:-                                          *  NC1384.2
000700*                                                              *  NC1384.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1384.2
000900*                                                              *  NC1384.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1384.2
001100*                                                              *  NC1384.2
001200****************************************************************  NC1384.2
001300*                                                              *  NC1384.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1384.2
001500*                                                              *  NC1384.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1384.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1384.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1384.2
001900*                                                              *  NC1384.2
002000****************************************************************  NC1384.2
002100*                                                                 NC1384.2
002200*    PROGRAM NC138A TESTS THE USE OF SPACES, COMMAS,              NC1384.2
002300*    SEMI-COLONS AND LEFT AND RIGHT PARENTHESIS AS SEPARATORS     NC1384.2
002400*    IN STATEMENTS WHICH REFERENCE TABLE ITEMS.                   NC1384.2
002500*                                                                 NC1384.2
002600 ENVIRONMENT DIVISION.                                            NC1384.2
002700 CONFIGURATION SECTION.                                           NC1384.2
002800 SOURCE-COMPUTER.                                                 NC1384.2
002900     XXXXX082.                                                    NC1384.2
003000 OBJECT-COMPUTER.                                                 NC1384.2
003100     XXXXX083.                                                    NC1384.2
003200 INPUT-OUTPUT SECTION.                                            NC1384.2
003300 FILE-CONTROL.                                                    NC1384.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1384.2
003500     XXXXX055.                                                    NC1384.2
003600 DATA DIVISION.                                                   NC1384.2
003700 FILE SECTION.                                                    NC1384.2
003800 FD  PRINT-FILE.                                                  NC1384.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1384.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1384.2
004100 WORKING-STORAGE SECTION.                                         NC1384.2
004200 77  WRK1    PIC S999  VALUE ZERO.                                NC1384.2
004300 77  TEMP  PIC S9(5).                                             NC1384.2
004400 77  EXPECTED-VALUE   PIC  S9(5).                                 NC1384.2
004500*    SUBSCRIPTS FOR REFERENCING TABLE ITEMS.                      NC1384.2
004600 77  S1     PIC  S999 VALUE 1.                                    NC1384.2
004700 77  S21    PIC  S999 VALUE 1.                                    NC1384.2
004800 77  S22    PIC  S999 SIGN IS LEADING SEPARATE CHARACTER          NC1384.2
004900        VALUE 1.                                                  NC1384.2
005000 77  S31    PIC  S999 COMPUTATIONAL  VALUE 1.                     NC1384.2
005100 77  S32    PIC  S999 SYNC LEFT VALUE 1.                          NC1384.2
005200 77  S33    PIC  S999            VALUE 1.                         NC1384.2
005300*    ONE DIMENSIONAL TABLE.                                       NC1384.2
005400 01  GRP-TAB1.                                                    NC1384.2
005500     02  ELEM1  PIC 99                                            NC1384.2
005600         OCCURS 60 TIMES.                                         NC1384.2
005700*    TWO DIMENSIONAL TABLE,  12 BY 5.                             NC1384.2
005800 01  GRP-TAP2.                                                    NC1384.2
005900     02  GRP-LEV2-O012F    OCCURS 12 TIMES.                       NC1384.2
006000         03  ELEM2   PIC 9999  COMPUTATIONAL                      NC1384.2
006100             OCCURS 5 TIMES.                                      NC1384.2
006200*    THREE DIMENSIONAL TABLE,  4 BY 3 BY 5.                       NC1384.2
006300 01  GRP-TAB3.                                                    NC1384.2
006400     02 GRP-LEV2-00004F                                           NC1384.2
006500           OCCURS 4 TIMES                                         NC1384.2
006600           INDEXED BY WRK-IX-0001.                                NC1384.2
006700        03 GRP-LEV3-O0003F    OCCURS 3 TIMES.                     NC1384.2
006800           04 ELEM3  PICTURE IS S999                              NC1384.2
006900              USAGE IS DISPLAY SIGN IS LEADING SEPARATE CHARACTER NC1384.2
007000              OCCURS 5 TIMES.                                     NC1384.2
007100 01  TEST-RESULTS.                                                NC1384.2
007200     02 FILLER                   PIC X      VALUE SPACE.          NC1384.2
007300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1384.2
007400     02 FILLER                   PIC X      VALUE SPACE.          NC1384.2
007500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1384.2
007600     02 FILLER                   PIC X      VALUE SPACE.          NC1384.2
007700     02  PAR-NAME.                                                NC1384.2
007800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1384.2
007900       03  PARDOT-X              PIC X      VALUE SPACE.          NC1384.2
008000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1384.2
008100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1384.2
008200     02 RE-MARK                  PIC X(61).                       NC1384.2
008300 01  TEST-COMPUTED.                                               NC1384.2
008400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1384.2
008500     02 FILLER                   PIC X(17)  VALUE                 NC1384.2
008600            "       COMPUTED=".                                   NC1384.2
008700     02 COMPUTED-X.                                               NC1384.2
008800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1384.2
008900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1384.2
009000                                 PIC -9(9).9(9).                  NC1384.2
009100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1384.2
009200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1384.2
009300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1384.2
009400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1384.2
009500         04 COMPUTED-18V0                    PIC -9(18).          NC1384.2
009600         04 FILLER                           PIC X.               NC1384.2
009700     03 FILLER PIC X(50) VALUE SPACE.                             NC1384.2
009800 01  TEST-CORRECT.                                                NC1384.2
009900     02 FILLER PIC X(30) VALUE SPACE.                             NC1384.2
010000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1384.2
010100     02 CORRECT-X.                                                NC1384.2
010200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1384.2
010300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1384.2
010400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1384.2
010500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1384.2
010600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1384.2
010700     03      CR-18V0 REDEFINES CORRECT-A.                         NC1384.2
010800         04 CORRECT-18V0                     PIC -9(18).          NC1384.2
010900         04 FILLER                           PIC X.               NC1384.2
011000     03 FILLER PIC X(2) VALUE SPACE.                              NC1384.2
011100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1384.2
011200 01  CCVS-C-1.                                                    NC1384.2
011300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1384.2
011400-    "SS  PARAGRAPH-NAME                                          NC1384.2
011500-    "       REMARKS".                                            NC1384.2
011600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1384.2
011700 01  CCVS-C-2.                                                    NC1384.2
011800     02 FILLER                     PIC X        VALUE SPACE.      NC1384.2
011900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1384.2
012000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1384.2
012100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1384.2
012200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1384.2
012300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1384.2
012400 01  REC-CT                        PIC 99       VALUE ZERO.       NC1384.2
012500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1384.2
012600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1384.2
012700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1384.2
012800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1384.2
012900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1384.2
013000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1384.2
013100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1384.2
013200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1384.2
013300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1384.2
013400 01  CCVS-H-1.                                                    NC1384.2
013500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1384.2
013600     02  FILLER                    PIC X(42)    VALUE             NC1384.2
013700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1384.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1384.2
013900 01  CCVS-H-2A.                                                   NC1384.2
014000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1384.2
014100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1384.2
014200   02  FILLER                        PIC XXXX   VALUE             NC1384.2
014300     "4.2 ".                                                      NC1384.2
014400   02  FILLER                        PIC X(28)  VALUE             NC1384.2
014500            " COPY - NOT FOR DISTRIBUTION".                       NC1384.2
014600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1384.2
014700                                                                  NC1384.2
014800 01  CCVS-H-2B.                                                   NC1384.2
014900   02  FILLER                        PIC X(15)  VALUE             NC1384.2
015000            "TEST RESULT OF ".                                    NC1384.2
015100   02  TEST-ID                       PIC X(9).                    NC1384.2
015200   02  FILLER                        PIC X(4)   VALUE             NC1384.2
015300            " IN ".                                               NC1384.2
015400   02  FILLER                        PIC X(12)  VALUE             NC1384.2
015500     " HIGH       ".                                              NC1384.2
015600   02  FILLER                        PIC X(22)  VALUE             NC1384.2
015700            " LEVEL VALIDATION FOR ".                             NC1384.2
015800   02  FILLER                        PIC X(58)  VALUE             NC1384.2
015900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1384.2
016000 01  CCVS-H-3.                                                    NC1384.2
016100     02  FILLER                      PIC X(34)  VALUE             NC1384.2
016200            " FOR OFFICIAL USE ONLY    ".                         NC1384.2
016300     02  FILLER                      PIC X(58)  VALUE             NC1384.2
016400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1384.2
016500     02  FILLER                      PIC X(28)  VALUE             NC1384.2
016600            "  COPYRIGHT   1985 ".                                NC1384.2
016700 01  CCVS-E-1.                                                    NC1384.2
016800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1384.2
016900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1384.2
017000     02 ID-AGAIN                     PIC X(9).                    NC1384.2
017100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1384.2
017200 01  CCVS-E-2.                                                    NC1384.2
017300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1384.2
017400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1384.2
017500     02 CCVS-E-2-2.                                               NC1384.2
017600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1384.2
017700         03 FILLER                   PIC X      VALUE SPACE.      NC1384.2
017800         03 ENDER-DESC               PIC X(44)  VALUE             NC1384.2
017900            "ERRORS ENCOUNTERED".                                 NC1384.2
018000 01  CCVS-E-3.                                                    NC1384.2
018100     02  FILLER                      PIC X(22)  VALUE             NC1384.2
018200            " FOR OFFICIAL USE ONLY".                             NC1384.2
018300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1384.2
018400     02  FILLER                      PIC X(58)  VALUE             NC1384.2
018500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1384.2
018600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1384.2
018700     02 FILLER                       PIC X(15)  VALUE             NC1384.2
018800             " COPYRIGHT 1985".                                   NC1384.2
018900 01  CCVS-E-4.                                                    NC1384.2
019000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1384.2
019100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1384.2
019200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1384.2
019300     02 FILLER                       PIC X(40)  VALUE             NC1384.2
019400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1384.2
019500 01  XXINFO.                                                      NC1384.2
019600     02 FILLER                       PIC X(19)  VALUE             NC1384.2
019700            "*** INFORMATION ***".                                NC1384.2
019800     02 INFO-TEXT.                                                NC1384.2
019900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1384.2
020000       04 XXCOMPUTED                 PIC X(20).                   NC1384.2
020100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1384.2
020200       04 XXCORRECT                  PIC X(20).                   NC1384.2
020300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1384.2
020400 01  HYPHEN-LINE.                                                 NC1384.2
020500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1384.2
020600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1384.2
020700-    "*****************************************".                 NC1384.2
020800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1384.2
020900-    "******************************".                            NC1384.2
021000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1384.2
021100     "NC138A".                                                    NC1384.2
021200 PROCEDURE DIVISION.                                              NC1384.2
021300 CCVS1 SECTION.                                                   NC1384.2
021400 OPEN-FILES.                                                      NC1384.2
021500     OPEN     OUTPUT PRINT-FILE.                                  NC1384.2
021600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1384.2
021700     MOVE    SPACE TO TEST-RESULTS.                               NC1384.2
021800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1384.2
021900     GO TO CCVS1-EXIT.                                            NC1384.2
022000 CLOSE-FILES.                                                     NC1384.2
022100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1384.2
022200 TERMINATE-CCVS.                                                  NC1384.2
022300*    EXIT PROGRAM.                                                NC1384.2
022400*TERMINATE-CALL.                                                  NC1384.2
022500     STOP     RUN.                                                NC1384.2
022600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1384.2
022700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1384.2
022800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1384.2
022900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1384.2
023000     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1384.2
023100 PRINT-DETAIL.                                                    NC1384.2
023200     IF REC-CT NOT EQUAL TO ZERO                                  NC1384.2
023300             MOVE "." TO PARDOT-X                                 NC1384.2
023400             MOVE REC-CT TO DOTVALUE.                             NC1384.2
023500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1384.2
023600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1384.2
023700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1384.2
023800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1384.2
023900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1384.2
024000     MOVE SPACE TO CORRECT-X.                                     NC1384.2
024100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1384.2
024200     MOVE     SPACE TO RE-MARK.                                   NC1384.2
024300 HEAD-ROUTINE.                                                    NC1384.2
024400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1384.2
024500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1384.2
024600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1384.2
024700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1384.2
024800 COLUMN-NAMES-ROUTINE.                                            NC1384.2
024900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1384.2
025000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1384.2
025100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1384.2
025200 END-ROUTINE.                                                     NC1384.2
025300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1384.2
025400 END-RTN-EXIT.                                                    NC1384.2
025500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1384.2
025600 END-ROUTINE-1.                                                   NC1384.2
025700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1384.2
025800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1384.2
025900      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1384.2
026000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1384.2
026100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1384.2
026200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1384.2
026300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1384.2
026400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1384.2
026500  END-ROUTINE-12.                                                 NC1384.2
026600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1384.2
026700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1384.2
026800         MOVE "NO " TO ERROR-TOTAL                                NC1384.2
026900         ELSE                                                     NC1384.2
027000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1384.2
027100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1384.2
027200     PERFORM WRITE-LINE.                                          NC1384.2
027300 END-ROUTINE-13.                                                  NC1384.2
027400     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1384.2
027500         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1384.2
027600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1384.2
027700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1384.2
027800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1384.2
027900      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1384.2
028000          MOVE "NO " TO ERROR-TOTAL                               NC1384.2
028100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1384.2
028200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1384.2
028300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1384.2
028400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1384.2
028500 WRITE-LINE.                                                      NC1384.2
028600     ADD 1 TO RECORD-COUNT.                                       NC1384.2
028700*    IF RECORD-COUNT GREATER 42                                   NC1384.2
028800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1384.2
028900*        MOVE SPACE TO DUMMY-RECORD                               NC1384.2
029000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1384.2
029100*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1384.2
029200*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1384.2
029300*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1384.2
029400*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1384.2
029500*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1384.2
029600*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1384.2
029700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1384.2
029800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1384.2
029900*        MOVE ZERO TO RECORD-COUNT.                               NC1384.2
030000     PERFORM WRT-LN.                                              NC1384.2
030100 WRT-LN.                                                          NC1384.2
030200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1384.2
030300     MOVE SPACE TO DUMMY-RECORD.                                  NC1384.2
030400 BLANK-LINE-PRINT.                                                NC1384.2
030500     PERFORM WRT-LN.                                              NC1384.2
030600 FAIL-ROUTINE.                                                    NC1384.2
030700     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1384.2
030800            GO TO FAIL-ROUTINE-WRITE.                             NC1384.2
030900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1384.2
031000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1384.2
031100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1384.2
031200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1384.2
031300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1384.2
031400     GO TO  FAIL-ROUTINE-EX.                                      NC1384.2
031500 FAIL-ROUTINE-WRITE.                                              NC1384.2
031600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1384.2
031700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1384.2
031800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1384.2
031900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1384.2
032000 FAIL-ROUTINE-EX. EXIT.                                           NC1384.2
032100 BAIL-OUT.                                                        NC1384.2
032200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1384.2
032300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1384.2
032400 BAIL-OUT-WRITE.                                                  NC1384.2
032500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1384.2
032600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1384.2
032700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1384.2
032800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1384.2
032900 BAIL-OUT-EX. EXIT.                                               NC1384.2
033000 CCVS1-EXIT.                                                      NC1384.2
033100     EXIT.                                                        NC1384.2
033200 SECT-NC138A-0001 SECTION.                                        NC1384.2
033300 NC138A-0001.                                                     NC1384.2
033400     MOVE "IV-4 4.2.1" TO ANSI-REFERENCE.                         NC1384.2
033500*    THIS SECTION STORES THE NUMBERS 1 THROUGH 60 IN THE 3 TABLES.NC1384.2
033600 MOVE-VALUE.                                                      NC1384.2
033700     ADD 1 TO WRK1.                                               NC1384.2
033800     MOVE WRK1 TO ELEM1 (S1)  ELEM2 (S21, S22)                    NC1384.2
033900                  ELEM3 (S31, S32, S33).                          NC1384.2
034000     IF WRK1 EQUAL TO 60   GO TO SECT-TH109-0002.                 NC1384.2
034100 INCRE-SUBS.                                                      NC1384.2
034200     ADD 1 TO S1.                                                 NC1384.2
034300     ADD 1 TO S22.                                                NC1384.2
034400     ADD 1 TO S33.                                                NC1384.2
034500     IF S22 LESS THAN 6 GO TO MOVE-VALUE.                         NC1384.2
034600     MOVE 1 TO S22 S33.                                           NC1384.2
034700     ADD 1 TO S21.                                                NC1384.2
034800     ADD 1 TO S32.                                                NC1384.2
034900     IF S32 LESS THAN 4 GO TO MOVE-VALUE.                         NC1384.2
035000     MOVE 1 TO S32.                                               NC1384.2
035100     ADD 1 TO S31.                                                NC1384.2
035200     GO TO MOVE-VALUE.                                            NC1384.2
035300 SECT-TH109-0002 SECTION.                                         NC1384.2
035400 SECT-0002-INIT.                                                  NC1384.2
035500     MOVE "LEFT PAREN SEPARATOR" TO FEATURE.                      NC1384.2
035600*       THIS SECTION TESTS THE USE OF LEFT PARENTHESIS AS A       NC1384.2
035700*    SEPARATOR IN REFERENCING TABLE ITEMS.                        NC1384.2
035800 SEP-INIT-001.                                                    NC1384.2
035900     MOVE 6 TO S1.                                                NC1384.2
036000 SEP-TEST-001.                                                    NC1384.2
036100     IF ELEM1(S1) IS EQUAL TO 6                                   NC1384.2
036200             PERFORM PASS                                         NC1384.2
036300             GO TO SEP-WRITE-001.                                 NC1384.2
036400         PERFORM FAIL.                                            NC1384.2
036500         GO TO SEP-FAIL-001.                                      NC1384.2
036600 SEP-DELETE-001.                                                  NC1384.2
036700     PERFORM DE-LETE.                                             NC1384.2
036800     GO TO SEP-WRITE-001.                                         NC1384.2
036900 SEP-FAIL-001.                                                    NC1384.2
037000     MOVE ELEM1 (S1) TO COMPUTED-18V0.                            NC1384.2
037100     MOVE 6 TO CORRECT-18V0.                                      NC1384.2
037200 SEP-WRITE-001.                                                   NC1384.2
037300     MOVE "SEP-TEST-001" TO PAR-NAME.                             NC1384.2
037400     PERFORM PRINT-DETAIL.                                        NC1384.2
037500 SEP-INIT-002.                                                    NC1384.2
037600     MOVE 2 TO S21 S22.                                           NC1384.2
037700 SEP-TEST-002.                                                    NC1384.2
037800     MOVE ELEM2(S21, S22) TO TEMP.                                NC1384.2
037900     IF TEMP EQUAL TO 7                                           NC1384.2
038000             PERFORM PASS                                         NC1384.2
038100             GO TO SEP-WRITE-002.                                 NC1384.2
038200         PERFORM FAIL.                                            NC1384.2
038300         GO TO SEP-FAIL-002.                                      NC1384.2
038400 SEP-DELETE-002.                                                  NC1384.2
038500     PERFORM DE-LETE.                                             NC1384.2
038600     GO TO SEP-WRITE-002.                                         NC1384.2
038700 SEP-FAIL-002.                                                    NC1384.2
038800     MOVE TEMP TO COMPUTED-18V0.                                  NC1384.2
038900     MOVE 7 TO CORRECT-18V0.                                      NC1384.2
039000 SEP-WRITE-002.                                                   NC1384.2
039100     MOVE "SEP-TEST-002" TO PAR-NAME.                             NC1384.2
039200     PERFORM PRINT-DETAIL.                                        NC1384.2
039300 SEP-INIT-003.                                                    NC1384.2
039400     MOVE 3 TO S31 S32 S33.                                       NC1384.2
039500 SEP-TEST-003.                                                    NC1384.2
039600     MOVE ELEM3(S31, S32, S33)  TO TEMP.                          NC1384.2
039700     IF TEMP EQUAL TO 43                                          NC1384.2
039800             PERFORM PASS                                         NC1384.2
039900             GO TO SEP-WRITE-003.                                 NC1384.2
040000         PERFORM FAIL.                                            NC1384.2
040100         GO TO SEP-FAIL-003.                                      NC1384.2
040200 SEP-DELETE-003.                                                  NC1384.2
040300     PERFORM DE-LETE.                                             NC1384.2
040400     GO TO SEP-WRITE-003.                                         NC1384.2
040500 SEP-FAIL-003.                                                    NC1384.2
040600     MOVE TEMP TO COMPUTED-18V0.                                  NC1384.2
040700     MOVE 43 TO CORRECT-18V0.                                     NC1384.2
040800 SEP-WRITE-003.                                                   NC1384.2
040900     MOVE "SEP-TEST-003" TO PAR-NAME.                             NC1384.2
041000     PERFORM PRINT-DETAIL.                                        NC1384.2
041100 SEP-INIT-004.                                                    NC1384.2
041200     MOVE "SPACES AS SEPARATOR" TO FEATURE.                       NC1384.2
041300     MOVE "SEP-TEST-004" TO PAR-NAME.                             NC1384.2
041400     MOVE 0 TO REC-CT.                                            NC1384.2
041500     MOVE 19 TO S1.                                               NC1384.2
041600*        THIS TEST USES SPACES AS SEPARATORS IN REFERENCING       NC1384.2
041700*    ONE DIMENSIONAL TABLE ELEMENTS.                              NC1384.2
041800     MOVE ZERO TO TEMP.                                           NC1384.2
041900     MOVE 19 TO EXPECTED-VALUE.                                   NC1384.2
042000 SEP-TEST-004-01.                                                 NC1384.2
042100     MOVE ELEM1 (S1 ) TO TEMP.                                    NC1384.2
042200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
042300 SEP-TEST-004-02.                                                 NC1384.2
042400     MOVE ELEM1    (S1) TO TEMP.                                  NC1384.2
042500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
042600 SEP-TEST-004-03.                                                 NC1384.2
042700     MOVE ELEM1   (S1     )   TO  TEMP.                           NC1384.2
042800     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
042900 SEP-TEST-004-04.                                                 NC1384.2
043000     MOVE ELEM1( S1) TO TEMP.                                     NC1384.2
043100     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
043200 SEP-TEST-004-05.                                                 NC1384.2
043300     MOVE ELEM1 ( S1) TO TEMP.                                    NC1384.2
043400     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
043500 SEP-TEST-004-06.                                                 NC1384.2
043600     MOVE ELEM1 ( S1 ) TO TEMP.                                   NC1384.2
043700     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
043800 SEP-TEST-004-07.                                                 NC1384.2
043900     MOVE ELEM1 ( 19) TO TEMP.                                    NC1384.2
044000     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
044100 SEP-TEST-004-08.                                                 NC1384.2
044200     MOVE ELEM1(S1 ) TO TEMP.                                     NC1384.2
044300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
044400 SEP-TEST-004-09.                                                 NC1384.2
044500     MOVE ELEM1 (   19     )   TO  TEMP.                          NC1384.2
044600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
044700     GO TO SEP-INIT-005.                                          NC1384.2
044800*         IF THE COMPILER REJECTS ANY OF THE ABOVE TABLE          NC1384.2
044900*    REFERENCES, DELETE THAT LINE OF CODE BY PLACING              NC1384.2
045000*    AN * IN COLUMN 7.  LEAVE THE PERFORM...THRU STATEMENT.       NC1384.2
045100*    THE ELEMENT DELETED APPEARS AS A FAILURE ON THE              NC1384.2
045200*    OUTPUT REPORT.                                               NC1384.2
045300 SEP-DELETE-004.                                                  NC1384.2
045400     PERFORM DE-LETE.                                             NC1384.2
045500     PERFORM SYNTAX-CHECK-WRITE.                                  NC1384.2
045600 SEP-INIT-005.                                                    NC1384.2
045700     MOVE "SEP-TEST-005" TO PAR-NAME.                             NC1384.2
045800     MOVE 0 TO REC-CT.                                            NC1384.2
045900     MOVE 10 TO S21.                                              NC1384.2
046000     MOVE 03 TO S22.                                              NC1384.2
046100     MOVE ZERO TO TEMP.                                           NC1384.2
046200     MOVE 48 TO EXPECTED-VALUE.                                   NC1384.2
046300*        THIS TEST USES SPACES AND COMMAS AS SEPARATORS IN        NC1384.2
046400*    REFERENCING TWO DIMENSIONAL TABLE ELEMENTS.                  NC1384.2
046500 SEP-TEST-005-01.                                                 NC1384.2
046600     MOVE ELEM2 (S21 S22) TO TEMP.                                NC1384.2
046700     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
046800 SEP-TEST-005-02.                                                 NC1384.2
046900     MOVE ELEM2 (S21,  S22)  TO  TEMP.                            NC1384.2
047000     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
047100 SEP-TEST-005-03.                                                 NC1384.2
047200     MOVE ELEM2 (S21, S22) TO TEMP.                               NC1384.2
047300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
047400 SEP-TEST-005-04.                                                 NC1384.2
047500     ADD ELEM2 ( S21   S22 ) TO TEMP.                             NC1384.2
047600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
047700 SEP-TEST-005-05.                                                 NC1384.2
047800     MOVE 96 TO TEMP.                                             NC1384.2
047900     SUBTRACT ELEM2(S21 S22) FROM TEMP.                           NC1384.2
048000     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
048100 SEP-TEST-005-06.                                                 NC1384.2
048200     MOVE ELEM2( S21,  S22) TO TEMP.                              NC1384.2
048300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
048400 SEP-TEST-005-07.                                                 NC1384.2
048500     MOVE ELEM2   (   S21   S22  ) TO TEMP.                       NC1384.2
048600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
048700 SEP-TEST-005-08.                                                 NC1384.2
048800     MOVE ELEM2 (S21 , S22) TO TEMP.                              NC1384.2
048900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
049000 SEP-TEST-005-09.                                                 NC1384.2
049100     ADD ELEM2 (3 5)  ELEM2(7, 3) TO TEMP.                        NC1384.2
049200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
049300 SEP-TEST-005-10.                                                 NC1384.2
049400     ADD ELEM2( 3  5 )  ELEM2 ( 7 3    )  TO  TEMP.               NC1384.2
049500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
049600     GO TO SEP-INIT-006.                                          NC1384.2
049700*         IF THE COMPILER REJECTS ANY OF THE ABOVE TABLE          NC1384.2
049800*    REFERENCES, DELETE THAT LINE OF CODE BY PLACING              NC1384.2
049900*    AN ASTERISK IN COLUMN 7. LEAVE THE PERFORM...THRU STATEMENT. NC1384.2
050000*    THE ELEMENT DELETED APPEARS AS A FAILURE ON THE              NC1384.2
050100*    OUTPUT REPORT.                                               NC1384.2
050200 SEP-DELETE-005.                                                  NC1384.2
050300     PERFORM DE-LETE.                                             NC1384.2
050400     PERFORM SYNTAX-CHECK-WRITE.                                  NC1384.2
050500 SEP-INIT-006.                                                    NC1384.2
050600     MOVE "SEP-TEST-006" TO PAR-NAME.                             NC1384.2
050700     MOVE 0 TO REC-CT.                                            NC1384.2
050800     MOVE ZERO TO TEMP.                                           NC1384.2
050900     MOVE 3 TO S31.                                               NC1384.2
051000     MOVE 2 TO S32 S33.                                           NC1384.2
051100     MOVE 37 TO EXPECTED-VALUE.                                   NC1384.2
051200*         THIS TEST USES SPACES AND COMMAS AS SEPARATORS IN       NC1384.2
051300*    REFERENCING THREE DIMENSIONAL TABLE ELEMENTS.                NC1384.2
051400 SEP-TEST-006-01.                                                 NC1384.2
051500     MOVE ELEM3(S31 S32 S33) TO TEMP.                             NC1384.2
051600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
051700 SEP-TEST-006-02.                                                 NC1384.2
051800     MOVE ELEM3(S31, S32 S33)   TO TEMP.                          NC1384.2
051900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
052000 SEP-TEST-006-03.                                                 NC1384.2
052100     ADD ELEM3  (  S31  S32  S33  )   TO  TEMP.                   NC1384.2
052200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
052300 SEP-TEST-006-04.                                                 NC1384.2
052400     MOVE 74 TO TEMP.                                             NC1384.2
052500     SUBTRACT ELEM3(S31 , S32 , S33) FROM TEMP.                   NC1384.2
052600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
052700 SEP-TEST-006-05.                                                 NC1384.2
052800     MOVE 37 TO TEMP.                                             NC1384.2
052900     IF ELEM3 ( S31, S32, S33 )                                   NC1384.2
053000              NOT EQUAL TO TEMP                                   NC1384.2
053100              MOVE ZERO TO TEMP.                                  NC1384.2
053200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
053300 SEP-TEST-006-06.                                                 NC1384.2
053400     MULTIPLY ELEM3 (3 2 2) BY 1                                  NC1384.2
053500              GIVING TEMP.                                        NC1384.2
053600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
053700 SEP-TEST-006-07.                                                 NC1384.2
053800     ADD ELEM3 (1, 1, 1)   ELEM3( 3  2  1 )                       NC1384.2
053900              GIVING TEMP.                                        NC1384.2
054000     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
054100     GO TO SEP-INIT-007.                                          NC1384.2
054200*         IF THE COMPILER REJECTS ANY OF THE ABOVE TABLE          NC1384.2
054300*    REFERENCES, DELETE THAT LINE OF CODE BY PLACING              NC1384.2
054400*    AN * IN COLUMN 7.  LEAVE THE PERFORM...THRU STATEMENT.       NC1384.2
054500*    THE ELEMENT DELETED APPEARS AS A FAILURE ON THE              NC1384.2
054600*    OUTPUT REPORT.                                               NC1384.2
054700 SEP-DELETE-006.                                                  NC1384.2
054800     PERFORM DE-LETE.                                             NC1384.2
054900     PERFORM SYNTAX-CHECK-WRITE.                                  NC1384.2
055000 SEP-INIT-007.                                                    NC1384.2
055100     MOVE "SEP-TEST-007" TO PAR-NAME.                             NC1384.2
055200     MOVE ZERO TO REC-CT.                                         NC1384.2
055300     MOVE ZERO TO TEMP.                                           NC1384.2
055400     MOVE 12 TO EXPECTED-VALUE.                                   NC1384.2
055500*        THIS TEST USES SIGNED POSITIVE INTEGERS AS               NC1384.2
055600*    SUBSCRIPTS AND SPACES AND COMMAS AS SEPARATORS.              NC1384.2
055700 SEP-TEST-007-01.                                                 NC1384.2
055800     MOVE ELEM1(+12) TO TEMP.                                     NC1384.2
055900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
056000 SEP-TEST-007-02.                                                 NC1384.2
056100     IF ELEM2(+3 +2) EQUAL TO 12                                  NC1384.2
056200        MOVE 12 TO TEMP.                                          NC1384.2
056300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
056400 SEP-TEST-007-03.                                                 NC1384.2
056500     ADD ELEM2 (+3, +2) TO TEMP.                                  NC1384.2
056600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
056700 SEP-TEST-007-04.                                                 NC1384.2
056800     MOVE 24 TO TEMP.                                             NC1384.2
056900     SUBTRACT ELEM2 ( +3   +2 ) FROM TEMP.                        NC1384.2
057000     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
057100 SEP-TEST-007-05.                                                 NC1384.2
057200     MULTIPLY ELEM3(+1, +3, +2) BY +1                             NC1384.2
057300             GIVING TEMP.                                         NC1384.2
057400     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
057500 SEP-TEST-007-06.                                                 NC1384.2
057600     DIVIDE ELEM3(+1 +3 +2) BY 1                                  NC1384.2
057700             GIVING TEMP.                                         NC1384.2
057800     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
057900 SEP-TEST-007-07.                                                 NC1384.2
058000     MOVE ELEM3 ( +1,  +3,  +2 ) TO TEMP.                         NC1384.2
058100     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
058200     GO TO CCVS-EXIT.                                             NC1384.2
058300*         IF THE COMPILER REJECTS ANY OF THE ABOVE TABLE          NC1384.2
058400*    REFERENCES, DELETE THAT LINE OF CODE BY PLACING              NC1384.2
058500*    AN * IN COLUMN 7.  LEAVE THE PERFORM...THRU STATEMENT.       NC1384.2
058600*    THE ELEMENT DELETED APPEARS AS A FAILURE ON THE              NC1384.2
058700*    OUTPUT REPORT.                                               NC1384.2
058800 SEP-DELETE-007.                                                  NC1384.2
058900     PERFORM DE-LETE.                                             NC1384.2
059000     PERFORM SYNTAX-CHECK-WRITE.                                  NC1384.2
059100     GO TO CCVS-EXIT.                                             NC1384.2
059200 SEP-INIT-008.                                                    NC1384.2
059300     MOVE "IV-4 4.2.1(2)" TO ANSI-REFERENCE.                      NC1384.2
059400     MOVE "SEP-TEST-008" TO PAR-NAME.                             NC1384.2
059500     MOVE 0 TO REC-CT.                                            NC1384.2
059600     MOVE ZERO TO TEMP.                                           NC1384.2
059700     MOVE 3 TO S31.                                               NC1384.2
059800     MOVE 2 TO S32 S33.                                           NC1384.2
059900     MOVE 37 TO EXPECTED-VALUE.                                   NC1384.2
060000*         THIS TEST USES SPACES AND COMMAS AND SEMI-COLONS        NC1384.2
060100*         AS SEPARATORS IN                                        NC1384.2
060200*         REFERENCING THREE DIMENSIONAL TABLE ELEMENTS.           NC1384.2
060300 SEP-TEST-008-01.                                                 NC1384.2
060400     MOVE ELEM3(S31 S32; S33) TO TEMP.                            NC1384.2
060500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
060600 SEP-TEST-008-02.                                                 NC1384.2
060700     MOVE ELEM3(S31, S32; S33)   TO TEMP.                         NC1384.2
060800     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
060900 SEP-TEST-008-03.                                                 NC1384.2
061000     ADD ELEM3  (  S31;  S32  S33  )   TO  TEMP.                  NC1384.2
061100     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
061200 SEP-TEST-008-04.                                                 NC1384.2
061300     MOVE 74 TO TEMP.                                             NC1384.2
061400     SUBTRACT ELEM3(S31; S32 , S33) FROM TEMP.                    NC1384.2
061500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
061600 SEP-TEST-008-05.                                                 NC1384.2
061700     MOVE 37 TO TEMP.                                             NC1384.2
061800     IF ELEM3 ( S31; S32; S33 )                                   NC1384.2
061900              NOT EQUAL TO TEMP                                   NC1384.2
062000              MOVE ZERO TO TEMP.                                  NC1384.2
062100     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
062200 SEP-TEST-008-06.                                                 NC1384.2
062300     MULTIPLY ELEM3 (3; 2, 2) BY 1                                NC1384.2
062400              GIVING TEMP.                                        NC1384.2
062500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
062600 SEP-TEST-008-07.                                                 NC1384.2
062700     ADD ELEM3 (1; 1, 1)   ELEM3( 3  2; 1 )                       NC1384.2
062800              GIVING TEMP.                                        NC1384.2
062900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1384.2
063000     GO TO SEP-INIT-007.                                          NC1384.2
063100*         IF THE COMPILER REJECTS ANY OF THE ABOVE TABLE          NC1384.2
063200*    REFERENCES, DELETE THAT LINE OF CODE BY PLACING              NC1384.2
063300*    AN * IN COLUMN 7.  LEAVE THE PERFORM...THRU STATEMENT.       NC1384.2
063400*    THE ELEMENT DELETED APPEARS AS A FAILURE ON THE              NC1384.2
063500*    OUTPUT REPORT.                                               NC1384.2
063600 SEP-DELETE-008.                                                  NC1384.2
063700     PERFORM DE-LETE.                                             NC1384.2
063800     PERFORM SYNTAX-CHECK-WRITE.                                  NC1384.2
063900 SECT-TH109-0003 SECTION.                                         NC1384.2
064000 SYNTAX-CHECK.                                                    NC1384.2
064100     ADD 1 TO REC-CT.                                             NC1384.2
064200     IF TEMP EQUAL TO EXPECTED-VALUE                              NC1384.2
064300             PERFORM PASS                                         NC1384.2
064400             GO TO SYNTAX-CHECK-WRITE.                            NC1384.2
064500 SYNTAX-FAIL.                                                     NC1384.2
064600     PERFORM FAIL.                                                NC1384.2
064700     MOVE TEMP TO COMPUTED-18V0.                                  NC1384.2
064800     MOVE EXPECTED-VALUE TO CORRECT-18V0.                         NC1384.2
064900 SYNTAX-CHECK-WRITE.                                              NC1384.2
065000     PERFORM PRINT-DETAIL.                                        NC1384.2
065100     MOVE ZERO TO TEMP.                                           NC1384.2
065200 CCVS-EXIT SECTION.                                               NC1384.2
065300 CCVS-999999.                                                     NC1384.2
065400     GO TO CLOSE-FILES.                                           NC1384.2
      *END-OF,NC138A                                                                  
