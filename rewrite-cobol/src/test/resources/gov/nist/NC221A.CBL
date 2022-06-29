      *HEADER,COBOL,NC221A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2214.2
000200 PROGRAM-ID.                                                      NC2214.2
000300     NC221A.                                                      NC2214.2
000400****************************************************************  NC2214.2
000500*                                                              *  NC2214.2
000600*    VALIDATION FOR:-                                          *  NC2214.2
000700*                                                              *  NC2214.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2214.2
000900*                                                              *  NC2214.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2214.2
001100*                                                              *  NC2214.2
001200****************************************************************  NC2214.2
001300*                                                              *  NC2214.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2214.2
001500*                                                              *  NC2214.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2214.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2214.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2214.2
001900*                                                              *  NC2214.2
002000****************************************************************  NC2214.2
002100*                                                              *  NC2214.2
002200*    PROGRAM NC221A TEST THE USE OF INDEXED IDENTIFIERS WITH   *  NC2214.2
002300*    FORMATS 1, 2 AND 3 OF THE "INSPECT" STATEMENT.            *  NC2214.2
002400*                                                              *  NC2214.2
002500****************************************************************  NC2214.2
002600 ENVIRONMENT DIVISION.                                            NC2214.2
002700 CONFIGURATION SECTION.                                           NC2214.2
002800 SOURCE-COMPUTER.                                                 NC2214.2
002900     XXXXX082.                                                    NC2214.2
003000 OBJECT-COMPUTER.                                                 NC2214.2
003100     XXXXX083.                                                    NC2214.2
003200 INPUT-OUTPUT SECTION.                                            NC2214.2
003300 FILE-CONTROL.                                                    NC2214.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC2214.2
003500     XXXXX055.                                                    NC2214.2
003600 DATA DIVISION.                                                   NC2214.2
003700 FILE SECTION.                                                    NC2214.2
003800 FD  PRINT-FILE.                                                  NC2214.2
003900 01  PRINT-REC PICTURE X(120).                                    NC2214.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC2214.2
004100 WORKING-STORAGE SECTION.                                         NC2214.2
004200                                                                  NC2214.2
004300 01  WS-RIGHT-1-83.                                               NC2214.2
004400   03  WS-RIGHT-1-20                PIC X(20).                    NC2214.2
004500   03  WS-RIGHT-21-40               PIC X(20).                    NC2214.2
004600   03  WS-RIGHT-41-60               PIC X(20).                    NC2214.2
004700   03  WS-RIGHT-61-80               PIC X(20).                    NC2214.2
004800   03  WS-RIGHT-81-83               PIC X(3).                     NC2214.2
004900 01  WS-WRONG-1-83.                                               NC2214.2
005000   03  WS-WRONG-1-20                PIC X(20).                    NC2214.2
005100   03  WS-WRONG-21-40               PIC X(20).                    NC2214.2
005200   03  WS-WRONG-41-60               PIC X(20).                    NC2214.2
005300   03  WS-WRONG-61-80               PIC X(20).                    NC2214.2
005400   03  WS-WRONG-81-83               PIC X(3).                     NC2214.2
005500                                                                  NC2214.2
005600 01  TABLE1.                                                      NC2214.2
005700     02  TABLE1-REC              PICTURE X(83)                    NC2214.2
005800                                 OCCURS 4 TIMES                   NC2214.2
005900                                 INDEXED BY INDEX1.               NC2214.2
006000 01  TABLE2.                                                      NC2214.2
006100     02  WRK-DU-999              PICTURE 999                      NC2214.2
006200                                 OCCURS 4 TIMES                   NC2214.2
006300                                 INDEXED BY INDEX2.               NC2214.2
006400 01  TABLE3.                                                      NC2214.2
006500     02  TABLE3-SYMBOL           PICTURE X                        NC2214.2
006600                                 OCCURS 3 TIMES                   NC2214.2
006700                                 INDEXED BY INDEX3.               NC2214.2
006800 01  TABLE4.                                                      NC2214.2
006900     02  TABLE4-LETTER           PICTURE XX                       NC2214.2
007000                                 OCCURS 5 TIMES                   NC2214.2
007100                                 INDEXED BY INDEX4.               NC2214.2
007200 01  WC-XN-83  PIC X(83) VALUE                                    NC2214.2
007300     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
007400-    "IDS CAN NOT BE ALL BAD.".                                   NC2214.2
007500 01  ANS-XN-83-1 PIC X(83) VALUE                                  NC2214.2
007600     "OH YES AH YES W.C. FROTOES HERE, ANYONE WHO HATES DOGS AND KNC2214.2
007700-    "IDS CAN NOT BE ALL BAD.".                                   NC2214.2
007800 01  ANS-XN-83-2 PIC X(83) VALUE                                  NC2214.2
007900     "AH YES,AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
008000-    "IDS CAN NOT BE ALL BAD.".                                   NC2214.2
008100 01  ANS-XN-83-3 PIC X(83) VALUE                                  NC2214.2
008200     "OH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
008300-    "IDS CAN NOT BE ALL BAD.".                                   NC2214.2
008400 01  ANS-XN-83-4 PIC X(83) VALUE                                  NC2214.2
008500     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
008600-    "IDS CAN NOT BE ALL-BAD.".                                   NC2214.2
008700 01  ANS-XN-83-5 PIC X(83) VALUE                                  NC2214.2
008800     "EH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
008900-    "IDS CAN NOT BE ALL BAD.".                                   NC2214.2
009000 01  ANS-XN-83-6 PIC X(83) VALUE                                  NC2214.2
009100     "AH YES OH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
009200-    "IDS CAN NOT BE ALL BAD.".                                   NC2214.2
009300*01  TEST-RESULTS.                                                NC2214.2
009400*    02 FILLER                   PIC X      VALUE SPACE.          NC2214.2
009500*    02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2214.2
009600*    02 FILLER                   PIC X      VALUE SPACE.          NC2214.2
009700*    02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2214.2
009800*    02 FILLER                   PIC X      VALUE SPACE.          NC2214.2
009900*    02  PAR-NAME.                                                NC2214.2
010000*      03 FILLER                 PIC X(19)  VALUE SPACE.          NC2214.2
010100*      03  PARDOT-X              PIC X      VALUE SPACE.          NC2214.2
010200*      03 DOTVALUE               PIC 99     VALUE ZERO.           NC2214.2
010300*    02 FILLER                   PIC X(8)   VALUE SPACE.          NC2214.2
010400*    02 RE-MARK                  PIC X(61).                       NC2214.2
010500 01  TEST-RESULTS.                                                NC2214.2
010600     02 FILLER                   PIC X      VALUE SPACE.          NC2214.2
010700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2214.2
010800     02 FILLER                   PIC X      VALUE SPACE.          NC2214.2
010900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2214.2
011000     02 FILLER                   PIC X      VALUE SPACE.          NC2214.2
011100     02  PAR-NAME.                                                NC2214.2
011200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2214.2
011300       03  PARDOT-X              PIC X      VALUE SPACE.          NC2214.2
011400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2214.2
011500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2214.2
011600     02 RE-MARK                  PIC X(61).                       NC2214.2
011700 01  TEST-COMPUTED.                                               NC2214.2
011800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2214.2
011900     02 FILLER                   PIC X(17)  VALUE                 NC2214.2
012000            "       COMPUTED=".                                   NC2214.2
012100     02 COMPUTED-X.                                               NC2214.2
012200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2214.2
012300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2214.2
012400                                 PIC -9(9).9(9).                  NC2214.2
012500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2214.2
012600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2214.2
012700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2214.2
012800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2214.2
012900         04 COMPUTED-18V0                    PIC -9(18).          NC2214.2
013000         04 FILLER                           PIC X.               NC2214.2
013100     03 FILLER PIC X(50) VALUE SPACE.                             NC2214.2
013200 01  TEST-CORRECT.                                                NC2214.2
013300     02 FILLER PIC X(30) VALUE SPACE.                             NC2214.2
013400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2214.2
013500     02 CORRECT-X.                                                NC2214.2
013600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2214.2
013700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2214.2
013800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2214.2
013900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2214.2
014000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2214.2
014100     03      CR-18V0 REDEFINES CORRECT-A.                         NC2214.2
014200         04 CORRECT-18V0                     PIC -9(18).          NC2214.2
014300         04 FILLER                           PIC X.               NC2214.2
014400     03 FILLER PIC X(2) VALUE SPACE.                              NC2214.2
014500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2214.2
014600 01  CCVS-C-1.                                                    NC2214.2
014700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2214.2
014800-    "SS  PARAGRAPH-NAME                                          NC2214.2
014900-    "       REMARKS".                                            NC2214.2
015000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2214.2
015100 01  CCVS-C-2.                                                    NC2214.2
015200     02 FILLER                     PIC X        VALUE SPACE.      NC2214.2
015300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2214.2
015400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2214.2
015500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2214.2
015600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2214.2
015700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2214.2
015800 01  REC-CT                        PIC 99       VALUE ZERO.       NC2214.2
015900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2214.2
016000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2214.2
016100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2214.2
016200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2214.2
016300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2214.2
016400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2214.2
016500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2214.2
016600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2214.2
016700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2214.2
016800 01  CCVS-H-1.                                                    NC2214.2
016900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2214.2
017000     02  FILLER                    PIC X(42)    VALUE             NC2214.2
017100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2214.2
017200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2214.2
017300 01  CCVS-H-2A.                                                   NC2214.2
017400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2214.2
017500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2214.2
017600   02  FILLER                        PIC XXXX   VALUE             NC2214.2
017700     "4.2 ".                                                      NC2214.2
017800   02  FILLER                        PIC X(28)  VALUE             NC2214.2
017900            " COPY - NOT FOR DISTRIBUTION".                       NC2214.2
018000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2214.2
018100                                                                  NC2214.2
018200 01  CCVS-H-2B.                                                   NC2214.2
018300   02  FILLER                        PIC X(15)  VALUE             NC2214.2
018400            "TEST RESULT OF ".                                    NC2214.2
018500   02  TEST-ID                       PIC X(9).                    NC2214.2
018600   02  FILLER                        PIC X(4)   VALUE             NC2214.2
018700            " IN ".                                               NC2214.2
018800   02  FILLER                        PIC X(12)  VALUE             NC2214.2
018900     " HIGH       ".                                              NC2214.2
019000   02  FILLER                        PIC X(22)  VALUE             NC2214.2
019100            " LEVEL VALIDATION FOR ".                             NC2214.2
019200   02  FILLER                        PIC X(58)  VALUE             NC2214.2
019300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2214.2
019400 01  CCVS-H-3.                                                    NC2214.2
019500     02  FILLER                      PIC X(34)  VALUE             NC2214.2
019600            " FOR OFFICIAL USE ONLY    ".                         NC2214.2
019700     02  FILLER                      PIC X(58)  VALUE             NC2214.2
019800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2214.2
019900     02  FILLER                      PIC X(28)  VALUE             NC2214.2
020000            "  COPYRIGHT   1985 ".                                NC2214.2
020100 01  CCVS-E-1.                                                    NC2214.2
020200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2214.2
020300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2214.2
020400     02 ID-AGAIN                     PIC X(9).                    NC2214.2
020500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2214.2
020600 01  CCVS-E-2.                                                    NC2214.2
020700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2214.2
020800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2214.2
020900     02 CCVS-E-2-2.                                               NC2214.2
021000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2214.2
021100         03 FILLER                   PIC X      VALUE SPACE.      NC2214.2
021200         03 ENDER-DESC               PIC X(44)  VALUE             NC2214.2
021300            "ERRORS ENCOUNTERED".                                 NC2214.2
021400 01  CCVS-E-3.                                                    NC2214.2
021500     02  FILLER                      PIC X(22)  VALUE             NC2214.2
021600            " FOR OFFICIAL USE ONLY".                             NC2214.2
021700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2214.2
021800     02  FILLER                      PIC X(58)  VALUE             NC2214.2
021900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2214.2
022000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2214.2
022100     02 FILLER                       PIC X(15)  VALUE             NC2214.2
022200             " COPYRIGHT 1985".                                   NC2214.2
022300 01  CCVS-E-4.                                                    NC2214.2
022400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2214.2
022500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2214.2
022600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2214.2
022700     02 FILLER                       PIC X(40)  VALUE             NC2214.2
022800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2214.2
022900 01  XXINFO.                                                      NC2214.2
023000     02 FILLER                       PIC X(19)  VALUE             NC2214.2
023100            "*** INFORMATION ***".                                NC2214.2
023200     02 INFO-TEXT.                                                NC2214.2
023300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2214.2
023400       04 XXCOMPUTED                 PIC X(20).                   NC2214.2
023500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2214.2
023600       04 XXCORRECT                  PIC X(20).                   NC2214.2
023700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2214.2
023800 01  HYPHEN-LINE.                                                 NC2214.2
023900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2214.2
024000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2214.2
024100-    "*****************************************".                 NC2214.2
024200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2214.2
024300-    "******************************".                            NC2214.2
024400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2214.2
024500     "NC221A".                                                    NC2214.2
024600 PROCEDURE DIVISION.                                              NC2214.2
024700 CCVS1 SECTION.                                                   NC2214.2
024800 OPEN-FILES.                                                      NC2214.2
024900     OPEN     OUTPUT PRINT-FILE.                                  NC2214.2
025000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2214.2
025100     MOVE    SPACE TO TEST-RESULTS.                               NC2214.2
025200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2214.2
025300     GO TO CCVS1-EXIT.                                            NC2214.2
025400 CLOSE-FILES.                                                     NC2214.2
025500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2214.2
025600 TERMINATE-CCVS.                                                  NC2214.2
025700*    EXIT PROGRAM.                                                NC2214.2
025800*TERMINATE-CALL.                                                  NC2214.2
025900     STOP     RUN.                                                NC2214.2
026000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2214.2
026100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2214.2
026200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2214.2
026300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2214.2
026400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2214.2
026500 PRINT-DETAIL.                                                    NC2214.2
026600     IF REC-CT NOT EQUAL TO ZERO                                  NC2214.2
026700             MOVE "." TO PARDOT-X                                 NC2214.2
026800             MOVE REC-CT TO DOTVALUE.                             NC2214.2
026900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2214.2
027000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2214.2
027100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2214.2
027200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2214.2
027300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2214.2
027400     MOVE SPACE TO CORRECT-X.                                     NC2214.2
027500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2214.2
027600     MOVE     SPACE TO RE-MARK.                                   NC2214.2
027700 HEAD-ROUTINE.                                                    NC2214.2
027800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2214.2
027900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2214.2
028000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2214.2
028100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2214.2
028200 COLUMN-NAMES-ROUTINE.                                            NC2214.2
028300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2214.2
028400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2214.2
028500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2214.2
028600 END-ROUTINE.                                                     NC2214.2
028700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2214.2
028800 END-RTN-EXIT.                                                    NC2214.2
028900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2214.2
029000 END-ROUTINE-1.                                                   NC2214.2
029100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2214.2
029200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2214.2
029300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2214.2
029400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2214.2
029500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2214.2
029600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2214.2
029700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2214.2
029800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2214.2
029900  END-ROUTINE-12.                                                 NC2214.2
030000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2214.2
030100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2214.2
030200         MOVE "NO " TO ERROR-TOTAL                                NC2214.2
030300         ELSE                                                     NC2214.2
030400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2214.2
030500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2214.2
030600     PERFORM WRITE-LINE.                                          NC2214.2
030700 END-ROUTINE-13.                                                  NC2214.2
030800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2214.2
030900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2214.2
031000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2214.2
031100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2214.2
031200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2214.2
031300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2214.2
031400          MOVE "NO " TO ERROR-TOTAL                               NC2214.2
031500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2214.2
031600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2214.2
031700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2214.2
031800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2214.2
031900 WRITE-LINE.                                                      NC2214.2
032000     ADD 1 TO RECORD-COUNT.                                       NC2214.2
032100*    IF RECORD-COUNT GREATER 50                                   NC2214.2
032200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2214.2
032300*        MOVE SPACE TO DUMMY-RECORD                               NC2214.2
032400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2214.2
032500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2214.2
032600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2214.2
032700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2214.2
032800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2214.2
032900*        MOVE ZERO TO RECORD-COUNT.                               NC2214.2
033000     PERFORM WRT-LN.                                              NC2214.2
033100 WRT-LN.                                                          NC2214.2
033200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2214.2
033300     MOVE SPACE TO DUMMY-RECORD.                                  NC2214.2
033400 BLANK-LINE-PRINT.                                                NC2214.2
033500     PERFORM WRT-LN.                                              NC2214.2
033600 FAIL-ROUTINE.                                                    NC2214.2
033700     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2214.2
033800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2214.2
033900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2214.2
034000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2214.2
034100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2214.2
034200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2214.2
034300     GO TO  FAIL-ROUTINE-EX.                                      NC2214.2
034400 FAIL-ROUTINE-WRITE.                                              NC2214.2
034500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2214.2
034600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2214.2
034700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2214.2
034800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2214.2
034900 FAIL-ROUTINE-EX. EXIT.                                           NC2214.2
035000 BAIL-OUT.                                                        NC2214.2
035100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2214.2
035200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2214.2
035300 BAIL-OUT-WRITE.                                                  NC2214.2
035400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2214.2
035500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2214.2
035600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2214.2
035700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2214.2
035800 BAIL-OUT-EX. EXIT.                                               NC2214.2
035900 CCVS1-EXIT.                                                      NC2214.2
036000     EXIT.                                                        NC2214.2
036100 INIT-TABLE1.                                                     NC2214.2
036200     MOVE                                                         NC2214.2
036300     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
036400-    "IDS CAN NOT BE ALL BAD."                                    NC2214.2
036500         TO WC-XN-83.                                             NC2214.2
036600     MOVE WC-XN-83 TO TABLE1-REC (1).                             NC2214.2
036700     MOVE WC-XN-83 TO TABLE1-REC (2).                             NC2214.2
036800     MOVE WC-XN-83 TO TABLE1-REC (3).                             NC2214.2
036900     MOVE WC-XN-83 TO TABLE1-REC (4).                             NC2214.2
037000 INIT-TABLE3.                                                     NC2214.2
037100     MOVE " " TO TABLE3-SYMBOL (1).                               NC2214.2
037200     MOVE "," TO TABLE3-SYMBOL (2).                               NC2214.2
037300     MOVE "-" TO TABLE3-SYMBOL (3).                               NC2214.2
037400 INIT-TABLE4.                                                     NC2214.2
037500     MOVE "AH" TO TABLE4-LETTER (1).                              NC2214.2
037600     MOVE "OH" TO TABLE4-LETTER (2).                              NC2214.2
037700     MOVE "HE" TO TABLE4-LETTER (3).                              NC2214.2
037800     MOVE "LL" TO TABLE4-LETTER (4).                              NC2214.2
037900     MOVE "H " TO TABLE4-LETTER (5).                              NC2214.2
038000*                                                                 NC2214.2
038100 INS-INIT-F1-1.                                                   NC2214.2
038200     MOVE "INS-TEST-F1-1"     TO PAR-NAME.                        NC2214.2
038300     MOVE "IV-21 4.3.8.2"     TO ANSI-REFERENCE.                  NC2214.2
038400     MOVE "TALLY FOR LEADING" TO FEATURE.                         NC2214.2
038500     MOVE ZEROS TO TABLE2.                                        NC2214.2
038600     SET INDEX1 TO 1.                                             NC2214.2
038700     SET INDEX2 TO 1.                                             NC2214.2
038800 INS-TEST-F1-1.                                                   NC2214.2
038900     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC2214.2
039000        FOR LEADING "AH"                                          NC2214.2
039100     IF WRK-DU-999 (INDEX2) EQUAL TO 1                            NC2214.2
039200        PERFORM PASS                                              NC2214.2
039300        GO TO INS-WRITE-F1-1.                                     NC2214.2
039400     GO TO INS-FAIL-F1-1.                                         NC2214.2
039500 INS-DELETE-F1-1.                                                 NC2214.2
039600     PERFORM DE-LETE.                                             NC2214.2
039700     GO TO INS-WRITE-F1-1.                                        NC2214.2
039800 INS-FAIL-F1-1.                                                   NC2214.2
039900     PERFORM FAIL.                                                NC2214.2
040000     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N.                      NC2214.2
040100     MOVE 1 TO CORRECT-N.                                         NC2214.2
040200 INS-WRITE-F1-1.                                                  NC2214.2
040300     PERFORM PRINT-DETAIL.                                        NC2214.2
040400*                                                                 NC2214.2
040500 INS-INIT-F1-2.                                                   NC2214.2
040600     MOVE "INS-TEST-F1-2"        TO PAR-NAME.                     NC2214.2
040700     MOVE "IV-21 4.3.8.2"        TO ANSI-REFERENCE.               NC2214.2
040800     MOVE "TALLY FOR CHAR AFTER" TO FEATURE.                      NC2214.2
040900     MOVE ZEROS TO TABLE2.                                        NC2214.2
041000     SET INDEX1 TO 2.                                             NC2214.2
041100     SET INDEX2 TO 2.                                             NC2214.2
041200 INS-TEST-F1-2.                                                   NC2214.2
041300     INSPECT TABLE1-REC (INDEX1 + 1)                              NC2214.2
041400        TALLYING WRK-DU-999 (INDEX2 + 1)                          NC2214.2
041500        FOR CHARACTERS AFTER " W".                                NC2214.2
041600     IF WRK-DU-999 (INDEX2 + 1) EQUAL TO 68                       NC2214.2
041700        PERFORM PASS                                              NC2214.2
041800        GO TO INS-WRITE-F1-2.                                     NC2214.2
041900     GO TO INS-FAIL-F1-2.                                         NC2214.2
042000 INS-DELETE-F1-2.                                                 NC2214.2
042100     PERFORM DE-LETE.                                             NC2214.2
042200     GO TO INS-WRITE-F1-2.                                        NC2214.2
042300 INS-FAIL-F1-2.                                                   NC2214.2
042400     PERFORM FAIL.                                                NC2214.2
042500     MOVE WRK-DU-999 (INDEX2 + 1) TO COMPUTED-N.                  NC2214.2
042600     MOVE 68 TO CORRECT-N.                                        NC2214.2
042700 INS-WRITE-F1-2.                                                  NC2214.2
042800     PERFORM PRINT-DETAIL.                                        NC2214.2
042900*                                                                 NC2214.2
043000 INS-INIT-F1-3.                                                   NC2214.2
043100     MOVE "INS-TEST-F1-3" TO PAR-NAME.                            NC2214.2
043200     MOVE "IV-21 4.3.8.2"        TO ANSI-REFERENCE.               NC2214.2
043300     MOVE "TALLY FOR ALL BEFORE" TO FEATURE.                      NC2214.2
043400     MOVE ZEROS TO TABLE2.                                        NC2214.2
043500     SET INDEX1 TO 3.                                             NC2214.2
043600     SET INDEX2 TO 3.                                             NC2214.2
043700 INS-TEST-F1-3.                                                   NC2214.2
043800     INSPECT TABLE1-REC (INDEX1 - 1)                              NC2214.2
043900        TALLYING WRK-DU-999 (INDEX2 - 2)                          NC2214.2
044000        FOR ALL " " BEFORE INITIAL "W.C.".                        NC2214.2
044100     IF WRK-DU-999 (INDEX2 - 2) EQUAL TO 4                        NC2214.2
044200        PERFORM PASS                                              NC2214.2
044300        GO TO INS-WRITE-F1-3.                                     NC2214.2
044400     GO TO INS-FAIL-F1-3.                                         NC2214.2
044500 INS-DELETE-F1-3.                                                 NC2214.2
044600     PERFORM DE-LETE.                                             NC2214.2
044700     GO TO INS-WRITE-F1-3.                                        NC2214.2
044800 INS-FAIL-F1-3.                                                   NC2214.2
044900     PERFORM FAIL.                                                NC2214.2
045000     MOVE WRK-DU-999 (INDEX2 - 2) TO COMPUTED-N.                  NC2214.2
045100     MOVE 4 TO CORRECT-N.                                         NC2214.2
045200 INS-WRITE-F1-3.                                                  NC2214.2
045300     PERFORM PRINT-DETAIL.                                        NC2214.2
045400*                                                                 NC2214.2
045500 INS-INIT-F2-4.                                                   NC2214.2
045600     MOVE "INS-TEST-F2-4" TO PAR-NAME.                            NC2214.2
045700     MOVE "IV-21 4.3.8.2"        TO ANSI-REFERENCE.               NC2214.2
045800     MOVE "REPLACE BEFORE FIRST" TO FEATURE.                      NC2214.2
045900     SET INDEX1 TO 4.                                             NC2214.2
046000     SET INDEX4 TO 1.                                             NC2214.2
046100 INS-TEST-F2-4.                                                   NC2214.2
046200     INSPECT TABLE1-REC (INDEX1) REPLACING LEADING                NC2214.2
046300        TABLE4-LETTER (INDEX4) BY TABLE4-LETTER (INDEX4 + 1)      NC2214.2
046400        BEFORE INITIAL " AH YES" FIRST "I" BY "O"                 NC2214.2
046500        AFTER INITIAL "." ALL ". " BY ", " AFTER INITIAL          NC2214.2
046600        TABLE4-LETTER (INDEX4 + 2).                               NC2214.2
046700     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-1                  NC2214.2
046800        PERFORM PASS                                              NC2214.2
046900        GO TO INS-WRITE-F2-4.                                     NC2214.2
047000     GO TO INS-FAIL-F2-4.                                         NC2214.2
047100 INS-DELETE-F2-4.                                                 NC2214.2
047200     PERFORM DE-LETE.                                             NC2214.2
047300     GO TO INS-WRITE-F2-4.                                        NC2214.2
047400 INS-FAIL-F2-4.                                                   NC2214.2
047500     MOVE    TABLE1-REC (INDEX1) TO WS-WRONG-1-83.                NC2214.2
047600     MOVE    ANS-XN-83-1     TO WS-RIGHT-1-83.                    NC2214.2
047700     PERFORM FAIL.                                                NC2214.2
047800     MOVE    WS-RIGHT-1-20   TO CORRECT-A.                        NC2214.2
047900     MOVE    WS-WRONG-1-20   TO COMPUTED-A.                       NC2214.2
048000     PERFORM PRINT-DETAIL.                                        NC2214.2
048100     MOVE    WS-RIGHT-21-40  TO CORRECT-A.                        NC2214.2
048200     MOVE    WS-WRONG-21-40  TO COMPUTED-A.                       NC2214.2
048300     PERFORM PRINT-DETAIL.                                        NC2214.2
048400     MOVE    WS-RIGHT-41-60  TO CORRECT-A.                        NC2214.2
048500     MOVE    WS-WRONG-41-60  TO COMPUTED-A.                       NC2214.2
048600     PERFORM PRINT-DETAIL.                                        NC2214.2
048700     MOVE    WS-RIGHT-61-80  TO CORRECT-A.                        NC2214.2
048800     MOVE    WS-WRONG-61-80  TO COMPUTED-A.                       NC2214.2
048900     PERFORM PRINT-DETAIL.                                        NC2214.2
049000     MOVE    WS-RIGHT-81-83  TO CORRECT-A.                        NC2214.2
049100     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2214.2
049200     PERFORM PRINT-DETAIL.                                        NC2214.2
049300 INS-WRITE-F2-4.                                                  NC2214.2
049400     PERFORM PRINT-DETAIL.                                        NC2214.2
049500*                                                                 NC2214.2
049600 INS-INIT-F2-5.                                                   NC2214.2
049700     MOVE "INS-TEST-F2-5" TO PAR-NAME.                            NC2214.2
049800     MOVE "IV-21 4.3.8.2"      TO ANSI-REFERENCE.                 NC2214.2
049900     MOVE "REPLACE LEAD AFTER" TO FEATURE                         NC2214.2
050000     PERFORM INIT-TABLE1.                                         NC2214.2
050100     MOVE                                                         NC2214.2
050200     "AH YES,AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
050300-    "IDS CAN NOT BE ALL BAD."                                    NC2214.2
050400         TO ANS-XN-83-2.                                          NC2214.2
050500     SET INDEX1 TO 1.                                             NC2214.2
050600     SET INDEX3 TO 1.                                             NC2214.2
050700 INS-TEST-F2-5.                                                   NC2214.2
050800     INSPECT TABLE1-REC (INDEX1) REPLACING LEADING                NC2214.2
050900        TABLE3-SYMBOL (INDEX3) BY TABLE3-SYMBOL (INDEX3 + 1)      NC2214.2
051000        AFTER INITIAL "YES".                                      NC2214.2
051100     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-2                  NC2214.2
051200        PERFORM PASS                                              NC2214.2
051300        GO TO INS-WRITE-F2-5.                                     NC2214.2
051400     GO TO INS-FAIL-F2-5.                                         NC2214.2
051500 INS-DELETE-F2-5.                                                 NC2214.2
051600     PERFORM DE-LETE.                                             NC2214.2
051700     GO TO INS-WRITE-F2-5.                                        NC2214.2
051800 INS-FAIL-F2-5.                                                   NC2214.2
051900     MOVE    TABLE1-REC (INDEX1) TO WS-WRONG-1-83.                NC2214.2
052000     MOVE    ANS-XN-83-2     TO WS-RIGHT-1-83.                    NC2214.2
052100     PERFORM FAIL.                                                NC2214.2
052200     MOVE    WS-RIGHT-1-20   TO CORRECT-A.                        NC2214.2
052300     MOVE    WS-WRONG-1-20   TO COMPUTED-A.                       NC2214.2
052400     PERFORM PRINT-DETAIL.                                        NC2214.2
052500     MOVE    WS-RIGHT-21-40  TO CORRECT-A.                        NC2214.2
052600     MOVE    WS-WRONG-21-40  TO COMPUTED-A.                       NC2214.2
052700     PERFORM PRINT-DETAIL.                                        NC2214.2
052800     MOVE    WS-RIGHT-41-60  TO CORRECT-A.                        NC2214.2
052900     MOVE    WS-WRONG-41-60  TO COMPUTED-A.                       NC2214.2
053000     PERFORM PRINT-DETAIL.                                        NC2214.2
053100     MOVE    WS-RIGHT-61-80  TO CORRECT-A.                        NC2214.2
053200     MOVE    WS-WRONG-61-80  TO COMPUTED-A.                       NC2214.2
053300     PERFORM PRINT-DETAIL.                                        NC2214.2
053400     MOVE    WS-RIGHT-81-83  TO CORRECT-A.                        NC2214.2
053500     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2214.2
053600     PERFORM PRINT-DETAIL.                                        NC2214.2
053700 INS-WRITE-F2-5.                                                  NC2214.2
053800     PERFORM PRINT-DETAIL.                                        NC2214.2
053900*                                                                 NC2214.2
054000 INS-INIT-F2-6.                                                   NC2214.2
054100     MOVE "INS-TEST-F2-6" TO PAR-NAME.                            NC2214.2
054200     MOVE "IV-21 4.3.8.2"        TO ANSI-REFERENCE.               NC2214.2
054300     MOVE "REPLACE FIRST BEFORE" TO FEATURE.                      NC2214.2
054400     PERFORM INIT-TABLE1.                                         NC2214.2
054500     SET INDEX1 TO 3.                                             NC2214.2
054600     MOVE                                                         NC2214.2
054700     "OH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
054800-    "IDS CAN NOT BE ALL BAD."                                    NC2214.2
054900         TO ANS-XN-83-3.                                          NC2214.2
055000 INS-TEST-F2-6.                                                   NC2214.2
055100     INSPECT TABLE1-REC (INDEX1 - 1) REPLACING FIRST "A" BY "O"   NC2214.2
055200        BEFORE INITIAL "H YES".                                   NC2214.2
055300     IF TABLE1-REC (INDEX1 - 1) EQUAL TO ANS-XN-83-3              NC2214.2
055400        PERFORM PASS                                              NC2214.2
055500        GO TO INS-WRITE-F2-6.                                     NC2214.2
055600     GO TO INS-FAIL-F2-6.                                         NC2214.2
055700 INS-DELETE-F2-6.                                                 NC2214.2
055800     PERFORM DE-LETE.                                             NC2214.2
055900     GO TO INS-WRITE-F2-6.                                        NC2214.2
056000 INS-FAIL-F2-6.                                                   NC2214.2
056100     MOVE    TABLE1-REC (INDEX1 - 1) TO WS-WRONG-1-83.            NC2214.2
056200     MOVE    ANS-XN-83-3     TO WS-RIGHT-1-83.                    NC2214.2
056300     PERFORM FAIL.                                                NC2214.2
056400     MOVE    WS-RIGHT-1-20   TO CORRECT-A.                        NC2214.2
056500     MOVE    WS-WRONG-1-20   TO COMPUTED-A.                       NC2214.2
056600     PERFORM PRINT-DETAIL.                                        NC2214.2
056700     MOVE    WS-RIGHT-21-40  TO CORRECT-A.                        NC2214.2
056800     MOVE    WS-WRONG-21-40  TO COMPUTED-A.                       NC2214.2
056900     PERFORM PRINT-DETAIL.                                        NC2214.2
057000     MOVE    WS-RIGHT-41-60  TO CORRECT-A.                        NC2214.2
057100     MOVE    WS-WRONG-41-60  TO COMPUTED-A.                       NC2214.2
057200     PERFORM PRINT-DETAIL.                                        NC2214.2
057300     MOVE    WS-RIGHT-61-80  TO CORRECT-A.                        NC2214.2
057400     MOVE    WS-WRONG-61-80  TO COMPUTED-A.                       NC2214.2
057500     PERFORM PRINT-DETAIL.                                        NC2214.2
057600     MOVE    WS-RIGHT-81-83  TO CORRECT-A.                        NC2214.2
057700     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2214.2
057800     PERFORM PRINT-DETAIL.                                        NC2214.2
057900 INS-WRITE-F2-6.                                                  NC2214.2
058000     PERFORM PRINT-DETAIL.                                        NC2214.2
058100*                                                                 NC2214.2
058200 INS-INIT-F2-7.                                                   NC2214.2
058300     MOVE "INS-TEST-F2-7"     TO PAR-NAME.                        NC2214.2
058400     MOVE "IV-21 4.3.8.2"     TO ANSI-REFERENCE.                  NC2214.2
058500     MOVE "REPLACE ALL AFTER" TO FEATURE.                         NC2214.2
058600     PERFORM INIT-TABLE1.                                         NC2214.2
058700     PERFORM INIT-TABLE4.                                         NC2214.2
058800     MOVE                                                         NC2214.2
058900     "AH YES AH YES W.C. FRITOES HERE. ANYONE WHO HATES DOGS AND KNC2214.2
059000-    "IDS CAN NOT BE ALL-BAD."                                    NC2214.2
059100     TO ANS-XN-83-4.                                              NC2214.2
059200     SET INDEX1 TO 1.                                             NC2214.2
059300     SET INDEX4 TO 4.                                             NC2214.2
059400 INS-TEST-F2-7.                                                   NC2214.2
059500     INSPECT TABLE1-REC (INDEX1 + 1) REPLACING ALL SPACES BY "-"  NC2214.2
059600        AFTER TABLE4-LETTER (INDEX4).                             NC2214.2
059700     IF TABLE1-REC (INDEX1 + 1) EQUAL TO ANS-XN-83-4              NC2214.2
059800        PERFORM PASS                                              NC2214.2
059900        GO TO INS-WRITE-F2-7.                                     NC2214.2
060000     GO TO INS-FAIL-F2-7.                                         NC2214.2
060100 INS-DELETE-F2-7.                                                 NC2214.2
060200     PERFORM DE-LETE.                                             NC2214.2
060300     GO TO INS-WRITE-F2-7.                                        NC2214.2
060400 INS-FAIL-F2-7.                                                   NC2214.2
060500     MOVE    TABLE1-REC (INDEX1 + 1) TO WS-WRONG-1-83.            NC2214.2
060600     MOVE    ANS-XN-83-4     TO WS-RIGHT-1-83.                    NC2214.2
060700     PERFORM FAIL.                                                NC2214.2
060800     MOVE    WS-RIGHT-1-20   TO CORRECT-A.                        NC2214.2
060900     MOVE    WS-WRONG-1-20   TO COMPUTED-A.                       NC2214.2
061000     PERFORM PRINT-DETAIL.                                        NC2214.2
061100     MOVE    WS-RIGHT-21-40  TO CORRECT-A.                        NC2214.2
061200     MOVE    WS-WRONG-21-40  TO COMPUTED-A.                       NC2214.2
061300     PERFORM PRINT-DETAIL.                                        NC2214.2
061400     MOVE    WS-RIGHT-41-60  TO CORRECT-A.                        NC2214.2
061500     MOVE    WS-WRONG-41-60  TO COMPUTED-A.                       NC2214.2
061600     PERFORM PRINT-DETAIL.                                        NC2214.2
061700     MOVE    WS-RIGHT-61-80  TO CORRECT-A.                        NC2214.2
061800     MOVE    WS-WRONG-61-80  TO COMPUTED-A.                       NC2214.2
061900     PERFORM PRINT-DETAIL.                                        NC2214.2
062000     MOVE    WS-RIGHT-81-83  TO CORRECT-A.                        NC2214.2
062100     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2214.2
062200     PERFORM PRINT-DETAIL.                                        NC2214.2
062300 INS-WRITE-F2-7.                                                  NC2214.2
062400     PERFORM PRINT-DETAIL.                                        NC2214.2
062500*                                                                 NC2214.2
062600 INS-INIT-F3-8.                                                   NC2214.2
062700     MOVE "INS-TEST-F3-8"       TO PAR-NAME.                      NC2214.2
062800     MOVE "IV-21 4.3.8.2"       TO ANSI-REFERENCE.                NC2214.2
062900     MOVE "TALLY REPLACE CHARS" TO FEATURE.                       NC2214.2
063000     MOVE 1 TO REC-CT.                                            NC2214.2
063100     PERFORM INIT-TABLE1.                                         NC2214.2
063200     PERFORM INIT-TABLE4.                                         NC2214.2
063300     MOVE ZEROS TO TABLE2.                                        NC2214.2
063400     SET INDEX1 TO 4.                                             NC2214.2
063500     SET INDEX2 TO 1.                                             NC2214.2
063600     SET INDEX4 TO 5.                                             NC2214.2
063700 INS-TEST-F3-8-0.                                                 NC2214.2
063800     INSPECT TABLE1-REC (INDEX1 - 2)                              NC2214.2
063900        TALLYING WRK-DU-999 (INDEX2 + 2) FOR CHARACTERS           NC2214.2
064000        AFTER "L" REPLACING ALL "A" BY "E"                        NC2214.2
064100        BEFORE INITIAL TABLE4-LETTER (INDEX4).                    NC2214.2
064200     GO TO INS-TEST-F3-8-1.                                       NC2214.2
064300 INS-DELETE-F3-8.                                                 NC2214.2
064400     PERFORM DE-LETE.                                             NC2214.2
064500     PERFORM PRINT-DETAIL.                                        NC2214.2
064600     GO TO INS-INIT-F3-9.                                         NC2214.2
064700 INS-TEST-F3-8-1.                                                 NC2214.2
064800     IF WRK-DU-999 (INDEX2 + 2) EQUAL TO 6                        NC2214.2
064900        PERFORM PASS                                              NC2214.2
065000        GO TO INS-WRITE-F3-8-1                                    NC2214.2
065100        ELSE GO TO INS-FAIL-F3-8-1.                               NC2214.2
065200 INS-DELETE-F3-8-1.                                               NC2214.2
065300     PERFORM DE-LETE.                                             NC2214.2
065400     GO TO INS-WRITE-F3-8-1.                                      NC2214.2
065500 INS-FAIL-F3-8-1.                                                 NC2214.2
065600     MOVE WRK-DU-999 (INDEX2 + 2) TO COMPUTED-N                   NC2214.2
065700     MOVE 6 TO CORRECT-N .                                        NC2214.2
065800     PERFORM FAIL.                                                NC2214.2
065900 INS-WRITE-F3-8-1.                                                NC2214.2
066000     PERFORM PRINT-DETAIL.                                        NC2214.2
066100*                                                                 NC2214.2
066200 INS-TEST-F3-8-2.                                                 NC2214.2
066300     ADD 1 TO REC-CT.                                             NC2214.2
066400     IF TABLE1-REC (INDEX1 - 2) EQUAL TO ANS-XN-83-5              NC2214.2
066500        PERFORM PASS                                              NC2214.2
066600        GO TO INS-WRITE-F3-8-2                                    NC2214.2
066700     ELSE                                                         NC2214.2
066800        GO TO INS-FAIL-F3-8-2.                                    NC2214.2
066900 INS-DELETE-F3-8-2.                                               NC2214.2
067000     PERFORM DE-LETE.                                             NC2214.2
067100     GO TO INS-WRITE-F3-8-2.                                      NC2214.2
067200 INS-FAIL-F3-8-2.                                                 NC2214.2
067300     MOVE    TABLE1-REC (INDEX1 - 2) TO WS-WRONG-1-83             NC2214.2
067400     MOVE    ANS-XN-83-5     TO WS-RIGHT-1-83                     NC2214.2
067500     PERFORM FAIL                                                 NC2214.2
067600     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2214.2
067700     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2214.2
067800     PERFORM PRINT-DETAIL                                         NC2214.2
067900     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2214.2
068000     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2214.2
068100     PERFORM PRINT-DETAIL                                         NC2214.2
068200     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2214.2
068300     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2214.2
068400     PERFORM PRINT-DETAIL                                         NC2214.2
068500     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2214.2
068600     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2214.2
068700     PERFORM PRINT-DETAIL                                         NC2214.2
068800     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2214.2
068900     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2214.2
069000 INS-WRITE-F3-8-2.                                                NC2214.2
069100     PERFORM PRINT-DETAIL.                                        NC2214.2
069200*                                                                 NC2214.2
069300 INS-INIT-F3-9.                                                   NC2214.2
069400     MOVE "INS-TEST-F3-9"        TO PAR-NAME.                     NC2214.2
069500     MOVE "IV-21 4.3.8.2"        TO ANSI-REFERENCE.               NC2214.2
069600     MOVE "TALLY BEFORE REPLACE" TO FEATURE.                      NC2214.2
069700     MOVE 1 TO REC-CT.                                            NC2214.2
069800     PERFORM INIT-TABLE1.                                         NC2214.2
069900     MOVE ZEROS TO TABLE2.                                        NC2214.2
070000     PERFORM INIT-TABLE4.                                         NC2214.2
070100     SET INDEX1 TO 4.                                             NC2214.2
070200     SET INDEX2 TO 2.                                             NC2214.2
070300     SET INDEX4 TO 1.                                             NC2214.2
070400 INS-TEST-F3-9-0.                                                 NC2214.2
070500     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC2214.2
070600        FOR ALL "A" BEFORE "L" REPLACING FIRST                    NC2214.2
070700        TABLE4-LETTER (INDEX4) BY TABLE4-LETTER (INDEX4 + 1)      NC2214.2
070800        AFTER INITIAL "H".                                        NC2214.2
070900     GO TO INS-TEST-F3-9-1.                                       NC2214.2
071000 INS-DELETE-F3-9.                                                 NC2214.2
071100     PERFORM DE-LETE.                                             NC2214.2
071200     PERFORM PRINT-DETAIL.                                        NC2214.2
071300     GO TO INS-INIT-F3-10.                                        NC2214.2
071400 INS-TEST-F3-9-1.                                                 NC2214.2
071500     IF WRK-DU-999 (INDEX2) EQUAL TO 7                            NC2214.2
071600        PERFORM PASS                                              NC2214.2
071700        GO TO INS-WRITE-F3-9-1                                    NC2214.2
071800        ELSE GO TO INS-FAIL-F3-9-1.                               NC2214.2
071900 INS-DELETE-F3-9-1.                                               NC2214.2
072000     PERFORM DE-LETE.                                             NC2214.2
072100     GO TO INS-WRITE-F3-9-1.                                      NC2214.2
072200 INS-FAIL-F3-9-1.                                                 NC2214.2
072300     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N                       NC2214.2
072400     MOVE 7 TO CORRECT-N                                          NC2214.2
072500     PERFORM FAIL.                                                NC2214.2
072600 INS-WRITE-F3-9-1.                                                NC2214.2
072700     PERFORM PRINT-DETAIL.                                        NC2214.2
072800*                                                                 NC2214.2
072900 INS-TEST-F3-9-2.                                                 NC2214.2
073000     ADD 1 TO REC-CT.                                             NC2214.2
073100     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-6                  NC2214.2
073200        PERFORM PASS                                              NC2214.2
073300        GO TO INS-WRITE-F3-9-2                                    NC2214.2
073400     ELSE                                                         NC2214.2
073500        GO TO INS-FAIL-F3-9-2.                                    NC2214.2
073600 INS-DELETE-F3-9-2.                                               NC2214.2
073700     PERFORM DE-LETE.                                             NC2214.2
073800     GO TO INS-WRITE-F3-9-2.                                      NC2214.2
073900 INS-FAIL-F3-9-2.                                                 NC2214.2
074000     MOVE    TABLE1-REC (INDEX1) TO WS-WRONG-1-83                 NC2214.2
074100     MOVE    ANS-XN-83-6     TO WS-RIGHT-1-83                     NC2214.2
074200     PERFORM FAIL                                                 NC2214.2
074300     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2214.2
074400     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2214.2
074500     PERFORM PRINT-DETAIL                                         NC2214.2
074600     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2214.2
074700     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2214.2
074800     PERFORM PRINT-DETAIL                                         NC2214.2
074900     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2214.2
075000     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2214.2
075100     PERFORM PRINT-DETAIL                                         NC2214.2
075200     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2214.2
075300     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2214.2
075400     PERFORM PRINT-DETAIL                                         NC2214.2
075500     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2214.2
075600     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2214.2
075700 INS-WRITE-F3-9-2.                                                NC2214.2
075800     PERFORM PRINT-DETAIL.                                        NC2214.2
075900*                                                                 NC2214.2
076000 INS-INIT-F3-10.                                                  NC2214.2
076100     MOVE "INS-TEST-F3-10"       TO PAR-NAME.                     NC2214.2
076200     MOVE "IV-21 4.3.8.2"        TO ANSI-REFERENCE.               NC2214.2
076300     MOVE "TALLY LEAD REPL LEAD" TO FEATURE.                      NC2214.2
076400     MOVE 1 TO REC-CT.                                            NC2214.2
076500     PERFORM INIT-TABLE1.                                         NC2214.2
076600     MOVE ZEROS TO TABLE2.                                        NC2214.2
076700     PERFORM INIT-TABLE4.                                         NC2214.2
076800     SET INDEX1 TO 1.                                             NC2214.2
076900     SET INDEX2 TO 1.                                             NC2214.2
077000     SET INDEX4 TO 1.                                             NC2214.2
077100 INS-TEST-F3-10-0.                                                NC2214.2
077200     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC2214.2
077300        FOR LEADING TABLE4-LETTER (INDEX4) REPLACING              NC2214.2
077400        LEADING TABLE4-LETTER (INDEX4) BY "OH".                   NC2214.2
077500     GO TO INS-TEST-F3-10-1.                                      NC2214.2
077600 INS-DELETE-F3-10.                                                NC2214.2
077700     PERFORM DE-LETE.                                             NC2214.2
077800     PERFORM PRINT-DETAIL.                                        NC2214.2
077900     GO TO INS-INIT-F3-11.                                        NC2214.2
078000 INS-TEST-F3-10-1.                                                NC2214.2
078100     IF WRK-DU-999 (INDEX2) EQUAL TO 1                            NC2214.2
078200        PERFORM PASS                                              NC2214.2
078300        GO TO INS-WRITE-F3-10-1                                   NC2214.2
078400     ELSE                                                         NC2214.2
078500        GO TO INS-FAIL-F3-10-1.                                   NC2214.2
078600 INS-DELETE-F3-10-1.                                              NC2214.2
078700     PERFORM DE-LETE.                                             NC2214.2
078800     GO TO INS-WRITE-F3-10-1.                                     NC2214.2
078900 INS-FAIL-F3-10-1.                                                NC2214.2
079000     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N                       NC2214.2
079100     MOVE 1 TO CORRECT-N                                          NC2214.2
079200     PERFORM FAIL.                                                NC2214.2
079300 INS-WRITE-F3-10-1.                                               NC2214.2
079400     PERFORM PRINT-DETAIL.                                        NC2214.2
079500*                                                                 NC2214.2
079600 INS-TEST-F3-10-2.                                                NC2214.2
079700     ADD 1 TO REC-CT.                                             NC2214.2
079800     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-3                  NC2214.2
079900        PERFORM PASS                                              NC2214.2
080000        GO TO INS-WRITE-F3-10-2                                   NC2214.2
080100     ELSE                                                         NC2214.2
080200        GO TO INS-FAIL-F3-10-2.                                   NC2214.2
080300 INS-DELETE-F3-10-2.                                              NC2214.2
080400     PERFORM DE-LETE.                                             NC2214.2
080500     GO TO INS-WRITE-F3-10-2.                                     NC2214.2
080600 INS-FAIL-F3-10-2.                                                NC2214.2
080700     MOVE    TABLE1-REC (INDEX1) TO WS-WRONG-1-83                 NC2214.2
080800     MOVE    ANS-XN-83-3     TO WS-RIGHT-1-83                     NC2214.2
080900     PERFORM FAIL                                                 NC2214.2
081000     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2214.2
081100     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2214.2
081200     PERFORM PRINT-DETAIL                                         NC2214.2
081300     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2214.2
081400     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2214.2
081500     PERFORM PRINT-DETAIL                                         NC2214.2
081600     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2214.2
081700     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2214.2
081800     PERFORM PRINT-DETAIL                                         NC2214.2
081900     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2214.2
082000     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2214.2
082100     PERFORM PRINT-DETAIL                                         NC2214.2
082200     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2214.2
082300     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2214.2
082400 INS-WRITE-F3-10-2.                                               NC2214.2
082500     PERFORM PRINT-DETAIL.                                        NC2214.2
082600*                                                                 NC2214.2
082700 INS-INIT-F3-11.                                                  NC2214.2
082800     MOVE "INS-TEST-F3-11"       TO PAR-NAME.                     NC2214.2
082900     MOVE "IV-21 4.3.8.2"        TO ANSI-REFERENCE.               NC2214.2
083000     MOVE "TALLY REPL FIRST AFT" TO FEATURE.                      NC2214.2
083100     MOVE 1 TO REC-CT.                                            NC2214.2
083200     PERFORM INIT-TABLE1.                                         NC2214.2
083300     MOVE ZEROS TO TABLE2.                                        NC2214.2
083400     SET INDEX1 TO 2.                                             NC2214.2
083500     SET INDEX2 TO 2.                                             NC2214.2
083600 INS-TEST-F3-11-0.                                                NC2214.2
083700     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC2214.2
083800        FOR ALL "A" REPLACING FIRST "AH" BY "OH" AFTER            NC2214.2
083900        INITIAL "YES".                                            NC2214.2
084000     GO TO INS-TEST-F3-11-1.                                      NC2214.2
084100 INS-DELETE-F3-11.                                                NC2214.2
084200     PERFORM DE-LETE.                                             NC2214.2
084300     PERFORM PRINT-DETAIL.                                        NC2214.2
084400     GO TO INS-INIT-F3-12.                                        NC2214.2
084500 INS-TEST-F3-11-1.                                                NC2214.2
084600     IF WRK-DU-999 (INDEX2) EQUAL TO 8                            NC2214.2
084700        PERFORM PASS                                              NC2214.2
084800        GO TO INS-WRITE-F3-11-1                                   NC2214.2
084900     ELSE                                                         NC2214.2
085000        GO TO INS-FAIL-F3-11-1.                                   NC2214.2
085100 INS-DELETE-F3-11-1.                                              NC2214.2
085200     PERFORM DE-LETE.                                             NC2214.2
085300     GO TO INS-WRITE-F3-11-1.                                     NC2214.2
085400 INS-FAIL-F3-11-1.                                                NC2214.2
085500     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N                       NC2214.2
085600     MOVE 8 TO CORRECT-N                                          NC2214.2
085700     PERFORM FAIL.                                                NC2214.2
085800 INS-WRITE-F3-11-1.                                               NC2214.2
085900     PERFORM PRINT-DETAIL.                                        NC2214.2
086000*                                                                 NC2214.2
086100 INS-TEST-F3-11-2.                                                NC2214.2
086200     ADD 1 TO REC-CT.                                             NC2214.2
086300     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-6                  NC2214.2
086400        PERFORM PASS                                              NC2214.2
086500        GO TO INS-WRITE-F3-11-2                                   NC2214.2
086600     ELSE                                                         NC2214.2
086700        GO TO INS-FAIL-F3-11-2.                                   NC2214.2
086800 INS-DELETE-F3-11-2.                                              NC2214.2
086900     PERFORM DE-LETE.                                             NC2214.2
087000     GO TO INS-WRITE-F3-11-2.                                     NC2214.2
087100 INS-FAIL-F3-11-2.                                                NC2214.2
087200     MOVE    TABLE1-REC (INDEX1) TO WS-WRONG-1-83                 NC2214.2
087300     MOVE    ANS-XN-83-6     TO WS-RIGHT-1-83                     NC2214.2
087400     PERFORM FAIL                                                 NC2214.2
087500     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2214.2
087600     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2214.2
087700     PERFORM PRINT-DETAIL                                         NC2214.2
087800     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2214.2
087900     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2214.2
088000     PERFORM PRINT-DETAIL                                         NC2214.2
088100     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2214.2
088200     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2214.2
088300     PERFORM PRINT-DETAIL                                         NC2214.2
088400     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2214.2
088500     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2214.2
088600     PERFORM PRINT-DETAIL                                         NC2214.2
088700     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2214.2
088800     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2214.2
088900 INS-WRITE-F3-11-2.                                               NC2214.2
089000     PERFORM PRINT-DETAIL.                                        NC2214.2
089100*                                                                 NC2214.2
089200 INS-INIT-F3-12.                                                  NC2214.2
089300     MOVE "INS-TEST-F3-12"       TO PAR-NAME.                     NC2214.2
089400     MOVE "IV-21 4.3.8.2"        TO ANSI-REFERENCE.               NC2214.2
089500     MOVE "TALLY CHARS REPL BEF" TO FEATURE.                      NC2214.2
089600     MOVE 1 TO REC-CT.                                            NC2214.2
089700     PERFORM INIT-TABLE1.                                         NC2214.2
089800     MOVE ZEROS TO TABLE2.                                        NC2214.2
089900     PERFORM INIT-TABLE4.                                         NC2214.2
090000     SET INDEX1 TO 3.                                             NC2214.2
090100     SET INDEX2 TO 3.                                             NC2214.2
090200     SET INDEX4 TO 1.                                             NC2214.2
090300 INS-TEST-F3-12-0.                                                NC2214.2
090400     INSPECT TABLE1-REC (INDEX1) TALLYING WRK-DU-999 (INDEX2)     NC2214.2
090500        FOR CHARACTERS AFTER TABLE4-LETTER (INDEX4)               NC2214.2
090600        REPLACING ALL "AH" BY "OH"                                NC2214.2
090700        BEFORE "YES".                                             NC2214.2
090800     GO TO INS-TEST-F3-12-1.                                      NC2214.2
090900 INS-DELETE-F3-12.                                                NC2214.2
091000     PERFORM DE-LETE.                                             NC2214.2
091100     PERFORM PRINT-DETAIL.                                        NC2214.2
091200     GO TO CCVS-999999.                                           NC2214.2
091300 INS-TEST-F3-12-1.                                                NC2214.2
091400     IF WRK-DU-999 (INDEX2) EQUAL TO 81                           NC2214.2
091500        PERFORM PASS                                              NC2214.2
091600        GO TO INS-WRITE-F3-12-1                                   NC2214.2
091700        ELSE                                                      NC2214.2
091800        GO TO INS-FAIL-F3-12-1.                                   NC2214.2
091900 INS-DELETE-F3-12-1.                                              NC2214.2
092000     PERFORM DE-LETE.                                             NC2214.2
092100     GO TO INS-WRITE-F3-12-1.                                     NC2214.2
092200 INS-FAIL-F3-12-1.                                                NC2214.2
092300     MOVE WRK-DU-999 (INDEX2) TO COMPUTED-N                       NC2214.2
092400     MOVE 81 TO CORRECT-N                                         NC2214.2
092500     PERFORM FAIL.                                                NC2214.2
092600 INS-WRITE-F3-12-1.                                               NC2214.2
092700     PERFORM PRINT-DETAIL.                                        NC2214.2
092800*                                                                 NC2214.2
092900 INS-TEST-F3-12-2.                                                NC2214.2
093000     ADD 1 TO REC-CT.                                             NC2214.2
093100     IF TABLE1-REC (INDEX1) EQUAL TO ANS-XN-83-3                  NC2214.2
093200        PERFORM PASS                                              NC2214.2
093300        GO TO INS-WRITE-F3-12-2                                   NC2214.2
093400     ELSE                                                         NC2214.2
093500        GO TO INS-FAIL-F3-12-2.                                   NC2214.2
093600 INS-DELETE-F3-12-2.                                              NC2214.2
093700     PERFORM DE-LETE.                                             NC2214.2
093800     GO TO INS-WRITE-F3-12-2.                                     NC2214.2
093900 INS-FAIL-F3-12-2.                                                NC2214.2
094000     MOVE    TABLE1-REC (INDEX1) TO WS-WRONG-1-83                 NC2214.2
094100     MOVE    ANS-XN-83-3     TO WS-RIGHT-1-83                     NC2214.2
094200     PERFORM FAIL                                                 NC2214.2
094300     MOVE    WS-RIGHT-1-20   TO CORRECT-A                         NC2214.2
094400     MOVE    WS-WRONG-1-20   TO COMPUTED-A                        NC2214.2
094500     PERFORM PRINT-DETAIL                                         NC2214.2
094600     MOVE    WS-RIGHT-21-40  TO CORRECT-A                         NC2214.2
094700     MOVE    WS-WRONG-21-40  TO COMPUTED-A                        NC2214.2
094800     PERFORM PRINT-DETAIL                                         NC2214.2
094900     MOVE    WS-RIGHT-41-60  TO CORRECT-A                         NC2214.2
095000     MOVE    WS-WRONG-41-60  TO COMPUTED-A                        NC2214.2
095100     PERFORM PRINT-DETAIL                                         NC2214.2
095200     MOVE    WS-RIGHT-61-80  TO CORRECT-A                         NC2214.2
095300     MOVE    WS-WRONG-61-80  TO COMPUTED-A                        NC2214.2
095400     PERFORM PRINT-DETAIL                                         NC2214.2
095500     MOVE    WS-RIGHT-81-83  TO CORRECT-A                         NC2214.2
095600     MOVE    WS-WRONG-81-83  TO COMPUTED-A.                       NC2214.2
095700 INS-WRITE-F3-12-2.                                               NC2214.2
095800     PERFORM PRINT-DETAIL.                                        NC2214.2
095900 CCVS-EXIT SECTION.                                               NC2214.2
096000 CCVS-999999.                                                     NC2214.2
096100     GO TO CLOSE-FILES.                                           NC2214.2
      *END-OF,NC221A                                                                  
