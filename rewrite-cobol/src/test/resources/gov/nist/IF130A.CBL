      *HEADER,COBOL,IF130A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1304.2
000200 PROGRAM-ID.                                                      IF1304.2
000300     IF130A.                                                      IF1304.2
000400                                                                  IF1304.2
000500***********************************************************       IF1304.2
000600*                                                         *       IF1304.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1304.2
000800* It contains tests for the Intrinsic Function            *       IF1304.2
000900* PRESENT-VALUE.                                          *       IF1304.2
001000*                                                         *       IF1304.2
001100***********************************************************       IF1304.2
001200 ENVIRONMENT DIVISION.                                            IF1304.2
001300 CONFIGURATION SECTION.                                           IF1304.2
001400 SOURCE-COMPUTER.                                                 IF1304.2
001500     XXXXX082.                                                    IF1304.2
001600 OBJECT-COMPUTER.                                                 IF1304.2
001700     XXXXX083.                                                    IF1304.2
001800 INPUT-OUTPUT SECTION.                                            IF1304.2
001900 FILE-CONTROL.                                                    IF1304.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1304.2
002100     XXXXX055.                                                    IF1304.2
002200 DATA DIVISION.                                                   IF1304.2
002300 FILE SECTION.                                                    IF1304.2
002400 FD  PRINT-FILE.                                                  IF1304.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1304.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1304.2
002700 WORKING-STORAGE SECTION.                                         IF1304.2
002800***********************************************************       IF1304.2
002900* Variables specific to the Intrinsic Function Test IF130A*       IF1304.2
003000***********************************************************       IF1304.2
003100 01  A                   PIC S9(10)          VALUE 5.             IF1304.2
003200 01  B                   PIC S9(10)          VALUE 7.             IF1304.2
003300 01  C                   PIC S9(10)          VALUE -4.            IF1304.2
003400 01  D                   PIC S9(10)          VALUE 10.            IF1304.2
003500 01  E                   PIC S9(5)V9(5)      VALUE 34.26.         IF1304.2
003600 01  F                   PIC S9(5)V9(5)      VALUE -8.32.         IF1304.2
003700 01  G                   PIC S9(5)V9(5)      VALUE 4.08.          IF1304.2
003800 01  H                   PIC S9(5)V9(5)      VALUE 5.3.           IF1304.2
003900 01  I                   PIC S9(5)V9(5)      VALUE 0.0009.        IF1304.2
004000 01  J                   PIC S9(5)V9(5)      VALUE 0.0008.        IF1304.2
004100 01  K                   PIC S9(10)          VALUE 23000.         IF1304.2
004200 01  L                   PIC S9(10)          VALUE -23000.        IF1304.2
004300 01  P                   PIC S9(10)          VALUE 1.             IF1304.2
004400 01  Q                   PIC S9(10)          VALUE 3.             IF1304.2
004500 01  R                   PIC S9(10)          VALUE 5.             IF1304.2
004600 01  ARG1                PIC S9(10)          VALUE 0.             IF1304.2
004700 01  ARR                                     VALUE "40537".       IF1304.2
004800     02  IND OCCURS 5 TIMES PIC 9.                                IF1304.2
004900 01  TEMP                PIC S9(10)V9(5).                         IF1304.2
005000 01  WS-NUM              PIC S9(5)V9(6).                          IF1304.2
005100 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1304.2
005200 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1304.2
005300*                                                                 IF1304.2
005400**********************************************************        IF1304.2
005500*                                                                 IF1304.2
005600 01  TEST-RESULTS.                                                IF1304.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IF1304.2
005800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1304.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IF1304.2
006000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1304.2
006100     02 FILLER                   PIC X      VALUE SPACE.          IF1304.2
006200     02  PAR-NAME.                                                IF1304.2
006300       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1304.2
006400       03  PARDOT-X              PIC X      VALUE SPACE.          IF1304.2
006500       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1304.2
006600     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1304.2
006700     02 RE-MARK                  PIC X(61).                       IF1304.2
006800 01  TEST-COMPUTED.                                               IF1304.2
006900     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1304.2
007000     02 FILLER                   PIC X(17)  VALUE                 IF1304.2
007100            "       COMPUTED=".                                   IF1304.2
007200     02 COMPUTED-X.                                               IF1304.2
007300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1304.2
007400     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1304.2
007500                                 PIC -9(9).9(9).                  IF1304.2
007600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1304.2
007700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1304.2
007800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1304.2
007900     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1304.2
008000         04 COMPUTED-18V0                    PIC -9(18).          IF1304.2
008100         04 FILLER                           PIC X.               IF1304.2
008200     03 FILLER PIC X(50) VALUE SPACE.                             IF1304.2
008300 01  TEST-CORRECT.                                                IF1304.2
008400     02 FILLER PIC X(30) VALUE SPACE.                             IF1304.2
008500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1304.2
008600     02 CORRECT-X.                                                IF1304.2
008700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1304.2
008800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1304.2
008900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1304.2
009000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1304.2
009100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1304.2
009200     03      CR-18V0 REDEFINES CORRECT-A.                         IF1304.2
009300         04 CORRECT-18V0                     PIC -9(18).          IF1304.2
009400         04 FILLER                           PIC X.               IF1304.2
009500     03 FILLER PIC X(2) VALUE SPACE.                              IF1304.2
009600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1304.2
009700 01  TEST-CORRECT-MIN.                                            IF1304.2
009800     02 FILLER PIC X(30) VALUE SPACE.                             IF1304.2
009900     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1304.2
010000     02 CORRECTMI-X.                                              IF1304.2
010100     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1304.2
010200     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1304.2
010300     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1304.2
010400     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1304.2
010500     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1304.2
010600     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1304.2
010700         04 CORRECTMI-18V0                     PIC -9(18).        IF1304.2
010800         04 FILLER                           PIC X.               IF1304.2
010900     03 FILLER PIC X(2) VALUE SPACE.                              IF1304.2
011000     03 FILLER                           PIC X(48) VALUE SPACE.   IF1304.2
011100 01  TEST-CORRECT-MAX.                                            IF1304.2
011200     02 FILLER PIC X(30) VALUE SPACE.                             IF1304.2
011300     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1304.2
011400     02 CORRECTMA-X.                                              IF1304.2
011500     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1304.2
011600     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1304.2
011700     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1304.2
011800     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1304.2
011900     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1304.2
012000     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1304.2
012100         04 CORRECTMA-18V0                     PIC -9(18).        IF1304.2
012200         04 FILLER                           PIC X.               IF1304.2
012300     03 FILLER PIC X(2) VALUE SPACE.                              IF1304.2
012400     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1304.2
012500 01  CCVS-C-1.                                                    IF1304.2
012600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1304.2
012700-    "SS  PARAGRAPH-NAME                                          IF1304.2
012800-    "       REMARKS".                                            IF1304.2
012900     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1304.2
013000 01  CCVS-C-2.                                                    IF1304.2
013100     02 FILLER                     PIC X        VALUE SPACE.      IF1304.2
013200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1304.2
013300     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1304.2
013400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1304.2
013500     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1304.2
013600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1304.2
013700 01  REC-CT                        PIC 99       VALUE ZERO.       IF1304.2
013800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1304.2
013900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1304.2
014000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1304.2
014100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1304.2
014200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1304.2
014300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1304.2
014400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1304.2
014500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1304.2
014600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1304.2
014700 01  CCVS-H-1.                                                    IF1304.2
014800     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1304.2
014900     02  FILLER                    PIC X(42)    VALUE             IF1304.2
015000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1304.2
015100     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1304.2
015200 01  CCVS-H-2A.                                                   IF1304.2
015300   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1304.2
015400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1304.2
015500   02  FILLER                        PIC XXXX   VALUE             IF1304.2
015600     "4.2 ".                                                      IF1304.2
015700   02  FILLER                        PIC X(28)  VALUE             IF1304.2
015800            " COPY - NOT FOR DISTRIBUTION".                       IF1304.2
015900   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1304.2
016000                                                                  IF1304.2
016100 01  CCVS-H-2B.                                                   IF1304.2
016200   02  FILLER                        PIC X(15)  VALUE             IF1304.2
016300            "TEST RESULT OF ".                                    IF1304.2
016400   02  TEST-ID                       PIC X(9).                    IF1304.2
016500   02  FILLER                        PIC X(4)   VALUE             IF1304.2
016600            " IN ".                                               IF1304.2
016700   02  FILLER                        PIC X(12)  VALUE             IF1304.2
016800     " HIGH       ".                                              IF1304.2
016900   02  FILLER                        PIC X(22)  VALUE             IF1304.2
017000            " LEVEL VALIDATION FOR ".                             IF1304.2
017100   02  FILLER                        PIC X(58)  VALUE             IF1304.2
017200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1304.2
017300 01  CCVS-H-3.                                                    IF1304.2
017400     02  FILLER                      PIC X(34)  VALUE             IF1304.2
017500            " FOR OFFICIAL USE ONLY    ".                         IF1304.2
017600     02  FILLER                      PIC X(58)  VALUE             IF1304.2
017700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1304.2
017800     02  FILLER                      PIC X(28)  VALUE             IF1304.2
017900            "  COPYRIGHT   1985 ".                                IF1304.2
018000 01  CCVS-E-1.                                                    IF1304.2
018100     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1304.2
018200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1304.2
018300     02 ID-AGAIN                     PIC X(9).                    IF1304.2
018400     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1304.2
018500 01  CCVS-E-2.                                                    IF1304.2
018600     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1304.2
018700     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1304.2
018800     02 CCVS-E-2-2.                                               IF1304.2
018900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1304.2
019000         03 FILLER                   PIC X      VALUE SPACE.      IF1304.2
019100         03 ENDER-DESC               PIC X(44)  VALUE             IF1304.2
019200            "ERRORS ENCOUNTERED".                                 IF1304.2
019300 01  CCVS-E-3.                                                    IF1304.2
019400     02  FILLER                      PIC X(22)  VALUE             IF1304.2
019500            " FOR OFFICIAL USE ONLY".                             IF1304.2
019600     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1304.2
019700     02  FILLER                      PIC X(58)  VALUE             IF1304.2
019800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1304.2
019900     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1304.2
020000     02 FILLER                       PIC X(15)  VALUE             IF1304.2
020100             " COPYRIGHT 1985".                                   IF1304.2
020200 01  CCVS-E-4.                                                    IF1304.2
020300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1304.2
020400     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1304.2
020500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1304.2
020600     02 FILLER                       PIC X(40)  VALUE             IF1304.2
020700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1304.2
020800 01  XXINFO.                                                      IF1304.2
020900     02 FILLER                       PIC X(19)  VALUE             IF1304.2
021000            "*** INFORMATION ***".                                IF1304.2
021100     02 INFO-TEXT.                                                IF1304.2
021200       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1304.2
021300       04 XXCOMPUTED                 PIC X(20).                   IF1304.2
021400       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1304.2
021500       04 XXCORRECT                  PIC X(20).                   IF1304.2
021600     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1304.2
021700 01  HYPHEN-LINE.                                                 IF1304.2
021800     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1304.2
021900     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1304.2
022000-    "*****************************************".                 IF1304.2
022100     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1304.2
022200-    "******************************".                            IF1304.2
022300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1304.2
022400     "IF130A".                                                    IF1304.2
022500 PROCEDURE DIVISION.                                              IF1304.2
022600 CCVS1 SECTION.                                                   IF1304.2
022700 OPEN-FILES.                                                      IF1304.2
022800     OPEN     OUTPUT PRINT-FILE.                                  IF1304.2
022900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1304.2
023000     MOVE    SPACE TO TEST-RESULTS.                               IF1304.2
023100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1304.2
023200     GO TO CCVS1-EXIT.                                            IF1304.2
023300 CLOSE-FILES.                                                     IF1304.2
023400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1304.2
023500 TERMINATE-CCVS.                                                  IF1304.2
023600     STOP     RUN.                                                IF1304.2
023700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1304.2
023800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1304.2
023900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1304.2
024000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1304.2
024100     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1304.2
024200 PRINT-DETAIL.                                                    IF1304.2
024300     IF REC-CT NOT EQUAL TO ZERO                                  IF1304.2
024400             MOVE "." TO PARDOT-X                                 IF1304.2
024500             MOVE REC-CT TO DOTVALUE.                             IF1304.2
024600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1304.2
024700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1304.2
024800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1304.2
024900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1304.2
025000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1304.2
025100     MOVE SPACE TO CORRECT-X.                                     IF1304.2
025200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1304.2
025300     MOVE     SPACE TO RE-MARK.                                   IF1304.2
025400 HEAD-ROUTINE.                                                    IF1304.2
025500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1304.2
025600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1304.2
025700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1304.2
025800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1304.2
025900 COLUMN-NAMES-ROUTINE.                                            IF1304.2
026000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1304.2
026100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1304.2
026200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1304.2
026300 END-ROUTINE.                                                     IF1304.2
026400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1304.2
026500 END-RTN-EXIT.                                                    IF1304.2
026600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1304.2
026700 END-ROUTINE-1.                                                   IF1304.2
026800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1304.2
026900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1304.2
027000      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1304.2
027100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1304.2
027200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1304.2
027300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1304.2
027400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1304.2
027500  END-ROUTINE-12.                                                 IF1304.2
027600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1304.2
027700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1304.2
027800         MOVE "NO " TO ERROR-TOTAL                                IF1304.2
027900         ELSE                                                     IF1304.2
028000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1304.2
028100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1304.2
028200     PERFORM WRITE-LINE.                                          IF1304.2
028300 END-ROUTINE-13.                                                  IF1304.2
028400     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1304.2
028500         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1304.2
028600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1304.2
028700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1304.2
028800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1304.2
028900      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1304.2
029000          MOVE "NO " TO ERROR-TOTAL                               IF1304.2
029100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1304.2
029200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1304.2
029300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1304.2
029400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1304.2
029500 WRITE-LINE.                                                      IF1304.2
029600     ADD 1 TO RECORD-COUNT.                                       IF1304.2
029700*    IF RECORD-COUNT GREATER 42                                   IF1304.2
029800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1304.2
029900*        MOVE SPACE TO DUMMY-RECORD                               IF1304.2
030000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1304.2
030100*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1304.2
030200*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1304.2
030300*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1304.2
030400*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1304.2
030500*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1304.2
030600*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1304.2
030700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1304.2
030800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1304.2
030900*        MOVE ZERO TO RECORD-COUNT.                               IF1304.2
031000     PERFORM WRT-LN.                                              IF1304.2
031100 WRT-LN.                                                          IF1304.2
031200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1304.2
031300     MOVE SPACE TO DUMMY-RECORD.                                  IF1304.2
031400 BLANK-LINE-PRINT.                                                IF1304.2
031500     PERFORM WRT-LN.                                              IF1304.2
031600 FAIL-ROUTINE.                                                    IF1304.2
031700     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1304.2
031800            GO TO FAIL-ROUTINE-WRITE.                             IF1304.2
031900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1304.2
032000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1304.2
032100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1304.2
032200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1304.2
032300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1304.2
032400     GO TO  FAIL-ROUTINE-EX.                                      IF1304.2
032500 FAIL-ROUTINE-WRITE.                                              IF1304.2
032600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1304.2
032700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1304.2
032800                              CORMA-ANSI-REFERENCE.               IF1304.2
032900     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1304.2
033000           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1304.2
033100           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1304.2
033200     ELSE                                                         IF1304.2
033300           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1304.2
033400     PERFORM WRITE-LINE.                                          IF1304.2
033500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1304.2
033600 FAIL-ROUTINE-EX. EXIT.                                           IF1304.2
033700 BAIL-OUT.                                                        IF1304.2
033800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1304.2
033900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1304.2
034000 BAIL-OUT-WRITE.                                                  IF1304.2
034100     MOVE CORRECT-A TO XXCORRECT.                                 IF1304.2
034200     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1304.2
034300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1304.2
034400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1304.2
034500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1304.2
034600 BAIL-OUT-EX. EXIT.                                               IF1304.2
034700 CCVS1-EXIT.                                                      IF1304.2
034800     EXIT.                                                        IF1304.2
034900********************************************************          IF1304.2
035000*                                                      *          IF1304.2
035100*    Intrinsic Function Tests   IF130A - PRESENT-VALUE *          IF1304.2
035200*                                                      *          IF1304.2
035300********************************************************          IF1304.2
035400 SECT-IF130A SECTION.                                             IF1304.2
035500 F-PRES-VAL-INFO.                                                 IF1304.2
035600     MOVE     "See ref. A-63 2.34" TO ANSI-REFERENCE.             IF1304.2
035700     MOVE     "PRESENT-VALUE Function" TO FEATURE.                IF1304.2
035800*****************TEST (a) - SIMPLE TEST*****************          IF1304.2
035900 F-PRES-VAL-01.                                                   IF1304.2
036000     MOVE ZERO TO WS-NUM.                                         IF1304.2
036100     MOVE 43.9991 TO MIN-RANGE.                                   IF1304.2
036200     MOVE 44.0009 TO MAX-RANGE.                                   IF1304.2
036300 F-PRES-VAL-TEST-01.                                              IF1304.2
036400     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(0, 23, 12, 9).       IF1304.2
036500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
036600        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
036700                    PERFORM PASS                                  IF1304.2
036800     ELSE                                                         IF1304.2
036900                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
037000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
037100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
037200                    PERFORM FAIL.                                 IF1304.2
037300     GO TO F-PRES-VAL-WRITE-01.                                   IF1304.2
037400 F-PRES-VAL-DELETE-01.                                            IF1304.2
037500     PERFORM  DE-LETE.                                            IF1304.2
037600     GO TO    F-PRES-VAL-WRITE-01.                                IF1304.2
037700 F-PRES-VAL-WRITE-01.                                             IF1304.2
037800     MOVE "F-PRES-VAL-01" TO PAR-NAME.                            IF1304.2
037900     PERFORM  PRINT-DETAIL.                                       IF1304.2
038000*****************TEST (b) - SIMPLE TEST*****************          IF1304.2
038100 F-PRES-VAL-02.                                                   IF1304.2
038200     EVALUATE FUNCTION PRESENT-VALUE(1, 10, 20, 10, 5)            IF1304.2
038300     WHEN  11.5623 THRU  11.5627                                  IF1304.2
038400                    PERFORM PASS                                  IF1304.2
038500     WHEN OTHER                                                   IF1304.2
038600                    PERFORM FAIL.                                 IF1304.2
038700     GO TO F-PRES-VAL-WRITE-02.                                   IF1304.2
038800 F-PRES-VAL-DELETE-02.                                            IF1304.2
038900     PERFORM  DE-LETE.                                            IF1304.2
039000     GO TO    F-PRES-VAL-WRITE-02.                                IF1304.2
039100 F-PRES-VAL-WRITE-02.                                             IF1304.2
039200     MOVE "F-PRES-VAL-02" TO PAR-NAME.                            IF1304.2
039300     PERFORM  PRINT-DETAIL.                                       IF1304.2
039400*****************TEST (c) - SIMPLE TEST*****************          IF1304.2
039500 F-PRES-VAL-03.                                                   IF1304.2
039600     MOVE  9.53314 TO MIN-RANGE.                                  IF1304.2
039700     MOVE  9.53352 TO MAX-RANGE.                                  IF1304.2
039800 F-PRES-VAL-TEST-03.                                              IF1304.2
039900     IF (FUNCTION PRESENT-VALUE(.5, 8.3, 2.4, 9.9)                IF1304.2
040000        >= MIN-RANGE) AND                                         IF1304.2
040100        (FUNCTION PRESENT-VALUE(.5, 8.3, 2.4, 9.9)                IF1304.2
040200        <= MAX-RANGE) THEN                                        IF1304.2
040300                    PERFORM PASS                                  IF1304.2
040400     ELSE                                                         IF1304.2
040500                    PERFORM FAIL.                                 IF1304.2
040600     GO TO F-PRES-VAL-WRITE-03.                                   IF1304.2
040700 F-PRES-VAL-DELETE-03.                                            IF1304.2
040800     PERFORM  DE-LETE.                                            IF1304.2
040900     GO TO    F-PRES-VAL-WRITE-03.                                IF1304.2
041000 F-PRES-VAL-WRITE-03.                                             IF1304.2
041100     MOVE "F-PRES-VAL-03" TO PAR-NAME.                            IF1304.2
041200     PERFORM  PRINT-DETAIL.                                       IF1304.2
041300*****************TEST (d) - SIMPLE TEST*****************          IF1304.2
041400 F-PRES-VAL-04.                                                   IF1304.2
041500     MOVE ZERO TO WS-NUM.                                         IF1304.2
041600     MOVE  22.6274 TO MIN-RANGE.                                  IF1304.2
041700     MOVE  22.6283 TO MAX-RANGE.                                  IF1304.2
041800 F-PRES-VAL-TEST-04.                                              IF1304.2
041900     COMPUTE WS-NUM =                                             IF1304.2
042000         FUNCTION PRESENT-VALUE(.1, 5, 4, 2.8, 3.1, 17).          IF1304.2
042100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
042200        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
042300                    PERFORM PASS                                  IF1304.2
042400     ELSE                                                         IF1304.2
042500                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
042600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
042700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
042800                    PERFORM FAIL.                                 IF1304.2
042900     GO TO F-PRES-VAL-WRITE-04.                                   IF1304.2
043000 F-PRES-VAL-DELETE-04.                                            IF1304.2
043100     PERFORM  DE-LETE.                                            IF1304.2
043200     GO TO    F-PRES-VAL-WRITE-04.                                IF1304.2
043300 F-PRES-VAL-WRITE-04.                                             IF1304.2
043400     MOVE "F-PRES-VAL-04" TO PAR-NAME.                            IF1304.2
043500     PERFORM  PRINT-DETAIL.                                       IF1304.2
043600*****************TEST (e) - SIMPLE TEST*****************          IF1304.2
043700 F-PRES-VAL-05.                                                   IF1304.2
043800     MOVE ZERO TO WS-NUM.                                         IF1304.2
043900     MOVE  20.1691 TO MIN-RANGE.                                  IF1304.2
044000     MOVE  20.1699 TO MAX-RANGE.                                  IF1304.2
044100 F-PRES-VAL-TEST-05.                                              IF1304.2
044200     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(.04, A, B, D).       IF1304.2
044300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
044400        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
044500                    PERFORM PASS                                  IF1304.2
044600     ELSE                                                         IF1304.2
044700                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
044800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
044900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
045000                    PERFORM FAIL.                                 IF1304.2
045100     GO TO F-PRES-VAL-WRITE-05.                                   IF1304.2
045200 F-PRES-VAL-DELETE-05.                                            IF1304.2
045300     PERFORM  DE-LETE.                                            IF1304.2
045400     GO TO    F-PRES-VAL-WRITE-05.                                IF1304.2
045500 F-PRES-VAL-WRITE-05.                                             IF1304.2
045600     MOVE "F-PRES-VAL-05" TO PAR-NAME.                            IF1304.2
045700     PERFORM  PRINT-DETAIL.                                       IF1304.2
045800*****************TEST (f) - SIMPLE TEST*****************          IF1304.2
045900 F-PRES-VAL-06.                                                   IF1304.2
046000     MOVE ZERO TO WS-NUM.                                         IF1304.2
046100     MOVE  33.3113 TO MIN-RANGE.                                  IF1304.2
046200     MOVE  33.3127 TO MAX-RANGE.                                  IF1304.2
046300 F-PRES-VAL-TEST-06.                                              IF1304.2
046400     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(.08, E, G, H, F).    IF1304.2
046500                                                                  IF1304.2
046600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
046700        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
046800                    PERFORM PASS                                  IF1304.2
046900     ELSE                                                         IF1304.2
047000                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
047100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
047200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
047300                    PERFORM FAIL.                                 IF1304.2
047400     GO TO F-PRES-VAL-WRITE-06.                                   IF1304.2
047500 F-PRES-VAL-DELETE-06.                                            IF1304.2
047600     PERFORM  DE-LETE.                                            IF1304.2
047700     GO TO    F-PRES-VAL-WRITE-06.                                IF1304.2
047800 F-PRES-VAL-WRITE-06.                                             IF1304.2
047900     MOVE "F-PRES-VAL-06" TO PAR-NAME.                            IF1304.2
048000     PERFORM  PRINT-DETAIL.                                       IF1304.2
048100*****************TEST (g) - SIMPLE TEST*****************          IF1304.2
048200 F-PRES-VAL-07.                                                   IF1304.2
048300     MOVE ZERO TO WS-NUM.                                         IF1304.2
048400     MOVE  5.76505 TO MIN-RANGE.                                  IF1304.2
048500     MOVE  5.76528 TO MAX-RANGE.                                  IF1304.2
048600 F-PRES-VAL-TEST-07.                                              IF1304.2
048700     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(.2, C, A, 5, 4, 2).  IF1304.2
048800                                                                  IF1304.2
048900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
049000        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
049100                    PERFORM PASS                                  IF1304.2
049200     ELSE                                                         IF1304.2
049300                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
049400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
049500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
049600                    PERFORM FAIL.                                 IF1304.2
049700     GO TO F-PRES-VAL-WRITE-07.                                   IF1304.2
049800 F-PRES-VAL-DELETE-07.                                            IF1304.2
049900     PERFORM  DE-LETE.                                            IF1304.2
050000     GO TO    F-PRES-VAL-WRITE-07.                                IF1304.2
050100 F-PRES-VAL-WRITE-07.                                             IF1304.2
050200     MOVE "F-PRES-VAL-07" TO PAR-NAME.                            IF1304.2
050300     PERFORM  PRINT-DETAIL.                                       IF1304.2
050400*****************TEST (h) - SIMPLE TEST*****************          IF1304.2
050500 F-PRES-VAL-08.                                                   IF1304.2
050600     MOVE ZERO TO WS-NUM.                                         IF1304.2
050700     MOVE  0.361674 TO MIN-RANGE.                                 IF1304.2
050800     MOVE  0.361689 TO MAX-RANGE.                                 IF1304.2
050900 F-PRES-VAL-TEST-08.                                              IF1304.2
051000     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(.3, A, H, .07, -19). IF1304.2
051100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
051200        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
051300                    PERFORM PASS                                  IF1304.2
051400     ELSE                                                         IF1304.2
051500                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
051600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
051700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
051800                    PERFORM FAIL.                                 IF1304.2
051900     GO TO F-PRES-VAL-WRITE-08.                                   IF1304.2
052000 F-PRES-VAL-DELETE-08.                                            IF1304.2
052100     PERFORM  DE-LETE.                                            IF1304.2
052200     GO TO    F-PRES-VAL-WRITE-08.                                IF1304.2
052300 F-PRES-VAL-WRITE-08.                                             IF1304.2
052400     MOVE "F-PRES-VAL-08" TO PAR-NAME.                            IF1304.2
052500     PERFORM  PRINT-DETAIL.                                       IF1304.2
052600*****************TEST (i) - SIMPLE TEST*****************          IF1304.2
052700 F-PRES-VAL-09.                                                   IF1304.2
052800     MOVE ZERO TO WS-NUM.                                         IF1304.2
052900     MOVE -0.001500 TO MIN-RANGE.                                 IF1304.2
053000     MOVE -0.001498 TO MAX-RANGE.                                 IF1304.2
053100 F-PRES-VAL-TEST-09.                                              IF1304.2
053200     COMPUTE WS-NUM =                                             IF1304.2
053300                FUNCTION PRESENT-VALUE(.09, -.0009, -.0008).      IF1304.2
053400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
053500        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
053600                    PERFORM PASS                                  IF1304.2
053700     ELSE                                                         IF1304.2
053800                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
053900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
054000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
054100                    PERFORM FAIL.                                 IF1304.2
054200     GO TO F-PRES-VAL-WRITE-09.                                   IF1304.2
054300 F-PRES-VAL-DELETE-09.                                            IF1304.2
054400     PERFORM  DE-LETE.                                            IF1304.2
054500     GO TO    F-PRES-VAL-WRITE-09.                                IF1304.2
054600 F-PRES-VAL-WRITE-09.                                             IF1304.2
054700     MOVE "F-PRES-VAL-09" TO PAR-NAME.                            IF1304.2
054800     PERFORM  PRINT-DETAIL.                                       IF1304.2
054900*****************TEST (k) - SIMPLE TEST*****************          IF1304.2
055000 F-PRES-VAL-11.                                                   IF1304.2
055100     MOVE ZERO TO WS-NUM.                                         IF1304.2
055200     MOVE  57454.07 TO MIN-RANGE.                                 IF1304.2
055300     MOVE  57456.37 TO MAX-RANGE.                                 IF1304.2
055400 F-PRES-VAL-TEST-11.                                              IF1304.2
055500     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(.4, 30000, 40000,    IF1304.2
055600                                             100000, -80000).     IF1304.2
055700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
055800        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
055900                    PERFORM PASS                                  IF1304.2
056000     ELSE                                                         IF1304.2
056100                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
056200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
056300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
056400                    PERFORM FAIL.                                 IF1304.2
056500     GO TO F-PRES-VAL-WRITE-11.                                   IF1304.2
056600 F-PRES-VAL-DELETE-11.                                            IF1304.2
056700     PERFORM  DE-LETE.                                            IF1304.2
056800     GO TO    F-PRES-VAL-WRITE-11.                                IF1304.2
056900 F-PRES-VAL-WRITE-11.                                             IF1304.2
057000     MOVE "F-PRES-VAL-11" TO PAR-NAME.                            IF1304.2
057100     PERFORM  PRINT-DETAIL.                                       IF1304.2
057200*****************TEST (l) - SIMPLE TEST*****************          IF1304.2
057300 F-PRES-VAL-12.                                                   IF1304.2
057400     MOVE ZERO TO WS-NUM.                                         IF1304.2
057500     MOVE -1406.26 TO MIN-RANGE.                                  IF1304.2
057600     MOVE -1406.21 TO MAX-RANGE.                                  IF1304.2
057700 F-PRES-VAL-TEST-12.                                              IF1304.2
057800     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(.07, L, K).          IF1304.2
057900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
058000        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
058100                    PERFORM PASS                                  IF1304.2
058200     ELSE                                                         IF1304.2
058300                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
058400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
058500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
058600                    PERFORM FAIL.                                 IF1304.2
058700     GO TO F-PRES-VAL-WRITE-12.                                   IF1304.2
058800 F-PRES-VAL-DELETE-12.                                            IF1304.2
058900     PERFORM  DE-LETE.                                            IF1304.2
059000     GO TO    F-PRES-VAL-WRITE-12.                                IF1304.2
059100 F-PRES-VAL-WRITE-12.                                             IF1304.2
059200     MOVE "F-PRES-VAL-12" TO PAR-NAME.                            IF1304.2
059300     PERFORM  PRINT-DETAIL.                                       IF1304.2
059400*****************TEST (m) - SIMPLE TEST*****************          IF1304.2
059500 F-PRES-VAL-13.                                                   IF1304.2
059600     MOVE ZERO TO WS-NUM.                                         IF1304.2
059700     MOVE  6.76570 TO MIN-RANGE.                                  IF1304.2
059800     MOVE  6.76597 TO MAX-RANGE.                                  IF1304.2
059900 F-PRES-VAL-TEST-13.                                              IF1304.2
060000     COMPUTE WS-NUM =                                             IF1304.2
060100          FUNCTION PRESENT-VALUE(.15, IND(1), IND(2),             IF1304.2
060200                                 IND(3)).                         IF1304.2
060300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
060400        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
060500                    PERFORM PASS                                  IF1304.2
060600     ELSE                                                         IF1304.2
060700                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
060800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
060900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
061000                    PERFORM FAIL.                                 IF1304.2
061100     GO TO F-PRES-VAL-WRITE-13.                                   IF1304.2
061200 F-PRES-VAL-DELETE-13.                                            IF1304.2
061300     PERFORM  DE-LETE.                                            IF1304.2
061400     GO TO    F-PRES-VAL-WRITE-13.                                IF1304.2
061500 F-PRES-VAL-WRITE-13.                                             IF1304.2
061600     MOVE "F-PRES-VAL-13" TO PAR-NAME.                            IF1304.2
061700     PERFORM  PRINT-DETAIL.                                       IF1304.2
061800*****************TEST (n) - SIMPLE TEST*****************          IF1304.2
061900 F-PRES-VAL-14.                                                   IF1304.2
062000     MOVE ZERO TO WS-NUM.                                         IF1304.2
062100     MOVE  12.3066 TO MIN-RANGE.                                  IF1304.2
062200     MOVE  12.3071 TO MAX-RANGE.                                  IF1304.2
062300 F-PRES-VAL-TEST-14.                                              IF1304.2
062400     COMPUTE WS-NUM =                                             IF1304.2
062500          FUNCTION PRESENT-VALUE(.13, IND(P), IND(Q), IND(R)).    IF1304.2
062600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
062700        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
062800                    PERFORM PASS                                  IF1304.2
062900     ELSE                                                         IF1304.2
063000                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
063100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
063200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
063300                    PERFORM FAIL.                                 IF1304.2
063400     GO TO F-PRES-VAL-WRITE-14.                                   IF1304.2
063500 F-PRES-VAL-DELETE-14.                                            IF1304.2
063600     PERFORM  DE-LETE.                                            IF1304.2
063700     GO TO    F-PRES-VAL-WRITE-14.                                IF1304.2
063800 F-PRES-VAL-WRITE-14.                                             IF1304.2
063900     MOVE "F-PRES-VAL-14" TO PAR-NAME.                            IF1304.2
064000     PERFORM  PRINT-DETAIL.                                       IF1304.2
064100*****************TEST (o) - SIMPLE TEST*****************          IF1304.2
064200 F-PRES-VAL-15.                                                   IF1304.2
064300     MOVE ZERO TO WS-NUM.                                         IF1304.2
064400     MOVE  37.9070 TO MIN-RANGE.                                  IF1304.2
064500     MOVE  37.9085 TO MAX-RANGE.                                  IF1304.2
064600 F-PRES-VAL-TEST-15.                                              IF1304.2
064700     COMPUTE WS-NUM =                                             IF1304.2
064800         FUNCTION PRESENT-VALUE(.1, 10, 10, 10, 10, 10).          IF1304.2
064900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
065000        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
065100                    PERFORM PASS                                  IF1304.2
065200     ELSE                                                         IF1304.2
065300                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
065400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
065500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
065600                    PERFORM FAIL.                                 IF1304.2
065700     GO TO F-PRES-VAL-WRITE-15.                                   IF1304.2
065800 F-PRES-VAL-DELETE-15.                                            IF1304.2
065900     PERFORM  DE-LETE.                                            IF1304.2
066000     GO TO    F-PRES-VAL-WRITE-15.                                IF1304.2
066100 F-PRES-VAL-WRITE-15.                                             IF1304.2
066200     MOVE "F-PRES-VAL-15" TO PAR-NAME.                            IF1304.2
066300     PERFORM  PRINT-DETAIL.                                       IF1304.2
066400*****************TEST (a) - COMPLEX TEST****************          IF1304.2
066500 F-PRES-VAL-16.                                                   IF1304.2
066600     MOVE ZERO TO WS-NUM.                                         IF1304.2
066700     MOVE  65.9974 TO MIN-RANGE.                                  IF1304.2
066800     MOVE  66.0026 TO MAX-RANGE.                                  IF1304.2
066900 F-PRES-VAL-TEST-16.                                              IF1304.2
067000     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE                      IF1304.2
067100                               (-.5, (2 + 3), (6 / 3), (9 - 3)).  IF1304.2
067200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
067300        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
067400                    PERFORM PASS                                  IF1304.2
067500     ELSE                                                         IF1304.2
067600                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
067700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
067800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
067900                    PERFORM FAIL.                                 IF1304.2
068000     GO TO F-PRES-VAL-WRITE-16.                                   IF1304.2
068100 F-PRES-VAL-DELETE-16.                                            IF1304.2
068200     PERFORM  DE-LETE.                                            IF1304.2
068300     GO TO    F-PRES-VAL-WRITE-16.                                IF1304.2
068400 F-PRES-VAL-WRITE-16.                                             IF1304.2
068500     MOVE "F-PRES-VAL-16" TO PAR-NAME.                            IF1304.2
068600     PERFORM  PRINT-DETAIL.                                       IF1304.2
068700*****************TEST (b) - COMPLEX TEST****************          IF1304.2
068800 F-PRES-VAL-17.                                                   IF1304.2
068900     MOVE ZERO TO WS-NUM.                                         IF1304.2
069000     MOVE  44.4513 TO MIN-RANGE.                                  IF1304.2
069100     MOVE  44.4549 TO MAX-RANGE.                                  IF1304.2
069200 F-PRES-VAL-TEST-17.                                              IF1304.2
069300     COMPUTE WS-NUM =                                             IF1304.2
069400            FUNCTION PRESENT-VALUE(-.2, 5 / 4, 3.3 * 4, 9.4 + 2). IF1304.2
069500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
069600        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
069700                    PERFORM PASS                                  IF1304.2
069800     ELSE                                                         IF1304.2
069900                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
070000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
070100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
070200                    PERFORM FAIL.                                 IF1304.2
070300     GO TO F-PRES-VAL-WRITE-17.                                   IF1304.2
070400 F-PRES-VAL-DELETE-17.                                            IF1304.2
070500     PERFORM  DE-LETE.                                            IF1304.2
070600     GO TO    F-PRES-VAL-WRITE-17.                                IF1304.2
070700 F-PRES-VAL-WRITE-17.                                             IF1304.2
070800     MOVE "F-PRES-VAL-17" TO PAR-NAME.                            IF1304.2
070900     PERFORM  PRINT-DETAIL.                                       IF1304.2
071000*****************TEST (c) - COMPLEX TEST****************          IF1304.2
071100 F-PRES-VAL-18.                                                   IF1304.2
071200     MOVE ZERO TO WS-NUM.                                         IF1304.2
071300     MOVE  7.91943 TO MIN-RANGE.                                  IF1304.2
071400     MOVE  7.92007 TO MAX-RANGE.                                  IF1304.2
071500 F-PRES-VAL-TEST-18.                                              IF1304.2
071600     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE                      IF1304.2
071700                         (.5, A + 2, 4.5 / C, 8, B).              IF1304.2
071800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
071900        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
072000                    PERFORM PASS                                  IF1304.2
072100     ELSE                                                         IF1304.2
072200                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
072300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
072400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
072500                    PERFORM FAIL.                                 IF1304.2
072600     GO TO F-PRES-VAL-WRITE-18.                                   IF1304.2
072700 F-PRES-VAL-DELETE-18.                                            IF1304.2
072800     PERFORM  DE-LETE.                                            IF1304.2
072900     GO TO    F-PRES-VAL-WRITE-18.                                IF1304.2
073000 F-PRES-VAL-WRITE-18.                                             IF1304.2
073100     MOVE "F-PRES-VAL-18" TO PAR-NAME.                            IF1304.2
073200     PERFORM  PRINT-DETAIL.                                       IF1304.2
073300*****************TEST (d) - COMPLEX TEST****************          IF1304.2
073400 F-PRES-VAL-19.                                                   IF1304.2
073500     MOVE ZERO TO WS-NUM.                                         IF1304.2
073600     MOVE  22.4229 TO MIN-RANGE.                                  IF1304.2
073700     MOVE  22.4247 TO MAX-RANGE.                                  IF1304.2
073800 F-PRES-VAL-TEST-19.                                              IF1304.2
073900     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(.08, 2, 3) + 18.     IF1304.2
074000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
074100        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
074200                    PERFORM PASS                                  IF1304.2
074300     ELSE                                                         IF1304.2
074400                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
074500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
074600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
074700                    PERFORM FAIL.                                 IF1304.2
074800     GO TO F-PRES-VAL-WRITE-19.                                   IF1304.2
074900 F-PRES-VAL-DELETE-19.                                            IF1304.2
075000     PERFORM  DE-LETE.                                            IF1304.2
075100     GO TO    F-PRES-VAL-WRITE-19.                                IF1304.2
075200 F-PRES-VAL-WRITE-19.                                             IF1304.2
075300     MOVE "F-PRES-VAL-19" TO PAR-NAME.                            IF1304.2
075400     PERFORM  PRINT-DETAIL.                                       IF1304.2
075500*****************TEST (e) - COMPLEX TEST****************          IF1304.2
075600 F-PRES-VAL-20.                                                   IF1304.2
075700     MOVE ZERO TO WS-NUM.                                         IF1304.2
075800     MOVE -2.09570 TO MIN-RANGE.                                  IF1304.2
075900     MOVE -2.09554 TO MAX-RANGE.                                  IF1304.2
076000 F-PRES-VAL-TEST-20.                                              IF1304.2
076100     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(.03, -6, -4) +       IF1304.2
076200                      FUNCTION PRESENT-VALUE(.2, 9).              IF1304.2
076300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
076400        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
076500                    PERFORM PASS                                  IF1304.2
076600     ELSE                                                         IF1304.2
076700                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
076800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
076900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
077000                    PERFORM FAIL.                                 IF1304.2
077100     GO TO F-PRES-VAL-WRITE-20.                                   IF1304.2
077200 F-PRES-VAL-DELETE-20.                                            IF1304.2
077300     PERFORM  DE-LETE.                                            IF1304.2
077400     GO TO    F-PRES-VAL-WRITE-20.                                IF1304.2
077500 F-PRES-VAL-WRITE-20.                                             IF1304.2
077600     MOVE "F-PRES-VAL-20" TO PAR-NAME.                            IF1304.2
077700     PERFORM  PRINT-DETAIL.                                       IF1304.2
077800*****************TEST (f) - COMPLEX TEST****************          IF1304.2
077900 F-PRES-VAL-21.                                                   IF1304.2
078000     MOVE ZERO TO WS-NUM.                                         IF1304.2
078100     MOVE  1.49994 TO MIN-RANGE.                                  IF1304.2
078200     MOVE  1.50006 TO MAX-RANGE.                                  IF1304.2
078300 F-PRES-VAL-TEST-21.                                              IF1304.2
078400     COMPUTE WS-NUM = FUNCTION PRESENT-VALUE(                     IF1304.2
078500                         FUNCTION PRESENT-VALUE(1, 2), 3).        IF1304.2
078600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1304.2
078700        (WS-NUM <= MAX-RANGE) THEN                                IF1304.2
078800                    PERFORM PASS                                  IF1304.2
078900     ELSE                                                         IF1304.2
079000                    MOVE WS-NUM TO COMPUTED-N                     IF1304.2
079100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1304.2
079200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1304.2
079300                    PERFORM FAIL.                                 IF1304.2
079400     GO TO F-PRES-VAL-WRITE-21.                                   IF1304.2
079500 F-PRES-VAL-DELETE-21.                                            IF1304.2
079600     PERFORM  DE-LETE.                                            IF1304.2
079700     GO TO    F-PRES-VAL-WRITE-21.                                IF1304.2
079800 F-PRES-VAL-WRITE-21.                                             IF1304.2
079900     MOVE "F-PRES-VAL-21" TO PAR-NAME.                            IF1304.2
080000     PERFORM  PRINT-DETAIL.                                       IF1304.2
080100*****************SPECIAL PERFORM TEST**********************       IF1304.2
080200 F-PRES-VAL-22.                                                   IF1304.2
080300     PERFORM F-PRES-VAL-TEST-22                                   IF1304.2
080400       UNTIL FUNCTION PRESENT-VALUE(ARG1, 2) < 0.5.               IF1304.2
080500     PERFORM PASS.                                                IF1304.2
080600     GO TO F-PRES-VAL-WRITE-22.                                   IF1304.2
080700 F-PRES-VAL-TEST-22.                                              IF1304.2
080800     COMPUTE ARG1 = ARG1 + 1.                                     IF1304.2
080900 F-PRES-VAL-DELETE-22.                                            IF1304.2
081000     PERFORM  DE-LETE.                                            IF1304.2
081100     GO TO    F-PRES-VAL-WRITE-22.                                IF1304.2
081200 F-PRES-VAL-WRITE-22.                                             IF1304.2
081300     MOVE "F-PRES-VAL-22" TO PAR-NAME.                            IF1304.2
081400     PERFORM  PRINT-DETAIL.                                       IF1304.2
081500********************END OF TESTS***************                   IF1304.2
081600 CCVS-EXIT SECTION.                                               IF1304.2
081700 CCVS-999999.                                                     IF1304.2
081800     GO TO CLOSE-FILES.                                           IF1304.2
      *END-OF,IF130A                                                                  
