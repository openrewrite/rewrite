      *HEADER,COBOL,IF139A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1394.2
000200 PROGRAM-ID.                                                      IF1394.2
000300     IF139A.                                                      IF1394.2
000400                                                                  IF1394.2
000500***********************************************************       IF1394.2
000600*                                                         *       IF1394.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1394.2
000800* It contains tests for the Intrinsic Function TAN.       *       IF1394.2
000900*                                                         *       IF1394.2
001000***********************************************************       IF1394.2
001100 ENVIRONMENT DIVISION.                                            IF1394.2
001200 CONFIGURATION SECTION.                                           IF1394.2
001300 SOURCE-COMPUTER.                                                 IF1394.2
001400     XXXXX082.                                                    IF1394.2
001500 OBJECT-COMPUTER.                                                 IF1394.2
001600     XXXXX083.                                                    IF1394.2
001700 INPUT-OUTPUT SECTION.                                            IF1394.2
001800 FILE-CONTROL.                                                    IF1394.2
001900     SELECT PRINT-FILE ASSIGN TO                                  IF1394.2
002000     XXXXX055.                                                    IF1394.2
002100 DATA DIVISION.                                                   IF1394.2
002200 FILE SECTION.                                                    IF1394.2
002300 FD  PRINT-FILE.                                                  IF1394.2
002400 01  PRINT-REC PICTURE X(120).                                    IF1394.2
002500 01  DUMMY-RECORD PICTURE X(120).                                 IF1394.2
002600 WORKING-STORAGE SECTION.                                         IF1394.2
002700***********************************************************       IF1394.2
002800* Variables specific to the Intrinsic Function Test IF139A*       IF1394.2
002900***********************************************************       IF1394.2
003000 01  A                   PIC S9(5)V9(5)      VALUE -0.00004.      IF1394.2
003100 01  B                   PIC S9(5)V9(5)      VALUE 14000.105.     IF1394.2
003200 01  C                   PIC S9(10)          VALUE 100000.        IF1394.2
003300 01  D                   PIC S9(10)          VALUE 1000.          IF1394.2
003400 01  PI                  PIC S9V9(17)        VALUE 3.141592654.   IF1394.2
003500 01  MINUSPI             PIC S9V9(17)        VALUE -3.141592654.  IF1394.2
003600 01  P                   PIC S9(10)          VALUE 1.             IF1394.2
003700 01  ARR                                     VALUE "40537".       IF1394.2
003800     02  IND OCCURS 5 TIMES PIC 9.                                IF1394.2
003900 01  TEMP                PIC S9(5)V9(5).                          IF1394.2
004000 01  WS-NUM              PIC S9(5)V9(7).                          IF1394.2
004100 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1394.2
004200 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1394.2
004300 01  ARG1                PIC S9(5)V9(2)     VALUE 1.              IF1394.2
004400*                                                                 IF1394.2
004500**********************************************************        IF1394.2
004600*                                                                 IF1394.2
004700 01  TEST-RESULTS.                                                IF1394.2
004800     02 FILLER                   PIC X      VALUE SPACE.          IF1394.2
004900     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1394.2
005000     02 FILLER                   PIC X      VALUE SPACE.          IF1394.2
005100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1394.2
005200     02 FILLER                   PIC X      VALUE SPACE.          IF1394.2
005300     02  PAR-NAME.                                                IF1394.2
005400       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1394.2
005500       03  PARDOT-X              PIC X      VALUE SPACE.          IF1394.2
005600       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1394.2
005700     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1394.2
005800     02 RE-MARK                  PIC X(61).                       IF1394.2
005900 01  TEST-COMPUTED.                                               IF1394.2
006000     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1394.2
006100     02 FILLER                   PIC X(17)  VALUE                 IF1394.2
006200            "       COMPUTED=".                                   IF1394.2
006300     02 COMPUTED-X.                                               IF1394.2
006400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1394.2
006500     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1394.2
006600                                 PIC -9(9).9(9).                  IF1394.2
006700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1394.2
006800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1394.2
006900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1394.2
007000     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1394.2
007100         04 COMPUTED-18V0                    PIC -9(18).          IF1394.2
007200         04 FILLER                           PIC X.               IF1394.2
007300     03 FILLER PIC X(50) VALUE SPACE.                             IF1394.2
007400 01  TEST-CORRECT.                                                IF1394.2
007500     02 FILLER PIC X(30) VALUE SPACE.                             IF1394.2
007600     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1394.2
007700     02 CORRECT-X.                                                IF1394.2
007800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1394.2
007900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1394.2
008000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1394.2
008100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1394.2
008200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1394.2
008300     03      CR-18V0 REDEFINES CORRECT-A.                         IF1394.2
008400         04 CORRECT-18V0                     PIC -9(18).          IF1394.2
008500         04 FILLER                           PIC X.               IF1394.2
008600     03 FILLER PIC X(2) VALUE SPACE.                              IF1394.2
008700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1394.2
008800 01  TEST-CORRECT-MIN.                                            IF1394.2
008900     02 FILLER PIC X(30) VALUE SPACE.                             IF1394.2
009000     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1394.2
009100     02 CORRECTMI-X.                                              IF1394.2
009200     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1394.2
009300     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1394.2
009400     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1394.2
009500     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1394.2
009600     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1394.2
009700     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1394.2
009800         04 CORRECTMI-18V0                     PIC -9(18).        IF1394.2
009900         04 FILLER                           PIC X.               IF1394.2
010000     03 FILLER PIC X(2) VALUE SPACE.                              IF1394.2
010100     03 FILLER                           PIC X(48) VALUE SPACE.   IF1394.2
010200 01  TEST-CORRECT-MAX.                                            IF1394.2
010300     02 FILLER PIC X(30) VALUE SPACE.                             IF1394.2
010400     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1394.2
010500     02 CORRECTMA-X.                                              IF1394.2
010600     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1394.2
010700     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1394.2
010800     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1394.2
010900     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1394.2
011000     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1394.2
011100     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1394.2
011200         04 CORRECTMA-18V0                     PIC -9(18).        IF1394.2
011300         04 FILLER                           PIC X.               IF1394.2
011400     03 FILLER PIC X(2) VALUE SPACE.                              IF1394.2
011500     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1394.2
011600 01  CCVS-C-1.                                                    IF1394.2
011700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1394.2
011800-    "SS  PARAGRAPH-NAME                                          IF1394.2
011900-    "       REMARKS".                                            IF1394.2
012000     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1394.2
012100 01  CCVS-C-2.                                                    IF1394.2
012200     02 FILLER                     PIC X        VALUE SPACE.      IF1394.2
012300     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1394.2
012400     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1394.2
012500     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1394.2
012600     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1394.2
012700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1394.2
012800 01  REC-CT                        PIC 99       VALUE ZERO.       IF1394.2
012900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1394.2
013000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1394.2
013100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1394.2
013200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1394.2
013300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1394.2
013400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1394.2
013500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1394.2
013600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1394.2
013700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1394.2
013800 01  CCVS-H-1.                                                    IF1394.2
013900     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1394.2
014000     02  FILLER                    PIC X(42)    VALUE             IF1394.2
014100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1394.2
014200     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1394.2
014300 01  CCVS-H-2A.                                                   IF1394.2
014400   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1394.2
014500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1394.2
014600   02  FILLER                        PIC XXXX   VALUE             IF1394.2
014700     "4.2 ".                                                      IF1394.2
014800   02  FILLER                        PIC X(28)  VALUE             IF1394.2
014900            " COPY - NOT FOR DISTRIBUTION".                       IF1394.2
015000   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1394.2
015100                                                                  IF1394.2
015200 01  CCVS-H-2B.                                                   IF1394.2
015300   02  FILLER                        PIC X(15)  VALUE             IF1394.2
015400            "TEST RESULT OF ".                                    IF1394.2
015500   02  TEST-ID                       PIC X(9).                    IF1394.2
015600   02  FILLER                        PIC X(4)   VALUE             IF1394.2
015700            " IN ".                                               IF1394.2
015800   02  FILLER                        PIC X(12)  VALUE             IF1394.2
015900     " HIGH       ".                                              IF1394.2
016000   02  FILLER                        PIC X(22)  VALUE             IF1394.2
016100            " LEVEL VALIDATION FOR ".                             IF1394.2
016200   02  FILLER                        PIC X(58)  VALUE             IF1394.2
016300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1394.2
016400 01  CCVS-H-3.                                                    IF1394.2
016500     02  FILLER                      PIC X(34)  VALUE             IF1394.2
016600            " FOR OFFICIAL USE ONLY    ".                         IF1394.2
016700     02  FILLER                      PIC X(58)  VALUE             IF1394.2
016800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1394.2
016900     02  FILLER                      PIC X(28)  VALUE             IF1394.2
017000            "  COPYRIGHT   1985 ".                                IF1394.2
017100 01  CCVS-E-1.                                                    IF1394.2
017200     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1394.2
017300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1394.2
017400     02 ID-AGAIN                     PIC X(9).                    IF1394.2
017500     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1394.2
017600 01  CCVS-E-2.                                                    IF1394.2
017700     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1394.2
017800     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1394.2
017900     02 CCVS-E-2-2.                                               IF1394.2
018000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1394.2
018100         03 FILLER                   PIC X      VALUE SPACE.      IF1394.2
018200         03 ENDER-DESC               PIC X(44)  VALUE             IF1394.2
018300            "ERRORS ENCOUNTERED".                                 IF1394.2
018400 01  CCVS-E-3.                                                    IF1394.2
018500     02  FILLER                      PIC X(22)  VALUE             IF1394.2
018600            " FOR OFFICIAL USE ONLY".                             IF1394.2
018700     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1394.2
018800     02  FILLER                      PIC X(58)  VALUE             IF1394.2
018900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1394.2
019000     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1394.2
019100     02 FILLER                       PIC X(15)  VALUE             IF1394.2
019200             " COPYRIGHT 1985".                                   IF1394.2
019300 01  CCVS-E-4.                                                    IF1394.2
019400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1394.2
019500     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1394.2
019600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1394.2
019700     02 FILLER                       PIC X(40)  VALUE             IF1394.2
019800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1394.2
019900 01  XXINFO.                                                      IF1394.2
020000     02 FILLER                       PIC X(19)  VALUE             IF1394.2
020100            "*** INFORMATION ***".                                IF1394.2
020200     02 INFO-TEXT.                                                IF1394.2
020300       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1394.2
020400       04 XXCOMPUTED                 PIC X(20).                   IF1394.2
020500       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1394.2
020600       04 XXCORRECT                  PIC X(20).                   IF1394.2
020700     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1394.2
020800 01  HYPHEN-LINE.                                                 IF1394.2
020900     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1394.2
021000     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1394.2
021100-    "*****************************************".                 IF1394.2
021200     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1394.2
021300-    "******************************".                            IF1394.2
021400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1394.2
021500     "IF139A".                                                    IF1394.2
021600 PROCEDURE DIVISION.                                              IF1394.2
021700 CCVS1 SECTION.                                                   IF1394.2
021800 OPEN-FILES.                                                      IF1394.2
021900     OPEN     OUTPUT PRINT-FILE.                                  IF1394.2
022000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1394.2
022100     MOVE    SPACE TO TEST-RESULTS.                               IF1394.2
022200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1394.2
022300     GO TO CCVS1-EXIT.                                            IF1394.2
022400 CLOSE-FILES.                                                     IF1394.2
022500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1394.2
022600 TERMINATE-CCVS.                                                  IF1394.2
022700     STOP     RUN.                                                IF1394.2
022800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1394.2
022900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1394.2
023000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1394.2
023100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1394.2
023200     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1394.2
023300 PRINT-DETAIL.                                                    IF1394.2
023400     IF REC-CT NOT EQUAL TO ZERO                                  IF1394.2
023500             MOVE "." TO PARDOT-X                                 IF1394.2
023600             MOVE REC-CT TO DOTVALUE.                             IF1394.2
023700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1394.2
023800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1394.2
023900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1394.2
024000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1394.2
024100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1394.2
024200     MOVE SPACE TO CORRECT-X.                                     IF1394.2
024300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1394.2
024400     MOVE     SPACE TO RE-MARK.                                   IF1394.2
024500 HEAD-ROUTINE.                                                    IF1394.2
024600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1394.2
024700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1394.2
024800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1394.2
024900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1394.2
025000 COLUMN-NAMES-ROUTINE.                                            IF1394.2
025100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1394.2
025200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1394.2
025300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1394.2
025400 END-ROUTINE.                                                     IF1394.2
025500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1394.2
025600 END-RTN-EXIT.                                                    IF1394.2
025700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1394.2
025800 END-ROUTINE-1.                                                   IF1394.2
025900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1394.2
026000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1394.2
026100      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1394.2
026200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1394.2
026300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1394.2
026400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1394.2
026500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1394.2
026600  END-ROUTINE-12.                                                 IF1394.2
026700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1394.2
026800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1394.2
026900         MOVE "NO " TO ERROR-TOTAL                                IF1394.2
027000         ELSE                                                     IF1394.2
027100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1394.2
027200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1394.2
027300     PERFORM WRITE-LINE.                                          IF1394.2
027400 END-ROUTINE-13.                                                  IF1394.2
027500     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1394.2
027600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1394.2
027700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1394.2
027800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1394.2
027900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1394.2
028000      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1394.2
028100          MOVE "NO " TO ERROR-TOTAL                               IF1394.2
028200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1394.2
028300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1394.2
028400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1394.2
028500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1394.2
028600 WRITE-LINE.                                                      IF1394.2
028700     ADD 1 TO RECORD-COUNT.                                       IF1394.2
028800*    IF RECORD-COUNT GREATER 42                                   IF1394.2
028900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1394.2
029000*        MOVE SPACE TO DUMMY-RECORD                               IF1394.2
029100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1394.2
029200*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1394.2
029300*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1394.2
029400*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1394.2
029500*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1394.2
029600*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1394.2
029700*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1394.2
029800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1394.2
029900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1394.2
030000*        MOVE ZERO TO RECORD-COUNT.                               IF1394.2
030100     PERFORM WRT-LN.                                              IF1394.2
030200 WRT-LN.                                                          IF1394.2
030300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1394.2
030400     MOVE SPACE TO DUMMY-RECORD.                                  IF1394.2
030500 BLANK-LINE-PRINT.                                                IF1394.2
030600     PERFORM WRT-LN.                                              IF1394.2
030700 FAIL-ROUTINE.                                                    IF1394.2
030800     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1394.2
030900            GO TO FAIL-ROUTINE-WRITE.                             IF1394.2
031000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1394.2
031100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1394.2
031200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1394.2
031300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1394.2
031400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1394.2
031500     GO TO  FAIL-ROUTINE-EX.                                      IF1394.2
031600 FAIL-ROUTINE-WRITE.                                              IF1394.2
031700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1394.2
031800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1394.2
031900                              CORMA-ANSI-REFERENCE.               IF1394.2
032000     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1394.2
032100           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1394.2
032200           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1394.2
032300     ELSE                                                         IF1394.2
032400           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1394.2
032500     PERFORM WRITE-LINE.                                          IF1394.2
032600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1394.2
032700 FAIL-ROUTINE-EX. EXIT.                                           IF1394.2
032800 BAIL-OUT.                                                        IF1394.2
032900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1394.2
033000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1394.2
033100 BAIL-OUT-WRITE.                                                  IF1394.2
033200     MOVE CORRECT-A TO XXCORRECT.                                 IF1394.2
033300     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1394.2
033400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1394.2
033500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1394.2
033600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1394.2
033700 BAIL-OUT-EX. EXIT.                                               IF1394.2
033800 CCVS1-EXIT.                                                      IF1394.2
033900     EXIT.                                                        IF1394.2
034000********************************************************          IF1394.2
034100*                                                      *          IF1394.2
034200*    Intrinsic Function Tests         IF139A - TAN     *          IF1394.2
034300*                                                      *          IF1394.2
034400********************************************************          IF1394.2
034500 SECT-IF139A SECTION.                                             IF1394.2
034600 F-TAN-INFO.                                                      IF1394.2
034700     MOVE     "See ref. A-71 2.43" TO ANSI-REFERENCE.             IF1394.2
034800     MOVE     "TAN Function" TO FEATURE.                          IF1394.2
034900*****************TEST (a) - SIMPLE TEST*****************          IF1394.2
035000 F-TAN-01.                                                        IF1394.2
035100     MOVE ZERO TO WS-NUM.                                         IF1394.2
035200     MOVE -0.000020 TO MIN-RANGE.                                 IF1394.2
035300     MOVE  0.000020 TO MAX-RANGE.                                 IF1394.2
035400 F-TAN-TEST-01.                                                   IF1394.2
035500     COMPUTE WS-NUM = FUNCTION TAN(0).                            IF1394.2
035600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
035700        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
035800                    PERFORM PASS                                  IF1394.2
035900     ELSE                                                         IF1394.2
036000                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
036100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
036200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
036300                    PERFORM FAIL.                                 IF1394.2
036400     GO TO F-TAN-WRITE-01.                                        IF1394.2
036500 F-TAN-DELETE-01.                                                 IF1394.2
036600     PERFORM  DE-LETE.                                            IF1394.2
036700     GO TO    F-TAN-WRITE-01.                                     IF1394.2
036800 F-TAN-WRITE-01.                                                  IF1394.2
036900     MOVE "F-TAN-01" TO PAR-NAME.                                 IF1394.2
037000     PERFORM  PRINT-DETAIL.                                       IF1394.2
037100*****************TEST (b) - SIMPLE TEST*****************          IF1394.2
037200 F-TAN-02.                                                        IF1394.2
037300     EVALUATE FUNCTION TAN(PI)                                    IF1394.2
037400     WHEN -0.000020 THRU 0.000020                                 IF1394.2
037500                    PERFORM PASS                                  IF1394.2
037600     WHEN OTHER                                                   IF1394.2
037700                    PERFORM FAIL.                                 IF1394.2
037800     GO TO F-TAN-WRITE-02.                                        IF1394.2
037900 F-TAN-DELETE-02.                                                 IF1394.2
038000     PERFORM  DE-LETE.                                            IF1394.2
038100     GO TO    F-TAN-WRITE-02.                                     IF1394.2
038200 F-TAN-WRITE-02.                                                  IF1394.2
038300     MOVE "F-TAN-02" TO PAR-NAME.                                 IF1394.2
038400     PERFORM  PRINT-DETAIL.                                       IF1394.2
038500*****************TEST (c) - SIMPLE TEST*****************          IF1394.2
038600 F-TAN-03.                                                        IF1394.2
038700     MOVE -0.000020 TO MIN-RANGE.                                 IF1394.2
038800     MOVE  0.000020 TO MAX-RANGE.                                 IF1394.2
038900 F-TAN-TEST-03.                                                   IF1394.2
039000     IF (FUNCTION TAN(MINUSPI) >= MIN-RANGE) AND                  IF1394.2
039100        (FUNCTION TAN(MINUSPI) <= MAX-RANGE) THEN                 IF1394.2
039200                    PERFORM PASS                                  IF1394.2
039300     ELSE                                                         IF1394.2
039400                    PERFORM FAIL.                                 IF1394.2
039500     GO TO F-TAN-WRITE-03.                                        IF1394.2
039600 F-TAN-DELETE-03.                                                 IF1394.2
039700     PERFORM  DE-LETE.                                            IF1394.2
039800     GO TO    F-TAN-WRITE-03.                                     IF1394.2
039900 F-TAN-WRITE-03.                                                  IF1394.2
040000     MOVE "F-TAN-03" TO PAR-NAME.                                 IF1394.2
040100     PERFORM  PRINT-DETAIL.                                       IF1394.2
040200*****************TEST (d) - SIMPLE TEST*****************          IF1394.2
040300 F-TAN-04.                                                        IF1394.2
040400     MOVE ZERO TO WS-NUM.                                         IF1394.2
040500     MOVE  0.000999 TO MIN-RANGE.                                 IF1394.2
040600     MOVE  0.001000 TO MAX-RANGE.                                 IF1394.2
040700 F-TAN-TEST-04.                                                   IF1394.2
040800     COMPUTE WS-NUM = FUNCTION TAN(.001).                         IF1394.2
040900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
041000        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
041100                    PERFORM PASS                                  IF1394.2
041200     ELSE                                                         IF1394.2
041300                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
041400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
041500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
041600                    PERFORM FAIL.                                 IF1394.2
041700     GO TO F-TAN-WRITE-04.                                        IF1394.2
041800 F-TAN-DELETE-04.                                                 IF1394.2
041900     PERFORM  DE-LETE.                                            IF1394.2
042000     GO TO    F-TAN-WRITE-04.                                     IF1394.2
042100 F-TAN-WRITE-04.                                                  IF1394.2
042200     MOVE "F-TAN-04" TO PAR-NAME.                                 IF1394.2
042300     PERFORM  PRINT-DETAIL.                                       IF1394.2
042400*****************TEST (e) - SIMPLE TEST*****************          IF1394.2
042500 F-TAN-05.                                                        IF1394.2
042600     MOVE ZERO TO WS-NUM.                                         IF1394.2
042700     MOVE  0.000089 TO MIN-RANGE.                                 IF1394.2
042800     MOVE  0.000090 TO MAX-RANGE.                                 IF1394.2
042900 F-TAN-TEST-05.                                                   IF1394.2
043000     COMPUTE WS-NUM = FUNCTION TAN(.00009).                       IF1394.2
043100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
043200        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
043300                    PERFORM PASS                                  IF1394.2
043400     ELSE                                                         IF1394.2
043500                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
043600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
043700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
043800                    PERFORM FAIL.                                 IF1394.2
043900     GO TO F-TAN-WRITE-05.                                        IF1394.2
044000 F-TAN-DELETE-05.                                                 IF1394.2
044100     PERFORM  DE-LETE.                                            IF1394.2
044200     GO TO    F-TAN-WRITE-05.                                     IF1394.2
044300 F-TAN-WRITE-05.                                                  IF1394.2
044400     MOVE "F-TAN-05" TO PAR-NAME.                                 IF1394.2
044500     PERFORM  PRINT-DETAIL.                                       IF1394.2
044600*****************TEST (f) - SIMPLE TEST*****************          IF1394.2
044700 F-TAN-06.                                                        IF1394.2
044800     MOVE ZERO TO WS-NUM.                                         IF1394.2
044900     MOVE -0.000040 TO MIN-RANGE.                                 IF1394.2
045000     MOVE -0.000039 TO MAX-RANGE.                                 IF1394.2
045100 F-TAN-TEST-06.                                                   IF1394.2
045200     COMPUTE WS-NUM = FUNCTION TAN(A).                            IF1394.2
045300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
045400        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
045500                    PERFORM PASS                                  IF1394.2
045600     ELSE                                                         IF1394.2
045700                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
045800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
045900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
046000                    PERFORM FAIL.                                 IF1394.2
046100     GO TO F-TAN-WRITE-06.                                        IF1394.2
046200 F-TAN-DELETE-06.                                                 IF1394.2
046300     PERFORM  DE-LETE.                                            IF1394.2
046400     GO TO    F-TAN-WRITE-06.                                     IF1394.2
046500 F-TAN-WRITE-06.                                                  IF1394.2
046600     MOVE "F-TAN-06" TO PAR-NAME.                                 IF1394.2
046700     PERFORM  PRINT-DETAIL.                                       IF1394.2
046800*****************TEST (g) - SIMPLE TEST*****************          IF1394.2
046900 F-TAN-07.                                                        IF1394.2
047000     MOVE ZERO TO WS-NUM.                                         IF1394.2
047100     MOVE  1.15780 TO MIN-RANGE.                                  IF1394.2
047200     MOVE  1.15784 TO MAX-RANGE.                                  IF1394.2
047300 F-TAN-TEST-07.                                                   IF1394.2
047400     COMPUTE WS-NUM = FUNCTION TAN(IND(P)).                       IF1394.2
047500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
047600        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
047700                    PERFORM PASS                                  IF1394.2
047800     ELSE                                                         IF1394.2
047900                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
048000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
048100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
048200                    PERFORM FAIL.                                 IF1394.2
048300     GO TO F-TAN-WRITE-07.                                        IF1394.2
048400 F-TAN-DELETE-07.                                                 IF1394.2
048500     PERFORM  DE-LETE.                                            IF1394.2
048600     GO TO    F-TAN-WRITE-07.                                     IF1394.2
048700 F-TAN-WRITE-07.                                                  IF1394.2
048800     MOVE "F-TAN-07" TO PAR-NAME.                                 IF1394.2
048900     PERFORM  PRINT-DETAIL.                                       IF1394.2
049000*****************TEST (h) - SIMPLE TEST*****************          IF1394.2
049100 F-TAN-08.                                                        IF1394.2
049200     MOVE ZERO TO WS-NUM.                                         IF1394.2
049300     MOVE  0.871430 TO MIN-RANGE.                                 IF1394.2
049400     MOVE  0.871464 TO MAX-RANGE.                                 IF1394.2
049500 F-TAN-TEST-08.                                                   IF1394.2
049600     COMPUTE WS-NUM = FUNCTION TAN(IND(5)).                       IF1394.2
049700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
049800        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
049900                    PERFORM PASS                                  IF1394.2
050000     ELSE                                                         IF1394.2
050100                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
050200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
050300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
050400                    PERFORM FAIL.                                 IF1394.2
050500     GO TO F-TAN-WRITE-08.                                        IF1394.2
050600 F-TAN-DELETE-08.                                                 IF1394.2
050700     PERFORM  DE-LETE.                                            IF1394.2
050800     GO TO    F-TAN-WRITE-08.                                     IF1394.2
050900 F-TAN-WRITE-08.                                                  IF1394.2
051000     MOVE "F-TAN-08" TO PAR-NAME.                                 IF1394.2
051100     PERFORM  PRINT-DETAIL.                                       IF1394.2
051200*****************TEST (a) - COMPLEX TEST****************          IF1394.2
051300 F-TAN-09.                                                        IF1394.2
051400     MOVE ZERO TO WS-NUM.                                         IF1394.2
051500     MOVE  0.999960 TO MIN-RANGE.                                 IF1394.2
051600     MOVE  1.00004 TO MAX-RANGE.                                  IF1394.2
051700 F-TAN-TEST-09.                                                   IF1394.2
051800     COMPUTE WS-NUM = FUNCTION TAN(PI / 4).                       IF1394.2
051900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
052000        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
052100                    PERFORM PASS                                  IF1394.2
052200     ELSE                                                         IF1394.2
052300                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
052400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
052500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
052600                    PERFORM FAIL.                                 IF1394.2
052700     GO TO F-TAN-WRITE-09.                                        IF1394.2
052800 F-TAN-DELETE-09.                                                 IF1394.2
052900     PERFORM  DE-LETE.                                            IF1394.2
053000     GO TO    F-TAN-WRITE-09.                                     IF1394.2
053100 F-TAN-WRITE-09.                                                  IF1394.2
053200     MOVE "F-TAN-09" TO PAR-NAME.                                 IF1394.2
053300     PERFORM  PRINT-DETAIL.                                       IF1394.2
053400*****************TEST (b) - COMPLEX TEST****************          IF1394.2
053500 F-TAN-10.                                                        IF1394.2
053600     MOVE ZERO TO WS-NUM.                                         IF1394.2
053700     MOVE -1.00004 TO MIN-RANGE.                                  IF1394.2
053800     MOVE -0.999960 TO MAX-RANGE.                                 IF1394.2
053900 F-TAN-TEST-10.                                                   IF1394.2
054000     COMPUTE WS-NUM = FUNCTION TAN((3 * PI) / 4).                 IF1394.2
054100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
054200        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
054300                    PERFORM PASS                                  IF1394.2
054400     ELSE                                                         IF1394.2
054500                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
054600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
054700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
054800                    PERFORM FAIL.                                 IF1394.2
054900     GO TO F-TAN-WRITE-10.                                        IF1394.2
055000 F-TAN-DELETE-10.                                                 IF1394.2
055100     PERFORM  DE-LETE.                                            IF1394.2
055200     GO TO    F-TAN-WRITE-10.                                     IF1394.2
055300 F-TAN-WRITE-10.                                                  IF1394.2
055400     MOVE "F-TAN-10" TO PAR-NAME.                                 IF1394.2
055500     PERFORM  PRINT-DETAIL.                                       IF1394.2
055600*****************TEST (c) - COMPLEX TEST****************          IF1394.2
055700 F-TAN-11.                                                        IF1394.2
055800     MOVE ZERO TO WS-NUM.                                         IF1394.2
055900     MOVE  0.999960 TO MIN-RANGE.                                 IF1394.2
056000     MOVE  1.00004 TO MAX-RANGE.                                  IF1394.2
056100 F-TAN-TEST-11.                                                   IF1394.2
056200     COMPUTE WS-NUM = FUNCTION TAN((5 * PI) / 4).                 IF1394.2
056300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
056400        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
056500                    PERFORM PASS                                  IF1394.2
056600     ELSE                                                         IF1394.2
056700                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
056800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
056900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
057000                    PERFORM FAIL.                                 IF1394.2
057100     GO TO F-TAN-WRITE-11.                                        IF1394.2
057200 F-TAN-DELETE-11.                                                 IF1394.2
057300     PERFORM  DE-LETE.                                            IF1394.2
057400     GO TO    F-TAN-WRITE-11.                                     IF1394.2
057500 F-TAN-WRITE-11.                                                  IF1394.2
057600     MOVE "F-TAN-11" TO PAR-NAME.                                 IF1394.2
057700     PERFORM  PRINT-DETAIL.                                       IF1394.2
057800*****************TEST (d) - COMPLEX TEST****************          IF1394.2
057900 F-TAN-12.                                                        IF1394.2
058000     MOVE ZERO TO WS-NUM.                                         IF1394.2
058100     MOVE -1.00004 TO MIN-RANGE.                                  IF1394.2
058200     MOVE -0.999960 TO MAX-RANGE.                                 IF1394.2
058300 F-TAN-TEST-12.                                                   IF1394.2
058400     COMPUTE WS-NUM = FUNCTION TAN((7 * PI) / 4).                 IF1394.2
058500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
058600        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
058700                    PERFORM PASS                                  IF1394.2
058800     ELSE                                                         IF1394.2
058900                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
059000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
059100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
059200                    PERFORM FAIL.                                 IF1394.2
059300     GO TO F-TAN-WRITE-12.                                        IF1394.2
059400 F-TAN-DELETE-12.                                                 IF1394.2
059500     PERFORM  DE-LETE.                                            IF1394.2
059600     GO TO    F-TAN-WRITE-12.                                     IF1394.2
059700 F-TAN-WRITE-12.                                                  IF1394.2
059800     MOVE "F-TAN-12" TO PAR-NAME.                                 IF1394.2
059900     PERFORM  PRINT-DETAIL.                                       IF1394.2
060000*****************TEST (e) - COMPLEX TEST****************          IF1394.2
060100 F-TAN-13.                                                        IF1394.2
060200     MOVE ZERO TO WS-NUM.                                         IF1394.2
060300     MOVE -1.00004 TO MIN-RANGE.                                  IF1394.2
060400     MOVE -0.999960 TO MAX-RANGE.                                 IF1394.2
060500 F-TAN-TEST-13.                                                   IF1394.2
060600     COMPUTE WS-NUM = FUNCTION TAN(MINUSPI / 4).                  IF1394.2
060700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
060800        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
060900                    PERFORM PASS                                  IF1394.2
061000     ELSE                                                         IF1394.2
061100                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
061200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
061300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
061400                    PERFORM FAIL.                                 IF1394.2
061500     GO TO F-TAN-WRITE-13.                                        IF1394.2
061600 F-TAN-DELETE-13.                                                 IF1394.2
061700     PERFORM  DE-LETE.                                            IF1394.2
061800     GO TO    F-TAN-WRITE-13.                                     IF1394.2
061900 F-TAN-WRITE-13.                                                  IF1394.2
062000     MOVE "F-TAN-13" TO PAR-NAME.                                 IF1394.2
062100     PERFORM  PRINT-DETAIL.                                       IF1394.2
062200*****************TEST (f) - COMPLEX TEST****************          IF1394.2
062300 F-TAN-14.                                                        IF1394.2
062400     MOVE ZERO TO WS-NUM.                                         IF1394.2
062500     MOVE  0.999960 TO MIN-RANGE.                                 IF1394.2
062600     MOVE  1.00004 TO MAX-RANGE.                                  IF1394.2
062700 F-TAN-TEST-14.                                                   IF1394.2
062800     COMPUTE WS-NUM = FUNCTION TAN((3 * MINUSPI) / 4).            IF1394.2
062900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
063000        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
063100                    PERFORM PASS                                  IF1394.2
063200     ELSE                                                         IF1394.2
063300                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
063400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
063500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
063600                    PERFORM FAIL.                                 IF1394.2
063700     GO TO F-TAN-WRITE-14.                                        IF1394.2
063800 F-TAN-DELETE-14.                                                 IF1394.2
063900     PERFORM  DE-LETE.                                            IF1394.2
064000     GO TO    F-TAN-WRITE-14.                                     IF1394.2
064100 F-TAN-WRITE-14.                                                  IF1394.2
064200     MOVE "F-TAN-14" TO PAR-NAME.                                 IF1394.2
064300     PERFORM  PRINT-DETAIL.                                       IF1394.2
064400*****************TEST (g) - COMPLEX TEST****************          IF1394.2
064500 F-TAN-15.                                                        IF1394.2
064600     MOVE ZERO TO WS-NUM.                                         IF1394.2
064700     MOVE -1.00004 TO MIN-RANGE.                                  IF1394.2
064800     MOVE -0.999960 TO MAX-RANGE.                                 IF1394.2
064900 F-TAN-TEST-15.                                                   IF1394.2
065000     COMPUTE WS-NUM = FUNCTION TAN((5 * MINUSPI) / 4).            IF1394.2
065100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
065200        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
065300                    PERFORM PASS                                  IF1394.2
065400     ELSE                                                         IF1394.2
065500                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
065600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
065700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
065800                    PERFORM FAIL.                                 IF1394.2
065900     GO TO F-TAN-WRITE-15.                                        IF1394.2
066000 F-TAN-DELETE-15.                                                 IF1394.2
066100     PERFORM  DE-LETE.                                            IF1394.2
066200     GO TO    F-TAN-WRITE-15.                                     IF1394.2
066300 F-TAN-WRITE-15.                                                  IF1394.2
066400     MOVE "F-TAN-15" TO PAR-NAME.                                 IF1394.2
066500     PERFORM  PRINT-DETAIL.                                       IF1394.2
066600*****************TEST (h) - COMPLEX TEST****************          IF1394.2
066700 F-TAN-16.                                                        IF1394.2
066800     MOVE ZERO TO WS-NUM.                                         IF1394.2
066900     MOVE  0.999960 TO MIN-RANGE.                                 IF1394.2
067000     MOVE  1.00004 TO MAX-RANGE.                                  IF1394.2
067100 F-TAN-TEST-16.                                                   IF1394.2
067200     COMPUTE WS-NUM = FUNCTION TAN((7 * MINUSPI) / 4).            IF1394.2
067300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
067400        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
067500                    PERFORM PASS                                  IF1394.2
067600     ELSE                                                         IF1394.2
067700                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
067800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
067900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
068000                    PERFORM FAIL.                                 IF1394.2
068100     GO TO F-TAN-WRITE-16.                                        IF1394.2
068200 F-TAN-DELETE-16.                                                 IF1394.2
068300     PERFORM  DE-LETE.                                            IF1394.2
068400     GO TO    F-TAN-WRITE-16.                                     IF1394.2
068500 F-TAN-WRITE-16.                                                  IF1394.2
068600     MOVE "F-TAN-16" TO PAR-NAME.                                 IF1394.2
068700     PERFORM  PRINT-DETAIL.                                       IF1394.2
068800*****************TEST (i) - COMPLEX TEST****************          IF1394.2
068900 F-TAN-17.                                                        IF1394.2
069000     MOVE ZERO TO WS-NUM.                                         IF1394.2
069100     MOVE  0.997961 TO MIN-RANGE.                                 IF1394.2
069200     MOVE  0.998041 TO MAX-RANGE.                                 IF1394.2
069300 F-TAN-TEST-17.                                                   IF1394.2
069400     COMPUTE WS-NUM = FUNCTION TAN((PI / 4) - .001).              IF1394.2
069500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
069600        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
069700                    PERFORM PASS                                  IF1394.2
069800     ELSE                                                         IF1394.2
069900                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
070000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
070100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
070200                    PERFORM FAIL.                                 IF1394.2
070300     GO TO F-TAN-WRITE-17.                                        IF1394.2
070400 F-TAN-DELETE-17.                                                 IF1394.2
070500     PERFORM  DE-LETE.                                            IF1394.2
070600     GO TO    F-TAN-WRITE-17.                                     IF1394.2
070700 F-TAN-WRITE-17.                                                  IF1394.2
070800     MOVE "F-TAN-17" TO PAR-NAME.                                 IF1394.2
070900     PERFORM  PRINT-DETAIL.                                       IF1394.2
071000*****************TEST (k) - COMPLEX TEST****************          IF1394.2
071100 F-TAN-19.                                                        IF1394.2
071200     MOVE ZERO TO WS-NUM.                                         IF1394.2
071300     MOVE  0.0055554 TO MIN-RANGE.                                IF1394.2
071400     MOVE  0.0055558 TO MAX-RANGE.                                IF1394.2
071500 F-TAN-TEST-19.                                                   IF1394.2
071600     COMPUTE WS-NUM = FUNCTION TAN(1 / 180).                      IF1394.2
071700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
071800        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
071900                    PERFORM PASS                                  IF1394.2
072000     ELSE                                                         IF1394.2
072100                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
072200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
072300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
072400                    PERFORM FAIL.                                 IF1394.2
072500     GO TO F-TAN-WRITE-19.                                        IF1394.2
072600 F-TAN-DELETE-19.                                                 IF1394.2
072700     PERFORM  DE-LETE.                                            IF1394.2
072800     GO TO    F-TAN-WRITE-19.                                     IF1394.2
072900 F-TAN-WRITE-19.                                                  IF1394.2
073000     MOVE "F-TAN-19" TO PAR-NAME.                                 IF1394.2
073100     PERFORM  PRINT-DETAIL.                                       IF1394.2
073200*****************TEST (l) - COMPLEX TEST****************          IF1394.2
073300 F-TAN-20.                                                        IF1394.2
073400     MOVE ZERO TO WS-NUM.                                         IF1394.2
073500     MOVE  0.965649 TO MIN-RANGE.                                 IF1394.2
073600     MOVE  0.965727 TO MAX-RANGE.                                 IF1394.2
073700 F-TAN-TEST-20.                                                   IF1394.2
073800     COMPUTE WS-NUM = FUNCTION TAN((PI / 4) - (PI / 180)).        IF1394.2
073900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
074000        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
074100                    PERFORM PASS                                  IF1394.2
074200     ELSE                                                         IF1394.2
074300                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
074400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
074500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
074600                    PERFORM FAIL.                                 IF1394.2
074700     GO TO F-TAN-WRITE-20.                                        IF1394.2
074800 F-TAN-DELETE-20.                                                 IF1394.2
074900     PERFORM  DE-LETE.                                            IF1394.2
075000     GO TO    F-TAN-WRITE-20.                                     IF1394.2
075100 F-TAN-WRITE-20.                                                  IF1394.2
075200     MOVE "F-TAN-20" TO PAR-NAME.                                 IF1394.2
075300     PERFORM  PRINT-DETAIL.                                       IF1394.2
075400*****************TEST (m) - COMPLEX TEST****************          IF1394.2
075500 F-TAN-21.                                                        IF1394.2
075600     MOVE ZERO TO WS-NUM.                                         IF1394.2
075700     MOVE  0.034919 TO MIN-RANGE.                                 IF1394.2
075800     MOVE  0.034921 TO MAX-RANGE.                                 IF1394.2
075900 F-TAN-TEST-21.                                                   IF1394.2
076000     COMPUTE WS-NUM = FUNCTION TAN(PI + ((2 * PI) / 180)).        IF1394.2
076100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
076200        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
076300                    PERFORM PASS                                  IF1394.2
076400     ELSE                                                         IF1394.2
076500                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
076600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
076700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
076800                    PERFORM FAIL.                                 IF1394.2
076900     GO TO F-TAN-WRITE-21.                                        IF1394.2
077000 F-TAN-DELETE-21.                                                 IF1394.2
077100     PERFORM  DE-LETE.                                            IF1394.2
077200     GO TO    F-TAN-WRITE-21.                                     IF1394.2
077300 F-TAN-WRITE-21.                                                  IF1394.2
077400     MOVE "F-TAN-21" TO PAR-NAME.                                 IF1394.2
077500     PERFORM  PRINT-DETAIL.                                       IF1394.2
077600*****************TEST (n) - COMPLEX TEST****************          IF1394.2
077700 F-TAN-22.                                                        IF1394.2
077800     MOVE ZERO TO WS-NUM.                                         IF1394.2
077900     MOVE -0.988990 TO MIN-RANGE.                                 IF1394.2
078000     MOVE -0.988910 TO MAX-RANGE.                                 IF1394.2
078100 F-TAN-TEST-22.                                                   IF1394.2
078200     COMPUTE WS-NUM = FUNCTION TAN(((PI * 3) / 4) + (1 / 180)).   IF1394.2
078300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
078400        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
078500                    PERFORM PASS                                  IF1394.2
078600     ELSE                                                         IF1394.2
078700                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
078800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
078900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
079000                    PERFORM FAIL.                                 IF1394.2
079100     GO TO F-TAN-WRITE-22.                                        IF1394.2
079200 F-TAN-DELETE-22.                                                 IF1394.2
079300     PERFORM  DE-LETE.                                            IF1394.2
079400     GO TO    F-TAN-WRITE-22.                                     IF1394.2
079500 F-TAN-WRITE-22.                                                  IF1394.2
079600     MOVE "F-TAN-22" TO PAR-NAME.                                 IF1394.2
079700     PERFORM  PRINT-DETAIL.                                       IF1394.2
079800*****************TEST (o) - COMPLEX TEST****************          IF1394.2
079900 F-TAN-23.                                                        IF1394.2
080000     MOVE ZERO TO WS-NUM.                                         IF1394.2
080100     MOVE  0.977982 TO MIN-RANGE.                                 IF1394.2
080200     MOVE  0.978060 TO MAX-RANGE.                                 IF1394.2
080300 F-TAN-TEST-23.                                                   IF1394.2
080400     COMPUTE WS-NUM = FUNCTION TAN(((PI * 5) / 4) - (2 / 180)).   IF1394.2
080500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
080600        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
080700                    PERFORM PASS                                  IF1394.2
080800     ELSE                                                         IF1394.2
080900                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
081000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
081100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
081200                    PERFORM FAIL.                                 IF1394.2
081300     GO TO F-TAN-WRITE-23.                                        IF1394.2
081400 F-TAN-DELETE-23.                                                 IF1394.2
081500     PERFORM  DE-LETE.                                            IF1394.2
081600     GO TO    F-TAN-WRITE-23.                                     IF1394.2
081700 F-TAN-WRITE-23.                                                  IF1394.2
081800     MOVE "F-TAN-23" TO PAR-NAME.                                 IF1394.2
081900     PERFORM  PRINT-DETAIL.                                       IF1394.2
082000*****************TEST (p) - COMPLEX TEST****************          IF1394.2
082100 F-TAN-24.                                                        IF1394.2
082200     MOVE ZERO TO WS-NUM.                                         IF1394.2
082300     MOVE -2.18512 TO MIN-RANGE.                                  IF1394.2
082400     MOVE -2.18494 TO MAX-RANGE.                                  IF1394.2
082500 F-TAN-TEST-24.                                                   IF1394.2
082600     COMPUTE WS-NUM = FUNCTION TAN(4 / 2).                        IF1394.2
082700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
082800        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
082900                    PERFORM PASS                                  IF1394.2
083000     ELSE                                                         IF1394.2
083100                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
083200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
083300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
083400                    PERFORM FAIL.                                 IF1394.2
083500     GO TO F-TAN-WRITE-24.                                        IF1394.2
083600 F-TAN-DELETE-24.                                                 IF1394.2
083700     PERFORM  DE-LETE.                                            IF1394.2
083800     GO TO    F-TAN-WRITE-24.                                     IF1394.2
083900 F-TAN-WRITE-24.                                                  IF1394.2
084000     MOVE "F-TAN-24" TO PAR-NAME.                                 IF1394.2
084100     PERFORM  PRINT-DETAIL.                                       IF1394.2
084200*****************TEST (q) - COMPLEX TEST****************          IF1394.2
084300 F-TAN-25.                                                        IF1394.2
084400     MOVE ZERO TO WS-NUM.                                         IF1394.2
084500     MOVE  14.1008   TO MIN-RANGE.                                IF1394.2
084600     MOVE  14.1020 TO MAX-RANGE.                                  IF1394.2
084700 F-TAN-TEST-25.                                                   IF1394.2
084800     COMPUTE WS-NUM = FUNCTION TAN(3 / 2).                        IF1394.2
084900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
085000        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
085100                    PERFORM PASS                                  IF1394.2
085200     ELSE                                                         IF1394.2
085300                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
085400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
085500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
085600                    PERFORM FAIL.                                 IF1394.2
085700     GO TO F-TAN-WRITE-25.                                        IF1394.2
085800 F-TAN-DELETE-25.                                                 IF1394.2
085900     PERFORM  DE-LETE.                                            IF1394.2
086000     GO TO    F-TAN-WRITE-25.                                     IF1394.2
086100 F-TAN-WRITE-25.                                                  IF1394.2
086200     MOVE "F-TAN-25" TO PAR-NAME.                                 IF1394.2
086300     PERFORM  PRINT-DETAIL.                                       IF1394.2
086400*****************TEST (s) - COMPLEX TEST****************          IF1394.2
086500 F-TAN-27.                                                        IF1394.2
086600     MOVE ZERO TO WS-NUM.                                         IF1394.2
086700     MOVE  0.648334 TO MIN-RANGE.                                 IF1394.2
086800     MOVE  0.648386 TO MAX-RANGE.                                 IF1394.2
086900 F-TAN-TEST-27.                                                   IF1394.2
087000     COMPUTE WS-NUM = FUNCTION TAN(D / 100).                      IF1394.2
087100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
087200        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
087300                    PERFORM PASS                                  IF1394.2
087400     ELSE                                                         IF1394.2
087500                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
087600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
087700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
087800                    PERFORM FAIL.                                 IF1394.2
087900     GO TO F-TAN-WRITE-27.                                        IF1394.2
088000 F-TAN-DELETE-27.                                                 IF1394.2
088100     PERFORM  DE-LETE.                                            IF1394.2
088200     GO TO    F-TAN-WRITE-27.                                     IF1394.2
088300 F-TAN-WRITE-27.                                                  IF1394.2
088400     MOVE "F-TAN-27" TO PAR-NAME.                                 IF1394.2
088500     PERFORM  PRINT-DETAIL.                                       IF1394.2
088600*****************TEST (t) - COMPLEX TEST****************          IF1394.2
088700 F-TAN-28.                                                        IF1394.2
088800     MOVE ZERO TO WS-NUM.                                         IF1394.2
088900     MOVE  0.017454  TO MIN-RANGE.                                IF1394.2
089000     MOVE  0.017456  TO MAX-RANGE.                                IF1394.2
089100 F-TAN-TEST-28.                                                   IF1394.2
089200     COMPUTE WS-NUM = FUNCTION TAN(PI / 180).                     IF1394.2
089300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
089400        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
089500                    PERFORM PASS                                  IF1394.2
089600     ELSE                                                         IF1394.2
089700                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
089800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
089900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
090000                    PERFORM FAIL.                                 IF1394.2
090100     GO TO F-TAN-WRITE-28.                                        IF1394.2
090200 F-TAN-DELETE-28.                                                 IF1394.2
090300     PERFORM  DE-LETE.                                            IF1394.2
090400     GO TO    F-TAN-WRITE-28.                                     IF1394.2
090500 F-TAN-WRITE-28.                                                  IF1394.2
090600     MOVE "F-TAN-28" TO PAR-NAME.                                 IF1394.2
090700     PERFORM  PRINT-DETAIL.                                       IF1394.2
090800*****************TEST (u) - COMPLEX TEST****************          IF1394.2
090900 F-TAN-29.                                                        IF1394.2
091000     MOVE ZERO TO WS-NUM.                                         IF1394.2
091100     MOVE 0.999960 TO MIN-RANGE.                                  IF1394.2
091200     MOVE 1.00004 TO MAX-RANGE.                                   IF1394.2
091300 F-TAN-TEST-29.                                                   IF1394.2
091400     COMPUTE WS-NUM = FUNCTION TAN(PI) + 1.                       IF1394.2
091500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
091600        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
091700                    PERFORM PASS                                  IF1394.2
091800     ELSE                                                         IF1394.2
091900                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
092000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
092100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
092200                    PERFORM FAIL.                                 IF1394.2
092300     GO TO F-TAN-WRITE-29.                                        IF1394.2
092400 F-TAN-DELETE-29.                                                 IF1394.2
092500     PERFORM  DE-LETE.                                            IF1394.2
092600     GO TO    F-TAN-WRITE-29.                                     IF1394.2
092700 F-TAN-WRITE-29.                                                  IF1394.2
092800     MOVE "F-TAN-29" TO PAR-NAME.                                 IF1394.2
092900     PERFORM  PRINT-DETAIL.                                       IF1394.2
093000*****************TEST (v) - COMPLEX TEST****************          IF1394.2
093100 F-TAN-30.                                                        IF1394.2
093200     MOVE ZERO TO WS-NUM.                                         IF1394.2
093300     MOVE 1.41786 TO MIN-RANGE.                                   IF1394.2
093400     MOVE 1.41798 TO MAX-RANGE.                                   IF1394.2
093500 F-TAN-TEST-30.                                                   IF1394.2
093600     COMPUTE WS-NUM = FUNCTION TAN(FUNCTION TAN(2)).              IF1394.2
093700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
093800        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
093900                    PERFORM PASS                                  IF1394.2
094000     ELSE                                                         IF1394.2
094100                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
094200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
094300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
094400                    PERFORM FAIL.                                 IF1394.2
094500     GO TO F-TAN-WRITE-30.                                        IF1394.2
094600 F-TAN-DELETE-30.                                                 IF1394.2
094700     PERFORM  DE-LETE.                                            IF1394.2
094800     GO TO    F-TAN-WRITE-30.                                     IF1394.2
094900 F-TAN-WRITE-30.                                                  IF1394.2
095000     MOVE "F-TAN-30" TO PAR-NAME.                                 IF1394.2
095100     PERFORM  PRINT-DETAIL.                                       IF1394.2
095200*****************TEST (w) - COMPLEX TEST****************          IF1394.2
095300 F-TAN-31.                                                        IF1394.2
095400     MOVE ZERO TO WS-NUM.                                         IF1394.2
095500     MOVE -0.000040 TO MIN-RANGE.                                 IF1394.2
095600     MOVE  0.000040 TO MAX-RANGE.                                 IF1394.2
095700 F-TAN-TEST-31.                                                   IF1394.2
095800     COMPUTE WS-NUM = FUNCTION TAN(PI / 3) +                      IF1394.2
095900                        FUNCTION TAN(MINUSPI / 3).                IF1394.2
096000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1394.2
096100        (WS-NUM <= MAX-RANGE) THEN                                IF1394.2
096200                    PERFORM PASS                                  IF1394.2
096300     ELSE                                                         IF1394.2
096400                    MOVE WS-NUM TO COMPUTED-N                     IF1394.2
096500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1394.2
096600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1394.2
096700                    PERFORM FAIL.                                 IF1394.2
096800     GO TO F-TAN-WRITE-31.                                        IF1394.2
096900 F-TAN-DELETE-31.                                                 IF1394.2
097000     PERFORM  DE-LETE.                                            IF1394.2
097100     GO TO    F-TAN-WRITE-31.                                     IF1394.2
097200 F-TAN-WRITE-31.                                                  IF1394.2
097300     MOVE "F-TAN-31" TO PAR-NAME.                                 IF1394.2
097400     PERFORM  PRINT-DETAIL.                                       IF1394.2
097500*****************SPECIAL PERFORM TEST**********************       IF1394.2
097600 F-TAN-32.                                                        IF1394.2
097700     PERFORM F-TAN-TEST-32                                        IF1394.2
097800       UNTIL FUNCTION TAN(ARG1) < 0.                              IF1394.2
097900     PERFORM PASS.                                                IF1394.2
098000     GO TO F-TAN-WRITE-32.                                        IF1394.2
098100 F-TAN-TEST-32.                                                   IF1394.2
098200     COMPUTE ARG1 = ARG1 - 0.25.                                  IF1394.2
098300 F-TAN-DELETE-32.                                                 IF1394.2
098400     PERFORM  DE-LETE.                                            IF1394.2
098500     GO TO    F-TAN-WRITE-32.                                     IF1394.2
098600 F-TAN-WRITE-32.                                                  IF1394.2
098700     MOVE "F-TAN-32" TO PAR-NAME.                                 IF1394.2
098800     PERFORM  PRINT-DETAIL.                                       IF1394.2
098900********************END OF TESTS***************                   IF1394.2
099000 CCVS-EXIT SECTION.                                               IF1394.2
099100 CCVS-999999.                                                     IF1394.2
099200     GO TO CLOSE-FILES.                                           IF1394.2
      *END-OF,IF139A                                                                  
