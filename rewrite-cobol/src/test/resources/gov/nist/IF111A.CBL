      *HEADER,COBOL,IF111A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1114.2
000200 PROGRAM-ID.                                                      IF1114.2
000300     IF111A.                                                      IF1114.2
000400                                                                  IF1114.2
000500***********************************************************       IF1114.2
000600*                                                         *       IF1114.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1114.2
000800* It contains tests for the Intrinsic Function            *       IF1114.2
000900* INTEGER.                                                *       IF1114.2
001000*                                                         *       IF1114.2
001100***********************************************************       IF1114.2
001200 ENVIRONMENT DIVISION.                                            IF1114.2
001300 CONFIGURATION SECTION.                                           IF1114.2
001400 SOURCE-COMPUTER.                                                 IF1114.2
001500     XXXXX082.                                                    IF1114.2
001600 OBJECT-COMPUTER.                                                 IF1114.2
001700     XXXXX083.                                                    IF1114.2
001800 INPUT-OUTPUT SECTION.                                            IF1114.2
001900 FILE-CONTROL.                                                    IF1114.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1114.2
002100     XXXXX055.                                                    IF1114.2
002200 DATA DIVISION.                                                   IF1114.2
002300 FILE SECTION.                                                    IF1114.2
002400 FD  PRINT-FILE.                                                  IF1114.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1114.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1114.2
002700 WORKING-STORAGE SECTION.                                         IF1114.2
002800***********************************************************       IF1114.2
002900* Variables specific to the Intrinsic Function Test IF111A*       IF1114.2
003000***********************************************************       IF1114.2
003100 01  A                   PIC S9(10)          VALUE 500000.        IF1114.2
003200 01  B                   PIC S9(10)          VALUE 1.             IF1114.2
003300 01  E                   PIC S9(6)V9(5)      VALUE 399999.122.    IF1114.2
003400 01  F                   PIC S9(5)V9(5)      VALUE 0.00032.       IF1114.2
003500 01  G                   PIC S9(5)V9(5)      VALUE 4.08.          IF1114.2
003600 01  H                   PIC S9(5)V9(5)      VALUE -5.            IF1114.2
003700 01  I                   PIC S9(5)V9(5)      VALUE 3.4.           IF1114.2
003800 01  ARG1                PIC S9(5)V9(5)      VALUE 4.4.           IF1114.2
003900 01  ARR                                     VALUE "40537".       IF1114.2
004000     02  IND OCCURS 5 TIMES PIC 9.                                IF1114.2
004100 01  TEMP                PIC S9(5)V9(5).                          IF1114.2
004200 01  WS-INT              PIC S9(10).                              IF1114.2
004300*                                                                 IF1114.2
004400**********************************************************        IF1114.2
004500*                                                                 IF1114.2
004600 01  TEST-RESULTS.                                                IF1114.2
004700     02 FILLER                   PIC X      VALUE SPACE.          IF1114.2
004800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1114.2
004900     02 FILLER                   PIC X      VALUE SPACE.          IF1114.2
005000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1114.2
005100     02 FILLER                   PIC X      VALUE SPACE.          IF1114.2
005200     02  PAR-NAME.                                                IF1114.2
005300       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1114.2
005400       03  PARDOT-X              PIC X      VALUE SPACE.          IF1114.2
005500       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1114.2
005600     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1114.2
005700     02 RE-MARK                  PIC X(61).                       IF1114.2
005800 01  TEST-COMPUTED.                                               IF1114.2
005900     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1114.2
006000     02 FILLER                   PIC X(17)  VALUE                 IF1114.2
006100            "       COMPUTED=".                                   IF1114.2
006200     02 COMPUTED-X.                                               IF1114.2
006300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1114.2
006400     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1114.2
006500                                 PIC -9(9).9(9).                  IF1114.2
006600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1114.2
006700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1114.2
006800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1114.2
006900     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1114.2
007000         04 COMPUTED-18V0                    PIC -9(18).          IF1114.2
007100         04 FILLER                           PIC X.               IF1114.2
007200     03 FILLER PIC X(50) VALUE SPACE.                             IF1114.2
007300 01  TEST-CORRECT.                                                IF1114.2
007400     02 FILLER PIC X(30) VALUE SPACE.                             IF1114.2
007500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1114.2
007600     02 CORRECT-X.                                                IF1114.2
007700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1114.2
007800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1114.2
007900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1114.2
008000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1114.2
008100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1114.2
008200     03      CR-18V0 REDEFINES CORRECT-A.                         IF1114.2
008300         04 CORRECT-18V0                     PIC -9(18).          IF1114.2
008400         04 FILLER                           PIC X.               IF1114.2
008500     03 FILLER PIC X(2) VALUE SPACE.                              IF1114.2
008600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1114.2
008700 01  CCVS-C-1.                                                    IF1114.2
008800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1114.2
008900-    "SS  PARAGRAPH-NAME                                          IF1114.2
009000-    "       REMARKS".                                            IF1114.2
009100     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1114.2
009200 01  CCVS-C-2.                                                    IF1114.2
009300     02 FILLER                     PIC X        VALUE SPACE.      IF1114.2
009400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1114.2
009500     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1114.2
009600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1114.2
009700     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1114.2
009800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1114.2
009900 01  REC-CT                        PIC 99       VALUE ZERO.       IF1114.2
010000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1114.2
010100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1114.2
010200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1114.2
010300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1114.2
010400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1114.2
010500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1114.2
010600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1114.2
010700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1114.2
010800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1114.2
010900 01  CCVS-H-1.                                                    IF1114.2
011000     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1114.2
011100     02  FILLER                    PIC X(42)    VALUE             IF1114.2
011200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1114.2
011300     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1114.2
011400 01  CCVS-H-2A.                                                   IF1114.2
011500   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1114.2
011600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1114.2
011700   02  FILLER                        PIC XXXX   VALUE             IF1114.2
011800     "4.2 ".                                                      IF1114.2
011900   02  FILLER                        PIC X(28)  VALUE             IF1114.2
012000            " COPY - NOT FOR DISTRIBUTION".                       IF1114.2
012100   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1114.2
012200                                                                  IF1114.2
012300 01  CCVS-H-2B.                                                   IF1114.2
012400   02  FILLER                        PIC X(15)  VALUE             IF1114.2
012500            "TEST RESULT OF ".                                    IF1114.2
012600   02  TEST-ID                       PIC X(9).                    IF1114.2
012700   02  FILLER                        PIC X(4)   VALUE             IF1114.2
012800            " IN ".                                               IF1114.2
012900   02  FILLER                        PIC X(12)  VALUE             IF1114.2
013000     " HIGH       ".                                              IF1114.2
013100   02  FILLER                        PIC X(22)  VALUE             IF1114.2
013200            " LEVEL VALIDATION FOR ".                             IF1114.2
013300   02  FILLER                        PIC X(58)  VALUE             IF1114.2
013400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1114.2
013500 01  CCVS-H-3.                                                    IF1114.2
013600     02  FILLER                      PIC X(34)  VALUE             IF1114.2
013700            " FOR OFFICIAL USE ONLY    ".                         IF1114.2
013800     02  FILLER                      PIC X(58)  VALUE             IF1114.2
013900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1114.2
014000     02  FILLER                      PIC X(28)  VALUE             IF1114.2
014100            "  COPYRIGHT   1985 ".                                IF1114.2
014200 01  CCVS-E-1.                                                    IF1114.2
014300     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1114.2
014400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1114.2
014500     02 ID-AGAIN                     PIC X(9).                    IF1114.2
014600     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1114.2
014700 01  CCVS-E-2.                                                    IF1114.2
014800     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1114.2
014900     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1114.2
015000     02 CCVS-E-2-2.                                               IF1114.2
015100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1114.2
015200         03 FILLER                   PIC X      VALUE SPACE.      IF1114.2
015300         03 ENDER-DESC               PIC X(44)  VALUE             IF1114.2
015400            "ERRORS ENCOUNTERED".                                 IF1114.2
015500 01  CCVS-E-3.                                                    IF1114.2
015600     02  FILLER                      PIC X(22)  VALUE             IF1114.2
015700            " FOR OFFICIAL USE ONLY".                             IF1114.2
015800     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1114.2
015900     02  FILLER                      PIC X(58)  VALUE             IF1114.2
016000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1114.2
016100     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1114.2
016200     02 FILLER                       PIC X(15)  VALUE             IF1114.2
016300             " COPYRIGHT 1985".                                   IF1114.2
016400 01  CCVS-E-4.                                                    IF1114.2
016500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1114.2
016600     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1114.2
016700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1114.2
016800     02 FILLER                       PIC X(40)  VALUE             IF1114.2
016900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1114.2
017000 01  XXINFO.                                                      IF1114.2
017100     02 FILLER                       PIC X(19)  VALUE             IF1114.2
017200            "*** INFORMATION ***".                                IF1114.2
017300     02 INFO-TEXT.                                                IF1114.2
017400       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1114.2
017500       04 XXCOMPUTED                 PIC X(20).                   IF1114.2
017600       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1114.2
017700       04 XXCORRECT                  PIC X(20).                   IF1114.2
017800     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1114.2
017900 01  HYPHEN-LINE.                                                 IF1114.2
018000     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1114.2
018100     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1114.2
018200-    "*****************************************".                 IF1114.2
018300     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1114.2
018400-    "******************************".                            IF1114.2
018500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1114.2
018600     "IF111A".                                                    IF1114.2
018700 PROCEDURE DIVISION.                                              IF1114.2
018800 CCVS1 SECTION.                                                   IF1114.2
018900 OPEN-FILES.                                                      IF1114.2
019000     OPEN     OUTPUT PRINT-FILE.                                  IF1114.2
019100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1114.2
019200     MOVE    SPACE TO TEST-RESULTS.                               IF1114.2
019300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1114.2
019400     GO TO CCVS1-EXIT.                                            IF1114.2
019500 CLOSE-FILES.                                                     IF1114.2
019600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1114.2
019700 TERMINATE-CCVS.                                                  IF1114.2
019800     STOP     RUN.                                                IF1114.2
019900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1114.2
020000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1114.2
020100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1114.2
020200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1114.2
020300     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1114.2
020400 PRINT-DETAIL.                                                    IF1114.2
020500     IF REC-CT NOT EQUAL TO ZERO                                  IF1114.2
020600             MOVE "." TO PARDOT-X                                 IF1114.2
020700             MOVE REC-CT TO DOTVALUE.                             IF1114.2
020800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1114.2
020900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1114.2
021000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1114.2
021100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1114.2
021200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1114.2
021300     MOVE SPACE TO CORRECT-X.                                     IF1114.2
021400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1114.2
021500     MOVE     SPACE TO RE-MARK.                                   IF1114.2
021600 HEAD-ROUTINE.                                                    IF1114.2
021700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1114.2
021800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1114.2
021900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1114.2
022000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1114.2
022100 COLUMN-NAMES-ROUTINE.                                            IF1114.2
022200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1114.2
022300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1114.2
022400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1114.2
022500 END-ROUTINE.                                                     IF1114.2
022600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1114.2
022700 END-RTN-EXIT.                                                    IF1114.2
022800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1114.2
022900 END-ROUTINE-1.                                                   IF1114.2
023000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1114.2
023100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1114.2
023200      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1114.2
023300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1114.2
023400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1114.2
023500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1114.2
023600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1114.2
023700  END-ROUTINE-12.                                                 IF1114.2
023800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1114.2
023900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1114.2
024000         MOVE "NO " TO ERROR-TOTAL                                IF1114.2
024100         ELSE                                                     IF1114.2
024200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1114.2
024300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1114.2
024400     PERFORM WRITE-LINE.                                          IF1114.2
024500 END-ROUTINE-13.                                                  IF1114.2
024600     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1114.2
024700         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1114.2
024800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1114.2
024900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1114.2
025000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1114.2
025100      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1114.2
025200          MOVE "NO " TO ERROR-TOTAL                               IF1114.2
025300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1114.2
025400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1114.2
025500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1114.2
025600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1114.2
025700 WRITE-LINE.                                                      IF1114.2
025800     ADD 1 TO RECORD-COUNT.                                       IF1114.2
025900*    IF RECORD-COUNT GREATER 42                                   IF1114.2
026000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1114.2
026100*        MOVE SPACE TO DUMMY-RECORD                               IF1114.2
026200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1114.2
026300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1114.2
026400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1114.2
026500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1114.2
026600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1114.2
026700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1114.2
026800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1114.2
026900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1114.2
027000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1114.2
027100*        MOVE ZERO TO RECORD-COUNT.                               IF1114.2
027200     PERFORM WRT-LN.                                              IF1114.2
027300 WRT-LN.                                                          IF1114.2
027400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1114.2
027500     MOVE SPACE TO DUMMY-RECORD.                                  IF1114.2
027600 BLANK-LINE-PRINT.                                                IF1114.2
027700     PERFORM WRT-LN.                                              IF1114.2
027800 FAIL-ROUTINE.                                                    IF1114.2
027900     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1114.2
028000            GO TO FAIL-ROUTINE-WRITE.                             IF1114.2
028100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1114.2
028200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1114.2
028300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1114.2
028400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1114.2
028500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1114.2
028600     GO TO  FAIL-ROUTINE-EX.                                      IF1114.2
028700 FAIL-ROUTINE-WRITE.                                              IF1114.2
028800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IF1114.2
028900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IF1114.2
029000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IF1114.2
029100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1114.2
029200 FAIL-ROUTINE-EX. EXIT.                                           IF1114.2
029300 BAIL-OUT.                                                        IF1114.2
029400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1114.2
029500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1114.2
029600 BAIL-OUT-WRITE.                                                  IF1114.2
029700     MOVE CORRECT-A TO XXCORRECT.                                 IF1114.2
029800     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1114.2
029900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1114.2
030000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1114.2
030100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1114.2
030200 BAIL-OUT-EX. EXIT.                                               IF1114.2
030300 CCVS1-EXIT.                                                      IF1114.2
030400     EXIT.                                                        IF1114.2
030500********************************************************          IF1114.2
030600*                                                      *          IF1114.2
030700*    Intrinsic Function Tests         IF111A - INTEGER    *       IF1114.2
030800*                                                      *          IF1114.2
030900********************************************************          IF1114.2
031000 SECT-IF111A SECTION.                                             IF1114.2
031100 F-INTEGER-INFO.                                                  IF1114.2
031200     MOVE     "See ref. A-44 2.15" TO ANSI-REFERENCE.             IF1114.2
031300     MOVE     "INTEGER Function"     TO FEATURE.                  IF1114.2
031400*****************TEST (a) ******************************          IF1114.2
031500 F-INTEGER-01.                                                    IF1114.2
031600     MOVE ZERO TO WS-INT.                                         IF1114.2
031700 F-INTEGER-TEST-01.                                               IF1114.2
031800     COMPUTE WS-INT = FUNCTION INTEGER(0).                        IF1114.2
031900     IF WS-INT = 0 THEN                                           IF1114.2
032000                    PERFORM PASS                                  IF1114.2
032100     ELSE                                                         IF1114.2
032200                    MOVE  0  TO CORRECT-N                         IF1114.2
032300                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
032400                    PERFORM FAIL.                                 IF1114.2
032500     GO TO F-INTEGER-WRITE-01.                                    IF1114.2
032600 F-INTEGER-DELETE-01.                                             IF1114.2
032700     PERFORM  DE-LETE.                                            IF1114.2
032800     GO TO    F-INTEGER-WRITE-01.                                 IF1114.2
032900 F-INTEGER-WRITE-01.                                              IF1114.2
033000     MOVE "F-INTEGER-01" TO PAR-NAME.                             IF1114.2
033100     PERFORM  PRINT-DETAIL.                                       IF1114.2
033200*****************TEST (b) ******************************          IF1114.2
033300 F-INTEGER-02.                                                    IF1114.2
033400     MOVE ZERO TO WS-INT.                                         IF1114.2
033500 F-INTEGER-TEST-02.                                               IF1114.2
033600     EVALUATE FUNCTION INTEGER(3)                                 IF1114.2
033700     WHEN 3                                                       IF1114.2
033800        PERFORM PASS                                              IF1114.2
033900        GO TO F-INTEGER-WRITE-02.                                 IF1114.2
034000     PERFORM FAIL.                                                IF1114.2
034100     GO TO F-INTEGER-WRITE-02.                                    IF1114.2
034200 F-INTEGER-DELETE-02.                                             IF1114.2
034300     PERFORM  DE-LETE.                                            IF1114.2
034400     GO TO    F-INTEGER-WRITE-02.                                 IF1114.2
034500 F-INTEGER-WRITE-02.                                              IF1114.2
034600     MOVE "F-INTEGER-02" TO PAR-NAME.                             IF1114.2
034700     PERFORM  PRINT-DETAIL.                                       IF1114.2
034800*****************TEST (c) ******************************          IF1114.2
034900 F-INTEGER-03.                                                    IF1114.2
035000     MOVE ZERO TO WS-INT.                                         IF1114.2
035100 F-INTEGER-TEST-03.                                               IF1114.2
035200     IF FUNCTION INTEGER(4.578) = 4 THEN                          IF1114.2
035300                    PERFORM PASS                                  IF1114.2
035400     ELSE                                                         IF1114.2
035500                    MOVE  4  TO CORRECT-N                         IF1114.2
035600                    PERFORM FAIL.                                 IF1114.2
035700     GO TO F-INTEGER-WRITE-03.                                    IF1114.2
035800 F-INTEGER-DELETE-03.                                             IF1114.2
035900     PERFORM  DE-LETE.                                            IF1114.2
036000     GO TO    F-INTEGER-WRITE-03.                                 IF1114.2
036100 F-INTEGER-WRITE-03.                                              IF1114.2
036200     MOVE "F-INTEGER-03" TO PAR-NAME.                             IF1114.2
036300     PERFORM  PRINT-DETAIL.                                       IF1114.2
036400*****************TEST (d) ******************************          IF1114.2
036500 F-INTEGER-04.                                                    IF1114.2
036600     MOVE ZERO TO WS-INT.                                         IF1114.2
036700 F-INTEGER-TEST-04.                                               IF1114.2
036800     COMPUTE WS-INT = FUNCTION INTEGER(-58).                      IF1114.2
036900     IF WS-INT = -58 THEN                                         IF1114.2
037000                    PERFORM PASS                                  IF1114.2
037100     ELSE                                                         IF1114.2
037200                    MOVE  -58  TO CORRECT-N                       IF1114.2
037300                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
037400                    PERFORM FAIL.                                 IF1114.2
037500     GO TO F-INTEGER-WRITE-04.                                    IF1114.2
037600 F-INTEGER-DELETE-04.                                             IF1114.2
037700     PERFORM  DE-LETE.                                            IF1114.2
037800     GO TO    F-INTEGER-WRITE-04.                                 IF1114.2
037900 F-INTEGER-WRITE-04.                                              IF1114.2
038000     MOVE "F-INTEGER-04" TO PAR-NAME.                             IF1114.2
038100     PERFORM  PRINT-DETAIL.                                       IF1114.2
038200*****************TEST (e) ******************************          IF1114.2
038300 F-INTEGER-05.                                                    IF1114.2
038400     MOVE ZERO TO WS-INT.                                         IF1114.2
038500 F-INTEGER-TEST-05.                                               IF1114.2
038600     COMPUTE WS-INT = FUNCTION INTEGER(-9.763).                   IF1114.2
038700     IF WS-INT = -10 THEN                                         IF1114.2
038800                    PERFORM PASS                                  IF1114.2
038900     ELSE                                                         IF1114.2
039000                    MOVE -10  TO CORRECT-N                        IF1114.2
039100                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
039200                    PERFORM FAIL.                                 IF1114.2
039300     GO TO F-INTEGER-WRITE-05.                                    IF1114.2
039400 F-INTEGER-DELETE-05.                                             IF1114.2
039500     PERFORM  DE-LETE.                                            IF1114.2
039600     GO TO    F-INTEGER-WRITE-05.                                 IF1114.2
039700 F-INTEGER-WRITE-05.                                              IF1114.2
039800     MOVE "F-INTEGER-05" TO PAR-NAME.                             IF1114.2
039900     PERFORM  PRINT-DETAIL.                                       IF1114.2
040000*****************TEST (f) ******************************          IF1114.2
040100 F-INTEGER-06.                                                    IF1114.2
040200     MOVE ZERO TO WS-INT.                                         IF1114.2
040300 F-INTEGER-TEST-06.                                               IF1114.2
040400     COMPUTE WS-INT = FUNCTION INTEGER(320485).                   IF1114.2
040500     IF WS-INT = 320485 THEN                                      IF1114.2
040600                    PERFORM PASS                                  IF1114.2
040700     ELSE                                                         IF1114.2
040800                    MOVE  320485  TO CORRECT-N                    IF1114.2
040900                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
041000                    PERFORM FAIL.                                 IF1114.2
041100     GO TO F-INTEGER-WRITE-06.                                    IF1114.2
041200 F-INTEGER-DELETE-06.                                             IF1114.2
041300     PERFORM  DE-LETE.                                            IF1114.2
041400     GO TO    F-INTEGER-WRITE-06.                                 IF1114.2
041500 F-INTEGER-WRITE-06.                                              IF1114.2
041600     MOVE "F-INTEGER-06" TO PAR-NAME.                             IF1114.2
041700     PERFORM  PRINT-DETAIL.                                       IF1114.2
041800*****************TEST (g) ******************************          IF1114.2
041900 F-INTEGER-07.                                                    IF1114.2
042000     MOVE ZERO TO WS-INT.                                         IF1114.2
042100 F-INTEGER-TEST-07.                                               IF1114.2
042200     COMPUTE WS-INT = FUNCTION INTEGER(230492.4828).              IF1114.2
042300     IF WS-INT = 230492 THEN                                      IF1114.2
042400                    PERFORM PASS                                  IF1114.2
042500     ELSE                                                         IF1114.2
042600                    MOVE  230492  TO CORRECT-N                    IF1114.2
042700                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
042800                    PERFORM FAIL.                                 IF1114.2
042900     GO TO F-INTEGER-WRITE-07.                                    IF1114.2
043000 F-INTEGER-DELETE-07.                                             IF1114.2
043100     PERFORM  DE-LETE.                                            IF1114.2
043200     GO TO    F-INTEGER-WRITE-07.                                 IF1114.2
043300 F-INTEGER-WRITE-07.                                              IF1114.2
043400     MOVE "F-INTEGER-07" TO PAR-NAME.                             IF1114.2
043500     PERFORM  PRINT-DETAIL.                                       IF1114.2
043600*****************TEST (h) ******************************          IF1114.2
043700 F-INTEGER-08.                                                    IF1114.2
043800     MOVE ZERO TO WS-INT.                                         IF1114.2
043900 F-INTEGER-TEST-08.                                               IF1114.2
044000     COMPUTE WS-INT = FUNCTION INTEGER(0.00032).                  IF1114.2
044100     IF WS-INT = 0 THEN                                           IF1114.2
044200                    PERFORM PASS                                  IF1114.2
044300     ELSE                                                         IF1114.2
044400                    MOVE  0  TO CORRECT-N                         IF1114.2
044500                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
044600                    PERFORM FAIL.                                 IF1114.2
044700     GO TO F-INTEGER-WRITE-08.                                    IF1114.2
044800 F-INTEGER-DELETE-08.                                             IF1114.2
044900     PERFORM  DE-LETE.                                            IF1114.2
045000     GO TO    F-INTEGER-WRITE-08.                                 IF1114.2
045100 F-INTEGER-WRITE-08.                                              IF1114.2
045200     MOVE "F-INTEGER-08" TO PAR-NAME.                             IF1114.2
045300     PERFORM  PRINT-DETAIL.                                       IF1114.2
045400*****************TEST (i) ******************************          IF1114.2
045500 F-INTEGER-09.                                                    IF1114.2
045600     MOVE ZERO TO WS-INT.                                         IF1114.2
045700 F-INTEGER-TEST-09.                                               IF1114.2
045800     COMPUTE WS-INT = FUNCTION INTEGER(A).                        IF1114.2
045900     IF WS-INT = 500000 THEN                                      IF1114.2
046000                    PERFORM PASS                                  IF1114.2
046100     ELSE                                                         IF1114.2
046200                    MOVE  500000  TO CORRECT-N                    IF1114.2
046300                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
046400                    PERFORM FAIL.                                 IF1114.2
046500     GO TO F-INTEGER-WRITE-09.                                    IF1114.2
046600 F-INTEGER-DELETE-09.                                             IF1114.2
046700     PERFORM  DE-LETE.                                            IF1114.2
046800     GO TO    F-INTEGER-WRITE-09.                                 IF1114.2
046900 F-INTEGER-WRITE-09.                                              IF1114.2
047000     MOVE "F-INTEGER-09" TO PAR-NAME.                             IF1114.2
047100     PERFORM  PRINT-DETAIL.                                       IF1114.2
047200*****************TEST (j) ******************************          IF1114.2
047300 F-INTEGER-10.                                                    IF1114.2
047400     MOVE ZERO TO WS-INT.                                         IF1114.2
047500 F-INTEGER-TEST-10.                                               IF1114.2
047600     COMPUTE WS-INT = FUNCTION INTEGER(E).                        IF1114.2
047700     IF WS-INT = 399999 THEN                                      IF1114.2
047800                    PERFORM PASS                                  IF1114.2
047900     ELSE                                                         IF1114.2
048000                    MOVE  399999  TO CORRECT-N                    IF1114.2
048100                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
048200                    PERFORM FAIL.                                 IF1114.2
048300     GO TO F-INTEGER-WRITE-10.                                    IF1114.2
048400 F-INTEGER-DELETE-10.                                             IF1114.2
048500     PERFORM  DE-LETE.                                            IF1114.2
048600     GO TO    F-INTEGER-WRITE-10.                                 IF1114.2
048700 F-INTEGER-WRITE-10.                                              IF1114.2
048800     MOVE "F-INTEGER-10" TO PAR-NAME.                             IF1114.2
048900     PERFORM  PRINT-DETAIL.                                       IF1114.2
049000*****************TEST (k) ******************************          IF1114.2
049100 F-INTEGER-11.                                                    IF1114.2
049200     MOVE ZERO TO WS-INT.                                         IF1114.2
049300 F-INTEGER-TEST-11.                                               IF1114.2
049400     COMPUTE WS-INT = FUNCTION INTEGER(B).                        IF1114.2
049500     IF WS-INT = 1 THEN                                           IF1114.2
049600                    PERFORM PASS                                  IF1114.2
049700     ELSE                                                         IF1114.2
049800                    MOVE  1  TO CORRECT-N                         IF1114.2
049900                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
050000                    PERFORM FAIL.                                 IF1114.2
050100     GO TO F-INTEGER-WRITE-11.                                    IF1114.2
050200 F-INTEGER-DELETE-11.                                             IF1114.2
050300     PERFORM  DE-LETE.                                            IF1114.2
050400     GO TO    F-INTEGER-WRITE-11.                                 IF1114.2
050500 F-INTEGER-WRITE-11.                                              IF1114.2
050600     MOVE "F-INTEGER-11" TO PAR-NAME.                             IF1114.2
050700     PERFORM  PRINT-DETAIL.                                       IF1114.2
050800*****************TEST (l) ******************************          IF1114.2
050900 F-INTEGER-12.                                                    IF1114.2
051000     MOVE ZERO TO WS-INT.                                         IF1114.2
051100 F-INTEGER-TEST-12.                                               IF1114.2
051200     COMPUTE WS-INT = FUNCTION INTEGER(F).                        IF1114.2
051300     IF WS-INT = 0 THEN                                           IF1114.2
051400                    PERFORM PASS                                  IF1114.2
051500     ELSE                                                         IF1114.2
051600                    MOVE  0  TO CORRECT-N                         IF1114.2
051700                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
051800                    PERFORM FAIL.                                 IF1114.2
051900     GO TO F-INTEGER-WRITE-12.                                    IF1114.2
052000 F-INTEGER-DELETE-12.                                             IF1114.2
052100     PERFORM  DE-LETE.                                            IF1114.2
052200     GO TO    F-INTEGER-WRITE-12.                                 IF1114.2
052300 F-INTEGER-WRITE-12.                                              IF1114.2
052400     MOVE "F-INTEGER-12" TO PAR-NAME.                             IF1114.2
052500     PERFORM  PRINT-DETAIL.                                       IF1114.2
052600*****************TEST (m) ******************************          IF1114.2
052700 F-INTEGER-13.                                                    IF1114.2
052800     MOVE ZERO TO WS-INT.                                         IF1114.2
052900 F-INTEGER-TEST-13.                                               IF1114.2
053000     COMPUTE WS-INT = FUNCTION INTEGER(IND(2)).                   IF1114.2
053100     IF WS-INT = 0 THEN                                           IF1114.2
053200                    PERFORM PASS                                  IF1114.2
053300     ELSE                                                         IF1114.2
053400                    MOVE  0  TO CORRECT-N                         IF1114.2
053500                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
053600                    PERFORM FAIL.                                 IF1114.2
053700     GO TO F-INTEGER-WRITE-13.                                    IF1114.2
053800 F-INTEGER-DELETE-13.                                             IF1114.2
053900     PERFORM  DE-LETE.                                            IF1114.2
054000     GO TO    F-INTEGER-WRITE-13.                                 IF1114.2
054100 F-INTEGER-WRITE-13.                                              IF1114.2
054200     MOVE "F-INTEGER-13" TO PAR-NAME.                             IF1114.2
054300     PERFORM  PRINT-DETAIL.                                       IF1114.2
054400*****************TEST (n) ******************************          IF1114.2
054500 F-INTEGER-14.                                                    IF1114.2
054600     MOVE ZERO TO WS-INT.                                         IF1114.2
054700 F-INTEGER-TEST-14.                                               IF1114.2
054800     COMPUTE WS-INT = FUNCTION INTEGER(IND(B)).                   IF1114.2
054900     IF WS-INT = 4 THEN                                           IF1114.2
055000                    PERFORM PASS                                  IF1114.2
055100     ELSE                                                         IF1114.2
055200                    MOVE  4  TO CORRECT-N                         IF1114.2
055300                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
055400                    PERFORM FAIL.                                 IF1114.2
055500     GO TO F-INTEGER-WRITE-14.                                    IF1114.2
055600 F-INTEGER-DELETE-14.                                             IF1114.2
055700     PERFORM  DE-LETE.                                            IF1114.2
055800     GO TO    F-INTEGER-WRITE-14.                                 IF1114.2
055900 F-INTEGER-WRITE-14.                                              IF1114.2
056000     MOVE "F-INTEGER-14" TO PAR-NAME.                             IF1114.2
056100     PERFORM  PRINT-DETAIL.                                       IF1114.2
056200*****************TEST (o) ******************************          IF1114.2
056300 F-INTEGER-15.                                                    IF1114.2
056400     MOVE ZERO TO WS-INT.                                         IF1114.2
056500 F-INTEGER-TEST-15.                                               IF1114.2
056600     COMPUTE WS-INT = FUNCTION INTEGER((6 / 3) + 9).              IF1114.2
056700     IF WS-INT = 11 THEN                                          IF1114.2
056800                    PERFORM PASS                                  IF1114.2
056900     ELSE                                                         IF1114.2
057000                    MOVE  11  TO CORRECT-N                        IF1114.2
057100                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
057200                    PERFORM FAIL.                                 IF1114.2
057300     GO TO F-INTEGER-WRITE-15.                                    IF1114.2
057400 F-INTEGER-DELETE-15.                                             IF1114.2
057500     PERFORM  DE-LETE.                                            IF1114.2
057600     GO TO    F-INTEGER-WRITE-15.                                 IF1114.2
057700 F-INTEGER-WRITE-15.                                              IF1114.2
057800     MOVE "F-INTEGER-15" TO PAR-NAME.                             IF1114.2
057900     PERFORM  PRINT-DETAIL.                                       IF1114.2
058000*****************TEST (p) ******************************          IF1114.2
058100 F-INTEGER-16.                                                    IF1114.2
058200     MOVE ZERO TO WS-INT.                                         IF1114.2
058300 F-INTEGER-TEST-16.                                               IF1114.2
058400     COMPUTE WS-INT = FUNCTION INTEGER(H + B).                    IF1114.2
058500     IF WS-INT = -4 THEN                                          IF1114.2
058600                    PERFORM PASS                                  IF1114.2
058700     ELSE                                                         IF1114.2
058800                    MOVE  -4  TO CORRECT-N                        IF1114.2
058900                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
059000                    PERFORM FAIL.                                 IF1114.2
059100     GO TO F-INTEGER-WRITE-16.                                    IF1114.2
059200 F-INTEGER-DELETE-16.                                             IF1114.2
059300     PERFORM  DE-LETE.                                            IF1114.2
059400     GO TO    F-INTEGER-WRITE-16.                                 IF1114.2
059500 F-INTEGER-WRITE-16.                                              IF1114.2
059600     MOVE "F-INTEGER-16" TO PAR-NAME.                             IF1114.2
059700     PERFORM  PRINT-DETAIL.                                       IF1114.2
059800*****************TEST (q) ******************************          IF1114.2
059900 F-INTEGER-17.                                                    IF1114.2
060000     MOVE ZERO TO WS-INT.                                         IF1114.2
060100 F-INTEGER-TEST-17.                                               IF1114.2
060200     COMPUTE WS-INT = FUNCTION INTEGER(6.3 - 4.2 / 2).            IF1114.2
060300     IF WS-INT = 4 THEN                                           IF1114.2
060400                    PERFORM PASS                                  IF1114.2
060500     ELSE                                                         IF1114.2
060600                    MOVE  4  TO CORRECT-N                         IF1114.2
060700                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
060800                    PERFORM FAIL.                                 IF1114.2
060900     GO TO F-INTEGER-WRITE-17.                                    IF1114.2
061000 F-INTEGER-DELETE-17.                                             IF1114.2
061100     PERFORM  DE-LETE.                                            IF1114.2
061200     GO TO    F-INTEGER-WRITE-17.                                 IF1114.2
061300 F-INTEGER-WRITE-17.                                              IF1114.2
061400     MOVE "F-INTEGER-17" TO PAR-NAME.                             IF1114.2
061500     PERFORM  PRINT-DETAIL.                                       IF1114.2
061600*****************TEST (r) ******************************          IF1114.2
061700 F-INTEGER-18.                                                    IF1114.2
061800     MOVE ZERO TO WS-INT.                                         IF1114.2
061900 F-INTEGER-TEST-18.                                               IF1114.2
062000     COMPUTE WS-INT = FUNCTION INTEGER((H + G) * I).              IF1114.2
062100     IF WS-INT = -4 THEN                                          IF1114.2
062200                    PERFORM PASS                                  IF1114.2
062300     ELSE                                                         IF1114.2
062400                    MOVE  -4  TO CORRECT-N                        IF1114.2
062500                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
062600                    PERFORM FAIL.                                 IF1114.2
062700     GO TO F-INTEGER-WRITE-18.                                    IF1114.2
062800 F-INTEGER-DELETE-18.                                             IF1114.2
062900     PERFORM  DE-LETE.                                            IF1114.2
063000     GO TO    F-INTEGER-WRITE-18.                                 IF1114.2
063100 F-INTEGER-WRITE-18.                                              IF1114.2
063200     MOVE "F-INTEGER-18" TO PAR-NAME.                             IF1114.2
063300     PERFORM  PRINT-DETAIL.                                       IF1114.2
063400*****************TEST (s) ******************************          IF1114.2
063500 F-INTEGER-19.                                                    IF1114.2
063600     MOVE ZERO TO WS-INT.                                         IF1114.2
063700 F-INTEGER-TEST-19.                                               IF1114.2
063800     COMPUTE WS-INT = FUNCTION INTEGER(H / 5).                    IF1114.2
063900     IF WS-INT = -1 THEN                                          IF1114.2
064000                    PERFORM PASS                                  IF1114.2
064100     ELSE                                                         IF1114.2
064200                    MOVE  -1  TO CORRECT-N                        IF1114.2
064300                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
064400                    PERFORM FAIL.                                 IF1114.2
064500     GO TO F-INTEGER-WRITE-19.                                    IF1114.2
064600 F-INTEGER-DELETE-19.                                             IF1114.2
064700     PERFORM  DE-LETE.                                            IF1114.2
064800     GO TO    F-INTEGER-WRITE-19.                                 IF1114.2
064900 F-INTEGER-WRITE-19.                                              IF1114.2
065000     MOVE "F-INTEGER-19" TO PAR-NAME.                             IF1114.2
065100     PERFORM  PRINT-DETAIL.                                       IF1114.2
065200*****************TEST (t) ******************************          IF1114.2
065300 F-INTEGER-20.                                                    IF1114.2
065400     MOVE ZERO TO TEMP.                                           IF1114.2
065500 F-INTEGER-TEST-20.                                               IF1114.2
065600     COMPUTE TEMP = FUNCTION INTEGER(3.2) + I.                    IF1114.2
065700     IF (TEMP >= 6.39987) AND                                     IF1114.2
065800        (TEMP <= 6.40013)                                         IF1114.2
065900                    PERFORM PASS                                  IF1114.2
066000     ELSE                                                         IF1114.2
066100                    MOVE  6.4  TO CORRECT-N                       IF1114.2
066200                    MOVE TEMP TO COMPUTED-N                       IF1114.2
066300                    PERFORM FAIL.                                 IF1114.2
066400     GO TO F-INTEGER-WRITE-20.                                    IF1114.2
066500 F-INTEGER-DELETE-20.                                             IF1114.2
066600     PERFORM  DE-LETE.                                            IF1114.2
066700     GO TO    F-INTEGER-WRITE-20.                                 IF1114.2
066800 F-INTEGER-WRITE-20.                                              IF1114.2
066900     MOVE "F-INTEGER-20" TO PAR-NAME.                             IF1114.2
067000     PERFORM  PRINT-DETAIL.                                       IF1114.2
067100*****************TEST (u) ******************************          IF1114.2
067200 F-INTEGER-21.                                                    IF1114.2
067300     MOVE ZERO TO WS-INT.                                         IF1114.2
067400 F-INTEGER-TEST-21.                                               IF1114.2
067500     COMPUTE WS-INT = FUNCTION INTEGER(FUNCTION INTEGER(1.6)).    IF1114.2
067600     IF WS-INT = 1 THEN                                           IF1114.2
067700                    PERFORM PASS                                  IF1114.2
067800     ELSE                                                         IF1114.2
067900                    MOVE  1  TO CORRECT-N                         IF1114.2
068000                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
068100                    PERFORM FAIL.                                 IF1114.2
068200     GO TO F-INTEGER-WRITE-21.                                    IF1114.2
068300 F-INTEGER-DELETE-21.                                             IF1114.2
068400     PERFORM  DE-LETE.                                            IF1114.2
068500     GO TO    F-INTEGER-WRITE-21.                                 IF1114.2
068600 F-INTEGER-WRITE-21.                                              IF1114.2
068700     MOVE "F-INTEGER-21" TO PAR-NAME.                             IF1114.2
068800     PERFORM  PRINT-DETAIL.                                       IF1114.2
068900*****************TEST (v) ******************************          IF1114.2
069000 F-INTEGER-22.                                                    IF1114.2
069100     MOVE ZERO TO WS-INT.                                         IF1114.2
069200 F-INTEGER-TEST-22.                                               IF1114.2
069300     COMPUTE WS-INT = FUNCTION INTEGER(1.2) +                     IF1114.2
069400                      FUNCTION INTEGER(1.6).                      IF1114.2
069500     IF WS-INT = 2 THEN                                           IF1114.2
069600                    PERFORM PASS                                  IF1114.2
069700     ELSE                                                         IF1114.2
069800                    MOVE  2  TO CORRECT-N                         IF1114.2
069900                    MOVE WS-INT TO COMPUTED-N                     IF1114.2
070000                    PERFORM FAIL.                                 IF1114.2
070100     GO TO F-INTEGER-WRITE-22.                                    IF1114.2
070200 F-INTEGER-DELETE-22.                                             IF1114.2
070300     PERFORM  DE-LETE.                                            IF1114.2
070400     GO TO    F-INTEGER-WRITE-22.                                 IF1114.2
070500 F-INTEGER-WRITE-22.                                              IF1114.2
070600     MOVE "F-INTEGER-22" TO PAR-NAME.                             IF1114.2
070700     PERFORM  PRINT-DETAIL.                                       IF1114.2
070800***************** SPECIAL TEST 1 ***********************          IF1114.2
070900 F-DATEOFINT-23.                                                  IF1114.2
071000*** ARG1:=4.4 ***                                                 IF1114.2
071100     PERFORM F-DATEOFINT-TEST-23                                  IF1114.2
071200             UNTIL FUNCTION INTEGER(ARG1) < 0.                    IF1114.2
071300     IF ARG1 < 0 THEN                                             IF1114.2
071400                            PERFORM PASS                          IF1114.2
071500     ELSE                                                         IF1114.2
071600                            PERFORM FAIL.                         IF1114.2
071700     GO TO F-DATEOFINT-WRITE-23.                                  IF1114.2
071800*                                                                 IF1114.2
071900 F-DATEOFINT-TEST-23.                                             IF1114.2
072000     COMPUTE ARG1 = ARG1 - 1.                                     IF1114.2
072100*                                                                 IF1114.2
072200 F-DATEOFINT-DELETE-23.                                           IF1114.2
072300     PERFORM  DE-LETE.                                            IF1114.2
072400     GO TO    F-DATEOFINT-WRITE-23.                               IF1114.2
072500 F-DATEOFINT-WRITE-23.                                            IF1114.2
072600     MOVE "F-DATEOFINT-23" TO PAR-NAME.                           IF1114.2
072700     PERFORM  PRINT-DETAIL.                                       IF1114.2
072800*******************END OF TESTS**************************         IF1114.2
072900 CCVS-EXIT SECTION.                                               IF1114.2
073000 CCVS-999999.                                                     IF1114.2
073100     GO TO CLOSE-FILES.                                           IF1114.2
      *END-OF,IF111A                                                                  
