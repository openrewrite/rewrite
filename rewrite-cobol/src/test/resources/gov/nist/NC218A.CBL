      *HEADER,COBOL,NC218A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2184.2
000200 PROGRAM-ID.                                                      NC2184.2
000300     NC218A.                                                      NC2184.2
000400****************************************************************  NC2184.2
000500*                                                              *  NC2184.2
000600*    VALIDATION FOR:-                                          *  NC2184.2
000700*                                                              *  NC2184.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2184.2
000900*                                                              *  NC2184.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2184.2
001100*                                                              *  NC2184.2
001200****************************************************************  NC2184.2
001300*                                                              *  NC2184.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2184.2
001500*                                                              *  NC2184.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2184.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2184.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2184.2
001900*                                                              *  NC2184.2
002000****************************************************************  NC2184.2
002100*                                                              *  NC2184.2
002200*    PROGRAM NC218A TESTS TYHE USE OF THE "UNSTRING" STATEMENT *  NC2184.2
002300*    INCLUDING THE OPTIONAL PHRASES "POINTER", "TALLYING",     *  NC2184.2
002400*    "OVERFLOW", "NOT OVERFLOW" AND "END-STRING".              *  NC2184.2
002500*                                                              *  NC2184.2
002600****************************************************************  NC2184.2
002700 ENVIRONMENT DIVISION.                                            NC2184.2
002800 CONFIGURATION SECTION.                                           NC2184.2
002900 SOURCE-COMPUTER.                                                 NC2184.2
003000     XXXXX082.                                                    NC2184.2
003100 OBJECT-COMPUTER.                                                 NC2184.2
003200     XXXXX083.                                                    NC2184.2
003300 INPUT-OUTPUT SECTION.                                            NC2184.2
003400 FILE-CONTROL.                                                    NC2184.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC2184.2
003600     XXXXX055.                                                    NC2184.2
003700 DATA DIVISION.                                                   NC2184.2
003800 FILE SECTION.                                                    NC2184.2
003900 FD  PRINT-FILE.                                                  NC2184.2
004000 01  PRINT-REC PICTURE X(120).                                    NC2184.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC2184.2
004200 WORKING-STORAGE SECTION.                                         NC2184.2
004300 01  WRK-XN-00001-1       PIC X.                                  NC2184.2
004400 01  WRK-XN-00001-2       PIC X.                                  NC2184.2
004500 01  WRK-XN-00001-3       PIC X.                                  NC2184.2
004600 01  ZERO-XN-1 PIC X VALUE "0".                                   NC2184.2
004700 01  GRP1-XN-6 PIC X(6) VALUE "ABCDEF".                           NC2184.2
004800 01  ID1-XN-7 PIC X(7) VALUE "1200000".                           NC2184.2
004900 01  GRP1-XN-7 PIC X(7) VALUE "ABCDEFG".                          NC2184.2
005000 01  GRP1-XN-10 PIC X(10) VALUE "ABCDEFGHIJ".                     NC2184.2
005100 01  ID1-XN-12 PIC X(12) VALUE "ABCDEFGHIJKL".                    NC2184.2
005200 01  GRP1-XN-36 PIC X(36) VALUE                                   NC2184.2
005300     "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".                      NC2184.2
005400 01  GRP1-XN-X-36 REDEFINES GRP1-XN-36.                           NC2184.2
005500     10 ID1 PIC X(6) OCCURS 6 TIMES.                              NC2184.2
005600 01  GRP2-XN-2 PIC XX VALUE "CE".                                 NC2184.2
005700 01  GRP2-XN-X-2 REDEFINES GRP2-XN-2.                             NC2184.2
005800     10 ID2A PIC X OCCURS 2 TIMES.                                NC2184.2
005900 01  GRP2-XN-7 PIC X(7) VALUE "BCDEFGH".                          NC2184.2
006000 01  GRP2-XN-X-7 REDEFINES GRP2-XN-7.                             NC2184.2
006100     10 ID2 PIC X OCCURS 7 TIMES.                                 NC2184.2
006200 01  ID4-X PIC X VALUE SPACE.                                     NC2184.2
006300 01  ID4-XJ PIC X JUSTIFIED RIGHT VALUE SPACE.                    NC2184.2
006400 01  ID4-XXX  PIC XXX VALUE SPACES.                               NC2184.2
006500 01  ID4-XXXJ PIC XXX JUST RIGHT VALUE SPACES.                    NC2184.2
006600 01  ID4-DU-1V0 PIC 9 VALUE ZERO.                                 NC2184.2
006700 01  ID4-DS-1V0 PIC S9 VALUE ZERO.                                NC2184.2
006800 01  ID4-DU-2V0 PIC 99 VALUE ZERO.                                NC2184.2
006900 01  ID4-DS-2V0 PIC S99 VALUE ZERO.                               NC2184.2
007000 01  ID4-DS-TS-1V0 PIC S9 TRAILING VALUE ZERO.                    NC2184.2
007100 01  ID4-DS-LS-1V0 PIC S9 LEADING VALUE ZERO.                     NC2184.2
007200 01  GRP4-XN-6.                                                   NC2184.2
007300     10 ID4A-XXXXX PIC X(5).                                      NC2184.2
007400     10 ID4B-X PIC X.                                             NC2184.2
007500 01  ID4C-XXXX PIC X(4) VALUE SPACES.                             NC2184.2
007600 01  ID4D-X PIC X VALUE SPACE.                                    NC2184.2
007700 01  GRP4-XN-10.                                                  NC2184.2
007800     10 ID4A-X PIC X.                                             NC2184.2
007900     10 ID4B-XX PIC XX.                                           NC2184.2
008000     10 ID4C-XXX PIC XXX.                                         NC2184.2
008100     10 ID4D-XXXX PIC XXXX.                                       NC2184.2
008200 01  ASTER-XN-4 PIC X(4) VALUE "****".                            NC2184.2
008300 01  ID5-XN-4 PIC X(4) VALUE SPACES.                              NC2184.2
008400 01  ID5-XN-4-2 PIC X(4) VALUE SPACES.                            NC2184.2
008500 01  ID5-XN-6 PIC X(6) VALUE SPACES.                              NC2184.2
008600 01  ID6-DU-2V0 PIC 99 VALUE ZERO.                                NC2184.2
008700 01  ID6-DU-2V0-2  PIC 99 VALUE ZERO.                             NC2184.2
008800 01  ID10-DU-2V0 PIC 99 VALUE ZERO.                               NC2184.2
008900 01  ID11-DU-2V0 PIC 99 VALUE ZERO.                               NC2184.2
009000 01  TEST-RESULTS.                                                NC2184.2
009100     02 FILLER                   PIC X      VALUE SPACE.          NC2184.2
009200     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2184.2
009300     02 FILLER                   PIC X      VALUE SPACE.          NC2184.2
009400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2184.2
009500     02 FILLER                   PIC X      VALUE SPACE.          NC2184.2
009600     02  PAR-NAME.                                                NC2184.2
009700       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2184.2
009800       03  PARDOT-X              PIC X      VALUE SPACE.          NC2184.2
009900       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2184.2
010000     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2184.2
010100     02 RE-MARK                  PIC X(61).                       NC2184.2
010200 01  TEST-COMPUTED.                                               NC2184.2
010300     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2184.2
010400     02 FILLER                   PIC X(17)  VALUE                 NC2184.2
010500            "       COMPUTED=".                                   NC2184.2
010600     02 COMPUTED-X.                                               NC2184.2
010700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2184.2
010800     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2184.2
010900                                 PIC -9(9).9(9).                  NC2184.2
011000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2184.2
011100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2184.2
011200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2184.2
011300     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2184.2
011400         04 COMPUTED-18V0                    PIC -9(18).          NC2184.2
011500         04 FILLER                           PIC X.               NC2184.2
011600     03 FILLER PIC X(50) VALUE SPACE.                             NC2184.2
011700 01  TEST-CORRECT.                                                NC2184.2
011800     02 FILLER PIC X(30) VALUE SPACE.                             NC2184.2
011900     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2184.2
012000     02 CORRECT-X.                                                NC2184.2
012100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2184.2
012200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2184.2
012300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2184.2
012400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2184.2
012500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2184.2
012600     03      CR-18V0 REDEFINES CORRECT-A.                         NC2184.2
012700         04 CORRECT-18V0                     PIC -9(18).          NC2184.2
012800         04 FILLER                           PIC X.               NC2184.2
012900     03 FILLER PIC X(2) VALUE SPACE.                              NC2184.2
013000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2184.2
013100 01  CCVS-C-1.                                                    NC2184.2
013200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2184.2
013300-    "SS  PARAGRAPH-NAME                                          NC2184.2
013400-    "       REMARKS".                                            NC2184.2
013500     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2184.2
013600 01  CCVS-C-2.                                                    NC2184.2
013700     02 FILLER                     PIC X        VALUE SPACE.      NC2184.2
013800     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2184.2
013900     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2184.2
014000     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2184.2
014100     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2184.2
014200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2184.2
014300 01  REC-CT                        PIC 99       VALUE ZERO.       NC2184.2
014400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2184.2
014500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2184.2
014600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2184.2
014700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2184.2
014800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2184.2
014900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2184.2
015000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2184.2
015100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2184.2
015200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2184.2
015300 01  CCVS-H-1.                                                    NC2184.2
015400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2184.2
015500     02  FILLER                    PIC X(42)    VALUE             NC2184.2
015600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2184.2
015700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2184.2
015800 01  CCVS-H-2A.                                                   NC2184.2
015900   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2184.2
016000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2184.2
016100   02  FILLER                        PIC XXXX   VALUE             NC2184.2
016200     "4.2 ".                                                      NC2184.2
016300   02  FILLER                        PIC X(28)  VALUE             NC2184.2
016400            " COPY - NOT FOR DISTRIBUTION".                       NC2184.2
016500   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2184.2
016600                                                                  NC2184.2
016700 01  CCVS-H-2B.                                                   NC2184.2
016800   02  FILLER                        PIC X(15)  VALUE             NC2184.2
016900            "TEST RESULT OF ".                                    NC2184.2
017000   02  TEST-ID                       PIC X(9).                    NC2184.2
017100   02  FILLER                        PIC X(4)   VALUE             NC2184.2
017200            " IN ".                                               NC2184.2
017300   02  FILLER                        PIC X(12)  VALUE             NC2184.2
017400     " HIGH       ".                                              NC2184.2
017500   02  FILLER                        PIC X(22)  VALUE             NC2184.2
017600            " LEVEL VALIDATION FOR ".                             NC2184.2
017700   02  FILLER                        PIC X(58)  VALUE             NC2184.2
017800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2184.2
017900 01  CCVS-H-3.                                                    NC2184.2
018000     02  FILLER                      PIC X(34)  VALUE             NC2184.2
018100            " FOR OFFICIAL USE ONLY    ".                         NC2184.2
018200     02  FILLER                      PIC X(58)  VALUE             NC2184.2
018300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2184.2
018400     02  FILLER                      PIC X(28)  VALUE             NC2184.2
018500            "  COPYRIGHT   1985 ".                                NC2184.2
018600 01  CCVS-E-1.                                                    NC2184.2
018700     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2184.2
018800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2184.2
018900     02 ID-AGAIN                     PIC X(9).                    NC2184.2
019000     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2184.2
019100 01  CCVS-E-2.                                                    NC2184.2
019200     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2184.2
019300     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2184.2
019400     02 CCVS-E-2-2.                                               NC2184.2
019500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2184.2
019600         03 FILLER                   PIC X      VALUE SPACE.      NC2184.2
019700         03 ENDER-DESC               PIC X(44)  VALUE             NC2184.2
019800            "ERRORS ENCOUNTERED".                                 NC2184.2
019900 01  CCVS-E-3.                                                    NC2184.2
020000     02  FILLER                      PIC X(22)  VALUE             NC2184.2
020100            " FOR OFFICIAL USE ONLY".                             NC2184.2
020200     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2184.2
020300     02  FILLER                      PIC X(58)  VALUE             NC2184.2
020400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2184.2
020500     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2184.2
020600     02 FILLER                       PIC X(15)  VALUE             NC2184.2
020700             " COPYRIGHT 1985".                                   NC2184.2
020800 01  CCVS-E-4.                                                    NC2184.2
020900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2184.2
021000     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2184.2
021100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2184.2
021200     02 FILLER                       PIC X(40)  VALUE             NC2184.2
021300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2184.2
021400 01  XXINFO.                                                      NC2184.2
021500     02 FILLER                       PIC X(19)  VALUE             NC2184.2
021600            "*** INFORMATION ***".                                NC2184.2
021700     02 INFO-TEXT.                                                NC2184.2
021800       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2184.2
021900       04 XXCOMPUTED                 PIC X(20).                   NC2184.2
022000       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2184.2
022100       04 XXCORRECT                  PIC X(20).                   NC2184.2
022200     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2184.2
022300 01  HYPHEN-LINE.                                                 NC2184.2
022400     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2184.2
022500     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2184.2
022600-    "*****************************************".                 NC2184.2
022700     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2184.2
022800-    "******************************".                            NC2184.2
022900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2184.2
023000     "NC218A".                                                    NC2184.2
023100 PROCEDURE DIVISION.                                              NC2184.2
023200 CCVS1 SECTION.                                                   NC2184.2
023300 OPEN-FILES.                                                      NC2184.2
023400     OPEN     OUTPUT PRINT-FILE.                                  NC2184.2
023500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2184.2
023600     MOVE    SPACE TO TEST-RESULTS.                               NC2184.2
023700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2184.2
023800     GO TO CCVS1-EXIT.                                            NC2184.2
023900 CLOSE-FILES.                                                     NC2184.2
024000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2184.2
024100 TERMINATE-CCVS.                                                  NC2184.2
024200*    EXIT PROGRAM.                                                NC2184.2
024300*TERMINATE-CALL.                                                  NC2184.2
024400     STOP     RUN.                                                NC2184.2
024500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2184.2
024600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2184.2
024700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2184.2
024800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2184.2
024900     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2184.2
025000 PRINT-DETAIL.                                                    NC2184.2
025100     IF REC-CT NOT EQUAL TO ZERO                                  NC2184.2
025200             MOVE "." TO PARDOT-X                                 NC2184.2
025300             MOVE REC-CT TO DOTVALUE.                             NC2184.2
025400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2184.2
025500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2184.2
025600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2184.2
025700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2184.2
025800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2184.2
025900     MOVE SPACE TO CORRECT-X.                                     NC2184.2
026000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2184.2
026100     MOVE     SPACE TO RE-MARK.                                   NC2184.2
026200 HEAD-ROUTINE.                                                    NC2184.2
026300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2184.2
026400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2184.2
026500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2184.2
026600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2184.2
026700 COLUMN-NAMES-ROUTINE.                                            NC2184.2
026800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2184.2
026900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2184.2
027000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2184.2
027100 END-ROUTINE.                                                     NC2184.2
027200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2184.2
027300 END-RTN-EXIT.                                                    NC2184.2
027400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2184.2
027500 END-ROUTINE-1.                                                   NC2184.2
027600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2184.2
027700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2184.2
027800      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2184.2
027900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2184.2
028000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2184.2
028100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2184.2
028200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2184.2
028300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2184.2
028400  END-ROUTINE-12.                                                 NC2184.2
028500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2184.2
028600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2184.2
028700         MOVE "NO " TO ERROR-TOTAL                                NC2184.2
028800         ELSE                                                     NC2184.2
028900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2184.2
029000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2184.2
029100     PERFORM WRITE-LINE.                                          NC2184.2
029200 END-ROUTINE-13.                                                  NC2184.2
029300     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2184.2
029400         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2184.2
029500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2184.2
029600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2184.2
029700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2184.2
029800      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2184.2
029900          MOVE "NO " TO ERROR-TOTAL                               NC2184.2
030000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2184.2
030100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2184.2
030200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2184.2
030300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2184.2
030400 WRITE-LINE.                                                      NC2184.2
030500     ADD 1 TO RECORD-COUNT.                                       NC2184.2
030600*    IF RECORD-COUNT GREATER 50                                   NC2184.2
030700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2184.2
030800*        MOVE SPACE TO DUMMY-RECORD                               NC2184.2
030900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2184.2
031000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2184.2
031100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2184.2
031200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2184.2
031300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2184.2
031400*        MOVE ZERO TO RECORD-COUNT.                               NC2184.2
031500     PERFORM WRT-LN.                                              NC2184.2
031600 WRT-LN.                                                          NC2184.2
031700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2184.2
031800     MOVE SPACE TO DUMMY-RECORD.                                  NC2184.2
031900 BLANK-LINE-PRINT.                                                NC2184.2
032000     PERFORM WRT-LN.                                              NC2184.2
032100 FAIL-ROUTINE.                                                    NC2184.2
032200     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2184.2
032300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2184.2
032400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2184.2
032500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2184.2
032600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2184.2
032700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2184.2
032800     GO TO  FAIL-ROUTINE-EX.                                      NC2184.2
032900 FAIL-ROUTINE-WRITE.                                              NC2184.2
033000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2184.2
033100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2184.2
033200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2184.2
033300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2184.2
033400 FAIL-ROUTINE-EX. EXIT.                                           NC2184.2
033500 BAIL-OUT.                                                        NC2184.2
033600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2184.2
033700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2184.2
033800 BAIL-OUT-WRITE.                                                  NC2184.2
033900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2184.2
034000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2184.2
034100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2184.2
034200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2184.2
034300 BAIL-OUT-EX. EXIT.                                               NC2184.2
034400 CCVS1-EXIT.                                                      NC2184.2
034500     EXIT.                                                        NC2184.2
034600 SECT-NC218A-001 SECTION.                                         NC2184.2
034700                                                                  NC2184.2
034800 UST-INIT-GF-1.                                                   NC2184.2
034900     MOVE   "VI-135" TO ANSI-REFERENCE.                           NC2184.2
035000     MOVE   "UST-TEST-GF-1" TO PAR-NAME.                          NC2184.2
035100     MOVE   "PIC X " TO FEATURE.                                  NC2184.2
035200     MOVE    ZERO TO ID4-X.                                       NC2184.2
035300     MOVE   "1200000" TO ID1-XN-7.                                NC2184.2
035400     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
035500     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
035600     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
035700     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
035800     MOVE    1 TO REC-CT.                                         NC2184.2
035900 UST-TEST-GF-1.                                                   NC2184.2
036000     UNSTRING ID1-XN-7 DELIMITED BY ZERO                          NC2184.2
036100              INTO ID4-X DELIMITER IN ID5-XN-4                    NC2184.2
036200              COUNT IN ID6-DU-2V0                                 NC2184.2
036300              WITH POINTER ID10-DU-2V0                            NC2184.2
036400              TALLYING ID11-DU-2V0                                NC2184.2
036500              ON OVERFLOW  PERFORM PASS                           NC2184.2
036600                           GO TO UST-WRITE-GF-1.                  NC2184.2
036700     GO TO UST-FAIL-GF-1.                                         NC2184.2
036800 UST-DELETE-GF-1.                                                 NC2184.2
036900     PERFORM DE-LETE.                                             NC2184.2
037000     PERFORM PRINT-DETAIL.                                        NC2184.2
037100     GO      TO UST-INIT-GF-2.                                    NC2184.2
037200 UST-FAIL-GF-1.                                                   NC2184.2
037300     PERFORM FAIL.                                                NC2184.2
037400     MOVE    "OVERFLOW SHOULD OCCUR" TO RE-MARK.                  NC2184.2
037500 UST-WRITE-GF-1.                                                  NC2184.2
037600     PERFORM PRINT-DETAIL.                                        NC2184.2
037700*                                                                 NC2184.2
037800 UST-TEST-GF-1-1.                                                 NC2184.2
037900     ADD     1 TO REC-CT.                                         NC2184.2
038000     IF      ID4-X = "1"                                          NC2184.2
038100             PERFORM PASS                                         NC2184.2
038200             GO TO UST-WRITE-GF-1-1                               NC2184.2
038300     ELSE                                                         NC2184.2
038400             GO TO UST-FAIL-GF-1-1.                               NC2184.2
038500 UST-DELETE-GF-1-1.                                               NC2184.2
038600     PERFORM DE-LETE.                                             NC2184.2
038700     GO TO UST-WRITE-GF-1-1.                                      NC2184.2
038800 UST-FAIL-GF-1-1.                                                 NC2184.2
038900     PERFORM FAIL                                                 NC2184.2
039000     MOVE    ID4-X TO COMPUTED-A                                  NC2184.2
039100     MOVE    "1" TO CORRECT-A.                                    NC2184.2
039200 UST-WRITE-GF-1-1.                                                NC2184.2
039300     PERFORM PRINT-DETAIL.                                        NC2184.2
039400*                                                                 NC2184.2
039500 UST-TEST-GF-1-2.                                                 NC2184.2
039600     ADD     1 TO REC-CT.                                         NC2184.2
039700     IF      ID5-XN-4 = "0   "                                    NC2184.2
039800             PERFORM PASS                                         NC2184.2
039900             GO TO UST-WRITE-GF-1-2                               NC2184.2
040000     ELSE                                                         NC2184.2
040100             GO TO UST-FAIL-GF-1-2.                               NC2184.2
040200 UST-DELETE-GF-1-2.                                               NC2184.2
040300     PERFORM DE-LETE.                                             NC2184.2
040400     GO TO UST-WRITE-GF-1-2.                                      NC2184.2
040500 UST-FAIL-GF-1-2.                                                 NC2184.2
040600     PERFORM FAIL                                                 NC2184.2
040700     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
040800     MOVE    "0   " TO CORRECT-A.                                 NC2184.2
040900 UST-WRITE-GF-1-2.                                                NC2184.2
041000     PERFORM PRINT-DETAIL.                                        NC2184.2
041100*                                                                 NC2184.2
041200 UST-TEST-GF-1-3.                                                 NC2184.2
041300     ADD     1 TO REC-CT.                                         NC2184.2
041400     IF      ID6-DU-2V0 = 2                                       NC2184.2
041500             PERFORM PASS                                         NC2184.2
041600             GO TO UST-WRITE-GF-1-3                               NC2184.2
041700     ELSE                                                         NC2184.2
041800             GO TO UST-FAIL-GF-1-3.                               NC2184.2
041900 UST-DELETE-GF-1-3.                                               NC2184.2
042000     PERFORM DE-LETE.                                             NC2184.2
042100     GO TO UST-WRITE-GF-1-3.                                      NC2184.2
042200 UST-FAIL-GF-1-3.                                                 NC2184.2
042300     PERFORM FAIL                                                 NC2184.2
042400     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
042500     MOVE    2 TO CORRECT-N.                                      NC2184.2
042600 UST-WRITE-GF-1-3.                                                NC2184.2
042700     PERFORM PRINT-DETAIL.                                        NC2184.2
042800*                                                                 NC2184.2
042900 UST-TEST-GF-1-4.                                                 NC2184.2
043000     ADD     1 TO REC-CT.                                         NC2184.2
043100     IF      ID10-DU-2V0 = 4                                      NC2184.2
043200             PERFORM PASS                                         NC2184.2
043300             GO TO UST-WRITE-GF-1-4                               NC2184.2
043400     ELSE                                                         NC2184.2
043500             GO TO UST-FAIL-GF-1-4.                               NC2184.2
043600 UST-DELETE-GF-1-4.                                               NC2184.2
043700     PERFORM DE-LETE.                                             NC2184.2
043800     GO TO UST-WRITE-GF-1-4.                                      NC2184.2
043900 UST-FAIL-GF-1-4.                                                 NC2184.2
044000     PERFORM FAIL                                                 NC2184.2
044100     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
044200     MOVE    4 TO CORRECT-N.                                      NC2184.2
044300 UST-WRITE-GF-1-4.                                                NC2184.2
044400     PERFORM PRINT-DETAIL.                                        NC2184.2
044500*                                                                 NC2184.2
044600 UST-TEST-GF-1-5.                                                 NC2184.2
044700     ADD     1 TO REC-CT.                                         NC2184.2
044800     IF      ID11-DU-2V0 = 1                                      NC2184.2
044900             PERFORM PASS                                         NC2184.2
045000             GO TO UST-WRITE-GF-1-5                               NC2184.2
045100     ELSE                                                         NC2184.2
045200             GO TO UST-FAIL-GF-1-5.                               NC2184.2
045300 UST-DELETE-GF-1-5.                                               NC2184.2
045400     PERFORM DE-LETE.                                             NC2184.2
045500     GO TO UST-WRITE-GF-1-5.                                      NC2184.2
045600 UST-FAIL-GF-1-5.                                                 NC2184.2
045700     PERFORM FAIL                                                 NC2184.2
045800     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
045900     MOVE    1 TO CORRECT-N.                                      NC2184.2
046000 UST-WRITE-GF-1-5.                                                NC2184.2
046100     PERFORM PRINT-DETAIL.                                        NC2184.2
046200*                                                                 NC2184.2
046300 UST-INIT-GF-2.                                                   NC2184.2
046400     MOVE    "UST-TEST-GF-2" TO PAR-NAME.                         NC2184.2
046500     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
046600     MOVE    "PIC X JUST" TO FEATURE.                             NC2184.2
046700     MOVE    "1200000" TO ID1-XN-7.                               NC2184.2
046800     MOVE    ZERO TO ID4-XJ.                                      NC2184.2
046900     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
047000     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
047100     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
047200     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
047300     MOVE    1 TO REC-CT.                                         NC2184.2
047400*                                                                 NC2184.2
047500 UST-TEST-GF-2.                                                   NC2184.2
047600     UNSTRING ID1-XN-7 DELIMITED ZERO INTO ID4-XJ                 NC2184.2
047700             DELIMITER ID5-XN-4                                   NC2184.2
047800             COUNT ID6-DU-2V0                                     NC2184.2
047900             POINTER ID10-DU-2V0                                  NC2184.2
048000             TALLYING ID11-DU-2V0                                 NC2184.2
048100             OVERFLOW PERFORM PASS                                NC2184.2
048200                      GO TO UST-WRITE-GF-2.                       NC2184.2
048300     GO TO UST-FAIL-GF-2.                                         NC2184.2
048400 UST-DELETE-GF-2.                                                 NC2184.2
048500     PERFORM DE-LETE.                                             NC2184.2
048600     PERFORM PRINT-DETAIL.                                        NC2184.2
048700     GO      TO UST-INIT-GF-3.                                    NC2184.2
048800 UST-FAIL-GF-2.                                                   NC2184.2
048900     PERFORM FAIL.                                                NC2184.2
049000     MOVE    "OVERFLOW SHOULD HAVE OCCURED" TO RE-MARK.           NC2184.2
049100 UST-WRITE-GF-2.                                                  NC2184.2
049200     PERFORM PRINT-DETAIL.                                        NC2184.2
049300*                                                                 NC2184.2
049400 UST-TEST-GF-2-1.                                                 NC2184.2
049500     ADD     1 TO REC-CT.                                         NC2184.2
049600     IF      ID4-XJ = "2"                                         NC2184.2
049700             PERFORM PASS                                         NC2184.2
049800             GO TO UST-WRITE-GF-2-1                               NC2184.2
049900     ELSE                                                         NC2184.2
050000             GO TO UST-FAIL-GF-2-1.                               NC2184.2
050100 UST-DELETE-GF-2-1.                                               NC2184.2
050200     PERFORM DE-LETE.                                             NC2184.2
050300     GO TO UST-WRITE-GF-2-1.                                      NC2184.2
050400 UST-FAIL-GF-2-1.                                                 NC2184.2
050500     PERFORM FAIL                                                 NC2184.2
050600     MOVE    ID4-XJ TO COMPUTED-A                                 NC2184.2
050700     MOVE    "2" TO CORRECT-A.                                    NC2184.2
050800 UST-WRITE-GF-2-1.                                                NC2184.2
050900     PERFORM PRINT-DETAIL.                                        NC2184.2
051000*                                                                 NC2184.2
051100 UST-TEST-GF-2-2.                                                 NC2184.2
051200     ADD     1 TO REC-CT.                                         NC2184.2
051300     IF      ID5-XN-4 = "0   "                                    NC2184.2
051400             PERFORM PASS                                         NC2184.2
051500             GO TO UST-WRITE-GF-2-2                               NC2184.2
051600     ELSE                                                         NC2184.2
051700             GO TO UST-FAIL-GF-2-2.                               NC2184.2
051800 UST-DELETE-GF-2-2.                                               NC2184.2
051900     PERFORM DE-LETE.                                             NC2184.2
052000     GO TO UST-WRITE-GF-2-2.                                      NC2184.2
052100 UST-FAIL-GF-2-2.                                                 NC2184.2
052200     PERFORM FAIL                                                 NC2184.2
052300     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
052400     MOVE    "0   " TO CORRECT-A.                                 NC2184.2
052500 UST-WRITE-GF-2-2.                                                NC2184.2
052600     PERFORM PRINT-DETAIL.                                        NC2184.2
052700*                                                                 NC2184.2
052800 UST-TEST-GF-2-3.                                                 NC2184.2
052900     ADD     1 TO REC-CT.                                         NC2184.2
053000     IF      ID6-DU-2V0 = 2                                       NC2184.2
053100             PERFORM PASS                                         NC2184.2
053200             GO TO UST-WRITE-GF-2-3                               NC2184.2
053300     ELSE                                                         NC2184.2
053400             GO TO UST-FAIL-GF-2-3.                               NC2184.2
053500 UST-DELETE-GF-2-3.                                               NC2184.2
053600     PERFORM DE-LETE.                                             NC2184.2
053700     GO TO UST-WRITE-GF-2-3.                                      NC2184.2
053800 UST-FAIL-GF-2-3.                                                 NC2184.2
053900     PERFORM FAIL                                                 NC2184.2
054000     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
054100     MOVE    2 TO CORRECT-N.                                      NC2184.2
054200 UST-WRITE-GF-2-3.                                                NC2184.2
054300     PERFORM PRINT-DETAIL.                                        NC2184.2
054400*                                                                 NC2184.2
054500 UST-TEST-GF-2-4.                                                 NC2184.2
054600     ADD     1 TO REC-CT.                                         NC2184.2
054700     IF      ID10-DU-2V0 = 4                                      NC2184.2
054800             PERFORM PASS                                         NC2184.2
054900             GO TO UST-WRITE-GF-2-4                               NC2184.2
055000     ELSE                                                         NC2184.2
055100             GO TO UST-FAIL-GF-2-4.                               NC2184.2
055200 UST-DELETE-GF-2-4.                                               NC2184.2
055300     PERFORM DE-LETE.                                             NC2184.2
055400     GO TO UST-WRITE-GF-2-4.                                      NC2184.2
055500 UST-FAIL-GF-2-4.                                                 NC2184.2
055600     PERFORM FAIL                                                 NC2184.2
055700     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
055800     MOVE    4 TO CORRECT-N.                                      NC2184.2
055900 UST-WRITE-GF-2-4.                                                NC2184.2
056000     PERFORM PRINT-DETAIL.                                        NC2184.2
056100*                                                                 NC2184.2
056200 UST-TEST-GF-2-5.                                                 NC2184.2
056300     ADD     1 TO REC-CT.                                         NC2184.2
056400     IF      ID11-DU-2V0 = 1                                      NC2184.2
056500             PERFORM PASS                                         NC2184.2
056600             GO TO UST-WRITE-GF-2-5                               NC2184.2
056700     ELSE                                                         NC2184.2
056800             GO TO UST-FAIL-GF-2-5.                               NC2184.2
056900 UST-DELETE-GF-2-5.                                               NC2184.2
057000     PERFORM DE-LETE.                                             NC2184.2
057100     GO TO UST-WRITE-GF-2-5.                                      NC2184.2
057200 UST-FAIL-GF-2-5.                                                 NC2184.2
057300     PERFORM FAIL                                                 NC2184.2
057400     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
057500     MOVE    1 TO CORRECT-N.                                      NC2184.2
057600 UST-WRITE-GF-2-5.                                                NC2184.2
057700     PERFORM PRINT-DETAIL.                                        NC2184.2
057800*                                                                 NC2184.2
057900 UST-INIT-GF-3.                                                   NC2184.2
058000     MOVE    "UST-TEST-GF-3" TO PAR-NAME.                         NC2184.2
058100     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
058200     MOVE    "PIC XXX" TO FEATURE.                                NC2184.2
058300     MOVE    "1200000" TO ID1-XN-7.                               NC2184.2
058400     MOVE    ZERO TO ID4-XXX.                                     NC2184.2
058500     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
058600     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
058700     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
058800     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
058900     MOVE    1 TO REC-CT.                                         NC2184.2
059000*                                                                 NC2184.2
059100 UST-TEST-GF-3.                                                   NC2184.2
059200     UNSTRING ID1-XN-7 DELIMITED "0" INTO ID4-XXX                 NC2184.2
059300              DELIMITER ID5-XN-4                                  NC2184.2
059400              COUNT ID6-DU-2V0                                    NC2184.2
059500              POINTER ID10-DU-2V0                                 NC2184.2
059600              TALLYING ID11-DU-2V0.                               NC2184.2
059700     GO TO UST-TEST-GF-3-1.                                       NC2184.2
059800 UST-DELETE-GF-3.                                                 NC2184.2
059900     PERFORM DE-LETE.                                             NC2184.2
060000     PERFORM PRINT-DETAIL.                                        NC2184.2
060100     GO      TO UST-INIT-GF-4.                                    NC2184.2
060200*                                                                 NC2184.2
060300 UST-TEST-GF-3-1.                                                 NC2184.2
060400     IF      ID4-XXX = "12  "                                     NC2184.2
060500             PERFORM PASS                                         NC2184.2
060600             GO TO UST-WRITE-GF-3-1                               NC2184.2
060700     ELSE                                                         NC2184.2
060800             GO TO UST-FAIL-GF-3-1.                               NC2184.2
060900 UST-DELETE-GF-3-1.                                               NC2184.2
061000     PERFORM DE-LETE.                                             NC2184.2
061100     GO TO UST-WRITE-GF-3-1.                                      NC2184.2
061200 UST-FAIL-GF-3-1.                                                 NC2184.2
061300     PERFORM FAIL                                                 NC2184.2
061400     MOVE    ID4-XXX TO COMPUTED-A                                NC2184.2
061500     MOVE    "12  " TO CORRECT-A.                                 NC2184.2
061600 UST-WRITE-GF-3-1.                                                NC2184.2
061700     PERFORM PRINT-DETAIL.                                        NC2184.2
061800*                                                                 NC2184.2
061900 UST-TEST-GF-3-2.                                                 NC2184.2
062000     ADD     1 TO REC-CT.                                         NC2184.2
062100     IF      ID5-XN-4 = "0   "                                    NC2184.2
062200             PERFORM PASS                                         NC2184.2
062300             GO TO UST-WRITE-GF-3-2                               NC2184.2
062400     ELSE                                                         NC2184.2
062500             GO TO UST-FAIL-GF-3-2.                               NC2184.2
062600 UST-DELETE-GF-3-2.                                               NC2184.2
062700     PERFORM DE-LETE.                                             NC2184.2
062800     GO TO UST-WRITE-GF-3-2.                                      NC2184.2
062900 UST-FAIL-GF-3-2.                                                 NC2184.2
063000     PERFORM FAIL                                                 NC2184.2
063100     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
063200     MOVE    "0   " TO CORRECT-A.                                 NC2184.2
063300 UST-WRITE-GF-3-2.                                                NC2184.2
063400     PERFORM PRINT-DETAIL.                                        NC2184.2
063500*                                                                 NC2184.2
063600 UST-TEST-GF-3-3.                                                 NC2184.2
063700     ADD     1 TO REC-CT.                                         NC2184.2
063800     IF      ID6-DU-2V0 = 2                                       NC2184.2
063900             PERFORM PASS                                         NC2184.2
064000             GO TO UST-WRITE-GF-3-3                               NC2184.2
064100     ELSE                                                         NC2184.2
064200             GO TO UST-FAIL-GF-3-3.                               NC2184.2
064300 UST-DELETE-GF-3-3.                                               NC2184.2
064400     PERFORM DE-LETE.                                             NC2184.2
064500     GO TO UST-WRITE-GF-3-3.                                      NC2184.2
064600 UST-FAIL-GF-3-3.                                                 NC2184.2
064700     PERFORM FAIL                                                 NC2184.2
064800     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
064900     MOVE    2 TO CORRECT-N.                                      NC2184.2
065000 UST-WRITE-GF-3-3.                                                NC2184.2
065100     PERFORM PRINT-DETAIL.                                        NC2184.2
065200*                                                                 NC2184.2
065300 UST-TEST-GF-3-4.                                                 NC2184.2
065400     ADD     1 TO REC-CT.                                         NC2184.2
065500     IF      ID10-DU-2V0 = 4                                      NC2184.2
065600             PERFORM PASS                                         NC2184.2
065700             GO TO UST-WRITE-GF-3-4                               NC2184.2
065800     ELSE                                                         NC2184.2
065900             GO TO UST-FAIL-GF-3-4.                               NC2184.2
066000 UST-DELETE-GF-3-4.                                               NC2184.2
066100     PERFORM DE-LETE.                                             NC2184.2
066200     GO TO UST-WRITE-GF-3-4.                                      NC2184.2
066300 UST-FAIL-GF-3-4.                                                 NC2184.2
066400     PERFORM FAIL                                                 NC2184.2
066500     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
066600     MOVE    4 TO CORRECT-N.                                      NC2184.2
066700 UST-WRITE-GF-3-4.                                                NC2184.2
066800     PERFORM PRINT-DETAIL.                                        NC2184.2
066900*                                                                 NC2184.2
067000 UST-TEST-GF-3-5.                                                 NC2184.2
067100     ADD     1 TO REC-CT.                                         NC2184.2
067200     IF      ID11-DU-2V0 = 1                                      NC2184.2
067300             PERFORM PASS                                         NC2184.2
067400             GO TO UST-WRITE-GF-3-5                               NC2184.2
067500     ELSE                                                         NC2184.2
067600             GO TO UST-FAIL-GF-3-5.                               NC2184.2
067700 UST-DELETE-GF-3-5.                                               NC2184.2
067800     PERFORM DE-LETE.                                             NC2184.2
067900     GO TO UST-WRITE-GF-3-5.                                      NC2184.2
068000 UST-FAIL-GF-3-5.                                                 NC2184.2
068100     PERFORM FAIL                                                 NC2184.2
068200     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
068300     MOVE    1 TO CORRECT-N.                                      NC2184.2
068400 UST-WRITE-GF-3-5.                                                NC2184.2
068500     PERFORM PRINT-DETAIL.                                        NC2184.2
068600*                                                                 NC2184.2
068700 UST-INIT-GF-4.                                                   NC2184.2
068800     MOVE    "UST-TEST-GF-4" TO PAR-NAME.                         NC2184.2
068900     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
069000     MOVE    "PIC XXX JUST" TO FEATURE.                           NC2184.2
069100     MOVE    "1200000" TO ID1-XN-7.                               NC2184.2
069200     MOVE    "0" TO ZERO-XN-1.                                    NC2184.2
069300     MOVE    ZERO TO ID4-XXXJ.                                    NC2184.2
069400     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
069500     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
069600     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
069700     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
069800     MOVE    1 TO REC-CT.                                         NC2184.2
069900*                                                                 NC2184.2
070000 UST-TEST-GF-4.                                                   NC2184.2
070100     UNSTRING ID1-XN-7 DELIMITED BY ZERO-XN-1 INTO ID4-XXXJ       NC2184.2
070200              DELIMITER ID5-XN-4                                  NC2184.2
070300              COUNT IN ID6-DU-2V0                                 NC2184.2
070400              POINTER ID10-DU-2V0                                 NC2184.2
070500              TALLYING IN ID11-DU-2V0.                            NC2184.2
070600     GO TO UST-TEST-GF-4-1.                                       NC2184.2
070700 UST-DELETE-GF-4.                                                 NC2184.2
070800     PERFORM DE-LETE.                                             NC2184.2
070900     PERFORM PRINT-DETAIL.                                        NC2184.2
071000     GO      TO UST-INIT-GF-5.                                    NC2184.2
071100*                                                                 NC2184.2
071200 UST-TEST-GF-4-1.                                                 NC2184.2
071300     IF      ID4-XXXJ = " 12"                                     NC2184.2
071400             PERFORM PASS                                         NC2184.2
071500             GO TO UST-WRITE-GF-4-1                               NC2184.2
071600     ELSE                                                         NC2184.2
071700             GO TO UST-FAIL-GF-4-1.                               NC2184.2
071800 UST-DELETE-GF-4-1.                                               NC2184.2
071900     PERFORM DE-LETE.                                             NC2184.2
072000     GO TO UST-WRITE-GF-4-1.                                      NC2184.2
072100 UST-FAIL-GF-4-1.                                                 NC2184.2
072200     PERFORM FAIL                                                 NC2184.2
072300     MOVE    ID4-XXXJ TO COMPUTED-A                               NC2184.2
072400     MOVE    " 12" TO CORRECT-A.                                  NC2184.2
072500 UST-WRITE-GF-4-1.                                                NC2184.2
072600     PERFORM PRINT-DETAIL.                                        NC2184.2
072700*                                                                 NC2184.2
072800 UST-TEST-GF-4-2.                                                 NC2184.2
072900     ADD     1 TO REC-CT.                                         NC2184.2
073000     IF      ID5-XN-4 = "0   "                                    NC2184.2
073100             PERFORM PASS                                         NC2184.2
073200             GO TO UST-WRITE-GF-4-2                               NC2184.2
073300     ELSE                                                         NC2184.2
073400             GO TO UST-FAIL-GF-4-2.                               NC2184.2
073500 UST-DELETE-GF-4-2.                                               NC2184.2
073600     PERFORM DE-LETE.                                             NC2184.2
073700     GO TO UST-WRITE-GF-4-2.                                      NC2184.2
073800 UST-FAIL-GF-4-2.                                                 NC2184.2
073900     PERFORM FAIL                                                 NC2184.2
074000     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
074100     MOVE "0   " TO CORRECT-A.                                    NC2184.2
074200 UST-WRITE-GF-4-2.                                                NC2184.2
074300     PERFORM PRINT-DETAIL.                                        NC2184.2
074400*                                                                 NC2184.2
074500 UST-TEST-GF-4-3.                                                 NC2184.2
074600     ADD     1 TO REC-CT.                                         NC2184.2
074700     IF      ID6-DU-2V0 = 2                                       NC2184.2
074800             PERFORM PASS                                         NC2184.2
074900             GO TO UST-WRITE-GF-4-3                               NC2184.2
075000     ELSE                                                         NC2184.2
075100             GO TO UST-FAIL-GF-4-3.                               NC2184.2
075200 UST-DELETE-GF-4-3.                                               NC2184.2
075300     PERFORM DE-LETE.                                             NC2184.2
075400     GO TO UST-WRITE-GF-4-3.                                      NC2184.2
075500 UST-FAIL-GF-4-3.                                                 NC2184.2
075600     PERFORM FAIL                                                 NC2184.2
075700     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
075800     MOVE    2 TO CORRECT-N.                                      NC2184.2
075900 UST-WRITE-GF-4-3.                                                NC2184.2
076000     PERFORM PRINT-DETAIL.                                        NC2184.2
076100*                                                                 NC2184.2
076200 UST-TEST-GF-4-4.                                                 NC2184.2
076300     ADD     1 TO REC-CT.                                         NC2184.2
076400     IF      ID10-DU-2V0 = 4                                      NC2184.2
076500             PERFORM PASS                                         NC2184.2
076600             GO TO UST-WRITE-GF-4-4                               NC2184.2
076700     ELSE                                                         NC2184.2
076800             GO TO UST-FAIL-GF-4-4.                               NC2184.2
076900 UST-DELETE-GF-4-4.                                               NC2184.2
077000     PERFORM DE-LETE.                                             NC2184.2
077100     GO TO UST-WRITE-GF-4-4.                                      NC2184.2
077200 UST-FAIL-GF-4-4.                                                 NC2184.2
077300     PERFORM FAIL                                                 NC2184.2
077400     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
077500     MOVE    4 TO CORRECT-N.                                      NC2184.2
077600 UST-WRITE-GF-4-4.                                                NC2184.2
077700     PERFORM PRINT-DETAIL.                                        NC2184.2
077800*                                                                 NC2184.2
077900 UST-TEST-GF-4-5.                                                 NC2184.2
078000     ADD     1 TO REC-CT.                                         NC2184.2
078100     IF      ID11-DU-2V0 = 1                                      NC2184.2
078200             PERFORM PASS                                         NC2184.2
078300             GO TO UST-WRITE-GF-4-5                               NC2184.2
078400     ELSE                                                         NC2184.2
078500             GO TO UST-FAIL-GF-4-5.                               NC2184.2
078600 UST-DELETE-GF-4-5.                                               NC2184.2
078700     PERFORM DE-LETE.                                             NC2184.2
078800     GO TO UST-WRITE-GF-4-5.                                      NC2184.2
078900 UST-FAIL-GF-4-5.                                                 NC2184.2
079000     PERFORM FAIL                                                 NC2184.2
079100     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
079200     MOVE    1 TO CORRECT-N.                                      NC2184.2
079300 UST-WRITE-GF-4-5.                                                NC2184.2
079400     PERFORM PRINT-DETAIL.                                        NC2184.2
079500*                                                                 NC2184.2
079600 UST-INIT-GF-5.                                                   NC2184.2
079700     MOVE    "UST-TEST-GF-5" TO PAR-NAME.                         NC2184.2
079800     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
079900     MOVE    "PIC 9" TO FEATURE.                                  NC2184.2
080000     MOVE    ZERO TO ID4-DU-1V0.                                  NC2184.2
080100     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
080200     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
080300     MOVE    "1200000" TO ID1-XN-7.                               NC2184.2
080400     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
080500     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
080600     MOVE    1 TO REC-CT.                                         NC2184.2
080700*                                                                 NC2184.2
080800 UST-TEST-GF-5.                                                   NC2184.2
080900     UNSTRING ID1-XN-7 DELIMITED "0" INTO ID4-DU-1V0              NC2184.2
081000              DELIMITER IN ID5-XN-4                               NC2184.2
081100              COUNT ID6-DU-2V0                                    NC2184.2
081200              WITH POINTER ID10-DU-2V0                            NC2184.2
081300              TALLYING ID11-DU-2V0.                               NC2184.2
081400     GO TO UST-TEST-GF-5-1.                                       NC2184.2
081500 UST-DELETE-GF-5.                                                 NC2184.2
081600     PERFORM DE-LETE.                                             NC2184.2
081700     PERFORM PRINT-DETAIL.                                        NC2184.2
081800     GO      TO UST-INIT-GF-6.                                    NC2184.2
081900*                                                                 NC2184.2
082000 UST-TEST-GF-5-1.                                                 NC2184.2
082100     IF      ID4-DU-1V0 = 2                                       NC2184.2
082200             PERFORM PASS                                         NC2184.2
082300             GO TO UST-WRITE-GF-5-1                               NC2184.2
082400     ELSE                                                         NC2184.2
082500             GO TO UST-FAIL-GF-5-1.                               NC2184.2
082600 UST-DELETE-GF-5-1.                                               NC2184.2
082700     PERFORM DE-LETE.                                             NC2184.2
082800     GO TO UST-WRITE-GF-5-1.                                      NC2184.2
082900 UST-FAIL-GF-5-1.                                                 NC2184.2
083000     PERFORM FAIL                                                 NC2184.2
083100     MOVE    ID4-DU-1V0 TO COMPUTED-N                             NC2184.2
083200     MOVE    2 TO CORRECT-N.                                      NC2184.2
083300 UST-WRITE-GF-5-1.                                                NC2184.2
083400     PERFORM PRINT-DETAIL.                                        NC2184.2
083500*                                                                 NC2184.2
083600 UST-TEST-GF-5-2.                                                 NC2184.2
083700     ADD     1 TO REC-CT.                                         NC2184.2
083800     IF      ID5-XN-4 = "0   "                                    NC2184.2
083900             PERFORM PASS                                         NC2184.2
084000             GO TO UST-WRITE-GF-5-2                               NC2184.2
084100     ELSE                                                         NC2184.2
084200             GO TO UST-FAIL-GF-5-2.                               NC2184.2
084300 UST-DELETE-GF-5-2.                                               NC2184.2
084400     PERFORM DE-LETE.                                             NC2184.2
084500     GO TO UST-WRITE-GF-5-2.                                      NC2184.2
084600 UST-FAIL-GF-5-2.                                                 NC2184.2
084700     PERFORM FAIL                                                 NC2184.2
084800     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
084900     MOVE    "0   " TO CORRECT-A.                                 NC2184.2
085000 UST-WRITE-GF-5-2.                                                NC2184.2
085100     PERFORM PRINT-DETAIL.                                        NC2184.2
085200*                                                                 NC2184.2
085300 UST-TEST-GF-5-3.                                                 NC2184.2
085400     ADD     1 TO REC-CT.                                         NC2184.2
085500     IF      ID6-DU-2V0 = 2                                       NC2184.2
085600             PERFORM PASS                                         NC2184.2
085700             GO TO UST-WRITE-GF-5-3                               NC2184.2
085800     ELSE                                                         NC2184.2
085900             GO TO UST-FAIL-GF-5-3.                               NC2184.2
086000 UST-DELETE-GF-5-3.                                               NC2184.2
086100     PERFORM DE-LETE.                                             NC2184.2
086200     GO TO UST-WRITE-GF-5-3.                                      NC2184.2
086300 UST-FAIL-GF-5-3.                                                 NC2184.2
086400     PERFORM FAIL                                                 NC2184.2
086500     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
086600     MOVE    2 TO CORRECT-N.                                      NC2184.2
086700 UST-WRITE-GF-5-3.                                                NC2184.2
086800     PERFORM PRINT-DETAIL.                                        NC2184.2
086900*                                                                 NC2184.2
087000 UST-TEST-GF-5-4.                                                 NC2184.2
087100     ADD     1 TO REC-CT.                                         NC2184.2
087200     IF      ID10-DU-2V0 = 4                                      NC2184.2
087300             PERFORM PASS                                         NC2184.2
087400             GO TO UST-WRITE-GF-5-4                               NC2184.2
087500     ELSE                                                         NC2184.2
087600             GO TO UST-FAIL-GF-5-4.                               NC2184.2
087700 UST-DELETE-GF-5-4.                                               NC2184.2
087800     PERFORM DE-LETE.                                             NC2184.2
087900     GO TO UST-WRITE-GF-5-4.                                      NC2184.2
088000 UST-FAIL-GF-5-4.                                                 NC2184.2
088100     PERFORM FAIL                                                 NC2184.2
088200     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
088300     MOVE    4 TO CORRECT-N.                                      NC2184.2
088400 UST-WRITE-GF-5-4.                                                NC2184.2
088500     PERFORM PRINT-DETAIL.                                        NC2184.2
088600*                                                                 NC2184.2
088700 UST-TEST-GF-5-5.                                                 NC2184.2
088800     ADD     1 TO REC-CT.                                         NC2184.2
088900     IF      ID11-DU-2V0 = 1                                      NC2184.2
089000             PERFORM PASS                                         NC2184.2
089100             GO TO UST-WRITE-GF-5-5                               NC2184.2
089200     ELSE                                                         NC2184.2
089300             GO TO UST-FAIL-GF-5-5.                               NC2184.2
089400 UST-DELETE-GF-5-5.                                               NC2184.2
089500     PERFORM DE-LETE.                                             NC2184.2
089600     GO TO UST-WRITE-GF-5-5.                                      NC2184.2
089700 UST-FAIL-GF-5-5.                                                 NC2184.2
089800     PERFORM FAIL                                                 NC2184.2
089900     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
090000     MOVE    1 TO CORRECT-N.                                      NC2184.2
090100 UST-WRITE-GF-5-5.                                                NC2184.2
090200     PERFORM PRINT-DETAIL.                                        NC2184.2
090300*                                                                 NC2184.2
090400 UST-INIT-GF-6.                                                   NC2184.2
090500     MOVE    "UST-TEST-GF-6" TO PAR-NAME.                         NC2184.2
090600     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
090700     MOVE    "PIC S9" TO FEATURE.                                 NC2184.2
090800     MOVE    ZERO TO ID4-DS-1V0.                                  NC2184.2
090900     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
091000     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
091100     MOVE    "1200000" TO ID1-XN-7.                               NC2184.2
091200     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
091300     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
091400     MOVE    1 TO REC-CT.                                         NC2184.2
091500*                                                                 NC2184.2
091600 UST-TEST-GF-6.                                                   NC2184.2
091700     UNSTRING ID1-XN-7 DELIMITED BY ALL ZERO INTO ID4-DS-1V0      NC2184.2
091800              DELIMITER ID5-XN-4                                  NC2184.2
091900              COUNT ID6-DU-2V0                                    NC2184.2
092000              POINTER ID10-DU-2V0                                 NC2184.2
092100              TALLYING ID11-DU-2V0.                               NC2184.2
092200     GO TO UST-TEST-GF-6-1.                                       NC2184.2
092300 UST-DELETE-GF-6.                                                 NC2184.2
092400     PERFORM DE-LETE.                                             NC2184.2
092500     PERFORM PRINT-DETAIL.                                        NC2184.2
092600     GO      TO UST-INIT-GF-7.                                    NC2184.2
092700*                                                                 NC2184.2
092800 UST-TEST-GF-6-1.                                                 NC2184.2
092900     IF      ID4-DS-1V0 = +2                                      NC2184.2
093000             PERFORM PASS                                         NC2184.2
093100             GO TO UST-WRITE-GF-6-1                               NC2184.2
093200     ELSE                                                         NC2184.2
093300             GO TO UST-FAIL-GF-6-1.                               NC2184.2
093400 UST-DELETE-GF-6-1.                                               NC2184.2
093500     PERFORM DE-LETE.                                             NC2184.2
093600     GO TO UST-WRITE-GF-6-1.                                      NC2184.2
093700 UST-FAIL-GF-6-1.                                                 NC2184.2
093800     PERFORM FAIL                                                 NC2184.2
093900     MOVE    ID4-DS-1V0 TO COMPUTED-N                             NC2184.2
094000     MOVE    +2 TO CORRECT-N.                                     NC2184.2
094100 UST-WRITE-GF-6-1.                                                NC2184.2
094200     PERFORM PRINT-DETAIL.                                        NC2184.2
094300*                                                                 NC2184.2
094400 UST-TEST-GF-6-2.                                                 NC2184.2
094500     ADD     1 TO REC-CT.                                         NC2184.2
094600     IF      ID5-XN-4 = "0   "                                    NC2184.2
094700             PERFORM PASS                                         NC2184.2
094800             GO TO UST-WRITE-GF-6-2                               NC2184.2
094900     ELSE                                                         NC2184.2
095000             GO TO UST-FAIL-GF-6-2.                               NC2184.2
095100 UST-DELETE-GF-6-2.                                               NC2184.2
095200     PERFORM DE-LETE.                                             NC2184.2
095300     GO TO UST-WRITE-GF-6-2.                                      NC2184.2
095400 UST-FAIL-GF-6-2.                                                 NC2184.2
095500     PERFORM FAIL                                                 NC2184.2
095600     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
095700     MOVE    "0   " TO CORRECT-A.                                 NC2184.2
095800 UST-WRITE-GF-6-2.                                                NC2184.2
095900     PERFORM PRINT-DETAIL.                                        NC2184.2
096000*                                                                 NC2184.2
096100 UST-TEST-GF-6-3.                                                 NC2184.2
096200     ADD     1 TO REC-CT.                                         NC2184.2
096300     IF      ID6-DU-2V0 = 2                                       NC2184.2
096400             PERFORM PASS                                         NC2184.2
096500             GO TO UST-WRITE-GF-6-3                               NC2184.2
096600     ELSE                                                         NC2184.2
096700             GO TO UST-FAIL-GF-6-3.                               NC2184.2
096800 UST-DELETE-GF-6-3.                                               NC2184.2
096900     PERFORM DE-LETE.                                             NC2184.2
097000     GO TO UST-WRITE-GF-6-3.                                      NC2184.2
097100 UST-FAIL-GF-6-3.                                                 NC2184.2
097200     PERFORM FAIL                                                 NC2184.2
097300     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
097400     MOVE    2 TO CORRECT-N.                                      NC2184.2
097500 UST-WRITE-GF-6-3.                                                NC2184.2
097600     PERFORM PRINT-DETAIL.                                        NC2184.2
097700*                                                                 NC2184.2
097800 UST-TEST-GF-6-4.                                                 NC2184.2
097900     ADD     1 TO REC-CT.                                         NC2184.2
098000     IF      ID10-DU-2V0 = 8                                      NC2184.2
098100             PERFORM PASS                                         NC2184.2
098200             GO TO UST-WRITE-GF-6-4                               NC2184.2
098300     ELSE                                                         NC2184.2
098400             GO TO UST-FAIL-GF-6-4.                               NC2184.2
098500 UST-DELETE-GF-6-4.                                               NC2184.2
098600     PERFORM DE-LETE.                                             NC2184.2
098700     GO TO UST-WRITE-GF-6-4.                                      NC2184.2
098800 UST-FAIL-GF-6-4.                                                 NC2184.2
098900     PERFORM FAIL                                                 NC2184.2
099000     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
099100     MOVE    8 TO CORRECT-N.                                      NC2184.2
099200 UST-WRITE-GF-6-4.                                                NC2184.2
099300     PERFORM PRINT-DETAIL.                                        NC2184.2
099400*                                                                 NC2184.2
099500 UST-TEST-GF-6-5.                                                 NC2184.2
099600     ADD     1 TO REC-CT.                                         NC2184.2
099700     IF      ID11-DU-2V0 = 1                                      NC2184.2
099800             PERFORM PASS                                         NC2184.2
099900             GO TO UST-WRITE-GF-6-5                               NC2184.2
100000     ELSE                                                         NC2184.2
100100             GO TO UST-FAIL-GF-6-5.                               NC2184.2
100200 UST-DELETE-GF-6-5.                                               NC2184.2
100300     PERFORM DE-LETE.                                             NC2184.2
100400     GO TO UST-WRITE-GF-6-5.                                      NC2184.2
100500 UST-FAIL-GF-6-5.                                                 NC2184.2
100600     PERFORM FAIL                                                 NC2184.2
100700     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
100800     MOVE    1 TO CORRECT-N.                                      NC2184.2
100900 UST-WRITE-GF-6-5.                                                NC2184.2
101000     PERFORM PRINT-DETAIL.                                        NC2184.2
101100*                                                                 NC2184.2
101200 UST-INIT-GF-7.                                                   NC2184.2
101300     MOVE    "UST-TEST-GF-7" TO PAR-NAME.                         NC2184.2
101400     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
101500     MOVE    "PIC 99" TO FEATURE.                                 NC2184.2
101600     MOVE    "1200000" TO ID1-XN-7.                               NC2184.2
101700     MOVE    ZERO TO ID4-DU-2V0.                                  NC2184.2
101800     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
101900     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
102000     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
102100     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
102200     MOVE    1 TO REC-CT.                                         NC2184.2
102300*                                                                 NC2184.2
102400 UST-TEST-GF-7.                                                   NC2184.2
102500     UNSTRING ID1-XN-7 DELIMITED ALL "0" INTO ID4-DU-2V0          NC2184.2
102600             DELIMITER ID5-XN-4                                   NC2184.2
102700             COUNT ID6-DU-2V0                                     NC2184.2
102800             POINTER ID10-DU-2V0                                  NC2184.2
102900             TALLYING ID11-DU-2V0.                                NC2184.2
103000     GO      TO UST-TEST-GF-7-1.                                  NC2184.2
103100 UST-DELETE-GF-7.                                                 NC2184.2
103200     PERFORM DE-LETE.                                             NC2184.2
103300     PERFORM PRINT-DETAIL.                                        NC2184.2
103400     GO      TO UST-INIT-GF-8.                                    NC2184.2
103500*                                                                 NC2184.2
103600 UST-TEST-GF-7-1.                                                 NC2184.2
103700     IF      ID4-DU-2V0 = 12                                      NC2184.2
103800             PERFORM PASS                                         NC2184.2
103900             GO TO UST-WRITE-GF-7-1                               NC2184.2
104000     ELSE                                                         NC2184.2
104100             GO TO UST-FAIL-GF-7-1.                               NC2184.2
104200 UST-DELETE-GF-7-1.                                               NC2184.2
104300     PERFORM DE-LETE.                                             NC2184.2
104400     GO TO UST-WRITE-GF-7-1.                                      NC2184.2
104500 UST-FAIL-GF-7-1.                                                 NC2184.2
104600     PERFORM FAIL                                                 NC2184.2
104700     MOVE    ID4-DU-2V0 TO COMPUTED-N                             NC2184.2
104800     MOVE    12 TO CORRECT-N.                                     NC2184.2
104900 UST-WRITE-GF-7-1.                                                NC2184.2
105000     PERFORM PRINT-DETAIL.                                        NC2184.2
105100*                                                                 NC2184.2
105200 UST-TEST-GF-7-2.                                                 NC2184.2
105300     ADD     1 TO REC-CT.                                         NC2184.2
105400     IF      ID5-XN-4 = "0   "                                    NC2184.2
105500             PERFORM PASS                                         NC2184.2
105600             GO TO UST-WRITE-GF-7-2                               NC2184.2
105700     ELSE                                                         NC2184.2
105800             GO TO UST-FAIL-GF-7-2.                               NC2184.2
105900 UST-DELETE-GF-7-2.                                               NC2184.2
106000     PERFORM DE-LETE.                                             NC2184.2
106100     GO TO UST-WRITE-GF-7-2.                                      NC2184.2
106200 UST-FAIL-GF-7-2.                                                 NC2184.2
106300     PERFORM FAIL                                                 NC2184.2
106400     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
106500     MOVE    "0   " TO CORRECT-A.                                 NC2184.2
106600 UST-WRITE-GF-7-2.                                                NC2184.2
106700     PERFORM PRINT-DETAIL.                                        NC2184.2
106800*                                                                 NC2184.2
106900 UST-TEST-GF-7-3.                                                 NC2184.2
107000     ADD     1 TO REC-CT.                                         NC2184.2
107100     IF      ID6-DU-2V0 = 2                                       NC2184.2
107200             PERFORM PASS                                         NC2184.2
107300             GO TO UST-WRITE-GF-7-3                               NC2184.2
107400     ELSE                                                         NC2184.2
107500             GO TO UST-FAIL-GF-7-3.                               NC2184.2
107600 UST-DELETE-GF-7-3.                                               NC2184.2
107700     PERFORM DE-LETE.                                             NC2184.2
107800     GO TO UST-WRITE-GF-7-3.                                      NC2184.2
107900 UST-FAIL-GF-7-3.                                                 NC2184.2
108000     PERFORM FAIL                                                 NC2184.2
108100     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
108200     MOVE    2 TO CORRECT-N.                                      NC2184.2
108300 UST-WRITE-GF-7-3.                                                NC2184.2
108400     PERFORM PRINT-DETAIL.                                        NC2184.2
108500*                                                                 NC2184.2
108600 UST-TEST-GF-7-4.                                                 NC2184.2
108700     ADD     1 TO REC-CT.                                         NC2184.2
108800     IF      ID10-DU-2V0 = 8                                      NC2184.2
108900             PERFORM PASS                                         NC2184.2
109000             GO TO UST-WRITE-GF-7-4                               NC2184.2
109100     ELSE                                                         NC2184.2
109200             GO TO UST-FAIL-GF-7-4.                               NC2184.2
109300 UST-DELETE-GF-7-4.                                               NC2184.2
109400     PERFORM DE-LETE.                                             NC2184.2
109500     GO TO UST-WRITE-GF-7-4.                                      NC2184.2
109600 UST-FAIL-GF-7-4.                                                 NC2184.2
109700     PERFORM FAIL                                                 NC2184.2
109800     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
109900     MOVE    8 TO CORRECT-N.                                      NC2184.2
110000 UST-WRITE-GF-7-4.                                                NC2184.2
110100     PERFORM PRINT-DETAIL.                                        NC2184.2
110200*                                                                 NC2184.2
110300 UST-TEST-GF-7-5.                                                 NC2184.2
110400     ADD     1 TO REC-CT.                                         NC2184.2
110500     IF      ID11-DU-2V0 = 1                                      NC2184.2
110600             PERFORM PASS                                         NC2184.2
110700             GO TO UST-WRITE-GF-7-5                               NC2184.2
110800     ELSE                                                         NC2184.2
110900             GO TO UST-FAIL-GF-7-5.                               NC2184.2
111000 UST-DELETE-GF-7-5.                                               NC2184.2
111100     PERFORM DE-LETE.                                             NC2184.2
111200     GO TO UST-WRITE-GF-7-5.                                      NC2184.2
111300 UST-FAIL-GF-7-5.                                                 NC2184.2
111400     PERFORM FAIL                                                 NC2184.2
111500     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
111600     MOVE    1 TO CORRECT-N.                                      NC2184.2
111700 UST-WRITE-GF-7-5.                                                NC2184.2
111800     PERFORM PRINT-DETAIL.                                        NC2184.2
111900*                                                                 NC2184.2
112000 UST-INIT-GF-8.                                                   NC2184.2
112100     MOVE    "UST-TEST-GF-8" TO PAR-NAME.                         NC2184.2
112200     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
112300     MOVE    "PIC S99" TO FEATURE.                                NC2184.2
112400     MOVE    "1200000" TO ID1-XN-7.                               NC2184.2
112500     MOVE    ZERO TO ID4-DS-2V0.                                  NC2184.2
112600     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
112700     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
112800     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
112900     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
113000     MOVE    1 TO REC-CT.                                         NC2184.2
113100*                                                                 NC2184.2
113200 UST-TEST-GF-8.                                                   NC2184.2
113300     UNSTRING ID1-XN-7 DELIMITED ZERO-XN-1 INTO ID4-DS-2V0        NC2184.2
113400             DELIMITER ID5-XN-4                                   NC2184.2
113500             COUNT ID6-DU-2V0                                     NC2184.2
113600             POINTER ID10-DU-2V0                                  NC2184.2
113700             TALLYING IN ID11-DU-2V0.                             NC2184.2
113800     GO TO UST-TEST-GF-8-1.                                       NC2184.2
113900 UST-DELETE-GF-8.                                                 NC2184.2
114000     PERFORM DE-LETE.                                             NC2184.2
114100     PERFORM PRINT-DETAIL.                                        NC2184.2
114200     GO      TO UST-INIT-GF-9.                                    NC2184.2
114300*                                                                 NC2184.2
114400 UST-TEST-GF-8-1.                                                 NC2184.2
114500     IF      ID4-DS-2V0 = +12                                     NC2184.2
114600             PERFORM PASS                                         NC2184.2
114700             GO TO UST-WRITE-GF-8-1                               NC2184.2
114800     ELSE                                                         NC2184.2
114900             GO TO UST-FAIL-GF-8-1.                               NC2184.2
115000 UST-DELETE-GF-8-1.                                               NC2184.2
115100     PERFORM DE-LETE.                                             NC2184.2
115200     GO TO UST-WRITE-GF-8-1.                                      NC2184.2
115300 UST-FAIL-GF-8-1.                                                 NC2184.2
115400     PERFORM FAIL                                                 NC2184.2
115500     MOVE    ID4-DS-2V0 TO COMPUTED-N                             NC2184.2
115600     MOVE    +12 TO CORRECT-N.                                    NC2184.2
115700 UST-WRITE-GF-8-1.                                                NC2184.2
115800     PERFORM PRINT-DETAIL.                                        NC2184.2
115900*                                                                 NC2184.2
116000 UST-TEST-GF-8-2.                                                 NC2184.2
116100     ADD     1 TO REC-CT.                                         NC2184.2
116200     IF      ID5-XN-4 = "0   "                                    NC2184.2
116300             PERFORM PASS                                         NC2184.2
116400             GO TO UST-WRITE-GF-8-2                               NC2184.2
116500     ELSE                                                         NC2184.2
116600             GO TO UST-FAIL-GF-8-2.                               NC2184.2
116700 UST-DELETE-GF-8-2.                                               NC2184.2
116800     PERFORM DE-LETE.                                             NC2184.2
116900     GO TO UST-WRITE-GF-8-2.                                      NC2184.2
117000 UST-FAIL-GF-8-2.                                                 NC2184.2
117100     PERFORM FAIL                                                 NC2184.2
117200     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
117300     MOVE    "0   " TO CORRECT-A.                                 NC2184.2
117400 UST-WRITE-GF-8-2.                                                NC2184.2
117500     PERFORM PRINT-DETAIL.                                        NC2184.2
117600*                                                                 NC2184.2
117700 UST-TEST-GF-8-3.                                                 NC2184.2
117800     ADD     1 TO REC-CT.                                         NC2184.2
117900     IF      ID6-DU-2V0 = 2                                       NC2184.2
118000             PERFORM PASS                                         NC2184.2
118100             GO TO UST-WRITE-GF-8-3                               NC2184.2
118200     ELSE                                                         NC2184.2
118300             GO TO UST-FAIL-GF-8-3.                               NC2184.2
118400 UST-DELETE-GF-8-3.                                               NC2184.2
118500     PERFORM DE-LETE.                                             NC2184.2
118600     GO TO UST-WRITE-GF-8-3.                                      NC2184.2
118700 UST-FAIL-GF-8-3.                                                 NC2184.2
118800     PERFORM FAIL                                                 NC2184.2
118900     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
119000     MOVE    2 TO CORRECT-N.                                      NC2184.2
119100 UST-WRITE-GF-8-3.                                                NC2184.2
119200     PERFORM PRINT-DETAIL.                                        NC2184.2
119300*                                                                 NC2184.2
119400 UST-TEST-GF-8-4.                                                 NC2184.2
119500     ADD     1 TO REC-CT.                                         NC2184.2
119600     IF      ID10-DU-2V0 = 4                                      NC2184.2
119700             PERFORM PASS                                         NC2184.2
119800             GO TO UST-WRITE-GF-8-4                               NC2184.2
119900     ELSE                                                         NC2184.2
120000             GO TO UST-FAIL-GF-8-4.                               NC2184.2
120100 UST-DELETE-GF-8-4.                                               NC2184.2
120200     PERFORM DE-LETE.                                             NC2184.2
120300     GO TO UST-WRITE-GF-8-4.                                      NC2184.2
120400 UST-FAIL-GF-8-4.                                                 NC2184.2
120500     PERFORM FAIL                                                 NC2184.2
120600     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
120700     MOVE    4 TO CORRECT-N.                                      NC2184.2
120800 UST-WRITE-GF-8-4.                                                NC2184.2
120900     PERFORM PRINT-DETAIL.                                        NC2184.2
121000*                                                                 NC2184.2
121100 UST-TEST-GF-8-5.                                                 NC2184.2
121200     ADD     1 TO REC-CT.                                         NC2184.2
121300     IF      ID11-DU-2V0 = 1                                      NC2184.2
121400             PERFORM PASS                                         NC2184.2
121500             GO TO UST-WRITE-GF-8-5                               NC2184.2
121600     ELSE                                                         NC2184.2
121700             GO TO UST-FAIL-GF-8-5.                               NC2184.2
121800 UST-DELETE-GF-8-5.                                               NC2184.2
121900     PERFORM DE-LETE.                                             NC2184.2
122000     GO TO UST-WRITE-GF-8-5.                                      NC2184.2
122100 UST-FAIL-GF-8-5.                                                 NC2184.2
122200     PERFORM FAIL                                                 NC2184.2
122300     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
122400     MOVE    1 TO CORRECT-N.                                      NC2184.2
122500 UST-WRITE-GF-8-5.                                                NC2184.2
122600     PERFORM PRINT-DETAIL.                                        NC2184.2
122700*                                                                 NC2184.2
122800 UST-INIT-GF-9.                                                   NC2184.2
122900     MOVE    "UST-TEST-GF-9" TO PAR-NAME.                         NC2184.2
123000     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
123100     MOVE    "PIC S9 TRAIL SEP" TO FEATURE.                       NC2184.2
123200     MOVE    "1200000" TO ID1-XN-7.                               NC2184.2
123300     MOVE    ZERO TO ID4-DS-TS-1V0.                               NC2184.2
123400     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
123500     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
123600     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
123700     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
123800     MOVE    1 TO REC-CT.                                         NC2184.2
123900*                                                                 NC2184.2
124000 UST-TEST-GF-9.                                                   NC2184.2
124100     UNSTRING ID1-XN-7 DELIMITED ALL ZERO-XN-1 INTO ID4-DS-TS-1V0 NC2184.2
124200             DELIMITER ID5-XN-4                                   NC2184.2
124300             COUNT ID6-DU-2V0                                     NC2184.2
124400             POINTER ID10-DU-2V0                                  NC2184.2
124500             TALLYING IN ID11-DU-2V0.                             NC2184.2
124600     GO TO UST-TEST-GF-9-1.                                       NC2184.2
124700 UST-DELETE-GF-9.                                                 NC2184.2
124800     PERFORM DE-LETE.                                             NC2184.2
124900     PERFORM PRINT-DETAIL.                                        NC2184.2
125000     GO      TO UST-INIT-GF-10.                                   NC2184.2
125100*                                                                 NC2184.2
125200 UST-TEST-GF-9-1.                                                 NC2184.2
125300     IF      ID4-DS-TS-1V0 = +2                                   NC2184.2
125400             PERFORM PASS                                         NC2184.2
125500             GO TO UST-WRITE-GF-9-1                               NC2184.2
125600     ELSE                                                         NC2184.2
125700             GO TO UST-FAIL-GF-9-1.                               NC2184.2
125800 UST-DELETE-GF-9-1.                                               NC2184.2
125900     PERFORM DE-LETE.                                             NC2184.2
126000     GO TO UST-WRITE-GF-9-1.                                      NC2184.2
126100 UST-FAIL-GF-9-1.                                                 NC2184.2
126200     PERFORM FAIL                                                 NC2184.2
126300     MOVE    ID4-DS-TS-1V0 TO COMPUTED-N                          NC2184.2
126400     MOVE    +2 TO CORRECT-N.                                     NC2184.2
126500 UST-WRITE-GF-9-1.                                                NC2184.2
126600     PERFORM PRINT-DETAIL.                                        NC2184.2
126700*                                                                 NC2184.2
126800 UST-TEST-GF-9-2.                                                 NC2184.2
126900     ADD     1 TO REC-CT.                                         NC2184.2
127000     IF      ID5-XN-4 = "0   "                                    NC2184.2
127100             PERFORM PASS                                         NC2184.2
127200             GO TO UST-WRITE-GF-9-2                               NC2184.2
127300     ELSE                                                         NC2184.2
127400             GO TO UST-FAIL-GF-9-2.                               NC2184.2
127500 UST-DELETE-GF-9-2.                                               NC2184.2
127600     PERFORM DE-LETE.                                             NC2184.2
127700     GO TO UST-WRITE-GF-9-2.                                      NC2184.2
127800 UST-FAIL-GF-9-2.                                                 NC2184.2
127900     PERFORM FAIL                                                 NC2184.2
128000     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
128100     MOVE    "0   " TO CORRECT-A.                                 NC2184.2
128200 UST-WRITE-GF-9-2.                                                NC2184.2
128300     PERFORM PRINT-DETAIL.                                        NC2184.2
128400*                                                                 NC2184.2
128500 UST-TEST-GF-9-3.                                                 NC2184.2
128600     IF      ID6-DU-2V0 = 2                                       NC2184.2
128700             PERFORM PASS                                         NC2184.2
128800             GO TO UST-WRITE-GF-9-3                               NC2184.2
128900     ELSE                                                         NC2184.2
129000             GO TO UST-FAIL-GF-9-3.                               NC2184.2
129100 UST-DELETE-GF-9-3.                                               NC2184.2
129200     PERFORM DE-LETE.                                             NC2184.2
129300     GO TO UST-WRITE-GF-9-3.                                      NC2184.2
129400 UST-FAIL-GF-9-3.                                                 NC2184.2
129500     PERFORM FAIL                                                 NC2184.2
129600     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
129700     MOVE    2 TO CORRECT-N.                                      NC2184.2
129800 UST-WRITE-GF-9-3.                                                NC2184.2
129900     PERFORM PRINT-DETAIL.                                        NC2184.2
130000*                                                                 NC2184.2
130100 UST-TEST-GF-9-4.                                                 NC2184.2
130200     ADD     1 TO REC-CT.                                         NC2184.2
130300     IF      ID10-DU-2V0 = 8                                      NC2184.2
130400             PERFORM PASS                                         NC2184.2
130500             GO TO UST-WRITE-GF-9-4                               NC2184.2
130600     ELSE                                                         NC2184.2
130700             GO TO UST-FAIL-GF-9-4.                               NC2184.2
130800 UST-DELETE-GF-9-4.                                               NC2184.2
130900     PERFORM DE-LETE.                                             NC2184.2
131000     GO TO UST-WRITE-GF-9-4.                                      NC2184.2
131100 UST-FAIL-GF-9-4.                                                 NC2184.2
131200     PERFORM FAIL                                                 NC2184.2
131300     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
131400     MOVE    8 TO CORRECT-N.                                      NC2184.2
131500 UST-WRITE-GF-9-4.                                                NC2184.2
131600     PERFORM PRINT-DETAIL.                                        NC2184.2
131700*                                                                 NC2184.2
131800 UST-TEST-GF-9-5.                                                 NC2184.2
131900     ADD     1 TO REC-CT.                                         NC2184.2
132000     IF      ID11-DU-2V0 = 1                                      NC2184.2
132100             PERFORM PASS                                         NC2184.2
132200             GO TO UST-WRITE-GF-9-5                               NC2184.2
132300     ELSE                                                         NC2184.2
132400             GO TO UST-FAIL-GF-9-5.                               NC2184.2
132500 UST-DELETE-GF-9-5.                                               NC2184.2
132600     PERFORM DE-LETE.                                             NC2184.2
132700     GO TO UST-WRITE-GF-9-5.                                      NC2184.2
132800 UST-FAIL-GF-9-5.                                                 NC2184.2
132900     PERFORM FAIL                                                 NC2184.2
133000     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
133100     MOVE    1 TO CORRECT-N.                                      NC2184.2
133200 UST-WRITE-GF-9-5.                                                NC2184.2
133300     PERFORM PRINT-DETAIL.                                        NC2184.2
133400*                                                                 NC2184.2
133500 UST-INIT-GF-10.                                                  NC2184.2
133600     MOVE    "UST-TEST-GF-10" TO PAR-NAME.                        NC2184.2
133700     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
133800     MOVE    "PIC S9 LEAD SEP" TO FEATURE.                        NC2184.2
133900     MOVE    "1200000" TO ID1-XN-7.                               NC2184.2
134000     MOVE    ZERO TO ID4-DS-LS-1V0.                               NC2184.2
134100     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
134200     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
134300     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
134400     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
134500     MOVE    1 TO REC-CT.                                         NC2184.2
134600*                                                                 NC2184.2
134700 UST-TEST-GF-10.                                                  NC2184.2
134800     UNSTRING ID1-XN-7 DELIMITED BY "0" INTO ID4-DS-LS-1V0        NC2184.2
134900             DELIMITER ID5-XN-4                                   NC2184.2
135000             COUNT ID6-DU-2V0                                     NC2184.2
135100             POINTER ID10-DU-2V0                                  NC2184.2
135200             TALLYING ID11-DU-2V0.                                NC2184.2
135300     GO TO UST-TEST-GF-10-1.                                      NC2184.2
135400 UST-DELETE-GF-10.                                                NC2184.2
135500     PERFORM DE-LETE.                                             NC2184.2
135600     PERFORM PRINT-DETAIL.                                        NC2184.2
135700     GO      TO UST-INIT-GF-11.                                   NC2184.2
135800*                                                                 NC2184.2
135900 UST-TEST-GF-10-1.                                                NC2184.2
136000     IF      ID4-DS-LS-1V0 = +2                                   NC2184.2
136100             PERFORM PASS                                         NC2184.2
136200             GO TO UST-WRITE-GF-10-1                              NC2184.2
136300     ELSE                                                         NC2184.2
136400             GO TO UST-FAIL-GF-10-1.                              NC2184.2
136500 UST-DELETE-GF-10-1.                                              NC2184.2
136600     PERFORM DE-LETE.                                             NC2184.2
136700     GO TO UST-WRITE-GF-10-1.                                     NC2184.2
136800 UST-FAIL-GF-10-1.                                                NC2184.2
136900     PERFORM FAIL                                                 NC2184.2
137000     MOVE    ID4-DS-LS-1V0 TO COMPUTED-N                          NC2184.2
137100     MOVE    +2 TO CORRECT-N.                                     NC2184.2
137200 UST-WRITE-GF-10-1.                                               NC2184.2
137300     PERFORM PRINT-DETAIL.                                        NC2184.2
137400*                                                                 NC2184.2
137500 UST-TEST-GF-10-2.                                                NC2184.2
137600     ADD     1 TO REC-CT.                                         NC2184.2
137700     IF      ID5-XN-4 = "0   "                                    NC2184.2
137800             PERFORM PASS                                         NC2184.2
137900             GO TO UST-WRITE-GF-10-2                              NC2184.2
138000     ELSE                                                         NC2184.2
138100             GO TO UST-FAIL-GF-10-2.                              NC2184.2
138200 UST-DELETE-GF-10-2.                                              NC2184.2
138300     PERFORM DE-LETE.                                             NC2184.2
138400     GO TO UST-WRITE-GF-10-2.                                     NC2184.2
138500 UST-FAIL-GF-10-2.                                                NC2184.2
138600     PERFORM FAIL                                                 NC2184.2
138700     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
138800     MOVE    "0   " TO CORRECT-A.                                 NC2184.2
138900 UST-WRITE-GF-10-2.                                               NC2184.2
139000     PERFORM PRINT-DETAIL.                                        NC2184.2
139100*                                                                 NC2184.2
139200 UST-TEST-GF-10-3.                                                NC2184.2
139300     ADD     1 TO REC-CT.                                         NC2184.2
139400     IF      ID6-DU-2V0 = 2                                       NC2184.2
139500             PERFORM PASS                                         NC2184.2
139600             GO TO UST-WRITE-GF-10-3                              NC2184.2
139700     ELSE                                                         NC2184.2
139800             GO TO UST-FAIL-GF-10-3.                              NC2184.2
139900 UST-DELETE-GF-10-3.                                              NC2184.2
140000     PERFORM DE-LETE.                                             NC2184.2
140100     GO TO UST-WRITE-GF-10-3.                                     NC2184.2
140200 UST-FAIL-GF-10-3.                                                NC2184.2
140300     PERFORM FAIL                                                 NC2184.2
140400     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
140500     MOVE    2 TO CORRECT-N.                                      NC2184.2
140600 UST-WRITE-GF-10-3.                                               NC2184.2
140700     PERFORM PRINT-DETAIL.                                        NC2184.2
140800*                                                                 NC2184.2
140900 UST-TEST-GF-10-4.                                                NC2184.2
141000     ADD     1 TO REC-CT.                                         NC2184.2
141100     IF      ID10-DU-2V0 = 4                                      NC2184.2
141200             PERFORM PASS                                         NC2184.2
141300             GO TO UST-WRITE-GF-10-4                              NC2184.2
141400     ELSE                                                         NC2184.2
141500             GO TO UST-FAIL-GF-10-4.                              NC2184.2
141600 UST-DELETE-GF-10-4.                                              NC2184.2
141700     PERFORM DE-LETE.                                             NC2184.2
141800     GO TO UST-WRITE-GF-10-4.                                     NC2184.2
141900 UST-FAIL-GF-10-4.                                                NC2184.2
142000     PERFORM FAIL                                                 NC2184.2
142100     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
142200     MOVE    4 TO CORRECT-N.                                      NC2184.2
142300 UST-WRITE-GF-10-4.                                               NC2184.2
142400     PERFORM PRINT-DETAIL.                                        NC2184.2
142500*                                                                 NC2184.2
142600 UST-TEST-GF-10-5.                                                NC2184.2
142700     ADD     1 TO REC-CT.                                         NC2184.2
142800     IF      ID11-DU-2V0 = 1                                      NC2184.2
142900             PERFORM PASS                                         NC2184.2
143000             GO TO UST-WRITE-GF-10-5                              NC2184.2
143100     ELSE                                                         NC2184.2
143200             GO TO UST-FAIL-GF-10-5.                              NC2184.2
143300 UST-DELETE-GF-10-5.                                              NC2184.2
143400     PERFORM DE-LETE.                                             NC2184.2
143500     GO TO UST-WRITE-GF-10-5.                                     NC2184.2
143600 UST-FAIL-GF-10-5.                                                NC2184.2
143700     PERFORM FAIL                                                 NC2184.2
143800     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
143900     MOVE    1 TO CORRECT-N.                                      NC2184.2
144000 UST-WRITE-GF-10-5.                                               NC2184.2
144100     PERFORM PRINT-DETAIL.                                        NC2184.2
144200*                                                                 NC2184.2
144300 UST-INIT-GF-11.                                                  NC2184.2
144400     MOVE    "UST-TEST-GF-11" TO PAR-NAME.                        NC2184.2
144500     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
144600     MOVE    "GROUP BOTTOM UP" TO FEATURE.                        NC2184.2
144700     MOVE    "ABCDEFGHIJ" TO GRP1-XN-10.                          NC2184.2
144800     MOVE    SPACES TO GRP4-XN-10.                                NC2184.2
144900     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
145000     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
145100     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
145200     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
145300     MOVE    ZERO TO REC-CT.                                      NC2184.2
145400*                                                                 NC2184.2
145500 UST-TEST-GF-11.                                                  NC2184.2
145600     UNSTRING GRP1-XN-10 INTO ID4D-XXXX ID4C-XXX ID4B-XX ID4A-X.  NC2184.2
145700     IF      GRP4-XN-10 = "JHIEFGABCD"                            NC2184.2
145800             PERFORM PASS                                         NC2184.2
145900             GO TO UST-WRITE-GF-11-1                              NC2184.2
146000     ELSE                                                         NC2184.2
146100             GO TO UST-FAIL-GF-11-1.                              NC2184.2
146200 UST-DELETE-GF-11-1.                                              NC2184.2
146300     PERFORM DE-LETE.                                             NC2184.2
146400     GO TO UST-WRITE-GF-11-1.                                     NC2184.2
146500 UST-FAIL-GF-11-1.                                                NC2184.2
146600     PERFORM FAIL                                                 NC2184.2
146700     MOVE    GRP4-XN-10 TO COMPUTED-A                             NC2184.2
146800     MOVE    "JHIEFGABCD" TO CORRECT-A.                           NC2184.2
146900 UST-WRITE-GF-11-1.                                               NC2184.2
147000     PERFORM PRINT-DETAIL.                                        NC2184.2
147100*                                                                 NC2184.2
147200 UST-INIT-GF-12.                                                  NC2184.2
147300     MOVE   "UST-TEST-GF-12" TO PAR-NAME.                         NC2184.2
147400     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
147500     MOVE   "OVERFLOW TEST" TO FEATURE.                           NC2184.2
147600     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
147700     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
147800     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
147900     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
148000     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
148100     MOVE    "ABCDEF" TO GRP1-XN-6.                               NC2184.2
148200     MOVE    1 TO REC-CT.                                         NC2184.2
148300*                                                                 NC2184.2
148400 UST-TEST-GF-12-1.                                                NC2184.2
148500     UNSTRING GRP1-XN-6  INTO ID4A-XXXXX ID4B-X                   NC2184.2
148600             ON OVERFLOW GO TO UST-FAIL-GF-12-1.                  NC2184.2
148700     PERFORM PASS.                                                NC2184.2
148800     GO TO UST-WRITE-GF-12-1.                                     NC2184.2
148900 UST-DELETE-GF-12-1.                                              NC2184.2
149000     PERFORM DE-LETE.                                             NC2184.2
149100     PERFORM PRINT-DETAIL.                                        NC2184.2
149200     GO      TO UST-INIT-GF-13.                                   NC2184.2
149300 UST-FAIL-GF-12-1.                                                NC2184.2
149400     PERFORM FAIL.                                                NC2184.2
149500     MOVE    "OVERFLOW SHOULD NOT HAVE OCCURED" TO RE-MARK.       NC2184.2
149600 UST-WRITE-GF-12-1.                                               NC2184.2
149700     PERFORM PRINT-DETAIL.                                        NC2184.2
149800*                                                                 NC2184.2
149900 UST-TEST-GF-12-2.                                                NC2184.2
150000     MOVE    "UST-TEST-GF-12" TO PAR-NAME.                        NC2184.2
150100     ADD     1 TO REC-CT.                                         NC2184.2
150200     IF      GRP4-XN-6 = "ABCDEF"                                 NC2184.2
150300             PERFORM PASS                                         NC2184.2
150400             GO TO UST-WRITE-GF-12-2                              NC2184.2
150500     ELSE                                                         NC2184.2
150600             GO TO UST-FAIL-GF-12-2.                              NC2184.2
150700 UST-DELETE-GF-12-2.                                              NC2184.2
150800     PERFORM DE-LETE.                                             NC2184.2
150900     GO TO UST-WRITE-GF-12-2.                                     NC2184.2
151000 UST-FAIL-GF-12-2.                                                NC2184.2
151100     PERFORM FAIL                                                 NC2184.2
151200     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
151300     MOVE    "ABCDEF" TO CORRECT-A.                               NC2184.2
151400 UST-WRITE-GF-12-2.                                               NC2184.2
151500     PERFORM PRINT-DETAIL.                                        NC2184.2
151600*                                                                 NC2184.2
151700 UST-INIT-GF-13.                                                  NC2184.2
151800     MOVE    "UST-TEST-GF-13" TO PAR-NAME.                        NC2184.2
151900     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
152000     MOVE    "OVERFLOW EXPECTED" TO FEATURE.                      NC2184.2
152100     MOVE    "ABCDEF" TO GRP1-XN-6.                               NC2184.2
152200     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
152300     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
152400     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
152500     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
152600     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
152700     MOVE    1 TO REC-CT.                                         NC2184.2
152800 UST-TEST-GF-13-1.                                                NC2184.2
152900     UNSTRING GRP1-XN-6 INTO ID4A-XXXXX OVERFLOW PERFORM PASS     NC2184.2
153000                                        GO TO UST-WRITE-GF-13-1.  NC2184.2
153100     GO      TO UST-FAIL-GF-13-1.                                 NC2184.2
153200 UST-DELETE-GF-13-1.                                              NC2184.2
153300     PERFORM DE-LETE.                                             NC2184.2
153400     PERFORM PRINT-DETAIL.                                        NC2184.2
153500     GO      TO UST-INIT-GF-14.                                   NC2184.2
153600 UST-FAIL-GF-13-1.                                                NC2184.2
153700     PERFORM FAIL.                                                NC2184.2
153800     MOVE    "OVERFLOW SHOULD HAVE OCCURED" TO RE-MARK.           NC2184.2
153900 UST-WRITE-GF-13-1.                                               NC2184.2
154000     PERFORM PRINT-DETAIL.                                        NC2184.2
154100*                                                                 NC2184.2
154200 UST-TEST-GF-13-2.                                                NC2184.2
154300     ADD     1 TO REC-CT.                                         NC2184.2
154400     IF      GRP4-XN-6 = "ABCDE "                                 NC2184.2
154500             PERFORM PASS                                         NC2184.2
154600             GO TO UST-WRITE-GF-13-2                              NC2184.2
154700     ELSE                                                         NC2184.2
154800             GO TO UST-FAIL-GF-13-2.                              NC2184.2
154900 UST-DELETE-GF-13-2.                                              NC2184.2
155000     PERFORM DE-LETE.                                             NC2184.2
155100     GO TO UST-WRITE-GF-13-2.                                     NC2184.2
155200 UST-FAIL-GF-13-2.                                                NC2184.2
155300     PERFORM FAIL                                                 NC2184.2
155400     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
155500     MOVE    "ABCDE " TO CORRECT-A.                               NC2184.2
155600 UST-WRITE-GF-13-2.                                               NC2184.2
155700     PERFORM PRINT-DETAIL.                                        NC2184.2
155800*                                                                 NC2184.2
155900 UST-INIT-GF-14.                                                  NC2184.2
156000     MOVE    "UST-TEST-GF-14" TO PAR-NAME.                        NC2184.2
156100     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
156200     MOVE    "INSTANT OVERFLOW" TO FEATURE.                       NC2184.2
156300     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
156400     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
156500     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
156600     MOVE    7 TO ID10-DU-2V0.                                    NC2184.2
156700     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
156800     MOVE    1 TO REC-CT.                                         NC2184.2
156900*                                                                 NC2184.2
157000 UST-TEST-GF-14-1.                                                NC2184.2
157100     UNSTRING GRP1-XN-6 INTO ID4A-XXXXX ID4B-X POINTER ID10-DU-2V0NC2184.2
157200             OVERFLOW PERFORM PASS                                NC2184.2
157300                      GO TO UST-WRITE-GF-14-1.                    NC2184.2
157400     GO      TO UST-FAIL-GF-14-1.                                 NC2184.2
157500 UST-DELETE-GF-14-1.                                              NC2184.2
157600     PERFORM DE-LETE.                                             NC2184.2
157700     PERFORM PRINT-DETAIL.                                        NC2184.2
157800     GO      TO UST-INIT-GF-15.                                   NC2184.2
157900 UST-FAIL-GF-14-1.                                                NC2184.2
158000     PERFORM FAIL.                                                NC2184.2
158100     MOVE    "OVERFLOW SHOULD HAVE OCCURED" TO RE-MARK.           NC2184.2
158200 UST-WRITE-GF-14-1.                                               NC2184.2
158300     PERFORM PRINT-DETAIL.                                        NC2184.2
158400*                                                                 NC2184.2
158500 UST-TEST-GF-14-2.                                                NC2184.2
158600     MOVE    "UST-TEST-GF-14" TO PAR-NAME.                        NC2184.2
158700     ADD     1 TO REC-CT.                                         NC2184.2
158800     IF      GRP4-XN-6 = SPACES                                   NC2184.2
158900             PERFORM PASS                                         NC2184.2
159000             GO TO UST-WRITE-GF-14-2                              NC2184.2
159100     ELSE                                                         NC2184.2
159200             GO TO UST-FAIL-GF-14-2.                              NC2184.2
159300 UST-DELETE-GF-14-2.                                              NC2184.2
159400     PERFORM DE-LETE.                                             NC2184.2
159500     GO TO UST-WRITE-GF-14-2.                                     NC2184.2
159600 UST-FAIL-GF-14-2.                                                NC2184.2
159700     PERFORM FAIL                                                 NC2184.2
159800     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
159900     MOVE    "ALL SPACES" TO CORRECT-A.                           NC2184.2
160000 UST-WRITE-GF-14-2.                                               NC2184.2
160100     PERFORM PRINT-DETAIL.                                        NC2184.2
160200*                                                                 NC2184.2
160300 UST-TEST-GF-14-3.                                                NC2184.2
160400     ADD     1 TO REC-CT.                                         NC2184.2
160500     IF      ID10-DU-2V0 = 7                                      NC2184.2
160600             PERFORM PASS                                         NC2184.2
160700             GO TO UST-WRITE-GF-14-3                              NC2184.2
160800     ELSE                                                         NC2184.2
160900             GO TO UST-FAIL-GF-14-3.                              NC2184.2
161000 UST-DELETE-GF-14-3.                                              NC2184.2
161100     PERFORM DE-LETE.                                             NC2184.2
161200     GO TO UST-WRITE-GF-14-3.                                     NC2184.2
161300 UST-FAIL-GF-14-3.                                                NC2184.2
161400     PERFORM FAIL                                                 NC2184.2
161500     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
161600     MOVE    7 TO CORRECT-N.                                      NC2184.2
161700 UST-WRITE-GF-14-3.                                               NC2184.2
161800     PERFORM PRINT-DETAIL.                                        NC2184.2
161900*                                                                 NC2184.2
162000 UST-INIT-GF-15.                                                  NC2184.2
162100     MOVE    "UST-TEST-GF-15" TO PAR-NAME.                        NC2184.2
162200     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
162300     MOVE    "POINTER NOT = 1" TO FEATURE.                        NC2184.2
162400     MOVE    "ABCDEF" TO GRP1-XN-6.                               NC2184.2
162500     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
162600     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
162700     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
162800     MOVE    3 TO ID10-DU-2V0.                                    NC2184.2
162900     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
163000     MOVE    1 TO REC-CT.                                         NC2184.2
163100*                                                                 NC2184.2
163200 UST-TEST-GF-15-1.                                                NC2184.2
163300     UNSTRING GRP1-XN-6 INTO ID4A-XXXXX ID4B-X POINTER ID10-DU-2V0NC2184.2
163400             OVERFLOW GO TO UST-FAIL-GF-15-1.                     NC2184.2
163500     PERFORM PASS                                                 NC2184.2
163600     GO TO UST-WRITE-GF-15-1.                                     NC2184.2
163700 UST-DELETE-GF-15-1.                                              NC2184.2
163800     PERFORM DE-LETE.                                             NC2184.2
163900     PERFORM PRINT-DETAIL.                                        NC2184.2
164000     GO      TO UST-INIT-GF-16.                                   NC2184.2
164100 UST-FAIL-GF-15-1.                                                NC2184.2
164200     PERFORM FAIL.                                                NC2184.2
164300     MOVE    "OVERFLOW SHOULD NOT HAVE OCCURED" TO RE-MARK.       NC2184.2
164400 UST-WRITE-GF-15-1.                                               NC2184.2
164500     PERFORM PRINT-DETAIL.                                        NC2184.2
164600*                                                                 NC2184.2
164700 UST-TEST-GF-15-2.                                                NC2184.2
164800     ADD     1 TO REC-CT.                                         NC2184.2
164900     IF      GRP4-XN-6 = "CDEF  "                                 NC2184.2
165000             PERFORM PASS                                         NC2184.2
165100             GO TO UST-WRITE-GF-15-2                              NC2184.2
165200     ELSE                                                         NC2184.2
165300             GO TO UST-FAIL-GF-15-2.                              NC2184.2
165400 UST-DELETE-GF-15-2.                                              NC2184.2
165500     PERFORM DE-LETE.                                             NC2184.2
165600     GO TO UST-WRITE-GF-15-2.                                     NC2184.2
165700 UST-FAIL-GF-15-2.                                                NC2184.2
165800     PERFORM FAIL                                                 NC2184.2
165900     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
166000     MOVE    "CDEF  " TO CORRECT-A.                               NC2184.2
166100 UST-WRITE-GF-15-2.                                               NC2184.2
166200     PERFORM PRINT-DETAIL.                                        NC2184.2
166300*                                                                 NC2184.2
166400 UST-TEST-GF-15-3.                                                NC2184.2
166500     ADD     1 TO REC-CT.                                         NC2184.2
166600     IF      ID10-DU-2V0 = 7                                      NC2184.2
166700             PERFORM PASS                                         NC2184.2
166800             GO TO UST-WRITE-GF-15-3                              NC2184.2
166900     ELSE                                                         NC2184.2
167000             GO TO UST-FAIL-GF-15-3.                              NC2184.2
167100 UST-DELETE-GF-15-3.                                              NC2184.2
167200     PERFORM DE-LETE.                                             NC2184.2
167300     GO TO UST-WRITE-GF-15-3.                                     NC2184.2
167400 UST-FAIL-GF-15-3.                                                NC2184.2
167500     PERFORM FAIL                                                 NC2184.2
167600     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
167700     MOVE    7 TO CORRECT-N.                                      NC2184.2
167800 UST-WRITE-GF-15-3.                                               NC2184.2
167900     PERFORM PRINT-DETAIL.                                        NC2184.2
168000*                                                                 NC2184.2
168100 UST-INIT-GF-16.                                                  NC2184.2
168200     MOVE    "UST-TEST-GF-16" TO PAR-NAME.                        NC2184.2
168300     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
168400     MOVE    "TALLY 3 FIELDS TEST" TO FEATURE.                    NC2184.2
168500     MOVE    "ABCDEF" TO GRP1-XN-6.                               NC2184.2
168600     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
168700     MOVE    "****" TO ID4C-XXXX.                                 NC2184.2
168800     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
168900     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
169000     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
169100     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
169200     MOVE    1 TO REC-CT.                                         NC2184.2
169300*                                                                 NC2184.2
169400 UST-TEST-GF-16-0.                                                NC2184.2
169500     UNSTRING GRP1-XN-6 INTO ID4A-XXXXX ID4B-X ID4C-XXXX          NC2184.2
169600             TALLYING ID11-DU-2V0.                                NC2184.2
169700     GO      TO UST-TEST-GF-16-1.                                 NC2184.2
169800 UST-DELETE-GF-16.                                                NC2184.2
169900     PERFORM DE-LETE.                                             NC2184.2
170000     PERFORM PRINT-DETAIL.                                        NC2184.2
170100     GO      TO UST-INIT-GF-17.                                   NC2184.2
170200*                                                                 NC2184.2
170300 UST-TEST-GF-16-1.                                                NC2184.2
170400     IF      GRP4-XN-6 = "ABCDEF"                                 NC2184.2
170500             PERFORM PASS                                         NC2184.2
170600             GO TO UST-WRITE-GF-16-1                              NC2184.2
170700     ELSE                                                         NC2184.2
170800             GO TO UST-FAIL-GF-16-1.                              NC2184.2
170900 UST-DELETE-GF-16-1.                                              NC2184.2
171000     PERFORM DE-LETE.                                             NC2184.2
171100     GO TO UST-WRITE-GF-16-1.                                     NC2184.2
171200 UST-FAIL-GF-16-1.                                                NC2184.2
171300     PERFORM FAIL                                                 NC2184.2
171400     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
171500     MOVE    "ABCDEF" TO CORRECT-A.                               NC2184.2
171600 UST-WRITE-GF-16-1.                                               NC2184.2
171700     PERFORM PRINT-DETAIL.                                        NC2184.2
171800*                                                                 NC2184.2
171900 UST-TEST-GF-16-2.                                                NC2184.2
172000     ADD     1 TO REC-CT.                                         NC2184.2
172100     IF      ID4C-XXXX = "****"                                   NC2184.2
172200             PERFORM PASS                                         NC2184.2
172300             GO TO UST-WRITE-GF-16-2                              NC2184.2
172400     ELSE                                                         NC2184.2
172500             GO TO UST-FAIL-GF-16-2.                              NC2184.2
172600 UST-DELETE-GF-16-2.                                              NC2184.2
172700     PERFORM DE-LETE.                                             NC2184.2
172800     GO TO UST-WRITE-GF-16-2.                                     NC2184.2
172900 UST-FAIL-GF-16-2.                                                NC2184.2
173000     PERFORM FAIL                                                 NC2184.2
173100     MOVE    ID4C-XXXX TO COMPUTED-A                              NC2184.2
173200     MOVE    "****" TO CORRECT-A.                                 NC2184.2
173300 UST-WRITE-GF-16-2.                                               NC2184.2
173400     PERFORM PRINT-DETAIL.                                        NC2184.2
173500*                                                                 NC2184.2
173600 UST-TEST-GF-16-3.                                                NC2184.2
173700     ADD     1 TO REC-CT.                                         NC2184.2
173800     IF      ID11-DU-2V0 = 2                                      NC2184.2
173900             PERFORM PASS                                         NC2184.2
174000             GO TO UST-WRITE-GF-16-3                              NC2184.2
174100     ELSE                                                         NC2184.2
174200             GO TO UST-FAIL-GF-16-3.                              NC2184.2
174300 UST-DELETE-GF-16-3.                                              NC2184.2
174400     PERFORM DE-LETE.                                             NC2184.2
174500     GO TO UST-WRITE-GF-16-3.                                     NC2184.2
174600 UST-FAIL-GF-16-3.                                                NC2184.2
174700     PERFORM FAIL                                                 NC2184.2
174800     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
174900     MOVE    2 TO CORRECT-N.                                      NC2184.2
175000 UST-WRITE-GF-16-3.                                               NC2184.2
175100     PERFORM PRINT-DETAIL.                                        NC2184.2
175200*                                                                 NC2184.2
175300 UST-INIT-GF-17.                                                  NC2184.2
175400     MOVE    "UST-TEST-GF-17" TO PAR-NAME.                        NC2184.2
175500     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
175600     MOVE    "QUAL DEL BY POINT" TO FEATURE.                      NC2184.2
175700     MOVE    "ABCDEFG" TO GRP1-XN-7.                              NC2184.2
175800     MOVE    "BCDEFGH" TO GRP2-XN-7.                              NC2184.2
175900     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
176000     MOVE    ALL "*" TO ID5-XN-6.                                 NC2184.2
176100     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
176200     MOVE    2 TO ID10-DU-2V0.                                    NC2184.2
176300     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
176400     MOVE    1 TO REC-CT.                                         NC2184.2
176500*                                                                 NC2184.2
176600 UST-TEST-GF-17.                                                  NC2184.2
176700     UNSTRING GRP1-XN-7 DELIMITED BY ID2 (ID10-DU-2V0)            NC2184.2
176800             INTO GRP4-XN-6                                       NC2184.2
176900             DELIMITER IN ID5-XN-6                                NC2184.2
177000             COUNT ID6-DU-2V0                                     NC2184.2
177100             POINTER ID10-DU-2V0.                                 NC2184.2
177200     GO TO UST-TEST-GF-17-1.                                      NC2184.2
177300 UST-DELETE-GF-17.                                                NC2184.2
177400     PERFORM DE-LETE.                                             NC2184.2
177500     PERFORM PRINT-DETAIL.                                        NC2184.2
177600     GO      TO UST-INIT-GF-18.                                   NC2184.2
177700*                                                                 NC2184.2
177800 UST-TEST-GF-17-1.                                                NC2184.2
177900     IF      GRP4-XN-6 = "B     "                                 NC2184.2
178000             PERFORM PASS                                         NC2184.2
178100             GO TO UST-WRITE-GF-17-1                              NC2184.2
178200     ELSE                                                         NC2184.2
178300             GO TO UST-FAIL-GF-17-1.                              NC2184.2
178400 UST-DELETE-GF-17-1.                                              NC2184.2
178500     PERFORM DE-LETE.                                             NC2184.2
178600     GO TO UST-WRITE-GF-17-1.                                     NC2184.2
178700 UST-FAIL-GF-17-1.                                                NC2184.2
178800     PERFORM FAIL                                                 NC2184.2
178900     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
179000     MOVE    "B     " TO CORRECT-A.                               NC2184.2
179100 UST-WRITE-GF-17-1.                                               NC2184.2
179200     PERFORM PRINT-DETAIL.                                        NC2184.2
179300*                                                                 NC2184.2
179400 UST-TEST-GF-17-2.                                                NC2184.2
179500     ADD     1 TO REC-CT.                                         NC2184.2
179600     IF      ID5-XN-6 = "C     "                                  NC2184.2
179700             PERFORM PASS                                         NC2184.2
179800             GO TO UST-WRITE-GF-17-2                              NC2184.2
179900     ELSE                                                         NC2184.2
180000             GO TO UST-FAIL-GF-17-2.                              NC2184.2
180100 UST-DELETE-GF-17-2.                                              NC2184.2
180200     PERFORM DE-LETE.                                             NC2184.2
180300     GO TO UST-WRITE-GF-17-2.                                     NC2184.2
180400 UST-FAIL-GF-17-2.                                                NC2184.2
180500     PERFORM FAIL                                                 NC2184.2
180600     MOVE    ID5-XN-6 TO COMPUTED-A                               NC2184.2
180700     MOVE    "C     " TO CORRECT-A.                               NC2184.2
180800 UST-WRITE-GF-17-2.                                               NC2184.2
180900     PERFORM PRINT-DETAIL.                                        NC2184.2
181000*                                                                 NC2184.2
181100 UST-TEST-GF-17-3.                                                NC2184.2
181200     ADD     1 TO REC-CT.                                         NC2184.2
181300     IF      ID6-DU-2V0 = 1                                       NC2184.2
181400             PERFORM PASS                                         NC2184.2
181500             GO TO UST-WRITE-GF-17-3                              NC2184.2
181600     ELSE                                                         NC2184.2
181700             GO TO UST-FAIL-GF-17-3.                              NC2184.2
181800 UST-DELETE-GF-17-3.                                              NC2184.2
181900     PERFORM DE-LETE.                                             NC2184.2
182000     GO TO UST-WRITE-GF-17-3.                                     NC2184.2
182100 UST-FAIL-GF-17-3.                                                NC2184.2
182200     PERFORM FAIL                                                 NC2184.2
182300     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
182400     MOVE    1 TO CORRECT-N.                                      NC2184.2
182500 UST-WRITE-GF-17-3.                                               NC2184.2
182600     PERFORM PRINT-DETAIL.                                        NC2184.2
182700*                                                                 NC2184.2
182800 UST-TEST-GF-17-4.                                                NC2184.2
182900     ADD     1 TO REC-CT.                                         NC2184.2
183000     IF      ID10-DU-2V0 = 4                                      NC2184.2
183100             PERFORM PASS                                         NC2184.2
183200             GO TO UST-WRITE-GF-17-4                              NC2184.2
183300     ELSE                                                         NC2184.2
183400             GO TO UST-FAIL-GF-17-4.                              NC2184.2
183500 UST-DELETE-GF-17-4.                                              NC2184.2
183600     PERFORM DE-LETE.                                             NC2184.2
183700     GO TO UST-WRITE-GF-17-4.                                     NC2184.2
183800 UST-FAIL-GF-17-4.                                                NC2184.2
183900     PERFORM FAIL                                                 NC2184.2
184000     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
184100     MOVE    4 TO CORRECT-N.                                      NC2184.2
184200 UST-WRITE-GF-17-4.                                               NC2184.2
184300     PERFORM PRINT-DETAIL.                                        NC2184.2
184400*                                                                 NC2184.2
184500 UST-INIT-GF-18.                                                  NC2184.2
184600     ADD     1 TO REC-CT.                                         NC2184.2
184700     MOVE    "UST-TEST-GF-18" TO PAR-NAME.                        NC2184.2
184800     MOVE    "VI-136" TO ANSI-REFERENCE.                          NC2184.2
184900     MOVE    "QUAL DEL BY TALLY" TO FEATURE.                      NC2184.2
185000     MOVE    "ABCDEFG" TO GRP1-XN-7.                              NC2184.2
185100     MOVE    "CE" TO GRP2-XN-2.                                   NC2184.2
185200     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
185300     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
185400     MOVE    "****" TO ID5-XN-4-2.                                NC2184.2
185500     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
185600     MOVE    ZERO TO ID6-DU-2V0-2.                                NC2184.2
185700     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
185800     MOVE    1 TO ID11-DU-2V0.                                    NC2184.2
185900     MOVE    1 TO REC-CT.                                         NC2184.2
186000*                                                                 NC2184.2
186100 UST-TEST-GF-18.                                                  NC2184.2
186200     UNSTRING GRP1-XN-7 DELIMITED ID2A (ID10-DU-2V0)              NC2184.2
186300              INTO ID4A-XXXXX                                     NC2184.2
186400              DELIMITER IN ID5-XN-4                               NC2184.2
186500              COUNT ID6-DU-2V0                                    NC2184.2
186600             ID4B-X DELIMITER IN ID5-XN-4-2                       NC2184.2
186700              COUNT ID6-DU-2V0-2                                  NC2184.2
186800              TALLYING ID11-DU-2V0.                               NC2184.2
186900     GO TO UST-TEST-GF-18-1.                                      NC2184.2
187000 UST-DELETE-GF-18.                                                NC2184.2
187100     PERFORM DE-LETE.                                             NC2184.2
187200     PERFORM PRINT-DETAIL.                                        NC2184.2
187300     GO      TO UST-INIT-GF-19.                                   NC2184.2
187400*                                                                 NC2184.2
187500 UST-TEST-GF-18-1.                                                NC2184.2
187600     IF      GRP4-XN-6 = "AB   D"                                 NC2184.2
187700             PERFORM PASS                                         NC2184.2
187800             GO TO UST-WRITE-GF-18-1                              NC2184.2
187900     ELSE                                                         NC2184.2
188000             GO TO UST-FAIL-GF-18-1.                              NC2184.2
188100 UST-DELETE-GF-18-1.                                              NC2184.2
188200     PERFORM DE-LETE.                                             NC2184.2
188300     GO TO UST-WRITE-GF-18-1.                                     NC2184.2
188400 UST-FAIL-GF-18-1.                                                NC2184.2
188500     PERFORM FAIL                                                 NC2184.2
188600     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
188700     MOVE    "AB   D" TO CORRECT-A.                               NC2184.2
188800 UST-WRITE-GF-18-1.                                               NC2184.2
188900     PERFORM PRINT-DETAIL.                                        NC2184.2
189000*                                                                 NC2184.2
189100 UST-TEST-GF-18-2.                                                NC2184.2
189200     ADD     1 TO REC-CT.                                         NC2184.2
189300     IF      ID5-XN-4 = "C   "                                    NC2184.2
189400             PERFORM PASS                                         NC2184.2
189500             GO TO UST-WRITE-GF-18-2                              NC2184.2
189600     ELSE                                                         NC2184.2
189700             GO TO UST-FAIL-GF-18-2.                              NC2184.2
189800 UST-DELETE-GF-18-2.                                              NC2184.2
189900     PERFORM DE-LETE.                                             NC2184.2
190000     GO TO UST-WRITE-GF-18-2.                                     NC2184.2
190100 UST-FAIL-GF-18-2.                                                NC2184.2
190200     PERFORM FAIL                                                 NC2184.2
190300     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
190400     MOVE    "C   " TO CORRECT-A.                                 NC2184.2
190500 UST-WRITE-GF-18-2.                                               NC2184.2
190600     PERFORM PRINT-DETAIL.                                        NC2184.2
190700*                                                                 NC2184.2
190800 UST-TEST-GF-18-3.                                                NC2184.2
190900     ADD     1 TO REC-CT.                                         NC2184.2
191000     IF      ID6-DU-2V0 = 2                                       NC2184.2
191100             PERFORM PASS                                         NC2184.2
191200             GO TO UST-WRITE-GF-18-3                              NC2184.2
191300     ELSE                                                         NC2184.2
191400             GO TO UST-FAIL-GF-18-3.                              NC2184.2
191500 UST-DELETE-GF-18-3.                                              NC2184.2
191600     PERFORM DE-LETE.                                             NC2184.2
191700     GO TO UST-WRITE-GF-18-3.                                     NC2184.2
191800 UST-FAIL-GF-18-3.                                                NC2184.2
191900     PERFORM FAIL                                                 NC2184.2
192000     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
192100     MOVE    2 TO CORRECT-N.                                      NC2184.2
192200 UST-WRITE-GF-18-3.                                               NC2184.2
192300     PERFORM PRINT-DETAIL.                                        NC2184.2
192400*                                                                 NC2184.2
192500 UST-TEST-GF-18-4.                                                NC2184.2
192600     ADD     1 TO REC-CT.                                         NC2184.2
192700     IF      ID6-DU-2V0-2 EQUAL TO 4                              NC2184.2
192800             PERFORM PASS                                         NC2184.2
192900             GO TO UST-WRITE-GF-18-4                              NC2184.2
193000     ELSE                                                         NC2184.2
193100             GO TO UST-FAIL-GF-18-4.                              NC2184.2
193200 UST-DELETE-GF-18-4.                                              NC2184.2
193300     PERFORM DE-LETE.                                             NC2184.2
193400     GO TO UST-WRITE-GF-18-4.                                     NC2184.2
193500 UST-FAIL-GF-18-4.                                                NC2184.2
193600     PERFORM FAIL                                                 NC2184.2
193700     MOVE    ID6-DU-2V0-2 TO COMPUTED-N                           NC2184.2
193800     MOVE    4 TO CORRECT-N.                                      NC2184.2
193900 UST-WRITE-GF-18-4.                                               NC2184.2
194000     PERFORM PRINT-DETAIL.                                        NC2184.2
194100*                                                                 NC2184.2
194200 UST-TEST-GF-18-5.                                                NC2184.2
194300     ADD     1 TO REC-CT.                                         NC2184.2
194400     IF      ID11-DU-2V0 = 3                                      NC2184.2
194500             PERFORM PASS                                         NC2184.2
194600             GO TO UST-WRITE-GF-18-5                              NC2184.2
194700     ELSE                                                         NC2184.2
194800             GO TO UST-FAIL-GF-18-5.                              NC2184.2
194900 UST-DELETE-GF-18-5.                                              NC2184.2
195000     PERFORM DE-LETE.                                             NC2184.2
195100     GO TO UST-WRITE-GF-18-5.                                     NC2184.2
195200 UST-FAIL-GF-18-5.                                                NC2184.2
195300     PERFORM FAIL                                                 NC2184.2
195400     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
195500     MOVE    3 TO CORRECT-N.                                      NC2184.2
195600 UST-WRITE-GF-18-5.                                               NC2184.2
195700     PERFORM PRINT-DETAIL.                                        NC2184.2
195800*                                                                 NC2184.2
195900 UST-TEST-GF-18-6.                                                NC2184.2
196000     ADD     1 TO REC-CT.                                         NC2184.2
196100     IF      ID5-XN-4-2 = SPACES  AND  ID6-DU-2V0-2 = 4           NC2184.2
196200             PERFORM PASS                                         NC2184.2
196300             GO TO UST-WRITE-GF-18-6                              NC2184.2
196400     ELSE                                                         NC2184.2
196500             GO TO UST-FAIL-GF-18-6.                              NC2184.2
196600 UST-DELETE-GF-18-6.                                              NC2184.2
196700     PERFORM DE-LETE.                                             NC2184.2
196800     GO TO UST-WRITE-GF-18-6.                                     NC2184.2
196900 UST-FAIL-GF-18-6.                                                NC2184.2
197000     PERFORM FAIL                                                 NC2184.2
197100     MOVE    ID5-XN-4-2   TO COMPUTED-A                           NC2184.2
197200     MOVE    4 TO CORRECT-A.                                      NC2184.2
197300 UST-WRITE-GF-18-6.                                               NC2184.2
197400     PERFORM PRINT-DETAIL.                                        NC2184.2
197500*                                                                 NC2184.2
197600 UST-INIT-GF-19.                                                  NC2184.2
197700     MOVE "UST-TEST-GF-19" TO PAR-NAME.                           NC2184.2
197800     MOVE "VI-135" TO ANSI-REFERENCE.                             NC2184.2
197900     MOVE "QUALIFIED ID1" TO FEATURE.                             NC2184.2
198000     MOVE "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" TO GRP1-XN-X-36. NC2184.2
198100     MOVE SPACES TO GRP4-XN-6.                                    NC2184.2
198200     MOVE "****" TO ID5-XN-4.                                     NC2184.2
198300     MOVE ZERO TO ID6-DU-2V0.                                     NC2184.2
198400     MOVE 1 TO ID10-DU-2V0.                                       NC2184.2
198500     MOVE ZERO TO ID11-DU-2V0.                                    NC2184.2
198600     MOVE 1 TO REC-CT.                                            NC2184.2
198700*                                                                 NC2184.2
198800 UST-TEST-GF-19-0.                                                NC2184.2
198900     UNSTRING ID1 OF GRP1-XN-X-36 (ID10-DU-2V0) INTO GRP4-XN-6    NC2184.2
199000             POINTER ID10-DU-2V0.                                 NC2184.2
199100     GO TO UST-TEST-GF-19-1.                                      NC2184.2
199200 UST-DELETE-GF-19.                                                NC2184.2
199300     PERFORM DE-LETE.                                             NC2184.2
199400     PERFORM PRINT-DETAIL.                                        NC2184.2
199500     GO TO UST-INIT-GF-20.                                        NC2184.2
199600*                                                                 NC2184.2
199700 UST-TEST-GF-19-1.                                                NC2184.2
199800     IF      GRP4-XN-6 = "ABCDEF"                                 NC2184.2
199900             PERFORM PASS                                         NC2184.2
200000             GO TO UST-WRITE-GF-19-1                              NC2184.2
200100     ELSE                                                         NC2184.2
200200             GO TO UST-FAIL-GF-19-1.                              NC2184.2
200300 UST-DELETE-GF-19-1.                                              NC2184.2
200400     PERFORM DE-LETE.                                             NC2184.2
200500     GO TO UST-WRITE-GF-19-1.                                     NC2184.2
200600 UST-FAIL-GF-19-1.                                                NC2184.2
200700     PERFORM FAIL                                                 NC2184.2
200800     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
200900     MOVE    "ABCDEF" TO CORRECT-A.                               NC2184.2
201000 UST-WRITE-GF-19-1.                                               NC2184.2
201100     PERFORM PRINT-DETAIL.                                        NC2184.2
201200*                                                                 NC2184.2
201300 UST-TEST-GF-19-2.                                                NC2184.2
201400     ADD     1 TO REC-CT.                                         NC2184.2
201500     IF      ID10-DU-2V0 = 7                                      NC2184.2
201600             PERFORM PASS                                         NC2184.2
201700             GO TO UST-WRITE-GF-19-2                              NC2184.2
201800     ELSE                                                         NC2184.2
201900             GO TO UST-FAIL-GF-19-2.                              NC2184.2
202000 UST-DELETE-GF-19-2.                                              NC2184.2
202100     PERFORM DE-LETE.                                             NC2184.2
202200     GO TO UST-WRITE-GF-19-2.                                     NC2184.2
202300 UST-FAIL-GF-19-2.                                                NC2184.2
202400     PERFORM FAIL                                                 NC2184.2
202500     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
202600     MOVE    7 TO CORRECT-N.                                      NC2184.2
202700 UST-WRITE-GF-19-2.                                               NC2184.2
202800     PERFORM PRINT-DETAIL.                                        NC2184.2
202900*                                                                 NC2184.2
203000 UST-TEST-GF-19-3.                                                NC2184.2
203100     ADD     1 TO REC-CT.                                         NC2184.2
203200     IF      ID11-DU-2V0 = ZERO                                   NC2184.2
203300             PERFORM PASS                                         NC2184.2
203400             GO TO UST-WRITE-GF-19-3                              NC2184.2
203500     ELSE                                                         NC2184.2
203600             GO TO UST-FAIL-GF-19-3.                              NC2184.2
203700 UST-DELETE-GF-19-3.                                              NC2184.2
203800     PERFORM DE-LETE.                                             NC2184.2
203900     GO TO UST-WRITE-GF-19-3.                                     NC2184.2
204000 UST-FAIL-GF-19-3.                                                NC2184.2
204100     PERFORM FAIL                                                 NC2184.2
204200     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
204300     MOVE    ZERO TO CORRECT-N.                                   NC2184.2
204400 UST-WRITE-GF-19-3.                                               NC2184.2
204500     PERFORM PRINT-DETAIL.                                        NC2184.2
204600*                                                                 NC2184.2
204700 UST-INIT-GF-20.                                                  NC2184.2
204800     MOVE    "UST-TEST-GF-20" TO PAR-NAME.                        NC2184.2
204900     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
205000     MOVE    "MULT RECEIVE AREAS" TO FEATURE.                     NC2184.2
205100     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
205200     MOVE    "****" TO ID4C-XXXX.                                 NC2184.2
205300     MOVE    "*" TO ID4D-X.                                       NC2184.2
205400     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
205500     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
205600     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
205700     MOVE    1 TO ID11-DU-2V0.                                    NC2184.2
205800     MOVE    1 TO REC-CT.                                         NC2184.2
205900*                                                                 NC2184.2
206000 UST-TEST-GF-20.                                                  NC2184.2
206100     UNSTRING ID1 OF GRP1-XN-X-36 (ID11-DU-2V0)                   NC2184.2
206200              INTO ID4A-XXXXX ID4B-X ID4C-XXXX ID4D-X             NC2184.2
206300              TALLYING ID11-DU-2V0.                               NC2184.2
206400     GO TO UST-TEST-GF-20-1.                                      NC2184.2
206500 UST-DELETE-GF-20.                                                NC2184.2
206600     PERFORM DE-LETE.                                             NC2184.2
206700     PERFORM PRINT-DETAIL.                                        NC2184.2
206800     GO      TO UST-INIT-GF-21.                                   NC2184.2
206900*                                                                 NC2184.2
207000 UST-TEST-GF-20-1.                                                NC2184.2
207100     IF      GRP4-XN-6 = "ABCDEF"                                 NC2184.2
207200             PERFORM PASS                                         NC2184.2
207300             GO TO UST-WRITE-GF-20-1                              NC2184.2
207400     ELSE                                                         NC2184.2
207500             GO TO UST-FAIL-GF-20-1.                              NC2184.2
207600 UST-DELETE-GF-20-1.                                              NC2184.2
207700     PERFORM DE-LETE.                                             NC2184.2
207800     GO TO UST-WRITE-GF-20-1.                                     NC2184.2
207900 UST-FAIL-GF-20-1.                                                NC2184.2
208000     PERFORM FAIL                                                 NC2184.2
208100     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
208200     MOVE    "ABCDEF" TO CORRECT-A.                               NC2184.2
208300 UST-WRITE-GF-20-1.                                               NC2184.2
208400     PERFORM PRINT-DETAIL.                                        NC2184.2
208500*                                                                 NC2184.2
208600 UST-TEST-GF-20-2.                                                NC2184.2
208700     ADD     1 TO REC-CT.                                         NC2184.2
208800     IF      ID4C-XXXX = "****"                                   NC2184.2
208900             PERFORM PASS                                         NC2184.2
209000             GO TO UST-WRITE-GF-20-2                              NC2184.2
209100     ELSE                                                         NC2184.2
209200             GO TO UST-FAIL-GF-20-2.                              NC2184.2
209300 UST-DELETE-GF-20-2.                                              NC2184.2
209400     PERFORM DE-LETE.                                             NC2184.2
209500     GO TO UST-WRITE-GF-20-2.                                     NC2184.2
209600 UST-FAIL-GF-20-2.                                                NC2184.2
209700     PERFORM FAIL                                                 NC2184.2
209800     MOVE    ID4C-XXXX TO COMPUTED-A                              NC2184.2
209900     MOVE    "****" TO CORRECT-A.                                 NC2184.2
210000 UST-WRITE-GF-20-2.                                               NC2184.2
210100     PERFORM PRINT-DETAIL.                                        NC2184.2
210200*                                                                 NC2184.2
210300 UST-TEST-GF-20-3.                                                NC2184.2
210400     ADD     1 TO REC-CT.                                         NC2184.2
210500     IF      ID4D-X = "*"                                         NC2184.2
210600             PERFORM PASS                                         NC2184.2
210700             GO TO UST-WRITE-GF-20-3                              NC2184.2
210800     ELSE                                                         NC2184.2
210900             GO TO UST-FAIL-GF-20-3.                              NC2184.2
211000 UST-DELETE-GF-20-3.                                              NC2184.2
211100     PERFORM DE-LETE.                                             NC2184.2
211200     GO TO UST-WRITE-GF-20-3.                                     NC2184.2
211300 UST-FAIL-GF-20-3.                                                NC2184.2
211400     PERFORM FAIL                                                 NC2184.2
211500     MOVE    ID4D-X TO COMPUTED-A                                 NC2184.2
211600     MOVE    "*" TO CORRECT-A.                                    NC2184.2
211700 UST-WRITE-GF-20-3.                                               NC2184.2
211800     PERFORM PRINT-DETAIL.                                        NC2184.2
211900*                                                                 NC2184.2
212000 UST-TEST-GF-20-4.                                                NC2184.2
212100     ADD     1 TO REC-CT.                                         NC2184.2
212200     IF      ID11-DU-2V0 = 3                                      NC2184.2
212300             PERFORM PASS                                         NC2184.2
212400             GO TO UST-WRITE-GF-20-4                              NC2184.2
212500     ELSE                                                         NC2184.2
212600             GO TO UST-FAIL-GF-20-4.                              NC2184.2
212700 UST-DELETE-GF-20-4.                                              NC2184.2
212800     PERFORM DE-LETE.                                             NC2184.2
212900     GO TO UST-WRITE-GF-20-4.                                     NC2184.2
213000 UST-FAIL-GF-20-4.                                                NC2184.2
213100     PERFORM FAIL                                                 NC2184.2
213200     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
213300     MOVE    3 TO CORRECT-N.                                      NC2184.2
213400 UST-WRITE-GF-20-4.                                               NC2184.2
213500     PERFORM PRINT-DETAIL.                                        NC2184.2
213600*                                                                 NC2184.2
213700 UST-INIT-GF-21.                                                  NC2184.2
213800     MOVE    "UST-TEST-GF-21" TO PAR-NAME.                        NC2184.2
213900     MOVE    "VI-135" TO ANSI-REFERENCE.                          NC2184.2
214000     MOVE    "TRUNCATION CHECKS" TO FEATURE.                      NC2184.2
214100     MOVE    "ABCDEFGHIJKL" TO ID1-XN-12.                         NC2184.2
214200     MOVE    ALL "*" TO GRP4-XN-10.                               NC2184.2
214300     MOVE    1 TO  ID11-DU-2V0.                                   NC2184.2
214400     MOVE    1 TO REC-CT.                                         NC2184.2
214500*                                                                 NC2184.2
214600 UST-TEST-GF-21.                                                  NC2184.2
214700     UNSTRING ID1-XN-12                                           NC2184.2
214800             DELIMITED BY "E" OR "H" OR "K" OR "L"                NC2184.2
214900             INTO ID4C-XXX ID4B-XX ID4A-X                         NC2184.2
215000             TALLYING IN ID11-DU-2V0.                             NC2184.2
215100     GO      TO UST-TEST-GF-21-1.                                 NC2184.2
215200 UNSTRING-DELETE.                                                 NC2184.2
215300     PERFORM  DE-LETE.                                            NC2184.2
215400     PERFORM PRINT-DETAIL.                                        NC2184.2
215500     GO TO UST-INIT-GF-22.                                        NC2184.2
215600*                                                                 NC2184.2
215700 UST-TEST-GF-21-1.                                                NC2184.2
215800     IF      ID4C-XXX = "ABC"                                     NC2184.2
215900             PERFORM PASS                                         NC2184.2
216000             GO TO UST-WRITE-GF-21-1                              NC2184.2
216100     ELSE                                                         NC2184.2
216200             GO TO UST-FAIL-GF-21-1.                              NC2184.2
216300 UST-DELETE-GF-21-1.                                              NC2184.2
216400     PERFORM DE-LETE.                                             NC2184.2
216500     GO TO UST-WRITE-GF-21-1.                                     NC2184.2
216600 UST-FAIL-GF-21-1.                                                NC2184.2
216700     PERFORM FAIL                                                 NC2184.2
216800     MOVE    ID4C-XXX TO COMPUTED-A                               NC2184.2
216900     MOVE    "ABC" TO CORRECT-A.                                  NC2184.2
217000 UST-WRITE-GF-21-1.                                               NC2184.2
217100     PERFORM PRINT-DETAIL.                                        NC2184.2
217200*                                                                 NC2184.2
217300 UST-TEST-GF-21-2.                                                NC2184.2
217400     ADD     1 TO REC-CT.                                         NC2184.2
217500     IF      ID4B-XX = "FG"                                       NC2184.2
217600             PERFORM PASS                                         NC2184.2
217700             GO TO UST-WRITE-GF-21-2                              NC2184.2
217800     ELSE                                                         NC2184.2
217900             GO TO UST-FAIL-GF-21-2.                              NC2184.2
218000 UST-DELETE-GF-21-2.                                              NC2184.2
218100     PERFORM DE-LETE.                                             NC2184.2
218200     GO TO UST-WRITE-GF-21-2.                                     NC2184.2
218300 UST-FAIL-GF-21-2.                                                NC2184.2
218400     PERFORM FAIL                                                 NC2184.2
218500     MOVE    ID4B-XX  TO COMPUTED-A                               NC2184.2
218600     MOVE    "FG" TO CORRECT-A.                                   NC2184.2
218700 UST-WRITE-GF-21-2.                                               NC2184.2
218800     PERFORM PRINT-DETAIL.                                        NC2184.2
218900*                                                                 NC2184.2
219000 UST-TEST-GF-21-3.                                                NC2184.2
219100     ADD     1 TO REC-CT.                                         NC2184.2
219200     IF      ID4A-X = "I"                                         NC2184.2
219300             PERFORM PASS                                         NC2184.2
219400             GO TO UST-WRITE-GF-21-3                              NC2184.2
219500     ELSE                                                         NC2184.2
219600             GO TO UST-FAIL-GF-21-3.                              NC2184.2
219700 UST-DELETE-GF-21-3.                                              NC2184.2
219800     PERFORM DE-LETE.                                             NC2184.2
219900     GO TO UST-WRITE-GF-21-3.                                     NC2184.2
220000 UST-FAIL-GF-21-3.                                                NC2184.2
220100     PERFORM FAIL                                                 NC2184.2
220200     MOVE    ID4A-X   TO COMPUTED-A                               NC2184.2
220300     MOVE    "I" TO CORRECT-A.                                    NC2184.2
220400 UST-WRITE-GF-21-3.                                               NC2184.2
220500     PERFORM PRINT-DETAIL.                                        NC2184.2
220600*                                                                 NC2184.2
220700 UST-TEST-GF-21-4.                                                NC2184.2
220800     ADD     1 TO REC-CT.                                         NC2184.2
220900     IF       ID11-DU-2V0 = 4                                     NC2184.2
221000             PERFORM PASS                                         NC2184.2
221100             GO TO UST-WRITE-GF-21-4                              NC2184.2
221200     ELSE                                                         NC2184.2
221300             GO TO UST-FAIL-GF-21-4.                              NC2184.2
221400 UST-DELETE-GF-21-4.                                              NC2184.2
221500     PERFORM DE-LETE.                                             NC2184.2
221600     GO TO UST-WRITE-GF-21-4.                                     NC2184.2
221700 UST-FAIL-GF-21-4.                                                NC2184.2
221800     PERFORM FAIL                                                 NC2184.2
221900     MOVE     ID11-DU-2V0 TO COMPUTED-N                           NC2184.2
222000     MOVE    4 TO CORRECT-N.                                      NC2184.2
222100 UST-WRITE-GF-21-4.                                               NC2184.2
222200     PERFORM PRINT-DETAIL.                                        NC2184.2
222300*                                                                 NC2184.2
222400 UST-INIT-GF-22.                                                  NC2184.2
222500*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2184.2
222600     MOVE   "VI-138 6.26.4 GR17" TO ANSI-REFERENCE.               NC2184.2
222700     MOVE   "UST-TEST-GF-22" TO PAR-NAME.                         NC2184.2
222800     MOVE   "PIC X " TO FEATURE.                                  NC2184.2
222900     MOVE    ZERO TO ID4-X.                                       NC2184.2
223000     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
223100     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
223200     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
223300     MOVE ZERO TO ID11-DU-2V0.                                    NC2184.2
223400     MOVE    1 TO REC-CT.                                         NC2184.2
223500*                                                                 NC2184.2
223600 UST-TEST-GF-22-1.                                                NC2184.2
223700     UNSTRING ID1-XN-7 DELIMITED BY ZERO INTO ID4-X DELIMITER IN  NC2184.2
223800             ID5-XN-4 COUNT IN ID6-DU-2V0 WITH POINTER ID10-DU-2V0NC2184.2
223900             TALLYING ID11-DU-2V0                                 NC2184.2
224000         NOT ON OVERFLOW  GO TO UST-FAIL-GF-22-1.                 NC2184.2
224100     PERFORM PASS.                                                NC2184.2
224200     GO TO UST-WRITE-GF-22-1.                                     NC2184.2
224300 UST-DELETE-GF-22-1.                                              NC2184.2
224400     PERFORM DE-LETE.                                             NC2184.2
224500     PERFORM PRINT-DETAIL.                                        NC2184.2
224600     GO TO   UST-INIT-GF-23.                                      NC2184.2
224700 UST-FAIL-GF-22-1.                                                NC2184.2
224800     PERFORM FAIL.                                                NC2184.2
224900     MOVE   "NOT ON OVERFLOW SHOULD NOT HAVE EXECUTED" TO RE-MARK.NC2184.2
225000 UST-WRITE-GF-22-1.                                               NC2184.2
225100     PERFORM PRINT-DETAIL.                                        NC2184.2
225200*                                                                 NC2184.2
225300 UST-TEST-GF-22-2.                                                NC2184.2
225400     ADD     1 TO REC-CT.                                         NC2184.2
225500     IF      ID4-X = "1"                                          NC2184.2
225600             PERFORM PASS                                         NC2184.2
225700             GO TO UST-WRITE-GF-22-2                              NC2184.2
225800     ELSE                                                         NC2184.2
225900             GO TO UST-FAIL-GF-22-2.                              NC2184.2
226000 UST-DELETE-GF-22-2.                                              NC2184.2
226100     PERFORM DE-LETE.                                             NC2184.2
226200     GO TO UST-WRITE-GF-22-2.                                     NC2184.2
226300 UST-FAIL-GF-22-2.                                                NC2184.2
226400     PERFORM FAIL                                                 NC2184.2
226500     MOVE    ID4-X TO COMPUTED-A                                  NC2184.2
226600     MOVE    "1" TO CORRECT-A.                                    NC2184.2
226700 UST-WRITE-GF-22-2.                                               NC2184.2
226800     PERFORM PRINT-DETAIL.                                        NC2184.2
226900*                                                                 NC2184.2
227000 UST-TEST-GF-22-3.                                                NC2184.2
227100     ADD     1 TO REC-CT.                                         NC2184.2
227200     IF      ID5-XN-4 = "0   "                                    NC2184.2
227300             PERFORM PASS                                         NC2184.2
227400             GO TO UST-WRITE-GF-22-3                              NC2184.2
227500     ELSE                                                         NC2184.2
227600             GO TO UST-FAIL-GF-22-3.                              NC2184.2
227700 UST-DELETE-GF-22-3.                                              NC2184.2
227800     PERFORM DE-LETE.                                             NC2184.2
227900     GO TO UST-WRITE-GF-22-3.                                     NC2184.2
228000 UST-FAIL-GF-22-3.                                                NC2184.2
228100     PERFORM FAIL                                                 NC2184.2
228200     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
228300     MOVE   "0   " TO CORRECT-A.                                  NC2184.2
228400 UST-WRITE-GF-22-3.                                               NC2184.2
228500     PERFORM PRINT-DETAIL.                                        NC2184.2
228600*                                                                 NC2184.2
228700 UST-TEST-GF-22-4.                                                NC2184.2
228800     ADD     1 TO REC-CT.                                         NC2184.2
228900     IF      ID6-DU-2V0 = 2                                       NC2184.2
229000             PERFORM PASS                                         NC2184.2
229100             GO TO UST-WRITE-GF-22-4                              NC2184.2
229200     ELSE                                                         NC2184.2
229300             GO TO UST-FAIL-GF-22-4.                              NC2184.2
229400 UST-DELETE-GF-22-4.                                              NC2184.2
229500     PERFORM DE-LETE.                                             NC2184.2
229600     GO TO UST-WRITE-GF-22-4.                                     NC2184.2
229700 UST-FAIL-GF-22-4.                                                NC2184.2
229800     PERFORM FAIL                                                 NC2184.2
229900     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
230000     MOVE    2 TO CORRECT-N.                                      NC2184.2
230100 UST-WRITE-GF-22-4.                                               NC2184.2
230200     PERFORM PRINT-DETAIL.                                        NC2184.2
230300*                                                                 NC2184.2
230400 UST-TEST-GF-22-5.                                                NC2184.2
230500     ADD     1 TO REC-CT.                                         NC2184.2
230600     IF      ID10-DU-2V0 = 4                                      NC2184.2
230700             PERFORM PASS                                         NC2184.2
230800             GO TO UST-WRITE-GF-22-5                              NC2184.2
230900     ELSE                                                         NC2184.2
231000             GO TO UST-FAIL-GF-22-5.                              NC2184.2
231100 UST-DELETE-GF-22-5.                                              NC2184.2
231200     PERFORM DE-LETE.                                             NC2184.2
231300     GO TO UST-WRITE-GF-22-5.                                     NC2184.2
231400 UST-FAIL-GF-22-5.                                                NC2184.2
231500     PERFORM FAIL                                                 NC2184.2
231600     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
231700     MOVE    4 TO CORRECT-N.                                      NC2184.2
231800 UST-WRITE-GF-22-5.                                               NC2184.2
231900     PERFORM PRINT-DETAIL.                                        NC2184.2
232000*                                                                 NC2184.2
232100 UST-TEST-GF-22-6.                                                NC2184.2
232200     ADD     1 TO REC-CT.                                         NC2184.2
232300     IF      ID11-DU-2V0 = 1                                      NC2184.2
232400             PERFORM PASS                                         NC2184.2
232500             GO TO UST-WRITE-GF-22-6                              NC2184.2
232600     ELSE                                                         NC2184.2
232700             GO TO UST-FAIL-GF-22-6.                              NC2184.2
232800 UST-DELETE-GF-22-6.                                              NC2184.2
232900     PERFORM DE-LETE.                                             NC2184.2
233000     GO TO UST-WRITE-GF-22-6.                                     NC2184.2
233100 UST-FAIL-GF-22-6.                                                NC2184.2
233200     PERFORM FAIL                                                 NC2184.2
233300     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
233400     MOVE    1 TO CORRECT-N.                                      NC2184.2
233500 UST-WRITE-GF-22-6.                                               NC2184.2
233600     PERFORM PRINT-DETAIL.                                        NC2184.2
233700*                                                                 NC2184.2
233800 UST-INIT-GF-23.                                                  NC2184.2
233900*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2184.2
234000     MOVE   "VI-138 6.26.4 GR17" TO ANSI-REFERENCE.               NC2184.2
234100     MOVE   "UST-TEST-GF-23" TO PAR-NAME.                         NC2184.2
234200     MOVE   "OVERFLOW TEST" TO FEATURE.                           NC2184.2
234300     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
234400     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
234500     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
234600     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
234700     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
234800     MOVE    1 TO REC-CT.                                         NC2184.2
234900*                                                                 NC2184.2
235000 UST-TEST-GF-23-1.                                                NC2184.2
235100     UNSTRING GRP1-XN-6  INTO ID4A-XXXXX ID4B-X                   NC2184.2
235200         NOT ON OVERFLOW PERFORM PASS                             NC2184.2
235300                         GO TO UST-WRITE-GF-23-1.                 NC2184.2
235400     GO TO   UST-FAIL-GF-23-1.                                    NC2184.2
235500 UST-DELETE-GF-23-1.                                              NC2184.2
235600     PERFORM DE-LETE.                                             NC2184.2
235700     PERFORM PRINT-DETAIL.                                        NC2184.2
235800     GO      TO UST-INIT-GF-24.                                   NC2184.2
235900 UST-FAIL-GF-23-1.                                                NC2184.2
236000     PERFORM FAIL.                                                NC2184.2
236100     MOVE    "OVERFLOW SHOULD NOT HAVE OCCURED" TO RE-MARK.       NC2184.2
236200 UST-WRITE-GF-23-1.                                               NC2184.2
236300     PERFORM PRINT-DETAIL.                                        NC2184.2
236400*                                                                 NC2184.2
236500 UST-TEST-GF-23-2.                                                NC2184.2
236600     ADD     1 TO REC-CT.                                         NC2184.2
236700     IF      GRP4-XN-6 = "ABCDEF"                                 NC2184.2
236800             PERFORM PASS                                         NC2184.2
236900             GO TO UST-WRITE-GF-23-2                              NC2184.2
237000     ELSE                                                         NC2184.2
237100             GO TO UST-FAIL-GF-23-2.                              NC2184.2
237200 UST-DELETE-GF-23-2.                                              NC2184.2
237300     PERFORM DE-LETE.                                             NC2184.2
237400     GO TO UST-WRITE-GF-23-2.                                     NC2184.2
237500 UST-FAIL-GF-23-2.                                                NC2184.2
237600     PERFORM FAIL                                                 NC2184.2
237700     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
237800     MOVE   "ABCDEF" TO CORRECT-A.                                NC2184.2
237900 UST-WRITE-GF-23-2.                                               NC2184.2
238000     PERFORM PRINT-DETAIL.                                        NC2184.2
238100*                                                                 NC2184.2
238200 UST-INIT-GF-24.                                                  NC2184.2
238300*    ===-->  BOTH "OVERFLOW" PHRASES  <--===                      NC2184.2
238400     MOVE   "VI-138 6.26.4 GR17" TO ANSI-REFERENCE.               NC2184.2
238500     MOVE   "UST-TEST-GF-24" TO PAR-NAME.                         NC2184.2
238600     MOVE   "PIC X " TO FEATURE.                                  NC2184.2
238700     MOVE    ZERO TO ID4-X.                                       NC2184.2
238800     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
238900     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
239000     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
239100     MOVE ZERO TO ID11-DU-2V0.                                    NC2184.2
239200     MOVE    1 TO REC-CT.                                         NC2184.2
239300*                                                                 NC2184.2
239400 UST-TEST-GF-24-1.                                                NC2184.2
239500     UNSTRING ID1-XN-7 DELIMITED BY ZERO INTO ID4-X               NC2184.2
239600             DELIMITER IN ID5-XN-4                                NC2184.2
239700             COUNT IN ID6-DU-2V0                                  NC2184.2
239800             WITH POINTER ID10-DU-2V0                             NC2184.2
239900             TALLYING ID11-DU-2V0                                 NC2184.2
240000             ON OVERFLOW  PERFORM PASS                            NC2184.2
240100                          GO TO UST-WRITE-GF-24-1                 NC2184.2
240200         NOT ON OVERFLOW  GO TO UST-FAIL-GF-24-1.                 NC2184.2
240300 UST-DELETE-GF-24-1.                                              NC2184.2
240400     PERFORM DE-LETE.                                             NC2184.2
240500     PERFORM PRINT-DETAIL.                                        NC2184.2
240600     GO TO   UST-INIT-GF-25.                                      NC2184.2
240700 UST-FAIL-GF-24-1.                                                NC2184.2
240800     PERFORM FAIL.                                                NC2184.2
240900     MOVE   "ON OVERFLOW SHOULD HAVE EXECUTED" TO RE-MARK.        NC2184.2
241000 UST-WRITE-GF-24-1.                                               NC2184.2
241100     PERFORM PRINT-DETAIL.                                        NC2184.2
241200*                                                                 NC2184.2
241300 UST-TEST-GF-24-2.                                                NC2184.2
241400     ADD     1 TO REC-CT.                                         NC2184.2
241500     IF      ID4-X = "1"                                          NC2184.2
241600             PERFORM PASS                                         NC2184.2
241700             GO TO UST-WRITE-GF-24-2                              NC2184.2
241800     ELSE                                                         NC2184.2
241900             GO TO UST-FAIL-GF-24-2.                              NC2184.2
242000 UST-DELETE-GF-24-2.                                              NC2184.2
242100     PERFORM DE-LETE.                                             NC2184.2
242200     GO TO UST-WRITE-GF-24-2.                                     NC2184.2
242300 UST-FAIL-GF-24-2.                                                NC2184.2
242400     PERFORM FAIL                                                 NC2184.2
242500     MOVE    ID4-X TO COMPUTED-A                                  NC2184.2
242600     MOVE    "1" TO CORRECT-A.                                    NC2184.2
242700 UST-WRITE-GF-24-2.                                               NC2184.2
242800     PERFORM PRINT-DETAIL.                                        NC2184.2
242900*                                                                 NC2184.2
243000 UST-TEST-GF-24-3.                                                NC2184.2
243100     ADD     1 TO REC-CT.                                         NC2184.2
243200     IF      ID5-XN-4 = "0   "                                    NC2184.2
243300             PERFORM PASS                                         NC2184.2
243400             GO TO UST-WRITE-GF-24-3                              NC2184.2
243500     ELSE                                                         NC2184.2
243600             GO TO UST-FAIL-GF-24-3.                              NC2184.2
243700 UST-DELETE-GF-24-3.                                              NC2184.2
243800     PERFORM DE-LETE.                                             NC2184.2
243900     GO TO UST-WRITE-GF-24-3.                                     NC2184.2
244000 UST-FAIL-GF-24-3.                                                NC2184.2
244100     PERFORM FAIL                                                 NC2184.2
244200     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
244300     MOVE   "0   " TO CORRECT-A.                                  NC2184.2
244400 UST-WRITE-GF-24-3.                                               NC2184.2
244500     PERFORM PRINT-DETAIL.                                        NC2184.2
244600*                                                                 NC2184.2
244700 UST-TEST-GF-24-4.                                                NC2184.2
244800     ADD     1 TO REC-CT.                                         NC2184.2
244900     IF      ID6-DU-2V0 = 2                                       NC2184.2
245000             PERFORM PASS                                         NC2184.2
245100             GO TO UST-WRITE-GF-24-4                              NC2184.2
245200     ELSE                                                         NC2184.2
245300             GO TO UST-FAIL-GF-24-4.                              NC2184.2
245400 UST-DELETE-GF-24-4.                                              NC2184.2
245500     PERFORM DE-LETE.                                             NC2184.2
245600     GO TO UST-WRITE-GF-24-4.                                     NC2184.2
245700 UST-FAIL-GF-24-4.                                                NC2184.2
245800     PERFORM FAIL                                                 NC2184.2
245900     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
246000     MOVE    2 TO CORRECT-N.                                      NC2184.2
246100 UST-WRITE-GF-24-4.                                               NC2184.2
246200     PERFORM PRINT-DETAIL.                                        NC2184.2
246300*                                                                 NC2184.2
246400 UST-TEST-GF-24-5.                                                NC2184.2
246500     ADD     1 TO REC-CT.                                         NC2184.2
246600     IF      ID10-DU-2V0 = 4                                      NC2184.2
246700             PERFORM PASS                                         NC2184.2
246800             GO TO UST-WRITE-GF-24-5                              NC2184.2
246900     ELSE                                                         NC2184.2
247000             GO TO UST-FAIL-GF-24-5.                              NC2184.2
247100 UST-DELETE-GF-24-5.                                              NC2184.2
247200     PERFORM DE-LETE.                                             NC2184.2
247300     GO TO UST-WRITE-GF-24-5.                                     NC2184.2
247400 UST-FAIL-GF-24-5.                                                NC2184.2
247500     PERFORM FAIL                                                 NC2184.2
247600     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
247700     MOVE    4 TO CORRECT-N.                                      NC2184.2
247800 UST-WRITE-GF-24-5.                                               NC2184.2
247900     PERFORM PRINT-DETAIL.                                        NC2184.2
248000*                                                                 NC2184.2
248100 UST-TEST-GF-24-6.                                                NC2184.2
248200     ADD     1 TO REC-CT.                                         NC2184.2
248300     IF      ID11-DU-2V0 = 1                                      NC2184.2
248400             PERFORM PASS                                         NC2184.2
248500             GO TO UST-WRITE-GF-24-6                              NC2184.2
248600     ELSE                                                         NC2184.2
248700             GO TO UST-FAIL-GF-24-6.                              NC2184.2
248800 UST-DELETE-GF-24-6.                                              NC2184.2
248900     PERFORM DE-LETE.                                             NC2184.2
249000     GO TO UST-WRITE-GF-24-6.                                     NC2184.2
249100 UST-FAIL-GF-24-6.                                                NC2184.2
249200     PERFORM FAIL                                                 NC2184.2
249300     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
249400     MOVE    1 TO CORRECT-N.                                      NC2184.2
249500 UST-WRITE-GF-24-6.                                               NC2184.2
249600     PERFORM PRINT-DETAIL.                                        NC2184.2
249700*                                                                 NC2184.2
249800 UST-INIT-GF-25.                                                  NC2184.2
249900*    ===-->  BOTH "OVERFLOW" PHRASES  <--===                      NC2184.2
250000     MOVE   "VI-138 6.26.4 GR17" TO ANSI-REFERENCE.               NC2184.2
250100     MOVE   "UST-TEST-GF-25" TO PAR-NAME.                         NC2184.2
250200     MOVE   "OVERFLOW TEST" TO FEATURE.                           NC2184.2
250300     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
250400     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
250500     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
250600     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
250700     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
250800     MOVE    1 TO REC-CT.                                         NC2184.2
250900*                                                                 NC2184.2
251000 UST-TEST-GF-25-1.                                                NC2184.2
251100     UNSTRING GRP1-XN-6  INTO ID4A-XXXXX ID4B-X                   NC2184.2
251200             ON OVERFLOW GO TO UST-FAIL-GF-25-1                   NC2184.2
251300         NOT ON OVERFLOW PERFORM PASS                             NC2184.2
251400                         GO TO UST-WRITE-GF-25-1.                 NC2184.2
251500 UST-DELETE-GF-25-1.                                              NC2184.2
251600     PERFORM DE-LETE.                                             NC2184.2
251700     PERFORM PRINT-DETAIL.                                        NC2184.2
251800     GO      TO UST-INIT-GF-26.                                   NC2184.2
251900 UST-FAIL-GF-25-1.                                                NC2184.2
252000     PERFORM FAIL.                                                NC2184.2
252100     MOVE    "ON OVERFLOW SHOULD NOT HAVE EXECUTED" TO RE-MARK.   NC2184.2
252200 UST-WRITE-GF-25-1.                                               NC2184.2
252300     PERFORM PRINT-DETAIL.                                        NC2184.2
252400*                                                                 NC2184.2
252500 UST-TEST-GF-25-2.                                                NC2184.2
252600     ADD     1 TO REC-CT.                                         NC2184.2
252700     IF      GRP4-XN-6 = "ABCDEF"                                 NC2184.2
252800             PERFORM PASS                                         NC2184.2
252900             GO TO UST-WRITE-GF-25-2                              NC2184.2
253000     ELSE                                                         NC2184.2
253100             GO TO UST-FAIL-GF-25-2.                              NC2184.2
253200 UST-DELETE-GF-25-2.                                              NC2184.2
253300     PERFORM DE-LETE.                                             NC2184.2
253400     GO TO UST-WRITE-GF-25-2.                                     NC2184.2
253500 UST-FAIL-GF-25-2.                                                NC2184.2
253600     PERFORM FAIL                                                 NC2184.2
253700     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
253800     MOVE   "ABCDEF" TO CORRECT-A.                                NC2184.2
253900 UST-WRITE-GF-25-2.                                               NC2184.2
254000     PERFORM PRINT-DETAIL.                                        NC2184.2
254100*                                                                 NC2184.2
254200 UST-INIT-GF-26.                                                  NC2184.2
254300*    ===-->  EXPLICIT SCOPE TERMINATOR  <--===                    NC2184.2
254400     MOVE   "VI-138 6.26.4 GR19" TO ANSI-REFERENCE.               NC2184.2
254500     MOVE   "ABCDEF" TO GRP1-XN-6.                                NC2184.2
254600     MOVE    SPACE TO WRK-XN-00001-1.                             NC2184.2
254700     MOVE    SPACE TO WRK-XN-00001-2.                             NC2184.2
254800     MOVE    SPACE TO WRK-XN-00001-3.                             NC2184.2
254900     MOVE    ZERO TO REC-CT.                                      NC2184.2
255000*                                                                 NC2184.2
255100 UST-TEST-GF-26-0.                                                NC2184.2
255200     UNSTRING GRP1-XN-6  INTO ID4A-XXXXX ID4B-X                   NC2184.2
255300             ON OVERFLOW                                          NC2184.2
255400                     MOVE "A" TO WRK-XN-00001-1                   NC2184.2
255500                     MOVE "B" TO WRK-XN-00001-2                   NC2184.2
255600         NOT ON OVERFLOW                                          NC2184.2
255700                     MOVE "C" TO WRK-XN-00001-1                   NC2184.2
255800                     MOVE "D" TO WRK-XN-00001-2                   NC2184.2
255900     END-UNSTRING                                                 NC2184.2
256000     MOVE   "Z"  TO WRK-XN-00001-3.                               NC2184.2
256100     GO TO   UST-TEST-GF-26-1.                                    NC2184.2
256200 UST-DELETE-GF-26.                                                NC2184.2
256300     PERFORM DE-LETE.                                             NC2184.2
256400     PERFORM PRINT-DETAIL.                                        NC2184.2
256500     GO      TO UST-INIT-GF-27.                                   NC2184.2
256600*                                                                 NC2184.2
256700 UST-TEST-GF-26-1.                                                NC2184.2
256800     ADD     1 TO REC-CT.                                         NC2184.2
256900     IF      GRP4-XN-6 = "ABCDEF"                                 NC2184.2
257000             PERFORM PASS                                         NC2184.2
257100             GO TO UST-WRITE-GF-26-1                              NC2184.2
257200     ELSE                                                         NC2184.2
257300             GO TO UST-FAIL-GF-26-1.                              NC2184.2
257400 UST-DELETE-GF-26-1.                                              NC2184.2
257500     PERFORM DE-LETE.                                             NC2184.2
257600     GO TO UST-WRITE-GF-26-1.                                     NC2184.2
257700 UST-FAIL-GF-26-1.                                                NC2184.2
257800     PERFORM FAIL                                                 NC2184.2
257900     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
258000     MOVE   "ABCDEF" TO CORRECT-A.                                NC2184.2
258100 UST-WRITE-GF-26-1.                                               NC2184.2
258200     PERFORM PRINT-DETAIL.                                        NC2184.2
258300*                                                                 NC2184.2
258400 UST-TEST-GF-26-2.                                                NC2184.2
258500     ADD     1 TO REC-CT.                                         NC2184.2
258600     IF      WRK-XN-00001-1 = "C"                                 NC2184.2
258700             PERFORM PASS                                         NC2184.2
258800             GO TO UST-WRITE-GF-26-2                              NC2184.2
258900     ELSE                                                         NC2184.2
259000             GO TO UST-FAIL-GF-26-2.                              NC2184.2
259100 UST-DELETE-GF-26-2.                                              NC2184.2
259200     PERFORM DE-LETE.                                             NC2184.2
259300     GO TO UST-WRITE-GF-26-2.                                     NC2184.2
259400 UST-FAIL-GF-26-2.                                                NC2184.2
259500     MOVE    WRK-XN-00001-1 TO COMPUTED-A                         NC2184.2
259600     MOVE   "C" TO CORRECT-A.                                     NC2184.2
259700     MOVE   "NOT ON OVERFLOW SHOULD HAVE EXECUTED" TO RE-MARK     NC2184.2
259800     PERFORM FAIL.                                                NC2184.2
259900 UST-WRITE-GF-26-2.                                               NC2184.2
260000     PERFORM PRINT-DETAIL.                                        NC2184.2
260100*                                                                 NC2184.2
260200 UST-TEST-GF-26-3.                                                NC2184.2
260300     ADD     1 TO REC-CT.                                         NC2184.2
260400     IF      WRK-XN-00001-2 = "D"                                 NC2184.2
260500             PERFORM PASS                                         NC2184.2
260600             GO TO UST-WRITE-GF-26-3                              NC2184.2
260700     ELSE                                                         NC2184.2
260800             GO TO UST-FAIL-GF-26-3.                              NC2184.2
260900 UST-DELETE-GF-26-3.                                              NC2184.2
261000     PERFORM DE-LETE.                                             NC2184.2
261100     GO TO UST-WRITE-GF-26-3.                                     NC2184.2
261200 UST-FAIL-GF-26-3.                                                NC2184.2
261300     MOVE    WRK-XN-00001-2 TO COMPUTED-A                         NC2184.2
261400     MOVE   "D" TO CORRECT-A.                                     NC2184.2
261500     MOVE   "NOT ON OVERFLOW SHOULD HAVE EXECUTED" TO RE-MARK     NC2184.2
261600     PERFORM FAIL.                                                NC2184.2
261700 UST-WRITE-GF-26-3.                                               NC2184.2
261800     PERFORM PRINT-DETAIL.                                        NC2184.2
261900*                                                                 NC2184.2
262000 UST-TEST-GF-26-4.                                                NC2184.2
262100     ADD     1 TO REC-CT.                                         NC2184.2
262200     IF      WRK-XN-00001-3 = "Z"                                 NC2184.2
262300             PERFORM PASS                                         NC2184.2
262400             GO TO UST-WRITE-GF-26-4                              NC2184.2
262500     ELSE                                                         NC2184.2
262600             GO TO UST-FAIL-GF-26-4.                              NC2184.2
262700 UST-DELETE-GF-26-4.                                              NC2184.2
262800     PERFORM DE-LETE.                                             NC2184.2
262900     GO TO UST-WRITE-GF-26-4.                                     NC2184.2
263000 UST-FAIL-GF-26-4.                                                NC2184.2
263100     MOVE    WRK-XN-00001-3 TO COMPUTED-A                         NC2184.2
263200     MOVE   "Z" TO CORRECT-A.                                     NC2184.2
263300     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2184.2
263400     PERFORM FAIL.                                                NC2184.2
263500 UST-WRITE-GF-26-4.                                               NC2184.2
263600     PERFORM PRINT-DETAIL.                                        NC2184.2
263700*                                                                 NC2184.2
263800 UST-INIT-GF-27.                                                  NC2184.2
263900*    ===-->  EXPLICIT SCOPE TERMINATOR  <--===                    NC2184.2
264000     MOVE   "VI-138 6.26.4 GR17" TO ANSI-REFERENCE.               NC2184.2
264100     MOVE   "UST-TEST-GF-27" TO PAR-NAME.                         NC2184.2
264200     MOVE   "PIC X " TO FEATURE.                                  NC2184.2
264300     MOVE   "1200000" TO ID1-XN-7.                                NC2184.2
264400     MOVE    ZERO TO ID4-X.                                       NC2184.2
264500     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
264600     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
264700     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
264800     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
264900     MOVE    ZERO TO REC-CT.                                      NC2184.2
265000     MOVE    SPACE TO WRK-XN-00001-1.                             NC2184.2
265100     MOVE    SPACE TO WRK-XN-00001-2.                             NC2184.2
265200     MOVE    SPACE TO WRK-XN-00001-3.                             NC2184.2
265300*                                                                 NC2184.2
265400 UST-TEST-GF-27-0.                                                NC2184.2
265500     UNSTRING ID1-XN-7 DELIMITED BY ZERO INTO ID4-X               NC2184.2
265600             DELIMITER IN ID5-XN-4                                NC2184.2
265700             COUNT IN ID6-DU-2V0                                  NC2184.2
265800             WITH POINTER ID10-DU-2V0                             NC2184.2
265900             TALLYING ID11-DU-2V0                                 NC2184.2
266000             ON OVERFLOW                                          NC2184.2
266100                     MOVE "A" TO WRK-XN-00001-1                   NC2184.2
266200                     MOVE "B" TO WRK-XN-00001-2                   NC2184.2
266300     END-UNSTRING                                                 NC2184.2
266400     MOVE   "Z"  TO WRK-XN-00001-3.                               NC2184.2
266500     GO TO   UST-TEST-GF-27-1.                                    NC2184.2
266600 UST-DELETE-GF-27.                                                NC2184.2
266700     PERFORM DE-LETE.                                             NC2184.2
266800     PERFORM PRINT-DETAIL.                                        NC2184.2
266900     GO TO   UST-INIT-GF-28.                                      NC2184.2
267000*                                                                 NC2184.2
267100 UST-TEST-GF-27-1.                                                NC2184.2
267200     ADD     1 TO REC-CT.                                         NC2184.2
267300     IF      ID4-X = "1"                                          NC2184.2
267400             PERFORM PASS                                         NC2184.2
267500             GO TO UST-WRITE-GF-27-1                              NC2184.2
267600     ELSE                                                         NC2184.2
267700             GO TO UST-FAIL-GF-27-1.                              NC2184.2
267800 UST-DELETE-GF-27-1.                                              NC2184.2
267900     PERFORM DE-LETE.                                             NC2184.2
268000     GO TO UST-WRITE-GF-27-1.                                     NC2184.2
268100 UST-FAIL-GF-27-1.                                                NC2184.2
268200     PERFORM FAIL                                                 NC2184.2
268300     MOVE    ID4-X TO COMPUTED-A                                  NC2184.2
268400     MOVE    "1" TO CORRECT-A.                                    NC2184.2
268500 UST-WRITE-GF-27-1.                                               NC2184.2
268600     PERFORM PRINT-DETAIL.                                        NC2184.2
268700*                                                                 NC2184.2
268800 UST-TEST-GF-27-2.                                                NC2184.2
268900     ADD     1 TO REC-CT.                                         NC2184.2
269000     IF      ID5-XN-4 = "0   "                                    NC2184.2
269100             PERFORM PASS                                         NC2184.2
269200             GO TO UST-WRITE-GF-27-2                              NC2184.2
269300     ELSE                                                         NC2184.2
269400             GO TO UST-FAIL-GF-27-2.                              NC2184.2
269500 UST-DELETE-GF-27-2.                                              NC2184.2
269600     PERFORM DE-LETE.                                             NC2184.2
269700     GO TO UST-WRITE-GF-27-2.                                     NC2184.2
269800 UST-FAIL-GF-27-2.                                                NC2184.2
269900     PERFORM FAIL                                                 NC2184.2
270000     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
270100     MOVE   "0   " TO CORRECT-A.                                  NC2184.2
270200 UST-WRITE-GF-27-2.                                               NC2184.2
270300     PERFORM PRINT-DETAIL.                                        NC2184.2
270400*                                                                 NC2184.2
270500 UST-TEST-GF-27-3.                                                NC2184.2
270600     ADD     1 TO REC-CT.                                         NC2184.2
270700     IF      ID6-DU-2V0 = 2                                       NC2184.2
270800             PERFORM PASS                                         NC2184.2
270900             GO TO UST-WRITE-GF-27-3                              NC2184.2
271000     ELSE                                                         NC2184.2
271100             GO TO UST-FAIL-GF-27-3.                              NC2184.2
271200 UST-DELETE-GF-27-3.                                              NC2184.2
271300     PERFORM DE-LETE.                                             NC2184.2
271400     GO TO UST-WRITE-GF-27-3.                                     NC2184.2
271500 UST-FAIL-GF-27-3.                                                NC2184.2
271600     PERFORM FAIL                                                 NC2184.2
271700     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
271800     MOVE    2 TO CORRECT-N.                                      NC2184.2
271900 UST-WRITE-GF-27-3.                                               NC2184.2
272000     PERFORM PRINT-DETAIL.                                        NC2184.2
272100*                                                                 NC2184.2
272200 UST-TEST-GF-27-4.                                                NC2184.2
272300     ADD     1 TO REC-CT.                                         NC2184.2
272400     IF      ID10-DU-2V0 = 4                                      NC2184.2
272500             PERFORM PASS                                         NC2184.2
272600             GO TO UST-WRITE-GF-27-4                              NC2184.2
272700     ELSE                                                         NC2184.2
272800             GO TO UST-FAIL-GF-27-4.                              NC2184.2
272900 UST-DELETE-GF-27-4.                                              NC2184.2
273000     PERFORM DE-LETE.                                             NC2184.2
273100     GO TO UST-WRITE-GF-27-4.                                     NC2184.2
273200 UST-FAIL-GF-27-4.                                                NC2184.2
273300     PERFORM FAIL                                                 NC2184.2
273400     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
273500     MOVE    4 TO CORRECT-N.                                      NC2184.2
273600 UST-WRITE-GF-27-4.                                               NC2184.2
273700     PERFORM PRINT-DETAIL.                                        NC2184.2
273800*                                                                 NC2184.2
273900 UST-TEST-GF-27-5.                                                NC2184.2
274000     ADD     1 TO REC-CT.                                         NC2184.2
274100     IF      ID11-DU-2V0 = 1                                      NC2184.2
274200             PERFORM PASS                                         NC2184.2
274300             GO TO UST-WRITE-GF-27-5                              NC2184.2
274400     ELSE                                                         NC2184.2
274500             GO TO UST-FAIL-GF-27-5.                              NC2184.2
274600 UST-DELETE-GF-27-5.                                              NC2184.2
274700     PERFORM DE-LETE.                                             NC2184.2
274800     GO TO UST-WRITE-GF-27-5.                                     NC2184.2
274900 UST-FAIL-GF-27-5.                                                NC2184.2
275000     PERFORM FAIL                                                 NC2184.2
275100     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
275200     MOVE    1 TO CORRECT-N.                                      NC2184.2
275300 UST-WRITE-GF-27-5.                                               NC2184.2
275400*                                                                 NC2184.2
275500 UST-INIT-GF-28.                                                  NC2184.2
275600*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2184.2
275700     MOVE   "VI-138 6.26.4 GR17" TO ANSI-REFERENCE.               NC2184.2
275800     MOVE   "UST-TEST-GF-28" TO PAR-NAME.                         NC2184.2
275900     MOVE   "OVERFLOW TEST" TO FEATURE.                           NC2184.2
276000     MOVE   "ABCDEF" TO GRP1-XN-6.                                NC2184.2
276100     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
276200     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
276300     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
276400     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
276500     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
276600     MOVE    ZERO TO REC-CT.                                      NC2184.2
276700     MOVE    SPACE TO WRK-XN-00001-1.                             NC2184.2
276800     MOVE    SPACE TO WRK-XN-00001-2.                             NC2184.2
276900     MOVE    SPACE TO WRK-XN-00001-3.                             NC2184.2
277000*                                                                 NC2184.2
277100 UST-TEST-GF-28-0.                                                NC2184.2
277200     UNSTRING GRP1-XN-6  INTO ID4A-XXXXX ID4B-X                   NC2184.2
277300             ON OVERFLOW                                          NC2184.2
277400                     MOVE "A" TO WRK-XN-00001-1                   NC2184.2
277500                     MOVE "B" TO WRK-XN-00001-2                   NC2184.2
277600     END-UNSTRING                                                 NC2184.2
277700     MOVE   "Z"  TO WRK-XN-00001-3.                               NC2184.2
277800     GO TO   UST-TEST-GF-28-1.                                    NC2184.2
277900 UST-DELETE-GF-28.                                                NC2184.2
278000     PERFORM DE-LETE.                                             NC2184.2
278100     PERFORM PRINT-DETAIL.                                        NC2184.2
278200     GO      TO UST-INIT-GF-29.                                   NC2184.2
278300*                                                                 NC2184.2
278400 UST-TEST-GF-28-1.                                                NC2184.2
278500     ADD     1 TO REC-CT.                                         NC2184.2
278600     IF      GRP4-XN-6 = "ABCDEF"                                 NC2184.2
278700             PERFORM PASS                                         NC2184.2
278800             GO TO UST-WRITE-GF-28-1                              NC2184.2
278900     ELSE                                                         NC2184.2
279000             GO TO UST-FAIL-GF-28-1.                              NC2184.2
279100 UST-DELETE-GF-28-1.                                              NC2184.2
279200     PERFORM DE-LETE.                                             NC2184.2
279300     GO TO UST-WRITE-GF-28-1.                                     NC2184.2
279400 UST-FAIL-GF-28-1.                                                NC2184.2
279500     PERFORM FAIL                                                 NC2184.2
279600     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
279700     MOVE   "ABCDEF" TO CORRECT-A.                                NC2184.2
279800 UST-WRITE-GF-28-1.                                               NC2184.2
279900     PERFORM PRINT-DETAIL.                                        NC2184.2
280000*                                                                 NC2184.2
280100*                                                                 NC2184.2
280200 UST-INIT-GF-29.                                                  NC2184.2
280300*    ===-->  EXPLICIT SCOPE TERMINATOR  <--===                    NC2184.2
280400     MOVE   "VI-138 6.26.4 GR17" TO ANSI-REFERENCE.               NC2184.2
280500     MOVE   "UST-TEST-GF-29" TO PAR-NAME.                         NC2184.2
280600     MOVE   "PIC X " TO FEATURE.                                  NC2184.2
280700     MOVE   "1200000" TO ID1-XN-7.                                NC2184.2
280800     MOVE    ZERO TO ID4-X.                                       NC2184.2
280900     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
281000     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
281100     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
281200     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
281300     MOVE    ZERO TO REC-CT.                                      NC2184.2
281400     MOVE    SPACE TO WRK-XN-00001-1.                             NC2184.2
281500     MOVE    SPACE TO WRK-XN-00001-2.                             NC2184.2
281600     MOVE    SPACE TO WRK-XN-00001-3.                             NC2184.2
281700*                                                                 NC2184.2
281800 UST-TEST-GF-29-0.                                                NC2184.2
281900     UNSTRING ID1-XN-7 DELIMITED BY ZERO INTO ID4-X               NC2184.2
282000             DELIMITER IN ID5-XN-4                                NC2184.2
282100             COUNT IN ID6-DU-2V0                                  NC2184.2
282200             WITH POINTER ID10-DU-2V0                             NC2184.2
282300             TALLYING ID11-DU-2V0                                 NC2184.2
282400         NOT ON OVERFLOW                                          NC2184.2
282500                     MOVE "A" TO WRK-XN-00001-1                   NC2184.2
282600                     MOVE "B" TO WRK-XN-00001-2                   NC2184.2
282700     END-UNSTRING                                                 NC2184.2
282800     MOVE   "Z"  TO WRK-XN-00001-3.                               NC2184.2
282900     GO TO   UST-TEST-GF-29-1.                                    NC2184.2
283000 UST-DELETE-GF-29.                                                NC2184.2
283100     PERFORM DE-LETE.                                             NC2184.2
283200     PERFORM PRINT-DETAIL.                                        NC2184.2
283300     GO TO   UST-INIT-GF-30.                                      NC2184.2
283400*                                                                 NC2184.2
283500 UST-TEST-GF-29-1.                                                NC2184.2
283600     MOVE   "UST-TEST-GF-29-1" TO PAR-NAME.                       NC2184.2
283700     ADD     1 TO REC-CT.                                         NC2184.2
283800     IF      ID4-X = "1"                                          NC2184.2
283900             PERFORM PASS                                         NC2184.2
284000             GO TO UST-WRITE-GF-29-1                              NC2184.2
284100     ELSE                                                         NC2184.2
284200             GO TO UST-FAIL-GF-29-1.                              NC2184.2
284300 UST-DELETE-GF-29-1.                                              NC2184.2
284400     PERFORM DE-LETE.                                             NC2184.2
284500     GO TO UST-WRITE-GF-29-1.                                     NC2184.2
284600 UST-FAIL-GF-29-1.                                                NC2184.2
284700     PERFORM FAIL                                                 NC2184.2
284800     MOVE    ID4-X TO COMPUTED-A                                  NC2184.2
284900     MOVE    "1" TO CORRECT-A.                                    NC2184.2
285000 UST-WRITE-GF-29-1.                                               NC2184.2
285100     PERFORM PRINT-DETAIL.                                        NC2184.2
285200*                                                                 NC2184.2
285300 UST-TEST-GF-29-2.                                                NC2184.2
285400     ADD     1 TO REC-CT.                                         NC2184.2
285500     IF      ID5-XN-4 = "0   "                                    NC2184.2
285600             PERFORM PASS                                         NC2184.2
285700             GO TO UST-WRITE-GF-29-2                              NC2184.2
285800     ELSE                                                         NC2184.2
285900             GO TO UST-FAIL-GF-29-2.                              NC2184.2
286000 UST-DELETE-GF-29-2.                                              NC2184.2
286100     PERFORM DE-LETE.                                             NC2184.2
286200     GO TO UST-WRITE-GF-29-2.                                     NC2184.2
286300 UST-FAIL-GF-29-2.                                                NC2184.2
286400     PERFORM FAIL                                                 NC2184.2
286500     MOVE    ID5-XN-4 TO COMPUTED-A                               NC2184.2
286600     MOVE   "0   " TO CORRECT-A.                                  NC2184.2
286700 UST-WRITE-GF-29-2.                                               NC2184.2
286800     PERFORM PRINT-DETAIL.                                        NC2184.2
286900*                                                                 NC2184.2
287000 UST-TEST-GF-29-3.                                                NC2184.2
287100     ADD     1 TO REC-CT.                                         NC2184.2
287200     IF      ID6-DU-2V0 = 2                                       NC2184.2
287300             PERFORM PASS                                         NC2184.2
287400             GO TO UST-WRITE-GF-29-3                              NC2184.2
287500     ELSE                                                         NC2184.2
287600             GO TO UST-FAIL-GF-29-3.                              NC2184.2
287700 UST-DELETE-GF-29-3.                                              NC2184.2
287800     PERFORM DE-LETE.                                             NC2184.2
287900     GO TO UST-WRITE-GF-29-3.                                     NC2184.2
288000 UST-FAIL-GF-29-3.                                                NC2184.2
288100     PERFORM FAIL                                                 NC2184.2
288200     MOVE    ID6-DU-2V0 TO COMPUTED-N                             NC2184.2
288300     MOVE    2 TO CORRECT-N.                                      NC2184.2
288400 UST-WRITE-GF-29-3.                                               NC2184.2
288500     PERFORM PRINT-DETAIL.                                        NC2184.2
288600*                                                                 NC2184.2
288700 UST-TEST-GF-29-4.                                                NC2184.2
288800     ADD     1 TO REC-CT.                                         NC2184.2
288900     IF      ID10-DU-2V0 = 4                                      NC2184.2
289000             PERFORM PASS                                         NC2184.2
289100             GO TO UST-WRITE-GF-29-4                              NC2184.2
289200     ELSE                                                         NC2184.2
289300             GO TO UST-FAIL-GF-29-4.                              NC2184.2
289400 UST-DELETE-GF-29-4.                                              NC2184.2
289500     PERFORM DE-LETE.                                             NC2184.2
289600     GO TO UST-WRITE-GF-29-4.                                     NC2184.2
289700 UST-FAIL-GF-29-4.                                                NC2184.2
289800     PERFORM FAIL                                                 NC2184.2
289900     MOVE    ID10-DU-2V0 TO COMPUTED-N                            NC2184.2
290000     MOVE    4 TO CORRECT-N.                                      NC2184.2
290100 UST-WRITE-GF-29-4.                                               NC2184.2
290200     PERFORM PRINT-DETAIL.                                        NC2184.2
290300*                                                                 NC2184.2
290400 UST-TEST-GF-29-5.                                                NC2184.2
290500     ADD     1 TO REC-CT.                                         NC2184.2
290600     IF      ID11-DU-2V0 = 1                                      NC2184.2
290700             PERFORM PASS                                         NC2184.2
290800             GO TO UST-WRITE-GF-29-5                              NC2184.2
290900     ELSE                                                         NC2184.2
291000             GO TO UST-FAIL-GF-29-5.                              NC2184.2
291100 UST-DELETE-GF-29-5.                                              NC2184.2
291200     PERFORM DE-LETE.                                             NC2184.2
291300     GO TO UST-WRITE-GF-29-5.                                     NC2184.2
291400 UST-FAIL-GF-29-5.                                                NC2184.2
291500     PERFORM FAIL                                                 NC2184.2
291600     MOVE    ID11-DU-2V0 TO COMPUTED-N                            NC2184.2
291700     MOVE    1 TO CORRECT-N.                                      NC2184.2
291800 UST-WRITE-GF-29-5.                                               NC2184.2
291900     PERFORM PRINT-DETAIL.                                        NC2184.2
292000*                                                                 NC2184.2
292100 UST-TEST-GF-29-6.                                                NC2184.2
292200     ADD     1 TO REC-CT.                                         NC2184.2
292300     IF      WRK-XN-00001-1 = SPACE                               NC2184.2
292400             PERFORM PASS                                         NC2184.2
292500             GO TO UST-WRITE-GF-29-6                              NC2184.2
292600     ELSE                                                         NC2184.2
292700             GO TO UST-FAIL-GF-29-6.                              NC2184.2
292800 UST-DELETE-GF-29-6.                                              NC2184.2
292900     PERFORM DE-LETE.                                             NC2184.2
293000     GO TO UST-WRITE-GF-29-6.                                     NC2184.2
293100 UST-FAIL-GF-29-6.                                                NC2184.2
293200     MOVE    WRK-XN-00001-1 TO COMPUTED-A                         NC2184.2
293300     MOVE    SPACE TO CORRECT-A.                                  NC2184.2
293400     MOVE "NOT ON OVERFLOW SHOULD NOT HAVE EXECUTED" TO RE-MARK   NC2184.2
293500     PERFORM FAIL.                                                NC2184.2
293600 UST-WRITE-GF-29-6.                                               NC2184.2
293700     PERFORM PRINT-DETAIL.                                        NC2184.2
293800*                                                                 NC2184.2
293900 UST-TEST-GF-29-7.                                                NC2184.2
294000     ADD     1 TO REC-CT.                                         NC2184.2
294100     IF      WRK-XN-00001-2 = SPACE                               NC2184.2
294200             PERFORM PASS                                         NC2184.2
294300             GO TO UST-WRITE-GF-29-7                              NC2184.2
294400     ELSE                                                         NC2184.2
294500             GO TO UST-FAIL-GF-29-7.                              NC2184.2
294600 UST-DELETE-GF-29-7.                                              NC2184.2
294700     PERFORM DE-LETE.                                             NC2184.2
294800     GO TO UST-WRITE-GF-29-7.                                     NC2184.2
294900 UST-FAIL-GF-29-7.                                                NC2184.2
295000     MOVE    WRK-XN-00001-2 TO COMPUTED-A                         NC2184.2
295100     MOVE    SPACE TO CORRECT-A.                                  NC2184.2
295200     MOVE "NOT ON OVERFLOW SHOULD NOT HAVE EXECUTED" TO RE-MARK.  NC2184.2
295300     PERFORM FAIL.                                                NC2184.2
295400 UST-WRITE-GF-29-7.                                               NC2184.2
295500     PERFORM PRINT-DETAIL.                                        NC2184.2
295600*                                                                 NC2184.2
295700 UST-TEST-GF-29-8.                                                NC2184.2
295800     ADD     1 TO REC-CT.                                         NC2184.2
295900     IF      WRK-XN-00001-3 = "Z"                                 NC2184.2
296000             PERFORM PASS                                         NC2184.2
296100             GO TO UST-WRITE-GF-29-8                              NC2184.2
296200     ELSE                                                         NC2184.2
296300             GO TO UST-FAIL-GF-29-8.                              NC2184.2
296400 UST-DELETE-GF-29-8.                                              NC2184.2
296500     PERFORM DE-LETE.                                             NC2184.2
296600     GO TO UST-WRITE-GF-29-8.                                     NC2184.2
296700 UST-FAIL-GF-29-8.                                                NC2184.2
296800     MOVE    WRK-XN-00001-3 TO COMPUTED-A                         NC2184.2
296900     MOVE   "Z" TO CORRECT-A.                                     NC2184.2
297000     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2184.2
297100     PERFORM FAIL.                                                NC2184.2
297200 UST-WRITE-GF-29-8.                                               NC2184.2
297300     PERFORM PRINT-DETAIL.                                        NC2184.2
297400*                                                                 NC2184.2
297500 UST-INIT-GF-30.                                                  NC2184.2
297600*    ===--> "NOT ON OVERFLOW" PHRASE  <--===                      NC2184.2
297700     MOVE   "VI-138 6.26.4 GR17" TO ANSI-REFERENCE.               NC2184.2
297800     MOVE   "UST-TEST-GF-30" TO PAR-NAME.                         NC2184.2
297900     MOVE   "OVERFLOW TEST" TO FEATURE.                           NC2184.2
298000     MOVE   "ABCDEF" TO GRP1-XN-6.                                NC2184.2
298100     MOVE    SPACES TO GRP4-XN-6.                                 NC2184.2
298200     MOVE    "****" TO ID5-XN-4.                                  NC2184.2
298300     MOVE    ZERO TO ID6-DU-2V0.                                  NC2184.2
298400     MOVE    1 TO ID10-DU-2V0.                                    NC2184.2
298500     MOVE    ZERO TO ID11-DU-2V0.                                 NC2184.2
298600     MOVE    ZERO TO REC-CT.                                      NC2184.2
298700     MOVE    SPACE TO WRK-XN-00001-1.                             NC2184.2
298800     MOVE    SPACE TO WRK-XN-00001-2.                             NC2184.2
298900     MOVE    SPACE TO WRK-XN-00001-3.                             NC2184.2
299000*                                                                 NC2184.2
299100 UST-TEST-GF-30-0.                                                NC2184.2
299200     UNSTRING GRP1-XN-6  INTO ID4A-XXXXX ID4B-X                   NC2184.2
299300         NOT ON OVERFLOW                                          NC2184.2
299400                     MOVE "A" TO WRK-XN-00001-1                   NC2184.2
299500                     MOVE "B" TO WRK-XN-00001-2                   NC2184.2
299600     END-UNSTRING                                                 NC2184.2
299700     MOVE   "Z"  TO WRK-XN-00001-3.                               NC2184.2
299800     GO TO   UST-TEST-GF-30-1.                                    NC2184.2
299900 UST-DELETE-GF-30.                                                NC2184.2
300000     PERFORM DE-LETE.                                             NC2184.2
300100     PERFORM PRINT-DETAIL.                                        NC2184.2
300200     GO      TO CCVS-EXIT.                                        NC2184.2
300300*                                                                 NC2184.2
300400 UST-TEST-GF-30-1.                                                NC2184.2
300500     MOVE    1 TO REC-CT.                                         NC2184.2
300600     IF      GRP4-XN-6 = "ABCDEF"                                 NC2184.2
300700             PERFORM PASS                                         NC2184.2
300800             GO TO UST-WRITE-GF-30-1                              NC2184.2
300900     ELSE                                                         NC2184.2
301000             GO TO UST-FAIL-GF-30-1.                              NC2184.2
301100 UST-DELETE-GF-30-1.                                              NC2184.2
301200     PERFORM DE-LETE.                                             NC2184.2
301300     GO TO UST-WRITE-GF-30-1.                                     NC2184.2
301400 UST-FAIL-GF-30-1.                                                NC2184.2
301500     PERFORM FAIL                                                 NC2184.2
301600     MOVE    GRP4-XN-6 TO COMPUTED-A                              NC2184.2
301700     MOVE   "ABCDEF" TO CORRECT-A.                                NC2184.2
301800 UST-WRITE-GF-30-1.                                               NC2184.2
301900     PERFORM PRINT-DETAIL.                                        NC2184.2
302000*                                                                 NC2184.2
302100 UST-TEST-GF-30-2.                                                NC2184.2
302200     ADD     1 TO REC-CT.                                         NC2184.2
302300     IF      WRK-XN-00001-1 = "A"                                 NC2184.2
302400             PERFORM PASS                                         NC2184.2
302500             GO TO UST-WRITE-GF-30-2                              NC2184.2
302600     ELSE                                                         NC2184.2
302700             GO TO UST-FAIL-GF-30-2.                              NC2184.2
302800 UST-DELETE-GF-30-2.                                              NC2184.2
302900     PERFORM DE-LETE.                                             NC2184.2
303000     GO TO UST-WRITE-GF-30-2.                                     NC2184.2
303100 UST-FAIL-GF-30-2.                                                NC2184.2
303200     MOVE    WRK-XN-00001-1 TO COMPUTED-A                         NC2184.2
303300     MOVE   "A"    TO CORRECT-A.                                  NC2184.2
303400     MOVE   "NOT ON OVERFLOW SHOULD HAVE EXECUTED"  TO RE-MARK.   NC2184.2
303500     PERFORM FAIL.                                                NC2184.2
303600 UST-WRITE-GF-30-2.                                               NC2184.2
303700     PERFORM PRINT-DETAIL.                                        NC2184.2
303800*                                                                 NC2184.2
303900 UST-TEST-GF-30-3.                                                NC2184.2
304000     ADD     1 TO REC-CT.                                         NC2184.2
304100     IF      WRK-XN-00001-2 = "B"                                 NC2184.2
304200             PERFORM PASS                                         NC2184.2
304300             GO TO UST-WRITE-GF-30-3                              NC2184.2
304400     ELSE                                                         NC2184.2
304500             GO TO UST-FAIL-GF-30-3.                              NC2184.2
304600 UST-DELETE-GF-30-3.                                              NC2184.2
304700     PERFORM DE-LETE.                                             NC2184.2
304800     GO TO UST-WRITE-GF-30-3.                                     NC2184.2
304900 UST-FAIL-GF-30-3.                                                NC2184.2
305000     MOVE    WRK-XN-00001-2 TO COMPUTED-A                         NC2184.2
305100     MOVE   "B"    TO CORRECT-A.                                  NC2184.2
305200     MOVE   "NOT ON OVERFLOW SHOULD HAVE EXECUTED" TO RE-MARK.    NC2184.2
305300     PERFORM FAIL.                                                NC2184.2
305400 UST-WRITE-GF-30-3.                                               NC2184.2
305500     PERFORM PRINT-DETAIL.                                        NC2184.2
305600*                                                                 NC2184.2
305700 UST-TEST-GF-30-4.                                                NC2184.2
305800     ADD     1 TO REC-CT.                                         NC2184.2
305900     IF      WRK-XN-00001-3 = "Z"                                 NC2184.2
306000             PERFORM PASS                                         NC2184.2
306100             GO TO UST-WRITE-GF-30-4                              NC2184.2
306200     ELSE                                                         NC2184.2
306300             GO TO UST-FAIL-GF-30-4.                              NC2184.2
306400 UST-DELETE-GF-30-4.                                              NC2184.2
306500     PERFORM DE-LETE.                                             NC2184.2
306600     GO TO UST-WRITE-GF-30-4.                                     NC2184.2
306700 UST-FAIL-GF-30-4.                                                NC2184.2
306800     MOVE    WRK-XN-00001-3 TO COMPUTED-A                         NC2184.2
306900     MOVE   "Z" TO CORRECT-A.                                     NC2184.2
307000     MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK                 NC2184.2
307100     PERFORM FAIL.                                                NC2184.2
307200 UST-WRITE-GF-30-4.                                               NC2184.2
307300     PERFORM PRINT-DETAIL.                                        NC2184.2
307400*                                                                 NC2184.2
307500 CCVS-EXIT SECTION.                                               NC2184.2
307600 CCVS-999999.                                                     NC2184.2
307700     GO TO CLOSE-FILES.                                           NC2184.2
      *END-OF,NC218A                                                                  
