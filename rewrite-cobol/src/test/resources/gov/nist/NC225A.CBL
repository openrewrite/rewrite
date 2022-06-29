      *HEADER,COBOL,NC225A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2254.2
000200 PROGRAM-ID.                                                      NC2254.2
000300     NC225A.                                                      NC2254.2
000400****************************************************************  NC2254.2
000500*                                                              *  NC2254.2
000600*    VALIDATION FOR:-                                          *  NC2254.2
000700*                                                              *  NC2254.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2254.2
000900*                                                              *  NC2254.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2254.2
001100*                                                              *  NC2254.2
001200****************************************************************  NC2254.2
001300*                                                              *  NC2254.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2254.2
001500*                                                              *  NC2254.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2254.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2254.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2254.2
001900*                                                              *  NC2254.2
002000****************************************************************  NC2254.2
002100*                                                              *  NC2254.2
002200*    PROGRAM NC225A TESTS THE USE OF THE "EVALUATE" STATEMENT. *  NC2254.2
002300*    VARIOUS COMBINATIONS OF IDENTIFIERS, LITERALS, ARITHMETIC *  NC2254.2
002400*    EXPRESSIONS AND CONDITIONAL EXPRESSIONS AS WELL AS THE    *  NC2254.2
002500*    OPTIONAL WORDS "TRUE" AND "FALSE" ARE USED AS SELECTION   *  NC2254.2
002600*    SUBJECTS AND SELECTION OBJECTS.                           *  NC2254.2
002700*    MULTIPLE SELECTION SUBJECTS AND SETS OF SELECTION         *  NC2254.2
002800*    OBJECTS ARE ALSO TESTED.                                  *  NC2254.2
002900*                                                              *  NC2254.2
003000****************************************************************  NC2254.2
003100 ENVIRONMENT DIVISION.                                            NC2254.2
003200 CONFIGURATION SECTION.                                           NC2254.2
003300 SOURCE-COMPUTER.                                                 NC2254.2
003400     XXXXX082.                                                    NC2254.2
003500 OBJECT-COMPUTER.                                                 NC2254.2
003600     XXXXX083.                                                    NC2254.2
003700 INPUT-OUTPUT SECTION.                                            NC2254.2
003800 FILE-CONTROL.                                                    NC2254.2
003900     SELECT PRINT-FILE ASSIGN TO                                  NC2254.2
004000     XXXXX055.                                                    NC2254.2
004100 DATA DIVISION.                                                   NC2254.2
004200 FILE SECTION.                                                    NC2254.2
004300 FD  PRINT-FILE.                                                  NC2254.2
004400 01  PRINT-REC PICTURE X(120).                                    NC2254.2
004500 01  DUMMY-RECORD PICTURE X(120).                                 NC2254.2
004600 WORKING-STORAGE SECTION.                                         NC2254.2
004700 01  WRK-XN-00001-1              PIC   X.                         NC2254.2
004800 01  WRK-XN-00001-2              PIC   X.                         NC2254.2
004900 01  WRK-XN-00001-3              PIC   X.                         NC2254.2
005000 01  WRK-XN-00001-4              PIC   X.                         NC2254.2
005100 01  WRK-DU-02V00                PIC   99.                        NC2254.2
005200 01  WRK-DU-08V00                PIC   9(8).                      NC2254.2
005300 01  WRK-DU-08V00-1              PIC   9(8).                      NC2254.2
005400 01  WRK-DU-08V00-2              PIC   9(8).                      NC2254.2
005500         88 IT-IS-81         VALUE 81.                            NC2254.2
005600 01  WRK-DU-08V00-3              PIC   9(8).                      NC2254.2
005700 01  WRK-DU-08V00-4              PIC   9(8).                      NC2254.2
005800 01  TEST-3-DATA                 PIC   X(6)     VALUE "ABCDEF".   NC2254.2
005900 01  TEST-4-DATA                 PIC   XXBXXBXX VALUE "AB CD EF". NC2254.2
006000                                                                  NC2254.2
006100 01  WS-2                        PIC  S9        VALUE +2.         NC2254.2
006200 01  WS-3                        PIC  S9        VALUE +3.         NC2254.2
006300 01  WS-5                        PIC  S9        VALUE +5.         NC2254.2
006400 01  WS-6                        PIC  S9        VALUE +6.         NC2254.2
006500 01  WS-7                        PIC  S9        VALUE +7.         NC2254.2
006600 01  WS-10                       PIC  S99       VALUE +10.        NC2254.2
006700 01  WS-81                       PIC  S99       VALUE +81.        NC2254.2
006800 01  TEST-5-TABLE.                                                NC2254.2
006900   03        TABLE-GROUP         OCCURS 4.                        NC2254.2
007000     05      TABLE-1             PIC 9(8)                         NC2254.2
007100                                 OCCURS 2.                        NC2254.2
007200   03         TEST-5-DATA        PIC X(6).                        NC2254.2
007300*                                                                 NC2254.2
007400 01  TEST-RESULTS.                                                NC2254.2
007500     02 FILLER                   PIC X      VALUE SPACE.          NC2254.2
007600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2254.2
007700     02 FILLER                   PIC X      VALUE SPACE.          NC2254.2
007800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2254.2
007900     02 FILLER                   PIC X      VALUE SPACE.          NC2254.2
008000     02  PAR-NAME.                                                NC2254.2
008100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2254.2
008200       03  PARDOT-X              PIC X      VALUE SPACE.          NC2254.2
008300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2254.2
008400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2254.2
008500     02 RE-MARK                  PIC X(61).                       NC2254.2
008600 01  TEST-COMPUTED.                                               NC2254.2
008700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2254.2
008800     02 FILLER                   PIC X(17)  VALUE                 NC2254.2
008900            "       COMPUTED=".                                   NC2254.2
009000     02 COMPUTED-X.                                               NC2254.2
009100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2254.2
009200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2254.2
009300                                 PIC -9(9).9(9).                  NC2254.2
009400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2254.2
009500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2254.2
009600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2254.2
009700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2254.2
009800         04 COMPUTED-18V0                    PIC -9(18).          NC2254.2
009900         04 FILLER                           PIC X.               NC2254.2
010000     03 FILLER PIC X(50) VALUE SPACE.                             NC2254.2
010100 01  TEST-CORRECT.                                                NC2254.2
010200     02 FILLER PIC X(30) VALUE SPACE.                             NC2254.2
010300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2254.2
010400     02 CORRECT-X.                                                NC2254.2
010500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2254.2
010600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2254.2
010700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2254.2
010800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2254.2
010900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2254.2
011000     03      CR-18V0 REDEFINES CORRECT-A.                         NC2254.2
011100         04 CORRECT-18V0                     PIC -9(18).          NC2254.2
011200         04 FILLER                           PIC X.               NC2254.2
011300     03 FILLER PIC X(2) VALUE SPACE.                              NC2254.2
011400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2254.2
011500 01  CCVS-C-1.                                                    NC2254.2
011600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2254.2
011700-    "SS  PARAGRAPH-NAME                                          NC2254.2
011800-    "       REMARKS".                                            NC2254.2
011900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2254.2
012000 01  CCVS-C-2.                                                    NC2254.2
012100     02 FILLER                     PIC X        VALUE SPACE.      NC2254.2
012200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2254.2
012300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2254.2
012400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2254.2
012500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2254.2
012600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2254.2
012700 01  REC-CT                        PIC 99       VALUE ZERO.       NC2254.2
012800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2254.2
012900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2254.2
013000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2254.2
013100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2254.2
013200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2254.2
013300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2254.2
013400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2254.2
013500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2254.2
013600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2254.2
013700 01  CCVS-H-1.                                                    NC2254.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2254.2
013900     02  FILLER                    PIC X(42)    VALUE             NC2254.2
014000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2254.2
014100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2254.2
014200 01  CCVS-H-2A.                                                   NC2254.2
014300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2254.2
014400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2254.2
014500   02  FILLER                        PIC XXXX   VALUE             NC2254.2
014600     "4.2 ".                                                      NC2254.2
014700   02  FILLER                        PIC X(28)  VALUE             NC2254.2
014800            " COPY - NOT FOR DISTRIBUTION".                       NC2254.2
014900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2254.2
015000                                                                  NC2254.2
015100 01  CCVS-H-2B.                                                   NC2254.2
015200   02  FILLER                        PIC X(15)  VALUE             NC2254.2
015300            "TEST RESULT OF ".                                    NC2254.2
015400   02  TEST-ID                       PIC X(9).                    NC2254.2
015500   02  FILLER                        PIC X(4)   VALUE             NC2254.2
015600            " IN ".                                               NC2254.2
015700   02  FILLER                        PIC X(12)  VALUE             NC2254.2
015800     " HIGH       ".                                              NC2254.2
015900   02  FILLER                        PIC X(22)  VALUE             NC2254.2
016000            " LEVEL VALIDATION FOR ".                             NC2254.2
016100   02  FILLER                        PIC X(58)  VALUE             NC2254.2
016200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2254.2
016300 01  CCVS-H-3.                                                    NC2254.2
016400     02  FILLER                      PIC X(34)  VALUE             NC2254.2
016500            " FOR OFFICIAL USE ONLY    ".                         NC2254.2
016600     02  FILLER                      PIC X(58)  VALUE             NC2254.2
016700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2254.2
016800     02  FILLER                      PIC X(28)  VALUE             NC2254.2
016900            "  COPYRIGHT   1985 ".                                NC2254.2
017000 01  CCVS-E-1.                                                    NC2254.2
017100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2254.2
017200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2254.2
017300     02 ID-AGAIN                     PIC X(9).                    NC2254.2
017400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2254.2
017500 01  CCVS-E-2.                                                    NC2254.2
017600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2254.2
017700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2254.2
017800     02 CCVS-E-2-2.                                               NC2254.2
017900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2254.2
018000         03 FILLER                   PIC X      VALUE SPACE.      NC2254.2
018100         03 ENDER-DESC               PIC X(44)  VALUE             NC2254.2
018200            "ERRORS ENCOUNTERED".                                 NC2254.2
018300 01  CCVS-E-3.                                                    NC2254.2
018400     02  FILLER                      PIC X(22)  VALUE             NC2254.2
018500            " FOR OFFICIAL USE ONLY".                             NC2254.2
018600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2254.2
018700     02  FILLER                      PIC X(58)  VALUE             NC2254.2
018800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2254.2
018900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2254.2
019000     02 FILLER                       PIC X(15)  VALUE             NC2254.2
019100             " COPYRIGHT 1985".                                   NC2254.2
019200 01  CCVS-E-4.                                                    NC2254.2
019300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2254.2
019400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2254.2
019500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2254.2
019600     02 FILLER                       PIC X(40)  VALUE             NC2254.2
019700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2254.2
019800 01  XXINFO.                                                      NC2254.2
019900     02 FILLER                       PIC X(19)  VALUE             NC2254.2
020000            "*** INFORMATION ***".                                NC2254.2
020100     02 INFO-TEXT.                                                NC2254.2
020200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2254.2
020300       04 XXCOMPUTED                 PIC X(20).                   NC2254.2
020400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2254.2
020500       04 XXCORRECT                  PIC X(20).                   NC2254.2
020600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2254.2
020700 01  HYPHEN-LINE.                                                 NC2254.2
020800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2254.2
020900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2254.2
021000-    "*****************************************".                 NC2254.2
021100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2254.2
021200-    "******************************".                            NC2254.2
021300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2254.2
021400     "NC225A".                                                    NC2254.2
021500 PROCEDURE DIVISION.                                              NC2254.2
021600 CCVS1 SECTION.                                                   NC2254.2
021700 OPEN-FILES.                                                      NC2254.2
021800     OPEN     OUTPUT PRINT-FILE.                                  NC2254.2
021900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2254.2
022000     MOVE    SPACE TO TEST-RESULTS.                               NC2254.2
022100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2254.2
022200     GO TO CCVS1-EXIT.                                            NC2254.2
022300 CLOSE-FILES.                                                     NC2254.2
022400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2254.2
022500 TERMINATE-CCVS.                                                  NC2254.2
022600*    EXIT PROGRAM.                                                NC2254.2
022700*TERMINATE-CALL.                                                  NC2254.2
022800     STOP     RUN.                                                NC2254.2
022900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2254.2
023000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2254.2
023100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2254.2
023200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2254.2
023300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2254.2
023400 PRINT-DETAIL.                                                    NC2254.2
023500     IF REC-CT NOT EQUAL TO ZERO                                  NC2254.2
023600             MOVE "." TO PARDOT-X                                 NC2254.2
023700             MOVE REC-CT TO DOTVALUE.                             NC2254.2
023800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2254.2
023900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2254.2
024000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2254.2
024100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2254.2
024200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2254.2
024300     MOVE SPACE TO CORRECT-X.                                     NC2254.2
024400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2254.2
024500     MOVE     SPACE TO RE-MARK.                                   NC2254.2
024600 HEAD-ROUTINE.                                                    NC2254.2
024700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2254.2
024800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2254.2
024900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2254.2
025000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2254.2
025100 COLUMN-NAMES-ROUTINE.                                            NC2254.2
025200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2254.2
025300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2254.2
025400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2254.2
025500 END-ROUTINE.                                                     NC2254.2
025600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2254.2
025700 END-RTN-EXIT.                                                    NC2254.2
025800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2254.2
025900 END-ROUTINE-1.                                                   NC2254.2
026000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2254.2
026100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2254.2
026200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2254.2
026300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2254.2
026400                                                                  NC2254.2
026500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2254.2
026600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2254.2
026700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2254.2
026800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2254.2
026900  END-ROUTINE-12.                                                 NC2254.2
027000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2254.2
027100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2254.2
027200         MOVE "NO " TO ERROR-TOTAL                                NC2254.2
027300         ELSE                                                     NC2254.2
027400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2254.2
027500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2254.2
027600     PERFORM WRITE-LINE.                                          NC2254.2
027700 END-ROUTINE-13.                                                  NC2254.2
027800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2254.2
027900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2254.2
028000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2254.2
028100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2254.2
028200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2254.2
028300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2254.2
028400          MOVE "NO " TO ERROR-TOTAL                               NC2254.2
028500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2254.2
028600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2254.2
028700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2254.2
028800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2254.2
028900 WRITE-LINE.                                                      NC2254.2
029000     ADD 1 TO RECORD-COUNT.                                       NC2254.2
029100*    IF RECORD-COUNT GREATER 50                                   NC2254.2
029200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2254.2
029300*        MOVE SPACE TO DUMMY-RECORD                               NC2254.2
029400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2254.2
029500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2254.2
029600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2254.2
029700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2254.2
029800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2254.2
029900*        MOVE ZERO TO RECORD-COUNT.                               NC2254.2
030000     PERFORM WRT-LN.                                              NC2254.2
030100 WRT-LN.                                                          NC2254.2
030200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2254.2
030300     MOVE SPACE TO DUMMY-RECORD.                                  NC2254.2
030400 BLANK-LINE-PRINT.                                                NC2254.2
030500     PERFORM WRT-LN.                                              NC2254.2
030600 FAIL-ROUTINE.                                                    NC2254.2
030700     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC2254.2
030800            GO TO    FAIL-ROUTINE-WRITE.                          NC2254.2
030900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2254.2
031000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2254.2
031100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2254.2
031200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2254.2
031300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2254.2
031400     GO TO  FAIL-ROUTINE-EX.                                      NC2254.2
031500 FAIL-ROUTINE-WRITE.                                              NC2254.2
031600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2254.2
031700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2254.2
031800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2254.2
031900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2254.2
032000 FAIL-ROUTINE-EX. EXIT.                                           NC2254.2
032100 BAIL-OUT.                                                        NC2254.2
032200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2254.2
032300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2254.2
032400 BAIL-OUT-WRITE.                                                  NC2254.2
032500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2254.2
032600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2254.2
032700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2254.2
032800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2254.2
032900 BAIL-OUT-EX. EXIT.                                               NC2254.2
033000 CCVS1-EXIT.                                                      NC2254.2
033100     EXIT.                                                        NC2254.2
033200*                                                                 NC2254.2
033300 SECT-NC225A-001-1 SECTION.                                       NC2254.2
033400*                                                                 NC2254.2
033500 EVA-INIT-GF-1.                                                   NC2254.2
033600     MOVE   "EVALUATE STATEMENT"  TO FEATURE.                     NC2254.2
033700     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
033800     MOVE    1       TO REC-CT.                                   NC2254.2
033900     MOVE   "6" TO WRK-XN-00001-1.                                NC2254.2
034000     GO TO   EVA-TEST-GF-1-1.                                     NC2254.2
034100 EVA-DELETE-GF-1.                                                 NC2254.2
034200     PERFORM DE-LETE.                                             NC2254.2
034300     PERFORM PRINT-DETAIL.                                        NC2254.2
034400     GO TO   EVA-INIT-GF-2.                                       NC2254.2
034500 EVA-TEST-GF-1-1.                                                 NC2254.2
034600     MOVE   "EVA-TEST-GF-1-1" TO PAR-NAME.                        NC2254.2
034700     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
034800        WHEN TRUE                                                 NC2254.2
034900             PERFORM PASS                                         NC2254.2
035000             GO TO   EVA-WRITE-GF-1-1.                            NC2254.2
035100     GO TO EVA-FAIL-GF-1-1.                                       NC2254.2
035200 EVA-DELETE-GF-1-1.                                               NC2254.2
035300     PERFORM DE-LETE.                                             NC2254.2
035400     GO TO EVA-WRITE-GF-1-1.                                      NC2254.2
035500 EVA-FAIL-GF-1-1.                                                 NC2254.2
035600     MOVE   "EXPECTING NUMERIC CONDITION" TO RE-MARK.             NC2254.2
035700     PERFORM FAIL.                                                NC2254.2
035800 EVA-WRITE-GF-1-1.                                                NC2254.2
035900     PERFORM PRINT-DETAIL.                                        NC2254.2
036000*                                                                 NC2254.2
036100 EVA-TEST-GF-1-2.                                                 NC2254.2
036200     ADD     1       TO REC-CT.                                   NC2254.2
036300     MOVE   "EVA-TEST-GF-1-2" TO PAR-NAME.                        NC2254.2
036400     EVALUATE WRK-XN-00001-1 NOT NUMERIC                          NC2254.2
036500        WHEN TRUE                                                 NC2254.2
036600             GO TO EVA-FAIL-GF-1-2.                               NC2254.2
036700     PERFORM PASS.                                                NC2254.2
036800     GO TO   EVA-WRITE-GF-1-2.                                    NC2254.2
036900 EVA-DELETE-GF-1-2.                                               NC2254.2
037000     PERFORM DE-LETE.                                             NC2254.2
037100     GO TO EVA-WRITE-GF-1-2.                                      NC2254.2
037200 EVA-FAIL-GF-1-2.                                                 NC2254.2
037300     MOVE   "EXPECTING NUMERIC CONDITION" TO RE-MARK              NC2254.2
037400     PERFORM FAIL.                                                NC2254.2
037500 EVA-WRITE-GF-1-2.                                                NC2254.2
037600     PERFORM PRINT-DETAIL.                                        NC2254.2
037700*                                                                 NC2254.2
037800 EVA-INIT-GF-2.                                                   NC2254.2
037900     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
038000     MOVE    1  TO REC-CT.                                        NC2254.2
038100     MOVE   "*" TO WRK-XN-00001-1.                                NC2254.2
038200     GO TO   EVA-TEST-GF-2-1.                                     NC2254.2
038300 EVA-DELETE-GF-2.                                                 NC2254.2
038400     PERFORM DE-LETE.                                             NC2254.2
038500     PERFORM PRINT-DETAIL.                                        NC2254.2
038600     GO TO   EVA-INIT-GF-3.                                       NC2254.2
038700 EVA-TEST-GF-2-1.                                                 NC2254.2
038800     MOVE   "EVA-TEST-GF-2-1" TO PAR-NAME.                        NC2254.2
038900     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
039000        WHEN TRUE                                                 NC2254.2
039100             GO TO EVA-FAIL-GF-2-1.                               NC2254.2
039200     PERFORM PASS.                                                NC2254.2
039300     GO TO EVA-WRITE-GF-2-1.                                      NC2254.2
039400 EVA-DELETE-GF-2-1.                                               NC2254.2
039500     PERFORM DE-LETE.                                             NC2254.2
039600     GO TO EVA-WRITE-GF-2-1.                                      NC2254.2
039700 EVA-FAIL-GF-2-1.                                                 NC2254.2
039800     MOVE   "EXPECTING NON-NUMERIC CONDITION" TO RE-MARK          NC2254.2
039900     PERFORM FAIL.                                                NC2254.2
040000 EVA-WRITE-GF-2-1.                                                NC2254.2
040100     PERFORM PRINT-DETAIL.                                        NC2254.2
040200*                                                                 NC2254.2
040300 EVA-TEST-GF-2-2.                                                 NC2254.2
040400     ADD     1 TO REC-CT.                                         NC2254.2
040500     MOVE   "EVA-TEST-GF-2-2" TO PAR-NAME.                        NC2254.2
040600     EVALUATE WRK-XN-00001-1 NOT NUMERIC                          NC2254.2
040700        WHEN TRUE                                                 NC2254.2
040800             PERFORM PASS                                         NC2254.2
040900             GO TO   EVA-WRITE-GF-2-2.                            NC2254.2
041000     GO TO EVA-FAIL-GF-2-2.                                       NC2254.2
041100 EVA-DELETE-GF-2-2.                                               NC2254.2
041200     PERFORM DE-LETE.                                             NC2254.2
041300     GO TO EVA-WRITE-GF-2-2.                                      NC2254.2
041400 EVA-FAIL-GF-2-2.                                                 NC2254.2
041500     MOVE   "EXPECTING NON-NUMERIC CONDITION" TO RE-MARK.         NC2254.2
041600     PERFORM FAIL.                                                NC2254.2
041700 EVA-WRITE-GF-2-2.                                                NC2254.2
041800     PERFORM PRINT-DETAIL.                                        NC2254.2
041900*                                                                 NC2254.2
042000 EVA-INIT-GF-3.                                                   NC2254.2
042100     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
042200     MOVE    1  TO REC-CT.                                        NC2254.2
042300     MOVE   "6" TO WRK-XN-00001-1.                                NC2254.2
042400     MOVE   "6" TO WRK-XN-00001-2.                                NC2254.2
042500     GO TO   EVA-TEST-GF-3-1.                                     NC2254.2
042600 EVA-DELETE-GF-3.                                                 NC2254.2
042700     PERFORM DE-LETE.                                             NC2254.2
042800     PERFORM PRINT-DETAIL.                                        NC2254.2
042900     GO TO   EVA-INIT-GF-4.                                       NC2254.2
043000 EVA-TEST-GF-3-1.                                                 NC2254.2
043100     MOVE   "EVA-TEST-GF-3-1" TO PAR-NAME.                        NC2254.2
043200     EVALUATE WRK-XN-00001-1                                      NC2254.2
043300        WHEN WRK-XN-00001-2                                       NC2254.2
043400             PERFORM PASS                                         NC2254.2
043500             GO TO   EVA-WRITE-GF-3-1.                            NC2254.2
043600     GO TO EVA-FAIL-GF-3-1.                                       NC2254.2
043700 EVA-DELETE-GF-3-1.                                               NC2254.2
043800     PERFORM DE-LETE.                                             NC2254.2
043900     GO TO EVA-WRITE-GF-3-1.                                      NC2254.2
044000 EVA-FAIL-GF-3-1.                                                 NC2254.2
044100     MOVE   "EXPECTING EQUAL IDENTIFIER" TO RE-MARK.              NC2254.2
044200     PERFORM FAIL.                                                NC2254.2
044300 EVA-WRITE-GF-3-1.                                                NC2254.2
044400     PERFORM PRINT-DETAIL.                                        NC2254.2
044500*                                                                 NC2254.2
044600 EVA-TEST-GF-3-2.                                                 NC2254.2
044700     ADD     1       TO REC-CT.                                   NC2254.2
044800     MOVE   "EVA-TEST-GF-3-2" TO PAR-NAME.                        NC2254.2
044900     EVALUATE WRK-XN-00001-1                                      NC2254.2
045000        WHEN NOT WRK-XN-00001-2                                   NC2254.2
045100             GO TO EVA-FAIL-GF-3-2.                               NC2254.2
045200     PERFORM PASS.                                                NC2254.2
045300     GO TO   EVA-WRITE-GF-3-2.                                    NC2254.2
045400 EVA-DELETE-GF-3-2.                                               NC2254.2
045500     PERFORM DE-LETE.                                             NC2254.2
045600     GO TO EVA-WRITE-GF-3-2.                                      NC2254.2
045700 EVA-FAIL-GF-3-2.                                                 NC2254.2
045800     MOVE   "EXPECTING EQUAL IDENTIFIERS" TO RE-MARK              NC2254.2
045900     PERFORM FAIL.                                                NC2254.2
046000 EVA-WRITE-GF-3-2.                                                NC2254.2
046100     PERFORM PRINT-DETAIL.                                        NC2254.2
046200*                                                                 NC2254.2
046300 EVA-INIT-GF-4.                                                   NC2254.2
046400     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
046500     MOVE    1  TO REC-CT.                                        NC2254.2
046600     MOVE   "1" TO WRK-XN-00001-1.                                NC2254.2
046700     MOVE   "*" TO WRK-XN-00001-2.                                NC2254.2
046800     GO TO   EVA-TEST-GF-4-1.                                     NC2254.2
046900 EVA-DELETE-GF-4.                                                 NC2254.2
047000     PERFORM DE-LETE.                                             NC2254.2
047100     PERFORM PRINT-DETAIL.                                        NC2254.2
047200     GO TO   EVA-INIT-GF-5.                                       NC2254.2
047300 EVA-TEST-GF-4-1.                                                 NC2254.2
047400     MOVE   "EVA-TEST-GF-4-1" TO PAR-NAME.                        NC2254.2
047500     EVALUATE WRK-XN-00001-1                                      NC2254.2
047600        WHEN WRK-XN-00001-2                                       NC2254.2
047700             GO TO EVA-FAIL-GF-4-1.                               NC2254.2
047800     PERFORM PASS.                                                NC2254.2
047900     GO TO   EVA-WRITE-GF-4-1.                                    NC2254.2
048000 EVA-DELETE-GF-4-1.                                               NC2254.2
048100     PERFORM DE-LETE.                                             NC2254.2
048200     GO TO EVA-WRITE-GF-4-1.                                      NC2254.2
048300 EVA-FAIL-GF-4-1.                                                 NC2254.2
048400     MOVE   "EXPECTING UNEQUAL IDENTIFIERS" TO RE-MARK            NC2254.2
048500     PERFORM FAIL.                                                NC2254.2
048600 EVA-WRITE-GF-4-1.                                                NC2254.2
048700     PERFORM PRINT-DETAIL.                                        NC2254.2
048800*                                                                 NC2254.2
048900 EVA-TEST-GF-4-2.                                                 NC2254.2
049000     ADD     1 TO REC-CT.                                         NC2254.2
049100     EVALUATE WRK-XN-00001-1                                      NC2254.2
049200        WHEN NOT WRK-XN-00001-2                                   NC2254.2
049300             PERFORM PASS                                         NC2254.2
049400             GO TO   EVA-WRITE-GF-4-2.                            NC2254.2
049500     GO TO EVA-FAIL-GF-4-2.                                       NC2254.2
049600 EVA-DELETE-GF-4-2.                                               NC2254.2
049700     PERFORM DE-LETE.                                             NC2254.2
049800     GO TO   EVA-WRITE-GF-4-2.                                    NC2254.2
049900 EVA-FAIL-GF-4-2.                                                 NC2254.2
050000     MOVE   "EXPECTING UNEQUAL IDENTIFIERS" TO RE-MARK.           NC2254.2
050100     PERFORM FAIL.                                                NC2254.2
050200 EVA-WRITE-GF-4-2.                                                NC2254.2
050300     MOVE   "EVA-TEST-GF-4-2" TO PAR-NAME.                        NC2254.2
050400     PERFORM PRINT-DETAIL.                                        NC2254.2
050500*                                                                 NC2254.2
050600 EVA-INIT-GF-5.                                                   NC2254.2
050700     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
050800     MOVE    1  TO REC-CT.                                        NC2254.2
050900     MOVE   "A" TO WRK-XN-00001-1.                                NC2254.2
051000     GO TO   EVA-TEST-GF-5-1.                                     NC2254.2
051100 EVA-DELETE-GF-5.                                                 NC2254.2
051200     PERFORM DE-LETE.                                             NC2254.2
051300     PERFORM PRINT-DETAIL.                                        NC2254.2
051400     GO TO   EVA-INIT-GF-6.                                       NC2254.2
051500 EVA-TEST-GF-5-1.                                                 NC2254.2
051600     MOVE   "EVA-TEST-GF-5-1" TO PAR-NAME.                        NC2254.2
051700     EVALUATE WRK-XN-00001-1                                      NC2254.2
051800        WHEN "A"                                                  NC2254.2
051900             PERFORM PASS                                         NC2254.2
052000             GO TO   EVA-WRITE-GF-5-1.                            NC2254.2
052100     GO TO EVA-FAIL-GF-5-1.                                       NC2254.2
052200 EVA-DELETE-GF-5-1.                                               NC2254.2
052300     PERFORM DE-LETE.                                             NC2254.2
052400     GO TO EVA-WRITE-GF-5-1.                                      NC2254.2
052500 EVA-FAIL-GF-5-1.                                                 NC2254.2
052600     MOVE   "EXPECTING EQUAL LITERAL" TO RE-MARK.                 NC2254.2
052700     PERFORM FAIL.                                                NC2254.2
052800 EVA-WRITE-GF-5-1.                                                NC2254.2
052900     PERFORM PRINT-DETAIL.                                        NC2254.2
053000*                                                                 NC2254.2
053100 EVA-TEST-GF-5-2.                                                 NC2254.2
053200     ADD     1       TO REC-CT.                                   NC2254.2
053300     MOVE   "EVA-TEST-GF-5-2" TO PAR-NAME.                        NC2254.2
053400     EVALUATE WRK-XN-00001-1                                      NC2254.2
053500        WHEN NOT "A"                                              NC2254.2
053600             GO TO EVA-FAIL-GF-5-2.                               NC2254.2
053700     PERFORM PASS.                                                NC2254.2
053800     GO TO   EVA-WRITE-GF-5-2.                                    NC2254.2
053900 EVA-DELETE-GF-5-2.                                               NC2254.2
054000     PERFORM DE-LETE.                                             NC2254.2
054100     GO TO EVA-WRITE-GF-5-2.                                      NC2254.2
054200 EVA-FAIL-GF-5-2.                                                 NC2254.2
054300     MOVE   "EXPECTING EQUAL LITERAL" TO RE-MARK                  NC2254.2
054400     PERFORM FAIL.                                                NC2254.2
054500 EVA-WRITE-GF-5-2.                                                NC2254.2
054600     PERFORM PRINT-DETAIL.                                        NC2254.2
054700*                                                                 NC2254.2
054800 EVA-INIT-GF-6.                                                   NC2254.2
054900     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
055000     MOVE    1  TO REC-CT.                                        NC2254.2
055100     MOVE   "A" TO WRK-XN-00001-1.                                NC2254.2
055200     GO TO   EVA-TEST-GF-6-1.                                     NC2254.2
055300 EVA-DELETE-GF-6.                                                 NC2254.2
055400     PERFORM DE-LETE.                                             NC2254.2
055500     PERFORM PRINT-DETAIL.                                        NC2254.2
055600     GO TO   EVA-INIT-GF-7.                                       NC2254.2
055700 EVA-TEST-GF-6-1.                                                 NC2254.2
055800     MOVE   "EVA-TEST-GF-6-1" TO PAR-NAME.                        NC2254.2
055900     EVALUATE WRK-XN-00001-1                                      NC2254.2
056000        WHEN "Z"                                                  NC2254.2
056100             GO TO EVA-FAIL-GF-6-1.                               NC2254.2
056200     PERFORM PASS.                                                NC2254.2
056300     GO TO   EVA-WRITE-GF-6-1.                                    NC2254.2
056400 EVA-DELETE-GF-6-1.                                               NC2254.2
056500     PERFORM DE-LETE.                                             NC2254.2
056600     GO TO EVA-WRITE-GF-6-1.                                      NC2254.2
056700 EVA-FAIL-GF-6-1.                                                 NC2254.2
056800     MOVE   "EXPECTING UNEQUAL LITERAL" TO RE-MARK                NC2254.2
056900     PERFORM FAIL.                                                NC2254.2
057000 EVA-WRITE-GF-6-1.                                                NC2254.2
057100     PERFORM PRINT-DETAIL.                                        NC2254.2
057200*                                                                 NC2254.2
057300 EVA-TEST-GF-6-2.                                                 NC2254.2
057400     ADD     1 TO REC-CT.                                         NC2254.2
057500     MOVE   "EVA-TEST-GF-6-2" TO PAR-NAME.                        NC2254.2
057600     EVALUATE WRK-XN-00001-1                                      NC2254.2
057700        WHEN NOT "Z"                                              NC2254.2
057800             PERFORM PASS                                         NC2254.2
057900             GO TO   EVA-WRITE-GF-6-2.                            NC2254.2
058000     GO TO EVA-FAIL-GF-6-2.                                       NC2254.2
058100 EVA-DELETE-GF-6-2.                                               NC2254.2
058200     PERFORM DE-LETE.                                             NC2254.2
058300     GO TO EVA-WRITE-GF-6-2.                                      NC2254.2
058400 EVA-FAIL-GF-6-2.                                                 NC2254.2
058500     MOVE   "EXPECTING UNEQUAL LITERAL" TO RE-MARK.               NC2254.2
058600     PERFORM FAIL.                                                NC2254.2
058700 EVA-WRITE-GF-6-2.                                                NC2254.2
058800     PERFORM PRINT-DETAIL.                                        NC2254.2
058900*                                                                 NC2254.2
059000 EVA-INIT-GF-7.                                                   NC2254.2
059100     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
059200     MOVE    1  TO REC-CT.                                        NC2254.2
059300     MOVE    89 TO WRK-DU-08V00.                                  NC2254.2
059400     GO TO   EVA-TEST-GF-7-1.                                     NC2254.2
059500 EVA-DELETE-GF-7.                                                 NC2254.2
059600     PERFORM DE-LETE.                                             NC2254.2
059700     PERFORM PRINT-DETAIL.                                        NC2254.2
059800     GO TO   EVA-INIT-GF-8.                                       NC2254.2
059900 EVA-TEST-GF-7-1.                                                 NC2254.2
060000     MOVE   "EVA-TEST-GF-7-1" TO PAR-NAME.                        NC2254.2
060100     EVALUATE WRK-DU-08V00                                        NC2254.2
060200        WHEN (33 + (99 - 43))                                     NC2254.2
060300             PERFORM PASS                                         NC2254.2
060400             GO TO   EVA-WRITE-GF-7-1.                            NC2254.2
060500     GO TO EVA-FAIL-GF-7-1.                                       NC2254.2
060600 EVA-DELETE-GF-7-1.                                               NC2254.2
060700     PERFORM DE-LETE.                                             NC2254.2
060800     GO TO EVA-WRITE-GF-7-1.                                      NC2254.2
060900 EVA-FAIL-GF-7-1.                                                 NC2254.2
061000     MOVE   "EXPECTING EQUAL ARITHMETIC EXPRESSION" TO RE-MARK.   NC2254.2
061100     PERFORM FAIL.                                                NC2254.2
061200 EVA-WRITE-GF-7-1.                                                NC2254.2
061300     PERFORM PRINT-DETAIL.                                        NC2254.2
061400*                                                                 NC2254.2
061500 EVA-TEST-GF-7-2.                                                 NC2254.2
061600     ADD     1       TO REC-CT.                                   NC2254.2
061700     MOVE   "EVA-TEST-GF-7-2" TO PAR-NAME.                        NC2254.2
061800     EVALUATE WRK-DU-08V00                                        NC2254.2
061900        WHEN NOT (33 + (99 - 43))                                 NC2254.2
062000             GO TO EVA-FAIL-GF-7-2.                               NC2254.2
062100     PERFORM PASS.                                                NC2254.2
062200     GO TO   EVA-WRITE-GF-7-2.                                    NC2254.2
062300 EVA-DELETE-GF-7-2.                                               NC2254.2
062400     PERFORM DE-LETE.                                             NC2254.2
062500     GO TO EVA-WRITE-GF-7-2.                                      NC2254.2
062600 EVA-FAIL-GF-7-2.                                                 NC2254.2
062700     MOVE   "EXPECTING EQUAL ARITHMETIC EXPRESSION"               NC2254.2
062800          TO RE-MARK                                              NC2254.2
062900     PERFORM FAIL.                                                NC2254.2
063000 EVA-WRITE-GF-7-2.                                                NC2254.2
063100     PERFORM PRINT-DETAIL.                                        NC2254.2
063200*                                                                 NC2254.2
063300 EVA-INIT-GF-8.                                                   NC2254.2
063400     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
063500     MOVE    1  TO REC-CT.                                        NC2254.2
063600     MOVE    89 TO WRK-DU-08V00.                                  NC2254.2
063700     GO TO   EVA-TEST-GF-8-1.                                     NC2254.2
063800 EVA-DELETE-GF-8.                                                 NC2254.2
063900     PERFORM DE-LETE.                                             NC2254.2
064000     PERFORM PRINT-DETAIL.                                        NC2254.2
064100     GO TO   EVA-INIT-GF-9.                                       NC2254.2
064200 EVA-TEST-GF-8-1.                                                 NC2254.2
064300     MOVE   "EVA-TEST-GF-8-1" TO PAR-NAME.                        NC2254.2
064400     EVALUATE WRK-DU-08V00                                        NC2254.2
064500        WHEN (2 + 4 + 8 + 16 + 32 + 64)                           NC2254.2
064600             GO TO EVA-FAIL-GF-8-1.                               NC2254.2
064700     PERFORM PASS.                                                NC2254.2
064800     GO TO   EVA-WRITE-GF-8-1.                                    NC2254.2
064900 EVA-DELETE-GF-8-1.                                               NC2254.2
065000     PERFORM DE-LETE.                                             NC2254.2
065100     GO TO EVA-WRITE-GF-8-1.                                      NC2254.2
065200 EVA-FAIL-GF-8-1.                                                 NC2254.2
065300     MOVE   "EXPECTING UNEQUAL ARITHMETIC EXPRESSION"             NC2254.2
065400          TO RE-MARK                                              NC2254.2
065500     PERFORM FAIL.                                                NC2254.2
065600 EVA-WRITE-GF-8-1.                                                NC2254.2
065700     PERFORM PRINT-DETAIL.                                        NC2254.2
065800*                                                                 NC2254.2
065900 EVA-TEST-GF-8-2.                                                 NC2254.2
066000     ADD     1 TO REC-CT.                                         NC2254.2
066100     MOVE   "EVA-TEST-GF-8-2" TO PAR-NAME.                        NC2254.2
066200     EVALUATE WRK-DU-08V00                                        NC2254.2
066300        WHEN NOT (2 + 4 + 8 + 16 + 32 + 64)                       NC2254.2
066400             PERFORM PASS                                         NC2254.2
066500             GO TO   EVA-WRITE-GF-8-2.                            NC2254.2
066600     GO TO   EVA-FAIL-GF-8-2.                                     NC2254.2
066700 EVA-DELETE-GF-8-2.                                               NC2254.2
066800     PERFORM DE-LETE.                                             NC2254.2
066900     GO TO EVA-WRITE-GF-8-2.                                      NC2254.2
067000 EVA-FAIL-GF-8-2.                                                 NC2254.2
067100     MOVE   "EXPECTING UNEQUAL ARITHMETIC EXPRESSION" TO RE-MARK. NC2254.2
067200     PERFORM FAIL.                                                NC2254.2
067300 EVA-WRITE-GF-8-2.                                                NC2254.2
067400     PERFORM PRINT-DETAIL.                                        NC2254.2
067500*                                                                 NC2254.2
067600 EVA-INIT-GF-9.                                                   NC2254.2
067700     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
067800     MOVE    1  TO REC-CT.                                        NC2254.2
067900     MOVE   "J" TO WRK-XN-00001-1.                                NC2254.2
068000     MOVE   "A" TO WRK-XN-00001-2.                                NC2254.2
068100     MOVE   "N" TO WRK-XN-00001-3.                                NC2254.2
068200     GO TO   EVA-TEST-GF-9-1.                                     NC2254.2
068300 EVA-DELETE-GF-9.                                                 NC2254.2
068400     PERFORM DE-LETE.                                             NC2254.2
068500     PERFORM PRINT-DETAIL.                                        NC2254.2
068600     GO TO   EVA-INIT-GF-10.                                      NC2254.2
068700 EVA-TEST-GF-9-1.                                                 NC2254.2
068800     MOVE   "EVA-TEST-GF-9-1" TO PAR-NAME.                        NC2254.2
068900     EVALUATE WRK-XN-00001-1                                      NC2254.2
069000        WHEN WRK-XN-00001-2 THRU WRK-XN-00001-3                   NC2254.2
069100             PERFORM PASS                                         NC2254.2
069200             GO TO   EVA-WRITE-GF-9-1.                            NC2254.2
069300     GO TO   EVA-FAIL-GF-9-1.                                     NC2254.2
069400 EVA-DELETE-GF-9-1.                                               NC2254.2
069500     PERFORM DE-LETE.                                             NC2254.2
069600     GO TO EVA-WRITE-GF-9-1.                                      NC2254.2
069700 EVA-FAIL-GF-9-1.                                                 NC2254.2
069800     MOVE   "SUBJECT IDENTIFIER SHOULD BE WITHIN RANGE"           NC2254.2
069900          TO RE-MARK.                                             NC2254.2
070000     PERFORM FAIL.                                                NC2254.2
070100 EVA-WRITE-GF-9-1.                                                NC2254.2
070200     PERFORM PRINT-DETAIL.                                        NC2254.2
070300*                                                                 NC2254.2
070400 EVA-TEST-GF-9-2.                                                 NC2254.2
070500     ADD     1       TO REC-CT.                                   NC2254.2
070600     MOVE   "EVA-TEST-GF-9-2" TO PAR-NAME.                        NC2254.2
070700     EVALUATE WRK-XN-00001-1                                      NC2254.2
070800        WHEN NOT WRK-XN-00001-2 THRU WRK-XN-00001-3               NC2254.2
070900             GO TO EVA-FAIL-GF-9-2.                               NC2254.2
071000     PERFORM PASS.                                                NC2254.2
071100     GO TO   EVA-WRITE-GF-9-2.                                    NC2254.2
071200 EVA-DELETE-GF-9-2.                                               NC2254.2
071300     PERFORM DE-LETE.                                             NC2254.2
071400     GO TO EVA-WRITE-GF-9-2.                                      NC2254.2
071500 EVA-FAIL-GF-9-2.                                                 NC2254.2
071600     MOVE   "SUBJECT IDENTIFIER SHOULD BE WITHIN RANGE"           NC2254.2
071700          TO RE-MARK                                              NC2254.2
071800     PERFORM FAIL.                                                NC2254.2
071900 EVA-WRITE-GF-9-2.                                                NC2254.2
072000     PERFORM PRINT-DETAIL.                                        NC2254.2
072100*                                                                 NC2254.2
072200 EVA-INIT-GF-10.                                                  NC2254.2
072300     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
072400     MOVE    1  TO REC-CT.                                        NC2254.2
072500     MOVE   "J" TO WRK-XN-00001-1.                                NC2254.2
072600     MOVE   "A" TO WRK-XN-00001-2.                                NC2254.2
072700     MOVE   "N" TO WRK-XN-00001-3.                                NC2254.2
072800     GO TO   EVA-TEST-GF-10-1.                                    NC2254.2
072900 EVA-DELETE-GF-10.                                                NC2254.2
073000     PERFORM DE-LETE.                                             NC2254.2
073100     PERFORM PRINT-DETAIL.                                        NC2254.2
073200     GO TO   EVA-INIT-GF-11.                                      NC2254.2
073300 EVA-TEST-GF-10-1.                                                NC2254.2
073400     MOVE   "EVA-TEST-GF-10-1" TO PAR-NAME.                       NC2254.2
073500     EVALUATE WRK-XN-00001-3                                      NC2254.2
073600        WHEN WRK-XN-00001-2 THRU WRK-XN-00001-1                   NC2254.2
073700             GO TO EVA-FAIL-GF-10-1.                              NC2254.2
073800     PERFORM PASS.                                                NC2254.2
073900     GO TO   EVA-WRITE-GF-10-1.                                   NC2254.2
074000 EVA-DELETE-GF-10-1.                                              NC2254.2
074100     PERFORM DE-LETE.                                             NC2254.2
074200     GO TO EVA-WRITE-GF-10-1.                                     NC2254.2
074300 EVA-FAIL-GF-10-1.                                                NC2254.2
074400     MOVE "SUBJECT IDENTIFIER SHOULD NOT BE WITHIN RANGE"         NC2254.2
074500          TO RE-MARK                                              NC2254.2
074600     PERFORM FAIL.                                                NC2254.2
074700 EVA-WRITE-GF-10-1.                                               NC2254.2
074800     PERFORM PRINT-DETAIL.                                        NC2254.2
074900*                                                                 NC2254.2
075000 EVA-TEST-GF-10-2.                                                NC2254.2
075100     ADD     1 TO REC-CT.                                         NC2254.2
075200     MOVE   "EVA-TEST-GF-10-2" TO PAR-NAME.                       NC2254.2
075300     EVALUATE WRK-XN-00001-3                                      NC2254.2
075400        WHEN NOT WRK-XN-00001-2 THRU WRK-XN-00001-1               NC2254.2
075500             PERFORM PASS                                         NC2254.2
075600             GO TO   EVA-WRITE-GF-10-2.                           NC2254.2
075700     GO TO EVA-FAIL-GF-10-2.                                      NC2254.2
075800 EVA-DELETE-GF-10-2.                                              NC2254.2
075900     PERFORM DE-LETE.                                             NC2254.2
076000     GO TO EVA-WRITE-GF-10-2.                                     NC2254.2
076100 EVA-FAIL-GF-10-2.                                                NC2254.2
076200     MOVE "  SUBJECT IDENTIFIER SHOULD NOT BE WITHIN RANGE"       NC2254.2
076300          TO RE-MARK.                                             NC2254.2
076400     PERFORM FAIL.                                                NC2254.2
076500 EVA-WRITE-GF-10-2.                                               NC2254.2
076600     PERFORM PRINT-DETAIL.                                        NC2254.2
076700*                                                                 NC2254.2
076800 EVA-INIT-GF-11.                                                  NC2254.2
076900     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
077000     MOVE    1  TO REC-CT.                                        NC2254.2
077100     MOVE   "J" TO WRK-XN-00001-1.                                NC2254.2
077200     MOVE   "A" TO WRK-XN-00001-2.                                NC2254.2
077300     MOVE   "N" TO WRK-XN-00001-3.                                NC2254.2
077400     GO TO   EVA-TEST-GF-11-1.                                    NC2254.2
077500 EVA-DELETE-GF-11.                                                NC2254.2
077600     PERFORM DE-LETE.                                             NC2254.2
077700     PERFORM PRINT-DETAIL.                                        NC2254.2
077800     GO TO   EVA-INIT-GF-12.                                      NC2254.2
077900 EVA-TEST-GF-11-1.                                                NC2254.2
078000     MOVE   "EVA-TEST-GF-11-1" TO PAR-NAME.                       NC2254.2
078100     EVALUATE WRK-XN-00001-1                                      NC2254.2
078200        WHEN "A" THROUGH "N"                                      NC2254.2
078300             PERFORM PASS                                         NC2254.2
078400             GO TO   EVA-WRITE-GF-11-1.                           NC2254.2
078500     GO TO EVA-FAIL-GF-11-1.                                      NC2254.2
078600 EVA-DELETE-GF-11-1.                                              NC2254.2
078700     PERFORM DE-LETE.                                             NC2254.2
078800     GO TO EVA-WRITE-GF-11-1.                                     NC2254.2
078900 EVA-FAIL-GF-11-1.                                                NC2254.2
079000     MOVE   "SUBJECT IDENTIFIER SHOULD BE WITHIN RANGE"           NC2254.2
079100          TO RE-MARK.                                             NC2254.2
079200     PERFORM FAIL.                                                NC2254.2
079300 EVA-WRITE-GF-11-1.                                               NC2254.2
079400     PERFORM PRINT-DETAIL.                                        NC2254.2
079500*                                                                 NC2254.2
079600 EVA-TEST-GF-11-2.                                                NC2254.2
079700     ADD     1       TO REC-CT.                                   NC2254.2
079800     MOVE   "EVA-TEST-GF-11-2" TO PAR-NAME.                       NC2254.2
079900     EVALUATE WRK-XN-00001-1                                      NC2254.2
080000        WHEN NOT "A" THROUGH "N"                                  NC2254.2
080100             GO TO EVA-FAIL-GF-11-2.                              NC2254.2
080200     PERFORM PASS.                                                NC2254.2
080300     GO TO   EVA-WRITE-GF-11-2.                                   NC2254.2
080400 EVA-DELETE-GF-11-2.                                              NC2254.2
080500     PERFORM DE-LETE.                                             NC2254.2
080600     GO TO EVA-WRITE-GF-11-2.                                     NC2254.2
080700 EVA-FAIL-GF-11-2.                                                NC2254.2
080800     MOVE   "SUBJECT IDENTIFIER SHOULD BE WITHIN RANGE"           NC2254.2
080900          TO RE-MARK                                              NC2254.2
081000     PERFORM FAIL.                                                NC2254.2
081100 EVA-WRITE-GF-11-2.                                               NC2254.2
081200     PERFORM PRINT-DETAIL.                                        NC2254.2
081300*                                                                 NC2254.2
081400 EVA-INIT-GF-12.                                                  NC2254.2
081500     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
081600     MOVE    1  TO REC-CT.                                        NC2254.2
081700     MOVE   "J" TO WRK-XN-00001-1.                                NC2254.2
081800     MOVE   "A" TO WRK-XN-00001-2.                                NC2254.2
081900     MOVE   "N" TO WRK-XN-00001-3.                                NC2254.2
082000     GO TO   EVA-TEST-GF-12-1.                                    NC2254.2
082100 EVA-DELETE-GF-12.                                                NC2254.2
082200     PERFORM DE-LETE.                                             NC2254.2
082300     PERFORM PRINT-DETAIL.                                        NC2254.2
082400     GO TO   EVA-INIT-GF-13.                                      NC2254.2
082500 EVA-TEST-GF-12-1.                                                NC2254.2
082600     MOVE   "EVA-TEST-GF-12-1" TO PAR-NAME.                       NC2254.2
082700     EVALUATE WRK-XN-00001-3                                      NC2254.2
082800        WHEN "A" THROUGH "J"                                      NC2254.2
082900             GO TO EVA-FAIL-GF-12-1.                              NC2254.2
083000     PERFORM PASS.                                                NC2254.2
083100     GO TO   EVA-WRITE-GF-12-1.                                   NC2254.2
083200 EVA-DELETE-GF-12-1.                                              NC2254.2
083300     PERFORM DE-LETE.                                             NC2254.2
083400     GO TO EVA-WRITE-GF-12-1.                                     NC2254.2
083500 EVA-FAIL-GF-12-1.                                                NC2254.2
083600     MOVE "SUBJECT IDENTIFIER SHOULD NOT BE WITHIN RANGE"         NC2254.2
083700          TO RE-MARK                                              NC2254.2
083800     PERFORM FAIL.                                                NC2254.2
083900 EVA-WRITE-GF-12-1.                                               NC2254.2
084000     PERFORM PRINT-DETAIL.                                        NC2254.2
084100*                                                                 NC2254.2
084200 EVA-TEST-GF-12-2.                                                NC2254.2
084300     ADD     1 TO REC-CT.                                         NC2254.2
084400     MOVE   "EVA-TEST-GF-12-2" TO PAR-NAME.                       NC2254.2
084500     EVALUATE WRK-XN-00001-3                                      NC2254.2
084600        WHEN NOT "A" THROUGH "J"                                  NC2254.2
084700             PERFORM PASS                                         NC2254.2
084800             GO TO   EVA-WRITE-GF-12-2.                           NC2254.2
084900     GO TO   EVA-FAIL-GF-12-2.                                    NC2254.2
085000 EVA-DELETE-GF-12-2.                                              NC2254.2
085100     PERFORM DE-LETE.                                             NC2254.2
085200     GO TO EVA-WRITE-GF-12-2.                                     NC2254.2
085300 EVA-FAIL-GF-12-2.                                                NC2254.2
085400     MOVE "  SUBJECT IDENTIFIER SHOULD NOT BE WITHIN RANGE"       NC2254.2
085500          TO RE-MARK.                                             NC2254.2
085600     PERFORM FAIL.                                                NC2254.2
085700 EVA-WRITE-GF-12-2.                                               NC2254.2
085800     PERFORM PRINT-DETAIL.                                        NC2254.2
085900*                                                                 NC2254.2
086000 EVA-INIT-GF-13.                                                  NC2254.2
086100     MOVE   "VI-84 6.12.4 GR1(a)" TO ANSI-REFERENCE.              NC2254.2
086200     MOVE    1  TO REC-CT.                                        NC2254.2
086300     MOVE    89 TO WRK-DU-08V00.                                  NC2254.2
086400     GO TO   EVA-TEST-GF-13-1.                                    NC2254.2
086500 EVA-DELETE-GF-13.                                                NC2254.2
086600     PERFORM DE-LETE.                                             NC2254.2
086700     PERFORM PRINT-DETAIL.                                        NC2254.2
086800     GO TO   EVA-INIT-GF-14.                                      NC2254.2
086900 EVA-TEST-GF-13-1.                                                NC2254.2
087000     MOVE   "EVA-TEST-GF-13-1" TO PAR-NAME.                       NC2254.2
087100     EVALUATE WRK-DU-08V00                                        NC2254.2
087200        WHEN (11 + (99 - 43)) THRU (20 * 5)                       NC2254.2
087300             PERFORM PASS                                         NC2254.2
087400             GO TO   EVA-WRITE-GF-13-1.                           NC2254.2
087500     GO TO   EVA-FAIL-GF-13-1.                                    NC2254.2
087600 EVA-DELETE-GF-13-1.                                              NC2254.2
087700     PERFORM DE-LETE.                                             NC2254.2
087800     GO TO EVA-WRITE-GF-13-1.                                     NC2254.2
087900 EVA-FAIL-GF-13-1.                                                NC2254.2
088000     MOVE   "SUBJECT IDENTIFIER SHOULD BE WITHIN RANGE"           NC2254.2
088100          TO RE-MARK.                                             NC2254.2
088200     PERFORM FAIL.                                                NC2254.2
088300 EVA-WRITE-GF-13-1.                                               NC2254.2
088400     PERFORM PRINT-DETAIL.                                        NC2254.2
088500*                                                                 NC2254.2
088600 EVA-TEST-GF-13-2.                                                NC2254.2
088700     ADD     1       TO REC-CT.                                   NC2254.2
088800     MOVE   "EVA-TEST-GF-13-2" TO PAR-NAME.                       NC2254.2
088900     EVALUATE WRK-DU-08V00                                        NC2254.2
089000        WHEN NOT (11 + (99 - 43)) THRU (20 * 5)                   NC2254.2
089100             GO TO EVA-FAIL-GF-13-2.                              NC2254.2
089200     PERFORM PASS.                                                NC2254.2
089300     GO TO   EVA-WRITE-GF-13-2.                                   NC2254.2
089400 EVA-DELETE-GF-13-2.                                              NC2254.2
089500     PERFORM DE-LETE.                                             NC2254.2
089600     GO TO EVA-WRITE-GF-13-2.                                     NC2254.2
089700 EVA-FAIL-GF-13-2.                                                NC2254.2
089800     MOVE   "SUBJECT IDENTIFIER SHOULD BE WITHIN RANGE"           NC2254.2
089900          TO RE-MARK                                              NC2254.2
090000     PERFORM FAIL.                                                NC2254.2
090100 EVA-WRITE-GF-13-2.                                               NC2254.2
090200     PERFORM PRINT-DETAIL.                                        NC2254.2
090300*                                                                 NC2254.2
090400 EVA-INIT-GF-14.                                                  NC2254.2
090500     MOVE   "VI-84 6.12.4 GR1(A)" TO ANSI-REFERENCE.              NC2254.2
090600     MOVE    1  TO REC-CT.                                        NC2254.2
090700     MOVE    89 TO WRK-DU-08V00.                                  NC2254.2
090800     GO TO   EVA-TEST-GF-14-1.                                    NC2254.2
090900 EVA-DELETE-GF-14.                                                NC2254.2
091000     PERFORM DE-LETE.                                             NC2254.2
091100     PERFORM PRINT-DETAIL.                                        NC2254.2
091200     GO TO   EVA-INIT-GF-15.                                      NC2254.2
091300 EVA-TEST-GF-14-1.                                                NC2254.2
091400     MOVE   "EVA-TEST-GF-14-1" TO PAR-NAME.                       NC2254.2
091500     EVALUATE WRK-DU-08V00                                        NC2254.2
091600        WHEN (11 + (99 - 20)) THRU (20 * 5)                       NC2254.2
091700             GO TO EVA-FAIL-GF-14-1.                              NC2254.2
091800     PERFORM PASS.                                                NC2254.2
091900     GO TO   EVA-WRITE-GF-14-1.                                   NC2254.2
092000 EVA-DELETE-GF-14-1.                                              NC2254.2
092100     PERFORM DE-LETE.                                             NC2254.2
092200     GO TO EVA-WRITE-GF-14-1.                                     NC2254.2
092300 EVA-FAIL-GF-14-1.                                                NC2254.2
092400     MOVE "SUBJECT IDENTIFIER SHOULD NOT BE WITHIN RANGE"         NC2254.2
092500          TO RE-MARK                                              NC2254.2
092600     PERFORM FAIL.                                                NC2254.2
092700 EVA-WRITE-GF-14-1.                                               NC2254.2
092800     PERFORM PRINT-DETAIL.                                        NC2254.2
092900*                                                                 NC2254.2
093000 EVA-TEST-GF-14-2.                                                NC2254.2
093100     ADD     1 TO REC-CT.                                         NC2254.2
093200     MOVE   "EVA-TEST-GF-14-2" TO PAR-NAME.                       NC2254.2
093300     EVALUATE WRK-DU-08V00                                        NC2254.2
093400        WHEN NOT (11 + (99 - 20)) THRU (20 * 5)                   NC2254.2
093500             PERFORM PASS                                         NC2254.2
093600             GO TO   EVA-WRITE-GF-14-2.                           NC2254.2
093700     GO TO   EVA-FAIL-GF-14-2.                                    NC2254.2
093800 EVA-DELETE-GF-14-2.                                              NC2254.2
093900     PERFORM DE-LETE.                                             NC2254.2
094000     GO TO EVA-WRITE-GF-14-2.                                     NC2254.2
094100 EVA-FAIL-GF-14-2.                                                NC2254.2
094200     MOVE   "SUBJECT IDENTIFIER SHOULD NOT BE WITHIN RANGE"       NC2254.2
094300          TO RE-MARK.                                             NC2254.2
094400     PERFORM FAIL.                                                NC2254.2
094500 EVA-WRITE-GF-14-2.                                               NC2254.2
094600     PERFORM PRINT-DETAIL.                                        NC2254.2
094700*                                                                 NC2254.2
094800 EVA-INIT-GF-15.                                                  NC2254.2
094900     MOVE   "VI-84 6.12.4 GR1(b)" TO ANSI-REFERENCE.              NC2254.2
095000     MOVE    1  TO REC-CT.                                        NC2254.2
095100     MOVE    26 TO WRK-DU-08V00.                                  NC2254.2
095200     GO TO   EVA-TEST-GF-15-1.                                    NC2254.2
095300 EVA-DELETE-GF-15.                                                NC2254.2
095400     PERFORM DE-LETE.                                             NC2254.2
095500     PERFORM PRINT-DETAIL.                                        NC2254.2
095600     GO TO   EVA-INIT-GF-16.                                      NC2254.2
095700 EVA-TEST-GF-15-1.                                                NC2254.2
095800     MOVE   "EVA-TEST-GF-15-1" TO PAR-NAME.                       NC2254.2
095900     EVALUATE 26                                                  NC2254.2
096000        WHEN WRK-DU-08V00                                         NC2254.2
096100             PERFORM PASS                                         NC2254.2
096200             GO TO   EVA-WRITE-GF-15-1.                           NC2254.2
096300     GO TO   EVA-FAIL-GF-15-1.                                    NC2254.2
096400 EVA-DELETE-GF-15-1.                                              NC2254.2
096500     PERFORM DE-LETE.                                             NC2254.2
096600     GO TO EVA-WRITE-GF-15-1.                                     NC2254.2
096700 EVA-FAIL-GF-15-1.                                                NC2254.2
096800     MOVE   "IDENTIFIER AND LITERAL SHOULD BE EQUAL"              NC2254.2
096900          TO RE-MARK.                                             NC2254.2
097000     PERFORM FAIL.                                                NC2254.2
097100 EVA-WRITE-GF-15-1.                                               NC2254.2
097200     PERFORM PRINT-DETAIL.                                        NC2254.2
097300*                                                                 NC2254.2
097400 EVA-TEST-GF-15-2.                                                NC2254.2
097500     ADD     1       TO REC-CT.                                   NC2254.2
097600     MOVE   "EVA-TEST-GF-15-2" TO PAR-NAME.                       NC2254.2
097700     EVALUATE 26                                                  NC2254.2
097800        WHEN NOT WRK-DU-08V00                                     NC2254.2
097900             GO TO EVA-FAIL-GF-15-2.                              NC2254.2
098000     PERFORM PASS.                                                NC2254.2
098100     GO TO   EVA-WRITE-GF-15-2.                                   NC2254.2
098200 EVA-DELETE-GF-15-2.                                              NC2254.2
098300     PERFORM DE-LETE.                                             NC2254.2
098400     GO TO EVA-WRITE-GF-15-2.                                     NC2254.2
098500 EVA-FAIL-GF-15-2.                                                NC2254.2
098600     MOVE   "IDENTIFIER AND LITERAL SHOULD BE EQUAL"              NC2254.2
098700          TO RE-MARK                                              NC2254.2
098800     PERFORM FAIL.                                                NC2254.2
098900 EVA-WRITE-GF-15-2.                                               NC2254.2
099000     PERFORM PRINT-DETAIL.                                        NC2254.2
099100*                                                                 NC2254.2
099200 EVA-INIT-GF-16.                                                  NC2254.2
099300     MOVE   "VI-84 6.12.4 GR1(b)" TO ANSI-REFERENCE.              NC2254.2
099400     MOVE    1  TO REC-CT.                                        NC2254.2
099500     MOVE    78 TO WRK-DU-08V00.                                  NC2254.2
099600     GO TO   EVA-TEST-GF-16-1.                                    NC2254.2
099700 EVA-DELETE-GF-16.                                                NC2254.2
099800     PERFORM DE-LETE.                                             NC2254.2
099900     PERFORM PRINT-DETAIL.                                        NC2254.2
100000     GO TO   EVA-INIT-GF-17.                                      NC2254.2
100100 EVA-TEST-GF-16-1.                                                NC2254.2
100200     MOVE   "EVA-TEST-GF-16-1" TO PAR-NAME.                       NC2254.2
100300     EVALUATE 1234                                                NC2254.2
100400        WHEN WRK-DU-08V00                                         NC2254.2
100500             GO TO EVA-FAIL-GF-16-1.                              NC2254.2
100600     PERFORM PASS.                                                NC2254.2
100700     GO TO   EVA-WRITE-GF-16-1.                                   NC2254.2
100800 EVA-DELETE-GF-16-1.                                              NC2254.2
100900     PERFORM DE-LETE.                                             NC2254.2
101000     GO TO EVA-WRITE-GF-16-1.                                     NC2254.2
101100 EVA-FAIL-GF-16-1.                                                NC2254.2
101200     MOVE   "IDENTIFIER AND LITERAL SHOULD NOT BE EQUAL"          NC2254.2
101300          TO RE-MARK                                              NC2254.2
101400     PERFORM FAIL.                                                NC2254.2
101500 EVA-WRITE-GF-16-1.                                               NC2254.2
101600     PERFORM PRINT-DETAIL.                                        NC2254.2
101700*                                                                 NC2254.2
101800 EVA-TEST-GF-16-2.                                                NC2254.2
101900     ADD     1 TO REC-CT.                                         NC2254.2
102000     MOVE   "EVA-TEST-GF-16-2" TO PAR-NAME.                       NC2254.2
102100     EVALUATE 1234                                                NC2254.2
102200        WHEN NOT WRK-DU-08V00                                     NC2254.2
102300             PERFORM PASS                                         NC2254.2
102400             GO TO   EVA-WRITE-GF-16-2.                           NC2254.2
102500     GO TO   EVA-FAIL-GF-16-2.                                    NC2254.2
102600 EVA-DELETE-GF-16-2.                                              NC2254.2
102700     PERFORM DE-LETE.                                             NC2254.2
102800     GO TO EVA-WRITE-GF-16-2.                                     NC2254.2
102900 EVA-FAIL-GF-16-2.                                                NC2254.2
103000     MOVE   "IDENTIFIER AND LITERAL SHOULD NOT BE EQUAL"          NC2254.2
103100          TO RE-MARK.                                             NC2254.2
103200     PERFORM FAIL.                                                NC2254.2
103300 EVA-WRITE-GF-16-2.                                               NC2254.2
103400     PERFORM PRINT-DETAIL.                                        NC2254.2
103500*                                                                 NC2254.2
103600 EVA-INIT-GF-17.                                                  NC2254.2
103700     MOVE   "VI-84 6.12.4 GR1(d)" TO ANSI-REFERENCE.              NC2254.2
103800     MOVE    1  TO REC-CT.                                        NC2254.2
103900     MOVE    8  TO WRK-XN-00001-1.                                NC2254.2
104000     GO TO   EVA-TEST-GF-17-1.                                    NC2254.2
104100 EVA-DELETE-GF-17.                                                NC2254.2
104200     PERFORM DE-LETE.                                             NC2254.2
104300     PERFORM PRINT-DETAIL.                                        NC2254.2
104400     GO TO   EVA-INIT-GF-18.                                      NC2254.2
104500 EVA-TEST-GF-17-1.                                                NC2254.2
104600     MOVE   "EVA-TEST-GF-17-1" TO PAR-NAME.                       NC2254.2
104700     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
104800        WHEN TRUE                                                 NC2254.2
104900             PERFORM PASS                                         NC2254.2
105000             GO TO   EVA-WRITE-GF-17-1.                           NC2254.2
105100     GO TO   EVA-FAIL-GF-17-1.                                    NC2254.2
105200 EVA-DELETE-GF-17-1.                                              NC2254.2
105300     PERFORM DE-LETE.                                             NC2254.2
105400     GO TO EVA-WRITE-GF-17-1.                                     NC2254.2
105500 EVA-FAIL-GF-17-1.                                                NC2254.2
105600     MOVE   "CONDITIONAL EXPRESSION SHOULD BE TRUE"               NC2254.2
105700          TO RE-MARK.                                             NC2254.2
105800     PERFORM FAIL.                                                NC2254.2
105900 EVA-WRITE-GF-17-1.                                               NC2254.2
106000     PERFORM PRINT-DETAIL.                                        NC2254.2
106100*                                                                 NC2254.2
106200 EVA-TEST-GF-17-2.                                                NC2254.2
106300     ADD     1       TO REC-CT.                                   NC2254.2
106400     MOVE   "EVA-TEST-GF-17-2" TO PAR-NAME.                       NC2254.2
106500     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
106600        WHEN FALSE                                                NC2254.2
106700             GO TO EVA-FAIL-GF-17-2.                              NC2254.2
106800     PERFORM PASS.                                                NC2254.2
106900     GO TO   EVA-WRITE-GF-17-2.                                   NC2254.2
107000 EVA-DELETE-GF-17-2.                                              NC2254.2
107100     PERFORM DE-LETE.                                             NC2254.2
107200     GO TO EVA-WRITE-GF-17-2.                                     NC2254.2
107300 EVA-FAIL-GF-17-2.                                                NC2254.2
107400     MOVE   "CONDITIONAL EXPRESSION SHOULD BE TRUE"               NC2254.2
107500          TO RE-MARK                                              NC2254.2
107600     PERFORM FAIL.                                                NC2254.2
107700 EVA-WRITE-GF-17-2.                                               NC2254.2
107800     PERFORM PRINT-DETAIL.                                        NC2254.2
107900*                                                                 NC2254.2
108000 EVA-TEST-GF-17-3.                                                NC2254.2
108100     ADD     1       TO REC-CT.                                   NC2254.2
108200     MOVE   "EVA-TEST-GF-17-3" TO PAR-NAME.                       NC2254.2
108300     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
108400        WHEN ANY                                                  NC2254.2
108500             PERFORM PASS                                         NC2254.2
108600             GO TO   EVA-WRITE-GF-17-3.                           NC2254.2
108700     GO TO   EVA-FAIL-GF-17-3.                                    NC2254.2
108800 EVA-DELETE-GF-17-3.                                              NC2254.2
108900     PERFORM DE-LETE.                                             NC2254.2
109000     GO TO EVA-WRITE-GF-17-3.                                     NC2254.2
109100 EVA-FAIL-GF-17-3.                                                NC2254.2
109200     MOVE   "WHEN 'ANY' SHOULD HAVE EXECUTED" TO RE-MARK          NC2254.2
109300     PERFORM FAIL.                                                NC2254.2
109400 EVA-WRITE-GF-17-3.                                               NC2254.2
109500     PERFORM PRINT-DETAIL.                                        NC2254.2
109600*                                                                 NC2254.2
109700 EVA-INIT-GF-18.                                                  NC2254.2
109800     MOVE   "VI-84 6.12.4 GR1(d)" TO ANSI-REFERENCE.              NC2254.2
109900     MOVE    1  TO REC-CT.                                        NC2254.2
110000     MOVE   "*" TO WRK-XN-00001-1.                                NC2254.2
110100     GO TO   EVA-TEST-GF-18-1.                                    NC2254.2
110200 EVA-DELETE-GF-18.                                                NC2254.2
110300     PERFORM DE-LETE.                                             NC2254.2
110400     PERFORM PRINT-DETAIL.                                        NC2254.2
110500     GO TO   EVA-INIT-GF-19.                                      NC2254.2
110600 EVA-TEST-GF-18-1.                                                NC2254.2
110700     MOVE   "EVA-TEST-GF-18-1" TO PAR-NAME.                       NC2254.2
110800     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
110900        WHEN TRUE                                                 NC2254.2
111000             GO TO EVA-FAIL-GF-18-1.                              NC2254.2
111100     PERFORM PASS.                                                NC2254.2
111200     GO TO   EVA-WRITE-GF-18-1.                                   NC2254.2
111300 EVA-DELETE-GF-18-1.                                              NC2254.2
111400     PERFORM DE-LETE.                                             NC2254.2
111500     GO TO EVA-WRITE-GF-18-1.                                     NC2254.2
111600 EVA-FAIL-GF-18-1.                                                NC2254.2
111700     MOVE   "CONDITIONAL EXPRESSION SHOULD BE FALSE"              NC2254.2
111800          TO RE-MARK                                              NC2254.2
111900     PERFORM FAIL.                                                NC2254.2
112000 EVA-WRITE-GF-18-1.                                               NC2254.2
112100     PERFORM PRINT-DETAIL.                                        NC2254.2
112200*                                                                 NC2254.2
112300 EVA-TEST-GF-18-2.                                                NC2254.2
112400     ADD     1 TO REC-CT.                                         NC2254.2
112500     MOVE   "EVA-TEST-GF-18-2" TO PAR-NAME.                       NC2254.2
112600     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
112700        WHEN FALSE                                                NC2254.2
112800             PERFORM PASS                                         NC2254.2
112900             GO TO   EVA-WRITE-GF-18-2.                           NC2254.2
113000     GO TO   EVA-WRITE-GF-18-2.                                   NC2254.2
113100 EVA-DELETE-GF-18-2.                                              NC2254.2
113200     PERFORM DE-LETE.                                             NC2254.2
113300     GO TO EVA-WRITE-GF-18-2.                                     NC2254.2
113400 EVA-FAIL-GF-18-2.                                                NC2254.2
113500     MOVE   "CONDITIONAL EXPRESSION SHOULD BE FALSE"              NC2254.2
113600          TO RE-MARK.                                             NC2254.2
113700     PERFORM FAIL.                                                NC2254.2
113800 EVA-WRITE-GF-18-2.                                               NC2254.2
113900     PERFORM PRINT-DETAIL.                                        NC2254.2
114000*                                                                 NC2254.2
114100 EVA-TEST-GF-18-3.                                                NC2254.2
114200     ADD     1       TO REC-CT.                                   NC2254.2
114300     MOVE   "EVA-TEST-GF-18-3" TO PAR-NAME.                       NC2254.2
114400     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
114500        WHEN ANY                                                  NC2254.2
114600             PERFORM PASS                                         NC2254.2
114700             GO TO   EVA-WRITE-GF-18-3.                           NC2254.2
114800     GO TO   EVA-FAIL-GF-18-3.                                    NC2254.2
114900 EVA-DELETE-GF-18-3.                                              NC2254.2
115000     PERFORM DE-LETE.                                             NC2254.2
115100     GO TO EVA-WRITE-GF-18-3.                                     NC2254.2
115200 EVA-FAIL-GF-18-3.                                                NC2254.2
115300     MOVE   "WHEN 'ANY' SHOULD HAVE EXECUTED" TO RE-MARK          NC2254.2
115400     PERFORM FAIL.                                                NC2254.2
115500 EVA-WRITE-GF-18-3.                                               NC2254.2
115600     PERFORM PRINT-DETAIL.                                        NC2254.2
115700*                                                                 NC2254.2
115800 EVA-INIT-GF-19.                                                  NC2254.2
115900     MOVE   "VI-84 6.12.4 GR1(c)" TO ANSI-REFERENCE.              NC2254.2
116000     MOVE    1  TO REC-CT.                                        NC2254.2
116100     MOVE    9  TO WRK-DU-08V00.                                  NC2254.2
116200     GO TO   EVA-TEST-GF-19-1.                                    NC2254.2
116300 EVA-DELETE-GF-19.                                                NC2254.2
116400     PERFORM DE-LETE.                                             NC2254.2
116500     PERFORM PRINT-DETAIL.                                        NC2254.2
116600     GO TO   EVA-INIT-GF-20.                                      NC2254.2
116700 EVA-TEST-GF-19-1.                                                NC2254.2
116800     MOVE   "EVA-TEST-GF-19-1" TO PAR-NAME.                       NC2254.2
116900     EVALUATE (WRK-DU-08V00 * 9)                                  NC2254.2
117000        WHEN WS-81                                                NC2254.2
117100             PERFORM PASS                                         NC2254.2
117200             GO TO   EVA-WRITE-GF-19-1.                           NC2254.2
117300     GO TO   EVA-FAIL-GF-19-1.                                    NC2254.2
117400 EVA-DELETE-GF-19-1.                                              NC2254.2
117500     PERFORM DE-LETE.                                             NC2254.2
117600     GO TO EVA-WRITE-GF-19-1.                                     NC2254.2
117700 EVA-FAIL-GF-19-1.                                                NC2254.2
117800     MOVE   "SELECTION SUBJECT SHOULD EQUAL IDENTIFIER"           NC2254.2
117900          TO RE-MARK.                                             NC2254.2
118000     PERFORM FAIL.                                                NC2254.2
118100 EVA-WRITE-GF-19-1.                                               NC2254.2
118200     PERFORM PRINT-DETAIL.                                        NC2254.2
118300*                                                                 NC2254.2
118400 EVA-TEST-GF-19-2.                                                NC2254.2
118500     ADD     1       TO REC-CT.                                   NC2254.2
118600     MOVE   "EVA-TEST-GF-19-2" TO PAR-NAME.                       NC2254.2
118700     EVALUATE (WRK-DU-08V00 * 9)                                  NC2254.2
118800        WHEN 81                                                   NC2254.2
118900             PERFORM PASS                                         NC2254.2
119000             GO TO   EVA-WRITE-GF-19-2.                           NC2254.2
119100     GO TO   EVA-FAIL-GF-19-2.                                    NC2254.2
119200 EVA-DELETE-GF-19-2.                                              NC2254.2
119300     PERFORM DE-LETE.                                             NC2254.2
119400     GO TO EVA-WRITE-GF-19-2.                                     NC2254.2
119500 EVA-FAIL-GF-19-2.                                                NC2254.2
119600     MOVE   "SELECTION SUBJECT SHOULD EQUAL LITERAL"              NC2254.2
119700          TO RE-MARK.                                             NC2254.2
119800     PERFORM FAIL.                                                NC2254.2
119900 EVA-WRITE-GF-19-2.                                               NC2254.2
120000     PERFORM PRINT-DETAIL.                                        NC2254.2
120100*                                                                 NC2254.2
120200 EVA-TEST-GF-19-3.                                                NC2254.2
120300     ADD     1       TO REC-CT.                                   NC2254.2
120400     MOVE   "EVA-TEST-GF-19-3" TO PAR-NAME.                       NC2254.2
120500     EVALUATE (WRK-DU-08V00 * 9)                                  NC2254.2
120600        WHEN (9 + 9 + 9 + 9 + 9 + 9 + 9 + 9 + 9)                  NC2254.2
120700             PERFORM PASS                                         NC2254.2
120800             GO TO   EVA-WRITE-GF-19-3.                           NC2254.2
120900     GO TO   EVA-FAIL-GF-19-3.                                    NC2254.2
121000 EVA-DELETE-GF-19-3.                                              NC2254.2
121100     PERFORM DE-LETE.                                             NC2254.2
121200     GO TO EVA-WRITE-GF-19-3.                                     NC2254.2
121300 EVA-FAIL-GF-19-3.                                                NC2254.2
121400     MOVE   "SELECTION SUBJECT SHOULD EQUAL ARITHMETIC EXPRESSION"NC2254.2
121500          TO RE-MARK.                                             NC2254.2
121600     PERFORM FAIL.                                                NC2254.2
121700 EVA-WRITE-GF-19-3.                                               NC2254.2
121800     PERFORM PRINT-DETAIL.                                        NC2254.2
121900*                                                                 NC2254.2
122000 EVA-INIT-GF-20.                                                  NC2254.2
122100     MOVE   "VI-84 6.12.4 GR1(c)" TO ANSI-REFERENCE.              NC2254.2
122200     MOVE    1  TO REC-CT.                                        NC2254.2
122300     MOVE    8  TO WRK-DU-08V00.                                  NC2254.2
122400     GO TO   EVA-TEST-GF-20-1.                                    NC2254.2
122500 EVA-DELETE-GF-20.                                                NC2254.2
122600     PERFORM DE-LETE.                                             NC2254.2
122700     PERFORM PRINT-DETAIL.                                        NC2254.2
122800     GO TO   EVA-INIT-GF-21.                                      NC2254.2
122900 EVA-TEST-GF-20-1.                                                NC2254.2
123000     MOVE   "EVA-TEST-GF-20-1" TO PAR-NAME.                       NC2254.2
123100     EVALUATE (WRK-DU-08V00 * 9)                                  NC2254.2
123200        WHEN WS-81                                                NC2254.2
123300             GO TO EVA-FAIL-GF-20-1.                              NC2254.2
123400     PERFORM PASS.                                                NC2254.2
123500     GO TO   EVA-WRITE-GF-20-1.                                   NC2254.2
123600 EVA-DELETE-GF-20-1.                                              NC2254.2
123700     PERFORM DE-LETE.                                             NC2254.2
123800     GO TO EVA-WRITE-GF-20-1.                                     NC2254.2
123900 EVA-FAIL-GF-20-1.                                                NC2254.2
124000     MOVE "SELECTION SUBJECT SHOULD NOT EQUAL IDENTIFIER"         NC2254.2
124100          TO RE-MARK                                              NC2254.2
124200     PERFORM FAIL.                                                NC2254.2
124300 EVA-WRITE-GF-20-1.                                               NC2254.2
124400     PERFORM PRINT-DETAIL.                                        NC2254.2
124500*                                                                 NC2254.2
124600 EVA-TEST-GF-20-2.                                                NC2254.2
124700     ADD     1 TO REC-CT.                                         NC2254.2
124800     MOVE   "EVA-TEST-GF-20-2" TO PAR-NAME.                       NC2254.2
124900     EVALUATE (WRK-DU-08V00 * 9)                                  NC2254.2
125000        WHEN 81                                                   NC2254.2
125100             GO TO EVA-FAIL-GF-20-2.                              NC2254.2
125200     PERFORM PASS.                                                NC2254.2
125300     GO TO   EVA-WRITE-GF-20-2.                                   NC2254.2
125400 EVA-DELETE-GF-20-2.                                              NC2254.2
125500     PERFORM DE-LETE.                                             NC2254.2
125600     GO TO EVA-WRITE-GF-20-2.                                     NC2254.2
125700 EVA-FAIL-GF-20-2.                                                NC2254.2
125800     MOVE   "SELECTION SUBJECT SHOULD NOT EQUAL LITERAL"          NC2254.2
125900          TO RE-MARK                                              NC2254.2
126000     PERFORM FAIL.                                                NC2254.2
126100 EVA-WRITE-GF-20-2.                                               NC2254.2
126200     PERFORM PRINT-DETAIL.                                        NC2254.2
126300                                                                  NC2254.2
126400 EVA-TEST-GF-20-3.                                                NC2254.2
126500     ADD     1       TO REC-CT.                                   NC2254.2
126600     MOVE   "EVA-TEST-GF-20-3" TO PAR-NAME.                       NC2254.2
126700     EVALUATE (WRK-DU-08V00 * 9)                                  NC2254.2
126800        WHEN (9 + 9 + 9 + 9 + 9 + 9 + 9 + 9 + 9)                  NC2254.2
126900             GO TO EVA-FAIL-GF-20-3.                              NC2254.2
127000     PERFORM PASS.                                                NC2254.2
127100     GO TO   EVA-WRITE-GF-20-3.                                   NC2254.2
127200 EVA-DELETE-GF-20-3.                                              NC2254.2
127300     PERFORM DE-LETE.                                             NC2254.2
127400     GO TO EVA-WRITE-GF-20-3.                                     NC2254.2
127500 EVA-FAIL-GF-20-3.                                                NC2254.2
127600     MOVE                                                         NC2254.2
127700     "SELECTION SUBJECT SHOULD NOT = ARITHMETIC EXPRESSION"       NC2254.2
127800          TO RE-MARK                                              NC2254.2
127900     PERFORM FAIL.                                                NC2254.2
128000 EVA-WRITE-GF-20-3.                                               NC2254.2
128100     PERFORM PRINT-DETAIL.                                        NC2254.2
128200*                                                                 NC2254.2
128300 EVA-INIT-GF-21.                                                  NC2254.2
128400     MOVE   "VI-84 6.12.4 GR1(e)" TO ANSI-REFERENCE.              NC2254.2
128500     MOVE    1  TO REC-CT.                                        NC2254.2
128600     MOVE    SPACE TO WRK-XN-00001-1.                             NC2254.2
128700     GO TO   EVA-TEST-GF-21-1.                                    NC2254.2
128800 EVA-DELETE-GF-21.                                                NC2254.2
128900     PERFORM DE-LETE.                                             NC2254.2
129000     PERFORM PRINT-DETAIL.                                        NC2254.2
129100     GO TO   EVA-INIT-GF-22.                                      NC2254.2
129200 EVA-TEST-GF-21-1.                                                NC2254.2
129300     MOVE   "EVA-TEST-GF-21-1" TO PAR-NAME.                       NC2254.2
129400     EVALUATE TRUE                                                NC2254.2
129500        WHEN WRK-XN-00001-1 = SPACE                               NC2254.2
129600             PERFORM PASS                                         NC2254.2
129700             GO TO   EVA-WRITE-GF-21-1.                           NC2254.2
129800     GO TO   EVA-FAIL-GF-21-1.                                    NC2254.2
129900 EVA-DELETE-GF-21-1.                                              NC2254.2
130000     PERFORM DE-LETE.                                             NC2254.2
130100     GO TO EVA-WRITE-GF-21-1.                                     NC2254.2
130200 EVA-FAIL-GF-21-1.                                                NC2254.2
130300     MOVE   "SELECTION OBJECT CONDITION SHOULD BE TRUE"           NC2254.2
130400          TO RE-MARK.                                             NC2254.2
130500     PERFORM FAIL.                                                NC2254.2
130600 EVA-WRITE-GF-21-1.                                               NC2254.2
130700     PERFORM PRINT-DETAIL.                                        NC2254.2
130800*                                                                 NC2254.2
130900 EVA-INIT-GF-22.                                                  NC2254.2
131000     MOVE   "VI-84 6.12.4 GR1(e)" TO ANSI-REFERENCE.              NC2254.2
131100     MOVE    1  TO REC-CT.                                        NC2254.2
131200     MOVE   "#" TO WRK-XN-00001-1.                                NC2254.2
131300     GO TO   EVA-TEST-GF-22-1.                                    NC2254.2
131400 EVA-DELETE-GF-22.                                                NC2254.2
131500     PERFORM DE-LETE.                                             NC2254.2
131600     PERFORM PRINT-DETAIL.                                        NC2254.2
131700     GO TO   EVA-INIT-GF-23.                                      NC2254.2
131800 EVA-TEST-GF-22-1.                                                NC2254.2
131900     MOVE   "EVA-TEST-GF-22-1" TO PAR-NAME.                       NC2254.2
132000     EVALUATE TRUE                                                NC2254.2
132100        WHEN WRK-XN-00001-1 = SPACE                               NC2254.2
132200             GO TO EVA-FAIL-GF-22-1.                              NC2254.2
132300     PERFORM PASS.                                                NC2254.2
132400     GO TO   EVA-WRITE-GF-22-1.                                   NC2254.2
132500 EVA-DELETE-GF-22-1.                                              NC2254.2
132600     PERFORM DE-LETE.                                             NC2254.2
132700     GO TO EVA-WRITE-GF-22-1.                                     NC2254.2
132800 EVA-FAIL-GF-22-1.                                                NC2254.2
132900     MOVE   "SELECTION OBJECT CONDITION SHOULD BE FALSE"          NC2254.2
133000          TO RE-MARK                                              NC2254.2
133100     PERFORM FAIL.                                                NC2254.2
133200 EVA-WRITE-GF-22-1.                                               NC2254.2
133300     PERFORM PRINT-DETAIL.                                        NC2254.2
133400*                                                                 NC2254.2
133500 EVA-INIT-GF-23.                                                  NC2254.2
133600     MOVE   "VI-84 6.12.4 GR1(e)" TO ANSI-REFERENCE.              NC2254.2
133700     MOVE    1  TO REC-CT.                                        NC2254.2
133800     MOVE    SPACE TO WRK-XN-00001-1.                             NC2254.2
133900     GO TO   EVA-TEST-GF-23-1.                                    NC2254.2
134000 EVA-DELETE-GF-23.                                                NC2254.2
134100     PERFORM DE-LETE.                                             NC2254.2
134200     PERFORM PRINT-DETAIL.                                        NC2254.2
134300     GO TO   EVA-INIT-GF-24.                                      NC2254.2
134400 EVA-TEST-GF-23-1.                                                NC2254.2
134500     MOVE   "EVA-TEST-GF-23-1" TO PAR-NAME.                       NC2254.2
134600     EVALUATE FALSE                                               NC2254.2
134700        WHEN WRK-XN-00001-1 = SPACE                               NC2254.2
134800             GO TO EVA-FAIL-GF-23-1.                              NC2254.2
134900     PERFORM PASS.                                                NC2254.2
135000     GO TO   EVA-WRITE-GF-23-1.                                   NC2254.2
135100 EVA-DELETE-GF-23-1.                                              NC2254.2
135200     PERFORM DE-LETE.                                             NC2254.2
135300     GO TO EVA-WRITE-GF-23-1.                                     NC2254.2
135400 EVA-FAIL-GF-23-1.                                                NC2254.2
135500     MOVE   "SELECTION OBJECT CONDITION SHOULD BE TRUE"           NC2254.2
135600          TO RE-MARK                                              NC2254.2
135700     PERFORM FAIL.                                                NC2254.2
135800 EVA-WRITE-GF-23-1.                                               NC2254.2
135900     PERFORM PRINT-DETAIL.                                        NC2254.2
136000*                                                                 NC2254.2
136100 EVA-INIT-GF-24.                                                  NC2254.2
136200     MOVE   "VI-84 6.12.4 GR1(e)" TO ANSI-REFERENCE.              NC2254.2
136300     MOVE    1  TO REC-CT.                                        NC2254.2
136400     MOVE   "#" TO WRK-XN-00001-1.                                NC2254.2
136500     GO TO   EVA-TEST-GF-24-1.                                    NC2254.2
136600 EVA-DELETE-GF-24.                                                NC2254.2
136700     PERFORM DE-LETE.                                             NC2254.2
136800     PERFORM PRINT-DETAIL.                                        NC2254.2
136900     GO TO   EVA-INIT-GF-25.                                      NC2254.2
137000 EVA-TEST-GF-24-1.                                                NC2254.2
137100     MOVE   "EVA-TEST-GF-24-1" TO PAR-NAME.                       NC2254.2
137200     EVALUATE FALSE                                               NC2254.2
137300        WHEN WRK-XN-00001-1 = SPACE                               NC2254.2
137400             PERFORM PASS                                         NC2254.2
137500             GO TO   EVA-WRITE-GF-24-1.                           NC2254.2
137600     GO TO   EVA-FAIL-GF-24-1.                                    NC2254.2
137700 EVA-DELETE-GF-24-1.                                              NC2254.2
137800     PERFORM DE-LETE.                                             NC2254.2
137900     GO TO EVA-WRITE-GF-24-1.                                     NC2254.2
138000 EVA-FAIL-GF-24-1.                                                NC2254.2
138100     MOVE   "SELECTION OBJECT CONDITION SHOULD BE FALSE"          NC2254.2
138200          TO RE-MARK.                                             NC2254.2
138300     PERFORM FAIL.                                                NC2254.2
138400 EVA-WRITE-GF-24-1.                                               NC2254.2
138500     PERFORM PRINT-DETAIL.                                        NC2254.2
138600*                                                                 NC2254.2
138700 EVA-INIT-GF-25.                                                  NC2254.2
138800     MOVE   "VI-84 6.12.4 GR3(b)" TO ANSI-REFERENCE.              NC2254.2
138900     MOVE    1  TO REC-CT.                                        NC2254.2
139000     MOVE    26 TO WRK-DU-08V00.                                  NC2254.2
139100     GO TO   EVA-TEST-GF-25-1.                                    NC2254.2
139200 EVA-DELETE-GF-25.                                                NC2254.2
139300     PERFORM DE-LETE.                                             NC2254.2
139400     PERFORM PRINT-DETAIL.                                        NC2254.2
139500     GO TO   EVA-INIT-GF-16.                                      NC2254.2
139600 EVA-TEST-GF-25-1.                                                NC2254.2
139700     MOVE   "EVA-TEST-GF-25-1" TO PAR-NAME.                       NC2254.2
139800     EVALUATE 26                                                  NC2254.2
139900        WHEN WRK-DU-08V00                                         NC2254.2
140000             PERFORM PASS                                         NC2254.2
140100        WHEN OTHER                                                NC2254.2
140200             GO TO EVA-FAIL-GF-25-1.                              NC2254.2
140300     GO TO EVA-WRITE-GF-25-1.                                     NC2254.2
140400 EVA-DELETE-GF-25-1.                                              NC2254.2
140500     PERFORM DE-LETE.                                             NC2254.2
140600     GO TO EVA-WRITE-GF-25-1.                                     NC2254.2
140700 EVA-FAIL-GF-25-1.                                                NC2254.2
140800     MOVE   "IDENTIFIER AND LITERAL SHOULD BE EQUAL"              NC2254.2
140900          TO RE-MARK                                              NC2254.2
141000     PERFORM FAIL.                                                NC2254.2
141100 EVA-WRITE-GF-25-1.                                               NC2254.2
141200     PERFORM PRINT-DETAIL.                                        NC2254.2
141300*                                                                 NC2254.2
141400 EVA-INIT-GF-26.                                                  NC2254.2
141500     MOVE   "VI-84 6.12.4 GR3(b)" TO ANSI-REFERENCE.              NC2254.2
141600     MOVE    1  TO REC-CT.                                        NC2254.2
141700     MOVE    78 TO WRK-DU-08V00.                                  NC2254.2
141800     GO TO   EVA-TEST-GF-26-1.                                    NC2254.2
141900 EVA-DELETE-GF-26.                                                NC2254.2
142000     PERFORM DE-LETE.                                             NC2254.2
142100     PERFORM PRINT-DETAIL.                                        NC2254.2
142200     GO TO   EVA-INIT-GF-27.                                      NC2254.2
142300 EVA-TEST-GF-26-1.                                                NC2254.2
142400     MOVE   "EVA-TEST-GF-26-1" TO PAR-NAME.                       NC2254.2
142500     EVALUATE 1234                                                NC2254.2
142600        WHEN WRK-DU-08V00                                         NC2254.2
142700             GO TO EVA-FAIL-GF-26-1                               NC2254.2
142800        WHEN OTHER                                                NC2254.2
142900             PERFORM PASS.                                        NC2254.2
143000     GO TO EVA-WRITE-GF-26-1.                                     NC2254.2
143100 EVA-DELETE-GF-26-1.                                              NC2254.2
143200     PERFORM DE-LETE.                                             NC2254.2
143300     GO TO EVA-WRITE-GF-26-1.                                     NC2254.2
143400 EVA-FAIL-GF-26-1.                                                NC2254.2
143500     MOVE   "IDENTIFIER AND LITERAL SHOULD NOT BE EQUAL"          NC2254.2
143600          TO RE-MARK                                              NC2254.2
143700     PERFORM FAIL.                                                NC2254.2
143800 EVA-WRITE-GF-26-1.                                               NC2254.2
143900     PERFORM PRINT-DETAIL.                                        NC2254.2
144000*                                                                 NC2254.2
144100 EVA-INIT-GF-27.                                                  NC2254.2
144200     MOVE   "VI-84 6.12.4 GR3(c)" TO ANSI-REFERENCE.              NC2254.2
144300     MOVE    1  TO REC-CT.                                        NC2254.2
144400     MOVE    26 TO WRK-DU-08V00.                                  NC2254.2
144500     GO TO   EVA-TEST-GF-27-1.                                    NC2254.2
144600 EVA-DELETE-GF-27.                                                NC2254.2
144700     PERFORM DE-LETE.                                             NC2254.2
144800     PERFORM PRINT-DETAIL.                                        NC2254.2
144900     GO TO   EVA-INIT-GF-28.                                      NC2254.2
145000 EVA-TEST-GF-27-1.                                                NC2254.2
145100     MOVE   "EVA-TEST-GF-27-1" TO PAR-NAME.                       NC2254.2
145200     EVALUATE 26                                                  NC2254.2
145300        WHEN NOT WRK-DU-08V00                                     NC2254.2
145400             GO TO   EVA-FAIL-GF-27-1                             NC2254.2
145500     END-EVALUATE.                                                NC2254.2
145600     PERFORM PASS.                                                NC2254.2
145700     GO TO   EVA-WRITE-GF-27-1.                                   NC2254.2
145800 EVA-DELETE-GF-27-1.                                              NC2254.2
145900     PERFORM DE-LETE.                                             NC2254.2
146000     GO TO EVA-WRITE-GF-27-1.                                     NC2254.2
146100 EVA-FAIL-GF-27-1.                                                NC2254.2
146200     MOVE   "IDENTIFIER AND LITERAL SHOULD BE EQUAL"              NC2254.2
146300          TO RE-MARK                                              NC2254.2
146400     PERFORM FAIL.                                                NC2254.2
146500 EVA-WRITE-GF-27-1.                                               NC2254.2
146600     PERFORM PRINT-DETAIL.                                        NC2254.2
146700*                                                                 NC2254.2
146800 EVA-INIT-GF-28.                                                  NC2254.2
146900     MOVE   "VI-84 6.12.4 GR3(c)" TO ANSI-REFERENCE.              NC2254.2
147000     MOVE    1  TO REC-CT.                                        NC2254.2
147100     MOVE    78 TO WRK-DU-08V00.                                  NC2254.2
147200     GO TO   EVA-TEST-GF-28-1.                                    NC2254.2
147300 EVA-DELETE-GF-28.                                                NC2254.2
147400     PERFORM DE-LETE.                                             NC2254.2
147500     PERFORM PRINT-DETAIL.                                        NC2254.2
147600     GO TO   EVA-INIT-GF-29.                                      NC2254.2
147700 EVA-TEST-GF-28-1.                                                NC2254.2
147800     MOVE   "EVA-TEST-GF-28-1" TO PAR-NAME.                       NC2254.2
147900     EVALUATE 1234                                                NC2254.2
148000        WHEN NOT WRK-DU-08V00                                     NC2254.2
148100             PERFORM PASS                                         NC2254.2
148200             GO TO   EVA-WRITE-GF-28-1                            NC2254.2
148300     end-evaluate.                                                NC2254.2
148400     GO TO   EVA-FAIL-GF-28-1.                                    NC2254.2
148500 EVA-DELETE-GF-28-1.                                              NC2254.2
148600     PERFORM DE-LETE.                                             NC2254.2
148700     GO TO EVA-WRITE-GF-28-1.                                     NC2254.2
148800 EVA-FAIL-GF-28-1.                                                NC2254.2
148900     MOVE   "IDENTIFIER AND LITERAL SHOULD NOT BE EQUAL"          NC2254.2
149000          TO RE-MARK.                                             NC2254.2
149100     PERFORM FAIL.                                                NC2254.2
149200 EVA-WRITE-GF-28-1.                                               NC2254.2
149300     PERFORM PRINT-DETAIL.                                        NC2254.2
149400*                                                                 NC2254.2
149500 EVA-INIT-GF-29.                                                  NC2254.2
149600     MOVE   "VI-84 6.12.4 GR3" TO ANSI-REFERENCE.                 NC2254.2
149700     MOVE    1  TO REC-CT.                                        NC2254.2
149800     MOVE    8  TO WRK-XN-00001-1.                                NC2254.2
149900     GO TO   EVA-TEST-GF-29-1.                                    NC2254.2
150000 EVA-DELETE-GF-29.                                                NC2254.2
150100     PERFORM DE-LETE.                                             NC2254.2
150200     PERFORM PRINT-DETAIL.                                        NC2254.2
150300     GO TO   EVA-INIT-GF-30.                                      NC2254.2
150400 EVA-TEST-GF-29-1.                                                NC2254.2
150500     MOVE   "EVA-TEST-GF-29-1" TO PAR-NAME.                       NC2254.2
150600     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
150700        WHEN TRUE                                                 NC2254.2
150800             PERFORM PASS                                         NC2254.2
150900        WHEN OTHER                                                NC2254.2
151000             GO TO   EVA-FAIL-GF-29-1                             NC2254.2
151100     END-EVALUATE                                                 NC2254.2
151200     GO TO   EVA-WRITE-GF-29-1.                                   NC2254.2
151300 EVA-DELETE-GF-29-1.                                              NC2254.2
151400     PERFORM DE-LETE.                                             NC2254.2
151500     GO TO EVA-WRITE-GF-29-1.                                     NC2254.2
151600 EVA-FAIL-GF-29-1.                                                NC2254.2
151700     MOVE   "CONDITIONAL EXPRESSION SHOULD BE TRUE"               NC2254.2
151800          TO RE-MARK                                              NC2254.2
151900     PERFORM FAIL.                                                NC2254.2
152000 EVA-WRITE-GF-29-1.                                               NC2254.2
152100     PERFORM PRINT-DETAIL.                                        NC2254.2
152200*                                                                 NC2254.2
152300 EVA-INIT-GF-30.                                                  NC2254.2
152400     MOVE   "VI-84 6.12.4 GR3" TO ANSI-REFERENCE.                 NC2254.2
152500     MOVE    1  TO REC-CT.                                        NC2254.2
152600     MOVE   "*" TO WRK-XN-00001-1.                                NC2254.2
152700     GO TO   EVA-TEST-GF-30-1.                                    NC2254.2
152800 EVA-DELETE-GF-30.                                                NC2254.2
152900     PERFORM DE-LETE.                                             NC2254.2
153000     PERFORM PRINT-DETAIL.                                        NC2254.2
153100     GO TO   EVA-INIT-GF-31.                                      NC2254.2
153200 EVA-TEST-GF-30-1.                                                NC2254.2
153300     MOVE   "EVA-TEST-GF-30-1" TO PAR-NAME.                       NC2254.2
153400     EVALUATE WRK-XN-00001-1 NUMERIC                              NC2254.2
153500        WHEN TRUE                                                 NC2254.2
153600             GO TO EVA-FAIL-GF-30-1                               NC2254.2
153700        WHEN OTHER                                                NC2254.2
153800             PERFORM PASS                                         NC2254.2
153900     END-EVALUATE                                                 NC2254.2
154000     GO TO   EVA-WRITE-GF-30-1.                                   NC2254.2
154100 EVA-DELETE-GF-30-1.                                              NC2254.2
154200     PERFORM DE-LETE.                                             NC2254.2
154300     GO TO EVA-WRITE-GF-30-1.                                     NC2254.2
154400 EVA-FAIL-GF-30-1.                                                NC2254.2
154500     MOVE   "CONDITIONAL EXPRESSION SHOULD BE FALSE"              NC2254.2
154600          TO RE-MARK                                              NC2254.2
154700     PERFORM FAIL.                                                NC2254.2
154800 EVA-WRITE-GF-30-1.                                               NC2254.2
154900     PERFORM PRINT-DETAIL.                                        NC2254.2
155000*                                                                 NC2254.2
155100 EVA-INIT-GF-31.                                                  NC2254.2
155200     MOVE   "VI-84 6.12.4 GR3" TO ANSI-REFERENCE.                 NC2254.2
155300     MOVE   "EVA-TEST-GF-31-1"   TO PAR-NAME.                     NC2254.2
155400     MOVE    1   TO REC-CT.                                       NC2254.2
155500     MOVE    81  TO WRK-DU-08V00.                                 NC2254.2
155600     MOVE   "*"  TO WRK-XN-00001-1.                               NC2254.2
155700     MOVE    "*" TO WRK-XN-00001-2.                               NC2254.2
155800     MOVE    987 TO WRK-DU-08V00-1.                               NC2254.2
155900     MOVE    81  TO WRK-DU-08V00-2.                               NC2254.2
156000     MOVE    0   TO WRK-DU-08V00-3.                               NC2254.2
156100     MOVE    567 TO WRK-DU-08V00-4.                               NC2254.2
156200     GO TO   EVA-TEST-GF-31-0.                                    NC2254.2
156300 EVA-DELETE-GF-31.                                                NC2254.2
156400     PERFORM DE-LETE.                                             NC2254.2
156500     PERFORM PRINT-DETAIL.                                        NC2254.2
156600     GO TO   EVA-INIT-GF-32.                                      NC2254.2
156700 EVA-TEST-GF-31-0.                                                NC2254.2
156800     EVALUATE     WRK-DU-08V00                                    NC2254.2
156900             ALSO 81                                              NC2254.2
157000             ALSO (WRK-DU-08V00 * 9)                              NC2254.2
157100             ALSO IT-IS-81                                        NC2254.2
157200             ALSO TRUE                                            NC2254.2
157300             ALSO FALSE                                           NC2254.2
157400        WHEN NOT  WRK-DU-08V00-1                                  NC2254.2
157500             ALSO WRK-DU-08V00-2                                  NC2254.2
157600             ALSO 729                                             NC2254.2
157700             ALSO TRUE                                            NC2254.2
157800             ALSO WRK-DU-08V00-3 = 0                              NC2254.2
157900             ALSO WRK-DU-08V00-4 < 9                              NC2254.2
158000                  MOVE  "A" TO WRK-XN-00001-1                     NC2254.2
158100                  MOVE  "B" TO WRK-XN-00001-2                     NC2254.2
158200        WHEN      81                                              NC2254.2
158300             ALSO WRK-DU-08V00                                    NC2254.2
158400             ALSO (9 * 9 * 9)                                     NC2254.2
158500             ALSO FALSE                                           NC2254.2
158600             ALSO WRK-XN-00001-2 = "*"                            NC2254.2
158700             ALSO WRK-DU-08V00 > 8                                NC2254.2
158800                  MOVE  "C" TO WRK-XN-00001-1                     NC2254.2
158900                  MOVE  "D" TO WRK-XN-00001-2                     NC2254.2
159000        WHEN      ANY                                             NC2254.2
159100             ALSO ANY                                             NC2254.2
159200             ALSO ANY                                             NC2254.2
159300             ALSO ANY                                             NC2254.2
159400             ALSO ANY                                             NC2254.2
159500             ALSO WRK-DU-08V00 = 6                                NC2254.2
159600                  MOVE  "E" TO WRK-XN-00001-1                     NC2254.2
159700                  MOVE  "F" TO WRK-XN-00001-2                     NC2254.2
159800        WHEN      OTHER                                           NC2254.2
159900                  MOVE  "G" TO WRK-XN-00001-1                     NC2254.2
160000                  MOVE  "H" TO WRK-XN-00001-2                     NC2254.2
160100     END-EVALUATE.                                                NC2254.2
160200 EVA-TEST-GF-31-1.                                                NC2254.2
160300     IF      WRK-XN-00001-1 = "A"                                 NC2254.2
160400             PERFORM PASS                                         NC2254.2
160500             GO TO EVA-WRITE-GF-31-1                              NC2254.2
160600     ELSE                                                         NC2254.2
160700             GO TO EVA-FAIL-GF-31-1.                              NC2254.2
160800 EVA-DELETE-GF-31-1.                                              NC2254.2
160900     PERFORM DE-LETE.                                             NC2254.2
161000     GO TO EVA-WRITE-GF-31-1.                                     NC2254.2
161100 EVA-FAIL-GF-31-1.                                                NC2254.2
161200     MOVE   "EVALUATE FAILURE" TO RE-MARK                         NC2254.2
161300     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2254.2
161400     MOVE   "A"             TO CORRECT-X                          NC2254.2
161500     PERFORM FAIL.                                                NC2254.2
161600 EVA-WRITE-GF-31-1.                                               NC2254.2
161700     PERFORM PRINT-DETAIL.                                        NC2254.2
161800 EVA-TEST-GF-31-2.                                                NC2254.2
161900     IF      WRK-XN-00001-2 = "B"                                 NC2254.2
162000             PERFORM PASS                                         NC2254.2
162100             GO TO EVA-WRITE-GF-31-2                              NC2254.2
162200     ELSE                                                         NC2254.2
162300             GO TO EVA-FAIL-GF-31-2.                              NC2254.2
162400 EVA-DELETE-GF-31-2.                                              NC2254.2
162500     PERFORM DE-LETE.                                             NC2254.2
162600     GO TO EVA-WRITE-GF-31-2.                                     NC2254.2
162700 EVA-FAIL-GF-31-2.                                                NC2254.2
162800     MOVE   "EVALUATE FAILURE" TO RE-MARK                         NC2254.2
162900     MOVE    WRK-XN-00001-2 TO COMPUTED-X                         NC2254.2
163000     MOVE   "B"             TO CORRECT-X                          NC2254.2
163100     PERFORM FAIL.                                                NC2254.2
163200 EVA-WRITE-GF-31-2.                                               NC2254.2
163300     PERFORM PRINT-DETAIL.                                        NC2254.2
163400*                                                                 NC2254.2
163500 EVA-INIT-GF-32.                                                  NC2254.2
163600     MOVE   "VI-84 6.12.4 GR3" TO ANSI-REFERENCE.                 NC2254.2
163700     MOVE   "EVA-TEST-GF-32-1"   TO PAR-NAME.                     NC2254.2
163800     MOVE    1   TO REC-CT.                                       NC2254.2
163900     MOVE    81  TO WRK-DU-08V00.                                 NC2254.2
164000     MOVE   "*"  TO WRK-XN-00001-1.                               NC2254.2
164100     MOVE    987 TO WRK-DU-08V00-1.                               NC2254.2
164200     MOVE    7   TO WRK-DU-08V00-2.                               NC2254.2
164300     MOVE    0   TO WRK-DU-08V00-3.                               NC2254.2
164400     MOVE    567 TO WRK-DU-08V00-4.                               NC2254.2
164500     GO TO   EVA-TEST-GF-32-0.                                    NC2254.2
164600 EVA-DELETE-GF-32.                                                NC2254.2
164700     PERFORM DE-LETE.                                             NC2254.2
164800     PERFORM PRINT-DETAIL.                                        NC2254.2
164900     GO TO   EVA-INIT-GF-33.                                      NC2254.2
165000 EVA-TEST-GF-32-0.                                                NC2254.2
165100     EVALUATE     WRK-DU-08V00                                    NC2254.2
165200             ALSO 81                                              NC2254.2
165300             ALSO (WRK-DU-08V00 * 2)                              NC2254.2
165400             ALSO IT-IS-81                                        NC2254.2
165500             ALSO TRUE                                            NC2254.2
165600             ALSO FALSE                                           NC2254.2
165700        WHEN NOT  WRK-DU-08V00-1                                  NC2254.2
165800             ALSO WRK-DU-08V00-2                                  NC2254.2
165900             ALSO 81                                              NC2254.2
166000             ALSO TRUE                                            NC2254.2
166100             ALSO WRK-DU-08V00-3 = 0                              NC2254.2
166200             ALSO WRK-DU-08V00-4 < 9                              NC2254.2
166300                  MOVE  "A" TO WRK-XN-00001-1                     NC2254.2
166400                  MOVE  "B" TO WRK-XN-00001-2                     NC2254.2
166500        WHEN      81                                              NC2254.2
166600             ALSO WRK-DU-08V00-3 THROUGH WRK-DU-08V00-4           NC2254.2
166700             ALSO (WRK-DU-08V00-2 * 8) THRU (WRK-DU-08V00-2 * 30) NC2254.2
166800             ALSO FALSE                                           NC2254.2
166900             ALSO WRK-DU-08V00-2 = 7                              NC2254.2
167000             ALSO WRK-DU-08V00 > 88                               NC2254.2
167100                  MOVE  "C" TO WRK-XN-00001-1                     NC2254.2
167200                  MOVE  "D" TO WRK-XN-00001-2                     NC2254.2
167300        WHEN      ANY                                             NC2254.2
167400             ALSO ANY                                             NC2254.2
167500             ALSO ANY                                             NC2254.2
167600             ALSO ANY                                             NC2254.2
167700             ALSO ANY                                             NC2254.2
167800             ALSO WRK-DU-08V00 = 6                                NC2254.2
167900                  MOVE  "E" TO WRK-XN-00001-1                     NC2254.2
168000                  MOVE  "F" TO WRK-XN-00001-2                     NC2254.2
168100        WHEN      OTHER                                           NC2254.2
168200                  MOVE  "G" TO WRK-XN-00001-1                     NC2254.2
168300                  MOVE  "H" TO WRK-XN-00001-2                     NC2254.2
168400     END-EVALUATE.                                                NC2254.2
168500 EVA-TEST-GF-32-1.                                                NC2254.2
168600     IF      WRK-XN-00001-1 = "C"                                 NC2254.2
168700             PERFORM PASS                                         NC2254.2
168800             GO TO EVA-WRITE-GF-32-1                              NC2254.2
168900     ELSE                                                         NC2254.2
169000             GO TO EVA-FAIL-GF-32-1.                              NC2254.2
169100 EVA-DELETE-GF-32-1.                                              NC2254.2
169200     PERFORM DE-LETE.                                             NC2254.2
169300     GO TO EVA-WRITE-GF-32-1.                                     NC2254.2
169400 EVA-FAIL-GF-32-1.                                                NC2254.2
169500     MOVE   "EVALUATE FAILURE" TO RE-MARK                         NC2254.2
169600     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2254.2
169700     MOVE   "C"             TO CORRECT-X                          NC2254.2
169800     PERFORM FAIL.                                                NC2254.2
169900 EVA-WRITE-GF-32-1.                                               NC2254.2
170000     PERFORM PRINT-DETAIL.                                        NC2254.2
170100 EVA-TEST-GF-32-2.                                                NC2254.2
170200     ADD     1 TO REC-CT.                                         NC2254.2
170300     IF      WRK-XN-00001-2 = "D"                                 NC2254.2
170400             PERFORM PASS                                         NC2254.2
170500             GO TO EVA-WRITE-GF-32-2                              NC2254.2
170600     ELSE                                                         NC2254.2
170700             GO TO EVA-FAIL-GF-32-2.                              NC2254.2
170800 EVA-DELETE-GF-32-2.                                              NC2254.2
170900     PERFORM DE-LETE.                                             NC2254.2
171000     GO TO EVA-WRITE-GF-32-2.                                     NC2254.2
171100 EVA-FAIL-GF-32-2.                                                NC2254.2
171200     MOVE   "EVALUATE FAILURE" TO RE-MARK                         NC2254.2
171300     MOVE    WRK-XN-00001-2 TO COMPUTED-X                         NC2254.2
171400     MOVE   "D"             TO CORRECT-X                          NC2254.2
171500     PERFORM FAIL.                                                NC2254.2
171600 EVA-WRITE-GF-32-2.                                               NC2254.2
171700     PERFORM PRINT-DETAIL.                                        NC2254.2
171800*                                                                 NC2254.2
171900 EVA-INIT-GF-33.                                                  NC2254.2
172000     MOVE   "VI-84 6.12.4 GR3" TO ANSI-REFERENCE.                 NC2254.2
172100     MOVE   "EVA-TEST-GF-33-1"   TO PAR-NAME.                     NC2254.2
172200     MOVE    1   TO REC-CT.                                       NC2254.2
172300     MOVE    6   TO WRK-DU-08V00.                                 NC2254.2
172400     MOVE   "*"  TO WRK-XN-00001-1.                               NC2254.2
172500     MOVE    987 TO WRK-DU-08V00-1.                               NC2254.2
172600     MOVE    7   TO WRK-DU-08V00-2.                               NC2254.2
172700     MOVE    0   TO WRK-DU-08V00-3.                               NC2254.2
172800     MOVE    567 TO WRK-DU-08V00-4.                               NC2254.2
172900     GO TO   EVA-TEST-GF-33-0.                                    NC2254.2
173000 EVA-DELETE-GF-33.                                                NC2254.2
173100     PERFORM DE-LETE.                                             NC2254.2
173200     PERFORM PRINT-DETAIL.                                        NC2254.2
173300     GO TO   EVA-INIT-GF-34.                                      NC2254.2
173400 EVA-TEST-GF-33-0.                                                NC2254.2
173500     EVALUATE     WRK-DU-08V00                                    NC2254.2
173600             ALSO 81                                              NC2254.2
173700             ALSO (WRK-DU-08V00-2 * 9)                            NC2254.2
173800             ALSO IT-IS-81                                        NC2254.2
173900             ALSO TRUE                                            NC2254.2
174000             ALSO FALSE                                           NC2254.2
174100        WHEN NOT  WRK-DU-08V00-1                                  NC2254.2
174200             ALSO WRK-DU-08V00-2                                  NC2254.2
174300             ALSO 81                                              NC2254.2
174400             ALSO TRUE                                            NC2254.2
174500             ALSO WRK-DU-08V00-3 = 0                              NC2254.2
174600             ALSO WRK-DU-08V00-4 < 9                              NC2254.2
174700                  MOVE  "A" TO WRK-XN-00001-1                     NC2254.2
174800                  MOVE  "B" TO WRK-XN-00001-2                     NC2254.2
174900        WHEN      81                                              NC2254.2
175000             ALSO WRK-DU-08V00-3 THROUGH WRK-DU-08V00-4           NC2254.2
175100             ALSO (WRK-DU-08V00-2 * 7) THRU (WRK-DU-08V00-2 * 8)  NC2254.2
175200             ALSO FALSE                                           NC2254.2
175300             ALSO WRK-DU-08V00-2 = 81                             NC2254.2
175400             ALSO WRK-DU-08V00 > 8                                NC2254.2
175500                  MOVE  "C" TO WRK-XN-00001-1                     NC2254.2
175600                  MOVE  "D" TO WRK-XN-00001-2                     NC2254.2
175700        WHEN      ANY                                             NC2254.2
175800             ALSO ANY                                             NC2254.2
175900             ALSO ANY                                             NC2254.2
176000             ALSO ANY                                             NC2254.2
176100             ALSO ANY                                             NC2254.2
176200             ALSO WRK-DU-08V00-2 = 6                              NC2254.2
176300                  MOVE  "E" TO WRK-XN-00001-1                     NC2254.2
176400                  MOVE  "F" TO WRK-XN-00001-2                     NC2254.2
176500        WHEN      OTHER                                           NC2254.2
176600                  MOVE  "G" TO WRK-XN-00001-1                     NC2254.2
176700                  MOVE  "H" TO WRK-XN-00001-2                     NC2254.2
176800     END-EVALUATE.                                                NC2254.2
176900 EVA-TEST-GF-33-1.                                                NC2254.2
177000     IF      WRK-XN-00001-1 = "E"                                 NC2254.2
177100             PERFORM PASS                                         NC2254.2
177200             GO TO EVA-WRITE-GF-33-1                              NC2254.2
177300     ELSE                                                         NC2254.2
177400             GO TO EVA-FAIL-GF-33-1.                              NC2254.2
177500 EVA-DELETE-GF-33-1.                                              NC2254.2
177600     PERFORM DE-LETE.                                             NC2254.2
177700     GO TO EVA-WRITE-GF-33-1.                                     NC2254.2
177800 EVA-FAIL-GF-33-1.                                                NC2254.2
177900     MOVE   "EVALUATE FAILURE" TO RE-MARK                         NC2254.2
178000     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2254.2
178100     MOVE   "E"             TO CORRECT-X                          NC2254.2
178200     PERFORM FAIL.                                                NC2254.2
178300 EVA-WRITE-GF-33-1.                                               NC2254.2
178400     PERFORM PRINT-DETAIL.                                        NC2254.2
178500 EVA-TEST-GF-33-2.                                                NC2254.2
178600     ADD     1 TO REC-CT.                                         NC2254.2
178700     IF      WRK-XN-00001-2 = "F"                                 NC2254.2
178800             PERFORM PASS                                         NC2254.2
178900             GO TO EVA-WRITE-GF-33-2                              NC2254.2
179000     ELSE                                                         NC2254.2
179100             GO TO EVA-FAIL-GF-33-2.                              NC2254.2
179200 EVA-DELETE-GF-33-2.                                              NC2254.2
179300     PERFORM DE-LETE.                                             NC2254.2
179400     GO TO EVA-WRITE-GF-33-2.                                     NC2254.2
179500 EVA-FAIL-GF-33-2.                                                NC2254.2
179600     MOVE   "EVALUATE FAILURE" TO RE-MARK                         NC2254.2
179700     MOVE    WRK-XN-00001-2 TO COMPUTED-X                         NC2254.2
179800     MOVE   "F"             TO CORRECT-X                          NC2254.2
179900     PERFORM FAIL.                                                NC2254.2
180000 EVA-WRITE-GF-33-2.                                               NC2254.2
180100     PERFORM PRINT-DETAIL.                                        NC2254.2
180200*                                                                 NC2254.2
180300 EVA-INIT-GF-34.                                                  NC2254.2
180400     MOVE   "VI-84 6.12.4 GR3" TO ANSI-REFERENCE.                 NC2254.2
180500     MOVE   "EVA-TEST-GF-34-1"   TO PAR-NAME.                     NC2254.2
180600     MOVE    1   TO REC-CT.                                       NC2254.2
180700     MOVE    99  TO WRK-DU-08V00.                                 NC2254.2
180800     MOVE   "*"  TO WRK-XN-00001-1.                               NC2254.2
180900     MOVE    99  TO WRK-DU-08V00-1.                               NC2254.2
181000     MOVE    99  TO WRK-DU-08V00-2.                               NC2254.2
181100     MOVE    99  TO WRK-DU-08V00-3.                               NC2254.2
181200     MOVE    99  TO WRK-DU-08V00-4.                               NC2254.2
181300     GO TO   EVA-TEST-GF-34-0.                                    NC2254.2
181400 EVA-DELETE-GF-34.                                                NC2254.2
181500     PERFORM DE-LETE.                                             NC2254.2
181600     PERFORM PRINT-DETAIL.                                        NC2254.2
181700     GO TO   EVA-INIT-GF-35.                                      NC2254.2
181800 EVA-TEST-GF-34-0.                                                NC2254.2
181900     EVALUATE     WRK-DU-08V00                                    NC2254.2
182000             ALSO 81                                              NC2254.2
182100             ALSO (WRK-DU-08V00-2 * 9)                            NC2254.2
182200             ALSO IT-IS-81                                        NC2254.2
182300             ALSO TRUE                                            NC2254.2
182400             ALSO FALSE                                           NC2254.2
182500        WHEN NOT  WRK-DU-08V00-1                                  NC2254.2
182600             ALSO WRK-DU-08V00-2                                  NC2254.2
182700             ALSO 81                                              NC2254.2
182800             ALSO TRUE                                            NC2254.2
182900             ALSO WRK-DU-08V00-3 = 0                              NC2254.2
183000             ALSO WRK-DU-08V00-4 < 9                              NC2254.2
183100                  MOVE  "A" TO WRK-XN-00001-1                     NC2254.2
183200                  MOVE  "B" TO WRK-XN-00001-2                     NC2254.2
183300        WHEN      81                                              NC2254.2
183400             ALSO WRK-DU-08V00-3 THROUGH WRK-DU-08V00-4           NC2254.2
183500             ALSO (WRK-DU-08V00-2 * 7) THRU (WRK-DU-08V00-2 * 8)  NC2254.2
183600             ALSO FALSE                                           NC2254.2
183700             ALSO WRK-DU-08V00-2 = 81                             NC2254.2
183800             ALSO WRK-DU-08V00 > 8                                NC2254.2
183900                  MOVE  "C" TO WRK-XN-00001-1                     NC2254.2
184000                  MOVE  "D" TO WRK-XN-00001-2                     NC2254.2
184100        WHEN      ANY                                             NC2254.2
184200             ALSO ANY                                             NC2254.2
184300             ALSO ANY                                             NC2254.2
184400             ALSO ANY                                             NC2254.2
184500             ALSO ANY                                             NC2254.2
184600             ALSO WRK-DU-08V00 = 99                               NC2254.2
184700                  MOVE  "E" TO WRK-XN-00001-1                     NC2254.2
184800                  MOVE  "F" TO WRK-XN-00001-2                     NC2254.2
184900        WHEN      OTHER                                           NC2254.2
185000                  MOVE  "G" TO WRK-XN-00001-1                     NC2254.2
185100                  MOVE  "H" TO WRK-XN-00001-2                     NC2254.2
185200     END-EVALUATE.                                                NC2254.2
185300 EVA-TEST-GF-34-1.                                                NC2254.2
185400     IF      WRK-XN-00001-1 = "G"                                 NC2254.2
185500             PERFORM PASS                                         NC2254.2
185600             GO TO EVA-WRITE-GF-34-1                              NC2254.2
185700     ELSE                                                         NC2254.2
185800             GO TO EVA-FAIL-GF-34-1.                              NC2254.2
185900 EVA-DELETE-GF-34-1.                                              NC2254.2
186000     PERFORM DE-LETE.                                             NC2254.2
186100     GO TO EVA-WRITE-GF-34-1.                                     NC2254.2
186200 EVA-FAIL-GF-34-1.                                                NC2254.2
186300     MOVE   "EVALUATE FAILURE" TO RE-MARK                         NC2254.2
186400     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2254.2
186500     MOVE   "G"             TO CORRECT-X                          NC2254.2
186600     PERFORM FAIL.                                                NC2254.2
186700 EVA-WRITE-GF-34-1.                                               NC2254.2
186800     PERFORM PRINT-DETAIL.                                        NC2254.2
186900 EVA-TEST-GF-34-2.                                                NC2254.2
187000     ADD     1 TO REC-CT.                                         NC2254.2
187100     IF      WRK-XN-00001-2 = "H"                                 NC2254.2
187200             PERFORM PASS                                         NC2254.2
187300             GO TO EVA-WRITE-GF-34-2                              NC2254.2
187400     ELSE                                                         NC2254.2
187500             GO TO EVA-FAIL-GF-34-2.                              NC2254.2
187600 EVA-DELETE-GF-34-2.                                              NC2254.2
187700     PERFORM DE-LETE.                                             NC2254.2
187800     GO TO EVA-WRITE-GF-34-2.                                     NC2254.2
187900 EVA-FAIL-GF-34-2.                                                NC2254.2
188000     MOVE   "EVALUATE FAILURE" TO RE-MARK                         NC2254.2
188100     MOVE    WRK-XN-00001-2 TO COMPUTED-X                         NC2254.2
188200     MOVE   "H"             TO CORRECT-X                          NC2254.2
188300     PERFORM FAIL.                                                NC2254.2
188400 EVA-WRITE-GF-34-2.                                               NC2254.2
188500     PERFORM PRINT-DETAIL.                                        NC2254.2
188600*                                                                 NC2254.2
188700 EVA-INIT-GF-35.                                                  NC2254.2
188800     MOVE   "VI-84 6.12.4 GR3" TO ANSI-REFERENCE.                 NC2254.2
188900     MOVE   "EVA-TEST-GF-35-1" TO PAR-NAME.                       NC2254.2
189000     MOVE    1   TO REC-CT.                                       NC2254.2
189100     MOVE    81  TO WRK-DU-08V00-1.                               NC2254.2
189200     GO TO   EVA-TEST-GF-35-1.                                    NC2254.2
189300 EVA-DELETE-GF-35.                                                NC2254.2
189400     PERFORM DE-LETE.                                             NC2254.2
189500     PERFORM PRINT-DETAIL.                                        NC2254.2
189600     GO TO   CCVS-EXIT.                                           NC2254.2
189700 EVA-TEST-GF-35-1.                                                NC2254.2
189800     EVALUATE     TRUE                                            NC2254.2
189900             WHEN WRK-DU-08V00 NUMERIC                            NC2254.2
190000             WHEN WRK-DU-08V00 > 10                               NC2254.2
190100             WHEN WRK-DU-08V00 < 100                              NC2254.2
190200                  MOVE "A" TO WRK-XN-00001-1                      NC2254.2
190300             WHEN OTHER                                           NC2254.2
190400                  MOVE "Z" TO WRK-XN-00001-1                      NC2254.2
190500     END-EVALUATE.                                                NC2254.2
190600     IF      WRK-XN-00001-1 = "A"                                 NC2254.2
190700             PERFORM PASS                                         NC2254.2
190800             GO TO EVA-WRITE-GF-35-1                              NC2254.2
190900     ELSE                                                         NC2254.2
191000             GO TO EVA-FAIL-GF-35-1.                              NC2254.2
191100 EVA-DELETE-GF-35-1.                                              NC2254.2
191200     PERFORM DE-LETE.                                             NC2254.2
191300     GO TO EVA-WRITE-GF-35-1.                                     NC2254.2
191400 EVA-FAIL-GF-35-1.                                                NC2254.2
191500     MOVE   "EVALUATE FAILURE" TO RE-MARK                         NC2254.2
191600     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2254.2
191700     MOVE   "A"             TO CORRECT-X                          NC2254.2
191800     PERFORM FAIL.                                                NC2254.2
191900 EVA-WRITE-GF-35-1.                                               NC2254.2
192000     PERFORM PRINT-DETAIL.                                        NC2254.2
192100*                                                                 NC2254.2
192200 CCVS-EXIT SECTION.                                               NC2254.2
192300 CCVS-999999.                                                     NC2254.2
192400     GO TO CLOSE-FILES.                                           NC2254.2
      *END-OF,NC225A                                                                  
