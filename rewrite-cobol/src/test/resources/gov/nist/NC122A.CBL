      *HEADER,COBOL,NC122A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1224.2
000200 PROGRAM-ID.                                                      NC1224.2
000300     NC122A.                                                      NC1224.2
000400****************************************************************  NC1224.2
000500*                                                              *  NC1224.2
000600*    VALIDATION FOR:-                                          *  NC1224.2
000700*                                                              *  NC1224.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1224.2
000900*                                                              *  NC1224.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1224.2
001100*                                                              *  NC1224.2
001200****************************************************************  NC1224.2
001300*                                                              *  NC1224.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1224.2
001500*                                                              *  NC1224.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1224.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1224.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1224.2
001900*                                                              *  NC1224.2
002000****************************************************************  NC1224.2
002100*                                                                 NC1224.2
002200*   PROGRAM NC122A TESTS THE USE OF INDEXED IDENTIFIERS USING     NC1224.2
002300*   FORMATS 1, 2 AND 3 OF THE "INSPECT" STATEMENT.                NC1224.2
002400*   SINGLE LEVEL AND RELATIVE INDEXING ARE USED.                  NC1224.2
002500*                                                                 NC1224.2
002600 ENVIRONMENT DIVISION.                                            NC1224.2
002700 CONFIGURATION SECTION.                                           NC1224.2
002800 SOURCE-COMPUTER.                                                 NC1224.2
002900     XXXXX082.                                                    NC1224.2
003000 OBJECT-COMPUTER.                                                 NC1224.2
003100     XXXXX083.                                                    NC1224.2
003200 INPUT-OUTPUT SECTION.                                            NC1224.2
003300 FILE-CONTROL.                                                    NC1224.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1224.2
003500     XXXXX055.                                                    NC1224.2
003600 DATA DIVISION.                                                   NC1224.2
003700 FILE SECTION.                                                    NC1224.2
003800 FD  PRINT-FILE.                                                  NC1224.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1224.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1224.2
004100 WORKING-STORAGE SECTION.                                         NC1224.2
004200 01  WRK-OK.                                                      NC1224.2
004300   03  WRK-OK-1-20                  PIC X(20).                    NC1224.2
004400   03  WRK-OK-21-40                 PIC X(20).                    NC1224.2
004500   03  WRK-OK-41-60                 PIC X(20).                    NC1224.2
004600   03  WRK-OK-61-80                 PIC X(20).                    NC1224.2
004700   03  WRK-OK-81-83                 PIC X(3).                     NC1224.2
004800 01  WRK-ER.                                                      NC1224.2
004900   03  WRK-ER-1-20                  PIC X(20).                    NC1224.2
005000   03  WRK-ER-21-40                 PIC X(20).                    NC1224.2
005100   03  WRK-ER-41-60                 PIC X(20).                    NC1224.2
005200   03  WRK-ER-61-80                 PIC X(20).                    NC1224.2
005300   03  WRK-ER-81-83                 PIC X(3).                     NC1224.2
005400 01  TABLE1.                                                      NC1224.2
005500     02  TABLE1-REC              PICTURE X(83)                    NC1224.2
005600                                 OCCURS 4 TIMES                   NC1224.2
005700                                 INDEXED BY INDEX1.               NC1224.2
005800 01  TABLE2.                                                      NC1224.2
005900     02  WRK-DU-999              PICTURE 999                      NC1224.2
006000                                 OCCURS 4 TIMES                   NC1224.2
006100                                 INDEXED BY INDEX2.               NC1224.2
006200 01  TABLE3.                                                      NC1224.2
006300     02  TABLE3-SYMBOL           PICTURE X                        NC1224.2
006400                                 OCCURS 3 TIMES                   NC1224.2
006500                                 INDEXED BY INDEX3.               NC1224.2
006600 01  TABLE4.                                                      NC1224.2
006700     02  TABLE4-LETTER           PICTURE X                        NC1224.2
006800                                 OCCURS 9 TIMES                   NC1224.2
006900                                 INDEXED BY INDEX4.               NC1224.2
007000 01  WC-XN-83  PIC X(83) VALUE                                    NC1224.2
007100     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1224.2
007200-    "IDS CAN NOT BE ALL BAD.".                                   NC1224.2
007300 01  ANS-XN-83-1 PIC X(83) VALUE                                  NC1224.2
007400     "OH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1224.2
007500-    "IDS CAN NOT BE ALL BAD.".                                   NC1224.2
007600 01  ANS-XN-83-2 PIC X(83) VALUE                                  NC1224.2
007700     "AH YES,AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1224.2
007800-    "IDS CAN NOT BE ALL BAD.".                                   NC1224.2
007900 01  ANS-XN-83-3 PIC X(83) VALUE                                  NC1224.2
008000     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1224.2
008100-    "IDS CAN NOT BE ALL-BAD.".                                   NC1224.2
008200 01  ANS-XN-83-4 PIC X(83) VALUE                                  NC1224.2
008300     "EH YES EH YES W.C. FRITOES HERE. ENYONE WHO HETES DOGS END KNC1224.2
008400-    "IDS CEN NOT BE ELL BAD.".                                   NC1224.2
008500 01  ANS-XN-83-5 PIC X(83) VALUE                                  NC1224.2
008600     "AH YES OH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC1224.2
008700-    "IDS CAN NOT BE ALL BAD.".                                   NC1224.2
008800 01  TEST-RESULTS.                                                NC1224.2
008900     02 FILLER                   PIC X      VALUE SPACE.          NC1224.2
009000     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1224.2
009100     02 FILLER                   PIC X      VALUE SPACE.          NC1224.2
009200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1224.2
009300     02 FILLER                   PIC X      VALUE SPACE.          NC1224.2
009400     02  PAR-NAME.                                                NC1224.2
009500       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1224.2
009600       03  PARDOT-X              PIC X      VALUE SPACE.          NC1224.2
009700       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1224.2
009800     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1224.2
009900     02 RE-MARK                  PIC X(61).                       NC1224.2
010000 01  TEST-COMPUTED.                                               NC1224.2
010100     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1224.2
010200     02 FILLER                   PIC X(17)  VALUE                 NC1224.2
010300            "       COMPUTED=".                                   NC1224.2
010400     02 COMPUTED-X.                                               NC1224.2
010500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1224.2
010600     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1224.2
010700                                 PIC -9(9).9(9).                  NC1224.2
010800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1224.2
010900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1224.2
011000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1224.2
011100     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1224.2
011200         04 COMPUTED-18V0                    PIC -9(18).          NC1224.2
011300         04 FILLER                           PIC X.               NC1224.2
011400     03 FILLER PIC X(50) VALUE SPACE.                             NC1224.2
011500 01  TEST-CORRECT.                                                NC1224.2
011600     02 FILLER PIC X(30) VALUE SPACE.                             NC1224.2
011700     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1224.2
011800     02 CORRECT-X.                                                NC1224.2
011900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1224.2
012000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1224.2
012100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1224.2
012200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1224.2
012300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1224.2
012400     03      CR-18V0 REDEFINES CORRECT-A.                         NC1224.2
012500         04 CORRECT-18V0                     PIC -9(18).          NC1224.2
012600         04 FILLER                           PIC X.               NC1224.2
012700     03 FILLER PIC X(2) VALUE SPACE.                              NC1224.2
012800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1224.2
012900 01  CCVS-C-1.                                                    NC1224.2
013000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1224.2
013100-    "SS  PARAGRAPH-NAME                                          NC1224.2
013200-    "       REMARKS".                                            NC1224.2
013300     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1224.2
013400 01  CCVS-C-2.                                                    NC1224.2
013500     02 FILLER                     PIC X        VALUE SPACE.      NC1224.2
013600     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1224.2
013700     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1224.2
013800     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1224.2
013900     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1224.2
014000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1224.2
014100 01  REC-CT                        PIC 99       VALUE ZERO.       NC1224.2
014200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1224.2
014300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1224.2
014400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1224.2
014500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1224.2
014600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1224.2
014700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1224.2
014800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1224.2
014900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1224.2
015000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1224.2
015100 01  CCVS-H-1.                                                    NC1224.2
015200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1224.2
015300     02  FILLER                    PIC X(42)    VALUE             NC1224.2
015400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1224.2
015500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1224.2
015600 01  CCVS-H-2A.                                                   NC1224.2
015700   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1224.2
015800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1224.2
015900   02  FILLER                        PIC XXXX   VALUE             NC1224.2
016000     "4.2 ".                                                      NC1224.2
016100   02  FILLER                        PIC X(28)  VALUE             NC1224.2
016200            " COPY - NOT FOR DISTRIBUTION".                       NC1224.2
016300   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1224.2
016400                                                                  NC1224.2
016500 01  CCVS-H-2B.                                                   NC1224.2
016600   02  FILLER                        PIC X(15)  VALUE             NC1224.2
016700            "TEST RESULT OF ".                                    NC1224.2
016800   02  TEST-ID                       PIC X(9).                    NC1224.2
016900   02  FILLER                        PIC X(4)   VALUE             NC1224.2
017000            " IN ".                                               NC1224.2
017100   02  FILLER                        PIC X(12)  VALUE             NC1224.2
017200     " HIGH       ".                                              NC1224.2
017300   02  FILLER                        PIC X(22)  VALUE             NC1224.2
017400            " LEVEL VALIDATION FOR ".                             NC1224.2
017500   02  FILLER                        PIC X(58)  VALUE             NC1224.2
017600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1224.2
017700 01  CCVS-H-3.                                                    NC1224.2
017800     02  FILLER                      PIC X(34)  VALUE             NC1224.2
017900            " FOR OFFICIAL USE ONLY    ".                         NC1224.2
018000     02  FILLER                      PIC X(58)  VALUE             NC1224.2
018100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1224.2
018200     02  FILLER                      PIC X(28)  VALUE             NC1224.2
018300            "  COPYRIGHT   1985 ".                                NC1224.2
018400 01  CCVS-E-1.                                                    NC1224.2
018500     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1224.2
018600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1224.2
018700     02 ID-AGAIN                     PIC X(9).                    NC1224.2
018800     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1224.2
018900 01  CCVS-E-2.                                                    NC1224.2
019000     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1224.2
019100     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1224.2
019200     02 CCVS-E-2-2.                                               NC1224.2
019300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1224.2
019400         03 FILLER                   PIC X      VALUE SPACE.      NC1224.2
019500         03 ENDER-DESC               PIC X(44)  VALUE             NC1224.2
019600            "ERRORS ENCOUNTERED".                                 NC1224.2
019700 01  CCVS-E-3.                                                    NC1224.2
019800     02  FILLER                      PIC X(22)  VALUE             NC1224.2
019900            " FOR OFFICIAL USE ONLY".                             NC1224.2
020000     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1224.2
020100     02  FILLER                      PIC X(58)  VALUE             NC1224.2
020200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1224.2
020300     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1224.2
020400     02 FILLER                       PIC X(15)  VALUE             NC1224.2
020500             " COPYRIGHT 1985".                                   NC1224.2
020600 01  CCVS-E-4.                                                    NC1224.2
020700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1224.2
020800     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1224.2
020900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1224.2
021000     02 FILLER                       PIC X(40)  VALUE             NC1224.2
021100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1224.2
021200 01  XXINFO.                                                      NC1224.2
021300     02 FILLER                       PIC X(19)  VALUE             NC1224.2
021400            "*** INFORMATION ***".                                NC1224.2
021500     02 INFO-TEXT.                                                NC1224.2
021600       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1224.2
021700       04 XXCOMPUTED                 PIC X(20).                   NC1224.2
021800       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1224.2
021900       04 XXCORRECT                  PIC X(20).                   NC1224.2
022000     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1224.2
022100 01  HYPHEN-LINE.                                                 NC1224.2
022200     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1224.2
022300     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1224.2
022400-    "*****************************************".                 NC1224.2
022500     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1224.2
022600-    "******************************".                            NC1224.2
022700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1224.2
022800     "NC122A".                                                    NC1224.2
022900 PROCEDURE DIVISION.                                              NC1224.2
023000 CCVS1 SECTION.                                                   NC1224.2
023100 OPEN-FILES.                                                      NC1224.2
023200     OPEN     OUTPUT PRINT-FILE.                                  NC1224.2
023300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1224.2
023400     MOVE    SPACE TO TEST-RESULTS.                               NC1224.2
023500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1224.2
023600     GO TO CCVS1-EXIT.                                            NC1224.2
023700 CLOSE-FILES.                                                     NC1224.2
023800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1224.2
023900 TERMINATE-CCVS.                                                  NC1224.2
024000*    EXIT PROGRAM.                                                NC1224.2
024100*TERMINATE-CALL.                                                  NC1224.2
024200     STOP     RUN.                                                NC1224.2
024300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1224.2
024400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1224.2
024500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1224.2
024600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1224.2
024700     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1224.2
024800 PRINT-DETAIL.                                                    NC1224.2
024900     IF REC-CT NOT EQUAL TO ZERO                                  NC1224.2
025000             MOVE "." TO PARDOT-X                                 NC1224.2
025100             MOVE REC-CT TO DOTVALUE.                             NC1224.2
025200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1224.2
025300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1224.2
025400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1224.2
025500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1224.2
025600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1224.2
025700     MOVE SPACE TO CORRECT-X.                                     NC1224.2
025800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1224.2
025900     MOVE     SPACE TO RE-MARK.                                   NC1224.2
026000 HEAD-ROUTINE.                                                    NC1224.2
026100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1224.2
026200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1224.2
026300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1224.2
026400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1224.2
026500 COLUMN-NAMES-ROUTINE.                                            NC1224.2
026600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1224.2
026700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1224.2
026800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1224.2
026900 END-ROUTINE.                                                     NC1224.2
027000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1224.2
027100 END-RTN-EXIT.                                                    NC1224.2
027200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1224.2
027300 END-ROUTINE-1.                                                   NC1224.2
027400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1224.2
027500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1224.2
027600      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1224.2
027700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1224.2
027800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1224.2
027900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1224.2
028000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1224.2
028100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1224.2
028200  END-ROUTINE-12.                                                 NC1224.2
028300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1224.2
028400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1224.2
028500         MOVE "NO " TO ERROR-TOTAL                                NC1224.2
028600         ELSE                                                     NC1224.2
028700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1224.2
028800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1224.2
028900     PERFORM WRITE-LINE.                                          NC1224.2
029000 END-ROUTINE-13.                                                  NC1224.2
029100     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1224.2
029200         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1224.2
029300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1224.2
029400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1224.2
029500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1224.2
029600      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1224.2
029700          MOVE "NO " TO ERROR-TOTAL                               NC1224.2
029800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1224.2
029900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1224.2
030000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1224.2
030100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1224.2
030200 WRITE-LINE.                                                      NC1224.2
030300     ADD 1 TO RECORD-COUNT.                                       NC1224.2
030400*    IF RECORD-COUNT GREATER 42                                   NC1224.2
030500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1224.2
030600*        MOVE SPACE TO DUMMY-RECORD                               NC1224.2
030700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1224.2
030800*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1224.2
030900*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1224.2
031000*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1224.2
031100*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1224.2
031200*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1224.2
031300*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1224.2
031400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1224.2
031500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1224.2
031600*        MOVE ZERO TO RECORD-COUNT.                               NC1224.2
031700     PERFORM WRT-LN.                                              NC1224.2
031800 WRT-LN.                                                          NC1224.2
031900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1224.2
032000     MOVE SPACE TO DUMMY-RECORD.                                  NC1224.2
032100 BLANK-LINE-PRINT.                                                NC1224.2
032200     PERFORM WRT-LN.                                              NC1224.2
032300 FAIL-ROUTINE.                                                    NC1224.2
032400     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1224.2
032500            GO TO FAIL-ROUTINE-WRITE.                             NC1224.2
032600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1224.2
032700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1224.2
032800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1224.2
032900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1224.2
033000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1224.2
033100     GO TO  FAIL-ROUTINE-EX.                                      NC1224.2
033200 FAIL-ROUTINE-WRITE.                                              NC1224.2
033300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1224.2
033400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1224.2
033500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1224.2
033600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1224.2
033700 FAIL-ROUTINE-EX. EXIT.                                           NC1224.2
033800 BAIL-OUT.                                                        NC1224.2
033900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1224.2
034000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1224.2
034100 BAIL-OUT-WRITE.                                                  NC1224.2
034200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1224.2
034300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1224.2
034400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1224.2
034500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1224.2
034600 BAIL-OUT-EX. EXIT.                                               NC1224.2
034700 CCVS1-EXIT.                                                      NC1224.2
034800     EXIT.                                                        NC1224.2
034900 BUILD-TABLE1.                                                    NC1224.2
035000     MOVE WC-XN-83 TO TABLE1-REC (1).                             NC1224.2
035100     MOVE WC-XN-83 TO TABLE1-REC (2).                             NC1224.2
035200     MOVE WC-XN-83 TO TABLE1-REC (3).                             NC1224.2
035300     MOVE WC-XN-83 TO TABLE1-REC (4).                             NC1224.2
035400 BUILD-TABLE3.                                                    NC1224.2
035500     MOVE " " TO TABLE3-SYMBOL (1).                               NC1224.2
035600     MOVE "," TO TABLE3-SYMBOL (2).                               NC1224.2
035700     MOVE "-" TO TABLE3-SYMBOL (3).                               NC1224.2
035800 BUILD-TABLE4.                                                    NC1224.2
035900     MOVE "A" TO TABLE4-LETTER (1).                               NC1224.2
036000     MOVE "D" TO TABLE4-LETTER (2).                               NC1224.2
036100     MOVE "G" TO TABLE4-LETTER (3).                               NC1224.2
036200     MOVE "H" TO TABLE4-LETTER (4).                               NC1224.2
036300     MOVE "L" TO TABLE4-LETTER (5).                               NC1224.2
036400     MOVE "O" TO TABLE4-LETTER (6).                               NC1224.2
036500     MOVE "Y" TO TABLE4-LETTER (7).                               NC1224.2
036600     MOVE "S" TO TABLE4-LETTER (8).                               NC1224.2
036700     MOVE "Z" TO TABLE4-LETTER (9).                               NC1224.2
036800 IND-INIT-GF-1.                                                   NC1224.2
036900     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
037000     MOVE ZERO TO TABLE2.                                         NC1224.2
037100     SET INDEX1 TO 1.                                             NC1224.2
037200     SET INDEX2 TO 1.                                             NC1224.2
037300 IND-TEST-GF-1-0.                                                 NC1224.2
037400     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
037500        FOR CHARACTERS.                                           NC1224.2
037600 IND-TEST-GF-1-1.                                                 NC1224.2
037700     IF WRK-DU-999 (INDEX2) EQUAL TO 83                           NC1224.2
037800        PERFORM PASS                                              NC1224.2
037900        GO TO IND-WRITE-GF-1.                                     NC1224.2
038000     GO TO IND-FAIL-GF-1.                                         NC1224.2
038100 IND-DELETE-GF-1.                                                 NC1224.2
038200     PERFORM DE-LETE.                                             NC1224.2
038300     GO TO IND-WRITE-GF-1.                                        NC1224.2
038400 IND-FAIL-GF-1.                                                   NC1224.2
038500     PERFORM FAIL.                                                NC1224.2
038600     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N.                      NC1224.2
038700     MOVE 83 TO CORRECT-N.                                        NC1224.2
038800 IND-WRITE-GF-1.                                                  NC1224.2
038900     MOVE "IND-TEST-GF-1" TO PAR-NAME.                            NC1224.2
039000     MOVE "TALLY FOR CHARACTERS" TO FEATURE.                      NC1224.2
039100     PERFORM PRINT-DETAIL.                                        NC1224.2
039200 IND-INIT-GF-2.                                                   NC1224.2
039300     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
039400     MOVE ZERO TO TABLE2.                                         NC1224.2
039500     SET INDEX1 TO 2.                                             NC1224.2
039600     SET INDEX2 TO 2.                                             NC1224.2
039700     MOVE ZERO TO WRK-DU-999 (INDEX2).                            NC1224.2
039800 IND-TEST-GF-2-0.                                                 NC1224.2
039900     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
040000        FOR ALL "A".                                              NC1224.2
040100 IND-TEST-GF-2-1.                                                 NC1224.2
040200     IF WRK-DU-999 (INDEX2) EQUAL TO 8                            NC1224.2
040300        PERFORM PASS                                              NC1224.2
040400        GO TO IND-WRITE-GF-2.                                     NC1224.2
040500     GO TO IND-FAIL-GF-2.                                         NC1224.2
040600 IND-DELETE-GF-2.                                                 NC1224.2
040700     PERFORM DE-LETE.                                             NC1224.2
040800     GO TO IND-WRITE-GF-2.                                        NC1224.2
040900 IND-FAIL-GF-2.                                                   NC1224.2
041000     PERFORM FAIL.                                                NC1224.2
041100     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N.                      NC1224.2
041200     MOVE 8 TO CORRECT-N.                                         NC1224.2
041300 IND-WRITE-GF-2.                                                  NC1224.2
041400     MOVE "IND-TEST-GF-2" TO PAR-NAME.                            NC1224.2
041500     MOVE "TALLY FOR LITERAL" TO FEATURE.                         NC1224.2
041600     PERFORM PRINT-DETAIL.                                        NC1224.2
041700 IND-INIT-GF-3.                                                   NC1224.2
041800     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
041900     MOVE ZERO TO TABLE2.                                         NC1224.2
042000     SET INDEX1 TO 3.                                             NC1224.2
042100     SET INDEX2 TO 3.                                             NC1224.2
042200     MOVE ZERO TO WRK-DU-999 (INDEX2).                            NC1224.2
042300 IND-TEST-GF-3-0.                                                 NC1224.2
042400     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
042500        FOR ALL SPACE.                                            NC1224.2
042600 IND-TEST-GF-3-1.                                                 NC1224.2
042700     IF WRK-DU-999 (INDEX2) EQUAL TO 17                           NC1224.2
042800        PERFORM PASS                                              NC1224.2
042900        GO TO IND-WRITE-GF-3.                                     NC1224.2
043000     GO TO IND-FAIL-GF-3.                                         NC1224.2
043100 IND-DELETE-GF-3.                                                 NC1224.2
043200     PERFORM DE-LETE.                                             NC1224.2
043300     GO TO IND-WRITE-GF-3.                                        NC1224.2
043400 IND-FAIL-GF-3.                                                   NC1224.2
043500     PERFORM FAIL.                                                NC1224.2
043600     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N.                      NC1224.2
043700     MOVE 17 TO CORRECT-N.                                        NC1224.2
043800 IND-WRITE-GF-3.                                                  NC1224.2
043900     MOVE "IND-TEST-GF-3" TO PAR-NAME.                            NC1224.2
044000     MOVE "TALLY FOR ALL SPACE" TO FEATURE.                       NC1224.2
044100     PERFORM PRINT-DETAIL.                                        NC1224.2
044200 IND-INIT-GF-4.                                                   NC1224.2
044300     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
044400     MOVE ZERO TO TABLE2.                                         NC1224.2
044500     SET INDEX1 TO 4.                                             NC1224.2
044600     SET INDEX2 TO 4.                                             NC1224.2
044700     MOVE ZERO TO WRK-DU-999 (INDEX2).                            NC1224.2
044800 IND-TEST-GF-4-0.                                                 NC1224.2
044900     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
045000        FOR LEADING "A".                                          NC1224.2
045100 IND-TEST-GF-4-1.                                                 NC1224.2
045200     IF WRK-DU-999 (INDEX2) EQUAL TO 1                            NC1224.2
045300        PERFORM PASS                                              NC1224.2
045400        GO TO IND-WRITE-GF-4.                                     NC1224.2
045500     GO TO IND-FAIL-GF-4.                                         NC1224.2
045600 IND-DELETE-GF-4.                                                 NC1224.2
045700     PERFORM DE-LETE.                                             NC1224.2
045800     GO TO IND-WRITE-GF-4.                                        NC1224.2
045900 IND-FAIL-GF-4.                                                   NC1224.2
046000     PERFORM FAIL.                                                NC1224.2
046100     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N.                      NC1224.2
046200     MOVE 1 TO CORRECT-N.                                         NC1224.2
046300 IND-WRITE-GF-4.                                                  NC1224.2
046400     MOVE "IND-TEST-GF-4" TO PAR-NAME.                            NC1224.2
046500     MOVE "TALLY LEADING LIT." TO FEATURE.                        NC1224.2
046600     PERFORM PRINT-DETAIL.                                        NC1224.2
046700 IND-INIT-GF-5.                                                   NC1224.2
046800     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
046900     SET INDEX1 TO 1.                                             NC1224.2
047000     SET INDEX2 TO 2.                                             NC1224.2
047100     MOVE ZERO TO TABLE2.                                         NC1224.2
047200     MOVE ZERO TO WRK-DU-999 (INDEX2 + 1).                        NC1224.2
047300 IND-TEST-GF-5-0.                                                 NC1224.2
047400     INSPECT TABLE1-REC (INDEX1 + 1) TALLYING                     NC1224.2
047500        WRK-DU-999 (INDEX2 + 1)                                   NC1224.2
047600        FOR CHARACTERS AFTER "W".                                 NC1224.2
047700 IND-TEST-GF-5-1.                                                 NC1224.2
047800     IF WRK-DU-999 (INDEX2 + 1) EQUAL TO 68                       NC1224.2
047900        PERFORM PASS                                              NC1224.2
048000        GO TO IND-WRITE-GF-5.                                     NC1224.2
048100     GO TO IND-FAIL-GF-5.                                         NC1224.2
048200 IND-DELETE-GF-5.                                                 NC1224.2
048300     PERFORM DE-LETE.                                             NC1224.2
048400     GO TO IND-WRITE-GF-5.                                        NC1224.2
048500 IND-FAIL-GF-5.                                                   NC1224.2
048600     PERFORM FAIL.                                                NC1224.2
048700     MOVE WRK-DU-999 (INDEX2 + 1) TO COMPUTED-N.                  NC1224.2
048800     MOVE 68 TO CORRECT-N.                                        NC1224.2
048900 IND-WRITE-GF-5.                                                  NC1224.2
049000     MOVE "IND-TEST-GF-5" TO PAR-NAME.                            NC1224.2
049100     MOVE "FOR CHARS AFTER LIT." TO FEATURE.                      NC1224.2
049200     PERFORM PRINT-DETAIL.                                        NC1224.2
049300 IND-INIT-GF-6.                                                   NC1224.2
049400     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
049500     MOVE ZERO TO TABLE2.                                         NC1224.2
049600     SET INDEX1 TO 3.                                             NC1224.2
049700     SET INDEX2 TO 4.                                             NC1224.2
049800     MOVE ZERO TO WRK-DU-999 (INDEX2 - 2).                        NC1224.2
049900 IND-TEST-GF-6-0.                                                 NC1224.2
050000     INSPECT TABLE1-REC (INDEX1 - 1)                              NC1224.2
050100        TALLYING WRK-DU-999 (INDEX2 - 2)                          NC1224.2
050200        FOR ALL " " BEFORE INITIAL "W".                           NC1224.2
050300 IND-TEST-GF-6-1.                                                 NC1224.2
050400     IF WRK-DU-999 (INDEX2 - 2) EQUAL TO 4                        NC1224.2
050500        PERFORM PASS                                              NC1224.2
050600        GO TO IND-WRITE-GF-6.                                     NC1224.2
050700     GO TO IND-FAIL-GF-6.                                         NC1224.2
050800 IND-DELETE-GF-6.                                                 NC1224.2
050900     PERFORM DE-LETE.                                             NC1224.2
051000     GO TO IND-WRITE-GF-6.                                        NC1224.2
051100 IND-FAIL-GF-6.                                                   NC1224.2
051200     PERFORM FAIL.                                                NC1224.2
051300     MOVE WRK-DU-999 (INDEX2 - 2) TO COMPUTED-N.                  NC1224.2
051400     MOVE 4 TO CORRECT-N.                                         NC1224.2
051500 IND-WRITE-GF-6.                                                  NC1224.2
051600     MOVE "IND-TEST-GF-6" TO PAR-NAME.                            NC1224.2
051700     MOVE "ALL BEFORE INITIAL" TO FEATURE.                        NC1224.2
051800     PERFORM PRINT-DETAIL.                                        NC1224.2
051900 IND-INIT-GF-7.                                                   NC1224.2
052000     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
052100     MOVE ZERO TO TABLE2.                                         NC1224.2
052200     SET INDEX1 TO 1.                                             NC1224.2
052300     SET INDEX2 TO 1.                                             NC1224.2
052400     MOVE ZERO TO WRK-DU-999 (INDEX2).                            NC1224.2
052500 IND-TEST-GF-7-0.                                                 NC1224.2
052600     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
052700        FOR LEADING "Y" AFTER INITIAL SPACE.                      NC1224.2
052800 IND-TEST-GF-7-1.                                                 NC1224.2
052900     IF WRK-DU-999 (INDEX2) EQUAL TO 1                            NC1224.2
053000        PERFORM PASS                                              NC1224.2
053100        GO TO IND-WRITE-GF-7.                                     NC1224.2
053200     GO TO IND-FAIL-GF-7.                                         NC1224.2
053300 IND-DELETE-GF-7.                                                 NC1224.2
053400     PERFORM DE-LETE.                                             NC1224.2
053500     GO TO IND-WRITE-GF-7.                                        NC1224.2
053600 IND-FAIL-GF-7.                                                   NC1224.2
053700     PERFORM FAIL.                                                NC1224.2
053800     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N.                      NC1224.2
053900     MOVE 1 TO CORRECT-N.                                         NC1224.2
054000 IND-WRITE-GF-7.                                                  NC1224.2
054100     MOVE "IND-TEST-GF-7" TO PAR-NAME.                            NC1224.2
054200     MOVE "LEAD. LIT. INITIAL" TO FEATURE.                        NC1224.2
054300     PERFORM PRINT-DETAIL.                                        NC1224.2
054400 IND-INIT-GF-8.                                                   NC1224.2
054500     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
054600     PERFORM BUILD-TABLE1.                                        NC1224.2
054700     SET INDEX1 TO 1.                                             NC1224.2
054800 IND-TEST-GF-8-0.                                                 NC1224.2
054900     INSPECT TABLE1-REC (INDEX1) REPLACING CHARACTERS BY SPACE.   NC1224.2
055000 IND-TEST-GF-8-1.                                                 NC1224.2
055100     IF TABLE1-REC (INDEX1) EQUAL TO SPACE                        NC1224.2
055200        PERFORM PASS                                              NC1224.2
055300        GO TO IND-WRITE-GF-8.                                     NC1224.2
055400     GO TO IND-FAIL-GF-8.                                         NC1224.2
055500 IND-DELETE-GF-8.                                                 NC1224.2
055600     PERFORM DE-LETE.                                             NC1224.2
055700     GO TO IND-WRITE-GF-8.                                        NC1224.2
055800 IND-FAIL-GF-8.                                                   NC1224.2
055900     PERFORM FAIL.                                                NC1224.2
056000     MOVE TABLE1-REC (INDEX1) TO COMPUTED-A.                      NC1224.2
056100     MOVE "SPACES" TO CORRECT-A.                                  NC1224.2
056200 IND-WRITE-GF-8.                                                  NC1224.2
056300     MOVE "IND-TEST-GF-8" TO PAR-NAME.                            NC1224.2
056400     MOVE "REP. CHARS BY SPACE" TO FEATURE.                       NC1224.2
056500     PERFORM PRINT-DETAIL.                                        NC1224.2
056600 IND-INIT-GF-9.                                                   NC1224.2
056700     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
056800     PERFORM BUILD-TABLE1.                                        NC1224.2
056900     SET INDEX1 TO 2.                                             NC1224.2
057000 IND-TEST-GF-9-0.                                                 NC1224.2
057100     INSPECT TABLE1-REC (INDEX1) REPLACING CHARACTERS             NC1224.2
057200        BY "O" BEFORE INITIAL "H".                                NC1224.2
057300 IND-TEST-GF-9-1.                                                 NC1224.2
057400     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-1                  NC1224.2
057500        PERFORM PASS                                              NC1224.2
057600        GO TO IND-WRITE-GF-9.                                     NC1224.2
057700     GO TO IND-FAIL-GF-9.                                         NC1224.2
057800 IND-DELETE-GF-9.                                                 NC1224.2
057900     PERFORM DE-LETE.                                             NC1224.2
058000     GO TO IND-WRITE-GF-9.                                        NC1224.2
058100 IND-FAIL-GF-9.                                                   NC1224.2
058200     PERFORM FAIL.                                                NC1224.2
058300     MOVE    TABLE1-REC (INDEX1) TO WRK-ER.                       NC1224.2
058400     MOVE    ANS-XN-83-1 TO WRK-OK.                               NC1224.2
058500     MOVE    WRK-OK-1-20 TO CORRECT-X.                            NC1224.2
058600     MOVE    WRK-ER-1-20 TO COMPUTED-X.                           NC1224.2
058700     PERFORM PRINT-DETAIL.                                        NC1224.2
058800     MOVE    WRK-OK-21-40 TO CORRECT-X.                           NC1224.2
058900     MOVE    WRK-ER-21-40 TO COMPUTED-X.                          NC1224.2
059000     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
059100     MOVE    WRK-OK-41-60 TO CORRECT-X.                           NC1224.2
059200     MOVE    WRK-ER-41-60 TO COMPUTED-X.                          NC1224.2
059300     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
059400     MOVE    WRK-OK-61-80 TO CORRECT-X.                           NC1224.2
059500     MOVE    WRK-ER-61-80 TO COMPUTED-X.                          NC1224.2
059600     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
059700     MOVE    WRK-OK-81-83 TO CORRECT-X.                           NC1224.2
059800     MOVE    WRK-ER-81-83 TO COMPUTED-X.                          NC1224.2
059900     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
060000 IND-WRITE-GF-9.                                                  NC1224.2
060100     MOVE "IND-TEST-GF-9" TO PAR-NAME.                            NC1224.2
060200     MOVE "CHARS BEFORE INITIAL" TO FEATURE.                      NC1224.2
060300     PERFORM PRINT-DETAIL.                                        NC1224.2
060400 IND-INIT-GF-10.                                                  NC1224.2
060500     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
060600     PERFORM BUILD-TABLE1.                                        NC1224.2
060700     SET INDEX1 TO 3.                                             NC1224.2
060800     SET INDEX3 TO 1.                                             NC1224.2
060900     SET INDEX4 TO 8.                                             NC1224.2
061000 IND-TEST-GF-10-0.                                                NC1224.2
061100     INSPECT TABLE1-REC (INDEX1) REPLACING LEADING                NC1224.2
061200        TABLE3-SYMBOL (INDEX3) BY TABLE3-SYMBOL (INDEX3 + 1)      NC1224.2
061300        AFTER INITIAL TABLE4-LETTER (INDEX4).                     NC1224.2
061400 IND-TEST-GF-10-1.                                                NC1224.2
061500     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-2                  NC1224.2
061600        PERFORM PASS                                              NC1224.2
061700        GO TO IND-WRITE-GF-10.                                    NC1224.2
061800     GO TO IND-FAIL-GF-10.                                        NC1224.2
061900 IND-DELETE-GF-10.                                                NC1224.2
062000     PERFORM DE-LETE.                                             NC1224.2
062100     GO TO IND-WRITE-GF-10.                                       NC1224.2
062200 IND-FAIL-GF-10.                                                  NC1224.2
062300     PERFORM FAIL.                                                NC1224.2
062400     MOVE    TABLE1-REC (INDEX1) TO WRK-ER.                       NC1224.2
062500     MOVE    ANS-XN-83-2 TO WRK-OK.                               NC1224.2
062600     MOVE    WRK-OK-1-20 TO CORRECT-X.                            NC1224.2
062700     MOVE    WRK-ER-1-20 TO COMPUTED-X.                           NC1224.2
062800     PERFORM PRINT-DETAIL.                                        NC1224.2
062900     MOVE    WRK-OK-21-40 TO CORRECT-X.                           NC1224.2
063000     MOVE    WRK-ER-21-40 TO COMPUTED-X.                          NC1224.2
063100     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
063200     MOVE    WRK-OK-41-60 TO CORRECT-X.                           NC1224.2
063300     MOVE    WRK-ER-41-60 TO COMPUTED-X.                          NC1224.2
063400     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
063500     MOVE    WRK-OK-61-80 TO CORRECT-X.                           NC1224.2
063600     MOVE    WRK-ER-61-80 TO COMPUTED-X.                          NC1224.2
063700     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
063800     MOVE    WRK-OK-81-83 TO CORRECT-X.                           NC1224.2
063900     MOVE    WRK-ER-81-83 TO COMPUTED-X.                          NC1224.2
064000     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
064100 IND-WRITE-GF-10.                                                 NC1224.2
064200     MOVE "IND-TEST-GF-10" TO PAR-NAME.                           NC1224.2
064300     MOVE "LEAD. AFTER INIT. ID" TO FEATURE.                      NC1224.2
064400     PERFORM PRINT-DETAIL.                                        NC1224.2
064500 IND-INIT-GF-11.                                                  NC1224.2
064600     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
064700     PERFORM BUILD-TABLE1.                                        NC1224.2
064800     SET INDEX1 TO 4.                                             NC1224.2
064900     SET INDEX4 TO 6.                                             NC1224.2
065000 IND-TEST-GF-11-0.                                                NC1224.2
065100     INSPECT TABLE1-REC (INDEX1) REPLACING FIRST "A"              NC1224.2
065200        BY TABLE4-LETTER (INDEX4) BEFORE INITIAL "H".             NC1224.2
065300 IND-TEST-GF-11-1.                                                NC1224.2
065400     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-1                  NC1224.2
065500        PERFORM PASS                                              NC1224.2
065600        GO TO IND-WRITE-GF-11.                                    NC1224.2
065700     GO TO IND-FAIL-GF-11.                                        NC1224.2
065800 IND-DELETE-GF-11.                                                NC1224.2
065900     PERFORM DE-LETE.                                             NC1224.2
066000     GO TO IND-WRITE-GF-11.                                       NC1224.2
066100 IND-FAIL-GF-11.                                                  NC1224.2
066200     PERFORM FAIL.                                                NC1224.2
066300     MOVE    TABLE1-REC (INDEX1) TO WRK-ER.                       NC1224.2
066400     MOVE    ANS-XN-83-1 TO WRK-OK.                               NC1224.2
066500     MOVE    WRK-OK-1-20 TO CORRECT-X.                            NC1224.2
066600     MOVE    WRK-ER-1-20 TO COMPUTED-X.                           NC1224.2
066700     PERFORM PRINT-DETAIL.                                        NC1224.2
066800     MOVE    WRK-OK-21-40 TO CORRECT-X.                           NC1224.2
066900     MOVE    WRK-ER-21-40 TO COMPUTED-X.                          NC1224.2
067000     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
067100     MOVE    WRK-OK-41-60 TO CORRECT-X.                           NC1224.2
067200     MOVE    WRK-ER-41-60 TO COMPUTED-X.                          NC1224.2
067300     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
067400     MOVE    WRK-OK-61-80 TO CORRECT-X.                           NC1224.2
067500     MOVE    WRK-ER-61-80 TO COMPUTED-X.                          NC1224.2
067600     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
067700     MOVE    WRK-OK-81-83 TO CORRECT-X.                           NC1224.2
067800     MOVE    WRK-ER-81-83 TO COMPUTED-X.                          NC1224.2
067900     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
068000 IND-WRITE-GF-11.                                                 NC1224.2
068100     MOVE "IND-TEST-GF-11" TO PAR-NAME.                           NC1224.2
068200     MOVE "FIRST BY ID BEFORE" TO FEATURE.                        NC1224.2
068300     PERFORM PRINT-DETAIL.                                        NC1224.2
068400 IND-INIT-GF-12.                                                  NC1224.2
068500     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
068600     PERFORM BUILD-TABLE1.                                        NC1224.2
068700     SET INDEX1 TO 1.                                             NC1224.2
068800     SET INDEX3 TO 1.                                             NC1224.2
068900     SET INDEX4 TO 5.                                             NC1224.2
069000 IND-TEST-GF-12-0.                                                NC1224.2
069100     INSPECT TABLE1-REC (INDEX1 + 1) REPLACING ALL                NC1224.2
069200      TABLE3-SYMBOL (INDEX3) BY "-" AFTER TABLE4-LETTER (INDEX4). NC1224.2
069300 IND-TEST-GF-12-1.                                                NC1224.2
069400     IF TABLE1-REC (INDEX1 + 1) EQUAL TO ANS-XN-83-3              NC1224.2
069500        PERFORM PASS                                              NC1224.2
069600        GO TO IND-WRITE-GF-12.                                    NC1224.2
069700     GO TO IND-FAIL-GF-12.                                        NC1224.2
069800 IND-DELETE-GF-12.                                                NC1224.2
069900     PERFORM DE-LETE.                                             NC1224.2
070000     GO TO IND-WRITE-GF-12.                                       NC1224.2
070100 IND-FAIL-GF-12.                                                  NC1224.2
070200     PERFORM FAIL.                                                NC1224.2
070300     MOVE    TABLE1-REC (INDEX1 + 1) TO WRK-ER.                   NC1224.2
070400     MOVE    ANS-XN-83-3 TO WRK-OK.                               NC1224.2
070500     MOVE    WRK-OK-1-20 TO CORRECT-X.                            NC1224.2
070600     MOVE    WRK-ER-1-20 TO COMPUTED-X.                           NC1224.2
070700     PERFORM PRINT-DETAIL.                                        NC1224.2
070800     MOVE    WRK-OK-21-40 TO CORRECT-X.                           NC1224.2
070900     MOVE    WRK-ER-21-40 TO COMPUTED-X.                          NC1224.2
071000     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
071100     MOVE    WRK-OK-41-60 TO CORRECT-X.                           NC1224.2
071200     MOVE    WRK-ER-41-60 TO COMPUTED-X.                          NC1224.2
071300     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
071400     MOVE    WRK-OK-61-80 TO CORRECT-X.                           NC1224.2
071500     MOVE    WRK-ER-61-80 TO COMPUTED-X.                          NC1224.2
071600     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
071700     MOVE    WRK-OK-81-83 TO CORRECT-X.                           NC1224.2
071800     MOVE    WRK-ER-81-83 TO COMPUTED-X.                          NC1224.2
071900     PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.             NC1224.2
072000 IND-WRITE-GF-12.                                                 NC1224.2
072100     MOVE "IND-TEST-GF-12" TO PAR-NAME.                           NC1224.2
072200     MOVE "ALL ID BY LIT. AFTER" TO FEATURE.                      NC1224.2
072300     PERFORM PRINT-DETAIL.                                        NC1224.2
072400 IND-INIT-GF-13.                                                  NC1224.2
072500     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
072600     MOVE "IND-TEST-GF-13" TO PAR-NAME.                           NC1224.2
072700     MOVE "TALLY REPLACE CHARS" TO FEATURE.                       NC1224.2
072800     MOVE 1 TO REC-CT.                                            NC1224.2
072900     PERFORM BUILD-TABLE1.                                        NC1224.2
073000     MOVE ZERO TO TABLE2.                                         NC1224.2
073100     SET INDEX1 TO 1.                                             NC1224.2
073200     SET INDEX2 TO 1.                                             NC1224.2
073300     MOVE 0 TO WRK-DU-999 (INDEX2).                               NC1224.2
073400 IND-TEST-GF-13-0.                                                NC1224.2
073500     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
073600        FOR CHARACTERS REPLACING CHARACTERS BY " ".               NC1224.2
073700     GO TO IND-TEST-GF-13-1.                                      NC1224.2
073800 IND-DELETE-GF-13.                                                NC1224.2
073900     PERFORM DE-LETE.                                             NC1224.2
074000     PERFORM PRINT-DETAIL.                                        NC1224.2
074100     GO TO IND-INIT-GF-14.                                        NC1224.2
074200 IND-TEST-GF-13-1.                                                NC1224.2
074300     IF WRK-DU-999 (INDEX2) EQUAL TO 83                           NC1224.2
074400        PERFORM PASS                                              NC1224.2
074500        PERFORM PRINT-DETAIL                                      NC1224.2
074600        ELSE PERFORM FAIL                                         NC1224.2
074700        MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N                    NC1224.2
074800        MOVE 83 TO CORRECT-N                                      NC1224.2
074900        PERFORM PRINT-DETAIL.                                     NC1224.2
075000     ADD 1 TO REC-CT.                                             NC1224.2
075100 IND-TEST-GF-13-2.                                                NC1224.2
075200     IF TABLE1-REC (INDEX1) EQUAL TO SPACE                        NC1224.2
075300        PERFORM PASS                                              NC1224.2
075400        PERFORM PRINT-DETAIL                                      NC1224.2
075500     ELSE                                                         NC1224.2
075600         PERFORM FAIL                                             NC1224.2
075700         MOVE    TABLE1-REC (INDEX1) TO WRK-ER                    NC1224.2
075800         MOVE    SPACES TO WRK-OK                                 NC1224.2
075900         MOVE    WRK-OK-1-20 TO CORRECT-X                         NC1224.2
076000         MOVE    WRK-ER-1-20 TO COMPUTED-X                        NC1224.2
076100         PERFORM PRINT-DETAIL                                     NC1224.2
076200         MOVE    WRK-OK-21-40 TO CORRECT-X                        NC1224.2
076300         MOVE    WRK-ER-21-40 TO COMPUTED-X                       NC1224.2
076400         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
076500         MOVE    WRK-OK-41-60 TO CORRECT-X                        NC1224.2
076600         MOVE    WRK-ER-41-60 TO COMPUTED-X                       NC1224.2
076700         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
076800         MOVE    WRK-OK-61-80 TO CORRECT-X                        NC1224.2
076900         MOVE    WRK-ER-61-80 TO COMPUTED-X                       NC1224.2
077000         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
077100         MOVE    WRK-OK-81-83 TO CORRECT-X                        NC1224.2
077200         MOVE    WRK-ER-81-83 TO COMPUTED-X                       NC1224.2
077300         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.         NC1224.2
077400 IND-INIT-GF-14.                                                  NC1224.2
077500     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
077600     MOVE "IND-TEST-GF-14" TO PAR-NAME.                           NC1224.2
077700     MOVE "LIT. BY BEFORE INIT." TO FEATURE.                      NC1224.2
077800     MOVE 1 TO REC-CT.                                            NC1224.2
077900     PERFORM BUILD-TABLE1.                                        NC1224.2
078000     MOVE ZERO TO TABLE2.                                         NC1224.2
078100     SET INDEX1 TO 4.                                             NC1224.2
078200     SET INDEX2 TO 2.                                             NC1224.2
078300     SET INDEX4 TO 5.                                             NC1224.2
078400 IND-TEST-GF-14-0.                                                NC1224.2
078500     INSPECT TABLE1-REC (INDEX1 - 2) TALLYING                     NC1224.2
078600        WRK-DU-999 (INDEX2 + 2) FOR CHARACTERS                    NC1224.2
078700        AFTER TABLE4-LETTER (INDEX4 - 1) REPLACING ALL            NC1224.2
078800        "A" BY "E" BEFORE INITIAL TABLE4-LETTER (INDEX4).         NC1224.2
078900     GO TO IND-TEST-GF-14-1.                                      NC1224.2
079000 IND-DELETE-GF-14.                                                NC1224.2
079100     PERFORM DE-LETE.                                             NC1224.2
079200     PERFORM PRINT-DETAIL.                                        NC1224.2
079300     GO TO IND-INIT-GF-15.                                        NC1224.2
079400 IND-TEST-GF-14-1.                                                NC1224.2
079500     IF WRK-DU-999 (INDEX2 + 2) EQUAL TO 81                       NC1224.2
079600        PERFORM PASS                                              NC1224.2
079700        PERFORM PRINT-DETAIL                                      NC1224.2
079800        ELSE PERFORM FAIL                                         NC1224.2
079900        MOVE WRK-DU-999 (INDEX2 + 2) TO COMPUTED-N                NC1224.2
080000        MOVE 6 TO CORRECT-N                                       NC1224.2
080100        PERFORM PRINT-DETAIL.                                     NC1224.2
080200     ADD 1 TO REC-CT.                                             NC1224.2
080300 IND-TEST-GF-14-2.                                                NC1224.2
080400     IF TABLE1-REC (INDEX1 - 2) EQUAL TO ANS-XN-83-4              NC1224.2
080500        PERFORM PASS                                              NC1224.2
080600        PERFORM PRINT-DETAIL                                      NC1224.2
080700     ELSE                                                         NC1224.2
080800        PERFORM FAIL                                              NC1224.2
080900         MOVE    TABLE1-REC (INDEX1 - 2) TO WRK-ER                NC1224.2
081000         MOVE    ANS-XN-83-4 TO WRK-OK                            NC1224.2
081100         MOVE    WRK-OK-1-20 TO CORRECT-X                         NC1224.2
081200         MOVE    WRK-ER-1-20 TO COMPUTED-X                        NC1224.2
081300         PERFORM PRINT-DETAIL                                     NC1224.2
081400         MOVE    WRK-OK-21-40 TO CORRECT-X                        NC1224.2
081500         MOVE    WRK-ER-21-40 TO COMPUTED-X                       NC1224.2
081600         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
081700         MOVE    WRK-OK-41-60 TO CORRECT-X                        NC1224.2
081800         MOVE    WRK-ER-41-60 TO COMPUTED-X                       NC1224.2
081900         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
082000         MOVE    WRK-OK-61-80 TO CORRECT-X                        NC1224.2
082100         MOVE    WRK-ER-61-80 TO COMPUTED-X                       NC1224.2
082200         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
082300         MOVE    WRK-OK-81-83 TO CORRECT-X                        NC1224.2
082400         MOVE    WRK-ER-81-83 TO COMPUTED-X                       NC1224.2
082500         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.         NC1224.2
082600 IND-INIT-GF-15.                                                  NC1224.2
082700     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
082800     MOVE "IND-TEST-GF-15" TO PAR-NAME.                           NC1224.2
082900     MOVE "REPL. FIRST AFTER" TO FEATURE.                         NC1224.2
083000     MOVE 1 TO REC-CT.                                            NC1224.2
083100     PERFORM BUILD-TABLE1.                                        NC1224.2
083200     MOVE ZERO TO TABLE2.                                         NC1224.2
083300     SET INDEX1 TO 1.                                             NC1224.2
083400     SET INDEX2 TO 1.                                             NC1224.2
083500     SET INDEX4 TO 4.                                             NC1224.2
083600     MOVE 0 TO WRK-DU-999 (INDEX2).                               NC1224.2
083700 IND-TEST-GF-15-0.                                                NC1224.2
083800     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
083900        FOR ALL "A" BEFORE TABLE4-LETTER (INDEX4 + 1)             NC1224.2
084000        REPLACING FIRST TABLE4-LETTER (INDEX4 - 3)                NC1224.2
084100        BY "O" AFTER INITIAL TABLE4-LETTER (INDEX4).              NC1224.2
084200     GO TO IND-TEST-GF-15-1.                                      NC1224.2
084300 IND-DELETE-GF-15.                                                NC1224.2
084400     PERFORM DE-LETE.                                             NC1224.2
084500     PERFORM PRINT-DETAIL.                                        NC1224.2
084600     GO TO IND-INIT-GF-16.                                        NC1224.2
084700 IND-TEST-GF-15-1.                                                NC1224.2
084800     IF WRK-DU-999 (INDEX2) EQUAL TO 7                            NC1224.2
084900        PERFORM PASS                                              NC1224.2
085000        PERFORM PRINT-DETAIL                                      NC1224.2
085100        ELSE PERFORM FAIL                                         NC1224.2
085200        MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N                    NC1224.2
085300        MOVE 7 TO CORRECT-N                                       NC1224.2
085400        PERFORM PRINT-DETAIL.                                     NC1224.2
085500     ADD 1 TO REC-CT.                                             NC1224.2
085600 IND-TEST-GF-15-2.                                                NC1224.2
085700     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-5                  NC1224.2
085800        PERFORM PASS                                              NC1224.2
085900        PERFORM PRINT-DETAIL                                      NC1224.2
086000        ELSE                                                      NC1224.2
086100         PERFORM FAIL                                             NC1224.2
086200         MOVE    TABLE1-REC (INDEX1) TO WRK-ER                    NC1224.2
086300         MOVE    ANS-XN-83-5 TO WRK-OK                            NC1224.2
086400         MOVE    WRK-OK-1-20 TO CORRECT-X                         NC1224.2
086500         MOVE    WRK-ER-1-20 TO COMPUTED-X                        NC1224.2
086600         PERFORM PRINT-DETAIL                                     NC1224.2
086700         MOVE    WRK-OK-21-40 TO CORRECT-X                        NC1224.2
086800         MOVE    WRK-ER-21-40 TO COMPUTED-X                       NC1224.2
086900         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
087000         MOVE    WRK-OK-41-60 TO CORRECT-X                        NC1224.2
087100         MOVE    WRK-ER-41-60 TO COMPUTED-X                       NC1224.2
087200         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
087300         MOVE    WRK-OK-61-80 TO CORRECT-X                        NC1224.2
087400         MOVE    WRK-ER-61-80 TO COMPUTED-X                       NC1224.2
087500         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
087600         MOVE    WRK-OK-81-83 TO CORRECT-X                        NC1224.2
087700         MOVE    WRK-ER-81-83 TO COMPUTED-X                       NC1224.2
087800         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.         NC1224.2
087900 IND-INIT-GF-16.                                                  NC1224.2
088000     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
088100     MOVE "IND-TEST-GF-16" TO PAR-NAME.                           NC1224.2
088200     MOVE "FOR LEADING" TO FEATURE.                               NC1224.2
088300     MOVE 1 TO REC-CT.                                            NC1224.2
088400     PERFORM BUILD-TABLE1.                                        NC1224.2
088500     MOVE ZERO TO TABLE2.                                         NC1224.2
088600     SET INDEX1 TO 2.                                             NC1224.2
088700     SET INDEX2 TO 2.                                             NC1224.2
088800     SET INDEX4 TO 1.                                             NC1224.2
088900     MOVE 0 TO WRK-DU-999 (INDEX2).                               NC1224.2
089000 IND-TEST-GF-16-0.                                                NC1224.2
089100     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
089200        FOR LEADING TABLE4-LETTER (INDEX4) REPLACING              NC1224.2
089300        LEADING TABLE4-LETTER (INDEX4) BY "O".                    NC1224.2
089400     GO TO IND-TEST-GF-16-1.                                      NC1224.2
089500 IND-DELETE-GF-16.                                                NC1224.2
089600     PERFORM DE-LETE.                                             NC1224.2
089700     PERFORM PRINT-DETAIL.                                        NC1224.2
089800     GO TO IND-INIT-GF-17.                                        NC1224.2
089900 IND-TEST-GF-16-1.                                                NC1224.2
090000     IF WRK-DU-999 (INDEX2) EQUAL TO 1                            NC1224.2
090100        PERFORM PASS                                              NC1224.2
090200        PERFORM PRINT-DETAIL                                      NC1224.2
090300        ELSE PERFORM FAIL                                         NC1224.2
090400        MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N                    NC1224.2
090500        MOVE 1 TO CORRECT-N                                       NC1224.2
090600        PERFORM PRINT-DETAIL.                                     NC1224.2
090700     ADD 1 TO REC-CT.                                             NC1224.2
090800 IND-TEST-GF-16-2.                                                NC1224.2
090900     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-1                  NC1224.2
091000        PERFORM PASS                                              NC1224.2
091100        PERFORM PRINT-DETAIL                                      NC1224.2
091200        ELSE                                                      NC1224.2
091300         PERFORM FAIL                                             NC1224.2
091400         MOVE    TABLE1-REC (INDEX1) TO WRK-ER                    NC1224.2
091500         MOVE    ANS-XN-83-1 TO WRK-OK                            NC1224.2
091600         MOVE    WRK-OK-1-20 TO CORRECT-X                         NC1224.2
091700         MOVE    WRK-ER-1-20 TO COMPUTED-X                        NC1224.2
091800         PERFORM PRINT-DETAIL                                     NC1224.2
091900         MOVE    WRK-OK-21-40 TO CORRECT-X                        NC1224.2
092000         MOVE    WRK-ER-21-40 TO COMPUTED-X                       NC1224.2
092100         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
092200         MOVE    WRK-OK-41-60 TO CORRECT-X                        NC1224.2
092300         MOVE    WRK-ER-41-60 TO COMPUTED-X                       NC1224.2
092400         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
092500         MOVE    WRK-OK-61-80 TO CORRECT-X                        NC1224.2
092600         MOVE    WRK-ER-61-80 TO COMPUTED-X                       NC1224.2
092700         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
092800         MOVE    WRK-OK-81-83 TO CORRECT-X                        NC1224.2
092900         MOVE    WRK-ER-81-83 TO COMPUTED-X                       NC1224.2
093000         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.         NC1224.2
093100 IND-INIT-GF-17.                                                  NC1224.2
093200     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
093300     MOVE "IND-TEST-GF-17" TO PAR-NAME.                           NC1224.2
093400     MOVE "LIT. BY AFTER INIT." TO FEATURE.                       NC1224.2
093500     MOVE 1 TO REC-CT.                                            NC1224.2
093600     PERFORM BUILD-TABLE1.                                        NC1224.2
093700     MOVE ZERO TO TABLE2.                                         NC1224.2
093800     SET INDEX1 TO 3.                                             NC1224.2
093900     SET INDEX2 TO 3.                                             NC1224.2
094000     SET INDEX4 TO 7.                                             NC1224.2
094100     MOVE 0 TO WRK-DU-999 (INDEX2).                               NC1224.2
094200 IND-TEST-GF-17-0.                                                NC1224.2
094300     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
094400        FOR ALL "A" REPLACING FIRST "A" BY "O"                    NC1224.2
094500        AFTER INITIAL TABLE4-LETTER (INDEX4).                     NC1224.2
094600     GO TO IND-TEST-GF-17-1.                                      NC1224.2
094700 IND-DELETE-GF-17.                                                NC1224.2
094800     PERFORM DE-LETE.                                             NC1224.2
094900     PERFORM PRINT-DETAIL.                                        NC1224.2
095000     GO TO IND-INIT-GF-18.                                        NC1224.2
095100 IND-TEST-GF-17-1.                                                NC1224.2
095200     IF WRK-DU-999 (INDEX2) EQUAL TO 8                            NC1224.2
095300        PERFORM PASS                                              NC1224.2
095400        PERFORM PRINT-DETAIL                                      NC1224.2
095500        ELSE PERFORM FAIL                                         NC1224.2
095600        MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N                    NC1224.2
095700        MOVE 8 TO CORRECT-N                                       NC1224.2
095800        PERFORM PRINT-DETAIL.                                     NC1224.2
095900     ADD 1 TO REC-CT.                                             NC1224.2
096000 IND-TEST-GF-17-2.                                                NC1224.2
096100     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-5                  NC1224.2
096200        PERFORM PASS                                              NC1224.2
096300        PERFORM PRINT-DETAIL                                      NC1224.2
096400        ELSE                                                      NC1224.2
096500         PERFORM FAIL                                             NC1224.2
096600         MOVE    TABLE1-REC (INDEX1) TO WRK-ER                    NC1224.2
096700         MOVE    ANS-XN-83-1 TO WRK-OK                            NC1224.2
096800         MOVE    WRK-OK-1-20 TO CORRECT-X                         NC1224.2
096900         MOVE    WRK-ER-1-20 TO COMPUTED-X                        NC1224.2
097000         PERFORM PRINT-DETAIL                                     NC1224.2
097100         MOVE    WRK-OK-21-40 TO CORRECT-X                        NC1224.2
097200         MOVE    WRK-ER-21-40 TO COMPUTED-X                       NC1224.2
097300         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
097400         MOVE    WRK-OK-41-60 TO CORRECT-X                        NC1224.2
097500         MOVE    WRK-ER-41-60 TO COMPUTED-X                       NC1224.2
097600         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
097700         MOVE    WRK-OK-61-80 TO CORRECT-X                        NC1224.2
097800         MOVE    WRK-ER-61-80 TO COMPUTED-X                       NC1224.2
097900         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
098000         MOVE    WRK-OK-81-83 TO CORRECT-X                        NC1224.2
098100         MOVE    WRK-ER-81-83 TO COMPUTED-X                       NC1224.2
098200         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.         NC1224.2
098300 IND-INIT-GF-18.                                                  NC1224.2
098400     MOVE "IV-21 4.3.8.2.3/4" TO ANSI-REFERENCE.                  NC1224.2
098500     MOVE "IND-TEST-GF-18" TO PAR-NAME.                           NC1224.2
098600     MOVE "CHARS AFTER ALL BEF." TO FEATURE.                      NC1224.2
098700     MOVE 1 TO REC-CT.                                            NC1224.2
098800     PERFORM BUILD-TABLE1.                                        NC1224.2
098900     MOVE ZERO TO TABLE2.                                         NC1224.2
099000     SET INDEX1 TO 4.                                             NC1224.2
099100     SET INDEX2 TO 4.                                             NC1224.2
099200     SET INDEX4 TO 1.                                             NC1224.2
099300     MOVE 0 TO WRK-DU-999 (INDEX2).                               NC1224.2
099400 IND-TEST-GF-18-0.                                                NC1224.2
099500     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC1224.2
099600        FOR CHARACTERS AFTER TABLE4-LETTER (INDEX4)               NC1224.2
099700        REPLACING ALL "A" BY "O"                                  NC1224.2
099800        BEFORE TABLE4-LETTER (INDEX4 + 3).                        NC1224.2
099900     GO TO IND-TEST-GF-18-1.                                      NC1224.2
100000 IND-DELETE-GF-18.                                                NC1224.2
100100     PERFORM DE-LETE.                                             NC1224.2
100200     PERFORM PRINT-DETAIL.                                        NC1224.2
100300     GO TO CCVS-999999.                                           NC1224.2
100400 IND-TEST-GF-18-1.                                                NC1224.2
100500     IF WRK-DU-999 (INDEX2) EQUAL TO 82                           NC1224.2
100600        PERFORM PASS                                              NC1224.2
100700        PERFORM PRINT-DETAIL                                      NC1224.2
100800        ELSE PERFORM FAIL                                         NC1224.2
100900        MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N                    NC1224.2
101000        MOVE 82 TO CORRECT-N                                      NC1224.2
101100        PERFORM PRINT-DETAIL.                                     NC1224.2
101200     ADD 1 TO REC-CT.                                             NC1224.2
101300 IND-TEST-GF-18-2.                                                NC1224.2
101400     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-1                  NC1224.2
101500        PERFORM PASS                                              NC1224.2
101600        PERFORM PRINT-DETAIL                                      NC1224.2
101700        ELSE                                                      NC1224.2
101800         PERFORM FAIL                                             NC1224.2
101900         MOVE    TABLE1-REC (INDEX1) TO WRK-ER                    NC1224.2
102000         MOVE    ANS-XN-83-1 TO WRK-OK                            NC1224.2
102100         MOVE    WRK-OK-1-20 TO CORRECT-X                         NC1224.2
102200         MOVE    WRK-ER-1-20 TO COMPUTED-X                        NC1224.2
102300         PERFORM PRINT-DETAIL                                     NC1224.2
102400         MOVE    WRK-OK-21-40 TO CORRECT-X                        NC1224.2
102500         MOVE    WRK-ER-21-40 TO COMPUTED-X                       NC1224.2
102600         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
102700         MOVE    WRK-OK-41-60 TO CORRECT-X                        NC1224.2
102800         MOVE    WRK-ER-41-60 TO COMPUTED-X                       NC1224.2
102900         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
103000         MOVE    WRK-OK-61-80 TO CORRECT-X                        NC1224.2
103100         MOVE    WRK-ER-61-80 TO COMPUTED-X                       NC1224.2
103200         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX          NC1224.2
103300         MOVE    WRK-OK-81-83 TO CORRECT-X                        NC1224.2
103400         MOVE    WRK-ER-81-83 TO COMPUTED-X                       NC1224.2
103500         PERFORM FAIL-ROUTINE-WRITE THRU FAIL-ROUTINE-EX.         NC1224.2
103600 CCVS-EXIT SECTION.                                               NC1224.2
103700 CCVS-999999.                                                     NC1224.2
103800     GO TO CLOSE-FILES.                                           NC1224.2
      *END-OF,NC122A                                                                  
