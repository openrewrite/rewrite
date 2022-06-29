      *HEADER,COBOL,NC111A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1114.2
000200 PROGRAM-ID.                                                      NC1114.2
000300     NC111A.                                                      NC1114.2
000400****************************************************************  NC1114.2
000500*                                                              *  NC1114.2
000600*    VALIDATION FOR:-                                          *  NC1114.2
000700*                                                              *  NC1114.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1114.2
000900*                                                              *  NC1114.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1114.2
001100*                                                              *  NC1114.2
001200****************************************************************  NC1114.2
001300*                                                              *  NC1114.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1114.2
001500*                                                              *  NC1114.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1114.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1114.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1114.2
001900*                                                              *  NC1114.2
002000****************************************************************  NC1114.2
002100*                                                                 NC1114.2
002200*   PROGRAM NC111A TESTS THE TRUNCATION OF RESULTANT IDENTIFIERS  NC1114.2
002300*         USING ADD SUBTRACT AND MULTIPLY STATEMENTS.             NC1114.2
002400*                                                                 NC1114.2
002500*                                                                 NC1114.2
002600                                                                  NC1114.2
002700 ENVIRONMENT DIVISION.                                            NC1114.2
002800 CONFIGURATION SECTION.                                           NC1114.2
002900 SOURCE-COMPUTER.                                                 NC1114.2
003000     XXXXX082.                                                    NC1114.2
003100 OBJECT-COMPUTER.                                                 NC1114.2
003200     XXXXX083.                                                    NC1114.2
003300 INPUT-OUTPUT SECTION.                                            NC1114.2
003400 FILE-CONTROL.                                                    NC1114.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1114.2
003600     XXXXX055.                                                    NC1114.2
003700 DATA DIVISION.                                                   NC1114.2
003800 FILE SECTION.                                                    NC1114.2
003900 FD  PRINT-FILE.                                                  NC1114.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1114.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1114.2
004200 WORKING-STORAGE SECTION.                                         NC1114.2
004300 01  N-11                               PICTURE IS 9 VALUE IS 9.  NC1114.2
004400 01  N-12                               PICTURE IS 9 VALUE IS 9.  NC1114.2
004500 01  N-40                               PICTURE IS 9(7)           NC1114.2
004600     VALUE IS 7777777.                                            NC1114.2
004700 01  N-41                               PICTURE IS 9(7)           NC1114.2
004800     VALUE IS 1111111.                                            NC1114.2
004900 01  N-42                               PICTURE IS 9(3)P(4).      NC1114.2
005000 01  TRUNC-DATA.                                                  NC1114.2
005100     02 N-43  PICTURE S9V9 VALUE +1.6.                            NC1114.2
005200     02 N-44  PICTURE S9V9 VALUE -1.6.                            NC1114.2
005300     02 N-45  PICTURE S9.                                         NC1114.2
005400 01  TEST-RESULTS.                                                NC1114.2
005500     02 FILLER                   PIC X      VALUE SPACE.          NC1114.2
005600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1114.2
005700     02 FILLER                   PIC X      VALUE SPACE.          NC1114.2
005800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1114.2
005900     02 FILLER                   PIC X      VALUE SPACE.          NC1114.2
006000     02  PAR-NAME.                                                NC1114.2
006100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1114.2
006200       03  PARDOT-X              PIC X      VALUE SPACE.          NC1114.2
006300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1114.2
006400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1114.2
006500     02 RE-MARK                  PIC X(61).                       NC1114.2
006600 01  TEST-COMPUTED.                                               NC1114.2
006700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1114.2
006800     02 FILLER                   PIC X(17)  VALUE                 NC1114.2
006900            "       COMPUTED=".                                   NC1114.2
007000     02 COMPUTED-X.                                               NC1114.2
007100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1114.2
007200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1114.2
007300                                 PIC -9(9).9(9).                  NC1114.2
007400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1114.2
007500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1114.2
007600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1114.2
007700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1114.2
007800         04 COMPUTED-18V0                    PIC -9(18).          NC1114.2
007900         04 FILLER                           PIC X.               NC1114.2
008000     03 FILLER PIC X(50) VALUE SPACE.                             NC1114.2
008100 01  TEST-CORRECT.                                                NC1114.2
008200     02 FILLER PIC X(30) VALUE SPACE.                             NC1114.2
008300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1114.2
008400     02 CORRECT-X.                                                NC1114.2
008500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1114.2
008600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1114.2
008700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1114.2
008800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1114.2
008900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1114.2
009000     03      CR-18V0 REDEFINES CORRECT-A.                         NC1114.2
009100         04 CORRECT-18V0                     PIC -9(18).          NC1114.2
009200         04 FILLER                           PIC X.               NC1114.2
009300     03 FILLER PIC X(2) VALUE SPACE.                              NC1114.2
009400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1114.2
009500 01  CCVS-C-1.                                                    NC1114.2
009600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1114.2
009700-    "SS  PARAGRAPH-NAME                                          NC1114.2
009800-    "       REMARKS".                                            NC1114.2
009900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1114.2
010000 01  CCVS-C-2.                                                    NC1114.2
010100     02 FILLER                     PIC X        VALUE SPACE.      NC1114.2
010200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1114.2
010300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1114.2
010400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1114.2
010500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1114.2
010600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1114.2
010700 01  REC-CT                        PIC 99       VALUE ZERO.       NC1114.2
010800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1114.2
010900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1114.2
011000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1114.2
011100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1114.2
011200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1114.2
011300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1114.2
011400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1114.2
011500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1114.2
011600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1114.2
011700 01  CCVS-H-1.                                                    NC1114.2
011800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1114.2
011900     02  FILLER                    PIC X(42)    VALUE             NC1114.2
012000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1114.2
012100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1114.2
012200 01  CCVS-H-2A.                                                   NC1114.2
012300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1114.2
012400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1114.2
012500   02  FILLER                        PIC XXXX   VALUE             NC1114.2
012600     "4.2 ".                                                      NC1114.2
012700   02  FILLER                        PIC X(28)  VALUE             NC1114.2
012800            " COPY - NOT FOR DISTRIBUTION".                       NC1114.2
012900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1114.2
013000                                                                  NC1114.2
013100 01  CCVS-H-2B.                                                   NC1114.2
013200   02  FILLER                        PIC X(15)  VALUE             NC1114.2
013300            "TEST RESULT OF ".                                    NC1114.2
013400   02  TEST-ID                       PIC X(9).                    NC1114.2
013500   02  FILLER                        PIC X(4)   VALUE             NC1114.2
013600            " IN ".                                               NC1114.2
013700   02  FILLER                        PIC X(12)  VALUE             NC1114.2
013800     " HIGH       ".                                              NC1114.2
013900   02  FILLER                        PIC X(22)  VALUE             NC1114.2
014000            " LEVEL VALIDATION FOR ".                             NC1114.2
014100   02  FILLER                        PIC X(58)  VALUE             NC1114.2
014200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1114.2
014300 01  CCVS-H-3.                                                    NC1114.2
014400     02  FILLER                      PIC X(34)  VALUE             NC1114.2
014500            " FOR OFFICIAL USE ONLY    ".                         NC1114.2
014600     02  FILLER                      PIC X(58)  VALUE             NC1114.2
014700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1114.2
014800     02  FILLER                      PIC X(28)  VALUE             NC1114.2
014900            "  COPYRIGHT   1985 ".                                NC1114.2
015000 01  CCVS-E-1.                                                    NC1114.2
015100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1114.2
015200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1114.2
015300     02 ID-AGAIN                     PIC X(9).                    NC1114.2
015400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1114.2
015500 01  CCVS-E-2.                                                    NC1114.2
015600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1114.2
015700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1114.2
015800     02 CCVS-E-2-2.                                               NC1114.2
015900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1114.2
016000         03 FILLER                   PIC X      VALUE SPACE.      NC1114.2
016100         03 ENDER-DESC               PIC X(44)  VALUE             NC1114.2
016200            "ERRORS ENCOUNTERED".                                 NC1114.2
016300 01  CCVS-E-3.                                                    NC1114.2
016400     02  FILLER                      PIC X(22)  VALUE             NC1114.2
016500            " FOR OFFICIAL USE ONLY".                             NC1114.2
016600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1114.2
016700     02  FILLER                      PIC X(58)  VALUE             NC1114.2
016800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1114.2
016900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1114.2
017000     02 FILLER                       PIC X(15)  VALUE             NC1114.2
017100             " COPYRIGHT 1985".                                   NC1114.2
017200 01  CCVS-E-4.                                                    NC1114.2
017300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1114.2
017400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1114.2
017500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1114.2
017600     02 FILLER                       PIC X(40)  VALUE             NC1114.2
017700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1114.2
017800 01  XXINFO.                                                      NC1114.2
017900     02 FILLER                       PIC X(19)  VALUE             NC1114.2
018000            "*** INFORMATION ***".                                NC1114.2
018100     02 INFO-TEXT.                                                NC1114.2
018200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1114.2
018300       04 XXCOMPUTED                 PIC X(20).                   NC1114.2
018400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1114.2
018500       04 XXCORRECT                  PIC X(20).                   NC1114.2
018600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1114.2
018700 01  HYPHEN-LINE.                                                 NC1114.2
018800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1114.2
018900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1114.2
019000-    "*****************************************".                 NC1114.2
019100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1114.2
019200-    "******************************".                            NC1114.2
019300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1114.2
019400     "NC111A".                                                    NC1114.2
019500 PROCEDURE DIVISION.                                              NC1114.2
019600 CCVS1 SECTION.                                                   NC1114.2
019700 OPEN-FILES.                                                      NC1114.2
019800     OPEN     OUTPUT PRINT-FILE.                                  NC1114.2
019900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1114.2
020000     MOVE    SPACE TO TEST-RESULTS.                               NC1114.2
020100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1114.2
020200     GO TO CCVS1-EXIT.                                            NC1114.2
020300 CLOSE-FILES.                                                     NC1114.2
020400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1114.2
020500 TERMINATE-CCVS.                                                  NC1114.2
020600*    EXIT PROGRAM.                                                NC1114.2
020700*TERMINATE-CALL.                                                  NC1114.2
020800     STOP     RUN.                                                NC1114.2
020900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1114.2
021000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1114.2
021100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1114.2
021200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1114.2
021300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1114.2
021400 PRINT-DETAIL.                                                    NC1114.2
021500     IF REC-CT NOT EQUAL TO ZERO                                  NC1114.2
021600             MOVE "." TO PARDOT-X                                 NC1114.2
021700             MOVE REC-CT TO DOTVALUE.                             NC1114.2
021800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1114.2
021900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1114.2
022000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1114.2
022100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1114.2
022200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1114.2
022300     MOVE SPACE TO CORRECT-X.                                     NC1114.2
022400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1114.2
022500     MOVE     SPACE TO RE-MARK.                                   NC1114.2
022600 HEAD-ROUTINE.                                                    NC1114.2
022700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1114.2
022800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1114.2
022900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1114.2
023000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1114.2
023100 COLUMN-NAMES-ROUTINE.                                            NC1114.2
023200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1114.2
023300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1114.2
023400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1114.2
023500 END-ROUTINE.                                                     NC1114.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1114.2
023700 END-RTN-EXIT.                                                    NC1114.2
023800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1114.2
023900 END-ROUTINE-1.                                                   NC1114.2
024000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1114.2
024100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1114.2
024200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1114.2
024300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1114.2
024400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1114.2
024500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1114.2
024600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1114.2
024700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1114.2
024800  END-ROUTINE-12.                                                 NC1114.2
024900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1114.2
025000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1114.2
025100         MOVE "NO " TO ERROR-TOTAL                                NC1114.2
025200         ELSE                                                     NC1114.2
025300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1114.2
025400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1114.2
025500     PERFORM WRITE-LINE.                                          NC1114.2
025600 END-ROUTINE-13.                                                  NC1114.2
025700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1114.2
025800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1114.2
025900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1114.2
026000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1114.2
026100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1114.2
026200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1114.2
026300          MOVE "NO " TO ERROR-TOTAL                               NC1114.2
026400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1114.2
026500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1114.2
026600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1114.2
026700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1114.2
026800 WRITE-LINE.                                                      NC1114.2
026900     ADD 1 TO RECORD-COUNT.                                       NC1114.2
027000*    IF RECORD-COUNT GREATER 42                                   NC1114.2
027100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1114.2
027200*        MOVE SPACE TO DUMMY-RECORD                               NC1114.2
027300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1114.2
027400*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1114.2
027500*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1114.2
027600*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1114.2
027700*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1114.2
027800*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1114.2
027900*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1114.2
028000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1114.2
028100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1114.2
028200*        MOVE ZERO TO RECORD-COUNT.                               NC1114.2
028300     PERFORM WRT-LN.                                              NC1114.2
028400 WRT-LN.                                                          NC1114.2
028500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1114.2
028600     MOVE SPACE TO DUMMY-RECORD.                                  NC1114.2
028700 BLANK-LINE-PRINT.                                                NC1114.2
028800     PERFORM WRT-LN.                                              NC1114.2
028900 FAIL-ROUTINE.                                                    NC1114.2
029000     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1114.2
029100            GO TO FAIL-ROUTINE-WRITE.                             NC1114.2
029200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1114.2
029300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1114.2
029400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1114.2
029500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1114.2
029600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1114.2
029700     GO TO  FAIL-ROUTINE-EX.                                      NC1114.2
029800 FAIL-ROUTINE-WRITE.                                              NC1114.2
029900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1114.2
030000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1114.2
030100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1114.2
030200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1114.2
030300 FAIL-ROUTINE-EX. EXIT.                                           NC1114.2
030400 BAIL-OUT.                                                        NC1114.2
030500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1114.2
030600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1114.2
030700 BAIL-OUT-WRITE.                                                  NC1114.2
030800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1114.2
030900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1114.2
031000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1114.2
031100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1114.2
031200 BAIL-OUT-EX. EXIT.                                               NC1114.2
031300 CCVS1-EXIT.                                                      NC1114.2
031400     EXIT.                                                        NC1114.2
031500 SECT-NC111A-001 SECTION.                                         NC1114.2
031600 BLURB-REMARK.                                                    NC1114.2
031700     MOVE     SPACE TO TEST-RESULTS.                              NC1114.2
031800     MOVE "THE FOLLOWING 3 TESTS TEST " TO RE-MARK.               NC1114.2
031900     PERFORM  PRINT-DETAIL.                                       NC1114.2
032000     MOVE "SOME SPECIFIC FEATURES OF  " TO RE-MARK.               NC1114.2
032100     PERFORM  PRINT-DETAIL.                                       NC1114.2
032200     MOVE "THE TRUNCATION, ROUNDED AND" TO RE-MARK.               NC1114.2
032300     PERFORM  PRINT-DETAIL.                                       NC1114.2
032400     MOVE "SIZE ERROR OPTIONS. GENERAL" TO RE-MARK.               NC1114.2
032500     PERFORM  PRINT-DETAIL.                                       NC1114.2
032600     MOVE "FEATURES OF THESE OPTIONS  " TO RE-MARK.               NC1114.2
032700     PERFORM  PRINT-DETAIL.                                       NC1114.2
032800     MOVE "ARE TESTED IN THE ADD, SUB-" TO RE-MARK.               NC1114.2
032900     PERFORM  PRINT-DETAIL.                                       NC1114.2
033000     MOVE "TRACT, MULTIPLY, AND DIVIDE" TO RE-MARK.               NC1114.2
033100     PERFORM  PRINT-DETAIL.                                       NC1114.2
033200     MOVE "TESTS.                     " TO RE-MARK.               NC1114.2
033300     PERFORM  PRINT-DETAIL.                                       NC1114.2
033400 TRU-INIT-GF-1.                                                   NC1114.2
033500     MOVE   "V1-67 6.4.1" TO ANSI-REFERENCE.                      NC1114.2
033600     MOVE    7777777 TO N-40.                                     NC1114.2
033700     MOVE    1111111 TO N-41.                                     NC1114.2
033800 TRU-TEST-GF-1-0.                                                 NC1114.2
033900     ADD      N-40 N-41 GIVING N-42.                              NC1114.2
034000 TRU-TEST-GF-1-1.                                                 NC1114.2
034100     IF       N-42 EQUAL TO 8880000                               NC1114.2
034200              PERFORM PASS                                        NC1114.2
034300              GO TO TRU-WRITE-GF-1.                               NC1114.2
034400     GO TO    TRU-FAIL-GF-1.                                      NC1114.2
034500 TRU-DELETE-GF-1.                                                 NC1114.2
034600     PERFORM  DE-LETE.                                            NC1114.2
034700     GO TO    TRU-WRITE-GF-1.                                     NC1114.2
034800 TRU-FAIL-GF-1.                                                   NC1114.2
034900     MOVE     N-42 TO COMPUTED-N.                                 NC1114.2
035000     MOVE     8880000 TO CORRECT-N.                               NC1114.2
035100     PERFORM  FAIL.                                               NC1114.2
035200 TRU-WRITE-GF-1.                                                  NC1114.2
035300     MOVE "TRUNCATION" TO FEATURE.                                NC1114.2
035400     MOVE "TRU-TEST-GF-1" TO PAR-NAME.                            NC1114.2
035500     PERFORM  PRINT-DETAIL.                                       NC1114.2
035600 TRU-INIT-GF-2.                                                   NC1114.2
035700     MOVE   "V1-67 6.4.1" TO ANSI-REFERENCE.                      NC1114.2
035800     MOVE    ZERO TO N-45.                                        NC1114.2
035900     MOVE   +1.6  TO N-43.                                        NC1114.2
036000 TRU-TEST-GF-2-0.                                                 NC1114.2
036100     ADD      N-43 1.4       GIVING N-45.                         NC1114.2
036200 TRU-TEST-GF-2-1.                                                 NC1114.2
036300     IF       N-45 EQUAL TO +3                                    NC1114.2
036400              PERFORM PASS GO TO TRU-WRITE-GF-2.                  NC1114.2
036500     GO       TO TRU-FAIL-GF-2.                                   NC1114.2
036600 TRU-DELETE-GF-2.                                                 NC1114.2
036700     PERFORM  DE-LETE.                                            NC1114.2
036800     GO       TO TRU-WRITE-GF-2.                                  NC1114.2
036900 TRU-FAIL-GF-2.                                                   NC1114.2
037000     PERFORM  FAIL.                                               NC1114.2
037100     MOVE     N-45 TO COMPUTED-N.                                 NC1114.2
037200     MOVE     3 TO CORRECT-N.                                     NC1114.2
037300 TRU-WRITE-GF-2.                                                  NC1114.2
037400     MOVE     "TRU-TEST-GF-2 " TO PAR-NAME.                       NC1114.2
037500     PERFORM  PRINT-DETAIL.                                       NC1114.2
037600 TRU-INIT-GF-3.                                                   NC1114.2
037700     MOVE   "V1-67 6.4.1" TO ANSI-REFERENCE.                      NC1114.2
037800     MOVE    ZERO TO N-45.                                        NC1114.2
037900     MOVE   -1.6  TO N-44.                                        NC1114.2
038000 TRU-TEST-GF-3-0.                                                 NC1114.2
038100     ADD      N-44 -1.4      GIVING N-45.                         NC1114.2
038200 TRU-TEST-GF-3-1.                                                 NC1114.2
038300     IF       N-45 EQUAL TO -3                                    NC1114.2
038400              PERFORM PASS GO TO TRU-WRITE-GF-3.                  NC1114.2
038500     GO       TO TRU-FAIL-GF-3.                                   NC1114.2
038600 TRU-DELETE-GF-3.                                                 NC1114.2
038700     PERFORM  DE-LETE.                                            NC1114.2
038800     GO       TO TRU-WRITE-GF-3.                                  NC1114.2
038900 TRU-FAIL-GF-3.                                                   NC1114.2
039000     PERFORM  FAIL.                                               NC1114.2
039100     MOVE     N-45 TO COMPUTED-N.                                 NC1114.2
039200     MOVE     -3 TO CORRECT-N.                                    NC1114.2
039300 TRU-WRITE-GF-3.                                                  NC1114.2
039400     MOVE     "TRU-TEST-GF-3 " TO PAR-NAME.                       NC1114.2
039500     PERFORM  PRINT-DETAIL.                                       NC1114.2
039600 TRU-INIT-GF-4.                                                   NC1114.2
039700     MOVE   "V1-67 6.4.1" TO ANSI-REFERENCE.                      NC1114.2
039800     MOVE    ZERO TO N-45.                                        NC1114.2
039900     MOVE   +1.6  TO N-43.                                        NC1114.2
040000 TRU-TEST-GF-4-0.                                                 NC1114.2
040100     MULTIPLY 5 BY N-43      GIVING N-45.                         NC1114.2
040200 TRU-TEST-GF-4-1.                                                 NC1114.2
040300     IF       N-45 EQUAL TO +8                                    NC1114.2
040400              PERFORM PASS GO TO TRU-WRITE-GF-4.                  NC1114.2
040500     GO       TO TRU-FAIL-GF-4.                                   NC1114.2
040600 TRU-DELETE-GF-4.                                                 NC1114.2
040700     PERFORM  DE-LETE.                                            NC1114.2
040800     GO       TO TRU-WRITE-GF-4.                                  NC1114.2
040900 TRU-FAIL-GF-4.                                                   NC1114.2
041000     PERFORM  FAIL.                                               NC1114.2
041100     MOVE     N-45 TO COMPUTED-N.                                 NC1114.2
041200     MOVE     8 TO CORRECT-N.                                     NC1114.2
041300 TRU-WRITE-GF-4.                                                  NC1114.2
041400     MOVE     "TRU-TEST-GF-4 " TO PAR-NAME.                       NC1114.2
041500     PERFORM  PRINT-DETAIL.                                       NC1114.2
041600 TRU-INIT-GF-5.                                                   NC1114.2
041700     MOVE   "V1-67 6.4.1" TO ANSI-REFERENCE.                      NC1114.2
041800     MOVE    ZERO TO N-45.                                        NC1114.2
041900     MOVE   -1.6  TO N-44.                                        NC1114.2
042000 TRU-TEST-GF-5-0.                                                 NC1114.2
042100     MULTIPLY 5 BY N-44      GIVING N-45.                         NC1114.2
042200 TRU-TEST-GF-5-1.                                                 NC1114.2
042300     IF       N-45 EQUAL TO -8                                    NC1114.2
042400              PERFORM PASS GO TO TRU-WRITE-GF-5.                  NC1114.2
042500     GO       TO TRU-FAIL-GF-5.                                   NC1114.2
042600 TRU-DELETE-GF-5.                                                 NC1114.2
042700     PERFORM  DE-LETE.                                            NC1114.2
042800     GO       TO TRU-WRITE-GF-5.                                  NC1114.2
042900 TRU-FAIL-GF-5.                                                   NC1114.2
043000     PERFORM  FAIL.                                               NC1114.2
043100     MOVE     N-45 TO COMPUTED-N.                                 NC1114.2
043200     MOVE     -8 TO CORRECT-N.                                    NC1114.2
043300 TRU-WRITE-GF-5.                                                  NC1114.2
043400     MOVE     "TRU-TEST-GF-5 " TO PAR-NAME.                       NC1114.2
043500     PERFORM  PRINT-DETAIL.                                       NC1114.2
043600 TRU-INIT-GF-6.                                                   NC1114.2
043700     MOVE   "V1-67 6.4.1" TO ANSI-REFERENCE.                      NC1114.2
043800     MOVE    ZERO TO N-45.                                        NC1114.2
043900     MOVE   +1.6  TO N-43.                                        NC1114.2
044000 TRU-TEST-GF-6-0.                                                 NC1114.2
044100     SUBTRACT -1.4 FROM N-43 GIVING N-45.                         NC1114.2
044200 TRU-TEST-GF-6-1.                                                 NC1114.2
044300     IF       N-45 EQUAL TO +3                                    NC1114.2
044400              PERFORM PASS GO TO TRU-WRITE-GF-6.                  NC1114.2
044500     GO       TO TRU-FAIL-GF-6.                                   NC1114.2
044600 TRU-DELETE-GF-6.                                                 NC1114.2
044700     PERFORM  DE-LETE.                                            NC1114.2
044800     GO       TO TRU-WRITE-GF-6.                                  NC1114.2
044900 TRU-FAIL-GF-6.                                                   NC1114.2
045000     PERFORM  FAIL.                                               NC1114.2
045100     MOVE     N-45 TO COMPUTED-N.                                 NC1114.2
045200     MOVE     3 TO CORRECT-N.                                     NC1114.2
045300 TRU-WRITE-GF-6.                                                  NC1114.2
045400     MOVE     "TRU-TEST-GF-6 " TO PAR-NAME.                       NC1114.2
045500     PERFORM  PRINT-DETAIL.                                       NC1114.2
045600 TRU-INIT-GF-7.                                                   NC1114.2
045700     MOVE   "V1-67 6.4.1" TO ANSI-REFERENCE.                      NC1114.2
045800     MOVE    ZERO TO N-45.                                        NC1114.2
045900     MOVE   -1.6  TO N-44.                                        NC1114.2
046000 TRU-TEST-GF-7-0.                                                 NC1114.2
046100     SUBTRACT +1.4 FROM N-44 GIVING N-45.                         NC1114.2
046200 TRU-TEST-GF-7-1.                                                 NC1114.2
046300     IF       N-45 EQUAL TO -3                                    NC1114.2
046400              PERFORM PASS GO TO TRU-WRITE-GF-7.                  NC1114.2
046500     GO       TO TRU-FAIL-GF-7.                                   NC1114.2
046600 TRU-DELETE-GF-7.                                                 NC1114.2
046700     PERFORM  DE-LETE.                                            NC1114.2
046800     GO       TO TRU-WRITE-GF-7.                                  NC1114.2
046900 TRU-FAIL-GF-7.                                                   NC1114.2
047000     PERFORM  FAIL.                                               NC1114.2
047100     MOVE     N-45 TO COMPUTED-N.                                 NC1114.2
047200     MOVE     -3 TO CORRECT-N.                                    NC1114.2
047300 TRU-WRITE-GF-7.                                                  NC1114.2
047400     MOVE     "TRU-TEST-GF-7 " TO PAR-NAME.                       NC1114.2
047500     PERFORM  PRINT-DETAIL.                                       NC1114.2
047600 CCVS-EXIT SECTION.                                               NC1114.2
047700 CCVS-999999.                                                     NC1114.2
047800     GO TO CLOSE-FILES.                                           NC1114.2
      *END-OF,NC111A                                                                  
