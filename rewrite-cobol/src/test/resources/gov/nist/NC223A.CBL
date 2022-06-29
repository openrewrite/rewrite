      *HEADER,COBOL,NC223A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2234.2
000200 PROGRAM-ID.                                                      NC2234.2
000300     NC223A.                                                      NC2234.2
000400****************************************************************  NC2234.2
000500*                                                              *  NC2234.2
000600*    VALIDATION FOR:-                                          *  NC2234.2
000700*                                                              *  NC2234.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2234.2
000900*                                                              *  NC2234.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2234.2
001100*                                                              *  NC2234.2
001200****************************************************************  NC2234.2
001300*                                                              *  NC2234.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2234.2
001500*                                                              *  NC2234.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2234.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2234.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2234.2
001900*                                                              *  NC2234.2
002000****************************************************************  NC2234.2
002100*    PROGRAM NC223A TESTS THE "INITIALISE" STATEMENT USING     *  NC2234.2
002200*    USING VARIOUS COMBINATIONS OD OPTIONAL PHRASES AND A      *  NC2234.2
002300*    VARIETY OF RECEIVING AREAS.                               *  NC2234.2
002400*                                                              *  NC2234.2
002500****************************************************************  NC2234.2
002600 ENVIRONMENT DIVISION.                                            NC2234.2
002700 CONFIGURATION SECTION.                                           NC2234.2
002800 SOURCE-COMPUTER.                                                 NC2234.2
002900     XXXXX082.                                                    NC2234.2
003000 OBJECT-COMPUTER.                                                 NC2234.2
003100     XXXXX083.                                                    NC2234.2
003200 INPUT-OUTPUT SECTION.                                            NC2234.2
003300 FILE-CONTROL.                                                    NC2234.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC2234.2
003500     XXXXX055.                                                    NC2234.2
003600 DATA DIVISION.                                                   NC2234.2
003700 FILE SECTION.                                                    NC2234.2
003800 FD  PRINT-FILE.                                                  NC2234.2
003900 01  PRINT-REC PICTURE X(120).                                    NC2234.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC2234.2
004100 WORKING-STORAGE SECTION.                                         NC2234.2
004200 01  TEST-1-DATA.                                                 NC2234.2
004300   03        TEST-1-1            PIC     9(6).                    NC2234.2
004400   03        TEST-1-2            PIC     $(3)9.99.                NC2234.2
004500   03        TEST-1-3            PIC     X(10).                   NC2234.2
004600   03        TEST-1-4            PIC     XXBXX/XX.                NC2234.2
004700   03        TEST-1-5            PIC     A(6).                    NC2234.2
004800   03        TEST-1-6            PIC     9(6).                    NC2234.2
004900   03        TEST-1-7            PIC     $(3)9.99.                NC2234.2
005000   03        TEST-1-8            PIC     X(10).                   NC2234.2
005100   03        TEST-1-9            PIC     XXBXX/XX.                NC2234.2
005200   03        TEST-1-10           PIC     A(6).                    NC2234.2
005300 01  NUM-1234                    PIC     9(4) VALUE 1234.         NC2234.2
005400 01  TEST-8-DATA-1               PIC     $(3)9.99.                NC2234.2
005500 01  TEST-8-DATA-2               PIC     A(10).                   NC2234.2
005600 01  TEST-RESULTS.                                                NC2234.2
005700     02 FILLER                   PIC X      VALUE SPACE.          NC2234.2
005800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2234.2
005900     02 FILLER                   PIC X      VALUE SPACE.          NC2234.2
006000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2234.2
006100     02 FILLER                   PIC X      VALUE SPACE.          NC2234.2
006200     02  PAR-NAME.                                                NC2234.2
006300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2234.2
006400       03  PARDOT-X              PIC X      VALUE SPACE.          NC2234.2
006500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2234.2
006600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2234.2
006700     02 RE-MARK                  PIC X(61).                       NC2234.2
006800 01  TEST-COMPUTED.                                               NC2234.2
006900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2234.2
007000     02 FILLER                   PIC X(17)  VALUE                 NC2234.2
007100            "       COMPUTED=".                                   NC2234.2
007200     02 COMPUTED-X.                                               NC2234.2
007300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2234.2
007400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2234.2
007500                                 PIC -9(9).9(9).                  NC2234.2
007600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2234.2
007700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2234.2
007800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2234.2
007900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2234.2
008000         04 COMPUTED-18V0                    PIC -9(18).          NC2234.2
008100         04 FILLER                           PIC X.               NC2234.2
008200     03 FILLER PIC X(50) VALUE SPACE.                             NC2234.2
008300 01  TEST-CORRECT.                                                NC2234.2
008400     02 FILLER PIC X(30) VALUE SPACE.                             NC2234.2
008500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2234.2
008600     02 CORRECT-X.                                                NC2234.2
008700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2234.2
008800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2234.2
008900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2234.2
009000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2234.2
009100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2234.2
009200     03      CR-18V0 REDEFINES CORRECT-A.                         NC2234.2
009300         04 CORRECT-18V0                     PIC -9(18).          NC2234.2
009400         04 FILLER                           PIC X.               NC2234.2
009500     03 FILLER PIC X(2) VALUE SPACE.                              NC2234.2
009600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2234.2
009700 01  CCVS-C-1.                                                    NC2234.2
009800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2234.2
009900-    "SS  PARAGRAPH-NAME                                          NC2234.2
010000-    "       REMARKS".                                            NC2234.2
010100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2234.2
010200 01  CCVS-C-2.                                                    NC2234.2
010300     02 FILLER                     PIC X        VALUE SPACE.      NC2234.2
010400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2234.2
010500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2234.2
010600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2234.2
010700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2234.2
010800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2234.2
010900 01  REC-CT                        PIC 99       VALUE ZERO.       NC2234.2
011000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2234.2
011100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2234.2
011200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2234.2
011300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2234.2
011400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2234.2
011500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2234.2
011600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2234.2
011700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2234.2
011800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2234.2
011900 01  CCVS-H-1.                                                    NC2234.2
012000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2234.2
012100     02  FILLER                    PIC X(42)    VALUE             NC2234.2
012200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2234.2
012300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2234.2
012400 01  CCVS-H-2A.                                                   NC2234.2
012500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2234.2
012600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2234.2
012700   02  FILLER                        PIC XXXX   VALUE             NC2234.2
012800     "4.2 ".                                                      NC2234.2
012900   02  FILLER                        PIC X(28)  VALUE             NC2234.2
013000            " COPY - NOT FOR DISTRIBUTION".                       NC2234.2
013100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2234.2
013200                                                                  NC2234.2
013300 01  CCVS-H-2B.                                                   NC2234.2
013400   02  FILLER                        PIC X(15)  VALUE             NC2234.2
013500            "TEST RESULT OF ".                                    NC2234.2
013600   02  TEST-ID                       PIC X(9).                    NC2234.2
013700   02  FILLER                        PIC X(4)   VALUE             NC2234.2
013800            " IN ".                                               NC2234.2
013900   02  FILLER                        PIC X(12)  VALUE             NC2234.2
014000     " HIGH       ".                                              NC2234.2
014100   02  FILLER                        PIC X(22)  VALUE             NC2234.2
014200            " LEVEL VALIDATION FOR ".                             NC2234.2
014300   02  FILLER                        PIC X(58)  VALUE             NC2234.2
014400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2234.2
014500 01  CCVS-H-3.                                                    NC2234.2
014600     02  FILLER                      PIC X(34)  VALUE             NC2234.2
014700            " FOR OFFICIAL USE ONLY    ".                         NC2234.2
014800     02  FILLER                      PIC X(58)  VALUE             NC2234.2
014900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2234.2
015000     02  FILLER                      PIC X(28)  VALUE             NC2234.2
015100            "  COPYRIGHT   1985 ".                                NC2234.2
015200 01  CCVS-E-1.                                                    NC2234.2
015300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2234.2
015400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2234.2
015500     02 ID-AGAIN                     PIC X(9).                    NC2234.2
015600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2234.2
015700 01  CCVS-E-2.                                                    NC2234.2
015800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2234.2
015900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2234.2
016000     02 CCVS-E-2-2.                                               NC2234.2
016100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2234.2
016200         03 FILLER                   PIC X      VALUE SPACE.      NC2234.2
016300         03 ENDER-DESC               PIC X(44)  VALUE             NC2234.2
016400            "ERRORS ENCOUNTERED".                                 NC2234.2
016500 01  CCVS-E-3.                                                    NC2234.2
016600     02  FILLER                      PIC X(22)  VALUE             NC2234.2
016700            " FOR OFFICIAL USE ONLY".                             NC2234.2
016800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2234.2
016900     02  FILLER                      PIC X(58)  VALUE             NC2234.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2234.2
017100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2234.2
017200     02 FILLER                       PIC X(15)  VALUE             NC2234.2
017300             " COPYRIGHT 1985".                                   NC2234.2
017400 01  CCVS-E-4.                                                    NC2234.2
017500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2234.2
017600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2234.2
017700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2234.2
017800     02 FILLER                       PIC X(40)  VALUE             NC2234.2
017900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2234.2
018000 01  XXINFO.                                                      NC2234.2
018100     02 FILLER                       PIC X(19)  VALUE             NC2234.2
018200            "*** INFORMATION ***".                                NC2234.2
018300     02 INFO-TEXT.                                                NC2234.2
018400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2234.2
018500       04 XXCOMPUTED                 PIC X(20).                   NC2234.2
018600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2234.2
018700       04 XXCORRECT                  PIC X(20).                   NC2234.2
018800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2234.2
018900 01  HYPHEN-LINE.                                                 NC2234.2
019000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2234.2
019100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2234.2
019200-    "*****************************************".                 NC2234.2
019300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2234.2
019400-    "******************************".                            NC2234.2
019500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2234.2
019600     "NC223A".                                                    NC2234.2
019700 PROCEDURE DIVISION.                                              NC2234.2
019800 CCVS1 SECTION.                                                   NC2234.2
019900 OPEN-FILES.                                                      NC2234.2
020000     OPEN     OUTPUT PRINT-FILE.                                  NC2234.2
020100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2234.2
020200     MOVE    SPACE TO TEST-RESULTS.                               NC2234.2
020300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2234.2
020400     GO TO CCVS1-EXIT.                                            NC2234.2
020500 CLOSE-FILES.                                                     NC2234.2
020600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2234.2
020700 TERMINATE-CCVS.                                                  NC2234.2
020800*    EXIT PROGRAM.                                                NC2234.2
020900*TERMINATE-CALL.                                                  NC2234.2
021000     STOP     RUN.                                                NC2234.2
021100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2234.2
021200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2234.2
021300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2234.2
021400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2234.2
021500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2234.2
021600 PRINT-DETAIL.                                                    NC2234.2
021700     IF REC-CT NOT EQUAL TO ZERO                                  NC2234.2
021800             MOVE "." TO PARDOT-X                                 NC2234.2
021900             MOVE REC-CT TO DOTVALUE.                             NC2234.2
022000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2234.2
022100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2234.2
022200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2234.2
022300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2234.2
022400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2234.2
022500     MOVE SPACE TO CORRECT-X.                                     NC2234.2
022600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2234.2
022700     MOVE     SPACE TO RE-MARK.                                   NC2234.2
022800 HEAD-ROUTINE.                                                    NC2234.2
022900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2234.2
023000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2234.2
023100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2234.2
023200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2234.2
023300 COLUMN-NAMES-ROUTINE.                                            NC2234.2
023400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2234.2
023500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2234.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2234.2
023700 END-ROUTINE.                                                     NC2234.2
023800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2234.2
023900 END-RTN-EXIT.                                                    NC2234.2
024000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2234.2
024100 END-ROUTINE-1.                                                   NC2234.2
024200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2234.2
024300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2234.2
024400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2234.2
024500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2234.2
024600                                                                  NC2234.2
024700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2234.2
024800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2234.2
024900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2234.2
025000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2234.2
025100  END-ROUTINE-12.                                                 NC2234.2
025200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2234.2
025300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2234.2
025400         MOVE "NO " TO ERROR-TOTAL                                NC2234.2
025500         ELSE                                                     NC2234.2
025600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2234.2
025700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2234.2
025800     PERFORM WRITE-LINE.                                          NC2234.2
025900 END-ROUTINE-13.                                                  NC2234.2
026000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2234.2
026100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2234.2
026200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2234.2
026300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2234.2
026400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2234.2
026500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2234.2
026600          MOVE "NO " TO ERROR-TOTAL                               NC2234.2
026700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2234.2
026800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2234.2
026900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2234.2
027000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2234.2
027100 WRITE-LINE.                                                      NC2234.2
027200     ADD 1 TO RECORD-COUNT.                                       NC2234.2
027300*    IF RECORD-COUNT GREATER 50                                   NC2234.2
027400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2234.2
027500*        MOVE SPACE TO DUMMY-RECORD                               NC2234.2
027600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2234.2
027700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2234.2
027800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2234.2
027900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2234.2
028000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2234.2
028100*        MOVE ZERO TO RECORD-COUNT.                               NC2234.2
028200     PERFORM WRT-LN.                                              NC2234.2
028300 WRT-LN.                                                          NC2234.2
028400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2234.2
028500     MOVE SPACE TO DUMMY-RECORD.                                  NC2234.2
028600 BLANK-LINE-PRINT.                                                NC2234.2
028700     PERFORM WRT-LN.                                              NC2234.2
028800 FAIL-ROUTINE.                                                    NC2234.2
028900     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2234.2
029000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2234.2
029100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2234.2
029200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2234.2
029300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2234.2
029400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2234.2
029500     GO TO  FAIL-ROUTINE-EX.                                      NC2234.2
029600 FAIL-ROUTINE-WRITE.                                              NC2234.2
029700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2234.2
029800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2234.2
029900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2234.2
030000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2234.2
030100 FAIL-ROUTINE-EX. EXIT.                                           NC2234.2
030200 BAIL-OUT.                                                        NC2234.2
030300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2234.2
030400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2234.2
030500 BAIL-OUT-WRITE.                                                  NC2234.2
030600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2234.2
030700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2234.2
030800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2234.2
030900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2234.2
031000 BAIL-OUT-EX. EXIT.                                               NC2234.2
031100 CCVS1-EXIT.                                                      NC2234.2
031200     EXIT.                                                        NC2234.2
031300 SECT-NC223A-001 SECTION.                                         NC2234.2
031400*                                                                 NC2234.2
031500 INI-INIT-GF-1.                                                   NC2234.2
031600     MOVE   "INITIALIZE STATEMENT" TO FEATURE.                    NC2234.2
031700     MOVE   "VI-92 6.16.2 GR5" TO ANSI-REFERENCE.                 NC2234.2
031800     MOVE    1 TO REC-CT.                                         NC2234.2
031900 INI-TEST-GF-1-0.                                                 NC2234.2
032000     INITIALIZE TEST-1-DATA.                                      NC2234.2
032100     GO TO   INI-TEST-GF-1-1.                                     NC2234.2
032200 INI-DELETE-GF-1.                                                 NC2234.2
032300     PERFORM DE-LETE.                                             NC2234.2
032400     PERFORM PRINT-DETAIL.                                        NC2234.2
032500     GO TO   INI-INIT-GF-2.                                       NC2234.2
032600 INI-TEST-GF-1-1.                                                 NC2234.2
032700     MOVE   "INI-TEST-GF-1-1" TO PAR-NAME.                        NC2234.2
032800     IF      TEST-1-1 = ZERO                                      NC2234.2
032900             PERFORM PASS                                         NC2234.2
033000             GO TO INI-WRITE-GF-1-1                               NC2234.2
033100     ELSE                                                         NC2234.2
033200             GO TO INI-FAIL-GF-1-1.                               NC2234.2
033300 INI-DELETE-GF-1-1.                                               NC2234.2
033400     PERFORM DE-LETE.                                             NC2234.2
033500     GO TO INI-WRITE-GF-1-1.                                      NC2234.2
033600 INI-FAIL-GF-1-1.                                                 NC2234.2
033700     MOVE    ZERO     TO CORRECT-N                                NC2234.2
033800     MOVE    TEST-1-1 TO COMPUTED-X                               NC2234.2
033900     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
034000     PERFORM FAIL.                                                NC2234.2
034100 INI-WRITE-GF-1-1.                                                NC2234.2
034200     PERFORM PRINT-DETAIL.                                        NC2234.2
034300*                                                                 NC2234.2
034400 INI-TEST-GF-1-2.                                                 NC2234.2
034500     ADD     1 TO REC-CT.                                         NC2234.2
034600     MOVE   "INI-TEST-GF-1-2" TO PAR-NAME.                        NC2234.2
034700     IF      TEST-1-2 = "  $0.00"                                 NC2234.2
034800             PERFORM PASS                                         NC2234.2
034900             GO TO INI-WRITE-GF-1-2                               NC2234.2
035000     ELSE                                                         NC2234.2
035100             GO TO INI-FAIL-GF-1-2.                               NC2234.2
035200 INI-DELETE-GF-1-2.                                               NC2234.2
035300     PERFORM DE-LETE.                                             NC2234.2
035400     GO TO INI-WRITE-GF-1-2.                                      NC2234.2
035500 INI-FAIL-GF-1-2.                                                 NC2234.2
035600     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
035700     MOVE    TEST-1-2 TO COMPUTED-X                               NC2234.2
035800     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
035900     PERFORM FAIL.                                                NC2234.2
036000 INI-WRITE-GF-1-2.                                                NC2234.2
036100     PERFORM PRINT-DETAIL.                                        NC2234.2
036200*                                                                 NC2234.2
036300 INI-TEST-GF-1-3.                                                 NC2234.2
036400     ADD     1 TO REC-CT.                                         NC2234.2
036500     MOVE   "INI-TEST-GF-1-3" TO PAR-NAME.                        NC2234.2
036600     IF      TEST-1-3 = SPACES                                    NC2234.2
036700             PERFORM PASS                                         NC2234.2
036800             GO TO INI-WRITE-GF-1-3                               NC2234.2
036900     ELSE                                                         NC2234.2
037000             GO TO INI-FAIL-GF-1-3.                               NC2234.2
037100 INI-DELETE-GF-1-3.                                               NC2234.2
037200     PERFORM DE-LETE.                                             NC2234.2
037300     GO TO INI-WRITE-GF-1-3.                                      NC2234.2
037400 INI-FAIL-GF-1-3.                                                 NC2234.2
037500     MOVE    SPACES   TO CORRECT-X                                NC2234.2
037600     MOVE    TEST-1-3 TO COMPUTED-X                               NC2234.2
037700     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
037800     PERFORM FAIL.                                                NC2234.2
037900 INI-WRITE-GF-1-3.                                                NC2234.2
038000     PERFORM PRINT-DETAIL.                                        NC2234.2
038100*                                                                 NC2234.2
038200 INI-TEST-GF-1-4.                                                 NC2234.2
038300     ADD     1 TO REC-CT.                                         NC2234.2
038400     MOVE   "INI-TEST-GF-1-4" TO PAR-NAME.                        NC2234.2
038500     IF      TEST-1-4 = "     /  "                                NC2234.2
038600             PERFORM PASS                                         NC2234.2
038700             GO TO INI-WRITE-GF-1-4                               NC2234.2
038800     ELSE                                                         NC2234.2
038900             GO TO INI-FAIL-GF-1-4.                               NC2234.2
039000 INI-DELETE-GF-1-4.                                               NC2234.2
039100     PERFORM DE-LETE.                                             NC2234.2
039200     GO TO INI-WRITE-GF-1-4.                                      NC2234.2
039300 INI-FAIL-GF-1-4.                                                 NC2234.2
039400     MOVE   "     /  " TO CORRECT-X                               NC2234.2
039500     MOVE    TEST-1-4  TO COMPUTED-X                              NC2234.2
039600     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
039700     PERFORM FAIL.                                                NC2234.2
039800 INI-WRITE-GF-1-4.                                                NC2234.2
039900     PERFORM PRINT-DETAIL.                                        NC2234.2
040000*                                                                 NC2234.2
040100 INI-TEST-GF-1-5.                                                 NC2234.2
040200     ADD     1 TO REC-CT.                                         NC2234.2
040300     MOVE   "INI-TEST-GF-1-5" TO PAR-NAME.                        NC2234.2
040400     IF      TEST-1-5 = SPACES                                    NC2234.2
040500             PERFORM PASS                                         NC2234.2
040600             GO TO INI-WRITE-GF-1-5                               NC2234.2
040700     ELSE                                                         NC2234.2
040800             GO TO INI-FAIL-GF-1-5.                               NC2234.2
040900 INI-DELETE-GF-1-5.                                               NC2234.2
041000     PERFORM DE-LETE.                                             NC2234.2
041100     GO TO INI-WRITE-GF-1-5.                                      NC2234.2
041200 INI-FAIL-GF-1-5.                                                 NC2234.2
041300             MOVE    SPACES   TO CORRECT-X                        NC2234.2
041400             MOVE    TEST-1-5 TO COMPUTED-X                       NC2234.2
041500             MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK          NC2234.2
041600             PERFORM FAIL.                                        NC2234.2
041700 INI-WRITE-GF-1-5.                                                NC2234.2
041800             PERFORM PRINT-DETAIL.                                NC2234.2
041900*                                                                 NC2234.2
042000 INI-TEST-GF-1-6.                                                 NC2234.2
042100     ADD     1 TO REC-CT.                                         NC2234.2
042200     MOVE   "INI-TEST-GF-1-6" TO PAR-NAME.                        NC2234.2
042300     IF      TEST-1-6 = ZERO                                      NC2234.2
042400             PERFORM PASS                                         NC2234.2
042500             GO TO INI-WRITE-GF-1-6                               NC2234.2
042600     ELSE                                                         NC2234.2
042700             GO TO INI-FAIL-GF-1-6.                               NC2234.2
042800 INI-DELETE-GF-1-6.                                               NC2234.2
042900     PERFORM DE-LETE.                                             NC2234.2
043000     GO TO INI-WRITE-GF-1-6.                                      NC2234.2
043100 INI-FAIL-GF-1-6.                                                 NC2234.2
043200     MOVE    ZERO     TO CORRECT-N                                NC2234.2
043300     MOVE    TEST-1-6 TO COMPUTED-X                               NC2234.2
043400     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
043500     PERFORM FAIL.                                                NC2234.2
043600 INI-WRITE-GF-1-6.                                                NC2234.2
043700     PERFORM PRINT-DETAIL.                                        NC2234.2
043800*                                                                 NC2234.2
043900 INI-TEST-GF-1-7.                                                 NC2234.2
044000     ADD     1 TO REC-CT.                                         NC2234.2
044100     MOVE   "INI-TEST-GF-1-7" TO PAR-NAME.                        NC2234.2
044200     IF      TEST-1-7 = "  $0.00"                                 NC2234.2
044300             PERFORM PASS                                         NC2234.2
044400             GO TO INI-WRITE-GF-1-7                               NC2234.2
044500     ELSE                                                         NC2234.2
044600             GO TO INI-FAIL-GF-1-7.                               NC2234.2
044700 INI-DELETE-GF-1-7.                                               NC2234.2
044800     PERFORM DE-LETE.                                             NC2234.2
044900     GO TO INI-WRITE-GF-1-7.                                      NC2234.2
045000 INI-FAIL-GF-1-7.                                                 NC2234.2
045100     MOVE   "  $0.00"  TO CORRECT-X.                              NC2234.2
045200     MOVE    TEST-1-7 TO COMPUTED-X.                              NC2234.2
045300     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK.                 NC2234.2
045400     PERFORM FAIL.                                                NC2234.2
045500 INI-WRITE-GF-1-7.                                                NC2234.2
045600     PERFORM PRINT-DETAIL.                                        NC2234.2
045700*                                                                 NC2234.2
045800 INI-TEST-GF-1-8.                                                 NC2234.2
045900     ADD     1 TO REC-CT.                                         NC2234.2
046000     MOVE   "INI-TEST-GF-1-8" TO PAR-NAME.                        NC2234.2
046100     IF      TEST-1-8 = SPACES                                    NC2234.2
046200             PERFORM PASS                                         NC2234.2
046300             GO TO INI-WRITE-GF-1-8                               NC2234.2
046400     ELSE                                                         NC2234.2
046500             GO TO INI-FAIL-GF-1-8.                               NC2234.2
046600 INI-DELETE-GF-1-8.                                               NC2234.2
046700     PERFORM DE-LETE.                                             NC2234.2
046800     GO TO INI-WRITE-GF-1-8.                                      NC2234.2
046900 INI-FAIL-GF-1-8.                                                 NC2234.2
047000     MOVE    SPACES   TO CORRECT-X                                NC2234.2
047100     MOVE    TEST-1-8 TO COMPUTED-X                               NC2234.2
047200     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
047300     PERFORM FAIL.                                                NC2234.2
047400 INI-WRITE-GF-1-8.                                                NC2234.2
047500     PERFORM PRINT-DETAIL.                                        NC2234.2
047600*                                                                 NC2234.2
047700 INI-TEST-GF-1-9.                                                 NC2234.2
047800     ADD     1 TO REC-CT.                                         NC2234.2
047900     MOVE   "INI-TEST-GF-1-9" TO PAR-NAME.                        NC2234.2
048000     IF      TEST-1-9 = "     /  "                                NC2234.2
048100             PERFORM PASS                                         NC2234.2
048200             GO TO INI-WRITE-GF-1-9                               NC2234.2
048300     ELSE                                                         NC2234.2
048400             GO TO INI-FAIL-GF-1-9.                               NC2234.2
048500 INI-DELETE-GF-1-9.                                               NC2234.2
048600     PERFORM DE-LETE.                                             NC2234.2
048700     GO TO INI-WRITE-GF-1-9.                                      NC2234.2
048800 INI-FAIL-GF-1-9.                                                 NC2234.2
048900     MOVE   "     /  " TO CORRECT-X                               NC2234.2
049000     MOVE    TEST-1-9  TO COMPUTED-X                              NC2234.2
049100     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
049200     PERFORM FAIL.                                                NC2234.2
049300 INI-WRITE-GF-1-9.                                                NC2234.2
049400     PERFORM PRINT-DETAIL.                                        NC2234.2
049500*                                                                 NC2234.2
049600 INI-TEST-GF-1-10.                                                NC2234.2
049700     ADD     1 TO REC-CT.                                         NC2234.2
049800     MOVE   "INI-TEST-GF-1-10" TO PAR-NAME.                       NC2234.2
049900     IF      TEST-1-10 = SPACES                                   NC2234.2
050000             PERFORM PASS                                         NC2234.2
050100             GO TO INI-WRITE-GF-1-10                              NC2234.2
050200     ELSE                                                         NC2234.2
050300             GO TO INI-FAIL-GF-1-10.                              NC2234.2
050400 INI-DELETE-GF-1-10.                                              NC2234.2
050500     PERFORM DE-LETE.                                             NC2234.2
050600     GO TO INI-WRITE-GF-1-10.                                     NC2234.2
050700 INI-FAIL-GF-1-10.                                                NC2234.2
050800     MOVE    SPACES    TO CORRECT-X                               NC2234.2
050900     MOVE    TEST-1-10 TO COMPUTED-X                              NC2234.2
051000     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
051100     PERFORM FAIL.                                                NC2234.2
051200 INI-WRITE-GF-1-10.                                               NC2234.2
051300     PERFORM PRINT-DETAIL.                                        NC2234.2
051400*                                                                 NC2234.2
051500 INI-INIT-GF-2.                                                   NC2234.2
051600     MOVE   "VI-91/2 6.16.2 GR2" TO ANSI-REFERENCE.               NC2234.2
051700     MOVE   "INITIALIZE STATEMENT" TO FEATURE.                    NC2234.2
051800     MOVE    1 TO REC-CT.                                         NC2234.2
051900 INI-TEST-GF-2-0.                                                 NC2234.2
052000     INITIALIZE TEST-1-DATA                                       NC2234.2
052100             REPLACING ALPHABETIC DATA BY "AAAAAA".               NC2234.2
052200     GO TO   INI-TEST-GF-2-1.                                     NC2234.2
052300 INI-DELETE-GF-2.                                                 NC2234.2
052400     PERFORM DE-LETE.                                             NC2234.2
052500     PERFORM PRINT-DETAIL.                                        NC2234.2
052600     GO TO   INI-INIT-GF-3.                                       NC2234.2
052700*                                                                 NC2234.2
052800 INI-TEST-GF-2-1.                                                 NC2234.2
052900     MOVE   "INI-TEST-GF-2-1" TO PAR-NAME.                        NC2234.2
053000     IF      TEST-1-5 = "AAAAAA"                                  NC2234.2
053100             PERFORM PASS                                         NC2234.2
053200             GO TO INI-WRITE-GF-2-1                               NC2234.2
053300     ELSE                                                         NC2234.2
053400             GO TO INI-FAIL-GF-2-1.                               NC2234.2
053500 INI-DELETE-GF-2-1.                                               NC2234.2
053600     PERFORM DE-LETE.                                             NC2234.2
053700     GO TO INI-WRITE-GF-2-1.                                      NC2234.2
053800 INI-FAIL-GF-2-1.                                                 NC2234.2
053900     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
054000     MOVE    TEST-1-5 TO COMPUTED-X                               NC2234.2
054100     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
054200     PERFORM FAIL.                                                NC2234.2
054300 INI-WRITE-GF-2-1.                                                NC2234.2
054400     PERFORM PRINT-DETAIL.                                        NC2234.2
054500*                                                                 NC2234.2
054600 INI-TEST-GF-2-2.                                                 NC2234.2
054700     ADD 1 TO REC-CT.                                             NC2234.2
054800     MOVE   "INI-TEST-GF-2-2" TO PAR-NAME.                        NC2234.2
054900     IF      TEST-1-10 = "AAAAAA"                                 NC2234.2
055000             PERFORM PASS                                         NC2234.2
055100             GO TO INI-WRITE-GF-2-2                               NC2234.2
055200     ELSE                                                         NC2234.2
055300             GO TO INI-FAIL-GF-2-2.                               NC2234.2
055400 INI-DELETE-GF-2-2.                                               NC2234.2
055500     PERFORM DE-LETE.                                             NC2234.2
055600     GO TO INI-WRITE-GF-2-2.                                      NC2234.2
055700 INI-FAIL-GF-2-2.                                                 NC2234.2
055800     MOVE   "AAAAAA"   TO CORRECT-X                               NC2234.2
055900     MOVE    TEST-1-10 TO COMPUTED-X                              NC2234.2
056000     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
056100     PERFORM FAIL.                                                NC2234.2
056200 INI-WRITE-GF-2-2.                                                NC2234.2
056300     PERFORM PRINT-DETAIL.                                        NC2234.2
056400*                                                                 NC2234.2
056500 INI-TEST-GF-2-3.                                                 NC2234.2
056600     ADD 1 TO REC-CT.                                             NC2234.2
056700     MOVE   "INI-TEST-GF-2-3" TO PAR-NAME.                        NC2234.2
056800     IF      TEST-1-1 = ZERO                                      NC2234.2
056900             PERFORM PASS                                         NC2234.2
057000             GO TO INI-WRITE-GF-2-3                               NC2234.2
057100     ELSE                                                         NC2234.2
057200             GO TO INI-FAIL-GF-2-3.                               NC2234.2
057300 INI-DELETE-GF-2-3.                                               NC2234.2
057400     PERFORM DE-LETE.                                             NC2234.2
057500     GO TO INI-WRITE-GF-2-3.                                      NC2234.2
057600 INI-FAIL-GF-2-3.                                                 NC2234.2
057700     MOVE    ZERO     TO CORRECT-N                                NC2234.2
057800     MOVE    TEST-1-1 TO COMPUTED-X                               NC2234.2
057900     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
058000          TO RE-MARK                                              NC2234.2
058100     PERFORM FAIL.                                                NC2234.2
058200 INI-WRITE-GF-2-3.                                                NC2234.2
058300     PERFORM PRINT-DETAIL.                                        NC2234.2
058400*                                                                 NC2234.2
058500 INI-TEST-GF-2-4.                                                 NC2234.2
058600     ADD     1 TO REC-CT.                                         NC2234.2
058700     MOVE   "INI-TEST-GF-2-4" TO PAR-NAME.                        NC2234.2
058800     IF      TEST-1-2 = "  $0.00"                                 NC2234.2
058900             PERFORM PASS                                         NC2234.2
059000             GO TO INI-WRITE-GF-2-4                               NC2234.2
059100     ELSE                                                         NC2234.2
059200             GO TO INI-FAIL-GF-2-4.                               NC2234.2
059300 INI-DELETE-GF-2-4.                                               NC2234.2
059400     PERFORM DE-LETE.                                             NC2234.2
059500     GO TO INI-WRITE-GF-2-4.                                      NC2234.2
059600 INI-FAIL-GF-2-4.                                                 NC2234.2
059700     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
059800     MOVE    TEST-1-2 TO COMPUTED-X                               NC2234.2
059900     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
060000          TO RE-MARK                                              NC2234.2
060100     PERFORM FAIL.                                                NC2234.2
060200 INI-WRITE-GF-2-4.                                                NC2234.2
060300     PERFORM PRINT-DETAIL.                                        NC2234.2
060400*                                                                 NC2234.2
060500 INI-TEST-GF-2-5.                                                 NC2234.2
060600     ADD     1 TO REC-CT.                                         NC2234.2
060700     MOVE   "INI-TEST-GF-2-5" TO PAR-NAME.                        NC2234.2
060800     IF      TEST-1-3 = SPACES                                    NC2234.2
060900             PERFORM PASS                                         NC2234.2
061000             GO TO INI-WRITE-GF-2-5                               NC2234.2
061100     ELSE                                                         NC2234.2
061200             GO TO INI-FAIL-GF-2-5.                               NC2234.2
061300 INI-DELETE-GF-2-5.                                               NC2234.2
061400     PERFORM DE-LETE.                                             NC2234.2
061500     GO TO INI-WRITE-GF-2-5.                                      NC2234.2
061600 INI-FAIL-GF-2-5.                                                 NC2234.2
061700     MOVE    SPACES   TO CORRECT-X                                NC2234.2
061800     MOVE    TEST-1-3 TO COMPUTED-X                               NC2234.2
061900     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
062000          TO RE-MARK                                              NC2234.2
062100     PERFORM FAIL.                                                NC2234.2
062200 INI-WRITE-GF-2-5.                                                NC2234.2
062300     PERFORM PRINT-DETAIL.                                        NC2234.2
062400*                                                                 NC2234.2
062500 INI-TEST-GF-2-6.                                                 NC2234.2
062600     ADD     1 TO REC-CT.                                         NC2234.2
062700     MOVE   "INI-TEST-GF-2-6" TO PAR-NAME.                        NC2234.2
062800     IF      TEST-1-4 = "     /  "                                NC2234.2
062900             PERFORM PASS                                         NC2234.2
063000             GO TO INI-WRITE-GF-2-6                               NC2234.2
063100     ELSE                                                         NC2234.2
063200             GO TO INI-FAIL-GF-2-6.                               NC2234.2
063300 INI-DELETE-GF-2-6.                                               NC2234.2
063400     PERFORM DE-LETE.                                             NC2234.2
063500     GO TO INI-WRITE-GF-2-6.                                      NC2234.2
063600 INI-FAIL-GF-2-6.                                                 NC2234.2
063700     MOVE   "     /  " TO CORRECT-X                               NC2234.2
063800     MOVE    TEST-1-4  TO COMPUTED-X                              NC2234.2
063900     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
064000          TO RE-MARK                                              NC2234.2
064100     PERFORM FAIL.                                                NC2234.2
064200 INI-WRITE-GF-2-6.                                                NC2234.2
064300     PERFORM PRINT-DETAIL.                                        NC2234.2
064400*                                                                 NC2234.2
064500 INI-TEST-GF-2-7.                                                 NC2234.2
064600     ADD     1 TO REC-CT.                                         NC2234.2
064700     MOVE   "INI-TEST-GF-2-7" TO PAR-NAME.                        NC2234.2
064800     IF      TEST-1-6 = ZERO                                      NC2234.2
064900             PERFORM PASS                                         NC2234.2
065000             GO TO INI-WRITE-GF-2-7                               NC2234.2
065100     ELSE                                                         NC2234.2
065200             GO TO INI-FAIL-GF-2-7.                               NC2234.2
065300 INI-DELETE-GF-2-7.                                               NC2234.2
065400     PERFORM DE-LETE.                                             NC2234.2
065500     GO TO INI-WRITE-GF-2-7.                                      NC2234.2
065600 INI-FAIL-GF-2-7.                                                 NC2234.2
065700     MOVE    ZERO     TO CORRECT-N                                NC2234.2
065800     MOVE    TEST-1-6 TO COMPUTED-X                               NC2234.2
065900     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
066000          TO RE-MARK                                              NC2234.2
066100     PERFORM FAIL.                                                NC2234.2
066200 INI-WRITE-GF-2-7.                                                NC2234.2
066300     PERFORM PRINT-DETAIL.                                        NC2234.2
066400*                                                                 NC2234.2
066500 INI-TEST-GF-2-8.                                                 NC2234.2
066600     ADD     1 TO REC-CT.                                         NC2234.2
066700     MOVE   "INI-TEST-GF-2-8" TO PAR-NAME.                        NC2234.2
066800     IF      TEST-1-7 = "  $0.00"                                 NC2234.2
066900             PERFORM PASS                                         NC2234.2
067000             GO TO INI-WRITE-GF-2-8                               NC2234.2
067100     ELSE                                                         NC2234.2
067200             GO TO INI-FAIL-GF-2-8.                               NC2234.2
067300 INI-DELETE-GF-2-8.                                               NC2234.2
067400     PERFORM DE-LETE.                                             NC2234.2
067500     GO TO INI-WRITE-GF-2-8.                                      NC2234.2
067600 INI-FAIL-GF-2-8.                                                 NC2234.2
067700     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
067800     MOVE    TEST-1-7 TO COMPUTED-X                               NC2234.2
067900     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
068000          TO RE-MARK                                              NC2234.2
068100     PERFORM FAIL.                                                NC2234.2
068200 INI-WRITE-GF-2-8.                                                NC2234.2
068300     PERFORM PRINT-DETAIL.                                        NC2234.2
068400*                                                                 NC2234.2
068500 INI-TEST-GF-2-9.                                                 NC2234.2
068600     ADD     1 TO REC-CT.                                         NC2234.2
068700     MOVE   "INI-TEST-GF-2-9" TO PAR-NAME.                        NC2234.2
068800     IF      TEST-1-8 = SPACES                                    NC2234.2
068900             PERFORM PASS                                         NC2234.2
069000             GO TO INI-WRITE-GF-2-9                               NC2234.2
069100     ELSE                                                         NC2234.2
069200             GO TO INI-FAIL-GF-2-9.                               NC2234.2
069300 INI-DELETE-GF-2-9.                                               NC2234.2
069400     PERFORM DE-LETE.                                             NC2234.2
069500     GO TO INI-WRITE-GF-2-9.                                      NC2234.2
069600 INI-FAIL-GF-2-9.                                                 NC2234.2
069700     MOVE    SPACES   TO CORRECT-X                                NC2234.2
069800     MOVE    TEST-1-8 TO COMPUTED-X                               NC2234.2
069900     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
070000          TO RE-MARK                                              NC2234.2
070100     PERFORM FAIL.                                                NC2234.2
070200 INI-WRITE-GF-2-9.                                                NC2234.2
070300     PERFORM PRINT-DETAIL.                                        NC2234.2
070400*                                                                 NC2234.2
070500 INI-TEST-GF-2-10.                                                NC2234.2
070600     ADD     1 TO REC-CT.                                         NC2234.2
070700     MOVE   "INI-TEST-GF-2-10" TO PAR-NAME.                       NC2234.2
070800     IF      TEST-1-9 = "     /  "                                NC2234.2
070900             PERFORM PASS                                         NC2234.2
071000             GO TO INI-WRITE-GF-2-10                              NC2234.2
071100     ELSE                                                         NC2234.2
071200             GO TO INI-FAIL-GF-2-10.                              NC2234.2
071300 INI-DELETE-GF-2-10.                                              NC2234.2
071400     PERFORM DE-LETE.                                             NC2234.2
071500     GO TO INI-WRITE-GF-2-10.                                     NC2234.2
071600 INI-FAIL-GF-2-10.                                                NC2234.2
071700     MOVE   "     /  " TO CORRECT-X                               NC2234.2
071800     MOVE    TEST-1-9  TO COMPUTED-X                              NC2234.2
071900     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
072000          TO RE-MARK                                              NC2234.2
072100     PERFORM FAIL.                                                NC2234.2
072200 INI-WRITE-GF-2-10.                                               NC2234.2
072300     PERFORM PRINT-DETAIL.                                        NC2234.2
072400*                                                                 NC2234.2
072500 INI-INIT-GF-3.                                                   NC2234.2
072600     MOVE   "VI-91/2 6.16.2 GR2" TO ANSI-REFERENCE.               NC2234.2
072700     MOVE   "INITIALIZE STATEMENT" TO FEATURE.                    NC2234.2
072800     MOVE    1 TO REC-CT.                                         NC2234.2
072900 INI-TEST-GF-3-0.                                                 NC2234.2
073000     INITIALIZE TEST-1-DATA                                       NC2234.2
073100             REPLACING ALPHANUMERIC BY "**********".              NC2234.2
073200     GO TO   INI-TEST-GF-3-1.                                     NC2234.2
073300 INI-DELETE-GF-3.                                                 NC2234.2
073400     PERFORM DE-LETE.                                             NC2234.2
073500     PERFORM PRINT-DETAIL.                                        NC2234.2
073600     GO TO   INI-INIT-GF-4.                                       NC2234.2
073700 INI-TEST-GF-3-1.                                                 NC2234.2
073800     MOVE   "INI-TEST-GF-3-1" TO PAR-NAME.                        NC2234.2
073900     IF      TEST-1-3 = "**********"                              NC2234.2
074000             PERFORM PASS                                         NC2234.2
074100             GO TO INI-WRITE-GF-3-1                               NC2234.2
074200     ELSE                                                         NC2234.2
074300             GO TO INI-FAIL-GF-3-1.                               NC2234.2
074400 INI-DELETE-GF-3-1.                                               NC2234.2
074500     PERFORM DE-LETE.                                             NC2234.2
074600     GO TO INI-WRITE-GF-3-1.                                      NC2234.2
074700 INI-FAIL-GF-3-1.                                                 NC2234.2
074800     MOVE   "**********"  TO CORRECT-X                            NC2234.2
074900     MOVE    TEST-1-3 TO COMPUTED-X                               NC2234.2
075000     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
075100     PERFORM FAIL.                                                NC2234.2
075200 INI-WRITE-GF-3-1.                                                NC2234.2
075300     PERFORM PRINT-DETAIL.                                        NC2234.2
075400*                                                                 NC2234.2
075500 INI-TEST-GF-3-2.                                                 NC2234.2
075600     ADD     1 TO REC-CT.                                         NC2234.2
075700     MOVE   "INI-TEST-GF-3-2" TO PAR-NAME.                        NC2234.2
075800     IF      TEST-1-8 = "**********"                              NC2234.2
075900             PERFORM PASS                                         NC2234.2
076000             GO TO INI-WRITE-GF-3-2                               NC2234.2
076100     ELSE                                                         NC2234.2
076200             GO TO INI-FAIL-GF-3-2.                               NC2234.2
076300 INI-DELETE-GF-3-2.                                               NC2234.2
076400     PERFORM DE-LETE.                                             NC2234.2
076500     GO TO INI-WRITE-GF-3-2.                                      NC2234.2
076600 INI-FAIL-GF-3-2.                                                 NC2234.2
076700     MOVE   "**********"   TO CORRECT-X                           NC2234.2
076800     MOVE    TEST-1-8 TO COMPUTED-X                               NC2234.2
076900     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
077000     PERFORM FAIL.                                                NC2234.2
077100 INI-WRITE-GF-3-2.                                                NC2234.2
077200     PERFORM PRINT-DETAIL.                                        NC2234.2
077300*                                                                 NC2234.2
077400 INI-TEST-GF-3-3.                                                 NC2234.2
077500     ADD     1 TO REC-CT.                                         NC2234.2
077600     MOVE   "INI-TEST-GF-3-3" TO PAR-NAME.                        NC2234.2
077700     IF      TEST-1-1 = ZERO                                      NC2234.2
077800             PERFORM PASS                                         NC2234.2
077900             GO TO INI-WRITE-GF-3-3                               NC2234.2
078000     ELSE                                                         NC2234.2
078100             GO TO INI-FAIL-GF-3-3.                               NC2234.2
078200 INI-DELETE-GF-3-3.                                               NC2234.2
078300     PERFORM DE-LETE.                                             NC2234.2
078400     GO TO INI-WRITE-GF-3-3.                                      NC2234.2
078500 INI-FAIL-GF-3-3.                                                 NC2234.2
078600     MOVE    ZERO     TO CORRECT-N                                NC2234.2
078700     MOVE    TEST-1-1 TO COMPUTED-X                               NC2234.2
078800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
078900          TO RE-MARK                                              NC2234.2
079000     PERFORM FAIL.                                                NC2234.2
079100 INI-WRITE-GF-3-3.                                                NC2234.2
079200     PERFORM PRINT-DETAIL.                                        NC2234.2
079300*                                                                 NC2234.2
079400 INI-TEST-GF-3-4.                                                 NC2234.2
079500     ADD     1 TO REC-CT.                                         NC2234.2
079600     MOVE   "INI-TEST-GF-3-4" TO PAR-NAME.                        NC2234.2
079700     IF      TEST-1-2 = "  $0.00"                                 NC2234.2
079800             PERFORM PASS                                         NC2234.2
079900             GO TO INI-WRITE-GF-3-4                               NC2234.2
080000     ELSE                                                         NC2234.2
080100             GO TO INI-FAIL-GF-3-4.                               NC2234.2
080200 INI-DELETE-GF-3-4.                                               NC2234.2
080300     PERFORM DE-LETE.                                             NC2234.2
080400     GO TO INI-WRITE-GF-3-4.                                      NC2234.2
080500 INI-FAIL-GF-3-4.                                                 NC2234.2
080600     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
080700     MOVE    TEST-1-2 TO COMPUTED-X                               NC2234.2
080800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
080900          TO RE-MARK                                              NC2234.2
081000     PERFORM FAIL.                                                NC2234.2
081100 INI-WRITE-GF-3-4.                                                NC2234.2
081200     PERFORM PRINT-DETAIL.                                        NC2234.2
081300*                                                                 NC2234.2
081400 INI-TEST-GF-3-5.                                                 NC2234.2
081500     ADD     1 TO REC-CT.                                         NC2234.2
081600     MOVE   "INI-TEST-GF-3-5" TO PAR-NAME.                        NC2234.2
081700     IF      TEST-1-4 = "     /  "                                NC2234.2
081800             PERFORM PASS                                         NC2234.2
081900             GO TO INI-WRITE-GF-3-5                               NC2234.2
082000     ELSE                                                         NC2234.2
082100             GO TO INI-FAIL-GF-3-5.                               NC2234.2
082200 INI-DELETE-GF-3-5.                                               NC2234.2
082300     PERFORM DE-LETE.                                             NC2234.2
082400     GO TO INI-WRITE-GF-3-5.                                      NC2234.2
082500 INI-FAIL-GF-3-5.                                                 NC2234.2
082600     MOVE   "     /  " TO CORRECT-X                               NC2234.2
082700     MOVE    TEST-1-4  TO COMPUTED-X                              NC2234.2
082800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
082900          TO RE-MARK                                              NC2234.2
083000     PERFORM FAIL.                                                NC2234.2
083100 INI-WRITE-GF-3-5.                                                NC2234.2
083200     PERFORM PRINT-DETAIL.                                        NC2234.2
083300*                                                                 NC2234.2
083400 INI-TEST-GF-3-6.                                                 NC2234.2
083500     ADD     1 TO REC-CT.                                         NC2234.2
083600     MOVE   "INI-TEST-GF-3-6" TO PAR-NAME.                        NC2234.2
083700     IF      TEST-1-5 = "AAAAAA"                                  NC2234.2
083800             PERFORM PASS                                         NC2234.2
083900             GO TO INI-WRITE-GF-3-6                               NC2234.2
084000     ELSE                                                         NC2234.2
084100             GO TO INI-FAIL-GF-3-6.                               NC2234.2
084200 INI-DELETE-GF-3-6.                                               NC2234.2
084300     PERFORM DE-LETE.                                             NC2234.2
084400     GO TO INI-WRITE-GF-3-6.                                      NC2234.2
084500 INI-FAIL-GF-3-6.                                                 NC2234.2
084600     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
084700     MOVE    TEST-1-5 TO COMPUTED-X                               NC2234.2
084800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
084900          TO RE-MARK                                              NC2234.2
085000     PERFORM FAIL.                                                NC2234.2
085100 INI-WRITE-GF-3-6.                                                NC2234.2
085200     PERFORM PRINT-DETAIL.                                        NC2234.2
085300*                                                                 NC2234.2
085400 INI-TEST-GF-3-7.                                                 NC2234.2
085500     ADD     1 TO REC-CT.                                         NC2234.2
085600     MOVE   "INI-TEST-GF-3-7" TO PAR-NAME.                        NC2234.2
085700     IF      TEST-1-6 = ZERO                                      NC2234.2
085800             PERFORM PASS                                         NC2234.2
085900             GO TO INI-WRITE-GF-3-7                               NC2234.2
086000     ELSE                                                         NC2234.2
086100             GO TO INI-FAIL-GF-3-7.                               NC2234.2
086200 INI-DELETE-GF-3-7.                                               NC2234.2
086300     PERFORM DE-LETE.                                             NC2234.2
086400     GO TO INI-WRITE-GF-3-7.                                      NC2234.2
086500 INI-FAIL-GF-3-7.                                                 NC2234.2
086600     MOVE    ZERO     TO CORRECT-N                                NC2234.2
086700     MOVE    TEST-1-6 TO COMPUTED-X                               NC2234.2
086800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
086900          TO RE-MARK                                              NC2234.2
087000     PERFORM FAIL.                                                NC2234.2
087100 INI-WRITE-GF-3-7.                                                NC2234.2
087200     PERFORM PRINT-DETAIL.                                        NC2234.2
087300*                                                                 NC2234.2
087400 INI-TEST-GF-3-8.                                                 NC2234.2
087500     ADD     1 TO REC-CT.                                         NC2234.2
087600     MOVE   "INI-TEST-GF-3-8" TO PAR-NAME.                        NC2234.2
087700     IF      TEST-1-7 = "  $0.00"                                 NC2234.2
087800             PERFORM PASS                                         NC2234.2
087900             GO TO INI-WRITE-GF-3-8                               NC2234.2
088000     ELSE                                                         NC2234.2
088100             GO TO INI-FAIL-GF-3-8.                               NC2234.2
088200 INI-DELETE-GF-3-8.                                               NC2234.2
088300     PERFORM DE-LETE.                                             NC2234.2
088400     GO TO INI-WRITE-GF-3-8.                                      NC2234.2
088500 INI-FAIL-GF-3-8.                                                 NC2234.2
088600     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
088700     MOVE    TEST-1-7 TO COMPUTED-X                               NC2234.2
088800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
088900          TO RE-MARK                                              NC2234.2
089000     PERFORM FAIL.                                                NC2234.2
089100 INI-WRITE-GF-3-8.                                                NC2234.2
089200     PERFORM PRINT-DETAIL.                                        NC2234.2
089300*                                                                 NC2234.2
089400 INI-TEST-GF-3-9.                                                 NC2234.2
089500     ADD     1 TO REC-CT.                                         NC2234.2
089600     MOVE   "INI-TEST-GF-3-9" TO PAR-NAME.                        NC2234.2
089700     IF      TEST-1-9 = "     /  "                                NC2234.2
089800             PERFORM PASS                                         NC2234.2
089900             GO TO INI-WRITE-GF-3-9                               NC2234.2
090000     ELSE                                                         NC2234.2
090100             GO TO INI-FAIL-GF-3-9.                               NC2234.2
090200 INI-DELETE-GF-3-9.                                               NC2234.2
090300     PERFORM DE-LETE.                                             NC2234.2
090400     GO TO INI-WRITE-GF-3-9.                                      NC2234.2
090500 INI-FAIL-GF-3-9.                                                 NC2234.2
090600     MOVE   "     /  " TO CORRECT-X                               NC2234.2
090700     MOVE    TEST-1-9  TO COMPUTED-X                              NC2234.2
090800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
090900          TO RE-MARK                                              NC2234.2
091000     PERFORM FAIL.                                                NC2234.2
091100 INI-WRITE-GF-3-9.                                                NC2234.2
091200     PERFORM PRINT-DETAIL.                                        NC2234.2
091300*                                                                 NC2234.2
091400 INI-TEST-GF-3-10.                                                NC2234.2
091500     ADD     1 TO REC-CT.                                         NC2234.2
091600     MOVE   "INI-TEST-GF-3-10" TO PAR-NAME.                       NC2234.2
091700     IF      TEST-1-10 = "AAAAAA"                                 NC2234.2
091800             PERFORM PASS                                         NC2234.2
091900             GO TO INI-WRITE-GF-3-10                              NC2234.2
092000     ELSE                                                         NC2234.2
092100             GO TO INI-FAIL-GF-3-10.                              NC2234.2
092200 INI-DELETE-GF-3-10.                                              NC2234.2
092300     PERFORM DE-LETE.                                             NC2234.2
092400     GO TO INI-WRITE-GF-3-10.                                     NC2234.2
092500 INI-FAIL-GF-3-10.                                                NC2234.2
092600     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
092700     MOVE    TEST-1-10 TO COMPUTED-X                              NC2234.2
092800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
092900          TO RE-MARK                                              NC2234.2
093000     PERFORM FAIL.                                                NC2234.2
093100 INI-WRITE-GF-3-10.                                               NC2234.2
093200     PERFORM PRINT-DETAIL.                                        NC2234.2
093300*                                                                 NC2234.2
093400 INI-INIT-GF-4.                                                   NC2234.2
093500     MOVE   "VI-91/2 6.16.2 GR2" TO ANSI-REFERENCE.               NC2234.2
093600     MOVE   "INITIALIZE STATEMENT" TO FEATURE.                    NC2234.2
093700     MOVE    1 TO REC-CT.                                         NC2234.2
093800 INI-TEST-GF-4-0.                                                 NC2234.2
093900     INITIALIZE TEST-1-DATA                                       NC2234.2
094000             REPLACING ALPHANUMERIC-EDITED BY "DDDDDD".           NC2234.2
094100     GO TO   INI-TEST-GF-4-1.                                     NC2234.2
094200 INI-DELETE-GF-4.                                                 NC2234.2
094300     PERFORM DE-LETE.                                             NC2234.2
094400     PERFORM PRINT-DETAIL.                                        NC2234.2
094500     GO TO   INI-INIT-GF-5.                                       NC2234.2
094600 INI-TEST-GF-4-1.                                                 NC2234.2
094700     MOVE   "INI-TEST-GF-4-1" TO PAR-NAME.                        NC2234.2
094800     IF      TEST-1-4 = "DD DD/DD"                                NC2234.2
094900             PERFORM PASS                                         NC2234.2
095000             GO TO INI-WRITE-GF-4-1                               NC2234.2
095100     ELSE                                                         NC2234.2
095200             GO TO INI-FAIL-GF-4-1.                               NC2234.2
095300 INI-DELETE-GF-4-1.                                               NC2234.2
095400     PERFORM DE-LETE.                                             NC2234.2
095500     GO TO INI-WRITE-GF-4-1.                                      NC2234.2
095600 INI-FAIL-GF-4-1.                                                 NC2234.2
095700     MOVE   "DD DD/DD" TO CORRECT-X                               NC2234.2
095800     MOVE    TEST-1-4  TO COMPUTED-X                              NC2234.2
095900     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
096000     PERFORM FAIL.                                                NC2234.2
096100 INI-WRITE-GF-4-1.                                                NC2234.2
096200     PERFORM PRINT-DETAIL.                                        NC2234.2
096300*                                                                 NC2234.2
096400 INI-TEST-GF-4-2.                                                 NC2234.2
096500     ADD     1 TO REC-CT.                                         NC2234.2
096600     MOVE   "INI-TEST-GF-4-2" TO PAR-NAME.                        NC2234.2
096700     IF      TEST-1-9 = "DD DD/DD"                                NC2234.2
096800             PERFORM PASS                                         NC2234.2
096900             GO TO INI-WRITE-GF-4-2                               NC2234.2
097000     ELSE                                                         NC2234.2
097100             GO TO INI-FAIL-GF-4-2.                               NC2234.2
097200 INI-DELETE-GF-4-2.                                               NC2234.2
097300     PERFORM DE-LETE.                                             NC2234.2
097400     GO TO INI-WRITE-GF-4-2.                                      NC2234.2
097500 INI-FAIL-GF-4-2.                                                 NC2234.2
097600     MOVE   "DD DD/DD" TO CORRECT-X                               NC2234.2
097700     MOVE    TEST-1-9  TO COMPUTED-X                              NC2234.2
097800     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
097900     PERFORM FAIL.                                                NC2234.2
098000 INI-WRITE-GF-4-2.                                                NC2234.2
098100     PERFORM PRINT-DETAIL.                                        NC2234.2
098200*                                                                 NC2234.2
098300 INI-TEST-GF-4-3.                                                 NC2234.2
098400     ADD     1 TO REC-CT.                                         NC2234.2
098500     MOVE   "INI-TEST-GF-4-3" TO PAR-NAME.                        NC2234.2
098600     IF      TEST-1-1 = ZERO                                      NC2234.2
098700             PERFORM PASS                                         NC2234.2
098800             GO TO INI-WRITE-GF-4-3                               NC2234.2
098900     ELSE                                                         NC2234.2
099000             GO TO INI-FAIL-GF-4-3.                               NC2234.2
099100 INI-DELETE-GF-4-3.                                               NC2234.2
099200     PERFORM DE-LETE.                                             NC2234.2
099300     GO TO INI-WRITE-GF-4-3.                                      NC2234.2
099400 INI-FAIL-GF-4-3.                                                 NC2234.2
099500     MOVE    ZERO     TO CORRECT-N                                NC2234.2
099600     MOVE    TEST-1-1 TO COMPUTED-X                               NC2234.2
099700     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
099800        TO RE-MARK                                                NC2234.2
099900     PERFORM FAIL.                                                NC2234.2
100000 INI-WRITE-GF-4-3.                                                NC2234.2
100100     PERFORM PRINT-DETAIL.                                        NC2234.2
100200*                                                                 NC2234.2
100300 INI-TEST-GF-4-4.                                                 NC2234.2
100400     ADD     1 TO REC-CT.                                         NC2234.2
100500     MOVE   "INI-TEST-GF-4-4" TO PAR-NAME.                        NC2234.2
100600     IF      TEST-1-2 = "  $0.00"                                 NC2234.2
100700             PERFORM PASS                                         NC2234.2
100800             GO TO INI-WRITE-GF-4-4                               NC2234.2
100900     ELSE                                                         NC2234.2
101000             GO TO INI-FAIL-GF-4-4.                               NC2234.2
101100 INI-DELETE-GF-4-4.                                               NC2234.2
101200     PERFORM DE-LETE.                                             NC2234.2
101300     GO TO INI-WRITE-GF-4-4.                                      NC2234.2
101400 INI-FAIL-GF-4-4.                                                 NC2234.2
101500     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
101600     MOVE    TEST-1-2 TO COMPUTED-X                               NC2234.2
101700     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
101800          TO RE-MARK                                              NC2234.2
101900     PERFORM FAIL.                                                NC2234.2
102000 INI-WRITE-GF-4-4.                                                NC2234.2
102100     PERFORM PRINT-DETAIL.                                        NC2234.2
102200*                                                                 NC2234.2
102300 INI-TEST-GF-4-5.                                                 NC2234.2
102400     ADD     1 TO REC-CT.                                         NC2234.2
102500     MOVE   "INI-TEST-GF-4-5" TO PAR-NAME.                        NC2234.2
102600     IF      TEST-1-3 = "**********"                              NC2234.2
102700             PERFORM PASS                                         NC2234.2
102800             GO TO INI-WRITE-GF-4-5                               NC2234.2
102900     ELSE                                                         NC2234.2
103000             GO TO INI-FAIL-GF-4-5.                               NC2234.2
103100 INI-DELETE-GF-4-5.                                               NC2234.2
103200     PERFORM DE-LETE.                                             NC2234.2
103300     GO TO INI-WRITE-GF-4-5.                                      NC2234.2
103400 INI-FAIL-GF-4-5.                                                 NC2234.2
103500     MOVE   "**********" TO CORRECT-X                             NC2234.2
103600     MOVE    TEST-1-3  TO COMPUTED-X                              NC2234.2
103700     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
103800          TO RE-MARK                                              NC2234.2
103900     PERFORM FAIL.                                                NC2234.2
104000 INI-WRITE-GF-4-5.                                                NC2234.2
104100     PERFORM PRINT-DETAIL.                                        NC2234.2
104200*                                                                 NC2234.2
104300 INI-TEST-GF-4-6.                                                 NC2234.2
104400     ADD     1 TO REC-CT.                                         NC2234.2
104500     MOVE   "INI-TEST-GF-4-6" TO PAR-NAME.                        NC2234.2
104600     IF      TEST-1-5 = "AAAAAA"                                  NC2234.2
104700             PERFORM PASS                                         NC2234.2
104800             GO TO INI-WRITE-GF-4-6                               NC2234.2
104900     ELSE                                                         NC2234.2
105000             GO TO INI-FAIL-GF-4-6.                               NC2234.2
105100 INI-DELETE-GF-4-6.                                               NC2234.2
105200     PERFORM DE-LETE.                                             NC2234.2
105300     GO TO INI-WRITE-GF-4-6.                                      NC2234.2
105400 INI-FAIL-GF-4-6.                                                 NC2234.2
105500     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
105600     MOVE    TEST-1-5 TO COMPUTED-X                               NC2234.2
105700     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
105800          TO RE-MARK                                              NC2234.2
105900     PERFORM FAIL.                                                NC2234.2
106000 INI-WRITE-GF-4-6.                                                NC2234.2
106100     PERFORM PRINT-DETAIL.                                        NC2234.2
106200*                                                                 NC2234.2
106300 INI-TEST-GF-4-7.                                                 NC2234.2
106400     ADD     1 TO REC-CT.                                         NC2234.2
106500     MOVE   "INI-TEST-GF-4-7" TO PAR-NAME.                        NC2234.2
106600     IF      TEST-1-6 = ZERO                                      NC2234.2
106700             PERFORM PASS                                         NC2234.2
106800             GO TO INI-WRITE-GF-4-7                               NC2234.2
106900     ELSE                                                         NC2234.2
107000             GO TO INI-FAIL-GF-4-7.                               NC2234.2
107100 INI-DELETE-GF-4-7.                                               NC2234.2
107200     PERFORM DE-LETE.                                             NC2234.2
107300     GO TO INI-WRITE-GF-4-7.                                      NC2234.2
107400 INI-FAIL-GF-4-7.                                                 NC2234.2
107500     MOVE    ZERO     TO CORRECT-N                                NC2234.2
107600     MOVE    TEST-1-6 TO COMPUTED-X                               NC2234.2
107700     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
107800          TO RE-MARK                                              NC2234.2
107900     PERFORM FAIL.                                                NC2234.2
108000 INI-WRITE-GF-4-7.                                                NC2234.2
108100     PERFORM PRINT-DETAIL.                                        NC2234.2
108200*                                                                 NC2234.2
108300 INI-TEST-GF-4-8.                                                 NC2234.2
108400     ADD     1 TO REC-CT.                                         NC2234.2
108500     MOVE   "INI-TEST-GF-4-8" TO PAR-NAME.                        NC2234.2
108600     IF      TEST-1-7 = "  $0.00"                                 NC2234.2
108700             PERFORM PASS                                         NC2234.2
108800             GO TO INI-WRITE-GF-4-8                               NC2234.2
108900     ELSE                                                         NC2234.2
109000             GO TO INI-FAIL-GF-4-8.                               NC2234.2
109100 INI-DELETE-GF-4-8.                                               NC2234.2
109200     PERFORM DE-LETE.                                             NC2234.2
109300     GO TO INI-WRITE-GF-4-8.                                      NC2234.2
109400 INI-FAIL-GF-4-8.                                                 NC2234.2
109500     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
109600     MOVE    TEST-1-7 TO COMPUTED-X                               NC2234.2
109700     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
109800          TO RE-MARK                                              NC2234.2
109900     PERFORM FAIL.                                                NC2234.2
110000 INI-WRITE-GF-4-8.                                                NC2234.2
110100     PERFORM PRINT-DETAIL.                                        NC2234.2
110200*                                                                 NC2234.2
110300 INI-TEST-GF-4-9.                                                 NC2234.2
110400     ADD     1 TO REC-CT.                                         NC2234.2
110500     MOVE   "INI-TEST-GF-4-9" TO PAR-NAME.                        NC2234.2
110600     IF      TEST-1-8 = "**********"                              NC2234.2
110700             PERFORM PASS                                         NC2234.2
110800             GO TO INI-WRITE-GF-4-9                               NC2234.2
110900     ELSE                                                         NC2234.2
111000             GO TO INI-FAIL-GF-4-9.                               NC2234.2
111100 INI-DELETE-GF-4-9.                                               NC2234.2
111200     PERFORM DE-LETE.                                             NC2234.2
111300     GO TO INI-WRITE-GF-4-9.                                      NC2234.2
111400 INI-FAIL-GF-4-9.                                                 NC2234.2
111500     MOVE   "**********" TO CORRECT-X                             NC2234.2
111600     MOVE    TEST-1-8  TO COMPUTED-X                              NC2234.2
111700     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
111800          TO RE-MARK                                              NC2234.2
111900     PERFORM FAIL.                                                NC2234.2
112000 INI-WRITE-GF-4-9.                                                NC2234.2
112100     PERFORM PRINT-DETAIL.                                        NC2234.2
112200*                                                                 NC2234.2
112300 INI-TEST-GF-4-10.                                                NC2234.2
112400     ADD     1 TO REC-CT.                                         NC2234.2
112500     MOVE   "INI-TEST-GF-4-10" TO PAR-NAME.                       NC2234.2
112600     IF      TEST-1-10 = "AAAAAA"                                 NC2234.2
112700             PERFORM PASS                                         NC2234.2
112800             GO TO INI-WRITE-GF-4-10                              NC2234.2
112900     ELSE                                                         NC2234.2
113000             GO TO INI-FAIL-GF-4-10.                              NC2234.2
113100 INI-DELETE-GF-4-10.                                              NC2234.2
113200     PERFORM DE-LETE.                                             NC2234.2
113300     GO TO INI-WRITE-GF-4-10.                                     NC2234.2
113400 INI-FAIL-GF-4-10.                                                NC2234.2
113500     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
113600     MOVE    TEST-1-10 TO COMPUTED-X                              NC2234.2
113700     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
113800     TO RE-MARK                                                   NC2234.2
113900     PERFORM FAIL.                                                NC2234.2
114000 INI-WRITE-GF-4-10.                                               NC2234.2
114100     PERFORM PRINT-DETAIL.                                        NC2234.2
114200*                                                                 NC2234.2
114300 INI-INIT-GF-5.                                                   NC2234.2
114400     MOVE   "VI-91/2 6.16.2 GR2" TO ANSI-REFERENCE.               NC2234.2
114500     MOVE   "INITIALIZE STATEMENT" TO FEATURE.                    NC2234.2
114600     MOVE    1 TO REC-CT.                                         NC2234.2
114700 INI-TEST-GF-5-0.                                                 NC2234.2
114800     INITIALIZE TEST-1-DATA                                       NC2234.2
114900             REPLACING NUMERIC DATA BY 1234.                      NC2234.2
115000     GO TO   INI-TEST-GF-5-1.                                     NC2234.2
115100 INI-DELETE-GF-5.                                                 NC2234.2
115200     PERFORM DE-LETE.                                             NC2234.2
115300     PERFORM PRINT-DETAIL.                                        NC2234.2
115400     GO TO   INI-INIT-GF-6.                                       NC2234.2
115500 INI-TEST-GF-5-1.                                                 NC2234.2
115600     MOVE   "INI-TEST-GF-5-1" TO PAR-NAME.                        NC2234.2
115700     IF      TEST-1-1 = 001234                                    NC2234.2
115800             PERFORM PASS                                         NC2234.2
115900             GO TO INI-WRITE-GF-5-1                               NC2234.2
116000     ELSE                                                         NC2234.2
116100             GO TO INI-FAIL-GF-5-1.                               NC2234.2
116200 INI-DELETE-GF-5-1.                                               NC2234.2
116300     PERFORM DE-LETE.                                             NC2234.2
116400     GO TO INI-WRITE-GF-5-1.                                      NC2234.2
116500 INI-FAIL-GF-5-1.                                                 NC2234.2
116600     MOVE    001234   TO CORRECT-N                                NC2234.2
116700     MOVE    TEST-1-1 TO COMPUTED-N                               NC2234.2
116800     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
116900     PERFORM FAIL.                                                NC2234.2
117000 INI-WRITE-GF-5-1.                                                NC2234.2
117100     PERFORM PRINT-DETAIL.                                        NC2234.2
117200*                                                                 NC2234.2
117300 INI-TEST-GF-5-2.                                                 NC2234.2
117400     ADD     1 TO REC-CT.                                         NC2234.2
117500     MOVE   "INI-TEST-GF-5-2" TO PAR-NAME.                        NC2234.2
117600     IF      TEST-1-6 = 001234                                    NC2234.2
117700             PERFORM PASS                                         NC2234.2
117800             GO TO INI-WRITE-GF-5-2                               NC2234.2
117900     ELSE                                                         NC2234.2
118000             GO TO INI-FAIL-GF-5-2.                               NC2234.2
118100 INI-DELETE-GF-5-2.                                               NC2234.2
118200     PERFORM DE-LETE.                                             NC2234.2
118300     GO TO INI-WRITE-GF-5-2.                                      NC2234.2
118400 INI-FAIL-GF-5-2.                                                 NC2234.2
118500     MOVE    001234   TO CORRECT-N                                NC2234.2
118600     MOVE    TEST-1-6 TO COMPUTED-N                               NC2234.2
118700     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
118800     PERFORM FAIL.                                                NC2234.2
118900 INI-WRITE-GF-5-2.                                                NC2234.2
119000     PERFORM PRINT-DETAIL.                                        NC2234.2
119100*                                                                 NC2234.2
119200 INI-TEST-GF-5-3.                                                 NC2234.2
119300     ADD     1 TO REC-CT.                                         NC2234.2
119400     MOVE   "INI-TEST-GF-5-3" TO PAR-NAME.                        NC2234.2
119500     IF      TEST-1-2 = "  $0.00"                                 NC2234.2
119600             PERFORM PASS                                         NC2234.2
119700             GO TO INI-WRITE-GF-5-3                               NC2234.2
119800     ELSE                                                         NC2234.2
119900             GO TO INI-FAIL-GF-5-3.                               NC2234.2
120000 INI-DELETE-GF-5-3.                                               NC2234.2
120100     PERFORM DE-LETE.                                             NC2234.2
120200     GO TO INI-WRITE-GF-5-3.                                      NC2234.2
120300 INI-FAIL-GF-5-3.                                                 NC2234.2
120400     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
120500     MOVE    TEST-1-2 TO COMPUTED-X                               NC2234.2
120600     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
120700          TO RE-MARK                                              NC2234.2
120800     PERFORM FAIL.                                                NC2234.2
120900 INI-WRITE-GF-5-3.                                                NC2234.2
121000     PERFORM PRINT-DETAIL.                                        NC2234.2
121100*                                                                 NC2234.2
121200 INI-TEST-GF-5-4.                                                 NC2234.2
121300     ADD     1 TO REC-CT.                                         NC2234.2
121400     MOVE   "INI-TEST-GF-5-4" TO PAR-NAME.                        NC2234.2
121500     IF      TEST-1-3 = "**********"                              NC2234.2
121600             PERFORM PASS                                         NC2234.2
121700             GO TO INI-WRITE-GF-5-4                               NC2234.2
121800     ELSE                                                         NC2234.2
121900             GO TO INI-FAIL-GF-5-4.                               NC2234.2
122000 INI-DELETE-GF-5-4.                                               NC2234.2
122100     PERFORM DE-LETE.                                             NC2234.2
122200     GO TO INI-WRITE-GF-5-4.                                      NC2234.2
122300 INI-FAIL-GF-5-4.                                                 NC2234.2
122400     MOVE   "**********" TO CORRECT-X                             NC2234.2
122500     MOVE    TEST-1-3  TO COMPUTED-X                              NC2234.2
122600     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
122700          TO RE-MARK                                              NC2234.2
122800     PERFORM FAIL.                                                NC2234.2
122900 INI-WRITE-GF-5-4.                                                NC2234.2
123000     PERFORM PRINT-DETAIL.                                        NC2234.2
123100*                                                                 NC2234.2
123200 INI-TEST-GF-5-5.                                                 NC2234.2
123300     ADD     1 TO REC-CT.                                         NC2234.2
123400     MOVE   "INI-TEST-GF-5-5" TO PAR-NAME.                        NC2234.2
123500     IF      TEST-1-4 = "DD DD/DD"                                NC2234.2
123600             PERFORM PASS                                         NC2234.2
123700             GO TO INI-WRITE-GF-5-5                               NC2234.2
123800     ELSE                                                         NC2234.2
123900             GO TO INI-FAIL-GF-5-5.                               NC2234.2
124000 INI-DELETE-GF-5-5.                                               NC2234.2
124100     PERFORM DE-LETE.                                             NC2234.2
124200     GO TO INI-WRITE-GF-5-5.                                      NC2234.2
124300 INI-FAIL-GF-5-5.                                                 NC2234.2
124400     MOVE   "DD DD/DD" TO CORRECT-X                               NC2234.2
124500     MOVE    TEST-1-4  TO COMPUTED-X                              NC2234.2
124600     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
124700          TO RE-MARK                                              NC2234.2
124800     PERFORM FAIL.                                                NC2234.2
124900 INI-WRITE-GF-5-5.                                                NC2234.2
125000     PERFORM PRINT-DETAIL.                                        NC2234.2
125100*                                                                 NC2234.2
125200 INI-TEST-GF-5-6.                                                 NC2234.2
125300     ADD     1 TO REC-CT.                                         NC2234.2
125400     MOVE   "INI-TEST-GF-5-6" TO PAR-NAME.                        NC2234.2
125500     IF      TEST-1-5 = "AAAAAA"                                  NC2234.2
125600             PERFORM PASS                                         NC2234.2
125700             GO TO INI-WRITE-GF-5-6                               NC2234.2
125800     ELSE                                                         NC2234.2
125900             GO TO INI-FAIL-GF-5-6.                               NC2234.2
126000 INI-DELETE-GF-5-6.                                               NC2234.2
126100     PERFORM DE-LETE.                                             NC2234.2
126200     GO TO INI-WRITE-GF-5-6.                                      NC2234.2
126300 INI-FAIL-GF-5-6.                                                 NC2234.2
126400     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
126500     MOVE    TEST-1-5 TO COMPUTED-X                               NC2234.2
126600     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
126700          TO RE-MARK                                              NC2234.2
126800     PERFORM FAIL.                                                NC2234.2
126900 INI-WRITE-GF-5-6.                                                NC2234.2
127000     PERFORM PRINT-DETAIL.                                        NC2234.2
127100*                                                                 NC2234.2
127200 INI-TEST-GF-5-7.                                                 NC2234.2
127300     ADD     1 TO REC-CT.                                         NC2234.2
127400     MOVE   "INI-TEST-GF-5-7" TO PAR-NAME.                        NC2234.2
127500     IF      TEST-1-7 = "  $0.00"                                 NC2234.2
127600             PERFORM PASS                                         NC2234.2
127700             GO TO INI-WRITE-GF-5-7                               NC2234.2
127800     ELSE                                                         NC2234.2
127900             GO TO INI-FAIL-GF-5-7.                               NC2234.2
128000 INI-DELETE-GF-5-7.                                               NC2234.2
128100     PERFORM DE-LETE.                                             NC2234.2
128200     GO TO INI-WRITE-GF-5-7.                                      NC2234.2
128300 INI-FAIL-GF-5-7.                                                 NC2234.2
128400     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
128500     MOVE    TEST-1-7 TO COMPUTED-X                               NC2234.2
128600     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
128700          TO RE-MARK                                              NC2234.2
128800     PERFORM FAIL.                                                NC2234.2
128900 INI-WRITE-GF-5-7.                                                NC2234.2
129000     PERFORM PRINT-DETAIL.                                        NC2234.2
129100*                                                                 NC2234.2
129200 INI-TEST-GF-5-8.                                                 NC2234.2
129300     ADD     1 TO REC-CT.                                         NC2234.2
129400     MOVE   "INI-TEST-GF-5-8" TO PAR-NAME.                        NC2234.2
129500     IF      TEST-1-8 = "**********"                              NC2234.2
129600             PERFORM PASS                                         NC2234.2
129700             GO TO INI-WRITE-GF-5-8                               NC2234.2
129800     ELSE                                                         NC2234.2
129900             GO TO INI-FAIL-GF-5-8.                               NC2234.2
130000 INI-DELETE-GF-5-8.                                               NC2234.2
130100     PERFORM DE-LETE.                                             NC2234.2
130200     GO TO INI-WRITE-GF-5-8.                                      NC2234.2
130300 INI-FAIL-GF-5-8.                                                 NC2234.2
130400     MOVE   "**********" TO CORRECT-X                             NC2234.2
130500     MOVE    TEST-1-8  TO COMPUTED-X                              NC2234.2
130600     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
130700          TO RE-MARK                                              NC2234.2
130800     PERFORM FAIL.                                                NC2234.2
130900 INI-WRITE-GF-5-8.                                                NC2234.2
131000     PERFORM PRINT-DETAIL.                                        NC2234.2
131100*                                                                 NC2234.2
131200 INI-TEST-GF-5-9.                                                 NC2234.2
131300     ADD     1 TO REC-CT.                                         NC2234.2
131400     MOVE   "INI-TEST-GF-5-9" TO PAR-NAME.                        NC2234.2
131500     IF      TEST-1-9 = "DD DD/DD"                                NC2234.2
131600             PERFORM PASS                                         NC2234.2
131700             GO TO INI-WRITE-GF-5-9                               NC2234.2
131800     ELSE                                                         NC2234.2
131900             GO TO INI-FAIL-GF-5-9.                               NC2234.2
132000 INI-DELETE-GF-5-9.                                               NC2234.2
132100     PERFORM DE-LETE.                                             NC2234.2
132200     GO TO INI-WRITE-GF-5-9.                                      NC2234.2
132300 INI-FAIL-GF-5-9.                                                 NC2234.2
132400     MOVE   "DD DD/DD" TO CORRECT-X                               NC2234.2
132500     MOVE    TEST-1-9  TO COMPUTED-X                              NC2234.2
132600     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
132700          TO RE-MARK                                              NC2234.2
132800     PERFORM FAIL.                                                NC2234.2
132900 INI-WRITE-GF-5-9.                                                NC2234.2
133000     PERFORM PRINT-DETAIL.                                        NC2234.2
133100*                                                                 NC2234.2
133200 INI-TEST-GF-5-10.                                                NC2234.2
133300     ADD     1 TO REC-CT.                                         NC2234.2
133400     MOVE   "INI-TEST-GF-5-10" TO PAR-NAME.                       NC2234.2
133500     IF      TEST-1-10 = "AAAAAA"                                 NC2234.2
133600             PERFORM PASS                                         NC2234.2
133700             GO TO INI-WRITE-GF-5-10                              NC2234.2
133800     ELSE                                                         NC2234.2
133900             GO TO INI-FAIL-GF-5-10.                              NC2234.2
134000 INI-DELETE-GF-5-10.                                              NC2234.2
134100     PERFORM DE-LETE.                                             NC2234.2
134200     GO TO INI-WRITE-GF-5-10.                                     NC2234.2
134300 INI-FAIL-GF-5-10.                                                NC2234.2
134400     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
134500     MOVE    TEST-1-10 TO COMPUTED-X                              NC2234.2
134600     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
134700          TO RE-MARK                                              NC2234.2
134800     PERFORM FAIL.                                                NC2234.2
134900 INI-WRITE-GF-5-10.                                               NC2234.2
135000     PERFORM PRINT-DETAIL.                                        NC2234.2
135100*                                                                 NC2234.2
135200 INI-INIT-GF-6.                                                   NC2234.2
135300     MOVE   "VI-91/2 6.16.2 GR2" TO ANSI-REFERENCE.               NC2234.2
135400     MOVE   "INITIALIZE STATEMENT" TO FEATURE.                    NC2234.2
135500     MOVE    1 TO REC-CT.                                         NC2234.2
135600 INI-TEST-GF-6-0.                                                 NC2234.2
135700     INITIALIZE TEST-1-DATA                                       NC2234.2
135800             REPLACING NUMERIC-EDITED DATA BY NUM-1234.           NC2234.2
135900     GO TO   INI-TEST-GF-6-1.                                     NC2234.2
136000 INI-DELETE-GF-6.                                                 NC2234.2
136100     PERFORM DE-LETE.                                             NC2234.2
136200     PERFORM PRINT-DETAIL.                                        NC2234.2
136300     GO TO   INI-INIT-GF-7.                                       NC2234.2
136400 INI-TEST-GF-6-1.                                                 NC2234.2
136500     MOVE   "INI-TEST-GF-6-1" TO PAR-NAME.                        NC2234.2
136600     IF      TEST-1-2 = "$234.00"                                 NC2234.2
136700             PERFORM PASS                                         NC2234.2
136800             GO TO INI-WRITE-GF-6-1                               NC2234.2
136900     ELSE                                                         NC2234.2
137000             GO TO INI-FAIL-GF-6-1.                               NC2234.2
137100 INI-DELETE-GF-6-1.                                               NC2234.2
137200     PERFORM DE-LETE.                                             NC2234.2
137300     GO TO INI-WRITE-GF-6-1.                                      NC2234.2
137400 INI-FAIL-GF-6-1.                                                 NC2234.2
137500     MOVE   "$234.00" TO CORRECT-X                                NC2234.2
137600     MOVE    TEST-1-2 TO COMPUTED-X                               NC2234.2
137700     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
137800     PERFORM FAIL.                                                NC2234.2
137900 INI-WRITE-GF-6-1.                                                NC2234.2
138000     PERFORM PRINT-DETAIL.                                        NC2234.2
138100*                                                                 NC2234.2
138200 INI-TEST-GF-6-2.                                                 NC2234.2
138300     ADD     1 TO REC-CT.                                         NC2234.2
138400     MOVE   "INI-TEST-GF-6-2" TO PAR-NAME.                        NC2234.2
138500     IF      TEST-1-7 = "$234.00"                                 NC2234.2
138600             PERFORM PASS                                         NC2234.2
138700             GO TO INI-WRITE-GF-6-2                               NC2234.2
138800     ELSE                                                         NC2234.2
138900             GO TO INI-FAIL-GF-6-2.                               NC2234.2
139000 INI-DELETE-GF-6-2.                                               NC2234.2
139100     PERFORM DE-LETE.                                             NC2234.2
139200     GO TO INI-WRITE-GF-6-2.                                      NC2234.2
139300 INI-FAIL-GF-6-2.                                                 NC2234.2
139400     MOVE   "$234.00" TO CORRECT-X                                NC2234.2
139500     MOVE    TEST-1-7 TO COMPUTED-X                               NC2234.2
139600     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
139700     PERFORM FAIL.                                                NC2234.2
139800 INI-WRITE-GF-6-2.                                                NC2234.2
139900     PERFORM PRINT-DETAIL.                                        NC2234.2
140000*                                                                 NC2234.2
140100 INI-TEST-GF-6-3.                                                 NC2234.2
140200     ADD     1 TO REC-CT.                                         NC2234.2
140300     MOVE   "INI-TEST-GF-6-3" TO PAR-NAME.                        NC2234.2
140400     IF      TEST-1-1 = 001234                                    NC2234.2
140500             PERFORM PASS                                         NC2234.2
140600             GO TO INI-WRITE-GF-6-3                               NC2234.2
140700     ELSE                                                         NC2234.2
140800             GO TO INI-FAIL-GF-6-3.                               NC2234.2
140900 INI-DELETE-GF-6-3.                                               NC2234.2
141000     PERFORM DE-LETE.                                             NC2234.2
141100     GO TO INI-WRITE-GF-6-3.                                      NC2234.2
141200 INI-FAIL-GF-6-3.                                                 NC2234.2
141300     MOVE    001234   TO CORRECT-N                                NC2234.2
141400     MOVE    TEST-1-1 TO COMPUTED-X                               NC2234.2
141500     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
141600          TO RE-MARK                                              NC2234.2
141700     PERFORM FAIL.                                                NC2234.2
141800 INI-WRITE-GF-6-3.                                                NC2234.2
141900     PERFORM PRINT-DETAIL.                                        NC2234.2
142000*                                                                 NC2234.2
142100 INI-TEST-GF-6-4.                                                 NC2234.2
142200     ADD     1 TO REC-CT.                                         NC2234.2
142300     MOVE   "INI-TEST-GF-6-4" TO PAR-NAME.                        NC2234.2
142400     IF      TEST-1-3 = "**********"                              NC2234.2
142500             PERFORM PASS                                         NC2234.2
142600             GO TO INI-WRITE-GF-6-4                               NC2234.2
142700     ELSE                                                         NC2234.2
142800             GO TO INI-FAIL-GF-6-4.                               NC2234.2
142900 INI-DELETE-GF-6-4.                                               NC2234.2
143000     PERFORM DE-LETE.                                             NC2234.2
143100     GO TO INI-WRITE-GF-6-4.                                      NC2234.2
143200 INI-FAIL-GF-6-4.                                                 NC2234.2
143300     MOVE   "**********" TO CORRECT-X                             NC2234.2
143400     MOVE    TEST-1-3  TO COMPUTED-X                              NC2234.2
143500     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
143600         TO RE-MARK                                               NC2234.2
143700     PERFORM FAIL.                                                NC2234.2
143800 INI-WRITE-GF-6-4.                                                NC2234.2
143900     PERFORM PRINT-DETAIL.                                        NC2234.2
144000*                                                                 NC2234.2
144100 INI-TEST-GF-6-5.                                                 NC2234.2
144200     ADD     1 TO REC-CT.                                         NC2234.2
144300     MOVE   "INI-TEST-GF-6-5" TO PAR-NAME.                        NC2234.2
144400     IF      TEST-1-4 = "DD DD/DD"                                NC2234.2
144500             PERFORM PASS                                         NC2234.2
144600             GO TO INI-WRITE-GF-6-5                               NC2234.2
144700     ELSE                                                         NC2234.2
144800             GO TO INI-FAIL-GF-6-5.                               NC2234.2
144900 INI-DELETE-GF-6-5.                                               NC2234.2
145000     PERFORM DE-LETE.                                             NC2234.2
145100     GO TO INI-WRITE-GF-6-5.                                      NC2234.2
145200 INI-FAIL-GF-6-5.                                                 NC2234.2
145300     MOVE   "DD DD/DD" TO CORRECT-X                               NC2234.2
145400     MOVE    TEST-1-4  TO COMPUTED-X                              NC2234.2
145500     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
145600          TO RE-MARK                                              NC2234.2
145700     PERFORM FAIL.                                                NC2234.2
145800 INI-WRITE-GF-6-5.                                                NC2234.2
145900     PERFORM PRINT-DETAIL.                                        NC2234.2
146000*                                                                 NC2234.2
146100 INI-TEST-GF-6-6.                                                 NC2234.2
146200     ADD     1 TO REC-CT.                                         NC2234.2
146300     MOVE   "INI-TEST-GF-6-6" TO PAR-NAME.                        NC2234.2
146400     IF      TEST-1-5 = "AAAAAA"                                  NC2234.2
146500             PERFORM PASS                                         NC2234.2
146600             GO TO INI-WRITE-GF-6-6                               NC2234.2
146700     ELSE                                                         NC2234.2
146800             GO TO INI-FAIL-GF-6-6.                               NC2234.2
146900 INI-DELETE-GF-6-6.                                               NC2234.2
147000     PERFORM DE-LETE.                                             NC2234.2
147100     GO TO INI-WRITE-GF-6-6.                                      NC2234.2
147200 INI-FAIL-GF-6-6.                                                 NC2234.2
147300     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
147400     MOVE    TEST-1-5 TO COMPUTED-X                               NC2234.2
147500     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
147600          TO RE-MARK                                              NC2234.2
147700     PERFORM FAIL.                                                NC2234.2
147800 INI-WRITE-GF-6-6.                                                NC2234.2
147900     PERFORM PRINT-DETAIL.                                        NC2234.2
148000*                                                                 NC2234.2
148100 INI-TEST-GF-6-7.                                                 NC2234.2
148200     ADD     1 TO REC-CT.                                         NC2234.2
148300     MOVE   "INI-TEST-GF-6-7" TO PAR-NAME.                        NC2234.2
148400     IF      TEST-1-6 = 1234                                      NC2234.2
148500             PERFORM PASS                                         NC2234.2
148600             GO TO INI-WRITE-GF-6-7                               NC2234.2
148700     ELSE                                                         NC2234.2
148800             GO TO INI-FAIL-GF-6-7.                               NC2234.2
148900 INI-DELETE-GF-6-7.                                               NC2234.2
149000     PERFORM DE-LETE.                                             NC2234.2
149100     GO TO INI-WRITE-GF-6-7.                                      NC2234.2
149200 INI-FAIL-GF-6-7.                                                 NC2234.2
149300     MOVE    1234     TO CORRECT-N                                NC2234.2
149400     MOVE    TEST-1-6 TO COMPUTED-X                               NC2234.2
149500     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
149600          TO RE-MARK                                              NC2234.2
149700     PERFORM FAIL.                                                NC2234.2
149800 INI-WRITE-GF-6-7.                                                NC2234.2
149900     PERFORM PRINT-DETAIL.                                        NC2234.2
150000*                                                                 NC2234.2
150100 INI-TEST-GF-6-8.                                                 NC2234.2
150200     ADD     1 TO REC-CT.                                         NC2234.2
150300     MOVE   "INI-TEST-GF-6-8" TO PAR-NAME.                        NC2234.2
150400     IF      TEST-1-8 = "**********"                              NC2234.2
150500             PERFORM PASS                                         NC2234.2
150600             GO TO INI-WRITE-GF-6-8                               NC2234.2
150700     ELSE                                                         NC2234.2
150800             GO TO INI-FAIL-GF-6-8.                               NC2234.2
150900 INI-DELETE-GF-6-8.                                               NC2234.2
151000     PERFORM DE-LETE.                                             NC2234.2
151100     GO TO INI-WRITE-GF-6-8.                                      NC2234.2
151200 INI-FAIL-GF-6-8.                                                 NC2234.2
151300     MOVE   "**********" TO CORRECT-X                             NC2234.2
151400     MOVE    TEST-1-8  TO COMPUTED-X                              NC2234.2
151500     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
151600          TO RE-MARK                                              NC2234.2
151700     PERFORM FAIL.                                                NC2234.2
151800 INI-WRITE-GF-6-8.                                                NC2234.2
151900     PERFORM PRINT-DETAIL.                                        NC2234.2
152000*                                                                 NC2234.2
152100 INI-TEST-GF-6-9.                                                 NC2234.2
152200     ADD     1 TO REC-CT.                                         NC2234.2
152300     MOVE   "INI-TEST-GF-6-9" TO PAR-NAME.                        NC2234.2
152400     IF      TEST-1-9 = "DD DD/DD"                                NC2234.2
152500             PERFORM PASS                                         NC2234.2
152600             GO TO INI-WRITE-GF-6-9                               NC2234.2
152700     ELSE                                                         NC2234.2
152800             GO TO INI-FAIL-GF-6-9.                               NC2234.2
152900 INI-DELETE-GF-6-9.                                               NC2234.2
153000     PERFORM DE-LETE.                                             NC2234.2
153100     GO TO INI-WRITE-GF-6-9.                                      NC2234.2
153200 INI-FAIL-GF-6-9.                                                 NC2234.2
153300     MOVE    TEST-1-9  TO COMPUTED-X                              NC2234.2
153400     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
153500          TO RE-MARK                                              NC2234.2
153600     PERFORM FAIL.                                                NC2234.2
153700 INI-WRITE-GF-6-9.                                                NC2234.2
153800     PERFORM PRINT-DETAIL.                                        NC2234.2
153900*                                                                 NC2234.2
154000 INI-TEST-GF-6-10.                                                NC2234.2
154100     ADD     1 TO REC-CT.                                         NC2234.2
154200     MOVE   "INI-TEST-GF-6-10" TO PAR-NAME.                       NC2234.2
154300     IF      TEST-1-10 = "AAAAAA"                                 NC2234.2
154400             PERFORM PASS                                         NC2234.2
154500             GO TO INI-WRITE-GF-6-10                              NC2234.2
154600     ELSE                                                         NC2234.2
154700             GO TO INI-FAIL-GF-6-10.                              NC2234.2
154800 INI-DELETE-GF-6-10.                                              NC2234.2
154900     PERFORM DE-LETE.                                             NC2234.2
155000     GO TO INI-WRITE-GF-6-10.                                     NC2234.2
155100 INI-FAIL-GF-6-10.                                                NC2234.2
155200     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
155300     MOVE    TEST-1-10 TO COMPUTED-X                              NC2234.2
155400     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
155500          TO RE-MARK                                              NC2234.2
155600     PERFORM FAIL.                                                NC2234.2
155700 INI-WRITE-GF-6-10.                                               NC2234.2
155800     PERFORM PRINT-DETAIL.                                        NC2234.2
155900*                                                                 NC2234.2
156000 INI-INIT-GF-7.                                                   NC2234.2
156100*    ===-->  MULTIPLE "REPLACING" PHRASES"  <--===                NC2234.2
156200     MOVE   "VI-91 6.16.2" TO ANSI-REFERENCE.                     NC2234.2
156300     MOVE   "INITIALIZE STATEMENT" TO FEATURE.                    NC2234.2
156400     MOVE    1 TO REC-CT.                                         NC2234.2
156500     MOVE    ZEROS  TO TEST-1-1.                                  NC2234.2
156600     MOVE    ZEROS  TO TEST-1-2.                                  NC2234.2
156700     MOVE    SPACES TO TEST-1-3.                                  NC2234.2
156800     MOVE    SPACES TO TEST-1-4.                                  NC2234.2
156900     MOVE    SPACES TO TEST-1-5.                                  NC2234.2
157000     MOVE    ZEROS  TO TEST-1-6.                                  NC2234.2
157100     MOVE    ZEROS  TO TEST-1-7.                                  NC2234.2
157200     MOVE    SPACES TO TEST-1-8.                                  NC2234.2
157300     MOVE    SPACES TO TEST-1-9.                                  NC2234.2
157400     MOVE    SPACES TO TEST-1-10.                                 NC2234.2
157500 INI-TEST-GF-7-0.                                                 NC2234.2
157600     INITIALIZE TEST-1-DATA                                       NC2234.2
157700             REPLACING ALPHABETIC DATA     BY "AAAAAA"            NC2234.2
157800                       ALPHANUMERIC        BY "**********"        NC2234.2
157900                       ALPHANUMERIC-EDITED BY "DDDDDD"            NC2234.2
158000                       NUMERIC DATA        BY  1234               NC2234.2
158100                       NUMERIC-EDITED      BY NUM-1234.           NC2234.2
158200     GO TO   INI-TEST-GF-7-1.                                     NC2234.2
158300 INI-DELETE-GF-7.                                                 NC2234.2
158400     PERFORM DE-LETE.                                             NC2234.2
158500     PERFORM PRINT-DETAIL.                                        NC2234.2
158600     GO TO   INI-INIT-GF-8.                                       NC2234.2
158700 INI-TEST-GF-7-1.                                                 NC2234.2
158800     MOVE   "INI-TEST-GF-7-1" TO PAR-NAME.                        NC2234.2
158900     IF      TEST-1-2 = "$234.00"                                 NC2234.2
159000             PERFORM PASS                                         NC2234.2
159100             GO TO INI-WRITE-GF-7-1                               NC2234.2
159200     ELSE                                                         NC2234.2
159300             GO TO INI-FAIL-GF-7-1.                               NC2234.2
159400 INI-DELETE-GF-7-1.                                               NC2234.2
159500     PERFORM DE-LETE.                                             NC2234.2
159600     GO TO INI-WRITE-GF-7-1.                                      NC2234.2
159700 INI-FAIL-GF-7-1.                                                 NC2234.2
159800     MOVE   "$234.00" TO CORRECT-X                                NC2234.2
159900     MOVE    TEST-1-2 TO COMPUTED-X                               NC2234.2
160000     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
160100     PERFORM FAIL.                                                NC2234.2
160200 INI-WRITE-GF-7-1.                                                NC2234.2
160300     PERFORM PRINT-DETAIL.                                        NC2234.2
160400*                                                                 NC2234.2
160500 INI-TEST-GF-7-2.                                                 NC2234.2
160600     ADD     1 TO REC-CT.                                         NC2234.2
160700     MOVE   "INI-TEST-GF-7-2" TO PAR-NAME.                        NC2234.2
160800     IF      TEST-1-7 = "$234.00"                                 NC2234.2
160900             PERFORM PASS                                         NC2234.2
161000             GO TO INI-WRITE-GF-7-2                               NC2234.2
161100     ELSE                                                         NC2234.2
161200             GO TO INI-FAIL-GF-7-2.                               NC2234.2
161300 INI-DELETE-GF-7-2.                                               NC2234.2
161400     PERFORM DE-LETE.                                             NC2234.2
161500     GO TO INI-WRITE-GF-7-2.                                      NC2234.2
161600 INI-FAIL-GF-7-2.                                                 NC2234.2
161700     MOVE   "$234.00" TO CORRECT-X                                NC2234.2
161800     MOVE    TEST-1-7 TO COMPUTED-X                               NC2234.2
161900     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
162000     PERFORM FAIL.                                                NC2234.2
162100 INI-WRITE-GF-7-2.                                                NC2234.2
162200     PERFORM PRINT-DETAIL.                                        NC2234.2
162300*                                                                 NC2234.2
162400 INI-TEST-GF-7-3.                                                 NC2234.2
162500     ADD     1 TO REC-CT.                                         NC2234.2
162600     MOVE   "INI-TEST-GF-7-3" TO PAR-NAME.                        NC2234.2
162700     IF      TEST-1-1 = 001234                                    NC2234.2
162800             PERFORM PASS                                         NC2234.2
162900             GO TO INI-WRITE-GF-7-3                               NC2234.2
163000     ELSE                                                         NC2234.2
163100             GO TO INI-FAIL-GF-7-3.                               NC2234.2
163200 INI-DELETE-GF-7-3.                                               NC2234.2
163300     PERFORM DE-LETE.                                             NC2234.2
163400     GO TO INI-WRITE-GF-7-3.                                      NC2234.2
163500 INI-FAIL-GF-7-3.                                                 NC2234.2
163600     MOVE    001234   TO CORRECT-N                                NC2234.2
163700     MOVE    TEST-1-1 TO COMPUTED-X                               NC2234.2
163800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
163900          TO RE-MARK                                              NC2234.2
164000     PERFORM FAIL.                                                NC2234.2
164100 INI-WRITE-GF-7-3.                                                NC2234.2
164200     PERFORM PRINT-DETAIL.                                        NC2234.2
164300*                                                                 NC2234.2
164400 INI-TEST-GF-7-4.                                                 NC2234.2
164500     ADD     1 TO REC-CT.                                         NC2234.2
164600     MOVE   "INI-TEST-GF-7-4" TO PAR-NAME.                        NC2234.2
164700     IF      TEST-1-3 = "**********"                              NC2234.2
164800             PERFORM PASS                                         NC2234.2
164900             GO TO INI-WRITE-GF-7-4                               NC2234.2
165000     ELSE                                                         NC2234.2
165100             GO TO INI-FAIL-GF-7-4.                               NC2234.2
165200 INI-DELETE-GF-7-4.                                               NC2234.2
165300     PERFORM DE-LETE.                                             NC2234.2
165400     GO TO INI-WRITE-GF-7-4.                                      NC2234.2
165500 INI-FAIL-GF-7-4.                                                 NC2234.2
165600     MOVE   "**********" TO CORRECT-X                             NC2234.2
165700     MOVE    TEST-1-3  TO COMPUTED-X                              NC2234.2
165800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
165900          TO RE-MARK                                              NC2234.2
166000     PERFORM FAIL.                                                NC2234.2
166100 INI-WRITE-GF-7-4.                                                NC2234.2
166200     PERFORM PRINT-DETAIL.                                        NC2234.2
166300*                                                                 NC2234.2
166400 INI-TEST-GF-7-5.                                                 NC2234.2
166500     ADD     1 TO REC-CT.                                         NC2234.2
166600     MOVE   "INI-TEST-GF-7-5" TO PAR-NAME.                        NC2234.2
166700     IF      TEST-1-4 = "DD DD/DD"                                NC2234.2
166800             PERFORM PASS                                         NC2234.2
166900             GO TO INI-WRITE-GF-7-5                               NC2234.2
167000     ELSE                                                         NC2234.2
167100             GO TO INI-FAIL-GF-7-5.                               NC2234.2
167200 INI-DELETE-GF-7-5.                                               NC2234.2
167300     PERFORM DE-LETE.                                             NC2234.2
167400     GO TO INI-WRITE-GF-7-5.                                      NC2234.2
167500 INI-FAIL-GF-7-5.                                                 NC2234.2
167600     MOVE   "DD DD/DD" TO CORRECT-X                               NC2234.2
167700     MOVE    TEST-1-4  TO COMPUTED-X                              NC2234.2
167800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
167900          TO RE-MARK                                              NC2234.2
168000     PERFORM FAIL.                                                NC2234.2
168100 INI-WRITE-GF-7-5.                                                NC2234.2
168200     PERFORM PRINT-DETAIL.                                        NC2234.2
168300*                                                                 NC2234.2
168400 INI-TEST-GF-7-6.                                                 NC2234.2
168500     ADD     1 TO REC-CT.                                         NC2234.2
168600     MOVE   "INI-TEST-GF-7-6" TO PAR-NAME.                        NC2234.2
168700     IF      TEST-1-5 = "AAAAAA"                                  NC2234.2
168800             PERFORM PASS                                         NC2234.2
168900             GO TO INI-WRITE-GF-7-6                               NC2234.2
169000     ELSE                                                         NC2234.2
169100             GO TO INI-FAIL-GF-7-6.                               NC2234.2
169200 INI-DELETE-GF-7-6.                                               NC2234.2
169300     PERFORM DE-LETE.                                             NC2234.2
169400     GO TO INI-WRITE-GF-7-6.                                      NC2234.2
169500 INI-FAIL-GF-7-6.                                                 NC2234.2
169600     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
169700     MOVE    TEST-1-5 TO COMPUTED-X                               NC2234.2
169800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
169900         TO RE-MARK                                               NC2234.2
170000     PERFORM FAIL.                                                NC2234.2
170100 INI-WRITE-GF-7-6.                                                NC2234.2
170200     PERFORM PRINT-DETAIL.                                        NC2234.2
170300*                                                                 NC2234.2
170400 INI-TEST-GF-7-7.                                                 NC2234.2
170500     ADD     1 TO REC-CT.                                         NC2234.2
170600     MOVE   "INI-TEST-GF-7-7" TO PAR-NAME.                        NC2234.2
170700     IF      TEST-1-6 = 001234                                    NC2234.2
170800             PERFORM PASS                                         NC2234.2
170900             GO TO INI-WRITE-GF-7-7                               NC2234.2
171000     ELSE                                                         NC2234.2
171100             GO TO INI-FAIL-GF-7-7.                               NC2234.2
171200 INI-DELETE-GF-7-7.                                               NC2234.2
171300     PERFORM DE-LETE.                                             NC2234.2
171400     GO TO INI-WRITE-GF-7-7.                                      NC2234.2
171500 INI-FAIL-GF-7-7.                                                 NC2234.2
171600     MOVE    ZERO     TO CORRECT-N                                NC2234.2
171700     MOVE    TEST-1-6 TO COMPUTED-X                               NC2234.2
171800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
171900          TO RE-MARK                                              NC2234.2
172000     PERFORM FAIL.                                                NC2234.2
172100 INI-WRITE-GF-7-7.                                                NC2234.2
172200     PERFORM PRINT-DETAIL.                                        NC2234.2
172300*                                                                 NC2234.2
172400 INI-TEST-GF-7-8.                                                 NC2234.2
172500     ADD     1 TO REC-CT.                                         NC2234.2
172600     MOVE   "INI-TEST-GF-7-8" TO PAR-NAME.                        NC2234.2
172700     IF      TEST-1-8 = "**********"                              NC2234.2
172800             PERFORM PASS                                         NC2234.2
172900             GO TO INI-WRITE-GF-7-8                               NC2234.2
173000     ELSE                                                         NC2234.2
173100             GO TO INI-FAIL-GF-7-8.                               NC2234.2
173200 INI-DELETE-GF-7-8.                                               NC2234.2
173300     PERFORM DE-LETE.                                             NC2234.2
173400     GO TO INI-WRITE-GF-7-8.                                      NC2234.2
173500 INI-FAIL-GF-7-8.                                                 NC2234.2
173600     MOVE   "**********" TO CORRECT-X                             NC2234.2
173700     MOVE    TEST-1-8  TO COMPUTED-X                              NC2234.2
173800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
173900          TO RE-MARK                                              NC2234.2
174000     PERFORM FAIL.                                                NC2234.2
174100 INI-WRITE-GF-7-8.                                                NC2234.2
174200     PERFORM PRINT-DETAIL.                                        NC2234.2
174300*                                                                 NC2234.2
174400 INI-TEST-GF-7-9.                                                 NC2234.2
174500     ADD     1 TO REC-CT.                                         NC2234.2
174600     MOVE   "INI-TEST-GF-7-9" TO PAR-NAME.                        NC2234.2
174700     IF      TEST-1-9 = "DD DD/DD"                                NC2234.2
174800             PERFORM PASS                                         NC2234.2
174900             GO TO INI-WRITE-GF-7-9                               NC2234.2
175000     ELSE                                                         NC2234.2
175100             GO TO INI-FAIL-GF-7-9.                               NC2234.2
175200 INI-DELETE-GF-7-9.                                               NC2234.2
175300     PERFORM DE-LETE.                                             NC2234.2
175400     GO TO INI-WRITE-GF-7-9.                                      NC2234.2
175500 INI-FAIL-GF-7-9.                                                 NC2234.2
175600     MOVE   "DD DD/DD" TO CORRECT-X                               NC2234.2
175700     MOVE    TEST-1-9  TO COMPUTED-X                              NC2234.2
175800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
175900          TO RE-MARK                                              NC2234.2
176000     PERFORM FAIL.                                                NC2234.2
176100 INI-WRITE-GF-7-9.                                                NC2234.2
176200     PERFORM PRINT-DETAIL.                                        NC2234.2
176300*                                                                 NC2234.2
176400 INI-TEST-GF-7-10.                                                NC2234.2
176500     ADD     1 TO REC-CT.                                         NC2234.2
176600     MOVE   "INI-TEST-GF-7-10" TO PAR-NAME.                       NC2234.2
176700     IF      TEST-1-10 = "AAAAAA"                                 NC2234.2
176800             PERFORM PASS                                         NC2234.2
176900             GO TO INI-WRITE-GF-7-10                              NC2234.2
177000     ELSE                                                         NC2234.2
177100             GO TO INI-FAIL-GF-7-10.                              NC2234.2
177200 INI-DELETE-GF-7-10.                                              NC2234.2
177300     PERFORM DE-LETE.                                             NC2234.2
177400     GO TO INI-WRITE-GF-7-10.                                     NC2234.2
177500 INI-FAIL-GF-7-10.                                                NC2234.2
177600     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
177700     MOVE    TEST-1-10 TO COMPUTED-X                              NC2234.2
177800     MOVE   "WRONGLY AFFECTED BY OTHER INITIALISATION"            NC2234.2
177900          TO RE-MARK                                              NC2234.2
178000     PERFORM FAIL.                                                NC2234.2
178100 INI-WRITE-GF-7-10.                                               NC2234.2
178200     PERFORM PRINT-DETAIL.                                        NC2234.2
178300*                                                                 NC2234.2
178400 INI-INIT-GF-8.                                                   NC2234.2
178500*    ===-->  MULTIPLE RECEIVING AREAS       <--===                NC2234.2
178600     MOVE   "VI-91 6.16.4 GR2" TO ANSI-REFERENCE.                 NC2234.2
178700     MOVE   "INITIALIZE STATEMENT" TO FEATURE.                    NC2234.2
178800     MOVE    1 TO REC-CT.                                         NC2234.2
178900     MOVE    LOW-VALUES  TO TEST-1-DATA.                          NC2234.2
179000     MOVE    999.99      TO TEST-8-DATA-1.                        NC2234.2
179100     MOVE   "ZZZZZZZZZZ" TO TEST-8-DATA-2.                        NC2234.2
179200 INI-TEST-GF-8-0.                                                 NC2234.2
179300     INITIALIZE TEST-8-DATA-1                                     NC2234.2
179400                TEST-1-DATA                                       NC2234.2
179500                TEST-8-DATA-2.                                    NC2234.2
179600     GO TO   INI-TEST-GF-8-1.                                     NC2234.2
179700 INI-DELETE-GF-8.                                                 NC2234.2
179800     PERFORM DE-LETE.                                             NC2234.2
179900     PERFORM PRINT-DETAIL.                                        NC2234.2
180000     GO TO   INI-INIT-GF-9.                                       NC2234.2
180100 INI-TEST-GF-8-1.                                                 NC2234.2
180200     MOVE   "INI-TEST-GF-8-1" TO PAR-NAME.                        NC2234.2
180300     IF      TEST-1-2 = "  $0.00"                                 NC2234.2
180400             PERFORM PASS                                         NC2234.2
180500             GO TO INI-WRITE-GF-8-1                               NC2234.2
180600     ELSE                                                         NC2234.2
180700             GO TO INI-FAIL-GF-8-1.                               NC2234.2
180800 INI-DELETE-GF-8-1.                                               NC2234.2
180900     PERFORM DE-LETE.                                             NC2234.2
181000     GO TO INI-WRITE-GF-8-1.                                      NC2234.2
181100 INI-FAIL-GF-8-1.                                                 NC2234.2
181200     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
181300     MOVE    TEST-1-2 TO COMPUTED-X                               NC2234.2
181400     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
181500     PERFORM FAIL.                                                NC2234.2
181600 INI-WRITE-GF-8-1.                                                NC2234.2
181700     PERFORM PRINT-DETAIL.                                        NC2234.2
181800*                                                                 NC2234.2
181900 INI-TEST-GF-8-2.                                                 NC2234.2
182000     ADD     1 TO REC-CT.                                         NC2234.2
182100     MOVE   "INI-TEST-GF-8-2" TO PAR-NAME.                        NC2234.2
182200     IF      TEST-1-7 = "  $0.00"                                 NC2234.2
182300             PERFORM PASS                                         NC2234.2
182400             GO TO INI-WRITE-GF-8-2                               NC2234.2
182500     ELSE                                                         NC2234.2
182600             GO TO INI-FAIL-GF-8-2.                               NC2234.2
182700 INI-DELETE-GF-8-2.                                               NC2234.2
182800     PERFORM DE-LETE.                                             NC2234.2
182900     GO TO INI-WRITE-GF-8-2.                                      NC2234.2
183000 INI-FAIL-GF-8-2.                                                 NC2234.2
183100     MOVE   "  $0.00"  TO CORRECT-X                               NC2234.2
183200     MOVE    TEST-1-7 TO COMPUTED-X                               NC2234.2
183300     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
183400     PERFORM FAIL.                                                NC2234.2
183500 INI-WRITE-GF-8-2.                                                NC2234.2
183600     PERFORM PRINT-DETAIL.                                        NC2234.2
183700*                                                                 NC2234.2
183800 INI-TEST-GF-8-3.                                                 NC2234.2
183900     ADD     1 TO REC-CT.                                         NC2234.2
184000     MOVE   "INI-TEST-GF-8-3" TO PAR-NAME.                        NC2234.2
184100     IF      TEST-1-1 = ZERO                                      NC2234.2
184200             PERFORM PASS                                         NC2234.2
184300             GO TO INI-WRITE-GF-8-3                               NC2234.2
184400     ELSE                                                         NC2234.2
184500             GO TO INI-FAIL-GF-8-3.                               NC2234.2
184600 INI-DELETE-GF-8-3.                                               NC2234.2
184700     PERFORM DE-LETE.                                             NC2234.2
184800     GO TO INI-WRITE-GF-8-3.                                      NC2234.2
184900 INI-FAIL-GF-8-3.                                                 NC2234.2
185000     MOVE    ZERO     TO CORRECT-N                                NC2234.2
185100     MOVE    TEST-1-1 TO COMPUTED-X                               NC2234.2
185200     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
185300     PERFORM FAIL.                                                NC2234.2
185400 INI-WRITE-GF-8-3.                                                NC2234.2
185500     PERFORM PRINT-DETAIL.                                        NC2234.2
185600*                                                                 NC2234.2
185700 INI-TEST-GF-8-4.                                                 NC2234.2
185800     ADD     1 TO REC-CT.                                         NC2234.2
185900     MOVE   "INI-TEST-GF-8-4" TO PAR-NAME.                        NC2234.2
186000     IF      TEST-1-3 = SPACES                                    NC2234.2
186100             PERFORM PASS                                         NC2234.2
186200             GO TO INI-WRITE-GF-8-4                               NC2234.2
186300     ELSE                                                         NC2234.2
186400             GO TO INI-FAIL-GF-8-4.                               NC2234.2
186500 INI-DELETE-GF-8-4.                                               NC2234.2
186600     PERFORM DE-LETE.                                             NC2234.2
186700     GO TO INI-WRITE-GF-8-4.                                      NC2234.2
186800 INI-FAIL-GF-8-4.                                                 NC2234.2
186900     MOVE    SPACES    TO CORRECT-X                               NC2234.2
187000     MOVE    TEST-1-3  TO COMPUTED-X                              NC2234.2
187100     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
187200     PERFORM FAIL.                                                NC2234.2
187300 INI-WRITE-GF-8-4.                                                NC2234.2
187400     PERFORM PRINT-DETAIL.                                        NC2234.2
187500*                                                                 NC2234.2
187600 INI-TEST-GF-8-5.                                                 NC2234.2
187700     ADD     1 TO REC-CT.                                         NC2234.2
187800     MOVE   "INI-TEST-GF-8-5" TO PAR-NAME.                        NC2234.2
187900     IF      TEST-1-4 = "     /  "                                NC2234.2
188000             PERFORM PASS                                         NC2234.2
188100             GO TO INI-WRITE-GF-8-5                               NC2234.2
188200     ELSE                                                         NC2234.2
188300             GO TO INI-FAIL-GF-8-5.                               NC2234.2
188400 INI-DELETE-GF-8-5.                                               NC2234.2
188500     PERFORM DE-LETE.                                             NC2234.2
188600     GO TO INI-WRITE-GF-8-5.                                      NC2234.2
188700 INI-FAIL-GF-8-5.                                                 NC2234.2
188800     MOVE   "     /  " TO CORRECT-X                               NC2234.2
188900     MOVE    TEST-1-4  TO COMPUTED-X                              NC2234.2
189000     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
189100     PERFORM FAIL.                                                NC2234.2
189200 INI-WRITE-GF-8-5.                                                NC2234.2
189300     PERFORM PRINT-DETAIL.                                        NC2234.2
189400*                                                                 NC2234.2
189500 INI-TEST-GF-8-6.                                                 NC2234.2
189600     ADD     1 TO REC-CT.                                         NC2234.2
189700     MOVE   "INI-TEST-GF-8-6" TO PAR-NAME.                        NC2234.2
189800     IF      TEST-1-5 = SPACES                                    NC2234.2
189900             PERFORM PASS                                         NC2234.2
190000             GO TO INI-WRITE-GF-8-6                               NC2234.2
190100     ELSE                                                         NC2234.2
190200             GO TO INI-FAIL-GF-8-6.                               NC2234.2
190300 INI-DELETE-GF-8-6.                                               NC2234.2
190400     PERFORM DE-LETE.                                             NC2234.2
190500     GO TO INI-WRITE-GF-8-6.                                      NC2234.2
190600 INI-FAIL-GF-8-6.                                                 NC2234.2
190700     MOVE    SPACES   TO CORRECT-X                                NC2234.2
190800     MOVE    TEST-1-5 TO COMPUTED-X                               NC2234.2
190900     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
191000     PERFORM FAIL.                                                NC2234.2
191100 INI-WRITE-GF-8-6.                                                NC2234.2
191200     PERFORM PRINT-DETAIL.                                        NC2234.2
191300*                                                                 NC2234.2
191400 INI-TEST-GF-8-7.                                                 NC2234.2
191500     ADD     1 TO REC-CT.                                         NC2234.2
191600     MOVE   "INI-TEST-GF-8-7" TO PAR-NAME.                        NC2234.2
191700     IF      TEST-1-6 = ZERO                                      NC2234.2
191800             PERFORM PASS                                         NC2234.2
191900             GO TO INI-WRITE-GF-8-7                               NC2234.2
192000     ELSE                                                         NC2234.2
192100             GO TO INI-FAIL-GF-8-7.                               NC2234.2
192200 INI-DELETE-GF-8-7.                                               NC2234.2
192300     PERFORM DE-LETE.                                             NC2234.2
192400     GO TO INI-WRITE-GF-8-7.                                      NC2234.2
192500 INI-FAIL-GF-8-7.                                                 NC2234.2
192600     MOVE    ZERO     TO CORRECT-N                                NC2234.2
192700     MOVE    TEST-1-6 TO COMPUTED-X                               NC2234.2
192800     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
192900     PERFORM FAIL.                                                NC2234.2
193000 INI-WRITE-GF-8-7.                                                NC2234.2
193100     PERFORM PRINT-DETAIL.                                        NC2234.2
193200*                                                                 NC2234.2
193300 INI-TEST-GF-8-8.                                                 NC2234.2
193400     ADD     1 TO REC-CT.                                         NC2234.2
193500     MOVE   "INI-TEST-GF-8-8" TO PAR-NAME.                        NC2234.2
193600     IF      TEST-1-8 = SPACES                                    NC2234.2
193700             PERFORM PASS                                         NC2234.2
193800             GO TO INI-WRITE-GF-8-8                               NC2234.2
193900     ELSE                                                         NC2234.2
194000             GO TO INI-FAIL-GF-8-8.                               NC2234.2
194100 INI-DELETE-GF-8-8.                                               NC2234.2
194200     PERFORM DE-LETE.                                             NC2234.2
194300     GO TO INI-WRITE-GF-8-8.                                      NC2234.2
194400 INI-FAIL-GF-8-8.                                                 NC2234.2
194500     MOVE    SPACES    TO CORRECT-X                               NC2234.2
194600     MOVE    TEST-1-8  TO COMPUTED-X                              NC2234.2
194700     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
194800     PERFORM FAIL.                                                NC2234.2
194900 INI-WRITE-GF-8-8.                                                NC2234.2
195000     PERFORM PRINT-DETAIL.                                        NC2234.2
195100*                                                                 NC2234.2
195200 INI-TEST-GF-8-9.                                                 NC2234.2
195300     ADD     1 TO REC-CT.                                         NC2234.2
195400     MOVE   "INI-TEST-GF-8-9" TO PAR-NAME.                        NC2234.2
195500     IF      TEST-1-9 = "     /  "                                NC2234.2
195600             PERFORM PASS                                         NC2234.2
195700             GO TO INI-WRITE-GF-8-9                               NC2234.2
195800     ELSE                                                         NC2234.2
195900             GO TO INI-FAIL-GF-8-9.                               NC2234.2
196000 INI-DELETE-GF-8-9.                                               NC2234.2
196100     PERFORM DE-LETE.                                             NC2234.2
196200     GO TO INI-WRITE-GF-8-9.                                      NC2234.2
196300 INI-FAIL-GF-8-9.                                                 NC2234.2
196400     MOVE   "     /  " TO CORRECT-X                               NC2234.2
196500     MOVE    TEST-1-9  TO COMPUTED-X                              NC2234.2
196600     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
196700     PERFORM FAIL.                                                NC2234.2
196800 INI-WRITE-GF-8-9.                                                NC2234.2
196900     PERFORM PRINT-DETAIL.                                        NC2234.2
197000*                                                                 NC2234.2
197100 INI-TEST-GF-8-10.                                                NC2234.2
197200     ADD     1 TO REC-CT.                                         NC2234.2
197300     MOVE   "INI-TEST-GF-8-10" TO PAR-NAME.                       NC2234.2
197400     IF      TEST-1-10 = SPACES                                   NC2234.2
197500             PERFORM PASS                                         NC2234.2
197600             GO TO INI-WRITE-GF-8-10                              NC2234.2
197700     ELSE                                                         NC2234.2
197800             GO TO INI-FAIL-GF-8-10.                              NC2234.2
197900 INI-DELETE-GF-8-10.                                              NC2234.2
198000     PERFORM DE-LETE.                                             NC2234.2
198100     GO TO INI-WRITE-GF-8-10.                                     NC2234.2
198200 INI-FAIL-GF-8-10.                                                NC2234.2
198300     MOVE    SPACES    TO CORRECT-X                               NC2234.2
198400     MOVE    TEST-1-10 TO COMPUTED-X                              NC2234.2
198500     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
198600     PERFORM FAIL.                                                NC2234.2
198700 INI-WRITE-GF-8-10.                                               NC2234.2
198800     PERFORM PRINT-DETAIL.                                        NC2234.2
198900*                                                                 NC2234.2
199000 INI-TEST-GF-8-11.                                                NC2234.2
199100     ADD     1 TO REC-CT.                                         NC2234.2
199200     MOVE   "INI-TEST-GF-8-11" TO PAR-NAME.                       NC2234.2
199300     IF      TEST-8-DATA-1 = "  $0.00"                            NC2234.2
199400             PERFORM PASS                                         NC2234.2
199500             GO TO INI-WRITE-GF-8-11                              NC2234.2
199600     ELSE                                                         NC2234.2
199700             GO TO INI-FAIL-GF-8-11.                              NC2234.2
199800 INI-DELETE-GF-8-11.                                              NC2234.2
199900     PERFORM DE-LETE.                                             NC2234.2
200000     GO TO INI-WRITE-GF-8-11.                                     NC2234.2
200100 INI-FAIL-GF-8-11.                                                NC2234.2
200200     MOVE   "  $0.00"   TO CORRECT-X                              NC2234.2
200300     MOVE    TEST-8-DATA-1  TO COMPUTED-X                         NC2234.2
200400     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
200500     PERFORM FAIL.                                                NC2234.2
200600 INI-WRITE-GF-8-11.                                               NC2234.2
200700     PERFORM PRINT-DETAIL.                                        NC2234.2
200800*                                                                 NC2234.2
200900 INI-TEST-GF-8-12.                                                NC2234.2
201000     ADD     1 TO REC-CT.                                         NC2234.2
201100     MOVE   "INI-TEST-GF-8-12" TO PAR-NAME.                       NC2234.2
201200     IF      TEST-8-DATA-2 = SPACES                               NC2234.2
201300             PERFORM PASS                                         NC2234.2
201400             GO TO INI-WRITE-GF-8-12                              NC2234.2
201500     ELSE                                                         NC2234.2
201600             GO TO INI-FAIL-GF-8-12.                              NC2234.2
201700 INI-DELETE-GF-8-12.                                              NC2234.2
201800     PERFORM DE-LETE.                                             NC2234.2
201900     GO TO INI-WRITE-GF-8-12.                                     NC2234.2
202000 INI-FAIL-GF-8-12.                                                NC2234.2
202100     MOVE    SPACES    TO CORRECT-X                               NC2234.2
202200     MOVE    TEST-8-DATA-2 TO COMPUTED-X                          NC2234.2
202300     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
202400     PERFORM FAIL.                                                NC2234.2
202500 INI-WRITE-GF-8-12.                                               NC2234.2
202600     PERFORM PRINT-DETAIL.                                        NC2234.2
202700*                                                                 NC2234.2
202800 INI-INIT-GF-9.                                                   NC2234.2
202900*    ===-->  MULTIPLE RECEIVING AREAS AND   <--===                NC2234.2
203000*    ===-->  MULTIPLE "REPLACING" PHRASES"  <--===                NC2234.2
203100     MOVE   "VI-91 6.16.2" TO ANSI-REFERENCE.                     NC2234.2
203200     MOVE   "INITIALIZE STATEMENT" TO FEATURE.                    NC2234.2
203300     MOVE    1 TO REC-CT.                                         NC2234.2
203400     MOVE    ZEROS  TO TEST-1-1.                                  NC2234.2
203500     MOVE    ZEROS  TO TEST-1-2.                                  NC2234.2
203600     MOVE    SPACES TO TEST-1-3.                                  NC2234.2
203700     MOVE    SPACES TO TEST-1-4.                                  NC2234.2
203800     MOVE    SPACES TO TEST-1-5.                                  NC2234.2
203900     MOVE    ZEROS  TO TEST-1-6.                                  NC2234.2
204000     MOVE    ZEROS  TO TEST-1-7.                                  NC2234.2
204100     MOVE    SPACES TO TEST-1-8.                                  NC2234.2
204200     MOVE    SPACES TO TEST-1-9.                                  NC2234.2
204300     MOVE    SPACES TO TEST-1-10.                                 NC2234.2
204400     MOVE    999.99      TO TEST-8-DATA-1.                        NC2234.2
204500     MOVE   "ZZZZZZZZZZ" TO TEST-8-DATA-2.                        NC2234.2
204600 INI-TEST-GF-9-0.                                                 NC2234.2
204700     INITIALIZE                                                   NC2234.2
204800             TEST-8-DATA-1                                        NC2234.2
204900             TEST-1-DATA                                          NC2234.2
205000             TEST-8-DATA-2                                        NC2234.2
205100             REPLACING ALPHABETIC DATA     BY "AAAAAA"            NC2234.2
205200                       ALPHANUMERIC        BY "**********"        NC2234.2
205300                       ALPHANUMERIC-EDITED BY "DDDDDD"            NC2234.2
205400                       NUMERIC DATA        BY  NUM-1234           NC2234.2
205500                       NUMERIC-EDITED      BY  1234.              NC2234.2
205600     GO TO   INI-TEST-GF-9-1.                                     NC2234.2
205700 INI-DELETE-GF-9.                                                 NC2234.2
205800     PERFORM DE-LETE.                                             NC2234.2
205900     PERFORM PRINT-DETAIL.                                        NC2234.2
206000     GO TO   CCVS-EXIT.                                           NC2234.2
206100 INI-TEST-GF-9-1.                                                 NC2234.2
206200     MOVE   "INI-TEST-GF-9-1" TO PAR-NAME.                        NC2234.2
206300     IF      TEST-1-2 = "$234.00"                                 NC2234.2
206400             PERFORM PASS                                         NC2234.2
206500             GO TO INI-WRITE-GF-9-1                               NC2234.2
206600     ELSE                                                         NC2234.2
206700             GO TO INI-FAIL-GF-9-1.                               NC2234.2
206800 INI-DELETE-GF-9-1.                                               NC2234.2
206900     PERFORM DE-LETE.                                             NC2234.2
207000     GO TO INI-WRITE-GF-9-1.                                      NC2234.2
207100 INI-FAIL-GF-9-1.                                                 NC2234.2
207200     MOVE   "$234.00" TO CORRECT-X                                NC2234.2
207300     MOVE    TEST-1-2 TO COMPUTED-X                               NC2234.2
207400     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
207500     PERFORM FAIL.                                                NC2234.2
207600 INI-WRITE-GF-9-1.                                                NC2234.2
207700     PERFORM PRINT-DETAIL.                                        NC2234.2
207800*                                                                 NC2234.2
207900 INI-TEST-GF-9-2.                                                 NC2234.2
208000     ADD     1 TO REC-CT.                                         NC2234.2
208100     MOVE   "INI-TEST-GF-9-2" TO PAR-NAME.                        NC2234.2
208200     IF      TEST-1-7 = "$234.00"                                 NC2234.2
208300             PERFORM PASS                                         NC2234.2
208400             GO TO INI-WRITE-GF-9-2                               NC2234.2
208500     ELSE                                                         NC2234.2
208600             GO TO INI-FAIL-GF-9-2.                               NC2234.2
208700 INI-DELETE-GF-9-2.                                               NC2234.2
208800     PERFORM DE-LETE.                                             NC2234.2
208900     GO TO INI-WRITE-GF-9-2.                                      NC2234.2
209000 INI-FAIL-GF-9-2.                                                 NC2234.2
209100     MOVE   "$234.00" TO CORRECT-X                                NC2234.2
209200     MOVE    TEST-1-7 TO COMPUTED-X                               NC2234.2
209300     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
209400     PERFORM FAIL.                                                NC2234.2
209500 INI-WRITE-GF-9-2.                                                NC2234.2
209600     PERFORM PRINT-DETAIL.                                        NC2234.2
209700*                                                                 NC2234.2
209800 INI-TEST-GF-9-3.                                                 NC2234.2
209900     ADD     1 TO REC-CT.                                         NC2234.2
210000     MOVE   "INI-TEST-GF-9-3" TO PAR-NAME.                        NC2234.2
210100     IF      TEST-1-1 = 001234                                    NC2234.2
210200             PERFORM PASS                                         NC2234.2
210300             GO TO INI-WRITE-GF-9-3                               NC2234.2
210400     ELSE                                                         NC2234.2
210500             GO TO INI-FAIL-GF-9-3.                               NC2234.2
210600 INI-DELETE-GF-9-3.                                               NC2234.2
210700     PERFORM DE-LETE.                                             NC2234.2
210800     GO TO INI-WRITE-GF-9-3.                                      NC2234.2
210900 INI-FAIL-GF-9-3.                                                 NC2234.2
211000     MOVE    001234   TO CORRECT-N                                NC2234.2
211100     MOVE    TEST-1-1 TO COMPUTED-X                               NC2234.2
211200     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
211300     PERFORM FAIL.                                                NC2234.2
211400 INI-WRITE-GF-9-3.                                                NC2234.2
211500     PERFORM PRINT-DETAIL.                                        NC2234.2
211600*                                                                 NC2234.2
211700 INI-TEST-GF-9-4.                                                 NC2234.2
211800     ADD     1 TO REC-CT.                                         NC2234.2
211900     MOVE   "INI-TEST-GF-9-4" TO PAR-NAME.                        NC2234.2
212000     IF      TEST-1-3 = "**********"                              NC2234.2
212100             PERFORM PASS                                         NC2234.2
212200             GO TO INI-WRITE-GF-9-4                               NC2234.2
212300     ELSE                                                         NC2234.2
212400             GO TO INI-FAIL-GF-9-4.                               NC2234.2
212500 INI-DELETE-GF-9-4.                                               NC2234.2
212600     PERFORM DE-LETE.                                             NC2234.2
212700     GO TO INI-WRITE-GF-9-4.                                      NC2234.2
212800 INI-FAIL-GF-9-4.                                                 NC2234.2
212900     MOVE   "**********" TO CORRECT-X                             NC2234.2
213000     MOVE    TEST-1-3  TO COMPUTED-X                              NC2234.2
213100     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
213200     PERFORM FAIL.                                                NC2234.2
213300 INI-WRITE-GF-9-4.                                                NC2234.2
213400     PERFORM PRINT-DETAIL.                                        NC2234.2
213500*                                                                 NC2234.2
213600 INI-TEST-GF-9-5.                                                 NC2234.2
213700     ADD     1 TO REC-CT.                                         NC2234.2
213800     MOVE   "INI-TEST-GF-9-5" TO PAR-NAME.                        NC2234.2
213900     IF      TEST-1-4 = "DD DD/DD"                                NC2234.2
214000             PERFORM PASS                                         NC2234.2
214100             GO TO INI-WRITE-GF-9-5                               NC2234.2
214200     ELSE                                                         NC2234.2
214300             GO TO INI-FAIL-GF-9-5.                               NC2234.2
214400 INI-DELETE-GF-9-5.                                               NC2234.2
214500     PERFORM DE-LETE.                                             NC2234.2
214600     GO TO INI-WRITE-GF-9-5.                                      NC2234.2
214700 INI-FAIL-GF-9-5.                                                 NC2234.2
214800     MOVE   "DD DD/DD" TO CORRECT-X                               NC2234.2
214900     MOVE    TEST-1-4  TO COMPUTED-X                              NC2234.2
215000     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
215100     PERFORM FAIL.                                                NC2234.2
215200 INI-WRITE-GF-9-5.                                                NC2234.2
215300     PERFORM PRINT-DETAIL.                                        NC2234.2
215400*                                                                 NC2234.2
215500 INI-TEST-GF-9-6.                                                 NC2234.2
215600     ADD     1 TO REC-CT.                                         NC2234.2
215700     MOVE   "INI-TEST-GF-9-6" TO PAR-NAME.                        NC2234.2
215800     IF      TEST-1-5 = "AAAAAA"                                  NC2234.2
215900             PERFORM PASS                                         NC2234.2
216000             GO TO INI-WRITE-GF-9-6                               NC2234.2
216100     ELSE                                                         NC2234.2
216200             GO TO INI-FAIL-GF-9-6.                               NC2234.2
216300 INI-DELETE-GF-9-6.                                               NC2234.2
216400     PERFORM DE-LETE.                                             NC2234.2
216500     GO TO INI-WRITE-GF-9-6.                                      NC2234.2
216600 INI-FAIL-GF-9-6.                                                 NC2234.2
216700     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
216800     MOVE    TEST-1-5 TO COMPUTED-X                               NC2234.2
216900     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
217000     PERFORM FAIL.                                                NC2234.2
217100 INI-WRITE-GF-9-6.                                                NC2234.2
217200     PERFORM PRINT-DETAIL.                                        NC2234.2
217300*                                                                 NC2234.2
217400 INI-TEST-GF-9-7.                                                 NC2234.2
217500     ADD     1 TO REC-CT.                                         NC2234.2
217600     MOVE   "INI-TEST-GF-9-7" TO PAR-NAME.                        NC2234.2
217700     IF      TEST-1-6 = 1234                                      NC2234.2
217800             PERFORM PASS                                         NC2234.2
217900             GO TO INI-WRITE-GF-9-7                               NC2234.2
218000     ELSE                                                         NC2234.2
218100             GO TO INI-FAIL-GF-9-7.                               NC2234.2
218200 INI-DELETE-GF-9-7.                                               NC2234.2
218300     PERFORM DE-LETE.                                             NC2234.2
218400     GO TO INI-WRITE-GF-9-7.                                      NC2234.2
218500 INI-FAIL-GF-9-7.                                                 NC2234.2
218600     MOVE    1234     TO CORRECT-N                                NC2234.2
218700     MOVE    TEST-1-6 TO COMPUTED-X                               NC2234.2
218800     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
218900     PERFORM FAIL.                                                NC2234.2
219000 INI-WRITE-GF-9-7.                                                NC2234.2
219100     PERFORM PRINT-DETAIL.                                        NC2234.2
219200*                                                                 NC2234.2
219300 INI-TEST-GF-9-8.                                                 NC2234.2
219400     ADD     1 TO REC-CT.                                         NC2234.2
219500     MOVE   "INI-TEST-GF-9-8" TO PAR-NAME.                        NC2234.2
219600     IF      TEST-1-8 = "**********"                              NC2234.2
219700             PERFORM PASS                                         NC2234.2
219800             GO TO INI-WRITE-GF-9-8                               NC2234.2
219900     ELSE                                                         NC2234.2
220000             GO TO INI-FAIL-GF-9-8.                               NC2234.2
220100 INI-DELETE-GF-9-8.                                               NC2234.2
220200     PERFORM DE-LETE.                                             NC2234.2
220300     GO TO INI-WRITE-GF-9-8.                                      NC2234.2
220400 INI-FAIL-GF-9-8.                                                 NC2234.2
220500     MOVE   "**********" TO CORRECT-X                             NC2234.2
220600     MOVE    TEST-1-8  TO COMPUTED-X                              NC2234.2
220700     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
220800     PERFORM FAIL.                                                NC2234.2
220900 INI-WRITE-GF-9-8.                                                NC2234.2
221000     PERFORM PRINT-DETAIL.                                        NC2234.2
221100*                                                                 NC2234.2
221200 INI-TEST-GF-9-9.                                                 NC2234.2
221300     ADD     1 TO REC-CT.                                         NC2234.2
221400     MOVE   "INI-TEST-GF-9-9" TO PAR-NAME.                        NC2234.2
221500     IF      TEST-1-9 = "DD DD/DD"                                NC2234.2
221600             PERFORM PASS                                         NC2234.2
221700             GO TO INI-WRITE-GF-9-9                               NC2234.2
221800     ELSE                                                         NC2234.2
221900             GO TO INI-FAIL-GF-9-9.                               NC2234.2
222000 INI-DELETE-GF-9-9.                                               NC2234.2
222100     PERFORM DE-LETE.                                             NC2234.2
222200     GO TO INI-WRITE-GF-9-9.                                      NC2234.2
222300 INI-FAIL-GF-9-9.                                                 NC2234.2
222400     MOVE   "DD DD/DD" TO CORRECT-X                               NC2234.2
222500     MOVE    TEST-1-9  TO COMPUTED-X                              NC2234.2
222600     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
222700     PERFORM FAIL.                                                NC2234.2
222800 INI-WRITE-GF-9-9.                                                NC2234.2
222900     PERFORM PRINT-DETAIL.                                        NC2234.2
223000*                                                                 NC2234.2
223100 INI-TEST-GF-9-10.                                                NC2234.2
223200     ADD     1 TO REC-CT.                                         NC2234.2
223300     MOVE   "INI-TEST-GF-9-10" TO PAR-NAME.                       NC2234.2
223400     IF      TEST-1-10 = "AAAAAA"                                 NC2234.2
223500             PERFORM PASS                                         NC2234.2
223600             GO TO INI-WRITE-GF-9-10                              NC2234.2
223700     ELSE                                                         NC2234.2
223800             GO TO INI-FAIL-GF-9-10.                              NC2234.2
223900 INI-DELETE-GF-9-10.                                              NC2234.2
224000     PERFORM DE-LETE.                                             NC2234.2
224100     GO TO INI-WRITE-GF-9-10.                                     NC2234.2
224200 INI-FAIL-GF-9-10.                                                NC2234.2
224300     MOVE   "AAAAAA"  TO CORRECT-X                                NC2234.2
224400     MOVE    TEST-1-10 TO COMPUTED-X                              NC2234.2
224500     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
224600     PERFORM FAIL.                                                NC2234.2
224700 INI-WRITE-GF-9-10.                                               NC2234.2
224800     PERFORM PRINT-DETAIL.                                        NC2234.2
224900*                                                                 NC2234.2
225000 INI-TEST-GF-9-11.                                                NC2234.2
225100     ADD     1 TO REC-CT.                                         NC2234.2
225200     MOVE   "INI-TEST-GF-9-11" TO PAR-NAME.                       NC2234.2
225300     IF      TEST-8-DATA-1 = "$234.00"                            NC2234.2
225400             PERFORM PASS                                         NC2234.2
225500             GO TO INI-WRITE-GF-9-11                              NC2234.2
225600     ELSE                                                         NC2234.2
225700             GO TO INI-FAIL-GF-9-11.                              NC2234.2
225800 INI-DELETE-GF-9-11.                                              NC2234.2
225900     PERFORM DE-LETE.                                             NC2234.2
226000     GO TO INI-WRITE-GF-9-11.                                     NC2234.2
226100 INI-FAIL-GF-9-11.                                                NC2234.2
226200     MOVE   "$234.00"  TO CORRECT-X                               NC2234.2
226300     MOVE    TEST-8-DATA-1  TO COMPUTED-X                         NC2234.2
226400     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
226500     PERFORM FAIL.                                                NC2234.2
226600 INI-WRITE-GF-9-11.                                               NC2234.2
226700     PERFORM PRINT-DETAIL.                                        NC2234.2
226800*                                                                 NC2234.2
226900 INI-TEST-GF-9-12.                                                NC2234.2
227000     ADD     1 TO REC-CT.                                         NC2234.2
227100     MOVE   "INI-TEST-GF-9-12" TO PAR-NAME.                       NC2234.2
227200     IF      TEST-8-DATA-2 = "AAAAAA    "                         NC2234.2
227300             PERFORM PASS                                         NC2234.2
227400             GO TO INI-WRITE-GF-9-12                              NC2234.2
227500     ELSE                                                         NC2234.2
227600             GO TO INI-FAIL-GF-9-12.                              NC2234.2
227700 INI-DELETE-GF-9-12.                                              NC2234.2
227800     PERFORM DE-LETE.                                             NC2234.2
227900     GO TO INI-WRITE-GF-9-12.                                     NC2234.2
228000 INI-FAIL-GF-9-12.                                                NC2234.2
228100     MOVE   "AAAAAA    " TO CORRECT-X                             NC2234.2
228200     MOVE    TEST-8-DATA-2 TO COMPUTED-X                          NC2234.2
228300     MOVE   "INCORRECTLY INITIALIZED" TO RE-MARK                  NC2234.2
228400     PERFORM FAIL.                                                NC2234.2
228500 INI-WRITE-GF-9-12.                                               NC2234.2
228600     PERFORM PRINT-DETAIL.                                        NC2234.2
228700*                                                                 NC2234.2
228800 CCVS-EXIT SECTION.                                               NC2234.2
228900 CCVS-999999.                                                     NC2234.2
229000     GO TO CLOSE-FILES.                                           NC2234.2
      *END-OF,NC223A                                                                  
