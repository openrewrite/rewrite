      *HEADER,COBOL,NC117A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1174.2
000200 PROGRAM-ID.                                                      NC1174.2
000300     NC117A.                                                      NC1174.2
000400****************************************************************  NC1174.2
000500*                                                              *  NC1174.2
000600*    VALIDATION FOR:-                                          *  NC1174.2
000700*                                                              *  NC1174.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1174.2
000900*                                                              *  NC1174.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1174.2
001100*                                                              *  NC1174.2
001200****************************************************************  NC1174.2
001300*                                                              *  NC1174.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1174.2
001500*                                                              *  NC1174.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1174.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1174.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1174.2
001900*                                                              *  NC1174.2
002000****************************************************************  NC1174.2
002100*                                                                 NC1174.2
002200*      PROGRAM NC117A TESTS THE USE OF THE "SIGN" CLAUSE USING    NC1174.2
002300*      THE "DIVIDE" STATEMENT.  ALL COMBINATIONS OF THE "SIGN"    NC1174.2
002400*      CLAUSE PHRASES ARE TESTED USING DATA ITEMS OF              NC1174.2
002500*      LENGTHS.                                                   NC1174.2
002600*                                                                 NC1174.2
002700*                                                                 NC1174.2
002800                                                                  NC1174.2
002900                                                                  NC1174.2
003000 ENVIRONMENT DIVISION.                                            NC1174.2
003100 CONFIGURATION SECTION.                                           NC1174.2
003200 SOURCE-COMPUTER.                                                 NC1174.2
003300     XXXXX082.                                                    NC1174.2
003400 OBJECT-COMPUTER.                                                 NC1174.2
003500     XXXXX083.                                                    NC1174.2
003600 INPUT-OUTPUT SECTION.                                            NC1174.2
003700 FILE-CONTROL.                                                    NC1174.2
003800     SELECT PRINT-FILE ASSIGN TO                                  NC1174.2
003900     XXXXX055.                                                    NC1174.2
004000 DATA DIVISION.                                                   NC1174.2
004100 FILE SECTION.                                                    NC1174.2
004200 FD  PRINT-FILE.                                                  NC1174.2
004300 01  PRINT-REC PICTURE X(120).                                    NC1174.2
004400 01  DUMMY-RECORD PICTURE X(120).                                 NC1174.2
004500 WORKING-STORAGE SECTION.                                         NC1174.2
004600 77  WRK-DS-LS-18V00                PICTURE S9(18)                NC1174.2
004700           SIGN IS LEADING SEPARATE CHARACTER.                    NC1174.2
004800 77  A06THREES-DS-LS-03V03          PICTURE S999V999 VALUE 333.333NC1174.2
004900           SIGN IS LEADING.                                       NC1174.2
005000 77  WRK-DS-TS-06V06                PICTURE S9(6)V9(6)            NC1174.2
005100           SIGN IS TRAILING SEPARATE CHARACTER.                   NC1174.2
005200 77  WRK-DS-TS-12V00-S-S REDEFINES WRK-DS-TS-06V06 PICTURE S9(12) NC1174.2
005300           SIGN TRAILING SEPARATE.                                NC1174.2
005400 77  A08TWOS-DS-02V06            PICTURE S99V9(6) VALUE 22.222222.NC1174.2
005500 77  WRK-DS-10V00                PICTURE S9(10).                  NC1174.2
005600 77  WRK-XN-00001                PICTURE X.                       NC1174.2
005700 77  A10ONES-DS-T-10V00            PICTURE S9(10)                 NC1174.2
005800         SIGN IS TRAILING                                         NC1174.2
005900                                 VALUE 1111111111.                NC1174.2
006000 77  A12THREES-DS-LS-06V06          PICTURE S9(6)V9(6)            NC1174.2
006100             SIGN IS LEADING SEPARATE                             NC1174.2
006200                                 VALUE 333333.333333.             NC1174.2
006300 77  WRK-DS-02V00                PICTURE S99.                     NC1174.2
006400 77  AZERO-DS-LS-05V05              PICTURE S9(5)V9(5) VALUE ZERO NC1174.2
006500           SIGN IS LEADING SEPARATE CHARACTER.                    NC1174.2
006600 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1174.2
006700 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC1174.2
006800 77  A05ONES-DS-LS-00V05            PICTURE SV9(5) VALUE .11111   NC1174.2
006900           SIGN IS LEADING SEPARATE CHARACTER.                    NC1174.2
007000 77  A12ONES-DS-12V00            PICTURE S9(12)                   NC1174.2
007100                                 VALUE 111111111111.              NC1174.2
007200 77  A01ONE-DS-TS-P0801            PICTURE SP(8)9 VALUE .000000001NC1174.2
007300            SIGN IS TRAILING SEPARATE.                            NC1174.2
007400 77  WRK-DS-T-09V08                PICTURE S9(9)V9(8)             NC1174.2
007500           SIGN IS TRAILING.                                      NC1174.2
007600 77  WKR-DS-T-17V00-S REDEFINES WRK-DS-T-09V08 PICTURE S9(17)     NC1174.2
007700         SIGN TRAILING.                                           NC1174.2
007800 77  A18ONES-DS-18V00            PICTURE S9(18)                   NC1174.2
007900                                 VALUE 111111111111111111.        NC1174.2
008000 77  WRK-DS-LS-0201P                PICTURE S99P                  NC1174.2
008100            SIGN IS LEADING SEPARATE.                             NC1174.2
008200 77  WRK-CS-18V00                PICTURE S9(18) COMPUTATIONAL.    NC1174.2
008300 77  WRK-DU-18V00                PICTURE 9(18).                   NC1174.2
008400 77  A99-CS-02V00                PICTURE S99 COMPUTATIONAL        NC1174.2
008500                                 VALUE 99.                        NC1174.2
008600 77  A01ONE-CS-00V01             PICTURE SV9 COMPUTATIONAL        NC1174.2
008700                                 VALUE .1.                        NC1174.2
008800 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC1174.2
008900 77  WRK-DS-TS-12V00-S           PICTURE S9(12)                   NC1174.2
009000             SIGN IS TRAILING SEPARATE CHARACTER.                 NC1174.2
009100 77  WRK-DS-LS-01V00             PICTURE S9 LEADING SEPARATE.     NC1174.2
009200 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC1174.2
009300 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC1174.2
009400             LEADING SEPARATE                                     NC1174.2
009500                                 VALUE 111111111.111111111.       NC1174.2
009600 77  A02TWOS-DU-02V00            PICTURE 99  VALUE 22.            NC1174.2
009700 77  WRK-DS-05V00                PICTURE S9(5).                   NC1174.2
009800 77  A02TWOS-DS-03V02            PICTURE S999V99 VALUE +022.00.   NC1174.2
009900 77  WRK-CS-02V02                PICTURE S99V99  COMPUTATIONAL.   NC1174.2
010000 77  A990-DS-0201P               PICTURE S99P   VALUE +990.       NC1174.2
010100 77  XRAY                        PICTURE X.                       NC1174.2
010200 77  A01ONES-CS-18V00   PICTURE S9(18) COMPUTATIONAL              NC1174.2
010300             VALUE +000000000000000001.                           NC1174.2
010400 77  A02THREES-CS-18V00 PICTURE S9(18) COMPUTATIONAL              NC1174.2
010500             VALUE -000000000000000033.                           NC1174.2
010600 77  A18SIXES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1174.2
010700             VALUE 666666666666666666.                            NC1174.2
010800 77  A16NINES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1174.2
010900             VALUE 009999999999999999.                            NC1174.2
011000 77  A14TWOS-CU-18V00   PICTURE 9(18) COMPUTATIONAL               NC1174.2
011100             VALUE 000022222222222222.                            NC1174.2
011200 01  MULTIPLY-DATA LEADING SEPARATE.                              NC1174.2
011300     02 MULT1                           PICTURE IS 999V99         NC1174.2
011400     VALUE IS 80.12.                                              NC1174.2
011500     02 MULT2                           PICTURE IS 999V999.       NC1174.2
011600     02 MULT3                           PICTURE IS $$99.99.       NC1174.2
011700     02 MULT4                           PICTURE IS S99            NC1174.2
011800     VALUE IS -56.                                                NC1174.2
011900     02 MULT5                           PICTURE IS 9 VALUE IS 4.  NC1174.2
012000     02 MULT6                           PICTURE IS 99 VALUE IS    NC1174.2
012100     20.                                                          NC1174.2
012200 01  DIVIDE-DATA TRAILING SEPARATE.                               NC1174.2
012300     02 DIV1                            PICTURE IS 9(4)V99        NC1174.2
012400     VALUE IS 1620.36.                                            NC1174.2
012500     02 DIV2                            PICTURE IS 99V9           NC1174.2
012600     VALUE IS 44.1.                                               NC1174.2
012700     02 DIV3                            PICTURE IS 9(4)V9         NC1174.2
012800     VALUE IS 1661.7.                                             NC1174.2
012900     02 DIV4                            PICTURE IS S9V999         NC1174.2
013000     VALUE IS -9.642.                                             NC1174.2
013100     02 SIG-02LEVEL-1.                                            NC1174.2
013200     03 DIV5                            PICTURE IS V99            NC1174.2
013300     VALUE IS .82.                                                NC1174.2
013400     03 DIV6                            PICTURE IS 9 VALUE IS 0.  NC1174.2
013500     03 DIV7                            PICTURE IS 9V9            NC1174.2
013600     VALUE IS 9.6.                                                NC1174.2
013700 01  SIG-DATA-2.                                                  NC1174.2
013800     02 DIV8                            PICTURE IS 99V9.          NC1174.2
013900     02 DIV9                            PICTURE IS ZZ,ZZZ.9.      NC1174.2
014000     02 DIV10                           PICTURE IS V999.          NC1174.2
014100 01  TEST-RESULTS.                                                NC1174.2
014200     02 FILLER                   PIC X      VALUE SPACE.          NC1174.2
014300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1174.2
014400     02 FILLER                   PIC X      VALUE SPACE.          NC1174.2
014500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1174.2
014600     02 FILLER                   PIC X      VALUE SPACE.          NC1174.2
014700     02  PAR-NAME.                                                NC1174.2
014800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1174.2
014900       03  PARDOT-X              PIC X      VALUE SPACE.          NC1174.2
015000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1174.2
015100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1174.2
015200     02 RE-MARK                  PIC X(61).                       NC1174.2
015300 01  TEST-COMPUTED.                                               NC1174.2
015400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1174.2
015500     02 FILLER                   PIC X(17)  VALUE                 NC1174.2
015600            "       COMPUTED=".                                   NC1174.2
015700     02 COMPUTED-X.                                               NC1174.2
015800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1174.2
015900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1174.2
016000                                 PIC -9(9).9(9).                  NC1174.2
016100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1174.2
016200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1174.2
016300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1174.2
016400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1174.2
016500         04 COMPUTED-18V0                    PIC -9(18).          NC1174.2
016600         04 FILLER                           PIC X.               NC1174.2
016700     03 FILLER PIC X(50) VALUE SPACE.                             NC1174.2
016800 01  TEST-CORRECT.                                                NC1174.2
016900     02 FILLER PIC X(30) VALUE SPACE.                             NC1174.2
017000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1174.2
017100     02 CORRECT-X.                                                NC1174.2
017200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1174.2
017300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1174.2
017400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1174.2
017500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1174.2
017600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1174.2
017700     03      CR-18V0 REDEFINES CORRECT-A.                         NC1174.2
017800         04 CORRECT-18V0                     PIC -9(18).          NC1174.2
017900         04 FILLER                           PIC X.               NC1174.2
018000     03 FILLER PIC X(2) VALUE SPACE.                              NC1174.2
018100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1174.2
018200 01  CCVS-C-1.                                                    NC1174.2
018300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1174.2
018400-    "SS  PARAGRAPH-NAME                                          NC1174.2
018500-    "       REMARKS".                                            NC1174.2
018600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1174.2
018700 01  CCVS-C-2.                                                    NC1174.2
018800     02 FILLER                     PIC X        VALUE SPACE.      NC1174.2
018900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1174.2
019000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1174.2
019100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1174.2
019200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1174.2
019300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1174.2
019400 01  REC-CT                        PIC 99       VALUE ZERO.       NC1174.2
019500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1174.2
019600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1174.2
019700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1174.2
019800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1174.2
019900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1174.2
020000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1174.2
020100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1174.2
020200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1174.2
020300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1174.2
020400 01  CCVS-H-1.                                                    NC1174.2
020500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1174.2
020600     02  FILLER                    PIC X(42)    VALUE             NC1174.2
020700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1174.2
020800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1174.2
020900 01  CCVS-H-2A.                                                   NC1174.2
021000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1174.2
021100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1174.2
021200   02  FILLER                        PIC XXXX   VALUE             NC1174.2
021300     "4.2 ".                                                      NC1174.2
021400   02  FILLER                        PIC X(28)  VALUE             NC1174.2
021500            " COPY - NOT FOR DISTRIBUTION".                       NC1174.2
021600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1174.2
021700                                                                  NC1174.2
021800 01  CCVS-H-2B.                                                   NC1174.2
021900   02  FILLER                        PIC X(15)  VALUE             NC1174.2
022000            "TEST RESULT OF ".                                    NC1174.2
022100   02  TEST-ID                       PIC X(9).                    NC1174.2
022200   02  FILLER                        PIC X(4)   VALUE             NC1174.2
022300            " IN ".                                               NC1174.2
022400   02  FILLER                        PIC X(12)  VALUE             NC1174.2
022500     " HIGH       ".                                              NC1174.2
022600   02  FILLER                        PIC X(22)  VALUE             NC1174.2
022700            " LEVEL VALIDATION FOR ".                             NC1174.2
022800   02  FILLER                        PIC X(58)  VALUE             NC1174.2
022900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1174.2
023000 01  CCVS-H-3.                                                    NC1174.2
023100     02  FILLER                      PIC X(34)  VALUE             NC1174.2
023200            " FOR OFFICIAL USE ONLY    ".                         NC1174.2
023300     02  FILLER                      PIC X(58)  VALUE             NC1174.2
023400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1174.2
023500     02  FILLER                      PIC X(28)  VALUE             NC1174.2
023600            "  COPYRIGHT   1985 ".                                NC1174.2
023700 01  CCVS-E-1.                                                    NC1174.2
023800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1174.2
023900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1174.2
024000     02 ID-AGAIN                     PIC X(9).                    NC1174.2
024100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1174.2
024200 01  CCVS-E-2.                                                    NC1174.2
024300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1174.2
024400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1174.2
024500     02 CCVS-E-2-2.                                               NC1174.2
024600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1174.2
024700         03 FILLER                   PIC X      VALUE SPACE.      NC1174.2
024800         03 ENDER-DESC               PIC X(44)  VALUE             NC1174.2
024900            "ERRORS ENCOUNTERED".                                 NC1174.2
025000 01  CCVS-E-3.                                                    NC1174.2
025100     02  FILLER                      PIC X(22)  VALUE             NC1174.2
025200            " FOR OFFICIAL USE ONLY".                             NC1174.2
025300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1174.2
025400     02  FILLER                      PIC X(58)  VALUE             NC1174.2
025500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1174.2
025600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1174.2
025700     02 FILLER                       PIC X(15)  VALUE             NC1174.2
025800             " COPYRIGHT 1985".                                   NC1174.2
025900 01  CCVS-E-4.                                                    NC1174.2
026000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1174.2
026100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1174.2
026200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1174.2
026300     02 FILLER                       PIC X(40)  VALUE             NC1174.2
026400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1174.2
026500 01  XXINFO.                                                      NC1174.2
026600     02 FILLER                       PIC X(19)  VALUE             NC1174.2
026700            "*** INFORMATION ***".                                NC1174.2
026800     02 INFO-TEXT.                                                NC1174.2
026900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1174.2
027000       04 XXCOMPUTED                 PIC X(20).                   NC1174.2
027100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1174.2
027200       04 XXCORRECT                  PIC X(20).                   NC1174.2
027300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1174.2
027400 01  HYPHEN-LINE.                                                 NC1174.2
027500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1174.2
027600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1174.2
027700-    "*****************************************".                 NC1174.2
027800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1174.2
027900-    "******************************".                            NC1174.2
028000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1174.2
028100     "NC117A".                                                    NC1174.2
028200 PROCEDURE DIVISION.                                              NC1174.2
028300 CCVS1 SECTION.                                                   NC1174.2
028400 OPEN-FILES.                                                      NC1174.2
028500     OPEN     OUTPUT PRINT-FILE.                                  NC1174.2
028600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1174.2
028700     MOVE    SPACE TO TEST-RESULTS.                               NC1174.2
028800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1174.2
028900     GO TO CCVS1-EXIT.                                            NC1174.2
029000 CLOSE-FILES.                                                     NC1174.2
029100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1174.2
029200 TERMINATE-CCVS.                                                  NC1174.2
029300*    EXIT PROGRAM.                                                NC1174.2
029400*TERMINATE-CALL.                                                  NC1174.2
029500     STOP     RUN.                                                NC1174.2
029600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1174.2
029700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1174.2
029800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1174.2
029900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1174.2
030000     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1174.2
030100 PRINT-DETAIL.                                                    NC1174.2
030200     IF REC-CT NOT EQUAL TO ZERO                                  NC1174.2
030300             MOVE "." TO PARDOT-X                                 NC1174.2
030400             MOVE REC-CT TO DOTVALUE.                             NC1174.2
030500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1174.2
030600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1174.2
030700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1174.2
030800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1174.2
030900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1174.2
031000     MOVE SPACE TO CORRECT-X.                                     NC1174.2
031100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1174.2
031200     MOVE     SPACE TO RE-MARK.                                   NC1174.2
031300 HEAD-ROUTINE.                                                    NC1174.2
031400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1174.2
031500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1174.2
031600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1174.2
031700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1174.2
031800 COLUMN-NAMES-ROUTINE.                                            NC1174.2
031900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1174.2
032000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1174.2
032100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1174.2
032200 END-ROUTINE.                                                     NC1174.2
032300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1174.2
032400 END-RTN-EXIT.                                                    NC1174.2
032500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1174.2
032600 END-ROUTINE-1.                                                   NC1174.2
032700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1174.2
032800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1174.2
032900      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1174.2
033000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1174.2
033100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1174.2
033200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1174.2
033300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1174.2
033400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1174.2
033500  END-ROUTINE-12.                                                 NC1174.2
033600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1174.2
033700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1174.2
033800         MOVE "NO " TO ERROR-TOTAL                                NC1174.2
033900         ELSE                                                     NC1174.2
034000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1174.2
034100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1174.2
034200     PERFORM WRITE-LINE.                                          NC1174.2
034300 END-ROUTINE-13.                                                  NC1174.2
034400     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1174.2
034500         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1174.2
034600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1174.2
034700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1174.2
034800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1174.2
034900      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1174.2
035000          MOVE "NO " TO ERROR-TOTAL                               NC1174.2
035100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1174.2
035200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1174.2
035300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1174.2
035400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1174.2
035500 WRITE-LINE.                                                      NC1174.2
035600     ADD 1 TO RECORD-COUNT.                                       NC1174.2
035700*    IF RECORD-COUNT GREATER 42                                   NC1174.2
035800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1174.2
035900*        MOVE SPACE TO DUMMY-RECORD                               NC1174.2
036000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1174.2
036100*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1174.2
036200*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1174.2
036300*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1174.2
036400*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1174.2
036500*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1174.2
036600*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1174.2
036700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1174.2
036800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1174.2
036900*        MOVE ZERO TO RECORD-COUNT.                               NC1174.2
037000     PERFORM WRT-LN.                                              NC1174.2
037100 WRT-LN.                                                          NC1174.2
037200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1174.2
037300     MOVE SPACE TO DUMMY-RECORD.                                  NC1174.2
037400 BLANK-LINE-PRINT.                                                NC1174.2
037500     PERFORM WRT-LN.                                              NC1174.2
037600 FAIL-ROUTINE.                                                    NC1174.2
037700     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1174.2
037800            GO TO FAIL-ROUTINE-WRITE.                             NC1174.2
037900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1174.2
038000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1174.2
038100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1174.2
038200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1174.2
038300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1174.2
038400     GO TO  FAIL-ROUTINE-EX.                                      NC1174.2
038500 FAIL-ROUTINE-WRITE.                                              NC1174.2
038600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1174.2
038700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1174.2
038800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1174.2
038900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1174.2
039000 FAIL-ROUTINE-EX. EXIT.                                           NC1174.2
039100 BAIL-OUT.                                                        NC1174.2
039200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1174.2
039300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1174.2
039400 BAIL-OUT-WRITE.                                                  NC1174.2
039500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1174.2
039600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1174.2
039700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1174.2
039800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1174.2
039900 BAIL-OUT-EX. EXIT.                                               NC1174.2
040000 CCVS1-EXIT.                                                      NC1174.2
040100     EXIT.                                                        NC1174.2
040200 SECT-NC117A-001 SECTION.                                         NC1174.2
040300 SIG-INIT-GF-1.                                                   NC1174.2
040400     MOVE   "DIVIDE INTO"  TO FEATURE.                            NC1174.2
040500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
040600     MOVE    1620.36 TO DIV1.                                     NC1174.2
040700 SIG-TEST-GF-1-0.                                                 NC1174.2
040800     DIVIDE   64.3 INTO DIV1.                                     NC1174.2
040900 SIG-TEST-GF-1-1.                                                 NC1174.2
041000     IF       DIV1 EQUAL TO 25.2                                  NC1174.2
041100              PERFORM PASS                                        NC1174.2
041200              ELSE                                                NC1174.2
041300              GO TO SIG-FAIL-GF-1.                                NC1174.2
041400     GO TO    SIG-WRITE-GF-1.                                     NC1174.2
041500 SIG-DELETE-GF-1.                                                 NC1174.2
041600     PERFORM  DE-LETE.                                            NC1174.2
041700     GO TO    SIG-WRITE-GF-1.                                     NC1174.2
041800 SIG-FAIL-GF-1.                                                   NC1174.2
041900     PERFORM  FAIL.                                               NC1174.2
042000     MOVE     DIV1 TO COMPUTED-N.                                 NC1174.2
042100     MOVE     +25.2 TO CORRECT-N.                                 NC1174.2
042200 SIG-WRITE-GF-1.                                                  NC1174.2
042300     MOVE "SIG-TEST-GF-1" TO PAR-NAME.                            NC1174.2
042400     PERFORM  PRINT-DETAIL.                                       NC1174.2
042500 SIG-INIT-GF-2.                                                   NC1174.2
042600     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
042700     MOVE    44.1 TO DIV2.                                        NC1174.2
042800     MOVE  1661.7 TO DIV3.                                        NC1174.2
042900 SIG-TEST-GF-2.                                                   NC1174.2
043000     DIVIDE   DIV2 INTO DIV3 ROUNDED.                             NC1174.2
043100     IF       DIV3 EQUAL TO 37.7                                  NC1174.2
043200              PERFORM PASS                                        NC1174.2
043300              ELSE                                                NC1174.2
043400              GO TO SIG-FAIL-GF-2.                                NC1174.2
043500     GO TO    SIG-WRITE-GF-2.                                     NC1174.2
043600 SIG-DELETE-GF-2.                                                 NC1174.2
043700     PERFORM  DE-LETE.                                            NC1174.2
043800     GO TO    SIG-WRITE-GF-2.                                     NC1174.2
043900 SIG-FAIL-GF-2.                                                   NC1174.2
044000     PERFORM  FAIL.                                               NC1174.2
044100     MOVE     DIV3 TO COMPUTED-N.                                 NC1174.2
044200     MOVE     +37.7 TO CORRECT-N.                                 NC1174.2
044300 SIG-WRITE-GF-2.                                                  NC1174.2
044400     MOVE "SIG-TEST-GF-2 " TO PAR-NAME.                           NC1174.2
044500     PERFORM  PRINT-DETAIL.                                       NC1174.2
044600 SIG-INIT-GF-3.                                                   NC1174.2
044700     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
044800     MOVE -9.642  TO DIV4.                                        NC1174.2
044900     MOVE    .82  TO DIV5.                                        NC1174.2
045000     MOVE     "A" TO XRAY.                                        NC1174.2
045100 SIG-TEST-GF-3-0.                                                 NC1174.2
045200     DIVIDE   DIV5 INTO DIV4 ON SIZE ERROR                        NC1174.2
045300              MOVE "M" TO XRAY.                                   NC1174.2
045400 SIG-TEST-GF-3-1.                                                 NC1174.2
045500     IF       XRAY EQUAL TO "M"                                   NC1174.2
045600              PERFORM PASS                                        NC1174.2
045700              ELSE                                                NC1174.2
045800              GO TO SIG-FAIL-GF-3.                                NC1174.2
045900     GO TO    SIG-WRITE-GF-3.                                     NC1174.2
046000 SIG-DELETE-GF-3-1.                                               NC1174.2
046100     PERFORM  DE-LETE.                                            NC1174.2
046200     GO TO    SIG-WRITE-GF-3.                                     NC1174.2
046300 SIG-FAIL-GF-3.                                                   NC1174.2
046400     MOVE     DIV4 TO COMPUTED-N.                                 NC1174.2
046500     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1174.2
046600     PERFORM  FAIL.                                               NC1174.2
046700 SIG-WRITE-GF-3.                                                  NC1174.2
046800     MOVE "SIG-TEST-GF-3-1" TO PAR-NAME.                          NC1174.2
046900     PERFORM  PRINT-DETAIL.                                       NC1174.2
047000 SIG-TEST-GF-3-2.                                                 NC1174.2
047100     IF       DIV4 EQUAL TO -9.642                                NC1174.2
047200              PERFORM PASS                                        NC1174.2
047300              ELSE                                                NC1174.2
047400              GO TO SIG-FAIL-GF-3-2.                              NC1174.2
047500     GO TO    SIG-WRITE-GF-3-2.                                   NC1174.2
047600 SIG-DELETE-GF-3-2.                                               NC1174.2
047700     PERFORM  DE-LETE.                                            NC1174.2
047800     GO TO    SIG-WRITE-GF-3-2.                                   NC1174.2
047900 SIG-FAIL-GF-3-2.                                                 NC1174.2
048000     PERFORM  FAIL.                                               NC1174.2
048100     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1174.2
048200     MOVE     DIV4 TO COMPUTED-N.                                 NC1174.2
048300     MOVE     -9.642 TO CORRECT-N.                                NC1174.2
048400 SIG-WRITE-GF-3-2.                                                NC1174.2
048500     MOVE "SIG-TEST-GF-3-2 " TO PAR-NAME.                         NC1174.2
048600     PERFORM  PRINT-DETAIL.                                       NC1174.2
048700 SIG-INIT-GF-4.                                                   NC1174.2
048800     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
048900     MOVE   44.1  TO DIV2.                                        NC1174.2
049000     MOVE      0  TO DIV6.                                        NC1174.2
049100     MOVE     "A" TO XRAY.                                        NC1174.2
049200 SIG-TEST-GF-4-1-0.                                               NC1174.2
049300     DIVIDE   DIV6 INTO DIV2 ON SIZE ERROR                        NC1174.2
049400              MOVE "N" TO XRAY.                                   NC1174.2
049500 SIG-TEST-GF-4-1-1.                                               NC1174.2
049600     IF       XRAY EQUAL TO "N"                                   NC1174.2
049700              PERFORM PASS                                        NC1174.2
049800              ELSE                                                NC1174.2
049900              GO TO SIG-FAIL-GF-4-1.                              NC1174.2
050000     GO TO    SIG-WRITE-GF-4-1.                                   NC1174.2
050100 SIG-DELETE-GF-4-1.                                               NC1174.2
050200     PERFORM  DE-LETE.                                            NC1174.2
050300     GO TO    SIG-WRITE-GF-4-1.                                   NC1174.2
050400 SIG-FAIL-GF-4-1.                                                 NC1174.2
050500     MOVE     DIV2 TO COMPUTED-N.                                 NC1174.2
050600     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1174.2
050700     PERFORM  FAIL.                                               NC1174.2
050800 SIG-WRITE-GF-4-1.                                                NC1174.2
050900     MOVE "SIG-TEST-GF-4-1 " TO PAR-NAME.                         NC1174.2
051000     PERFORM  PRINT-DETAIL.                                       NC1174.2
051100 SIG-TEST-GF-4-2.                                                 NC1174.2
051200     IF       DIV2 EQUAL TO 44.1                                  NC1174.2
051300              PERFORM PASS                                        NC1174.2
051400              ELSE                                                NC1174.2
051500              GO TO SIG-FAIL-GF-4-2.                              NC1174.2
051600     GO TO    SIG-WRITE-GF-4-2.                                   NC1174.2
051700 SIG-DELETE-GF-4-2.                                               NC1174.2
051800     PERFORM  DE-LETE.                                            NC1174.2
051900     GO TO    SIG-WRITE-GF-4-2.                                   NC1174.2
052000 SIG-FAIL-GF-4-2.                                                 NC1174.2
052100     PERFORM  FAIL.                                               NC1174.2
052200     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1174.2
052300     MOVE     DIV2 TO COMPUTED-N.                                 NC1174.2
052400     MOVE     +44.1000 TO CORRECT-N.                              NC1174.2
052500 SIG-WRITE-GF-4-2.                                                NC1174.2
052600     MOVE "SIG-TEST-GF-4-2 " TO PAR-NAME.                         NC1174.2
052700     PERFORM  PRINT-DETAIL.                                       NC1174.2
052800 SIG-INIT-GF-5.                                                   NC1174.2
052900     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
053000     MOVE    9.6  TO DIV7.                                        NC1174.2
053100     MOVE     "A" TO XRAY.                                        NC1174.2
053200 SIG-TEST-GF-5-0.                                                 NC1174.2
053300     DIVIDE   0.097 INTO DIV7 ROUNDED ON SIZE ERROR               NC1174.2
053400              MOVE "N" TO XRAY.                                   NC1174.2
053500 SIG-TEST-GF-5-1.                                                 NC1174.2
053600     IF       XRAY EQUAL TO "N"                                   NC1174.2
053700              PERFORM PASS                                        NC1174.2
053800              ELSE                                                NC1174.2
053900              GO TO SIG-FAIL-GF-5-1.                              NC1174.2
054000     GO TO    SIG-WRITE-GF-5-1.                                   NC1174.2
054100 SIG-DELETE-GF-5-1.                                               NC1174.2
054200     PERFORM  DE-LETE.                                            NC1174.2
054300     GO TO    SIG-WRITE-GF-5-1.                                   NC1174.2
054400 SIG-FAIL-GF-5-1.                                                 NC1174.2
054500     MOVE     DIV7 TO COMPUTED-N.                                 NC1174.2
054600     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1174.2
054700     PERFORM  FAIL.                                               NC1174.2
054800 SIG-WRITE-GF-5-1.                                                NC1174.2
054900     MOVE "SIG-TEST-GF-5-1 " TO PAR-NAME.                         NC1174.2
055000     PERFORM  PRINT-DETAIL.                                       NC1174.2
055100 SIG-TEST-GF-5-2.                                                 NC1174.2
055200     IF       DIV7 NOT EQUAL TO 9.6                               NC1174.2
055300              GO TO SIG-FAIL-GF-5-2.                              NC1174.2
055400     PERFORM  PASS.                                               NC1174.2
055500     GO TO    SIG-WRITE-GF-5-2.                                   NC1174.2
055600 SIG-DELETE-GF-5-2.                                               NC1174.2
055700     PERFORM  DE-LETE.                                            NC1174.2
055800     GO TO    SIG-WRITE-GF-5-2.                                   NC1174.2
055900 SIG-FAIL-GF-5-2.                                                 NC1174.2
056000     PERFORM  FAIL.                                               NC1174.2
056100     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1174.2
056200     MOVE     DIV7 TO COMPUTED-N.                                 NC1174.2
056300     MOVE     +9.6 TO CORRECT-N.                                  NC1174.2
056400 SIG-WRITE-GF-5-2.                                                NC1174.2
056500     MOVE "SIG-TEST-GF-5-2 " TO PAR-NAME.                         NC1174.2
056600     PERFORM  PRINT-DETAIL.                                       NC1174.2
056700 SIG-INIT-GF-11.                                                  NC1174.2
056800     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
056900     MOVE "DIVIDE INTO GIVING" TO FEATURE.                        NC1174.2
057000     MOVE     44.1 TO DIV2.                                       NC1174.2
057100     MOVE     ZERO TO DIV8.                                       NC1174.2
057200 SIG-TEST-GF-11-0.                                                NC1174.2
057300     DIVIDE   DIV2 INTO 864.36 GIVING DIV8.                       NC1174.2
057400 SIG-TEST-GF-11-1.                                                NC1174.2
057500     IF       DIV8 EQUAL TO 19.6                                  NC1174.2
057600              PERFORM PASS                                        NC1174.2
057700              ELSE                                                NC1174.2
057800              GO TO SIG-FAIL-GF-11.                               NC1174.2
057900     GO TO    SIG-WRITE-GF-11.                                    NC1174.2
058000 SIG-DELETE-GF-11.                                                NC1174.2
058100     PERFORM  DE-LETE.                                            NC1174.2
058200     GO TO    SIG-WRITE-GF-11.                                    NC1174.2
058300 SIG-FAIL-GF-11.                                                  NC1174.2
058400     PERFORM  FAIL.                                               NC1174.2
058500     MOVE     DIV8 TO COMPUTED-N.                                 NC1174.2
058600     MOVE     +19.6 TO CORRECT-N.                                 NC1174.2
058700 SIG-WRITE-GF-11.                                                 NC1174.2
058800     MOVE "SIG-TEST-GF-11 " TO PAR-NAME.                          NC1174.2
058900     PERFORM  PRINT-DETAIL.                                       NC1174.2
059000 SIG-INIT-GF-12.                                                  NC1174.2
059100     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
059200     MOVE    1620.36 TO DIV1.                                     NC1174.2
059300     MOVE    ZERO TO DIV9.                                        NC1174.2
059400 SIG-TEST-GF-12-0.                                                NC1174.2
059500     DIVIDE   0.533 INTO DIV1 GIVING DIV9 ROUNDED.                NC1174.2
059600 SIG-TEST-GF-12-1.                                                NC1174.2
059700     IF       DIV9 EQUAL TO " 3,040.1"                            NC1174.2
059800              PERFORM PASS                                        NC1174.2
059900              ELSE                                                NC1174.2
060000              GO TO SIG-FAIL-GF-12.                               NC1174.2
060100     GO TO    SIG-WRITE-GF-12.                                    NC1174.2
060200 SIG-DELETE-GF-12.                                                NC1174.2
060300     PERFORM  DE-LETE.                                            NC1174.2
060400     GO TO    SIG-WRITE-GF-12.                                    NC1174.2
060500 SIG-FAIL-GF-12.                                                  NC1174.2
060600     PERFORM  FAIL.                                               NC1174.2
060700     MOVE     DIV9 TO COMPUTED-A.                                 NC1174.2
060800     MOVE " 3,040.1" TO CORRECT-A.                                NC1174.2
060900 SIG-WRITE-GF-12.                                                 NC1174.2
061000     MOVE "SIG-TEST-GF-12" TO PAR-NAME.                           NC1174.2
061100     PERFORM  PRINT-DETAIL.                                       NC1174.2
061200 SIG-INIT-GF-13.                                                  NC1174.2
061300     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
061400     MOVE     -9.642 TO DIV4.                                     NC1174.2
061500     MOVE     ZERO TO DIV10.                                      NC1174.2
061600     MOVE     44.1 TO DIV2.                                       NC1174.2
061700     MOVE     "A"  TO XRAY.                                       NC1174.2
061800 SIG-TEST-GF-13-0.                                                NC1174.2
061900     DIVIDE   DIV4 INTO DIV2 GIVING DIV10 ON SIZE ERROR           NC1174.2
062000              MOVE "P" TO XRAY.                                   NC1174.2
062100 SIG-TEST-GF-13-1.                                                NC1174.2
062200     IF       XRAY EQUAL TO "P"                                   NC1174.2
062300              PERFORM PASS                                        NC1174.2
062400              ELSE                                                NC1174.2
062500              GO TO SIG-FAIL-GF-13-1.                             NC1174.2
062600     GO TO    SIG-WRITE-GF-13-1.                                  NC1174.2
062700 SIG-DELETE-GF-13-1.                                              NC1174.2
062800     PERFORM  DE-LETE.                                            NC1174.2
062900     GO TO    SIG-WRITE-GF-13-1.                                  NC1174.2
063000 SIG-FAIL-GF-13-1.                                                NC1174.2
063100     MOVE     DIV10 TO COMPUTED-N.                                NC1174.2
063200     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1174.2
063300     PERFORM  FAIL.                                               NC1174.2
063400 SIG-WRITE-GF-13-1.                                               NC1174.2
063500     MOVE "SIG-TEST-GF-13-1" TO PAR-NAME.                         NC1174.2
063600     PERFORM  PRINT-DETAIL.                                       NC1174.2
063700 SIG-TEST-GF-13-2.                                                NC1174.2
063800     IF       DIV10 NOT EQUAL TO ZERO                             NC1174.2
063900              GO TO SIG-FAIL-GF-13-2.                             NC1174.2
064000     PERFORM  PASS.                                               NC1174.2
064100     GO TO    SIG-WRITE-GF-13-2.                                  NC1174.2
064200 SIG-DELETE-GF-13-2.                                              NC1174.2
064300     PERFORM  DE-LETE.                                            NC1174.2
064400     GO TO    SIG-WRITE-GF-13-2.                                  NC1174.2
064500 SIG-FAIL-GF-13-2.                                                NC1174.2
064600     PERFORM FAIL.                                                NC1174.2
064700     MOVE    DIV10 TO COMPUTED-N.                                 NC1174.2
064800     MOVE    ZERO TO CORRECT-N.                                   NC1174.2
064900     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1174.2
065000 SIG-WRITE-GF-13-2.                                               NC1174.2
065100     MOVE "SIG-TEST-GF-13-2" TO PAR-NAME.                         NC1174.2
065200     PERFORM  PRINT-DETAIL.                                       NC1174.2
065300 SIG-INIT-GF-19.                                                  NC1174.2
065400     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
065500     MOVE     ZERO TO DIV8.                                       NC1174.2
065600     MOVE     44.1 TO DIV2.                                       NC1174.2
065700     MOVE "DIVIDE BY" TO FEATURE.                                 NC1174.2
065800 SIG-TEST-GF-19.                                                  NC1174.2
065900     DIVIDE   864.36 BY DIV2 GIVING DIV8.                         NC1174.2
066000     IF       DIV8 EQUAL TO 19.6                                  NC1174.2
066100              PERFORM PASS                                        NC1174.2
066200              ELSE                                                NC1174.2
066300              GO TO SIG-FAIL-GF-19.                               NC1174.2
066400     GO TO    SIG-WRITE-GF-19.                                    NC1174.2
066500 SIG-DELETE-GF-19.                                                NC1174.2
066600     PERFORM  DE-LETE.                                            NC1174.2
066700     GO TO    SIG-WRITE-GF-19.                                    NC1174.2
066800 SIG-FAIL-GF-19.                                                  NC1174.2
066900     PERFORM  FAIL.                                               NC1174.2
067000     MOVE     DIV8 TO COMPUTED-N.                                 NC1174.2
067100     MOVE     19.6 TO CORRECT-N.                                  NC1174.2
067200 SIG-WRITE-GF-19.                                                 NC1174.2
067300     MOVE "SIG-TEST-GF-19" TO PAR-NAME.                           NC1174.2
067400     PERFORM  PRINT-DETAIL.                                       NC1174.2
067500 SIG-INIT-GF-20.                                                  NC1174.2
067600     MOVE "DIVIDE BY GIVING" TO FEATURE.                          NC1174.2
067700     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
067800     MOVE  1620.36 TO DIV1.                                       NC1174.2
067900     MOVE     ZERO TO DIV9.                                       NC1174.2
068000 SIG-TEST-GF-20-0.                                                NC1174.2
068100     DIVIDE   DIV1 BY 0.533 GIVING DIV9 ROUNDED.                  NC1174.2
068200 SIG-TEST-GF-20-1.                                                NC1174.2
068300     IF       DIV9 EQUAL TO " 3,040.1"                            NC1174.2
068400              PERFORM PASS                                        NC1174.2
068500              ELSE                                                NC1174.2
068600              GO TO SIG-FAIL-GF-20.                               NC1174.2
068700     GO TO    SIG-WRITE-GF-20.                                    NC1174.2
068800 SIG-DELETE-GF-20.                                                NC1174.2
068900     PERFORM  DE-LETE.                                            NC1174.2
069000     GO TO    SIG-WRITE-GF-20.                                    NC1174.2
069100 SIG-FAIL-GF-20.                                                  NC1174.2
069200     PERFORM  FAIL.                                               NC1174.2
069300     MOVE     DIV9 TO COMPUTED-A.                                 NC1174.2
069400     MOVE " 3,040.1" TO CORRECT-A.                                NC1174.2
069500 SIG-WRITE-GF-20.                                                 NC1174.2
069600     MOVE "SIG-TEST-GF-20" TO PAR-NAME.                           NC1174.2
069700     PERFORM  PRINT-DETAIL.                                       NC1174.2
069800 SIG-INIT-GF-21.                                                  NC1174.2
069900     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
070000     MOVE     -9.642 TO DIV4.                                     NC1174.2
070100     MOVE     ZERO TO DIV10.                                      NC1174.2
070200     MOVE     44.1 TO DIV2.                                       NC1174.2
070300     MOVE     "A"  TO XRAY.                                       NC1174.2
070400 SIG-TEST-GF-21-0.                                                NC1174.2
070500     DIVIDE   DIV2 BY DIV4 GIVING DIV10 ON SIZE ERROR             NC1174.2
070600              MOVE "P" TO XRAY.                                   NC1174.2
070700 SIG-TEST-GF-21-1.                                                NC1174.2
070800     IF       XRAY EQUAL TO "P"                                   NC1174.2
070900              PERFORM PASS                                        NC1174.2
071000              ELSE                                                NC1174.2
071100              GO TO SIG-FAIL-GF-21-1.                             NC1174.2
071200     GO TO    SIG-WRITE-GF-21-1.                                  NC1174.2
071300 SIG-DELETE-GF-21-1.                                              NC1174.2
071400     PERFORM  DE-LETE.                                            NC1174.2
071500     GO TO    SIG-WRITE-GF-21-1.                                  NC1174.2
071600 SIG-FAIL-GF-21-1.                                                NC1174.2
071700     MOVE     DIV10 TO COMPUTED-N.                                NC1174.2
071800     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1174.2
071900     PERFORM  FAIL.                                               NC1174.2
072000 SIG-WRITE-GF-21-1.                                               NC1174.2
072100     MOVE "SIG-TEST-GF-21-1" TO PAR-NAME.                         NC1174.2
072200     PERFORM  PRINT-DETAIL.                                       NC1174.2
072300 SIG-TEST-GF-21-2.                                                NC1174.2
072400     IF       DIV10 = 0                                           NC1174.2
072500              PERFORM PASS                                        NC1174.2
072600              ELSE                                                NC1174.2
072700              GO TO SIG-FAIL-GF-21-2.                             NC1174.2
072800     GO TO    SIG-WRITE-GF-21-2.                                  NC1174.2
072900 SIG-DELETE-GF-21-2.                                              NC1174.2
073000     PERFORM  DE-LETE.                                            NC1174.2
073100     GO TO    SIG-WRITE-GF-21-2.                                  NC1174.2
073200 SIG-FAIL-GF-21-2.                                                NC1174.2
073300     MOVE     DIV10 TO COMPUTED-N.                                NC1174.2
073400     MOVE     0     TO CORRECT-N.                                 NC1174.2
073500     MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.          NC1174.2
073600     PERFORM FAIL.                                                NC1174.2
073700 SIG-WRITE-GF-21-2.                                               NC1174.2
073800     MOVE "SIG-TEST-GF-21-2" TO PAR-NAME.                         NC1174.2
073900     PERFORM  PRINT-DETAIL.                                       NC1174.2
074000 SIG-INIT-GF-6.                                                   NC1174.2
074100     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
074200     MOVE "DIVIDE INTO         " TO FEATURE.                      NC1174.2
074300     MOVE 99 TO WRK-DS-LS-18V00.                                  NC1174.2
074400 SIG-TEST-GF-6-0.                                                 NC1174.2
074500     DIVIDE A99-DS-02V00 INTO WRK-DS-LS-18V00.                    NC1174.2
074600 SIG-TEST-GF-6-1.                                                 NC1174.2
074700     IF WRK-DS-LS-18V00 EQUAL TO 000000000000000001               NC1174.2
074800         PERFORM PASS                                             NC1174.2
074900         GO TO SIG-WRITE-GF-6.                                    NC1174.2
075000     GO TO SIG-FAIL-GF-6.                                         NC1174.2
075100 SIG-DELETE-GF-6.                                                 NC1174.2
075200     PERFORM DE-LETE.                                             NC1174.2
075300     GO TO SIG-WRITE-GF-6.                                        NC1174.2
075400 SIG-FAIL-GF-6.                                                   NC1174.2
075500     MOVE  000000000000000001 TO CORRECT-18V0.                    NC1174.2
075600     MOVE  WRK-DS-LS-18V00       TO COMPUTED-18V0.                NC1174.2
075700     PERFORM FAIL.                                                NC1174.2
075800 SIG-WRITE-GF-6.                                                  NC1174.2
075900     MOVE "SIG-TEST-GF-6         " TO PAR-NAME.                   NC1174.2
076000     PERFORM PRINT-DETAIL.                                        NC1174.2
076100 SIG-INIT-GF-7.                                                   NC1174.2
076200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
076300     MOVE 2 TO WRK-DS-TS-12V00-S.                                 NC1174.2
076400 SIG-TEST-GF-7-0.                                                 NC1174.2
076500     DIVIDE 4 INTO WRK-DS-TS-12V00-S ROUNDED.                     NC1174.2
076600 SIG-TEST-GF-7-1.                                                 NC1174.2
076700     IF WRK-DS-TS-12V00-S EQUAL TO 000000000001                   NC1174.2
076800         PERFORM PASS                                             NC1174.2
076900         GO TO SIG-WRITE-GF-7.                                    NC1174.2
077000     GO TO SIG-FAIL-GF-7.                                         NC1174.2
077100 SIG-DELETE-GF-7.                                                 NC1174.2
077200     PERFORM DE-LETE.                                             NC1174.2
077300     GO TO SIG-WRITE-GF-7.                                        NC1174.2
077400 SIG-FAIL-GF-7.                                                   NC1174.2
077500     MOVE  WRK-DS-TS-12V00-S       TO COMPUTED-18V0.              NC1174.2
077600     MOVE  000000000001       TO CORRECT-18V0.                    NC1174.2
077700     PERFORM FAIL.                                                NC1174.2
077800 SIG-WRITE-GF-7.                                                  NC1174.2
077900     MOVE "SIG-TEST-GF-7         " TO PAR-NAME.                   NC1174.2
078000     PERFORM PRINT-DETAIL.                                        NC1174.2
078100 SIG-INIT-GF-8.                                                   NC1174.2
078200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
078300     MOVE 1 TO WRK-DS-LS-01V00.                                   NC1174.2
078400     MOVE "0" TO WRK-XN-00001.                                    NC1174.2
078500 SIG-TEST-GF-8-0.                                                 NC1174.2
078600     DIVIDE 0.1 INTO WRK-DS-LS-01V00 ON SIZE ERROR                NC1174.2
078700         MOVE "1" TO WRK-XN-00001.                                NC1174.2
078800 SIG-TEST-GF-8-1.                                                 NC1174.2
078900     IF WRK-DS-LS-01V00 EQUAL TO 1                                NC1174.2
079000         PERFORM PASS                                             NC1174.2
079100         GO TO SIG-WRITE-GF-8-1.                                  NC1174.2
079200     GO TO SIG-FAIL-GF-8-1.                                       NC1174.2
079300 SIG-DELETE-GF-8-1.                                               NC1174.2
079400     PERFORM DE-LETE.                                             NC1174.2
079500     GO TO SIG-WRITE-GF-8-1.                                      NC1174.2
079600 SIG-FAIL-GF-8-1.                                                 NC1174.2
079700     MOVE  1                  TO CORRECT-N.                       NC1174.2
079800     MOVE  WRK-DS-LS-01V00       TO COMPUTED-N.                   NC1174.2
079900     MOVE  "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.           NC1174.2
080000     PERFORM FAIL.                                                NC1174.2
080100 SIG-WRITE-GF-8-1.                                                NC1174.2
080200     MOVE "SIG-TEST-GF-8-1         " TO PAR-NAME.                 NC1174.2
080300     PERFORM PRINT-DETAIL.                                        NC1174.2
080400 SIG-TEST-GF-8-2.                                                 NC1174.2
080500     IF WRK-XN-00001 EQUAL TO "1"                                 NC1174.2
080600         PERFORM PASS                                             NC1174.2
080700         GO TO SIG-WRITE-GF-8-2.                                  NC1174.2
080800     MOVE "1" TO CORRECT-A.                                       NC1174.2
080900     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1174.2
081000     MOVE "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.          NC1174.2
081100     PERFORM FAIL.                                                NC1174.2
081200     GO TO SIG-WRITE-GF-8-2.                                      NC1174.2
081300 SIG-DELETE-GF-8-2.                                               NC1174.2
081400     PERFORM DE-LETE.                                             NC1174.2
081500 SIG-WRITE-GF-8-2.                                                NC1174.2
081600     MOVE "SIG-TEST-GF-8-2         " TO PAR-NAME.                 NC1174.2
081700     PERFORM PRINT-DETAIL.                                        NC1174.2
081800 SIG-INIT-GF-9.                                                   NC1174.2
081900     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
082000     MOVE -.000000001 TO WRK-DS-09V09.                            NC1174.2
082100     MOVE "1" TO WRK-XN-00001.                                    NC1174.2
082200 SIG-TEST-GF-9-0.                                                 NC1174.2
082300     DIVIDE A01ONE-DS-TS-P0801 INTO WRK-DS-09V09 ON SIZE ERROR    NC1174.2
082400         MOVE "0" TO WRK-XN-00001.                                NC1174.2
082500 SIG-TEST-GF-9-1.                                                 NC1174.2
082600     IF WRK-DS-18V00-S EQUAL TO -000000001000000000               NC1174.2
082700         PERFORM PASS                                             NC1174.2
082800         GO TO SIG-WRITE-GF-9-1.                                  NC1174.2
082900     GO TO SIG-FAIL-GF-9-1.                                       NC1174.2
083000 SIG-DELETE-GF-9-1.                                               NC1174.2
083100     PERFORM DE-LETE.                                             NC1174.2
083200     GO TO SIG-WRITE-GF-9-1.                                      NC1174.2
083300 SIG-FAIL-GF-9-1.                                                 NC1174.2
083400     MOVE -000000001000000000 TO CORRECT-18V0.                    NC1174.2
083500     MOVE WRK-DS-18V00-S TO COMPUTED-18V0.                        NC1174.2
083600     PERFORM FAIL.                                                NC1174.2
083700 SIG-WRITE-GF-9-1.                                                NC1174.2
083800     MOVE "SIG-TEST-GF-9-1         " TO PAR-NAME.                 NC1174.2
083900     PERFORM PRINT-DETAIL.                                        NC1174.2
084000 SIG-TEST-GF-9-2.                                                 NC1174.2
084100     IF WRK-XN-00001 EQUAL TO "0"                                 NC1174.2
084200         MOVE "1" TO CORRECT-A                                    NC1174.2
084300         MOVE "0" TO COMPUTED-A                                   NC1174.2
084400         MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK   NC1174.2
084500         PERFORM FAIL                                             NC1174.2
084600         GO TO SIG-WRITE-GF-9-2.                                  NC1174.2
084700     PERFORM PASS.                                                NC1174.2
084800     GO TO SIG-WRITE-GF-9-2.                                      NC1174.2
084900 SIG-DELETE-GF-9-2.                                               NC1174.2
085000     PERFORM DE-LETE.                                             NC1174.2
085100 SIG-WRITE-GF-9-2.                                                NC1174.2
085200     MOVE "SIG-TEST-GF-9-2         " TO PAR-NAME.                 NC1174.2
085300     PERFORM PRINT-DETAIL.                                        NC1174.2
085400 SIG-INIT-GF-10.                                                  NC1174.2
085500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
085600     MOVE ZERO TO WRK-DS-LS-01V00  AZERO-DS-LS-05V05.             NC1174.2
085700     MOVE "0" TO WRK-XN-00001.                                    NC1174.2
085800 SIG-TEST-GF-10-0.                                                NC1174.2
085900     DIVIDE AZERO-DS-LS-05V05 INTO WRK-DS-LS-01V00 ROUNDED        NC1174.2
086000         ON SIZE ERROR  MOVE "1" TO WRK-XN-00001.                 NC1174.2
086100 SIG-TEST-GF-10-1.                                                NC1174.2
086200     IF WRK-DS-LS-01V00 EQUAL TO 0                                NC1174.2
086300         PERFORM PASS                                             NC1174.2
086400         GO TO SIG-WRITE-GF-10-1.                                 NC1174.2
086500     GO TO SIG-FAIL-GF-10-1.                                      NC1174.2
086600 SIG-DELETE-GF-10-1.                                              NC1174.2
086700     PERFORM DE-LETE.                                             NC1174.2
086800     GO TO SIG-WRITE-GF-10-1.                                     NC1174.2
086900 SIG-FAIL-GF-10-1.                                                NC1174.2
087000     MOVE  0                  TO CORRECT-N.                       NC1174.2
087100     MOVE  WRK-DS-LS-01V00       TO COMPUTED-N.                   NC1174.2
087200     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1174.2
087300     PERFORM FAIL.                                                NC1174.2
087400 SIG-WRITE-GF-10-1.                                               NC1174.2
087500     MOVE "SIG-TEST-GF-10-1         " TO PAR-NAME.                NC1174.2
087600     PERFORM PRINT-DETAIL.                                        NC1174.2
087700 SIG-TEST-GF-10-2.                                                NC1174.2
087800     IF WRK-XN-00001 EQUAL TO "1"                                 NC1174.2
087900         PERFORM PASS                                             NC1174.2
088000         GO TO SIG-WRITE-GF-10-2.                                 NC1174.2
088100     MOVE "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.          NC1174.2
088200     MOVE "1" TO CORRECT-A.                                       NC1174.2
088300     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1174.2
088400     PERFORM FAIL.                                                NC1174.2
088500     GO TO SIG-WRITE-GF-10-2.                                     NC1174.2
088600 SIG-DELETE-GF-10-2.                                              NC1174.2
088700     PERFORM DE-LETE.                                             NC1174.2
088800 SIG-WRITE-GF-10-2.                                               NC1174.2
088900     MOVE "SIG-TEST-GF-10-2         " TO PAR-NAME.                NC1174.2
089000     PERFORM PRINT-DETAIL.                                        NC1174.2
089100 SIG-INIT-GF-14.                                                  NC1174.2
089200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
089300     MOVE "DIVIDE INTO GIVING  " TO FEATURE.                      NC1174.2
089400     MOVE ZERO TO WRK-DS-LS-01V00.                                NC1174.2
089500     MOVE 22 TO A02TWOS-DU-02V00.                                 NC1174.2
089600 SIG-TEST-GF-14-0.                                                NC1174.2
089700     DIVIDE -10.9 INTO A02TWOS-DU-02V00 GIVING WRK-DS-LS-01V00.   NC1174.2
089800 SIG-TEST-GF-14-1.                                                NC1174.2
089900     IF WRK-DS-LS-01V00 EQUAL TO -2                               NC1174.2
090000         PERFORM PASS                                             NC1174.2
090100         GO TO SIG-WRITE-GF-14.                                   NC1174.2
090200     GO TO SIG-FAIL-GF-14.                                        NC1174.2
090300 SIG-DELETE-GF-14.                                                NC1174.2
090400     PERFORM DE-LETE.                                             NC1174.2
090500     GO TO SIG-WRITE-GF-14.                                       NC1174.2
090600 SIG-FAIL-GF-14.                                                  NC1174.2
090700     MOVE  -2                 TO CORRECT-N.                       NC1174.2
090800     MOVE  WRK-DS-LS-01V00       TO COMPUTED-N.                   NC1174.2
090900     PERFORM FAIL.                                                NC1174.2
091000 SIG-WRITE-GF-14.                                                 NC1174.2
091100     MOVE "SIG-TEST-GF-14         " TO PAR-NAME.                  NC1174.2
091200     PERFORM PRINT-DETAIL.                                        NC1174.2
091300 SIG-INIT-GF-15.                                                  NC1174.2
091400     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
091500     MOVE 0.0000000001 TO WRK-DS-03V10.                           NC1174.2
091600     MOVE ZERO TO WRK-DS-LS-18V00.                                NC1174.2
091700     MOVE .000000001 TO A01ONE-DS-TS-P0801.                       NC1174.2
091800 SIG-TEST-GF-15-0.                                                NC1174.2
091900     DIVIDE WRK-DS-03V10 INTO A01ONE-DS-TS-P0801                  NC1174.2
092000         GIVING WRK-DS-LS-18V00 ROUNDED.                          NC1174.2
092100 SIG-TEST-GF-15-1.                                                NC1174.2
092200     IF WRK-DS-LS-18V00 EQUAL TO 000000000000000010               NC1174.2
092300         PERFORM PASS                                             NC1174.2
092400         GO TO SIG-WRITE-GF-15.                                   NC1174.2
092500     GO TO SIG-FAIL-GF-15.                                        NC1174.2
092600 SIG-DELETE-GF-15.                                                NC1174.2
092700     PERFORM DE-LETE.                                             NC1174.2
092800     GO TO SIG-WRITE-GF-15.                                       NC1174.2
092900 SIG-FAIL-GF-15.                                                  NC1174.2
093000     MOVE  000000000000000010 TO CORRECT-18V0.                    NC1174.2
093100     MOVE  WRK-DS-LS-18V00       TO COMPUTED-18V0.                NC1174.2
093200     PERFORM FAIL.                                                NC1174.2
093300 SIG-WRITE-GF-15.                                                 NC1174.2
093400     MOVE "SIG-TEST-GF-15         " TO PAR-NAME.                  NC1174.2
093500     PERFORM PRINT-DETAIL.                                        NC1174.2
093600 SIG-INIT-GF-16.                                                  NC1174.2
093700     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
093800     MOVE ZERO TO WRK-DS-LS-18V00  AZERO-DS-LS-05V05.             NC1174.2
093900     MOVE "0" TO WRK-XN-00001.                                    NC1174.2
094000 SIG-TEST-GF-16-0.                                                NC1174.2
094100     DIVIDE AZERO-DS-LS-05V05 INTO A99-DS-02V00                   NC1174.2
094200         GIVING WRK-DS-LS-18V00 ON SIZE ERROR                     NC1174.2
094300         MOVE "1" TO WRK-XN-00001.                                NC1174.2
094400 SIG-TEST-GF-16-1.                                                NC1174.2
094500     IF WRK-DS-LS-18V00 EQUAL TO 000000000000000000               NC1174.2
094600         PERFORM PASS                                             NC1174.2
094700         GO TO SIG-WRITE-GF-16-1.                                 NC1174.2
094800     GO TO SIG-FAIL-GF-16-1.                                      NC1174.2
094900 SIG-DELETE-GF-16-1.                                              NC1174.2
095000     PERFORM DE-LETE.                                             NC1174.2
095100     GO TO SIG-WRITE-GF-16-1.                                     NC1174.2
095200 SIG-FAIL-GF-16-1.                                                NC1174.2
095300     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1174.2
095400     MOVE  000000000000000000 TO CORRECT-18V0.                    NC1174.2
095500     MOVE  WRK-DS-LS-18V00       TO COMPUTED-18V0.                NC1174.2
095600     PERFORM FAIL.                                                NC1174.2
095700 SIG-WRITE-GF-16-1.                                               NC1174.2
095800     MOVE "SIG-TEST-GF-16-1         " TO PAR-NAME.                NC1174.2
095900     PERFORM PRINT-DETAIL.                                        NC1174.2
096000 SIG-TEST-GF-16-2.                                                NC1174.2
096100     IF WRK-XN-00001 EQUAL TO "1"                                 NC1174.2
096200         PERFORM PASS                                             NC1174.2
096300         GO TO SIG-WRITE-GF-16-2.                                 NC1174.2
096400     MOVE "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.          NC1174.2
096500     MOVE "1" TO CORRECT-A.                                       NC1174.2
096600     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1174.2
096700     PERFORM FAIL.                                                NC1174.2
096800     GO TO SIG-WRITE-GF-16-2.                                     NC1174.2
096900 SIG-DELETE-GF-16-2.                                              NC1174.2
097000     PERFORM DE-LETE.                                             NC1174.2
097100 SIG-WRITE-GF-16-2.                                               NC1174.2
097200     MOVE "SIG-TEST-32         " TO PAR-NAME.                     NC1174.2
097300     PERFORM PRINT-DETAIL.                                        NC1174.2
097400 SIG-INIT-GF-17.                                                  NC1174.2
097500     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
097600     MOVE ZERO TO WRK-DS-LS-0201P.                                NC1174.2
097700     MOVE -0.005 TO WRK-DS-09V09.                                 NC1174.2
097800     MOVE "0" TO WRK-XN-00001.                                    NC1174.2
097900 SIG-TEST-GF-17-0.                                                NC1174.2
098000     DIVIDE WRK-DS-09V09 INTO A05ONES-DS-LS-00V05 GIVING          NC1174.2
098100         WRK-DS-LS-0201P ROUNDED ON SIZE ERROR                    NC1174.2
098200         MOVE "1" TO WRK-XN-00001.                                NC1174.2
098300 SIG-TEST-GF-17-1.                                                NC1174.2
098400     MOVE WRK-DS-LS-0201P TO WRK-DS-05V00.                        NC1174.2
098500     IF WRK-DS-05V00 EQUAL TO  -00020                             NC1174.2
098600         PERFORM PASS                                             NC1174.2
098700         GO TO SIG-WRITE-GF-17-1.                                 NC1174.2
098800     GO TO SIG-FAIL-GF-17-1.                                      NC1174.2
098900 SIG-DELETE-GF-17-1.                                              NC1174.2
099000     PERFORM DE-LETE.                                             NC1174.2
099100     GO TO SIG-WRITE-GF-17-1.                                     NC1174.2
099200 SIG-FAIL-GF-17-1.                                                NC1174.2
099300     MOVE -00020 TO CORRECT-N.                                    NC1174.2
099400     MOVE WRK-DS-05V00 TO COMPUTED-N.                             NC1174.2
099500     PERFORM FAIL.                                                NC1174.2
099600 SIG-WRITE-GF-17-1.                                               NC1174.2
099700     MOVE "SIG-TEST-GF-17-1         " TO PAR-NAME.                NC1174.2
099800     PERFORM PRINT-DETAIL.                                        NC1174.2
099900 SIG-TEST-GF-17-2.                                                NC1174.2
100000     IF WRK-XN-00001 EQUAL TO "0"                                 NC1174.2
100100         PERFORM PASS                                             NC1174.2
100200         GO TO SIG-WRITE-GF-17-2.                                 NC1174.2
100300     MOVE "0" TO CORRECT-A.                                       NC1174.2
100400     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1174.2
100500     MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.      NC1174.2
100600     PERFORM FAIL.                                                NC1174.2
100700     GO TO SIG-WRITE-GF-17-2.                                     NC1174.2
100800 SIG-DELETE-GF-17-2.                                              NC1174.2
100900     PERFORM DE-LETE.                                             NC1174.2
101000 SIG-WRITE-GF-17-2.                                               NC1174.2
101100     MOVE "SIG-TEST-GF-17-2         " TO PAR-NAME.                NC1174.2
101200     PERFORM PRINT-DETAIL.                                        NC1174.2
101300 SIG-INIT-GF-18.                                                  NC1174.2
101400     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
101500     MOVE "1" TO WRK-XN-00001.                                    NC1174.2
101600     MOVE ZERO TO WRK-DS-LS-01V00.                                NC1174.2
101700     MOVE 22 TO A02TWOS-DU-02V00  A02TWOS-DS-03V02.               NC1174.2
101800 SIG-TEST-GF-18-0.                                                NC1174.2
101900     DIVIDE A02TWOS-DU-02V00 INTO A02TWOS-DS-03V02  GIVING        NC1174.2
102000         WRK-DS-LS-01V00 ROUNDED ON SIZE ERROR                    NC1174.2
102100         MOVE "0" TO WRK-XN-00001.                                NC1174.2
102200 SIG-TEST-GF-18-1.                                                NC1174.2
102300     IF WRK-DS-LS-01V00 EQUAL TO +1                               NC1174.2
102400         PERFORM PASS                                             NC1174.2
102500         GO TO SIG-WRITE-GF-18-1.                                 NC1174.2
102600     GO TO SIG-FAIL-GF-18-1.                                      NC1174.2
102700 SIG-DELETE-GF-18-1.                                              NC1174.2
102800     PERFORM DE-LETE.                                             NC1174.2
102900     GO TO SIG-WRITE-GF-18-1.                                     NC1174.2
103000 SIG-FAIL-GF-18-1.                                                NC1174.2
103100     MOVE  +1                 TO CORRECT-N.                       NC1174.2
103200     MOVE  WRK-DS-LS-01V00       TO COMPUTED-N.                   NC1174.2
103300     PERFORM FAIL.                                                NC1174.2
103400 SIG-WRITE-GF-18-1.                                               NC1174.2
103500     MOVE "SIG-TEST-GF-18-1         " TO PAR-NAME.                NC1174.2
103600     PERFORM PRINT-DETAIL.                                        NC1174.2
103700 SIG-TEST-GF-18-2.                                                NC1174.2
103800     IF WRK-XN-00001 EQUAL TO "1"                                 NC1174.2
103900         PERFORM PASS                                             NC1174.2
104000         GO TO SIG-WRITE-GF-18-2.                                 NC1174.2
104100     MOVE "1" TO CORRECT-A.                                       NC1174.2
104200     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1174.2
104300     MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.      NC1174.2
104400     PERFORM FAIL.                                                NC1174.2
104500     GO TO SIG-WRITE-GF-18-2.                                     NC1174.2
104600 SIG-DELETE-GF-18-2.                                              NC1174.2
104700     PERFORM DE-LETE.                                             NC1174.2
104800 SIG-WRITE-GF-18-2.                                               NC1174.2
104900     MOVE "SIG-TEST-GF-18-2         " TO PAR-NAME.                NC1174.2
105000     PERFORM PRINT-DETAIL.                                        NC1174.2
105100 SIG-INIT-GF-22.                                                  NC1174.2
105200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
105300     MOVE "DIVIDE BY GIVING    " TO FEATURE.                      NC1174.2
105400     MOVE ZERO TO WRK-DS-LS-01V00.                                NC1174.2
105500 SIG-TEST-GF-22-0.                                                NC1174.2
105600     DIVIDE A02TWOS-DU-02V00 BY -10.9 GIVING WRK-DS-LS-01V00.     NC1174.2
105700 SIG-TEST-GF-22-1.                                                NC1174.2
105800     IF WRK-DS-LS-01V00 EQUAL TO -2                               NC1174.2
105900         PERFORM PASS                                             NC1174.2
106000         GO TO SIG-WRITE-GF-22.                                   NC1174.2
106100     GO TO SIG-FAIL-GF-22.                                        NC1174.2
106200 SIG-DELETE-GF-22.                                                NC1174.2
106300     PERFORM DE-LETE.                                             NC1174.2
106400     GO TO SIG-WRITE-GF-22.                                       NC1174.2
106500 SIG-FAIL-GF-22.                                                  NC1174.2
106600     MOVE  -2                 TO CORRECT-N.                       NC1174.2
106700     MOVE  WRK-DS-LS-01V00       TO COMPUTED-N.                   NC1174.2
106800     PERFORM FAIL.                                                NC1174.2
106900 SIG-WRITE-GF-22.                                                 NC1174.2
107000     MOVE "SIG-TEST-GF-22         " TO PAR-NAME.                  NC1174.2
107100     PERFORM PRINT-DETAIL.                                        NC1174.2
107200 SIG-INIT-GF-23.                                                  NC1174.2
107300     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
107400     MOVE 0.0000000001 TO WRK-DS-03V10.                           NC1174.2
107500     MOVE ZERO TO WRK-DS-LS-18V00.                                NC1174.2
107600     MOVE .000000001 TO A01ONE-DS-TS-P0801.                       NC1174.2
107700 SIG-TEST-GF-23-0.                                                NC1174.2
107800     DIVIDE A01ONE-DS-TS-P0801 BY WRK-DS-03V10 GIVING             NC1174.2
107900         WRK-DS-LS-18V00 ROUNDED.                                 NC1174.2
108000 SIG-TEST-GF-23-1.                                                NC1174.2
108100     IF WRK-DS-LS-18V00 EQUAL TO 000000000000000010               NC1174.2
108200         PERFORM PASS                                             NC1174.2
108300         GO TO SIG-WRITE-GF-23.                                   NC1174.2
108400     GO TO SIG-FAIL-GF-23.                                        NC1174.2
108500 SIG-DELETE-GF-23.                                                NC1174.2
108600     PERFORM DE-LETE.                                             NC1174.2
108700     GO TO SIG-WRITE-GF-23.                                       NC1174.2
108800 SIG-FAIL-GF-23.                                                  NC1174.2
108900     MOVE  000000000000000010 TO CORRECT-18V0.                    NC1174.2
109000     MOVE  WRK-DS-LS-18V00       TO COMPUTED-18V0.                NC1174.2
109100     PERFORM FAIL.                                                NC1174.2
109200 SIG-WRITE-GF-23.                                                 NC1174.2
109300     MOVE "SIG-TEST-GF-23         " TO PAR-NAME.                  NC1174.2
109400     PERFORM PRINT-DETAIL.                                        NC1174.2
109500 SIG-INIT-GF-24.                                                  NC1174.2
109600     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
109700     MOVE ZERO TO WRK-DS-LS-18V00 AZERO-DS-LS-05V05.              NC1174.2
109800     MOVE "0" TO WRK-XN-00001.                                    NC1174.2
109900     MOVE 99 TO A99-DS-02V00.                                     NC1174.2
110000 SIG-TEST-GF-24-0.                                                NC1174.2
110100     DIVIDE A99-DS-02V00 BY AZERO-DS-LS-05V05 GIVING              NC1174.2
110200         WRK-DS-LS-18V00 ON SIZE ERROR MOVE "1" TO WRK-XN-00001.  NC1174.2
110300 SIG-TEST-GF-24-1.                                                NC1174.2
110400     IF WRK-DS-LS-18V00 EQUAL TO 000000000000000000               NC1174.2
110500         PERFORM PASS                                             NC1174.2
110600         GO TO SIG-WRITE-GF-24-1.                                 NC1174.2
110700     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1174.2
110800     MOVE  000000000000000000 TO CORRECT-18V0.                    NC1174.2
110900     MOVE  WRK-DS-LS-18V00       TO COMPUTED-18V0.                NC1174.2
111000     PERFORM FAIL.                                                NC1174.2
111100     GO TO SIG-WRITE-GF-24-1.                                     NC1174.2
111200 SIG-DELETE-GF-24-1.                                              NC1174.2
111300     PERFORM DE-LETE.                                             NC1174.2
111400 SIG-WRITE-GF-24-1.                                               NC1174.2
111500     MOVE "SIG-TEST-GF-24-1         " TO PAR-NAME.                NC1174.2
111600     PERFORM PRINT-DETAIL.                                        NC1174.2
111700 SIG-TEST-GF-24-2.                                                NC1174.2
111800     IF WRK-XN-00001 EQUAL TO "1"                                 NC1174.2
111900         PERFORM PASS                                             NC1174.2
112000         GO TO SIG-WRITE-GF-24-2.                                 NC1174.2
112100     MOVE "1" TO CORRECT-A.                                       NC1174.2
112200     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1174.2
112300     MOVE "ON SIZE ERROR SHOULD BE EXECUTED" TO RE-MARK.          NC1174.2
112400     PERFORM FAIL.                                                NC1174.2
112500     GO TO SIG-WRITE-GF-24-2.                                     NC1174.2
112600 SIG-DELETE-GF-24-2.                                              NC1174.2
112700     PERFORM DE-LETE.                                             NC1174.2
112800 SIG-WRITE-GF-24-2.                                               NC1174.2
112900     MOVE "SIG-TEST-GF-24-2         " TO PAR-NAME.                NC1174.2
113000     PERFORM PRINT-DETAIL.                                        NC1174.2
113100 SIG-INIT-GF-25.                                                  NC1174.2
113200     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
113300     MOVE .11111 TO A05ONES-DS-LS-00V05.                          NC1174.2
113400     MOVE ZERO TO WRK-DS-LS-0201P.                                NC1174.2
113500     MOVE -0.005 TO WRK-DS-09V09.                                 NC1174.2
113600     MOVE "0" TO WRK-XN-00001.                                    NC1174.2
113700 SIG-TEST-GF-25-0.                                                NC1174.2
113800     DIVIDE A05ONES-DS-LS-00V05 BY WRK-DS-09V09 GIVING            NC1174.2
113900         WRK-DS-LS-0201P ROUNDED ON SIZE ERROR                    NC1174.2
114000         MOVE "1" TO WRK-XN-00001.                                NC1174.2
114100 SIG-TEST-GF-25-1.                                                NC1174.2
114200     MOVE WRK-DS-LS-0201P TO WRK-DS-05V00.                        NC1174.2
114300     IF  WRK-DS-05V00 EQUAL TO -00020                             NC1174.2
114400         PERFORM PASS                                             NC1174.2
114500         GO TO SIG-WRITE-GF-25-1.                                 NC1174.2
114600     MOVE  -00020             TO CORRECT-N.                       NC1174.2
114700     MOVE  WRK-DS-05V00       TO COMPUTED-N.                      NC1174.2
114800     PERFORM FAIL.                                                NC1174.2
114900     GO TO SIG-WRITE-GF-25-1.                                     NC1174.2
115000 SIG-DELETE-GF-25-1.                                              NC1174.2
115100     PERFORM DE-LETE.                                             NC1174.2
115200 SIG-WRITE-GF-25-1.                                               NC1174.2
115300     MOVE "SIG-TEST-GF-25-1         " TO PAR-NAME.                NC1174.2
115400     PERFORM PRINT-DETAIL.                                        NC1174.2
115500 SIG-TEST-GF-25-2.                                                NC1174.2
115600     IF WRK-XN-00001 EQUAL TO "0"                                 NC1174.2
115700         PERFORM PASS                                             NC1174.2
115800         GO TO SIG-WRITE-GF-25-2.                                 NC1174.2
115900     MOVE "0" TO CORRECT-A.                                       NC1174.2
116000     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1174.2
116100     MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.      NC1174.2
116200     PERFORM FAIL.                                                NC1174.2
116300     GO TO SIG-WRITE-GF-25-2.                                     NC1174.2
116400 SIG-DELETE-GF-25-2.                                              NC1174.2
116500     PERFORM DE-LETE.                                             NC1174.2
116600 SIG-WRITE-GF-25-2.                                               NC1174.2
116700     MOVE "SIG-TEST-GF-25-2         " TO PAR-NAME.                NC1174.2
116800     PERFORM PRINT-DETAIL.                                        NC1174.2
116900 SIG-INIT-GF-26.                                                  NC1174.2
117000     MOVE   "VI-42 5.12.4" TO ANSI-REFERENCE.                     NC1174.2
117100     MOVE 22 TO A02TWOS-DU-02V00  A02TWOS-DS-03V02.               NC1174.2
117200     MOVE "1" TO WRK-XN-00001.                                    NC1174.2
117300     MOVE ZERO TO WRK-DS-LS-01V00.                                NC1174.2
117400 SIG-TEST-GF-26-0.                                                NC1174.2
117500     DIVIDE A02TWOS-DS-03V02 BY A02TWOS-DU-02V00 GIVING           NC1174.2
117600         WRK-DS-LS-01V00 ROUNDED ON SIZE ERROR                    NC1174.2
117700         MOVE "0" TO WRK-XN-00001.                                NC1174.2
117800 SIG-TEST-GF-26-1.                                                NC1174.2
117900     IF WRK-DS-LS-01V00 EQUAL TO +1                               NC1174.2
118000         PERFORM PASS                                             NC1174.2
118100         GO TO SIG-WRITE-GF-26-1.                                 NC1174.2
118200     MOVE  +1                 TO CORRECT-N.                       NC1174.2
118300     MOVE  WRK-DS-LS-01V00       TO COMPUTED-N.                   NC1174.2
118400     PERFORM FAIL.                                                NC1174.2
118500     GO TO SIG-WRITE-GF-26-1.                                     NC1174.2
118600 SIG-DELETE-GF-26-1.                                              NC1174.2
118700     PERFORM DE-LETE.                                             NC1174.2
118800 SIG-WRITE-GF-26-1.                                               NC1174.2
118900     MOVE "SIG-TEST-GF-26-1         " TO PAR-NAME.                NC1174.2
119000     PERFORM PRINT-DETAIL.                                        NC1174.2
119100 SIG-TEST-GF-26-2.                                                NC1174.2
119200     IF WRK-XN-00001 EQUAL TO "0"                                 NC1174.2
119300         MOVE "0" TO COMPUTED-A                                   NC1174.2
119400         MOVE "1" TO CORRECT-A                                    NC1174.2
119500         MOVE "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK   NC1174.2
119600         PERFORM FAIL                                             NC1174.2
119700        GO TO SIG-WRITE-GF-26-2.                                  NC1174.2
119800     PERFORM PASS.                                                NC1174.2
119900     GO TO SIG-WRITE-GF-26-2.                                     NC1174.2
120000 SIG-DELETE-GF-26-2.                                              NC1174.2
120100     PERFORM DE-LETE.                                             NC1174.2
120200 SIG-WRITE-GF-26-2.                                               NC1174.2
120300     MOVE "SIG-TEST-GF-26-2         " TO PAR-NAME.                NC1174.2
120400     PERFORM PRINT-DETAIL.                                        NC1174.2
120500*                                                                 NC1174.2
120600 CCVS-EXIT SECTION.                                               NC1174.2
120700 CCVS-999999.                                                     NC1174.2
120800     GO TO CLOSE-FILES.                                           NC1174.2
      *END-OF,NC117A                                                                  
