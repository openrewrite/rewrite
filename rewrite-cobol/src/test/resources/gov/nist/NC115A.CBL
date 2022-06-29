      *HEADER,COBOL,NC115A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1154.2
000200 PROGRAM-ID.                                                      NC1154.2
000300     NC115A.                                                      NC1154.2
000400****************************************************************  NC1154.2
000500*                                                              *  NC1154.2
000600*    VALIDATION FOR:-                                          *  NC1154.2
000700*                                                              *  NC1154.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1154.2
000900*                                                              *  NC1154.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1154.2
001100*                                                              *  NC1154.2
001200****************************************************************  NC1154.2
001300*                                                              *  NC1154.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1154.2
001500*                                                              *  NC1154.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1154.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1154.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1154.2
001900*                                                              *  NC1154.2
002000****************************************************************  NC1154.2
002100*                                                                 NC1154.2
002200*    PROGRAM NC115A TESTS FORMATS 1, 2, AND 3 OF                  NC1154.2
002300*    THE INSPECT STATEMENT.                                       NC1154.2
002400*                                                                 NC1154.2
002500******************************************************************NC1154.2
002600 ENVIRONMENT DIVISION.                                            NC1154.2
002700 CONFIGURATION SECTION.                                           NC1154.2
002800 SOURCE-COMPUTER.                                                 NC1154.2
002900     XXXXX082.                                                    NC1154.2
003000 OBJECT-COMPUTER.                                                 NC1154.2
003100     XXXXX083.                                                    NC1154.2
003200 INPUT-OUTPUT SECTION.                                            NC1154.2
003300 FILE-CONTROL.                                                    NC1154.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1154.2
003500     XXXXX055.                                                    NC1154.2
003600 DATA DIVISION.                                                   NC1154.2
003700 FILE SECTION.                                                    NC1154.2
003800 FD  PRINT-FILE.                                                  NC1154.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1154.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1154.2
004100 WORKING-STORAGE SECTION.                                         NC1154.2
004200 01  WRK-DU-999-1 PIC 999.                                        NC1154.2
004300 01  WRK-DU-999-2 PIC 999.                                        NC1154.2
004400 01  WRK-DU-999-3 PIC 999.                                        NC1154.2
004500 01  WRK-DU-999-4 PIC 999.                                        NC1154.2
004600 01  JUST-XN-20-1 PIC X(20) JUSTIFIED.                            NC1154.2
004700 01  SPACE-XN-1-1 PIC X VALUE SPACE.                              NC1154.2
004800 01  COMMA-XN-1-1 PIC X VALUE ",".                                NC1154.2
004900 01  HYPEN-XN-1-1 PIC X VALUE "-".                                NC1154.2
005000 01  A-XN-1-1 PIC X VALUE "A".                                    NC1154.2
005100 01  D-XN-1-1 PIC X VALUE "D".                                    NC1154.2
005200 01  G-XN-1-1 PIC X VALUE "G".                                    NC1154.2
005300 01  H-XN-1-1 PIC X VALUE "H".                                    NC1154.2
005400 01  L-XN-1-1 PIC X VALUE "L".                                    NC1154.2
005500 01  O-XN-1-1 PIC X VALUE "O".                                    NC1154.2
005600 01  P-XN-1-1 PIC X VALUE "P".                                    NC1154.2
005700 01  S-XN-1-1 PIC X VALUE "S".                                    NC1154.2
005800 01  Z-XN-1-1 PIC X VALUE "Z".                                    NC1154.2
005900 01  WRK-OK.                                                      NC1154.2
006000   03  WRK-OK-1-20                  PIC X(20).                    NC1154.2
006100   03  WRK-OK-21-40                 PIC X(20).                    NC1154.2
006200   03  WRK-OK-41-60                 PIC X(20).                    NC1154.2
006300   03  WRK-OK-61-80                 PIC X(20).                    NC1154.2
006400   03  WRK-OK-81-83                 PIC X(3).                     NC1154.2
006500 01  WRK-ER.                                                      NC1154.2
006600   03  WRK-ER-1-20                  PIC X(20).                    NC1154.2
006700   03  WRK-ER-21-40                 PIC X(20).                    NC1154.2
006800   03  WRK-ER-41-60                 PIC X(20).                    NC1154.2
006900   03  WRK-ER-61-80                 PIC X(20).                    NC1154.2
007000   03  WRK-ER-81-83                 PIC X(3).                     NC1154.2
007100                                                                  NC1154.2
007200 01  WRK-XN-83-1 PIC X(83).                                       NC1154.2
007300 01  WC-XN-83  PIC X(83) VALUE                                    NC1154.2
007400     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1154.2
007500-    "IDS CAN NOT BE ALL BAD.".                                   NC1154.2
007600 01  ANS-XN-83-1 PIC X(83) VALUE                                  NC1154.2
007700     "OH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1154.2
007800-    "IDS CAN NOT BE ALL BAD.".                                   NC1154.2
007900 01  ANS-XN-83-2 PIC X(83) VALUE                                  NC1154.2
008000     "AH YES,AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1154.2
008100-    "IDS CAN NOT BE ALL BAD.".                                   NC1154.2
008200 01  ANS-XN-83-3 PIC X(83) VALUE                                  NC1154.2
008300     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1154.2
008400-    "IDS CAN NOT BE ALL-BAD.".                                   NC1154.2
008500 01  ANS-XN-83-4 PIC X(83) VALUE                                  NC1154.2
008600     "EH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1154.2
008700-    "IDS CAN NOT BE ALL BAD.".                                   NC1154.2
008800 01  ANS-XN-83-5 PIC X(83) VALUE                                  NC1154.2
008900     "AH YES OH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1154.2
009000-    "IDS CAN NOT BE ALL BAD.".                                   NC1154.2
009100 01  ANS-XN-83-6 PIC X(83) VALUE                                  NC1154.2
009200     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1154.2
009300-    "IDS CAN NOT BE ALZZZZZZ".                                   NC1154.2
009400 01  ANS-XN-83-7 PIC X(83) VALUE                                  NC1154.2
009500     "OH-YES-AH-YES-W.P.-ZRITOES-HERE.-ANYONE-WHO-HATES-DOGS-AND-KNC1154.2
009600-    "IDS-CAN-NOT-BE-ALZZZZZZ".                                   NC1154.2
009700 01  TEST-RESULTS.                                                NC1154.2
009800     02 FILLER                   PIC X      VALUE SPACE.          NC1154.2
009900     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1154.2
010000     02 FILLER                   PIC X      VALUE SPACE.          NC1154.2
010100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1154.2
010200     02 FILLER                   PIC X      VALUE SPACE.          NC1154.2
010300     02  PAR-NAME.                                                NC1154.2
010400       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1154.2
010500       03  PARDOT-X              PIC X      VALUE SPACE.          NC1154.2
010600       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1154.2
010700     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1154.2
010800     02 RE-MARK                  PIC X(61).                       NC1154.2
010900 01  TEST-COMPUTED.                                               NC1154.2
011000     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1154.2
011100     02 FILLER                   PIC X(17)  VALUE                 NC1154.2
011200            "       COMPUTED=".                                   NC1154.2
011300     02 COMPUTED-X.                                               NC1154.2
011400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1154.2
011500     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1154.2
011600                                 PIC -9(9).9(9).                  NC1154.2
011700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1154.2
011800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1154.2
011900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1154.2
012000     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1154.2
012100         04 COMPUTED-18V0                    PIC -9(18).          NC1154.2
012200         04 FILLER                           PIC X.               NC1154.2
012300     03 FILLER PIC X(50) VALUE SPACE.                             NC1154.2
012400 01  TEST-CORRECT.                                                NC1154.2
012500     02 FILLER PIC X(30) VALUE SPACE.                             NC1154.2
012600     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1154.2
012700     02 CORRECT-X.                                                NC1154.2
012800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1154.2
012900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1154.2
013000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1154.2
013100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1154.2
013200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1154.2
013300     03      CR-18V0 REDEFINES CORRECT-A.                         NC1154.2
013400         04 CORRECT-18V0                     PIC -9(18).          NC1154.2
013500         04 FILLER                           PIC X.               NC1154.2
013600     03 FILLER PIC X(2) VALUE SPACE.                              NC1154.2
013700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1154.2
013800 01  CCVS-C-1.                                                    NC1154.2
013900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1154.2
014000-    "SS  PARAGRAPH-NAME                                          NC1154.2
014100-    "       REMARKS".                                            NC1154.2
014200     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1154.2
014300 01  CCVS-C-2.                                                    NC1154.2
014400     02 FILLER                     PIC X        VALUE SPACE.      NC1154.2
014500     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1154.2
014600     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1154.2
014700     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1154.2
014800     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1154.2
014900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1154.2
015000 01  REC-CT                        PIC 99       VALUE ZERO.       NC1154.2
015100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1154.2
015200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1154.2
015300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1154.2
015400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1154.2
015500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1154.2
015600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1154.2
015700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1154.2
015800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1154.2
015900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1154.2
016000 01  CCVS-H-1.                                                    NC1154.2
016100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1154.2
016200     02  FILLER                    PIC X(42)    VALUE             NC1154.2
016300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1154.2
016400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1154.2
016500 01  CCVS-H-2A.                                                   NC1154.2
016600   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1154.2
016700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1154.2
016800   02  FILLER                        PIC XXXX   VALUE             NC1154.2
016900     "4.2 ".                                                      NC1154.2
017000   02  FILLER                        PIC X(28)  VALUE             NC1154.2
017100            " COPY - NOT FOR DISTRIBUTION".                       NC1154.2
017200   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1154.2
017300                                                                  NC1154.2
017400 01  CCVS-H-2B.                                                   NC1154.2
017500   02  FILLER                        PIC X(15)  VALUE             NC1154.2
017600            "TEST RESULT OF ".                                    NC1154.2
017700   02  TEST-ID                       PIC X(9).                    NC1154.2
017800   02  FILLER                        PIC X(4)   VALUE             NC1154.2
017900            " IN ".                                               NC1154.2
018000   02  FILLER                        PIC X(12)  VALUE             NC1154.2
018100     " HIGH       ".                                              NC1154.2
018200   02  FILLER                        PIC X(22)  VALUE             NC1154.2
018300            " LEVEL VALIDATION FOR ".                             NC1154.2
018400   02  FILLER                        PIC X(58)  VALUE             NC1154.2
018500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1154.2
018600 01  CCVS-H-3.                                                    NC1154.2
018700     02  FILLER                      PIC X(34)  VALUE             NC1154.2
018800            " FOR OFFICIAL USE ONLY    ".                         NC1154.2
018900     02  FILLER                      PIC X(58)  VALUE             NC1154.2
019000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1154.2
019100     02  FILLER                      PIC X(28)  VALUE             NC1154.2
019200            "  COPYRIGHT   1985 ".                                NC1154.2
019300 01  CCVS-E-1.                                                    NC1154.2
019400     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1154.2
019500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1154.2
019600     02 ID-AGAIN                     PIC X(9).                    NC1154.2
019700     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1154.2
019800 01  CCVS-E-2.                                                    NC1154.2
019900     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1154.2
020000     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1154.2
020100     02 CCVS-E-2-2.                                               NC1154.2
020200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1154.2
020300         03 FILLER                   PIC X      VALUE SPACE.      NC1154.2
020400         03 ENDER-DESC               PIC X(44)  VALUE             NC1154.2
020500            "ERRORS ENCOUNTERED".                                 NC1154.2
020600 01  CCVS-E-3.                                                    NC1154.2
020700     02  FILLER                      PIC X(22)  VALUE             NC1154.2
020800            " FOR OFFICIAL USE ONLY".                             NC1154.2
020900     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1154.2
021000     02  FILLER                      PIC X(58)  VALUE             NC1154.2
021100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1154.2
021200     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1154.2
021300     02 FILLER                       PIC X(15)  VALUE             NC1154.2
021400             " COPYRIGHT 1985".                                   NC1154.2
021500 01  CCVS-E-4.                                                    NC1154.2
021600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1154.2
021700     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1154.2
021800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1154.2
021900     02 FILLER                       PIC X(40)  VALUE             NC1154.2
022000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1154.2
022100 01  XXINFO.                                                      NC1154.2
022200     02 FILLER                       PIC X(19)  VALUE             NC1154.2
022300            "*** INFORMATION ***".                                NC1154.2
022400     02 INFO-TEXT.                                                NC1154.2
022500       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1154.2
022600       04 XXCOMPUTED                 PIC X(20).                   NC1154.2
022700       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1154.2
022800       04 XXCORRECT                  PIC X(20).                   NC1154.2
022900     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1154.2
023000 01  HYPHEN-LINE.                                                 NC1154.2
023100     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1154.2
023200     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1154.2
023300-    "*****************************************".                 NC1154.2
023400     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1154.2
023500-    "******************************".                            NC1154.2
023600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1154.2
023700     "NC115A".                                                    NC1154.2
023800 PROCEDURE DIVISION.                                              NC1154.2
023900 CCVS1 SECTION.                                                   NC1154.2
024000 OPEN-FILES.                                                      NC1154.2
024100     OPEN     OUTPUT PRINT-FILE.                                  NC1154.2
024200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1154.2
024300     MOVE    SPACE TO TEST-RESULTS.                               NC1154.2
024400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1154.2
024500     GO TO CCVS1-EXIT.                                            NC1154.2
024600 CLOSE-FILES.                                                     NC1154.2
024700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1154.2
024800 TERMINATE-CCVS.                                                  NC1154.2
024900*    EXIT PROGRAM.                                                NC1154.2
025000*TERMINATE-CALL.                                                  NC1154.2
025100     STOP     RUN.                                                NC1154.2
025200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1154.2
025300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1154.2
025400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1154.2
025500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1154.2
025600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1154.2
025700 PRINT-DETAIL.                                                    NC1154.2
025800     IF REC-CT NOT EQUAL TO ZERO                                  NC1154.2
025900             MOVE "." TO PARDOT-X                                 NC1154.2
026000             MOVE REC-CT TO DOTVALUE.                             NC1154.2
026100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1154.2
026200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1154.2
026300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1154.2
026400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1154.2
026500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1154.2
026600     MOVE SPACE TO CORRECT-X.                                     NC1154.2
026700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1154.2
026800     MOVE     SPACE TO RE-MARK.                                   NC1154.2
026900 HEAD-ROUTINE.                                                    NC1154.2
027000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1154.2
027100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1154.2
027200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1154.2
027300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1154.2
027400 COLUMN-NAMES-ROUTINE.                                            NC1154.2
027500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1154.2
027600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1154.2
027700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1154.2
027800 END-ROUTINE.                                                     NC1154.2
027900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1154.2
028000 END-RTN-EXIT.                                                    NC1154.2
028100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1154.2
028200 END-ROUTINE-1.                                                   NC1154.2
028300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1154.2
028400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1154.2
028500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1154.2
028600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1154.2
028700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1154.2
028800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1154.2
028900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1154.2
029000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1154.2
029100  END-ROUTINE-12.                                                 NC1154.2
029200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1154.2
029300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1154.2
029400         MOVE "NO " TO ERROR-TOTAL                                NC1154.2
029500         ELSE                                                     NC1154.2
029600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1154.2
029700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1154.2
029800     PERFORM WRITE-LINE.                                          NC1154.2
029900 END-ROUTINE-13.                                                  NC1154.2
030000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1154.2
030100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1154.2
030200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1154.2
030300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1154.2
030400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1154.2
030500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1154.2
030600          MOVE "NO " TO ERROR-TOTAL                               NC1154.2
030700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1154.2
030800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1154.2
030900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1154.2
031000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1154.2
031100 WRITE-LINE.                                                      NC1154.2
031200     ADD 1 TO RECORD-COUNT.                                       NC1154.2
031300*    IF RECORD-COUNT GREATER 42                                   NC1154.2
031400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1154.2
031500*        MOVE SPACE TO DUMMY-RECORD                               NC1154.2
031600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1154.2
031700*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1154.2
031800*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1154.2
031900*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1154.2
032000*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1154.2
032100*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1154.2
032200*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1154.2
032300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1154.2
032400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1154.2
032500*        MOVE ZERO TO RECORD-COUNT.                               NC1154.2
032600     PERFORM WRT-LN.                                              NC1154.2
032700 WRT-LN.                                                          NC1154.2
032800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1154.2
032900     MOVE SPACE TO DUMMY-RECORD.                                  NC1154.2
033000 BLANK-LINE-PRINT.                                                NC1154.2
033100     PERFORM WRT-LN.                                              NC1154.2
033200 FAIL-ROUTINE.                                                    NC1154.2
033300     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1154.2
033400            GO TO FAIL-ROUTINE-WRITE.                             NC1154.2
033500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1154.2
033600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1154.2
033700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1154.2
033800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1154.2
033900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1154.2
034000     GO TO  FAIL-ROUTINE-EX.                                      NC1154.2
034100 FAIL-ROUTINE-WRITE.                                              NC1154.2
034200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1154.2
034300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1154.2
034400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1154.2
034500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1154.2
034600 FAIL-ROUTINE-EX. EXIT.                                           NC1154.2
034700 BAIL-OUT.                                                        NC1154.2
034800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1154.2
034900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1154.2
035000 BAIL-OUT-WRITE.                                                  NC1154.2
035100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1154.2
035200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1154.2
035300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1154.2
035400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1154.2
035500 BAIL-OUT-EX. EXIT.                                               NC1154.2
035600 CCVS1-EXIT.                                                      NC1154.2
035700     EXIT.                                                        NC1154.2
035800 SECT-NC115A-001 SECTION.                                         NC1154.2
035900 INSP-INIT-F1-1.                                                  NC1154.2
036000     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
036100     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
036200 INSP-TEST-F1-1-0.                                                NC1154.2
036300     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR CHARACTERS.       NC1154.2
036400 INSP-TEST-F1-1-1.                                                NC1154.2
036500     IF      WRK-DU-999-1 EQUAL TO 83                             NC1154.2
036600     PERFORM PASS                                                 NC1154.2
036700     GO      TO INSP-WRITE-F1-1.                                  NC1154.2
036800     GO TO INSP-FAIL-F1-1.                                        NC1154.2
036900 INSP-DELETE-F1-1.                                                NC1154.2
037000     PERFORM DE-LETE.                                             NC1154.2
037100     GO TO INSP-WRITE-F1-1.                                       NC1154.2
037200 INSP-FAIL-F1-1.                                                  NC1154.2
037300     PERFORM FAIL.                                                NC1154.2
037400     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC1154.2
037500     MOVE 83 TO CORRECT-N.                                        NC1154.2
037600 INSP-WRITE-F1-1.                                                 NC1154.2
037700     MOVE "INSP-TEST-F1-1" TO PAR-NAME.                           NC1154.2
037800     MOVE "TALLY FOR CHARACTERS" TO FEATURE.                      NC1154.2
037900     PERFORM PRINT-DETAIL.                                        NC1154.2
038000 INSP-INIT-F1-2.                                                  NC1154.2
038100     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
038200     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
038300 INSP-TEST-F1-2-0.                                                NC1154.2
038400     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR ALL "A".          NC1154.2
038500 INSP-TEST-F1-2-1.                                                NC1154.2
038600     IF      WRK-DU-999-1 EQUAL TO 8                              NC1154.2
038700     PERFORM PASS                                                 NC1154.2
038800     GO      TO INSP-WRITE-F1-2.                                  NC1154.2
038900     GO TO INSP-FAIL-F1-2.                                        NC1154.2
039000 INSP-DELETE-F1-2.                                                NC1154.2
039100     PERFORM DE-LETE.                                             NC1154.2
039200     GO TO INSP-WRITE-F1-2.                                       NC1154.2
039300 INSP-FAIL-F1-2.                                                  NC1154.2
039400     PERFORM FAIL.                                                NC1154.2
039500     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC1154.2
039600     MOVE 8 TO CORRECT-N.                                         NC1154.2
039700 INSP-WRITE-F1-2.                                                 NC1154.2
039800     MOVE "INSP-TEST-F1-2" TO PAR-NAME.                           NC1154.2
039900     MOVE "TALLY ALL LITERAL" TO FEATURE.                         NC1154.2
040000     PERFORM PRINT-DETAIL.                                        NC1154.2
040100 INSP-INIT-F1-3.                                                  NC1154.2
040200     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
040300     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
040400 INSP-TEST-F1-3-0.                                                NC1154.2
040500     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR ALL SPACE.        NC1154.2
040600 INSP-TEST-F1-3-1.                                                NC1154.2
040700     IF      WRK-DU-999-1 EQUAL TO 17                             NC1154.2
040800     PERFORM PASS                                                 NC1154.2
040900     GO      TO INSP-WRITE-F1-3.                                  NC1154.2
041000     GO TO INSP-FAIL-F1-3.                                        NC1154.2
041100 INSP-DELETE-F1-3.                                                NC1154.2
041200     PERFORM DE-LETE.                                             NC1154.2
041300     GO TO INSP-WRITE-F1-3.                                       NC1154.2
041400 INSP-FAIL-F1-3.                                                  NC1154.2
041500     PERFORM FAIL.                                                NC1154.2
041600     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC1154.2
041700     MOVE 17 TO CORRECT-N.                                        NC1154.2
041800 INSP-WRITE-F1-3.                                                 NC1154.2
041900     MOVE "INSP-TEST-F1-3" TO PAR-NAME.                           NC1154.2
042000     MOVE "TALLY FOR ALL SPACE" TO FEATURE.                       NC1154.2
042100     PERFORM PRINT-DETAIL.                                        NC1154.2
042200 INSP-INIT-F1-4.                                                  NC1154.2
042300     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
042400     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
042500 INSP-TEST-F1-4-0.                                                NC1154.2
042600     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR LEADING "A".      NC1154.2
042700 INSP-TEST-F1-4-1.                                                NC1154.2
042800     IF      WRK-DU-999-1 EQUAL TO 1                              NC1154.2
042900     PERFORM PASS                                                 NC1154.2
043000     GO      TO INSP-WRITE-F1-4.                                  NC1154.2
043100     GO TO INSP-FAIL-F1-4.                                        NC1154.2
043200 INSP-DELETE-F1-4.                                                NC1154.2
043300     PERFORM DE-LETE.                                             NC1154.2
043400     GO TO INSP-WRITE-F1-4.                                       NC1154.2
043500 INSP-FAIL-F1-4.                                                  NC1154.2
043600     PERFORM FAIL.                                                NC1154.2
043700     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC1154.2
043800     MOVE 1 TO CORRECT-N.                                         NC1154.2
043900 INSP-WRITE-F1-4.                                                 NC1154.2
044000     MOVE "INSP-TEST-F1-4" TO PAR-NAME.                           NC1154.2
044100     MOVE "TALLY LEADING LIT" TO FEATURE.                         NC1154.2
044200     PERFORM PRINT-DETAIL.                                        NC1154.2
044300 INSP-INIT-F1-5.                                                  NC1154.2
044400     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
044500     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
044600 INSP-TEST-F1-5-0.                                                NC1154.2
044700     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR CHARACTERS        NC1154.2
044800         AFTER "W".                                               NC1154.2
044900 INSP-TEST-F1-5-1.                                                NC1154.2
045000     IF      WRK-DU-999-1 EQUAL TO 68                             NC1154.2
045100     PERFORM PASS                                                 NC1154.2
045200     GO      TO INSP-WRITE-F1-5.                                  NC1154.2
045300     GO TO INSP-FAIL-F1-5.                                        NC1154.2
045400 INSP-DELETE-F1-5.                                                NC1154.2
045500     PERFORM DE-LETE.                                             NC1154.2
045600     GO TO INSP-WRITE-F1-5.                                       NC1154.2
045700 INSP-FAIL-F1-5.                                                  NC1154.2
045800     PERFORM FAIL.                                                NC1154.2
045900     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC1154.2
046000     MOVE 68 TO CORRECT-N.                                        NC1154.2
046100 INSP-WRITE-F1-5.                                                 NC1154.2
046200     MOVE "INSP-TEST-F1-5" TO PAR-NAME.                           NC1154.2
046300     MOVE "FOR CHARS AFTER LIT" TO FEATURE.                       NC1154.2
046400     PERFORM PRINT-DETAIL.                                        NC1154.2
046500 INSP-INIT-F1-6.                                                  NC1154.2
046600     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
046700     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
046800 INSP-TEST-F1-6-0.                                                NC1154.2
046900     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR ALL " "           NC1154.2
047000         BEFORE INITIAL "W".                                      NC1154.2
047100 INSP-TEST-F1-6-1.                                                NC1154.2
047200     IF      WRK-DU-999-1 EQUAL TO 4                              NC1154.2
047300     PERFORM PASS                                                 NC1154.2
047400     GO      TO INSP-WRITE-F1-6.                                  NC1154.2
047500     GO TO INSP-FAIL-F1-6.                                        NC1154.2
047600 INSP-DELETE-F1-6.                                                NC1154.2
047700     PERFORM DE-LETE.                                             NC1154.2
047800     GO TO INSP-WRITE-F1-6.                                       NC1154.2
047900 INSP-FAIL-F1-6.                                                  NC1154.2
048000     PERFORM FAIL.                                                NC1154.2
048100     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC1154.2
048200     MOVE 4 TO CORRECT-N.                                         NC1154.2
048300 INSP-WRITE-F1-6.                                                 NC1154.2
048400     MOVE "INSP-TEST-F1-6" TO PAR-NAME.                           NC1154.2
048500     MOVE "ALL BEFORE INITIAL" TO FEATURE.                        NC1154.2
048600     PERFORM PRINT-DETAIL.                                        NC1154.2
048700 INSP-INIT-F1-7.                                                  NC1154.2
048800     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
048900     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
049000 INSP-TEST-F1-7-0.                                                NC1154.2
049100     INSPECT WC-XN-83 TALLYING WRK-DU-999-1 FOR LEADING "Y"       NC1154.2
049200         AFTER INITIAL SPACE.                                     NC1154.2
049300 INSP-TEST-F1-7-1.                                                NC1154.2
049400     IF      WRK-DU-999-1 EQUAL TO 1                              NC1154.2
049500     PERFORM PASS                                                 NC1154.2
049600     GO      TO INSP-WRITE-F1-7.                                  NC1154.2
049700     GO TO INSP-FAIL-F1-7.                                        NC1154.2
049800 INSP-DELETE-F1-7.                                                NC1154.2
049900     PERFORM DE-LETE.                                             NC1154.2
050000     GO TO INSP-WRITE-F1-7.                                       NC1154.2
050100 INSP-FAIL-F1-7.                                                  NC1154.2
050200     PERFORM FAIL.                                                NC1154.2
050300     MOVE WRK-DU-999-1 TO COMPUTED-N.                             NC1154.2
050400     MOVE 1 TO CORRECT-N.                                         NC1154.2
050500 INSP-WRITE-F1-7.                                                 NC1154.2
050600     MOVE "INSP-TEST-F1-7" TO PAR-NAME.                           NC1154.2
050700     MOVE "LEAD LIT INITIAL FIG" TO FEATURE.                      NC1154.2
050800     PERFORM PRINT-DETAIL.                                        NC1154.2
050900 INSP-INIT-F2-1.                                                  NC1154.2
051000     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
051100     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
051200     MOVE "INSP-TEST-F2-1" TO PAR-NAME.                           NC1154.2
051300     MOVE "REP CHARS BY SPACE" TO FEATURE.                        NC1154.2
051400 INSP-TEST-F2-1-0.                                                NC1154.2
051500     INSPECT WRK-XN-83-1 REPLACING CHARACTERS BY SPACE.           NC1154.2
051600 INSP-TEST-F2-1-1.                                                NC1154.2
051700     IF      WRK-XN-83-1 EQUAL TO SPACE                           NC1154.2
051800             PERFORM PASS                                         NC1154.2
051900             PERFORM PRINT-DETAIL                                 NC1154.2
052000             GO      TO INSP-INIT-F2-2.                           NC1154.2
052100     GO TO INSP-FAIL-F2-1.                                        NC1154.2
052200 INSP-DELETE-F2-1.                                                NC1154.2
052300     PERFORM DE-LETE.                                             NC1154.2
052400     PERFORM PRINT-DETAIL.                                        NC1154.2
052500     GO TO INSP-INIT-F2-2.                                        NC1154.2
052600 INSP-FAIL-F2-1.                                                  NC1154.2
052700     PERFORM FAIL.                                                NC1154.2
052800     MOVE    WRK-XN-83-1 TO WRK-ER.                               NC1154.2
052900     MOVE    SPACE TO WRK-OK.                                     NC1154.2
053000     MOVE    WRK-OK-1-20 TO CORRECT-X.                            NC1154.2
053100     MOVE    WRK-ER-1-20 TO COMPUTED-X.                           NC1154.2
053200     PERFORM PRINT-DETAIL.                                        NC1154.2
053300     MOVE    WRK-OK-21-40 TO CORRECT-X.                           NC1154.2
053400     MOVE    WRK-ER-21-40 TO COMPUTED-X.                          NC1154.2
053500     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
053600     MOVE    WRK-OK-41-60 TO CORRECT-X.                           NC1154.2
053700     MOVE    WRK-ER-41-60 TO COMPUTED-X.                          NC1154.2
053800     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
053900     MOVE    WRK-OK-61-80 TO CORRECT-X.                           NC1154.2
054000     MOVE    WRK-ER-61-80 TO COMPUTED-X.                          NC1154.2
054100     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
054200     MOVE    WRK-OK-81-83 TO CORRECT-X.                           NC1154.2
054300     MOVE    WRK-ER-81-83 TO COMPUTED-X.                          NC1154.2
054400     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
054500 INSP-INIT-F2-2.                                                  NC1154.2
054600     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
054700     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
054800     MOVE "INSP-TEST-F2-2" TO PAR-NAME.                           NC1154.2
054900     MOVE "CHARS BEFORE INITIAL" TO FEATURE.                      NC1154.2
055000 INSP-TEST-F2-2-0.                                                NC1154.2
055100     INSPECT WRK-XN-83-1 REPLACING CHARACTERS BY "O"              NC1154.2
055200         BEFORE INITIAL "H".                                      NC1154.2
055300 INSP-TEST-F2-2-1.                                                NC1154.2
055400     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-1                     NC1154.2
055500             PERFORM PASS                                         NC1154.2
055600             PERFORM PRINT-DETAIL                                 NC1154.2
055700             GO      TO INSP-INIT-F2-3.                           NC1154.2
055800     GO TO INSP-FAIL-F2-2.                                        NC1154.2
055900 INSP-DELETE-F2-2.                                                NC1154.2
056000     PERFORM DE-LETE.                                             NC1154.2
056100     PERFORM PRINT-DETAIL.                                        NC1154.2
056200     GO TO INSP-INIT-F2-3.                                        NC1154.2
056300 INSP-FAIL-F2-2.                                                  NC1154.2
056400     PERFORM FAIL.                                                NC1154.2
056500     MOVE    WRK-XN-83-1 TO WRK-ER.                               NC1154.2
056600     MOVE    ANS-XN-83-1 TO WRK-OK.                               NC1154.2
056700     MOVE    WRK-OK-1-20 TO CORRECT-X.                            NC1154.2
056800     MOVE    WRK-ER-1-20 TO COMPUTED-X.                           NC1154.2
056900     PERFORM PRINT-DETAIL.                                        NC1154.2
057000     MOVE    WRK-OK-21-40 TO CORRECT-X.                           NC1154.2
057100     MOVE    WRK-ER-21-40 TO COMPUTED-X.                          NC1154.2
057200     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
057300     MOVE    WRK-OK-41-60 TO CORRECT-X.                           NC1154.2
057400     MOVE    WRK-ER-41-60 TO COMPUTED-X.                          NC1154.2
057500     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
057600     MOVE    WRK-OK-61-80 TO CORRECT-X.                           NC1154.2
057700     MOVE    WRK-ER-61-80 TO COMPUTED-X.                          NC1154.2
057800     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
057900     MOVE    WRK-OK-81-83 TO CORRECT-X.                           NC1154.2
058000     MOVE    WRK-ER-81-83 TO COMPUTED-X.                          NC1154.2
058100     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
058200 INSP-INIT-F2-3.                                                  NC1154.2
058300     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
058400     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
058500     MOVE "INSP-TEST-F2-3" TO PAR-NAME.                           NC1154.2
058600     MOVE "LEAD AFTER INIT ID" TO FEATURE.                        NC1154.2
058700 INSP-TEST-F2-3-0.                                                NC1154.2
058800     INSPECT WRK-XN-83-1 REPLACING LEADING SPACE-XN-1-1           NC1154.2
058900         BY COMMA-XN-1-1 AFTER INITIAL S-XN-1-1.                  NC1154.2
059000 INSP-TEST-F2-3-1.                                                NC1154.2
059100     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-2                     NC1154.2
059200             PERFORM PASS                                         NC1154.2
059300             PERFORM PRINT-DETAIL                                 NC1154.2
059400             GO TO INSP-INIT-F2-4.                                NC1154.2
059500     GO TO INSP-FAIL-F2-3.                                        NC1154.2
059600 INSP-DELETE-F2-3.                                                NC1154.2
059700     PERFORM DE-LETE.                                             NC1154.2
059800     PERFORM PRINT-DETAIL.                                        NC1154.2
059900     GO TO INSP-INIT-F2-4.                                        NC1154.2
060000 INSP-FAIL-F2-3.                                                  NC1154.2
060100     PERFORM FAIL.                                                NC1154.2
060200     MOVE    WRK-XN-83-1 TO WRK-ER.                               NC1154.2
060300     MOVE    ANS-XN-83-2 TO WRK-OK.                               NC1154.2
060400     MOVE    WRK-OK-1-20 TO CORRECT-X.                            NC1154.2
060500     MOVE    WRK-ER-1-20 TO COMPUTED-X.                           NC1154.2
060600     PERFORM PRINT-DETAIL.                                        NC1154.2
060700     MOVE    WRK-OK-21-40 TO CORRECT-X.                           NC1154.2
060800     MOVE    WRK-ER-21-40 TO COMPUTED-X.                          NC1154.2
060900     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
061000     MOVE    WRK-OK-41-60 TO CORRECT-X.                           NC1154.2
061100     MOVE    WRK-ER-41-60 TO COMPUTED-X.                          NC1154.2
061200     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
061300     MOVE    WRK-OK-61-80 TO CORRECT-X.                           NC1154.2
061400     MOVE    WRK-ER-61-80 TO COMPUTED-X.                          NC1154.2
061500     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
061600     MOVE    WRK-OK-81-83 TO CORRECT-X.                           NC1154.2
061700     MOVE    WRK-ER-81-83 TO COMPUTED-X.                          NC1154.2
061800     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
061900 INSP-INIT-F2-4.                                                  NC1154.2
062000     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
062100     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
062200     MOVE "INSP-TEST-F2-4" TO PAR-NAME.                           NC1154.2
062300     MOVE "FIRST BY ID BEFORE" TO FEATURE.                        NC1154.2
062400 INSP-TEST-F2-4-0.                                                NC1154.2
062500     INSPECT WRK-XN-83-1 REPLACING FIRST "A" BY O-XN-1-1          NC1154.2
062600         BEFORE INITIAL "H".                                      NC1154.2
062700 INSP-TEST-F2-4-1.                                                NC1154.2
062800     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-1                     NC1154.2
062900             PERFORM PASS                                         NC1154.2
063000             PERFORM PRINT-DETAIL                                 NC1154.2
063100             GO      TO INSP-INIT-F2-5.                           NC1154.2
063200     GO TO INSP-FAIL-F2-4.                                        NC1154.2
063300 INSP-DELETE-F2-4.                                                NC1154.2
063400     PERFORM DE-LETE.                                             NC1154.2
063500     PERFORM PRINT-DETAIL.                                        NC1154.2
063600     GO TO INSP-INIT-F2-5.                                        NC1154.2
063700 INSP-FAIL-F2-4.                                                  NC1154.2
063800     PERFORM FAIL.                                                NC1154.2
063900     MOVE    WRK-XN-83-1 TO WRK-ER.                               NC1154.2
064000     MOVE    ANS-XN-83-1 TO WRK-OK.                               NC1154.2
064100     MOVE    WRK-OK-1-20 TO CORRECT-X.                            NC1154.2
064200     MOVE    WRK-ER-1-20 TO COMPUTED-X.                           NC1154.2
064300     PERFORM PRINT-DETAIL.                                        NC1154.2
064400     MOVE    WRK-OK-21-40 TO CORRECT-X.                           NC1154.2
064500     MOVE    WRK-ER-21-40 TO COMPUTED-X.                          NC1154.2
064600     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
064700     MOVE    WRK-OK-41-60 TO CORRECT-X.                           NC1154.2
064800     MOVE    WRK-ER-41-60 TO COMPUTED-X.                          NC1154.2
064900     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
065000     MOVE    WRK-OK-61-80 TO CORRECT-X.                           NC1154.2
065100     MOVE    WRK-ER-61-80 TO COMPUTED-X.                          NC1154.2
065200     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
065300     MOVE    WRK-OK-81-83 TO CORRECT-X.                           NC1154.2
065400     MOVE    WRK-ER-81-83 TO COMPUTED-X.                          NC1154.2
065500     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
065600 INSP-INIT-F2-5.                                                  NC1154.2
065700     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
065800     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
065900     MOVE "INSP-TEST-F2-5" TO PAR-NAME.                           NC1154.2
066000     MOVE "ALL ID BY LIT AFTER" TO FEATURE.                       NC1154.2
066100 INSP-TEST-F2-5-0.                                                NC1154.2
066200     INSPECT WRK-XN-83-1 REPLACING ALL SPACE-XN-1-1 BY "-"        NC1154.2
066300         AFTER L-XN-1-1.                                          NC1154.2
066400 INSP-TEST-F2-5-1.                                                NC1154.2
066500     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-3                     NC1154.2
066600             PERFORM PASS                                         NC1154.2
066700             PERFORM PRINT-DETAIL                                 NC1154.2
066800             GO      TO INSP-INIT-F3-1.                           NC1154.2
066900     GO TO INSP-FAIL-F2-5.                                        NC1154.2
067000 INSP-DELETE-F2-5.                                                NC1154.2
067100     PERFORM DE-LETE.                                             NC1154.2
067200     PERFORM PRINT-DETAIL.                                        NC1154.2
067300     GO TO INSP-INIT-F3-1.                                        NC1154.2
067400 INSP-FAIL-F2-5.                                                  NC1154.2
067500     PERFORM FAIL.                                                NC1154.2
067600     MOVE    WRK-XN-83-1 TO WRK-ER.                               NC1154.2
067700     MOVE    ANS-XN-83-3 TO WRK-OK.                               NC1154.2
067800     MOVE    WRK-OK-1-20 TO CORRECT-X.                            NC1154.2
067900     MOVE    WRK-ER-1-20 TO COMPUTED-X.                           NC1154.2
068000     PERFORM PRINT-DETAIL.                                        NC1154.2
068100     MOVE    WRK-OK-21-40 TO CORRECT-X.                           NC1154.2
068200     MOVE    WRK-ER-21-40 TO COMPUTED-X.                          NC1154.2
068300     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
068400     MOVE    WRK-OK-41-60 TO CORRECT-X.                           NC1154.2
068500     MOVE    WRK-ER-41-60 TO COMPUTED-X.                          NC1154.2
068600     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
068700     MOVE    WRK-OK-61-80 TO CORRECT-X.                           NC1154.2
068800     MOVE    WRK-ER-61-80 TO COMPUTED-X.                          NC1154.2
068900     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
069000     MOVE    WRK-OK-81-83 TO CORRECT-X.                           NC1154.2
069100     MOVE    WRK-ER-81-83 TO COMPUTED-X.                          NC1154.2
069200     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1154.2
069300 INSP-INIT-F3-1.                                                  NC1154.2
069400     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
069500     MOVE "INSP-TEST-F3-1" TO PAR-NAME.                           NC1154.2
069600     MOVE "TALLY-REPLACE CHARS" TO FEATURE.                       NC1154.2
069700     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
069800     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
069900     MOVE    1 TO REC-CT.                                         NC1154.2
070000 INSP-TEST-F3-1-0.                                                NC1154.2
070100     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR CHARACTERS     NC1154.2
070200         REPLACING CHARACTERS BY " ".                             NC1154.2
070300     GO      TO TEST-F3-1-1.                                      NC1154.2
070400 INSP-DELETE-F3-1.                                                NC1154.2
070500     PERFORM DE-LETE.                                             NC1154.2
070600     PERFORM PRINT-DETAIL.                                        NC1154.2
070700     GO      TO INSP-INIT-F3-2.                                   NC1154.2
070800 TEST-F3-1-1.                                                     NC1154.2
070900     IF      WRK-DU-999-1 EQUAL TO 83                             NC1154.2
071000     PERFORM PASS                                                 NC1154.2
071100     PERFORM PRINT-DETAIL                                         NC1154.2
071200     ELSE                                                         NC1154.2
071300     PERFORM FAIL                                                 NC1154.2
071400     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC1154.2
071500     MOVE    83 TO CORRECT-N                                      NC1154.2
071600     PERFORM PRINT-DETAIL.                                        NC1154.2
071700     ADD     1 TO REC-CT.                                         NC1154.2
071800 TEST-F3-1-2.                                                     NC1154.2
071900     IF   WRK-XN-83-1 EQUAL TO SPACE                              NC1154.2
072000          PERFORM PASS                                            NC1154.2
072100          PERFORM PRINT-DETAIL                                    NC1154.2
072200     ELSE                                                         NC1154.2
072300          PERFORM FAIL                                            NC1154.2
072400          MOVE  WRK-XN-83-1 TO WRK-ER                             NC1154.2
072500          MOVE  SPACES      TO WRK-OK                             NC1154.2
072600          MOVE  WRK-OK-1-20 TO CORRECT-X                          NC1154.2
072700          MOVE  WRK-ER-1-20 TO COMPUTED-X                         NC1154.2
072800          PERFORM PRINT-DETAIL                                    NC1154.2
072900          MOVE  WRK-OK-21-40 TO CORRECT-X                         NC1154.2
073000          MOVE  WRK-ER-21-40 TO COMPUTED-X                        NC1154.2
073100          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
073200          MOVE  WRK-OK-41-60 TO CORRECT-X                         NC1154.2
073300          MOVE  WRK-ER-41-60 TO COMPUTED-X                        NC1154.2
073400          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
073500          MOVE  WRK-OK-61-80 TO CORRECT-X                         NC1154.2
073600          MOVE  WRK-ER-61-80 TO COMPUTED-X                        NC1154.2
073700          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
073800          MOVE  WRK-OK-81-83 TO CORRECT-X                         NC1154.2
073900          MOVE  WRK-ER-81-83 TO COMPUTED-X                        NC1154.2
074000          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.        NC1154.2
074100 INSP-INIT-F3-2.                                                  NC1154.2
074200     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
074300     MOVE "INSP-TEST-F3-2" TO PAR-NAME.                           NC1154.2
074400     MOVE "LIT BY BEFORE INIT" TO FEATURE.                        NC1154.2
074500     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
074600     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
074700     MOVE    1 TO REC-CT.                                         NC1154.2
074800 INSP-TEST-F3-2-0.                                                NC1154.2
074900     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR CHARACTERS     NC1154.2
075000         AFTER L-XN-1-1 REPLACING ALL "A" BY "E" BEFORE INITIAL   NC1154.2
075100     H-XN-1-1.                                                    NC1154.2
075200     GO      TO TEST-F3-2-1.                                      NC1154.2
075300 INSP-DELETE-F3-2.                                                NC1154.2
075400     PERFORM DE-LETE.                                             NC1154.2
075500     PERFORM PRINT-DETAIL.                                        NC1154.2
075600     GO      TO INSP-INIT-F3-3.                                   NC1154.2
075700 TEST-F3-2-1.                                                     NC1154.2
075800     IF      WRK-DU-999-1 EQUAL TO 6                              NC1154.2
075900     PERFORM PASS                                                 NC1154.2
076000     PERFORM PRINT-DETAIL                                         NC1154.2
076100     ELSE                                                         NC1154.2
076200     PERFORM FAIL                                                 NC1154.2
076300     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC1154.2
076400     MOVE    6 TO CORRECT-N                                       NC1154.2
076500     PERFORM PRINT-DETAIL.                                        NC1154.2
076600     ADD     1 TO REC-CT.                                         NC1154.2
076700 TEST-F3-2-2.                                                     NC1154.2
076800     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-4                     NC1154.2
076900          PERFORM PASS                                            NC1154.2
077000          PERFORM PRINT-DETAIL                                    NC1154.2
077100     ELSE                                                         NC1154.2
077200          PERFORM FAIL                                            NC1154.2
077300          MOVE    WRK-XN-83-1 TO WRK-ER                           NC1154.2
077400          MOVE    ANS-XN-83-4 TO WRK-OK                           NC1154.2
077500          MOVE    WRK-OK-1-20 TO CORRECT-X                        NC1154.2
077600          MOVE    WRK-ER-1-20 TO COMPUTED-X                       NC1154.2
077700          PERFORM PRINT-DETAIL                                    NC1154.2
077800          MOVE    WRK-OK-21-40 TO CORRECT-X                       NC1154.2
077900          MOVE    WRK-ER-21-40 TO COMPUTED-X                      NC1154.2
078000          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
078100          MOVE    WRK-OK-41-60 TO CORRECT-X                       NC1154.2
078200          MOVE    WRK-ER-41-60 TO COMPUTED-X                      NC1154.2
078300          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
078400          MOVE    WRK-OK-61-80 TO CORRECT-X                       NC1154.2
078500          MOVE    WRK-ER-61-80 TO COMPUTED-X                      NC1154.2
078600          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
078700          MOVE    WRK-OK-81-83 TO CORRECT-X                       NC1154.2
078800          MOVE    WRK-ER-81-83 TO COMPUTED-X                      NC1154.2
078900          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.        NC1154.2
079000 INSP-INIT-F3-3.                                                  NC1154.2
079100     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
079200     MOVE "INSP-TEST-F3-3" TO PAR-NAME.                           NC1154.2
079300     MOVE "REPL FIRST AFTER" TO FEATURE.                          NC1154.2
079400     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
079500     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
079600     MOVE    1 TO REC-CT.                                         NC1154.2
079700 INSP-TEST-F3-3-0.                                                NC1154.2
079800     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR ALL "A" BEFORE NC1154.2
079900         L-XN-1-1 REPLACING FIRST  A-XN-1-1 BY "O" AFTER          NC1154.2
080000         INITIAL H-XN-1-1.                                        NC1154.2
080100     GO      TO TEST-F3-3-1.                                      NC1154.2
080200 INSP-DELETE-F3-3.                                                NC1154.2
080300     PERFORM DE-LETE.                                             NC1154.2
080400     PERFORM PRINT-DETAIL.                                        NC1154.2
080500     GO      TO INSP-INIT-F3-4.                                   NC1154.2
080600 TEST-F3-3-1.                                                     NC1154.2
080700     IF      WRK-DU-999-1 EQUAL TO 7                              NC1154.2
080800     PERFORM PASS                                                 NC1154.2
080900     PERFORM PRINT-DETAIL                                         NC1154.2
081000     ELSE                                                         NC1154.2
081100     PERFORM FAIL                                                 NC1154.2
081200     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC1154.2
081300     MOVE    7 TO CORRECT-N                                       NC1154.2
081400     PERFORM PRINT-DETAIL.                                        NC1154.2
081500     ADD     1 TO REC-CT.                                         NC1154.2
081600 TEST-F3-3-2.                                                     NC1154.2
081700     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-5                     NC1154.2
081800          PERFORM PASS                                            NC1154.2
081900          PERFORM PRINT-DETAIL                                    NC1154.2
082000     ELSE                                                         NC1154.2
082100          PERFORM FAIL                                            NC1154.2
082200          MOVE    WRK-XN-83-1 TO WRK-ER                           NC1154.2
082300          MOVE    ANS-XN-83-5 TO WRK-OK                           NC1154.2
082400          MOVE    WRK-OK-1-20 TO CORRECT-X                        NC1154.2
082500          MOVE    WRK-ER-1-20 TO COMPUTED-X                       NC1154.2
082600          PERFORM PRINT-DETAIL                                    NC1154.2
082700          MOVE    WRK-OK-21-40 TO CORRECT-X                       NC1154.2
082800          MOVE    WRK-ER-21-40 TO COMPUTED-X                      NC1154.2
082900          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
083000          MOVE    WRK-OK-41-60 TO CORRECT-X                       NC1154.2
083100          MOVE    WRK-ER-41-60 TO COMPUTED-X                      NC1154.2
083200          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
083300          MOVE    WRK-OK-61-80 TO CORRECT-X                       NC1154.2
083400          MOVE    WRK-ER-61-80 TO COMPUTED-X                      NC1154.2
083500          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
083600          MOVE    WRK-OK-81-83 TO CORRECT-X                       NC1154.2
083700          MOVE    WRK-ER-81-83 TO COMPUTED-X                      NC1154.2
083800          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.        NC1154.2
083900 INSP-INIT-F3-4.                                                  NC1154.2
084000     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
084100     MOVE "INSP-TEST-F3-4" TO PAR-NAME.                           NC1154.2
084200     MOVE "FOR LEADING" TO FEATURE.                               NC1154.2
084300     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
084400     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
084500     MOVE    1 TO REC-CT.                                         NC1154.2
084600 INSP-TEST-F3-4-0.                                                NC1154.2
084700     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR LEADING        NC1154.2
084800         A-XN-1-1 REPLACING LEADING A-XN-1-1 BY "O".              NC1154.2
084900     GO      TO TEST-F3-4-1.                                      NC1154.2
085000 INSP-DELETE-F3-4.                                                NC1154.2
085100     PERFORM DE-LETE.                                             NC1154.2
085200     PERFORM PRINT-DETAIL.                                        NC1154.2
085300     GO      TO INSP-INIT-F3-5.                                   NC1154.2
085400 TEST-F3-4-1.                                                     NC1154.2
085500     IF      WRK-DU-999-1 EQUAL TO 1                              NC1154.2
085600     PERFORM PASS                                                 NC1154.2
085700     PERFORM PRINT-DETAIL                                         NC1154.2
085800     ELSE                                                         NC1154.2
085900     PERFORM FAIL                                                 NC1154.2
086000     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC1154.2
086100     MOVE    1 TO CORRECT-N                                       NC1154.2
086200     PERFORM PRINT-DETAIL.                                        NC1154.2
086300     ADD     1 TO REC-CT.                                         NC1154.2
086400 TEST-F3-4-2.                                                     NC1154.2
086500     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-1                     NC1154.2
086600          PERFORM PASS                                            NC1154.2
086700          PERFORM PRINT-DETAIL                                    NC1154.2
086800     ELSE                                                         NC1154.2
086900          PERFORM FAIL                                            NC1154.2
087000          MOVE    WRK-XN-83-1 TO WRK-ER                           NC1154.2
087100          MOVE    ANS-XN-83-1 TO WRK-OK                           NC1154.2
087200          MOVE    WRK-OK-1-20 TO CORRECT-X                        NC1154.2
087300          MOVE    WRK-ER-1-20 TO COMPUTED-X                       NC1154.2
087400          PERFORM PRINT-DETAIL                                    NC1154.2
087500          MOVE    WRK-OK-21-40 TO CORRECT-X                       NC1154.2
087600          MOVE    WRK-ER-21-40 TO COMPUTED-X                      NC1154.2
087700          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
087800          MOVE    WRK-OK-41-60 TO CORRECT-X                       NC1154.2
087900          MOVE    WRK-ER-41-60 TO COMPUTED-X                      NC1154.2
088000          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
088100          MOVE    WRK-OK-61-80 TO CORRECT-X                       NC1154.2
088200          MOVE    WRK-ER-61-80 TO COMPUTED-X                      NC1154.2
088300          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
088400          MOVE    WRK-OK-81-83 TO CORRECT-X                       NC1154.2
088500          MOVE    WRK-ER-81-83 TO COMPUTED-X                      NC1154.2
088600          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.        NC1154.2
088700 INSP-INIT-F3-5.                                                  NC1154.2
088800     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
088900     MOVE "INSP-TEST-F3-5" TO PAR-NAME.                           NC1154.2
089000     MOVE "LIT BY AFTER INIT" TO FEATURE.                         NC1154.2
089100     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
089200     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
089300     MOVE    1 TO REC-CT.                                         NC1154.2
089400 INSP-TEST-F3-5-0.                                                NC1154.2
089500     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR ALL "A"        NC1154.2
089600         REPLACING FIRST "A" BY "O" AFTER INITIAL "Y".            NC1154.2
089700     GO      TO TEST-F3-5-1.                                      NC1154.2
089800 INSP-DELETE-F3-5.                                                NC1154.2
089900     PERFORM DE-LETE.                                             NC1154.2
090000     PERFORM PRINT-DETAIL.                                        NC1154.2
090100     GO      TO INSP-INIT-F3-6.                                   NC1154.2
090200 TEST-F3-5-1.                                                     NC1154.2
090300     IF      WRK-DU-999-1 EQUAL TO 8                              NC1154.2
090400     PERFORM PASS                                                 NC1154.2
090500     PERFORM PRINT-DETAIL                                         NC1154.2
090600     ELSE                                                         NC1154.2
090700     PERFORM FAIL                                                 NC1154.2
090800     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC1154.2
090900     MOVE    8 TO CORRECT-N                                       NC1154.2
091000     PERFORM PRINT-DETAIL.                                        NC1154.2
091100     ADD     1 TO REC-CT.                                         NC1154.2
091200 TEST-F3-5-2.                                                     NC1154.2
091300     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-5                     NC1154.2
091400          PERFORM PASS                                            NC1154.2
091500          PERFORM PRINT-DETAIL                                    NC1154.2
091600     ELSE                                                         NC1154.2
091700          PERFORM FAIL                                            NC1154.2
091800          MOVE    WRK-XN-83-1 TO WRK-ER                           NC1154.2
091900          MOVE    ANS-XN-83-5 TO WRK-OK                           NC1154.2
092000          MOVE    WRK-OK-1-20 TO CORRECT-X                        NC1154.2
092100          MOVE    WRK-ER-1-20 TO COMPUTED-X                       NC1154.2
092200          PERFORM PRINT-DETAIL                                    NC1154.2
092300          MOVE    WRK-OK-21-40 TO CORRECT-X                       NC1154.2
092400          MOVE    WRK-ER-21-40 TO COMPUTED-X                      NC1154.2
092500          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
092600          MOVE    WRK-OK-41-60 TO CORRECT-X                       NC1154.2
092700          MOVE    WRK-ER-41-60 TO COMPUTED-X                      NC1154.2
092800          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
092900          MOVE    WRK-OK-61-80 TO CORRECT-X                       NC1154.2
093000          MOVE    WRK-ER-61-80 TO COMPUTED-X                      NC1154.2
093100          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
093200          MOVE    WRK-OK-81-83 TO CORRECT-X                       NC1154.2
093300          MOVE    WRK-ER-81-83 TO COMPUTED-X                      NC1154.2
093400          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.        NC1154.2
093500 INSP-INIT-F3-6.                                                  NC1154.2
093600     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
093700     MOVE "INSP-TEST-F3-6" TO PAR-NAME.                           NC1154.2
093800     MOVE "CHAR AFTER ALL BEF" TO FEATURE.                        NC1154.2
093900     MOVE ZERO TO WRK-DU-999-1.                                   NC1154.2
094000     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
094100     MOVE 1 TO REC-CT.                                            NC1154.2
094200 INSP-TEST-F3-6-0.                                                NC1154.2
094300     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR CHARACTERS     NC1154.2
094400         AFTER A-XN-1-1 REPLACING ALL "A" BY "O" BEFORE H-XN-1-1. NC1154.2
094500     GO      TO TEST-F3-6-1.                                      NC1154.2
094600 INSP-DELETE-F3-6.                                                NC1154.2
094700     PERFORM DE-LETE.                                             NC1154.2
094800     PERFORM PRINT-DETAIL.                                        NC1154.2
094900     GO      TO INSP-INIT-F3-7.                                   NC1154.2
095000 TEST-F3-6-1.                                                     NC1154.2
095100     IF      WRK-DU-999-1 EQUAL TO 82                             NC1154.2
095200     PERFORM PASS                                                 NC1154.2
095300     PERFORM PRINT-DETAIL                                         NC1154.2
095400     ELSE                                                         NC1154.2
095500     PERFORM FAIL                                                 NC1154.2
095600     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC1154.2
095700     MOVE    82 TO CORRECT-N                                      NC1154.2
095800     PERFORM PRINT-DETAIL.                                        NC1154.2
095900     ADD     1 TO REC-CT.                                         NC1154.2
096000 TEST-F3-6-2.                                                     NC1154.2
096100     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-1                     NC1154.2
096200          PERFORM PASS                                            NC1154.2
096300          PERFORM PRINT-DETAIL                                    NC1154.2
096400     ELSE                                                         NC1154.2
096500          PERFORM FAIL                                            NC1154.2
096600          MOVE    WRK-XN-83-1 TO WRK-ER                           NC1154.2
096700          MOVE    ANS-XN-83-1 TO WRK-OK                           NC1154.2
096800          MOVE    WRK-OK-1-20 TO CORRECT-X                        NC1154.2
096900          MOVE    WRK-ER-1-20 TO COMPUTED-X                       NC1154.2
097000          PERFORM PRINT-DETAIL                                    NC1154.2
097100          MOVE    WRK-OK-21-40 TO CORRECT-X                       NC1154.2
097200          MOVE    WRK-ER-21-40 TO COMPUTED-X                      NC1154.2
097300          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
097400          MOVE    WRK-OK-41-60 TO CORRECT-X                       NC1154.2
097500          MOVE    WRK-ER-41-60 TO COMPUTED-X                      NC1154.2
097600          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
097700          MOVE    WRK-OK-61-80 TO CORRECT-X                       NC1154.2
097800          MOVE    WRK-ER-61-80 TO COMPUTED-X                      NC1154.2
097900          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX         NC1154.2
098000          MOVE    WRK-OK-81-83 TO CORRECT-X                       NC1154.2
098100          MOVE    WRK-ER-81-83 TO COMPUTED-X                      NC1154.2
098200          PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.        NC1154.2
098300 INSP-INIT-F3-7.                                                  NC1154.2
098400     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
098500     MOVE ZERO TO WRK-DU-999-1 WRK-DU-999-2 WRK-DU-999-3          NC1154.2
098600         WRK-DU-999-4.                                            NC1154.2
098700     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
098800     MOVE "INSP-TEST-F3-7" TO PAR-NAME.                           NC1154.2
098900     MOVE "TALLY SERIES" TO FEATURE.                              NC1154.2
099000     MOVE 1 TO REC-CT.                                            NC1154.2
099100 INSP-TEST-F3-7-0.                                                NC1154.2
099200     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR ALL "A".       NC1154.2
099300     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-2 FOR LEADING "A".   NC1154.2
099400     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-3 FOR CHARACTERS     NC1154.2
099500             BEFORE ".".                                          NC1154.2
099600     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-4 FOR CHARACTERS     NC1154.2
099700             AFTER "L".                                           NC1154.2
099800     INSPECT WRK-XN-83-1 REPLACING CHARACTERS BY "Z" AFTER "L".   NC1154.2
099900     GO TO INSP-TEST-F3-7-1.                                      NC1154.2
100000 INSP-DELETE-F3-7.                                                NC1154.2
100100     PERFORM DE-LETE.                                             NC1154.2
100200     PERFORM PRINT-DETAIL.                                        NC1154.2
100300     GO TO INSP-INIT-F3-8.                                        NC1154.2
100400 INSP-TEST-F3-7-1.                                                NC1154.2
100500     IF WRK-DU-999-1 EQUAL TO 8 PERFORM PASS PERFORM PRINT-DETAIL NC1154.2
100600     ELSE                                                         NC1154.2
100700     PERFORM FAIL MOVE WRK-DU-999-1 TO COMPUTED-N MOVE 8          NC1154.2
100800     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1154.2
100900     ADD 1 TO REC-CT.                                             NC1154.2
101000 INSP-TEST-F3-7-2.                                                NC1154.2
101100     IF WRK-DU-999-2 EQUAL TO 1 PERFORM PASS PERFORM PRINT-DETAIL NC1154.2
101200     ELSE                                                         NC1154.2
101300     PERFORM FAIL MOVE WRK-DU-999-2 TO COMPUTED-N MOVE 1          NC1154.2
101400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1154.2
101500     ADD 1 TO REC-CT.                                             NC1154.2
101600 INSP-TEST-F3-7-3.                                                NC1154.2
101700     IF WRK-DU-999-3 EQUAL TO 15 PERFORM PASS PERFORM PRINT-DETAILNC1154.2
101800     ELSE                                                         NC1154.2
101900     PERFORM FAIL MOVE WRK-DU-999-3 TO COMPUTED-N MOVE 15         NC1154.2
102000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1154.2
102100     ADD 1 TO REC-CT.                                             NC1154.2
102200 INSP-TEST-F3-7-4.                                                NC1154.2
102300     IF WRK-DU-999-4 EQUAL TO 6 PERFORM PASS PERFORM PRINT-DETAIL NC1154.2
102400         ELSE                                                     NC1154.2
102500     PERFORM FAIL MOVE WRK-DU-999-4 TO COMPUTED-N MOVE 6          NC1154.2
102600     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1154.2
102700     ADD 1 TO REC-CT.                                             NC1154.2
102800 INSP-TEST-F3-7-5.                                                NC1154.2
102900     IF WRK-XN-83-1 EQUAL TO ANS-XN-83-6 PERFORM PASS PERFORM     NC1154.2
103000     PRINT-DETAIL ELSE                                            NC1154.2
103100        PERFORM FAIL                                              NC1154.2
103200        MOVE    WRK-XN-83-1 TO WRK-ER                             NC1154.2
103300        MOVE    ANS-XN-83-6 TO WRK-OK                             NC1154.2
103400        MOVE    WRK-OK-1-20 TO CORRECT-X                          NC1154.2
103500        MOVE    WRK-ER-1-20 TO COMPUTED-X                         NC1154.2
103600        PERFORM PRINT-DETAIL                                      NC1154.2
103700        MOVE    WRK-OK-21-40 TO CORRECT-X                         NC1154.2
103800        MOVE    WRK-ER-21-40 TO COMPUTED-X                        NC1154.2
103900        PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX           NC1154.2
104000        MOVE    WRK-OK-41-60 TO CORRECT-X                         NC1154.2
104100        MOVE    WRK-ER-41-60 TO COMPUTED-X                        NC1154.2
104200        PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX           NC1154.2
104300        MOVE    WRK-OK-61-80 TO CORRECT-X                         NC1154.2
104400        MOVE    WRK-ER-61-80 TO COMPUTED-X                        NC1154.2
104500        PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX           NC1154.2
104600        MOVE    WRK-OK-81-83 TO CORRECT-X                         NC1154.2
104700        MOVE    WRK-ER-81-83 TO COMPUTED-X                        NC1154.2
104800        PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.          NC1154.2
104900 INSP-INIT-F3-8.                                                  NC1154.2
105000     MOVE   "VI-95 6.17.4" TO ANSI-REFERENCE.                     NC1154.2
105100     MOVE "INSP-TEST-F3-8" TO PAR-NAME.                           NC1154.2
105200     MOVE "REPLACE SERIES" TO FEATURE.                            NC1154.2
105300     MOVE ZERO TO REC-CT WRK-DU-999-1.                            NC1154.2
105400     MOVE WC-XN-83 TO WRK-XN-83-1.                                NC1154.2
105500     MOVE    1 TO REC-CT.                                         NC1154.2
105600 INSP-TEST-F3-8-0.                                                NC1154.2
105700     INSPECT WRK-XN-83-1 TALLYING WRK-DU-999-1 FOR CHARACTERS     NC1154.2
105800         BEFORE "." REPLACING CHARACTERS BY Z-XN-1-1 AFTER        NC1154.2
105900             L-XN-1-1.                                            NC1154.2
106000     INSPECT WRK-XN-83-1 REPLACING ALL " " BY HYPEN-XN-1-1.       NC1154.2
106100     INSPECT WRK-XN-83-1 REPLACING FIRST "C" BY P-XN-1-1.         NC1154.2
106200     INSPECT WRK-XN-83-1 REPLACING LEADING A-XN-1-1 BY O-XN-1-1.  NC1154.2
106300     INSPECT WRK-XN-83-1 REPLACING ALL "F" BY "Z" BEFORE G-XN-1-1.NC1154.2
106400     GO      TO TEST-F3-8-1.                                      NC1154.2
106500 INSP-DELETE-F3-8.                                                NC1154.2
106600     PERFORM DE-LETE.                                             NC1154.2
106700     PERFORM PRINT-DETAIL.                                        NC1154.2
106800     GO      TO CCVS-999999.                                      NC1154.2
106900 TEST-F3-8-1.                                                     NC1154.2
107000     IF      WRK-DU-999-1 EQUAL TO 15                             NC1154.2
107100     PERFORM PASS                                                 NC1154.2
107200     PERFORM PRINT-DETAIL                                         NC1154.2
107300     ELSE                                                         NC1154.2
107400     PERFORM FAIL                                                 NC1154.2
107500     MOVE    WRK-DU-999-1 TO COMPUTED-N                           NC1154.2
107600     MOVE    15 TO CORRECT-N                                      NC1154.2
107700     PERFORM PRINT-DETAIL.                                        NC1154.2
107800     ADD     1 TO REC-CT.                                         NC1154.2
107900 TEST-F3-8-2.                                                     NC1154.2
108000     IF      WRK-XN-83-1 EQUAL TO ANS-XN-83-7                     NC1154.2
108100     PERFORM PASS                                                 NC1154.2
108200     PERFORM PRINT-DETAIL                                         NC1154.2
108300     ELSE                                                         NC1154.2
108400        PERFORM FAIL                                              NC1154.2
108500        MOVE    WRK-XN-83-1 TO WRK-ER                             NC1154.2
108600        MOVE    ANS-XN-83-7 TO WRK-OK                             NC1154.2
108700        MOVE    WRK-OK-1-20 TO CORRECT-X                          NC1154.2
108800        MOVE    WRK-ER-1-20 TO COMPUTED-X                         NC1154.2
108900        PERFORM PRINT-DETAIL                                      NC1154.2
109000        MOVE    WRK-OK-21-40 TO CORRECT-X                         NC1154.2
109100        MOVE    WRK-ER-21-40 TO COMPUTED-X                        NC1154.2
109200        PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX           NC1154.2
109300        MOVE    WRK-OK-41-60 TO CORRECT-X                         NC1154.2
109400        MOVE    WRK-ER-41-60 TO COMPUTED-X                        NC1154.2
109500        PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX           NC1154.2
109600        MOVE    WRK-OK-61-80 TO CORRECT-X                         NC1154.2
109700        MOVE    WRK-ER-61-80 TO COMPUTED-X                        NC1154.2
109800        PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX           NC1154.2
109900        MOVE    WRK-OK-81-83 TO CORRECT-X                         NC1154.2
110000        MOVE    WRK-ER-81-83 TO COMPUTED-X                        NC1154.2
110100        PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.          NC1154.2
110200 CCVS-EXIT SECTION.                                               NC1154.2
110300 CCVS-999999.                                                     NC1154.2
110400     GO TO CLOSE-FILES.                                           NC1154.2
      *END-OF,NC115A                                                                  
