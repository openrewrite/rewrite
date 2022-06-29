      *HEADER,COBOL,IF128A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1284.2
000200 PROGRAM-ID.                                                      IF1284.2
000300     IF128A.                                                      IF1284.2
000400                                                                  IF1284.2
000500***********************************************************       IF1284.2
000600*                                                         *       IF1284.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1284.2
000800* It contains tests for the Intrinsic Function ORD-MAX.   *       IF1284.2
000900*                                                         *       IF1284.2
001000*                                                         *       IF1284.2
001100***********************************************************       IF1284.2
001200 ENVIRONMENT DIVISION.                                            IF1284.2
001300 CONFIGURATION SECTION.                                           IF1284.2
001400 SOURCE-COMPUTER.                                                 IF1284.2
001500     XXXXX082.                                                    IF1284.2
001600 OBJECT-COMPUTER.                                                 IF1284.2
001700     XXXXX083                                                     IF1284.2
001800     PROGRAM COLLATING SEQUENCE IS PRG-COLL-SEQ.                  IF1284.2
001900 SPECIAL-NAMES.                                                   IF1284.2
002000     ALPHABET PRG-COLL-SEQ IS                                     IF1284.2
002100     STANDARD-2.                                                  IF1284.2
002200 INPUT-OUTPUT SECTION.                                            IF1284.2
002300 FILE-CONTROL.                                                    IF1284.2
002400     SELECT PRINT-FILE ASSIGN TO                                  IF1284.2
002500     XXXXX055.                                                    IF1284.2
002600 DATA DIVISION.                                                   IF1284.2
002700 FILE SECTION.                                                    IF1284.2
002800 FD  PRINT-FILE.                                                  IF1284.2
002900 01  PRINT-REC PICTURE X(120).                                    IF1284.2
003000 01  DUMMY-RECORD PICTURE X(120).                                 IF1284.2
003100 WORKING-STORAGE SECTION.                                         IF1284.2
003200***********************************************************       IF1284.2
003300* Variables specific to the Intrinsic Function Test IF128A*       IF1284.2
003400***********************************************************       IF1284.2
003500 01  A                   PIC S9(10)     VALUE 5.                  IF1284.2
003600 01  B                   PIC S9(10)     VALUE 7.                  IF1284.2
003700 01  C                   PIC S9(10)     VALUE 4.                  IF1284.2
003800 01  D                   PIC S9(10)     VALUE 10.                 IF1284.2
003900 01  I                   PIC X(4)       VALUE "R".                IF1284.2
004000 01  J                   PIC X(4)       VALUE "U".                IF1284.2
004100 01  P                   PIC S9(10)     VALUE 1.                  IF1284.2
004200 01  Q                   PIC S9(10)     VALUE 3.                  IF1284.2
004300 01  R                   PIC S9(10)     VALUE 5.                  IF1284.2
004400 01  ARG1                PIC S9(10)     VALUE 1.                  IF1284.2
004500 01  ARR                                VALUE "40537".            IF1284.2
004600     02 IND OCCURS 5 TIMES   PIC 9.                               IF1284.2
004700 01  TEMP                PIC S9(10).                              IF1284.2
004800 01  WS-INT              PIC S9(10).                              IF1284.2
004900*                                                                 IF1284.2
005000**********************************************************        IF1284.2
005100*                                                                 IF1284.2
005200 01  TEST-RESULTS.                                                IF1284.2
005300     02 FILLER                   PIC X      VALUE SPACE.          IF1284.2
005400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1284.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IF1284.2
005600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1284.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IF1284.2
005800     02  PAR-NAME.                                                IF1284.2
005900       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1284.2
006000       03  PARDOT-X              PIC X      VALUE SPACE.          IF1284.2
006100       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1284.2
006200     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1284.2
006300     02 RE-MARK                  PIC X(61).                       IF1284.2
006400 01  TEST-COMPUTED.                                               IF1284.2
006500     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1284.2
006600     02 FILLER                   PIC X(17)  VALUE                 IF1284.2
006700            "       COMPUTED=".                                   IF1284.2
006800     02 COMPUTED-X.                                               IF1284.2
006900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1284.2
007000     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1284.2
007100                                 PIC -9(9).9(9).                  IF1284.2
007200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1284.2
007300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1284.2
007400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1284.2
007500     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1284.2
007600         04 COMPUTED-18V0                    PIC -9(18).          IF1284.2
007700         04 FILLER                           PIC X.               IF1284.2
007800     03 FILLER PIC X(50) VALUE SPACE.                             IF1284.2
007900 01  TEST-CORRECT.                                                IF1284.2
008000     02 FILLER PIC X(30) VALUE SPACE.                             IF1284.2
008100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1284.2
008200     02 CORRECT-X.                                                IF1284.2
008300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1284.2
008400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1284.2
008500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1284.2
008600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1284.2
008700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1284.2
008800     03      CR-18V0 REDEFINES CORRECT-A.                         IF1284.2
008900         04 CORRECT-18V0                     PIC -9(18).          IF1284.2
009000         04 FILLER                           PIC X.               IF1284.2
009100     03 FILLER PIC X(2) VALUE SPACE.                              IF1284.2
009200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1284.2
009300 01  TEST-CORRECT-MIN.                                            IF1284.2
009400     02 FILLER PIC X(30) VALUE SPACE.                             IF1284.2
009500     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1284.2
009600     02 CORRECTMI-X.                                              IF1284.2
009700     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1284.2
009800     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1284.2
009900     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1284.2
010000     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1284.2
010100     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1284.2
010200     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1284.2
010300         04 CORRECTMI-18V0                     PIC -9(18).        IF1284.2
010400         04 FILLER                           PIC X.               IF1284.2
010500     03 FILLER PIC X(2) VALUE SPACE.                              IF1284.2
010600     03 FILLER                           PIC X(48) VALUE SPACE.   IF1284.2
010700 01  TEST-CORRECT-MAX.                                            IF1284.2
010800     02 FILLER PIC X(30) VALUE SPACE.                             IF1284.2
010900     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1284.2
011000     02 CORRECTMA-X.                                              IF1284.2
011100     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1284.2
011200     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1284.2
011300     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1284.2
011400     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1284.2
011500     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1284.2
011600     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1284.2
011700         04 CORRECTMA-18V0                     PIC -9(18).        IF1284.2
011800         04 FILLER                           PIC X.               IF1284.2
011900     03 FILLER PIC X(2) VALUE SPACE.                              IF1284.2
012000     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1284.2
012100 01  CCVS-C-1.                                                    IF1284.2
012200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1284.2
012300-    "SS  PARAGRAPH-NAME                                          IF1284.2
012400-    "       REMARKS".                                            IF1284.2
012500     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1284.2
012600 01  CCVS-C-2.                                                    IF1284.2
012700     02 FILLER                     PIC X        VALUE SPACE.      IF1284.2
012800     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1284.2
012900     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1284.2
013000     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1284.2
013100     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1284.2
013200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1284.2
013300 01  REC-CT                        PIC 99       VALUE ZERO.       IF1284.2
013400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1284.2
013500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1284.2
013600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1284.2
013700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1284.2
013800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1284.2
013900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1284.2
014000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1284.2
014100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1284.2
014200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1284.2
014300 01  CCVS-H-1.                                                    IF1284.2
014400     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1284.2
014500     02  FILLER                    PIC X(42)    VALUE             IF1284.2
014600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1284.2
014700     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1284.2
014800 01  CCVS-H-2A.                                                   IF1284.2
014900   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1284.2
015000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1284.2
015100   02  FILLER                        PIC XXXX   VALUE             IF1284.2
015200     "4.2 ".                                                      IF1284.2
015300   02  FILLER                        PIC X(28)  VALUE             IF1284.2
015400            " COPY - NOT FOR DISTRIBUTION".                       IF1284.2
015500   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1284.2
015600                                                                  IF1284.2
015700 01  CCVS-H-2B.                                                   IF1284.2
015800   02  FILLER                        PIC X(15)  VALUE             IF1284.2
015900            "TEST RESULT OF ".                                    IF1284.2
016000   02  TEST-ID                       PIC X(9).                    IF1284.2
016100   02  FILLER                        PIC X(4)   VALUE             IF1284.2
016200            " IN ".                                               IF1284.2
016300   02  FILLER                        PIC X(12)  VALUE             IF1284.2
016400     " HIGH       ".                                              IF1284.2
016500   02  FILLER                        PIC X(22)  VALUE             IF1284.2
016600            " LEVEL VALIDATION FOR ".                             IF1284.2
016700   02  FILLER                        PIC X(58)  VALUE             IF1284.2
016800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1284.2
016900 01  CCVS-H-3.                                                    IF1284.2
017000     02  FILLER                      PIC X(34)  VALUE             IF1284.2
017100            " FOR OFFICIAL USE ONLY    ".                         IF1284.2
017200     02  FILLER                      PIC X(58)  VALUE             IF1284.2
017300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1284.2
017400     02  FILLER                      PIC X(28)  VALUE             IF1284.2
017500            "  COPYRIGHT   1985 ".                                IF1284.2
017600 01  CCVS-E-1.                                                    IF1284.2
017700     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1284.2
017800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1284.2
017900     02 ID-AGAIN                     PIC X(9).                    IF1284.2
018000     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1284.2
018100 01  CCVS-E-2.                                                    IF1284.2
018200     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1284.2
018300     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1284.2
018400     02 CCVS-E-2-2.                                               IF1284.2
018500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1284.2
018600         03 FILLER                   PIC X      VALUE SPACE.      IF1284.2
018700         03 ENDER-DESC               PIC X(44)  VALUE             IF1284.2
018800            "ERRORS ENCOUNTERED".                                 IF1284.2
018900 01  CCVS-E-3.                                                    IF1284.2
019000     02  FILLER                      PIC X(22)  VALUE             IF1284.2
019100            " FOR OFFICIAL USE ONLY".                             IF1284.2
019200     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1284.2
019300     02  FILLER                      PIC X(58)  VALUE             IF1284.2
019400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1284.2
019500     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1284.2
019600     02 FILLER                       PIC X(15)  VALUE             IF1284.2
019700             " COPYRIGHT 1985".                                   IF1284.2
019800 01  CCVS-E-4.                                                    IF1284.2
019900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1284.2
020000     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1284.2
020100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1284.2
020200     02 FILLER                       PIC X(40)  VALUE             IF1284.2
020300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1284.2
020400 01  XXINFO.                                                      IF1284.2
020500     02 FILLER                       PIC X(19)  VALUE             IF1284.2
020600            "*** INFORMATION ***".                                IF1284.2
020700     02 INFO-TEXT.                                                IF1284.2
020800       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1284.2
020900       04 XXCOMPUTED                 PIC X(20).                   IF1284.2
021000       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1284.2
021100       04 XXCORRECT                  PIC X(20).                   IF1284.2
021200     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1284.2
021300 01  HYPHEN-LINE.                                                 IF1284.2
021400     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1284.2
021500     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1284.2
021600-    "*****************************************".                 IF1284.2
021700     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1284.2
021800-    "******************************".                            IF1284.2
021900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1284.2
022000     "IF128A".                                                    IF1284.2
022100 PROCEDURE DIVISION.                                              IF1284.2
022200 CCVS1 SECTION.                                                   IF1284.2
022300 OPEN-FILES.                                                      IF1284.2
022400     OPEN     OUTPUT PRINT-FILE.                                  IF1284.2
022500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1284.2
022600     MOVE    SPACE TO TEST-RESULTS.                               IF1284.2
022700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1284.2
022800     GO TO CCVS1-EXIT.                                            IF1284.2
022900 CLOSE-FILES.                                                     IF1284.2
023000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1284.2
023100 TERMINATE-CCVS.                                                  IF1284.2
023200     STOP     RUN.                                                IF1284.2
023300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1284.2
023400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1284.2
023500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1284.2
023600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1284.2
023700     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1284.2
023800 PRINT-DETAIL.                                                    IF1284.2
023900     IF REC-CT NOT EQUAL TO ZERO                                  IF1284.2
024000             MOVE "." TO PARDOT-X                                 IF1284.2
024100             MOVE REC-CT TO DOTVALUE.                             IF1284.2
024200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1284.2
024300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1284.2
024400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1284.2
024500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1284.2
024600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1284.2
024700     MOVE SPACE TO CORRECT-X.                                     IF1284.2
024800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1284.2
024900     MOVE     SPACE TO RE-MARK.                                   IF1284.2
025000 HEAD-ROUTINE.                                                    IF1284.2
025100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1284.2
025200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1284.2
025300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1284.2
025400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1284.2
025500 COLUMN-NAMES-ROUTINE.                                            IF1284.2
025600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1284.2
025700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1284.2
025800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1284.2
025900 END-ROUTINE.                                                     IF1284.2
026000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1284.2
026100 END-RTN-EXIT.                                                    IF1284.2
026200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1284.2
026300 END-ROUTINE-1.                                                   IF1284.2
026400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1284.2
026500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1284.2
026600      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1284.2
026700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1284.2
026800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1284.2
026900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1284.2
027000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1284.2
027100  END-ROUTINE-12.                                                 IF1284.2
027200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1284.2
027300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1284.2
027400         MOVE "NO " TO ERROR-TOTAL                                IF1284.2
027500         ELSE                                                     IF1284.2
027600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1284.2
027700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1284.2
027800     PERFORM WRITE-LINE.                                          IF1284.2
027900 END-ROUTINE-13.                                                  IF1284.2
028000     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1284.2
028100         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1284.2
028200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1284.2
028300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1284.2
028400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1284.2
028500      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1284.2
028600          MOVE "NO " TO ERROR-TOTAL                               IF1284.2
028700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1284.2
028800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1284.2
028900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1284.2
029000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1284.2
029100 WRITE-LINE.                                                      IF1284.2
029200     ADD 1 TO RECORD-COUNT.                                       IF1284.2
029300*    IF RECORD-COUNT GREATER 42                                   IF1284.2
029400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1284.2
029500*        MOVE SPACE TO DUMMY-RECORD                               IF1284.2
029600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1284.2
029700*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1284.2
029800*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1284.2
029900*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1284.2
030000*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1284.2
030100*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1284.2
030200*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1284.2
030300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1284.2
030400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1284.2
030500*        MOVE ZERO TO RECORD-COUNT.                               IF1284.2
030600     PERFORM WRT-LN.                                              IF1284.2
030700 WRT-LN.                                                          IF1284.2
030800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1284.2
030900     MOVE SPACE TO DUMMY-RECORD.                                  IF1284.2
031000 BLANK-LINE-PRINT.                                                IF1284.2
031100     PERFORM WRT-LN.                                              IF1284.2
031200 FAIL-ROUTINE.                                                    IF1284.2
031300     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1284.2
031400            GO TO FAIL-ROUTINE-WRITE.                             IF1284.2
031500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1284.2
031600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1284.2
031700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1284.2
031800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1284.2
031900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1284.2
032000     GO TO  FAIL-ROUTINE-EX.                                      IF1284.2
032100 FAIL-ROUTINE-WRITE.                                              IF1284.2
032200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1284.2
032300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1284.2
032400                              CORMA-ANSI-REFERENCE.               IF1284.2
032500     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1284.2
032600           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1284.2
032700           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1284.2
032800     ELSE                                                         IF1284.2
032900           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1284.2
033000     PERFORM WRITE-LINE.                                          IF1284.2
033100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1284.2
033200 FAIL-ROUTINE-EX. EXIT.                                           IF1284.2
033300 BAIL-OUT.                                                        IF1284.2
033400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1284.2
033500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1284.2
033600 BAIL-OUT-WRITE.                                                  IF1284.2
033700     MOVE CORRECT-A TO XXCORRECT.                                 IF1284.2
033800     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1284.2
033900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1284.2
034000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1284.2
034100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1284.2
034200 BAIL-OUT-EX. EXIT.                                               IF1284.2
034300 CCVS1-EXIT.                                                      IF1284.2
034400     EXIT.                                                        IF1284.2
034500********************************************************          IF1284.2
034600*                                                      *          IF1284.2
034700*    Intrinsic Function Tests         IF128A - ORD-MAX *          IF1284.2
034800*                                                      *          IF1284.2
034900********************************************************          IF1284.2
035000 SECT-IF128A SECTION.                                             IF1284.2
035100 F-ORD-MAX-INFO.                                                  IF1284.2
035200     MOVE     "See ref. A-61 2.32" TO ANSI-REFERENCE.             IF1284.2
035300     MOVE     "ORD-MAX Function"     TO FEATURE.                  IF1284.2
035400*****************TEST (a) ******************************          IF1284.2
035500 F-ORD-MAX-01.                                                    IF1284.2
035600     MOVE ZERO TO WS-INT.                                         IF1284.2
035700 F-ORD-MAX-TEST-01.                                               IF1284.2
035800     COMPUTE WS-INT = FUNCTION ORD-MAX(5, 3, 2, 8, 3, 1).         IF1284.2
035900     IF WS-INT = 4 THEN                                           IF1284.2
036000                        PERFORM PASS                              IF1284.2
036100     ELSE                                                         IF1284.2
036200                        MOVE 4  TO CORRECT-N                      IF1284.2
036300                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
036400                        PERFORM FAIL.                             IF1284.2
036500     GO TO F-ORD-MAX-WRITE-01.                                    IF1284.2
036600 F-ORD-MAX-DELETE-01.                                             IF1284.2
036700     PERFORM  DE-LETE.                                            IF1284.2
036800     GO TO    F-ORD-MAX-WRITE-01.                                 IF1284.2
036900 F-ORD-MAX-WRITE-01.                                              IF1284.2
037000     MOVE "F-ORD-MAX-01" TO PAR-NAME.                             IF1284.2
037100     PERFORM  PRINT-DETAIL.                                       IF1284.2
037200*****************TEST (b) ******************************          IF1284.2
037300 F-ORD-MAX-TEST-02.                                               IF1284.2
037400     EVALUATE FUNCTION ORD-MAX(3, 2, 7, 1, 5)                     IF1284.2
037500     WHEN 3                                                       IF1284.2
037600                                 PERFORM PASS                     IF1284.2
037700                                 GO TO F-ORD-MAX-WRITE-02.        IF1284.2
037800     PERFORM FAIL.                                                IF1284.2
037900     GO TO F-ORD-MAX-WRITE-02.                                    IF1284.2
038000 F-ORD-MAX-DELETE-02.                                             IF1284.2
038100     PERFORM  DE-LETE.                                            IF1284.2
038200     GO TO    F-ORD-MAX-WRITE-02.                                 IF1284.2
038300 F-ORD-MAX-WRITE-02.                                              IF1284.2
038400     MOVE "F-ORD-MAX-02" TO PAR-NAME.                             IF1284.2
038500     PERFORM  PRINT-DETAIL.                                       IF1284.2
038600*****************TEST (c) ******************************          IF1284.2
038700 F-ORD-MAX-03.                                                    IF1284.2
038800     MOVE ZERO TO WS-INT.                                         IF1284.2
038900 F-ORD-MAX-TEST-03.                                               IF1284.2
039000     IF FUNCTION ORD-MAX(A, B, D) = 3 THEN                        IF1284.2
039100                        PERFORM PASS                              IF1284.2
039200     ELSE                                                         IF1284.2
039300                        PERFORM FAIL.                             IF1284.2
039400     GO TO F-ORD-MAX-WRITE-03.                                    IF1284.2
039500 F-ORD-MAX-DELETE-03.                                             IF1284.2
039600     PERFORM  DE-LETE.                                            IF1284.2
039700     GO TO    F-ORD-MAX-WRITE-03.                                 IF1284.2
039800 F-ORD-MAX-WRITE-03.                                              IF1284.2
039900     MOVE "F-ORD-MAX-03" TO PAR-NAME.                             IF1284.2
040000     PERFORM  PRINT-DETAIL.                                       IF1284.2
040100*****************TEST (d) ******************************          IF1284.2
040200 F-ORD-MAX-04.                                                    IF1284.2
040300     MOVE ZERO TO WS-INT.                                         IF1284.2
040400 F-ORD-MAX-TEST-04.                                               IF1284.2
040500     COMPUTE WS-INT = FUNCTION ORD-MAX(A, B, C).                  IF1284.2
040600     IF WS-INT = 2 THEN                                           IF1284.2
040700                        PERFORM PASS                              IF1284.2
040800     ELSE                                                         IF1284.2
040900                        MOVE 2   TO CORRECT-N                     IF1284.2
041000                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
041100                        PERFORM FAIL.                             IF1284.2
041200     GO TO F-ORD-MAX-WRITE-04.                                    IF1284.2
041300 F-ORD-MAX-DELETE-04.                                             IF1284.2
041400     PERFORM  DE-LETE.                                            IF1284.2
041500     GO TO    F-ORD-MAX-WRITE-04.                                 IF1284.2
041600 F-ORD-MAX-WRITE-04.                                              IF1284.2
041700     MOVE "F-ORD-MAX-04" TO PAR-NAME.                             IF1284.2
041800     PERFORM  PRINT-DETAIL.                                       IF1284.2
041900*****************TEST (e) ******************************          IF1284.2
042000 F-ORD-MAX-05.                                                    IF1284.2
042100     MOVE ZERO TO WS-INT.                                         IF1284.2
042200 F-ORD-MAX-TEST-05.                                               IF1284.2
042300     COMPUTE WS-INT = FUNCTION ORD-MAX(A, 4, B, 7, C, 9).         IF1284.2
042400     IF WS-INT = 6 THEN                                           IF1284.2
042500                        PERFORM PASS                              IF1284.2
042600     ELSE                                                         IF1284.2
042700                        MOVE 6    TO CORRECT-N                    IF1284.2
042800                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
042900                        PERFORM FAIL.                             IF1284.2
043000     GO TO F-ORD-MAX-WRITE-05.                                    IF1284.2
043100 F-ORD-MAX-DELETE-05.                                             IF1284.2
043200     PERFORM  DE-LETE.                                            IF1284.2
043300     GO TO    F-ORD-MAX-WRITE-05.                                 IF1284.2
043400 F-ORD-MAX-WRITE-05.                                              IF1284.2
043500     MOVE "F-ORD-MAX-05" TO PAR-NAME.                             IF1284.2
043600     PERFORM  PRINT-DETAIL.                                       IF1284.2
043700*****************TEST (f) ******************************          IF1284.2
043800 F-ORD-MAX-06.                                                    IF1284.2
043900     MOVE ZERO TO WS-INT.                                         IF1284.2
044000 F-ORD-MAX-TEST-06.                                               IF1284.2
044100     COMPUTE WS-INT = FUNCTION ORD-MAX(4, 9, A, 3).               IF1284.2
044200     IF WS-INT = 2 THEN                                           IF1284.2
044300                        PERFORM PASS                              IF1284.2
044400     ELSE                                                         IF1284.2
044500                        MOVE 2   TO CORRECT-N                     IF1284.2
044600                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
044700                        PERFORM FAIL.                             IF1284.2
044800     GO TO F-ORD-MAX-WRITE-06.                                    IF1284.2
044900 F-ORD-MAX-DELETE-06.                                             IF1284.2
045000     PERFORM  DE-LETE.                                            IF1284.2
045100     GO TO    F-ORD-MAX-WRITE-06.                                 IF1284.2
045200 F-ORD-MAX-WRITE-06.                                              IF1284.2
045300     MOVE "F-ORD-MAX-06" TO PAR-NAME.                             IF1284.2
045400     PERFORM  PRINT-DETAIL.                                       IF1284.2
045500*****************TEST (g) ******************************          IF1284.2
045600 F-ORD-MAX-07.                                                    IF1284.2
045700     MOVE ZERO TO WS-INT.                                         IF1284.2
045800 F-ORD-MAX-TEST-07.                                               IF1284.2
045900     COMPUTE WS-INT = FUNCTION ORD-MAX("A", I, "P").              IF1284.2
046000     IF WS-INT = 2 THEN                                           IF1284.2
046100                        PERFORM PASS                              IF1284.2
046200     ELSE                                                         IF1284.2
046300                        MOVE 2    TO CORRECT-N                    IF1284.2
046400                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
046500                        PERFORM FAIL.                             IF1284.2
046600     GO TO F-ORD-MAX-WRITE-07.                                    IF1284.2
046700 F-ORD-MAX-DELETE-07.                                             IF1284.2
046800     PERFORM  DE-LETE.                                            IF1284.2
046900     GO TO    F-ORD-MAX-WRITE-07.                                 IF1284.2
047000 F-ORD-MAX-WRITE-07.                                              IF1284.2
047100     MOVE "F-ORD-MAX-07" TO PAR-NAME.                             IF1284.2
047200     PERFORM  PRINT-DETAIL.                                       IF1284.2
047300*****************TEST (h) ******************************          IF1284.2
047400 F-ORD-MAX-08.                                                    IF1284.2
047500     MOVE ZERO TO WS-INT.                                         IF1284.2
047600 F-ORD-MAX-TEST-08.                                               IF1284.2
047700     COMPUTE WS-INT = FUNCTION ORD-MAX("S", "D", J).              IF1284.2
047800     IF WS-INT = 3 THEN                                           IF1284.2
047900                        PERFORM PASS                              IF1284.2
048000     ELSE                                                         IF1284.2
048100                        MOVE 3    TO CORRECT-N                    IF1284.2
048200                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
048300                        PERFORM FAIL.                             IF1284.2
048400     GO TO F-ORD-MAX-WRITE-08.                                    IF1284.2
048500 F-ORD-MAX-DELETE-08.                                             IF1284.2
048600     PERFORM  DE-LETE.                                            IF1284.2
048700     GO TO    F-ORD-MAX-WRITE-08.                                 IF1284.2
048800 F-ORD-MAX-WRITE-08.                                              IF1284.2
048900     MOVE "F-ORD-MAX-08" TO PAR-NAME.                             IF1284.2
049000     PERFORM  PRINT-DETAIL.                                       IF1284.2
049100*****************TEST (i) ******************************          IF1284.2
049200 F-ORD-MAX-09.                                                    IF1284.2
049300     MOVE ZERO TO WS-INT.                                         IF1284.2
049400 F-ORD-MAX-TEST-09.                                               IF1284.2
049500     COMPUTE WS-INT = FUNCTION ORD-MAX(A, 5, 5, A).               IF1284.2
049600     IF WS-INT = 1 THEN                                           IF1284.2
049700                        PERFORM PASS                              IF1284.2
049800     ELSE                                                         IF1284.2
049900                        MOVE 1   TO CORRECT-N                     IF1284.2
050000                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
050100                        PERFORM FAIL.                             IF1284.2
050200     GO TO F-ORD-MAX-WRITE-09.                                    IF1284.2
050300 F-ORD-MAX-DELETE-09.                                             IF1284.2
050400     PERFORM  DE-LETE.                                            IF1284.2
050500     GO TO    F-ORD-MAX-WRITE-09.                                 IF1284.2
050600 F-ORD-MAX-WRITE-09.                                              IF1284.2
050700     MOVE "F-ORD-MAX-09" TO PAR-NAME.                             IF1284.2
050800     PERFORM  PRINT-DETAIL.                                       IF1284.2
050900*****************TEST (j) ******************************          IF1284.2
051000 F-ORD-MAX-10.                                                    IF1284.2
051100     MOVE ZERO TO WS-INT.                                         IF1284.2
051200 F-ORD-MAX-TEST-10.                                               IF1284.2
051300     COMPUTE WS-INT = FUNCTION ORD-MAX(IND(1), IND(2), IND(3)).   IF1284.2
051400     IF WS-INT = 3 THEN                                           IF1284.2
051500                        PERFORM PASS                              IF1284.2
051600     ELSE                                                         IF1284.2
051700                        MOVE 3    TO CORRECT-N                    IF1284.2
051800                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
051900                        PERFORM FAIL.                             IF1284.2
052000     GO TO F-ORD-MAX-WRITE-10.                                    IF1284.2
052100 F-ORD-MAX-DELETE-10.                                             IF1284.2
052200     PERFORM  DE-LETE.                                            IF1284.2
052300     GO TO    F-ORD-MAX-WRITE-10.                                 IF1284.2
052400 F-ORD-MAX-WRITE-10.                                              IF1284.2
052500     MOVE "F-ORD-MAX-10" TO PAR-NAME.                             IF1284.2
052600     PERFORM  PRINT-DETAIL.                                       IF1284.2
052700*****************TEST (k) ******************************          IF1284.2
052800 F-ORD-MAX-11.                                                    IF1284.2
052900     MOVE ZERO TO WS-INT.                                         IF1284.2
053000 F-ORD-MAX-TEST-11.                                               IF1284.2
053100     COMPUTE WS-INT = FUNCTION ORD-MAX(IND(R), IND(P), IND(Q)).   IF1284.2
053200     IF WS-INT = 1 THEN                                           IF1284.2
053300                        PERFORM PASS                              IF1284.2
053400     ELSE                                                         IF1284.2
053500                        MOVE 1   TO CORRECT-N                     IF1284.2
053600                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
053700                        PERFORM FAIL.                             IF1284.2
053800     GO TO F-ORD-MAX-WRITE-11.                                    IF1284.2
053900 F-ORD-MAX-DELETE-11.                                             IF1284.2
054000     PERFORM  DE-LETE.                                            IF1284.2
054100     GO TO    F-ORD-MAX-WRITE-11.                                 IF1284.2
054200 F-ORD-MAX-WRITE-11.                                              IF1284.2
054300     MOVE "F-ORD-MAX-11" TO PAR-NAME.                             IF1284.2
054400     PERFORM  PRINT-DETAIL.                                       IF1284.2
054500*****************TEST (l) ******************************          IF1284.2
054600 F-ORD-MAX-12.                                                    IF1284.2
054700     MOVE ZERO TO WS-INT.                                         IF1284.2
054800 F-ORD-MAX-TEST-12.                                               IF1284.2
054900     COMPUTE WS-INT = FUNCTION ORD-MAX(IND(ALL)).                 IF1284.2
055000     IF WS-INT = 5 THEN                                           IF1284.2
055100                        PERFORM PASS                              IF1284.2
055200     ELSE                                                         IF1284.2
055300                        MOVE 5    TO CORRECT-N                    IF1284.2
055400                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
055500                        PERFORM FAIL.                             IF1284.2
055600     GO TO F-ORD-MAX-WRITE-12.                                    IF1284.2
055700 F-ORD-MAX-DELETE-12.                                             IF1284.2
055800     PERFORM  DE-LETE.                                            IF1284.2
055900     GO TO    F-ORD-MAX-WRITE-12.                                 IF1284.2
056000 F-ORD-MAX-WRITE-12.                                              IF1284.2
056100     MOVE "F-ORD-MAX-12" TO PAR-NAME.                             IF1284.2
056200     PERFORM  PRINT-DETAIL.                                       IF1284.2
056300*****************TEST (m) ******************************          IF1284.2
056400 F-ORD-MAX-13.                                                    IF1284.2
056500     MOVE ZERO TO WS-INT.                                         IF1284.2
056600 F-ORD-MAX-TEST-13.                                               IF1284.2
056700     COMPUTE WS-INT = FUNCTION ORD-MAX(                           IF1284.2
056800                           FUNCTION ORD-MAX(1, 4), 3, 1).         IF1284.2
056900     IF WS-INT = 2 THEN                                           IF1284.2
057000                        PERFORM PASS                              IF1284.2
057100     ELSE                                                         IF1284.2
057200                        MOVE 2    TO CORRECT-N                    IF1284.2
057300                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
057400                        PERFORM FAIL.                             IF1284.2
057500     GO TO F-ORD-MAX-WRITE-13.                                    IF1284.2
057600 F-ORD-MAX-DELETE-13.                                             IF1284.2
057700     PERFORM  DE-LETE.                                            IF1284.2
057800     GO TO    F-ORD-MAX-WRITE-13.                                 IF1284.2
057900 F-ORD-MAX-WRITE-13.                                              IF1284.2
058000     MOVE "F-ORD-MAX-13" TO PAR-NAME.                             IF1284.2
058100     PERFORM  PRINT-DETAIL.                                       IF1284.2
058200*****************TEST (n) ******************************          IF1284.2
058300 F-ORD-MAX-14.                                                    IF1284.2
058400     MOVE ZERO TO WS-INT.                                         IF1284.2
058500 F-ORD-MAX-TEST-14.                                               IF1284.2
058600     COMPUTE WS-INT = FUNCTION ORD-MAX(2, 3, C) + A.              IF1284.2
058700     IF WS-INT = 8 THEN                                           IF1284.2
058800                        PERFORM PASS                              IF1284.2
058900     ELSE                                                         IF1284.2
059000                        MOVE 8    TO CORRECT-N                    IF1284.2
059100                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
059200                        PERFORM FAIL.                             IF1284.2
059300     GO TO F-ORD-MAX-WRITE-14.                                    IF1284.2
059400 F-ORD-MAX-DELETE-14.                                             IF1284.2
059500     PERFORM  DE-LETE.                                            IF1284.2
059600     GO TO    F-ORD-MAX-WRITE-14.                                 IF1284.2
059700 F-ORD-MAX-WRITE-14.                                              IF1284.2
059800     MOVE "F-ORD-MAX-14" TO PAR-NAME.                             IF1284.2
059900     PERFORM  PRINT-DETAIL.                                       IF1284.2
060000*****************TEST (o) ******************************          IF1284.2
060100 F-ORD-MAX-15.                                                    IF1284.2
060200     MOVE ZERO TO WS-INT.                                         IF1284.2
060300 F-ORD-MAX-TEST-15.                                               IF1284.2
060400     COMPUTE WS-INT = FUNCTION ORD-MAX(2, 3, A) +                 IF1284.2
060500                      FUNCTION ORD-MAX(1, 1).                     IF1284.2
060600     IF WS-INT = 4 THEN                                           IF1284.2
060700                        PERFORM PASS                              IF1284.2
060800     ELSE                                                         IF1284.2
060900                        MOVE 4   TO CORRECT-N                     IF1284.2
061000                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
061100                        PERFORM FAIL.                             IF1284.2
061200     GO TO F-ORD-MAX-WRITE-15.                                    IF1284.2
061300 F-ORD-MAX-DELETE-15.                                             IF1284.2
061400     PERFORM  DE-LETE.                                            IF1284.2
061500     GO TO    F-ORD-MAX-WRITE-15.                                 IF1284.2
061600 F-ORD-MAX-WRITE-15.                                              IF1284.2
061700     MOVE "F-ORD-MAX-15" TO PAR-NAME.                             IF1284.2
061800     PERFORM  PRINT-DETAIL.                                       IF1284.2
061900*****************SPECIAL TEST 1******************************     IF1284.2
062000 F-ORD-MAX-16.                                                    IF1284.2
062100     MOVE 1 TO ARG1                                               IF1284.2
062200     PERFORM F-ORD-MAX-TEST-16                                    IF1284.2
062300         UNTIL FUNCTION ORD-MAX (5, ARG1) = 2.                    IF1284.2
062400*                                                                 IF1284.2
062500**    when ARG1 = 6 , ORD-MAX(5,ARG1) = 2                         IF1284.2
062600*                                                                 IF1284.2
062700     IF ARG1 = 6 THEN                                             IF1284.2
062800                        PERFORM PASS                              IF1284.2
062900     ELSE                                                         IF1284.2
063000                        MOVE 6 TO CORRECT-N                       IF1284.2
063100                        MOVE WS-INT TO COMPUTED-N                 IF1284.2
063200                        PERFORM FAIL.                             IF1284.2
063300     GO TO F-ORD-MAX-WRITE-16.                                    IF1284.2
063400*                                                                 IF1284.2
063500 F-ORD-MAX-TEST-16.                                               IF1284.2
063600     COMPUTE ARG1 = ARG1 + 1.                                     IF1284.2
063700*                                                                 IF1284.2
063800 F-ORD-MAX-DELETE-16.                                             IF1284.2
063900     PERFORM  DE-LETE.                                            IF1284.2
064000     GO TO    F-ORD-MAX-WRITE-16.                                 IF1284.2
064100 F-ORD-MAX-WRITE-16.                                              IF1284.2
064200     MOVE "F-ORD-MAX-16" TO PAR-NAME.                             IF1284.2
064300     PERFORM  PRINT-DETAIL.                                       IF1284.2
064400*******************END OF TESTS**************************         IF1284.2
064500 CCVS-EXIT SECTION.                                               IF1284.2
064600 CCVS-999999.                                                     IF1284.2
064700     GO TO CLOSE-FILES.                                           IF1284.2
      *END-OF,IF128A                                                                  
