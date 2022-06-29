      *HEADER,COBOL,IF122A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1224.2
000200 PROGRAM-ID.                                                      IF1224.2
000300     IF122A.                                                      IF1224.2
000400                                                                  IF1224.2
000500***********************************************************       IF1224.2
000600*                                                         *       IF1224.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1224.2
000800* It contains tests for the Intrinsic Function MIDRANGE   *       IF1224.2
000900*                                                         *       IF1224.2
001000***********************************************************       IF1224.2
001100 ENVIRONMENT DIVISION.                                            IF1224.2
001200 CONFIGURATION SECTION.                                           IF1224.2
001300 SOURCE-COMPUTER.                                                 IF1224.2
001400     XXXXX082.                                                    IF1224.2
001500 OBJECT-COMPUTER.                                                 IF1224.2
001600     XXXXX083.                                                    IF1224.2
001700 INPUT-OUTPUT SECTION.                                            IF1224.2
001800 FILE-CONTROL.                                                    IF1224.2
001900     SELECT PRINT-FILE ASSIGN TO                                  IF1224.2
002000     XXXXX055.                                                    IF1224.2
002100 DATA DIVISION.                                                   IF1224.2
002200 FILE SECTION.                                                    IF1224.2
002300 FD  PRINT-FILE.                                                  IF1224.2
002400 01  PRINT-REC PICTURE X(120).                                    IF1224.2
002500 01  DUMMY-RECORD PICTURE X(120).                                 IF1224.2
002600 WORKING-STORAGE SECTION.                                         IF1224.2
002700***********************************************************       IF1224.2
002800* Variables specific to the Intrinsic Function Test IF122A*       IF1224.2
002900***********************************************************       IF1224.2
003000 01  A                   PIC S9(10)          VALUE 5.             IF1224.2
003100 01  B                   PIC S9(10)          VALUE 7.             IF1224.2
003200 01  C                   PIC S9(10)          VALUE -4.            IF1224.2
003300 01  D                   PIC S9(10)          VALUE 10.            IF1224.2
003400 01  E                   PIC S9(5)V9(5)      VALUE 34.26.         IF1224.2
003500 01  F                   PIC S9(5)V9(5)      VALUE -8.32.         IF1224.2
003600 01  G                   PIC S9(5)V9(5)      VALUE 4.08.          IF1224.2
003700 01  H                   PIC S9(5)V9(5)      VALUE -5.3.          IF1224.2
003800 01  M                   PIC S9(10)          VALUE 320000.        IF1224.2
003900 01  N                   PIC S9(10)          VALUE 650000.        IF1224.2
004000 01  O                   PIC S9(10)          VALUE -430000.       IF1224.2
004100 01  P                   PIC S9(10)          VALUE 1.             IF1224.2
004200 01  Q                   PIC S9(10)          VALUE 3.             IF1224.2
004300 01  R                   PIC S9(10)          VALUE 5.             IF1224.2
004400 01  ARG1                PIC S9(10)          VALUE 2.             IF1224.2
004500 01  ARR                                     VALUE "40537".       IF1224.2
004600     02  IND OCCURS 5 TIMES PIC 9.                                IF1224.2
004700 01  TEMP                PIC S9(10)V9(5).                         IF1224.2
004800 01  WS-NUM              PIC S9(6)V9(7).                          IF1224.2
004900 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1224.2
005000 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1224.2
005100*                                                                 IF1224.2
005200**********************************************************        IF1224.2
005300*                                                                 IF1224.2
005400 01  TEST-RESULTS.                                                IF1224.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IF1224.2
005600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1224.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IF1224.2
005800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1224.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IF1224.2
006000     02  PAR-NAME.                                                IF1224.2
006100       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1224.2
006200       03  PARDOT-X              PIC X      VALUE SPACE.          IF1224.2
006300       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1224.2
006400     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1224.2
006500     02 RE-MARK                  PIC X(61).                       IF1224.2
006600 01  TEST-COMPUTED.                                               IF1224.2
006700     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1224.2
006800     02 FILLER                   PIC X(17)  VALUE                 IF1224.2
006900            "       COMPUTED=".                                   IF1224.2
007000     02 COMPUTED-X.                                               IF1224.2
007100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1224.2
007200     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1224.2
007300                                 PIC -9(9).9(9).                  IF1224.2
007400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1224.2
007500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1224.2
007600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1224.2
007700     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1224.2
007800         04 COMPUTED-18V0                    PIC -9(18).          IF1224.2
007900         04 FILLER                           PIC X.               IF1224.2
008000     03 FILLER PIC X(50) VALUE SPACE.                             IF1224.2
008100 01  TEST-CORRECT.                                                IF1224.2
008200     02 FILLER PIC X(30) VALUE SPACE.                             IF1224.2
008300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1224.2
008400     02 CORRECT-X.                                                IF1224.2
008500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1224.2
008600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1224.2
008700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1224.2
008800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1224.2
008900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1224.2
009000     03      CR-18V0 REDEFINES CORRECT-A.                         IF1224.2
009100         04 CORRECT-18V0                     PIC -9(18).          IF1224.2
009200         04 FILLER                           PIC X.               IF1224.2
009300     03 FILLER PIC X(2) VALUE SPACE.                              IF1224.2
009400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1224.2
009500 01  TEST-CORRECT-MIN.                                            IF1224.2
009600     02 FILLER PIC X(30) VALUE SPACE.                             IF1224.2
009700     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1224.2
009800     02 CORRECTMI-X.                                              IF1224.2
009900     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1224.2
010000     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1224.2
010100     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1224.2
010200     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1224.2
010300     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1224.2
010400     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1224.2
010500         04 CORRECTMI-18V0                     PIC -9(18).        IF1224.2
010600         04 FILLER                           PIC X.               IF1224.2
010700     03 FILLER PIC X(2) VALUE SPACE.                              IF1224.2
010800     03 FILLER                           PIC X(48) VALUE SPACE.   IF1224.2
010900 01  TEST-CORRECT-MAX.                                            IF1224.2
011000     02 FILLER PIC X(30) VALUE SPACE.                             IF1224.2
011100     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1224.2
011200     02 CORRECTMA-X.                                              IF1224.2
011300     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1224.2
011400     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1224.2
011500     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1224.2
011600     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1224.2
011700     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1224.2
011800     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1224.2
011900         04 CORRECTMA-18V0                     PIC -9(18).        IF1224.2
012000         04 FILLER                           PIC X.               IF1224.2
012100     03 FILLER PIC X(2) VALUE SPACE.                              IF1224.2
012200     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1224.2
012300 01  CCVS-C-1.                                                    IF1224.2
012400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1224.2
012500-    "SS  PARAGRAPH-NAME                                          IF1224.2
012600-    "       REMARKS".                                            IF1224.2
012700     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1224.2
012800 01  CCVS-C-2.                                                    IF1224.2
012900     02 FILLER                     PIC X        VALUE SPACE.      IF1224.2
013000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1224.2
013100     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1224.2
013200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1224.2
013300     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1224.2
013400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1224.2
013500 01  REC-CT                        PIC 99       VALUE ZERO.       IF1224.2
013600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1224.2
013700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1224.2
013800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1224.2
013900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1224.2
014000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1224.2
014100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1224.2
014200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1224.2
014300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1224.2
014400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1224.2
014500 01  CCVS-H-1.                                                    IF1224.2
014600     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1224.2
014700     02  FILLER                    PIC X(42)    VALUE             IF1224.2
014800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1224.2
014900     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1224.2
015000 01  CCVS-H-2A.                                                   IF1224.2
015100   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1224.2
015200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1224.2
015300   02  FILLER                        PIC XXXX   VALUE             IF1224.2
015400     "4.2 ".                                                      IF1224.2
015500   02  FILLER                        PIC X(28)  VALUE             IF1224.2
015600            " COPY - NOT FOR DISTRIBUTION".                       IF1224.2
015700   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1224.2
015800                                                                  IF1224.2
015900 01  CCVS-H-2B.                                                   IF1224.2
016000   02  FILLER                        PIC X(15)  VALUE             IF1224.2
016100            "TEST RESULT OF ".                                    IF1224.2
016200   02  TEST-ID                       PIC X(9).                    IF1224.2
016300   02  FILLER                        PIC X(4)   VALUE             IF1224.2
016400            " IN ".                                               IF1224.2
016500   02  FILLER                        PIC X(12)  VALUE             IF1224.2
016600     " HIGH       ".                                              IF1224.2
016700   02  FILLER                        PIC X(22)  VALUE             IF1224.2
016800            " LEVEL VALIDATION FOR ".                             IF1224.2
016900   02  FILLER                        PIC X(58)  VALUE             IF1224.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1224.2
017100 01  CCVS-H-3.                                                    IF1224.2
017200     02  FILLER                      PIC X(34)  VALUE             IF1224.2
017300            " FOR OFFICIAL USE ONLY    ".                         IF1224.2
017400     02  FILLER                      PIC X(58)  VALUE             IF1224.2
017500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1224.2
017600     02  FILLER                      PIC X(28)  VALUE             IF1224.2
017700            "  COPYRIGHT   1985 ".                                IF1224.2
017800 01  CCVS-E-1.                                                    IF1224.2
017900     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1224.2
018000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1224.2
018100     02 ID-AGAIN                     PIC X(9).                    IF1224.2
018200     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1224.2
018300 01  CCVS-E-2.                                                    IF1224.2
018400     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1224.2
018500     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1224.2
018600     02 CCVS-E-2-2.                                               IF1224.2
018700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1224.2
018800         03 FILLER                   PIC X      VALUE SPACE.      IF1224.2
018900         03 ENDER-DESC               PIC X(44)  VALUE             IF1224.2
019000            "ERRORS ENCOUNTERED".                                 IF1224.2
019100 01  CCVS-E-3.                                                    IF1224.2
019200     02  FILLER                      PIC X(22)  VALUE             IF1224.2
019300            " FOR OFFICIAL USE ONLY".                             IF1224.2
019400     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1224.2
019500     02  FILLER                      PIC X(58)  VALUE             IF1224.2
019600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1224.2
019700     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1224.2
019800     02 FILLER                       PIC X(15)  VALUE             IF1224.2
019900             " COPYRIGHT 1985".                                   IF1224.2
020000 01  CCVS-E-4.                                                    IF1224.2
020100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1224.2
020200     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1224.2
020300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1224.2
020400     02 FILLER                       PIC X(40)  VALUE             IF1224.2
020500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1224.2
020600 01  XXINFO.                                                      IF1224.2
020700     02 FILLER                       PIC X(19)  VALUE             IF1224.2
020800            "*** INFORMATION ***".                                IF1224.2
020900     02 INFO-TEXT.                                                IF1224.2
021000       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1224.2
021100       04 XXCOMPUTED                 PIC X(20).                   IF1224.2
021200       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1224.2
021300       04 XXCORRECT                  PIC X(20).                   IF1224.2
021400     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1224.2
021500 01  HYPHEN-LINE.                                                 IF1224.2
021600     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1224.2
021700     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1224.2
021800-    "*****************************************".                 IF1224.2
021900     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1224.2
022000-    "******************************".                            IF1224.2
022100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1224.2
022200     "IF122A".                                                    IF1224.2
022300 PROCEDURE DIVISION.                                              IF1224.2
022400 CCVS1 SECTION.                                                   IF1224.2
022500 OPEN-FILES.                                                      IF1224.2
022600     OPEN     OUTPUT PRINT-FILE.                                  IF1224.2
022700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1224.2
022800     MOVE    SPACE TO TEST-RESULTS.                               IF1224.2
022900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1224.2
023000     GO TO CCVS1-EXIT.                                            IF1224.2
023100 CLOSE-FILES.                                                     IF1224.2
023200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1224.2
023300 TERMINATE-CCVS.                                                  IF1224.2
023400     STOP     RUN.                                                IF1224.2
023500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1224.2
023600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1224.2
023700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1224.2
023800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1224.2
023900     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1224.2
024000 PRINT-DETAIL.                                                    IF1224.2
024100     IF REC-CT NOT EQUAL TO ZERO                                  IF1224.2
024200             MOVE "." TO PARDOT-X                                 IF1224.2
024300             MOVE REC-CT TO DOTVALUE.                             IF1224.2
024400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1224.2
024500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1224.2
024600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1224.2
024700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1224.2
024800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1224.2
024900     MOVE SPACE TO CORRECT-X.                                     IF1224.2
025000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1224.2
025100     MOVE     SPACE TO RE-MARK.                                   IF1224.2
025200 HEAD-ROUTINE.                                                    IF1224.2
025300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1224.2
025400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1224.2
025500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1224.2
025600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1224.2
025700 COLUMN-NAMES-ROUTINE.                                            IF1224.2
025800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1224.2
025900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1224.2
026000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1224.2
026100 END-ROUTINE.                                                     IF1224.2
026200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1224.2
026300 END-RTN-EXIT.                                                    IF1224.2
026400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1224.2
026500 END-ROUTINE-1.                                                   IF1224.2
026600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1224.2
026700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1224.2
026800      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1224.2
026900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1224.2
027000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1224.2
027100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1224.2
027200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1224.2
027300  END-ROUTINE-12.                                                 IF1224.2
027400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1224.2
027500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1224.2
027600         MOVE "NO " TO ERROR-TOTAL                                IF1224.2
027700         ELSE                                                     IF1224.2
027800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1224.2
027900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1224.2
028000     PERFORM WRITE-LINE.                                          IF1224.2
028100 END-ROUTINE-13.                                                  IF1224.2
028200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1224.2
028300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1224.2
028400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1224.2
028500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1224.2
028600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1224.2
028700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1224.2
028800          MOVE "NO " TO ERROR-TOTAL                               IF1224.2
028900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1224.2
029000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1224.2
029100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1224.2
029200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1224.2
029300 WRITE-LINE.                                                      IF1224.2
029400     ADD 1 TO RECORD-COUNT.                                       IF1224.2
029500*    IF RECORD-COUNT GREATER 42                                   IF1224.2
029600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1224.2
029700*        MOVE SPACE TO DUMMY-RECORD                               IF1224.2
029800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1224.2
029900*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1224.2
030000*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1224.2
030100*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1224.2
030200*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1224.2
030300*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1224.2
030400*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1224.2
030500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1224.2
030600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1224.2
030700*        MOVE ZERO TO RECORD-COUNT.                               IF1224.2
030800     PERFORM WRT-LN.                                              IF1224.2
030900 WRT-LN.                                                          IF1224.2
031000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1224.2
031100     MOVE SPACE TO DUMMY-RECORD.                                  IF1224.2
031200 BLANK-LINE-PRINT.                                                IF1224.2
031300     PERFORM WRT-LN.                                              IF1224.2
031400 FAIL-ROUTINE.                                                    IF1224.2
031500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1224.2
031600            GO TO FAIL-ROUTINE-WRITE.                             IF1224.2
031700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1224.2
031800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1224.2
031900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1224.2
032000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1224.2
032100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1224.2
032200     GO TO  FAIL-ROUTINE-EX.                                      IF1224.2
032300 FAIL-ROUTINE-WRITE.                                              IF1224.2
032400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1224.2
032500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1224.2
032600                              CORMA-ANSI-REFERENCE.               IF1224.2
032700     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1224.2
032800           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1224.2
032900           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1224.2
033000     ELSE                                                         IF1224.2
033100           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1224.2
033200     PERFORM WRITE-LINE.                                          IF1224.2
033300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1224.2
033400 FAIL-ROUTINE-EX. EXIT.                                           IF1224.2
033500 BAIL-OUT.                                                        IF1224.2
033600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1224.2
033700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1224.2
033800 BAIL-OUT-WRITE.                                                  IF1224.2
033900     MOVE CORRECT-A TO XXCORRECT.                                 IF1224.2
034000     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1224.2
034100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1224.2
034200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1224.2
034300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1224.2
034400 BAIL-OUT-EX. EXIT.                                               IF1224.2
034500 CCVS1-EXIT.                                                      IF1224.2
034600     EXIT.                                                        IF1224.2
034700********************************************************          IF1224.2
034800*                                                      *          IF1224.2
034900*    Intrinsic Function Tests        IF122A - MIDRANGE *          IF1224.2
035000*                                                      *          IF1224.2
035100********************************************************          IF1224.2
035200 SECT-IF122A SECTION.                                             IF1224.2
035300 F-MIDRANGE-INFO.                                                 IF1224.2
035400     MOVE     "See ref. A-55 2.26" TO ANSI-REFERENCE.             IF1224.2
035500     MOVE     "MIDRANGE Function" TO FEATURE.                     IF1224.2
035600*****************TEST (a) - SIMPLE TEST*****************          IF1224.2
035700 F-MIDRANGE-01.                                                   IF1224.2
035800     MOVE ZERO TO WS-NUM.                                         IF1224.2
035900 F-MIDRANGE-TEST-01.                                              IF1224.2
036000     COMPUTE WS-NUM = FUNCTION MIDRANGE(5, -2, -14, 0).           IF1224.2
036100     IF (WS-NUM >= -4.50009) AND                                  IF1224.2
036200        (WS-NUM <= -4.49991)                                      IF1224.2
036300                    PERFORM PASS                                  IF1224.2
036400     ELSE                                                         IF1224.2
036500                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
036600                    MOVE -4.5 TO CORRECT-N                        IF1224.2
036700                    PERFORM FAIL.                                 IF1224.2
036800     GO TO F-MIDRANGE-WRITE-01.                                   IF1224.2
036900 F-MIDRANGE-DELETE-01.                                            IF1224.2
037000     PERFORM  DE-LETE.                                            IF1224.2
037100     GO TO    F-MIDRANGE-WRITE-01.                                IF1224.2
037200 F-MIDRANGE-WRITE-01.                                             IF1224.2
037300     MOVE "F-MIDRANGE-01" TO PAR-NAME.                            IF1224.2
037400     PERFORM  PRINT-DETAIL.                                       IF1224.2
037500*****************TEST (b) - SIMPLE TEST*****************          IF1224.2
037600 F-MIDRANGE-02.                                                   IF1224.2
037700     EVALUATE FUNCTION MIDRANGE(3.9, -0.3, 8.7, 100.2)            IF1224.2
037800     WHEN 49.9490 THRU 49.9510                                    IF1224.2
037900                    PERFORM PASS                                  IF1224.2
038000     WHEN OTHER                                                   IF1224.2
038100                    PERFORM FAIL.                                 IF1224.2
038200     GO TO F-MIDRANGE-WRITE-02.                                   IF1224.2
038300 F-MIDRANGE-DELETE-02.                                            IF1224.2
038400     PERFORM  DE-LETE.                                            IF1224.2
038500     GO TO    F-MIDRANGE-WRITE-02.                                IF1224.2
038600 F-MIDRANGE-WRITE-02.                                             IF1224.2
038700     MOVE "F-MIDRANGE-02" TO PAR-NAME.                            IF1224.2
038800     PERFORM  PRINT-DETAIL.                                       IF1224.2
038900*****************TEST (c) - SIMPLE TEST*****************          IF1224.2
039000 F-MIDRANGE-03.                                                   IF1224.2
039100     IF FUNCTION MIDRANGE(A, B, C, D) = 3 THEN                    IF1224.2
039200                    PERFORM PASS                                  IF1224.2
039300     ELSE                                                         IF1224.2
039400                    PERFORM FAIL.                                 IF1224.2
039500     GO TO F-MIDRANGE-WRITE-03.                                   IF1224.2
039600 F-MIDRANGE-DELETE-03.                                            IF1224.2
039700     PERFORM  DE-LETE.                                            IF1224.2
039800     GO TO    F-MIDRANGE-WRITE-03.                                IF1224.2
039900 F-MIDRANGE-WRITE-03.                                             IF1224.2
040000     MOVE "F-MIDRANGE-03" TO PAR-NAME.                            IF1224.2
040100     PERFORM  PRINT-DETAIL.                                       IF1224.2
040200*****************TEST (d) - SIMPLE TEST*****************          IF1224.2
040300 F-MIDRANGE-04.                                                   IF1224.2
040400     MOVE ZERO TO WS-NUM.                                         IF1224.2
040500 F-MIDRANGE-TEST-04.                                              IF1224.2
040600     COMPUTE WS-NUM = FUNCTION MIDRANGE(E, F, G, H).              IF1224.2
040700     IF (WS-NUM >= 12.9697) AND                                   IF1224.2
040800        (WS-NUM <= 12.9703)                                       IF1224.2
040900                    PERFORM PASS                                  IF1224.2
041000     ELSE                                                         IF1224.2
041100                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
041200                    MOVE 12.97 TO CORRECT-N                       IF1224.2
041300                    PERFORM FAIL.                                 IF1224.2
041400     GO TO F-MIDRANGE-WRITE-04.                                   IF1224.2
041500 F-MIDRANGE-DELETE-04.                                            IF1224.2
041600     PERFORM  DE-LETE.                                            IF1224.2
041700     GO TO    F-MIDRANGE-WRITE-04.                                IF1224.2
041800 F-MIDRANGE-WRITE-04.                                             IF1224.2
041900     MOVE "F-MIDRANGE-04" TO PAR-NAME.                            IF1224.2
042000     PERFORM  PRINT-DETAIL.                                       IF1224.2
042100*****************TEST (e) - SIMPLE TEST*****************          IF1224.2
042200 F-MIDRANGE-05.                                                   IF1224.2
042300     MOVE ZERO TO WS-NUM.                                         IF1224.2
042400 F-MIDRANGE-TEST-05.                                              IF1224.2
042500     COMPUTE WS-NUM = FUNCTION MIDRANGE(10.2, -0.2, 5.6, -15.6).  IF1224.2
042600     IF (WS-NUM >= -2.70005) AND                                  IF1224.2
042700        (WS-NUM <= -2.69995)                                      IF1224.2
042800                    PERFORM PASS                                  IF1224.2
042900     ELSE                                                         IF1224.2
043000                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
043100                    MOVE -2.7 TO CORRECT-N                        IF1224.2
043200                    PERFORM FAIL.                                 IF1224.2
043300     GO TO F-MIDRANGE-WRITE-05.                                   IF1224.2
043400 F-MIDRANGE-DELETE-05.                                            IF1224.2
043500     PERFORM  DE-LETE.                                            IF1224.2
043600     GO TO    F-MIDRANGE-WRITE-05.                                IF1224.2
043700 F-MIDRANGE-WRITE-05.                                             IF1224.2
043800     MOVE "F-MIDRANGE-05" TO PAR-NAME.                            IF1224.2
043900     PERFORM  PRINT-DETAIL.                                       IF1224.2
044000*****************TEST (f) - SIMPLE TEST*****************          IF1224.2
044100 F-MIDRANGE-06.                                                   IF1224.2
044200     MOVE ZERO TO WS-NUM.                                         IF1224.2
044300 F-MIDRANGE-TEST-06.                                              IF1224.2
044400     COMPUTE WS-NUM = FUNCTION MIDRANGE(A, B, C, D, E, F, G, H).  IF1224.2
044500     IF (WS-NUM >= 12.9697) AND                                   IF1224.2
044600        (WS-NUM <= 12.9703)                                       IF1224.2
044700                    PERFORM PASS                                  IF1224.2
044800     ELSE                                                         IF1224.2
044900                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
045000                    MOVE 12.97 TO CORRECT-N                       IF1224.2
045100                    PERFORM FAIL.                                 IF1224.2
045200     GO TO F-MIDRANGE-WRITE-06.                                   IF1224.2
045300 F-MIDRANGE-DELETE-06.                                            IF1224.2
045400     PERFORM  DE-LETE.                                            IF1224.2
045500     GO TO    F-MIDRANGE-WRITE-06.                                IF1224.2
045600 F-MIDRANGE-WRITE-06.                                             IF1224.2
045700     MOVE "F-MIDRANGE-06" TO PAR-NAME.                            IF1224.2
045800     PERFORM  PRINT-DETAIL.                                       IF1224.2
045900*****************TEST (g) - SIMPLE TEST*****************          IF1224.2
046000 F-MIDRANGE-07.                                                   IF1224.2
046100     MOVE ZERO TO WS-NUM.                                         IF1224.2
046200 F-MIDRANGE-TEST-07.                                              IF1224.2
046300     COMPUTE WS-NUM = FUNCTION MIDRANGE(2.6 + 30, 4.5 * 2).       IF1224.2
046400     IF (WS-NUM >= 20.7996) AND                                   IF1224.2
046500        (WS-NUM <= 20.8004)                                       IF1224.2
046600                    PERFORM PASS                                  IF1224.2
046700     ELSE                                                         IF1224.2
046800                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
046900                    MOVE 20.8 TO CORRECT-N                        IF1224.2
047000                    PERFORM FAIL.                                 IF1224.2
047100     GO TO F-MIDRANGE-WRITE-07.                                   IF1224.2
047200 F-MIDRANGE-DELETE-07.                                            IF1224.2
047300     PERFORM  DE-LETE.                                            IF1224.2
047400     GO TO    F-MIDRANGE-WRITE-07.                                IF1224.2
047500 F-MIDRANGE-WRITE-07.                                             IF1224.2
047600     MOVE "F-MIDRANGE-07" TO PAR-NAME.                            IF1224.2
047700     PERFORM  PRINT-DETAIL.                                       IF1224.2
047800*****************TEST (h) - SIMPLE TEST*****************          IF1224.2
047900 F-MIDRANGE-08.                                                   IF1224.2
048000     MOVE ZERO TO WS-NUM.                                         IF1224.2
048100 F-MIDRANGE-TEST-08.                                              IF1224.2
048200     COMPUTE WS-NUM = FUNCTION MIDRANGE(IND(1), IND(2),           IF1224.2
048300                                                   IND(3)).       IF1224.2
048400     IF (WS-NUM >= 2.49995) AND                                   IF1224.2
048500        (WS-NUM <= 2.50005)                                       IF1224.2
048600                    PERFORM PASS                                  IF1224.2
048700     ELSE                                                         IF1224.2
048800                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
048900                    MOVE 2.5 TO CORRECT-N                         IF1224.2
049000                    PERFORM FAIL.                                 IF1224.2
049100     GO TO F-MIDRANGE-WRITE-08.                                   IF1224.2
049200 F-MIDRANGE-DELETE-08.                                            IF1224.2
049300     PERFORM  DE-LETE.                                            IF1224.2
049400     GO TO    F-MIDRANGE-WRITE-08.                                IF1224.2
049500 F-MIDRANGE-WRITE-08.                                             IF1224.2
049600     MOVE "F-MIDRANGE-08" TO PAR-NAME.                            IF1224.2
049700     PERFORM  PRINT-DETAIL.                                       IF1224.2
049800*****************TEST (i) - SIMPLE TEST*****************          IF1224.2
049900 F-MIDRANGE-09.                                                   IF1224.2
050000     MOVE ZERO TO WS-NUM.                                         IF1224.2
050100 F-MIDRANGE-TEST-09.                                              IF1224.2
050200     COMPUTE WS-NUM = FUNCTION MIDRANGE(IND(P), IND(Q),           IF1224.2
050300                                                  IND(R)).        IF1224.2
050400     IF (WS-NUM >= 5.49989) AND                                   IF1224.2
050500        (WS-NUM <= 5.50011)                                       IF1224.2
050600                    PERFORM PASS                                  IF1224.2
050700     ELSE                                                         IF1224.2
050800                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
050900                    MOVE 5.5 TO CORRECT-N                         IF1224.2
051000                    PERFORM FAIL.                                 IF1224.2
051100     GO TO F-MIDRANGE-WRITE-09.                                   IF1224.2
051200 F-MIDRANGE-DELETE-09.                                            IF1224.2
051300     PERFORM  DE-LETE.                                            IF1224.2
051400     GO TO    F-MIDRANGE-WRITE-09.                                IF1224.2
051500 F-MIDRANGE-WRITE-09.                                             IF1224.2
051600     MOVE "F-MIDRANGE-09" TO PAR-NAME.                            IF1224.2
051700     PERFORM  PRINT-DETAIL.                                       IF1224.2
051800*****************TEST (j) - SIMPLE TEST*****************          IF1224.2
051900 F-MIDRANGE-10.                                                   IF1224.2
052000     MOVE ZERO TO WS-NUM.                                         IF1224.2
052100 F-MIDRANGE-TEST-10.                                              IF1224.2
052200     COMPUTE WS-NUM = FUNCTION MIDRANGE(IND(ALL)).                IF1224.2
052300     IF (WS-NUM >= 3.49993) AND                                   IF1224.2
052400        (WS-NUM <= 3.50007)                                       IF1224.2
052500                    PERFORM PASS                                  IF1224.2
052600     ELSE                                                         IF1224.2
052700                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
052800                    MOVE 3.5 TO CORRECT-N                         IF1224.2
052900                    PERFORM FAIL.                                 IF1224.2
053000     GO TO F-MIDRANGE-WRITE-10.                                   IF1224.2
053100 F-MIDRANGE-DELETE-10.                                            IF1224.2
053200     PERFORM  DE-LETE.                                            IF1224.2
053300     GO TO    F-MIDRANGE-WRITE-10.                                IF1224.2
053400 F-MIDRANGE-WRITE-10.                                             IF1224.2
053500     MOVE "F-MIDRANGE-10" TO PAR-NAME.                            IF1224.2
053600     PERFORM  PRINT-DETAIL.                                       IF1224.2
053700*****************TEST (l) - SIMPLE TEST*****************          IF1224.2
053800 F-MIDRANGE-12.                                                   IF1224.2
053900     MOVE ZERO TO WS-NUM.                                         IF1224.2
054000 F-MIDRANGE-TEST-12.                                              IF1224.2
054100     COMPUTE WS-NUM = FUNCTION MIDRANGE(M, N, O).                 IF1224.2
054200     IF WS-NUM = 110000 THEN                                      IF1224.2
054300                    PERFORM PASS                                  IF1224.2
054400     ELSE                                                         IF1224.2
054500                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
054600                    MOVE 110000 TO CORRECT-N                      IF1224.2
054700                    PERFORM FAIL.                                 IF1224.2
054800     GO TO F-MIDRANGE-WRITE-12.                                   IF1224.2
054900 F-MIDRANGE-DELETE-12.                                            IF1224.2
055000     PERFORM  DE-LETE.                                            IF1224.2
055100     GO TO    F-MIDRANGE-WRITE-12.                                IF1224.2
055200 F-MIDRANGE-WRITE-12.                                             IF1224.2
055300     MOVE "F-MIDRANGE-12" TO PAR-NAME.                            IF1224.2
055400     PERFORM  PRINT-DETAIL.                                       IF1224.2
055500*****************TEST (m) - SIMPLE TEST*****************          IF1224.2
055600 F-MIDRANGE-13.                                                   IF1224.2
055700     MOVE ZERO TO WS-NUM.                                         IF1224.2
055800 F-MIDRANGE-TEST-13.                                              IF1224.2
055900     COMPUTE WS-NUM = FUNCTION MIDRANGE(A, 5, A).                 IF1224.2
056000     IF WS-NUM = 5 THEN                                           IF1224.2
056100                    PERFORM PASS                                  IF1224.2
056200     ELSE                                                         IF1224.2
056300                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
056400                    MOVE 5 TO CORRECT-N                           IF1224.2
056500                    PERFORM FAIL.                                 IF1224.2
056600     GO TO F-MIDRANGE-WRITE-13.                                   IF1224.2
056700 F-MIDRANGE-DELETE-13.                                            IF1224.2
056800     PERFORM  DE-LETE.                                            IF1224.2
056900     GO TO    F-MIDRANGE-WRITE-13.                                IF1224.2
057000 F-MIDRANGE-WRITE-13.                                             IF1224.2
057100     MOVE "F-MIDRANGE-13" TO PAR-NAME.                            IF1224.2
057200     PERFORM  PRINT-DETAIL.                                       IF1224.2
057300*****************TEST (a) - COMPLEX TEST****************          IF1224.2
057400 F-MIDRANGE-14.                                                   IF1224.2
057500     MOVE ZERO TO WS-NUM.                                         IF1224.2
057600     MOVE  22.4995   TO MIN-RANGE.                                IF1224.2
057700     MOVE  22.5004 TO MAX-RANGE.                                  IF1224.2
057800 F-MIDRANGE-TEST-14.                                              IF1224.2
057900     COMPUTE WS-NUM = FUNCTION MIDRANGE(E, 9 * A, 0, B / 2).      IF1224.2
058000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1224.2
058100        (WS-NUM <= MAX-RANGE) THEN                                IF1224.2
058200                    PERFORM PASS                                  IF1224.2
058300     ELSE                                                         IF1224.2
058400                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
058500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1224.2
058600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1224.2
058700                    PERFORM FAIL.                                 IF1224.2
058800     GO TO F-MIDRANGE-WRITE-14.                                   IF1224.2
058900 F-MIDRANGE-DELETE-14.                                            IF1224.2
059000     PERFORM  DE-LETE.                                            IF1224.2
059100     GO TO    F-MIDRANGE-WRITE-14.                                IF1224.2
059200 F-MIDRANGE-WRITE-14.                                             IF1224.2
059300     MOVE "F-MIDRANGE-14" TO PAR-NAME.                            IF1224.2
059400     PERFORM  PRINT-DETAIL.                                       IF1224.2
059500*****************TEST (b) - COMPLEX TEST****************          IF1224.2
059600 F-MIDRANGE-15.                                                   IF1224.2
059700     MOVE ZERO TO WS-NUM.                                         IF1224.2
059800     MOVE 83.9983 TO MIN-RANGE.                                   IF1224.2
059900     MOVE 84.0017 TO MAX-RANGE.                                   IF1224.2
060000 F-MIDRANGE-TEST-15.                                              IF1224.2
060100     COMPUTE WS-NUM = FUNCTION MIDRANGE(A, B) + 78.               IF1224.2
060200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1224.2
060300        (WS-NUM <= MAX-RANGE) THEN                                IF1224.2
060400                    PERFORM PASS                                  IF1224.2
060500     ELSE                                                         IF1224.2
060600                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
060700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1224.2
060800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1224.2
060900                    PERFORM FAIL.                                 IF1224.2
061000     GO TO F-MIDRANGE-WRITE-15.                                   IF1224.2
061100 F-MIDRANGE-DELETE-15.                                            IF1224.2
061200     PERFORM  DE-LETE.                                            IF1224.2
061300     GO TO    F-MIDRANGE-WRITE-15.                                IF1224.2
061400 F-MIDRANGE-WRITE-15.                                             IF1224.2
061500     MOVE "F-MIDRANGE-15" TO PAR-NAME.                            IF1224.2
061600     PERFORM  PRINT-DETAIL.                                       IF1224.2
061700*****************TEST (c) - COMPLEX TEST****************          IF1224.2
061800 F-MIDRANGE-16.                                                   IF1224.2
061900     MOVE ZERO TO WS-NUM.                                         IF1224.2
062000     MOVE 2.49995 TO MIN-RANGE.                                   IF1224.2
062100     MOVE 2.50005 TO MAX-RANGE.                                   IF1224.2
062200 F-MIDRANGE-TEST-16.                                              IF1224.2
062300     COMPUTE WS-NUM = FUNCTION MIDRANGE(A, B) +                   IF1224.2
062400                      FUNCTION MIDRANGE(-2.6, -4.4).              IF1224.2
062500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1224.2
062600        (WS-NUM <= MAX-RANGE) THEN                                IF1224.2
062700                    PERFORM PASS                                  IF1224.2
062800     ELSE                                                         IF1224.2
062900                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
063000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1224.2
063100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1224.2
063200                    PERFORM FAIL.                                 IF1224.2
063300     GO TO F-MIDRANGE-WRITE-16.                                   IF1224.2
063400 F-MIDRANGE-DELETE-16.                                            IF1224.2
063500     PERFORM  DE-LETE.                                            IF1224.2
063600     GO TO    F-MIDRANGE-WRITE-16.                                IF1224.2
063700 F-MIDRANGE-WRITE-16.                                             IF1224.2
063800     MOVE "F-MIDRANGE-16" TO PAR-NAME.                            IF1224.2
063900     PERFORM  PRINT-DETAIL.                                       IF1224.2
064000*****************TEST (d) - COMPLEX TEST****************          IF1224.2
064100 F-MIDRANGE-17.                                                   IF1224.2
064200     MOVE ZERO TO WS-NUM.                                         IF1224.2
064300     MOVE 3.49993 TO MIN-RANGE.                                   IF1224.2
064400     MOVE 3.50007 TO MAX-RANGE.                                   IF1224.2
064500 F-MIDRANGE-TEST-17.                                              IF1224.2
064600     COMPUTE WS-NUM =                                             IF1224.2
064700             FUNCTION MIDRANGE(FUNCTION MIDRANGE(1, 3), 5).       IF1224.2
064800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1224.2
064900        (WS-NUM <= MAX-RANGE) THEN                                IF1224.2
065000                    PERFORM PASS                                  IF1224.2
065100     ELSE                                                         IF1224.2
065200                    MOVE WS-NUM TO COMPUTED-N                     IF1224.2
065300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1224.2
065400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1224.2
065500                    PERFORM FAIL.                                 IF1224.2
065600     GO TO F-MIDRANGE-WRITE-17.                                   IF1224.2
065700 F-MIDRANGE-DELETE-17.                                            IF1224.2
065800     PERFORM  DE-LETE.                                            IF1224.2
065900     GO TO    F-MIDRANGE-WRITE-17.                                IF1224.2
066000 F-MIDRANGE-WRITE-17.                                             IF1224.2
066100     MOVE "F-MIDRANGE-17" TO PAR-NAME.                            IF1224.2
066200     PERFORM  PRINT-DETAIL.                                       IF1224.2
066300*****************SPECIAL PERFORM TEST**********************       IF1224.2
066400 F-MIDRANGE-18.                                                   IF1224.2
066500     PERFORM F-MIDRANGE-TEST-18                                   IF1224.2
066600       UNTIL FUNCTION MIDRANGE(1, ARG1) > 10.                     IF1224.2
066700     PERFORM PASS.                                                IF1224.2
066800     GO TO F-MIDRANGE-WRITE-18.                                   IF1224.2
066900 F-MIDRANGE-TEST-18.                                              IF1224.2
067000     COMPUTE ARG1 = ARG1 + 1.                                     IF1224.2
067100 F-MIDRANGE-DELETE-18.                                            IF1224.2
067200     PERFORM  DE-LETE.                                            IF1224.2
067300     GO TO    F-MIDRANGE-WRITE-18.                                IF1224.2
067400 F-MIDRANGE-WRITE-18.                                             IF1224.2
067500     MOVE "F-MIDRANGE-18" TO PAR-NAME.                            IF1224.2
067600     PERFORM  PRINT-DETAIL.                                       IF1224.2
067700********************END OF TESTS***************                   IF1224.2
067800 CCVS-EXIT SECTION.                                               IF1224.2
067900 CCVS-999999.                                                     IF1224.2
068000     GO TO CLOSE-FILES.                                           IF1224.2
      *END-OF,IF122A                                                                  
