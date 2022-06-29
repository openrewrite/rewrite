      *HEADER,COBOL,NC121M                                                            
000100 IDENTIFICATION DIVISION.                                         NC1214.2
000200 PROGRAM-ID.                                                      NC1214.2
000300     NC121M.                                                      NC1214.2
000400****************************************************************  NC1214.2
000500*                                                              *  NC1214.2
000600*    VALIDATION FOR:-                                          *  NC1214.2
000700*                                                              *  NC1214.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1214.2
000900*                                                              *  NC1214.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1214.2
001100*                                                              *  NC1214.2
001200****************************************************************  NC1214.2
001300*                                                              *  NC1214.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1214.2
001500*                                                              *  NC1214.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1214.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1214.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1214.2
001900*                                                              *  NC1214.2
002000****************************************************************  NC1214.2
002100*                                                                 NC1214.2
002200*    PROGRAM NC121M TESTS THE USE OF INDEXED IDENTIFIERS WITH     NC1214.2
002300*    FORMATS 1 AND 2 OF THE "MULTIPLY" STATEMENT,  FORMATS        NC1214.2
002400*    1, 2 AND 3 OF THE "DIVIDE" STATEMENT,  FORMATS 1 AND 2 OF    NC1214.2
002500*    THE "PERFORM" STATEMENT AND THE "DISPLAY" STATEMENT          NC1214.2
002600*    GENERAL FORMAT.                                              NC1214.2
002700*    ONE AND TWO LEVELS OF INDEXING ARE USED AS WELL AS           NC1214.2
002800*    RELATIVE INDEXING.                                           NC1214.2
002900*                                                                 NC1214.2
003000 ENVIRONMENT DIVISION.                                            NC1214.2
003100 CONFIGURATION SECTION.                                           NC1214.2
003200 SOURCE-COMPUTER.                                                 NC1214.2
003300     XXXXX082.                                                    NC1214.2
003400 OBJECT-COMPUTER.                                                 NC1214.2
003500     XXXXX083.                                                    NC1214.2
003600 INPUT-OUTPUT SECTION.                                            NC1214.2
003700 FILE-CONTROL.                                                    NC1214.2
003800     SELECT PRINT-FILE ASSIGN TO                                  NC1214.2
003900     XXXXX055.                                                    NC1214.2
004000 DATA DIVISION.                                                   NC1214.2
004100 FILE SECTION.                                                    NC1214.2
004200 FD  PRINT-FILE.                                                  NC1214.2
004300 01  PRINT-REC PICTURE X(120).                                    NC1214.2
004400 01  DUMMY-RECORD PICTURE X(120).                                 NC1214.2
004500 WORKING-STORAGE SECTION.                                         NC1214.2
004600 01  TABLE1.                                                      NC1214.2
004700     02  TABLE1-NUM              PICTURE S9V99                    NC1214.2
004800                                 OCCURS 10 TIMES                  NC1214.2
004900                                 INDEXED BY INDEX1.               NC1214.2
005000 01  TABLE2.                                                      NC1214.2
005100     02  TABLE2-NUM              PICTURE 9V9                      NC1214.2
005200                                 OCCURS 6 TIMES                   NC1214.2
005300                                 INDEXED BY INDEX2.               NC1214.2
005400 01  TABLE3.                                                      NC1214.2
005500     02  TABLE3-NUM              PICTURE 99V9                     NC1214.2
005600                                 OCCURS 6 TIMES                   NC1214.2
005700                                 INDEXED BY INDEX3.               NC1214.2
005800 01  TABLE4.                                                      NC1214.2
005900     02  TABLE4-NUM1             OCCURS 3 TIMES                   NC1214.2
006000                                 INDEXED BY INDEX4-1.             NC1214.2
006100         03 TABLE4-NUM2          PICTURE 99                       NC1214.2
006200                                 OCCURS 3 TIMES                   NC1214.2
006300                                 INDEXED BY INDEX4-2.             NC1214.2
006400 01  TABLE5.                                                      NC1214.2
006500     02  TABLE5-NUM              PIC 9                            NC1214.2
006600                                 OCCURS 2 TIMES                   NC1214.2
006700                                 INDEXED BY INDEX5.               NC1214.2
006800 01  TABLE6.                                                      NC1214.2
006900     02  TABLE6-REC              PICTURE X(10)                    NC1214.2
007000                                 OCCURS 2 TIMES                   NC1214.2
007100                                 INDEXED BY INDEX6.               NC1214.2
007200 01  NUM-9V9                     PICTURE 9V9.                     NC1214.2
007300 01  TEST-RESULTS.                                                NC1214.2
007400     02 FILLER                   PIC X      VALUE SPACE.          NC1214.2
007500     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1214.2
007600     02 FILLER                   PIC X      VALUE SPACE.          NC1214.2
007700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1214.2
007800     02 FILLER                   PIC X      VALUE SPACE.          NC1214.2
007900     02  PAR-NAME.                                                NC1214.2
008000       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1214.2
008100       03  PARDOT-X              PIC X      VALUE SPACE.          NC1214.2
008200       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1214.2
008300     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1214.2
008400     02 RE-MARK                  PIC X(61).                       NC1214.2
008500 01  TEST-COMPUTED.                                               NC1214.2
008600     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1214.2
008700     02 FILLER                   PIC X(17)  VALUE                 NC1214.2
008800            "       COMPUTED=".                                   NC1214.2
008900     02 COMPUTED-X.                                               NC1214.2
009000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1214.2
009100     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1214.2
009200                                 PIC -9(9).9(9).                  NC1214.2
009300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1214.2
009400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1214.2
009500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1214.2
009600     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1214.2
009700         04 COMPUTED-18V0                    PIC -9(18).          NC1214.2
009800         04 FILLER                           PIC X.               NC1214.2
009900     03 FILLER PIC X(50) VALUE SPACE.                             NC1214.2
010000 01  TEST-CORRECT.                                                NC1214.2
010100     02 FILLER PIC X(30) VALUE SPACE.                             NC1214.2
010200     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1214.2
010300     02 CORRECT-X.                                                NC1214.2
010400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1214.2
010500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1214.2
010600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1214.2
010700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1214.2
010800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1214.2
010900     03      CR-18V0 REDEFINES CORRECT-A.                         NC1214.2
011000         04 CORRECT-18V0                     PIC -9(18).          NC1214.2
011100         04 FILLER                           PIC X.               NC1214.2
011200     03 FILLER PIC X(2) VALUE SPACE.                              NC1214.2
011300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1214.2
011400 01  CCVS-C-1.                                                    NC1214.2
011500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1214.2
011600-    "SS  PARAGRAPH-NAME                                          NC1214.2
011700-    "       REMARKS".                                            NC1214.2
011800     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1214.2
011900 01  CCVS-C-2.                                                    NC1214.2
012000     02 FILLER                     PIC X        VALUE SPACE.      NC1214.2
012100     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1214.2
012200     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1214.2
012300     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1214.2
012400     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1214.2
012500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1214.2
012600 01  REC-CT                        PIC 99       VALUE ZERO.       NC1214.2
012700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1214.2
012800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1214.2
012900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1214.2
013000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1214.2
013100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1214.2
013200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1214.2
013300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1214.2
013400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1214.2
013500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1214.2
013600 01  CCVS-H-1.                                                    NC1214.2
013700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1214.2
013800     02  FILLER                    PIC X(42)    VALUE             NC1214.2
013900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1214.2
014000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1214.2
014100 01  CCVS-H-2A.                                                   NC1214.2
014200   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1214.2
014300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1214.2
014400   02  FILLER                        PIC XXXX   VALUE             NC1214.2
014500     "4.2 ".                                                      NC1214.2
014600   02  FILLER                        PIC X(28)  VALUE             NC1214.2
014700            " COPY - NOT FOR DISTRIBUTION".                       NC1214.2
014800   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1214.2
014900                                                                  NC1214.2
015000 01  CCVS-H-2B.                                                   NC1214.2
015100   02  FILLER                        PIC X(15)  VALUE             NC1214.2
015200            "TEST RESULT OF ".                                    NC1214.2
015300   02  TEST-ID                       PIC X(9).                    NC1214.2
015400   02  FILLER                        PIC X(4)   VALUE             NC1214.2
015500            " IN ".                                               NC1214.2
015600   02  FILLER                        PIC X(12)  VALUE             NC1214.2
015700     " HIGH       ".                                              NC1214.2
015800   02  FILLER                        PIC X(22)  VALUE             NC1214.2
015900            " LEVEL VALIDATION FOR ".                             NC1214.2
016000   02  FILLER                        PIC X(58)  VALUE             NC1214.2
016100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1214.2
016200 01  CCVS-H-3.                                                    NC1214.2
016300     02  FILLER                      PIC X(34)  VALUE             NC1214.2
016400            " FOR OFFICIAL USE ONLY    ".                         NC1214.2
016500     02  FILLER                      PIC X(58)  VALUE             NC1214.2
016600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1214.2
016700     02  FILLER                      PIC X(28)  VALUE             NC1214.2
016800            "  COPYRIGHT   1985 ".                                NC1214.2
016900 01  CCVS-E-1.                                                    NC1214.2
017000     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1214.2
017100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1214.2
017200     02 ID-AGAIN                     PIC X(9).                    NC1214.2
017300     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1214.2
017400 01  CCVS-E-2.                                                    NC1214.2
017500     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1214.2
017600     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1214.2
017700     02 CCVS-E-2-2.                                               NC1214.2
017800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1214.2
017900         03 FILLER                   PIC X      VALUE SPACE.      NC1214.2
018000         03 ENDER-DESC               PIC X(44)  VALUE             NC1214.2
018100            "ERRORS ENCOUNTERED".                                 NC1214.2
018200 01  CCVS-E-3.                                                    NC1214.2
018300     02  FILLER                      PIC X(22)  VALUE             NC1214.2
018400            " FOR OFFICIAL USE ONLY".                             NC1214.2
018500     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1214.2
018600     02  FILLER                      PIC X(58)  VALUE             NC1214.2
018700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1214.2
018800     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1214.2
018900     02 FILLER                       PIC X(15)  VALUE             NC1214.2
019000             " COPYRIGHT 1985".                                   NC1214.2
019100 01  CCVS-E-4.                                                    NC1214.2
019200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1214.2
019300     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1214.2
019400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1214.2
019500     02 FILLER                       PIC X(40)  VALUE             NC1214.2
019600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1214.2
019700 01  XXINFO.                                                      NC1214.2
019800     02 FILLER                       PIC X(19)  VALUE             NC1214.2
019900            "*** INFORMATION ***".                                NC1214.2
020000     02 INFO-TEXT.                                                NC1214.2
020100       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1214.2
020200       04 XXCOMPUTED                 PIC X(20).                   NC1214.2
020300       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1214.2
020400       04 XXCORRECT                  PIC X(20).                   NC1214.2
020500     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1214.2
020600 01  HYPHEN-LINE.                                                 NC1214.2
020700     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1214.2
020800     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1214.2
020900-    "*****************************************".                 NC1214.2
021000     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1214.2
021100-    "******************************".                            NC1214.2
021200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1214.2
021300     "NC121M".                                                    NC1214.2
021400 PROCEDURE DIVISION.                                              NC1214.2
021500 CCVS1 SECTION.                                                   NC1214.2
021600 OPEN-FILES.                                                      NC1214.2
021700     OPEN     OUTPUT PRINT-FILE.                                  NC1214.2
021800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1214.2
021900     MOVE    SPACE TO TEST-RESULTS.                               NC1214.2
022000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1214.2
022100     GO TO CCVS1-EXIT.                                            NC1214.2
022200 CLOSE-FILES.                                                     NC1214.2
022300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1214.2
022400 TERMINATE-CCVS.                                                  NC1214.2
022500*    EXIT PROGRAM.                                                NC1214.2
022600*TERMINATE-CALL.                                                  NC1214.2
022700     STOP     RUN.                                                NC1214.2
022800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1214.2
022900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1214.2
023000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1214.2
023100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1214.2
023200     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1214.2
023300 PRINT-DETAIL.                                                    NC1214.2
023400     IF REC-CT NOT EQUAL TO ZERO                                  NC1214.2
023500             MOVE "." TO PARDOT-X                                 NC1214.2
023600             MOVE REC-CT TO DOTVALUE.                             NC1214.2
023700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1214.2
023800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1214.2
023900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1214.2
024000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1214.2
024100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1214.2
024200     MOVE SPACE TO CORRECT-X.                                     NC1214.2
024300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1214.2
024400     MOVE     SPACE TO RE-MARK.                                   NC1214.2
024500 HEAD-ROUTINE.                                                    NC1214.2
024600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1214.2
024700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1214.2
024800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1214.2
024900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1214.2
025000 COLUMN-NAMES-ROUTINE.                                            NC1214.2
025100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1214.2
025200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1214.2
025300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1214.2
025400 END-ROUTINE.                                                     NC1214.2
025500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1214.2
025600 END-RTN-EXIT.                                                    NC1214.2
025700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1214.2
025800 END-ROUTINE-1.                                                   NC1214.2
025900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1214.2
026000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1214.2
026100      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1214.2
026200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1214.2
026300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1214.2
026400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1214.2
026500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1214.2
026600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1214.2
026700  END-ROUTINE-12.                                                 NC1214.2
026800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1214.2
026900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1214.2
027000         MOVE "NO " TO ERROR-TOTAL                                NC1214.2
027100         ELSE                                                     NC1214.2
027200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1214.2
027300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1214.2
027400     PERFORM WRITE-LINE.                                          NC1214.2
027500 END-ROUTINE-13.                                                  NC1214.2
027600     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1214.2
027700         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1214.2
027800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1214.2
027900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1214.2
028000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1214.2
028100      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1214.2
028200          MOVE "NO " TO ERROR-TOTAL                               NC1214.2
028300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1214.2
028400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1214.2
028500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1214.2
028600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1214.2
028700 WRITE-LINE.                                                      NC1214.2
028800     ADD 1 TO RECORD-COUNT.                                       NC1214.2
028900*    IF RECORD-COUNT GREATER 42                                   NC1214.2
029000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1214.2
029100*        MOVE SPACE TO DUMMY-RECORD                               NC1214.2
029200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1214.2
029300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1214.2
029400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1214.2
029500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1214.2
029600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1214.2
029700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1214.2
029800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1214.2
029900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1214.2
030000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1214.2
030100*        MOVE ZERO TO RECORD-COUNT.                               NC1214.2
030200     PERFORM WRT-LN.                                              NC1214.2
030300 WRT-LN.                                                          NC1214.2
030400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1214.2
030500     MOVE SPACE TO DUMMY-RECORD.                                  NC1214.2
030600 BLANK-LINE-PRINT.                                                NC1214.2
030700     PERFORM WRT-LN.                                              NC1214.2
030800 FAIL-ROUTINE.                                                    NC1214.2
030900     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1214.2
031000            GO TO FAIL-ROUTINE-WRITE.                             NC1214.2
031100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1214.2
031200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1214.2
031300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1214.2
031400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1214.2
031500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1214.2
031600     GO TO  FAIL-ROUTINE-EX.                                      NC1214.2
031700 FAIL-ROUTINE-WRITE.                                              NC1214.2
031800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1214.2
031900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1214.2
032000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1214.2
032100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1214.2
032200 FAIL-ROUTINE-EX. EXIT.                                           NC1214.2
032300 BAIL-OUT.                                                        NC1214.2
032400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1214.2
032500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1214.2
032600 BAIL-OUT-WRITE.                                                  NC1214.2
032700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1214.2
032800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1214.2
032900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1214.2
033000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1214.2
033100 BAIL-OUT-EX. EXIT.                                               NC1214.2
033200 CCVS1-EXIT.                                                      NC1214.2
033300     EXIT.                                                        NC1214.2
033400 SECT-NC121M-001 SECTION.                                         NC1214.2
033500 BUILD-TABLE1.                                                    NC1214.2
033600     MOVE 4.00 TO TABLE1-NUM (1).                                 NC1214.2
033700     MOVE 1.34 TO TABLE1-NUM (2).                                 NC1214.2
033800     MOVE 7.00 TO TABLE1-NUM (3).                                 NC1214.2
033900     MOVE 3.00 TO TABLE1-NUM (4).                                 NC1214.2
034000     MOVE 2.00 TO TABLE1-NUM (5).                                 NC1214.2
034100     MOVE 1.50 TO TABLE1-NUM (6).                                 NC1214.2
034200     MOVE 3.50 TO TABLE1-NUM (7).                                 NC1214.2
034300     MOVE 0.00 TO TABLE1-NUM (8).                                 NC1214.2
034400     MOVE 5.00 TO TABLE1-NUM (9).                                 NC1214.2
034500     MOVE -9.00 TO TABLE1-NUM (10).                               NC1214.2
034600 BUILD-TABLE2.                                                    NC1214.2
034700     MOVE 1.0 TO TABLE2-NUM (1).                                  NC1214.2
034800     MOVE 6.0 TO TABLE2-NUM (2).                                  NC1214.2
034900     MOVE 3.0 TO TABLE2-NUM (3).                                  NC1214.2
035000     MOVE 2.0 TO TABLE2-NUM (4).                                  NC1214.2
035100     MOVE 9.7 TO TABLE2-NUM (5).                                  NC1214.2
035200     MOVE 1.2 TO TABLE2-NUM (6).                                  NC1214.2
035300 BUILD-TABLE4.                                                    NC1214.2
035400     MOVE 01 TO TABLE4-NUM2 (1  1).                               NC1214.2
035500     MOVE 02 TO TABLE4-NUM2 (1  2).                               NC1214.2
035600     MOVE 03 TO TABLE4-NUM2 (1  3).                               NC1214.2
035700     MOVE 12 TO TABLE4-NUM2 (2  1).                               NC1214.2
035800     MOVE 24 TO TABLE4-NUM2 (2  2).                               NC1214.2
035900     MOVE 25 TO TABLE4-NUM2 (2  3).                               NC1214.2
036000     MOVE 14 TO TABLE4-NUM2 (3  1).                               NC1214.2
036100     MOVE 15 TO TABLE4-NUM2 (3  2).                               NC1214.2
036200     MOVE 16 TO TABLE4-NUM2 (3  3).                               NC1214.2
036300 BUILD-TABLE5.                                                    NC1214.2
036400     MOVE 3 TO TABLE5-NUM (1).                                    NC1214.2
036500     MOVE 2 TO TABLE5-NUM (2).                                    NC1214.2
036600 BUILD-TABLE6.                                                    NC1214.2
036700     MOVE "LITERAL-01" TO TABLE6-REC (1).                         NC1214.2
036800     MOVE "0123456789" TO TABLE6-REC (2).                         NC1214.2
036900 IND-INIT-GF-1.                                                   NC1214.2
037000     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
037100     MOVE "MULTIPLY BY" TO FEATURE.                               NC1214.2
037200     MOVE 2.0 TO NUM-9V9.                                         NC1214.2
037300     SET INDEX1 TO 1.                                             NC1214.2
037400 IND-TEST-GF-1-0.                                                 NC1214.2
037500     MULTIPLY TABLE1-NUM (INDEX1) BY NUM-9V9.                     NC1214.2
037600 IND-TEST-GF-1-1.                                                 NC1214.2
037700     IF NUM-9V9 EQUAL TO 8.0                                      NC1214.2
037800        PERFORM PASS                                              NC1214.2
037900        ELSE GO TO IND-FAIL-GF-1.                                 NC1214.2
038000     GO TO IND-WRITE-GF-1.                                        NC1214.2
038100 IND-DELETE-GF-1.                                                 NC1214.2
038200     PERFORM DE-LETE.                                             NC1214.2
038300     GO TO IND-WRITE-GF-1.                                        NC1214.2
038400 IND-FAIL-GF-1.                                                   NC1214.2
038500     PERFORM FAIL.                                                NC1214.2
038600     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
038700     MOVE 8.0 TO CORRECT-14V4.                                    NC1214.2
038800 IND-WRITE-GF-1.                                                  NC1214.2
038900     MOVE "IND-TEST-GF-1" TO PAR-NAME.                            NC1214.2
039000     PERFORM PRINT-DETAIL.                                        NC1214.2
039100 IND-INIT-GF-2.                                                   NC1214.2
039200     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
039300     MOVE "MULTIPLY ROUNDED" TO FEATURE.                          NC1214.2
039400     MOVE 4.0 TO NUM-9V9.                                         NC1214.2
039500     SET INDEX1 TO 2.                                             NC1214.2
039600 IND-TEST-GF-2-0.                                                 NC1214.2
039700     MULTIPLY TABLE1-NUM (INDEX1) BY NUM-9V9 ROUNDED.             NC1214.2
039800 IND-TEST-GF-2-1.                                                 NC1214.2
039900     IF NUM-9V9 EQUAL TO 5.4                                      NC1214.2
040000        PERFORM PASS                                              NC1214.2
040100        ELSE GO TO IND-FAIL-GF-2.                                 NC1214.2
040200     GO TO IND-WRITE-GF-2.                                        NC1214.2
040300 IND-DELETE-GF-2.                                                 NC1214.2
040400     PERFORM DE-LETE.                                             NC1214.2
040500     GO TO IND-WRITE-GF-2.                                        NC1214.2
040600 IND-FAIL-GF-2.                                                   NC1214.2
040700     PERFORM FAIL.                                                NC1214.2
040800     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
040900     MOVE 5.4 TO CORRECT-14V4.                                    NC1214.2
041000 IND-WRITE-GF-2.                                                  NC1214.2
041100     MOVE "IND-TEST-GF-2" TO PAR-NAME.                            NC1214.2
041200     PERFORM PRINT-DETAIL.                                        NC1214.2
041300 IND-INIT-GF-3.                                                   NC1214.2
041400     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
041500     MOVE "MULTIPLY ON SIZE ERR" TO FEATURE.                      NC1214.2
041600     MOVE 6.0 TO NUM-9V9.                                         NC1214.2
041700     SET INDEX1 TO 3.                                             NC1214.2
041800 IND-TEST-GF-3-1.                                                 NC1214.2
041900     MULTIPLY TABLE1-NUM (INDEX1) BY NUM-9V9 ON SIZE ERROR        NC1214.2
042000        PERFORM PASS                                              NC1214.2
042100        GO TO IND-WRITE-GF-3-1.                                   NC1214.2
042200     GO TO IND-FAIL-GF-3-1.                                       NC1214.2
042300 IND-DELETE-GF-3-1.                                               NC1214.2
042400     PERFORM DE-LETE.                                             NC1214.2
042500     GO TO IND-WRITE-GF-3-1.                                      NC1214.2
042600 IND-FAIL-GF-3-1.                                                 NC1214.2
042700     PERFORM FAIL.                                                NC1214.2
042800     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
042900     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1214.2
043000 IND-WRITE-GF-3-1.                                                NC1214.2
043100     MOVE "IND-TEST-GF-3-1" TO PAR-NAME.                          NC1214.2
043200     PERFORM PRINT-DETAIL.                                        NC1214.2
043300 IND-TEST-GF-3-2.                                                 NC1214.2
043400     IF      NUM-9V9 = 6.0                                        NC1214.2
043500             PERFORM PASS                                         NC1214.2
043600             GO TO   IND-WRITE-GF-3-2.                            NC1214.2
043700     GO TO   IND-FAIL-GF-3-2.                                     NC1214.2
043800 IND-DELETE-GF-3-2.                                               NC1214.2
043900     PERFORM DE-LETE.                                             NC1214.2
044000     GO TO IND-WRITE-GF-3-2.                                      NC1214.2
044100 IND-FAIL-GF-3-2.                                                 NC1214.2
044200     PERFORM FAIL.                                                NC1214.2
044300     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
044400     MOVE 6.0 TO CORRECT-14V4.                                    NC1214.2
044500     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1214.2
044600 IND-WRITE-GF-3-2.                                                NC1214.2
044700     MOVE "IND-TEST-GF-3-2" TO PAR-NAME.                          NC1214.2
044800     PERFORM PRINT-DETAIL.                                        NC1214.2
044900 IND-INIT-GF-4.                                                   NC1214.2
045000     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
045100     MOVE "MULTIPLY BY" TO FEATURE.                               NC1214.2
045200     SET INDEX1 TO 1.                                             NC1214.2
045300     SET INDEX2 TO 1.                                             NC1214.2
045400 IND-TEST-GF-4-0.                                                 NC1214.2
045500     MULTIPLY TABLE1-NUM (INDEX1) BY TABLE2-NUM (INDEX2).         NC1214.2
045600 IND-TEST-GF-4-1.                                                 NC1214.2
045700     IF TABLE2-NUM (INDEX2) EQUAL TO 4.0                          NC1214.2
045800        PERFORM PASS                                              NC1214.2
045900        ELSE GO TO IND-FAIL-GF-4.                                 NC1214.2
046000     GO TO IND-WRITE-GF-4.                                        NC1214.2
046100 IND-DELETE-GF-4.                                                 NC1214.2
046200     PERFORM DE-LETE.                                             NC1214.2
046300     GO TO IND-WRITE-GF-4.                                        NC1214.2
046400 IND-FAIL-GF-4.                                                   NC1214.2
046500     PERFORM FAIL.                                                NC1214.2
046600     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1214.2
046700     MOVE 4.0 TO CORRECT-14V4.                                    NC1214.2
046800 IND-WRITE-GF-4.                                                  NC1214.2
046900     MOVE "IND-TEST-GF-4" TO PAR-NAME.                            NC1214.2
047000     PERFORM PRINT-DETAIL.                                        NC1214.2
047100 IND-INIT-GF-5.                                                   NC1214.2
047200     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
047300     MOVE "MULTIPLY ROUNDED" TO FEATURE.                          NC1214.2
047400     PERFORM BUILD-TABLE2.                                        NC1214.2
047500     SET INDEX1 TO 2.                                             NC1214.2
047600     SET INDEX2 TO 2.                                             NC1214.2
047700 IND-TEST-GF-5-0.                                                 NC1214.2
047800     MULTIPLY TABLE1-NUM (INDEX1) BY TABLE2-NUM (INDEX2) ROUNDED. NC1214.2
047900 IND-TEST-GF-5-1.                                                 NC1214.2
048000     IF TABLE2-NUM (INDEX2) EQUAL TO 8.0                          NC1214.2
048100        PERFORM PASS                                              NC1214.2
048200        ELSE GO TO IND-FAIL-GF-5.                                 NC1214.2
048300     GO TO IND-WRITE-GF-5.                                        NC1214.2
048400 IND-DELETE-GF-5.                                                 NC1214.2
048500     PERFORM DE-LETE.                                             NC1214.2
048600     GO TO IND-WRITE-GF-5.                                        NC1214.2
048700 IND-FAIL-GF-5.                                                   NC1214.2
048800     PERFORM FAIL.                                                NC1214.2
048900     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1214.2
049000     MOVE 8.0 TO CORRECT-14V4.                                    NC1214.2
049100 IND-WRITE-GF-5.                                                  NC1214.2
049200     MOVE "IND-TEST-GF-5" TO PAR-NAME.                            NC1214.2
049300     PERFORM PRINT-DETAIL.                                        NC1214.2
049400 IND-INIT-GF-6.                                                   NC1214.2
049500     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
049600     MOVE "MULTIPLY ON SIZE ERR" TO FEATURE.                      NC1214.2
049700     PERFORM BUILD-TABLE2.                                        NC1214.2
049800     SET INDEX1 TO 3.                                             NC1214.2
049900     SET INDEX2 TO 3.                                             NC1214.2
050000 IND-TEST-GF-6-1.                                                 NC1214.2
050100     MULTIPLY TABLE1-NUM (INDEX1) BY TABLE2-NUM (INDEX2)          NC1214.2
050200        ON SIZE ERROR                                             NC1214.2
050300        PERFORM PASS                                              NC1214.2
050400        GO TO IND-WRITE-GF-6-1.                                   NC1214.2
050500     GO TO IND-FAIL-GF-6-1.                                       NC1214.2
050600 IND-DELETE-GF-6-1.                                               NC1214.2
050700     PERFORM DE-LETE.                                             NC1214.2
050800     GO TO IND-WRITE-GF-6-1.                                      NC1214.2
050900 IND-FAIL-GF-6-1.                                                 NC1214.2
051000     PERFORM FAIL.                                                NC1214.2
051100     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1214.2
051200     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1214.2
051300 IND-WRITE-GF-6-1.                                                NC1214.2
051400     MOVE "IND-TEST-GF-6-1" TO PAR-NAME.                          NC1214.2
051500     PERFORM PRINT-DETAIL.                                        NC1214.2
051600 IND-TEST-GF-6-2.                                                 NC1214.2
051700     IF      TABLE2-NUM (INDEX2) = 3.0                            NC1214.2
051800             PERFORM PASS                                         NC1214.2
051900             GO TO   IND-WRITE-GF-6-2.                            NC1214.2
052000     GO TO   IND-FAIL-GF-6-2.                                     NC1214.2
052100 IND-DELETE-GF-6-2.                                               NC1214.2
052200     PERFORM DE-LETE.                                             NC1214.2
052300     GO TO IND-WRITE-GF-6-2.                                      NC1214.2
052400 IND-FAIL-GF-6-2.                                                 NC1214.2
052500     PERFORM FAIL.                                                NC1214.2
052600     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1214.2
052700     MOVE 3.0 TO CORRECT-14V4.                                    NC1214.2
052800     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1214.2
052900 IND-WRITE-GF-6-2.                                                NC1214.2
053000     MOVE "IND-TEST-GF-6-2" TO PAR-NAME.                          NC1214.2
053100     PERFORM PRINT-DETAIL.                                        NC1214.2
053200 IND-INIT-GF-7.                                                   NC1214.2
053300     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
053400     MOVE "MULTIPLY BY GIVING" TO FEATURE.                        NC1214.2
053500     PERFORM BUILD-TABLE2.                                        NC1214.2
053600     MOVE ZERO TO NUM-9V9.                                        NC1214.2
053700     SET INDEX1 TO 1.                                             NC1214.2
053800 IND-TEST-GF-7-0.                                                 NC1214.2
053900     MULTIPLY 2 BY TABLE1-NUM (INDEX1) GIVING NUM-9V9.            NC1214.2
054000 IND-TEST-GF-7-1.                                                 NC1214.2
054100     IF NUM-9V9 EQUAL TO 8.0                                      NC1214.2
054200        PERFORM PASS                                              NC1214.2
054300        ELSE GO TO IND-FAIL-GF-7.                                 NC1214.2
054400     GO TO IND-WRITE-GF-7.                                        NC1214.2
054500 IND-DELETE-GF-7.                                                 NC1214.2
054600     PERFORM DE-LETE.                                             NC1214.2
054700     GO TO IND-WRITE-GF-7.                                        NC1214.2
054800 IND-FAIL-GF-7.                                                   NC1214.2
054900     PERFORM FAIL.                                                NC1214.2
055000     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
055100     MOVE 8.0 TO CORRECT-14V4.                                    NC1214.2
055200 IND-WRITE-GF-7.                                                  NC1214.2
055300     MOVE "IND-TEST-GF-7" TO PAR-NAME.                            NC1214.2
055400     PERFORM PRINT-DETAIL.                                        NC1214.2
055500 IND-INIT-GF-8.                                                   NC1214.2
055600     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
055700     MOVE "MULTIPLY BY GIVING" TO FEATURE.                        NC1214.2
055800     PERFORM BUILD-TABLE2.                                        NC1214.2
055900     MOVE ZERO TO NUM-9V9.                                        NC1214.2
056000     SET INDEX1 TO 4.                                             NC1214.2
056100     SET INDEX2 TO 4.                                             NC1214.2
056200 IND-TEST-GF-8-0.                                                 NC1214.2
056300     MULTIPLY TABLE1-NUM (INDEX1) BY TABLE2-NUM (INDEX2)          NC1214.2
056400        GIVING NUM-9V9.                                           NC1214.2
056500 IND-TEST-GF-8-1.                                                 NC1214.2
056600     IF NUM-9V9 EQUAL TO 6.0                                      NC1214.2
056700        PERFORM PASS                                              NC1214.2
056800        ELSE GO TO IND-FAIL-GF-8.                                 NC1214.2
056900     GO TO IND-WRITE-GF-8.                                        NC1214.2
057000 IND-DELETE-GF-8.                                                 NC1214.2
057100     PERFORM DE-LETE.                                             NC1214.2
057200     GO TO IND-WRITE-GF-8.                                        NC1214.2
057300 IND-FAIL-GF-8.                                                   NC1214.2
057400     PERFORM FAIL.                                                NC1214.2
057500     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
057600     MOVE 6.0 TO CORRECT-14V4.                                    NC1214.2
057700 IND-WRITE-GF-8.                                                  NC1214.2
057800     MOVE "IND-TEST-GF-8" TO PAR-NAME.                            NC1214.2
057900     PERFORM PRINT-DETAIL.                                        NC1214.2
058000 IND-INIT-GF-9.                                                   NC1214.2
058100     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
058200     MOVE "MULTIPLY BY GIVING" TO FEATURE.                        NC1214.2
058300     PERFORM BUILD-TABLE2.                                        NC1214.2
058400     MOVE ZERO TO TABLE3.                                         NC1214.2
058500     SET INDEX1 TO 3.                                             NC1214.2
058600     SET INDEX2 TO 2.                                             NC1214.2
058700     SET INDEX3 TO 1.                                             NC1214.2
058800 IND-TEST-GF-9-0.                                                 NC1214.2
058900     MULTIPLY TABLE1-NUM (INDEX1) BY TABLE2-NUM (INDEX2)          NC1214.2
059000        GIVING TABLE3-NUM (INDEX3).                               NC1214.2
059100 IND-TEST-GF-9-1.                                                 NC1214.2
059200     IF TABLE3-NUM (INDEX3) EQUAL TO 42.0                         NC1214.2
059300        PERFORM PASS                                              NC1214.2
059400        ELSE GO TO IND-FAIL-GF-9.                                 NC1214.2
059500     GO TO IND-WRITE-GF-9.                                        NC1214.2
059600 IND-DELETE-GF-9.                                                 NC1214.2
059700     PERFORM DE-LETE.                                             NC1214.2
059800     GO TO IND-WRITE-GF-9.                                        NC1214.2
059900 IND-FAIL-GF-9.                                                   NC1214.2
060000     PERFORM FAIL.                                                NC1214.2
060100     MOVE TABLE3-NUM (INDEX3) TO COMPUTED-14V4.                   NC1214.2
060200     MOVE 42.0 TO CORRECT-14V4.                                   NC1214.2
060300 IND-WRITE-GF-9.                                                  NC1214.2
060400     MOVE "IND-TEST-GF-9" TO PAR-NAME.                            NC1214.2
060500     PERFORM PRINT-DETAIL.                                        NC1214.2
060600 IND-INIT-GF-10.                                                  NC1214.2
060700     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
060800     MOVE "MULTIPLY BY" TO FEATURE.                               NC1214.2
060900     MOVE 2.0 TO NUM-9V9.                                         NC1214.2
061000     SET INDEX1 TO 3.                                             NC1214.2
061100 IND-TEST-GF-10-0.                                                NC1214.2
061200     MULTIPLY TABLE1-NUM (INDEX1 - 2) BY NUM-9V9.                 NC1214.2
061300 IND-TEST-GF-10-1.                                                NC1214.2
061400     IF NUM-9V9 EQUAL TO 8.0                                      NC1214.2
061500        PERFORM PASS                                              NC1214.2
061600        ELSE GO TO IND-FAIL-GF-10.                                NC1214.2
061700     GO TO IND-WRITE-GF-10.                                       NC1214.2
061800 IND-DELETE-GF-10.                                                NC1214.2
061900     PERFORM DE-LETE.                                             NC1214.2
062000     GO TO IND-WRITE-GF-10.                                       NC1214.2
062100 IND-FAIL-GF-10.                                                  NC1214.2
062200     PERFORM FAIL.                                                NC1214.2
062300     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
062400     MOVE 8.0 TO CORRECT-14V4.                                    NC1214.2
062500 IND-WRITE-GF-10.                                                 NC1214.2
062600     MOVE "IND-TEST-GF-10" TO PAR-NAME.                           NC1214.2
062700     PERFORM PRINT-DETAIL.                                        NC1214.2
062800 IND-INIT-GF-11.                                                  NC1214.2
062900     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
063000     MOVE "MULTIPLY BY" TO FEATURE.                               NC1214.2
063100     PERFORM BUILD-TABLE2.                                        NC1214.2
063200     SET INDEX1 TO 2.                                             NC1214.2
063300     SET INDEX2 TO 5.                                             NC1214.2
063400 IND-TEST-GF-11-0.                                                NC1214.2
063500     MULTIPLY TABLE1-NUM (INDEX1 - 1) BY TABLE2-NUM (INDEX2 + 1). NC1214.2
063600 IND-TEST-GF-11-1.                                                NC1214.2
063700     IF TABLE2-NUM (INDEX2 + 1) EQUAL TO 4.8                      NC1214.2
063800        PERFORM PASS                                              NC1214.2
063900        ELSE GO TO IND-FAIL-GF-11.                                NC1214.2
064000     GO TO IND-WRITE-GF-11.                                       NC1214.2
064100 IND-DELETE-GF-11.                                                NC1214.2
064200     PERFORM DE-LETE.                                             NC1214.2
064300     GO TO IND-WRITE-GF-11.                                       NC1214.2
064400 IND-FAIL-GF-11.                                                  NC1214.2
064500     PERFORM FAIL.                                                NC1214.2
064600     MOVE TABLE2-NUM (INDEX2 + 1) TO COMPUTED-14V4.               NC1214.2
064700     MOVE 4.8 TO CORRECT-14V4.                                    NC1214.2
064800 IND-WRITE-GF-11.                                                 NC1214.2
064900     MOVE "IND-TEST-GF-11" TO PAR-NAME.                           NC1214.2
065000     PERFORM PRINT-DETAIL.                                        NC1214.2
065100 IND-INIT-GF-12.                                                  NC1214.2
065200     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
065300     MOVE "MULTIPLY BY GIVING" TO FEATURE.                        NC1214.2
065400     PERFORM BUILD-TABLE2.                                        NC1214.2
065500     MOVE ZERO TO NUM-9V9.                                        NC1214.2
065600     SET INDEX1 TO 6.                                             NC1214.2
065700     SET INDEX2 TO 3.                                             NC1214.2
065800 IND-TEST-GF-12-0.                                                NC1214.2
065900     MULTIPLY TABLE1-NUM (INDEX1 - 2) BY TABLE2-NUM (INDEX2 - 2)  NC1214.2
066000        GIVING NUM-9V9.                                           NC1214.2
066100 IND-TEST-GF-12-1.                                                NC1214.2
066200     IF NUM-9V9 EQUAL TO 3.0                                      NC1214.2
066300        PERFORM PASS                                              NC1214.2
066400        ELSE GO TO IND-FAIL-GF-12.                                NC1214.2
066500     GO TO IND-WRITE-GF-12.                                       NC1214.2
066600 IND-DELETE-GF-12.                                                NC1214.2
066700     PERFORM DE-LETE.                                             NC1214.2
066800     GO TO IND-WRITE-GF-12.                                       NC1214.2
066900 IND-FAIL-GF-12.                                                  NC1214.2
067000     PERFORM FAIL.                                                NC1214.2
067100     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
067200     MOVE 3.0 TO CORRECT-14V4.                                    NC1214.2
067300 IND-WRITE-GF-12.                                                 NC1214.2
067400     MOVE "IND-TEST-GF-12" TO PAR-NAME.                           NC1214.2
067500     PERFORM PRINT-DETAIL.                                        NC1214.2
067600 IND-INIT-GF-13.                                                  NC1214.2
067700     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
067800     MOVE "MULTIPLY BY GIVING" TO FEATURE.                        NC1214.2
067900     PERFORM BUILD-TABLE2.                                        NC1214.2
068000     MOVE ZERO TO TABLE3.                                         NC1214.2
068100     SET INDEX1 TO 1.                                             NC1214.2
068200     SET INDEX2 TO 3.                                             NC1214.2
068300     SET INDEX3 TO 1.                                             NC1214.2
068400 IND-TEST-GF-13-0.                                                NC1214.2
068500     MULTIPLY TABLE1-NUM (INDEX1 + 2) BY TABLE2-NUM (INDEX2 - 1)  NC1214.2
068600        GIVING TABLE3-NUM (INDEX3 + 1).                           NC1214.2
068700 IND-TEST-GF-13-1.                                                NC1214.2
068800     IF TABLE3-NUM (INDEX3 + 1) EQUAL TO 42.0                     NC1214.2
068900        PERFORM PASS                                              NC1214.2
069000        ELSE GO TO IND-FAIL-GF-13.                                NC1214.2
069100     GO TO IND-WRITE-GF-13.                                       NC1214.2
069200 IND-DELETE-GF-13.                                                NC1214.2
069300     PERFORM DE-LETE.                                             NC1214.2
069400     GO TO IND-WRITE-GF-13.                                       NC1214.2
069500 IND-FAIL-GF-13.                                                  NC1214.2
069600     PERFORM FAIL.                                                NC1214.2
069700     MOVE TABLE3-NUM (INDEX3 + 1) TO COMPUTED-14V4.               NC1214.2
069800     MOVE 42.0 TO CORRECT-14V4.                                   NC1214.2
069900 IND-WRITE-GF-13.                                                 NC1214.2
070000     MOVE "IND-TEST-GF-13" TO PAR-NAME.                           NC1214.2
070100     PERFORM PRINT-DETAIL.                                        NC1214.2
070200 IND-INIT-GF-14.                                                  NC1214.2
070300     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
070400     MOVE "DIVIDE INTO" TO FEATURE.                               NC1214.2
070500     MOVE 9.0 TO NUM-9V9.                                         NC1214.2
070600     SET INDEX1 TO 4.                                             NC1214.2
070700 IND-TEST-GF-14-0.                                                NC1214.2
070800     DIVIDE TABLE1-NUM (INDEX1) INTO NUM-9V9.                     NC1214.2
070900 IND-TEST-GF-14-1.                                                NC1214.2
071000     IF NUM-9V9 EQUAL TO 3.0                                      NC1214.2
071100        PERFORM PASS                                              NC1214.2
071200        GO TO IND-WRITE-GF-14.                                    NC1214.2
071300     GO TO IND-FAIL-GF-14.                                        NC1214.2
071400 IND-DELETE-GF-14.                                                NC1214.2
071500     PERFORM DE-LETE.                                             NC1214.2
071600     GO TO IND-WRITE-GF-14.                                       NC1214.2
071700 IND-FAIL-GF-14.                                                  NC1214.2
071800     PERFORM FAIL.                                                NC1214.2
071900     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
072000     MOVE 3.0 TO CORRECT-14V4.                                    NC1214.2
072100 IND-WRITE-GF-14.                                                 NC1214.2
072200     MOVE "IND-TEST-GF-14" TO PAR-NAME.                           NC1214.2
072300     PERFORM PRINT-DETAIL.                                        NC1214.2
072400 IND-INIT-GF-15.                                                  NC1214.2
072500     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
072600     MOVE "DIVIDE ROUNDED" TO FEATURE.                            NC1214.2
072700     MOVE 8.1 TO NUM-9V9.                                         NC1214.2
072800     SET INDEX1 TO 9.                                             NC1214.2
072900 IND-TEST-GF-15-0.                                                NC1214.2
073000     DIVIDE TABLE1-NUM (INDEX1) INTO NUM-9V9 ROUNDED.             NC1214.2
073100 IND-TEST-GF-15-1.                                                NC1214.2
073200     IF NUM-9V9 EQUAL TO 1.6                                      NC1214.2
073300        PERFORM PASS                                              NC1214.2
073400        GO TO IND-WRITE-GF-15.                                    NC1214.2
073500     GO TO IND-FAIL-GF-15.                                        NC1214.2
073600 IND-DELETE-GF-15.                                                NC1214.2
073700     PERFORM DE-LETE.                                             NC1214.2
073800     GO TO IND-WRITE-GF-15.                                       NC1214.2
073900 IND-FAIL-GF-15.                                                  NC1214.2
074000     PERFORM FAIL.                                                NC1214.2
074100     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
074200     MOVE 1.6 TO CORRECT-14V4.                                    NC1214.2
074300 IND-WRITE-GF-15.                                                 NC1214.2
074400     MOVE "IND-TEST-GF-15" TO PAR-NAME.                           NC1214.2
074500     PERFORM PRINT-DETAIL.                                        NC1214.2
074600 IND-INIT-GF-16.                                                  NC1214.2
074700     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
074800     MOVE "DIVIDE ON SIZE ERROR" TO FEATURE.                      NC1214.2
074900     MOVE 9.9 TO NUM-9V9.                                         NC1214.2
075000     SET INDEX1 TO 8.                                             NC1214.2
075100 IND-TEST-GF-16-1.                                                NC1214.2
075200     DIVIDE TABLE1-NUM (INDEX1) INTO NUM-9V9 ON SIZE ERROR        NC1214.2
075300        PERFORM PASS                                              NC1214.2
075400        GO TO IND-WRITE-GF-16-1.                                  NC1214.2
075500     GO TO IND-FAIL-GF-16-1.                                      NC1214.2
075600 IND-DELETE-GF-16-1.                                              NC1214.2
075700     PERFORM DE-LETE.                                             NC1214.2
075800     GO TO IND-WRITE-GF-16-1.                                     NC1214.2
075900 IND-FAIL-GF-16-1.                                                NC1214.2
076000     PERFORM FAIL.                                                NC1214.2
076100     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
076200     MOVE "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.          NC1214.2
076300 IND-WRITE-GF-16-1.                                               NC1214.2
076400     MOVE "IND-TEST-GF-16-1" TO PAR-NAME.                         NC1214.2
076500     PERFORM PRINT-DETAIL.                                        NC1214.2
076600 IND-TEST-GF-16-2.                                                NC1214.2
076700     MOVE 9.9 TO NUM-9V9.                                         NC1214.2
076800     IF      NUM-9V9 = 9.9                                        NC1214.2
076900             PERFORM PASS                                         NC1214.2
077000             GO TO   IND-WRITE-GF-16-2.                           NC1214.2
077100     GO TO   IND-FAIL-GF-16-2.                                    NC1214.2
077200 IND-DELETE-GF-16-2.                                              NC1214.2
077300     PERFORM DE-LETE.                                             NC1214.2
077400     GO TO IND-WRITE-GF-16-2.                                     NC1214.2
077500 IND-FAIL-GF-16-2.                                                NC1214.2
077600     PERFORM FAIL.                                                NC1214.2
077700     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
077800     MOVE 9.9 TO CORRECT-14V4.                                    NC1214.2
077900     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1214.2
078000 IND-WRITE-GF-16-2.                                               NC1214.2
078100     MOVE "IND-TEST-GF-16-2" TO PAR-NAME.                         NC1214.2
078200     PERFORM PRINT-DETAIL.                                        NC1214.2
078300 IND-INIT-GF-17.                                                  NC1214.2
078400     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
078500     MOVE "DIVIDE INTO" TO FEATURE.                               NC1214.2
078600     PERFORM BUILD-TABLE2.                                        NC1214.2
078700     SET INDEX1 TO 4.                                             NC1214.2
078800     SET INDEX2 TO 2.                                             NC1214.2
078900 IND-TEST-GF-17-0.                                                NC1214.2
079000     DIVIDE TABLE1-NUM (INDEX1) INTO TABLE2-NUM (INDEX2).         NC1214.2
079100 IND-TEST-GF-17-1.                                                NC1214.2
079200     IF TABLE2-NUM (INDEX2) EQUAL TO 2.0                          NC1214.2
079300        PERFORM PASS                                              NC1214.2
079400        GO TO IND-WRITE-GF-17.                                    NC1214.2
079500     GO TO IND-FAIL-GF-17.                                        NC1214.2
079600 IND-DELETE-GF-17.                                                NC1214.2
079700     PERFORM DE-LETE.                                             NC1214.2
079800     GO TO IND-WRITE-GF-17.                                       NC1214.2
079900 IND-FAIL-GF-17.                                                  NC1214.2
080000     PERFORM FAIL.                                                NC1214.2
080100     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1214.2
080200     MOVE 2.0 TO CORRECT-14V4.                                    NC1214.2
080300 IND-WRITE-GF-17.                                                 NC1214.2
080400     MOVE "IND-TEST-GF-17" TO PAR-NAME.                           NC1214.2
080500     PERFORM PRINT-DETAIL.                                        NC1214.2
080600 IND-INIT-GF-18.                                                  NC1214.2
080700     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
080800     MOVE "DIVIDE ROUNDED" TO FEATURE.                            NC1214.2
080900     PERFORM BUILD-TABLE2.                                        NC1214.2
081000     SET INDEX1 TO 9.                                             NC1214.2
081100     SET INDEX2 TO 5.                                             NC1214.2
081200 IND-TEST-GF-18-0.                                                NC1214.2
081300     DIVIDE TABLE1-NUM (INDEX1) INTO TABLE2-NUM (INDEX2) ROUNDED. NC1214.2
081400 IND-TEST-GF-18-1.                                                NC1214.2
081500     IF TABLE2-NUM (INDEX2) EQUAL TO 1.9                          NC1214.2
081600        PERFORM PASS                                              NC1214.2
081700        GO TO IND-WRITE-GF-18.                                    NC1214.2
081800     GO TO IND-FAIL-GF-18.                                        NC1214.2
081900 IND-DELETE-GF-18.                                                NC1214.2
082000     PERFORM DE-LETE.                                             NC1214.2
082100     GO TO IND-WRITE-GF-18.                                       NC1214.2
082200 IND-FAIL-GF-18.                                                  NC1214.2
082300     PERFORM FAIL.                                                NC1214.2
082400     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1214.2
082500     MOVE 1.9 TO CORRECT-14V4.                                    NC1214.2
082600 IND-WRITE-GF-18.                                                 NC1214.2
082700     MOVE "IND-TEST-GF-18" TO PAR-NAME.                           NC1214.2
082800     PERFORM PRINT-DETAIL.                                        NC1214.2
082900 IND-INIT-GF-19.                                                  NC1214.2
083000     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
083100     MOVE "DIVIDE ON SIZE ERROR" TO FEATURE.                      NC1214.2
083200     PERFORM BUILD-TABLE2.                                        NC1214.2
083300     SET INDEX1 TO 8.                                             NC1214.2
083400     SET INDEX2 TO 5.                                             NC1214.2
083500 IND-TEST-GF-19-1.                                                NC1214.2
083600     DIVIDE TABLE1-NUM (INDEX1) INTO TABLE2-NUM (INDEX2)          NC1214.2
083700        ON SIZE ERROR                                             NC1214.2
083800        PERFORM PASS                                              NC1214.2
083900        GO TO IND-WRITE-GF-19-1.                                  NC1214.2
084000     GO TO IND-FAIL-GF-19-1.                                      NC1214.2
084100 IND-DELETE-GF-19-1.                                              NC1214.2
084200     PERFORM DE-LETE.                                             NC1214.2
084300     GO TO IND-WRITE-GF-19-1.                                     NC1214.2
084400 IND-FAIL-GF-19-1.                                                NC1214.2
084500     PERFORM FAIL.                                                NC1214.2
084600     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1214.2
084700     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1214.2
084800 IND-WRITE-GF-19-1.                                               NC1214.2
084900     MOVE "IND-TEST-GF-19-1" TO PAR-NAME.                         NC1214.2
085000     PERFORM PRINT-DETAIL.                                        NC1214.2
085100 IND-TEST-GF-19-2.                                                NC1214.2
085200     IF      TABLE2-NUM (INDEX2) = 9.7                            NC1214.2
085300             PERFORM PASS                                         NC1214.2
085400             GO TO   IND-WRITE-GF-19-2.                           NC1214.2
085500     GO TO   IND-FAIL-GF-19-2.                                    NC1214.2
085600 IND-DELETE-GF-19-2.                                              NC1214.2
085700     PERFORM DE-LETE.                                             NC1214.2
085800     GO TO   IND-WRITE-GF-19-2.                                   NC1214.2
085900 IND-FAIL-GF-19-2.                                                NC1214.2
086000     PERFORM FAIL.                                                NC1214.2
086100     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1214.2
086200     MOVE  9.7 TO CORRECT-14V4.                                   NC1214.2
086300     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1214.2
086400 IND-WRITE-GF-19-2.                                               NC1214.2
086500     MOVE "IND-TEST-GF-19-2" TO PAR-NAME.                         NC1214.2
086600     PERFORM PRINT-DETAIL.                                        NC1214.2
086700 IND-INIT-GF-22.                                                  NC1214.2
086800     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
086900     MOVE "DIVIDE INTO GIVING" TO FEATURE.                        NC1214.2
087000     MOVE ZERO TO NUM-9V9.                                        NC1214.2
087100     SET INDEX1 TO 4.                                             NC1214.2
087200 IND-TEST-GF-22-0.                                                NC1214.2
087300     DIVIDE 3 INTO TABLE1-NUM (INDEX1) GIVING NUM-9V9.            NC1214.2
087400 IND-TEST-GF-22-1.                                                NC1214.2
087500     IF NUM-9V9 EQUAL TO 1.0                                      NC1214.2
087600        PERFORM PASS                                              NC1214.2
087700        GO TO IND-WRITE-GF-22.                                    NC1214.2
087800     GO TO IND-FAIL-GF-22.                                        NC1214.2
087900 IND-DELETE-GF-22.                                                NC1214.2
088000     PERFORM DE-LETE.                                             NC1214.2
088100     GO TO IND-WRITE-GF-22.                                       NC1214.2
088200 IND-FAIL-GF-22.                                                  NC1214.2
088300     PERFORM FAIL.                                                NC1214.2
088400     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
088500     MOVE 1.0 TO CORRECT-14V4.                                    NC1214.2
088600 IND-WRITE-GF-22.                                                 NC1214.2
088700     MOVE "IND-TEST-GF-22" TO PAR-NAME.                           NC1214.2
088800     PERFORM PRINT-DETAIL.                                        NC1214.2
088900 IND-INIT-GF-23.                                                  NC1214.2
089000     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
089100     MOVE "DIVIDE INTO GIVING" TO FEATURE.                        NC1214.2
089200     PERFORM BUILD-TABLE2.                                        NC1214.2
089300     MOVE ZERO TO NUM-9V9.                                        NC1214.2
089400     SET INDEX1 TO 4.                                             NC1214.2
089500     SET INDEX2 TO 2.                                             NC1214.2
089600 IND-TEST-GF-23-0.                                                NC1214.2
089700     DIVIDE TABLE1-NUM (INDEX1) INTO TABLE2-NUM (INDEX2)          NC1214.2
089800        GIVING NUM-9V9.                                           NC1214.2
089900 IND-TEST-GF-23-1.                                                NC1214.2
090000     IF NUM-9V9 EQUAL TO 2.0                                      NC1214.2
090100        PERFORM PASS                                              NC1214.2
090200        GO TO IND-WRITE-GF-23.                                    NC1214.2
090300     GO TO IND-FAIL-GF-23.                                        NC1214.2
090400 IND-DELETE-GF-23.                                                NC1214.2
090500     PERFORM DE-LETE.                                             NC1214.2
090600     GO TO IND-WRITE-GF-23.                                       NC1214.2
090700 IND-FAIL-GF-23.                                                  NC1214.2
090800     PERFORM FAIL.                                                NC1214.2
090900     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
091000     MOVE 2.0 TO CORRECT-14V4.                                    NC1214.2
091100 IND-WRITE-GF-23.                                                 NC1214.2
091200     MOVE "IND-TEST-GF-23" TO PAR-NAME.                           NC1214.2
091300     PERFORM PRINT-DETAIL.                                        NC1214.2
091400 IND-INIT-GF-24.                                                  NC1214.2
091500     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
091600     MOVE "DIVIDE INTO GIVING" TO FEATURE.                        NC1214.2
091700     PERFORM BUILD-TABLE2.                                        NC1214.2
091800     MOVE ZERO TO TABLE3.                                         NC1214.2
091900     SET INDEX1 TO 4.                                             NC1214.2
092000     SET INDEX2 TO 2.                                             NC1214.2
092100     SET INDEX3 TO 3.                                             NC1214.2
092200 IND-TEST-GF-24-0.                                                NC1214.2
092300     DIVIDE TABLE1-NUM (INDEX1) INTO TABLE2-NUM (INDEX2)          NC1214.2
092400        GIVING TABLE3-NUM (INDEX3).                               NC1214.2
092500 IND-TEST-GF-24-1.                                                NC1214.2
092600     IF TABLE3-NUM (INDEX3) EQUAL TO 2.0                          NC1214.2
092700        PERFORM PASS                                              NC1214.2
092800        GO TO IND-WRITE-GF-24.                                    NC1214.2
092900     GO TO IND-FAIL-GF-24.                                        NC1214.2
093000 IND-DELETE-GF-24.                                                NC1214.2
093100     PERFORM DE-LETE.                                             NC1214.2
093200     GO TO IND-WRITE-GF-24.                                       NC1214.2
093300 IND-FAIL-GF-24.                                                  NC1214.2
093400     PERFORM FAIL.                                                NC1214.2
093500     MOVE TABLE3-NUM (INDEX3) TO COMPUTED-14V4.                   NC1214.2
093600     MOVE 2.0 TO CORRECT-14V4.                                    NC1214.2
093700 IND-WRITE-GF-24.                                                 NC1214.2
093800     MOVE "IND-TEST-GF-24" TO PAR-NAME.                           NC1214.2
093900     PERFORM PRINT-DETAIL.                                        NC1214.2
094000 IND-INIT-GF-25.                                                  NC1214.2
094100     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
094200     MOVE "DIVIDE INTO" TO FEATURE.                               NC1214.2
094300     PERFORM BUILD-TABLE4.                                        NC1214.2
094400     SET INDEX4-1 TO 3.                                           NC1214.2
094500     SET INDEX4-2 TO 1.                                           NC1214.2
094600 IND-TEST-GF-25-0.                                                NC1214.2
094700     DIVIDE TABLE4-NUM2 (1  2) INTO                               NC1214.2
094800        TABLE4-NUM2 (INDEX4-1  INDEX4-2).                         NC1214.2
094900 IND-TEST-GF-25-1.                                                NC1214.2
095000     IF TABLE4-NUM2 (INDEX4-1  INDEX4-2) EQUAL TO 7               NC1214.2
095100        PERFORM PASS                                              NC1214.2
095200        GO TO IND-WRITE-GF-25.                                    NC1214.2
095300     GO TO IND-FAIL-GF-25.                                        NC1214.2
095400 IND-DELETE-GF-25.                                                NC1214.2
095500     PERFORM DE-LETE.                                             NC1214.2
095600     GO TO IND-WRITE-GF-25.                                       NC1214.2
095700 IND-FAIL-GF-25.                                                  NC1214.2
095800     PERFORM FAIL.                                                NC1214.2
095900     MOVE TABLE4-NUM2 (INDEX4-1  INDEX4-2) TO COMPUTED-14V4.      NC1214.2
096000     MOVE 7 TO CORRECT-14V4.                                      NC1214.2
096100 IND-WRITE-GF-25.                                                 NC1214.2
096200     MOVE "IND-TEST-GF-25" TO PAR-NAME.                           NC1214.2
096300     PERFORM PRINT-DETAIL.                                        NC1214.2
096400 IND-INIT-GF-20.                                                  NC1214.2
096500     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
096600     MOVE "DIVIDE INTO" TO FEATURE.                               NC1214.2
096700     MOVE 9.5 TO NUM-9V9.                                         NC1214.2
096800     SET INDEX1 TO 8.                                             NC1214.2
096900 IND-TEST-GF-20-0.                                                NC1214.2
097000     DIVIDE TABLE1-NUM (INDEX1 + 1) INTO NUM-9V9.                 NC1214.2
097100 IND-TEST-GF-20-1.                                                NC1214.2
097200     IF NUM-9V9 EQUAL TO 1.9                                      NC1214.2
097300        PERFORM PASS                                              NC1214.2
097400        GO TO IND-WRITE-GF-20.                                    NC1214.2
097500     GO TO IND-FAIL-GF-20.                                        NC1214.2
097600 IND-DELETE-GF-20.                                                NC1214.2
097700     PERFORM DE-LETE.                                             NC1214.2
097800     GO TO IND-WRITE-GF-20.                                       NC1214.2
097900 IND-FAIL-GF-20.                                                  NC1214.2
098000     PERFORM FAIL.                                                NC1214.2
098100     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
098200     MOVE 1.9 TO CORRECT-14V4.                                    NC1214.2
098300 IND-WRITE-GF-20.                                                 NC1214.2
098400     MOVE "IND-TEST-GF-20" TO PAR-NAME.                           NC1214.2
098500     PERFORM PRINT-DETAIL.                                        NC1214.2
098600 IND-INIT-GF-21.                                                  NC1214.2
098700     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
098800     MOVE "DIVIDE INTO" TO FEATURE.                               NC1214.2
098900     PERFORM BUILD-TABLE2.                                        NC1214.2
099000     SET INDEX1 TO 6.                                             NC1214.2
099100     SET INDEX2 TO 4.                                             NC1214.2
099200 IND-TEST-GF-21-0.                                                NC1214.2
099300     DIVIDE TABLE1-NUM (INDEX1 - 2) INTO                          NC1214.2
099400        TABLE2-NUM (INDEX2 + 2).                                  NC1214.2
099500 IND-TEST-GF-21-1.                                                NC1214.2
099600     IF TABLE2-NUM (INDEX2 + 2) EQUAL TO .4                       NC1214.2
099700        PERFORM PASS                                              NC1214.2
099800        GO TO IND-WRITE-GF-21.                                    NC1214.2
099900     GO TO IND-FAIL-GF-21.                                        NC1214.2
100000 IND-DELETE-GF-21.                                                NC1214.2
100100     PERFORM DE-LETE.                                             NC1214.2
100200     GO TO IND-WRITE-GF-21.                                       NC1214.2
100300 IND-FAIL-GF-21.                                                  NC1214.2
100400     PERFORM FAIL.                                                NC1214.2
100500     MOVE TABLE2-NUM (INDEX2 + 2) TO COMPUTED-14V4.               NC1214.2
100600     MOVE .4 TO CORRECT-14V4.                                     NC1214.2
100700 IND-WRITE-GF-21.                                                 NC1214.2
100800     MOVE "IND-TEST-GF-21" TO PAR-NAME.                           NC1214.2
100900     PERFORM PRINT-DETAIL.                                        NC1214.2
101000 IND-INIT-GF-26.                                                  NC1214.2
101100     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
101200     MOVE "DIVIDE INTO GIVING" TO FEATURE.                        NC1214.2
101300     PERFORM BUILD-TABLE2.                                        NC1214.2
101400     MOVE ZERO TO NUM-9V9.                                        NC1214.2
101500     SET INDEX1 TO 6.                                             NC1214.2
101600     SET INDEX2 TO 1.                                             NC1214.2
101700 IND-TEST-GF-26-0.                                                NC1214.2
101800     DIVIDE TABLE1-NUM (INDEX1 - 2) INTO                          NC1214.2
101900        TABLE2-NUM (INDEX2 + 1) GIVING NUM-9V9.                   NC1214.2
102000 IND-TEST-GF-26-1.                                                NC1214.2
102100     IF NUM-9V9 EQUAL TO 2.0                                      NC1214.2
102200        PERFORM PASS                                              NC1214.2
102300        GO TO IND-WRITE-GF-26.                                    NC1214.2
102400     GO TO IND-FAIL-GF-26.                                        NC1214.2
102500 IND-DELETE-GF-26.                                                NC1214.2
102600     PERFORM DE-LETE.                                             NC1214.2
102700     GO TO IND-WRITE-GF-26.                                       NC1214.2
102800 IND-FAIL-GF-26.                                                  NC1214.2
102900     PERFORM FAIL.                                                NC1214.2
103000     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
103100     MOVE 2.0 TO CORRECT-14V4.                                    NC1214.2
103200 IND-WRITE-GF-26.                                                 NC1214.2
103300     MOVE "IND-TEST-GF-26" TO PAR-NAME.                           NC1214.2
103400     PERFORM PRINT-DETAIL.                                        NC1214.2
103500 IND-INIT-GF-27.                                                  NC1214.2
103600     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
103700     MOVE "DIVIDE INTO GIVING" TO FEATURE.                        NC1214.2
103800     PERFORM BUILD-TABLE2.                                        NC1214.2
103900     MOVE ZERO TO TABLE3.                                         NC1214.2
104000     SET INDEX1 TO 8.                                             NC1214.2
104100     SET INDEX2 TO 1.                                             NC1214.2
104200     SET INDEX3 TO 4.                                             NC1214.2
104300 IND-TEST-GF-27-0.                                                NC1214.2
104400     DIVIDE TABLE1-NUM (INDEX1 - 2) INTO                          NC1214.2
104500        TABLE2-NUM (INDEX2 + 1) GIVING TABLE3-NUM (INDEX3 - 1).   NC1214.2
104600 IND-TEST-GF-27-1.                                                NC1214.2
104700     IF TABLE3-NUM (INDEX3 - 1) EQUAL TO 4                        NC1214.2
104800        PERFORM PASS                                              NC1214.2
104900        GO TO IND-WRITE-GF-27.                                    NC1214.2
105000     GO TO IND-FAIL-GF-27.                                        NC1214.2
105100 IND-DELETE-GF-27.                                                NC1214.2
105200     PERFORM DE-LETE.                                             NC1214.2
105300     GO TO IND-WRITE-GF-27.                                       NC1214.2
105400 IND-FAIL-GF-27.                                                  NC1214.2
105500     PERFORM FAIL.                                                NC1214.2
105600     MOVE TABLE3-NUM (INDEX3 - 1) TO COMPUTED-14V4.               NC1214.2
105700     MOVE 4.0 TO CORRECT-14V4.                                    NC1214.2
105800 IND-WRITE-GF-27.                                                 NC1214.2
105900     MOVE "IND-TEST-GF-27" TO PAR-NAME.                           NC1214.2
106000     PERFORM PRINT-DETAIL.                                        NC1214.2
106100 IND-INIT-GF-28.                                                  NC1214.2
106200     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
106300     MOVE "DIVIDE BY GIVING" TO FEATURE.                          NC1214.2
106400     PERFORM BUILD-TABLE4.                                        NC1214.2
106500     SET INDEX4-1 TO 2.                                           NC1214.2
106600     SET INDEX4-2 TO 1.                                           NC1214.2
106700 IND-TEST-GF-28-0.                                                NC1214.2
106800     DIVIDE TABLE4-NUM2 (INDEX4-1  INDEX4-2) BY                   NC1214.2
106900        TABLE4-NUM2 (1  3) GIVING TABLE4-NUM2 (3  3).             NC1214.2
107000 IND-TEST-GF-28-1.                                                NC1214.2
107100     IF TABLE4-NUM2 (3  3) EQUAL TO 4                             NC1214.2
107200        PERFORM PASS                                              NC1214.2
107300        GO TO IND-WRITE-GF-28.                                    NC1214.2
107400     GO TO IND-FAIL-GF-28.                                        NC1214.2
107500 IND-DELETE-GF-28.                                                NC1214.2
107600     PERFORM DE-LETE.                                             NC1214.2
107700     GO TO IND-WRITE-GF-28.                                       NC1214.2
107800 IND-FAIL-GF-28.                                                  NC1214.2
107900     PERFORM FAIL.                                                NC1214.2
108000     MOVE TABLE4-NUM2 (3  3) TO COMPUTED-14V4.                    NC1214.2
108100     MOVE 4.0 TO CORRECT-14V4.                                    NC1214.2
108200 IND-WRITE-GF-28.                                                 NC1214.2
108300     MOVE "IND-TEST-GF-28" TO PAR-NAME.                           NC1214.2
108400     PERFORM PRINT-DETAIL.                                        NC1214.2
108500 IND-INIT-GF-29.                                                  NC1214.2
108600     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
108700     MOVE "DIVIDE BY GIVING" TO FEATURE.                          NC1214.2
108800     PERFORM BUILD-TABLE2.                                        NC1214.2
108900     MOVE ZERO TO NUM-9V9.                                        NC1214.2
109000     SET INDEX2 TO 2.                                             NC1214.2
109100 IND-TEST-GF-29-0.                                                NC1214.2
109200     DIVIDE TABLE2-NUM (INDEX2) BY TABLE2-NUM (INDEX2 + 1)        NC1214.2
109300        GIVING NUM-9V9.                                           NC1214.2
109400 IND-TEST-GF-29-1.                                                NC1214.2
109500     IF NUM-9V9 EQUAL TO 2.0                                      NC1214.2
109600        PERFORM PASS                                              NC1214.2
109700        GO TO IND-WRITE-GF-29.                                    NC1214.2
109800     GO TO IND-FAIL-GF-29.                                        NC1214.2
109900 IND-DELETE-GF-29.                                                NC1214.2
110000     PERFORM DE-LETE.                                             NC1214.2
110100     GO TO IND-WRITE-GF-29.                                       NC1214.2
110200 IND-FAIL-GF-29.                                                  NC1214.2
110300     PERFORM FAIL.                                                NC1214.2
110400     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
110500     MOVE 2.0 TO CORRECT-14V4.                                    NC1214.2
110600 IND-WRITE-GF-29.                                                 NC1214.2
110700     MOVE "IND-TEST-GF-29" TO PAR-NAME.                           NC1214.2
110800     PERFORM PRINT-DETAIL.                                        NC1214.2
110900 IND-INIT-GF-30.                                                  NC1214.2
111000     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
111100     MOVE "DIVIDE BY GIVING" TO FEATURE.                          NC1214.2
111200     PERFORM BUILD-TABLE2.                                        NC1214.2
111300     SET INDEX2 TO 6.                                             NC1214.2
111400 IND-TEST-GF-30-0.                                                NC1214.2
111500     DIVIDE TABLE2-NUM (INDEX2) BY TABLE2-NUM (INDEX2 - 3)        NC1214.2
111600        GIVING TABLE2-NUM (INDEX2 - 1).                           NC1214.2
111700 IND-TEST-GF-30-1.                                                NC1214.2
111800     IF TABLE2-NUM (INDEX2 - 1) EQUAL TO 0.4                      NC1214.2
111900        PERFORM PASS                                              NC1214.2
112000        GO TO IND-WRITE-GF-30.                                    NC1214.2
112100     GO TO IND-FAIL-GF-30.                                        NC1214.2
112200 IND-DELETE-GF-30.                                                NC1214.2
112300     PERFORM DE-LETE.                                             NC1214.2
112400     GO TO IND-WRITE-GF-30.                                       NC1214.2
112500 IND-FAIL-GF-30.                                                  NC1214.2
112600     PERFORM FAIL.                                                NC1214.2
112700     MOVE TABLE2-NUM (INDEX2 - 1) TO COMPUTED-14V4.               NC1214.2
112800     MOVE 0.4 TO CORRECT-14V4.                                    NC1214.2
112900 IND-WRITE-GF-30.                                                 NC1214.2
113000     MOVE "IND-TEST-GF-30" TO PAR-NAME.                           NC1214.2
113100     PERFORM PRINT-DETAIL.                                        NC1214.2
113200 IND-INIT-GF-31.                                                  NC1214.2
113300     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
113400     MOVE "DIVIDE BY GIVING" TO FEATURE.                          NC1214.2
113500     MOVE ZERO TO NUM-9V9.                                        NC1214.2
113600     SET INDEX1 TO 1.                                             NC1214.2
113700 IND-TEST-GF-31-0.                                                NC1214.2
113800     DIVIDE 8 BY TABLE1-NUM (INDEX1) GIVING NUM-9V9.              NC1214.2
113900 IND-TEST-GF-31-1.                                                NC1214.2
114000     IF NUM-9V9 EQUAL TO 2.0                                      NC1214.2
114100        PERFORM PASS                                              NC1214.2
114200        GO TO IND-WRITE-GF-31.                                    NC1214.2
114300     GO TO IND-FAIL-GF-31.                                        NC1214.2
114400 IND-DELETE-GF-31.                                                NC1214.2
114500     PERFORM DE-LETE.                                             NC1214.2
114600     GO TO IND-WRITE-GF-31.                                       NC1214.2
114700 IND-FAIL-GF-31.                                                  NC1214.2
114800     PERFORM FAIL.                                                NC1214.2
114900     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
115000     MOVE 2.0 TO CORRECT-14V4.                                    NC1214.2
115100 IND-WRITE-GF-31.                                                 NC1214.2
115200     MOVE "IND-TEST-GF-31" TO PAR-NAME.                           NC1214.2
115300     PERFORM PRINT-DETAIL.                                        NC1214.2
115400 IND-INIT-GF-32.                                                  NC1214.2
115500     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
115600     MOVE "DIVIDE BY GIVING" TO FEATURE.                          NC1214.2
115700     MOVE ZERO TO NUM-9V9.                                        NC1214.2
115800     PERFORM BUILD-TABLE2.                                        NC1214.2
115900     SET INDEX1 TO 3.                                             NC1214.2
116000     SET INDEX2 TO 4.                                             NC1214.2
116100 IND-TEST-GF-32-0.                                                NC1214.2
116200     DIVIDE TABLE1-NUM (INDEX1) BY TABLE2-NUM (INDEX2)            NC1214.2
116300        GIVING NUM-9V9.                                           NC1214.2
116400 IND-TEST-GF-32-1.                                                NC1214.2
116500     IF NUM-9V9 EQUAL TO 3.5                                      NC1214.2
116600        PERFORM PASS                                              NC1214.2
116700        GO TO IND-WRITE-GF-32.                                    NC1214.2
116800     GO TO IND-FAIL-GF-32.                                        NC1214.2
116900 IND-DELETE-GF-32.                                                NC1214.2
117000     PERFORM DE-LETE.                                             NC1214.2
117100     GO TO IND-WRITE-GF-32.                                       NC1214.2
117200 IND-FAIL-GF-32.                                                  NC1214.2
117300     PERFORM FAIL.                                                NC1214.2
117400     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1214.2
117500     MOVE 3.5 TO CORRECT-14V4.                                    NC1214.2
117600 IND-WRITE-GF-32.                                                 NC1214.2
117700     MOVE "IND-TEST-GF-32" TO PAR-NAME.                           NC1214.2
117800     PERFORM PRINT-DETAIL.                                        NC1214.2
117900 IND-INIT-GF-33.                                                  NC1214.2
118000     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
118100     MOVE "DIVIDE BY GIVING" TO FEATURE.                          NC1214.2
118200     MOVE ZERO TO TABLE3.                                         NC1214.2
118300     PERFORM BUILD-TABLE2.                                        NC1214.2
118400     SET INDEX1 TO 4.                                             NC1214.2
118500     SET INDEX2 TO 3.                                             NC1214.2
118600     SET INDEX3 TO 2.                                             NC1214.2
118700 IND-TEST-GF-33-0.                                                NC1214.2
118800     DIVIDE TABLE1-NUM (INDEX1) BY TABLE2-NUM (INDEX2)            NC1214.2
118900        GIVING TABLE3-NUM (INDEX3).                               NC1214.2
119000 IND-TEST-GF-33-1.                                                NC1214.2
119100     IF TABLE3-NUM (INDEX3) EQUAL TO 1.0                          NC1214.2
119200        PERFORM PASS                                              NC1214.2
119300        GO TO IND-WRITE-GF-33.                                    NC1214.2
119400     GO TO IND-FAIL-GF-33.                                        NC1214.2
119500 IND-DELETE-GF-33.                                                NC1214.2
119600     PERFORM DE-LETE.                                             NC1214.2
119700     GO TO IND-WRITE-GF-33.                                       NC1214.2
119800 IND-FAIL-GF-33.                                                  NC1214.2
119900     PERFORM FAIL.                                                NC1214.2
120000     MOVE TABLE3-NUM (INDEX3) TO COMPUTED-14V4.                   NC1214.2
120100     MOVE 1.0 TO CORRECT-14V4.                                    NC1214.2
120200 IND-WRITE-GF-33.                                                 NC1214.2
120300     MOVE "IND-TEST-GF-33" TO PAR-NAME.                           NC1214.2
120400     PERFORM PRINT-DETAIL.                                        NC1214.2
120500 IND-INIT-GF-34.                                                  NC1214.2
120600     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
120700     MOVE "PERFORM" TO FEATURE.                                   NC1214.2
120800     MOVE ZERO TO NUM-9V9.                                        NC1214.2
120900     SET INDEX5 TO 1.                                             NC1214.2
121000 IND-TEST-GF-34-0.                                                NC1214.2
121100     PERFORM PARAGRAPH-A TABLE5-NUM (INDEX5) TIMES.               NC1214.2
121200 IND-TEST-GF-34-1.                                                NC1214.2
121300     IF NUM-9V9 EQUAL TO 3                                        NC1214.2
121400        PERFORM PASS                                              NC1214.2
121500        ELSE GO TO IND-FAIL-GF-34.                                NC1214.2
121600     GO TO IND-WRITE-GF-34.                                       NC1214.2
121700 IND-DELETE-GF-34.                                                NC1214.2
121800     PERFORM DE-LETE.                                             NC1214.2
121900     GO TO IND-WRITE-GF-34.                                       NC1214.2
122000 IND-FAIL-GF-34.                                                  NC1214.2
122100     PERFORM FAIL.                                                NC1214.2
122200     MOVE NUM-9V9 TO COMPUTED-18V0.                               NC1214.2
122300     MOVE 3 TO CORRECT-18V0.                                      NC1214.2
122400 IND-WRITE-GF-34.                                                 NC1214.2
122500     MOVE "IND-TEST-GF-34" TO PAR-NAME.                           NC1214.2
122600     PERFORM PRINT-DETAIL.                                        NC1214.2
122700 IND-INIT-GF-35.                                                  NC1214.2
122800     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
122900     MOVE "PERFORM THRU" TO FEATURE.                              NC1214.2
123000     MOVE ZERO TO NUM-9V9.                                        NC1214.2
123100     SET INDEX5 TO 2.                                             NC1214.2
123200 IND-TEST-GF-35-0.                                                NC1214.2
123300     PERFORM PARAGRAPH-A THRU PARAGRAPH-B                         NC1214.2
123400        TABLE5-NUM (INDEX5) TIMES.                                NC1214.2
123500 IND-TEST-GF-35-1.                                                NC1214.2
123600     IF NUM-9V9 EQUAL TO 4                                        NC1214.2
123700        PERFORM PASS                                              NC1214.2
123800        ELSE GO TO IND-FAIL-GF-35.                                NC1214.2
123900     GO TO IND-WRITE-GF-35.                                       NC1214.2
124000 IND-DELETE-GF-35.                                                NC1214.2
124100     PERFORM DE-LETE.                                             NC1214.2
124200     GO TO IND-WRITE-GF-35.                                       NC1214.2
124300 IND-FAIL-GF-35.                                                  NC1214.2
124400     PERFORM FAIL.                                                NC1214.2
124500     MOVE NUM-9V9 TO COMPUTED-18V0.                               NC1214.2
124600     MOVE 4 TO CORRECT-18V0.                                      NC1214.2
124700 IND-WRITE-GF-35.                                                 NC1214.2
124800     MOVE "IND-TEST-GF-35" TO PAR-NAME.                           NC1214.2
124900     PERFORM PRINT-DETAIL.                                        NC1214.2
125000 IND-INIT-GF-36.                                                  NC1214.2
125100     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
125200     MOVE "DISPLAY" TO FEATURE.                                   NC1214.2
125300     MOVE "RESULTS MUST BE" TO RE-MARK.                           NC1214.2
125400     MOVE "LITERAL-01" TO CORRECT-A.                              NC1214.2
125500     SET INDEX6 TO 1.                                             NC1214.2
125600 IND-TEST-GF-36.                                                  NC1214.2
125700     DISPLAY "  ".                                                NC1214.2
125800     DISPLAY TABLE6-REC (INDEX6).                                 NC1214.2
125900     PERFORM INSPT.                                               NC1214.2
126000     GO TO IND-WRITE-GF-36.                                       NC1214.2
126100 IND-DELETE-GF-36.                                                NC1214.2
126200     PERFORM DE-LETE.                                             NC1214.2
126300 IND-WRITE-GF-36.                                                 NC1214.2
126400     MOVE "IND-TEST-GF-36" TO PAR-NAME.                           NC1214.2
126500     PERFORM PRINT-DETAIL.                                        NC1214.2
126600 IND-INIT-GF-37.                                                  NC1214.2
126700     MOVE  "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                 NC1214.2
126800     MOVE "DISPLAY" TO FEATURE.                                   NC1214.2
126900     MOVE "VISUALLY CHECKED" TO RE-MARK.                          NC1214.2
127000     MOVE "0123456789" TO CORRECT-A.                              NC1214.2
127100     SET INDEX6 TO 1.                                             NC1214.2
127200 IND-TEST-GF-37.                                                  NC1214.2
127300     DISPLAY TABLE6-REC (INDEX6 + 1).                             NC1214.2
127400     PERFORM INSPT.                                               NC1214.2
127500     GO TO IND-WRITE-GF-37.                                       NC1214.2
127600 IND-DELETE-GF-37.                                                NC1214.2
127700     PERFORM DE-LETE.                                             NC1214.2
127800 IND-WRITE-GF-37.                                                 NC1214.2
127900     MOVE "IND-TEST-GF-37" TO PAR-NAME.                           NC1214.2
128000     PERFORM PRINT-DETAIL.                                        NC1214.2
128100     GO TO CCVS-999999.                                           NC1214.2
128200 PARAGRAPH-A.                                                     NC1214.2
128300     ADD 1 TO NUM-9V9.                                            NC1214.2
128400 PARAGRAPH-B.                                                     NC1214.2
128500     ADD 1 TO NUM-9V9.                                            NC1214.2
128600 CCVS-EXIT SECTION.                                               NC1214.2
128700 CCVS-999999.                                                     NC1214.2
128800     GO TO CLOSE-FILES.                                           NC1214.2
      *END-OF,NC121M                                                                  
