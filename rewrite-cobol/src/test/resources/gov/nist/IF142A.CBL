      *HEADER,COBOL,IF142A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1424.2
000200 PROGRAM-ID.                                                      IF1424.2
000300     IF142A.                                                      IF1424.2
000400                                                                  IF1424.2
000500***********************************************************       IF1424.2
000600*                                                         *       IF1424.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1424.2
000800* It contains tests for the Intrinsic Function            *       IF1424.2
000900* WHEN-COMPILED.                                          *       IF1424.2
001000*                                                         *       IF1424.2
001100***********************************************************       IF1424.2
001200 ENVIRONMENT DIVISION.                                            IF1424.2
001300 CONFIGURATION SECTION.                                           IF1424.2
001400 SOURCE-COMPUTER.                                                 IF1424.2
001500     XXXXX082.                                                    IF1424.2
001600 OBJECT-COMPUTER.                                                 IF1424.2
001700     XXXXX083.                                                    IF1424.2
001800 INPUT-OUTPUT SECTION.                                            IF1424.2
001900 FILE-CONTROL.                                                    IF1424.2
002000     SELECT PRINT-FILE ASSIGN TO                                  IF1424.2
002100     XXXXX055.                                                    IF1424.2
002200 DATA DIVISION.                                                   IF1424.2
002300 FILE SECTION.                                                    IF1424.2
002400 FD  PRINT-FILE.                                                  IF1424.2
002500 01  PRINT-REC PICTURE X(120).                                    IF1424.2
002600 01  DUMMY-RECORD PICTURE X(120).                                 IF1424.2
002700 WORKING-STORAGE SECTION.                                         IF1424.2
002800***********************************************************       IF1424.2
002900* Variables specific to the Intrinsic Function Test IF142A*       IF1424.2
003000***********************************************************       IF1424.2
003100 01  TEMP1                       PIC X(21).                       IF1424.2
003200 01  WS-DATE.                                                     IF1424.2
003300     02  WS-YEAR                 PIC 9999.                        IF1424.2
003400              88 CON-YEAR        VALUE 1990 THRU 9999.            IF1424.2
003500     02  WS-MONTH                PIC 99.                          IF1424.2
003600              88 CON-MONTH       VALUE 01 THRU 12.                IF1424.2
003700     02  WS-DAY                  PIC 99.                          IF1424.2
003800              88 CON-DAY         VALUE 01 THRU 31.                IF1424.2
003900     02  WS-HOUR                 PIC 99.                          IF1424.2
004000              88 CON-HOUR        VALUE 00 THRU 23.                IF1424.2
004100     02  WS-MIN                  PIC 99.                          IF1424.2
004200              88 CON-MIN         VALUE 00 THRU 59.                IF1424.2
004300     02  WS-SECOND               PIC 99.                          IF1424.2
004400              88 CON-SEC         VALUE 00 THRU 59.                IF1424.2
004500     02  WS-HUNDSEC              PIC 99.                          IF1424.2
004600              88 CON-HUNDSEC     VALUE 00 THRU 99.                IF1424.2
004700     02  WS-GREENW               PIC X.                           IF1424.2
004800              88 CON-GREENW      VALUE "-", "+", "0".             IF1424.2
004900     02  WS-OFFSET               PIC 99.                          IF1424.2
005000              88 CON-OFFSET      VALUE 00 THRU 13.                IF1424.2
005100*                                                                 IF1424.2
005200**********************************************************        IF1424.2
005300*                                                                 IF1424.2
005400 01  TEST-RESULTS.                                                IF1424.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IF1424.2
005600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1424.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IF1424.2
005800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1424.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IF1424.2
006000     02  PAR-NAME.                                                IF1424.2
006100       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1424.2
006200       03  PARDOT-X              PIC X      VALUE SPACE.          IF1424.2
006300       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1424.2
006400     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1424.2
006500     02 RE-MARK                  PIC X(61).                       IF1424.2
006600 01  TEST-COMPUTED.                                               IF1424.2
006700     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1424.2
006800     02 FILLER                   PIC X(17)  VALUE                 IF1424.2
006900            "       COMPUTED=".                                   IF1424.2
007000     02 COMPUTED-X.                                               IF1424.2
007100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1424.2
007200     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1424.2
007300                                 PIC -9(9).9(9).                  IF1424.2
007400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1424.2
007500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1424.2
007600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1424.2
007700     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1424.2
007800         04 COMPUTED-18V0                    PIC -9(18).          IF1424.2
007900         04 FILLER                           PIC X.               IF1424.2
008000     03 FILLER PIC X(50) VALUE SPACE.                             IF1424.2
008100 01  TEST-CORRECT.                                                IF1424.2
008200     02 FILLER PIC X(30) VALUE SPACE.                             IF1424.2
008300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1424.2
008400     02 CORRECT-X.                                                IF1424.2
008500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1424.2
008600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1424.2
008700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1424.2
008800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1424.2
008900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1424.2
009000     03      CR-18V0 REDEFINES CORRECT-A.                         IF1424.2
009100         04 CORRECT-18V0                     PIC -9(18).          IF1424.2
009200         04 FILLER                           PIC X.               IF1424.2
009300     03 FILLER PIC X(2) VALUE SPACE.                              IF1424.2
009400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1424.2
009500 01  CCVS-C-1.                                                    IF1424.2
009600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1424.2
009700-    "SS  PARAGRAPH-NAME                                          IF1424.2
009800-    "       REMARKS".                                            IF1424.2
009900     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1424.2
010000 01  CCVS-C-2.                                                    IF1424.2
010100     02 FILLER                     PIC X        VALUE SPACE.      IF1424.2
010200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1424.2
010300     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1424.2
010400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1424.2
010500     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1424.2
010600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1424.2
010700 01  REC-CT                        PIC 99       VALUE ZERO.       IF1424.2
010800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1424.2
010900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1424.2
011000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1424.2
011100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1424.2
011200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1424.2
011300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1424.2
011400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1424.2
011500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1424.2
011600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1424.2
011700 01  CCVS-H-1.                                                    IF1424.2
011800     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1424.2
011900     02  FILLER                    PIC X(42)    VALUE             IF1424.2
012000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1424.2
012100     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1424.2
012200 01  CCVS-H-2A.                                                   IF1424.2
012300   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1424.2
012400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1424.2
012500   02  FILLER                        PIC XXXX   VALUE             IF1424.2
012600     "4.2 ".                                                      IF1424.2
012700   02  FILLER                        PIC X(28)  VALUE             IF1424.2
012800            " COPY - NOT FOR DISTRIBUTION".                       IF1424.2
012900   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1424.2
013000                                                                  IF1424.2
013100 01  CCVS-H-2B.                                                   IF1424.2
013200   02  FILLER                        PIC X(15)  VALUE             IF1424.2
013300            "TEST RESULT OF ".                                    IF1424.2
013400   02  TEST-ID                       PIC X(9).                    IF1424.2
013500   02  FILLER                        PIC X(4)   VALUE             IF1424.2
013600            " IN ".                                               IF1424.2
013700   02  FILLER                        PIC X(12)  VALUE             IF1424.2
013800     " HIGH       ".                                              IF1424.2
013900   02  FILLER                        PIC X(22)  VALUE             IF1424.2
014000            " LEVEL VALIDATION FOR ".                             IF1424.2
014100   02  FILLER                        PIC X(58)  VALUE             IF1424.2
014200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1424.2
014300 01  CCVS-H-3.                                                    IF1424.2
014400     02  FILLER                      PIC X(34)  VALUE             IF1424.2
014500            " FOR OFFICIAL USE ONLY    ".                         IF1424.2
014600     02  FILLER                      PIC X(58)  VALUE             IF1424.2
014700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1424.2
014800     02  FILLER                      PIC X(28)  VALUE             IF1424.2
014900            "  COPYRIGHT   1985 ".                                IF1424.2
015000 01  CCVS-E-1.                                                    IF1424.2
015100     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1424.2
015200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1424.2
015300     02 ID-AGAIN                     PIC X(9).                    IF1424.2
015400     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1424.2
015500 01  CCVS-E-2.                                                    IF1424.2
015600     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1424.2
015700     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1424.2
015800     02 CCVS-E-2-2.                                               IF1424.2
015900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1424.2
016000         03 FILLER                   PIC X      VALUE SPACE.      IF1424.2
016100         03 ENDER-DESC               PIC X(44)  VALUE             IF1424.2
016200            "ERRORS ENCOUNTERED".                                 IF1424.2
016300 01  CCVS-E-3.                                                    IF1424.2
016400     02  FILLER                      PIC X(22)  VALUE             IF1424.2
016500            " FOR OFFICIAL USE ONLY".                             IF1424.2
016600     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1424.2
016700     02  FILLER                      PIC X(58)  VALUE             IF1424.2
016800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1424.2
016900     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1424.2
017000     02 FILLER                       PIC X(15)  VALUE             IF1424.2
017100             " COPYRIGHT 1985".                                   IF1424.2
017200 01  CCVS-E-4.                                                    IF1424.2
017300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1424.2
017400     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1424.2
017500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1424.2
017600     02 FILLER                       PIC X(40)  VALUE             IF1424.2
017700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1424.2
017800 01  XXINFO.                                                      IF1424.2
017900     02 FILLER                       PIC X(19)  VALUE             IF1424.2
018000            "*** INFORMATION ***".                                IF1424.2
018100     02 INFO-TEXT.                                                IF1424.2
018200       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1424.2
018300       04 XXCOMPUTED                 PIC X(20).                   IF1424.2
018400       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1424.2
018500       04 XXCORRECT                  PIC X(20).                   IF1424.2
018600     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1424.2
018700 01  HYPHEN-LINE.                                                 IF1424.2
018800     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1424.2
018900     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1424.2
019000-    "*****************************************".                 IF1424.2
019100     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1424.2
019200-    "******************************".                            IF1424.2
019300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1424.2
019400     "IF142A".                                                    IF1424.2
019500 PROCEDURE DIVISION.                                              IF1424.2
019600 CCVS1 SECTION.                                                   IF1424.2
019700 OPEN-FILES.                                                      IF1424.2
019800     OPEN     OUTPUT PRINT-FILE.                                  IF1424.2
019900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1424.2
020000     MOVE    SPACE TO TEST-RESULTS.                               IF1424.2
020100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1424.2
020200     GO TO CCVS1-EXIT.                                            IF1424.2
020300 CLOSE-FILES.                                                     IF1424.2
020400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1424.2
020500 TERMINATE-CCVS.                                                  IF1424.2
020600     STOP     RUN.                                                IF1424.2
020700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1424.2
020800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1424.2
020900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1424.2
021000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1424.2
021100     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1424.2
021200 PRINT-DETAIL.                                                    IF1424.2
021300     IF REC-CT NOT EQUAL TO ZERO                                  IF1424.2
021400             MOVE "." TO PARDOT-X                                 IF1424.2
021500             MOVE REC-CT TO DOTVALUE.                             IF1424.2
021600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1424.2
021700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1424.2
021800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1424.2
021900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1424.2
022000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1424.2
022100     MOVE SPACE TO CORRECT-X.                                     IF1424.2
022200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1424.2
022300     MOVE     SPACE TO RE-MARK.                                   IF1424.2
022400 HEAD-ROUTINE.                                                    IF1424.2
022500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1424.2
022600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1424.2
022700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1424.2
022800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1424.2
022900 COLUMN-NAMES-ROUTINE.                                            IF1424.2
023000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1424.2
023100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1424.2
023200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1424.2
023300 END-ROUTINE.                                                     IF1424.2
023400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5       IF1424.2
023500     TIMES.                                                       IF1424.2
023600 END-RTN-EXIT.                                                    IF1424.2
023700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1424.2
023800 END-ROUTINE-1.                                                   IF1424.2
023900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1424.2
024000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1424.2
024100      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1424.2
024200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1424.2
024300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1424.2
024400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1424.2
024500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1424.2
024600  END-ROUTINE-12.                                                 IF1424.2
024700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1424.2
024800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1424.2
024900         MOVE "NO " TO ERROR-TOTAL                                IF1424.2
025000         ELSE                                                     IF1424.2
025100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1424.2
025200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1424.2
025300     PERFORM WRITE-LINE.                                          IF1424.2
025400 END-ROUTINE-13.                                                  IF1424.2
025500     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1424.2
025600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1424.2
025700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1424.2
025800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1424.2
025900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1424.2
026000      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1424.2
026100          MOVE "NO " TO ERROR-TOTAL                               IF1424.2
026200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1424.2
026300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1424.2
026400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1424.2
026500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1424.2
026600 WRITE-LINE.                                                      IF1424.2
026700     ADD 1 TO RECORD-COUNT.                                       IF1424.2
026800*    IF RECORD-COUNT GREATER 42                                   IF1424.2
026900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1424.2
027000*        MOVE SPACE TO DUMMY-RECORD                               IF1424.2
027100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1424.2
027200*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1424.2
027300*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1424.2
027400*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1424.2
027500*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1424.2
027600*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1424.2
027700*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1424.2
027800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1424.2
027900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1424.2
028000*        MOVE ZERO TO RECORD-COUNT.                               IF1424.2
028100     PERFORM WRT-LN.                                              IF1424.2
028200 WRT-LN.                                                          IF1424.2
028300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1424.2
028400     MOVE SPACE TO DUMMY-RECORD.                                  IF1424.2
028500 BLANK-LINE-PRINT.                                                IF1424.2
028600     PERFORM WRT-LN.                                              IF1424.2
028700 FAIL-ROUTINE.                                                    IF1424.2
028800     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1424.2
028900            GO TO FAIL-ROUTINE-WRITE.                             IF1424.2
029000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1424.2
029100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1424.2
029200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1424.2
029300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1424.2
029400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1424.2
029500     GO TO  FAIL-ROUTINE-EX.                                      IF1424.2
029600 FAIL-ROUTINE-WRITE.                                              IF1424.2
029700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IF1424.2
029800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IF1424.2
029900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IF1424.2
030000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1424.2
030100 FAIL-ROUTINE-EX. EXIT.                                           IF1424.2
030200 BAIL-OUT.                                                        IF1424.2
030300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1424.2
030400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1424.2
030500 BAIL-OUT-WRITE.                                                  IF1424.2
030600     MOVE CORRECT-A TO XXCORRECT.                                 IF1424.2
030700     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1424.2
030800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1424.2
030900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1424.2
031000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1424.2
031100 BAIL-OUT-EX. EXIT.                                               IF1424.2
031200 CCVS1-EXIT.                                                      IF1424.2
031300     EXIT.                                                        IF1424.2
031400********************************************************          IF1424.2
031500*                                                      *          IF1424.2
031600*    Intrinsic Function Tests   IF142A - WHEN-COMPILED *          IF1424.2
031700*                                                      *          IF1424.2
031800********************************************************          IF1424.2
031900 SECT-IF142A SECTION.                                             IF1424.2
032000 F-WHENCOMP-INFO.                                                 IF1424.2
032100     MOVE     "See ref. A-75 2.46" TO ANSI-REFERENCE.             IF1424.2
032200     MOVE     "WHEN-COMPILED"     TO FEATURE.                     IF1424.2
032300*****************TEST (a) ******************************          IF1424.2
032400 F-WHENCOMP-01.                                                   IF1424.2
032500     MOVE SPACES TO TEMP1.                                        IF1424.2
032600     MOVE SPACES TO WS-DATE.                                      IF1424.2
032700 F-WHENCOMP-TEST-01.                                              IF1424.2
032800     MOVE FUNCTION WHEN-COMPILED TO TEMP1.                        IF1424.2
032900     MOVE TEMP1 TO WS-DATE.                                       IF1424.2
033000     IF CON-YEAR     AND                                          IF1424.2
033100        CON-MONTH    AND                                          IF1424.2
033200        CON-DAY      AND                                          IF1424.2
033300        CON-HOUR     AND                                          IF1424.2
033400        CON-MIN      AND                                          IF1424.2
033500        CON-SEC      AND                                          IF1424.2
033600        CON-HUNDSEC  AND                                          IF1424.2
033700        CON-GREENW   AND                                          IF1424.2
033800        CON-OFFSET   THEN                                         IF1424.2
033900                  PERFORM PASS                                    IF1424.2
034000     ELSE                                                         IF1424.2
034100                  MOVE TEMP1 TO COMPUTED-A                        IF1424.2
034200                  MOVE "Date & Time value " TO CORRECT-X          IF1424.2
034300                  PERFORM FAIL.                                   IF1424.2
034400     GO TO F-WHENCOMP-WRITE-01.                                   IF1424.2
034500 F-WHENCOMP-DELETE-01.                                            IF1424.2
034600     PERFORM  DE-LETE.                                            IF1424.2
034700     GO TO    F-WHENCOMP-WRITE-01.                                IF1424.2
034800 F-WHENCOMP-WRITE-01.                                             IF1424.2
034900     MOVE "F-WHENCOMP-01" TO PAR-NAME.                            IF1424.2
035000     PERFORM  PRINT-DETAIL.                                       IF1424.2
035100*****************TEST (b) ******************************          IF1424.2
035200 F-WHENCOMP-TEST-02.                                              IF1424.2
035300     IF FUNCTION WHEN-COMPILED >= TEMP1 THEN                      IF1424.2
035400                    PERFORM PASS                                  IF1424.2
035500     ELSE                                                         IF1424.2
035600                    PERFORM FAIL.                                 IF1424.2
035700     GO TO F-WHENCOMP-WRITE-02.                                   IF1424.2
035800 F-WHENCOMP-DELETE-02.                                            IF1424.2
035900     PERFORM  DE-LETE.                                            IF1424.2
036000     GO TO    F-WHENCOMP-WRITE-02.                                IF1424.2
036100 F-WHENCOMP-WRITE-02.                                             IF1424.2
036200     MOVE "F-WHENCOMP-02" TO PAR-NAME.                            IF1424.2
036300     PERFORM  PRINT-DETAIL.                                       IF1424.2
036400*******************END OF TESTS**************************         IF1424.2
036500 CCVS-EXIT SECTION.                                               IF1424.2
036600 CCVS-999999.                                                     IF1424.2
036700     GO TO CLOSE-FILES.                                           IF1424.2
      *END-OF,IF142A                                                                  
