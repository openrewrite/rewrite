      *HEADER,COBOL,NC135A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1354.2
000200 PROGRAM-ID.                                                      NC1354.2
000300     NC135A.                                                      NC1354.2
000400****************************************************************  NC1354.2
000500*                                                              *  NC1354.2
000600*    VALIDATION FOR:-                                          *  NC1354.2
000700*                                                              *  NC1354.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1354.2
000900*                                                              *  NC1354.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1354.2
001100*                                                              *  NC1354.2
001200****************************************************************  NC1354.2
001300*                                                              *  NC1354.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1354.2
001500*                                                              *  NC1354.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1354.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1354.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1354.2
001900*                                                              *  NC1354.2
002000****************************************************************  NC1354.2
002100*                                                                 NC1354.2
002200*     PROGRAM NC135A TESTS THE USE OF INDEX-NAMES TO REFERENCE    NC1354.2
002300*     3 DIMENSIONAL TABLE WHICH HAS BEEN REDEFINED.               NC1354.2
002400*     FORMAT 2 OF THE SET STATEMENT AND RELATIVE INDEXING ARE     NC1354.2
002500*     ALSO TESTED.                                                NC1354.2
002600*                                                                 NC1354.2
002700 ENVIRONMENT DIVISION.                                            NC1354.2
002800 CONFIGURATION SECTION.                                           NC1354.2
002900 SOURCE-COMPUTER.                                                 NC1354.2
003000     XXXXX082.                                                    NC1354.2
003100 OBJECT-COMPUTER.                                                 NC1354.2
003200     XXXXX083.                                                    NC1354.2
003300 INPUT-OUTPUT SECTION.                                            NC1354.2
003400 FILE-CONTROL.                                                    NC1354.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1354.2
003600     XXXXX055.                                                    NC1354.2
003700 DATA DIVISION.                                                   NC1354.2
003800 FILE SECTION.                                                    NC1354.2
003900 FD  PRINT-FILE.                                                  NC1354.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1354.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1354.2
004200 WORKING-STORAGE SECTION.                                         NC1354.2
004300 77  ONE PICTURE 999 VALUE IS 001.                                NC1354.2
004400 77  CTR-1 PICTURE 999 VALUE IS ZERO.                             NC1354.2
004500 77  W-3 PICTURE 999 VALUE ZERO.                                  NC1354.2
004600 01  IDEN-1 PICTURE 99 VALUE 03.                                  NC1354.2
004700 01  TABLE-9.                                                     NC1354.2
004800     02 TABLE-8 OCCURS 10 TIMES INDEXED BY INXEX1.                NC1354.2
004900     03 TABLE-7 OCCURS 10 TIMES INDEXED BY INXEX2.                NC1354.2
005000     04 TABLE-1 PICTURE 999 OCCURS 3 TIMES INDEXED BY INXEX3.     NC1354.2
005100 01  TABLE-6 REDEFINES TABLE-9.                                   NC1354.2
005200     02 TABLE-5 OCCURS 10 TIMES.                                  NC1354.2
005300     03 TABLE-4 OCCURS 10 TIMES.                                  NC1354.2
005400     04 TABLE-2 PICTURE 999 OCCURS 3 TIMES.                       NC1354.2
005500 01  NUMBER-LIST.                                                 NC1354.2
005600     02  FILLER PICTURE IS X VALUE IS SPACE.                      NC1354.2
005700     02 LINE-1 OCCURS 20 TIMES.                                   NC1354.2
005800       03 BLANKSPACE PICTURE IS XX.                               NC1354.2
005900       03 PRINT-ELE PICTURE IS 999.                               NC1354.2
006000 01  DATA-NAMES USAGE IS INDEX.                                   NC1354.2
006100     02 KEY-1.                                                    NC1354.2
006200     02 KEY-2.                                                    NC1354.2
006300     02 KEY-3.                                                    NC1354.2
006400 01  TEST-RESULTS.                                                NC1354.2
006500     02 FILLER                   PIC X      VALUE SPACE.          NC1354.2
006600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1354.2
006700     02 FILLER                   PIC X      VALUE SPACE.          NC1354.2
006800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1354.2
006900     02 FILLER                   PIC X      VALUE SPACE.          NC1354.2
007000     02  PAR-NAME.                                                NC1354.2
007100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1354.2
007200       03  PARDOT-X              PIC X      VALUE SPACE.          NC1354.2
007300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1354.2
007400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1354.2
007500     02 RE-MARK                  PIC X(61).                       NC1354.2
007600 01  TEST-COMPUTED.                                               NC1354.2
007700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1354.2
007800     02 FILLER                   PIC X(17)  VALUE                 NC1354.2
007900            "       COMPUTED=".                                   NC1354.2
008000     02 COMPUTED-X.                                               NC1354.2
008100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1354.2
008200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1354.2
008300                                 PIC -9(9).9(9).                  NC1354.2
008400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1354.2
008500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1354.2
008600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1354.2
008700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1354.2
008800         04 COMPUTED-18V0                    PIC -9(18).          NC1354.2
008900         04 FILLER                           PIC X.               NC1354.2
009000     03 FILLER PIC X(50) VALUE SPACE.                             NC1354.2
009100 01  TEST-CORRECT.                                                NC1354.2
009200     02 FILLER PIC X(30) VALUE SPACE.                             NC1354.2
009300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1354.2
009400     02 CORRECT-X.                                                NC1354.2
009500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1354.2
009600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1354.2
009700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1354.2
009800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1354.2
009900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1354.2
010000     03      CR-18V0 REDEFINES CORRECT-A.                         NC1354.2
010100         04 CORRECT-18V0                     PIC -9(18).          NC1354.2
010200         04 FILLER                           PIC X.               NC1354.2
010300     03 FILLER PIC X(2) VALUE SPACE.                              NC1354.2
010400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1354.2
010500 01  CCVS-C-1.                                                    NC1354.2
010600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1354.2
010700-    "SS  PARAGRAPH-NAME                                          NC1354.2
010800-    "       REMARKS".                                            NC1354.2
010900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1354.2
011000 01  CCVS-C-2.                                                    NC1354.2
011100     02 FILLER                     PIC X        VALUE SPACE.      NC1354.2
011200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1354.2
011300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1354.2
011400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1354.2
011500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1354.2
011600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1354.2
011700 01  REC-CT                        PIC 99       VALUE ZERO.       NC1354.2
011800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1354.2
011900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1354.2
012000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1354.2
012100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1354.2
012200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1354.2
012300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1354.2
012400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1354.2
012500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1354.2
012600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1354.2
012700 01  CCVS-H-1.                                                    NC1354.2
012800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1354.2
012900     02  FILLER                    PIC X(42)    VALUE             NC1354.2
013000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1354.2
013100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1354.2
013200 01  CCVS-H-2A.                                                   NC1354.2
013300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1354.2
013400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1354.2
013500   02  FILLER                        PIC XXXX   VALUE             NC1354.2
013600     "4.2 ".                                                      NC1354.2
013700   02  FILLER                        PIC X(28)  VALUE             NC1354.2
013800            " COPY - NOT FOR DISTRIBUTION".                       NC1354.2
013900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1354.2
014000                                                                  NC1354.2
014100 01  CCVS-H-2B.                                                   NC1354.2
014200   02  FILLER                        PIC X(15)  VALUE             NC1354.2
014300            "TEST RESULT OF ".                                    NC1354.2
014400   02  TEST-ID                       PIC X(9).                    NC1354.2
014500   02  FILLER                        PIC X(4)   VALUE             NC1354.2
014600            " IN ".                                               NC1354.2
014700   02  FILLER                        PIC X(12)  VALUE             NC1354.2
014800     " HIGH       ".                                              NC1354.2
014900   02  FILLER                        PIC X(22)  VALUE             NC1354.2
015000            " LEVEL VALIDATION FOR ".                             NC1354.2
015100   02  FILLER                        PIC X(58)  VALUE             NC1354.2
015200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1354.2
015300 01  CCVS-H-3.                                                    NC1354.2
015400     02  FILLER                      PIC X(34)  VALUE             NC1354.2
015500            " FOR OFFICIAL USE ONLY    ".                         NC1354.2
015600     02  FILLER                      PIC X(58)  VALUE             NC1354.2
015700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1354.2
015800     02  FILLER                      PIC X(28)  VALUE             NC1354.2
015900            "  COPYRIGHT   1985 ".                                NC1354.2
016000 01  CCVS-E-1.                                                    NC1354.2
016100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1354.2
016200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1354.2
016300     02 ID-AGAIN                     PIC X(9).                    NC1354.2
016400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1354.2
016500 01  CCVS-E-2.                                                    NC1354.2
016600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1354.2
016700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1354.2
016800     02 CCVS-E-2-2.                                               NC1354.2
016900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1354.2
017000         03 FILLER                   PIC X      VALUE SPACE.      NC1354.2
017100         03 ENDER-DESC               PIC X(44)  VALUE             NC1354.2
017200            "ERRORS ENCOUNTERED".                                 NC1354.2
017300 01  CCVS-E-3.                                                    NC1354.2
017400     02  FILLER                      PIC X(22)  VALUE             NC1354.2
017500            " FOR OFFICIAL USE ONLY".                             NC1354.2
017600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1354.2
017700     02  FILLER                      PIC X(58)  VALUE             NC1354.2
017800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1354.2
017900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1354.2
018000     02 FILLER                       PIC X(15)  VALUE             NC1354.2
018100             " COPYRIGHT 1985".                                   NC1354.2
018200 01  CCVS-E-4.                                                    NC1354.2
018300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1354.2
018400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1354.2
018500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1354.2
018600     02 FILLER                       PIC X(40)  VALUE             NC1354.2
018700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1354.2
018800 01  XXINFO.                                                      NC1354.2
018900     02 FILLER                       PIC X(19)  VALUE             NC1354.2
019000            "*** INFORMATION ***".                                NC1354.2
019100     02 INFO-TEXT.                                                NC1354.2
019200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1354.2
019300       04 XXCOMPUTED                 PIC X(20).                   NC1354.2
019400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1354.2
019500       04 XXCORRECT                  PIC X(20).                   NC1354.2
019600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1354.2
019700 01  HYPHEN-LINE.                                                 NC1354.2
019800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1354.2
019900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1354.2
020000-    "*****************************************".                 NC1354.2
020100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1354.2
020200-    "******************************".                            NC1354.2
020300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1354.2
020400     "NC135A".                                                    NC1354.2
020500 PROCEDURE DIVISION.                                              NC1354.2
020600 CCVS1 SECTION.                                                   NC1354.2
020700 OPEN-FILES.                                                      NC1354.2
020800     OPEN     OUTPUT PRINT-FILE.                                  NC1354.2
020900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1354.2
021000     MOVE    SPACE TO TEST-RESULTS.                               NC1354.2
021100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1354.2
021200     GO TO CCVS1-EXIT.                                            NC1354.2
021300 CLOSE-FILES.                                                     NC1354.2
021400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1354.2
021500 TERMINATE-CCVS.                                                  NC1354.2
021600*    EXIT PROGRAM.                                                NC1354.2
021700*TERMINATE-CALL.                                                  NC1354.2
021800     STOP     RUN.                                                NC1354.2
021900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1354.2
022000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1354.2
022100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1354.2
022200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1354.2
022300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1354.2
022400 PRINT-DETAIL.                                                    NC1354.2
022500     IF REC-CT NOT EQUAL TO ZERO                                  NC1354.2
022600             MOVE "." TO PARDOT-X                                 NC1354.2
022700             MOVE REC-CT TO DOTVALUE.                             NC1354.2
022800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1354.2
022900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1354.2
023000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1354.2
023100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1354.2
023200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1354.2
023300     MOVE SPACE TO CORRECT-X.                                     NC1354.2
023400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1354.2
023500     MOVE     SPACE TO RE-MARK.                                   NC1354.2
023600 HEAD-ROUTINE.                                                    NC1354.2
023700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1354.2
023800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1354.2
023900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1354.2
024000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1354.2
024100 COLUMN-NAMES-ROUTINE.                                            NC1354.2
024200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1354.2
024300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1354.2
024400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1354.2
024500 END-ROUTINE.                                                     NC1354.2
024600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1354.2
024700 END-RTN-EXIT.                                                    NC1354.2
024800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1354.2
024900 END-ROUTINE-1.                                                   NC1354.2
025000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1354.2
025100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1354.2
025200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1354.2
025300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1354.2
025400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1354.2
025500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1354.2
025600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1354.2
025700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1354.2
025800  END-ROUTINE-12.                                                 NC1354.2
025900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1354.2
026000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1354.2
026100         MOVE "NO " TO ERROR-TOTAL                                NC1354.2
026200         ELSE                                                     NC1354.2
026300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1354.2
026400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1354.2
026500     PERFORM WRITE-LINE.                                          NC1354.2
026600 END-ROUTINE-13.                                                  NC1354.2
026700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1354.2
026800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1354.2
026900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1354.2
027000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1354.2
027100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1354.2
027200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1354.2
027300          MOVE "NO " TO ERROR-TOTAL                               NC1354.2
027400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1354.2
027500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1354.2
027600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1354.2
027700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1354.2
027800 WRITE-LINE.                                                      NC1354.2
027900     ADD 1 TO RECORD-COUNT.                                       NC1354.2
028000*    IF RECORD-COUNT GREATER 42                                   NC1354.2
028100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1354.2
028200*        MOVE SPACE TO DUMMY-RECORD                               NC1354.2
028300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1354.2
028400*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1354.2
028500*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1354.2
028600*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1354.2
028700*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1354.2
028800*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1354.2
028900*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1354.2
029000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1354.2
029100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1354.2
029200*        MOVE ZERO TO RECORD-COUNT.                               NC1354.2
029300     PERFORM WRT-LN.                                              NC1354.2
029400 WRT-LN.                                                          NC1354.2
029500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1354.2
029600     MOVE SPACE TO DUMMY-RECORD.                                  NC1354.2
029700 BLANK-LINE-PRINT.                                                NC1354.2
029800     PERFORM WRT-LN.                                              NC1354.2
029900 FAIL-ROUTINE.                                                    NC1354.2
030000     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1354.2
030100            GO TO FAIL-ROUTINE-WRITE.                             NC1354.2
030200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1354.2
030300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1354.2
030400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1354.2
030500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1354.2
030600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1354.2
030700     GO TO  FAIL-ROUTINE-EX.                                      NC1354.2
030800 FAIL-ROUTINE-WRITE.                                              NC1354.2
030900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1354.2
031000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1354.2
031100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1354.2
031200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1354.2
031300 FAIL-ROUTINE-EX. EXIT.                                           NC1354.2
031400 BAIL-OUT.                                                        NC1354.2
031500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1354.2
031600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1354.2
031700 BAIL-OUT-WRITE.                                                  NC1354.2
031800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1354.2
031900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1354.2
032000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1354.2
032100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1354.2
032200 BAIL-OUT-EX. EXIT.                                               NC1354.2
032300 CCVS1-EXIT.                                                      NC1354.2
032400     EXIT.                                                        NC1354.2
032500 SECT-NC135A-001 SECTION.                                         NC1354.2
032600 PARAGRAPH-NAME-4.                                                NC1354.2
032700     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC1354.2
032800     SET INXEX1  INXEX2 INXEX3 TO 001.                            NC1354.2
032900     MOVE 001 TO W-3.                                             NC1354.2
033000 PARAGRAPH-NAME-5.                                                NC1354.2
033100     MOVE W-3 TO TABLE-1 (INXEX1  INXEX2  INXEX3).                NC1354.2
033200     ADD 01 TO W-3.                                               NC1354.2
033300     IF       INXEX3 EQUAL TO 3                                   NC1354.2
033400              GO TO PARAGRAPH-NAME-6.                             NC1354.2
033500     SET INXEX3 UP BY 1.                                          NC1354.2
033600     GO TO PARAGRAPH-NAME-5.                                      NC1354.2
033700 PARAGRAPH-NAME-6.                                                NC1354.2
033800     SET     INXEX3 TO 1                                          NC1354.2
033900     IF       INXEX2 EQUAL TO 10                                  NC1354.2
034000                  GO TO PARAGRAPH-NAME-7.                         NC1354.2
034100     SET INXEX2 UP BY 001.                                        NC1354.2
034200     GO TO PARAGRAPH-NAME-5.                                      NC1354.2
034300 PARAGRAPH-NAME-7.                                                NC1354.2
034400     SET     INXEX2 TO 1                                          NC1354.2
034500     IF       INXEX1 EQUAL TO 10                                  NC1354.2
034600               GO TO PARAGRAPH-NAME-8.                            NC1354.2
034700     SET INXEX1 UP BY 001.                                        NC1354.2
034800     GO TO PARAGRAPH-NAME-5.                                      NC1354.2
034900*    THIS SECTION GENERATES VALUES FOR A 10X10X3 TABLE            NC1354.2
035000*    AND THE TABLE IS USED IN THE TESTS IN THIS ROUTINE.          NC1354.2
035100 PARAGRAPH-NAME-8.                                                NC1354.2
035200     EXIT.                                                        NC1354.2
035300 SECTION-NAME-2 SECTION.                                          NC1354.2
035400 PARAGRAPH-NAME-9.                                                NC1354.2
035500*                                                                 NC1354.2
035600     MOVE SPACE TO COMPUTED-A  CORRECT-A.                         NC1354.2
035700     MOVE "INDEXING" TO FEATURE.                                  NC1354.2
035800     SET INXEX1 INXEX2 INXEX3 TO  01.                             NC1354.2
035900 INDEX-TEST-1.                                                    NC1354.2
036000     IF TABLE-1 (INXEX1  INXEX2  INXEX3) EQUAL TO  001            NC1354.2
036100     PERFORM PASS                                                 NC1354.2
036200     GO TO WRITE-1.                                               NC1354.2
036300     MOVE TABLE-1 (INXEX1  INXEX2  INXEX3) TO COMPUTED-A.         NC1354.2
036400     MOVE 001 TO CORRECT-A.                                       NC1354.2
036500     PERFORM FAIL.                                                NC1354.2
036600     GO TO WRITE-1.                                               NC1354.2
036700 DELETE-1.                                                        NC1354.2
036800     PERFORM DE-LETE.                                             NC1354.2
036900 WRITE-1.                                                         NC1354.2
037000     MOVE "INDEX-TEST-1" TO PAR-NAME.                             NC1354.2
037100     PERFORM PRINT-DETAIL.                                        NC1354.2
037200 INDEX-TEST-2.                                                    NC1354.2
037300     SET INXEX1 INXEX2 INXEX3 TO 01.                              NC1354.2
037400     IF TABLE-1 (INXEX1  INXEX2 + 1  INXEX3 + 1) EQUAL TO 005     NC1354.2
037500     PERFORM PASS                                                 NC1354.2
037600     GO TO WRITE-2.                                               NC1354.2
037700     MOVE TABLE-1 (INXEX1  INXEX2 + 1  INXEX3 + 1) TO             NC1354.2
037800         COMPUTED-A.                                              NC1354.2
037900     MOVE "005" TO CORRECT-A.                                     NC1354.2
038000     PERFORM FAIL.                                                NC1354.2
038100     GO TO WRITE-2.                                               NC1354.2
038200 DELETE-2.                                                        NC1354.2
038300     PERFORM DE-LETE.                                             NC1354.2
038400 WRITE-2.                                                         NC1354.2
038500     MOVE "INDEX-TEST-2" TO PAR-NAME.                             NC1354.2
038600     PERFORM PRINT-DETAIL.                                        NC1354.2
038700 INDEX-TEST-3.                                                    NC1354.2
038800     SET INXEX1 INXEX2 TO  10.                                    NC1354.2
038900     SET INXEX3 TO  03.                                           NC1354.2
039000     IF TABLE-1 (INXEX1  INXEX2  INXEX3 - 1) EQUAL TO 299         NC1354.2
039100     PERFORM PASS                                                 NC1354.2
039200     GO TO WRITE-3.                                               NC1354.2
039300     MOVE TABLE-1 (INXEX1  INXEX2  INXEX3 - 1) TO COMPUTED-A.     NC1354.2
039400     MOVE "299" TO CORRECT-A.                                     NC1354.2
039500     PERFORM FAIL.                                                NC1354.2
039600     GO TO WRITE-3.                                               NC1354.2
039700 DELETE-3.                                                        NC1354.2
039800     PERFORM DE-LETE.                                             NC1354.2
039900 WRITE-3.                                                         NC1354.2
040000     MOVE "INDEX-TEST-3" TO PAR-NAME.                             NC1354.2
040100     PERFORM PRINT-DETAIL.                                        NC1354.2
040200 INDEX-TEST-4.                                                    NC1354.2
040300     SET INXEX1 INXEX2 TO 10.                                     NC1354.2
040400     SET INXEX3 TO 03.                                            NC1354.2
040500     IF TABLE-1 (INXEX1 - 5  INXEX2 - 7  INXEX3) EQUAL TO 129     NC1354.2
040600     PERFORM PASS                                                 NC1354.2
040700     GO TO WRITE-4.                                               NC1354.2
040800     MOVE TABLE-1 (INXEX1 - 5  INXEX2 - 7  INXEX3) TO             NC1354.2
040900         COMPUTED-A.                                              NC1354.2
041000     MOVE "129" TO CORRECT-A.                                     NC1354.2
041100     PERFORM FAIL.                                                NC1354.2
041200     GO TO WRITE-4.                                               NC1354.2
041300 DELETE-4.                                                        NC1354.2
041400     PERFORM DE-LETE.                                             NC1354.2
041500 WRITE-4.                                                         NC1354.2
041600     MOVE "INDEX-TEST-4" TO PAR-NAME.                             NC1354.2
041700     PERFORM PRINT-DETAIL.                                        NC1354.2
041800 INDEX-TEST-5.                                                    NC1354.2
041900     SET INXEX1 TO 10.                                            NC1354.2
042000     SET KEY-1 TO INXEX1.                                         NC1354.2
042100     SET INXEX1 TO 05.                                            NC1354.2
042200     SET INXEX2 TO 10.                                            NC1354.2
042300     SET INXEX3 TO 03.                                            NC1354.2
042400     SET INXEX1 TO KEY-1.                                         NC1354.2
042500     IF TABLE-1  (INXEX1  INXEX2  INXEX3) EQUAL TO 300            NC1354.2
042600         PERFORM PASS                                             NC1354.2
042700         GO TO WRITE-5.                                           NC1354.2
042800     MOVE TABLE-1 (INXEX1  INXEX2  INXEX3) TO COMPUTED-A.         NC1354.2
042900     MOVE "300" TO CORRECT-A.                                     NC1354.2
043000     PERFORM FAIL.                                                NC1354.2
043100     GO TO WRITE-5.                                               NC1354.2
043200 DELETE-5.                                                        NC1354.2
043300     PERFORM DE-LETE.                                             NC1354.2
043400 WRITE-5.                                                         NC1354.2
043500     MOVE "INDEX-TEST-5" TO PAR-NAME.                             NC1354.2
043600     PERFORM PRINT-DETAIL.                                        NC1354.2
043700 INDEX-TEST-6.                                                    NC1354.2
043800     SET INXEX1 INXEX2 TO 10.                                     NC1354.2
043900     SET INXEX3 TO 03.                                            NC1354.2
044000     SET INXEX1 DOWN BY 01.                                       NC1354.2
044100     SET  INXEX2 DOWN BY  IDEN-1.                                 NC1354.2
044200*    NOTE IDEN-1 IS EQUAL TO 03.                                  NC1354.2
044300     SET INXEX3 DOWN BY 2.                                        NC1354.2
044400     IF TABLE-1  (INXEX1  INXEX2  INXEX3) EQUAL TO 259            NC1354.2
044500         PERFORM PASS                                             NC1354.2
044600         GO TO WRITE-6.                                           NC1354.2
044700     MOVE TABLE-1 (INXEX1  INXEX2  INXEX3) TO COMPUTED-A.         NC1354.2
044800     MOVE "259" TO CORRECT-A.                                     NC1354.2
044900     PERFORM FAIL.                                                NC1354.2
045000     GO TO WRITE-6.                                               NC1354.2
045100 DELETE-6.                                                        NC1354.2
045200     PERFORM DE-LETE.                                             NC1354.2
045300 WRITE-6.                                                         NC1354.2
045400     MOVE "INDEX-TEST-6" TO PAR-NAME.                             NC1354.2
045500     PERFORM PRINT-DETAIL.                                        NC1354.2
045600 INDEX-TEST-7.                                                    NC1354.2
045700     SET INXEX1 TO ONE.                                           NC1354.2
045800     SET INXEX2 INXEX3 TO INXEX1.                                 NC1354.2
045900     IF TABLE-1  (INXEX1  INXEX2  INXEX3) EQUAL TO 001            NC1354.2
046000         PERFORM PASS                                             NC1354.2
046100         GO TO WRITE-7.                                           NC1354.2
046200     MOVE TABLE-1 (INXEX1  INXEX2  INXEX3) TO COMPUTED-A.         NC1354.2
046300     MOVE "001" TO CORRECT-A.                                     NC1354.2
046400     PERFORM FAIL.                                                NC1354.2
046500     GO TO WRITE-7.                                               NC1354.2
046600 DELETE-7.                                                        NC1354.2
046700     PERFORM DE-LETE.                                             NC1354.2
046800 WRITE-7.                                                         NC1354.2
046900     MOVE "INDEX-TEST-7" TO PAR-NAME.                             NC1354.2
047000     PERFORM PRINT-DETAIL.                                        NC1354.2
047100 SECTION-NAME-3 SECTION.                                          NC1354.2
047200 PARAGRAPH-NAME-10.                                               NC1354.2
047300     PERFORM BLANK-LINE-PRINT 2 TIMES.                            NC1354.2
047400     PERFORM INSPT.                                               NC1354.2
047500     MOVE SPACES TO PRINT-REC.                                    NC1354.2
047600     WRITE PRINT-REC.                                             NC1354.2
047700     MOVE "NOTE THIS SECTION WRITES A 20X15 TABLE OF THREE-DIGIT  NC1354.2
047800-       "NUMBERS 001 TO 300 --- THERE SHOULD BE TWO" TO PRINT-REC.NC1354.2
047900     WRITE PRINT-REC.                                             NC1354.2
048000     MOVE SPACES TO PRINT-REC.                                    NC1354.2
048100     MOVE "SPACES BETWEEN EACH NUMBER ON A LINE --- THE NUMBERS   NC1354.2
048200-      "001 THRU 020 SHOULD FORM THE FIRST LINE ---" TO PRINT-REC.NC1354.2
048300     WRITE PRINT-REC.                                             NC1354.2
048400     MOVE SPACES TO PRINT-REC.                                    NC1354.2
048500     MOVE "THE VALUES IN THIS TABLE WERE GENERATED IN             NC1354.2
048600-       "SECTION-NAME-1 SECTION." TO PRINT-REC.                   NC1354.2
048700     WRITE PRINT-REC.                                             NC1354.2
048800     MOVE SPACES TO PRINT-REC.                                    NC1354.2
048900     SET INXEX1  INXEX2 INXEX3 TO ONE.                            NC1354.2
049000     MOVE 01 TO CTR-1.                                            NC1354.2
049100 PARAGRAPH-NAME-11.                                               NC1354.2
049200     MOVE TABLE-1 (INXEX1  INXEX2  INXEX3)  TO                    NC1354.2
049300     PRINT-ELE (CTR-1)                                            NC1354.2
049400     MOVE SPACE TO BLANKSPACE (CTR-1).                            NC1354.2
049500     ADD 1 TO CTR-1                                               NC1354.2
049600     IF CTR-1 EQUAL TO 21 PERFORM PARAGRAPH-NAME-15.              NC1354.2
049700     IF       INXEX3 EQUAL TO 3 GO TO PARAGRAPH-NAME-12.          NC1354.2
049800     SET INXEX3 UP BY   1.                                        NC1354.2
049900     GO TO    PARAGRAPH-NAME-11.                                  NC1354.2
050000 PARAGRAPH-NAME-12.                                               NC1354.2
050100     SET INXEX3 TO 1.                                             NC1354.2
050200     IF       INXEX2 EQUAL TO 10 GO TO PARAGRAPH-NAME-13.         NC1354.2
050300     SET INXEX2 UP BY 1.                                          NC1354.2
050400     GO TO    PARAGRAPH-NAME-11.                                  NC1354.2
050500 PARAGRAPH-NAME-13.                                               NC1354.2
050600     SET INXEX2 TO 1.                                             NC1354.2
050700     IF       INXEX1 EQUAL TO 10 GO TO PARAGRAPH-NAME-14.         NC1354.2
050800     SET INXEX1 UP BY 1.                                          NC1354.2
050900     GO TO    PARAGRAPH-NAME-11.                                  NC1354.2
051000 PARAGRAPH-NAME-14.                                               NC1354.2
051100     GO TO PARAGRAPH-NAME-16.                                     NC1354.2
051200 PARAGRAPH-NAME-15.                                               NC1354.2
051300     MOVE SPACE TO PRINT-REC.                                     NC1354.2
051400     MOVE NUMBER-LIST TO PRINT-REC.                               NC1354.2
051500     WRITE PRINT-REC AFTER 1.                                     NC1354.2
051600     MOVE 01 TO CTR-1.                                            NC1354.2
051700 PARAGRAPH-NAME-16.                                               NC1354.2
051800     EXIT.                                                        NC1354.2
051900 CCVS-EXIT SECTION.                                               NC1354.2
052000 CCVS-999999.                                                     NC1354.2
052100     GO TO CLOSE-FILES.                                           NC1354.2
      *END-OF,NC135A                                                                  
