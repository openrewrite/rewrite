      *HEADER,COBOL,IF120A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1204.2
000200 PROGRAM-ID.                                                      IF1204.2
000300     IF120A.                                                      IF1204.2
000400                                                                  IF1204.2
000500***********************************************************       IF1204.2
000600*                                                         *       IF1204.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1204.2
000800* It contains tests for the Intrinsic Function MEAN.      *       IF1204.2
000900*                                                         *       IF1204.2
001000***********************************************************       IF1204.2
001100 ENVIRONMENT DIVISION.                                            IF1204.2
001200 CONFIGURATION SECTION.                                           IF1204.2
001300 SOURCE-COMPUTER.                                                 IF1204.2
001400     XXXXX082.                                                    IF1204.2
001500 OBJECT-COMPUTER.                                                 IF1204.2
001600     XXXXX083.                                                    IF1204.2
001700 INPUT-OUTPUT SECTION.                                            IF1204.2
001800 FILE-CONTROL.                                                    IF1204.2
001900     SELECT PRINT-FILE ASSIGN TO                                  IF1204.2
002000     XXXXX055.                                                    IF1204.2
002100 DATA DIVISION.                                                   IF1204.2
002200 FILE SECTION.                                                    IF1204.2
002300 FD  PRINT-FILE.                                                  IF1204.2
002400 01  PRINT-REC PICTURE X(120).                                    IF1204.2
002500 01  DUMMY-RECORD PICTURE X(120).                                 IF1204.2
002600 WORKING-STORAGE SECTION.                                         IF1204.2
002700***********************************************************       IF1204.2
002800* Variables specific to the Intrinsic Function Test IF120A*       IF1204.2
002900***********************************************************       IF1204.2
003000 01  A                   PIC S9(10)          VALUE 5.             IF1204.2
003100 01  B                   PIC S9(10)          VALUE 7.             IF1204.2
003200 01  C                   PIC S9(10)          VALUE -4.            IF1204.2
003300 01  D                   PIC S9(10)          VALUE 10.            IF1204.2
003400 01  E                   PIC S9(5)V9(5)      VALUE 34.26.         IF1204.2
003500 01  F                   PIC S9(5)V9(5)      VALUE -8.32.         IF1204.2
003600 01  G                   PIC S9(5)V9(5)      VALUE 4.08.          IF1204.2
003700 01  H                   PIC S9(5)V9(5)      VALUE -5.3.          IF1204.2
003800 01  M                   PIC S9(10)          VALUE 320000.        IF1204.2
003900 01  N                   PIC S9(10)          VALUE 650000.        IF1204.2
004000 01  O                   PIC S9(10)          VALUE -430000.       IF1204.2
004100 01  P                   PIC S9(10)          VALUE 1.             IF1204.2
004200 01  Q                   PIC S9(10)          VALUE 3.             IF1204.2
004300 01  R                   PIC S9(10)          VALUE 5.             IF1204.2
004400 01  ARG1                PIC S9(10)          VALUE 1.             IF1204.2
004500 01  ARG2                PIC S9(10)          VALUE 1.             IF1204.2
004600 01  ARR                                     VALUE "40537".       IF1204.2
004700     02  IND OCCURS 5 TIMES PIC 9.                                IF1204.2
004800 01  TEMP                PIC S9(10)V9(5).                         IF1204.2
004900 01  WS-NUM              PIC S9(6)V9(6).                          IF1204.2
005000 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1204.2
005100 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1204.2
005200*                                                                 IF1204.2
005300**********************************************************        IF1204.2
005400*                                                                 IF1204.2
005500 01  TEST-RESULTS.                                                IF1204.2
005600     02 FILLER                   PIC X      VALUE SPACE.          IF1204.2
005700     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1204.2
005800     02 FILLER                   PIC X      VALUE SPACE.          IF1204.2
005900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1204.2
006000     02 FILLER                   PIC X      VALUE SPACE.          IF1204.2
006100     02  PAR-NAME.                                                IF1204.2
006200       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1204.2
006300       03  PARDOT-X              PIC X      VALUE SPACE.          IF1204.2
006400       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1204.2
006500     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1204.2
006600     02 RE-MARK                  PIC X(61).                       IF1204.2
006700 01  TEST-COMPUTED.                                               IF1204.2
006800     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1204.2
006900     02 FILLER                   PIC X(17)  VALUE                 IF1204.2
007000            "       COMPUTED=".                                   IF1204.2
007100     02 COMPUTED-X.                                               IF1204.2
007200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1204.2
007300     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1204.2
007400                                 PIC -9(9).9(9).                  IF1204.2
007500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1204.2
007600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1204.2
007700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1204.2
007800     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1204.2
007900         04 COMPUTED-18V0                    PIC -9(18).          IF1204.2
008000         04 FILLER                           PIC X.               IF1204.2
008100     03 FILLER PIC X(50) VALUE SPACE.                             IF1204.2
008200 01  TEST-CORRECT.                                                IF1204.2
008300     02 FILLER PIC X(30) VALUE SPACE.                             IF1204.2
008400     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1204.2
008500     02 CORRECT-X.                                                IF1204.2
008600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1204.2
008700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1204.2
008800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1204.2
008900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1204.2
009000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1204.2
009100     03      CR-18V0 REDEFINES CORRECT-A.                         IF1204.2
009200         04 CORRECT-18V0                     PIC -9(18).          IF1204.2
009300         04 FILLER                           PIC X.               IF1204.2
009400     03 FILLER PIC X(2) VALUE SPACE.                              IF1204.2
009500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1204.2
009600 01  TEST-CORRECT-MIN.                                            IF1204.2
009700     02 FILLER PIC X(30) VALUE SPACE.                             IF1204.2
009800     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1204.2
009900     02 CORRECTMI-X.                                              IF1204.2
010000     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1204.2
010100     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1204.2
010200     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1204.2
010300     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1204.2
010400     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1204.2
010500     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1204.2
010600         04 CORRECTMI-18V0                     PIC -9(18).        IF1204.2
010700         04 FILLER                           PIC X.               IF1204.2
010800     03 FILLER PIC X(2) VALUE SPACE.                              IF1204.2
010900     03 FILLER                           PIC X(48) VALUE SPACE.   IF1204.2
011000 01  TEST-CORRECT-MAX.                                            IF1204.2
011100     02 FILLER PIC X(30) VALUE SPACE.                             IF1204.2
011200     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1204.2
011300     02 CORRECTMA-X.                                              IF1204.2
011400     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1204.2
011500     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1204.2
011600     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1204.2
011700     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1204.2
011800     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1204.2
011900     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1204.2
012000         04 CORRECTMA-18V0                     PIC -9(18).        IF1204.2
012100         04 FILLER                           PIC X.               IF1204.2
012200     03 FILLER PIC X(2) VALUE SPACE.                              IF1204.2
012300     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1204.2
012400 01  CCVS-C-1.                                                    IF1204.2
012500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1204.2
012600-    "SS  PARAGRAPH-NAME                                          IF1204.2
012700-    "       REMARKS".                                            IF1204.2
012800     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1204.2
012900 01  CCVS-C-2.                                                    IF1204.2
013000     02 FILLER                     PIC X        VALUE SPACE.      IF1204.2
013100     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1204.2
013200     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1204.2
013300     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1204.2
013400     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1204.2
013500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1204.2
013600 01  REC-CT                        PIC 99       VALUE ZERO.       IF1204.2
013700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1204.2
013800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1204.2
013900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1204.2
014000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1204.2
014100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1204.2
014200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1204.2
014300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1204.2
014400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1204.2
014500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1204.2
014600 01  CCVS-H-1.                                                    IF1204.2
014700     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1204.2
014800     02  FILLER                    PIC X(42)    VALUE             IF1204.2
014900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1204.2
015000     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1204.2
015100 01  CCVS-H-2A.                                                   IF1204.2
015200   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1204.2
015300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1204.2
015400   02  FILLER                        PIC XXXX   VALUE             IF1204.2
015500     "4.2 ".                                                      IF1204.2
015600   02  FILLER                        PIC X(28)  VALUE             IF1204.2
015700            " COPY - NOT FOR DISTRIBUTION".                       IF1204.2
015800   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1204.2
015900                                                                  IF1204.2
016000 01  CCVS-H-2B.                                                   IF1204.2
016100   02  FILLER                        PIC X(15)  VALUE             IF1204.2
016200            "TEST RESULT OF ".                                    IF1204.2
016300   02  TEST-ID                       PIC X(9).                    IF1204.2
016400   02  FILLER                        PIC X(4)   VALUE             IF1204.2
016500            " IN ".                                               IF1204.2
016600   02  FILLER                        PIC X(12)  VALUE             IF1204.2
016700     " HIGH       ".                                              IF1204.2
016800   02  FILLER                        PIC X(22)  VALUE             IF1204.2
016900            " LEVEL VALIDATION FOR ".                             IF1204.2
017000   02  FILLER                        PIC X(58)  VALUE             IF1204.2
017100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1204.2
017200 01  CCVS-H-3.                                                    IF1204.2
017300     02  FILLER                      PIC X(34)  VALUE             IF1204.2
017400            " FOR OFFICIAL USE ONLY    ".                         IF1204.2
017500     02  FILLER                      PIC X(58)  VALUE             IF1204.2
017600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1204.2
017700     02  FILLER                      PIC X(28)  VALUE             IF1204.2
017800            "  COPYRIGHT   1985 ".                                IF1204.2
017900 01  CCVS-E-1.                                                    IF1204.2
018000     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1204.2
018100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1204.2
018200     02 ID-AGAIN                     PIC X(9).                    IF1204.2
018300     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1204.2
018400 01  CCVS-E-2.                                                    IF1204.2
018500     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1204.2
018600     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1204.2
018700     02 CCVS-E-2-2.                                               IF1204.2
018800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1204.2
018900         03 FILLER                   PIC X      VALUE SPACE.      IF1204.2
019000         03 ENDER-DESC               PIC X(44)  VALUE             IF1204.2
019100            "ERRORS ENCOUNTERED".                                 IF1204.2
019200 01  CCVS-E-3.                                                    IF1204.2
019300     02  FILLER                      PIC X(22)  VALUE             IF1204.2
019400            " FOR OFFICIAL USE ONLY".                             IF1204.2
019500     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1204.2
019600     02  FILLER                      PIC X(58)  VALUE             IF1204.2
019700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1204.2
019800     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1204.2
019900     02 FILLER                       PIC X(15)  VALUE             IF1204.2
020000             " COPYRIGHT 1985".                                   IF1204.2
020100 01  CCVS-E-4.                                                    IF1204.2
020200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1204.2
020300     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1204.2
020400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1204.2
020500     02 FILLER                       PIC X(40)  VALUE             IF1204.2
020600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1204.2
020700 01  XXINFO.                                                      IF1204.2
020800     02 FILLER                       PIC X(19)  VALUE             IF1204.2
020900            "*** INFORMATION ***".                                IF1204.2
021000     02 INFO-TEXT.                                                IF1204.2
021100       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1204.2
021200       04 XXCOMPUTED                 PIC X(20).                   IF1204.2
021300       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1204.2
021400       04 XXCORRECT                  PIC X(20).                   IF1204.2
021500     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1204.2
021600 01  HYPHEN-LINE.                                                 IF1204.2
021700     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1204.2
021800     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1204.2
021900-    "*****************************************".                 IF1204.2
022000     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1204.2
022100-    "******************************".                            IF1204.2
022200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1204.2
022300     "IF120A".                                                    IF1204.2
022400 PROCEDURE DIVISION.                                              IF1204.2
022500 CCVS1 SECTION.                                                   IF1204.2
022600 OPEN-FILES.                                                      IF1204.2
022700     OPEN     OUTPUT PRINT-FILE.                                  IF1204.2
022800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1204.2
022900     MOVE    SPACE TO TEST-RESULTS.                               IF1204.2
023000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1204.2
023100     GO TO CCVS1-EXIT.                                            IF1204.2
023200 CLOSE-FILES.                                                     IF1204.2
023300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1204.2
023400 TERMINATE-CCVS.                                                  IF1204.2
023500     STOP     RUN.                                                IF1204.2
023600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1204.2
023700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1204.2
023800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1204.2
023900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1204.2
024000     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1204.2
024100 PRINT-DETAIL.                                                    IF1204.2
024200     IF REC-CT NOT EQUAL TO ZERO                                  IF1204.2
024300             MOVE "." TO PARDOT-X                                 IF1204.2
024400             MOVE REC-CT TO DOTVALUE.                             IF1204.2
024500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1204.2
024600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1204.2
024700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1204.2
024800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1204.2
024900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1204.2
025000     MOVE SPACE TO CORRECT-X.                                     IF1204.2
025100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1204.2
025200     MOVE     SPACE TO RE-MARK.                                   IF1204.2
025300 HEAD-ROUTINE.                                                    IF1204.2
025400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1204.2
025500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1204.2
025600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1204.2
025700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1204.2
025800 COLUMN-NAMES-ROUTINE.                                            IF1204.2
025900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1204.2
026000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1204.2
026100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1204.2
026200 END-ROUTINE.                                                     IF1204.2
026300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1204.2
026400 END-RTN-EXIT.                                                    IF1204.2
026500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1204.2
026600 END-ROUTINE-1.                                                   IF1204.2
026700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1204.2
026800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1204.2
026900      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1204.2
027000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1204.2
027100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1204.2
027200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1204.2
027300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1204.2
027400  END-ROUTINE-12.                                                 IF1204.2
027500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1204.2
027600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1204.2
027700         MOVE "NO " TO ERROR-TOTAL                                IF1204.2
027800         ELSE                                                     IF1204.2
027900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1204.2
028000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1204.2
028100     PERFORM WRITE-LINE.                                          IF1204.2
028200 END-ROUTINE-13.                                                  IF1204.2
028300     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1204.2
028400         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1204.2
028500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1204.2
028600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1204.2
028700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1204.2
028800      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1204.2
028900          MOVE "NO " TO ERROR-TOTAL                               IF1204.2
029000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1204.2
029100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1204.2
029200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1204.2
029300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1204.2
029400 WRITE-LINE.                                                      IF1204.2
029500     ADD 1 TO RECORD-COUNT.                                       IF1204.2
029600*    IF RECORD-COUNT GREATER 42                                   IF1204.2
029700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1204.2
029800*        MOVE SPACE TO DUMMY-RECORD                               IF1204.2
029900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1204.2
030000*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1204.2
030100*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1204.2
030200*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1204.2
030300*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1204.2
030400*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1204.2
030500*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1204.2
030600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1204.2
030700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1204.2
030800*        MOVE ZERO TO RECORD-COUNT.                               IF1204.2
030900     PERFORM WRT-LN.                                              IF1204.2
031000 WRT-LN.                                                          IF1204.2
031100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1204.2
031200     MOVE SPACE TO DUMMY-RECORD.                                  IF1204.2
031300 BLANK-LINE-PRINT.                                                IF1204.2
031400     PERFORM WRT-LN.                                              IF1204.2
031500 FAIL-ROUTINE.                                                    IF1204.2
031600     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1204.2
031700            GO TO FAIL-ROUTINE-WRITE.                             IF1204.2
031800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1204.2
031900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1204.2
032000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1204.2
032100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1204.2
032200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1204.2
032300     GO TO  FAIL-ROUTINE-EX.                                      IF1204.2
032400 FAIL-ROUTINE-WRITE.                                              IF1204.2
032500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1204.2
032600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1204.2
032700                              CORMA-ANSI-REFERENCE.               IF1204.2
032800     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1204.2
032900           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1204.2
033000           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1204.2
033100     ELSE                                                         IF1204.2
033200           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1204.2
033300     PERFORM WRITE-LINE.                                          IF1204.2
033400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1204.2
033500 FAIL-ROUTINE-EX. EXIT.                                           IF1204.2
033600 BAIL-OUT.                                                        IF1204.2
033700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1204.2
033800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1204.2
033900 BAIL-OUT-WRITE.                                                  IF1204.2
034000     MOVE CORRECT-A TO XXCORRECT.                                 IF1204.2
034100     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1204.2
034200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1204.2
034300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1204.2
034400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1204.2
034500 BAIL-OUT-EX. EXIT.                                               IF1204.2
034600 CCVS1-EXIT.                                                      IF1204.2
034700     EXIT.                                                        IF1204.2
034800********************************************************          IF1204.2
034900*                                                      *          IF1204.2
035000*    Intrinsic Function Tests         IF120A - MEAN    *          IF1204.2
035100*                                                      *          IF1204.2
035200********************************************************          IF1204.2
035300 SECT-IF120A SECTION.                                             IF1204.2
035400 F-MEAN-TEST-INFO.                                                IF1204.2
035500     MOVE     "See ref. A-53 2.24" TO ANSI-REFERENCE.             IF1204.2
035600     MOVE     "MEAN Function" TO FEATURE.                         IF1204.2
035700*****************TEST (a) - SIMPLE TEST*****************          IF1204.2
035800 F-MEAN-01.                                                       IF1204.2
035900     MOVE ZERO TO WS-NUM.                                         IF1204.2
036000 F-MEAN-TEST-01.                                                  IF1204.2
036100     COMPUTE WS-NUM = FUNCTION MEAN(5, -2, -14, 0).               IF1204.2
036200     IF (WS-NUM >= -2.75006) AND                                  IF1204.2
036300        (WS-NUM <= -2.74995)                                      IF1204.2
036400                    PERFORM PASS                                  IF1204.2
036500     ELSE                                                         IF1204.2
036600                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
036700                    MOVE -2.75 TO CORRECT-N                       IF1204.2
036800                    PERFORM FAIL.                                 IF1204.2
036900     GO TO F-MEAN-WRITE-01.                                       IF1204.2
037000 F-MEAN-DELETE-01.                                                IF1204.2
037100     PERFORM  DE-LETE.                                            IF1204.2
037200     GO TO    F-MEAN-WRITE-01.                                    IF1204.2
037300 F-MEAN-WRITE-01.                                                 IF1204.2
037400     MOVE "F-MEAN-01" TO PAR-NAME.                                IF1204.2
037500     PERFORM  PRINT-DETAIL.                                       IF1204.2
037600*****************TEST (b) - SIMPLE TEST*****************          IF1204.2
037700 F-MEAN-02.                                                       IF1204.2
037800     EVALUATE FUNCTION MEAN(3.9, -0.3, 8.7, 100.2)                IF1204.2
037900     WHEN 28.1244 THRU 28.1256                                    IF1204.2
038000                    PERFORM PASS                                  IF1204.2
038100     WHEN OTHER                                                   IF1204.2
038200                    PERFORM FAIL.                                 IF1204.2
038300     GO TO F-MEAN-WRITE-02.                                       IF1204.2
038400 F-MEAN-DELETE-02.                                                IF1204.2
038500     PERFORM  DE-LETE.                                            IF1204.2
038600     GO TO    F-MEAN-WRITE-02.                                    IF1204.2
038700 F-MEAN-WRITE-02.                                                 IF1204.2
038800     MOVE "F-MEAN-02" TO PAR-NAME.                                IF1204.2
038900     PERFORM  PRINT-DETAIL.                                       IF1204.2
039000*****************TEST (c) - SIMPLE TEST*****************          IF1204.2
039100 F-MEAN-03.                                                       IF1204.2
039200     IF (FUNCTION MEAN(A, B, C, D) >= 4.49991) AND                IF1204.2
039300        (FUNCTION MEAN(A, B, C, D) <= 4.50009)                    IF1204.2
039400                    PERFORM PASS                                  IF1204.2
039500     ELSE                                                         IF1204.2
039600                    PERFORM FAIL.                                 IF1204.2
039700     GO TO F-MEAN-WRITE-03.                                       IF1204.2
039800 F-MEAN-DELETE-03.                                                IF1204.2
039900     PERFORM  DE-LETE.                                            IF1204.2
040000     GO TO    F-MEAN-WRITE-03.                                    IF1204.2
040100 F-MEAN-WRITE-03.                                                 IF1204.2
040200     MOVE "F-MEAN-03" TO PAR-NAME.                                IF1204.2
040300     PERFORM  PRINT-DETAIL.                                       IF1204.2
040400*****************TEST (d) - SIMPLE TEST*****************          IF1204.2
040500 F-MEAN-04.                                                       IF1204.2
040600     MOVE ZERO TO WS-NUM.                                         IF1204.2
040700 F-MEAN-TEST-04.                                                  IF1204.2
040800     COMPUTE WS-NUM = FUNCTION MEAN(E, F, G, H).                  IF1204.2
040900     IF (WS-NUM >= 6.17988) AND                                   IF1204.2
041000        (WS-NUM <= 6.18012)                                       IF1204.2
041100                    PERFORM PASS                                  IF1204.2
041200     ELSE                                                         IF1204.2
041300                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
041400                    MOVE 6.18 TO CORRECT-N                        IF1204.2
041500                    PERFORM FAIL.                                 IF1204.2
041600     GO TO F-MEAN-WRITE-04.                                       IF1204.2
041700 F-MEAN-DELETE-04.                                                IF1204.2
041800     PERFORM  DE-LETE.                                            IF1204.2
041900     GO TO    F-MEAN-WRITE-04.                                    IF1204.2
042000 F-MEAN-WRITE-04.                                                 IF1204.2
042100     MOVE "F-MEAN-04" TO PAR-NAME.                                IF1204.2
042200     PERFORM  PRINT-DETAIL.                                       IF1204.2
042300*****************TEST (e) - SIMPLE TEST*****************          IF1204.2
042400 F-MEAN-05.                                                       IF1204.2
042500     MOVE ZERO TO WS-NUM.                                         IF1204.2
042600 F-MEAN-TEST-05.                                                  IF1204.2
042700     COMPUTE WS-NUM = FUNCTION MEAN(10.2, -0.2, 5.6, -15.6).      IF1204.2
042800     IF (WS-NUM >= -0.000020) AND                                 IF1204.2
042900        (WS-NUM <= 0.000020)                                      IF1204.2
043000                    PERFORM PASS                                  IF1204.2
043100     ELSE                                                         IF1204.2
043200                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
043300                    MOVE 0.0 TO CORRECT-N                         IF1204.2
043400                    PERFORM FAIL.                                 IF1204.2
043500     GO TO F-MEAN-WRITE-05.                                       IF1204.2
043600 F-MEAN-DELETE-05.                                                IF1204.2
043700     PERFORM  DE-LETE.                                            IF1204.2
043800     GO TO    F-MEAN-WRITE-05.                                    IF1204.2
043900 F-MEAN-WRITE-05.                                                 IF1204.2
044000     MOVE "F-MEAN-05" TO PAR-NAME.                                IF1204.2
044100     PERFORM  PRINT-DETAIL.                                       IF1204.2
044200*****************TEST (f) - SIMPLE TEST*****************          IF1204.2
044300 F-MEAN-06.                                                       IF1204.2
044400     MOVE ZERO TO WS-NUM.                                         IF1204.2
044500 F-MEAN-TEST-06.                                                  IF1204.2
044600     COMPUTE WS-NUM = FUNCTION MEAN(A, B, C, D, E, F, G, H).      IF1204.2
044700     IF (WS-NUM >= 5.33989) AND                                   IF1204.2
044800        (WS-NUM <= 5.34011)                                       IF1204.2
044900                    PERFORM PASS                                  IF1204.2
045000     ELSE                                                         IF1204.2
045100                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
045200                    MOVE 5.34 TO CORRECT-N                        IF1204.2
045300                    PERFORM FAIL.                                 IF1204.2
045400     GO TO F-MEAN-WRITE-06.                                       IF1204.2
045500 F-MEAN-DELETE-06.                                                IF1204.2
045600     PERFORM  DE-LETE.                                            IF1204.2
045700     GO TO    F-MEAN-WRITE-06.                                    IF1204.2
045800 F-MEAN-WRITE-06.                                                 IF1204.2
045900     MOVE "F-MEAN-06" TO PAR-NAME.                                IF1204.2
046000     PERFORM  PRINT-DETAIL.                                       IF1204.2
046100*****************TEST (g) - SIMPLE TEST*****************          IF1204.2
046200 F-MEAN-07.                                                       IF1204.2
046300     MOVE ZERO TO WS-NUM.                                         IF1204.2
046400 F-MEAN-TEST-07.                                                  IF1204.2
046500     COMPUTE WS-NUM = FUNCTION MEAN(IND(2), IND(1), IND(3)).      IF1204.2
046600     IF (WS-NUM >= 2.99994) AND                                   IF1204.2
046700        (WS-NUM <= 3.00006)                                       IF1204.2
046800                    PERFORM PASS                                  IF1204.2
046900     ELSE                                                         IF1204.2
047000                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
047100                    MOVE 3.0 TO CORRECT-N                         IF1204.2
047200                    PERFORM FAIL.                                 IF1204.2
047300     GO TO F-MEAN-WRITE-07.                                       IF1204.2
047400 F-MEAN-DELETE-07.                                                IF1204.2
047500     PERFORM  DE-LETE.                                            IF1204.2
047600     GO TO    F-MEAN-WRITE-07.                                    IF1204.2
047700 F-MEAN-WRITE-07.                                                 IF1204.2
047800     MOVE "F-MEAN-07" TO PAR-NAME.                                IF1204.2
047900     PERFORM  PRINT-DETAIL.                                       IF1204.2
048000*****************TEST (h) - SIMPLE TEST*****************          IF1204.2
048100 F-MEAN-08.                                                       IF1204.2
048200     MOVE ZERO TO WS-NUM.                                         IF1204.2
048300 F-MEAN-TEST-08.                                                  IF1204.2
048400     COMPUTE WS-NUM = FUNCTION MEAN(IND(P), IND(Q), IND(R)).      IF1204.2
048500     IF (WS-NUM >= 5.33323) AND                                   IF1204.2
048600        (WS-NUM <= 5.33344)                                       IF1204.2
048700                    PERFORM PASS                                  IF1204.2
048800     ELSE                                                         IF1204.2
048900                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
049000                    MOVE 5.333 TO CORRECT-N                       IF1204.2
049100                    PERFORM FAIL.                                 IF1204.2
049200     GO TO F-MEAN-WRITE-08.                                       IF1204.2
049300 F-MEAN-DELETE-08.                                                IF1204.2
049400     PERFORM  DE-LETE.                                            IF1204.2
049500     GO TO    F-MEAN-WRITE-08.                                    IF1204.2
049600 F-MEAN-WRITE-08.                                                 IF1204.2
049700     MOVE "F-MEAN-08" TO PAR-NAME.                                IF1204.2
049800     PERFORM  PRINT-DETAIL.                                       IF1204.2
049900*****************TEST (i) - SIMPLE TEST*****************          IF1204.2
050000 F-MEAN-09.                                                       IF1204.2
050100     MOVE ZERO TO WS-NUM.                                         IF1204.2
050200 F-MEAN-TEST-09.                                                  IF1204.2
050300     COMPUTE WS-NUM = FUNCTION MEAN(IND(ALL)).                    IF1204.2
050400     IF (WS-NUM >= 3.79992) AND                                   IF1204.2
050500        (WS-NUM <= 3.80008)                                       IF1204.2
050600                    PERFORM PASS                                  IF1204.2
050700     ELSE                                                         IF1204.2
050800                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
050900                    MOVE 3.8 TO CORRECT-N                         IF1204.2
051000                    PERFORM FAIL.                                 IF1204.2
051100     GO TO F-MEAN-WRITE-09.                                       IF1204.2
051200 F-MEAN-DELETE-09.                                                IF1204.2
051300     PERFORM  DE-LETE.                                            IF1204.2
051400     GO TO    F-MEAN-WRITE-09.                                    IF1204.2
051500 F-MEAN-WRITE-09.                                                 IF1204.2
051600     MOVE "F-MEAN-09" TO PAR-NAME.                                IF1204.2
051700     PERFORM  PRINT-DETAIL.                                       IF1204.2
051800*****************TEST (k) - SIMPLE TEST*****************          IF1204.2
051900 F-MEAN-11.                                                       IF1204.2
052000     MOVE ZERO TO WS-NUM.                                         IF1204.2
052100 F-MEAN-TEST-11.                                                  IF1204.2
052200     COMPUTE WS-NUM = FUNCTION MEAN(M, N, O).                     IF1204.2
052300     IF WS-NUM = 180000  THEN                                     IF1204.2
052400                    PERFORM PASS                                  IF1204.2
052500     ELSE                                                         IF1204.2
052600                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
052700                    MOVE 180000  TO CORRECT-N                     IF1204.2
052800                    PERFORM FAIL.                                 IF1204.2
052900     GO TO F-MEAN-WRITE-11.                                       IF1204.2
053000 F-MEAN-DELETE-11.                                                IF1204.2
053100     PERFORM  DE-LETE.                                            IF1204.2
053200     GO TO    F-MEAN-WRITE-11.                                    IF1204.2
053300 F-MEAN-WRITE-11.                                                 IF1204.2
053400     MOVE "F-MEAN-11" TO PAR-NAME.                                IF1204.2
053500     PERFORM  PRINT-DETAIL.                                       IF1204.2
053600*****************TEST (l) - SIMPLE TEST*****************          IF1204.2
053700 F-MEAN-12.                                                       IF1204.2
053800     MOVE ZERO TO WS-NUM.                                         IF1204.2
053900 F-MEAN-TEST-12.                                                  IF1204.2
054000     COMPUTE WS-NUM = FUNCTION MEAN(A, 5, A).                     IF1204.2
054100     IF WS-NUM = 5    THEN                                        IF1204.2
054200                    PERFORM PASS                                  IF1204.2
054300     ELSE                                                         IF1204.2
054400                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
054500                    MOVE 5    TO CORRECT-N                        IF1204.2
054600                    PERFORM FAIL.                                 IF1204.2
054700     GO TO F-MEAN-WRITE-12.                                       IF1204.2
054800 F-MEAN-DELETE-12.                                                IF1204.2
054900     PERFORM  DE-LETE.                                            IF1204.2
055000     GO TO    F-MEAN-WRITE-12.                                    IF1204.2
055100 F-MEAN-WRITE-12.                                                 IF1204.2
055200     MOVE "F-MEAN-12" TO PAR-NAME.                                IF1204.2
055300     PERFORM  PRINT-DETAIL.                                       IF1204.2
055400*****************TEST (a) - COMPLEX TEST****************          IF1204.2
055500 F-MEAN-13.                                                       IF1204.2
055600     MOVE ZERO TO WS-NUM.                                         IF1204.2
055700     MOVE  20.6896 TO MIN-RANGE.                                  IF1204.2
055800     MOVE  20.6904 TO MAX-RANGE.                                  IF1204.2
055900 F-MEAN-TEST-13.                                                  IF1204.2
056000     COMPUTE WS-NUM = FUNCTION MEAN(E, 9 * A, 0, B / 2).          IF1204.2
056100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1204.2
056200        (WS-NUM <= MAX-RANGE) THEN                                IF1204.2
056300                    PERFORM PASS                                  IF1204.2
056400     ELSE                                                         IF1204.2
056500                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
056600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1204.2
056700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1204.2
056800                    PERFORM FAIL.                                 IF1204.2
056900     GO TO F-MEAN-WRITE-13.                                       IF1204.2
057000 F-MEAN-DELETE-13.                                                IF1204.2
057100     PERFORM  DE-LETE.                                            IF1204.2
057200     GO TO    F-MEAN-WRITE-13.                                    IF1204.2
057300 F-MEAN-WRITE-13.                                                 IF1204.2
057400     MOVE "F-MEAN-13" TO PAR-NAME.                                IF1204.2
057500     PERFORM  PRINT-DETAIL.                                       IF1204.2
057600*****************TEST (b) - COMPLEX TEST****************          IF1204.2
057700 F-MEAN-14.                                                       IF1204.2
057800     MOVE ZERO TO WS-NUM.                                         IF1204.2
057900     MOVE 83.9983 TO MIN-RANGE.                                   IF1204.2
058000     MOVE 84.0017 TO MAX-RANGE.                                   IF1204.2
058100 F-MEAN-TEST-14.                                                  IF1204.2
058200     COMPUTE WS-NUM = FUNCTION MEAN(A, B) + 78.                   IF1204.2
058300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1204.2
058400        (WS-NUM <= MAX-RANGE) THEN                                IF1204.2
058500                    PERFORM PASS                                  IF1204.2
058600     ELSE                                                         IF1204.2
058700                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
058800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1204.2
058900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1204.2
059000                    PERFORM FAIL.                                 IF1204.2
059100     GO TO F-MEAN-WRITE-14.                                       IF1204.2
059200 F-MEAN-DELETE-14.                                                IF1204.2
059300     PERFORM  DE-LETE.                                            IF1204.2
059400     GO TO    F-MEAN-WRITE-14.                                    IF1204.2
059500 F-MEAN-WRITE-14.                                                 IF1204.2
059600     MOVE "F-MEAN-14" TO PAR-NAME.                                IF1204.2
059700     PERFORM  PRINT-DETAIL.                                       IF1204.2
059800*****************TEST (c) - COMPLEX TEST****************          IF1204.2
059900 F-MEAN-15.                                                       IF1204.2
060000     MOVE ZERO TO WS-NUM.                                         IF1204.2
060100     MOVE 2.49995 TO MIN-RANGE.                                   IF1204.2
060200     MOVE 2.50005 TO MAX-RANGE.                                   IF1204.2
060300 F-MEAN-TEST-15.                                                  IF1204.2
060400     COMPUTE WS-NUM = FUNCTION MEAN(A , B) +                      IF1204.2
060500                      FUNCTION MEAN(-2.6, -4.4).                  IF1204.2
060600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1204.2
060700        (WS-NUM <= MAX-RANGE) THEN                                IF1204.2
060800                    PERFORM PASS                                  IF1204.2
060900     ELSE                                                         IF1204.2
061000                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
061100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1204.2
061200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1204.2
061300                    PERFORM FAIL.                                 IF1204.2
061400     GO TO F-MEAN-WRITE-15.                                       IF1204.2
061500 F-MEAN-DELETE-15.                                                IF1204.2
061600     PERFORM  DE-LETE.                                            IF1204.2
061700     GO TO    F-MEAN-WRITE-15.                                    IF1204.2
061800 F-MEAN-WRITE-15.                                                 IF1204.2
061900     MOVE "F-MEAN-15" TO PAR-NAME.                                IF1204.2
062000     PERFORM  PRINT-DETAIL.                                       IF1204.2
062100*****************TEST (d) - COMPLEX TEST****************          IF1204.2
062200 F-MEAN-16.                                                       IF1204.2
062300     MOVE ZERO TO WS-NUM.                                         IF1204.2
062400     MOVE 4.49991 TO MIN-RANGE.                                   IF1204.2
062500     MOVE 4.50009 TO MAX-RANGE.                                   IF1204.2
062600 F-MEAN-TEST-16.                                                  IF1204.2
062700     COMPUTE WS-NUM = FUNCTION MEAN(FUNCTION MEAN(4, 2), 6).      IF1204.2
062800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1204.2
062900        (WS-NUM <= MAX-RANGE) THEN                                IF1204.2
063000                    PERFORM PASS                                  IF1204.2
063100     ELSE                                                         IF1204.2
063200                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
063300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1204.2
063400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1204.2
063500                    PERFORM FAIL.                                 IF1204.2
063600     GO TO F-MEAN-WRITE-16.                                       IF1204.2
063700 F-MEAN-DELETE-16.                                                IF1204.2
063800     PERFORM  DE-LETE.                                            IF1204.2
063900     GO TO    F-MEAN-WRITE-16.                                    IF1204.2
064000 F-MEAN-WRITE-16.                                                 IF1204.2
064100     MOVE "F-MEAN-16" TO PAR-NAME.                                IF1204.2
064200     PERFORM  PRINT-DETAIL.                                       IF1204.2
064300*****************TEST (e) - COMPLEX TEST****************          IF1204.2
064400 F-MEAN-17.                                                       IF1204.2
064500     MOVE ZERO TO WS-NUM.                                         IF1204.2
064600     MOVE  20.7996 TO MIN-RANGE.                                  IF1204.2
064700     MOVE  20.8004 TO MAX-RANGE.                                  IF1204.2
064800 F-MEAN-TEST-17.                                                  IF1204.2
064900     COMPUTE WS-NUM = FUNCTION MEAN(2.6 + 30, 4.5 * 2).           IF1204.2
065000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1204.2
065100        (WS-NUM <= MAX-RANGE) THEN                                IF1204.2
065200                    PERFORM PASS                                  IF1204.2
065300     ELSE                                                         IF1204.2
065400                    MOVE WS-NUM TO COMPUTED-N                     IF1204.2
065500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1204.2
065600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1204.2
065700                    PERFORM FAIL.                                 IF1204.2
065800     GO TO F-MEAN-WRITE-17.                                       IF1204.2
065900 F-MEAN-DELETE-17.                                                IF1204.2
066000     PERFORM  DE-LETE.                                            IF1204.2
066100     GO TO    F-MEAN-WRITE-17.                                    IF1204.2
066200 F-MEAN-WRITE-17.                                                 IF1204.2
066300     MOVE "F-MEAN-17" TO PAR-NAME.                                IF1204.2
066400     PERFORM  PRINT-DETAIL.                                       IF1204.2
066500*****************SPECIAL PERFORM TEST**********************       IF1204.2
066600 F-MEAN-18.                                                       IF1204.2
066700     MOVE ZERO TO WS-NUM.                                         IF1204.2
066800     PERFORM F-MEAN-TEST-18                                       IF1204.2
066900       UNTIL FUNCTION MEAN(ARG1, ARG2) > 8.                       IF1204.2
067000     PERFORM PASS.                                                IF1204.2
067100     GO TO F-MEAN-WRITE-18.                                       IF1204.2
067200 F-MEAN-TEST-18.                                                  IF1204.2
067300     COMPUTE ARG1 = ARG1 + 1.                                     IF1204.2
067400     COMPUTE ARG2 = ARG2 + 1.                                     IF1204.2
067500 F-MEAN-DELETE-18.                                                IF1204.2
067600     PERFORM  DE-LETE.                                            IF1204.2
067700     GO TO    F-MEAN-WRITE-18.                                    IF1204.2
067800 F-MEAN-WRITE-18.                                                 IF1204.2
067900     MOVE "F-MEAN-18" TO PAR-NAME.                                IF1204.2
068000     PERFORM  PRINT-DETAIL.                                       IF1204.2
068100********************END OF TESTS***************                   IF1204.2
068200 CCVS-EXIT SECTION.                                               IF1204.2
068300 CCVS-999999.                                                     IF1204.2
068400     GO TO CLOSE-FILES.                                           IF1204.2
      *END-OF,IF120A                                                                  
