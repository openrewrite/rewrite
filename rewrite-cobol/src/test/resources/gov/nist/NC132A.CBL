      *HEADER,COBOL,NC132A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1324.2
000200 PROGRAM-ID.                                                      NC1324.2
000300     NC132A.                                                      NC1324.2
000400****************************************************************  NC1324.2
000500*                                                              *  NC1324.2
000600*    VALIDATION FOR:-                                          *  NC1324.2
000700*                                                              *  NC1324.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1324.2
000900*                                                              *  NC1324.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1324.2
001100*                                                              *  NC1324.2
001200****************************************************************  NC1324.2
001300*                                                              *  NC1324.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1324.2
001500*                                                              *  NC1324.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1324.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1324.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1324.2
001900*                                                              *  NC1324.2
002000****************************************************************  NC1324.2
002100*                                                                 NC1324.2
002200*    PROGRAM NC132A TESTS THE USE OF SUBSCRIPTS TO ACCESS A       NC1324.2
002300*    SINGLE LEVEL TABLE USING INTEGER DISPLAY AND COMPUTATIONAL   NC1324.2
002400*    FIELDS AS SUBSCRIPTS.                                        NC1324.2
002500*                                                                 NC1324.2
002600 ENVIRONMENT DIVISION.                                            NC1324.2
002700 CONFIGURATION SECTION.                                           NC1324.2
002800 SOURCE-COMPUTER.                                                 NC1324.2
002900     XXXXX082.                                                    NC1324.2
003000 OBJECT-COMPUTER.                                                 NC1324.2
003100     XXXXX083.                                                    NC1324.2
003200 INPUT-OUTPUT SECTION.                                            NC1324.2
003300 FILE-CONTROL.                                                    NC1324.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1324.2
003500     XXXXX055.                                                    NC1324.2
003600 DATA DIVISION.                                                   NC1324.2
003700 FILE SECTION.                                                    NC1324.2
003800 FD  PRINT-FILE.                                                  NC1324.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1324.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1324.2
004100 WORKING-STORAGE SECTION.                                         NC1324.2
004200 77  CON-1             PICTURE 9  VALUE 1.                        NC1324.2
004300 77  CON-2             PICTURE 9  VALUE 2.                        NC1324.2
004400 77  CON-3             PICTURE 9  VALUE 3.                        NC1324.2
004500 77  CON-4             PICTURE 9  VALUE 4.                        NC1324.2
004600 77  SUB-3  PICTURE S9(18) COMPUTATIONAL VALUE 2.                 NC1324.2
004700 77  SUB-4  PICTURE 9(18)  COMPUTATIONAL VALUE 4.                 NC1324.2
004800 01  CONSTANTS-77.                                                NC1324.2
004900     02  SUB1   PICTURE 9  VALUE  1.                              NC1324.2
005000     02  SUB2   PICTURE  S9  VALUE  +4.                           NC1324.2
005100     02  TABLE-A-VALUES   PICTURE X(20) VALUE "1112223334441122334NC1324.2
005200-    "4".                                                         NC1324.2
005300 01  TABLE-A.                                                     NC1324.2
005400     02  ENTRY-A-1   PICTURE XXX   OCCURS  4 TIMES.               NC1324.2
005500     02  ENTRY-A-2   OCCURS  4 TIMES.                             NC1324.2
005600       03  ENTRY-A-3    PICTURE   X.                              NC1324.2
005700       03  ENTRY-A-4    PICTURE   X.                              NC1324.2
005800 01  TABLE-B.                                                     NC1324.2
005900     02  ENTRY-B-1  PICTURE X(4)  VALUE  "1234".                  NC1324.2
006000     02  ENTRY-B-2  REDEFINES  ENTRY-B-1  PICTURE  9 OCCURS  4.   NC1324.2
006100 01  TABLE-C.                                                     NC1324.2
006200     02  ENTRY-C    PICTURE 9  OCCURS  4 TIMES.                   NC1324.2
006300 01  TEST-RESULTS.                                                NC1324.2
006400     02 FILLER                   PIC X      VALUE SPACE.          NC1324.2
006500     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1324.2
006600     02 FILLER                   PIC X      VALUE SPACE.          NC1324.2
006700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1324.2
006800     02 FILLER                   PIC X      VALUE SPACE.          NC1324.2
006900     02  PAR-NAME.                                                NC1324.2
007000       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1324.2
007100       03  PARDOT-X              PIC X      VALUE SPACE.          NC1324.2
007200       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1324.2
007300     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1324.2
007400     02 RE-MARK                  PIC X(61).                       NC1324.2
007500 01  TEST-COMPUTED.                                               NC1324.2
007600     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1324.2
007700     02 FILLER                   PIC X(17)  VALUE                 NC1324.2
007800            "       COMPUTED=".                                   NC1324.2
007900     02 COMPUTED-X.                                               NC1324.2
008000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1324.2
008100     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1324.2
008200                                 PIC -9(9).9(9).                  NC1324.2
008300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1324.2
008400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1324.2
008500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1324.2
008600     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1324.2
008700         04 COMPUTED-18V0                    PIC -9(18).          NC1324.2
008800         04 FILLER                           PIC X.               NC1324.2
008900     03 FILLER PIC X(50) VALUE SPACE.                             NC1324.2
009000 01  TEST-CORRECT.                                                NC1324.2
009100     02 FILLER PIC X(30) VALUE SPACE.                             NC1324.2
009200     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1324.2
009300     02 CORRECT-X.                                                NC1324.2
009400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1324.2
009500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1324.2
009600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1324.2
009700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1324.2
009800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1324.2
009900     03      CR-18V0 REDEFINES CORRECT-A.                         NC1324.2
010000         04 CORRECT-18V0                     PIC -9(18).          NC1324.2
010100         04 FILLER                           PIC X.               NC1324.2
010200     03 FILLER PIC X(2) VALUE SPACE.                              NC1324.2
010300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1324.2
010400 01  CCVS-C-1.                                                    NC1324.2
010500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1324.2
010600-    "SS  PARAGRAPH-NAME                                          NC1324.2
010700-    "       REMARKS".                                            NC1324.2
010800     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1324.2
010900 01  CCVS-C-2.                                                    NC1324.2
011000     02 FILLER                     PIC X        VALUE SPACE.      NC1324.2
011100     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1324.2
011200     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1324.2
011300     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1324.2
011400     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1324.2
011500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1324.2
011600 01  REC-CT                        PIC 99       VALUE ZERO.       NC1324.2
011700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1324.2
011800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1324.2
011900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1324.2
012000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1324.2
012100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1324.2
012200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1324.2
012300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1324.2
012400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1324.2
012500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1324.2
012600 01  CCVS-H-1.                                                    NC1324.2
012700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1324.2
012800     02  FILLER                    PIC X(42)    VALUE             NC1324.2
012900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1324.2
013000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1324.2
013100 01  CCVS-H-2A.                                                   NC1324.2
013200   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1324.2
013300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1324.2
013400   02  FILLER                        PIC XXXX   VALUE             NC1324.2
013500     "4.2 ".                                                      NC1324.2
013600   02  FILLER                        PIC X(28)  VALUE             NC1324.2
013700            " COPY - NOT FOR DISTRIBUTION".                       NC1324.2
013800   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1324.2
013900                                                                  NC1324.2
014000 01  CCVS-H-2B.                                                   NC1324.2
014100   02  FILLER                        PIC X(15)  VALUE             NC1324.2
014200            "TEST RESULT OF ".                                    NC1324.2
014300   02  TEST-ID                       PIC X(9).                    NC1324.2
014400   02  FILLER                        PIC X(4)   VALUE             NC1324.2
014500            " IN ".                                               NC1324.2
014600   02  FILLER                        PIC X(12)  VALUE             NC1324.2
014700     " HIGH       ".                                              NC1324.2
014800   02  FILLER                        PIC X(22)  VALUE             NC1324.2
014900            " LEVEL VALIDATION FOR ".                             NC1324.2
015000   02  FILLER                        PIC X(58)  VALUE             NC1324.2
015100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1324.2
015200 01  CCVS-H-3.                                                    NC1324.2
015300     02  FILLER                      PIC X(34)  VALUE             NC1324.2
015400            " FOR OFFICIAL USE ONLY    ".                         NC1324.2
015500     02  FILLER                      PIC X(58)  VALUE             NC1324.2
015600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1324.2
015700     02  FILLER                      PIC X(28)  VALUE             NC1324.2
015800            "  COPYRIGHT   1985 ".                                NC1324.2
015900 01  CCVS-E-1.                                                    NC1324.2
016000     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1324.2
016100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1324.2
016200     02 ID-AGAIN                     PIC X(9).                    NC1324.2
016300     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1324.2
016400 01  CCVS-E-2.                                                    NC1324.2
016500     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1324.2
016600     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1324.2
016700     02 CCVS-E-2-2.                                               NC1324.2
016800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1324.2
016900         03 FILLER                   PIC X      VALUE SPACE.      NC1324.2
017000         03 ENDER-DESC               PIC X(44)  VALUE             NC1324.2
017100            "ERRORS ENCOUNTERED".                                 NC1324.2
017200 01  CCVS-E-3.                                                    NC1324.2
017300     02  FILLER                      PIC X(22)  VALUE             NC1324.2
017400            " FOR OFFICIAL USE ONLY".                             NC1324.2
017500     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1324.2
017600     02  FILLER                      PIC X(58)  VALUE             NC1324.2
017700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1324.2
017800     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1324.2
017900     02 FILLER                       PIC X(15)  VALUE             NC1324.2
018000             " COPYRIGHT 1985".                                   NC1324.2
018100 01  CCVS-E-4.                                                    NC1324.2
018200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1324.2
018300     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1324.2
018400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1324.2
018500     02 FILLER                       PIC X(40)  VALUE             NC1324.2
018600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1324.2
018700 01  XXINFO.                                                      NC1324.2
018800     02 FILLER                       PIC X(19)  VALUE             NC1324.2
018900            "*** INFORMATION ***".                                NC1324.2
019000     02 INFO-TEXT.                                                NC1324.2
019100       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1324.2
019200       04 XXCOMPUTED                 PIC X(20).                   NC1324.2
019300       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1324.2
019400       04 XXCORRECT                  PIC X(20).                   NC1324.2
019500     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1324.2
019600 01  HYPHEN-LINE.                                                 NC1324.2
019700     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1324.2
019800     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1324.2
019900-    "*****************************************".                 NC1324.2
020000     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1324.2
020100-    "******************************".                            NC1324.2
020200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1324.2
020300     "NC132A".                                                    NC1324.2
020400 PROCEDURE DIVISION.                                              NC1324.2
020500 CCVS1 SECTION.                                                   NC1324.2
020600 OPEN-FILES.                                                      NC1324.2
020700     OPEN     OUTPUT PRINT-FILE.                                  NC1324.2
020800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1324.2
020900     MOVE    SPACE TO TEST-RESULTS.                               NC1324.2
021000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1324.2
021100     GO TO CCVS1-EXIT.                                            NC1324.2
021200 CLOSE-FILES.                                                     NC1324.2
021300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1324.2
021400 TERMINATE-CCVS.                                                  NC1324.2
021500*    EXIT PROGRAM.                                                NC1324.2
021600*TERMINATE-CALL.                                                  NC1324.2
021700     STOP     RUN.                                                NC1324.2
021800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1324.2
021900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1324.2
022000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1324.2
022100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1324.2
022200     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1324.2
022300 PRINT-DETAIL.                                                    NC1324.2
022400     IF REC-CT NOT EQUAL TO ZERO                                  NC1324.2
022500             MOVE "." TO PARDOT-X                                 NC1324.2
022600             MOVE REC-CT TO DOTVALUE.                             NC1324.2
022700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1324.2
022800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1324.2
022900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1324.2
023000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1324.2
023100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1324.2
023200     MOVE SPACE TO CORRECT-X.                                     NC1324.2
023300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1324.2
023400     MOVE     SPACE TO RE-MARK.                                   NC1324.2
023500 HEAD-ROUTINE.                                                    NC1324.2
023600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1324.2
023700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1324.2
023800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1324.2
023900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1324.2
024000 COLUMN-NAMES-ROUTINE.                                            NC1324.2
024100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1324.2
024200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1324.2
024300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1324.2
024400 END-ROUTINE.                                                     NC1324.2
024500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1324.2
024600 END-RTN-EXIT.                                                    NC1324.2
024700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1324.2
024800 END-ROUTINE-1.                                                   NC1324.2
024900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1324.2
025000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1324.2
025100      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1324.2
025200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1324.2
025300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1324.2
025400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1324.2
025500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1324.2
025600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1324.2
025700  END-ROUTINE-12.                                                 NC1324.2
025800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1324.2
025900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1324.2
026000         MOVE "NO " TO ERROR-TOTAL                                NC1324.2
026100         ELSE                                                     NC1324.2
026200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1324.2
026300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1324.2
026400     PERFORM WRITE-LINE.                                          NC1324.2
026500 END-ROUTINE-13.                                                  NC1324.2
026600     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1324.2
026700         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1324.2
026800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1324.2
026900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1324.2
027000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1324.2
027100      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1324.2
027200          MOVE "NO " TO ERROR-TOTAL                               NC1324.2
027300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1324.2
027400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1324.2
027500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1324.2
027600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1324.2
027700 WRITE-LINE.                                                      NC1324.2
027800     ADD 1 TO RECORD-COUNT.                                       NC1324.2
027900*    IF RECORD-COUNT GREATER 42                                   NC1324.2
028000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1324.2
028100*        MOVE SPACE TO DUMMY-RECORD                               NC1324.2
028200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1324.2
028300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1324.2
028400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1324.2
028500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1324.2
028600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1324.2
028700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1324.2
028800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1324.2
028900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1324.2
029000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1324.2
029100*        MOVE ZERO TO RECORD-COUNT.                               NC1324.2
029200     PERFORM WRT-LN.                                              NC1324.2
029300 WRT-LN.                                                          NC1324.2
029400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1324.2
029500     MOVE SPACE TO DUMMY-RECORD.                                  NC1324.2
029600 BLANK-LINE-PRINT.                                                NC1324.2
029700     PERFORM WRT-LN.                                              NC1324.2
029800 FAIL-ROUTINE.                                                    NC1324.2
029900     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1324.2
030000            GO TO FAIL-ROUTINE-WRITE.                             NC1324.2
030100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1324.2
030200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1324.2
030300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1324.2
030400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1324.2
030500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1324.2
030600     GO TO  FAIL-ROUTINE-EX.                                      NC1324.2
030700 FAIL-ROUTINE-WRITE.                                              NC1324.2
030800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1324.2
030900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1324.2
031000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1324.2
031100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1324.2
031200 FAIL-ROUTINE-EX. EXIT.                                           NC1324.2
031300 BAIL-OUT.                                                        NC1324.2
031400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1324.2
031500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1324.2
031600 BAIL-OUT-WRITE.                                                  NC1324.2
031700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1324.2
031800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1324.2
031900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1324.2
032000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1324.2
032100 BAIL-OUT-EX. EXIT.                                               NC1324.2
032200 CCVS1-EXIT.                                                      NC1324.2
032300     EXIT.                                                        NC1324.2
032400 SECT-TH132A-001 SECTION.                                         NC1324.2
032500 TH-02-001.                                                       NC1324.2
032600     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC1324.2
032700     MOVE TABLE-A-VALUES  TO  TABLE-A.                            NC1324.2
032800     MOVE  "SUBSCRIPTING"  TO FEATURE.                            NC1324.2
032900 SUB-SCRIPT-1.                                                    NC1324.2
033000     IF TABLE-A IS NOT EQUAL TO TABLE-A-VALUES                    NC1324.2
033100     GO TO  SUB-SCRIPT-1B.                                        NC1324.2
033200     PERFORM PASS.                                                NC1324.2
033300     GO TO  SUB-SCRIPT-1C.                                        NC1324.2
033400 SUB-SCRIPT-1A.                                                   NC1324.2
033500     PERFORM DE-LETE.                                             NC1324.2
033600     GO TO SUB-SCRIPT-1C.                                         NC1324.2
033700 SUB-SCRIPT-1B.                                                   NC1324.2
033800     PERFORM FAIL.                                                NC1324.2
033900     MOVE TABLE-A TO COMPUTED-A.                                  NC1324.2
034000     MOVE   "11122233344411223344" TO CORRECT-A.                  NC1324.2
034100 SUB-SCRIPT-1C.                                                   NC1324.2
034200     MOVE "SUB-SCRIPT-1" TO PAR-NAME.                             NC1324.2
034300     PERFORM PRINT-DETAIL.                                        NC1324.2
034400*    NOTE    *******  THIS  TEST CHECKS TO SEE  THAT THE TABLE    NC1324.2
034500*                     TO BE USED IN THE PROGRAM IS SET UP         NC1324.2
034600*                     CORRECTLY.                                  NC1324.2
034700 SUB-SCRIPT-2.                                                    NC1324.2
034800     IF  ENTRY-A-1 (1) NOT EQUAL TO "111"                         NC1324.2
034900     GO TO SUB-SCRIPT-2B.                                         NC1324.2
035000     PERFORM PASS.                                                NC1324.2
035100     GO TO SUB-SCRIPT-2C.                                         NC1324.2
035200 SUB-SCRIPT-2A.                                                   NC1324.2
035300     PERFORM DE-LETE.                                             NC1324.2
035400     GO TO SUB-SCRIPT-2C.                                         NC1324.2
035500 SUB-SCRIPT-2B.                                                   NC1324.2
035600     PERFORM FAIL.                                                NC1324.2
035700     MOVE ENTRY-A-1 (1) TO COMPUTED-A                             NC1324.2
035800     MOVE "111" TO CORRECT-A.                                     NC1324.2
035900 SUB-SCRIPT-2C.                                                   NC1324.2
036000     MOVE "SUB-SCRIPT-2" TO PAR-NAME.                             NC1324.2
036100     PERFORM  PRINT-DETAIL.                                       NC1324.2
036200*    NOTE   ******   THIS CHECKS THE USE OF NUMERIC  LITERALS     NC1324.2
036300*                    AS SUBSCRIPTS.                               NC1324.2
036400 SUB-SCRIPT-3.                                                    NC1324.2
036500     IF ENTRY-A-1 (SUB1)    NOT EQUAL TO "111"                    NC1324.2
036600     GO TO SUB-SCRIPT-3B.                                         NC1324.2
036700     PERFORM PASS.                                                NC1324.2
036800     GO TO SUB-SCRIPT-3C.                                         NC1324.2
036900 SUB-SCRIPT-3A.                                                   NC1324.2
037000     PERFORM DE-LETE.                                             NC1324.2
037100     GO TO SUB-SCRIPT-3C.                                         NC1324.2
037200 SUB-SCRIPT-3B.                                                   NC1324.2
037300     PERFORM FAIL.                                                NC1324.2
037400     MOVE ENTRY-A-1 (SUB1) TO COMPUTED-A.                         NC1324.2
037500     MOVE "111" TO CORRECT-A.                                     NC1324.2
037600 SUB-SCRIPT-3C.                                                   NC1324.2
037700     MOVE "SUB-SCRIPT-3" TO PAR-NAME.                             NC1324.2
037800     PERFORM PRINT-DETAIL.                                        NC1324.2
037900*    NOTE  *******      THIS CHECKS THE USE OF UNSIGNED           NC1324.2
038000*                       CONSTANTS AS SUBSCRIPTS.                  NC1324.2
038100 SUB-SCRIPT-4.                                                    NC1324.2
038200     ADD 1 TO SUB1.                                               NC1324.2
038300     IF ENTRY-A-1 (SUB1) NOT EQUAL TO "222"                       NC1324.2
038400     GO TO SUB-SCRIPT-4B.                                         NC1324.2
038500     PERFORM PASS.                                                NC1324.2
038600     GO TO SUB-SCRIPT-4C.                                         NC1324.2
038700 SUB-SCRIPT-4A.                                                   NC1324.2
038800     PERFORM DE-LETE.                                             NC1324.2
038900     GO TO SUB-SCRIPT-4C.                                         NC1324.2
039000 SUB-SCRIPT-4B.                                                   NC1324.2
039100     PERFORM FAIL.                                                NC1324.2
039200     MOVE ENTRY-A-1 (SUB1) TO COMPUTED-A.                         NC1324.2
039300     MOVE "222" TO CORRECT-A.                                     NC1324.2
039400 SUB-SCRIPT-4C.                                                   NC1324.2
039500     MOVE "SUB-SCRIPT-4" TO  PAR-NAME.                            NC1324.2
039600     PERFORM PRINT-DETAIL.                                        NC1324.2
039700*    NOTE  *******    THIS CHECKS THE VARYING OF AN               NC1324.2
039800*                     UNSIGNED SUBSCRIPT.                         NC1324.2
039900 SUB-SCRIPT-5.                                                    NC1324.2
040000     MOVE +4 TO SUB2.                                             NC1324.2
040100     IF ENTRY-A-1 (SUB2)  NOT EQUAL TO "444"                      NC1324.2
040200     GO TO  SUB-SCRIPT-5B.                                        NC1324.2
040300     PERFORM PASS.                                                NC1324.2
040400     GO TO SUB-SCRIPT-5C.                                         NC1324.2
040500 SUB-SCRIPT-5A.                                                   NC1324.2
040600     PERFORM DE-LETE.                                             NC1324.2
040700     GO TO SUB-SCRIPT-5C.                                         NC1324.2
040800 SUB-SCRIPT-5B.                                                   NC1324.2
040900     PERFORM FAIL.                                                NC1324.2
041000     MOVE ENTRY-A-1 (SUB2) TO COMPUTED-A.                         NC1324.2
041100     MOVE "444" TO CORRECT-A.                                     NC1324.2
041200 SUB-SCRIPT-5C.                                                   NC1324.2
041300     MOVE "SUB-SCRIPT-5" TO  PAR-NAME.                            NC1324.2
041400     PERFORM PRINT-DETAIL.                                        NC1324.2
041500*    NOTE  ******  THIS CHECKS THE USE OF A SIGNED                NC1324.2
041600*                  CONSTANT AS SUBSCRIPTS.                        NC1324.2
041700 SUB-SCRIPT-6.                                                    NC1324.2
041800     SUBTRACT  +1 FROM SUB2.                                      NC1324.2
041900     IF ENTRY-A-1 (SUB2)  NOT EQUAL TO "333"                      NC1324.2
042000     GO TO SUB-SCRIPT-6B.                                         NC1324.2
042100     PERFORM PASS.                                                NC1324.2
042200     GO TO SUB-SCRIPT-6C.                                         NC1324.2
042300 SUB-SCRIPT-6A.                                                   NC1324.2
042400     PERFORM DE-LETE.                                             NC1324.2
042500     GO TO SUB-SCRIPT-6C.                                         NC1324.2
042600 SUB-SCRIPT-6B.                                                   NC1324.2
042700     PERFORM FAIL.                                                NC1324.2
042800     MOVE ENTRY-A-1 (SUB2) TO COMPUTED-A.                         NC1324.2
042900     MOVE "333" TO CORRECT-A.                                     NC1324.2
043000 SUB-SCRIPT-6C.                                                   NC1324.2
043100     MOVE "SUB-SCRIPT-6" TO PAR-NAME.                             NC1324.2
043200     PERFORM PRINT-DETAIL.                                        NC1324.2
043300*    NOTE  ******   THIS CHECKS THE VARYING OF A                  NC1324.2
043400*                   SIGNED SUBSCRIPT.                             NC1324.2
043500*SUB-SCRIPT-7.                                                    NC1324.2
043600*    THIS TEST WAS DELETED BY THE FCCTS SINCE IT DID NOT          NC1324.2
043700*    APPLY TO THE REVISED FEDERAL STANDARD - FIPS PUB 21-1.       NC1324.2
043800 SUB-SCRIPT-8.                                                    NC1324.2
043900     IF ENTRY-A-3 (2)  NOT EQUAL TO "2"                           NC1324.2
044000     GO TO SUB-SCRIPT-8B.                                         NC1324.2
044100     PERFORM PASS.                                                NC1324.2
044200     GO TO SUB-SCRIPT-8C.                                         NC1324.2
044300 SUB-SCRIPT-8A.                                                   NC1324.2
044400     PERFORM DE-LETE.                                             NC1324.2
044500     GO TO SUB-SCRIPT-8C.                                         NC1324.2
044600 SUB-SCRIPT-8B.                                                   NC1324.2
044700     PERFORM FAIL.                                                NC1324.2
044800     MOVE ENTRY-A-3 (2) TO COMPUTED-A.                            NC1324.2
044900     MOVE "2" TO CORRECT-A.                                       NC1324.2
045000 SUB-SCRIPT-8C.                                                   NC1324.2
045100     MOVE "SUB-SCRIPT-8" TO PAR-NAME.                             NC1324.2
045200     PERFORM PRINT-DETAIL.                                        NC1324.2
045300*    NOTE  *****  THIS CHECKS THE USE OF AN ELEMENTARY ITEM       NC1324.2
045400*                 WHEN THE GROUP ITEM HAS THE OCCURS.             NC1324.2
045500 SUB-SCRIPT-9.                                                    NC1324.2
045600     IF ENTRY-A-2 (4)    NOT EQUAL TO  "44"                       NC1324.2
045700     GO TO SUB-SCRIPT-9B.                                         NC1324.2
045800     PERFORM PASS.                                                NC1324.2
045900     GO TO SUB-SCRIPT-9C.                                         NC1324.2
046000 SUB-SCRIPT-9A.                                                   NC1324.2
046100     PERFORM DE-LETE.                                             NC1324.2
046200     GO TO SUB-SCRIPT-9C.                                         NC1324.2
046300 SUB-SCRIPT-9B.                                                   NC1324.2
046400     PERFORM FAIL.                                                NC1324.2
046500     MOVE ENTRY-A-2 (4) TO COMPUTED-A.                            NC1324.2
046600     MOVE "44" TO CORRECT-A.                                      NC1324.2
046700 SUB-SCRIPT-9C.                                                   NC1324.2
046800     MOVE "SUB-SCRIPT-9" TO PAR-NAME.                             NC1324.2
046900     PERFORM PRINT-DETAIL.                                        NC1324.2
047000*    NOTE  *******  THIS CHECKS A GROUP ITEM WHEN IT              NC1324.2
047100*                   HAS THE OCCURS.                               NC1324.2
047200 SUB-SCRIPT-10.                                                   NC1324.2
047300     IF ENTRY-B-2 (1)  NOT EQUAL TO "1"                           NC1324.2
047400     GO TO SUB-SCRIPT-10B.                                        NC1324.2
047500     PERFORM PASS.                                                NC1324.2
047600     GO TO SUB-SCRIPT-10C.                                        NC1324.2
047700 SUB-SCRIPT-10A.                                                  NC1324.2
047800     PERFORM DE-LETE.                                             NC1324.2
047900     GO TO SUB-SCRIPT-10C.                                        NC1324.2
048000 SUB-SCRIPT-10B.                                                  NC1324.2
048100     PERFORM FAIL.                                                NC1324.2
048200     MOVE ENTRY-B-2 (1) TO COMPUTED-A.                            NC1324.2
048300     MOVE "1" TO CORRECT-A.                                       NC1324.2
048400 SUB-SCRIPT-10C.                                                  NC1324.2
048500     MOVE "SUB-SCRIPT-10" TO PAR-NAME.                            NC1324.2
048600     PERFORM PRINT-DETAIL.                                        NC1324.2
048700*    NOTE   ******  THIS CHECKS THE USE OF THE REDEFINE.          NC1324.2
048800 SUB-SCRIPT-11.                                                   NC1324.2
048900     MOVE  ENTRY-B-2 (1) TO ENTRY-C (4).                          NC1324.2
049000     MOVE  ENTRY-B-2 (2) TO ENTRY-C (3).                          NC1324.2
049100     MOVE  ENTRY-B-2 (3) TO ENTRY-C (2).                          NC1324.2
049200     MOVE  ENTRY-B-2 (4) TO ENTRY-C (1).                          NC1324.2
049300     IF TABLE-C    NOT  EQUAL TO "4321"                           NC1324.2
049400     GO TO SUB-SCRIPT-11B.                                        NC1324.2
049500     PERFORM PASS.                                                NC1324.2
049600     GO TO SUB-SCRIPT-11C.                                        NC1324.2
049700 SUB-SCRIPT-11A.                                                  NC1324.2
049800     PERFORM DE-LETE.                                             NC1324.2
049900     GO TO SUB-SCRIPT-11C.                                        NC1324.2
050000 SUB-SCRIPT-11B.                                                  NC1324.2
050100     PERFORM FAIL.                                                NC1324.2
050200     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
050300     MOVE "4321" TO CORRECT-A.                                    NC1324.2
050400 SUB-SCRIPT-11C.                                                  NC1324.2
050500     MOVE "SUB-SCRIPT-11" TO PAR-NAME.                            NC1324.2
050600     PERFORM PRINT-DETAIL.                                        NC1324.2
050700 SUB-SCRIPT-12.                                                   NC1324.2
050800     MOVE "0000" TO TABLE-C.                                      NC1324.2
050900     ADD ENTRY-B-2 (1) TO ENTRY-C (1).                            NC1324.2
051000     ADD ENTRY-B-2 (2) TO ENTRY-C (2).                            NC1324.2
051100     ADD ENTRY-B-2 (3) TO ENTRY-C (3).                            NC1324.2
051200     ADD ENTRY-B-2 (4) TO ENTRY-C (4).                            NC1324.2
051300     IF TABLE-C NOT EQUAL TO "1234"                               NC1324.2
051400     GO TO SUB-SCRIPT-12B.                                        NC1324.2
051500     PERFORM PASS.                                                NC1324.2
051600     GO TO SUB-SCRIPT-12C.                                        NC1324.2
051700 SUB-SCRIPT-12A.                                                  NC1324.2
051800     PERFORM DE-LETE.                                             NC1324.2
051900     GO TO SUB-SCRIPT-12C.                                        NC1324.2
052000 SUB-SCRIPT-12B.                                                  NC1324.2
052100     PERFORM FAIL.                                                NC1324.2
052200     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
052300     MOVE "1234" TO CORRECT-A.                                    NC1324.2
052400 SUB-SCRIPT-12C.                                                  NC1324.2
052500     MOVE "SUB-SCRIPT-12" TO PAR-NAME.                            NC1324.2
052600     PERFORM  PRINT-DETAIL.                                       NC1324.2
052700*    THIS TEST CHECKS THE USE OF SUBSCRIPTED DATA-NAMES           NC1324.2
052800*    IN AN ADD STATEMENT                                          NC1324.2
052900 SUB-SCRIPT-13.                                                   NC1324.2
053000     MOVE  "1234" TO TABLE-C.                                     NC1324.2
053100     SUBTRACT ENTRY-B-2 (1) FROM ENTRY-C (1).                     NC1324.2
053200     SUBTRACT ENTRY-B-2 (2) FROM ENTRY-C (2).                     NC1324.2
053300     SUBTRACT ENTRY-B-2 (3) FROM ENTRY-C (3).                     NC1324.2
053400     SUBTRACT ENTRY-B-2 (4) FROM ENTRY-C (4).                     NC1324.2
053500     IF TABLE-C  NOT EQUAL TO  "0000"                             NC1324.2
053600     GO TO  SUB-SCRIPT-13B.                                       NC1324.2
053700     PERFORM PASS.                                                NC1324.2
053800     GO TO SUB-SCRIPT-13C.                                        NC1324.2
053900 SUB-SCRIPT-13A.                                                  NC1324.2
054000     PERFORM DE-LETE.                                             NC1324.2
054100     GO TO SUB-SCRIPT-13C.                                        NC1324.2
054200 SUB-SCRIPT-13B.                                                  NC1324.2
054300     PERFORM FAIL.                                                NC1324.2
054400     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
054500     MOVE "0000" TO CORRECT-A.                                    NC1324.2
054600 SUB-SCRIPT-13C.                                                  NC1324.2
054700     MOVE "SUB-SCRIPT-13" TO PAR-NAME.                            NC1324.2
054800     PERFORM  PRINT-DETAIL.                                       NC1324.2
054900*    THIS TEST CHECKS THE USE OF SUBSCRIPTED DATA-NAMES           NC1324.2
055000*    IN A SUBTRACT STATEMENT.                                     NC1324.2
055100 SUB-SCRIPT-14.                                                   NC1324.2
055200     MOVE "1111" TO TABLE-C.                                      NC1324.2
055300     MULTIPLY ENTRY-B-2 (1) BY ENTRY-C (1).                       NC1324.2
055400     MULTIPLY ENTRY-B-2 (2) BY ENTRY-C (2).                       NC1324.2
055500     MULTIPLY ENTRY-B-2 (3) BY ENTRY-C (3).                       NC1324.2
055600     MULTIPLY ENTRY-B-2 (4) BY ENTRY-C (4).                       NC1324.2
055700     IF TABLE-C  NOT EQUAL TO "1234"                              NC1324.2
055800     GO TO SUB-SCRIPT-14B.                                        NC1324.2
055900     PERFORM PASS.                                                NC1324.2
056000     GO TO SUB-SCRIPT-14C.                                        NC1324.2
056100 SUB-SCRIPT-14A.                                                  NC1324.2
056200     PERFORM DE-LETE.                                             NC1324.2
056300     GO TO SUB-SCRIPT-14C.                                        NC1324.2
056400 SUB-SCRIPT-14B.                                                  NC1324.2
056500     PERFORM FAIL.                                                NC1324.2
056600     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
056700     MOVE "1234" TO CORRECT-A.                                    NC1324.2
056800 SUB-SCRIPT-14C.                                                  NC1324.2
056900     MOVE  "SUB-SCRIPT-14" TO PAR-NAME.                           NC1324.2
057000     PERFORM  PRINT-DETAIL.                                       NC1324.2
057100*    THIS TEST CHECKS THE USE OF SUBSCRIPTED DATA-NAMES           NC1324.2
057200*    IN A MULTIPLY STATEMENT.                                     NC1324.2
057300 SUB-SCRIPT-15.                                                   NC1324.2
057400     MOVE  "1234" TO TABLE-C.                                     NC1324.2
057500     DIVIDE  ENTRY-B-2 (1) INTO ENTRY-C (1).                      NC1324.2
057600     DIVIDE  ENTRY-B-2 (2) INTO ENTRY-C (2).                      NC1324.2
057700     DIVIDE  ENTRY-B-2 (3) INTO ENTRY-C (3).                      NC1324.2
057800     DIVIDE  ENTRY-B-2 (4) INTO ENTRY-C (4).                      NC1324.2
057900     IF  TABLE-C  NOT EQUAL TO "1111"                             NC1324.2
058000     GO TO  SUB-SCRIPT-15B.                                       NC1324.2
058100     PERFORM PASS.                                                NC1324.2
058200     GO TO SUB-SCRIPT-15C.                                        NC1324.2
058300 SUB-SCRIPT-15A.                                                  NC1324.2
058400     PERFORM DE-LETE.                                             NC1324.2
058500     GO TO  SUB-SCRIPT-15C.                                       NC1324.2
058600 SUB-SCRIPT-15B.                                                  NC1324.2
058700     PERFORM FAIL.                                                NC1324.2
058800     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
058900     MOVE "1111" TO CORRECT-A.                                    NC1324.2
059000 SUB-SCRIPT-15C.                                                  NC1324.2
059100     MOVE "SUB-SCRIPT-15" TO PAR-NAME.                            NC1324.2
059200     PERFORM PRINT-DETAIL.                                        NC1324.2
059300*    THIS TEST CHECKS THE USE OF SUBSCRIPTED DATA-NAMES           NC1324.2
059400*    IN A DIVIDE STATEMENT.                                       NC1324.2
059500     MOVE TABLE-A-VALUES TO TABLE-A.                              NC1324.2
059600 SUB-SCRIPT-16.                                                   NC1324.2
059700     IF ENTRY-A-1 (SUB-3) EQUAL TO "222"                          NC1324.2
059800         PERFORM PASS                                             NC1324.2
059900         GO TO SUB-SCRIPT-16B.                                    NC1324.2
060000     MOVE "222" TO CORRECT-A.                                     NC1324.2
060100     MOVE ENTRY-A-1 (SUB-3) TO COMPUTED-A.                        NC1324.2
060200     PERFORM FAIL.                                                NC1324.2
060300     GO TO SUB-SCRIPT-16B.                                        NC1324.2
060400 SUB-SCRIPT-16A.                                                  NC1324.2
060500     PERFORM DE-LETE.                                             NC1324.2
060600 SUB-SCRIPT-16B.                                                  NC1324.2
060700     MOVE "SUB-SCRIPT-16" TO PAR-NAME.                            NC1324.2
060800*    NOTE  COMPUTATIONAL SUBSCRIPT  USED  S9(18).                 NC1324.2
060900     PERFORM PRINT-DETAIL.                                        NC1324.2
061000 SUB-SCRIPT-17.                                                   NC1324.2
061100     IF ENTRY-A-2 (SUB-4) EQUAL TO "44"                           NC1324.2
061200         PERFORM PASS                                             NC1324.2
061300         GO TO SUB-SCRIPT-17B.                                    NC1324.2
061400     MOVE "44" TO CORRECT-A.                                      NC1324.2
061500     MOVE ENTRY-A-2 (SUB-4) TO COMPUTED-A.                        NC1324.2
061600     PERFORM FAIL.                                                NC1324.2
061700     GO TO SUB-SCRIPT-17B.                                        NC1324.2
061800 SUB-SCRIPT-17A.                                                  NC1324.2
061900     PERFORM DE-LETE.                                             NC1324.2
062000 SUB-SCRIPT-17B.                                                  NC1324.2
062100     MOVE "SUB-SCRIPT-17" TO PAR-NAME.                            NC1324.2
062200*    NOTE COMPUTATIONAL SUBSCRIPT  USED  9(18).                   NC1324.2
062300     PERFORM PRINT-DETAIL.                                        NC1324.2
062400 SUB-SCRIPT-18.                                                   NC1324.2
062500     IF ENTRY-A-2 (+4) EQUAL TO "44"                              NC1324.2
062600         PERFORM PASS                                             NC1324.2
062700         GO TO SUB-SCRIPT-18B.                                    NC1324.2
062800     MOVE "44" TO CORRECT-A.                                      NC1324.2
062900     MOVE ENTRY-A-2 (+4) TO COMPUTED-A.                           NC1324.2
063000     PERFORM FAIL.                                                NC1324.2
063100     GO TO SUB-SCRIPT-18B.                                        NC1324.2
063200 SUB-SCRIPT-18A.                                                  NC1324.2
063300     PERFORM DE-LETE.                                             NC1324.2
063400 SUB-SCRIPT-18B.                                                  NC1324.2
063500     MOVE "SUB-SCRIPT-18" TO PAR-NAME.                            NC1324.2
063600*    NOTE SIGNED NUMERIC LITERAL SUBSCRIPT.                       NC1324.2
063700     PERFORM PRINT-DETAIL.                                        NC1324.2
063800 SUB-SCRIPT-19.                                                   NC1324.2
063900     IF ENTRY-A-3 (CON-2) NOT EQUAL TO "2"                        NC1324.2
064000     GO TO SUB-SCRIPT-19B.                                        NC1324.2
064100     PERFORM PASS.                                                NC1324.2
064200     GO TO SUB-SCRIPT-19C.                                        NC1324.2
064300 SUB-SCRIPT-19A.                                                  NC1324.2
064400     PERFORM DE-LETE.                                             NC1324.2
064500     GO TO SUB-SCRIPT-19C.                                        NC1324.2
064600 SUB-SCRIPT-19B.                                                  NC1324.2
064700     PERFORM FAIL.                                                NC1324.2
064800     MOVE ENTRY-A-3 (CON-2) TO COMPUTED-A.                        NC1324.2
064900     MOVE "2" TO CORRECT-A.                                       NC1324.2
065000 SUB-SCRIPT-19C.                                                  NC1324.2
065100     MOVE "SUB-SCRIPT-19" TO PAR-NAME.                            NC1324.2
065200     PERFORM PRINT-DETAIL.                                        NC1324.2
065300*    NOTE  *****  THIS CHECKS THE USE OF AN ELEMENTARY ITEM       NC1324.2
065400*                 WHEN THE GROUP ITEM HAS THE OCCURS.             NC1324.2
065500 SUB-SCRIPT-20.                                                   NC1324.2
065600     IF ENTRY-A-2 (CON-4) NOT EQUAL TO "44"                       NC1324.2
065700     GO TO SUB-SCRIPT-20B.                                        NC1324.2
065800     PERFORM PASS.                                                NC1324.2
065900     GO TO SUB-SCRIPT-20C.                                        NC1324.2
066000 SUB-SCRIPT-20A.                                                  NC1324.2
066100     PERFORM DE-LETE.                                             NC1324.2
066200     GO TO SUB-SCRIPT-20C.                                        NC1324.2
066300 SUB-SCRIPT-20B.                                                  NC1324.2
066400     PERFORM FAIL.                                                NC1324.2
066500     MOVE ENTRY-A-2 (CON-4) TO COMPUTED-A.                        NC1324.2
066600     MOVE "44" TO CORRECT-A.                                      NC1324.2
066700 SUB-SCRIPT-20C.                                                  NC1324.2
066800     MOVE "SUB-SCRIPT-20" TO PAR-NAME.                            NC1324.2
066900     PERFORM PRINT-DETAIL.                                        NC1324.2
067000*    NOTE  *******  THIS CHECKS A GROUP ITEM WHEN IT              NC1324.2
067100*                   HAS THE OCCURS.                               NC1324.2
067200 SUB-SCRIPT-21.                                                   NC1324.2
067300     IF ENTRY-B-2 (CON-1) NOT EQUAL TO "1"                        NC1324.2
067400     GO TO SUB-SCRIPT-21B.                                        NC1324.2
067500     PERFORM PASS.                                                NC1324.2
067600     GO TO SUB-SCRIPT-21C.                                        NC1324.2
067700 SUB-SCRIPT-21A.                                                  NC1324.2
067800     PERFORM DE-LETE.                                             NC1324.2
067900     GO TO SUB-SCRIPT-21C.                                        NC1324.2
068000 SUB-SCRIPT-21B.                                                  NC1324.2
068100     PERFORM FAIL.                                                NC1324.2
068200     MOVE ENTRY-B-2 (CON-1) TO COMPUTED-A.                        NC1324.2
068300     MOVE "1" TO CORRECT-A.                                       NC1324.2
068400 SUB-SCRIPT-21C.                                                  NC1324.2
068500     MOVE "SUB-SCRIPT-21" TO PAR-NAME.                            NC1324.2
068600     PERFORM PRINT-DETAIL.                                        NC1324.2
068700*    USE OF ITEM WHICH IS DEFINED WITH BOTH THE REDEFINES         NC1324.2
068800*    AND THE OCCURS CLAUSE.                                       NC1324.2
068900 SUB-SCRIPT-22.                                                   NC1324.2
069000     MOVE ENTRY-B-2 (CON-1) TO ENTRY-C (CON-4).                   NC1324.2
069100     MOVE ENTRY-B-2 (CON-2) TO ENTRY-C (CON-3).                   NC1324.2
069200     MOVE ENTRY-B-2 (CON-3) TO ENTRY-C (CON-2).                   NC1324.2
069300     MOVE ENTRY-B-2 (CON-4) TO ENTRY-C (CON-1).                   NC1324.2
069400     IF TABLE-C    NOT  EQUAL TO "4321"                           NC1324.2
069500     GO TO SUB-SCRIPT-22B.                                        NC1324.2
069600     PERFORM PASS.                                                NC1324.2
069700     GO TO SUB-SCRIPT-22C.                                        NC1324.2
069800 SUB-SCRIPT-22A.                                                  NC1324.2
069900     PERFORM DE-LETE.                                             NC1324.2
070000     GO TO SUB-SCRIPT-22C.                                        NC1324.2
070100 SUB-SCRIPT-22B.                                                  NC1324.2
070200     PERFORM FAIL.                                                NC1324.2
070300     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
070400     MOVE "4321" TO CORRECT-A.                                    NC1324.2
070500 SUB-SCRIPT-22C.                                                  NC1324.2
070600     MOVE "SUB-SCRIPT-22" TO PAR-NAME.                            NC1324.2
070700     PERFORM PRINT-DETAIL.                                        NC1324.2
070800 SUB-SCRIPT-23.                                                   NC1324.2
070900     MOVE "0000" TO TABLE-C.                                      NC1324.2
071000     ADD ENTRY-B-2 (CON-1) TO ENTRY-C (CON-1).                    NC1324.2
071100     ADD ENTRY-B-2 (CON-2) TO ENTRY-C (CON-2).                    NC1324.2
071200     ADD ENTRY-B-2 (CON-3) TO ENTRY-C (CON-3).                    NC1324.2
071300     ADD ENTRY-B-2 (CON-4) TO ENTRY-C (CON-4).                    NC1324.2
071400     IF TABLE-C NOT EQUAL TO "1234"                               NC1324.2
071500     GO TO SUB-SCRIPT-23B.                                        NC1324.2
071600     PERFORM PASS.                                                NC1324.2
071700     GO TO SUB-SCRIPT-23C.                                        NC1324.2
071800 SUB-SCRIPT-23A.                                                  NC1324.2
071900     PERFORM DE-LETE.                                             NC1324.2
072000     GO TO SUB-SCRIPT-23C.                                        NC1324.2
072100 SUB-SCRIPT-23B.                                                  NC1324.2
072200     PERFORM FAIL.                                                NC1324.2
072300     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
072400     MOVE "1234" TO CORRECT-A.                                    NC1324.2
072500 SUB-SCRIPT-23C.                                                  NC1324.2
072600     MOVE "SUB-SCRIPT-23" TO PAR-NAME.                            NC1324.2
072700     PERFORM  PRINT-DETAIL.                                       NC1324.2
072800*    ADD STATEMENTS WITH UNSIGNED NUMERIC ITEMS AS SUBSCRIPTS.    NC1324.2
072900 SUB-SCRIPT-24.                                                   NC1324.2
073000     MOVE  "1234" TO TABLE-C.                                     NC1324.2
073100     SUBTRACT ENTRY-B-2 (CON-1) FROM ENTRY-C (CON-1).             NC1324.2
073200     SUBTRACT ENTRY-B-2 (CON-2) FROM ENTRY-C (CON-2).             NC1324.2
073300     SUBTRACT ENTRY-B-2 (CON-3) FROM ENTRY-C (CON-3).             NC1324.2
073400     SUBTRACT ENTRY-B-2 (CON-4) FROM ENTRY-C (CON-4).             NC1324.2
073500     IF TABLE-C  NOT EQUAL TO  "0000"                             NC1324.2
073600     GO TO  SUB-SCRIPT-24B.                                       NC1324.2
073700     PERFORM PASS.                                                NC1324.2
073800     GO TO SUB-SCRIPT-24C.                                        NC1324.2
073900 SUB-SCRIPT-24A.                                                  NC1324.2
074000     PERFORM DE-LETE.                                             NC1324.2
074100     GO TO SUB-SCRIPT-24C.                                        NC1324.2
074200 SUB-SCRIPT-24B.                                                  NC1324.2
074300     PERFORM FAIL.                                                NC1324.2
074400     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
074500     MOVE "0000" TO CORRECT-A.                                    NC1324.2
074600 SUB-SCRIPT-24C.                                                  NC1324.2
074700     MOVE "SUB-SCRIPT-24" TO PAR-NAME.                            NC1324.2
074800     PERFORM  PRINT-DETAIL.                                       NC1324.2
074900*    SUBSTRACT STATEMENTS WITH UNSIGNED NUMERIC ITEMS AS SUBSCRIPTNC1324.2
075000 SUB-SCRIPT-25.                                                   NC1324.2
075100     MOVE "1111" TO TABLE-C.                                      NC1324.2
075200     MULTIPLY ENTRY-B-2 (CON-1) BY ENTRY-C (CON-1).               NC1324.2
075300     MULTIPLY ENTRY-B-2 (CON-2) BY ENTRY-C (CON-2).               NC1324.2
075400     MULTIPLY ENTRY-B-2 (CON-3) BY ENTRY-C (CON-3).               NC1324.2
075500     MULTIPLY ENTRY-B-2 (CON-4) BY ENTRY-C (CON-4).               NC1324.2
075600     IF TABLE-C  NOT EQUAL TO "1234"                              NC1324.2
075700     GO TO SUB-SCRIPT-25B.                                        NC1324.2
075800     PERFORM PASS.                                                NC1324.2
075900     GO TO SUB-SCRIPT-25C.                                        NC1324.2
076000 SUB-SCRIPT-25A.                                                  NC1324.2
076100     PERFORM DE-LETE.                                             NC1324.2
076200     GO TO SUB-SCRIPT-25C.                                        NC1324.2
076300 SUB-SCRIPT-25B.                                                  NC1324.2
076400     PERFORM FAIL.                                                NC1324.2
076500     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
076600     MOVE "1234" TO CORRECT-A.                                    NC1324.2
076700 SUB-SCRIPT-25C.                                                  NC1324.2
076800     MOVE  "SUB-SCRIPT-25" TO PAR-NAME.                           NC1324.2
076900     PERFORM  PRINT-DETAIL.                                       NC1324.2
077000*    MULTIPLY STATEMENTS WITH UNSIGNED NUMERIC ITEMS AS SUBSCRIPTSNC1324.2
077100 SUB-SCRIPT-26.                                                   NC1324.2
077200     MOVE  "1234" TO TABLE-C.                                     NC1324.2
077300     DIVIDE ENTRY-B-2 (CON-1) INTO ENTRY-C (CON-1).               NC1324.2
077400     DIVIDE ENTRY-B-2 (CON-2) INTO ENTRY-C (CON-2).               NC1324.2
077500     DIVIDE ENTRY-B-2 (CON-3) INTO ENTRY-C (CON-3).               NC1324.2
077600     DIVIDE ENTRY-B-2 (CON-4) INTO ENTRY-C (CON-4).               NC1324.2
077700     IF  TABLE-C  NOT EQUAL TO "1111"                             NC1324.2
077800     GO TO  SUB-SCRIPT-26B.                                       NC1324.2
077900     PERFORM PASS.                                                NC1324.2
078000     GO TO SUB-SCRIPT-26C.                                        NC1324.2
078100 SUB-SCRIPT-26A.                                                  NC1324.2
078200     PERFORM DE-LETE.                                             NC1324.2
078300     GO TO  SUB-SCRIPT-26C.                                       NC1324.2
078400 SUB-SCRIPT-26B.                                                  NC1324.2
078500     PERFORM FAIL.                                                NC1324.2
078600     MOVE TABLE-C TO COMPUTED-A.                                  NC1324.2
078700     MOVE "1111" TO CORRECT-A.                                    NC1324.2
078800 SUB-SCRIPT-26C.                                                  NC1324.2
078900     MOVE "SUB-SCRIPT-26" TO PAR-NAME.                            NC1324.2
079000     PERFORM PRINT-DETAIL.                                        NC1324.2
079100*    DIVIDE STATEMENTS WITH UNSIGNED NUMERIC ITEMS AS SUBSCRIPTS. NC1324.2
079200 CCVS-EXIT SECTION.                                               NC1324.2
079300 CCVS-999999.                                                     NC1324.2
079400     GO TO CLOSE-FILES.                                           NC1324.2
      *END-OF,NC132A                                                                  
