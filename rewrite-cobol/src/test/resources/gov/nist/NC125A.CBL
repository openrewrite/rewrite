      *HEADER,COBOL,NC125A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1254.2
000200 PROGRAM-ID.                                                      NC1254.2
000300     NC125A.                                                      NC1254.2
000400****************************************************************  NC1254.2
000500*                                                              *  NC1254.2
000600*    VALIDATION FOR:-                                          *  NC1254.2
000700*                                                              *  NC1254.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1254.2
000900*                                                              *  NC1254.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1254.2
001100*                                                              *  NC1254.2
001200****************************************************************  NC1254.2
001300*                                                              *  NC1254.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1254.2
001500*                                                              *  NC1254.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1254.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1254.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1254.2
001900*                                                              *  NC1254.2
002000****************************************************************  NC1254.2
002100*                                                                 NC1254.2
002200*    PROGRAM NC125A TESTS THE USE OF PICTURE CHARACTERS           NC1254.2
002300*    $ + * . , WITH FORMAT 1 OF THE "MOVE" STATEMENT AND          NC1254.2
002400*    FORMAT 2 OF THE "ADD" AND "SUBTRACT" STATEMENTS.             NC1254.2
002500*                                                                 NC1254.2
002600 ENVIRONMENT DIVISION.                                            NC1254.2
002700 CONFIGURATION SECTION.                                           NC1254.2
002800 SOURCE-COMPUTER.                                                 NC1254.2
002900     XXXXX082.                                                    NC1254.2
003000 OBJECT-COMPUTER.                                                 NC1254.2
003100     XXXXX083.                                                    NC1254.2
003200 INPUT-OUTPUT SECTION.                                            NC1254.2
003300 FILE-CONTROL.                                                    NC1254.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1254.2
003500     XXXXX055.                                                    NC1254.2
003600 DATA DIVISION.                                                   NC1254.2
003700 FILE SECTION.                                                    NC1254.2
003800 FD  PRINT-FILE.                                                  NC1254.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1254.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1254.2
004100 WORKING-STORAGE SECTION.                                         NC1254.2
004200 01  W1.                                                          NC1254.2
004300     02 WRK-EDIT-001 PIC $$,$$$,$$$,$$$,$$$,$$$.99.               NC1254.2
004400 01  W2.                                                          NC1254.2
004500     02 WRK-EDIT-002 PIC ++,+++,+++,+++,+++,+++.99.               NC1254.2
004600 01  W3.                                                          NC1254.2
004700     02 WRK-EDIT-003 PIC --,---,---,---,---,---.99.               NC1254.2
004800 01  W4.                                                          NC1254.2
004900     02 WRK-EDIT-004 PIC  *,***,***,***,***,***.99.               NC1254.2
005000 01  W5.                                                          NC1254.2
005100     02 WRK-EDIT-005 PIC  9,9,9,9,9,9,9,9,9,9,9,9,.               NC1254.2
005200 01  W6.                                                          NC1254.2
005300     02 WRK-EDIT-006 PIC  999999999999..                          NC1254.2
005400 01  TBL-001.                                                     NC1254.2
005500     02 FILLER PIC S9(16)V99 VALUE ZERO.                          NC1254.2
005600     02 FILLER PIC S9(16)V99 VALUE .01.                           NC1254.2
005700     02 FILLER PIC S9(16)V99 VALUE .19.                           NC1254.2
005800     02 FILLER PIC S9(16)V99 VALUE 1.00.                          NC1254.2
005900     02 FILLER PIC S9(16)V99 VALUE 111.11.                        NC1254.2
006000     02 FILLER PIC S9(16)V99 VALUE 9999.11.                       NC1254.2
006100     02 FILLER PIC S9(16)V99 VALUE 1010101.99.                    NC1254.2
006200     02 FILLER PIC S9(16)V99 VALUE 900000000.11.                  NC1254.2
006300     02 FILLER PIC S9(16)V99 VALUE 9999999999.99.                 NC1254.2
006400 01  TBL-001-R REDEFINES TBL-001.                                 NC1254.2
006500     02 TBL-001-O PIC S9(16)V99 OCCURS 9 TIMES.                   NC1254.2
006600 01  TBL-002.                                                     NC1254.2
006700     02 FILLER PIC X(25) VALUE "                     $.00".       NC1254.2
006800     02 FILLER PIC X(25) VALUE "                     $.01".       NC1254.2
006900     02 FILLER PIC X(25) VALUE "                     $.19".       NC1254.2
007000     02 FILLER PIC X(25) VALUE "                    $1.00".       NC1254.2
007100     02 FILLER PIC X(25) VALUE "                  $111.11".       NC1254.2
007200     02 FILLER PIC X(25) VALUE "                $9,999.11".       NC1254.2
007300     02 FILLER PIC X(25) VALUE "            $1,010,101.99".       NC1254.2
007400     02 FILLER PIC X(25) VALUE "          $900,000,000.11".       NC1254.2
007500     02 FILLER PIC X(25) VALUE "        $9,999,999,999.99".       NC1254.2
007600 01  TBL-002-R REDEFINES TBL-002.                                 NC1254.2
007700     02 TBL-002-O PIC X(25) OCCURS 9 TIMES.                       NC1254.2
007800 01  TBL-003.                                                     NC1254.2
007900     02 FILLER PIC X(25) VALUE "                     +.00".       NC1254.2
008000     02 FILLER PIC X(25) VALUE "                     +.01".       NC1254.2
008100     02 FILLER PIC X(25) VALUE "                     +.19".       NC1254.2
008200     02 FILLER PIC X(25) VALUE "                    +1.00".       NC1254.2
008300     02 FILLER PIC X(25) VALUE "                  +111.11".       NC1254.2
008400     02 FILLER PIC X(25) VALUE "                +9,999.11".       NC1254.2
008500     02 FILLER PIC X(25) VALUE "            +1,010,101.99".       NC1254.2
008600     02 FILLER PIC X(25) VALUE "          +900,000,000.11".       NC1254.2
008700     02 FILLER PIC X(25) VALUE "        +9,999,999,999.99".       NC1254.2
008800 01  TBL-003-R REDEFINES TBL-003.                                 NC1254.2
008900     02 TBL-003-O PIC X(25) OCCURS 9 TIMES.                       NC1254.2
009000 01  TBL-004.                                                     NC1254.2
009100     02 FILLER PIC X(25) VALUE "                      .00".       NC1254.2
009200     02 FILLER PIC X(25) VALUE "                      .01".       NC1254.2
009300     02 FILLER PIC X(25) VALUE "                      .19".       NC1254.2
009400     02 FILLER PIC X(25) VALUE "                     1.00".       NC1254.2
009500     02 FILLER PIC X(25) VALUE "                   111.11".       NC1254.2
009600     02 FILLER PIC X(25) VALUE "                 9,999.11".       NC1254.2
009700     02 FILLER PIC X(25) VALUE "             1,010,101.99".       NC1254.2
009800     02 FILLER PIC X(25) VALUE "           900,000,000.11".       NC1254.2
009900     02 FILLER PIC X(25) VALUE "         9,999,999,999.99".       NC1254.2
010000 01  TBL-004-R REDEFINES TBL-004.                                 NC1254.2
010100     02 TBL-004-O PIC X(25) OCCURS 9 TIMES.                       NC1254.2
010200 01  TBL-005.                                                     NC1254.2
010300     02 FILLER PIC X(24) VALUE "*********************.00".        NC1254.2
010400     02 FILLER PIC X(24) VALUE "*********************.01".        NC1254.2
010500     02 FILLER PIC X(24) VALUE "*********************.19".        NC1254.2
010600     02 FILLER PIC X(24) VALUE "********************1.00".        NC1254.2
010700     02 FILLER PIC X(24) VALUE "******************111.11".        NC1254.2
010800     02 FILLER PIC X(24) VALUE "****************9,999.11".        NC1254.2
010900     02 FILLER PIC X(24) VALUE "************1,010,101.99".        NC1254.2
011000     02 FILLER PIC X(24) VALUE "**********900,000,000.11".        NC1254.2
011100     02 FILLER PIC X(24) VALUE "********9,999,999,999.99".        NC1254.2
011200 01  TBL-005-R REDEFINES TBL-005.                                 NC1254.2
011300     02 TBL-005-O PIC X(24) OCCURS 9 TIMES.                       NC1254.2
011400 01  TBL-006.                                                     NC1254.2
011500     02 FILLER PIC X(25) VALUE "                      .00".       NC1254.2
011600     02 FILLER PIC X(25) VALUE "                     -.01".       NC1254.2
011700     02 FILLER PIC X(25) VALUE "                     -.19".       NC1254.2
011800     02 FILLER PIC X(25) VALUE "                    -1.00".       NC1254.2
011900     02 FILLER PIC X(25) VALUE "                  -111.11".       NC1254.2
012000     02 FILLER PIC X(25) VALUE "                -9,999.11".       NC1254.2
012100     02 FILLER PIC X(25) VALUE "            -1,010,101.99".       NC1254.2
012200     02 FILLER PIC X(25) VALUE "          -900,000,000.11".       NC1254.2
012300     02 FILLER PIC X(25) VALUE "        -9,999,999,999.99".       NC1254.2
012400 01  TBL-006-R REDEFINES TBL-006.                                 NC1254.2
012500     02 TBL-006-O PIC X(25) OCCURS 9 TIMES.                       NC1254.2
012600 01  CTR-1 PIC 999 VALUE 0.                                       NC1254.2
012700 01  CRT-2 PIC 999 VALUE 9.                                       NC1254.2
012800 01  CTR-3 PIC 999 VALUE 0.                                       NC1254.2
012900 01  TEST-RESULTS.                                                NC1254.2
013000     02 FILLER                   PIC X      VALUE SPACE.          NC1254.2
013100     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1254.2
013200     02 FILLER                   PIC X      VALUE SPACE.          NC1254.2
013300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1254.2
013400     02 FILLER                   PIC X      VALUE SPACE.          NC1254.2
013500     02  PAR-NAME.                                                NC1254.2
013600       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1254.2
013700       03  PARDOT-X              PIC X      VALUE SPACE.          NC1254.2
013800       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1254.2
013900     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1254.2
014000     02 RE-MARK                  PIC X(61).                       NC1254.2
014100 01  TEST-COMPUTED.                                               NC1254.2
014200     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1254.2
014300     02 FILLER                   PIC X(17)  VALUE                 NC1254.2
014400            "       COMPUTED=".                                   NC1254.2
014500     02 COMPUTED-X.                                               NC1254.2
014600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1254.2
014700     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1254.2
014800                                 PIC -9(9).9(9).                  NC1254.2
014900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1254.2
015000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1254.2
015100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1254.2
015200     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1254.2
015300         04 COMPUTED-18V0                    PIC -9(18).          NC1254.2
015400         04 FILLER                           PIC X.               NC1254.2
015500     03 FILLER PIC X(50) VALUE SPACE.                             NC1254.2
015600 01  TEST-CORRECT.                                                NC1254.2
015700     02 FILLER PIC X(30) VALUE SPACE.                             NC1254.2
015800     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1254.2
015900     02 CORRECT-X.                                                NC1254.2
016000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1254.2
016100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1254.2
016200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1254.2
016300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1254.2
016400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1254.2
016500     03      CR-18V0 REDEFINES CORRECT-A.                         NC1254.2
016600         04 CORRECT-18V0                     PIC -9(18).          NC1254.2
016700         04 FILLER                           PIC X.               NC1254.2
016800     03 FILLER PIC X(2) VALUE SPACE.                              NC1254.2
016900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1254.2
017000 01  CCVS-C-1.                                                    NC1254.2
017100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1254.2
017200-    "SS  PARAGRAPH-NAME                                          NC1254.2
017300-    "       REMARKS".                                            NC1254.2
017400     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1254.2
017500 01  CCVS-C-2.                                                    NC1254.2
017600     02 FILLER                     PIC X        VALUE SPACE.      NC1254.2
017700     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1254.2
017800     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1254.2
017900     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1254.2
018000     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1254.2
018100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1254.2
018200 01  REC-CT                        PIC 99       VALUE ZERO.       NC1254.2
018300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1254.2
018400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1254.2
018500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1254.2
018600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1254.2
018700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1254.2
018800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1254.2
018900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1254.2
019000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1254.2
019100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1254.2
019200 01  CCVS-H-1.                                                    NC1254.2
019300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1254.2
019400     02  FILLER                    PIC X(42)    VALUE             NC1254.2
019500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1254.2
019600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1254.2
019700 01  CCVS-H-2A.                                                   NC1254.2
019800   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1254.2
019900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1254.2
020000   02  FILLER                        PIC XXXX   VALUE             NC1254.2
020100     "4.2 ".                                                      NC1254.2
020200   02  FILLER                        PIC X(28)  VALUE             NC1254.2
020300            " COPY - NOT FOR DISTRIBUTION".                       NC1254.2
020400   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1254.2
020500                                                                  NC1254.2
020600 01  CCVS-H-2B.                                                   NC1254.2
020700   02  FILLER                        PIC X(15)  VALUE             NC1254.2
020800            "TEST RESULT OF ".                                    NC1254.2
020900   02  TEST-ID                       PIC X(9).                    NC1254.2
021000   02  FILLER                        PIC X(4)   VALUE             NC1254.2
021100            " IN ".                                               NC1254.2
021200   02  FILLER                        PIC X(12)  VALUE             NC1254.2
021300     " HIGH       ".                                              NC1254.2
021400   02  FILLER                        PIC X(22)  VALUE             NC1254.2
021500            " LEVEL VALIDATION FOR ".                             NC1254.2
021600   02  FILLER                        PIC X(58)  VALUE             NC1254.2
021700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1254.2
021800 01  CCVS-H-3.                                                    NC1254.2
021900     02  FILLER                      PIC X(34)  VALUE             NC1254.2
022000            " FOR OFFICIAL USE ONLY    ".                         NC1254.2
022100     02  FILLER                      PIC X(58)  VALUE             NC1254.2
022200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1254.2
022300     02  FILLER                      PIC X(28)  VALUE             NC1254.2
022400            "  COPYRIGHT   1985 ".                                NC1254.2
022500 01  CCVS-E-1.                                                    NC1254.2
022600     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1254.2
022700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1254.2
022800     02 ID-AGAIN                     PIC X(9).                    NC1254.2
022900     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1254.2
023000 01  CCVS-E-2.                                                    NC1254.2
023100     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1254.2
023200     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1254.2
023300     02 CCVS-E-2-2.                                               NC1254.2
023400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1254.2
023500         03 FILLER                   PIC X      VALUE SPACE.      NC1254.2
023600         03 ENDER-DESC               PIC X(44)  VALUE             NC1254.2
023700            "ERRORS ENCOUNTERED".                                 NC1254.2
023800 01  CCVS-E-3.                                                    NC1254.2
023900     02  FILLER                      PIC X(22)  VALUE             NC1254.2
024000            " FOR OFFICIAL USE ONLY".                             NC1254.2
024100     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1254.2
024200     02  FILLER                      PIC X(58)  VALUE             NC1254.2
024300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1254.2
024400     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1254.2
024500     02 FILLER                       PIC X(15)  VALUE             NC1254.2
024600             " COPYRIGHT 1985".                                   NC1254.2
024700 01  CCVS-E-4.                                                    NC1254.2
024800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1254.2
024900     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1254.2
025000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1254.2
025100     02 FILLER                       PIC X(40)  VALUE             NC1254.2
025200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1254.2
025300 01  XXINFO.                                                      NC1254.2
025400     02 FILLER                       PIC X(19)  VALUE             NC1254.2
025500            "*** INFORMATION ***".                                NC1254.2
025600     02 INFO-TEXT.                                                NC1254.2
025700       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1254.2
025800       04 XXCOMPUTED                 PIC X(20).                   NC1254.2
025900       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1254.2
026000       04 XXCORRECT                  PIC X(20).                   NC1254.2
026100     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1254.2
026200 01  HYPHEN-LINE.                                                 NC1254.2
026300     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1254.2
026400     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1254.2
026500-    "*****************************************".                 NC1254.2
026600     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1254.2
026700-    "******************************".                            NC1254.2
026800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1254.2
026900     "NC125A".                                                    NC1254.2
027000 PROCEDURE DIVISION.                                              NC1254.2
027100 CCVS1 SECTION.                                                   NC1254.2
027200 OPEN-FILES.                                                      NC1254.2
027300     OPEN     OUTPUT PRINT-FILE.                                  NC1254.2
027400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1254.2
027500     MOVE    SPACE TO TEST-RESULTS.                               NC1254.2
027600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1254.2
027700     GO TO CCVS1-EXIT.                                            NC1254.2
027800 CLOSE-FILES.                                                     NC1254.2
027900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1254.2
028000 TERMINATE-CCVS.                                                  NC1254.2
028100*    EXIT PROGRAM.                                                NC1254.2
028200*TERMINATE-CALL.                                                  NC1254.2
028300     STOP     RUN.                                                NC1254.2
028400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1254.2
028500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1254.2
028600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1254.2
028700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1254.2
028800     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1254.2
028900 PRINT-DETAIL.                                                    NC1254.2
029000     IF REC-CT NOT EQUAL TO ZERO                                  NC1254.2
029100             MOVE "." TO PARDOT-X                                 NC1254.2
029200             MOVE REC-CT TO DOTVALUE.                             NC1254.2
029300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1254.2
029400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1254.2
029500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1254.2
029600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1254.2
029700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1254.2
029800     MOVE SPACE TO CORRECT-X.                                     NC1254.2
029900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1254.2
030000     MOVE     SPACE TO RE-MARK.                                   NC1254.2
030100 HEAD-ROUTINE.                                                    NC1254.2
030200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1254.2
030300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1254.2
030400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1254.2
030500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1254.2
030600 COLUMN-NAMES-ROUTINE.                                            NC1254.2
030700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1254.2
030800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1254.2
030900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1254.2
031000 END-ROUTINE.                                                     NC1254.2
031100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1254.2
031200 END-RTN-EXIT.                                                    NC1254.2
031300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1254.2
031400 END-ROUTINE-1.                                                   NC1254.2
031500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1254.2
031600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1254.2
031700      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1254.2
031800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1254.2
031900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1254.2
032000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1254.2
032100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1254.2
032200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1254.2
032300  END-ROUTINE-12.                                                 NC1254.2
032400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1254.2
032500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1254.2
032600         MOVE "NO " TO ERROR-TOTAL                                NC1254.2
032700         ELSE                                                     NC1254.2
032800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1254.2
032900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1254.2
033000     PERFORM WRITE-LINE.                                          NC1254.2
033100 END-ROUTINE-13.                                                  NC1254.2
033200     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1254.2
033300         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1254.2
033400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1254.2
033500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1254.2
033600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1254.2
033700      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1254.2
033800          MOVE "NO " TO ERROR-TOTAL                               NC1254.2
033900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1254.2
034000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1254.2
034100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1254.2
034200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1254.2
034300 WRITE-LINE.                                                      NC1254.2
034400     ADD 1 TO RECORD-COUNT.                                       NC1254.2
034500*    IF RECORD-COUNT GREATER 42                                   NC1254.2
034600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1254.2
034700*        MOVE SPACE TO DUMMY-RECORD                               NC1254.2
034800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1254.2
034900*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1254.2
035000*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1254.2
035100*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1254.2
035200*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1254.2
035300*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1254.2
035400*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1254.2
035500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1254.2
035600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1254.2
035700*        MOVE ZERO TO RECORD-COUNT.                               NC1254.2
035800     PERFORM WRT-LN.                                              NC1254.2
035900 WRT-LN.                                                          NC1254.2
036000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1254.2
036100     MOVE SPACE TO DUMMY-RECORD.                                  NC1254.2
036200 BLANK-LINE-PRINT.                                                NC1254.2
036300     PERFORM WRT-LN.                                              NC1254.2
036400 FAIL-ROUTINE.                                                    NC1254.2
036500     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1254.2
036600            GO TO FAIL-ROUTINE-WRITE.                             NC1254.2
036700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1254.2
036800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1254.2
036900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1254.2
037000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1254.2
037100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1254.2
037200     GO TO  FAIL-ROUTINE-EX.                                      NC1254.2
037300 FAIL-ROUTINE-WRITE.                                              NC1254.2
037400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1254.2
037500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1254.2
037600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1254.2
037700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1254.2
037800 FAIL-ROUTINE-EX. EXIT.                                           NC1254.2
037900 BAIL-OUT.                                                        NC1254.2
038000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1254.2
038100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1254.2
038200 BAIL-OUT-WRITE.                                                  NC1254.2
038300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1254.2
038400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1254.2
038500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1254.2
038600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1254.2
038700 BAIL-OUT-EX. EXIT.                                               NC1254.2
038800 CCVS1-EXIT.                                                      NC1254.2
038900     EXIT.                                                        NC1254.2
039000 SECT-NC125A-001 SECTION.                                         NC1254.2
039100 EDI-TEST-GF1.                                                    NC1254.2
039200     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
039300     MOVE "EDI-TEST-GF1" TO PAR-NAME.                             NC1254.2
039400     MOVE "l EDIT MOVE" TO FEATURE.                               NC1254.2
039500     MOVE 0 TO REC-CT.                                            NC1254.2
039600     MOVE 0 TO CTR-1.                                             NC1254.2
039700     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
039800     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
039900     PERFORM EDI-TEST-GF1-R CRT-2 TIMES                           NC1254.2
040000     GO TO EDI-TEST-GF1-EXIT.                                     NC1254.2
040100 EDI-TEST-GF1-DELETE.                                             NC1254.2
040200     PERFORM  DE-LETE.                                            NC1254.2
040300     PERFORM  PRINT-DETAIL.                                       NC1254.2
040400     GO TO    EDI-TEST-GF1-EXIT.                                  NC1254.2
040500 EDI-TEST-GF1-R.                                                  NC1254.2
040600     ADD 1 TO REC-CT.                                             NC1254.2
040700     ADD 1 TO CTR-1.                                              NC1254.2
040800     MOVE TBL-001-O (CTR-1) TO WRK-EDIT-001.                      NC1254.2
040900     IF WRK-EDIT-001 EQUAL TO TBL-002-O (CTR-1) PERFORM PASS      NC1254.2
041000     ELSE MOVE W1 TO COMPUTED-X MOVE TBL-002-O (CTR-1) TO         NC1254.2
041100         CORRECT-X                                                NC1254.2
041200     PERFORM FAIL.                                                NC1254.2
041300     PERFORM PRINT-DETAIL.                                        NC1254.2
041400 EDI-TEST-GF1-EXIT.                                               NC1254.2
041500     EXIT.                                                        NC1254.2
041600 EDI-TEST-GF2.                                                    NC1254.2
041700     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
041800     MOVE "EDI-TEST-GF2" TO PAR-NAME.                             NC1254.2
041900     MOVE "+ EDIT MOVE" TO FEATURE.                               NC1254.2
042000     MOVE 0 TO REC-CT.                                            NC1254.2
042100     MOVE 0 TO CTR-1.                                             NC1254.2
042200     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
042300     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
042400     PERFORM EDI-TEST-GF2-R CRT-2 TIMES                           NC1254.2
042500     GO TO EDI-TEST-GF2-EXIT.                                     NC1254.2
042600 EDI-TEST-GF2-DELETE.                                             NC1254.2
042700     PERFORM  DE-LETE.                                            NC1254.2
042800     PERFORM  PRINT-DETAIL.                                       NC1254.2
042900     GO TO    EDI-TEST-GF2-EXIT.                                  NC1254.2
043000 EDI-TEST-GF2-R.                                                  NC1254.2
043100     ADD 1 TO REC-CT.                                             NC1254.2
043200     ADD 1 TO CTR-1.                                              NC1254.2
043300     MOVE TBL-001-O (CTR-1) TO WRK-EDIT-002.                      NC1254.2
043400     IF WRK-EDIT-002 EQUAL TO TBL-003-O (CTR-1) PERFORM PASS      NC1254.2
043500     ELSE MOVE W2 TO COMPUTED-X MOVE TBL-003-O (CTR-1) TO         NC1254.2
043600         CORRECT-X                                                NC1254.2
043700     PERFORM FAIL.                                                NC1254.2
043800     PERFORM PRINT-DETAIL.                                        NC1254.2
043900 EDI-TEST-GF2-EXIT.                                               NC1254.2
044000     EXIT.                                                        NC1254.2
044100 EDI-TEST-GF3.                                                    NC1254.2
044200     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
044300     MOVE "EDI-TEST-GF3" TO PAR-NAME.                             NC1254.2
044400     MOVE "- EDIT MOVE" TO FEATURE.                               NC1254.2
044500     MOVE 0 TO REC-CT.                                            NC1254.2
044600     MOVE 0 TO CTR-1.                                             NC1254.2
044700     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
044800     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
044900     PERFORM EDI-TEST-GF3-R CRT-2 TIMES                           NC1254.2
045000     GO TO EDI-TEST-GF3-EXIT.                                     NC1254.2
045100 EDI-TEST-GF3-DELETE.                                             NC1254.2
045200     PERFORM  DE-LETE.                                            NC1254.2
045300     PERFORM  PRINT-DETAIL.                                       NC1254.2
045400     GO TO    EDI-TEST-GF3-EXIT.                                  NC1254.2
045500 EDI-TEST-GF3-R.                                                  NC1254.2
045600     ADD 1 TO REC-CT.                                             NC1254.2
045700     ADD 1 TO CTR-1.                                              NC1254.2
045800     MOVE TBL-001-O (CTR-1) TO WRK-EDIT-003.                      NC1254.2
045900     IF WRK-EDIT-003 EQUAL TO TBL-004-O (CTR-1) PERFORM PASS      NC1254.2
046000     ELSE MOVE W3 TO COMPUTED-X MOVE TBL-004-O (CTR-1) TO         NC1254.2
046100         CORRECT-X                                                NC1254.2
046200     PERFORM FAIL.                                                NC1254.2
046300     PERFORM PRINT-DETAIL.                                        NC1254.2
046400 EDI-TEST-GF3-EXIT.                                               NC1254.2
046500     EXIT.                                                        NC1254.2
046600 EDI-TEST-GF4.                                                    NC1254.2
046700     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
046800     MOVE "EDI-TEST-GF4" TO PAR-NAME.                             NC1254.2
046900     MOVE "* EDIT MOVE" TO FEATURE.                               NC1254.2
047000     MOVE 0 TO REC-CT.                                            NC1254.2
047100     MOVE 0 TO CTR-1.                                             NC1254.2
047200     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
047300     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
047400     PERFORM EDI-TEST-GF4-R CRT-2 TIMES                           NC1254.2
047500     GO TO EDI-TEST-GF4-EXIT.                                     NC1254.2
047600 EDI-TEST-GF4-DELETE.                                             NC1254.2
047700     PERFORM  DE-LETE.                                            NC1254.2
047800     PERFORM  PRINT-DETAIL.                                       NC1254.2
047900     GO TO    EDI-TEST-GF4-EXIT.                                  NC1254.2
048000 EDI-TEST-GF4-R.                                                  NC1254.2
048100     ADD 1 TO REC-CT.                                             NC1254.2
048200     ADD 1 TO CTR-1.                                              NC1254.2
048300     MOVE TBL-001-O (CTR-1) TO WRK-EDIT-004.                      NC1254.2
048400     IF WRK-EDIT-004 EQUAL TO TBL-005-O (CTR-1) PERFORM PASS      NC1254.2
048500     ELSE MOVE W4 TO COMPUTED-X MOVE TBL-005-O (CTR-1) TO         NC1254.2
048600         CORRECT-X                                                NC1254.2
048700     PERFORM FAIL.                                                NC1254.2
048800     PERFORM PRINT-DETAIL.                                        NC1254.2
048900 EDI-TEST-GF4-EXIT.                                               NC1254.2
049000     EXIT.                                                        NC1254.2
049100 EDI-TEST-GF5.                                                    NC1254.2
049200     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
049300     MOVE "EDI-TEST-GF5" TO PAR-NAME.                             NC1254.2
049400     MOVE "l EDIT ADD" TO FEATURE.                                NC1254.2
049500     MOVE 0 TO CTR-1.                                             NC1254.2
049600     MOVE 0 TO CTR-3.                                             NC1254.2
049700     MOVE 0 TO REC-CT.                                            NC1254.2
049800     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
049900     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
050000     PERFORM EDI-TEST-GF5-R CRT-2 TIMES                           NC1254.2
050100     GO TO EDI-TEST-GF5-EXIT.                                     NC1254.2
050200 EDI-TEST-GF5-DELETE.                                             NC1254.2
050300     PERFORM  DE-LETE.                                            NC1254.2
050400     PERFORM  PRINT-DETAIL.                                       NC1254.2
050500     GO TO    EDI-TEST-GF5-EXIT.                                  NC1254.2
050600 EDI-TEST-GF5-R.                                                  NC1254.2
050700     ADD 1 TO REC-CT.                                             NC1254.2
050800     ADD 1 TO CTR-1.                                              NC1254.2
050900     ADD TBL-001-O (CTR-1)  CTR-3 GIVING WRK-EDIT-001.            NC1254.2
051000     IF WRK-EDIT-001 EQUAL TO TBL-002-O (CTR-1) PERFORM PASS      NC1254.2
051100     ELSE MOVE W1 TO COMPUTED-X MOVE TBL-002-O (CTR-1) TO         NC1254.2
051200         CORRECT-X                                                NC1254.2
051300     PERFORM FAIL.                                                NC1254.2
051400     PERFORM PRINT-DETAIL.                                        NC1254.2
051500 EDI-TEST-GF5-EXIT.                                               NC1254.2
051600     EXIT.                                                        NC1254.2
051700 EDI-TEST-GF6.                                                    NC1254.2
051800     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
051900     MOVE "EDI-TEST-GF6" TO PAR-NAME.                             NC1254.2
052000     MOVE "+ EDIT ADD" TO FEATURE.                                NC1254.2
052100     MOVE 0 TO CTR-1.                                             NC1254.2
052200     MOVE 0 TO CTR-3.                                             NC1254.2
052300     MOVE 0 TO REC-CT.                                            NC1254.2
052400     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
052500     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
052600     PERFORM EDI-TEST-GF6-R CRT-2 TIMES                           NC1254.2
052700     GO TO EDI-TEST-GF6-EXIT.                                     NC1254.2
052800 EDI-TEST-GF6-DELETE.                                             NC1254.2
052900     PERFORM  DE-LETE.                                            NC1254.2
053000     PERFORM  PRINT-DETAIL.                                       NC1254.2
053100     GO TO    EDI-TEST-GF6-EXIT.                                  NC1254.2
053200 EDI-TEST-GF6-R.                                                  NC1254.2
053300     ADD 1 TO REC-CT.                                             NC1254.2
053400     ADD 1 TO CTR-1.                                              NC1254.2
053500     ADD TBL-001-O (CTR-1)  CTR-3 GIVING WRK-EDIT-002.            NC1254.2
053600     IF WRK-EDIT-002 EQUAL TO TBL-003-O (CTR-1) PERFORM PASS      NC1254.2
053700     ELSE MOVE W2 TO COMPUTED-X MOVE TBL-003-O (CTR-1) TO         NC1254.2
053800         CORRECT-X                                                NC1254.2
053900     PERFORM FAIL.                                                NC1254.2
054000     PERFORM PRINT-DETAIL.                                        NC1254.2
054100 EDI-TEST-GF6-EXIT.                                               NC1254.2
054200     EXIT.                                                        NC1254.2
054300 EDI-TEST-GF7.                                                    NC1254.2
054400     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
054500     MOVE "EDI-TEST-GF7" TO PAR-NAME.                             NC1254.2
054600     MOVE "- EDIT ADD" TO FEATURE.                                NC1254.2
054700     MOVE 0 TO CTR-1.                                             NC1254.2
054800     MOVE 0 TO CTR-3.                                             NC1254.2
054900     MOVE 0 TO REC-CT.                                            NC1254.2
055000     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
055100     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
055200     PERFORM EDI-TEST-GF7-R CRT-2 TIMES                           NC1254.2
055300     GO TO EDI-TEST-GF7-EXIT.                                     NC1254.2
055400 EDI-TEST-GF7-DELETE.                                             NC1254.2
055500     PERFORM  DE-LETE.                                            NC1254.2
055600     PERFORM  PRINT-DETAIL.                                       NC1254.2
055700     GO TO    EDI-TEST-GF7-EXIT.                                  NC1254.2
055800 EDI-TEST-GF7-R.                                                  NC1254.2
055900     ADD 1 TO REC-CT.                                             NC1254.2
056000     ADD 1 TO CTR-1.                                              NC1254.2
056100     ADD TBL-001-O (CTR-1)  CTR-3 GIVING WRK-EDIT-003.            NC1254.2
056200     IF WRK-EDIT-003 EQUAL TO TBL-004-O (CTR-1) PERFORM PASS      NC1254.2
056300     ELSE MOVE W3 TO COMPUTED-X MOVE TBL-004-O (CTR-1) TO         NC1254.2
056400         CORRECT-X                                                NC1254.2
056500     PERFORM FAIL.                                                NC1254.2
056600     PERFORM PRINT-DETAIL.                                        NC1254.2
056700 EDI-TEST-GF7-EXIT.                                               NC1254.2
056800     EXIT.                                                        NC1254.2
056900 EDI-TEST-GF8.                                                    NC1254.2
057000     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
057100     MOVE "EDI-TEST-GF8" TO PAR-NAME.                             NC1254.2
057200     MOVE "* EDIT ADD" TO FEATURE.                                NC1254.2
057300     MOVE 0 TO CTR-1.                                             NC1254.2
057400     MOVE 0 TO CTR-3.                                             NC1254.2
057500     MOVE 0 TO REC-CT.                                            NC1254.2
057600     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
057700     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
057800     PERFORM EDI-TEST-GF8-R CRT-2 TIMES                           NC1254.2
057900     GO TO EDI-TEST-GF8-EXIT.                                     NC1254.2
058000 EDI-TEST-GF8-DELETE.                                             NC1254.2
058100     PERFORM  DE-LETE.                                            NC1254.2
058200     PERFORM  PRINT-DETAIL.                                       NC1254.2
058300     GO TO    EDI-TEST-GF8-EXIT.                                  NC1254.2
058400 EDI-TEST-GF8-R.                                                  NC1254.2
058500     ADD 1 TO REC-CT.                                             NC1254.2
058600     ADD 1 TO CTR-1.                                              NC1254.2
058700     ADD TBL-001-O (CTR-1)  CTR-3 GIVING WRK-EDIT-004.            NC1254.2
058800     IF WRK-EDIT-004 EQUAL TO TBL-005-O (CTR-1) PERFORM PASS      NC1254.2
058900     ELSE MOVE W4 TO COMPUTED-X MOVE TBL-005-O (CTR-1) TO         NC1254.2
059000         CORRECT-X                                                NC1254.2
059100     PERFORM FAIL.                                                NC1254.2
059200     PERFORM PRINT-DETAIL.                                        NC1254.2
059300 EDI-TEST-GF8-EXIT.                                               NC1254.2
059400     EXIT.                                                        NC1254.2
059500 EDI-TEST-GF9.                                                    NC1254.2
059600     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
059700     MOVE "EDI-TEST-GF9" TO PAR-NAME.                             NC1254.2
059800     MOVE "l EDIT SUB" TO FEATURE.                                NC1254.2
059900     MOVE 0 TO CTR-1.                                             NC1254.2
060000     MOVE 0 TO CTR-3.                                             NC1254.2
060100     MOVE 0 TO REC-CT.                                            NC1254.2
060200     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
060300     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
060400     PERFORM EDI-TEST-GF9-R CRT-2 TIMES                           NC1254.2
060500     GO TO EDI-TEST-GF9-EXIT.                                     NC1254.2
060600 EDI-TEST-GF9-DELETE.                                             NC1254.2
060700     PERFORM  DE-LETE.                                            NC1254.2
060800     PERFORM  PRINT-DETAIL.                                       NC1254.2
060900     GO TO    EDI-TEST-GF9-EXIT.                                  NC1254.2
061000 EDI-TEST-GF9-R.                                                  NC1254.2
061100     ADD 1 TO REC-CT.                                             NC1254.2
061200     ADD 1 TO CTR-1.                                              NC1254.2
061300     SUBTRACT TBL-001-O (CTR-1) FROM CTR-3 GIVING WRK-EDIT-001.   NC1254.2
061400     IF WRK-EDIT-001 EQUAL TO TBL-002-O (CTR-1) PERFORM PASS      NC1254.2
061500     ELSE MOVE W1 TO COMPUTED-X MOVE TBL-002-O (CTR-1) TO         NC1254.2
061600         CORRECT-X                                                NC1254.2
061700     PERFORM FAIL.                                                NC1254.2
061800     PERFORM PRINT-DETAIL.                                        NC1254.2
061900 EDI-TEST-GF9-EXIT.                                               NC1254.2
062000     EXIT.                                                        NC1254.2
062100 EDI-TEST-GF10.                                                   NC1254.2
062200     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
062300     MOVE "EDI-TEST-GF10" TO PAR-NAME.                            NC1254.2
062400     MOVE "+ EDIT SUB" TO FEATURE.                                NC1254.2
062500     MOVE 0 TO CTR-1.                                             NC1254.2
062600     MOVE 0 TO CTR-3.                                             NC1254.2
062700     MOVE 0 TO REC-CT.                                            NC1254.2
062800     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
062900     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
063000     MOVE "                     +.00" TO TBL-006-O (1).           NC1254.2
063100     PERFORM EDI-TEST-GF10-R CRT-2 TIMES                          NC1254.2
063200     GO TO EDI-TEST-GF10-EXIT.                                    NC1254.2
063300 EDI-TEST-GF10-DELETE.                                            NC1254.2
063400     PERFORM  DE-LETE.                                            NC1254.2
063500     PERFORM  PRINT-DETAIL.                                       NC1254.2
063600     GO TO    EDI-TEST-GF10-EXIT.                                 NC1254.2
063700 EDI-TEST-GF10-R.                                                 NC1254.2
063800     ADD 1 TO REC-CT.                                             NC1254.2
063900     ADD 1 TO CTR-1.                                              NC1254.2
064000     SUBTRACT TBL-001-O (CTR-1) FROM CTR-3 GIVING WRK-EDIT-002.   NC1254.2
064100     IF WRK-EDIT-002 EQUAL TO TBL-006-O (CTR-1) PERFORM PASS      NC1254.2
064200     ELSE MOVE W2 TO COMPUTED-X MOVE TBL-006-O (CTR-1) TO         NC1254.2
064300         CORRECT-X                                                NC1254.2
064400     PERFORM FAIL.                                                NC1254.2
064500     PERFORM PRINT-DETAIL.                                        NC1254.2
064600 EDI-TEST-GF10-EXIT.                                              NC1254.2
064700     EXIT.                                                        NC1254.2
064800 EDI-TEST-GF11.                                                   NC1254.2
064900     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
065000     MOVE "EDI-TEST-GF11" TO PAR-NAME.                            NC1254.2
065100     MOVE "- EDIT SUB" TO FEATURE.                                NC1254.2
065200     MOVE 0 TO CTR-1.                                             NC1254.2
065300     MOVE 0 TO CTR-3.                                             NC1254.2
065400     MOVE 0 TO REC-CT.                                            NC1254.2
065500     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
065600     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
065700     MOVE "                      .00" TO TBL-006-O (1).           NC1254.2
065800     PERFORM EDI-TEST-GF11-R CRT-2 TIMES                          NC1254.2
065900     GO TO EDI-TEST-GF11-EXIT.                                    NC1254.2
066000 EDI-TEST-GF11-DELETE.                                            NC1254.2
066100     PERFORM  DE-LETE.                                            NC1254.2
066200     PERFORM  PRINT-DETAIL.                                       NC1254.2
066300     GO TO    EDI-TEST-GF11-EXIT.                                 NC1254.2
066400 EDI-TEST-GF11-R.                                                 NC1254.2
066500     ADD 1 TO REC-CT.                                             NC1254.2
066600     ADD 1 TO CTR-1.                                              NC1254.2
066700     SUBTRACT TBL-001-O (CTR-1) FROM CTR-3 GIVING WRK-EDIT-003.   NC1254.2
066800     IF WRK-EDIT-003 EQUAL TO TBL-006-O (CTR-1) PERFORM PASS      NC1254.2
066900     ELSE MOVE W3 TO COMPUTED-X MOVE TBL-006-O (CTR-1) TO         NC1254.2
067000         CORRECT-X                                                NC1254.2
067100     PERFORM FAIL.                                                NC1254.2
067200     PERFORM PRINT-DETAIL.                                        NC1254.2
067300 EDI-TEST-GF11-EXIT.                                              NC1254.2
067400     EXIT.                                                        NC1254.2
067500 EDI-TEST-GF12.                                                   NC1254.2
067600     MOVE "VI-34 5.9.5 (7), (8)" TO ANSI-REFERENCE.               NC1254.2
067700     MOVE "EDI-TEST-GF12" TO PAR-NAME.                            NC1254.2
067800     MOVE "* EDIT SUB" TO FEATURE.                                NC1254.2
067900     MOVE 0 TO CTR-1.                                             NC1254.2
068000     MOVE 0 TO CTR-3.                                             NC1254.2
068100     MOVE 0 TO REC-CT.                                            NC1254.2
068200     MOVE ZERO TO TBL-001-O (1).                                  NC1254.2
068300     MOVE .01  TO TBL-001-O (2).                                  NC1254.2
068400     PERFORM EDI-TEST-GF12-R CRT-2 TIMES                          NC1254.2
068500     GO TO EDI-TEST-GF12-EXIT.                                    NC1254.2
068600 EDI-TEST-GF12-DELETE.                                            NC1254.2
068700     PERFORM  DE-LETE.                                            NC1254.2
068800     PERFORM  PRINT-DETAIL.                                       NC1254.2
068900     GO TO    EDI-TEST-GF12-EXIT.                                 NC1254.2
069000 EDI-TEST-GF12-R.                                                 NC1254.2
069100     ADD 1 TO REC-CT.                                             NC1254.2
069200     ADD 1 TO CTR-1.                                              NC1254.2
069300     SUBTRACT TBL-001-O (CTR-1) FROM CTR-3 GIVING WRK-EDIT-004.   NC1254.2
069400     IF WRK-EDIT-004 EQUAL TO TBL-005-O (CTR-1) PERFORM PASS      NC1254.2
069500     ELSE MOVE W4 TO COMPUTED-X MOVE TBL-005-O (CTR-1) TO         NC1254.2
069600         CORRECT-X                                                NC1254.2
069700     PERFORM FAIL.                                                NC1254.2
069800     PERFORM PRINT-DETAIL.                                        NC1254.2
069900 EDI-TEST-GF12-EXIT.                                              NC1254.2
070000     EXIT.                                                        NC1254.2
070100*                                                                 NC1254.2
070200 EDI-INIT-GF-13.                                                  NC1254.2
070300     MOVE   "VI-33 5.9.5 (4), (5)" TO ANSI-REFERENCE.             NC1254.2
070400     MOVE   "EDI-TEST-GF-13"       TO PAR-NAME.                   NC1254.2
070500     MOVE   "COMMA AS LAST SYMBOL" TO FEATURE.                    NC1254.2
070600 EDI-TEST-GF-13-0.                                                NC1254.2
070700     MOVE    123456789012 TO WRK-EDIT-005.                        NC1254.2
070800 EDI-TEST-GF-13-1.                                                NC1254.2
070900     IF      WRK-EDIT-005 = "1,2,3,4,5,6,7,8,9,0,1,2,"            NC1254.2
071000             PERFORM PASS                                         NC1254.2
071100     ELSE                                                         NC1254.2
071200             GO TO   EDI-FAIL-GF-13.                              NC1254.2
071300     GO TO   EDI-WRITE-GF-13.                                     NC1254.2
071400 EDI-DELETE-GF-13.                                                NC1254.2
071500     PERFORM DE-LETE.                                             NC1254.2
071600     PERFORM PRINT-DETAIL.                                        NC1254.2
071700     GO TO   EDI-INIT-GF-14.                                      NC1254.2
071800 EDI-FAIL-GF-13.                                                  NC1254.2
071900     MOVE   "1,2,3,4,5,6,7,8,9,0,1,2," TO CORRECT-X.              NC1254.2
072000     MOVE    W5 TO COMPUTED-X.                                    NC1254.2
072100     PERFORM FAIL.                                                NC1254.2
072200 EDI-WRITE-GF-13.                                                 NC1254.2
072300     PERFORM PRINT-DETAIL.                                        NC1254.2
072400*                                                                 NC1254.2
072500 EDI-INIT-GF-14.                                                  NC1254.2
072600     MOVE   "VI-34 5.9.5 (4), (5)" TO ANSI-REFERENCE.             NC1254.2
072700     MOVE   "EDI-TEST-GF-14"       TO PAR-NAME.                   NC1254.2
072800     MOVE   "PERIOD LAST SYMBOL"   TO FEATURE.                    NC1254.2
072900 EDI-TEST-GF-14-0.                                                NC1254.2
073000     MOVE    123456789012 TO WRK-EDIT-006.                        NC1254.2
073100 EDI-TEST-GF-14-1.                                                NC1254.2
073200     IF      WRK-EDIT-006 = "123456789012."                       NC1254.2
073300             PERFORM PASS                                         NC1254.2
073400     ELSE                                                         NC1254.2
073500             GO TO   EDI-FAIL-GF-14.                              NC1254.2
073600     GO TO   EDI-WRITE-GF-14.                                     NC1254.2
073700 EDI-DELETE-GF-14.                                                NC1254.2
073800     PERFORM DE-LETE.                                             NC1254.2
073900     PERFORM PRINT-DETAIL.                                        NC1254.2
074000     GO TO   EDI-INIT-GF-14.                                      NC1254.2
074100 EDI-FAIL-GF-14.                                                  NC1254.2
074200     MOVE   "123456789012." TO CORRECT-X.                         NC1254.2
074300     MOVE    W6 TO COMPUTED-X.                                    NC1254.2
074400     PERFORM FAIL.                                                NC1254.2
074500 EDI-WRITE-GF-14.                                                 NC1254.2
074600     PERFORM PRINT-DETAIL.                                        NC1254.2
074700*                                                                 NC1254.2
074800 CCVS-EXIT SECTION.                                               NC1254.2
074900 CCVS-999999.                                                     NC1254.2
075000     GO TO CLOSE-FILES.                                           NC1254.2
      *END-OF,NC125A                                                                  
