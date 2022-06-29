      *HEADER,COBOL,NC131A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1314.2
000200 PROGRAM-ID.                                                      NC1314.2
000300     NC131A.                                                      NC1314.2
000400****************************************************************  NC1314.2
000500*                                                              *  NC1314.2
000600*    VALIDATION FOR:-                                          *  NC1314.2
000700*                                                              *  NC1314.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1314.2
000900*                                                              *  NC1314.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1314.2
001100*                                                              *  NC1314.2
001200****************************************************************  NC1314.2
001300*                                                              *  NC1314.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1314.2
001500*                                                              *  NC1314.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1314.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1314.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1314.2
001900*                                                              *  NC1314.2
002000****************************************************************  NC1314.2
002100*                                                                 NC1314.2
002200*    PROGRAM NC131A TESTS FORMAT 1 OF THE SET STATEMENT USING     NC1314.2
002300*    VARIOUS COMBINATIONS OF INDEX-NAMES, IDENTIFIERS & INTEGERS  NC1314.2
002400*                                                                 NC1314.2
002500 ENVIRONMENT DIVISION.                                            NC1314.2
002600 CONFIGURATION SECTION.                                           NC1314.2
002700 SOURCE-COMPUTER.                                                 NC1314.2
002800     XXXXX082.                                                    NC1314.2
002900 OBJECT-COMPUTER.                                                 NC1314.2
003000     XXXXX083.                                                    NC1314.2
003100 INPUT-OUTPUT SECTION.                                            NC1314.2
003200 FILE-CONTROL.                                                    NC1314.2
003300     SELECT PRINT-FILE ASSIGN TO                                  NC1314.2
003400     XXXXX055.                                                    NC1314.2
003500 DATA DIVISION.                                                   NC1314.2
003600 FILE SECTION.                                                    NC1314.2
003700 FD  PRINT-FILE.                                                  NC1314.2
003800 01  PRINT-REC PICTURE X(120).                                    NC1314.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 NC1314.2
004000 WORKING-STORAGE SECTION.                                         NC1314.2
004100 77  I-DATA-1  USAGE IS INDEX.                                    NC1314.2
004200 77  IDENT-1   PICTURE   9 VALUE IS 4.                            NC1314.2
004300 77  IDENT-2   PICTURE 9.                                         NC1314.2
004400 77  IDENT-3  PICTURE S99  USAGE COMPUTATIONAL.                   NC1314.2
004500 77  SGN-IDX          PICTURE  9(18).                             NC1314.2
004600 01   INDEX-VALUE  PIC 9999.                                      NC1314.2
004700 01  I-DATA-GROUP USAGE IS INDEX.                                 NC1314.2
004800     02  I-DATA-2 USAGE IS INDEX.                                 NC1314.2
004900     02  I-DATA-3 USAGE IS INDEX.                                 NC1314.2
005000 01  TABLE-1.                                                     NC1314.2
005100     02  TAB1-REC  PICTURE  99  OCCURS 100 TIMES                  NC1314.2
005200                   INDEXED BY INDEX1.                             NC1314.2
005300 01  TABLE-2.                                                     NC1314.2
005400     02  TAB2-REC  PICTURE 999 OCCURS 100 TIMES                   NC1314.2
005500                   INDEXED BY INDEX2.                             NC1314.2
005600 01  TEST-RESULTS.                                                NC1314.2
005700     02 FILLER                   PIC X      VALUE SPACE.          NC1314.2
005800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1314.2
005900     02 FILLER                   PIC X      VALUE SPACE.          NC1314.2
006000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1314.2
006100     02 FILLER                   PIC X      VALUE SPACE.          NC1314.2
006200     02  PAR-NAME.                                                NC1314.2
006300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1314.2
006400       03  PARDOT-X              PIC X      VALUE SPACE.          NC1314.2
006500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1314.2
006600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1314.2
006700     02 RE-MARK                  PIC X(61).                       NC1314.2
006800 01  TEST-COMPUTED.                                               NC1314.2
006900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1314.2
007000     02 FILLER                   PIC X(17)  VALUE                 NC1314.2
007100            "       COMPUTED=".                                   NC1314.2
007200     02 COMPUTED-X.                                               NC1314.2
007300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1314.2
007400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1314.2
007500                                 PIC -9(9).9(9).                  NC1314.2
007600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1314.2
007700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1314.2
007800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1314.2
007900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1314.2
008000         04 COMPUTED-18V0                    PIC -9(18).          NC1314.2
008100         04 FILLER                           PIC X.               NC1314.2
008200     03 FILLER PIC X(50) VALUE SPACE.                             NC1314.2
008300 01  TEST-CORRECT.                                                NC1314.2
008400     02 FILLER PIC X(30) VALUE SPACE.                             NC1314.2
008500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1314.2
008600     02 CORRECT-X.                                                NC1314.2
008700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1314.2
008800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1314.2
008900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1314.2
009000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1314.2
009100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1314.2
009200     03      CR-18V0 REDEFINES CORRECT-A.                         NC1314.2
009300         04 CORRECT-18V0                     PIC -9(18).          NC1314.2
009400         04 FILLER                           PIC X.               NC1314.2
009500     03 FILLER PIC X(2) VALUE SPACE.                              NC1314.2
009600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1314.2
009700 01  CCVS-C-1.                                                    NC1314.2
009800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1314.2
009900-    "SS  PARAGRAPH-NAME                                          NC1314.2
010000-    "       REMARKS".                                            NC1314.2
010100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1314.2
010200 01  CCVS-C-2.                                                    NC1314.2
010300     02 FILLER                     PIC X        VALUE SPACE.      NC1314.2
010400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1314.2
010500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1314.2
010600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1314.2
010700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1314.2
010800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1314.2
010900 01  REC-CT                        PIC 99       VALUE ZERO.       NC1314.2
011000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1314.2
011100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1314.2
011200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1314.2
011300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1314.2
011400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1314.2
011500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1314.2
011600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1314.2
011700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1314.2
011800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1314.2
011900 01  CCVS-H-1.                                                    NC1314.2
012000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1314.2
012100     02  FILLER                    PIC X(42)    VALUE             NC1314.2
012200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1314.2
012300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1314.2
012400 01  CCVS-H-2A.                                                   NC1314.2
012500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1314.2
012600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1314.2
012700   02  FILLER                        PIC XXXX   VALUE             NC1314.2
012800     "4.2 ".                                                      NC1314.2
012900   02  FILLER                        PIC X(28)  VALUE             NC1314.2
013000            " COPY - NOT FOR DISTRIBUTION".                       NC1314.2
013100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1314.2
013200                                                                  NC1314.2
013300 01  CCVS-H-2B.                                                   NC1314.2
013400   02  FILLER                        PIC X(15)  VALUE             NC1314.2
013500            "TEST RESULT OF ".                                    NC1314.2
013600   02  TEST-ID                       PIC X(9).                    NC1314.2
013700   02  FILLER                        PIC X(4)   VALUE             NC1314.2
013800            " IN ".                                               NC1314.2
013900   02  FILLER                        PIC X(12)  VALUE             NC1314.2
014000     " HIGH       ".                                              NC1314.2
014100   02  FILLER                        PIC X(22)  VALUE             NC1314.2
014200            " LEVEL VALIDATION FOR ".                             NC1314.2
014300   02  FILLER                        PIC X(58)  VALUE             NC1314.2
014400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1314.2
014500 01  CCVS-H-3.                                                    NC1314.2
014600     02  FILLER                      PIC X(34)  VALUE             NC1314.2
014700            " FOR OFFICIAL USE ONLY    ".                         NC1314.2
014800     02  FILLER                      PIC X(58)  VALUE             NC1314.2
014900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1314.2
015000     02  FILLER                      PIC X(28)  VALUE             NC1314.2
015100            "  COPYRIGHT   1985 ".                                NC1314.2
015200 01  CCVS-E-1.                                                    NC1314.2
015300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1314.2
015400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1314.2
015500     02 ID-AGAIN                     PIC X(9).                    NC1314.2
015600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1314.2
015700 01  CCVS-E-2.                                                    NC1314.2
015800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1314.2
015900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1314.2
016000     02 CCVS-E-2-2.                                               NC1314.2
016100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1314.2
016200         03 FILLER                   PIC X      VALUE SPACE.      NC1314.2
016300         03 ENDER-DESC               PIC X(44)  VALUE             NC1314.2
016400            "ERRORS ENCOUNTERED".                                 NC1314.2
016500 01  CCVS-E-3.                                                    NC1314.2
016600     02  FILLER                      PIC X(22)  VALUE             NC1314.2
016700            " FOR OFFICIAL USE ONLY".                             NC1314.2
016800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1314.2
016900     02  FILLER                      PIC X(58)  VALUE             NC1314.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1314.2
017100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1314.2
017200     02 FILLER                       PIC X(15)  VALUE             NC1314.2
017300             " COPYRIGHT 1985".                                   NC1314.2
017400 01  CCVS-E-4.                                                    NC1314.2
017500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1314.2
017600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1314.2
017700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1314.2
017800     02 FILLER                       PIC X(40)  VALUE             NC1314.2
017900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1314.2
018000 01  XXINFO.                                                      NC1314.2
018100     02 FILLER                       PIC X(19)  VALUE             NC1314.2
018200            "*** INFORMATION ***".                                NC1314.2
018300     02 INFO-TEXT.                                                NC1314.2
018400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1314.2
018500       04 XXCOMPUTED                 PIC X(20).                   NC1314.2
018600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1314.2
018700       04 XXCORRECT                  PIC X(20).                   NC1314.2
018800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1314.2
018900 01  HYPHEN-LINE.                                                 NC1314.2
019000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1314.2
019100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1314.2
019200-    "*****************************************".                 NC1314.2
019300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1314.2
019400-    "******************************".                            NC1314.2
019500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1314.2
019600     "NC131A".                                                    NC1314.2
019700 PROCEDURE DIVISION.                                              NC1314.2
019800 CCVS1 SECTION.                                                   NC1314.2
019900 OPEN-FILES.                                                      NC1314.2
020000     OPEN     OUTPUT PRINT-FILE.                                  NC1314.2
020100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1314.2
020200     MOVE    SPACE TO TEST-RESULTS.                               NC1314.2
020300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1314.2
020400     GO TO CCVS1-EXIT.                                            NC1314.2
020500 CLOSE-FILES.                                                     NC1314.2
020600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1314.2
020700 TERMINATE-CCVS.                                                  NC1314.2
020800*    EXIT PROGRAM.                                                NC1314.2
020900*TERMINATE-CALL.                                                  NC1314.2
021000     STOP     RUN.                                                NC1314.2
021100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1314.2
021200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1314.2
021300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1314.2
021400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1314.2
021500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1314.2
021600 PRINT-DETAIL.                                                    NC1314.2
021700     IF REC-CT NOT EQUAL TO ZERO                                  NC1314.2
021800             MOVE "." TO PARDOT-X                                 NC1314.2
021900             MOVE REC-CT TO DOTVALUE.                             NC1314.2
022000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1314.2
022100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1314.2
022200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1314.2
022300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1314.2
022400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1314.2
022500     MOVE SPACE TO CORRECT-X.                                     NC1314.2
022600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1314.2
022700     MOVE     SPACE TO RE-MARK.                                   NC1314.2
022800 HEAD-ROUTINE.                                                    NC1314.2
022900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1314.2
023000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1314.2
023100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1314.2
023200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1314.2
023300 COLUMN-NAMES-ROUTINE.                                            NC1314.2
023400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1314.2
023500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1314.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1314.2
023700 END-ROUTINE.                                                     NC1314.2
023800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1314.2
023900 END-RTN-EXIT.                                                    NC1314.2
024000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1314.2
024100 END-ROUTINE-1.                                                   NC1314.2
024200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1314.2
024300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1314.2
024400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1314.2
024500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1314.2
024600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1314.2
024700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1314.2
024800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1314.2
024900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1314.2
025000  END-ROUTINE-12.                                                 NC1314.2
025100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1314.2
025200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1314.2
025300         MOVE "NO " TO ERROR-TOTAL                                NC1314.2
025400         ELSE                                                     NC1314.2
025500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1314.2
025600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1314.2
025700     PERFORM WRITE-LINE.                                          NC1314.2
025800 END-ROUTINE-13.                                                  NC1314.2
025900     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1314.2
026000         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1314.2
026100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1314.2
026200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1314.2
026300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1314.2
026400      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1314.2
026500          MOVE "NO " TO ERROR-TOTAL                               NC1314.2
026600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1314.2
026700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1314.2
026800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1314.2
026900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1314.2
027000 WRITE-LINE.                                                      NC1314.2
027100     ADD 1 TO RECORD-COUNT.                                       NC1314.2
027200*    IF RECORD-COUNT GREATER 42                                   NC1314.2
027300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1314.2
027400*        MOVE SPACE TO DUMMY-RECORD                               NC1314.2
027500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1314.2
027600*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1314.2
027700*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1314.2
027800*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1314.2
027900*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1314.2
028000*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1314.2
028100*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1314.2
028200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1314.2
028300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1314.2
028400*        MOVE ZERO TO RECORD-COUNT.                               NC1314.2
028500     PERFORM WRT-LN.                                              NC1314.2
028600 WRT-LN.                                                          NC1314.2
028700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1314.2
028800     MOVE SPACE TO DUMMY-RECORD.                                  NC1314.2
028900 BLANK-LINE-PRINT.                                                NC1314.2
029000     PERFORM WRT-LN.                                              NC1314.2
029100 FAIL-ROUTINE.                                                    NC1314.2
029200     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1314.2
029300            GO TO FAIL-ROUTINE-WRITE.                             NC1314.2
029400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1314.2
029500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1314.2
029600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1314.2
029700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1314.2
029800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1314.2
029900     GO TO  FAIL-ROUTINE-EX.                                      NC1314.2
030000 FAIL-ROUTINE-WRITE.                                              NC1314.2
030100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1314.2
030200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1314.2
030300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1314.2
030400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1314.2
030500 FAIL-ROUTINE-EX. EXIT.                                           NC1314.2
030600 BAIL-OUT.                                                        NC1314.2
030700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1314.2
030800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1314.2
030900 BAIL-OUT-WRITE.                                                  NC1314.2
031000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1314.2
031100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1314.2
031200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1314.2
031300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1314.2
031400 BAIL-OUT-EX. EXIT.                                               NC1314.2
031500 CCVS1-EXIT.                                                      NC1314.2
031600     EXIT.                                                        NC1314.2
031700 SECT-NC131A-001 SECTION.                                         NC1314.2
031800 TEST-1.                                                          NC1314.2
031900     MOVE "VI-127 6.23.4" TO ANSI-REFERENCE.                      NC1314.2
032000     SET INDEX1 TO 5.                                             NC1314.2
032100     IF INDEX1 EQUAL TO 5 PERFORM PASS GO TO OK1.                 NC1314.2
032200     SET SGN-IDX TO INDEX1.                                       NC1314.2
032300     MOVE SGN-IDX TO COMPUTED-18V0.                               NC1314.2
032400     MOVE 5 TO CORRECT-18V0.                                      NC1314.2
032500     PERFORM FAIL.                                                NC1314.2
032600 OK1.                                                             NC1314.2
032700     MOVE "TEST-1" TO PAR-NAME.                                   NC1314.2
032800     MOVE  "SET  OPT 1" TO FEATURE.                               NC1314.2
032900     PERFORM PRINT-DETAIL.                                        NC1314.2
033000 TEST-2.                                                          NC1314.2
033100     SET INDEX1 TO IDENT-1.                                       NC1314.2
033200     IF INDEX1 EQUAL TO 4 PERFORM PASS GO TO OK2.                 NC1314.2
033300     SET SGN-IDX TO INDEX1.                                       NC1314.2
033400     MOVE SGN-IDX TO COMPUTED-18V0.                               NC1314.2
033500     MOVE IDENT-1 TO CORRECT-18V0.                                NC1314.2
033600     PERFORM FAIL.                                                NC1314.2
033700 OK2.                                                             NC1314.2
033800     MOVE "TEST-2" TO PAR-NAME.                                   NC1314.2
033900     MOVE "SET  OPT 2" TO FEATURE.                                NC1314.2
034000     PERFORM PRINT-DETAIL.                                        NC1314.2
034100 TEST-3.                                                          NC1314.2
034200     SET INDEX1 TO 4.                                             NC1314.2
034300     SET INDEX2 TO INDEX1.                                        NC1314.2
034400     IF INDEX2 EQUAL TO INDEX1 PERFORM PASS GO TO OK3.            NC1314.2
034500     SET SGN-IDX TO INDEX2.                                       NC1314.2
034600     MOVE SGN-IDX TO COMPUTED-18V0.                               NC1314.2
034700     MOVE 4 TO CORRECT-18V0.                                      NC1314.2
034800     PERFORM FAIL.                                                NC1314.2
034900 OK3.                                                             NC1314.2
035000     MOVE "TEST-3" TO PAR-NAME.                                   NC1314.2
035100     MOVE "SET  OPT 3" TO FEATURE.                                NC1314.2
035200     PERFORM PRINT-DETAIL.                                        NC1314.2
035300 TEST-4.                                                          NC1314.2
035400     SET INDEX2 TO 4.                                             NC1314.2
035500     SET I-DATA-1  TO INDEX2.                                     NC1314.2
035600     IF I-DATA-1 EQUAL TO INDEX2 PERFORM PASS GO TO OK4.          NC1314.2
035700     SET      INDEX-VALUE TO INDEX2.                              NC1314.2
035800     MOVE     INDEX-VALUE TO COMPUTED-18V0.                       NC1314.2
035900     MOVE 4 TO CORRECT-18V0.                                      NC1314.2
036000     PERFORM FAIL.                                                NC1314.2
036100 OK4.                                                             NC1314.2
036200     MOVE "TEST-4" TO PAR-NAME.                                   NC1314.2
036300     MOVE  "SET  OPT 4" TO FEATURE.                               NC1314.2
036400     PERFORM PRINT-DETAIL.                                        NC1314.2
036500 TEST-5.                                                          NC1314.2
036600     SET INDEX2 TO 4.                                             NC1314.2
036700     SET I-DATA-1 TO INDEX2.                                      NC1314.2
036800     SET I-DATA-2 TO I-DATA-1.                                    NC1314.2
036900     IF I-DATA-2 EQUAL TO I-DATA-1 PERFORM PASS GO TO OK5.        NC1314.2
037000     SET      INDEX-VALUE TO INDEX2.                              NC1314.2
037100     MOVE     INDEX-VALUE TO COMPUTED-18V0.                       NC1314.2
037200     MOVE 4 TO CORRECT-18V0.                                      NC1314.2
037300     PERFORM FAIL.                                                NC1314.2
037400 OK5.                                                             NC1314.2
037500     MOVE "TEST-5" TO PAR-NAME.                                   NC1314.2
037600     MOVE "SET  OPT 5" TO FEATURE.                                NC1314.2
037700     PERFORM PRINT-DETAIL.                                        NC1314.2
037800 TEST-6.                                                          NC1314.2
037900     SET INDEX2 TO 6.                                             NC1314.2
038000     SET IDENT-2 TO  INDEX2.                                      NC1314.2
038100     IF IDENT-2 EQUAL TO INDEX2 PERFORM PASS GO TO OK6.           NC1314.2
038200     SET SGN-IDX TO INDEX2.                                       NC1314.2
038300     MOVE SGN-IDX TO COMPUTED-18V0.                               NC1314.2
038400     MOVE 6 TO CORRECT-18V0.                                      NC1314.2
038500     PERFORM FAIL.                                                NC1314.2
038600 OK6.                                                             NC1314.2
038700     MOVE "TEST-6" TO PAR-NAME.                                   NC1314.2
038800     MOVE "SET  OPT 6" TO FEATURE.                                NC1314.2
038900     PERFORM PRINT-DETAIL.                                        NC1314.2
039000     MOVE SPACE TO FEATURE.                                       NC1314.2
039100 END-TEST.                                                        NC1314.2
039200     PERFORM BLANK-LINE-PRINT 2 TIMES.                            NC1314.2
039300      MOVE "ASCENDING NUMBER LIST" TO RE-MARK.                    NC1314.2
039400      PERFORM PRINT-DETAIL.                                       NC1314.2
039500     PERFORM BLANK-LINE-PRINT.                                    NC1314.2
039600     SET INDEX1 TO 1.                                             NC1314.2
039700     MOVE 1 TO IDENT-3.                                           NC1314.2
039800 L.  MOVE IDENT-3 TO TAB1-REC (INDEX1).                           NC1314.2
039900     IF   IDENT-3          EQUAL TO 99 GO TO P-LIST.              NC1314.2
040000     ADD 1 TO IDENT-3.                                            NC1314.2
040100     SET INDEX1 TO IDENT-3.                                       NC1314.2
040200     GO TO L.                                                     NC1314.2
040300 P-LIST.                                                          NC1314.2
040400     SET INDEX1 TO 1.                                             NC1314.2
040500     MOVE 1 TO IDENT-3.                                           NC1314.2
040600 M.                                                               NC1314.2
040700      MOVE TAB1-REC (INDEX1) TO RE-MARK.                          NC1314.2
040800      PERFORM PRINT-DETAIL.                                       NC1314.2
040900     IF IDENT-3 EQUAL TO 99 GO TO CL-OSE.                         NC1314.2
041000     ADD 1 TO  IDENT-3.                                           NC1314.2
041100     SET INDEX1 TO IDENT-3.                                       NC1314.2
041200     GO TO M.                                                     NC1314.2
041300 CL-OSE.                                                          NC1314.2
041400     PERFORM BLANK-LINE-PRINT.                                    NC1314.2
041500     MOVE "END OF TABLE LIST" TO RE-MARK.                         NC1314.2
041600     PERFORM PRINT-DETAIL.                                        NC1314.2
041700*                                                                 NC1314.2
041800 IDX-INIT-8.                                                      NC1314.2
041900     MOVE   "VI-127 6.23.4 GR3(c)" TO ANSI-REFERENCE.             NC1314.2
042000     SET     INDEX1 TO 4.                                         NC1314.2
042100 IDX-TEST-8-0.                                                    NC1314.2
042200     SET     INDEX2                                               NC1314.2
042300             IDENT-1 TO INDEX1.                                   NC1314.2
042400 IDX-TEST-8-1.                                                    NC1314.2
042500     IF      IDENT-1 EQUAL TO 4 PERFORM PASS GO TO OK8-1.         NC1314.2
042600     MOVE    IDENT-1 TO COMPUTED-18V0.                            NC1314.2
042700     MOVE    4 TO CORRECT-18V0.                                   NC1314.2
042800     PERFORM FAIL.                                                NC1314.2
042900 OK8-1.                                                           NC1314.2
043000     MOVE   "IDX-TEST-8-1" TO PAR-NAME.                           NC1314.2
043100     PERFORM PRINT-DETAIL.                                        NC1314.2
043200 IDX-TEST-8-2.                                                    NC1314.2
043300     IF      INDEX2  EQUAL TO INDEX1 PERFORM PASS GO TO OK8-2.    NC1314.2
043400     SET     INDEX-VALUE TO INDEX2.                               NC1314.2
043500     MOVE    INDEX-VALUE TO COMPUTED-18V0.                        NC1314.2
043600     MOVE    4 TO CORRECT-18V0.                                   NC1314.2
043700     PERFORM FAIL.                                                NC1314.2
043800 OK8-2.                                                           NC1314.2
043900     MOVE   "IDX-TEST-8-2" TO PAR-NAME.                           NC1314.2
044000     PERFORM PRINT-DETAIL.                                        NC1314.2
044100*                                                                 NC1314.2
044200 IDX-INIT-9.                                                      NC1314.2
044300     MOVE   "VI-127 6.23.4 GR3(c)" TO ANSI-REFERENCE.             NC1314.2
044400     SET     INDEX1 TO 4.                                         NC1314.2
044500 IDX-TEST-9-0.                                                    NC1314.2
044600     SET IDENT-1                                                  NC1314.2
044700         INDEX2  TO INDEX1.                                       NC1314.2
044800 IDX-TEST-9-1.                                                    NC1314.2
044900     IF      IDENT-1 EQUAL TO 4 PERFORM PASS GO TO OK9-1.         NC1314.2
045000     MOVE    IDENT-1 TO COMPUTED-18V0.                            NC1314.2
045100     MOVE    4 TO CORRECT-18V0.                                   NC1314.2
045200     PERFORM FAIL.                                                NC1314.2
045300 OK9-1.                                                           NC1314.2
045400     MOVE   "IDX-TEST-9-1" TO PAR-NAME.                           NC1314.2
045500     PERFORM PRINT-DETAIL.                                        NC1314.2
045600 IDX-TEST-9-2.                                                    NC1314.2
045700     IF      INDEX2  EQUAL TO INDEX1 PERFORM PASS GO TO OK9-2.    NC1314.2
045800     SET     INDEX-VALUE TO INDEX2.                               NC1314.2
045900     MOVE    INDEX-VALUE TO COMPUTED-18V0.                        NC1314.2
046000     MOVE    4 TO CORRECT-18V0.                                   NC1314.2
046100     PERFORM FAIL.                                                NC1314.2
046200 OK9-2.                                                           NC1314.2
046300     MOVE   "IDX-TEST-9-2" TO PAR-NAME.                           NC1314.2
046400     PERFORM PRINT-DETAIL.                                        NC1314.2
046500*                                                                 NC1314.2
046600 CCVS-EXIT SECTION.                                               NC1314.2
046700 CCVS-999999.                                                     NC1314.2
046800     GO TO CLOSE-FILES.                                           NC1314.2
      *END-OF,NC131A                                                                  
