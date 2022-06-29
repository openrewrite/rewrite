      *HEADER,COBOL,NC120A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1204.2
000200 PROGRAM-ID.                                                      NC1204.2
000300     NC120A.                                                      NC1204.2
000400****************************************************************  NC1204.2
000500*                                                              *  NC1204.2
000600*    VALIDATION FOR:-                                          *  NC1204.2
000700*                                                              *  NC1204.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1204.2
000900*                                                              *  NC1204.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1204.2
001100*                                                              *  NC1204.2
001200****************************************************************  NC1204.2
001300*                                                              *  NC1204.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1204.2
001500*                                                              *  NC1204.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1204.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1204.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1204.2
001900*                                                              *  NC1204.2
002000****************************************************************  NC1204.2
002100*                                                                 NC1204.2
002200*    PROGRAM NC120A TESTS THE USE OF THE "SIGN" CLAUSE WITH       NC1204.2
002300*    FORMATS 1 AND 2 OF THE MULTIPLY STATEMENT.                   NC1204.2
002400*    ALL COMBINATIONS OF THE SIGN CLAUSE ARE USED WITH            NC1204.2
002500*    DATA ITEMS OF VARIOUS LENGTHS.                               NC1204.2
002600*                                                                 NC1204.2
002700 ENVIRONMENT DIVISION.                                            NC1204.2
002800 CONFIGURATION SECTION.                                           NC1204.2
002900 SOURCE-COMPUTER.                                                 NC1204.2
003000     XXXXX082.                                                    NC1204.2
003100 OBJECT-COMPUTER.                                                 NC1204.2
003200     XXXXX083.                                                    NC1204.2
003300 INPUT-OUTPUT SECTION.                                            NC1204.2
003400 FILE-CONTROL.                                                    NC1204.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1204.2
003600     XXXXX055.                                                    NC1204.2
003700 DATA DIVISION.                                                   NC1204.2
003800 FILE SECTION.                                                    NC1204.2
003900 FD  PRINT-FILE.                                                  NC1204.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1204.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1204.2
004200 WORKING-STORAGE SECTION.                                         NC1204.2
004300 77  WRK-DS-LS-18V00                PICTURE S9(18)                NC1204.2
004400           SIGN IS LEADING SEPARATE CHARACTER.                    NC1204.2
004500 77  A06THREES-DS-LS-03V03          PICTURE S999V999 VALUE 333.333NC1204.2
004600           SIGN IS LEADING.                                       NC1204.2
004700 77  WRK-DS-TS-06V06                PICTURE S9(6)V9(6)            NC1204.2
004800           SIGN IS TRAILING SEPARATE CHARACTER.                   NC1204.2
004900 77  WRK-DS-TS-12V00-S-S REDEFINES WRK-DS-TS-06V06 PICTURE S9(12) NC1204.2
005000           SIGN TRAILING SEPARATE.                                NC1204.2
005100 77  A08TWOS-DS-02V06            PICTURE S99V9(6) VALUE 22.222222.NC1204.2
005200 77  WRK-DS-10V00                PICTURE S9(10).                  NC1204.2
005300 77  WRK-XN-00001                PICTURE X.                       NC1204.2
005400 77  A10ONES-DS-T-10V00            PICTURE S9(10)                 NC1204.2
005500         SIGN IS TRAILING                                         NC1204.2
005600                                 VALUE 1111111111.                NC1204.2
005700 77  A12THREES-DS-LS-06V06          PICTURE S9(6)V9(6)            NC1204.2
005800             SIGN IS LEADING SEPARATE                             NC1204.2
005900                                 VALUE 333333.333333.             NC1204.2
006000 77  WRK-DS-02V00                PICTURE S99.                     NC1204.2
006100 77  AZERO-DS-LS-05V05              PICTURE S9(5)V9(5) VALUE ZERO NC1204.2
006200           SIGN IS LEADING SEPARATE CHARACTER.                    NC1204.2
006300 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1204.2
006400 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC1204.2
006500 77  A05ONES-DS-LS-00V05            PICTURE SV9(5) VALUE .11111   NC1204.2
006600           SIGN IS LEADING SEPARATE CHARACTER.                    NC1204.2
006700 77  A12ONES-DS-12V00            PICTURE S9(12)                   NC1204.2
006800                                 VALUE 111111111111.              NC1204.2
006900 77  A01ONE-DS-TS-P0801            PICTURE SP(8)9 VALUE .000000001NC1204.2
007000            SIGN IS TRAILING SEPARATE.                            NC1204.2
007100 77  WRK-DS-T-09V08                PICTURE S9(9)V9(8)             NC1204.2
007200           SIGN IS TRAILING.                                      NC1204.2
007300 77  WKR-DS-T-17V00-S REDEFINES WRK-DS-T-09V08 PICTURE S9(17)     NC1204.2
007400         SIGN TRAILING.                                           NC1204.2
007500 77  A18ONES-DS-18V00            PICTURE S9(18)                   NC1204.2
007600                                 VALUE 111111111111111111.        NC1204.2
007700 77  WRK-DS-LS-0201P                PICTURE S99P                  NC1204.2
007800            SIGN IS LEADING SEPARATE.                             NC1204.2
007900 77  WRK-CS-18V00                PICTURE S9(18) COMPUTATIONAL.    NC1204.2
008000 77  WRK-DU-18V00                PICTURE 9(18).                   NC1204.2
008100 77  A99-CS-02V00                PICTURE S99 COMPUTATIONAL        NC1204.2
008200                                 VALUE 99.                        NC1204.2
008300 77  A01ONE-CS-00V01             PICTURE SV9 COMPUTATIONAL        NC1204.2
008400                                 VALUE .1.                        NC1204.2
008500 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC1204.2
008600 77  WRK-DS-TS-12V00-S                PICTURE S9(12).             NC1204.2
008700 77  WRK-DS-LS-01V00             PICTURE S9 LEADING SEPARATE.     NC1204.2
008800 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC1204.2
008900 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC1204.2
009000                                 VALUE 111111111.111111111.       NC1204.2
009100 77  A02TWOS-DU-02V00            PICTURE 99  VALUE 22.            NC1204.2
009200 77  WRK-DS-05V00                PICTURE S9(5).                   NC1204.2
009300 77  A02TWOS-DS-03V02            PICTURE S999V99 VALUE +022.00.   NC1204.2
009400 77  WRK-CS-02V02                PICTURE S99V99  COMPUTATIONAL.   NC1204.2
009500 77  A990-DS-0201P               PICTURE S99P   VALUE +990.       NC1204.2
009600 77  XRAY                        PICTURE X.                       NC1204.2
009700 77  A01ONES-CS-18V00   PICTURE S9(18) COMPUTATIONAL              NC1204.2
009800             VALUE +000000000000000001.                           NC1204.2
009900 77  A02THREES-CS-18V00 PICTURE S9(18) COMPUTATIONAL              NC1204.2
010000             VALUE -000000000000000033.                           NC1204.2
010100 77  A18SIXES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1204.2
010200             VALUE 666666666666666666.                            NC1204.2
010300 77  A16NINES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1204.2
010400             VALUE 009999999999999999.                            NC1204.2
010500 77  A14TWOS-CU-18V00   PICTURE 9(18) COMPUTATIONAL               NC1204.2
010600             VALUE 000022222222222222.                            NC1204.2
010700 01  MULTIPLY-DATA LEADING SEPARATE.                              NC1204.2
010800     02 MULT1                           PICTURE IS 999V99         NC1204.2
010900     VALUE IS 80.12.                                              NC1204.2
011000     02 MULT2                           PICTURE IS 999V999.       NC1204.2
011100     02 MULT3                           PICTURE IS $$99.99.       NC1204.2
011200     02 MULT4                           PICTURE IS S99            NC1204.2
011300     VALUE IS -56.                                                NC1204.2
011400     02 MULT5                           PICTURE IS 9 VALUE IS 4.  NC1204.2
011500     02 MULT6                           PICTURE IS 99 VALUE IS    NC1204.2
011600     20.                                                          NC1204.2
011700 01  DIVIDE-DATA TRAILING SEPARATE.                               NC1204.2
011800     02 DIV1                            PICTURE IS 9(4)V99        NC1204.2
011900     VALUE IS 1620.36.                                            NC1204.2
012000     02 DIV2                            PICTURE IS 99V9           NC1204.2
012100     VALUE IS 44.1.                                               NC1204.2
012200     02 DIV3                            PICTURE IS 9(4)V9         NC1204.2
012300     VALUE IS 1661.7.                                             NC1204.2
012400     02 DIV4                            PICTURE IS S9V999         NC1204.2
012500     VALUE IS -9.642.                                             NC1204.2
012600     02 DIV-02LEVEL-1.                                            NC1204.2
012700     03 DIV5                            PICTURE IS V99            NC1204.2
012800     VALUE IS .82.                                                NC1204.2
012900     03 DIV6                            PICTURE IS 9 VALUE IS 0.  NC1204.2
013000     03 DIV7                            PICTURE IS 9V9            NC1204.2
013100     VALUE IS 9.6.                                                NC1204.2
013200 01  DIV-DATA-2.                                                  NC1204.2
013300     02 DIV8                            PICTURE IS 99V9.          NC1204.2
013400     02 DIV9                            PICTURE IS ZZ,ZZZ.9.      NC1204.2
013500     02 DIV10                           PICTURE IS V999.          NC1204.2
013600 01  TEST-RESULTS.                                                NC1204.2
013700     02 FILLER                   PIC X      VALUE SPACE.          NC1204.2
013800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1204.2
013900     02 FILLER                   PIC X      VALUE SPACE.          NC1204.2
014000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1204.2
014100     02 FILLER                   PIC X      VALUE SPACE.          NC1204.2
014200     02  PAR-NAME.                                                NC1204.2
014300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1204.2
014400       03  PARDOT-X              PIC X      VALUE SPACE.          NC1204.2
014500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1204.2
014600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1204.2
014700     02 RE-MARK                  PIC X(61).                       NC1204.2
014800 01  TEST-COMPUTED.                                               NC1204.2
014900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1204.2
015000     02 FILLER                   PIC X(17)  VALUE                 NC1204.2
015100            "       COMPUTED=".                                   NC1204.2
015200     02 COMPUTED-X.                                               NC1204.2
015300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1204.2
015400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1204.2
015500                                 PIC -9(9).9(9).                  NC1204.2
015600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1204.2
015700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1204.2
015800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1204.2
015900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1204.2
016000         04 COMPUTED-18V0                    PIC -9(18).          NC1204.2
016100         04 FILLER                           PIC X.               NC1204.2
016200     03 FILLER PIC X(50) VALUE SPACE.                             NC1204.2
016300 01  TEST-CORRECT.                                                NC1204.2
016400     02 FILLER PIC X(30) VALUE SPACE.                             NC1204.2
016500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1204.2
016600     02 CORRECT-X.                                                NC1204.2
016700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1204.2
016800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1204.2
016900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1204.2
017000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1204.2
017100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1204.2
017200     03      CR-18V0 REDEFINES CORRECT-A.                         NC1204.2
017300         04 CORRECT-18V0                     PIC -9(18).          NC1204.2
017400         04 FILLER                           PIC X.               NC1204.2
017500     03 FILLER PIC X(2) VALUE SPACE.                              NC1204.2
017600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1204.2
017700 01  CCVS-C-1.                                                    NC1204.2
017800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1204.2
017900-    "SS  PARAGRAPH-NAME                                          NC1204.2
018000-    "       REMARKS".                                            NC1204.2
018100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1204.2
018200 01  CCVS-C-2.                                                    NC1204.2
018300     02 FILLER                     PIC X        VALUE SPACE.      NC1204.2
018400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1204.2
018500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1204.2
018600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1204.2
018700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1204.2
018800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1204.2
018900 01  REC-CT                        PIC 99       VALUE ZERO.       NC1204.2
019000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1204.2
019100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1204.2
019200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1204.2
019300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1204.2
019400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1204.2
019500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1204.2
019600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1204.2
019700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1204.2
019800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1204.2
019900 01  CCVS-H-1.                                                    NC1204.2
020000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1204.2
020100     02  FILLER                    PIC X(42)    VALUE             NC1204.2
020200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1204.2
020300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1204.2
020400 01  CCVS-H-2A.                                                   NC1204.2
020500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1204.2
020600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1204.2
020700   02  FILLER                        PIC XXXX   VALUE             NC1204.2
020800     "4.2 ".                                                      NC1204.2
020900   02  FILLER                        PIC X(28)  VALUE             NC1204.2
021000            " COPY - NOT FOR DISTRIBUTION".                       NC1204.2
021100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1204.2
021200                                                                  NC1204.2
021300 01  CCVS-H-2B.                                                   NC1204.2
021400   02  FILLER                        PIC X(15)  VALUE             NC1204.2
021500            "TEST RESULT OF ".                                    NC1204.2
021600   02  TEST-ID                       PIC X(9).                    NC1204.2
021700   02  FILLER                        PIC X(4)   VALUE             NC1204.2
021800            " IN ".                                               NC1204.2
021900   02  FILLER                        PIC X(12)  VALUE             NC1204.2
022000     " HIGH       ".                                              NC1204.2
022100   02  FILLER                        PIC X(22)  VALUE             NC1204.2
022200            " LEVEL VALIDATION FOR ".                             NC1204.2
022300   02  FILLER                        PIC X(58)  VALUE             NC1204.2
022400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1204.2
022500 01  CCVS-H-3.                                                    NC1204.2
022600     02  FILLER                      PIC X(34)  VALUE             NC1204.2
022700            " FOR OFFICIAL USE ONLY    ".                         NC1204.2
022800     02  FILLER                      PIC X(58)  VALUE             NC1204.2
022900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1204.2
023000     02  FILLER                      PIC X(28)  VALUE             NC1204.2
023100            "  COPYRIGHT   1985 ".                                NC1204.2
023200 01  CCVS-E-1.                                                    NC1204.2
023300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1204.2
023400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1204.2
023500     02 ID-AGAIN                     PIC X(9).                    NC1204.2
023600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1204.2
023700 01  CCVS-E-2.                                                    NC1204.2
023800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1204.2
023900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1204.2
024000     02 CCVS-E-2-2.                                               NC1204.2
024100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1204.2
024200         03 FILLER                   PIC X      VALUE SPACE.      NC1204.2
024300         03 ENDER-DESC               PIC X(44)  VALUE             NC1204.2
024400            "ERRORS ENCOUNTERED".                                 NC1204.2
024500 01  CCVS-E-3.                                                    NC1204.2
024600     02  FILLER                      PIC X(22)  VALUE             NC1204.2
024700            " FOR OFFICIAL USE ONLY".                             NC1204.2
024800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1204.2
024900     02  FILLER                      PIC X(58)  VALUE             NC1204.2
025000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1204.2
025100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1204.2
025200     02 FILLER                       PIC X(15)  VALUE             NC1204.2
025300             " COPYRIGHT 1985".                                   NC1204.2
025400 01  CCVS-E-4.                                                    NC1204.2
025500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1204.2
025600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1204.2
025700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1204.2
025800     02 FILLER                       PIC X(40)  VALUE             NC1204.2
025900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1204.2
026000 01  XXINFO.                                                      NC1204.2
026100     02 FILLER                       PIC X(19)  VALUE             NC1204.2
026200            "*** INFORMATION ***".                                NC1204.2
026300     02 INFO-TEXT.                                                NC1204.2
026400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1204.2
026500       04 XXCOMPUTED                 PIC X(20).                   NC1204.2
026600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1204.2
026700       04 XXCORRECT                  PIC X(20).                   NC1204.2
026800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1204.2
026900 01  HYPHEN-LINE.                                                 NC1204.2
027000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1204.2
027100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1204.2
027200-    "*****************************************".                 NC1204.2
027300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1204.2
027400-    "******************************".                            NC1204.2
027500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1204.2
027600     "NC120A".                                                    NC1204.2
027700 PROCEDURE DIVISION.                                              NC1204.2
027800 CCVS1 SECTION.                                                   NC1204.2
027900 OPEN-FILES.                                                      NC1204.2
028000     OPEN     OUTPUT PRINT-FILE.                                  NC1204.2
028100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1204.2
028200     MOVE    SPACE TO TEST-RESULTS.                               NC1204.2
028300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1204.2
028400     GO TO CCVS1-EXIT.                                            NC1204.2
028500 CLOSE-FILES.                                                     NC1204.2
028600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1204.2
028700 TERMINATE-CCVS.                                                  NC1204.2
028800*    EXIT PROGRAM.                                                NC1204.2
028900*TERMINATE-CALL.                                                  NC1204.2
029000     STOP     RUN.                                                NC1204.2
029100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1204.2
029200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1204.2
029300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1204.2
029400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1204.2
029500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1204.2
029600 PRINT-DETAIL.                                                    NC1204.2
029700     IF REC-CT NOT EQUAL TO ZERO                                  NC1204.2
029800             MOVE "." TO PARDOT-X                                 NC1204.2
029900             MOVE REC-CT TO DOTVALUE.                             NC1204.2
030000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1204.2
030100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1204.2
030200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1204.2
030300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1204.2
030400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1204.2
030500     MOVE SPACE TO CORRECT-X.                                     NC1204.2
030600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1204.2
030700     MOVE     SPACE TO RE-MARK.                                   NC1204.2
030800 HEAD-ROUTINE.                                                    NC1204.2
030900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1204.2
031000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1204.2
031100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1204.2
031200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1204.2
031300 COLUMN-NAMES-ROUTINE.                                            NC1204.2
031400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1204.2
031500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1204.2
031600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1204.2
031700 END-ROUTINE.                                                     NC1204.2
031800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1204.2
031900 END-RTN-EXIT.                                                    NC1204.2
032000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1204.2
032100 END-ROUTINE-1.                                                   NC1204.2
032200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1204.2
032300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1204.2
032400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1204.2
032500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1204.2
032600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1204.2
032700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1204.2
032800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1204.2
032900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1204.2
033000  END-ROUTINE-12.                                                 NC1204.2
033100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1204.2
033200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1204.2
033300         MOVE "NO " TO ERROR-TOTAL                                NC1204.2
033400         ELSE                                                     NC1204.2
033500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1204.2
033600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1204.2
033700     PERFORM WRITE-LINE.                                          NC1204.2
033800 END-ROUTINE-13.                                                  NC1204.2
033900     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1204.2
034000         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1204.2
034100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1204.2
034200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1204.2
034300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1204.2
034400      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1204.2
034500          MOVE "NO " TO ERROR-TOTAL                               NC1204.2
034600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1204.2
034700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1204.2
034800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1204.2
034900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1204.2
035000 WRITE-LINE.                                                      NC1204.2
035100     ADD 1 TO RECORD-COUNT.                                       NC1204.2
035200*    IF RECORD-COUNT GREATER 42                                   NC1204.2
035300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1204.2
035400*        MOVE SPACE TO DUMMY-RECORD                               NC1204.2
035500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1204.2
035600*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1204.2
035700*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1204.2
035800*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1204.2
035900*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1204.2
036000*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1204.2
036100*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1204.2
036200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1204.2
036300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1204.2
036400*        MOVE ZERO TO RECORD-COUNT.                               NC1204.2
036500     PERFORM WRT-LN.                                              NC1204.2
036600 WRT-LN.                                                          NC1204.2
036700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1204.2
036800     MOVE SPACE TO DUMMY-RECORD.                                  NC1204.2
036900 BLANK-LINE-PRINT.                                                NC1204.2
037000     PERFORM WRT-LN.                                              NC1204.2
037100 FAIL-ROUTINE.                                                    NC1204.2
037200     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1204.2
037300            GO TO FAIL-ROUTINE-WRITE.                             NC1204.2
037400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1204.2
037500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1204.2
037600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1204.2
037700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1204.2
037800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1204.2
037900     GO TO  FAIL-ROUTINE-EX.                                      NC1204.2
038000 FAIL-ROUTINE-WRITE.                                              NC1204.2
038100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1204.2
038200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1204.2
038300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1204.2
038400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1204.2
038500 FAIL-ROUTINE-EX. EXIT.                                           NC1204.2
038600 BAIL-OUT.                                                        NC1204.2
038700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1204.2
038800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1204.2
038900 BAIL-OUT-WRITE.                                                  NC1204.2
039000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1204.2
039100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1204.2
039200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1204.2
039300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1204.2
039400 BAIL-OUT-EX. EXIT.                                               NC1204.2
039500 CCVS1-EXIT.                                                      NC1204.2
039600     EXIT.                                                        NC1204.2
039700 SECTION-NC120A-001 SECTION.                                      NC1204.2
039800 SIG-INIT-GF-14.                                                  NC1204.2
039900     MOVE  "MULTIPLY BY GIVING" TO FEATURE.                       NC1204.2
040000     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
040100     MOVE   80.12  TO MULT1.                                      NC1204.2
040200     MOVE    0     TO MULT2.                                      NC1204.2
040300 SIG-TEST-GF-14-0.                                                NC1204.2
040400     MULTIPLY MULT1 BY 4.3 GIVING MULT2.                          NC1204.2
040500 SIG-TEST-GF-14-1.                                                NC1204.2
040600     IF       MULT2 NOT EQUAL TO 344.516                          NC1204.2
040700              GO TO SIG-FAIL-GF-14.                               NC1204.2
040800     PERFORM  PASS                                                NC1204.2
040900              GO TO SIG-WRITE-GF-14.                              NC1204.2
041000 SIG-DELETE-GF-14.                                                NC1204.2
041100     PERFORM  DE-LETE.                                            NC1204.2
041200     GO TO    SIG-WRITE-GF-14.                                    NC1204.2
041300 SIG-FAIL-GF-14.                                                  NC1204.2
041400     PERFORM  FAIL.                                               NC1204.2
041500     MOVE     MULT2 TO COMPUTED-N.                                NC1204.2
041600     MOVE     +344.516 TO CORRECT-N.                              NC1204.2
041700 SIG-WRITE-GF-14.                                                 NC1204.2
041800     MOVE "SIG-TEST-GF-14" TO PAR-NAME.                           NC1204.2
041900     PERFORM  PRINT-DETAIL.                                       NC1204.2
042000 SIG-INIT-GF-15.                                                  NC1204.2
042100     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
042200     MOVE   80.12  TO MULT1.                                      NC1204.2
042300     MOVE     0    TO MULT3.                                      NC1204.2
042400 SIG-TEST-GF-15-0.                                                NC1204.2
042500     MULTIPLY .9 BY MULT1 GIVING MULT3 ROUNDED.                   NC1204.2
042600 SIG-TEST-GF-15-1.                                                NC1204.2
042700     IF       MULT3 NOT EQUAL TO " $72.11"                        NC1204.2
042800              GO TO SIG-FAIL-GF-15.                               NC1204.2
042900     PERFORM  PASS.                                               NC1204.2
043000     GO TO    SIG-WRITE-GF-15.                                    NC1204.2
043100 SIG-DELETE-GF-15.                                                NC1204.2
043200     PERFORM  DE-LETE.                                            NC1204.2
043300     GO TO    SIG-WRITE-GF-15.                                    NC1204.2
043400 SIG-FAIL-GF-15.                                                  NC1204.2
043500     PERFORM  FAIL.                                               NC1204.2
043600     MOVE     MULT3 TO COMPUTED-A.                                NC1204.2
043700     MOVE " l72.11" TO CORRECT-A.                                 NC1204.2
043800 SIG-WRITE-GF-15.                                                 NC1204.2
043900     MOVE "SIG-TEST-GF-15 " TO PAR-NAME.                          NC1204.2
044000     PERFORM  PRINT-DETAIL.                                       NC1204.2
044100 SIG-INIT-GF-16.                                                  NC1204.2
044200     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
044300     MOVE   -56    TO MULT4.                                      NC1204.2
044400     MOVE    80.12 TO MULT1.                                      NC1204.2
044500     MOVE     4    TO MULT5.                                      NC1204.2
044600 SIG-TEST-GF-16-0.                                                NC1204.2
044700     MULTIPLY MULT4 BY MULT1 GIVING MULT5 ON SIZE ERROR           NC1204.2
044800              MOVE "H" TO XRAY.                                   NC1204.2
044900 SIG-TEST-GF-16-1.                                                NC1204.2
045000     IF       XRAY EQUAL TO "H"                                   NC1204.2
045100              PERFORM PASS                                        NC1204.2
045200              ELSE                                                NC1204.2
045300              GO TO SIG-FAIL-GF-16-1.                             NC1204.2
045400     GO TO    SIG-WRITE-GF-16-1.                                  NC1204.2
045500 SIG-DELETE-GF-16-1.                                              NC1204.2
045600     PERFORM  DE-LETE.                                            NC1204.2
045700     GO TO    SIG-WRITE-GF-16-1.                                  NC1204.2
045800 SIG-FAIL-GF-16-1.                                                NC1204.2
045900     MOVE     MULT5 TO COMPUTED-N.                                NC1204.2
046000     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1204.2
046100     PERFORM  FAIL.                                               NC1204.2
046200 SIG-WRITE-GF-16-1.                                               NC1204.2
046300     MOVE "SIG-TEST-GF-16-1 " TO PAR-NAME.                        NC1204.2
046400     PERFORM  PRINT-DETAIL.                                       NC1204.2
046500 SIG-TEST-GF-16-2.                                                NC1204.2
046600     IF       MULT5 NOT EQUAL TO 4                                NC1204.2
046700              GO TO SIG-FAIL-GF-16-2.                             NC1204.2
046800     PERFORM  PASS.                                               NC1204.2
046900     GO TO    SIG-WRITE-GF-16-2.                                  NC1204.2
047000 SIG-DELETE-GF-16-2.                                              NC1204.2
047100     PERFORM  DE-LETE.                                            NC1204.2
047200     GO TO    SIG-WRITE-GF-16-2.                                  NC1204.2
047300 SIG-FAIL-GF-16-2.                                                NC1204.2
047400     PERFORM  FAIL.                                               NC1204.2
047500     MOVE     MULT5 TO COMPUTED-N.                                NC1204.2
047600     MOVE     +4 TO CORRECT-N.                                    NC1204.2
047700     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1204.2
047800 SIG-WRITE-GF-16-2.                                               NC1204.2
047900     MOVE "SIG-TEST-GF-16-2 " TO PAR-NAME.                        NC1204.2
048000     PERFORM  PRINT-DETAIL.                                       NC1204.2
048100 SIG-INIT-GF-1.                                                   NC1204.2
048200     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
048300     MOVE     4 TO MULT5.                                         NC1204.2
048400     MOVE   "A" TO XRAY.                                          NC1204.2
048500 SIG-TEST-GF-1-0.                                                 NC1204.2
048600     MULTIPLY 3.3 BY -3 GIVING MULT5 ROUNDED ON SIZE ERROR        NC1204.2
048700              MOVE "J" TO XRAY.                                   NC1204.2
048800 SIG-TEST-GF-1-1.                                                 NC1204.2
048900     IF       XRAY NOT EQUAL TO "J"                               NC1204.2
049000              GO TO SIG-FAIL-GF-1-1                               NC1204.2
049100              ELSE                                                NC1204.2
049200              PERFORM PASS.                                       NC1204.2
049300     GO TO    SIG-WRITE-GF-1-1.                                   NC1204.2
049400 SIG-DELETE-GF-1-1.                                               NC1204.2
049500     PERFORM  DE-LETE.                                            NC1204.2
049600     GO TO    SIG-WRITE-GF-1-1.                                   NC1204.2
049700 SIG-FAIL-GF-1-1.                                                 NC1204.2
049800     MOVE     MULT5 TO COMPUTED-N.                                NC1204.2
049900     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1204.2
050000     PERFORM  FAIL.                                               NC1204.2
050100 SIG-WRITE-GF-1-1.                                                NC1204.2
050200     MOVE "SIG-TEST-GF-1-1 " TO PAR-NAME.                         NC1204.2
050300     PERFORM  PRINT-DETAIL.                                       NC1204.2
050400 SIG-TEST-GF-1-2.                                                 NC1204.2
050500     IF       MULT5 EQUAL TO 4                                    NC1204.2
050600              PERFORM PASS                                        NC1204.2
050700              ELSE                                                NC1204.2
050800              GO TO SIG-FAIL-GF-1-2.                              NC1204.2
050900     GO TO    SIG-WRITE-GF-1-2.                                   NC1204.2
051000 SIG-DELETE-GF-1-2.                                               NC1204.2
051100     PERFORM  DE-LETE.                                            NC1204.2
051200     GO TO    SIG-WRITE-GF-1-2.                                   NC1204.2
051300 SIG-FAIL-GF-1-2.                                                 NC1204.2
051400     PERFORM  FAIL.                                               NC1204.2
051500     MOVE     MULT5 TO COMPUTED-N.                                NC1204.2
051600     MOVE     +4 TO CORRECT-N.                                    NC1204.2
051700     MOVE     4 TO MULT5.                                         NC1204.2
051800     MOVE     "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.        NC1204.2
051900 SIG-WRITE-GF-1-2.                                                NC1204.2
052000     MOVE "SIG-TEST-GF-1-2 " TO PAR-NAME.                         NC1204.2
052100     PERFORM  PRINT-DETAIL.                                       NC1204.2
052200 SIG-INIT-GF-2.                                                   NC1204.2
052300     MOVE "MULTIPLY BY" TO FEATURE.                               NC1204.2
052400     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
052500     MOVE     4 TO MULT5.                                         NC1204.2
052600 SIG-TEST-GF-2-0.                                                 NC1204.2
052700     MULTIPLY MULT5 BY MULT1.                                     NC1204.2
052800 SIG-TEST-GF-2-1.                                                 NC1204.2
052900     IF       MULT1 EQUAL TO 320.48                               NC1204.2
053000              PERFORM PASS                                        NC1204.2
053100              ELSE                                                NC1204.2
053200              GO TO SIG-FAIL-GF-2.                                NC1204.2
053300     GO TO    SIG-WRITE-GF-2.                                     NC1204.2
053400 SIG-DELETE-GF-2.                                                 NC1204.2
053500     PERFORM  DE-LETE.                                            NC1204.2
053600     GO TO    SIG-WRITE-GF-2.                                     NC1204.2
053700 SIG-FAIL-GF-2.                                                   NC1204.2
053800     PERFORM  FAIL.                                               NC1204.2
053900     MOVE     MULT1 TO COMPUTED-N.                                NC1204.2
054000     MOVE     +320.48 TO CORRECT-N.                               NC1204.2
054100 SIG-WRITE-GF-2.                                                  NC1204.2
054200     MOVE "SIG-TEST-GF-2 " TO PAR-NAME.                           NC1204.2
054300     PERFORM  PRINT-DETAIL.                                       NC1204.2
054400 SIG-INIT-GF-3.                                                   NC1204.2
054500     MOVE "MULTIPLY BY" TO FEATURE.                               NC1204.2
054600     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
054700     MOVE   -56 TO MULT4.                                         NC1204.2
054800 SIG-TEST-GF-3-0.                                                 NC1204.2
054900     MULTIPLY -1.3 BY MULT4 ROUNDED.                              NC1204.2
055000 SIG-TEST-GF-3-1.                                                 NC1204.2
055100     IF       MULT4 EQUAL TO 73                                   NC1204.2
055200              PERFORM PASS                                        NC1204.2
055300              ELSE                                                NC1204.2
055400              GO TO SIG-FAIL-GF-3.                                NC1204.2
055500     GO TO    SIG-WRITE-GF-3.                                     NC1204.2
055600 SIG-DELETE-GF-3.                                                 NC1204.2
055700     PERFORM  DE-LETE.                                            NC1204.2
055800     GO TO    SIG-WRITE-GF-3.                                     NC1204.2
055900 SIG-FAIL-GF-3.                                                   NC1204.2
056000     PERFORM  FAIL.                                               NC1204.2
056100     MOVE     MULT4 TO COMPUTED-N.                                NC1204.2
056200     MOVE     +73 TO CORRECT-N.                                   NC1204.2
056300 SIG-WRITE-GF-3.                                                  NC1204.2
056400     MOVE "SIG-TEST-GF-3 " TO PAR-NAME.                           NC1204.2
056500     PERFORM  PRINT-DETAIL.                                       NC1204.2
056600 SIG-INIT-GF-4.                                                   NC1204.2
056700     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
056800     MOVE     4 TO MULT5.                                         NC1204.2
056900     MOVE   "B" TO XRAY.                                          NC1204.2
057000 SIG-TEST-GF-4-0.                                                 NC1204.2
057100     MULTIPLY MULT5 BY MULT5 ON SIZE ERROR                        NC1204.2
057200              MOVE "K" TO XRAY.                                   NC1204.2
057300 SIG-TEST-GF-4-1.                                                 NC1204.2
057400     IF       XRAY EQUAL TO "K"                                   NC1204.2
057500              PERFORM PASS                                        NC1204.2
057600              ELSE                                                NC1204.2
057700              GO TO SIG-FAIL-GF-4-1.                              NC1204.2
057800     GO TO    SIG-WRITE-GF-4-1.                                   NC1204.2
057900 SIG-DELETE-GF-4-1.                                               NC1204.2
058000     PERFORM  DE-LETE.                                            NC1204.2
058100     GO TO    SIG-WRITE-GF-4-1.                                   NC1204.2
058200 SIG-FAIL-GF-4-1.                                                 NC1204.2
058300     MOVE     MULT5 TO COMPUTED-N.                                NC1204.2
058400     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1204.2
058500     PERFORM  FAIL.                                               NC1204.2
058600 SIG-WRITE-GF-4-1.                                                NC1204.2
058700     MOVE "SIG-TEST-GF-4-1 " TO PAR-NAME.                         NC1204.2
058800     PERFORM  PRINT-DETAIL.                                       NC1204.2
058900 SIG-TEST-GF-4-2.                                                 NC1204.2
059000     IF       MULT5 EQUAL TO 4                                    NC1204.2
059100              PERFORM PASS                                        NC1204.2
059200              ELSE                                                NC1204.2
059300              GO TO SIG-FAIL-GF-4-2.                              NC1204.2
059400     GO TO    SIG-WRITE-GF-4-2.                                   NC1204.2
059500 SIG-DELETE-GF-4-2.                                               NC1204.2
059600     PERFORM  DE-LETE.                                            NC1204.2
059700     GO TO    SIG-WRITE-GF-4-2.                                   NC1204.2
059800 SIG-FAIL-GF-4-2.                                                 NC1204.2
059900     PERFORM  FAIL.                                               NC1204.2
060000     MOVE     MULT5 TO COMPUTED-N.                                NC1204.2
060100     MOVE     +4 TO CORRECT-N.                                    NC1204.2
060200     MOVE "NOT AFFECTED BY SIZE ERROR" TO RE-MARK.                NC1204.2
060300 SIG-WRITE-GF-4-2.                                                NC1204.2
060400     MOVE "SIG-TEST-GF-4-2" TO PAR-NAME.                          NC1204.2
060500     PERFORM  PRINT-DETAIL.                                       NC1204.2
060600 SIG-INIT-GF-5.                                                   NC1204.2
060700     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
060800     MOVE    20 TO MULT6.                                         NC1204.2
060900     MOVE   "C" TO XRAY.                                          NC1204.2
061000 SIG-TEST-GF-5-0.                                                 NC1204.2
061100     MULTIPLY 4.99 BY MULT6 ROUNDED ON SIZE ERROR                 NC1204.2
061200              MOVE "L" TO XRAY.                                   NC1204.2
061300 SIG-TEST-GF-5-1.                                                 NC1204.2
061400     IF       XRAY EQUAL TO "L"                                   NC1204.2
061500              PERFORM PASS                                        NC1204.2
061600              ELSE                                                NC1204.2
061700              GO TO SIG-FAIL-GF-5-1.                              NC1204.2
061800     GO TO    SIG-WRITE-GF-5-1.                                   NC1204.2
061900 SIG-DELETE-GF-5-1.                                               NC1204.2
062000     PERFORM  DE-LETE.                                            NC1204.2
062100     GO TO    SIG-WRITE-GF-5-1.                                   NC1204.2
062200 SIG-FAIL-GF-5-1.                                                 NC1204.2
062300     MOVE     MULT6 TO COMPUTED-N.                                NC1204.2
062400     MOVE "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.                NC1204.2
062500     PERFORM  FAIL.                                               NC1204.2
062600 SIG-WRITE-GF-5-1.                                                NC1204.2
062700     MOVE "SIG-TEST-GF-5-1" TO PAR-NAME.                          NC1204.2
062800     PERFORM  PRINT-DETAIL.                                       NC1204.2
062900 SIG-TEST-GF-5-2.                                                 NC1204.2
063000     IF       MULT6 EQUAL TO 20                                   NC1204.2
063100              PERFORM PASS                                        NC1204.2
063200              ELSE                                                NC1204.2
063300              GO TO SIG-FAIL-GF-5-2.                              NC1204.2
063400     GO TO    SIG-WRITE-GF-5-2.                                   NC1204.2
063500 SIG-DELETE-GF-5-2.                                               NC1204.2
063600     PERFORM  DE-LETE.                                            NC1204.2
063700     GO TO    SIG-WRITE-GF-5-2.                                   NC1204.2
063800 SIG-FAIL-GF-5-2.                                                 NC1204.2
063900     PERFORM  FAIL.                                               NC1204.2
064000     MOVE     MULT6 TO COMPUTED-N.                                NC1204.2
064100     MOVE     +20 TO CORRECT-N.                                   NC1204.2
064200     MOVE "NOT AFFECTED BY SIZE ERROR" TO RE-MARK.                NC1204.2
064300 SIG-WRITE-GF-5-2.                                                NC1204.2
064400     MOVE "SIG-TEST-GF-5-2" TO PAR-NAME.                          NC1204.2
064500     PERFORM  PRINT-DETAIL.                                       NC1204.2
064600 SIG-INIT-GF-6.                                                   NC1204.2
064700     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
064800     MOVE 222222222222 TO WRK-DS-LS-18V00.                        NC1204.2
064900 SIG-TEST-GF-6-0.                                                 NC1204.2
065000     MULTIPLY A06THREES-DS-LS-03V03 BY WRK-DS-LS-18V00.           NC1204.2
065100 SIG-TEST-GF-6-1.                                                 NC1204.2
065200     IF WRK-DS-LS-18V00 EQUAL TO 000074073999999925               NC1204.2
065300         PERFORM PASS                                             NC1204.2
065400         GO TO SIG-WRITE-GF-6.                                    NC1204.2
065500     GO TO SIG-FAIL-GF-6.                                         NC1204.2
065600 SIG-DELETE-GF-6.                                                 NC1204.2
065700     PERFORM DE-LETE.                                             NC1204.2
065800     GO TO SIG-WRITE-GF-6.                                        NC1204.2
065900 SIG-FAIL-GF-6.                                                   NC1204.2
066000     MOVE  000074073999999925 TO CORRECT-18V0.                    NC1204.2
066100     MOVE  WRK-DS-LS-18V00       TO COMPUTED-18V0.                NC1204.2
066200     PERFORM FAIL.                                                NC1204.2
066300 SIG-WRITE-GF-6.                                                  NC1204.2
066400     MOVE "SIG-TEST-GF-6         " TO PAR-NAME.                   NC1204.2
066500     PERFORM PRINT-DETAIL.                                        NC1204.2
066600 SIG-INIT-GF-7.                                                   NC1204.2
066700     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
066800     MOVE A08TWOS-DS-02V06 TO WRK-DS-TS-06V06.                    NC1204.2
066900 SIG-TEST-GF-7-0.                                                 NC1204.2
067000     MULTIPLY 0.4 BY WRK-DS-TS-06V06 ROUNDED.                     NC1204.2
067100 SIG-TEST-GF-7-1.                                                 NC1204.2
067200     IF WRK-DS-TS-12V00-S-S EQUAL TO 000008888889                 NC1204.2
067300         PERFORM PASS                                             NC1204.2
067400         GO TO SIG-WRITE-GF-7.                                    NC1204.2
067500     GO TO SIG-FAIL-GF-7.                                         NC1204.2
067600 SIG-DELETE-GF-7.                                                 NC1204.2
067700     PERFORM DE-LETE.                                             NC1204.2
067800     GO TO SIG-WRITE-GF-7.                                        NC1204.2
067900 SIG-FAIL-GF-7.                                                   NC1204.2
068000     MOVE  WRK-DS-TS-12V00-S-S     TO COMPUTED-18V0.              NC1204.2
068100     MOVE  000008888889       TO CORRECT-18V0.                    NC1204.2
068200     PERFORM FAIL.                                                NC1204.2
068300 SIG-WRITE-GF-7.                                                  NC1204.2
068400     MOVE "SIG-TEST-GF-7         " TO PAR-NAME.                   NC1204.2
068500     PERFORM PRINT-DETAIL.                                        NC1204.2
068600 SIG-INIT-GF-8.                                                   NC1204.2
068700     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
068800     MOVE "0" TO WRK-XN-00001.                                    NC1204.2
068900     MOVE A10ONES-DS-T-10V00 TO WRK-DS-10V00.                     NC1204.2
069000 SIG-TEST-GF-8-0.                                                 NC1204.2
069100     MULTIPLY A12THREES-DS-LS-06V06 BY WRK-DS-10V00               NC1204.2
069200         ON SIZE ERROR MOVE "1" TO WRK-XN-00001.                  NC1204.2
069300 SIG-TEST-GF-8-1.                                                 NC1204.2
069400     IF WRK-DS-10V00 EQUAL TO 1111111111                          NC1204.2
069500         PERFORM PASS                                             NC1204.2
069600         GO TO SIG-WRITE-GF-8-1.                                  NC1204.2
069700     GO TO SIG-FAIL-GF-8-1.                                       NC1204.2
069800 SIG-DELETE-GF-8-1.                                               NC1204.2
069900     PERFORM DE-LETE.                                             NC1204.2
070000     GO TO SIG-WRITE-GF-8-1.                                      NC1204.2
070100 SIG-FAIL-GF-8-1.                                                 NC1204.2
070200     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1204.2
070300     MOVE  1111111111         TO CORRECT-18V0.                    NC1204.2
070400     PERFORM FAIL.                                                NC1204.2
070500 SIG-WRITE-GF-8-1.                                                NC1204.2
070600     MOVE "SIG-TEST-GF-8-1         " TO PAR-NAME.                 NC1204.2
070700     PERFORM PRINT-DETAIL.                                        NC1204.2
070800 SIG-TEST-GF-8-2.                                                 NC1204.2
070900     IF WRK-XN-00001 EQUAL TO "1"                                 NC1204.2
071000         PERFORM PASS                                             NC1204.2
071100         GO TO SIG-WRITE-GF-8-2.                                  NC1204.2
071200     MOVE "1" TO CORRECT-A.                                       NC1204.2
071300     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1204.2
071400     PERFORM FAIL.                                                NC1204.2
071500     GO TO SIG-WRITE-GF-8-2.                                      NC1204.2
071600 SIG-DELETE-GF-8-2.                                               NC1204.2
071700     PERFORM DE-LETE.                                             NC1204.2
071800 SIG-WRITE-GF-8-2.                                                NC1204.2
071900     MOVE "SIG-TEST-GF-8-2         " TO PAR-NAME.                 NC1204.2
072000     PERFORM PRINT-DETAIL.                                        NC1204.2
072100 SIG-INIT-GF-9.                                                   NC1204.2
072200     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
072300     MOVE "1" TO WRK-XN-00001.                                    NC1204.2
072400     MOVE -99 TO WRK-DS-02V00.                                    NC1204.2
072500 SIG-TEST-GF-9-0.                                                 NC1204.2
072600     MULTIPLY AZERO-DS-LS-05V05 BY WRK-DS-02V00                   NC1204.2
072700         ON SIZE ERROR MOVE "0" TO WRK-XN-00001.                  NC1204.2
072800 SIG-TEST-GF-9-1.                                                 NC1204.2
072900     IF WRK-DS-02V00 EQUAL TO 00                                  NC1204.2
073000         PERFORM PASS                                             NC1204.2
073100         GO TO SIG-WRITE-GF-9-1.                                  NC1204.2
073200     GO TO SIG-FAIL-GF-9-1.                                       NC1204.2
073300 SIG-DELETE-GF-9-1.                                               NC1204.2
073400     PERFORM DE-LETE.                                             NC1204.2
073500     GO TO SIG-WRITE-GF-9-1.                                      NC1204.2
073600 SIG-FAIL-GF-9-1.                                                 NC1204.2
073700     MOVE  WRK-DS-02V00       TO COMPUTED-N.                      NC1204.2
073800     MOVE  00                 TO CORRECT-N.                       NC1204.2
073900     PERFORM FAIL.                                                NC1204.2
074000 SIG-WRITE-GF-9-1.                                                NC1204.2
074100     MOVE "SIG-TEST-GF-9-1         " TO PAR-NAME.                 NC1204.2
074200     PERFORM PRINT-DETAIL.                                        NC1204.2
074300 SIG-TEST-GF-9-2.                                                 NC1204.2
074400     IF WRK-XN-00001 EQUAL TO "1"                                 NC1204.2
074500         PERFORM PASS                                             NC1204.2
074600         GO TO SIG-WRITE-GF-9-2.                                  NC1204.2
074700     MOVE "1" TO CORRECT-A.                                       NC1204.2
074800     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1204.2
074900     PERFORM FAIL.                                                NC1204.2
075000     GO TO SIG-WRITE-GF-9-2.                                      NC1204.2
075100 SIG-DELETE-GF-9-2.                                               NC1204.2
075200     PERFORM DE-LETE.                                             NC1204.2
075300 SIG-WRITE-GF-9-2.                                                NC1204.2
075400     MOVE "SIG-TEST-GF-9-2         " TO PAR-NAME.                 NC1204.2
075500     PERFORM PRINT-DETAIL.                                        NC1204.2
075600 SIG-INIT-GF-10.                                                  NC1204.2
075700     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
075800     MOVE "0" TO WRK-XN-00001.                                    NC1204.2
075900     MOVE -01 TO WRK-DS-02V00.                                    NC1204.2
076000 SIG-TEST-GF-10-0.                                                NC1204.2
076100     MULTIPLY 99.5 BY WRK-DS-02V00 ROUNDED                        NC1204.2
076200         ON SIZE ERROR MOVE "1" TO WRK-XN-00001.                  NC1204.2
076300 SIG-TEST-GF-10-1.                                                NC1204.2
076400     IF WRK-DS-02V00 EQUAL TO -01                                 NC1204.2
076500         PERFORM PASS                                             NC1204.2
076600         GO TO SIG-WRITE-GF-10-1.                                 NC1204.2
076700     GO TO SIG-FAIL-GF-10-1.                                      NC1204.2
076800 SIG-DELETE-GF-10-1.                                              NC1204.2
076900     PERFORM DE-LETE.                                             NC1204.2
077000     GO TO SIG-WRITE-GF-10-1.                                     NC1204.2
077100 SIG-FAIL-GF-10-1.                                                NC1204.2
077200     MOVE  WRK-DS-02V00       TO COMPUTED-N.                      NC1204.2
077300     MOVE  -01                TO CORRECT-N.                       NC1204.2
077400     PERFORM FAIL.                                                NC1204.2
077500 SIG-WRITE-GF-10-1.                                               NC1204.2
077600     MOVE "SIG-TEST-GF-10-1         " TO PAR-NAME.                NC1204.2
077700     PERFORM PRINT-DETAIL.                                        NC1204.2
077800 SIG-TEST-GF-10-2.                                                NC1204.2
077900     IF WRK-XN-00001 EQUAL TO "1"                                 NC1204.2
078000         PERFORM PASS                                             NC1204.2
078100         GO TO SIG-WRITE-GF-10-2.                                 NC1204.2
078200     MOVE "1" TO CORRECT-A.                                       NC1204.2
078300     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1204.2
078400     PERFORM FAIL.                                                NC1204.2
078500     GO TO SIG-WRITE-GF-10-2.                                     NC1204.2
078600 SIG-DELETE-GF-10-2.                                              NC1204.2
078700     PERFORM DE-LETE.                                             NC1204.2
078800 SIG-WRITE-GF-10-2.                                               NC1204.2
078900     MOVE "SIG-TEST-GF-10-2         " TO PAR-NAME.                NC1204.2
079000     PERFORM PRINT-DETAIL.                                        NC1204.2
079100 SIG-INIT-GF-11.                                                  NC1204.2
079200     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
079300     MOVE "1" TO WRK-XN-00001.                                    NC1204.2
079400     MOVE -01 TO WRK-DS-02V00.                                    NC1204.2
079500 SIG-TEST-GF-11-0.                                                NC1204.2
079600     MULTIPLY 99.4 BY WRK-DS-02V00 ROUNDED                        NC1204.2
079700         ON SIZE ERROR MOVE "0" TO WRK-XN-00001.                  NC1204.2
079800 SIG-TEST-GF-11-1.                                                NC1204.2
079900     IF WRK-DS-02V00 EQUAL TO -99                                 NC1204.2
080000         PERFORM PASS                                             NC1204.2
080100         GO TO SIG-WRITE-GF-11-1.                                 NC1204.2
080200     GO TO SIG-FAIL-GF-11-1.                                      NC1204.2
080300 SIG-DELETE-GF-11-1.                                              NC1204.2
080400     PERFORM DE-LETE.                                             NC1204.2
080500     GO TO SIG-WRITE-GF-11-1.                                     NC1204.2
080600 SIG-FAIL-GF-11-1.                                                NC1204.2
080700     MOVE  WRK-DS-02V00       TO COMPUTED-N.                      NC1204.2
080800     MOVE  -99                TO CORRECT-N.                       NC1204.2
080900     PERFORM FAIL.                                                NC1204.2
081000 SIG-WRITE-GF-11-1.                                               NC1204.2
081100     MOVE "SIG-TEST-GF-11-1         " TO PAR-NAME.                NC1204.2
081200     PERFORM PRINT-DETAIL.                                        NC1204.2
081300 SIG-TEST-GF-11-2.                                                NC1204.2
081400     IF WRK-XN-00001 EQUAL TO "1"                                 NC1204.2
081500         PERFORM PASS                                             NC1204.2
081600         GO TO SIG-WRITE-GF-11-2.                                 NC1204.2
081700     MOVE "1" TO CORRECT-A.                                       NC1204.2
081800     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1204.2
081900     PERFORM FAIL.                                                NC1204.2
082000     GO TO SIG-WRITE-GF-11-2.                                     NC1204.2
082100 SIG-DELETE-GF-11-2.                                              NC1204.2
082200     PERFORM DE-LETE.                                             NC1204.2
082300 SIG-WRITE-GF-11-2.                                               NC1204.2
082400     MOVE "SIG-TEST-GF-11-2         " TO PAR-NAME.                NC1204.2
082500     PERFORM PRINT-DETAIL.                                        NC1204.2
082600 SIG-INIT-GF-17.                                                  NC1204.2
082700     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
082800     MOVE "MULTIPLY BY GIVING  " TO FEATURE.                      NC1204.2
082900     MOVE ZERO TO WRK-DS-09V09.                                   NC1204.2
083000 SIG-TEST-GF-17-0.                                                NC1204.2
083100     MULTIPLY A06THREES-DS-LS-03V03 BY A12THREES-DS-LS-06V06      NC1204.2
083200         GIVING WRK-DS-09V09.                                     NC1204.2
083300 SIG-TEST-GF-17-1.                                                NC1204.2
083400     IF WRK-DS-18V00-S EQUAL TO 111110999999888889                NC1204.2
083500         PERFORM PASS                                             NC1204.2
083600         GO TO SIG-WRITE-GF-17.                                   NC1204.2
083700     GO TO SIG-FAIL-GF-17.                                        NC1204.2
083800 SIG-DELETE-GF-17.                                                NC1204.2
083900     PERFORM DE-LETE.                                             NC1204.2
084000     GO TO SIG-WRITE-GF-17.                                       NC1204.2
084100 SIG-FAIL-GF-17.                                                  NC1204.2
084200     MOVE  111110999999888889 TO CORRECT-18V0.                    NC1204.2
084300     MOVE  WRK-DS-18V00-S     TO COMPUTED-18V0.                   NC1204.2
084400     PERFORM FAIL.                                                NC1204.2
084500 SIG-WRITE-GF-17.                                                 NC1204.2
084600     MOVE "SIG-TEST-GF-17         " TO PAR-NAME.                  NC1204.2
084700     PERFORM PRINT-DETAIL.                                        NC1204.2
084800 SIG-INIT-GF-18.                                                  NC1204.2
084900     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
085000     MOVE ZERO TO WRK-DS-10V00.                                   NC1204.2
085100 SIG-TEST-GF-18-0.                                                NC1204.2
085200     MULTIPLY A06THREES-DS-LS-03V03 BY A06THREES-DS-LS-03V03      NC1204.2
085300         GIVING WRK-DS-10V00 ROUNDED.                             NC1204.2
085400 SIG-TEST-GF-18-1.                                                NC1204.2
085500     IF WRK-DS-10V00 EQUAL TO 0000111111                          NC1204.2
085600         PERFORM PASS                                             NC1204.2
085700         GO TO SIG-WRITE-GF-18.                                   NC1204.2
085800     GO TO SIG-FAIL-GF-18.                                        NC1204.2
085900 SIG-DELETE-GF-18.                                                NC1204.2
086000     PERFORM DE-LETE.                                             NC1204.2
086100     GO TO SIG-WRITE-GF-18.                                       NC1204.2
086200 SIG-FAIL-GF-18.                                                  NC1204.2
086300     MOVE  0000111111         TO CORRECT-18V0.                    NC1204.2
086400     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1204.2
086500     PERFORM FAIL.                                                NC1204.2
086600 SIG-WRITE-GF-18.                                                 NC1204.2
086700     MOVE "SIG-TEST-GF-18         " TO PAR-NAME.                  NC1204.2
086800     PERFORM PRINT-DETAIL.                                        NC1204.2
086900 SIG-INIT-GF-19.                                                  NC1204.2
087000     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
087100     MOVE "0" TO WRK-XN-00001.                                    NC1204.2
087200     MOVE ZERO TO WRK-DS-10V00.                                   NC1204.2
087300 SIG-TEST-GF-19-0.                                                NC1204.2
087400     MULTIPLY A05ONES-DS-LS-00V05 BY A12ONES-DS-12V00             NC1204.2
087500         GIVING WRK-DS-10V00 ON SIZE ERROR                        NC1204.2
087600         MOVE "1" TO WRK-XN-00001.                                NC1204.2
087700 SIG-TEST-GF-19-1.                                                NC1204.2
087800     IF WRK-DS-10V00 EQUAL TO 0000000000                          NC1204.2
087900         PERFORM PASS                                             NC1204.2
088000         GO TO SIG-WRITE-GF-19-1.                                 NC1204.2
088100     GO TO SIG-FAIL-GF-19-1.                                      NC1204.2
088200 SIG-DELETE-GF-19-1.                                              NC1204.2
088300     PERFORM DE-LETE.                                             NC1204.2
088400     GO TO SIG-WRITE-GF-19-1.                                     NC1204.2
088500 SIG-FAIL-GF-19-1.                                                NC1204.2
088600     MOVE  0000000000         TO CORRECT-18V0.                    NC1204.2
088700     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1204.2
088800     PERFORM FAIL.                                                NC1204.2
088900 SIG-WRITE-GF-19-1.                                               NC1204.2
089000     MOVE "SIG-TEST-GF-19-1         " TO PAR-NAME.                NC1204.2
089100     PERFORM PRINT-DETAIL.                                        NC1204.2
089200 SIG-TEST-GF-19-2.                                                NC1204.2
089300     IF WRK-XN-00001 EQUAL TO "1"                                 NC1204.2
089400         PERFORM PASS                                             NC1204.2
089500         GO TO SIG-WRITE-GF-19-2.                                 NC1204.2
089600     MOVE "1" TO CORRECT-A.                                       NC1204.2
089700     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1204.2
089800     PERFORM FAIL.                                                NC1204.2
089900     GO TO SIG-WRITE-GF-19-2.                                     NC1204.2
090000 SIG-DELETE-GF-19-2.                                              NC1204.2
090100     PERFORM DE-LETE.                                             NC1204.2
090200 SIG-WRITE-GF-19-2.                                               NC1204.2
090300     MOVE "SIG-TEST-GF-19-2         " TO PAR-NAME.                NC1204.2
090400     PERFORM PRINT-DETAIL.                                        NC1204.2
090500 SIG-INIT-GF-20.                                                  NC1204.2
090600     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
090700     MOVE "1" TO WRK-XN-00001.                                    NC1204.2
090800     MOVE ZERO TO WRK-DS-10V00.                                   NC1204.2
090900 SIG-TEST-GF-20-0.                                                NC1204.2
091000     MULTIPLY A01ONE-DS-TS-P0801 BY A12ONES-DS-12V00              NC1204.2
091100         GIVING WRK-DS-10V00 ON SIZE ERROR                        NC1204.2
091200         MOVE "0" TO WRK-XN-00001.                                NC1204.2
091300 SIG-TEST-GF-20-1.                                                NC1204.2
091400     IF WRK-DS-10V00 EQUAL TO 0000000111                          NC1204.2
091500         PERFORM PASS                                             NC1204.2
091600         GO TO SIG-WRITE-GF-20-1.                                 NC1204.2
091700     GO TO SIG-FAIL-GF-20-1.                                      NC1204.2
091800 SIG-DELETE-GF-20-1.                                              NC1204.2
091900     PERFORM DE-LETE.                                             NC1204.2
092000     GO TO SIG-WRITE-GF-20-1.                                     NC1204.2
092100 SIG-FAIL-GF-20-1.                                                NC1204.2
092200     MOVE  0000000111         TO CORRECT-18V0.                    NC1204.2
092300     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1204.2
092400     PERFORM FAIL.                                                NC1204.2
092500 SIG-WRITE-GF-20-1.                                               NC1204.2
092600     MOVE "SIG-TEST-GF-20-1         " TO PAR-NAME.                NC1204.2
092700     PERFORM PRINT-DETAIL.                                        NC1204.2
092800 SIG-TEST-GF-20-2.                                                NC1204.2
092900     IF WRK-XN-00001 EQUAL TO "1"                                 NC1204.2
093000         PERFORM PASS                                             NC1204.2
093100         GO TO SIG-WRITE-GF-20-2.                                 NC1204.2
093200     MOVE "1" TO CORRECT-A.                                       NC1204.2
093300     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1204.2
093400     PERFORM FAIL.                                                NC1204.2
093500     GO TO SIG-WRITE-GF-20-2.                                     NC1204.2
093600 SIG-DELETE-GF-20-2.                                              NC1204.2
093700     PERFORM DE-LETE.                                             NC1204.2
093800 SIG-WRITE-GF-20-2.                                               NC1204.2
093900     MOVE "SIG-TEST-GF-20-2         " TO PAR-NAME.                NC1204.2
094000     PERFORM PRINT-DETAIL.                                        NC1204.2
094100 SIG-INIT-GF-21.                                                  NC1204.2
094200     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
094300     MOVE "0" TO WRK-XN-00001.                                    NC1204.2
094400     MOVE ZERO TO WRK-DS-10V00.                                   NC1204.2
094500 SIG-TEST-GF-21-0.                                                NC1204.2
094600     MULTIPLY 9.5 BY A10ONES-DS-T-10V00                           NC1204.2
094700         GIVING WRK-DS-10V00 ROUNDED ON SIZE ERROR                NC1204.2
094800         MOVE "1" TO WRK-XN-00001.                                NC1204.2
094900 SIG-TEST-GF-21-1.                                                NC1204.2
095000     IF WRK-DS-10V00 EQUAL TO 0000000000                          NC1204.2
095100         PERFORM PASS                                             NC1204.2
095200         GO TO SIG-WRITE-GF-21-1.                                 NC1204.2
095300     MOVE  0000000000         TO CORRECT-18V0.                    NC1204.2
095400     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1204.2
095500     PERFORM FAIL.                                                NC1204.2
095600     GO TO SIG-WRITE-GF-21-1.                                     NC1204.2
095700 SIG-DELETE-GF-21-1.                                              NC1204.2
095800     PERFORM DE-LETE.                                             NC1204.2
095900 SIG-WRITE-GF-21-1.                                               NC1204.2
096000     MOVE "SIG-TEST-GF-21-1         " TO PAR-NAME.                NC1204.2
096100     PERFORM PRINT-DETAIL.                                        NC1204.2
096200 SIG-TEST-GF-21-2.                                                NC1204.2
096300     IF WRK-XN-00001 EQUAL TO "1"                                 NC1204.2
096400         PERFORM PASS                                             NC1204.2
096500         GO TO SIG-WRITE-GF-21-2.                                 NC1204.2
096600     MOVE "1" TO CORRECT-A.                                       NC1204.2
096700     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1204.2
096800     PERFORM FAIL.                                                NC1204.2
096900     GO TO SIG-WRITE-GF-21-2.                                     NC1204.2
097000 SIG-DELETE-GF-21-2.                                              NC1204.2
097100     PERFORM DE-LETE.                                             NC1204.2
097200 SIG-WRITE-GF-21-2.                                               NC1204.2
097300     MOVE "SIG-TEST-GF-21-2         " TO PAR-NAME.                NC1204.2
097400     PERFORM PRINT-DETAIL.                                        NC1204.2
097500 SIG-INIT-GF-22.                                                  NC1204.2
097600     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
097700     MOVE "1" TO WRK-XN-00001.                                    NC1204.2
097800     MOVE ZERO TO WRK-DS-T-09V08.                                 NC1204.2
097900 SIG-TEST-GF-22-0.                                                NC1204.2
098000     MULTIPLY A01ONE-DS-TS-P0801 BY A18ONES-DS-18V00              NC1204.2
098100         GIVING WRK-DS-T-09V08 ROUNDED ON SIZE ERROR              NC1204.2
098200         MOVE "0" TO WRK-XN-00001.                                NC1204.2
098300 SIG-TEST-GF-22-1.                                                NC1204.2
098400     IF WKR-DS-T-17V00-S EQUAL TO 11111111111111111               NC1204.2
098500         PERFORM PASS                                             NC1204.2
098600         GO TO SIG-WRITE-GF-22-1.                                 NC1204.2
098700     MOVE  11111111111111111  TO CORRECT-18V0.                    NC1204.2
098800     MOVE  WKR-DS-T-17V00-S     TO COMPUTED-18V0.                 NC1204.2
098900     PERFORM FAIL.                                                NC1204.2
099000     GO TO SIG-WRITE-GF-22-1.                                     NC1204.2
099100 SIG-DELETE-GF-22-1.                                              NC1204.2
099200     PERFORM DE-LETE.                                             NC1204.2
099300 SIG-WRITE-GF-22-1.                                               NC1204.2
099400     MOVE "SIG-TEST-GF-22-1         " TO PAR-NAME.                NC1204.2
099500     PERFORM PRINT-DETAIL.                                        NC1204.2
099600 SIG-TEST-GF-22-2.                                                NC1204.2
099700     IF WRK-XN-00001 EQUAL TO "1"                                 NC1204.2
099800         PERFORM PASS                                             NC1204.2
099900         GO TO SIG-WRITE-GF-22-2.                                 NC1204.2
100000     MOVE "1" TO CORRECT-A.                                       NC1204.2
100100     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1204.2
100200     PERFORM FAIL.                                                NC1204.2
100300     GO TO SIG-WRITE-GF-22-2.                                     NC1204.2
100400 SIG-DELETE-GF-22-2.                                              NC1204.2
100500     PERFORM DE-LETE.                                             NC1204.2
100600 SIG-WRITE-GF-22-2.                                               NC1204.2
100700     MOVE "SIG-TEST-GF-22-2         " TO PAR-NAME.                NC1204.2
100800     PERFORM PRINT-DETAIL.                                        NC1204.2
100900 SIG-INIT-GF-12.                                                  NC1204.2
101000     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
101100     MOVE "MULTIPLY BY         " TO FEATURE.                      NC1204.2
101200     MOVE -990 TO WRK-DS-LS-0201P.                                NC1204.2
101300 SIG-TEST-GF-12-0.                                                NC1204.2
101400     MULTIPLY A01ONE-CS-00V01 BY WRK-DS-LS-0201P.                 NC1204.2
101500 SIG-TEST-GF-12-1.                                                NC1204.2
101600     MOVE WRK-DS-LS-0201P TO WRK-DS-05V00.                        NC1204.2
101700     IF WRK-DS-05V00 EQUAL TO -00090                              NC1204.2
101800         PERFORM PASS                                             NC1204.2
101900         GO TO SIG-WRITE-GF-12.                                   NC1204.2
102000     MOVE -00090 TO CORRECT-N.                                    NC1204.2
102100     MOVE  WRK-DS-05V00       TO COMPUTED-N.                      NC1204.2
102200     PERFORM FAIL.                                                NC1204.2
102300     GO TO SIG-WRITE-GF-12.                                       NC1204.2
102400 SIG-DELETE-GF-12.                                                NC1204.2
102500     PERFORM DE-LETE.                                             NC1204.2
102600 SIG-WRITE-GF-12.                                                 NC1204.2
102700     MOVE "SIG-TEST-GF-12         " TO PAR-NAME.                  NC1204.2
102800     PERFORM PRINT-DETAIL.                                        NC1204.2
102900 SIG-INIT-GF-13.                                                  NC1204.2
103000     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
103100     MOVE A18ONES-DS-18V00 TO WRK-CS-18V00.                       NC1204.2
103200 SIG-TEST-GF-13-0.                                                NC1204.2
103300     MULTIPLY A01ONE-DS-TS-P0801 BY WRK-CS-18V00.                 NC1204.2
103400 SIG-TEST-GF-13-1.                                                NC1204.2
103500     MOVE WRK-CS-18V00 TO WRK-DU-18V00.                           NC1204.2
103600     IF WRK-DU-18V00 EQUAL TO 000000000111111111                  NC1204.2
103700         PERFORM PASS                                             NC1204.2
103800         GO TO SIG-WRITE-GF-13.                                   NC1204.2
103900     MOVE  000000000111111111 TO CORRECT-18V0.                    NC1204.2
104000     MOVE WRK-DU-18V00     TO COMPUTED-18V0.                      NC1204.2
104100     PERFORM FAIL.                                                NC1204.2
104200     GO TO SIG-WRITE-GF-13.                                       NC1204.2
104300 SIG-DELETE-GF-13.                                                NC1204.2
104400     PERFORM DE-LETE.                                             NC1204.2
104500 SIG-WRITE-GF-13.                                                 NC1204.2
104600     MOVE "SIG-TEST-GF-13         " TO PAR-NAME.                  NC1204.2
104700     PERFORM PRINT-DETAIL.                                        NC1204.2
104800 SIG-INIT-GF-23.                                                  NC1204.2
104900     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
105000     MOVE ZERO TO WRK-CS-02V02.                                   NC1204.2
105100 SIG-TEST-GF-23-0.                                                NC1204.2
105200     MULTIPLY A99-CS-02V00 BY A01ONE-CS-00V01 GIVING WRK-CS-02V02.NC1204.2
105300 SIG-TEST-GF-23-1.                                                NC1204.2
105400     MOVE WRK-CS-02V02 TO WRK-DS-TS-06V06.                        NC1204.2
105500     IF WRK-DS-TS-12V00-S-S EQUAL TO 000009900000                 NC1204.2
105600         PERFORM PASS                                             NC1204.2
105700         GO TO SIG-WRITE-GF-23.                                   NC1204.2
105800     MOVE  000009900000       TO CORRECT-18V0.                    NC1204.2
105900     MOVE  WRK-DS-TS-12V00-S-S     TO COMPUTED-18V0.              NC1204.2
106000     PERFORM FAIL.                                                NC1204.2
106100     GO TO SIG-WRITE-GF-23.                                       NC1204.2
106200 SIG-DELETE-GF-23.                                                NC1204.2
106300     PERFORM DE-LETE.                                             NC1204.2
106400 SIG-WRITE-GF-23.                                                 NC1204.2
106500     MOVE "MULTIPLY BY GIVING  " TO FEATURE.                      NC1204.2
106600     MOVE "SIG-TEST-GF-23         " TO PAR-NAME.                  NC1204.2
106700     PERFORM PRINT-DETAIL.                                        NC1204.2
106800 SIG-INIT-GF-24.                                                  NC1204.2
106900     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
107000     MOVE ZERO TO WRK-CS-18V00.                                   NC1204.2
107100 SIG-TEST-GF-24-0.                                                NC1204.2
107200     MULTIPLY A01ONES-CS-18V00 BY A02THREES-CS-18V00              NC1204.2
107300             GIVING WRK-CS-18V00.                                 NC1204.2
107400 SIG-TEST-GF-24-1.                                                NC1204.2
107500     IF WRK-CS-18V00 EQUAL TO -000000000000000033                 NC1204.2
107600         PERFORM PASS                                             NC1204.2
107700         GO TO SIG-WRITE-GF-24.                                   NC1204.2
107800     MOVE -000000000000000033 TO CORRECT-18V0.                    NC1204.2
107900     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1204.2
108000     PERFORM FAIL.                                                NC1204.2
108100     GO TO SIG-WRITE-GF-24.                                       NC1204.2
108200 SIG-DELETE-GF-24.                                                NC1204.2
108300     PERFORM DE-LETE.                                             NC1204.2
108400 SIG-WRITE-GF-24.                                                 NC1204.2
108500     MOVE "SIG-TEST-GF-24 " TO PAR-NAME.                          NC1204.2
108600     PERFORM PRINT-DETAIL.                                        NC1204.2
108700 SIG-INIT-GF-25.                                                  NC1204.2
108800     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
108900     MOVE ZERO TO WRK-DU-18V00.                                   NC1204.2
109000 SIG-TEST-GF-25-0.                                                NC1204.2
109100     MULTIPLY A02THREES-CS-18V00 BY A14TWOS-CU-18V00              NC1204.2
109200             GIVING WRK-DU-18V00.                                 NC1204.2
109300 SIG-TEST-GF-25-1.                                                NC1204.2
109400     IF WRK-DU-18V00 EQUAL TO 000733333333333326                  NC1204.2
109500         PERFORM PASS                                             NC1204.2
109600         GO TO SIG-WRITE-GF-25.                                   NC1204.2
109700     MOVE 000733333333333326 TO CORRECT-18V0.                     NC1204.2
109800     MOVE WRK-DU-18V00 TO COMPUTED-18V0.                          NC1204.2
109900     PERFORM FAIL.                                                NC1204.2
110000     GO TO SIG-WRITE-GF-25.                                       NC1204.2
110100 SIG-DELETE-GF-25.                                                NC1204.2
110200     PERFORM DE-LETE.                                             NC1204.2
110300 SIG-WRITE-GF-25.                                                 NC1204.2
110400     MOVE "SIG-TEST-GF-25 " TO PAR-NAME.                          NC1204.2
110500     PERFORM PRINT-DETAIL.                                        NC1204.2
110600 SIG-INIT-GF-26.                                                  NC1204.2
110700     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
110800     MOVE ZERO TO WRK-CS-18V00.                                   NC1204.2
110900 SIG-TEST-GF-26-0.                                                NC1204.2
111000     MULTIPLY A02THREES-CS-18V00 BY A16NINES-CU-18V00             NC1204.2
111100             GIVING WRK-CS-18V00.                                 NC1204.2
111200 SIG-TEST-GF-26-1.                                                NC1204.2
111300     IF WRK-CS-18V00 EQUAL TO -329999999999999967                 NC1204.2
111400         PERFORM PASS                                             NC1204.2
111500         GO TO SIG-WRITE-GF-26.                                   NC1204.2
111600     MOVE -329999999999999967 TO CORRECT-18V0.                    NC1204.2
111700     MOVE WRK-CS-18V00 TO COMPUTED-18V0.                          NC1204.2
111800     PERFORM FAIL.                                                NC1204.2
111900     GO TO SIG-WRITE-GF-26.                                       NC1204.2
112000 SIG-DELETE-GF-26.                                                NC1204.2
112100     PERFORM DE-LETE.                                             NC1204.2
112200 SIG-WRITE-GF-26.                                                 NC1204.2
112300     MOVE "SIG-TEST-GF-26 " TO PAR-NAME.                          NC1204.2
112400     PERFORM PRINT-DETAIL.                                        NC1204.2
112500 SIG-INIT-GF-27.                                                  NC1204.2
112600     MOVE   "VI-42 5.12.4"      TO ANSI-REFERENCE.                NC1204.2
112700     MOVE ZERO TO WRK-DU-18V00.                                   NC1204.2
112800 SIG-TEST-GF-27-0.                                                NC1204.2
112900     MULTIPLY A01ONES-CS-18V00 BY A18SIXES-CU-18V00               NC1204.2
113000         GIVING WRK-DU-18V00.                                     NC1204.2
113100 SIG-TEST-GF-27-1.                                                NC1204.2
113200     IF WRK-DU-18V00 EQUAL TO 666666666666666666                  NC1204.2
113300         PERFORM PASS                                             NC1204.2
113400         GO TO SIG-WRITE-GF-27.                                   NC1204.2
113500     MOVE 666666666666666666 TO CORRECT-18V0.                     NC1204.2
113600     MOVE WRK-DU-18V00 TO COMPUTED-18V0.                          NC1204.2
113700     PERFORM FAIL.                                                NC1204.2
113800     GO TO SIG-WRITE-GF-27.                                       NC1204.2
113900 SIG-DELETE-GF-27.                                                NC1204.2
114000     PERFORM DE-LETE.                                             NC1204.2
114100 SIG-WRITE-GF-27.                                                 NC1204.2
114200     MOVE "SIG-TEST-GF-27 " TO PAR-NAME.                          NC1204.2
114300     PERFORM PRINT-DETAIL.                                        NC1204.2
114400     PERFORM END-ROUTINE.                                         NC1204.2
114500 CCVS-EXIT SECTION.                                               NC1204.2
114600 CCVS-999999.                                                     NC1204.2
114700     GO TO CLOSE-FILES.                                           NC1204.2
      *END-OF,NC120A                                                                  
