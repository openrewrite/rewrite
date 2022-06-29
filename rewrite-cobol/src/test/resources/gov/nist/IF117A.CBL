      *HEADER,COBOL,IF117A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1174.2
000200 PROGRAM-ID.                                                      IF1174.2
000300     IF117A.                                                      IF1174.2
000400                                                                  IF1174.2
000500***********************************************************       IF1174.2
000600*                                                         *       IF1174.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1174.2
000800* It contains tests for the Intrinsic Function LOG10.     *       IF1174.2
000900*                                                         *       IF1174.2
001000***********************************************************       IF1174.2
001100 ENVIRONMENT DIVISION.                                            IF1174.2
001200 CONFIGURATION SECTION.                                           IF1174.2
001300 SOURCE-COMPUTER.                                                 IF1174.2
001400     XXXXX082.                                                    IF1174.2
001500 OBJECT-COMPUTER.                                                 IF1174.2
001600     XXXXX083.                                                    IF1174.2
001700 INPUT-OUTPUT SECTION.                                            IF1174.2
001800 FILE-CONTROL.                                                    IF1174.2
001900     SELECT PRINT-FILE ASSIGN TO                                  IF1174.2
002000     XXXXX055.                                                    IF1174.2
002100 DATA DIVISION.                                                   IF1174.2
002200 FILE SECTION.                                                    IF1174.2
002300 FD  PRINT-FILE.                                                  IF1174.2
002400 01  PRINT-REC PICTURE X(120).                                    IF1174.2
002500 01  DUMMY-RECORD PICTURE X(120).                                 IF1174.2
002600 WORKING-STORAGE SECTION.                                         IF1174.2
002700***********************************************************       IF1174.2
002800* Variables specific to the Intrinsic Function Test IF117A*       IF1174.2
002900***********************************************************       IF1174.2
003000 01  A                   PIC S9(10)          VALUE 600000.        IF1174.2
003100 01  B                   PIC S9(10)          VALUE 7.             IF1174.2
003200 01  C                   PIC S9(10)          VALUE -4.            IF1174.2
003300 01  D                   PIC S9(10)          VALUE 10.            IF1174.2
003400 01  E                   PIC S9(1)V9(9)      VALUE 2.718281828.   IF1174.2
003500 01  F                   PIC S9(5)V9(5)      VALUE 32000.8.       IF1174.2
003600 01  G                   PIC S9(5)V9(5)      VALUE .00002.        IF1174.2
003700 01  H                   PIC S9(5)V9(5)      VALUE -5.3.          IF1174.2
003800 01  ARG1                PIC S9(5)V9(5)      VALUE 10.00.         IF1174.2
003900 01  ARR                                     VALUE "40537".       IF1174.2
004000     02  IND OCCURS 5 TIMES PIC 9.                                IF1174.2
004100 01  TEMP                PIC S9(10).                              IF1174.2
004200 01  WS-NUM              PIC S9(5)V9(6).                          IF1174.2
004300 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1174.2
004400 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1174.2
004500*                                                                 IF1174.2
004600**********************************************************        IF1174.2
004700*                                                                 IF1174.2
004800 01  TEST-RESULTS.                                                IF1174.2
004900     02 FILLER                   PIC X      VALUE SPACE.          IF1174.2
005000     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1174.2
005100     02 FILLER                   PIC X      VALUE SPACE.          IF1174.2
005200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1174.2
005300     02 FILLER                   PIC X      VALUE SPACE.          IF1174.2
005400     02  PAR-NAME.                                                IF1174.2
005500       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1174.2
005600       03  PARDOT-X              PIC X      VALUE SPACE.          IF1174.2
005700       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1174.2
005800     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1174.2
005900     02 RE-MARK                  PIC X(61).                       IF1174.2
006000 01  TEST-COMPUTED.                                               IF1174.2
006100     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1174.2
006200     02 FILLER                   PIC X(17)  VALUE                 IF1174.2
006300            "       COMPUTED=".                                   IF1174.2
006400     02 COMPUTED-X.                                               IF1174.2
006500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1174.2
006600     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1174.2
006700                                 PIC -9(9).9(9).                  IF1174.2
006800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1174.2
006900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1174.2
007000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1174.2
007100     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1174.2
007200         04 COMPUTED-18V0                    PIC -9(18).          IF1174.2
007300         04 FILLER                           PIC X.               IF1174.2
007400     03 FILLER PIC X(50) VALUE SPACE.                             IF1174.2
007500 01  TEST-CORRECT.                                                IF1174.2
007600     02 FILLER PIC X(30) VALUE SPACE.                             IF1174.2
007700     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1174.2
007800     02 CORRECT-X.                                                IF1174.2
007900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1174.2
008000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1174.2
008100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1174.2
008200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1174.2
008300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1174.2
008400     03      CR-18V0 REDEFINES CORRECT-A.                         IF1174.2
008500         04 CORRECT-18V0                     PIC -9(18).          IF1174.2
008600         04 FILLER                           PIC X.               IF1174.2
008700     03 FILLER PIC X(2) VALUE SPACE.                              IF1174.2
008800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1174.2
008900 01  TEST-CORRECT-MIN.                                            IF1174.2
009000     02 FILLER PIC X(30) VALUE SPACE.                             IF1174.2
009100     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1174.2
009200     02 CORRECTMI-X.                                              IF1174.2
009300     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1174.2
009400     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1174.2
009500     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1174.2
009600     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1174.2
009700     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1174.2
009800     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1174.2
009900         04 CORRECTMI-18V0                     PIC -9(18).        IF1174.2
010000         04 FILLER                           PIC X.               IF1174.2
010100     03 FILLER PIC X(2) VALUE SPACE.                              IF1174.2
010200     03 FILLER                           PIC X(48) VALUE SPACE.   IF1174.2
010300 01  TEST-CORRECT-MAX.                                            IF1174.2
010400     02 FILLER PIC X(30) VALUE SPACE.                             IF1174.2
010500     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1174.2
010600     02 CORRECTMA-X.                                              IF1174.2
010700     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1174.2
010800     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1174.2
010900     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1174.2
011000     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1174.2
011100     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1174.2
011200     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1174.2
011300         04 CORRECTMA-18V0                     PIC -9(18).        IF1174.2
011400         04 FILLER                           PIC X.               IF1174.2
011500     03 FILLER PIC X(2) VALUE SPACE.                              IF1174.2
011600     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1174.2
011700 01  CCVS-C-1.                                                    IF1174.2
011800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1174.2
011900-    "SS  PARAGRAPH-NAME                                          IF1174.2
012000-    "       REMARKS".                                            IF1174.2
012100     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1174.2
012200 01  CCVS-C-2.                                                    IF1174.2
012300     02 FILLER                     PIC X        VALUE SPACE.      IF1174.2
012400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1174.2
012500     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1174.2
012600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1174.2
012700     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1174.2
012800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1174.2
012900 01  REC-CT                        PIC 99       VALUE ZERO.       IF1174.2
013000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1174.2
013100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1174.2
013200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1174.2
013300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1174.2
013400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1174.2
013500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1174.2
013600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1174.2
013700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1174.2
013800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1174.2
013900 01  CCVS-H-1.                                                    IF1174.2
014000     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1174.2
014100     02  FILLER                    PIC X(42)    VALUE             IF1174.2
014200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1174.2
014300     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1174.2
014400 01  CCVS-H-2A.                                                   IF1174.2
014500   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1174.2
014600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1174.2
014700   02  FILLER                        PIC XXXX   VALUE             IF1174.2
014800     "4.2 ".                                                      IF1174.2
014900   02  FILLER                        PIC X(28)  VALUE             IF1174.2
015000            " COPY - NOT FOR DISTRIBUTION".                       IF1174.2
015100   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1174.2
015200                                                                  IF1174.2
015300 01  CCVS-H-2B.                                                   IF1174.2
015400   02  FILLER                        PIC X(15)  VALUE             IF1174.2
015500            "TEST RESULT OF ".                                    IF1174.2
015600   02  TEST-ID                       PIC X(9).                    IF1174.2
015700   02  FILLER                        PIC X(4)   VALUE             IF1174.2
015800            " IN ".                                               IF1174.2
015900   02  FILLER                        PIC X(12)  VALUE             IF1174.2
016000     " HIGH       ".                                              IF1174.2
016100   02  FILLER                        PIC X(22)  VALUE             IF1174.2
016200            " LEVEL VALIDATION FOR ".                             IF1174.2
016300   02  FILLER                        PIC X(58)  VALUE             IF1174.2
016400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1174.2
016500 01  CCVS-H-3.                                                    IF1174.2
016600     02  FILLER                      PIC X(34)  VALUE             IF1174.2
016700            " FOR OFFICIAL USE ONLY    ".                         IF1174.2
016800     02  FILLER                      PIC X(58)  VALUE             IF1174.2
016900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1174.2
017000     02  FILLER                      PIC X(28)  VALUE             IF1174.2
017100            "  COPYRIGHT   1985 ".                                IF1174.2
017200 01  CCVS-E-1.                                                    IF1174.2
017300     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1174.2
017400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1174.2
017500     02 ID-AGAIN                     PIC X(9).                    IF1174.2
017600     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1174.2
017700 01  CCVS-E-2.                                                    IF1174.2
017800     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1174.2
017900     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1174.2
018000     02 CCVS-E-2-2.                                               IF1174.2
018100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1174.2
018200         03 FILLER                   PIC X      VALUE SPACE.      IF1174.2
018300         03 ENDER-DESC               PIC X(44)  VALUE             IF1174.2
018400            "ERRORS ENCOUNTERED".                                 IF1174.2
018500 01  CCVS-E-3.                                                    IF1174.2
018600     02  FILLER                      PIC X(22)  VALUE             IF1174.2
018700            " FOR OFFICIAL USE ONLY".                             IF1174.2
018800     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1174.2
018900     02  FILLER                      PIC X(58)  VALUE             IF1174.2
019000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1174.2
019100     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1174.2
019200     02 FILLER                       PIC X(15)  VALUE             IF1174.2
019300             " COPYRIGHT 1985".                                   IF1174.2
019400 01  CCVS-E-4.                                                    IF1174.2
019500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1174.2
019600     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1174.2
019700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1174.2
019800     02 FILLER                       PIC X(40)  VALUE             IF1174.2
019900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1174.2
020000 01  XXINFO.                                                      IF1174.2
020100     02 FILLER                       PIC X(19)  VALUE             IF1174.2
020200            "*** INFORMATION ***".                                IF1174.2
020300     02 INFO-TEXT.                                                IF1174.2
020400       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1174.2
020500       04 XXCOMPUTED                 PIC X(20).                   IF1174.2
020600       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1174.2
020700       04 XXCORRECT                  PIC X(20).                   IF1174.2
020800     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1174.2
020900 01  HYPHEN-LINE.                                                 IF1174.2
021000     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1174.2
021100     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1174.2
021200-    "*****************************************".                 IF1174.2
021300     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1174.2
021400-    "******************************".                            IF1174.2
021500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1174.2
021600     "IF117A".                                                    IF1174.2
021700 PROCEDURE DIVISION.                                              IF1174.2
021800 CCVS1 SECTION.                                                   IF1174.2
021900 OPEN-FILES.                                                      IF1174.2
022000     OPEN     OUTPUT PRINT-FILE.                                  IF1174.2
022100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1174.2
022200     MOVE    SPACE TO TEST-RESULTS.                               IF1174.2
022300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1174.2
022400     GO TO CCVS1-EXIT.                                            IF1174.2
022500 CLOSE-FILES.                                                     IF1174.2
022600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1174.2
022700 TERMINATE-CCVS.                                                  IF1174.2
022800     STOP     RUN.                                                IF1174.2
022900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1174.2
023000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1174.2
023100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1174.2
023200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1174.2
023300     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1174.2
023400 PRINT-DETAIL.                                                    IF1174.2
023500     IF REC-CT NOT EQUAL TO ZERO                                  IF1174.2
023600             MOVE "." TO PARDOT-X                                 IF1174.2
023700             MOVE REC-CT TO DOTVALUE.                             IF1174.2
023800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1174.2
023900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1174.2
024000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1174.2
024100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1174.2
024200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1174.2
024300     MOVE SPACE TO CORRECT-X.                                     IF1174.2
024400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1174.2
024500     MOVE     SPACE TO RE-MARK.                                   IF1174.2
024600 HEAD-ROUTINE.                                                    IF1174.2
024700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1174.2
024800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1174.2
024900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1174.2
025000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1174.2
025100 COLUMN-NAMES-ROUTINE.                                            IF1174.2
025200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1174.2
025300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1174.2
025400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1174.2
025500 END-ROUTINE.                                                     IF1174.2
025600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1174.2
025700 END-RTN-EXIT.                                                    IF1174.2
025800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1174.2
025900 END-ROUTINE-1.                                                   IF1174.2
026000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1174.2
026100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1174.2
026200      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1174.2
026300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1174.2
026400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1174.2
026500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1174.2
026600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1174.2
026700  END-ROUTINE-12.                                                 IF1174.2
026800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1174.2
026900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1174.2
027000         MOVE "NO " TO ERROR-TOTAL                                IF1174.2
027100         ELSE                                                     IF1174.2
027200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1174.2
027300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1174.2
027400     PERFORM WRITE-LINE.                                          IF1174.2
027500 END-ROUTINE-13.                                                  IF1174.2
027600     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1174.2
027700         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1174.2
027800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1174.2
027900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1174.2
028000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1174.2
028100      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1174.2
028200          MOVE "NO " TO ERROR-TOTAL                               IF1174.2
028300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1174.2
028400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1174.2
028500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1174.2
028600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1174.2
028700 WRITE-LINE.                                                      IF1174.2
028800     ADD 1 TO RECORD-COUNT.                                       IF1174.2
028900*    IF RECORD-COUNT GREATER 42                                   IF1174.2
029000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1174.2
029100*        MOVE SPACE TO DUMMY-RECORD                               IF1174.2
029200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1174.2
029300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1174.2
029400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1174.2
029500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1174.2
029600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1174.2
029700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1174.2
029800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1174.2
029900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1174.2
030000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1174.2
030100*        MOVE ZERO TO RECORD-COUNT.                               IF1174.2
030200     PERFORM WRT-LN.                                              IF1174.2
030300 WRT-LN.                                                          IF1174.2
030400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1174.2
030500     MOVE SPACE TO DUMMY-RECORD.                                  IF1174.2
030600 BLANK-LINE-PRINT.                                                IF1174.2
030700     PERFORM WRT-LN.                                              IF1174.2
030800 FAIL-ROUTINE.                                                    IF1174.2
030900     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1174.2
031000            GO TO FAIL-ROUTINE-WRITE.                             IF1174.2
031100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1174.2
031200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1174.2
031300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1174.2
031400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1174.2
031500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1174.2
031600     GO TO  FAIL-ROUTINE-EX.                                      IF1174.2
031700 FAIL-ROUTINE-WRITE.                                              IF1174.2
031800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1174.2
031900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1174.2
032000                              CORMA-ANSI-REFERENCE.               IF1174.2
032100     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1174.2
032200           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1174.2
032300           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1174.2
032400     ELSE                                                         IF1174.2
032500           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1174.2
032600     PERFORM WRITE-LINE.                                          IF1174.2
032700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1174.2
032800 FAIL-ROUTINE-EX. EXIT.                                           IF1174.2
032900 BAIL-OUT.                                                        IF1174.2
033000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1174.2
033100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1174.2
033200 BAIL-OUT-WRITE.                                                  IF1174.2
033300     MOVE CORRECT-A TO XXCORRECT.                                 IF1174.2
033400     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1174.2
033500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1174.2
033600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1174.2
033700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1174.2
033800 BAIL-OUT-EX. EXIT.                                               IF1174.2
033900 CCVS1-EXIT.                                                      IF1174.2
034000     EXIT.                                                        IF1174.2
034100********************************************************          IF1174.2
034200*                                                      *          IF1174.2
034300*    Intrinsic Function Tests         IF117A - LOG10   *          IF1174.2
034400*                                                      *          IF1174.2
034500********************************************************          IF1174.2
034600 SECT-IF117A SECTION.                                             IF1174.2
034700 F-LOG10-INFO.                                                    IF1174.2
034800     MOVE     "See ref. A-49 2.21" TO ANSI-REFERENCE.             IF1174.2
034900     MOVE     "LOG10 Function" TO FEATURE.                        IF1174.2
035000*****************TEST (a) - SIMPLE TEST*****************          IF1174.2
035100 F-LOG10-01.                                                      IF1174.2
035200     MOVE ZERO TO WS-NUM.                                         IF1174.2
035300     MOVE -0.000020 TO MIN-RANGE.                                 IF1174.2
035400     MOVE  0.000020 TO MAX-RANGE.                                 IF1174.2
035500 F-LOG10-TEST-01.                                                 IF1174.2
035600     COMPUTE WS-NUM = FUNCTION LOG10(1).                          IF1174.2
035700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
035800        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
035900                    PERFORM PASS                                  IF1174.2
036000     ELSE                                                         IF1174.2
036100                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
036200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
036300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
036400                    PERFORM FAIL.                                 IF1174.2
036500     GO TO F-LOG10-WRITE-01.                                      IF1174.2
036600 F-LOG10-DELETE-01.                                               IF1174.2
036700     PERFORM  DE-LETE.                                            IF1174.2
036800     GO TO    F-LOG10-WRITE-01.                                   IF1174.2
036900 F-LOG10-WRITE-01.                                                IF1174.2
037000     MOVE "F-LOG10-01" TO PAR-NAME.                               IF1174.2
037100     PERFORM  PRINT-DETAIL.                                       IF1174.2
037200*****************TEST (b) - SIMPLE TEST*****************          IF1174.2
037300 F-LOG10-02.                                                      IF1174.2
037400     EVALUATE FUNCTION LOG10(10)                                  IF1174.2
037500     WHEN 0.999980 THRU 1.000020                                  IF1174.2
037600                    PERFORM PASS                                  IF1174.2
037700     WHEN OTHER                                                   IF1174.2
037800                    PERFORM FAIL.                                 IF1174.2
037900     GO TO F-LOG10-WRITE-02.                                      IF1174.2
038000 F-LOG10-DELETE-02.                                               IF1174.2
038100     PERFORM  DE-LETE.                                            IF1174.2
038200     GO TO    F-LOG10-WRITE-02.                                   IF1174.2
038300 F-LOG10-WRITE-02.                                                IF1174.2
038400     MOVE "F-LOG10-02" TO PAR-NAME.                               IF1174.2
038500     PERFORM  PRINT-DETAIL.                                       IF1174.2
038600*****************TEST (c) - SIMPLE TEST*****************          IF1174.2
038700 F-LOG10-03.                                                      IF1174.2
038800     MOVE -2.00004 TO MIN-RANGE.                                  IF1174.2
038900     MOVE -1.99996 TO MAX-RANGE.                                  IF1174.2
039000 F-LOG10-TEST-03.                                                 IF1174.2
039100     IF (FUNCTION LOG10(.01) >= MIN-RANGE) AND                    IF1174.2
039200        (FUNCTION LOG10(.01) <= MAX-RANGE) THEN                   IF1174.2
039300                    PERFORM PASS                                  IF1174.2
039400     ELSE                                                         IF1174.2
039500                    PERFORM FAIL.                                 IF1174.2
039600     GO TO F-LOG10-WRITE-03.                                      IF1174.2
039700 F-LOG10-DELETE-03.                                               IF1174.2
039800     PERFORM  DE-LETE.                                            IF1174.2
039900     GO TO    F-LOG10-WRITE-03.                                   IF1174.2
040000 F-LOG10-WRITE-03.                                                IF1174.2
040100     MOVE "F-LOG10-03" TO PAR-NAME.                               IF1174.2
040200     PERFORM  PRINT-DETAIL.                                       IF1174.2
040300*****************TEST (d) - SIMPLE TEST*****************          IF1174.2
040400 F-LOG10-04.                                                      IF1174.2
040500     MOVE ZERO TO WS-NUM.                                         IF1174.2
040600     MOVE -3.00006 TO MIN-RANGE.                                  IF1174.2
040700     MOVE -2.99994 TO MAX-RANGE.                                  IF1174.2
040800 F-LOG10-TEST-04.                                                 IF1174.2
040900     COMPUTE WS-NUM = FUNCTION LOG10(.001).                       IF1174.2
041000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
041100        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
041200                    PERFORM PASS                                  IF1174.2
041300     ELSE                                                         IF1174.2
041400                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
041500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
041600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
041700                    PERFORM FAIL.                                 IF1174.2
041800     GO TO F-LOG10-WRITE-04.                                      IF1174.2
041900 F-LOG10-DELETE-04.                                               IF1174.2
042000     PERFORM  DE-LETE.                                            IF1174.2
042100     GO TO    F-LOG10-WRITE-04.                                   IF1174.2
042200 F-LOG10-WRITE-04.                                                IF1174.2
042300     MOVE "F-LOG10-04" TO PAR-NAME.                               IF1174.2
042400     PERFORM  PRINT-DETAIL.                                       IF1174.2
042500*****************TEST (e) - SIMPLE TEST*****************          IF1174.2
042600 F-LOG10-05.                                                      IF1174.2
042700     MOVE ZERO TO WS-NUM.                                         IF1174.2
042800     MOVE  1.99996 TO MIN-RANGE.                                  IF1174.2
042900     MOVE  2.00004 TO MAX-RANGE.                                  IF1174.2
043000 F-LOG10-TEST-05.                                                 IF1174.2
043100     COMPUTE WS-NUM = FUNCTION LOG10(100).                        IF1174.2
043200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
043300        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
043400                    PERFORM PASS                                  IF1174.2
043500     ELSE                                                         IF1174.2
043600                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
043700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
043800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
043900                    PERFORM FAIL.                                 IF1174.2
044000     GO TO F-LOG10-WRITE-05.                                      IF1174.2
044100 F-LOG10-DELETE-05.                                               IF1174.2
044200     PERFORM  DE-LETE.                                            IF1174.2
044300     GO TO    F-LOG10-WRITE-05.                                   IF1174.2
044400 F-LOG10-WRITE-05.                                                IF1174.2
044500     MOVE "F-LOG10-05" TO PAR-NAME.                               IF1174.2
044600     PERFORM  PRINT-DETAIL.                                       IF1174.2
044700*****************TEST (f) - SIMPLE TEST*****************          IF1174.2
044800 F-LOG10-06.                                                      IF1174.2
044900     MOVE ZERO TO WS-NUM.                                         IF1174.2
045000     MOVE  0.999936 TO MIN-RANGE.                                 IF1174.2
045100     MOVE  0.999976 TO MAX-RANGE.                                 IF1174.2
045200 F-LOG10-TEST-06.                                                 IF1174.2
045300     COMPUTE WS-NUM = FUNCTION LOG10(9.999).                      IF1174.2
045400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
045500        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
045600                    PERFORM PASS                                  IF1174.2
045700     ELSE                                                         IF1174.2
045800                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
045900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
046000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
046100                    PERFORM FAIL.                                 IF1174.2
046200     GO TO F-LOG10-WRITE-06.                                      IF1174.2
046300 F-LOG10-DELETE-06.                                               IF1174.2
046400     PERFORM  DE-LETE.                                            IF1174.2
046500     GO TO    F-LOG10-WRITE-06.                                   IF1174.2
046600 F-LOG10-WRITE-06.                                                IF1174.2
046700     MOVE "F-LOG10-06" TO PAR-NAME.                               IF1174.2
046800     PERFORM  PRINT-DETAIL.                                       IF1174.2
046900*****************TEST (h) - SIMPLE TEST*****************          IF1174.2
047000 F-LOG10-08.                                                      IF1174.2
047100     MOVE ZERO TO WS-NUM.                                         IF1174.2
047200     MOVE -2.04579 TO MIN-RANGE.                                  IF1174.2
047300     MOVE -2.04571 TO MAX-RANGE.                                  IF1174.2
047400 F-LOG10-TEST-08.                                                 IF1174.2
047500     COMPUTE WS-NUM = FUNCTION LOG10(.009).                       IF1174.2
047600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
047700        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
047800                    PERFORM PASS                                  IF1174.2
047900     ELSE                                                         IF1174.2
048000                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
048100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
048200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
048300                    PERFORM FAIL.                                 IF1174.2
048400     GO TO F-LOG10-WRITE-08.                                      IF1174.2
048500 F-LOG10-DELETE-08.                                               IF1174.2
048600     PERFORM  DE-LETE.                                            IF1174.2
048700     GO TO    F-LOG10-WRITE-08.                                   IF1174.2
048800 F-LOG10-WRITE-08.                                                IF1174.2
048900     MOVE "F-LOG10-08" TO PAR-NAME.                               IF1174.2
049000     PERFORM  PRINT-DETAIL.                                       IF1174.2
049100*****************TEST (i) - SIMPLE TEST*****************          IF1174.2
049200 F-LOG10-09.                                                      IF1174.2
049300     MOVE ZERO TO WS-NUM.                                         IF1174.2
049400     MOVE  2.00039 TO MIN-RANGE.                                  IF1174.2
049500     MOVE  2.00047 TO MAX-RANGE.                                  IF1174.2
049600 F-LOG10-TEST-09.                                                 IF1174.2
049700     COMPUTE WS-NUM = FUNCTION LOG10(100.1).                      IF1174.2
049800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
049900        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
050000                    PERFORM PASS                                  IF1174.2
050100     ELSE                                                         IF1174.2
050200                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
050300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
050400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
050500                    PERFORM FAIL.                                 IF1174.2
050600     GO TO F-LOG10-WRITE-09.                                      IF1174.2
050700 F-LOG10-DELETE-09.                                               IF1174.2
050800     PERFORM  DE-LETE.                                            IF1174.2
050900     GO TO    F-LOG10-WRITE-09.                                   IF1174.2
051000 F-LOG10-WRITE-09.                                                IF1174.2
051100     MOVE "F-LOG10-09" TO PAR-NAME.                               IF1174.2
051200     PERFORM  PRINT-DETAIL.                                       IF1174.2
051300*****************TEST (j) - SIMPLE TEST*****************          IF1174.2
051400 F-LOG10-10.                                                      IF1174.2
051500     MOVE ZERO TO WS-NUM.                                         IF1174.2
051600     MOVE  3.99992 TO MIN-RANGE.                                  IF1174.2
051700     MOVE  4.00008 TO MAX-RANGE.                                  IF1174.2
051800 F-LOG10-TEST-10.                                                 IF1174.2
051900     COMPUTE WS-NUM = FUNCTION LOG10(10000).                      IF1174.2
052000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
052100        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
052200                    PERFORM PASS                                  IF1174.2
052300     ELSE                                                         IF1174.2
052400                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
052500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
052600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
052700                    PERFORM FAIL.                                 IF1174.2
052800     GO TO F-LOG10-WRITE-10.                                      IF1174.2
052900 F-LOG10-DELETE-10.                                               IF1174.2
053000     PERFORM  DE-LETE.                                            IF1174.2
053100     GO TO    F-LOG10-WRITE-10.                                   IF1174.2
053200 F-LOG10-WRITE-10.                                                IF1174.2
053300     MOVE "F-LOG10-10" TO PAR-NAME.                               IF1174.2
053400     PERFORM  PRINT-DETAIL.                                       IF1174.2
053500*****************TEST (k) - SIMPLE TEST*****************          IF1174.2
053600 F-LOG10-11.                                                      IF1174.2
053700     MOVE ZERO TO WS-NUM.                                         IF1174.2
053800     MOVE  3.48129 TO MIN-RANGE.                                  IF1174.2
053900     MOVE  3.48143 TO MAX-RANGE.                                  IF1174.2
054000 F-LOG10-TEST-11.                                                 IF1174.2
054100     COMPUTE WS-NUM = FUNCTION LOG10(3029.48).                    IF1174.2
054200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
054300        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
054400                    PERFORM PASS                                  IF1174.2
054500     ELSE                                                         IF1174.2
054600                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
054700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
054800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
054900                    PERFORM FAIL.                                 IF1174.2
055000     GO TO F-LOG10-WRITE-11.                                      IF1174.2
055100 F-LOG10-DELETE-11.                                               IF1174.2
055200     PERFORM  DE-LETE.                                            IF1174.2
055300     GO TO    F-LOG10-WRITE-11.                                   IF1174.2
055400 F-LOG10-WRITE-11.                                                IF1174.2
055500     MOVE "F-LOG10-11" TO PAR-NAME.                               IF1174.2
055600     PERFORM  PRINT-DETAIL.                                       IF1174.2
055700*****************TEST (l) - SIMPLE TEST*****************          IF1174.2
055800 F-LOG10-12.                                                      IF1174.2
055900     MOVE ZERO TO WS-NUM.                                         IF1174.2
056000     MOVE -4.30111 TO MIN-RANGE.                                  IF1174.2
056100     MOVE -4.30093 TO MAX-RANGE.                                  IF1174.2
056200 F-LOG10-TEST-12.                                                 IF1174.2
056300     COMPUTE WS-NUM = FUNCTION LOG10(.00005).                     IF1174.2
056400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
056500        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
056600                    PERFORM PASS                                  IF1174.2
056700     ELSE                                                         IF1174.2
056800                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
056900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
057000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
057100                    PERFORM FAIL.                                 IF1174.2
057200     GO TO F-LOG10-WRITE-12.                                      IF1174.2
057300 F-LOG10-DELETE-12.                                               IF1174.2
057400     PERFORM  DE-LETE.                                            IF1174.2
057500     GO TO    F-LOG10-WRITE-12.                                   IF1174.2
057600 F-LOG10-WRITE-12.                                                IF1174.2
057700     MOVE "F-LOG10-12" TO PAR-NAME.                               IF1174.2
057800     PERFORM  PRINT-DETAIL.                                       IF1174.2
057900*****************TEST (m) - SIMPLE TEST*****************          IF1174.2
058000 F-LOG10-13.                                                      IF1174.2
058100     MOVE ZERO TO WS-NUM.                                         IF1174.2
058200     MOVE  5.77803 TO MIN-RANGE.                                  IF1174.2
058300     MOVE  5.77826 TO MAX-RANGE.                                  IF1174.2
058400 F-LOG10-TEST-13.                                                 IF1174.2
058500     COMPUTE WS-NUM = FUNCTION LOG10(A).                          IF1174.2
058600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
058700        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
058800                    PERFORM PASS                                  IF1174.2
058900     ELSE                                                         IF1174.2
059000                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
059100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
059200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
059300                    PERFORM FAIL.                                 IF1174.2
059400     GO TO F-LOG10-WRITE-13.                                      IF1174.2
059500 F-LOG10-DELETE-13.                                               IF1174.2
059600     PERFORM  DE-LETE.                                            IF1174.2
059700     GO TO    F-LOG10-WRITE-13.                                   IF1174.2
059800 F-LOG10-WRITE-13.                                                IF1174.2
059900     MOVE "F-LOG10-13" TO PAR-NAME.                               IF1174.2
060000     PERFORM  PRINT-DETAIL.                                       IF1174.2
060100*****************TEST (n) - SIMPLE TEST*****************          IF1174.2
060200 F-LOG10-14.                                                      IF1174.2
060300     MOVE ZERO TO WS-NUM.                                         IF1174.2
060400     MOVE  4.50507 TO MIN-RANGE.                                  IF1174.2
060500     MOVE  4.50525 TO MAX-RANGE.                                  IF1174.2
060600 F-LOG10-TEST-14.                                                 IF1174.2
060700     COMPUTE WS-NUM = FUNCTION LOG10(F).                          IF1174.2
060800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
060900        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
061000                    PERFORM PASS                                  IF1174.2
061100     ELSE                                                         IF1174.2
061200                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
061300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
061400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
061500                    PERFORM FAIL.                                 IF1174.2
061600     GO TO F-LOG10-WRITE-14.                                      IF1174.2
061700 F-LOG10-DELETE-14.                                               IF1174.2
061800     PERFORM  DE-LETE.                                            IF1174.2
061900     GO TO    F-LOG10-WRITE-14.                                   IF1174.2
062000 F-LOG10-WRITE-14.                                                IF1174.2
062100     MOVE "F-LOG10-14" TO PAR-NAME.                               IF1174.2
062200     PERFORM  PRINT-DETAIL.                                       IF1174.2
062300*****************TEST (o) - SIMPLE TEST*****************          IF1174.2
062400 F-LOG10-15.                                                      IF1174.2
062500     MOVE ZERO TO WS-NUM.                                         IF1174.2
062600     MOVE -4.69906 TO MIN-RANGE.                                  IF1174.2
062700     MOVE -4.69888 TO MAX-RANGE.                                  IF1174.2
062800 F-LOG10-TEST-15.                                                 IF1174.2
062900     COMPUTE WS-NUM = FUNCTION LOG10(G).                          IF1174.2
063000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
063100        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
063200                    PERFORM PASS                                  IF1174.2
063300     ELSE                                                         IF1174.2
063400                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
063500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
063600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
063700                    PERFORM FAIL.                                 IF1174.2
063800     GO TO F-LOG10-WRITE-15.                                      IF1174.2
063900 F-LOG10-DELETE-15.                                               IF1174.2
064000     PERFORM  DE-LETE.                                            IF1174.2
064100     GO TO    F-LOG10-WRITE-15.                                   IF1174.2
064200 F-LOG10-WRITE-15.                                                IF1174.2
064300     MOVE "F-LOG10-15" TO PAR-NAME.                               IF1174.2
064400     PERFORM  PRINT-DETAIL.                                       IF1174.2
064500*****************TEST (p) - SIMPLE TEST*****************          IF1174.2
064600 F-LOG10-16.                                                      IF1174.2
064700     MOVE ZERO TO WS-NUM.                                         IF1174.2
064800     MOVE  0.477111 TO MIN-RANGE.                                 IF1174.2
064900     MOVE  0.477131 TO MAX-RANGE.                                 IF1174.2
065000 F-LOG10-TEST-16.                                                 IF1174.2
065100     COMPUTE WS-NUM = FUNCTION LOG10(IND(4)).                     IF1174.2
065200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
065300        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
065400                    PERFORM PASS                                  IF1174.2
065500     ELSE                                                         IF1174.2
065600                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
065700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
065800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
065900                    PERFORM FAIL.                                 IF1174.2
066000     GO TO F-LOG10-WRITE-16.                                      IF1174.2
066100 F-LOG10-DELETE-16.                                               IF1174.2
066200     PERFORM  DE-LETE.                                            IF1174.2
066300     GO TO    F-LOG10-WRITE-16.                                   IF1174.2
066400 F-LOG10-WRITE-16.                                                IF1174.2
066500     MOVE "F-LOG10-16" TO PAR-NAME.                               IF1174.2
066600     PERFORM  PRINT-DETAIL.                                       IF1174.2
066700*****************TEST (a) - COMPLEX TEST****************          IF1174.2
066800 F-LOG10-17.                                                      IF1174.2
066900     MOVE ZERO TO WS-NUM.                                         IF1174.2
067000     MOVE  0.434437 TO MIN-RANGE.                                 IF1174.2
067100     MOVE  0.434471 TO MAX-RANGE.                                 IF1174.2
067200 F-LOG10-TEST-17.                                                 IF1174.2
067300     COMPUTE WS-NUM = FUNCTION LOG10(E + .001).                   IF1174.2
067400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
067500        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
067600                    PERFORM PASS                                  IF1174.2
067700     ELSE                                                         IF1174.2
067800                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
067900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
068000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
068100                    PERFORM FAIL.                                 IF1174.2
068200     GO TO F-LOG10-WRITE-17.                                      IF1174.2
068300 F-LOG10-DELETE-17.                                               IF1174.2
068400     PERFORM  DE-LETE.                                            IF1174.2
068500     GO TO    F-LOG10-WRITE-17.                                   IF1174.2
068600 F-LOG10-WRITE-17.                                                IF1174.2
068700     MOVE "F-LOG10-17" TO PAR-NAME.                               IF1174.2
068800     PERFORM  PRINT-DETAIL.                                       IF1174.2
068900*****************TEST (b) - COMPLEX TEST****************          IF1174.2
069000 F-LOG10-18.                                                      IF1174.2
069100     MOVE ZERO TO WS-NUM.                                         IF1174.2
069200     MOVE -1.00004 TO MIN-RANGE.                                  IF1174.2
069300     MOVE -0.999960 TO MAX-RANGE.                                 IF1174.2
069400 F-LOG10-TEST-18.                                                 IF1174.2
069500     COMPUTE WS-NUM = FUNCTION LOG10(1 / 10).                     IF1174.2
069600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
069700        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
069800                    PERFORM PASS                                  IF1174.2
069900     ELSE                                                         IF1174.2
070000                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
070100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
070200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
070300                    PERFORM FAIL.                                 IF1174.2
070400     GO TO F-LOG10-WRITE-18.                                      IF1174.2
070500 F-LOG10-DELETE-18.                                               IF1174.2
070600     PERFORM  DE-LETE.                                            IF1174.2
070700     GO TO    F-LOG10-WRITE-18.                                   IF1174.2
070800 F-LOG10-WRITE-18.                                                IF1174.2
070900     MOVE "F-LOG10-18" TO PAR-NAME.                               IF1174.2
071000     PERFORM  PRINT-DETAIL.                                       IF1174.2
071100*****************TEST (c) - COMPLEX TEST****************          IF1174.2
071200 F-LOG10-19.                                                      IF1174.2
071300     MOVE ZERO TO WS-NUM.                                         IF1174.2
071400     MOVE  0.417999 TO MIN-RANGE.                                 IF1174.2
071500     MOVE  0.418033 TO MAX-RANGE.                                 IF1174.2
071600 F-LOG10-TEST-19.                                                 IF1174.2
071700     COMPUTE WS-NUM = FUNCTION LOG10(E - .1).                     IF1174.2
071800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
071900        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
072000                    PERFORM PASS                                  IF1174.2
072100     ELSE                                                         IF1174.2
072200                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
072300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
072400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
072500                    PERFORM FAIL.                                 IF1174.2
072600     GO TO F-LOG10-WRITE-19.                                      IF1174.2
072700 F-LOG10-DELETE-19.                                               IF1174.2
072800     PERFORM  DE-LETE.                                            IF1174.2
072900     GO TO    F-LOG10-WRITE-19.                                   IF1174.2
073000 F-LOG10-WRITE-19.                                                IF1174.2
073100     MOVE "F-LOG10-19" TO PAR-NAME.                               IF1174.2
073200     PERFORM  PRINT-DETAIL.                                       IF1174.2
073300*****************TEST (d) - COMPLEX TEST****************          IF1174.2
073400 F-LOG10-20.                                                      IF1174.2
073500     MOVE ZERO TO WS-NUM.                                         IF1174.2
073600     MOVE -0.045759  TO MIN-RANGE.                                IF1174.2
073700     MOVE -0.045755 TO MAX-RANGE.                                 IF1174.2
073800 F-LOG10-TEST-20.                                                 IF1174.2
073900     COMPUTE WS-NUM = FUNCTION LOG10(1 - .1).                     IF1174.2
074000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
074100        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
074200                    PERFORM PASS                                  IF1174.2
074300     ELSE                                                         IF1174.2
074400                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
074500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
074600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
074700                    PERFORM FAIL.                                 IF1174.2
074800     GO TO F-LOG10-WRITE-20.                                      IF1174.2
074900 F-LOG10-DELETE-20.                                               IF1174.2
075000     PERFORM  DE-LETE.                                            IF1174.2
075100     GO TO    F-LOG10-WRITE-20.                                   IF1174.2
075200 F-LOG10-WRITE-20.                                                IF1174.2
075300     MOVE "F-LOG10-20" TO PAR-NAME.                               IF1174.2
075400     PERFORM  PRINT-DETAIL.                                       IF1174.2
075500*****************TEST (e) - COMPLEX TEST****************          IF1174.2
075600 F-LOG10-21.                                                      IF1174.2
075700     MOVE ZERO TO WS-NUM.                                         IF1174.2
075800     MOVE  1.04135 TO MIN-RANGE.                                  IF1174.2
075900     MOVE  1.04143 TO MAX-RANGE.                                  IF1174.2
076000 F-LOG10-TEST-21.                                                 IF1174.2
076100     COMPUTE WS-NUM = FUNCTION LOG10(10 * 1.1).                   IF1174.2
076200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
076300        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
076400                    PERFORM PASS                                  IF1174.2
076500     ELSE                                                         IF1174.2
076600                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
076700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
076800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
076900                    PERFORM FAIL.                                 IF1174.2
077000     GO TO F-LOG10-WRITE-21.                                      IF1174.2
077100 F-LOG10-DELETE-21.                                               IF1174.2
077200     PERFORM  DE-LETE.                                            IF1174.2
077300     GO TO    F-LOG10-WRITE-21.                                   IF1174.2
077400 F-LOG10-WRITE-21.                                                IF1174.2
077500     MOVE "F-LOG10-21" TO PAR-NAME.                               IF1174.2
077600     PERFORM  PRINT-DETAIL.                                       IF1174.2
077700*****************TEST (f) - COMPLEX TEST****************          IF1174.2
077800 F-LOG10-22.                                                      IF1174.2
077900     MOVE ZERO TO WS-NUM.                                         IF1174.2
078000     MOVE -1.92090 TO MIN-RANGE.                                  IF1174.2
078100     MOVE -1.92074 TO MAX-RANGE.                                  IF1174.2
078200 F-LOG10-TEST-22.                                                 IF1174.2
078300     COMPUTE WS-NUM = FUNCTION LOG10((A * G)/ 1000).              IF1174.2
078400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
078500        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
078600                    PERFORM PASS                                  IF1174.2
078700     ELSE                                                         IF1174.2
078800                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
078900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
079000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
079100                    PERFORM FAIL.                                 IF1174.2
079200     GO TO F-LOG10-WRITE-22.                                      IF1174.2
079300 F-LOG10-DELETE-22.                                               IF1174.2
079400     PERFORM  DE-LETE.                                            IF1174.2
079500     GO TO    F-LOG10-WRITE-22.                                   IF1174.2
079600 F-LOG10-WRITE-22.                                                IF1174.2
079700     MOVE "F-LOG10-22" TO PAR-NAME.                               IF1174.2
079800     PERFORM  PRINT-DETAIL.                                       IF1174.2
079900*****************TEST (g) - COMPLEX TEST****************          IF1174.2
080000 F-LOG10-23.                                                      IF1174.2
080100     MOVE ZERO TO WS-NUM.                                         IF1174.2
080200     MOVE  0.845064 TO MIN-RANGE.                                 IF1174.2
080300     MOVE  0.845132 TO MAX-RANGE.                                 IF1174.2
080400 F-LOG10-TEST-23.                                                 IF1174.2
080500     COMPUTE WS-NUM = FUNCTION LOG10(IND(D - 5)).                 IF1174.2
080600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
080700        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
080800                    PERFORM PASS                                  IF1174.2
080900     ELSE                                                         IF1174.2
081000                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
081100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
081200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
081300                    PERFORM FAIL.                                 IF1174.2
081400     GO TO F-LOG10-WRITE-23.                                      IF1174.2
081500 F-LOG10-DELETE-23.                                               IF1174.2
081600     PERFORM  DE-LETE.                                            IF1174.2
081700     GO TO    F-LOG10-WRITE-23.                                   IF1174.2
081800 F-LOG10-WRITE-23.                                                IF1174.2
081900     MOVE "F-LOG10-23" TO PAR-NAME.                               IF1174.2
082000     PERFORM  PRINT-DETAIL.                                       IF1174.2
082100*****************TEST (h) - COMPLEX TEST****************          IF1174.2
082200 F-LOG10-24.                                                      IF1174.2
082300     MOVE ZERO TO WS-NUM.                                         IF1174.2
082400     MOVE  1.30097 TO MIN-RANGE.                                  IF1174.2
082500     MOVE  1.30107 TO MAX-RANGE.                                  IF1174.2
082600 F-LOG10-TEST-24.                                                 IF1174.2
082700     COMPUTE WS-NUM = FUNCTION LOG10(2 * 10).                     IF1174.2
082800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
082900        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
083000                    PERFORM PASS                                  IF1174.2
083100     ELSE                                                         IF1174.2
083200                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
083300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
083400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
083500                    PERFORM FAIL.                                 IF1174.2
083600     GO TO F-LOG10-WRITE-24.                                      IF1174.2
083700 F-LOG10-DELETE-24.                                               IF1174.2
083800     PERFORM  DE-LETE.                                            IF1174.2
083900     GO TO    F-LOG10-WRITE-24.                                   IF1174.2
084000 F-LOG10-WRITE-24.                                                IF1174.2
084100     MOVE "F-LOG10-24" TO PAR-NAME.                               IF1174.2
084200     PERFORM  PRINT-DETAIL.                                       IF1174.2
084300*****************TEST (i) - COMPLEX TEST****************          IF1174.2
084400 F-LOG10-25.                                                      IF1174.2
084500     MOVE ZERO TO WS-NUM.                                         IF1174.2
084600     MOVE  0.477102 TO MIN-RANGE.                                 IF1174.2
084700     MOVE  0.477140 TO MAX-RANGE.                                 IF1174.2
084800 F-LOG10-TEST-25.                                                 IF1174.2
084900     COMPUTE WS-NUM = FUNCTION LOG10(B + C).                      IF1174.2
085000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
085100        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
085200                    PERFORM PASS                                  IF1174.2
085300     ELSE                                                         IF1174.2
085400                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
085500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
085600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
085700                    PERFORM FAIL.                                 IF1174.2
085800     GO TO F-LOG10-WRITE-25.                                      IF1174.2
085900 F-LOG10-DELETE-25.                                               IF1174.2
086000     PERFORM  DE-LETE.                                            IF1174.2
086100     GO TO    F-LOG10-WRITE-25.                                   IF1174.2
086200 F-LOG10-WRITE-25.                                                IF1174.2
086300     MOVE "F-LOG10-25" TO PAR-NAME.                               IF1174.2
086400     PERFORM  PRINT-DETAIL.                                       IF1174.2
086500*****************TEST (j) -COMPLEX TEST*****************          IF1174.2
086600 F-LOG10-26.                                                      IF1174.2
086700     MOVE ZERO TO WS-NUM.                                         IF1174.2
086800     MOVE  0.274690 TO MIN-RANGE.                                 IF1174.2
086900     MOVE  0.274712 TO MAX-RANGE.                                 IF1174.2
087000 F-LOG10-TEST-26.                                                 IF1174.2
087100     COMPUTE WS-NUM = FUNCTION LOG10(3.2 / 1.7).                  IF1174.2
087200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
087300        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
087400                    PERFORM PASS                                  IF1174.2
087500     ELSE                                                         IF1174.2
087600                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
087700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
087800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
087900                    PERFORM FAIL.                                 IF1174.2
088000     GO TO F-LOG10-WRITE-26.                                      IF1174.2
088100 F-LOG10-DELETE-26.                                               IF1174.2
088200     PERFORM  DE-LETE.                                            IF1174.2
088300     GO TO    F-LOG10-WRITE-26.                                   IF1174.2
088400 F-LOG10-WRITE-26.                                                IF1174.2
088500     MOVE "F-LOG10-26" TO PAR-NAME.                               IF1174.2
088600     PERFORM  PRINT-DETAIL.                                       IF1174.2
088700*****************TEST (k) - COMPLEX TEST****************          IF1174.2
088800 F-LOG10-27.                                                      IF1174.2
088900     MOVE ZERO TO WS-NUM.                                         IF1174.2
089000     MOVE  0.904045 TO MIN-RANGE.                                 IF1174.2
089100     MOVE  0.904117 TO MAX-RANGE.                                 IF1174.2
089200 F-LOG10-TEST-27.                                                 IF1174.2
089300     COMPUTE WS-NUM = FUNCTION LOG10(E - H).                      IF1174.2
089400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
089500        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
089600                    PERFORM PASS                                  IF1174.2
089700     ELSE                                                         IF1174.2
089800                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
089900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
090000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
090100                    PERFORM FAIL.                                 IF1174.2
090200     GO TO F-LOG10-WRITE-27.                                      IF1174.2
090300 F-LOG10-DELETE-27.                                               IF1174.2
090400     PERFORM  DE-LETE.                                            IF1174.2
090500     GO TO    F-LOG10-WRITE-27.                                   IF1174.2
090600 F-LOG10-WRITE-27.                                                IF1174.2
090700     MOVE "F-LOG10-27" TO PAR-NAME.                               IF1174.2
090800     PERFORM  PRINT-DETAIL.                                       IF1174.2
090900*****************TEST (l) - COMPLEX TEST****************          IF1174.2
091000 F-LOG10-28.                                                      IF1174.2
091100     MOVE ZERO TO WS-NUM.                                         IF1174.2
091200     MOVE  0.698942 TO MIN-RANGE.                                 IF1174.2
091300     MOVE  0.698998 TO MAX-RANGE.                                 IF1174.2
091400 F-LOG10-TEST-28.                                                 IF1174.2
091500     COMPUTE WS-NUM = FUNCTION LOG10(B - 2).                      IF1174.2
091600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
091700        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
091800                    PERFORM PASS                                  IF1174.2
091900     ELSE                                                         IF1174.2
092000                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
092100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
092200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
092300                    PERFORM FAIL.                                 IF1174.2
092400     GO TO F-LOG10-WRITE-28.                                      IF1174.2
092500 F-LOG10-DELETE-28.                                               IF1174.2
092600     PERFORM  DE-LETE.                                            IF1174.2
092700     GO TO    F-LOG10-WRITE-28.                                   IF1174.2
092800 F-LOG10-WRITE-28.                                                IF1174.2
092900     MOVE "F-LOG10-28" TO PAR-NAME.                               IF1174.2
093000     PERFORM  PRINT-DETAIL.                                       IF1174.2
093100*****************TEST (m) - COMPLEX TEST****************          IF1174.2
093200 F-LOG10-29.                                                      IF1174.2
093300     MOVE ZERO TO WS-NUM.                                         IF1174.2
093400     MOVE  0.645227 TO MIN-RANGE.                                 IF1174.2
093500     MOVE  0.645279 TO MAX-RANGE.                                 IF1174.2
093600 F-LOG10-TEST-29.                                                 IF1174.2
093700     COMPUTE WS-NUM = FUNCTION LOG10(E + 1.7).                    IF1174.2
093800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
093900        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
094000                    PERFORM PASS                                  IF1174.2
094100     ELSE                                                         IF1174.2
094200                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
094300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
094400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
094500                    PERFORM FAIL.                                 IF1174.2
094600     GO TO F-LOG10-WRITE-29.                                      IF1174.2
094700 F-LOG10-DELETE-29.                                               IF1174.2
094800     PERFORM  DE-LETE.                                            IF1174.2
094900     GO TO    F-LOG10-WRITE-29.                                   IF1174.2
095000 F-LOG10-WRITE-29.                                                IF1174.2
095100     MOVE "F-LOG10-29" TO PAR-NAME.                               IF1174.2
095200     PERFORM  PRINT-DETAIL.                                       IF1174.2
095300*****************TEST (n) - COMPLEX TEST****************          IF1174.2
095400 F-LOG10-30.                                                      IF1174.2
095500     MOVE ZERO TO WS-NUM.                                         IF1174.2
095600     MOVE 4.84490 TO MIN-RANGE.                                   IF1174.2
095700     MOVE 4.84529 TO MAX-RANGE.                                   IF1174.2
095800 F-LOG10-TEST-30.                                                 IF1174.2
095900     COMPUTE WS-NUM = FUNCTION LOG10(B) + 4.                      IF1174.2
096000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
096100        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
096200                    PERFORM PASS                                  IF1174.2
096300     ELSE                                                         IF1174.2
096400                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
096500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
096600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
096700                    PERFORM FAIL.                                 IF1174.2
096800     GO TO F-LOG10-WRITE-30.                                      IF1174.2
096900 F-LOG10-DELETE-30.                                               IF1174.2
097000     PERFORM  DE-LETE.                                            IF1174.2
097100     GO TO    F-LOG10-WRITE-30.                                   IF1174.2
097200 F-LOG10-WRITE-30.                                                IF1174.2
097300     MOVE "F-LOG10-30" TO PAR-NAME.                               IF1174.2
097400     PERFORM  PRINT-DETAIL.                                       IF1174.2
097500*****************TEST (o) - COMPLEX TEST****************          IF1174.2
097600 F-LOG10-31.                                                      IF1174.2
097700     MOVE ZERO TO WS-NUM.                                         IF1174.2
097800     MOVE -0.521411 TO MIN-RANGE.                                 IF1174.2
097900     MOVE -0.521369 TO MAX-RANGE.                                 IF1174.2
098000 F-LOG10-TEST-31.                                                 IF1174.2
098100     COMPUTE WS-NUM = FUNCTION LOG10(FUNCTION LOG10(2)).          IF1174.2
098200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
098300        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
098400                    PERFORM PASS                                  IF1174.2
098500     ELSE                                                         IF1174.2
098600                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
098700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
098800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
098900                    PERFORM FAIL.                                 IF1174.2
099000     GO TO F-LOG10-WRITE-31.                                      IF1174.2
099100 F-LOG10-DELETE-31.                                               IF1174.2
099200     PERFORM  DE-LETE.                                            IF1174.2
099300     GO TO    F-LOG10-WRITE-31.                                   IF1174.2
099400 F-LOG10-WRITE-31.                                                IF1174.2
099500     MOVE "F-LOG10-31" TO PAR-NAME.                               IF1174.2
099600     PERFORM  PRINT-DETAIL.                                       IF1174.2
099700*****************TEST (p) - COMPLEX TEST****************          IF1174.2
099800 F-LOG10-32.                                                      IF1174.2
099900     MOVE ZERO TO WS-NUM.                                         IF1174.2
100000     MOVE -0.000040 TO MIN-RANGE.                                 IF1174.2
100100     MOVE  0.000040 TO MAX-RANGE.                                 IF1174.2
100200 F-LOG10-TEST-32.                                                 IF1174.2
100300     COMPUTE WS-NUM = FUNCTION LOG10(1) +                         IF1174.2
100400                      FUNCTION LOG10(1).                          IF1174.2
100500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1174.2
100600        (WS-NUM <= MAX-RANGE) THEN                                IF1174.2
100700                    PERFORM PASS                                  IF1174.2
100800     ELSE                                                         IF1174.2
100900                    MOVE WS-NUM TO COMPUTED-N                     IF1174.2
101000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1174.2
101100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1174.2
101200                    PERFORM FAIL.                                 IF1174.2
101300     GO TO F-LOG10-WRITE-32.                                      IF1174.2
101400 F-LOG10-DELETE-32.                                               IF1174.2
101500     PERFORM  DE-LETE.                                            IF1174.2
101600     GO TO    F-LOG10-WRITE-32.                                   IF1174.2
101700 F-LOG10-WRITE-32.                                                IF1174.2
101800     MOVE "F-LOG10-32" TO PAR-NAME.                               IF1174.2
101900     PERFORM  PRINT-DETAIL.                                       IF1174.2
102000*****************SPECIAL PERFORM TEST**********************       IF1174.2
102100 F-LOG10-33.                                                      IF1174.2
102200     PERFORM F-LOG10-TEST-33                                      IF1174.2
102300       UNTIL FUNCTION LOG10(ARG1) < 0.30.                         IF1174.2
102400     PERFORM PASS.                                                IF1174.2
102500     GO TO F-LOG10-WRITE-33.                                      IF1174.2
102600 F-LOG10-TEST-33.                                                 IF1174.2
102700     COMPUTE ARG1 = ARG1 - 1.00.                                  IF1174.2
102800 F-LOG10-DELETE-33.                                               IF1174.2
102900     PERFORM  DE-LETE.                                            IF1174.2
103000     GO TO    F-LOG10-WRITE-33.                                   IF1174.2
103100 F-LOG10-WRITE-33.                                                IF1174.2
103200     MOVE "F-LOG10-33" TO PAR-NAME.                               IF1174.2
103300     PERFORM  PRINT-DETAIL.                                       IF1174.2
103400********************END OF TESTS***************                   IF1174.2
103500 CCVS-EXIT SECTION.                                               IF1174.2
103600 CCVS-999999.                                                     IF1174.2
103700     GO TO CLOSE-FILES.                                           IF1174.2
      *END-OF,IF117A                                                                  
