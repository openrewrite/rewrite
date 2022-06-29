      *HEADER,COBOL,IF123A                                                            
000100 IDENTIFICATION DIVISION.                                         IF1234.2
000200 PROGRAM-ID.                                                      IF1234.2
000300     IF123A.                                                      IF1234.2
000400                                                                  IF1234.2
000500***********************************************************       IF1234.2
000600*                                                         *       IF1234.2
000700* This program forms part of the CCVS85 COBOL Test Suite. *       IF1234.2
000800* It contains tests for the Intrinsic Function MIN.       *       IF1234.2
000900*                                                         *       IF1234.2
001000***********************************************************       IF1234.2
001100 ENVIRONMENT DIVISION.                                            IF1234.2
001200 CONFIGURATION SECTION.                                           IF1234.2
001300 SOURCE-COMPUTER.                                                 IF1234.2
001400     XXXXX082.                                                    IF1234.2
001500 OBJECT-COMPUTER.                                                 IF1234.2
001600     XXXXX083                                                     IF1234.2
001700     PROGRAM COLLATING SEQUENCE IS PRG-COLL-SEQ.                  IF1234.2
001800 SPECIAL-NAMES.                                                   IF1234.2
001900     ALPHABET PRG-COLL-SEQ IS                                     IF1234.2
002000     STANDARD-2.                                                  IF1234.2
002100 INPUT-OUTPUT SECTION.                                            IF1234.2
002200 FILE-CONTROL.                                                    IF1234.2
002300     SELECT PRINT-FILE ASSIGN TO                                  IF1234.2
002400     XXXXX055.                                                    IF1234.2
002500 DATA DIVISION.                                                   IF1234.2
002600 FILE SECTION.                                                    IF1234.2
002700 FD  PRINT-FILE.                                                  IF1234.2
002800 01  PRINT-REC PICTURE X(120).                                    IF1234.2
002900 01  DUMMY-RECORD PICTURE X(120).                                 IF1234.2
003000 WORKING-STORAGE SECTION.                                         IF1234.2
003100***********************************************************       IF1234.2
003200* Variables specific to the Intrinsic Function Test IF123A*       IF1234.2
003300***********************************************************       IF1234.2
003400 01  A                   PIC S9(10)          VALUE 5.             IF1234.2
003500 01  B                   PIC S9(10)          VALUE 7.             IF1234.2
003600 01  C                   PIC S9(10)          VALUE -4.            IF1234.2
003700 01  D                   PIC S9(10)          VALUE 10.            IF1234.2
003800 01  E                   PIC S9(5)V9(5)      VALUE 34.26.         IF1234.2
003900 01  F                   PIC S9(5)V9(5)      VALUE -8.32.         IF1234.2
004000 01  G                   PIC S9(5)V9(5)      VALUE 4.08.          IF1234.2
004100 01  H                   PIC S9(5)V9(5)      VALUE -5.3.          IF1234.2
004200 01  I                   PIC X               VALUE "R".           IF1234.2
004300 01  J                   PIC X               VALUE "U".           IF1234.2
004400 01  M                   PIC S9(10)          VALUE 1.             IF1234.2
004500 01  N                   PIC S9(10)          VALUE 3.             IF1234.2
004600 01  O                   PIC S9(10)          VALUE 5.             IF1234.2
004700 01  P                   PIC S9(10)          VALUE 1.             IF1234.2
004800 01  Q                   PIC S9(10)          VALUE 3.             IF1234.2
004900 01  R                   PIC S9(10)          VALUE 5.             IF1234.2
005000 01  ARG1                PIC S9(10)          VALUE 15.            IF1234.2
005100 01  ARR                                     VALUE "40537".       IF1234.2
005200     02  IND OCCURS 5 TIMES PIC 9.                                IF1234.2
005300 01  TEMP                PIC S9(10).                              IF1234.2
005400 01  WS-NUM              PIC S9(5)V9(6).                          IF1234.2
005500 01  WS-ANUM             PIC X.                                   IF1234.2
005600 01  MIN-RANGE           PIC S9(5)V9(7).                          IF1234.2
005700 01  MAX-RANGE           PIC S9(5)V9(7).                          IF1234.2
005800*                                                                 IF1234.2
005900**********************************************************        IF1234.2
006000*                                                                 IF1234.2
006100 01  TEST-RESULTS.                                                IF1234.2
006200     02 FILLER                   PIC X      VALUE SPACE.          IF1234.2
006300     02 FEATURE                  PIC X(20)  VALUE SPACE.          IF1234.2
006400     02 FILLER                   PIC X      VALUE SPACE.          IF1234.2
006500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IF1234.2
006600     02 FILLER                   PIC X      VALUE SPACE.          IF1234.2
006700     02  PAR-NAME.                                                IF1234.2
006800       03 FILLER                 PIC X(19)  VALUE SPACE.          IF1234.2
006900       03  PARDOT-X              PIC X      VALUE SPACE.          IF1234.2
007000       03 DOTVALUE               PIC 99     VALUE ZERO.           IF1234.2
007100     02 FILLER                   PIC X(8)   VALUE SPACE.          IF1234.2
007200     02 RE-MARK                  PIC X(61).                       IF1234.2
007300 01  TEST-COMPUTED.                                               IF1234.2
007400     02 FILLER                   PIC X(30)  VALUE SPACE.          IF1234.2
007500     02 FILLER                   PIC X(17)  VALUE                 IF1234.2
007600            "       COMPUTED=".                                   IF1234.2
007700     02 COMPUTED-X.                                               IF1234.2
007800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IF1234.2
007900     03 COMPUTED-N               REDEFINES COMPUTED-A             IF1234.2
008000                                 PIC -9(9).9(9).                  IF1234.2
008100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IF1234.2
008200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IF1234.2
008300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IF1234.2
008400     03       CM-18V0 REDEFINES COMPUTED-A.                       IF1234.2
008500         04 COMPUTED-18V0                    PIC -9(18).          IF1234.2
008600         04 FILLER                           PIC X.               IF1234.2
008700     03 FILLER PIC X(50) VALUE SPACE.                             IF1234.2
008800 01  TEST-CORRECT.                                                IF1234.2
008900     02 FILLER PIC X(30) VALUE SPACE.                             IF1234.2
009000     02 FILLER PIC X(17) VALUE "       CORRECT =".                IF1234.2
009100     02 CORRECT-X.                                                IF1234.2
009200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IF1234.2
009300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IF1234.2
009400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IF1234.2
009500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IF1234.2
009600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IF1234.2
009700     03      CR-18V0 REDEFINES CORRECT-A.                         IF1234.2
009800         04 CORRECT-18V0                     PIC -9(18).          IF1234.2
009900         04 FILLER                           PIC X.               IF1234.2
010000     03 FILLER PIC X(2) VALUE SPACE.                              IF1234.2
010100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IF1234.2
010200 01  TEST-CORRECT-MIN.                                            IF1234.2
010300     02 FILLER PIC X(30) VALUE SPACE.                             IF1234.2
010400     02 FILLER PIC X(17) VALUE "     MIN VALUE =".                IF1234.2
010500     02 CORRECTMI-X.                                              IF1234.2
010600     03 CORRECTMI-A                 PIC X(20) VALUE SPACE.        IF1234.2
010700     03 CORRECT-MIN    REDEFINES CORRECTMI-A     PIC -9(9).9(9).  IF1234.2
010800     03 CORRECTMI-0V18 REDEFINES CORRECTMI-A     PIC -.9(18).     IF1234.2
010900     03 CORRECTMI-4V14 REDEFINES CORRECTMI-A     PIC -9(4).9(14). IF1234.2
011000     03 CORRECTMI-14V4 REDEFINES CORRECTMI-A     PIC -9(14).9(4). IF1234.2
011100     03      CR-18V0 REDEFINES CORRECTMI-A.                       IF1234.2
011200         04 CORRECTMI-18V0                     PIC -9(18).        IF1234.2
011300         04 FILLER                           PIC X.               IF1234.2
011400     03 FILLER PIC X(2) VALUE SPACE.                              IF1234.2
011500     03 FILLER                           PIC X(48) VALUE SPACE.   IF1234.2
011600 01  TEST-CORRECT-MAX.                                            IF1234.2
011700     02 FILLER PIC X(30) VALUE SPACE.                             IF1234.2
011800     02 FILLER PIC X(17) VALUE "     MAX VALUE =".                IF1234.2
011900     02 CORRECTMA-X.                                              IF1234.2
012000     03 CORRECTMA-A                  PIC X(20) VALUE SPACE.       IF1234.2
012100     03 CORRECT-MAX    REDEFINES CORRECTMA-A     PIC -9(9).9(9).  IF1234.2
012200     03 CORRECTMA-0V18 REDEFINES CORRECTMA-A     PIC -.9(18).     IF1234.2
012300     03 CORRECTMA-4V14 REDEFINES CORRECTMA-A     PIC -9(4).9(14). IF1234.2
012400     03 CORRECTMA-14V4 REDEFINES CORRECTMA-A     PIC -9(14).9(4). IF1234.2
012500     03      CR-18V0 REDEFINES CORRECTMA-A.                       IF1234.2
012600         04 CORRECTMA-18V0                     PIC -9(18).        IF1234.2
012700         04 FILLER                           PIC X.               IF1234.2
012800     03 FILLER PIC X(2) VALUE SPACE.                              IF1234.2
012900     03 CORMA-ANSI-REFERENCE             PIC X(48) VALUE SPACE.   IF1234.2
013000 01  CCVS-C-1.                                                    IF1234.2
013100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIF1234.2
013200-    "SS  PARAGRAPH-NAME                                          IF1234.2
013300-    "       REMARKS".                                            IF1234.2
013400     02 FILLER                     PIC X(20)    VALUE SPACE.      IF1234.2
013500 01  CCVS-C-2.                                                    IF1234.2
013600     02 FILLER                     PIC X        VALUE SPACE.      IF1234.2
013700     02 FILLER                     PIC X(6)     VALUE "TESTED".   IF1234.2
013800     02 FILLER                     PIC X(15)    VALUE SPACE.      IF1234.2
013900     02 FILLER                     PIC X(4)     VALUE "FAIL".     IF1234.2
014000     02 FILLER                     PIC X(94)    VALUE SPACE.      IF1234.2
014100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IF1234.2
014200 01  REC-CT                        PIC 99       VALUE ZERO.       IF1234.2
014300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IF1234.2
014400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IF1234.2
014500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IF1234.2
014600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IF1234.2
014700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IF1234.2
014800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IF1234.2
014900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IF1234.2
015000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IF1234.2
015100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IF1234.2
015200 01  CCVS-H-1.                                                    IF1234.2
015300     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1234.2
015400     02  FILLER                    PIC X(42)    VALUE             IF1234.2
015500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IF1234.2
015600     02  FILLER                    PIC X(39)    VALUE SPACES.     IF1234.2
015700 01  CCVS-H-2A.                                                   IF1234.2
015800   02  FILLER                        PIC X(40)  VALUE SPACE.      IF1234.2
015900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IF1234.2
016000   02  FILLER                        PIC XXXX   VALUE             IF1234.2
016100     "4.2 ".                                                      IF1234.2
016200   02  FILLER                        PIC X(28)  VALUE             IF1234.2
016300            " COPY - NOT FOR DISTRIBUTION".                       IF1234.2
016400   02  FILLER                        PIC X(41)  VALUE SPACE.      IF1234.2
016500                                                                  IF1234.2
016600 01  CCVS-H-2B.                                                   IF1234.2
016700   02  FILLER                        PIC X(15)  VALUE             IF1234.2
016800            "TEST RESULT OF ".                                    IF1234.2
016900   02  TEST-ID                       PIC X(9).                    IF1234.2
017000   02  FILLER                        PIC X(4)   VALUE             IF1234.2
017100            " IN ".                                               IF1234.2
017200   02  FILLER                        PIC X(12)  VALUE             IF1234.2
017300     " HIGH       ".                                              IF1234.2
017400   02  FILLER                        PIC X(22)  VALUE             IF1234.2
017500            " LEVEL VALIDATION FOR ".                             IF1234.2
017600   02  FILLER                        PIC X(58)  VALUE             IF1234.2
017700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1234.2
017800 01  CCVS-H-3.                                                    IF1234.2
017900     02  FILLER                      PIC X(34)  VALUE             IF1234.2
018000            " FOR OFFICIAL USE ONLY    ".                         IF1234.2
018100     02  FILLER                      PIC X(58)  VALUE             IF1234.2
018200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IF1234.2
018300     02  FILLER                      PIC X(28)  VALUE             IF1234.2
018400            "  COPYRIGHT   1985 ".                                IF1234.2
018500 01  CCVS-E-1.                                                    IF1234.2
018600     02 FILLER                       PIC X(52)  VALUE SPACE.      IF1234.2
018700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IF1234.2
018800     02 ID-AGAIN                     PIC X(9).                    IF1234.2
018900     02 FILLER                       PIC X(45)  VALUE SPACES.     IF1234.2
019000 01  CCVS-E-2.                                                    IF1234.2
019100     02  FILLER                      PIC X(31)  VALUE SPACE.      IF1234.2
019200     02  FILLER                      PIC X(21)  VALUE SPACE.      IF1234.2
019300     02 CCVS-E-2-2.                                               IF1234.2
019400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IF1234.2
019500         03 FILLER                   PIC X      VALUE SPACE.      IF1234.2
019600         03 ENDER-DESC               PIC X(44)  VALUE             IF1234.2
019700            "ERRORS ENCOUNTERED".                                 IF1234.2
019800 01  CCVS-E-3.                                                    IF1234.2
019900     02  FILLER                      PIC X(22)  VALUE             IF1234.2
020000            " FOR OFFICIAL USE ONLY".                             IF1234.2
020100     02  FILLER                      PIC X(12)  VALUE SPACE.      IF1234.2
020200     02  FILLER                      PIC X(58)  VALUE             IF1234.2
020300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IF1234.2
020400     02  FILLER                      PIC X(13)  VALUE SPACE.      IF1234.2
020500     02 FILLER                       PIC X(15)  VALUE             IF1234.2
020600             " COPYRIGHT 1985".                                   IF1234.2
020700 01  CCVS-E-4.                                                    IF1234.2
020800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IF1234.2
020900     02 FILLER                       PIC X(4)   VALUE " OF ".     IF1234.2
021000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IF1234.2
021100     02 FILLER                       PIC X(40)  VALUE             IF1234.2
021200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IF1234.2
021300 01  XXINFO.                                                      IF1234.2
021400     02 FILLER                       PIC X(19)  VALUE             IF1234.2
021500            "*** INFORMATION ***".                                IF1234.2
021600     02 INFO-TEXT.                                                IF1234.2
021700       04 FILLER                     PIC X(8)   VALUE SPACE.      IF1234.2
021800       04 XXCOMPUTED                 PIC X(20).                   IF1234.2
021900       04 FILLER                     PIC X(5)   VALUE SPACE.      IF1234.2
022000       04 XXCORRECT                  PIC X(20).                   IF1234.2
022100     02 INF-ANSI-REFERENCE           PIC X(48).                   IF1234.2
022200 01  HYPHEN-LINE.                                                 IF1234.2
022300     02 FILLER  PIC IS X VALUE IS SPACE.                          IF1234.2
022400     02 FILLER  PIC IS X(65)    VALUE IS "************************IF1234.2
022500-    "*****************************************".                 IF1234.2
022600     02 FILLER  PIC IS X(54)    VALUE IS "************************IF1234.2
022700-    "******************************".                            IF1234.2
022800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IF1234.2
022900     "IF123A".                                                    IF1234.2
023000 PROCEDURE DIVISION.                                              IF1234.2
023100 CCVS1 SECTION.                                                   IF1234.2
023200 OPEN-FILES.                                                      IF1234.2
023300     OPEN     OUTPUT PRINT-FILE.                                  IF1234.2
023400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IF1234.2
023500     MOVE    SPACE TO TEST-RESULTS.                               IF1234.2
023600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IF1234.2
023700     GO TO CCVS1-EXIT.                                            IF1234.2
023800 CLOSE-FILES.                                                     IF1234.2
023900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IF1234.2
024000 TERMINATE-CCVS.                                                  IF1234.2
024100     STOP     RUN.                                                IF1234.2
024200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IF1234.2
024300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IF1234.2
024400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IF1234.2
024500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IF1234.2
024600     MOVE "****TEST DELETED****" TO RE-MARK.                      IF1234.2
024700 PRINT-DETAIL.                                                    IF1234.2
024800     IF REC-CT NOT EQUAL TO ZERO                                  IF1234.2
024900             MOVE "." TO PARDOT-X                                 IF1234.2
025000             MOVE REC-CT TO DOTVALUE.                             IF1234.2
025100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IF1234.2
025200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IF1234.2
025300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IF1234.2
025400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IF1234.2
025500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IF1234.2
025600     MOVE SPACE TO CORRECT-X.                                     IF1234.2
025700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IF1234.2
025800     MOVE     SPACE TO RE-MARK.                                   IF1234.2
025900 HEAD-ROUTINE.                                                    IF1234.2
026000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1234.2
026100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IF1234.2
026200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1234.2
026300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IF1234.2
026400 COLUMN-NAMES-ROUTINE.                                            IF1234.2
026500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1234.2
026600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1234.2
026700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IF1234.2
026800 END-ROUTINE.                                                     IF1234.2
026900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IF1234.2
027000 END-RTN-EXIT.                                                    IF1234.2
027100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1234.2
027200 END-ROUTINE-1.                                                   IF1234.2
027300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IF1234.2
027400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IF1234.2
027500      ADD PASS-COUNTER TO ERROR-HOLD.                             IF1234.2
027600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IF1234.2
027700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IF1234.2
027800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IF1234.2
027900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IF1234.2
028000  END-ROUTINE-12.                                                 IF1234.2
028100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IF1234.2
028200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IF1234.2
028300         MOVE "NO " TO ERROR-TOTAL                                IF1234.2
028400         ELSE                                                     IF1234.2
028500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IF1234.2
028600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IF1234.2
028700     PERFORM WRITE-LINE.                                          IF1234.2
028800 END-ROUTINE-13.                                                  IF1234.2
028900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IF1234.2
029000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IF1234.2
029100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IF1234.2
029200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IF1234.2
029300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1234.2
029400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IF1234.2
029500          MOVE "NO " TO ERROR-TOTAL                               IF1234.2
029600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IF1234.2
029700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IF1234.2
029800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IF1234.2
029900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IF1234.2
030000 WRITE-LINE.                                                      IF1234.2
030100     ADD 1 TO RECORD-COUNT.                                       IF1234.2
030200*    IF RECORD-COUNT GREATER 42                                   IF1234.2
030300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IF1234.2
030400*        MOVE SPACE TO DUMMY-RECORD                               IF1234.2
030500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IF1234.2
030600*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1234.2
030700*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   IF1234.2
030800*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1234.2
030900*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   IF1234.2
031000*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           IF1234.2
031100*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           IF1234.2
031200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IF1234.2
031300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IF1234.2
031400*        MOVE ZERO TO RECORD-COUNT.                               IF1234.2
031500     PERFORM WRT-LN.                                              IF1234.2
031600 WRT-LN.                                                          IF1234.2
031700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IF1234.2
031800     MOVE SPACE TO DUMMY-RECORD.                                  IF1234.2
031900 BLANK-LINE-PRINT.                                                IF1234.2
032000     PERFORM WRT-LN.                                              IF1234.2
032100 FAIL-ROUTINE.                                                    IF1234.2
032200     IF     COMPUTED-X NOT EQUAL TO SPACE                         IF1234.2
032300            GO TO FAIL-ROUTINE-WRITE.                             IF1234.2
032400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IF1234.2
032500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1234.2
032600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IF1234.2
032700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1234.2
032800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1234.2
032900     GO TO  FAIL-ROUTINE-EX.                                      IF1234.2
033000 FAIL-ROUTINE-WRITE.                                              IF1234.2
033100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE.        IF1234.2
033200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE                  IF1234.2
033300                              CORMA-ANSI-REFERENCE.               IF1234.2
033400     IF CORRECT-MIN NOT EQUAL TO SPACES THEN                      IF1234.2
033500           MOVE TEST-CORRECT-MIN TO PRINT-REC PERFORM WRITE-LINE  IF1234.2
033600           MOVE TEST-CORRECT-MAX TO PRINT-REC PERFORM WRITE-LINE  IF1234.2
033700     ELSE                                                         IF1234.2
033800           MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE.     IF1234.2
033900     PERFORM WRITE-LINE.                                          IF1234.2
034000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IF1234.2
034100 FAIL-ROUTINE-EX. EXIT.                                           IF1234.2
034200 BAIL-OUT.                                                        IF1234.2
034300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IF1234.2
034400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IF1234.2
034500 BAIL-OUT-WRITE.                                                  IF1234.2
034600     MOVE CORRECT-A TO XXCORRECT.                                 IF1234.2
034700     MOVE COMPUTED-A TO XXCOMPUTED.                               IF1234.2
034800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IF1234.2
034900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IF1234.2
035000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IF1234.2
035100 BAIL-OUT-EX. EXIT.                                               IF1234.2
035200 CCVS1-EXIT.                                                      IF1234.2
035300     EXIT.                                                        IF1234.2
035400********************************************************          IF1234.2
035500*                                                      *          IF1234.2
035600*    Intrinsic Function Tests         IF123A - MIN     *          IF1234.2
035700*                                                      *          IF1234.2
035800********************************************************          IF1234.2
035900 SECT-IF123A SECTION.                                             IF1234.2
036000 F-MIN-INFO.                                                      IF1234.2
036100     MOVE     "See ref. A-56 2.27" TO ANSI-REFERENCE.             IF1234.2
036200     MOVE     "MIN Function" TO FEATURE.                          IF1234.2
036300*****************TEST (a) - SIMPLE TEST*****************          IF1234.2
036400 F-MIN-01.                                                        IF1234.2
036500     MOVE ZERO TO WS-NUM.                                         IF1234.2
036600 F-MIN-TEST-01.                                                   IF1234.2
036700     COMPUTE WS-NUM = FUNCTION MIN(5, 6, 10, 3, 7).               IF1234.2
036800     IF WS-NUM = 3 THEN                                           IF1234.2
036900                    PERFORM PASS                                  IF1234.2
037000     ELSE                                                         IF1234.2
037100                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
037200                    MOVE 3 TO CORRECT-N                           IF1234.2
037300                    PERFORM FAIL.                                 IF1234.2
037400     GO TO F-MIN-WRITE-01.                                        IF1234.2
037500 F-MIN-DELETE-01.                                                 IF1234.2
037600     PERFORM  DE-LETE.                                            IF1234.2
037700     GO TO    F-MIN-WRITE-01.                                     IF1234.2
037800 F-MIN-WRITE-01.                                                  IF1234.2
037900     MOVE "F-MIN-01" TO PAR-NAME.                                 IF1234.2
038000     PERFORM  PRINT-DETAIL.                                       IF1234.2
038100*****************TEST (b) - SIMPLE TEST*****************          IF1234.2
038200 F-MIN-02.                                                        IF1234.2
038300     EVALUATE FUNCTION MIN(-4, 7, 2304, 3, -8)                    IF1234.2
038400     WHEN -8                                                      IF1234.2
038500                    PERFORM PASS                                  IF1234.2
038600     WHEN OTHER                                                   IF1234.2
038700                    PERFORM FAIL.                                 IF1234.2
038800     GO TO F-MIN-WRITE-02.                                        IF1234.2
038900 F-MIN-DELETE-02.                                                 IF1234.2
039000     PERFORM  DE-LETE.                                            IF1234.2
039100     GO TO    F-MIN-WRITE-02.                                     IF1234.2
039200 F-MIN-WRITE-02.                                                  IF1234.2
039300     MOVE "F-MIN-02" TO PAR-NAME.                                 IF1234.2
039400     PERFORM  PRINT-DETAIL.                                       IF1234.2
039500*****************TEST (c) - SIMPLE TEST*****************          IF1234.2
039600 F-MIN-03.                                                        IF1234.2
039700     IF (FUNCTION MIN(4.3, 2.6, 7.3, 9.1) >= 2.59995) AND         IF1234.2
039800        (FUNCTION MIN(4.3, 2.6, 7.3, 9.1) <= 2.60005) THEN        IF1234.2
039900                    PERFORM PASS                                  IF1234.2
040000     ELSE                                                         IF1234.2
040100                    PERFORM FAIL.                                 IF1234.2
040200     GO TO F-MIN-WRITE-03.                                        IF1234.2
040300 F-MIN-DELETE-03.                                                 IF1234.2
040400     PERFORM  DE-LETE.                                            IF1234.2
040500     GO TO    F-MIN-WRITE-03.                                     IF1234.2
040600 F-MIN-WRITE-03.                                                  IF1234.2
040700     MOVE "F-MIN-03" TO PAR-NAME.                                 IF1234.2
040800     PERFORM  PRINT-DETAIL.                                       IF1234.2
040900*****************TEST (d) - SIMPLE TEST*****************          IF1234.2
041000 F-MIN-04.                                                        IF1234.2
041100     MOVE ZERO TO WS-NUM.                                         IF1234.2
041200 F-MIN-TEST-04.                                                   IF1234.2
041300     COMPUTE WS-NUM = FUNCTION MIN(-4.3, 10.2, -0.7, 3.9).        IF1234.2
041400     IF (WS-NUM >= -4.30009) AND                                  IF1234.2
041500        (WS-NUM <= -4.29991)                                      IF1234.2
041600                    PERFORM PASS                                  IF1234.2
041700     ELSE                                                         IF1234.2
041800                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
041900                    MOVE -4.3 TO CORRECT-N                        IF1234.2
042000                    PERFORM FAIL.                                 IF1234.2
042100     GO TO F-MIN-WRITE-04.                                        IF1234.2
042200 F-MIN-DELETE-04.                                                 IF1234.2
042300     PERFORM  DE-LETE.                                            IF1234.2
042400     GO TO    F-MIN-WRITE-04.                                     IF1234.2
042500 F-MIN-WRITE-04.                                                  IF1234.2
042600     MOVE "F-MIN-04" TO PAR-NAME.                                 IF1234.2
042700     PERFORM  PRINT-DETAIL.                                       IF1234.2
042800*****************TEST (e) - SIMPLE TEST*****************          IF1234.2
042900 F-MIN-05.                                                        IF1234.2
043000     MOVE ZERO TO WS-NUM.                                         IF1234.2
043100 F-MIN-TEST-05.                                                   IF1234.2
043200     COMPUTE WS-NUM = FUNCTION MIN(A, B, D).                      IF1234.2
043300     IF WS-NUM = 5 THEN                                           IF1234.2
043400                    PERFORM PASS                                  IF1234.2
043500     ELSE                                                         IF1234.2
043600                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
043700                    MOVE 5 TO CORRECT-N                           IF1234.2
043800                    PERFORM FAIL.                                 IF1234.2
043900     GO TO F-MIN-WRITE-05.                                        IF1234.2
044000 F-MIN-DELETE-05.                                                 IF1234.2
044100     PERFORM  DE-LETE.                                            IF1234.2
044200     GO TO    F-MIN-WRITE-05.                                     IF1234.2
044300 F-MIN-WRITE-05.                                                  IF1234.2
044400     MOVE "F-MIN-05" TO PAR-NAME.                                 IF1234.2
044500     PERFORM  PRINT-DETAIL.                                       IF1234.2
044600*****************TEST (f) - SIMPLE TEST*****************          IF1234.2
044700 F-MIN-06.                                                        IF1234.2
044800     MOVE ZERO TO WS-NUM.                                         IF1234.2
044900 F-MIN-TEST-06.                                                   IF1234.2
045000     COMPUTE WS-NUM = FUNCTION MIN(A, B, C, D).                   IF1234.2
045100     IF WS-NUM = -4 THEN                                          IF1234.2
045200                    PERFORM PASS                                  IF1234.2
045300     ELSE                                                         IF1234.2
045400                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
045500                    MOVE -4 TO CORRECT-N                          IF1234.2
045600                    PERFORM FAIL.                                 IF1234.2
045700     GO TO F-MIN-WRITE-06.                                        IF1234.2
045800 F-MIN-DELETE-06.                                                 IF1234.2
045900     PERFORM  DE-LETE.                                            IF1234.2
046000     GO TO    F-MIN-WRITE-06.                                     IF1234.2
046100 F-MIN-WRITE-06.                                                  IF1234.2
046200     MOVE "F-MIN-06" TO PAR-NAME.                                 IF1234.2
046300     PERFORM  PRINT-DETAIL.                                       IF1234.2
046400*****************TEST (g) - SIMPLE TEST*****************          IF1234.2
046500 F-MIN-07.                                                        IF1234.2
046600     MOVE ZERO TO WS-NUM.                                         IF1234.2
046700 F-MIN-TEST-07.                                                   IF1234.2
046800     COMPUTE WS-NUM = FUNCTION MIN(E, G).                         IF1234.2
046900     IF (WS-NUM >= 4.07992) AND                                   IF1234.2
047000        (WS-NUM <= 4.08008)                                       IF1234.2
047100                    PERFORM PASS                                  IF1234.2
047200     ELSE                                                         IF1234.2
047300                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
047400                    MOVE 4.08 TO CORRECT-N                        IF1234.2
047500                    PERFORM FAIL.                                 IF1234.2
047600     GO TO F-MIN-WRITE-07.                                        IF1234.2
047700 F-MIN-DELETE-07.                                                 IF1234.2
047800     PERFORM  DE-LETE.                                            IF1234.2
047900     GO TO    F-MIN-WRITE-07.                                     IF1234.2
048000 F-MIN-WRITE-07.                                                  IF1234.2
048100     MOVE "F-MIN-07" TO PAR-NAME.                                 IF1234.2
048200     PERFORM  PRINT-DETAIL.                                       IF1234.2
048300*****************TEST (h) - SIMPLE TEST*****************          IF1234.2
048400 F-MIN-08.                                                        IF1234.2
048500     MOVE ZERO TO WS-NUM.                                         IF1234.2
048600 F-MIN-TEST-08.                                                   IF1234.2
048700     COMPUTE WS-NUM = FUNCTION MIN(E, F, G, H).                   IF1234.2
048800     IF (WS-NUM >= -8.32017) AND                                  IF1234.2
048900        (WS-NUM <= -8.31983)                                      IF1234.2
049000                    PERFORM PASS                                  IF1234.2
049100     ELSE                                                         IF1234.2
049200                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
049300                    MOVE -8.32 TO CORRECT-N                       IF1234.2
049400                    PERFORM FAIL.                                 IF1234.2
049500     GO TO F-MIN-WRITE-08.                                        IF1234.2
049600 F-MIN-DELETE-08.                                                 IF1234.2
049700     PERFORM  DE-LETE.                                            IF1234.2
049800     GO TO    F-MIN-WRITE-08.                                     IF1234.2
049900 F-MIN-WRITE-08.                                                  IF1234.2
050000     MOVE "F-MIN-08" TO PAR-NAME.                                 IF1234.2
050100     PERFORM  PRINT-DETAIL.                                       IF1234.2
050200*****************TEST (i) - SIMPLE TEST*****************          IF1234.2
050300 F-MIN-09.                                                        IF1234.2
050400     MOVE ZERO TO WS-NUM.                                         IF1234.2
050500 F-MIN-TEST-09.                                                   IF1234.2
050600     COMPUTE WS-NUM = FUNCTION MIN(A, 4, 8, -10, C, 0).           IF1234.2
050700     IF WS-NUM = -10 THEN                                         IF1234.2
050800                    PERFORM PASS                                  IF1234.2
050900     ELSE                                                         IF1234.2
051000                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
051100                    MOVE -10 TO CORRECT-N                         IF1234.2
051200                    PERFORM FAIL.                                 IF1234.2
051300     GO TO F-MIN-WRITE-09.                                        IF1234.2
051400 F-MIN-DELETE-09.                                                 IF1234.2
051500     PERFORM  DE-LETE.                                            IF1234.2
051600     GO TO    F-MIN-WRITE-09.                                     IF1234.2
051700 F-MIN-WRITE-09.                                                  IF1234.2
051800     MOVE "F-MIN-09" TO PAR-NAME.                                 IF1234.2
051900     PERFORM  PRINT-DETAIL.                                       IF1234.2
052000*****************TEST (j) - SIMPLE TEST*****************          IF1234.2
052100 F-MIN-10.                                                        IF1234.2
052200     MOVE ZERO TO WS-NUM.                                         IF1234.2
052300 F-MIN-TEST-10.                                                   IF1234.2
052400     COMPUTE WS-NUM = FUNCTION MIN(4, D, E, 6.3, -2.0).           IF1234.2
052500     IF (WS-NUM >= -2.00004) AND                                  IF1234.2
052600        (WS-NUM <= -1.99996)                                      IF1234.2
052700                    PERFORM PASS                                  IF1234.2
052800     ELSE                                                         IF1234.2
052900                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
053000                    MOVE -2.0 TO CORRECT-N                        IF1234.2
053100                    PERFORM FAIL.                                 IF1234.2
053200     GO TO F-MIN-WRITE-10.                                        IF1234.2
053300 F-MIN-DELETE-10.                                                 IF1234.2
053400     PERFORM  DE-LETE.                                            IF1234.2
053500     GO TO    F-MIN-WRITE-10.                                     IF1234.2
053600 F-MIN-WRITE-10.                                                  IF1234.2
053700     MOVE "F-MIN-10" TO PAR-NAME.                                 IF1234.2
053800     PERFORM  PRINT-DETAIL.                                       IF1234.2
053900*****************TEST (k) - SIMPLE TEST*****************          IF1234.2
054000 F-MIN-11.                                                        IF1234.2
054100     MOVE SPACES TO WS-ANUM.                                      IF1234.2
054200 F-MIN-TEST-11.                                                   IF1234.2
054300     MOVE FUNCTION MIN("R", I, "I", "a") TO WS-ANUM.              IF1234.2
054400     IF WS-ANUM = "I" THEN                                        IF1234.2
054500                    PERFORM PASS                                  IF1234.2
054600     ELSE                                                         IF1234.2
054700                    MOVE WS-ANUM TO COMPUTED-A                    IF1234.2
054800                    MOVE "I" TO CORRECT-A                         IF1234.2
054900                    PERFORM FAIL.                                 IF1234.2
055000     GO TO F-MIN-WRITE-11.                                        IF1234.2
055100 F-MIN-DELETE-11.                                                 IF1234.2
055200     PERFORM  DE-LETE.                                            IF1234.2
055300     GO TO    F-MIN-WRITE-11.                                     IF1234.2
055400 F-MIN-WRITE-11.                                                  IF1234.2
055500     MOVE "F-MIN-11" TO PAR-NAME.                                 IF1234.2
055600     PERFORM  PRINT-DETAIL.                                       IF1234.2
055700*****************TEST (l) - SIMPLE TEST*****************          IF1234.2
055800 F-MIN-12.                                                        IF1234.2
055900     MOVE ZERO TO WS-NUM.                                         IF1234.2
056000 F-MIN-TEST-12.                                                   IF1234.2
056100     MOVE FUNCTION MIN("a", J, "J") TO WS-ANUM.                   IF1234.2
056200     IF WS-ANUM = "J" THEN                                        IF1234.2
056300                    PERFORM PASS                                  IF1234.2
056400     ELSE                                                         IF1234.2
056500                    MOVE WS-ANUM TO COMPUTED-A                    IF1234.2
056600                    MOVE "J" TO CORRECT-A                         IF1234.2
056700                    PERFORM FAIL.                                 IF1234.2
056800     GO TO F-MIN-WRITE-12.                                        IF1234.2
056900 F-MIN-DELETE-12.                                                 IF1234.2
057000     PERFORM  DE-LETE.                                            IF1234.2
057100     GO TO    F-MIN-WRITE-12.                                     IF1234.2
057200 F-MIN-WRITE-12.                                                  IF1234.2
057300     MOVE "F-MIN-12" TO PAR-NAME.                                 IF1234.2
057400     PERFORM  PRINT-DETAIL.                                       IF1234.2
057500*****************TEST (m) - SIMPLE TEST*****************          IF1234.2
057600 F-MIN-13.                                                        IF1234.2
057700     MOVE ZERO TO WS-NUM.                                         IF1234.2
057800 F-MIN-TEST-13.                                                   IF1234.2
057900     COMPUTE WS-NUM = FUNCTION MIN(IND(1), IND(2), IND(3)).       IF1234.2
058000     IF WS-NUM = 0 THEN                                           IF1234.2
058100                    PERFORM PASS                                  IF1234.2
058200     ELSE                                                         IF1234.2
058300                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
058400                    MOVE 0 TO CORRECT-N                           IF1234.2
058500                    PERFORM FAIL.                                 IF1234.2
058600     GO TO F-MIN-WRITE-13.                                        IF1234.2
058700 F-MIN-DELETE-13.                                                 IF1234.2
058800     PERFORM  DE-LETE.                                            IF1234.2
058900     GO TO    F-MIN-WRITE-13.                                     IF1234.2
059000 F-MIN-WRITE-13.                                                  IF1234.2
059100     MOVE "F-MIN-13" TO PAR-NAME.                                 IF1234.2
059200     PERFORM  PRINT-DETAIL.                                       IF1234.2
059300*****************TEST (n) - SIMPLE TEST*****************          IF1234.2
059400 F-MIN-14.                                                        IF1234.2
059500     MOVE ZERO TO WS-NUM.                                         IF1234.2
059600 F-MIN-TEST-14.                                                   IF1234.2
059700     COMPUTE WS-NUM = FUNCTION MIN(IND(P), IND(Q), IND(R)).       IF1234.2
059800     IF WS-NUM = 4 THEN                                           IF1234.2
059900                    PERFORM PASS                                  IF1234.2
060000     ELSE                                                         IF1234.2
060100                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
060200                    MOVE 4 TO CORRECT-N                           IF1234.2
060300                    PERFORM FAIL.                                 IF1234.2
060400     GO TO F-MIN-WRITE-14.                                        IF1234.2
060500 F-MIN-DELETE-14.                                                 IF1234.2
060600     PERFORM  DE-LETE.                                            IF1234.2
060700     GO TO    F-MIN-WRITE-14.                                     IF1234.2
060800 F-MIN-WRITE-14.                                                  IF1234.2
060900     MOVE "F-MIN-14" TO PAR-NAME.                                 IF1234.2
061000     PERFORM  PRINT-DETAIL.                                       IF1234.2
061100*****************TEST (o) - SIMPLE TEST*****************          IF1234.2
061200 F-MIN-15.                                                        IF1234.2
061300     MOVE ZERO TO WS-NUM.                                         IF1234.2
061400 F-MIN-TEST-15.                                                   IF1234.2
061500     COMPUTE WS-NUM = FUNCTION MIN(IND(ALL)).                     IF1234.2
061600     IF WS-NUM = 0 THEN                                           IF1234.2
061700                    PERFORM PASS                                  IF1234.2
061800     ELSE                                                         IF1234.2
061900                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
062000                    MOVE 0 TO CORRECT-N                           IF1234.2
062100                    PERFORM FAIL.                                 IF1234.2
062200     GO TO F-MIN-WRITE-15.                                        IF1234.2
062300 F-MIN-DELETE-15.                                                 IF1234.2
062400     PERFORM  DE-LETE.                                            IF1234.2
062500     GO TO    F-MIN-WRITE-15.                                     IF1234.2
062600 F-MIN-WRITE-15.                                                  IF1234.2
062700     MOVE "F-MIN-15" TO PAR-NAME.                                 IF1234.2
062800     PERFORM  PRINT-DETAIL.                                       IF1234.2
062900*****************TEST (q) - SIMPLE TEST*****************          IF1234.2
063000 F-MIN-17.                                                        IF1234.2
063100     MOVE ZERO TO WS-NUM.                                         IF1234.2
063200 F-MIN-TEST-17.                                                   IF1234.2
063300     COMPUTE WS-NUM =                                             IF1234.2
063400         FUNCTION MIN(31000, 310001, 78000, 29000, 12000).        IF1234.2
063500     IF WS-NUM = 12000 THEN                                       IF1234.2
063600                    PERFORM PASS                                  IF1234.2
063700     ELSE                                                         IF1234.2
063800                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
063900                    MOVE 1200 TO CORRECT-N                        IF1234.2
064000                    PERFORM FAIL.                                 IF1234.2
064100     GO TO F-MIN-WRITE-17.                                        IF1234.2
064200 F-MIN-DELETE-17.                                                 IF1234.2
064300     PERFORM  DE-LETE.                                            IF1234.2
064400     GO TO    F-MIN-WRITE-17.                                     IF1234.2
064500 F-MIN-WRITE-17.                                                  IF1234.2
064600     MOVE "F-MIN-17" TO PAR-NAME.                                 IF1234.2
064700     PERFORM  PRINT-DETAIL.                                       IF1234.2
064800*****************TEST (a) - COMPLEX TEST****************          IF1234.2
064900 F-MIN-18.                                                        IF1234.2
065000     MOVE ZERO TO WS-NUM.                                         IF1234.2
065100     MOVE  1.99996 TO MIN-RANGE.                                  IF1234.2
065200     MOVE  2.00004 TO MAX-RANGE.                                  IF1234.2
065300 F-MIN-TEST-18.                                                   IF1234.2
065400     COMPUTE WS-NUM = FUNCTION MIN(A * B, (3 + 1) / 2, 3 + 4).    IF1234.2
065500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1234.2
065600        (WS-NUM <= MAX-RANGE) THEN                                IF1234.2
065700                    PERFORM PASS                                  IF1234.2
065800     ELSE                                                         IF1234.2
065900                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
066000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1234.2
066100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1234.2
066200                    PERFORM FAIL.                                 IF1234.2
066300     GO TO F-MIN-WRITE-18.                                        IF1234.2
066400 F-MIN-DELETE-18.                                                 IF1234.2
066500     PERFORM  DE-LETE.                                            IF1234.2
066600     GO TO    F-MIN-WRITE-18.                                     IF1234.2
066700 F-MIN-WRITE-18.                                                  IF1234.2
066800     MOVE "F-MIN-18" TO PAR-NAME.                                 IF1234.2
066900     PERFORM  PRINT-DETAIL.                                       IF1234.2
067000*****************TEST (b) - COMPLEX TEST****************          IF1234.2
067100 F-MIN-19.                                                        IF1234.2
067200     MOVE ZERO TO WS-NUM.                                         IF1234.2
067300     MOVE -10.6002 TO MIN-RANGE.                                  IF1234.2
067400     MOVE -10.5998 TO MAX-RANGE.                                  IF1234.2
067500 F-MIN-TEST-19.                                                   IF1234.2
067600     COMPUTE WS-NUM = FUNCTION MIN(E + 4, H * 2, 5 + A).          IF1234.2
067700     IF (WS-NUM >= MIN-RANGE) AND                                 IF1234.2
067800        (WS-NUM <= MAX-RANGE) THEN                                IF1234.2
067900                    PERFORM PASS                                  IF1234.2
068000     ELSE                                                         IF1234.2
068100                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
068200                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1234.2
068300                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1234.2
068400                    PERFORM FAIL.                                 IF1234.2
068500     GO TO F-MIN-WRITE-19.                                        IF1234.2
068600 F-MIN-DELETE-19.                                                 IF1234.2
068700     PERFORM  DE-LETE.                                            IF1234.2
068800     GO TO    F-MIN-WRITE-19.                                     IF1234.2
068900 F-MIN-WRITE-19.                                                  IF1234.2
069000     MOVE "F-MIN-19" TO PAR-NAME.                                 IF1234.2
069100     PERFORM  PRINT-DETAIL.                                       IF1234.2
069200*****************TEST (c) - COMPLEX TEST****************          IF1234.2
069300 F-MIN-20.                                                        IF1234.2
069400     MOVE ZERO TO WS-NUM.                                         IF1234.2
069500     MOVE -7.00014 TO MIN-RANGE.                                  IF1234.2
069600     MOVE -6.99986 TO MAX-RANGE.                                  IF1234.2
069700 F-MIN-TEST-20.                                                   IF1234.2
069800     COMPUTE WS-NUM = FUNCTION MIN(-7, -9 + 2, (- B)).            IF1234.2
069900     IF (WS-NUM >= MIN-RANGE) AND                                 IF1234.2
070000        (WS-NUM <= MAX-RANGE) THEN                                IF1234.2
070100                    PERFORM PASS                                  IF1234.2
070200     ELSE                                                         IF1234.2
070300                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
070400                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1234.2
070500                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1234.2
070600                    PERFORM FAIL.                                 IF1234.2
070700     GO TO F-MIN-WRITE-20.                                        IF1234.2
070800 F-MIN-DELETE-20.                                                 IF1234.2
070900     PERFORM  DE-LETE.                                            IF1234.2
071000     GO TO    F-MIN-WRITE-20.                                     IF1234.2
071100 F-MIN-WRITE-20.                                                  IF1234.2
071200     MOVE "F-MIN-20" TO PAR-NAME.                                 IF1234.2
071300     PERFORM  PRINT-DETAIL.                                       IF1234.2
071400*****************TEST (d) - COMPLEX TEST****************          IF1234.2
071500 F-MIN-21.                                                        IF1234.2
071600     MOVE ZERO TO WS-NUM.                                         IF1234.2
071700     MOVE 4.99990 TO MIN-RANGE.                                   IF1234.2
071800     MOVE 5.00010 TO MAX-RANGE.                                   IF1234.2
071900 F-MIN-TEST-21.                                                   IF1234.2
072000     COMPUTE WS-NUM = FUNCTION MIN(FUNCTION MIN(14, A), E, 50).   IF1234.2
072100     IF (WS-NUM >= MIN-RANGE) AND                                 IF1234.2
072200        (WS-NUM <= MAX-RANGE) THEN                                IF1234.2
072300                    PERFORM PASS                                  IF1234.2
072400     ELSE                                                         IF1234.2
072500                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
072600                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1234.2
072700                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1234.2
072800                    PERFORM FAIL.                                 IF1234.2
072900     GO TO F-MIN-WRITE-21.                                        IF1234.2
073000 F-MIN-DELETE-21.                                                 IF1234.2
073100     PERFORM  DE-LETE.                                            IF1234.2
073200     GO TO    F-MIN-WRITE-21.                                     IF1234.2
073300 F-MIN-WRITE-21.                                                  IF1234.2
073400     MOVE "F-MIN-21" TO PAR-NAME.                                 IF1234.2
073500     PERFORM  PRINT-DETAIL.                                       IF1234.2
073600*****************TEST (e) - COMPLEX TEST****************          IF1234.2
073700 F-MIN-22.                                                        IF1234.2
073800     MOVE ZERO TO WS-NUM.                                         IF1234.2
073900     MOVE 8.99982 TO MIN-RANGE.                                   IF1234.2
074000     MOVE 9.00018 TO MAX-RANGE.                                   IF1234.2
074100 F-MIN-TEST-22.                                                   IF1234.2
074200     COMPUTE WS-NUM = FUNCTION MIN(4, B, E) + A.                  IF1234.2
074300     IF (WS-NUM >= MIN-RANGE) AND                                 IF1234.2
074400        (WS-NUM <= MAX-RANGE) THEN                                IF1234.2
074500                    PERFORM PASS                                  IF1234.2
074600     ELSE                                                         IF1234.2
074700                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
074800                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1234.2
074900                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1234.2
075000                    PERFORM FAIL.                                 IF1234.2
075100     GO TO F-MIN-WRITE-22.                                        IF1234.2
075200 F-MIN-DELETE-22.                                                 IF1234.2
075300     PERFORM  DE-LETE.                                            IF1234.2
075400     GO TO    F-MIN-WRITE-22.                                     IF1234.2
075500 F-MIN-WRITE-22.                                                  IF1234.2
075600     MOVE "F-MIN-22" TO PAR-NAME.                                 IF1234.2
075700     PERFORM  PRINT-DETAIL.                                       IF1234.2
075800*****************TEST (f) - COMPLEX TEST****************          IF1234.2
075900 F-MIN-23.                                                        IF1234.2
076000     MOVE ZERO TO WS-NUM.                                         IF1234.2
076100     MOVE 4.99990 TO MIN-RANGE.                                   IF1234.2
076200     MOVE 5.00010 TO MAX-RANGE.                                   IF1234.2
076300 F-MIN-TEST-23.                                                   IF1234.2
076400     COMPUTE WS-NUM = FUNCTION MIN(A, E) + FUNCTION MIN(B, 0).    IF1234.2
076500     IF (WS-NUM >= MIN-RANGE) AND                                 IF1234.2
076600        (WS-NUM <= MAX-RANGE) THEN                                IF1234.2
076700                    PERFORM PASS                                  IF1234.2
076800     ELSE                                                         IF1234.2
076900                    MOVE WS-NUM TO COMPUTED-N                     IF1234.2
077000                    MOVE MIN-RANGE TO CORRECT-MIN                 IF1234.2
077100                    MOVE MAX-RANGE TO CORRECT-MAX                 IF1234.2
077200                    PERFORM FAIL.                                 IF1234.2
077300     GO TO F-MIN-WRITE-23.                                        IF1234.2
077400 F-MIN-DELETE-23.                                                 IF1234.2
077500     PERFORM  DE-LETE.                                            IF1234.2
077600     GO TO    F-MIN-WRITE-23.                                     IF1234.2
077700 F-MIN-WRITE-23.                                                  IF1234.2
077800     MOVE "F-MIN-23" TO PAR-NAME.                                 IF1234.2
077900     PERFORM  PRINT-DETAIL.                                       IF1234.2
078000*****************SPECIAL PERFORM TEST**********************       IF1234.2
078100 F-MIN-24.                                                        IF1234.2
078200     PERFORM F-MIN-TEST-24                                        IF1234.2
078300       UNTIL FUNCTION MIN(ARG1, 20) < 10.                         IF1234.2
078400     PERFORM PASS.                                                IF1234.2
078500     GO TO F-MIN-WRITE-24.                                        IF1234.2
078600 F-MIN-TEST-24.                                                   IF1234.2
078700     COMPUTE ARG1 = ARG1 - 1.                                     IF1234.2
078800 F-MIN-DELETE-24.                                                 IF1234.2
078900     PERFORM  DE-LETE.                                            IF1234.2
079000     GO TO    F-MIN-WRITE-24.                                     IF1234.2
079100 F-MIN-WRITE-24.                                                  IF1234.2
079200     MOVE "F-MIN-24" TO PAR-NAME.                                 IF1234.2
079300     PERFORM  PRINT-DETAIL.                                       IF1234.2
079400********************END OF TESTS***************                   IF1234.2
079500 CCVS-EXIT SECTION.                                               IF1234.2
079600 CCVS-999999.                                                     IF1234.2
079700     GO TO CLOSE-FILES.                                           IF1234.2
      *END-OF,IF123A                                                                  
