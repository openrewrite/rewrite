      *HEADER,COBOL,IF107A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1074.2
000200 PROGRAM-ID.                                                      IF1074.2
000300     IF107A.                                                      IF1074.2
000400                                                                  IF1074.2
000500***********************************************************       IF1074.2
000600*                                                         *       IF1074.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1074.2
000800* It contains tests for the Intrinsic Function            *       IF1074.2
000900* CURRENT-DATE.                                           *       IF1074.2
001000*                                                         *       IF1074.2
001100***********************************************************       IF1074.2
001200 ENVIRONMENT DIVISION.                                            IF1074.2
001300 CONFIGURATION SECTION.                                           IF1074.2
001400 SOURCE-COMPUTER.                                                 IF1074.2
001500     XXXXX082.                                                    IF1074.2
001600 OBJECT-COMPUTER.                                                 IF1074.2
001700     XXXXX083.                                                    IF1074.2
001800 INPUT-OUTPUT SECTION.                                            IF1074.2
001900 FILE-CONTROL.                                                    IF1074.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1074.2
002100     XXXXX055.                                                    IF1074.2
002200 DATA DIVISION.                                                   IF1074.2
002300 FILE SECTION.                                                    IF1074.2
002400 FD  PRINT-FILE.                                                  IF1074.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1074.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1074.2
002700 WORKING-STORAGE SECTION.                                         IF1074.2
002800***********************************************************       IF1074.2
002900* Variables specific to the Intrinsic Function Test IF107A*       IF1074.2
003000***********************************************************       IF1074.2
003100 01  TEMP1                       PIC X(21).                       IF1074.2
003200 01  TEMP2                       PIC X(21).                       IF1074.2
003300 01  WS-FIRST                    VALUE SPACES.                    IF1074.2
003400     02  FILLER                  PIC X(8).                        IF1074.2
003500     02  WS-TIME1                PIC X(8).                        IF1074.2
003600     02  FILLER                  PIC X(5).                        IF1074.2
003700 01  WS-SECOND                   VALUE SPACES.                    IF1074.2
003800     02  FILLER                  PIC X(8).                        IF1074.2
003900     02  WS-TIME2                PIC X(8).                        IF1074.2
004000     02  FILLER                  PIC X(5).                        IF1074.2
004100 01  WS-DATE.                                                     IF1074.2
004200     02  WS-YEAR                 PIC 9999.                        IF1074.2
004300              88 CON-YEAR        VALUE 1990 THRU 9999.            IF1074.2
004400     02  WS-MONTH                PIC 99.                          IF1074.2
004500              88 CON-MONTH       VALUE 01 THRU 12.                IF1074.2
004600     02  WS-DAY                  PIC 99.                          IF1074.2
004700              88 CON-DAY         VALUE 01 THRU 31.                IF1074.2
004800     02  WS-HOUR                 PIC 99.                          IF1074.2
004900              88 CON-HOUR        VALUE 00 THRU 23.                IF1074.2
005000     02  WS-MIN                  PIC 99.                          IF1074.2
005100              88 CON-MIN         VALUE 00 THRU 59.                IF1074.2
005200     02  WS-SECOND               PIC 99.                          IF1074.2
005300              88 CON-SEC         VALUE 00 THRU 59.                IF1074.2
005400     02  WS-HUNDSEC              PIC 99.                          IF1074.2
005500              88 CON-HUNDSEC     VALUE 00 THRU 99.                IF1074.2
005600     02  WS-GREENW               PIC X.                           IF1074.2
005700              88 CON-GREENW      VALUE "-", "+", "0".             IF1074.2
005800     02  WS-OFFSET               PIC 99.                          IF1074.2
005900              88 CON-OFFSET      VALUE 00 THRU 13.                IF1074.2
006000     02  WS-OFFSET2              PIC 99.                          IF1074.2
006100              88 CON-OFFSET2     VALUE 00 THRU 59.                IF1074.2
006200*                                                                 IF1074.2
006300**********************************************************        IF1074.2
006400*                                                                 IF1074.2
006500 01  TEST-RESULTS.                                                IF1074.2
006600     02 FILLER                   PIC X      VALUE SPACE.          IF1074.2
006700     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1074.2
006800     02 FILLER                   PIC X      VALUE SPACE.          IF1074.2
006900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1074.2
007000     02 FILLER                   PIC X      VALUE SPACE.          IF1074.2
007100     02  PAR-NAME.                                                IF1074.2
007200       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1074.2
007300       03  PARDOT-X              PIC X      VALUE SPACE.          IF1074.2
007400       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1074.2
007500     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1074.2
007600     02 RE-MARK                  PIC X(61).                       IF1074.2
007700 01  TEST-COMPUTED.                                               IF1074.2
007800     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1074.2
007900     02 FILLER                   PIC X(17)  VALUE                 IF1074.2
008000            "       COMPUTED=".                                   IF1074.2
008100     02 COMPUTED-X.                                               IF1074.2
008200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1074.2
008300     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1074.2
008400                                 PIC -9(9).9(9).                  IF1074.2
008500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1074.2
008600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1074.2
008700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1074.2
008800     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1074.2
008900         04 COMPUTED-18V0                    PIC -9(18).          IF1074.2
009000         04 FILLER                           PIC X.               IF1074.2
009100     03 FILLER PIC X(50) VALUE SPACE.                             IF1074.2
009200 01  TEST-CORRECT.                                                IF1074.2
009300     02 FILLER PIC X(30) VALUE SPACE.                             IF1074.2
009400     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1074.2
009500     02 CORRECT-X.                                                IF1074.2
009600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1074.2
009700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1074.2
009800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1074.2
009900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1074.2
010000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1074.2
010100     03      CR-18V0 REDEFINES CORRECT-A.                         IF1074.2
010200         04 CORRECT-18V0                     PIC -9(18).          IF1074.2
010300         04 FILLER                           PIC X.               IF1074.2
010400     03 FILLER PIC X(2) VALUE SPACE.                              IF1074.2
010500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1074.2
010600 01  CCVS-C-1.                                                    IF1074.2
010700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1074.2
010800-    "SS  PARAGRAPH-NAME                                          IF1074.2
010900-    "       REMARKS".                                            IF1074.2
011000     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1074.2
011100 01  CCVS-C-2.                                                    IF1074.2
011200     02 FILLER                     PIC X        VALUE SPACE.      IF1074.2
011300     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1074.2
011400     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1074.2
011500     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1074.2
011600     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1074.2
011700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1074.2
011800 01  REC-CT                        PIC 99       VALUE ZERO.       IF1074.2
011900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1074.2
012000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1074.2
012100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1074.2
012200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1074.2
012300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1074.2
012400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1074.2
012500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1074.2
012600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1074.2
012700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1074.2
012800 01  CCVS-H-1.                                                    IF1074.2
012900     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1074.2
013000     02  FILLER                    PIC X(42)    VALUE             IF1074.2
013100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1074.2
013200     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1074.2
013300 01  CCVS-H-2A.                                                   IF1074.2
013400   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1074.2
013500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1074.2
013600   02  FILLER                        PIC XXXX   VALUE             IF1074.2
013700     "4.2 ".                                                      IF1074.2
013800   02  FILLER                        PIC X(28)  VALUE             IF1074.2
013900            " COPY - NOT FOR DISTRIBUTION".                       IF1074.2
014000   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1074.2
014100                                                                  IF1074.2
014200 01  CCVS-H-2B.                                                   IF1074.2
014300   02  FILLER                        PIC X(15)  VALUE             IF1074.2
014400            "TEST RESULT OF ".                                    IF1074.2
014500   02  TEST-ID                       PIC X(9).                    IF1074.2
014600   02  FILLER                        PIC X(4)   VALUE             IF1074.2
014700            " IN ".                                               IF1074.2
014800   02  FILLER                        PIC X(12)  VALUE             IF1074.2
014900     " HIGH       ".                                              IF1074.2
015000   02  FILLER                        PIC X(22)  VALUE             IF1074.2
015100            " LEVEL VALIDATION FOR ".                             IF1074.2
015200   02  FILLER                        PIC X(58)  VALUE             IF1074.2
015300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1074.2
015400 01  CCVS-H-3.                                                    IF1074.2
015500     02  FILLER                      PIC X(34)  VALUE             IF1074.2
015600            " FOR OFFICIAL USE ONLY    ".                         IF1074.2
015700     02  FILLER                      PIC X(58)  VALUE             IF1074.2
015800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1074.2
015900     02  FILLER                      PIC X(28)  VALUE             IF1074.2
016000            "  COPYRIGHT   1985 ".                                IF1074.2
016100 01  CCVS-E-1.                                                    IF1074.2
016200     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1074.2
016300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1074.2
016400     02 ID-AGAIN                     PIC X(9).                    IF1074.2
016500     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1074.2
016600 01  CCVS-E-2.                                                    IF1074.2
016700     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1074.2
016800     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1074.2
016900     02 CCVS-E-2-2.                                               IF1074.2
017000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1074.2
017100         03 FILLER                   PIC X      VALUE SPACE.      IF1074.2
017200         03 ENDER-DESC               PIC X(44)  VALUE             IF1074.2
017300            "ERRORS ENCOUNTERED".                                 IF1074.2
017400 01  CCVS-E-3.                                                    IF1074.2
017500     02  FILLER                      PIC X(22)  VALUE             IF1074.2
017600            " FOR OFFICIAL USE ONLY".                             IF1074.2
017700     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1074.2
017800     02  FILLER                      PIC X(58)  VALUE             IF1074.2
017900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1074.2
018000     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1074.2
018100     02 FILLER                       PIC X(15)  VALUE             IF1074.2
018200             " COPYRIGHT 1985".                                   IF1074.2
018300 01  CCVS-E-4.                                                    IF1074.2
018400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1074.2
018500     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1074.2
018600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1074.2
018700     02 FILLER                       PIC X(40)  VALUE             IF1074.2
018800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1074.2
018900 01  XXINFO.                                                      IF1074.2
019000     02 FILLER                       PIC X(19)  VALUE             IF1074.2
019100            "*** INFORMATION ***".                                IF1074.2
019200     02 INFO-TEXT.                                                IF1074.2
019300       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1074.2
019400       04 XXCOMPUTED                 PIC X(20).                   IF1074.2
019500       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1074.2
019600       04 XXCORRECT                  PIC X(20).                   IF1074.2
019700     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1074.2
019800 01  HYPHEN-LINE.                                                 IF1074.2
019900     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1074.2
020000     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1074.2
020100-    "*****************************************".                 IF1074.2
020200     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1074.2
020300-    "******************************".                            IF1074.2
020400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1074.2
020500     "IF107A".                                                    IF1074.2
020600 PROCEDURE DIVISION.                                              IF1074.2
020700 CCVS1 SECTION.                                                   IF1074.2
020800 OPEN-FILES.                                                      IF1074.2
020900     OPEN     OUTPUT PRINT-FILE.                                  IF1074.2
021000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1074.2
021100     MOVE    SPACE TO TEST-RESULTS.                               IF1074.2
021200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1074.2
021300     GO TO CCVS1-EXIT.                                            IF1074.2
021400 CLOSE-FILES.                                                     IF1074.2
021500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1074.2
021600 TERMINATE-CCVS.                                                  IF1074.2
021700     STOP     RUN.                                                IF1074.2
021800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1074.2
021900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1074.2
022000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1074.2
022100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1074.2
022200     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1074.2
022300 PRINT-DETAIL.                                                    IF1074.2
022400     IF REC-CT NOT EQUAL TO ZERO                                  IF1074.2
022500             MOVE "." TO PARDOT-X                                 IF1074.2
022600             MOVE REC-CT TO DOTVALUE.                             IF1074.2
022700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1074.2
022800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1074.2
022900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1074.2
023000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1074.2
023100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1074.2
023200     MOVE SPACE TO CORRECT-X.                                     IF1074.2
023300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1074.2
023400     MOVE     SPACE TO RE-MARK.                                   IF1074.2
023500 HEAD-ROUTINE.                                                    IF1074.2
023600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1074.2
023700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1074.2
023800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1074.2
023900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1074.2
024000 COLUMN-NAMES-ROUTINE.                                            IF1074.2
024100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1074.2
024200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1074.2
024300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1074.2
024400 END-ROUTINE.                                                     IF1074.2
024500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5       IF1074.2
024600     TIMES.                                                       IF1074.2
024700 END-RTN-EXIT.                                                    IF1074.2
024800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1074.2
024900 END-ROUTINE-1.                                                   IF1074.2
025000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1074.2
025100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1074.2
025200      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1074.2
025300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1074.2
025400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1074.2
025500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1074.2
025600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1074.2
025700  END-ROUTINE-12.                                                 IF1074.2
025800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1074.2
025900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1074.2
026000         MOVE "NO " TO ERROR-TOTAL                                IF1074.2
026100         ELSE                                                     IF1074.2
026200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1074.2
026300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1074.2
026400     PERFORM WRITE-LINE.                                          IF1074.2
026500 END-ROUTINE-13.                                                  IF1074.2
026600     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1074.2
026700         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1074.2
026800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1074.2
026900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1074.2
027000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1074.2
027100      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1074.2
027200          MOVE "NO " TO ERROR-TOTAL                               IF1074.2
027300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1074.2
027400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1074.2
027500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1074.2
027600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1074.2
027700 WRITE-LINE.                                                      IF1074.2
027800     ADD 1 TO RECORD-COUNT.                                       IF1074.2
027900*    IF RECORD-COUNT GREATER 42                                   IF1074.2
028000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1074.2
028100*        MOVE SPACE TO DUMMY-RECORD                               IF1074.2
028200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1074.2
028300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1074.2
028400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1074.2
028500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1074.2
028600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1074.2
028700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1074.2
028800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1074.2
028900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1074.2
029000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1074.2
029100*        MOVE ZERO TO RECORD-COUNT.                               IF1074.2
029200     PERFORM WRT-LN.                                              IF1074.2
029300 WRT-LN.                                                          IF1074.2
029400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1074.2
029500     MOVE SPACE TO DUMMY-RECORD.                                  IF1074.2
029600 BLANK-LINE-PRINT.                                                IF1074.2
029700     PERFORM WRT-LN.                                              IF1074.2
029800 FAIL-ROUTINE.                                                    IF1074.2
029900     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1074.2
030000            GO TO FAIL-ROUTINE-WRITE.                             IF1074.2
030100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1074.2
030200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1074.2
030300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1074.2
030400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1074.2
030500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1074.2
030600     GO TO  FAIL-ROUTINE-EX.                                      IF1074.2
030700 FAIL-ROUTINE-WRITE.                                              IF1074.2
030800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IF1074.2
030900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IF1074.2
031000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IF1074.2
031100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1074.2
031200 FAIL-ROUTINE-EX. EXIT.                                           IF1074.2
031300 BAIL-OUT.                                                        IF1074.2
031400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1074.2
031500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1074.2
031600 BAIL-OUT-WRITE.                                                  IF1074.2
031700     MOVE CORRECT-A TO XXCORRECT.                                 IF1074.2
031800     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1074.2
031900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1074.2
032000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1074.2
032100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1074.2
032200 BAIL-OUT-EX. EXIT.                                               IF1074.2
032300 CCVS1-EXIT.                                                      IF1074.2
032400     EXIT.                                                        IF1074.2
032500********************************************************          IF1074.2
032600*                                                      *          IF1074.2
032700*    Intrinsic Function Tests   IF107A - CURRENT-DATE *           IF1074.2
032800*                                                      *          IF1074.2
032900********************************************************          IF1074.2
033000 SECT-IF107A SECTION.                                             IF1074.2
033100 F-WHENCOMP-INFO.                                                 IF1074.2
033200     MOVE     "See ref. A-39 2.11" TO ANSI-REFERENCE.             IF1074.2
033300     MOVE     "CURRENT-DATE"     TO FEATURE.                      IF1074.2
033400*****************TEST (a) ******************************          IF1074.2
033500 F-WHENCOMP-01.                                                   IF1074.2
033600     MOVE SPACES TO TEMP1.                                        IF1074.2
033700     MOVE SPACES TO WS-DATE.                                      IF1074.2
033800 F-WHENCOMP-TEST-01.                                              IF1074.2
033900     MOVE FUNCTION CURRENT-DATE TO TEMP1.                         IF1074.2
034000     MOVE TEMP1 TO WS-DATE.                                       IF1074.2
034100     IF CON-YEAR     AND                                          IF1074.2
034200        CON-MONTH    AND                                          IF1074.2
034300        CON-DAY      AND                                          IF1074.2
034400        CON-HOUR     AND                                          IF1074.2
034500        CON-MIN      AND                                          IF1074.2
034600        CON-SEC      AND                                          IF1074.2
034700        CON-HUNDSEC  AND                                          IF1074.2
034800        CON-GREENW   AND                                          IF1074.2
034900        CON-OFFSET   AND                                          IF1074.2
035000        CON-OFFSET2  THEN                                         IF1074.2
035100                  PERFORM PASS                                    IF1074.2
035200     ELSE                                                         IF1074.2
035300                  MOVE TEMP1 TO COMPUTED-A                        IF1074.2
035400                  MOVE "Date & Time value " TO CORRECT-X          IF1074.2
035500                  PERFORM FAIL.                                   IF1074.2
035600     GO TO F-WHENCOMP-WRITE-01.                                   IF1074.2
035700 F-WHENCOMP-DELETE-01.                                            IF1074.2
035800     PERFORM  DE-LETE.                                            IF1074.2
035900     GO TO    F-WHENCOMP-WRITE-01.                                IF1074.2
036000 F-WHENCOMP-WRITE-01.                                             IF1074.2
036100     MOVE "F-WHENCOMP-01" TO PAR-NAME.                            IF1074.2
036200     PERFORM  PRINT-DETAIL.                                       IF1074.2
036300*****************TEST (b) ******************************          IF1074.2
036400 F-WHENCOMP-TEST-02.                                              IF1074.2
036500     IF FUNCTION CURRENT-DATE >= TEMP1 THEN                       IF1074.2
036600                    PERFORM PASS                                  IF1074.2
036700     ELSE                                                         IF1074.2
036800                    PERFORM FAIL.                                 IF1074.2
036900     GO TO F-WHENCOMP-WRITE-02.                                   IF1074.2
037000 F-WHENCOMP-DELETE-02.                                            IF1074.2
037100     PERFORM  DE-LETE.                                            IF1074.2
037200     GO TO    F-WHENCOMP-WRITE-02.                                IF1074.2
037300 F-WHENCOMP-WRITE-02.                                             IF1074.2
037400     MOVE "F-WHENCOMP-02" TO PAR-NAME.                            IF1074.2
037500     PERFORM  PRINT-DETAIL.                                       IF1074.2
037600*******************END OF TESTS**************************         IF1074.2
037700 CCVS-EXIT SECTION.                                               IF1074.2
037800 CCVS-999999.                                                     IF1074.2
037900     GO TO CLOSE-FILES.                                           IF1074.2
      *END-OF,IF107A                                                                  
