      *HEADER,COBOL,NC140A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1404.2
000200 PROGRAM-ID.                                                      NC1404.2
000300     NC140A.                                                      NC1404.2
000400****************************************************************  NC1404.2
000500*                                                              *  NC1404.2
000600*    VALIDATION FOR:-                                          *  NC1404.2
000700*                                                              *  NC1404.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1404.2
000900*                                                              *  NC1404.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1404.2
001100*                                                              *  NC1404.2
001200****************************************************************  NC1404.2
001300*                                                              *  NC1404.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1404.2
001500*                                                              *  NC1404.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1404.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1404.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1404.2
001900*                                                              *  NC1404.2
002000****************************************************************  NC1404.2
002100*                                                                 NC1404.2
002200*    PROGRAM NC140A TESTS FORMAT 2 OF THE "SET" STATEMENT         NC1404.2
002300*    USING A VARIETY OF DATA-NAMES CONTAINING POSITIVE AND        NC1404.2
002400*    NEGATIVE VALUES, AS WELL AS POSITIVE AND NEGATIVE INTEGERS.  NC1404.2
002500*                                                                 NC1404.2
002600****************************************************************  NC1404.2
002700 ENVIRONMENT DIVISION.                                            NC1404.2
002800 CONFIGURATION SECTION.                                           NC1404.2
002900 SOURCE-COMPUTER.                                                 NC1404.2
003000     XXXXX082.                                                    NC1404.2
003100 OBJECT-COMPUTER.                                                 NC1404.2
003200     XXXXX083.                                                    NC1404.2
003300 INPUT-OUTPUT SECTION.                                            NC1404.2
003400 FILE-CONTROL.                                                    NC1404.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1404.2
003600     XXXXX055.                                                    NC1404.2
003700 DATA DIVISION.                                                   NC1404.2
003800 FILE SECTION.                                                    NC1404.2
003900 FD  PRINT-FILE.                                                  NC1404.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1404.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1404.2
004200 WORKING-STORAGE SECTION.                                         NC1404.2
004300 01  GRP-TABLE1.                                                  NC1404.2
004400     02  ELEM1  PIC S999    OCCURS 100 TIMES                      NC1404.2
004500              INDEXED BY INDEX1.                                  NC1404.2
004600*    TWO DIMENSIONAL TABLE.                                       NC1404.2
004700 01  GRP-TABLE2.                                                  NC1404.2
004800     02  GROUP1    OCCURS 10 TIMES                                NC1404.2
004900              INDEXED BY IN1.                                     NC1404.2
005000         03  ELEM2  PIC S9999                                     NC1404.2
005100              USAGE IS COMPUTATIONAL                              NC1404.2
005200              OCCURS 10 TIMES                                     NC1404.2
005300              INDEXED BY IN2.                                     NC1404.2
005400*    SUBSCRIPTS FOR TABLE REFERENCES.                             NC1404.2
005500 01  GRP-SUB.                                                     NC1404.2
005600     02  S1  PIC  S999  VALUE 1.                                  NC1404.2
005700     02  S21 PIC  S999  VALUE 1.                                  NC1404.2
005800     02  S22 PIC  S999  VALUE 1.                                  NC1404.2
005900*    DATA ITEMS USED IN SET STATEMENTS, FORMAT 2.                 NC1404.2
006000 77  CS-3     PICTURE S999    COMPUTATIONAL   VALUE ZERO.         NC1404.2
006100 77  CU-3     PICTURE  999    COMPUTATIONAL   VALUE ZERO.         NC1404.2
006200 77  DS-3     PICTURE S999    DISPLAY         VALUE ZERO.         NC1404.2
006300 77  DU-3     PICTURE  999    DISPLAY         VALUE ZERO.         NC1404.2
006400 77  DS-LS-3  PICTURE S999    SIGN IS LEADING SEPARATE CHARACTER  NC1404.2
006500              VALUE ZERO.                                         NC1404.2
006600 77  DS-TS-3  PICTURE S999    SIGN IS TRAILING SEPARATE           NC1404.2
006700               CHARACTER  VALUE ZERO.                             NC1404.2
006800 77  EXPECTED-VALUE  PIC S999.                                    NC1404.2
006900 77  TEMP  PICTURE S999.                                          NC1404.2
007000 77  WRK1   PIC   S999   VALUE ZERO.                              NC1404.2
007100 01  TEST-RESULTS.                                                NC1404.2
007200     02 FILLER                   PIC X      VALUE SPACE.          NC1404.2
007300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1404.2
007400     02 FILLER                   PIC X      VALUE SPACE.          NC1404.2
007500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1404.2
007600     02 FILLER                   PIC X      VALUE SPACE.          NC1404.2
007700     02  PAR-NAME.                                                NC1404.2
007800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1404.2
007900       03  PARDOT-X              PIC X      VALUE SPACE.          NC1404.2
008000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1404.2
008100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1404.2
008200     02 RE-MARK                  PIC X(61).                       NC1404.2
008300 01  TEST-COMPUTED.                                               NC1404.2
008400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1404.2
008500     02 FILLER                   PIC X(17)  VALUE                 NC1404.2
008600            "       COMPUTED=".                                   NC1404.2
008700     02 COMPUTED-X.                                               NC1404.2
008800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1404.2
008900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1404.2
009000                                 PIC -9(9).9(9).                  NC1404.2
009100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1404.2
009200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1404.2
009300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1404.2
009400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1404.2
009500         04 COMPUTED-18V0                    PIC -9(18).          NC1404.2
009600         04 FILLER                           PIC X.               NC1404.2
009700     03 FILLER PIC X(50) VALUE SPACE.                             NC1404.2
009800 01  TEST-CORRECT.                                                NC1404.2
009900     02 FILLER PIC X(30) VALUE SPACE.                             NC1404.2
010000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1404.2
010100     02 CORRECT-X.                                                NC1404.2
010200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1404.2
010300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1404.2
010400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1404.2
010500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1404.2
010600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1404.2
010700     03      CR-18V0 REDEFINES CORRECT-A.                         NC1404.2
010800         04 CORRECT-18V0                     PIC -9(18).          NC1404.2
010900         04 FILLER                           PIC X.               NC1404.2
011000     03 FILLER PIC X(2) VALUE SPACE.                              NC1404.2
011100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1404.2
011200 01  CCVS-C-1.                                                    NC1404.2
011300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1404.2
011400-    "SS  PARAGRAPH-NAME                                          NC1404.2
011500-    "       REMARKS".                                            NC1404.2
011600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1404.2
011700 01  CCVS-C-2.                                                    NC1404.2
011800     02 FILLER                     PIC X        VALUE SPACE.      NC1404.2
011900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1404.2
012000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1404.2
012100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1404.2
012200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1404.2
012300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1404.2
012400 01  REC-CT                        PIC 99       VALUE ZERO.       NC1404.2
012500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1404.2
012600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1404.2
012700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1404.2
012800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1404.2
012900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1404.2
013000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1404.2
013100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1404.2
013200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1404.2
013300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1404.2
013400 01  CCVS-H-1.                                                    NC1404.2
013500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1404.2
013600     02  FILLER                    PIC X(42)    VALUE             NC1404.2
013700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1404.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1404.2
013900 01  CCVS-H-2A.                                                   NC1404.2
014000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1404.2
014100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1404.2
014200   02  FILLER                        PIC XXXX   VALUE             NC1404.2
014300     "4.2 ".                                                      NC1404.2
014400   02  FILLER                        PIC X(28)  VALUE             NC1404.2
014500            " COPY - NOT FOR DISTRIBUTION".                       NC1404.2
014600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1404.2
014700                                                                  NC1404.2
014800 01  CCVS-H-2B.                                                   NC1404.2
014900   02  FILLER                        PIC X(15)  VALUE             NC1404.2
015000            "TEST RESULT OF ".                                    NC1404.2
015100   02  TEST-ID                       PIC X(9).                    NC1404.2
015200   02  FILLER                        PIC X(4)   VALUE             NC1404.2
015300            " IN ".                                               NC1404.2
015400   02  FILLER                        PIC X(12)  VALUE             NC1404.2
015500     " HIGH       ".                                              NC1404.2
015600   02  FILLER                        PIC X(22)  VALUE             NC1404.2
015700            " LEVEL VALIDATION FOR ".                             NC1404.2
015800   02  FILLER                        PIC X(58)  VALUE             NC1404.2
015900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1404.2
016000 01  CCVS-H-3.                                                    NC1404.2
016100     02  FILLER                      PIC X(34)  VALUE             NC1404.2
016200            " FOR OFFICIAL USE ONLY    ".                         NC1404.2
016300     02  FILLER                      PIC X(58)  VALUE             NC1404.2
016400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1404.2
016500     02  FILLER                      PIC X(28)  VALUE             NC1404.2
016600            "  COPYRIGHT   1985 ".                                NC1404.2
016700 01  CCVS-E-1.                                                    NC1404.2
016800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1404.2
016900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1404.2
017000     02 ID-AGAIN                     PIC X(9).                    NC1404.2
017100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1404.2
017200 01  CCVS-E-2.                                                    NC1404.2
017300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1404.2
017400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1404.2
017500     02 CCVS-E-2-2.                                               NC1404.2
017600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1404.2
017700         03 FILLER                   PIC X      VALUE SPACE.      NC1404.2
017800         03 ENDER-DESC               PIC X(44)  VALUE             NC1404.2
017900            "ERRORS ENCOUNTERED".                                 NC1404.2
018000 01  CCVS-E-3.                                                    NC1404.2
018100     02  FILLER                      PIC X(22)  VALUE             NC1404.2
018200            " FOR OFFICIAL USE ONLY".                             NC1404.2
018300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1404.2
018400     02  FILLER                      PIC X(58)  VALUE             NC1404.2
018500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1404.2
018600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1404.2
018700     02 FILLER                       PIC X(15)  VALUE             NC1404.2
018800             " COPYRIGHT 1985".                                   NC1404.2
018900 01  CCVS-E-4.                                                    NC1404.2
019000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1404.2
019100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1404.2
019200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1404.2
019300     02 FILLER                       PIC X(40)  VALUE             NC1404.2
019400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1404.2
019500 01  XXINFO.                                                      NC1404.2
019600     02 FILLER                       PIC X(19)  VALUE             NC1404.2
019700            "*** INFORMATION ***".                                NC1404.2
019800     02 INFO-TEXT.                                                NC1404.2
019900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1404.2
020000       04 XXCOMPUTED                 PIC X(20).                   NC1404.2
020100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1404.2
020200       04 XXCORRECT                  PIC X(20).                   NC1404.2
020300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1404.2
020400 01  HYPHEN-LINE.                                                 NC1404.2
020500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1404.2
020600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1404.2
020700-    "*****************************************".                 NC1404.2
020800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1404.2
020900-    "******************************".                            NC1404.2
021000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1404.2
021100     "NC140A".                                                    NC1404.2
021200 PROCEDURE DIVISION.                                              NC1404.2
021300 CCVS1 SECTION.                                                   NC1404.2
021400 OPEN-FILES.                                                      NC1404.2
021500     OPEN     OUTPUT PRINT-FILE.                                  NC1404.2
021600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1404.2
021700     MOVE    SPACE TO TEST-RESULTS.                               NC1404.2
021800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1404.2
021900     GO TO CCVS1-EXIT.                                            NC1404.2
022000 CLOSE-FILES.                                                     NC1404.2
022100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1404.2
022200 TERMINATE-CCVS.                                                  NC1404.2
022300*    EXIT PROGRAM.                                                NC1404.2
022400*TERMINATE-CALL.                                                  NC1404.2
022500     STOP     RUN.                                                NC1404.2
022600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1404.2
022700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1404.2
022800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1404.2
022900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1404.2
023000     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1404.2
023100 PRINT-DETAIL.                                                    NC1404.2
023200     IF REC-CT NOT EQUAL TO ZERO                                  NC1404.2
023300             MOVE "." TO PARDOT-X                                 NC1404.2
023400             MOVE REC-CT TO DOTVALUE.                             NC1404.2
023500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1404.2
023600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1404.2
023700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1404.2
023800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1404.2
023900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1404.2
024000     MOVE SPACE TO CORRECT-X.                                     NC1404.2
024100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1404.2
024200     MOVE     SPACE TO RE-MARK.                                   NC1404.2
024300 HEAD-ROUTINE.                                                    NC1404.2
024400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1404.2
024500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1404.2
024600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1404.2
024700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1404.2
024800 COLUMN-NAMES-ROUTINE.                                            NC1404.2
024900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1404.2
025000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1404.2
025100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1404.2
025200 END-ROUTINE.                                                     NC1404.2
025300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1404.2
025400 END-RTN-EXIT.                                                    NC1404.2
025500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1404.2
025600 END-ROUTINE-1.                                                   NC1404.2
025700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1404.2
025800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1404.2
025900      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1404.2
026000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1404.2
026100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1404.2
026200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1404.2
026300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1404.2
026400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1404.2
026500  END-ROUTINE-12.                                                 NC1404.2
026600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1404.2
026700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1404.2
026800         MOVE "NO " TO ERROR-TOTAL                                NC1404.2
026900         ELSE                                                     NC1404.2
027000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1404.2
027100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1404.2
027200     PERFORM WRITE-LINE.                                          NC1404.2
027300 END-ROUTINE-13.                                                  NC1404.2
027400     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1404.2
027500         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1404.2
027600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1404.2
027700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1404.2
027800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1404.2
027900      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1404.2
028000          MOVE "NO " TO ERROR-TOTAL                               NC1404.2
028100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1404.2
028200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1404.2
028300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1404.2
028400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1404.2
028500 WRITE-LINE.                                                      NC1404.2
028600     ADD 1 TO RECORD-COUNT.                                       NC1404.2
028700*    IF RECORD-COUNT GREATER 42                                   NC1404.2
028800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1404.2
028900*        MOVE SPACE TO DUMMY-RECORD                               NC1404.2
029000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1404.2
029100*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1404.2
029200*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1404.2
029300*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1404.2
029400*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1404.2
029500*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1404.2
029600*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1404.2
029700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1404.2
029800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1404.2
029900*        MOVE ZERO TO RECORD-COUNT.                               NC1404.2
030000     PERFORM WRT-LN.                                              NC1404.2
030100 WRT-LN.                                                          NC1404.2
030200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1404.2
030300     MOVE SPACE TO DUMMY-RECORD.                                  NC1404.2
030400 BLANK-LINE-PRINT.                                                NC1404.2
030500     PERFORM WRT-LN.                                              NC1404.2
030600 FAIL-ROUTINE.                                                    NC1404.2
030700     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1404.2
030800            GO TO FAIL-ROUTINE-WRITE.                             NC1404.2
030900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1404.2
031000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1404.2
031100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1404.2
031200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1404.2
031300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1404.2
031400     GO TO  FAIL-ROUTINE-EX.                                      NC1404.2
031500 FAIL-ROUTINE-WRITE.                                              NC1404.2
031600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1404.2
031700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1404.2
031800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1404.2
031900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1404.2
032000 FAIL-ROUTINE-EX. EXIT.                                           NC1404.2
032100 BAIL-OUT.                                                        NC1404.2
032200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1404.2
032300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1404.2
032400 BAIL-OUT-WRITE.                                                  NC1404.2
032500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1404.2
032600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1404.2
032700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1404.2
032800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1404.2
032900 BAIL-OUT-EX. EXIT.                                               NC1404.2
033000 CCVS1-EXIT.                                                      NC1404.2
033100     EXIT.                                                        NC1404.2
033200**********************************                                NC1404.2
033300*STATEMENT DELETION INSTRUCTIONS                                  NC1404.2
033400*       IF THE COMPILER REJECTS ANY SET STATEMENTS IN THESE TESTS,NC1404.2
033500*    DELETE THAT LINE OF CODE BY PLACING AN * IN COLUMN 7.  LEAVE NC1404.2
033600*    THE PERFORM ... THRU STATEMENT.  THE TEST DELETED APPEARS AS NC1404.2
033700*    A FAILURE ON THE OUTPUT REPORT.                              NC1404.2
033800**********************************                                NC1404.2
033900 SECT-NC140A-0001 SECTION.                                        NC1404.2
034000 NC140A-0001.                                                     NC1404.2
034100     MOVE "VI-127 6.22.4" TO ANSI-REFERENCE.                      NC1404.2
034200*    THIS SECTION STORES VALUES IN THE TWO TABLES                 NC1404.2
034300*    USED IN TESTING THE INDEX VALUES.                            NC1404.2
034400 MOVE-VALUE.                                                      NC1404.2
034500     ADD 1 TO WRK1.                                               NC1404.2
034600     MOVE WRK1 TO ELEM1 (S1) ELEM2 (S21 S22).                     NC1404.2
034700     IF WRK1 IS EQUAL TO 100                                      NC1404.2
034800              GO TO SECT-TH111-0002.                              NC1404.2
034900 INCRE-SUBS.                                                      NC1404.2
035000     ADD 1 TO S1.                                                 NC1404.2
035100     ADD 1 TO S22.                                                NC1404.2
035200     IF S22 LESS THAN 11                                          NC1404.2
035300              GO TO MOVE-VALUE.                                   NC1404.2
035400     MOVE 1 TO S22.                                               NC1404.2
035500     ADD 1 TO S21.                                                NC1404.2
035600     GO TO MOVE-VALUE.                                            NC1404.2
035700 SECT-TH111-0002 SECTION.                                         NC1404.2
035800 SET-INIT-001.                                                    NC1404.2
035900     MOVE ZERO TO REC-CT.                                         NC1404.2
036000*        THIS TEST VERIFIES THAT THE SET INDEX-NAME UP BY INTEGER NC1404.2
036100*    FUNCTIONS CORRECTLY. THE INTEGER MAY BE POSITIVE, NEGATIVE,  NC1404.2
036200*    OR UNSIGNED.                                                 NC1404.2
036300     MOVE ZERO TO TEMP.                                           NC1404.2
036400     MOVE 6 TO EXPECTED-VALUE.                                    NC1404.2
036500     MOVE "SET IN UP BY INTEGER" TO FEATURE.                      NC1404.2
036600     MOVE "SET-TEST-001" TO PAR-NAME.                             NC1404.2
036700 SET-TEST-001-01.                                                 NC1404.2
036800     SET INDEX1 TO 1.                                             NC1404.2
036900     SET INDEX1 UP BY 5.                                          NC1404.2
037000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
037100 SET-TEST-001-02.                                                 NC1404.2
037200     SET INDEX1 TO 1.                                             NC1404.2
037300     SET INDEX1 UP BY +5.                                         NC1404.2
037400     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
037500 SET-TEST-001-03.                                                 NC1404.2
037600     SET INDEX1 TO 11.                                            NC1404.2
037700     SET INDEX1 UP BY -5.                                         NC1404.2
037800     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
037900 SET-TEST-001-04.                                                 NC1404.2
038000     SET IN1 TO 1.                                                NC1404.2
038100     SET IN2 INDEX1 TO 2.                                         NC1404.2
038200     SET IN2 INDEX1 UP BY 4.                                      NC1404.2
038300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
038400 SET-TEST-001-05.                                                 NC1404.2
038500     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
038600 SET-TEST-001-06.                                                 NC1404.2
038700     SET IN2 INDEX1 TO 2.                                         NC1404.2
038800     SET IN2 INDEX1 UP BY +4.                                     NC1404.2
038900     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
039000 SET-TEST-001-07.                                                 NC1404.2
039100     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
039200 SET-TEST-001-08.                                                 NC1404.2
039300     SET IN2 INDEX1 TO 10.                                        NC1404.2
039400     SET IN2 INDEX1 UP BY -4.                                     NC1404.2
039500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
039600 SET-TEST-001-09.                                                 NC1404.2
039700     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
039800 SET-TEST-001-10.                                                 NC1404.2
039900     SET IN1 TO 5.                                                NC1404.2
040000     SET IN2 TO 10.                                               NC1404.2
040100     SET IN1 IN2 UP BY -4.                                        NC1404.2
040200     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
040300 SET-TEST-001-11.                                                 NC1404.2
040400     SET IN2 INDEX1 TO 10.                                        NC1404.2
040500     SET IN1 TO 5.                                                NC1404.2
040600     SET IN1 IN2 INDEX1 UP BY -4.                                 NC1404.2
040700     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
040800 SET-TEST-001-12.                                                 NC1404.2
040900     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
041000     GO TO SET-INIT-002.                                          NC1404.2
041100 SET-DELETE-001.                                                  NC1404.2
041200     PERFORM DE-LETE.                                             NC1404.2
041300     PERFORM TEST-WRITE.                                          NC1404.2
041400 SET-INIT-002.                                                    NC1404.2
041500     MOVE ZERO TO REC-CT.                                         NC1404.2
041600     MOVE "SET-TEST-002" TO PAR-NAME.                             NC1404.2
041700     MOVE ZERO TO TEMP.                                           NC1404.2
041800     MOVE 54 TO EXPECTED-VALUE.                                   NC1404.2
041900     MOVE "SET IN DOWN BY INTEG" TO FEATURE.                      NC1404.2
042000*       THIS TEST VERIFIES THAT THE SET INDEX-NAME DOWN BY        NC1404.2
042100*    INTEGER FUNCTIONS CORRECTLY.  THE INTEGER MAY BE POSITIVE,   NC1404.2
042200*    NEGATIVE, OR UNSIGNED.                                       NC1404.2
042300 SET-TEST-002-01.                                                 NC1404.2
042400     SET INDEX1 TO 95.                                            NC1404.2
042500     SET INDEX1 DOWN BY 41.                                       NC1404.2
042600     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
042700 SET-TEST-002-02.                                                 NC1404.2
042800     SET INDEX1 TO 95.                                            NC1404.2
042900     SET INDEX1 DOWN BY +41.                                      NC1404.2
043000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
043100 SET-TEST-002-03.                                                 NC1404.2
043200     SET INDEX1 TO 21.                                            NC1404.2
043300     SET INDEX1 DOWN BY -33.                                      NC1404.2
043400     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
043500 SET-TEST-002-04.                                                 NC1404.2
043600     SET IN1 TO 9.                                                NC1404.2
043700     SET IN2 TO 4.                                                NC1404.2
043800     SET INDEX1 TO 57.                                            NC1404.2
043900     SET IN1 INDEX1 DOWN BY 3.                                    NC1404.2
044000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
044100 SET-TEST-002-05.                                                 NC1404.2
044200     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
044300 SET-TEST-002-06.                                                 NC1404.2
044400     SET IN1 TO 9.                                                NC1404.2
044500     SET INDEX1 TO 57.                                            NC1404.2
044600     SET IN1 INDEX1 DOWN BY +3.                                   NC1404.2
044700     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
044800 SET-TEST-002-07.                                                 NC1404.2
044900     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
045000 SET-TEST-002-08.                                                 NC1404.2
045100     SET IN1 TO 3.                                                NC1404.2
045200     SET INDEX1 TO 51.                                            NC1404.2
045300     SET INDEX1 IN1 DOWN BY -3.                                   NC1404.2
045400     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
045500 SET-TEST-002-09.                                                 NC1404.2
045600     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
045700 SET-TEST-002-10.                                                 NC1404.2
045800     SET IN1 TO 4.                                                NC1404.2
045900     SET IN2 TO 2.                                                NC1404.2
046000     SET IN1 IN2 DOWN BY -2.                                      NC1404.2
046100     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
046200 SET-TEST-002-11.                                                 NC1404.2
046300     SET IN1 TO 5.                                                NC1404.2
046400     SET IN2 TO 3.                                                NC1404.2
046500     SET INDEX1 TO 53.                                            NC1404.2
046600     SET IN1 IN2 INDEX1 DOWN BY -1.                               NC1404.2
046700     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
046800 SET-TEST-002-12.                                                 NC1404.2
046900     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
047000     GO TO SET-INIT-003.                                          NC1404.2
047100 SET-DELETE-002.                                                  NC1404.2
047200     PERFORM DE-LETE.                                             NC1404.2
047300     PERFORM TEST-WRITE.                                          NC1404.2
047400 SET-INIT-003.                                                    NC1404.2
047500     MOVE ZERO TO REC-CT.                                         NC1404.2
047600     MOVE "SET-TEST-003" TO PAR-NAME.                             NC1404.2
047700     MOVE ZERO TO TEMP.                                           NC1404.2
047800     MOVE 39 TO EXPECTED-VALUE.                                   NC1404.2
047900     MOVE "SET IN UP BY DATA-NM" TO FEATURE.                      NC1404.2
048000*        THIS TEST VERIFIES THAT THE SET INDEX-NAME UP BY         NC1404.2
048100*    DATA-NAME FUNCTIONS CORRECTLY.  THE VALUE STORED IN DATA-NAMENC1404.2
048200*    MAY BE POSITIVE, NEGATIVE OR ZERO.  VARIOUS DATA TYPES ARE   NC1404.2
048300*    USED IN THE TEST.                                            NC1404.2
048400 SET-TEST-003-01.                                                 NC1404.2
048500     SET INDEX1 TO 21.                                            NC1404.2
048600     MOVE +18 TO CS-3.                                            NC1404.2
048700     SET INDEX1 UP BY CS-3.                                       NC1404.2
048800     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
048900 SET-TEST-003-02.                                                 NC1404.2
049000     SET INDEX1 TO 21.                                            NC1404.2
049100     MOVE +18 TO DS-3.                                            NC1404.2
049200     SET INDEX1 UP BY DS-3.                                       NC1404.2
049300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
049400 SET-TEST-003-03.                                                 NC1404.2
049500     SET INDEX1 TO 21.                                            NC1404.2
049600     MOVE +18 TO DS-LS-3.                                         NC1404.2
049700     SET INDEX1  UP BY DS-LS-3.                                   NC1404.2
049800     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
049900 SET-TEST-003-04.                                                 NC1404.2
050000     SET INDEX1 TO 21.                                            NC1404.2
050100     MOVE +18 TO DS-TS-3.                                         NC1404.2
050200     SET INDEX1 UP BY DS-TS-3.                                    NC1404.2
050300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
050400 SET-TEST-003-05.                                                 NC1404.2
050500     SET INDEX1 TO 21.                                            NC1404.2
050600     MOVE 18 TO CU-3.                                             NC1404.2
050700     SET INDEX1 UP BY CU-3.                                       NC1404.2
050800     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
050900 SET-TEST-003-06.                                                 NC1404.2
051000     SET INDEX1 TO 21.                                            NC1404.2
051100     MOVE 18 TO DU-3.                                             NC1404.2
051200     SET INDEX1 UP BY DU-3.                                       NC1404.2
051300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
051400 SET-TEST-003-07.                                                 NC1404.2
051500     SET INDEX1 TO 39.                                            NC1404.2
051600     MOVE 0 TO CS-3.                                              NC1404.2
051700     SET INDEX1 UP BY CS-3.                                       NC1404.2
051800     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
051900 SET-TEST-003-08.                                                 NC1404.2
052000     SET INDEX1 TO 39.                                            NC1404.2
052100     MOVE ZERO TO DS-3.                                           NC1404.2
052200     SET INDEX1 UP BY DS-3.                                       NC1404.2
052300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
052400 SET-TEST-003-09.                                                 NC1404.2
052500     SET INDEX1 TO 39.                                            NC1404.2
052600     MOVE 0 TO DS-LS-3.                                           NC1404.2
052700     SET INDEX1 UP BY DS-LS-3.                                    NC1404.2
052800     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
052900 SET-TEST-003-10.                                                 NC1404.2
053000     SET INDEX1 TO 39.                                            NC1404.2
053100     MOVE ZERO TO DS-TS-3.                                        NC1404.2
053200     SET INDEX1 UP BY DS-TS-3.                                    NC1404.2
053300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
053400 SET-TEST-003-11.                                                 NC1404.2
053500     SET INDEX1 TO 39.                                            NC1404.2
053600     MOVE 0 TO CU-3.                                              NC1404.2
053700     SET INDEX1 UP BY CU-3.                                       NC1404.2
053800     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
053900 SET-TEST-003-12.                                                 NC1404.2
054000     SET INDEX1 TO 39.                                            NC1404.2
054100     MOVE ZERO TO DU-3.                                           NC1404.2
054200     SET INDEX1 UP BY DU-3.                                       NC1404.2
054300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
054400 SET-TEST-003-13.                                                 NC1404.2
054500     SET INDEX1 TO 70.                                            NC1404.2
054600     MOVE -31 TO CS-3.                                            NC1404.2
054700     SET INDEX1 UP BY CS-3.                                       NC1404.2
054800     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
054900 SET-TEST-003-14.                                                 NC1404.2
055000     SET INDEX1 TO 70.                                            NC1404.2
055100     MOVE -31 TO DS-3.                                            NC1404.2
055200     SET INDEX1 UP BY DS-3.                                       NC1404.2
055300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
055400 SET-TEST-003-15.                                                 NC1404.2
055500     SET INDEX1 TO 70.                                            NC1404.2
055600     MOVE -31 TO DS-LS-3.                                         NC1404.2
055700     SET INDEX1 UP BY DS-LS-3.                                    NC1404.2
055800     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
055900 SET-TEST-003-16.                                                 NC1404.2
056000     SET INDEX1 TO 70.                                            NC1404.2
056100     MOVE -31 TO DS-TS-3.                                         NC1404.2
056200     SET INDEX1 UP BY DS-TS-3.                                    NC1404.2
056300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
056400 SET-TEST-003-17.                                                 NC1404.2
056500     SET IN1 TO 1.                                                NC1404.2
056600     SET IN2 TO 6.                                                NC1404.2
056700     MOVE +3 TO DS-LS-3.                                          NC1404.2
056800     SET IN1 IN2 UP BY DS-LS-3.                                   NC1404.2
056900     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
057000 SET-TEST-003-18.                                                 NC1404.2
057100     SET IN1 TO 1.                                                NC1404.2
057200     SET IN2 TO 6.                                                NC1404.2
057300     MOVE +3 TO CS-3.                                             NC1404.2
057400     SET INDEX1 TO 36.                                            NC1404.2
057500     SET IN1 IN2 INDEX1 UP BY CS-3.                               NC1404.2
057600     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
057700 SET-TEST-003-19.                                                 NC1404.2
057800     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
057900 SET-TEST-003-20.                                                 NC1404.2
058000     SET IN1 TO 5.                                                NC1404.2
058100     SET IN2 TO 10.                                               NC1404.2
058200     SET INDEX1 TO 40.                                            NC1404.2
058300     MOVE -1 TO DS-TS-3.                                          NC1404.2
058400     SET IN1 IN2 INDEX1 UP BY DS-TS-3.                            NC1404.2
058500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
058600 SET-TEST-003-21.                                                 NC1404.2
058700     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
058800 SET-TEST-003-22.                                                 NC1404.2
058900     SET IN1 TO 4.                                                NC1404.2
059000     SET IN2 TO 9.                                                NC1404.2
059100     SET INDEX1 TO 39.                                            NC1404.2
059200     MOVE ZERO TO CU-3.                                           NC1404.2
059300     SET IN1 IN2 INDEX1 UP BY CU-3.                               NC1404.2
059400     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
059500 SET-TEST-003-23.                                                 NC1404.2
059600     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
059700     GO TO SET-INIT-004.                                          NC1404.2
059800 SET-DELETE-003.                                                  NC1404.2
059900     PERFORM DE-LETE.                                             NC1404.2
060000     PERFORM TEST-WRITE.                                          NC1404.2
060100 SET-INIT-004.                                                    NC1404.2
060200     MOVE ZERO TO REC-CT.                                         NC1404.2
060300     MOVE "SET-TEST-004" TO PAR-NAME.                             NC1404.2
060400     MOVE ZERO TO TEMP.                                           NC1404.2
060500     MOVE 77 TO EXPECTED-VALUE.                                   NC1404.2
060600     MOVE "SET IN DOWN BY DNAME" TO FEATURE.                      NC1404.2
060700*        THIS TEST VERIFIES THAT THE SET INDEX-NAME DOWN BY       NC1404.2
060800*    DATA-NAME FUNCTIONS CORRECTLY.  THE VALUE STORED IN DATA-NAMENC1404.2
060900*    MAY BE POSITIVE, NEGATIVE OR ZERO.  VARIOUS DATA TYPES ARE   NC1404.2
061000*    USED IN THE TEST.                                            NC1404.2
061100 SET-TEST-004-01.                                                 NC1404.2
061200     SET INDEX1 TO 100.                                           NC1404.2
061300     MOVE +23 TO CS-3.                                            NC1404.2
061400     SET INDEX1 DOWN BY CS-3.                                     NC1404.2
061500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
061600 SET-TEST-004-02.                                                 NC1404.2
061700     SET INDEX1 TO 100.                                           NC1404.2
061800     MOVE +23 TO DS-3.                                            NC1404.2
061900     SET INDEX1 DOWN BY DS-3.                                     NC1404.2
062000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
062100 SET-TEST-004-03.                                                 NC1404.2
062200     SET INDEX1 TO 100.                                           NC1404.2
062300     MOVE +23 TO DS-LS-3.                                         NC1404.2
062400     SET INDEX1 DOWN BY DS-LS-3.                                  NC1404.2
062500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
062600 SET-TEST-004-04.                                                 NC1404.2
062700     SET INDEX1 TO 100.                                           NC1404.2
062800     MOVE +23 TO DS-TS-3.                                         NC1404.2
062900     SET INDEX1 DOWN BY DS-TS-3.                                  NC1404.2
063000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
063100 SET-TEST-004-05.                                                 NC1404.2
063200     SET INDEX1 TO 100.                                           NC1404.2
063300     MOVE 23 TO CU-3.                                             NC1404.2
063400     SET INDEX1 DOWN BY CU-3.                                     NC1404.2
063500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
063600 SET-TEST-004-06.                                                 NC1404.2
063700     SET INDEX1 TO 100.                                           NC1404.2
063800     MOVE 23 TO DU-3.                                             NC1404.2
063900     SET INDEX1 DOWN BY DU-3.                                     NC1404.2
064000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
064100 SET-TEST-004-07.                                                 NC1404.2
064200     MOVE ZERO TO CS-3.                                           NC1404.2
064300     SET INDEX1 TO 77.                                            NC1404.2
064400     SET INDEX1 DOWN BY CS-3.                                     NC1404.2
064500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
064600 SET-TEST-004-08.                                                 NC1404.2
064700     MOVE 0 TO DS-3.                                              NC1404.2
064800     SET INDEX1 TO 77.                                            NC1404.2
064900     SET INDEX1 DOWN BY DS-3.                                     NC1404.2
065000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
065100 SET-TEST-004-09.                                                 NC1404.2
065200     MOVE 0 TO DS-LS-3.                                           NC1404.2
065300     SET INDEX1 TO 77.                                            NC1404.2
065400     SET INDEX1 DOWN BY DS-LS-3.                                  NC1404.2
065500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
065600 SET-TEST-004-10.                                                 NC1404.2
065700     MOVE ZERO TO DS-TS-3.                                        NC1404.2
065800     SET INDEX1 TO 77.                                            NC1404.2
065900     SET INDEX1 DOWN BY DS-TS-3.                                  NC1404.2
066000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
066100 SET-TEST-004-11.                                                 NC1404.2
066200     MOVE 0 TO CU-3.                                              NC1404.2
066300     SET INDEX1 TO 77.                                            NC1404.2
066400     SET INDEX1 DOWN BY CU-3.                                     NC1404.2
066500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
066600 SET-TEST-004-12.                                                 NC1404.2
066700     MOVE ZERO TO DU-3.                                           NC1404.2
066800     SET INDEX1 TO 77.                                            NC1404.2
066900     SET INDEX1 DOWN BY DU-3.                                     NC1404.2
067000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
067100 SET-TEST-004-13.                                                 NC1404.2
067200     SET INDEX1 TO 2.                                             NC1404.2
067300     MOVE -75 TO CS-3.                                            NC1404.2
067400     SET INDEX1 DOWN BY CS-3.                                     NC1404.2
067500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
067600 SET-TEST-004-14.                                                 NC1404.2
067700     SET INDEX1 TO 2.                                             NC1404.2
067800     MOVE -75 TO DS-3.                                            NC1404.2
067900     SET INDEX1 DOWN BY DS-3.                                     NC1404.2
068000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
068100 SET-TEST-004-15.                                                 NC1404.2
068200     SET INDEX1 TO 2.                                             NC1404.2
068300     MOVE -75 TO DS-LS-3.                                         NC1404.2
068400     SET INDEX1 DOWN BY DS-LS-3.                                  NC1404.2
068500     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
068600 SET-TEST-004-16.                                                 NC1404.2
068700     SET INDEX1 TO 2.                                             NC1404.2
068800     MOVE -75 TO DS-TS-3.                                         NC1404.2
068900     SET INDEX1 DOWN BY DS-TS-3.                                  NC1404.2
069000     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
069100 SET-TEST-004-17.                                                 NC1404.2
069200     SET IN1 TO 10.                                               NC1404.2
069300     SET IN2 TO 9.                                                NC1404.2
069400     MOVE +2 TO DS-TS-3.                                          NC1404.2
069500     SET IN1 IN2 DOWN BY DS-TS-3.                                 NC1404.2
069600     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
069700 SET-TEST-004-18.                                                 NC1404.2
069800     SET IN1 TO 10.                                               NC1404.2
069900     SET IN2 TO 9.                                                NC1404.2
070000     SET INDEX1 TO 79.                                            NC1404.2
070100     MOVE 2 TO CU-3.                                              NC1404.2
070200     SET IN1 IN2 INDEX1 DOWN BY CU-3.                             NC1404.2
070300     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
070400 SET-TEST-004-19.                                                 NC1404.2
070500     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
070600 SET-TEST-004-20.                                                 NC1404.2
070700     SET IN1 TO 3.                                                NC1404.2
070800     SET IN2 TO 2.                                                NC1404.2
070900     SET INDEX1 TO 72.                                            NC1404.2
071000     MOVE -5 TO DS-3.                                             NC1404.2
071100     SET INDEX1 IN1 IN2 DOWN BY DS-3.                             NC1404.2
071200     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
071300 SET-TEST-004-21.                                                 NC1404.2
071400     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
071500 SET-TEST-004-22.                                                 NC1404.2
071600     MOVE ZERO TO DS-TS-3.                                        NC1404.2
071700     SET IN1 TO 8.                                                NC1404.2
071800     SET IN2 TO 7.                                                NC1404.2
071900     SET INDEX1 TO 77.                                            NC1404.2
072000     SET IN1 IN2 INDEX1 DOWN BY DS-TS-3.                          NC1404.2
072100     PERFORM TEST-CHECK1 THRU TEST-WRITE.                         NC1404.2
072200 SET-TEST-004-23.                                                 NC1404.2
072300     PERFORM TEST-CHECK2 THRU TEST-WRITE.                         NC1404.2
072400     GO TO CCVS-EXIT.                                             NC1404.2
072500 SET-DELETE-004.                                                  NC1404.2
072600     PERFORM DE-LETE.                                             NC1404.2
072700     PERFORM TEST-WRITE.                                          NC1404.2
072800     GO TO CCVS-EXIT.                                             NC1404.2
072900 SECT-TH111-0003 SECTION.                                         NC1404.2
073000 TEST-CHECK1.                                                     NC1404.2
073100     MOVE ELEM1 (INDEX1) TO TEMP.                                 NC1404.2
073200     GO TO TEST-CHECK.                                            NC1404.2
073300 TEST-CHECK2.                                                     NC1404.2
073400     MOVE ELEM2 (IN1 IN2) TO TEMP.                                NC1404.2
073500 TEST-CHECK.                                                      NC1404.2
073600     ADD 1 TO REC-CT.                                             NC1404.2
073700     IF TEMP IS EQUAL TO EXPECTED-VALUE                           NC1404.2
073800          PERFORM PASS                                            NC1404.2
073900          GO TO TEST-WRITE.                                       NC1404.2
074000 TEST-FAIL.                                                       NC1404.2
074100     PERFORM FAIL.                                                NC1404.2
074200     MOVE TEMP TO COMPUTED-18V0.                                  NC1404.2
074300     MOVE EXPECTED-VALUE TO CORRECT-18V0.                         NC1404.2
074400 TEST-WRITE.                                                      NC1404.2
074500     PERFORM PRINT-DETAIL.                                        NC1404.2
074600     MOVE ZERO TO TEMP.                                           NC1404.2
074700 CCVS-EXIT SECTION.                                               NC1404.2
074800 CCVS-999999.                                                     NC1404.2
074900     GO TO CLOSE-FILES.                                           NC1404.2
      *END-OF,NC140A                                                                  
