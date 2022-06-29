      *HEADER,COBOL,IF127A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1274.2
000200 PROGRAM-ID.                                                      IF1274.2
000300     IF127A.                                                      IF1274.2
000400                                                                  IF1274.2
000500***********************************************************       IF1274.2
000600*                                                         *       IF1274.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1274.2
000800* It contains tests for the Intrinsic Function ORD.       *       IF1274.2
000900*                                                         *       IF1274.2
001000*                                                         *       IF1274.2
001100***********************************************************       IF1274.2
001200 ENVIRONMENT DIVISION.                                            IF1274.2
001300 CONFIGURATION SECTION.                                           IF1274.2
001400 SOURCE-COMPUTER.                                                 IF1274.2
001500     XXXXX082.                                                    IF1274.2
001600 OBJECT-COMPUTER.                                                 IF1274.2
001700     XXXXX083                                                     IF1274.2
001800     PROGRAM COLLATING SEQUENCE IS PRG-COLL-SEQ.                  IF1274.2
001900 SPECIAL-NAMES.                                                   IF1274.2
002000     ALPHABET PRG-COLL-SEQ IS                                     IF1274.2
002100     STANDARD-2.                                                  IF1274.2
002200 INPUT-OUTPUT SECTION.                                            IF1274.2
002300 FILE-CONTROL.                                                    IF1274.2
002400     SELECT PRINT-FILE ASSIGN TO                                  IF1274.2
002500     XXXXX055.                                                    IF1274.2
002600 DATA DIVISION.                                                   IF1274.2
002700 FILE SECTION.                                                    IF1274.2
002800 FD  PRINT-FILE.                                                  IF1274.2
002900 01  PRINT-REC PICTURE X(120).                                    IF1274.2
003000 01  DUMMY-RECORD PICTURE X(120).                                 IF1274.2
003100 WORKING-STORAGE SECTION.                                         IF1274.2
003200***********************************************************       IF1274.2
003300* Variables specific to the Intrinsic Function Test IF127A*       IF1274.2
003400***********************************************************       IF1274.2
003500 01  A                   PIC X          VALUE "F".                IF1274.2
003600 01  B                   PIC X          VALUE "d".                IF1274.2
003700 01  C                   PIC X          VALUE "3".                IF1274.2
003800 01  ARG1                PIC X          VALUE "A".                IF1274.2
003900 01  TEMP                PIC S9(10).                              IF1274.2
004000 01  WS-INT              PIC S9(10).                              IF1274.2
004100*                                                                 IF1274.2
004200**********************************************************        IF1274.2
004300*                                                                 IF1274.2
004400 01  TEST-RESULTS.                                                IF1274.2
004500     02 FILLER                   PIC X      VALUE SPACE.          IF1274.2
004600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1274.2
004700     02 FILLER                   PIC X      VALUE SPACE.          IF1274.2
004800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1274.2
004900     02 FILLER                   PIC X      VALUE SPACE.          IF1274.2
005000     02  PAR-NAME.                                                IF1274.2
005100       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1274.2
005200       03  PARDOT-X              PIC X      VALUE SPACE.          IF1274.2
005300       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1274.2
005400     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1274.2
005500     02 RE-MARK                  PIC X(61).                       IF1274.2
005600 01  TEST-COMPUTED.                                               IF1274.2
005700     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1274.2
005800     02 FILLER                   PIC X(17)  VALUE                 IF1274.2
005900            "       COMPUTED=".                                   IF1274.2
006000     02 COMPUTED-X.                                               IF1274.2
006100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1274.2
006200     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1274.2
006300                                 PIC -9(9).9(9).                  IF1274.2
006400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1274.2
006500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1274.2
006600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1274.2
006700     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1274.2
006800         04 COMPUTED-18V0                    PIC -9(18).          IF1274.2
006900         04 FILLER                           PIC X.               IF1274.2
007000     03 FILLER PIC X(50) VALUE SPACE.                             IF1274.2
007100 01  TEST-CORRECT.                                                IF1274.2
007200     02 FILLER PIC X(30) VALUE SPACE.                             IF1274.2
007300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1274.2
007400     02 CORRECT-X.                                                IF1274.2
007500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1274.2
007600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1274.2
007700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1274.2
007800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1274.2
007900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1274.2
008000     03      CR-18V0 REDEFINES CORRECT-A.                         IF1274.2
008100         04 CORRECT-18V0                     PIC -9(18).          IF1274.2
008200         04 FILLER                           PIC X.               IF1274.2
008300     03 FILLER PIC X(2) VALUE SPACE.                              IF1274.2
008400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1274.2
008500 01  TEST-CORRECT-MIN.                                            IF1274.2
008600     02 FILLER PIC X(30) VALUE SPACE.                             IF1274.2
008700     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1274.2
008800     02 CORRECTMI-X.                                              IF1274.2
008900     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1274.2
009000     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1274.2
009100     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1274.2
009200     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1274.2
009300     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1274.2
009400     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1274.2
009500         04 CORRECTMI-18V0                     PIC -9(18).        IF1274.2
009600         04 FILLER                           PIC X.               IF1274.2
009700     03 FILLER PIC X(2) VALUE SPACE.                              IF1274.2
009800     03 FILLER                           PIC X(48) VALUE SPACE.   IF1274.2
009900 01  TEST-CORRECT-MAX.                                            IF1274.2
010000     02 FILLER PIC X(30) VALUE SPACE.                             IF1274.2
010100     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1274.2
010200     02 CORRECTMA-X.                                              IF1274.2
010300     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1274.2
010400     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1274.2
010500     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1274.2
010600     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1274.2
010700     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1274.2
010800     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1274.2
010900         04 CORRECTMA-18V0                     PIC -9(18).        IF1274.2
011000         04 FILLER                           PIC X.               IF1274.2
011100     03 FILLER PIC X(2) VALUE SPACE.                              IF1274.2
011200     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1274.2
011300 01  CCVS-C-1.                                                    IF1274.2
011400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1274.2
011500-    "SS  PARAGRAPH-NAME                                          IF1274.2
011600-    "       REMARKS".                                            IF1274.2
011700     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1274.2
011800 01  CCVS-C-2.                                                    IF1274.2
011900     02 FILLER                     PIC X        VALUE SPACE.      IF1274.2
012000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1274.2
012100     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1274.2
012200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1274.2
012300     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1274.2
012400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1274.2
012500 01  REC-CT                        PIC 99       VALUE ZERO.       IF1274.2
012600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1274.2
012700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1274.2
012800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1274.2
012900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1274.2
013000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1274.2
013100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1274.2
013200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1274.2
013300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1274.2
013400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1274.2
013500 01  CCVS-H-1.                                                    IF1274.2
013600     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1274.2
013700     02  FILLER                    PIC X(42)    VALUE             IF1274.2
013800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1274.2
013900     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1274.2
014000 01  CCVS-H-2A.                                                   IF1274.2
014100   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1274.2
014200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1274.2
014300   02  FILLER                        PIC XXXX   VALUE             IF1274.2
014400     "4.2 ".                                                      IF1274.2
014500   02  FILLER                        PIC X(28)  VALUE             IF1274.2
014600            " COPY - NOT FOR DISTRIBUTION".                       IF1274.2
014700   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1274.2
014800                                                                  IF1274.2
014900 01  CCVS-H-2B.                                                   IF1274.2
015000   02  FILLER                        PIC X(15)  VALUE             IF1274.2
015100            "TEST RESULT OF ".                                    IF1274.2
015200   02  TEST-ID                       PIC X(9).                    IF1274.2
015300   02  FILLER                        PIC X(4)   VALUE             IF1274.2
015400            " IN ".                                               IF1274.2
015500   02  FILLER                        PIC X(12)  VALUE             IF1274.2
015600     " HIGH       ".                                              IF1274.2
015700   02  FILLER                        PIC X(22)  VALUE             IF1274.2
015800            " LEVEL VALIDATION FOR ".                             IF1274.2
015900   02  FILLER                        PIC X(58)  VALUE             IF1274.2
016000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1274.2
016100 01  CCVS-H-3.                                                    IF1274.2
016200     02  FILLER                      PIC X(34)  VALUE             IF1274.2
016300            " FOR OFFICIAL USE ONLY    ".                         IF1274.2
016400     02  FILLER                      PIC X(58)  VALUE             IF1274.2
016500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1274.2
016600     02  FILLER                      PIC X(28)  VALUE             IF1274.2
016700            "  COPYRIGHT   1985 ".                                IF1274.2
016800 01  CCVS-E-1.                                                    IF1274.2
016900     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1274.2
017000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1274.2
017100     02 ID-AGAIN                     PIC X(9).                    IF1274.2
017200     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1274.2
017300 01  CCVS-E-2.                                                    IF1274.2
017400     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1274.2
017500     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1274.2
017600     02 CCVS-E-2-2.                                               IF1274.2
017700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1274.2
017800         03 FILLER                   PIC X      VALUE SPACE.      IF1274.2
017900         03 ENDER-DESC               PIC X(44)  VALUE             IF1274.2
018000            "ERRORS ENCOUNTERED".                                 IF1274.2
018100 01  CCVS-E-3.                                                    IF1274.2
018200     02  FILLER                      PIC X(22)  VALUE             IF1274.2
018300            " FOR OFFICIAL USE ONLY".                             IF1274.2
018400     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1274.2
018500     02  FILLER                      PIC X(58)  VALUE             IF1274.2
018600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1274.2
018700     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1274.2
018800     02 FILLER                       PIC X(15)  VALUE             IF1274.2
018900             " COPYRIGHT 1985".                                   IF1274.2
019000 01  CCVS-E-4.                                                    IF1274.2
019100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1274.2
019200     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1274.2
019300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1274.2
019400     02 FILLER                       PIC X(40)  VALUE             IF1274.2
019500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1274.2
019600 01  XXINFO.                                                      IF1274.2
019700     02 FILLER                       PIC X(19)  VALUE             IF1274.2
019800            "*** INFORMATION ***".                                IF1274.2
019900     02 INFO-TEXT.                                                IF1274.2
020000       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1274.2
020100       04 XXCOMPUTED                 PIC X(20).                   IF1274.2
020200       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1274.2
020300       04 XXCORRECT                  PIC X(20).                   IF1274.2
020400     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1274.2
020500 01  HYPHEN-LINE.                                                 IF1274.2
020600     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1274.2
020700     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1274.2
020800-    "*****************************************".                 IF1274.2
020900     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1274.2
021000-    "******************************".                            IF1274.2
021100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1274.2
021200     "IF127A".                                                    IF1274.2
021300 PROCEDURE DIVISION.                                              IF1274.2
021400 CCVS1 SECTION.                                                   IF1274.2
021500 OPEN-FILES.                                                      IF1274.2
021600     OPEN     OUTPUT PRINT-FILE.                                  IF1274.2
021700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1274.2
021800     MOVE    SPACE TO TEST-RESULTS.                               IF1274.2
021900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1274.2
022000     GO TO CCVS1-EXIT.                                            IF1274.2
022100 CLOSE-FILES.                                                     IF1274.2
022200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1274.2
022300 TERMINATE-CCVS.                                                  IF1274.2
022400     STOP     RUN.                                                IF1274.2
022500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1274.2
022600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1274.2
022700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1274.2
022800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1274.2
022900     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1274.2
023000 PRINT-DETAIL.                                                    IF1274.2
023100     IF REC-CT NOT EQUAL TO ZERO                                  IF1274.2
023200             MOVE "." TO PARDOT-X                                 IF1274.2
023300             MOVE REC-CT TO DOTVALUE.                             IF1274.2
023400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1274.2
023500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1274.2
023600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1274.2
023700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1274.2
023800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1274.2
023900     MOVE SPACE TO CORRECT-X.                                     IF1274.2
024000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1274.2
024100     MOVE     SPACE TO RE-MARK.                                   IF1274.2
024200 HEAD-ROUTINE.                                                    IF1274.2
024300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1274.2
024400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1274.2
024500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1274.2
024600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1274.2
024700 COLUMN-NAMES-ROUTINE.                                            IF1274.2
024800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1274.2
024900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1274.2
025000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1274.2
025100 END-ROUTINE.                                                     IF1274.2
025200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1274.2
025300 END-RTN-EXIT.                                                    IF1274.2
025400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1274.2
025500 END-ROUTINE-1.                                                   IF1274.2
025600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1274.2
025700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1274.2
025800      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1274.2
025900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1274.2
026000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1274.2
026100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1274.2
026200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1274.2
026300  END-ROUTINE-12.                                                 IF1274.2
026400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1274.2
026500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1274.2
026600         MOVE "NO " TO ERROR-TOTAL                                IF1274.2
026700         ELSE                                                     IF1274.2
026800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1274.2
026900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1274.2
027000     PERFORM WRITE-LINE.                                          IF1274.2
027100 END-ROUTINE-13.                                                  IF1274.2
027200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1274.2
027300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1274.2
027400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1274.2
027500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1274.2
027600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1274.2
027700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1274.2
027800          MOVE "NO " TO ERROR-TOTAL                               IF1274.2
027900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1274.2
028000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1274.2
028100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1274.2
028200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1274.2
028300 WRITE-LINE.                                                      IF1274.2
028400     ADD 1 TO RECORD-COUNT.                                       IF1274.2
028500*    IF RECORD-COUNT GREATER 42                                   IF1274.2
028600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1274.2
028700*        MOVE SPACE TO DUMMY-RECORD                               IF1274.2
028800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1274.2
028900*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1274.2
029000*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1274.2
029100*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1274.2
029200*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1274.2
029300*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1274.2
029400*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1274.2
029500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1274.2
029600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1274.2
029700*        MOVE ZERO TO RECORD-COUNT.                               IF1274.2
029800     PERFORM WRT-LN.                                              IF1274.2
029900 WRT-LN.                                                          IF1274.2
030000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1274.2
030100     MOVE SPACE TO DUMMY-RECORD.                                  IF1274.2
030200 BLANK-LINE-PRINT.                                                IF1274.2
030300     PERFORM WRT-LN.                                              IF1274.2
030400 FAIL-ROUTINE.                                                    IF1274.2
030500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1274.2
030600            GO TO FAIL-ROUTINE-WRITE.                             IF1274.2
030700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1274.2
030800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1274.2
030900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1274.2
031000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1274.2
031100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1274.2
031200     GO TO  FAIL-ROUTINE-EX.                                      IF1274.2
031300 FAIL-ROUTINE-WRITE.                                              IF1274.2
031400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1274.2
031500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1274.2
031600                              CORMA-ANSI-REFERENCE.               IF1274.2
031700     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1274.2
031800           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1274.2
031900           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1274.2
032000     ELSE                                                         IF1274.2
032100           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1274.2
032200     PERFORM WRITE-LINE.                                          IF1274.2
032300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1274.2
032400 FAIL-ROUTINE-EX. EXIT.                                           IF1274.2
032500 BAIL-OUT.                                                        IF1274.2
032600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1274.2
032700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1274.2
032800 BAIL-OUT-WRITE.                                                  IF1274.2
032900     MOVE CORRECT-A TO XXCORRECT.                                 IF1274.2
033000     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1274.2
033100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1274.2
033200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1274.2
033300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1274.2
033400 BAIL-OUT-EX. EXIT.                                               IF1274.2
033500 CCVS1-EXIT.                                                      IF1274.2
033600     EXIT.                                                        IF1274.2
033700********************************************************          IF1274.2
033800*                                                      *          IF1274.2
033900*    Intrinsic Function Tests         IF127A - ORD     *          IF1274.2
034000*                                                      *          IF1274.2
034100********************************************************          IF1274.2
034200 SECT-IF127A SECTION.                                             IF1274.2
034300 F-ORD-INFO.                                                      IF1274.2
034400     MOVE     "See ref. A-60 2.31" TO ANSI-REFERENCE.             IF1274.2
034500     MOVE     "ORD Function"     TO FEATURE.                      IF1274.2
034600*****************TEST (a) ******************************          IF1274.2
034700 F-ORD-01.                                                        IF1274.2
034800     MOVE ZERO TO WS-INT.                                         IF1274.2
034900 F-ORD-TEST-01.                                                   IF1274.2
035000     COMPUTE WS-INT = FUNCTION ORD("A").                          IF1274.2
035100     IF WS-INT = 66 THEN                                          IF1274.2
035200                        PERFORM PASS                              IF1274.2
035300     ELSE                                                         IF1274.2
035400                        MOVE 66  TO CORRECT-N                     IF1274.2
035500                        MOVE WS-INT TO COMPUTED-N                 IF1274.2
035600                        PERFORM FAIL.                             IF1274.2
035700     GO TO F-ORD-WRITE-01.                                        IF1274.2
035800 F-ORD-DELETE-01.                                                 IF1274.2
035900     PERFORM  DE-LETE.                                            IF1274.2
036000     GO TO    F-ORD-WRITE-01.                                     IF1274.2
036100 F-ORD-WRITE-01.                                                  IF1274.2
036200     MOVE "F-ORD-01" TO PAR-NAME.                                 IF1274.2
036300     PERFORM  PRINT-DETAIL.                                       IF1274.2
036400*****************TEST (b) ******************************          IF1274.2
036500 F-ORD-TEST-02.                                                   IF1274.2
036600     EVALUATE FUNCTION ORD("m")                                   IF1274.2
036700     WHEN 110                                                     IF1274.2
036800                                 PERFORM PASS                     IF1274.2
036900                                 GO TO F-ORD-WRITE-02.            IF1274.2
037000     PERFORM FAIL.                                                IF1274.2
037100     GO TO F-ORD-WRITE-02.                                        IF1274.2
037200 F-ORD-DELETE-02.                                                 IF1274.2
037300     PERFORM  DE-LETE.                                            IF1274.2
037400     GO TO    F-ORD-WRITE-02.                                     IF1274.2
037500 F-ORD-WRITE-02.                                                  IF1274.2
037600     MOVE "F-ORD-02" TO PAR-NAME.                                 IF1274.2
037700     PERFORM  PRINT-DETAIL.                                       IF1274.2
037800*****************TEST (c) ******************************          IF1274.2
037900 F-ORD-03.                                                        IF1274.2
038000     MOVE ZERO TO WS-INT.                                         IF1274.2
038100 F-ORD-TEST-03.                                                   IF1274.2
038200     IF FUNCTION ORD("5") = 54 THEN                               IF1274.2
038300                        PERFORM PASS                              IF1274.2
038400     ELSE                                                         IF1274.2
038500                        PERFORM FAIL.                             IF1274.2
038600     GO TO F-ORD-WRITE-03.                                        IF1274.2
038700 F-ORD-DELETE-03.                                                 IF1274.2
038800     PERFORM  DE-LETE.                                            IF1274.2
038900     GO TO    F-ORD-WRITE-03.                                     IF1274.2
039000 F-ORD-WRITE-03.                                                  IF1274.2
039100     MOVE "F-ORD-03" TO PAR-NAME.                                 IF1274.2
039200     PERFORM  PRINT-DETAIL.                                       IF1274.2
039300*****************TEST (d) ******************************          IF1274.2
039400 F-ORD-04.                                                        IF1274.2
039500     MOVE ZERO TO WS-INT.                                         IF1274.2
039600 F-ORD-TEST-04.                                                   IF1274.2
039700     COMPUTE WS-INT = FUNCTION ORD(A).                            IF1274.2
039800     IF WS-INT = 71 THEN                                          IF1274.2
039900                        PERFORM PASS                              IF1274.2
040000     ELSE                                                         IF1274.2
040100                        MOVE 71  TO CORRECT-N                     IF1274.2
040200                        MOVE WS-INT TO COMPUTED-N                 IF1274.2
040300                        PERFORM FAIL.                             IF1274.2
040400     GO TO F-ORD-WRITE-04.                                        IF1274.2
040500 F-ORD-DELETE-04.                                                 IF1274.2
040600     PERFORM  DE-LETE.                                            IF1274.2
040700     GO TO    F-ORD-WRITE-04.                                     IF1274.2
040800 F-ORD-WRITE-04.                                                  IF1274.2
040900     MOVE "F-ORD-04" TO PAR-NAME.                                 IF1274.2
041000     PERFORM  PRINT-DETAIL.                                       IF1274.2
041100*****************TEST (e) ******************************          IF1274.2
041200 F-ORD-05.                                                        IF1274.2
041300     MOVE ZERO TO WS-INT.                                         IF1274.2
041400 F-ORD-TEST-05.                                                   IF1274.2
041500     COMPUTE WS-INT = FUNCTION ORD(B).                            IF1274.2
041600     IF WS-INT = 101 THEN                                         IF1274.2
041700                        PERFORM PASS                              IF1274.2
041800     ELSE                                                         IF1274.2
041900                        MOVE 101  TO CORRECT-N                    IF1274.2
042000                        MOVE WS-INT TO COMPUTED-N                 IF1274.2
042100                        PERFORM FAIL.                             IF1274.2
042200     GO TO F-ORD-WRITE-05.                                        IF1274.2
042300 F-ORD-DELETE-05.                                                 IF1274.2
042400     PERFORM  DE-LETE.                                            IF1274.2
042500     GO TO    F-ORD-WRITE-05.                                     IF1274.2
042600 F-ORD-WRITE-05.                                                  IF1274.2
042700     MOVE "F-ORD-05" TO PAR-NAME.                                 IF1274.2
042800     PERFORM  PRINT-DETAIL.                                       IF1274.2
042900*****************TEST (f) ******************************          IF1274.2
043000 F-ORD-06.                                                        IF1274.2
043100     MOVE ZERO TO WS-INT.                                         IF1274.2
043200 F-ORD-TEST-06.                                                   IF1274.2
043300     COMPUTE WS-INT = FUNCTION ORD(C).                            IF1274.2
043400     IF WS-INT = 52 THEN                                          IF1274.2
043500                        PERFORM PASS                              IF1274.2
043600     ELSE                                                         IF1274.2
043700                        MOVE 52  TO CORRECT-N                     IF1274.2
043800                        MOVE WS-INT TO COMPUTED-N                 IF1274.2
043900                        PERFORM FAIL.                             IF1274.2
044000     GO TO F-ORD-WRITE-06.                                        IF1274.2
044100 F-ORD-DELETE-06.                                                 IF1274.2
044200     PERFORM  DE-LETE.                                            IF1274.2
044300     GO TO    F-ORD-WRITE-06.                                     IF1274.2
044400 F-ORD-WRITE-06.                                                  IF1274.2
044500     MOVE "F-ORD-06" TO PAR-NAME.                                 IF1274.2
044600     PERFORM  PRINT-DETAIL.                                       IF1274.2
044700*****************TEST (g) ******************************          IF1274.2
044800 F-ORD-07.                                                        IF1274.2
044900     MOVE ZERO TO WS-INT.                                         IF1274.2
045000 F-ORD-TEST-07.                                                   IF1274.2
045100     COMPUTE WS-INT = FUNCTION ORD("g") + 1.                      IF1274.2
045200     IF WS-INT = 105 THEN                                         IF1274.2
045300                        PERFORM PASS                              IF1274.2
045400     ELSE                                                         IF1274.2
045500                        MOVE 105  TO CORRECT-N                    IF1274.2
045600                        MOVE WS-INT TO COMPUTED-N                 IF1274.2
045700                        PERFORM FAIL.                             IF1274.2
045800     GO TO F-ORD-WRITE-07.                                        IF1274.2
045900 F-ORD-DELETE-07.                                                 IF1274.2
046000     PERFORM  DE-LETE.                                            IF1274.2
046100     GO TO    F-ORD-WRITE-07.                                     IF1274.2
046200 F-ORD-WRITE-07.                                                  IF1274.2
046300     MOVE "F-ORD-07" TO PAR-NAME.                                 IF1274.2
046400     PERFORM  PRINT-DETAIL.                                       IF1274.2
046500*****************TEST (h) ******************************          IF1274.2
046600 F-ORD-08.                                                        IF1274.2
046700     MOVE ZERO TO WS-INT.                                         IF1274.2
046800 F-ORD-TEST-08.                                                   IF1274.2
046900     COMPUTE WS-INT = FUNCTION ORD("A") +                         IF1274.2
047000             FUNCTION ORD(A).                                     IF1274.2
047100     IF WS-INT = 137 THEN                                         IF1274.2
047200                        PERFORM PASS                              IF1274.2
047300     ELSE                                                         IF1274.2
047400                        MOVE 137  TO CORRECT-N                    IF1274.2
047500                        MOVE WS-INT TO COMPUTED-N                 IF1274.2
047600                        PERFORM FAIL.                             IF1274.2
047700     GO TO F-ORD-WRITE-08.                                        IF1274.2
047800 F-ORD-DELETE-08.                                                 IF1274.2
047900     PERFORM  DE-LETE.                                            IF1274.2
048000     GO TO    F-ORD-WRITE-08.                                     IF1274.2
048100 F-ORD-WRITE-08.                                                  IF1274.2
048200     MOVE "F-ORD-08" TO PAR-NAME.                                 IF1274.2
048300     PERFORM  PRINT-DETAIL.                                       IF1274.2
048400*****************SPECIAL TEST 1******************************     IF1274.2
048500 F-ORD-09.                                                        IF1274.2
048600     PERFORM F-ORD-TEST-09 UNTIL FUNCTION ORD(ARG1) = 67.         IF1274.2
048700     IF ARG1 = "B" THEN                                           IF1274.2
048800                        PERFORM PASS                              IF1274.2
048900     ELSE                                                         IF1274.2
049000                        PERFORM FAIL.                             IF1274.2
049100     GO TO F-ORD-WRITE-09.                                        IF1274.2
049200*                                                                 IF1274.2
049300 F-ORD-TEST-09.                                                   IF1274.2
049400     MOVE "B" TO ARG1.                                            IF1274.2
049500*                                                                 IF1274.2
049600 F-ORD-DELETE-09.                                                 IF1274.2
049700     PERFORM  DE-LETE.                                            IF1274.2
049800     GO TO    F-ORD-WRITE-09.                                     IF1274.2
049900 F-ORD-WRITE-09.                                                  IF1274.2
050000     MOVE "F-ORD-09" TO PAR-NAME.                                 IF1274.2
050100     PERFORM  PRINT-DETAIL.                                       IF1274.2
050200*******************END OF TESTS**************************         IF1274.2
050300 CCVS-EXIT SECTION.                                               IF1274.2
050400 CCVS-999999.                                                     IF1274.2
050500     GO TO CLOSE-FILES.                                           IF1274.2
      *END-OF,IF127A                                                                  
