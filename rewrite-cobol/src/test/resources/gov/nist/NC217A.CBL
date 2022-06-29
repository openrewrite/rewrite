      *HEADER,COBOL,NC217A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2174.2
000200 PROGRAM-ID.                                                      NC2174.2
000300     NC217A.                                                      NC2174.2
000400****************************************************************  NC2174.2
000500*                                                              *  NC2174.2
000600*    VALIDATION FOR:-                                          *  NC2174.2
000700*                                                              *  NC2174.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2174.2
000900*                                                              *  NC2174.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2174.2
001100*                                                              *  NC2174.2
001200****************************************************************  NC2174.2
001300*                                                              *  NC2174.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2174.2
001500*                                                              *  NC2174.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2174.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2174.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2174.2
001900*                                                              *  NC2174.2
002000****************************************************************  NC2174.2
002100*                                                              *  NC2174.2
002200*    PROGRAM NC217A TESTS THE USE OF THE "STRING" STATEMENT,   *  NC2174.2
002300*    INCLUDING THE OPTIONAL PHRASES "POINTER", "OVERFLOW",     *  NC2174.2
002400*    "NOT OVERFLOW" AND "END-STRING".                          *  NC2174.2
002500*                                                              *  NC2174.2
002600*                                                              *  NC2174.2
002700****************************************************************  NC2174.2
002800 ENVIRONMENT DIVISION.                                            NC2174.2
002900 CONFIGURATION SECTION.                                           NC2174.2
003000 SOURCE-COMPUTER.                                                 NC2174.2
003100     XXXXX082.                                                    NC2174.2
003200 OBJECT-COMPUTER.                                                 NC2174.2
003300     XXXXX083.                                                    NC2174.2
003400 INPUT-OUTPUT SECTION.                                            NC2174.2
003500 FILE-CONTROL.                                                    NC2174.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2174.2
003700     XXXXX055.                                                    NC2174.2
003800 DATA DIVISION.                                                   NC2174.2
003900 FILE SECTION.                                                    NC2174.2
004000 FD  PRINT-FILE.                                                  NC2174.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2174.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2174.2
004300 WORKING-STORAGE SECTION.                                         NC2174.2
004400 01  WRK-XN-00001-1     PIC X.                                    NC2174.2
004500 01  WRK-XN-00001-2     PIC X.                                    NC2174.2
004600 01  WRK-XN-00001-3     PIC X.                                    NC2174.2
004700 01  ID8-DU-2V0 PIC 99.                                           NC2174.2
004800 01  ISUB-DU-2V0 PIC 99.                                          NC2174.2
004900 01  MY-BOSS-DU-2V0 PIC 99 VALUE ZERO.                            NC2174.2
005000 01  ID1-DS-LS-4 PIC S9(4) VALUE +1001 SIGN IS LEADING SEPARATE.  NC2174.2
005100 01  ID1-DS-TS-4 PIC S9(4) VALUE +1001 SIGN IS TRAILING SEPARATE. NC2174.2
005200 01  ID1-XN-25 PIC X(25) VALUE                                    NC2174.2
005300     "A2345B2345C2345D2345E2345".                                 NC2174.2
005400 01  ID1-XN-X-25 REDEFINES ID1-XN-25.                             NC2174.2
005500     10 ID1-1 PIC X OCCURS 5 TIMES.                               NC2174.2
005600     10 ID1-2 PIC X OCCURS 5 TIMES.                               NC2174.2
005700     10 ID1-3 PIC X OCCURS 5 TIMES.                               NC2174.2
005800     10 ID1-4 PIC X OCCURS 5 TIMES.                               NC2174.2
005900     10 ID1-5 PIC X OCCURS 5 TIMES.                               NC2174.2
006000 01  ZEROX-XN-1 PIC X VALUE ZERO.                                 NC2174.2
006100 01  A-XN-1 PIC X VALUE "A".                                      NC2174.2
006200 01  B-XN-1 PIC X VALUE "B".                                      NC2174.2
006300 01  AB-XN-2 PIC XX VALUE "AB".                                   NC2174.2
006400 01  ID7-XN-5 PIC X(5).                                           NC2174.2
006500 01  ASTER-XN-5 PIC X(5) VALUE "*****".                           NC2174.2
006600 01  ANS-XN-5-1.                                                  NC2174.2
006700     10 FILLER PIC X VALUE LOW-VALUE.                             NC2174.2
006800     10 FILLER PIC X(4) VALUE "ABCD".                             NC2174.2
006900 01  ANS-XN-5-2.                                                  NC2174.2
007000     10 FILLER PIC X VALUE HIGH-VALUE.                            NC2174.2
007100     10 FILLER PIC X(4) VALUE "****".                             NC2174.2
007200 01  DELIM-TABLE-XN-5 PIC X(5) VALUE "CDEFF".                     NC2174.2
007300 01  DELIM-XN-X-1 REDEFINES DELIM-TABLE-XN-5.                     NC2174.2
007400     10 ID3-XN-1 PIC X OCCURS 5 TIMES.                            NC2174.2
007500 01  ABCDEFG-XN-7 PIC X(7) VALUE "ABCDEFG".                       NC2174.2
007600 01  ID7-XN-15  PIC X(15) VALUE SPACES.                           NC2174.2
007700 01  WISH-LIST-XN-37 PIC X(37) VALUE SPACES.                      NC2174.2
007800 01  ANS-XN-37 PIC X(37) VALUE                                    NC2174.2
007900     "GEE I WISH I WAS A FORTRAN PROGRAMMER".                     NC2174.2
008000 01  TEST-21-GROUP.                                               NC2174.2
008100   03  TEST-21-A                 PIC XX.                          NC2174.2
008200   03  TEST-21-B                 PIC XX.                          NC2174.2
008300   03  TEST-21-C                 PIC X.                           NC2174.2
008400                                                                  NC2174.2
008500 01  TEST-RESULTS.                                                NC2174.2
008600     02 FILLER                   PIC X      VALUE SPACE.          NC2174.2
008700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2174.2
008800     02 FILLER                   PIC X      VALUE SPACE.          NC2174.2
008900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2174.2
009000     02 FILLER                   PIC X      VALUE SPACE.          NC2174.2
009100     02  PAR-NAME.                                                NC2174.2
009200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2174.2
009300       03  PARDOT-X              PIC X      VALUE SPACE.          NC2174.2
009400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2174.2
009500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2174.2
009600     02 RE-MARK                  PIC X(61).                       NC2174.2
009700 01  TEST-COMPUTED.                                               NC2174.2
009800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2174.2
009900     02 FILLER                   PIC X(17)  VALUE                 NC2174.2
010000            "       COMPUTED=".                                   NC2174.2
010100     02 COMPUTED-X.                                               NC2174.2
010200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2174.2
010300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2174.2
010400                                 PIC -9(9).9(9).                  NC2174.2
010500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2174.2
010600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2174.2
010700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2174.2
010800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2174.2
010900         04 COMPUTED-18V0                    PIC -9(18).          NC2174.2
011000         04 FILLER                           PIC X.               NC2174.2
011100     03 FILLER PIC X(50) VALUE SPACE.                             NC2174.2
011200 01  TEST-CORRECT.                                                NC2174.2
011300     02 FILLER PIC X(30) VALUE SPACE.                             NC2174.2
011400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2174.2
011500     02 CORRECT-X.                                                NC2174.2
011600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2174.2
011700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2174.2
011800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2174.2
011900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2174.2
012000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2174.2
012100     03      CR-18V0 REDEFINES CORRECT-A.                         NC2174.2
012200         04 CORRECT-18V0                     PIC -9(18).          NC2174.2
012300         04 FILLER                           PIC X.               NC2174.2
012400     03 FILLER PIC X(2) VALUE SPACE.                              NC2174.2
012500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2174.2
012600 01  CCVS-C-1.                                                    NC2174.2
012700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2174.2
012800-    "SS  PARAGRAPH-NAME                                          NC2174.2
012900-    "       REMARKS".                                            NC2174.2
013000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2174.2
013100 01  CCVS-C-2.                                                    NC2174.2
013200     02 FILLER                     PIC X        VALUE SPACE.      NC2174.2
013300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2174.2
013400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2174.2
013500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2174.2
013600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2174.2
013700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2174.2
013800 01  REC-CT                        PIC 99       VALUE ZERO.       NC2174.2
013900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2174.2
014000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2174.2
014100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2174.2
014200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2174.2
014300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2174.2
014400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2174.2
014500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2174.2
014600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2174.2
014700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2174.2
014800 01  CCVS-H-1.                                                    NC2174.2
014900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2174.2
015000     02  FILLER                    PIC X(42)    VALUE             NC2174.2
015100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2174.2
015200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2174.2
015300 01  CCVS-H-2A.                                                   NC2174.2
015400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2174.2
015500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2174.2
015600   02  FILLER                        PIC XXXX   VALUE             NC2174.2
015700     "4.2 ".                                                      NC2174.2
015800   02  FILLER                        PIC X(28)  VALUE             NC2174.2
015900            " COPY - NOT FOR DISTRIBUTION".                       NC2174.2
016000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2174.2
016100                                                                  NC2174.2
016200 01  CCVS-H-2B.                                                   NC2174.2
016300   02  FILLER                        PIC X(15)  VALUE             NC2174.2
016400            "TEST RESULT OF ".                                    NC2174.2
016500   02  TEST-ID                       PIC X(9).                    NC2174.2
016600   02  FILLER                        PIC X(4)   VALUE             NC2174.2
016700            " IN ".                                               NC2174.2
016800   02  FILLER                        PIC X(12)  VALUE             NC2174.2
016900     " HIGH       ".                                              NC2174.2
017000   02  FILLER                        PIC X(22)  VALUE             NC2174.2
017100            " LEVEL VALIDATION FOR ".                             NC2174.2
017200   02  FILLER                        PIC X(58)  VALUE             NC2174.2
017300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2174.2
017400 01  CCVS-H-3.                                                    NC2174.2
017500     02  FILLER                      PIC X(34)  VALUE             NC2174.2
017600            " FOR OFFICIAL USE ONLY    ".                         NC2174.2
017700     02  FILLER                      PIC X(58)  VALUE             NC2174.2
017800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2174.2
017900     02  FILLER                      PIC X(28)  VALUE             NC2174.2
018000            "  COPYRIGHT   1985 ".                                NC2174.2
018100 01  CCVS-E-1.                                                    NC2174.2
018200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2174.2
018300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2174.2
018400     02 ID-AGAIN                     PIC X(9).                    NC2174.2
018500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2174.2
018600 01  CCVS-E-2.                                                    NC2174.2
018700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2174.2
018800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2174.2
018900     02 CCVS-E-2-2.                                               NC2174.2
019000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2174.2
019100         03 FILLER                   PIC X      VALUE SPACE.      NC2174.2
019200         03 ENDER-DESC               PIC X(44)  VALUE             NC2174.2
019300            "ERRORS ENCOUNTERED".                                 NC2174.2
019400 01  CCVS-E-3.                                                    NC2174.2
019500     02  FILLER                      PIC X(22)  VALUE             NC2174.2
019600            " FOR OFFICIAL USE ONLY".                             NC2174.2
019700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2174.2
019800     02  FILLER                      PIC X(58)  VALUE             NC2174.2
019900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2174.2
020000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2174.2
020100     02 FILLER                       PIC X(15)  VALUE             NC2174.2
020200             " COPYRIGHT 1985".                                   NC2174.2
020300 01  CCVS-E-4.                                                    NC2174.2
020400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2174.2
020500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2174.2
020600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2174.2
020700     02 FILLER                       PIC X(40)  VALUE             NC2174.2
020800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2174.2
020900 01  XXINFO.                                                      NC2174.2
021000     02 FILLER                       PIC X(19)  VALUE             NC2174.2
021100            "*** INFORMATION ***".                                NC2174.2
021200     02 INFO-TEXT.                                                NC2174.2
021300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2174.2
021400       04 XXCOMPUTED                 PIC X(20).                   NC2174.2
021500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2174.2
021600       04 XXCORRECT                  PIC X(20).                   NC2174.2
021700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2174.2
021800 01  HYPHEN-LINE.                                                 NC2174.2
021900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2174.2
022000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2174.2
022100-    "*****************************************".                 NC2174.2
022200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2174.2
022300-    "******************************".                            NC2174.2
022400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2174.2
022500     "NC217A".                                                    NC2174.2
022600 PROCEDURE DIVISION.                                              NC2174.2
022700 CCVS1 SECTION.                                                   NC2174.2
022800 OPEN-FILES.                                                      NC2174.2
022900     OPEN     OUTPUT PRINT-FILE.                                  NC2174.2
023000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2174.2
023100     MOVE    SPACE TO TEST-RESULTS.                               NC2174.2
023200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2174.2
023300     GO TO CCVS1-EXIT.                                            NC2174.2
023400 CLOSE-FILES.                                                     NC2174.2
023500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2174.2
023600 TERMINATE-CCVS.                                                  NC2174.2
023700*    EXIT PROGRAM.                                                NC2174.2
023800*TERMINATE-CALL.                                                  NC2174.2
023900     STOP     RUN.                                                NC2174.2
024000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2174.2
024100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2174.2
024200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2174.2
024300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2174.2
024400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2174.2
024500 PRINT-DETAIL.                                                    NC2174.2
024600     IF REC-CT NOT EQUAL TO ZERO                                  NC2174.2
024700             MOVE "." TO PARDOT-X                                 NC2174.2
024800             MOVE REC-CT TO DOTVALUE.                             NC2174.2
024900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2174.2
025000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2174.2
025100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2174.2
025200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2174.2
025300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2174.2
025400     MOVE SPACE TO CORRECT-X.                                     NC2174.2
025500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2174.2
025600     MOVE     SPACE TO RE-MARK.                                   NC2174.2
025700 HEAD-ROUTINE.                                                    NC2174.2
025800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2174.2
025900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2174.2
026000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2174.2
026100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2174.2
026200 COLUMN-NAMES-ROUTINE.                                            NC2174.2
026300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2174.2
026400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2174.2
026500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2174.2
026600 END-ROUTINE.                                                     NC2174.2
026700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2174.2
026800 END-RTN-EXIT.                                                    NC2174.2
026900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2174.2
027000 END-ROUTINE-1.                                                   NC2174.2
027100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2174.2
027200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2174.2
027300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2174.2
027400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2174.2
027500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2174.2
027600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2174.2
027700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2174.2
027800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2174.2
027900  END-ROUTINE-12.                                                 NC2174.2
028000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2174.2
028100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2174.2
028200         MOVE "NO " TO ERROR-TOTAL                                NC2174.2
028300         ELSE                                                     NC2174.2
028400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2174.2
028500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2174.2
028600     PERFORM WRITE-LINE.                                          NC2174.2
028700 END-ROUTINE-13.                                                  NC2174.2
028800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2174.2
028900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2174.2
029000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2174.2
029100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2174.2
029200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2174.2
029300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2174.2
029400          MOVE "NO " TO ERROR-TOTAL                               NC2174.2
029500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2174.2
029600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2174.2
029700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2174.2
029800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2174.2
029900 WRITE-LINE.                                                      NC2174.2
030000     ADD 1 TO RECORD-COUNT.                                       NC2174.2
030100*    IF RECORD-COUNT GREATER 50                                   NC2174.2
030200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2174.2
030300*        MOVE SPACE TO DUMMY-RECORD                               NC2174.2
030400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2174.2
030500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2174.2
030600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2174.2
030700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2174.2
030800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2174.2
030900*        MOVE ZERO TO RECORD-COUNT.                               NC2174.2
031000     PERFORM WRT-LN.                                              NC2174.2
031100 WRT-LN.                                                          NC2174.2
031200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2174.2
031300     MOVE SPACE TO DUMMY-RECORD.                                  NC2174.2
031400 BLANK-LINE-PRINT.                                                NC2174.2
031500     PERFORM WRT-LN.                                              NC2174.2
031600 FAIL-ROUTINE.                                                    NC2174.2
031700     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2174.2
031800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2174.2
031900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2174.2
032000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2174.2
032100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2174.2
032200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2174.2
032300     GO TO  FAIL-ROUTINE-EX.                                      NC2174.2
032400 FAIL-ROUTINE-WRITE.                                              NC2174.2
032500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2174.2
032600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2174.2
032700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2174.2
032800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2174.2
032900 FAIL-ROUTINE-EX. EXIT.                                           NC2174.2
033000 BAIL-OUT.                                                        NC2174.2
033100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2174.2
033200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2174.2
033300 BAIL-OUT-WRITE.                                                  NC2174.2
033400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2174.2
033500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2174.2
033600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2174.2
033700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2174.2
033800 BAIL-OUT-EX. EXIT.                                               NC2174.2
033900 CCVS1-EXIT.                                                      NC2174.2
034000     EXIT.                                                        NC2174.2
034100 SECT-NC217A-001 SECTION.                                         NC2174.2
034200 STR-INIT-GF-1.                                                   NC2174.2
034300     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
034400     MOVE   "STR-TEST-GF-1" TO PAR-NAME.                          NC2174.2
034500     MOVE   "LIT DEL BY SIZE" TO FEATURE.                         NC2174.2
034600     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
034700     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
034800     MOVE    1 TO REC-CT.                                         NC2174.2
034900 STR-TEST-GF-1.                                                   NC2174.2
035000     STRING "ABCDEF" DELIMITED BY SIZE INTO ID7-XN-5              NC2174.2
035100         WITH POINTER ID8-DU-2V0.                                 NC2174.2
035200     GO TO STR-TEST-GF-1-1.                                       NC2174.2
035300 STR-DELETE-GF-1.                                                 NC2174.2
035400     PERFORM DE-LETE.                                             NC2174.2
035500     PERFORM PRINT-DETAIL.                                        NC2174.2
035600     GO      TO STR-INIT-GF-2.                                    NC2174.2
035700 STR-TEST-GF-1-1.                                                 NC2174.2
035800     IF ID7-XN-5 = "ABCDE"                                        NC2174.2
035900         PERFORM PASS                                             NC2174.2
036000         GO TO STR-WRITE-GF-1-1                                   NC2174.2
036100     ELSE                                                         NC2174.2
036200         GO TO STR-FAIL-GF-1-1.                                   NC2174.2
036300 STR-DELETE-GF-1-1.                                               NC2174.2
036400     PERFORM DE-LETE.                                             NC2174.2
036500     GO TO STR-WRITE-GF-1-1.                                      NC2174.2
036600 STR-FAIL-GF-1-1.                                                 NC2174.2
036700     PERFORM FAIL                                                 NC2174.2
036800     MOVE ID7-XN-5 TO COMPUTED-A                                  NC2174.2
036900     MOVE "ABCDE" TO CORRECT-A.                                   NC2174.2
037000 STR-WRITE-GF-1-1.                                                NC2174.2
037100     PERFORM PRINT-DETAIL.                                        NC2174.2
037200*                                                                 NC2174.2
037300 STR-TEST-GF-1-2.                                                 NC2174.2
037400     ADD 1 TO REC-CT.                                             NC2174.2
037500     IF ID8-DU-2V0 = 6                                            NC2174.2
037600         PERFORM PASS                                             NC2174.2
037700         GO TO STR-WRITE-GF-1-2                                   NC2174.2
037800     ELSE                                                         NC2174.2
037900         GO TO STR-FAIL-GF-1-2.                                   NC2174.2
038000 STR-DELETE-GF-1-2.                                               NC2174.2
038100     PERFORM DE-LETE.                                             NC2174.2
038200     GO TO STR-WRITE-GF-1-2.                                      NC2174.2
038300 STR-FAIL-GF-1-2.                                                 NC2174.2
038400     PERFORM FAIL                                                 NC2174.2
038500     MOVE ID8-DU-2V0 TO COMPUTED-N                                NC2174.2
038600     MOVE 6 TO CORRECT-N.                                         NC2174.2
038700 STR-WRITE-GF-1-2.                                                NC2174.2
038800     PERFORM PRINT-DETAIL.                                        NC2174.2
038900*                                                                 NC2174.2
039000 STR-INIT-GF-2.                                                   NC2174.2
039100     MOVE   "STR-TEST-GF-2" TO PAR-NAME.                          NC2174.2
039200     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
039300     MOVE   "LIT DEL BY LIT" TO FEATURE.                          NC2174.2
039400     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
039500     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
039600     MOVE    1 TO REC-CT.                                         NC2174.2
039700 STR-TEST-GF-2-0.                                                 NC2174.2
039800     STRING "ABCDEF" DELIMITED BY "ABCDEFG"                       NC2174.2
039900             INTO ID7-XN-5 WITH POINTER ID8-DU-2V0                NC2174.2
040000             ON OVERFLOW PERFORM PASS                             NC2174.2
040100                         GO TO STR-WRITE-GF-2-1.                  NC2174.2
040200     GO      TO STR-FAIL-GF-2-1.                                  NC2174.2
040300 STR-DELETE-GF-2.                                                 NC2174.2
040400     PERFORM DE-LETE.                                             NC2174.2
040500     PERFORM PRINT-DETAIL.                                        NC2174.2
040600     GO      TO STR-INIT-GF-3.                                    NC2174.2
040700 STR-TEST-GF-2-1.                                                 NC2174.2
040800*    THIS IS THE BLOCK TO WHICH CONTROL WILL BE SENT BY           NC2174.2
040900*    PARAGRAPH "STR-TEST-GF-2-0".                                 NC2174.2
041000 STR-DELETE-GF-2-1.                                               NC2174.2
041100*    GO TO STR-DELETE-GF-2.                                       NC2174.2
041200 STR-FAIL-GF-2-1.                                                 NC2174.2
041300     PERFORM FAIL.                                                NC2174.2
041400     MOVE    "OVERFLOW SHOULD OCCUR" TO RE-MARK.                  NC2174.2
041500 STR-WRITE-GF-2-1.                                                NC2174.2
041600     PERFORM PRINT-DETAIL.                                        NC2174.2
041700*                                                                 NC2174.2
041800 STR-TEST-GF-2-2.                                                 NC2174.2
041900     ADD 1 TO REC-CT.                                             NC2174.2
042000     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
042100          PERFORM PASS                                            NC2174.2
042200          GO TO STR-WRITE-GF-2-2                                  NC2174.2
042300     ELSE                                                         NC2174.2
042400          GO TO STR-FAIL-GF-2-2.                                  NC2174.2
042500 STR-DELETE-GF-2-2.                                               NC2174.2
042600     PERFORM DE-LETE.                                             NC2174.2
042700     GO TO STR-WRITE-GF-2-2.                                      NC2174.2
042800 STR-FAIL-GF-2-2.                                                 NC2174.2
042900     PERFORM FAIL                                                 NC2174.2
043000     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
043100     MOVE    "ABCDE" TO CORRECT-A.                                NC2174.2
043200 STR-WRITE-GF-2-2.                                                NC2174.2
043300     PERFORM PRINT-DETAIL.                                        NC2174.2
043400*                                                                 NC2174.2
043500 STR-TEST-GF-2-3.                                                 NC2174.2
043600     ADD     1 TO REC-CT.                                         NC2174.2
043700     IF      ID8-DU-2V0 = 6                                       NC2174.2
043800          PERFORM PASS                                            NC2174.2
043900          GO TO STR-WRITE-GF-2-3                                  NC2174.2
044000     ELSE                                                         NC2174.2
044100          GO TO STR-FAIL-GF-2-3.                                  NC2174.2
044200 STR-DELETE-GF-2-3.                                               NC2174.2
044300     PERFORM DE-LETE.                                             NC2174.2
044400     GO TO STR-WRITE-GF-2-3.                                      NC2174.2
044500 STR-FAIL-GF-2-3.                                                 NC2174.2
044600     PERFORM FAIL                                                 NC2174.2
044700     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
044800     MOVE    6 TO CORRECT-N.                                      NC2174.2
044900 STR-WRITE-GF-2-3.                                                NC2174.2
045000     PERFORM PRINT-DETAIL.                                        NC2174.2
045100*                                                                 NC2174.2
045200 STR-INIT-GF-3.                                                   NC2174.2
045300     MOVE   "STR-TEST-GF-3" TO PAR-NAME.                          NC2174.2
045400     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
045500     MOVE    "ID DEL BY QUAL ID" TO FEATURE.                      NC2174.2
045600     MOVE "*****" TO ID7-XN-5.                                    NC2174.2
045700     MOVE 1 TO ID8-DU-2V0.                                        NC2174.2
045800     MOVE    5 TO ISUB-DU-2V0.                                    NC2174.2
045900     MOVE 1 TO REC-CT.                                            NC2174.2
046000*                                                                 NC2174.2
046100 STR-TEST-GF-3.                                                   NC2174.2
046200     STRING  ABCDEFG-XN-7 DELIMITED BY ID3-XN-1 (ISUB-DU-2V0)     NC2174.2
046300             INTO ID7-XN-5 POINTER ID8-DU-2V0.                    NC2174.2
046400     GO      TO STR-TEST-GF-3-1.                                  NC2174.2
046500 STR-DELETE-GF-3.                                                 NC2174.2
046600     PERFORM DE-LETE.                                             NC2174.2
046700     PERFORM PRINT-DETAIL.                                        NC2174.2
046800     GO      TO STRING-INIT-4.                                    NC2174.2
046900*                                                                 NC2174.2
047000 STR-TEST-GF-3-1.                                                 NC2174.2
047100     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
047200          PERFORM PASS                                            NC2174.2
047300          GO TO STR-WRITE-GF-3-1                                  NC2174.2
047400     ELSE                                                         NC2174.2
047500          GO TO STR-FAIL-GF-3-1.                                  NC2174.2
047600 STR-DELETE-GF-3-1.                                               NC2174.2
047700     PERFORM DE-LETE.                                             NC2174.2
047800     GO TO STR-WRITE-GF-3-1.                                      NC2174.2
047900 STR-FAIL-GF-3-1.                                                 NC2174.2
048000     PERFORM FAIL                                                 NC2174.2
048100     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
048200     MOVE    "ABCDE" TO CORRECT-A.                                NC2174.2
048300 STR-WRITE-GF-3-1.                                                NC2174.2
048400     PERFORM PRINT-DETAIL.                                        NC2174.2
048500*                                                                 NC2174.2
048600 STR-TEST-GF-3-2.                                                 NC2174.2
048700     ADD     1 TO REC-CT.                                         NC2174.2
048800     IF      ID8-DU-2V0 = 6                                       NC2174.2
048900          PERFORM PASS                                            NC2174.2
049000          GO TO STR-WRITE-GF-3-2                                  NC2174.2
049100     ELSE                                                         NC2174.2
049200          GO TO STR-FAIL-GF-3-2.                                  NC2174.2
049300 STR-DELETE-GF-3-2.                                               NC2174.2
049400     PERFORM DE-LETE.                                             NC2174.2
049500     GO TO STR-WRITE-GF-3-2.                                      NC2174.2
049600 STR-FAIL-GF-3-2.                                                 NC2174.2
049700     PERFORM FAIL                                                 NC2174.2
049800     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
049900     MOVE    6 TO CORRECT-N.                                      NC2174.2
050000 STR-WRITE-GF-3-2.                                                NC2174.2
050100     PERFORM PRINT-DETAIL.                                        NC2174.2
050200*                                                                 NC2174.2
050300 STRING-INIT-4.                                                   NC2174.2
050400     MOVE    "STR-TEST-GF-4" TO PAR-NAME.                         NC2174.2
050500     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
050600     MOVE    "SUBSCRIPTED IDS" TO FEATURE.                        NC2174.2
050700     MOVE "*****" TO ID7-XN-5.                                    NC2174.2
050800     MOVE 1 TO ID8-DU-2V0.                                        NC2174.2
050900     MOVE    ZERO TO REC-CT.                                      NC2174.2
051000     MOVE    "**** " TO P-OR-F.                                   NC2174.2
051100*                                                                 NC2174.2
051200 STRING-TEST-4.                                                   NC2174.2
051300*    STRING  ID1-1 OF ID1-XN-X-25 (ID8-DU-2V0)                    NC2174.2
051400*            ID1-2 OF ID1-XN-X-25 (ID8-DU-2V0)                    NC2174.2
051500*            ID1-3 OF ID1-XN-X-25 (ID8-DU-2V0)                    NC2174.2
051600*            ID1-4 OF ID1-XN-X-25 (ID8-DU-2V0)                    NC2174.2
051700*            ID1-5 OF ID1-XN-X-25 (ID8-DU-2V0)                    NC2174.2
051800*            DELIMITED BY SIZE INTO ID7-XN-5                      NC2174.2
051900*            POINTER ID8-DU-2V0.                                  NC2174.2
052000*    GO      TO STRING-TEST-4-1.                                  NC2174.2
052100 STRING-DELETE-4.                                                 NC2174.2
052200     PERFORM DE-LETE.                                             NC2174.2
052300     MOVE    "*DELETED - ANSC INTERPRETATION*" TO RE-MARK.        NC2174.2
052400     PERFORM PRINT-DETAIL.                                        NC2174.2
052500     GO      TO STR-INIT-GF-5.                                    NC2174.2
052600 STRING-TEST-4-1.                                                 NC2174.2
052700     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
052800     PERFORM PASS                                                 NC2174.2
052900     PERFORM PRINT-DETAIL                                         NC2174.2
053000     ELSE                                                         NC2174.2
053100     PERFORM FAIL                                                 NC2174.2
053200     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
053300     MOVE    "ABCDE" TO CORRECT-A                                 NC2174.2
053400     PERFORM PRINT-DETAIL.                                        NC2174.2
053500     ADD     1 TO REC-CT.                                         NC2174.2
053600 STRING-TEST-4-2.                                                 NC2174.2
053700     IF      ID8-DU-2V0 = 6                                       NC2174.2
053800     PERFORM PASS                                                 NC2174.2
053900     PERFORM PRINT-DETAIL                                         NC2174.2
054000     ELSE                                                         NC2174.2
054100     PERFORM FAIL                                                 NC2174.2
054200     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
054300     MOVE    6 TO CORRECT-N                                       NC2174.2
054400     PERFORM PRINT-DETAIL.                                        NC2174.2
054500*                                                                 NC2174.2
054600 STR-INIT-GF-5.                                                   NC2174.2
054700     MOVE    "STR-TEST-GF-5" TO PAR-NAME.                         NC2174.2
054800     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
054900     MOVE    "IMMEDIATE OVERFLOW" TO FEATURE.                     NC2174.2
055000     MOVE "*****" TO ID7-XN-5.                                    NC2174.2
055100     MOVE "*****" TO ASTER-XN-5.                                  NC2174.2
055200* NOTE THAT THE POINTER IS SET TO A VALUE GREATER THAN            NC2174.2
055300*    THE LENGTH OF THE RECEIVING ITEM ID7-XN-5..........          NC2174.2
055400     MOVE    7 TO ID8-DU-2V0.                                     NC2174.2
055500     MOVE    1 TO REC-CT.                                         NC2174.2
055600*                                                                 NC2174.2
055700 STR-TEST-GF-5-1.                                                 NC2174.2
055800     STRING  "ABCDE" DELIMITED BY ABCDEFG-XN-7 INTO ID7-XN-5      NC2174.2
055900             POINTER ID8-DU-2V0                                   NC2174.2
056000                     ON OVERFLOW PERFORM PASS                     NC2174.2
056100                                 GO TO STR-WRITE-GF-5-1.          NC2174.2
056200     GO      TO STR-FAIL-GF-5-1.                                  NC2174.2
056300 STR-DELETE-GF-5-1.                                               NC2174.2
056400     PERFORM DE-LETE.                                             NC2174.2
056500     PERFORM PRINT-DETAIL.                                        NC2174.2
056600     GO TO STR-INIT-GF-6.                                         NC2174.2
056700 STR-FAIL-GF-5-1.                                                 NC2174.2
056800     PERFORM FAIL.                                                NC2174.2
056900     MOVE    "OVERFLOW SHOULD OCCUR" TO RE-MARK.                  NC2174.2
057000 STR-WRITE-GF-5-1.                                                NC2174.2
057100     PERFORM PRINT-DETAIL.                                        NC2174.2
057200*                                                                 NC2174.2
057300 STR-TEST-GF-5-2.                                                 NC2174.2
057400     ADD 1 TO REC-CT.                                             NC2174.2
057500     IF      ID7-XN-5 = ASTER-XN-5                                NC2174.2
057600          PERFORM PASS                                            NC2174.2
057700          GO TO STR-WRITE-GF-5-2                                  NC2174.2
057800     ELSE                                                         NC2174.2
057900          GO TO STR-FAIL-GF-5-2.                                  NC2174.2
058000 STR-DELETE-GF-5-2.                                               NC2174.2
058100     PERFORM DE-LETE.                                             NC2174.2
058200     GO TO STR-WRITE-GF-5-2.                                      NC2174.2
058300 STR-FAIL-GF-5-2.                                                 NC2174.2
058400     PERFORM FAIL                                                 NC2174.2
058500     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
058600     MOVE    "*****" TO CORRECT-A.                                NC2174.2
058700 STR-WRITE-GF-5-2.                                                NC2174.2
058800     PERFORM PRINT-DETAIL.                                        NC2174.2
058900*                                                                 NC2174.2
059000 STR-TEST-GF-5-3.                                                 NC2174.2
059100     ADD     1 TO REC-CT.                                         NC2174.2
059200     IF      ID8-DU-2V0 = 7                                       NC2174.2
059300          PERFORM PASS                                            NC2174.2
059400          GO TO STR-WRITE-GF-5-3                                  NC2174.2
059500     ELSE                                                         NC2174.2
059600          GO TO STR-FAIL-GF-5-3.                                  NC2174.2
059700 STR-DELETE-GF-5-3.                                               NC2174.2
059800     PERFORM DE-LETE.                                             NC2174.2
059900     GO TO STR-WRITE-GF-5-3.                                      NC2174.2
060000 STR-FAIL-GF-5-3.                                                 NC2174.2
060100     PERFORM FAIL                                                 NC2174.2
060200     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
060300     MOVE    7 TO CORRECT-N.                                      NC2174.2
060400 STR-WRITE-GF-5-3.                                                NC2174.2
060500     PERFORM PRINT-DETAIL.                                        NC2174.2
060600*                                                                 NC2174.2
060700 STR-INIT-GF-6.                                                   NC2174.2
060800     MOVE    "STR-TEST-GF-6" TO PAR-NAME.                         NC2174.2
060900     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
061000     MOVE    "SPACE LIT OVERFLOWS" TO FEATURE.                    NC2174.2
061100     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
061200     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
061300     MOVE    1 TO REC-CT.                                         NC2174.2
061400*                                                                 NC2174.2
061500 STR-TEST-GF-6-1.                                                 NC2174.2
061600     STRING  SPACE "ABCDE" DELIMITED BY " ABCDE"                  NC2174.2
061700             INTO ID7-XN-5 OVERFLOW PERFORM PASS                  NC2174.2
061800                                    GO TO STR-WRITE-GF-6-1.       NC2174.2
061900     GO      TO STR-FAIL-GF-6-1.                                  NC2174.2
062000 STR-DELETE-GF-6-1.                                               NC2174.2
062100     PERFORM DE-LETE.                                             NC2174.2
062200     PERFORM PRINT-DETAIL.                                        NC2174.2
062300     GO      TO STR-INIT-GF-7.                                    NC2174.2
062400 STR-FAIL-GF-6-1.                                                 NC2174.2
062500     PERFORM FAIL.                                                NC2174.2
062600     MOVE    "OVERFLOW SHOULD OCCUR" TO RE-MARK.                  NC2174.2
062700 STR-WRITE-GF-6-1.                                                NC2174.2
062800     PERFORM PRINT-DETAIL.                                        NC2174.2
062900*                                                                 NC2174.2
063000 STR-TEST-GF-6-2.                                                 NC2174.2
063100     ADD 1 TO REC-CT.                                             NC2174.2
063200     IF      ID7-XN-5 = " ABCD"                                   NC2174.2
063300          PERFORM PASS                                            NC2174.2
063400          GO TO STR-WRITE-GF-6-2                                  NC2174.2
063500     ELSE                                                         NC2174.2
063600          GO TO STR-FAIL-GF-6-2.                                  NC2174.2
063700 STR-DELETE-GF-6-2.                                               NC2174.2
063800     PERFORM DE-LETE.                                             NC2174.2
063900     GO TO STR-WRITE-GF-6-2.                                      NC2174.2
064000 STR-FAIL-GF-6-2.                                                 NC2174.2
064100     PERFORM FAIL                                                 NC2174.2
064200     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
064300     MOVE    " ABCD" TO CORRECT-A.                                NC2174.2
064400 STR-WRITE-GF-6-2.                                                NC2174.2
064500     PERFORM PRINT-DETAIL.                                        NC2174.2
064600*                                                                 NC2174.2
064700 STR-INIT-GF-7.                                                   NC2174.2
064800     MOVE    "STR-TEST-GF-7" TO PAR-NAME.                         NC2174.2
064900     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
065000     MOVE    "LITERAL SERIES" TO FEATURE.                         NC2174.2
065100     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
065200     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
065300     MOVE    1 TO REC-CT.                                         NC2174.2
065400*                                                                 NC2174.2
065500 STR-TEST-GF-7-1.                                                 NC2174.2
065600     STRING  "A" "B" "C" "D" "E" DELIMITED "ABCDE" INTO ID7-XN-5  NC2174.2
065700             POINTER ID8-DU-2V0 OVERFLOW GO TO STR-FAIL-GF-7-1.   NC2174.2
065800     PERFORM PASS.                                                NC2174.2
065900     GO TO STR-WRITE-GF-7-1.                                      NC2174.2
066000 STR-DELETE-GF-7-1.                                               NC2174.2
066100     PERFORM DE-LETE.                                             NC2174.2
066200     PERFORM PRINT-DETAIL.                                        NC2174.2
066300     GO      TO STR-INIT-GF-8.                                    NC2174.2
066400 STR-FAIL-GF-7-1.                                                 NC2174.2
066500     PERFORM FAIL.                                                NC2174.2
066600     MOVE    "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK.              NC2174.2
066700 STR-WRITE-GF-7-1.                                                NC2174.2
066800     PERFORM PRINT-DETAIL.                                        NC2174.2
066900*                                                                 NC2174.2
067000 STR-TEST-GF-7-2.                                                 NC2174.2
067100     ADD     1 TO REC-CT.                                         NC2174.2
067200     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
067300          PERFORM PASS                                            NC2174.2
067400          GO TO STR-WRITE-GF-7-2                                  NC2174.2
067500     ELSE                                                         NC2174.2
067600          GO TO STR-FAIL-GF-7-2.                                  NC2174.2
067700 STR-DELETE-GF-7-2.                                               NC2174.2
067800     PERFORM DE-LETE.                                             NC2174.2
067900     GO TO STR-WRITE-GF-7-2.                                      NC2174.2
068000 STR-FAIL-GF-7-2.                                                 NC2174.2
068100     PERFORM FAIL                                                 NC2174.2
068200     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
068300     MOVE    "ABCDE" TO CORRECT-A.                                NC2174.2
068400 STR-WRITE-GF-7-2.                                                NC2174.2
068500     PERFORM PRINT-DETAIL.                                        NC2174.2
068600*                                                                 NC2174.2
068700 STR-TEST-GF-7-3.                                                 NC2174.2
068800     ADD     1 TO REC-CT.                                         NC2174.2
068900     IF      ID8-DU-2V0 = 6                                       NC2174.2
069000          PERFORM PASS                                            NC2174.2
069100          GO TO STR-WRITE-GF-7-3                                  NC2174.2
069200     ELSE                                                         NC2174.2
069300          GO TO STR-FAIL-GF-7-3.                                  NC2174.2
069400 STR-DELETE-GF-7-3.                                               NC2174.2
069500     PERFORM DE-LETE.                                             NC2174.2
069600     GO TO STR-WRITE-GF-7-3.                                      NC2174.2
069700 STR-FAIL-GF-7-3.                                                 NC2174.2
069800     PERFORM FAIL                                                 NC2174.2
069900     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
070000     MOVE    6 TO CORRECT-N.                                      NC2174.2
070100 STR-WRITE-GF-7-3.                                                NC2174.2
070200     PERFORM PRINT-DETAIL.                                        NC2174.2
070300*                                                                 NC2174.2
070400 STR-INIT-GF-8.                                                   NC2174.2
070500     MOVE    "STR-TEST-GF-8" TO PAR-NAME.                         NC2174.2
070600     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
070700     MOVE    "LOW-VALUE OVERFLOW" TO FEATURE.                     NC2174.2
070800     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
070900     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
071000     MOVE    1 TO REC-CT.                                         NC2174.2
071100*                                                                 NC2174.2
071200 STR-TEST-GF-8-1.                                                 NC2174.2
071300     STRING  LOW-VALUE "ABCDE" DELIMITED BY SIZE INTO ID7-XN-5    NC2174.2
071400             WITH POINTER ID8-DU-2V0                              NC2174.2
071500                  ON OVERFLOW PERFORM PASS                        NC2174.2
071600                              GO TO STR-WRITE-GF-8-1.             NC2174.2
071700     GO      TO STR-FAIL-GF-8-1.                                  NC2174.2
071800 STR-DELETE-GF-8-1.                                               NC2174.2
071900     PERFORM DE-LETE.                                             NC2174.2
072000     PERFORM PRINT-DETAIL.                                        NC2174.2
072100     GO      TO STR-INIT-GF-9.                                    NC2174.2
072200 STR-FAIL-GF-8-1.                                                 NC2174.2
072300     PERFORM FAIL.                                                NC2174.2
072400     MOVE    "OVERFLOW SHOULD OCCUR" TO RE-MARK.                  NC2174.2
072500 STR-WRITE-GF-8-1.                                                NC2174.2
072600     PERFORM PRINT-DETAIL.                                        NC2174.2
072700*                                                                 NC2174.2
072800 STR-TEST-GF-8-2.                                                 NC2174.2
072900     ADD     1 TO REC-CT.                                         NC2174.2
073000     IF      ID7-XN-5 = ANS-XN-5-1                                NC2174.2
073100          PERFORM PASS                                            NC2174.2
073200          GO TO STR-WRITE-GF-8-2                                  NC2174.2
073300     ELSE                                                         NC2174.2
073400          GO TO STR-FAIL-GF-8-2.                                  NC2174.2
073500 STR-DELETE-GF-8-2.                                               NC2174.2
073600     PERFORM DE-LETE.                                             NC2174.2
073700     GO TO STR-WRITE-GF-8-2.                                      NC2174.2
073800 STR-FAIL-GF-8-2.                                                 NC2174.2
073900     PERFORM FAIL                                                 NC2174.2
074000     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
074100     MOVE    ANS-XN-5-1 TO CORRECT-A.                             NC2174.2
074200 STR-WRITE-GF-8-2.                                                NC2174.2
074300     PERFORM PRINT-DETAIL.                                        NC2174.2
074400*                                                                 NC2174.2
074500 STR-TEST-GF-8-3.                                                 NC2174.2
074600     ADD     1 TO REC-CT.                                         NC2174.2
074700     IF      ID8-DU-2V0 = 6                                       NC2174.2
074800          PERFORM PASS                                            NC2174.2
074900          GO TO STR-WRITE-GF-8-3                                  NC2174.2
075000     ELSE                                                         NC2174.2
075100          GO TO STR-FAIL-GF-8-3.                                  NC2174.2
075200 STR-DELETE-GF-8-3.                                               NC2174.2
075300     PERFORM DE-LETE.                                             NC2174.2
075400     GO TO STR-WRITE-GF-8-3.                                      NC2174.2
075500 STR-FAIL-GF-8-3.                                                 NC2174.2
075600     PERFORM FAIL                                                 NC2174.2
075700     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
075800     MOVE    6 TO CORRECT-N.                                      NC2174.2
075900 STR-WRITE-GF-8-3.                                                NC2174.2
076000     PERFORM PRINT-DETAIL.                                        NC2174.2
076100*                                                                 NC2174.2
076200 STR-INIT-GF-9.                                                   NC2174.2
076300     MOVE    "STR-TEST-GF-9" TO PAR-NAME.                         NC2174.2
076400     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
076500     MOVE    "HIGH-VALUE DEL SIZE" TO FEATURE.                    NC2174.2
076600     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
076700     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
076800     MOVE    1 TO REC-CT.                                         NC2174.2
076900*                                                                 NC2174.2
077000 STR-TEST-GF-9-1.                                                 NC2174.2
077100     STRING  HIGH-VALUE DELIMITED BY SIZE INTO ID7-XN-5           NC2174.2
077200             POINTER ID8-DU-2V0                                   NC2174.2
077300                   OVERFLOW GO TO STR-FAIL-GF-9-1.                NC2174.2
077400     PERFORM PASS.                                                NC2174.2
077500     GO TO STR-WRITE-GF-9-1.                                      NC2174.2
077600 STR-DELETE-GF-9-1.                                               NC2174.2
077700     PERFORM DE-LETE.                                             NC2174.2
077800     PERFORM PRINT-DETAIL.                                        NC2174.2
077900     GO      TO STR-INIT-GF-10.                                   NC2174.2
078000 STR-FAIL-GF-9-1.                                                 NC2174.2
078100     PERFORM FAIL.                                                NC2174.2
078200     MOVE    "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK.              NC2174.2
078300 STR-WRITE-GF-9-1.                                                NC2174.2
078400     PERFORM PRINT-DETAIL.                                        NC2174.2
078500*                                                                 NC2174.2
078600 STR-TEST-GF-9-2.                                                 NC2174.2
078700     ADD     1 TO REC-CT.                                         NC2174.2
078800     IF      ID7-XN-5 = ANS-XN-5-2                                NC2174.2
078900          PERFORM PASS                                            NC2174.2
079000          GO TO STR-WRITE-GF-9-2                                  NC2174.2
079100     ELSE                                                         NC2174.2
079200          GO TO STR-FAIL-GF-9-2.                                  NC2174.2
079300 STR-DELETE-GF-9-2.                                               NC2174.2
079400     PERFORM DE-LETE.                                             NC2174.2
079500     GO TO STR-WRITE-GF-9-2.                                      NC2174.2
079600 STR-FAIL-GF-9-2.                                                 NC2174.2
079700     PERFORM FAIL                                                 NC2174.2
079800     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
079900     MOVE    ANS-XN-5-2 TO CORRECT-A.                             NC2174.2
080000 STR-WRITE-GF-9-2.                                                NC2174.2
080100     PERFORM PRINT-DETAIL.                                        NC2174.2
080200*                                                                 NC2174.2
080300 STR-TEST-GF-9-3.                                                 NC2174.2
080400     ADD     1 TO REC-CT.                                         NC2174.2
080500     IF      ID8-DU-2V0 = 2                                       NC2174.2
080600          PERFORM PASS                                            NC2174.2
080700          GO TO STR-WRITE-GF-9-3                                  NC2174.2
080800     ELSE                                                         NC2174.2
080900          GO TO STR-FAIL-GF-9-3.                                  NC2174.2
081000 STR-DELETE-GF-9-3.                                               NC2174.2
081100     PERFORM DE-LETE.                                             NC2174.2
081200     GO TO STR-WRITE-GF-9-3.                                      NC2174.2
081300 STR-FAIL-GF-9-3.                                                 NC2174.2
081400     PERFORM FAIL                                                 NC2174.2
081500     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
081600     MOVE    2 TO CORRECT-N.                                      NC2174.2
081700 STR-WRITE-GF-9-3.                                                NC2174.2
081800     PERFORM PRINT-DETAIL.                                        NC2174.2
081900*                                                                 NC2174.2
082000 STR-INIT-GF-10.                                                  NC2174.2
082100     MOVE    "STR-TEST-GF-10" TO PAR-NAME.                        NC2174.2
082200     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
082300     MOVE    "LIT DEL ZERO" TO FEATURE.                           NC2174.2
082400     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
082500     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
082600     MOVE    1 TO REC-CT.                                         NC2174.2
082700*                                                                 NC2174.2
082800 STR-TEST-GF-10-0.                                                NC2174.2
082900     STRING  "A0" "B0D" "C0LKJSD" "D0321" "E0987LKJALKJKLLKJSD"   NC2174.2
083000             DELIMITED BY ZERO INTO ID7-XN-5 POINTER ID8-DU-2V0.  NC2174.2
083100     GO      TO STR-TEST-GF-10-1.                                 NC2174.2
083200 STR-DELETE-GF-10.                                                NC2174.2
083300     PERFORM DE-LETE.                                             NC2174.2
083400     PERFORM PRINT-DETAIL.                                        NC2174.2
083500     GO      TO STR-INIT-GF-11.                                   NC2174.2
083600*                                                                 NC2174.2
083700 STR-TEST-GF-10-1.                                                NC2174.2
083800     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
083900             PERFORM PASS                                         NC2174.2
084000             GO TO STR-WRITE-GF-10-1                              NC2174.2
084100     ELSE                                                         NC2174.2
084200             GO TO STR-FAIL-GF-10-1.                              NC2174.2
084300 STR-DELETE-GF-10-1.                                              NC2174.2
084400     PERFORM DE-LETE.                                             NC2174.2
084500     GO TO STR-WRITE-GF-10-1.                                     NC2174.2
084600 STR-FAIL-GF-10-1.                                                NC2174.2
084700     PERFORM FAIL                                                 NC2174.2
084800     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
084900     MOVE    "ABCDE" TO CORRECT-A.                                NC2174.2
085000 STR-WRITE-GF-10-1.                                               NC2174.2
085100     PERFORM PRINT-DETAIL.                                        NC2174.2
085200*                                                                 NC2174.2
085300 STR-TEST-GF-10-2.                                                NC2174.2
085400     ADD     1 TO REC-CT.                                         NC2174.2
085500     IF      ID8-DU-2V0 = 6                                       NC2174.2
085600             PERFORM PASS                                         NC2174.2
085700             GO TO STR-WRITE-GF-10-2                              NC2174.2
085800     ELSE                                                         NC2174.2
085900             GO TO STR-FAIL-GF-10-2.                              NC2174.2
086000 STR-DELETE-GF-10-2.                                              NC2174.2
086100     PERFORM DE-LETE.                                             NC2174.2
086200     GO TO STR-WRITE-GF-10-2.                                     NC2174.2
086300 STR-FAIL-GF-10-2.                                                NC2174.2
086400     PERFORM FAIL                                                 NC2174.2
086500     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
086600     MOVE    6 TO CORRECT-N.                                      NC2174.2
086700 STR-WRITE-GF-10-2.                                               NC2174.2
086800     PERFORM PRINT-DETAIL.                                        NC2174.2
086900*                                                                 NC2174.2
087000 STR-INIT-GF-11.                                                  NC2174.2
087100     MOVE    "STR-TEST-GF-11" TO PAR-NAME.                        NC2174.2
087200     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
087300     MOVE    "LIT DEL BY QUOTE" TO FEATURE.                       NC2174.2
087400     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
087500     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
087600     MOVE    1 TO REC-CT.                                         NC2174.2
087700*                                                                 NC2174.2
087800 STR-TEST-GF-11.                                                  NC2174.2
087900     STRING  "A""" "B""KJHSF" "C""321654987LLKJHAF" "D""=,l."     NC2174.2
088000             "E""********" DELIMITED BY QUOTE INTO ID7-XN-5       NC2174.2
088100             POINTER ID8-DU-2V0.                                  NC2174.2
088200     GO      TO STR-TEST-GF-11-1.                                 NC2174.2
088300 STR-DELETE-GF-11-0.                                              NC2174.2
088400     PERFORM DE-LETE.                                             NC2174.2
088500     PERFORM PRINT-DETAIL.                                        NC2174.2
088600     GO      TO STR-INIT-GF-12.                                   NC2174.2
088700*                                                                 NC2174.2
088800 STR-TEST-GF-11-1.                                                NC2174.2
088900     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
089000             PERFORM PASS                                         NC2174.2
089100             GO TO STR-WRITE-GF-11-1                              NC2174.2
089200     ELSE                                                         NC2174.2
089300             GO TO STR-FAIL-GF-11-1.                              NC2174.2
089400 STR-DELETE-GF-11-1.                                              NC2174.2
089500     PERFORM DE-LETE.                                             NC2174.2
089600     GO TO STR-WRITE-GF-11-1.                                     NC2174.2
089700 STR-FAIL-GF-11-1.                                                NC2174.2
089800     PERFORM FAIL                                                 NC2174.2
089900     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
090000     MOVE    "ABCDE" TO CORRECT-A.                                NC2174.2
090100 STR-WRITE-GF-11-1.                                               NC2174.2
090200     PERFORM PRINT-DETAIL.                                        NC2174.2
090300     ADD     1 TO REC-CT.                                         NC2174.2
090400*                                                                 NC2174.2
090500 STR-TEST-GF-11-2.                                                NC2174.2
090600     IF      ID8-DU-2V0 = 6                                       NC2174.2
090700             PERFORM PASS                                         NC2174.2
090800             GO TO STR-WRITE-GF-11-2                              NC2174.2
090900     ELSE                                                         NC2174.2
091000             GO TO STR-FAIL-GF-11-2.                              NC2174.2
091100 STR-DELETE-GF-11-2.                                              NC2174.2
091200     PERFORM DE-LETE.                                             NC2174.2
091300     GO TO STR-WRITE-GF-11-2.                                     NC2174.2
091400 STR-FAIL-GF-11-2.                                                NC2174.2
091500     PERFORM FAIL                                                 NC2174.2
091600     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
091700     MOVE    6 TO CORRECT-N.                                      NC2174.2
091800 STR-WRITE-GF-11-2.                                               NC2174.2
091900     PERFORM PRINT-DETAIL.                                        NC2174.2
092000*                                                                 NC2174.2
092100 STR-INIT-GF-12.                                                  NC2174.2
092200     MOVE    "STR-TEST-GF-12" TO PAR-NAME.                        NC2174.2
092300     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
092400     MOVE    "ZERO ID DEL SIZE" TO FEATURE.                       NC2174.2
092500     MOVE    0 TO REC-CT.                                         NC2174.2
092600     MOVE    ALL "*" TO ID7-XN-15.                                NC2174.2
092700*                                                                 NC2174.2
092800 STR-TEST-GF-12-1.                                                NC2174.2
092900     STRING  ZERO ABCDEFG-XN-7 DELIMITED BY SIZE ZERO ABCDEFG-XN-7NC2174.2
093000             DELIMITED BY SIZE INTO ID7-XN-15.                    NC2174.2
093100     IF      ID7-XN-15 = "0ABCDEFG0ABCDEF"                        NC2174.2
093200             PERFORM PASS                                         NC2174.2
093300             GO TO STR-WRITE-GF-12-1                              NC2174.2
093400     ELSE                                                         NC2174.2
093500             GO TO STR-FAIL-GF-12-1.                              NC2174.2
093600 STR-DELETE-GF-12-1.                                              NC2174.2
093700     PERFORM DE-LETE.                                             NC2174.2
093800     GO TO STR-WRITE-GF-12-1.                                     NC2174.2
093900 STR-FAIL-GF-12-1.                                                NC2174.2
094000     PERFORM FAIL                                                 NC2174.2
094100     MOVE    ID7-XN-15 TO COMPUTED-A                              NC2174.2
094200     MOVE    "0ABCDEFG0ABCDEF" TO CORRECT-A.                      NC2174.2
094300 STR-WRITE-GF-12-1.                                               NC2174.2
094400     PERFORM PRINT-DETAIL.                                        NC2174.2
094500*                                                                 NC2174.2
094600 STR-INIT-GF-13.                                                  NC2174.2
094700     MOVE    "STR-TEST-GF-13" TO PAR-NAME.                        NC2174.2
094800     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
094900     MOVE    "LITERAL SERIES" TO FEATURE.                         NC2174.2
095000     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
095100*                                                                 NC2174.2
095200 STR-TEST-GF-13.                                                  NC2174.2
095300     STRING  "A" "B" "C" DELIMITED BY SIZE "D" "E" "F"  DELIMITED NC2174.2
095400             BY SIZE INTO ID7-XN-5.                               NC2174.2
095500     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
095600             PERFORM PASS                                         NC2174.2
095700             GO TO STR-WRITE-GF-13                                NC2174.2
095800     ELSE                                                         NC2174.2
095900             GO TO STR-FAIL-GF-13.                                NC2174.2
096000 STR-DELETE-GF-13.                                                NC2174.2
096100     PERFORM DE-LETE.                                             NC2174.2
096200     GO TO STR-WRITE-GF-13.                                       NC2174.2
096300 STR-FAIL-GF-13.                                                  NC2174.2
096400     PERFORM FAIL                                                 NC2174.2
096500     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
096600     MOVE    "ABCDE" TO CORRECT-A.                                NC2174.2
096700 STR-WRITE-GF-13.                                                 NC2174.2
096800     PERFORM PRINT-DETAIL.                                        NC2174.2
096900*                                                                 NC2174.2
097000 STR-INIT-GF-14.                                                  NC2174.2
097100     MOVE    "STR-TEST-GF-14" TO PAR-NAME.                        NC2174.2
097200     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
097300     MOVE    "IDENTIFIER SERIES" TO FEATURE.                      NC2174.2
097400     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
097500*                                                                 NC2174.2
097600 STR-TEST-GF-14.                                                  NC2174.2
097700     STRING  AB-XN-2 AB-XN-2 AB-XN-2 DELIMITED BY B-XN-1          NC2174.2
097800     AB-XN-2 AB-XN-2 DELIMITED BY B-XN-1 INTO ID7-XN-5.           NC2174.2
097900     IF      ID7-XN-5 = "AAAAA"                                   NC2174.2
098000             PERFORM PASS                                         NC2174.2
098100             GO TO STR-WRITE-GF-14                                NC2174.2
098200     ELSE                                                         NC2174.2
098300             GO TO STR-FAIL-GF-14.                                NC2174.2
098400 STR-DELETE-GF-14.                                                NC2174.2
098500     PERFORM DE-LETE.                                             NC2174.2
098600     GO TO STR-WRITE-GF-14.                                       NC2174.2
098700 STR-FAIL-GF-14.                                                  NC2174.2
098800     PERFORM FAIL                                                 NC2174.2
098900     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
099000     MOVE "AAAAA" TO CORRECT-A.                                   NC2174.2
099100 STR-WRITE-GF-14.                                                 NC2174.2
099200     PERFORM PRINT-DETAIL.                                        NC2174.2
099300*                                                                 NC2174.2
099400 STR-INIT-GF-15.                                                  NC2174.2
099500     MOVE    "STR-TEST-GF-15" TO PAR-NAME.                        NC2174.2
099600     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
099700     MOVE    "SIGN LEADING" TO FEATURE.                           NC2174.2
099800     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
099900     MOVE    +1001 TO ID1-DS-LS-4.                                NC2174.2
100000     MOVE    ZERO TO ZEROX-XN-1.                                  NC2174.2
100100     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
100200     MOVE    1 TO REC-CT.                                         NC2174.2
100300*                                                                 NC2174.2
100400 STR-TEST-GF-15-0.                                                NC2174.2
100500     STRING  ID1-DS-LS-4 DELIMITED BY ZEROX-XN-1 SPACE DELIMITED  NC2174.2
100600             BY SIZE ID1-DS-LS-4 DELIMITED "0" INTO               NC2174.2
100700             ID7-XN-5 POINTER ID8-DU-2V0.                         NC2174.2
100800     GO      TO STR-TEST-GF-15-1.                                 NC2174.2
100900 STR-DELETE-GF-15.                                                NC2174.2
101000     PERFORM DE-LETE.                                             NC2174.2
101100     PERFORM PRINT-DETAIL.                                        NC2174.2
101200     GO      TO STR-INIT-GF-16.                                   NC2174.2
101300*                                                                 NC2174.2
101400 STR-TEST-GF-15-1.                                                NC2174.2
101500     IF      ID7-XN-5 = "+1 +1"                                   NC2174.2
101600             PERFORM PASS                                         NC2174.2
101700             GO TO STR-WRITE-GF-15-1                              NC2174.2
101800     ELSE                                                         NC2174.2
101900             GO TO STR-FAIL-GF-15-1.                              NC2174.2
102000 STR-DELETE-GF-15-1.                                              NC2174.2
102100     PERFORM DE-LETE.                                             NC2174.2
102200     GO TO STR-WRITE-GF-15-1.                                     NC2174.2
102300 STR-FAIL-GF-15-1.                                                NC2174.2
102400     PERFORM FAIL                                                 NC2174.2
102500     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
102600     MOVE    "+1 +1" TO CORRECT-A.                                NC2174.2
102700 STR-WRITE-GF-15-1.                                               NC2174.2
102800     PERFORM PRINT-DETAIL.                                        NC2174.2
102900*                                                                 NC2174.2
103000 STR-TEST-GF-15-2.                                                NC2174.2
103100     ADD     1 TO REC-CT.                                         NC2174.2
103200     IF      ID8-DU-2V0 = 6                                       NC2174.2
103300             PERFORM PASS                                         NC2174.2
103400             GO TO STR-WRITE-GF-15-2                              NC2174.2
103500     ELSE                                                         NC2174.2
103600             GO TO STR-FAIL-GF-15-2.                              NC2174.2
103700 STR-DELETE-GF-15-2.                                              NC2174.2
103800     PERFORM DE-LETE.                                             NC2174.2
103900     GO TO STR-WRITE-GF-15-2.                                     NC2174.2
104000 STR-FAIL-GF-15-2.                                                NC2174.2
104100     PERFORM FAIL                                                 NC2174.2
104200     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
104300     MOVE 6 TO CORRECT-N.                                         NC2174.2
104400 STR-WRITE-GF-15-2.                                               NC2174.2
104500     PERFORM PRINT-DETAIL.                                        NC2174.2
104600*                                                                 NC2174.2
104700 STR-INIT-GF-16.                                                  NC2174.2
104800     MOVE    "STR-TEST-GF-16" TO PAR-NAME.                        NC2174.2
104900     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
105000     MOVE    "SIGN LEADING" TO FEATURE.                           NC2174.2
105100     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
105200     MOVE    +1001 TO ID1-DS-LS-4.                                NC2174.2
105300     MOVE    ZERO TO ZEROX-XN-1.                                  NC2174.2
105400     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
105500     MOVE    1 TO REC-CT.                                         NC2174.2
105600*                                                                 NC2174.2
105700 STR-TEST-GF-16-0.                                                NC2174.2
105800     STRING  ID1-DS-LS-4 DELIMITED "0" " " DELIMITED BY SIZE      NC2174.2
105900             ID1-DS-LS-4 DELIMITED BY ZEROX-XN-1 INTO ID7-XN-5    NC2174.2
106000             POINTER ID8-DU-2V0.                                  NC2174.2
106100     GO      TO STR-TEST-GF-16-1.                                 NC2174.2
106200 STR-DELETE-GF-16.                                                NC2174.2
106300     PERFORM DE-LETE.                                             NC2174.2
106400     PERFORM PRINT-DETAIL.                                        NC2174.2
106500     GO      TO STR-INIT-GF-17.                                   NC2174.2
106600*                                                                 NC2174.2
106700 STR-TEST-GF-16-1.                                                NC2174.2
106800     IF      ID7-XN-5 = "+1 +1"                                   NC2174.2
106900             PERFORM PASS                                         NC2174.2
107000             GO TO STR-WRITE-GF-16-1                              NC2174.2
107100     ELSE                                                         NC2174.2
107200             GO TO STR-FAIL-GF-16-1.                              NC2174.2
107300 STR-DELETE-GF-16-1.                                              NC2174.2
107400     PERFORM DE-LETE.                                             NC2174.2
107500     GO TO STR-WRITE-GF-16-1.                                     NC2174.2
107600 STR-FAIL-GF-16-1.                                                NC2174.2
107700     PERFORM FAIL                                                 NC2174.2
107800     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
107900     MOVE    "+1 +1" TO CORRECT-A.                                NC2174.2
108000 STR-WRITE-GF-16-1.                                               NC2174.2
108100     PERFORM PRINT-DETAIL.                                        NC2174.2
108200*                                                                 NC2174.2
108300 STR-TEST-GF-16-2.                                                NC2174.2
108400     ADD     1 TO REC-CT.                                         NC2174.2
108500     IF      ID8-DU-2V0 = 6                                       NC2174.2
108600             PERFORM PASS                                         NC2174.2
108700             GO TO STR-WRITE-GF-16-2                              NC2174.2
108800     ELSE                                                         NC2174.2
108900             GO TO STR-FAIL-GF-16-2.                              NC2174.2
109000 STR-DELETE-GF-16-2.                                              NC2174.2
109100     PERFORM DE-LETE.                                             NC2174.2
109200     GO TO STR-WRITE-GF-16-2.                                     NC2174.2
109300 STR-FAIL-GF-16-2.                                                NC2174.2
109400     PERFORM FAIL                                                 NC2174.2
109500     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
109600     MOVE    6 TO CORRECT-N.                                      NC2174.2
109700 STR-WRITE-GF-16-2.                                               NC2174.2
109800     PERFORM PRINT-DETAIL.                                        NC2174.2
109900*                                                                 NC2174.2
110000 STR-INIT-GF-17.                                                  NC2174.2
110100     MOVE    "STR-TEST-GF-17" TO PAR-NAME.                        NC2174.2
110200     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
110300     MOVE    "SIGN TRAILING" TO FEATURE.                          NC2174.2
110400     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
110500     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
110600     MOVE    +1001 TO ID1-DS-TS-4.                                NC2174.2
110700     MOVE    1 TO REC-CT.                                         NC2174.2
110800*                                                                 NC2174.2
110900 STR-TEST-GF-17-0.                                                NC2174.2
111000     STRING  ID1-DS-TS-4 DELIMITED BY SIZE SPACE DELIMITED SIZE   NC2174.2
111100             ID1-DS-TS-4 DELIMITED BY SIZE                        NC2174.2
111200             INTO ID7-XN-5 POINTER ID8-DU-2V0.                    NC2174.2
111300     GO      TO STR-TEST-GF-17-1.                                 NC2174.2
111400 STR-DELETE-GF-17.                                                NC2174.2
111500     PERFORM DE-LETE.                                             NC2174.2
111600     PERFORM PRINT-DETAIL.                                        NC2174.2
111700     GO      TO STR-INIT-GF-18.                                   NC2174.2
111800*                                                                 NC2174.2
111900 STR-TEST-GF-17-1.                                                NC2174.2
112000     IF      ID7-XN-5 = "1001+"                                   NC2174.2
112100             PERFORM PASS                                         NC2174.2
112200             GO TO STR-WRITE-GF-17-1                              NC2174.2
112300     ELSE                                                         NC2174.2
112400             GO TO STR-FAIL-GF-17-1.                              NC2174.2
112500 STR-DELETE-GF-17-1.                                              NC2174.2
112600     PERFORM DE-LETE.                                             NC2174.2
112700     GO TO STR-WRITE-GF-17-1.                                     NC2174.2
112800 STR-FAIL-GF-17-1.                                                NC2174.2
112900     PERFORM FAIL                                                 NC2174.2
113000     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
113100     MOVE    "1001+" TO CORRECT-A.                                NC2174.2
113200 STR-WRITE-GF-17-1.                                               NC2174.2
113300     PERFORM PRINT-DETAIL.                                        NC2174.2
113400*                                                                 NC2174.2
113500 STR-TEST-GF-17-2.                                                NC2174.2
113600     ADD     1 TO REC-CT.                                         NC2174.2
113700     IF      ID8-DU-2V0 = 6                                       NC2174.2
113800             PERFORM PASS                                         NC2174.2
113900             GO TO STR-WRITE-GF-17-2                              NC2174.2
114000     ELSE                                                         NC2174.2
114100             GO TO STR-FAIL-GF-17-2.                              NC2174.2
114200 STR-DELETE-GF-17-2.                                              NC2174.2
114300     PERFORM DE-LETE.                                             NC2174.2
114400     GO TO STR-WRITE-GF-17-2.                                     NC2174.2
114500 STR-FAIL-GF-17-2.                                                NC2174.2
114600     PERFORM FAIL                                                 NC2174.2
114700     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
114800     MOVE    6 TO CORRECT-N.                                      NC2174.2
114900 STR-WRITE-GF-17-2.                                               NC2174.2
115000     PERFORM PRINT-DETAIL.                                        NC2174.2
115100*                                                                 NC2174.2
115200 STR-INIT-GF-18.                                                  NC2174.2
115300     MOVE    "STR-TEST-GF-18" TO PAR-NAME.                        NC2174.2
115400     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
115500     MOVE    "NEG LEADING SEPARATE" TO FEATURE.                   NC2174.2
115600     MOVE    -1001 TO ID1-DS-LS-4.                                NC2174.2
115700     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
115800     MOVE    ZERO TO ZEROX-XN-1.                                  NC2174.2
115900     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
116000     MOVE    1 TO REC-CT.                                         NC2174.2
116100*                                                                 NC2174.2
116200 STR-TEST-GF-18-0.                                                NC2174.2
116300     STRING  ID1-DS-LS-4 DELIMITED BY ZEROX-XN-1 SPACE DELIMITED  NC2174.2
116400             BY SIZE ID1-DS-LS-4 DELIMITED BY "0" INTO            NC2174.2
116500             ID7-XN-5 POINTER ID8-DU-2V0.                         NC2174.2
116600     GO      TO STR-TEST-GF-18-1.                                 NC2174.2
116700 STR-DELETE-GF-18.                                                NC2174.2
116800     PERFORM DE-LETE.                                             NC2174.2
116900     PERFORM PRINT-DETAIL.                                        NC2174.2
117000     GO      TO STR-INIT-GF-19.                                   NC2174.2
117100*                                                                 NC2174.2
117200 STR-TEST-GF-18-1.                                                NC2174.2
117300     IF      ID7-XN-5 = "-1 -1"                                   NC2174.2
117400             PERFORM PASS                                         NC2174.2
117500             GO TO STR-WRITE-GF-18-1                              NC2174.2
117600     ELSE                                                         NC2174.2
117700             GO TO STR-FAIL-GF-18-1.                              NC2174.2
117800 STR-DELETE-GF-18-1.                                              NC2174.2
117900     PERFORM DE-LETE.                                             NC2174.2
118000     GO TO STR-WRITE-GF-18-1.                                     NC2174.2
118100 STR-FAIL-GF-18-1.                                                NC2174.2
118200     PERFORM FAIL                                                 NC2174.2
118300     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
118400     MOVE    "-1 -1" TO CORRECT-A.                                NC2174.2
118500 STR-WRITE-GF-18-1.                                               NC2174.2
118600     PERFORM PRINT-DETAIL.                                        NC2174.2
118700*                                                                 NC2174.2
118800 STR-TEST-GF-18-2.                                                NC2174.2
118900     ADD     1 TO REC-CT.                                         NC2174.2
119000     IF      ID8-DU-2V0 = 6                                       NC2174.2
119100             PERFORM PASS                                         NC2174.2
119200             GO TO STR-WRITE-GF-18-2                              NC2174.2
119300     ELSE                                                         NC2174.2
119400             GO TO STR-FAIL-GF-18-2.                              NC2174.2
119500 STR-DELETE-GF-18-2.                                              NC2174.2
119600     PERFORM DE-LETE.                                             NC2174.2
119700     GO TO STR-WRITE-GF-18-2.                                     NC2174.2
119800 STR-FAIL-GF-18-2.                                                NC2174.2
119900     PERFORM FAIL                                                 NC2174.2
120000     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
120100     MOVE    6 TO CORRECT-N.                                      NC2174.2
120200 STR-WRITE-GF-18-2.                                               NC2174.2
120300     PERFORM PRINT-DETAIL.                                        NC2174.2
120400*                                                                 NC2174.2
120500 STR-INIT-GF-19.                                                  NC2174.2
120600     MOVE    "STR-TEST-GF-19" TO PAR-NAME.                        NC2174.2
120700     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
120800     MOVE    "NEGATIVE SIGN TRAIL" TO FEATURE.                    NC2174.2
120900     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
121000     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
121100     MOVE    -1001 TO ID1-DS-TS-4.                                NC2174.2
121200     MOVE    1 TO REC-CT.                                         NC2174.2
121300*                                                                 NC2174.2
121400 STR-TEST-GF-19-0.                                                NC2174.2
121500     STRING  ID1-DS-TS-4 DELIMITED BY SIZE SPACE DELIMITED SIZE   NC2174.2
121600             ID1-DS-TS-4 DELIMITED BY SIZE INTO ID7-XN-5          NC2174.2
121700             POINTER ID8-DU-2V0.                                  NC2174.2
121800     GO      TO STR-TEST-GF-19-1.                                 NC2174.2
121900 STR-DELETE-GF-19.                                                NC2174.2
122000     PERFORM DE-LETE.                                             NC2174.2
122100     PERFORM PRINT-DETAIL.                                        NC2174.2
122200     GO      TO STR-INIT-GF-20.                                   NC2174.2
122300*                                                                 NC2174.2
122400 STR-TEST-GF-19-1.                                                NC2174.2
122500     IF      ID7-XN-5 = "1001-"                                   NC2174.2
122600             PERFORM PASS                                         NC2174.2
122700             GO TO STR-WRITE-GF-19-1                              NC2174.2
122800     ELSE                                                         NC2174.2
122900             GO TO STR-FAIL-GF-19-1.                              NC2174.2
123000 STR-DELETE-GF-19-1.                                              NC2174.2
123100     PERFORM DE-LETE.                                             NC2174.2
123200     GO TO STR-WRITE-GF-19-1.                                     NC2174.2
123300 STR-FAIL-GF-19-1.                                                NC2174.2
123400     PERFORM FAIL                                                 NC2174.2
123500     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
123600     MOVE    "1001-" TO CORRECT-A.                                NC2174.2
123700 STR-WRITE-GF-19-1.                                               NC2174.2
123800     PERFORM PRINT-DETAIL.                                        NC2174.2
123900*                                                                 NC2174.2
124000 STR-TEST-GF-19-2.                                                NC2174.2
124100     ADD     1 TO REC-CT.                                         NC2174.2
124200     IF      ID8-DU-2V0 = 6                                       NC2174.2
124300             PERFORM PASS                                         NC2174.2
124400             GO TO STR-WRITE-GF-19-2                              NC2174.2
124500     ELSE                                                         NC2174.2
124600             GO TO STR-FAIL-GF-19-2.                              NC2174.2
124700 STR-DELETE-GF-19-2.                                              NC2174.2
124800     PERFORM DE-LETE.                                             NC2174.2
124900     GO TO STR-WRITE-GF-19-2.                                     NC2174.2
125000 STR-FAIL-GF-19-2.                                                NC2174.2
125100     PERFORM FAIL                                                 NC2174.2
125200     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
125300     MOVE 6 TO CORRECT-N.                                         NC2174.2
125400 STR-WRITE-GF-19-2.                                               NC2174.2
125500     PERFORM PRINT-DETAIL.                                        NC2174.2
125600*                                                                 NC2174.2
125700 STR-INIT-GF-20.                                                  NC2174.2
125800     MOVE    "STR-TEST-GF-20" TO PAR-NAME.                        NC2174.2
125900     MOVE   "VI-130 6.24.3+4" TO ANSI-REFERENCE.                  NC2174.2
126000     MOVE    "LITERAL SERIES" TO FEATURE.                         NC2174.2
126100     MOVE ALL "*" TO WISH-LIST-XN-37.                             NC2174.2
126200     MOVE "GEE I WISH I WAS A FORTRAN PROGRAMMER" TO ANS-XN-37.   NC2174.2
126300     MOVE    1 TO MY-BOSS-DU-2V0.                                 NC2174.2
126400     MOVE    1 TO REC-CT.                                         NC2174.2
126500*                                                                 NC2174.2
126600 STR-TEST-GF-20-1.                                                NC2174.2
126700     STRING  "GEE" SPACE "I WISH I" SPACES "WAS A FORTRAN" " "    NC2174.2
126800             "PROGRAMMER"                                         NC2174.2
126900             DELIMITED BY SIZE INTO WISH-LIST-XN-37               NC2174.2
127000             WITH POINTER MY-BOSS-DU-2V0                          NC2174.2
127100             ON OVERFLOW GO TO STR-FAIL-GF-20-1.                  NC2174.2
127200     PERFORM PASS.                                                NC2174.2
127300     GO TO STR-WRITE-GF-20-1.                                     NC2174.2
127400 STR-DELETE-GF-20.                                                NC2174.2
127500     PERFORM DE-LETE.                                             NC2174.2
127600     PERFORM PRINT-DETAIL.                                        NC2174.2
127700     GO TO   STR-INIT-GF-21.                                      NC2174.2
127800 STR-FAIL-GF-20-1.                                                NC2174.2
127900     PERFORM FAIL.                                                NC2174.2
128000     MOVE    "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK.              NC2174.2
128100 STR-WRITE-GF-20-1.                                               NC2174.2
128200     PERFORM PRINT-DETAIL.                                        NC2174.2
128300*                                                                 NC2174.2
128400 STR-TEST-GF-20-2.                                                NC2174.2
128500     MOVE    "STR-TEST-GF-20" TO PAR-NAME.                        NC2174.2
128600     MOVE    1 TO REC-CT.                                         NC2174.2
128700     IF      WISH-LIST-XN-37 = ANS-XN-37                          NC2174.2
128800             PERFORM PASS                                         NC2174.2
128900             GO TO STR-WRITE-GF-20-2                              NC2174.2
129000     ELSE                                                         NC2174.2
129100             GO TO STR-FAIL-GF-20-2.                              NC2174.2
129200 STR-DELETE-GF-20-2.                                              NC2174.2
129300     PERFORM DE-LETE.                                             NC2174.2
129400     GO TO STR-WRITE-GF-20-2.                                     NC2174.2
129500 STR-FAIL-GF-20-2.                                                NC2174.2
129600     PERFORM FAIL                                                 NC2174.2
129700     MOVE    WISH-LIST-XN-37 TO COMPUTED-A                        NC2174.2
129800     MOVE    ANS-XN-37 TO CORRECT-A.                              NC2174.2
129900 STR-WRITE-GF-20-2.                                               NC2174.2
130000     PERFORM PRINT-DETAIL.                                        NC2174.2
130100*                                                                 NC2174.2
130200 STR-TEST-GF-20-3.                                                NC2174.2
130300     ADD     1 TO REC-CT.                                         NC2174.2
130400     IF      MY-BOSS-DU-2V0 = 38                                  NC2174.2
130500             PERFORM PASS                                         NC2174.2
130600             GO TO STR-WRITE-GF-20-3                              NC2174.2
130700     ELSE                                                         NC2174.2
130800             GO TO STR-FAIL-GF-20-3.                              NC2174.2
130900 STR-DELETE-GF-20-3.                                              NC2174.2
131000     PERFORM DE-LETE.                                             NC2174.2
131100     GO TO STR-WRITE-GF-20-3.                                     NC2174.2
131200 STR-FAIL-GF-20-3.                                                NC2174.2
131300     PERFORM FAIL                                                 NC2174.2
131400     MOVE    MY-BOSS-DU-2V0 TO COMPUTED-N                         NC2174.2
131500     MOVE    38 TO CORRECT-N.                                     NC2174.2
131600 STR-WRITE-GF-20-3.                                               NC2174.2
131700     PERFORM PRINT-DETAIL.                                        NC2174.2
131800*                                                                 NC2174.2
131900*                                                                 NC2174.2
132000 STR-INIT-GF-21.                                                  NC2174.2
132100*    ===-->  INTO GROUP FIELD  <--===                             NC2174.2
132200     MOVE   "VI-130 6.24.3 GR4" TO ANSI-REFERENCE.                NC2174.2
132300     MOVE   "STR-TEST-GF-21" TO PAR-NAME.                         NC2174.2
132400     MOVE   "LIT DEL BY SIZE" TO FEATURE.                         NC2174.2
132500     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
132600     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
132700     MOVE    1 TO REC-CT.                                         NC2174.2
132800*                                                                 NC2174.2
132900 STR-TEST-GF-21-0.                                                NC2174.2
133000     STRING "ABCDEF" DELIMITED BY SIZE INTO TEST-21-GROUP         NC2174.2
133100             WITH POINTER ID8-DU-2V0.                             NC2174.2
133200     GO TO   STR-TEST-GF-21-1.                                    NC2174.2
133300 STR-DELETE-GF-21.                                                NC2174.2
133400     PERFORM DE-LETE.                                             NC2174.2
133500     PERFORM PRINT-DETAIL.                                        NC2174.2
133600     GO TO   STR-INIT-GF-22.                                      NC2174.2
133700*                                                                 NC2174.2
133800 STR-TEST-GF-21-1.                                                NC2174.2
133900     IF      TEST-21-GROUP = "ABCDE"                              NC2174.2
134000             PERFORM PASS                                         NC2174.2
134100             GO TO STR-WRITE-GF-21-1                              NC2174.2
134200     ELSE                                                         NC2174.2
134300             GO TO STR-FAIL-GF-21-1.                              NC2174.2
134400 STR-DELETE-GF-21-1.                                              NC2174.2
134500     PERFORM DE-LETE.                                             NC2174.2
134600     GO TO STR-WRITE-GF-21-1.                                     NC2174.2
134700 STR-FAIL-GF-21-1.                                                NC2174.2
134800     PERFORM FAIL                                                 NC2174.2
134900     MOVE    TEST-21-GROUP TO COMPUTED-A                          NC2174.2
135000     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
135100 STR-WRITE-GF-21-1.                                               NC2174.2
135200     PERFORM PRINT-DETAIL.                                        NC2174.2
135300*                                                                 NC2174.2
135400 STR-TEST-GF-21-2.                                                NC2174.2
135500     ADD 1 TO REC-CT.                                             NC2174.2
135600     IF      ID8-DU-2V0 = 6                                       NC2174.2
135700             PERFORM PASS                                         NC2174.2
135800             GO TO STR-WRITE-GF-21-2                              NC2174.2
135900     ELSE                                                         NC2174.2
136000             GO TO STR-FAIL-GF-21-2.                              NC2174.2
136100 STR-DELETE-GF-21-2.                                              NC2174.2
136200     PERFORM DE-LETE.                                             NC2174.2
136300     GO TO STR-WRITE-GF-21-2.                                     NC2174.2
136400 STR-FAIL-GF-21-2.                                                NC2174.2
136500     PERFORM FAIL                                                 NC2174.2
136600     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
136700     MOVE    6 TO CORRECT-N.                                      NC2174.2
136800 STR-WRITE-GF-21-2.                                               NC2174.2
136900     PERFORM PRINT-DETAIL.                                        NC2174.2
137000*                                                                 NC2174.2
137100 STR-INIT-GF-22.                                                  NC2174.2
137200*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2174.2
137300     MOVE   "VI-132 6.24.4 GR9, 10" TO ANSI-REFERENCE.            NC2174.2
137400     MOVE   "STR-TEST-GF-22" TO PAR-NAME.                         NC2174.2
137500     MOVE   "LIT DEL BY LIT" TO FEATURE.                          NC2174.2
137600     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
137700     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
137800     MOVE    1 TO REC-CT.                                         NC2174.2
137900*                                                                 NC2174.2
138000 STR-TEST-GF-22-1.                                                NC2174.2
138100     STRING "ABCDEF" DELIMITED BY "ABCDEFG"                       NC2174.2
138200             INTO ID7-XN-5 WITH POINTER ID8-DU-2V0                NC2174.2
138300         NOT ON OVERFLOW GO TO STR-FAIL-GF-22-1.                  NC2174.2
138400     PERFORM PASS.                                                NC2174.2
138500     GO TO STR-WRITE-GF-22-1.                                     NC2174.2
138600 STR-DELETE-GF-22.                                                NC2174.2
138700     PERFORM DE-LETE.                                             NC2174.2
138800     PERFORM PRINT-DETAIL.                                        NC2174.2
138900     GO      TO STR-INIT-GF-23.                                   NC2174.2
139000 STR-FAIL-GF-22-1.                                                NC2174.2
139100     PERFORM FAIL.                                                NC2174.2
139200     MOVE    "NOT ON OVERFLOW SHOULD NOT OCCUR" TO RE-MARK.       NC2174.2
139300 STR-WRITE-GF-22-1.                                               NC2174.2
139400     PERFORM PRINT-DETAIL.                                        NC2174.2
139500*                                                                 NC2174.2
139600 STR-TEST-GF-22-2.                                                NC2174.2
139700     ADD     1 TO REC-CT.                                         NC2174.2
139800     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
139900             PERFORM PASS                                         NC2174.2
140000             GO TO STR-WRITE-GF-22-2                              NC2174.2
140100     ELSE                                                         NC2174.2
140200             GO TO STR-FAIL-GF-22-2.                              NC2174.2
140300 STR-DELETE-GF-22-2.                                              NC2174.2
140400     PERFORM DE-LETE.                                             NC2174.2
140500     GO TO STR-WRITE-GF-22-2.                                     NC2174.2
140600 STR-FAIL-GF-22-2.                                                NC2174.2
140700     PERFORM FAIL.                                                NC2174.2
140800     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
140900     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
141000 STR-WRITE-GF-22-2.                                               NC2174.2
141100     PERFORM PRINT-DETAIL.                                        NC2174.2
141200*                                                                 NC2174.2
141300 STR-TEST-GF-22-3.                                                NC2174.2
141400     ADD     1 TO REC-CT.                                         NC2174.2
141500     IF      ID8-DU-2V0 = 6                                       NC2174.2
141600             PERFORM PASS                                         NC2174.2
141700             GO TO STR-WRITE-GF-22-3                              NC2174.2
141800     ELSE                                                         NC2174.2
141900             GO TO STR-FAIL-GF-22-3.                              NC2174.2
142000 STR-DELETE-GF-22-3.                                              NC2174.2
142100     PERFORM DE-LETE.                                             NC2174.2
142200     GO TO STR-WRITE-GF-22-3.                                     NC2174.2
142300 STR-FAIL-GF-22-3.                                                NC2174.2
142400     PERFORM FAIL                                                 NC2174.2
142500     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
142600     MOVE    6 TO CORRECT-N.                                      NC2174.2
142700 STR-WRITE-GF-22-3.                                               NC2174.2
142800     PERFORM PRINT-DETAIL.                                        NC2174.2
142900*                                                                 NC2174.2
143000 STR-INIT-GF-23.                                                  NC2174.2
143100*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2174.2
143200     MOVE   "VI-132 6.24.4 GR9, 10" TO ANSI-REFERENCE.            NC2174.2
143300     MOVE   "STR-TEST-GF-23" TO PAR-NAME.                         NC2174.2
143400     MOVE   "LITERAL SERIES" TO FEATURE.                          NC2174.2
143500     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
143600     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
143700     MOVE    1 TO REC-CT.                                         NC2174.2
143800*                                                                 NC2174.2
143900 STR-TEST-GF-23-1.                                                NC2174.2
144000     STRING "A" "B" "C" "D" "E" DELIMITED "ABCDE" INTO ID7-XN-5   NC2174.2
144100             POINTER ID8-DU-2V0                                   NC2174.2
144200         NOT ON OVERFLOW PERFORM PASS                             NC2174.2
144300                         GO TO   STR-WRITE-GF-23-1.               NC2174.2
144400     GO TO   STR-FAIL-GF-23-1.                                    NC2174.2
144500 STR-DELETE-GF-23.                                                NC2174.2
144600     PERFORM DE-LETE.                                             NC2174.2
144700     PERFORM PRINT-DETAIL.                                        NC2174.2
144800     GO TO   STR-INIT-GF-24.                                      NC2174.2
144900 STR-FAIL-GF-23-1.                                                NC2174.2
145000     PERFORM FAIL.                                                NC2174.2
145100     MOVE   "NOT ON OVERFLOW SHOULD HAVE EXECUTED" TO RE-MARK.    NC2174.2
145200 STR-WRITE-GF-23-1.                                               NC2174.2
145300     PERFORM PRINT-DETAIL.                                        NC2174.2
145400*                                                                 NC2174.2
145500 STR-TEST-GF-23-2.                                                NC2174.2
145600     ADD     1 TO REC-CT.                                         NC2174.2
145700     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
145800             PERFORM PASS                                         NC2174.2
145900             GO TO STR-WRITE-GF-23-2                              NC2174.2
146000     ELSE                                                         NC2174.2
146100             GO TO STR-FAIL-GF-23-2.                              NC2174.2
146200 STR-DELETE-GF-23-2.                                              NC2174.2
146300     PERFORM DE-LETE.                                             NC2174.2
146400     GO TO STR-WRITE-GF-23-2.                                     NC2174.2
146500 STR-FAIL-GF-23-2.                                                NC2174.2
146600     PERFORM FAIL                                                 NC2174.2
146700     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
146800     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
146900 STR-WRITE-GF-23-2.                                               NC2174.2
147000     PERFORM PRINT-DETAIL.                                        NC2174.2
147100*                                                                 NC2174.2
147200 STR-TEST-GF-23-3.                                                NC2174.2
147300     ADD     1 TO REC-CT.                                         NC2174.2
147400     IF      ID8-DU-2V0 = 6                                       NC2174.2
147500             PERFORM PASS                                         NC2174.2
147600             GO TO STR-WRITE-GF-23-3                              NC2174.2
147700     ELSE                                                         NC2174.2
147800             GO TO STR-FAIL-GF-23-3.                              NC2174.2
147900 STR-DELETE-GF-23-3.                                              NC2174.2
148000     PERFORM DE-LETE.                                             NC2174.2
148100     GO TO STR-WRITE-GF-23-3.                                     NC2174.2
148200 STR-FAIL-GF-23-3.                                                NC2174.2
148300     PERFORM FAIL                                                 NC2174.2
148400     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
148500     MOVE    6 TO CORRECT-N.                                      NC2174.2
148600 STR-WRITE-GF-23-3.                                               NC2174.2
148700     PERFORM PRINT-DETAIL.                                        NC2174.2
148800*                                                                 NC2174.2
148900 STR-INIT-GF-24.                                                  NC2174.2
149000*    ===-->  BOTH "OVERFLOW" PHRASES  <--===                      NC2174.2
149100     MOVE   "VI-132 6.24.4 GR9, 10" TO ANSI-REFERENCE.            NC2174.2
149200     MOVE   "STR-TEST-GF-24" TO PAR-NAME.                         NC2174.2
149300     MOVE   "LIT DEL BY LIT" TO FEATURE.                          NC2174.2
149400     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
149500     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
149600     MOVE    1 TO REC-CT.                                         NC2174.2
149700*                                                                 NC2174.2
149800 STR-TEST-GF-24-1.                                                NC2174.2
149900     STRING "ABCDEF" DELIMITED BY "ABCDEFG"                       NC2174.2
150000             INTO ID7-XN-5 WITH POINTER ID8-DU-2V0                NC2174.2
150100             ON OVERFLOW PERFORM PASS                             NC2174.2
150200                         GO TO STR-WRITE-GF-24-1                  NC2174.2
150300         NOT ON OVERFLOW GO TO STR-FAIL-GF-24-1.                  NC2174.2
150400 STR-DELETE-GF-24.                                                NC2174.2
150500     PERFORM DE-LETE.                                             NC2174.2
150600     PERFORM PRINT-DETAIL.                                        NC2174.2
150700     GO      TO STR-INIT-GF-25.                                   NC2174.2
150800 STR-FAIL-GF-24-1.                                                NC2174.2
150900     PERFORM FAIL.                                                NC2174.2
151000     MOVE    "NOT ON OVERFLOW SHOULD NOT OCCUR" TO RE-MARK.       NC2174.2
151100 STR-WRITE-GF-24-1.                                               NC2174.2
151200     PERFORM PRINT-DETAIL.                                        NC2174.2
151300*                                                                 NC2174.2
151400 STR-TEST-GF-24-2.                                                NC2174.2
151500     ADD     1 TO REC-CT.                                         NC2174.2
151600     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
151700             PERFORM PASS                                         NC2174.2
151800             GO TO STR-WRITE-GF-24-2                              NC2174.2
151900     ELSE                                                         NC2174.2
152000             GO TO STR-FAIL-GF-24-2.                              NC2174.2
152100 STR-DELETE-GF-24-2.                                              NC2174.2
152200     PERFORM DE-LETE.                                             NC2174.2
152300     GO TO STR-WRITE-GF-24-2.                                     NC2174.2
152400 STR-FAIL-GF-24-2.                                                NC2174.2
152500     PERFORM FAIL                                                 NC2174.2
152600     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
152700     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
152800 STR-WRITE-GF-24-2.                                               NC2174.2
152900     PERFORM PRINT-DETAIL.                                        NC2174.2
153000*                                                                 NC2174.2
153100 STR-TEST-GF-24-3.                                                NC2174.2
153200     ADD     1 TO REC-CT.                                         NC2174.2
153300     IF      ID8-DU-2V0 = 6                                       NC2174.2
153400             PERFORM PASS                                         NC2174.2
153500             GO TO STR-WRITE-GF-24-3                              NC2174.2
153600     ELSE                                                         NC2174.2
153700             GO TO STR-FAIL-GF-24-3.                              NC2174.2
153800 STR-DELETE-GF-24-3.                                              NC2174.2
153900     PERFORM DE-LETE.                                             NC2174.2
154000     GO TO STR-WRITE-GF-24-3.                                     NC2174.2
154100 STR-FAIL-GF-24-3.                                                NC2174.2
154200     PERFORM FAIL                                                 NC2174.2
154300     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
154400     MOVE    6 TO CORRECT-N.                                      NC2174.2
154500 STR-WRITE-GF-24-3.                                               NC2174.2
154600     PERFORM PRINT-DETAIL.                                        NC2174.2
154700*                                                                 NC2174.2
154800 STR-INIT-GF-25.                                                  NC2174.2
154900*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2174.2
155000     MOVE   "VI-132 6.24.4 GR9, 10" TO ANSI-REFERENCE.            NC2174.2
155100     MOVE   "STR-TEST-GF-25" TO PAR-NAME.                         NC2174.2
155200     MOVE   "LITERAL SERIES" TO FEATURE.                          NC2174.2
155300     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
155400     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
155500     MOVE    ZERO TO REC-CT.                                      NC2174.2
155600*                                                                 NC2174.2
155700 STR-TEST-GF-25-1.                                                NC2174.2
155800     STRING "A" "B" "C" "D" "E" DELIMITED "ABCDE" INTO ID7-XN-5   NC2174.2
155900             POINTER ID8-DU-2V0                                   NC2174.2
156000             ON OVERFLOW GO TO STR-FAIL-GF-25-1                   NC2174.2
156100         NOT ON OVERFLOW PERFORM PASS                             NC2174.2
156200                         GO TO STR-WRITE-GF-25-1.                 NC2174.2
156300 STR-DELETE-GF-25-1.                                              NC2174.2
156400     PERFORM DE-LETE.                                             NC2174.2
156500     PERFORM PRINT-DETAIL.                                        NC2174.2
156600     GO TO   STR-INIT-GF-26.                                      NC2174.2
156700 STR-FAIL-GF-25-1.                                                NC2174.2
156800     PERFORM FAIL.                                                NC2174.2
156900     MOVE   "ON OVERFLOW SHOULD NOT HAVE EXECUTED" TO RE-MARK.    NC2174.2
157000 STR-WRITE-GF-25-1.                                               NC2174.2
157100     PERFORM PRINT-DETAIL.                                        NC2174.2
157200*                                                                 NC2174.2
157300 STR-TEST-GF-25-2.                                                NC2174.2
157400     MOVE   "STR-TEST-GF-25-1" TO PAR-NAME.                       NC2174.2
157500     MOVE    1 TO REC-CT.                                         NC2174.2
157600     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
157700             PERFORM PASS                                         NC2174.2
157800             GO TO STR-WRITE-GF-25-2                              NC2174.2
157900     ELSE                                                         NC2174.2
158000             GO TO STR-FAIL-GF-25-2.                              NC2174.2
158100 STR-DELETE-GF-25-2.                                              NC2174.2
158200     PERFORM DE-LETE.                                             NC2174.2
158300     GO TO STR-WRITE-GF-25-2.                                     NC2174.2
158400 STR-FAIL-GF-25-2.                                                NC2174.2
158500     PERFORM FAIL                                                 NC2174.2
158600     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
158700     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
158800 STR-WRITE-GF-25-2.                                               NC2174.2
158900     PERFORM PRINT-DETAIL.                                        NC2174.2
159000*                                                                 NC2174.2
159100 STR-TEST-GF-25-3.                                                NC2174.2
159200     ADD     1 TO REC-CT.                                         NC2174.2
159300     IF      ID8-DU-2V0 = 6                                       NC2174.2
159400             PERFORM PASS                                         NC2174.2
159500             GO TO STR-WRITE-GF-25-3                              NC2174.2
159600     ELSE                                                         NC2174.2
159700             GO TO STR-FAIL-GF-25-3.                              NC2174.2
159800 STR-DELETE-GF-25-3.                                              NC2174.2
159900     PERFORM DE-LETE.                                             NC2174.2
160000     GO TO STR-WRITE-GF-25-3.                                     NC2174.2
160100 STR-FAIL-GF-25-3.                                                NC2174.2
160200     PERFORM FAIL                                                 NC2174.2
160300     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
160400     MOVE    6 TO CORRECT-N.                                      NC2174.2
160500 STR-WRITE-GF-25-3.                                               NC2174.2
160600     PERFORM PRINT-DETAIL.                                        NC2174.2
160700*                                                                 NC2174.2
160800 STR-INIT-GF-26.                                                  NC2174.2
160900*    ===-->  EXPLICIT SCOPE TERMINATOR  <--===                    NC2174.2
161000     MOVE   "VI-132 6.24.4 GR11" TO ANSI-REFERENCE.               NC2174.2
161100     MOVE   "STR-TEST-GF-26" TO PAR-NAME.                         NC2174.2
161200     MOVE   "LIT DEL BY LIT" TO FEATURE.                          NC2174.2
161300     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
161400     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
161500     MOVE    1 TO REC-CT.                                         NC2174.2
161600     MOVE    SPACE TO WRK-XN-00001-1.                             NC2174.2
161700     MOVE    SPACE TO WRK-XN-00001-2.                             NC2174.2
161800     MOVE    SPACE TO WRK-XN-00001-3.                             NC2174.2
161900*                                                                 NC2174.2
162000 STR-TEST-GF-26-0.                                                NC2174.2
162100     STRING "ABCDEF" DELIMITED BY "ABCDEFG"                       NC2174.2
162200             INTO ID7-XN-5 WITH POINTER ID8-DU-2V0                NC2174.2
162300             ON OVERFLOW                                          NC2174.2
162400             MOVE "A" TO WRK-XN-00001-1                           NC2174.2
162500             MOVE "B" TO WRK-XN-00001-2                           NC2174.2
162600         NOT ON OVERFLOW                                          NC2174.2
162700             MOVE "C" TO WRK-XN-00001-1                           NC2174.2
162800             MOVE "D" TO WRK-XN-00001-2                           NC2174.2
162900     END-STRING                                                   NC2174.2
163000     MOVE   "Z" TO WRK-XN-00001-3.                                NC2174.2
163100     GO TO   STR-TEST-GF-26-1.                                    NC2174.2
163200 STR-DELETE-GF-26.                                                NC2174.2
163300     PERFORM DE-LETE.                                             NC2174.2
163400     PERFORM PRINT-DETAIL.                                        NC2174.2
163500     GO      TO STR-INIT-GF-27.                                   NC2174.2
163600*                                                                 NC2174.2
163700 STR-TEST-GF-26-1.                                                NC2174.2
163800     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
163900             PERFORM PASS                                         NC2174.2
164000             GO TO STR-WRITE-GF-26-1                              NC2174.2
164100     ELSE                                                         NC2174.2
164200             GO TO STR-FAIL-GF-26-1.                              NC2174.2
164300 STR-DELETE-GF-26-1.                                              NC2174.2
164400     PERFORM DE-LETE.                                             NC2174.2
164500     GO TO STR-WRITE-GF-26-1.                                     NC2174.2
164600 STR-FAIL-GF-26-1.                                                NC2174.2
164700     PERFORM FAIL                                                 NC2174.2
164800     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
164900     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
165000 STR-WRITE-GF-26-1.                                               NC2174.2
165100     PERFORM PRINT-DETAIL.                                        NC2174.2
165200*                                                                 NC2174.2
165300 STR-TEST-GF-26-2.                                                NC2174.2
165400     ADD     1 TO REC-CT.                                         NC2174.2
165500     MOVE   "STR-TEST-GF-26-2" TO PAR-NAME.                       NC2174.2
165600     IF      ID8-DU-2V0 = 6                                       NC2174.2
165700             PERFORM PASS                                         NC2174.2
165800             GO TO STR-WRITE-GF-26-2                              NC2174.2
165900     ELSE                                                         NC2174.2
166000             GO TO STR-FAIL-GF-26-2.                              NC2174.2
166100 STR-DELETE-GF-26-2.                                              NC2174.2
166200     PERFORM DE-LETE.                                             NC2174.2
166300     GO TO STR-WRITE-GF-26-2.                                     NC2174.2
166400 STR-FAIL-GF-26-2.                                                NC2174.2
166500     PERFORM FAIL                                                 NC2174.2
166600     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
166700     MOVE    6 TO CORRECT-N.                                      NC2174.2
166800 STR-WRITE-GF-26-2.                                               NC2174.2
166900     PERFORM PRINT-DETAIL.                                        NC2174.2
167000*                                                                 NC2174.2
167100 STR-TEST-GF-26-3.                                                NC2174.2
167200     ADD     1 TO REC-CT.                                         NC2174.2
167300     IF      WRK-XN-00001-1 = "A"                                 NC2174.2
167400             PERFORM PASS                                         NC2174.2
167500             GO TO STR-WRITE-GF-26-3                              NC2174.2
167600     ELSE                                                         NC2174.2
167700             GO TO STR-FAIL-GF-26-3.                              NC2174.2
167800 STR-DELETE-GF-26-3.                                              NC2174.2
167900     PERFORM DE-LETE.                                             NC2174.2
168000     GO TO STR-WRITE-GF-26-3.                                     NC2174.2
168100 STR-FAIL-GF-26-3.                                                NC2174.2
168200     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2174.2
168300     MOVE   "A" TO CORRECT-X                                      NC2174.2
168400     MOVE   "ON OVERFLOW SHOULD HAVE EXECUTED" TO RE-MARK         NC2174.2
168500     PERFORM FAIL.                                                NC2174.2
168600 STR-WRITE-GF-26-3.                                               NC2174.2
168700     PERFORM PRINT-DETAIL.                                        NC2174.2
168800*                                                                 NC2174.2
168900 STR-TEST-GF-26-4.                                                NC2174.2
169000     ADD     1 TO REC-CT.                                         NC2174.2
169100     IF      WRK-XN-00001-2 = "B"                                 NC2174.2
169200             PERFORM PASS                                         NC2174.2
169300             GO TO STR-WRITE-GF-26-4                              NC2174.2
169400     ELSE                                                         NC2174.2
169500             GO TO STR-FAIL-GF-26-4.                              NC2174.2
169600 STR-DELETE-GF-26-4.                                              NC2174.2
169700     PERFORM DE-LETE.                                             NC2174.2
169800     GO TO STR-WRITE-GF-26-4.                                     NC2174.2
169900 STR-FAIL-GF-26-4.                                                NC2174.2
170000     MOVE    WRK-XN-00001-2 TO COMPUTED-X                         NC2174.2
170100     MOVE   "B" TO CORRECT-X                                      NC2174.2
170200     MOVE   "ON OVERFLOW SHOULD HAVE EXECUTED" TO RE-MARK         NC2174.2
170300     PERFORM FAIL.                                                NC2174.2
170400 STR-WRITE-GF-26-4.                                               NC2174.2
170500     PERFORM PRINT-DETAIL.                                        NC2174.2
170600*                                                                 NC2174.2
170700 STR-TEST-GF-26-5.                                                NC2174.2
170800     ADD     1 TO REC-CT.                                         NC2174.2
170900     IF      WRK-XN-00001-3 = "Z"                                 NC2174.2
171000             PERFORM PASS                                         NC2174.2
171100             GO TO STR-WRITE-GF-26-5                              NC2174.2
171200     ELSE                                                         NC2174.2
171300             GO TO STR-FAIL-GF-26-5.                              NC2174.2
171400 STR-DELETE-GF-26-5.                                              NC2174.2
171500     PERFORM DE-LETE.                                             NC2174.2
171600     GO TO STR-WRITE-GF-26-5.                                     NC2174.2
171700 STR-FAIL-GF-26-5.                                                NC2174.2
171800     MOVE    WRK-XN-00001-3 TO COMPUTED-X                         NC2174.2
171900     MOVE   "Z" TO CORRECT-X                                      NC2174.2
172000     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2174.2
172100     PERFORM FAIL.                                                NC2174.2
172200 STR-WRITE-GF-26-5.                                               NC2174.2
172300     PERFORM PRINT-DETAIL.                                        NC2174.2
172400*                                                                 NC2174.2
172500 STR-INIT-GF-27.                                                  NC2174.2
172600*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2174.2
172700     MOVE   "VI-132 6.24.4 GR9, 10" TO ANSI-REFERENCE.            NC2174.2
172800     MOVE   "STR-TEST-GF-27" TO PAR-NAME.                         NC2174.2
172900     MOVE   "LIT DEL BY LIT" TO FEATURE.                          NC2174.2
173000     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
173100     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
173200     MOVE    ZERO TO REC-CT.                                      NC2174.2
173300     MOVE    SPACE TO WRK-XN-00001-1.                             NC2174.2
173400     MOVE    SPACE TO WRK-XN-00001-2.                             NC2174.2
173500     MOVE    SPACE TO WRK-XN-00001-3.                             NC2174.2
173600*                                                                 NC2174.2
173700 STR-TEST-GF-27-0.                                                NC2174.2
173800     STRING "ABCDEF" DELIMITED BY "ABCDEFG"                       NC2174.2
173900             INTO ID7-XN-5 WITH POINTER ID8-DU-2V0                NC2174.2
174000             ON OVERFLOW                                          NC2174.2
174100             MOVE "A" TO WRK-XN-00001-1                           NC2174.2
174200             MOVE "B" TO WRK-XN-00001-2                           NC2174.2
174300     END-STRING                                                   NC2174.2
174400     MOVE   "Z" TO WRK-XN-00001-3.                                NC2174.2
174500     GO TO   STR-TEST-GF-27-1.                                    NC2174.2
174600 STR-DELETE-GF-27.                                                NC2174.2
174700     PERFORM DE-LETE.                                             NC2174.2
174800     PERFORM PRINT-DETAIL.                                        NC2174.2
174900     GO TO   STR-INIT-GF-28.                                      NC2174.2
175000*                                                                 NC2174.2
175100 STR-TEST-GF-27-1.                                                NC2174.2
175200     MOVE    1 TO REC-CT.                                         NC2174.2
175300     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
175400             PERFORM PASS                                         NC2174.2
175500             GO TO STR-WRITE-GF-27-1                              NC2174.2
175600     ELSE                                                         NC2174.2
175700             GO TO STR-FAIL-GF-27-1.                              NC2174.2
175800 STR-DELETE-GF-27-1.                                              NC2174.2
175900     PERFORM DE-LETE.                                             NC2174.2
176000     GO TO STR-WRITE-GF-27-1.                                     NC2174.2
176100 STR-FAIL-GF-27-1.                                                NC2174.2
176200     PERFORM FAIL                                                 NC2174.2
176300     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
176400     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
176500 STR-WRITE-GF-27-1.                                               NC2174.2
176600     PERFORM PRINT-DETAIL.                                        NC2174.2
176700*                                                                 NC2174.2
176800 STR-TEST-GF-27-2.                                                NC2174.2
176900     ADD     1 TO REC-CT.                                         NC2174.2
177000     IF      ID8-DU-2V0 = 6                                       NC2174.2
177100             PERFORM PASS                                         NC2174.2
177200             GO TO STR-WRITE-GF-27-2                              NC2174.2
177300     ELSE                                                         NC2174.2
177400             GO TO STR-FAIL-GF-27-2.                              NC2174.2
177500 STR-DELETE-GF-27-2.                                              NC2174.2
177600     PERFORM DE-LETE.                                             NC2174.2
177700     GO TO STR-WRITE-GF-27-2.                                     NC2174.2
177800 STR-FAIL-GF-27-2.                                                NC2174.2
177900             PERFORM FAIL                                         NC2174.2
178000             MOVE    ID8-DU-2V0 TO COMPUTED-N                     NC2174.2
178100             MOVE    6 TO CORRECT-N.                              NC2174.2
178200 STR-WRITE-GF-27-2.                                               NC2174.2
178300             PERFORM PRINT-DETAIL.                                NC2174.2
178400*                                                                 NC2174.2
178500 STR-TEST-GF-27-3.                                                NC2174.2
178600     ADD     1 TO REC-CT.                                         NC2174.2
178700     IF      WRK-XN-00001-1 = "A"                                 NC2174.2
178800             PERFORM PASS                                         NC2174.2
178900             GO TO STR-WRITE-GF-27-3                              NC2174.2
179000     ELSE                                                         NC2174.2
179100             GO TO STR-FAIL-GF-27-3.                              NC2174.2
179200 STR-DELETE-GF-27-3.                                              NC2174.2
179300     PERFORM DE-LETE.                                             NC2174.2
179400     GO TO STR-WRITE-GF-27-3.                                     NC2174.2
179500 STR-FAIL-GF-27-3.                                                NC2174.2
179600     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2174.2
179700     MOVE   "A"    TO CORRECT-X                                   NC2174.2
179800     MOVE   "ON OVERFLOW SHOULD HAVE EXECUTED" TO RE-MARK         NC2174.2
179900     PERFORM FAIL.                                                NC2174.2
180000 STR-WRITE-GF-27-3.                                               NC2174.2
180100     PERFORM PRINT-DETAIL.                                        NC2174.2
180200*                                                                 NC2174.2
180300 STR-TEST-GF-27-4.                                                NC2174.2
180400     ADD     1 TO REC-CT.                                         NC2174.2
180500     IF      WRK-XN-00001-2 = "B"                                 NC2174.2
180600             PERFORM PASS                                         NC2174.2
180700             GO TO STR-WRITE-GF-27-4                              NC2174.2
180800     ELSE                                                         NC2174.2
180900             GO TO STR-FAIL-GF-27-4.                              NC2174.2
181000 STR-DELETE-GF-27-4.                                              NC2174.2
181100     PERFORM DE-LETE.                                             NC2174.2
181200     GO TO STR-WRITE-GF-27-4.                                     NC2174.2
181300 STR-FAIL-GF-27-4.                                                NC2174.2
181400     MOVE    WRK-XN-00001-2 TO COMPUTED-X                         NC2174.2
181500     MOVE   "B" TO CORRECT-X                                      NC2174.2
181600     MOVE   "ON OVERFLOW SHOULD HAVE EXECUTED" TO RE-MARK         NC2174.2
181700     PERFORM FAIL.                                                NC2174.2
181800 STR-WRITE-GF-27-4.                                               NC2174.2
181900     PERFORM PRINT-DETAIL.                                        NC2174.2
182000*                                                                 NC2174.2
182100 STR-TEST-GF-27-5.                                                NC2174.2
182200     ADD     1 TO REC-CT.                                         NC2174.2
182300     IF      WRK-XN-00001-3 = "Z"                                 NC2174.2
182400             PERFORM PASS                                         NC2174.2
182500             GO TO STR-WRITE-GF-27-5                              NC2174.2
182600     ELSE                                                         NC2174.2
182700             GO TO STR-FAIL-GF-27-5.                              NC2174.2
182800 STR-DELETE-GF-27-5.                                              NC2174.2
182900     PERFORM DE-LETE.                                             NC2174.2
183000     GO TO STR-WRITE-GF-27-5.                                     NC2174.2
183100 STR-FAIL-GF-27-5.                                                NC2174.2
183200     MOVE    WRK-XN-00001-3 TO COMPUTED-X                         NC2174.2
183300     MOVE   "Z" TO CORRECT-X                                      NC2174.2
183400     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2174.2
183500     PERFORM FAIL.                                                NC2174.2
183600 STR-WRITE-GF-27-5.                                               NC2174.2
183700     PERFORM PRINT-DETAIL.                                        NC2174.2
183800*                                                                 NC2174.2
183900 STR-INIT-GF-28.                                                  NC2174.2
184000*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2174.2
184100     MOVE   "VI-132 6.24.4 GR9, 10" TO ANSI-REFERENCE.            NC2174.2
184200     MOVE   "STR-TEST-GF-28" TO PAR-NAME.                         NC2174.2
184300     MOVE   "LITERAL SERIES" TO FEATURE.                          NC2174.2
184400     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
184500     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
184600     MOVE    ZERO TO REC-CT.                                      NC2174.2
184700     MOVE    SPACE TO WRK-XN-00001-1.                             NC2174.2
184800     MOVE    SPACE TO WRK-XN-00001-2.                             NC2174.2
184900     MOVE    SPACE TO WRK-XN-00001-3.                             NC2174.2
185000*                                                                 NC2174.2
185100 STR-TEST-GF-28-0.                                                NC2174.2
185200     STRING "A" "B" "C" "D" "E" DELIMITED "ABCDE" INTO ID7-XN-5   NC2174.2
185300             POINTER ID8-DU-2V0                                   NC2174.2
185400             ON OVERFLOW                                          NC2174.2
185500             MOVE "A" TO WRK-XN-00001-1                           NC2174.2
185600             MOVE "B" TO WRK-XN-00001-2                           NC2174.2
185700     END-STRING                                                   NC2174.2
185800     MOVE   "Z" TO WRK-XN-00001-3.                                NC2174.2
185900     GO TO   STR-TEST-GF-28-1.                                    NC2174.2
186000 STR-DELETE-GF-28.                                                NC2174.2
186100     PERFORM DE-LETE.                                             NC2174.2
186200     PERFORM PRINT-DETAIL.                                        NC2174.2
186300     GO TO   STR-INIT-GF-29.                                      NC2174.2
186400*                                                                 NC2174.2
186500 STR-TEST-GF-28-1.                                                NC2174.2
186600     MOVE    1 TO REC-CT.                                         NC2174.2
186700     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
186800             PERFORM PASS                                         NC2174.2
186900             GO TO STR-WRITE-GF-28-1                              NC2174.2
187000     ELSE                                                         NC2174.2
187100             GO TO STR-FAIL-GF-28-1.                              NC2174.2
187200 STR-DELETE-GF-28-1.                                              NC2174.2
187300     PERFORM DE-LETE.                                             NC2174.2
187400     GO TO STR-WRITE-GF-28-1.                                     NC2174.2
187500 STR-FAIL-GF-28-1.                                                NC2174.2
187600     PERFORM FAIL                                                 NC2174.2
187700     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
187800     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
187900 STR-WRITE-GF-28-1.                                               NC2174.2
188000     PERFORM PRINT-DETAIL.                                        NC2174.2
188100*                                                                 NC2174.2
188200 STR-TEST-GF-28-2.                                                NC2174.2
188300     ADD     1 TO REC-CT.                                         NC2174.2
188400     IF      ID8-DU-2V0 = 6                                       NC2174.2
188500             PERFORM PASS                                         NC2174.2
188600             GO TO STR-WRITE-GF-28-2                              NC2174.2
188700     ELSE                                                         NC2174.2
188800             GO TO STR-FAIL-GF-28-2.                              NC2174.2
188900 STR-DELETE-GF-28-2.                                              NC2174.2
189000     PERFORM DE-LETE.                                             NC2174.2
189100     GO TO STR-WRITE-GF-28-2.                                     NC2174.2
189200 STR-FAIL-GF-28-2.                                                NC2174.2
189300     PERFORM FAIL                                                 NC2174.2
189400     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
189500     MOVE    6 TO CORRECT-N.                                      NC2174.2
189600 STR-WRITE-GF-28-2.                                               NC2174.2
189700     PERFORM PRINT-DETAIL.                                        NC2174.2
189800*                                                                 NC2174.2
189900 STR-TEST-GF-28-3.                                                NC2174.2
190000     ADD     1 TO REC-CT.                                         NC2174.2
190100     IF      WRK-XN-00001-1 = SPACE                               NC2174.2
190200             PERFORM PASS                                         NC2174.2
190300             GO TO STR-WRITE-GF-28-3                              NC2174.2
190400     ELSE                                                         NC2174.2
190500             GO TO STR-FAIL-GF-28-3.                              NC2174.2
190600 STR-DELETE-GF-28-3.                                              NC2174.2
190700     PERFORM DE-LETE.                                             NC2174.2
190800     GO TO STR-WRITE-GF-28-3.                                     NC2174.2
190900 STR-FAIL-GF-28-3.                                                NC2174.2
191000     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2174.2
191100     MOVE    SPACE TO CORRECT-X                                   NC2174.2
191200     MOVE   "ON OVERFLOW SHOULD NOT HAVE EXECUTED" TO RE-MARK     NC2174.2
191300     PERFORM FAIL.                                                NC2174.2
191400 STR-WRITE-GF-28-3.                                               NC2174.2
191500     PERFORM PRINT-DETAIL.                                        NC2174.2
191600*                                                                 NC2174.2
191700 STR-TEST-GF-28-4.                                                NC2174.2
191800     ADD     1 TO REC-CT.                                         NC2174.2
191900     IF      WRK-XN-00001-2 =  SPACE                              NC2174.2
192000             PERFORM PASS                                         NC2174.2
192100             GO TO STR-WRITE-GF-28-4                              NC2174.2
192200     ELSE                                                         NC2174.2
192300             GO TO STR-FAIL-GF-28-4.                              NC2174.2
192400 STR-DELETE-GF-28-4.                                              NC2174.2
192500     PERFORM DE-LETE.                                             NC2174.2
192600     GO TO STR-WRITE-GF-28-4.                                     NC2174.2
192700 STR-FAIL-GF-28-4.                                                NC2174.2
192800     MOVE    WRK-XN-00001-2 TO COMPUTED-X                         NC2174.2
192900     MOVE    SPACE TO CORRECT-X                                   NC2174.2
193000     MOVE   "ON OVERFLOW SHOULD NOT HAVE EXECUTED" TO RE-MARK     NC2174.2
193100     PERFORM FAIL.                                                NC2174.2
193200 STR-WRITE-GF-28-4.                                               NC2174.2
193300     PERFORM PRINT-DETAIL.                                        NC2174.2
193400*                                                                 NC2174.2
193500 STR-TEST-GF-28-5.                                                NC2174.2
193600     ADD     1 TO REC-CT.                                         NC2174.2
193700     IF      WRK-XN-00001-3 = "Z"                                 NC2174.2
193800             PERFORM PASS                                         NC2174.2
193900             GO TO STR-WRITE-GF-28-5                              NC2174.2
194000     ELSE                                                         NC2174.2
194100             GO TO STR-FAIL-GF-28-5.                              NC2174.2
194200 STR-DELETE-GF-28-5.                                              NC2174.2
194300     PERFORM DE-LETE.                                             NC2174.2
194400     GO TO STR-WRITE-GF-28-5.                                     NC2174.2
194500 STR-FAIL-GF-28-5.                                                NC2174.2
194600     MOVE    WRK-XN-00001-3 TO COMPUTED-X                         NC2174.2
194700     MOVE   "Z" TO CORRECT-X                                      NC2174.2
194800     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2174.2
194900     PERFORM FAIL.                                                NC2174.2
195000 STR-WRITE-GF-28-5.                                               NC2174.2
195100     PERFORM PRINT-DETAIL.                                        NC2174.2
195200*                                                                 NC2174.2
195300 STR-INIT-GF-29.                                                  NC2174.2
195400*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2174.2
195500     MOVE   "VI-132 6.24.4 GR9, 10" TO ANSI-REFERENCE.            NC2174.2
195600     MOVE   "STR-TEST-GF-29" TO PAR-NAME.                         NC2174.2
195700     MOVE   "LIT DEL BY LIT" TO FEATURE.                          NC2174.2
195800     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
195900     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
196000     MOVE    ZERO TO REC-CT.                                      NC2174.2
196100     MOVE    SPACE TO WRK-XN-00001-1.                             NC2174.2
196200     MOVE    SPACE TO WRK-XN-00001-2.                             NC2174.2
196300     MOVE    SPACE TO WRK-XN-00001-3.                             NC2174.2
196400*                                                                 NC2174.2
196500 STR-TEST-GF-29.                                                  NC2174.2
196600     STRING "ABCDEF" DELIMITED BY "ABCDEFG"                       NC2174.2
196700             INTO ID7-XN-5 WITH POINTER ID8-DU-2V0                NC2174.2
196800         NOT ON OVERFLOW                                          NC2174.2
196900             MOVE "A" TO WRK-XN-00001-1                           NC2174.2
197000             MOVE "B" TO WRK-XN-00001-2                           NC2174.2
197100     END-STRING                                                   NC2174.2
197200     MOVE   "Z" TO WRK-XN-00001-3.                                NC2174.2
197300     GO TO   STR-TEST-GF-29-1.                                    NC2174.2
197400 STR-DELETE-GF-29.                                                NC2174.2
197500     PERFORM DE-LETE.                                             NC2174.2
197600     PERFORM PRINT-DETAIL.                                        NC2174.2
197700     GO TO   STR-INIT-GF-30.                                      NC2174.2
197800*                                                                 NC2174.2
197900 STR-TEST-GF-29-1.                                                NC2174.2
198000     MOVE    1 TO REC-CT.                                         NC2174.2
198100     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
198200             PERFORM PASS                                         NC2174.2
198300             GO TO STR-WRITE-GF-29-1                              NC2174.2
198400     ELSE                                                         NC2174.2
198500             GO TO STR-FAIL-GF-29-1.                              NC2174.2
198600 STR-DELETE-GF-29-1.                                              NC2174.2
198700     PERFORM DE-LETE.                                             NC2174.2
198800     GO TO STR-WRITE-GF-29-1.                                     NC2174.2
198900 STR-FAIL-GF-29-1.                                                NC2174.2
199000     PERFORM FAIL                                                 NC2174.2
199100     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
199200     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
199300 STR-WRITE-GF-29-1.                                               NC2174.2
199400     PERFORM PRINT-DETAIL.                                        NC2174.2
199500*                                                                 NC2174.2
199600 STR-TEST-GF-29-2.                                                NC2174.2
199700     ADD     1 TO REC-CT.                                         NC2174.2
199800     IF      ID8-DU-2V0 = 6                                       NC2174.2
199900             PERFORM PASS                                         NC2174.2
200000             GO TO STR-WRITE-GF-29-2                              NC2174.2
200100     ELSE                                                         NC2174.2
200200             GO TO STR-FAIL-GF-29-2.                              NC2174.2
200300 STR-DELETE-GF-29-2.                                              NC2174.2
200400     PERFORM DE-LETE.                                             NC2174.2
200500     GO TO STR-WRITE-GF-29-2.                                     NC2174.2
200600 STR-FAIL-GF-29-2.                                                NC2174.2
200700     PERFORM FAIL                                                 NC2174.2
200800     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
200900     MOVE    6 TO CORRECT-N.                                      NC2174.2
201000 STR-WRITE-GF-29-2.                                               NC2174.2
201100     PERFORM PRINT-DETAIL.                                        NC2174.2
201200*                                                                 NC2174.2
201300 STR-TEST-GF-29-3.                                                NC2174.2
201400     ADD     1 TO REC-CT.                                         NC2174.2
201500     IF      WRK-XN-00001-1 = SPACE                               NC2174.2
201600             PERFORM PASS                                         NC2174.2
201700             GO TO STR-WRITE-GF-29-3                              NC2174.2
201800     ELSE                                                         NC2174.2
201900             GO TO STR-FAIL-GF-29-3.                              NC2174.2
202000 STR-DELETE-GF-29-3.                                              NC2174.2
202100     PERFORM DE-LETE.                                             NC2174.2
202200     GO TO STR-WRITE-GF-29-3.                                     NC2174.2
202300 STR-FAIL-GF-29-3.                                                NC2174.2
202400     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2174.2
202500     MOVE    SPACE TO CORRECT-X                                   NC2174.2
202600     MOVE   "NOT ON OVERFLOW SHOULD NOT HAVE EXECUTED"            NC2174.2
202700          TO RE-MARK                                              NC2174.2
202800     PERFORM FAIL.                                                NC2174.2
202900 STR-WRITE-GF-29-3.                                               NC2174.2
203000     PERFORM PRINT-DETAIL.                                        NC2174.2
203100*                                                                 NC2174.2
203200 STR-TEST-GF-29-4.                                                NC2174.2
203300     ADD     1 TO REC-CT.                                         NC2174.2
203400     IF      WRK-XN-00001-2 = SPACE                               NC2174.2
203500             PERFORM PASS                                         NC2174.2
203600             GO TO STR-WRITE-GF-29-4                              NC2174.2
203700     ELSE                                                         NC2174.2
203800             GO TO STR-FAIL-GF-29-4.                              NC2174.2
203900 STR-DELETE-GF-29-4.                                              NC2174.2
204000     PERFORM DE-LETE.                                             NC2174.2
204100     GO TO STR-WRITE-GF-29-4.                                     NC2174.2
204200 STR-FAIL-GF-29-4.                                                NC2174.2
204300     MOVE    WRK-XN-00001-2 TO COMPUTED-X                         NC2174.2
204400     MOVE    SPACE TO CORRECT-X                                   NC2174.2
204500     MOVE   "NOT ON OVERFLOW SHOULD NOT HAVE EXECUTED"            NC2174.2
204600          TO RE-MARK                                              NC2174.2
204700     PERFORM FAIL.                                                NC2174.2
204800 STR-WRITE-GF-29-4.                                               NC2174.2
204900     PERFORM PRINT-DETAIL.                                        NC2174.2
205000*                                                                 NC2174.2
205100 STR-TEST-GF-29-5.                                                NC2174.2
205200     ADD     1 TO REC-CT.                                         NC2174.2
205300     IF      WRK-XN-00001-3 = "Z"                                 NC2174.2
205400             PERFORM PASS                                         NC2174.2
205500             GO TO STR-WRITE-GF-29-5                              NC2174.2
205600     ELSE                                                         NC2174.2
205700             GO TO STR-FAIL-GF-29-5.                              NC2174.2
205800 STR-DELETE-GF-29-5.                                              NC2174.2
205900     PERFORM DE-LETE.                                             NC2174.2
206000     GO TO STR-WRITE-GF-29-5.                                     NC2174.2
206100 STR-FAIL-GF-29-5.                                                NC2174.2
206200     MOVE    WRK-XN-00001-3 TO COMPUTED-X                         NC2174.2
206300     MOVE   "Z" TO CORRECT-X                                      NC2174.2
206400     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2174.2
206500     PERFORM FAIL.                                                NC2174.2
206600 STR-WRITE-GF-29-5.                                               NC2174.2
206700     PERFORM PRINT-DETAIL.                                        NC2174.2
206800*                                                                 NC2174.2
206900 STR-INIT-GF-30.                                                  NC2174.2
207000*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2174.2
207100     MOVE   "VI-132 6.24.4 GR9, 10" TO ANSI-REFERENCE.            NC2174.2
207200     MOVE   "STR-TEST-GF-30" TO PAR-NAME.                         NC2174.2
207300     MOVE   "LITERAL SERIES" TO FEATURE.                          NC2174.2
207400     MOVE    "*****" TO ID7-XN-5.                                 NC2174.2
207500     MOVE    1 TO ID8-DU-2V0.                                     NC2174.2
207600     MOVE    ZERO TO REC-CT.                                      NC2174.2
207700     MOVE    SPACE TO WRK-XN-00001-1.                             NC2174.2
207800     MOVE    SPACE TO WRK-XN-00001-2.                             NC2174.2
207900     MOVE    SPACE TO WRK-XN-00001-3.                             NC2174.2
208000*                                                                 NC2174.2
208100 STR-TEST-GF-30.                                                  NC2174.2
208200     STRING "A" "B" "C" "D" "E" DELIMITED "ABCDE" INTO ID7-XN-5   NC2174.2
208300             POINTER ID8-DU-2V0                                   NC2174.2
208400         NOT ON OVERFLOW                                          NC2174.2
208500             MOVE "A" TO WRK-XN-00001-1                           NC2174.2
208600             MOVE "B" TO WRK-XN-00001-2                           NC2174.2
208700     END-STRING                                                   NC2174.2
208800     MOVE   "Z" TO WRK-XN-00001-3.                                NC2174.2
208900     GO TO   STR-TEST-GF-30-1.                                    NC2174.2
209000 STR-DELETE-GF-30.                                                NC2174.2
209100     PERFORM DE-LETE.                                             NC2174.2
209200     PERFORM PRINT-DETAIL.                                        NC2174.2
209300     GO TO   CCVS-EXIT.                                           NC2174.2
209400*                                                                 NC2174.2
209500 STR-TEST-GF-30-1.                                                NC2174.2
209600     MOVE    1 TO REC-CT.                                         NC2174.2
209700     IF      ID7-XN-5 = "ABCDE"                                   NC2174.2
209800             PERFORM PASS                                         NC2174.2
209900             GO TO STR-WRITE-GF-30-1                              NC2174.2
210000     ELSE                                                         NC2174.2
210100             GO TO STR-FAIL-GF-30-1.                              NC2174.2
210200 STR-DELETE-GF-30-1.                                              NC2174.2
210300     PERFORM DE-LETE.                                             NC2174.2
210400     GO TO STR-WRITE-GF-30-1.                                     NC2174.2
210500 STR-FAIL-GF-30-1.                                                NC2174.2
210600     PERFORM FAIL                                                 NC2174.2
210700     MOVE    ID7-XN-5 TO COMPUTED-A                               NC2174.2
210800     MOVE   "ABCDE" TO CORRECT-A.                                 NC2174.2
210900 STR-WRITE-GF-30-1.                                               NC2174.2
211000     PERFORM PRINT-DETAIL.                                        NC2174.2
211100*                                                                 NC2174.2
211200 STR-TEST-GF-30-2.                                                NC2174.2
211300     ADD     1 TO REC-CT.                                         NC2174.2
211400     IF      ID8-DU-2V0 = 6                                       NC2174.2
211500             PERFORM PASS                                         NC2174.2
211600             GO TO STR-WRITE-GF-30-2                              NC2174.2
211700     ELSE                                                         NC2174.2
211800             GO TO STR-FAIL-GF-30-2.                              NC2174.2
211900 STR-DELETE-GF-30-2.                                              NC2174.2
212000     PERFORM DE-LETE.                                             NC2174.2
212100     GO TO STR-WRITE-GF-30-2.                                     NC2174.2
212200 STR-FAIL-GF-30-2.                                                NC2174.2
212300     PERFORM FAIL                                                 NC2174.2
212400     MOVE    ID8-DU-2V0 TO COMPUTED-N                             NC2174.2
212500     MOVE    6 TO CORRECT-N.                                      NC2174.2
212600 STR-WRITE-GF-30-2.                                               NC2174.2
212700     PERFORM PRINT-DETAIL.                                        NC2174.2
212800*                                                                 NC2174.2
212900 STR-TEST-GF-30-3.                                                NC2174.2
213000     ADD     1 TO REC-CT.                                         NC2174.2
213100     IF      WRK-XN-00001-1 = "A"                                 NC2174.2
213200             PERFORM PASS                                         NC2174.2
213300             GO TO STR-WRITE-GF-30-3                              NC2174.2
213400     ELSE                                                         NC2174.2
213500             GO TO STR-FAIL-GF-30-3.                              NC2174.2
213600 STR-DELETE-GF-30-3.                                              NC2174.2
213700     PERFORM DE-LETE.                                             NC2174.2
213800     GO TO STR-WRITE-GF-30-3.                                     NC2174.2
213900 STR-FAIL-GF-30-3.                                                NC2174.2
214000     MOVE    WRK-XN-00001-1 TO COMPUTED-X                         NC2174.2
214100     MOVE   "A"    TO CORRECT-X                                   NC2174.2
214200     MOVE   "NOT ON OVERFLOW SHOULD HAVE EXECUTED"                NC2174.2
214300          TO RE-MARK                                              NC2174.2
214400     PERFORM FAIL.                                                NC2174.2
214500 STR-WRITE-GF-30-3.                                               NC2174.2
214600     PERFORM PRINT-DETAIL.                                        NC2174.2
214700*                                                                 NC2174.2
214800 STR-TEST-GF-30-4.                                                NC2174.2
214900     ADD     1 TO REC-CT.                                         NC2174.2
215000     IF      WRK-XN-00001-2 =  "B"                                NC2174.2
215100             PERFORM PASS                                         NC2174.2
215200             GO TO STR-WRITE-GF-30-4                              NC2174.2
215300     ELSE                                                         NC2174.2
215400             GO TO STR-FAIL-GF-30-4.                              NC2174.2
215500 STR-DELETE-GF-30-4.                                              NC2174.2
215600     PERFORM DE-LETE.                                             NC2174.2
215700     GO TO STR-WRITE-GF-30-4.                                     NC2174.2
215800 STR-FAIL-GF-30-4.                                                NC2174.2
215900     MOVE    WRK-XN-00001-2 TO COMPUTED-X                         NC2174.2
216000     MOVE   "B"    TO CORRECT-X                                   NC2174.2
216100     MOVE   "NOT ON OVERFLOW SHOULD HAVE EXECUTED"                NC2174.2
216200          TO RE-MARK                                              NC2174.2
216300     PERFORM FAIL.                                                NC2174.2
216400 STR-WRITE-GF-30-4.                                               NC2174.2
216500     PERFORM PRINT-DETAIL.                                        NC2174.2
216600*                                                                 NC2174.2
216700 STR-TEST-GF-30-5.                                                NC2174.2
216800     ADD     1 TO REC-CT.                                         NC2174.2
216900     IF      WRK-XN-00001-3 = "Z"                                 NC2174.2
217000             PERFORM PASS                                         NC2174.2
217100             GO TO STR-WRITE-GF-30-5                              NC2174.2
217200     ELSE                                                         NC2174.2
217300             GO TO STR-FAIL-GF-30-5.                              NC2174.2
217400 STR-DELETE-GF-30-5.                                              NC2174.2
217500     PERFORM DE-LETE.                                             NC2174.2
217600     GO TO STR-WRITE-GF-30-5.                                     NC2174.2
217700 STR-FAIL-GF-30-5.                                                NC2174.2
217800     MOVE    WRK-XN-00001-3 TO COMPUTED-X                         NC2174.2
217900     MOVE   "Z" TO CORRECT-X                                      NC2174.2
218000     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2174.2
218100     PERFORM FAIL.                                                NC2174.2
218200 STR-WRITE-GF-30-5.                                               NC2174.2
218300     PERFORM PRINT-DETAIL.                                        NC2174.2
218400*                                                                 NC2174.2
218500 CCVS-EXIT SECTION.                                               NC2174.2
218600 CCVS-999999.                                                     NC2174.2
218700     GO TO CLOSE-FILES.                                           NC2174.2
      *END-OF,NC217A                                                                  
