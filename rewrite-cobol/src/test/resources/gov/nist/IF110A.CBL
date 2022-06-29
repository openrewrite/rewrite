      *HEADER,COBOL,IF110A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1104.2
000200 PROGRAM-ID.                                                      IF1104.2
000300     IF110A.                                                      IF1104.2
000400                                                                  IF1104.2
000500***********************************************************       IF1104.2
000600*                                                         *       IF1104.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1104.2
000800* It contains tests for the Intrinsic Function            *       IF1104.2
000900* FACTORIAL.                                              *       IF1104.2
001000*                                                         *       IF1104.2
001100***********************************************************       IF1104.2
001200 ENVIRONMENT DIVISION.                                            IF1104.2
001300 CONFIGURATION SECTION.                                           IF1104.2
001400 SOURCE-COMPUTER.                                                 IF1104.2
001500     XXXXX082.                                                    IF1104.2
001600 OBJECT-COMPUTER.                                                 IF1104.2
001700     XXXXX083.                                                    IF1104.2
001800 INPUT-OUTPUT SECTION.                                            IF1104.2
001900 FILE-CONTROL.                                                    IF1104.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1104.2
002100     XXXXX055.                                                    IF1104.2
002200 DATA DIVISION.                                                   IF1104.2
002300 FILE SECTION.                                                    IF1104.2
002400 FD  PRINT-FILE.                                                  IF1104.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1104.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1104.2
002700 WORKING-STORAGE SECTION.                                         IF1104.2
002800***********************************************************       IF1104.2
002900* Variables specific to the Intrinsic Function Test IF110A*       IF1104.2
003000***********************************************************       IF1104.2
003100 01  A                   PIC S9(10)          VALUE 5.             IF1104.2
003200 01  B                   PIC S9(10)          VALUE 7.             IF1104.2
003300 01  ARG1                PIC S9(10)          VALUE 1.             IF1104.2
003400 01  ARR                                     VALUE "40537".       IF1104.2
003500     02  IND OCCURS 5 TIMES PIC 9.                                IF1104.2
003600 01  TEMP                PIC S9(5)V9(5).                          IF1104.2
003700 01  WS-NUM              PIC S9(5)V9(6).                          IF1104.2
003800 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1104.2
003900 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1104.2
004000*                                                                 IF1104.2
004100**********************************************************        IF1104.2
004200*                                                                 IF1104.2
004300 01  TEST-RESULTS.                                                IF1104.2
004400     02 FILLER                   PIC X      VALUE SPACE.          IF1104.2
004500     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1104.2
004600     02 FILLER                   PIC X      VALUE SPACE.          IF1104.2
004700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1104.2
004800     02 FILLER                   PIC X      VALUE SPACE.          IF1104.2
004900     02  PAR-NAME.                                                IF1104.2
005000       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1104.2
005100       03  PARDOT-X              PIC X      VALUE SPACE.          IF1104.2
005200       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1104.2
005300     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1104.2
005400     02 RE-MARK                  PIC X(61).                       IF1104.2
005500 01  TEST-COMPUTED.                                               IF1104.2
005600     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1104.2
005700     02 FILLER                   PIC X(17)  VALUE                 IF1104.2
005800            "       COMPUTED=".                                   IF1104.2
005900     02 COMPUTED-X.                                               IF1104.2
006000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1104.2
006100     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1104.2
006200                                 PIC -9(9).9(9).                  IF1104.2
006300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1104.2
006400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1104.2
006500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1104.2
006600     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1104.2
006700         04 COMPUTED-18V0                    PIC -9(18).          IF1104.2
006800         04 FILLER                           PIC X.               IF1104.2
006900     03 FILLER PIC X(50) VALUE SPACE.                             IF1104.2
007000 01  TEST-CORRECT.                                                IF1104.2
007100     02 FILLER PIC X(30) VALUE SPACE.                             IF1104.2
007200     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1104.2
007300     02 CORRECT-X.                                                IF1104.2
007400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1104.2
007500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1104.2
007600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1104.2
007700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1104.2
007800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1104.2
007900     03      CR-18V0 REDEFINES CORRECT-A.                         IF1104.2
008000         04 CORRECT-18V0                     PIC -9(18).          IF1104.2
008100         04 FILLER                           PIC X.               IF1104.2
008200     03 FILLER PIC X(2) VALUE SPACE.                              IF1104.2
008300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1104.2
008400 01  TEST-CORRECT-MIN.                                            IF1104.2
008500     02 FILLER PIC X(30) VALUE SPACE.                             IF1104.2
008600     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1104.2
008700     02 CORRECTMI-X.                                              IF1104.2
008800     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1104.2
008900     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1104.2
009000     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1104.2
009100     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1104.2
009200     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1104.2
009300     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1104.2
009400         04 CORRECTMI-18V0                     PIC -9(18).        IF1104.2
009500         04 FILLER                           PIC X.               IF1104.2
009600     03 FILLER PIC X(2) VALUE SPACE.                              IF1104.2
009700     03 FILLER                           PIC X(48) VALUE SPACE.   IF1104.2
009800 01  TEST-CORRECT-MAX.                                            IF1104.2
009900     02 FILLER PIC X(30) VALUE SPACE.                             IF1104.2
010000     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1104.2
010100     02 CORRECTMA-X.                                              IF1104.2
010200     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1104.2
010300     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1104.2
010400     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1104.2
010500     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1104.2
010600     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1104.2
010700     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1104.2
010800         04 CORRECTMA-18V0                     PIC -9(18).        IF1104.2
010900         04 FILLER                           PIC X.               IF1104.2
011000     03 FILLER PIC X(2) VALUE SPACE.                              IF1104.2
011100     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1104.2
011200 01  CCVS-C-1.                                                    IF1104.2
011300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1104.2
011400-    "SS  PARAGRAPH-NAME                                          IF1104.2
011500-    "       REMARKS".                                            IF1104.2
011600     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1104.2
011700 01  CCVS-C-2.                                                    IF1104.2
011800     02 FILLER                     PIC X        VALUE SPACE.      IF1104.2
011900     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1104.2
012000     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1104.2
012100     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1104.2
012200     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1104.2
012300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1104.2
012400 01  REC-CT                        PIC 99       VALUE ZERO.       IF1104.2
012500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1104.2
012600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1104.2
012700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1104.2
012800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1104.2
012900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1104.2
013000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1104.2
013100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1104.2
013200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1104.2
013300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1104.2
013400 01  CCVS-H-1.                                                    IF1104.2
013500     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1104.2
013600     02  FILLER                    PIC X(42)    VALUE             IF1104.2
013700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1104.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1104.2
013900 01  CCVS-H-2A.                                                   IF1104.2
014000   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1104.2
014100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1104.2
014200   02  FILLER                        PIC XXXX   VALUE             IF1104.2
014300     "4.2 ".                                                      IF1104.2
014400   02  FILLER                        PIC X(28)  VALUE             IF1104.2
014500            " COPY - NOT FOR DISTRIBUTION".                       IF1104.2
014600   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1104.2
014700                                                                  IF1104.2
014800 01  CCVS-H-2B.                                                   IF1104.2
014900   02  FILLER                        PIC X(15)  VALUE             IF1104.2
015000            "TEST RESULT OF ".                                    IF1104.2
015100   02  TEST-ID                       PIC X(9).                    IF1104.2
015200   02  FILLER                        PIC X(4)   VALUE             IF1104.2
015300            " IN ".                                               IF1104.2
015400   02  FILLER                        PIC X(12)  VALUE             IF1104.2
015500     " HIGH       ".                                              IF1104.2
015600   02  FILLER                        PIC X(22)  VALUE             IF1104.2
015700            " LEVEL VALIDATION FOR ".                             IF1104.2
015800   02  FILLER                        PIC X(58)  VALUE             IF1104.2
015900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1104.2
016000 01  CCVS-H-3.                                                    IF1104.2
016100     02  FILLER                      PIC X(34)  VALUE             IF1104.2
016200            " FOR OFFICIAL USE ONLY    ".                         IF1104.2
016300     02  FILLER                      PIC X(58)  VALUE             IF1104.2
016400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1104.2
016500     02  FILLER                      PIC X(28)  VALUE             IF1104.2
016600            "  COPYRIGHT   1985 ".                                IF1104.2
016700 01  CCVS-E-1.                                                    IF1104.2
016800     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1104.2
016900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1104.2
017000     02 ID-AGAIN                     PIC X(9).                    IF1104.2
017100     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1104.2
017200 01  CCVS-E-2.                                                    IF1104.2
017300     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1104.2
017400     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1104.2
017500     02 CCVS-E-2-2.                                               IF1104.2
017600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1104.2
017700         03 FILLER                   PIC X      VALUE SPACE.      IF1104.2
017800         03 ENDER-DESC               PIC X(44)  VALUE             IF1104.2
017900            "ERRORS ENCOUNTERED".                                 IF1104.2
018000 01  CCVS-E-3.                                                    IF1104.2
018100     02  FILLER                      PIC X(22)  VALUE             IF1104.2
018200            " FOR OFFICIAL USE ONLY".                             IF1104.2
018300     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1104.2
018400     02  FILLER                      PIC X(58)  VALUE             IF1104.2
018500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1104.2
018600     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1104.2
018700     02 FILLER                       PIC X(15)  VALUE             IF1104.2
018800             " COPYRIGHT 1985".                                   IF1104.2
018900 01  CCVS-E-4.                                                    IF1104.2
019000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1104.2
019100     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1104.2
019200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1104.2
019300     02 FILLER                       PIC X(40)  VALUE             IF1104.2
019400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1104.2
019500 01  XXINFO.                                                      IF1104.2
019600     02 FILLER                       PIC X(19)  VALUE             IF1104.2
019700            "*** INFORMATION ***".                                IF1104.2
019800     02 INFO-TEXT.                                                IF1104.2
019900       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1104.2
020000       04 XXCOMPUTED                 PIC X(20).                   IF1104.2
020100       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1104.2
020200       04 XXCORRECT                  PIC X(20).                   IF1104.2
020300     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1104.2
020400 01  HYPHEN-LINE.                                                 IF1104.2
020500     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1104.2
020600     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1104.2
020700-    "*****************************************".                 IF1104.2
020800     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1104.2
020900-    "******************************".                            IF1104.2
021000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1104.2
021100     "IF110A".                                                    IF1104.2
021200 PROCEDURE DIVISION.                                              IF1104.2
021300 CCVS1 SECTION.                                                   IF1104.2
021400 OPEN-FILES.                                                      IF1104.2
021500     OPEN     OUTPUT PRINT-FILE.                                  IF1104.2
021600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1104.2
021700     MOVE    SPACE TO TEST-RESULTS.                               IF1104.2
021800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1104.2
021900     GO TO CCVS1-EXIT.                                            IF1104.2
022000 CLOSE-FILES.                                                     IF1104.2
022100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1104.2
022200 TERMINATE-CCVS.                                                  IF1104.2
022300     STOP     RUN.                                                IF1104.2
022400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1104.2
022500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1104.2
022600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1104.2
022700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1104.2
022800     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1104.2
022900 PRINT-DETAIL.                                                    IF1104.2
023000     IF REC-CT NOT EQUAL TO ZERO                                  IF1104.2
023100             MOVE "." TO PARDOT-X                                 IF1104.2
023200             MOVE REC-CT TO DOTVALUE.                             IF1104.2
023300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1104.2
023400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1104.2
023500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1104.2
023600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1104.2
023700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1104.2
023800     MOVE SPACE TO CORRECT-X.                                     IF1104.2
023900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1104.2
024000     MOVE     SPACE TO RE-MARK.                                   IF1104.2
024100 HEAD-ROUTINE.                                                    IF1104.2
024200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1104.2
024300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1104.2
024400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1104.2
024500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1104.2
024600 COLUMN-NAMES-ROUTINE.                                            IF1104.2
024700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1104.2
024800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1104.2
024900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1104.2
025000 END-ROUTINE.                                                     IF1104.2
025100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1104.2
025200 END-RTN-EXIT.                                                    IF1104.2
025300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1104.2
025400 END-ROUTINE-1.                                                   IF1104.2
025500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1104.2
025600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1104.2
025700      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1104.2
025800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1104.2
025900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1104.2
026000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1104.2
026100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1104.2
026200  END-ROUTINE-12.                                                 IF1104.2
026300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1104.2
026400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1104.2
026500         MOVE "NO " TO ERROR-TOTAL                                IF1104.2
026600         ELSE                                                     IF1104.2
026700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1104.2
026800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1104.2
026900     PERFORM WRITE-LINE.                                          IF1104.2
027000 END-ROUTINE-13.                                                  IF1104.2
027100     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1104.2
027200         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1104.2
027300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1104.2
027400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1104.2
027500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1104.2
027600      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1104.2
027700          MOVE "NO " TO ERROR-TOTAL                               IF1104.2
027800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1104.2
027900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1104.2
028000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1104.2
028100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1104.2
028200 WRITE-LINE.                                                      IF1104.2
028300     ADD 1 TO RECORD-COUNT.                                       IF1104.2
028400*    IF RECORD-COUNT GREATER 42                                   IF1104.2
028500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1104.2
028600*        MOVE SPACE TO DUMMY-RECORD                               IF1104.2
028700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1104.2
028800*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1104.2
028900*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1104.2
029000*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1104.2
029100*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1104.2
029200*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1104.2
029300*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1104.2
029400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1104.2
029500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1104.2
029600*        MOVE ZERO TO RECORD-COUNT.                               IF1104.2
029700     PERFORM WRT-LN.                                              IF1104.2
029800 WRT-LN.                                                          IF1104.2
029900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1104.2
030000     MOVE SPACE TO DUMMY-RECORD.                                  IF1104.2
030100 BLANK-LINE-PRINT.                                                IF1104.2
030200     PERFORM WRT-LN.                                              IF1104.2
030300 FAIL-ROUTINE.                                                    IF1104.2
030400     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1104.2
030500            GO TO FAIL-ROUTINE-WRITE.                             IF1104.2
030600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1104.2
030700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1104.2
030800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1104.2
030900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1104.2
031000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1104.2
031100     GO TO  FAIL-ROUTINE-EX.                                      IF1104.2
031200 FAIL-ROUTINE-WRITE.                                              IF1104.2
031300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1104.2
031400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1104.2
031500                              CORMA-ANSI-REFERENCE.               IF1104.2
031600     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1104.2
031700           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1104.2
031800           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1104.2
031900     ELSE                                                         IF1104.2
032000           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1104.2
032100     PERFORM WRITE-LINE.                                          IF1104.2
032200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1104.2
032300 FAIL-ROUTINE-EX. EXIT.                                           IF1104.2
032400 BAIL-OUT.                                                        IF1104.2
032500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1104.2
032600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1104.2
032700 BAIL-OUT-WRITE.                                                  IF1104.2
032800     MOVE CORRECT-A TO XXCORRECT.                                 IF1104.2
032900     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1104.2
033000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1104.2
033100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1104.2
033200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1104.2
033300 BAIL-OUT-EX. EXIT.                                               IF1104.2
033400 CCVS1-EXIT.                                                      IF1104.2
033500     EXIT.                                                        IF1104.2
033600********************************************************          IF1104.2
033700*                                                      *          IF1104.2
033800*    Intrinsic Function Tests      IF110A - FACTORIAL  *          IF1104.2
033900*                                                      *          IF1104.2
034000********************************************************          IF1104.2
034100 SECT-IF110A SECTION.                                             IF1104.2
034200 F-FACTORIAL-INFO.                                                IF1104.2
034300     MOVE     "See ref. A-43 2.14" TO ANSI-REFERENCE.             IF1104.2
034400     MOVE     "FACTORIAL Function" TO FEATURE.                    IF1104.2
034500*****************TEST (a) ******************************          IF1104.2
034600 F-FACTORIAL-01.                                                  IF1104.2
034700     MOVE ZERO TO WS-NUM.                                         IF1104.2
034800 F-FACTORIAL-TEST-01.                                             IF1104.2
034900     COMPUTE WS-NUM = FUNCTION FACTORIAL(0).                      IF1104.2
035000     IF WS-NUM = 1 THEN                                           IF1104.2
035100                    PERFORM PASS                                  IF1104.2
035200     ELSE                                                         IF1104.2
035300                    MOVE WS-NUM TO COMPUTED-N                     IF1104.2
035400                    MOVE 1 TO CORRECT-N                           IF1104.2
035500                    PERFORM FAIL.                                 IF1104.2
035600     GO TO F-FACTORIAL-WRITE-01.                                  IF1104.2
035700 F-FACTORIAL-DELETE-01.                                           IF1104.2
035800     PERFORM  DE-LETE.                                            IF1104.2
035900     GO TO    F-FACTORIAL-WRITE-01.                               IF1104.2
036000 F-FACTORIAL-WRITE-01.                                            IF1104.2
036100     MOVE "F-FACTORIAL-01" TO PAR-NAME.                           IF1104.2
036200     PERFORM  PRINT-DETAIL.                                       IF1104.2
036300*****************TEST (b) ******************************          IF1104.2
036400 F-FACTORIAL-02.                                                  IF1104.2
036500     EVALUATE FUNCTION FACTORIAL(3)                               IF1104.2
036600     WHEN 6                                                       IF1104.2
036700                    PERFORM PASS                                  IF1104.2
036800     WHEN OTHER                                                   IF1104.2
036900                    PERFORM FAIL.                                 IF1104.2
037000     GO TO F-FACTORIAL-WRITE-02.                                  IF1104.2
037100 F-FACTORIAL-DELETE-02.                                           IF1104.2
037200     PERFORM  DE-LETE.                                            IF1104.2
037300     GO TO    F-FACTORIAL-WRITE-02.                               IF1104.2
037400 F-FACTORIAL-WRITE-02.                                            IF1104.2
037500     MOVE "F-FACTORIAL-02" TO PAR-NAME.                           IF1104.2
037600     PERFORM  PRINT-DETAIL.                                       IF1104.2
037700*****************TEST (c) ******************************          IF1104.2
037800 F-FACTORIAL-03.                                                  IF1104.2
037900     IF FUNCTION FACTORIAL(A) = 120 THEN                          IF1104.2
038000                    PERFORM PASS                                  IF1104.2
038100     ELSE                                                         IF1104.2
038200                    PERFORM FAIL.                                 IF1104.2
038300     GO TO F-FACTORIAL-WRITE-03.                                  IF1104.2
038400 F-FACTORIAL-DELETE-03.                                           IF1104.2
038500     PERFORM  DE-LETE.                                            IF1104.2
038600     GO TO    F-FACTORIAL-WRITE-03.                               IF1104.2
038700 F-FACTORIAL-WRITE-03.                                            IF1104.2
038800     MOVE "F-FACTORIAL-03" TO PAR-NAME.                           IF1104.2
038900     PERFORM  PRINT-DETAIL.                                       IF1104.2
039000*****************TEST (d) ******************************          IF1104.2
039100 F-FACTORIAL-04.                                                  IF1104.2
039200     MOVE ZERO TO WS-NUM.                                         IF1104.2
039300 F-FACTORIAL-TEST-04.                                             IF1104.2
039400     COMPUTE WS-NUM = FUNCTION FACTORIAL(IND(4)).                 IF1104.2
039500     IF WS-NUM = 6 THEN                                           IF1104.2
039600                    PERFORM PASS                                  IF1104.2
039700     ELSE                                                         IF1104.2
039800                    MOVE WS-NUM TO COMPUTED-N                     IF1104.2
039900                    MOVE 6 TO CORRECT-N                           IF1104.2
040000                    PERFORM FAIL.                                 IF1104.2
040100     GO TO F-FACTORIAL-WRITE-04.                                  IF1104.2
040200 F-FACTORIAL-DELETE-04.                                           IF1104.2
040300     PERFORM  DE-LETE.                                            IF1104.2
040400     GO TO    F-FACTORIAL-WRITE-04.                               IF1104.2
040500 F-FACTORIAL-WRITE-04.                                            IF1104.2
040600     MOVE "F-FACTORIAL-04" TO PAR-NAME.                           IF1104.2
040700     PERFORM  PRINT-DETAIL.                                       IF1104.2
040800*****************TEST (e) ******************************          IF1104.2
040900 F-FACTORIAL-05.                                                  IF1104.2
041000     MOVE ZERO TO WS-NUM.                                         IF1104.2
041100 F-FACTORIAL-TEST-05.                                             IF1104.2
041200     COMPUTE WS-NUM = FUNCTION FACTORIAL(IND(A)).                 IF1104.2
041300     IF WS-NUM = 5040 THEN                                        IF1104.2
041400                    PERFORM PASS                                  IF1104.2
041500     ELSE                                                         IF1104.2
041600                    MOVE WS-NUM TO COMPUTED-N                     IF1104.2
041700                    MOVE 5040 TO CORRECT-N                        IF1104.2
041800                    PERFORM FAIL.                                 IF1104.2
041900     GO TO F-FACTORIAL-WRITE-05.                                  IF1104.2
042000 F-FACTORIAL-DELETE-05.                                           IF1104.2
042100     PERFORM  DE-LETE.                                            IF1104.2
042200     GO TO    F-FACTORIAL-WRITE-05.                               IF1104.2
042300 F-FACTORIAL-WRITE-05.                                            IF1104.2
042400     MOVE "F-FACTORIAL-05" TO PAR-NAME.                           IF1104.2
042500     PERFORM  PRINT-DETAIL.                                       IF1104.2
042600*****************TEST (f) ******************************          IF1104.2
042700 F-FACTORIAL-06.                                                  IF1104.2
042800     MOVE ZERO TO WS-NUM.                                         IF1104.2
042900 F-FACTORIAL-TEST-06.                                             IF1104.2
043000     COMPUTE WS-NUM = FUNCTION FACTORIAL(                         IF1104.2
043100                      FUNCTION FACTORIAL(3)).                     IF1104.2
043200     IF WS-NUM = 720 THEN                                         IF1104.2
043300                    PERFORM PASS                                  IF1104.2
043400     ELSE                                                         IF1104.2
043500                    MOVE WS-NUM TO COMPUTED-N                     IF1104.2
043600                    MOVE 720 TO CORRECT-N                         IF1104.2
043700                    PERFORM FAIL.                                 IF1104.2
043800     GO TO F-FACTORIAL-WRITE-06.                                  IF1104.2
043900 F-FACTORIAL-DELETE-06.                                           IF1104.2
044000     PERFORM  DE-LETE.                                            IF1104.2
044100     GO TO    F-FACTORIAL-WRITE-06.                               IF1104.2
044200 F-FACTORIAL-WRITE-06.                                            IF1104.2
044300     MOVE "F-FACTORIAL-06" TO PAR-NAME.                           IF1104.2
044400     PERFORM  PRINT-DETAIL.                                       IF1104.2
044500*****************TEST (g) ******************************          IF1104.2
044600 F-FACTORIAL-07.                                                  IF1104.2
044700     MOVE ZERO TO WS-NUM.                                         IF1104.2
044800 F-FACTORIAL-TEST-07.                                             IF1104.2
044900     COMPUTE WS-NUM = FUNCTION FACTORIAL(1) + B.                  IF1104.2
045000     IF WS-NUM = 8 THEN                                           IF1104.2
045100                    PERFORM PASS                                  IF1104.2
045200     ELSE                                                         IF1104.2
045300                    MOVE WS-NUM TO COMPUTED-N                     IF1104.2
045400                    MOVE 8 TO CORRECT-N                           IF1104.2
045500                    PERFORM FAIL.                                 IF1104.2
045600     GO TO F-FACTORIAL-WRITE-07.                                  IF1104.2
045700 F-FACTORIAL-DELETE-07.                                           IF1104.2
045800     PERFORM  DE-LETE.                                            IF1104.2
045900     GO TO    F-FACTORIAL-WRITE-07.                               IF1104.2
046000 F-FACTORIAL-WRITE-07.                                            IF1104.2
046100     MOVE "F-FACTORIAL-07" TO PAR-NAME.                           IF1104.2
046200     PERFORM  PRINT-DETAIL.                                       IF1104.2
046300*****************TEST (h) ******************************          IF1104.2
046400 F-FACTORIAL-08.                                                  IF1104.2
046500     MOVE ZERO TO WS-NUM.                                         IF1104.2
046600 F-FACTORIAL-TEST-08.                                             IF1104.2
046700     COMPUTE WS-NUM = FUNCTION FACTORIAL(4) +                     IF1104.2
046800                      FUNCTION FACTORIAL(2).                      IF1104.2
046900     IF WS-NUM = 26 THEN                                          IF1104.2
047000                    PERFORM PASS                                  IF1104.2
047100     ELSE                                                         IF1104.2
047200                    MOVE WS-NUM TO COMPUTED-N                     IF1104.2
047300                    MOVE 26 TO CORRECT-N                          IF1104.2
047400                    PERFORM FAIL.                                 IF1104.2
047500     GO TO F-FACTORIAL-WRITE-08.                                  IF1104.2
047600 F-FACTORIAL-DELETE-08.                                           IF1104.2
047700     PERFORM  DE-LETE.                                            IF1104.2
047800     GO TO    F-FACTORIAL-WRITE-08.                               IF1104.2
047900 F-FACTORIAL-WRITE-08.                                            IF1104.2
048000     MOVE "F-FACTORIAL-08" TO PAR-NAME.                           IF1104.2
048100     PERFORM  PRINT-DETAIL.                                       IF1104.2
048200*****************SPECIAL PERFORM TEST**********************       IF1104.2
048300 F-FACTORIAL-09.                                                  IF1104.2
048400     MOVE ZERO TO WS-NUM.                                         IF1104.2
048500     PERFORM F-FACTORIAL-TEST-09                                  IF1104.2
048600       UNTIL FUNCTION FACTORIAL(ARG1) > 120.                      IF1104.2
048700     PERFORM PASS.                                                IF1104.2
048800     GO TO F-FACTORIAL-WRITE-09.                                  IF1104.2
048900 F-FACTORIAL-TEST-09.                                             IF1104.2
049000     COMPUTE ARG1 = ARG1 + 1.                                     IF1104.2
049100 F-FACTORIAL-DELETE-09.                                           IF1104.2
049200     PERFORM  DE-LETE.                                            IF1104.2
049300     GO TO    F-FACTORIAL-WRITE-09.                               IF1104.2
049400 F-FACTORIAL-WRITE-09.                                            IF1104.2
049500     MOVE "F-FACTORIAL-09" TO PAR-NAME.                           IF1104.2
049600     PERFORM  PRINT-DETAIL.                                       IF1104.2
049700********************END OF TESTS***************                   IF1104.2
049800 CCVS-EXIT SECTION.                                               IF1104.2
049900 CCVS-999999.                                                     IF1104.2
050000     GO TO CLOSE-FILES.                                           IF1104.2
      *END-OF,IF110A                                                                  
