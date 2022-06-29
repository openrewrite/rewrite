      *HEADER,COBOL,NC127A                                                            
000100 identification division.                                         NC1274.2
000200 program-id.                                                      NC1274.2
000300     nc127A.                                                      NC1274.2
000400****************************************************************  NC1274.2
000500*                                                              *  NC1274.2
000600*    VALIDATION FOR:-                                          *  NC1274.2
000700*                                                              *  NC1274.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1274.2
000900*                                                              *  NC1274.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1274.2
001100*                                                              *  NC1274.2
001200****************************************************************  NC1274.2
001300*                                                              *  NC1274.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1274.2
001500*                                                              *  NC1274.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1274.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1274.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1274.2
001900*                                                              *  NC1274.2
002000****************************************************************  NC1274.2
002100*                                                                 NC1274.2
002200*   program nc127a is written using lower case letters            NC1274.2
002300*   throughout, with the exception of standard COBOL text         NC1274.2
002400*   which is copied into every CCVS8x program from a library      NC1274.2
002500*   and some alphanumeric literals.                               NC1274.2
002600*                                                                 NC1274.2
002700 environment division.                                            NC1274.2
002800 configuration section.                                           NC1274.2
002900 source-computer.                                                 NC1274.2
003000     XXXXX082.                                                    NC1274.2
003100 object-computer.                                                 NC1274.2
003200     XXXXX083.                                                    NC1274.2
003300 input-output section.                                            NC1274.2
003400 file-control.                                                    NC1274.2
003500     select print-file assign to                                  NC1274.2
003600     XXXXX055.                                                    NC1274.2
003700 data division.                                                   NC1274.2
003800 file section.                                                    NC1274.2
003900 FD  PRINT-FILE.                                                  NC1274.2
004000 01  print-rec picture x(120).                                    NC1274.2
004100 01  dummy-record picture x(120).                                 NC1274.2
004200 working-storage section.                                         NC1274.2
004300 01  alphabetic-lit-upper  pic x(9) value "ABCRSTXYZ".            NC1274.2
004400 01  alphabetic-lit-lower  pic x(9) value "abcrstxyz".            NC1274.2
004500 01  alpha-lit-upper-lower pic x(9) value "dEfJkLuVw".            NC1274.2
004600 01  TEST-RESULTS.                                                NC1274.2
004700     02 FILLER                   PIC X      VALUE SPACE.          NC1274.2
004800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1274.2
004900     02 FILLER                   PIC X      VALUE SPACE.          NC1274.2
005000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1274.2
005100     02 FILLER                   PIC X      VALUE SPACE.          NC1274.2
005200     02  PAR-NAME.                                                NC1274.2
005300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1274.2
005400       03  PARDOT-X              PIC X      VALUE SPACE.          NC1274.2
005500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1274.2
005600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1274.2
005700     02 RE-MARK                  PIC X(61).                       NC1274.2
005800 01  TEST-COMPUTED.                                               NC1274.2
005900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1274.2
006000     02 FILLER                   PIC X(17)  VALUE                 NC1274.2
006100            "       COMPUTED=".                                   NC1274.2
006200     02 COMPUTED-X.                                               NC1274.2
006300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1274.2
006400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1274.2
006500                                 PIC -9(9).9(9).                  NC1274.2
006600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1274.2
006700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1274.2
006800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1274.2
006900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1274.2
007000         04 COMPUTED-18V0                    PIC -9(18).          NC1274.2
007100         04 FILLER                           PIC X.               NC1274.2
007200     03 FILLER PIC X(50) VALUE SPACE.                             NC1274.2
007300 01  TEST-CORRECT.                                                NC1274.2
007400     02 FILLER PIC X(30) VALUE SPACE.                             NC1274.2
007500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1274.2
007600     02 CORRECT-X.                                                NC1274.2
007700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1274.2
007800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1274.2
007900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1274.2
008000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1274.2
008100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1274.2
008200     03      CR-18V0 REDEFINES CORRECT-A.                         NC1274.2
008300         04 CORRECT-18V0                     PIC -9(18).          NC1274.2
008400         04 FILLER                           PIC X.               NC1274.2
008500     03 FILLER PIC X(2) VALUE SPACE.                              NC1274.2
008600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1274.2
008700 01  CCVS-C-1.                                                    NC1274.2
008800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1274.2
008900-    "SS  PARAGRAPH-NAME                                          NC1274.2
009000-    "       REMARKS".                                            NC1274.2
009100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1274.2
009200 01  CCVS-C-2.                                                    NC1274.2
009300     02 FILLER                     PIC X        VALUE SPACE.      NC1274.2
009400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1274.2
009500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1274.2
009600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1274.2
009700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1274.2
009800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1274.2
009900 01  REC-CT                        PIC 99       VALUE ZERO.       NC1274.2
010000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1274.2
010100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1274.2
010200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1274.2
010300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1274.2
010400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1274.2
010500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1274.2
010600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1274.2
010700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1274.2
010800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1274.2
010900 01  CCVS-H-1.                                                    NC1274.2
011000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1274.2
011100     02  FILLER                    PIC X(42)    VALUE             NC1274.2
011200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1274.2
011300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1274.2
011400 01  CCVS-H-2A.                                                   NC1274.2
011500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1274.2
011600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1274.2
011700   02  FILLER                        PIC XXXX   VALUE             NC1274.2
011800     "4.2 ".                                                      NC1274.2
011900   02  FILLER                        PIC X(28)  VALUE             NC1274.2
012000            " COPY - NOT FOR DISTRIBUTION".                       NC1274.2
012100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1274.2
012200                                                                  NC1274.2
012300 01  CCVS-H-2B.                                                   NC1274.2
012400   02  FILLER                        PIC X(15)  VALUE             NC1274.2
012500            "TEST RESULT OF ".                                    NC1274.2
012600   02  TEST-ID                       PIC X(9).                    NC1274.2
012700   02  FILLER                        PIC X(4)   VALUE             NC1274.2
012800            " IN ".                                               NC1274.2
012900   02  FILLER                        PIC X(12)  VALUE             NC1274.2
013000     " HIGH       ".                                              NC1274.2
013100   02  FILLER                        PIC X(22)  VALUE             NC1274.2
013200            " LEVEL VALIDATION FOR ".                             NC1274.2
013300   02  FILLER                        PIC X(58)  VALUE             NC1274.2
013400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1274.2
013500 01  CCVS-H-3.                                                    NC1274.2
013600     02  FILLER                      PIC X(34)  VALUE             NC1274.2
013700            " FOR OFFICIAL USE ONLY    ".                         NC1274.2
013800     02  FILLER                      PIC X(58)  VALUE             NC1274.2
013900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1274.2
014000     02  FILLER                      PIC X(28)  VALUE             NC1274.2
014100            "  COPYRIGHT   1985 ".                                NC1274.2
014200 01  CCVS-E-1.                                                    NC1274.2
014300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1274.2
014400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1274.2
014500     02 ID-AGAIN                     PIC X(9).                    NC1274.2
014600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1274.2
014700 01  CCVS-E-2.                                                    NC1274.2
014800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1274.2
014900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1274.2
015000     02 CCVS-E-2-2.                                               NC1274.2
015100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1274.2
015200         03 FILLER                   PIC X      VALUE SPACE.      NC1274.2
015300         03 ENDER-DESC               PIC X(44)  VALUE             NC1274.2
015400            "ERRORS ENCOUNTERED".                                 NC1274.2
015500 01  CCVS-E-3.                                                    NC1274.2
015600     02  FILLER                      PIC X(22)  VALUE             NC1274.2
015700            " FOR OFFICIAL USE ONLY".                             NC1274.2
015800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1274.2
015900     02  FILLER                      PIC X(58)  VALUE             NC1274.2
016000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1274.2
016100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1274.2
016200     02 FILLER                       PIC X(15)  VALUE             NC1274.2
016300             " COPYRIGHT 1985".                                   NC1274.2
016400 01  CCVS-E-4.                                                    NC1274.2
016500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1274.2
016600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1274.2
016700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1274.2
016800     02 FILLER                       PIC X(40)  VALUE             NC1274.2
016900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1274.2
017000 01  XXINFO.                                                      NC1274.2
017100     02 FILLER                       PIC X(19)  VALUE             NC1274.2
017200            "*** INFORMATION ***".                                NC1274.2
017300     02 INFO-TEXT.                                                NC1274.2
017400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1274.2
017500       04 XXCOMPUTED                 PIC X(20).                   NC1274.2
017600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1274.2
017700       04 XXCORRECT                  PIC X(20).                   NC1274.2
017800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1274.2
017900 01  HYPHEN-LINE.                                                 NC1274.2
018000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1274.2
018100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1274.2
018200-    "*****************************************".                 NC1274.2
018300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1274.2
018400-    "******************************".                            NC1274.2
018500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1274.2
018600     "NC127A".                                                    NC1274.2
018700 PROCEDURE DIVISION.                                              NC1274.2
018800 CCVS1 SECTION.                                                   NC1274.2
018900 OPEN-FILES.                                                      NC1274.2
019000     OPEN     OUTPUT PRINT-FILE.                                  NC1274.2
019100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1274.2
019200     MOVE    SPACE TO TEST-RESULTS.                               NC1274.2
019300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1274.2
019400     GO TO CCVS1-EXIT.                                            NC1274.2
019500 CLOSE-FILES.                                                     NC1274.2
019600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1274.2
019700 TERMINATE-CCVS.                                                  NC1274.2
019800*    EXIT PROGRAM.                                                NC1274.2
019900*TERMINATE-CALL.                                                  NC1274.2
020000     STOP     RUN.                                                NC1274.2
020100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1274.2
020200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1274.2
020300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1274.2
020400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1274.2
020500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1274.2
020600 PRINT-DETAIL.                                                    NC1274.2
020700     IF REC-CT NOT EQUAL TO ZERO                                  NC1274.2
020800             MOVE "." TO PARDOT-X                                 NC1274.2
020900             MOVE REC-CT TO DOTVALUE.                             NC1274.2
021000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1274.2
021100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1274.2
021200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1274.2
021300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1274.2
021400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1274.2
021500     MOVE SPACE TO CORRECT-X.                                     NC1274.2
021600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1274.2
021700     MOVE     SPACE TO RE-MARK.                                   NC1274.2
021800 HEAD-ROUTINE.                                                    NC1274.2
021900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1274.2
022000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1274.2
022100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1274.2
022200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1274.2
022300 COLUMN-NAMES-ROUTINE.                                            NC1274.2
022400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1274.2
022500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1274.2
022600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1274.2
022700 END-ROUTINE.                                                     NC1274.2
022800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1274.2
022900 END-RTN-EXIT.                                                    NC1274.2
023000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1274.2
023100 END-ROUTINE-1.                                                   NC1274.2
023200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1274.2
023300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1274.2
023400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1274.2
023500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1274.2
023600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1274.2
023700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1274.2
023800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1274.2
023900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1274.2
024000  END-ROUTINE-12.                                                 NC1274.2
024100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1274.2
024200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1274.2
024300         MOVE "NO " TO ERROR-TOTAL                                NC1274.2
024400         ELSE                                                     NC1274.2
024500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1274.2
024600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1274.2
024700     PERFORM WRITE-LINE.                                          NC1274.2
024800 END-ROUTINE-13.                                                  NC1274.2
024900     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1274.2
025000         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1274.2
025100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1274.2
025200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1274.2
025300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1274.2
025400      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1274.2
025500          MOVE "NO " TO ERROR-TOTAL                               NC1274.2
025600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1274.2
025700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1274.2
025800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1274.2
025900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1274.2
026000 WRITE-LINE.                                                      NC1274.2
026100     ADD 1 TO RECORD-COUNT.                                       NC1274.2
026200*    IF RECORD-COUNT GREATER 42                                   NC1274.2
026300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1274.2
026400*        MOVE SPACE TO DUMMY-RECORD                               NC1274.2
026500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1274.2
026600*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1274.2
026700*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1274.2
026800*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1274.2
026900*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1274.2
027000*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1274.2
027100*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1274.2
027200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1274.2
027300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1274.2
027400*        MOVE ZERO TO RECORD-COUNT.                               NC1274.2
027500     PERFORM WRT-LN.                                              NC1274.2
027600 WRT-LN.                                                          NC1274.2
027700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1274.2
027800     MOVE SPACE TO DUMMY-RECORD.                                  NC1274.2
027900 BLANK-LINE-PRINT.                                                NC1274.2
028000     PERFORM WRT-LN.                                              NC1274.2
028100 FAIL-ROUTINE.                                                    NC1274.2
028200     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1274.2
028300            GO TO FAIL-ROUTINE-WRITE.                             NC1274.2
028400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1274.2
028500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1274.2
028600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1274.2
028700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1274.2
028800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1274.2
028900     GO TO  FAIL-ROUTINE-EX.                                      NC1274.2
029000 FAIL-ROUTINE-WRITE.                                              NC1274.2
029100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1274.2
029200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1274.2
029300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1274.2
029400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1274.2
029500 FAIL-ROUTINE-EX. EXIT.                                           NC1274.2
029600 BAIL-OUT.                                                        NC1274.2
029700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1274.2
029800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1274.2
029900 BAIL-OUT-WRITE.                                                  NC1274.2
030000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1274.2
030100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1274.2
030200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1274.2
030300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1274.2
030400 BAIL-OUT-EX. EXIT.                                               NC1274.2
030500 CCVS1-EXIT.                                                      NC1274.2
030600     EXIT.                                                        NC1274.2
030700 sect-nc127a-001 section.                                         NC1274.2
030800 low-init-gf-1.                                                   NC1274.2
030900     move   "III-4 NOTE-2" to ansi-reference.                     NC1274.2
031000 low-test-gf-1-1.                                                 NC1274.2
031100     if      alphabetic-lit-upper not = alphabetic-lit-lower      NC1274.2
031200             perform pass                                         NC1274.2
031300             go to   low-write-gf-1.                              NC1274.2
031400     go to   low-fail-gf-1.                                       NC1274.2
031500 low-delete-gf-1.                                                 NC1274.2
031600     perform de-lete.                                             NC1274.2
031700     go to   low-write-GF-1.                                      NC1274.2
031800 low-fail-gf-1.                                                   NC1274.2
031900     move    alphabetic-lit-upper to correct-x.                   NC1274.2
032000     move    alphabetic-lit-lower to computed-x.                  NC1274.2
032100     move   "upper and lower case should not be equal"            NC1274.2
032200          to re-mark.                                             NC1274.2
032300     perform fail.                                                NC1274.2
032400 low-write-gf-1.                                                  NC1274.2
032500     move   "lower case program" to feature.                      NC1274.2
032600     MOVE   "low-test-gf-1" to par-name.                          NC1274.2
032700     perform print-detail.                                        NC1274.2
032800 low-init-gf-2.                                                   NC1274.2
032900     move   "vi-67 6.4.1" to ansi-reference.                      NC1274.2
033000 low-test-gf-2.                                                   NC1274.2
033100     if      alpha-lit-upper-lower = "dEfJkLuVw"                  NC1274.2
033200             perform pass                                         NC1274.2
033300             go to   low-write-gf-2.                              NC1274.2
033400     go to   low-fail-gf-2.                                       NC1274.2
033500 low-delete-gf-2.                                                 NC1274.2
033600     perform de-lete.                                             NC1274.2
033700     go to   low-write-GF-2.                                      NC1274.2
033800 low-fail-gf-2.                                                   NC1274.2
033900     move    alpha-lit-upper-lower to correct-x.                  NC1274.2
034000     move    alpha-lit-upper-lower to computed-x.                 NC1274.2
034100     move   "identical literals should be equal"                  NC1274.2
034200          to re-mark.                                             NC1274.2
034300     perform fail.                                                NC1274.2
034400 low-write-gf-2.                                                  NC1274.2
034500     MOVE   "low-test-gf-2" to par-name.                          NC1274.2
034600     perform print-detail.                                        NC1274.2
034700 CCVS-EXIT SECTION.                                               NC1274.2
034800 CCVS-999999.                                                     NC1274.2
034900     GO TO CLOSE-FILES.                                           NC1274.2
      *END-OF,NC127A                                                                  
