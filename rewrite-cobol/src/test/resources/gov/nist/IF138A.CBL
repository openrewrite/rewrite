      *HEADER,COBOL,IF138A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1384.2
000200 PROGRAM-ID.                                                      IF1384.2
000300     IF138A.                                                      IF1384.2
000400                                                                  IF1384.2
000500***********************************************************       IF1384.2
000600*                                                         *       IF1384.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1384.2
000800* It contains tests for the Intrinsic Function SUM .      *       IF1384.2
000900*                                                         *       IF1384.2
001000***********************************************************       IF1384.2
001100 ENVIRONMENT DIVISION.                                            IF1384.2
001200 CONFIGURATION SECTION.                                           IF1384.2
001300 SOURCE-COMPUTER.                                                 IF1384.2
001400     XXXXX082.                                                    IF1384.2
001500 OBJECT-COMPUTER.                                                 IF1384.2
001600     XXXXX083.                                                    IF1384.2
001700 INPUT-OUTPUT SECTION.                                            IF1384.2
001800 FILE-CONTROL.                                                    IF1384.2
001900     SELECT PRINT-FILE ASSIGN TO                                  IF1384.2
002000     XXXXX055.                                                    IF1384.2
002100 DATA DIVISION.                                                   IF1384.2
002200 FILE SECTION.                                                    IF1384.2
002300 FD  PRINT-FILE.                                                  IF1384.2
002400 01  PRINT-REC PICTURE X(120).                                    IF1384.2
002500 01  DUMMY-RECORD PICTURE X(120).                                 IF1384.2
002600 WORKING-STORAGE SECTION.                                         IF1384.2
002700***********************************************************       IF1384.2
002800* Variables specific to the Intrinsic Function Test IF138A*       IF1384.2
002900***********************************************************       IF1384.2
003000 01  A                   PIC S9(10)          VALUE 5.             IF1384.2
003100 01  B                   PIC S9(10)          VALUE 7.             IF1384.2
003200 01  C                   PIC S9(10)          VALUE -4.            IF1384.2
003300 01  D                   PIC S9(10)          VALUE 10.            IF1384.2
003400 01  E                   PIC S9(5)V9(5)      VALUE 34.26.         IF1384.2
003500 01  F                   PIC S9(5)V9(5)      VALUE -8.32.         IF1384.2
003600 01  G                   PIC S9(5)V9(5)      VALUE 4.08.          IF1384.2
003700 01  H                   PIC S9(5)V9(5)      VALUE -5.3.          IF1384.2
003800 01  M                   PIC S9(10)          VALUE 320000.        IF1384.2
003900 01  N                   PIC S9(10)          VALUE 650000.        IF1384.2
004000 01  O                   PIC S9(10)          VALUE -430000.       IF1384.2
004100 01  P                   PIC S9(10)          VALUE 1.             IF1384.2
004200 01  Q                   PIC S9(10)          VALUE 3.             IF1384.2
004300 01  R                   PIC S9(10)          VALUE 5.             IF1384.2
004400 01  ARG1                PIC S9(10)          VALUE 1.             IF1384.2
004500 01  ARR                                     VALUE "40537".       IF1384.2
004600     02  IND OCCURS 5 TIMES PIC 9.                                IF1384.2
004700 01  TEMP                PIC S9(10)V9(5).                         IF1384.2
004800 01  WS-NUM              PIC S9(6)V9(7).                          IF1384.2
004900 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1384.2
005000 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1384.2
005100*                                                                 IF1384.2
005200**********************************************************        IF1384.2
005300*                                                                 IF1384.2
005400 01  TEST-RESULTS.                                                IF1384.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IF1384.2
005600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1384.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IF1384.2
005800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1384.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IF1384.2
006000     02  PAR-NAME.                                                IF1384.2
006100       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1384.2
006200       03  PARDOT-X              PIC X      VALUE SPACE.          IF1384.2
006300       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1384.2
006400     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1384.2
006500     02 RE-MARK                  PIC X(61).                       IF1384.2
006600 01  TEST-COMPUTED.                                               IF1384.2
006700     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1384.2
006800     02 FILLER                   PIC X(17)  VALUE                 IF1384.2
006900            "       COMPUTED=".                                   IF1384.2
007000     02 COMPUTED-X.                                               IF1384.2
007100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1384.2
007200     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1384.2
007300                                 PIC -9(9).9(9).                  IF1384.2
007400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1384.2
007500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1384.2
007600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1384.2
007700     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1384.2
007800         04 COMPUTED-18V0                    PIC -9(18).          IF1384.2
007900         04 FILLER                           PIC X.               IF1384.2
008000     03 FILLER PIC X(50) VALUE SPACE.                             IF1384.2
008100 01  TEST-CORRECT.                                                IF1384.2
008200     02 FILLER PIC X(30) VALUE SPACE.                             IF1384.2
008300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1384.2
008400     02 CORRECT-X.                                                IF1384.2
008500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1384.2
008600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1384.2
008700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1384.2
008800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1384.2
008900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1384.2
009000     03      CR-18V0 REDEFINES CORRECT-A.                         IF1384.2
009100         04 CORRECT-18V0                     PIC -9(18).          IF1384.2
009200         04 FILLER                           PIC X.               IF1384.2
009300     03 FILLER PIC X(2) VALUE SPACE.                              IF1384.2
009400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1384.2
009500 01  TEST-CORRECT-MIN.                                            IF1384.2
009600     02 FILLER PIC X(30) VALUE SPACE.                             IF1384.2
009700     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1384.2
009800     02 CORRECTMI-X.                                              IF1384.2
009900     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1384.2
010000     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1384.2
010100     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1384.2
010200     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1384.2
010300     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1384.2
010400     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1384.2
010500         04 CORRECTMI-18V0                     PIC -9(18).        IF1384.2
010600         04 FILLER                           PIC X.               IF1384.2
010700     03 FILLER PIC X(2) VALUE SPACE.                              IF1384.2
010800     03 FILLER                           PIC X(48) VALUE SPACE.   IF1384.2
010900 01  TEST-CORRECT-MAX.                                            IF1384.2
011000     02 FILLER PIC X(30) VALUE SPACE.                             IF1384.2
011100     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1384.2
011200     02 CORRECTMA-X.                                              IF1384.2
011300     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1384.2
011400     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1384.2
011500     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1384.2
011600     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1384.2
011700     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1384.2
011800     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1384.2
011900         04 CORRECTMA-18V0                     PIC -9(18).        IF1384.2
012000         04 FILLER                           PIC X.               IF1384.2
012100     03 FILLER PIC X(2) VALUE SPACE.                              IF1384.2
012200     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1384.2
012300 01  CCVS-C-1.                                                    IF1384.2
012400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1384.2
012500-    "SS  PARAGRAPH-NAME                                          IF1384.2
012600-    "       REMARKS".                                            IF1384.2
012700     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1384.2
012800 01  CCVS-C-2.                                                    IF1384.2
012900     02 FILLER                     PIC X        VALUE SPACE.      IF1384.2
013000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1384.2
013100     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1384.2
013200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1384.2
013300     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1384.2
013400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1384.2
013500 01  REC-CT                        PIC 99       VALUE ZERO.       IF1384.2
013600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1384.2
013700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1384.2
013800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1384.2
013900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1384.2
014000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1384.2
014100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1384.2
014200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1384.2
014300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1384.2
014400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1384.2
014500 01  CCVS-H-1.                                                    IF1384.2
014600     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1384.2
014700     02  FILLER                    PIC X(42)    VALUE             IF1384.2
014800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1384.2
014900     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1384.2
015000 01  CCVS-H-2A.                                                   IF1384.2
015100   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1384.2
015200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1384.2
015300   02  FILLER                        PIC XXXX   VALUE             IF1384.2
015400     "4.2 ".                                                      IF1384.2
015500   02  FILLER                        PIC X(28)  VALUE             IF1384.2
015600            " COPY - NOT FOR DISTRIBUTION".                       IF1384.2
015700   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1384.2
015800                                                                  IF1384.2
015900 01  CCVS-H-2B.                                                   IF1384.2
016000   02  FILLER                        PIC X(15)  VALUE             IF1384.2
016100            "TEST RESULT OF ".                                    IF1384.2
016200   02  TEST-ID                       PIC X(9).                    IF1384.2
016300   02  FILLER                        PIC X(4)   VALUE             IF1384.2
016400            " IN ".                                               IF1384.2
016500   02  FILLER                        PIC X(12)  VALUE             IF1384.2
016600     " HIGH       ".                                              IF1384.2
016700   02  FILLER                        PIC X(22)  VALUE             IF1384.2
016800            " LEVEL VALIDATION FOR ".                             IF1384.2
016900   02  FILLER                        PIC X(58)  VALUE             IF1384.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1384.2
017100 01  CCVS-H-3.                                                    IF1384.2
017200     02  FILLER                      PIC X(34)  VALUE             IF1384.2
017300            " FOR OFFICIAL USE ONLY    ".                         IF1384.2
017400     02  FILLER                      PIC X(58)  VALUE             IF1384.2
017500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1384.2
017600     02  FILLER                      PIC X(28)  VALUE             IF1384.2
017700            "  COPYRIGHT   1985 ".                                IF1384.2
017800 01  CCVS-E-1.                                                    IF1384.2
017900     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1384.2
018000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1384.2
018100     02 ID-AGAIN                     PIC X(9).                    IF1384.2
018200     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1384.2
018300 01  CCVS-E-2.                                                    IF1384.2
018400     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1384.2
018500     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1384.2
018600     02 CCVS-E-2-2.                                               IF1384.2
018700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1384.2
018800         03 FILLER                   PIC X      VALUE SPACE.      IF1384.2
018900         03 ENDER-DESC               PIC X(44)  VALUE             IF1384.2
019000            "ERRORS ENCOUNTERED".                                 IF1384.2
019100 01  CCVS-E-3.                                                    IF1384.2
019200     02  FILLER                      PIC X(22)  VALUE             IF1384.2
019300            " FOR OFFICIAL USE ONLY".                             IF1384.2
019400     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1384.2
019500     02  FILLER                      PIC X(58)  VALUE             IF1384.2
019600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1384.2
019700     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1384.2
019800     02 FILLER                       PIC X(15)  VALUE             IF1384.2
019900             " COPYRIGHT 1985".                                   IF1384.2
020000 01  CCVS-E-4.                                                    IF1384.2
020100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1384.2
020200     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1384.2
020300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1384.2
020400     02 FILLER                       PIC X(40)  VALUE             IF1384.2
020500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1384.2
020600 01  XXINFO.                                                      IF1384.2
020700     02 FILLER                       PIC X(19)  VALUE             IF1384.2
020800            "*** INFORMATION ***".                                IF1384.2
020900     02 INFO-TEXT.                                                IF1384.2
021000       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1384.2
021100       04 XXCOMPUTED                 PIC X(20).                   IF1384.2
021200       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1384.2
021300       04 XXCORRECT                  PIC X(20).                   IF1384.2
021400     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1384.2
021500 01  HYPHEN-LINE.                                                 IF1384.2
021600     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1384.2
021700     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1384.2
021800-    "*****************************************".                 IF1384.2
021900     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1384.2
022000-    "******************************".                            IF1384.2
022100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1384.2
022200     "IF138A".                                                    IF1384.2
022300 PROCEDURE DIVISION.                                              IF1384.2
022400 CCVS1 SECTION.                                                   IF1384.2
022500 OPEN-FILES.                                                      IF1384.2
022600     OPEN     OUTPUT PRINT-FILE.                                  IF1384.2
022700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1384.2
022800     MOVE    SPACE TO TEST-RESULTS.                               IF1384.2
022900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1384.2
023000     GO TO CCVS1-EXIT.                                            IF1384.2
023100 CLOSE-FILES.                                                     IF1384.2
023200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1384.2
023300 TERMINATE-CCVS.                                                  IF1384.2
023400     STOP     RUN.                                                IF1384.2
023500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1384.2
023600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1384.2
023700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1384.2
023800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1384.2
023900     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1384.2
024000 PRINT-DETAIL.                                                    IF1384.2
024100     IF REC-CT NOT EQUAL TO ZERO                                  IF1384.2
024200             MOVE "." TO PARDOT-X                                 IF1384.2
024300             MOVE REC-CT TO DOTVALUE.                             IF1384.2
024400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1384.2
024500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1384.2
024600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1384.2
024700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1384.2
024800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1384.2
024900     MOVE SPACE TO CORRECT-X.                                     IF1384.2
025000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1384.2
025100     MOVE     SPACE TO RE-MARK.                                   IF1384.2
025200 HEAD-ROUTINE.                                                    IF1384.2
025300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1384.2
025400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1384.2
025500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1384.2
025600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1384.2
025700 COLUMN-NAMES-ROUTINE.                                            IF1384.2
025800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1384.2
025900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1384.2
026000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1384.2
026100 END-ROUTINE.                                                     IF1384.2
026200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1384.2
026300 END-RTN-EXIT.                                                    IF1384.2
026400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1384.2
026500 END-ROUTINE-1.                                                   IF1384.2
026600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1384.2
026700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1384.2
026800      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1384.2
026900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1384.2
027000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1384.2
027100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1384.2
027200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1384.2
027300  END-ROUTINE-12.                                                 IF1384.2
027400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1384.2
027500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1384.2
027600         MOVE "NO " TO ERROR-TOTAL                                IF1384.2
027700         ELSE                                                     IF1384.2
027800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1384.2
027900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1384.2
028000     PERFORM WRITE-LINE.                                          IF1384.2
028100 END-ROUTINE-13.                                                  IF1384.2
028200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1384.2
028300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1384.2
028400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1384.2
028500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1384.2
028600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1384.2
028700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1384.2
028800          MOVE "NO " TO ERROR-TOTAL                               IF1384.2
028900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1384.2
029000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1384.2
029100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1384.2
029200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1384.2
029300 WRITE-LINE.                                                      IF1384.2
029400     ADD 1 TO RECORD-COUNT.                                       IF1384.2
029500*    IF RECORD-COUNT GREATER 42                                   IF1384.2
029600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1384.2
029700*        MOVE SPACE TO DUMMY-RECORD                               IF1384.2
029800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1384.2
029900*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1384.2
030000*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1384.2
030100*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1384.2
030200*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1384.2
030300*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1384.2
030400*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1384.2
030500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1384.2
030600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1384.2
030700*        MOVE ZERO TO RECORD-COUNT.                               IF1384.2
030800     PERFORM WRT-LN.                                              IF1384.2
030900 WRT-LN.                                                          IF1384.2
031000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1384.2
031100     MOVE SPACE TO DUMMY-RECORD.                                  IF1384.2
031200 BLANK-LINE-PRINT.                                                IF1384.2
031300     PERFORM WRT-LN.                                              IF1384.2
031400 FAIL-ROUTINE.                                                    IF1384.2
031500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1384.2
031600            GO TO FAIL-ROUTINE-WRITE.                             IF1384.2
031700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1384.2
031800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1384.2
031900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1384.2
032000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1384.2
032100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1384.2
032200     GO TO  FAIL-ROUTINE-EX.                                      IF1384.2
032300 FAIL-ROUTINE-WRITE.                                              IF1384.2
032400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1384.2
032500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1384.2
032600                              CORMA-ANSI-REFERENCE.               IF1384.2
032700     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1384.2
032800           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1384.2
032900           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1384.2
033000     ELSE                                                         IF1384.2
033100           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1384.2
033200     PERFORM WRITE-LINE.                                          IF1384.2
033300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1384.2
033400 FAIL-ROUTINE-EX. EXIT.                                           IF1384.2
033500 BAIL-OUT.                                                        IF1384.2
033600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1384.2
033700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1384.2
033800 BAIL-OUT-WRITE.                                                  IF1384.2
033900     MOVE CORRECT-A TO XXCORRECT.                                 IF1384.2
034000     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1384.2
034100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1384.2
034200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1384.2
034300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1384.2
034400 BAIL-OUT-EX. EXIT.                                               IF1384.2
034500 CCVS1-EXIT.                                                      IF1384.2
034600     EXIT.                                                        IF1384.2
034700********************************************************          IF1384.2
034800*                                                      *          IF1384.2
034900*    Intrinsic Function Tests         IF138A - SUM     *          IF1384.2
035000*                                                      *          IF1384.2
035100********************************************************          IF1384.2
035200 SECT-IF138A SECTION.                                             IF1384.2
035300 F-SUM-INFO.                                                      IF1384.2
035400     MOVE     "See ref. A-69 2.40" TO ANSI-REFERENCE.             IF1384.2
035500     MOVE     "SUM Function" TO FEATURE.                          IF1384.2
035600*****************TEST (a) - SIMPLE TEST*****************          IF1384.2
035700 F-SUM-01.                                                        IF1384.2
035800     MOVE ZERO TO WS-NUM.                                         IF1384.2
035900 F-SUM-TEST-01.                                                   IF1384.2
036000     COMPUTE WS-NUM = FUNCTION SUM(5, -2, -14, 0).                IF1384.2
036100     IF WS-NUM = -11 THEN                                         IF1384.2
036200                    PERFORM PASS                                  IF1384.2
036300     ELSE                                                         IF1384.2
036400                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
036500                    MOVE -11 TO CORRECT-N                         IF1384.2
036600                    PERFORM FAIL.                                 IF1384.2
036700     GO TO F-SUM-WRITE-01.                                        IF1384.2
036800 F-SUM-DELETE-01.                                                 IF1384.2
036900     PERFORM  DE-LETE.                                            IF1384.2
037000     GO TO    F-SUM-WRITE-01.                                     IF1384.2
037100 F-SUM-WRITE-01.                                                  IF1384.2
037200     MOVE "F-SUM-01" TO PAR-NAME.                                 IF1384.2
037300     PERFORM  PRINT-DETAIL.                                       IF1384.2
037400*****************TEST (b) - SIMPLE TEST*****************          IF1384.2
037500 F-SUM-02.                                                        IF1384.2
037600     EVALUATE FUNCTION SUM(3.9, -0.3, 8.7, 100.2)                 IF1384.2
037700     WHEN 112.498 THRU 122.502                                    IF1384.2
037800                    PERFORM PASS                                  IF1384.2
037900     WHEN OTHER                                                   IF1384.2
038000                    PERFORM FAIL.                                 IF1384.2
038100     GO TO F-SUM-WRITE-02.                                        IF1384.2
038200 F-SUM-DELETE-02.                                                 IF1384.2
038300     PERFORM  DE-LETE.                                            IF1384.2
038400     GO TO    F-SUM-WRITE-02.                                     IF1384.2
038500 F-SUM-WRITE-02.                                                  IF1384.2
038600     MOVE "F-SUM-02" TO PAR-NAME.                                 IF1384.2
038700     PERFORM  PRINT-DETAIL.                                       IF1384.2
038800*****************TEST (c) - SIMPLE TEST*****************          IF1384.2
038900 F-SUM-03.                                                        IF1384.2
039000     IF FUNCTION SUM(A, B, C, D) = 18 THEN                        IF1384.2
039100                    PERFORM PASS                                  IF1384.2
039200     ELSE                                                         IF1384.2
039300                    PERFORM FAIL.                                 IF1384.2
039400     GO TO F-SUM-WRITE-03.                                        IF1384.2
039500 F-SUM-DELETE-03.                                                 IF1384.2
039600     PERFORM  DE-LETE.                                            IF1384.2
039700     GO TO    F-SUM-WRITE-03.                                     IF1384.2
039800 F-SUM-WRITE-03.                                                  IF1384.2
039900     MOVE "F-SUM-03" TO PAR-NAME.                                 IF1384.2
040000     PERFORM  PRINT-DETAIL.                                       IF1384.2
040100*****************TEST (d) - SIMPLE TEST*****************          IF1384.2
040200 F-SUM-04.                                                        IF1384.2
040300     MOVE ZERO TO WS-NUM.                                         IF1384.2
040400 F-SUM-TEST-04.                                                   IF1384.2
040500     COMPUTE WS-NUM = FUNCTION SUM(E, F, G, H).                   IF1384.2
040600     IF (WS-NUM >= 24.7195) AND                                   IF1384.2
040700        (WS-NUM <= 24.7205)                                       IF1384.2
040800                    PERFORM PASS                                  IF1384.2
040900     ELSE                                                         IF1384.2
041000                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
041100                    MOVE 24.72 TO CORRECT-N                       IF1384.2
041200                    PERFORM FAIL.                                 IF1384.2
041300     GO TO F-SUM-WRITE-04.                                        IF1384.2
041400 F-SUM-DELETE-04.                                                 IF1384.2
041500     PERFORM  DE-LETE.                                            IF1384.2
041600     GO TO    F-SUM-WRITE-04.                                     IF1384.2
041700 F-SUM-WRITE-04.                                                  IF1384.2
041800     MOVE "F-SUM-04" TO PAR-NAME.                                 IF1384.2
041900     PERFORM  PRINT-DETAIL.                                       IF1384.2
042000*****************TEST (e) - SIMPLE TEST*****************          IF1384.2
042100 F-SUM-05.                                                        IF1384.2
042200     MOVE ZERO TO WS-NUM.                                         IF1384.2
042300 F-SUM-TEST-05.                                                   IF1384.2
042400     COMPUTE WS-NUM = FUNCTION SUM(10.2, -0.2, 5.6, -15.6).       IF1384.2
042500     IF (WS-NUM >= -0.000020) AND                                 IF1384.2
042600        (WS-NUM <=  0.000020)                                     IF1384.2
042700                    PERFORM PASS                                  IF1384.2
042800     ELSE                                                         IF1384.2
042900                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
043000                    MOVE 0 TO CORRECT-N                           IF1384.2
043100                    PERFORM FAIL.                                 IF1384.2
043200     GO TO F-SUM-WRITE-05.                                        IF1384.2
043300 F-SUM-DELETE-05.                                                 IF1384.2
043400     PERFORM  DE-LETE.                                            IF1384.2
043500     GO TO    F-SUM-WRITE-05.                                     IF1384.2
043600 F-SUM-WRITE-05.                                                  IF1384.2
043700     MOVE "F-SUM-05" TO PAR-NAME.                                 IF1384.2
043800     PERFORM  PRINT-DETAIL.                                       IF1384.2
043900*****************TEST (f) - SIMPLE TEST*****************          IF1384.2
044000 F-SUM-06.                                                        IF1384.2
044100     MOVE ZERO TO WS-NUM.                                         IF1384.2
044200 F-SUM-TEST-06.                                                   IF1384.2
044300     COMPUTE WS-NUM = FUNCTION SUM(A, B, C, D, E, F, G, H).       IF1384.2
044400     IF (WS-NUM >= 42.7191) AND                                   IF1384.2
044500        (WS-NUM <= 42.7209)                                       IF1384.2
044600                    PERFORM PASS                                  IF1384.2
044700     ELSE                                                         IF1384.2
044800                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
044900                    MOVE 42.72 TO CORRECT-N                       IF1384.2
045000                    PERFORM FAIL.                                 IF1384.2
045100     GO TO F-SUM-WRITE-06.                                        IF1384.2
045200 F-SUM-DELETE-06.                                                 IF1384.2
045300     PERFORM  DE-LETE.                                            IF1384.2
045400     GO TO    F-SUM-WRITE-06.                                     IF1384.2
045500 F-SUM-WRITE-06.                                                  IF1384.2
045600     MOVE "F-SUM-06" TO PAR-NAME.                                 IF1384.2
045700     PERFORM  PRINT-DETAIL.                                       IF1384.2
045800*****************TEST (g) - SIMPLE TEST*****************          IF1384.2
045900 F-SUM-07.                                                        IF1384.2
046000     MOVE ZERO TO WS-NUM.                                         IF1384.2
046100 F-SUM-TEST-07.                                                   IF1384.2
046200     COMPUTE WS-NUM = FUNCTION SUM(IND(1), IND(2), IND(3)).       IF1384.2
046300     IF WS-NUM = 9 THEN                                           IF1384.2
046400                    PERFORM PASS                                  IF1384.2
046500     ELSE                                                         IF1384.2
046600                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
046700                    MOVE 9 TO CORRECT-N                           IF1384.2
046800                    PERFORM FAIL.                                 IF1384.2
046900     GO TO F-SUM-WRITE-07.                                        IF1384.2
047000 F-SUM-DELETE-07.                                                 IF1384.2
047100     PERFORM  DE-LETE.                                            IF1384.2
047200     GO TO    F-SUM-WRITE-07.                                     IF1384.2
047300 F-SUM-WRITE-07.                                                  IF1384.2
047400     MOVE "F-SUM-07" TO PAR-NAME.                                 IF1384.2
047500     PERFORM  PRINT-DETAIL.                                       IF1384.2
047600*****************TEST (h) - SIMPLE TEST*****************          IF1384.2
047700 F-SUM-08.                                                        IF1384.2
047800     MOVE ZERO TO WS-NUM.                                         IF1384.2
047900 F-SUM-TEST-08.                                                   IF1384.2
048000     COMPUTE WS-NUM = FUNCTION SUM(IND(P), IND(Q), IND(R)).       IF1384.2
048100     IF WS-NUM = 16 THEN                                          IF1384.2
048200                    PERFORM PASS                                  IF1384.2
048300     ELSE                                                         IF1384.2
048400                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
048500                    MOVE 16 TO CORRECT-N                          IF1384.2
048600                    PERFORM FAIL.                                 IF1384.2
048700     GO TO F-SUM-WRITE-08.                                        IF1384.2
048800 F-SUM-DELETE-08.                                                 IF1384.2
048900     PERFORM  DE-LETE.                                            IF1384.2
049000     GO TO    F-SUM-WRITE-08.                                     IF1384.2
049100 F-SUM-WRITE-08.                                                  IF1384.2
049200     MOVE "F-SUM-08" TO PAR-NAME.                                 IF1384.2
049300     PERFORM  PRINT-DETAIL.                                       IF1384.2
049400*****************TEST (i) - SIMPLE TEST*****************          IF1384.2
049500 F-SUM-09.                                                        IF1384.2
049600     MOVE ZERO TO WS-NUM.                                         IF1384.2
049700 F-SUM-TEST-09.                                                   IF1384.2
049800     COMPUTE WS-NUM = FUNCTION SUM(IND(ALL)).                     IF1384.2
049900     IF WS-NUM = 19 THEN                                          IF1384.2
050000                    PERFORM PASS                                  IF1384.2
050100     ELSE                                                         IF1384.2
050200                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
050300                    MOVE 19 TO CORRECT-N                          IF1384.2
050400                    PERFORM FAIL.                                 IF1384.2
050500     GO TO F-SUM-WRITE-09.                                        IF1384.2
050600 F-SUM-DELETE-09.                                                 IF1384.2
050700     PERFORM  DE-LETE.                                            IF1384.2
050800     GO TO    F-SUM-WRITE-09.                                     IF1384.2
050900 F-SUM-WRITE-09.                                                  IF1384.2
051000     MOVE "F-SUM-09" TO PAR-NAME.                                 IF1384.2
051100     PERFORM  PRINT-DETAIL.                                       IF1384.2
051200*****************TEST (k) - SIMPLE TEST*****************          IF1384.2
051300 F-SUM-11.                                                        IF1384.2
051400     MOVE ZERO TO WS-NUM.                                         IF1384.2
051500 F-SUM-TEST-11.                                                   IF1384.2
051600     COMPUTE WS-NUM = FUNCTION SUM(M, N, O).                      IF1384.2
051700     IF WS-NUM = 540000 THEN                                      IF1384.2
051800                    PERFORM PASS                                  IF1384.2
051900     ELSE                                                         IF1384.2
052000                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
052100                    MOVE 540000 TO CORRECT-N                      IF1384.2
052200                    PERFORM FAIL.                                 IF1384.2
052300     GO TO F-SUM-WRITE-11.                                        IF1384.2
052400 F-SUM-DELETE-11.                                                 IF1384.2
052500     PERFORM  DE-LETE.                                            IF1384.2
052600     GO TO    F-SUM-WRITE-11.                                     IF1384.2
052700 F-SUM-WRITE-11.                                                  IF1384.2
052800     MOVE "F-SUM-11" TO PAR-NAME.                                 IF1384.2
052900     PERFORM  PRINT-DETAIL.                                       IF1384.2
053000*****************TEST (a) - COMPLEX TEST****************          IF1384.2
053100 F-SUM-12.                                                        IF1384.2
053200     MOVE ZERO TO WS-NUM.                                         IF1384.2
053300     MOVE  41.5992 TO MIN-RANGE.                                  IF1384.2
053400     MOVE  41.6008 TO MAX-RANGE.                                  IF1384.2
053500 F-SUM-TEST-12.                                                   IF1384.2
053600     COMPUTE WS-NUM = FUNCTION SUM(2.6 + 30, 4.5 * 2).            IF1384.2
053700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1384.2
053800        (WS-NUM <= MAX-RANGE) THEN                                IF1384.2
053900                    PERFORM PASS                                  IF1384.2
054000     ELSE                                                         IF1384.2
054100                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
054200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1384.2
054300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1384.2
054400                    PERFORM FAIL.                                 IF1384.2
054500     GO TO F-SUM-WRITE-12.                                        IF1384.2
054600 F-SUM-DELETE-12.                                                 IF1384.2
054700     PERFORM  DE-LETE.                                            IF1384.2
054800     GO TO    F-SUM-WRITE-12.                                     IF1384.2
054900 F-SUM-WRITE-12.                                                  IF1384.2
055000     MOVE "F-SUM-12" TO PAR-NAME.                                 IF1384.2
055100     PERFORM  PRINT-DETAIL.                                       IF1384.2
055200*****************TEST (b) - COMPLEX TEST****************          IF1384.2
055300 F-SUM-13.                                                        IF1384.2
055400     MOVE ZERO TO WS-NUM.                                         IF1384.2
055500     MOVE  82.7583 TO MIN-RANGE.                                  IF1384.2
055600     MOVE  82.7616 TO MAX-RANGE.                                  IF1384.2
055700 F-SUM-TEST-13.                                                   IF1384.2
055800     COMPUTE WS-NUM = FUNCTION SUM(E, 9 * A, B / 2).              IF1384.2
055900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1384.2
056000        (WS-NUM <= MAX-RANGE) THEN                                IF1384.2
056100                    PERFORM PASS                                  IF1384.2
056200     ELSE                                                         IF1384.2
056300                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
056400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1384.2
056500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1384.2
056600                    PERFORM FAIL.                                 IF1384.2
056700     GO TO F-SUM-WRITE-13.                                        IF1384.2
056800 F-SUM-DELETE-13.                                                 IF1384.2
056900     PERFORM  DE-LETE.                                            IF1384.2
057000     GO TO    F-SUM-WRITE-13.                                     IF1384.2
057100 F-SUM-WRITE-13.                                                  IF1384.2
057200     MOVE "F-SUM-13" TO PAR-NAME.                                 IF1384.2
057300     PERFORM  PRINT-DETAIL.                                       IF1384.2
057400*****************TEST (c) - COMPLEX TEST****************          IF1384.2
057500 F-SUM-14.                                                        IF1384.2
057600     MOVE ZERO TO WS-NUM.                                         IF1384.2
057700     MOVE 89.9982 TO MIN-RANGE.                                   IF1384.2
057800     MOVE 90.0018 TO MAX-RANGE.                                   IF1384.2
057900 F-SUM-TEST-14.                                                   IF1384.2
058000     COMPUTE WS-NUM = FUNCTION SUM(A, B) + 78.                    IF1384.2
058100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1384.2
058200        (WS-NUM <= MAX-RANGE) THEN                                IF1384.2
058300                    PERFORM PASS                                  IF1384.2
058400     ELSE                                                         IF1384.2
058500                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
058600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1384.2
058700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1384.2
058800                    PERFORM FAIL.                                 IF1384.2
058900     GO TO F-SUM-WRITE-14.                                        IF1384.2
059000 F-SUM-DELETE-14.                                                 IF1384.2
059100     PERFORM  DE-LETE.                                            IF1384.2
059200     GO TO    F-SUM-WRITE-14.                                     IF1384.2
059300 F-SUM-WRITE-14.                                                  IF1384.2
059400     MOVE "F-SUM-14" TO PAR-NAME.                                 IF1384.2
059500     PERFORM  PRINT-DETAIL.                                       IF1384.2
059600*****************TEST (d) - COMPLEX TEST****************          IF1384.2
059700 F-SUM-15.                                                        IF1384.2
059800     MOVE ZERO TO WS-NUM.                                         IF1384.2
059900     MOVE 4.99990 TO MIN-RANGE.                                   IF1384.2
060000     MOVE 5.00010 TO MAX-RANGE.                                   IF1384.2
060100 F-SUM-TEST-15.                                                   IF1384.2
060200     COMPUTE WS-NUM = FUNCTION SUM(A, B) +                        IF1384.2
060300                      FUNCTION SUM(-2.6, -4.4).                   IF1384.2
060400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1384.2
060500        (WS-NUM <= MAX-RANGE) THEN                                IF1384.2
060600                    PERFORM PASS                                  IF1384.2
060700     ELSE                                                         IF1384.2
060800                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
060900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1384.2
061000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1384.2
061100                    PERFORM FAIL.                                 IF1384.2
061200     GO TO F-SUM-WRITE-15.                                        IF1384.2
061300 F-SUM-DELETE-15.                                                 IF1384.2
061400     PERFORM  DE-LETE.                                            IF1384.2
061500     GO TO    F-SUM-WRITE-15.                                     IF1384.2
061600 F-SUM-WRITE-15.                                                  IF1384.2
061700     MOVE "F-SUM-15" TO PAR-NAME.                                 IF1384.2
061800     PERFORM  PRINT-DETAIL.                                       IF1384.2
061900*****************TEST (e) - COMPLEX TEST****************          IF1384.2
062000 F-SUM-16.                                                        IF1384.2
062100     MOVE ZERO TO WS-NUM.                                         IF1384.2
062200     MOVE 3.99992 TO MIN-RANGE.                                   IF1384.2
062300     MOVE 4.00008 TO MAX-RANGE.                                   IF1384.2
062400 F-SUM-TEST-16.                                                   IF1384.2
062500     COMPUTE WS-NUM =                                             IF1384.2
062600               FUNCTION SUM(FUNCTION SUM(6.8, -6.8), 4).          IF1384.2
062700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1384.2
062800        (WS-NUM <= MAX-RANGE) THEN                                IF1384.2
062900                    PERFORM PASS                                  IF1384.2
063000     ELSE                                                         IF1384.2
063100                    MOVE WS-NUM TO COMPUTED-N                     IF1384.2
063200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1384.2
063300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1384.2
063400                    PERFORM FAIL.                                 IF1384.2
063500     GO TO F-SUM-WRITE-16.                                        IF1384.2
063600 F-SUM-DELETE-16.                                                 IF1384.2
063700     PERFORM  DE-LETE.                                            IF1384.2
063800     GO TO    F-SUM-WRITE-16.                                     IF1384.2
063900 F-SUM-WRITE-16.                                                  IF1384.2
064000     MOVE "F-SUM-16" TO PAR-NAME.                                 IF1384.2
064100     PERFORM  PRINT-DETAIL.                                       IF1384.2
064200*****************SPECIAL PERFORM TEST**********************       IF1384.2
064300 F-SUM-17.                                                        IF1384.2
064400     PERFORM F-SUM-TEST-17                                        IF1384.2
064500       UNTIL FUNCTION SUM(ARG1, 1) > 10.                          IF1384.2
064600     PERFORM PASS.                                                IF1384.2
064700     GO TO F-SUM-WRITE-17.                                        IF1384.2
064800 F-SUM-TEST-17.                                                   IF1384.2
064900     COMPUTE ARG1 = ARG1 + 1.                                     IF1384.2
065000 F-SUM-DELETE-17.                                                 IF1384.2
065100     PERFORM  DE-LETE.                                            IF1384.2
065200     GO TO    F-SUM-WRITE-17.                                     IF1384.2
065300 F-SUM-WRITE-17.                                                  IF1384.2
065400     MOVE "F-SUM-17" TO PAR-NAME.                                 IF1384.2
065500     PERFORM  PRINT-DETAIL.                                       IF1384.2
065600********************END OF TESTS***************                   IF1384.2
065700 CCVS-EXIT SECTION.                                               IF1384.2
065800 CCVS-999999.                                                     IF1384.2
065900     GO TO CLOSE-FILES.                                           IF1384.2
      *END-OF,IF138A                                                                  
