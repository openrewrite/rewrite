      *HEADER,COBOL,IF131A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1314.2
000200 PROGRAM-ID.                                                      IF1314.2
000300     IF131A.                                                      IF1314.2
000400                                                                  IF1314.2
000500***********************************************************       IF1314.2
000600*                                                         *       IF1314.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1314.2
000800* It contains tests for the Intrinsic Function            *       IF1314.2
000900* RANDOM.                                                 *       IF1314.2
001000*                                                         *       IF1314.2
001100***********************************************************       IF1314.2
001200 ENVIRONMENT DIVISION.                                            IF1314.2
001300 CONFIGURATION SECTION.                                           IF1314.2
001400 SOURCE-COMPUTER.                                                 IF1314.2
001500     XXXXX082.                                                    IF1314.2
001600 OBJECT-COMPUTER.                                                 IF1314.2
001700     XXXXX083.                                                    IF1314.2
001800 INPUT-OUTPUT SECTION.                                            IF1314.2
001900 FILE-CONTROL.                                                    IF1314.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1314.2
002100     XXXXX055.                                                    IF1314.2
002200 DATA DIVISION.                                                   IF1314.2
002300 FILE SECTION.                                                    IF1314.2
002400 FD  PRINT-FILE.                                                  IF1314.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1314.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1314.2
002700 WORKING-STORAGE SECTION.                                         IF1314.2
002800***********************************************************       IF1314.2
002900* Variables specific to the Intrinsic Function Test IF131A*       IF1314.2
003000***********************************************************       IF1314.2
003100 01  A                   PIC S9(10)          VALUE 4.             IF1314.2
003200 01  Q                   PIC S9(10)          VALUE 3.             IF1314.2
003300 01  ARR                                     VALUE "40537".       IF1314.2
003400     02  IND OCCURS 5 TIMES PIC 9.                                IF1314.2
003500 01  TEMP                PIC S9(8)V9(8).                          IF1314.2
003600 01  WS-NUM              PIC S9(5)V9(6).                          IF1314.2
003700 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1314.2
003800 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1314.2
003900*                                                                 IF1314.2
004000**********************************************************        IF1314.2
004100*                                                                 IF1314.2
004200 01  TEST-RESULTS.                                                IF1314.2
004300     02 FILLER                   PIC X      VALUE SPACE.          IF1314.2
004400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1314.2
004500     02 FILLER                   PIC X      VALUE SPACE.          IF1314.2
004600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1314.2
004700     02 FILLER                   PIC X      VALUE SPACE.          IF1314.2
004800     02  PAR-NAME.                                                IF1314.2
004900       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1314.2
005000       03  PARDOT-X              PIC X      VALUE SPACE.          IF1314.2
005100       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1314.2
005200     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1314.2
005300     02 RE-MARK                  PIC X(61).                       IF1314.2
005400 01  TEST-COMPUTED.                                               IF1314.2
005500     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1314.2
005600     02 FILLER                   PIC X(17)  VALUE                 IF1314.2
005700            "       COMPUTED=".                                   IF1314.2
005800     02 COMPUTED-X.                                               IF1314.2
005900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1314.2
006000     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1314.2
006100                                 PIC -9(9).9(9).                  IF1314.2
006200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1314.2
006300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1314.2
006400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1314.2
006500     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1314.2
006600         04 COMPUTED-18V0                    PIC -9(18).          IF1314.2
006700         04 FILLER                           PIC X.               IF1314.2
006800     03 FILLER PIC X(50) VALUE SPACE.                             IF1314.2
006900 01  TEST-CORRECT.                                                IF1314.2
007000     02 FILLER PIC X(30) VALUE SPACE.                             IF1314.2
007100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1314.2
007200     02 CORRECT-X.                                                IF1314.2
007300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1314.2
007400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1314.2
007500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1314.2
007600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1314.2
007700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1314.2
007800     03      CR-18V0 REDEFINES CORRECT-A.                         IF1314.2
007900         04 CORRECT-18V0                     PIC -9(18).          IF1314.2
008000         04 FILLER                           PIC X.               IF1314.2
008100     03 FILLER PIC X(2) VALUE SPACE.                              IF1314.2
008200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1314.2
008300 01  TEST-CORRECT-MIN.                                            IF1314.2
008400     02 FILLER PIC X(30) VALUE SPACE.                             IF1314.2
008500     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1314.2
008600     02 CORRECTMI-X.                                              IF1314.2
008700     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1314.2
008800     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1314.2
008900     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1314.2
009000     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1314.2
009100     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1314.2
009200     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1314.2
009300         04 CORRECTMI-18V0                     PIC -9(18).        IF1314.2
009400         04 FILLER                           PIC X.               IF1314.2
009500     03 FILLER PIC X(2) VALUE SPACE.                              IF1314.2
009600     03 FILLER                           PIC X(48) VALUE SPACE.   IF1314.2
009700 01  TEST-CORRECT-MAX.                                            IF1314.2
009800     02 FILLER PIC X(30) VALUE SPACE.                             IF1314.2
009900     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1314.2
010000     02 CORRECTMA-X.                                              IF1314.2
010100     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1314.2
010200     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1314.2
010300     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1314.2
010400     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1314.2
010500     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1314.2
010600     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1314.2
010700         04 CORRECTMA-18V0                     PIC -9(18).        IF1314.2
010800         04 FILLER                           PIC X.               IF1314.2
010900     03 FILLER PIC X(2) VALUE SPACE.                              IF1314.2
011000     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1314.2
011100 01  CCVS-C-1.                                                    IF1314.2
011200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1314.2
011300-    "SS  PARAGRAPH-NAME                                          IF1314.2
011400-    "       REMARKS".                                            IF1314.2
011500     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1314.2
011600 01  CCVS-C-2.                                                    IF1314.2
011700     02 FILLER                     PIC X        VALUE SPACE.      IF1314.2
011800     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1314.2
011900     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1314.2
012000     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1314.2
012100     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1314.2
012200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1314.2
012300 01  REC-CT                        PIC 99       VALUE ZERO.       IF1314.2
012400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1314.2
012500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1314.2
012600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1314.2
012700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1314.2
012800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1314.2
012900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1314.2
013000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1314.2
013100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1314.2
013200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1314.2
013300 01  CCVS-H-1.                                                    IF1314.2
013400     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1314.2
013500     02  FILLER                    PIC X(42)    VALUE             IF1314.2
013600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1314.2
013700     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1314.2
013800 01  CCVS-H-2A.                                                   IF1314.2
013900   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1314.2
014000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1314.2
014100   02  FILLER                        PIC XXXX   VALUE             IF1314.2
014200     "4.2 ".                                                      IF1314.2
014300   02  FILLER                        PIC X(28)  VALUE             IF1314.2
014400            " COPY - NOT FOR DISTRIBUTION".                       IF1314.2
014500   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1314.2
014600                                                                  IF1314.2
014700 01  CCVS-H-2B.                                                   IF1314.2
014800   02  FILLER                        PIC X(15)  VALUE             IF1314.2
014900            "TEST RESULT OF ".                                    IF1314.2
015000   02  TEST-ID                       PIC X(9).                    IF1314.2
015100   02  FILLER                        PIC X(4)   VALUE             IF1314.2
015200            " IN ".                                               IF1314.2
015300   02  FILLER                        PIC X(12)  VALUE             IF1314.2
015400     " HIGH       ".                                              IF1314.2
015500   02  FILLER                        PIC X(22)  VALUE             IF1314.2
015600            " LEVEL VALIDATION FOR ".                             IF1314.2
015700   02  FILLER                        PIC X(58)  VALUE             IF1314.2
015800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1314.2
015900 01  CCVS-H-3.                                                    IF1314.2
016000     02  FILLER                      PIC X(34)  VALUE             IF1314.2
016100            " FOR OFFICIAL USE ONLY    ".                         IF1314.2
016200     02  FILLER                      PIC X(58)  VALUE             IF1314.2
016300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1314.2
016400     02  FILLER                      PIC X(28)  VALUE             IF1314.2
016500            "  COPYRIGHT   1985 ".                                IF1314.2
016600 01  CCVS-E-1.                                                    IF1314.2
016700     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1314.2
016800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1314.2
016900     02 ID-AGAIN                     PIC X(9).                    IF1314.2
017000     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1314.2
017100 01  CCVS-E-2.                                                    IF1314.2
017200     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1314.2
017300     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1314.2
017400     02 CCVS-E-2-2.                                               IF1314.2
017500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1314.2
017600         03 FILLER                   PIC X      VALUE SPACE.      IF1314.2
017700         03 ENDER-DESC               PIC X(44)  VALUE             IF1314.2
017800            "ERRORS ENCOUNTERED".                                 IF1314.2
017900 01  CCVS-E-3.                                                    IF1314.2
018000     02  FILLER                      PIC X(22)  VALUE             IF1314.2
018100            " FOR OFFICIAL USE ONLY".                             IF1314.2
018200     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1314.2
018300     02  FILLER                      PIC X(58)  VALUE             IF1314.2
018400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1314.2
018500     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1314.2
018600     02 FILLER                       PIC X(15)  VALUE             IF1314.2
018700             " COPYRIGHT 1985".                                   IF1314.2
018800 01  CCVS-E-4.                                                    IF1314.2
018900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1314.2
019000     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1314.2
019100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1314.2
019200     02 FILLER                       PIC X(40)  VALUE             IF1314.2
019300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1314.2
019400 01  XXINFO.                                                      IF1314.2
019500     02 FILLER                       PIC X(19)  VALUE             IF1314.2
019600            "*** INFORMATION ***".                                IF1314.2
019700     02 INFO-TEXT.                                                IF1314.2
019800       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1314.2
019900       04 XXCOMPUTED                 PIC X(20).                   IF1314.2
020000       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1314.2
020100       04 XXCORRECT                  PIC X(20).                   IF1314.2
020200     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1314.2
020300 01  HYPHEN-LINE.                                                 IF1314.2
020400     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1314.2
020500     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1314.2
020600-    "*****************************************".                 IF1314.2
020700     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1314.2
020800-    "******************************".                            IF1314.2
020900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1314.2
021000     "IF131A".                                                    IF1314.2
021100 PROCEDURE DIVISION.                                              IF1314.2
021200 CCVS1 SECTION.                                                   IF1314.2
021300 OPEN-FILES.                                                      IF1314.2
021400     OPEN     OUTPUT PRINT-FILE.                                  IF1314.2
021500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1314.2
021600     MOVE    SPACE TO TEST-RESULTS.                               IF1314.2
021700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1314.2
021800     GO TO CCVS1-EXIT.                                            IF1314.2
021900 CLOSE-FILES.                                                     IF1314.2
022000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1314.2
022100 TERMINATE-CCVS.                                                  IF1314.2
022200     STOP     RUN.                                                IF1314.2
022300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1314.2
022400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1314.2
022500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1314.2
022600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1314.2
022700     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1314.2
022800 PRINT-DETAIL.                                                    IF1314.2
022900     IF REC-CT NOT EQUAL TO ZERO                                  IF1314.2
023000             MOVE "." TO PARDOT-X                                 IF1314.2
023100             MOVE REC-CT TO DOTVALUE.                             IF1314.2
023200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1314.2
023300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1314.2
023400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1314.2
023500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1314.2
023600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1314.2
023700     MOVE SPACE TO CORRECT-X.                                     IF1314.2
023800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1314.2
023900     MOVE     SPACE TO RE-MARK.                                   IF1314.2
024000 HEAD-ROUTINE.                                                    IF1314.2
024100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1314.2
024200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1314.2
024300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1314.2
024400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1314.2
024500 COLUMN-NAMES-ROUTINE.                                            IF1314.2
024600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1314.2
024700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1314.2
024800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1314.2
024900 END-ROUTINE.                                                     IF1314.2
025000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1314.2
025100 END-RTN-EXIT.                                                    IF1314.2
025200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1314.2
025300 END-ROUTINE-1.                                                   IF1314.2
025400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1314.2
025500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1314.2
025600      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1314.2
025700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1314.2
025800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1314.2
025900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1314.2
026000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1314.2
026100  END-ROUTINE-12.                                                 IF1314.2
026200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1314.2
026300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1314.2
026400         MOVE "NO " TO ERROR-TOTAL                                IF1314.2
026500         ELSE                                                     IF1314.2
026600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1314.2
026700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1314.2
026800     PERFORM WRITE-LINE.                                          IF1314.2
026900 END-ROUTINE-13.                                                  IF1314.2
027000     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1314.2
027100         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1314.2
027200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1314.2
027300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1314.2
027400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1314.2
027500      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1314.2
027600          MOVE "NO " TO ERROR-TOTAL                               IF1314.2
027700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1314.2
027800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1314.2
027900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1314.2
028000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1314.2
028100 WRITE-LINE.                                                      IF1314.2
028200     ADD 1 TO RECORD-COUNT.                                       IF1314.2
028300*    IF RECORD-COUNT GREATER 42                                   IF1314.2
028400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1314.2
028500*        MOVE SPACE TO DUMMY-RECORD                               IF1314.2
028600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1314.2
028700*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1314.2
028800*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1314.2
028900*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1314.2
029000*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1314.2
029100*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1314.2
029200*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1314.2
029300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1314.2
029400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1314.2
029500*        MOVE ZERO TO RECORD-COUNT.                               IF1314.2
029600     PERFORM WRT-LN.                                              IF1314.2
029700 WRT-LN.                                                          IF1314.2
029800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1314.2
029900     MOVE SPACE TO DUMMY-RECORD.                                  IF1314.2
030000 BLANK-LINE-PRINT.                                                IF1314.2
030100     PERFORM WRT-LN.                                              IF1314.2
030200 FAIL-ROUTINE.                                                    IF1314.2
030300     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1314.2
030400            GO TO FAIL-ROUTINE-WRITE.                             IF1314.2
030500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1314.2
030600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1314.2
030700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1314.2
030800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1314.2
030900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1314.2
031000     GO TO  FAIL-ROUTINE-EX.                                      IF1314.2
031100 FAIL-ROUTINE-WRITE.                                              IF1314.2
031200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1314.2
031300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1314.2
031400                              CORMA-ANSI-REFERENCE.               IF1314.2
031500     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1314.2
031600           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1314.2
031700           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1314.2
031800     ELSE                                                         IF1314.2
031900           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1314.2
032000     PERFORM WRITE-LINE.                                          IF1314.2
032100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1314.2
032200 FAIL-ROUTINE-EX. EXIT.                                           IF1314.2
032300 BAIL-OUT.                                                        IF1314.2
032400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1314.2
032500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1314.2
032600 BAIL-OUT-WRITE.                                                  IF1314.2
032700     MOVE CORRECT-A TO XXCORRECT.                                 IF1314.2
032800     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1314.2
032900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1314.2
033000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1314.2
033100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1314.2
033200 BAIL-OUT-EX. EXIT.                                               IF1314.2
033300 CCVS1-EXIT.                                                      IF1314.2
033400     EXIT.                                                        IF1314.2
033500********************************************************          IF1314.2
033600*                                                      *          IF1314.2
033700*    Intrinsic Function Tests         IF131A - RANDOM  *          IF1314.2
033800*                                                      *          IF1314.2
033900********************************************************          IF1314.2
034000 SECT-IF131A SECTION.                                             IF1314.2
034100 F-RANDOM-INFO.                                                   IF1314.2
034200     MOVE     "See ref. A-64 2.35" TO ANSI-REFERENCE.             IF1314.2
034300     MOVE     "RANDOM Function" TO FEATURE.                       IF1314.2
034400*****************TEST (a) ******************************          IF1314.2
034500 F-RANDOM-01.                                                     IF1314.2
034600     MOVE ZERO TO WS-NUM.                                         IF1314.2
034700     MOVE 0 TO MIN-RANGE.                                         IF1314.2
034800     MOVE 1 TO MAX-RANGE.                                         IF1314.2
034900 F-RANDOM-TEST-01.                                                IF1314.2
035000     COMPUTE WS-NUM = FUNCTION RANDOM.                            IF1314.2
035100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1314.2
035200        (WS-NUM < MAX-RANGE) THEN                                 IF1314.2
035300                    PERFORM PASS                                  IF1314.2
035400     ELSE                                                         IF1314.2
035500                    MOVE WS-NUM TO COMPUTED-N                     IF1314.2
035600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1314.2
035700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1314.2
035800                    PERFORM FAIL.                                 IF1314.2
035900     GO TO F-RANDOM-WRITE-01.                                     IF1314.2
036000 F-RANDOM-DELETE-01.                                              IF1314.2
036100     PERFORM  DE-LETE.                                            IF1314.2
036200     GO TO    F-RANDOM-WRITE-01.                                  IF1314.2
036300 F-RANDOM-WRITE-01.                                               IF1314.2
036400     MOVE "F-RANDOM-01" TO PAR-NAME.                              IF1314.2
036500     PERFORM  PRINT-DETAIL.                                       IF1314.2
036600*****************TEST (b) ******************************          IF1314.2
036700 F-RANDOM-02.                                                     IF1314.2
036800     EVALUATE FUNCTION RANDOM(3)                                  IF1314.2
036900     WHEN 0 THRU 1                                                IF1314.2
037000                    PERFORM PASS                                  IF1314.2
037100     WHEN OTHER                                                   IF1314.2
037200                    PERFORM FAIL.                                 IF1314.2
037300     GO TO F-RANDOM-WRITE-02.                                     IF1314.2
037400 F-RANDOM-DELETE-02.                                              IF1314.2
037500     PERFORM  DE-LETE.                                            IF1314.2
037600     GO TO    F-RANDOM-WRITE-02.                                  IF1314.2
037700 F-RANDOM-WRITE-02.                                               IF1314.2
037800     MOVE "F-RANDOM-02" TO PAR-NAME.                              IF1314.2
037900     PERFORM  PRINT-DETAIL.                                       IF1314.2
038000*****************TEST (c) ******************************          IF1314.2
038100 F-RANDOM-03.                                                     IF1314.2
038200     MOVE 0 TO MIN-RANGE.                                         IF1314.2
038300     MOVE 1 TO MAX-RANGE.                                         IF1314.2
038400 F-RANDOM-TEST-03.                                                IF1314.2
038500     IF (FUNCTION RANDOM(Q) >= MIN-RANGE) AND                     IF1314.2
038600        (FUNCTION RANDOM(Q) <  MAX-RANGE) THEN                    IF1314.2
038700                    PERFORM PASS                                  IF1314.2
038800     ELSE                                                         IF1314.2
038900                    PERFORM FAIL.                                 IF1314.2
039000     GO TO F-RANDOM-WRITE-03.                                     IF1314.2
039100 F-RANDOM-DELETE-03.                                              IF1314.2
039200     PERFORM  DE-LETE.                                            IF1314.2
039300     GO TO    F-RANDOM-WRITE-03.                                  IF1314.2
039400 F-RANDOM-WRITE-03.                                               IF1314.2
039500     MOVE "F-RANDOM-03" TO PAR-NAME.                              IF1314.2
039600     PERFORM  PRINT-DETAIL.                                       IF1314.2
039700*****************TEST (d) ******************************          IF1314.2
039800 F-RANDOM-04.                                                     IF1314.2
039900     MOVE ZERO TO WS-NUM.                                         IF1314.2
040000     MOVE 0 TO MIN-RANGE.                                         IF1314.2
040100     MOVE 1 TO MAX-RANGE.                                         IF1314.2
040200 F-RANDOM-TEST-04.                                                IF1314.2
040300     COMPUTE WS-NUM = FUNCTION RANDOM(IND(4)).                    IF1314.2
040400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1314.2
040500        (WS-NUM < MAX-RANGE) THEN                                 IF1314.2
040600                    PERFORM PASS                                  IF1314.2
040700     ELSE                                                         IF1314.2
040800                    MOVE WS-NUM TO COMPUTED-N                     IF1314.2
040900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1314.2
041000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1314.2
041100                    PERFORM FAIL.                                 IF1314.2
041200     GO TO F-RANDOM-WRITE-04.                                     IF1314.2
041300 F-RANDOM-DELETE-04.                                              IF1314.2
041400     PERFORM  DE-LETE.                                            IF1314.2
041500     GO TO    F-RANDOM-WRITE-04.                                  IF1314.2
041600 F-RANDOM-WRITE-04.                                               IF1314.2
041700     MOVE "F-RANDOM-04" TO PAR-NAME.                              IF1314.2
041800     PERFORM  PRINT-DETAIL.                                       IF1314.2
041900*****************TEST (e) ******************************          IF1314.2
042000 F-RANDOM-05.                                                     IF1314.2
042100     MOVE ZERO TO WS-NUM.                                         IF1314.2
042200     MOVE 0 TO MIN-RANGE.                                         IF1314.2
042300     MOVE 1 TO MAX-RANGE.                                         IF1314.2
042400 F-RANDOM-TEST-05.                                                IF1314.2
042500     COMPUTE WS-NUM = FUNCTION RANDOM(IND(A)).                    IF1314.2
042600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1314.2
042700        (WS-NUM < MAX-RANGE) THEN                                 IF1314.2
042800                    PERFORM PASS                                  IF1314.2
042900     ELSE                                                         IF1314.2
043000                    MOVE WS-NUM TO COMPUTED-N                     IF1314.2
043100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1314.2
043200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1314.2
043300                    PERFORM FAIL.                                 IF1314.2
043400     GO TO F-RANDOM-WRITE-05.                                     IF1314.2
043500 F-RANDOM-DELETE-05.                                              IF1314.2
043600     PERFORM  DE-LETE.                                            IF1314.2
043700     GO TO    F-RANDOM-WRITE-05.                                  IF1314.2
043800 F-RANDOM-WRITE-05.                                               IF1314.2
043900     MOVE "F-RANDOM-05" TO PAR-NAME.                              IF1314.2
044000     PERFORM  PRINT-DETAIL.                                       IF1314.2
044100*****************TEST (f) ******************************          IF1314.2
044200 F-RANDOM-06.                                                     IF1314.2
044300     MOVE ZERO TO WS-NUM.                                         IF1314.2
044400     MOVE 1 TO MIN-RANGE.                                         IF1314.2
044500     MOVE 2 TO MAX-RANGE.                                         IF1314.2
044600 F-RANDOM-TEST-06.                                                IF1314.2
044700     COMPUTE WS-NUM = FUNCTION RANDOM(2) + 1.                     IF1314.2
044800     IF (WS-NUM >= MIN-RANGE) AND                                 IF1314.2
044900        (WS-NUM < MAX-RANGE) THEN                                 IF1314.2
045000                    PERFORM PASS                                  IF1314.2
045100     ELSE                                                         IF1314.2
045200                    MOVE WS-NUM TO COMPUTED-N                     IF1314.2
045300                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1314.2
045400                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1314.2
045500                    PERFORM FAIL.                                 IF1314.2
045600     GO TO F-RANDOM-WRITE-06.                                     IF1314.2
045700 F-RANDOM-DELETE-06.                                              IF1314.2
045800     PERFORM  DE-LETE.                                            IF1314.2
045900     GO TO    F-RANDOM-WRITE-06.                                  IF1314.2
046000 F-RANDOM-WRITE-06.                                               IF1314.2
046100     MOVE "F-RANDOM-06" TO PAR-NAME.                              IF1314.2
046200     PERFORM  PRINT-DETAIL.                                       IF1314.2
046300*****************TEST (g) ******************************          IF1314.2
046400 F-RANDOM-07.                                                     IF1314.2
046500     MOVE ZERO TO WS-NUM.                                         IF1314.2
046600     MOVE 0 TO MIN-RANGE.                                         IF1314.2
046700     MOVE 2 TO MAX-RANGE.                                         IF1314.2
046800 F-RANDOM-TEST-07.                                                IF1314.2
046900     COMPUTE WS-NUM = FUNCTION RANDOM(1) +                        IF1314.2
047000                      FUNCTION RANDOM(2).                         IF1314.2
047100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1314.2
047200        (WS-NUM < MAX-RANGE) THEN                                 IF1314.2
047300                    PERFORM PASS                                  IF1314.2
047400     ELSE                                                         IF1314.2
047500                    MOVE WS-NUM TO COMPUTED-N                     IF1314.2
047600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1314.2
047700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1314.2
047800                    PERFORM FAIL.                                 IF1314.2
047900     GO TO F-RANDOM-WRITE-07.                                     IF1314.2
048000 F-RANDOM-DELETE-07.                                              IF1314.2
048100     PERFORM  DE-LETE.                                            IF1314.2
048200     GO TO    F-RANDOM-WRITE-07.                                  IF1314.2
048300 F-RANDOM-WRITE-07.                                               IF1314.2
048400     MOVE "F-RANDOM-07" TO PAR-NAME.                              IF1314.2
048500     PERFORM  PRINT-DETAIL.                                       IF1314.2
048600*****************TEST (h) ******************************          IF1314.2
048700 F-RANDOM-08.                                                     IF1314.2
048800     MOVE ZERO TO WS-NUM.                                         IF1314.2
048900     MOVE 0 TO MIN-RANGE.                                         IF1314.2
049000     MOVE 1 TO MAX-RANGE.                                         IF1314.2
049100 F-RANDOM-TEST-08.                                                IF1314.2
049200     COMPUTE WS-NUM = FUNCTION RANDOM(                            IF1314.2
049300             FUNCTION INTEGER(100 * FUNCTION RANDOM(1))).         IF1314.2
049400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1314.2
049500        (WS-NUM < MAX-RANGE) THEN                                 IF1314.2
049600                    PERFORM PASS                                  IF1314.2
049700     ELSE                                                         IF1314.2
049800                    MOVE WS-NUM TO COMPUTED-N                     IF1314.2
049900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1314.2
050000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1314.2
050100                    PERFORM FAIL.                                 IF1314.2
050200     GO TO F-RANDOM-WRITE-08.                                     IF1314.2
050300 F-RANDOM-DELETE-08.                                              IF1314.2
050400     PERFORM  DE-LETE.                                            IF1314.2
050500     GO TO    F-RANDOM-WRITE-08.                                  IF1314.2
050600 F-RANDOM-WRITE-08.                                               IF1314.2
050700     MOVE "F-RANDOM-08" TO PAR-NAME.                              IF1314.2
050800     PERFORM  PRINT-DETAIL.                                       IF1314.2
050900********************END OF TESTS***************                   IF1314.2
051000 CCVS-EXIT SECTION.                                               IF1314.2
051100 CCVS-999999.                                                     IF1314.2
051200     GO TO CLOSE-FILES.                                           IF1314.2
      *END-OF,IF131A                                                                  
