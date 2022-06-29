      *HEADER,COBOL,IF141A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1414.2
000200 PROGRAM-ID.                                                      IF1414.2
000300     IF141A.                                                      IF1414.2
000400***********************************************************       IF1414.2
000500*                                                         *       IF1414.2
000600* This program forms part of the CCVS85 COBOL Test Suite. *       IF1414.2
000700* It contains tests for the Intrinsic Function VARIANCE   *       IF1414.2
000800*                                                         *       IF1414.2
000900***********************************************************       IF1414.2
001000 ENVIRONMENT DIVISION.                                            IF1414.2
001100 CONFIGURATION SECTION.                                           IF1414.2
001200 SOURCE-COMPUTER.                                                 IF1414.2
001300     XXXXX082.                                                    IF1414.2
001400 OBJECT-COMPUTER.                                                 IF1414.2
001500     XXXXX083.                                                    IF1414.2
001600 INPUT-OUTPUT SECTION.                                            IF1414.2
001700 FILE-CONTROL.                                                    IF1414.2
001800     SELECT PRINT-FILE ASSIGN TO                                  IF1414.2
001900     XXXXX055.                                                    IF1414.2
002000 DATA DIVISION.                                                   IF1414.2
002100 FILE SECTION.                                                    IF1414.2
002200 FD  PRINT-FILE.                                                  IF1414.2
002300 01  PRINT-REC PICTURE X(120).                                    IF1414.2
002400 01  DUMMY-RECORD PICTURE X(120).                                 IF1414.2
002500 WORKING-STORAGE SECTION.                                         IF1414.2
002600***********************************************************       IF1414.2
002700* Variables specific to the Intrinsic Function Test IF141A*       IF1414.2
002800***********************************************************       IF1414.2
002900 01  A                   PIC S9(10)          VALUE 5.             IF1414.2
003000 01  B                   PIC S9(10)          VALUE 7.             IF1414.2
003100 01  C                   PIC S9(10)          VALUE -4.            IF1414.2
003200 01  D                   PIC S9(10)          VALUE 10.            IF1414.2
003300 01  E                   PIC S9(5)V9(5)      VALUE 34.26.         IF1414.2
003400 01  F                   PIC S9(5)V9(5)      VALUE -8.32.         IF1414.2
003500 01  G                   PIC S9(5)V9(5)      VALUE 4.08.          IF1414.2
003600 01  H                   PIC S9(5)V9(5)      VALUE -5.3.          IF1414.2
003700 01  P                   PIC S9(10)          VALUE 4.             IF1414.2
003800 01  Q                   PIC S9(10)          VALUE 3.             IF1414.2
003900 01  R                   PIC S9(10)          VALUE 5.             IF1414.2
004000 01  ARG3                PIC S9(10)          VALUE 2.             IF1414.2
004100 01  ARR                                     VALUE "40537".       IF1414.2
004200     02  IND OCCURS 5 TIMES PIC 9.                                IF1414.2
004300 01  TEMP                PIC S9(10).                              IF1414.2
004400 01  WS-NUM              PIC S9(5)V9(6).                          IF1414.2
004500 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1414.2
004600 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1414.2
004700*                                                                 IF1414.2
004800**********************************************************        IF1414.2
004900*                                                                 IF1414.2
005000 01  TEST-RESULTS.                                                IF1414.2
005100     02 FILLER                   PIC X      VALUE SPACE.          IF1414.2
005200     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1414.2
005300     02 FILLER                   PIC X      VALUE SPACE.          IF1414.2
005400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1414.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IF1414.2
005600     02  PAR-NAME.                                                IF1414.2
005700       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1414.2
005800       03  PARDOT-X              PIC X      VALUE SPACE.          IF1414.2
005900       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1414.2
006000     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1414.2
006100     02 RE-MARK                  PIC X(61).                       IF1414.2
006200 01  TEST-COMPUTED.                                               IF1414.2
006300     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1414.2
006400     02 FILLER                   PIC X(17)  VALUE                 IF1414.2
006500            "       COMPUTED=".                                   IF1414.2
006600     02 COMPUTED-X.                                               IF1414.2
006700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1414.2
006800     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1414.2
006900                                 PIC -9(9).9(9).                  IF1414.2
007000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1414.2
007100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1414.2
007200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1414.2
007300     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1414.2
007400         04 COMPUTED-18V0                    PIC -9(18).          IF1414.2
007500         04 FILLER                           PIC X.               IF1414.2
007600     03 FILLER PIC X(50) VALUE SPACE.                             IF1414.2
007700 01  TEST-CORRECT.                                                IF1414.2
007800     02 FILLER PIC X(30) VALUE SPACE.                             IF1414.2
007900     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1414.2
008000     02 CORRECT-X.                                                IF1414.2
008100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1414.2
008200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1414.2
008300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1414.2
008400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1414.2
008500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1414.2
008600     03      CR-18V0 REDEFINES CORRECT-A.                         IF1414.2
008700         04 CORRECT-18V0                     PIC -9(18).          IF1414.2
008800         04 FILLER                           PIC X.               IF1414.2
008900     03 FILLER PIC X(2) VALUE SPACE.                              IF1414.2
009000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1414.2
009100 01  TEST-CORRECT-MIN.                                            IF1414.2
009200     02 FILLER PIC X(30) VALUE SPACE.                             IF1414.2
009300     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1414.2
009400     02 CORRECTMI-X.                                              IF1414.2
009500     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1414.2
009600     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1414.2
009700     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1414.2
009800     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1414.2
009900     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1414.2
010000     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1414.2
010100         04 CORRECTMI-18V0                     PIC -9(18).        IF1414.2
010200         04 FILLER                           PIC X.               IF1414.2
010300     03 FILLER PIC X(2) VALUE SPACE.                              IF1414.2
010400     03 FILLER                           PIC X(48) VALUE SPACE.   IF1414.2
010500 01  TEST-CORRECT-MAX.                                            IF1414.2
010600     02 FILLER PIC X(30) VALUE SPACE.                             IF1414.2
010700     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1414.2
010800     02 CORRECTMA-X.                                              IF1414.2
010900     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1414.2
011000     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1414.2
011100     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1414.2
011200     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1414.2
011300     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1414.2
011400     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1414.2
011500         04 CORRECTMA-18V0                     PIC -9(18).        IF1414.2
011600         04 FILLER                           PIC X.               IF1414.2
011700     03 FILLER PIC X(2) VALUE SPACE.                              IF1414.2
011800     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1414.2
011900 01  CCVS-C-1.                                                    IF1414.2
012000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1414.2
012100-    "SS  PARAGRAPH-NAME                                          IF1414.2
012200-    "       REMARKS".                                            IF1414.2
012300     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1414.2
012400 01  CCVS-C-2.                                                    IF1414.2
012500     02 FILLER                     PIC X        VALUE SPACE.      IF1414.2
012600     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1414.2
012700     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1414.2
012800     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1414.2
012900     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1414.2
013000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1414.2
013100 01  REC-CT                        PIC 99       VALUE ZERO.       IF1414.2
013200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1414.2
013300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1414.2
013400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1414.2
013500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1414.2
013600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1414.2
013700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1414.2
013800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1414.2
013900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1414.2
014000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1414.2
014100 01  CCVS-H-1.                                                    IF1414.2
014200     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1414.2
014300     02  FILLER                    PIC X(42)    VALUE             IF1414.2
014400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1414.2
014500     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1414.2
014600 01  CCVS-H-2A.                                                   IF1414.2
014700   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1414.2
014800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1414.2
014900   02  FILLER                        PIC XXXX   VALUE             IF1414.2
015000     "4.2 ".                                                      IF1414.2
015100   02  FILLER                        PIC X(28)  VALUE             IF1414.2
015200            " COPY - NOT FOR DISTRIBUTION".                       IF1414.2
015300   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1414.2
015400                                                                  IF1414.2
015500 01  CCVS-H-2B.                                                   IF1414.2
015600   02  FILLER                        PIC X(15)  VALUE             IF1414.2
015700            "TEST RESULT OF ".                                    IF1414.2
015800   02  TEST-ID                       PIC X(9).                    IF1414.2
015900   02  FILLER                        PIC X(4)   VALUE             IF1414.2
016000            " IN ".                                               IF1414.2
016100   02  FILLER                        PIC X(12)  VALUE             IF1414.2
016200     " HIGH       ".                                              IF1414.2
016300   02  FILLER                        PIC X(22)  VALUE             IF1414.2
016400            " LEVEL VALIDATION FOR ".                             IF1414.2
016500   02  FILLER                        PIC X(58)  VALUE             IF1414.2
016600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1414.2
016700 01  CCVS-H-3.                                                    IF1414.2
016800     02  FILLER                      PIC X(34)  VALUE             IF1414.2
016900            " FOR OFFICIAL USE ONLY    ".                         IF1414.2
017000     02  FILLER                      PIC X(58)  VALUE             IF1414.2
017100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1414.2
017200     02  FILLER                      PIC X(28)  VALUE             IF1414.2
017300            "  COPYRIGHT   1985 ".                                IF1414.2
017400 01  CCVS-E-1.                                                    IF1414.2
017500     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1414.2
017600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1414.2
017700     02 ID-AGAIN                     PIC X(9).                    IF1414.2
017800     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1414.2
017900 01  CCVS-E-2.                                                    IF1414.2
018000     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1414.2
018100     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1414.2
018200     02 CCVS-E-2-2.                                               IF1414.2
018300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1414.2
018400         03 FILLER                   PIC X      VALUE SPACE.      IF1414.2
018500         03 ENDER-DESC               PIC X(44)  VALUE             IF1414.2
018600            "ERRORS ENCOUNTERED".                                 IF1414.2
018700 01  CCVS-E-3.                                                    IF1414.2
018800     02  FILLER                      PIC X(22)  VALUE             IF1414.2
018900            " FOR OFFICIAL USE ONLY".                             IF1414.2
019000     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1414.2
019100     02  FILLER                      PIC X(58)  VALUE             IF1414.2
019200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1414.2
019300     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1414.2
019400     02 FILLER                       PIC X(15)  VALUE             IF1414.2
019500             " COPYRIGHT 1985".                                   IF1414.2
019600 01  CCVS-E-4.                                                    IF1414.2
019700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1414.2
019800     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1414.2
019900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1414.2
020000     02 FILLER                       PIC X(40)  VALUE             IF1414.2
020100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1414.2
020200 01  XXINFO.                                                      IF1414.2
020300     02 FILLER                       PIC X(19)  VALUE             IF1414.2
020400            "*** INFORMATION ***".                                IF1414.2
020500     02 INFO-TEXT.                                                IF1414.2
020600       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1414.2
020700       04 XXCOMPUTED                 PIC X(20).                   IF1414.2
020800       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1414.2
020900       04 XXCORRECT                  PIC X(20).                   IF1414.2
021000     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1414.2
021100 01  HYPHEN-LINE.                                                 IF1414.2
021200     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1414.2
021300     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1414.2
021400-    "*****************************************".                 IF1414.2
021500     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1414.2
021600-    "******************************".                            IF1414.2
021700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1414.2
021800     "IF141A".                                                    IF1414.2
021900 PROCEDURE DIVISION.                                              IF1414.2
022000 CCVS1 SECTION.                                                   IF1414.2
022100 OPEN-FILES.                                                      IF1414.2
022200     OPEN     OUTPUT PRINT-FILE.                                  IF1414.2
022300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1414.2
022400     MOVE    SPACE TO TEST-RESULTS.                               IF1414.2
022500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1414.2
022600     GO TO CCVS1-EXIT.                                            IF1414.2
022700 CLOSE-FILES.                                                     IF1414.2
022800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1414.2
022900 TERMINATE-CCVS.                                                  IF1414.2
023000     STOP     RUN.                                                IF1414.2
023100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1414.2
023200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1414.2
023300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1414.2
023400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1414.2
023500     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1414.2
023600 PRINT-DETAIL.                                                    IF1414.2
023700     IF REC-CT NOT EQUAL TO ZERO                                  IF1414.2
023800             MOVE "." TO PARDOT-X                                 IF1414.2
023900             MOVE REC-CT TO DOTVALUE.                             IF1414.2
024000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1414.2
024100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1414.2
024200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1414.2
024300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1414.2
024400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1414.2
024500     MOVE SPACE TO CORRECT-X.                                     IF1414.2
024600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1414.2
024700     MOVE     SPACE TO RE-MARK.                                   IF1414.2
024800 HEAD-ROUTINE.                                                    IF1414.2
024900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1414.2
025000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1414.2
025100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1414.2
025200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1414.2
025300 COLUMN-NAMES-ROUTINE.                                            IF1414.2
025400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1414.2
025500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1414.2
025600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1414.2
025700 END-ROUTINE.                                                     IF1414.2
025800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1414.2
025900 END-RTN-EXIT.                                                    IF1414.2
026000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1414.2
026100 END-ROUTINE-1.                                                   IF1414.2
026200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1414.2
026300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1414.2
026400      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1414.2
026500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1414.2
026600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1414.2
026700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1414.2
026800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1414.2
026900  END-ROUTINE-12.                                                 IF1414.2
027000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1414.2
027100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1414.2
027200         MOVE "NO " TO ERROR-TOTAL                                IF1414.2
027300         ELSE                                                     IF1414.2
027400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1414.2
027500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1414.2
027600     PERFORM WRITE-LINE.                                          IF1414.2
027700 END-ROUTINE-13.                                                  IF1414.2
027800     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1414.2
027900         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1414.2
028000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1414.2
028100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1414.2
028200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1414.2
028300      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1414.2
028400          MOVE "NO " TO ERROR-TOTAL                               IF1414.2
028500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1414.2
028600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1414.2
028700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1414.2
028800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1414.2
028900 WRITE-LINE.                                                      IF1414.2
029000     ADD 1 TO RECORD-COUNT.                                       IF1414.2
029100*    IF RECORD-COUNT GREATER 42                                   IF1414.2
029200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1414.2
029300*        MOVE SPACE TO DUMMY-RECORD                               IF1414.2
029400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1414.2
029500*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1414.2
029600*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1414.2
029700*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1414.2
029800*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1414.2
029900*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1414.2
030000*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1414.2
030100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1414.2
030200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1414.2
030300*        MOVE ZERO TO RECORD-COUNT.                               IF1414.2
030400     PERFORM WRT-LN.                                              IF1414.2
030500 WRT-LN.                                                          IF1414.2
030600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1414.2
030700     MOVE SPACE TO DUMMY-RECORD.                                  IF1414.2
030800 BLANK-LINE-PRINT.                                                IF1414.2
030900     PERFORM WRT-LN.                                              IF1414.2
031000 FAIL-ROUTINE.                                                    IF1414.2
031100     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1414.2
031200            GO TO FAIL-ROUTINE-WRITE.                             IF1414.2
031300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1414.2
031400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1414.2
031500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1414.2
031600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1414.2
031700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1414.2
031800     GO TO  FAIL-ROUTINE-EX.                                      IF1414.2
031900 FAIL-ROUTINE-WRITE.                                              IF1414.2
032000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1414.2
032100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1414.2
032200                              CORMA-ANSI-REFERENCE.               IF1414.2
032300     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1414.2
032400           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1414.2
032500           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1414.2
032600     ELSE                                                         IF1414.2
032700           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1414.2
032800     PERFORM WRITE-LINE.                                          IF1414.2
032900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1414.2
033000 FAIL-ROUTINE-EX. EXIT.                                           IF1414.2
033100 BAIL-OUT.                                                        IF1414.2
033200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1414.2
033300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1414.2
033400 BAIL-OUT-WRITE.                                                  IF1414.2
033500     MOVE CORRECT-A TO XXCORRECT.                                 IF1414.2
033600     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1414.2
033700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1414.2
033800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1414.2
033900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1414.2
034000 BAIL-OUT-EX. EXIT.                                               IF1414.2
034100 CCVS1-EXIT.                                                      IF1414.2
034200     EXIT.                                                        IF1414.2
034300********************************************************          IF1414.2
034400*                                                      *          IF1414.2
034500*    Intrinsic Function Tests     IF141A - VARIANCE    *          IF1414.2
034600*                                                      *          IF1414.2
034700********************************************************          IF1414.2
034800 SECT-IF141A SECTION.                                             IF1414.2
034900 F-VARIANCE-INFO.                                                 IF1414.2
035000     MOVE     "See ref. A-74 2.45" TO ANSI-REFERENCE.             IF1414.2
035100     MOVE     "VARIANCE Function"     TO FEATURE.                 IF1414.2
035200*****************TEST (a) - SIMPLE TEST*****************          IF1414.2
035300 F-VARIANCE-01.                                                   IF1414.2
035400     MOVE ZERO TO WS-NUM.                                         IF1414.2
035500     MOVE  48.6865 TO MIN-RANGE.                                  IF1414.2
035600     MOVE  48.6885 TO MAX-RANGE.                                  IF1414.2
035700 F-VARIANCE-TEST-01.                                              IF1414.2
035800     COMPUTE WS-NUM = FUNCTION VARIANCE(5, -2, -14, 0).           IF1414.2
035900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
036000        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
036100                    PERFORM PASS                                  IF1414.2
036200     ELSE                                                         IF1414.2
036300                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
036400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
036500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
036600                    PERFORM FAIL.                                 IF1414.2
036700     GO TO F-VARIANCE-WRITE-01.                                   IF1414.2
036800 F-VARIANCE-DELETE-01.                                            IF1414.2
036900     PERFORM  DE-LETE.                                            IF1414.2
037000     GO TO    F-VARIANCE-WRITE-01.                                IF1414.2
037100 F-VARIANCE-WRITE-01.                                             IF1414.2
037200     MOVE "F-VARIANCE-01" TO PAR-NAME.                            IF1414.2
037300     PERFORM  PRINT-DETAIL.                                       IF1414.2
037400*****************TEST (b) - SIMPLE TEST*****************          IF1414.2
037500 F-VARIANCE-02.                                                   IF1414.2
037600     EVALUATE FUNCTION VARIANCE(3.9, -0.3, 8.7, 100.2)            IF1414.2
037700     WHEN  1741.70 THRU  1741.77                                  IF1414.2
037800                    PERFORM PASS                                  IF1414.2
037900     WHEN OTHER                                                   IF1414.2
038000                    PERFORM FAIL.                                 IF1414.2
038100     GO TO F-VARIANCE-WRITE-02.                                   IF1414.2
038200 F-VARIANCE-DELETE-02.                                            IF1414.2
038300     PERFORM  DE-LETE.                                            IF1414.2
038400     GO TO    F-VARIANCE-WRITE-02.                                IF1414.2
038500 F-VARIANCE-WRITE-02.                                             IF1414.2
038600     MOVE "F-VARIANCE-02" TO PAR-NAME.                            IF1414.2
038700     PERFORM  PRINT-DETAIL.                                       IF1414.2
038800*****************TEST (c) - SIMPLE TEST*****************          IF1414.2
038900 F-VARIANCE-03.                                                   IF1414.2
039000     MOVE  27.2494 TO MIN-RANGE.                                  IF1414.2
039100     MOVE  27.2505 TO MAX-RANGE.                                  IF1414.2
039200 F-VARIANCE-TEST-03.                                              IF1414.2
039300     IF (FUNCTION VARIANCE(A, B, C, D) >= MIN-RANGE) AND          IF1414.2
039400        (FUNCTION VARIANCE(A, B, C, D) <= MAX-RANGE) THEN         IF1414.2
039500                    PERFORM PASS                                  IF1414.2
039600     ELSE                                                         IF1414.2
039700                    PERFORM FAIL.                                 IF1414.2
039800     GO TO F-VARIANCE-WRITE-03.                                   IF1414.2
039900 F-VARIANCE-DELETE-03.                                            IF1414.2
040000     PERFORM  DE-LETE.                                            IF1414.2
040100     GO TO    F-VARIANCE-WRITE-03.                                IF1414.2
040200 F-VARIANCE-WRITE-03.                                             IF1414.2
040300     MOVE "F-VARIANCE-03" TO PAR-NAME.                            IF1414.2
040400     PERFORM  PRINT-DETAIL.                                       IF1414.2
040500*****************TEST (d) - SIMPLE TEST*****************          IF1414.2
040600 F-VARIANCE-04.                                                   IF1414.2
040700     MOVE ZERO TO WS-NUM.                                         IF1414.2
040800     MOVE  283.728 TO MIN-RANGE.                                  IF1414.2
040900     MOVE  283.740 TO MAX-RANGE.                                  IF1414.2
041000 F-VARIANCE-TEST-04.                                              IF1414.2
041100     COMPUTE WS-NUM = FUNCTION VARIANCE(E, F, G, H).              IF1414.2
041200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
041300        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
041400                    PERFORM PASS                                  IF1414.2
041500     ELSE                                                         IF1414.2
041600                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
041700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
041800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
041900                    PERFORM FAIL.                                 IF1414.2
042000     GO TO F-VARIANCE-WRITE-04.                                   IF1414.2
042100 F-VARIANCE-DELETE-04.                                            IF1414.2
042200     PERFORM  DE-LETE.                                            IF1414.2
042300     GO TO    F-VARIANCE-WRITE-04.                                IF1414.2
042400 F-VARIANCE-WRITE-04.                                             IF1414.2
042500     MOVE "F-VARIANCE-04" TO PAR-NAME.                            IF1414.2
042600     PERFORM  PRINT-DETAIL.                                       IF1414.2
042700*****************TEST (e) - SIMPLE TEST*****************          IF1414.2
042800 F-VARIANCE-05.                                                   IF1414.2
042900     MOVE ZERO TO WS-NUM.                                         IF1414.2
043000     MOVE  94.6981 TO MIN-RANGE.                                  IF1414.2
043100     MOVE  94.7019 TO MAX-RANGE.                                  IF1414.2
043200 F-VARIANCE-TEST-05.                                              IF1414.2
043300     COMPUTE WS-NUM =                                             IF1414.2
043400          FUNCTION VARIANCE(10.2, -0.2, 5.6, -15.6).              IF1414.2
043500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
043600        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
043700                    PERFORM PASS                                  IF1414.2
043800     ELSE                                                         IF1414.2
043900                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
044000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
044100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
044200                    PERFORM FAIL.                                 IF1414.2
044300     GO TO F-VARIANCE-WRITE-05.                                   IF1414.2
044400 F-VARIANCE-DELETE-05.                                            IF1414.2
044500     PERFORM  DE-LETE.                                            IF1414.2
044600     GO TO    F-VARIANCE-WRITE-05.                                IF1414.2
044700 F-VARIANCE-WRITE-05.                                             IF1414.2
044800     MOVE "F-VARIANCE-05" TO PAR-NAME.                            IF1414.2
044900     PERFORM  PRINT-DETAIL.                                       IF1414.2
045000*****************TEST (f) - SIMPLE TEST*****************          IF1414.2
045100 F-VARIANCE-06.                                                   IF1414.2
045200     MOVE ZERO TO WS-NUM.                                         IF1414.2
045300     MOVE  156.194 TO MIN-RANGE.                                  IF1414.2
045400     MOVE  156.200 TO MAX-RANGE.                                  IF1414.2
045500 F-VARIANCE-TEST-06.                                              IF1414.2
045600     COMPUTE WS-NUM =                                             IF1414.2
045700            FUNCTION VARIANCE(A, B, C, D, E, F, G, H).            IF1414.2
045800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
045900        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
046000                    PERFORM PASS                                  IF1414.2
046100     ELSE                                                         IF1414.2
046200                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
046300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
046400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
046500                    PERFORM FAIL.                                 IF1414.2
046600     GO TO F-VARIANCE-WRITE-06.                                   IF1414.2
046700 F-VARIANCE-DELETE-06.                                            IF1414.2
046800     PERFORM  DE-LETE.                                            IF1414.2
046900     GO TO    F-VARIANCE-WRITE-06.                                IF1414.2
047000 F-VARIANCE-WRITE-06.                                             IF1414.2
047100     MOVE "F-VARIANCE-06" TO PAR-NAME.                            IF1414.2
047200     PERFORM  PRINT-DETAIL.                                       IF1414.2
047300*****************TEST (g) - SIMPLE TEST*****************          IF1414.2
047400 F-VARIANCE-07.                                                   IF1414.2
047500     MOVE ZERO TO WS-NUM.                                         IF1414.2
047600     MOVE  4.66657 TO MIN-RANGE.                                  IF1414.2
047700     MOVE  4.66675 TO MAX-RANGE.                                  IF1414.2
047800 F-VARIANCE-TEST-07.                                              IF1414.2
047900     COMPUTE WS-NUM =                                             IF1414.2
048000           FUNCTION VARIANCE(IND(1), IND(2), IND(3)).             IF1414.2
048100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
048200        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
048300                    PERFORM PASS                                  IF1414.2
048400     ELSE                                                         IF1414.2
048500                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
048600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
048700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
048800                    PERFORM FAIL.                                 IF1414.2
048900     GO TO F-VARIANCE-WRITE-07.                                   IF1414.2
049000 F-VARIANCE-DELETE-07.                                            IF1414.2
049100     PERFORM  DE-LETE.                                            IF1414.2
049200     GO TO    F-VARIANCE-WRITE-07.                                IF1414.2
049300 F-VARIANCE-WRITE-07.                                             IF1414.2
049400     MOVE "F-VARIANCE-07" TO PAR-NAME.                            IF1414.2
049500     PERFORM  PRINT-DETAIL.                                       IF1414.2
049600*****************TEST (h) - SIMPLE TEST*****************          IF1414.2
049700 F-VARIANCE-08.                                                   IF1414.2
049800     MOVE ZERO TO WS-NUM.                                         IF1414.2
049900     MOVE  2.66661 TO MIN-RANGE.                                  IF1414.2
050000     MOVE  2.66671 TO MAX-RANGE.                                  IF1414.2
050100 F-VARIANCE-TEST-08.                                              IF1414.2
050200     COMPUTE WS-NUM =                                             IF1414.2
050300            FUNCTION VARIANCE(IND(P), IND(Q), IND(R)).            IF1414.2
050400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
050500        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
050600                    PERFORM PASS                                  IF1414.2
050700     ELSE                                                         IF1414.2
050800                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
050900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
051000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
051100                    PERFORM FAIL.                                 IF1414.2
051200     GO TO F-VARIANCE-WRITE-08.                                   IF1414.2
051300 F-VARIANCE-DELETE-08.                                            IF1414.2
051400     PERFORM  DE-LETE.                                            IF1414.2
051500     GO TO    F-VARIANCE-WRITE-08.                                IF1414.2
051600 F-VARIANCE-WRITE-08.                                             IF1414.2
051700     MOVE "F-VARIANCE-08" TO PAR-NAME.                            IF1414.2
051800     PERFORM  PRINT-DETAIL.                                       IF1414.2
051900*****************TEST (i) - SIMPLE TEST*****************          IF1414.2
052000 F-VARIANCE-09.                                                   IF1414.2
052100     MOVE ZERO TO WS-NUM.                                         IF1414.2
052200     MOVE 5.35989 TO MIN-RANGE.                                   IF1414.2
052300     MOVE 5.36011 TO MAX-RANGE.                                   IF1414.2
052400 F-VARIANCE-TEST-09.                                              IF1414.2
052500     COMPUTE WS-NUM = FUNCTION VARIANCE(IND(ALL)).                IF1414.2
052600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
052700        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
052800                    PERFORM PASS                                  IF1414.2
052900     ELSE                                                         IF1414.2
053000                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
053100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
053200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
053300                    PERFORM FAIL.                                 IF1414.2
053400     GO TO F-VARIANCE-WRITE-09.                                   IF1414.2
053500 F-VARIANCE-DELETE-09.                                            IF1414.2
053600     PERFORM  DE-LETE.                                            IF1414.2
053700     GO TO    F-VARIANCE-WRITE-09.                                IF1414.2
053800 F-VARIANCE-WRITE-09.                                             IF1414.2
053900     MOVE "F-VARIANCE-09" TO PAR-NAME.                            IF1414.2
054000     PERFORM  PRINT-DETAIL.                                       IF1414.2
054100*****************TEST (k) - SIMPLE TEST*****************          IF1414.2
054200 F-VARIANCE-11.                                                   IF1414.2
054300     MOVE ZERO TO WS-NUM.                                         IF1414.2
054400     MOVE -0.000020 TO MIN-RANGE.                                 IF1414.2
054500     MOVE  0.000020 TO MAX-RANGE.                                 IF1414.2
054600 F-VARIANCE-TEST-11.                                              IF1414.2
054700     COMPUTE WS-NUM = FUNCTION VARIANCE(A, 5, A).                 IF1414.2
054800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
054900        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
055000                    PERFORM PASS                                  IF1414.2
055100     ELSE                                                         IF1414.2
055200                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
055300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
055400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
055500                    PERFORM FAIL.                                 IF1414.2
055600     GO TO F-VARIANCE-WRITE-11.                                   IF1414.2
055700 F-VARIANCE-DELETE-11.                                            IF1414.2
055800     PERFORM  DE-LETE.                                            IF1414.2
055900     GO TO    F-VARIANCE-WRITE-11.                                IF1414.2
056000 F-VARIANCE-WRITE-11.                                             IF1414.2
056100     MOVE "F-VARIANCE-11" TO PAR-NAME.                            IF1414.2
056200     PERFORM  PRINT-DETAIL.                                       IF1414.2
056300*****************TEST (a) - COMPLEX TEST****************          IF1414.2
056400 F-VARIANCE-12.                                                   IF1414.2
056500     MOVE ZERO TO WS-NUM.                                         IF1414.2
056600     MOVE 78.9968 TO MIN-RANGE.                                   IF1414.2
056700     MOVE 79.0031 TO MAX-RANGE.                                   IF1414.2
056800 F-VARIANCE-TEST-12.                                              IF1414.2
056900     COMPUTE WS-NUM = FUNCTION VARIANCE(A, B) + 78.               IF1414.2
057000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
057100        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
057200                    PERFORM PASS                                  IF1414.2
057300     ELSE                                                         IF1414.2
057400                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
057500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
057600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
057700                    PERFORM FAIL.                                 IF1414.2
057800     GO TO F-VARIANCE-WRITE-12.                                   IF1414.2
057900 F-VARIANCE-DELETE-12.                                            IF1414.2
058000     PERFORM  DE-LETE.                                            IF1414.2
058100     GO TO    F-VARIANCE-WRITE-12.                                IF1414.2
058200 F-VARIANCE-WRITE-12.                                             IF1414.2
058300     MOVE "F-VARIANCE-12" TO PAR-NAME.                            IF1414.2
058400     PERFORM  PRINT-DETAIL.                                       IF1414.2
058500*****************TEST (b) - COMPLEX TEST****************          IF1414.2
058600 F-VARIANCE-13.                                                   IF1414.2
058700     MOVE ZERO TO WS-NUM.                                         IF1414.2
058800     MOVE  139.234 TO MIN-RANGE.                                  IF1414.2
058900     MOVE  139.245 TO MAX-RANGE.                                  IF1414.2
059000 F-VARIANCE-TEST-13.                                              IF1414.2
059100     COMPUTE WS-NUM = FUNCTION VARIANCE(2.6 + 30, 4.5 * 2).       IF1414.2
059200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
059300        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
059400                    PERFORM PASS                                  IF1414.2
059500     ELSE                                                         IF1414.2
059600                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
059700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
059800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
059900                    PERFORM FAIL.                                 IF1414.2
060000     GO TO F-VARIANCE-WRITE-13.                                   IF1414.2
060100 F-VARIANCE-DELETE-13.                                            IF1414.2
060200     PERFORM  DE-LETE.                                            IF1414.2
060300     GO TO    F-VARIANCE-WRITE-13.                                IF1414.2
060400 F-VARIANCE-WRITE-13.                                             IF1414.2
060500     MOVE "F-VARIANCE-13" TO PAR-NAME.                            IF1414.2
060600     PERFORM  PRINT-DETAIL.                                       IF1414.2
060700*****************TEST (c) - COMPLEX TEST****************          IF1414.2
060800 F-VARIANCE-14.                                                   IF1414.2
060900     MOVE ZERO TO WS-NUM.                                         IF1414.2
061000     MOVE  374.658 TO MIN-RANGE.                                  IF1414.2
061100     MOVE  374.688 TO MAX-RANGE.                                  IF1414.2
061200 F-VARIANCE-TEST-14.                                              IF1414.2
061300     COMPUTE WS-NUM = FUNCTION VARIANCE(E, 9 * A, 0, B / 2).      IF1414.2
061400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
061500        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
061600                    PERFORM PASS                                  IF1414.2
061700     ELSE                                                         IF1414.2
061800                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
061900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
062000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
062100                    PERFORM FAIL.                                 IF1414.2
062200     GO TO F-VARIANCE-WRITE-14.                                   IF1414.2
062300 F-VARIANCE-DELETE-14.                                            IF1414.2
062400     PERFORM  DE-LETE.                                            IF1414.2
062500     GO TO    F-VARIANCE-WRITE-14.                                IF1414.2
062600 F-VARIANCE-WRITE-14.                                             IF1414.2
062700     MOVE "F-VARIANCE-14" TO PAR-NAME.                            IF1414.2
062800     PERFORM  PRINT-DETAIL.                                       IF1414.2
062900*****************TEST (d) - COMPLEX TEST****************          IF1414.2
063000 F-VARIANCE-15.                                                   IF1414.2
063100     MOVE ZERO TO WS-NUM.                                         IF1414.2
063200     MOVE 0.999960 TO MIN-RANGE.                                  IF1414.2
063300     MOVE 1.00004 TO MAX-RANGE.                                   IF1414.2
063400 F-VARIANCE-TEST-15.                                              IF1414.2
063500     COMPUTE WS-NUM = FUNCTION VARIANCE(A, B) +                   IF1414.2
063600                      FUNCTION VARIANCE(1, 1).                    IF1414.2
063700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
063800        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
063900                    PERFORM PASS                                  IF1414.2
064000     ELSE                                                         IF1414.2
064100                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
064200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
064300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
064400                    PERFORM FAIL.                                 IF1414.2
064500     GO TO F-VARIANCE-WRITE-15.                                   IF1414.2
064600 F-VARIANCE-DELETE-15.                                            IF1414.2
064700     PERFORM  DE-LETE.                                            IF1414.2
064800     GO TO    F-VARIANCE-WRITE-15.                                IF1414.2
064900 F-VARIANCE-WRITE-15.                                             IF1414.2
065000     MOVE "F-VARIANCE-15" TO PAR-NAME.                            IF1414.2
065100     PERFORM  PRINT-DETAIL.                                       IF1414.2
065200*****************TEST (e) - COMPLEX TEST****************          IF1414.2
065300 F-VARIANCE-16.                                                   IF1414.2
065400     MOVE ZERO TO WS-NUM.                                         IF1414.2
065500     MOVE -0.000040 TO MIN-RANGE.                                 IF1414.2
065600     MOVE 0.000040 TO MAX-RANGE.                                  IF1414.2
065700 F-VARIANCE-TEST-16.                                              IF1414.2
065800     COMPUTE WS-NUM = FUNCTION VARIANCE(                          IF1414.2
065900                      FUNCTION VARIANCE(0), 0).                   IF1414.2
066000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1414.2
066100        (WS-NUM <= MAX-RANGE) THEN                                IF1414.2
066200                    PERFORM PASS                                  IF1414.2
066300     ELSE                                                         IF1414.2
066400                    MOVE WS-NUM TO COMPUTED-N                     IF1414.2
066500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1414.2
066600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1414.2
066700                    PERFORM FAIL.                                 IF1414.2
066800     GO TO F-VARIANCE-WRITE-16.                                   IF1414.2
066900 F-VARIANCE-DELETE-16.                                            IF1414.2
067000     PERFORM  DE-LETE.                                            IF1414.2
067100     GO TO    F-VARIANCE-WRITE-16.                                IF1414.2
067200 F-VARIANCE-WRITE-16.                                             IF1414.2
067300     MOVE "F-VARIANCE-16" TO PAR-NAME.                            IF1414.2
067400     PERFORM  PRINT-DETAIL.                                       IF1414.2
067500*****************SPECIAL PERFORM TEST**********************       IF1414.2
067600 F-VARIANCE-17.                                                   IF1414.2
067700     PERFORM F-VARIANCE-TEST-17                                   IF1414.2
067800       UNTIL FUNCTION VARIANCE(1, 1, ARG3) > 3.                   IF1414.2
067900     PERFORM PASS.                                                IF1414.2
068000     GO TO F-VARIANCE-WRITE-17.                                   IF1414.2
068100 F-VARIANCE-TEST-17.                                              IF1414.2
068200     COMPUTE ARG3 = ARG3 + 1.                                     IF1414.2
068300 F-VARIANCE-DELETE-17.                                            IF1414.2
068400     PERFORM  DE-LETE.                                            IF1414.2
068500     GO TO    F-VARIANCE-WRITE-17.                                IF1414.2
068600 F-VARIANCE-WRITE-17.                                             IF1414.2
068700     MOVE "F-VARIANCE-17" TO PAR-NAME.                            IF1414.2
068800     PERFORM  PRINT-DETAIL.                                       IF1414.2
068900********************END OF TESTS***************                   IF1414.2
069000 CCVS-EXIT SECTION.                                               IF1414.2
069100 CCVS-999999.                                                     IF1414.2
069200     GO TO CLOSE-FILES.                                           IF1414.2
      *END-OF,IF141A                                                                  
