      *HEADER,COBOL,IF137A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1374.2
000200 PROGRAM-ID.                                                      IF1374.2
000300     IF137A.                                                      IF1374.2
000400                                                                  IF1374.2
000500***********************************************************       IF1374.2
000600*                                                         *       IF1374.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1374.2
000800* It contains tests for the Intrinsic Function            *       IF1374.2
000900* STANDARD-DEVIATION.                                     *       IF1374.2
001000*                                                         *       IF1374.2
001100***********************************************************       IF1374.2
001200 ENVIRONMENT DIVISION.                                            IF1374.2
001300 CONFIGURATION SECTION.                                           IF1374.2
001400 SOURCE-COMPUTER.                                                 IF1374.2
001500     XXXXX082.                                                    IF1374.2
001600 OBJECT-COMPUTER.                                                 IF1374.2
001700     XXXXX083.                                                    IF1374.2
001800 INPUT-OUTPUT SECTION.                                            IF1374.2
001900 FILE-CONTROL.                                                    IF1374.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1374.2
002100     XXXXX055.                                                    IF1374.2
002200 DATA DIVISION.                                                   IF1374.2
002300 FILE SECTION.                                                    IF1374.2
002400 FD  PRINT-FILE.                                                  IF1374.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1374.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1374.2
002700 WORKING-STORAGE SECTION.                                         IF1374.2
002800***********************************************************       IF1374.2
002900* Variables specific to the Intrinsic Function Test IF137A*       IF1374.2
003000***********************************************************       IF1374.2
003100 01  A                   PIC S9(10)          VALUE 5.             IF1374.2
003200 01  B                   PIC S9(10)          VALUE 7.             IF1374.2
003300 01  C                   PIC S9(10)          VALUE -4.            IF1374.2
003400 01  D                   PIC S9(10)          VALUE 10.            IF1374.2
003500 01  E                   PIC S9(5)V9(5)      VALUE 34.26.         IF1374.2
003600 01  F                   PIC S9(5)V9(5)      VALUE -8.32.         IF1374.2
003700 01  G                   PIC S9(5)V9(5)      VALUE 4.08.          IF1374.2
003800 01  H                   PIC S9(5)V9(5)      VALUE -5.3.          IF1374.2
003900 01  P                   PIC S9(10)          VALUE 4.             IF1374.2
004000 01  Q                   PIC S9(10)          VALUE 3.             IF1374.2
004100 01  R                   PIC S9(10)          VALUE 5.             IF1374.2
004200 01  ARG3                PIC S9(10)          VALUE 2.             IF1374.2
004300 01  ARR                                     VALUE "40537".       IF1374.2
004400     02  IND OCCURS 5 TIMES PIC 9.                                IF1374.2
004500 01  TEMP                PIC S9(10).                              IF1374.2
004600 01  WS-NUM              PIC S9(5)V9(6).                          IF1374.2
004700 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1374.2
004800 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1374.2
004900*                                                                 IF1374.2
005000**********************************************************        IF1374.2
005100*                                                                 IF1374.2
005200 01  TEST-RESULTS.                                                IF1374.2
005300     02 FILLER                   PIC X      VALUE SPACE.          IF1374.2
005400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1374.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IF1374.2
005600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1374.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IF1374.2
005800     02  PAR-NAME.                                                IF1374.2
005900       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1374.2
006000       03  PARDOT-X              PIC X      VALUE SPACE.          IF1374.2
006100       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1374.2
006200     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1374.2
006300     02 RE-MARK                  PIC X(61).                       IF1374.2
006400 01  TEST-COMPUTED.                                               IF1374.2
006500     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1374.2
006600     02 FILLER                   PIC X(17)  VALUE                 IF1374.2
006700            "       COMPUTED=".                                   IF1374.2
006800     02 COMPUTED-X.                                               IF1374.2
006900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1374.2
007000     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1374.2
007100                                 PIC -9(9).9(9).                  IF1374.2
007200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1374.2
007300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1374.2
007400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1374.2
007500     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1374.2
007600         04 COMPUTED-18V0                    PIC -9(18).          IF1374.2
007700         04 FILLER                           PIC X.               IF1374.2
007800     03 FILLER PIC X(50) VALUE SPACE.                             IF1374.2
007900 01  TEST-CORRECT.                                                IF1374.2
008000     02 FILLER PIC X(30) VALUE SPACE.                             IF1374.2
008100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1374.2
008200     02 CORRECT-X.                                                IF1374.2
008300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1374.2
008400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1374.2
008500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1374.2
008600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1374.2
008700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1374.2
008800     03      CR-18V0 REDEFINES CORRECT-A.                         IF1374.2
008900         04 CORRECT-18V0                     PIC -9(18).          IF1374.2
009000         04 FILLER                           PIC X.               IF1374.2
009100     03 FILLER PIC X(2) VALUE SPACE.                              IF1374.2
009200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1374.2
009300 01  TEST-CORRECT-MIN.                                            IF1374.2
009400     02 FILLER PIC X(30) VALUE SPACE.                             IF1374.2
009500     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1374.2
009600     02 CORRECTMI-X.                                              IF1374.2
009700     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1374.2
009800     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1374.2
009900     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1374.2
010000     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1374.2
010100     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1374.2
010200     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1374.2
010300         04 CORRECTMI-18V0                     PIC -9(18).        IF1374.2
010400         04 FILLER                           PIC X.               IF1374.2
010500     03 FILLER PIC X(2) VALUE SPACE.                              IF1374.2
010600     03 FILLER                           PIC X(48) VALUE SPACE.   IF1374.2
010700 01  TEST-CORRECT-MAX.                                            IF1374.2
010800     02 FILLER PIC X(30) VALUE SPACE.                             IF1374.2
010900     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1374.2
011000     02 CORRECTMA-X.                                              IF1374.2
011100     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1374.2
011200     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1374.2
011300     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1374.2
011400     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1374.2
011500     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1374.2
011600     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1374.2
011700         04 CORRECTMA-18V0                     PIC -9(18).        IF1374.2
011800         04 FILLER                           PIC X.               IF1374.2
011900     03 FILLER PIC X(2) VALUE SPACE.                              IF1374.2
012000     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1374.2
012100 01  CCVS-C-1.                                                    IF1374.2
012200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1374.2
012300-    "SS  PARAGRAPH-NAME                                          IF1374.2
012400-    "       REMARKS".                                            IF1374.2
012500     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1374.2
012600 01  CCVS-C-2.                                                    IF1374.2
012700     02 FILLER                     PIC X        VALUE SPACE.      IF1374.2
012800     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1374.2
012900     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1374.2
013000     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1374.2
013100     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1374.2
013200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1374.2
013300 01  REC-CT                        PIC 99       VALUE ZERO.       IF1374.2
013400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1374.2
013500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1374.2
013600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1374.2
013700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1374.2
013800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1374.2
013900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1374.2
014000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1374.2
014100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1374.2
014200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1374.2
014300 01  CCVS-H-1.                                                    IF1374.2
014400     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1374.2
014500     02  FILLER                    PIC X(42)    VALUE             IF1374.2
014600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1374.2
014700     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1374.2
014800 01  CCVS-H-2A.                                                   IF1374.2
014900   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1374.2
015000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1374.2
015100   02  FILLER                        PIC XXXX   VALUE             IF1374.2
015200     "4.2 ".                                                      IF1374.2
015300   02  FILLER                        PIC X(28)  VALUE             IF1374.2
015400            " COPY - NOT FOR DISTRIBUTION".                       IF1374.2
015500   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1374.2
015600                                                                  IF1374.2
015700 01  CCVS-H-2B.                                                   IF1374.2
015800   02  FILLER                        PIC X(15)  VALUE             IF1374.2
015900            "TEST RESULT OF ".                                    IF1374.2
016000   02  TEST-ID                       PIC X(9).                    IF1374.2
016100   02  FILLER                        PIC X(4)   VALUE             IF1374.2
016200            " IN ".                                               IF1374.2
016300   02  FILLER                        PIC X(12)  VALUE             IF1374.2
016400     " HIGH       ".                                              IF1374.2
016500   02  FILLER                        PIC X(22)  VALUE             IF1374.2
016600            " LEVEL VALIDATION FOR ".                             IF1374.2
016700   02  FILLER                        PIC X(58)  VALUE             IF1374.2
016800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1374.2
016900 01  CCVS-H-3.                                                    IF1374.2
017000     02  FILLER                      PIC X(34)  VALUE             IF1374.2
017100            " FOR OFFICIAL USE ONLY    ".                         IF1374.2
017200     02  FILLER                      PIC X(58)  VALUE             IF1374.2
017300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1374.2
017400     02  FILLER                      PIC X(28)  VALUE             IF1374.2
017500            "  COPYRIGHT   1985 ".                                IF1374.2
017600 01  CCVS-E-1.                                                    IF1374.2
017700     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1374.2
017800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1374.2
017900     02 ID-AGAIN                     PIC X(9).                    IF1374.2
018000     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1374.2
018100 01  CCVS-E-2.                                                    IF1374.2
018200     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1374.2
018300     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1374.2
018400     02 CCVS-E-2-2.                                               IF1374.2
018500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1374.2
018600         03 FILLER                   PIC X      VALUE SPACE.      IF1374.2
018700         03 ENDER-DESC               PIC X(44)  VALUE             IF1374.2
018800            "ERRORS ENCOUNTERED".                                 IF1374.2
018900 01  CCVS-E-3.                                                    IF1374.2
019000     02  FILLER                      PIC X(22)  VALUE             IF1374.2
019100            " FOR OFFICIAL USE ONLY".                             IF1374.2
019200     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1374.2
019300     02  FILLER                      PIC X(58)  VALUE             IF1374.2
019400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1374.2
019500     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1374.2
019600     02 FILLER                       PIC X(15)  VALUE             IF1374.2
019700             " COPYRIGHT 1985".                                   IF1374.2
019800 01  CCVS-E-4.                                                    IF1374.2
019900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1374.2
020000     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1374.2
020100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1374.2
020200     02 FILLER                       PIC X(40)  VALUE             IF1374.2
020300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1374.2
020400 01  XXINFO.                                                      IF1374.2
020500     02 FILLER                       PIC X(19)  VALUE             IF1374.2
020600            "*** INFORMATION ***".                                IF1374.2
020700     02 INFO-TEXT.                                                IF1374.2
020800       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1374.2
020900       04 XXCOMPUTED                 PIC X(20).                   IF1374.2
021000       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1374.2
021100       04 XXCORRECT                  PIC X(20).                   IF1374.2
021200     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1374.2
021300 01  HYPHEN-LINE.                                                 IF1374.2
021400     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1374.2
021500     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1374.2
021600-    "*****************************************".                 IF1374.2
021700     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1374.2
021800-    "******************************".                            IF1374.2
021900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1374.2
022000     "IF137A".                                                    IF1374.2
022100 PROCEDURE DIVISION.                                              IF1374.2
022200 CCVS1 SECTION.                                                   IF1374.2
022300 OPEN-FILES.                                                      IF1374.2
022400     OPEN     OUTPUT PRINT-FILE.                                  IF1374.2
022500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1374.2
022600     MOVE    SPACE TO TEST-RESULTS.                               IF1374.2
022700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1374.2
022800     GO TO CCVS1-EXIT.                                            IF1374.2
022900 CLOSE-FILES.                                                     IF1374.2
023000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1374.2
023100 TERMINATE-CCVS.                                                  IF1374.2
023200     STOP     RUN.                                                IF1374.2
023300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1374.2
023400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1374.2
023500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1374.2
023600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1374.2
023700     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1374.2
023800 PRINT-DETAIL.                                                    IF1374.2
023900     IF REC-CT NOT EQUAL TO ZERO                                  IF1374.2
024000             MOVE "." TO PARDOT-X                                 IF1374.2
024100             MOVE REC-CT TO DOTVALUE.                             IF1374.2
024200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1374.2
024300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1374.2
024400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1374.2
024500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1374.2
024600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1374.2
024700     MOVE SPACE TO CORRECT-X.                                     IF1374.2
024800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1374.2
024900     MOVE     SPACE TO RE-MARK.                                   IF1374.2
025000 HEAD-ROUTINE.                                                    IF1374.2
025100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1374.2
025200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1374.2
025300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1374.2
025400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1374.2
025500 COLUMN-NAMES-ROUTINE.                                            IF1374.2
025600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1374.2
025700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1374.2
025800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1374.2
025900 END-ROUTINE.                                                     IF1374.2
026000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1374.2
026100 END-RTN-EXIT.                                                    IF1374.2
026200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1374.2
026300 END-ROUTINE-1.                                                   IF1374.2
026400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1374.2
026500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1374.2
026600      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1374.2
026700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1374.2
026800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1374.2
026900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1374.2
027000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1374.2
027100  END-ROUTINE-12.                                                 IF1374.2
027200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1374.2
027300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1374.2
027400         MOVE "NO " TO ERROR-TOTAL                                IF1374.2
027500         ELSE                                                     IF1374.2
027600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1374.2
027700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1374.2
027800     PERFORM WRITE-LINE.                                          IF1374.2
027900 END-ROUTINE-13.                                                  IF1374.2
028000     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1374.2
028100         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1374.2
028200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1374.2
028300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1374.2
028400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1374.2
028500      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1374.2
028600          MOVE "NO " TO ERROR-TOTAL                               IF1374.2
028700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1374.2
028800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1374.2
028900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1374.2
029000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1374.2
029100 WRITE-LINE.                                                      IF1374.2
029200     ADD 1 TO RECORD-COUNT.                                       IF1374.2
029300*    IF RECORD-COUNT GREATER 42                                   IF1374.2
029400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1374.2
029500*        MOVE SPACE TO DUMMY-RECORD                               IF1374.2
029600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1374.2
029700*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1374.2
029800*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1374.2
029900*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1374.2
030000*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1374.2
030100*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1374.2
030200*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1374.2
030300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1374.2
030400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1374.2
030500*        MOVE ZERO TO RECORD-COUNT.                               IF1374.2
030600     PERFORM WRT-LN.                                              IF1374.2
030700 WRT-LN.                                                          IF1374.2
030800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1374.2
030900     MOVE SPACE TO DUMMY-RECORD.                                  IF1374.2
031000 BLANK-LINE-PRINT.                                                IF1374.2
031100     PERFORM WRT-LN.                                              IF1374.2
031200 FAIL-ROUTINE.                                                    IF1374.2
031300     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1374.2
031400            GO TO FAIL-ROUTINE-WRITE.                             IF1374.2
031500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1374.2
031600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1374.2
031700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1374.2
031800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1374.2
031900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1374.2
032000     GO TO  FAIL-ROUTINE-EX.                                      IF1374.2
032100 FAIL-ROUTINE-WRITE.                                              IF1374.2
032200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1374.2
032300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1374.2
032400                              CORMA-ANSI-REFERENCE.               IF1374.2
032500     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1374.2
032600           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1374.2
032700           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1374.2
032800     ELSE                                                         IF1374.2
032900           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1374.2
033000     PERFORM WRITE-LINE.                                          IF1374.2
033100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1374.2
033200 FAIL-ROUTINE-EX. EXIT.                                           IF1374.2
033300 BAIL-OUT.                                                        IF1374.2
033400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1374.2
033500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1374.2
033600 BAIL-OUT-WRITE.                                                  IF1374.2
033700     MOVE CORRECT-A TO XXCORRECT.                                 IF1374.2
033800     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1374.2
033900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1374.2
034000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1374.2
034100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1374.2
034200 BAIL-OUT-EX. EXIT.                                               IF1374.2
034300 CCVS1-EXIT.                                                      IF1374.2
034400     EXIT.                                                        IF1374.2
034500********************************************************          IF1374.2
034600*                                                      *          IF1374.2
034700* Intrinsic Function Tests IF137A - STANDARD-DEVIATION *          IF1374.2
034800*                                                      *          IF1374.2
034900********************************************************          IF1374.2
035000 SECT-IF137A SECTION.                                             IF1374.2
035100 F-STD-DEV-INFO.                                                  IF1374.2
035200     MOVE     "See ref. A-70 2.41" TO ANSI-REFERENCE.             IF1374.2
035300     MOVE     "STANDARD-DEVIATION" TO FEATURE.                    IF1374.2
035400*****************TEST (a) - SIMPLE TEST*****************          IF1374.2
035500 F-STD-DEV-01.                                                    IF1374.2
035600     MOVE ZERO TO WS-NUM.                                         IF1374.2
035700     MOVE  6.97750 TO MIN-RANGE.                                  IF1374.2
035800     MOVE  6.97778 TO MAX-RANGE.                                  IF1374.2
035900 F-STD-DEV-TEST-01.                                               IF1374.2
036000     COMPUTE WS-NUM =                                             IF1374.2
036100            FUNCTION STANDARD-DEVIATION(5, -2, -14, 0).           IF1374.2
036200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
036300        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
036400                    PERFORM PASS                                  IF1374.2
036500     ELSE                                                         IF1374.2
036600                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
036700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
036800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
036900                    PERFORM FAIL.                                 IF1374.2
037000     GO TO F-STD-DEV-WRITE-01.                                    IF1374.2
037100 F-STD-DEV-DELETE-01.                                             IF1374.2
037200     PERFORM  DE-LETE.                                            IF1374.2
037300     GO TO    F-STD-DEV-WRITE-01.                                 IF1374.2
037400 F-STD-DEV-WRITE-01.                                              IF1374.2
037500     MOVE "F-STD-DEV-01" TO PAR-NAME.                             IF1374.2
037600     PERFORM  PRINT-DETAIL.                                       IF1374.2
037700*****************TEST (b) - SIMPLE TEST*****************          IF1374.2
037800 F-STD-DEV-02.                                                    IF1374.2
037900     EVALUATE FUNCTION STANDARD-DEVIATION(3.9, -0.3, 8.7, 100.2)  IF1374.2
038000     WHEN  41.7333 THRU  41.7350                                  IF1374.2
038100                    PERFORM PASS                                  IF1374.2
038200     WHEN OTHER                                                   IF1374.2
038300                    PERFORM FAIL.                                 IF1374.2
038400     GO TO F-STD-DEV-WRITE-02.                                    IF1374.2
038500 F-STD-DEV-DELETE-02.                                             IF1374.2
038600     PERFORM  DE-LETE.                                            IF1374.2
038700     GO TO    F-STD-DEV-WRITE-02.                                 IF1374.2
038800 F-STD-DEV-WRITE-02.                                              IF1374.2
038900     MOVE "F-STD-DEV-02" TO PAR-NAME.                             IF1374.2
039000     PERFORM  PRINT-DETAIL.                                       IF1374.2
039100*****************TEST (c) - SIMPLE TEST*****************          IF1374.2
039200 F-STD-DEV-03.                                                    IF1374.2
039300     MOVE  5.22005 TO MIN-RANGE.                                  IF1374.2
039400     MOVE  5.22025 TO MAX-RANGE.                                  IF1374.2
039500 F-STD-DEV-TEST-03.                                               IF1374.2
039600     IF (FUNCTION STANDARD-DEVIATION(A, B, C, D)                  IF1374.2
039700                                   >= MIN-RANGE) AND              IF1374.2
039800        (FUNCTION STANDARD-DEVIATION(A, B, C, D)                  IF1374.2
039900                                   <= MAX-RANGE) THEN             IF1374.2
040000                    PERFORM PASS                                  IF1374.2
040100     ELSE                                                         IF1374.2
040200                    PERFORM FAIL.                                 IF1374.2
040300     GO TO F-STD-DEV-WRITE-03.                                    IF1374.2
040400 F-STD-DEV-DELETE-03.                                             IF1374.2
040500     PERFORM  DE-LETE.                                            IF1374.2
040600     GO TO    F-STD-DEV-WRITE-03.                                 IF1374.2
040700 F-STD-DEV-WRITE-03.                                              IF1374.2
040800     MOVE "F-STD-DEV-03" TO PAR-NAME.                             IF1374.2
040900     PERFORM  PRINT-DETAIL.                                       IF1374.2
041000*****************TEST (d) - SIMPLE TEST*****************          IF1374.2
041100 F-STD-DEV-04.                                                    IF1374.2
041200     MOVE ZERO TO WS-NUM.                                         IF1374.2
041300     MOVE  16.8440 TO MIN-RANGE.                                  IF1374.2
041400     MOVE  16.8447 TO MAX-RANGE.                                  IF1374.2
041500 F-STD-DEV-TEST-04.                                               IF1374.2
041600     COMPUTE WS-NUM =                                             IF1374.2
041700               FUNCTION STANDARD-DEVIATION(E, F, G, H).           IF1374.2
041800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
041900        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
042000                    PERFORM PASS                                  IF1374.2
042100     ELSE                                                         IF1374.2
042200                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
042300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
042400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
042500                    PERFORM FAIL.                                 IF1374.2
042600     GO TO F-STD-DEV-WRITE-04.                                    IF1374.2
042700 F-STD-DEV-DELETE-04.                                             IF1374.2
042800     PERFORM  DE-LETE.                                            IF1374.2
042900     GO TO    F-STD-DEV-WRITE-04.                                 IF1374.2
043000 F-STD-DEV-WRITE-04.                                              IF1374.2
043100     MOVE "F-STD-DEV-04" TO PAR-NAME.                             IF1374.2
043200     PERFORM  PRINT-DETAIL.                                       IF1374.2
043300*****************TEST (e) - SIMPLE TEST*****************          IF1374.2
043400 F-STD-DEV-05.                                                    IF1374.2
043500     MOVE ZERO TO WS-NUM.                                         IF1374.2
043600     MOVE  9.73119 TO MIN-RANGE.                                  IF1374.2
043700     MOVE  9.73158 TO MAX-RANGE.                                  IF1374.2
043800 F-STD-DEV-TEST-05.                                               IF1374.2
043900     COMPUTE WS-NUM =                                             IF1374.2
044000       FUNCTION STANDARD-DEVIATION(10.2, -0.2, 5.6, -15.6).       IF1374.2
044100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
044200        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
044300                    PERFORM PASS                                  IF1374.2
044400     ELSE                                                         IF1374.2
044500                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
044600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
044700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
044800                    PERFORM FAIL.                                 IF1374.2
044900     GO TO F-STD-DEV-WRITE-05.                                    IF1374.2
045000 F-STD-DEV-DELETE-05.                                             IF1374.2
045100     PERFORM  DE-LETE.                                            IF1374.2
045200     GO TO    F-STD-DEV-WRITE-05.                                 IF1374.2
045300 F-STD-DEV-WRITE-05.                                              IF1374.2
045400     MOVE "F-STD-DEV-05" TO PAR-NAME.                             IF1374.2
045500     PERFORM  PRINT-DETAIL.                                       IF1374.2
045600*****************TEST (f) - SIMPLE TEST*****************          IF1374.2
045700 F-STD-DEV-06.                                                    IF1374.2
045800     MOVE ZERO TO WS-NUM.                                         IF1374.2
045900     MOVE  12.4976 TO MIN-RANGE.                                  IF1374.2
046000     MOVE  12.4981 TO MAX-RANGE.                                  IF1374.2
046100 F-STD-DEV-TEST-06.                                               IF1374.2
046200     COMPUTE WS-NUM =                                             IF1374.2
046300        FUNCTION STANDARD-DEVIATION(A, B, C, D, E, F, G, H).      IF1374.2
046400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
046500        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
046600                    PERFORM PASS                                  IF1374.2
046700     ELSE                                                         IF1374.2
046800                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
046900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
047000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
047100                    PERFORM FAIL.                                 IF1374.2
047200     GO TO F-STD-DEV-WRITE-06.                                    IF1374.2
047300 F-STD-DEV-DELETE-06.                                             IF1374.2
047400     PERFORM  DE-LETE.                                            IF1374.2
047500     GO TO    F-STD-DEV-WRITE-06.                                 IF1374.2
047600 F-STD-DEV-WRITE-06.                                              IF1374.2
047700     MOVE "F-STD-DEV-06" TO PAR-NAME.                             IF1374.2
047800     PERFORM  PRINT-DETAIL.                                       IF1374.2
047900*****************TEST (g) - SIMPLE TEST*****************          IF1374.2
048000 F-STD-DEV-07.                                                    IF1374.2
048100     MOVE ZERO TO WS-NUM.                                         IF1374.2
048200     MOVE  2.16020 TO MIN-RANGE.                                  IF1374.2
048300     MOVE  2.16028 TO MAX-RANGE.                                  IF1374.2
048400 F-STD-DEV-TEST-07.                                               IF1374.2
048500     COMPUTE WS-NUM =                                             IF1374.2
048600        FUNCTION STANDARD-DEVIATION(IND(1), IND(2), IND(3)).      IF1374.2
048700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
048800        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
048900                    PERFORM PASS                                  IF1374.2
049000     ELSE                                                         IF1374.2
049100                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
049200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
049300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
049400                    PERFORM FAIL.                                 IF1374.2
049500     GO TO F-STD-DEV-WRITE-07.                                    IF1374.2
049600 F-STD-DEV-DELETE-07.                                             IF1374.2
049700     PERFORM  DE-LETE.                                            IF1374.2
049800     GO TO    F-STD-DEV-WRITE-07.                                 IF1374.2
049900 F-STD-DEV-WRITE-07.                                              IF1374.2
050000     MOVE "F-STD-DEV-07" TO PAR-NAME.                             IF1374.2
050100     PERFORM  PRINT-DETAIL.                                       IF1374.2
050200*****************TEST (h) - SIMPLE TEST*****************          IF1374.2
050300 F-STD-DEV-08.                                                    IF1374.2
050400     MOVE ZERO TO WS-NUM.                                         IF1374.2
050500     MOVE  1.63296 TO MIN-RANGE.                                  IF1374.2
050600     MOVE  1.63302 TO MAX-RANGE.                                  IF1374.2
050700 F-STD-DEV-TEST-08.                                               IF1374.2
050800     COMPUTE WS-NUM =                                             IF1374.2
050900        FUNCTION STANDARD-DEVIATION(IND(P), IND(Q), IND(R)).      IF1374.2
051000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
051100        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
051200                    PERFORM PASS                                  IF1374.2
051300     ELSE                                                         IF1374.2
051400                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
051500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
051600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
051700                    PERFORM FAIL.                                 IF1374.2
051800     GO TO F-STD-DEV-WRITE-08.                                    IF1374.2
051900 F-STD-DEV-DELETE-08.                                             IF1374.2
052000     PERFORM  DE-LETE.                                            IF1374.2
052100     GO TO    F-STD-DEV-WRITE-08.                                 IF1374.2
052200 F-STD-DEV-WRITE-08.                                              IF1374.2
052300     MOVE "F-STD-DEV-08" TO PAR-NAME.                             IF1374.2
052400     PERFORM  PRINT-DETAIL.                                       IF1374.2
052500*****************TEST (i) - SIMPLE TEST*****************          IF1374.2
052600 F-STD-DEV-09.                                                    IF1374.2
052700     MOVE ZERO TO WS-NUM.                                         IF1374.2
052800     MOVE 2.31511 TO MIN-RANGE.                                   IF1374.2
052900     MOVE 2.31521 TO MAX-RANGE.                                   IF1374.2
053000 F-STD-DEV-TEST-09.                                               IF1374.2
053100     COMPUTE WS-NUM = FUNCTION STANDARD-DEVIATION(IND(ALL)).      IF1374.2
053200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
053300        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
053400                    PERFORM PASS                                  IF1374.2
053500     ELSE                                                         IF1374.2
053600                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
053700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
053800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
053900                    PERFORM FAIL.                                 IF1374.2
054000     GO TO F-STD-DEV-WRITE-09.                                    IF1374.2
054100 F-STD-DEV-DELETE-09.                                             IF1374.2
054200     PERFORM  DE-LETE.                                            IF1374.2
054300     GO TO    F-STD-DEV-WRITE-09.                                 IF1374.2
054400 F-STD-DEV-WRITE-09.                                              IF1374.2
054500     MOVE "F-STD-DEV-09" TO PAR-NAME.                             IF1374.2
054600     PERFORM  PRINT-DETAIL.                                       IF1374.2
054700*****************TEST (j) - SIMPLE TEST*****************          IF1374.2
054800 F-STD-DEV-10.                                                    IF1374.2
054900     MOVE ZERO TO WS-NUM.                                         IF1374.2
055000     MOVE  0.028559  TO MIN-RANGE.                                IF1374.2
055100     MOVE  0.028561  TO MAX-RANGE.                                IF1374.2
055200 F-STD-DEV-TEST-10.                                               IF1374.2
055300     COMPUTE WS-NUM =                                             IF1374.2
055400        FUNCTION STANDARD-DEVIATION(0.00032, 0.00019,             IF1374.2
055500                                    0.00014, -0.06574).           IF1374.2
055600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
055700        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
055800                    PERFORM PASS                                  IF1374.2
055900     ELSE                                                         IF1374.2
056000                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
056100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
056200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
056300                    PERFORM FAIL.                                 IF1374.2
056400     GO TO F-STD-DEV-WRITE-10.                                    IF1374.2
056500 F-STD-DEV-DELETE-10.                                             IF1374.2
056600     PERFORM  DE-LETE.                                            IF1374.2
056700     GO TO    F-STD-DEV-WRITE-10.                                 IF1374.2
056800 F-STD-DEV-WRITE-10.                                              IF1374.2
056900     MOVE "F-STD-DEV-10" TO PAR-NAME.                             IF1374.2
057000     PERFORM  PRINT-DETAIL.                                       IF1374.2
057100*****************TEST (k) - SIMPLE TEST*****************          IF1374.2
057200 F-STD-DEV-11.                                                    IF1374.2
057300     MOVE ZERO TO WS-NUM.                                         IF1374.2
057400     MOVE -0.000020 TO MIN-RANGE.                                 IF1374.2
057500     MOVE  0.000020 TO MAX-RANGE.                                 IF1374.2
057600 F-STD-DEV-TEST-11.                                               IF1374.2
057700     COMPUTE WS-NUM = FUNCTION STANDARD-DEVIATION(A, 5, A).       IF1374.2
057800                                                                  IF1374.2
057900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
058000        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
058100                    PERFORM PASS                                  IF1374.2
058200     ELSE                                                         IF1374.2
058300                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
058400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
058500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
058600                    PERFORM FAIL.                                 IF1374.2
058700     GO TO F-STD-DEV-WRITE-11.                                    IF1374.2
058800 F-STD-DEV-DELETE-11.                                             IF1374.2
058900     PERFORM  DE-LETE.                                            IF1374.2
059000     GO TO    F-STD-DEV-WRITE-11.                                 IF1374.2
059100 F-STD-DEV-WRITE-11.                                              IF1374.2
059200     MOVE "F-STD-DEV-11" TO PAR-NAME.                             IF1374.2
059300     PERFORM  PRINT-DETAIL.                                       IF1374.2
059400*****************TEST (a) - COMPLEX TEST****************          IF1374.2
059500 F-STD-DEV-12.                                                    IF1374.2
059600     MOVE ZERO TO WS-NUM.                                         IF1374.2
059700     MOVE  11.7995 TO MIN-RANGE.                                  IF1374.2
059800     MOVE  11.8005 TO MAX-RANGE.                                  IF1374.2
059900 F-STD-DEV-TEST-12.                                               IF1374.2
060000     COMPUTE WS-NUM =                                             IF1374.2
060100        FUNCTION STANDARD-DEVIATION(2.6 + 30, 4.5 * 2).           IF1374.2
060200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
060300        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
060400                    PERFORM PASS                                  IF1374.2
060500     ELSE                                                         IF1374.2
060600                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
060700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
060800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
060900                    PERFORM FAIL.                                 IF1374.2
061000     GO TO F-STD-DEV-WRITE-12.                                    IF1374.2
061100 F-STD-DEV-DELETE-12.                                             IF1374.2
061200     PERFORM  DE-LETE.                                            IF1374.2
061300     GO TO    F-STD-DEV-WRITE-12.                                 IF1374.2
061400 F-STD-DEV-WRITE-12.                                              IF1374.2
061500     MOVE "F-STD-DEV-12" TO PAR-NAME.                             IF1374.2
061600     PERFORM  PRINT-DETAIL.                                       IF1374.2
061700*****************TEST (b) - COMPLEX TEST****************          IF1374.2
061800 F-STD-DEV-13.                                                    IF1374.2
061900     MOVE ZERO TO WS-NUM.                                         IF1374.2
062000     MOVE  19.3556 TO MIN-RANGE.                                  IF1374.2
062100     MOVE  19.3572 TO MAX-RANGE.                                  IF1374.2
062200 F-STD-DEV-TEST-13.                                               IF1374.2
062300     COMPUTE WS-NUM =                                             IF1374.2
062400         FUNCTION STANDARD-DEVIATION(E, 9 * A, 0, B / 2).         IF1374.2
062500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
062600        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
062700                    PERFORM PASS                                  IF1374.2
062800     ELSE                                                         IF1374.2
062900                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
063000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
063100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
063200                    PERFORM FAIL.                                 IF1374.2
063300     GO TO F-STD-DEV-WRITE-13.                                    IF1374.2
063400 F-STD-DEV-DELETE-13.                                             IF1374.2
063500     PERFORM  DE-LETE.                                            IF1374.2
063600     GO TO    F-STD-DEV-WRITE-13.                                 IF1374.2
063700 F-STD-DEV-WRITE-13.                                              IF1374.2
063800     MOVE "F-STD-DEV-13" TO PAR-NAME.                             IF1374.2
063900     PERFORM  PRINT-DETAIL.                                       IF1374.2
064000*****************TEST (c) - COMPLEX TEST****************          IF1374.2
064100 F-STD-DEV-14.                                                    IF1374.2
064200     MOVE ZERO TO WS-NUM.                                         IF1374.2
064300     MOVE 77.9969 TO MIN-RANGE.                                   IF1374.2
064400     MOVE 78.0031 TO MAX-RANGE.                                   IF1374.2
064500 F-STD-DEV-TEST-14.                                               IF1374.2
064600     COMPUTE WS-NUM = FUNCTION STANDARD-DEVIATION(A) + 78.        IF1374.2
064700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
064800        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
064900                    PERFORM PASS                                  IF1374.2
065000     ELSE                                                         IF1374.2
065100                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
065200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
065300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
065400                    PERFORM FAIL.                                 IF1374.2
065500     GO TO F-STD-DEV-WRITE-14.                                    IF1374.2
065600 F-STD-DEV-DELETE-14.                                             IF1374.2
065700     PERFORM  DE-LETE.                                            IF1374.2
065800     GO TO    F-STD-DEV-WRITE-14.                                 IF1374.2
065900 F-STD-DEV-WRITE-14.                                              IF1374.2
066000     MOVE "F-STD-DEV-14" TO PAR-NAME.                             IF1374.2
066100     PERFORM  PRINT-DETAIL.                                       IF1374.2
066200*****************TEST (d) - COMPLEX TEST****************          IF1374.2
066300 F-STD-DEV-15.                                                    IF1374.2
066400     MOVE ZERO TO WS-NUM.                                         IF1374.2
066500     MOVE 0.99996 TO MIN-RANGE.                                   IF1374.2
066600     MOVE 1.00004 TO MAX-RANGE.                                   IF1374.2
066700 F-STD-DEV-TEST-15.                                               IF1374.2
066800     COMPUTE WS-NUM = FUNCTION STANDARD-DEVIATION(A, B) +         IF1374.2
066900                      FUNCTION STANDARD-DEVIATION(1, 1).          IF1374.2
067000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
067100        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
067200                    PERFORM PASS                                  IF1374.2
067300     ELSE                                                         IF1374.2
067400                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
067500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
067600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
067700                    PERFORM FAIL.                                 IF1374.2
067800     GO TO F-STD-DEV-WRITE-15.                                    IF1374.2
067900 F-STD-DEV-DELETE-15.                                             IF1374.2
068000     PERFORM  DE-LETE.                                            IF1374.2
068100     GO TO    F-STD-DEV-WRITE-15.                                 IF1374.2
068200 F-STD-DEV-WRITE-15.                                              IF1374.2
068300     MOVE "F-STD-DEV-15" TO PAR-NAME.                             IF1374.2
068400     PERFORM  PRINT-DETAIL.                                       IF1374.2
068500*****************TEST (e) - COMPLEX TEST****************          IF1374.2
068600 F-STD-DEV-16.                                                    IF1374.2
068700     MOVE ZERO TO WS-NUM.                                         IF1374.2
068800     MOVE -0.000040 TO MIN-RANGE.                                 IF1374.2
068900     MOVE  0.000040 TO MAX-RANGE.                                 IF1374.2
069000 F-STD-DEV-TEST-16.                                               IF1374.2
069100     COMPUTE WS-NUM = FUNCTION STANDARD-DEVIATION(                IF1374.2
069200                      FUNCTION STANDARD-DEVIATION(0, 0)).         IF1374.2
069300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1374.2
069400        (WS-NUM <= MAX-RANGE) THEN                                IF1374.2
069500                    PERFORM PASS                                  IF1374.2
069600     ELSE                                                         IF1374.2
069700                    MOVE WS-NUM TO COMPUTED-N                     IF1374.2
069800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1374.2
069900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1374.2
070000                    PERFORM FAIL.                                 IF1374.2
070100     GO TO F-STD-DEV-WRITE-16.                                    IF1374.2
070200 F-STD-DEV-DELETE-16.                                             IF1374.2
070300     PERFORM  DE-LETE.                                            IF1374.2
070400     GO TO    F-STD-DEV-WRITE-16.                                 IF1374.2
070500 F-STD-DEV-WRITE-16.                                              IF1374.2
070600     MOVE "F-STD-DEV-16" TO PAR-NAME.                             IF1374.2
070700     PERFORM  PRINT-DETAIL.                                       IF1374.2
070800*****************SPECIAL PERFORM TEST**********************       IF1374.2
070900 F-STD-DEV-17.                                                    IF1374.2
071000     PERFORM F-STD-DEV-TEST-17                                    IF1374.2
071100       UNTIL FUNCTION STANDARD-DEVIATION(1, 1, ARG3) > 1.         IF1374.2
071200     PERFORM PASS.                                                IF1374.2
071300     GO TO F-STD-DEV-WRITE-17.                                    IF1374.2
071400 F-STD-DEV-TEST-17.                                               IF1374.2
071500     COMPUTE ARG3 = ARG3 + 1.                                     IF1374.2
071600 F-STD-DEV-DELETE-17.                                             IF1374.2
071700     PERFORM  DE-LETE.                                            IF1374.2
071800     GO TO    F-STD-DEV-WRITE-17.                                 IF1374.2
071900 F-STD-DEV-WRITE-17.                                              IF1374.2
072000     MOVE "F-STD-DEV-17" TO PAR-NAME.                             IF1374.2
072100     PERFORM  PRINT-DETAIL.                                       IF1374.2
072200********************END OF TESTS***************                   IF1374.2
072300 CCVS-EXIT SECTION.                                               IF1374.2
072400 CCVS-999999.                                                     IF1374.2
072500     GO TO CLOSE-FILES.                                           IF1374.2
      *END-OF,IF137A                                                                  
