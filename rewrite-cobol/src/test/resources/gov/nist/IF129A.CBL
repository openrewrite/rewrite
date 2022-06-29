      *HEADER,COBOL,IF129A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1294.2
000200 PROGRAM-ID.                                                      IF1294.2
000300     IF129A.                                                      IF1294.2
000400                                                                  IF1294.2
000500***********************************************************       IF1294.2
000600*                                                         *       IF1294.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1294.2
000800* It contains tests for the Intrinsic Function ORD-MIN.   *       IF1294.2
000900*                                                         *       IF1294.2
001000*                                                         *       IF1294.2
001100***********************************************************       IF1294.2
001200 ENVIRONMENT DIVISION.                                            IF1294.2
001300 CONFIGURATION SECTION.                                           IF1294.2
001400 SOURCE-COMPUTER.                                                 IF1294.2
001500     XXXXX082.                                                    IF1294.2
001600 OBJECT-COMPUTER.                                                 IF1294.2
001700     XXXXX083                                                     IF1294.2
001800     PROGRAM COLLATING SEQUENCE IS PRG-COLL-SEQ.                  IF1294.2
001900 SPECIAL-NAMES.                                                   IF1294.2
002000     ALPHABET PRG-COLL-SEQ IS                                     IF1294.2
002100     STANDARD-2.                                                  IF1294.2
002200 INPUT-OUTPUT SECTION.                                            IF1294.2
002300 FILE-CONTROL.                                                    IF1294.2
002400     SELECT PRINT-FILE ASSIGN TO                                  IF1294.2
002500     XXXXX055.                                                    IF1294.2
002600 DATA DIVISION.                                                   IF1294.2
002700 FILE SECTION.                                                    IF1294.2
002800 FD  PRINT-FILE.                                                  IF1294.2
002900 01  PRINT-REC PICTURE X(120).                                    IF1294.2
003000 01  DUMMY-RECORD PICTURE X(120).                                 IF1294.2
003100 WORKING-STORAGE SECTION.                                         IF1294.2
003200***********************************************************       IF1294.2
003300* Variables specific to the Intrinsic Function Test IF129A*       IF1294.2
003400***********************************************************       IF1294.2
003500 01  A                   PIC S9(10)     VALUE 5.                  IF1294.2
003600 01  B                   PIC S9(10)     VALUE 7.                  IF1294.2
003700 01  C                   PIC S9(10)     VALUE 4.                  IF1294.2
003800 01  D                   PIC S9(10)     VALUE 10.                 IF1294.2
003900 01  I                   PIC X(4)       VALUE "R".                IF1294.2
004000 01  J                   PIC X(4)       VALUE "U".                IF1294.2
004100 01  P                   PIC S9(10)     VALUE 1.                  IF1294.2
004200 01  Q                   PIC S9(10)     VALUE 3.                  IF1294.2
004300 01  R                   PIC S9(10)     VALUE 5.                  IF1294.2
004400 01  ARG1                PIC S9(10)     VALUE 10.                 IF1294.2
004500 01  ARR                                VALUE "40537".            IF1294.2
004600     02 IND OCCURS 5 TIMES   PIC 9.                               IF1294.2
004700 01  TEMP                PIC S9(10).                              IF1294.2
004800 01  WS-INT              PIC S9(10).                              IF1294.2
004900*                                                                 IF1294.2
005000**********************************************************        IF1294.2
005100*                                                                 IF1294.2
005200 01  TEST-RESULTS.                                                IF1294.2
005300     02 FILLER                   PIC X      VALUE SPACE.          IF1294.2
005400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1294.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IF1294.2
005600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1294.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IF1294.2
005800     02  PAR-NAME.                                                IF1294.2
005900       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1294.2
006000       03  PARDOT-X              PIC X      VALUE SPACE.          IF1294.2
006100       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1294.2
006200     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1294.2
006300     02 RE-MARK                  PIC X(61).                       IF1294.2
006400 01  TEST-COMPUTED.                                               IF1294.2
006500     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1294.2
006600     02 FILLER                   PIC X(17)  VALUE                 IF1294.2
006700            "       COMPUTED=".                                   IF1294.2
006800     02 COMPUTED-X.                                               IF1294.2
006900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1294.2
007000     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1294.2
007100                                 PIC -9(9).9(9).                  IF1294.2
007200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1294.2
007300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1294.2
007400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1294.2
007500     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1294.2
007600         04 COMPUTED-18V0                    PIC -9(18).          IF1294.2
007700         04 FILLER                           PIC X.               IF1294.2
007800     03 FILLER PIC X(50) VALUE SPACE.                             IF1294.2
007900 01  TEST-CORRECT.                                                IF1294.2
008000     02 FILLER PIC X(30) VALUE SPACE.                             IF1294.2
008100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1294.2
008200     02 CORRECT-X.                                                IF1294.2
008300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1294.2
008400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1294.2
008500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1294.2
008600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1294.2
008700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1294.2
008800     03      CR-18V0 REDEFINES CORRECT-A.                         IF1294.2
008900         04 CORRECT-18V0                     PIC -9(18).          IF1294.2
009000         04 FILLER                           PIC X.               IF1294.2
009100     03 FILLER PIC X(2) VALUE SPACE.                              IF1294.2
009200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1294.2
009300 01  CCVS-C-1.                                                    IF1294.2
009400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1294.2
009500-    "SS  PARAGRAPH-NAME                                          IF1294.2
009600-    "       REMARKS".                                            IF1294.2
009700     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1294.2
009800 01  CCVS-C-2.                                                    IF1294.2
009900     02 FILLER                     PIC X        VALUE SPACE.      IF1294.2
010000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1294.2
010100     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1294.2
010200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1294.2
010300     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1294.2
010400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1294.2
010500 01  REC-CT                        PIC 99       VALUE ZERO.       IF1294.2
010600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1294.2
010700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1294.2
010800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1294.2
010900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1294.2
011000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1294.2
011100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1294.2
011200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1294.2
011300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1294.2
011400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1294.2
011500 01  CCVS-H-1.                                                    IF1294.2
011600     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1294.2
011700     02  FILLER                    PIC X(42)    VALUE             IF1294.2
011800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1294.2
011900     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1294.2
012000 01  CCVS-H-2A.                                                   IF1294.2
012100   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1294.2
012200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1294.2
012300   02  FILLER                        PIC XXXX   VALUE             IF1294.2
012400     "4.2 ".                                                      IF1294.2
012500   02  FILLER                        PIC X(28)  VALUE             IF1294.2
012600            " COPY - NOT FOR DISTRIBUTION".                       IF1294.2
012700   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1294.2
012800                                                                  IF1294.2
012900 01  CCVS-H-2B.                                                   IF1294.2
013000   02  FILLER                        PIC X(15)  VALUE             IF1294.2
013100            "TEST RESULT OF ".                                    IF1294.2
013200   02  TEST-ID                       PIC X(9).                    IF1294.2
013300   02  FILLER                        PIC X(4)   VALUE             IF1294.2
013400            " IN ".                                               IF1294.2
013500   02  FILLER                        PIC X(12)  VALUE             IF1294.2
013600     " HIGH       ".                                              IF1294.2
013700   02  FILLER                        PIC X(22)  VALUE             IF1294.2
013800            " LEVEL VALIDATION FOR ".                             IF1294.2
013900   02  FILLER                        PIC X(58)  VALUE             IF1294.2
014000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1294.2
014100 01  CCVS-H-3.                                                    IF1294.2
014200     02  FILLER                      PIC X(34)  VALUE             IF1294.2
014300            " FOR OFFICIAL USE ONLY    ".                         IF1294.2
014400     02  FILLER                      PIC X(58)  VALUE             IF1294.2
014500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1294.2
014600     02  FILLER                      PIC X(28)  VALUE             IF1294.2
014700            "  COPYRIGHT   1985 ".                                IF1294.2
014800 01  CCVS-E-1.                                                    IF1294.2
014900     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1294.2
015000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1294.2
015100     02 ID-AGAIN                     PIC X(9).                    IF1294.2
015200     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1294.2
015300 01  CCVS-E-2.                                                    IF1294.2
015400     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1294.2
015500     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1294.2
015600     02 CCVS-E-2-2.                                               IF1294.2
015700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1294.2
015800         03 FILLER                   PIC X      VALUE SPACE.      IF1294.2
015900         03 ENDER-DESC               PIC X(44)  VALUE             IF1294.2
016000            "ERRORS ENCOUNTERED".                                 IF1294.2
016100 01  CCVS-E-3.                                                    IF1294.2
016200     02  FILLER                      PIC X(22)  VALUE             IF1294.2
016300            " FOR OFFICIAL USE ONLY".                             IF1294.2
016400     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1294.2
016500     02  FILLER                      PIC X(58)  VALUE             IF1294.2
016600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1294.2
016700     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1294.2
016800     02 FILLER                       PIC X(15)  VALUE             IF1294.2
016900             " COPYRIGHT 1985".                                   IF1294.2
017000 01  CCVS-E-4.                                                    IF1294.2
017100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1294.2
017200     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1294.2
017300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1294.2
017400     02 FILLER                       PIC X(40)  VALUE             IF1294.2
017500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1294.2
017600 01  XXINFO.                                                      IF1294.2
017700     02 FILLER                       PIC X(19)  VALUE             IF1294.2
017800            "*** INFORMATION ***".                                IF1294.2
017900     02 INFO-TEXT.                                                IF1294.2
018000       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1294.2
018100       04 XXCOMPUTED                 PIC X(20).                   IF1294.2
018200       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1294.2
018300       04 XXCORRECT                  PIC X(20).                   IF1294.2
018400     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1294.2
018500 01  HYPHEN-LINE.                                                 IF1294.2
018600     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1294.2
018700     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1294.2
018800-    "*****************************************".                 IF1294.2
018900     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1294.2
019000-    "******************************".                            IF1294.2
019100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1294.2
019200     "IF129A".                                                    IF1294.2
019300 PROCEDURE DIVISION.                                              IF1294.2
019400 CCVS1 SECTION.                                                   IF1294.2
019500 OPEN-FILES.                                                      IF1294.2
019600     OPEN     OUTPUT PRINT-FILE.                                  IF1294.2
019700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1294.2
019800     MOVE    SPACE TO TEST-RESULTS.                               IF1294.2
019900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1294.2
020000     GO TO CCVS1-EXIT.                                            IF1294.2
020100 CLOSE-FILES.                                                     IF1294.2
020200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1294.2
020300 TERMINATE-CCVS.                                                  IF1294.2
020400     STOP     RUN.                                                IF1294.2
020500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1294.2
020600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1294.2
020700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1294.2
020800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1294.2
020900     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1294.2
021000 PRINT-DETAIL.                                                    IF1294.2
021100     IF REC-CT NOT EQUAL TO ZERO                                  IF1294.2
021200             MOVE "." TO PARDOT-X                                 IF1294.2
021300             MOVE REC-CT TO DOTVALUE.                             IF1294.2
021400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1294.2
021500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1294.2
021600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1294.2
021700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1294.2
021800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1294.2
021900     MOVE SPACE TO CORRECT-X.                                     IF1294.2
022000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1294.2
022100     MOVE     SPACE TO RE-MARK.                                   IF1294.2
022200 HEAD-ROUTINE.                                                    IF1294.2
022300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1294.2
022400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1294.2
022500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1294.2
022600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1294.2
022700 COLUMN-NAMES-ROUTINE.                                            IF1294.2
022800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1294.2
022900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1294.2
023000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1294.2
023100 END-ROUTINE.                                                     IF1294.2
023200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1294.2
023300 END-RTN-EXIT.                                                    IF1294.2
023400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1294.2
023500 END-ROUTINE-1.                                                   IF1294.2
023600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1294.2
023700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1294.2
023800      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1294.2
023900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1294.2
024000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1294.2
024100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1294.2
024200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1294.2
024300  END-ROUTINE-12.                                                 IF1294.2
024400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1294.2
024500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1294.2
024600         MOVE "NO " TO ERROR-TOTAL                                IF1294.2
024700         ELSE                                                     IF1294.2
024800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1294.2
024900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1294.2
025000     PERFORM WRITE-LINE.                                          IF1294.2
025100 END-ROUTINE-13.                                                  IF1294.2
025200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1294.2
025300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1294.2
025400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1294.2
025500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1294.2
025600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1294.2
025700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1294.2
025800          MOVE "NO " TO ERROR-TOTAL                               IF1294.2
025900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1294.2
026000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1294.2
026100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1294.2
026200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1294.2
026300 WRITE-LINE.                                                      IF1294.2
026400     ADD 1 TO RECORD-COUNT.                                       IF1294.2
026500*    IF RECORD-COUNT GREATER 42                                   IF1294.2
026600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1294.2
026700*        MOVE SPACE TO DUMMY-RECORD                               IF1294.2
026800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1294.2
026900*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1294.2
027000*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1294.2
027100*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1294.2
027200*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1294.2
027300*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1294.2
027400*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1294.2
027500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1294.2
027600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1294.2
027700*        MOVE ZERO TO RECORD-COUNT.                               IF1294.2
027800     PERFORM WRT-LN.                                              IF1294.2
027900 WRT-LN.                                                          IF1294.2
028000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1294.2
028100     MOVE SPACE TO DUMMY-RECORD.                                  IF1294.2
028200 BLANK-LINE-PRINT.                                                IF1294.2
028300     PERFORM WRT-LN.                                              IF1294.2
028400 FAIL-ROUTINE.                                                    IF1294.2
028500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1294.2
028600            GO TO FAIL-ROUTINE-WRITE.                             IF1294.2
028700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1294.2
028800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1294.2
028900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1294.2
029000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1294.2
029100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1294.2
029200     GO TO  FAIL-ROUTINE-EX.                                      IF1294.2
029300 FAIL-ROUTINE-WRITE.                                              IF1294.2
029400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IF1294.2
029500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IF1294.2
029600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IF1294.2
029700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1294.2
029800 FAIL-ROUTINE-EX. EXIT.                                           IF1294.2
029900 BAIL-OUT.                                                        IF1294.2
030000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1294.2
030100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1294.2
030200 BAIL-OUT-WRITE.                                                  IF1294.2
030300     MOVE CORRECT-A TO XXCORRECT.                                 IF1294.2
030400     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1294.2
030500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1294.2
030600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1294.2
030700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1294.2
030800 BAIL-OUT-EX. EXIT.                                               IF1294.2
030900 CCVS1-EXIT.                                                      IF1294.2
031000     EXIT.                                                        IF1294.2
031100********************************************************          IF1294.2
031200*                                                      *          IF1294.2
031300*    Intrinsic Function Tests         IF129A - ORD-MIN *          IF1294.2
031400*                                                      *          IF1294.2
031500********************************************************          IF1294.2
031600 SECT-IF129A SECTION.                                             IF1294.2
031700 F-ORD-MIN-INFO.                                                  IF1294.2
031800     MOVE     "See ref. A-62 2.33" TO ANSI-REFERENCE.             IF1294.2
031900     MOVE     "ORD-MIN Function"     TO FEATURE.                  IF1294.2
032000*****************TEST (a) ******************************          IF1294.2
032100 F-ORD-MIN-01.                                                    IF1294.2
032200     MOVE ZERO TO WS-INT.                                         IF1294.2
032300 F-ORD-MIN-TEST-01.                                               IF1294.2
032400     COMPUTE WS-INT = FUNCTION ORD-MIN(5, 3, 2, 8, 3, 1).         IF1294.2
032500     IF WS-INT = 6 THEN                                           IF1294.2
032600                        PERFORM PASS                              IF1294.2
032700     ELSE                                                         IF1294.2
032800                        MOVE 6   TO CORRECT-N                     IF1294.2
032900                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
033000                        PERFORM FAIL.                             IF1294.2
033100     GO TO F-ORD-MIN-WRITE-01.                                    IF1294.2
033200 F-ORD-MIN-DELETE-01.                                             IF1294.2
033300     PERFORM  DE-LETE.                                            IF1294.2
033400     GO TO    F-ORD-MIN-WRITE-01.                                 IF1294.2
033500 F-ORD-MIN-WRITE-01.                                              IF1294.2
033600     MOVE "F-ORD-MIN-01" TO PAR-NAME.                             IF1294.2
033700     PERFORM  PRINT-DETAIL.                                       IF1294.2
033800*****************TEST (b) ******************************          IF1294.2
033900 F-ORD-MIN-TEST-02.                                               IF1294.2
034000     EVALUATE FUNCTION ORD-MIN(3, 2, 7, 1, 5)                     IF1294.2
034100     WHEN 4                                                       IF1294.2
034200                                 PERFORM PASS                     IF1294.2
034300                                 GO TO F-ORD-MIN-WRITE-02.        IF1294.2
034400     PERFORM FAIL.                                                IF1294.2
034500     GO TO F-ORD-MIN-WRITE-02.                                    IF1294.2
034600 F-ORD-MIN-DELETE-02.                                             IF1294.2
034700     PERFORM  DE-LETE.                                            IF1294.2
034800     GO TO    F-ORD-MIN-WRITE-02.                                 IF1294.2
034900 F-ORD-MIN-WRITE-02.                                              IF1294.2
035000     MOVE "F-ORD-MIN-02" TO PAR-NAME.                             IF1294.2
035100     PERFORM  PRINT-DETAIL.                                       IF1294.2
035200*****************TEST (c) ******************************          IF1294.2
035300 F-ORD-MIN-03.                                                    IF1294.2
035400     MOVE ZERO TO WS-INT.                                         IF1294.2
035500 F-ORD-MIN-TEST-03.                                               IF1294.2
035600     IF FUNCTION ORD-MIN(5, 4, 3, 6, 2, 8) = 5                    IF1294.2
035700                        PERFORM PASS                              IF1294.2
035800     ELSE                                                         IF1294.2
035900                        PERFORM FAIL.                             IF1294.2
036000     GO TO F-ORD-MIN-WRITE-03.                                    IF1294.2
036100 F-ORD-MIN-DELETE-03.                                             IF1294.2
036200     PERFORM  DE-LETE.                                            IF1294.2
036300     GO TO    F-ORD-MIN-WRITE-03.                                 IF1294.2
036400 F-ORD-MIN-WRITE-03.                                              IF1294.2
036500     MOVE "F-ORD-MIN-03" TO PAR-NAME.                             IF1294.2
036600     PERFORM  PRINT-DETAIL.                                       IF1294.2
036700*****************TEST (d) ******************************          IF1294.2
036800 F-ORD-MIN-04.                                                    IF1294.2
036900     MOVE ZERO TO WS-INT.                                         IF1294.2
037000 F-ORD-MIN-TEST-04.                                               IF1294.2
037100     COMPUTE WS-INT = FUNCTION ORD-MIN(A, B, C).                  IF1294.2
037200     IF WS-INT = 3 THEN                                           IF1294.2
037300                        PERFORM PASS                              IF1294.2
037400     ELSE                                                         IF1294.2
037500                        MOVE 3   TO CORRECT-N                     IF1294.2
037600                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
037700                        PERFORM FAIL.                             IF1294.2
037800     GO TO F-ORD-MIN-WRITE-04.                                    IF1294.2
037900 F-ORD-MIN-DELETE-04.                                             IF1294.2
038000     PERFORM  DE-LETE.                                            IF1294.2
038100     GO TO    F-ORD-MIN-WRITE-04.                                 IF1294.2
038200 F-ORD-MIN-WRITE-04.                                              IF1294.2
038300     MOVE "F-ORD-MIN-04" TO PAR-NAME.                             IF1294.2
038400     PERFORM  PRINT-DETAIL.                                       IF1294.2
038500*****************TEST (e) ******************************          IF1294.2
038600 F-ORD-MIN-05.                                                    IF1294.2
038700     MOVE ZERO TO WS-INT.                                         IF1294.2
038800 F-ORD-MIN-TEST-05.                                               IF1294.2
038900     COMPUTE WS-INT = FUNCTION ORD-MIN(A, B, D).                  IF1294.2
039000     IF WS-INT = 1 THEN                                           IF1294.2
039100                        PERFORM PASS                              IF1294.2
039200     ELSE                                                         IF1294.2
039300                        MOVE 1    TO CORRECT-N                    IF1294.2
039400                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
039500                        PERFORM FAIL.                             IF1294.2
039600     GO TO F-ORD-MIN-WRITE-05.                                    IF1294.2
039700 F-ORD-MIN-DELETE-05.                                             IF1294.2
039800     PERFORM  DE-LETE.                                            IF1294.2
039900     GO TO    F-ORD-MIN-WRITE-05.                                 IF1294.2
040000 F-ORD-MIN-WRITE-05.                                              IF1294.2
040100     MOVE "F-ORD-MIN-05" TO PAR-NAME.                             IF1294.2
040200     PERFORM  PRINT-DETAIL.                                       IF1294.2
040300*****************TEST (f) ******************************          IF1294.2
040400 F-ORD-MIN-06.                                                    IF1294.2
040500     MOVE ZERO TO WS-INT.                                         IF1294.2
040600 F-ORD-MIN-TEST-06.                                               IF1294.2
040700     COMPUTE WS-INT = FUNCTION ORD-MIN(A, 4, B, 7, 1, 9).         IF1294.2
040800     IF WS-INT = 5 THEN                                           IF1294.2
040900                        PERFORM PASS                              IF1294.2
041000     ELSE                                                         IF1294.2
041100                        MOVE 5   TO CORRECT-N                     IF1294.2
041200                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
041300                        PERFORM FAIL.                             IF1294.2
041400     GO TO F-ORD-MIN-WRITE-06.                                    IF1294.2
041500 F-ORD-MIN-DELETE-06.                                             IF1294.2
041600     PERFORM  DE-LETE.                                            IF1294.2
041700     GO TO    F-ORD-MIN-WRITE-06.                                 IF1294.2
041800 F-ORD-MIN-WRITE-06.                                              IF1294.2
041900     MOVE "F-ORD-MIN-06" TO PAR-NAME.                             IF1294.2
042000     PERFORM  PRINT-DETAIL.                                       IF1294.2
042100*****************TEST (g) ******************************          IF1294.2
042200 F-ORD-MIN-07.                                                    IF1294.2
042300     MOVE ZERO TO WS-INT.                                         IF1294.2
042400 F-ORD-MIN-TEST-07.                                               IF1294.2
042500     COMPUTE WS-INT = FUNCTION ORD-MIN(4, 1, A, 3).               IF1294.2
042600     IF WS-INT = 2 THEN                                           IF1294.2
042700                        PERFORM PASS                              IF1294.2
042800     ELSE                                                         IF1294.2
042900                        MOVE 2    TO CORRECT-N                    IF1294.2
043000                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
043100                        PERFORM FAIL.                             IF1294.2
043200     GO TO F-ORD-MIN-WRITE-07.                                    IF1294.2
043300 F-ORD-MIN-DELETE-07.                                             IF1294.2
043400     PERFORM  DE-LETE.                                            IF1294.2
043500     GO TO    F-ORD-MIN-WRITE-07.                                 IF1294.2
043600 F-ORD-MIN-WRITE-07.                                              IF1294.2
043700     MOVE "F-ORD-MIN-07" TO PAR-NAME.                             IF1294.2
043800     PERFORM  PRINT-DETAIL.                                       IF1294.2
043900*****************TEST (h) ******************************          IF1294.2
044000 F-ORD-MIN-08.                                                    IF1294.2
044100     MOVE ZERO TO WS-INT.                                         IF1294.2
044200 F-ORD-MIN-TEST-08.                                               IF1294.2
044300     COMPUTE WS-INT = FUNCTION ORD-MIN("A", I, "P").              IF1294.2
044400     IF WS-INT = 1 THEN                                           IF1294.2
044500                        PERFORM PASS                              IF1294.2
044600     ELSE                                                         IF1294.2
044700                        MOVE 1    TO CORRECT-N                    IF1294.2
044800                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
044900                        PERFORM FAIL.                             IF1294.2
045000     GO TO F-ORD-MIN-WRITE-08.                                    IF1294.2
045100 F-ORD-MIN-DELETE-08.                                             IF1294.2
045200     PERFORM  DE-LETE.                                            IF1294.2
045300     GO TO    F-ORD-MIN-WRITE-08.                                 IF1294.2
045400 F-ORD-MIN-WRITE-08.                                              IF1294.2
045500     MOVE "F-ORD-MIN-08" TO PAR-NAME.                             IF1294.2
045600     PERFORM  PRINT-DETAIL.                                       IF1294.2
045700*****************TEST (i) ******************************          IF1294.2
045800 F-ORD-MIN-09.                                                    IF1294.2
045900     MOVE ZERO TO WS-INT.                                         IF1294.2
046000 F-ORD-MIN-TEST-09.                                               IF1294.2
046100     COMPUTE WS-INT = FUNCTION ORD-MIN("S", "D", J).              IF1294.2
046200     IF WS-INT = 2 THEN                                           IF1294.2
046300                        PERFORM PASS                              IF1294.2
046400     ELSE                                                         IF1294.2
046500                        MOVE 2   TO CORRECT-N                     IF1294.2
046600                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
046700                        PERFORM FAIL.                             IF1294.2
046800     GO TO F-ORD-MIN-WRITE-09.                                    IF1294.2
046900 F-ORD-MIN-DELETE-09.                                             IF1294.2
047000     PERFORM  DE-LETE.                                            IF1294.2
047100     GO TO    F-ORD-MIN-WRITE-09.                                 IF1294.2
047200 F-ORD-MIN-WRITE-09.                                              IF1294.2
047300     MOVE "F-ORD-MIN-09" TO PAR-NAME.                             IF1294.2
047400     PERFORM  PRINT-DETAIL.                                       IF1294.2
047500*****************TEST (j) ******************************          IF1294.2
047600 F-ORD-MIN-10.                                                    IF1294.2
047700     MOVE ZERO TO WS-INT.                                         IF1294.2
047800 F-ORD-MIN-TEST-10.                                               IF1294.2
047900     COMPUTE WS-INT = FUNCTION ORD-MIN(A, 5, 5, A).               IF1294.2
048000     IF WS-INT = 1 THEN                                           IF1294.2
048100                        PERFORM PASS                              IF1294.2
048200     ELSE                                                         IF1294.2
048300                        MOVE 1    TO CORRECT-N                    IF1294.2
048400                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
048500                        PERFORM FAIL.                             IF1294.2
048600     GO TO F-ORD-MIN-WRITE-10.                                    IF1294.2
048700 F-ORD-MIN-DELETE-10.                                             IF1294.2
048800     PERFORM  DE-LETE.                                            IF1294.2
048900     GO TO    F-ORD-MIN-WRITE-10.                                 IF1294.2
049000 F-ORD-MIN-WRITE-10.                                              IF1294.2
049100     MOVE "F-ORD-MIN-10" TO PAR-NAME.                             IF1294.2
049200     PERFORM  PRINT-DETAIL.                                       IF1294.2
049300*****************TEST (k) ******************************          IF1294.2
049400 F-ORD-MIN-11.                                                    IF1294.2
049500     MOVE ZERO TO WS-INT.                                         IF1294.2
049600 F-ORD-MIN-TEST-11.                                               IF1294.2
049700     COMPUTE WS-INT = FUNCTION ORD-MIN(IND(1), IND(2), IND(3)).   IF1294.2
049800     IF WS-INT = 2 THEN                                           IF1294.2
049900                        PERFORM PASS                              IF1294.2
050000     ELSE                                                         IF1294.2
050100                        MOVE 2   TO CORRECT-N                     IF1294.2
050200                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
050300                        PERFORM FAIL.                             IF1294.2
050400     GO TO F-ORD-MIN-WRITE-11.                                    IF1294.2
050500 F-ORD-MIN-DELETE-11.                                             IF1294.2
050600     PERFORM  DE-LETE.                                            IF1294.2
050700     GO TO    F-ORD-MIN-WRITE-11.                                 IF1294.2
050800 F-ORD-MIN-WRITE-11.                                              IF1294.2
050900     MOVE "F-ORD-MIN-11" TO PAR-NAME.                             IF1294.2
051000     PERFORM  PRINT-DETAIL.                                       IF1294.2
051100*****************TEST (l) ******************************          IF1294.2
051200 F-ORD-MIN-12.                                                    IF1294.2
051300     MOVE ZERO TO WS-INT.                                         IF1294.2
051400 F-ORD-MIN-TEST-12.                                               IF1294.2
051500     COMPUTE WS-INT = FUNCTION ORD-MIN(IND(P), IND(Q), IND(R)).   IF1294.2
051600     IF WS-INT = 1 THEN                                           IF1294.2
051700                        PERFORM PASS                              IF1294.2
051800     ELSE                                                         IF1294.2
051900                        MOVE 1    TO CORRECT-N                    IF1294.2
052000                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
052100                        PERFORM FAIL.                             IF1294.2
052200     GO TO F-ORD-MIN-WRITE-12.                                    IF1294.2
052300 F-ORD-MIN-DELETE-12.                                             IF1294.2
052400     PERFORM  DE-LETE.                                            IF1294.2
052500     GO TO    F-ORD-MIN-WRITE-12.                                 IF1294.2
052600 F-ORD-MIN-WRITE-12.                                              IF1294.2
052700     MOVE "F-ORD-MIN-12" TO PAR-NAME.                             IF1294.2
052800     PERFORM  PRINT-DETAIL.                                       IF1294.2
052900*****************TEST (m) ******************************          IF1294.2
053000 F-ORD-MIN-13.                                                    IF1294.2
053100     MOVE ZERO TO WS-INT.                                         IF1294.2
053200 F-ORD-MIN-TEST-13.                                               IF1294.2
053300     COMPUTE WS-INT = FUNCTION ORD-MIN(                           IF1294.2
053400                           FUNCTION ORD-MIN(1, 4), 3, 7).         IF1294.2
053500     IF WS-INT = 1 THEN                                           IF1294.2
053600                        PERFORM PASS                              IF1294.2
053700     ELSE                                                         IF1294.2
053800                        MOVE 1    TO CORRECT-N                    IF1294.2
053900                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
054000                        PERFORM FAIL.                             IF1294.2
054100     GO TO F-ORD-MIN-WRITE-13.                                    IF1294.2
054200 F-ORD-MIN-DELETE-13.                                             IF1294.2
054300     PERFORM  DE-LETE.                                            IF1294.2
054400     GO TO    F-ORD-MIN-WRITE-13.                                 IF1294.2
054500 F-ORD-MIN-WRITE-13.                                              IF1294.2
054600     MOVE "F-ORD-MIN-13" TO PAR-NAME.                             IF1294.2
054700     PERFORM  PRINT-DETAIL.                                       IF1294.2
054800*****************TEST (n) ******************************          IF1294.2
054900 F-ORD-MIN-14.                                                    IF1294.2
055000     MOVE ZERO TO WS-INT.                                         IF1294.2
055100 F-ORD-MIN-TEST-14.                                               IF1294.2
055200     COMPUTE WS-INT = FUNCTION ORD-MIN(IND(ALL)).                 IF1294.2
055300     IF WS-INT = 2 THEN                                           IF1294.2
055400                        PERFORM PASS                              IF1294.2
055500     ELSE                                                         IF1294.2
055600                        MOVE 2    TO CORRECT-N                    IF1294.2
055700                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
055800                        PERFORM FAIL.                             IF1294.2
055900     GO TO F-ORD-MIN-WRITE-14.                                    IF1294.2
056000 F-ORD-MIN-DELETE-14.                                             IF1294.2
056100     PERFORM  DE-LETE.                                            IF1294.2
056200     GO TO    F-ORD-MIN-WRITE-14.                                 IF1294.2
056300 F-ORD-MIN-WRITE-14.                                              IF1294.2
056400     MOVE "F-ORD-MIN-14" TO PAR-NAME.                             IF1294.2
056500     PERFORM  PRINT-DETAIL.                                       IF1294.2
056600*****************TEST (o) ******************************          IF1294.2
056700 F-ORD-MIN-15.                                                    IF1294.2
056800     MOVE ZERO TO WS-INT.                                         IF1294.2
056900 F-ORD-MIN-TEST-15.                                               IF1294.2
057000     COMPUTE WS-INT = FUNCTION ORD-MIN(2, 3, C) + A.              IF1294.2
057100     IF WS-INT = 6 THEN                                           IF1294.2
057200                        PERFORM PASS                              IF1294.2
057300     ELSE                                                         IF1294.2
057400                        MOVE 6    TO CORRECT-N                    IF1294.2
057500                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
057600                        PERFORM FAIL.                             IF1294.2
057700     GO TO F-ORD-MIN-WRITE-15.                                    IF1294.2
057800 F-ORD-MIN-DELETE-15.                                             IF1294.2
057900     PERFORM  DE-LETE.                                            IF1294.2
058000     GO TO    F-ORD-MIN-WRITE-15.                                 IF1294.2
058100 F-ORD-MIN-WRITE-15.                                              IF1294.2
058200     MOVE "F-ORD-MIN-15" TO PAR-NAME.                             IF1294.2
058300     PERFORM  PRINT-DETAIL.                                       IF1294.2
058400*****************TEST (p) ******************************          IF1294.2
058500 F-ORD-MIN-16.                                                    IF1294.2
058600     MOVE ZERO TO WS-INT.                                         IF1294.2
058700 F-ORD-MIN-TEST-16.                                               IF1294.2
058800     COMPUTE WS-INT = FUNCTION ORD-MIN(9, 3, A) +                 IF1294.2
058900                      FUNCTION ORD-MIN(1, 1).                     IF1294.2
059000     IF WS-INT = 3 THEN                                           IF1294.2
059100                        PERFORM PASS                              IF1294.2
059200     ELSE                                                         IF1294.2
059300                        MOVE 3    TO CORRECT-N                    IF1294.2
059400                        MOVE WS-INT TO COMPUTED-N                 IF1294.2
059500                        PERFORM FAIL.                             IF1294.2
059600     GO TO F-ORD-MIN-WRITE-16.                                    IF1294.2
059700 F-ORD-MIN-DELETE-16.                                             IF1294.2
059800     PERFORM  DE-LETE.                                            IF1294.2
059900     GO TO    F-ORD-MIN-WRITE-16.                                 IF1294.2
060000 F-ORD-MIN-WRITE-16.                                              IF1294.2
060100     MOVE "F-ORD-MIN-16" TO PAR-NAME.                             IF1294.2
060200     PERFORM  PRINT-DETAIL.                                       IF1294.2
060300*****************SPECIAL TEST 1******************************     IF1294.2
060400 F-ORD-MIN-17.                                                    IF1294.2
060500     MOVE 10 TO ARG1                                              IF1294.2
060600     PERFORM F-ORD-MIN-TEST-17                                    IF1294.2
060700       UNTIL FUNCTION ORD-MIN(2, ARG1) > 1.                       IF1294.2
060800*                                                                 IF1294.2
060900**    when ARG1 = 10 .. 2 ORD-MIN(2,ARG1) = 1                     IF1294.2
061000**    when ARG1 = 1 , ORD-MIN(5,ARG1) = 2                         IF1294.2
061100*                                                                 IF1294.2
061200     IF ARG1 = 1 THEN                                             IF1294.2
061300                        PERFORM PASS                              IF1294.2
061400     ELSE                                                         IF1294.2
061500                        MOVE 1 TO CORRECT-N                       IF1294.2
061600                        MOVE ARG1   TO COMPUTED-N                 IF1294.2
061700                        PERFORM FAIL.                             IF1294.2
061800     GO TO F-ORD-MIN-WRITE-17.                                    IF1294.2
061900*                                                                 IF1294.2
062000 F-ORD-MIN-TEST-17.                                               IF1294.2
062100     COMPUTE ARG1 = ARG1 - 1.                                     IF1294.2
062200*                                                                 IF1294.2
062300 F-ORD-MIN-DELETE-17.                                             IF1294.2
062400     PERFORM  DE-LETE.                                            IF1294.2
062500     GO TO    F-ORD-MIN-WRITE-17.                                 IF1294.2
062600 F-ORD-MIN-WRITE-17.                                              IF1294.2
062700     MOVE "F-ORD-MIN-17" TO PAR-NAME.                             IF1294.2
062800     PERFORM  PRINT-DETAIL.                                       IF1294.2
062900*******************END OF TESTS**************************         IF1294.2
063000 CCVS-EXIT SECTION.                                               IF1294.2
063100 CCVS-999999.                                                     IF1294.2
063200     GO TO CLOSE-FILES.                                           IF1294.2
      *END-OF,IF129A                                                                  
