      *HEADER,COBOL,IF116A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1164.2
000200 PROGRAM-ID.                                                      IF1164.2
000300     IF116A.                                                      IF1164.2
000400                                                                  IF1164.2
000500***********************************************************       IF1164.2
000600*                                                         *       IF1164.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1164.2
000800* It contains tests for the Intrinsic Function LOG.       *       IF1164.2
000900*                                                         *       IF1164.2
001000***********************************************************       IF1164.2
001100 ENVIRONMENT DIVISION.                                            IF1164.2
001200 CONFIGURATION SECTION.                                           IF1164.2
001300 SOURCE-COMPUTER.                                                 IF1164.2
001400     XXXXX082.                                                    IF1164.2
001500 OBJECT-COMPUTER.                                                 IF1164.2
001600     XXXXX083.                                                    IF1164.2
001700 INPUT-OUTPUT SECTION.                                            IF1164.2
001800 FILE-CONTROL.                                                    IF1164.2
001900     SELECT PRINT-FILE ASSIGN TO                                  IF1164.2
002000     XXXXX055.                                                    IF1164.2
002100 DATA DIVISION.                                                   IF1164.2
002200 FILE SECTION.                                                    IF1164.2
002300 FD  PRINT-FILE.                                                  IF1164.2
002400 01  PRINT-REC PICTURE X(120).                                    IF1164.2
002500 01  DUMMY-RECORD PICTURE X(120).                                 IF1164.2
002600 WORKING-STORAGE SECTION.                                         IF1164.2
002700***********************************************************       IF1164.2
002800* Variables specific to the Intrinsic Function Test IF116A*       IF1164.2
002900***********************************************************       IF1164.2
003000 01  A                   PIC S9(10)          VALUE 600000.        IF1164.2
003100 01  B                   PIC S9(10)          VALUE 7.             IF1164.2
003200 01  C                   PIC S9(10)          VALUE -4.            IF1164.2
003300 01  D                   PIC S9(10)          VALUE 10.            IF1164.2
003400 01  E                   PIC S9(1)V9(9)      VALUE 2.718281828.   IF1164.2
003500 01  F                   PIC S9(5)V9(5)      VALUE 32000.8.       IF1164.2
003600 01  G                   PIC S9(5)V9(5)      VALUE .00002.        IF1164.2
003700 01  H                   PIC S9(5)V9(5)      VALUE -5.3.          IF1164.2
003800 01  ARG1                PIC S9(5)V9(5)      VALUE 1.00.          IF1164.2
003900 01  ARR                                     VALUE "40537".       IF1164.2
004000     02  IND OCCURS 5 TIMES PIC 9.                                IF1164.2
004100 01  TEMP                PIC S9(10).                              IF1164.2
004200 01  WS-NUM              PIC S9(5)V9(6).                          IF1164.2
004300 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1164.2
004400 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1164.2
004500*                                                                 IF1164.2
004600**********************************************************        IF1164.2
004700*                                                                 IF1164.2
004800 01  TEST-RESULTS.                                                IF1164.2
004900     02 FILLER                   PIC X      VALUE SPACE.          IF1164.2
005000     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1164.2
005100     02 FILLER                   PIC X      VALUE SPACE.          IF1164.2
005200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1164.2
005300     02 FILLER                   PIC X      VALUE SPACE.          IF1164.2
005400     02  PAR-NAME.                                                IF1164.2
005500       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1164.2
005600       03  PARDOT-X              PIC X      VALUE SPACE.          IF1164.2
005700       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1164.2
005800     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1164.2
005900     02 RE-MARK                  PIC X(61).                       IF1164.2
006000 01  TEST-COMPUTED.                                               IF1164.2
006100     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1164.2
006200     02 FILLER                   PIC X(17)  VALUE                 IF1164.2
006300            "       COMPUTED=".                                   IF1164.2
006400     02 COMPUTED-X.                                               IF1164.2
006500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1164.2
006600     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1164.2
006700                                 PIC -9(9).9(9).                  IF1164.2
006800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1164.2
006900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1164.2
007000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1164.2
007100     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1164.2
007200         04 COMPUTED-18V0                    PIC -9(18).          IF1164.2
007300         04 FILLER                           PIC X.               IF1164.2
007400     03 FILLER PIC X(50) VALUE SPACE.                             IF1164.2
007500 01  TEST-CORRECT.                                                IF1164.2
007600     02 FILLER PIC X(30) VALUE SPACE.                             IF1164.2
007700     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1164.2
007800     02 CORRECT-X.                                                IF1164.2
007900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1164.2
008000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1164.2
008100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1164.2
008200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1164.2
008300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1164.2
008400     03      CR-18V0 REDEFINES CORRECT-A.                         IF1164.2
008500         04 CORRECT-18V0                     PIC -9(18).          IF1164.2
008600         04 FILLER                           PIC X.               IF1164.2
008700     03 FILLER PIC X(2) VALUE SPACE.                              IF1164.2
008800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1164.2
008900 01  TEST-CORRECT-MIN.                                            IF1164.2
009000     02 FILLER PIC X(30) VALUE SPACE.                             IF1164.2
009100     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1164.2
009200     02 CORRECTMI-X.                                              IF1164.2
009300     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1164.2
009400     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1164.2
009500     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1164.2
009600     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1164.2
009700     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1164.2
009800     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1164.2
009900         04 CORRECTMI-18V0                     PIC -9(18).        IF1164.2
010000         04 FILLER                           PIC X.               IF1164.2
010100     03 FILLER PIC X(2) VALUE SPACE.                              IF1164.2
010200     03 FILLER                           PIC X(48) VALUE SPACE.   IF1164.2
010300 01  TEST-CORRECT-MAX.                                            IF1164.2
010400     02 FILLER PIC X(30) VALUE SPACE.                             IF1164.2
010500     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1164.2
010600     02 CORRECTMA-X.                                              IF1164.2
010700     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1164.2
010800     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1164.2
010900     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1164.2
011000     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1164.2
011100     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1164.2
011200     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1164.2
011300         04 CORRECTMA-18V0                     PIC -9(18).        IF1164.2
011400         04 FILLER                           PIC X.               IF1164.2
011500     03 FILLER PIC X(2) VALUE SPACE.                              IF1164.2
011600     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1164.2
011700 01  CCVS-C-1.                                                    IF1164.2
011800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1164.2
011900-    "SS  PARAGRAPH-NAME                                          IF1164.2
012000-    "       REMARKS".                                            IF1164.2
012100     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1164.2
012200 01  CCVS-C-2.                                                    IF1164.2
012300     02 FILLER                     PIC X        VALUE SPACE.      IF1164.2
012400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1164.2
012500     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1164.2
012600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1164.2
012700     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1164.2
012800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1164.2
012900 01  REC-CT                        PIC 99       VALUE ZERO.       IF1164.2
013000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1164.2
013100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1164.2
013200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1164.2
013300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1164.2
013400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1164.2
013500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1164.2
013600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1164.2
013700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1164.2
013800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1164.2
013900 01  CCVS-H-1.                                                    IF1164.2
014000     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1164.2
014100     02  FILLER                    PIC X(42)    VALUE             IF1164.2
014200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1164.2
014300     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1164.2
014400 01  CCVS-H-2A.                                                   IF1164.2
014500   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1164.2
014600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1164.2
014700   02  FILLER                        PIC XXXX   VALUE             IF1164.2
014800     "4.2 ".                                                      IF1164.2
014900   02  FILLER                        PIC X(28)  VALUE             IF1164.2
015000            " COPY - NOT FOR DISTRIBUTION".                       IF1164.2
015100   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1164.2
015200                                                                  IF1164.2
015300 01  CCVS-H-2B.                                                   IF1164.2
015400   02  FILLER                        PIC X(15)  VALUE             IF1164.2
015500            "TEST RESULT OF ".                                    IF1164.2
015600   02  TEST-ID                       PIC X(9).                    IF1164.2
015700   02  FILLER                        PIC X(4)   VALUE             IF1164.2
015800            " IN ".                                               IF1164.2
015900   02  FILLER                        PIC X(12)  VALUE             IF1164.2
016000     " HIGH       ".                                              IF1164.2
016100   02  FILLER                        PIC X(22)  VALUE             IF1164.2
016200            " LEVEL VALIDATION FOR ".                             IF1164.2
016300   02  FILLER                        PIC X(58)  VALUE             IF1164.2
016400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1164.2
016500 01  CCVS-H-3.                                                    IF1164.2
016600     02  FILLER                      PIC X(34)  VALUE             IF1164.2
016700            " FOR OFFICIAL USE ONLY    ".                         IF1164.2
016800     02  FILLER                      PIC X(58)  VALUE             IF1164.2
016900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1164.2
017000     02  FILLER                      PIC X(28)  VALUE             IF1164.2
017100            "  COPYRIGHT   1985 ".                                IF1164.2
017200 01  CCVS-E-1.                                                    IF1164.2
017300     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1164.2
017400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1164.2
017500     02 ID-AGAIN                     PIC X(9).                    IF1164.2
017600     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1164.2
017700 01  CCVS-E-2.                                                    IF1164.2
017800     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1164.2
017900     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1164.2
018000     02 CCVS-E-2-2.                                               IF1164.2
018100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1164.2
018200         03 FILLER                   PIC X      VALUE SPACE.      IF1164.2
018300         03 ENDER-DESC               PIC X(44)  VALUE             IF1164.2
018400            "ERRORS ENCOUNTERED".                                 IF1164.2
018500 01  CCVS-E-3.                                                    IF1164.2
018600     02  FILLER                      PIC X(22)  VALUE             IF1164.2
018700            " FOR OFFICIAL USE ONLY".                             IF1164.2
018800     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1164.2
018900     02  FILLER                      PIC X(58)  VALUE             IF1164.2
019000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1164.2
019100     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1164.2
019200     02 FILLER                       PIC X(15)  VALUE             IF1164.2
019300             " COPYRIGHT 1985".                                   IF1164.2
019400 01  CCVS-E-4.                                                    IF1164.2
019500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1164.2
019600     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1164.2
019700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1164.2
019800     02 FILLER                       PIC X(40)  VALUE             IF1164.2
019900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1164.2
020000 01  XXINFO.                                                      IF1164.2
020100     02 FILLER                       PIC X(19)  VALUE             IF1164.2
020200            "*** INFORMATION ***".                                IF1164.2
020300     02 INFO-TEXT.                                                IF1164.2
020400       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1164.2
020500       04 XXCOMPUTED                 PIC X(20).                   IF1164.2
020600       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1164.2
020700       04 XXCORRECT                  PIC X(20).                   IF1164.2
020800     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1164.2
020900 01  HYPHEN-LINE.                                                 IF1164.2
021000     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1164.2
021100     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1164.2
021200-    "*****************************************".                 IF1164.2
021300     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1164.2
021400-    "******************************".                            IF1164.2
021500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1164.2
021600     "IF116A".                                                    IF1164.2
021700 PROCEDURE DIVISION.                                              IF1164.2
021800 CCVS1 SECTION.                                                   IF1164.2
021900 OPEN-FILES.                                                      IF1164.2
022000     OPEN     OUTPUT PRINT-FILE.                                  IF1164.2
022100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1164.2
022200     MOVE    SPACE TO TEST-RESULTS.                               IF1164.2
022300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1164.2
022400     GO TO CCVS1-EXIT.                                            IF1164.2
022500 CLOSE-FILES.                                                     IF1164.2
022600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1164.2
022700 TERMINATE-CCVS.                                                  IF1164.2
022800     STOP     RUN.                                                IF1164.2
022900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1164.2
023000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1164.2
023100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1164.2
023200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1164.2
023300     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1164.2
023400 PRINT-DETAIL.                                                    IF1164.2
023500     IF REC-CT NOT EQUAL TO ZERO                                  IF1164.2
023600             MOVE "." TO PARDOT-X                                 IF1164.2
023700             MOVE REC-CT TO DOTVALUE.                             IF1164.2
023800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1164.2
023900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1164.2
024000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1164.2
024100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1164.2
024200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1164.2
024300     MOVE SPACE TO CORRECT-X.                                     IF1164.2
024400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1164.2
024500     MOVE     SPACE TO RE-MARK.                                   IF1164.2
024600 HEAD-ROUTINE.                                                    IF1164.2
024700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1164.2
024800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1164.2
024900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1164.2
025000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1164.2
025100 COLUMN-NAMES-ROUTINE.                                            IF1164.2
025200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1164.2
025300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1164.2
025400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1164.2
025500 END-ROUTINE.                                                     IF1164.2
025600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1164.2
025700 END-RTN-EXIT.                                                    IF1164.2
025800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1164.2
025900 END-ROUTINE-1.                                                   IF1164.2
026000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1164.2
026100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1164.2
026200      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1164.2
026300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1164.2
026400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1164.2
026500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1164.2
026600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1164.2
026700  END-ROUTINE-12.                                                 IF1164.2
026800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1164.2
026900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1164.2
027000         MOVE "NO " TO ERROR-TOTAL                                IF1164.2
027100         ELSE                                                     IF1164.2
027200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1164.2
027300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1164.2
027400     PERFORM WRITE-LINE.                                          IF1164.2
027500 END-ROUTINE-13.                                                  IF1164.2
027600     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1164.2
027700         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1164.2
027800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1164.2
027900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1164.2
028000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1164.2
028100      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1164.2
028200          MOVE "NO " TO ERROR-TOTAL                               IF1164.2
028300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1164.2
028400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1164.2
028500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1164.2
028600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1164.2
028700 WRITE-LINE.                                                      IF1164.2
028800     ADD 1 TO RECORD-COUNT.                                       IF1164.2
028900*    IF RECORD-COUNT GREATER 42                                   IF1164.2
029000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1164.2
029100*        MOVE SPACE TO DUMMY-RECORD                               IF1164.2
029200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1164.2
029300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1164.2
029400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1164.2
029500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1164.2
029600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1164.2
029700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1164.2
029800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1164.2
029900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1164.2
030000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1164.2
030100*        MOVE ZERO TO RECORD-COUNT.                               IF1164.2
030200     PERFORM WRT-LN.                                              IF1164.2
030300 WRT-LN.                                                          IF1164.2
030400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1164.2
030500     MOVE SPACE TO DUMMY-RECORD.                                  IF1164.2
030600 BLANK-LINE-PRINT.                                                IF1164.2
030700     PERFORM WRT-LN.                                              IF1164.2
030800 FAIL-ROUTINE.                                                    IF1164.2
030900     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1164.2
031000            GO TO FAIL-ROUTINE-WRITE.                             IF1164.2
031100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1164.2
031200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1164.2
031300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1164.2
031400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1164.2
031500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1164.2
031600     GO TO  FAIL-ROUTINE-EX.                                      IF1164.2
031700 FAIL-ROUTINE-WRITE.                                              IF1164.2
031800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1164.2
031900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1164.2
032000                              CORMA-ANSI-REFERENCE.               IF1164.2
032100     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1164.2
032200           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1164.2
032300           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1164.2
032400     ELSE                                                         IF1164.2
032500           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1164.2
032600     PERFORM WRITE-LINE.                                          IF1164.2
032700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1164.2
032800 FAIL-ROUTINE-EX. EXIT.                                           IF1164.2
032900 BAIL-OUT.                                                        IF1164.2
033000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1164.2
033100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1164.2
033200 BAIL-OUT-WRITE.                                                  IF1164.2
033300     MOVE CORRECT-A TO XXCORRECT.                                 IF1164.2
033400     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1164.2
033500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1164.2
033600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1164.2
033700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1164.2
033800 BAIL-OUT-EX. EXIT.                                               IF1164.2
033900 CCVS1-EXIT.                                                      IF1164.2
034000     EXIT.                                                        IF1164.2
034100********************************************************          IF1164.2
034200*                                                      *          IF1164.2
034300*    Intrinsic Function Tests         IF116A - LOG     *          IF1164.2
034400*                                                      *          IF1164.2
034500********************************************************          IF1164.2
034600 SECT-IF116A SECTION.                                             IF1164.2
034700 F-LOG-INFO.                                                      IF1164.2
034800     MOVE     "See ref. A-49 2.20" TO ANSI-REFERENCE.             IF1164.2
034900     MOVE     "LOG Function" TO FEATURE.                          IF1164.2
035000*****************TEST (a) - SIMPLE TEST*****************          IF1164.2
035100 F-LOG-01.                                                        IF1164.2
035200     MOVE ZERO TO WS-NUM.                                         IF1164.2
035300     MOVE  0.999980 TO MIN-RANGE.                                 IF1164.2
035400     MOVE  1.00002 TO MAX-RANGE.                                  IF1164.2
035500 F-LOG-TEST-01.                                                   IF1164.2
035600     COMPUTE WS-NUM = FUNCTION LOG(E).                            IF1164.2
035700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
035800        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
035900                    PERFORM PASS                                  IF1164.2
036000     ELSE                                                         IF1164.2
036100                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
036200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
036300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
036400                    PERFORM FAIL.                                 IF1164.2
036500     GO TO F-LOG-WRITE-01.                                        IF1164.2
036600 F-LOG-DELETE-01.                                                 IF1164.2
036700     PERFORM  DE-LETE.                                            IF1164.2
036800     GO TO    F-LOG-WRITE-01.                                     IF1164.2
036900 F-LOG-WRITE-01.                                                  IF1164.2
037000     MOVE "F-LOG-01" TO PAR-NAME.                                 IF1164.2
037100     PERFORM  PRINT-DETAIL.                                       IF1164.2
037200*****************TEST (b) - SIMPLE TEST*****************          IF1164.2
037300 F-LOG-02.                                                        IF1164.2
037400     EVALUATE FUNCTION LOG(1)                                     IF1164.2
037500     WHEN -0.000020 THRU 0.000020                                 IF1164.2
037600                 PERFORM PASS                                     IF1164.2
037700     WHEN OTHER                                                   IF1164.2
037800                 PERFORM FAIL.                                    IF1164.2
037900     GO TO F-LOG-WRITE-02.                                        IF1164.2
038000 F-LOG-DELETE-02.                                                 IF1164.2
038100     PERFORM  DE-LETE.                                            IF1164.2
038200     GO TO    F-LOG-WRITE-02.                                     IF1164.2
038300 F-LOG-WRITE-02.                                                  IF1164.2
038400     MOVE "F-LOG-02" TO PAR-NAME.                                 IF1164.2
038500     PERFORM  PRINT-DETAIL.                                       IF1164.2
038600*****************TEST (d) - SIMPLE TEST*****************          IF1164.2
038700 F-LOG-04.                                                        IF1164.2
038800     MOVE ZERO TO WS-NUM.                                         IF1164.2
038900     MOVE -6.90789  TO MIN-RANGE.                                 IF1164.2
039000     MOVE -6.90761  TO MAX-RANGE.                                 IF1164.2
039100 F-LOG-TEST-04.                                                   IF1164.2
039200     COMPUTE WS-NUM = FUNCTION LOG(.001).                         IF1164.2
039300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
039400        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
039500                    PERFORM PASS                                  IF1164.2
039600     ELSE                                                         IF1164.2
039700                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
039800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
039900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
040000                    PERFORM FAIL.                                 IF1164.2
040100     GO TO F-LOG-WRITE-04.                                        IF1164.2
040200 F-LOG-DELETE-04.                                                 IF1164.2
040300     PERFORM  DE-LETE.                                            IF1164.2
040400     GO TO    F-LOG-WRITE-04.                                     IF1164.2
040500 F-LOG-WRITE-04.                                                  IF1164.2
040600     MOVE "F-LOG-04" TO PAR-NAME.                                 IF1164.2
040700     PERFORM  PRINT-DETAIL.                                       IF1164.2
040800*****************TEST (e) - SIMPLE TEST*****************          IF1164.2
040900 F-LOG-05.                                                        IF1164.2
041000     MOVE ZERO TO WS-NUM.                                         IF1164.2
041100     MOVE  9.21015 TO MIN-RANGE.                                  IF1164.2
041200     MOVE  9.21524 TO MAX-RANGE.                                  IF1164.2
041300 F-LOG-TEST-05.                                                   IF1164.2
041400     COMPUTE WS-NUM = FUNCTION LOG(10000).                        IF1164.2
041500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
041600        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
041700                    PERFORM PASS                                  IF1164.2
041800     ELSE                                                         IF1164.2
041900                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
042000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
042100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
042200                    PERFORM FAIL.                                 IF1164.2
042300     GO TO F-LOG-WRITE-05.                                        IF1164.2
042400 F-LOG-DELETE-05.                                                 IF1164.2
042500     PERFORM  DE-LETE.                                            IF1164.2
042600     GO TO    F-LOG-WRITE-05.                                     IF1164.2
042700 F-LOG-WRITE-05.                                                  IF1164.2
042800     MOVE "F-LOG-05" TO PAR-NAME.                                 IF1164.2
042900     PERFORM  PRINT-DETAIL.                                       IF1164.2
043000*****************TEST (f) - SIMPLE TEST*****************          IF1164.2
043100 F-LOG-06.                                                        IF1164.2
043200     MOVE ZERO TO WS-NUM.                                         IF1164.2
043300     MOVE  8.01598 TO MIN-RANGE.                                  IF1164.2
043400     MOVE  8.01630 TO MAX-RANGE.                                  IF1164.2
043500 F-LOG-TEST-06.                                                   IF1164.2
043600     COMPUTE WS-NUM = FUNCTION LOG(3029.48).                      IF1164.2
043700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
043800        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
043900                    PERFORM PASS                                  IF1164.2
044000     ELSE                                                         IF1164.2
044100                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
044200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
044300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
044400                    PERFORM FAIL.                                 IF1164.2
044500     GO TO F-LOG-WRITE-06.                                        IF1164.2
044600 F-LOG-DELETE-06.                                                 IF1164.2
044700     PERFORM  DE-LETE.                                            IF1164.2
044800     GO TO    F-LOG-WRITE-06.                                     IF1164.2
044900 F-LOG-WRITE-06.                                                  IF1164.2
045000     MOVE "F-LOG-06" TO PAR-NAME.                                 IF1164.2
045100     PERFORM  PRINT-DETAIL.                                       IF1164.2
045200*****************TEST (g) - SIMPLE TEST*****************          IF1164.2
045300 F-LOG-07.                                                        IF1164.2
045400     MOVE ZERO TO WS-NUM.                                         IF1164.2
045500     MOVE -9.90368 TO MIN-RANGE.                                  IF1164.2
045600     MOVE -9.90328 TO MAX-RANGE.                                  IF1164.2
045700 F-LOG-TEST-07.                                                   IF1164.2
045800     COMPUTE WS-NUM = FUNCTION LOG(.00005).                       IF1164.2
045900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
046000        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
046100                    PERFORM PASS                                  IF1164.2
046200     ELSE                                                         IF1164.2
046300                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
046400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
046500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
046600                    PERFORM FAIL.                                 IF1164.2
046700     GO TO F-LOG-WRITE-07.                                        IF1164.2
046800 F-LOG-DELETE-07.                                                 IF1164.2
046900     PERFORM  DE-LETE.                                            IF1164.2
047000     GO TO    F-LOG-WRITE-07.                                     IF1164.2
047100 F-LOG-WRITE-07.                                                  IF1164.2
047200     MOVE "F-LOG-07" TO PAR-NAME.                                 IF1164.2
047300     PERFORM  PRINT-DETAIL.                                       IF1164.2
047400*****************TEST (h) - SIMPLE TEST*****************          IF1164.2
047500 F-LOG-08.                                                        IF1164.2
047600     MOVE ZERO TO WS-NUM.                                         IF1164.2
047700     MOVE  13.3044 TO MIN-RANGE.                                  IF1164.2
047800     MOVE  13.3050 TO MAX-RANGE.                                  IF1164.2
047900 F-LOG-TEST-08.                                                   IF1164.2
048000     COMPUTE WS-NUM = FUNCTION LOG(A).                            IF1164.2
048100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
048200        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
048300                    PERFORM PASS                                  IF1164.2
048400     ELSE                                                         IF1164.2
048500                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
048600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
048700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
048800                    PERFORM FAIL.                                 IF1164.2
048900     GO TO F-LOG-WRITE-08.                                        IF1164.2
049000 F-LOG-DELETE-08.                                                 IF1164.2
049100     PERFORM  DE-LETE.                                            IF1164.2
049200     GO TO    F-LOG-WRITE-08.                                     IF1164.2
049300 F-LOG-WRITE-08.                                                  IF1164.2
049400     MOVE "F-LOG-08" TO PAR-NAME.                                 IF1164.2
049500     PERFORM  PRINT-DETAIL.                                       IF1164.2
049600*****************TEST (i) - SIMPLE TEST*****************          IF1164.2
049700 F-LOG-09.                                                        IF1164.2
049800     MOVE ZERO TO WS-NUM.                                         IF1164.2
049900     MOVE  10.3733 TO MIN-RANGE.                                  IF1164.2
050000     MOVE  10.3737 TO MAX-RANGE.                                  IF1164.2
050100 F-LOG-TEST-09.                                                   IF1164.2
050200     COMPUTE WS-NUM = FUNCTION LOG(F).                            IF1164.2
050300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
050400        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
050500                    PERFORM PASS                                  IF1164.2
050600     ELSE                                                         IF1164.2
050700                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
050800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
050900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
051000                    PERFORM FAIL.                                 IF1164.2
051100     GO TO F-LOG-WRITE-09.                                        IF1164.2
051200 F-LOG-DELETE-09.                                                 IF1164.2
051300     PERFORM  DE-LETE.                                            IF1164.2
051400     GO TO    F-LOG-WRITE-09.                                     IF1164.2
051500 F-LOG-WRITE-09.                                                  IF1164.2
051600     MOVE "F-LOG-09" TO PAR-NAME.                                 IF1164.2
051700     PERFORM  PRINT-DETAIL.                                       IF1164.2
051800*****************TEST (j) - SIMPLE TEST*****************          IF1164.2
051900 F-LOG-10.                                                        IF1164.2
052000     MOVE ZERO TO WS-NUM.                                         IF1164.2
052100     MOVE -10.8199 TO MIN-RANGE.                                  IF1164.2
052200     MOVE -10.8195 TO MAX-RANGE.                                  IF1164.2
052300 F-LOG-TEST-10.                                                   IF1164.2
052400     COMPUTE WS-NUM = FUNCTION LOG(G).                            IF1164.2
052500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
052600        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
052700                    PERFORM PASS                                  IF1164.2
052800     ELSE                                                         IF1164.2
052900                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
053000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
053100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
053200                    PERFORM FAIL.                                 IF1164.2
053300     GO TO F-LOG-WRITE-10.                                        IF1164.2
053400 F-LOG-DELETE-10.                                                 IF1164.2
053500     PERFORM  DE-LETE.                                            IF1164.2
053600     GO TO    F-LOG-WRITE-10.                                     IF1164.2
053700 F-LOG-WRITE-10.                                                  IF1164.2
053800     MOVE "F-LOG-10" TO PAR-NAME.                                 IF1164.2
053900     PERFORM  PRINT-DETAIL.                                       IF1164.2
054000*****************TEST (k) - SIMPLE TEST*****************          IF1164.2
054100 F-LOG-11.                                                        IF1164.2
054200     MOVE ZERO TO WS-NUM.                                         IF1164.2
054300     MOVE  1.09859 TO MIN-RANGE.                                  IF1164.2
054400     MOVE  1.09863 TO MAX-RANGE.                                  IF1164.2
054500 F-LOG-TEST-11.                                                   IF1164.2
054600     COMPUTE WS-NUM = FUNCTION LOG(IND(4)).                       IF1164.2
054700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
054800        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
054900                    PERFORM PASS                                  IF1164.2
055000     ELSE                                                         IF1164.2
055100                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
055200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
055300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
055400                    PERFORM FAIL.                                 IF1164.2
055500     GO TO F-LOG-WRITE-11.                                        IF1164.2
055600 F-LOG-DELETE-11.                                                 IF1164.2
055700     PERFORM  DE-LETE.                                            IF1164.2
055800     GO TO    F-LOG-WRITE-11.                                     IF1164.2
055900 F-LOG-WRITE-11.                                                  IF1164.2
056000     MOVE "F-LOG-11" TO PAR-NAME.                                 IF1164.2
056100     PERFORM  PRINT-DETAIL.                                       IF1164.2
056200*****************TEST (a) - COMPLEX TEST****************          IF1164.2
056300 F-LOG-12.                                                        IF1164.2
056400     MOVE ZERO TO WS-NUM.                                         IF1164.2
056500     MOVE  1.00032 TO MIN-RANGE.                                  IF1164.2
056600     MOVE  1.00040 TO MAX-RANGE.                                  IF1164.2
056700 F-LOG-TEST-12.                                                   IF1164.2
056800     COMPUTE WS-NUM = FUNCTION LOG(E + .001).                     IF1164.2
056900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
057000        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
057100                    PERFORM PASS                                  IF1164.2
057200     ELSE                                                         IF1164.2
057300                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
057400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
057500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
057600                    PERFORM FAIL.                                 IF1164.2
057700     GO TO F-LOG-WRITE-12.                                        IF1164.2
057800 F-LOG-DELETE-12.                                                 IF1164.2
057900     PERFORM  DE-LETE.                                            IF1164.2
058000     GO TO    F-LOG-WRITE-12.                                     IF1164.2
058100 F-LOG-WRITE-12.                                                  IF1164.2
058200     MOVE "F-LOG-12" TO PAR-NAME.                                 IF1164.2
058300     PERFORM  PRINT-DETAIL.                                       IF1164.2
058400*****************TEST (b) - COMPLEX TEST****************          IF1164.2
058500 F-LOG-13.                                                        IF1164.2
058600     MOVE ZERO TO WS-NUM.                                         IF1164.2
058700     MOVE -2.30267 TO MIN-RANGE.                                  IF1164.2
058800     MOVE -2.30249 TO MAX-RANGE.                                  IF1164.2
058900 F-LOG-TEST-13.                                                   IF1164.2
059000     COMPUTE WS-NUM = FUNCTION LOG(1 / 10).                       IF1164.2
059100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
059200        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
059300                    PERFORM PASS                                  IF1164.2
059400     ELSE                                                         IF1164.2
059500                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
059600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
059700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
059800                    PERFORM FAIL.                                 IF1164.2
059900     GO TO F-LOG-WRITE-13.                                        IF1164.2
060000 F-LOG-DELETE-13.                                                 IF1164.2
060100     PERFORM  DE-LETE.                                            IF1164.2
060200     GO TO    F-LOG-WRITE-13.                                     IF1164.2
060300 F-LOG-WRITE-13.                                                  IF1164.2
060400     MOVE "F-LOG-13" TO PAR-NAME.                                 IF1164.2
060500     PERFORM  PRINT-DETAIL.                                       IF1164.2
060600*****************TEST (c) - COMPLEX TEST****************          IF1164.2
060700 F-LOG-14.                                                        IF1164.2
060800     MOVE ZERO TO WS-NUM.                                         IF1164.2
060900     MOVE  0.962479 TO MIN-RANGE.                                 IF1164.2
061000     MOVE  0.962556 TO MAX-RANGE.                                 IF1164.2
061100 F-LOG-TEST-14.                                                   IF1164.2
061200     COMPUTE WS-NUM = FUNCTION LOG(E - .1).                       IF1164.2
061300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
061400        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
061500                    PERFORM PASS                                  IF1164.2
061600     ELSE                                                         IF1164.2
061700                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
061800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
061900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
062000                    PERFORM FAIL.                                 IF1164.2
062100     GO TO F-LOG-WRITE-14.                                        IF1164.2
062200 F-LOG-DELETE-14.                                                 IF1164.2
062300     PERFORM  DE-LETE.                                            IF1164.2
062400     GO TO    F-LOG-WRITE-14.                                     IF1164.2
062500 F-LOG-WRITE-14.                                                  IF1164.2
062600     MOVE "F-LOG-14" TO PAR-NAME.                                 IF1164.2
062700     PERFORM  PRINT-DETAIL.                                       IF1164.2
062800*****************TEST (d) - COMPLEX TEST****************          IF1164.2
062900 F-LOG-15.                                                        IF1164.2
063000     MOVE ZERO TO WS-NUM.                                         IF1164.2
063100     MOVE -0.105364 TO MIN-RANGE.                                 IF1164.2
063200     MOVE -0.105356 TO MAX-RANGE.                                 IF1164.2
063300 F-LOG-TEST-15.                                                   IF1164.2
063400     COMPUTE WS-NUM = FUNCTION LOG(1 - .1).                       IF1164.2
063500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
063600        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
063700                    PERFORM PASS                                  IF1164.2
063800     ELSE                                                         IF1164.2
063900                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
064000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
064100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
064200                    PERFORM FAIL.                                 IF1164.2
064300     GO TO F-LOG-WRITE-15.                                        IF1164.2
064400 F-LOG-DELETE-15.                                                 IF1164.2
064500     PERFORM  DE-LETE.                                            IF1164.2
064600     GO TO    F-LOG-WRITE-15.                                     IF1164.2
064700 F-LOG-WRITE-15.                                                  IF1164.2
064800     MOVE "F-LOG-15" TO PAR-NAME.                                 IF1164.2
064900     PERFORM  PRINT-DETAIL.                                       IF1164.2
065000*****************TEST (e) - COMPLEX TEST****************          IF1164.2
065100 F-LOG-16.                                                        IF1164.2
065200     MOVE ZERO TO WS-NUM.                                         IF1164.2
065300     MOVE  1.94583 TO MIN-RANGE.                                  IF1164.2
065400     MOVE  1.94599 TO MAX-RANGE.                                  IF1164.2
065500 F-LOG-TEST-16.                                                   IF1164.2
065600     COMPUTE WS-NUM = FUNCTION LOG(IND(D - 5)).                   IF1164.2
065700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
065800        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
065900                    PERFORM PASS                                  IF1164.2
066000     ELSE                                                         IF1164.2
066100                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
066200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
066300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
066400                    PERFORM FAIL.                                 IF1164.2
066500     GO TO F-LOG-WRITE-16.                                        IF1164.2
066600 F-LOG-DELETE-16.                                                 IF1164.2
066700     PERFORM  DE-LETE.                                            IF1164.2
066800     GO TO    F-LOG-WRITE-16.                                     IF1164.2
066900 F-LOG-WRITE-16.                                                  IF1164.2
067000     MOVE "F-LOG-16" TO PAR-NAME.                                 IF1164.2
067100     PERFORM  PRINT-DETAIL.                                       IF1164.2
067200*****************TEST (f) - COMPLEX TEST****************          IF1164.2
067300 F-LOG-17.                                                        IF1164.2
067400     MOVE ZERO TO WS-NUM.                                         IF1164.2
067500     MOVE  2.99561 TO MIN-RANGE.                                  IF1164.2
067600     MOVE  2.99585 TO MAX-RANGE.                                  IF1164.2
067700 F-LOG-TEST-17.                                                   IF1164.2
067800     COMPUTE WS-NUM = FUNCTION LOG(2 * 10).                       IF1164.2
067900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
068000        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
068100                    PERFORM PASS                                  IF1164.2
068200     ELSE                                                         IF1164.2
068300                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
068400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
068500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
068600                    PERFORM FAIL.                                 IF1164.2
068700     GO TO F-LOG-WRITE-17.                                        IF1164.2
068800 F-LOG-DELETE-17.                                                 IF1164.2
068900     PERFORM  DE-LETE.                                            IF1164.2
069000     GO TO    F-LOG-WRITE-17.                                     IF1164.2
069100 F-LOG-WRITE-17.                                                  IF1164.2
069200     MOVE "F-LOG-17" TO PAR-NAME.                                 IF1164.2
069300     PERFORM  PRINT-DETAIL.                                       IF1164.2
069400*****************TEST (g) - COMPLEX TEST****************          IF1164.2
069500 F-LOG-18.                                                        IF1164.2
069600     MOVE ZERO TO WS-NUM.                                         IF1164.2
069700     MOVE  1.09857 TO MIN-RANGE.                                  IF1164.2
069800     MOVE  1.09865 TO MAX-RANGE.                                  IF1164.2
069900 F-LOG-TEST-18.                                                   IF1164.2
070000     COMPUTE WS-NUM = FUNCTION LOG(B + C).                        IF1164.2
070100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
070200        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
070300                    PERFORM PASS                                  IF1164.2
070400     ELSE                                                         IF1164.2
070500                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
070600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
070700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
070800                    PERFORM FAIL.                                 IF1164.2
070900     GO TO F-LOG-WRITE-18.                                        IF1164.2
071000 F-LOG-DELETE-18.                                                 IF1164.2
071100     PERFORM  DE-LETE.                                            IF1164.2
071200     GO TO    F-LOG-WRITE-18.                                     IF1164.2
071300 F-LOG-WRITE-18.                                                  IF1164.2
071400     MOVE "F-LOG-18" TO PAR-NAME.                                 IF1164.2
071500     PERFORM  PRINT-DETAIL.                                       IF1164.2
071600*****************TEST (h) - COMPLEX TEST****************          IF1164.2
071700 F-LOG-19.                                                        IF1164.2
071800     MOVE ZERO TO WS-NUM.                                         IF1164.2
071900     MOVE  0.632497 TO MIN-RANGE.                                 IF1164.2
072000     MOVE  0.632547 TO MAX-RANGE.                                 IF1164.2
072100 F-LOG-TEST-19.                                                   IF1164.2
072200     COMPUTE WS-NUM = FUNCTION LOG(3.2 / 1.7).                    IF1164.2
072300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
072400        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
072500                    PERFORM PASS                                  IF1164.2
072600     ELSE                                                         IF1164.2
072700                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
072800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
072900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
073000                    PERFORM FAIL.                                 IF1164.2
073100     GO TO F-LOG-WRITE-19.                                        IF1164.2
073200 F-LOG-DELETE-19.                                                 IF1164.2
073300     PERFORM  DE-LETE.                                            IF1164.2
073400     GO TO    F-LOG-WRITE-19.                                     IF1164.2
073500 F-LOG-WRITE-19.                                                  IF1164.2
073600     MOVE "F-LOG-19" TO PAR-NAME.                                 IF1164.2
073700     PERFORM  PRINT-DETAIL.                                       IF1164.2
073800*****************TEST (i) - COMPLEX TEST****************          IF1164.2
073900 F-LOG-20.                                                        IF1164.2
074000     MOVE ZERO TO WS-NUM.                                         IF1164.2
074100     MOVE  2.08164 TO MIN-RANGE.                                  IF1164.2
074200     MOVE  2.08180 TO MAX-RANGE.                                  IF1164.2
074300 F-LOG-TEST-20.                                                   IF1164.2
074400     COMPUTE WS-NUM = FUNCTION LOG(E - H).                        IF1164.2
074500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
074600        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
074700                    PERFORM PASS                                  IF1164.2
074800     ELSE                                                         IF1164.2
074900                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
075000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
075100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
075200                    PERFORM FAIL.                                 IF1164.2
075300     GO TO F-LOG-WRITE-20.                                        IF1164.2
075400 F-LOG-DELETE-20.                                                 IF1164.2
075500     PERFORM  DE-LETE.                                            IF1164.2
075600     GO TO    F-LOG-WRITE-20.                                     IF1164.2
075700 F-LOG-WRITE-20.                                                  IF1164.2
075800     MOVE "F-LOG-20" TO PAR-NAME.                                 IF1164.2
075900     PERFORM  PRINT-DETAIL.                                       IF1164.2
076000*****************TEST (j) - COMPLEX TEST****************          IF1164.2
076100 F-LOG-21.                                                        IF1164.2
076200     MOVE ZERO TO WS-NUM.                                         IF1164.2
076300     MOVE  1.60937 TO MIN-RANGE.                                  IF1164.2
076400     MOVE  1.60949 TO MAX-RANGE.                                  IF1164.2
076500 F-LOG-TEST-21.                                                   IF1164.2
076600     COMPUTE WS-NUM = FUNCTION LOG(B - 2).                        IF1164.2
076700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
076800        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
076900                    PERFORM PASS                                  IF1164.2
077000     ELSE                                                         IF1164.2
077100                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
077200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
077300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
077400                    PERFORM FAIL.                                 IF1164.2
077500     GO TO F-LOG-WRITE-21.                                        IF1164.2
077600 F-LOG-DELETE-21.                                                 IF1164.2
077700     PERFORM  DE-LETE.                                            IF1164.2
077800     GO TO    F-LOG-WRITE-21.                                     IF1164.2
077900 F-LOG-WRITE-21.                                                  IF1164.2
078000     MOVE "F-LOG-21" TO PAR-NAME.                                 IF1164.2
078100     PERFORM  PRINT-DETAIL.                                       IF1164.2
078200*****************TEST (k) - COMPLEX TEST****************          IF1164.2
078300 F-LOG-22.                                                        IF1164.2
078400     MOVE ZERO TO WS-NUM.                                         IF1164.2
078500     MOVE  1.48569 TO MIN-RANGE.                                  IF1164.2
078600     MOVE  1.48581 TO MAX-RANGE.                                  IF1164.2
078700 F-LOG-TEST-22.                                                   IF1164.2
078800     COMPUTE WS-NUM = FUNCTION LOG(E + 1.7).                      IF1164.2
078900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
079000        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
079100                    PERFORM PASS                                  IF1164.2
079200     ELSE                                                         IF1164.2
079300                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
079400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
079500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
079600                    PERFORM FAIL.                                 IF1164.2
079700     GO TO F-LOG-WRITE-22.                                        IF1164.2
079800 F-LOG-DELETE-22.                                                 IF1164.2
079900     PERFORM  DE-LETE.                                            IF1164.2
080000     GO TO    F-LOG-WRITE-22.                                     IF1164.2
080100 F-LOG-WRITE-22.                                                  IF1164.2
080200     MOVE "F-LOG-22" TO PAR-NAME.                                 IF1164.2
080300     PERFORM  PRINT-DETAIL.                                       IF1164.2
080400*****************TEST (l) - COMPLEX TEST****************          IF1164.2
080500 F-LOG-23.                                                        IF1164.2
080600     MOVE ZERO TO WS-NUM.                                         IF1164.2
080700     MOVE 4.99980 TO MIN-RANGE.                                   IF1164.2
080800     MOVE 5.00002 TO MAX-RANGE.                                   IF1164.2
080900 F-LOG-TEST-23.                                                   IF1164.2
081000     COMPUTE WS-NUM = FUNCTION LOG(E) + 4.                        IF1164.2
081100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
081200        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
081300                    PERFORM PASS                                  IF1164.2
081400     ELSE                                                         IF1164.2
081500                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
081600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
081700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
081800                    PERFORM FAIL.                                 IF1164.2
081900     GO TO F-LOG-WRITE-23.                                        IF1164.2
082000 F-LOG-DELETE-23.                                                 IF1164.2
082100     PERFORM  DE-LETE.                                            IF1164.2
082200     GO TO    F-LOG-WRITE-23.                                     IF1164.2
082300 F-LOG-WRITE-23.                                                  IF1164.2
082400     MOVE "F-LOG-23" TO PAR-NAME.                                 IF1164.2
082500     PERFORM  PRINT-DETAIL.                                       IF1164.2
082600*****************TEST (m) - COMPLEX TEST****************          IF1164.2
082700 F-LOG-24.                                                        IF1164.2
082800     MOVE ZERO TO WS-NUM.                                         IF1164.2
082900     MOVE 0.665702 TO MIN-RANGE.                                  IF1164.2
083000     MOVE 0.665756 TO MAX-RANGE.                                  IF1164.2
083100 F-LOG-TEST-24.                                                   IF1164.2
083200     COMPUTE WS-NUM = FUNCTION LOG(FUNCTION LOG(B)).              IF1164.2
083300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
083400        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
083500                    PERFORM PASS                                  IF1164.2
083600     ELSE                                                         IF1164.2
083700                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
083800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
083900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
084000                    PERFORM FAIL.                                 IF1164.2
084100     GO TO F-LOG-WRITE-24.                                        IF1164.2
084200 F-LOG-DELETE-24.                                                 IF1164.2
084300     PERFORM  DE-LETE.                                            IF1164.2
084400     GO TO    F-LOG-WRITE-24.                                     IF1164.2
084500 F-LOG-WRITE-24.                                                  IF1164.2
084600     MOVE "F-LOG-24" TO PAR-NAME.                                 IF1164.2
084700     PERFORM  PRINT-DETAIL.                                       IF1164.2
084800*****************TEST (n) - COMPLEX TEST****************          IF1164.2
084900 F-LOG-25.                                                        IF1164.2
085000     MOVE ZERO TO WS-NUM.                                         IF1164.2
085100     MOVE 1.69307 TO MIN-RANGE.                                   IF1164.2
085200     MOVE 1.69321 TO MAX-RANGE.                                   IF1164.2
085300 F-LOG-TEST-25.                                                   IF1164.2
085400     COMPUTE WS-NUM = FUNCTION LOG(E) +                           IF1164.2
085500                      FUNCTION LOG(2).                            IF1164.2
085600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1164.2
085700        (WS-NUM <= MAX-RANGE) THEN                                IF1164.2
085800                    PERFORM PASS                                  IF1164.2
085900     ELSE                                                         IF1164.2
086000                    MOVE WS-NUM TO COMPUTED-N                     IF1164.2
086100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1164.2
086200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1164.2
086300                    PERFORM FAIL.                                 IF1164.2
086400     GO TO F-LOG-WRITE-25.                                        IF1164.2
086500 F-LOG-DELETE-25.                                                 IF1164.2
086600     PERFORM  DE-LETE.                                            IF1164.2
086700     GO TO    F-LOG-WRITE-25.                                     IF1164.2
086800 F-LOG-WRITE-25.                                                  IF1164.2
086900     MOVE "F-LOG-25" TO PAR-NAME.                                 IF1164.2
087000     PERFORM  PRINT-DETAIL.                                       IF1164.2
087100*****************SPECIAL PERFORM TEST**********************       IF1164.2
087200 F-LOG-26.                                                        IF1164.2
087300     MOVE ZERO TO WS-NUM.                                         IF1164.2
087400     PERFORM F-LOG-TEST-26                                        IF1164.2
087500       UNTIL FUNCTION LOG(ARG1) > 1.                              IF1164.2
087600     PERFORM PASS.                                                IF1164.2
087700     GO TO F-LOG-WRITE-26.                                        IF1164.2
087800 F-LOG-TEST-26.                                                   IF1164.2
087900     COMPUTE ARG1 = ARG1 + 0.2.                                   IF1164.2
088000 F-LOG-DELETE-26.                                                 IF1164.2
088100     PERFORM  DE-LETE.                                            IF1164.2
088200     GO TO    F-LOG-WRITE-26.                                     IF1164.2
088300 F-LOG-WRITE-26.                                                  IF1164.2
088400     MOVE "F-LOG-26" TO PAR-NAME.                                 IF1164.2
088500     PERFORM  PRINT-DETAIL.                                       IF1164.2
088600********************END OF TESTS***************                   IF1164.2
088700 CCVS-EXIT SECTION.                                               IF1164.2
088800 CCVS-999999.                                                     IF1164.2
088900     GO TO CLOSE-FILES.                                           IF1164.2
      *END-OF,IF116A                                                                  
