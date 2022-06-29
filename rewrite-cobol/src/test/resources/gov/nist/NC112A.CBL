      *HEADER,COBOL,NC112A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1124.2
000200 PROGRAM-ID.                                                      NC1124.2
000300     NC112A.                                                      NC1124.2
000400****************************************************************  NC1124.2
000500*                                                              *  NC1124.2
000600*    VALIDATION FOR:-                                          *  NC1124.2
000700*                                                              *  NC1124.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1124.2
000900*                                                              *  NC1124.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1124.2
001100*                                                              *  NC1124.2
001200****************************************************************  NC1124.2
001300*                                                              *  NC1124.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1124.2
001500*                                                              *  NC1124.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1124.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1124.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1124.2
001900*                                                              *  NC1124.2
002000****************************************************************  NC1124.2
002100*                                                                 NC1124.2
002200*    PROGRAM NC112A TESTS THE USE OF MULTIPLE OPERANDS WITH       NC1124.2
002300*           THE ADD, SUBTRACT AND MOVE STATEMENTS.                NC1124.2
002400*                                                                 NC1124.2
002500 ENVIRONMENT DIVISION.                                            NC1124.2
002600 CONFIGURATION SECTION.                                           NC1124.2
002700 SOURCE-COMPUTER.                                                 NC1124.2
002800     XXXXX082.                                                    NC1124.2
002900 OBJECT-COMPUTER.                                                 NC1124.2
003000     XXXXX083.                                                    NC1124.2
003100 INPUT-OUTPUT SECTION.                                            NC1124.2
003200 FILE-CONTROL.                                                    NC1124.2
003300     SELECT PRINT-FILE ASSIGN TO                                  NC1124.2
003400     XXXXX055.                                                    NC1124.2
003500 DATA DIVISION.                                                   NC1124.2
003600 FILE SECTION.                                                    NC1124.2
003700 FD  PRINT-FILE.                                                  NC1124.2
003800 01  PRINT-REC PICTURE X(120).                                    NC1124.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 NC1124.2
004000 WORKING-STORAGE SECTION.                                         NC1124.2
004100 77  ACCUM-1  PICTURE 9(17) VALUE ZERO.                           NC1124.2
004200 77  ACCUM-2   PICTURE 9(18)  VALUE ZERO.                         NC1124.2
004300 77  ACCUM-3   PICTURE 9V9(3)  VALUE 1.                           NC1124.2
004400 77  ACCUM-4   PICTURE 9V9(3)  VALUE ZERO.                        NC1124.2
004500 01  D-NAMES.                                                     NC1124.2
004600     02  DNAME-1                 PICTURE 9   VALUE 1.             NC1124.2
004700     02  DNAME-2                 PICTURE 9(3)  VALUE 1.           NC1124.2
004800     02  DNAME-3                 PICTURE 9(5)  VALUE 1.           NC1124.2
004900     02  DNAME-4                 PICTURE 9(7)  VALUE 1.           NC1124.2
005000     02  DNAME-5                 PICTURE 9(9)  VALUE 1.           NC1124.2
005100     02  DNAME-6                 PICTURE 9(11) VALUE 1.           NC1124.2
005200     02  DNAME-7                 PICTURE 9(13) VALUE 1.           NC1124.2
005300     02  DNAME-8                 PICTURE 9(15) VALUE 1.           NC1124.2
005400     02  DNAME-9                 PICTURE 9(17) VALUE 1.           NC1124.2
005500     02  DNAME-10                PICTURE 9(18) VALUE 1.           NC1124.2
005600 01  TEST-RESULTS.                                                NC1124.2
005700     02 FILLER                   PIC X      VALUE SPACE.          NC1124.2
005800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1124.2
005900     02 FILLER                   PIC X      VALUE SPACE.          NC1124.2
006000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1124.2
006100     02 FILLER                   PIC X      VALUE SPACE.          NC1124.2
006200     02  PAR-NAME.                                                NC1124.2
006300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1124.2
006400       03  PARDOT-X              PIC X      VALUE SPACE.          NC1124.2
006500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1124.2
006600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1124.2
006700     02 RE-MARK                  PIC X(61).                       NC1124.2
006800 01  TEST-COMPUTED.                                               NC1124.2
006900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1124.2
007000     02 FILLER                   PIC X(17)  VALUE                 NC1124.2
007100            "       COMPUTED=".                                   NC1124.2
007200     02 COMPUTED-X.                                               NC1124.2
007300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1124.2
007400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1124.2
007500                                 PIC -9(9).9(9).                  NC1124.2
007600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1124.2
007700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1124.2
007800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1124.2
007900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1124.2
008000         04 COMPUTED-18V0                    PIC -9(18).          NC1124.2
008100         04 FILLER                           PIC X.               NC1124.2
008200     03 FILLER PIC X(50) VALUE SPACE.                             NC1124.2
008300 01  TEST-CORRECT.                                                NC1124.2
008400     02 FILLER PIC X(30) VALUE SPACE.                             NC1124.2
008500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1124.2
008600     02 CORRECT-X.                                                NC1124.2
008700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1124.2
008800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1124.2
008900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1124.2
009000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1124.2
009100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1124.2
009200     03      CR-18V0 REDEFINES CORRECT-A.                         NC1124.2
009300         04 CORRECT-18V0                     PIC -9(18).          NC1124.2
009400         04 FILLER                           PIC X.               NC1124.2
009500     03 FILLER PIC X(2) VALUE SPACE.                              NC1124.2
009600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1124.2
009700 01  CCVS-C-1.                                                    NC1124.2
009800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1124.2
009900-    "SS  PARAGRAPH-NAME                                          NC1124.2
010000-    "       REMARKS".                                            NC1124.2
010100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1124.2
010200 01  CCVS-C-2.                                                    NC1124.2
010300     02 FILLER                     PIC X        VALUE SPACE.      NC1124.2
010400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1124.2
010500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1124.2
010600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1124.2
010700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1124.2
010800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1124.2
010900 01  REC-CT                        PIC 99       VALUE ZERO.       NC1124.2
011000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1124.2
011100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1124.2
011200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1124.2
011300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1124.2
011400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1124.2
011500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1124.2
011600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1124.2
011700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1124.2
011800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1124.2
011900 01  CCVS-H-1.                                                    NC1124.2
012000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1124.2
012100     02  FILLER                    PIC X(42)    VALUE             NC1124.2
012200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1124.2
012300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1124.2
012400 01  CCVS-H-2A.                                                   NC1124.2
012500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1124.2
012600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1124.2
012700   02  FILLER                        PIC XXXX   VALUE             NC1124.2
012800     "4.2 ".                                                      NC1124.2
012900   02  FILLER                        PIC X(28)  VALUE             NC1124.2
013000            " COPY - NOT FOR DISTRIBUTION".                       NC1124.2
013100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1124.2
013200                                                                  NC1124.2
013300 01  CCVS-H-2B.                                                   NC1124.2
013400   02  FILLER                        PIC X(15)  VALUE             NC1124.2
013500            "TEST RESULT OF ".                                    NC1124.2
013600   02  TEST-ID                       PIC X(9).                    NC1124.2
013700   02  FILLER                        PIC X(4)   VALUE             NC1124.2
013800            " IN ".                                               NC1124.2
013900   02  FILLER                        PIC X(12)  VALUE             NC1124.2
014000     " HIGH       ".                                              NC1124.2
014100   02  FILLER                        PIC X(22)  VALUE             NC1124.2
014200            " LEVEL VALIDATION FOR ".                             NC1124.2
014300   02  FILLER                        PIC X(58)  VALUE             NC1124.2
014400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1124.2
014500 01  CCVS-H-3.                                                    NC1124.2
014600     02  FILLER                      PIC X(34)  VALUE             NC1124.2
014700            " FOR OFFICIAL USE ONLY    ".                         NC1124.2
014800     02  FILLER                      PIC X(58)  VALUE             NC1124.2
014900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1124.2
015000     02  FILLER                      PIC X(28)  VALUE             NC1124.2
015100            "  COPYRIGHT   1985 ".                                NC1124.2
015200 01  CCVS-E-1.                                                    NC1124.2
015300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1124.2
015400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1124.2
015500     02 ID-AGAIN                     PIC X(9).                    NC1124.2
015600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1124.2
015700 01  CCVS-E-2.                                                    NC1124.2
015800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1124.2
015900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1124.2
016000     02 CCVS-E-2-2.                                               NC1124.2
016100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1124.2
016200         03 FILLER                   PIC X      VALUE SPACE.      NC1124.2
016300         03 ENDER-DESC               PIC X(44)  VALUE             NC1124.2
016400            "ERRORS ENCOUNTERED".                                 NC1124.2
016500 01  CCVS-E-3.                                                    NC1124.2
016600     02  FILLER                      PIC X(22)  VALUE             NC1124.2
016700            " FOR OFFICIAL USE ONLY".                             NC1124.2
016800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1124.2
016900     02  FILLER                      PIC X(58)  VALUE             NC1124.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1124.2
017100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1124.2
017200     02 FILLER                       PIC X(15)  VALUE             NC1124.2
017300             " COPYRIGHT 1985".                                   NC1124.2
017400 01  CCVS-E-4.                                                    NC1124.2
017500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1124.2
017600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1124.2
017700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1124.2
017800     02 FILLER                       PIC X(40)  VALUE             NC1124.2
017900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1124.2
018000 01  XXINFO.                                                      NC1124.2
018100     02 FILLER                       PIC X(19)  VALUE             NC1124.2
018200            "*** INFORMATION ***".                                NC1124.2
018300     02 INFO-TEXT.                                                NC1124.2
018400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1124.2
018500       04 XXCOMPUTED                 PIC X(20).                   NC1124.2
018600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1124.2
018700       04 XXCORRECT                  PIC X(20).                   NC1124.2
018800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1124.2
018900 01  HYPHEN-LINE.                                                 NC1124.2
019000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1124.2
019100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1124.2
019200-    "*****************************************".                 NC1124.2
019300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1124.2
019400-    "******************************".                            NC1124.2
019500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1124.2
019600     "NC112A".                                                    NC1124.2
019700 PROCEDURE DIVISION.                                              NC1124.2
019800 CCVS1 SECTION.                                                   NC1124.2
019900 OPEN-FILES.                                                      NC1124.2
020000     OPEN     OUTPUT PRINT-FILE.                                  NC1124.2
020100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1124.2
020200     MOVE    SPACE TO TEST-RESULTS.                               NC1124.2
020300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1124.2
020400     GO TO CCVS1-EXIT.                                            NC1124.2
020500 CLOSE-FILES.                                                     NC1124.2
020600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1124.2
020700 TERMINATE-CCVS.                                                  NC1124.2
020800*    EXIT PROGRAM.                                                NC1124.2
020900*TERMINATE-CALL.                                                  NC1124.2
021000     STOP     RUN.                                                NC1124.2
021100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1124.2
021200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1124.2
021300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1124.2
021400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1124.2
021500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1124.2
021600 PRINT-DETAIL.                                                    NC1124.2
021700     IF REC-CT NOT EQUAL TO ZERO                                  NC1124.2
021800             MOVE "." TO PARDOT-X                                 NC1124.2
021900             MOVE REC-CT TO DOTVALUE.                             NC1124.2
022000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1124.2
022100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1124.2
022200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1124.2
022300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1124.2
022400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1124.2
022500     MOVE SPACE TO CORRECT-X.                                     NC1124.2
022600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1124.2
022700     MOVE     SPACE TO RE-MARK.                                   NC1124.2
022800 HEAD-ROUTINE.                                                    NC1124.2
022900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1124.2
023000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1124.2
023100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1124.2
023200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1124.2
023300 COLUMN-NAMES-ROUTINE.                                            NC1124.2
023400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1124.2
023500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1124.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1124.2
023700 END-ROUTINE.                                                     NC1124.2
023800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1124.2
023900 END-RTN-EXIT.                                                    NC1124.2
024000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1124.2
024100 END-ROUTINE-1.                                                   NC1124.2
024200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1124.2
024300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1124.2
024400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1124.2
024500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1124.2
024600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1124.2
024700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1124.2
024800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1124.2
024900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1124.2
025000  END-ROUTINE-12.                                                 NC1124.2
025100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1124.2
025200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1124.2
025300         MOVE "NO " TO ERROR-TOTAL                                NC1124.2
025400         ELSE                                                     NC1124.2
025500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1124.2
025600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1124.2
025700     PERFORM WRITE-LINE.                                          NC1124.2
025800 END-ROUTINE-13.                                                  NC1124.2
025900     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1124.2
026000         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1124.2
026100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1124.2
026200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1124.2
026300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1124.2
026400      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1124.2
026500          MOVE "NO " TO ERROR-TOTAL                               NC1124.2
026600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1124.2
026700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1124.2
026800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1124.2
026900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1124.2
027000 WRITE-LINE.                                                      NC1124.2
027100     ADD 1 TO RECORD-COUNT.                                       NC1124.2
027200*    IF RECORD-COUNT GREATER 42                                   NC1124.2
027300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1124.2
027400*        MOVE SPACE TO DUMMY-RECORD                               NC1124.2
027500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1124.2
027600*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1124.2
027700*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1124.2
027800*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1124.2
027900*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1124.2
028000*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1124.2
028100*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1124.2
028200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1124.2
028300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1124.2
028400*        MOVE ZERO TO RECORD-COUNT.                               NC1124.2
028500     PERFORM WRT-LN.                                              NC1124.2
028600 WRT-LN.                                                          NC1124.2
028700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1124.2
028800     MOVE SPACE TO DUMMY-RECORD.                                  NC1124.2
028900 BLANK-LINE-PRINT.                                                NC1124.2
029000     PERFORM WRT-LN.                                              NC1124.2
029100 FAIL-ROUTINE.                                                    NC1124.2
029200     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1124.2
029300            GO TO FAIL-ROUTINE-WRITE.                             NC1124.2
029400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1124.2
029500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1124.2
029600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1124.2
029700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1124.2
029800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1124.2
029900     GO TO  FAIL-ROUTINE-EX.                                      NC1124.2
030000 FAIL-ROUTINE-WRITE.                                              NC1124.2
030100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1124.2
030200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1124.2
030300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1124.2
030400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1124.2
030500 FAIL-ROUTINE-EX. EXIT.                                           NC1124.2
030600 BAIL-OUT.                                                        NC1124.2
030700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1124.2
030800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1124.2
030900 BAIL-OUT-WRITE.                                                  NC1124.2
031000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1124.2
031100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1124.2
031200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1124.2
031300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1124.2
031400 BAIL-OUT-EX. EXIT.                                               NC1124.2
031500 CCVS1-EXIT.                                                      NC1124.2
031600     EXIT.                                                        NC1124.2
031700 SECT-NC112A-001 SECTION.                                         NC1124.2
031800 ADD-INIT-F1-1.                                                   NC1124.2
031900     MOVE   "V1-74 6.6.4 GR1,2" TO ANSI-REFERENCE.                NC1124.2
032000     MOVE    0 TO ACCUM-1.                                        NC1124.2
032100     MOVE    1 TO DNAME-1.                                        NC1124.2
032200     MOVE    1 TO DNAME-2.                                        NC1124.2
032300     MOVE    1 TO DNAME-3.                                        NC1124.2
032400     MOVE    1 TO DNAME-4.                                        NC1124.2
032500     MOVE    1 TO DNAME-5.                                        NC1124.2
032600     MOVE    1 TO DNAME-6.                                        NC1124.2
032700     MOVE    1 TO DNAME-7.                                        NC1124.2
032800     MOVE    1 TO DNAME-8.                                        NC1124.2
032900     MOVE    1 TO DNAME-9.                                        NC1124.2
033000     MOVE    1 TO DNAME-10.                                       NC1124.2
033100 ADD-TEST-F1-1-0.                                                 NC1124.2
033200     ADD DNAME-1                                                  NC1124.2
033300         DNAME-2                                                  NC1124.2
033400         DNAME-3                                                  NC1124.2
033500         DNAME-4                                                  NC1124.2
033600         DNAME-5                                                  NC1124.2
033700         DNAME-6                                                  NC1124.2
033800         DNAME-7                                                  NC1124.2
033900         DNAME-8                                                  NC1124.2
034000         DNAME-9                                                  NC1124.2
034100         DNAME-10 TO ACCUM-1.                                     NC1124.2
034200 ADD-TEST-F1-1-1.                                                 NC1124.2
034300     IF ACCUM-1 EQUAL TO 10                                       NC1124.2
034400         PERFORM PASS                                             NC1124.2
034500         GO TO ADD-WRITE-F1-1.                                    NC1124.2
034600     MOVE ACCUM-1 TO COMPUTED-A.                                  NC1124.2
034700     MOVE 10 TO CORRECT-A.                                        NC1124.2
034800     PERFORM FAIL.                                                NC1124.2
034900     GO TO ADD-WRITE-F1-1.                                        NC1124.2
035000 ADD-DELETE-F1-1.                                                 NC1124.2
035100     PERFORM DE-LETE.                                             NC1124.2
035200 ADD-WRITE-F1-1.                                                  NC1124.2
035300     MOVE "ADD LIMITS TESTS" TO FEATURE.                          NC1124.2
035400     MOVE "ADD-TEST-F1-1" TO PAR-NAME.                            NC1124.2
035500     PERFORM PRINT-DETAIL.                                        NC1124.2
035600 ADD-INIT-F1-2.                                                   NC1124.2
035700     MOVE   "V1-74 6.6.4 GR1,2" TO ANSI-REFERENCE.                NC1124.2
035800     MOVE    0 TO ACCUM-1.                                        NC1124.2
035900     MOVE    1 TO DNAME-1.                                        NC1124.2
036000     MOVE    1 TO DNAME-2.                                        NC1124.2
036100     MOVE    1 TO DNAME-3.                                        NC1124.2
036200     MOVE    1 TO DNAME-4.                                        NC1124.2
036300     MOVE    1 TO DNAME-5.                                        NC1124.2
036400     MOVE    1 TO DNAME-6.                                        NC1124.2
036500     MOVE    1 TO DNAME-7.                                        NC1124.2
036600     MOVE    1 TO DNAME-8.                                        NC1124.2
036700     MOVE    1 TO DNAME-9.                                        NC1124.2
036800     MOVE    1 TO DNAME-10.                                       NC1124.2
036900 ADD-TEST-F1-2-0.                                                 NC1124.2
037000     ADD DNAME-1                                                  NC1124.2
037100         DNAME-2                                                  NC1124.2
037200         DNAME-3                                                  NC1124.2
037300         DNAME-4                                                  NC1124.2
037400         DNAME-5                                                  NC1124.2
037500         DNAME-6                                                  NC1124.2
037600         DNAME-7                                                  NC1124.2
037700         DNAME-8                                                  NC1124.2
037800         DNAME-9                                                  NC1124.2
037900         DNAME-10 TO ACCUM-1 ROUNDED ON SIZE ERROR                NC1124.2
038000             MOVE 0 TO ACCUM-1.                                   NC1124.2
038100 ADD-TEST-F1-2-1.                                                 NC1124.2
038200     IF ACCUM-1 EQUAL TO 10                                       NC1124.2
038300         PERFORM PASS                                             NC1124.2
038400         GO TO ADD-WRITE-F1-2.                                    NC1124.2
038500     MOVE ACCUM-1 TO COMPUTED-A.                                  NC1124.2
038600     MOVE 10 TO CORRECT-A.                                        NC1124.2
038700     PERFORM FAIL.                                                NC1124.2
038800     GO TO ADD-WRITE-F1-2.                                        NC1124.2
038900 ADD-DELETE-F1-2.                                                 NC1124.2
039000     PERFORM DE-LETE.                                             NC1124.2
039100 ADD-WRITE-F1-2.                                                  NC1124.2
039200     MOVE "ADD-TEST-F1-2" TO PAR-NAME.                            NC1124.2
039300     PERFORM PRINT-DETAIL.                                        NC1124.2
039400 ADD-INIT-F2-1.                                                   NC1124.2
039500     MOVE   "V1-74 6.6.4 GR1,2" TO ANSI-REFERENCE.                NC1124.2
039600     MOVE    0 TO ACCUM-1.                                        NC1124.2
039700     MOVE    1 TO DNAME-1.                                        NC1124.2
039800     MOVE    1 TO DNAME-2.                                        NC1124.2
039900     MOVE    1 TO DNAME-3.                                        NC1124.2
040000     MOVE    1 TO DNAME-4.                                        NC1124.2
040100     MOVE    1 TO DNAME-5.                                        NC1124.2
040200     MOVE    1 TO DNAME-6.                                        NC1124.2
040300     MOVE    1 TO DNAME-7.                                        NC1124.2
040400     MOVE    1 TO DNAME-8.                                        NC1124.2
040500     MOVE    1 TO DNAME-9.                                        NC1124.2
040600     MOVE    1 TO DNAME-10.                                       NC1124.2
040700 ADD-TEST-F2-1-0.                                                 NC1124.2
040800     ADD DNAME-1                                                  NC1124.2
040900         DNAME-2                                                  NC1124.2
041000         DNAME-3                                                  NC1124.2
041100         DNAME-4                                                  NC1124.2
041200         DNAME-5                                                  NC1124.2
041300         DNAME-6                                                  NC1124.2
041400         DNAME-7                                                  NC1124.2
041500         DNAME-8                                                  NC1124.2
041600         DNAME-9                                                  NC1124.2
041700         DNAME-10  GIVING ACCUM-1.                                NC1124.2
041800 ADD-TEST-F2-1-1.                                                 NC1124.2
041900     IF ACCUM-1 EQUAL TO 10                                       NC1124.2
042000         PERFORM PASS                                             NC1124.2
042100         GO TO ADD-WRITE-F2-1.                                    NC1124.2
042200     MOVE ACCUM-1 TO COMPUTED-A.                                  NC1124.2
042300     MOVE 10 TO CORRECT-A.                                        NC1124.2
042400     PERFORM FAIL.                                                NC1124.2
042500     GO TO ADD-WRITE-F2-1.                                        NC1124.2
042600 ADD-DELETE-F2-1.                                                 NC1124.2
042700     PERFORM DE-LETE.                                             NC1124.2
042800 ADD-WRITE-F2-1.                                                  NC1124.2
042900     MOVE "ADD-TEST-F2-1" TO PAR-NAME.                            NC1124.2
043000     PERFORM PRINT-DETAIL.                                        NC1124.2
043100 ADD-INIT-F2-2.                                                   NC1124.2
043200     MOVE   "V1-74 6.6.4 GR1,2" TO ANSI-REFERENCE.                NC1124.2
043300     MOVE    1 TO ACCUM-1.                                        NC1124.2
043400     MOVE    1 TO DNAME-1.                                        NC1124.2
043500     MOVE    1 TO DNAME-2.                                        NC1124.2
043600     MOVE    1 TO DNAME-3.                                        NC1124.2
043700     MOVE    1 TO DNAME-4.                                        NC1124.2
043800     MOVE    1 TO DNAME-5.                                        NC1124.2
043900     MOVE    1 TO DNAME-6.                                        NC1124.2
044000     MOVE    1 TO DNAME-7.                                        NC1124.2
044100     MOVE    1 TO DNAME-8.                                        NC1124.2
044200     MOVE    1 TO DNAME-9.                                        NC1124.2
044300     MOVE    1 TO DNAME-10.                                       NC1124.2
044400 ADD-TEST-F2-2-0.                                                 NC1124.2
044500     ADD DNAME-1                                                  NC1124.2
044600         DNAME-2                                                  NC1124.2
044700         DNAME-3                                                  NC1124.2
044800         DNAME-4                                                  NC1124.2
044900         DNAME-5                                                  NC1124.2
045000         DNAME-6                                                  NC1124.2
045100         DNAME-7                                                  NC1124.2
045200         DNAME-8                                                  NC1124.2
045300         DNAME-9                                                  NC1124.2
045400         DNAME-10 GIVING ACCUM-1 ROUNDED  ON SIZE ERROR           NC1124.2
045500         MOVE 0 TO ACCUM-1.                                       NC1124.2
045600 ADD-TEST-F2-2-1.                                                 NC1124.2
045700     IF ACCUM-1 EQUAL TO 10                                       NC1124.2
045800         PERFORM PASS                                             NC1124.2
045900         GO TO ADD-WRITE-F2-2.                                    NC1124.2
046000     MOVE ACCUM-1 TO COMPUTED-A.                                  NC1124.2
046100     MOVE 10 TO CORRECT-A.                                        NC1124.2
046200     PERFORM FAIL.                                                NC1124.2
046300     GO TO ADD-WRITE-F2-2.                                        NC1124.2
046400 ADD-DELETE-F2-2.                                                 NC1124.2
046500     PERFORM DE-LETE.                                             NC1124.2
046600 ADD-WRITE-F2-2.                                                  NC1124.2
046700     MOVE "ADD-TEST-F2-2" TO PAR-NAME.                            NC1124.2
046800     PERFORM PRINT-DETAIL.                                        NC1124.2
046900 ADD-INIT-F1-3.                                                   NC1124.2
047000     MOVE   "V1-74 6.6.4 GR1,2" TO ANSI-REFERENCE.                NC1124.2
047100     MOVE    1 TO DNAME-1.                                        NC1124.2
047200     MOVE    1 TO DNAME-2.                                        NC1124.2
047300     MOVE    1 TO DNAME-3.                                        NC1124.2
047400     MOVE    1 TO DNAME-4.                                        NC1124.2
047500     MOVE    1 TO DNAME-5.                                        NC1124.2
047600     MOVE    1 TO DNAME-6.                                        NC1124.2
047700     MOVE    1 TO DNAME-7.                                        NC1124.2
047800     MOVE    1 TO DNAME-8.                                        NC1124.2
047900     MOVE    1 TO DNAME-9.                                        NC1124.2
048000     MOVE    1 TO DNAME-10.                                       NC1124.2
048100 ADD-TEST-F1-3-0.                                                 NC1124.2
048200     ADD DNAME-2                                                  NC1124.2
048300         DNAME-3                                                  NC1124.2
048400         DNAME-4                                                  NC1124.2
048500         DNAME-5                                                  NC1124.2
048600         DNAME-6                                                  NC1124.2
048700         DNAME-7                                                  NC1124.2
048800         DNAME-8                                                  NC1124.2
048900         DNAME-9                                                  NC1124.2
049000         DNAME-10 TO DNAME-1                                      NC1124.2
049100      ON SIZE ERROR                                               NC1124.2
049200         PERFORM PASS                                             NC1124.2
049300         GO TO   ADD-WRITE-F1-3.                                  NC1124.2
049400     MOVE "SIZE ERROR EXPECTED" TO RE-MARK.                       NC1124.2
049500     MOVE DNAME-1 TO COMPUTED-A.                                  NC1124.2
049600     MOVE "UNCHANGED (STILL 1)" TO CORRECT-A.                     NC1124.2
049700     PERFORM FAIL.                                                NC1124.2
049800     GO TO ADD-WRITE-F1-3.                                        NC1124.2
049900 ADD-DELETE-F1-3.                                                 NC1124.2
050000     PERFORM DE-LETE.                                             NC1124.2
050100 ADD-WRITE-F1-3.                                                  NC1124.2
050200     MOVE "ADD-TEST-F1-3" TO PAR-NAME.                            NC1124.2
050300     PERFORM PRINT-DETAIL.                                        NC1124.2
050400 ADD-INIT-F2-3.                                                   NC1124.2
050500     MOVE   "V1-74 6.6.4 GR1,2" TO ANSI-REFERENCE.                NC1124.2
050600     MOVE    1 TO DNAME-1.                                        NC1124.2
050700     MOVE    1 TO DNAME-2.                                        NC1124.2
050800     MOVE    1 TO DNAME-3.                                        NC1124.2
050900     MOVE    1 TO DNAME-4.                                        NC1124.2
051000     MOVE    1 TO DNAME-5.                                        NC1124.2
051100     MOVE    1 TO DNAME-6.                                        NC1124.2
051200     MOVE    1 TO DNAME-7.                                        NC1124.2
051300     MOVE    1 TO DNAME-8.                                        NC1124.2
051400     MOVE    1 TO DNAME-9.                                        NC1124.2
051500     MOVE    1 TO DNAME-10.                                       NC1124.2
051600 ADD-TEST-F2-3-0.                                                 NC1124.2
051700     ADD DNAME-1                                                  NC1124.2
051800         DNAME-2                                                  NC1124.2
051900         DNAME-3                                                  NC1124.2
052000         DNAME-4                                                  NC1124.2
052100         DNAME-5                                                  NC1124.2
052200         DNAME-6                                                  NC1124.2
052300         DNAME-7                                                  NC1124.2
052400         DNAME-8                                                  NC1124.2
052500         DNAME-9                                                  NC1124.2
052600         DNAME-10  GIVING DNAME-1 ON SIZE ERROR                   NC1124.2
052700         PERFORM PASS                                             NC1124.2
052800         GO TO ADD-WRITE-F2-3.                                    NC1124.2
052900     MOVE "SIZE ERROR EXPECTED" TO RE-MARK.                       NC1124.2
053000     MOVE "UNCHANGED (STILL 1)" TO CORRECT-A.                     NC1124.2
053100     MOVE DNAME-1 TO COMPUTED-A.                                  NC1124.2
053200     PERFORM FAIL.                                                NC1124.2
053300     GO TO ADD-WRITE-F2-3.                                        NC1124.2
053400 ADD-DELETE-F2-3.                                                 NC1124.2
053500     PERFORM DE-LETE.                                             NC1124.2
053600 ADD-WRITE-F2-3.                                                  NC1124.2
053700     MOVE "ADD-TEST-F2-3" TO PAR-NAME.                            NC1124.2
053800     PERFORM PRINT-DETAIL.                                        NC1124.2
053900 SUB-INIT-F1-1.                                                   NC1124.2
054000     MOVE   "V1-134 6.25.4 GR1,2" TO ANSI-REFERENCE.              NC1124.2
054100     MOVE    1 TO DNAME-1.                                        NC1124.2
054200     MOVE    1 TO DNAME-2.                                        NC1124.2
054300     MOVE    1 TO DNAME-3.                                        NC1124.2
054400     MOVE    1 TO DNAME-4.                                        NC1124.2
054500     MOVE    1 TO DNAME-5.                                        NC1124.2
054600     MOVE    1 TO DNAME-6.                                        NC1124.2
054700     MOVE    1 TO DNAME-7.                                        NC1124.2
054800     MOVE    1 TO DNAME-8.                                        NC1124.2
054900     MOVE    1 TO DNAME-9.                                        NC1124.2
055000     MOVE    1 TO DNAME-10.                                       NC1124.2
055100     MOVE   10 TO ACCUM-1.                                        NC1124.2
055200 SUB-TEST-F1-1-0.                                                 NC1124.2
055300     SUBTRACT DNAME-1                                             NC1124.2
055400              DNAME-2                                             NC1124.2
055500              DNAME-3                                             NC1124.2
055600              DNAME-4                                             NC1124.2
055700              DNAME-5                                             NC1124.2
055800              DNAME-6                                             NC1124.2
055900              DNAME-7                                             NC1124.2
056000              DNAME-8                                             NC1124.2
056100              DNAME-9                                             NC1124.2
056200              DNAME-10 FROM ACCUM-1.                              NC1124.2
056300 SUB-TEST-F1-1-1.                                                 NC1124.2
056400     IF ACCUM-1 EQUAL TO ZERO                                     NC1124.2
056500         PERFORM PASS                                             NC1124.2
056600         GO TO SUB-WRITE-F1-1.                                    NC1124.2
056700     MOVE 0 TO CORRECT-A.                                         NC1124.2
056800     MOVE ACCUM-1 TO COMPUTED-A.                                  NC1124.2
056900     PERFORM FAIL.                                                NC1124.2
057000     GO TO SUB-WRITE-F1-1.                                        NC1124.2
057100 SUB-DELETE-F1-1.                                                 NC1124.2
057200     PERFORM DE-LETE.                                             NC1124.2
057300 SUB-WRITE-F1-1.                                                  NC1124.2
057400     MOVE "SUBTRACT LIMITS" TO FEATURE.                           NC1124.2
057500     PERFORM END-ROUTINE.                                         NC1124.2
057600     MOVE "SUB-TEST-F1-1" TO PAR-NAME.                            NC1124.2
057700     PERFORM PRINT-DETAIL.                                        NC1124.2
057800 SUB-INIT-F2-1.                                                   NC1124.2
057900     MOVE   "V1-134 6.25.4 GR1,2" TO ANSI-REFERENCE.              NC1124.2
058000     MOVE    1 TO DNAME-1.                                        NC1124.2
058100     MOVE    1 TO DNAME-2.                                        NC1124.2
058200     MOVE    1 TO DNAME-3.                                        NC1124.2
058300     MOVE    1 TO DNAME-4.                                        NC1124.2
058400     MOVE    1 TO DNAME-5.                                        NC1124.2
058500     MOVE    1 TO DNAME-6.                                        NC1124.2
058600     MOVE    1 TO DNAME-7.                                        NC1124.2
058700     MOVE    1 TO DNAME-8.                                        NC1124.2
058800     MOVE    1 TO DNAME-9.                                        NC1124.2
058900     MOVE    1 TO DNAME-10.                                       NC1124.2
059000     MOVE   10 TO ACCUM-1.                                        NC1124.2
059100 SUB-TEST-F2-1-0.                                                 NC1124.2
059200     SUBTRACT DNAME-1                                             NC1124.2
059300              DNAME-2                                             NC1124.2
059400              DNAME-3                                             NC1124.2
059500              DNAME-4                                             NC1124.2
059600              DNAME-5                                             NC1124.2
059700              DNAME-6                                             NC1124.2
059800              DNAME-7                                             NC1124.2
059900              DNAME-8                                             NC1124.2
060000              DNAME-9                                             NC1124.2
060100              DNAME-10 FROM ACCUM-1 GIVING ACCUM-1.               NC1124.2
060200 SUB-TEST-F2-1-1.                                                 NC1124.2
060300     IF ACCUM-1 EQUAL TO 0                                        NC1124.2
060400         PERFORM PASS                                             NC1124.2
060500         GO TO SUB-WRITE-F2-1.                                    NC1124.2
060600     MOVE ACCUM-1 TO COMPUTED-A.                                  NC1124.2
060700     MOVE 0 TO CORRECT-A.                                         NC1124.2
060800     PERFORM FAIL.                                                NC1124.2
060900     GO TO SUB-WRITE-F2-1.                                        NC1124.2
061000 SUB-DELETE-F2-1.                                                 NC1124.2
061100     PERFORM DE-LETE.                                             NC1124.2
061200 SUB-WRITE-F2-1.                                                  NC1124.2
061300     MOVE "SUB-TEST-F2-1" TO PAR-NAME.                            NC1124.2
061400     PERFORM PRINT-DETAIL.                                        NC1124.2
061500 SUB-INIT-F2-2.                                                   NC1124.2
061600     MOVE   "V1-134 6.25.4 GR1,2" TO ANSI-REFERENCE.              NC1124.2
061700     MOVE    1 TO DNAME-1.                                        NC1124.2
061800     MOVE    1 TO DNAME-2.                                        NC1124.2
061900     MOVE    1 TO DNAME-3.                                        NC1124.2
062000     MOVE    1 TO DNAME-4.                                        NC1124.2
062100     MOVE    1 TO DNAME-5.                                        NC1124.2
062200     MOVE    1 TO DNAME-6.                                        NC1124.2
062300     MOVE    1 TO DNAME-7.                                        NC1124.2
062400     MOVE    1 TO DNAME-8.                                        NC1124.2
062500     MOVE    1 TO DNAME-9.                                        NC1124.2
062600     MOVE    1 TO DNAME-10.                                       NC1124.2
062700 SUB-TEST-F2-2-0.                                                 NC1124.2
062800     SUBTRACT DNAME-2                                             NC1124.2
062900              DNAME-3                                             NC1124.2
063000              DNAME-4                                             NC1124.2
063100              DNAME-5                                             NC1124.2
063200              DNAME-6                                             NC1124.2
063300              DNAME-7                                             NC1124.2
063400              DNAME-8                                             NC1124.2
063500              DNAME-9                                             NC1124.2
063600              DNAME-10 FROM 100 GIVING DNAME-1 ON SIZE ERROR      NC1124.2
063700         PERFORM PASS                                             NC1124.2
063800         GO TO SUB-WRITE-F2-2.                                    NC1124.2
063900     MOVE "UNCHANGED (STILL 1)" TO CORRECT-A.                     NC1124.2
064000     MOVE DNAME-1 TO COMPUTED-A.                                  NC1124.2
064100     PERFORM FAIL.                                                NC1124.2
064200     MOVE "SIZE ERROR EXPECTED" TO RE-MARK.                       NC1124.2
064300     GO TO SUB-WRITE-F2-2.                                        NC1124.2
064400 SUB-DELETE-F2-2.                                                 NC1124.2
064500     PERFORM DE-LETE.                                             NC1124.2
064600 SUB-WRITE-F2-2.                                                  NC1124.2
064700     MOVE "SUB-TEST-F2-2" TO PAR-NAME.                            NC1124.2
064800     PERFORM PRINT-DETAIL.                                        NC1124.2
064900 SUB-INIT-F2-3.                                                   NC1124.2
065000     MOVE   "V1-134 6.25.4 GR1,2" TO ANSI-REFERENCE.              NC1124.2
065100     MOVE    1 TO DNAME-1.                                        NC1124.2
065200     MOVE  100 TO ACCUM-1.                                        NC1124.2
065300 SUB-TEST-F2-3.                                                   NC1124.2
065400     SUBTRACT DNAME-1                                             NC1124.2
065500              1                                                   NC1124.2
065600              DNAME-1                                             NC1124.2
065700              DNAME-1                                             NC1124.2
065800              DNAME-1                                             NC1124.2
065900              1                                                   NC1124.2
066000              -1                                                  NC1124.2
066100              1                                                   NC1124.2
066200              1        FROM 7 GIVING ACCUM-1.                     NC1124.2
066300     IF ACCUM-1 EQUAL TO 0                                        NC1124.2
066400         PERFORM PASS                                             NC1124.2
066500         GO TO SUB-WRITE-F2-3.                                    NC1124.2
066600     MOVE 0 TO CORRECT-A.                                         NC1124.2
066700     MOVE ACCUM-1 TO COMPUTED-A.                                  NC1124.2
066800     PERFORM FAIL.                                                NC1124.2
066900     GO TO SUB-WRITE-F2-3.                                        NC1124.2
067000 SUB-DELETE-F2-3.                                                 NC1124.2
067100     PERFORM DE-LETE.                                             NC1124.2
067200 SUB-WRITE-F2-3.                                                  NC1124.2
067300     MOVE "SUB-TEST-F2-3" TO PAR-NAME.                            NC1124.2
067400     PERFORM PRINT-DETAIL.                                        NC1124.2
067500 SUB-INIT-F1-2.                                                   NC1124.2
067600     MOVE   "V1-134 6.25.4 GR1,2" TO ANSI-REFERENCE.              NC1124.2
067700     MOVE    1 TO DNAME-1.                                        NC1124.2
067800     MOVE   10 TO ACCUM-1.                                        NC1124.2
067900     MOVE    1 TO ACCUM-3.                                        NC1124.2
068000 SUB-TEST-F1-2-0.                                                 NC1124.2
068100     SUBTRACT DNAME-1                                             NC1124.2
068200                    .5                                            NC1124.2
068300                    .5                                            NC1124.2
068400                    .5                                            NC1124.2
068500                    .5                                            NC1124.2
068600                    .5                                            NC1124.2
068700                    .5                                            NC1124.2
068800                    .5                                            NC1124.2
068900                    .5                                            NC1124.2
069000                    .5 FROM ACCUM-1 ROUNDED.                      NC1124.2
069100 SUB-TEST-F1-2-1.                                                 NC1124.2
069200     IF ACCUM-1 EQUAL TO 5                                        NC1124.2
069300         PERFORM PASS                                             NC1124.2
069400         GO TO SUB-WRITE-F1-2.                                    NC1124.2
069500     MOVE ACCUM-1 TO COMPUTED-A.                                  NC1124.2
069600     MOVE 5 TO CORRECT-A.                                         NC1124.2
069700     PERFORM FAIL.                                                NC1124.2
069800     GO TO SUB-WRITE-F1-2.                                        NC1124.2
069900 SUB-DELETE-F1-2.                                                 NC1124.2
070000     PERFORM DE-LETE.                                             NC1124.2
070100 SUB-WRITE-F1-2.                                                  NC1124.2
070200     MOVE "SUB-TEST-F1-2" TO PAR-NAME.                            NC1124.2
070300     PERFORM PRINT-DETAIL.                                        NC1124.2
070400 SUB-INIT-F2-4.                                                   NC1124.2
070500     MOVE   "V1-134 6.25.4 GR1,2" TO ANSI-REFERENCE.              NC1124.2
070600     MOVE    1 TO DNAME-1.                                        NC1124.2
070700     MOVE   10 TO ACCUM-1.                                        NC1124.2
070800     MOVE    1 TO ACCUM-2.                                        NC1124.2
070900 SUB-TEST-F2-4-0.                                                 NC1124.2
071000     SUBTRACT DNAME-1                                             NC1124.2
071100                    .5                                            NC1124.2
071200                    .5                                            NC1124.2
071300                    .5                                            NC1124.2
071400                    .5                                            NC1124.2
071500                    .5                                            NC1124.2
071600                    .5                                            NC1124.2
071700                    .5                                            NC1124.2
071800                    .5                                            NC1124.2
071900                    .5 FROM ACCUM-1 GIVING ACCUM-2 ROUNDED.       NC1124.2
072000 SUB-TEST-F2-4-1.                                                 NC1124.2
072100     IF ACCUM-2 EQUAL TO 5                                        NC1124.2
072200         PERFORM PASS                                             NC1124.2
072300         GO TO SUB-WRITE-F2-4.                                    NC1124.2
072400     MOVE ACCUM-2 TO COMPUTED-A.                                  NC1124.2
072500     MOVE 5 TO CORRECT-A.                                         NC1124.2
072600     PERFORM FAIL.                                                NC1124.2
072700     GO TO SUB-WRITE-F2-4.                                        NC1124.2
072800 SUB-DELETE-F2-4.                                                 NC1124.2
072900     PERFORM DE-LETE.                                             NC1124.2
073000 SUB-WRITE-F2-4.                                                  NC1124.2
073100     MOVE "SUB-TEST-F2-4" TO PAR-NAME.                            NC1124.2
073200     PERFORM PRINT-DETAIL.                                        NC1124.2
073300 MOVE-INIT-F1-1-1.                                                NC1124.2
073400     MOVE   "V1-134 6.25.4 GR1,2" TO ANSI-REFERENCE.              NC1124.2
073500     MOVE    ZERO TO D-NAMES.                                     NC1124.2
073600     MOVE    1 TO ACCUM-3.                                        NC1124.2
073700 MOVE-TEST-F1-1-1.                                                NC1124.2
073800     MOVE ACCUM-3 TO                                              NC1124.2
073900             DNAME-1                                              NC1124.2
074000             DNAME-2                                              NC1124.2
074100             DNAME-3                                              NC1124.2
074200             DNAME-4                                              NC1124.2
074300             DNAME-5                                              NC1124.2
074400             DNAME-6                                              NC1124.2
074500             DNAME-7                                              NC1124.2
074600             DNAME-8                                              NC1124.2
074700             DNAME-9                                              NC1124.2
074800             DNAME-10.                                            NC1124.2
074900 MOVE-TEST-F1-1-1-1.                                              NC1124.2
075000     IF DNAME-1 EQUAL TO 1                                        NC1124.2
075100         PERFORM PASS                                             NC1124.2
075200         GO TO MOVE-WRITE-F1-1-1.                                 NC1124.2
075300     MOVE 1 TO CORRECT-N.                                         NC1124.2
075400     MOVE DNAME-1 TO COMPUTED-N.                                  NC1124.2
075500     PERFORM FAIL.                                                NC1124.2
075600     GO TO MOVE-WRITE-F1-1-1.                                     NC1124.2
075700 MOVE-DELETE-F1-1-1.                                              NC1124.2
075800     PERFORM DE-LETE.                                             NC1124.2
075900 MOVE-WRITE-F1-1-1.                                               NC1124.2
076000     MOVE "MOVE LIMITS TESTS" TO FEATURE.                         NC1124.2
076100     PERFORM END-ROUTINE.                                         NC1124.2
076200     MOVE "MOVE-TEST-F1-1-1" TO PAR-NAME.                         NC1124.2
076300     PERFORM PRINT-DETAIL.                                        NC1124.2
076400 MOVE-TEST-F1-1-2.                                                NC1124.2
076500     IF DNAME-2 EQUAL TO 1                                        NC1124.2
076600         PERFORM PASS                                             NC1124.2
076700         GO TO MOVE-WRITE-F1-1-2.                                 NC1124.2
076800     MOVE 1 TO CORRECT-N.                                         NC1124.2
076900     MOVE DNAME-2 TO COMPUTED-N.                                  NC1124.2
077000     PERFORM FAIL.                                                NC1124.2
077100     GO TO MOVE-WRITE-F1-1-2.                                     NC1124.2
077200 MOVE-DELETE-F1-1-2.                                              NC1124.2
077300     PERFORM DE-LETE.                                             NC1124.2
077400 MOVE-WRITE-F1-1-2.                                               NC1124.2
077500     MOVE "MOVE-TEST-F1-1-2" TO PAR-NAME.                         NC1124.2
077600     PERFORM PRINT-DETAIL.                                        NC1124.2
077700 MOVE-TEST-F1-1-3.                                                NC1124.2
077800     IF DNAME-3 EQUAL TO 1                                        NC1124.2
077900         PERFORM PASS                                             NC1124.2
078000         GO TO MOVE-WRITE-F1-1-3.                                 NC1124.2
078100     MOVE 1 TO CORRECT-N.                                         NC1124.2
078200     MOVE DNAME-3 TO COMPUTED-N.                                  NC1124.2
078300     PERFORM FAIL.                                                NC1124.2
078400     GO TO MOVE-WRITE-F1-1-3.                                     NC1124.2
078500 MOVE-DELETE-F1-1-3.                                              NC1124.2
078600     PERFORM DE-LETE.                                             NC1124.2
078700 MOVE-WRITE-F1-1-3.                                               NC1124.2
078800     MOVE "MOVE-TEST-F1-1-3" TO PAR-NAME.                         NC1124.2
078900     PERFORM PRINT-DETAIL.                                        NC1124.2
079000 MOVE-TEST-F1-1-4.                                                NC1124.2
079100     IF DNAME-4 EQUAL TO 1                                        NC1124.2
079200         PERFORM PASS                                             NC1124.2
079300         GO TO MOVE-WRITE-F1-1-4.                                 NC1124.2
079400     MOVE 1 TO CORRECT-N.                                         NC1124.2
079500     MOVE DNAME-4 TO COMPUTED-N.                                  NC1124.2
079600     PERFORM FAIL.                                                NC1124.2
079700     GO TO MOVE-WRITE-F1-1-4.                                     NC1124.2
079800 MOVE-DELETE-F1-1-4.                                              NC1124.2
079900     PERFORM DE-LETE.                                             NC1124.2
080000 MOVE-WRITE-F1-1-4.                                               NC1124.2
080100     MOVE "MOVE-TEST-F1-1-4" TO PAR-NAME.                         NC1124.2
080200     PERFORM PRINT-DETAIL.                                        NC1124.2
080300 MOVE-TEST-F1-1-5.                                                NC1124.2
080400     IF DNAME-5  EQUAL TO 1                                       NC1124.2
080500         PERFORM PASS                                             NC1124.2
080600         GO TO MOVE-WRITE-F1-1-5.                                 NC1124.2
080700     MOVE 1 TO CORRECT-N.                                         NC1124.2
080800     MOVE DNAME-5  TO COMPUTED-N.                                 NC1124.2
080900     PERFORM FAIL.                                                NC1124.2
081000     GO TO MOVE-WRITE-F1-1-5.                                     NC1124.2
081100 MOVE-DELETE-F1-1-5.                                              NC1124.2
081200     PERFORM DE-LETE.                                             NC1124.2
081300 MOVE-WRITE-F1-1-5.                                               NC1124.2
081400     MOVE "MOVE-TEST-F1-1-5 " TO PAR-NAME.                        NC1124.2
081500     PERFORM PRINT-DETAIL.                                        NC1124.2
081600 MOVE-TEST-F1-1-6.                                                NC1124.2
081700     IF DNAME-6  EQUAL TO 1                                       NC1124.2
081800         PERFORM PASS                                             NC1124.2
081900         GO TO MOVE-WRITE-F1-1-6.                                 NC1124.2
082000     MOVE 1 TO CORRECT-N.                                         NC1124.2
082100     MOVE DNAME-6  TO COMPUTED-N.                                 NC1124.2
082200     PERFORM FAIL.                                                NC1124.2
082300     GO TO MOVE-WRITE-F1-1-6.                                     NC1124.2
082400 MOVE-DELETE-F1-1-6.                                              NC1124.2
082500     PERFORM DE-LETE.                                             NC1124.2
082600 MOVE-WRITE-F1-1-6.                                               NC1124.2
082700     MOVE "MOVE-TEST-F1-1-6 " TO PAR-NAME.                        NC1124.2
082800     PERFORM PRINT-DETAIL.                                        NC1124.2
082900 MOVE-TEST-F1-1-7.                                                NC1124.2
083000     IF DNAME-7  EQUAL TO 1                                       NC1124.2
083100         PERFORM PASS                                             NC1124.2
083200         GO TO MOVE-WRITE-F1-1-7.                                 NC1124.2
083300     MOVE 1 TO CORRECT-N.                                         NC1124.2
083400     MOVE DNAME-7  TO COMPUTED-N.                                 NC1124.2
083500     PERFORM FAIL.                                                NC1124.2
083600     GO TO MOVE-WRITE-F1-1-7.                                     NC1124.2
083700 MOVE-DELETE-F1-1-7.                                              NC1124.2
083800     PERFORM DE-LETE.                                             NC1124.2
083900 MOVE-WRITE-F1-1-7.                                               NC1124.2
084000     MOVE "MOVE-TEST-F1-1-7 " TO PAR-NAME.                        NC1124.2
084100     PERFORM PRINT-DETAIL.                                        NC1124.2
084200 MOVE-TEST-F1-1-8.                                                NC1124.2
084300     IF DNAME-8  EQUAL TO 1                                       NC1124.2
084400         PERFORM PASS                                             NC1124.2
084500         GO TO MOVE-WRITE-F1-1-8.                                 NC1124.2
084600     MOVE 1 TO CORRECT-N.                                         NC1124.2
084700     MOVE DNAME-8  TO COMPUTED-N.                                 NC1124.2
084800     PERFORM FAIL.                                                NC1124.2
084900     GO TO MOVE-WRITE-F1-1-8.                                     NC1124.2
085000 MOVE-DELETE-F1-1-8.                                              NC1124.2
085100     PERFORM DE-LETE.                                             NC1124.2
085200 MOVE-WRITE-F1-1-8.                                               NC1124.2
085300     MOVE "MOVE-TEST-F1-1-8 " TO PAR-NAME.                        NC1124.2
085400     PERFORM PRINT-DETAIL.                                        NC1124.2
085500 MOVE-TEST-F1-1-9.                                                NC1124.2
085600     IF DNAME-9  EQUAL TO 1                                       NC1124.2
085700         PERFORM PASS                                             NC1124.2
085800         GO TO MOVE-WRITE-F1-1-9.                                 NC1124.2
085900     MOVE 1 TO CORRECT-N.                                         NC1124.2
086000     MOVE DNAME-9  TO COMPUTED-N.                                 NC1124.2
086100     PERFORM FAIL.                                                NC1124.2
086200     GO TO MOVE-WRITE-F1-1-9.                                     NC1124.2
086300 MOVE-DELETE-F1-1-9.                                              NC1124.2
086400     PERFORM DE-LETE.                                             NC1124.2
086500 MOVE-WRITE-F1-1-9.                                               NC1124.2
086600     MOVE "MOVE-TEST-F1-1-9 " TO PAR-NAME.                        NC1124.2
086700     PERFORM PRINT-DETAIL.                                        NC1124.2
086800 MOVE-TEST-F1-1-10.                                               NC1124.2
086900     IF DNAME-10 EQUAL TO 1                                       NC1124.2
087000         PERFORM PASS                                             NC1124.2
087100         GO TO MOVE-WRITE-F1-1-10.                                NC1124.2
087200     MOVE 1 TO CORRECT-N.                                         NC1124.2
087300     MOVE DNAME-10 TO COMPUTED-N.                                 NC1124.2
087400     PERFORM FAIL.                                                NC1124.2
087500     GO TO MOVE-WRITE-F1-1-10.                                    NC1124.2
087600 MOVE-DELETE-F1-1-10.                                             NC1124.2
087700     PERFORM DE-LETE.                                             NC1124.2
087800 MOVE-WRITE-F1-1-10.                                              NC1124.2
087900     MOVE "MOVE-TEST-F1-1-10" TO PAR-NAME.                        NC1124.2
088000     PERFORM PRINT-DETAIL.                                        NC1124.2
088100 MOVE-INIT-F1-2-1.                                                NC1124.2
088200     MOVE   "V1-134 6.25.4 GR1,2" TO ANSI-REFERENCE.              NC1124.2
088300     MOVE    HIGH-VALUE TO D-NAMES.                               NC1124.2
088400 MOVE-TEST-F1-2-0.                                                NC1124.2
088500     MOVE ZERO TO DNAME-1                                         NC1124.2
088600                  DNAME-2                                         NC1124.2
088700                  DNAME-3                                         NC1124.2
088800                  DNAME-4                                         NC1124.2
088900                  DNAME-5                                         NC1124.2
089000                  DNAME-6                                         NC1124.2
089100                  DNAME-7                                         NC1124.2
089200                  DNAME-8                                         NC1124.2
089300                  DNAME-9                                         NC1124.2
089400                  DNAME-10.                                       NC1124.2
089500 MOVE-TEST-F1-2-1.                                                NC1124.2
089600     IF DNAME-1  EQUAL TO 0                                       NC1124.2
089700         PERFORM PASS                                             NC1124.2
089800         GO TO MOVE-WRITE-F1-2-1.                                 NC1124.2
089900     MOVE 0 TO CORRECT-N.                                         NC1124.2
090000     MOVE DNAME-1  TO COMPUTED-N.                                 NC1124.2
090100     PERFORM FAIL.                                                NC1124.2
090200     GO TO MOVE-WRITE-F1-2-1.                                     NC1124.2
090300 MOVE-DELETE-F1-2-1.                                              NC1124.2
090400     PERFORM DE-LETE.                                             NC1124.2
090500 MOVE-WRITE-F1-2-1.                                               NC1124.2
090600     MOVE "MOVE-TEST-F1-2-1" TO PAR-NAME.                         NC1124.2
090700     PERFORM PRINT-DETAIL.                                        NC1124.2
090800 MOVE-TEST-F1-2-2.                                                NC1124.2
090900     IF DNAME-2  EQUAL TO 0                                       NC1124.2
091000         PERFORM PASS                                             NC1124.2
091100         GO TO MOVE-WRITE-F1-2-2.                                 NC1124.2
091200     MOVE 0 TO CORRECT-N.                                         NC1124.2
091300     MOVE DNAME-2  TO COMPUTED-N.                                 NC1124.2
091400     PERFORM FAIL.                                                NC1124.2
091500     GO TO MOVE-WRITE-F1-2-2.                                     NC1124.2
091600 MOVE-DELETE-F1-2-2.                                              NC1124.2
091700     PERFORM DE-LETE.                                             NC1124.2
091800 MOVE-WRITE-F1-2-2.                                               NC1124.2
091900     MOVE "MOVE-TEST-F1-2-2" TO PAR-NAME.                         NC1124.2
092000     PERFORM PRINT-DETAIL.                                        NC1124.2
092100 MOVE-TEST-F1-2-3.                                                NC1124.2
092200     IF DNAME-3  EQUAL TO 0                                       NC1124.2
092300         PERFORM PASS                                             NC1124.2
092400         GO TO MOVE-WRITE-F1-2-3.                                 NC1124.2
092500     MOVE 0 TO CORRECT-N.                                         NC1124.2
092600     MOVE DNAME-3  TO COMPUTED-N.                                 NC1124.2
092700     PERFORM FAIL.                                                NC1124.2
092800     GO TO MOVE-WRITE-F1-2-3.                                     NC1124.2
092900 MOVE-DELETE-F1-2-3.                                              NC1124.2
093000     PERFORM DE-LETE.                                             NC1124.2
093100 MOVE-WRITE-F1-2-3.                                               NC1124.2
093200     MOVE "MOVE-TEST-F1-2-3" TO PAR-NAME.                         NC1124.2
093300     PERFORM PRINT-DETAIL.                                        NC1124.2
093400 MOVE-TEST-F1-2-4.                                                NC1124.2
093500     IF DNAME-4  EQUAL TO 0                                       NC1124.2
093600         PERFORM PASS                                             NC1124.2
093700         GO TO MOVE-WRITE-F1-2-4.                                 NC1124.2
093800     MOVE 0 TO CORRECT-N.                                         NC1124.2
093900     MOVE DNAME-4  TO COMPUTED-N.                                 NC1124.2
094000     PERFORM FAIL.                                                NC1124.2
094100     GO TO MOVE-WRITE-F1-2-4.                                     NC1124.2
094200 MOVE-DELETE-F1-2-4.                                              NC1124.2
094300     PERFORM DE-LETE.                                             NC1124.2
094400 MOVE-WRITE-F1-2-4.                                               NC1124.2
094500     MOVE "MOVE-TEST-F1-2-4" TO PAR-NAME.                         NC1124.2
094600     PERFORM PRINT-DETAIL.                                        NC1124.2
094700 MOVE-TEST-F1-2-5.                                                NC1124.2
094800     IF DNAME-5  EQUAL TO 0                                       NC1124.2
094900         PERFORM PASS                                             NC1124.2
095000         GO TO MOVE-WRITE-F1-2-5.                                 NC1124.2
095100     MOVE 0 TO CORRECT-N.                                         NC1124.2
095200     MOVE DNAME-5  TO COMPUTED-N.                                 NC1124.2
095300     PERFORM FAIL.                                                NC1124.2
095400     GO TO MOVE-WRITE-F1-2-5.                                     NC1124.2
095500 MOVE-DELETE-F1-2-5.                                              NC1124.2
095600     PERFORM DE-LETE.                                             NC1124.2
095700 MOVE-WRITE-F1-2-5.                                               NC1124.2
095800     MOVE "MOVE-TEST-F1-2-5" TO PAR-NAME.                         NC1124.2
095900     PERFORM PRINT-DETAIL.                                        NC1124.2
096000 MOVE-TEST-F1-2-6.                                                NC1124.2
096100     IF DNAME-6  EQUAL TO 0                                       NC1124.2
096200         PERFORM PASS                                             NC1124.2
096300         GO TO MOVE-WRITE-F1-2-6.                                 NC1124.2
096400     MOVE 0 TO CORRECT-N.                                         NC1124.2
096500     MOVE DNAME-6  TO COMPUTED-N.                                 NC1124.2
096600     PERFORM FAIL.                                                NC1124.2
096700     GO TO MOVE-WRITE-F1-2-6.                                     NC1124.2
096800 MOVE-DELETE-F1-2-6.                                              NC1124.2
096900     PERFORM DE-LETE.                                             NC1124.2
097000 MOVE-WRITE-F1-2-6.                                               NC1124.2
097100     MOVE "MOVE-TEST-F1-2-6" TO PAR-NAME.                         NC1124.2
097200     PERFORM PRINT-DETAIL.                                        NC1124.2
097300 MOVE-TEST-F1-2-7.                                                NC1124.2
097400     IF DNAME-7  EQUAL TO 0                                       NC1124.2
097500         PERFORM PASS                                             NC1124.2
097600         GO TO MOVE-WRITE-F1-2-7.                                 NC1124.2
097700     MOVE 0 TO CORRECT-N.                                         NC1124.2
097800     MOVE DNAME-7  TO COMPUTED-N.                                 NC1124.2
097900     PERFORM FAIL.                                                NC1124.2
098000     GO TO MOVE-WRITE-F1-2-7.                                     NC1124.2
098100 MOVE-DELETE-F1-2-7.                                              NC1124.2
098200     PERFORM DE-LETE.                                             NC1124.2
098300 MOVE-WRITE-F1-2-7.                                               NC1124.2
098400     MOVE "MOVE-TEST-F1-2-7" TO PAR-NAME.                         NC1124.2
098500     PERFORM PRINT-DETAIL.                                        NC1124.2
098600 MOVE-TEST-F1-2-8.                                                NC1124.2
098700     IF DNAME-8  EQUAL TO 0                                       NC1124.2
098800         PERFORM PASS                                             NC1124.2
098900         GO TO MOVE-WRITE-F1-2-8.                                 NC1124.2
099000     MOVE 0 TO CORRECT-N.                                         NC1124.2
099100     MOVE DNAME-8  TO COMPUTED-N.                                 NC1124.2
099200     PERFORM FAIL.                                                NC1124.2
099300     GO TO MOVE-WRITE-F1-2-8.                                     NC1124.2
099400 MOVE-DELETE-F1-2-8.                                              NC1124.2
099500     PERFORM DE-LETE.                                             NC1124.2
099600 MOVE-WRITE-F1-2-8.                                               NC1124.2
099700     MOVE "MOVE-TEST-F1-2-8" TO PAR-NAME.                         NC1124.2
099800     PERFORM PRINT-DETAIL.                                        NC1124.2
099900 MOVE-TEST-F1-2-9.                                                NC1124.2
100000     IF DNAME-9  EQUAL TO 0                                       NC1124.2
100100         PERFORM PASS                                             NC1124.2
100200         GO TO MOVE-WRITE-F1-2-9.                                 NC1124.2
100300     MOVE 0 TO CORRECT-N.                                         NC1124.2
100400     MOVE DNAME-9  TO COMPUTED-N.                                 NC1124.2
100500     PERFORM FAIL.                                                NC1124.2
100600     GO TO MOVE-WRITE-F1-2-9.                                     NC1124.2
100700 MOVE-DELETE-F1-2-9.                                              NC1124.2
100800     PERFORM DE-LETE.                                             NC1124.2
100900 MOVE-WRITE-F1-2-9.                                               NC1124.2
101000     MOVE "MOVE-TEST-F1-2-9" TO PAR-NAME.                         NC1124.2
101100     PERFORM PRINT-DETAIL.                                        NC1124.2
101200 MOVE-TEST-F1-2-10.                                               NC1124.2
101300     IF DNAME-10 EQUAL TO 0                                       NC1124.2
101400         PERFORM PASS                                             NC1124.2
101500         GO TO MOVE-WRITE-F1-2-10.                                NC1124.2
101600     MOVE 0 TO CORRECT-N.                                         NC1124.2
101700     MOVE DNAME-10 TO COMPUTED-N.                                 NC1124.2
101800     PERFORM FAIL.                                                NC1124.2
101900     GO TO MOVE-WRITE-F1-2-10.                                    NC1124.2
102000 MOVE-DELETE-F1-2-10.                                             NC1124.2
102100     PERFORM DE-LETE.                                             NC1124.2
102200 MOVE-WRITE-F1-2-10.                                              NC1124.2
102300     MOVE "MOVE-TEST-F1-2-10" TO PAR-NAME.                        NC1124.2
102400     PERFORM PRINT-DETAIL.                                        NC1124.2
102500 CCVS-EXIT SECTION.                                               NC1124.2
102600 CCVS-999999.                                                     NC1124.2
102700     GO TO CLOSE-FILES.                                           NC1124.2
      *END-OF,NC112A                                                                  
