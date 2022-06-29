      *HEADER,COBOL,IF133A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1334.2
000200 PROGRAM-ID.                                                      IF1334.2
000300     IF133A.                                                      IF1334.2
000400                                                                  IF1334.2
000500***********************************************************       IF1334.2
000600*                                                         *       IF1334.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1334.2
000800* It contains tests for the Intrinsic Function            *       IF1334.2
000900* REM.                                                    *       IF1334.2
001000*                                                         *       IF1334.2
001100***********************************************************       IF1334.2
001200 ENVIRONMENT DIVISION.                                            IF1334.2
001300 CONFIGURATION SECTION.                                           IF1334.2
001400 SOURCE-COMPUTER.                                                 IF1334.2
001500     XXXXX082.                                                    IF1334.2
001600 OBJECT-COMPUTER.                                                 IF1334.2
001700     XXXXX083.                                                    IF1334.2
001800 INPUT-OUTPUT SECTION.                                            IF1334.2
001900 FILE-CONTROL.                                                    IF1334.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1334.2
002100     XXXXX055.                                                    IF1334.2
002200 DATA DIVISION.                                                   IF1334.2
002300 FILE SECTION.                                                    IF1334.2
002400 FD  PRINT-FILE.                                                  IF1334.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1334.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1334.2
002700 WORKING-STORAGE SECTION.                                         IF1334.2
002800***********************************************************       IF1334.2
002900* Variables specific to the Intrinsic Function Test IF133A*       IF1334.2
003000***********************************************************       IF1334.2
003100 01  A                   PIC S9(10)          VALUE 5.             IF1334.2
003200 01  B                   PIC S9(5)V9(5)      VALUE 7.36.          IF1334.2
003300 01  C                   PIC S9(10)          VALUE -4.            IF1334.2
003400 01  D                   PIC S9(10)          VALUE 7.             IF1334.2
003500 01  ARG2                PIC S9(10)          VALUE 1.             IF1334.2
003600 01  TEMP                PIC S9(10).                              IF1334.2
003700 01  WS-NUM              PIC S9(5)V9(6).                          IF1334.2
003800 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1334.2
003900 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1334.2
004000*                                                                 IF1334.2
004100**********************************************************        IF1334.2
004200*                                                                 IF1334.2
004300 01  TEST-RESULTS.                                                IF1334.2
004400     02 FILLER                   PIC X      VALUE SPACE.          IF1334.2
004500     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1334.2
004600     02 FILLER                   PIC X      VALUE SPACE.          IF1334.2
004700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1334.2
004800     02 FILLER                   PIC X      VALUE SPACE.          IF1334.2
004900     02  PAR-NAME.                                                IF1334.2
005000       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1334.2
005100       03  PARDOT-X              PIC X      VALUE SPACE.          IF1334.2
005200       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1334.2
005300     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1334.2
005400     02 RE-MARK                  PIC X(61).                       IF1334.2
005500 01  TEST-COMPUTED.                                               IF1334.2
005600     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1334.2
005700     02 FILLER                   PIC X(17)  VALUE                 IF1334.2
005800            "       COMPUTED=".                                   IF1334.2
005900     02 COMPUTED-X.                                               IF1334.2
006000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1334.2
006100     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1334.2
006200                                 PIC -9(9).9(9).                  IF1334.2
006300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1334.2
006400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1334.2
006500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1334.2
006600     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1334.2
006700         04 COMPUTED-18V0                    PIC -9(18).          IF1334.2
006800         04 FILLER                           PIC X.               IF1334.2
006900     03 FILLER PIC X(50) VALUE SPACE.                             IF1334.2
007000 01  TEST-CORRECT.                                                IF1334.2
007100     02 FILLER PIC X(30) VALUE SPACE.                             IF1334.2
007200     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1334.2
007300     02 CORRECT-X.                                                IF1334.2
007400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1334.2
007500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1334.2
007600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1334.2
007700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1334.2
007800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1334.2
007900     03      CR-18V0 REDEFINES CORRECT-A.                         IF1334.2
008000         04 CORRECT-18V0                     PIC -9(18).          IF1334.2
008100         04 FILLER                           PIC X.               IF1334.2
008200     03 FILLER PIC X(2) VALUE SPACE.                              IF1334.2
008300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1334.2
008400 01  TEST-CORRECT-MIN.                                            IF1334.2
008500     02 FILLER PIC X(30) VALUE SPACE.                             IF1334.2
008600     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1334.2
008700     02 CORRECTMI-X.                                              IF1334.2
008800     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1334.2
008900     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1334.2
009000     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1334.2
009100     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1334.2
009200     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1334.2
009300     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1334.2
009400         04 CORRECTMI-18V0                     PIC -9(18).        IF1334.2
009500         04 FILLER                           PIC X.               IF1334.2
009600     03 FILLER PIC X(2) VALUE SPACE.                              IF1334.2
009700     03 FILLER                           PIC X(48) VALUE SPACE.   IF1334.2
009800 01  TEST-CORRECT-MAX.                                            IF1334.2
009900     02 FILLER PIC X(30) VALUE SPACE.                             IF1334.2
010000     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1334.2
010100     02 CORRECTMA-X.                                              IF1334.2
010200     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1334.2
010300     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1334.2
010400     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1334.2
010500     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1334.2
010600     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1334.2
010700     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1334.2
010800         04 CORRECTMA-18V0                     PIC -9(18).        IF1334.2
010900         04 FILLER                           PIC X.               IF1334.2
011000     03 FILLER PIC X(2) VALUE SPACE.                              IF1334.2
011100     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1334.2
011200 01  CCVS-C-1.                                                    IF1334.2
011300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1334.2
011400-    "SS  PARAGRAPH-NAME                                          IF1334.2
011500-    "       REMARKS".                                            IF1334.2
011600     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1334.2
011700 01  CCVS-C-2.                                                    IF1334.2
011800     02 FILLER                     PIC X        VALUE SPACE.      IF1334.2
011900     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1334.2
012000     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1334.2
012100     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1334.2
012200     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1334.2
012300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1334.2
012400 01  REC-CT                        PIC 99       VALUE ZERO.       IF1334.2
012500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1334.2
012600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1334.2
012700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1334.2
012800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1334.2
012900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1334.2
013000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1334.2
013100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1334.2
013200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1334.2
013300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1334.2
013400 01  CCVS-H-1.                                                    IF1334.2
013500     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1334.2
013600     02  FILLER                    PIC X(42)    VALUE             IF1334.2
013700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1334.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1334.2
013900 01  CCVS-H-2A.                                                   IF1334.2
014000   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1334.2
014100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1334.2
014200   02  FILLER                        PIC XXXX   VALUE             IF1334.2
014300     "4.2 ".                                                      IF1334.2
014400   02  FILLER                        PIC X(28)  VALUE             IF1334.2
014500            " COPY - NOT FOR DISTRIBUTION".                       IF1334.2
014600   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1334.2
014700                                                                  IF1334.2
014800 01  CCVS-H-2B.                                                   IF1334.2
014900   02  FILLER                        PIC X(15)  VALUE             IF1334.2
015000            "TEST RESULT OF ".                                    IF1334.2
015100   02  TEST-ID                       PIC X(9).                    IF1334.2
015200   02  FILLER                        PIC X(4)   VALUE             IF1334.2
015300            " IN ".                                               IF1334.2
015400   02  FILLER                        PIC X(12)  VALUE             IF1334.2
015500     " HIGH       ".                                              IF1334.2
015600   02  FILLER                        PIC X(22)  VALUE             IF1334.2
015700            " LEVEL VALIDATION FOR ".                             IF1334.2
015800   02  FILLER                        PIC X(58)  VALUE             IF1334.2
015900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1334.2
016000 01  CCVS-H-3.                                                    IF1334.2
016100     02  FILLER                      PIC X(34)  VALUE             IF1334.2
016200            " FOR OFFICIAL USE ONLY    ".                         IF1334.2
016300     02  FILLER                      PIC X(58)  VALUE             IF1334.2
016400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1334.2
016500     02  FILLER                      PIC X(28)  VALUE             IF1334.2
016600            "  COPYRIGHT   1985 ".                                IF1334.2
016700 01  CCVS-E-1.                                                    IF1334.2
016800     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1334.2
016900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1334.2
017000     02 ID-AGAIN                     PIC X(9).                    IF1334.2
017100     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1334.2
017200 01  CCVS-E-2.                                                    IF1334.2
017300     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1334.2
017400     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1334.2
017500     02 CCVS-E-2-2.                                               IF1334.2
017600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1334.2
017700         03 FILLER                   PIC X      VALUE SPACE.      IF1334.2
017800         03 ENDER-DESC               PIC X(44)  VALUE             IF1334.2
017900            "ERRORS ENCOUNTERED".                                 IF1334.2
018000 01  CCVS-E-3.                                                    IF1334.2
018100     02  FILLER                      PIC X(22)  VALUE             IF1334.2
018200            " FOR OFFICIAL USE ONLY".                             IF1334.2
018300     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1334.2
018400     02  FILLER                      PIC X(58)  VALUE             IF1334.2
018500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1334.2
018600     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1334.2
018700     02 FILLER                       PIC X(15)  VALUE             IF1334.2
018800             " COPYRIGHT 1985".                                   IF1334.2
018900 01  CCVS-E-4.                                                    IF1334.2
019000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1334.2
019100     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1334.2
019200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1334.2
019300     02 FILLER                       PIC X(40)  VALUE             IF1334.2
019400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1334.2
019500 01  XXINFO.                                                      IF1334.2
019600     02 FILLER                       PIC X(19)  VALUE             IF1334.2
019700            "*** INFORMATION ***".                                IF1334.2
019800     02 INFO-TEXT.                                                IF1334.2
019900       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1334.2
020000       04 XXCOMPUTED                 PIC X(20).                   IF1334.2
020100       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1334.2
020200       04 XXCORRECT                  PIC X(20).                   IF1334.2
020300     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1334.2
020400 01  HYPHEN-LINE.                                                 IF1334.2
020500     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1334.2
020600     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1334.2
020700-    "*****************************************".                 IF1334.2
020800     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1334.2
020900-    "******************************".                            IF1334.2
021000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1334.2
021100     "IF133A".                                                    IF1334.2
021200 PROCEDURE DIVISION.                                              IF1334.2
021300 CCVS1 SECTION.                                                   IF1334.2
021400 OPEN-FILES.                                                      IF1334.2
021500     OPEN     OUTPUT PRINT-FILE.                                  IF1334.2
021600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1334.2
021700     MOVE    SPACE TO TEST-RESULTS.                               IF1334.2
021800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1334.2
021900     GO TO CCVS1-EXIT.                                            IF1334.2
022000 CLOSE-FILES.                                                     IF1334.2
022100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1334.2
022200 TERMINATE-CCVS.                                                  IF1334.2
022300     STOP     RUN.                                                IF1334.2
022400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1334.2
022500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1334.2
022600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1334.2
022700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1334.2
022800     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1334.2
022900 PRINT-DETAIL.                                                    IF1334.2
023000     IF REC-CT NOT EQUAL TO ZERO                                  IF1334.2
023100             MOVE "." TO PARDOT-X                                 IF1334.2
023200             MOVE REC-CT TO DOTVALUE.                             IF1334.2
023300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1334.2
023400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1334.2
023500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1334.2
023600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1334.2
023700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1334.2
023800     MOVE SPACE TO CORRECT-X.                                     IF1334.2
023900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1334.2
024000     MOVE     SPACE TO RE-MARK.                                   IF1334.2
024100 HEAD-ROUTINE.                                                    IF1334.2
024200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1334.2
024300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1334.2
024400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1334.2
024500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1334.2
024600 COLUMN-NAMES-ROUTINE.                                            IF1334.2
024700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1334.2
024800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1334.2
024900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1334.2
025000 END-ROUTINE.                                                     IF1334.2
025100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1334.2
025200 END-RTN-EXIT.                                                    IF1334.2
025300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1334.2
025400 END-ROUTINE-1.                                                   IF1334.2
025500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1334.2
025600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1334.2
025700      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1334.2
025800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1334.2
025900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1334.2
026000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1334.2
026100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1334.2
026200  END-ROUTINE-12.                                                 IF1334.2
026300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1334.2
026400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1334.2
026500         MOVE "NO " TO ERROR-TOTAL                                IF1334.2
026600         ELSE                                                     IF1334.2
026700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1334.2
026800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1334.2
026900     PERFORM WRITE-LINE.                                          IF1334.2
027000 END-ROUTINE-13.                                                  IF1334.2
027100     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1334.2
027200         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1334.2
027300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1334.2
027400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1334.2
027500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1334.2
027600      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1334.2
027700          MOVE "NO " TO ERROR-TOTAL                               IF1334.2
027800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1334.2
027900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1334.2
028000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1334.2
028100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1334.2
028200 WRITE-LINE.                                                      IF1334.2
028300     ADD 1 TO RECORD-COUNT.                                       IF1334.2
028400*    IF RECORD-COUNT GREATER 42                                   IF1334.2
028500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1334.2
028600*        MOVE SPACE TO DUMMY-RECORD                               IF1334.2
028700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1334.2
028800*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1334.2
028900*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1334.2
029000*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1334.2
029100*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1334.2
029200*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1334.2
029300*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1334.2
029400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1334.2
029500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1334.2
029600*        MOVE ZERO TO RECORD-COUNT.                               IF1334.2
029700     PERFORM WRT-LN.                                              IF1334.2
029800 WRT-LN.                                                          IF1334.2
029900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1334.2
030000     MOVE SPACE TO DUMMY-RECORD.                                  IF1334.2
030100 BLANK-LINE-PRINT.                                                IF1334.2
030200     PERFORM WRT-LN.                                              IF1334.2
030300 FAIL-ROUTINE.                                                    IF1334.2
030400     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1334.2
030500            GO TO FAIL-ROUTINE-WRITE.                             IF1334.2
030600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1334.2
030700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1334.2
030800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1334.2
030900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1334.2
031000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1334.2
031100     GO TO  FAIL-ROUTINE-EX.                                      IF1334.2
031200 FAIL-ROUTINE-WRITE.                                              IF1334.2
031300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1334.2
031400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1334.2
031500                              CORMA-ANSI-REFERENCE.               IF1334.2
031600     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1334.2
031700           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1334.2
031800           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1334.2
031900     ELSE                                                         IF1334.2
032000           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1334.2
032100     PERFORM WRITE-LINE.                                          IF1334.2
032200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1334.2
032300 FAIL-ROUTINE-EX. EXIT.                                           IF1334.2
032400 BAIL-OUT.                                                        IF1334.2
032500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1334.2
032600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1334.2
032700 BAIL-OUT-WRITE.                                                  IF1334.2
032800     MOVE CORRECT-A TO XXCORRECT.                                 IF1334.2
032900     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1334.2
033000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1334.2
033100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1334.2
033200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1334.2
033300 BAIL-OUT-EX. EXIT.                                               IF1334.2
033400 CCVS1-EXIT.                                                      IF1334.2
033500     EXIT.                                                        IF1334.2
033600********************************************************          IF1334.2
033700*                                                      *          IF1334.2
033800*    Intrinsic Function Tests         IF133A - REM     *          IF1334.2
033900*                                                      *          IF1334.2
034000********************************************************          IF1334.2
034100 SECT-IF133A SECTION.                                             IF1334.2
034200 F-REM-INFO.                                                      IF1334.2
034300     MOVE     "See ref. A-66 2.37" TO ANSI-REFERENCE.             IF1334.2
034400     MOVE     "REM Function" TO FEATURE.                          IF1334.2
034500*****************TEST (a) - SIMPLE TEST*****************          IF1334.2
034600 F-REM-01.                                                        IF1334.2
034700     MOVE ZERO TO WS-NUM.                                         IF1334.2
034800 F-REM-TEST-01.                                                   IF1334.2
034900     COMPUTE WS-NUM = FUNCTION REM(0, 20).                        IF1334.2
035000     IF WS-NUM = 0 THEN                                           IF1334.2
035100                    PERFORM PASS                                  IF1334.2
035200     ELSE                                                         IF1334.2
035300                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
035400                    MOVE 0 TO CORRECT-N                           IF1334.2
035500                    PERFORM FAIL.                                 IF1334.2
035600     GO TO F-REM-WRITE-01.                                        IF1334.2
035700 F-REM-DELETE-01.                                                 IF1334.2
035800     PERFORM  DE-LETE.                                            IF1334.2
035900     GO TO    F-REM-WRITE-01.                                     IF1334.2
036000 F-REM-WRITE-01.                                                  IF1334.2
036100     MOVE "F-REM-01" TO PAR-NAME.                                 IF1334.2
036200     PERFORM  PRINT-DETAIL.                                       IF1334.2
036300*****************TEST (b) - SIMPLE TEST*****************          IF1334.2
036400 F-REM-02.                                                        IF1334.2
036500     EVALUATE FUNCTION REM(10.674, 10.674)                        IF1334.2
036600     WHEN -0.000020 THRU 0.000020                                 IF1334.2
036700                    PERFORM PASS                                  IF1334.2
036800     WHEN OTHER                                                   IF1334.2
036900                    PERFORM FAIL.                                 IF1334.2
037000     GO TO F-REM-WRITE-02.                                        IF1334.2
037100 F-REM-DELETE-02.                                                 IF1334.2
037200     PERFORM  DE-LETE.                                            IF1334.2
037300     GO TO    F-REM-WRITE-02.                                     IF1334.2
037400 F-REM-WRITE-02.                                                  IF1334.2
037500     MOVE "F-REM-02" TO PAR-NAME.                                 IF1334.2
037600     PERFORM  PRINT-DETAIL.                                       IF1334.2
037700*****************TEST (c) - SIMPLE TEST*****************          IF1334.2
037800 F-REM-03.                                                        IF1334.2
037900     IF (FUNCTION REM(2.5, A) >= 2.49995) AND                     IF1334.2
038000        (FUNCTION REM(2.5, A) <= 2.50005)                         IF1334.2
038100                    PERFORM PASS                                  IF1334.2
038200     ELSE                                                         IF1334.2
038300                    PERFORM FAIL.                                 IF1334.2
038400     GO TO F-REM-WRITE-03.                                        IF1334.2
038500 F-REM-DELETE-03.                                                 IF1334.2
038600     PERFORM  DE-LETE.                                            IF1334.2
038700     GO TO    F-REM-WRITE-03.                                     IF1334.2
038800 F-REM-WRITE-03.                                                  IF1334.2
038900     MOVE "F-REM-03" TO PAR-NAME.                                 IF1334.2
039000     PERFORM  PRINT-DETAIL.                                       IF1334.2
039100*****************TEST (d) - SIMPLE TEST*****************          IF1334.2
039200 F-REM-04.                                                        IF1334.2
039300     MOVE ZERO TO WS-NUM.                                         IF1334.2
039400 F-REM-TEST-04.                                                   IF1334.2
039500     COMPUTE WS-NUM = FUNCTION REM(A, 2).                         IF1334.2
039600     IF WS-NUM = 1 THEN                                           IF1334.2
039700                    PERFORM PASS                                  IF1334.2
039800     ELSE                                                         IF1334.2
039900                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
040000                    MOVE 1 TO CORRECT-N                           IF1334.2
040100                    PERFORM FAIL.                                 IF1334.2
040200     GO TO F-REM-WRITE-04.                                        IF1334.2
040300 F-REM-DELETE-04.                                                 IF1334.2
040400     PERFORM  DE-LETE.                                            IF1334.2
040500     GO TO    F-REM-WRITE-04.                                     IF1334.2
040600 F-REM-WRITE-04.                                                  IF1334.2
040700     MOVE "F-REM-04" TO PAR-NAME.                                 IF1334.2
040800     PERFORM  PRINT-DETAIL.                                       IF1334.2
040900*****************TEST (e) - SIMPLE TEST*****************          IF1334.2
041000 F-REM-05.                                                        IF1334.2
041100     MOVE ZERO TO WS-NUM.                                         IF1334.2
041200 F-REM-TEST-05.                                                   IF1334.2
041300     COMPUTE WS-NUM = FUNCTION REM(B, A).                         IF1334.2
041400     IF (WS-NUM >= 2.35995) AND                                   IF1334.2
041500        (WS-NUM <= 2.36005)                                       IF1334.2
041600                    PERFORM PASS                                  IF1334.2
041700     ELSE                                                         IF1334.2
041800                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
041900                    MOVE 2.36 TO CORRECT-N                        IF1334.2
042000                    PERFORM FAIL.                                 IF1334.2
042100     GO TO F-REM-WRITE-05.                                        IF1334.2
042200 F-REM-DELETE-05.                                                 IF1334.2
042300     PERFORM  DE-LETE.                                            IF1334.2
042400     GO TO    F-REM-WRITE-05.                                     IF1334.2
042500 F-REM-WRITE-05.                                                  IF1334.2
042600     MOVE "F-REM-05" TO PAR-NAME.                                 IF1334.2
042700     PERFORM  PRINT-DETAIL.                                       IF1334.2
042800*****************TEST (f) - SIMPLE TEST*****************          IF1334.2
042900 F-REM-06.                                                        IF1334.2
043000     MOVE ZERO TO WS-NUM.                                         IF1334.2
043100 F-REM-TEST-06.                                                   IF1334.2
043200     COMPUTE WS-NUM = FUNCTION REM(-11, -5).                      IF1334.2
043300     IF WS-NUM = -1    THEN                                       IF1334.2
043400                    PERFORM PASS                                  IF1334.2
043500     ELSE                                                         IF1334.2
043600                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
043700                    MOVE -1    TO CORRECT-N                       IF1334.2
043800                    PERFORM FAIL.                                 IF1334.2
043900     GO TO F-REM-WRITE-06.                                        IF1334.2
044000 F-REM-DELETE-06.                                                 IF1334.2
044100     PERFORM  DE-LETE.                                            IF1334.2
044200     GO TO    F-REM-WRITE-06.                                     IF1334.2
044300 F-REM-WRITE-06.                                                  IF1334.2
044400     MOVE "F-REM-06" TO PAR-NAME.                                 IF1334.2
044500     PERFORM  PRINT-DETAIL.                                       IF1334.2
044600*****************TEST (g) - SIMPLE TEST*****************          IF1334.2
044700 F-REM-07.                                                        IF1334.2
044800     MOVE ZERO TO WS-NUM.                                         IF1334.2
044900 F-REM-TEST-07.                                                   IF1334.2
045000     COMPUTE WS-NUM = FUNCTION REM(11, -5).                       IF1334.2
045100     IF WS-NUM = 1 THEN                                           IF1334.2
045200                    PERFORM PASS                                  IF1334.2
045300     ELSE                                                         IF1334.2
045400                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
045500                    MOVE 1 TO CORRECT-N                           IF1334.2
045600                    PERFORM FAIL.                                 IF1334.2
045700     GO TO F-REM-WRITE-07.                                        IF1334.2
045800 F-REM-DELETE-07.                                                 IF1334.2
045900     PERFORM  DE-LETE.                                            IF1334.2
046000     GO TO    F-REM-WRITE-07.                                     IF1334.2
046100 F-REM-WRITE-07.                                                  IF1334.2
046200     MOVE "F-REM-07" TO PAR-NAME.                                 IF1334.2
046300     PERFORM  PRINT-DETAIL.                                       IF1334.2
046400*****************TEST (h) - SIMPLE TEST*****************          IF1334.2
046500 F-REM-08.                                                        IF1334.2
046600     MOVE ZERO TO WS-NUM.                                         IF1334.2
046700 F-REM-TEST-08.                                                   IF1334.2
046800     COMPUTE WS-NUM = FUNCTION REM(-11, 5).                       IF1334.2
046900     IF WS-NUM = -1 THEN                                          IF1334.2
047000                    PERFORM PASS                                  IF1334.2
047100     ELSE                                                         IF1334.2
047200                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
047300                    MOVE -1 TO CORRECT-N                          IF1334.2
047400                    PERFORM FAIL.                                 IF1334.2
047500     GO TO F-REM-WRITE-08.                                        IF1334.2
047600 F-REM-DELETE-08.                                                 IF1334.2
047700     PERFORM  DE-LETE.                                            IF1334.2
047800     GO TO    F-REM-WRITE-08.                                     IF1334.2
047900 F-REM-WRITE-08.                                                  IF1334.2
048000     MOVE "F-REM-08" TO PAR-NAME.                                 IF1334.2
048100     PERFORM  PRINT-DETAIL.                                       IF1334.2
048200*****************TEST (a) - COMPLEX TEST****************          IF1334.2
048300 F-REM-09.                                                        IF1334.2
048400     MOVE ZERO TO WS-NUM.                                         IF1334.2
048500     MOVE  0.889982 TO MIN-RANGE.                                 IF1334.2
048600     MOVE  0.890018 TO MAX-RANGE.                                 IF1334.2
048700 F-REM-TEST-09.                                                   IF1334.2
048800     COMPUTE WS-NUM = FUNCTION REM(0.89, B + 1).                  IF1334.2
048900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1334.2
049000        (WS-NUM <= MAX-RANGE) THEN                                IF1334.2
049100                    PERFORM PASS                                  IF1334.2
049200     ELSE                                                         IF1334.2
049300                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
049400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1334.2
049500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1334.2
049600                    PERFORM FAIL.                                 IF1334.2
049700     GO TO F-REM-WRITE-09.                                        IF1334.2
049800 F-REM-DELETE-09.                                                 IF1334.2
049900     PERFORM  DE-LETE.                                            IF1334.2
050000     GO TO    F-REM-WRITE-09.                                     IF1334.2
050100 F-REM-WRITE-09.                                                  IF1334.2
050200     MOVE "F-REM-09" TO PAR-NAME.                                 IF1334.2
050300     PERFORM  PRINT-DETAIL.                                       IF1334.2
050400*****************TEST (b) - COMPLEX TEST****************          IF1334.2
050500 F-REM-10.                                                        IF1334.2
050600     MOVE ZERO TO WS-NUM.                                         IF1334.2
050700     MOVE  0.159997 TO MIN-RANGE.                                 IF1334.2
050800     MOVE  0.160003 TO MAX-RANGE.                                 IF1334.2
050900 F-REM-TEST-10.                                                   IF1334.2
051000     COMPUTE WS-NUM = FUNCTION REM(B, C + 2.2).                   IF1334.2
051100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1334.2
051200        (WS-NUM <= MAX-RANGE) THEN                                IF1334.2
051300                    PERFORM PASS                                  IF1334.2
051400     ELSE                                                         IF1334.2
051500                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
051600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1334.2
051700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1334.2
051800                    PERFORM FAIL.                                 IF1334.2
051900     GO TO F-REM-WRITE-10.                                        IF1334.2
052000 F-REM-DELETE-10.                                                 IF1334.2
052100     PERFORM  DE-LETE.                                            IF1334.2
052200     GO TO    F-REM-WRITE-10.                                     IF1334.2
052300 F-REM-WRITE-10.                                                  IF1334.2
052400     MOVE "F-REM-10" TO PAR-NAME.                                 IF1334.2
052500     PERFORM  PRINT-DETAIL.                                       IF1334.2
052600*****************TEST (c) - COMPLEX TEST****************          IF1334.2
052700 F-REM-11.                                                        IF1334.2
052800     MOVE ZERO TO WS-NUM.                                         IF1334.2
052900     MOVE -0.000020 TO MIN-RANGE.                                 IF1334.2
053000     MOVE  0.000020 TO MAX-RANGE.                                 IF1334.2
053100 F-REM-TEST-11.                                                   IF1334.2
053200     COMPUTE WS-NUM = FUNCTION REM(3 / 2, .75).                   IF1334.2
053300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1334.2
053400        (WS-NUM <= MAX-RANGE) THEN                                IF1334.2
053500                    PERFORM PASS                                  IF1334.2
053600     ELSE                                                         IF1334.2
053700                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
053800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1334.2
053900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1334.2
054000                    PERFORM FAIL.                                 IF1334.2
054100     GO TO F-REM-WRITE-11.                                        IF1334.2
054200 F-REM-DELETE-11.                                                 IF1334.2
054300     PERFORM  DE-LETE.                                            IF1334.2
054400     GO TO    F-REM-WRITE-11.                                     IF1334.2
054500 F-REM-WRITE-11.                                                  IF1334.2
054600     MOVE "F-REM-11" TO PAR-NAME.                                 IF1334.2
054700     PERFORM  PRINT-DETAIL.                                       IF1334.2
054800*****************TEST (d) - COMPLEX TEST****************          IF1334.2
054900 F-REM-12.                                                        IF1334.2
055000     MOVE ZERO TO WS-NUM.                                         IF1334.2
055100     MOVE  6.63987 TO MIN-RANGE.                                  IF1334.2
055200     MOVE  6.64013 TO MAX-RANGE.                                  IF1334.2
055300 F-REM-TEST-12.                                                   IF1334.2
055400     COMPUTE WS-NUM = FUNCTION REM(8 + 6, B).                     IF1334.2
055500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1334.2
055600        (WS-NUM <= MAX-RANGE) THEN                                IF1334.2
055700                    PERFORM PASS                                  IF1334.2
055800     ELSE                                                         IF1334.2
055900                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
056000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1334.2
056100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1334.2
056200                    PERFORM FAIL.                                 IF1334.2
056300     GO TO F-REM-WRITE-12.                                        IF1334.2
056400 F-REM-DELETE-12.                                                 IF1334.2
056500     PERFORM  DE-LETE.                                            IF1334.2
056600     GO TO    F-REM-WRITE-12.                                     IF1334.2
056700 F-REM-WRITE-12.                                                  IF1334.2
056800     MOVE "F-REM-12" TO PAR-NAME.                                 IF1334.2
056900     PERFORM  PRINT-DETAIL.                                       IF1334.2
057000*****************TEST (e) - COMPLEX TEST****************          IF1334.2
057100 F-REM-13.                                                        IF1334.2
057200     MOVE ZERO TO WS-NUM.                                         IF1334.2
057300     MOVE -1.00002 TO MIN-RANGE.                                  IF1334.2
057400     MOVE -0.999980 TO MAX-RANGE.                                 IF1334.2
057500 F-REM-TEST-13.                                                   IF1334.2
057600     COMPUTE WS-NUM = FUNCTION REM(C + 1, 2).                     IF1334.2
057700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1334.2
057800        (WS-NUM <= MAX-RANGE) THEN                                IF1334.2
057900                    PERFORM PASS                                  IF1334.2
058000     ELSE                                                         IF1334.2
058100                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
058200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1334.2
058300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1334.2
058400                    PERFORM FAIL.                                 IF1334.2
058500     GO TO F-REM-WRITE-13.                                        IF1334.2
058600 F-REM-DELETE-13.                                                 IF1334.2
058700     PERFORM  DE-LETE.                                            IF1334.2
058800     GO TO    F-REM-WRITE-13.                                     IF1334.2
058900 F-REM-WRITE-13.                                                  IF1334.2
059000     MOVE "F-REM-13" TO PAR-NAME.                                 IF1334.2
059100     PERFORM  PRINT-DETAIL.                                       IF1334.2
059200*****************TEST (f) - COMPLEX TEST****************          IF1334.2
059300 F-REM-14.                                                        IF1334.2
059400     MOVE ZERO TO WS-NUM.                                         IF1334.2
059500     MOVE  1.99996 TO MIN-RANGE.                                  IF1334.2
059600     MOVE  2.00004 TO MAX-RANGE.                                  IF1334.2
059700 F-REM-TEST-14.                                                   IF1334.2
059800     COMPUTE WS-NUM = FUNCTION REM(                               IF1334.2
059900                         FUNCTION REM(D, A), A).                  IF1334.2
060000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1334.2
060100        (WS-NUM <= MAX-RANGE) THEN                                IF1334.2
060200                    PERFORM PASS                                  IF1334.2
060300     ELSE                                                         IF1334.2
060400                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
060500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1334.2
060600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1334.2
060700                    PERFORM FAIL.                                 IF1334.2
060800     GO TO F-REM-WRITE-14.                                        IF1334.2
060900 F-REM-DELETE-14.                                                 IF1334.2
061000     PERFORM  DE-LETE.                                            IF1334.2
061100     GO TO    F-REM-WRITE-14.                                     IF1334.2
061200 F-REM-WRITE-14.                                                  IF1334.2
061300     MOVE "F-REM-14" TO PAR-NAME.                                 IF1334.2
061400     PERFORM  PRINT-DETAIL.                                       IF1334.2
061500*****************TEST (g) - COMPLEX TEST****************          IF1334.2
061600 F-REM-15.                                                        IF1334.2
061700     MOVE ZERO TO WS-NUM.                                         IF1334.2
061800     MOVE -0.000020 TO MIN-RANGE.                                 IF1334.2
061900     MOVE  0.000020 TO MAX-RANGE.                                 IF1334.2
062000 F-REM-TEST-15.                                                   IF1334.2
062100     COMPUTE WS-NUM = FUNCTION REM(C,                             IF1334.2
062200                         FUNCTION REM(C, D)).                     IF1334.2
062300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1334.2
062400        (WS-NUM <= MAX-RANGE) THEN                                IF1334.2
062500                    PERFORM PASS                                  IF1334.2
062600     ELSE                                                         IF1334.2
062700                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
062800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1334.2
062900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1334.2
063000                    PERFORM FAIL.                                 IF1334.2
063100     GO TO F-REM-WRITE-15.                                        IF1334.2
063200 F-REM-DELETE-15.                                                 IF1334.2
063300     PERFORM  DE-LETE.                                            IF1334.2
063400     GO TO    F-REM-WRITE-15.                                     IF1334.2
063500 F-REM-WRITE-15.                                                  IF1334.2
063600     MOVE "F-REM-15" TO PAR-NAME.                                 IF1334.2
063700     PERFORM  PRINT-DETAIL.                                       IF1334.2
063800*****************TEST (h) - COMPLEX TEST****************          IF1334.2
063900 F-REM-16.                                                        IF1334.2
064000     MOVE ZERO TO WS-NUM.                                         IF1334.2
064100     MOVE  0.999980 TO MIN-RANGE.                                 IF1334.2
064200     MOVE  1.00002 TO MAX-RANGE.                                  IF1334.2
064300 F-REM-TEST-16.                                                   IF1334.2
064400     COMPUTE WS-NUM = FUNCTION REM( FUNCTION REM(9, 5),           IF1334.2
064500                                    FUNCTION REM(D, 4)).          IF1334.2
064600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1334.2
064700        (WS-NUM <= MAX-RANGE) THEN                                IF1334.2
064800                    PERFORM PASS                                  IF1334.2
064900     ELSE                                                         IF1334.2
065000                    MOVE WS-NUM TO COMPUTED-N                     IF1334.2
065100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1334.2
065200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1334.2
065300                    PERFORM FAIL.                                 IF1334.2
065400     GO TO F-REM-WRITE-16.                                        IF1334.2
065500 F-REM-DELETE-16.                                                 IF1334.2
065600     PERFORM  DE-LETE.                                            IF1334.2
065700     GO TO    F-REM-WRITE-16.                                     IF1334.2
065800 F-REM-WRITE-16.                                                  IF1334.2
065900     MOVE "F-REM-16" TO PAR-NAME.                                 IF1334.2
066000     PERFORM  PRINT-DETAIL.                                       IF1334.2
066100*****************SPECIAL PERFORM TEST**********************       IF1334.2
066200 F-REM-17.                                                        IF1334.2
066300     PERFORM F-REM-TEST-17                                        IF1334.2
066400       UNTIL FUNCTION REM(5, ARG2) >= 2.                          IF1334.2
066500     PERFORM PASS.                                                IF1334.2
066600     GO TO F-REM-WRITE-17.                                        IF1334.2
066700 F-REM-TEST-17.                                                   IF1334.2
066800     COMPUTE ARG2 = ARG2 + 1.                                     IF1334.2
066900 F-REM-DELETE-17.                                                 IF1334.2
067000     PERFORM  DE-LETE.                                            IF1334.2
067100     GO TO    F-REM-WRITE-17.                                     IF1334.2
067200 F-REM-WRITE-17.                                                  IF1334.2
067300     MOVE "F-REM-17" TO PAR-NAME.                                 IF1334.2
067400     PERFORM  PRINT-DETAIL.                                       IF1334.2
067500********************END OF TESTS***************                   IF1334.2
067600 CCVS-EXIT SECTION.                                               IF1334.2
067700 CCVS-999999.                                                     IF1334.2
067800     GO TO CLOSE-FILES.                                           IF1334.2
      *END-OF,IF133A                                                                  
