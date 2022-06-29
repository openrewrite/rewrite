      *HEADER,COBOL,NC248A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2484.2
000200 PROGRAM-ID.                                                      NC2484.2
000300     NC248A.                                                      NC2484.2
000400****************************************************************  NC2484.2
000500*                                                              *  NC2484.2
000600*    VALIDATION FOR:-                                          *  NC2484.2
000700*                                                              *  NC2484.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2484.2
000900*                                                              *  NC2484.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2484.2
001100*                                                              *  NC2484.2
001200****************************************************************  NC2484.2
001300*                                                              *  NC2484.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2484.2
001500*                                                              *  NC2484.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2484.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2484.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2484.2
001900*                                                              *  NC2484.2
002000****************************************************************  NC2484.2
002100*                                                                 NC2484.2
002200*    PROGRAM NC248A TESTS FORMATS 1 AND 2 OF THE "SET"         *  NC2484.2
002300*    STATEMENT USING QUALIFICATION WITH INDEXED AND            *  NC2484.2
002400*    RELATIVE-INDEXED IDENTIFIERS.                             *  NC2484.2
002500*    FORMAT 4 OF THE "SET" STATEMENT IS ALSO TESTED.           *  NC2484.2
002600*                                                              *  NC2484.2
002700****************************************************************  NC2484.2
002800 ENVIRONMENT DIVISION.                                            NC2484.2
002900 CONFIGURATION SECTION.                                           NC2484.2
003000 SOURCE-COMPUTER.                                                 NC2484.2
003100     XXXXX082.                                                    NC2484.2
003200 OBJECT-COMPUTER.                                                 NC2484.2
003300     XXXXX083.                                                    NC2484.2
003400 INPUT-OUTPUT SECTION.                                            NC2484.2
003500 FILE-CONTROL.                                                    NC2484.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2484.2
003700     XXXXX055.                                                    NC2484.2
003800 DATA DIVISION.                                                   NC2484.2
003900 FILE SECTION.                                                    NC2484.2
004000 FD  PRINT-FILE.                                                  NC2484.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2484.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2484.2
004300 WORKING-STORAGE SECTION.                                         NC2484.2
004400 01  TEST-7-DATA.                                                 NC2484.2
004500   03  TEST-7-1                  PIC X.                           NC2484.2
004600               88 TEST-7-1-TRUE  VALUE "%".                       NC2484.2
004700 01  TEST-8-DATA.                                                 NC2484.2
004800   03  TEST-8-1                  PIC X.                           NC2484.2
004900               88 TEST-8-1-1-TRUE  VALUE "J".                     NC2484.2
005000               88 TEST-8-1-2-TRUE  VALUE "A".                     NC2484.2
005100               88 TEST-8-1-3-TRUE  VALUE "N".                     NC2484.2
005200 01  TEST-9-DATA.                                                 NC2484.2
005300   03  TEST-9-1                  PIC X.                           NC2484.2
005400               88 TEST-9-1-1-TRUE  VALUE "6".                     NC2484.2
005500               88 TEST-9-1-2-TRUE  VALUE "2".                     NC2484.2
005600   03  TEST-9-2                  PIC X.                           NC2484.2
005700               88 TEST-9-2-1-TRUE  VALUE "B".                     NC2484.2
005800               88 TEST-9-2-2-TRUE  VALUE "C".                     NC2484.2
005900   03  TEST-9-3                  PIC X.                           NC2484.2
006000               88 TEST-9-3-1-TRUE VALUE "*".                      NC2484.2
006100               88 TEST-9-3-2-TRUE VALUE "+".                      NC2484.2
006200                                                                  NC2484.2
006300 01  TABLE1.                                                      NC2484.2
006400     02  TABLE1-REC              PICTURE 99                       NC2484.2
006500                                 OCCURS 100 TIMES                 NC2484.2
006600                                 INDEXED BY INDEX1.               NC2484.2
006700 01  TABLE2.                                                      NC2484.2
006800     02  TABLE2-REC              PICTURE 99                       NC2484.2
006900                                 OCCURS 12 TIMES                  NC2484.2
007000                                 INDEXED BY INDEX2.               NC2484.2
007100 01  INDEX-ID                    PIC 999         VALUE ZEROS.     NC2484.2
007200 01  TEST-RESULTS.                                                NC2484.2
007300     02 FILLER                   PIC X      VALUE SPACE.          NC2484.2
007400     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2484.2
007500     02 FILLER                   PIC X      VALUE SPACE.          NC2484.2
007600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2484.2
007700     02 FILLER                   PIC X      VALUE SPACE.          NC2484.2
007800     02  PAR-NAME.                                                NC2484.2
007900       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2484.2
008000       03  PARDOT-X              PIC X      VALUE SPACE.          NC2484.2
008100       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2484.2
008200     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2484.2
008300     02 RE-MARK                  PIC X(61).                       NC2484.2
008400 01  TEST-COMPUTED.                                               NC2484.2
008500     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2484.2
008600     02 FILLER                   PIC X(17)  VALUE                 NC2484.2
008700            "       COMPUTED=".                                   NC2484.2
008800     02 COMPUTED-X.                                               NC2484.2
008900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2484.2
009000     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2484.2
009100                                 PIC -9(9).9(9).                  NC2484.2
009200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2484.2
009300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2484.2
009400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2484.2
009500     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2484.2
009600         04 COMPUTED-18V0                    PIC -9(18).          NC2484.2
009700         04 FILLER                           PIC X.               NC2484.2
009800     03 FILLER PIC X(50) VALUE SPACE.                             NC2484.2
009900 01  TEST-CORRECT.                                                NC2484.2
010000     02 FILLER PIC X(30) VALUE SPACE.                             NC2484.2
010100     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2484.2
010200     02 CORRECT-X.                                                NC2484.2
010300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2484.2
010400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2484.2
010500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2484.2
010600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2484.2
010700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2484.2
010800     03      CR-18V0 REDEFINES CORRECT-A.                         NC2484.2
010900         04 CORRECT-18V0                     PIC -9(18).          NC2484.2
011000         04 FILLER                           PIC X.               NC2484.2
011100     03 FILLER PIC X(2) VALUE SPACE.                              NC2484.2
011200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2484.2
011300 01  CCVS-C-1.                                                    NC2484.2
011400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2484.2
011500-    "SS  PARAGRAPH-NAME                                          NC2484.2
011600-    "       REMARKS".                                            NC2484.2
011700     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2484.2
011800 01  CCVS-C-2.                                                    NC2484.2
011900     02 FILLER                     PIC X        VALUE SPACE.      NC2484.2
012000     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2484.2
012100     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2484.2
012200     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2484.2
012300     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2484.2
012400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2484.2
012500 01  REC-CT                        PIC 99       VALUE ZERO.       NC2484.2
012600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2484.2
012700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2484.2
012800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2484.2
012900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2484.2
013000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2484.2
013100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2484.2
013200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2484.2
013300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2484.2
013400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2484.2
013500 01  CCVS-H-1.                                                    NC2484.2
013600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2484.2
013700     02  FILLER                    PIC X(42)    VALUE             NC2484.2
013800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2484.2
013900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2484.2
014000 01  CCVS-H-2A.                                                   NC2484.2
014100   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2484.2
014200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2484.2
014300   02  FILLER                        PIC XXXX   VALUE             NC2484.2
014400     "4.2 ".                                                      NC2484.2
014500   02  FILLER                        PIC X(28)  VALUE             NC2484.2
014600            " COPY - NOT FOR DISTRIBUTION".                       NC2484.2
014700   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2484.2
014800                                                                  NC2484.2
014900 01  CCVS-H-2B.                                                   NC2484.2
015000   02  FILLER                        PIC X(15)  VALUE             NC2484.2
015100            "TEST RESULT OF ".                                    NC2484.2
015200   02  TEST-ID                       PIC X(9).                    NC2484.2
015300   02  FILLER                        PIC X(4)   VALUE             NC2484.2
015400            " IN ".                                               NC2484.2
015500   02  FILLER                        PIC X(12)  VALUE             NC2484.2
015600     " HIGH       ".                                              NC2484.2
015700   02  FILLER                        PIC X(22)  VALUE             NC2484.2
015800            " LEVEL VALIDATION FOR ".                             NC2484.2
015900   02  FILLER                        PIC X(58)  VALUE             NC2484.2
016000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2484.2
016100 01  CCVS-H-3.                                                    NC2484.2
016200     02  FILLER                      PIC X(34)  VALUE             NC2484.2
016300            " FOR OFFICIAL USE ONLY    ".                         NC2484.2
016400     02  FILLER                      PIC X(58)  VALUE             NC2484.2
016500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2484.2
016600     02  FILLER                      PIC X(28)  VALUE             NC2484.2
016700            "  COPYRIGHT   1985 ".                                NC2484.2
016800 01  CCVS-E-1.                                                    NC2484.2
016900     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2484.2
017000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2484.2
017100     02 ID-AGAIN                     PIC X(9).                    NC2484.2
017200     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2484.2
017300 01  CCVS-E-2.                                                    NC2484.2
017400     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2484.2
017500     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2484.2
017600     02 CCVS-E-2-2.                                               NC2484.2
017700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2484.2
017800         03 FILLER                   PIC X      VALUE SPACE.      NC2484.2
017900         03 ENDER-DESC               PIC X(44)  VALUE             NC2484.2
018000            "ERRORS ENCOUNTERED".                                 NC2484.2
018100 01  CCVS-E-3.                                                    NC2484.2
018200     02  FILLER                      PIC X(22)  VALUE             NC2484.2
018300            " FOR OFFICIAL USE ONLY".                             NC2484.2
018400     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2484.2
018500     02  FILLER                      PIC X(58)  VALUE             NC2484.2
018600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2484.2
018700     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2484.2
018800     02 FILLER                       PIC X(15)  VALUE             NC2484.2
018900             " COPYRIGHT 1985".                                   NC2484.2
019000 01  CCVS-E-4.                                                    NC2484.2
019100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2484.2
019200     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2484.2
019300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2484.2
019400     02 FILLER                       PIC X(40)  VALUE             NC2484.2
019500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2484.2
019600 01  XXINFO.                                                      NC2484.2
019700     02 FILLER                       PIC X(19)  VALUE             NC2484.2
019800            "*** INFORMATION ***".                                NC2484.2
019900     02 INFO-TEXT.                                                NC2484.2
020000       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2484.2
020100       04 XXCOMPUTED                 PIC X(20).                   NC2484.2
020200       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2484.2
020300       04 XXCORRECT                  PIC X(20).                   NC2484.2
020400     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2484.2
020500 01  HYPHEN-LINE.                                                 NC2484.2
020600     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2484.2
020700     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2484.2
020800-    "*****************************************".                 NC2484.2
020900     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2484.2
021000-    "******************************".                            NC2484.2
021100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2484.2
021200     "NC248A".                                                    NC2484.2
021300 PROCEDURE DIVISION.                                              NC2484.2
021400 CCVS1 SECTION.                                                   NC2484.2
021500 OPEN-FILES.                                                      NC2484.2
021600     OPEN     OUTPUT PRINT-FILE.                                  NC2484.2
021700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2484.2
021800     MOVE    SPACE TO TEST-RESULTS.                               NC2484.2
021900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2484.2
022000     GO TO CCVS1-EXIT.                                            NC2484.2
022100 CLOSE-FILES.                                                     NC2484.2
022200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2484.2
022300 TERMINATE-CCVS.                                                  NC2484.2
022400*    EXIT PROGRAM.                                                NC2484.2
022500*TERMINATE-CALL.                                                  NC2484.2
022600     STOP     RUN.                                                NC2484.2
022700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2484.2
022800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2484.2
022900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2484.2
023000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2484.2
023100     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2484.2
023200 PRINT-DETAIL.                                                    NC2484.2
023300     IF REC-CT NOT EQUAL TO ZERO                                  NC2484.2
023400             MOVE "." TO PARDOT-X                                 NC2484.2
023500             MOVE REC-CT TO DOTVALUE.                             NC2484.2
023600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2484.2
023700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2484.2
023800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2484.2
023900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2484.2
024000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2484.2
024100     MOVE SPACE TO CORRECT-X.                                     NC2484.2
024200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2484.2
024300     MOVE     SPACE TO RE-MARK.                                   NC2484.2
024400 HEAD-ROUTINE.                                                    NC2484.2
024500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2484.2
024600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2484.2
024700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2484.2
024800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2484.2
024900 COLUMN-NAMES-ROUTINE.                                            NC2484.2
025000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2484.2
025100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2484.2
025200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2484.2
025300 END-ROUTINE.                                                     NC2484.2
025400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2484.2
025500 END-RTN-EXIT.                                                    NC2484.2
025600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2484.2
025700 END-ROUTINE-1.                                                   NC2484.2
025800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2484.2
025900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2484.2
026000      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2484.2
026100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2484.2
026200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2484.2
026300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2484.2
026400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2484.2
026500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2484.2
026600  END-ROUTINE-12.                                                 NC2484.2
026700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2484.2
026800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2484.2
026900         MOVE "NO " TO ERROR-TOTAL                                NC2484.2
027000         ELSE                                                     NC2484.2
027100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2484.2
027200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2484.2
027300     PERFORM WRITE-LINE.                                          NC2484.2
027400 END-ROUTINE-13.                                                  NC2484.2
027500     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2484.2
027600         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2484.2
027700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2484.2
027800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2484.2
027900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2484.2
028000      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2484.2
028100          MOVE "NO " TO ERROR-TOTAL                               NC2484.2
028200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2484.2
028300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2484.2
028400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2484.2
028500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2484.2
028600 WRITE-LINE.                                                      NC2484.2
028700     ADD 1 TO RECORD-COUNT.                                       NC2484.2
028800*    IF RECORD-COUNT GREATER 50                                   NC2484.2
028900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2484.2
029000*        MOVE SPACE TO DUMMY-RECORD                               NC2484.2
029100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2484.2
029200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2484.2
029300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2484.2
029400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2484.2
029500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2484.2
029600*        MOVE ZERO TO RECORD-COUNT.                               NC2484.2
029700     PERFORM WRT-LN.                                              NC2484.2
029800 WRT-LN.                                                          NC2484.2
029900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2484.2
030000     MOVE SPACE TO DUMMY-RECORD.                                  NC2484.2
030100 BLANK-LINE-PRINT.                                                NC2484.2
030200     PERFORM WRT-LN.                                              NC2484.2
030300 FAIL-ROUTINE.                                                    NC2484.2
030400     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2484.2
030500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2484.2
030600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2484.2
030700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2484.2
030800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2484.2
030900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2484.2
031000     GO TO  FAIL-ROUTINE-EX.                                      NC2484.2
031100 FAIL-ROUTINE-WRITE.                                              NC2484.2
031200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2484.2
031300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2484.2
031400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2484.2
031500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2484.2
031600 FAIL-ROUTINE-EX. EXIT.                                           NC2484.2
031700 BAIL-OUT.                                                        NC2484.2
031800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2484.2
031900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2484.2
032000 BAIL-OUT-WRITE.                                                  NC2484.2
032100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2484.2
032200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2484.2
032300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2484.2
032400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2484.2
032500 BAIL-OUT-EX. EXIT.                                               NC2484.2
032600 CCVS1-EXIT.                                                      NC2484.2
032700     EXIT.                                                        NC2484.2
032800 BUILD-TABLE2.                                                    NC2484.2
032900     MOVE 21 TO TABLE2-REC (1).                                   NC2484.2
033000     MOVE 02 TO TABLE2-REC (2).                                   NC2484.2
033100     MOVE 03 TO TABLE2-REC (3).                                   NC2484.2
033200     MOVE 11 TO TABLE2-REC (4).                                   NC2484.2
033300     MOVE 05 TO TABLE2-REC (5).                                   NC2484.2
033400     MOVE 10 TO TABLE2-REC (6).                                   NC2484.2
033500     MOVE 26 TO TABLE2-REC (7).                                   NC2484.2
033600     MOVE 02 TO TABLE2-REC (8).                                   NC2484.2
033700     MOVE 16 TO TABLE2-REC (9).                                   NC2484.2
033800     MOVE 62 TO TABLE2-REC (10).                                  NC2484.2
033900     MOVE 10 TO TABLE2-REC (11).                                  NC2484.2
034000     MOVE 04 TO TABLE2-REC (12).                                  NC2484.2
034100*                                                                 NC2484.2
034200 SET-INIT-F1-1.                                                   NC2484.2
034300     MOVE "SET-TEST-F1-1" TO PAR-NAME.                            NC2484.2
034400     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2484.2
034500     MOVE "SET ... TO" TO FEATURE.                                NC2484.2
034600 SET-TEST-F1-1.                                                   NC2484.2
034700     SET INDEX1 TO 1.                                             NC2484.2
034800     SET INDEX2 TO 4.                                             NC2484.2
034900     SET INDEX1 TO TABLE2-REC OF TABLE2 (INDEX2).                 NC2484.2
035000     IF INDEX1 EQUAL TO 11                                        NC2484.2
035100        PERFORM PASS                                              NC2484.2
035200        GO TO SET-WRITE-F1-1                                      NC2484.2
035300     ELSE                                                         NC2484.2
035400        GO TO SET-FAIL-F1-1.                                      NC2484.2
035500 SET-DELETE-F1-1.                                                 NC2484.2
035600     PERFORM DE-LETE.                                             NC2484.2
035700     GO TO SET-WRITE-F1-1.                                        NC2484.2
035800 SET-FAIL-F1-1.                                                   NC2484.2
035900     PERFORM FAIL.                                                NC2484.2
036000     SET INDEX-ID TO INDEX1.                                      NC2484.2
036100     MOVE INDEX-ID TO COMPUTED-18V0.                              NC2484.2
036200     MOVE 11 TO CORRECT-18V0.                                     NC2484.2
036300 SET-WRITE-F1-1.                                                  NC2484.2
036400     PERFORM PRINT-DETAIL.                                        NC2484.2
036500*                                                                 NC2484.2
036600 SET-INIT-F2-2.                                                   NC2484.2
036700     MOVE "SET-TEST-F2-2" TO PAR-NAME.                            NC2484.2
036800     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2484.2
036900     MOVE "SET ... UP BY" TO FEATURE.                             NC2484.2
037000     PERFORM BUILD-TABLE2.                                        NC2484.2
037100 SET-TEST-F2-2.                                                   NC2484.2
037200     SET INDEX1 TO 11.                                            NC2484.2
037300     SET INDEX2 TO 5.                                             NC2484.2
037400     SET INDEX1 UP BY TABLE2-REC OF TABLE2 (INDEX2).              NC2484.2
037500     IF INDEX1 EQUAL TO 16                                        NC2484.2
037600        PERFORM PASS                                              NC2484.2
037700        GO TO SET-WRITE-F2-2                                      NC2484.2
037800     ELSE                                                         NC2484.2
037900        GO TO SET-FAIL-F2-2.                                      NC2484.2
038000 SET-DELETE-F2-2.                                                 NC2484.2
038100     PERFORM DE-LETE.                                             NC2484.2
038200     GO TO SET-WRITE-F2-2.                                        NC2484.2
038300 SET-FAIL-F2-2.                                                   NC2484.2
038400     PERFORM FAIL.                                                NC2484.2
038500     SET INDEX-ID TO INDEX1.                                      NC2484.2
038600     MOVE INDEX-ID TO COMPUTED-18V0.                              NC2484.2
038700     MOVE 16 TO CORRECT-18V0.                                     NC2484.2
038800 SET-WRITE-F2-2.                                                  NC2484.2
038900     PERFORM PRINT-DETAIL.                                        NC2484.2
039000*                                                                 NC2484.2
039100 SET-INIT-F2-3.                                                   NC2484.2
039200     MOVE "SET-TEST-F2-3" TO PAR-NAME.                            NC2484.2
039300     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2484.2
039400     MOVE "SET ... DOWN BY" TO FEATURE.                           NC2484.2
039500     PERFORM BUILD-TABLE2.                                        NC2484.2
039600 SET-TEST-F2-3.                                                   NC2484.2
039700     SET INDEX1 TO 16.                                            NC2484.2
039800     SET INDEX2 TO 6.                                             NC2484.2
039900     SET INDEX1 DOWN BY TABLE2-REC OF TABLE2 (INDEX2).            NC2484.2
040000     IF INDEX1 EQUAL TO 06                                        NC2484.2
040100        PERFORM PASS                                              NC2484.2
040200        GO TO SET-WRITE-F2-3                                      NC2484.2
040300     ELSE                                                         NC2484.2
040400        GO TO SET-FAIL-F2-3.                                      NC2484.2
040500 SET-DELETE-F2-3.                                                 NC2484.2
040600     PERFORM DE-LETE.                                             NC2484.2
040700     GO TO SET-WRITE-F2-3.                                        NC2484.2
040800 SET-FAIL-F2-3.                                                   NC2484.2
040900     PERFORM FAIL.                                                NC2484.2
041000     SET INDEX-ID TO INDEX1.                                      NC2484.2
041100     MOVE INDEX-ID TO COMPUTED-18V0.                              NC2484.2
041200     MOVE 06 TO CORRECT-18V0.                                     NC2484.2
041300 SET-WRITE-F2-3.                                                  NC2484.2
041400     PERFORM PRINT-DETAIL.                                        NC2484.2
041500*                                                                 NC2484.2
041600 SET-INIT-F1-4.                                                   NC2484.2
041700     MOVE "SET-TEST-F1-4" TO PAR-NAME.                            NC2484.2
041800     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2484.2
041900     MOVE "SET ... TO" TO FEATURE.                                NC2484.2
042000     PERFORM BUILD-TABLE2.                                        NC2484.2
042100 SET-TEST-F1-4.                                                   NC2484.2
042200     SET INDEX1 TO 1.                                             NC2484.2
042300     SET INDEX2 TO 11.                                            NC2484.2
042400     SET INDEX1 TO TABLE2-REC OF TABLE2 (INDEX2 + 1).             NC2484.2
042500     IF INDEX1 EQUAL TO 4                                         NC2484.2
042600        PERFORM PASS                                              NC2484.2
042700        GO TO SET-WRITE-F1-4                                      NC2484.2
042800     ELSE                                                         NC2484.2
042900        GO TO SET-FAIL-F1-4.                                      NC2484.2
043000 SET-DELETE-F1-4.                                                 NC2484.2
043100     PERFORM DE-LETE.                                             NC2484.2
043200     GO TO SET-WRITE-F1-4.                                        NC2484.2
043300 SET-FAIL-F1-4.                                                   NC2484.2
043400     PERFORM FAIL.                                                NC2484.2
043500     SET INDEX-ID TO INDEX1.                                      NC2484.2
043600     MOVE INDEX-ID TO COMPUTED-18V0.                              NC2484.2
043700     MOVE 4 TO CORRECT-18V0.                                      NC2484.2
043800 SET-WRITE-F1-4.                                                  NC2484.2
043900     PERFORM PRINT-DETAIL.                                        NC2484.2
044000*                                                                 NC2484.2
044100 SET-INIT-F2-5.                                                   NC2484.2
044200     MOVE "SET-TEST-F2-5" TO PAR-NAME.                            NC2484.2
044300     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2484.2
044400     MOVE "SET ... UP BY" TO FEATURE.                             NC2484.2
044500     PERFORM BUILD-TABLE2.                                        NC2484.2
044600 SET-TEST-F2-5.                                                   NC2484.2
044700     SET INDEX1 TO 1.                                             NC2484.2
044800     SET INDEX2 TO 3.                                             NC2484.2
044900     SET INDEX1 UP BY TABLE2-REC OF TABLE2 (INDEX2 - 2).          NC2484.2
045000     IF INDEX1 EQUAL TO 22                                        NC2484.2
045100        PERFORM PASS                                              NC2484.2
045200        GO TO SET-WRITE-F2-5                                      NC2484.2
045300     ELSE                                                         NC2484.2
045400        GO TO SET-FAIL-F2-5.                                      NC2484.2
045500 SET-DELETE-F2-5.                                                 NC2484.2
045600     PERFORM DE-LETE.                                             NC2484.2
045700     GO TO SET-WRITE-F2-5.                                        NC2484.2
045800 SET-FAIL-F2-5.                                                   NC2484.2
045900     PERFORM FAIL.                                                NC2484.2
046000     SET INDEX-ID TO INDEX1.                                      NC2484.2
046100     MOVE INDEX-ID TO COMPUTED-18V0.                              NC2484.2
046200     MOVE 22 TO CORRECT-18V0.                                     NC2484.2
046300 SET-WRITE-F2-5.                                                  NC2484.2
046400     PERFORM PRINT-DETAIL.                                        NC2484.2
046500*                                                                 NC2484.2
046600 SET-INIT-F2-6.                                                   NC2484.2
046700     MOVE "SET-TEST-F2-6" TO PAR-NAME.                            NC2484.2
046800     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2484.2
046900     MOVE "SET ... DOWN BY" TO FEATURE.                           NC2484.2
047000     PERFORM BUILD-TABLE2.                                        NC2484.2
047100 SET-TEST-F2-6.                                                   NC2484.2
047200     SET INDEX1 TO 16.                                            NC2484.2
047300     SET INDEX2 TO 12.                                            NC2484.2
047400     SET INDEX1 DOWN BY TABLE2-REC OF TABLE2 (INDEX2 - 6).        NC2484.2
047500     IF INDEX1 EQUAL TO 06                                        NC2484.2
047600        PERFORM PASS                                              NC2484.2
047700        GO TO SET-WRITE-F2-6                                      NC2484.2
047800     ELSE                                                         NC2484.2
047900        GO TO SET-FAIL-F2-6.                                      NC2484.2
048000 SET-DELETE-F2-6.                                                 NC2484.2
048100     PERFORM DE-LETE.                                             NC2484.2
048200     GO TO SET-WRITE-F2-6.                                        NC2484.2
048300 SET-FAIL-F2-6.                                                   NC2484.2
048400     PERFORM FAIL.                                                NC2484.2
048500     SET INDEX-ID TO INDEX1.                                      NC2484.2
048600     MOVE INDEX-ID TO COMPUTED-18V0.                              NC2484.2
048700     MOVE 06 TO CORRECT-18V0.                                     NC2484.2
048800 SET-WRITE-F2-6.                                                  NC2484.2
048900     PERFORM PRINT-DETAIL.                                        NC2484.2
049000*                                                                 NC2484.2
049100 SET-INIT-F2-7.                                                   NC2484.2
049200     MOVE   "SET-TEST-F2-7" TO PAR-NAME.                          NC2484.2
049300     MOVE   "VI-128 6.22.4 GR7" TO ANSI-REFERENCE.                NC2484.2
049400     MOVE    SPACE TO TEST-7-1.                                   NC2484.2
049500 SET-TEST-F2-7-0.                                                 NC2484.2
049600     SET     TEST-7-1-TRUE TO TRUE.                               NC2484.2
049700     GO TO   SET-TEST-F2-7-1.                                     NC2484.2
049800 SET-DELETE-F2-7.                                                 NC2484.2
049900     PERFORM DE-LETE.                                             NC2484.2
050000     GO TO   SET-WRITE-F2-7-1.                                    NC2484.2
050100*                                                                 NC2484.2
050200 SET-TEST-F2-7-1.                                                 NC2484.2
050300     IF      TEST-7-1 = "%"                                       NC2484.2
050400             PERFORM PASS                                         NC2484.2
050500             GO TO   SET-WRITE-F2-7-1                             NC2484.2
050600     ELSE                                                         NC2484.2
050700             GO TO   SET-FAIL-F2-7-1.                             NC2484.2
050800 SET-DELETE-F2-7-1.                                               NC2484.2
050900     PERFORM DE-LETE.                                             NC2484.2
051000     GO TO SET-WRITE-F2-7-1.                                      NC2484.2
051100 SET-FAIL-F2-7-1.                                                 NC2484.2
051200     PERFORM FAIL.                                                NC2484.2
051300     MOVE   "CONDITION VARIABLE NOT SET TO TRUE VALUE"            NC2484.2
051400          TO RE-MARK.                                             NC2484.2
051500     MOVE    TEST-7-1 TO COMPUTED-X.                              NC2484.2
051600     MOVE   "%"       TO CORRECT-X.                               NC2484.2
051700 SET-WRITE-F2-7-1.                                                NC2484.2
051800     PERFORM PRINT-DETAIL.                                        NC2484.2
051900*                                                                 NC2484.2
052000 SET-INIT-F2-8.                                                   NC2484.2
052100     MOVE   "SET-TEST-F2-8" TO PAR-NAME.                          NC2484.2
052200     MOVE   "VI-128 6.22.4 GR7" TO ANSI-REFERENCE.                NC2484.2
052300     MOVE    SPACE TO TEST-8-1.                                   NC2484.2
052400 SET-TEST-F2-8-0.                                                 NC2484.2
052500     SET     TEST-8-1-1-TRUE TO TRUE.                             NC2484.2
052600     GO TO   SET-TEST-F2-8-1.                                     NC2484.2
052700 SET-DELETE-F2-8.                                                 NC2484.2
052800     PERFORM DE-LETE.                                             NC2484.2
052900     GO TO   SET-WRITE-F2-8-1.                                    NC2484.2
053000*                                                                 NC2484.2
053100 SET-TEST-F2-8-1.                                                 NC2484.2
053200     IF      TEST-8-1 = "J"                                       NC2484.2
053300             PERFORM PASS                                         NC2484.2
053400             GO TO   SET-WRITE-F2-8-1                             NC2484.2
053500     ELSE                                                         NC2484.2
053600             GO TO   SET-FAIL-F2-8-1.                             NC2484.2
053700 SET-DELETE-F2-8-1.                                               NC2484.2
053800     PERFORM DE-LETE.                                             NC2484.2
053900     GO TO SET-WRITE-F2-8-1.                                      NC2484.2
054000 SET-FAIL-F2-8-1.                                                 NC2484.2
054100     PERFORM FAIL.                                                NC2484.2
054200     MOVE   "CONDITION VARIABLE NOT SET TO FIRST TRUE VALUE"      NC2484.2
054300          TO RE-MARK.                                             NC2484.2
054400     MOVE    TEST-8-1 TO COMPUTED-X.                              NC2484.2
054500     MOVE   "J"       TO CORRECT-X.                               NC2484.2
054600 SET-WRITE-F2-8-1.                                                NC2484.2
054700     PERFORM PRINT-DETAIL.                                        NC2484.2
054800*                                                                 NC2484.2
054900 SET-INIT-F2-9.                                                   NC2484.2
055000     MOVE   "SET-TEST-F2-9" TO PAR-NAME.                          NC2484.2
055100     MOVE   "VI-128 6.22.4 GR7" TO ANSI-REFERENCE.                NC2484.2
055200     MOVE    SPACE TO TEST-8-1.                                   NC2484.2
055300     MOVE    1 TO REC-CT.                                         NC2484.2
055400 SET-TEST-F2-9-0.                                                 NC2484.2
055500     SET     TEST-9-1-1-TRUE                                      NC2484.2
055600             TEST-9-2-1-TRUE                                      NC2484.2
055700             TEST-9-3-1-TRUE TO TRUE.                             NC2484.2
055800     GO TO   SET-TEST-F2-9-1.                                     NC2484.2
055900 SET-DELETE-F2-9.                                                 NC2484.2
056000     PERFORM DE-LETE.                                             NC2484.2
056100     GO TO   SET-WRITE-F2-9-3.                                    NC2484.2
056200*                                                                 NC2484.2
056300 SET-TEST-F2-9-1.                                                 NC2484.2
056400     IF      TEST-9-1 = "6"                                       NC2484.2
056500             PERFORM PASS                                         NC2484.2
056600             GO TO   SET-WRITE-F2-9-1                             NC2484.2
056700     ELSE                                                         NC2484.2
056800             GO TO   SET-FAIL-F2-9-1.                             NC2484.2
056900 SET-DELETE-F2-9-1.                                               NC2484.2
057000     PERFORM DE-LETE.                                             NC2484.2
057100     GO TO SET-WRITE-F2-9-1.                                      NC2484.2
057200 SET-FAIL-F2-9-1.                                                 NC2484.2
057300     PERFORM FAIL.                                                NC2484.2
057400     MOVE   "CONDITION VARIABLE NOT SET TO FIRST TRUE VALUE"      NC2484.2
057500          TO RE-MARK.                                             NC2484.2
057600     MOVE    TEST-9-1 TO COMPUTED-X.                              NC2484.2
057700     MOVE   "6"       TO CORRECT-X.                               NC2484.2
057800 SET-WRITE-F2-9-1.                                                NC2484.2
057900     PERFORM PRINT-DETAIL.                                        NC2484.2
058000*                                                                 NC2484.2
058100 SET-TEST-F2-9-2.                                                 NC2484.2
058200     ADD     1 TO REC-CT.                                         NC2484.2
058300     IF      TEST-9-2 = "B"                                       NC2484.2
058400             PERFORM PASS                                         NC2484.2
058500             GO TO   SET-WRITE-F2-9-2                             NC2484.2
058600     ELSE                                                         NC2484.2
058700             GO TO   SET-FAIL-F2-9-2.                             NC2484.2
058800 SET-DELETE-F2-9-2.                                               NC2484.2
058900     PERFORM DE-LETE.                                             NC2484.2
059000     GO TO SET-WRITE-F2-9-2.                                      NC2484.2
059100 SET-FAIL-F2-9-2.                                                 NC2484.2
059200     PERFORM FAIL.                                                NC2484.2
059300     MOVE   "CONDITION VARIABLE NOT SET TO FIRST TRUE VALUE"      NC2484.2
059400          TO RE-MARK.                                             NC2484.2
059500     MOVE    TEST-9-2 TO COMPUTED-X.                              NC2484.2
059600     MOVE   "6"       TO CORRECT-X.                               NC2484.2
059700 SET-WRITE-F2-9-2.                                                NC2484.2
059800     PERFORM PRINT-DETAIL.                                        NC2484.2
059900*                                                                 NC2484.2
060000 SET-TEST-F2-9-3.                                                 NC2484.2
060100     ADD     1 TO REC-CT.                                         NC2484.2
060200     IF      TEST-9-3 = "*"                                       NC2484.2
060300             PERFORM PASS                                         NC2484.2
060400             GO TO   SET-WRITE-F2-9-3                             NC2484.2
060500     ELSE                                                         NC2484.2
060600             GO TO   SET-FAIL-F2-9-3.                             NC2484.2
060700 SET-DELETE-F2-9-3.                                               NC2484.2
060800     PERFORM DE-LETE.                                             NC2484.2
060900     GO TO SET-WRITE-F2-9-3.                                      NC2484.2
061000 SET-FAIL-F2-9-3.                                                 NC2484.2
061100     PERFORM FAIL.                                                NC2484.2
061200     MOVE   "CONDITION VARIABLE NOT SET TO FIRST TRUE VALUE"      NC2484.2
061300          TO RE-MARK.                                             NC2484.2
061400     MOVE    TEST-9-3 TO COMPUTED-X.                              NC2484.2
061500     MOVE   "*"       TO CORRECT-X.                               NC2484.2
061600 SET-WRITE-F2-9-3.                                                NC2484.2
061700     PERFORM PRINT-DETAIL.                                        NC2484.2
061800*                                                                 NC2484.2
061900 CCVS-EXIT SECTION.                                               NC2484.2
062000 CCVS-999999.                                                     NC2484.2
062100     GO TO CLOSE-FILES.                                           NC2484.2
      *END-OF,NC248A                                                                  
