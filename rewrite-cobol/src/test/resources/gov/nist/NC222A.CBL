      *HEADER,COBOL,NC222A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2224.2
000200 PROGRAM-ID.                                                      NC2224.2
000300     NC222A.                                                      NC2224.2
000400*                                                              *  NC2224.2
000500****************************************************************  NC2224.2
000600*                                                              *  NC2224.2
000700*    VALIDATION FOR:-                                          *  NC2224.2
000800*                                                              *  NC2224.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2224.2
001000*                                                              *  NC2224.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2224.2
001200*                                                              *  NC2224.2
001300****************************************************************  NC2224.2
001400*                                                              *  NC2224.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2224.2
001600*                                                              *  NC2224.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2224.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2224.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2224.2
002000*                                                              *  NC2224.2
002100****************************************************************  NC2224.2
002200*    PROGRAM NC222A TESTS THE USE OF INDEXED IDENTIFIERS WITH  *  NC2224.2
002300*    FORMAT 2 OF THE "ADD", "SUBTRACT" AND "MOVE" STATEMENTS.  *  NC2224.2
002400*    DE-EDITING BY USE OF THE "MOVE" STATEMENT IS ALSO TESTED. *  NC2224.2
002500*                                                              *  NC2224.2
002600****************************************************************  NC2224.2
002700 ENVIRONMENT DIVISION.                                            NC2224.2
002800 CONFIGURATION SECTION.                                           NC2224.2
002900 SOURCE-COMPUTER.                                                 NC2224.2
003000     XXXXX082.                                                    NC2224.2
003100 OBJECT-COMPUTER.                                                 NC2224.2
003200     XXXXX083.                                                    NC2224.2
003300 INPUT-OUTPUT SECTION.                                            NC2224.2
003400 FILE-CONTROL.                                                    NC2224.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC2224.2
003600     XXXXX055.                                                    NC2224.2
003700 DATA DIVISION.                                                   NC2224.2
003800 FILE SECTION.                                                    NC2224.2
003900 FD  PRINT-FILE.                                                  NC2224.2
004000 01  PRINT-REC PICTURE X(120).                                    NC2224.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC2224.2
004200 WORKING-STORAGE SECTION.                                         NC2224.2
004300 01  TABLE1.                                                      NC2224.2
004400     02  RECORD1                 PICTURE 99.                      NC2224.2
004500     02  RECORD2                 PICTURE 99                       NC2224.2
004600                                 OCCURS 2 TIMES                   NC2224.2
004700                                 INDEXED BY INDEX1.               NC2224.2
004800     02  RECORD3                 PICTURE 99.                      NC2224.2
004900 01  TABLE2.                                                      NC2224.2
005000     02  RECORD1                 PICTURE 99.                      NC2224.2
005100     02  RECORD2                 PICTURE 99                       NC2224.2
005200                                 OCCURS 2 TIMES                   NC2224.2
005300                                 INDEXED BY INDEX2.               NC2224.2
005400     02  RECORD3                 PICTURE 99.                      NC2224.2
005500 01  TABLE3.                                                      NC2224.2
005600     02  RECORD1                 PICTURE XX   VALUE "AA".         NC2224.2
005700     02  RECORD2                 PICTURE XX                       NC2224.2
005800                                 OCCURS 2 TIMES                   NC2224.2
005900                                 INDEXED BY INDEX3.               NC2224.2
006000     02  RECORD3                 PICTURE XX   VALUE "DD".         NC2224.2
006100 01  TABLE4.                                                      NC2224.2
006200     02  RECORD1                 PICTURE XX   VALUE "EE".         NC2224.2
006300     02  RECORD2                 PICTURE XX                       NC2224.2
006400                                 OCCURS 2 TIMES                   NC2224.2
006500                                 INDEXED BY INDEX4.               NC2224.2
006600     02  RECORD3                 PICTURE XX   VALUE "HH".         NC2224.2
006700 01  MOVE-TEST-3-A               PIC $(4)9.99CR.                  NC2224.2
006800 01  MOVE-TEST-3-B               PIC S9(4)V99.                    NC2224.2
006900 01  MOVE-TEST-4-A               PIC --9B.99B99/99.               NC2224.2
007000 01  MOVE-TEST-4-B               PIC S99V9(6).                    NC2224.2
007100 01  TEST-RESULTS.                                                NC2224.2
007200     02 FILLER                   PIC X      VALUE SPACE.          NC2224.2
007300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2224.2
007400     02 FILLER                   PIC X      VALUE SPACE.          NC2224.2
007500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2224.2
007600     02 FILLER                   PIC X      VALUE SPACE.          NC2224.2
007700     02  PAR-NAME.                                                NC2224.2
007800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2224.2
007900       03  PARDOT-X              PIC X      VALUE SPACE.          NC2224.2
008000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2224.2
008100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2224.2
008200     02 RE-MARK                  PIC X(61).                       NC2224.2
008300 01  TEST-COMPUTED.                                               NC2224.2
008400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2224.2
008500     02 FILLER                   PIC X(17)  VALUE                 NC2224.2
008600            "       COMPUTED=".                                   NC2224.2
008700     02 COMPUTED-X.                                               NC2224.2
008800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2224.2
008900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2224.2
009000                                 PIC -9(9).9(9).                  NC2224.2
009100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2224.2
009200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2224.2
009300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2224.2
009400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2224.2
009500         04 COMPUTED-18V0                    PIC -9(18).          NC2224.2
009600         04 FILLER                           PIC X.               NC2224.2
009700     03 FILLER PIC X(50) VALUE SPACE.                             NC2224.2
009800 01  TEST-CORRECT.                                                NC2224.2
009900     02 FILLER PIC X(30) VALUE SPACE.                             NC2224.2
010000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2224.2
010100     02 CORRECT-X.                                                NC2224.2
010200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2224.2
010300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2224.2
010400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2224.2
010500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2224.2
010600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2224.2
010700     03      CR-18V0 REDEFINES CORRECT-A.                         NC2224.2
010800         04 CORRECT-18V0                     PIC -9(18).          NC2224.2
010900         04 FILLER                           PIC X.               NC2224.2
011000     03 FILLER PIC X(2) VALUE SPACE.                              NC2224.2
011100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2224.2
011200 01  CCVS-C-1.                                                    NC2224.2
011300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2224.2
011400-    "SS  PARAGRAPH-NAME                                          NC2224.2
011500-    "       REMARKS".                                            NC2224.2
011600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2224.2
011700 01  CCVS-C-2.                                                    NC2224.2
011800     02 FILLER                     PIC X        VALUE SPACE.      NC2224.2
011900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2224.2
012000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2224.2
012100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2224.2
012200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2224.2
012300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2224.2
012400 01  REC-CT                        PIC 99       VALUE ZERO.       NC2224.2
012500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2224.2
012600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2224.2
012700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2224.2
012800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2224.2
012900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2224.2
013000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2224.2
013100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2224.2
013200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2224.2
013300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2224.2
013400 01  CCVS-H-1.                                                    NC2224.2
013500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2224.2
013600     02  FILLER                    PIC X(42)    VALUE             NC2224.2
013700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2224.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2224.2
013900 01  CCVS-H-2A.                                                   NC2224.2
014000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2224.2
014100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2224.2
014200   02  FILLER                        PIC XXXX   VALUE             NC2224.2
014300     "4.2 ".                                                      NC2224.2
014400   02  FILLER                        PIC X(28)  VALUE             NC2224.2
014500            " COPY - NOT FOR DISTRIBUTION".                       NC2224.2
014600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2224.2
014700                                                                  NC2224.2
014800 01  CCVS-H-2B.                                                   NC2224.2
014900   02  FILLER                        PIC X(15)  VALUE             NC2224.2
015000            "TEST RESULT OF ".                                    NC2224.2
015100   02  TEST-ID                       PIC X(9).                    NC2224.2
015200   02  FILLER                        PIC X(4)   VALUE             NC2224.2
015300            " IN ".                                               NC2224.2
015400   02  FILLER                        PIC X(12)  VALUE             NC2224.2
015500     " HIGH       ".                                              NC2224.2
015600   02  FILLER                        PIC X(22)  VALUE             NC2224.2
015700            " LEVEL VALIDATION FOR ".                             NC2224.2
015800   02  FILLER                        PIC X(58)  VALUE             NC2224.2
015900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2224.2
016000 01  CCVS-H-3.                                                    NC2224.2
016100     02  FILLER                      PIC X(34)  VALUE             NC2224.2
016200            " FOR OFFICIAL USE ONLY    ".                         NC2224.2
016300     02  FILLER                      PIC X(58)  VALUE             NC2224.2
016400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2224.2
016500     02  FILLER                      PIC X(28)  VALUE             NC2224.2
016600            "  COPYRIGHT   1985 ".                                NC2224.2
016700 01  CCVS-E-1.                                                    NC2224.2
016800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2224.2
016900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2224.2
017000     02 ID-AGAIN                     PIC X(9).                    NC2224.2
017100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2224.2
017200 01  CCVS-E-2.                                                    NC2224.2
017300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2224.2
017400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2224.2
017500     02 CCVS-E-2-2.                                               NC2224.2
017600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2224.2
017700         03 FILLER                   PIC X      VALUE SPACE.      NC2224.2
017800         03 ENDER-DESC               PIC X(44)  VALUE             NC2224.2
017900            "ERRORS ENCOUNTERED".                                 NC2224.2
018000 01  CCVS-E-3.                                                    NC2224.2
018100     02  FILLER                      PIC X(22)  VALUE             NC2224.2
018200            " FOR OFFICIAL USE ONLY".                             NC2224.2
018300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2224.2
018400     02  FILLER                      PIC X(58)  VALUE             NC2224.2
018500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2224.2
018600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2224.2
018700     02 FILLER                       PIC X(15)  VALUE             NC2224.2
018800             " COPYRIGHT 1985".                                   NC2224.2
018900 01  CCVS-E-4.                                                    NC2224.2
019000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2224.2
019100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2224.2
019200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2224.2
019300     02 FILLER                       PIC X(40)  VALUE             NC2224.2
019400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2224.2
019500 01  XXINFO.                                                      NC2224.2
019600     02 FILLER                       PIC X(19)  VALUE             NC2224.2
019700            "*** INFORMATION ***".                                NC2224.2
019800     02 INFO-TEXT.                                                NC2224.2
019900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2224.2
020000       04 XXCOMPUTED                 PIC X(20).                   NC2224.2
020100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2224.2
020200       04 XXCORRECT                  PIC X(20).                   NC2224.2
020300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2224.2
020400 01  HYPHEN-LINE.                                                 NC2224.2
020500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2224.2
020600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2224.2
020700-    "*****************************************".                 NC2224.2
020800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2224.2
020900-    "******************************".                            NC2224.2
021000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2224.2
021100     "NC222A".                                                    NC2224.2
021200 PROCEDURE DIVISION.                                              NC2224.2
021300 CCVS1 SECTION.                                                   NC2224.2
021400 OPEN-FILES.                                                      NC2224.2
021500     OPEN     OUTPUT PRINT-FILE.                                  NC2224.2
021600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2224.2
021700     MOVE    SPACE TO TEST-RESULTS.                               NC2224.2
021800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2224.2
021900     GO TO CCVS1-EXIT.                                            NC2224.2
022000 CLOSE-FILES.                                                     NC2224.2
022100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2224.2
022200 TERMINATE-CCVS.                                                  NC2224.2
022300*    EXIT PROGRAM.                                                NC2224.2
022400*TERMINATE-CALL.                                                  NC2224.2
022500     STOP     RUN.                                                NC2224.2
022600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2224.2
022700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2224.2
022800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2224.2
022900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2224.2
023000     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2224.2
023100 PRINT-DETAIL.                                                    NC2224.2
023200     IF REC-CT NOT EQUAL TO ZERO                                  NC2224.2
023300             MOVE "." TO PARDOT-X                                 NC2224.2
023400             MOVE REC-CT TO DOTVALUE.                             NC2224.2
023500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2224.2
023600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2224.2
023700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2224.2
023800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2224.2
023900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2224.2
024000     MOVE SPACE TO CORRECT-X.                                     NC2224.2
024100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2224.2
024200     MOVE     SPACE TO RE-MARK.                                   NC2224.2
024300 HEAD-ROUTINE.                                                    NC2224.2
024400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2224.2
024500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2224.2
024600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2224.2
024700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2224.2
024800 COLUMN-NAMES-ROUTINE.                                            NC2224.2
024900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2224.2
025000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2224.2
025100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2224.2
025200 END-ROUTINE.                                                     NC2224.2
025300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2224.2
025400 END-RTN-EXIT.                                                    NC2224.2
025500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2224.2
025600 END-ROUTINE-1.                                                   NC2224.2
025700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2224.2
025800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2224.2
025900      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2224.2
026000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2224.2
026100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2224.2
026200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2224.2
026300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2224.2
026400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2224.2
026500  END-ROUTINE-12.                                                 NC2224.2
026600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2224.2
026700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2224.2
026800         MOVE "NO " TO ERROR-TOTAL                                NC2224.2
026900         ELSE                                                     NC2224.2
027000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2224.2
027100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2224.2
027200     PERFORM WRITE-LINE.                                          NC2224.2
027300 END-ROUTINE-13.                                                  NC2224.2
027400     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2224.2
027500         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2224.2
027600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2224.2
027700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2224.2
027800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2224.2
027900      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2224.2
028000          MOVE "NO " TO ERROR-TOTAL                               NC2224.2
028100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2224.2
028200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2224.2
028300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2224.2
028400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2224.2
028500 WRITE-LINE.                                                      NC2224.2
028600     ADD 1 TO RECORD-COUNT.                                       NC2224.2
028700*    IF RECORD-COUNT GREATER 50                                   NC2224.2
028800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2224.2
028900*        MOVE SPACE TO DUMMY-RECORD                               NC2224.2
029000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2224.2
029100*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2224.2
029200*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2224.2
029300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2224.2
029400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2224.2
029500*        MOVE ZERO TO RECORD-COUNT.                               NC2224.2
029600     PERFORM WRT-LN.                                              NC2224.2
029700 WRT-LN.                                                          NC2224.2
029800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2224.2
029900     MOVE SPACE TO DUMMY-RECORD.                                  NC2224.2
030000 BLANK-LINE-PRINT.                                                NC2224.2
030100     PERFORM WRT-LN.                                              NC2224.2
030200 FAIL-ROUTINE.                                                    NC2224.2
030300     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2224.2
030400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2224.2
030500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2224.2
030600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2224.2
030700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2224.2
030800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2224.2
030900     GO TO  FAIL-ROUTINE-EX.                                      NC2224.2
031000 FAIL-ROUTINE-WRITE.                                              NC2224.2
031100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2224.2
031200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2224.2
031300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2224.2
031400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2224.2
031500 FAIL-ROUTINE-EX. EXIT.                                           NC2224.2
031600 BAIL-OUT.                                                        NC2224.2
031700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2224.2
031800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2224.2
031900 BAIL-OUT-WRITE.                                                  NC2224.2
032000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2224.2
032100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2224.2
032200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2224.2
032300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2224.2
032400 BAIL-OUT-EX. EXIT.                                               NC2224.2
032500 CCVS1-EXIT.                                                      NC2224.2
032600     EXIT.                                                        NC2224.2
032700 INITIALISE-TABLE1.                                               NC2224.2
032800     MOVE 06 TO RECORD1 OF TABLE1.                                NC2224.2
032900     MOVE 01 TO RECORD2 OF TABLE1 (1).                            NC2224.2
033000     MOVE 02 TO RECORD2 OF TABLE1 (2).                            NC2224.2
033100     MOVE 07 TO RECORD3 OF TABLE1.                                NC2224.2
033200 INITIALISE-TABLE2.                                               NC2224.2
033300     MOVE 08 TO RECORD1 OF TABLE2.                                NC2224.2
033400     MOVE 03 TO RECORD2 OF TABLE2 (1).                            NC2224.2
033500     MOVE 04 TO RECORD2 OF TABLE2 (2).                            NC2224.2
033600     MOVE 09 TO RECORD3 OF TABLE2.                                NC2224.2
033700*                                                                 NC2224.2
033800 ADD-INIT-F2-1.                                                   NC2224.2
033900     MOVE   "ADD-TEST-F2-1" TO PAR-NAME.                          NC2224.2
034000     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2224.2
034100     MOVE "ADD - QUALIFICATION" TO FEATURE.                       NC2224.2
034200     PERFORM INITIALISE-TABLE1.                                   NC2224.2
034300     PERFORM INITIALISE-TABLE2.                                   NC2224.2
034400     SET INDEX1 TO 1.                                             NC2224.2
034500     SET INDEX2 TO 1.                                             NC2224.2
034600 ADD-TEST-F2-1.                                                   NC2224.2
034700     ADD RECORD2 OF TABLE1 (INDEX1) TO RECORD2 OF TABLE2 (INDEX2).NC2224.2
034800     IF      RECORD2 OF TABLE2 (1) = 04                           NC2224.2
034900         AND RECORD2 OF TABLE2 (2) = 04                           NC2224.2
035000         AND RECORD1 OF TABLE2 = 08                               NC2224.2
035100         AND RECORD3 OF TABLE2 = 09                               NC2224.2
035200             PERFORM PASS                                         NC2224.2
035300     ELSE                                                         NC2224.2
035400             GO TO   ADD-FAIL-F2-1.                               NC2224.2
035500     GO TO   ADD-WRITE-F2-1.                                      NC2224.2
035600 ADD-DELETE-F2-1.                                                 NC2224.2
035700     PERFORM DE-LETE.                                             NC2224.2
035800     GO TO   ADD-WRITE-F2-1.                                      NC2224.2
035900 ADD-FAIL-F2-1.                                                   NC2224.2
036000     PERFORM FAIL.                                                NC2224.2
036100     MOVE    TABLE2 TO COMPUTED-A.                                NC2224.2
036200     MOVE   "08040409" TO CORRECT-A.                              NC2224.2
036300 ADD-WRITE-F2-1.                                                  NC2224.2
036400     PERFORM PRINT-DETAIL.                                        NC2224.2
036500                                                                  NC2224.2
036600 SUBTRACT-INIT-F2-1.                                              NC2224.2
036700     MOVE "SUBTRACT-TEST-F2-1" TO PAR-NAME.                       NC2224.2
036800     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2224.2
036900     MOVE   "SUBTRACT - QUAL." TO FEATURE.                        NC2224.2
037000     PERFORM INITIALISE-TABLE1.                                   NC2224.2
037100     PERFORM INITIALISE-TABLE2.                                   NC2224.2
037200     SET     INDEX1 TO 1.                                         NC2224.2
037300     SET     INDEX2 TO 1.                                         NC2224.2
037400 SUBTRACT-TEST-F2-1-0.                                            NC2224.2
037500     SUBTRACT RECORD2 OF TABLE1 (INDEX1)                          NC2224.2
037600         FROM RECORD2 OF TABLE2 (INDEX2).                         NC2224.2
037700 SUBTRACT-TEST-F2-1-1.                                            NC2224.2
037800     IF      RECORD2 OF TABLE2 (1) = 02                           NC2224.2
037900         AND RECORD2 OF TABLE2 (2) = 04                           NC2224.2
038000         AND RECORD1 OF TABLE2 = 08                               NC2224.2
038100         AND RECORD3 OF TABLE2 = 09                               NC2224.2
038200             PERFORM PASS                                         NC2224.2
038300     ELSE                                                         NC2224.2
038400             GO TO   SUBTRACT-FAIL-F2-1.                          NC2224.2
038500     GO TO   SUBTRACT-WRITE-F2-1.                                 NC2224.2
038600 SUBTRACT-DELETE-F2-1.                                            NC2224.2
038700     PERFORM DE-LETE.                                             NC2224.2
038800     GO TO SUBTRACT-WRITE-F2-1.                                   NC2224.2
038900 SUBTRACT-FAIL-F2-1.                                              NC2224.2
039000     PERFORM FAIL.                                                NC2224.2
039100     MOVE TABLE2 TO COMPUTED-A.                                   NC2224.2
039200     MOVE "08020409" TO CORRECT-A.                                NC2224.2
039300 SUBTRACT-WRITE-F2-1.                                             NC2224.2
039400     PERFORM PRINT-DETAIL.                                        NC2224.2
039500*                                                                 NC2224.2
039600 MOV-INIT-F2-1.                                                   NC2224.2
039700     MOVE "MOV-TEST-F2-1" TO PAR-NAME.                            NC2224.2
039800     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2224.2
039900     MOVE "MOVE CORRESPONDING" TO FEATURE.                        NC2224.2
040000     PERFORM INITIALISE-TABLE1.                                   NC2224.2
040100     PERFORM INITIALISE-TABLE2.                                   NC2224.2
040200 MOV-TEST-F2-1.                                                   NC2224.2
040300     MOVE CORRESPONDING TABLE1 TO TABLE2.                         NC2224.2
040400     IF RECORD1 OF TABLE2 = 06 AND                                NC2224.2
040500        RECORD2 OF TABLE2 (1) = 03 AND                            NC2224.2
040600        RECORD2 OF TABLE2 (2) = 04 AND                            NC2224.2
040700        RECORD3 OF TABLE2 = 07                                    NC2224.2
040800        PERFORM PASS                                              NC2224.2
040900        ELSE GO TO MOV-FAIL-F2-1.                                 NC2224.2
041000     GO TO MOV-WRITE-F2-1.                                        NC2224.2
041100 MOV-DELETE-F2-1.                                                 NC2224.2
041200     PERFORM DE-LETE.                                             NC2224.2
041300     GO TO MOV-WRITE-F2-1.                                        NC2224.2
041400 MOV-FAIL-F2-1.                                                   NC2224.2
041500     PERFORM FAIL.                                                NC2224.2
041600     MOVE TABLE2 TO COMPUTED-A.                                   NC2224.2
041700     MOVE "06030407" TO CORRECT-A.                                NC2224.2
041800 MOV-WRITE-F2-1.                                                  NC2224.2
041900     PERFORM PRINT-DETAIL.                                        NC2224.2
042000*                                                                 NC2224.2
042100 INITIALISE-TABLE3.                                               NC2224.2
042200     MOVE "BB" TO RECORD2 OF TABLE3 (1).                          NC2224.2
042300     MOVE "CC" TO RECORD2 OF TABLE3 (2).                          NC2224.2
042400 INITIALISE-TABLE4.                                               NC2224.2
042500     MOVE "FF" TO RECORD2 OF TABLE4 (1).                          NC2224.2
042600     MOVE "GG" TO RECORD2 OF TABLE4 (2).                          NC2224.2
042700*                                                                 NC2224.2
042800 MOV-INIT-F2-2.                                                   NC2224.2
042900     MOVE "MOV-TEST-F2-2" TO PAR-NAME.                            NC2224.2
043000     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2224.2
043100     MOVE "MOVE CORRESPONDING" TO FEATURE.                        NC2224.2
043200 MOV-TEST-F2-2.                                                   NC2224.2
043300     MOVE CORRESPONDING TABLE3 TO TABLE4.                         NC2224.2
043400     IF RECORD1 OF TABLE4 = "AA" AND                              NC2224.2
043500        RECORD2 OF TABLE4 (1) = "FF" AND                          NC2224.2
043600        RECORD2 OF TABLE4 (2) = "GG" AND                          NC2224.2
043700        RECORD3 OF TABLE4 = "DD"                                  NC2224.2
043800        PERFORM PASS                                              NC2224.2
043900        ELSE GO TO MOV-FAIL-F2-2.                                 NC2224.2
044000     GO TO MOV-WRITE-F2-2.                                        NC2224.2
044100 MOV-DELETE-F2-2.                                                 NC2224.2
044200     PERFORM DE-LETE.                                             NC2224.2
044300     GO TO MOV-WRITE-F2-2.                                        NC2224.2
044400 MOV-FAIL-F2-2.                                                   NC2224.2
044500     PERFORM FAIL.                                                NC2224.2
044600     MOVE TABLE4 TO COMPUTED-A.                                   NC2224.2
044700     MOVE "AAFFGGDD" TO CORRECT-A.                                NC2224.2
044800 MOV-WRITE-F2-2.                                                  NC2224.2
044900     PERFORM PRINT-DETAIL.                                        NC2224.2
045000*                                                                 NC2224.2
045100 MOV-INIT-F2-3.                                                   NC2224.2
045200*    ===-->  DE-EDITING  <--===                                   NC2224.2
045300     MOVE   "VI-104 6.18.4 GR4(b)" TO ANSI-REFERENCE.             NC2224.2
045400     MOVE   -123.45 TO MOVE-TEST-3-A.                             NC2224.2
045500     MOVE    1 TO REC-CT.                                         NC2224.2
045600 MOV-TEST-F2-3-0.                                                 NC2224.2
045700     MOVE    MOVE-TEST-3-A TO MOVE-TEST-3-B.                      NC2224.2
045800     GO TO   MOV-TEST-F2-3-1.                                     NC2224.2
045900 MOV-DELETE-F2-3.                                                 NC2224.2
046000     PERFORM DE-LETE.                                             NC2224.2
046100     PERFORM PRINT-DETAIL.                                        NC2224.2
046200     GO TO MOV-INIT-F2-4.                                         NC2224.2
046300 MOV-TEST-F2-3-1.                                                 NC2224.2
046400     MOVE   "MOV-TEST-F2-3-1" TO PAR-NAME.                        NC2224.2
046500     IF      MOVE-TEST-3-B = -123.45                              NC2224.2
046600             PERFORM PASS                                         NC2224.2
046700             GO TO MOV-WRITE-F2-3-1                               NC2224.2
046800     ELSE                                                         NC2224.2
046900             GO TO MOV-FAIL-F2-3-1.                               NC2224.2
047000 MOV-DELETE-F2-3-1.                                               NC2224.2
047100     PERFORM DE-LETE.                                             NC2224.2
047200     GO TO MOV-WRITE-F2-3-1.                                      NC2224.2
047300 MOV-FAIL-F2-3-1.                                                 NC2224.2
047400     MOVE   -123.45 TO CORRECT-N                                  NC2224.2
047500     MOVE    MOVE-TEST-3-B TO COMPUTED-N                          NC2224.2
047600     MOVE   "DE-EDITING MOVE ERROR" TO RE-MARK                    NC2224.2
047700     PERFORM FAIL.                                                NC2224.2
047800 MOV-WRITE-F2-3-1.                                                NC2224.2
047900     PERFORM PRINT-DETAIL.                                        NC2224.2
048000*                                                                 NC2224.2
048100 MOV-INIT-F2-3-2.                                                 NC2224.2
048200     ADD     1 TO REC-CT.                                         NC2224.2
048300     MOVE   "MOV-TEST-F2-3-2" TO PAR-NAME.                        NC2224.2
048400 MOV-TEST-F2-3-2.                                                 NC2224.2
048500     IF      MOVE-TEST-3-A = " $123.45CR"                         NC2224.2
048600             PERFORM PASS                                         NC2224.2
048700             GO TO MOV-WRITE-F2-3-2                               NC2224.2
048800     ELSE                                                         NC2224.2
048900             GO TO MOV-FAIL-F2-3-2.                               NC2224.2
049000 MOV-DELETE-F2-3-2.                                               NC2224.2
049100     PERFORM DE-LETE.                                             NC2224.2
049200     GO TO MOV-WRITE-F2-3-2.                                      NC2224.2
049300 MOV-FAIL-F2-3-2.                                                 NC2224.2
049400     MOVE   " $123.45" TO CORRECT-X                               NC2224.2
049500     MOVE    MOVE-TEST-3-A TO COMPUTED-X                          NC2224.2
049600     MOVE   "EDITED DATA MOVE ERROR" TO RE-MARK                   NC2224.2
049700     PERFORM FAIL.                                                NC2224.2
049800 MOV-WRITE-F2-3-2.                                                NC2224.2
049900     PERFORM PRINT-DETAIL.                                        NC2224.2
050000*                                                                 NC2224.2
050100 MOV-INIT-F2-4.                                                   NC2224.2
050200*    ===-->  DE-EDITING  <--===                                   NC2224.2
050300     MOVE   "VI-104 6.18.4 GR4(b)" TO ANSI-REFERENCE.             NC2224.2
050400     MOVE   -42.9876 TO MOVE-TEST-4-A.                            NC2224.2
050500     MOVE    1 TO REC-CT.                                         NC2224.2
050600 MOV-TEST-F2-4-0.                                                 NC2224.2
050700     MOVE    MOVE-TEST-4-A TO MOVE-TEST-4-B.                      NC2224.2
050800     GO TO MOV-TEST-F2-4-1.                                       NC2224.2
050900 MOV-DELETE-F2-4.                                                 NC2224.2
051000     PERFORM DE-LETE.                                             NC2224.2
051100     PERFORM PRINT-DETAIL.                                        NC2224.2
051200     GO TO CCVS-999999.                                           NC2224.2
051300 MOV-TEST-F2-4-1.                                                 NC2224.2
051400     MOVE   "MOV-TEST-F2-4-1" TO PAR-NAME.                        NC2224.2
051500     IF      MOVE-TEST-4-B = -42.987600                           NC2224.2
051600             PERFORM PASS                                         NC2224.2
051700             GO TO MOV-WRITE-F2-4-1                               NC2224.2
051800     ELSE                                                         NC2224.2
051900             GO TO MOV-FAIL-F2-4-1.                               NC2224.2
052000 MOV-DELETE-F2-4-1.                                               NC2224.2
052100     PERFORM DE-LETE.                                             NC2224.2
052200     GO TO MOV-WRITE-F2-4-1.                                      NC2224.2
052300 MOV-FAIL-F2-4-1.                                                 NC2224.2
052400     MOVE   -42.987600 TO CORRECT-N                               NC2224.2
052500     MOVE    MOVE-TEST-4-B TO COMPUTED-N                          NC2224.2
052600     MOVE   "DE-EDITING MOVE ERROR" TO RE-MARK                    NC2224.2
052700     PERFORM FAIL.                                                NC2224.2
052800 MOV-WRITE-F2-4-1.                                                NC2224.2
052900     PERFORM PRINT-DETAIL.                                        NC2224.2
053000*                                                                 NC2224.2
053100 MOV-TEST-F2-4-2.                                                 NC2224.2
053200     ADD     1 TO REC-CT.                                         NC2224.2
053300     MOVE   "MOV-TEST-F2-4-2" TO PAR-NAME.                        NC2224.2
053400     IF      MOVE-TEST-4-A = "-42 .98 76/00"                      NC2224.2
053500             PERFORM PASS                                         NC2224.2
053600             GO TO MOV-WRITE-F2-4-2                               NC2224.2
053700     ELSE                                                         NC2224.2
053800             GO TO MOV-FAIL-F2-4-2.                               NC2224.2
053900 MOV-DELETE-F2-4-2.                                               NC2224.2
054000     PERFORM DE-LETE.                                             NC2224.2
054100     GO TO MOV-WRITE-F2-4-2.                                      NC2224.2
054200 MOV-FAIL-F2-4-2.                                                 NC2224.2
054300     MOVE   "-42 .98 76/00" TO CORRECT-X                          NC2224.2
054400     MOVE    MOVE-TEST-4-A TO COMPUTED-X                          NC2224.2
054500     MOVE   "EDITED DATA MOVE ERROR" TO RE-MARK                   NC2224.2
054600     PERFORM FAIL.                                                NC2224.2
054700 MOV-WRITE-F2-4-2.                                                NC2224.2
054800     PERFORM PRINT-DETAIL.                                        NC2224.2
054900*                                                                 NC2224.2
055000 CCVS-EXIT SECTION.                                               NC2224.2
055100 CCVS-999999.                                                     NC2224.2
055200     GO TO CLOSE-FILES.                                           NC2224.2
      *END-OF,NC222A                                                                  
