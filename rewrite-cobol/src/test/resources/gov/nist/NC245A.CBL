      *HEADER,COBOL,NC245A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2454.2
000200 PROGRAM-ID.                                                      NC2454.2
000300     NC245A.                                                      NC2454.2
000400****************************************************************  NC2454.2
000500*                                                              *  NC2454.2
000600*    VALIDATION FOR:-                                          *  NC2454.2
000700*                                                              *  NC2454.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2454.2
000900*                                                              *  NC2454.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2454.2
001100*                                                              *  NC2454.2
001200****************************************************************  NC2454.2
001300*                                                              *  NC2454.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2454.2
001500*                                                              *  NC2454.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2454.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2454.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2454.2
001900*                                                              *  NC2454.2
002000****************************************************************  NC2454.2
002100*                                                              *  NC2454.2
002200*    PROGRAM NC245A TESTS THE USE OF THE COMMA, SEMI-COLON AND *  NC2454.2
002300*    SPACE SEPARATORS WHEN SPECIFYING SUBSCRIPTS AND INDICES   *  NC2454.2
002400*    TO ACCESS TWO AND THREE-DIMENSIONAL TABLES                *  NC2454.2
002500*    RELATIVE INDEXING IS ALSO USED.                           *  NC2454.2
002600*                                                              *  NC2454.2
002700****************************************************************  NC2454.2
002800 ENVIRONMENT DIVISION.                                            NC2454.2
002900 CONFIGURATION SECTION.                                           NC2454.2
003000 SOURCE-COMPUTER.                                                 NC2454.2
003100     XXXXX082.                                                    NC2454.2
003200 OBJECT-COMPUTER.                                                 NC2454.2
003300     XXXXX083.                                                    NC2454.2
003400 INPUT-OUTPUT SECTION.                                            NC2454.2
003500 FILE-CONTROL.                                                    NC2454.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2454.2
003700     XXXXX055.                                                    NC2454.2
003800 DATA DIVISION.                                                   NC2454.2
003900 FILE SECTION.                                                    NC2454.2
004000 FD  PRINT-FILE.                                                  NC2454.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2454.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2454.2
004300 WORKING-STORAGE SECTION.                                         NC2454.2
004400 77  WRK1 PIC S999; COMPUTATIONAL, VALUE ZERO.                    NC2454.2
004500 77  EXPECTED-VALUE,   PIC    S999999.                            NC2454.2
004600 77  TEMP; PIC S999999.                                           NC2454.2
004700*    TWO DIMENSIONAL TABLE; 15 X 10.                              NC2454.2
004800 01  GRP-TAB2.                                                    NC2454.2
004900     02  GRP-LEV2-0015F; OCCURS 15 TIMES;                         NC2454.2
005000         INDEXED BY IN1,  INDEX1,                                 NC2454.2
005100         USAGE IS COMPUTATIONAL.                                  NC2454.2
005200         03  ELEM2 PIC S999999; OCCURS 10 TIMES;                  NC2454.2
005300                INDEXED BY IN2; INDEX2.                           NC2454.2
005400*    THREE DIMENSIONAL TABLE; 10 X 5 X 3.                         NC2454.2
005500 01  GRP-TAB3.                                                    NC2454.2
005600     02  GRP-LEV2-0003F; OCCURS 3 TIMES;                          NC2454.2
005700         INDEXED BY INAME1, IN-NAME-1,                            NC2454.2
005800         USAGE IS COMPUTATIONAL.                                  NC2454.2
005900         03  GRP-LEV3-0005F; OCCURS 5 TIMES;                      NC2454.2
006000             INDEXED BY INAME2; IN-NAME-2.                        NC2454.2
006100             04  ELEM3 PIC S999999; OCCURS 10 TIMES;              NC2454.2
006200                 INDEXED BY INAME3; IN-NAME-3.                    NC2454.2
006300*    SUBSCRIPTS FOR REFERENCING TABLE ITEMS                       NC2454.2
006400 01  SUBSCRIPT-TABLE; USAGE COMPUTATIONAL.                        NC2454.2
006500     02  S21  PIC S999; VALUE IS 1.                               NC2454.2
006600     02  S22  PIC S999; VALUE IS 1.                               NC2454.2
006700     02  S31  PIC S999; VALUE IS 1.                               NC2454.2
006800     02  S32  PIC S999; VALUE IS 1.                               NC2454.2
006900     02  S33  PIC S999; VALUE IS 1.                               NC2454.2
007000 01  TEST-RESULTS.                                                NC2454.2
007100     02 FILLER                   PIC X      VALUE SPACE.          NC2454.2
007200     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2454.2
007300     02 FILLER                   PIC X      VALUE SPACE.          NC2454.2
007400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2454.2
007500     02 FILLER                   PIC X      VALUE SPACE.          NC2454.2
007600     02  PAR-NAME.                                                NC2454.2
007700       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2454.2
007800       03  PARDOT-X              PIC X      VALUE SPACE.          NC2454.2
007900       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2454.2
008000     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2454.2
008100     02 RE-MARK                  PIC X(61).                       NC2454.2
008200 01  TEST-COMPUTED.                                               NC2454.2
008300     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2454.2
008400     02 FILLER                   PIC X(17)  VALUE                 NC2454.2
008500            "       COMPUTED=".                                   NC2454.2
008600     02 COMPUTED-X.                                               NC2454.2
008700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2454.2
008800     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2454.2
008900                                 PIC -9(9).9(9).                  NC2454.2
009000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2454.2
009100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2454.2
009200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2454.2
009300     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2454.2
009400         04 COMPUTED-18V0                    PIC -9(18).          NC2454.2
009500         04 FILLER                           PIC X.               NC2454.2
009600     03 FILLER PIC X(50) VALUE SPACE.                             NC2454.2
009700 01  TEST-CORRECT.                                                NC2454.2
009800     02 FILLER PIC X(30) VALUE SPACE.                             NC2454.2
009900     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2454.2
010000     02 CORRECT-X.                                                NC2454.2
010100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2454.2
010200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2454.2
010300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2454.2
010400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2454.2
010500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2454.2
010600     03      CR-18V0 REDEFINES CORRECT-A.                         NC2454.2
010700         04 CORRECT-18V0                     PIC -9(18).          NC2454.2
010800         04 FILLER                           PIC X.               NC2454.2
010900     03 FILLER PIC X(2) VALUE SPACE.                              NC2454.2
011000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2454.2
011100 01  CCVS-C-1.                                                    NC2454.2
011200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2454.2
011300-    "SS  PARAGRAPH-NAME                                          NC2454.2
011400-    "       REMARKS".                                            NC2454.2
011500     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2454.2
011600 01  CCVS-C-2.                                                    NC2454.2
011700     02 FILLER                     PIC X        VALUE SPACE.      NC2454.2
011800     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2454.2
011900     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2454.2
012000     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2454.2
012100     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2454.2
012200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2454.2
012300 01  REC-CT                        PIC 99       VALUE ZERO.       NC2454.2
012400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2454.2
012500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2454.2
012600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2454.2
012700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2454.2
012800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2454.2
012900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2454.2
013000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2454.2
013100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2454.2
013200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2454.2
013300 01  CCVS-H-1.                                                    NC2454.2
013400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2454.2
013500     02  FILLER                    PIC X(42)    VALUE             NC2454.2
013600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2454.2
013700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2454.2
013800 01  CCVS-H-2A.                                                   NC2454.2
013900   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2454.2
014000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2454.2
014100   02  FILLER                        PIC XXXX   VALUE             NC2454.2
014200     "4.2 ".                                                      NC2454.2
014300   02  FILLER                        PIC X(28)  VALUE             NC2454.2
014400            " COPY - NOT FOR DISTRIBUTION".                       NC2454.2
014500   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2454.2
014600                                                                  NC2454.2
014700 01  CCVS-H-2B.                                                   NC2454.2
014800   02  FILLER                        PIC X(15)  VALUE             NC2454.2
014900            "TEST RESULT OF ".                                    NC2454.2
015000   02  TEST-ID                       PIC X(9).                    NC2454.2
015100   02  FILLER                        PIC X(4)   VALUE             NC2454.2
015200            " IN ".                                               NC2454.2
015300   02  FILLER                        PIC X(12)  VALUE             NC2454.2
015400     " HIGH       ".                                              NC2454.2
015500   02  FILLER                        PIC X(22)  VALUE             NC2454.2
015600            " LEVEL VALIDATION FOR ".                             NC2454.2
015700   02  FILLER                        PIC X(58)  VALUE             NC2454.2
015800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2454.2
015900 01  CCVS-H-3.                                                    NC2454.2
016000     02  FILLER                      PIC X(34)  VALUE             NC2454.2
016100            " FOR OFFICIAL USE ONLY    ".                         NC2454.2
016200     02  FILLER                      PIC X(58)  VALUE             NC2454.2
016300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2454.2
016400     02  FILLER                      PIC X(28)  VALUE             NC2454.2
016500            "  COPYRIGHT   1985 ".                                NC2454.2
016600 01  CCVS-E-1.                                                    NC2454.2
016700     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2454.2
016800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2454.2
016900     02 ID-AGAIN                     PIC X(9).                    NC2454.2
017000     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2454.2
017100 01  CCVS-E-2.                                                    NC2454.2
017200     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2454.2
017300     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2454.2
017400     02 CCVS-E-2-2.                                               NC2454.2
017500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2454.2
017600         03 FILLER                   PIC X      VALUE SPACE.      NC2454.2
017700         03 ENDER-DESC               PIC X(44)  VALUE             NC2454.2
017800            "ERRORS ENCOUNTERED".                                 NC2454.2
017900 01  CCVS-E-3.                                                    NC2454.2
018000     02  FILLER                      PIC X(22)  VALUE             NC2454.2
018100            " FOR OFFICIAL USE ONLY".                             NC2454.2
018200     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2454.2
018300     02  FILLER                      PIC X(58)  VALUE             NC2454.2
018400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2454.2
018500     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2454.2
018600     02 FILLER                       PIC X(15)  VALUE             NC2454.2
018700             " COPYRIGHT 1985".                                   NC2454.2
018800 01  CCVS-E-4.                                                    NC2454.2
018900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2454.2
019000     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2454.2
019100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2454.2
019200     02 FILLER                       PIC X(40)  VALUE             NC2454.2
019300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2454.2
019400 01  XXINFO.                                                      NC2454.2
019500     02 FILLER                       PIC X(19)  VALUE             NC2454.2
019600            "*** INFORMATION ***".                                NC2454.2
019700     02 INFO-TEXT.                                                NC2454.2
019800       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2454.2
019900       04 XXCOMPUTED                 PIC X(20).                   NC2454.2
020000       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2454.2
020100       04 XXCORRECT                  PIC X(20).                   NC2454.2
020200     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2454.2
020300 01  HYPHEN-LINE.                                                 NC2454.2
020400     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2454.2
020500     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2454.2
020600-    "*****************************************".                 NC2454.2
020700     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2454.2
020800-    "******************************".                            NC2454.2
020900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2454.2
021000     "NC245A".                                                    NC2454.2
021100 PROCEDURE DIVISION.                                              NC2454.2
021200 CCVS1 SECTION.                                                   NC2454.2
021300 OPEN-FILES.                                                      NC2454.2
021400     OPEN     OUTPUT PRINT-FILE.                                  NC2454.2
021500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2454.2
021600     MOVE    SPACE TO TEST-RESULTS.                               NC2454.2
021700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2454.2
021800     GO TO CCVS1-EXIT.                                            NC2454.2
021900 CLOSE-FILES.                                                     NC2454.2
022000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2454.2
022100 TERMINATE-CCVS.                                                  NC2454.2
022200*    EXIT PROGRAM.                                                NC2454.2
022300*TERMINATE-CALL.                                                  NC2454.2
022400     STOP     RUN.                                                NC2454.2
022500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2454.2
022600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2454.2
022700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2454.2
022800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2454.2
022900     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2454.2
023000 PRINT-DETAIL.                                                    NC2454.2
023100     IF REC-CT NOT EQUAL TO ZERO                                  NC2454.2
023200             MOVE "." TO PARDOT-X                                 NC2454.2
023300             MOVE REC-CT TO DOTVALUE.                             NC2454.2
023400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2454.2
023500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2454.2
023600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2454.2
023700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2454.2
023800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2454.2
023900     MOVE SPACE TO CORRECT-X.                                     NC2454.2
024000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2454.2
024100     MOVE     SPACE TO RE-MARK.                                   NC2454.2
024200 HEAD-ROUTINE.                                                    NC2454.2
024300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2454.2
024400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2454.2
024500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2454.2
024600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2454.2
024700 COLUMN-NAMES-ROUTINE.                                            NC2454.2
024800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2454.2
024900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2454.2
025000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2454.2
025100 END-ROUTINE.                                                     NC2454.2
025200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2454.2
025300 END-RTN-EXIT.                                                    NC2454.2
025400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2454.2
025500 END-ROUTINE-1.                                                   NC2454.2
025600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2454.2
025700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2454.2
025800      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2454.2
025900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2454.2
026000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2454.2
026100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2454.2
026200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2454.2
026300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2454.2
026400  END-ROUTINE-12.                                                 NC2454.2
026500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2454.2
026600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2454.2
026700         MOVE "NO " TO ERROR-TOTAL                                NC2454.2
026800         ELSE                                                     NC2454.2
026900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2454.2
027000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2454.2
027100     PERFORM WRITE-LINE.                                          NC2454.2
027200 END-ROUTINE-13.                                                  NC2454.2
027300     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2454.2
027400         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2454.2
027500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2454.2
027600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2454.2
027700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2454.2
027800      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2454.2
027900          MOVE "NO " TO ERROR-TOTAL                               NC2454.2
028000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2454.2
028100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2454.2
028200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2454.2
028300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2454.2
028400 WRITE-LINE.                                                      NC2454.2
028500     ADD 1 TO RECORD-COUNT.                                       NC2454.2
028600*    IF RECORD-COUNT GREATER 50                                   NC2454.2
028700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2454.2
028800*        MOVE SPACE TO DUMMY-RECORD                               NC2454.2
028900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2454.2
029000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2454.2
029100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2454.2
029200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2454.2
029300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2454.2
029400*        MOVE ZERO TO RECORD-COUNT.                               NC2454.2
029500     PERFORM WRT-LN.                                              NC2454.2
029600 WRT-LN.                                                          NC2454.2
029700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2454.2
029800     MOVE SPACE TO DUMMY-RECORD.                                  NC2454.2
029900 BLANK-LINE-PRINT.                                                NC2454.2
030000     PERFORM WRT-LN.                                              NC2454.2
030100 FAIL-ROUTINE.                                                    NC2454.2
030200     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2454.2
030300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2454.2
030400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2454.2
030500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2454.2
030600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2454.2
030700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2454.2
030800     GO TO  FAIL-ROUTINE-EX.                                      NC2454.2
030900 FAIL-ROUTINE-WRITE.                                              NC2454.2
031000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2454.2
031100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2454.2
031200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2454.2
031300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2454.2
031400 FAIL-ROUTINE-EX. EXIT.                                           NC2454.2
031500 BAIL-OUT.                                                        NC2454.2
031600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2454.2
031700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2454.2
031800 BAIL-OUT-WRITE.                                                  NC2454.2
031900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2454.2
032000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2454.2
032100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2454.2
032200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2454.2
032300 BAIL-OUT-EX. EXIT.                                               NC2454.2
032400 CCVS1-EXIT.                                                      NC2454.2
032500     EXIT.                                                        NC2454.2
032600*        SECTION 2.1.6 ON PAGE IV-3 OF AMERICAN NATIONAL          NC2454.2
032700*    STANDARD COBOL, X3.23 - 1985 STATES THAT COMMA AND           NC2454.2
032800*    SEMICOLON ARE OPTIONAL WHERE SHOWN IN THE FORMATS AND        NC2454.2
032900*    ARE INTERCHANGEABLE.  EITHER ONE MAY BE USED ANYWHERE        NC2454.2
033000*    ONE OF THEM IS SHOWN IN THE LANGUAGE FORMATS.                NC2454.2
033100*                                                                 NC2454.2
033200*        THIS ROUTINE TESTS THE USE OF SEMICOLON IN PLACE OF      NC2454.2
033300*    COMMA AS SEPARATORS FOR SUBSCRIPTS AND INDEXES IN            NC2454.2
033400*    REFERENCING TABLE ITEMS.                                     NC2454.2
033500****************************************                          NC2454.2
033600*STATEMENT DELETION INSTRUCTIONS                                  NC2454.2
033700*        IF THE COMPILER REJECTS ANY TABLE REFERENCE IN THESE     NC2454.2
033800*    TESTS, DELETE THAT LINE OF CODE BY PLACING AN * IN COLUMN 7. NC2454.2
033900*    LEAVE THE PERFORM ... THRU STATEMENT.  THE TEST DELETED      NC2454.2
034000*    APPEARS AS A FAILURE ON THE OUTPUT REPORT.                   NC2454.2
034100****************************************                          NC2454.2
034200 SECT-NC245A-001 SECTION.                                         NC2454.2
034300*        THIS SECTION STORES THE VALUES 1 THRU 150 IN THE         NC2454.2
034400*    TWO TABLES USED IN THE TESTS OF SEMICOLON AS SUBSCRIPT       NC2454.2
034500*    AND INDEX SEPARATOR.                                         NC2454.2
034600 BUILD-TABLE.                                                     NC2454.2
034700     ADD 1 TO WRK1.                                               NC2454.2
034800     MOVE WRK1 TO ELEM2 (S21, S22)                                NC2454.2
034900                  ELEM3 (S31, S32, S33).                          NC2454.2
035000     IF WRK1 EQUAL TO 150 GO TO SECT-TH219-0002.                  NC2454.2
035100 INCRE-SUBS.                                                      NC2454.2
035200     ADD 1 TO S22, S33.                                           NC2454.2
035300     IF S22 LESS THAN 11 GO TO BUILD-TABLE.                       NC2454.2
035400     MOVE 1 TO S22, S33.                                          NC2454.2
035500     ADD 1 TO S21, S32.                                           NC2454.2
035600     IF S32 LESS THAN 6 GO TO BUILD-TABLE.                        NC2454.2
035700     MOVE 1 TO S32.                                               NC2454.2
035800     ADD 1 TO S31.                                                NC2454.2
035900     GO TO BUILD-TABLE.                                           NC2454.2
036000 SECT-TH219-0002 SECTION.                                         NC2454.2
036100*        THIS SECTION CONTAINS THE TESTS ON THE USE OF SEMICOLON  NC2454.2
036200*    AS A SEPARATOR IN REFERENCING TABLE ITEMS.                   NC2454.2
036300 SEP-INIT-008.                                                    NC2454.2
036400     MOVE "SEP-TEST-008" TO PAR-NAME.                             NC2454.2
036500     MOVE "SEMICLN AS SEPARATOR" TO FEATURE.                      NC2454.2
036600     MOVE 0 TO REC-CT.                                            NC2454.2
036700     MOVE 0 TO TEMP.                                              NC2454.2
036800     MOVE 6 TO EXPECTED-VALUE.                                    NC2454.2
036900     MOVE 1 TO S21.                                               NC2454.2
037000     MOVE 6 TO S22.                                               NC2454.2
037100*        THIS TEST USES SPACES AND SEMICOLONS IN REFERENCING      NC2454.2
037200*    TWO DIMENSIONAL TABLE ELEMENTS WITH SUBSCRIPTS.              NC2454.2
037300 SEP-TEST-008-01.                                                 NC2454.2
037400     MOVE ELEM2 (S21; S22) TO TEMP.                               NC2454.2
037500     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
037600 SEP-TEST-008-02.                                                 NC2454.2
037700     MOVE ELEM2(S21; S22) TO TEMP.                                NC2454.2
037800     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
037900 SEP-TEST-008-03.                                                 NC2454.2
038000     ADD ELEM2 (S21 ; S22) TO TEMP.                               NC2454.2
038100     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
038200 SEP-TEST-008-04.                                                 NC2454.2
038300     MOVE ELEM2( S21; S22 ) TO TEMP.                              NC2454.2
038400     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
038500 SEP-TEST-008-05.                                                 NC2454.2
038600     MOVE ELEM2 ( S21;  S22  ) TO TEMP.                           NC2454.2
038700     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
038800     GO TO SEP-INIT-009.                                          NC2454.2
038900 SEP-DELETE-008.                                                  NC2454.2
039000     PERFORM DE-LETE.                                             NC2454.2
039100     PERFORM TEST-WRITE.                                          NC2454.2
039200 SEP-INIT-009.                                                    NC2454.2
039300     MOVE "SEP-TEST-009" TO PAR-NAME.                             NC2454.2
039400     MOVE 0 TO REC-CT.                                            NC2454.2
039500     MOVE 0 TO TEMP.                                              NC2454.2
039600     MOVE 150 TO EXPECTED-VALUE.                                  NC2454.2
039700     MOVE 3 TO S31.                                               NC2454.2
039800     MOVE 5 TO S32.                                               NC2454.2
039900     MOVE 10 TO S33.                                              NC2454.2
040000*        THIS TEST USES SEMICOLONS, COMMAS, AND SPACES IN         NC2454.2
040100*    REFERENCING THREE DIMENSIONAL TABLE ELEMENTS WITH SUBSCRIPTS.NC2454.2
040200 SEP-TEST-009-01.                                                 NC2454.2
040300     MOVE ELEM3 (S31; S32; S33) TO TEMP.                          NC2454.2
040400     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
040500 SEP-TEST-009-02.                                                 NC2454.2
040600     MOVE ELEM3(S31; S32; S33) TO TEMP.                           NC2454.2
040700     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
040800 SEP-TEST-009-03.                                                 NC2454.2
040900     ADD ELEM3 (S31, S32; S33) TO TEMP.                           NC2454.2
041000     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
041100 SEP-TEST-009-04.                                                 NC2454.2
041200     MOVE     300 TO TEMP.                                        NC2454.2
041300     SUBTRACT ELEM3 (S31; S32 S33) FROM TEMP.                     NC2454.2
041400     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
041500 SEP-TEST-009-05.                                                 NC2454.2
041600     MOVE ELEM3 (S31 ; S32 ; S33) TO TEMP.                        NC2454.2
041700     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
041800 SEP-TEST-009-06.                                                 NC2454.2
041900     MOVE ELEM3( S31   S32; S33) TO TEMP.                         NC2454.2
042000     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
042100     GO TO SEP-INIT-010.                                          NC2454.2
042200 SEP-DELETE-009.                                                  NC2454.2
042300     PERFORM DE-LETE.                                             NC2454.2
042400     PERFORM TEST-WRITE.                                          NC2454.2
042500*                                                                 NC2454.2
042600 SEP-INIT-010.                                                    NC2454.2
042700     MOVE "SEP-TEST-010" TO PAR-NAME.                             NC2454.2
042800     MOVE 0 TO REC-CT.                                            NC2454.2
042900     MOVE 0 TO TEMP.                                              NC2454.2
043000     MOVE 150 TO EXPECTED-VALUE.                                  NC2454.2
043100*        THIS TEST USES SEMICOLONS, SPACES AND COMMAS IN          NC2454.2
043200*    REFERENCING TABLE ELEMENTS WITH LITERAL SUBSCRIPTS.          NC2454.2
043300 SEP-TEST-010-01.                                                 NC2454.2
043400     MOVE ELEM2 (15; 10) TO TEMP.                                 NC2454.2
043500     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
043600 SEP-TEST-010-02.                                                 NC2454.2
043700     MOVE ELEM2 ( 15; 10 ) TO TEMP.                               NC2454.2
043800     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
043900 SEP-TEST-010-03.                                                 NC2454.2
044000     ADD ELEM2(15; 10) TO TEMP.                                   NC2454.2
044100     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
044200 SEP-TEST-010-04.                                                 NC2454.2
044300     MOVE ELEM2 (+15; 10) TO TEMP.                                NC2454.2
044400     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
044500 SEP-TEST-010-05.                                                 NC2454.2
044600     ADD ELEM3 (3; 5; 10) TO TEMP.                                NC2454.2
044700     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
044800 SEP-TEST-010-06.                                                 NC2454.2
044900     MOVE ELEM3( +3; +5, +10) TO TEMP.                            NC2454.2
045000     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
045100 SEP-TEST-010-07.                                                 NC2454.2
045200     MOVE ELEM3 (+3, 5; 10) TO TEMP.                              NC2454.2
045300     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
045400     GO TO SEP-INIT-011.                                          NC2454.2
045500 SEP-DELETE-010.                                                  NC2454.2
045600     PERFORM DE-LETE.                                             NC2454.2
045700     PERFORM TEST-WRITE.                                          NC2454.2
045800*                                                                 NC2454.2
045900 SEP-INIT-011.                                                    NC2454.2
046000     MOVE "SEP-TEST-011" TO PAR-NAME.                             NC2454.2
046100     MOVE 0 TO TEMP; REC-CT.                                      NC2454.2
046200     MOVE 135 TO EXPECTED-VALUE.                                  NC2454.2
046300*        THIS TEST USES SEMICOLON, COMMA AND SPACE IN             NC2454.2
046400*    REFERENCING 2 AND 3-DIM. TABLES WITH INDEXING.               NC2454.2
046500 SEP-TEST-011-01.                                                 NC2454.2
046600     SET IN1 TO 14.                                               NC2454.2
046700     SET IN2 TO  5.                                               NC2454.2
046800     MOVE ELEM2 (IN1; IN2) TO TEMP.                               NC2454.2
046900     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
047000 SEP-TEST-011-02.                                                 NC2454.2
047100     SET INAME1 TO 3.                                             NC2454.2
047200     SET INAME2 TO 4.                                             NC2454.2
047300     SET INAME3 TO 5.                                             NC2454.2
047400     MOVE ELEM3 (INAME1; INAME2; INAME3) TO TEMP.                 NC2454.2
047500     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
047600 SEP-TEST-011-03.                                                 NC2454.2
047700     MOVE ELEM3 (INAME1, INAME2; INAME3) TO TEMP.                 NC2454.2
047800     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
047900 SEP-TEST-011-04.                                                 NC2454.2
048000     MOVE ELEM3 (INAME1; INAME2 INAME3) TO TEMP.                  NC2454.2
048100     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
048200 SEP-TEST-011-05.                                                 NC2454.2
048300     MOVE ELEM3 (3; INAME2; 5) TO TEMP.                           NC2454.2
048400     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
048500 SEP-TEST-011-06.                                                 NC2454.2
048600     MOVE ELEM3 (3, INAME2; 5) TO TEMP.                           NC2454.2
048700     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
048800     GO TO SEP-INIT-012.                                          NC2454.2
048900 SEP-DELETE-011.                                                  NC2454.2
049000     PERFORM DE-LETE.                                             NC2454.2
049100     PERFORM TEST-WRITE.                                          NC2454.2
049200*                                                                 NC2454.2
049300 SEP-INIT-012.                                                    NC2454.2
049400     MOVE "SEP-TEST-012" TO PAR-NAME.                             NC2454.2
049500     MOVE ZERO TO TEMP; REC-CT.                                   NC2454.2
049600     MOVE 123 TO EXPECTED-VALUE.                                  NC2454.2
049700*        THIS TEST USES SEMICOLON, COMMA AND SPACE AS             NC2454.2
049800*    SEPARATORS IN REFERENCING 3-DIMENSIONAL TABLE                NC2454.2
049900*    ITEMS WITH RELATIVE INDEXING.                                NC2454.2
050000 SEP-TEST-012-01.                                                 NC2454.2
050100     SET INAME1; INAME2; INAME3 TO 3.                             NC2454.2
050200     SET IN-NAME-1; IN-NAME-2; IN-NAME-3 TO 1.                    NC2454.2
050300     MOVE ELEM3 (IN-NAME-1 + 2; INAME2; 3) TO TEMP.               NC2454.2
050400     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
050500 SEP-TEST-012-02.                                                 NC2454.2
050600     MOVE ELEM3 (IN-NAME-1 + 2; IN-NAME-2 + 2;                    NC2454.2
050700         IN-NAME-3 + 2) TO TEMP.                                  NC2454.2
050800     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
050900 SEP-TEST-012-03.                                                 NC2454.2
051000     MOVE ELEM3 (INAME1; IN-NAME-2 + 2; IN-NAME-3 + 2)            NC2454.2
051100         TO TEMP.                                                 NC2454.2
051200     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
051300 SEP-TEST-012-04.                                                 NC2454.2
051400     MOVE ELEM3 (+3, INAME2; IN-NAME-3 + 2) TO TEMP.              NC2454.2
051500     PERFORM TEST-CHECK THRU TEST-WRITE.                          NC2454.2
051600     GO TO CCVS-EXIT.                                             NC2454.2
051700 SEP-DELETE-012.                                                  NC2454.2
051800     PERFORM DE-LETE.                                             NC2454.2
051900     PERFORM TEST-WRITE.                                          NC2454.2
052000*                                                                 NC2454.2
052100 SECT-TH219-0003 SECTION.                                         NC2454.2
052200*                                                                 NC2454.2
052300 TEST-CHECK.                                                      NC2454.2
052400     ADD 1 TO REC-CT.                                             NC2454.2
052500     IF TEMP IS EQUAL TO EXPECTED-VALUE                           NC2454.2
052600         PERFORM PASS                                             NC2454.2
052700         GO TO TEST-WRITE.                                        NC2454.2
052800 TEST-FAIL.                                                       NC2454.2
052900     PERFORM FAIL.                                                NC2454.2
053000     MOVE TEMP TO COMPUTED-18V0.                                  NC2454.2
053100     MOVE EXPECTED-VALUE TO CORRECT-18V0.                         NC2454.2
053200 TEST-WRITE.                                                      NC2454.2
053300     PERFORM PRINT-DETAIL.                                        NC2454.2
053400     MOVE 0 TO TEMP.                                              NC2454.2
053500 CCVS-EXIT SECTION.                                               NC2454.2
053600 CCVS-999999.                                                     NC2454.2
053700     GO TO CLOSE-FILES.                                           NC2454.2
      *END-OF,NC245A                                                                  
