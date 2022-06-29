      *HEADER,COBOL,IF106A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1064.2
000200 PROGRAM-ID.                                                      IF1064.2
000300     IF106A.                                                      IF1064.2
000400                                                                  IF1064.2
000500***********************************************************       IF1064.2
000600*                                                         *       IF1064.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1064.2
000800* It contains tests for the Intrinsic Function COS.       *       IF1064.2
000900*                                                         *       IF1064.2
001000***********************************************************       IF1064.2
001100 ENVIRONMENT DIVISION.                                            IF1064.2
001200 CONFIGURATION SECTION.                                           IF1064.2
001300 SOURCE-COMPUTER.                                                 IF1064.2
001400     XXXXX082.                                                    IF1064.2
001500 OBJECT-COMPUTER.                                                 IF1064.2
001600     XXXXX083.                                                    IF1064.2
001700 INPUT-OUTPUT SECTION.                                            IF1064.2
001800 FILE-CONTROL.                                                    IF1064.2
001900     SELECT PRINT-FILE ASSIGN TO                                  IF1064.2
002000     XXXXX055.                                                    IF1064.2
002100 DATA DIVISION.                                                   IF1064.2
002200 FILE SECTION.                                                    IF1064.2
002300 FD  PRINT-FILE.                                                  IF1064.2
002400 01  PRINT-REC PICTURE X(120).                                    IF1064.2
002500 01  DUMMY-RECORD PICTURE X(120).                                 IF1064.2
002600 WORKING-STORAGE SECTION.                                         IF1064.2
002700***********************************************************       IF1064.2
002800* Variables specific to the Intrinsic Function Test IF106A*       IF1064.2
002900***********************************************************       IF1064.2
003000 01  A                   PIC S9(5)V9(5)      VALUE -0.00004.      IF1064.2
003100 01  B                   PIC S9(5)V9(5)      VALUE 14000.105.     IF1064.2
003200 01  C                   PIC S9(10)          VALUE 100000.        IF1064.2
003300 01  D                   PIC S9(10)          VALUE 1000.          IF1064.2
003400 01  E                   PIC S9(10)          VALUE 3.             IF1064.2
003500 01  PI                  PIC S9V9(17)        VALUE 3.141592654.   IF1064.2
003600 01  MINUSPI             PIC S9V9(17)        VALUE -3.141592654.  IF1064.2
003700 01  ARG1                PIC S9V9(17)        VALUE 1.00.          IF1064.2
003800 01  ARR                                     VALUE "40537".       IF1064.2
003900     02  IND OCCURS 5 TIMES PIC 9.                                IF1064.2
004000 01  TEMP                PIC S9(5)V9(5).                          IF1064.2
004100 01  WS-NUM              PIC S9(5)V9(6).                          IF1064.2
004200 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1064.2
004300 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1064.2
004400*                                                                 IF1064.2
004500**********************************************************        IF1064.2
004600*                                                                 IF1064.2
004700 01  TEST-RESULTS.                                                IF1064.2
004800     02 FILLER                   PIC X      VALUE SPACE.          IF1064.2
004900     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1064.2
005000     02 FILLER                   PIC X      VALUE SPACE.          IF1064.2
005100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1064.2
005200     02 FILLER                   PIC X      VALUE SPACE.          IF1064.2
005300     02  PAR-NAME.                                                IF1064.2
005400       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1064.2
005500       03  PARDOT-X              PIC X      VALUE SPACE.          IF1064.2
005600       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1064.2
005700     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1064.2
005800     02 RE-MARK                  PIC X(61).                       IF1064.2
005900 01  TEST-COMPUTED.                                               IF1064.2
006000     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1064.2
006100     02 FILLER                   PIC X(17)  VALUE                 IF1064.2
006200            "       COMPUTED=".                                   IF1064.2
006300     02 COMPUTED-X.                                               IF1064.2
006400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1064.2
006500     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1064.2
006600                                 PIC -9(9).9(9).                  IF1064.2
006700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1064.2
006800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1064.2
006900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1064.2
007000     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1064.2
007100         04 COMPUTED-18V0                    PIC -9(18).          IF1064.2
007200         04 FILLER                           PIC X.               IF1064.2
007300     03 FILLER PIC X(50) VALUE SPACE.                             IF1064.2
007400 01  TEST-CORRECT.                                                IF1064.2
007500     02 FILLER PIC X(30) VALUE SPACE.                             IF1064.2
007600     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1064.2
007700     02 CORRECT-X.                                                IF1064.2
007800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1064.2
007900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1064.2
008000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1064.2
008100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1064.2
008200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1064.2
008300     03      CR-18V0 REDEFINES CORRECT-A.                         IF1064.2
008400         04 CORRECT-18V0                     PIC -9(18).          IF1064.2
008500         04 FILLER                           PIC X.               IF1064.2
008600     03 FILLER PIC X(2) VALUE SPACE.                              IF1064.2
008700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1064.2
008800 01  TEST-CORRECT-MIN.                                            IF1064.2
008900     02 FILLER PIC X(30) VALUE SPACE.                             IF1064.2
009000     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1064.2
009100     02 CORRECTMI-X.                                              IF1064.2
009200     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1064.2
009300     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1064.2
009400     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1064.2
009500     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1064.2
009600     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1064.2
009700     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1064.2
009800         04 CORRECTMI-18V0                     PIC -9(18).        IF1064.2
009900         04 FILLER                           PIC X.               IF1064.2
010000     03 FILLER PIC X(2) VALUE SPACE.                              IF1064.2
010100     03 FILLER                           PIC X(48) VALUE SPACE.   IF1064.2
010200 01  TEST-CORRECT-MAX.                                            IF1064.2
010300     02 FILLER PIC X(30) VALUE SPACE.                             IF1064.2
010400     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1064.2
010500     02 CORRECTMA-X.                                              IF1064.2
010600     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1064.2
010700     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1064.2
010800     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1064.2
010900     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1064.2
011000     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1064.2
011100     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1064.2
011200         04 CORRECTMA-18V0                     PIC -9(18).        IF1064.2
011300         04 FILLER                           PIC X.               IF1064.2
011400     03 FILLER PIC X(2) VALUE SPACE.                              IF1064.2
011500     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1064.2
011600 01  CCVS-C-1.                                                    IF1064.2
011700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1064.2
011800-    "SS  PARAGRAPH-NAME                                          IF1064.2
011900-    "       REMARKS".                                            IF1064.2
012000     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1064.2
012100 01  CCVS-C-2.                                                    IF1064.2
012200     02 FILLER                     PIC X        VALUE SPACE.      IF1064.2
012300     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1064.2
012400     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1064.2
012500     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1064.2
012600     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1064.2
012700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1064.2
012800 01  REC-CT                        PIC 99       VALUE ZERO.       IF1064.2
012900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1064.2
013000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1064.2
013100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1064.2
013200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1064.2
013300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1064.2
013400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1064.2
013500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1064.2
013600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1064.2
013700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1064.2
013800 01  CCVS-H-1.                                                    IF1064.2
013900     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1064.2
014000     02  FILLER                    PIC X(42)    VALUE             IF1064.2
014100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1064.2
014200     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1064.2
014300 01  CCVS-H-2A.                                                   IF1064.2
014400   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1064.2
014500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1064.2
014600   02  FILLER                        PIC XXXX   VALUE             IF1064.2
014700     "4.2 ".                                                      IF1064.2
014800   02  FILLER                        PIC X(28)  VALUE             IF1064.2
014900            " COPY - NOT FOR DISTRIBUTION".                       IF1064.2
015000   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1064.2
015100                                                                  IF1064.2
015200 01  CCVS-H-2B.                                                   IF1064.2
015300   02  FILLER                        PIC X(15)  VALUE             IF1064.2
015400            "TEST RESULT OF ".                                    IF1064.2
015500   02  TEST-ID                       PIC X(9).                    IF1064.2
015600   02  FILLER                        PIC X(4)   VALUE             IF1064.2
015700            " IN ".                                               IF1064.2
015800   02  FILLER                        PIC X(12)  VALUE             IF1064.2
015900     " HIGH       ".                                              IF1064.2
016000   02  FILLER                        PIC X(22)  VALUE             IF1064.2
016100            " LEVEL VALIDATION FOR ".                             IF1064.2
016200   02  FILLER                        PIC X(58)  VALUE             IF1064.2
016300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1064.2
016400 01  CCVS-H-3.                                                    IF1064.2
016500     02  FILLER                      PIC X(34)  VALUE             IF1064.2
016600            " FOR OFFICIAL USE ONLY    ".                         IF1064.2
016700     02  FILLER                      PIC X(58)  VALUE             IF1064.2
016800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1064.2
016900     02  FILLER                      PIC X(28)  VALUE             IF1064.2
017000            "  COPYRIGHT   1985 ".                                IF1064.2
017100 01  CCVS-E-1.                                                    IF1064.2
017200     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1064.2
017300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1064.2
017400     02 ID-AGAIN                     PIC X(9).                    IF1064.2
017500     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1064.2
017600 01  CCVS-E-2.                                                    IF1064.2
017700     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1064.2
017800     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1064.2
017900     02 CCVS-E-2-2.                                               IF1064.2
018000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1064.2
018100         03 FILLER                   PIC X      VALUE SPACE.      IF1064.2
018200         03 ENDER-DESC               PIC X(44)  VALUE             IF1064.2
018300            "ERRORS ENCOUNTERED".                                 IF1064.2
018400 01  CCVS-E-3.                                                    IF1064.2
018500     02  FILLER                      PIC X(22)  VALUE             IF1064.2
018600            " FOR OFFICIAL USE ONLY".                             IF1064.2
018700     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1064.2
018800     02  FILLER                      PIC X(58)  VALUE             IF1064.2
018900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1064.2
019000     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1064.2
019100     02 FILLER                       PIC X(15)  VALUE             IF1064.2
019200             " COPYRIGHT 1985".                                   IF1064.2
019300 01  CCVS-E-4.                                                    IF1064.2
019400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1064.2
019500     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1064.2
019600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1064.2
019700     02 FILLER                       PIC X(40)  VALUE             IF1064.2
019800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1064.2
019900 01  XXINFO.                                                      IF1064.2
020000     02 FILLER                       PIC X(19)  VALUE             IF1064.2
020100            "*** INFORMATION ***".                                IF1064.2
020200     02 INFO-TEXT.                                                IF1064.2
020300       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1064.2
020400       04 XXCOMPUTED                 PIC X(20).                   IF1064.2
020500       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1064.2
020600       04 XXCORRECT                  PIC X(20).                   IF1064.2
020700     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1064.2
020800 01  HYPHEN-LINE.                                                 IF1064.2
020900     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1064.2
021000     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1064.2
021100-    "*****************************************".                 IF1064.2
021200     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1064.2
021300-    "******************************".                            IF1064.2
021400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1064.2
021500     "IF106A".                                                    IF1064.2
021600 PROCEDURE DIVISION.                                              IF1064.2
021700 CCVS1 SECTION.                                                   IF1064.2
021800 OPEN-FILES.                                                      IF1064.2
021900     OPEN     OUTPUT PRINT-FILE.                                  IF1064.2
022000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1064.2
022100     MOVE    SPACE TO TEST-RESULTS.                               IF1064.2
022200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1064.2
022300     GO TO CCVS1-EXIT.                                            IF1064.2
022400 CLOSE-FILES.                                                     IF1064.2
022500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1064.2
022600 TERMINATE-CCVS.                                                  IF1064.2
022700     STOP     RUN.                                                IF1064.2
022800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1064.2
022900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1064.2
023000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1064.2
023100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1064.2
023200     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1064.2
023300 PRINT-DETAIL.                                                    IF1064.2
023400     IF REC-CT NOT EQUAL TO ZERO                                  IF1064.2
023500             MOVE "." TO PARDOT-X                                 IF1064.2
023600             MOVE REC-CT TO DOTVALUE.                             IF1064.2
023700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1064.2
023800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1064.2
023900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1064.2
024000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1064.2
024100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1064.2
024200     MOVE SPACE TO CORRECT-X.                                     IF1064.2
024300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1064.2
024400     MOVE     SPACE TO RE-MARK.                                   IF1064.2
024500 HEAD-ROUTINE.                                                    IF1064.2
024600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1064.2
024700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1064.2
024800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1064.2
024900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1064.2
025000 COLUMN-NAMES-ROUTINE.                                            IF1064.2
025100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1064.2
025200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1064.2
025300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1064.2
025400 END-ROUTINE.                                                     IF1064.2
025500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1064.2
025600 END-RTN-EXIT.                                                    IF1064.2
025700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1064.2
025800 END-ROUTINE-1.                                                   IF1064.2
025900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1064.2
026000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1064.2
026100      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1064.2
026200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1064.2
026300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1064.2
026400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1064.2
026500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1064.2
026600  END-ROUTINE-12.                                                 IF1064.2
026700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1064.2
026800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1064.2
026900         MOVE "NO " TO ERROR-TOTAL                                IF1064.2
027000         ELSE                                                     IF1064.2
027100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1064.2
027200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1064.2
027300     PERFORM WRITE-LINE.                                          IF1064.2
027400 END-ROUTINE-13.                                                  IF1064.2
027500     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1064.2
027600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1064.2
027700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1064.2
027800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1064.2
027900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1064.2
028000      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1064.2
028100          MOVE "NO " TO ERROR-TOTAL                               IF1064.2
028200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1064.2
028300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1064.2
028400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1064.2
028500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1064.2
028600 WRITE-LINE.                                                      IF1064.2
028700     ADD 1 TO RECORD-COUNT.                                       IF1064.2
028800*    IF RECORD-COUNT GREATER 42                                   IF1064.2
028900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1064.2
029000*        MOVE SPACE TO DUMMY-RECORD                               IF1064.2
029100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1064.2
029200*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1064.2
029300*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1064.2
029400*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1064.2
029500*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1064.2
029600*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1064.2
029700*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1064.2
029800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1064.2
029900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1064.2
030000*        MOVE ZERO TO RECORD-COUNT.                               IF1064.2
030100     PERFORM WRT-LN.                                              IF1064.2
030200 WRT-LN.                                                          IF1064.2
030300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1064.2
030400     MOVE SPACE TO DUMMY-RECORD.                                  IF1064.2
030500 BLANK-LINE-PRINT.                                                IF1064.2
030600     PERFORM WRT-LN.                                              IF1064.2
030700 FAIL-ROUTINE.                                                    IF1064.2
030800     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1064.2
030900            GO TO FAIL-ROUTINE-WRITE.                             IF1064.2
031000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1064.2
031100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1064.2
031200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1064.2
031300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1064.2
031400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1064.2
031500     GO TO  FAIL-ROUTINE-EX.                                      IF1064.2
031600 FAIL-ROUTINE-WRITE.                                              IF1064.2
031700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1064.2
031800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1064.2
031900                              CORMA-ANSI-REFERENCE.               IF1064.2
032000     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1064.2
032100           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1064.2
032200           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1064.2
032300     ELSE                                                         IF1064.2
032400           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1064.2
032500     PERFORM WRITE-LINE.                                          IF1064.2
032600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1064.2
032700 FAIL-ROUTINE-EX. EXIT.                                           IF1064.2
032800 BAIL-OUT.                                                        IF1064.2
032900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1064.2
033000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1064.2
033100 BAIL-OUT-WRITE.                                                  IF1064.2
033200     MOVE CORRECT-A TO XXCORRECT.                                 IF1064.2
033300     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1064.2
033400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1064.2
033500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1064.2
033600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1064.2
033700 BAIL-OUT-EX. EXIT.                                               IF1064.2
033800 CCVS1-EXIT.                                                      IF1064.2
033900     EXIT.                                                        IF1064.2
034000********************************************************          IF1064.2
034100*                                                      *          IF1064.2
034200*    Intrinsic Function Tests         IF106A - COS     *          IF1064.2
034300*                                                      *          IF1064.2
034400********************************************************          IF1064.2
034500 SECT-IF106A SECTION.                                             IF1064.2
034600 F-COS-INFO.                                                      IF1064.2
034700     MOVE     "See ref. A-38 2.8" TO ANSI-REFERENCE.              IF1064.2
034800     MOVE     "COS Function" TO FEATURE.                          IF1064.2
034900*****************TEST (a) - SIMPLE TEST*****************          IF1064.2
035000 F-COS-01.                                                        IF1064.2
035100     MOVE ZERO TO WS-NUM.                                         IF1064.2
035200     MOVE  0.999980 TO MIN-RANGE.                                 IF1064.2
035300     MOVE  1.00000 TO MAX-RANGE.                                  IF1064.2
035400 F-COS-TEST-01.                                                   IF1064.2
035500     COMPUTE WS-NUM = FUNCTION COS(0).                            IF1064.2
035600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
035700        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
035800                    PERFORM PASS                                  IF1064.2
035900     ELSE                                                         IF1064.2
036000                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
036100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
036200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
036300                    PERFORM FAIL.                                 IF1064.2
036400     GO TO F-COS-WRITE-01.                                        IF1064.2
036500 F-COS-DELETE-01.                                                 IF1064.2
036600     PERFORM  DE-LETE.                                            IF1064.2
036700     GO TO    F-COS-WRITE-01.                                     IF1064.2
036800 F-COS-WRITE-01.                                                  IF1064.2
036900     MOVE "F-COS-01" TO PAR-NAME.                                 IF1064.2
037000     PERFORM  PRINT-DETAIL.                                       IF1064.2
037100*****************TEST (b) - SIMPLE TEST*****************          IF1064.2
037200 F-COS-02.                                                        IF1064.2
037300     MOVE ZERO TO WS-NUM.                                         IF1064.2
037400     MOVE -1.00000   TO MIN-RANGE.                                IF1064.2
037500     MOVE -0.999980 TO MAX-RANGE.                                 IF1064.2
037600 F-COS-TEST-02.                                                   IF1064.2
037700     COMPUTE WS-NUM = FUNCTION COS(PI).                           IF1064.2
037800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
037900        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
038000                    PERFORM PASS                                  IF1064.2
038100     ELSE                                                         IF1064.2
038200                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
038300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
038400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
038500                    PERFORM FAIL.                                 IF1064.2
038600     GO TO F-COS-WRITE-02.                                        IF1064.2
038700 F-COS-DELETE-02.                                                 IF1064.2
038800     PERFORM  DE-LETE.                                            IF1064.2
038900     GO TO    F-COS-WRITE-02.                                     IF1064.2
039000 F-COS-WRITE-02.                                                  IF1064.2
039100     MOVE "F-COS-02" TO PAR-NAME.                                 IF1064.2
039200     PERFORM  PRINT-DETAIL.                                       IF1064.2
039300*****************TEST (c) - SIMPLE TEST*****************          IF1064.2
039400 F-COS-03.                                                        IF1064.2
039500     MOVE ZERO TO WS-NUM.                                         IF1064.2
039600     MOVE -1.00000 TO MIN-RANGE.                                  IF1064.2
039700     MOVE -0.999980 TO MAX-RANGE.                                 IF1064.2
039800 F-COS-TEST-03.                                                   IF1064.2
039900     COMPUTE WS-NUM = FUNCTION COS(MINUSPI).                      IF1064.2
040000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
040100        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
040200                    PERFORM PASS                                  IF1064.2
040300     ELSE                                                         IF1064.2
040400                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
040500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
040600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
040700                    PERFORM FAIL.                                 IF1064.2
040800     GO TO F-COS-WRITE-03.                                        IF1064.2
040900 F-COS-DELETE-03.                                                 IF1064.2
041000     PERFORM  DE-LETE.                                            IF1064.2
041100     GO TO    F-COS-WRITE-03.                                     IF1064.2
041200 F-COS-WRITE-03.                                                  IF1064.2
041300     MOVE "F-COS-03" TO PAR-NAME.                                 IF1064.2
041400     PERFORM  PRINT-DETAIL.                                       IF1064.2
041500*****************TEST (d) - SIMPLE TEST*****************          IF1064.2
041600 F-COS-04.                                                        IF1064.2
041700     MOVE ZERO TO WS-NUM.                                         IF1064.2
041800     MOVE  0.999980 TO MIN-RANGE.                                 IF1064.2
041900     MOVE  1.000000 TO MAX-RANGE.                                 IF1064.2
042000 F-COS-TEST-04.                                                   IF1064.2
042100     COMPUTE WS-NUM = FUNCTION COS(0.001).                        IF1064.2
042200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
042300        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
042400                    PERFORM PASS                                  IF1064.2
042500     ELSE                                                         IF1064.2
042600                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
042700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
042800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
042900                    PERFORM FAIL.                                 IF1064.2
043000     GO TO F-COS-WRITE-04.                                        IF1064.2
043100 F-COS-DELETE-04.                                                 IF1064.2
043200     PERFORM  DE-LETE.                                            IF1064.2
043300     GO TO    F-COS-WRITE-04.                                     IF1064.2
043400 F-COS-WRITE-04.                                                  IF1064.2
043500     MOVE "F-COS-04" TO PAR-NAME.                                 IF1064.2
043600     PERFORM  PRINT-DETAIL.                                       IF1064.2
043700*****************TEST (e) - SIMPLE TEST*****************          IF1064.2
043800 F-COS-05.                                                        IF1064.2
043900     MOVE ZERO TO WS-NUM.                                         IF1064.2
044000     MOVE  0.999980 TO MIN-RANGE.                                 IF1064.2
044100     MOVE  1.000000 TO MAX-RANGE.                                 IF1064.2
044200 F-COS-TEST-05.                                                   IF1064.2
044300     COMPUTE WS-NUM = FUNCTION COS(.00009).                       IF1064.2
044400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
044500        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
044600                    PERFORM PASS                                  IF1064.2
044700     ELSE                                                         IF1064.2
044800                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
044900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
045000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
045100                    PERFORM FAIL.                                 IF1064.2
045200     GO TO F-COS-WRITE-05.                                        IF1064.2
045300 F-COS-DELETE-05.                                                 IF1064.2
045400     PERFORM  DE-LETE.                                            IF1064.2
045500     GO TO    F-COS-WRITE-05.                                     IF1064.2
045600 F-COS-WRITE-05.                                                  IF1064.2
045700     MOVE "F-COS-05" TO PAR-NAME.                                 IF1064.2
045800     PERFORM  PRINT-DETAIL.                                       IF1064.2
045900*****************TEST (f) - SIMPLE TEST*****************          IF1064.2
046000 F-COS-06.                                                        IF1064.2
046100     MOVE ZERO TO WS-NUM.                                         IF1064.2
046200     MOVE  0.99998  TO MIN-RANGE.                                 IF1064.2
046300     MOVE  1.000000 TO MAX-RANGE.                                 IF1064.2
046400 F-COS-TEST-06.                                                   IF1064.2
046500     COMPUTE WS-NUM = FUNCTION COS(A).                            IF1064.2
046600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
046700        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
046800                    PERFORM PASS                                  IF1064.2
046900     ELSE                                                         IF1064.2
047000                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
047100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
047200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
047300                    PERFORM FAIL.                                 IF1064.2
047400     GO TO F-COS-WRITE-06.                                        IF1064.2
047500 F-COS-DELETE-06.                                                 IF1064.2
047600     PERFORM  DE-LETE.                                            IF1064.2
047700     GO TO    F-COS-WRITE-06.                                     IF1064.2
047800 F-COS-WRITE-06.                                                  IF1064.2
047900     MOVE "F-COS-06" TO PAR-NAME.                                 IF1064.2
048000     PERFORM  PRINT-DETAIL.                                       IF1064.2
048100*****************TEST (g) - SIMPLE TEST*****************          IF1064.2
048200 F-COS-07.                                                        IF1064.2
048300     MOVE ZERO TO WS-NUM.                                         IF1064.2
048400     MOVE  0.283656 TO MIN-RANGE.                                 IF1064.2
048500     MOVE  0.283668 TO MAX-RANGE.                                 IF1064.2
048600 F-COS-TEST-07.                                                   IF1064.2
048700     COMPUTE WS-NUM = FUNCTION COS(IND(E)).                       IF1064.2
048800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
048900        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
049000                    PERFORM PASS                                  IF1064.2
049100     ELSE                                                         IF1064.2
049200                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
049300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
049400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
049500                    PERFORM FAIL.                                 IF1064.2
049600     GO TO F-COS-WRITE-07.                                        IF1064.2
049700 F-COS-DELETE-07.                                                 IF1064.2
049800     PERFORM  DE-LETE.                                            IF1064.2
049900     GO TO    F-COS-WRITE-07.                                     IF1064.2
050000 F-COS-WRITE-07.                                                  IF1064.2
050100     MOVE "F-COS-07" TO PAR-NAME.                                 IF1064.2
050200     PERFORM  PRINT-DETAIL.                                       IF1064.2
050300*****************TEST (h) - SIMPLE TEST*****************          IF1064.2
050400 F-COS-08.                                                        IF1064.2
050500     MOVE ZERO TO WS-NUM.                                         IF1064.2
050600     MOVE  0.753887 TO MIN-RANGE.                                 IF1064.2
050700     MOVE  0.753917 TO MAX-RANGE.                                 IF1064.2
050800 F-COS-TEST-08.                                                   IF1064.2
050900     COMPUTE WS-NUM = FUNCTION COS(IND(5)).                       IF1064.2
051000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
051100        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
051200                    PERFORM PASS                                  IF1064.2
051300     ELSE                                                         IF1064.2
051400                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
051500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
051600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
051700                    PERFORM FAIL.                                 IF1064.2
051800     GO TO F-COS-WRITE-08.                                        IF1064.2
051900 F-COS-DELETE-08.                                                 IF1064.2
052000     PERFORM  DE-LETE.                                            IF1064.2
052100     GO TO    F-COS-WRITE-08.                                     IF1064.2
052200 F-COS-WRITE-08.                                                  IF1064.2
052300     MOVE "F-COS-08" TO PAR-NAME.                                 IF1064.2
052400     PERFORM  PRINT-DETAIL.                                       IF1064.2
052500*****************TEST (a) - COMPLEX TEST****************          IF1064.2
052600 F-COS-09.                                                        IF1064.2
052700     MOVE ZERO TO WS-NUM.                                         IF1064.2
052800     MOVE  0.499980 TO MIN-RANGE.                                 IF1064.2
052900     MOVE  0.500020 TO MAX-RANGE.                                 IF1064.2
053000 F-COS-TEST-09.                                                   IF1064.2
053100     COMPUTE WS-NUM = FUNCTION COS(PI / 3).                       IF1064.2
053200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
053300        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
053400                    PERFORM PASS                                  IF1064.2
053500     ELSE                                                         IF1064.2
053600                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
053700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
053800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
053900                    PERFORM FAIL.                                 IF1064.2
054000     GO TO F-COS-WRITE-09.                                        IF1064.2
054100 F-COS-DELETE-09.                                                 IF1064.2
054200     PERFORM  DE-LETE.                                            IF1064.2
054300     GO TO    F-COS-WRITE-09.                                     IF1064.2
054400 F-COS-WRITE-09.                                                  IF1064.2
054500     MOVE "F-COS-09" TO PAR-NAME.                                 IF1064.2
054600     PERFORM  PRINT-DETAIL.                                       IF1064.2
054700*****************TEST (b) - COMPLEX TEST****************          IF1064.2
054800 F-COS-10.                                                        IF1064.2
054900     MOVE ZERO TO WS-NUM.                                         IF1064.2
055000     MOVE -0.000040 TO MIN-RANGE.                                 IF1064.2
055100     MOVE  0.000040 TO MAX-RANGE.                                 IF1064.2
055200 F-COS-TEST-10.                                                   IF1064.2
055300     COMPUTE WS-NUM = FUNCTION COS(PI / 2).                       IF1064.2
055400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
055500        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
055600                    PERFORM PASS                                  IF1064.2
055700     ELSE                                                         IF1064.2
055800                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
055900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
056000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
056100                    PERFORM FAIL.                                 IF1064.2
056200     GO TO F-COS-WRITE-10.                                        IF1064.2
056300 F-COS-DELETE-10.                                                 IF1064.2
056400     PERFORM  DE-LETE.                                            IF1064.2
056500     GO TO    F-COS-WRITE-10.                                     IF1064.2
056600 F-COS-WRITE-10.                                                  IF1064.2
056700     MOVE "F-COS-10" TO PAR-NAME.                                 IF1064.2
056800     PERFORM  PRINT-DETAIL.                                       IF1064.2
056900*****************TEST (c) - COMPLEX TEST****************          IF1064.2
057000 F-COS-11.                                                        IF1064.2
057100     MOVE ZERO TO WS-NUM.                                         IF1064.2
057200     MOVE -0.000040 TO MIN-RANGE.                                 IF1064.2
057300     MOVE  0.000040 TO MAX-RANGE.                                 IF1064.2
057400 F-COS-TEST-11.                                                   IF1064.2
057500     COMPUTE WS-NUM = FUNCTION COS((3 * PI) / 2).                 IF1064.2
057600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
057700        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
057800                    PERFORM PASS                                  IF1064.2
057900     ELSE                                                         IF1064.2
058000                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
058100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
058200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
058300                    PERFORM FAIL.                                 IF1064.2
058400     GO TO F-COS-WRITE-11.                                        IF1064.2
058500 F-COS-DELETE-11.                                                 IF1064.2
058600     PERFORM  DE-LETE.                                            IF1064.2
058700     GO TO    F-COS-WRITE-11.                                     IF1064.2
058800 F-COS-WRITE-11.                                                  IF1064.2
058900     MOVE "F-COS-11" TO PAR-NAME.                                 IF1064.2
059000     PERFORM  PRINT-DETAIL.                                       IF1064.2
059100*****************TEST (d) - COMPLEX TEST****************          IF1064.2
059200 F-COS-12.                                                        IF1064.2
059300     MOVE ZERO TO WS-NUM.                                         IF1064.2
059400     MOVE  0.499980 TO MIN-RANGE.                                 IF1064.2
059500     MOVE  0.500002 TO MAX-RANGE.                                 IF1064.2
059600 F-COS-TEST-12.                                                   IF1064.2
059700     COMPUTE WS-NUM = FUNCTION COS(MINUSPI / 3).                  IF1064.2
059800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
059900        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
060000                    PERFORM PASS                                  IF1064.2
060100     ELSE                                                         IF1064.2
060200                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
060300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
060400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
060500                    PERFORM FAIL.                                 IF1064.2
060600     GO TO F-COS-WRITE-12.                                        IF1064.2
060700 F-COS-DELETE-12.                                                 IF1064.2
060800     PERFORM  DE-LETE.                                            IF1064.2
060900     GO TO    F-COS-WRITE-12.                                     IF1064.2
061000 F-COS-WRITE-12.                                                  IF1064.2
061100     MOVE "F-COS-12" TO PAR-NAME.                                 IF1064.2
061200     PERFORM  PRINT-DETAIL.                                       IF1064.2
061300*****************TEST (e) - COMPLEX TEST****************          IF1064.2
061400 F-COS-13.                                                        IF1064.2
061500     MOVE ZERO TO WS-NUM.                                         IF1064.2
061600     MOVE -0.000040 TO MIN-RANGE.                                 IF1064.2
061700     MOVE  0.000040 TO MAX-RANGE.                                 IF1064.2
061800 F-COS-TEST-13.                                                   IF1064.2
061900     COMPUTE WS-NUM = FUNCTION COS(MINUSPI / 2).                  IF1064.2
062000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
062100        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
062200                    PERFORM PASS                                  IF1064.2
062300     ELSE                                                         IF1064.2
062400                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
062500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
062600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
062700                    PERFORM FAIL.                                 IF1064.2
062800     GO TO F-COS-WRITE-13.                                        IF1064.2
062900 F-COS-DELETE-13.                                                 IF1064.2
063000     PERFORM  DE-LETE.                                            IF1064.2
063100     GO TO    F-COS-WRITE-13.                                     IF1064.2
063200 F-COS-WRITE-13.                                                  IF1064.2
063300     MOVE "F-COS-13" TO PAR-NAME.                                 IF1064.2
063400     PERFORM  PRINT-DETAIL.                                       IF1064.2
063500*****************TEST (f) - COMPLEX TEST****************          IF1064.2
063600 F-COS-14.                                                        IF1064.2
063700     MOVE ZERO TO WS-NUM.                                         IF1064.2
063800     MOVE -0.000040 TO MIN-RANGE.                                 IF1064.2
063900     MOVE  0.000040 TO MAX-RANGE.                                 IF1064.2
064000 F-COS-TEST-14.                                                   IF1064.2
064100     COMPUTE WS-NUM = FUNCTION COS((3 * MINUSPI) / 2).            IF1064.2
064200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
064300        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
064400                    PERFORM PASS                                  IF1064.2
064500     ELSE                                                         IF1064.2
064600                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
064700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
064800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
064900                    PERFORM FAIL.                                 IF1064.2
065000     GO TO F-COS-WRITE-14.                                        IF1064.2
065100 F-COS-DELETE-14.                                                 IF1064.2
065200     PERFORM  DE-LETE.                                            IF1064.2
065300     GO TO    F-COS-WRITE-14.                                     IF1064.2
065400 F-COS-WRITE-14.                                                  IF1064.2
065500     MOVE "F-COS-14" TO PAR-NAME.                                 IF1064.2
065600     PERFORM  PRINT-DETAIL.                                       IF1064.2
065700*****************TEST (h) - COMPLEX TEST****************          IF1064.2
065800 F-COS-16.                                                        IF1064.2
065900     MOVE ZERO TO WS-NUM.                                         IF1064.2
066000     MOVE  0.499113 TO MIN-RANGE.                                 IF1064.2
066100     MOVE  0.499153 TO MAX-RANGE.                                 IF1064.2
066200 F-COS-TEST-16.                                                   IF1064.2
066300     COMPUTE WS-NUM = FUNCTION COS((PI / 3) + 0.001).             IF1064.2
066400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
066500        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
066600                    PERFORM PASS                                  IF1064.2
066700     ELSE                                                         IF1064.2
066800                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
066900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
067000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
067100                    PERFORM FAIL.                                 IF1064.2
067200     GO TO F-COS-WRITE-16.                                        IF1064.2
067300 F-COS-DELETE-16.                                                 IF1064.2
067400     PERFORM  DE-LETE.                                            IF1064.2
067500     GO TO    F-COS-WRITE-16.                                     IF1064.2
067600 F-COS-WRITE-16.                                                  IF1064.2
067700     MOVE "F-COS-16" TO PAR-NAME.                                 IF1064.2
067800     PERFORM  PRINT-DETAIL.                                       IF1064.2
067900*****************TEST (j) - COMPLEX TEST****************          IF1064.2
068000 F-COS-18.                                                        IF1064.2
068100     MOVE ZERO TO WS-NUM.                                         IF1064.2
068200     MOVE  0.999350 TO MIN-RANGE.                                 IF1064.2
068300     MOVE  0.999430 TO MAX-RANGE.                                 IF1064.2
068400 F-COS-TEST-18.                                                   IF1064.2
068500     COMPUTE WS-NUM = FUNCTION COS(PI * (4 - 2) / 180).           IF1064.2
068600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
068700        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
068800                    PERFORM PASS                                  IF1064.2
068900     ELSE                                                         IF1064.2
069000                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
069100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
069200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
069300                    PERFORM FAIL.                                 IF1064.2
069400     GO TO F-COS-WRITE-18.                                        IF1064.2
069500 F-COS-DELETE-18.                                                 IF1064.2
069600     PERFORM  DE-LETE.                                            IF1064.2
069700     GO TO    F-COS-WRITE-18.                                     IF1064.2
069800 F-COS-WRITE-18.                                                  IF1064.2
069900     MOVE "F-COS-18" TO PAR-NAME.                                 IF1064.2
070000     PERFORM  PRINT-DETAIL.                                       IF1064.2
070100*****************TEST (k) - COMPLEX TEST****************          IF1064.2
070200 F-COS-19.                                                        IF1064.2
070300     MOVE ZERO TO WS-NUM.                                         IF1064.2
070400     MOVE  0.017451 TO MIN-RANGE.                                 IF1064.2
070500     MOVE  0.017453 TO MAX-RANGE.                                 IF1064.2
070600 F-COS-TEST-19.                                                   IF1064.2
070700     COMPUTE WS-NUM = FUNCTION COS((PI / 2) - (PI / 180)).        IF1064.2
070800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
070900        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
071000                    PERFORM PASS                                  IF1064.2
071100     ELSE                                                         IF1064.2
071200                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
071300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
071400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
071500                    PERFORM FAIL.                                 IF1064.2
071600     GO TO F-COS-WRITE-19.                                        IF1064.2
071700 F-COS-DELETE-19.                                                 IF1064.2
071800     PERFORM  DE-LETE.                                            IF1064.2
071900     GO TO    F-COS-WRITE-19.                                     IF1064.2
072000 F-COS-WRITE-19.                                                  IF1064.2
072100     MOVE "F-COS-19" TO PAR-NAME.                                 IF1064.2
072200     PERFORM  PRINT-DETAIL.                                       IF1064.2
072300*****************TEST (l) - COMPLEX TEST****************          IF1064.2
072400 F-COS-20.                                                        IF1064.2
072500     MOVE ZERO TO WS-NUM.                                         IF1064.2
072600     MOVE  0.515017  TO MIN-RANGE.                                IF1064.2
072700     MOVE  0.515059 TO MAX-RANGE.                                 IF1064.2
072800 F-COS-TEST-20.                                                   IF1064.2
072900     COMPUTE WS-NUM = FUNCTION COS((PI / 3) - (PI / 180)).        IF1064.2
073000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
073100        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
073200                    PERFORM PASS                                  IF1064.2
073300     ELSE                                                         IF1064.2
073400                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
073500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
073600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
073700                    PERFORM FAIL.                                 IF1064.2
073800     GO TO F-COS-WRITE-20.                                        IF1064.2
073900 F-COS-DELETE-20.                                                 IF1064.2
074000     PERFORM  DE-LETE.                                            IF1064.2
074100     GO TO    F-COS-WRITE-20.                                     IF1064.2
074200 F-COS-WRITE-20.                                                  IF1064.2
074300     MOVE "F-COS-20" TO PAR-NAME.                                 IF1064.2
074400     PERFORM  PRINT-DETAIL.                                       IF1064.2
074500*****************TEST (m) - COMPLEX TEST****************          IF1064.2
074600 F-COS-21.                                                        IF1064.2
074700     MOVE ZERO TO WS-NUM.                                         IF1064.2
074800     MOVE -0.999887 TO MIN-RANGE.                                 IF1064.2
074900     MOVE -0.999807 TO MAX-RANGE.                                 IF1064.2
075000 F-COS-TEST-21.                                                   IF1064.2
075100     COMPUTE WS-NUM = FUNCTION COS(PI + (PI / 180)).              IF1064.2
075200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
075300        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
075400                    PERFORM PASS                                  IF1064.2
075500     ELSE                                                         IF1064.2
075600                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
075700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
075800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
075900                    PERFORM FAIL.                                 IF1064.2
076000     GO TO F-COS-WRITE-21.                                        IF1064.2
076100 F-COS-DELETE-21.                                                 IF1064.2
076200     PERFORM  DE-LETE.                                            IF1064.2
076300     GO TO    F-COS-WRITE-21.                                     IF1064.2
076400 F-COS-WRITE-21.                                                  IF1064.2
076500     MOVE "F-COS-21" TO PAR-NAME.                                 IF1064.2
076600     PERFORM  PRINT-DETAIL.                                       IF1064.2
076700*****************TEST (n) - COMPLEX TEST****************          IF1064.2
076800 F-COS-22.                                                        IF1064.2
076900     MOVE ZERO TO WS-NUM.                                         IF1064.2
077000     MOVE  0.034898 TO MIN-RANGE.                                 IF1064.2
077100     MOVE  0.034900 TO MAX-RANGE.                                 IF1064.2
077200 F-COS-TEST-22.                                                   IF1064.2
077300     COMPUTE WS-NUM = FUNCTION COS(( PI * 272) / 180).            IF1064.2
077400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
077500        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
077600                    PERFORM PASS                                  IF1064.2
077700     ELSE                                                         IF1064.2
077800                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
077900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
078000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
078100                    PERFORM FAIL.                                 IF1064.2
078200     GO TO F-COS-WRITE-22.                                        IF1064.2
078300 F-COS-DELETE-22.                                                 IF1064.2
078400     PERFORM  DE-LETE.                                            IF1064.2
078500     GO TO    F-COS-WRITE-22.                                     IF1064.2
078600 F-COS-WRITE-22.                                                  IF1064.2
078700     MOVE "F-COS-22" TO PAR-NAME.                                 IF1064.2
078800     PERFORM  PRINT-DETAIL.                                       IF1064.2
078900*****************TEST (o) - COMPLEX TEST****************          IF1064.2
079000 F-COS-23.                                                        IF1064.2
079100     MOVE ZERO TO WS-NUM.                                         IF1064.2
079200     MOVE -0.416163  TO MIN-RANGE.                                IF1064.2
079300     MOVE -0.416129 TO MAX-RANGE.                                 IF1064.2
079400 F-COS-TEST-23.                                                   IF1064.2
079500     COMPUTE WS-NUM = FUNCTION COS(4 / 2).                        IF1064.2
079600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
079700        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
079800                    PERFORM PASS                                  IF1064.2
079900     ELSE                                                         IF1064.2
080000                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
080100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
080200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
080300                    PERFORM FAIL.                                 IF1064.2
080400     GO TO F-COS-WRITE-23.                                        IF1064.2
080500 F-COS-DELETE-23.                                                 IF1064.2
080600     PERFORM  DE-LETE.                                            IF1064.2
080700     GO TO    F-COS-WRITE-23.                                     IF1064.2
080800 F-COS-WRITE-23.                                                  IF1064.2
080900     MOVE "F-COS-23" TO PAR-NAME.                                 IF1064.2
081000     PERFORM  PRINT-DETAIL.                                       IF1064.2
081100*****************TEST (p) - COMPLEX TEST****************          IF1064.2
081200 F-COS-24.                                                        IF1064.2
081300     MOVE ZERO TO WS-NUM.                                         IF1064.2
081400     MOVE  0.070734 TO MIN-RANGE.                                 IF1064.2
081500     MOVE  0.070740 TO MAX-RANGE.                                 IF1064.2
081600 F-COS-TEST-24.                                                   IF1064.2
081700     COMPUTE WS-NUM = FUNCTION COS(3 / 2).                        IF1064.2
081800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
081900        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
082000                    PERFORM PASS                                  IF1064.2
082100     ELSE                                                         IF1064.2
082200                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
082300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
082400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
082500                    PERFORM FAIL.                                 IF1064.2
082600     GO TO F-COS-WRITE-24.                                        IF1064.2
082700 F-COS-DELETE-24.                                                 IF1064.2
082800     PERFORM  DE-LETE.                                            IF1064.2
082900     GO TO    F-COS-WRITE-24.                                     IF1064.2
083000 F-COS-WRITE-24.                                                  IF1064.2
083100     MOVE "F-COS-24" TO PAR-NAME.                                 IF1064.2
083200     PERFORM  PRINT-DETAIL.                                       IF1064.2
083300*****************TEST (q) - COMPLEX TEST****************          IF1064.2
083400 F-COS-25.                                                        IF1064.2
083500     MOVE ZERO TO WS-NUM.                                         IF1064.2
083600     MOVE -1.000000 TO MIN-RANGE.                                 IF1064.2
083700     MOVE -0.999960 TO MAX-RANGE.                                 IF1064.2
083800 F-COS-TEST-25.                                                   IF1064.2
083900     COMPUTE WS-NUM = FUNCTION COS(PI - A).                       IF1064.2
084000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
084100        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
084200                    PERFORM PASS                                  IF1064.2
084300     ELSE                                                         IF1064.2
084400                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
084500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
084600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
084700                    PERFORM FAIL.                                 IF1064.2
084800     GO TO F-COS-WRITE-25.                                        IF1064.2
084900 F-COS-DELETE-25.                                                 IF1064.2
085000     PERFORM  DE-LETE.                                            IF1064.2
085100     GO TO    F-COS-WRITE-25.                                     IF1064.2
085200 F-COS-WRITE-25.                                                  IF1064.2
085300     MOVE "F-COS-25" TO PAR-NAME.                                 IF1064.2
085400     PERFORM  PRINT-DETAIL.                                       IF1064.2
085500*****************TEST (r) - COMPLEX TEST****************          IF1064.2
085600 F-COS-26.                                                        IF1064.2
085700     MOVE ZERO TO WS-NUM.                                         IF1064.2
085800     MOVE -0.839105 TO MIN-RANGE.                                 IF1064.2
085900     MOVE -0.839037 TO MAX-RANGE.                                 IF1064.2
086000 F-COS-TEST-26.                                                   IF1064.2
086100     COMPUTE WS-NUM = FUNCTION COS(D / 100).                      IF1064.2
086200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
086300        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
086400                    PERFORM PASS                                  IF1064.2
086500     ELSE                                                         IF1064.2
086600                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
086700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
086800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
086900                    PERFORM FAIL.                                 IF1064.2
087000     GO TO F-COS-WRITE-26.                                        IF1064.2
087100 F-COS-DELETE-26.                                                 IF1064.2
087200     PERFORM  DE-LETE.                                            IF1064.2
087300     GO TO    F-COS-WRITE-26.                                     IF1064.2
087400 F-COS-WRITE-26.                                                  IF1064.2
087500     MOVE "F-COS-26" TO PAR-NAME.                                 IF1064.2
087600     PERFORM  PRINT-DETAIL.                                       IF1064.2
087700*****************TEST (s) - COMPLEX TEST****************          IF1064.2
087800 F-COS-27.                                                        IF1064.2
087900     MOVE ZERO TO WS-NUM.                                         IF1064.2
088000     MOVE  0.999807  TO MIN-RANGE.                                IF1064.2
088100     MOVE  0.999887 TO MAX-RANGE.                                 IF1064.2
088200 F-COS-TEST-27.                                                   IF1064.2
088300     COMPUTE WS-NUM = FUNCTION COS(PI / 180).                     IF1064.2
088400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
088500        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
088600                    PERFORM PASS                                  IF1064.2
088700     ELSE                                                         IF1064.2
088800                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
088900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
089000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
089100                    PERFORM FAIL.                                 IF1064.2
089200     GO TO F-COS-WRITE-27.                                        IF1064.2
089300 F-COS-DELETE-27.                                                 IF1064.2
089400     PERFORM  DE-LETE.                                            IF1064.2
089500     GO TO    F-COS-WRITE-27.                                     IF1064.2
089600 F-COS-WRITE-27.                                                  IF1064.2
089700     MOVE "F-COS-27" TO PAR-NAME.                                 IF1064.2
089800     PERFORM  PRINT-DETAIL.                                       IF1064.2
089900*****************TEST (t) - COMPLEX TEST****************          IF1064.2
090000 F-COS-28.                                                        IF1064.2
090100     MOVE ZERO TO WS-NUM.                                         IF1064.2
090200     MOVE -1.000000 TO MIN-RANGE.                                 IF1064.2
090300     MOVE -0.999960 TO MAX-RANGE.                                 IF1064.2
090400 F-COS-TEST-28.                                                   IF1064.2
090500     COMPUTE WS-NUM = FUNCTION COS(PI - 0.001).                   IF1064.2
090600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
090700        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
090800                    PERFORM PASS                                  IF1064.2
090900     ELSE                                                         IF1064.2
091000                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
091100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
091200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
091300                    PERFORM FAIL.                                 IF1064.2
091400     GO TO F-COS-WRITE-28.                                        IF1064.2
091500 F-COS-DELETE-28.                                                 IF1064.2
091600     PERFORM  DE-LETE.                                            IF1064.2
091700     GO TO    F-COS-WRITE-28.                                     IF1064.2
091800 F-COS-WRITE-28.                                                  IF1064.2
091900     MOVE "F-COS-28" TO PAR-NAME.                                 IF1064.2
092000     PERFORM  PRINT-DETAIL.                                       IF1064.2
092100*****************TEST (u) - COMPLEX TEST****************          IF1064.2
092200 F-COS-29.                                                        IF1064.2
092300     MOVE ZERO TO WS-NUM.                                         IF1064.2
092400     MOVE -0.000040 TO MIN-RANGE.                                 IF1064.2
092500     MOVE  0.000040 TO MAX-RANGE.                                 IF1064.2
092600 F-COS-TEST-29.                                                   IF1064.2
092700     COMPUTE WS-NUM = FUNCTION COS(PI) + 1.                       IF1064.2
092800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
092900        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
093000                    PERFORM PASS                                  IF1064.2
093100     ELSE                                                         IF1064.2
093200                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
093300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
093400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
093500                    PERFORM FAIL.                                 IF1064.2
093600     GO TO F-COS-WRITE-29.                                        IF1064.2
093700 F-COS-DELETE-29.                                                 IF1064.2
093800     PERFORM  DE-LETE.                                            IF1064.2
093900     GO TO    F-COS-WRITE-29.                                     IF1064.2
094000 F-COS-WRITE-29.                                                  IF1064.2
094100     MOVE "F-COS-29" TO PAR-NAME.                                 IF1064.2
094200     PERFORM  PRINT-DETAIL.                                       IF1064.2
094300*****************TEST (v) - COMPLEX TEST****************          IF1064.2
094400 F-COS-30.                                                        IF1064.2
094500     MOVE ZERO TO WS-NUM.                                         IF1064.2
094600     MOVE 0.914616 TO MIN-RANGE.                                  IF1064.2
094700     MOVE 0.914690 TO MAX-RANGE.                                  IF1064.2
094800 F-COS-TEST-30.                                                   IF1064.2
094900     COMPUTE WS-NUM = FUNCTION COS(FUNCTION COS(2)).              IF1064.2
095000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
095100        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
095200                    PERFORM PASS                                  IF1064.2
095300     ELSE                                                         IF1064.2
095400                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
095500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
095600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
095700                    PERFORM FAIL.                                 IF1064.2
095800     GO TO F-COS-WRITE-30.                                        IF1064.2
095900 F-COS-DELETE-30.                                                 IF1064.2
096000     PERFORM  DE-LETE.                                            IF1064.2
096100     GO TO    F-COS-WRITE-30.                                     IF1064.2
096200 F-COS-WRITE-30.                                                  IF1064.2
096300     MOVE "F-COS-30" TO PAR-NAME.                                 IF1064.2
096400     PERFORM  PRINT-DETAIL.                                       IF1064.2
096500*****************TEST (w) - COMPLEX TEST****************          IF1064.2
096600 F-COS-31.                                                        IF1064.2
096700     MOVE ZERO TO WS-NUM.                                         IF1064.2
096800     MOVE -2.00008 TO MIN-RANGE.                                  IF1064.2
096900     MOVE -1.99992 TO MAX-RANGE.                                  IF1064.2
097000 F-COS-TEST-31.                                                   IF1064.2
097100     COMPUTE WS-NUM = FUNCTION COS(PI) +                          IF1064.2
097200                              FUNCTION COS(PI).                   IF1064.2
097300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1064.2
097400        (WS-NUM <= MAX-RANGE) THEN                                IF1064.2
097500                    PERFORM PASS                                  IF1064.2
097600     ELSE                                                         IF1064.2
097700                    MOVE WS-NUM TO COMPUTED-N                     IF1064.2
097800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1064.2
097900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1064.2
098000                    PERFORM FAIL.                                 IF1064.2
098100     GO TO F-COS-WRITE-31.                                        IF1064.2
098200 F-COS-DELETE-31.                                                 IF1064.2
098300     PERFORM  DE-LETE.                                            IF1064.2
098400     GO TO    F-COS-WRITE-31.                                     IF1064.2
098500 F-COS-WRITE-31.                                                  IF1064.2
098600     MOVE "F-COS-31" TO PAR-NAME.                                 IF1064.2
098700     PERFORM  PRINT-DETAIL.                                       IF1064.2
098800*****************SPECIAL PERFORM TEST**********************       IF1064.2
098900 F-COS-32.                                                        IF1064.2
099000     PERFORM F-COS-TEST-32                                        IF1064.2
099100       UNTIL FUNCTION COS(ARG1) < 0.                              IF1064.2
099200     PERFORM PASS.                                                IF1064.2
099300     GO TO F-COS-WRITE-32.                                        IF1064.2
099400 F-COS-TEST-32.                                                   IF1064.2
099500     COMPUTE ARG1 = ARG1 - 0.25.                                  IF1064.2
099600 F-COS-DELETE-32.                                                 IF1064.2
099700     PERFORM  DE-LETE.                                            IF1064.2
099800     GO TO    F-COS-WRITE-32.                                     IF1064.2
099900 F-COS-WRITE-32.                                                  IF1064.2
100000     MOVE "F-COS-32" TO PAR-NAME.                                 IF1064.2
100100     PERFORM  PRINT-DETAIL.                                       IF1064.2
100200********************END OF TESTS***************                   IF1064.2
100300 CCVS-EXIT SECTION.                                               IF1064.2
100400 CCVS-999999.                                                     IF1064.2
100500     GO TO CLOSE-FILES.                                           IF1064.2
      *END-OF,IF106A                                                                  
