      *HEADER,COBOL,IF125A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1254.2
000200 PROGRAM-ID.                                                      IF1254.2
000300     IF125A.                                                      IF1254.2
000400                                                                  IF1254.2
000500***********************************************************       IF1254.2
000600*                                                         *       IF1254.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1254.2
000800* It contains tests for the Intrinsic Function NUMVAL.    *       IF1254.2
000900*                                                         *       IF1254.2
001000*                                                         *       IF1254.2
001100***********************************************************       IF1254.2
001200 ENVIRONMENT DIVISION.                                            IF1254.2
001300 CONFIGURATION SECTION.                                           IF1254.2
001400 SOURCE-COMPUTER.                                                 IF1254.2
001500     XXXXX082.                                                    IF1254.2
001600 OBJECT-COMPUTER.                                                 IF1254.2
001700     XXXXX083.                                                    IF1254.2
001800 INPUT-OUTPUT SECTION.                                            IF1254.2
001900 FILE-CONTROL.                                                    IF1254.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1254.2
002100     XXXXX055.                                                    IF1254.2
002200 DATA DIVISION.                                                   IF1254.2
002300 FILE SECTION.                                                    IF1254.2
002400 FD  PRINT-FILE.                                                  IF1254.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1254.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1254.2
002700 WORKING-STORAGE SECTION.                                         IF1254.2
002800***********************************************************       IF1254.2
002900* Variables specific to the Intrinsic Function Test IF125A*       IF1254.2
003000***********************************************************       IF1254.2
003100 01  A                   PIC X(1)       VALUE "4".                IF1254.2
003200 01  B                   PIC X(5)       VALUE "203".              IF1254.2
003300 01  C                   PIC X(4)       VALUE ".429".             IF1254.2
003400 01  D                   PIC X(7)       VALUE "928.344".          IF1254.2
003500 01  E                   PIC X(9)       VALUE "-042.3240".        IF1254.2
003600 01  F                   PIC X(7)       VALUE " 23.000".          IF1254.2
003700 01  G                   PIC X(8)       VALUE "-92924.3".         IF1254.2
003800 01  H                   PIC X(6)       VALUE "93.21+".           IF1254.2
003900 01  I                   PIC X(9)       VALUE " 92.92  -".        IF1254.2
004000 01  TEMP                PIC S9(5)V9(5).                          IF1254.2
004100                                                                  IF1254.2
004200*                                                                 IF1254.2
004300**********************************************************        IF1254.2
004400*                                                                 IF1254.2
004500 01  TEST-RESULTS.                                                IF1254.2
004600     02 FILLER                   PIC X      VALUE SPACE.          IF1254.2
004700     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1254.2
004800     02 FILLER                   PIC X      VALUE SPACE.          IF1254.2
004900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1254.2
005000     02 FILLER                   PIC X      VALUE SPACE.          IF1254.2
005100     02  PAR-NAME.                                                IF1254.2
005200       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1254.2
005300       03  PARDOT-X              PIC X      VALUE SPACE.          IF1254.2
005400       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1254.2
005500     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1254.2
005600     02 RE-MARK                  PIC X(61).                       IF1254.2
005700 01  TEST-COMPUTED.                                               IF1254.2
005800     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1254.2
005900     02 FILLER                   PIC X(17)  VALUE                 IF1254.2
006000            "       COMPUTED=".                                   IF1254.2
006100     02 COMPUTED-X.                                               IF1254.2
006200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1254.2
006300     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1254.2
006400                                 PIC -9(9).9(9).                  IF1254.2
006500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1254.2
006600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1254.2
006700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1254.2
006800     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1254.2
006900         04 COMPUTED-18V0                    PIC -9(18).          IF1254.2
007000         04 FILLER                           PIC X.               IF1254.2
007100     03 FILLER PIC X(50) VALUE SPACE.                             IF1254.2
007200 01  TEST-CORRECT.                                                IF1254.2
007300     02 FILLER PIC X(30) VALUE SPACE.                             IF1254.2
007400     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1254.2
007500     02 CORRECT-X.                                                IF1254.2
007600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1254.2
007700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1254.2
007800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1254.2
007900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1254.2
008000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1254.2
008100     03      CR-18V0 REDEFINES CORRECT-A.                         IF1254.2
008200         04 CORRECT-18V0                     PIC -9(18).          IF1254.2
008300         04 FILLER                           PIC X.               IF1254.2
008400     03 FILLER PIC X(2) VALUE SPACE.                              IF1254.2
008500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1254.2
008600 01  CCVS-C-1.                                                    IF1254.2
008700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1254.2
008800-    "SS  PARAGRAPH-NAME                                          IF1254.2
008900-    "       REMARKS".                                            IF1254.2
009000     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1254.2
009100 01  CCVS-C-2.                                                    IF1254.2
009200     02 FILLER                     PIC X        VALUE SPACE.      IF1254.2
009300     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1254.2
009400     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1254.2
009500     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1254.2
009600     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1254.2
009700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1254.2
009800 01  REC-CT                        PIC 99       VALUE ZERO.       IF1254.2
009900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1254.2
010000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1254.2
010100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1254.2
010200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1254.2
010300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1254.2
010400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1254.2
010500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1254.2
010600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1254.2
010700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1254.2
010800 01  CCVS-H-1.                                                    IF1254.2
010900     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1254.2
011000     02  FILLER                    PIC X(42)    VALUE             IF1254.2
011100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1254.2
011200     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1254.2
011300 01  CCVS-H-2A.                                                   IF1254.2
011400   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1254.2
011500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1254.2
011600   02  FILLER                        PIC XXXX   VALUE             IF1254.2
011700     "4.2 ".                                                      IF1254.2
011800   02  FILLER                        PIC X(28)  VALUE             IF1254.2
011900            " COPY - NOT FOR DISTRIBUTION".                       IF1254.2
012000   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1254.2
012100                                                                  IF1254.2
012200 01  CCVS-H-2B.                                                   IF1254.2
012300   02  FILLER                        PIC X(15)  VALUE             IF1254.2
012400            "TEST RESULT OF ".                                    IF1254.2
012500   02  TEST-ID                       PIC X(9).                    IF1254.2
012600   02  FILLER                        PIC X(4)   VALUE             IF1254.2
012700            " IN ".                                               IF1254.2
012800   02  FILLER                        PIC X(12)  VALUE             IF1254.2
012900     " HIGH       ".                                              IF1254.2
013000   02  FILLER                        PIC X(22)  VALUE             IF1254.2
013100            " LEVEL VALIDATION FOR ".                             IF1254.2
013200   02  FILLER                        PIC X(58)  VALUE             IF1254.2
013300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1254.2
013400 01  CCVS-H-3.                                                    IF1254.2
013500     02  FILLER                      PIC X(34)  VALUE             IF1254.2
013600            " FOR OFFICIAL USE ONLY    ".                         IF1254.2
013700     02  FILLER                      PIC X(58)  VALUE             IF1254.2
013800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1254.2
013900     02  FILLER                      PIC X(28)  VALUE             IF1254.2
014000            "  COPYRIGHT   1985 ".                                IF1254.2
014100 01  CCVS-E-1.                                                    IF1254.2
014200     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1254.2
014300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1254.2
014400     02 ID-AGAIN                     PIC X(9).                    IF1254.2
014500     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1254.2
014600 01  CCVS-E-2.                                                    IF1254.2
014700     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1254.2
014800     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1254.2
014900     02 CCVS-E-2-2.                                               IF1254.2
015000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1254.2
015100         03 FILLER                   PIC X      VALUE SPACE.      IF1254.2
015200         03 ENDER-DESC               PIC X(44)  VALUE             IF1254.2
015300            "ERRORS ENCOUNTERED".                                 IF1254.2
015400 01  CCVS-E-3.                                                    IF1254.2
015500     02  FILLER                      PIC X(22)  VALUE             IF1254.2
015600            " FOR OFFICIAL USE ONLY".                             IF1254.2
015700     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1254.2
015800     02  FILLER                      PIC X(58)  VALUE             IF1254.2
015900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1254.2
016000     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1254.2
016100     02 FILLER                       PIC X(15)  VALUE             IF1254.2
016200             " COPYRIGHT 1985".                                   IF1254.2
016300 01  CCVS-E-4.                                                    IF1254.2
016400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1254.2
016500     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1254.2
016600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1254.2
016700     02 FILLER                       PIC X(40)  VALUE             IF1254.2
016800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1254.2
016900 01  XXINFO.                                                      IF1254.2
017000     02 FILLER                       PIC X(19)  VALUE             IF1254.2
017100            "*** INFORMATION ***".                                IF1254.2
017200     02 INFO-TEXT.                                                IF1254.2
017300       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1254.2
017400       04 XXCOMPUTED                 PIC X(20).                   IF1254.2
017500       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1254.2
017600       04 XXCORRECT                  PIC X(20).                   IF1254.2
017700     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1254.2
017800 01  HYPHEN-LINE.                                                 IF1254.2
017900     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1254.2
018000     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1254.2
018100-    "*****************************************".                 IF1254.2
018200     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1254.2
018300-    "******************************".                            IF1254.2
018400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1254.2
018500     "IF125A".                                                    IF1254.2
018600 PROCEDURE DIVISION.                                              IF1254.2
018700 CCVS1 SECTION.                                                   IF1254.2
018800 OPEN-FILES.                                                      IF1254.2
018900     OPEN     OUTPUT PRINT-FILE.                                  IF1254.2
019000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1254.2
019100     MOVE    SPACE TO TEST-RESULTS.                               IF1254.2
019200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1254.2
019300     GO TO CCVS1-EXIT.                                            IF1254.2
019400 CLOSE-FILES.                                                     IF1254.2
019500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1254.2
019600 TERMINATE-CCVS.                                                  IF1254.2
019700     STOP     RUN.                                                IF1254.2
019800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1254.2
019900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1254.2
020000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1254.2
020100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1254.2
020200     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1254.2
020300 PRINT-DETAIL.                                                    IF1254.2
020400     IF REC-CT NOT EQUAL TO ZERO                                  IF1254.2
020500             MOVE "." TO PARDOT-X                                 IF1254.2
020600             MOVE REC-CT TO DOTVALUE.                             IF1254.2
020700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1254.2
020800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1254.2
020900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1254.2
021000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1254.2
021100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1254.2
021200     MOVE SPACE TO CORRECT-X.                                     IF1254.2
021300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1254.2
021400     MOVE     SPACE TO RE-MARK.                                   IF1254.2
021500 HEAD-ROUTINE.                                                    IF1254.2
021600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1254.2
021700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1254.2
021800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1254.2
021900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1254.2
022000 COLUMN-NAMES-ROUTINE.                                            IF1254.2
022100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1254.2
022200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1254.2
022300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1254.2
022400 END-ROUTINE.                                                     IF1254.2
022500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1254.2
022600 END-RTN-EXIT.                                                    IF1254.2
022700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1254.2
022800 END-ROUTINE-1.                                                   IF1254.2
022900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1254.2
023000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1254.2
023100      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1254.2
023200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1254.2
023300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1254.2
023400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1254.2
023500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1254.2
023600  END-ROUTINE-12.                                                 IF1254.2
023700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1254.2
023800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1254.2
023900         MOVE "NO " TO ERROR-TOTAL                                IF1254.2
024000         ELSE                                                     IF1254.2
024100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1254.2
024200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1254.2
024300     PERFORM WRITE-LINE.                                          IF1254.2
024400 END-ROUTINE-13.                                                  IF1254.2
024500     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1254.2
024600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1254.2
024700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1254.2
024800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1254.2
024900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1254.2
025000      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1254.2
025100          MOVE "NO " TO ERROR-TOTAL                               IF1254.2
025200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1254.2
025300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1254.2
025400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1254.2
025500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1254.2
025600 WRITE-LINE.                                                      IF1254.2
025700     ADD 1 TO RECORD-COUNT.                                       IF1254.2
025800*    IF RECORD-COUNT GREATER 42                                   IF1254.2
025900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1254.2
026000*        MOVE SPACE TO DUMMY-RECORD                               IF1254.2
026100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1254.2
026200*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1254.2
026300*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1254.2
026400*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1254.2
026500*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1254.2
026600*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1254.2
026700*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1254.2
026800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1254.2
026900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1254.2
027000*        MOVE ZERO TO RECORD-COUNT.                               IF1254.2
027100     PERFORM WRT-LN.                                              IF1254.2
027200 WRT-LN.                                                          IF1254.2
027300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1254.2
027400     MOVE SPACE TO DUMMY-RECORD.                                  IF1254.2
027500 BLANK-LINE-PRINT.                                                IF1254.2
027600     PERFORM WRT-LN.                                              IF1254.2
027700 FAIL-ROUTINE.                                                    IF1254.2
027800     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1254.2
027900            GO TO FAIL-ROUTINE-WRITE.                             IF1254.2
028000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1254.2
028100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1254.2
028200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1254.2
028300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1254.2
028400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1254.2
028500     GO TO  FAIL-ROUTINE-EX.                                      IF1254.2
028600 FAIL-ROUTINE-WRITE.                                              IF1254.2
028700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IF1254.2
028800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IF1254.2
028900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IF1254.2
029000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1254.2
029100 FAIL-ROUTINE-EX. EXIT.                                           IF1254.2
029200 BAIL-OUT.                                                        IF1254.2
029300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1254.2
029400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1254.2
029500 BAIL-OUT-WRITE.                                                  IF1254.2
029600     MOVE CORRECT-A TO XXCORRECT.                                 IF1254.2
029700     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1254.2
029800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1254.2
029900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1254.2
030000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1254.2
030100 BAIL-OUT-EX. EXIT.                                               IF1254.2
030200 CCVS1-EXIT.                                                      IF1254.2
030300     EXIT.                                                        IF1254.2
030400********************************************************          IF1254.2
030500*                                                      *          IF1254.2
030600*    Intrinsic Function Tests         IF125A - NUMVAL  *          IF1254.2
030700*                                                      *          IF1254.2
030800********************************************************          IF1254.2
030900 SECT-IF125A SECTION.                                             IF1254.2
031000 F-NUMVAL-INFO.                                                   IF1254.2
031100     MOVE     "See ref. A-58 2.29" TO ANSI-REFERENCE.             IF1254.2
031200     MOVE     "NUMVAL Function"     TO FEATURE.                   IF1254.2
031300*****************TEST (a) ******************************          IF1254.2
031400 F-NUMVAL-01.                                                     IF1254.2
031500     MOVE ZERO TO TEMP.                                           IF1254.2
031600 F-NUMVAL-TEST-01.                                                IF1254.2
031700     COMPUTE TEMP = FUNCTION NUMVAL ("9").                        IF1254.2
031800     IF TEMP = 9 THEN                                             IF1254.2
031900                        PERFORM PASS                              IF1254.2
032000     ELSE                                                         IF1254.2
032100                        MOVE 9 TO CORRECT-N                       IF1254.2
032200                        MOVE TEMP TO COMPUTED-N                   IF1254.2
032300                        PERFORM FAIL.                             IF1254.2
032400     GO TO F-NUMVAL-WRITE-01.                                     IF1254.2
032500 F-NUMVAL-DELETE-01.                                              IF1254.2
032600     PERFORM  DE-LETE.                                            IF1254.2
032700     GO TO    F-NUMVAL-WRITE-01.                                  IF1254.2
032800 F-NUMVAL-WRITE-01.                                               IF1254.2
032900     MOVE "F-NUMVAL-01" TO PAR-NAME.                              IF1254.2
033000     PERFORM  PRINT-DETAIL.                                       IF1254.2
033100*****************TEST (b) ******************************          IF1254.2
033200 F-NUMVAL-02.                                                     IF1254.2
033300     MOVE 77  TO TEMP.                                            IF1254.2
033400 F-NUMVAL-TEST-02.                                                IF1254.2
033500     EVALUATE FUNCTION NUMVAL ("4738")                            IF1254.2
033600              ALSO ( TEMP + 96 ) * 2                              IF1254.2
033700     WHEN     4738                                                IF1254.2
033800              ALSO 346                                            IF1254.2
033900        PERFORM PASS                                              IF1254.2
034000        GO TO F-NUMVAL-WRITE-02.                                  IF1254.2
034100     PERFORM FAIL.                                                IF1254.2
034200     GO TO    F-NUMVAL-WRITE-02.                                  IF1254.2
034300 F-NUMVAL-DELETE-02.                                              IF1254.2
034400     PERFORM  DE-LETE.                                            IF1254.2
034500     GO TO    F-NUMVAL-WRITE-02.                                  IF1254.2
034600 F-NUMVAL-WRITE-02.                                               IF1254.2
034700     MOVE "F-NUMVAL-02" TO PAR-NAME.                              IF1254.2
034800     PERFORM  PRINT-DETAIL.                                       IF1254.2
034900*****************TEST (c) ******************************          IF1254.2
035000 F-NUMVAL-TEST-03.                                                IF1254.2
035100     IF (FUNCTION NUMVAL (".935") >= 0.934981) AND                IF1254.2
035200        (FUNCTION NUMVAL (".935") <= 0.935019)                    IF1254.2
035300                        PERFORM PASS                              IF1254.2
035400     ELSE                                                         IF1254.2
035500                        PERFORM FAIL.                             IF1254.2
035600     GO TO F-NUMVAL-WRITE-03.                                     IF1254.2
035700 F-NUMVAL-DELETE-03.                                              IF1254.2
035800     PERFORM  DE-LETE.                                            IF1254.2
035900     GO TO    F-NUMVAL-WRITE-03.                                  IF1254.2
036000 F-NUMVAL-WRITE-03.                                               IF1254.2
036100     MOVE "F-NUMVAL-03" TO PAR-NAME.                              IF1254.2
036200     PERFORM  PRINT-DETAIL.                                       IF1254.2
036300*****************TEST (d) ******************************          IF1254.2
036400 F-NUMVAL-04.                                                     IF1254.2
036500     MOVE ZERO TO TEMP.                                           IF1254.2
036600 F-NUMVAL-TEST-04.                                                IF1254.2
036700     COMPUTE TEMP = FUNCTION NUMVAL ("385.93").                   IF1254.2
036800     IF (TEMP >= 385.922) AND                                     IF1254.2
036900        (TEMP <= 385.938)                                         IF1254.2
037000                        PERFORM PASS                              IF1254.2
037100     ELSE                                                         IF1254.2
037200                        MOVE  385.93  TO CORRECT-N                IF1254.2
037300                        MOVE TEMP TO COMPUTED-N                   IF1254.2
037400                        PERFORM FAIL.                             IF1254.2
037500     GO TO F-NUMVAL-WRITE-04.                                     IF1254.2
037600 F-NUMVAL-DELETE-04.                                              IF1254.2
037700     PERFORM  DE-LETE.                                            IF1254.2
037800     GO TO    F-NUMVAL-WRITE-04.                                  IF1254.2
037900 F-NUMVAL-WRITE-04.                                               IF1254.2
038000     MOVE "F-NUMVAL-04" TO PAR-NAME.                              IF1254.2
038100     PERFORM  PRINT-DETAIL.                                       IF1254.2
038200*****************TEST (e) ******************************          IF1254.2
038300 F-NUMVAL-05.                                                     IF1254.2
038400     MOVE ZERO TO TEMP.                                           IF1254.2
038500 F-NUMVAL-TEST-05.                                                IF1254.2
038600     COMPUTE TEMP = FUNCTION NUMVAL ("+394.2").                   IF1254.2
038700     IF (TEMP >= 394.192) AND                                     IF1254.2
038800        (TEMP <= 394.208)                                         IF1254.2
038900                        PERFORM PASS                              IF1254.2
039000     ELSE                                                         IF1254.2
039100                        MOVE  394.2  TO CORRECT-N                 IF1254.2
039200                        MOVE TEMP TO COMPUTED-N                   IF1254.2
039300                        PERFORM FAIL.                             IF1254.2
039400     GO TO F-NUMVAL-WRITE-05.                                     IF1254.2
039500 F-NUMVAL-DELETE-05.                                              IF1254.2
039600     PERFORM  DE-LETE.                                            IF1254.2
039700     GO TO    F-NUMVAL-WRITE-05.                                  IF1254.2
039800 F-NUMVAL-WRITE-05.                                               IF1254.2
039900     MOVE "F-NUMVAL-05" TO PAR-NAME.                              IF1254.2
040000     PERFORM  PRINT-DETAIL.                                       IF1254.2
040100*****************TEST (f) ******************************          IF1254.2
040200 F-NUMVAL-06.                                                     IF1254.2
040300     MOVE ZERO TO TEMP.                                           IF1254.2
040400 F-NUMVAL-TEST-06.                                                IF1254.2
040500     COMPUTE TEMP = FUNCTION NUMVAL ("  939.83").                 IF1254.2
040600     IF (TEMP >= 939.811) AND                                     IF1254.2
040700        (TEMP <= 939.849)                                         IF1254.2
040800                        PERFORM PASS                              IF1254.2
040900     ELSE                                                         IF1254.2
041000                        MOVE  939.83  TO CORRECT-N                IF1254.2
041100                        MOVE TEMP TO COMPUTED-N                   IF1254.2
041200                        PERFORM FAIL.                             IF1254.2
041300     GO TO F-NUMVAL-WRITE-06.                                     IF1254.2
041400 F-NUMVAL-DELETE-06.                                              IF1254.2
041500     PERFORM  DE-LETE.                                            IF1254.2
041600     GO TO    F-NUMVAL-WRITE-06.                                  IF1254.2
041700 F-NUMVAL-WRITE-06.                                               IF1254.2
041800     MOVE "F-NUMVAL-06" TO PAR-NAME.                              IF1254.2
041900     PERFORM  PRINT-DETAIL.                                       IF1254.2
042000*****************TEST (g) ******************************          IF1254.2
042100 F-NUMVAL-07.                                                     IF1254.2
042200     MOVE ZERO TO TEMP.                                           IF1254.2
042300 F-NUMVAL-TEST-07.                                                IF1254.2
042400     COMPUTE TEMP = FUNCTION NUMVAL ("   -  4929.0323").          IF1254.2
042500     IF (TEMP >= -4929.1309) AND                                  IF1254.2
042600        (TEMP <= -4928.9337)                                      IF1254.2
042700                        PERFORM PASS                              IF1254.2
042800     ELSE                                                         IF1254.2
042900                        MOVE  -4929.0323  TO CORRECT-N            IF1254.2
043000                        MOVE TEMP TO COMPUTED-N                   IF1254.2
043100                        PERFORM FAIL.                             IF1254.2
043200     GO TO F-NUMVAL-WRITE-07.                                     IF1254.2
043300 F-NUMVAL-DELETE-07.                                              IF1254.2
043400     PERFORM  DE-LETE.                                            IF1254.2
043500     GO TO    F-NUMVAL-WRITE-07.                                  IF1254.2
043600 F-NUMVAL-WRITE-07.                                               IF1254.2
043700     MOVE "F-NUMVAL-07" TO PAR-NAME.                              IF1254.2
043800     PERFORM  PRINT-DETAIL.                                       IF1254.2
043900*****************TEST (h) ******************************          IF1254.2
044000 F-NUMVAL-08.                                                     IF1254.2
044100     MOVE ZERO TO TEMP.                                           IF1254.2
044200 F-NUMVAL-TEST-08.                                                IF1254.2
044300     COMPUTE TEMP = FUNCTION NUMVAL ("82.9312+").                 IF1254.2
044400     IF (TEMP >= 82.9295) AND                                     IF1254.2
044500        (TEMP <= 82.9329)                                         IF1254.2
044600                        PERFORM PASS                              IF1254.2
044700     ELSE                                                         IF1254.2
044800                        MOVE  82.9312  TO CORRECT-N               IF1254.2
044900                        MOVE TEMP TO COMPUTED-N                   IF1254.2
045000                        PERFORM FAIL.                             IF1254.2
045100     GO TO F-NUMVAL-WRITE-08.                                     IF1254.2
045200 F-NUMVAL-DELETE-08.                                              IF1254.2
045300     PERFORM  DE-LETE.                                            IF1254.2
045400     GO TO    F-NUMVAL-WRITE-08.                                  IF1254.2
045500 F-NUMVAL-WRITE-08.                                               IF1254.2
045600     MOVE "F-NUMVAL-08" TO PAR-NAME.                              IF1254.2
045700     PERFORM  PRINT-DETAIL.                                       IF1254.2
045800*****************TEST (i) ******************************          IF1254.2
045900 F-NUMVAL-09.                                                     IF1254.2
046000     MOVE ZERO TO TEMP.                                           IF1254.2
046100 F-NUMVAL-TEST-09.                                                IF1254.2
046200     COMPUTE TEMP = FUNCTION NUMVAL ("   200.0002   - ").         IF1254.2
046300     IF (TEMP >= -200.0042) AND                                   IF1254.2
046400        (TEMP <= -199.9962)                                       IF1254.2
046500                        PERFORM PASS                              IF1254.2
046600     ELSE                                                         IF1254.2
046700                        MOVE  -200.0002  TO CORRECT-N             IF1254.2
046800                        MOVE TEMP TO COMPUTED-N                   IF1254.2
046900                        PERFORM FAIL.                             IF1254.2
047000     GO TO F-NUMVAL-WRITE-09.                                     IF1254.2
047100 F-NUMVAL-DELETE-09.                                              IF1254.2
047200     PERFORM  DE-LETE.                                            IF1254.2
047300     GO TO    F-NUMVAL-WRITE-09.                                  IF1254.2
047400 F-NUMVAL-WRITE-09.                                               IF1254.2
047500     MOVE "F-NUMVAL-09" TO PAR-NAME.                              IF1254.2
047600     PERFORM  PRINT-DETAIL.                                       IF1254.2
047700*****************TEST (j) ******************************          IF1254.2
047800 F-NUMVAL-10.                                                     IF1254.2
047900     MOVE ZERO TO TEMP.                                           IF1254.2
048000 F-NUMVAL-TEST-10.                                                IF1254.2
048100     COMPUTE TEMP = FUNCTION NUMVAL (A).                          IF1254.2
048200     IF TEMP = 4 THEN                                             IF1254.2
048300                        PERFORM PASS                              IF1254.2
048400     ELSE                                                         IF1254.2
048500                        MOVE  4  TO CORRECT-N                     IF1254.2
048600                        MOVE TEMP TO COMPUTED-N                   IF1254.2
048700                        PERFORM FAIL.                             IF1254.2
048800     GO TO F-NUMVAL-WRITE-10.                                     IF1254.2
048900 F-NUMVAL-DELETE-10.                                              IF1254.2
049000     PERFORM  DE-LETE.                                            IF1254.2
049100     GO TO    F-NUMVAL-WRITE-10.                                  IF1254.2
049200 F-NUMVAL-WRITE-10.                                               IF1254.2
049300     MOVE "F-NUMVAL-10" TO PAR-NAME.                              IF1254.2
049400     PERFORM  PRINT-DETAIL.                                       IF1254.2
049500*****************TEST (k) ******************************          IF1254.2
049600 F-NUMVAL-11.                                                     IF1254.2
049700     MOVE ZERO TO TEMP.                                           IF1254.2
049800 F-NUMVAL-TEST-11.                                                IF1254.2
049900     COMPUTE TEMP = FUNCTION NUMVAL (B).                          IF1254.2
050000     IF TEMP = 203 THEN                                           IF1254.2
050100                        PERFORM PASS                              IF1254.2
050200     ELSE                                                         IF1254.2
050300                        MOVE  203  TO CORRECT-N                   IF1254.2
050400                        MOVE TEMP TO COMPUTED-N                   IF1254.2
050500                        PERFORM FAIL.                             IF1254.2
050600     GO TO F-NUMVAL-WRITE-11.                                     IF1254.2
050700 F-NUMVAL-DELETE-11.                                              IF1254.2
050800     PERFORM  DE-LETE.                                            IF1254.2
050900     GO TO    F-NUMVAL-WRITE-11.                                  IF1254.2
051000 F-NUMVAL-WRITE-11.                                               IF1254.2
051100     MOVE "F-NUMVAL-11" TO PAR-NAME.                              IF1254.2
051200     PERFORM  PRINT-DETAIL.                                       IF1254.2
051300*****************TEST (l) ******************************          IF1254.2
051400 F-NUMVAL-12.                                                     IF1254.2
051500     MOVE ZERO TO TEMP.                                           IF1254.2
051600 F-NUMVAL-TEST-12.                                                IF1254.2
051700     COMPUTE TEMP = FUNCTION NUMVAL (C).                          IF1254.2
051800     IF (TEMP >= 0.428991) AND                                    IF1254.2
051900        (TEMP <= 0.429009)                                        IF1254.2
052000                        PERFORM PASS                              IF1254.2
052100     ELSE                                                         IF1254.2
052200                        MOVE  0.429  TO CORRECT-N                 IF1254.2
052300                        MOVE TEMP TO COMPUTED-N                   IF1254.2
052400                        PERFORM FAIL.                             IF1254.2
052500     GO TO F-NUMVAL-WRITE-12.                                     IF1254.2
052600 F-NUMVAL-DELETE-12.                                              IF1254.2
052700     PERFORM  DE-LETE.                                            IF1254.2
052800     GO TO    F-NUMVAL-WRITE-12.                                  IF1254.2
052900 F-NUMVAL-WRITE-12.                                               IF1254.2
053000     MOVE "F-NUMVAL-12" TO PAR-NAME.                              IF1254.2
053100     PERFORM  PRINT-DETAIL.                                       IF1254.2
053200*****************TEST (m) ******************************          IF1254.2
053300 F-NUMVAL-13.                                                     IF1254.2
053400     MOVE ZERO TO TEMP.                                           IF1254.2
053500 F-NUMVAL-TEST-13.                                                IF1254.2
053600     COMPUTE TEMP = FUNCTION NUMVAL (D).                          IF1254.2
053700     IF (TEMP >= 928.325) AND                                     IF1254.2
053800        (TEMP <= 928.363)                                         IF1254.2
053900                        PERFORM PASS                              IF1254.2
054000     ELSE                                                         IF1254.2
054100                        MOVE  928.344  TO CORRECT-N               IF1254.2
054200                        MOVE TEMP TO COMPUTED-N                   IF1254.2
054300                        PERFORM FAIL.                             IF1254.2
054400     GO TO F-NUMVAL-WRITE-13.                                     IF1254.2
054500 F-NUMVAL-DELETE-13.                                              IF1254.2
054600     PERFORM  DE-LETE.                                            IF1254.2
054700     GO TO    F-NUMVAL-WRITE-13.                                  IF1254.2
054800 F-NUMVAL-WRITE-13.                                               IF1254.2
054900     MOVE "F-NUMVAL-13" TO PAR-NAME.                              IF1254.2
055000     PERFORM  PRINT-DETAIL.                                       IF1254.2
055100*****************TEST (n) ******************************          IF1254.2
055200 F-NUMVAL-14.                                                     IF1254.2
055300     MOVE ZERO TO TEMP.                                           IF1254.2
055400 F-NUMVAL-TEST-14.                                                IF1254.2
055500     COMPUTE TEMP = FUNCTION NUMVAL (E).                          IF1254.2
055600     IF (TEMP >= -42.3248) AND                                    IF1254.2
055700        (TEMP <= -42.3232)                                        IF1254.2
055800                        PERFORM PASS                              IF1254.2
055900     ELSE                                                         IF1254.2
056000                        MOVE  -42.324      TO CORRECT-N           IF1254.2
056100                        MOVE TEMP TO COMPUTED-N                   IF1254.2
056200                        PERFORM FAIL.                             IF1254.2
056300     GO TO F-NUMVAL-WRITE-14.                                     IF1254.2
056400 F-NUMVAL-DELETE-14.                                              IF1254.2
056500     PERFORM  DE-LETE.                                            IF1254.2
056600     GO TO    F-NUMVAL-WRITE-14.                                  IF1254.2
056700 F-NUMVAL-WRITE-14.                                               IF1254.2
056800     MOVE "F-NUMVAL-14" TO PAR-NAME.                              IF1254.2
056900     PERFORM  PRINT-DETAIL.                                       IF1254.2
057000*****************TEST (o) ******************************          IF1254.2
057100 F-NUMVAL-15.                                                     IF1254.2
057200     MOVE ZERO TO TEMP.                                           IF1254.2
057300 F-NUMVAL-TEST-15.                                                IF1254.2
057400     COMPUTE TEMP = FUNCTION NUMVAL (F).                          IF1254.2
057500     IF (TEMP >= 22.9995) AND                                     IF1254.2
057600        (TEMP <= 23.0005)                                         IF1254.2
057700                        PERFORM PASS                              IF1254.2
057800     ELSE                                                         IF1254.2
057900                        MOVE  23.0  TO CORRECT-N                  IF1254.2
058000                        MOVE TEMP TO COMPUTED-N                   IF1254.2
058100                        PERFORM FAIL.                             IF1254.2
058200     GO TO F-NUMVAL-WRITE-15.                                     IF1254.2
058300 F-NUMVAL-DELETE-15.                                              IF1254.2
058400     PERFORM  DE-LETE.                                            IF1254.2
058500     GO TO    F-NUMVAL-WRITE-15.                                  IF1254.2
058600 F-NUMVAL-WRITE-15.                                               IF1254.2
058700     MOVE "F-NUMVAL-15" TO PAR-NAME.                              IF1254.2
058800     PERFORM  PRINT-DETAIL.                                       IF1254.2
058900*****************TEST (p) ******************************          IF1254.2
059000 F-NUMVAL-16.                                                     IF1254.2
059100     MOVE ZERO TO TEMP.                                           IF1254.2
059200 F-NUMVAL-TEST-16.                                                IF1254.2
059300     COMPUTE TEMP = FUNCTION NUMVAL (G).                          IF1254.2
059400     IF (TEMP >= -92926.16) AND                                   IF1254.2
059500        (TEMP <= -92922.44)                                       IF1254.2
059600                        PERFORM PASS                              IF1254.2
059700     ELSE                                                         IF1254.2
059800                        MOVE  -92924.3  TO CORRECT-N              IF1254.2
059900                        MOVE TEMP TO COMPUTED-N                   IF1254.2
060000                        PERFORM FAIL.                             IF1254.2
060100     GO TO F-NUMVAL-WRITE-16.                                     IF1254.2
060200 F-NUMVAL-DELETE-16.                                              IF1254.2
060300     PERFORM  DE-LETE.                                            IF1254.2
060400     GO TO    F-NUMVAL-WRITE-16.                                  IF1254.2
060500 F-NUMVAL-WRITE-16.                                               IF1254.2
060600     MOVE "F-NUMVAL-16" TO PAR-NAME.                              IF1254.2
060700     PERFORM  PRINT-DETAIL.                                       IF1254.2
060800*****************TEST (q) ******************************          IF1254.2
060900 F-NUMVAL-17.                                                     IF1254.2
061000     MOVE ZERO TO TEMP.                                           IF1254.2
061100 F-NUMVAL-TEST-17.                                                IF1254.2
061200     COMPUTE TEMP = FUNCTION NUMVAL (H).                          IF1254.2
061300     IF (TEMP >= 93.2081) AND                                     IF1254.2
061400        (TEMP <= 93.2119)                                         IF1254.2
061500                        PERFORM PASS                              IF1254.2
061600     ELSE                                                         IF1254.2
061700                        MOVE  93.21  TO CORRECT-N                 IF1254.2
061800                        MOVE TEMP TO COMPUTED-N                   IF1254.2
061900                        PERFORM FAIL.                             IF1254.2
062000     GO TO F-NUMVAL-WRITE-17.                                     IF1254.2
062100 F-NUMVAL-DELETE-17.                                              IF1254.2
062200     PERFORM  DE-LETE.                                            IF1254.2
062300     GO TO    F-NUMVAL-WRITE-17.                                  IF1254.2
062400 F-NUMVAL-WRITE-17.                                               IF1254.2
062500     MOVE "F-NUMVAL-17" TO PAR-NAME.                              IF1254.2
062600     PERFORM  PRINT-DETAIL.                                       IF1254.2
062700*****************TEST (r) ******************************          IF1254.2
062800 F-NUMVAL-18.                                                     IF1254.2
062900     MOVE ZERO TO TEMP.                                           IF1254.2
063000 F-NUMVAL-TEST-18.                                                IF1254.2
063100     COMPUTE TEMP = FUNCTION NUMVAL (I).                          IF1254.2
063200     IF (TEMP >= -92.9219) AND                                    IF1254.2
063300        (TEMP <= -92.9181)                                        IF1254.2
063400                        PERFORM PASS                              IF1254.2
063500     ELSE                                                         IF1254.2
063600                        MOVE  -92.92  TO CORRECT-N                IF1254.2
063700                        MOVE TEMP TO COMPUTED-N                   IF1254.2
063800                        PERFORM FAIL.                             IF1254.2
063900     GO TO F-NUMVAL-WRITE-18.                                     IF1254.2
064000 F-NUMVAL-DELETE-18.                                              IF1254.2
064100     PERFORM  DE-LETE.                                            IF1254.2
064200     GO TO    F-NUMVAL-WRITE-18.                                  IF1254.2
064300 F-NUMVAL-WRITE-18.                                               IF1254.2
064400     MOVE "F-NUMVAL-18" TO PAR-NAME.                              IF1254.2
064500     PERFORM  PRINT-DETAIL.                                       IF1254.2
064600*****************TEST (s) ******************************          IF1254.2
064700 F-NUMVAL-19.                                                     IF1254.2
064800     MOVE ZERO TO TEMP.                                           IF1254.2
064900 F-NUMVAL-TEST-19.                                                IF1254.2
065000     COMPUTE TEMP = (FUNCTION NUMVAL ("90") + 10).                IF1254.2
065100     IF TEMP = 100 THEN                                           IF1254.2
065200                        PERFORM PASS                              IF1254.2
065300     ELSE                                                         IF1254.2
065400                        MOVE  100  TO CORRECT-N                   IF1254.2
065500                        MOVE TEMP TO COMPUTED-N                   IF1254.2
065600                        PERFORM FAIL.                             IF1254.2
065700     GO TO F-NUMVAL-WRITE-19.                                     IF1254.2
065800 F-NUMVAL-DELETE-19.                                              IF1254.2
065900     PERFORM  DE-LETE.                                            IF1254.2
066000     GO TO    F-NUMVAL-WRITE-19.                                  IF1254.2
066100 F-NUMVAL-WRITE-19.                                               IF1254.2
066200     MOVE "F-NUMVAL-19" TO PAR-NAME.                              IF1254.2
066300     PERFORM  PRINT-DETAIL.                                       IF1254.2
066400*****************TEST (t) ******************************          IF1254.2
066500 F-NUMVAL-20.                                                     IF1254.2
066600     MOVE ZERO TO TEMP.                                           IF1254.2
066700 F-NUMVAL-TEST-20.                                                IF1254.2
066800     COMPUTE TEMP = (FUNCTION NUMVAL ("2") +                      IF1254.2
066900                     FUNCTION NUMVAL ("8") ).                     IF1254.2
067000     IF TEMP = 10 THEN                                            IF1254.2
067100                        PERFORM PASS                              IF1254.2
067200     ELSE                                                         IF1254.2
067300                        MOVE  10  TO CORRECT-N                    IF1254.2
067400                        MOVE TEMP TO COMPUTED-N                   IF1254.2
067500                        PERFORM FAIL.                             IF1254.2
067600     GO TO F-NUMVAL-WRITE-20.                                     IF1254.2
067700 F-NUMVAL-DELETE-20.                                              IF1254.2
067800     PERFORM  DE-LETE.                                            IF1254.2
067900     GO TO    F-NUMVAL-WRITE-20.                                  IF1254.2
068000 F-NUMVAL-WRITE-20.                                               IF1254.2
068100     MOVE "F-NUMVAL-20" TO PAR-NAME.                              IF1254.2
068200     PERFORM  PRINT-DETAIL.                                       IF1254.2
068300*******************END OF TESTS**************************         IF1254.2
068400 CCVS-EXIT SECTION.                                               IF1254.2
068500 CCVS-999999.                                                     IF1254.2
068600     GO TO CLOSE-FILES.                                           IF1254.2
      *END-OF,IF125A                                                                  
