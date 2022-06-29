      *HEADER,COBOL,NC220M                                                            
000100 IDENTIFICATION DIVISION.                                         NC2204.2
000200 PROGRAM-ID.                                                      NC2204.2
000300     NC220M.                                                      NC2204.2
000400****************************************************************  NC2204.2
000500*                                                              *  NC2204.2
000600*    VALIDATION FOR:-                                          *  NC2204.2
000700*                                                              *  NC2204.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2204.2
000900*                                                              *  NC2204.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2204.2
001100*                                                              *  NC2204.2
001200****************************************************************  NC2204.2
001300*                                                              *  NC2204.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2204.2
001500*                                                              *  NC2204.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2204.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2204.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2204.2
001900*                                                              *  NC2204.2
002000****************************************************************  NC2204.2
002100*                                                              *  NC2204.2
002200*    PROGRAM NC220M TESTS THE USE OF INDEXED IDENTIFIERS AND   *  NC2204.2
002300*    QUALIFIED DATANAMES WITH FORMAT 1 OF THE "MULTIPLY"       *  NC2204.2
002400*    STATEMENT, FORMATS 3 & 4 OF THE "PERFORM" STATEMENT AND   *  NC2204.2
002500*    THE GENERAL FORMAT OF THE "DISPLAY" STATEMENT.            *  NC2204.2
002600*                                                              *  NC2204.2
002700*                                                              *  NC2204.2
002800****************************************************************  NC2204.2
002900 ENVIRONMENT DIVISION.                                            NC2204.2
003000 CONFIGURATION SECTION.                                           NC2204.2
003100 SOURCE-COMPUTER.                                                 NC2204.2
003200     XXXXX082.                                                    NC2204.2
003300 OBJECT-COMPUTER.                                                 NC2204.2
003400     XXXXX083.                                                    NC2204.2
003500 SPECIAL-NAMES.                                                   NC2204.2
003600     XXXXX056                                                     NC2204.2
003700     IS DISPLAY-OUTPUT-DEVICE.                                    NC2204.2
003800 INPUT-OUTPUT SECTION.                                            NC2204.2
003900 FILE-CONTROL.                                                    NC2204.2
004000     SELECT PRINT-FILE ASSIGN TO                                  NC2204.2
004100     XXXXX055.                                                    NC2204.2
004200 DATA DIVISION.                                                   NC2204.2
004300 FILE SECTION.                                                    NC2204.2
004400 FD  PRINT-FILE.                                                  NC2204.2
004500 01  PRINT-REC PICTURE X(120).                                    NC2204.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 NC2204.2
004700 WORKING-STORAGE SECTION.                                         NC2204.2
004800 01  TABLE1.                                                      NC2204.2
004900     02  TABLE1-REC              PICTURE X(10)                    NC2204.2
005000                                 OCCURS 2 TIMES                   NC2204.2
005100                                 INDEXED BY INDEX1.               NC2204.2
005200 01  TABLE2.                                                      NC2204.2
005300     02  NUMBER1                 PICTURE 99   VALUE 03.           NC2204.2
005400     02  NUMBER2                 PICTURE 99                       NC2204.2
005500                                 OCCURS 4 TIMES                   NC2204.2
005600                                 INDEXED BY INDEX2.               NC2204.2
005700     02  NUMBER3                 PICTURE 99   VALUE 06.           NC2204.2
005800 01  TABLE3.                                                      NC2204.2
005900     02  NUMBER1                 PICTURE 99   VALUE 10.           NC2204.2
006000     02  NUMBER2                 PICTURE 99                       NC2204.2
006100                                 OCCURS 4 TIMES                   NC2204.2
006200                                 INDEXED BY INDEX3.               NC2204.2
006300     02  NUMBER3                 PICTURE 99   VALUE 13.           NC2204.2
006400 01  TABLE4.                                                      NC2204.2
006500     02  TABLE4-NUM1             OCCURS 3 TIMES                   NC2204.2
006600                                 INDEXED BY INDEX4-1.             NC2204.2
006700         03 TABLE4-NUM2          PICTURE 99                       NC2204.2
006800                                 OCCURS 3 TIMES                   NC2204.2
006900                                 INDEXED BY INDEX4-2.             NC2204.2
007000 01  TABLE5.                                                      NC2204.2
007100     02  TABLE5-NUM              PICTURE 999                      NC2204.2
007200                                 OCCURS 6 TIMES                   NC2204.2
007300                                 INDEXED BY INDEX5.               NC2204.2
007400 01  TABLE6.                                                      NC2204.2
007500     02  TABLE6-NUM              PICTURE 999                      NC2204.2
007600                                 OCCURS 6 TIMES                   NC2204.2
007700                                 INDEXED BY INDEX6.               NC2204.2
007800 01  TABLE7.                                                      NC2204.2
007900     02  TABLE7-NUM              PICTURE 9                        NC2204.2
008000                                 OCCURS 2 TIMES                   NC2204.2
008100                                 INDEXED BY INDEX7.               NC2204.2
008200 01  TABLE8.                                                      NC2204.2
008300     02  TABLE8-NUM              PICTURE 9                        NC2204.2
008400                                 OCCURS 3 TIMES                   NC2204.2
008500                                 INDEXED BY INDEX8.               NC2204.2
008600 01  NUM-9                       PICTURE 9.                       NC2204.2
008700 01  NUM-999                     PICTURE 999.                     NC2204.2
008800 01  TEST-RESULTS.                                                NC2204.2
008900     02 FILLER                   PIC X      VALUE SPACE.          NC2204.2
009000     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2204.2
009100     02 FILLER                   PIC X      VALUE SPACE.          NC2204.2
009200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2204.2
009300     02 FILLER                   PIC X      VALUE SPACE.          NC2204.2
009400     02  PAR-NAME.                                                NC2204.2
009500       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2204.2
009600       03  PARDOT-X              PIC X      VALUE SPACE.          NC2204.2
009700       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2204.2
009800     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2204.2
009900     02 RE-MARK                  PIC X(61).                       NC2204.2
010000 01  TEST-COMPUTED.                                               NC2204.2
010100     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2204.2
010200     02 FILLER                   PIC X(17)  VALUE                 NC2204.2
010300            "       COMPUTED=".                                   NC2204.2
010400     02 COMPUTED-X.                                               NC2204.2
010500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2204.2
010600     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2204.2
010700                                 PIC -9(9).9(9).                  NC2204.2
010800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2204.2
010900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2204.2
011000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2204.2
011100     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2204.2
011200         04 COMPUTED-18V0                    PIC -9(18).          NC2204.2
011300         04 FILLER                           PIC X.               NC2204.2
011400     03 FILLER PIC X(50) VALUE SPACE.                             NC2204.2
011500 01  TEST-CORRECT.                                                NC2204.2
011600     02 FILLER PIC X(30) VALUE SPACE.                             NC2204.2
011700     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2204.2
011800     02 CORRECT-X.                                                NC2204.2
011900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2204.2
012000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2204.2
012100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2204.2
012200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2204.2
012300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2204.2
012400     03      CR-18V0 REDEFINES CORRECT-A.                         NC2204.2
012500         04 CORRECT-18V0                     PIC -9(18).          NC2204.2
012600         04 FILLER                           PIC X.               NC2204.2
012700     03 FILLER PIC X(2) VALUE SPACE.                              NC2204.2
012800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2204.2
012900 01  CCVS-C-1.                                                    NC2204.2
013000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2204.2
013100-    "SS  PARAGRAPH-NAME                                          NC2204.2
013200-    "       REMARKS".                                            NC2204.2
013300     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2204.2
013400 01  CCVS-C-2.                                                    NC2204.2
013500     02 FILLER                     PIC X        VALUE SPACE.      NC2204.2
013600     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2204.2
013700     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2204.2
013800     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2204.2
013900     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2204.2
014000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2204.2
014100 01  REC-CT                        PIC 99       VALUE ZERO.       NC2204.2
014200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2204.2
014300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2204.2
014400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2204.2
014500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2204.2
014600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2204.2
014700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2204.2
014800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2204.2
014900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2204.2
015000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2204.2
015100 01  CCVS-H-1.                                                    NC2204.2
015200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2204.2
015300     02  FILLER                    PIC X(42)    VALUE             NC2204.2
015400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2204.2
015500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2204.2
015600 01  CCVS-H-2A.                                                   NC2204.2
015700   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2204.2
015800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2204.2
015900   02  FILLER                        PIC XXXX   VALUE             NC2204.2
016000     "4.2 ".                                                      NC2204.2
016100   02  FILLER                        PIC X(28)  VALUE             NC2204.2
016200            " COPY - NOT FOR DISTRIBUTION".                       NC2204.2
016300   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2204.2
016400                                                                  NC2204.2
016500 01  CCVS-H-2B.                                                   NC2204.2
016600   02  FILLER                        PIC X(15)  VALUE             NC2204.2
016700            "TEST RESULT OF ".                                    NC2204.2
016800   02  TEST-ID                       PIC X(9).                    NC2204.2
016900   02  FILLER                        PIC X(4)   VALUE             NC2204.2
017000            " IN ".                                               NC2204.2
017100   02  FILLER                        PIC X(12)  VALUE             NC2204.2
017200     " HIGH       ".                                              NC2204.2
017300   02  FILLER                        PIC X(22)  VALUE             NC2204.2
017400            " LEVEL VALIDATION FOR ".                             NC2204.2
017500   02  FILLER                        PIC X(58)  VALUE             NC2204.2
017600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2204.2
017700 01  CCVS-H-3.                                                    NC2204.2
017800     02  FILLER                      PIC X(34)  VALUE             NC2204.2
017900            " FOR OFFICIAL USE ONLY    ".                         NC2204.2
018000     02  FILLER                      PIC X(58)  VALUE             NC2204.2
018100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2204.2
018200     02  FILLER                      PIC X(28)  VALUE             NC2204.2
018300            "  COPYRIGHT   1985 ".                                NC2204.2
018400 01  CCVS-E-1.                                                    NC2204.2
018500     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2204.2
018600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2204.2
018700     02 ID-AGAIN                     PIC X(9).                    NC2204.2
018800     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2204.2
018900 01  CCVS-E-2.                                                    NC2204.2
019000     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2204.2
019100     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2204.2
019200     02 CCVS-E-2-2.                                               NC2204.2
019300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2204.2
019400         03 FILLER                   PIC X      VALUE SPACE.      NC2204.2
019500         03 ENDER-DESC               PIC X(44)  VALUE             NC2204.2
019600            "ERRORS ENCOUNTERED".                                 NC2204.2
019700 01  CCVS-E-3.                                                    NC2204.2
019800     02  FILLER                      PIC X(22)  VALUE             NC2204.2
019900            " FOR OFFICIAL USE ONLY".                             NC2204.2
020000     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2204.2
020100     02  FILLER                      PIC X(58)  VALUE             NC2204.2
020200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2204.2
020300     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2204.2
020400     02 FILLER                       PIC X(15)  VALUE             NC2204.2
020500             " COPYRIGHT 1985".                                   NC2204.2
020600 01  CCVS-E-4.                                                    NC2204.2
020700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2204.2
020800     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2204.2
020900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2204.2
021000     02 FILLER                       PIC X(40)  VALUE             NC2204.2
021100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2204.2
021200 01  XXINFO.                                                      NC2204.2
021300     02 FILLER                       PIC X(19)  VALUE             NC2204.2
021400            "*** INFORMATION ***".                                NC2204.2
021500     02 INFO-TEXT.                                                NC2204.2
021600       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2204.2
021700       04 XXCOMPUTED                 PIC X(20).                   NC2204.2
021800       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2204.2
021900       04 XXCORRECT                  PIC X(20).                   NC2204.2
022000     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2204.2
022100 01  HYPHEN-LINE.                                                 NC2204.2
022200     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2204.2
022300     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2204.2
022400-    "*****************************************".                 NC2204.2
022500     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2204.2
022600-    "******************************".                            NC2204.2
022700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2204.2
022800     "NC220M".                                                    NC2204.2
022900 PROCEDURE DIVISION.                                              NC2204.2
023000 CCVS1 SECTION.                                                   NC2204.2
023100 OPEN-FILES.                                                      NC2204.2
023200     OPEN     OUTPUT PRINT-FILE.                                  NC2204.2
023300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2204.2
023400     MOVE    SPACE TO TEST-RESULTS.                               NC2204.2
023500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2204.2
023600     GO TO CCVS1-EXIT.                                            NC2204.2
023700 CLOSE-FILES.                                                     NC2204.2
023800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2204.2
023900 TERMINATE-CCVS.                                                  NC2204.2
024000*    EXIT PROGRAM.                                                NC2204.2
024100*TERMINATE-CALL.                                                  NC2204.2
024200     STOP     RUN.                                                NC2204.2
024300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2204.2
024400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2204.2
024500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2204.2
024600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2204.2
024700     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2204.2
024800 PRINT-DETAIL.                                                    NC2204.2
024900     IF REC-CT NOT EQUAL TO ZERO                                  NC2204.2
025000             MOVE "." TO PARDOT-X                                 NC2204.2
025100             MOVE REC-CT TO DOTVALUE.                             NC2204.2
025200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2204.2
025300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2204.2
025400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2204.2
025500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2204.2
025600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2204.2
025700     MOVE SPACE TO CORRECT-X.                                     NC2204.2
025800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2204.2
025900     MOVE     SPACE TO RE-MARK.                                   NC2204.2
026000 HEAD-ROUTINE.                                                    NC2204.2
026100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2204.2
026200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2204.2
026300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2204.2
026400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2204.2
026500 COLUMN-NAMES-ROUTINE.                                            NC2204.2
026600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2204.2
026700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2204.2
026800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2204.2
026900 END-ROUTINE.                                                     NC2204.2
027000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2204.2
027100 END-RTN-EXIT.                                                    NC2204.2
027200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2204.2
027300 END-ROUTINE-1.                                                   NC2204.2
027400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2204.2
027500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2204.2
027600      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2204.2
027700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2204.2
027800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2204.2
027900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2204.2
028000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2204.2
028100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2204.2
028200  END-ROUTINE-12.                                                 NC2204.2
028300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2204.2
028400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2204.2
028500         MOVE "NO " TO ERROR-TOTAL                                NC2204.2
028600         ELSE                                                     NC2204.2
028700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2204.2
028800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2204.2
028900     PERFORM WRITE-LINE.                                          NC2204.2
029000 END-ROUTINE-13.                                                  NC2204.2
029100     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2204.2
029200         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2204.2
029300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2204.2
029400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2204.2
029500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2204.2
029600      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2204.2
029700          MOVE "NO " TO ERROR-TOTAL                               NC2204.2
029800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2204.2
029900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2204.2
030000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2204.2
030100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2204.2
030200 WRITE-LINE.                                                      NC2204.2
030300     ADD 1 TO RECORD-COUNT.                                       NC2204.2
030400*    IF RECORD-COUNT GREATER 50                                   NC2204.2
030500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2204.2
030600*        MOVE SPACE TO DUMMY-RECORD                               NC2204.2
030700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2204.2
030800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2204.2
030900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2204.2
031000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2204.2
031100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2204.2
031200*        MOVE ZERO TO RECORD-COUNT.                               NC2204.2
031300     PERFORM WRT-LN.                                              NC2204.2
031400 WRT-LN.                                                          NC2204.2
031500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2204.2
031600     MOVE SPACE TO DUMMY-RECORD.                                  NC2204.2
031700 BLANK-LINE-PRINT.                                                NC2204.2
031800     PERFORM WRT-LN.                                              NC2204.2
031900 FAIL-ROUTINE.                                                    NC2204.2
032000     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2204.2
032100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2204.2
032200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2204.2
032300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2204.2
032400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2204.2
032500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2204.2
032600     GO TO  FAIL-ROUTINE-EX.                                      NC2204.2
032700 FAIL-ROUTINE-WRITE.                                              NC2204.2
032800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2204.2
032900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2204.2
033000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2204.2
033100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2204.2
033200 FAIL-ROUTINE-EX. EXIT.                                           NC2204.2
033300 BAIL-OUT.                                                        NC2204.2
033400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2204.2
033500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2204.2
033600 BAIL-OUT-WRITE.                                                  NC2204.2
033700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2204.2
033800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2204.2
033900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2204.2
034000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2204.2
034100 BAIL-OUT-EX. EXIT.                                               NC2204.2
034200 CCVS1-EXIT.                                                      NC2204.2
034300     EXIT.                                                        NC2204.2
034400 SECT-NC220M-001 SECTION.                                         NC2204.2
034500 BUILD-TABLE1.                                                    NC2204.2
034600     MOVE "LITERAL-02" TO TABLE1-REC (1).                         NC2204.2
034700     MOVE "9876543210" TO TABLE1-REC (2).                         NC2204.2
034800 BUILD-TABLE2.                                                    NC2204.2
034900     MOVE 04 TO NUMBER2 OF TABLE2 (1).                            NC2204.2
035000     MOVE 23 TO NUMBER2 OF TABLE2 (2).                            NC2204.2
035100     MOVE 02 TO NUMBER2 OF TABLE2 (3).                            NC2204.2
035200     MOVE 06 TO NUMBER2 OF TABLE2 (4).                            NC2204.2
035300 BUILD-TABLE3.                                                    NC2204.2
035400     MOVE 11 TO NUMBER2 OF TABLE3 (1).                            NC2204.2
035500     MOVE 04 TO NUMBER2 OF TABLE3 (2).                            NC2204.2
035600     MOVE 04 TO NUMBER2 OF TABLE3 (3).                            NC2204.2
035700     MOVE 24 TO NUMBER2 OF TABLE3 (4).                            NC2204.2
035800 BUILD-TABLE4.                                                    NC2204.2
035900     MOVE 03 TO TABLE4-NUM2 (1, 1).                               NC2204.2
036000     MOVE 04 TO TABLE4-NUM2 (1, 2).                               NC2204.2
036100     MOVE 05 TO TABLE4-NUM2 (1, 3).                               NC2204.2
036200     MOVE 12 TO TABLE4-NUM2 (2, 1).                               NC2204.2
036300     MOVE 13 TO TABLE4-NUM2 (2, 2).                               NC2204.2
036400     MOVE 14 TO TABLE4-NUM2 (2, 3).                               NC2204.2
036500     MOVE 31 TO TABLE4-NUM2 (3, 1).                               NC2204.2
036600     MOVE 32 TO TABLE4-NUM2 (3, 2).                               NC2204.2
036700     MOVE 33 TO TABLE4-NUM2 (3, 3).                               NC2204.2
036800 BUILD-TABLE5.                                                    NC2204.2
036900     MOVE 011 TO TABLE5-NUM (1).                                  NC2204.2
037000     MOVE 005 TO TABLE5-NUM (2).                                  NC2204.2
037100     MOVE 597 TO TABLE5-NUM (3).                                  NC2204.2
037200     MOVE 036 TO TABLE5-NUM (4).                                  NC2204.2
037300     MOVE 082 TO TABLE5-NUM (5).                                  NC2204.2
037400     MOVE 125 TO TABLE5-NUM (6).                                  NC2204.2
037500 BUILD-TABLE7.                                                    NC2204.2
037600     MOVE 1 TO TABLE7-NUM (1).                                    NC2204.2
037700     MOVE 9 TO TABLE7-NUM (2).                                    NC2204.2
037800 BUILD-TABLE8.                                                    NC2204.2
037900     MOVE 4 TO TABLE8-NUM (1).                                    NC2204.2
038000     MOVE 7 TO TABLE8-NUM (2).                                    NC2204.2
038100     MOVE 2 TO TABLE8-NUM (3).                                    NC2204.2
038200*                                                                 NC2204.2
038300 DIS-INIT-GF-1.                                                   NC2204.2
038400     MOVE "DIS-TEST-GF-1" TO PAR-NAME.                            NC2204.2
038500     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
038600     MOVE "DISPLAY UPON" TO FEATURE.                              NC2204.2
038700     MOVE "RESULTS MUST BE" TO RE-MARK.                           NC2204.2
038800     MOVE "LITERAL-02" TO CORRECT-A.                              NC2204.2
038900     PERFORM BUILD-TABLE1.                                        NC2204.2
039000     SET INDEX1 TO 1.                                             NC2204.2
039100 DIS-TEST-GF-1.                                                   NC2204.2
039200     DISPLAY "   " UPON DISPLAY-OUTPUT-DEVICE.                    NC2204.2
039300     DISPLAY TABLE1-REC (INDEX1) UPON DISPLAY-OUTPUT-DEVICE.      NC2204.2
039400     PERFORM INSPT.                                               NC2204.2
039500     GO TO DIS-WRITE-GF-1.                                        NC2204.2
039600 DIS-DELETE-GF-1.                                                 NC2204.2
039700     PERFORM DE-LETE.                                             NC2204.2
039800 DIS-WRITE-GF-1.                                                  NC2204.2
039900     PERFORM PRINT-DETAIL.                                        NC2204.2
040000*                                                                 NC2204.2
040100 DIS-INIT-GF-2.                                                   NC2204.2
040200     MOVE "DIS-TEST-GF-2" TO PAR-NAME.                            NC2204.2
040300     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
040400     MOVE "DISPLAY UPON" TO FEATURE.                              NC2204.2
040500     MOVE "VISUALLY CHECKED" TO RE-MARK.                          NC2204.2
040600     MOVE "9876543210" TO CORRECT-A.                              NC2204.2
040700     PERFORM BUILD-TABLE1.                                        NC2204.2
040800     SET INDEX1 TO 1.                                             NC2204.2
040900 DIS-TEST-GF-2.                                                   NC2204.2
041000     DISPLAY TABLE1-REC (INDEX1 + 1)                              NC2204.2
041100        UPON DISPLAY-OUTPUT-DEVICE.                               NC2204.2
041200     PERFORM INSPT.                                               NC2204.2
041300     GO TO DIS-WRITE-GF-2.                                        NC2204.2
041400 DIS-DELETE-GF-2.                                                 NC2204.2
041500     PERFORM DE-LETE.                                             NC2204.2
041600 DIS-WRITE-GF-2.                                                  NC2204.2
041700     PERFORM PRINT-DETAIL.                                        NC2204.2
041800*                                                                 NC2204.2
041900 MLT-INIT-F1-1.                                                   NC2204.2
042000     MOVE "MLT-TEST-F1-1" TO PAR-NAME.                            NC2204.2
042100     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
042200     MOVE "MULTIPLY BY" TO FEATURE.                               NC2204.2
042300     PERFORM BUILD-TABLE2.                                        NC2204.2
042400     PERFORM BUILD-TABLE3.                                        NC2204.2
042500     SET INDEX2 TO 1.                                             NC2204.2
042600     SET INDEX3 TO 1.                                             NC2204.2
042700 MLT-TEST-F1-1.                                                   NC2204.2
042800     MULTIPLY NUMBER2 OF TABLE2 (INDEX2)                          NC2204.2
042900        BY NUMBER2 OF TABLE3 (INDEX3).                            NC2204.2
043000     IF NUMBER2 OF TABLE3 (INDEX3) = 44                           NC2204.2
043100        PERFORM PASS                                              NC2204.2
043200        ELSE GO TO MLT-FAIL-F1-1.                                 NC2204.2
043300     GO TO MLT-WRITE-F1-1.                                        NC2204.2
043400 MLT-DELETE-F1-1.                                                 NC2204.2
043500     PERFORM DE-LETE.                                             NC2204.2
043600     GO TO MLT-WRITE-F1-1.                                        NC2204.2
043700 MLT-FAIL-F1-1.                                                   NC2204.2
043800     PERFORM FAIL.                                                NC2204.2
043900     MOVE NUMBER2 OF TABLE3 (INDEX3) TO COMPUTED-18V0.            NC2204.2
044000     MOVE 44 TO CORRECT-18V0.                                     NC2204.2
044100 MLT-WRITE-F1-1.                                                  NC2204.2
044200     PERFORM PRINT-DETAIL.                                        NC2204.2
044300*                                                                 NC2204.2
044400 MLT-INIT-F1-2.                                                   NC2204.2
044500     MOVE "MLT-TEST-F1-2" TO PAR-NAME.                            NC2204.2
044600     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
044700     MOVE "MULTIPLY BY" TO FEATURE.                               NC2204.2
044800     PERFORM BUILD-TABLE2.                                        NC2204.2
044900     PERFORM BUILD-TABLE3.                                        NC2204.2
045000     PERFORM BUILD-TABLE4.                                        NC2204.2
045100     SET INDEX2 TO 1.                                             NC2204.2
045200     SET INDEX3 TO 1.                                             NC2204.2
045300 MLT-TEST-F1-2.                                                   NC2204.2
045400     MULTIPLY NUMBER2 OF TABLE2 (INDEX2 + 1)                      NC2204.2
045500        BY NUMBER2 OF TABLE3 (INDEX3 + 1).                        NC2204.2
045600     IF NUMBER2 OF TABLE3 (INDEX3 + 1) = 92                       NC2204.2
045700        PERFORM PASS                                              NC2204.2
045800        ELSE GO TO MLT-FAIL-F1-2.                                 NC2204.2
045900     GO TO MLT-WRITE-F1-2.                                        NC2204.2
046000 MLT-DELETE-F1-2.                                                 NC2204.2
046100     PERFORM DE-LETE.                                             NC2204.2
046200     GO TO MLT-WRITE-F1-2.                                        NC2204.2
046300 MLT-FAIL-F1-2.                                                   NC2204.2
046400     PERFORM FAIL.                                                NC2204.2
046500     MOVE NUMBER2 OF TABLE3 (INDEX3 + 1) TO COMPUTED-18V0.        NC2204.2
046600     MOVE 92 TO CORRECT-18V0.                                     NC2204.2
046700 MLT-WRITE-F1-2.                                                  NC2204.2
046800     PERFORM PRINT-DETAIL.                                        NC2204.2
046900*                                                                 NC2204.2
047000 MLT-INIT-F1-3.                                                   NC2204.2
047100     MOVE "MLT-TEST-F1-3" TO PAR-NAME.                            NC2204.2
047200     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
047300     MOVE "MULTIPLY BY" TO FEATURE.                               NC2204.2
047400     PERFORM BUILD-TABLE4.                                        NC2204.2
047500     SET INDEX4-1 TO 2.                                           NC2204.2
047600     SET INDEX4-2 TO 1.                                           NC2204.2
047700 MLT-TEST-F1-3.                                                   NC2204.2
047800     MULTIPLY TABLE4-NUM2 (1, 3)                                  NC2204.2
047900        BY TABLE4-NUM2 (INDEX4-1, INDEX4-2).                      NC2204.2
048000     IF TABLE4-NUM2 (INDEX4-1, INDEX4-2) = 60                     NC2204.2
048100        PERFORM PASS                                              NC2204.2
048200        ELSE GO TO MLT-FAIL-F1-3.                                 NC2204.2
048300     GO TO MLT-WRITE-F1-3.                                        NC2204.2
048400 MLT-DELETE-F1-3.                                                 NC2204.2
048500     PERFORM DE-LETE.                                             NC2204.2
048600     GO TO MLT-WRITE-F1-3.                                        NC2204.2
048700 MLT-FAIL-F1-3.                                                   NC2204.2
048800     PERFORM FAIL.                                                NC2204.2
048900     MOVE TABLE4-NUM2 (INDEX4-1, INDEX4-2) TO COMPUTED-18V0.      NC2204.2
049000     MOVE 60 TO CORRECT-18V0.                                     NC2204.2
049100 MLT-WRITE-F1-3.                                                  NC2204.2
049200     PERFORM PRINT-DETAIL.                                        NC2204.2
049300*                                                                 NC2204.2
049400 DIV-INIT-F5-1.                                                   NC2204.2
049500     MOVE "DIV-TEST-F5-1" TO PAR-NAME.                            NC2204.2
049600     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
049700     MOVE "DIVIDE BY REMAINDER" TO FEATURE.                       NC2204.2
049800     MOVE 1 TO REC-CT.                                            NC2204.2
049900     MOVE ZEROS TO TABLE6.                                        NC2204.2
050000     MOVE ZEROS TO NUM-999.                                       NC2204.2
050100     PERFORM BUILD-TABLE5.                                        NC2204.2
050200     SET INDEX5 TO 1.                                             NC2204.2
050300     SET INDEX6 TO 1.                                             NC2204.2
050400 DIV-TEST-F5-1.                                                   NC2204.2
050500     DIVIDE TABLE5-NUM (INDEX5) BY TABLE5-NUM (INDEX5 + 1)        NC2204.2
050600        GIVING TABLE6-NUM (INDEX6) REMAINDER NUM-999.             NC2204.2
050700     GO TO DIV-TEST-F5-1-1.                                       NC2204.2
050800 DIV-DELETE-F5-1.                                                 NC2204.2
050900     PERFORM DE-LETE.                                             NC2204.2
051000     PERFORM PRINT-DETAIL.                                        NC2204.2
051100     GO TO DIV-TEST-F5-2.                                         NC2204.2
051200*                                                                 NC2204.2
051300 DIV-TEST-F5-1-1.                                                 NC2204.2
051400     MOVE "QUOTIENT" TO RE-MARK.                                  NC2204.2
051500     IF TABLE6-NUM (INDEX6) = 2                                   NC2204.2
051600           PERFORM PASS                                           NC2204.2
051700           GO TO DIV-WRITE-F5-1-1                                 NC2204.2
051800     ELSE                                                         NC2204.2
051900           GO TO DIV-FAIL-F5-1-1.                                 NC2204.2
052000 DIV-DELETE-F5-1-1.                                               NC2204.2
052100     PERFORM DE-LETE.                                             NC2204.2
052200     GO TO DIV-WRITE-F5-1-1.                                      NC2204.2
052300 DIV-FAIL-F5-1-1.                                                 NC2204.2
052400     PERFORM FAIL                                                 NC2204.2
052500     MOVE TABLE6-NUM (INDEX6) TO COMPUTED-18V0                    NC2204.2
052600     MOVE 2 TO CORRECT-18V0.                                      NC2204.2
052700 DIV-WRITE-F5-1-1.                                                NC2204.2
052800     PERFORM PRINT-DETAIL.                                        NC2204.2
052900*                                                                 NC2204.2
053000 DIV-TEST-F5-1-2.                                                 NC2204.2
053100     ADD 1 TO REC-CT.                                             NC2204.2
053200     MOVE "REMAINDER" TO RE-MARK.                                 NC2204.2
053300     IF NUM-999 = 1                                               NC2204.2
053400           PERFORM PASS                                           NC2204.2
053500           GO TO DIV-WRITE-F5-1-2                                 NC2204.2
053600     ELSE                                                         NC2204.2
053700           GO TO DIV-FAIL-F5-1-2.                                 NC2204.2
053800 DIV-DELETE-F5-1-2.                                               NC2204.2
053900     PERFORM DE-LETE.                                             NC2204.2
054000     GO TO DIV-WRITE-F5-1-2.                                      NC2204.2
054100 DIV-FAIL-F5-1-2.                                                 NC2204.2
054200     PERFORM FAIL                                                 NC2204.2
054300     MOVE NUM-999 TO COMPUTED-18V0                                NC2204.2
054400     MOVE 1 TO CORRECT-18V0.                                      NC2204.2
054500 DIV-WRITE-F5-1-2.                                                NC2204.2
054600     PERFORM PRINT-DETAIL.                                        NC2204.2
054700*                                                                 NC2204.2
054800 DIV-INIT-F5-2.                                                   NC2204.2
054900     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
055000     MOVE "DIV-TEST-F5-2" TO PAR-NAME.                            NC2204.2
055100     MOVE "DIVIDE BY REMAINDER" TO FEATURE.                       NC2204.2
055200     MOVE 1 TO REC-CT.                                            NC2204.2
055300     MOVE ZEROS TO TABLE6.                                        NC2204.2
055400     MOVE ZEROS TO NUM-999.                                       NC2204.2
055500     SET INDEX5 TO 3.                                             NC2204.2
055600     SET INDEX6 TO 3.                                             NC2204.2
055700 DIV-TEST-F5-2.                                                   NC2204.2
055800     DIVIDE TABLE5-NUM (INDEX5) BY TABLE5-NUM (INDEX5 + 1)        NC2204.2
055900        GIVING NUM-999 REMAINDER TABLE6-NUM (INDEX6).             NC2204.2
056000     GO TO DIV-TEST-F5-2-1.                                       NC2204.2
056100 DIV-DELETE-F5-2.                                                 NC2204.2
056200     PERFORM DE-LETE.                                             NC2204.2
056300     PERFORM PRINT-DETAIL.                                        NC2204.2
056400     GO TO DIV-TEST-F5-3.                                         NC2204.2
056500*                                                                 NC2204.2
056600 DIV-TEST-F5-2-1.                                                 NC2204.2
056700     MOVE "QUOTIENT" TO RE-MARK.                                  NC2204.2
056800     IF NUM-999 = 16                                              NC2204.2
056900           PERFORM PASS                                           NC2204.2
057000           GO TO DIV-WRITE-F5-2-1                                 NC2204.2
057100     ELSE                                                         NC2204.2
057200           GO TO DIV-FAIL-F5-2-1.                                 NC2204.2
057300 DIV-DELETE-F5-2-1.                                               NC2204.2
057400     PERFORM DE-LETE.                                             NC2204.2
057500     GO TO DIV-WRITE-F5-2-1.                                      NC2204.2
057600 DIV-FAIL-F5-2-1.                                                 NC2204.2
057700     PERFORM FAIL                                                 NC2204.2
057800     MOVE NUM-999 TO COMPUTED-18V0                                NC2204.2
057900     MOVE 16 TO CORRECT-18V0.                                     NC2204.2
058000 DIV-WRITE-F5-2-1.                                                NC2204.2
058100     PERFORM PRINT-DETAIL.                                        NC2204.2
058200*                                                                 NC2204.2
058300 DIV-TEST-F5-2-2.                                                 NC2204.2
058400     ADD 1 TO REC-CT.                                             NC2204.2
058500     MOVE "REMAINDER" TO RE-MARK.                                 NC2204.2
058600     IF TABLE6-NUM (INDEX6) = 21                                  NC2204.2
058700           PERFORM PASS                                           NC2204.2
058800           GO TO DIV-WRITE-F5-2-2                                 NC2204.2
058900     ELSE                                                         NC2204.2
059000           GO TO DIV-FAIL-F5-2-2.                                 NC2204.2
059100 DIV-DELETE-F5-2-2.                                               NC2204.2
059200     PERFORM DE-LETE.                                             NC2204.2
059300     GO TO DIV-WRITE-F5-2-2.                                      NC2204.2
059400 DIV-FAIL-F5-2-2.                                                 NC2204.2
059500     PERFORM FAIL                                                 NC2204.2
059600     MOVE TABLE6-NUM (INDEX6) TO COMPUTED-18V0                    NC2204.2
059700     MOVE 21 TO CORRECT-18V0.                                     NC2204.2
059800 DIV-WRITE-F5-2-2.                                                NC2204.2
059900     PERFORM PRINT-DETAIL.                                        NC2204.2
060000*                                                                 NC2204.2
060100 DIV-INIT-F5-3.                                                   NC2204.2
060200     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
060300     MOVE "DIV-TEST-F5-3" TO PAR-NAME.                            NC2204.2
060400     MOVE "DIVIDE BY REMAINDER" TO FEATURE.                       NC2204.2
060500     MOVE 1 TO REC-CT.                                            NC2204.2
060600     MOVE ZEROS TO TABLE6.                                        NC2204.2
060700     SET INDEX5 TO 5.                                             NC2204.2
060800     SET INDEX6 TO 5.                                             NC2204.2
060900 DIV-TEST-F5-3.                                                   NC2204.2
061000     DIVIDE TABLE5-NUM (INDEX5) BY TABLE5-NUM (INDEX5 + 1)        NC2204.2
061100        GIVING TABLE6-NUM (INDEX6)                                NC2204.2
061200        REMAINDER TABLE6-NUM (INDEX6 + 1).                        NC2204.2
061300     GO TO DIV-TEST-F5-3-1.                                       NC2204.2
061400 DIV-DELETE-F5-3.                                                 NC2204.2
061500     PERFORM DE-LETE.                                             NC2204.2
061600     PERFORM PRINT-DETAIL.                                        NC2204.2
061700     GO TO DIV-TEST-F4-4.                                         NC2204.2
061800*                                                                 NC2204.2
061900 DIV-TEST-F5-3-1.                                                 NC2204.2
062000     MOVE "QUOTIENT" TO RE-MARK.                                  NC2204.2
062100     IF TABLE6-NUM (INDEX6) = 0                                   NC2204.2
062200           PERFORM PASS                                           NC2204.2
062300           GO TO DIV-WRITE-F5-3-1                                 NC2204.2
062400     ELSE                                                         NC2204.2
062500           GO TO DIV-FAIL-F5-3-1.                                 NC2204.2
062600 DIV-DELETE-F5-3-1.                                               NC2204.2
062700     PERFORM DE-LETE.                                             NC2204.2
062800     GO TO DIV-WRITE-F5-3-1.                                      NC2204.2
062900 DIV-FAIL-F5-3-1.                                                 NC2204.2
063000     PERFORM FAIL                                                 NC2204.2
063100     MOVE TABLE6-NUM (INDEX6) TO COMPUTED-18V0                    NC2204.2
063200     MOVE 0 TO CORRECT-18V0.                                      NC2204.2
063300 DIV-WRITE-F5-3-1.                                                NC2204.2
063400     PERFORM PRINT-DETAIL.                                        NC2204.2
063500*                                                                 NC2204.2
063600 DIV-TEST-F5-3-2.                                                 NC2204.2
063700     ADD 1 TO REC-CT.                                             NC2204.2
063800     MOVE "REMAINDER" TO RE-MARK.                                 NC2204.2
063900     IF TABLE6-NUM (INDEX6 + 1) = 82                              NC2204.2
064000           PERFORM PASS                                           NC2204.2
064100           GO TO DIV-WRITE-F5-3-2                                 NC2204.2
064200     ELSE                                                         NC2204.2
064300           GO TO DIV-FAIL-F5-3-2.                                 NC2204.2
064400 DIV-DELETE-F5-3-2.                                               NC2204.2
064500     PERFORM DE-LETE.                                             NC2204.2
064600     GO TO DIV-WRITE-F5-3-2.                                      NC2204.2
064700 DIV-FAIL-F5-3-2.                                                 NC2204.2
064800     PERFORM FAIL                                                 NC2204.2
064900     MOVE TABLE6-NUM (INDEX6 + 1) TO COMPUTED-18V0                NC2204.2
065000     MOVE 82 TO CORRECT-18V0.                                     NC2204.2
065100 DIV-WRITE-F5-3-2.                                                NC2204.2
065200     PERFORM PRINT-DETAIL.                                        NC2204.2
065300*                                                                 NC2204.2
065400 DIV-INIT-F4-4.                                                   NC2204.2
065500     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
065600     MOVE "DIV-TEST-F4-4" TO PAR-NAME.                            NC2204.2
065700     MOVE "DIVIDE INTO REMNDER" TO FEATURE.                       NC2204.2
065800     MOVE 1 TO REC-CT.                                            NC2204.2
065900     MOVE ZEROS TO TABLE6.                                        NC2204.2
066000     MOVE ZEROS TO NUM-999.                                       NC2204.2
066100     SET INDEX5 TO 1.                                             NC2204.2
066200     SET INDEX6 TO 1.                                             NC2204.2
066300 DIV-TEST-F4-4.                                                   NC2204.2
066400     DIVIDE TABLE5-NUM (INDEX5 + 1) INTO TABLE5-NUM (INDEX5)      NC2204.2
066500        GIVING TABLE6-NUM (INDEX6) REMAINDER NUM-999.             NC2204.2
066600     GO TO DIV-TEST-F4-4-1.                                       NC2204.2
066700 DIV-DELETE-F4-4.                                                 NC2204.2
066800     PERFORM DE-LETE.                                             NC2204.2
066900     PERFORM PRINT-DETAIL.                                        NC2204.2
067000     GO TO DIV-TEST-F4-5.                                         NC2204.2
067100*                                                                 NC2204.2
067200 DIV-TEST-F4-4-1.                                                 NC2204.2
067300     MOVE "QUOTIENT" TO RE-MARK.                                  NC2204.2
067400     IF TABLE6-NUM (INDEX6) = 2                                   NC2204.2
067500           PERFORM PASS                                           NC2204.2
067600           GO TO DIV-WRITE-F4-4-1                                 NC2204.2
067700     ELSE                                                         NC2204.2
067800           GO TO DIV-FAIL-F4-4-1.                                 NC2204.2
067900 DIV-DELETE-F4-4-1.                                               NC2204.2
068000     PERFORM DE-LETE.                                             NC2204.2
068100     GO TO DIV-WRITE-F4-4-1.                                      NC2204.2
068200 DIV-FAIL-F4-4-1.                                                 NC2204.2
068300     PERFORM FAIL                                                 NC2204.2
068400     MOVE TABLE6-NUM (INDEX6) TO COMPUTED-18V0                    NC2204.2
068500     MOVE 2 TO CORRECT-18V0.                                      NC2204.2
068600 DIV-WRITE-F4-4-1.                                                NC2204.2
068700     PERFORM PRINT-DETAIL.                                        NC2204.2
068800     ADD 1 TO REC-CT.                                             NC2204.2
068900*                                                                 NC2204.2
069000 DIV-TEST-F4-4-2.                                                 NC2204.2
069100     MOVE "REMAINDER" TO RE-MARK.                                 NC2204.2
069200     IF NUM-999 = 1                                               NC2204.2
069300           PERFORM PASS                                           NC2204.2
069400           GO TO DIV-WRITE-F4-4-2                                 NC2204.2
069500     ELSE                                                         NC2204.2
069600           GO TO DIV-FAIL-F4-4-2.                                 NC2204.2
069700 DIV-DELETE-F4-4-2.                                               NC2204.2
069800     PERFORM DE-LETE.                                             NC2204.2
069900     GO TO DIV-WRITE-F4-4-2.                                      NC2204.2
070000 DIV-FAIL-F4-4-2.                                                 NC2204.2
070100     PERFORM FAIL                                                 NC2204.2
070200     MOVE NUM-999 TO COMPUTED-18V0                                NC2204.2
070300     MOVE 1 TO CORRECT-18V0.                                      NC2204.2
070400 DIV-WRITE-F4-4-2.                                                NC2204.2
070500     PERFORM PRINT-DETAIL.                                        NC2204.2
070600*                                                                 NC2204.2
070700 DIV-INIT-F4-5.                                                   NC2204.2
070800     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
070900     MOVE "DIV-TEST-F4-5" TO PAR-NAME.                            NC2204.2
071000     MOVE "DIVIDE INTO REMNDER" TO FEATURE.                       NC2204.2
071100     MOVE 1 TO REC-CT.                                            NC2204.2
071200     MOVE ZEROS TO TABLE6.                                        NC2204.2
071300     MOVE ZEROS TO NUM-999.                                       NC2204.2
071400     SET INDEX5 TO 3.                                             NC2204.2
071500     SET INDEX6 TO 3.                                             NC2204.2
071600 DIV-TEST-F4-5.                                                   NC2204.2
071700     DIVIDE TABLE5-NUM (INDEX5 + 1) INTO TABLE5-NUM (INDEX5)      NC2204.2
071800        GIVING NUM-999 REMAINDER TABLE6-NUM (INDEX6).             NC2204.2
071900     GO TO DIV-TEST-F4-5-1.                                       NC2204.2
072000 DIV-DELETE-F4-5.                                                 NC2204.2
072100     PERFORM DE-LETE.                                             NC2204.2
072200     PERFORM PRINT-DETAIL.                                        NC2204.2
072300     GO TO DIV-TEST-F4-6.                                         NC2204.2
072400*                                                                 NC2204.2
072500 DIV-TEST-F4-5-1.                                                 NC2204.2
072600     MOVE "QUOTIENT" TO RE-MARK.                                  NC2204.2
072700     IF NUM-999 = 16                                              NC2204.2
072800           PERFORM PASS                                           NC2204.2
072900           GO TO DIV-WRITE-F4-5-1                                 NC2204.2
073000     ELSE                                                         NC2204.2
073100           GO TO DIV-FAIL-F4-5-1.                                 NC2204.2
073200 DIV-DELETE-F4-5-1.                                               NC2204.2
073300     PERFORM DE-LETE.                                             NC2204.2
073400     GO TO DIV-WRITE-F4-5-1.                                      NC2204.2
073500 DIV-FAIL-F4-5-1.                                                 NC2204.2
073600     PERFORM FAIL                                                 NC2204.2
073700     MOVE NUM-999 TO COMPUTED-18V0                                NC2204.2
073800     MOVE 16 TO CORRECT-18V0.                                     NC2204.2
073900 DIV-WRITE-F4-5-1.                                                NC2204.2
074000     PERFORM PRINT-DETAIL.                                        NC2204.2
074100     ADD 1 TO REC-CT.                                             NC2204.2
074200*                                                                 NC2204.2
074300 DIV-TEST-F4-5-2.                                                 NC2204.2
074400     MOVE "REMAINDER" TO RE-MARK.                                 NC2204.2
074500     IF TABLE6-NUM (INDEX6) = 21                                  NC2204.2
074600           PERFORM PASS                                           NC2204.2
074700           GO TO DIV-WRITE-F4-5-2                                 NC2204.2
074800     ELSE                                                         NC2204.2
074900           GO TO DIV-FAIL-F4-5-2.                                 NC2204.2
075000 DIV-DELETE-F4-5-2.                                               NC2204.2
075100     PERFORM DE-LETE.                                             NC2204.2
075200     GO TO DIV-WRITE-F4-5-2.                                      NC2204.2
075300 DIV-FAIL-F4-5-2.                                                 NC2204.2
075400     PERFORM FAIL                                                 NC2204.2
075500     MOVE TABLE6-NUM (INDEX6) TO COMPUTED-18V0                    NC2204.2
075600     MOVE 21 TO CORRECT-18V0.                                     NC2204.2
075700 DIV-WRITE-F4-5-2.                                                NC2204.2
075800     PERFORM PRINT-DETAIL.                                        NC2204.2
075900*                                                                 NC2204.2
076000 DIV-INIT-F4-6.                                                   NC2204.2
076100     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
076200     MOVE "DIV-TEST-F4-6" TO PAR-NAME.                            NC2204.2
076300     MOVE "DIVIDE INTO REMNDER" TO FEATURE.                       NC2204.2
076400     MOVE 1 TO REC-CT.                                            NC2204.2
076500     MOVE ZEROS TO TABLE6.                                        NC2204.2
076600     MOVE ZEROS TO NUM-999.                                       NC2204.2
076700     SET INDEX5 TO 5.                                             NC2204.2
076800     SET INDEX6 TO 5.                                             NC2204.2
076900 DIV-TEST-F4-6.                                                   NC2204.2
077000     DIVIDE TABLE5-NUM (INDEX5 + 1) INTO TABLE5-NUM (INDEX5)      NC2204.2
077100        GIVING TABLE6-NUM (INDEX6)                                NC2204.2
077200        REMAINDER TABLE6-NUM (INDEX6 + 1).                        NC2204.2
077300     GO TO DIV-TEST-F4-6-1.                                       NC2204.2
077400 DIV-DELETE-F4-6.                                                 NC2204.2
077500     PERFORM DE-LETE.                                             NC2204.2
077600     PERFORM PRINT-DETAIL.                                        NC2204.2
077700     GO TO DIV-TEST-F1-7.                                         NC2204.2
077800*                                                                 NC2204.2
077900 DIV-TEST-F4-6-1.                                                 NC2204.2
078000     MOVE "QUOTIENT" TO RE-MARK.                                  NC2204.2
078100     IF TABLE6-NUM (INDEX6) = 0                                   NC2204.2
078200           PERFORM PASS                                           NC2204.2
078300           GO TO DIV-WRITE-F4-6-1                                 NC2204.2
078400     ELSE                                                         NC2204.2
078500           GO TO DIV-FAIL-F4-6-1.                                 NC2204.2
078600 DIV-DELETE-F4-6-1.                                               NC2204.2
078700     PERFORM DE-LETE.                                             NC2204.2
078800     GO TO DIV-WRITE-F4-6-1.                                      NC2204.2
078900 DIV-FAIL-F4-6-1.                                                 NC2204.2
079000     PERFORM FAIL                                                 NC2204.2
079100     MOVE TABLE6-NUM (INDEX6) TO COMPUTED-18V0                    NC2204.2
079200     MOVE 0 TO CORRECT-18V0.                                      NC2204.2
079300 DIV-WRITE-F4-6-1.                                                NC2204.2
079400     PERFORM PRINT-DETAIL.                                        NC2204.2
079500     ADD 1 TO REC-CT.                                             NC2204.2
079600*                                                                 NC2204.2
079700 DIV-TEST-F4-6-2.                                                 NC2204.2
079800     MOVE "REMAINDER" TO RE-MARK.                                 NC2204.2
079900     IF TABLE6-NUM (INDEX6 + 1) = 82                              NC2204.2
080000           PERFORM PASS                                           NC2204.2
080100           GO TO DIV-WRITE-F4-6-2                                 NC2204.2
080200     ELSE                                                         NC2204.2
080300           GO TO DIV-FAIL-F4-6-2.                                 NC2204.2
080400 DIV-DELETE-F4-6-2.                                               NC2204.2
080500     PERFORM DE-LETE.                                             NC2204.2
080600     GO TO DIV-WRITE-F4-6-2.                                      NC2204.2
080700 DIV-FAIL-F4-6-2.                                                 NC2204.2
080800     PERFORM FAIL                                                 NC2204.2
080900     MOVE TABLE6-NUM (INDEX6 + 1) TO COMPUTED-18V0                NC2204.2
081000     MOVE 82 TO CORRECT-18V0.                                     NC2204.2
081100 DIV-WRITE-F4-6-2.                                                NC2204.2
081200     PERFORM PRINT-DETAIL.                                        NC2204.2
081300*                                                                 NC2204.2
081400 DIV-INIT-F1-7.                                                   NC2204.2
081500     MOVE "DIV-TEST-F1-7" TO PAR-NAME.                            NC2204.2
081600     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
081700     MOVE ZEROS TO REC-CT.                                        NC2204.2
081800     PERFORM BUILD-TABLE2.                                        NC2204.2
081900     PERFORM BUILD-TABLE3.                                        NC2204.2
082000     MOVE "DIVIDE INTO" TO FEATURE.                               NC2204.2
082100     SET INDEX2 TO 3.                                             NC2204.2
082200     SET INDEX3 TO 3.                                             NC2204.2
082300 DIV-TEST-F1-7.                                                   NC2204.2
082400     DIVIDE NUMBER2 OF TABLE2 (INDEX2)                            NC2204.2
082500        INTO NUMBER2 OF TABLE3 (INDEX3).                          NC2204.2
082600     IF NUMBER2 OF TABLE3 (INDEX3) = 2                            NC2204.2
082700        PERFORM PASS                                              NC2204.2
082800        ELSE GO TO DIV-FAIL-F1-7.                                 NC2204.2
082900     GO TO DIV-WRITE-F1-7.                                        NC2204.2
083000 DIV-DELETE-F1-7.                                                 NC2204.2
083100     PERFORM DE-LETE.                                             NC2204.2
083200     GO TO DIV-WRITE-F1-7.                                        NC2204.2
083300 DIV-FAIL-F1-7.                                                   NC2204.2
083400     PERFORM FAIL.                                                NC2204.2
083500     MOVE NUMBER2 OF TABLE3 (INDEX3) TO COMPUTED-18V0.            NC2204.2
083600     MOVE 2 TO CORRECT-18V0.                                      NC2204.2
083700 DIV-WRITE-F1-7.                                                  NC2204.2
083800     PERFORM PRINT-DETAIL.                                        NC2204.2
083900*                                                                 NC2204.2
084000 DIV-INIT-F1-8.                                                   NC2204.2
084100     MOVE "DIV-TEST-F1-8" TO PAR-NAME.                            NC2204.2
084200     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
084300     MOVE ZEROS TO REC-CT.                                        NC2204.2
084400     PERFORM BUILD-TABLE2.                                        NC2204.2
084500     PERFORM BUILD-TABLE3.                                        NC2204.2
084600     MOVE "DIVIDE INTO" TO FEATURE.                               NC2204.2
084700     SET INDEX2 TO 3.                                             NC2204.2
084800     SET INDEX3 TO 3.                                             NC2204.2
084900 DIV-TEST-F1-8.                                                   NC2204.2
085000     DIVIDE NUMBER2 OF TABLE2 (INDEX2 + 1)                        NC2204.2
085100        INTO NUMBER2 OF TABLE3 (INDEX3 + 1).                      NC2204.2
085200     IF NUMBER2 OF TABLE3 (INDEX3 + 1) = 4                        NC2204.2
085300        PERFORM PASS                                              NC2204.2
085400        ELSE GO TO DIV-FAIL-F1-8.                                 NC2204.2
085500     GO TO DIV-WRITE-F1-8.                                        NC2204.2
085600 DIV-DELETE-F1-8.                                                 NC2204.2
085700     PERFORM DE-LETE.                                             NC2204.2
085800     GO TO DIV-WRITE-F1-8.                                        NC2204.2
085900 DIV-FAIL-F1-8.                                                   NC2204.2
086000     PERFORM FAIL.                                                NC2204.2
086100     MOVE NUMBER2 OF TABLE3 (INDEX3 + 1) TO COMPUTED-18V0.        NC2204.2
086200     MOVE 4 TO CORRECT-18V0.                                      NC2204.2
086300 DIV-WRITE-F1-8.                                                  NC2204.2
086400     PERFORM PRINT-DETAIL.                                        NC2204.2
086500*                                                                 NC2204.2
086600 DIV-INIT-F5-9.                                                   NC2204.2
086700     MOVE "DIV-TEST-F5-9" TO PAR-NAME.                            NC2204.2
086800     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
086900     MOVE ZEROS TO REC-CT.                                        NC2204.2
087000     PERFORM BUILD-TABLE2.                                        NC2204.2
087100     PERFORM BUILD-TABLE3.                                        NC2204.2
087200     MOVE "DIVIDE BY GIVING" TO FEATURE.                          NC2204.2
087300     SET INDEX2 TO 1.                                             NC2204.2
087400     SET INDEX3 TO 2.                                             NC2204.2
087500 DIV-TEST-F5-9.                                                   NC2204.2
087600     DIVIDE NUMBER2 OF TABLE2 (INDEX2)                            NC2204.2
087700        BY NUMBER2 OF TABLE3 (INDEX3)                             NC2204.2
087800        GIVING NUMBER2 OF TABLE3 (INDEX3 + 1).                    NC2204.2
087900     IF NUMBER2 OF TABLE3 (INDEX3 + 1) = 1                        NC2204.2
088000        PERFORM PASS                                              NC2204.2
088100        ELSE GO TO DIV-FAIL-F5-9.                                 NC2204.2
088200     GO TO DIV-WRITE-F5-9.                                        NC2204.2
088300 DIV-DELETE-F5-9.                                                 NC2204.2
088400     PERFORM DE-LETE.                                             NC2204.2
088500     GO TO DIV-WRITE-F5-9.                                        NC2204.2
088600 DIV-FAIL-F5-9.                                                   NC2204.2
088700     PERFORM FAIL.                                                NC2204.2
088800     MOVE NUMBER2 OF TABLE3 (INDEX3 + 1) TO COMPUTED-18V0.        NC2204.2
088900     MOVE 1 TO CORRECT-18V0.                                      NC2204.2
089000 DIV-WRITE-F5-9.                                                  NC2204.2
089100     PERFORM PRINT-DETAIL.                                        NC2204.2
089200*                                                                 NC2204.2
089300 DIV-INIT-F5-10.                                                  NC2204.2
089400     MOVE "DIV-TEST-F5-10" TO PAR-NAME.                           NC2204.2
089500     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
089600     MOVE ZEROS TO REC-CT.                                        NC2204.2
089700     PERFORM BUILD-TABLE2.                                        NC2204.2
089800     PERFORM BUILD-TABLE3.                                        NC2204.2
089900     MOVE "DIVIDE BY GIVING" TO FEATURE.                          NC2204.2
090000     SET INDEX2 TO 2.                                             NC2204.2
090100     SET INDEX3 TO 3.                                             NC2204.2
090200 DIV-TEST-F5-10.                                                  NC2204.2
090300     DIVIDE NUMBER2 OF TABLE3 (INDEX3 + 1)                        NC2204.2
090400        BY NUMBER2 OF TABLE2 (INDEX2 + 2)                         NC2204.2
090500        GIVING NUMBER2 OF TABLE2 (INDEX2).                        NC2204.2
090600     IF NUMBER2 OF TABLE2 (INDEX2) = 4                            NC2204.2
090700        PERFORM PASS                                              NC2204.2
090800        ELSE GO TO DIV-FAIL-F5-10.                                NC2204.2
090900     GO TO DIV-WRITE-F5-10.                                       NC2204.2
091000 DIV-DELETE-F5-10.                                                NC2204.2
091100     PERFORM DE-LETE.                                             NC2204.2
091200     GO TO DIV-WRITE-F5-10.                                       NC2204.2
091300 DIV-FAIL-F5-10.                                                  NC2204.2
091400     PERFORM FAIL.                                                NC2204.2
091500     MOVE NUMBER2 OF TABLE2 (INDEX2) TO COMPUTED-18V0.            NC2204.2
091600     MOVE 4 TO CORRECT-18V0.                                      NC2204.2
091700 DIV-WRITE-F5-10.                                                 NC2204.2
091800     PERFORM PRINT-DETAIL.                                        NC2204.2
091900*                                                                 NC2204.2
092000 PFM-INIT-F3-1.                                                   NC2204.2
092100     MOVE "PFM-TEST-F3-1" TO PAR-NAME.                            NC2204.2
092200     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
092300     MOVE ZEROS TO REC-CT.                                        NC2204.2
092400     MOVE "PERFORM UNTIL" TO FEATURE.                             NC2204.2
092500     PERFORM BUILD-TABLE7.                                        NC2204.2
092600     PERFORM BUILD-TABLE8.                                        NC2204.2
092700     SET INDEX7 TO 1.                                             NC2204.2
092800     SET INDEX8 TO 1.                                             NC2204.2
092900 PFM-TEST-F3-1.                                                   NC2204.2
093000     PERFORM PARAGRAPH-A UNTIL TABLE7-NUM (INDEX7)                NC2204.2
093100        IS EQUAL TO TABLE8-NUM (INDEX8).                          NC2204.2
093200     IF TABLE7-NUM (INDEX7) = 4                                   NC2204.2
093300        PERFORM PASS                                              NC2204.2
093400        ELSE GO TO PFM-FAIL-F3-1.                                 NC2204.2
093500     GO TO PFM-WRITE-F3-1.                                        NC2204.2
093600 PFM-DELETE-F3-1.                                                 NC2204.2
093700     PERFORM DE-LETE.                                             NC2204.2
093800     GO TO PFM-WRITE-F3-1.                                        NC2204.2
093900 PFM-FAIL-F3-1.                                                   NC2204.2
094000     PERFORM FAIL.                                                NC2204.2
094100     MOVE TABLE7-NUM (INDEX7) TO COMPUTED-18V0.                   NC2204.2
094200     MOVE 4 TO CORRECT-18V0.                                      NC2204.2
094300 PFM-WRITE-F3-1.                                                  NC2204.2
094400     PERFORM PRINT-DETAIL.                                        NC2204.2
094500*                                                                 NC2204.2
094600 PFM-INIT-F3-2.                                                   NC2204.2
094700     MOVE "PFM-TEST-F3-2" TO PAR-NAME.                            NC2204.2
094800     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
094900     MOVE ZEROS TO REC-CT.                                        NC2204.2
095000     MOVE "PERFORM UNTIL" TO FEATURE.                             NC2204.2
095100     PERFORM BUILD-TABLE7.                                        NC2204.2
095200     PERFORM BUILD-TABLE8.                                        NC2204.2
095300     SET INDEX7 TO 1.                                             NC2204.2
095400     SET INDEX8 TO 1.                                             NC2204.2
095500 PFM-TEST-F3-2.                                                   NC2204.2
095600     PERFORM PARAGRAPH-A UNTIL TABLE7-NUM (INDEX7)                NC2204.2
095700        IS GREATER THAN TABLE8-NUM (INDEX8).                      NC2204.2
095800     IF TABLE7-NUM (INDEX7) = 5                                   NC2204.2
095900        PERFORM PASS                                              NC2204.2
096000        ELSE GO TO PFM-FAIL-F3-2.                                 NC2204.2
096100     GO TO PFM-WRITE-F3-2.                                        NC2204.2
096200 PFM-DELETE-F3-2.                                                 NC2204.2
096300     PERFORM DE-LETE.                                             NC2204.2
096400     GO TO PFM-WRITE-F3-2.                                        NC2204.2
096500 PFM-FAIL-F3-2.                                                   NC2204.2
096600     PERFORM FAIL.                                                NC2204.2
096700     MOVE TABLE7-NUM (INDEX7) TO COMPUTED-18V0.                   NC2204.2
096800     MOVE 5 TO CORRECT-18V0.                                      NC2204.2
096900 PFM-WRITE-F3-2.                                                  NC2204.2
097000     PERFORM PRINT-DETAIL.                                        NC2204.2
097100*                                                                 NC2204.2
097200 PFM-INIT-F4-3.                                                   NC2204.2
097300     MOVE "PFM-TEST-F4-3" TO PAR-NAME.                            NC2204.2
097400     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
097500     MOVE ZEROS TO REC-CT.                                        NC2204.2
097600     MOVE "PERFORM VARYING" TO FEATURE.                           NC2204.2
097700     MOVE ZEROS TO NUM-9.                                         NC2204.2
097800     PERFORM BUILD-TABLE7.                                        NC2204.2
097900     PERFORM BUILD-TABLE8.                                        NC2204.2
098000     SET INDEX7 TO 2.                                             NC2204.2
098100     SET INDEX8 TO 2.                                             NC2204.2
098200 PFM-TEST-F4-3.                                                   NC2204.2
098300     PERFORM PARAGRAPH-B VARYING NUM-9                            NC2204.2
098400        FROM 1 BY 1                                               NC2204.2
098500        UNTIL NUM-9 > TABLE8-NUM (INDEX8).                        NC2204.2
098600     IF NUM-9 = 8                                                 NC2204.2
098700        PERFORM PASS                                              NC2204.2
098800        ELSE GO TO PFM-FAIL-F4-3.                                 NC2204.2
098900     GO TO PFM-WRITE-F4-3.                                        NC2204.2
099000 PFM-DELETE-F4-3.                                                 NC2204.2
099100     PERFORM DE-LETE.                                             NC2204.2
099200     GO TO PFM-WRITE-F4-3.                                        NC2204.2
099300 PFM-FAIL-F4-3.                                                   NC2204.2
099400     PERFORM FAIL.                                                NC2204.2
099500     MOVE NUM-9 TO COMPUTED-18V0.                                 NC2204.2
099600     MOVE 8 TO CORRECT-18V0.                                      NC2204.2
099700 PFM-WRITE-F4-3.                                                  NC2204.2
099800     PERFORM PRINT-DETAIL.                                        NC2204.2
099900*                                                                 NC2204.2
100000 PFM-INIT-F4-4.                                                   NC2204.2
100100     MOVE "PFM-TEST-F4-4" TO PAR-NAME.                            NC2204.2
100200     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC2204.2
100300     MOVE ZEROS TO REC-CT.                                        NC2204.2
100400     MOVE "PERFORM VARYING" TO FEATURE.                           NC2204.2
100500     MOVE ZEROS TO NUM-9.                                         NC2204.2
100600     PERFORM BUILD-TABLE7.                                        NC2204.2
100700     PERFORM BUILD-TABLE8.                                        NC2204.2
100800     SET INDEX7 TO 2.                                             NC2204.2
100900     SET INDEX8 TO 3.                                             NC2204.2
101000 PFM-TEST-F4-4.                                                   NC2204.2
101100     PERFORM PARAGRAPH-B VARYING NUM-9                            NC2204.2
101200        FROM 7 BY -1                                              NC2204.2
101300        UNTIL NUM-9 < TABLE8-NUM (INDEX8).                        NC2204.2
101400     IF NUM-9 = 1                                                 NC2204.2
101500        PERFORM PASS                                              NC2204.2
101600        ELSE GO TO PFM-FAIL-F4-4.                                 NC2204.2
101700     GO TO PFM-WRITE-F4-4.                                        NC2204.2
101800 PFM-DELETE-F4-4.                                                 NC2204.2
101900     PERFORM DE-LETE.                                             NC2204.2
102000     GO TO PFM-WRITE-F4-4.                                        NC2204.2
102100 PFM-FAIL-F4-4.                                                   NC2204.2
102200     PERFORM FAIL.                                                NC2204.2
102300     MOVE NUM-9 TO COMPUTED-18V0.                                 NC2204.2
102400     MOVE 1 TO CORRECT-18V0.                                      NC2204.2
102500 PFM-WRITE-F4-4.                                                  NC2204.2
102600     PERFORM PRINT-DETAIL.                                        NC2204.2
102700     GO TO CCVS-999999.                                           NC2204.2
102800*                                                                 NC2204.2
102900 PARAGRAPH-A.                                                     NC2204.2
103000     ADD 1 TO TABLE7-NUM (INDEX7).                                NC2204.2
103100*                                                                 NC2204.2
103200 PARAGRAPH-B.                                                     NC2204.2
103300     MOVE NUM-9 TO TABLE7-NUM (INDEX7).                           NC2204.2
103400*                                                                 NC2204.2
103500 CCVS-EXIT SECTION.                                               NC2204.2
103600 CCVS-999999.                                                     NC2204.2
103700     GO TO CLOSE-FILES.                                           NC2204.2
      *END-OF,NC220M                                                                  
