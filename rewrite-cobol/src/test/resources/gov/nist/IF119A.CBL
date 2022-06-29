      *HEADER,COBOL,IF119A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1194.2
000200 PROGRAM-ID.                                                      IF1194.2
000300     IF119A.                                                      IF1194.2
000400                                                                  IF1194.2
000500***********************************************************       IF1194.2
000600*                                                         *       IF1194.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1194.2
000800* It contains tests for the Intrinsic Function MAX.       *       IF1194.2
000900*                                                         *       IF1194.2
001000***********************************************************       IF1194.2
001100 ENVIRONMENT DIVISION.                                            IF1194.2
001200 CONFIGURATION SECTION.                                           IF1194.2
001300 SOURCE-COMPUTER.                                                 IF1194.2
001400     XXXXX082.                                                    IF1194.2
001500 OBJECT-COMPUTER.                                                 IF1194.2
001600     XXXXX083                                                     IF1194.2
001700     PROGRAM COLLATING SEQUENCE IS PRG-COLL-SEQ.                  IF1194.2
001800 SPECIAL-NAMES.                                                   IF1194.2
001900     ALPHABET PRG-COLL-SEQ IS                                     IF1194.2
002000     STANDARD-2.                                                  IF1194.2
002100 INPUT-OUTPUT SECTION.                                            IF1194.2
002200 FILE-CONTROL.                                                    IF1194.2
002300     SELECT PRINT-FILE ASSIGN TO                                  IF1194.2
002400     XXXXX055.                                                    IF1194.2
002500 DATA DIVISION.                                                   IF1194.2
002600 FILE SECTION.                                                    IF1194.2
002700 FD  PRINT-FILE.                                                  IF1194.2
002800 01  PRINT-REC PICTURE X(120).                                    IF1194.2
002900 01  DUMMY-RECORD PICTURE X(120).                                 IF1194.2
003000 WORKING-STORAGE SECTION.                                         IF1194.2
003100***********************************************************       IF1194.2
003200* Variables specific to the Intrinsic Function Test IF119A*       IF1194.2
003300***********************************************************       IF1194.2
003400 01  A                   PIC S9(10)          VALUE 5.             IF1194.2
003500 01  B                   PIC S9(10)          VALUE 7.             IF1194.2
003600 01  C                   PIC S9(10)          VALUE -4.            IF1194.2
003700 01  D                   PIC S9(10)          VALUE 10.            IF1194.2
003800 01  E                   PIC S9(5)V9(5)      VALUE 34.26.         IF1194.2
003900 01  F                   PIC S9(5)V9(5)      VALUE -8.32.         IF1194.2
004000 01  G                   PIC S9(5)V9(5)      VALUE 4.08.          IF1194.2
004100 01  H                   PIC S9(5)V9(5)      VALUE -5.3.          IF1194.2
004200 01  I                   PIC X               VALUE "R".           IF1194.2
004300 01  J                   PIC X               VALUE "U".           IF1194.2
004400 01  M                   PIC S9(10)          VALUE 1.             IF1194.2
004500 01  N                   PIC S9(10)          VALUE 3.             IF1194.2
004600 01  O                   PIC S9(10)          VALUE 5.             IF1194.2
004700 01  ARG1                PIC S9(10)          VALUE 1.             IF1194.2
004800 01  ARR                                     VALUE "40537".       IF1194.2
004900     02  IND OCCURS 5 TIMES PIC 9.                                IF1194.2
005000 01  TEMP                PIC S9(10).                              IF1194.2
005100 01  WS-NUM              PIC S9(6)V9(6).                          IF1194.2
005200 01  WS-ANUM             PIC X.                                   IF1194.2
005300 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1194.2
005400 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1194.2
005500*                                                                 IF1194.2
005600**********************************************************        IF1194.2
005700*                                                                 IF1194.2
005800 01  TEST-RESULTS.                                                IF1194.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IF1194.2
006000     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1194.2
006100     02 FILLER                   PIC X      VALUE SPACE.          IF1194.2
006200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1194.2
006300     02 FILLER                   PIC X      VALUE SPACE.          IF1194.2
006400     02  PAR-NAME.                                                IF1194.2
006500       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1194.2
006600       03  PARDOT-X              PIC X      VALUE SPACE.          IF1194.2
006700       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1194.2
006800     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1194.2
006900     02 RE-MARK                  PIC X(61).                       IF1194.2
007000 01  TEST-COMPUTED.                                               IF1194.2
007100     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1194.2
007200     02 FILLER                   PIC X(17)  VALUE                 IF1194.2
007300            "       COMPUTED=".                                   IF1194.2
007400     02 COMPUTED-X.                                               IF1194.2
007500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1194.2
007600     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1194.2
007700                                 PIC -9(9).9(9).                  IF1194.2
007800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1194.2
007900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1194.2
008000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1194.2
008100     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1194.2
008200         04 COMPUTED-18V0                    PIC -9(18).          IF1194.2
008300         04 FILLER                           PIC X.               IF1194.2
008400     03 FILLER PIC X(50) VALUE SPACE.                             IF1194.2
008500 01  TEST-CORRECT.                                                IF1194.2
008600     02 FILLER PIC X(30) VALUE SPACE.                             IF1194.2
008700     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1194.2
008800     02 CORRECT-X.                                                IF1194.2
008900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1194.2
009000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1194.2
009100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1194.2
009200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1194.2
009300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1194.2
009400     03      CR-18V0 REDEFINES CORRECT-A.                         IF1194.2
009500         04 CORRECT-18V0                     PIC -9(18).          IF1194.2
009600         04 FILLER                           PIC X.               IF1194.2
009700     03 FILLER PIC X(2) VALUE SPACE.                              IF1194.2
009800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1194.2
009900 01  TEST-CORRECT-MIN.                                            IF1194.2
010000     02 FILLER PIC X(30) VALUE SPACE.                             IF1194.2
010100     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1194.2
010200     02 CORRECTMI-X.                                              IF1194.2
010300     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1194.2
010400     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1194.2
010500     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1194.2
010600     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1194.2
010700     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1194.2
010800     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1194.2
010900         04 CORRECTMI-18V0                     PIC -9(18).        IF1194.2
011000         04 FILLER                           PIC X.               IF1194.2
011100     03 FILLER PIC X(2) VALUE SPACE.                              IF1194.2
011200     03 FILLER                           PIC X(48) VALUE SPACE.   IF1194.2
011300 01  TEST-CORRECT-MAX.                                            IF1194.2
011400     02 FILLER PIC X(30) VALUE SPACE.                             IF1194.2
011500     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1194.2
011600     02 CORRECTMA-X.                                              IF1194.2
011700     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1194.2
011800     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1194.2
011900     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1194.2
012000     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1194.2
012100     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1194.2
012200     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1194.2
012300         04 CORRECTMA-18V0                     PIC -9(18).        IF1194.2
012400         04 FILLER                           PIC X.               IF1194.2
012500     03 FILLER PIC X(2) VALUE SPACE.                              IF1194.2
012600     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1194.2
012700 01  CCVS-C-1.                                                    IF1194.2
012800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1194.2
012900-    "SS  PARAGRAPH-NAME                                          IF1194.2
013000-    "       REMARKS".                                            IF1194.2
013100     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1194.2
013200 01  CCVS-C-2.                                                    IF1194.2
013300     02 FILLER                     PIC X        VALUE SPACE.      IF1194.2
013400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1194.2
013500     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1194.2
013600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1194.2
013700     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1194.2
013800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1194.2
013900 01  REC-CT                        PIC 99       VALUE ZERO.       IF1194.2
014000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1194.2
014100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1194.2
014200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1194.2
014300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1194.2
014400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1194.2
014500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1194.2
014600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1194.2
014700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1194.2
014800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1194.2
014900 01  CCVS-H-1.                                                    IF1194.2
015000     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1194.2
015100     02  FILLER                    PIC X(42)    VALUE             IF1194.2
015200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1194.2
015300     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1194.2
015400 01  CCVS-H-2A.                                                   IF1194.2
015500   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1194.2
015600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1194.2
015700   02  FILLER                        PIC XXXX   VALUE             IF1194.2
015800     "4.2 ".                                                      IF1194.2
015900   02  FILLER                        PIC X(28)  VALUE             IF1194.2
016000            " COPY - NOT FOR DISTRIBUTION".                       IF1194.2
016100   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1194.2
016200                                                                  IF1194.2
016300 01  CCVS-H-2B.                                                   IF1194.2
016400   02  FILLER                        PIC X(15)  VALUE             IF1194.2
016500            "TEST RESULT OF ".                                    IF1194.2
016600   02  TEST-ID                       PIC X(9).                    IF1194.2
016700   02  FILLER                        PIC X(4)   VALUE             IF1194.2
016800            " IN ".                                               IF1194.2
016900   02  FILLER                        PIC X(12)  VALUE             IF1194.2
017000     " HIGH       ".                                              IF1194.2
017100   02  FILLER                        PIC X(22)  VALUE             IF1194.2
017200            " LEVEL VALIDATION FOR ".                             IF1194.2
017300   02  FILLER                        PIC X(58)  VALUE             IF1194.2
017400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1194.2
017500 01  CCVS-H-3.                                                    IF1194.2
017600     02  FILLER                      PIC X(34)  VALUE             IF1194.2
017700            " FOR OFFICIAL USE ONLY    ".                         IF1194.2
017800     02  FILLER                      PIC X(58)  VALUE             IF1194.2
017900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1194.2
018000     02  FILLER                      PIC X(28)  VALUE             IF1194.2
018100            "  COPYRIGHT   1985 ".                                IF1194.2
018200 01  CCVS-E-1.                                                    IF1194.2
018300     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1194.2
018400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1194.2
018500     02 ID-AGAIN                     PIC X(9).                    IF1194.2
018600     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1194.2
018700 01  CCVS-E-2.                                                    IF1194.2
018800     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1194.2
018900     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1194.2
019000     02 CCVS-E-2-2.                                               IF1194.2
019100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1194.2
019200         03 FILLER                   PIC X      VALUE SPACE.      IF1194.2
019300         03 ENDER-DESC               PIC X(44)  VALUE             IF1194.2
019400            "ERRORS ENCOUNTERED".                                 IF1194.2
019500 01  CCVS-E-3.                                                    IF1194.2
019600     02  FILLER                      PIC X(22)  VALUE             IF1194.2
019700            " FOR OFFICIAL USE ONLY".                             IF1194.2
019800     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1194.2
019900     02  FILLER                      PIC X(58)  VALUE             IF1194.2
020000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1194.2
020100     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1194.2
020200     02 FILLER                       PIC X(15)  VALUE             IF1194.2
020300             " COPYRIGHT 1985".                                   IF1194.2
020400 01  CCVS-E-4.                                                    IF1194.2
020500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1194.2
020600     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1194.2
020700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1194.2
020800     02 FILLER                       PIC X(40)  VALUE             IF1194.2
020900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1194.2
021000 01  XXINFO.                                                      IF1194.2
021100     02 FILLER                       PIC X(19)  VALUE             IF1194.2
021200            "*** INFORMATION ***".                                IF1194.2
021300     02 INFO-TEXT.                                                IF1194.2
021400       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1194.2
021500       04 XXCOMPUTED                 PIC X(20).                   IF1194.2
021600       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1194.2
021700       04 XXCORRECT                  PIC X(20).                   IF1194.2
021800     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1194.2
021900 01  HYPHEN-LINE.                                                 IF1194.2
022000     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1194.2
022100     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1194.2
022200-    "*****************************************".                 IF1194.2
022300     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1194.2
022400-    "******************************".                            IF1194.2
022500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1194.2
022600     "IF119A".                                                    IF1194.2
022700 PROCEDURE DIVISION.                                              IF1194.2
022800 CCVS1 SECTION.                                                   IF1194.2
022900 OPEN-FILES.                                                      IF1194.2
023000     OPEN     OUTPUT PRINT-FILE.                                  IF1194.2
023100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1194.2
023200     MOVE    SPACE TO TEST-RESULTS.                               IF1194.2
023300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1194.2
023400     GO TO CCVS1-EXIT.                                            IF1194.2
023500 CLOSE-FILES.                                                     IF1194.2
023600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1194.2
023700 TERMINATE-CCVS.                                                  IF1194.2
023800     STOP     RUN.                                                IF1194.2
023900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1194.2
024000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1194.2
024100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1194.2
024200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1194.2
024300     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1194.2
024400 PRINT-DETAIL.                                                    IF1194.2
024500     IF REC-CT NOT EQUAL TO ZERO                                  IF1194.2
024600             MOVE "." TO PARDOT-X                                 IF1194.2
024700             MOVE REC-CT TO DOTVALUE.                             IF1194.2
024800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1194.2
024900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1194.2
025000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1194.2
025100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1194.2
025200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1194.2
025300     MOVE SPACE TO CORRECT-X.                                     IF1194.2
025400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1194.2
025500     MOVE     SPACE TO RE-MARK.                                   IF1194.2
025600 HEAD-ROUTINE.                                                    IF1194.2
025700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1194.2
025800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1194.2
025900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1194.2
026000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1194.2
026100 COLUMN-NAMES-ROUTINE.                                            IF1194.2
026200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1194.2
026300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1194.2
026400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1194.2
026500 END-ROUTINE.                                                     IF1194.2
026600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1194.2
026700 END-RTN-EXIT.                                                    IF1194.2
026800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1194.2
026900 END-ROUTINE-1.                                                   IF1194.2
027000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1194.2
027100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1194.2
027200      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1194.2
027300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1194.2
027400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1194.2
027500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1194.2
027600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1194.2
027700  END-ROUTINE-12.                                                 IF1194.2
027800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1194.2
027900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1194.2
028000         MOVE "NO " TO ERROR-TOTAL                                IF1194.2
028100         ELSE                                                     IF1194.2
028200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1194.2
028300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1194.2
028400     PERFORM WRITE-LINE.                                          IF1194.2
028500 END-ROUTINE-13.                                                  IF1194.2
028600     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1194.2
028700         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1194.2
028800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1194.2
028900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1194.2
029000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1194.2
029100      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1194.2
029200          MOVE "NO " TO ERROR-TOTAL                               IF1194.2
029300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1194.2
029400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1194.2
029500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1194.2
029600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1194.2
029700 WRITE-LINE.                                                      IF1194.2
029800     ADD 1 TO RECORD-COUNT.                                       IF1194.2
029900*    IF RECORD-COUNT GREATER 42                                   IF1194.2
030000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1194.2
030100*        MOVE SPACE TO DUMMY-RECORD                               IF1194.2
030200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1194.2
030300*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1194.2
030400*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1194.2
030500*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1194.2
030600*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1194.2
030700*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1194.2
030800*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1194.2
030900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1194.2
031000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1194.2
031100*        MOVE ZERO TO RECORD-COUNT.                               IF1194.2
031200     PERFORM WRT-LN.                                              IF1194.2
031300 WRT-LN.                                                          IF1194.2
031400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1194.2
031500     MOVE SPACE TO DUMMY-RECORD.                                  IF1194.2
031600 BLANK-LINE-PRINT.                                                IF1194.2
031700     PERFORM WRT-LN.                                              IF1194.2
031800 FAIL-ROUTINE.                                                    IF1194.2
031900     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1194.2
032000            GO TO FAIL-ROUTINE-WRITE.                             IF1194.2
032100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1194.2
032200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1194.2
032300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1194.2
032400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1194.2
032500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1194.2
032600     GO TO  FAIL-ROUTINE-EX.                                      IF1194.2
032700 FAIL-ROUTINE-WRITE.                                              IF1194.2
032800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1194.2
032900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1194.2
033000                              CORMA-ANSI-REFERENCE.               IF1194.2
033100     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1194.2
033200           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1194.2
033300           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1194.2
033400     ELSE                                                         IF1194.2
033500           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1194.2
033600     PERFORM WRITE-LINE.                                          IF1194.2
033700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1194.2
033800 FAIL-ROUTINE-EX. EXIT.                                           IF1194.2
033900 BAIL-OUT.                                                        IF1194.2
034000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1194.2
034100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1194.2
034200 BAIL-OUT-WRITE.                                                  IF1194.2
034300     MOVE CORRECT-A TO XXCORRECT.                                 IF1194.2
034400     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1194.2
034500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1194.2
034600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1194.2
034700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1194.2
034800 BAIL-OUT-EX. EXIT.                                               IF1194.2
034900 CCVS1-EXIT.                                                      IF1194.2
035000     EXIT.                                                        IF1194.2
035100********************************************************          IF1194.2
035200*                                                      *          IF1194.2
035300*    Intrinsic Function Tests         IF119A - MAX     *          IF1194.2
035400*                                                      *          IF1194.2
035500********************************************************          IF1194.2
035600 SECT-IF119A SECTION.                                             IF1194.2
035700 F-MAX-INFO.                                                      IF1194.2
035800     MOVE     "See ref. A-52 2.23" TO ANSI-REFERENCE.             IF1194.2
035900     MOVE     "MAX Function" TO FEATURE.                          IF1194.2
036000*****************TEST (a) - SIMPLE TEST*****************          IF1194.2
036100 F-MAX-01.                                                        IF1194.2
036200     MOVE ZERO TO WS-NUM.                                         IF1194.2
036300 F-MAX-TEST-01.                                                   IF1194.2
036400     COMPUTE WS-NUM = FUNCTION MAX(5, 6, 10, 3, 7).               IF1194.2
036500     IF WS-NUM = 10 THEN                                          IF1194.2
036600                    PERFORM PASS                                  IF1194.2
036700     ELSE                                                         IF1194.2
036800                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
036900                    MOVE 10 TO CORRECT-N                          IF1194.2
037000                    PERFORM FAIL.                                 IF1194.2
037100     GO TO F-MAX-WRITE-01.                                        IF1194.2
037200 F-MAX-DELETE-01.                                                 IF1194.2
037300     PERFORM  DE-LETE.                                            IF1194.2
037400     GO TO    F-MAX-WRITE-01.                                     IF1194.2
037500 F-MAX-WRITE-01.                                                  IF1194.2
037600     MOVE "F-MAX-01" TO PAR-NAME.                                 IF1194.2
037700     PERFORM  PRINT-DETAIL.                                       IF1194.2
037800*****************TEST (b) - SIMPLE TEST*****************          IF1194.2
037900 F-MAX-02.                                                        IF1194.2
038000     EVALUATE FUNCTION MAX(-4, 7, 304, 3, -8)                     IF1194.2
038100     WHEN 304                                                     IF1194.2
038200                    PERFORM PASS                                  IF1194.2
038300     WHEN OTHER                                                   IF1194.2
038400                    PERFORM FAIL.                                 IF1194.2
038500     GO TO F-MAX-WRITE-02.                                        IF1194.2
038600 F-MAX-DELETE-02.                                                 IF1194.2
038700     PERFORM  DE-LETE.                                            IF1194.2
038800     GO TO    F-MAX-WRITE-02.                                     IF1194.2
038900 F-MAX-WRITE-02.                                                  IF1194.2
039000     MOVE "F-MAX-02" TO PAR-NAME.                                 IF1194.2
039100     PERFORM  PRINT-DETAIL.                                       IF1194.2
039200*****************TEST (c) - SIMPLE TEST*****************          IF1194.2
039300 F-MAX-03.                                                        IF1194.2
039400     IF (FUNCTION MAX(4.3, 2.6, 7.3, 9.1) >= 9.09982) AND         IF1194.2
039500        (FUNCTION MAX(4.3, 2.6, 7.3, 9.1) <= 9.10018)             IF1194.2
039600                    PERFORM PASS                                  IF1194.2
039700     ELSE                                                         IF1194.2
039800                    PERFORM FAIL.                                 IF1194.2
039900     GO TO F-MAX-WRITE-03.                                        IF1194.2
040000 F-MAX-DELETE-03.                                                 IF1194.2
040100     PERFORM  DE-LETE.                                            IF1194.2
040200     GO TO    F-MAX-WRITE-03.                                     IF1194.2
040300 F-MAX-WRITE-03.                                                  IF1194.2
040400     MOVE "F-MAX-03" TO PAR-NAME.                                 IF1194.2
040500     PERFORM  PRINT-DETAIL.                                       IF1194.2
040600*****************TEST (d) - SIMPLE TEST*****************          IF1194.2
040700 F-MAX-04.                                                        IF1194.2
040800     MOVE ZERO TO WS-NUM.                                         IF1194.2
040900 F-MAX-TEST-04.                                                   IF1194.2
041000     COMPUTE WS-NUM = FUNCTION MAX(-4.3, 10.2, -0.7, 3.9).        IF1194.2
041100     IF (WS-NUM >= 10.1998) AND                                   IF1194.2
041200        (WS-NUM <= 10.2002)                                       IF1194.2
041300                    PERFORM PASS                                  IF1194.2
041400     ELSE                                                         IF1194.2
041500                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
041600                    MOVE 10.2 TO CORRECT-N                        IF1194.2
041700                    PERFORM FAIL.                                 IF1194.2
041800     GO TO F-MAX-WRITE-04.                                        IF1194.2
041900 F-MAX-DELETE-04.                                                 IF1194.2
042000     PERFORM  DE-LETE.                                            IF1194.2
042100     GO TO    F-MAX-WRITE-04.                                     IF1194.2
042200 F-MAX-WRITE-04.                                                  IF1194.2
042300     MOVE "F-MAX-04" TO PAR-NAME.                                 IF1194.2
042400     PERFORM  PRINT-DETAIL.                                       IF1194.2
042500*****************TEST (e) - SIMPLE TEST*****************          IF1194.2
042600 F-MAX-05.                                                        IF1194.2
042700     MOVE ZERO TO WS-NUM.                                         IF1194.2
042800 F-MAX-TEST-05.                                                   IF1194.2
042900     COMPUTE WS-NUM = FUNCTION MAX(A, B, D).                      IF1194.2
043000     IF WS-NUM = 10 THEN                                          IF1194.2
043100                    PERFORM PASS                                  IF1194.2
043200     ELSE                                                         IF1194.2
043300                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
043400                    MOVE 10 TO CORRECT-N                          IF1194.2
043500                    PERFORM FAIL.                                 IF1194.2
043600     GO TO F-MAX-WRITE-05.                                        IF1194.2
043700 F-MAX-DELETE-05.                                                 IF1194.2
043800     PERFORM  DE-LETE.                                            IF1194.2
043900     GO TO    F-MAX-WRITE-05.                                     IF1194.2
044000 F-MAX-WRITE-05.                                                  IF1194.2
044100     MOVE "F-MAX-05" TO PAR-NAME.                                 IF1194.2
044200     PERFORM  PRINT-DETAIL.                                       IF1194.2
044300*****************TEST (f) - SIMPLE TEST*****************          IF1194.2
044400 F-MAX-06.                                                        IF1194.2
044500     MOVE ZERO TO WS-NUM.                                         IF1194.2
044600 F-MAX-TEST-06.                                                   IF1194.2
044700     COMPUTE WS-NUM = FUNCTION MAX(A, B, C).                      IF1194.2
044800     IF WS-NUM = 7 THEN                                           IF1194.2
044900                    PERFORM PASS                                  IF1194.2
045000     ELSE                                                         IF1194.2
045100                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
045200                    MOVE 7 TO CORRECT-N                           IF1194.2
045300                    PERFORM FAIL.                                 IF1194.2
045400     GO TO F-MAX-WRITE-06.                                        IF1194.2
045500 F-MAX-DELETE-06.                                                 IF1194.2
045600     PERFORM  DE-LETE.                                            IF1194.2
045700     GO TO    F-MAX-WRITE-06.                                     IF1194.2
045800 F-MAX-WRITE-06.                                                  IF1194.2
045900     MOVE "F-MAX-06" TO PAR-NAME.                                 IF1194.2
046000     PERFORM  PRINT-DETAIL.                                       IF1194.2
046100*****************TEST (g) - SIMPLE TEST*****************          IF1194.2
046200 F-MAX-07.                                                        IF1194.2
046300     MOVE ZERO TO WS-NUM.                                         IF1194.2
046400 F-MAX-TEST-07.                                                   IF1194.2
046500     COMPUTE WS-NUM = FUNCTION MAX(E, G).                         IF1194.2
046600     IF (WS-NUM >= 34.2593) AND                                   IF1194.2
046700        (WS-NUM <= 34.2607)                                       IF1194.2
046800                    PERFORM PASS                                  IF1194.2
046900     ELSE                                                         IF1194.2
047000                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
047100                    MOVE 34.26 TO CORRECT-N                       IF1194.2
047200                    PERFORM FAIL.                                 IF1194.2
047300     GO TO F-MAX-WRITE-07.                                        IF1194.2
047400 F-MAX-DELETE-07.                                                 IF1194.2
047500     PERFORM  DE-LETE.                                            IF1194.2
047600     GO TO    F-MAX-WRITE-07.                                     IF1194.2
047700 F-MAX-WRITE-07.                                                  IF1194.2
047800     MOVE "F-MAX-07" TO PAR-NAME.                                 IF1194.2
047900     PERFORM  PRINT-DETAIL.                                       IF1194.2
048000*****************TEST (h) - SIMPLE TEST*****************          IF1194.2
048100 F-MAX-08.                                                        IF1194.2
048200     MOVE ZERO TO WS-NUM.                                         IF1194.2
048300 F-MAX-TEST-08.                                                   IF1194.2
048400     COMPUTE WS-NUM = FUNCTION MAX(F, G, H).                      IF1194.2
048500     IF (WS-NUM >= 4.07992) AND                                   IF1194.2
048600        (WS-NUM <= 4.08008)                                       IF1194.2
048700                    PERFORM PASS                                  IF1194.2
048800     ELSE                                                         IF1194.2
048900                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
049000                    MOVE 4.08 TO CORRECT-N                        IF1194.2
049100                    PERFORM FAIL.                                 IF1194.2
049200     GO TO F-MAX-WRITE-08.                                        IF1194.2
049300 F-MAX-DELETE-08.                                                 IF1194.2
049400     PERFORM  DE-LETE.                                            IF1194.2
049500     GO TO    F-MAX-WRITE-08.                                     IF1194.2
049600 F-MAX-WRITE-08.                                                  IF1194.2
049700     MOVE "F-MAX-08" TO PAR-NAME.                                 IF1194.2
049800     PERFORM  PRINT-DETAIL.                                       IF1194.2
049900*****************TEST (i) - SIMPLE TEST*****************          IF1194.2
050000 F-MAX-09.                                                        IF1194.2
050100     MOVE ZERO TO WS-NUM.                                         IF1194.2
050200 F-MAX-TEST-09.                                                   IF1194.2
050300     COMPUTE WS-NUM = FUNCTION MAX(A, 4, 8, -10, C, 0).           IF1194.2
050400     IF WS-NUM = 8 THEN                                           IF1194.2
050500                    PERFORM PASS                                  IF1194.2
050600     ELSE                                                         IF1194.2
050700                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
050800                    MOVE 8 TO CORRECT-N                           IF1194.2
050900                    PERFORM FAIL.                                 IF1194.2
051000     GO TO F-MAX-WRITE-09.                                        IF1194.2
051100 F-MAX-DELETE-09.                                                 IF1194.2
051200     PERFORM  DE-LETE.                                            IF1194.2
051300     GO TO    F-MAX-WRITE-09.                                     IF1194.2
051400 F-MAX-WRITE-09.                                                  IF1194.2
051500     MOVE "F-MAX-09" TO PAR-NAME.                                 IF1194.2
051600     PERFORM  PRINT-DETAIL.                                       IF1194.2
051700*****************TEST (j) - SIMPLE TEST*****************          IF1194.2
051800 F-MAX-10.                                                        IF1194.2
051900     MOVE ZERO TO WS-NUM.                                         IF1194.2
052000 F-MAX-TEST-10.                                                   IF1194.2
052100     COMPUTE WS-NUM = FUNCTION MAX(4, D, H, 6.3, -2.0).           IF1194.2
052200     IF (WS-NUM >= 9.9998) AND                                    IF1194.2
052300        (WS-NUM <= 10.0002)                                       IF1194.2
052400                    PERFORM PASS                                  IF1194.2
052500     ELSE                                                         IF1194.2
052600                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
052700                    MOVE 10  TO CORRECT-N                         IF1194.2
052800                    PERFORM FAIL.                                 IF1194.2
052900     GO TO F-MAX-WRITE-10.                                        IF1194.2
053000 F-MAX-DELETE-10.                                                 IF1194.2
053100     PERFORM  DE-LETE.                                            IF1194.2
053200     GO TO    F-MAX-WRITE-10.                                     IF1194.2
053300 F-MAX-WRITE-10.                                                  IF1194.2
053400     MOVE "F-MAX-10" TO PAR-NAME.                                 IF1194.2
053500     PERFORM  PRINT-DETAIL.                                       IF1194.2
053600*****************TEST (k) - SIMPLE TEST*****************          IF1194.2
053700 F-MAX-11.                                                        IF1194.2
053800     MOVE SPACES TO WS-ANUM.                                      IF1194.2
053900 F-MAX-TEST-11.                                                   IF1194.2
054000     MOVE FUNCTION MAX("R", I, "I", "a") TO WS-ANUM.              IF1194.2
054100     IF WS-ANUM = "a" THEN                                        IF1194.2
054200                    PERFORM PASS                                  IF1194.2
054300     ELSE                                                         IF1194.2
054400                    MOVE WS-ANUM TO COMPUTED-A                    IF1194.2
054500                    MOVE "a" TO CORRECT-A                         IF1194.2
054600                    PERFORM FAIL.                                 IF1194.2
054700     GO TO F-MAX-WRITE-11.                                        IF1194.2
054800 F-MAX-DELETE-11.                                                 IF1194.2
054900     PERFORM  DE-LETE.                                            IF1194.2
055000     GO TO    F-MAX-WRITE-11.                                     IF1194.2
055100 F-MAX-WRITE-11.                                                  IF1194.2
055200     MOVE "F-MAX-11" TO PAR-NAME.                                 IF1194.2
055300     PERFORM  PRINT-DETAIL.                                       IF1194.2
055400*****************TEST (l) - SIMPLE TEST*****************          IF1194.2
055500 F-MAX-12.                                                        IF1194.2
055600     MOVE ZERO TO WS-NUM.                                         IF1194.2
055700 F-MAX-TEST-12.                                                   IF1194.2
055800     MOVE FUNCTION MAX("A", J, "J") TO WS-ANUM.                   IF1194.2
055900     IF WS-ANUM = "U" THEN                                        IF1194.2
056000                    PERFORM PASS                                  IF1194.2
056100     ELSE                                                         IF1194.2
056200                    MOVE WS-ANUM TO COMPUTED-A                    IF1194.2
056300                    MOVE "U" TO CORRECT-A                         IF1194.2
056400                    PERFORM FAIL.                                 IF1194.2
056500     GO TO F-MAX-WRITE-12.                                        IF1194.2
056600 F-MAX-DELETE-12.                                                 IF1194.2
056700     PERFORM  DE-LETE.                                            IF1194.2
056800     GO TO    F-MAX-WRITE-12.                                     IF1194.2
056900 F-MAX-WRITE-12.                                                  IF1194.2
057000     MOVE "F-MAX-12" TO PAR-NAME.                                 IF1194.2
057100     PERFORM  PRINT-DETAIL.                                       IF1194.2
057200*****************TEST (m) - SIMPLE TEST*****************          IF1194.2
057300 F-MAX-13.                                                        IF1194.2
057400     MOVE ZERO TO WS-NUM.                                         IF1194.2
057500 F-MAX-TEST-13.                                                   IF1194.2
057600     COMPUTE WS-NUM = FUNCTION MAX(IND(M), IND(N), IND(O)).       IF1194.2
057700     IF WS-NUM = 7 THEN                                           IF1194.2
057800                    PERFORM PASS                                  IF1194.2
057900     ELSE                                                         IF1194.2
058000                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
058100                    MOVE 7 TO CORRECT-N                           IF1194.2
058200                    PERFORM FAIL.                                 IF1194.2
058300     GO TO F-MAX-WRITE-13.                                        IF1194.2
058400 F-MAX-DELETE-13.                                                 IF1194.2
058500     PERFORM  DE-LETE.                                            IF1194.2
058600     GO TO    F-MAX-WRITE-13.                                     IF1194.2
058700 F-MAX-WRITE-13.                                                  IF1194.2
058800     MOVE "F-MAX-13" TO PAR-NAME.                                 IF1194.2
058900     PERFORM  PRINT-DETAIL.                                       IF1194.2
059000*****************TEST (n) - SIMPLE TEST*****************          IF1194.2
059100 F-MAX-14.                                                        IF1194.2
059200     MOVE ZERO TO WS-NUM.                                         IF1194.2
059300 F-MAX-TEST-14.                                                   IF1194.2
059400     COMPUTE WS-NUM = FUNCTION MAX(IND(1), IND(2), IND(3)).       IF1194.2
059500     IF WS-NUM = 5 THEN                                           IF1194.2
059600                    PERFORM PASS                                  IF1194.2
059700     ELSE                                                         IF1194.2
059800                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
059900                    MOVE 5 TO CORRECT-N                           IF1194.2
060000                    PERFORM FAIL.                                 IF1194.2
060100     GO TO F-MAX-WRITE-14.                                        IF1194.2
060200 F-MAX-DELETE-14.                                                 IF1194.2
060300     PERFORM  DE-LETE.                                            IF1194.2
060400     GO TO    F-MAX-WRITE-14.                                     IF1194.2
060500 F-MAX-WRITE-14.                                                  IF1194.2
060600     MOVE "F-MAX-14" TO PAR-NAME.                                 IF1194.2
060700     PERFORM  PRINT-DETAIL.                                       IF1194.2
060800*****************TEST (o) - SIMPLE TEST*****************          IF1194.2
060900 F-MAX-15.                                                        IF1194.2
061000     MOVE ZERO TO WS-NUM.                                         IF1194.2
061100 F-MAX-TEST-15.                                                   IF1194.2
061200     COMPUTE WS-NUM = FUNCTION MAX(IND(ALL)).                     IF1194.2
061300     IF WS-NUM = 7 THEN                                           IF1194.2
061400                    PERFORM PASS                                  IF1194.2
061500     ELSE                                                         IF1194.2
061600                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
061700                    MOVE 7       TO CORRECT-N                     IF1194.2
061800                    PERFORM FAIL.                                 IF1194.2
061900     GO TO F-MAX-WRITE-15.                                        IF1194.2
062000 F-MAX-DELETE-15.                                                 IF1194.2
062100     PERFORM  DE-LETE.                                            IF1194.2
062200     GO TO    F-MAX-WRITE-15.                                     IF1194.2
062300 F-MAX-WRITE-15.                                                  IF1194.2
062400     MOVE "F-MAX-15" TO PAR-NAME.                                 IF1194.2
062500     PERFORM  PRINT-DETAIL.                                       IF1194.2
062600*****************TEST (q) - SIMPLE TEST*****************          IF1194.2
062700 F-MAX-17.                                                        IF1194.2
062800     MOVE ZERO TO WS-NUM.                                         IF1194.2
062900 F-MAX-TEST-17.                                                   IF1194.2
063000     COMPUTE WS-NUM =                                             IF1194.2
063100         FUNCTION MAX(31000, 310001, 78000, 29000, 12000).        IF1194.2
063200     IF WS-NUM = 310001 THEN                                      IF1194.2
063300                    PERFORM PASS                                  IF1194.2
063400     ELSE                                                         IF1194.2
063500                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
063600                    MOVE 310001 TO CORRECT-N                      IF1194.2
063700                    PERFORM FAIL.                                 IF1194.2
063800     GO TO F-MAX-WRITE-17.                                        IF1194.2
063900 F-MAX-DELETE-17.                                                 IF1194.2
064000     PERFORM  DE-LETE.                                            IF1194.2
064100     GO TO    F-MAX-WRITE-17.                                     IF1194.2
064200 F-MAX-WRITE-17.                                                  IF1194.2
064300     MOVE "F-MAX-17" TO PAR-NAME.                                 IF1194.2
064400     PERFORM  PRINT-DETAIL.                                       IF1194.2
064500*****************TEST (a) - COMPLEX TEST****************          IF1194.2
064600 F-MAX-18.                                                        IF1194.2
064700     MOVE ZERO TO WS-NUM.                                         IF1194.2
064800     MOVE  34.9993 TO MIN-RANGE.                                  IF1194.2
064900     MOVE  35.0007 TO MAX-RANGE.                                  IF1194.2
065000 F-MAX-TEST-18.                                                   IF1194.2
065100     COMPUTE WS-NUM = FUNCTION MAX(A * B, (C + 1) / 2, 3 + 4).    IF1194.2
065200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1194.2
065300        (WS-NUM <= MAX-RANGE) THEN                                IF1194.2
065400                    PERFORM PASS                                  IF1194.2
065500     ELSE                                                         IF1194.2
065600                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
065700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1194.2
065800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1194.2
065900                    PERFORM FAIL.                                 IF1194.2
066000     GO TO F-MAX-WRITE-18.                                        IF1194.2
066100 F-MAX-DELETE-18.                                                 IF1194.2
066200     PERFORM  DE-LETE.                                            IF1194.2
066300     GO TO    F-MAX-WRITE-18.                                     IF1194.2
066400 F-MAX-WRITE-18.                                                  IF1194.2
066500     MOVE "F-MAX-18" TO PAR-NAME.                                 IF1194.2
066600     PERFORM  PRINT-DETAIL.                                       IF1194.2
066700*****************TEST (b) - COMPLEX TEST****************          IF1194.2
066800 F-MAX-19.                                                        IF1194.2
066900     MOVE ZERO TO WS-NUM.                                         IF1194.2
067000     MOVE  38.2592 TO MIN-RANGE.                                  IF1194.2
067100     MOVE  38.2608 TO MAX-RANGE.                                  IF1194.2
067200 F-MAX-TEST-19.                                                   IF1194.2
067300     COMPUTE WS-NUM = FUNCTION MAX(E + 4, H * 2, 5 + A).          IF1194.2
067400     IF (WS-NUM >= MIN-RANGE) AND                                 IF1194.2
067500        (WS-NUM <= MAX-RANGE) THEN                                IF1194.2
067600                    PERFORM PASS                                  IF1194.2
067700     ELSE                                                         IF1194.2
067800                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
067900                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1194.2
068000                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1194.2
068100                    PERFORM FAIL.                                 IF1194.2
068200     GO TO F-MAX-WRITE-19.                                        IF1194.2
068300 F-MAX-DELETE-19.                                                 IF1194.2
068400     PERFORM  DE-LETE.                                            IF1194.2
068500     GO TO    F-MAX-WRITE-19.                                     IF1194.2
068600 F-MAX-WRITE-19.                                                  IF1194.2
068700     MOVE "F-MAX-19" TO PAR-NAME.                                 IF1194.2
068800     PERFORM  PRINT-DETAIL.                                       IF1194.2
068900*****************TEST (c) - COMPLEX TEST****************          IF1194.2
069000 F-MAX-20.                                                        IF1194.2
069100     MOVE ZERO TO WS-NUM.                                         IF1194.2
069200     MOVE -7.00014 TO MIN-RANGE.                                  IF1194.2
069300     MOVE -6.99986 TO MAX-RANGE.                                  IF1194.2
069400 F-MAX-TEST-20.                                                   IF1194.2
069500     COMPUTE WS-NUM = FUNCTION MAX(-7, -9 + 2, -7).               IF1194.2
069600     IF (WS-NUM >= MIN-RANGE) AND                                 IF1194.2
069700        (WS-NUM <= MAX-RANGE) THEN                                IF1194.2
069800                    PERFORM PASS                                  IF1194.2
069900     ELSE                                                         IF1194.2
070000                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
070100                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1194.2
070200                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1194.2
070300                    PERFORM FAIL.                                 IF1194.2
070400     GO TO F-MAX-WRITE-20.                                        IF1194.2
070500 F-MAX-DELETE-20.                                                 IF1194.2
070600     PERFORM  DE-LETE.                                            IF1194.2
070700     GO TO    F-MAX-WRITE-20.                                     IF1194.2
070800 F-MAX-WRITE-20.                                                  IF1194.2
070900     MOVE "F-MAX-20" TO PAR-NAME.                                 IF1194.2
071000     PERFORM  PRINT-DETAIL.                                       IF1194.2
071100*****************TEST (d) - COMPLEX TEST****************          IF1194.2
071200 F-MAX-21.                                                        IF1194.2
071300     MOVE ZERO TO WS-NUM.                                         IF1194.2
071400     MOVE 49.9990 TO MIN-RANGE.                                   IF1194.2
071500     MOVE 50.0001 TO MAX-RANGE.                                   IF1194.2
071600 F-MAX-TEST-21.                                                   IF1194.2
071700     COMPUTE WS-NUM = FUNCTION MAX(FUNCTION MAX(14, A), E, 50).   IF1194.2
071800                                                                  IF1194.2
071900                                                                  IF1194.2
072000     IF (WS-NUM >= MIN-RANGE) AND                                 IF1194.2
072100        (WS-NUM <= MAX-RANGE) THEN                                IF1194.2
072200                    PERFORM PASS                                  IF1194.2
072300     ELSE                                                         IF1194.2
072400                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
072500                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1194.2
072600                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1194.2
072700                    PERFORM FAIL.                                 IF1194.2
072800     GO TO F-MAX-WRITE-21.                                        IF1194.2
072900 F-MAX-DELETE-21.                                                 IF1194.2
073000     PERFORM  DE-LETE.                                            IF1194.2
073100     GO TO    F-MAX-WRITE-21.                                     IF1194.2
073200 F-MAX-WRITE-21.                                                  IF1194.2
073300     MOVE "F-MAX-21" TO PAR-NAME.                                 IF1194.2
073400     PERFORM  PRINT-DETAIL.                                       IF1194.2
073500*****************TEST (e) - COMPLEX TEST****************          IF1194.2
073600 F-MAX-22.                                                        IF1194.2
073700     MOVE ZERO TO WS-NUM.                                         IF1194.2
073800     MOVE 36.2593 TO MIN-RANGE.                                   IF1194.2
073900     MOVE 36.2607 TO MAX-RANGE.                                   IF1194.2
074000 F-MAX-TEST-22.                                                   IF1194.2
074100     COMPUTE WS-NUM = FUNCTION MAX(4, B, E) + 2.                  IF1194.2
074200     IF (WS-NUM >= MIN-RANGE) AND                                 IF1194.2
074300        (WS-NUM <= MAX-RANGE) THEN                                IF1194.2
074400                    PERFORM PASS                                  IF1194.2
074500     ELSE                                                         IF1194.2
074600                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
074700                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1194.2
074800                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1194.2
074900                    PERFORM FAIL.                                 IF1194.2
075000     GO TO F-MAX-WRITE-22.                                        IF1194.2
075100 F-MAX-DELETE-22.                                                 IF1194.2
075200     PERFORM  DE-LETE.                                            IF1194.2
075300     GO TO    F-MAX-WRITE-22.                                     IF1194.2
075400 F-MAX-WRITE-22.                                                  IF1194.2
075500     MOVE "F-MAX-22" TO PAR-NAME.                                 IF1194.2
075600     PERFORM  PRINT-DETAIL.                                       IF1194.2
075700*****************TEST (f) - COMPLEX TEST****************          IF1194.2
075800 F-MAX-23.                                                        IF1194.2
075900     MOVE ZERO TO WS-NUM.                                         IF1194.2
076000     MOVE 11.9998 TO MIN-RANGE.                                   IF1194.2
076100     MOVE 12.0002 TO MAX-RANGE.                                   IF1194.2
076200 F-MAX-TEST-23.                                                   IF1194.2
076300     COMPUTE WS-NUM = FUNCTION MAX(A, G) +                        IF1194.2
076400                      FUNCTION MAX(B, 0).                         IF1194.2
076500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1194.2
076600        (WS-NUM <= MAX-RANGE) THEN                                IF1194.2
076700                    PERFORM PASS                                  IF1194.2
076800     ELSE                                                         IF1194.2
076900                    MOVE WS-NUM TO COMPUTED-N                     IF1194.2
077000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1194.2
077100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1194.2
077200                    PERFORM FAIL.                                 IF1194.2
077300     GO TO F-MAX-WRITE-23.                                        IF1194.2
077400 F-MAX-DELETE-23.                                                 IF1194.2
077500     PERFORM  DE-LETE.                                            IF1194.2
077600     GO TO    F-MAX-WRITE-23.                                     IF1194.2
077700 F-MAX-WRITE-23.                                                  IF1194.2
077800     MOVE "F-MAX-23" TO PAR-NAME.                                 IF1194.2
077900     PERFORM  PRINT-DETAIL.                                       IF1194.2
078000*****************SPECIAL PERFORM TEST**********************       IF1194.2
078100 F-MAX-24.                                                        IF1194.2
078200     PERFORM F-MAX-TEST-24                                        IF1194.2
078300       UNTIL FUNCTION MAX(ARG1, 1) > 5.                           IF1194.2
078400     PERFORM PASS.                                                IF1194.2
078500     GO TO F-MAX-WRITE-24.                                        IF1194.2
078600 F-MAX-TEST-24.                                                   IF1194.2
078700     COMPUTE ARG1 = ARG1 + 1.                                     IF1194.2
078800 F-MAX-DELETE-24.                                                 IF1194.2
078900     PERFORM  DE-LETE.                                            IF1194.2
079000     GO TO    F-MAX-WRITE-24.                                     IF1194.2
079100 F-MAX-WRITE-24.                                                  IF1194.2
079200     MOVE "F-MAX-24" TO PAR-NAME.                                 IF1194.2
079300     PERFORM  PRINT-DETAIL.                                       IF1194.2
079400********************END OF TESTS***************                   IF1194.2
079500 CCVS-EXIT SECTION.                                               IF1194.2
079600 CCVS-999999.                                                     IF1194.2
079700     GO TO CLOSE-FILES.                                           IF1194.2
      *END-OF,IF119A                                                                  
