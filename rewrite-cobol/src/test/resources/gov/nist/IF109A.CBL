      *HEADER,COBOL,IF109A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1094.2
000200 PROGRAM-ID.                                                      IF1094.2
000300     IF109A.                                                      IF1094.2
000400                                                                  IF1094.2
000500***********************************************************       IF1094.2
000600*                                                         *       IF1094.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1094.2
000800* It contains tests for the Intrinsic Function            *       IF1094.2
000900* DAY-OF-INTEGER.                                         *       IF1094.2
001000*                                                         *       IF1094.2
001100***********************************************************       IF1094.2
001200 ENVIRONMENT DIVISION.                                            IF1094.2
001300 CONFIGURATION SECTION.                                           IF1094.2
001400 SOURCE-COMPUTER.                                                 IF1094.2
001500     XXXXX082.                                                    IF1094.2
001600 OBJECT-COMPUTER.                                                 IF1094.2
001700     XXXXX083.                                                    IF1094.2
001800 INPUT-OUTPUT SECTION.                                            IF1094.2
001900 FILE-CONTROL.                                                    IF1094.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1094.2
002100     XXXXX055.                                                    IF1094.2
002200 DATA DIVISION.                                                   IF1094.2
002300 FILE SECTION.                                                    IF1094.2
002400 FD  PRINT-FILE.                                                  IF1094.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1094.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1094.2
002700 WORKING-STORAGE SECTION.                                         IF1094.2
002800***********************************************************       IF1094.2
002900* Variables specific to the Intrinsic Function Test IF109A*       IF1094.2
003000***********************************************************       IF1094.2
003100 01  A                   PIC S9(10)     VALUE 400.                IF1094.2
003200 01  C                   PIC S9(10)     VALUE 365.                IF1094.2
003300 01  D                   PIC S9(10)     VALUE 1.                  IF1094.2
003400 01  ARG1                PIC S9(10)     VALUE 1.                  IF1094.2
003500 01  ARR                                VALUE "40537".            IF1094.2
003600     02 IND OCCURS 5 TIMES PIC 9.                                 IF1094.2
003700 01  TEMP                PIC S9(5)V9(5).                          IF1094.2
003800 01  WS-DATE             PIC 9(7).                                IF1094.2
003900*                                                                 IF1094.2
004000**********************************************************        IF1094.2
004100*                                                                 IF1094.2
004200 01  TEST-RESULTS.                                                IF1094.2
004300     02 FILLER                   PIC X      VALUE SPACE.          IF1094.2
004400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1094.2
004500     02 FILLER                   PIC X      VALUE SPACE.          IF1094.2
004600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1094.2
004700     02 FILLER                   PIC X      VALUE SPACE.          IF1094.2
004800     02  PAR-NAME.                                                IF1094.2
004900       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1094.2
005000       03  PARDOT-X              PIC X      VALUE SPACE.          IF1094.2
005100       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1094.2
005200     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1094.2
005300     02 RE-MARK                  PIC X(61).                       IF1094.2
005400 01  TEST-COMPUTED.                                               IF1094.2
005500     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1094.2
005600     02 FILLER                   PIC X(17)  VALUE                 IF1094.2
005700            "       COMPUTED=".                                   IF1094.2
005800     02 COMPUTED-X.                                               IF1094.2
005900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1094.2
006000     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1094.2
006100                                 PIC -9(9).9(9).                  IF1094.2
006200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1094.2
006300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1094.2
006400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1094.2
006500     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1094.2
006600         04 COMPUTED-18V0                    PIC -9(18).          IF1094.2
006700         04 FILLER                           PIC X.               IF1094.2
006800     03 FILLER PIC X(50) VALUE SPACE.                             IF1094.2
006900 01  TEST-CORRECT.                                                IF1094.2
007000     02 FILLER PIC X(30) VALUE SPACE.                             IF1094.2
007100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1094.2
007200     02 CORRECT-X.                                                IF1094.2
007300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1094.2
007400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1094.2
007500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1094.2
007600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1094.2
007700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1094.2
007800     03      CR-18V0 REDEFINES CORRECT-A.                         IF1094.2
007900         04 CORRECT-18V0                     PIC -9(18).          IF1094.2
008000         04 FILLER                           PIC X.               IF1094.2
008100     03 FILLER PIC X(2) VALUE SPACE.                              IF1094.2
008200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1094.2
008300 01  CCVS-C-1.                                                    IF1094.2
008400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1094.2
008500-    "SS  PARAGRAPH-NAME                                          IF1094.2
008600-    "       REMARKS".                                            IF1094.2
008700     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1094.2
008800 01  CCVS-C-2.                                                    IF1094.2
008900     02 FILLER                     PIC X        VALUE SPACE.      IF1094.2
009000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1094.2
009100     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1094.2
009200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1094.2
009300     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1094.2
009400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1094.2
009500 01  REC-CT                        PIC 99       VALUE ZERO.       IF1094.2
009600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1094.2
009700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1094.2
009800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1094.2
009900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1094.2
010000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1094.2
010100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1094.2
010200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1094.2
010300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1094.2
010400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1094.2
010500 01  CCVS-H-1.                                                    IF1094.2
010600     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1094.2
010700     02  FILLER                    PIC X(42)    VALUE             IF1094.2
010800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1094.2
010900     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1094.2
011000 01  CCVS-H-2A.                                                   IF1094.2
011100   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1094.2
011200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1094.2
011300   02  FILLER                        PIC XXXX   VALUE             IF1094.2
011400     "4.2 ".                                                      IF1094.2
011500   02  FILLER                        PIC X(28)  VALUE             IF1094.2
011600            " COPY - NOT FOR DISTRIBUTION".                       IF1094.2
011700   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1094.2
011800                                                                  IF1094.2
011900 01  CCVS-H-2B.                                                   IF1094.2
012000   02  FILLER                        PIC X(15)  VALUE             IF1094.2
012100            "TEST RESULT OF ".                                    IF1094.2
012200   02  TEST-ID                       PIC X(9).                    IF1094.2
012300   02  FILLER                        PIC X(4)   VALUE             IF1094.2
012400            " IN ".                                               IF1094.2
012500   02  FILLER                        PIC X(12)  VALUE             IF1094.2
012600     " HIGH       ".                                              IF1094.2
012700   02  FILLER                        PIC X(22)  VALUE             IF1094.2
012800            " LEVEL VALIDATION FOR ".                             IF1094.2
012900   02  FILLER                        PIC X(58)  VALUE             IF1094.2
013000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1094.2
013100 01  CCVS-H-3.                                                    IF1094.2
013200     02  FILLER                      PIC X(34)  VALUE             IF1094.2
013300            " FOR OFFICIAL USE ONLY    ".                         IF1094.2
013400     02  FILLER                      PIC X(58)  VALUE             IF1094.2
013500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1094.2
013600     02  FILLER                      PIC X(28)  VALUE             IF1094.2
013700            "  COPYRIGHT   1985 ".                                IF1094.2
013800 01  CCVS-E-1.                                                    IF1094.2
013900     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1094.2
014000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1094.2
014100     02 ID-AGAIN                     PIC X(9).                    IF1094.2
014200     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1094.2
014300 01  CCVS-E-2.                                                    IF1094.2
014400     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1094.2
014500     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1094.2
014600     02 CCVS-E-2-2.                                               IF1094.2
014700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1094.2
014800         03 FILLER                   PIC X      VALUE SPACE.      IF1094.2
014900         03 ENDER-DESC               PIC X(44)  VALUE             IF1094.2
015000            "ERRORS ENCOUNTERED".                                 IF1094.2
015100 01  CCVS-E-3.                                                    IF1094.2
015200     02  FILLER                      PIC X(22)  VALUE             IF1094.2
015300            " FOR OFFICIAL USE ONLY".                             IF1094.2
015400     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1094.2
015500     02  FILLER                      PIC X(58)  VALUE             IF1094.2
015600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1094.2
015700     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1094.2
015800     02 FILLER                       PIC X(15)  VALUE             IF1094.2
015900             " COPYRIGHT 1985".                                   IF1094.2
016000 01  CCVS-E-4.                                                    IF1094.2
016100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1094.2
016200     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1094.2
016300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1094.2
016400     02 FILLER                       PIC X(40)  VALUE             IF1094.2
016500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1094.2
016600 01  XXINFO.                                                      IF1094.2
016700     02 FILLER                       PIC X(19)  VALUE             IF1094.2
016800            "*** INFORMATION ***".                                IF1094.2
016900     02 INFO-TEXT.                                                IF1094.2
017000       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1094.2
017100       04 XXCOMPUTED                 PIC X(20).                   IF1094.2
017200       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1094.2
017300       04 XXCORRECT                  PIC X(20).                   IF1094.2
017400     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1094.2
017500 01  HYPHEN-LINE.                                                 IF1094.2
017600     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1094.2
017700     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1094.2
017800-    "*****************************************".                 IF1094.2
017900     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1094.2
018000-    "******************************".                            IF1094.2
018100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1094.2
018200     "IF109A".                                                    IF1094.2
018300 PROCEDURE DIVISION.                                              IF1094.2
018400 CCVS1 SECTION.                                                   IF1094.2
018500 OPEN-FILES.                                                      IF1094.2
018600     OPEN     OUTPUT PRINT-FILE.                                  IF1094.2
018700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1094.2
018800     MOVE    SPACE TO TEST-RESULTS.                               IF1094.2
018900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1094.2
019000     GO TO CCVS1-EXIT.                                            IF1094.2
019100 CLOSE-FILES.                                                     IF1094.2
019200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1094.2
019300 TERMINATE-CCVS.                                                  IF1094.2
019400     STOP     RUN.                                                IF1094.2
019500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1094.2
019600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1094.2
019700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1094.2
019800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1094.2
019900     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1094.2
020000 PRINT-DETAIL.                                                    IF1094.2
020100     IF REC-CT NOT EQUAL TO ZERO                                  IF1094.2
020200             MOVE "." TO PARDOT-X                                 IF1094.2
020300             MOVE REC-CT TO DOTVALUE.                             IF1094.2
020400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1094.2
020500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1094.2
020600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1094.2
020700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1094.2
020800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1094.2
020900     MOVE SPACE TO CORRECT-X.                                     IF1094.2
021000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1094.2
021100     MOVE     SPACE TO RE-MARK.                                   IF1094.2
021200 HEAD-ROUTINE.                                                    IF1094.2
021300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1094.2
021400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1094.2
021500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1094.2
021600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1094.2
021700 COLUMN-NAMES-ROUTINE.                                            IF1094.2
021800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1094.2
021900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1094.2
022000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1094.2
022100 END-ROUTINE.                                                     IF1094.2
022200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1094.2
022300 END-RTN-EXIT.                                                    IF1094.2
022400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1094.2
022500 END-ROUTINE-1.                                                   IF1094.2
022600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1094.2
022700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1094.2
022800      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1094.2
022900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1094.2
023000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1094.2
023100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1094.2
023200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1094.2
023300  END-ROUTINE-12.                                                 IF1094.2
023400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1094.2
023500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1094.2
023600         MOVE "NO " TO ERROR-TOTAL                                IF1094.2
023700         ELSE                                                     IF1094.2
023800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1094.2
023900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1094.2
024000     PERFORM WRITE-LINE.                                          IF1094.2
024100 END-ROUTINE-13.                                                  IF1094.2
024200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1094.2
024300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1094.2
024400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1094.2
024500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1094.2
024600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1094.2
024700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1094.2
024800          MOVE "NO " TO ERROR-TOTAL                               IF1094.2
024900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1094.2
025000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1094.2
025100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1094.2
025200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1094.2
025300 WRITE-LINE.                                                      IF1094.2
025400     ADD 1 TO RECORD-COUNT.                                       IF1094.2
025500*    IF RECORD-COUNT GREATER 42                                   IF1094.2
025600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1094.2
025700*        MOVE SPACE TO DUMMY-RECORD                               IF1094.2
025800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1094.2
025900*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1094.2
026000*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1094.2
026100*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1094.2
026200*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1094.2
026300*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1094.2
026400*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1094.2
026500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1094.2
026600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1094.2
026700*        MOVE ZERO TO RECORD-COUNT.                               IF1094.2
026800     PERFORM WRT-LN.                                              IF1094.2
026900 WRT-LN.                                                          IF1094.2
027000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1094.2
027100     MOVE SPACE TO DUMMY-RECORD.                                  IF1094.2
027200 BLANK-LINE-PRINT.                                                IF1094.2
027300     PERFORM WRT-LN.                                              IF1094.2
027400 FAIL-ROUTINE.                                                    IF1094.2
027500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1094.2
027600            GO TO FAIL-ROUTINE-WRITE.                             IF1094.2
027700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1094.2
027800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1094.2
027900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1094.2
028000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1094.2
028100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1094.2
028200     GO TO  FAIL-ROUTINE-EX.                                      IF1094.2
028300 FAIL-ROUTINE-WRITE.                                              IF1094.2
028400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IF1094.2
028500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IF1094.2
028600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IF1094.2
028700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1094.2
028800 FAIL-ROUTINE-EX. EXIT.                                           IF1094.2
028900 BAIL-OUT.                                                        IF1094.2
029000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1094.2
029100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1094.2
029200 BAIL-OUT-WRITE.                                                  IF1094.2
029300     MOVE CORRECT-A TO XXCORRECT.                                 IF1094.2
029400     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1094.2
029500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1094.2
029600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1094.2
029700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1094.2
029800 BAIL-OUT-EX. EXIT.                                               IF1094.2
029900 CCVS1-EXIT.                                                      IF1094.2
030000     EXIT.                                                        IF1094.2
030100********************************************************          IF1094.2
030200*                                                      *          IF1094.2
030300*    Intrinsic Function Test  IF109A - DAY-OF-INTEGER  *          IF1094.2
030400*                                                      *          IF1094.2
030500********************************************************          IF1094.2
030600 SECT-IF109A SECTION.                                             IF1094.2
030700 F-DAYOFINT-INFO.                                                 IF1094.2
030800     MOVE     "See ref. A-42 2.13" TO ANSI-REFERENCE.             IF1094.2
030900     MOVE     "DAY-OF-INTEGER"     TO FEATURE.                    IF1094.2
031000*****************TEST (a) ******************************          IF1094.2
031100 F-DAYOFINT-01.                                                   IF1094.2
031200     MOVE ZERO TO WS-DATE.                                        IF1094.2
031300 F-DAYOFINT-TEST-01.                                              IF1094.2
031400     COMPUTE WS-DATE = FUNCTION DAY-OF-INTEGER(1).                IF1094.2
031500     IF WS-DATE = 1601001 THEN                                    IF1094.2
031600                        PERFORM PASS                              IF1094.2
031700     ELSE                                                         IF1094.2
031800                        MOVE  1601001  TO CORRECT-N               IF1094.2
031900                        MOVE WS-DATE TO COMPUTED-N                IF1094.2
032000                        PERFORM FAIL.                             IF1094.2
032100     GO TO F-DAYOFINT-WRITE-01.                                   IF1094.2
032200 F-DAYOFINT-DELETE-01.                                            IF1094.2
032300     PERFORM  DE-LETE.                                            IF1094.2
032400     GO TO    F-DAYOFINT-WRITE-01.                                IF1094.2
032500 F-DAYOFINT-WRITE-01.                                             IF1094.2
032600     MOVE "F-DAYOFINT-01" TO PAR-NAME.                            IF1094.2
032700     PERFORM  PRINT-DETAIL.                                       IF1094.2
032800*****************TEST (b) ******************************          IF1094.2
032900 F-DAYOFINT-TEST-02.                                              IF1094.2
033000     EVALUATE FUNCTION DAY-OF-INTEGER(A)                          IF1094.2
033100     WHEN     1602035                                             IF1094.2
033200                    PERFORM PASS                                  IF1094.2
033300                    GO TO F-DAYOFINT-WRITE-02.                    IF1094.2
033400     PERFORM FAIL.                                                IF1094.2
033500     GO TO F-DAYOFINT-WRITE-02.                                   IF1094.2
033600 F-DAYOFINT-DELETE-02.                                            IF1094.2
033700     PERFORM  DE-LETE.                                            IF1094.2
033800     GO TO    F-DAYOFINT-WRITE-02.                                IF1094.2
033900 F-DAYOFINT-WRITE-02.                                             IF1094.2
034000     MOVE "F-DAYOFINT-02" TO PAR-NAME.                            IF1094.2
034100     PERFORM  PRINT-DETAIL.                                       IF1094.2
034200*****************TEST (c) ******************************          IF1094.2
034300 F-DAYOFINT-TEST-03.                                              IF1094.2
034400     IF FUNCTION DAY-OF-INTEGER(IND(1)) = 1601004 THEN            IF1094.2
034500                        PERFORM PASS                              IF1094.2
034600     ELSE                                                         IF1094.2
034700                        PERFORM FAIL.                             IF1094.2
034800     GO TO F-DAYOFINT-WRITE-03.                                   IF1094.2
034900 F-DAYOFINT-DELETE-03.                                            IF1094.2
035000     PERFORM  DE-LETE.                                            IF1094.2
035100     GO TO    F-DAYOFINT-WRITE-03.                                IF1094.2
035200 F-DAYOFINT-WRITE-03.                                             IF1094.2
035300     MOVE "F-DAYOFINT-03" TO PAR-NAME.                            IF1094.2
035400     PERFORM  PRINT-DETAIL.                                       IF1094.2
035500*****************TEST (d) ******************************          IF1094.2
035600 F-DAYOFINT-04.                                                   IF1094.2
035700     MOVE ZERO TO WS-DATE.                                        IF1094.2
035800 F-DAYOFINT-TEST-04.                                              IF1094.2
035900     COMPUTE WS-DATE = FUNCTION DAY-OF-INTEGER(IND(D)).           IF1094.2
036000     IF WS-DATE = 1601004 THEN                                    IF1094.2
036100                        PERFORM PASS                              IF1094.2
036200     ELSE                                                         IF1094.2
036300                        MOVE  1601004  TO CORRECT-N               IF1094.2
036400                        MOVE WS-DATE TO COMPUTED-N                IF1094.2
036500                        PERFORM FAIL.                             IF1094.2
036600     GO TO F-DAYOFINT-WRITE-04.                                   IF1094.2
036700 F-DAYOFINT-DELETE-04.                                            IF1094.2
036800     PERFORM  DE-LETE.                                            IF1094.2
036900     GO TO    F-DAYOFINT-WRITE-04.                                IF1094.2
037000 F-DAYOFINT-WRITE-04.                                             IF1094.2
037100     MOVE "F-DAYOFINT-04" TO PAR-NAME.                            IF1094.2
037200     PERFORM  PRINT-DETAIL.                                       IF1094.2
037300*****************TEST (e) ******************************          IF1094.2
037400 F-DAYOFINT-05.                                                   IF1094.2
037500     MOVE ZERO TO WS-DATE.                                        IF1094.2
037600 F-DAYOFINT-TEST-05.                                              IF1094.2
037700     COMPUTE WS-DATE = FUNCTION DAY-OF-INTEGER(C).                IF1094.2
037800     IF WS-DATE = 1601365 THEN                                    IF1094.2
037900                        PERFORM PASS                              IF1094.2
038000     ELSE                                                         IF1094.2
038100                        MOVE  1601365  TO CORRECT-N               IF1094.2
038200                        MOVE WS-DATE TO COMPUTED-N                IF1094.2
038300                        PERFORM FAIL.                             IF1094.2
038400     GO TO F-DAYOFINT-WRITE-05.                                   IF1094.2
038500 F-DAYOFINT-DELETE-05.                                            IF1094.2
038600     PERFORM  DE-LETE.                                            IF1094.2
038700     GO TO    F-DAYOFINT-WRITE-05.                                IF1094.2
038800 F-DAYOFINT-WRITE-05.                                             IF1094.2
038900     MOVE "F-DAYOFINT-05" TO PAR-NAME.                            IF1094.2
039000     PERFORM  PRINT-DETAIL.                                       IF1094.2
039100*****************TEST (f) ******************************          IF1094.2
039200 F-DAYOFINT-06.                                                   IF1094.2
039300     MOVE ZERO TO WS-DATE.                                        IF1094.2
039400 F-DAYOFINT-TEST-06.                                              IF1094.2
039500     COMPUTE WS-DATE = FUNCTION DAY-OF-INTEGER(D) + 10.           IF1094.2
039600     IF WS-DATE = 1601011 THEN                                    IF1094.2
039700                        PERFORM PASS                              IF1094.2
039800     ELSE                                                         IF1094.2
039900                        MOVE  1601011  TO CORRECT-N               IF1094.2
040000                        MOVE WS-DATE TO COMPUTED-N                IF1094.2
040100                        PERFORM FAIL.                             IF1094.2
040200     GO TO F-DAYOFINT-WRITE-06.                                   IF1094.2
040300 F-DAYOFINT-DELETE-06.                                            IF1094.2
040400     PERFORM  DE-LETE.                                            IF1094.2
040500     GO TO    F-DAYOFINT-WRITE-06.                                IF1094.2
040600 F-DAYOFINT-WRITE-06.                                             IF1094.2
040700     MOVE "F-DAYOFINT-06" TO PAR-NAME.                            IF1094.2
040800     PERFORM  PRINT-DETAIL.                                       IF1094.2
040900*****************TEST (g) ******************************          IF1094.2
041000 F-DAYOFINT-07.                                                   IF1094.2
041100     MOVE ZERO TO WS-DATE.                                        IF1094.2
041200 F-DAYOFINT-TEST-07.                                              IF1094.2
041300     COMPUTE WS-DATE = FUNCTION DAY-OF-INTEGER(D) +               IF1094.2
041400                       FUNCTION DAY-OF-INTEGER(D).                IF1094.2
041500     IF WS-DATE = 3202002 THEN                                    IF1094.2
041600                        PERFORM PASS                              IF1094.2
041700     ELSE                                                         IF1094.2
041800                        MOVE  3202002  TO CORRECT-N               IF1094.2
041900                        MOVE WS-DATE TO COMPUTED-N                IF1094.2
042000                        PERFORM FAIL.                             IF1094.2
042100     GO TO F-DAYOFINT-WRITE-07.                                   IF1094.2
042200 F-DAYOFINT-DELETE-07.                                            IF1094.2
042300     PERFORM  DE-LETE.                                            IF1094.2
042400     GO TO    F-DAYOFINT-WRITE-07.                                IF1094.2
042500 F-DAYOFINT-WRITE-07.                                             IF1094.2
042600     MOVE "F-DAYOFINT-07" TO PAR-NAME.                            IF1094.2
042700     PERFORM  PRINT-DETAIL.                                       IF1094.2
042800***************** SPECIAL TEST 1 ***********************          IF1094.2
042900 F-DAYOFINT-08.                                                   IF1094.2
043000     MOVE 1 TO ARG1.                                              IF1094.2
043100     PERFORM F-DAYOFINT-TEST-08                                   IF1094.2
043200             UNTIL FUNCTION DAY-OF-INTEGER(ARG1) > 1601010.       IF1094.2
043300     IF ARG1 = 11 THEN                                            IF1094.2
043400                            PERFORM PASS                          IF1094.2
043500     ELSE                                                         IF1094.2
043600                            PERFORM FAIL.                         IF1094.2
043700     GO TO F-DAYOFINT-WRITE-08.                                   IF1094.2
043800*                                                                 IF1094.2
043900 F-DAYOFINT-TEST-08.                                              IF1094.2
044000     COMPUTE ARG1 = ARG1 + 1.                                     IF1094.2
044100*                                                                 IF1094.2
044200 F-DAYOFINT-DELETE-08.                                            IF1094.2
044300     PERFORM  DE-LETE.                                            IF1094.2
044400     GO TO    F-DAYOFINT-WRITE-08.                                IF1094.2
044500 F-DAYOFINT-WRITE-08.                                             IF1094.2
044600     MOVE "F-DAYOFINT-08" TO PAR-NAME.                            IF1094.2
044700     PERFORM  PRINT-DETAIL.                                       IF1094.2
044800*******************END OF TESTS**************************         IF1094.2
044900 CCVS-EXIT SECTION.                                               IF1094.2
045000 CCVS-999999.                                                     IF1094.2
045100     GO TO CLOSE-FILES.                                           IF1094.2
      *END-OF,IF109A                                                                  
