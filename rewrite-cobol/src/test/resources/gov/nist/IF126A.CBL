      *HEADER,COBOL,IF126A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1264.2
000200 PROGRAM-ID.                                                      IF1264.2
000300     IF126A.                                                      IF1264.2
000400                                                                  IF1264.2
000500***********************************************************       IF1264.2
000600*                                                         *       IF1264.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1264.2
000800* It contains tests for the Intrinsic Function            *       IF1264.2
000900* NUMVAL-C.                                               *       IF1264.2
001000*                                                         *       IF1264.2
001100*                                                         *       IF1264.2
001200***********************************************************       IF1264.2
001300 ENVIRONMENT DIVISION.                                            IF1264.2
001400 CONFIGURATION SECTION.                                           IF1264.2
001500 SOURCE-COMPUTER.                                                 IF1264.2
001600     XXXXX082.                                                    IF1264.2
001700 OBJECT-COMPUTER.                                                 IF1264.2
001800     XXXXX083.                                                    IF1264.2
001900 INPUT-OUTPUT SECTION.                                            IF1264.2
002000 FILE-CONTROL.                                                    IF1264.2
002100     SELECT PRINT-FILE ASSIGN TO                                  IF1264.2
002200     XXXXX055.                                                    IF1264.2
002300 DATA DIVISION.                                                   IF1264.2
002400 FILE SECTION.                                                    IF1264.2
002500 FD  PRINT-FILE.                                                  IF1264.2
002600 01  PRINT-REC PICTURE X(120).                                    IF1264.2
002700 01  DUMMY-RECORD PICTURE X(120).                                 IF1264.2
002800 WORKING-STORAGE SECTION.                                         IF1264.2
002900***********************************************************       IF1264.2
003000* Variables specific to the Intrinsic Function Test IF126A*       IF1264.2
003100***********************************************************       IF1264.2
003200 01  A                   PIC X(1)       VALUE "4".                IF1264.2
003300 01  B                   PIC X(5)       VALUE "203".              IF1264.2
003400 01  C                   PIC X(4)       VALUE ".429".             IF1264.2
003500 01  D                   PIC X(7)       VALUE "928.344".          IF1264.2
003600 01  E                   PIC X(9)       VALUE "-042.3240".        IF1264.2
003700 01  F                   PIC X(7)       VALUE " 23.000".          IF1264.2
003800 01  G                   PIC X(8)       VALUE "-92924.3".         IF1264.2
003900 01  H                   PIC X(6)       VALUE "93.21+".           IF1264.2
004000 01  I                   PIC X(9)       VALUE " 92.92  -".        IF1264.2
004100 01  J                   PIC X(9)       VALUE "8,848.934".        IF1264.2
004200 01  K                   PIC X(12)      VALUE "4,825,293.92".     IF1264.2
004300 01  L                   PIC X(12)      VALUE " - 5,555.55 ".     IF1264.2
004400 01  M                   PIC X(9)       VALUE "5,555.55-".        IF1264.2
004500 01  N                   PIC X(13)      VALUE " 77,777.77 + ".    IF1264.2
004600 01  O                   PIC X(3)       VALUE "$33".              IF1264.2
004700 01  P                   PIC X(5)       VALUE "$0.11".            IF1264.2
004800 01  Q                   PIC X(9)       VALUE "$4,000.00".        IF1264.2
004900 01  R                   PIC X(14)      VALUE "$1,000,000.50".    IF1264.2
005000 01  S                   PIC X(14)       VALUE " $ 3,900.21".     IF1264.2
005100 01  T                   PIC X(14)       VALUE " + $ 9,000.99".   IF1264.2
005200 01  U                   PIC X(15)       VALUE " $ 3,890.20 + ".  IF1264.2
005300 01  TEMP                PIC S9(7)V9(5).                          IF1264.2
005400                                                                  IF1264.2
005500*                                                                 IF1264.2
005600**********************************************************        IF1264.2
005700*                                                                 IF1264.2
005800 01  TEST-RESULTS.                                                IF1264.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IF1264.2
006000     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1264.2
006100     02 FILLER                   PIC X      VALUE SPACE.          IF1264.2
006200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1264.2
006300     02 FILLER                   PIC X      VALUE SPACE.          IF1264.2
006400     02  PAR-NAME.                                                IF1264.2
006500       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1264.2
006600       03  PARDOT-X              PIC X      VALUE SPACE.          IF1264.2
006700       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1264.2
006800     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1264.2
006900     02 RE-MARK                  PIC X(61).                       IF1264.2
007000 01  TEST-COMPUTED.                                               IF1264.2
007100     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1264.2
007200     02 FILLER                   PIC X(17)  VALUE                 IF1264.2
007300            "       COMPUTED=".                                   IF1264.2
007400     02 COMPUTED-X.                                               IF1264.2
007500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1264.2
007600     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1264.2
007700                                 PIC -9(9).9(9).                  IF1264.2
007800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1264.2
007900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1264.2
008000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1264.2
008100     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1264.2
008200         04 COMPUTED-18V0                    PIC -9(18).          IF1264.2
008300         04 FILLER                           PIC X.               IF1264.2
008400     03 FILLER PIC X(50) VALUE SPACE.                             IF1264.2
008500 01  TEST-CORRECT.                                                IF1264.2
008600     02 FILLER PIC X(30) VALUE SPACE.                             IF1264.2
008700     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1264.2
008800     02 CORRECT-X.                                                IF1264.2
008900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1264.2
009000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1264.2
009100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1264.2
009200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1264.2
009300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1264.2
009400     03      CR-18V0 REDEFINES CORRECT-A.                         IF1264.2
009500         04 CORRECT-18V0                     PIC -9(18).          IF1264.2
009600         04 FILLER                           PIC X.               IF1264.2
009700     03 FILLER PIC X(2) VALUE SPACE.                              IF1264.2
009800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1264.2
009900 01  CCVS-C-1.                                                    IF1264.2
010000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1264.2
010100-    "SS  PARAGRAPH-NAME                                          IF1264.2
010200-    "       REMARKS".                                            IF1264.2
010300     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1264.2
010400 01  CCVS-C-2.                                                    IF1264.2
010500     02 FILLER                     PIC X        VALUE SPACE.      IF1264.2
010600     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1264.2
010700     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1264.2
010800     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1264.2
010900     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1264.2
011000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1264.2
011100 01  REC-CT                        PIC 99       VALUE ZERO.       IF1264.2
011200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1264.2
011300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1264.2
011400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1264.2
011500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1264.2
011600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1264.2
011700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1264.2
011800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1264.2
011900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1264.2
012000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1264.2
012100 01  CCVS-H-1.                                                    IF1264.2
012200     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1264.2
012300     02  FILLER                    PIC X(42)    VALUE             IF1264.2
012400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1264.2
012500     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1264.2
012600 01  CCVS-H-2A.                                                   IF1264.2
012700   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1264.2
012800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1264.2
012900   02  FILLER                        PIC XXXX   VALUE             IF1264.2
013000     "4.2 ".                                                      IF1264.2
013100   02  FILLER                        PIC X(28)  VALUE             IF1264.2
013200            " COPY - NOT FOR DISTRIBUTION".                       IF1264.2
013300   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1264.2
013400                                                                  IF1264.2
013500 01  CCVS-H-2B.                                                   IF1264.2
013600   02  FILLER                        PIC X(15)  VALUE             IF1264.2
013700            "TEST RESULT OF ".                                    IF1264.2
013800   02  TEST-ID                       PIC X(9).                    IF1264.2
013900   02  FILLER                        PIC X(4)   VALUE             IF1264.2
014000            " IN ".                                               IF1264.2
014100   02  FILLER                        PIC X(12)  VALUE             IF1264.2
014200     " HIGH       ".                                              IF1264.2
014300   02  FILLER                        PIC X(22)  VALUE             IF1264.2
014400            " LEVEL VALIDATION FOR ".                             IF1264.2
014500   02  FILLER                        PIC X(58)  VALUE             IF1264.2
014600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1264.2
014700 01  CCVS-H-3.                                                    IF1264.2
014800     02  FILLER                      PIC X(34)  VALUE             IF1264.2
014900            " FOR OFFICIAL USE ONLY    ".                         IF1264.2
015000     02  FILLER                      PIC X(58)  VALUE             IF1264.2
015100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1264.2
015200     02  FILLER                      PIC X(28)  VALUE             IF1264.2
015300            "  COPYRIGHT   1985 ".                                IF1264.2
015400 01  CCVS-E-1.                                                    IF1264.2
015500     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1264.2
015600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1264.2
015700     02 ID-AGAIN                     PIC X(9).                    IF1264.2
015800     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1264.2
015900 01  CCVS-E-2.                                                    IF1264.2
016000     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1264.2
016100     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1264.2
016200     02 CCVS-E-2-2.                                               IF1264.2
016300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1264.2
016400         03 FILLER                   PIC X      VALUE SPACE.      IF1264.2
016500         03 ENDER-DESC               PIC X(44)  VALUE             IF1264.2
016600            "ERRORS ENCOUNTERED".                                 IF1264.2
016700 01  CCVS-E-3.                                                    IF1264.2
016800     02  FILLER                      PIC X(22)  VALUE             IF1264.2
016900            " FOR OFFICIAL USE ONLY".                             IF1264.2
017000     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1264.2
017100     02  FILLER                      PIC X(58)  VALUE             IF1264.2
017200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1264.2
017300     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1264.2
017400     02 FILLER                       PIC X(15)  VALUE             IF1264.2
017500             " COPYRIGHT 1985".                                   IF1264.2
017600 01  CCVS-E-4.                                                    IF1264.2
017700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1264.2
017800     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1264.2
017900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1264.2
018000     02 FILLER                       PIC X(40)  VALUE             IF1264.2
018100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1264.2
018200 01  XXINFO.                                                      IF1264.2
018300     02 FILLER                       PIC X(19)  VALUE             IF1264.2
018400            "*** INFORMATION ***".                                IF1264.2
018500     02 INFO-TEXT.                                                IF1264.2
018600       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1264.2
018700       04 XXCOMPUTED                 PIC X(20).                   IF1264.2
018800       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1264.2
018900       04 XXCORRECT                  PIC X(20).                   IF1264.2
019000     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1264.2
019100 01  HYPHEN-LINE.                                                 IF1264.2
019200     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1264.2
019300     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1264.2
019400-    "*****************************************".                 IF1264.2
019500     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1264.2
019600-    "******************************".                            IF1264.2
019700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1264.2
019800     "IF126A".                                                    IF1264.2
019900 PROCEDURE DIVISION.                                              IF1264.2
020000 CCVS1 SECTION.                                                   IF1264.2
020100 OPEN-FILES.                                                      IF1264.2
020200     OPEN     OUTPUT PRINT-FILE.                                  IF1264.2
020300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1264.2
020400     MOVE    SPACE TO TEST-RESULTS.                               IF1264.2
020500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1264.2
020600     GO TO CCVS1-EXIT.                                            IF1264.2
020700 CLOSE-FILES.                                                     IF1264.2
020800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1264.2
020900 TERMINATE-CCVS.                                                  IF1264.2
021000     STOP     RUN.                                                IF1264.2
021100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1264.2
021200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1264.2
021300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1264.2
021400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1264.2
021500     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1264.2
021600 PRINT-DETAIL.                                                    IF1264.2
021700     IF REC-CT NOT EQUAL TO ZERO                                  IF1264.2
021800             MOVE "." TO PARDOT-X                                 IF1264.2
021900             MOVE REC-CT TO DOTVALUE.                             IF1264.2
022000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1264.2
022100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1264.2
022200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1264.2
022300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1264.2
022400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1264.2
022500     MOVE SPACE TO CORRECT-X.                                     IF1264.2
022600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1264.2
022700     MOVE     SPACE TO RE-MARK.                                   IF1264.2
022800 HEAD-ROUTINE.                                                    IF1264.2
022900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1264.2
023000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1264.2
023100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1264.2
023200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1264.2
023300 COLUMN-NAMES-ROUTINE.                                            IF1264.2
023400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1264.2
023500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1264.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1264.2
023700 END-ROUTINE.                                                     IF1264.2
023800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1264.2
023900 END-RTN-EXIT.                                                    IF1264.2
024000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1264.2
024100 END-ROUTINE-1.                                                   IF1264.2
024200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1264.2
024300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1264.2
024400      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1264.2
024500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1264.2
024600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1264.2
024700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1264.2
024800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1264.2
024900  END-ROUTINE-12.                                                 IF1264.2
025000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1264.2
025100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1264.2
025200         MOVE "NO " TO ERROR-TOTAL                                IF1264.2
025300         ELSE                                                     IF1264.2
025400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1264.2
025500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1264.2
025600     PERFORM WRITE-LINE.                                          IF1264.2
025700 END-ROUTINE-13.                                                  IF1264.2
025800     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1264.2
025900         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1264.2
026000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1264.2
026100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1264.2
026200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1264.2
026300      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1264.2
026400          MOVE "NO " TO ERROR-TOTAL                               IF1264.2
026500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1264.2
026600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1264.2
026700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1264.2
026800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1264.2
026900 WRITE-LINE.                                                      IF1264.2
027000     ADD 1 TO RECORD-COUNT.                                       IF1264.2
027100*    IF RECORD-COUNT GREATER 42                                   IF1264.2
027200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1264.2
027300*        MOVE SPACE TO DUMMY-RECORD                               IF1264.2
027400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1264.2
027500*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1264.2
027600*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1264.2
027700*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1264.2
027800*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1264.2
027900*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1264.2
028000*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1264.2
028100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1264.2
028200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1264.2
028300*        MOVE ZERO TO RECORD-COUNT.                               IF1264.2
028400     PERFORM WRT-LN.                                              IF1264.2
028500 WRT-LN.                                                          IF1264.2
028600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1264.2
028700     MOVE SPACE TO DUMMY-RECORD.                                  IF1264.2
028800 BLANK-LINE-PRINT.                                                IF1264.2
028900     PERFORM WRT-LN.                                              IF1264.2
029000 FAIL-ROUTINE.                                                    IF1264.2
029100     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1264.2
029200            GO TO FAIL-ROUTINE-WRITE.                             IF1264.2
029300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1264.2
029400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1264.2
029500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1264.2
029600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1264.2
029700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1264.2
029800     GO TO  FAIL-ROUTINE-EX.                                      IF1264.2
029900 FAIL-ROUTINE-WRITE.                                              IF1264.2
030000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IF1264.2
030100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IF1264.2
030200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IF1264.2
030300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1264.2
030400 FAIL-ROUTINE-EX. EXIT.                                           IF1264.2
030500 BAIL-OUT.                                                        IF1264.2
030600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1264.2
030700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1264.2
030800 BAIL-OUT-WRITE.                                                  IF1264.2
030900     MOVE CORRECT-A TO XXCORRECT.                                 IF1264.2
031000     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1264.2
031100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1264.2
031200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1264.2
031300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1264.2
031400 BAIL-OUT-EX. EXIT.                                               IF1264.2
031500 CCVS1-EXIT.                                                      IF1264.2
031600     EXIT.                                                        IF1264.2
031700********************************************************          IF1264.2
031800*                                                      *          IF1264.2
031900*    Intrinsic Function Tests       IF126A - NUMVAL-C  *          IF1264.2
032000*                                                      *          IF1264.2
032100********************************************************          IF1264.2
032200 SECT-IF126A SECTION.                                             IF1264.2
032300 F-NUMVALC-INFO.                                                  IF1264.2
032400     MOVE     "See ref. A-59 2.30" TO ANSI-REFERENCE.             IF1264.2
032500     MOVE     "NUMVAL-C Function"     TO FEATURE.                 IF1264.2
032600*****************TEST (a) ******************************          IF1264.2
032700 F-NUMVALC-01.                                                    IF1264.2
032800     MOVE ZERO TO TEMP.                                           IF1264.2
032900 F-NUMVALC-TEST-01.                                               IF1264.2
033000     COMPUTE TEMP = FUNCTION NUMVAL-C ("9").                      IF1264.2
033100     IF TEMP = 9 THEN                                             IF1264.2
033200                        PERFORM PASS                              IF1264.2
033300     ELSE                                                         IF1264.2
033400                        MOVE 9 TO CORRECT-N                       IF1264.2
033500                        MOVE TEMP TO COMPUTED-N                   IF1264.2
033600                        PERFORM FAIL.                             IF1264.2
033700     GO TO F-NUMVALC-WRITE-01.                                    IF1264.2
033800 F-NUMVALC-DELETE-01.                                             IF1264.2
033900     PERFORM  DE-LETE.                                            IF1264.2
034000     GO TO    F-NUMVALC-WRITE-01.                                 IF1264.2
034100 F-NUMVALC-WRITE-01.                                              IF1264.2
034200     MOVE "F-NUMVALC-01" TO PAR-NAME.                             IF1264.2
034300     PERFORM  PRINT-DETAIL.                                       IF1264.2
034400*****************TEST (b) *******************************         IF1264.2
034500 F-NUMVALC-02.                                                    IF1264.2
034600     MOVE 77  TO TEMP.                                            IF1264.2
034700 F-NUMVALC-TEST-02.                                               IF1264.2
034800     EVALUATE FUNCTION NUMVAL-C (B)                               IF1264.2
034900              ALSO ( TEMP + 96 ) * 2                              IF1264.2
035000     WHEN     203                                                 IF1264.2
035100              ALSO 346                                            IF1264.2
035200        PERFORM PASS                                              IF1264.2
035300        GO TO F-NUMVALC-WRITE-02.                                 IF1264.2
035400     PERFORM FAIL.                                                IF1264.2
035500     GO TO    F-NUMVALC-WRITE-02.                                 IF1264.2
035600 F-NUMVALC-DELETE-02.                                             IF1264.2
035700     PERFORM  DE-LETE.                                            IF1264.2
035800     GO TO    F-NUMVALC-WRITE-02.                                 IF1264.2
035900 F-NUMVALC-WRITE-02.                                              IF1264.2
036000     MOVE "F-NUMVALC-02" TO PAR-NAME.                             IF1264.2
036100     PERFORM  PRINT-DETAIL.                                       IF1264.2
036200*****************TEST (c) ******************************          IF1264.2
036300 F-NUMVALC-TEST-03.                                               IF1264.2
036400     IF FUNCTION NUMVAL-C ("92,483") = 92483 THEN                 IF1264.2
036500                        PERFORM PASS                              IF1264.2
036600     ELSE                                                         IF1264.2
036700                        PERFORM FAIL.                             IF1264.2
036800     GO TO F-NUMVALC-WRITE-03.                                    IF1264.2
036900 F-NUMVALC-DELETE-03.                                             IF1264.2
037000     PERFORM  DE-LETE.                                            IF1264.2
037100     GO TO    F-NUMVALC-WRITE-03.                                 IF1264.2
037200 F-NUMVALC-WRITE-03.                                              IF1264.2
037300     MOVE "F-NUMVALC-03" TO PAR-NAME.                             IF1264.2
037400     PERFORM  PRINT-DETAIL.                                       IF1264.2
037500*****************TEST (d) ******************************          IF1264.2
037600 F-NUMVALC-04.                                                    IF1264.2
037700     MOVE ZERO TO TEMP.                                           IF1264.2
037800 F-NUMVALC-TEST-04.                                               IF1264.2
037900     COMPUTE TEMP = FUNCTION NUMVAL-C (C).                        IF1264.2
038000     IF (TEMP >= 0.428991) AND                                    IF1264.2
038100        (TEMP <= 0.429009)                                        IF1264.2
038200                        PERFORM PASS                              IF1264.2
038300     ELSE                                                         IF1264.2
038400                        MOVE  0.429  TO CORRECT-N                 IF1264.2
038500                        MOVE TEMP TO COMPUTED-N                   IF1264.2
038600                        PERFORM FAIL.                             IF1264.2
038700     GO TO F-NUMVALC-WRITE-04.                                    IF1264.2
038800 F-NUMVALC-DELETE-04.                                             IF1264.2
038900     PERFORM  DE-LETE.                                            IF1264.2
039000     GO TO    F-NUMVALC-WRITE-04.                                 IF1264.2
039100 F-NUMVALC-WRITE-04.                                              IF1264.2
039200     MOVE "F-NUMVALC-04" TO PAR-NAME.                             IF1264.2
039300     PERFORM  PRINT-DETAIL.                                       IF1264.2
039400*****************TEST (e) ******************************          IF1264.2
039500 F-NUMVALC-05.                                                    IF1264.2
039600     MOVE ZERO TO TEMP.                                           IF1264.2
039700 F-NUMVALC-TEST-05.                                               IF1264.2
039800     COMPUTE TEMP = FUNCTION NUMVAL-C ("385.93").                 IF1264.2
039900     IF (TEMP >= 385.922) AND                                     IF1264.2
040000        (TEMP <= 385.938)                                         IF1264.2
040100                        PERFORM PASS                              IF1264.2
040200     ELSE                                                         IF1264.2
040300                        MOVE  385.93  TO CORRECT-N                IF1264.2
040400                        MOVE TEMP TO COMPUTED-N                   IF1264.2
040500                        PERFORM FAIL.                             IF1264.2
040600     GO TO F-NUMVALC-WRITE-05.                                    IF1264.2
040700 F-NUMVALC-DELETE-05.                                             IF1264.2
040800     PERFORM  DE-LETE.                                            IF1264.2
040900     GO TO    F-NUMVALC-WRITE-05.                                 IF1264.2
041000 F-NUMVALC-WRITE-05.                                              IF1264.2
041100     MOVE "F-NUMVALC-05" TO PAR-NAME.                             IF1264.2
041200     PERFORM  PRINT-DETAIL.                                       IF1264.2
041300*****************TEST (f) ******************************          IF1264.2
041400 F-NUMVALC-06.                                                    IF1264.2
041500     MOVE ZERO TO TEMP.                                           IF1264.2
041600 F-NUMVALC-TEST-06.                                               IF1264.2
041700     COMPUTE TEMP = FUNCTION NUMVAL-C (J).                        IF1264.2
041800     IF (TEMP >= 8848.76) AND                                     IF1264.2
041900        (TEMP <= 8849.11)                                         IF1264.2
042000                        PERFORM PASS                              IF1264.2
042100     ELSE                                                         IF1264.2
042200                        MOVE  8848.934  TO CORRECT-N              IF1264.2
042300                        MOVE TEMP TO COMPUTED-N                   IF1264.2
042400                        PERFORM FAIL.                             IF1264.2
042500     GO TO F-NUMVALC-WRITE-06.                                    IF1264.2
042600 F-NUMVALC-DELETE-06.                                             IF1264.2
042700     PERFORM  DE-LETE.                                            IF1264.2
042800     GO TO    F-NUMVALC-WRITE-06.                                 IF1264.2
042900 F-NUMVALC-WRITE-06.                                              IF1264.2
043000     MOVE "F-NUMVALC-06" TO PAR-NAME.                             IF1264.2
043100     PERFORM  PRINT-DETAIL.                                       IF1264.2
043200*****************TEST (g) ******************************          IF1264.2
043300 F-NUMVALC-07.                                                    IF1264.2
043400     MOVE ZERO TO TEMP.                                           IF1264.2
043500 F-NUMVALC-TEST-07.                                               IF1264.2
043600     COMPUTE TEMP = FUNCTION NUMVAL-C ("+394.2  ").               IF1264.2
043700     IF (TEMP >= 394.192) AND                                     IF1264.2
043800        (TEMP <= 394.208)                                         IF1264.2
043900                        PERFORM PASS                              IF1264.2
044000     ELSE                                                         IF1264.2
044100                        MOVE  394.2  TO CORRECT-N                 IF1264.2
044200                        MOVE TEMP TO COMPUTED-N                   IF1264.2
044300                        PERFORM FAIL.                             IF1264.2
044400     GO TO F-NUMVALC-WRITE-07.                                    IF1264.2
044500 F-NUMVALC-DELETE-07.                                             IF1264.2
044600     PERFORM  DE-LETE.                                            IF1264.2
044700     GO TO    F-NUMVALC-WRITE-07.                                 IF1264.2
044800 F-NUMVALC-WRITE-07.                                              IF1264.2
044900     MOVE "F-NUMVALC-07" TO PAR-NAME.                             IF1264.2
045000     PERFORM  PRINT-DETAIL.                                       IF1264.2
045100*****************TEST (h) ******************************          IF1264.2
045200 F-NUMVALC-08.                                                    IF1264.2
045300     MOVE ZERO TO TEMP.                                           IF1264.2
045400 F-NUMVALC-TEST-08.                                               IF1264.2
045500     COMPUTE TEMP = FUNCTION NUMVAL-C ("  939.83").               IF1264.2
045600     IF (TEMP >= 939.811) AND                                     IF1264.2
045700        (TEMP <= 939.849)                                         IF1264.2
045800                        PERFORM PASS                              IF1264.2
045900     ELSE                                                         IF1264.2
046000                        MOVE  939.83  TO CORRECT-N                IF1264.2
046100                        MOVE TEMP TO COMPUTED-N                   IF1264.2
046200                        PERFORM FAIL.                             IF1264.2
046300     GO TO F-NUMVALC-WRITE-08.                                    IF1264.2
046400 F-NUMVALC-DELETE-08.                                             IF1264.2
046500     PERFORM  DE-LETE.                                            IF1264.2
046600     GO TO    F-NUMVALC-WRITE-08.                                 IF1264.2
046700 F-NUMVALC-WRITE-08.                                              IF1264.2
046800     MOVE "F-NUMVALC-08" TO PAR-NAME.                             IF1264.2
046900     PERFORM  PRINT-DETAIL.                                       IF1264.2
047000*****************TEST (i) ******************************          IF1264.2
047100 F-NUMVALC-09.                                                    IF1264.2
047200     MOVE ZERO TO TEMP.                                           IF1264.2
047300 F-NUMVALC-TEST-09.                                               IF1264.2
047400     COMPUTE TEMP = FUNCTION NUMVAL-C ("  -  4929.0323").         IF1264.2
047500     IF (TEMP >= -4929.1309) AND                                  IF1264.2
047600        (TEMP <= -4928.9337)                                      IF1264.2
047700                        PERFORM PASS                              IF1264.2
047800     ELSE                                                         IF1264.2
047900                        MOVE  -4929.0323  TO CORRECT-N            IF1264.2
048000                        MOVE TEMP TO COMPUTED-N                   IF1264.2
048100                        PERFORM FAIL.                             IF1264.2
048200     GO TO F-NUMVALC-WRITE-09.                                    IF1264.2
048300 F-NUMVALC-DELETE-09.                                             IF1264.2
048400     PERFORM  DE-LETE.                                            IF1264.2
048500     GO TO    F-NUMVALC-WRITE-09.                                 IF1264.2
048600 F-NUMVALC-WRITE-09.                                              IF1264.2
048700     MOVE "F-NUMVALC-09" TO PAR-NAME.                             IF1264.2
048800     PERFORM  PRINT-DETAIL.                                       IF1264.2
048900*****************TEST (j) ******************************          IF1264.2
049000 F-NUMVALC-10.                                                    IF1264.2
049100     MOVE ZERO TO TEMP.                                           IF1264.2
049200 F-NUMVALC-TEST-10.                                               IF1264.2
049300     COMPUTE TEMP = FUNCTION NUMVAL-C (K).                        IF1264.2
049400     IF (TEMP >= 4825197.41) AND                                  IF1264.2
049500        (TEMP <= 4825390.43)                                      IF1264.2
049600                        PERFORM PASS                              IF1264.2
049700     ELSE                                                         IF1264.2
049800                        MOVE  4825293.92  TO CORRECT-N            IF1264.2
049900                        MOVE TEMP TO COMPUTED-N                   IF1264.2
050000                        PERFORM FAIL.                             IF1264.2
050100     GO TO F-NUMVALC-WRITE-10.                                    IF1264.2
050200 F-NUMVALC-DELETE-10.                                             IF1264.2
050300     PERFORM  DE-LETE.                                            IF1264.2
050400     GO TO    F-NUMVALC-WRITE-10.                                 IF1264.2
050500 F-NUMVALC-WRITE-10.                                              IF1264.2
050600     MOVE "F-NUMVALC-10" TO PAR-NAME.                             IF1264.2
050700     PERFORM  PRINT-DETAIL.                                       IF1264.2
050800*****************TEST (k) ******************************          IF1264.2
050900 F-NUMVALC-11.                                                    IF1264.2
051000     MOVE ZERO TO TEMP.                                           IF1264.2
051100 F-NUMVALC-TEST-11.                                               IF1264.2
051200     COMPUTE TEMP = FUNCTION NUMVAL-C (L).                        IF1264.2
051300     IF (TEMP >= -5555.66) AND                                    IF1264.2
051400        (TEMP <= -5555.44)                                        IF1264.2
051500                        PERFORM PASS                              IF1264.2
051600     ELSE                                                         IF1264.2
051700                        MOVE  -5555.55  TO CORRECT-N              IF1264.2
051800                        MOVE TEMP TO COMPUTED-N                   IF1264.2
051900                        PERFORM FAIL.                             IF1264.2
052000     GO TO F-NUMVALC-WRITE-11.                                    IF1264.2
052100 F-NUMVALC-DELETE-11.                                             IF1264.2
052200     PERFORM  DE-LETE.                                            IF1264.2
052300     GO TO    F-NUMVALC-WRITE-11.                                 IF1264.2
052400 F-NUMVALC-WRITE-11.                                              IF1264.2
052500     MOVE "F-NUMVALC-11" TO PAR-NAME.                             IF1264.2
052600     PERFORM  PRINT-DETAIL.                                       IF1264.2
052700*****************TEST (l) ******************************          IF1264.2
052800 F-NUMVALC-12.                                                    IF1264.2
052900     MOVE ZERO TO TEMP.                                           IF1264.2
053000 F-NUMVALC-TEST-12.                                               IF1264.2
053100     COMPUTE TEMP = FUNCTION NUMVAL-C ("82.9312+").               IF1264.2
053200     IF (TEMP >= 82.9295) AND                                     IF1264.2
053300        (TEMP <= 82.9329)                                         IF1264.2
053400                        PERFORM PASS                              IF1264.2
053500     ELSE                                                         IF1264.2
053600                        MOVE  82.9312  TO CORRECT-N               IF1264.2
053700                        MOVE TEMP TO COMPUTED-N                   IF1264.2
053800                        PERFORM FAIL.                             IF1264.2
053900     GO TO F-NUMVALC-WRITE-12.                                    IF1264.2
054000 F-NUMVALC-DELETE-12.                                             IF1264.2
054100     PERFORM  DE-LETE.                                            IF1264.2
054200     GO TO    F-NUMVALC-WRITE-12.                                 IF1264.2
054300 F-NUMVALC-WRITE-12.                                              IF1264.2
054400     MOVE "F-NUMVALC-12" TO PAR-NAME.                             IF1264.2
054500     PERFORM  PRINT-DETAIL.                                       IF1264.2
054600*****************TEST (m) ******************************          IF1264.2
054700 F-NUMVALC-13.                                                    IF1264.2
054800     MOVE ZERO TO TEMP.                                           IF1264.2
054900 F-NUMVALC-TEST-13.                                               IF1264.2
055000     COMPUTE TEMP = FUNCTION NUMVAL-C (M).                        IF1264.2
055100     IF (TEMP >= -5555.66) AND                                    IF1264.2
055200        (TEMP <= -5555.44)                                        IF1264.2
055300                        PERFORM PASS                              IF1264.2
055400     ELSE                                                         IF1264.2
055500                        MOVE  -5555.55  TO CORRECT-N              IF1264.2
055600                        MOVE TEMP TO COMPUTED-N                   IF1264.2
055700                        PERFORM FAIL.                             IF1264.2
055800     GO TO F-NUMVALC-WRITE-13.                                    IF1264.2
055900 F-NUMVALC-DELETE-13.                                             IF1264.2
056000     PERFORM  DE-LETE.                                            IF1264.2
056100     GO TO    F-NUMVALC-WRITE-13.                                 IF1264.2
056200 F-NUMVALC-WRITE-13.                                              IF1264.2
056300     MOVE "F-NUMVALC-13" TO PAR-NAME.                             IF1264.2
056400     PERFORM  PRINT-DETAIL.                                       IF1264.2
056500*****************TEST (n) ******************************          IF1264.2
056600 F-NUMVALC-14.                                                    IF1264.2
056700     MOVE ZERO TO TEMP.                                           IF1264.2
056800 F-NUMVALC-TEST-14.                                               IF1264.2
056900     COMPUTE TEMP = FUNCTION NUMVAL-C ("  200.0002  -  ").        IF1264.2
057000     IF (TEMP >= -200.0042) AND                                   IF1264.2
057100        (TEMP <= -199.9962)                                       IF1264.2
057200                        PERFORM PASS                              IF1264.2
057300     ELSE                                                         IF1264.2
057400                        MOVE  -200.0002  TO CORRECT-N             IF1264.2
057500                        MOVE TEMP TO COMPUTED-N                   IF1264.2
057600                        PERFORM FAIL.                             IF1264.2
057700     GO TO F-NUMVALC-WRITE-14.                                    IF1264.2
057800 F-NUMVALC-DELETE-14.                                             IF1264.2
057900     PERFORM  DE-LETE.                                            IF1264.2
058000     GO TO    F-NUMVALC-WRITE-14.                                 IF1264.2
058100 F-NUMVALC-WRITE-14.                                              IF1264.2
058200     MOVE "F-NUMVALC-14" TO PAR-NAME.                             IF1264.2
058300     PERFORM  PRINT-DETAIL.                                       IF1264.2
058400*****************TEST (o) ******************************          IF1264.2
058500 F-NUMVALC-15.                                                    IF1264.2
058600     MOVE ZERO TO TEMP.                                           IF1264.2
058700 F-NUMVALC-TEST-15.                                               IF1264.2
058800     COMPUTE TEMP = FUNCTION NUMVAL-C (N).                        IF1264.2
058900     IF (TEMP >= 77776.21) AND                                    IF1264.2
059000        (TEMP <= 77779.33)                                        IF1264.2
059100                        PERFORM PASS                              IF1264.2
059200     ELSE                                                         IF1264.2
059300                        MOVE  77777.77  TO CORRECT-N              IF1264.2
059400                        MOVE TEMP TO COMPUTED-N                   IF1264.2
059500                        PERFORM FAIL.                             IF1264.2
059600     GO TO F-NUMVALC-WRITE-15.                                    IF1264.2
059700 F-NUMVALC-DELETE-15.                                             IF1264.2
059800     PERFORM  DE-LETE.                                            IF1264.2
059900     GO TO    F-NUMVALC-WRITE-15.                                 IF1264.2
060000 F-NUMVALC-WRITE-15.                                              IF1264.2
060100     MOVE "F-NUMVALC-15" TO PAR-NAME.                             IF1264.2
060200     PERFORM  PRINT-DETAIL.                                       IF1264.2
060300*****************TEST (p) ******************************          IF1264.2
060400 F-NUMVALC-16.                                                    IF1264.2
060500     MOVE ZERO TO TEMP.                                           IF1264.2
060600 F-NUMVALC-TEST-16.                                               IF1264.2
060700     COMPUTE TEMP = FUNCTION NUMVAL-C ("$5", "$").                IF1264.2
060800     IF TEMP = 5 THEN                                             IF1264.2
060900                        PERFORM PASS                              IF1264.2
061000     ELSE                                                         IF1264.2
061100                        MOVE  5  TO CORRECT-N                     IF1264.2
061200                        MOVE TEMP TO COMPUTED-N                   IF1264.2
061300                        PERFORM FAIL.                             IF1264.2
061400     GO TO F-NUMVALC-WRITE-16.                                    IF1264.2
061500 F-NUMVALC-DELETE-16.                                             IF1264.2
061600     PERFORM  DE-LETE.                                            IF1264.2
061700     GO TO    F-NUMVALC-WRITE-16.                                 IF1264.2
061800 F-NUMVALC-WRITE-16.                                              IF1264.2
061900     MOVE "F-NUMVALC-16" TO PAR-NAME.                             IF1264.2
062000     PERFORM  PRINT-DETAIL.                                       IF1264.2
062100*****************TEST (q) ******************************          IF1264.2
062200 F-NUMVALC-17.                                                    IF1264.2
062300     MOVE ZERO TO TEMP.                                           IF1264.2
062400 F-NUMVALC-TEST-17.                                               IF1264.2
062500     COMPUTE TEMP = FUNCTION NUMVAL-C (O, "$").                   IF1264.2
062600     IF TEMP = 33 THEN                                            IF1264.2
062700                        PERFORM PASS                              IF1264.2
062800     ELSE                                                         IF1264.2
062900                        MOVE  33  TO CORRECT-N                    IF1264.2
063000                        MOVE TEMP TO COMPUTED-N                   IF1264.2
063100                        PERFORM FAIL.                             IF1264.2
063200     GO TO F-NUMVALC-WRITE-17.                                    IF1264.2
063300 F-NUMVALC-DELETE-17.                                             IF1264.2
063400     PERFORM  DE-LETE.                                            IF1264.2
063500     GO TO    F-NUMVALC-WRITE-17.                                 IF1264.2
063600 F-NUMVALC-WRITE-17.                                              IF1264.2
063700     MOVE "F-NUMVALC-17" TO PAR-NAME.                             IF1264.2
063800     PERFORM  PRINT-DETAIL.                                       IF1264.2
063900*****************TEST (r) ******************************          IF1264.2
064000 F-NUMVALC-18.                                                    IF1264.2
064100     MOVE ZERO TO TEMP.                                           IF1264.2
064200 F-NUMVALC-TEST-18.                                               IF1264.2
064300     COMPUTE TEMP = FUNCTION NUMVAL-C ("$93,021", "$").           IF1264.2
064400     IF TEMP = 93021 THEN                                         IF1264.2
064500                        PERFORM PASS                              IF1264.2
064600     ELSE                                                         IF1264.2
064700                        MOVE  93021  TO CORRECT-N                 IF1264.2
064800                        MOVE TEMP TO COMPUTED-N                   IF1264.2
064900                        PERFORM FAIL.                             IF1264.2
065000     GO TO F-NUMVALC-WRITE-18.                                    IF1264.2
065100 F-NUMVALC-DELETE-18.                                             IF1264.2
065200     PERFORM  DE-LETE.                                            IF1264.2
065300     GO TO    F-NUMVALC-WRITE-18.                                 IF1264.2
065400 F-NUMVALC-WRITE-18.                                              IF1264.2
065500     MOVE "F-NUMVALC-18" TO PAR-NAME.                             IF1264.2
065600     PERFORM  PRINT-DETAIL.                                       IF1264.2
065700*****************TEST (t) ******************************          IF1264.2
065800 F-NUMVALC-20.                                                    IF1264.2
065900     MOVE ZERO TO TEMP.                                           IF1264.2
066000 F-NUMVALC-TEST-20.                                               IF1264.2
066100     COMPUTE TEMP = FUNCTION NUMVAL-C ("$924.93", "$").           IF1264.2
066200     IF (TEMP >= 924.912) AND                                     IF1264.2
066300        (TEMP <= 924.948)                                         IF1264.2
066400                        PERFORM PASS                              IF1264.2
066500     ELSE                                                         IF1264.2
066600                        MOVE  924.93  TO CORRECT-N                IF1264.2
066700                        MOVE TEMP TO COMPUTED-N                   IF1264.2
066800                        PERFORM FAIL.                             IF1264.2
066900     GO TO F-NUMVALC-WRITE-20.                                    IF1264.2
067000 F-NUMVALC-DELETE-20.                                             IF1264.2
067100     PERFORM  DE-LETE.                                            IF1264.2
067200     GO TO    F-NUMVALC-WRITE-20.                                 IF1264.2
067300 F-NUMVALC-WRITE-20.                                              IF1264.2
067400     MOVE "F-NUMVALC-20" TO PAR-NAME.                             IF1264.2
067500     PERFORM  PRINT-DETAIL.                                       IF1264.2
067600*****************TEST (u) ******************************          IF1264.2
067700 F-NUMVALC-21.                                                    IF1264.2
067800     MOVE ZERO TO TEMP.                                           IF1264.2
067900 F-NUMVALC-TEST-21.                                               IF1264.2
068000     COMPUTE TEMP = FUNCTION NUMVAL-C (Q, "$").                   IF1264.2
068100     IF TEMP = 4000 THEN                                          IF1264.2
068200                        PERFORM PASS                              IF1264.2
068300     ELSE                                                         IF1264.2
068400                        MOVE  4000  TO CORRECT-N                  IF1264.2
068500                        MOVE TEMP TO COMPUTED-N                   IF1264.2
068600                        PERFORM FAIL.                             IF1264.2
068700     GO TO F-NUMVALC-WRITE-21.                                    IF1264.2
068800 F-NUMVALC-DELETE-21.                                             IF1264.2
068900     PERFORM  DE-LETE.                                            IF1264.2
069000     GO TO    F-NUMVALC-WRITE-21.                                 IF1264.2
069100 F-NUMVALC-WRITE-21.                                              IF1264.2
069200     MOVE "F-NUMVALC-21" TO PAR-NAME.                             IF1264.2
069300     PERFORM  PRINT-DETAIL.                                       IF1264.2
069400*****************TEST (v) ******************************          IF1264.2
069500 F-NUMVALC-22.                                                    IF1264.2
069600     MOVE ZERO TO TEMP.                                           IF1264.2
069700 F-NUMVALC-TEST-22.                                               IF1264.2
069800     COMPUTE TEMP = FUNCTION NUMVAL-C ("-$34.03", "$").           IF1264.2
069900     IF (TEMP >= -34.0307) AND                                    IF1264.2
070000        (TEMP <= -34.0293)                                        IF1264.2
070100                        PERFORM PASS                              IF1264.2
070200     ELSE                                                         IF1264.2
070300                        MOVE  -34.03  TO CORRECT-N                IF1264.2
070400                        MOVE TEMP TO COMPUTED-N                   IF1264.2
070500                        PERFORM FAIL.                             IF1264.2
070600     GO TO F-NUMVALC-WRITE-22.                                    IF1264.2
070700 F-NUMVALC-DELETE-22.                                             IF1264.2
070800     PERFORM  DE-LETE.                                            IF1264.2
070900     GO TO    F-NUMVALC-WRITE-22.                                 IF1264.2
071000 F-NUMVALC-WRITE-22.                                              IF1264.2
071100     MOVE "F-NUMVALC-22" TO PAR-NAME.                             IF1264.2
071200     PERFORM  PRINT-DETAIL.                                       IF1264.2
071300*****************TEST (w) ******************************          IF1264.2
071400 F-NUMVALC-23.                                                    IF1264.2
071500     MOVE ZERO TO TEMP.                                           IF1264.2
071600 F-NUMVALC-TEST-23.                                               IF1264.2
071700     COMPUTE TEMP = FUNCTION NUMVAL-C (R, "$").                   IF1264.2
071800     IF (TEMP >= 999980.5) AND                                    IF1264.2
071900        (TEMP <= 1000020.5)                                       IF1264.2
072000                        PERFORM PASS                              IF1264.2
072100     ELSE                                                         IF1264.2
072200                        MOVE  1000000.5  TO CORRECT-N             IF1264.2
072300                        MOVE TEMP TO COMPUTED-N                   IF1264.2
072400                        PERFORM FAIL.                             IF1264.2
072500     GO TO F-NUMVALC-WRITE-23.                                    IF1264.2
072600 F-NUMVALC-DELETE-23.                                             IF1264.2
072700     PERFORM  DE-LETE.                                            IF1264.2
072800     GO TO    F-NUMVALC-WRITE-23.                                 IF1264.2
072900 F-NUMVALC-WRITE-23.                                              IF1264.2
073000     MOVE "F-NUMVALC-23" TO PAR-NAME.                             IF1264.2
073100     PERFORM  PRINT-DETAIL.                                       IF1264.2
073200*****************TEST (x) ******************************          IF1264.2
073300 F-NUMVALC-24.                                                    IF1264.2
073400     MOVE ZERO TO TEMP.                                           IF1264.2
073500 F-NUMVALC-TEST-24.                                               IF1264.2
073600     COMPUTE TEMP = FUNCTION NUMVAL-C ("  $  89.01", "$").        IF1264.2
073700     IF (TEMP >= 89.0082) AND                                     IF1264.2
073800        (TEMP <= 89.0118)                                         IF1264.2
073900                        PERFORM PASS                              IF1264.2
074000     ELSE                                                         IF1264.2
074100                        MOVE  89.01  TO CORRECT-N                 IF1264.2
074200                        MOVE TEMP TO COMPUTED-N                   IF1264.2
074300                        PERFORM FAIL.                             IF1264.2
074400     GO TO F-NUMVALC-WRITE-24.                                    IF1264.2
074500 F-NUMVALC-DELETE-24.                                             IF1264.2
074600     PERFORM  DE-LETE.                                            IF1264.2
074700     GO TO    F-NUMVALC-WRITE-24.                                 IF1264.2
074800 F-NUMVALC-WRITE-24.                                              IF1264.2
074900     MOVE "F-NUMVALC-24" TO PAR-NAME.                             IF1264.2
075000     PERFORM  PRINT-DETAIL.                                       IF1264.2
075100*****************TEST (y) ******************************          IF1264.2
075200 F-NUMVALC-25.                                                    IF1264.2
075300     MOVE ZERO TO TEMP.                                           IF1264.2
075400 F-NUMVALC-TEST-25.                                               IF1264.2
075500     COMPUTE TEMP = FUNCTION NUMVAL-C (S, "$").                   IF1264.2
075600     IF (TEMP >= 3900.13) AND                                     IF1264.2
075700        (TEMP <= 3900.29)                                         IF1264.2
075800                        PERFORM PASS                              IF1264.2
075900     ELSE                                                         IF1264.2
076000                        MOVE  3900.21  TO CORRECT-N               IF1264.2
076100                        MOVE TEMP TO COMPUTED-N                   IF1264.2
076200                        PERFORM FAIL.                             IF1264.2
076300     GO TO F-NUMVALC-WRITE-25.                                    IF1264.2
076400 F-NUMVALC-DELETE-25.                                             IF1264.2
076500     PERFORM  DE-LETE.                                            IF1264.2
076600     GO TO    F-NUMVALC-WRITE-25.                                 IF1264.2
076700 F-NUMVALC-WRITE-25.                                              IF1264.2
076800     MOVE "F-NUMVALC-25" TO PAR-NAME.                             IF1264.2
076900     PERFORM  PRINT-DETAIL.                                       IF1264.2
077000*****************TEST (z) ******************************          IF1264.2
077100 F-NUMVALC-26.                                                    IF1264.2
077200     MOVE ZERO TO TEMP.                                           IF1264.2
077300 F-NUMVALC-TEST-26.                                               IF1264.2
077400     COMPUTE TEMP = FUNCTION NUMVAL-C ("- $ 890.21", "$").        IF1264.2
077500     IF (TEMP >= -890.228) AND                                    IF1264.2
077600        (TEMP <= -890.192)                                        IF1264.2
077700                        PERFORM PASS                              IF1264.2
077800     ELSE                                                         IF1264.2
077900                        MOVE  -890.21  TO CORRECT-N               IF1264.2
078000                        MOVE TEMP TO COMPUTED-N                   IF1264.2
078100                        PERFORM FAIL.                             IF1264.2
078200     GO TO F-NUMVALC-WRITE-26.                                    IF1264.2
078300 F-NUMVALC-DELETE-26.                                             IF1264.2
078400     PERFORM  DE-LETE.                                            IF1264.2
078500     GO TO    F-NUMVALC-WRITE-26.                                 IF1264.2
078600 F-NUMVALC-WRITE-26.                                              IF1264.2
078700     MOVE "F-NUMVALC-26" TO PAR-NAME.                             IF1264.2
078800     PERFORM  PRINT-DETAIL.                                       IF1264.2
078900*****************TEST (aa) ******************************         IF1264.2
079000 F-NUMVALC-27.                                                    IF1264.2
079100     MOVE ZERO TO TEMP.                                           IF1264.2
079200 F-NUMVALC-TEST-27.                                               IF1264.2
079300     COMPUTE TEMP = FUNCTION NUMVAL-C (T, "$").                   IF1264.2
079400     IF (TEMP >= 9000.81) AND                                     IF1264.2
079500        (TEMP <= 9001.17)                                         IF1264.2
079600                        PERFORM PASS                              IF1264.2
079700     ELSE                                                         IF1264.2
079800                        MOVE  9000.99  TO CORRECT-N               IF1264.2
079900                        MOVE TEMP TO COMPUTED-N                   IF1264.2
080000                        PERFORM FAIL.                             IF1264.2
080100     GO TO F-NUMVALC-WRITE-27.                                    IF1264.2
080200 F-NUMVALC-DELETE-27.                                             IF1264.2
080300     PERFORM  DE-LETE.                                            IF1264.2
080400     GO TO    F-NUMVALC-WRITE-27.                                 IF1264.2
080500 F-NUMVALC-WRITE-27.                                              IF1264.2
080600     MOVE "F-NUMVALC-27" TO PAR-NAME.                             IF1264.2
080700     PERFORM  PRINT-DETAIL.                                       IF1264.2
080800*****************TEST (bb) ******************************         IF1264.2
080900 F-NUMVALC-28.                                                    IF1264.2
081000     MOVE ZERO TO TEMP.                                           IF1264.2
081100 F-NUMVALC-TEST-28.                                               IF1264.2
081200     COMPUTE TEMP = FUNCTION NUMVAL-C ("  $  90.54 -  ", "$").    IF1264.2
081300     IF (TEMP >= -90.5418) AND                                    IF1264.2
081400        (TEMP <= -90.5382)                                        IF1264.2
081500                        PERFORM PASS                              IF1264.2
081600     ELSE                                                         IF1264.2
081700                        MOVE  -90.54  TO CORRECT-N                IF1264.2
081800                        MOVE TEMP TO COMPUTED-N                   IF1264.2
081900                        PERFORM FAIL.                             IF1264.2
082000     GO TO F-NUMVALC-WRITE-28.                                    IF1264.2
082100 F-NUMVALC-DELETE-28.                                             IF1264.2
082200     PERFORM  DE-LETE.                                            IF1264.2
082300     GO TO    F-NUMVALC-WRITE-28.                                 IF1264.2
082400 F-NUMVALC-WRITE-28.                                              IF1264.2
082500     MOVE "F-NUMVALC-28" TO PAR-NAME.                             IF1264.2
082600     PERFORM  PRINT-DETAIL.                                       IF1264.2
082700*****************TEST (cc) ******************************         IF1264.2
082800 F-NUMVALC-29.                                                    IF1264.2
082900     MOVE ZERO TO TEMP.                                           IF1264.2
083000 F-NUMVALC-TEST-29.                                               IF1264.2
083100     COMPUTE TEMP = FUNCTION NUMVAL-C (U, "$").                   IF1264.2
083200     IF (TEMP >= 3890.12) AND                                     IF1264.2
083300        (TEMP <= 3890.28)                                         IF1264.2
083400                        PERFORM PASS                              IF1264.2
083500     ELSE                                                         IF1264.2
083600                        MOVE  3890.2  TO CORRECT-N                IF1264.2
083700                        MOVE TEMP TO COMPUTED-N                   IF1264.2
083800                        PERFORM FAIL.                             IF1264.2
083900     GO TO F-NUMVALC-WRITE-29.                                    IF1264.2
084000 F-NUMVALC-DELETE-29.                                             IF1264.2
084100     PERFORM  DE-LETE.                                            IF1264.2
084200     GO TO    F-NUMVALC-WRITE-29.                                 IF1264.2
084300 F-NUMVALC-WRITE-29.                                              IF1264.2
084400     MOVE "F-NUMVALC-29" TO PAR-NAME.                             IF1264.2
084500     PERFORM  PRINT-DETAIL.                                       IF1264.2
084600*****************TEST (dd) ******************************         IF1264.2
084700 F-NUMVALC-30.                                                    IF1264.2
084800     MOVE ZERO TO TEMP.                                           IF1264.2
084900 F-NUMVALC-TEST-30.                                               IF1264.2
085000     COMPUTE TEMP = FUNCTION NUMVAL-C ("90") + 10.                IF1264.2
085100     IF TEMP = 100 THEN                                           IF1264.2
085200                        PERFORM PASS                              IF1264.2
085300     ELSE                                                         IF1264.2
085400                        MOVE  100  TO CORRECT-N                   IF1264.2
085500                        MOVE TEMP TO COMPUTED-N                   IF1264.2
085600                        PERFORM FAIL.                             IF1264.2
085700     GO TO F-NUMVALC-WRITE-30.                                    IF1264.2
085800 F-NUMVALC-DELETE-30.                                             IF1264.2
085900     PERFORM  DE-LETE.                                            IF1264.2
086000     GO TO    F-NUMVALC-WRITE-30.                                 IF1264.2
086100 F-NUMVALC-WRITE-30.                                              IF1264.2
086200     MOVE "F-NUMVALC-30" TO PAR-NAME.                             IF1264.2
086300     PERFORM  PRINT-DETAIL.                                       IF1264.2
086400*****************TEST (ee) ******************************         IF1264.2
086500 F-NUMVALC-31.                                                    IF1264.2
086600     MOVE ZERO TO TEMP.                                           IF1264.2
086700 F-NUMVALC-TEST-31.                                               IF1264.2
086800     COMPUTE TEMP = FUNCTION NUMVAL-C ("2") +                     IF1264.2
086900                    FUNCTION NUMVAL-C ("8").                      IF1264.2
087000     IF TEMP = 10 THEN                                            IF1264.2
087100                        PERFORM PASS                              IF1264.2
087200     ELSE                                                         IF1264.2
087300                        MOVE  10  TO CORRECT-N                    IF1264.2
087400                        MOVE TEMP TO COMPUTED-N                   IF1264.2
087500                        PERFORM FAIL.                             IF1264.2
087600     GO TO F-NUMVALC-WRITE-31.                                    IF1264.2
087700 F-NUMVALC-DELETE-31.                                             IF1264.2
087800     PERFORM  DE-LETE.                                            IF1264.2
087900     GO TO    F-NUMVALC-WRITE-31.                                 IF1264.2
088000 F-NUMVALC-WRITE-31.                                              IF1264.2
088100     MOVE "F-NUMVALC-31" TO PAR-NAME.                             IF1264.2
088200     PERFORM  PRINT-DETAIL.                                       IF1264.2
088300*******************END OF TESTS**************************         IF1264.2
088400 CCVS-EXIT SECTION.                                               IF1264.2
088500 CCVS-999999.                                                     IF1264.2
088600     GO TO CLOSE-FILES.                                           IF1264.2
      *END-OF,IF126A                                                                  
