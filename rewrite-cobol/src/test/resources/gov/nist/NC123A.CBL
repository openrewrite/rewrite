      *HEADER,COBOL,NC123A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1234.2
000200 PROGRAM-ID.                                                      NC1234.2
000300     NC123A.                                                      NC1234.2
000400****************************************************************  NC1234.2
000500*                                                              *  NC1234.2
000600*    VALIDATION FOR:-                                          *  NC1234.2
000700*                                                              *  NC1234.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1234.2
000900*                                                              *  NC1234.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1234.2
001100*                                                              *  NC1234.2
001200****************************************************************  NC1234.2
001300*                                                              *  NC1234.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1234.2
001500*                                                              *  NC1234.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1234.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1234.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1234.2
001900*                                                              *  NC1234.2
002000****************************************************************  NC1234.2
002100*                                                                 NC1234.2
002200*    PROGRAM NC123A TESTS THE USE OF INDEXED IDENTIFIERS WITH     NC1234.2
002300*    FORMATS 1 AND 2 OF THE "ADD" AND "SUBTRACT" STATEMENTS AND   NC1234.2
002400*    FORMAT 2 OF THE "GO" STATEMENT.                              NC1234.2
002500*    ONE AND TWO LEVELS OF INDEXING ARE USED AS WELL AS           NC1234.2
002600*    RELATIVE INDEXING.                                           NC1234.2
002700*                                                                 NC1234.2
002800 ENVIRONMENT DIVISION.                                            NC1234.2
002900 CONFIGURATION SECTION.                                           NC1234.2
003000 SOURCE-COMPUTER.                                                 NC1234.2
003100     XXXXX082.                                                    NC1234.2
003200 OBJECT-COMPUTER.                                                 NC1234.2
003300     XXXXX083.                                                    NC1234.2
003400 INPUT-OUTPUT SECTION.                                            NC1234.2
003500 FILE-CONTROL.                                                    NC1234.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC1234.2
003700     XXXXX055.                                                    NC1234.2
003800 DATA DIVISION.                                                   NC1234.2
003900 FILE SECTION.                                                    NC1234.2
004000 FD  PRINT-FILE.                                                  NC1234.2
004100 01  PRINT-REC PICTURE X(120).                                    NC1234.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC1234.2
004300 WORKING-STORAGE SECTION.                                         NC1234.2
004400 01  TABLE1.                                                      NC1234.2
004500     02  TABLE1-NUM              PICTURE S9V99                    NC1234.2
004600                                 OCCURS 10 TIMES                  NC1234.2
004700                                 INDEXED BY INDEX1.               NC1234.2
004800 01  TABLE2.                                                      NC1234.2
004900     02  TABLE2-NUM              PICTURE 9V9                      NC1234.2
005000                                 OCCURS 6 TIMES                   NC1234.2
005100                                 INDEXED BY INDEX2.               NC1234.2
005200 01  TABLE3.                                                      NC1234.2
005300     02  TABLE3-NUM              PICTURE 99V9                     NC1234.2
005400                                 OCCURS 6 TIMES                   NC1234.2
005500                                 INDEXED BY INDEX3.               NC1234.2
005600 01  TABLE4.                                                      NC1234.2
005700     02  TABLE4-NUM1             OCCURS 3 TIMES                   NC1234.2
005800                                 INDEXED BY INDEX4-1.             NC1234.2
005900         03 TABLE4-NUM2          PICTURE 99                       NC1234.2
006000                                 OCCURS 3 TIMES                   NC1234.2
006100                                 INDEXED BY INDEX4-2.             NC1234.2
006200 01  TABLE5.                                                      NC1234.2
006300     02  TABLE5-NUM              PIC 999                          NC1234.2
006400                                 OCCURS 2 TIMES                   NC1234.2
006500                                 INDEXED BY INDEX5.               NC1234.2
006600 01  NUM-9V9                     PICTURE 9V9.                     NC1234.2
006700 01  TEST-RESULTS.                                                NC1234.2
006800     02 FILLER                   PIC X      VALUE SPACE.          NC1234.2
006900     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1234.2
007000     02 FILLER                   PIC X      VALUE SPACE.          NC1234.2
007100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1234.2
007200     02 FILLER                   PIC X      VALUE SPACE.          NC1234.2
007300     02  PAR-NAME.                                                NC1234.2
007400       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1234.2
007500       03  PARDOT-X              PIC X      VALUE SPACE.          NC1234.2
007600       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1234.2
007700     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1234.2
007800     02 RE-MARK                  PIC X(61).                       NC1234.2
007900 01  TEST-COMPUTED.                                               NC1234.2
008000     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1234.2
008100     02 FILLER                   PIC X(17)  VALUE                 NC1234.2
008200            "       COMPUTED=".                                   NC1234.2
008300     02 COMPUTED-X.                                               NC1234.2
008400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1234.2
008500     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1234.2
008600                                 PIC -9(9).9(9).                  NC1234.2
008700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1234.2
008800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1234.2
008900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1234.2
009000     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1234.2
009100         04 COMPUTED-18V0                    PIC -9(18).          NC1234.2
009200         04 FILLER                           PIC X.               NC1234.2
009300     03 FILLER PIC X(50) VALUE SPACE.                             NC1234.2
009400 01  TEST-CORRECT.                                                NC1234.2
009500     02 FILLER PIC X(30) VALUE SPACE.                             NC1234.2
009600     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1234.2
009700     02 CORRECT-X.                                                NC1234.2
009800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1234.2
009900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1234.2
010000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1234.2
010100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1234.2
010200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1234.2
010300     03      CR-18V0 REDEFINES CORRECT-A.                         NC1234.2
010400         04 CORRECT-18V0                     PIC -9(18).          NC1234.2
010500         04 FILLER                           PIC X.               NC1234.2
010600     03 FILLER PIC X(2) VALUE SPACE.                              NC1234.2
010700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1234.2
010800 01  CCVS-C-1.                                                    NC1234.2
010900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1234.2
011000-    "SS  PARAGRAPH-NAME                                          NC1234.2
011100-    "       REMARKS".                                            NC1234.2
011200     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1234.2
011300 01  CCVS-C-2.                                                    NC1234.2
011400     02 FILLER                     PIC X        VALUE SPACE.      NC1234.2
011500     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1234.2
011600     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1234.2
011700     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1234.2
011800     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1234.2
011900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1234.2
012000 01  REC-CT                        PIC 99       VALUE ZERO.       NC1234.2
012100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1234.2
012200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1234.2
012300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1234.2
012400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1234.2
012500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1234.2
012600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1234.2
012700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1234.2
012800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1234.2
012900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1234.2
013000 01  CCVS-H-1.                                                    NC1234.2
013100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1234.2
013200     02  FILLER                    PIC X(42)    VALUE             NC1234.2
013300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1234.2
013400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1234.2
013500 01  CCVS-H-2A.                                                   NC1234.2
013600   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1234.2
013700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1234.2
013800   02  FILLER                        PIC XXXX   VALUE             NC1234.2
013900     "4.2 ".                                                      NC1234.2
014000   02  FILLER                        PIC X(28)  VALUE             NC1234.2
014100            " COPY - NOT FOR DISTRIBUTION".                       NC1234.2
014200   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1234.2
014300                                                                  NC1234.2
014400 01  CCVS-H-2B.                                                   NC1234.2
014500   02  FILLER                        PIC X(15)  VALUE             NC1234.2
014600            "TEST RESULT OF ".                                    NC1234.2
014700   02  TEST-ID                       PIC X(9).                    NC1234.2
014800   02  FILLER                        PIC X(4)   VALUE             NC1234.2
014900            " IN ".                                               NC1234.2
015000   02  FILLER                        PIC X(12)  VALUE             NC1234.2
015100     " HIGH       ".                                              NC1234.2
015200   02  FILLER                        PIC X(22)  VALUE             NC1234.2
015300            " LEVEL VALIDATION FOR ".                             NC1234.2
015400   02  FILLER                        PIC X(58)  VALUE             NC1234.2
015500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1234.2
015600 01  CCVS-H-3.                                                    NC1234.2
015700     02  FILLER                      PIC X(34)  VALUE             NC1234.2
015800            " FOR OFFICIAL USE ONLY    ".                         NC1234.2
015900     02  FILLER                      PIC X(58)  VALUE             NC1234.2
016000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1234.2
016100     02  FILLER                      PIC X(28)  VALUE             NC1234.2
016200            "  COPYRIGHT   1985 ".                                NC1234.2
016300 01  CCVS-E-1.                                                    NC1234.2
016400     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1234.2
016500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1234.2
016600     02 ID-AGAIN                     PIC X(9).                    NC1234.2
016700     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1234.2
016800 01  CCVS-E-2.                                                    NC1234.2
016900     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1234.2
017000     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1234.2
017100     02 CCVS-E-2-2.                                               NC1234.2
017200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1234.2
017300         03 FILLER                   PIC X      VALUE SPACE.      NC1234.2
017400         03 ENDER-DESC               PIC X(44)  VALUE             NC1234.2
017500            "ERRORS ENCOUNTERED".                                 NC1234.2
017600 01  CCVS-E-3.                                                    NC1234.2
017700     02  FILLER                      PIC X(22)  VALUE             NC1234.2
017800            " FOR OFFICIAL USE ONLY".                             NC1234.2
017900     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1234.2
018000     02  FILLER                      PIC X(58)  VALUE             NC1234.2
018100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1234.2
018200     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1234.2
018300     02 FILLER                       PIC X(15)  VALUE             NC1234.2
018400             " COPYRIGHT 1985".                                   NC1234.2
018500 01  CCVS-E-4.                                                    NC1234.2
018600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1234.2
018700     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1234.2
018800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1234.2
018900     02 FILLER                       PIC X(40)  VALUE             NC1234.2
019000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1234.2
019100 01  XXINFO.                                                      NC1234.2
019200     02 FILLER                       PIC X(19)  VALUE             NC1234.2
019300            "*** INFORMATION ***".                                NC1234.2
019400     02 INFO-TEXT.                                                NC1234.2
019500       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1234.2
019600       04 XXCOMPUTED                 PIC X(20).                   NC1234.2
019700       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1234.2
019800       04 XXCORRECT                  PIC X(20).                   NC1234.2
019900     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1234.2
020000 01  HYPHEN-LINE.                                                 NC1234.2
020100     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1234.2
020200     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1234.2
020300-    "*****************************************".                 NC1234.2
020400     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1234.2
020500-    "******************************".                            NC1234.2
020600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1234.2
020700     "NC123A".                                                    NC1234.2
020800 PROCEDURE DIVISION.                                              NC1234.2
020900 CCVS1 SECTION.                                                   NC1234.2
021000 OPEN-FILES.                                                      NC1234.2
021100     OPEN     OUTPUT PRINT-FILE.                                  NC1234.2
021200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1234.2
021300     MOVE    SPACE TO TEST-RESULTS.                               NC1234.2
021400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1234.2
021500     GO TO CCVS1-EXIT.                                            NC1234.2
021600 CLOSE-FILES.                                                     NC1234.2
021700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1234.2
021800 TERMINATE-CCVS.                                                  NC1234.2
021900*    EXIT PROGRAM.                                                NC1234.2
022000*TERMINATE-CALL.                                                  NC1234.2
022100     STOP     RUN.                                                NC1234.2
022200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1234.2
022300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1234.2
022400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1234.2
022500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1234.2
022600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1234.2
022700 PRINT-DETAIL.                                                    NC1234.2
022800     IF REC-CT NOT EQUAL TO ZERO                                  NC1234.2
022900             MOVE "." TO PARDOT-X                                 NC1234.2
023000             MOVE REC-CT TO DOTVALUE.                             NC1234.2
023100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1234.2
023200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1234.2
023300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1234.2
023400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1234.2
023500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1234.2
023600     MOVE SPACE TO CORRECT-X.                                     NC1234.2
023700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1234.2
023800     MOVE     SPACE TO RE-MARK.                                   NC1234.2
023900 HEAD-ROUTINE.                                                    NC1234.2
024000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1234.2
024100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1234.2
024200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1234.2
024300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1234.2
024400 COLUMN-NAMES-ROUTINE.                                            NC1234.2
024500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1234.2
024600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1234.2
024700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1234.2
024800 END-ROUTINE.                                                     NC1234.2
024900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1234.2
025000 END-RTN-EXIT.                                                    NC1234.2
025100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1234.2
025200 END-ROUTINE-1.                                                   NC1234.2
025300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1234.2
025400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1234.2
025500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1234.2
025600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1234.2
025700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1234.2
025800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1234.2
025900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1234.2
026000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1234.2
026100  END-ROUTINE-12.                                                 NC1234.2
026200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1234.2
026300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1234.2
026400         MOVE "NO " TO ERROR-TOTAL                                NC1234.2
026500         ELSE                                                     NC1234.2
026600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1234.2
026700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1234.2
026800     PERFORM WRITE-LINE.                                          NC1234.2
026900 END-ROUTINE-13.                                                  NC1234.2
027000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1234.2
027100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1234.2
027200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1234.2
027300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1234.2
027400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1234.2
027500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1234.2
027600          MOVE "NO " TO ERROR-TOTAL                               NC1234.2
027700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1234.2
027800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1234.2
027900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1234.2
028000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1234.2
028100 WRITE-LINE.                                                      NC1234.2
028200     ADD 1 TO RECORD-COUNT.                                       NC1234.2
028300*    IF RECORD-COUNT GREATER 42                                   NC1234.2
028400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1234.2
028500*        MOVE SPACE TO DUMMY-RECORD                               NC1234.2
028600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1234.2
028700*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1234.2
028800*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1234.2
028900*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1234.2
029000*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1234.2
029100*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1234.2
029200*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1234.2
029300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1234.2
029400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1234.2
029500*        MOVE ZERO TO RECORD-COUNT.                               NC1234.2
029600     PERFORM WRT-LN.                                              NC1234.2
029700 WRT-LN.                                                          NC1234.2
029800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1234.2
029900     MOVE SPACE TO DUMMY-RECORD.                                  NC1234.2
030000 BLANK-LINE-PRINT.                                                NC1234.2
030100     PERFORM WRT-LN.                                              NC1234.2
030200 FAIL-ROUTINE.                                                    NC1234.2
030300     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1234.2
030400            GO TO FAIL-ROUTINE-WRITE.                             NC1234.2
030500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1234.2
030600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1234.2
030700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1234.2
030800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1234.2
030900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1234.2
031000     GO TO  FAIL-ROUTINE-EX.                                      NC1234.2
031100 FAIL-ROUTINE-WRITE.                                              NC1234.2
031200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1234.2
031300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1234.2
031400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1234.2
031500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1234.2
031600 FAIL-ROUTINE-EX. EXIT.                                           NC1234.2
031700 BAIL-OUT.                                                        NC1234.2
031800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1234.2
031900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1234.2
032000 BAIL-OUT-WRITE.                                                  NC1234.2
032100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1234.2
032200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1234.2
032300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1234.2
032400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1234.2
032500 BAIL-OUT-EX. EXIT.                                               NC1234.2
032600 CCVS1-EXIT.                                                      NC1234.2
032700     EXIT.                                                        NC1234.2
032800 BUILD-TABLE1.                                                    NC1234.2
032900     MOVE 1.00 TO TABLE1-NUM (1).                                 NC1234.2
033000     MOVE 0.68 TO TABLE1-NUM (2).                                 NC1234.2
033100     MOVE 9.00 TO TABLE1-NUM (3).                                 NC1234.2
033200     MOVE 5.00 TO TABLE1-NUM (4).                                 NC1234.2
033300     MOVE 2.00 TO TABLE1-NUM (5).                                 NC1234.2
033400     MOVE 1.50 TO TABLE1-NUM (6).                                 NC1234.2
033500     MOVE 3.50 TO TABLE1-NUM (7).                                 NC1234.2
033600     MOVE 6.60 TO TABLE1-NUM (8).                                 NC1234.2
033700     MOVE 2.56 TO TABLE1-NUM (9).                                 NC1234.2
033800     MOVE -9.00 TO TABLE1-NUM (10).                               NC1234.2
033900 BUILD-TABLE2.                                                    NC1234.2
034000     MOVE 5.0 TO TABLE2-NUM (1).                                  NC1234.2
034100     MOVE 4.0 TO TABLE2-NUM (2).                                  NC1234.2
034200     MOVE 9.0 TO TABLE2-NUM (3).                                  NC1234.2
034300     MOVE 4.0 TO TABLE2-NUM (4).                                  NC1234.2
034400     MOVE 4.6 TO TABLE2-NUM (5).                                  NC1234.2
034500     MOVE 1.3 TO TABLE2-NUM (6).                                  NC1234.2
034600 BUILD-TABLE4.                                                    NC1234.2
034700     MOVE 20 TO TABLE4-NUM2 (1  1).                               NC1234.2
034800     MOVE 21 TO TABLE4-NUM2 (1  2).                               NC1234.2
034900     MOVE 22 TO TABLE4-NUM2 (1  3).                               NC1234.2
035000     MOVE 23 TO TABLE4-NUM2 (2  1).                               NC1234.2
035100     MOVE 24 TO TABLE4-NUM2 (2  2).                               NC1234.2
035200     MOVE 25 TO TABLE4-NUM2 (2  3).                               NC1234.2
035300     MOVE 26 TO TABLE4-NUM2 (3  1).                               NC1234.2
035400     MOVE 27 TO TABLE4-NUM2 (3  2).                               NC1234.2
035500     MOVE 28 TO TABLE4-NUM2 (3  3).                               NC1234.2
035600 BUILD-TABLE5.                                                    NC1234.2
035700     MOVE 003 TO TABLE5-NUM (1).                                  NC1234.2
035800     MOVE 002 TO TABLE5-NUM (2).                                  NC1234.2
035900 IND-INIT-GF-1.                                                   NC1234.2
036000     MOVE "ADD TO" TO FEATURE.                                    NC1234.2
036100     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
036200     MOVE 1.0 TO NUM-9V9.                                         NC1234.2
036300     SET INDEX1 TO 1.                                             NC1234.2
036400 IND-TEST-GF-1-0.                                                 NC1234.2
036500     ADD TABLE1-NUM (INDEX1) TO NUM-9V9.                          NC1234.2
036600 IND-TEST-GF-1-1.                                                 NC1234.2
036700     IF NUM-9V9 EQUAL TO 2.0                                      NC1234.2
036800        PERFORM PASS                                              NC1234.2
036900        ELSE GO TO IND-FAIL-GF-1.                                 NC1234.2
037000     GO TO IND-WRITE-GF-1.                                        NC1234.2
037100 IND-DELETE-GF-1.                                                 NC1234.2
037200     PERFORM DE-LETE.                                             NC1234.2
037300     GO TO IND-WRITE-GF-1.                                        NC1234.2
037400 IND-FAIL-GF-1.                                                   NC1234.2
037500     PERFORM FAIL.                                                NC1234.2
037600     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
037700     MOVE 2.0 TO CORRECT-14V4.                                    NC1234.2
037800 IND-WRITE-GF-1.                                                  NC1234.2
037900     MOVE "IND-TEST-GF-1" TO PAR-NAME.                            NC1234.2
038000     PERFORM PRINT-DETAIL.                                        NC1234.2
038100 IND-INIT-GF-2.                                                   NC1234.2
038200     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
038300     MOVE "ADD ROUNDED" TO FEATURE.                               NC1234.2
038400     MOVE 2.0 TO NUM-9V9.                                         NC1234.2
038500     SET INDEX1 TO 2.                                             NC1234.2
038600 IND-TEST-GF-2-0.                                                 NC1234.2
038700     ADD TABLE1-NUM (INDEX1) TO NUM-9V9 ROUNDED.                  NC1234.2
038800 IND-TEST-GF-2-1.                                                 NC1234.2
038900     IF NUM-9V9 EQUAL TO 2.7                                      NC1234.2
039000        PERFORM PASS                                              NC1234.2
039100        ELSE GO TO IND-FAIL-GF-2.                                 NC1234.2
039200     GO TO IND-WRITE-GF-2.                                        NC1234.2
039300 IND-DELETE-GF-2.                                                 NC1234.2
039400     PERFORM DE-LETE.                                             NC1234.2
039500     GO TO IND-WRITE-GF-2.                                        NC1234.2
039600 IND-FAIL-GF-2.                                                   NC1234.2
039700     PERFORM FAIL.                                                NC1234.2
039800     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
039900     MOVE 2.7 TO CORRECT-14V4.                                    NC1234.2
040000 IND-WRITE-GF-2.                                                  NC1234.2
040100     MOVE "IND-TEST-GF-2" TO PAR-NAME.                            NC1234.2
040200     PERFORM PRINT-DETAIL.                                        NC1234.2
040300 IND-INIT-GF-3.                                                   NC1234.2
040400     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
040500     MOVE "ADD ON SIZE ERROR" TO FEATURE.                         NC1234.2
040600     MOVE 5.0 TO NUM-9V9.                                         NC1234.2
040700     SET INDEX1 TO 3.                                             NC1234.2
040800 IND-TEST-GF-3-1.                                                 NC1234.2
040900     ADD TABLE1-NUM (INDEX1) TO NUM-9V9 ON SIZE ERROR             NC1234.2
041000        PERFORM PASS                                              NC1234.2
041100        GO TO IND-WRITE-GF-3-1.                                   NC1234.2
041200     GO TO IND-FAIL-GF-3-1.                                       NC1234.2
041300 IND-DELETE-GF-3-1.                                               NC1234.2
041400     PERFORM DE-LETE.                                             NC1234.2
041500     GO TO IND-WRITE-GF-3-1.                                      NC1234.2
041600 IND-FAIL-GF-3-1.                                                 NC1234.2
041700     PERFORM FAIL.                                                NC1234.2
041800     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
041900     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1234.2
042000 IND-WRITE-GF-3-1.                                                NC1234.2
042100     MOVE "IND-TEST-GF-3-1" TO PAR-NAME.                          NC1234.2
042200     PERFORM PRINT-DETAIL.                                        NC1234.2
042300 IND-TEST-GF-3-2.                                                 NC1234.2
042400     IF  NUM-9V9 = 5.0                                            NC1234.2
042500         PERFORM PASS                                             NC1234.2
042600         GO TO IND-WRITE-GF-3-2.                                  NC1234.2
042700     GO TO IND-FAIL-GF-3-2.                                       NC1234.2
042800 IND-DELETE-GF-3-2.                                               NC1234.2
042900     PERFORM DE-LETE.                                             NC1234.2
043000     GO TO IND-WRITE-GF-3-2.                                      NC1234.2
043100 IND-FAIL-GF-3-2.                                                 NC1234.2
043200     PERFORM FAIL.                                                NC1234.2
043300     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
043400     MOVE 5.0 TO CORRECT-14V4.                                    NC1234.2
043500     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1234.2
043600 IND-WRITE-GF-3-2.                                                NC1234.2
043700     MOVE "IND-TEST-GF-3-2" TO PAR-NAME.                          NC1234.2
043800     PERFORM PRINT-DETAIL.                                        NC1234.2
043900 IND-INIT-GF-4.                                                   NC1234.2
044000     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
044100     MOVE "ADD TO" TO FEATURE.                                    NC1234.2
044200     SET INDEX1 TO 1.                                             NC1234.2
044300     SET INDEX2 TO 1.                                             NC1234.2
044400 IND-TEST-GF-4-0.                                                 NC1234.2
044500     ADD TABLE1-NUM (INDEX1) TO TABLE2-NUM (INDEX2).              NC1234.2
044600 IND-TEST-GF-4-1.                                                 NC1234.2
044700     IF TABLE2-NUM (INDEX2) EQUAL TO 6.0                          NC1234.2
044800        PERFORM PASS                                              NC1234.2
044900        ELSE GO TO IND-FAIL-GF-4.                                 NC1234.2
045000     GO TO IND-WRITE-GF-4.                                        NC1234.2
045100 IND-DELETE-GF-4.                                                 NC1234.2
045200     PERFORM DE-LETE.                                             NC1234.2
045300     GO TO IND-WRITE-GF-4.                                        NC1234.2
045400 IND-FAIL-GF-4.                                                   NC1234.2
045500     PERFORM FAIL.                                                NC1234.2
045600     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1234.2
045700     MOVE 6.0 TO CORRECT-14V4.                                    NC1234.2
045800 IND-WRITE-GF-4.                                                  NC1234.2
045900     MOVE "IND-TEST-GF-4" TO PAR-NAME.                            NC1234.2
046000     PERFORM PRINT-DETAIL.                                        NC1234.2
046100 IND-INIT-GF-5.                                                   NC1234.2
046200     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
046300     MOVE "ADD ROUNDED" TO FEATURE.                               NC1234.2
046400     PERFORM BUILD-TABLE2.                                        NC1234.2
046500     SET INDEX1 TO 2.                                             NC1234.2
046600     SET INDEX2 TO 2.                                             NC1234.2
046700 IND-TEST-GF-5-0.                                                 NC1234.2
046800     ADD TABLE1-NUM (INDEX1) TO TABLE2-NUM (INDEX2) ROUNDED.      NC1234.2
046900 IND-TEST-GF-5-1.                                                 NC1234.2
047000     IF TABLE2-NUM (INDEX2) EQUAL TO 4.7                          NC1234.2
047100        PERFORM PASS                                              NC1234.2
047200        ELSE GO TO IND-FAIL-GF-5.                                 NC1234.2
047300     GO TO IND-WRITE-GF-5.                                        NC1234.2
047400 IND-DELETE-GF-5.                                                 NC1234.2
047500     PERFORM DE-LETE.                                             NC1234.2
047600     GO TO IND-WRITE-GF-5.                                        NC1234.2
047700 IND-FAIL-GF-5.                                                   NC1234.2
047800     PERFORM FAIL.                                                NC1234.2
047900     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1234.2
048000     MOVE 4.7 TO CORRECT-14V4.                                    NC1234.2
048100 IND-WRITE-GF-5.                                                  NC1234.2
048200     MOVE "IND-TEST-GF-5" TO PAR-NAME.                            NC1234.2
048300     PERFORM PRINT-DETAIL.                                        NC1234.2
048400 IND-INIT-GF-6.                                                   NC1234.2
048500     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
048600     MOVE "ADD ON SIZE ERROR" TO FEATURE.                         NC1234.2
048700     PERFORM BUILD-TABLE2.                                        NC1234.2
048800     SET INDEX1 TO 3.                                             NC1234.2
048900     SET INDEX2 TO 3.                                             NC1234.2
049000 IND-TEST-GF-6-1.                                                 NC1234.2
049100     ADD TABLE1-NUM (INDEX1) TO TABLE2-NUM (INDEX2) ON SIZE ERROR NC1234.2
049200        PERFORM PASS                                              NC1234.2
049300        GO TO IND-WRITE-GF-6-1.                                   NC1234.2
049400     GO TO IND-FAIL-GF-6-1.                                       NC1234.2
049500 IND-DELETE-GF-6-1.                                               NC1234.2
049600     PERFORM DE-LETE.                                             NC1234.2
049700     GO TO IND-WRITE-GF-6-1.                                      NC1234.2
049800 IND-FAIL-GF-6-1.                                                 NC1234.2
049900     PERFORM FAIL.                                                NC1234.2
050000     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1234.2
050100     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1234.2
050200 IND-WRITE-GF-6-1.                                                NC1234.2
050300     MOVE "IND-TEST-GF-6-1" TO PAR-NAME.                          NC1234.2
050400     PERFORM PRINT-DETAIL.                                        NC1234.2
050500 IND-TEST-GF-6-2.                                                 NC1234.2
050600     IF   TABLE2-NUM (INDEX2) = 9.0                               NC1234.2
050700        PERFORM PASS                                              NC1234.2
050800        GO TO IND-WRITE-GF-6-2.                                   NC1234.2
050900     GO TO IND-FAIL-GF-6-2.                                       NC1234.2
051000 IND-DELETE-GF-6-2.                                               NC1234.2
051100     PERFORM DE-LETE.                                             NC1234.2
051200     GO TO IND-WRITE-GF-6-2.                                      NC1234.2
051300 IND-FAIL-GF-6-2.                                                 NC1234.2
051400     PERFORM FAIL.                                                NC1234.2
051500     MOVE 9.0 TO CORRECT-14V4.                                    NC1234.2
051600     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1234.2
051700     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1234.2
051800 IND-WRITE-GF-6-2.                                                NC1234.2
051900     MOVE "IND-TEST-GF-6-2" TO PAR-NAME.                          NC1234.2
052000     PERFORM PRINT-DETAIL.                                        NC1234.2
052100 IND-INIT-GF-10.                                                  NC1234.2
052200     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
052300     MOVE "ADD GIVING" TO FEATURE.                                NC1234.2
052400     MOVE ZERO TO NUM-9V9.                                        NC1234.2
052500     SET INDEX1 TO 4.                                             NC1234.2
052600 IND-TEST-GF-10-0.                                                NC1234.2
052700     ADD 3 TABLE1-NUM (INDEX1) GIVING NUM-9V9.                    NC1234.2
052800 IND-TEST-GF-10-1.                                                NC1234.2
052900     IF NUM-9V9 EQUAL TO 8.0                                      NC1234.2
053000        PERFORM PASS                                              NC1234.2
053100        ELSE GO TO IND-FAIL-GF-10.                                NC1234.2
053200     GO TO IND-WRITE-GF-10.                                       NC1234.2
053300 IND-DELETE-GF-10.                                                NC1234.2
053400     PERFORM DE-LETE.                                             NC1234.2
053500     GO TO IND-WRITE-GF-10.                                       NC1234.2
053600 IND-FAIL-GF-10.                                                  NC1234.2
053700     PERFORM FAIL.                                                NC1234.2
053800     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
053900     MOVE 8.0 TO CORRECT-14V4.                                    NC1234.2
054000 IND-WRITE-GF-10.                                                 NC1234.2
054100     MOVE "IND-TEST-GF-10" TO PAR-NAME.                           NC1234.2
054200     PERFORM PRINT-DETAIL.                                        NC1234.2
054300 IND-INIT-GF-11.                                                  NC1234.2
054400     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
054500     MOVE "ADD GIVING" TO FEATURE.                                NC1234.2
054600     MOVE ZERO TO NUM-9V9.                                        NC1234.2
054700     PERFORM BUILD-TABLE2.                                        NC1234.2
054800     SET INDEX1 TO 1.                                             NC1234.2
054900     SET INDEX2 TO 4.                                             NC1234.2
055000 IND-TEST-GF-11-0.                                                NC1234.2
055100     ADD TABLE1-NUM (INDEX1) TABLE2-NUM (INDEX2) GIVING NUM-9V9.  NC1234.2
055200 IND-TEST-GF-11-1.                                                NC1234.2
055300     IF NUM-9V9 EQUAL TO 5.0                                      NC1234.2
055400        PERFORM PASS                                              NC1234.2
055500        ELSE GO TO IND-FAIL-GF-11.                                NC1234.2
055600     GO TO IND-WRITE-GF-11.                                       NC1234.2
055700 IND-DELETE-GF-11.                                                NC1234.2
055800     PERFORM DE-LETE.                                             NC1234.2
055900     GO TO IND-WRITE-GF-11.                                       NC1234.2
056000 IND-FAIL-GF-11.                                                  NC1234.2
056100     PERFORM FAIL.                                                NC1234.2
056200     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
056300     MOVE 5.0 TO CORRECT-14V4.                                    NC1234.2
056400 IND-WRITE-GF-11.                                                 NC1234.2
056500     MOVE "IND-TEST-GF-11" TO PAR-NAME.                           NC1234.2
056600     PERFORM PRINT-DETAIL.                                        NC1234.2
056700 IND-INIT-GF-12.                                                  NC1234.2
056800     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
056900     MOVE "ADD GIVING" TO FEATURE.                                NC1234.2
057000     PERFORM BUILD-TABLE2.                                        NC1234.2
057100     MOVE ZERO TO TABLE3.                                         NC1234.2
057200     SET INDEX1 TO 5.                                             NC1234.2
057300     SET INDEX2 TO 1.                                             NC1234.2
057400     SET INDEX3 TO 3.                                             NC1234.2
057500 IND-TEST-GF-12-0.                                                NC1234.2
057600     ADD TABLE1-NUM (INDEX1) TABLE2-NUM (INDEX2)                  NC1234.2
057700        GIVING TABLE3-NUM (INDEX3).                               NC1234.2
057800 IND-TEST-GF-12-1.                                                NC1234.2
057900     IF TABLE3-NUM (INDEX3) EQUAL TO 7.0                          NC1234.2
058000        PERFORM PASS                                              NC1234.2
058100        ELSE GO TO IND-FAIL-GF-12.                                NC1234.2
058200     GO TO IND-WRITE-GF-12.                                       NC1234.2
058300 IND-DELETE-GF-12.                                                NC1234.2
058400     PERFORM DE-LETE.                                             NC1234.2
058500     GO TO IND-WRITE-GF-12.                                       NC1234.2
058600 IND-FAIL-GF-12.                                                  NC1234.2
058700     PERFORM FAIL.                                                NC1234.2
058800     MOVE TABLE3-NUM (INDEX3) TO COMPUTED-14V4.                   NC1234.2
058900     MOVE 7.0 TO CORRECT-14V4.                                    NC1234.2
059000 IND-WRITE-GF-12.                                                 NC1234.2
059100     MOVE "IND-TEST-GF-12" TO PAR-NAME.                           NC1234.2
059200     PERFORM PRINT-DETAIL.                                        NC1234.2
059300 IND-INIT-GF-7.                                                   NC1234.2
059400     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
059500     MOVE "ADD TO" TO FEATURE.                                    NC1234.2
059600     SET INDEX4-1 TO 3.                                           NC1234.2
059700     SET INDEX4-2 TO 2.                                           NC1234.2
059800 IND-TEST-GF-7-0.                                                 NC1234.2
059900     ADD TABLE4-NUM2 (3  1) TO TABLE4-NUM2 (INDEX4-1  INDEX4-2).  NC1234.2
060000 IND-TEST-GF-7-1.                                                 NC1234.2
060100     IF TABLE4-NUM2 (INDEX4-1  INDEX4-2) EQUAL TO 53              NC1234.2
060200        PERFORM PASS                                              NC1234.2
060300        ELSE GO TO IND-FAIL-GF-7.                                 NC1234.2
060400     GO TO IND-WRITE-GF-7.                                        NC1234.2
060500 IND-DELETE-GF-7.                                                 NC1234.2
060600     PERFORM DE-LETE.                                             NC1234.2
060700     GO TO IND-WRITE-GF-7.                                        NC1234.2
060800 IND-FAIL-GF-7.                                                   NC1234.2
060900     PERFORM FAIL.                                                NC1234.2
061000     MOVE TABLE4-NUM2 (INDEX4-1  INDEX4-2) TO COMPUTED-14V4.      NC1234.2
061100     MOVE 53 TO CORRECT-14V4.                                     NC1234.2
061200 IND-WRITE-GF-7.                                                  NC1234.2
061300     MOVE "IND-TEST-GF-7" TO PAR-NAME.                            NC1234.2
061400     PERFORM PRINT-DETAIL.                                        NC1234.2
061500 IND-INIT-GF-8.                                                   NC1234.2
061600     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
061700     MOVE "ADD TO" TO FEATURE.                                    NC1234.2
061800     MOVE 1.6 TO NUM-9V9.                                         NC1234.2
061900     SET INDEX1 TO 5.                                             NC1234.2
062000 IND-TEST-GF-8-0.                                                 NC1234.2
062100     ADD TABLE1-NUM (INDEX1 + 1) TO NUM-9V9.                      NC1234.2
062200 IND-TEST-GF-8-1.                                                 NC1234.2
062300     IF NUM-9V9 EQUAL TO 3.1                                      NC1234.2
062400        PERFORM PASS                                              NC1234.2
062500        ELSE GO TO IND-FAIL-GF-8.                                 NC1234.2
062600     GO TO IND-WRITE-GF-8.                                        NC1234.2
062700 IND-DELETE-GF-8.                                                 NC1234.2
062800     PERFORM DE-LETE.                                             NC1234.2
062900     GO TO IND-WRITE-GF-8.                                        NC1234.2
063000 IND-FAIL-GF-8.                                                   NC1234.2
063100     PERFORM FAIL.                                                NC1234.2
063200     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
063300     MOVE 3.1 TO CORRECT-14V4.                                    NC1234.2
063400 IND-WRITE-GF-8.                                                  NC1234.2
063500     MOVE "IND-TEST-GF-8" TO PAR-NAME.                            NC1234.2
063600     PERFORM PRINT-DETAIL.                                        NC1234.2
063700 IND-INIT-GF-9.                                                   NC1234.2
063800     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
063900     MOVE "ADD TO" TO FEATURE.                                    NC1234.2
064000     PERFORM BUILD-TABLE2.                                        NC1234.2
064100     SET INDEX1 TO 6.                                             NC1234.2
064200     SET INDEX2 TO 6.                                             NC1234.2
064300 IND-TEST-GF-9-0.                                                 NC1234.2
064400     ADD TABLE1-NUM (INDEX1 + 1) TO TABLE2-NUM (INDEX2 - 1).      NC1234.2
064500 IND-TEST-GF-9-1.                                                 NC1234.2
064600     IF TABLE2-NUM (INDEX2 - 1) EQUAL TO 8.1                      NC1234.2
064700        PERFORM PASS                                              NC1234.2
064800        ELSE GO TO IND-FAIL-GF-9.                                 NC1234.2
064900     GO TO IND-WRITE-GF-9.                                        NC1234.2
065000 IND-DELETE-GF-9.                                                 NC1234.2
065100     PERFORM DE-LETE.                                             NC1234.2
065200     GO TO IND-WRITE-GF-9.                                        NC1234.2
065300 IND-FAIL-GF-9.                                                   NC1234.2
065400     PERFORM FAIL.                                                NC1234.2
065500     MOVE TABLE2-NUM (INDEX2 - 1) TO COMPUTED-14V4.               NC1234.2
065600     MOVE 8.1 TO CORRECT-14V4.                                    NC1234.2
065700 IND-WRITE-GF-9.                                                  NC1234.2
065800     MOVE "IND-TEST-GF-9" TO PAR-NAME.                            NC1234.2
065900     PERFORM PRINT-DETAIL.                                        NC1234.2
066000 IND-INIT-GF-13.                                                  NC1234.2
066100     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
066200     MOVE "ADD GIVING" TO FEATURE.                                NC1234.2
066300     PERFORM BUILD-TABLE2.                                        NC1234.2
066400     MOVE ZERO TO NUM-9V9.                                        NC1234.2
066500     SET INDEX1 TO 7.                                             NC1234.2
066600     SET INDEX2 TO 4.                                             NC1234.2
066700 IND-TEST-GF-13-0.                                                NC1234.2
066800     ADD TABLE1-NUM (INDEX1 + 1) TABLE2-NUM (INDEX2 + 2)          NC1234.2
066900        GIVING NUM-9V9.                                           NC1234.2
067000 IND-TEST-GF-13-1.                                                NC1234.2
067100     IF NUM-9V9 EQUAL TO 7.9                                      NC1234.2
067200        PERFORM PASS                                              NC1234.2
067300        ELSE GO TO IND-FAIL-GF-13.                                NC1234.2
067400     GO TO IND-WRITE-GF-13.                                       NC1234.2
067500 IND-DELETE-GF-13.                                                NC1234.2
067600     PERFORM DE-LETE.                                             NC1234.2
067700     GO TO IND-WRITE-GF-13.                                       NC1234.2
067800 IND-FAIL-GF-13.                                                  NC1234.2
067900     PERFORM FAIL.                                                NC1234.2
068000     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
068100     MOVE 7.9 TO CORRECT-14V4.                                    NC1234.2
068200 IND-WRITE-GF-13.                                                 NC1234.2
068300     MOVE "IND-TEST-GF-13" TO PAR-NAME.                           NC1234.2
068400     PERFORM PRINT-DETAIL.                                        NC1234.2
068500 IND-INIT-GF-14.                                                  NC1234.2
068600     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
068700     MOVE "ADD GIVING" TO FEATURE.                                NC1234.2
068800     PERFORM BUILD-TABLE2.                                        NC1234.2
068900     MOVE ZERO TO TABLE3.                                         NC1234.2
069000     SET INDEX1 TO 3.                                             NC1234.2
069100     SET INDEX2 TO 2.                                             NC1234.2
069200     SET INDEX3 TO 6.                                             NC1234.2
069300 IND-TEST-GF-14-0.                                                NC1234.2
069400     ADD TABLE1-NUM (INDEX1 + 1) TABLE2-NUM (INDEX2 + 1)          NC1234.2
069500        GIVING TABLE3-NUM (INDEX3 - 2).                           NC1234.2
069600 IND-TEST-GF-14-1.                                                NC1234.2
069700     IF TABLE3-NUM (INDEX3 - 2) EQUAL TO 14.0                     NC1234.2
069800        PERFORM PASS                                              NC1234.2
069900        ELSE GO TO IND-FAIL-GF-14.                                NC1234.2
070000     GO TO IND-WRITE-GF-14.                                       NC1234.2
070100 IND-DELETE-GF-14.                                                NC1234.2
070200     PERFORM DE-LETE.                                             NC1234.2
070300     GO TO IND-WRITE-GF-14.                                       NC1234.2
070400 IND-FAIL-GF-14.                                                  NC1234.2
070500     PERFORM FAIL.                                                NC1234.2
070600     MOVE TABLE3-NUM (INDEX3 - 2) TO COMPUTED-14V4.               NC1234.2
070700     MOVE 14.0 TO CORRECT-14V4.                                   NC1234.2
070800 IND-WRITE-GF-14.                                                 NC1234.2
070900     MOVE "IND-TEST-GF-14" TO PAR-NAME.                           NC1234.2
071000     PERFORM PRINT-DETAIL.                                        NC1234.2
071100 IND-INIT-GF-15.                                                  NC1234.2
071200     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
071300     MOVE "SUBTRACT FROM" TO FEATURE.                             NC1234.2
071400     MOVE 3.0 TO NUM-9V9.                                         NC1234.2
071500     SET INDEX1 TO 1.                                             NC1234.2
071600 IND-TEST-GF-15-0.                                                NC1234.2
071700     SUBTRACT TABLE1-NUM (INDEX1) FROM NUM-9V9.                   NC1234.2
071800 IND-TEST-GF-15-1.                                                NC1234.2
071900     IF NUM-9V9 EQUAL TO 2.0                                      NC1234.2
072000        PERFORM PASS                                              NC1234.2
072100        ELSE GO TO IND-FAIL-GF-15.                                NC1234.2
072200     GO TO IND-WRITE-GF-15.                                       NC1234.2
072300 IND-DELETE-GF-15.                                                NC1234.2
072400     PERFORM DE-LETE.                                             NC1234.2
072500     GO TO IND-WRITE-GF-15.                                       NC1234.2
072600 IND-FAIL-GF-15.                                                  NC1234.2
072700     PERFORM FAIL.                                                NC1234.2
072800     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
072900     MOVE 2.0 TO CORRECT-14V4.                                    NC1234.2
073000 IND-WRITE-GF-15.                                                 NC1234.2
073100     MOVE "IND-TEST-GF-15" TO PAR-NAME.                           NC1234.2
073200     PERFORM PRINT-DETAIL.                                        NC1234.2
073300 IND-INIT-GF-16.                                                  NC1234.2
073400     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
073500     MOVE "SUBTRACT ROUNDED" TO FEATURE.                          NC1234.2
073600     MOVE 5.0 TO NUM-9V9.                                         NC1234.2
073700     SET INDEX1 TO 9.                                             NC1234.2
073800 IND-TEST-GF-16-0.                                                NC1234.2
073900     SUBTRACT TABLE1-NUM (INDEX1) FROM NUM-9V9 ROUNDED.           NC1234.2
074000 IND-TEST-GF-16-1.                                                NC1234.2
074100     IF NUM-9V9 EQUAL TO 2.4                                      NC1234.2
074200        PERFORM PASS                                              NC1234.2
074300        ELSE GO TO IND-FAIL-GF-16.                                NC1234.2
074400     GO TO IND-WRITE-GF-16.                                       NC1234.2
074500 IND-DELETE-GF-16.                                                NC1234.2
074600     PERFORM DE-LETE.                                             NC1234.2
074700     GO TO IND-WRITE-GF-16.                                       NC1234.2
074800 IND-FAIL-GF-16.                                                  NC1234.2
074900     PERFORM FAIL.                                                NC1234.2
075000     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
075100     MOVE 2.4 TO CORRECT-14V4.                                    NC1234.2
075200 IND-WRITE-GF-16.                                                 NC1234.2
075300     MOVE "IND-TEST-GF-16" TO PAR-NAME.                           NC1234.2
075400     PERFORM PRINT-DETAIL.                                        NC1234.2
075500 IND-INIT-GF-17.                                                  NC1234.2
075600     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
075700     MOVE "SUBTRACT ON SIZE ERROR" TO FEATURE.                    NC1234.2
075800     MOVE 9.0 TO NUM-9V9.                                         NC1234.2
075900     SET INDEX1 TO 10.                                            NC1234.2
076000 IND-TEST-GF-17-1.                                                NC1234.2
076100     SUBTRACT TABLE1-NUM (INDEX1) FROM NUM-9V9 ON SIZE ERROR      NC1234.2
076200        PERFORM PASS                                              NC1234.2
076300        GO TO IND-WRITE-GF-17-1.                                  NC1234.2
076400     GO TO IND-FAIL-GF-17-1.                                      NC1234.2
076500 IND-DELETE-GF-17-1.                                              NC1234.2
076600     PERFORM DE-LETE.                                             NC1234.2
076700     GO TO IND-WRITE-GF-17-1.                                     NC1234.2
076800 IND-FAIL-GF-17-1.                                                NC1234.2
076900     PERFORM FAIL.                                                NC1234.2
077000     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
077100     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1234.2
077200 IND-WRITE-GF-17-1.                                               NC1234.2
077300     MOVE "IND-TEST-GF-17-1" TO PAR-NAME.                         NC1234.2
077400     PERFORM PRINT-DETAIL.                                        NC1234.2
077500 IND-TEST-GF-17-2.                                                NC1234.2
077600     IF      NUM-9V9 = 9.0                                        NC1234.2
077700             PERFORM PASS                                         NC1234.2
077800             GO TO   IND-WRITE-GF-17-2.                           NC1234.2
077900     GO TO   IND-FAIL-GF-17-2.                                    NC1234.2
078000 IND-DELETE-GF-17-2.                                              NC1234.2
078100     PERFORM DE-LETE.                                             NC1234.2
078200     GO TO IND-WRITE-GF-17-2.                                     NC1234.2
078300 IND-FAIL-GF-17-2.                                                NC1234.2
078400     PERFORM FAIL.                                                NC1234.2
078500     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
078600     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1234.2
078700 IND-WRITE-GF-17-2.                                               NC1234.2
078800     MOVE "IND-TEST-GF-17-2" TO PAR-NAME.                         NC1234.2
078900     PERFORM PRINT-DETAIL.                                        NC1234.2
079000 IND-INIT-GF-18.                                                  NC1234.2
079100     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
079200     MOVE "SUBTRACT FROM" TO FEATURE.                             NC1234.2
079300     PERFORM BUILD-TABLE2.                                        NC1234.2
079400     SET INDEX1 TO 1.                                             NC1234.2
079500     SET INDEX2 TO 1.                                             NC1234.2
079600 IND-TEST-GF-18-0.                                                NC1234.2
079700     SUBTRACT TABLE1-NUM (INDEX1) FROM TABLE2-NUM (INDEX2).       NC1234.2
079800 IND-TEST-GF-18-1.                                                NC1234.2
079900     IF TABLE2-NUM (INDEX2) EQUAL TO 4.0                          NC1234.2
080000        PERFORM PASS                                              NC1234.2
080100        ELSE GO TO IND-FAIL-GF-18.                                NC1234.2
080200     GO TO IND-WRITE-GF-18.                                       NC1234.2
080300 IND-DELETE-GF-18.                                                NC1234.2
080400     PERFORM DE-LETE.                                             NC1234.2
080500     GO TO IND-WRITE-GF-18.                                       NC1234.2
080600 IND-FAIL-GF-18.                                                  NC1234.2
080700     PERFORM FAIL.                                                NC1234.2
080800     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1234.2
080900     MOVE 4.0 TO CORRECT-14V4.                                    NC1234.2
081000 IND-WRITE-GF-18.                                                 NC1234.2
081100     MOVE "IND-TEST-GF-18" TO PAR-NAME.                           NC1234.2
081200     PERFORM PRINT-DETAIL.                                        NC1234.2
081300 IND-INIT-GF-19.                                                  NC1234.2
081400     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
081500     MOVE "SUBTRACT ROUNDED" TO FEATURE.                          NC1234.2
081600     PERFORM BUILD-TABLE2.                                        NC1234.2
081700     SET INDEX1 TO 9.                                             NC1234.2
081800     SET INDEX2 TO 3.                                             NC1234.2
081900 IND-TEST-GF-19-0.                                                NC1234.2
082000     SUBTRACT TABLE1-NUM (INDEX1) FROM TABLE2-NUM (INDEX2)        NC1234.2
082100        ROUNDED.                                                  NC1234.2
082200 IND-TEST-GF-19-1.                                                NC1234.2
082300     IF TABLE2-NUM (INDEX2) EQUAL TO 6.4                          NC1234.2
082400        PERFORM PASS                                              NC1234.2
082500        ELSE GO TO IND-FAIL-GF-19.                                NC1234.2
082600     GO TO IND-WRITE-GF-19.                                       NC1234.2
082700 IND-DELETE-GF-19.                                                NC1234.2
082800     PERFORM DE-LETE.                                             NC1234.2
082900     GO TO IND-WRITE-GF-19.                                       NC1234.2
083000 IND-FAIL-GF-19.                                                  NC1234.2
083100     PERFORM FAIL.                                                NC1234.2
083200     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1234.2
083300     MOVE 6.4 TO CORRECT-14V4.                                    NC1234.2
083400 IND-WRITE-GF-19.                                                 NC1234.2
083500     MOVE "IND-TEST-GF-19" TO PAR-NAME.                           NC1234.2
083600     PERFORM PRINT-DETAIL.                                        NC1234.2
083700 IND-INIT-GF-20.                                                  NC1234.2
083800     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
083900     MOVE "SUBTRACT ON SIZE ERROR" TO FEATURE.                    NC1234.2
084000     PERFORM BUILD-TABLE2.                                        NC1234.2
084100     SET INDEX1 TO 10.                                            NC1234.2
084200     SET INDEX2 TO 4.                                             NC1234.2
084300 IND-TEST-GF-20-1.                                                NC1234.2
084400     SUBTRACT TABLE1-NUM (INDEX1) FROM TABLE2-NUM (INDEX2)        NC1234.2
084500        ON SIZE ERROR                                             NC1234.2
084600        PERFORM PASS                                              NC1234.2
084700        GO TO IND-WRITE-GF-20-1.                                  NC1234.2
084800     GO TO IND-FAIL-GF-20-1.                                      NC1234.2
084900 IND-DELETE-GF-20-1.                                              NC1234.2
085000     PERFORM DE-LETE.                                             NC1234.2
085100     GO TO IND-WRITE-GF-20-1.                                     NC1234.2
085200 IND-FAIL-GF-20-1.                                                NC1234.2
085300     PERFORM FAIL.                                                NC1234.2
085400     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1234.2
085500     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1234.2
085600 IND-WRITE-GF-20-1.                                               NC1234.2
085700     MOVE "IND-TEST-GF-20-1" TO PAR-NAME.                         NC1234.2
085800     PERFORM PRINT-DETAIL.                                        NC1234.2
085900 IND-TEST-GF-20-2.                                                NC1234.2
086000     IF      TABLE2-NUM (INDEX2) = 4.0                            NC1234.2
086100             PERFORM PASS                                         NC1234.2
086200             GO TO   IND-WRITE-GF-20-2.                           NC1234.2
086300     GO TO   IND-FAIL-GF-20-2.                                    NC1234.2
086400 IND-DELETE-GF-20-2.                                              NC1234.2
086500     PERFORM DE-LETE.                                             NC1234.2
086600     GO TO IND-WRITE-GF-20-2.                                     NC1234.2
086700 IND-FAIL-GF-20-2.                                                NC1234.2
086800     PERFORM FAIL.                                                NC1234.2
086900     MOVE TABLE2-NUM (INDEX2) TO COMPUTED-14V4.                   NC1234.2
087000     MOVE 4 TO CORRECT-14V4.                                      NC1234.2
087100     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1234.2
087200 IND-WRITE-GF-20-2.                                               NC1234.2
087300     MOVE "IND-TEST-GF-20-2" TO PAR-NAME.                         NC1234.2
087400     PERFORM PRINT-DETAIL.                                        NC1234.2
087500 IND-INIT-GF-24.                                                  NC1234.2
087600     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
087700     MOVE "SUBTRACT GIVING" TO FEATURE.                           NC1234.2
087800     MOVE ZERO TO NUM-9V9.                                        NC1234.2
087900     SET INDEX1 TO 1.                                             NC1234.2
088000 IND-TEST-GF-24-0.                                                NC1234.2
088100     SUBTRACT TABLE1-NUM (INDEX1) FROM 8 GIVING NUM-9V9.          NC1234.2
088200 IND-TEST-GF-24-1.                                                NC1234.2
088300     IF NUM-9V9 EQUAL TO 7.0                                      NC1234.2
088400        PERFORM PASS                                              NC1234.2
088500        ELSE GO TO IND-FAIL-GF-24.                                NC1234.2
088600     GO TO IND-WRITE-GF-24.                                       NC1234.2
088700 IND-DELETE-GF-24.                                                NC1234.2
088800     PERFORM DE-LETE.                                             NC1234.2
088900     GO TO IND-WRITE-GF-24.                                       NC1234.2
089000 IND-FAIL-GF-24.                                                  NC1234.2
089100     PERFORM FAIL.                                                NC1234.2
089200     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
089300     MOVE 7.0 TO CORRECT-14V4.                                    NC1234.2
089400 IND-WRITE-GF-24.                                                 NC1234.2
089500     MOVE "IND-TEST-GF-24" TO PAR-NAME.                           NC1234.2
089600     PERFORM PRINT-DETAIL.                                        NC1234.2
089700 IND-INIT-GF-25.                                                  NC1234.2
089800     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
089900     MOVE "SUBTRACT GIVING" TO FEATURE.                           NC1234.2
090000     MOVE ZERO TO NUM-9V9.                                        NC1234.2
090100     PERFORM BUILD-TABLE2.                                        NC1234.2
090200     SET INDEX1 TO 1.                                             NC1234.2
090300     SET INDEX2 TO 3.                                             NC1234.2
090400 IND-TEST-GF-25-0.                                                NC1234.2
090500     SUBTRACT TABLE1-NUM (INDEX1) FROM TABLE2-NUM (INDEX2)        NC1234.2
090600        GIVING NUM-9V9.                                           NC1234.2
090700 IND-TEST-GF-25-1.                                                NC1234.2
090800     IF NUM-9V9 EQUAL TO 8.0                                      NC1234.2
090900        PERFORM PASS                                              NC1234.2
091000        ELSE GO TO IND-FAIL-GF-25.                                NC1234.2
091100     GO TO IND-WRITE-GF-25.                                       NC1234.2
091200 IND-DELETE-GF-25.                                                NC1234.2
091300     PERFORM DE-LETE.                                             NC1234.2
091400     GO TO IND-WRITE-GF-25.                                       NC1234.2
091500 IND-FAIL-GF-25.                                                  NC1234.2
091600     PERFORM FAIL.                                                NC1234.2
091700     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
091800     MOVE 8.0 TO CORRECT-14V4.                                    NC1234.2
091900 IND-WRITE-GF-25.                                                 NC1234.2
092000     MOVE "IND-TEST-GF-25" TO PAR-NAME.                           NC1234.2
092100     PERFORM PRINT-DETAIL.                                        NC1234.2
092200 IND-INIT-GF-26.                                                  NC1234.2
092300     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
092400     MOVE "SUBTRACT GIVING" TO FEATURE.                           NC1234.2
092500     MOVE ZERO TO TABLE3.                                         NC1234.2
092600     PERFORM BUILD-TABLE2.                                        NC1234.2
092700     SET INDEX1 TO 10.                                            NC1234.2
092800     SET INDEX2 TO 1.                                             NC1234.2
092900     SET INDEX3 TO 3.                                             NC1234.2
093000 IND-TEST-GF-26-0.                                                NC1234.2
093100     SUBTRACT TABLE1-NUM (INDEX1) FROM TABLE2-NUM (INDEX2)        NC1234.2
093200        GIVING TABLE3-NUM (INDEX3).                               NC1234.2
093300 IND-TEST-GF-26-1.                                                NC1234.2
093400     IF TABLE3-NUM (INDEX3) EQUAL TO 14.0                         NC1234.2
093500        PERFORM PASS                                              NC1234.2
093600        ELSE GO TO IND-FAIL-GF-26.                                NC1234.2
093700     GO TO IND-WRITE-GF-26.                                       NC1234.2
093800 IND-DELETE-GF-26.                                                NC1234.2
093900     PERFORM DE-LETE.                                             NC1234.2
094000     GO TO IND-WRITE-GF-26.                                       NC1234.2
094100 IND-FAIL-GF-26.                                                  NC1234.2
094200     PERFORM FAIL.                                                NC1234.2
094300     MOVE TABLE3-NUM (INDEX3) TO COMPUTED-14V4.                   NC1234.2
094400     MOVE 14.0 TO CORRECT-14V4.                                   NC1234.2
094500 IND-WRITE-GF-26.                                                 NC1234.2
094600     MOVE "IND-TEST-GF-26" TO PAR-NAME.                           NC1234.2
094700     PERFORM PRINT-DETAIL.                                        NC1234.2
094800 IND-INIT-GF-21.                                                  NC1234.2
094900     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
095000     MOVE "SUBTRACT FROM" TO FEATURE.                             NC1234.2
095100     PERFORM BUILD-TABLE4.                                        NC1234.2
095200     SET INDEX4-1 TO 3.                                           NC1234.2
095300     SET INDEX4-2 TO 3.                                           NC1234.2
095400 IND-TEST-GF-21-0.                                                NC1234.2
095500     SUBTRACT TABLE4-NUM2 (1  1) FROM                             NC1234.2
095600        TABLE4-NUM2 (INDEX4-1  INDEX4-2).                         NC1234.2
095700 IND-TEST-GF-21-1.                                                NC1234.2
095800     IF TABLE4-NUM2 (INDEX4-1  INDEX4-2) EQUAL TO 8               NC1234.2
095900        PERFORM PASS                                              NC1234.2
096000        ELSE GO TO IND-FAIL-GF-21.                                NC1234.2
096100     GO TO IND-WRITE-GF-21.                                       NC1234.2
096200 IND-DELETE-GF-21.                                                NC1234.2
096300     PERFORM DE-LETE.                                             NC1234.2
096400     GO TO IND-WRITE-GF-21.                                       NC1234.2
096500 IND-FAIL-GF-21.                                                  NC1234.2
096600     PERFORM FAIL.                                                NC1234.2
096700     MOVE TABLE4-NUM2 (INDEX4-1  INDEX4-2) TO COMPUTED-14V4.      NC1234.2
096800     MOVE 8.0 TO CORRECT-14V4.                                    NC1234.2
096900 IND-WRITE-GF-21.                                                 NC1234.2
097000     MOVE "SUBTRCT-TEST-GF-21" TO PAR-NAME.                       NC1234.2
097100     PERFORM PRINT-DETAIL.                                        NC1234.2
097200 IND-INIT-GF-22.                                                  NC1234.2
097300     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
097400     MOVE "SUBTRACT FROM" TO FEATURE.                             NC1234.2
097500     MOVE 0.0 TO NUM-9V9.                                         NC1234.2
097600     SET INDEX1 TO 9.                                             NC1234.2
097700 IND-TEST-GF-22-0.                                                NC1234.2
097800     SUBTRACT TABLE1-NUM (INDEX1 + 1) FROM NUM-9V9.               NC1234.2
097900 IND-TEST-GF-22-1.                                                NC1234.2
098000     IF NUM-9V9 EQUAL TO 9.0                                      NC1234.2
098100        PERFORM PASS                                              NC1234.2
098200        ELSE GO TO IND-FAIL-GF-22.                                NC1234.2
098300     GO TO IND-WRITE-GF-22.                                       NC1234.2
098400 IND-DELETE-GF-22.                                                NC1234.2
098500     PERFORM DE-LETE.                                             NC1234.2
098600     GO TO IND-WRITE-GF-22.                                       NC1234.2
098700 IND-FAIL-GF-22.                                                  NC1234.2
098800     PERFORM FAIL.                                                NC1234.2
098900     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
099000     MOVE 9.0 TO CORRECT-14V4.                                    NC1234.2
099100 IND-WRITE-GF-22.                                                 NC1234.2
099200     MOVE "SUBTRCT-TEST-GF-22" TO PAR-NAME.                       NC1234.2
099300     PERFORM PRINT-DETAIL.                                        NC1234.2
099400 IND-INIT-GF-23.                                                  NC1234.2
099500     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
099600     MOVE "SUBTRACT FROM" TO FEATURE.                             NC1234.2
099700     PERFORM BUILD-TABLE2.                                        NC1234.2
099800     SET INDEX1 TO 9.                                             NC1234.2
099900     SET INDEX2 TO 6.                                             NC1234.2
100000 IND-TEST-GF-23-0.                                                NC1234.2
100100     SUBTRACT TABLE1-NUM (INDEX1 - 2) FROM                        NC1234.2
100200        TABLE2-NUM (INDEX2 - 1).                                  NC1234.2
100300 IND-TEST-GF-23-1.                                                NC1234.2
100400     IF TABLE2-NUM (INDEX2 - 1) EQUAL TO 1.1                      NC1234.2
100500        PERFORM PASS                                              NC1234.2
100600        ELSE GO TO IND-FAIL-GF-23.                                NC1234.2
100700     GO TO IND-WRITE-GF-23.                                       NC1234.2
100800 IND-DELETE-GF-23.                                                NC1234.2
100900     PERFORM DE-LETE.                                             NC1234.2
101000     GO TO IND-WRITE-GF-23.                                       NC1234.2
101100 IND-FAIL-GF-23.                                                  NC1234.2
101200     PERFORM FAIL.                                                NC1234.2
101300     MOVE TABLE2-NUM (INDEX2 - 1) TO COMPUTED-14V4.               NC1234.2
101400     MOVE 1.1 TO CORRECT-14V4.                                    NC1234.2
101500 IND-WRITE-GF-23.                                                 NC1234.2
101600     MOVE "SUBTRCT-TEST-GF-23" TO PAR-NAME.                       NC1234.2
101700     PERFORM PRINT-DETAIL.                                        NC1234.2
101800 IND-INIT-GF-27.                                                  NC1234.2
101900     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
102000     MOVE "SUBTRACT GIVING" TO FEATURE.                           NC1234.2
102100     MOVE ZERO TO NUM-9V9.                                        NC1234.2
102200     PERFORM BUILD-TABLE2.                                        NC1234.2
102300     SET INDEX1 TO 7.                                             NC1234.2
102400     SET INDEX2 TO 4.                                             NC1234.2
102500 IND-TEST-GF-27-0.                                                NC1234.2
102600     SUBTRACT TABLE1-NUM (INDEX1 - 2) FROM                        NC1234.2
102700        TABLE2-NUM (INDEX2 - 1) GIVING NUM-9V9.                   NC1234.2
102800 IND-TEST-GF-27-1.                                                NC1234.2
102900     IF NUM-9V9 EQUAL TO 7.0                                      NC1234.2
103000        PERFORM PASS                                              NC1234.2
103100        ELSE GO TO IND-FAIL-GF-27.                                NC1234.2
103200     GO TO IND-WRITE-GF-27.                                       NC1234.2
103300 IND-DELETE-GF-27.                                                NC1234.2
103400     PERFORM DE-LETE.                                             NC1234.2
103500     GO TO IND-WRITE-GF-27.                                       NC1234.2
103600 IND-FAIL-GF-27.                                                  NC1234.2
103700     PERFORM FAIL.                                                NC1234.2
103800     MOVE NUM-9V9 TO COMPUTED-14V4.                               NC1234.2
103900     MOVE 7.0 TO CORRECT-14V4.                                    NC1234.2
104000 IND-WRITE-GF-27.                                                 NC1234.2
104100     MOVE "SUBTRCT-TEST-GF-27" TO PAR-NAME.                       NC1234.2
104200     PERFORM PRINT-DETAIL.                                        NC1234.2
104300 IND-INIT-GF-28.                                                  NC1234.2
104400     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
104500     MOVE "SUBTRACT GIVING" TO FEATURE.                           NC1234.2
104600     MOVE ZERO TO TABLE3.                                         NC1234.2
104700     PERFORM BUILD-TABLE2.                                        NC1234.2
104800     SET INDEX1 TO 8.                                             NC1234.2
104900     SET INDEX2 TO 2.                                             NC1234.2
105000     SET INDEX3 TO 6.                                             NC1234.2
105100 IND-TEST-GF-28-0.                                                NC1234.2
105200     SUBTRACT TABLE1-NUM (INDEX1 + 2) FROM                        NC1234.2
105300        TABLE2-NUM (INDEX2 + 1) GIVING TABLE3-NUM (INDEX3 - 1).   NC1234.2
105400 IND-TEST-GF-28-1.                                                NC1234.2
105500     IF TABLE3-NUM (INDEX3 - 1) EQUAL TO 18.0                     NC1234.2
105600        PERFORM PASS                                              NC1234.2
105700        ELSE GO TO IND-FAIL-GF-28.                                NC1234.2
105800     GO TO IND-WRITE-GF-28.                                       NC1234.2
105900 IND-DELETE-GF-28.                                                NC1234.2
106000     PERFORM DE-LETE.                                             NC1234.2
106100     GO TO IND-WRITE-GF-28.                                       NC1234.2
106200 IND-FAIL-GF-28.                                                  NC1234.2
106300     PERFORM FAIL.                                                NC1234.2
106400     MOVE TABLE3-NUM (INDEX3 - 1) TO COMPUTED-14V4.               NC1234.2
106500     MOVE 18.0 TO CORRECT-14V4.                                   NC1234.2
106600 IND-WRITE-GF-28.                                                 NC1234.2
106700     MOVE "SUBTRCT-TEST-GF-28" TO PAR-NAME.                       NC1234.2
106800     PERFORM PRINT-DETAIL.                                        NC1234.2
106900 IND-INIT-GF-29.                                                  NC1234.2
107000     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
107100     MOVE "GO TO DEPENDING ON" TO FEATURE.                        NC1234.2
107200     SET INDEX5 TO 1.                                             NC1234.2
107300 IND-TEST-GF-29.                                                  NC1234.2
107400     GO TO IND-A                                                  NC1234.2
107500           IND-B                                                  NC1234.2
107600           IND-C DEPENDING ON TABLE5-NUM (INDEX5).                NC1234.2
107700     GO TO IND-FAIL-GF-29.                                        NC1234.2
107800 IND-DELETE-GF-29.                                                NC1234.2
107900     PERFORM DE-LETE.                                             NC1234.2
108000     GO TO IND-WRITE-GF-29.                                       NC1234.2
108100 IND-FAIL-GF-29.                                                  NC1234.2
108200     PERFORM FAIL.                                                NC1234.2
108300     MOVE "TRANSFERED CONTROL TO WRONG PAR" TO RE-MARK.           NC1234.2
108400     GO TO IND-WRITE-GF-29.                                       NC1234.2
108500 IND-A.                                                           NC1234.2
108600     MOVE "IND-A" TO COMPUTED-A.                                  NC1234.2
108700     MOVE "IND-C" TO CORRECT-A.                                   NC1234.2
108800     GO TO IND-FAIL-GF-29.                                        NC1234.2
108900 IND-B.                                                           NC1234.2
109000     MOVE "IND-B" TO COMPUTED-A.                                  NC1234.2
109100     MOVE "IND-C" TO CORRECT-A.                                   NC1234.2
109200     GO TO IND-FAIL-GF-29.                                        NC1234.2
109300 IND-C.                                                           NC1234.2
109400     PERFORM PASS.                                                NC1234.2
109500 IND-WRITE-GF-29.                                                 NC1234.2
109600     MOVE "IND-TEST-GF-29" TO PAR-NAME.                           NC1234.2
109700     PERFORM PRINT-DETAIL.                                        NC1234.2
109800 IND-INIT-GF-30.                                                  NC1234.2
109900     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1234.2
110000     MOVE "GO TO DEPENDING ON" TO FEATURE.                        NC1234.2
110100     SET INDEX5 TO 1.                                             NC1234.2
110200 IND-TEST-GF-30.                                                  NC1234.2
110300     GO TO IND-D                                                  NC1234.2
110400           IND-E                                                  NC1234.2
110500           IND-F DEPENDING ON TABLE5-NUM (INDEX5 + 1).            NC1234.2
110600     GO TO IND-FAIL-GF-30.                                        NC1234.2
110700 IND-DELETE-GF-30.                                                NC1234.2
110800     PERFORM DE-LETE.                                             NC1234.2
110900     GO TO IND-WRITE-GF-30.                                       NC1234.2
111000 IND-FAIL-GF-30.                                                  NC1234.2
111100     PERFORM FAIL.                                                NC1234.2
111200     MOVE "TRANSFERED CONTROL TO WRONG PAR" TO RE-MARK.           NC1234.2
111300     GO TO IND-WRITE-GF-30.                                       NC1234.2
111400 IND-D.                                                           NC1234.2
111500     MOVE "IND-D" TO COMPUTED-A.                                  NC1234.2
111600     MOVE "IND-E" TO CORRECT-A.                                   NC1234.2
111700     GO TO IND-FAIL-GF-30.                                        NC1234.2
111800 IND-F.                                                           NC1234.2
111900     MOVE "IND-F" TO COMPUTED-A.                                  NC1234.2
112000     MOVE "IND-E" TO CORRECT-A.                                   NC1234.2
112100     GO TO IND-FAIL-GF-30.                                        NC1234.2
112200 IND-E.                                                           NC1234.2
112300     PERFORM PASS.                                                NC1234.2
112400 IND-WRITE-GF-30.                                                 NC1234.2
112500     MOVE "IND-TEST-GF-30" TO PAR-NAME.                           NC1234.2
112600     PERFORM PRINT-DETAIL.                                        NC1234.2
112700 CCVS-EXIT SECTION.                                               NC1234.2
112800 CCVS-999999.                                                     NC1234.2
112900     GO TO CLOSE-FILES.                                           NC1234.2
      *END-OF,NC123A                                                                  
