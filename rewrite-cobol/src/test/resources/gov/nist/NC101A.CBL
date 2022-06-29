      *HEADER,COBOL,NC101A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1014.2
000200 PROGRAM-ID.                                                      NC1014.2
000300     NC101A.                                                      NC1014.2
000400****************************************************************  NC1014.2
000500*                                                              *  NC1014.2
000600*    VALIDATION FOR:-                                          *  NC1014.2
000700*                                                              *  NC1014.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1014.2
000900*                                                              *  NC1014.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1014.2
001100*                                                              *  NC1014.2
001200****************************************************************  NC1014.2
001300*                                                              *  NC1014.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1014.2
001500*                                                              *  NC1014.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1014.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1014.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1014.2
001900*                                                              *  NC1014.2
002000****************************************************************  NC1014.2
002100****************************************************************  NC1014.2
002200*    THIS PROGRAM TESTS THE FORMAT 1 MULTIPLY STATEMENT FOUND     NC1014.2
002300*    IN LEVEL 1.   ALL COMBINATIONS OF THE OPTIONAL PHRASES       NC1014.2
002400*    "SIZE ERROR", "NOT SIZE ERROR" AND "END-MULTIPLY" ARE        NC1014.2
002500*    TESTED, AS WELL AS THE ROUNDED OPTION.                       NC1014.2
002600*                                                                 NC1014.2
002700*    VARIOUS COMBINATIONS OF SIGNED AND UNSIGNED NUMERIC          NC1014.2
002800*    LITERALS, DISPLAY AND COMPUTATIONAL FIELDS ARE USED          NC1014.2
002900*    AS OPERANDS.                                                 NC1014.2
003000*                                                                 NC1014.2
003100                                                                  NC1014.2
003200                                                                  NC1014.2
003300 ENVIRONMENT DIVISION.                                            NC1014.2
003400 CONFIGURATION SECTION.                                           NC1014.2
003500 SOURCE-COMPUTER.                                                 NC1014.2
003600     XXXXX082.                                                    NC1014.2
003700 OBJECT-COMPUTER.                                                 NC1014.2
003800     XXXXX083.                                                    NC1014.2
003900 INPUT-OUTPUT SECTION.                                            NC1014.2
004000 FILE-CONTROL.                                                    NC1014.2
004100     SELECT PRINT-FILE ASSIGN TO                                  NC1014.2
004200     XXXXX055.                                                    NC1014.2
004300 DATA DIVISION.                                                   NC1014.2
004400 FILE SECTION.                                                    NC1014.2
004500 FD  PRINT-FILE.                                                  NC1014.2
004600 01  PRINT-REC PICTURE X(120).                                    NC1014.2
004700 01  DUMMY-RECORD PICTURE X(120).                                 NC1014.2
004800 WORKING-STORAGE SECTION.                                         NC1014.2
004900 77  WRK-DS-18V00                PICTURE S9(18).                  NC1014.2
005000 77  A06THREES-DS-03V03          PICTURE S999V999 VALUE 333.333.  NC1014.2
005100 77  WRK-DS-06V06                PICTURE S9(6)V9(6).              NC1014.2
005200 77  WRK-DS-12V00-S REDEFINES WRK-DS-06V06 PICTURE S9(12).        NC1014.2
005300 77  A08TWOS-DS-02V06            PICTURE S99V9(6) VALUE 22.222222.NC1014.2
005400 77  WRK-DS-10V00                PICTURE S9(10).                  NC1014.2
005500 77  WRK-XN-00001                PICTURE X.                       NC1014.2
005600 77  A10ONES-DS-10V00            PICTURE S9(10)                   NC1014.2
005700                                 VALUE 1111111111.                NC1014.2
005800 77  A12THREES-DS-06V06          PICTURE S9(6)V9(6)               NC1014.2
005900                                 VALUE 333333.333333.             NC1014.2
006000 77  WRK-DS-02V00                PICTURE S99.                     NC1014.2
006100 77  AZERO-DS-05V05              PICTURE S9(5)V9(5) VALUE ZERO.   NC1014.2
006200 77  WRK-DS-09V09                 PICTURE S9(9)V9(9).             NC1014.2
006300 77  WRK-DS-18V00-S REDEFINES WRK-DS-09V09 PICTURE S9(18).        NC1014.2
006400 77  A05ONES-DS-00V05            PICTURE SV9(5) VALUE .11111.     NC1014.2
006500 77  A12ONES-DS-12V00            PICTURE S9(12)                   NC1014.2
006600                                 VALUE 111111111111.              NC1014.2
006700 77  A01ONE-DS-P0801             PICTURE SP(8)9 VALUE .000000001. NC1014.2
006800 77  WRK-DS-09V08                PICTURE S9(9)V9(8).              NC1014.2
006900 77  WRK-DS-17V00-S REDEFINES WRK-DS-09V08 PICTURE S9(17).        NC1014.2
007000 77  A18ONES-DS-18V00            PICTURE S9(18)                   NC1014.2
007100                                 VALUE 111111111111111111.        NC1014.2
007200 77  WRK-DS-0201P                PICTURE S99P.                    NC1014.2
007300 77  WRK-CS-18V00                PICTURE S9(18) COMPUTATIONAL.    NC1014.2
007400 77  WRK-DU-18V00                PICTURE 9(18).                   NC1014.2
007500 77  A99-CS-02V00                PICTURE S99 COMPUTATIONAL        NC1014.2
007600                                 VALUE 99.                        NC1014.2
007700 77  A01ONE-CS-00V01             PICTURE SV9 COMPUTATIONAL        NC1014.2
007800                                 VALUE .1.                        NC1014.2
007900 77  A99-DS-02V00                PICTURE S99   VALUE 99.          NC1014.2
008000 77  WRK-DS-12V00                PICTURE S9(12).                  NC1014.2
008100 77  WRK-DS-01V00                PICTURE S9.                      NC1014.2
008200 77  WRK-DS-03V10                PICTURE S9(3)V9(10).             NC1014.2
008300 77  A18ONES-DS-09V09            PICTURE S9(9)V9(9)               NC1014.2
008400                                 VALUE 111111111.111111111.       NC1014.2
008500 77  A02TWOS-DU-02V00            PICTURE 99  VALUE 22.            NC1014.2
008600 77  WRK-DS-05V00                PICTURE S9(5).                   NC1014.2
008700 77  A02TWOS-DS-03V02            PICTURE S999V99 VALUE +022.00.   NC1014.2
008800 77  WRK-CS-02V02                PICTURE S99V99  COMPUTATIONAL.   NC1014.2
008900 77  A990-DS-0201P               PICTURE S99P   VALUE +990.       NC1014.2
009000 77  XRAY                        PICTURE X.                       NC1014.2
009100 01  WRK-XN-18-1 PIC X(18).                                       NC1014.2
009200 01  WRK-AN-X-18-1, REDEFINES WRK-XN-18-1 PIC A(18).              NC1014.2
009300 01  WRK-DU-X-18V0-1; REDEFINES WRK-XN-18-1 PIC 9(18).            NC1014.2
009400 01  WRK-DU-0V1-1 PIC V9 VALUE .3.                                NC1014.2
009500 01  WRK-DU-0V2-1 PIC V99 VALUE .25.                              NC1014.2
009600 01  WRK-DU-0V12-1 PIC V9(12) VALUE .00001.                       NC1014.2
009700 01  WRK-DU-1V0-1 PIC 9 VALUE ZERO.                               NC1014.2
009800 01  WRK-DU-1V1-1 PIC 9V9 VALUE 1.1.                              NC1014.2
009900 01  WRK-DU-1V1-2 PIC 9V9 VALUE 2.4.                              NC1014.2
010000 01  WRK-DU-1V3-1 PIC 9V999 VALUE 1.001.                          NC1014.2
010100 01  WRK-DU-1V3-2 PIC 9V999 VALUE 1.001.                          NC1014.2
010200 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC1014.2
010300 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC1014.2
010400 01  WRK-DU-2P4-1 PIC 99P(4) VALUE 990000.                        NC1014.2
010500 01  WRK-DU-2V0-1 PIC 99.                                         NC1014.2
010600 01  WRK-DU-2V0-2 PIC 99.                                         NC1014.2
010700 01  WRK-DU-2V0-3 PIC 99.                                         NC1014.2
010800 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1014.2
010900 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1014.2
011000 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1014.2
011100 01  WRK-DU-2V2-1 PIC 99V99 VALUE 15.44.                          NC1014.2
011200 01  WRK-DU-2V2-2 PIC 99V99 VALUE 60.89.                          NC1014.2
011300 01  WRK-DU-2V2-3 PIC 99V99 VALUE 60.99.                          NC1014.2
011400 01  WRK-DU-2V2-4 PIC 99V99 VALUE 60.99.                          NC1014.2
011500 01  WRK-DU-2V2-5 PIC 99V99 VALUE 10.00.                          NC1014.2
011600 01  WRK-DU-2V5-1 PIC 99V9(5).                                    NC1014.2
011700 01  WRK-DU-4P1-1 PIC P(4)9 VALUE .00001.                         NC1014.2
011800 01  WRK-DU-5V1-1 PIC 9(5)V9 VALUE 12345.6.                       NC1014.2
011900 01  WRK-DU-6V0-1 PIC 9(6) VALUE 99999.                           NC1014.2
012000 01  WRK-DU-6V0-2 PIC 9(6) VALUE 99999.                           NC1014.2
012100 01  WRK-DU-16V2-1 PIC 9(16)V99 VALUE 9999999999999999.99.        NC1014.2
012200 01  WRK-NE-X-1 PIC 9(16).99.                                     NC1014.2
012300 01  WRK-DS-1V0-1 PIC S9 VALUE -3.                                NC1014.2
012400 01  WRK-DS-1V0-2 PIC S9 VALUE 2.                                 NC1014.2
012500 01  WRK-DS-1V2-1 PIC S9V99 VALUE ZERO.                           NC1014.2
012600 01  WRK-DS-2V0-1 PIC S99 VALUE ZERO.                             NC1014.2
012700 01  WRK-DS-2V1-1 PIC S99V9 VALUE ZERO.                           NC1014.2
012800 01  WRK-DS-2V2-1 PIC S99V99 VALUE ZERO.                          NC1014.2
012900 01  WRK-DS-2V2-2 PIC S99V99 VALUE -12.34.                        NC1014.2
013000 01  WRK-DS-16V2-1 PIC S9(16)V99 VALUE -9999999999999999.99.      NC1014.2
013100 01  WRK-NE-X-2 PIC -9(16).99.                                    NC1014.2
013200 01  WRK-NE-1 PIC .9999/99999,99999,99.                           NC1014.2
013300 01  WRK-NE-2 PIC $**.99.                                         NC1014.2
013400 01  WRK-NE-3 PIC $99.99CR.                                       NC1014.2
013500 01  WRK-NE-4 PIC $*9.99 VALUE ZERO.                              NC1014.2
013600 77  A01ONES-CS-18V00   PICTURE S9(18) COMPUTATIONAL              NC1014.2
013700             VALUE +000000000000000001.                           NC1014.2
013800 77  A02THREES-CS-18V00 PICTURE S9(18) COMPUTATIONAL              NC1014.2
013900             VALUE -000000000000000033.                           NC1014.2
014000 77  A18SIXES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1014.2
014100             VALUE 666666666666666666.                            NC1014.2
014200 77  A16NINES-CU-18V00  PICTURE 9(18) COMPUTATIONAL               NC1014.2
014300             VALUE 009999999999999999.                            NC1014.2
014400 77  A14TWOS-CU-18V00   PICTURE 9(18) COMPUTATIONAL               NC1014.2
014500             VALUE 000022222222222222.                            NC1014.2
014600 01  MULTIPLY-DATA.                                               NC1014.2
014700     02 MULT1                           PICTURE IS 999V99         NC1014.2
014800     VALUE IS 80.12.                                              NC1014.2
014900     02 MULT2                           PICTURE IS 999V999.       NC1014.2
015000     02 MULT3                           PICTURE IS $$99.99.       NC1014.2
015100     02 MULT4                           PICTURE IS S99            NC1014.2
015200     VALUE IS -56.                                                NC1014.2
015300     02 MULT5                           PICTURE IS 9 VALUE IS 4.  NC1014.2
015400     02 MULT6                           PICTURE IS 99 VALUE IS    NC1014.2
015500     20.                                                          NC1014.2
015600 01  DIVIDE-DATA.                                                 NC1014.2
015700     02 DIV1                            PICTURE IS 9(4)V99        NC1014.2
015800     VALUE IS 1620.36.                                            NC1014.2
015900     02 DIV2                            PICTURE IS 99V9           NC1014.2
016000     VALUE IS 44.1.                                               NC1014.2
016100     02 DIV3                            PICTURE IS 9(4)V9         NC1014.2
016200     VALUE IS 1661.7.                                             NC1014.2
016300     02 DIV4                            PICTURE IS S9V999         NC1014.2
016400     VALUE IS -9.642.                                             NC1014.2
016500     02 DIV-02LEVEL-1.                                            NC1014.2
016600     03 DIV5                            PICTURE IS V99            NC1014.2
016700     VALUE IS .82.                                                NC1014.2
016800     03 DIV6                            PICTURE IS 9 VALUE IS 0.  NC1014.2
016900     03 DIV7                            PICTURE IS 9V9            NC1014.2
017000     VALUE IS 9.6.                                                NC1014.2
017100 01  DIV-DATA-2.                                                  NC1014.2
017200     02 DIV8                            PICTURE IS 99V9.          NC1014.2
017300     02 DIV9                            PICTURE IS ZZ,ZZZ.9.      NC1014.2
017400     02 DIV10                           PICTURE IS V999.          NC1014.2
017500 01  TEST-RESULTS.                                                NC1014.2
017600     02 FILLER                   PIC X      VALUE SPACE.          NC1014.2
017700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1014.2
017800     02 FILLER                   PIC X      VALUE SPACE.          NC1014.2
017900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1014.2
018000     02 FILLER                   PIC X      VALUE SPACE.          NC1014.2
018100     02  PAR-NAME.                                                NC1014.2
018200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1014.2
018300       03  PARDOT-X              PIC X      VALUE SPACE.          NC1014.2
018400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1014.2
018500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1014.2
018600     02 RE-MARK                  PIC X(61).                       NC1014.2
018700 01  TEST-COMPUTED.                                               NC1014.2
018800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1014.2
018900     02 FILLER                   PIC X(17)  VALUE                 NC1014.2
019000            "       COMPUTED=".                                   NC1014.2
019100     02 COMPUTED-X.                                               NC1014.2
019200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1014.2
019300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1014.2
019400                                 PIC -9(9).9(9).                  NC1014.2
019500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1014.2
019600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1014.2
019700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1014.2
019800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1014.2
019900         04 COMPUTED-18V0                    PIC -9(18).          NC1014.2
020000         04 FILLER                           PIC X.               NC1014.2
020100     03 FILLER PIC X(50) VALUE SPACE.                             NC1014.2
020200 01  TEST-CORRECT.                                                NC1014.2
020300     02 FILLER PIC X(30) VALUE SPACE.                             NC1014.2
020400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1014.2
020500     02 CORRECT-X.                                                NC1014.2
020600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1014.2
020700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1014.2
020800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1014.2
020900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1014.2
021000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1014.2
021100     03      CR-18V0 REDEFINES CORRECT-A.                         NC1014.2
021200         04 CORRECT-18V0                     PIC -9(18).          NC1014.2
021300         04 FILLER                           PIC X.               NC1014.2
021400     03 FILLER PIC X(2) VALUE SPACE.                              NC1014.2
021500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1014.2
021600 01  CCVS-C-1.                                                    NC1014.2
021700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1014.2
021800-    "SS  PARAGRAPH-NAME                                          NC1014.2
021900-    "       REMARKS".                                            NC1014.2
022000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1014.2
022100 01  CCVS-C-2.                                                    NC1014.2
022200     02 FILLER                     PIC X        VALUE SPACE.      NC1014.2
022300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1014.2
022400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1014.2
022500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1014.2
022600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1014.2
022700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1014.2
022800 01  REC-CT                        PIC 99       VALUE ZERO.       NC1014.2
022900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1014.2
023000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1014.2
023100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1014.2
023200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1014.2
023300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1014.2
023400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1014.2
023500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1014.2
023600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1014.2
023700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1014.2
023800 01  CCVS-H-1.                                                    NC1014.2
023900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1014.2
024000     02  FILLER                    PIC X(42)    VALUE             NC1014.2
024100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1014.2
024200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1014.2
024300 01  CCVS-H-2A.                                                   NC1014.2
024400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1014.2
024500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1014.2
024600   02  FILLER                        PIC XXXX   VALUE             NC1014.2
024700     "4.2 ".                                                      NC1014.2
024800   02  FILLER                        PIC X(28)  VALUE             NC1014.2
024900            " COPY - NOT FOR DISTRIBUTION".                       NC1014.2
025000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1014.2
025100                                                                  NC1014.2
025200 01  CCVS-H-2B.                                                   NC1014.2
025300   02  FILLER                        PIC X(15)  VALUE             NC1014.2
025400            "TEST RESULT OF ".                                    NC1014.2
025500   02  TEST-ID                       PIC X(9).                    NC1014.2
025600   02  FILLER                        PIC X(4)   VALUE             NC1014.2
025700            " IN ".                                               NC1014.2
025800   02  FILLER                        PIC X(12)  VALUE             NC1014.2
025900     " HIGH       ".                                              NC1014.2
026000   02  FILLER                        PIC X(22)  VALUE             NC1014.2
026100            " LEVEL VALIDATION FOR ".                             NC1014.2
026200   02  FILLER                        PIC X(58)  VALUE             NC1014.2
026300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1014.2
026400 01  CCVS-H-3.                                                    NC1014.2
026500     02  FILLER                      PIC X(34)  VALUE             NC1014.2
026600            " FOR OFFICIAL USE ONLY    ".                         NC1014.2
026700     02  FILLER                      PIC X(58)  VALUE             NC1014.2
026800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1014.2
026900     02  FILLER                      PIC X(28)  VALUE             NC1014.2
027000            "  COPYRIGHT   1985 ".                                NC1014.2
027100 01  CCVS-E-1.                                                    NC1014.2
027200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1014.2
027300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1014.2
027400     02 ID-AGAIN                     PIC X(9).                    NC1014.2
027500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1014.2
027600 01  CCVS-E-2.                                                    NC1014.2
027700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1014.2
027800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1014.2
027900     02 CCVS-E-2-2.                                               NC1014.2
028000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1014.2
028100         03 FILLER                   PIC X      VALUE SPACE.      NC1014.2
028200         03 ENDER-DESC               PIC X(44)  VALUE             NC1014.2
028300            "ERRORS ENCOUNTERED".                                 NC1014.2
028400 01  CCVS-E-3.                                                    NC1014.2
028500     02  FILLER                      PIC X(22)  VALUE             NC1014.2
028600            " FOR OFFICIAL USE ONLY".                             NC1014.2
028700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1014.2
028800     02  FILLER                      PIC X(58)  VALUE             NC1014.2
028900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1014.2
029000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1014.2
029100     02 FILLER                       PIC X(15)  VALUE             NC1014.2
029200             " COPYRIGHT 1985".                                   NC1014.2
029300 01  CCVS-E-4.                                                    NC1014.2
029400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1014.2
029500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1014.2
029600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1014.2
029700     02 FILLER                       PIC X(40)  VALUE             NC1014.2
029800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1014.2
029900 01  XXINFO.                                                      NC1014.2
030000     02 FILLER                       PIC X(19)  VALUE             NC1014.2
030100            "*** INFORMATION ***".                                NC1014.2
030200     02 INFO-TEXT.                                                NC1014.2
030300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1014.2
030400       04 XXCOMPUTED                 PIC X(20).                   NC1014.2
030500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1014.2
030600       04 XXCORRECT                  PIC X(20).                   NC1014.2
030700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1014.2
030800 01  HYPHEN-LINE.                                                 NC1014.2
030900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1014.2
031000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1014.2
031100-    "*****************************************".                 NC1014.2
031200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1014.2
031300-    "******************************".                            NC1014.2
031400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1014.2
031500     "NC101A".                                                    NC1014.2
031600 PROCEDURE DIVISION.                                              NC1014.2
031700 CCVS1 SECTION.                                                   NC1014.2
031800 OPEN-FILES.                                                      NC1014.2
031900     OPEN     OUTPUT PRINT-FILE.                                  NC1014.2
032000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1014.2
032100     MOVE    SPACE TO TEST-RESULTS.                               NC1014.2
032200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1014.2
032300     GO TO CCVS1-EXIT.                                            NC1014.2
032400 CLOSE-FILES.                                                     NC1014.2
032500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1014.2
032600 TERMINATE-CCVS.                                                  NC1014.2
032700*    EXIT PROGRAM.                                                NC1014.2
032800*TERMINATE-CALL.                                                  NC1014.2
032900     STOP     RUN.                                                NC1014.2
033000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1014.2
033100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1014.2
033200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1014.2
033300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1014.2
033400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1014.2
033500 PRINT-DETAIL.                                                    NC1014.2
033600     IF REC-CT NOT EQUAL TO ZERO                                  NC1014.2
033700             MOVE "." TO PARDOT-X                                 NC1014.2
033800             MOVE REC-CT TO DOTVALUE.                             NC1014.2
033900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1014.2
034000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1014.2
034100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1014.2
034200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1014.2
034300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1014.2
034400     MOVE SPACE TO CORRECT-X.                                     NC1014.2
034500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1014.2
034600     MOVE     SPACE TO RE-MARK.                                   NC1014.2
034700 HEAD-ROUTINE.                                                    NC1014.2
034800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1014.2
034900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1014.2
035000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1014.2
035100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1014.2
035200 COLUMN-NAMES-ROUTINE.                                            NC1014.2
035300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1014.2
035400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1014.2
035500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1014.2
035600 END-ROUTINE.                                                     NC1014.2
035700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1014.2
035800 END-RTN-EXIT.                                                    NC1014.2
035900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1014.2
036000 END-ROUTINE-1.                                                   NC1014.2
036100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1014.2
036200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1014.2
036300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1014.2
036400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1014.2
036500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1014.2
036600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1014.2
036700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1014.2
036800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1014.2
036900  END-ROUTINE-12.                                                 NC1014.2
037000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1014.2
037100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1014.2
037200         MOVE "NO " TO ERROR-TOTAL                                NC1014.2
037300         ELSE                                                     NC1014.2
037400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1014.2
037500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1014.2
037600     PERFORM WRITE-LINE.                                          NC1014.2
037700 END-ROUTINE-13.                                                  NC1014.2
037800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1014.2
037900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1014.2
038000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1014.2
038100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1014.2
038200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1014.2
038300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1014.2
038400          MOVE "NO " TO ERROR-TOTAL                               NC1014.2
038500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1014.2
038600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1014.2
038700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1014.2
038800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1014.2
038900 WRITE-LINE.                                                      NC1014.2
039000     ADD 1 TO RECORD-COUNT.                                       NC1014.2
039100*    IF RECORD-COUNT GREATER 42                                   NC1014.2
039200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1014.2
039300*        MOVE SPACE TO DUMMY-RECORD                               NC1014.2
039400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1014.2
039500*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1014.2
039600*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1014.2
039700*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1014.2
039800*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1014.2
039900*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1014.2
040000*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1014.2
040100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1014.2
040200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1014.2
040300*        MOVE ZERO TO RECORD-COUNT.                               NC1014.2
040400     PERFORM WRT-LN.                                              NC1014.2
040500 WRT-LN.                                                          NC1014.2
040600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1014.2
040700     MOVE SPACE TO DUMMY-RECORD.                                  NC1014.2
040800 BLANK-LINE-PRINT.                                                NC1014.2
040900     PERFORM WRT-LN.                                              NC1014.2
041000 FAIL-ROUTINE.                                                    NC1014.2
041100     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1014.2
041200            GO TO FAIL-ROUTINE-WRITE.                             NC1014.2
041300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1014.2
041400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1014.2
041500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1014.2
041600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1014.2
041700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1014.2
041800     GO TO  FAIL-ROUTINE-EX.                                      NC1014.2
041900 FAIL-ROUTINE-WRITE.                                              NC1014.2
042000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1014.2
042100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1014.2
042200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1014.2
042300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1014.2
042400 FAIL-ROUTINE-EX. EXIT.                                           NC1014.2
042500 BAIL-OUT.                                                        NC1014.2
042600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1014.2
042700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1014.2
042800 BAIL-OUT-WRITE.                                                  NC1014.2
042900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1014.2
043000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1014.2
043100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1014.2
043200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1014.2
043300 BAIL-OUT-EX. EXIT.                                               NC1014.2
043400 CCVS1-EXIT.                                                      NC1014.2
043500     EXIT.                                                        NC1014.2
043600 SECT-NC101A-001 SECTION.                                         NC1014.2
043700 MPY-INIT-F1-1.                                                   NC1014.2
043800     MOVE    "MULTIPLY BY" TO FEATURE.                            NC1014.2
043900     MOVE    "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.               NC1014.2
044000     MOVE     80.12 TO MULT1.                                     NC1014.2
044100     MOVE     4     TO MULT5.                                     NC1014.2
044200 MPY-TEST-F1-1.                                                   NC1014.2
044300     MULTIPLY MULT5 BY MULT1.                                     NC1014.2
044400     IF       MULT1 EQUAL TO 320.48                               NC1014.2
044500              PERFORM PASS                                        NC1014.2
044600     ELSE                                                         NC1014.2
044700              GO TO MPY-FAIL-F1-1.                                NC1014.2
044800     GO TO    MPY-WRITE-F1-1.                                     NC1014.2
044900 MPY-DELETE-F1-1.                                                 NC1014.2
045000     PERFORM  DE-LETE.                                            NC1014.2
045100     GO TO    MPY-WRITE-F1-1.                                     NC1014.2
045200 MPY-FAIL-F1-1.                                                   NC1014.2
045300     PERFORM  FAIL.                                               NC1014.2
045400     MOVE     MULT1 TO COMPUTED-N.                                NC1014.2
045500     MOVE     +320.48 TO CORRECT-N.                               NC1014.2
045600 MPY-WRITE-F1-1.                                                  NC1014.2
045700     MOVE "MPY-TEST-F1-1 " TO PAR-NAME.                           NC1014.2
045800     PERFORM  PRINT-DETAIL.                                       NC1014.2
045900*                                                                 NC1014.2
046000 MPY-INIT-F1-2.                                                   NC1014.2
046100     MOVE    "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.               NC1014.2
046200     MOVE    -56 TO MULT4.                                        NC1014.2
046300 MPY-TEST-F1-2.                                                   NC1014.2
046400     MULTIPLY -1.3 BY MULT4 ROUNDED.                              NC1014.2
046500     IF       MULT4 EQUAL TO 73                                   NC1014.2
046600              PERFORM PASS                                        NC1014.2
046700     ELSE                                                         NC1014.2
046800              GO TO MPY-FAIL-F1-2.                                NC1014.2
046900     GO TO    MPY-WRITE-F1-2.                                     NC1014.2
047000 MPY-DELETE-F1-2.                                                 NC1014.2
047100     PERFORM  DE-LETE.                                            NC1014.2
047200     GO TO    MPY-WRITE-F1-2.                                     NC1014.2
047300 MPY-FAIL-F1-2.                                                   NC1014.2
047400     PERFORM  FAIL.                                               NC1014.2
047500     MOVE     MULT4 TO COMPUTED-N.                                NC1014.2
047600     MOVE     +73   TO CORRECT-N.                                 NC1014.2
047700 MPY-WRITE-F1-2.                                                  NC1014.2
047800     MOVE "MPY-TEST-F1-2 " TO PAR-NAME.                           NC1014.2
047900     PERFORM  PRINT-DETAIL.                                       NC1014.2
048000                                                                  NC1014.2
048100 MPY-INIT-F1-3-1.                                                 NC1014.2
048200     MOVE    "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.               NC1014.2
048300     MOVE     4 TO MULT5.                                         NC1014.2
048400     MOVE    "A" TO XRAY.                                         NC1014.2
048500 MPY-TEST-F1-3-0.                                                 NC1014.2
048600     MULTIPLY MULT5 BY MULT5 ON SIZE ERROR                        NC1014.2
048700              MOVE "K" TO XRAY.                                   NC1014.2
048800 MPY-TEST-F1-3-1.                                                 NC1014.2
048900     IF       XRAY EQUAL TO "K"                                   NC1014.2
049000              PERFORM PASS                                        NC1014.2
049100     ELSE                                                         NC1014.2
049200              GO TO MPY-FAIL-F1-3-1.                              NC1014.2
049300     GO TO    MPY-WRITE-F1-3-1.                                   NC1014.2
049400 MPY-DELETE-F1-3-1.                                               NC1014.2
049500     PERFORM  DE-LETE.                                            NC1014.2
049600     GO TO    MPY-WRITE-F1-3-1.                                   NC1014.2
049700 MPY-FAIL-F1-3-1.                                                 NC1014.2
049800     MOVE     XRAY  TO COMPUTED-X.                                NC1014.2
049900     MOVE    "A"    TO CORRECT-X.                                 NC1014.2
050000     MOVE    "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.             NC1014.2
050100     PERFORM  FAIL.                                               NC1014.2
050200 MPY-WRITE-F1-3-1.                                                NC1014.2
050300     MOVE    "MPY-TEST-F1-3-1 " TO PAR-NAME.                      NC1014.2
050400     PERFORM  PRINT-DETAIL.                                       NC1014.2
050500 MPY-TEST-F1-3-2.                                                 NC1014.2
050600     IF       MULT5 EQUAL TO 4                                    NC1014.2
050700              PERFORM PASS                                        NC1014.2
050800     ELSE                                                         NC1014.2
050900              GO TO MPY-FAIL-F1-3-2.                              NC1014.2
051000     GO TO    MPY-WRITE-F1-3-2.                                   NC1014.2
051100 MPY-DELETE-F1-3-2.                                               NC1014.2
051200     PERFORM  DE-LETE.                                            NC1014.2
051300     GO TO    MPY-WRITE-F1-3-2.                                   NC1014.2
051400 MPY-FAIL-F1-3-2.                                                 NC1014.2
051500     PERFORM  FAIL.                                               NC1014.2
051600     MOVE     MULT5 TO COMPUTED-N.                                NC1014.2
051700     MOVE    +4 TO CORRECT-N.                                     NC1014.2
051800     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1014.2
051900 MPY-WRITE-F1-3-2.                                                NC1014.2
052000     MOVE    "MPY-TEST-F1-3-2" TO PAR-NAME.                       NC1014.2
052100     PERFORM  PRINT-DETAIL.                                       NC1014.2
052200                                                                  NC1014.2
052300 MPY-INIT-F1-4-1.                                                 NC1014.2
052400     MOVE    "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.               NC1014.2
052500     MOVE     20 TO MULT6.                                        NC1014.2
052600     MOVE    "B" TO XRAY.                                         NC1014.2
052700 MPY-TEST-F1-4-O.                                                 NC1014.2
052800     MULTIPLY 4.99 BY MULT6 ROUNDED ON SIZE ERROR                 NC1014.2
052900              MOVE "L" TO XRAY.                                   NC1014.2
053000 MPY-TEST-F1-4-1.                                                 NC1014.2
053100     IF       XRAY EQUAL TO "L"                                   NC1014.2
053200              PERFORM PASS                                        NC1014.2
053300     ELSE                                                         NC1014.2
053400              GO TO MPY-FAIL-F1-4-1.                              NC1014.2
053500     GO TO    MPY-WRITE-F1-4-1.                                   NC1014.2
053600 MPY-DELETE-F1-4-1.                                               NC1014.2
053700     PERFORM  DE-LETE.                                            NC1014.2
053800     GO TO    MPY-WRITE-F1-4-1.                                   NC1014.2
053900 MPY-FAIL-F1-4-1.                                                 NC1014.2
054000     MOVE    "L"   TO CORRECT-X.                                  NC1014.2
054100     MOVE     XRAY TO COMPUTED-X.                                 NC1014.2
054200     MOVE  "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.               NC1014.2
054300     PERFORM  FAIL.                                               NC1014.2
054400 MPY-WRITE-F1-4-1.                                                NC1014.2
054500     MOVE    "MPY-TEST-F1-4-1" TO PAR-NAME.                       NC1014.2
054600     PERFORM  PRINT-DETAIL.                                       NC1014.2
054700 MPY-TEST-F1-4-2.                                                 NC1014.2
054800     IF       MULT6 EQUAL TO 20                                   NC1014.2
054900              PERFORM PASS                                        NC1014.2
055000     ELSE                                                         NC1014.2
055100              GO TO MPY-FAIL-F1-4-2.                              NC1014.2
055200     GO TO    MPY-WRITE-F1-4-2.                                   NC1014.2
055300 MPY-DELETE-F1-4-2.                                               NC1014.2
055400     PERFORM  DE-LETE.                                            NC1014.2
055500     GO TO    MPY-WRITE-F1-4-2.                                   NC1014.2
055600 MPY-FAIL-F1-4-2.                                                 NC1014.2
055700     PERFORM  FAIL.                                               NC1014.2
055800     MOVE     MULT6 TO COMPUTED-N.                                NC1014.2
055900     MOVE     +20 TO CORRECT-N.                                   NC1014.2
056000     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1014.2
056100 MPY-WRITE-F1-4-2.                                                NC1014.2
056200     MOVE    "MPY-TEST-F1-4-2" TO PAR-NAME.                       NC1014.2
056300     PERFORM  PRINT-DETAIL.                                       NC1014.2
056400                                                                  NC1014.2
056500 MPY-INIT-F1-5.                                                   NC1014.2
056600     MOVE    "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.               NC1014.2
056700     MOVE     222222222222 TO WRK-DS-18V00.                       NC1014.2
056800 MPY-TEST-F1-5-0.                                                 NC1014.2
056900     MULTIPLY A06THREES-DS-03V03 BY WRK-DS-18V00.                 NC1014.2
057000 MPY-TEST-F1-5-1.                                                 NC1014.2
057100     IF  WRK-DS-18V00 EQUAL TO 000074073999999925                 NC1014.2
057200         PERFORM PASS                                             NC1014.2
057300         GO TO MPY-WRITE-F1-5.                                    NC1014.2
057400     GO TO  MPY-FAIL-F1-5.                                        NC1014.2
057500 MPY-DELETE-F1-5.                                                 NC1014.2
057600     PERFORM DE-LETE.                                             NC1014.2
057700     GO TO  MPY-WRITE-F1-5.                                       NC1014.2
057800 MPY-FAIL-F1-5.                                                   NC1014.2
057900     MOVE   000074073999999925 TO CORRECT-18V0.                   NC1014.2
058000     MOVE   WRK-DS-18V00       TO COMPUTED-18V0.                  NC1014.2
058100     PERFORM FAIL.                                                NC1014.2
058200 MPY-WRITE-F1-5.                                                  NC1014.2
058300     MOVE "MPY-TEST-F1-5 " TO PAR-NAME.                           NC1014.2
058400     PERFORM PRINT-DETAIL.                                        NC1014.2
058500                                                                  NC1014.2
058600 MPY-INIT-F1-6.                                                   NC1014.2
058700     MOVE   "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.                NC1014.2
058800     MOVE    A08TWOS-DS-02V06 TO WRK-DS-06V06.                    NC1014.2
058900 MPY-TEST-F1-6-0.                                                 NC1014.2
059000     MULTIPLY 0.4 BY WRK-DS-06V06 ROUNDED.                        NC1014.2
059100 MPY-TEST-F1-6-1.                                                 NC1014.2
059200     IF WRK-DS-12V00-S EQUAL TO 000008888889                      NC1014.2
059300         PERFORM PASS                                             NC1014.2
059400         GO TO MPY-WRITE-F1-6.                                    NC1014.2
059500     GO TO MPY-FAIL-F1-6.                                         NC1014.2
059600 MPY-DELETE-F1-6.                                                 NC1014.2
059700     PERFORM DE-LETE.                                             NC1014.2
059800     GO TO MPY-WRITE-F1-6.                                        NC1014.2
059900 MPY-FAIL-F1-6.                                                   NC1014.2
060000     MOVE  WRK-DS-12V00-S     TO COMPUTED-18V0.                   NC1014.2
060100     MOVE  000008888889       TO CORRECT-18V0.                    NC1014.2
060200     PERFORM FAIL.                                                NC1014.2
060300 MPY-WRITE-F1-6.                                                  NC1014.2
060400     MOVE "MPY-TEST-F1-6         " TO PAR-NAME.                   NC1014.2
060500     PERFORM PRINT-DETAIL.                                        NC1014.2
060600                                                                  NC1014.2
060700 MPY-INIT-F1-7.                                                   NC1014.2
060800     MOVE   "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.                NC1014.2
060900     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
061000     MOVE    A10ONES-DS-10V00 TO WRK-DS-10V00.                    NC1014.2
061100 MPY-TEST-F1-7-0.                                                 NC1014.2
061200     MULTIPLY A12THREES-DS-06V06 BY WRK-DS-10V00                  NC1014.2
061300         ON SIZE ERROR MOVE "1" TO WRK-XN-00001.                  NC1014.2
061400 MPY-TEST-F1-7-1.                                                 NC1014.2
061500     IF WRK-DS-10V00 EQUAL TO 1111111111                          NC1014.2
061600         PERFORM PASS                                             NC1014.2
061700         GO TO MPY-WRITE-F1-7-1.                                  NC1014.2
061800     GO TO MPY-FAIL-F1-7-1.                                       NC1014.2
061900 MPY-DELETE-F1-7-1.                                               NC1014.2
062000     PERFORM DE-LETE.                                             NC1014.2
062100     GO TO MPY-WRITE-F1-7-1.                                      NC1014.2
062200 MPY-FAIL-F1-7-1.                                                 NC1014.2
062300     MOVE  WRK-DS-10V00       TO COMPUTED-18V0.                   NC1014.2
062400     MOVE  1111111111         TO CORRECT-18V0.                    NC1014.2
062500     MOVE    "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.         NC1014.2
062600     PERFORM FAIL.                                                NC1014.2
062700 MPY-WRITE-F1-7-1.                                                NC1014.2
062800     MOVE "MPY-TEST-F1-7-1         " TO PAR-NAME.                 NC1014.2
062900     PERFORM PRINT-DETAIL.                                        NC1014.2
063000 MPY-TEST-F1-7-2.                                                 NC1014.2
063100     IF WRK-XN-00001 EQUAL TO "1"                                 NC1014.2
063200         PERFORM PASS                                             NC1014.2
063300         GO TO MPY-WRITE-F1-7-2.                                  NC1014.2
063400     MOVE    "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.             NC1014.2
063500     MOVE "1" TO CORRECT-A.                                       NC1014.2
063600     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1014.2
063700     PERFORM FAIL.                                                NC1014.2
063800     GO TO MPY-WRITE-F1-7-2.                                      NC1014.2
063900 MPY-DELETE-F1-7-2.                                               NC1014.2
064000     PERFORM DE-LETE.                                             NC1014.2
064100 MPY-WRITE-F1-7-2.                                                NC1014.2
064200     MOVE "MPY-TEST-F1-7-2         " TO PAR-NAME.                 NC1014.2
064300     PERFORM PRINT-DETAIL.                                        NC1014.2
064400                                                                  NC1014.2
064500 MPY-INIT-F1-8.                                                   NC1014.2
064600     MOVE   "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.                NC1014.2
064700     MOVE   "1" TO WRK-XN-00001.                                  NC1014.2
064800     MOVE   -99 TO WRK-DS-02V00.                                  NC1014.2
064900 MPY-TEST-F1-8-0.                                                 NC1014.2
065000     MULTIPLY AZERO-DS-05V05 BY WRK-DS-02V00                      NC1014.2
065100         ON SIZE ERROR MOVE "0" TO WRK-XN-00001.                  NC1014.2
065200 MPY-TEST-F1-8-1.                                                 NC1014.2
065300     IF WRK-DS-02V00 EQUAL TO 00                                  NC1014.2
065400         PERFORM PASS                                             NC1014.2
065500         GO TO MPY-WRITE-F1-8-1.                                  NC1014.2
065600     GO TO MPY-FAIL-F1-8-1.                                       NC1014.2
065700 MPY-DELETE-F1-8-1.                                               NC1014.2
065800     PERFORM DE-LETE.                                             NC1014.2
065900     GO TO MPY-WRITE-F1-8-1.                                      NC1014.2
066000 MPY-FAIL-F1-8-1.                                                 NC1014.2
066100     MOVE  WRK-DS-02V00       TO COMPUTED-N.                      NC1014.2
066200     MOVE  00                 TO CORRECT-N.                       NC1014.2
066300     PERFORM FAIL.                                                NC1014.2
066400 MPY-WRITE-F1-8-1.                                                NC1014.2
066500     MOVE "MPY-TEST-F1-8-1         " TO PAR-NAME.                 NC1014.2
066600     PERFORM PRINT-DETAIL.                                        NC1014.2
066700 MPY-TEST-F1-8-2.                                                 NC1014.2
066800     IF WRK-XN-00001 EQUAL TO "1"                                 NC1014.2
066900         PERFORM PASS                                             NC1014.2
067000         GO TO MPY-WRITE-F1-8-2.                                  NC1014.2
067100     MOVE "1" TO CORRECT-A.                                       NC1014.2
067200     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1014.2
067300     MOVE    "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.   NC1014.2
067400     PERFORM FAIL.                                                NC1014.2
067500     GO TO MPY-WRITE-F1-8-2.                                      NC1014.2
067600 MPY-DELETE-F1-8-2.                                               NC1014.2
067700     PERFORM DE-LETE.                                             NC1014.2
067800 MPY-WRITE-F1-8-2.                                                NC1014.2
067900     MOVE "MPY-TEST-F1-8-2         " TO PAR-NAME.                 NC1014.2
068000     PERFORM PRINT-DETAIL.                                        NC1014.2
068100                                                                  NC1014.2
068200 MPY-INIT-F1-9.                                                   NC1014.2
068300     MOVE   "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.                NC1014.2
068400     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
068500     MOVE   -01 TO WRK-DS-02V00.                                  NC1014.2
068600 MPY-TEST-F1-9-0.                                                 NC1014.2
068700     MULTIPLY 99.5 BY WRK-DS-02V00 ROUNDED                        NC1014.2
068800         ON SIZE ERROR MOVE "1" TO WRK-XN-00001.                  NC1014.2
068900 MPY-TEST-F1-9-1.                                                 NC1014.2
069000     IF WRK-DS-02V00 EQUAL TO -01                                 NC1014.2
069100         PERFORM PASS                                             NC1014.2
069200         GO TO MPY-WRITE-F1-9-1.                                  NC1014.2
069300     GO TO MPY-FAIL-F1-9-1.                                       NC1014.2
069400 MPY-DELETE-F1-9-1.                                               NC1014.2
069500     PERFORM DE-LETE.                                             NC1014.2
069600     GO TO MPY-WRITE-F1-9-1.                                      NC1014.2
069700 MPY-FAIL-F1-9-1.                                                 NC1014.2
069800     MOVE  WRK-DS-02V00       TO COMPUTED-N.                      NC1014.2
069900     MOVE  -01                TO CORRECT-N.                       NC1014.2
070000     MOVE "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK.            NC1014.2
070100     PERFORM FAIL.                                                NC1014.2
070200 MPY-WRITE-F1-9-1.                                                NC1014.2
070300     MOVE "MPY-TEST-F1-9-1         " TO PAR-NAME.                 NC1014.2
070400     PERFORM PRINT-DETAIL.                                        NC1014.2
070500 MPY-TEST-F1-9-2.                                                 NC1014.2
070600     IF WRK-XN-00001 EQUAL TO "1"                                 NC1014.2
070700         PERFORM PASS                                             NC1014.2
070800         GO TO MPY-WRITE-F1-9-2.                                  NC1014.2
070900     MOVE "1" TO CORRECT-A.                                       NC1014.2
071000     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1014.2
071100     MOVE    "ON SIZE ERROR NOT EXECUTED" TO RE-MARK.             NC1014.2
071200     PERFORM FAIL.                                                NC1014.2
071300     GO TO MPY-WRITE-F1-9-2.                                      NC1014.2
071400 MPY-DELETE-F1-9-2.                                               NC1014.2
071500     PERFORM DE-LETE.                                             NC1014.2
071600 MPY-WRITE-F1-9-2.                                                NC1014.2
071700     MOVE "MPY-TEST-F1-9-2         " TO PAR-NAME.                 NC1014.2
071800     PERFORM PRINT-DETAIL.                                        NC1014.2
071900                                                                  NC1014.2
072000 MPY-INIT-F1-10.                                                  NC1014.2
072100     MOVE   "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.                NC1014.2
072200     MOVE   "1" TO WRK-XN-00001.                                  NC1014.2
072300     MOVE   -01 TO WRK-DS-02V00.                                  NC1014.2
072400 MPY-TEST-F1-10-0.                                                NC1014.2
072500     MULTIPLY 99.4 BY WRK-DS-02V00 ROUNDED                        NC1014.2
072600         ON SIZE ERROR MOVE "0" TO WRK-XN-00001.                  NC1014.2
072700 MPY-TEST-F1-10-1.                                                NC1014.2
072800     IF WRK-DS-02V00 EQUAL TO -99                                 NC1014.2
072900         PERFORM PASS                                             NC1014.2
073000         GO TO MPY-WRITE-F1-10-1.                                 NC1014.2
073100     GO TO MPY-FAIL-F1-10-1.                                      NC1014.2
073200 MPY-DELETE-F1-10-1.                                              NC1014.2
073300     PERFORM DE-LETE.                                             NC1014.2
073400     GO TO MPY-WRITE-F1-10-1.                                     NC1014.2
073500 MPY-FAIL-F1-10-1.                                                NC1014.2
073600     MOVE  WRK-DS-02V00       TO COMPUTED-N.                      NC1014.2
073700     MOVE  -99                TO CORRECT-N.                       NC1014.2
073800     PERFORM FAIL.                                                NC1014.2
073900 MPY-WRITE-F1-10-1.                                               NC1014.2
074000     MOVE "MPY-TEST-F1-10-1         " TO PAR-NAME.                NC1014.2
074100     PERFORM PRINT-DETAIL.                                        NC1014.2
074200 MPY-TEST-F1-10-2.                                                NC1014.2
074300     IF WRK-XN-00001 EQUAL TO "1"                                 NC1014.2
074400         PERFORM PASS                                             NC1014.2
074500         GO TO MPY-WRITE-F1-10-2.                                 NC1014.2
074600     MOVE "1" TO CORRECT-A.                                       NC1014.2
074700     MOVE WRK-XN-00001 TO COMPUTED-A.                             NC1014.2
074800     MOVE    "ON SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARK.   NC1014.2
074900     PERFORM FAIL.                                                NC1014.2
075000     GO TO MPY-WRITE-F1-10-2.                                     NC1014.2
075100 MPY-DELETE-F1-10-2.                                              NC1014.2
075200     PERFORM DE-LETE.                                             NC1014.2
075300 MPY-WRITE-F1-10-2.                                               NC1014.2
075400     MOVE "MPY-TEST-F1-10-2         " TO PAR-NAME.                NC1014.2
075500     PERFORM PRINT-DETAIL.                                        NC1014.2
075600                                                                  NC1014.2
075700 MPY-INIT-F1-11.                                                  NC1014.2
075800     MOVE   "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.                NC1014.2
075900     MOVE   -990 TO WRK-DS-0201P.                                 NC1014.2
076000 MPY-TEST-F1-11-0.                                                NC1014.2
076100     MULTIPLY A01ONE-CS-00V01 BY WRK-DS-0201P.                    NC1014.2
076200 MPY-TEST-F1-11.                                                  NC1014.2
076300     MOVE WRK-DS-0201P TO WRK-DS-05V00.                           NC1014.2
076400     IF WRK-DS-05V00 EQUAL TO -00090                              NC1014.2
076500         PERFORM PASS                                             NC1014.2
076600         GO TO MPY-WRITE-F1-11.                                   NC1014.2
076700     MOVE -00090 TO CORRECT-N.                                    NC1014.2
076800     MOVE  WRK-DS-05V00       TO COMPUTED-N.                      NC1014.2
076900     PERFORM FAIL.                                                NC1014.2
077000     GO TO MPY-WRITE-F1-11.                                       NC1014.2
077100 MPY-DELETE-F1-11.                                                NC1014.2
077200     PERFORM DE-LETE.                                             NC1014.2
077300 MPY-WRITE-F1-11.                                                 NC1014.2
077400     MOVE "MPY-TEST-F1-11         " TO PAR-NAME.                  NC1014.2
077500     PERFORM PRINT-DETAIL.                                        NC1014.2
077600                                                                  NC1014.2
077700 MPY-INIT-F1-12.                                                  NC1014.2
077800     MOVE   "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.                NC1014.2
077900     MOVE    A18ONES-DS-18V00   TO WRK-CS-18V00.                  NC1014.2
078000 MPY-TEST-F1-12-0.                                                NC1014.2
078100     MULTIPLY A01ONE-DS-P0801 BY WRK-CS-18V00.                    NC1014.2
078200 MPY-TEST-F1-12.                                                  NC1014.2
078300     MOVE WRK-CS-18V00 TO WRK-DU-18V00.                           NC1014.2
078400     IF WRK-DU-18V00 EQUAL TO 000000000111111111                  NC1014.2
078500         PERFORM PASS                                             NC1014.2
078600         GO TO MPY-WRITE-F1-12.                                   NC1014.2
078700     MOVE  000000000111111111 TO CORRECT-18V0.                    NC1014.2
078800     MOVE WRK-DU-18V00     TO COMPUTED-18V0.                      NC1014.2
078900     PERFORM FAIL.                                                NC1014.2
079000     GO TO MPY-WRITE-F1-12.                                       NC1014.2
079100 MPY-DELETE-F1-12.                                                NC1014.2
079200     PERFORM DE-LETE.                                             NC1014.2
079300 MPY-WRITE-F1-12.                                                 NC1014.2
079400     MOVE "MPY-TEST-F1-12         " TO PAR-NAME.                  NC1014.2
079500     PERFORM PRINT-DETAIL.                                        NC1014.2
079600*                                                                 NC1014.2
079700*                                                                 NC1014.2
079800 MPY-INIT-F1-13.                                                  NC1014.2
079900*    ===--> NEW SIZE ERROR TESTS <--===                           NC1014.2
080000     MOVE   "VI-67 6.4.2 " TO ANSI-REFERENCE.                     NC1014.2
080100     MOVE   "MPY-TEST-F1-13       " TO PAR-NAME.                  NC1014.2
080200     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
080300     MOVE    1111111111       TO WRK-DS-10V00.                    NC1014.2
080400     MOVE    1 TO REC-CT.                                         NC1014.2
080500 MPY-TEST-F1-13-0.                                                NC1014.2
080600     MULTIPLY A12THREES-DS-06V06 BY WRK-DS-10V00                  NC1014.2
080700              ON SIZE ERROR GO TO MPY-TEST-F1-13-1                NC1014.2
080800              NOT ON SIZE ERROR MOVE "1" TO WRK-XN-00001.         NC1014.2
080900     GO TO MPY-TEST-F1-13-1.                                      NC1014.2
081000 MPY-DELETE-F1-13.                                                NC1014.2
081100     PERFORM DE-LETE.                                             NC1014.2
081200     PERFORM PRINT-DETAIL.                                        NC1014.2
081300     GO TO MPY-INIT-F1-14.                                        NC1014.2
081400 MPY-TEST-F1-13-1.                                                NC1014.2
081500     MOVE   "MPY-TEST-F1-13-1" TO PAR-NAME                        NC1014.2
081600     IF      WRK-XN-00001 = "0"                                   NC1014.2
081700             PERFORM PASS                                         NC1014.2
081800             PERFORM PRINT-DETAIL                                 NC1014.2
081900     ELSE                                                         NC1014.2
082000             MOVE    WRK-XN-00001     TO COMPUTED-X               NC1014.2
082100             MOVE   "0"               TO CORRECT-X                NC1014.2
082200             MOVE   "SIZE ERROR SHOULD HAVE OCCURRED" TO RE-MARK  NC1014.2
082300             PERFORM FAIL                                         NC1014.2
082400             PERFORM PRINT-DETAIL.                                NC1014.2
082500     ADD     1 TO REC-CT.                                         NC1014.2
082600 MPY-TEST-F1-13-2.                                                NC1014.2
082700     MOVE   "MPY-TEST-F1-13-2" TO PAR-NAME                        NC1014.2
082800     IF      WRK-DS-10V00 = 1111111111                            NC1014.2
082900             PERFORM PASS                                         NC1014.2
083000             PERFORM PRINT-DETAIL                                 NC1014.2
083100     ELSE                                                         NC1014.2
083200             MOVE    WRK-DS-10V00       TO COMPUTED-18V0          NC1014.2
083300             MOVE    1111111111         TO CORRECT-18V0           NC1014.2
083400             MOVE   "SIZE ERROR SHOULD HAVE OCCURRED" TO RE-MARK  NC1014.2
083500             PERFORM FAIL                                         NC1014.2
083600             PERFORM PRINT-DETAIL.                                NC1014.2
083700*                                                                 NC1014.2
083800*                                                                 NC1014.2
083900 MPY-INIT-F1-14.                                                  NC1014.2
084000*    ===--> NEW SIZE ERROR TESTS <--===                           NC1014.2
084100     MOVE   "VI-67 6.4.2 " TO ANSI-REFERENCE.                     NC1014.2
084200     MOVE   "MPY-TEST-F1-14        " TO PAR-NAME.                 NC1014.2
084300     MOVE   "1" TO WRK-XN-00001.                                  NC1014.2
084400     MOVE    1  TO REC-CT.                                        NC1014.2
084500     MOVE   -99 TO WRK-DS-02V00.                                  NC1014.2
084600 MPY-TEST-F1-14-0.                                                NC1014.2
084700     MULTIPLY AZERO-DS-05V05 BY WRK-DS-02V00                      NC1014.2
084800          ON SIZE ERROR GO TO MPY-TEST-F1-14-1                    NC1014.2
084900          NOT ON SIZE ERROR MOVE "0" TO WRK-XN-00001.             NC1014.2
085000     GO TO   MPY-TEST-F1-14-1.                                    NC1014.2
085100 MPY-DELETE-F1-14.                                                NC1014.2
085200     PERFORM DE-LETE.                                             NC1014.2
085300     PERFORM PRINT-DETAIL.                                        NC1014.2
085400     GO TO   MPY-INIT-F1-15.                                      NC1014.2
085500 MPY-TEST-F1-14-1.                                                NC1014.2
085600     MOVE   "MPY-TEST-F1-14-1" TO PAR-NAME.                       NC1014.2
085700     IF      WRK-XN-00001 = "0"                                   NC1014.2
085800             PERFORM PASS                                         NC1014.2
085900             PERFORM PRINT-DETAIL                                 NC1014.2
086000     ELSE                                                         NC1014.2
086100             MOVE    WRK-XN-00001     TO COMPUTED-X               NC1014.2
086200             MOVE   "0"               TO CORRECT-X                NC1014.2
086300             MOVE    "NOT ON SIZE ERROR SHOULD BE EXECUTED"       NC1014.2
086400                  TO RE-MARK                                      NC1014.2
086500             PERFORM FAIL                                         NC1014.2
086600             PERFORM PRINT-DETAIL.                                NC1014.2
086700     ADD     1  TO REC-CT.                                        NC1014.2
086800 MPY-TEST-F1-14-2.                                                NC1014.2
086900     MOVE   "MPY-TEST-F1-14-2" TO PAR-NAME.                       NC1014.2
087000     IF      WRK-DS-02V00 = 00                                    NC1014.2
087100             PERFORM PASS                                         NC1014.2
087200             PERFORM PRINT-DETAIL                                 NC1014.2
087300     ELSE                                                         NC1014.2
087400             MOVE    WRK-DS-02V00     TO COMPUTED-N               NC1014.2
087500             MOVE    00               TO CORRECT-N                NC1014.2
087600             PERFORM FAIL                                         NC1014.2
087700             PERFORM PRINT-DETAIL.                                NC1014.2
087800*                                                                 NC1014.2
087900*                                                                 NC1014.2
088000 MPY-INIT-F1-15.                                                  NC1014.2
088100*    ===--> NEW SIZE ERROR TESTS <--===                           NC1014.2
088200     MOVE   "VI-67 6.4.2 " TO ANSI-REFERENCE.                     NC1014.2
088300     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
088400     MOVE    1111111111 TO WRK-DS-10V00.                          NC1014.2
088500     MOVE    1 TO REC-CT.                                         NC1014.2
088600 MPY-TEST-F1-15-0.                                                NC1014.2
088700     MULTIPLY A12THREES-DS-06V06 BY WRK-DS-10V00                  NC1014.2
088800              ON SIZE ERROR MOVE "1" TO WRK-XN-00001              NC1014.2
088900          NOT ON SIZE ERROR MOVE "2" TO WRK-XN-00001.             NC1014.2
089000     GO TO MPY-TEST-F1-15-1.                                      NC1014.2
089100 MPY-DELETE-F1-15.                                                NC1014.2
089200     PERFORM DE-LETE.                                             NC1014.2
089300     PERFORM PRINT-DETAIL.                                        NC1014.2
089400     GO TO MPY-INIT-F1-16.                                        NC1014.2
089500 MPY-TEST-F1-15-1.                                                NC1014.2
089600     MOVE   "MPY-TEST-F1-15-1" TO PAR-NAME.                       NC1014.2
089700     IF      WRK-XN-00001 = "1"                                   NC1014.2
089800             PERFORM PASS                                         NC1014.2
089900             PERFORM PRINT-DETAIL                                 NC1014.2
090000     ELSE                                                         NC1014.2
090100             MOVE    WRK-XN-00001     TO COMPUTED-X               NC1014.2
090200             MOVE   "1"               TO CORRECT-X                NC1014.2
090300             MOVE   "SIZE ERROR SHOULD HAVE OCCURRED" TO RE-MARK  NC1014.2
090400             PERFORM FAIL                                         NC1014.2
090500             PERFORM PRINT-DETAIL.                                NC1014.2
090600     ADD     1 TO REC-CT.                                         NC1014.2
090700 MPY-TEST-F1-15-2.                                                NC1014.2
090800     MOVE   "MPY-TEST-F1-15-2" TO PAR-NAME.                       NC1014.2
090900     IF      WRK-DS-10V00 = 1111111111                            NC1014.2
091000             PERFORM PASS                                         NC1014.2
091100             PERFORM PRINT-DETAIL                                 NC1014.2
091200     ELSE                                                         NC1014.2
091300             MOVE    WRK-DS-10V00       TO COMPUTED-18V0          NC1014.2
091400             MOVE    1111111111         TO CORRECT-18V0           NC1014.2
091500             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1014.2
091600             PERFORM FAIL                                         NC1014.2
091700             PERFORM PRINT-DETAIL.                                NC1014.2
091800*                                                                 NC1014.2
091900*                                                                 NC1014.2
092000 MPY-INIT-F1-16.                                                  NC1014.2
092100*    ===--> NEW SIZE ERROR TESTS <--===                           NC1014.2
092200     MOVE   "VI-67 6.4.2 " TO ANSI-REFERENCE.                     NC1014.2
092300     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
092400     MOVE   -99 TO WRK-DS-02V00.                                  NC1014.2
092500     MOVE    1 TO REC-CT.                                         NC1014.2
092600 MPY-TEST-F1-16-0.                                                NC1014.2
092700     MULTIPLY AZERO-DS-05V05 BY WRK-DS-02V00                      NC1014.2
092800              ON SIZE ERROR MOVE "1" TO WRK-XN-00001              NC1014.2
092900          NOT ON SIZE ERROR MOVE "2" TO WRK-XN-00001.             NC1014.2
093000     GO TO   MPY-TEST-F1-16-1.                                    NC1014.2
093100 MPY-DELETE-F1-16.                                                NC1014.2
093200     PERFORM DE-LETE.                                             NC1014.2
093300     PERFORM PRINT-DETAIL.                                        NC1014.2
093400     GO TO   MPY-INIT-F1-17.                                      NC1014.2
093500 MPY-TEST-F1-16-1.                                                NC1014.2
093600     MOVE   "MPY-TEST-F1-16-1" TO PAR-NAME.                       NC1014.2
093700     IF      WRK-XN-00001 = "2"                                   NC1014.2
093800             PERFORM PASS                                         NC1014.2
093900             PERFORM PRINT-DETAIL                                 NC1014.2
094000     ELSE                                                         NC1014.2
094100             MOVE    WRK-XN-00001     TO COMPUTED-X               NC1014.2
094200             MOVE   "2"               TO CORRECT-X                NC1014.2
094300             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1014.2
094400                  TO RE-MARK                                      NC1014.2
094500             PERFORM FAIL                                         NC1014.2
094600             PERFORM PRINT-DETAIL.                                NC1014.2
094700     ADD     1 TO REC-CT.                                         NC1014.2
094800 MPY-TEST-F1-16-2.                                                NC1014.2
094900     MOVE   "MPY-TEST-F1-16-2" TO PAR-NAME.                       NC1014.2
095000     IF      WRK-DS-02V00 = 00                                    NC1014.2
095100             PERFORM PASS                                         NC1014.2
095200             PERFORM PRINT-DETAIL                                 NC1014.2
095300     ELSE                                                         NC1014.2
095400             MOVE    WRK-DS-02V00     TO COMPUTED-N               NC1014.2
095500             MOVE    00               TO CORRECT-N                NC1014.2
095600             MOVE   "WRONGLY AFFECTED BY SIZE ERROR" TO RE-MARK   NC1014.2
095700             PERFORM FAIL                                         NC1014.2
095800             PERFORM PRINT-DETAIL.                                NC1014.2
095900*                                                                 NC1014.2
096000*                                                                 NC1014.2
096100 MPY-INIT-F1-17.                                                  NC1014.2
096200     MOVE   "VI-106 6.19.4 GR1" TO ANSI-REFERENCE.                NC1014.2
096300*    ===--> MULTIPLE RESULT FIELDS <--===                         NC1014.2
096400     MOVE   "MPY-TEST-F1-17" TO PAR-NAME.                         NC1014.2
096500     MOVE    .00001  TO WRK-DU-4P1-1.                             NC1014.2
096600     MOVE    12345.6 TO WRK-DU-5V1-1.                             NC1014.2
096700     MOVE    1 TO REC-CT.                                         NC1014.2
096800 MPY-TEST-F1-17-0.                                                NC1014.2
096900     MULTIPLY WRK-DU-4P1-1 BY WRK-DU-5V1-1 ROUNDED WRK-DU-2P4-1   NC1014.2
097000         WRK-DU-6V0-1 ROUNDED WRK-DU-6V0-2 WRK-DU-0V12-1.         NC1014.2
097100     GO TO MPY-TEST-F1-17-1.                                      NC1014.2
097200 MPY-DELETE-F1-17.                                                NC1014.2
097300     PERFORM DE-LETE.                                             NC1014.2
097400     MOVE "*DELETED BY FCTC*" TO FEATURE.                         NC1014.2
097500     PERFORM PRINT-DETAIL.                                        NC1014.2
097600     GO TO MPY-INIT-F1-18.                                        NC1014.2
097700 MPY-TEST-F1-17-1.                                                NC1014.2
097800     IF WRK-DU-5V1-1 = .1 PERFORM PASS PERFORM PRINT-DETAIL       NC1014.2
097900     ELSE                                                         NC1014.2
098000     PERFORM FAIL MOVE WRK-DU-5V1-1 TO COMPUTED-N MOVE .1         NC1014.2
098100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
098200     ADD 1 TO REC-CT.                                             NC1014.2
098300 MPY-TEST-F1-17-2.                                                NC1014.2
098400     IF WRK-DU-2P4-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
098500     ELSE                                                         NC1014.2
098600     PERFORM FAIL MOVE WRK-DU-2P4-1 TO COMPUTED-N MOVE 0 TO       NC1014.2
098700     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
098800     ADD 1 TO REC-CT.                                             NC1014.2
098900 MPY-TEST-F1-17-3.                                                NC1014.2
099000     IF WRK-DU-6V0-1 = 1 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
099100     ELSE                                                         NC1014.2
099200     PERFORM FAIL MOVE WRK-DU-6V0-1 TO COMPUTED-N MOVE 1          NC1014.2
099300     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
099400     ADD 1 TO REC-CT.                                             NC1014.2
099500 MPY-TEST-F1-17-4.                                                NC1014.2
099600     IF WRK-DU-6V0-2 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
099700     ELSE                                                         NC1014.2
099800     PERFORM FAIL MOVE WRK-DU-6V0-2 TO COMPUTED-N MOVE 0 TO       NC1014.2
099900     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
100000     ADD 1 TO REC-CT.                                             NC1014.2
100100 MPY-TEST-F1-17-5.                                                NC1014.2
100200     IF WRK-DU-0V12-1 = .0000000001 PERFORM PASS PERFORM          NC1014.2
100300     PRINT-DETAIL ELSE                                            NC1014.2
100400     PERFORM FAIL MOVE WRK-DU-0V12-1 TO COMPUTED-0V18 MOVE        NC1014.2
100500     .0000000001 TO CORRECT-0V18 PERFORM PRINT-DETAIL.            NC1014.2
100600*                                                                 NC1014.2
100700 MPY-INIT-F1-18.                                                  NC1014.2
100800*     =>   SIZE ERROR CONDITION. <--==                            NC1014.2
100900*    ==-->   MULTIPLE RESULT FIELDS<--==                          NC1014.2
101000*    ===--> & SIZE ERROR CONDITIONS<--===                         NC1014.2
101100     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1014.2
101200     MOVE   "MPY-TEST-F1-18" TO PAR-NAME.                         NC1014.2
101300     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
101400     MOVE    0  TO WRK-DU-2P4-1.                                  NC1014.2
101500     MOVE    0  TO WRK-DU-6V0-1.                                  NC1014.2
101600     MOVE    0  TO WRK-DU-6V0-2.                                  NC1014.2
101700     MOVE    0  TO WRK-DU-0V12-1.                                 NC1014.2
101800     MOVE   99  TO WRK-DU-2V0-1.                                  NC1014.2
101900     MOVE 12345.6 TO WRK-DU-5V1-1.                                NC1014.2
102000     MOVE    1 TO REC-CT.                                         NC1014.2
102100 MPY-TEST-F1-18-0.                                                NC1014.2
102200     MULTIPLY WRK-DU-5V1-1 BY WRK-DU-2V0-1 ROUNDED WRK-DU-2P4-1   NC1014.2
102300             WRK-DU-6V0-1 ROUNDED WRK-DU-6V0-2 WRK-DU-0V12-1      NC1014.2
102400             ON SIZE ERROR MOVE "1" TO WRK-XN-00001.              NC1014.2
102500     GO TO MPY-TEST-F1-18-1.                                      NC1014.2
102600 MPY-DELETE-F1-18.                                                NC1014.2
102700     PERFORM DE-LETE.                                             NC1014.2
102800     MOVE   "*DELETED BY FCTC*" TO FEATURE.                       NC1014.2
102900     PERFORM PRINT-DETAIL.                                        NC1014.2
103000     GO TO   MPY-INIT-F1-19.                                      NC1014.2
103100 MPY-TEST-F1-18-1.                                                NC1014.2
103200     IF WRK-DU-2V0-1 = 99 PERFORM PASS PERFORM PRINT-DETAIL       NC1014.2
103300     ELSE                                                         NC1014.2
103400     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 99         NC1014.2
103500     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
103600     ADD 1 TO REC-CT.                                             NC1014.2
103700 MPY-TEST-F1-18-2.                                                NC1014.2
103800     IF WRK-DU-2P4-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
103900     ELSE                                                         NC1014.2
104000     PERFORM FAIL MOVE WRK-DU-2P4-1 TO COMPUTED-N MOVE 0 TO       NC1014.2
104100     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
104200     ADD 1 TO REC-CT.                                             NC1014.2
104300 MPY-TEST-F1-18-3.                                                NC1014.2
104400     IF WRK-DU-6V0-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
104500     ELSE                                                         NC1014.2
104600     PERFORM FAIL MOVE WRK-DU-6V0-1 TO COMPUTED-N MOVE 0          NC1014.2
104700     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
104800     ADD 1 TO REC-CT.                                             NC1014.2
104900 MPY-TEST-F1-18-4.                                                NC1014.2
105000     IF WRK-DU-6V0-2 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
105100     ELSE                                                         NC1014.2
105200     PERFORM FAIL MOVE WRK-DU-6V0-2 TO COMPUTED-N MOVE 0 TO       NC1014.2
105300     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
105400     ADD 1 TO REC-CT.                                             NC1014.2
105500 MPY-TEST-F1-18-5.                                                NC1014.2
105600     IF WRK-DU-0V12-1 = 0 PERFORM PASS PERFORM                    NC1014.2
105700     PRINT-DETAIL ELSE                                            NC1014.2
105800     PERFORM FAIL MOVE WRK-DU-0V12-1 TO COMPUTED-0V18 MOVE 0      NC1014.2
105900     TO CORRECT-0V18 PERFORM PRINT-DETAIL.                        NC1014.2
106000     ADD 1 TO REC-CT.                                             NC1014.2
106100 MPY-TEST-F1-18-6.                                                NC1014.2
106200     IF      WRK-XN-00001 = "1"                                   NC1014.2
106300             PERFORM PASS                                         NC1014.2
106400             PERFORM PRINT-DETAIL                                 NC1014.2
106500     ELSE                                                         NC1014.2
106600             MOVE   "ON SIZE ERROR SHOULD HAVE EXECUTED"          NC1014.2
106700                  TO RE-MARK                                      NC1014.2
106800             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1014.2
106900             MOVE   "1"            TO CORRECT-X                   NC1014.2
107000             PERFORM PRINT-DETAIL.                                NC1014.2
107100*                                                                 NC1014.2
107200 MPY-INIT-F1-19.                                                  NC1014.2
107300*    ==-->  NO   SIZE ERROR CONDITION. <--==                      NC1014.2
107400*    ==-->   MULTIPLE RESULT FIELDS <--==                         NC1014.2
107500*    ===--> & SIZE ERROR CONDITIONS <--===                        NC1014.2
107600     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1014.2
107700     MOVE   "MPY-TEST-F1-19" TO PAR-NAME.                         NC1014.2
107800     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
107900     MOVE    0  TO WRK-DU-2P4-1.                                  NC1014.2
108000     MOVE    0  TO WRK-DU-6V0-1.                                  NC1014.2
108100     MOVE    0  TO WRK-DU-6V0-2.                                  NC1014.2
108200     MOVE   .00001  TO WRK-DU-0V12-1.                             NC1014.2
108300     MOVE   99  TO WRK-DU-2V0-1.                                  NC1014.2
108400     MOVE 12345.6 TO WRK-DU-5V1-1.                                NC1014.2
108500     MOVE .00001 TO WRK-DU-4P1-1.                                 NC1014.2
108600     MOVE    1  TO REC-CT.                                        NC1014.2
108700 MPY-TEST-F1-19-0.                                                NC1014.2
108800     MULTIPLY WRK-DU-4P1-1 BY WRK-DU-5V1-1 ROUNDED WRK-DU-2P4-1   NC1014.2
108900             WRK-DU-6V0-1 ROUNDED WRK-DU-6V0-2                    NC1014.2
109000             WRK-DU-0V12-1                                        NC1014.2
109100             ON SIZE ERROR MOVE "1" TO WRK-XN-00001.              NC1014.2
109200     GO TO MPY-TEST-F1-19-1.                                      NC1014.2
109300 MPY-DELETE-F1-19.                                                NC1014.2
109400     PERFORM DE-LETE.                                             NC1014.2
109500     MOVE "*DELETED BY FCTC*" TO FEATURE.                         NC1014.2
109600     PERFORM PRINT-DETAIL.                                        NC1014.2
109700     GO TO MPY-INIT-F1-20.                                        NC1014.2
109800 MPY-TEST-F1-19-1.                                                NC1014.2
109900     IF WRK-DU-5V1-1 = .1 PERFORM PASS PERFORM PRINT-DETAIL       NC1014.2
110000     ELSE                                                         NC1014.2
110100     PERFORM FAIL MOVE WRK-DU-5V1-1 TO COMPUTED-N MOVE .1         NC1014.2
110200     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
110300     ADD 1 TO REC-CT.                                             NC1014.2
110400 MPY-TEST-F1-19-2.                                                NC1014.2
110500     IF WRK-DU-2P4-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
110600     ELSE                                                         NC1014.2
110700     PERFORM FAIL MOVE WRK-DU-2P4-1 TO COMPUTED-N MOVE 0 TO       NC1014.2
110800     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
110900     ADD 1 TO REC-CT.                                             NC1014.2
111000 MPY-TEST-F1-19-3.                                                NC1014.2
111100     IF WRK-DU-6V0-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
111200     ELSE                                                         NC1014.2
111300     PERFORM FAIL MOVE WRK-DU-6V0-1 TO COMPUTED-N MOVE 0          NC1014.2
111400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
111500     ADD 1 TO REC-CT.                                             NC1014.2
111600 MPY-TEST-F1-19-4.                                                NC1014.2
111700     IF WRK-DU-6V0-2 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
111800     ELSE                                                         NC1014.2
111900     PERFORM FAIL MOVE WRK-DU-6V0-2 TO COMPUTED-N MOVE 0 TO       NC1014.2
112000     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
112100     ADD 1 TO REC-CT.                                             NC1014.2
112200 MPY-TEST-F1-19-5.                                                NC1014.2
112300     IF WRK-DU-0V12-1 = .0000000001 PERFORM PASS PERFORM          NC1014.2
112400     PRINT-DETAIL ELSE                                            NC1014.2
112500     PERFORM FAIL MOVE WRK-DU-0V12-1 TO COMPUTED-0V18 MOVE        NC1014.2
112600     .0000000001 TO CORRECT-0V18 PERFORM PRINT-DETAIL.            NC1014.2
112700     ADD 1 TO REC-CT.                                             NC1014.2
112800 MPY-TEST-F1-19-6.                                                NC1014.2
112900     IF      WRK-XN-00001 = "0"                                   NC1014.2
113000             PERFORM PASS                                         NC1014.2
113100             PERFORM PRINT-DETAIL                                 NC1014.2
113200     ELSE                                                         NC1014.2
113300             MOVE   "ON SIZE ERROR SHOULD NOT HAVE EXECUTED"      NC1014.2
113400                  TO RE-MARK                                      NC1014.2
113500             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1014.2
113600             MOVE   "0"            TO CORRECT-X                   NC1014.2
113700             PERFORM PRINT-DETAIL.                                NC1014.2
113800*                                                                 NC1014.2
113900 MPY-INIT-F1-20.                                                  NC1014.2
114000*   ==-->   SIZE ERROR CONDITION.   <--==                         NC1014.2
114100*   ==-->  MULTIPLE RESULT FIELDS   <--==                         NC1014.2
114200*   ==-->  NEW SIZE ERROR TESTS     <--==                         NC1014.2
114300     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1014.2
114400     MOVE   "MPY-TEST-F1-20" TO PAR-NAME.                         NC1014.2
114500     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
114600     MOVE    0  TO WRK-DU-2P4-1.                                  NC1014.2
114700     MOVE    0  TO WRK-DU-6V0-1.                                  NC1014.2
114800     MOVE    0  TO WRK-DU-6V0-2.                                  NC1014.2
114900     MOVE    0  TO WRK-DU-0V12-1.                                 NC1014.2
115000     MOVE   99  TO WRK-DU-2V0-1.                                  NC1014.2
115100     MOVE 12345.6 TO WRK-DU-5V1-1.                                NC1014.2
115200     MOVE    1 TO REC-CT.                                         NC1014.2
115300 MPY-TEST-F1-20-0.                                                NC1014.2
115400     MULTIPLY WRK-DU-5V1-1 BY WRK-DU-2V0-1 ROUNDED WRK-DU-2P4-1   NC1014.2
115500             WRK-DU-6V0-1 ROUNDED WRK-DU-6V0-2 WRK-DU-0V12-1      NC1014.2
115600             ON SIZE ERROR GO TO MPY-TEST-F1-20-1                 NC1014.2
115700         NOT ON SIZE ERROR MOVE "1" TO WRK-XN-00001.              NC1014.2
115800     GO TO MPY-TEST-F1-20-1.                                      NC1014.2
115900 MPY-DELETE-F1-20.                                                NC1014.2
116000     PERFORM DE-LETE.                                             NC1014.2
116100     MOVE "*DELETED BY FCTC*" TO FEATURE.                         NC1014.2
116200     PERFORM PRINT-DETAIL.                                        NC1014.2
116300     GO TO MPY-INIT-F1-21.                                        NC1014.2
116400 MPY-TEST-F1-20-1.                                                NC1014.2
116500     IF WRK-DU-2V0-1 = 99 PERFORM PASS PERFORM PRINT-DETAIL       NC1014.2
116600     ELSE                                                         NC1014.2
116700     PERFORM FAIL MOVE WRK-DU-2V0-1 TO COMPUTED-N MOVE 99         NC1014.2
116800     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
116900     ADD 1 TO REC-CT.                                             NC1014.2
117000 MPY-TEST-F1-20-2.                                                NC1014.2
117100     IF WRK-DU-2P4-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
117200     ELSE                                                         NC1014.2
117300     PERFORM FAIL MOVE WRK-DU-2P4-1 TO COMPUTED-N MOVE 0 TO       NC1014.2
117400     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
117500     ADD 1 TO REC-CT.                                             NC1014.2
117600 MPY-TEST-F1-20-3.                                                NC1014.2
117700     IF WRK-DU-6V0-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
117800     ELSE                                                         NC1014.2
117900     PERFORM FAIL MOVE WRK-DU-6V0-1 TO COMPUTED-N MOVE 0          NC1014.2
118000     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
118100     ADD 1 TO REC-CT.                                             NC1014.2
118200 MPY-TEST-F1-20-4.                                                NC1014.2
118300     IF WRK-DU-6V0-2 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
118400     ELSE                                                         NC1014.2
118500     PERFORM FAIL MOVE WRK-DU-6V0-2 TO COMPUTED-N MOVE 0 TO       NC1014.2
118600     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
118700     ADD 1 TO REC-CT.                                             NC1014.2
118800 MPY-TEST-F1-20-5.                                                NC1014.2
118900     IF WRK-DU-0V12-1 = 0 PERFORM PASS PERFORM                    NC1014.2
119000     PRINT-DETAIL ELSE                                            NC1014.2
119100     PERFORM FAIL MOVE WRK-DU-0V12-1 TO COMPUTED-0V18 MOVE 0      NC1014.2
119200     TO CORRECT-0V18 PERFORM PRINT-DETAIL.                        NC1014.2
119300     ADD 1 TO REC-CT.                                             NC1014.2
119400 MPY-TEST-F1-20-6.                                                NC1014.2
119500     IF      WRK-XN-00001 = "0"                                   NC1014.2
119600             PERFORM PASS                                         NC1014.2
119700             PERFORM PRINT-DETAIL                                 NC1014.2
119800     ELSE                                                         NC1014.2
119900             MOVE   "NOT ON SIZE ERROR SHOULD NOT HAVE EXECUTED"  NC1014.2
120000                  TO RE-MARK                                      NC1014.2
120100             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1014.2
120200             MOVE   "0"            TO CORRECT-X                   NC1014.2
120300             PERFORM PRINT-DETAIL.                                NC1014.2
120400*                                                                 NC1014.2
120500 MPY-INIT-F1-21.                                                  NC1014.2
120600*   ==-->   NO SIZE ERROR CONDITION.  <--==                       NC1014.2
120700*   ==-->   MULTIPLE RESULT FIELDS    <--==                       NC1014.2
120800*   ==-->    NEW SIZE ERROR TESTS     <--==                       NC1014.2
120900     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1014.2
121000     MOVE   "MPY-TEST-F1-21" TO PAR-NAME.                         NC1014.2
121100     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
121200     MOVE    0  TO WRK-DU-2P4-1.                                  NC1014.2
121300     MOVE    0  TO WRK-DU-6V0-1.                                  NC1014.2
121400     MOVE    0  TO WRK-DU-6V0-2.                                  NC1014.2
121500     MOVE   .00001  TO WRK-DU-0V12-1.                             NC1014.2
121600     MOVE   99  TO WRK-DU-2V0-1.                                  NC1014.2
121700     MOVE 12345.6 TO WRK-DU-5V1-1.                                NC1014.2
121800     MOVE .00001 TO WRK-DU-4P1-1.                                 NC1014.2
121900     MOVE    1  TO REC-CT.                                        NC1014.2
122000 MPY-TEST-F1-21-0.                                                NC1014.2
122100     MULTIPLY WRK-DU-4P1-1 BY WRK-DU-5V1-1 ROUNDED WRK-DU-2P4-1   NC1014.2
122200             WRK-DU-6V0-1 ROUNDED                                 NC1014.2
122300             WRK-DU-6V0-2 ROUNDED WRK-DU-0V12-1                   NC1014.2
122400             ON SIZE ERROR GO TO MPY-TEST-F1-21-1                 NC1014.2
122500         NOT ON SIZE ERROR MOVE "1" TO WRK-XN-00001.              NC1014.2
122600     GO TO  MPY-TEST-F1-21-1.                                     NC1014.2
122700 MPY-DELETE-F1-21.                                                NC1014.2
122800     PERFORM DE-LETE.                                             NC1014.2
122900     MOVE "*DELETED BY FCTC*" TO FEATURE.                         NC1014.2
123000     PERFORM PRINT-DETAIL.                                        NC1014.2
123100     GO TO  MPY-INIT-F1-22.                                       NC1014.2
123200 MPY-TEST-F1-21-1.                                                NC1014.2
123300     IF  WRK-DU-5V1-1 = .1 PERFORM PASS PERFORM PRINT-DETAIL      NC1014.2
123400     ELSE                                                         NC1014.2
123500     PERFORM FAIL MOVE WRK-DU-5V1-1 TO COMPUTED-N MOVE .1         NC1014.2
123600     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
123700     ADD 1 TO REC-CT.                                             NC1014.2
123800 MPY-TEST-F1-21-2.                                                NC1014.2
123900     IF WRK-DU-2P4-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
124000     ELSE                                                         NC1014.2
124100     PERFORM FAIL MOVE WRK-DU-2P4-1 TO COMPUTED-N MOVE 0 TO       NC1014.2
124200     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
124300     ADD 1 TO REC-CT.                                             NC1014.2
124400 MPY-TEST-F1-21-3.                                                NC1014.2
124500     IF WRK-DU-6V0-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
124600     ELSE                                                         NC1014.2
124700     PERFORM FAIL MOVE WRK-DU-6V0-1 TO COMPUTED-N MOVE 1          NC1014.2
124800     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
124900     ADD 1 TO REC-CT.                                             NC1014.2
125000 MPY-TEST-F1-21-4.                                                NC1014.2
125100     IF WRK-DU-6V0-2 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
125200     ELSE                                                         NC1014.2
125300     PERFORM FAIL MOVE WRK-DU-6V0-2 TO COMPUTED-N MOVE 0 TO       NC1014.2
125400     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
125500     ADD 1 TO REC-CT.                                             NC1014.2
125600 MPY-TEST-F1-21-5.                                                NC1014.2
125700     IF WRK-DU-0V12-1 = .0000000001 PERFORM PASS PERFORM          NC1014.2
125800     PRINT-DETAIL ELSE                                            NC1014.2
125900     PERFORM FAIL MOVE WRK-DU-0V12-1 TO COMPUTED-0V18 MOVE        NC1014.2
126000     .0000000001 TO CORRECT-0V18 PERFORM PRINT-DETAIL.            NC1014.2
126100     ADD 1 TO REC-CT.                                             NC1014.2
126200 MPY-TEST-F1-21-6.                                                NC1014.2
126300     IF      WRK-XN-00001 = "1"                                   NC1014.2
126400             PERFORM PASS                                         NC1014.2
126500             PERFORM PRINT-DETAIL                                 NC1014.2
126600     ELSE                                                         NC1014.2
126700             MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"      NC1014.2
126800                  TO RE-MARK                                      NC1014.2
126900             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1014.2
127000             MOVE   "1"            TO CORRECT-X                   NC1014.2
127100             PERFORM PRINT-DETAIL.                                NC1014.2
127200*                                                                 NC1014.2
127300 MPY-INIT-F1-22.                                                  NC1014.2
127400*   ==-->   SIZE ERROR CONDITION.   <--==                         NC1014.2
127500*   ==-->   MULTIPLE RESULT FIELDS  <--==                         NC1014.2
127600*   ==-->   NEW SIZE ERROR TESTS    <--==                         NC1014.2
127700     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1014.2
127800     MOVE   "MPY-TEST-F1-22" TO PAR-NAME.                         NC1014.2
127900     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
128000     MOVE    0  TO WRK-DU-2P4-1.                                  NC1014.2
128100     MOVE    0  TO WRK-DU-6V0-1.                                  NC1014.2
128200     MOVE    0  TO WRK-DU-6V0-2.                                  NC1014.2
128300     MOVE    0  TO WRK-DU-0V12-1.                                 NC1014.2
128400     MOVE   99  TO WRK-DU-2V0-1.                                  NC1014.2
128500     MOVE 12345.6 TO WRK-DU-5V1-1.                                NC1014.2
128600     MOVE    1  TO REC-CT.                                        NC1014.2
128700 MPY-TEST-F1-22-0.                                                NC1014.2
128800     MULTIPLY WRK-DU-5V1-1 BY WRK-DU-2V0-1 ROUNDED WRK-DU-2P4-1   NC1014.2
128900             WRK-DU-6V0-1 ROUNDED WRK-DU-6V0-2 WRK-DU-0V12-1      NC1014.2
129000             ON SIZE ERROR MOVE "1" TO WRK-XN-00001               NC1014.2
129100         NOT ON SIZE ERROR MOVE "2" TO WRK-XN-00001.              NC1014.2
129200     GO TO MPY-TEST-F1-22-1.                                      NC1014.2
129300 MPY-DELETE-F1-22.                                                NC1014.2
129400     PERFORM DE-LETE.                                             NC1014.2
129500     MOVE "*DELETED BY FCTC*" TO FEATURE.                         NC1014.2
129600     PERFORM PRINT-DETAIL.                                        NC1014.2
129700     GO TO MPY-INIT-F1-23.                                        NC1014.2
129800 MPY-TEST-F1-22-1.                                                NC1014.2
129900     IF WRK-DU-5V1-1 = 12345.6 PERFORM PASS PERFORM PRINT-DETAIL  NC1014.2
130000     ELSE                                                         NC1014.2
130100     PERFORM FAIL MOVE WRK-DU-5V1-1 TO COMPUTED-N MOVE 12345.6    NC1014.2
130200     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
130300     ADD 1 TO REC-CT.                                             NC1014.2
130400 MPY-TEST-F1-22-2.                                                NC1014.2
130500     IF WRK-DU-2P4-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
130600     ELSE                                                         NC1014.2
130700     PERFORM FAIL MOVE WRK-DU-2P4-1 TO COMPUTED-N MOVE 0 TO       NC1014.2
130800     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
130900     ADD 1 TO REC-CT.                                             NC1014.2
131000 MPY-TEST-F1-22-3.                                                NC1014.2
131100     IF WRK-DU-6V0-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
131200     ELSE                                                         NC1014.2
131300     PERFORM FAIL MOVE WRK-DU-6V0-1 TO COMPUTED-N MOVE 0          NC1014.2
131400     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
131500     ADD 1 TO REC-CT.                                             NC1014.2
131600 MPY-TEST-F1-22-4.                                                NC1014.2
131700     IF WRK-DU-6V0-2 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
131800     ELSE                                                         NC1014.2
131900     PERFORM FAIL MOVE WRK-DU-6V0-2 TO COMPUTED-N MOVE 0 TO       NC1014.2
132000     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
132100     ADD 1 TO REC-CT.                                             NC1014.2
132200 MPY-TEST-F1-22-5.                                                NC1014.2
132300     IF WRK-DU-0V12-1 = 0 PERFORM PASS PERFORM                    NC1014.2
132400     PRINT-DETAIL ELSE                                            NC1014.2
132500     PERFORM FAIL MOVE WRK-DU-0V12-1 TO COMPUTED-0V18 MOVE        NC1014.2
132600     0 TO CORRECT-0V18 PERFORM PRINT-DETAIL.                      NC1014.2
132700     ADD 1 TO REC-CT.                                             NC1014.2
132800 MPY-TEST-F1-22-6.                                                NC1014.2
132900     IF      WRK-XN-00001 = "1"                                   NC1014.2
133000             PERFORM PASS                                         NC1014.2
133100             PERFORM PRINT-DETAIL                                 NC1014.2
133200     ELSE                                                         NC1014.2
133300             MOVE   "ON SIZE ERROR SHOULD HAVE EXECUTED"          NC1014.2
133400                  TO RE-MARK                                      NC1014.2
133500             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1014.2
133600             MOVE   "1"            TO CORRECT-X                   NC1014.2
133700             PERFORM PRINT-DETAIL.                                NC1014.2
133800*                                                                 NC1014.2
133900*                                                                 NC1014.2
134000 MPY-INIT-F1-23.                                                  NC1014.2
134100*   ==-->   NO SIZE ERROR CONDITION.   <--==                      NC1014.2
134200*   ==-->   MULTIPLE RESULT FIELDS     <--==                      NC1014.2
134300*   ==-->   NEW SIZE ERROR TESTS       <--==                      NC1014.2
134400     MOVE   "VI-67 6.4.2" TO ANSI-REFERENCE.                      NC1014.2
134500     MOVE   "MPY-TEST-F1-23" TO PAR-NAME.                         NC1014.2
134600     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
134700     MOVE    0  TO WRK-DU-2P4-1.                                  NC1014.2
134800     MOVE    0  TO WRK-DU-6V0-1.                                  NC1014.2
134900     MOVE    0  TO WRK-DU-6V0-2.                                  NC1014.2
135000     MOVE   .00001  TO WRK-DU-0V12-1.                             NC1014.2
135100     MOVE   99  TO WRK-DU-2V0-1.                                  NC1014.2
135200     MOVE 12345.6 TO WRK-DU-5V1-1.                                NC1014.2
135300     MOVE .00001 TO WRK-DU-4P1-1.                                 NC1014.2
135400     MOVE    1  TO REC-CT.                                        NC1014.2
135500 MPY-TEST-F1-23-0.                                                NC1014.2
135600     MULTIPLY WRK-DU-4P1-1 BY WRK-DU-5V1-1 ROUNDED WRK-DU-2P4-1   NC1014.2
135700             WRK-DU-6V0-1 ROUNDED                                 NC1014.2
135800             WRK-DU-6V0-2 ROUNDED WRK-DU-0V12-1                   NC1014.2
135900             ON SIZE ERROR MOVE "1" TO WRK-XN-00001               NC1014.2
136000         NOT ON SIZE ERROR MOVE "2" TO WRK-XN-00001.              NC1014.2
136100     GO TO MPY-TEST-F1-23-1.                                      NC1014.2
136200 MPY-DELETE-F1-23.                                                NC1014.2
136300     PERFORM DE-LETE.                                             NC1014.2
136400     MOVE "*DELETED BY FCTC*" TO FEATURE.                         NC1014.2
136500     PERFORM PRINT-DETAIL.                                        NC1014.2
136600     GO TO MPY-INIT-F1-24.                                        NC1014.2
136700 MPY-TEST-F1-23-1.                                                NC1014.2
136800     IF WRK-DU-5V1-1 = .1 PERFORM PASS PERFORM PRINT-DETAIL       NC1014.2
136900     ELSE                                                         NC1014.2
137000     PERFORM FAIL MOVE WRK-DU-5V1-1 TO COMPUTED-N MOVE .1         NC1014.2
137100     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
137200     ADD 1 TO REC-CT.                                             NC1014.2
137300 MPY-TEST-F1-23-2.                                                NC1014.2
137400     IF WRK-DU-2P4-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
137500     ELSE                                                         NC1014.2
137600     PERFORM FAIL MOVE WRK-DU-2P4-1 TO COMPUTED-N MOVE 0 TO       NC1014.2
137700     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
137800     ADD 1 TO REC-CT.                                             NC1014.2
137900 MPY-TEST-F1-23-3.                                                NC1014.2
138000     IF WRK-DU-6V0-1 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
138100     ELSE                                                         NC1014.2
138200     PERFORM FAIL MOVE WRK-DU-6V0-1 TO COMPUTED-N MOVE 0          NC1014.2
138300     TO CORRECT-N PERFORM PRINT-DETAIL.                           NC1014.2
138400     ADD 1 TO REC-CT.                                             NC1014.2
138500 MPY-TEST-F1-23-4.                                                NC1014.2
138600     IF WRK-DU-6V0-2 = 0 PERFORM PASS PERFORM PRINT-DETAIL        NC1014.2
138700     ELSE                                                         NC1014.2
138800     PERFORM FAIL MOVE WRK-DU-6V0-2 TO COMPUTED-N MOVE 0 TO       NC1014.2
138900     CORRECT-N PERFORM PRINT-DETAIL.                              NC1014.2
139000     ADD 1 TO REC-CT.                                             NC1014.2
139100 MPY-TEST-F1-23-5.                                                NC1014.2
139200     IF WRK-DU-0V12-1 = .0000000001 PERFORM PASS PERFORM          NC1014.2
139300     PRINT-DETAIL ELSE                                            NC1014.2
139400     PERFORM FAIL MOVE WRK-DU-0V12-1 TO COMPUTED-0V18 MOVE        NC1014.2
139500     .0000000001 TO CORRECT-0V18 PERFORM PRINT-DETAIL.            NC1014.2
139600     ADD 1 TO REC-CT.                                             NC1014.2
139700 MPY-TEST-F1-23-6.                                                NC1014.2
139800     IF      WRK-XN-00001 = "2"                                   NC1014.2
139900             PERFORM PASS                                         NC1014.2
140000             PERFORM PRINT-DETAIL                                 NC1014.2
140100     ELSE                                                         NC1014.2
140200             MOVE   "NOT ON SIZE ERROR SHOULD HAVE EXECUTED"      NC1014.2
140300                  TO RE-MARK                                      NC1014.2
140400             MOVE    WRK-XN-00001  TO COMPUTED-X                  NC1014.2
140500             MOVE   "1"            TO CORRECT-X                   NC1014.2
140600             PERFORM PRINT-DETAIL.                                NC1014.2
140700*                                                                 NC1014.2
140800*                                                                 NC1014.2
140900 MPY-INIT-F1-24.                                                  NC1014.2
141000*    ==-->EXPLICIT SCOPE TERMINATOR <--==                         NC1014.2
141100*    ==-->   SIZE ERROR CONDITION   <--==                         NC1014.2
141200     MOVE   "IV-41 6.4.3" TO ANSI-REFERENCE.                      NC1014.2
141300     MOVE   "MPY-TEST-F1-24" TO PAR-NAME                          NC1014.2
141400     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
141500     MOVE    A10ONES-DS-10V00 TO WRK-DS-10V00.                    NC1014.2
141600     MOVE    0  TO WRK-DS-05V00.                                  NC1014.2
141700     MOVE    0  TO WRK-DS-02V00.                                  NC1014.2
141800     MOVE    0  TO WRK-CS-18V00.                                  NC1014.2
141900     MOVE    1  TO REC-CT.                                        NC1014.2
142000 MPY-TEST-F1-24-0.                                                NC1014.2
142100     MULTIPLY A12THREES-DS-06V06 BY WRK-DS-10V00                  NC1014.2
142200         ON SIZE ERROR MOVE "1" TO WRK-XN-00001                   NC1014.2
142300                       MOVE 23  TO WRK-DS-05V00                   NC1014.2
142400                       MOVE -4  TO WRK-DS-02V00                   NC1014.2
142500     END-MULTIPLY                                                 NC1014.2
142600     MOVE    99 TO WRK-CS-18V00.                                  NC1014.2
142700     GO TO   MPY-TEST-F1-24-1.                                    NC1014.2
142800 MPY-DELETE-F1-24-1.                                              NC1014.2
142900     PERFORM DE-LETE.                                             NC1014.2
143000     PERFORM PRINT-DETAIL.                                        NC1014.2
143100     GO TO   MPY-INIT-F1-25.                                      NC1014.2
143200 MPY-TEST-F1-24-1.                                                NC1014.2
143300     MOVE   "MPY-TEST-F1-24-1" TO PAR-NAME.                       NC1014.2
143400     IF      WRK-XN-00001 = "1"                                   NC1014.2
143500             PERFORM PASS                                         NC1014.2
143600             PERFORM PRINT-DETAIL                                 NC1014.2
143700     ELSE                                                         NC1014.2
143800             MOVE   WRK-XN-00001 TO COMPUTED-X                    NC1014.2
143900             MOVE  "1"           TO CORRECT-X                     NC1014.2
144000             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1014.2
144100             PERFORM FAIL                                         NC1014.2
144200             PERFORM PRINT-DETAIL.                                NC1014.2
144300     ADD     1 TO REC-CT.                                         NC1014.2
144400 MPY-TEST-F1-24-2.                                                NC1014.2
144500     MOVE   "MPY-TEST-F1-24-2" TO PAR-NAME.                       NC1014.2
144600     IF      WRK-DS-05V00 = 23                                    NC1014.2
144700             PERFORM PASS                                         NC1014.2
144800             PERFORM PRINT-DETAIL                                 NC1014.2
144900     ELSE                                                         NC1014.2
145000             MOVE   WRK-DS-05V00 TO COMPUTED-N                    NC1014.2
145100             MOVE   23           TO CORRECT-N                     NC1014.2
145200             MOVE   "ON SIZE ERROR NOT EXECUTED" TO RE-MARK       NC1014.2
145300             PERFORM FAIL                                         NC1014.2
145400             PERFORM PRINT-DETAIL.                                NC1014.2
145500     ADD     1 TO REC-CT.                                         NC1014.2
145600 MPY-TEST-F1-24-3.                                                NC1014.2
145700     MOVE   "MPY-TEST-F1-24-3" TO PAR-NAME.                       NC1014.2
145800     IF      WRK-DS-02V00 = -4                                    NC1014.2
145900             PERFORM PASS                                         NC1014.2
146000             PERFORM PRINT-DETAIL                                 NC1014.2
146100     ELSE                                                         NC1014.2
146200             MOVE   WRK-DS-02V00 TO COMPUTED-N                    NC1014.2
146300             MOVE   -4           TO CORRECT-N                     NC1014.2
146400             MOVE  "ON SIZE ERROR NOT EXECUTED" TO RE-MARK        NC1014.2
146500             PERFORM FAIL                                         NC1014.2
146600             PERFORM PRINT-DETAIL.                                NC1014.2
146700     ADD     1 TO REC-CT.                                         NC1014.2
146800 MPY-TEST-F1-24-4.                                                NC1014.2
146900     MOVE   "MPY-TEST-F1-24-4"  TO PAR-NAME.                      NC1014.2
147000     IF      WRK-DS-10V00 = 1111111111                            NC1014.2
147100             PERFORM PASS                                         NC1014.2
147200             PERFORM PRINT-DETAIL                                 NC1014.2
147300     ELSE                                                         NC1014.2
147400             MOVE   WRK-DS-10V00       TO COMPUTED-18V0           NC1014.2
147500             MOVE   1111111111         TO CORRECT-18V0            NC1014.2
147600             MOVE  "ON SIZE ERROR NOT EXECUTED" TO RE-MARK        NC1014.2
147700             PERFORM FAIL                                         NC1014.2
147800             PERFORM PRINT-DETAIL.                                NC1014.2
147900     ADD     1 TO REC-CT.                                         NC1014.2
148000 MPY-TEST-F1-24-5.                                                NC1014.2
148100     MOVE   "MPY-TEST-F1-24-5" TO PAR-NAME.                       NC1014.2
148200     IF      WRK-CS-18V00 = 000000000000000099                    NC1014.2
148300             PERFORM PASS                                         NC1014.2
148400             PERFORM PRINT-DETAIL                                 NC1014.2
148500     ELSE                                                         NC1014.2
148600             MOVE   WRK-CS-18V00       TO COMPUTED-N              NC1014.2
148700             MOVE   000000000000000099 TO CORRECT-N               NC1014.2
148800             MOVE  "SCOPE TERMINATOR IGNORED" TO RE-MARK          NC1014.2
148900             PERFORM FAIL                                         NC1014.2
149000             PERFORM PRINT-DETAIL.                                NC1014.2
149100*                                                                 NC1014.2
149200*                                                                 NC1014.2
149300 MPY-INIT-F1-25.                                                  NC1014.2
149400*    ==-->EXPLICIT SCOPE TERMINATOR <--==                         NC1014.2
149500     MOVE   "IV-41 6.4.3" TO ANSI-REFERENCE.                      NC1014.2
149600     MOVE   "MPY-TEST-F1-25" TO PAR-NAME                          NC1014.2
149700     MOVE   "1" TO WRK-XN-00001.                                  NC1014.2
149800     MOVE   -99 TO WRK-DS-02V00.                                  NC1014.2
149900     MOVE    0  TO WRK-DS-10V00.                                  NC1014.2
150000     MOVE    0  TO WRK-DS-01V00.                                  NC1014.2
150100     MOVE    0  TO WRK-CS-18V00.                                  NC1014.2
150200     MOVE    1  TO REC-CT.                                        NC1014.2
150300 MPY-TEST-F1-25-0.                                                NC1014.2
150400     MULTIPLY AZERO-DS-05V05 BY WRK-DS-02V00                      NC1014.2
150500          ON SIZE ERROR MOVE "0" TO WRK-XN-00001                  NC1014.2
150600                        MOVE 23  TO WRK-DS-10V00                  NC1014.2
150700                        MOVE -4  TO WRK-DS-01V00                  NC1014.2
150800     END-MULTIPLY                                                 NC1014.2
150900     MOVE    99 TO WRK-CS-18V00.                                  NC1014.2
151000     GO TO   MPY-TEST-F1-25-1.                                    NC1014.2
151100 MPY-DELETE-F1-25-1.                                              NC1014.2
151200     PERFORM DE-LETE.                                             NC1014.2
151300     PERFORM PRINT-DETAIL.                                        NC1014.2
151400     GO TO   MPY-INIT-F1-26.                                      NC1014.2
151500 MPY-TEST-F1-25-1.                                                NC1014.2
151600     MOVE   "MPY-TEST-F1-25-1" TO PAR-NAME.                       NC1014.2
151700     IF      WRK-XN-00001 = "1"                                   NC1014.2
151800             PERFORM PASS                                         NC1014.2
151900             PERFORM PRINT-DETAIL                                 NC1014.2
152000     ELSE                                                         NC1014.2
152100             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1014.2
152200             MOVE   "1"           TO CORRECT-X                    NC1014.2
152300             MOVE   "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARKNC1014.2
152400             PERFORM FAIL                                         NC1014.2
152500             PERFORM PRINT-DETAIL.                                NC1014.2
152600     ADD     1  TO REC-CT.                                        NC1014.2
152700 MPY-TEST-F1-25-2.                                                NC1014.2
152800     MOVE   "MPY-TEST-F1-25-2" TO PAR-NAME.                       NC1014.2
152900     IF      WRK-DS-10V00 = 0000000000                            NC1014.2
153000             PERFORM PASS                                         NC1014.2
153100             PERFORM PRINT-DETAIL                                 NC1014.2
153200     ELSE                                                         NC1014.2
153300             MOVE    WRK-DS-10V00 TO COMPUTED-N                   NC1014.2
153400             MOVE    0000000000   TO CORRECT-N                    NC1014.2
153500             MOVE   "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARKNC1014.2
153600             PERFORM FAIL                                         NC1014.2
153700             PERFORM PRINT-DETAIL.                                NC1014.2
153800     ADD     1  TO REC-CT.                                        NC1014.2
153900 MPY-TEST-F1-25-3.                                                NC1014.2
154000     MOVE   "MPY-TEST-F1-25-3" TO PAR-NAME.                       NC1014.2
154100     IF      WRK-DS-01V00 = 0                                     NC1014.2
154200             PERFORM PASS                                         NC1014.2
154300             PERFORM PRINT-DETAIL                                 NC1014.2
154400     ELSE                                                         NC1014.2
154500             MOVE   WRK-DS-01V00 TO COMPUTED-N                    NC1014.2
154600             MOVE   0            TO CORRECT-N                     NC1014.2
154700             MOVE   "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARKNC1014.2
154800             PERFORM FAIL                                         NC1014.2
154900             PERFORM PRINT-DETAIL.                                NC1014.2
155000     ADD     1  TO REC-CT.                                        NC1014.2
155100 MPY-TEST-F1-25-4.                                                NC1014.2
155200     MOVE   "MPY-TEST-F1-25-4" TO PAR-NAME.                       NC1014.2
155300     IF      WRK-DS-02V00 = 00                                    NC1014.2
155400             PERFORM PASS                                         NC1014.2
155500             PERFORM PRINT-DETAIL                                 NC1014.2
155600     ELSE                                                         NC1014.2
155700             MOVE   WRK-DS-02V00 TO COMPUTED-N                    NC1014.2
155800             MOVE   00           TO CORRECT-N                     NC1014.2
155900             MOVE   "SIZE ERROR SHOULD NOT BE EXECUTED" TO RE-MARKNC1014.2
156000             PERFORM FAIL                                         NC1014.2
156100             PERFORM PRINT-DETAIL.                                NC1014.2
156200     ADD     1  TO REC-CT.                                        NC1014.2
156300 MPY-TEST-F1-25-5.                                                NC1014.2
156400     MOVE   "MPY-TEST-F1-25-5" TO PAR-NAME.                       NC1014.2
156500     IF      WRK-CS-18V00 = 000000000000000099                    NC1014.2
156600             PERFORM PASS                                         NC1014.2
156700             PERFORM PRINT-DETAIL                                 NC1014.2
156800     ELSE                                                         NC1014.2
156900             MOVE   WRK-CS-18V00       TO COMPUTED-N              NC1014.2
157000             MOVE   000000000000000099 TO CORRECT-N               NC1014.2
157100             MOVE  "SCOPE TERMINATOR IGNORED" TO RE-MARK          NC1014.2
157200             PERFORM FAIL                                         NC1014.2
157300             PERFORM PRINT-DETAIL.                                NC1014.2
157400*                                                                 NC1014.2
157500*                                                                 NC1014.2
157600 MPY-INIT-F1-26.                                                  NC1014.2
157700*    ==-->EXPLICIT SCOPE TERMINATOR <--==                         NC1014.2
157800     MOVE   "IV-41 6.4.3" TO ANSI-REFERENCE.                      NC1014.2
157900     MOVE   "MPY-TEST-F1-26" TO PAR-NAME                          NC1014.2
158000     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
158100     MOVE    A10ONES-DS-10V00        TO WRK-DS-10V00.             NC1014.2
158200     MOVE    0  TO WRK-DS-05V00.                                  NC1014.2
158300     MOVE    0  TO WRK-DS-02V00.                                  NC1014.2
158400     MOVE    0  TO WRK-CS-18V00.                                  NC1014.2
158500     MOVE    1  TO REC-CT.                                        NC1014.2
158600 MPY-TEST-F1-26-0.                                                NC1014.2
158700     MULTIPLY A12THREES-DS-06V06     BY WRK-DS-10V00              NC1014.2
158800          ON SIZE ERROR GO TO MPY-TEST-F1-26-01                   NC1014.2
158900          NOT ON SIZE ERROR MOVE "1" TO WRK-XN-00001              NC1014.2
159000                            MOVE 23  TO WRK-DS-05V00              NC1014.2
159100                            MOVE -4  TO WRK-DS-02V00              NC1014.2
159200     END-MULTIPLY.                                                NC1014.2
159300 MPY-TEST-F1-26-01.                                               NC1014.2
159400     MOVE    99 TO WRK-CS-18V00.                                  NC1014.2
159500     GO TO   MPY-TEST-F1-26-1.                                    NC1014.2
159600 MPY-DELETE-F1-26-1.                                              NC1014.2
159700     PERFORM DE-LETE.                                             NC1014.2
159800     PERFORM PRINT-DETAIL.                                        NC1014.2
159900     GO TO   MPY-INIT-F1-27.                                      NC1014.2
160000 MPY-TEST-F1-26-1.                                                NC1014.2
160100     MOVE   "MPY-TEST-F1-26-1" TO PAR-NAME.                       NC1014.2
160200     IF      WRK-XN-00001 = "0"                                   NC1014.2
160300             PERFORM PASS                                         NC1014.2
160400             PERFORM PRINT-DETAIL                                 NC1014.2
160500     ELSE                                                         NC1014.2
160600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1014.2
160700             MOVE   "0"           TO CORRECT-X                    NC1014.2
160800             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1014.2
160900                  TO RE-MARK                                      NC1014.2
161000             PERFORM FAIL                                         NC1014.2
161100             PERFORM PRINT-DETAIL.                                NC1014.2
161200     ADD     1  TO REC-CT.                                        NC1014.2
161300 MPY-TEST-F1-26-2.                                                NC1014.2
161400     MOVE   "MPY-TEST-F1-26-2" TO PAR-NAME.                       NC1014.2
161500     IF      WRK-DS-05V00 = 00000                                 NC1014.2
161600             PERFORM PASS                                         NC1014.2
161700             PERFORM PRINT-DETAIL                                 NC1014.2
161800     ELSE                                                         NC1014.2
161900             MOVE    WRK-DS-05V00 TO COMPUTED-N                   NC1014.2
162000             MOVE    0            TO CORRECT-N                    NC1014.2
162100             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1014.2
162200                  TO RE-MARK                                      NC1014.2
162300             PERFORM FAIL                                         NC1014.2
162400             PERFORM PRINT-DETAIL.                                NC1014.2
162500     ADD     1  TO REC-CT.                                        NC1014.2
162600 MPY-TEST-F1-26-3.                                                NC1014.2
162700     MOVE   "MPY-TEST-F1-26-3" TO PAR-NAME.                       NC1014.2
162800     IF      WRK-DS-02V00 = 0                                     NC1014.2
162900             PERFORM PASS                                         NC1014.2
163000             PERFORM PRINT-DETAIL                                 NC1014.2
163100     ELSE                                                         NC1014.2
163200             MOVE   WRK-DS-02V00 TO COMPUTED-N                    NC1014.2
163300             MOVE   0            TO CORRECT-N                     NC1014.2
163400             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1014.2
163500                  TO RE-MARK                                      NC1014.2
163600             PERFORM FAIL                                         NC1014.2
163700             PERFORM PRINT-DETAIL.                                NC1014.2
163800     ADD     1  TO REC-CT.                                        NC1014.2
163900 MPY-TEST-F1-26-4.                                                NC1014.2
164000     MOVE   "MPY-TEST-F1-26-4" TO PAR-NAME.                       NC1014.2
164100     IF      WRK-CS-18V00 = 000000000000000099                    NC1014.2
164200             PERFORM PASS                                         NC1014.2
164300             PERFORM PRINT-DETAIL                                 NC1014.2
164400     ELSE                                                         NC1014.2
164500             MOVE   WRK-CS-18V00       TO COMPUTED-N              NC1014.2
164600             MOVE   000000000000000099 TO CORRECT-N               NC1014.2
164700             MOVE  "SCOPE TERMINATOR IGNORED" TO RE-MARK          NC1014.2
164800             PERFORM FAIL                                         NC1014.2
164900             PERFORM PRINT-DETAIL.                                NC1014.2
165000     ADD     1  TO REC-CT.                                        NC1014.2
165100 MPY-TEST-F1-26-5.                                                NC1014.2
165200     MOVE   "MPY-TEST-F1-26-5" TO PAR-NAME.                       NC1014.2
165300     IF      WRK-DS-10V00 = 1111111111                            NC1014.2
165400             PERFORM PASS                                         NC1014.2
165500             PERFORM PRINT-DETAIL                                 NC1014.2
165600     ELSE                                                         NC1014.2
165700             MOVE    WRK-DS-10V00 TO COMPUTED-18V0                NC1014.2
165800             MOVE    1111111111   TO CORRECT-18V0                 NC1014.2
165900             MOVE   "NOT ON SIZE ERROR SHOULD NOT BE EXECUTED"    NC1014.2
166000                  TO RE-MARK                                      NC1014.2
166100             PERFORM FAIL                                         NC1014.2
166200             PERFORM PRINT-DETAIL.                                NC1014.2
166300*                                                                 NC1014.2
166400*                                                                 NC1014.2
166500 MPY-INIT-F1-27.                                                  NC1014.2
166600*    ==-->EXPLICIT SCOPE TERMINATOR <--==                         NC1014.2
166700     MOVE   "IV-41 6.4.3" TO ANSI-REFERENCE.                      NC1014.2
166800     MOVE   "1" TO WRK-XN-00001.                                  NC1014.2
166900     MOVE   -99 TO WRK-DS-02V00.                                  NC1014.2
167000     MOVE    0  TO WRK-DS-10V00.                                  NC1014.2
167100     MOVE    0  TO WRK-DS-01V00.                                  NC1014.2
167200     MOVE    0  TO WRK-DS-18V00.                                  NC1014.2
167300     MOVE    1  TO REC-CT.                                        NC1014.2
167400 MPY-TEST-F1-27-0.                                                NC1014.2
167500     MULTIPLY AZERO-DS-05V05      BY WRK-DS-02V00                 NC1014.2
167600       ON SIZE ERROR GO TO MPY-TEST-F1-27-01                      NC1014.2
167700       NOT ON SIZE ERROR MOVE "0" TO WRK-XN-00001                 NC1014.2
167800                         MOVE 23  TO WRK-DS-10V00                 NC1014.2
167900                         MOVE -4  TO WRK-DS-01V00                 NC1014.2
168000     END-MULTIPLY.                                                NC1014.2
168100 MPY-TEST-F1-27-01.                                               NC1014.2
168200     MOVE    99 TO WRK-CS-18V00.                                  NC1014.2
168300     GO TO   MPY-TEST-F1-27-1.                                    NC1014.2
168400 MPY-DELETE-F1-27-1.                                              NC1014.2
168500     PERFORM DE-LETE.                                             NC1014.2
168600     PERFORM PRINT-DETAIL.                                        NC1014.2
168700     GO TO   MPY-INIT-F1-28.                                      NC1014.2
168800 MPY-TEST-F1-27-1.                                                NC1014.2
168900     MOVE   "MPY-TEST-F1-27-1" TO PAR-NAME.                       NC1014.2
169000     IF      WRK-XN-00001 = "0"                                   NC1014.2
169100             PERFORM PASS                                         NC1014.2
169200             PERFORM PRINT-DETAIL                                 NC1014.2
169300     ELSE                                                         NC1014.2
169400             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1014.2
169500             MOVE   "0"           TO CORRECT-X                    NC1014.2
169600             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1014.2
169700                  TO RE-MARK                                      NC1014.2
169800             PERFORM FAIL                                         NC1014.2
169900             PERFORM PRINT-DETAIL.                                NC1014.2
170000     ADD 1 TO REC-CT.                                             NC1014.2
170100 MPY-TEST-F1-27-2.                                                NC1014.2
170200     MOVE   "MPY-TEST-F1-27-2" TO PAR-NAME.                       NC1014.2
170300     IF      WRK-DS-10V00 = 23                                    NC1014.2
170400             PERFORM PASS                                         NC1014.2
170500             PERFORM PRINT-DETAIL                                 NC1014.2
170600     ELSE                                                         NC1014.2
170700             MOVE   WRK-DS-10V00 TO COMPUTED-N                    NC1014.2
170800             MOVE   23           TO CORRECT-N                     NC1014.2
170900             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1014.2
171000                  TO RE-MARK                                      NC1014.2
171100             PERFORM FAIL                                         NC1014.2
171200             PERFORM PRINT-DETAIL.                                NC1014.2
171300     ADD 1 TO REC-CT.                                             NC1014.2
171400 MPY-TEST-F1-27-3.                                                NC1014.2
171500     MOVE   "MPY-TEST-F1-27-3" TO PAR-NAME.                       NC1014.2
171600     IF      WRK-DS-02V00 = 00                                    NC1014.2
171700             PERFORM PASS                                         NC1014.2
171800             PERFORM PRINT-DETAIL                                 NC1014.2
171900     ELSE                                                         NC1014.2
172000             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1014.2
172100             MOVE    00           TO CORRECT-N                    NC1014.2
172200             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1014.2
172300                  TO RE-MARK                                      NC1014.2
172400             PERFORM FAIL                                         NC1014.2
172500             PERFORM PRINT-DETAIL.                                NC1014.2
172600     ADD 1 TO REC-CT.                                             NC1014.2
172700 MPY-TEST-F1-27-4.                                                NC1014.2
172800     MOVE   "MPY-TEST-F1-27-4" TO PAR-NAME.                       NC1014.2
172900     IF      WRK-DS-01V00 = -4                                    NC1014.2
173000             PERFORM PASS                                         NC1014.2
173100             PERFORM PRINT-DETAIL                                 NC1014.2
173200     ELSE                                                         NC1014.2
173300             MOVE   WRK-DS-01V00 TO COMPUTED-N                    NC1014.2
173400             MOVE   -4           TO CORRECT-N                     NC1014.2
173500             MOVE   "NOT ON SIZE ERROR SHOULD BE EXECUTED"        NC1014.2
173600                  TO RE-MARK                                      NC1014.2
173700             PERFORM FAIL                                         NC1014.2
173800             PERFORM PRINT-DETAIL.                                NC1014.2
173900     ADD 1 TO REC-CT.                                             NC1014.2
174000 MPY-TEST-F1-27-5.                                                NC1014.2
174100     MOVE   "MPY-TEST-F1-27-5" TO PAR-NAME                        NC1014.2
174200     IF      WRK-CS-18V00 = 000000000000000099                    NC1014.2
174300             PERFORM PASS                                         NC1014.2
174400             PERFORM PRINT-DETAIL                                 NC1014.2
174500     ELSE                                                         NC1014.2
174600             MOVE    WRK-CS-18V00       TO COMPUTED-N             NC1014.2
174700             MOVE    000000000000000099 TO CORRECT-N              NC1014.2
174800             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1014.2
174900             PERFORM FAIL                                         NC1014.2
175000             PERFORM PRINT-DETAIL.                                NC1014.2
175100*                                                                 NC1014.2
175200*                                                                 NC1014.2
175300 MPY-INIT-F1-28.                                                  NC1014.2
175400*    ==-->EXPLICIT SCOPE TERMINATOR <--==                         NC1014.2
175500     MOVE    A10ONES-DS-10V00        TO WRK-DS-10V00.             NC1014.2
175600     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
175700     MOVE    0  TO WRK-CS-18V00.                                  NC1014.2
175800     MOVE    1  TO REC-CT.                                        NC1014.2
175900 MPY-TEST-F1-28-0.                                                NC1014.2
176000     MULTIPLY A12THREES-DS-06V06     BY WRK-DS-10V00              NC1014.2
176100              ON SIZE ERROR MOVE "1" TO WRK-XN-00001              NC1014.2
176200          NOT ON SIZE ERROR MOVE "2" TO WRK-XN-00001              NC1014.2
176300     END-MULTIPLY                                                 NC1014.2
176400     MOVE    99 TO WRK-CS-18V00.                                  NC1014.2
176500     GO TO   MPY-TEST-F1-28-1.                                    NC1014.2
176600 MPY-DELETE-F1-28-1.                                              NC1014.2
176700     PERFORM DE-LETE.                                             NC1014.2
176800     PERFORM PRINT-DETAIL.                                        NC1014.2
176900     GO TO   MPY-INIT-F1-29.                                      NC1014.2
177000 MPY-TEST-F1-28-1.                                                NC1014.2
177100     MOVE   "MPY-TEST-F1-28-1" TO PAR-NAME.                       NC1014.2
177200     IF      WRK-XN-00001 = "1"                                   NC1014.2
177300             PERFORM PASS                                         NC1014.2
177400             PERFORM PRINT-DETAIL                                 NC1014.2
177500     ELSE                                                         NC1014.2
177600             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1014.2
177700             MOVE   "1"           TO CORRECT-X                    NC1014.2
177800             MOVE   "SIZE ERROR SHOULD HAVE OCCURRED"             NC1014.2
177900                  TO RE-MARK                                      NC1014.2
178000             PERFORM FAIL                                         NC1014.2
178100             PERFORM PRINT-DETAIL.                                NC1014.2
178200     ADD     1  TO REC-CT.                                        NC1014.2
178300 MPY-TEST-F1-28-2.                                                NC1014.2
178400     MOVE   "MPY-TEST-F1-28-2" TO PAR-NAME.                       NC1014.2
178500     IF      WRK-DS-10V00 = 1111111111                            NC1014.2
178600             PERFORM PASS                                         NC1014.2
178700             PERFORM PRINT-DETAIL                                 NC1014.2
178800     ELSE                                                         NC1014.2
178900             MOVE    WRK-DS-10V00 TO COMPUTED-18V0                NC1014.2
179000             MOVE    1111111111   TO CORRECT-18V0                 NC1014.2
179100             MOVE   "SIZE ERROR SHOULD HAVE OCCURRED"             NC1014.2
179200                  TO RE-MARK                                      NC1014.2
179300             PERFORM FAIL                                         NC1014.2
179400             PERFORM PRINT-DETAIL.                                NC1014.2
179500     ADD     1  TO REC-CT.                                        NC1014.2
179600 MPY-TEST-F1-28-3.                                                NC1014.2
179700     MOVE   "MPY-TEST-F1-28-3" TO PAR-NAME.                       NC1014.2
179800     IF      WRK-CS-18V00 = 000000000000000099                    NC1014.2
179900             PERFORM PASS                                         NC1014.2
180000             PERFORM PRINT-DETAIL                                 NC1014.2
180100     ELSE                                                         NC1014.2
180200             MOVE    WRK-CS-18V00       TO COMPUTED-N             NC1014.2
180300             MOVE    000000000000000099 TO CORRECT-N              NC1014.2
180400             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1014.2
180500             PERFORM FAIL                                         NC1014.2
180600             PERFORM PRINT-DETAIL.                                NC1014.2
180700*                                                                 NC1014.2
180800*                                                                 NC1014.2
180900 MPY-INIT-F1-29.                                                  NC1014.2
181000*    ==-->EXPLICIT SCOPE TERMINATOR <--==                         NC1014.2
181100     MOVE   "0" TO WRK-XN-00001.                                  NC1014.2
181200     MOVE   -99 TO WRK-DS-02V00.                                  NC1014.2
181300     MOVE    ZERO TO WRK-CS-18V00.                                NC1014.2
181400     MOVE    1 TO REC-CT.                                         NC1014.2
181500 MPY-TEST-F1-29-0.                                                NC1014.2
181600     MULTIPLY AZERO-DS-05V05         BY WRK-DS-02V00              NC1014.2
181700              ON SIZE ERROR MOVE "1" TO WRK-XN-00001              NC1014.2
181800          NOT ON SIZE ERROR MOVE "2" TO WRK-XN-00001              NC1014.2
181900     END-MULTIPLY                                                 NC1014.2
182000     MOVE    99 TO WRK-CS-18V00.                                  NC1014.2
182100     GO TO   MPY-TEST-F1-29-1.                                    NC1014.2
182200 MPY-DELETE-F1-29-1.                                              NC1014.2
182300     PERFORM DE-LETE.                                             NC1014.2
182400     PERFORM PRINT-DETAIL.                                        NC1014.2
182500     GO TO   CCVS-EXIT.                                           NC1014.2
182600 MPY-TEST-F1-29-1.                                                NC1014.2
182700     MOVE   "MPY-TEST-F1-29-1" TO PAR-NAME.                       NC1014.2
182800     IF      WRK-XN-00001 = "2"                                   NC1014.2
182900             PERFORM PASS                                         NC1014.2
183000             PERFORM PRINT-DETAIL                                 NC1014.2
183100     ELSE                                                         NC1014.2
183200             MOVE    WRK-XN-00001 TO COMPUTED-X                   NC1014.2
183300             MOVE   "2"           TO CORRECT-X                    NC1014.2
183400             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1014.2
183500                  TO RE-MARK                                      NC1014.2
183600             PERFORM FAIL                                         NC1014.2
183700             PERFORM PRINT-DETAIL.                                NC1014.2
183800     ADD     1 TO REC-CT.                                         NC1014.2
183900 MPY-TEST-F1-29-2.                                                NC1014.2
184000     MOVE   "MPY-TEST-F1-29-2" TO PAR-NAME.                       NC1014.2
184100     IF      WRK-DS-02V00 = 00                                    NC1014.2
184200             PERFORM PASS                                         NC1014.2
184300             PERFORM PRINT-DETAIL                                 NC1014.2
184400     ELSE                                                         NC1014.2
184500             MOVE    WRK-DS-02V00 TO COMPUTED-N                   NC1014.2
184600             MOVE    00           TO CORRECT-N                    NC1014.2
184700             MOVE   "WRONGLY AFFECTED BY SIZE ERROR"              NC1014.2
184800                  TO RE-MARK                                      NC1014.2
184900             PERFORM FAIL                                         NC1014.2
185000             PERFORM PRINT-DETAIL.                                NC1014.2
185100     ADD     1 TO REC-CT.                                         NC1014.2
185200 MPY-TEST-F1-29-3.                                                NC1014.2
185300     MOVE   "MPY-TEST-F1-29-3" TO PAR-NAME.                       NC1014.2
185400     IF      WRK-CS-18V00 = 000000000000000099                    NC1014.2
185500             PERFORM PASS                                         NC1014.2
185600             PERFORM PRINT-DETAIL                                 NC1014.2
185700     ELSE                                                         NC1014.2
185800             MOVE    WRK-CS-18V00       TO COMPUTED-N             NC1014.2
185900             MOVE    000000000000000099 TO CORRECT-N              NC1014.2
186000             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC1014.2
186100             PERFORM FAIL                                         NC1014.2
186200             PERFORM PRINT-DETAIL.                                NC1014.2
186300*                                                                 NC1014.2
186400*                                                                 NC1014.2
186500 CCVS-EXIT SECTION.                                               NC1014.2
186600 CCVS-999999.                                                     NC1014.2
186700     GO TO CLOSE-FILES.                                           NC1014.2
      *END-OF,NC101A                                                                  
